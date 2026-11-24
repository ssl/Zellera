import { sql, intParam } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';
import { LOOKUP_LIMIT, doorLabel, resolveText } from './rooms-common.mjs';

export const owner = 'community';
export const grants = [
  { table: 'rooms', select: ['id', 'owner_id', 'name', 'description', 'door_mode', 'player_count', 'max_players', 'guild_id', 'category_id', 'created_at'] },
  { table: 'room_tags', select: ['room_id', 'position', 'tag'] },
  { table: 'settings', select: ['name', 'value'] },
  { table: 'players', select: ['id', 'name'] },
  { table: 'guilds', select: ['id', 'name'] },
  { table: 'room_categories', select: ['id', 'caption'] },
];

const loadRoom = async (ctx, id) => {
  const [room] = await ctx.db.query(sql`
    SELECT r.id, r.owner_id, r.name, r.description, r.door_mode, r.player_count, r.max_players, r.created_at,
           COALESCE((SELECT GROUP_CONCAT(CONCAT(t.tag, ';') ORDER BY t.position SEPARATOR '') FROM room_tags t WHERE t.room_id = r.id), '') AS tags,
           (SELECT \`value\` FROM settings WHERE \`name\` = 'room.ownerless.owner_name') AS nobody_name,
           u.name AS owner_username, g.id AS group_id, g.name AS group_name, c.caption AS category_caption
      FROM rooms r
      LEFT JOIN players u ON u.id = r.owner_id
      LEFT JOIN guilds g ON g.id = r.guild_id
      LEFT JOIN room_categories c ON c.id = r.category_id
     WHERE r.id = ${id} AND r.door_mode <> 'invisible'
     LIMIT 1`);
  return room ?? null;
};

const splitTags = (tags) => String(tags ?? '').split(';').map((t) => t.trim()).filter(Boolean);

const roomAnswer = (ctx, room) => ({
  ok: true,
  room: {
    id: Number(room.id), name: String(room.name || `Room ${room.id}`), description: String(room.description ?? ''),
    owner: room.owner_username ? { id: Number(room.owner_id), name: String(room.owner_username) } : null,
    ownerLabel: room.owner_username ? null : (room.nobody_name || null),
    category: resolveText(ctx, room.category_caption) || null,
    users: Number(room.player_count) || 0, max: Number(room.max_players) || 0,
    door: doorLabel(ctx, room.door_mode),
    group: room.group_id ? { id: Number(room.group_id), name: String(room.group_name || `Group ${room.group_id}`) } : null,
    created: Number(room.created_at) || 0,
    tags: splitTags(room.tags),
  },
});

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/rooms/:id(\\d{1,10})`, gate: 'players',
    handler: async (req, res, ctx) => {
      if (!ctx.rateLimit(res, ...LOOKUP_LIMIT)) return;
      const id = intParam(ctx.params.id);
      const room = id ? await loadRoom(ctx, id) : null;
      if (!room) return ctx.send.notFound(res, 'There is no room with this number, or it is not open to visitors.');
      return ctx.send.json(res, 200, roomAnswer(ctx, room));
    },
  },
];
