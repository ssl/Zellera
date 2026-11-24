import { sql } from '../cms/lib/sql.mjs';

export const GRANTS = [
  { table: 'players', select: ['id', 'name', 'figure', 'gender', 'motto', 'created_at', 'last_online_at', 'is_online'] },
  { table: 'player_preferences', select: ['player_id', 'hide_profile', 'hide_online'] },
  { table: 'guild_members', select: ['player_id', 'guild_id', 'level', 'member_since'] },
  { table: 'guilds', select: ['id', 'owner_id', 'name'] },
  { table: 'rooms', select: ['id', 'owner_id', 'name', 'player_count', 'max_players', 'door_mode'] },
  { table: 'friendships', select: ['player_id', 'friend_id'] },
  { table: 'camera_photos', select: ['id', 'player_id', 'room_id', 'published', 'published_at'] },
];

export const MAX_NAME = 50;
export const MAX_ROOMS = 50;
const MAX_GROUPS = 100;
export const MAX_PHOTOS = 24;
const FRIEND_ROWS = 12;

const USER_COLUMNS = sql`u.id, u.name AS username, u.figure, u.gender, u.motto, u.created_at AS account_created, COALESCE(u.last_online_at, 0) AS last_online,
  u.is_online AS online, COALESCE(s.hide_profile, FALSE) AS hide_profile, COALESCE(s.hide_online, FALSE) AS hide_online`;

export const userByName = async (db, name) => {
  const [u] = await db.query(sql`SELECT ${USER_COLUMNS}
    FROM players u LEFT JOIN player_preferences s ON s.player_id = u.id
    WHERE u.name = ${String(name)} LIMIT 1`);
  return u ?? null;
};

export const usernameById = async (db, id) => {
  const [u] = await db.query(sql`SELECT name AS username FROM players WHERE id = ${id} LIMIT 1`);
  return u?.username ?? null;
};

export const memberGroups = (db, userId) => db.query(sql`SELECT g.id, g.name, g.owner_id AS owner_id, m.level
  FROM guild_members m JOIN guilds g ON g.id = m.guild_id
  WHERE m.player_id = ${userId} AND m.level IN (0, 1, 2)
  ORDER BY m.member_since ASC, g.id ASC LIMIT ${MAX_GROUPS}`);

export const ownedRooms = (db, userId) => db.query(sql`SELECT id, name, player_count, max_players, door_mode FROM rooms
  WHERE owner_id = ${userId} AND door_mode <> 'invisible'
  ORDER BY player_count DESC, id DESC LIMIT ${MAX_ROOMS}`);

export const roomName = async (db, roomId) => {
  const [r] = await db.query(sql`SELECT id, name FROM rooms WHERE id = ${roomId} AND door_mode <> 'invisible' LIMIT 1`);
  return r ?? null;
};

export const friendCount = async (db, userId) => {
  const [r] = await db.query(sql`SELECT COUNT(*) AS n FROM friendships WHERE player_id = ${userId}`);
  return Number(r?.n ?? 0);
};

export const shownFriends = (db, userId) => db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender,
    (u.is_online = TRUE AND COALESCE(s.hide_online, FALSE) = FALSE) AS on_now
  FROM friendships f
  JOIN players u ON u.id = f.friend_id
  LEFT JOIN player_preferences s ON s.player_id = u.id
  WHERE f.player_id = ${userId} AND COALESCE(s.hide_profile, FALSE) = FALSE
  ORDER BY on_now DESC, u.name ASC LIMIT ${FRIEND_ROWS}`);

export const publishedPhotos = (db, userId) => db.query(sql`SELECT id, room_id, published_at FROM camera_photos
  WHERE player_id = ${userId} AND published = 1
  ORDER BY published_at DESC, id ASC LIMIT ${MAX_PHOTOS}`);

export const publishedPhoto = async (db, userId, photoId) => {
  const [p] = await db.query(sql`SELECT id, room_id, published_at FROM camera_photos
    WHERE id = ${String(photoId)} AND player_id = ${userId} AND published = 1 LIMIT 1`);
  return p ?? null;
};

export const DOOR = Object.freeze({ open: 'open', locked: 'doorbell', password: 'password' });
export const ROLE = Object.freeze(['Owner', 'Admin', 'Member']);
