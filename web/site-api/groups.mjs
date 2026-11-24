import { urls } from '../cms/lib/html.mjs';
import { sql, intParam } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';
import { LOOKUP_LIMIT, groupTypeLabel } from './rooms-common.mjs';

export const owner = 'community';
export const grants = [
  { table: 'guilds', select: ['id', 'owner_id', 'name', 'description', 'room_id', 'join_mode', 'created_at', 'has_forum'] },
  { table: 'guild_members', select: ['guild_id', 'player_id', 'level', 'member_since'] },
  { table: 'players', select: ['id', 'name', 'figure', 'gender'] },
  { table: 'rooms', select: ['id', 'name', 'door_mode', 'player_count', 'max_players'] },
];

const MEMBER_LIMIT = 100;

const loadGroup = async (ctx, id) => {
  const [group] = await ctx.db.query(sql`
    SELECT g.id, g.name, g.description, g.join_mode, g.created_at, g.has_forum,
           u.id AS owner_id, u.name AS owner_username,
           r.id AS room_id, r.name AS room_name, r.player_count AS room_users, r.max_players AS room_users_max
      FROM guilds g
      LEFT JOIN players u ON u.id = g.owner_id
      LEFT JOIN rooms r ON r.id = g.room_id AND r.door_mode <> 'invisible'
     WHERE g.id = ${id}
     LIMIT 1`);
  if (!group) return null;
  const [count] = await ctx.db.query(sql`
    SELECT COUNT(gm.player_id) AS n
      FROM guild_members gm
      JOIN players u ON u.id = gm.player_id
     WHERE gm.guild_id = ${id} AND gm.level IN (0, 1, 2)`);
  const members = await ctx.db.query(sql`
    SELECT u.id, u.name AS username, u.figure, u.gender, gm.level, gm.member_since
      FROM guild_members gm
      JOIN players u ON u.id = gm.player_id
     WHERE gm.guild_id = ${id} AND gm.level IN (0, 1, 2)
     ORDER BY gm.level ASC, gm.member_since ASC, gm.player_id ASC
     LIMIT ${MEMBER_LIMIT}`);
  return { ...group, memberCount: Number(count?.n ?? 0), members: [...members] };
};

const ROLES = ['Owner', 'Admin', 'Member'];

const groupAnswer = (ctx, g) => ({
  ok: true,
  group: {
    id: Number(g.id), name: String(g.name || `Group ${g.id}`), description: String(g.description ?? ''),
    type: groupTypeLabel(ctx, g.join_mode), created: Number(g.created_at) || 0, forum: Number(g.has_forum) === 1,
    owner: g.owner_username ? { id: Number(g.owner_id), name: String(g.owner_username) } : null,
    room: g.room_id ? { id: Number(g.room_id), name: String(g.room_name ?? ''), users: Number(g.room_users) || 0, max: Number(g.room_users_max) || 0 } : null,
  },
  memberCount: g.memberCount,
  members: g.members.map((m) => ({
    id: Number(m.id), name: String(m.username), figure: String(m.figure ?? ''), gender: String(m.gender ?? 'M'),
    role: ROLES[Number(m.level)] ?? 'Member', since: Number(m.member_since) || 0,
  })),
});

export const routes = [
  {
    method: 'GET', path: '/groups/:id(\\d{1,10})', gate: 'players',
    handler: async (req, res, ctx) => {
      const id = intParam(ctx.params.id);
      if (!id) return ctx.send.notFound(res, 'There is no group with this number.');
      return ctx.send.redirect(res, urls.group(id));
    },
  },
  {
    method: 'GET', path: `${SITE_API}/groups/:id(\\d{1,10})`, gate: 'players',
    handler: async (req, res, ctx) => {
      if (!ctx.rateLimit(res, ...LOOKUP_LIMIT)) return;
      const id = intParam(ctx.params.id);
      const group = id ? await loadGroup(ctx, id) : null;
      if (!group) return ctx.send.notFound(res, 'There is no group with this number.');
      return ctx.send.json(res, 200, groupAnswer(ctx, group));
    },
  },
];
