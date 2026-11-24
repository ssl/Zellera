import { existsSync, mkdirSync, readFileSync, renameSync, writeFileSync } from 'node:fs';
import { dirname, extname, join, posix } from 'node:path';
import { assetRoots, bundleExtension, escapeRegExp, fromProfile, furniPath } from './client-profile.mjs';
import { isFile, safeJoin, sendFile, sendText } from './files.mjs';
import { createGate, ipLimitKey, isLoopbackIp } from './limits.mjs';
import { furniFile, furniFilePath } from './furni-files.mjs';
import { fitIcon, furniIconPath, pageIconFurniId } from './page-icon.mjs';
import { SECONDS_PER_DAY } from './time.mjs';

const ASSET_TYPES = Object.freeze(['.png', '.gif', '.jpg', '.jpeg', '.webp', '.json', '.xml', '.txt', '.mp3', '.swf']);
const isAssetType = (type) => ASSET_TYPES.includes(type) || type === bundleExtension();
const bareIcon = fromProfile(() => (furniPath() ? new RegExp(`^${escapeRegExp(furniPath())}/(\\d+)/([^/]+)_icon\\.png$`) : null));
export const MISS_LIMIT = Object.freeze([120, 60]);
export const NOT_FOUND_TTL_MS = SECONDS_PER_DAY * 1000;
const NOT_FOUND_MAX = 50_000;
const MAX_LENGTH = 256;
export const UPSTREAM_TIMEOUT_MS = 20_000;
export const UPSTREAM_MAX_BYTES = 32 * 1024 * 1024;
// XML goes out as a plain text download: the client reads it as text, and no browser renders it as a page of this site.
const XML_HEADERS = Object.freeze({ 'content-type': 'text/plain; charset=utf-8', 'content-disposition': 'attachment' });
const MAX_DEPTH = 8;
const SEGMENTS = /^[A-Za-z0-9_][A-Za-z0-9_.-]*(?:\/[A-Za-z0-9_][A-Za-z0-9_.-]*)*$/;

export const canonicalAssetPath = (rel) => {
  if (typeof rel !== 'string' || rel.length > MAX_LENGTH || !SEGMENTS.test(rel) || posix.normalize(rel) !== rel) return null;
  const parts = rel.split('/');
  if (parts.length < 2 || parts.length > MAX_DEPTH || !assetRoots().includes(parts[0])) return null;
  return isAssetType(extname(rel).toLowerCase()) ? rel : null;
};

const tooLarge = (rel) => new Error(`upstream file ${rel} is larger than ${UPSTREAM_MAX_BYTES} bytes`);
const readCapped = async (res, rel, max) => {
  if (Number(res.headers?.get?.('content-length')) > max) throw tooLarge(rel);
  if (typeof res.body?.getReader !== 'function') {
    const all = Buffer.from(await res.arrayBuffer());
    if (all.length > max) throw tooLarge(rel);
    return all;
  }
  const reader = res.body.getReader();
  const chunks = [];
  let size = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    size += value.length;
    if (size > max) {
      await reader.cancel().catch(() => {});
      throw tooLarge(rel);
    }
    chunks.push(value);
  }
  return Buffer.concat(chunks);
};

export const createAssetMirror = ({
  cacheDir, ownDir = null, origin = '', fetchImpl = fetch, limiter, bundleIcon = null, bundleRename = null, furniById = null,
  furniTypes = null, now = Date.now, maxFetches = 8, maxQueue = 256, timeoutMs = UPSTREAM_TIMEOUT_MS, maxBytes = UPSTREAM_MAX_BYTES,
}) => {
  const inflight = new Map();
  const notFound = new Map();
  const gate = createGate(maxFetches, maxQueue);
  const headers = { 'cache-control': 'public, max-age=86400', 'access-control-allow-origin': '*', 'x-content-type-options': 'nosniff' };

  const knownMissing = (rel) => {
    const until = notFound.get(rel);
    if (until === undefined) return false;
    if (until > now()) return true;
    notFound.delete(rel);
    return false;
  };
  const rememberMissing = (rel) => {
    notFound.delete(rel);
    notFound.set(rel, now() + NOT_FOUND_TTL_MS);
    if (notFound.size > NOT_FOUND_MAX) notFound.delete(notFound.keys().next().value);
  };

  const fetchAsset = (rel) => {
    const target = safeJoin(cacheDir, rel);
    if (!target || target === cacheDir) return Promise.resolve(null);
    if (isFile(target)) return Promise.resolve(target);
    if (!origin || knownMissing(rel)) return Promise.resolve(null);
    if (inflight.has(rel)) return inflight.get(rel);
    const job = gate.run(async () => {
      const res = await fetchImpl(`${origin}/${rel}`, {
        redirect: 'error', headers: { 'user-agent': 'Mozilla/5.0 Zellera-mirror' }, signal: AbortSignal.timeout(timeoutMs),
      });
      if (res.status === 404) { rememberMissing(rel); return null; }
      if (!res.ok) throw new Error(`upstream ${res.status}`);
      const buf = await readCapped(res, rel, maxBytes);
      mkdirSync(dirname(target), { recursive: true });
      const tmp = `${target}.part${process.pid}`;
      writeFileSync(tmp, buf);
      renameSync(tmp, target);
      return target;
    }).finally(() => inflight.delete(rel));
    inflight.set(rel, job);
    return job;
  };

  const store = (rel, buf) => {
    const target = join(cacheDir, rel);
    mkdirSync(dirname(target), { recursive: true });
    writeFileSync(`${target}.part${process.pid}`, buf);
    renameSync(`${target}.part${process.pid}`, target);
    return target;
  };
  const furni = (rel) => (furniTypes ? furniFile(rel, furniTypes) : null);
  const fileOf = furniFilePath;

  const furniAsset = async (f) => {
    const rel = fileOf(f.rev, f.name, f.kind);
    const asked = await fetchAsset(rel);
    if (asked) return asked;
    for (const r of f.revisions.slice(1)) {
      const file = await fetchAsset(fileOf(r, f.name, f.kind)).catch(() => null);
      if (file) return store(rel, readFileSync(file));
    }
    return null;
  };
  const furniOf = (name, kind, rev) => (furniTypes ? furni(fileOf(rev, name, kind)) : null);

  const iconFallback = async (rel) => {
    if (!bundleIcon) return null;
    let bundle = null, colour = null;
    const f = furni(rel);
    if (f) {
      if (f.kind !== 'icon') return null;
      colour = f.colour;
      const b = furniOf(f.colour == null ? f.name : f.type, 'bundle', f.rev);
      bundle = b ? await furniAsset(b).catch(() => null) : null;
    } else {
      const m = bareIcon()?.exec(rel);
      if (!m) return null;
      bundle = await fetchAsset(fileOf(m[1], m[2], 'bundle')).catch(() => null);
    }
    if (!bundle) return null;
    let png = null;
    try { png = bundleIcon(readFileSync(bundle), colour); } catch { return null; }
    return png ? store(rel, png) : null;
  };

  const nftTwin = async (f) => {
    if (!bundleRename || f.kind !== 'bundle' || f.colour != null || !f.name.startsWith('nft_')) return null;
    const twinName = f.name.slice('nft_'.length);
    const own = furniTypes(f.name), twin = furniTypes(twinName);
    if (!own || !twin || twin.type !== own.type) return null;
    const twinRev = Math.max(...twin.revisions);
    const iconOf = async (name, rev) => {
      const i = furniOf(name, 'icon', rev);
      const file = i ? await furniAsset(i).catch(() => null) : null;
      return file ? readFileSync(file) : null;
    };
    const [a, b] = await Promise.all([iconOf(f.name, f.rev), iconOf(twinName, twinRev)]);
    if (!a || !b || !a.equals(b)) return null;
    const source = furniOf(twinName, 'bundle', twinRev);
    const twinFile = source ? await furniAsset(source).catch(() => null) : null;
    if (!twinFile) return null;
    let renamed = null;
    try { renamed = bundleRename(readFileSync(twinFile), twinName, f.name); } catch { return null; }
    return renamed ? store(fileOf(f.rev, f.name, 'bundle'), renamed) : null;
  };

  const find = async (rel) => {
    const f = furni(rel);
    if (!f) return (await fetchAsset(rel)) ?? (await iconFallback(rel));
    return (await furniAsset(f)) ?? (f.kind === 'icon' ? await iconFallback(rel) : await nftTwin(f));
  };

  const pageIcon = async (rel) => {
    const f = furniById?.(pageIconFurniId(rel));
    if (!f) return null;
    const iconRel = furniIconPath(f);
    const icon = await find(iconRel);
    if (!icon) return null;
    let png = null;
    try { png = fitIcon(readFileSync(icon)); } catch { return null; }
    if (!png) return null;
    return store(rel, png);
  };

  const handle = async (req, res, rawRel, ip) => {
    const rel = canonicalAssetPath(typeof rawRel === 'string' ? rawRel.replace(/\/{2,}/g, '/') : rawRel);
    if (!rel) return sendText(res, 404, 'not found');
    const sent = extname(rel).toLowerCase() === '.xml' ? { ...headers, ...XML_HEADERS } : headers;
    const own = ownDir && safeJoin(ownDir, rel);
    if (own && isFile(own)) return sendFile(req, res, own, sent);
    const target = join(cacheDir, rel);
    if (isFile(target)) return sendFile(req, res, target, sent);
    if (existsSync(target)) return sendText(res, 404, 'not found');
    const fixedSet = furni(rel) || (pageIconFurniId(rel) && furniById?.(pageIconFurniId(rel)));
    if (!knownMissing(rel) && limiter && !isLoopbackIp(ip) && !fixedSet) {
      const hit = limiter.hit(`assets:${ipLimitKey(ip)}`, ...MISS_LIMIT);
      if (!hit.ok) {
        return sendText(res, 429, 'too many new assets, try again soon', 'text/plain; charset=utf-8',
          { 'retry-after': String(hit.retryAfter), 'cache-control': 'no-store' });
      }
    }
    let file;
    try {
      file = pageIconFurniId(rel) ? await pageIcon(rel) : await find(rel);
    } catch (e) {
      if (e?.code !== 'EBUSY') throw e;
      return sendText(res, 503, 'busy, try again soon', 'text/plain; charset=utf-8', { 'retry-after': '5', 'cache-control': 'no-store' });
    }
    if (!file) return sendText(res, 404, 'not found');
    return sendFile(req, res, file, sent);
  };

  return { handle, fetchAsset };
};
