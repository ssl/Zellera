import { createHash, randomBytes } from 'node:crypto';

const JSON_TYPE = 'application/json; charset=utf-8';
const XML_TYPE = 'text/xml; charset=utf-8';
const HTML_TYPE = 'text/html; charset=utf-8';
const STATUS_TEXT = { 400: 'Bad Request', 404: 'Not Found', 413: 'Payload Too Large', 415: 'Unsupported Media Type', 500: 'Internal Server Error' };

export const json = (status, body, headers = {}) => ({ status, type: JSON_TYPE, body: Buffer.from(JSON.stringify(body), 'utf8'), headers });
export const ok = (body, headers) => json(200, body, headers);
export const error = (status, code, headers) => json(status, { error: code }, headers);
export const notFound = () => error(404, 'not-found');
export const validation = (param, msg, value) =>
  json(404, { errors: [value === undefined ? { param, msg } : { param, msg, value }] });
export const xml = (status, text) => ({ status, type: XML_TYPE, body: Buffer.from(text, 'utf8'), headers: {} });
export const empty = (status, headers = {}) => ({ status, type: null, body: Buffer.alloc(0), headers, emptyBody: true });
export const noContent = () => ({ status: 204, type: null, body: null, headers: {} });

const escapeHtml = (s) => String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const encodeUrl = (s) => String(s)
  .replace(/(^|[^\uD800-\uDBFF])[\uDC00-\uDFFF]|[\uD800-\uDBFF]([^\uDC00-\uDFFF]|$)/g, '$1�$2')
  .replace(/(?:[^\x21\x25\x26-\x3B\x3D\x3F-\x5B\x5D\x5F\x61-\x7A\x7E]|%(?![0-9A-Fa-f]{2}))+/g, (m) => encodeURI(m));
const htmlPage = (status, message) => {
  const body = '<!DOCTYPE html>\n<html lang="en">\n<head>\n<meta charset="utf-8">\n<title>Error</title>\n</head>\n<body>\n'
    + `<pre>${escapeHtml(message).replace(/\n/g, '<br>').replace(/  /g, ' &nbsp;')}</pre>\n</body>\n</html>\n`;
  return {
    status, type: HTML_TYPE, body: Buffer.from(body, 'utf8'), noEtag: true,
    headers: { 'content-security-policy': "default-src 'none'", 'x-content-type-options': 'nosniff' },
  };
};
export const htmlNotFound = (method, pathname) => htmlPage(404, `Cannot ${method} ${encodeUrl(pathname)}`);
export const htmlError = (status) => htmlPage(status, STATUS_TEXT[status] ?? 'Error');

export class HttpError extends Error {
  constructor(reply) {
    super(`http ${reply.status}`);
    this.reply = reply;
  }
}
export const fail = (reply) => { throw new HttpError(reply); };

export const etagOf = (buf) => {
  if (buf.length === 0) return 'W/"0-2jmj7l5rSw0yVb/vlWAYkK/YBwk"';
  const hash = createHash('sha1').update(buf).digest('base64').slice(0, 27);
  return `W/"${buf.length.toString(16)}-${hash}"`;
};

const isFresh = (req, etag) => {
  const inm = req.headers['if-none-match'];
  if (!inm) return false;
  if (/(?:^|,)\s*no-cache\s*(?:,|$)/.test(req.headers['cache-control'] ?? '')) return false;
  if (inm.trim() === '*') return true;
  const strip = (t) => t.trim().replace(/^W\//, '');
  return inm.split(',').some((t) => strip(t) === strip(etag));
};

const corsHeaders = (req) => {
  const h = {};
  if (req.headers.origin) h['access-control-allow-origin'] = req.headers.origin;
  h.vary = 'Origin';
  return h;
};

export const preflight = (req) => {
  const h = corsHeaders(req);
  h['access-control-allow-methods'] = 'GET,HEAD,PUT,PATCH,POST,DELETE';
  const asked = req.headers['access-control-request-headers'];
  if (asked) {
    h['access-control-allow-headers'] = asked;
    h.vary = 'Origin, Access-Control-Request-Headers';
  }
  h['content-length'] = '0';
  return { status: 204, type: null, body: null, headers: h, raw: true };
};

const requestId = () => randomBytes(16).toString('hex');

export const send = (req, res, reply) => {
  if (reply.raw) {
    res.writeHead(reply.status, { ...reply.headers, 'X-DIS-Request-ID': requestId() });
    res.end();
    return;
  }
  const headers = { ...corsHeaders(req), 'X-DIS-Request-ID': requestId() };
  const { body } = reply;
  if (reply.status === 204 || body === null) {
    Object.assign(headers, reply.headers);
    res.writeHead(reply.status, headers);
    res.end();
    return;
  }
  if (reply.type) headers['Content-Type'] = reply.type;
  headers['Content-Length'] = String(body.length);
  if (!reply.noEtag && !reply.emptyBody) headers.etag = etagOf(body);
  Object.assign(headers, reply.headers);
  const getLike = req.method === 'GET' || req.method === 'HEAD';
  if (getLike && headers.etag && ((reply.status >= 200 && reply.status < 300) || reply.status === 304) && isFresh(req, headers.etag)) {
    delete headers['Content-Type'];
    delete headers['Content-Length'];
    res.writeHead(304, headers);
    res.end();
    return;
  }
  res.writeHead(reply.status, headers);
  res.end(req.method === 'HEAD' ? undefined : body);
};

const LIMIT = 100 * 1024;

const keepBigIntegers = (key, value, context) => (typeof value === 'number' && Number.isInteger(value)
  && !Number.isSafeInteger(value) && typeof context?.source === 'string' && /^-?\d+$/.test(context.source)
  ? BigInt(context.source) : value);

export const readJsonBody = (req) => new Promise((resolve, reject) => {
  const type = String(req.headers['content-type'] ?? '').split(';')[0].trim().toLowerCase();
  const isJson = type === 'application/json' || /^application\/[a-z0-9.+-]+\+json$/.test(type);
  let size = 0;
  let tooBig = false;
  const chunks = [];
  req.on('data', (c) => {
    size += c.length;
    if (size > LIMIT) tooBig = true;
    else if (isJson) chunks.push(c);
  });
  req.on('end', () => {
    if (tooBig) return reject(new HttpError(htmlError(413)));
    if (!isJson) return resolve({});
    const text = Buffer.concat(chunks).toString('utf8');
    if (text.length === 0) return resolve({});
    const first = text.trimStart()[0];
    if (first !== '{' && first !== '[') return reject(new HttpError(htmlError(400)));
    try { resolve(JSON.parse(text, keepBigIntegers)); } catch { reject(new HttpError(htmlError(400))); }
  });
  req.on('error', () => reject(new HttpError(htmlError(400))));
});
