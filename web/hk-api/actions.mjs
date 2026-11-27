import { fmtDateTime } from '../cms/lib/html.mjs';
import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import { intParam, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit, higherRank } from './_guard.mjs';
import { PERMANENT, banActive, banEnds, charCount, findUser, oneLine, unbanRefusal } from './actions-lib.mjs';
import { BAN_COLUMNS, banCovers, isPermanent, ref } from './users-shared.mjs';
import { SECONDS_PER_HOUR } from '../lib/time.mjs';

export const owner = 'hk-actions';

export const grants = [
  { table: 'players', select: ['id', 'account_id', 'name', 'rank_id', 'is_online'] },
  { table: 'player_accounts', select: ['id', 'last_ip', 'machine_id'] },
  { table: 'ranks', select: ['id', 'name'] },
  {
    table: 'bans',
    select: ['id', 'player_id', 'issued_by', 'issued_at', 'ends_at', 'lifted_by', 'reason', 'barred_address', 'barred_machine'],
    insert: ['player_id', 'issued_by', 'issued_at', 'ends_at', 'reason'],
    update: ['ends_at', 'lifted_by'],
  },
  COMMAND_GRANT,
];

const PERM = 'moderation.player.ban';
export const LENGTHS = Object.freeze([
  ['1', '1 hour', 1], ['24', '1 day', 24], ['72', '3 days', 72], ['168', '1 week', 168],
  ['720', '30 days', 720], ['8760', '1 year', 8760], ['permanent', 'Permanent', null],
]);
export const REASON_MAX = 200;
const LATIN1_PRINTABLE = /^[\x20-\x7E\xA0-\xFF]*$/;
const NOTE_MAX = 200;

const loadTarget = async (ctx, id) => {
  const [u] = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.rank_id AS \`rank\`, u.is_online AS online, r.name AS rank_name
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id WHERE u.id = ${id} LIMIT 1`);
  return u ?? null;
};

const queryActiveBans = (ctx, userId) => ctx.db.query(sql`SELECT ${BAN_COLUMNS}, s.name AS staff_name
  FROM bans b LEFT JOIN players s ON s.id = b.issued_by
  WHERE b.player_id = ${userId} AND b.ends_at > UNIX_TIMESTAMP() ORDER BY b.id DESC LIMIT 50`);

const activeBans = async (ctx, target) => {
  const rows = await queryActiveBans(ctx, Number(target.id));
  return rows.map((b) => ({
    id: Number(b.id), covers: banCovers(b), reason: String(b.reason ?? ''), bannedAt: Number(b.issued_at), expiresAt: Number(b.ends_at),
    permanent: isPermanent(b.ends_at), staff: ref(b.issued_by, b.staff_name),
    canLift: unbanRefusal(ctx, { ...b, player_id: target.id, username: target.username, target_rank: target.rank }) === null,
  }));
};

const targetOf = (t) => ({
  id: Number(t.id), name: String(t.username), rank: Number(t.rank), rankName: t.rank_name != null ? String(t.rank_name) : null,
  online: String(t.online) === '1',
});

const whyNot = (ctx, target) => higherRank(ctx.staff, target.rank, target.username);

export const validateBan = (form) => {
  const errors = [];
  let field = null;
  const hoursValue = String(form.get('hours') ?? '');
  const length = LENGTHS.find(([v]) => v === hoursValue);
  if (!length) { errors.push('Pick a length from the list.'); field = 'hours'; }
  const reason = oneLine(form.get('reason'));
  const before = errors.length;
  if (!reason) errors.push('Write a reason.');
  else if (charCount(reason) > REASON_MAX) errors.push(`The reason can be at most ${REASON_MAX} characters.`);
  else if (!LATIN1_PRINTABLE.test(reason)) {
    errors.push('The reason has characters the ban list can not store (emoji, or letters outside Western European languages). Use plain letters, digits and punctuation.');
  }
  if (errors.length > before) field ??= 'reason';
  return { errors, field, length, reason, hoursValue };
};

const banGet = async (req, res, ctx) => {
  const wanted = String(ctx.query.get('user') ?? '').trim();
  const form = { lengths: LENGTHS.map(([value, label]) => ({ value, label })), reasonMax: REASON_MAX };
  if (!wanted) return ctx.send.json(res, 200, { ok: true, target: null, ...form });
  const found = await findUser(ctx, wanted);
  const target = found && await loadTarget(ctx, Number(found.id));
  if (!target) return ctx.send.notFound(res, `No user has the name or id “${wanted.slice(0, 50)}”.`);
  const bans = await activeBans(ctx, target);
  const refusal = whyNot(ctx, target);
  if (refusal) return ctx.send.error(res, 403, 'Not for your rank', refusal, { target: targetOf(target), bans });
  return ctx.send.json(res, 200, { ok: true, target: targetOf(target), bans, ...form });
};

const banPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const now = unixNow();
  const userId = intParam(ctx.form.get('user'));
  const target = userId && await loadTarget(ctx, userId);
  if (!target) return ctx.send.notFound(res, 'No user has that id.');
  const label = String(target.username);
  const refusal = whyNot(ctx, target);
  if (refusal) {
    await audit(ctx, { action: 'hk.ban', targetType: 'user', targetId: Number(target.id), label, detail: `refused: ${refusal}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', refusal);
  }
  const { errors, field, length, reason } = validateBan(ctx.form);
  if (errors.length) return ctx.send.error(res, 400, 'Not banned', errors.join(' '), { field, errors });
  const [, lengthLabel, hours] = length;
  const expire = hours == null ? sql`${PERMANENT}` : sql`UNIX_TIMESTAMP() + ${hours * SECONDS_PER_HOUR}`;
  const ends = hours == null ? 'permanent' : `until ${fmtDateTime(now + hours * SECONDS_PER_HOUR)}`;
  let result;
  try {
    result = await ctx.db.write(sql`INSERT INTO bans (player_id, issued_by, issued_at, ends_at, reason)
      SELECT p.id, ${ctx.staff.id}, UNIX_TIMESTAMP(), ${expire}, ${reason}
      FROM players p
      WHERE p.id = ${Number(target.id)} AND p.rank_id <= ${ctx.staff.rank} LIMIT 1`);
  } catch (e) {
    await audit(ctx, { action: 'hk.ban', targetType: 'user', targetId: Number(target.id), label, detail: `failed (${lengthLabel}): ${reason}`, success: false });
    throw e;
  }
  if (Number(result?.affectedRows) !== 1) {
    await audit(ctx, { action: 'hk.ban', targetType: 'user', targetId: Number(target.id), label, detail: `not written (rank changed?): ${reason}`, success: false });
    return ctx.send.error(res, 409, 'Not banned', `${label} could not be banned: their rank may have changed. Open the ban page again.`);
  }
  await audit(ctx, {
    action: 'hk.ban', targetType: 'user', targetId: Number(target.id), label,
    detail: `ban #${result.insertId}, ${lengthLabel} (${ends}): ${reason}`,
  });
  const kicked = await kickBanned(ctx, Number(target.id));
  return ctx.send.json(res, 200, {
    ok: true, ban: { id: Number(result.insertId) }, userId: Number(target.id),
    message: kicked ? 'Banned.' : 'Banned. The game was not told: they stay online until they log out.',
  });
};

export const kickBanned = async (ctx, userId) => {
  try {
    await ctx.command('kickBanned', {}, { userId });
    return true;
  } catch (e) {
    ctx.log(`kickBanned of user ${userId}: ${e?.message ?? e}`);
    return false;
  }
};

const loadBan = async (ctx, id) => {
  const [b] = await ctx.db.query(sql`SELECT ${BAN_COLUMNS}, b.player_id,
      u.name AS username, u.rank_id AS target_rank, s.name AS staff_name
    FROM bans b LEFT JOIN players u ON u.id = b.player_id LEFT JOIN players s ON s.id = b.issued_by
    WHERE b.id = ${id} LIMIT 1`);
  return b ?? null;
};

const banOf = (b, now) => ({
  id: Number(b.id), user: ref(b.player_id, b.username), covers: banCovers(b), reason: String(b.reason ?? ''),
  staff: ref(b.issued_by, b.staff_name), bannedAt: Number(b.issued_at), expiresAt: Number(b.ends_at),
  permanent: isPermanent(b.ends_at), active: banActive(b, now),
});

const unbanGet = async (req, res, ctx) => {
  const id = intParam(ctx.query.get('ban'));
  const ban = id && await loadBan(ctx, id);
  if (!ban) return ctx.send.notFound(res, 'No ban has that id.');
  const refusal = unbanRefusal(ctx, ban);
  if (refusal) return ctx.send.error(res, 403, 'Not for your rank', refusal, { ban: banOf(ban, unixNow()) });
  return ctx.send.json(res, 200, { ok: true, ban: banOf(ban, unixNow()), noteMax: NOTE_MAX });
};

const unbanPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const now = unixNow();
  const id = intParam(ctx.form.get('ban'));
  const ban = id && await loadBan(ctx, id);
  if (!ban) return ctx.send.notFound(res, 'No ban has that id.');
  const userId = Number(ban.player_id);
  const label = String(ban.username ?? '');
  const refusal = unbanRefusal(ctx, ban);
  if (refusal) {
    await audit(ctx, { action: 'hk.unban', targetType: 'user', targetId: userId, label, detail: `ban #${ban.id} refused: ${refusal}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', refusal);
  }
  const note = oneLine(ctx.form.get('note'));
  if (charCount(note) > NOTE_MAX) return ctx.send.error(res, 400, 'Not lifted', `The note can be at most ${NOTE_MAX} characters.`, { field: 'note' });
  const about = `ban #${ban.id} (${banCovers(ban)}, ${banEnds(ban.ends_at)}: ${ban.reason})${note ? `; note: ${note}` : ''}`;
  let result;
  try {
    const { staff } = ctx;
    result = await ctx.db.write(sql`UPDATE bans b
        LEFT JOIN players t ON t.id = b.player_id
      SET b.ends_at = UNIX_TIMESTAMP(), b.lifted_by = ${staff.id}
      WHERE b.id = ${Number(ban.id)} AND b.ends_at > UNIX_TIMESTAMP() AND COALESCE(t.rank_id, 0) <= ${staff.rank}`);
  } catch (e) {
    await audit(ctx, { action: 'hk.unban', targetType: 'user', targetId: userId, label, detail: `failed: ${about}`, success: false });
    throw e;
  }
  if (Number(result?.affectedRows) !== 1) {
    const fresh = (await loadBan(ctx, Number(ban.id))) ?? ban;
    const active = banActive(fresh, unixNow());
    const late = active ? unbanRefusal(ctx, fresh) : null;
    const why = !active ? 'already ended' : late ? `refused: ${late}` : 'not lifted';
    await audit(ctx, { action: 'hk.unban', targetType: 'user', targetId: userId, label, detail: `${why}: ${about}`, success: false });
    const shown = banOf(active ? fresh : { ...fresh, ends_at: Math.min(Number(fresh.ends_at), now) }, now);
    return late
      ? ctx.send.error(res, 403, 'Not for your rank', late, { ban: shown })
      : ctx.send.error(res, 409, 'Not lifted', active ? 'The ban was not lifted. Open it again.' : 'This ban has ended already. Nothing to lift.', { ban: shown });
  }
  await audit(ctx, { action: 'hk.unban', targetType: 'user', targetId: userId, label, detail: about });
  return ctx.send.json(res, 200, { ok: true, userId: userId > 0 ? userId : null, message: 'Unbanned. They can log in again.' });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/actions/ban`, perm: PERM, handler: banGet },
  { method: 'POST', path: `${HK_API}/actions/ban`, perm: PERM, handler: banPost },
  { method: 'GET', path: `${HK_API}/actions/unban`, perm: PERM, handler: unbanGet },
  { method: 'POST', path: `${HK_API}/actions/unban`, perm: PERM, handler: unbanPost },
];
