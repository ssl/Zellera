import { gameRules } from '../lib/game-rules.mjs';
import { currencyIds } from '../cms/display-names.mjs';
import { COMMAND_GRANT, seasonalName, seasonalTypes } from '../cms/lib/commands.mjs';
import { likePrefix, sql } from '../cms/lib/sql.mjs';
import { SETTING_KEYS, maxFrom } from './me-motto.mjs';
import { unixNow } from '../lib/time.mjs';

export const GRANTS = [
  { table: 'players', select: ['id', 'name', 'motto', 'created_at', 'last_online_at', 'is_online'] },
  { table: 'player_preferences', select: ['player_id', 'hide_profile', 'hide_online'] },
  { table: 'player_balances', select: ['player_id', 'currency', 'amount'] },
  { table: 'guild_members', select: ['player_id', 'guild_id', 'level', 'member_since'] },
  { table: 'guilds', select: ['id', 'owner_id', 'name'] },
  { table: 'friendships', select: ['player_id', 'friend_id'] },
  { table: 'player_subscriptions', select: ['player_id', 'kind', 'started_at', 'duration_seconds', 'active'] },
  { table: 'rooms', select: ['id', 'owner_id', 'name', 'player_count', 'max_players', 'door_mode'] },
  { table: 'news_panels', select: ['id', 'title', 'body', 'enabled', 'sort_order', 'created_at'] },
  { table: 'settings', select: ['name', 'value'] },
  COMMAND_GRANT,
];

const MAX_NAME = 50;
const NEWS_ROWS = 4;
const FRIEND_ROWS = 12;
const ROOM_ROWS = 5;
const GROUP_ROWS = 6;
export const SEARCH_ROWS = 20;

export const ownUser = async (db, userId) => {
  const [u] = await db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.motto, u.created_at AS member_since,
      COALESCE(u.last_online_at, 0) AS last_online,
      u.is_online AS online, COALESCE(b.amount, 0) AS credits, COALESCE(s.hide_online, FALSE) AS hide_online
    FROM players u LEFT JOIN player_preferences s ON s.player_id = u.id
      LEFT JOIN player_balances b ON b.player_id = u.id AND b.currency = -1
    WHERE u.id = ${userId} LIMIT 1`);
  return u ?? null;
};

export const mottoRules = async (db) => {
  const settings = [...await db.query(sql`SELECT \`name\`, \`value\` FROM settings WHERE \`name\` IN (${sql.in(SETTING_KEYS)})`)];
  return { max: maxFrom(settings) };
};

export const purse = async (db, userId, texts, rules = gameRules()) => {
  const seasonal = seasonalTypes(rules);
  const ids = currencyIds();
  const held = [ids.points, ids.premium].filter((id) => id !== null);
  const wanted = [...held, ...seasonal];
  const rows = wanted.length === 0 ? [] : await db.query(sql`SELECT currency AS type, amount FROM player_balances
    WHERE player_id = ${userId} AND currency IN (${sql.in(wanted)})`);
  const byType = new Map(rows.map((r) => [Number(r.type), Number(r.amount)]));
  return {
    points: byType.get(ids.points) ?? 0,
    premium: byType.get(ids.premium) ?? 0,
    seasonal: seasonal.map((type) => ({ type, name: seasonalName(texts, type), amount: byType.get(type) ?? 0 })),
  };
};

export const memberships = async (db, userId) => {
  const rows = await db.query(sql`SELECT kind, MAX(started_at + duration_seconds) AS until FROM player_subscriptions
    WHERE player_id = ${userId} AND active = TRUE AND kind IN ('club', 'build_pass') GROUP BY kind`);
  const now = unixNow();
  const until = (kind) => {
    const t = Number([...rows].find((r) => String(r.kind) === kind)?.until) || 0;
    return t > now ? { until: t } : null;
  };
  return { club: until('club'), buildPass: until('build_pass') };
};

export const myRooms = async (db, userId) => {
  const [{ n } = { n: 0 }] = await db.query(sql`SELECT COUNT(*) AS n FROM rooms WHERE owner_id = ${userId} AND door_mode <> 'invisible'`);
  const rows = Number(n) > 0 ? await db.query(sql`SELECT id, name, player_count, max_players, door_mode FROM rooms
    WHERE owner_id = ${userId} AND door_mode <> 'invisible' ORDER BY player_count DESC, id DESC LIMIT ${ROOM_ROWS}`) : [];
  return { total: Number(n), rows: [...rows] };
};

export const myGroups = async (db, userId) => {
  const [{ n } = { n: 0 }] = await db.query(sql`SELECT COUNT(*) AS n FROM guild_members WHERE player_id = ${userId} AND level IN (0, 1, 2)`);
  const rows = Number(n) > 0 ? await db.query(sql`SELECT g.id, g.name, g.owner_id, m.level
    FROM guild_members m JOIN guilds g ON g.id = m.guild_id
    WHERE m.player_id = ${userId} AND m.level IN (0, 1, 2)
    ORDER BY m.member_since ASC, g.id ASC LIMIT ${GROUP_ROWS}`) : [];
  return { total: Number(n), rows: [...rows] };
};

export const latestNews = (db) => db.query(sql`SELECT id, title, LEFT(body, 600) AS body, created_at AS created
  FROM news_panels WHERE enabled = 1 ORDER BY sort_order ASC, id DESC LIMIT ${NEWS_ROWS}`);

export const friends = async (db, userId) => {
  const [{ n } = { n: 0 }] = await db.query(sql`SELECT COUNT(*) AS n FROM friendships f
    JOIN players u ON u.id = f.friend_id WHERE f.player_id = ${userId}`);
  const rows = Number(n) > 0 ? await db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender,
      (u.is_online = TRUE AND COALESCE(s.hide_online, FALSE) = FALSE) AS on_now
    FROM friendships f
    JOIN players u ON u.id = f.friend_id
    LEFT JOIN player_preferences s ON s.player_id = u.id
    WHERE f.player_id = ${userId}
    ORDER BY on_now DESC, u.name ASC LIMIT ${FRIEND_ROWS}`) : [];
  return { total: Number(n), rows: [...rows] };
};

export const searchable = (q) => typeof q === 'string' && q.length > 0 && q.length <= MAX_NAME
  && !/[\u{10000}-\u{10FFFF}]/u.test(q) && !/[\u0000-\u001f\u007f]/.test(q);

export const searchUsers = async (db, q, viewerId, seesPrivate = false) => {
  if (!searchable(q)) return [];
  const rows = await db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.motto,
      (u.is_online = TRUE AND COALESCE(s.hide_online, FALSE) = FALSE) AS on_now
    FROM players u LEFT JOIN player_preferences s ON s.player_id = u.id
    WHERE u.name LIKE ${likePrefix(q)}
      ${seesPrivate ? sql`` : sql`AND (COALESCE(s.hide_profile, FALSE) = FALSE OR u.id = ${viewerId})`}
    ORDER BY (u.name = ${q}) DESC, u.name ASC
    LIMIT ${SEARCH_ROWS + 1}`);
  return [...rows];
};
