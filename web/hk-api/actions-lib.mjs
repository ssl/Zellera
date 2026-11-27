import { fmtDate, fmtDateTime } from '../cms/lib/html.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { higherRank } from './_guard.mjs';

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

export const unbanRefusal = (ctx, ban) => higherRank(ctx.staff, ban.target_rank, ban.username ?? `User #${ban.player_id}`);
