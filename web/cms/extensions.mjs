import { createHash } from 'node:crypto';
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { basename, isAbsolute, join, normalize, sep } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import { VERSION as ZEL_VERSION, customDir } from '../lib/env.mjs';
import { compile } from './lib/router.mjs';
import { Sql, sql } from './lib/sql.mjs';
import { HK_API } from '../hk-api/index.mjs';
import { HK_PERMS } from '../hk-api/_guard.mjs';

export const EXTENSIONS_DIR = normalize(join(fileURLToPath(new URL('.', import.meta.url)), '..', 'extensions'));
export const CUSTOM_EXTENSIONS_DIR = join(customDir, 'extensions');
export const EXT_API = '/api/ext';
const HK_EXT_API = `${HK_API}/ext`;
export const EXTENSIONS_SETTING = Object.freeze({ key: 'site.extensions', env: 'ZEL_WEB_EXTENSIONS', list: 'guestbook' });
export const MIGRATIONS_TABLE = 'ext_migrations';

const EXT_NAME = /^[a-z][a-z0-9]{1,31}$/;
const TABLE = /^[a-z][a-z0-9_]{0,39}$/;
const VERSION = /^\d{1,4}\.\d{1,4}\.\d{1,6}$/;
const ASSET = /^(?!.*\.\.)[a-z0-9][a-z0-9_./-]*\.(css|js)$/i;
const SLOT = /^[a-z][a-z0-9-]{0,31}$/;
const MIGRATION = /^V([1-9]\d{0,5})__([a-z0-9_]{1,80})\.sql$/;
const GATES = ['photos', 'players', 'community'];
export const LOOKUPS = Object.freeze(['players', 'blocks', 'filter']);
const NO_FILTER = 'chat.unfiltered';

export const LOOKUP_GRANTS = Object.freeze([
  { table: 'players', select: ['id', 'name', 'figure', 'gender', 'rank_id'] },
  { table: 'player_preferences', select: ['player_id', 'hide_profile'] },
  { table: 'player_blocks', select: ['player_id', 'blocked_id'] },
  { table: 'rank_permissions', select: ['rank_id', 'permission_code'] },
  { table: 'word_filters', select: ['word', 'replacement'] },
  { table: 'settings', select: ['name', 'value'] },
]);

export class ExtensionError extends Error {}

export const parseList = (raw, custom = CUSTOM_EXTENSIONS_DIR) => {
  const out = new Map();
  for (const entry of String(raw ?? '').split(',').map((s) => s.trim()).filter(Boolean)) {
    let dir;
    if (EXT_NAME.test(entry)) dir = existsSync(join(custom, entry)) ? join(custom, entry) : join(EXTENSIONS_DIR, entry);
    else if (isAbsolute(entry) && !/[\s,]/.test(entry)) dir = normalize(entry).replace(/[/\\]+$/, '');
    else throw new ExtensionError(`${EXTENSIONS_SETTING.env}: ${JSON.stringify(entry)} is neither an extension name (a-z, 0-9) nor an absolute folder`);
    const name = basename(dir);
    if (!EXT_NAME.test(name)) throw new ExtensionError(`${EXTENSIONS_SETTING.env}: the folder ${dir} has no valid extension name (a-z, 0-9, 2 to 32 characters)`);
    if (out.has(name) && out.get(name) !== dir) throw new ExtensionError(`${EXTENSIONS_SETTING.env}: two extensions are called ${name}`);
    out.set(name, dir);
  }
  return [...out].map(([name, dir]) => ({ name, dir }));
};

export const namesOf = (raw) => {
  try { return new Set(parseList(raw).map((e) => e.name)); } catch { return new Set(); }
};

const RANGE_PART = /^(>=|<=|>|<|=)?(\d{1,4})\.(\d{1,4})\.(\d{1,6})$/;
const compareVersions = (a, b) => a.map((n, i) => n - b[i]).find((d) => d !== 0) ?? 0;
const HOLDS = { '>=': (d) => d >= 0, '<=': (d) => d <= 0, '>': (d) => d > 0, '<': (d) => d < 0, '=': (d) => d === 0 };

export const versionInRange = (version, range) => {
  const running = RANGE_PART.exec(version)?.slice(2).map(Number);
  const parts = String(range).trim().split(/\s+/).map((p) => RANGE_PART.exec(p));
  if (!running || parts.some((p) => !p)) return null;
  return parts.every((p) => HOLDS[p[1] ?? '='](compareVersions(running, p.slice(2).map(Number))));
};

const fail = (name, msg) => { throw new ExtensionError(`web extension ${name}: ${msg}`); };
const assets = (name, where, list) => {
  if (list == null) return [];
  if (!Array.isArray(list) || list.some((a) => typeof a !== 'string' || !ASSET.test(a))) fail(name, `${where} must list .css or .js files inside the extension`);
  return [...list];
};
const text = (name, where, v, max) => {
  if (v == null) return '';
  if (typeof v !== 'string' || v.length > max || /[<>\u0000-\u001f]/.test(v)) fail(name, `${where} must be plain text of at most ${max} characters`);
  return v;
};

const pageEntry = (name, pageName, e) => {
  const hk = pageName.startsWith('hk/');
  const own = hk ? `hk/ext/${name}` : `ext/${name}`;
  if (pageName !== own && !pageName.startsWith(`${own}/`)) fail(name, `page ${pageName}: its name starts with ${own}`);
  if (!e || typeof e !== 'object') fail(name, `page ${pageName} must be an object`);
  const base = hk ? `/housekeeping/ext/${name}` : `/ext/${name}`;
  if (typeof e.path !== 'string' || (e.path !== base && !e.path.startsWith(`${base}/`))) fail(name, `page ${pageName}: path starts with ${base}`);
  try { compile(e.path); } catch (err) { fail(name, `page ${pageName}: ${err.message}`); }
  if (hk && !HK_PERMS.includes(e.perm)) fail(name, `page ${pageName}: a staff page needs perm, one of ${HK_PERMS.join(', ')}`);
  if (!hk && e.perm != null) fail(name, `page ${pageName}: perm is for staff pages`);
  if (e.gate != null && (hk || !GATES.includes(e.gate))) fail(name, `page ${pageName}: gate is ${GATES.join(', ')} and only on site pages`);
  if (e.auth != null && (hk || e.auth !== 'user')) fail(name, `page ${pageName}: auth is 'user' and only on site pages`);
  return Object.freeze({
    path: e.path, perm: e.perm ?? null, gate: e.gate ?? null, auth: e.auth ?? null,
    title: text(name, `page ${pageName} title`, e.title, 60), nav: text(name, `page ${pageName} nav`, e.nav, 30),
    tab: text(name, `page ${pageName} tab`, e.tab ?? (hk ? `ext-${name}` : ''), 32), sub: text(name, `page ${pageName} sub`, e.sub, 32),
    styles: assets(name, `page ${pageName} styles`, e.styles), scripts: assets(name, `page ${pageName} scripts`, e.scripts),
  });
};

export const readManifest = (dir, running = ZEL_VERSION) => {
  const name = basename(dir);
  const file = join(dir, 'extension.json');
  if (!existsSync(file)) fail(name, `no extension.json in ${dir}`);
  let m;
  try { m = JSON.parse(readFileSync(file, 'utf8')); } catch (e) { fail(name, `extension.json is not JSON (${e.message})`); }
  if (m?.name !== name) fail(name, `extension.json names ${JSON.stringify(m?.name)}, its folder ${name}`);
  if (typeof m.version !== 'string' || !VERSION.test(m.version)) fail(name, 'version is <major>.<minor>.<patch>');
  if (m.zel != null) {
    const fits = typeof m.zel === 'string' ? versionInRange(running, m.zel) : null;
    if (fits === null) fail(name, 'zel is a version range such as ">=1.0.0" or ">=1.0.0 <2.0.0"');
    if (!fits) fail(name, `it is made for version ${m.zel}, this is ${running}`);
  }
  const needs = m.needs ?? {};
  const tables = needs.tables ?? [];
  if (!Array.isArray(tables) || tables.some((t) => typeof t !== 'string' || !TABLE.test(t))) fail(name, 'needs.tables lists table names (a-z, 0-9, _)');
  const lookups = needs.lookups ?? [];
  if (!Array.isArray(lookups) || lookups.some((l) => !LOOKUPS.includes(l))) fail(name, `needs.lookups is a list of ${LOOKUPS.join(', ')}`);
  const permissions = needs.permissions ?? [];
  if (!Array.isArray(permissions) || permissions.some((p) => !HK_PERMS.includes(p))) fail(name, `needs.permissions is a list of ${HK_PERMS.join(', ')}`);
  if (m.server != null && (typeof m.server !== 'string' || !/^[a-z0-9][a-z0-9_-]*\.mjs$/i.test(m.server))) fail(name, 'server names an .mjs file in the folder');
  const theme = m.theme ?? {};
  const slots = {};
  for (const [slot, parts] of Object.entries(theme.slots ?? {})) {
    if (!SLOT.test(slot)) fail(name, `slot ${JSON.stringify(slot)}: a-z, 0-9 and -`);
    slots[slot] = Object.freeze({ styles: assets(name, `slot ${slot} styles`, parts?.styles), scripts: assets(name, `slot ${slot} scripts`, parts?.scripts) });
  }
  const pages = {};
  for (const [pageName, e] of Object.entries(theme.pages ?? {})) pages[pageName] = pageEntry(name, pageName, e);
  return Object.freeze({
    name, version: m.version, description: text(name, 'description', m.description, 300), server: m.server ?? null,
    tables: Object.freeze(tables.map((t) => `ext_${name}_${t}`)), lookups: Object.freeze([...lookups]), permissions: Object.freeze([...permissions]),
    slots: Object.freeze(slots), pages: Object.freeze(pages),
  });
};

const REFS = /\b(?:FROM|JOIN|INTO|UPDATE|TABLE|REFERENCES)\s+(?:IF\s+(?:NOT\s+)?EXISTS\s+)?(`?)([^\s`(),;]+)\1/gi;
const INDEX_ON = /^\s*CREATE\s+(?:UNIQUE\s+)?INDEX\s+\S+\s+ON\s+(`?)([^\s`(]+)\1/i;
const PARENTHESISED = /\b(?:FROM|JOIN|INTO|UPDATE|TABLE|REFERENCES)\s*\((?!\s*(?:SELECT|WITH)\b)/i;
const QUOTED_OR_COMMENT = /'(?:[^'\\]|\\.|'')*'|"(?:[^"\\]|\\.|"")*"|`[^`]*`|\/\*[\s\S]*?\*\/|--(?=\s|$)[^\n]*|#[^\n]*/g;
const COMMA_JOIN = /\bFROM\s+`?[a-z0-9_]+`?(?:\s+(?:AS\s+)?[a-z0-9_]+)?\s*,/i;

export const checkTables = (name, sqlText, tables) => {
  const text = sqlText.replace(QUOTED_OR_COMMENT, (part) => (/^['"`]/.test(part) ? part : ' '));
  if (/\/\*/.test(text.replace(QUOTED_OR_COMMENT, ''))) fail(name, 'a comment is left open');
  const bare = text.replace(/'(?:[^'\\]|\\.|'')*'|"(?:[^"\\]|\\.|"")*"/g, "''");
  if (bare.includes(';')) fail(name, 'one statement at a time (no ;)');
  if (COMMA_JOIN.test(bare)) fail(name, 'join tables with JOIN, not a comma');
  if (PARENTHESISED.test(bare)) fail(name, 'name a table without parentheses around it');
  const refs = [...bare.matchAll(REFS)].map((m) => m[2]);
  const index = INDEX_ON.exec(bare);
  if (index) refs.push(index[2]);
  for (const t of refs) if (!tables.includes(t)) fail(name, `SQL may only name its own tables (${tables.join(', ') || 'none declared'}), not ${t}`);
  return text;
};

const READ = /^\s*(SELECT|WITH)\b/i;
const WRITE = /^\s*(INSERT|UPDATE|DELETE)\b/i;
const DDL = /^\s*(CREATE\s+TABLE|CREATE\s+(UNIQUE\s+)?INDEX|ALTER\s+TABLE|DROP\s+TABLE|DROP\s+INDEX|INSERT|UPDATE|DELETE)\b/i;

export const extensionDb = (ext, query, { canWrite = false, readOnly = false } = {}) => {
  const run = (q, kind) => {
    if (!(q instanceof Sql)) fail(ext.name, `db.${kind} takes sql\`…\` only`);
    return query(checkTables(ext.name, q.text, ext.tables), q.params);
  };
  return Object.freeze({
    query: async (q) => {
      if (!READ.test(q?.text ?? '')) fail(ext.name, 'db.query reads (SELECT); use db.write for changes');
      return run(q, 'query');
    },
    write: async (q) => {
      if (!WRITE.test(q?.text ?? '')) fail(ext.name, 'db.write is INSERT, UPDATE or DELETE');
      if (!canWrite) fail(ext.name, 'db.write only in a POST route');
      if (readOnly) fail(ext.name, 'this server runs read-only');
      return run(q, 'write');
    },
  });
};

export const migrationFiles = (ext, dir) => {
  const folder = join(dir, 'migrations');
  if (!existsSync(folder)) return [];
  const files = readdirSync(folder).filter((f) => f.endsWith('.sql')).map((f) => {
    const m = MIGRATION.exec(f);
    if (!m) fail(ext.name, `migration ${f}: name it V<number>__<words>.sql`);
    const body = readFileSync(join(folder, f), 'utf8');
    return { version: Number(m[1]), file: f, body, checksum: createHash('sha256').update(body).digest('hex') };
  }).sort((a, b) => a.version - b.version);
  files.forEach((f, i) => { if (i && files[i - 1].version === f.version) fail(ext.name, `two migrations are V${f.version}`); });
  return files;
};

export const statements = (ext, body) => body.split('\n').filter((l) => !/^\s*--/.test(l)).join('\n')
  .split(/;[ \t]*(?:\n|$)/).map((s) => s.trim()).filter(Boolean)
  .map((s) => {
    if (!DDL.test(s)) fail(ext.name, `a migration creates, changes or fills its tables only: ${s.slice(0, 60)}`);
    return checkTables(ext.name, s, ext.tables);
  });

const MIGRATIONS_DDL = `CREATE TABLE IF NOT EXISTS ${MIGRATIONS_TABLE} (
  extension VARCHAR(32) NOT NULL, version INT NOT NULL, name VARCHAR(120) NOT NULL, checksum CHAR(64) NOT NULL,
  applied_at INT UNSIGNED NOT NULL, PRIMARY KEY (extension, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='The SQL migrations of the web extensions.'`;

export const migrate = async (ext, dir, query, { readOnly = false, log = () => {} } = {}) => {
  const files = migrationFiles(ext, dir);
  if (!files.length) return 0;
  if (!readOnly) await query(MIGRATIONS_DDL);
  const applied = new Map((await query(`SELECT version, name, checksum FROM ${MIGRATIONS_TABLE} WHERE extension = ?`, [ext.name]))
    .map((r) => [Number(r.version), r]));
  for (const [version, row] of applied) {
    const f = files.find((x) => x.version === version);
    if (!f) fail(ext.name, `migration V${version} (${row.name}) is applied but its file is gone`);
    if (f.checksum !== row.checksum) fail(ext.name, `migration ${f.file} changed after it was applied; add a new migration instead`);
  }
  let n = 0;
  for (const f of files.filter((x) => !applied.has(x.version))) {
    if (readOnly) fail(ext.name, `migration ${f.file} is pending and this server runs read-only`);
    for (const s of statements(ext, f.body)) await query(s);
    await query(`INSERT INTO ${MIGRATIONS_TABLE} (extension, version, name, checksum, applied_at) VALUES (?, ?, ?, ?, UNIX_TIMESTAMP())`,
      [ext.name, f.version, f.file, f.checksum]);
    log(`web extension ${ext.name}: applied ${f.file}`);
    n += 1;
  }
  return n;
};

const routeOf = (ext, area, method, path, opts, handler) => {
  if (typeof opts === 'function') [opts, handler] = [{}, opts];
  opts ??= {};
  if (typeof handler !== 'function') fail(ext.name, `${method} ${path}: no handler`);
  if (typeof path !== 'string' || !path.startsWith('/')) fail(ext.name, `${method} ${path}: a path starts with /`);
  const full = `${area === 'hk' ? HK_EXT_API : EXT_API}/${ext.name}${path === '/' ? '' : path}`;
  try { compile(full); } catch (e) { fail(ext.name, e.message); }
  if (area === 'hk') {
    if (!HK_PERMS.includes(opts.perm)) fail(ext.name, `${method} ${full}: a staff route needs perm, one of ${HK_PERMS.join(', ')}`);
    if (opts.gate != null || opts.auth != null) fail(ext.name, `${method} ${full}: gate and auth are for site routes`);
  } else {
    if (opts.perm != null) fail(ext.name, `${method} ${full}: perm is for staff routes (ext.hk)`);
    if (opts.gate != null && (method !== 'GET' || !GATES.includes(opts.gate))) fail(ext.name, `${method} ${full}: gate is ${GATES.join(', ')}, on GET routes`);
    if (opts.auth != null && opts.auth !== 'user') fail(ext.name, `${method} ${full}: auth is 'user'`);
  }
  if (opts.maxBody != null && (method !== 'POST' || !Number.isInteger(opts.maxBody) || opts.maxBody < 1 || opts.maxBody > 16384)) {
    fail(ext.name, `${method} ${full}: maxBody is 1..16384, on POST routes`);
  }
  return Object.freeze({
    method, path: full, handler, hk: area === 'hk', perm: opts.perm ?? null, gate: opts.gate ?? null, auth: opts.auth ?? null,
    maxBody: opts.maxBody ?? null, audited: false,
  });
};

const registrar = (ext, routes) => {
  const add = (area, method) => (path, opts, handler) => { routes.push(routeOf(ext, area, method, path, opts, handler)); };
  return Object.freeze({
    name: ext.name, version: ext.version, sql,
    get: add('site', 'GET'), post: add('site', 'POST'),
    hk: Object.freeze({ get: add('hk', 'GET'), post: add('hk', 'POST') }),
  });
};

export const loadExtensions = async ({ list, query, readOnly = false, log = () => {} }) => {
  const loaded = [];
  for (const { name, dir } of list) {
    if (!existsSync(dir) || !statSync(dir).isDirectory()) fail(name, `no folder ${dir}`);
    const manifest = readManifest(dir);
    const routes = [];
    if (manifest.server) {
      const file = normalize(join(dir, manifest.server));
      if (!file.startsWith(dir + sep) || !existsSync(file)) fail(name, `no server module ${manifest.server}`);
      const mod = await import(pathToFileURL(file).href);
      if (typeof mod.default !== 'function') fail(name, `${manifest.server} has no default export function (register)`);
      await mod.default(registrar(manifest, routes));
    }
    const seen = new Set();
    for (const r of routes) {
      const key = `${r.method} ${r.path}`;
      if (seen.has(key)) fail(name, `${key} is registered twice`);
      seen.add(key);
    }
    try {
      await migrate(manifest, dir, query, { readOnly, log });
    } catch (e) {
      if (e instanceof ExtensionError) throw e;
      log(`web extension ${name} is not loaded: its migrations could not run (${e?.message ?? e}); restart the web once the database answers`);
      continue;
    }
    loaded.push(Object.freeze({ ...manifest, dir, routes: Object.freeze(routes) }));
    log(`web extension ${name} ${manifest.version}: ${routes.length} routes${manifest.tables.length ? `, tables ${manifest.tables.join(', ')}` : ''}`);
  }
  return loaded;
};

export const createExtensions = (loaded, { setting = async () => undefined } = {}) => {
  const byName = new Map(loaded.map((e) => [e.name, e]));
  const activeNames = async () => {
    let raw;
    try { raw = await setting(); } catch { return new Set(byName.keys()); }
    if (raw === undefined) return new Set(byName.keys());
    const on = namesOf(raw);
    return new Set([...byName.keys()].filter((n) => on.has(n)));
  };
  return Object.freeze({
    list: Object.freeze([...loaded]),
    get: (name) => byName.get(name) ?? null,
    active: async (name) => (await activeNames()).has(name),
    parts: async () => {
      const on = await activeNames();
      const slots = {};
      const pages = {};
      for (const e of loaded) {
        if (!on.has(e.name)) continue;
        for (const [slot, p] of Object.entries(e.slots)) (slots[slot] ??= []).push({ name: e.name, ...p });
        for (const [page, p] of Object.entries(e.pages)) pages[page] = { ext: e.name, ...p };
      }
      return { slots, pages };
    },
    dirOf: (name) => byName.get(name)?.dir ?? null,
    hkNav: async (can) => {
      const on = await activeNames();
      return loaded.filter((e) => on.has(e.name)).flatMap((e) => Object.entries(e.pages)
        .filter(([page, p]) => page.startsWith('hk/') && p.nav && can(p.perm) && !/[:*]/.test(p.path))
        .map(([, p]) => ({ key: p.tab, label: p.nav, href: p.path })));
    },
  });
};

export const NO_EXTENSIONS = createExtensions([]);

const positiveId = (v) => Number.isInteger(v) && v > 0 && v <= 2147483647;

export const keysOf = async (query, userId, keys) => {
  const held = new Map(keys.map((k) => [k, false]));
  if (!keys.length) return held;
  const q = sql`SELECT rp.permission_code FROM players p JOIN rank_permissions rp ON rp.rank_id = p.rank_id
    WHERE p.id = ${userId} AND rp.permission_code IN (${sql.in(keys)})`;
  for (const r of await query(q.text, q.params)) held.set(r.permission_code, true);
  return held;
};

const playerOf = (r) => (r ? Object.freeze({
  id: Number(r.id), name: String(r.name), figure: String(r.figure ?? ''), gender: String(r.gender ?? 'M') === 'F' ? 'F' : 'M',
  private: String(r.hide_profile ?? '0') === '1',
}) : null);
const PLAYER = 'SELECT p.id, p.name, p.figure, p.gender, COALESCE(s.hide_profile, FALSE) AS hide_profile FROM players p LEFT JOIN player_preferences s ON s.player_id = p.id';

export const lookups = (ext, query, { wordFilter = null, viewer = null } = {}) => {
  const out = {};
  if (ext.lookups.includes('players')) {
    out.players = Object.freeze({
      byId: async (id) => (positiveId(Number(id)) ? playerOf((await query(`${PLAYER} WHERE p.id = ? LIMIT 1`, [Number(id)]))[0]) : null),
      byName: async (name) => (typeof name === 'string' && name.length > 0 && name.length <= 50
        ? playerOf((await query(`${PLAYER} WHERE p.name = ? LIMIT 1`, [name]))[0]) : null),
      byIds: async (ids) => {
        const list = [...new Set([...ids].map(Number).filter(positiveId))].slice(0, 500);
        if (!list.length) return new Map();
        const rows = await query(`${PLAYER} WHERE p.id IN (${list.map(() => '?').join(', ')})`, list);
        return new Map(rows.map((r) => [Number(r.id), playerOf(r)]));
      },
    });
  }
  if (ext.lookups.includes('blocks')) {
    out.blocks = Object.freeze({
      blocked: async (by, whom) => positiveId(Number(by)) && positiveId(Number(whom))
        && (await query('SELECT 1 FROM player_blocks WHERE player_id = ? AND blocked_id = ? LIMIT 1', [Number(by), Number(whom)])).length > 0,
    });
  }
  if (ext.lookups.includes('filter')) {
    out.filter = async (text) => {
      const v = await viewer();
      if (v.loggedIn && (await keysOf(query, v.id, [NO_FILTER])).get(NO_FILTER)) return Object.freeze({ text: String(text ?? ''), hidden: false });
      return Object.freeze(await wordFilter.filter(text));
    };
  }
  return Object.freeze(out);
};
