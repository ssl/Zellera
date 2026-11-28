import { createHash, createHmac, randomBytes } from 'node:crypto';
import { SECRET } from './config.mjs';
import { safeEqual } from './auth.mjs';
import { unixNow } from '../lib/time.mjs';

export const POW_TTL = 300;
export const POW_MAX_BITS = 30;
const CHALLENGE = /^(\d{1,12})\.([0-9a-f]{32})\.([A-Za-z0-9_-]{43})$/;
const NONCE = /^[0-9a-z]{1,16}$/;

export const powBits = (cfg) => {
  const n = Math.trunc(Number(cfg?.['site.pow.bits']));
  return Number.isFinite(n) ? Math.min(Math.max(n, 0), POW_MAX_BITS) : 0;
};

export const leadingZeroBits = (buf) => {
  let n = 0;
  for (const byte of buf) {
    if (byte === 0) { n += 8; continue; }
    return n + Math.clz32(byte) - 24;
  }
  return n;
};

export const createPow = ({ key = createHmac('sha256', SECRET).update('pow').digest(), now = unixNow, ttl = POW_TTL } = {}) => {
  const used = new Map();
  const sign = (body) => createHmac('sha256', key).update(body).digest('base64url');
  const prune = (t) => { for (const [c, end] of used) if (end < t) used.delete(c); };

  const challenge = () => {
    const body = `${now() + ttl}.${randomBytes(16).toString('hex')}`;
    return `${body}.${sign(body)}`;
  };

  const check = (proof, bits) => {
    const text = typeof proof === 'string' ? proof : '';
    if (!text) return 'missing';
    const cut = text.lastIndexOf(':');
    const c = text.slice(0, cut);
    const nonce = text.slice(cut + 1);
    const m = cut > 0 ? CHALLENGE.exec(c) : null;
    if (!m || !NONCE.test(nonce) || !safeEqual(m[3], sign(`${m[1]}.${m[2]}`))) return 'bad';
    const t = now();
    if (Number(m[1]) < t) return 'expired';
    if (used.has(c)) return 'used';
    if (leadingZeroBits(createHash('sha256').update(`${c}:${nonce}`).digest()) < bits) return 'bad';
    if (used.size >= 1000) prune(t);
    used.set(c, Number(m[1]));
    return 'ok';
  };

  return { challenge, check };
};
