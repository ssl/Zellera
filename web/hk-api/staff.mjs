import { HK_API } from './index.mjs';
import { HK_PERMS } from './_guard.mjs';

export const owner = 'hk-users';
export const grants = [];

export const routes = [
  {
    method: 'GET', path: `${HK_API}/me`, perm: 'moderation.tool',
    handler: async (req, res, ctx) => ctx.send.json(res, 200, {
      ok: true,
      name: ctx.GAME_NAME,
      staff: { id: ctx.staff.id, name: ctx.staff.username, rank: ctx.staff.rank, rankName: ctx.staff.rankName },
      can: Object.fromEntries(HK_PERMS.map((k) => [k, ctx.staff.can(k)])),
      avatars: await ctx.access.on('avatars'),
      extensions: await ctx.extensions.hkNav((key) => ctx.staff.can(key)),
    }),
  },
];
