import { fmtDateTime } from '../cms/lib/html.mjs';
import { intParam, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';
import { LENGTHS, REASON_MAX, kickBanned, validateBan } from './actions.mjs';
import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import {
  COMBO, MIN_WITHOUT_ID, PIECES, anyOrEqual, deviceOf, loadDevice, loadUser, patternEqual, patternHas, patternRefusal, patternText,
  pieceNames, piecesEqual, userNotFound,
} from './devices-shared.mjs';
import { ref, userIdParam, userOf } from './users-shared.mjs';
import { SECONDS_PER_HOUR } from '../lib/time.mjs';

export const owner = 'hk-devices';

const P_COLUMNS = PIECES.map((p) => `p_${p}`);

export const grants = [
  { table: 'players', select: ['id', 'name', 'figure', 'gender', 'rank_id'] },
  { table: 'ranks', select: ['id', 'name'] },
  { table: 'player_devices', select: ['player_id', ...PIECES, 'first_seen_at', 'last_seen_at', 'logins'] },
  {
    table: 'device_bans',
    select: ['id', ...P_COLUMNS, 'reason', 'staff_id', 'created_at', 'expires_at'],
    insert: [...P_COLUMNS, 'reason', 'staff_id', 'created_at', 'expires_at'],
    delete: true,
  },
  COMMAND_GRANT,
];

const PERM = 'moderation.player.ban';
const PAGE = 50;
const REST = PIECES.filter((name) => name !== 'id');

const n = (v) => Number(v) || 0;
const active = (row, now) => row.expires_at == null || Number(row.expires_at) > now;
const patternOf = (row) => Object.fromEntries(PIECES.map((name) => [name, row[`p_${name}`] != null ? String(row[`p_${name}`]) : null]));

const matchCounts = async (ctx, ids, now) => {
  if (ids.length === 0) return new Map();
  const these = sql`b.id IN (${sql.in(ids)}) AND (b.expires_at IS NULL OR b.expires_at > ${now})`;
  const rows = await ctx.db.query(sql`SELECT b.id, COUNT(DISTINCT d.player_id) AS n
      FROM device_bans b JOIN player_devices d ON ${patternEqual(['id'])} AND ${anyOrEqual(REST)}
      WHERE ${these} AND ${patternHas(['id'])} GROUP BY b.id
    UNION ALL
    SELECT b.id, COUNT(DISTINCT d.player_id) AS n
      FROM device_bans b JOIN player_devices d ON ${patternEqual(COMBO)} AND ${anyOrEqual(REST.filter((name) => !COMBO.includes(name)))}
      WHERE ${these} AND b.p_id IS NULL AND ${patternHas(COMBO)} GROUP BY b.id
    UNION ALL
    SELECT b.id, COUNT(DISTINCT d.player_id) AS n
      FROM device_bans b JOIN player_devices d ON ${anyOrEqual(REST)}
      WHERE ${these} AND b.p_id IS NULL AND NOT (${patternHas(COMBO)}) GROUP BY b.id`);
  return new Map(rows.map((r) => [n(r.id), n(r.n)]));
};

const list = async (req, res, ctx) => {
  const pageNo = intParam(ctx.query.get('page'), { max: 1000 }) ?? 1;
  const now = unixNow();
  const rows = await ctx.db.query(sql`SELECT b.id, b.p_id, b.p_gpu, b.p_scr, b.p_cpu, b.p_tz, b.p_fnt, b.p_cnv, b.p_aud, b.p_gl, b.p_ua,
      b.reason, b.staff_id, b.created_at, b.expires_at, s.name AS staff_name, s.rank_id AS staff_rank
    FROM device_bans b LEFT JOIN players s ON s.id = b.staff_id
    ORDER BY b.id DESC LIMIT ${PAGE + 1} OFFSET ${(pageNo - 1) * PAGE}`);
  const shown = rows.slice(0, PAGE);
  const counts = await matchCounts(ctx, shown.map((r) => n(r.id)), now);
  const { staff } = ctx;
  return ctx.send.json(res, 200, {
    ok: true, page: pageNo, hasNext: rows.length > PAGE,
    can: { ban: staff.can(PERM) },
    patterns: shown.map((r) => ({
      id: n(r.id), pieces: patternOf(r), reason: String(r.reason ?? ''), staff: ref(r.staff_id, r.staff_name),
      createdAt: n(r.created_at), expiresAt: r.expires_at == null ? null : n(r.expires_at), active: active(r, now),
      matches: active(r, now) ? counts.get(n(r.id)) ?? 0 : null,
      canDelete: staff.can(PERM) && n(r.staff_rank) <= staff.rank,
    })),
  });
};

const source = async (ctx, res, userId, reading) => {
  const u = await loadUser(ctx, userIdParam(userId));
  if (!u) { userNotFound(ctx, res); return null; }
  const row = await loadDevice(ctx, Number(u.id), reading);
  if (!row) { ctx.send.notFound(res, `${u.username} has no device with this reading.`); return null; }
  return { u, row };
};

const matchInfo = async (ctx, row, names) => {
  const [m] = await ctx.db.query(sql`SELECT COUNT(DISTINCT d.player_id) AS n, COALESCE(MAX(p.rank_id), 0) AS top,
      COALESCE(MAX(d.player_id = ${ctx.staff.id}), 0) AS own
    FROM player_devices d JOIN players p ON p.id = d.player_id WHERE ${piecesEqual(row, names)}`);
  return { matches: n(m?.n), top: n(m?.top), own: n(m?.own) === 1 };
};

const rankRefusal = (staff, info) => {
  if (info.own) return 'This pattern matches a device of your own account: it would keep you out as well.';
  if (info.top >= staff.rank) return `This pattern matches an account with a rank (${info.top}) that is not below yours (${staff.rankName}).`;
  return null;
};

const newGet = async (req, res, ctx) => {
  const src = await source(ctx, res, ctx.query.get('user'), ctx.query.get('reading'));
  if (!src) return;
  return ctx.send.json(res, 200, {
    ok: true, user: userOf(src.u), device: deviceOf(src.row),
    lengths: LENGTHS.map(([value, label]) => ({ value, label: value === 'permanent' ? 'No end' : label })),
    reasonMax: REASON_MAX, minWithoutId: MIN_WITHOUT_ID,
  });
};

const count = async (req, res, ctx) => {
  const src = await source(ctx, res, ctx.query.get('user'), ctx.query.get('reading'));
  if (!src) return;
  const names = pieceNames(ctx.query.get('pieces'));
  const refusal = patternRefusal(src.row, names);
  if (refusal) return ctx.send.error(res, 400, 'No pattern', refusal, { field: 'pieces' });
  const info = await matchInfo(ctx, src.row, names);
  return ctx.send.json(res, 200, { ok: true, pieces: names, matches: info.matches, refusal: rankRefusal(ctx.staff, info) });
};

const createPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const src = await source(ctx, res, ctx.form.get('user'), ctx.form.get('reading'));
  if (!src) return;
  const { u, row } = src;
  const label = String(u.username);
  const names = pieceNames(ctx.form.get('pieces'));
  const refusal = patternRefusal(row, names);
  if (refusal) return ctx.send.error(res, 400, 'Not banned', refusal, { field: 'pieces' });
  const { errors, field, length, reason } = validateBan(ctx.form);
  if (errors.length) return ctx.send.error(res, 400, 'Not banned', errors.join(' '), { field, errors });
  const pattern = Object.fromEntries(PIECES.map((name) => [name, names.includes(name) ? String(row[name]) : null]));
  const about = `pattern (${patternText(pattern)})`;
  const info = await matchInfo(ctx, row, names);
  const late = rankRefusal(ctx.staff, info);
  if (late) {
    await audit(ctx, { action: 'hk.device.ban', targetId: Number(u.id), label, detail: `refused: ${about}: ${late}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', late, { matches: info.matches });
  }
  const seen = intParam(ctx.form.get('seen'), { min: 0 });
  if (seen == null) return ctx.send.error(res, 400, 'Not banned', 'Look at how many accounts the pattern matches first.', { field: 'pieces', matches: info.matches });
  if (seen !== info.matches) {
    return ctx.send.error(res, 409, 'Not banned', `The pattern matches ${info.matches} ${info.matches === 1 ? 'account' : 'accounts'} now, not the ${seen} the page showed. Look again, then save.`, { matches: info.matches });
  }
  const now = unixNow();
  const same = sql.join(PIECES.map((name) => sql`${sql.ident(`p_${name}`, P_COLUMNS)} <=> ${pattern[name]}`), ' AND ');
  const [twin] = await ctx.db.query(sql`SELECT id FROM device_bans WHERE ${same} AND (expires_at IS NULL OR expires_at > ${now}) LIMIT 1`);
  if (twin) return ctx.send.error(res, 409, 'Not banned', `Device ban #${twin.id} holds exactly this pattern already.`, { pattern: { id: n(twin.id) } });
  const [, lengthLabel, hours] = length;
  const expire = hours == null ? sql`NULL` : sql`UNIX_TIMESTAMP() + ${hours * SECONDS_PER_HOUR}`;
  const ends = hours == null ? 'no end' : `${lengthLabel}, until ${fmtDateTime(now + hours * SECONDS_PER_HOUR)}`;
  let result;
  try {
    result = await ctx.db.write(sql`INSERT INTO device_bans (p_id, p_gpu, p_scr, p_cpu, p_tz, p_fnt, p_cnv, p_aud, p_gl, p_ua, reason, staff_id, created_at, expires_at)
      VALUES (${sql.join(PIECES.map((name) => sql`${pattern[name]}`))}, ${reason}, ${ctx.staff.id}, UNIX_TIMESTAMP(), ${expire})`);
  } catch (e) {
    await audit(ctx, { action: 'hk.device.ban', targetId: Number(u.id), label, detail: `failed: ${about}: ${reason}`, success: false });
    throw e;
  }
  await audit(ctx, {
    action: 'hk.device.ban', targetId: Number(u.id), label,
    detail: `device ban #${result.insertId}, ${about}, ${ends}, matches ${info.matches}: ${reason}`,
  });
  const kicked = await kickBanned(ctx, Number(u.id));
  return ctx.send.json(res, 200, {
    ok: true, pattern: { id: Number(result.insertId) }, matches: info.matches,
    message: kicked ? 'Saved.' : 'Saved. The game was not told: who is online now stays until they leave.',
  });
};

const deletePost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const id = intParam(ctx.params.id);
  const [row] = id ? await ctx.db.query(sql`SELECT b.id, b.p_id, b.p_gpu, b.p_scr, b.p_cpu, b.p_tz, b.p_fnt, b.p_cnv, b.p_aud, b.p_gl, b.p_ua,
      b.reason, b.staff_id, s.name AS staff_name, s.rank_id AS staff_rank
    FROM device_bans b LEFT JOIN players s ON s.id = b.staff_id WHERE b.id = ${id} LIMIT 1`) : [];
  if (!row) return ctx.send.notFound(res, 'No device ban has that id.');
  const { staff } = ctx;
  const target = { targetType: 'device', targetId: id, label: `Device ban #${id}` };
  const about = `pattern (${patternText(patternOf(row))}): ${row.reason}`;
  if (n(row.staff_rank) > staff.rank) {
    const refusal = `This device ban was made by ${row.staff_name ?? `user #${row.staff_id}`}, whose rank is above yours (${staff.rankName}).`;
    await audit(ctx, { action: 'hk.device.unban', ...target, detail: `refused: ${refusal}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', refusal);
  }
  let result;
  try {
    result = await ctx.db.remove(sql`DELETE b FROM device_bans b LEFT JOIN players i ON i.id = b.staff_id
      WHERE b.id = ${id} AND COALESCE(i.rank_id, 0) <= ${staff.rank}`);
  } catch (e) {
    await audit(ctx, { action: 'hk.device.unban', ...target, detail: `failed: ${about}`, success: false });
    throw e;
  }
  if (Number(result?.affectedRows) !== 1) {
    await audit(ctx, { action: 'hk.device.unban', ...target, detail: `not deleted: ${about}`, success: false });
    return ctx.send.error(res, 409, 'Not deleted', 'The device ban was not deleted: it is gone already, or it changed. Open the list again.');
  }
  await audit(ctx, { action: 'hk.device.unban', ...target, detail: `deleted ${about}` });
  return ctx.send.json(res, 200, { ok: true, message: `Device ban #${id} is deleted. Its devices can log in again.` });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/device-bans`, perm: 'moderation.tool', handler: list },
  { method: 'GET', path: `${HK_API}/device-bans/new`, perm: PERM, handler: newGet },
  { method: 'GET', path: `${HK_API}/device-bans/count`, perm: PERM, handler: count },
  { method: 'POST', path: `${HK_API}/device-bans`, perm: PERM, handler: createPost },
  { method: 'POST', path: `${HK_API}/device-bans/:id(\\d{1,10})/delete`, perm: PERM, handler: deletePost },
];
