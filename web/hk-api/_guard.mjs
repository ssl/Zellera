import { crossSite } from '../cms/lib/http.mjs';
import { sql } from '../cms/lib/sql.mjs';

export const owner = 'shared';

export const HK_PERMS = Object.freeze([
  'moderation.tool',
  'moderation.tickets',
  'moderation.room.info',
  'moderation.room.chatlog',
  'moderation.room.alert',
  'moderation.player.chatlog',
  'moderation.player.ban',
  'moderation.player.alert',
  'moderation.player.kick',
  'housekeeping.wallet',
  'housekeeping.badges',
  'housekeeping.ranks',
  'housekeeping.landing_view',
  'housekeeping.news',
  'housekeeping.youtube',
]);

export const grants = [
  { table: 'players', select: ['id', 'account_id', 'name', 'rank_id'] },
  { table: 'player_accounts', select: ['id', 'last_ip', 'machine_id'] },
  { table: 'ranks', select: ['id', 'name'] },
  { table: 'rank_permissions', select: ['rank_id', 'permission_code'] },
  { table: 'bans', select: ['id', 'player_id', 'ends_at', 'barred_address', 'barred_machine'] },
  { table: 'staff_actions', insert: ['acted_at', 'staff_id', 'action', 'subject_type', 'subject_id', 'subject_label', 'detail', 'success'] },
];

const activeBan = async (ctx, u) => {
  const ips = [...new Set([u.ip_current, ctx.ip].map((v) => String(v ?? '').trim()).filter(Boolean))];
  const machine = String(u.machine_id ?? '').trim();
  const conds = [sql`b.player_id = ${Number(u.id)}`];
  if (ips.length) conds.push(sql`b.barred_address IN (${sql.in(ips)})`);
  if (machine) conds.push(sql`b.barred_machine = ${machine}`);
  const [ban] = await ctx.db.query(sql`SELECT b.id FROM bans b
    WHERE b.ends_at > UNIX_TIMESTAMP() AND (${sql.join(conds, ' OR ')}) LIMIT 1`);
  return ban ?? null;
};

const staffOfRequest = new WeakMap();
export const loadStaff = (ctx) => {
  let found = staffOfRequest.get(ctx);
  if (!found) {
    found = lookupStaff(ctx);
    staffOfRequest.set(ctx, found);
  }
  return found;
};

export const showsHousekeepingLink = async (ctx) => {
  try {
    return (await loadStaff(ctx)).state === 'staff';
  } catch (e) {
    ctx.log?.(`housekeeping link: ${e?.message ?? e}`);
    return false;
  }
};

const lookupStaff = async (ctx) => {
  const s = await ctx.session();
  if (!s || !(Number(s.userId) > 0)) return { state: 'anonymous' };
  const [u] = await ctx.db.query(sql`SELECT p.id, p.name AS username, p.rank_id AS \`rank\`, a.last_ip AS ip_current, a.machine_id FROM players p LEFT JOIN player_accounts a ON a.id = p.account_id WHERE p.id = ${Number(s.userId)} LIMIT 1`);
  if (!u) return { state: 'anonymous' };
  const rank = Number(u.rank);
  const perms = new Map(HK_PERMS.map((k) => [k, false]));
  if (Number.isInteger(rank) && rank >= 0) {
    const defs = await ctx.db.query(sql`SELECT permission_code FROM rank_permissions
      WHERE rank_id = ${rank} AND permission_code IN (${sql.in(HK_PERMS)})`);
    for (const d of defs) perms.set(d.permission_code, true);
  }
  if (!perms.get('moderation.tool')) return { state: 'member' };
  if (await activeBan(ctx, u)) return { state: 'banned' };
  const [r] = await ctx.db.query(sql`SELECT name AS rank_name FROM ranks WHERE id = ${rank} LIMIT 1`);
  const staff = Object.freeze({
    id: Number(u.id), username: String(u.username), rank, rankName: String(r?.rank_name ?? `Rank ${rank}`),
    sessionId: String(s.sessionId ?? ''),
    can: (key) => perms.get(key) === true,
  });
  return { state: 'staff', staff };
};

export const requireStaff = async (ctx, res, perm) => {
  const r = await loadStaff(ctx);
  if (r.state === 'anonymous') {
    ctx.send.error(res, 401, 'Log in first', `Log in to ${ctx.GAME_NAME} with your staff account on the front page, then open this link again.`, { reason: 'login-required' });
    return null;
  }
  if (r.state === 'member') {
    ctx.send.notFound(res);
    return null;
  }
  if (r.state === 'banned') {
    ctx.send.error(res, 403, 'Your account is banned', 'A banned account can not use housekeeping.', { reason: 'banned' });
    return null;
  }
  ctx.staff = r.staff;
  const missing = [].concat(perm ?? []).filter((k) => !r.staff.can(k));
  if (missing.length > 0) {
    ctx.send.error(res, 403, 'Not for your rank', `Your rank (${r.staff.rankName}) cannot open this. It needs: ${missing.join(', ')}.`, { reason: 'permission', missing });
    return null;
  }
  return r.staff;
};

export const higherRank = (staff, rank, name) => (Number(rank) > staff.rank ? `${name} has a higher rank than you.` : null);

export const audit = (ctx, { action, targetType = 'user', targetId = 0, label = '', detail = '', success = true }) => {
  if (!ctx.staff) throw new Error('audit: no staff on this request');
  if (ctx.req && ctx.req.method !== 'POST' && crossSite(ctx.req)) {
    return Promise.reject(new Error(`audit: ${action} on a GET from another site (the API refuses those)`));
  }
  return ctx.db.audit({
    actorId: ctx.staff.id, targetType, targetId, targetLabel: label, action, detail, success,
  });
};
