import { createHash, createHmac, randomBytes, randomInt, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';
import { createGate } from '../lib/limits.mjs';
import { SECRET } from './config.mjs';
import { SECONDS_PER_DAY, unixNow } from '../lib/time.mjs';

const scryptAsync = promisify(scrypt);

export const safeEqual = (a, b) => {
  const ha = createHash('sha256').update(String(a)).digest();
  const hb = createHash('sha256').update(String(b)).digest();
  return timingSafeEqual(ha, hb) && String(a).length === String(b).length;
};

const LOG_N = 16;
const SALT_LEN = 16;
const KEY_LEN = 28;
const gateSize = (name, dflt) => { const n = Number(process.env[name]); return Number.isInteger(n) && n > 0 ? n : dflt; };
export const SCRYPT_GATE = Object.freeze({ max: gateSize('ZEL_WEB_SCRYPT_MAX', 2), queue: gateSize('ZEL_WEB_SCRYPT_QUEUE', 32) });
export const LOGIN_GATE = Object.freeze({ max: gateSize('ZEL_WEB_LOGIN_SCRYPT_MAX', 2), queue: gateSize('ZEL_WEB_LOGIN_SCRYPT_QUEUE', 32) });
const scryptGate = createGate(SCRYPT_GATE.max, SCRYPT_GATE.queue);
const loginGate = createGate(LOGIN_GATE.max, LOGIN_GATE.queue);
const derive = (password, salt, logN, gate = scryptGate) => gate.run(() =>
  scryptAsync(password.normalize('NFKC'), salt, KEY_LEN, { N: 2 ** logN, r: 8, p: 1, maxmem: 256 * 2 ** logN * 8 }));

export const hashPassword = async (password, { login = false } = {}) => {
  const salt = randomBytes(SALT_LEN);
  const key = await derive(password, salt, LOG_N, login ? loginGate : scryptGate);
  return `s${LOG_N}$${Buffer.concat([salt, key]).toString('base64url')}`;
};

const FORMAT = /^s(1[4-7])\$([A-Za-z0-9_-]{59})$/;
const DUMMY = `s${LOG_N}$${Buffer.alloc(SALT_LEN + KEY_LEN).toString('base64url')}`;
export const verifyPassword = async (password, stored, { login = false } = {}) => {
  const ours = FORMAT.exec(stored ?? '');
  const m = ours ?? FORMAT.exec(DUMMY);
  const raw = Buffer.from(m[2], 'base64url');
  const key = await derive(password, raw.subarray(0, SALT_LEN), Number(m[1]), login ? loginGate : scryptGate);
  return timingSafeEqual(key, raw.subarray(SALT_LEN)) && ours !== null;
};
export const hashIsOutdated = (stored) => {
  const m = FORMAT.exec(stored ?? '');
  return m !== null && Number(m[1]) < LOG_N;
};

const SESSION_TTL = 7 * SECONDS_PER_DAY;
const hmac = (s) => createHmac('sha256', SECRET).update(s).digest('base64url');
const pwPrint = (passwordHash) => hmac(`pw:${passwordHash}`).slice(0, 16);

export const GUEST_SESSION_TTL = 365 * SECONDS_PER_DAY;

export const newSession = ({ accountId, avatarId = 0, password, ttl = SESSION_TTL }) => {
  const exp = unixNow() + ttl;
  const body = `v2.${accountId}.${avatarId}.${exp}.${randomBytes(12).toString('base64url')}.${pwPrint(password)}`;
  return { value: `${body}.${hmac(body)}`, maxAge: ttl };
};

const ID = /^\d{1,10}$/;
export const readSession = (value) => {
  const parts = (value ?? '').split('.');
  if (parts.length !== 7 || parts[0] !== 'v2') return null;
  const body = parts.slice(0, -1).join('.');
  if (!safeEqual(hmac(body), parts.at(-1))) return null;
  const [accountId, avatarId, exp, nonce, print] = parts.slice(1, 6);
  if (!ID.test(accountId) || !ID.test(avatarId) || Number(exp) < Date.now() / 1000) return null;
  return { accountId: Number(accountId), avatarId: Number(avatarId), nonce, exp: Number(exp), print };
};
export const sessionMatchesUser = (session, account) => account != null && safeEqual(session.print, pwPrint(account.password));

const PAGES_KEY = createHmac('sha256', SECRET).update('zel-pages-hmac-v1').digest();
export const pagesHmac = (s) => createHmac('sha256', PAGES_KEY).update(String(s)).digest('base64url');

const TICKET_ALPHABET = 'abcdefghijklmnopqrstuvwxyz123456789';
export const newTicket = () => {
  let t = 'zel.';
  for (let i = 0; i < 25; i++) t += TICKET_ALPHABET[randomInt(TICKET_ALPHABET.length)];
  return t;
};
