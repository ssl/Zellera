import { sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { LOG_COLUMNS, logRow } from './users-shared.mjs';
import { VERSION } from '../lib/env.mjs';
import { SECONDS_PER_DAY } from '../lib/time.mjs';

export const owner = 'hk-users';

export const grants = [
  { table: 'players', select: ['id', 'name', 'is_online', 'created_at', 'last_login_at'] },
  { table: 'rooms', select: ['player_count'] },
  { table: 'support_tickets', select: ['status'] },
  { table: 'bans', select: ['ends_at'] },
  { table: 'staff_actions', select: LOG_COLUMNS },
  { table: 'settings', select: ['name', 'value'] },
];

const SERVER_VERSION = 'server.version';

const ONLINE_LIST = 30;

const dashboard = async (req, res, ctx) => {
  const { staff } = ctx;
  const now = unixNow();
  const canTickets = staff.can('moderation.tickets');
  const canBans = staff.can('moderation.player.ban');
  const none = Promise.resolve([]);
  const [[online], [fresh], [rooms], [tickets], [bans], onlineList, log, [server]] = await Promise.all([
    ctx.db.query(sql`SELECT COUNT(*) AS n FROM players WHERE is_online = TRUE`),
    ctx.db.query(sql`SELECT SUM(created_at >= ${now - SECONDS_PER_DAY}) AS d1, COUNT(*) AS d7 FROM players WHERE created_at >= ${now - 7 * SECONDS_PER_DAY}`),
    ctx.db.query(sql`SELECT COUNT(*) AS n FROM rooms WHERE player_count > 0`),
    canTickets ? ctx.db.query(sql`SELECT SUM(status = 'open') AS open, SUM(status = 'picked') AS picked FROM support_tickets WHERE status IN ('open', 'picked')`) : Promise.resolve([{}]),
    canBans ? ctx.db.query(sql`SELECT COUNT(*) AS n FROM bans WHERE ends_at > ${now}`) : Promise.resolve([{}]),
    ctx.db.query(sql`SELECT id, name AS username, COALESCE(last_login_at, 0) AS last_login FROM players WHERE is_online = TRUE ORDER BY last_login_at DESC, id LIMIT ${ONLINE_LIST + 1}`),
    canBans ? ctx.db.query(sql`SELECT a.id, a.acted_at, a.staff_id, s.name AS staff_name, a.action, a.subject_type, a.subject_id, a.subject_label, a.detail, a.success
      FROM staff_actions a LEFT JOIN players s ON s.id = a.staff_id ORDER BY a.id DESC LIMIT 10`) : none,
    ctx.db.query(sql`SELECT value FROM settings WHERE name = ${SERVER_VERSION}`),
  ]);
  const n = (v) => Number(v) || 0;
  return ctx.send.json(res, 200, {
    ok: true,
    stats: {
      online: n(online?.n),
      newDay: n(fresh?.d1),
      newWeek: n(fresh?.d7),
      roomsWithUsers: n(rooms?.n),
      ...(canTickets ? { openTickets: n(tickets?.open), pickedTickets: n(tickets?.picked) } : {}),
      ...(canBans ? { activeBans: n(bans?.n) } : {}),
    },
    online: onlineList.slice(0, ONLINE_LIST).map((u) => ({ id: n(u.id), name: String(u.username), lastLogin: n(u.last_login) })),
    log: canBans ? log.map(logRow) : null,
    version: { site: VERSION, server: String(server?.value ?? '') },
  });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/dashboard`, perm: 'moderation.tool', handler: dashboard },
];
