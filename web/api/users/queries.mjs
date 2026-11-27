export const GRANTS = [
  { table: 'players', select: ['id', 'name', 'figure', 'motto', 'gender', 'is_online', 'last_online_at', 'created_at'] },
  { table: 'player_preferences', select: ['player_id', 'hide_profile', 'hide_online'] },
  { table: 'player_stats', select: ['player_id', 'achievement_score', 'respect_received'] },
  { table: 'player_badges', select: ['player_id', 'slot', 'badge_code'] },
  { table: 'player_account_levels', select: ['player_id', 'counted_points', 'level'] },
  { table: 'player_achievements', select: ['player_id', 'achievement_id', 'progress'] },
  { table: 'achievements', select: ['id', 'code', 'category', 'state'] },
  { table: 'achievement_levels', select: ['achievement_id', 'level', 'progress_needed'] },
  { table: 'friendships', select: ['player_id', 'friend_id'] },
  { table: 'guilds', select: ['id', 'name', 'description', 'join_mode', 'room_id', 'badge_code', 'color_one', 'color_two'] },
  { table: 'guild_members', select: ['guild_id', 'player_id', 'level', 'member_since'] },
  { table: 'guild_badge_parts', select: ['id', 'kind', 'asset'] },
  { table: 'rooms', select: ['id', 'owner_id', 'name', 'description', 'created_at', 'guild_id', 'max_players', 'is_public', 'category_id', 'score', 'door_mode'] },
  { table: 'room_tags', select: ['room_id', 'position', 'tag'] },
  { table: 'room_categories', select: ['id', 'caption'] },
  { table: 'camera_photos', select: ['id', 'player_id', 'room_id', 'created_at_ms', 'published', 'published_at'] },
];

const LIMITS = { friends: 5000, rooms: 500, members: 1000, photos: 200 };

export const canBeUtf8mb3 = (s) => !/[\u{10000}-\u{10FFFF}]/u.test(s);

const USER = `u.id, u.name AS username, u.figure AS look, u.motto, u.is_online AS online, COALESCE(u.last_online_at, 0) AS last_online,
  u.created_at AS account_created, COALESCE(s.hide_profile, FALSE) AS hide_profile, COALESCE(s.hide_online, FALSE) AS hide_online,
  COALESCE(t.achievement_score, 0) AS achievement_score, COALESCE(t.respect_received, 0) AS respects_received`;
const USER_FROM = `FROM players u LEFT JOIN player_preferences s ON s.player_id = u.id LEFT JOIN player_stats t ON t.player_id = u.id`;

export const userByName = (db, name) => db.one(`SELECT ${USER}
  ${USER_FROM}
  WHERE u.name = ? LIMIT 1`, [name]);

export const userById = (db, id) => db.one(`SELECT ${USER}
  ${USER_FROM}
  WHERE u.id = ? LIMIT 1`, [id]);

export const wornBadges = (db, userId) => db.query(`SELECT b.slot AS slot_id, b.badge_code FROM player_badges b
  WHERE b.player_id = ? AND b.slot > 0 ORDER BY b.slot, b.badge_code`, [userId]);

export const accountLevel = async (db, userId) => {
  const row = await db.one('SELECT a.counted_points, a.level FROM player_account_levels a WHERE a.player_id = ?', [userId]);
  return row ? { points: Number(row.counted_points), level: Number(row.level) } : null;
};

export const allBadges = (db, userId) => db.query(`SELECT b.badge_code FROM player_badges b
  WHERE b.player_id = ?`, [userId]);

export const badgeOwnerCount = async (db, code) => {
  const row = await db.one(`SELECT COUNT(b.player_id) AS n FROM player_badges b
    WHERE b.badge_code = ?`, [code]);
  return Number(row?.n ?? 0);
};

export const friends = (db, userId) => db.query(`SELECT u.id, u.name AS username, u.figure AS look, u.motto, u.is_online AS online,
    COALESCE(s.hide_online, FALSE) AS hide_online
  FROM friendships f JOIN players u ON u.id = f.friend_id LEFT JOIN player_preferences s ON s.player_id = u.id
  WHERE f.player_id = ?
  ORDER BY u.name, u.id LIMIT ${LIMITS.friends}`, [userId]);

export const userAchievements = (db, userId) => db.query(`SELECT a.code AS achievement_name, ua.progress FROM player_achievements ua
  JOIN achievements a ON a.id = ua.achievement_id WHERE ua.player_id = ?`, [userId]);

export const achievementRows = (db) => db.query(`SELECT a.id, a.code AS name, a.category, l.level, l.progress_needed, a.state
  FROM achievements a JOIN achievement_levels l ON l.achievement_id = a.id ORDER BY a.code, l.level`);

const GROUP = `g.id, g.name, g.description, g.join_mode, g.room_id, g.badge_code,
  sc.asset AS primary_colour, bc.asset AS secondary_colour`;
const COLOURS = `LEFT JOIN guild_badge_parts sc ON sc.kind = 'color2' AND sc.id = g.color_one
  LEFT JOIN guild_badge_parts bc ON bc.kind = 'color3' AND bc.id = g.color_two`;

export const groupById = (db, id) => db.one(`SELECT ${GROUP} FROM guilds g ${COLOURS} WHERE g.id = ? LIMIT 1`, [id]);

export const userGroups = (db, userId) => db.query(`SELECT ${GROUP}, m.level
  FROM guild_members m JOIN guilds g ON g.id = m.guild_id ${COLOURS}
  WHERE m.player_id = ? AND m.level IN (0, 1, 2)
  ORDER BY g.name, g.id`, [userId]);

export const groupMembers = (db, groupId) => db.query(`SELECT u.id, u.name AS username, u.figure AS look, u.motto, u.gender, u.is_online AS online,
    u.created_at AS account_created, COALESCE(s.hide_online, FALSE) AS hide_online, m.level
  FROM guild_members m JOIN players u ON u.id = m.player_id LEFT JOIN player_preferences s ON s.player_id = u.id
  WHERE m.guild_id = ? AND m.level IN (0, 1, 2)
  ORDER BY m.member_since, m.player_id LIMIT ${LIMITS.members}`, [groupId]);

const ROOM = `SELECT r.id, r.name, r.description, r.created_at, g.id AS group_id,
    COALESCE((SELECT GROUP_CONCAT(CONCAT(t.tag, ';') ORDER BY t.position SEPARATOR '') FROM room_tags t WHERE t.room_id = r.id), '') AS tags,
    r.max_players, r.is_public, r.score, r.door_mode, r.owner_id, u.name AS owner_name, c.caption
  FROM rooms r JOIN players u ON u.id = r.owner_id
  LEFT JOIN guilds g ON g.id = r.guild_id
  LEFT JOIN room_categories c ON c.id = r.category_id`;

export const roomById = (db, id) => db.one(`${ROOM} WHERE r.id = ? AND r.door_mode <> 'invisible' LIMIT 1`, [id]);

export const userRooms = (db, userId) => db.query(`${ROOM}
  WHERE r.owner_id = ? AND r.door_mode <> 'invisible' ORDER BY r.id LIMIT ${LIMITS.rooms}`, [userId]);

const PHOTO = `SELECT p.id, p.player_id AS user_id, p.room_id, p.created_at_ms AS created_at, u.name AS username FROM camera_photos p JOIN players u ON u.id = p.player_id`;

export const userPhotos = (db, userId) => db.query(`${PHOTO}
  WHERE p.player_id = ? AND p.published = 1
  ORDER BY p.published_at DESC, p.id LIMIT ${LIMITS.photos}`, [userId]);

export const publicPhotos = (db) => db.query(`${PHOTO} LEFT JOIN player_preferences s ON s.player_id = u.id
  WHERE p.published = 1 AND COALESCE(s.hide_profile, FALSE) = FALSE
  ORDER BY p.published_at DESC, p.id LIMIT ${LIMITS.photos}`);
