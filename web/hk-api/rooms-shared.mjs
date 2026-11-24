import { sql } from '../cms/lib/sql.mjs';

export const USER_GRANT = Object.freeze({ table: 'players', select: ['id', 'name'] });
export const ROOM_NAME_GRANT = Object.freeze({ table: 'rooms', select: ['id', 'name', 'door_mode'] });
const TICKET_COLS = Object.freeze([
  'id', 'status', 'resolution', 'kind', 'created_at', 'priority', 'reporter_id', 'accused_id', 'room_id', 'moderator_id',
  'message', 'topic_id', 'guild_id', 'forum_thread_id', 'forum_post_id', 'photo_furni_id', 'closed_at',
  'appeal', 'appealed_at', 'appeal_resolved_at',
]);
export const TICKET_GRANTS = Object.freeze([
  { table: 'support_tickets', select: [...TICKET_COLS] },
  { table: 'report_topics', select: ['id', 'category_id', 'internal_name', 'display_name'] },
  { table: 'report_categories', select: ['id', 'internal_name', 'display_name'] },
]);
export const CHAT_ROOM_GRANT = Object.freeze({ table: 'room_chat_messages', select: ['id', 'room_id', 'sender_id', 'target_id', 'message', 'sent_at'] });

const ids = (list) => [...new Set(Array.from(list ?? [], Number).filter((n) => Number.isInteger(n) && n > 0))];
const chunks = (arr, size = 1000) => Array.from({ length: Math.ceil(arr.length / size) }, (_, i) => arr.slice(i * size, (i + 1) * size));

export const loadUsers = async (ctx, list) => {
  const map = new Map();
  for (const part of chunks(ids(list))) {
    const rows = await ctx.db.query(sql`SELECT id, name AS username, figure, gender FROM players WHERE id IN (${sql.in(part)})`);
    for (const r of rows) map.set(Number(r.id), r);
  }
  return map;
};

export const loadRooms = async (ctx, list) => {
  const map = new Map();
  for (const part of chunks(ids(list))) {
    const rows = await ctx.db.query(sql`SELECT id, name, door_mode FROM rooms WHERE id IN (${sql.in(part)})`);
    for (const r of rows) map.set(Number(r.id), r);
  }
  return map;
};

export const peopleOf = (users) => Object.fromEntries([...users].map(([id, u]) => [id, {
  id, name: String(u.username), figure: String(u.figure ?? ''), gender: String(u.gender ?? 'M'),
}]));
export const roomsOf = (rooms) => Object.fromEntries([...rooms].map(([id, r]) => [id, { id, name: String(r.name ?? ''), door: String(r.door_mode ?? '') }]));

export const caption = (ctx, value) => {
  const m = /^\$\{([^}]{1,200})\}$/.exec(String(value ?? ''));
  return m ? ctx.texts.get(m[1], m[1]) : String(value ?? '');
};

export const chatLine = (r) => (r.marker
  ? { marker: r.marker, at: Number(r.timestamp), text: String(r.text) }
  : {
    id: Number(r.id), at: Number(r.timestamp), roomId: Number(r.room_id ?? 0), senderId: Number(r.sender_id ?? 0),
    targetId: Number(r.target_id ?? 0), message: String(r.message ?? ''),
  });

export const cutLogPage = (rows, size) => {
  if (rows.length <= size) return { rows, nextBefore: null };
  const boundary = Number(rows[size].timestamp);
  const page = rows.slice(0, size).filter((r) => Number(r.timestamp) > boundary);
  if (page.length === 0) return { rows: rows.slice(0, size), nextBefore: boundary };
  return { rows: page, nextBefore: boundary + 1 };
};

export const TICKET_STATES = Object.freeze([
  { key: 'open', value: 'open', label: 'Open' },
  { key: 'picked', value: 'picked', label: 'Picked' },
  { key: 'closed', value: 'closed', label: 'Closed' },
]);
const stateOf = (value) => TICKET_STATES.find((s) => s.value === String(value)) ?? { key: 'unknown', value, label: `State ${value}` };

const TICKET_TYPES = Object.freeze({
  1: ['NORMAL', 'User report'],
  2: ['NORMAL_UNKNOWN', 'User report'],
  3: ['AUTOMATIC', 'Automatic'],
  4: ['AUTOMATIC_IM', 'Automatic (private chat)'],
  5: ['GUIDE_SYSTEM', 'Guide report'],
  6: ['IM', 'Private chat report'],
  7: ['ROOM', 'Room report'],
  8: ['PANIC', 'Panic'],
  9: ['GUARDIAN', 'Guardian (bully report)'],
  10: ['AUTOMATIC_HELPER', 'Automatic (helper)'],
  11: ['DISCUSSION', 'Forum report'],
  12: ['SELFIE', 'Selfie report'],
  13: ['POTATO', 'Potato'],
  14: ['PHOTO', 'Photo report'],
  15: ['AMBASSADOR', 'Ambassador'],
});
export const UNSAVED_TYPE = 0;

export const ticketType = (t) => {
  const stored = Number(t.kind);
  if (stored !== 1) return { type: stored, inferred: false };
  if (Number(t.photo_furni_id) > 0) return { type: 14, inferred: true };
  if (Number(t.forum_thread_id) > 0) return { type: 11, inferred: true };
  if (!(Number(t.sender_id) > 0)) return { type: 3, inferred: true };
  if (!(Number(t.room_id) > 0)) return { type: UNSAVED_TYPE, inferred: true };
  return { type: 1, inferred: false };
};

export const typeOf = (t) => {
  const { type, inferred } = ticketType(t);
  if (type === UNSAVED_TYPE) return { type, code: '', words: 'Not saved yet', note: 'private chat, guide or guardian report', inferred };
  const [code, words] = TICKET_TYPES[type] ?? [`TYPE_${type}`, 'Unknown type'];
  return { type, code, words, note: inferred ? "from the ticket's data" : '', inferred };
};

export const CLIENT_TOPICS = Object.freeze({ 0: 'Automatic', 1024: 'Guide', 1025: 'Bullying', 1026: 'Severe Alert' });

export const topicOf = (t) => {
  const id = Number(t.topic_id);
  if (t.topic_name == null && t.topic_key == null) {
    const own = CLIENT_TOPICS[id];
    if (own === undefined) return { id, label: `#${id}`, note: '' };
    return { id, label: own, note: `topic ${id}, filed by the game` };
  }
  return { id, label: String(t.topic_name || t.topic_key), note: t.topic_category ? String(t.topic_category) : '' };
};

export const TICKET_SELECT = sql`st.id, st.status AS state, st.kind, st.created_at, st.priority AS score,
  st.reporter_id AS sender_id, st.accused_id AS reported_id, st.room_id,
  st.moderator_id, st.message AS issue, st.topic_id, st.guild_id, st.forum_thread_id, st.forum_post_id, st.photo_furni_id, st.closed_at,
  (st.resolution = 'handled') AS sanctioned, (st.resolution = 'automatic') AS sanction_auto,
  FIELD(st.appeal, 'pending', 'upheld', 'rejected') AS appeal_state, st.appealed_at, st.appeal_resolved_at,
  tp.display_name AS topic_name, tp.internal_name AS topic_key, tc.display_name AS topic_category`;
export const TICKET_JOIN = sql`support_tickets st
  LEFT JOIN report_topics tp ON tp.id = st.topic_id
  LEFT JOIN report_categories tc ON tc.id = tp.category_id`;

export const ticketJson = (t) => {
  const st = stateOf(t.state);
  return {
    id: Number(t.id), state: { key: st.key, label: st.label }, type: typeOf(t), topic: topicOf(t),
    senderId: Number(t.sender_id ?? 0), reportedId: Number(t.reported_id ?? 0), roomId: Number(t.room_id ?? 0),
    moderatorId: Number(t.moderator_id ?? 0), created: Number(t.created_at ?? 0), closed: Number(t.closed_at ?? 0),
    issue: String(t.issue ?? ''), score: Number(t.score ?? 0), photoItemId: Number(t.photo_furni_id ?? 0),
  };
};

export const READ_ONLY_NOTE = 'Housekeeping only reads. Pick, release, close and sanction in the in-client mod tool: the game server keeps open tickets and rooms in memory, and this site has no line to it.';
