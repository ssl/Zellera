import { existsSync } from 'node:fs';
import { displayNames } from './display-names.mjs';
import { fileURLToPath } from 'node:url';
import { COMMAND_GRANT, waitForCommands } from './lib/commands.mjs';
import { makeDb } from './lib/db.mjs';
import { HK_HEADERS, crossSite, originOk, readForm, readJson, redirect, sameSiteFetch, sendJson, sendText } from './lib/http.mjs';
import { compile, match, normalizePath } from './lib/router.mjs';
import { FORBIDDEN_COLUMNS, PUBLIC_INSERTS, STAFF_ONLY_COLUMNS, STAFF_ONLY_TABLES } from './lib/sql.mjs';
import { emptyTexts } from './lib/texts.mjs';
import { HK_API, HK_MODULES } from '../hk-api/index.mjs';
import { SHELLS, isHkShell } from './shells.mjs';
import { SITE_API, SITE_MODULES } from '../site-api/index.mjs';
import { DEFAULT_ACCESS, placeOf } from '../lib/site-access.mjs';
import { EXT_API, LOOKUP_GRANTS, NO_EXTENSIONS, extensionDb, keysOf, lookups } from './extensions.mjs';
import { createLimiter, ipLimitKey } from '../lib/limits.mjs';
import { createWordFilter } from './word-filter.mjs';
import * as guard from '../hk-api/_guard.mjs';

const HK_PREFIX = '/housekeeping';
export const MODULES = Object.freeze([...SITE_MODULES, ...HK_MODULES]);

const BASELINE_LIMITS = Object.freeze({
  publicGet: [240, 60],
  publicPost: [30, 60],
  hkGet: [600, 600],
  hkPost: [60, 3600],
});

const MAX_BODY = 16384;

const GATES = ['photos', 'players', 'community'];
const SHOWN = ['staff'];
const IDENT = /^[a-z_][a-z0-9_]*$/;
const under = (p, prefix) => p === prefix || p.startsWith(`${prefix}/`);
const isHkPath = (p) => under(p, HK_PREFIX) || under(p, HK_API);
const isApiPath = (p) => under(p, SITE_API) || under(p, HK_API) || under(p, EXT_API);

export const validateModule = (file, owner, mod) => {
  const fail = (msg) => { throw new Error(`${file}: ${msg}`); };
  const hkFile = file.startsWith('hk-api/');
  if (mod.owner !== owner) fail(`export const owner = '${owner}' is required (got ${JSON.stringify(mod.owner)})`);
  if (!Array.isArray(mod.routes)) fail('export const routes = [...] is required');
  for (const r of mod.routes) {
    if (!r || typeof r !== 'object') fail('route must be an object');
    if (r.method !== 'GET' && r.method !== 'POST') fail(`method must be 'GET' or 'POST' (${r.path})`);
    if (typeof r.handler !== 'function') fail(`handler must be a function (${r.method} ${r.path})`);
    compile(r.path);
    if (isHkPath(r.path) !== hkFile) fail(`${r.path}: housekeeping routes (${HK_API}, ${HK_PREFIX}) belong in housekeeping/*.mjs and only there`);
    if (under(r.path, SITE_API) && hkFile) fail(`${r.path}: a housekeeping module serves no site API route`);
    if (hkFile && !under(r.path, HK_API) && r.method !== 'GET') fail(`${r.method} ${r.path}: housekeeping writes go through ${HK_API}`);
    if (hkFile) {
      const perms = [].concat(r.perm ?? []);
      if (perms.length === 0) fail(`${r.method} ${r.path}: housekeeping routes need perm`);
      for (const p of perms) if (!guard.HK_PERMS.includes(p)) fail(`${r.method} ${r.path}: unknown perm ${p}`);
    } else if (r.perm != null) {
      fail(`${r.method} ${r.path}: perm is for housekeeping routes`);
    }
    if (r.audited != null && (typeof r.audited !== 'boolean' || (r.audited && (!hkFile || r.method !== 'GET')))) {
      fail(`${r.method} ${r.path}: audited is true or false, and only on housekeeping GET routes`);
    }
    if (r.gate != null && (hkFile || r.method !== 'GET' || !GATES.includes(r.gate))) {
      fail(`${r.method} ${r.path}: gate is ${GATES.join(' or ')}, and only on public GET routes`);
    }
    if (r.shown != null && (hkFile || r.method !== 'GET' || !SHOWN.includes(r.shown))) {
      fail(`${r.method} ${r.path}: shown is ${SHOWN.join(' or ')}, and only on public GET routes`);
    }
    if (r.maxBody != null && (!Number.isInteger(r.maxBody) || r.maxBody < 1 || r.maxBody > MAX_BODY || r.method !== 'POST')) {
      fail(`${r.method} ${r.path}: maxBody is 1..${MAX_BODY}, only on POST routes`);
    }
    if (r.write != null) {
      if (r.write !== true) fail(`${r.method} ${r.path}: write is true or left out`);
      if (hkFile) fail(`${r.method} ${r.path}: housekeeping POST routes write without write: true`);
      if (r.method !== 'POST') fail(`${r.method} ${r.path}: only a POST route writes`);
      if (!sendsCommands(mod) && intakeTables(mod).length === 0) {
        fail(`${r.method} ${r.path}: write: true needs the web_commands insert or a public insert (PUBLIC_INSERTS) in the module's grants`);
      }
    }
  }
  validateGrants(file, mod.grants ?? [], hkFile);
};

const deleteTables = (mod) => (Array.isArray(mod.grants) ? mod.grants : []).filter((g) => g?.delete === true).map((g) => g.table);
const sendsCommands = (mod) => (Array.isArray(mod.grants) ? mod.grants : [])
  .some((g) => g?.table === COMMAND_GRANT.table && Array.isArray(g.insert) && g.insert.length > 0);
const intakeTables = (mod) => (Array.isArray(mod.grants) ? mod.grants : [])
  .filter((g) => Object.hasOwn(PUBLIC_INSERTS, g?.table ?? '') && Array.isArray(g.insert) && g.insert.length > 0).map((g) => g.table);

export const validateGrants = (file, grants, hkFile) => {
  const fail = (msg) => { throw new Error(`${file}: grants: ${msg}`); };
  if (!Array.isArray(grants)) fail('must be an array');
  for (const g of grants) {
    if (!g || !IDENT.test(g.table ?? '')) fail(`bad table ${JSON.stringify(g?.table)}`);
    const forbidden = FORBIDDEN_COLUMNS[g.table] ?? [];
    const staffCols = STAFF_ONLY_COLUMNS[g.table] ?? [];
    if (!hkFile && STAFF_ONLY_TABLES.includes(g.table)) fail(`${g.table} is housekeeping-only`);
    if (!hkFile && g.update) fail(`${g.table}: a public module updates nothing (it sends web_commands rows)`);
    if (g.delete != null && (g.delete !== true || !hkFile)) fail(`${g.table}: delete is true or left out, and only in a housekeeping module`);
    if (!hkFile && g.insert) {
      const allowed = g.table === COMMAND_GRANT.table ? COMMAND_GRANT.insert : PUBLIC_INSERTS[g.table];
      if (!Object.hasOwn(PUBLIC_INSERTS, g.table) && g.table !== COMMAND_GRANT.table) {
        fail(`${g.table}: only housekeeping modules insert (a public module only web_commands and ${Object.keys(PUBLIC_INSERTS).join(', ')})`);
      }
      for (const c of Array.isArray(g.insert) ? g.insert : [g.insert]) {
        if (!allowed.includes(c)) fail(`${g.table}.${c}: a public module inserts only ${allowed.join(', ')}`);
      }
    }
    for (const priv of ['select', 'insert', 'update']) {
      const cols = g[priv];
      if (cols == null) continue;
      if (cols === '*') {
        if (priv !== 'select') fail(`${g.table}: '*' only for select`);
        if (forbidden.length || (!hkFile && staffCols.length)) fail(`${g.table}: name the columns (this table has protected ones)`);
        continue;
      }
      if (!Array.isArray(cols) || cols.length === 0) fail(`${g.table}.${priv} must be a non-empty array or '*'`);
      for (const c of cols) {
        if (!IDENT.test(c)) fail(`${g.table}: bad column ${JSON.stringify(c)}`);
        if (forbidden.includes(c)) fail(`${g.table}.${c} may not be granted to the web`);
        if (!hkFile && priv === 'select' && staffCols.includes(c)) fail(`${g.table}.${c} is housekeeping-only`);
      }
    }
  }
};

export const loadModules = async () => {
  const loaded = [];
  const missing = [];
  for (const [file, owner] of MODULES) {
    const url = new URL(`../${file}`, import.meta.url);
    if (!existsSync(fileURLToPath(url))) { missing.push(file); continue; }
    const mod = await import(url.href);
    validateModule(file, owner, mod);
    loaded.push({ file, owner, mod });
  }
  return { loaded, missing };
};

export const collectGrants = (loaded) => {
  const byTable = new Map();
  const add = (g) => {
    const t = byTable.get(g.table) ?? { table: g.table, select: new Set(), selectAll: false, insert: new Set(), update: new Set(), delete: false };
    if (g.select === '*') t.selectAll = true; else for (const c of g.select ?? []) t.select.add(c);
    for (const c of g.insert ?? []) t.insert.add(c);
    for (const c of g.update ?? []) t.update.add(c);
    if (g.delete === true) t.delete = true;
    byTable.set(g.table, t);
  };
  for (const g of guard.grants) add(g);
  for (const g of LOOKUP_GRANTS) add(g);
  for (const { mod } of loaded) for (const g of mod.grants ?? []) add(g);
  return [...byTable.values()].sort((a, b) => a.table.localeCompare(b.table)).map((t) => ({
    table: t.table,
    select: t.selectAll ? '*' : [...t.select].sort(),
    insert: [...t.insert].sort(),
    update: [...t.update].sort(),
    ...(t.delete ? { delete: true } : {}),
  }));
};

const formOf = (body) => {
  const form = new URLSearchParams();
  for (const [k, v] of Object.entries(body ?? {})) {
    if (typeof v === 'string' || typeof v === 'number' || typeof v === 'boolean') form.set(k, String(v));
  }
  return form;
};

const REQUIRED_DEPS = ['SITE', 'query', 'session', 'hmac', 'safeEqual', 'clientIp'];

export const createPages = async (deps) => {
  for (const k of REQUIRED_DEPS) if (deps?.[k] == null) throw new Error(`createPages: deps.${k} is required`);
  const { loaded, missing } = await loadModules();
  const table = [];
  const seen = new Map();
  for (const { file, owner, mod } of loaded) {
    for (const r of mod.routes) {
      const key = `${r.method} ${r.path}`;
      if (seen.has(key)) throw new Error(`pages: ${key} is defined in ${seen.get(key)} and ${file}`);
      seen.set(key, file);
      table.push({
        method: r.method, path: r.path, handler: r.handler, file, owner,
        hk: isHkPath(r.path), api: isApiPath(r.path), perm: [].concat(r.perm ?? []), audited: r.audited === true, compiled: compile(r.path),
        gate: r.gate ?? null,
        shown: r.shown ?? null,
        write: r.write === true,
        inserts: r.write === true ? intakeTables(mod) : [],
        deletes: deleteTables(mod),
        maxBody: r.maxBody ?? MAX_BODY,
      });
    }
  }
  const extensions = deps.extensions ?? NO_EXTENSIONS;
  const limiter = createLimiter();
  const wordFilter = createWordFilter(deps.query);
  for (const ext of extensions.list) {
    for (const r of ext.routes) {
      const key = `${r.method} ${r.path}`;
      if (seen.has(key)) throw new Error(`pages: ${key} of web extension ${ext.name} is defined in ${seen.get(key)} too`);
      seen.set(key, `extension ${ext.name}`);
      table.push({
        method: r.method, path: r.path, handler: (req, res, ctx) => r.handler(extCtx(ctx, res, ext, r)), file: `ext:${ext.name}`, owner: `ext:${ext.name}`,
        hk: r.hk, api: true, perm: r.perm ? [r.perm] : [], audited: false, compiled: compile(r.path), gate: r.gate, shown: null, auth: r.auth,
        write: false, inserts: [], maxBody: r.maxBody ?? MAX_BODY, ext: ext.name,
      });
    }
  }
  const extShells = extensions.list.flatMap((ext) => Object.entries(ext.pages).map(([page, p]) => ({
    path: p.path, page, perm: p.perm, gate: p.gate, auth: p.auth, ext: ext.name,
  })));
  const shells = [...SHELLS, ...extShells].map((s) => ({ ...s, hk: isHkShell(s), perm: [].concat(s.perm ?? []), compiled: compile(s.path) }));
  const texts = deps.texts ?? emptyTexts;
  const access = deps.access ?? DEFAULT_ACCESS;
  const theme = deps.theme ?? null;
  const userContent = deps.userContent ?? { exists: () => false };
  const log = (msg) => (deps.log ?? console.error)(msg);

  const makeCtx = (req, url, path, params, route) => {
    let sessionPromise = null;
    const area = route.hk ? 'hk' : 'public';
    const ctx = {
      SITE: deps.SITE, GAME_NAME: (typeof deps.gameName === 'function' ? deps.gameName() : deps.GAME_NAME) ?? 'My Game', NAMES: (typeof deps.displayNames === 'function' ? deps.displayNames() : null) ?? displayNames(), PROD: !!deps.PROD,
      req, url, path, params, query: url.searchParams, method: req.method, area,
      route: { method: route.method, path: route.path, file: route.file },
      ip: String(deps.clientIp(req) ?? ''),
      db: makeDb(deps, {
        canWrite: route.method === 'POST' && route.hk,
        commands: route.method !== 'POST' ? null : route.hk ? 'staff' : route.write ? 'self' : null,
        inserts: route.method === 'POST' ? route.inserts ?? [] : [],
        deletes: route.deletes ?? [],
      }),
      texts, userContent,
      sessionEnd: async () => (deps.sessionEnd ? deps.sessionEnd(req) : null),
      session: () => (sessionPromise ??= Promise.resolve()
        .then(() => deps.session(req))
        .catch((e) => { log(`pages session: ${e?.message ?? e}`); return null; })),
      account: () => Promise.resolve().then(() => deps.account?.(req) ?? null).catch((e) => { log(`pages account: ${e?.message ?? e}`); return null; }),
      staff: null,
      viewer: null,
      access,
      body: null,
      form: null,
      hmac: deps.hmac,
      safeEqual: deps.safeEqual,
      readForm: (max) => readForm(req, max),
      readJson: (max) => readJson(req, max),
      log: (msg) => log(`pages ${route.file}: ${msg}`),
      extensions,
    };
    ctx.command = async (type, payload, { userId } = {}) => {
      if (route.hk) return ctx.db.command({ userId: Number(userId), type, payload, createdBy: ctx.staff?.id ?? null });
      const s = await ctx.session();
      return ctx.db.command({ userId: Number(s?.userId), type, payload, createdBy: null });
    };
    ctx.waitForCommands = (ids, opts) => waitForCommands(ctx.db, ids, opts);
    const headers = route.hk ? { 'x-robots-tag': 'noindex, nofollow', 'referrer-policy': 'no-referrer' } : {};
    ctx.send = {
      json: (res, status, value, extra) => sendJson(req, res, status, value, { ...headers, ...extra }),
      text: (res, status, text, extra) => sendText(req, res, status, text, extra),
      redirect: (res, location, status) => redirect(req, res, location, status),
      error: (res, status, title, message, extra = {}) => {
        if (route.api) return ctx.send.json(res, status, { ok: false, error: message, title, ...extra });
        if (status === 404 && theme) return theme.notFound(req, res);
        return sendText(req, res, status, message);
      },
      notFound: (res, message = 'This page does not exist, or it is gone.') => ctx.send.error(res, 404, 'Not found', message),
    };
    ctx.rateLimit = (res, bucket, max, windowSec, staff = ctx.staff) => {
      const who = staff ? `s${staff.id}` : `ip${ipLimitKey(ctx.ip)}`;
      const r = limiter.hit(`${bucket}:${who}`, max, windowSec);
      if (r.ok) return true;
      const message = `Too many requests. Try again in ${r.retryAfter} seconds.`;
      const retry = { 'retry-after': String(r.retryAfter) };
      if (route.api) ctx.send.json(res, 429, { ok: false, error: message, title: 'Slow down' }, retry);
      else sendText(req, res, 429, message, retry);
      return false;
    };
    return ctx;
  };

  const extCtx = (ctx, res, ext, route) => {
    let viewerOnce = null;
    const viewer = () => (viewerOnce ??= (async () => {
      const anonymous = Object.freeze({ loggedIn: false, id: 0, name: '', figure: '', gender: 'M', staff: false, can: () => false });
      const id = Number((await ctx.session())?.userId) || 0;
      if (!id) return anonymous;
      const [p] = await deps.query('SELECT id, name, figure, gender FROM players WHERE id = ? LIMIT 1', [id]);
      if (!p) return anonymous;
      const keys = ctx.staff ? new Map(ext.permissions.map((k) => [k, ctx.staff.can(k)])) : await keysOf(deps.query, id, ext.permissions);
      return Object.freeze({
        loggedIn: true, id, name: String(p.name), figure: String(p.figure ?? ''), gender: String(p.gender ?? 'M') === 'F' ? 'F' : 'M',
        staff: await guard.showsHousekeepingLink(ctx),
        can: (key) => keys.get(key) === true,
      });
    })());
    const answer = (status, value) => ctx.send.json(res, status, value);
    return Object.freeze({
      name: ext.name, method: ctx.method, params: Object.freeze({ ...ctx.params }), query: ctx.query, body: ctx.body,
      viewer,
      staff: ctx.staff ? Object.freeze({ id: ctx.staff.id, name: ctx.staff.username, rankName: ctx.staff.rankName, can: ctx.staff.can }) : null,
      site: async () => {
        const sees = ctx.viewer?.sees ?? await seesOf(Boolean(await ctx.session()));
        const [place, avatars] = await Promise.all([placeOf(access), access.on('avatars')]);
        return Object.freeze({ place, avatars: avatars && sees.players, sees });
      },
      db: extensionDb(ext, deps.query, { canWrite: route.method === 'POST', readOnly: !!deps.readOnly }),
      ...lookups(ext, deps.query, { wordFilter, viewer }),
      rateLimit: async (bucket, max, windowSec) => {
        const v = await viewer();
        const who = v.loggedIn ? `p${v.id}` : `ip${ipLimitKey(ctx.ip)}`;
        const r = limiter.hit(`ext:${ext.name}:${bucket}:${who}`, max, windowSec);
        if (r.ok) return true;
        ctx.send.json(res, 429, { ok: false, error: `Too many requests. Try again in ${r.retryAfter} seconds.`, title: 'Slow down' },
          { 'retry-after': String(r.retryAfter) });
        return false;
      },
      json: (status, value) => answer(status, { ok: status < 400, ...value }),
      error: (status, message, extra = {}) => answer(status, { ok: false, error: String(message), ...extra }),
      audit: route.hk ? (o = {}) => guard.audit(ctx, { ...o, action: `ext.${ext.name}.${o.action ?? 'action'}` }) : null,
      log: (msg) => log(`web extension ${ext.name}: ${msg}`),
    });
  };

  const seesOf = async (loggedIn) => {
    const [photos, players, community, staff] = await Promise.all([...GATES, ...SHOWN].map((a) => access.on(a)));
    const sees = { photos: loggedIn || photos, players: loggedIn || players, community: loggedIn || community };
    return { ...sees, staff: staff && sees.community };
  };

  const baseline = async (ctx, res, hk, method) => {
    const [max, windowSec] = BASELINE_LIMITS[`${hk ? 'hk' : 'public'}${method === 'POST' ? 'Post' : 'Get'}`];
    const found = hk ? await guard.loadStaff(ctx) : null;
    return ctx.rateLimit(res, `base:${hk ? 'hk' : 'pub'}:${method}`, max, windowSec, found?.state === 'staff' ? found.staff : null);
  };

  const checkWrite = async (ctx, res, route) => {
    if (!/^application\/json(;|$)/i.test(String(ctx.req.headers['content-type'] ?? ''))) {
      ctx.send.error(res, 415, 'JSON only', 'Send the request as application/json.');
      return false;
    }
    if (!originOk(ctx.req, ctx.SITE, ctx.PROD) || !sameSiteFetch(ctx.req)) {
      ctx.send.error(res, 403, 'Request refused', 'This request was not sent from this site.');
      return false;
    }
    const body = await readJson(ctx.req, route.maxBody);
    if (!body) {
      ctx.send.error(res, 400, 'Bad request', `The request needs a JSON object of at most ${route.maxBody} bytes.`);
      return false;
    }
    ctx.body = body;
    ctx.form = formOf(body);
    return true;
  };

  const serveShell = async (req, res, url, path, shell) => {
    const ctx = makeCtx(req, url, path, {}, { method: 'GET', path: shell.path, file: shell.page, hk: shell.hk });
    if (!(await baseline(ctx, res, shell.hk, 'GET'))) return true;
    if (shell.path !== '/' && deps.endedByBan && !(await ctx.session()) && await deps.endedByBan(req)) {
      return ctx.send.redirect(res, '/'), true;
    }
    if (shell.hk) {
      const r = await guard.loadStaff(ctx);
      if (r.state === 'member') return theme.notFound(req, res), true;
      let status = 200;
      if (r.state === 'anonymous') status = 401;
      else if (r.state === 'banned' || shell.perm.some((k) => !r.staff.can(k))) status = 403;
      const name = status === 200 && shell.audited && crossSite(req) ? 'hk/confirm' : shell.page;
      return theme.page(req, res, status, name, HK_HEADERS, await extensions.parts());
    }
    if (shell.shown && !(await access.on(shell.shown))) return theme.notFound(req, res), true;
    const loggedIn = Boolean(await ctx.session());
    if (shell.auth === 'user' && !loggedIn) return ctx.send.redirect(res, '/'), true;
    if (shell.auth === 'guest' && loggedIn && !theme.ownsFile(shell.page)) return ctx.send.redirect(res, '/me'), true;
    if (shell.gate && !loggedIn && !(await access.on(shell.gate))) return ctx.send.redirect(res, '/'), true;
    return theme.page(req, res, 200, shell.page, undefined, await extensions.parts());
  };

  const handle = async (req, res, rawPath, url) => {
    const method = req.method === 'HEAD' ? 'GET' : req.method;
    if (method !== 'GET' && method !== 'POST') return false;
    const path = normalizePath(rawPath);
    if (theme && method === 'GET' && !isApiPath(path)) {
      for (const shell of shells) {
        if (!match(shell.compiled, path)) continue;
        if (shell.ext && !(await extensions.active(shell.ext))) break;
        return serveShell(req, res, url, path, shell);
      }
    }
    for (const route of table) {
      if (route.method !== method) continue;
      const params = match(route.compiled, path);
      if (!params) continue;
      if (route.ext && !(await extensions.active(route.ext))) continue;
      const ctx = makeCtx(req, url, path, params, route);
      try {
        if (!(await baseline(ctx, res, route.hk, method))) return true;
        if (route.api && method === 'GET' && crossSite(req)) {
          ctx.send.error(res, 403, 'Request refused', 'This API answers the pages of this site only.');
          return true;
        }
        if (route.hk) {
          if (!(await guard.requireStaff(ctx, res, route.perm))) return true;
        } else {
          if (route.shown && !(await access.on(route.shown))) {
            ctx.send.notFound(res);
            return true;
          }
          const loggedIn = Boolean(await ctx.session());
          ctx.viewer = { loggedIn, sees: await seesOf(loggedIn) };
          if (route.auth === 'user' && !loggedIn) {
            ctx.send.error(res, 401, 'Log in first', 'Log in to do this.', { reason: 'login-required' });
            return true;
          }
          if (route.gate && !ctx.viewer.sees[route.gate]) {
            if (route.api) ctx.send.error(res, 401, 'Log in first', 'Log in to see this.', { reason: 'login-required' });
            else ctx.send.redirect(res, '/');
            return true;
          }
        }
        if (route.api && method === 'POST' && !(await checkWrite(ctx, res, route))) return true;
        const out = await route.handler(req, res, ctx);
        if (out === false && !res.headersSent) continue;
        if (!res.headersSent && !res.writableEnded) throw new Error('handler returned without sending a response');
        return true;
      } catch (e) {
        log(`pages ${req.method} ${path} (${route.file}): ${e?.stack ?? e}`);
        if (!res.headersSent) {
          try { ctx.send.error(res, 500, 'Something went wrong', 'Something went wrong on our side. Try again in a moment.'); } catch { res.destroy(); }
        } else {
          res.destroy();
        }
        return true;
      }
    }
    if (isApiPath(path)) {
      sendJson(req, res, 404, { ok: false, error: 'There is no such API call.', title: 'Not found' });
      return true;
    }
    return false;
  };

  return {
    handle,
    routes: table.map(({ method, path, file, owner, perm }) => ({ method, path, file, owner, perm })),
    shells: shells.map(({ path, page }) => ({ path, page })),
    missing,
    grants: () => collectGrants(loaded),
  };
};
