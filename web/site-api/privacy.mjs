import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import { loginFirst, signedIn } from '../cms/lib/session.mjs';
import { sql } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';

export const owner = 'me';

export const CHOICES = Object.freeze({
  profileVisible: 'hide_profile',
  onlineStatusVisible: 'hide_online',
  friendCanFollow: 'block_following',
  friendRequestEnabled: 'block_friend_requests',
  roomInvitesEnabled: 'block_room_invites',
});

export const grants = [
  { table: 'player_preferences', select: ['player_id', ...Object.values(CHOICES)] },
  COMMAND_GRANT,
];

const PRIVACY_LIMIT = ['me-privacy', 30, 60];
const PRIVACY_WAIT_MS = 2000;

const set = (v) => String(v ?? '0') === '1' || v === 1 || v === true;

export const privacyOf = async (db, userId) => {
  const [row] = await db.query(sql`SELECT ${sql.join(Object.values(CHOICES).map((c) => sql.ident(c, Object.values(CHOICES))))}
    FROM player_preferences WHERE player_id = ${userId} LIMIT 1`);
  return Object.fromEntries(Object.entries(CHOICES).map(([key, column]) => [key, !set(row?.[column])]));
};

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/me/privacy`,
    handler: async (req, res, ctx) => {
      const s = await signedIn(ctx);
      if (!s) return loginFirst(ctx, res);
      return ctx.send.json(res, 200, { ok: true, privacy: await privacyOf(ctx.db, Number(s.userId)) });
    },
  },
  {
    method: 'POST', path: `${SITE_API}/me/privacy`, write: true, maxBody: 1024,
    handler: async (req, res, ctx) => {
      const s = await signedIn(ctx);
      if (!s) return loginFirst(ctx, res);
      if (!ctx.rateLimit(res, ...PRIVACY_LIMIT)) return;
      const userId = Number(s.userId);
      const given = Object.fromEntries(Object.keys(CHOICES).filter((k) => typeof ctx.body[k] === 'boolean').map((k) => [k, ctx.body[k]]));
      if (Object.keys(given).length === 0) {
        return ctx.send.error(res, 400, 'Bad request', 'The request named no setting to change.');
      }
      const before = await privacyOf(ctx.db, userId);
      const changed = Object.fromEntries(Object.entries(given).filter(([k, v]) => before[k] !== v));
      if (Object.keys(changed).length === 0) return ctx.send.json(res, 200, { ok: true, state: 'saved', privacy: before });
      let row;
      try {
        const id = await ctx.command('setPrivacy', changed);
        [row] = await ctx.waitForCommands([id], { userId, timeoutMs: PRIVACY_WAIT_MS });
      } catch (e) {
        ctx.log(`privacy for user ${userId} not sent: ${e?.message ?? e}`);
        return ctx.send.error(res, 503, 'Not saved', 'Your settings could not be saved right now. Try again in a minute.');
      }
      if (!row || row.status === 'pending') return ctx.send.json(res, 202, { ok: true, state: 'pending', privacy: { ...before, ...changed } });
      if (row.status !== 'done') return ctx.send.error(res, 422, 'Not saved', `The game did not save it: ${row.result || 'it was refused.'}`);
      return ctx.send.json(res, 200, { ok: true, state: 'saved', privacy: await privacyOf(ctx.db, userId) });
    },
  },
];
