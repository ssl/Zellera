export const CSP = [
  "default-src 'self'", "script-src 'self'", "style-src 'self'", "font-src 'self'",
  "img-src 'self'", "connect-src 'self'", "form-action 'self'",
  "frame-ancestors 'none'", "base-uri 'none'", "object-src 'none'",
].join('; ');

export const PAGE_HEADERS = {
  'cache-control': 'no-store', 'content-security-policy': CSP, 'x-content-type-options': 'nosniff',
  'x-frame-options': 'DENY', 'referrer-policy': 'same-origin',
};
export const HK_HEADERS = { ...PAGE_HEADERS, 'referrer-policy': 'no-referrer', 'x-robots-tag': 'noindex, nofollow' };

const end = (req, res, status, headers, body) => {
  const buf = Buffer.from(body ?? '', 'utf8');
  res.writeHead(status, { 'content-length': buf.length, ...headers });
  res.end(req.method === 'HEAD' ? undefined : buf);
};

export const sendText = (req, res, status, text, headers = {}) =>
  end(req, res, status, { 'content-type': 'text/plain; charset=utf-8', 'x-content-type-options': 'nosniff', ...headers }, String(text));

export const sendJson = (req, res, status, value, headers = {}) =>
  end(req, res, status, {
    'content-type': 'application/json; charset=utf-8', 'cache-control': 'no-store', 'x-content-type-options': 'nosniff', ...headers,
  }, JSON.stringify(value));

const SAFE_LOCATION = /^\/(?![/\\])[^\r\n\s]*$/;
export const redirect = (req, res, location, status = 302) => {
  if (!SAFE_LOCATION.test(location)) throw new Error(`redirect: refusing location ${JSON.stringify(location)}`);
  end(req, res, status, { location, 'cache-control': 'no-store', 'referrer-policy': 'no-referrer' }, '');
};

const DRAIN_MAX = 1 << 20;
export const readBody = (req, max) => new Promise((resolve) => {
  let size = 0;
  let chunks = [];
  req.on('data', (c) => {
    size += c.length;
    if (size <= max) { chunks?.push(c); return; }
    // Keeps reading what is too long for a while, so the refusal reaches the sender instead of a reset connection.
    chunks = null;
    resolve(null);
    if (size > max + DRAIN_MAX) req.destroy();
  });
  req.on('end', () => resolve(chunks ? Buffer.concat(chunks) : null));
  req.on('error', () => resolve(null));
});

export const readForm = async (req, max = 8192) => {
  if (!/^application\/x-www-form-urlencoded(;|$)/i.test(String(req.headers['content-type'] ?? ''))) return null;
  const body = await readBody(req, max);
  if (!body) return null;
  try { return new URLSearchParams(body.toString('utf8')); } catch { return null; }
};

export const readJson = async (req, max = 16384) => {
  if (!/^application\/json(;|$)/i.test(String(req.headers['content-type'] ?? ''))) return null;
  const body = await readBody(req, max);
  if (!body) return null;
  try {
    const v = JSON.parse(body.toString('utf8') || 'null');
    return v && typeof v === 'object' && !Array.isArray(v) ? v : null;
  } catch { return null; }
};

export const extraOrigins = () => String(process.env.ZEL_EXTRA_ORIGINS ?? '').split(',').map((s) => s.trim()).filter(Boolean);

export const originOk = (req, SITE, PROD) => {
  const origin = req.headers.origin;
  if (!origin) return false;
  if (origin === 'null') return req.headers['sec-fetch-site'] === 'same-origin';
  if (origin === SITE || extraOrigins().includes(origin)) return true;
  if (PROD) return false;
  const host = req.headers.host;
  return !!host && (origin === `http://${host}` || origin === `https://${host}`);
};
export const crossSite = (req) => {
  const site = req?.headers?.['sec-fetch-site'];
  return site != null && site !== 'same-origin' && site !== 'none';
};
export const sameOriginRead = (req, SITE, PROD) => !crossSite(req) && (!req.headers.origin || originOk(req, SITE, PROD));
export const sameSiteFetch = (req) => {
  const site = req.headers['sec-fetch-site'];
  return !site || site === 'same-origin';
};
