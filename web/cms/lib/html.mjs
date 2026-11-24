import { photoDir } from '../../lib/photo-files.mjs';

class SafeHtml {
  constructor(value) { this.value = String(value); }
  toString() { return this.value; }
}

const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;', '`': '&#96;' };
export const esc = (v) => String(v ?? '').replace(/[&<>"'`]/g, (c) => ESC[c]);

const part = (v) => {
  if (v == null || v === false) return '';
  if (v instanceof SafeHtml) return v.value;
  if (Array.isArray(v)) return v.map(part).join('');
  return esc(v);
};

export const html = (strings, ...values) => {
  let out = strings[0];
  for (let i = 0; i < values.length; i++) out += part(values[i]) + strings[i + 1];
  return new SafeHtml(out);
};

export const safeUrl = (u, { external = false } = {}) => {
  const s = String(u ?? '').trim();
  if (/^\/(?![/\\])[^\s]*$/.test(s)) return s;
  if (external && /^https?:\/\/[^\s/\\?#]+(?:[/?#][^\s]*)?$/i.test(s)) return s;
  return '#';
};

const pad = (n) => String(n).padStart(2, '0');
const toDate = (unix) => new Date(Number(unix) * 1000);
export const fmtDate = (unix) => {
  if (!(Number(unix) > 0)) return '-';
  const d = toDate(unix);
  return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;
};
export const fmtDateTime = (unix) => {
  if (!(Number(unix) > 0)) return '-';
  const d = toDate(unix);
  return `${fmtDate(unix)} ${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())} UTC`;
};
export const fmtNumber = (n) => Number(n ?? 0).toLocaleString('en-US');

const enc = encodeURIComponent;
const id = (v) => {
  const n = Number(v);
  if (!Number.isInteger(n) || n < 0) throw new TypeError(`url id must be a non-negative integer, got ${v}`);
  return n;
};
const qs = (params) => {
  const q = new URLSearchParams();
  for (const [k, v] of Object.entries(params ?? {})) if (v != null && v !== '') q.set(k, String(v));
  const s = q.toString();
  return s ? `?${s}` : '';
};

export const urls = {
  home: () => '/',
  me: () => '/me',
  profile: (name) => `/profile/${enc(name)}`,
  group: (groupId) => `/groups/${id(groupId)}/id`,
  help: (topic) => (topic ? `/help/${enc(topic)}` : '/help'),
  ask: () => '/help/ask',
  safety: () => '/safety',
  terms: () => '/legal/terms',
  privacy: () => '/legal/privacy',

  photoImage: (photoUuid) => `/usercontent/${photoDir()}/${enc(photoUuid)}.png`,

  hkUsers: (q) => `/housekeeping/users${qs({ q })}`,
  hkUser: (userId) => `/housekeeping/users/${id(userId)}`,
  hkUserSanctions: (userId) => `/housekeeping/users/${id(userId)}/sanctions`,
};
