import { lookGender, lookHash } from '../api/handlers/rest.mjs';
import { accountRulesOf, placeOf } from '../lib/site-access.mjs';
import { BETA_CODE, BETA_MODE } from '../cms/config.mjs';
import { sql } from '../cms/lib/sql.mjs';
import { showsHousekeepingLink } from '../hk-api/_guard.mjs';
import { SITE_API } from './index.mjs';

export const owner = 'me';
export const grants = [
  { table: 'players', select: ['id', 'name', 'figure', 'gender'] },
  { table: 'featured_looks', select: ['gender', 'figure', 'enabled'] },
];

const FRONT_LOOKS = 6;

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/site`,
    handler: async (req, res, ctx) => {
      const s = await ctx.session();
      const [u] = s && Number(s.userId) > 0
        ? await ctx.db.query(sql`SELECT id, name, figure, gender FROM players WHERE id = ${Number(s.userId)} LIMIT 1`)
        : [];
      const end = u ? null : await ctx.sessionEnd();
      const [place, avatars, rules, account] = await Promise.all([
        placeOf(ctx.access), ctx.access.on('avatars'), accountRulesOf(ctx.access), u ? null : ctx.account(),
      ]);
      return ctx.send.json(res, 200, {
        ok: true,
        name: ctx.GAME_NAME,
        names: { ...ctx.NAMES },
        place,
        avatars: avatars && ctx.viewer.sees.players,
        register: { betaCode: BETA_MODE, open: !BETA_MODE || BETA_CODE !== '' },
        guests: !BETA_MODE && await ctx.access.on('guests'),
        accounts: { email: rules.email, avatarsPerAccount: rules.avatars },
        choosingAvatar: !u && !end && Boolean(account) && rules.avatars > 1,
        viewer: u ? { id: Number(u.id), name: String(u.name), figure: String(u.figure ?? ''), gender: String(u.gender ?? 'M') } : null,
        sees: ctx.viewer.sees,
        housekeeping: u ? await showsHousekeepingLink(ctx) : false,
        banNotice: end?.notice ?? null,
      }, end ? { 'set-cookie': end.cookie } : {});
    },
  },
  {
    method: 'GET', path: `${SITE_API}/front`,
    handler: async (req, res, ctx) => {
      let rows = [];
      try {
        rows = await ctx.db.query(sql`SELECT gender, figure FROM featured_looks
          WHERE enabled = TRUE AND figure REGEXP '^[a-z0-9.-]{1,256}$' ORDER BY RAND() LIMIT ${FRONT_LOOKS}`);
      } catch (e) {
        ctx.log(`featured looks: ${e?.message ?? e}`);
      }
      const looks = [...rows].map((r) => ({ image: `/imaging/avatar/${lookHash(lookGender(r.gender), r.figure)}.png` }));
      return ctx.send.json(res, 200, { ok: true, looks });
    },
  },
];
