import { intParam, sql } from '../cms/lib/sql.mjs';

export const PERMANENT_FROM = 2147483000;

export const LOG_COLUMNS = ['id', 'acted_at', 'staff_id', 'action', 'subject_type', 'subject_id', 'subject_label', 'detail', 'success'];

export const userIdParam = (v) => intParam(v, { min: 1 });

export const maskIps = (s) => String(s ?? '')
  .replace(/\bip=[^\s,;]*/gi, 'ip=(hidden)')
  .replace(/\b\d{1,3}(?:\.\d{1,3}){3}\b/g, '(ip hidden)')
  .replace(/\b[0-9a-f]{1,4}(?::[0-9a-f]{0,4}){3,7}\b/gi, '(ip hidden)');

export const isPermanent = (unix) => Number(unix) >= PERMANENT_FROM;
export const isTradeLocked = (standing, now) => Boolean(standing) && Number(standing.trade_locked_until) > now;

const num = (v) => Number(v) || 0;
const on = (v) => String(v ?? '0') === '1' || v === true;

export const ref = (id, name) => (num(id) > 0 || name ? { id: num(id), name: name ? String(name) : null } : null);

export const userOf = (u) => ({
  id: num(u.id), name: String(u.username ?? ''), figure: String(u.figure ?? ''), gender: String(u.gender ?? 'M'),
  rank: num(u.rank_id ?? u.rank), rankName: u.rank_name != null ? String(u.rank_name) : null,
  ...('online' in u ? { online: on(u.online) } : {}),
  ...('last_online' in u ? { lastOnline: num(u.last_online) } : {}),
  ...('account_created' in u ? { created: num(u.account_created) } : {}),
});

export const logRow = (r) => ({
  id: num(r.id), at: num(r.acted_at), actor: ref(r.staff_id, r.staff_name || (num(r.staff_id) > 0 ? null : 'system')),
  action: String(r.action ?? ''), target: { type: String(r.subject_type ?? ''), id: num(r.subject_id), label: r.subject_label ? String(r.subject_label) : null },
  detail: maskIps(r.detail), success: num(r.success) === 1,
});

export const BAN_COLUMNS = sql`b.id, b.reason, b.issued_at, b.ends_at, b.issued_by, b.lifted_by,
  b.barred_address IS NOT NULL AS bars_address, b.barred_machine IS NOT NULL AS bars_machine`;

export const banCovers = (b) => ['player', on(b.bars_address) ? 'address' : '', on(b.bars_machine) ? 'machine' : ''].filter(Boolean).join(' + ');

export const banRow = (b, now) => ({
  id: num(b.id), covers: banCovers(b), reason: String(b.reason ?? ''), bannedAt: num(b.issued_at), expiresAt: num(b.ends_at),
  permanent: isPermanent(b.ends_at), active: num(b.ends_at) > now, lifted: num(b.lifted_by) > 0,
  staff: num(b.issued_by) > 0 ? ref(b.issued_by, b.staff_name) : null,
});

export const readCursor = (query) => {
  const before = intParam(query.get('before'));
  if (!before) return null;
  return { before, bid: intParam(query.get('bid')) };
};

export const nextCursor = (shown, more) => (more && shown.length ? { before: num(shown.at(-1).timestamp), bid: num(shown.at(-1).id) } : null);
