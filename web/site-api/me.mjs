import { excerpt } from '../cms/lib/excerpt.mjs';
import { loginFirst, signedIn } from '../cms/lib/session.mjs';
import { SITE_API } from './index.mjs';
import { showsHousekeepingLink } from '../hk-api/_guard.mjs';
import { checkTyped } from './me-motto.mjs';
import { titleOf } from './news.mjs';
import {
  GRANTS, SEARCH_ROWS, friends, latestNews, memberships, mottoRules, myGroups, myRooms, ownUser, purse, searchUsers,
} from './me-queries.mjs';
import { DOOR, ROLE } from './profile-queries.mjs';

export const owner = 'me';
export const grants = GRANTS;

const MOTTO_LIMIT = ['me-motto', 10, 60];
const SEARCH_LIMIT = ['me-search', 30, 60];

const MESSAGES = {
  length: (m, max) => `A motto is at most ${max} characters; this one has ${m.length}.`,
  line: () => 'A motto is one line of text.',
  symbols: () => 'Emoji and other special symbols cannot go in a motto.',
  game: (m, max, reason) => `The game did not save that motto: ${reason || 'it was refused.'}`,
  notSaved: () => 'Your motto could not be saved right now, so it was not changed. Try again in a minute.',
};
const MOTTO_WAIT_MS = 2000;

const on = (v) => String(v ?? '0') === '1' || v === 1 || v === true;

export const player = (u) => ({ id: Number(u.id), name: String(u.username), figure: String(u.figure ?? ''), gender: String(u.gender ?? 'M') });

const mePage = async (ctx, userId) => {
  const user = await ownUser(ctx.db, userId);
  if (!user) return null;
  const [wallet, clubs, rooms, groups, news, friendList, { max }] = await Promise.all([
    purse(ctx.db, user.id, ctx.texts), memberships(ctx.db, user.id), myRooms(ctx.db, user.id), myGroups(ctx.db, user.id),
    latestNews(ctx.db), friends(ctx.db, user.id), mottoRules(ctx.db),
  ]);
  return {
    ok: true,
    user: {
      ...player(user), motto: String(user.motto ?? ''), online: on(user.online), lastOnline: Number(user.last_online) || 0,
      memberSince: Number(user.member_since) || 0,
    },
    purse: { credits: Number(user.credits) || 0, ...wallet },
    memberships: clubs,
    rooms: {
      total: rooms.total,
      rows: rooms.rows.map((r) => ({
        id: Number(r.id), name: String(r.name), users: Number(r.player_count) || 0, max: Number(r.max_players) || 0, door: DOOR[r.door_mode] ?? 'open',
      })),
    },
    groups: {
      total: groups.total,
      rows: groups.rows.map((g) => ({
        id: Number(g.id), name: String(g.name ?? ''), role: Number(g.owner_id) === Number(user.id) ? 'Owner' : ROLE[Number(g.level)] ?? 'Member',
      })),
    },
    news: [...news].map((n) => ({ id: Number(n.id), title: titleOf(n), excerpt: excerpt(n.body, 120), created: Number(n.created) || 0 })),
    friends: {
      total: friendList.total,
      rows: friendList.rows.map((f) => ({ ...player(f), online: on(f.on_now) })),
    },
    motto: { max },
  };
};

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/me`,
    handler: async (req, res, ctx) => {
      const s = await signedIn(ctx);
      const body = s && await mePage(ctx, Number(s.userId));
      if (!body) return loginFirst(ctx, res);
      return ctx.send.json(res, 200, body);
    },
  },
  {
    method: 'POST', path: `${SITE_API}/me/motto`, write: true, maxBody: 2048,
    handler: async (req, res, ctx) => {
      const s = await signedIn(ctx);
      if (!s) return loginFirst(ctx, res);
      if (!ctx.rateLimit(res, ...MOTTO_LIMIT)) return;
      if (typeof ctx.body.motto !== 'string') return ctx.send.error(res, 400, 'Bad request', 'The request had no motto in it.', { field: 'motto' });
      const rules = await mottoRules(ctx.db);
      const { motto, error } = checkTyped(ctx.body.motto, rules.max);
      const refuse = (status, key, reason = '') => ctx.send.error(res, status, 'Motto not saved', MESSAGES[key](motto, rules.max, reason), { field: 'motto', reason: key });
      if (error) return refuse(400, error);
      const user = await ownUser(ctx.db, Number(s.userId));
      if (!user) return loginFirst(ctx, res);
      if (motto === String(user.motto ?? '')) return ctx.send.json(res, 200, { ok: true, state: 'saved', motto });

      let row;
      try {
        const id = await ctx.command('setMotto', { motto });
        [row] = await ctx.waitForCommands([id], { userId: user.id, timeoutMs: MOTTO_WAIT_MS });
      } catch (e) {
        ctx.log(`motto for user ${user.id} not sent: ${e?.message ?? e}`);
        return refuse(503, 'notSaved');
      }
      if (!row || row.status === 'pending') return ctx.send.json(res, 202, { ok: true, state: 'pending', motto });
      if (row.status !== 'done') return refuse(422, 'game', String(row.result ?? ''));
      const stored = await ownUser(ctx.db, user.id);
      const saved = String(stored?.motto ?? '');
      return ctx.send.json(res, 200, { ok: true, state: saved === motto ? 'saved' : 'filtered', motto: saved });
    },
  },
  {
    method: 'GET', path: `${SITE_API}/players`,
    handler: async (req, res, ctx) => {
      const s = await signedIn(ctx);
      if (!s) return loginFirst(ctx, res);
      if (!ctx.rateLimit(res, ...SEARCH_LIMIT)) return;
      const q = String(ctx.query.get('name') ?? '').trim().slice(0, 64);
      const found = q ? await searchUsers(ctx.db, q, Number(s.userId), await showsHousekeepingLink(ctx)) : [];
      return ctx.send.json(res, 200, {
        ok: true,
        q,
        rows: found.slice(0, SEARCH_ROWS).map((r) => ({ ...player(r), motto: String(r.motto ?? ''), online: on(r.on_now) })),
        more: found.length > SEARCH_ROWS,
      });
    },
  },
];
