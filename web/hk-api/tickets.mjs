import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import { DbRefused } from '../cms/lib/db.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';
import {
  CHAT_ROOM_GRANT, READ_ONLY_NOTE, ROOM_NAME_GRANT, TICKET_GRANTS, TICKET_JOIN, TICKET_SELECT, TICKET_STATES, UNSAVED_TYPE,
  USER_GRANT, chatLine, loadRooms, loadUsers, peopleOf, roomsOf, ticketJson, ticketType,
} from './rooms-shared.mjs';
import { SECONDS_PER_HOUR } from '../lib/time.mjs';

export const owner = 'hk-rooms';

const PER_PAGE = 100;
const ROOM_WINDOW = 30 * 60;
const IM_WINDOW = SECONDS_PER_HOUR;
const FORUM_MAX = 100;
const SUPER_USER = 9;

export const grants = [
  USER_GRANT,
  ROOM_NAME_GRANT,
  ...TICKET_GRANTS,
  COMMAND_GRANT,
  CHAT_ROOM_GRANT,
  { table: 'direct_messages', select: ['id', 'sender_id', 'target_id', 'message', 'icon_id', 'sent_at'] },
  { table: 'guilds', select: ['id', 'name', 'badge_code'] },
  { table: 'guild_forum_threads', select: ['id', 'guild_id', 'author_id', 'subject', 'post_count', 'created_at', 'state', 'locked', 'pinned'] },
  { table: 'guild_forum_posts', select: ['id', 'thread_id', 'author_id', 'message', 'created_at', 'state'] },
];

const APPEAL = { 0: 'None', 1: 'Waiting for staff', 2: 'Reviewed: staff acted', 3: 'Reviewed: no action' };
const FORUM_STATE = { 0: 'visible', 1: 'closed', 10: 'hidden by staff', 20: 'hidden by group admin' };
const forumState = (state) => FORUM_STATE[Number(state)] ?? `state ${state}`;

const list = async (req, res, ctx) => {
  const state = TICKET_STATES.find((s) => s.key === ctx.query.get('state')) ?? TICKET_STATES[0];
  const pageNo = intParam(ctx.query.get('page'), { max: 10000 }) ?? 1;
  const counts = new Map((await ctx.db.query(sql`SELECT status, COUNT(id) AS n FROM support_tickets GROUP BY status`))
    .map((r) => [String(r.status), Number(r.n)]));
  const rows = await ctx.db.query(sql`SELECT ${TICKET_SELECT} FROM ${TICKET_JOIN}
    WHERE st.status = ${state.value} ORDER BY st.id DESC LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`);
  const tickets = rows.slice(0, PER_PAGE);
  const users = await loadUsers(ctx, tickets.flatMap((t) => [t.sender_id, t.reported_id, t.moderator_id]));
  const rooms = await loadRooms(ctx, tickets.map((t) => t.room_id));
  return ctx.send.json(res, 200, {
    ok: true,
    state: state.key,
    states: TICKET_STATES.map((s) => ({ key: s.key, label: s.label, count: counts.get(s.value) ?? 0 })),
    page: pageNo, hasNext: rows.length > PER_PAGE,
    tickets: tickets.map(ticketJson), users: peopleOf(users), rooms: roomsOf(rooms), note: READ_ONLY_NOTE,
  });
};

const CHAT_COLS = sql`id, room_id, sender_id, target_id, message, sent_at AS timestamp`;

const roomChatAround = async (ctx, where, ts, before = 100, after = 50) => {
  const older = await ctx.db.query(sql`SELECT ${CHAT_COLS} FROM room_chat_messages WHERE ${where}
    AND sent_at BETWEEN ${ts - ROOM_WINDOW} AND ${ts} ORDER BY sent_at DESC, id DESC LIMIT ${before}`);
  const newer = await ctx.db.query(sql`SELECT ${CHAT_COLS} FROM room_chat_messages WHERE ${where}
    AND sent_at > ${ts} AND sent_at <= ${ts + ROOM_WINDOW} ORDER BY sent_at ASC, id ASC LIMIT ${after}`);
  return { lines: [...older.reverse(), ...newer], count: older.length + newer.length, splitAt: older.length };
};

const privateAround = async (ctx, a, b, ts, before = 70, after = 30) => {
  const pair = sql`icon_id IS NULL AND ((sender_id = ${a} AND target_id = ${b}) OR (sender_id = ${b} AND target_id = ${a}))`;
  const older = await ctx.db.query(sql`SELECT id, sender_id, target_id, message, sent_at AS timestamp FROM direct_messages
    WHERE ${pair} AND sent_at BETWEEN ${ts - IM_WINDOW} AND ${ts} ORDER BY sent_at DESC, id DESC LIMIT ${before}`);
  const newer = await ctx.db.query(sql`SELECT id, sender_id, target_id, message, sent_at AS timestamp FROM direct_messages
    WHERE ${pair} AND sent_at > ${ts} AND sent_at <= ${ts + IM_WINDOW} ORDER BY sent_at ASC, id ASC LIMIT ${after}`);
  return { lines: [...older.reverse(), ...newer], count: older.length + newer.length, splitAt: older.length };
};

const withMarker = ({ lines, splitAt }, ts) => [
  ...lines.slice(0, splitAt), { marker: 'report', timestamp: ts, text: 'The report was sent here.' }, ...lines.slice(splitAt),
];

const context = async (ctx, t, users, rooms) => {
  const ts = Number(t.created_at);
  const sender = Number(t.sender_id);
  const reported = Number(t.reported_id);
  const roomId = Number(t.room_id);
  const { type } = ticketType(t);
  const addUsers = async (list) => { for (const [k, v] of await loadUsers(ctx, list)) users.set(k, v); };

  if (type === 6 || type === 4) {
    if (!(sender > 0 && reported > 0)) return { kind: 'none', title: 'Private messages', empty: 'This ticket names no two users.' };
    const chat = await privateAround(ctx, sender, reported, ts);
    await addUsers(chat.lines.flatMap((r) => [r.sender_id, r.target_id]));
    await audit(ctx, {
      action: 'hk.view.ticket', targetType: 'ticket', targetId: Number(t.id), label: `#${t.id}`,
      detail: `private messages between ${sender} and ${reported}, lines=${chat.count}`,
    });
    return {
      kind: 'private', title: 'Private messages between the two', private: true,
      note: 'One hour either side of the report, at most 100 messages. Viewing them is written to the audit log.',
      lines: withMarker(chat, ts).map(chatLine), empty: 'No private messages in this range.',
    };
  }

  if (type === 11) {
    const threadId = Number(t.forum_thread_id);
    const commentId = Number(t.forum_post_id);
    const [thread] = threadId > 0 ? await ctx.db.query(sql`SELECT id, guild_id, author_id, subject, post_count, created_at, state, locked, pinned
      FROM guild_forum_threads WHERE id = ${threadId} LIMIT 1`) : [];
    if (!thread) return { kind: 'none', title: 'Forum thread', empty: `The thread (#${threadId}) is gone.` };
    const [group] = await ctx.db.query(sql`SELECT id, name, badge_code FROM guilds WHERE id = ${Number(thread.guild_id)} LIMIT 1`);
    const comments = commentId > 0
      ? [
        ...(await ctx.db.query(sql`SELECT id, author_id, message, created_at, state FROM guild_forum_posts
          WHERE thread_id = ${threadId} AND id <= ${commentId} ORDER BY id DESC LIMIT ${FORUM_MAX / 2}`)).reverse(),
        ...await ctx.db.query(sql`SELECT id, author_id, message, created_at, state FROM guild_forum_posts
          WHERE thread_id = ${threadId} AND id > ${commentId} ORDER BY id ASC LIMIT ${FORUM_MAX / 2}`),
      ]
      : await ctx.db.query(sql`SELECT id, author_id, message, created_at, state FROM guild_forum_posts
          WHERE thread_id = ${threadId} ORDER BY id ASC LIMIT ${FORUM_MAX}`);
    await addUsers([thread.author_id, ...comments.map((c) => c.author_id)]);
    return {
      kind: 'forum', title: commentId > 0 ? 'Reported forum message' : 'Reported forum thread',
      thread: {
        id: threadId, subject: String(thread.subject ?? ''), state: forumState(thread.state), locked: Number(thread.locked) === 1,
        authorId: Number(thread.author_id ?? 0), created: Number(thread.created_at ?? 0), posts: Number(thread.post_count ?? 0),
        groupId: Number(thread.guild_id ?? 0), group: group ? String(group.name ?? '') : null,
      },
      comments: comments.map((c) => ({
        id: Number(c.id), authorId: Number(c.author_id ?? 0), at: Number(c.created_at ?? 0), state: forumState(c.state),
        message: String(c.message ?? ''), reported: Number(c.id) === commentId,
      })),
      note: 'Hiding a thread or a message is done in the client (the forum\'s own moderation, or the mod tool).',
    };
  }

  if (type === 14) {
    const item = Number(t.photo_furni_id);
    return item > 0 ? { kind: 'photo', title: 'Reported photo', photoItemId: item }
      : { kind: 'none', title: 'Reported photo', empty: 'The ticket names no photo item.' };
  }

  if (roomId > 0) {
    const chat = await roomChatAround(ctx, sql`room_id = ${roomId}`, ts);
    await addUsers(chat.lines.flatMap((r) => [r.sender_id, r.target_id]));
    await audit(ctx, {
      action: 'hk.view.chatlog.room', targetType: 'room', targetId: roomId, label: `ticket #${t.id}`,
      detail: `ticket=${t.id} lines=${chat.count}`,
    });
    return {
      kind: 'room', title: 'Room chat around the report',
      note: 'Half an hour either side of the report, at most 150 lines. Viewing them is written to the audit log.',
      lines: withMarker(chat, ts).map(chatLine), empty: 'No chat in this range.',
    };
  }

  const who = [sender, reported].filter((n) => n > 0);
  if (who.length === 0) return { kind: 'none', title: 'Chat', empty: 'This ticket names no room and no users.' };
  const chat = await roomChatAround(ctx, sql`sender_id IN (${sql.in(who)})`, ts);
  await addUsers(chat.lines.flatMap((r) => [r.sender_id, r.target_id]));
  for (const [k, v] of await loadRooms(ctx, chat.lines.map((r) => r.room_id))) rooms.set(k, v);
  await audit(ctx, {
    action: 'hk.view.chatlog.user', targetType: 'user', targetId: reported > 0 ? reported : sender, label: `ticket #${t.id}`,
    detail: `ticket=${t.id} users=${who.join(',')} lines=${chat.count}`,
  });
  return {
    kind: 'users', title: 'What they said around the report', showRooms: true,
    warn: type === UNSAVED_TYPE ? 'This ticket\'s type was not stored, so a private chat report looks like this one here. Its private messages show in the in-client mod tool, or on this page once the ticket is picked.' : null,
    note: 'Room chat by the reporter and the reported user, half an hour either side of the report, at most 150 lines. Viewing them is written to the audit log.',
    lines: withMarker(chat, ts).map(chatLine), empty: 'No chat in this range.',
  };
};

const detail = async (req, res, ctx) => {
  const id = intParam(ctx.params.id);
  const [t] = id ? await ctx.db.query(sql`SELECT ${TICKET_SELECT} FROM ${TICKET_JOIN} WHERE st.id = ${id} LIMIT 1`) : [];
  if (!t) return ctx.send.notFound(res, 'There is no ticket with this id.');
  const users = await loadUsers(ctx, [t.sender_id, t.reported_id, t.moderator_id]);
  const rooms = await loadRooms(ctx, [t.room_id]);
  const about = await context(ctx, t, users, rooms);
  const appeal = Number(t.appeal_state);
  return ctx.send.json(res, 200, {
    ok: true,
    ticket: {
      ...ticketJson(t),
      sanctioned: Number(t.sanctioned) ? (Number(t.sanction_auto) ? 'yes, by auto moderation' : 'yes') : 'no',
      appeal: appeal > 0 ? { label: APPEAL[appeal] ?? `state ${appeal}`, filed: Number(t.appealed_at ?? 0) } : null,
    },
    chatBefore: Number(t.created_at) + ROOM_WINDOW + 1,
    can: {
      room: ctx.staff.can('moderation.room.info'), roomLogs: ctx.staff.can('moderation.room.chatlog'),
      userLogs: ctx.staff.can('moderation.player.chatlog'),
    },
    context: about,
    users: peopleOf(users),
    rooms: roomsOf(rooms),
    note: READ_ONLY_NOTE,
  });
};

const release = async (req, res, ctx) => {
  if (ctx.staff.rank < SUPER_USER) {
    return ctx.send.error(res, 403, 'Not for your rank', 'Only a super user can release a stuck ticket.', { reason: 'permission' });
  }
  const id = intParam(ctx.params.id);
  const [t] = id ? await ctx.db.query(sql`SELECT id, status, moderator_id FROM support_tickets WHERE id = ${id} LIMIT 1`) : [];
  if (!t) return ctx.send.notFound(res, 'There is no ticket with this id.');
  if (String(t.status) !== 'picked') {
    return ctx.send.error(res, 409, 'Not changed', 'This ticket is not picked, so there is nothing to release. Reload the page.', { reason: 'state' });
  }
  const holder = Number(t.moderator_id ?? 0);
  let commandId;
  try {
    commandId = await ctx.command('releaseTicket', { ticketId: id }, { userId: holder });
  } catch (e) {
    await audit(ctx, { action: 'hk.ticket.release', targetType: 'ticket', targetId: id, label: `#${id}`, detail: 'not sent', success: false });
    if (e instanceof DbRefused) return ctx.send.error(res, 503, 'Not released', 'This server can not send changes to the game right now.');
    throw e;
  }
  await audit(ctx, { action: 'hk.ticket.release', targetType: 'ticket', targetId: id, label: `#${id}`, detail: `released from moderator ${holder}, command #${commandId}` });
  const [row] = await ctx.waitForCommands([commandId], { userId: holder });
  if (row && String(row.status) === 'failed') {
    return ctx.send.error(res, 409, 'Not changed', 'Someone changed this ticket just now. Reload the page.', { reason: 'state' });
  }
  const [after] = await ctx.db.query(sql`SELECT ${TICKET_SELECT} FROM ${TICKET_JOIN} WHERE st.id = ${id} LIMIT 1`);
  const users = await loadUsers(ctx, [after.sender_id, after.reported_id, after.moderator_id]);
  const rooms = await loadRooms(ctx, [after.room_id]);
  return ctx.send.json(res, 200, { ok: true, ticket: ticketJson(after), users: peopleOf(users), rooms: roomsOf(rooms) });
};

const ID = ':id(\\d{1,10})';
export const routes = [
  { method: 'GET', path: `${HK_API}/tickets`, perm: 'moderation.tickets', handler: list },
  { method: 'GET', path: `${HK_API}/tickets/${ID}`, perm: 'moderation.tickets', audited: true, handler: detail },
  { method: 'POST', path: `${HK_API}/tickets/${ID}/release`, perm: 'moderation.tickets', handler: release },
];
