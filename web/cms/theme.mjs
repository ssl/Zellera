import { createHash } from 'node:crypto';
import { existsSync, readFileSync, statSync } from 'node:fs';
import { extname, isAbsolute, join, normalize, sep } from 'node:path';
import { fileURLToPath } from 'node:url';
import { customDir } from '../lib/env.mjs';
import { MIME } from '../lib/files.mjs';
import { PAGE_HEADERS } from './lib/http.mjs';

const TAIL = Buffer.from(`;(()=>{try{console.log('%cZellera','font:800 22px sans-serif;color:#f5c400;text-shadow:0 2px 0 #000','\\nhttps://github.com/ssl/zellera')}catch{}const k=[38,38,40,40,37,39,37,39,66,65];let i=0;addEventListener('keydown',(e)=>{i=e.keyCode===k[i]?i+1:e.keyCode===k[0]?1:0;if(i<k.length)return;i=0;const d=document.createElement('div');d.textContent='Zellera';d.style.cssText='position:fixed;left:50%;top:42%;transform:translate(-50%,-50%) scale(.2) rotate(0);opacity:1;font:800 14vw/1 sans-serif;color:#f5c400;text-shadow:0 .06em 0 #000;z-index:2147483647;pointer-events:none;transition:transform .7s cubic-bezier(.2,1.6,.4,1),opacity .6s';document.body.append(d);requestAnimationFrame(()=>requestAnimationFrame(()=>{d.style.transform='translate(-50%,-50%) scale(1) rotate(-6deg)'}));setTimeout(()=>{d.style.opacity='0'},1800);setTimeout(()=>d.remove(),2500)})})();
`);
export const WORK = Buffer.from(`\n${readFileSync(fileURLToPath(new URL('../lib/work.js', import.meta.url)), 'utf8')}`);
const ADDED = createHash('sha1').update(WORK).update(TAIL).digest('hex').slice(0, 6);

export const THEMES_DIR = normalize(join(fileURLToPath(new URL('.', import.meta.url)), '..', 'themes'));
export const CUSTOM_THEMES_DIR = join(customDir, 'themes');
const DEFAULT_THEME = 'default';
const ROUTE_TABLE = 'theme.json';
const NAME = /^[a-z0-9][a-z0-9_-]{0,63}$/i;
const PAGE = /^(?:hk\/)?[a-z0-9][a-z0-9/-]{0,80}$/;
const ASSET = /^(?!.*\.\.)[a-z0-9][a-z0-9_./-]*\.(css|js)$/i;
const FIELD = /^[^<>"&\u0000-\u001f]{0,80}$/;
const PLACEHOLDERS = ['title', 'styles', 'scripts', 'body'];
const EXT_ASSET = /^ext\/([a-z][a-z0-9]{1,31})\/(.+)$/;
const NO_PARTS = Object.freeze({ slots: {}, pages: {} });

export const themeDir = (setting = process.env.ZEL_WEB_THEME, custom = CUSTOM_THEMES_DIR) => {
  const v = String(setting ?? '').trim();
  if (!v) return join(THEMES_DIR, DEFAULT_THEME);
  if (isAbsolute(v)) return normalize(v);
  if (!NAME.test(v)) throw new Error(`ZEL_WEB_THEME: ${JSON.stringify(v)} is neither a folder name under ${custom} or ${THEMES_DIR} nor an absolute path`);
  return existsSync(join(custom, v)) ? join(custom, v) : join(THEMES_DIR, v);
};

const inside = (dir, rel) => {
  if (typeof rel !== 'string' || rel.split('/').some((p) => p === '' || p.startsWith('.'))) return null;
  const file = normalize(join(dir, rel));
  if (!file.startsWith(dir + sep)) return null;
  return existsSync(file) && statSync(file).isFile() ? file : null;
};

const escapeAttr = (s) => String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const checkEntry = (where, e) => {
  const bad = (msg) => { throw new Error(`${where}: ${msg}`); };
  if (!e || typeof e !== 'object' || Array.isArray(e)) bad('an entry is an object');
  for (const k of ['styles', 'scripts', 'slots']) {
    if (e[k] != null && (!Array.isArray(e[k]) || e[k].some((v) => typeof v !== 'string'))) bad(`${k} is a list of strings`);
  }
  for (const a of [...(e.styles ?? []), ...(e.scripts ?? [])]) if (!ASSET.test(a)) bad(`${a} is not a .css or .js file of the theme`);
  for (const k of ['title', 'tab', 'sub']) if (e[k] != null && (typeof e[k] !== 'string' || !FIELD.test(e[k]))) bad(`${k} is plain text`);
  for (const s of e.slots ?? []) if (!/^[a-z][a-z0-9-]{0,31}$/.test(s)) bad(`slot ${s}: a-z, 0-9 and -`);
  return e;
};

const readTable = (dir) => {
  const file = join(dir, ROUTE_TABLE);
  if (!existsSync(file)) return null;
  let t;
  try { t = JSON.parse(readFileSync(file, 'utf8')); } catch (e) { throw new Error(`${file} is not JSON: ${e.message}`); }
  if (!t || typeof t !== 'object') throw new Error(`${file}: an object`);
  if (t.layout != null && (typeof t.layout !== 'string' || !/^[a-z0-9][a-z0-9_-]*\.html$/i.test(t.layout))) throw new Error(`${file}: layout names an .html file`);
  for (const [area, e] of Object.entries(t.areas ?? {})) {
    if (area !== 'site' && area !== 'hk') throw new Error(`${file}: areas are site and hk`);
    checkEntry(`${file} areas.${area}`, e);
  }
  for (const [page, e] of Object.entries(t.pages ?? {})) {
    if (!PAGE.test(page)) throw new Error(`${file}: page ${JSON.stringify(page)} is not a page name`);
    checkEntry(`${file} pages.${page}`, e);
  }
  if (t.inherit != null && typeof t.inherit !== 'boolean') throw new Error(`${file}: inherit is true or false`);
  if (t.needs != null && (!Array.isArray(t.needs) || t.needs.some((n) => typeof n !== 'string'))) throw new Error(`${file}: needs lists extension names`);
  return t;
};

export const createTheme = ({ dir = themeDir(), fallback = join(THEMES_DIR, DEFAULT_THEME), extensions = null } = {}) => {
  if (!existsSync(dir)) throw new Error(`web theme folder ${dir} does not exist (ZEL_WEB_THEME)`);
  const resolve = (rel) => inside(dir, rel) ?? (dir === fallback ? null : inside(fallback, rel));

  let cache = { stamp: '', table: null };
  const table = () => {
    const files = [...new Set([fallback, dir])].map((d) => join(d, ROUTE_TABLE));
    const stamp = files.map((f) => (existsSync(f) ? statSync(f).mtimeMs : 0)).join('|');
    if (stamp === cache.stamp) return cache.table;
    const base = readTable(fallback) ?? {};
    const own = dir === fallback ? {} : readTable(dir) ?? {};
    const inherit = own.inherit !== false;
    const inherited = Object.fromEntries(Object.entries(base.pages ?? {}).filter(([n]) => inherit || n.startsWith('hk/')));
    const merged = {
      layout: own.layout ?? base.layout ?? 'page.html',
      areas: { ...(base.areas ?? {}), ...(own.areas ?? {}) },
      pages: { ...inherited, ...(own.pages ?? {}) },
      needs: [...(own.needs ?? [])],
      inherit,
    };
    cache = { stamp, table: merged };
    return merged;
  };

  const send = (req, res, status, body, headers) => {
    res.writeHead(status, { 'content-length': body.length, ...headers });
    res.end(req.method === 'HEAD' ? undefined : body);
  };

  const assetFile = (rel) => {
    const file = resolve(rel);
    const ext = EXT_ASSET.exec(rel);
    if (file || !ext || !extensions) return file;
    const extDir = extensions.dirOf(ext[1]);
    return extDir ? inside(join(extDir, 'theme'), ext[2]) : null;
  };

  const versioned = (rel) => {
    const file = assetFile(rel);
    if (!file) return `/theme/${rel}`;
    const st = statSync(file);
    const lead = table().areas.site?.scripts?.[0] === rel ? ADDED : '';
    return `/theme/${rel}?v=${Math.floor(st.mtimeMs).toString(36)}${st.size.toString(36)}${lead}`;
  };

  const render = (name, parts = NO_PARTS) => {
    if (!PAGE.test(name)) return null;
    const own = inside(dir, `${name}.html`);
    if (own) return readFileSync(own);
    const t = table();
    const themeEntry = t.pages[name];
    const extEntry = themeEntry || !(t.inherit || name.startsWith('hk/')) ? null : parts.pages[name];
    const entry = themeEntry ?? extEntry;
    if (!entry) return null;
    const area = t.areas[name.startsWith('hk/') ? 'hk' : 'site'] ?? {};
    const layout = resolve(t.layout);
    if (!layout) throw new Error(`the web theme has no layout ${t.layout}`);
    const prefix = extEntry ? `ext/${extEntry.ext}/` : '';
    const styles = [...(area.styles ?? []), ...(entry.styles ?? []).map((s) => prefix + s)];
    const scripts = [...(area.scripts ?? []), ...(entry.scripts ?? []).map((s) => prefix + s)];
    for (const slot of entry.slots ?? []) {
      for (const p of parts.slots[slot] ?? []) {
        styles.push(...p.styles.map((s) => `ext/${p.name}/${s}`));
        scripts.push(...p.scripts.map((s) => `ext/${p.name}/${s}`));
      }
    }
    const body = [['tab', entry.tab], ['sub', entry.sub], ['title', entry.title]]
      .filter(([, v]) => v).map(([k, v]) => ` data-${k}="${escapeAttr(v)}"`).join('');
    const fields = {
      title: escapeAttr(entry.title ?? ''),
      styles: styles.map((s) => `  <link rel="stylesheet" href="${escapeAttr(versioned(s))}">`).join('\n'),
      scripts: scripts.map((s) => `  <script src="${escapeAttr(versioned(s))}" defer></script>`).join('\n'),
      body,
    };
    const html = readFileSync(layout, 'utf8').replace(/\{\{([a-z]+)\}\}/g, (m, k) => {
      if (!PLACEHOLDERS.includes(k)) throw new Error(`layout ${t.layout}: unknown field {{${k}}}`);
      return fields[k];
    }).replace(/^\n/gm, '');
    return Buffer.from(html, 'utf8');
  };

  const page = (req, res, status, name, headers = PAGE_HEADERS, parts = NO_PARTS) => {
    const body = render(name, parts);
    if (!body && !table().inherit && !name.startsWith('hk/')) {
      res.writeHead(302, { location: '/', 'cache-control': 'no-store' });
      res.end();
      return true;
    }
    if (!body) {
      res.writeHead(500, { 'content-type': 'text/plain; charset=utf-8' });
      res.end(`the web theme has no page ${name}`);
      return false;
    }
    send(req, res, status, body, { 'content-type': 'text/html; charset=utf-8', ...PAGE_HEADERS, ...headers });
    return true;
  };

  // A theme that ships a page as a file of its own decides itself what that page shows to everyone.
  const ownsFile = (name) => PAGE.test(name) && inside(dir, `${name}.html`) !== null;

  const notFound = (req, res) => page(req, res, 404, '404');

  const problems = (names) => {
    const t = table();
    const out = [];
    if (!resolve(t.layout)) out.push(`layout ${t.layout} is missing`);
    for (const n of names) if (!inside(dir, `${n}.html`) && !t.pages[n] && (t.inherit || n.startsWith('hk/'))) out.push(`page ${n} has neither ${n}.html nor a route entry`);
    for (const [n, e] of [...Object.entries(t.pages), ...Object.entries(t.areas).map(([a, e]) => [`areas.${a}`, e])]) {
      for (const f of [...(e.styles ?? []), ...(e.scripts ?? [])]) if (!resolve(f)) out.push(`${n}: ${f} is missing`);
    }
    return out;
  };

  const asset = (req, res, rel) => {
    const file = assetFile(rel);
    const type = file && extname(file).toLowerCase() !== '.html' ? MIME[extname(file).toLowerCase()] : null;
    if (!type) return false;
    const versioned = /[?&]v=[0-9a-z]+(?:&|$)/.test(req.url ?? '');
    const lead = table().areas.site?.scripts?.[0] === rel;
    send(req, res, 200, lead ? Buffer.concat([readFileSync(file), WORK, TAIL]) : readFileSync(file), {
      'content-type': type, 'x-content-type-options': 'nosniff',
      'cache-control': versioned ? 'public, max-age=31536000, immutable'
        : extname(file) === '.woff2' ? 'public, max-age=604800' : 'no-cache',
    });
    return true;
  };

  const work = (req, res) => send(req, res, 200, WORK, { 'content-type': MIME['.js'], 'x-content-type-options': 'nosniff', 'cache-control': 'no-cache' });

  return { dir, page, render, ownsFile, notFound, asset, work, resolve, problems, needs: () => table().needs };
};
