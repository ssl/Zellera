import { readFileSync, statSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { etagOf } from '../lib/http.mjs';
import { gameNameNow } from '../lib/config.mjs';
import { spec, withSite } from '../lib/spec.mjs';
import { buildOpenApi } from '../docs/openapi.mjs';
import { renderPage } from '../docs/render.mjs';

export const handlers = {};
export const grants = [];

const DOCS = '/api/public/api-docs';
export const CSP = "default-src 'none'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'";
const PAGE_HEADERS = {
  'content-security-policy': CSP,
  'x-content-type-options': 'nosniff',
  'x-frame-options': 'DENY',
  'referrer-policy': 'same-origin',
  'cache-control': 'no-cache',
};
const JSON_TYPE = 'application/json; charset=utf-8';

const fixedReply = (type, body, headers = {}) => Object.freeze({
  status: 200, type, body, noEtag: true, headers: Object.freeze({ etag: etagOf(body), ...headers }),
});

const ASSETS = {
  'docs.css': 'text/css; charset=utf-8',
  'docs.js': 'text/javascript; charset=utf-8',
  'shape.mjs': 'text/javascript; charset=utf-8',
};
const ASSET_HEADERS = { 'x-content-type-options': 'nosniff', 'cache-control': 'no-cache' };
const assetCache = new Map();
const asset = (name) => {
  const file = fileURLToPath(new URL(`../docs/${name}`, import.meta.url));
  const { mtimeMs } = statSync(file);
  const hit = assetCache.get(name);
  if (hit && hit.mtimeMs === mtimeMs) return hit.reply;
  const reply = fixedReply(ASSETS[name], readFileSync(file), ASSET_HEADERS);
  assetCache.set(name, { mtimeMs, reply });
  return reply;
};

let built = null;
export const docs = () => {
  const name = gameNameNow();
  if (built && built.name === name) return built;
  const filled = withSite(spec);
  const openapi = buildOpenApi(filled);
  const page = Buffer.from(renderPage(filled), 'utf8');
  built = {
    name,
    page,
    data: filled,
    openapi,
    replies: {
      page: fixedReply('text/html; charset=utf-8', page, PAGE_HEADERS),
      data: fixedReply(JSON_TYPE, Buffer.from(JSON.stringify(filled), 'utf8')),
      openapi: fixedReply(JSON_TYPE, Buffer.from(JSON.stringify(openapi), 'utf8')),
    },
  };
  return built;
};

const escapeHtml = (s) => s.replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const redirect = (url) => {
  const location = `${url.pathname}/${url.search}`;
  const body = `<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>Redirecting</title>\n</head>\n<body>\n<pre>Redirecting to ${escapeHtml(location)}</pre>\n</body>\n</html>\n`;
  return {
    status: 301, type: 'text/html; charset=UTF-8', body: Buffer.from(body, 'utf8'), noEtag: true,
    headers: { 'content-security-policy': "default-src 'none'", 'x-content-type-options': 'nosniff', location },
  };
};

export const serve = async (ctx) => {
  const getLike = ctx.method === 'GET' || ctx.method === 'HEAD';
  const path = ctx.pathname;
  const lower = path.toLowerCase();
  if (lower === `${DOCS}.json`) return getLike ? docs().replies.openapi : null;
  if (lower !== DOCS && !lower.startsWith(`${DOCS}/`)) return null;
  if (!getLike) return docs().replies.page;
  if (lower === DOCS) return redirect(ctx.url);
  const rest = path.slice(DOCS.length + 1);
  if (rest === 'api.json') return docs().replies.data;
  if (Object.hasOwn(ASSETS, rest)) return asset(rest);
  return docs().replies.page;
};
