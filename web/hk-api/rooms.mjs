import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import { DbRefused } from '../cms/lib/db.mjs';
import { intParam, likeContains, likePrefix, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';
import {
  CHAT_ROOM_GRANT, READ_ONLY_NOTE, TICKET_GRANTS, TICKET_JOIN, TICKET_SELECT, USER_GRANT, caption, chatLine, cutLogPage,
  loadRooms, loadUsers, peopleOf, roomsOf, ticketJson,
} from './rooms-shared.mjs';
import { PHOTO_ID, photoDir } from '../lib/photo-files.mjs';
import { SECONDS_PER_DAY } from '../lib/time.mjs';

export const owner = 'hk-rooms';

export const ROOM_COLS = Object.freeze([
  'id', 'owner_id', 'name', 'description', 'model_name', 'door_mode', 'player_count', 'max_players', 'guild_id', 'category_id',
  'score', 'floor_paper', 'wall_paper', 'landscape_paper', 'wall_thickness', 'wall_height', 'floor_thickness',
  'is_public', 'is_staff_picked', 'pets_allowed', 'pets_may_eat', 'pets_muted',
  'walk_through', 'leave_on_door_tile', 'hide_walls', 'chat_protection', 'has_custom_floor', 'who_can_mute',
  'who_can_kick', 'who_can_ban',
  'is_promoted', 'trade_mode', 'move_diagonally', 'idle_sleep', 'idle_sleep_seconds',
  'idle_kick', 'idle_kick_seconds', 'build_pass_locked', 'build_pass_door_mode', 'created_at',
]);
const ROOM_LISTS = Object.freeze(['tags', 'moodlight']);

const CHATLOG_PAGE = 150;
const SEARCH_PAGE = 50;
const LIST_MAX = 100;
const VISITORS = 30;
const QUESTION_MAX = 255;
const QUESTION_SECONDS = Object.freeze({ min: 5, max: 3600, default: 60 });
const QUESTION_PERM = 'moderation.room.alert';
const EDIT_LIMIT = ['hk-edit', 60, 3600];

export const grants = [
  USER_GRANT,
  { table: 'rooms', select: [...ROOM_COLS] },
  { table: 'guilds', select: ['id', 'name', 'badge_code'] },
  { table: 'room_tags', select: ['room_id', 'position', 'tag'] },
  { table: 'room_dimmer_presets', select: ['room_id', 'position', 'preset', 'selected', 'background_only', 'color', 'brightness'] },
  { table: 'room_categories', select: ['id', 'caption'] },
  { table: 'room_promotions', select: ['room_id', 'title', 'description', 'starts_at', 'ends_at'] },
  COMMAND_GRANT,
  { table: 'room_rights', select: ['room_id', 'player_id'] },
  { table: 'room_bans', select: ['room_id', 'player_id', 'ends_at'] },
  { table: 'room_mutes', select: ['room_id', 'player_id', 'ends_at'] },
  { table: 'room_visits', select: ['id', 'room_id', 'player_id', 'entered_at', 'left_at'] },
  CHAT_ROOM_GRANT,
  ...TICKET_GRANTS,
  { table: 'furni', select: ['id', 'player_id', 'room_id', 'definition_id', 'wall_position', 'state'] },
  { table: 'furni_definitions', select: ['id', 'code', 'display_name', 'behaviour'] },
  { table: 'camera_photos', select: ['id', 'player_id', 'room_id', 'created_at_ms', 'purchases', 'published', 'published_at'] },
];

const MODERATION = {
  0: ['navigator.roomsettings.moderation.none', 'None'],
  1: ['navigator.roomsettings.moderation.rights', 'Users with rights'],
  2: ['navigator.roomsettings.moderation.all', 'All users'],
  4: ['navigator.roomsettings.moderation.group_admins', 'Group admins'],
  5: ['navigator.roomsettings.moderation.group_admins_and_rights', 'Group admins and users with rights'],
};
const TRADE = {
  0: ['navigator.roomsettings.trade_not_allowed', 'Trading not allowed'],
  1: ['navigator.roomsettings.trade_not_with_Controller', 'Room owners and users with rights'],
  2: ['navigator.roomsettings.trade_allowed', 'Everyone can trade'],
};
const option = (ctx, map, value) => {
  const o = map[Number(value)];
  return o ? ctx.texts.get(o[0], o[1]) : `Value ${value}`;
};
const yes = (v) => String(v) === '1';
const ending = (ends, now) => ({ ends: Number(ends), permanent: Number(ends) > now + 20 * 365 * SECONDS_PER_DAY, active: Number(ends) > now });
const capped = (rows, map) => ({ rows: rows.slice(0, LIST_MAX).map(map), more: rows.length > LIST_MAX });
const NO_ROOM = 'There is no room with this id. It may have been deleted.';

const loadRoom = async (ctx, id) => {
  const [room] = await ctx.db.query(sql`SELECT ${sql.join(ROOM_COLS.map((c) => sql.ident(c, ROOM_COLS)))},
      COALESCE((SELECT GROUP_CONCAT(CONCAT(t.tag, ';') ORDER BY t.position SEPARATOR '') FROM room_tags t WHERE t.room_id = rooms.id), '') AS tags,
      COALESCE((SELECT GROUP_CONCAT(CONCAT_WS(',', IF(d.selected, 2, 1), d.preset, IF(d.background_only, 2, 1), d.color, CONCAT(d.brightness, ';')) ORDER BY d.position SEPARATOR '')
        FROM room_dimmer_presets d WHERE d.room_id = rooms.id), '') AS moodlight
    FROM rooms WHERE id = ${id} LIMIT 1`);
  return room ?? null;
};


const questionPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const id = intParam(ctx.params.id);
  const room = id && await loadRoom(ctx, id);
  if (!room) return ctx.send.notFound(res, NO_ROOM);
  const question = String(ctx.form.get('question') ?? '').trim();
  const seconds = intParam(ctx.form.get('seconds'), { max: QUESTION_SECONDS.max });
  if (!question || question.length > QUESTION_MAX) {
    return ctx.send.error(res, 400, 'Not asked', `A question is 1 to ${QUESTION_MAX} characters.`, { field: 'question' });
  }
  if (seconds == null || seconds < QUESTION_SECONDS.min) {
    return ctx.send.error(res, 400, 'Not asked', `Seconds are ${QUESTION_SECONDS.min} to ${QUESTION_SECONDS.max}.`, { field: 'seconds' });
  }
  const label = String(room.name || `Room ${id}`);
  let commandId;
  try {
    commandId = await ctx.command('askRoomQuestion', { roomId: id, question, seconds }, { userId: ctx.staff.id });
  } catch (e) {
    await audit(ctx, { action: 'hk.room.question', targetType: 'room', targetId: id, label, detail: `not sent: "${question}"`, success: false });
    if (e instanceof DbRefused) return ctx.send.error(res, 503, 'Not asked', 'This server can not send changes to the game right now.');
    throw e;
  }
  await audit(ctx, { action: 'hk.room.question', targetType: 'room', targetId: id, label, detail: `"${question}" for ${seconds} s, command #${commandId}` });
  const [row] = await ctx.waitForCommands([commandId], { userId: ctx.staff.id });
  return ctx.send.json(res, 200, {
    ok: true, command: { id: commandId, status: String(row?.status ?? 'pending'), result: row?.result == null ? null : String(row.result) },
  });
};

const roomRow = (ctx, r) => ({
  id: Number(r.id), name: String(r.name ?? ''), ownerId: Number(r.owner_id ?? 0), door: String(r.door_mode ?? ''),
  players: Number(r.player_count ?? 0), maxPlayers: Number(r.max_players ?? 0), categoryId: Number(r.category_id ?? 0),
  category: r.cat_caption == null ? null : caption(ctx, r.cat_caption), created: Number(r.created_at ?? 0),
});

const search = async (req, res, ctx) => {
  const q = String(ctx.query.get('q') ?? '').trim().slice(0, 64);
  const pageNo = intParam(ctx.query.get('page'), { max: 10000 }) ?? 1;
  const COLS = sql`r.id, r.name, r.owner_id, r.door_mode, r.player_count, r.max_players, r.category_id, r.created_at, c.caption AS cat_caption`;
  if (q) {
    if (!ctx.rateLimit(res, 'hk-lookup', 60, 60)) return;
    const asId = intParam(q) ?? -1;
    const rows = await ctx.db.query(sql`SELECT ${COLS}
      FROM rooms r LEFT JOIN players u ON u.id = r.owner_id LEFT JOIN room_categories c ON c.id = r.category_id
      WHERE r.id = ${asId} OR LOWER(r.name) LIKE LOWER(${likeContains(q)}) OR u.name LIKE ${likePrefix(q)}
      ORDER BY (r.id = ${asId}) DESC, r.player_count DESC, r.id DESC
      LIMIT ${SEARCH_PAGE + 1} OFFSET ${(pageNo - 1) * SEARCH_PAGE}`);
    const shown = rows.slice(0, SEARCH_PAGE);
    const users = await loadUsers(ctx, shown.map((r) => r.owner_id));
    return ctx.send.json(res, 200, {
      ok: true, q, page: pageNo, hasNext: rows.length > SEARCH_PAGE, rows: shown.map((r) => roomRow(ctx, r)), users: peopleOf(users),
    });
  }
  const busy = await ctx.db.query(sql`SELECT ${COLS} FROM rooms r LEFT JOIN room_categories c ON c.id = r.category_id
    WHERE r.player_count > 0 ORDER BY r.player_count DESC, r.id DESC LIMIT ${SEARCH_PAGE}`);
  const newest = await ctx.db.query(sql`SELECT ${COLS} FROM rooms r LEFT JOIN room_categories c ON c.id = r.category_id
    ORDER BY r.id DESC LIMIT 20`);
  const users = await loadUsers(ctx, [...busy, ...newest].map((r) => r.owner_id));
  return ctx.send.json(res, 200, {
    ok: true, q: '', busy: busy.map((r) => roomRow(ctx, r)), newest: newest.map((r) => roomRow(ctx, r)), users: peopleOf(users),
  });
};

const detail = async (req, res, ctx) => {
  const id = intParam(ctx.params.id);
  const room = id && await loadRoom(ctx, id);
  if (!room) return ctx.send.notFound(res, NO_ROOM);
  const now = unixNow();
  const can = {
    logs: ctx.staff.can('moderation.room.chatlog'), tickets: ctx.staff.can('moderation.tickets'), ask: ctx.staff.can(QUESTION_PERM),
  };
  const [group] = Number(room.guild_id) > 0
    ? await ctx.db.query(sql`SELECT id, name, badge_code FROM guilds WHERE id = ${Number(room.guild_id)} LIMIT 1`) : [];
  const [cat] = await ctx.db.query(sql`SELECT id, caption FROM room_categories WHERE id = ${Number(room.category_id)} LIMIT 1`);
  const [promo] = await ctx.db.query(sql`SELECT title, description, starts_at, ends_at FROM room_promotions WHERE room_id = ${id} LIMIT 1`);
  const rights = await ctx.db.query(sql`SELECT player_id FROM room_rights WHERE room_id = ${id} ORDER BY player_id LIMIT ${LIST_MAX + 1}`);
  const bans = await ctx.db.query(sql`SELECT player_id, ends_at FROM room_bans WHERE room_id = ${id} ORDER BY ends_at DESC LIMIT ${LIST_MAX + 1}`);
  const mutes = await ctx.db.query(sql`SELECT player_id, ends_at FROM room_mutes WHERE room_id = ${id} ORDER BY ends_at DESC LIMIT ${LIST_MAX + 1}`);
  const visits = await ctx.db.query(sql`SELECT player_id, entered_at, left_at FROM room_visits WHERE room_id = ${id}
    ORDER BY entered_at DESC, id DESC LIMIT ${VISITORS}`);
  const tickets = can.tickets
    ? await ctx.db.query(sql`SELECT ${TICKET_SELECT} FROM ${TICKET_JOIN} WHERE st.room_id = ${id} ORDER BY st.id DESC LIMIT ${LIST_MAX + 1}`)
    : [];
  const users = await loadUsers(ctx, [
    room.owner_id, ...rights.map((r) => r.player_id), ...bans.map((r) => r.player_id), ...mutes.map((r) => r.player_id),
    ...visits.map((r) => r.player_id), ...tickets.flatMap((t) => [t.sender_id, t.reported_id, t.moderator_id]),
  ]);
  return ctx.send.json(res, 200, {
    ok: true,
    room: {
      id, name: String(room.name ?? ''), description: String(room.description ?? ''), ownerId: Number(room.owner_id ?? 0),
      door: String(room.door_mode ?? ''), players: Number(room.player_count ?? 0), maxPlayers: Number(room.max_players ?? 0),
      isPublic: yes(room.is_public), staffPick: yes(room.is_staff_picked), promoted: yes(room.is_promoted),
      categoryId: Number(room.category_id ?? 0), category: cat ? caption(ctx, cat.caption) : null,
      group: group ? { id: Number(group.id), name: String(group.name ?? '') } : null,
      trading: option(ctx, TRADE, room.trade_mode), whoCanMute: option(ctx, MODERATION, room.who_can_mute),
      whoCanKick: option(ctx, MODERATION, room.who_can_kick), whoCanBan: option(ctx, MODERATION, room.who_can_ban),
      model: String(room.model_name ?? ''), customFloor: yes(room.has_custom_floor), tags: String(room.tags ?? ''),
      created: Number(room.created_at ?? 0),
      promotion: promo ? { title: String(promo.title ?? ''), ends: Number(promo.ends_at ?? 0) } : null,
    },
    can,
    question: { max: QUESTION_MAX, seconds: QUESTION_SECONDS },
    rights: capped(rights, (r) => Number(r.player_id)),
    bans: capped(bans, (b) => ({ userId: Number(b.player_id), ...ending(b.ends_at, now) })),
    mutes: capped(mutes, (m) => ({ userId: Number(m.player_id), ...ending(m.ends_at, now) })),
    visits: visits.map((v) => ({ userId: Number(v.player_id), entered: Number(v.entered_at ?? 0), left: Number(v.left_at ?? 0) })),
    tickets: can.tickets ? capped(tickets, ticketJson) : null,
    users: peopleOf(users),
    raw: [...ROOM_COLS, ...ROOM_LISTS].map((c) => [c, String(room[c] ?? '')]),
    note: READ_ONLY_NOTE,
  });
};

const chatlog = async (req, res, ctx) => {
  const id = intParam(ctx.params.id);
  if (!id) return ctx.send.notFound(res, 'There is no room with this id.');
  const before = intParam(ctx.query.get('before'));
  const room = await loadRoom(ctx, id);
  if (!room) {
    const [any] = await ctx.db.query(sql`SELECT id FROM room_chat_messages WHERE room_id = ${id} LIMIT 1`);
    if (!any) return ctx.send.notFound(res, 'There is no room with this id, and no chat logged for it.');
  }
  const rows = await ctx.db.query(sql`SELECT id, room_id, sender_id, target_id, message, sent_at AS timestamp FROM room_chat_messages
    WHERE room_id = ${id} ${before ? sql`AND sent_at < ${before}` : sql``}
    ORDER BY sent_at DESC, id DESC LIMIT ${CHATLOG_PAGE + 1}`);
  const { rows: lines, nextBefore } = cutLogPage(rows, CHATLOG_PAGE);
  const users = await loadUsers(ctx, lines.flatMap((r) => [r.sender_id, r.target_id]));
  const name = room ? (room.name || `Room ${id}`) : `Room ${id} (deleted)`;
  await audit(ctx, {
    action: 'hk.view.chatlog.room', targetType: 'room', targetId: id, label: name,
    detail: `${before ? `before=${before}` : 'newest'} lines=${lines.length}`,
  });
  return ctx.send.json(res, 200, {
    ok: true, room: { id, name, exists: Boolean(room) }, pageSize: CHATLOG_PAGE, before: before ?? null, nextBefore,
    lines: lines.map(chatLine), users: peopleOf(users),
  });
};

export const photoIdOf = (extraData) => {
  try {
    const v = JSON.parse(String(extraData ?? ''));
    return v && typeof v === 'object' && typeof v.id === 'string' && PHOTO_ID.test(v.id) ? v.id : null;
  } catch {
    return null;
  }
};

const photo = async (req, res, ctx) => {
  const id = intParam(ctx.params.id);
  const [item] = id ? await ctx.db.query(sql`SELECT i.id, i.player_id, i.room_id, i.definition_id, i.wall_position, i.state,
      b.code, b.display_name, b.behaviour
    FROM furni i LEFT JOIN furni_definitions b ON b.id = i.definition_id WHERE i.id = ${id} LIMIT 1`) : [];
  if (!item) return ctx.send.notFound(res, 'There is no item with this id. The photo may have been deleted.');
  const uuid = photoIdOf(item.state);
  if (!uuid && item.behaviour !== 'display.image') return ctx.send.notFound(res, `Item #${id} is not a camera photo.`);
  const [shot] = uuid ? await ctx.db.query(sql`SELECT id, player_id AS user_id, room_id, created_at_ms AS created_at, purchases, published, published_at
    FROM camera_photos WHERE id = ${uuid} LIMIT 1`) : [];
  const tickets = await ctx.db.query(sql`SELECT ${TICKET_SELECT} FROM ${TICKET_JOIN} WHERE st.photo_furni_id = ${id} ORDER BY st.id DESC LIMIT ${LIST_MAX}`);
  const users = await loadUsers(ctx, [item.player_id, shot?.user_id, ...tickets.flatMap((t) => [t.sender_id, t.reported_id, t.moderator_id])]);
  const rooms = await loadRooms(ctx, [item.room_id, shot?.room_id, ...tickets.map((t) => t.room_id)]);
  return ctx.send.json(res, 200, {
    ok: true,
    item: {
      id: Number(item.id), ownerId: Number(item.player_id ?? 0), roomId: Number(item.room_id ?? 0),
      furni: { name: String(item.display_name || item.code || `#${item.definition_id}`), code: String(item.code ?? '') },
    },
    uuid,
    image: uuid && ctx.userContent.exists(`${photoDir()}/${uuid}.png`) ? `/usercontent/${photoDir()}/${uuid}.png` : null,
    photo: shot ? {
      userId: Number(shot.user_id ?? 0), roomId: Number(shot.room_id ?? 0), taken: Math.floor(Number(shot.created_at) / 1000),
      published: Number(shot.published) === 1, publishedAt: Number(shot.published_at ?? 0), purchases: Number(shot.purchases ?? 0),
    } : null,
    tickets: tickets.map(ticketJson),
    users: peopleOf(users),
    rooms: roomsOf(rooms),
  });
};

const ID = ':id(\\d{1,10})';
export const routes = [
  { method: 'GET', path: `${HK_API}/rooms`, perm: 'moderation.room.info', handler: search },
  { method: 'GET', path: `${HK_API}/rooms/${ID}`, perm: 'moderation.room.info', handler: detail },
  { method: 'GET', path: `${HK_API}/rooms/${ID}/chatlog`, perm: 'moderation.room.chatlog', audited: true, handler: chatlog },
  { method: 'POST', path: `${HK_API}/rooms/${ID}/question`, perm: QUESTION_PERM, handler: questionPost },
  { method: 'GET', path: `${HK_API}/photos/${ID}`, perm: 'moderation.tickets', handler: photo },
];
