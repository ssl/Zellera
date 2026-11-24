import { fmtDate, fmtDateTime } from '../cms/lib/html.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { barsMore } from './users-shared.mjs';

export const PERMANENT = 2147483647;

export const findUser = async (ctx, input) => {
  const s = String(input ?? '').trim();
  if (!s || s.length > 50) return null;
  const id = intParam(s);
  if (id) {
    const [byId] = await ctx.db.query(sql`SELECT id, name AS username, rank_id AS \`rank\` FROM players WHERE id = ${id} LIMIT 1`);
    if (byId) return byId;
  }
  const [byName] = await ctx.db.query(sql`SELECT id, name AS username, rank_id AS \`rank\` FROM players WHERE name = ${s} LIMIT 1`);
  return byName ?? null;
};

export const banActive = (ban, now) => Number(ban.ends_at) > now;
export const banEnds = (expire) => (Number(expire) >= PERMANENT ? 'Permanent' : fmtDateTime(expire));

export const parseDay = (value) => {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value ?? '').trim());
  if (!m) return null;
  const [y, mo, d] = [Number(m[1]), Number(m[2]), Number(m[3])];
  const t = Date.UTC(y, mo - 1, d);
  const back = new Date(t);
  if (y < 2000 || back.getUTCFullYear() !== y || back.getUTCMonth() !== mo - 1 || back.getUTCDate() !== d) return null;
  return t / 1000;
};
export const dayString = (unix) => fmtDate(unix);

export const oneLine = (value) => String(value ?? '').replace(/\s+/g, ' ').trim();
export const charCount = (s) => [...String(s ?? '')].length;

export const topRank = async (ctx) => {
  const [r] = await ctx.db.query(sql`SELECT MAX(id) AS top FROM ranks`);
  return Number(r?.top) || 0;
};

export const unbanRefusal = (ctx, ban, top) => {
  const { staff } = ctx;
  if (Number(ban.player_id) === staff.id) return 'You can not lift a ban on your own account.';
  if (Number(ban.player_id) > 0 && Number(ban.target_rank) >= staff.rank) {
    return `${ban.username ?? `User #${ban.player_id}`} has a rank (${ban.target_rank}) that is not below yours (${staff.rankName}).`;
  }
  if (Number(ban.issued_by) > 0 && Number(ban.staff_rank) > staff.rank) {
    return `This ban was given by ${ban.staff_name ?? `user #${ban.issued_by}`}, whose rank is above yours (${staff.rankName}).`;
  }
  if (barsMore(ban) && staff.rank < top) {
    return 'Only the top rank can lift a ban that also keeps out an address or a machine: it covers every account there.';
  }
  return null;
};
