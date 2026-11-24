import { intParam, likeContains, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';
import { charCount, dayString, findUser, parseDay } from './actions-lib.mjs';
import { SECONDS_PER_DAY } from '../lib/time.mjs';

export const owner = 'hk-actions';

export const grants = [
  { table: 'room_chat_messages', select: ['id', 'room_id', 'sender_id', 'target_id', 'message', 'sent_at'] },
  { table: 'players', select: ['id', 'name', 'rank_id'] },
  { table: 'rooms', select: ['id', 'name'] },
];

const PERM = ['moderation.room.chatlog', 'moderation.player.chatlog'];
const PER_PAGE = 100;
const MAX_PAGE = 1000;
const DEFAULT_DAYS = 7;
const MAX_DAYS = 31;
const MIN_TEXT = 3;
const MAX_TEXT = 100;
const PARAMS = ['user', 'room', 'q', 'from', 'to', 'page'];

const range = (fromText, toText, now) => {
  const today = Math.floor(now / SECONDS_PER_DAY) * SECONDS_PER_DAY;
  const to = toText ? parseDay(toText) : today;
  if (to == null) return { error: '“To” is not a date. Use the date picker or YYYY-MM-DD.' };
  const from = fromText ? parseDay(fromText) : to - (DEFAULT_DAYS - 1) * SECONDS_PER_DAY;
  if (from == null) return { error: '“From” is not a date. Use the date picker or YYYY-MM-DD.' };
  if (from > to) return { error: '“From” is after “To”.', from, to };
  const days = (to - from) / SECONDS_PER_DAY + 1;
  if (days > MAX_DAYS) return { error: `That range is ${days} days. Search at most ${MAX_DAYS} days at a time.`, from, to };
  return { from, to };
};

const handler = async (req, res, ctx) => {
  const qp = ctx.query;
  const raw = (k) => String(qp.get(k) ?? '').trim();
  const input = { user: raw('user'), room: raw('room'), q: raw('q'), from: raw('from'), to: raw('to') };
  const now = unixNow();
  const r = range(input.from, input.to, now);
  const search = {
    ...input,
    from: input.from || (r.from != null ? dayString(r.from) : ''),
    to: input.to || (r.to != null ? dayString(r.to) : ''),
  };
  const limits = { defaultDays: DEFAULT_DAYS, maxDays: MAX_DAYS, minText: MIN_TEXT };
  const refuse = (status, errors) => ctx.send.json(res, status, { ok: false, error: errors.join(' '), errors, title: 'Not searched', search, limits });

  if (!input.user && !input.room && !input.q) {
    const submitted = PARAMS.some((k) => qp.has(k));
    if (submitted) return refuse(400, ['Fill in at least one of: user, room id or text.']);
    return ctx.send.json(res, 200, { ok: true, search, limits, results: null });
  }
  if (!ctx.rateLimit(res, 'hk-chatsearch', 20, 60)) return;

  const errors = [];
  if (r.error) errors.push(r.error);
  let roomId = null;
  if (input.room) {
    roomId = intParam(input.room.replace(/^#/, ''));
    if (!roomId) errors.push('Room id is a number, like 123.');
  }
  const qLen = charCount(input.q);
  if (input.q && qLen < MIN_TEXT) errors.push(`Search text needs at least ${MIN_TEXT} characters.`);
  if (qLen > MAX_TEXT) errors.push(`Search text can be at most ${MAX_TEXT} characters.`);
  if (charCount(input.user) > 50) errors.push('That user name is too long.');
  if (errors.length) return refuse(400, errors);

  let user = null;
  if (input.user) {
    user = await findUser(ctx, input.user);
    if (!user) return refuse(404, [`No user has the name or id “${input.user}”.`]);
  }
  let room = null;
  if (roomId) [room] = await ctx.db.query(sql`SELECT id, name FROM rooms WHERE id = ${roomId} LIMIT 1`);

  const pageNo = intParam(qp.get('page'), { max: MAX_PAGE }) ?? 1;
  const where = [sql`c.sent_at >= ${r.from}`, sql`c.sent_at < ${r.to + SECONDS_PER_DAY}`];
  if (user) where.push(sql`c.sender_id = ${Number(user.id)}`);
  if (roomId) where.push(sql`c.room_id = ${roomId}`);
  if (input.q) where.push(sql`c.message LIKE ${likeContains(input.q)}`);
  const found = await ctx.db.query(sql`SELECT c.id, c.room_id, c.sender_id, c.target_id, c.message, c.sent_at AS timestamp,
      uf.name AS from_name, ut.name AS to_name, rm.name AS room_name
    FROM room_chat_messages c
    LEFT JOIN players uf ON uf.id = c.sender_id
    LEFT JOIN players ut ON ut.id = c.target_id
    LEFT JOIN rooms rm ON rm.id = c.room_id
    WHERE ${sql.join(where, ' AND ')}
    ORDER BY c.sent_at DESC, c.id DESC
    LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`);
  const hasNext = found.length > PER_PAGE;
  const rows = found.slice(0, PER_PAGE);

  const criteria = [
    user && `user=#${user.id} ${user.username}`,
    roomId && `room=#${roomId}${room ? ` ${room.name}` : ''}`,
    input.q && `text="${input.q}"`,
    `${dayString(r.from)}..${dayString(r.to)}`,
    `page ${pageNo}`,
    `${rows.length} rows`,
  ].filter(Boolean).join('; ');
  await audit(ctx, {
    action: 'hk.search.chatlog',
    targetType: user ? 'user' : roomId ? 'room' : 'chatlog',
    targetId: user ? Number(user.id) : roomId ?? 0,
    label: user ? user.username : room?.name ?? '',
    detail: criteria,
  });

  return ctx.send.json(res, 200, {
    ok: true, search, limits,
    results: {
      user: user ? { id: Number(user.id), name: String(user.username) } : null,
      roomId, roomExists: roomId ? Boolean(room) : null,
      canRoom: ctx.staff.can('moderation.room.info'),
      from: dayString(r.from), to: dayString(r.to), page: pageNo, perPage: PER_PAGE, hasNext,
      rows: rows.map((m) => ({
        id: Number(m.id), at: Number(m.timestamp), roomId: Number(m.room_id ?? 0), room: m.room_name == null ? null : String(m.room_name),
        senderId: Number(m.sender_id ?? 0), from: m.from_name == null ? null : String(m.from_name),
        targetId: Number(m.target_id ?? 0), to: m.to_name == null ? null : String(m.to_name), message: String(m.message ?? ''),
      })),
    },
  });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/chatlogs`, perm: PERM, audited: true, handler },
];
