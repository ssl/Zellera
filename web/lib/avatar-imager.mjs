import { createHash } from 'node:crypto';
import { mkdirSync, readdirSync, readFileSync, renameSync, statSync, unlinkSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';
import { encodePng } from './asset-bundle.mjs';

const GESTURES = Object.freeze(['std', 'sml', 'agr', 'sad', 'srp', 'eyb', 'spk']);
const ACTIONS = Object.freeze(['std', 'wlk', 'sit', 'lay', 'wav', 'sig', 'blow', 'laugh', 'respect']);
export const MISS_LIMIT = Object.freeze([30, 60]);
const CACHE_MAX = 2000;
const DISK_MAX = 20_000;

const FIGURE = /^[a-z0-9.-]{1,256}$/;
const DIGIT07 = /^[0-7]$/;

const ACTION_STATE = { wlk: 'mv', sit: 'sit', lay: 'lay', wav: 'wave', sig: 'sign', blow: 'blow', laugh: 'laugh', respect: 'respect' };
const GESTURE_STATE = { sml: 'sml', agr: 'agr', sad: 'sad', srp: 'srp', eyb: 'Sleep', spk: 'talk' };

export const normalizeAvatarQuery = (params) => {
  const get = (k) => params.get(k);
  const figure = get('figure');
  if (figure == null || !FIGURE.test(figure)) return { error: 'figure' };
  const gender = (get('gender') ?? 'M').toUpperCase();
  if (gender !== 'M' && gender !== 'F') return { error: 'gender' };
  const direction = get('direction') ?? '2';
  const headDirection = get('head_direction') ?? '3';
  if (!DIGIT07.test(direction)) return { error: 'direction' };
  if (!DIGIT07.test(headDirection)) return { error: 'head_direction' };
  const size = (get('size') ?? 'm').toLowerCase();
  if (!['s', 'm', 'l'].includes(size)) return { error: 'size' };
  const headonly = get('headonly') ?? '0';
  if (headonly !== '0' && headonly !== '1') return { error: 'headonly' };
  const gesture = get('gesture') ?? '';
  if (gesture !== '' && !GESTURES.includes(gesture)) return { error: 'gesture' };
  const actionRaw = get('action') ?? '';
  const actions = actionRaw === '' ? [] : [...new Set(actionRaw.split(','))];
  if (actions.length > 3 || actions.some((a) => !ACTIONS.includes(a))) return { error: 'action' };
  const q = new URLSearchParams({ figure, gender, direction, head_direction: headDirection, size });
  if (headonly === '1') q.set('headonly', '1');
  if (gesture) q.set('gesture', gesture);
  if (actions.length) q.set('action', actions.join(','));
  return { query: q.toString() };
};

const crop = (img, x, y, w, h) => {
  const rgba = Buffer.alloc(w * h * 4);
  for (let row = 0; row < h; row++) {
    const sy = y + row;
    if (sy < 0 || sy >= img.height) continue;
    for (let col = 0; col < w; col++) {
      const sx = x + col;
      if (sx < 0 || sx >= img.width) continue;
      img.rgba.copy(rgba, (row * w + col) * 4, (sy * img.width + sx) * 4, (sy * img.width + sx) * 4 + 4);
    }
  }
  return { width: w, height: h, rgba };
};
const double = (img) => {
  const w = img.width * 2, h = img.height * 2, rgba = Buffer.alloc(w * h * 4);
  for (let y = 0; y < h; y++) for (let x = 0; x < w; x++) img.rgba.copy(rgba, (y * w + x) * 4, ((y >> 1) * img.width + (x >> 1)) * 4, ((y >> 1) * img.width + (x >> 1)) * 4 + 4);
  return { width: w, height: h, rgba };
};
export const FRAME = Object.freeze({
  h: { full: [0, 14, 64, 110], head: [6, 22, 54, 62] },
  sh: { full: [0, 15, 32, 56], head: [3, 19, 27, 30] },
});

export const renderAvatarPng = async (renderer, query) => {
  const p = new URLSearchParams(query);
  const headonly = p.get('headonly') === '1';
  const size = p.get('size');
  const scale = size === 's' ? 'sh' : 'h';
  const actions = (p.get('action') ?? '').split(',').filter((a) => ACTION_STATE[a]).map((a) => ({ type: ACTION_STATE[a] }));
  const canvas = await renderer.render({
    figure: p.get('figure'), direction: Number(p.get('direction')), headDirection: Number(p.get('head_direction')),
    headonly, gesture: GESTURE_STATE[p.get('gesture') ?? ''] ?? '', actions, scale,
    mirror: { shift: scale === 'h' ? 64 : 32, keep: 64 },
  });
  if (!canvas) return null;
  const [fx, fy, fw, fh] = FRAME[scale][headonly ? 'head' : 'full'];
  let img = canvas.geometryType === 'vertical' || canvas.geometryType === 'sitting'
    ? crop(canvas, Math.floor(canvas.regX) + fx, fy, fw, fh)
    : canvas;
  if (size === 'l') img = double(img);
  for (let i = 0; i < img.rgba.length; i += 4) if (img.rgba[i + 3] === 0) img.rgba.fill(255, i, i + 3);
  return encodePng(img.width, img.height, img.rgba);
};

const pictureKey = (version, query) => createHash('sha1').update(`${version}\n${query}`).digest('hex');

export const createAvatarImager = ({ renderer, limiter, cacheDir = null, diskMax = DISK_MAX, log = () => {} }) => {
  const cache = new Map();
  const inflight = new Map();
  const remember = (key, png) => {
    cache.delete(key);
    cache.set(key, png);
    if (cache.size > CACHE_MAX) cache.delete(cache.keys().next().value);
  };
  let onDisk = null;
  const diskIndex = () => {
    if (onDisk) return onDisk;
    let files = [];
    try {
      files = readdirSync(cacheDir).filter((n) => /^[0-9a-f]{40}\.png$/.test(n))
        .map((n) => { try { return [n.slice(0, -4), statSync(join(cacheDir, n)).mtimeMs]; } catch { return null; } })
        .filter(Boolean).sort((a, b) => a[1] - b[1]);
    } catch { }
    onDisk = new Set(files.map(([key]) => key));
    return onDisk;
  };
  const touchDisk = (key) => {
    const index = diskIndex();
    index.delete(key);
    index.add(key);
    while (index.size > diskMax) {
      const oldest = index.values().next().value;
      index.delete(oldest);
      try { unlinkSync(join(cacheDir, `${oldest}.png`)); } catch { }
    }
  };
  const fromDisk = (key) => {
    if (!cacheDir) return null;
    try {
      return readFileSync(join(cacheDir, `${key}.png`));
    } catch {
      return null;
    }
  };
  const toDisk = (key, png) => {
    if (!cacheDir) return;
    try {
      mkdirSync(cacheDir, { recursive: true });
      const tmp = join(cacheDir, `${key}.png.part${process.pid}`);
      writeFileSync(tmp, png);
      renameSync(tmp, join(cacheDir, `${key}.png`));
      touchDisk(key);
    } catch (e) {
      log(`avatar imager: cannot keep ${key}.png: ${e?.message ?? e}`);
    }
  };
  const send = (req, res, status, body, type, headers = {}) => {
    res.writeHead(status, { 'content-type': type, 'content-length': body.length, 'x-content-type-options': 'nosniff', ...headers });
    res.end(req.method === 'HEAD' ? undefined : body);
  };
  const text = (req, res, status, msg, headers) => send(req, res, status, Buffer.from(msg), 'text/plain; charset=utf-8', { 'cache-control': 'no-store', ...headers });

  const draw = (key, query) => {
    if (inflight.has(key)) return inflight.get(key);
    const job = renderAvatarPng(renderer, query).then((png) => {
      if (png) {
        remember(key, png);
        toDisk(key, png);
      }
      return png;
    }).finally(() => inflight.delete(key));
    inflight.set(key, job);
    return job;
  };

  return async (req, res, params, { ip, open = true } = {}) => {
    const n = normalizeAvatarQuery(params);
    if (n.error) return text(req, res, 400, `bad ${n.error}`);
    let key;
    try {
      key = pictureKey(renderer.version?.() ?? '', n.query);
    } catch (e) {
      log(`avatar imager: ${e?.message ?? e}`);
      return text(req, res, 503, 'avatar imager unavailable');
    }
    const etag = `"${key}"`;
    const headers = { 'cache-control': open ? 'public, max-age=86400' : 'private, max-age=86400', etag, 'access-control-allow-origin': '*' };
    const ok = (png) => {
      const tags = String(req.headers['if-none-match'] ?? '').split(',').map((t) => t.trim().replace(/^W\//, ''));
      if (tags.includes(etag)) {
        res.writeHead(304, headers);
        return res.end();
      }
      return send(req, res, 200, png, 'image/png', headers);
    };
    const hit = cache.get(key) ?? fromDisk(key);
    if (hit) {
      remember(key, hit);
      if (cacheDir) touchDisk(key);
      return ok(hit);
    }
    const allowed = limiter.hit(`avatarimage:${ip}`, ...MISS_LIMIT);
    if (!allowed.ok) return text(req, res, 429, 'too many new avatars, try again soon', { 'retry-after': String(allowed.retryAfter) });
    let png;
    try {
      png = await draw(key, n.query);
    } catch (e) {
      log(`avatar imager: ${e?.message ?? e}`);
      return text(req, res, 503, 'avatar imager unavailable');
    }
    if (!png) return text(req, res, 404, 'not found');
    return ok(png);
  };
};
