import { sql } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';

export const owner = 'community';
export const grants = [
  { table: 'players', select: ['id', 'name', 'figure', 'gender', 'motto', 'rank_id', 'is_online'] },
  { table: 'player_preferences', select: ['player_id', 'hide_online'] },
  { table: 'ranks', select: ['id', 'name'] },
];

const STAFF_FROM = 4;
const MAX_ROWS = 200;

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/staff`, gate: 'community', shown: 'staff',
    handler: async (req, res, ctx) => {
      const rows = await ctx.db.query(sql`SELECT p.id, p.name, p.figure, p.gender, p.motto, p.rank_id, r.name AS rank_name,
          (p.is_online = TRUE AND COALESCE(s.hide_online, FALSE) = FALSE) AS on_now
        FROM players p JOIN ranks r ON r.id = p.rank_id LEFT JOIN player_preferences s ON s.player_id = p.id
        WHERE p.rank_id >= ${STAFF_FROM} ORDER BY p.rank_id DESC, p.name ASC LIMIT ${MAX_ROWS}`);
      const ranks = [];
      for (const r of rows) {
        let rank = ranks.at(-1);
        if (rank?.level !== Number(r.rank_id)) ranks.push(rank = { level: Number(r.rank_id), name: String(r.rank_name), members: [] });
        rank.members.push({
          id: Number(r.id), name: String(r.name), figure: String(r.figure ?? ''), gender: String(r.gender ?? 'M'),
          motto: String(r.motto ?? ''), online: Number(r.on_now) === 1,
        });
      }
      return ctx.send.json(res, 200, { ok: true, ranks });
    },
  },
];
