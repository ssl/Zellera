import { urls } from '../cms/lib/html.mjs';
import { intParam, likePrefix, sql, unixNow } from '../cms/lib/sql.mjs';
import { COMMAND_SELECT, walletCurrencies } from '../cms/lib/commands.mjs';
import { HK_API } from './index.mjs';
import { badgeNameOrCode } from '../lib/badges.mjs';
import { badgeImagePath } from '../lib/client-profile.mjs';
import { gameRules } from '../lib/game-rules.mjs';
import { audit } from './_guard.mjs';
import { PIECES, readingOf, usable } from './devices-shared.mjs';
import { topRank, unbanRefusal } from './actions-lib.mjs';
import { BADGE_PERM, CURRENCY_PERM, commandOf, editRefusal, rankLevels, rankRefusal } from './actions-user.mjs';
import {
  BAN_COLUMNS, LOG_COLUMNS, banRow, isTradeLocked, logRow, nextCursor, readCursor, ref, userIdParam, userOf,
} from './users-shared.mjs';

export const owner = 'hk-users';

export const grants = [
  {
    table: 'players',
    select: ['id', 'account_id', 'name', 'motto', 'rank_id', 'created_at', 'last_login_at', 'last_online_at', 'is_online', 'home_room_id'],
  },
  { table: 'player_accounts', select: ['id', 'registered_ip', 'last_ip', 'machine_id'] },
  { table: 'player_devices', select: ['player_id', ...PIECES, 'last_seen_at'] },
  { table: 'ranks', select: ['id', 'name'] },
  { table: 'rank_permissions', select: ['rank_id', 'permission_code'] },
  { table: 'player_balances', select: ['player_id', 'currency', 'amount'] },
  {
    table: 'player_standing',
    select: ['player_id', 'reports_sent', 'reports_abusive', 'warnings_received', 'bans_received', 'trade_lock_count', 'trade_locked_until',
      'safety_locked', 'muted_until'],
  },
  { table: 'player_stats', select: ['player_id', 'achievement_score', 'online_seconds'] },
  { table: 'bans', select: ['id', 'player_id', 'issued_by', 'issued_at', 'ends_at', 'lifted_by', 'reason', 'barred_address', 'barred_machine'] },
  { table: 'sanctions', select: ['id', 'player_id', 'step', 'kind', 'probation_ends_at', 'reason', 'ends_at'] },
  { table: 'sanction_steps', select: ['step', 'kind', 'hours', 'probation_days'] },
  { table: 'staff_actions', select: LOG_COLUMNS },
  { table: 'rank_changes', select: ['id', 'changed_at', 'actor_id', 'player_id', 'old_rank', 'new_rank', 'via'] },
  { table: 'room_chat_messages', select: ['id', 'room_id', 'sender_id', 'target_id', 'message', 'sent_at'] },
  { table: 'room_visits', select: ['id', 'room_id', 'player_id', 'entered_at', 'left_at'] },
  { table: 'rooms', select: ['id', 'name', 'owner_id', 'player_count', 'max_players', 'door_mode'] },
  { table: 'friendships', select: ['player_id'] },
  { table: 'player_badges', select: ['player_id', 'badge_code', 'slot'] },
  { table: 'web_commands', select: [...COMMAND_SELECT] },
];

const SEARCH_PAGE = 50;
const BADGE_ROWS = 500;
const COMMAND_ROWS = 10;
const LOG_PAGE = 100;
const SHARED_LIMIT = 50;
const MAX_Q = 64;

const n = (v) => Number(v) || 0;

const loadUser = async (ctx) => {
  const id = userIdParam(ctx.params.id);
  if (id == null) return null;
  const [u] = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id WHERE u.id = ${id} LIMIT 1`);
  return u ?? null;
};
const userNotFound = (ctx, res) => ctx.send.notFound(res, 'There is no user with this id.');
const roomOf = (id, name) => (n(id) > 0 ? { id: n(id), name: name != null ? String(name) : null } : null);

const search = async (req, res, ctx) => {
  const query = String(ctx.query.get('q') ?? '').trim().slice(0, MAX_Q);
  if (query && !ctx.rateLimit(res, 'hk-lookup', 60, 60)) return;
  const pageNo = intParam(ctx.query.get('page'), { max: 1000 }) ?? 1;
  const id = /^\d{1,10}$/.test(query) ? userIdParam(query) : null;
  const where = query
    ? sql`WHERE u.name LIKE ${likePrefix(query)}${id != null ? sql` OR u.id = ${id}` : sql``}`
    : sql``;
  const order = query
    ? sql`ORDER BY (u.id = ${id ?? -1}) DESC, (u.name = ${query}) DESC, u.name ASC, u.id ASC`
    : sql`ORDER BY u.id DESC`;
  const rows = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name, u.is_online AS online,
      u.last_online_at AS last_online, u.created_at AS account_created
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id
    ${where} ${order} LIMIT ${SEARCH_PAGE + 1} OFFSET ${(pageNo - 1) * SEARCH_PAGE}`);
  return ctx.send.json(res, 200, {
    ok: true, q: query, id, page: pageNo, hasNext: rows.length > SEARCH_PAGE, rows: rows.slice(0, SEARCH_PAGE).map(userOf),
  });
};

const lookup = (target) => async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-lookup', 60, 60)) return;
  const name = (ctx.query.get('name') ?? '').trim();
  const idRaw = (ctx.query.get('id') ?? '').trim();
  let row;
  if (idRaw) {
    const id = userIdParam(idRaw);
    if (id != null) [row] = await ctx.db.query(sql`SELECT id FROM players WHERE id = ${id} LIMIT 1`);
  }
  if (!row && name && name.length <= MAX_Q) {
    [row] = await ctx.db.query(sql`SELECT id FROM players WHERE name = ${name} ORDER BY (BINARY name = ${name}) DESC, id LIMIT 1`);
  }
  if (row) return ctx.send.redirect(res, target(Number(row.id)));
  const asked = (name || idRaw).slice(0, MAX_Q);
  return ctx.send.redirect(res, `${urls.hkUsers(asked)}${asked ? '&' : '?'}missing=1`);
};

const accountOf = async (ctx, accountId) => {
  if (!(n(accountId) > 0)) return null;
  const avatars = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name,
      u.is_online AS online, u.last_online_at AS last_online, u.created_at AS account_created
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id
    WHERE u.account_id = ${n(accountId)} ORDER BY u.id LIMIT ${SHARED_LIMIT}`);
  return { id: n(accountId), avatars: avatars.map(userOf) };
};

const sentIds = (ctx) => String(ctx.query.get('commands') ?? '').split(',').map((v) => intParam(v)).filter(Boolean).slice(0, 20);

const loadCommands = async (ctx, id) => {
  const ids = sentIds(ctx);
  try {
    const [sent, recent] = await Promise.all([
      ids.length ? ctx.db.query(sql`SELECT id, player_id, kind, status, result, processed_at FROM web_commands
        WHERE id IN (${sql.in(ids)}) AND player_id = ${id} ORDER BY id`) : Promise.resolve([]),
      ctx.db.query(sql`SELECT id, player_id, kind, status, result, processed_at FROM web_commands
        WHERE player_id = ${id} ORDER BY id DESC LIMIT ${COMMAND_ROWS}`),
    ]);
    return { sent: [...sent].map(commandOf), recent: [...recent].map(commandOf) };
  } catch (e) {
    ctx.log(`web_commands of user ${id}: ${e?.message ?? e}`);
    return { sent: [], recent: [] };
  }
};

const detail = async (req, res, ctx) => {
  const id = userIdParam(ctx.params.id);
  if (id == null) return userNotFound(ctx, res);
  const now = unixNow();
  const currencies = walletCurrencies(gameRules(), ctx.texts);
  const pointTypes = currencies.filter((c) => c.type >= 0).map((c) => c.type);
  const [[u], [standing], currency, [activeBan], [banCount], [friends], [roomCount], rooms, badges, commands] = await Promise.all([
    ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.motto, u.rank_id, r.name AS rank_name, u.created_at AS account_created,
        u.last_login_at AS last_login, u.last_online_at AS last_online, u.is_online AS online, COALESCE(b.amount, 0) AS credits,
        u.home_room_id AS home_room, hr.name AS home_room_name, u.account_id
      FROM players u LEFT JOIN ranks r ON r.id = u.rank_id LEFT JOIN rooms hr ON hr.id = u.home_room_id
        LEFT JOIN player_balances b ON b.player_id = u.id AND b.currency = -1
      WHERE u.id = ${id} LIMIT 1`),
    ctx.db.query(sql`SELECT st.reports_sent AS cfh_send, st.reports_abusive AS cfh_abusive, st.warnings_received AS cfh_warnings,
        st.bans_received AS cfh_bans, st.trade_lock_count AS tradelock_amount, st.trade_locked_until, st.safety_locked,
        st.muted_until AS mute_end_timestamp, s.achievement_score, s.online_seconds AS online_time
      FROM player_standing st JOIN player_stats s ON s.player_id = st.player_id WHERE st.player_id = ${id} LIMIT 1`),
    ctx.db.query(sql`SELECT currency AS type, amount FROM player_balances WHERE player_id = ${id} AND currency IN (${sql.in(pointTypes)})`),
    ctx.db.query(sql`SELECT ${BAN_COLUMNS}, s.name AS staff_name
      FROM bans b LEFT JOIN players s ON s.id = b.issued_by
      WHERE b.player_id = ${id} AND b.ends_at > ${now} ORDER BY b.ends_at DESC, b.id DESC LIMIT 1`),
    ctx.db.query(sql`SELECT COUNT(*) AS n FROM bans WHERE player_id = ${id}`),
    ctx.db.query(sql`SELECT COUNT(*) AS n FROM friendships WHERE player_id = ${id}`),
    ctx.db.query(sql`SELECT COUNT(*) AS n FROM rooms WHERE owner_id = ${id}`),
    ctx.db.query(sql`SELECT id, name, player_count, max_players, door_mode FROM rooms WHERE owner_id = ${id} ORDER BY id LIMIT ${SHARED_LIMIT}`),
    ctx.db.query(sql`SELECT badge_code, slot AS slot_id FROM player_badges WHERE player_id = ${id}
      ORDER BY slot = 0, slot, badge_code LIMIT ${BADGE_ROWS + 1}`),
    loadCommands(ctx, id),
  ]);
  if (!u) return userNotFound(ctx, res);
  const account = await accountOf(ctx, u.account_id);
  const { staff } = ctx;
  const wallet = new Map(currency.map((c) => [Number(c.type), Number(c.amount)]));
  const canRank = rankRefusal(staff, u) === null;
  const named = (code) => Boolean(ctx.texts?.get?.(`badge_name_${code}`, ''));
  return ctx.send.json(res, 200, {
    ok: true,
    user: {
      ...userOf(u), motto: String(u.motto ?? ''), lastLogin: n(u.last_login), homeRoom: roomOf(u.home_room, u.home_room_name),
    },
    wallet: currencies.map((c) => ({ key: c.key, label: c.label, amount: c.type < 0 ? n(u.credits) : wallet.get(c.type) ?? 0 })),
    standing: standing ? {
      reportsSent: n(standing.cfh_send), reportsAbusive: n(standing.cfh_abusive), cautions: n(standing.cfh_warnings),
      bansCounted: n(standing.cfh_bans), tradeLocks: n(standing.tradelock_amount), safetyLocked: String(standing.safety_locked) === '1',
      mutedUntil: n(standing.mute_end_timestamp) > now ? n(standing.mute_end_timestamp) : 0,
      tradeLockedUntil: isTradeLocked(standing, now) ? n(standing.trade_locked_until) : 0,
      achievementScore: n(standing.achievement_score), onlineSeconds: n(standing.online_time),
    } : null,
    ban: activeBan ? banRow(activeBan, now) : null,
    account,
    bans: n(banCount?.n),
    friends: n(friends?.n),
    roomCount: n(roomCount?.n),
    rooms: rooms.map((r) => ({ id: n(r.id), name: String(r.name ?? ''), users: n(r.player_count), maxUsers: n(r.max_players), door: String(r.door_mode) })),
    badges: badges.slice(0, BADGE_ROWS).map((b) => {
      const code = String(b.badge_code);
      const name = badgeNameOrCode(ctx.texts, code);
      return { code, name: name === code && !named(code) ? null : name, slot: n(b.slot_id), image: badgeImagePath() ? `/assets/${badgeImagePath()}/${encodeURIComponent(code)}.gif` : null };
    }),
    badgesMore: badges.length > BADGE_ROWS,
    commands,
    can: {
      ban: staff.can('moderation.player.ban') && Number(u.id) !== staff.id && Number(u.rank_id) < staff.rank,
      wallet: editRefusal(staff, { ...u, rank: u.rank_id }) === null && staff.can(CURRENCY_PERM),
      badges: editRefusal(staff, { ...u, rank: u.rank_id }) === null && staff.can(BADGE_PERM),
      rank: canRank,
    },
    levels: canRank ? await rankLevels(ctx) : [],
  });
};

const sanctionsPage = async (req, res, ctx) => {
  const u = await loadUser(ctx);
  if (!u) return userNotFound(ctx, res);
  const id = Number(u.id);
  const now = unixNow();
  const { staff } = ctx;
  const canBan = staff.can('moderation.player.ban');
  const top = canBan ? await topRank(ctx) : 0;
  const [bans, sanctions, [standing], permLog, hkLog] = await Promise.all([
    ctx.db.query(sql`SELECT ${BAN_COLUMNS}, s.name AS staff_name,
        s.\`rank_id\` AS staff_rank
      FROM bans b LEFT JOIN players s ON s.id = b.issued_by
      WHERE b.player_id = ${id} ORDER BY b.issued_at DESC, b.id DESC LIMIT ${LOG_PAGE}`),
    ctx.db.query(sql`SELECT s.id, s.step AS level, s.kind, l.hours, s.probation_ends_at, s.reason,
        (s.kind = 'MUTE') AS is_muted, COALESCE(s.ends_at, 0) AS mute_seconds
      FROM sanctions s LEFT JOIN sanction_steps l ON l.step = s.step
      WHERE s.player_id = ${id} ORDER BY s.id DESC LIMIT ${LOG_PAGE}`),
    ctx.db.query(sql`SELECT reports_sent AS cfh_send, reports_abusive AS cfh_abusive, warnings_received AS cfh_warnings, bans_received AS cfh_bans,
        trade_lock_count AS tradelock_amount, trade_locked_until
      FROM player_standing WHERE player_id = ${id} LIMIT 1`),
    ctx.db.query(sql`SELECT c.id, c.changed_at, c.actor_id, act.name AS actor_name, 'rank.change' AS action, 'rank' AS subject,
        c.old_rank AS old_value, c.new_rank AS new_value, c.via
      FROM rank_changes c LEFT JOIN players act ON act.id = c.actor_id WHERE c.player_id = ${id} ORDER BY c.id DESC LIMIT ${SHARED_LIMIT}`),
    canBan ? ctx.db.query(sql`SELECT a.id, a.acted_at, a.staff_id, s.name AS staff_name, a.action, a.subject_type, a.subject_id, a.subject_label, a.detail, a.success
      FROM staff_actions a LEFT JOIN players s ON s.id = a.staff_id
      WHERE a.subject_type = 'user' AND a.subject_id = ${id} ORDER BY a.id DESC LIMIT ${SHARED_LIMIT}`) : Promise.resolve([]),
  ]);
  return ctx.send.json(res, 200, {
    ok: true,
    user: userOf(u),
    can: { ban: canBan && Number(u.rank_id) < staff.rank && id !== staff.id },
    bans: bans.map((b) => ({
      ...banRow(b, now),
      canLift: canBan && Number(b.ends_at) > now
        && unbanRefusal(ctx, { ...b, player_id: id, username: u.username, target_rank: u.rank_id }, top) === null,
    })),
    standing: standing ? {
      reportsSent: n(standing.cfh_send), reportsAbusive: n(standing.cfh_abusive), cautions: n(standing.cfh_warnings),
      bansCounted: n(standing.cfh_bans), tradeLocks: n(standing.tradelock_amount),
      tradeLockedUntil: isTradeLocked(standing, now) ? n(standing.trade_locked_until) : 0,
    } : null,
    sanctions: sanctions.map((x) => ({
      id: n(x.id), level: n(x.level), kind: x.kind ? String(x.kind) : null, hours: x.hours != null ? n(x.hours) : null,
      reason: String(x.reason ?? ''), probationEndsAt: n(x.probation_ends_at), muted: n(x.is_muted) === 1, muteUntil: n(x.mute_seconds),
    })),
    permissionChanges: permLog.map((p) => ({
      id: n(p.id), at: n(p.changed_at), actor: ref(p.actor_id, p.actor_name || (n(p.actor_id) > 0 ? null : 'system')),
      action: String(p.action ?? ''), subject: String(p.subject ?? ''), old: String(p.old_value ?? ''), new: String(p.new_value ?? ''), via: String(p.via ?? ''),
    })),
    log: canBan ? hkLog.map(logRow) : null,
  });
};

const chatlog = async (req, res, ctx) => {
  const u = await loadUser(ctx);
  if (!u) return userNotFound(ctx, res);
  const id = Number(u.id);
  const cursor = readCursor(ctx.query);
  const older = !cursor ? sql``
    : cursor.bid ? sql`AND (c.sent_at < ${cursor.before} OR (c.sent_at = ${cursor.before} AND c.id < ${cursor.bid}))`
      : sql`AND c.sent_at < ${cursor.before}`;
  await audit(ctx, {
    action: 'hk.view.chatlog.user', targetType: 'user', targetId: id, label: u.username,
    detail: cursor ? `before=${cursor.before}${cursor.bid ? ` bid=${cursor.bid}` : ''}` : '',
  });
  const rows = await ctx.db.query(sql`SELECT c.id, c.room_id, r.name AS room_name, c.target_id, t.name AS to_name, c.message, c.sent_at AS timestamp
    FROM room_chat_messages c LEFT JOIN rooms r ON r.id = c.room_id LEFT JOIN players t ON t.id = c.target_id
    WHERE c.sender_id = ${id} ${older} ORDER BY c.sent_at DESC, c.id DESC LIMIT ${LOG_PAGE + 1}`);
  const shown = rows.slice(0, LOG_PAGE);
  return ctx.send.json(res, 200, {
    ok: true, user: userOf(u), cursor,
    rows: shown.map((c) => ({
      id: n(c.id), at: n(c.timestamp), room: roomOf(c.room_id, c.room_name), to: n(c.target_id) > 0 ? ref(c.target_id, c.to_name) : null,
      message: String(c.message ?? ''),
    })),
    next: nextCursor(shown, rows.length > LOG_PAGE),
  });
};

const visits = async (req, res, ctx) => {
  const u = await loadUser(ctx);
  if (!u) return userNotFound(ctx, res);
  const cursor = readCursor(ctx.query);
  const older = !cursor ? sql``
    : cursor.bid ? sql`AND (v.entered_at < ${cursor.before} OR (v.entered_at = ${cursor.before} AND v.id < ${cursor.bid}))`
      : sql`AND v.entered_at < ${cursor.before}`;
  const rows = await ctx.db.query(sql`SELECT v.id, v.room_id, r.name AS room_name, v.entered_at, v.left_at, v.entered_at AS timestamp
    FROM room_visits v LEFT JOIN rooms r ON r.id = v.room_id
    WHERE v.player_id = ${Number(u.id)} ${older} ORDER BY v.entered_at DESC, v.id DESC LIMIT ${LOG_PAGE + 1}`);
  const shown = rows.slice(0, LOG_PAGE);
  return ctx.send.json(res, 200, {
    ok: true, user: userOf(u), cursor,
    rows: shown.map((v) => {
      const left = n(v.left_at) >= n(v.entered_at) && n(v.left_at) > 0;
      return { id: n(v.id), room: roomOf(v.room_id, v.room_name), enteredAt: n(v.entered_at), leftAt: left ? n(v.left_at) : 0 };
    }),
    next: nextCursor(shown, rows.length > LOG_PAGE),
  });
};

const identity = async (req, res, ctx) => {
  const id = userIdParam(ctx.params.id);
  if (id == null) return userNotFound(ctx, res);
  const [u] = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name,
      a.registered_ip AS ip_register, a.last_ip AS ip_current, a.machine_id
    FROM players u LEFT JOIN player_accounts a ON a.id = u.account_id LEFT JOIN ranks r ON r.id = u.rank_id WHERE u.id = ${id} LIMIT 1`);
  if (!u) return userNotFound(ctx, res);
  await audit(ctx, { action: 'hk.view.identity', targetType: 'user', targetId: id, label: u.username });
  const now = unixNow();
  const ipReg = String(u.ip_register ?? '');
  const ipCur = String(u.ip_current ?? '');
  const machine = String(u.machine_id ?? '');
  const [last] = await ctx.db.query(sql`SELECT d.id, d.gpu, d.scr, d.cpu, d.tz, d.fnt, d.cnv, d.aud, d.gl, d.ua
    FROM player_devices d WHERE d.player_id = ${id} ORDER BY d.last_seen_at DESC LIMIT 1`);
  const device = last ? readingOf(last) : '';
  const deviceId = last && usable(last.id) ? String(last.id) : '';
  const values = [];
  if (ipReg && ipReg === ipCur) values.push({ kind: 'ip', label: 'IP at registration and last IP', value: ipReg });
  else {
    if (ipReg) values.push({ kind: 'ip', label: 'IP at registration', value: ipReg });
    if (ipCur) values.push({ kind: 'ip', label: 'Last IP', value: ipCur });
  }
  if (deviceId) values.push({ kind: 'device', label: 'Device id', value: deviceId });

  const shared = await Promise.all(values.map((v) => ctx.db.query(v.kind === 'ip'
    ? sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name, u.is_online AS online, u.last_online_at AS last_online
        FROM players u JOIN player_accounts a ON a.id = u.account_id LEFT JOIN ranks r ON r.id = u.rank_id
        WHERE (a.registered_ip = ${v.value} OR a.last_ip = ${v.value}) AND u.id <> ${id}
        ORDER BY u.last_online_at DESC, u.id DESC LIMIT ${SHARED_LIMIT + 1}`
    : sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name, u.is_online AS online, u.last_online_at AS last_online
        FROM players u LEFT JOIN ranks r ON r.id = u.rank_id
        WHERE u.id IN (SELECT d.player_id FROM player_devices d WHERE d.id = ${v.value}) AND u.id <> ${id}
        ORDER BY u.last_online_at DESC, u.id DESC LIMIT ${SHARED_LIMIT + 1}`)));

  const ips = [...new Set([ipReg, ipCur].filter(Boolean))];
  const conds = [];
  if (ips.length) conds.push(sql`b.barred_address IN (${sql.in(ips)})`);
  if (machine) conds.push(sql`b.barred_machine = ${machine}`);
  const bans = conds.length ? await ctx.db.query(sql`SELECT ${BAN_COLUMNS}, b.player_id, t.name AS username, b.barred_address, b.barred_machine,
      s.name AS staff_name
    FROM bans b LEFT JOIN players t ON t.id = b.player_id LEFT JOIN players s ON s.id = b.issued_by
    WHERE ${sql.join(conds, ' OR ')} ORDER BY b.id DESC LIMIT ${LOG_PAGE}`) : [];

  return ctx.send.json(res, 200, {
    ok: true,
    user: userOf(u),
    addresses: { registered: ipReg || null, last: ipCur || null, device: device || null },
    shared: values.map((v, i) => ({
      kind: v.kind, label: v.label, value: v.value, rows: shared[i].slice(0, SHARED_LIMIT).map(userOf), more: shared[i].length > SHARED_LIMIT,
    })),
    bans: bans.map((b) => ({
      ...banRow(b, now), user: ref(b.player_id, b.username),
      on: [b.barred_address, b.barred_machine].filter((v) => v != null).join(' '),
    })),
  });
};

const USER = `${HK_API}/users/:id(\\d{1,10})`;

export const routes = [
  { method: 'GET', path: `${HK_API}/users`, perm: 'moderation.tool', handler: search },
  { method: 'GET', path: '/housekeeping/users/lookup', perm: 'moderation.tool', handler: lookup((id) => urls.hkUser(id)) },
  { method: 'GET', path: '/housekeeping/users/sanctions', perm: 'moderation.tool', handler: lookup((id) => urls.hkUserSanctions(id)) },
  { method: 'GET', path: USER, perm: 'moderation.tool', handler: detail },
  { method: 'GET', path: `${USER}/sanctions`, perm: 'moderation.tool', handler: sanctionsPage },
  { method: 'GET', path: `${USER}/chatlog`, perm: 'moderation.player.chatlog', audited: true, handler: chatlog },
  { method: 'GET', path: `${USER}/visits`, perm: 'moderation.player.chatlog', handler: visits },
  { method: 'GET', path: `${HK_API}/identity/:id(\\d{1,10})`, perm: 'moderation.player.ban', audited: true, handler: identity },
];
