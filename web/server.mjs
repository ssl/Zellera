#!/usr/bin/env node
import { createServer } from 'node:http';
import { connect } from 'node:net';
import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { join, normalize } from 'node:path';
import { fileURLToPath } from 'node:url';
import { pagesHmac, safeEqual } from './cms/auth.mjs';
import { PROD } from './cms/config.mjs';
import { productionProblems, readDevSettings } from './lib/production-check.mjs';
import { query, transaction, READ_ONLY, settings } from './cms/db.mjs';
import { gameRules, gameRulesFile, gameRulesLocalFile, ruledVariables } from './lib/game-rules.mjs';
import { gameName, startGameName } from './cms/game-name.mjs';
import { displayNames, startDisplayNames } from './cms/display-names.mjs';
import { articleTexts, articleVariables, landingViewArticles, landingViewChangedAt, landingViewVersion, startLandingView } from './lib/landing-view.mjs';
import { createPages } from './cms/registry.mjs';
import { readBody, sameOriginRead } from './cms/lib/http.mjs';
import { createTexts } from './cms/lib/texts.mjs';
import { createCms } from './cms/routes.mjs';
import { createTheme } from './cms/theme.mjs';
import { EXTENSIONS_SETTING, createExtensions, loadExtensions, parseList } from './cms/extensions.mjs';
import { SHELLS } from './cms/shells.mjs';
import { createAssetMirror } from './lib/asset-mirror.mjs';
import { createAvatarImager } from './lib/avatar-imager.mjs';
import { createAvatarRenderer } from './lib/avatar-render.mjs';
import { lookGender, lookHash } from './api/handlers/rest.mjs';
import { ClientError, createClientFiles } from './lib/client-files.mjs';
import { VERSION, keyModulus, siteUrl } from './lib/env.mjs';
import { safeJoin, sendFile, sendText as send } from './lib/files.mjs';
import { bundleIcon, bundleRename } from './lib/asset-bundle.mjs';
import { furnidataTypes } from './lib/furni-files.mjs';
import { furnidataIndex } from './lib/page-icon.mjs';
import { createSiteAccess } from './lib/site-access.mjs';
import { renderPlayPage } from './lib/play-page.mjs';
import { createPlayRoute } from './lib/play-route.mjs';
import {
  applyOverrides as applyOverrideLines, clearForeignAdUrls, rewriteAssetUrls, rewriteTexts, rewriteVariables,
} from './lib/outbound.mjs';
import { PROFILE_MISSING, missingProfile, serverId } from './lib/client-profile.mjs';
import { startClientStrings } from './lib/client-strings.mjs';
import { createUserContent } from './lib/usercontent.mjs';
import { photoData, photoRows } from './lib/photodata.mjs';
import { PHOTO_ID, photoDir } from './lib/photo-files.mjs';
import { clientIp, createLimiter, ipLimitKey } from './lib/limits.mjs';

const root = normalize(join(fileURLToPath(new URL('.', import.meta.url)), '..'));
const PORT = Number(process.env.ZEL_WEB_PORT ?? 8760);
const LISTEN_HOST = process.env.ZEL_LISTEN_HOST || '127.0.0.1';
// Where this computer reaches a listener on LISTEN_HOST: loopback when that is every address.
const LOCAL_HOST = ['0.0.0.0', '::', '[::]'].includes(LISTEN_HOST) ? '127.0.0.1' : LISTEN_HOST;
const SITE = siteUrl(process.env);
const WS_URL = `${SITE.replace(/^http/, 'ws')}/websocket`;
const GAME_PORT = Number(process.env.ZEL_WS_PORT ?? 2076);
const ASSET_URL = (process.env.ZEL_ASSET_URL ?? '').replace(/\/+$/, '');

const clientDir = normalize(process.env.ZEL_CLIENT_DIR ?? join(root, 'client'));
const gamedataDir = normalize(process.env.ZEL_GAMEDATA_DIR ?? join(root, '.data', 'gamedata'));
const overridesDir = join(gamedataDir, 'overrides');
const assetCache = normalize(process.env.ZEL_ASSET_DIR || join(root, '.data', 'assets'));
const userContentDir = normalize(join(process.env.ZEL_DATA_DIR ?? join(root, '.data', 'run', 'data'), 'usercontent'));
const offersFile = normalize(join(process.env.ZEL_DATA_DIR ?? join(root, '.data', 'run', 'data'), 'furnidata-offers.json'));
gameRules();
const access = createSiteAccess({ query });
const cms = createCms({ SITE, accountRules: () => access.accounts(), guestsOn: () => access.on('guests') });

const extensionList = process.env[EXTENSIONS_SETTING.env] ?? EXTENSIONS_SETTING.list;
const extensions = createExtensions(await loadExtensions({ list: parseList(extensionList), query, readOnly: READ_ONLY, log: (m) => console.log(m) }),
  { setting: () => access.extensions() });
const theme = createTheme({ extensions });
const themeProblems = theme.problems([...new Set([...SHELLS.map((s) => s.page), '404', 'hk/confirm'])]);
if (themeProblems.length) throw new Error(`the web theme ${theme.dir} is incomplete:\n  ${themeProblems.join('\n  ')}`);
for (const n of theme.needs()) {
  if (!extensions.get(n)) console.error(`the web theme ${theme.dir} needs the web extension ${n}, which ${EXTENSIONS_SETTING.env} does not turn on: its parts stay empty`);
}
await startClientStrings(query);
await startGameName(settings);
await startDisplayNames(async () => ({
  currencies: await query('SELECT `id`, `name`, `role` FROM currencies'),
  names: await query('SELECT `code`, `name` FROM display_names'),
}));
await startLandingView(() => query(`SELECT id, title, body AS text, button_text, button_kind AS button_type, button_link, image FROM news_articles
  WHERE deleted = 0 ORDER BY sort_order, id LIMIT 4`));

const applyOverrides = (text, overridesFile) =>
  (existsSync(overridesFile) ? applyOverrideLines(text, readFileSync(overridesFile, 'utf8'), SITE) : text);

const GAMEDATA = {
  furnidata: { file: 'furnidata', type: 'application/json; charset=utf-8', extraInputs: () => [offersFile], transform: (t) => transformFurnidata(t) },
  productdata: { file: 'productdata', type: 'application/json; charset=utf-8', extraInputs: () => [productOverridesFile], transform: (t) => addProducts(t) },
  external_variables: {
    file: 'external_variables', type: 'text/plain; charset=utf-8', extraInputs: () => [gameRulesFile(), gameRulesLocalFile()],
    transform: (t) => applyOverrideLines(rewriteVariables(ruledVariables(applyOverrides(rewriteAssetUrls(t, SITE), join(overridesDir, 'external_variables.txt'))), SITE),
      articleVariables(landingViewArticles(), SITE), SITE),
    version: landingViewVersion, changedAt: landingViewChangedAt,
  },
  external_texts: {
    file: 'external_texts', type: 'text/plain; charset=utf-8',
    transform: (t) => applyOverrideLines(rewriteTexts(applyOverrides(t, join(overridesDir, 'external_texts.txt')), SITE),
      articleTexts(landingViewArticles()), SITE),
    version: landingViewVersion, changedAt: landingViewChangedAt,
  },
  figurepartlist: { file: 'figurepartlist', type: 'application/xml; charset=utf-8', transform: (t) => unlockClothing(t) },
  figurepartlist_json: { file: 'figurepartlist_json', type: 'application/json; charset=utf-8', transform: (t) => unlockClothingJson(t) },
};
const productOverridesFile = join(overridesDir, 'productdata.json');
function addProducts(text) {
  if (!existsSync(productOverridesFile)) return text;
  const data = JSON.parse(text);
  const list = data.productdata?.product;
  if (!Array.isArray(list)) return text;
  const have = new Set(list.map((p) => p.code));
  for (const p of JSON.parse(readFileSync(productOverridesFile, 'utf8')).product ?? []) {
    if (p?.code && !have.has(p.code)) list.push({ code: p.code, name: p.name ?? '', description: p.description ?? '' });
  }
  return JSON.stringify(data);
}
function transformFurnidata(text) {
  const data = JSON.parse(text);
  if (existsSync(offersFile)) applyFurniOffers(data);
  clearForeignAdUrls(data, SITE);
  return JSON.stringify(data);
}
function applyFurniOffers(data) {
  const { offers } = JSON.parse(readFileSync(offersFile, 'utf8'));
  for (const [section, kind] of [['roomitemtypes', 's'], ['wallitemtypes', 'i']]) {
    for (const f of data[section]?.furnitype ?? []) {
      const o = offers[`${kind}:${f.classname}`] ?? {};
      f.offerid = o.offerid ?? -1;
      f.bcofferid = o.bcofferid ?? -1;
      f.bc = o.bcofferid != null;
      f.rentofferid = -1;
      f.buyout = false;
      f.rentbuyout = false;
    }
  }
}
function unlockClothing(xml) {
  return xml.replace(/<(set|color)\b[^>]*>/g, (tag) => tag.replace(/\bclub="[0-9]+"/, 'club="0"').replace(/\bsellable="1"/, 'sellable="0"'));
}
function unlockClothingJson(text) {
  const data = JSON.parse(text);
  const walk = (o) => {
    if (Array.isArray(o)) { o.forEach(walk); return; }
    if (!o || typeof o !== 'object') return;
    if ('club' in o && typeof o.club === 'number') o.club = 0;
    if (o.sellable === 1 || o.sellable === true) o.sellable = typeof o.sellable === 'boolean' ? false : 0;
    for (const v of Object.values(o)) if (v && typeof v === 'object') walk(v);
  };
  walk(data);
  return JSON.stringify(data);
}
const GAMEDATA_PATHS = {
  furnidata_json: 'furnidata', furnidata: 'furnidata', productdata_json: 'productdata', productdata: 'productdata',
  external_variables: 'external_variables', external_flash_texts: 'external_texts', external_texts: 'external_texts',
  figuredata: 'figurepartlist', figuredata_json: 'figurepartlist_json',
};
const HASHES_ORDER = [
  ['furnidata', 'furnidata_json'], ['productdata', 'productdata_json'], ['external_variables', 'external_variables'],
  ['external_texts', 'external_flash_texts'], ['figurepartlist', 'figuredata'], ['figurepartlist_json', 'figuredata_json'],
];

const gamedataCache = new Map();

let missingLogged = '';
const missingGamedata = () => {
  const missing = HASHES_ORDER.map(([name]) => GAMEDATA[name].file).filter((f) => !existsSync(join(gamedataDir, f)));
  const key = missing.join(',');
  if (key && key !== missingLogged) console.error(`gamedata missing in ${gamedataDir}: ${key} (it comes with a content pack: ./zel import <pack.zip>)`);
  missingLogged = key;
  return missing;
};
const GAMEDATA_MISSING = 'gamedata missing on this server: it comes with a content pack (./zel import <pack.zip>)';
const loadGamedata = (name) => {
  const def = GAMEDATA[name];
  const file = join(gamedataDir, def.file);
  const overrides = name === 'external_variables' || name === 'external_texts'
    ? join(overridesDir, `${name}.txt`) : null;
  const extra = [overrides, ...(def.extraInputs?.() ?? [])].filter((f) => f && existsSync(f));
  const mtime = Math.max(statSync(file).mtimeMs, ...extra.map((f) => statSync(f).mtimeMs));
  const stamp = `${mtime}|${def.version?.() ?? ''}`;
  const cached = gamedataCache.get(name);
  if (cached && cached.stamp === stamp) return cached;
  let body = readFileSync(file);
  if (def.transform) body = Buffer.from(def.transform(body.toString('utf8')), 'utf8');
  const entry = { body, hash: createHash('sha1').update(body).digest('hex'), mtime: Math.max(mtime, def.changedAt?.() ?? 0), stamp, type: def.type };
  gamedataCache.set(name, entry);
  return entry;
};

const readJsonArray = async (req, max) => {
  const body = await readBody(req, max);
  if (!body) return null;
  try {
    const v = JSON.parse(body.toString('utf8'));
    return Array.isArray(v) ? v : null;
  } catch { return null; }
};

const hashesIndex = () => ({
  hashes: HASHES_ORDER.map(([name, path]) => ({ name, url: `${SITE}/gamedata/${path}`, hash: loadGamedata(name).hash })),
});

const clientFiles = (() => {
  try {
    return createClientFiles({ clientDir, modulus: keyModulus(process.env) });
  } catch (e) {
    if (!(e instanceof ClientError)) throw e;
    console.error(e.message);
    return process.exit(1);
  }
})();

const playPage = (ticket) => renderPlayPage(clientFiles.entryPage(), {
  base: clientFiles.base(),
  session: { server: serverId(), ticket, websiteUrl: SITE, websocketUrl: WS_URL },
});
const playLimiter = createLimiter();
const BADGE_LIMIT = Object.freeze([600, 60]);
const playRoute = createPlayRoute({ cms, playPage, limiter: playLimiter, clientIp });

const canSee = async (req, area) => (await access.on(area))
  || Boolean(await Promise.resolve().then(() => cms.currentUser(req)).catch(() => null));

const pages = await createPages({
  SITE, gameName, displayNames, PROD, query, transaction, access, theme, extensions,
  readOnly: READ_ONLY,
  onAudit: (row) => console.error(`pages audit (read-only server, not written): ${JSON.stringify(row)}`),
  session: async (req) => {
    const me = await cms.currentUser(req);
    return me && { userId: me.user.id, sessionId: me.session.nonce, accountId: me.account.id };
  },
  account: async (req) => {
    const me = await cms.currentAccount(req);
    return me && { id: me.account.id, avatarId: me.user ? Number(me.user.id) : 0 };
  },
  endedByBan: (req) => cms.endedByBan(req),
  sessionEnd: (req) => cms.sessionEnd(req),
  hmac: pagesHmac,
  safeEqual,
  clientIp,
  texts: createTexts(() => loadGamedata('external_texts').body),
  userContent: {
    exists: (rel) => {
      const file = safeJoin(userContentDir, String(rel));
      return !!file && existsSync(file) && statSync(file).isFile();
    },
  },
  log: (msg) => console.error(msg),
});
if (pages.missing.length) console.log(`page modules not present: ${pages.missing.join(', ')}`);
if (PROD) {
  const problems = await readDevSettings(query)
    .then((settings) => productionProblems({ settings, routes: pages.routes }))
    .catch((e) => [`cannot read the settings: ${e.message}`]);
  if (problems.length) {
    console.error(`production mode (ZEL_ENV is not dev) refuses to start:\n${problems.map((p) => `  ${p}`).join('\n')}`);
    process.exit(1);
  }
}

const webLimiter = createLimiter();
let furniIndex = { mtime: 0, map: new Map(), types: new Map() };
const furnidataNow = () => {
  try {
    const file = join(gamedataDir, 'furnidata'), mtime = statSync(file).mtimeMs;
    if (mtime !== furniIndex.mtime) {
      const text = readFileSync(file, 'utf8');
      furniIndex = { mtime, map: furnidataIndex(text), types: furnidataTypes(text) };
    }
  } catch { return null; }
  return furniIndex;
};
const furniById = (id) => furnidataNow()?.map.get(id) ?? null;
const furniTypes = (name) => furnidataNow()?.types.get(name) ?? null;
const assetMirror = createAssetMirror({
  cacheDir: assetCache, ownDir: join(overridesDir, 'assets'), origin: ASSET_URL, limiter: webLimiter, bundleIcon, bundleRename,
  furniById, furniTypes,
});
const userContent = createUserContent({ dir: userContentDir, currentUser: (req) => cms.currentUser(req), limiter: webLimiter, access });

const assetRelease = () => new URL(readFileSync(join(gamedataDir, 'asset-release.txt'), 'utf8').trim()).pathname.replace(/^\/+|\/+$/g, '');
const renderLibFile = () => {
  const file = clientFiles.renderLibrary();
  if (!file) throw new Error(`the client at ${clientDir} names no avatar render library`);
  return file;
};
const avatarImager = createAvatarImager({
  renderer: createAvatarRenderer({
    readRenderLib: () => readFileSync(renderLibFile()),
    readAsset: async (name) => {
      const file = await assetMirror.fetchAsset(`${assetRelease()}/${name}`);
      return file ? readFileSync(file) : null;
    },
    readFiguredata: () => loadGamedata('figurepartlist').body.toString('utf8'),
    version: () => `${loadGamedata('figurepartlist').hash} ${statSync(renderLibFile()).mtimeMs} ${assetRelease()}`,
  }),
  limiter: createLimiter(),
  cacheDir: join(userContentDir, 'avatarimage'),
  log: (msg) => console.error(msg),
});
const LOOKS_MS = 10_000;
let looksByHash = { at: -Infinity, map: new Map() };
const featuredLook = async (hash) => {
  if (Date.now() - looksByHash.at >= LOOKS_MS) {
    const looks = await query('SELECT gender, figure FROM featured_looks WHERE enabled = TRUE');
    looksByHash = { at: Date.now(), map: new Map(looks.map((l) => [lookHash(lookGender(l.gender), l.figure), l])) };
  }
  return looksByHash.map.get(hash) ?? null;
};
const avatarParams = async (params, hash) => {
  let row = null;
  if (hash) {
    row = await featuredLook(hash);
  } else if (params.has('user') && !params.has('figure')) {
    [row] = await query('SELECT figure, gender FROM players WHERE name = ? LIMIT 1', [params.get('user').slice(0, 50)]);
  } else return params;
  if (!row) return null;
  const out = new URLSearchParams(params);
  out.set('figure', row.figure);
  out.set('gender', lookGender(row.gender).toUpperCase());
  return out;
};

let publicApiBroken = false;
const apiCanSee = async (req, area) => (sameOriginRead(req, SITE, PROD) ? canSee(req, area) : access.on(area));
const publicApi = (req, res, url) => import('./api/index.mjs').then((api) => api.handle(req, res, url, { canSee: apiCanSee }), (e) => {
  if (!publicApiBroken) console.error(`web/api did not load; /api/public and /extradata/public answer 500 until a restart: ${e?.stack ?? e}`);
  publicApiBroken = true;
  send(res, 500, 'internal server error');
});

const gameServerUp = () => new Promise((resolve) => {
  const socket = connect({ host: LOCAL_HOST, port: GAME_PORT });
  const done = (up) => { socket.destroy(); resolve(up); };
  socket.setTimeout(500, () => done(false));
  socket.once('connect', () => done(true));
  socket.once('error', () => done(false));
});

const server = createServer(async (req, res) => {
  let url, path;
  try {
    url = new URL(req.url, SITE);
    if (/^\/(?:api|extradata)\/public(?:\/|$)/i.test(url.pathname)) return void await publicApi(req, res, url);
    path = decodeURIComponent(url.pathname);
  } catch {
    return send(res, 400, 'bad request');
  }
  try {
    if (await cms.handle(req, res, path)) return;
    if ((req.method === 'GET' || req.method === 'HEAD') && path.startsWith('/theme/') && theme.asset(req, res, path.slice('/theme/'.length))) return;
    if (await pages.handle(req, res, path, url)) return;

    const isPlay = path === '/play' || path === '/play/';
    const photoBatch = req.method === 'POST' && path === '/photodata/public/furni/';
    if (req.method !== 'GET' && req.method !== 'HEAD' && !photoBatch && !(isPlay && req.method === 'POST')) return send(res, 405, 'method not allowed');

    if (path === '/health') {
      const down = [clientFiles.present() ? '' : 'the client is missing', existsSync(gamedataDir) ? '' : 'the gamedata is missing'].filter(Boolean);
      const body = { status: down.length ? down.join('; ') : 'ok', version: VERSION, gameServer: await gameServerUp() };
      return send(res, down.length ? 503 : 200, JSON.stringify(body), 'application/json', { 'cache-control': 'no-store' });
    }

    if (path.startsWith('/usercontent/')) return await userContent(req, res, path.slice('/usercontent/'.length), clientIp(req));

    if (path.startsWith('/photodata/public/furni/')) {
      if (!(await canSee(req, 'photos'))) return send(res, 404, 'not found');
      const exists = (id) => {
        if (typeof id !== 'string' || !PHOTO_ID.test(id)) return false;
        const file = safeJoin(userContentDir, `${photoDir()}/${id}.png`);
        return Boolean(file && existsSync(file));
      };
      const answer = async (ids) => {
        const rows = await photoRows(query, ids.filter(exists));
        return ids.map((id) => (exists(id) ? photoData(id, rows.get(id) ?? null, SITE, serverId()) : null));
      };
      const headers = { 'cache-control': 'no-cache', 'access-control-allow-origin': '*' };
      if (req.method === 'POST' && path === '/photodata/public/furni/') {
        const ids = await readJsonArray(req, 8192);
        if (!ids) return send(res, 400, 'bad request');
        const asked = ids.slice(0, 50);
        const found = await answer(asked);
        const out = asked.map((id, i) => found[i] ?? { id: String(id).slice(0, 64), status: 'REJECTED' });
        return send(res, 200, JSON.stringify(out), 'application/json; charset=utf-8', headers);
      }
      const [found] = await answer([path.slice('/photodata/public/furni/'.length)]);
      if (!found) return send(res, 404, 'not found');
      return send(res, 200, JSON.stringify(found), 'application/json; charset=utf-8', headers);
    }

    const look = /^\/imaging\/avatar\/([0-9a-f]{32})(?:\.png|\.gif)?$/.exec(path);
    if (path === '/imaging/avatarimage' || look) {
      const byName = !look && url.searchParams.has('user') && !url.searchParams.has('figure');
      if (byName && !(await canSee(req, 'players'))) return send(res, 404, 'not found', 'text/plain', { 'cache-control': 'no-cache' });
      const params = await avatarParams(url.searchParams, look?.[1]);
      if (!params) return send(res, 404, 'not found', 'text/plain', { 'cache-control': 'no-cache' });
      const open = !byName || (await access.on('players'));
      return await avatarImager(req, res, params, { ip: ipLimitKey(clientIp(req)), open });
    }

    const badge = /^\/imaging\/badge\/([a-z0-9]{1,64})\.(?:png|gif)$/i.exec(path);
    if (badge) {
      const hit = playLimiter.hit(`badge:${ipLimitKey(clientIp(req))}`, ...BADGE_LIMIT);
      if (!hit.ok) return send(res, 429, 'too many requests', 'text/plain', { 'retry-after': String(hit.retryAfter), 'cache-control': 'no-store' });
      if (!(await canSee(req, 'players'))) return send(res, 404, 'not found', 'text/plain', { 'cache-control': 'no-cache' });
      const file = safeJoin(userContentDir, `badges/${badge[1]}.png`);
      if (!file || !existsSync(file)) return send(res, 404, 'not found', 'text/plain', { 'cache-control': 'no-cache' });
      return sendFile(req, res, file, { 'cache-control': (await access.on('players')) ? 'public, max-age=86400' : 'private, max-age=86400', 'x-content-type-options': 'nosniff', 'access-control-allow-origin': '*' });
    }

    if (isPlay) {
      if (missingProfile().length) return send(res, 503, PROFILE_MISSING, 'text/plain; charset=utf-8', { 'cache-control': 'no-store' });
      return await playRoute(req, res, url);
    }

    if (path.startsWith('/client/')) return clientFiles.send(req, res, path);

    if (path === '/gamedata/hashes2' || path === '/gamedata/hashes') {
      if (missingGamedata().length) return send(res, 503, GAMEDATA_MISSING, 'text/plain; charset=utf-8', { 'cache-control': 'no-store' });
      return send(res, 200, JSON.stringify(hashesIndex()), 'application/json; charset=utf-8', { 'cache-control': 'no-cache' });
    }

    if (path.startsWith('/gamedata/')) {
      const seg = path.split('/')[2];
      const name = Object.hasOwn(GAMEDATA_PATHS, seg) ? GAMEDATA_PATHS[seg] : null;
      if (!name) return send(res, 404, 'not found');
      if (!existsSync(join(gamedataDir, GAMEDATA[name].file))) {
        missingGamedata();
        return send(res, 503, GAMEDATA_MISSING, 'text/plain; charset=utf-8', { 'cache-control': 'no-store' });
      }
      const g = loadGamedata(name);
      const etag = `"${g.hash}"`;
      const headers = { 'cache-control': 'public, no-cache', etag, 'last-modified': new Date(g.mtime).toUTCString() };
      const tags = (req.headers['if-none-match'] ?? '').split(',').map((t) => t.trim().replace(/^W\//, ''));
      const since = Date.parse(req.headers['if-modified-since'] ?? '');
      const fresh = req.headers['if-none-match'] ? tags.includes(etag) || tags.includes('*')
        : !Number.isNaN(since) && Math.floor(g.mtime / 1000) <= Math.floor(since / 1000);
      if (fresh) {
        res.writeHead(304, headers);
        return res.end();
      }
      res.writeHead(200, { 'content-type': g.type, 'content-length': g.body.length, ...headers });
      return res.end(req.method === 'HEAD' ? undefined : g.body);
    }

    if (path.startsWith('/assets/')) {
      if (missingProfile().length) return send(res, 503, PROFILE_MISSING, 'text/plain; charset=utf-8', { 'cache-control': 'no-store' });
      return await assetMirror.handle(req, res, path.slice('/assets/'.length), clientIp(req));
    }

    if (/text\/html/.test(String(req.headers.accept ?? ''))) return void theme.notFound(req, res);
    return send(res, 404, 'not found');
  } catch (e) {
    console.error(`${req.method} ${url.pathname}: ${e.stack ?? e}`);
    if (!res.headersSent) send(res, 500, 'error');
    else res.destroy();
  }
});

server.listen(PORT, LISTEN_HOST, () => {
  console.log(`web on http://${LISTEN_HOST}:${server.address().port}  (public ${SITE}/play?sso=…)`);
});
