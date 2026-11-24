import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { config, gameNameNow } from './lib/config.mjs';
import { database, isDbDown } from './lib/db.mjs';
import { BridgeDown } from './lib/bridge.mjs';
import { error, htmlError, htmlNotFound, HttpError, preflight, readJsonBody, send } from './lib/http.mjs';
import { clientIp, createLimiter, ipLimitKey } from '../lib/limits.mjs';
import { BadParam, compileRoutes, matchRoute } from './lib/router.mjs';
import { spec } from './lib/spec.mjs';

const MODULE_NAMES = ['users', 'rest', 'vars', 'docs'];
const loadModule = async (name) => {
  const url = new URL(`./handlers/${name}.mjs`, import.meta.url);
  if (!existsSync(fileURLToPath(url))) return { handlers: {}, grants: [], missing: true };
  return import(url.href);
};
export const MODULES = Object.fromEntries(await Promise.all(MODULE_NAMES.map(async (n) => [n, await loadModule(n)])));
const PREFIXES = ['/api/public', '/extradata/public'];

const API_PATH = /^\/(?:api|extradata)\/public(?:\/|$)/i;
export const isApiPath = (pathname) => API_PATH.test(pathname);
const isDocsPath = (pathname) => {
  const p = pathname.toLowerCase();
  return p === '/api/public/api-docs.json' || p === '/api/public/api-docs' || p.startsWith('/api/public/api-docs/');
};

const areaOf = (endpoint) => {
  if (endpoint.builder === 'vars' || endpoint.id === 'ping') return null;
  return endpoint.path.toLowerCase().startsWith('/extradata/') ? 'photos' : 'api';
};

export const addressKey = (ip) => {
  const k = ipLimitKey(ip);
  return k.endsWith('::/64') ? `ip6:${k}` : `ip:${k}`;
};

export const createApi = ({
  modules = MODULES, db = database, limiter = createLimiter(), ipLimiter = createLimiter(), ipLimit = config.ipLimit,
  endpoints = spec.endpoints, log = (m) => console.error(m),
} = {}) => {
  const table = compileRoutes(endpoints);
  const warned = new Set();

  const makeCtx = (req, url, endpoint, params) => {
    let body;
    return {
      req, url, method: req.method, pathname: url.pathname, endpoint, params,
      query: url.searchParams,
      header: (name) => { const v = req.headers[String(name).toLowerCase()]; return Array.isArray(v) ? v[0] : v; },
      body: () => (body ??= readJsonBody(req)),
      ip: clientIp(req),
      db,
      limiter,
      game: { site: config.site, get serverId() { return config.serverId; }, get name() { return gameNameNow(); } },
      log,
    };
  };

  const handle = async (req, res, url, { canSee = async () => false } = {}) => {
    const pathname = url.pathname;
    try {
      if (req.method === 'OPTIONS') { send(req, res, preflight(req)); return true; }

      if (isDocsPath(pathname)) {
        const reply = await modules.docs?.serve?.(makeCtx(req, url, null, {}));
        send(req, res, reply ?? htmlNotFound(req.method, pathname));
        return true;
      }

      let match;
      try { match = matchRoute(table, req.method, pathname); } catch (e) {
        if (e instanceof BadParam) { send(req, res, htmlError(400)); return true; }
        throw e;
      }
      const fn = match && modules[match.endpoint.builder]?.handlers?.[match.endpoint.id];
      if (!fn) {
        if (match && !warned.has(match.endpoint.id)) { warned.add(match.endpoint.id); log(`api: no handler ${match.endpoint.handler} yet`); }
        send(req, res, htmlNotFound(req.method, pathname));
        return true;
      }

      if (!config.serverId) {
        log(`api ${req.method} ${pathname}: the client profile names no server id (it comes with a content pack)`);
        send(req, res, error(503, 'maintenance'));
        return true;
      }

      const area = areaOf(match.endpoint);
      if (area && !(await canSee(req, area))) { send(req, res, error(401, 'login-required')); return true; }

      if (ipLimit) {
        const r = ipLimiter.take(addressKey(clientIp(req)), ipLimit);
        if (!r.ok) { send(req, res, error(429, 'too-many-requests', { 'Retry-After': String(r.retryAfter) })); return true; }
      }

      const reply = await fn(makeCtx(req, url, match.endpoint, match.params));
      if (!reply || typeof reply.status !== 'number') throw new Error(`${match.endpoint.handler} returned no reply`);
      send(req, res, reply);
    } catch (e) {
      if (res.headersSent) { res.destroy(); return true; }
      if (e instanceof HttpError) send(req, res, e.reply);
      else if (e instanceof BridgeDown || isDbDown(e)) {
        log(`api ${req.method} ${pathname}: ${e.message ?? e}`);
        send(req, res, error(503, 'maintenance'));
      } else {
        log(`api ${req.method} ${pathname}: ${e?.stack ?? e}`);
        send(req, res, htmlError(500));
      }
    }
    return true;
  };

  return { handle, table };
};

let instance = null;
export const handle = (req, res, url, opts) => (instance ??= createApi()).handle(req, res, url, opts);
