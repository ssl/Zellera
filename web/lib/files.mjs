import { createReadStream, statSync } from 'node:fs';
import { extname, join, normalize, sep } from 'node:path';

export const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8', '.mjs': 'text/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.webmanifest': 'application/manifest+json', '.png': 'image/png',
  '.gif': 'image/gif', '.jpg': 'image/jpeg', '.ico': 'image/x-icon', '.xml': 'application/xml; charset=utf-8',
  '.txt': 'text/plain; charset=utf-8', '.swf': 'application/octet-stream',
  '.mp3': 'audio/mpeg', '.svg': 'image/svg+xml', '.css': 'text/css; charset=utf-8', '.woff2': 'font/woff2',
};

export const safeJoin = (base, rel) => {
  const p = normalize(join(base, rel));
  return p === base || p.startsWith(base + sep) ? p : null;
};

export const isFile = (path) => {
  try { return statSync(path).isFile(); } catch { return false; }
};

export const sendText = (res, status, body, type = 'text/plain; charset=utf-8', headers = {}) => {
  res.writeHead(status, { 'content-type': type, 'content-length': Buffer.byteLength(body), ...headers });
  res.end(body);
};

export const sendFile = (req, res, file, extraHeaders = {}, { revalidate = false } = {}) => {
  let st;
  try { st = statSync(file); } catch { st = null; }
  if (!st?.isFile()) return sendText(res, 404, 'not found');
  const validators = revalidate
    ? { etag: `W/"${st.size.toString(16)}-${Math.floor(st.mtimeMs).toString(16)}"`, 'last-modified': st.mtime.toUTCString() }
    : {};
  if (revalidate && isCurrent(req, validators.etag, st.mtime)) {
    res.writeHead(304, { ...extraHeaders, ...validators });
    return res.end();
  }
  res.writeHead(200, {
    'content-type': MIME[extname(file).toLowerCase()] ?? 'application/octet-stream',
    'content-length': st.size,
    ...extraHeaders,
    ...validators,
  });
  if (req.method === 'HEAD') return res.end();
  const stream = createReadStream(file);
  stream.on('error', () => res.destroy());
  stream.pipe(res);
};

const isCurrent = (req, etag, mtime) => {
  const match = req.headers['if-none-match'];
  if (match) return match.split(',').some((tag) => tag.trim() === etag);
  const since = Date.parse(req.headers['if-modified-since'] ?? '');
  return Number.isFinite(since) && Math.floor(mtime.getTime() / 1000) <= Math.floor(since / 1000);
};
