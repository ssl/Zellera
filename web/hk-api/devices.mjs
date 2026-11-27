import { intParam, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit, higherRank } from './_guard.mjs';
import { findUser } from './actions-lib.mjs';
import {
  COMBO, PIECES, anyOrEqual, deviceBanCatch, deviceOf, loadUser, parseReading, piecesEqual, readingOf, usable, userNotFound,
} from './devices-shared.mjs';
import { ref, userIdParam, userOf } from './users-shared.mjs';

export const owner = 'hk-devices';

export const grants = [
  { table: 'players', select: ['id', 'name', 'figure', 'gender', 'rank_id', 'is_online', 'last_online_at'] },
  { table: 'ranks', select: ['id', 'name'] },
  { table: 'player_devices', select: ['player_id', ...PIECES, 'first_seen_at', 'last_seen_at', 'logins'] },
  { table: 'device_bans', select: ['id', ...PIECES.map((p) => `p_${p}`), 'staff_id', 'expires_at'] },
  { table: 'device_ban_exemptions', select: ['player_id', 'staff_id', 'created_at'], insert: ['player_id', 'staff_id', 'created_at'], delete: true },
  { table: 'bans', select: ['player_id', 'issued_by', 'ends_at', 'avatar_only'] },
];

const PERM = 'moderation.player.ban';
const DEVICE_ROWS = 50;
const COMPARE_ROWS = 20;
const SHARED_PAGE = 50;

const n = (v) => Number(v) || 0;
const comboKey = (row) => COMBO.map((name) => row[name]).join('-');

const devicesOf = (ctx, userId, limit) => ctx.db.query(sql`SELECT d.id, d.gpu, d.scr, d.cpu, d.tz, d.fnt, d.cnv, d.aud, d.gl, d.ua,
    d.first_seen_at, d.last_seen_at, d.logins
  FROM player_devices d WHERE d.player_id = ${userId} ORDER BY d.last_seen_at DESC LIMIT ${limit}`);

const sharedCounts = async (ctx, userId, rows) => {
  const ids = [...new Set(rows.map((r) => String(r.id)).filter(usable))];
  const combos = [...new Map(rows.filter((r) => COMBO.every((name) => usable(r[name]))).map((r) => [comboKey(r), r])).values()];
  const [byId, byCombo] = await Promise.all([
    ids.length ? ctx.db.query(sql`SELECT d.id, COUNT(DISTINCT d.player_id) AS n FROM player_devices d
      WHERE d.id IN (${sql.in(ids)}) AND d.player_id <> ${userId} GROUP BY d.id`) : [],
    combos.length ? ctx.db.query(sql`SELECT d.gpu, d.scr, d.fnt, COUNT(DISTINCT d.player_id) AS n FROM player_devices d
      WHERE (${sql.join(combos.map((c) => sql`(${piecesEqual(c, COMBO)})`), ' OR ')}) AND d.player_id <> ${userId}
      GROUP BY d.gpu, d.scr, d.fnt`) : [],
  ]);
  return { id: new Map(byId.map((r) => [String(r.id), n(r.n)])), combo: new Map(byCombo.map((r) => [comboKey(r), n(r.n)])) };
};

const matchingBans = async (ctx, userId, now) => {
  const rows = await ctx.db.query(sql`SELECT b.id AS ban_id, d.id, d.gpu, d.scr, d.cpu, d.tz, d.fnt, d.cnv, d.aud, d.gl, d.ua
    FROM player_devices d JOIN device_bans b ON ${anyOrEqual(PIECES)}
    WHERE d.player_id = ${userId} AND (b.expires_at IS NULL OR b.expires_at > ${now}) ORDER BY b.id LIMIT 500`);
  const out = new Map();
  for (const r of rows) out.set(readingOf(r), [...(out.get(readingOf(r)) ?? []), n(r.ban_id)]);
  return out;
};

const exemptRefusal = (staff, u) => higherRank(staff, u.rank_id, u.username);

const devices = async (req, res, ctx) => {
  const u = await loadUser(ctx, userIdParam(ctx.params.id));
  if (!u) return userNotFound(ctx, res);
  const id = Number(u.id);
  const now = unixNow();
  const [rows, [exemption], banned] = await Promise.all([
    devicesOf(ctx, id, DEVICE_ROWS + 1),
    ctx.db.query(sql`SELECT e.staff_id, e.created_at, s.name AS staff_name
      FROM device_ban_exemptions e LEFT JOIN players s ON s.id = e.staff_id WHERE e.player_id = ${id} LIMIT 1`),
    matchingBans(ctx, id, now),
  ]);
  const shown = rows.slice(0, DEVICE_ROWS);
  const counts = await sharedCounts(ctx, id, shown);
  const { staff } = ctx;
  return ctx.send.json(res, 200, {
    ok: true,
    user: userOf(u),
    exempt: exemption ? { at: n(exemption.created_at), staff: ref(exemption.staff_id, exemption.staff_name) } : null,
    can: { ban: staff.can(PERM), exempt: staff.can(PERM) && exemptRefusal(staff, u) === null },
    devices: shown.map((r) => ({
      ...deviceOf(r),
      sharedId: usable(r.id) ? counts.id.get(String(r.id)) ?? 0 : null,
      sharedCombo: COMBO.every((name) => usable(r[name])) ? counts.combo.get(comboKey(r)) ?? 0 : null,
      bans: banned.get(readingOf(r)) ?? [],
    })),
    more: rows.length > DEVICE_ROWS,
  });
};

const shared = async (req, res, ctx) => {
  const u = await loadUser(ctx, userIdParam(ctx.params.id));
  if (!u) return userNotFound(ctx, res);
  const id = Number(u.id);
  const names = ctx.query.has('id') ? ['id'] : COMBO;
  const values = Object.fromEntries(names.map((name) => [name, String(ctx.query.get(name) ?? '')]));
  const shape = (name) => (name === 'id' ? /^[0-9a-f]{16}$/ : /^[0-9a-f]{8}$/);
  if (names.some((name) => !shape(name).test(values[name]) || !usable(values[name]))) {
    return ctx.send.error(res, 400, 'Bad request', 'Give ?id= (16 hex characters) or ?gpu=&scr=&fnt= (8 each), usable pieces only.');
  }
  const [own] = await ctx.db.query(sql`SELECT 1 AS ok FROM player_devices d WHERE d.player_id = ${id} AND ${piecesEqual(values, names)} LIMIT 1`);
  if (!own) return ctx.send.notFound(res, `${u.username} has no device with these pieces.`);
  const pageNo = intParam(ctx.query.get('page'), { max: 1000 }) ?? 1;
  const now = unixNow();
  const [rows, [total]] = await Promise.all([
    ctx.db.query(sql`SELECT d.player_id, MIN(d.first_seen_at) AS first_seen, MAX(d.last_seen_at) AS last_seen, SUM(d.logins) AS logins
      FROM player_devices d WHERE ${piecesEqual(values, names)} AND d.player_id <> ${id}
      GROUP BY d.player_id ORDER BY d.player_id DESC LIMIT ${SHARED_PAGE + 1} OFFSET ${(pageNo - 1) * SHARED_PAGE}`),
    ctx.db.query(sql`SELECT COUNT(DISTINCT d.player_id) AS n FROM player_devices d WHERE ${piecesEqual(values, names)} AND d.player_id <> ${id}`),
  ]);
  const shown = rows.slice(0, SHARED_PAGE);
  const ids = shown.map((r) => n(r.player_id));
  const [players, bans, exempt] = ids.length ? await Promise.all([
    ctx.db.query(sql`SELECT p.id, p.name AS username, p.figure, p.gender, p.rank_id, r.name AS rank_name, p.is_online AS online,
        p.last_online_at AS last_online
      FROM players p LEFT JOIN ranks r ON r.id = p.rank_id WHERE p.id IN (${sql.in(ids)})`),
    ctx.db.query(sql`SELECT DISTINCT player_id FROM bans WHERE player_id IN (${sql.in(ids)}) AND ends_at > ${now}`),
    ctx.db.query(sql`SELECT player_id FROM device_ban_exemptions WHERE player_id IN (${sql.in(ids)})`),
  ]) : [[], [], []];
  const byId = new Map(players.map((p) => [n(p.id), p]));
  const bannedIds = new Set(bans.map((x) => n(x.player_id)));
  const exemptIds = new Set(exempt.map((x) => n(x.player_id)));
  return ctx.send.json(res, 200, {
    ok: true,
    user: userOf(u),
    by: names.length === 1 ? 'id' : 'combo',
    pieces: names.map((name) => ({ name, value: values[name] })),
    total: n(total?.n), page: pageNo, hasNext: rows.length > SHARED_PAGE,
    rows: shown.filter((r) => byId.has(n(r.player_id))).map((r) => ({
      ...userOf(byId.get(n(r.player_id))), firstSeen: n(r.first_seen), lastSeen: n(r.last_seen), logins: n(r.logins),
      banned: bannedIds.has(n(r.player_id)), exempt: exemptIds.has(n(r.player_id)),
    })),
  });
};

const pieceState = (a, b) => (!usable(a) || !usable(b) ? 'unusable' : a === b ? 'equal' : 'different');
const tally = (a, b) => {
  const states = PIECES.map((name) => pieceState(String(a[name]), String(b[name])));
  return { equal: states.filter((s) => s === 'equal').length, different: states.filter((s) => s === 'different').length };
};

const pickPair = (rowsA, rowsB, wantedA, wantedB) => {
  const asked = (rows, reading) => (parseReading(reading) ? rows.find((r) => readingOf(r) === reading) : null);
  const [a, b] = [asked(rowsA, wantedA), asked(rowsB, wantedB)];
  let best = null;
  for (const ra of a ? [a] : rowsA) {
    for (const rb of b ? [b] : rowsB) {
      const t = tally(ra, rb);
      if (!best || t.equal > best.t.equal || (t.equal === best.t.equal && t.different < best.t.different)) best = { ra, rb, t };
    }
  }
  return best;
};

const compare = async (req, res, ctx) => {
  const wanted = ['a', 'b'].map((k) => String(ctx.query.get(k) ?? '').trim());
  if (!wanted[0] || !wanted[1]) return ctx.send.json(res, 200, { ok: true, a: null, b: null, devices: null, pair: null });
  const users = [];
  for (const name of wanted) {
    const found = await findUser(ctx, name);
    const u = found && await loadUser(ctx, Number(found.id));
    if (!u) return ctx.send.notFound(res, `No user has the name or id “${name.slice(0, 50)}”.`);
    users.push(u);
  }
  const [rowsA, rowsB] = await Promise.all(users.map((u) => devicesOf(ctx, Number(u.id), COMPARE_ROWS)));
  const best = pickPair(rowsA, rowsB, String(ctx.query.get('da') ?? ''), String(ctx.query.get('db') ?? ''));
  const pieces = best ? PIECES.map((name) => ({
    name, a: String(best.ra[name]), b: String(best.rb[name]), state: pieceState(String(best.ra[name]), String(best.rb[name])),
  })) : [];
  const equal = (names) => names.every((name) => pieces.find((p) => p.name === name)?.state === 'equal');
  return ctx.send.json(res, 200, {
    ok: true,
    a: userOf(users[0]), b: userOf(users[1]),
    devices: { a: rowsA.map(deviceOf), b: rowsB.map(deviceOf) },
    pair: best ? {
      a: readingOf(best.ra), b: readingOf(best.rb), pieces, equal: best.t.equal, different: best.t.different,
      unusable: PIECES.length - best.t.equal - best.t.different,
      sameId: equal(['id']), sameCombo: equal(COMBO),
    } : null,
  });
};

const exemptPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const u = await loadUser(ctx, userIdParam(ctx.params.id));
  if (!u) return userNotFound(ctx, res);
  const id = Number(u.id);
  const label = String(u.username);
  const asked = String(ctx.form.get('exempt') ?? '');
  if (asked !== 'true' && asked !== 'false') return ctx.send.error(res, 400, 'Not changed', 'exempt is true or false.', { field: 'exempt' });
  const on = asked === 'true';
  const action = on ? 'hk.device.exempt' : 'hk.device.unexempt';
  const refusal = exemptRefusal(ctx.staff, u);
  if (refusal) {
    await audit(ctx, { action, targetId: id, label, detail: `refused: ${refusal}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', refusal);
  }
  if (on) {
    const caught = await deviceBanCatch(ctx, id, unixNow());
    const late = caught.matched ? higherRank(ctx.staff, caught.topRank, 'A device ban on this player') : null;
    if (late) {
      await audit(ctx, { action, targetId: id, label, detail: `refused: ${late}`, success: false });
      return ctx.send.error(res, 403, 'Not for your rank', late);
    }
  }
  let result;
  try {
    result = on
      ? await ctx.db.write(sql`INSERT INTO device_ban_exemptions (player_id, staff_id, created_at)
          SELECT p.id, ${ctx.staff.id}, UNIX_TIMESTAMP() FROM players p
          WHERE p.id = ${id} AND p.rank_id <= ${ctx.staff.rank}
            AND NOT EXISTS (SELECT 1 FROM device_ban_exemptions e WHERE e.player_id = p.id)`)
      : await ctx.db.remove(sql`DELETE FROM device_ban_exemptions WHERE player_id = ${id}`);
  } catch (e) {
    await audit(ctx, { action, targetId: id, label, detail: 'failed', success: false });
    throw e;
  }
  if (Number(result?.affectedRows) !== 1) {
    return ctx.send.json(res, 200, { ok: true, unchanged: true, exempt: on, message: on ? 'Was exempt already.' : 'Was not exempt.' });
  }
  await audit(ctx, { action, targetId: id, label, detail: on ? 'no device ban refuses this player' : 'device bans count again' });
  return ctx.send.json(res, 200, {
    ok: true, exempt: on,
    message: on ? 'Exempt: no device ban refuses this player from the next login on.' : 'Not exempt any more: device bans count again.',
  });
};

const USER = `${HK_API}/users/:id(\\d{1,10})`;

export const routes = [
  { method: 'GET', path: `${USER}/devices`, perm: 'moderation.tool', handler: devices },
  { method: 'GET', path: `${USER}/devices/shared`, perm: 'moderation.tool', handler: shared },
  { method: 'GET', path: `${HK_API}/devices/compare`, perm: 'moderation.tool', handler: compare },
  { method: 'POST', path: `${USER}/device-exemption`, perm: PERM, handler: exemptPost },
];
