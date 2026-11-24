import { urls } from '../cms/lib/html.mjs';
import { intParam } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';
import { showsHousekeepingLink } from '../hk-api/_guard.mjs';
import { photoThumb } from './photos.mjs';
import { photoDir } from '../lib/photo-files.mjs';
import {
  DOOR, GRANTS, MAX_NAME, MAX_PHOTOS, ROLE, friendCount, memberGroups, ownedRooms, publishedPhoto, publishedPhotos,
  roomName, shownFriends, userByName, usernameById,
} from './profile-queries.mjs';

export const owner = 'profile';
export const grants = GRANTS;

const LOOKUP = ['pub-lookup', 120, 60];
const lookupOk = (ctx, res) => ctx.rateLimit(res, ...LOOKUP);

const on = (v) => String(v ?? '0') === '1';
const nameOk = (name) => typeof name === 'string' && name.length > 0 && name.length <= MAX_NAME
  && !/[\u{10000}-\u{10FFFF}]/u.test(name);
const noSuchUser = (ctx, res, name) => ctx.send.notFound(res, `There is no one called ${name} in ${ctx.GAME_NAME}.`);

const viewerId = async (ctx) => Number((await ctx.session())?.userId ?? 0) || 0;

export const visibility = (user, viewerIsOwner = false, viewerIsStaff = false) => ({
  hidden: on(user.hide_profile) && !viewerIsOwner && !viewerIsStaff,
  showOnline: !on(user.hide_online) || viewerIsOwner || viewerIsStaff,
});
const privateProfile = (ctx, res) => ctx.send.error(res, 404, 'Private profile',
  'This profile is private: only its owner and staff can see it.', { reason: 'private' });

const viewerOf = async (ctx, user) => {
  const owner = (await viewerId(ctx)) === Number(user.id);
  return { owner, staff: !owner && on(user.hide_profile) && await showsHousekeepingLink(ctx) };
};

export const presence = (user, vis) => {
  if (vis.hidden || !vis.showOnline) return null;
  if (on(user.online)) return 'online';
  return Number(user.last_online) > 0 ? 'offline' : null;
};

const loadUser = async (ctx, res) => {
  const name = ctx.params.name;
  if (!nameOk(name)) { noSuchUser(ctx, res, name.slice(0, MAX_NAME)); return null; }
  const user = await userByName(ctx.db, name);
  if (!user) { noSuchUser(ctx, res, name); return null; }
  return user;
};

export const profileAnswer = (ctx, { user, viewerIsOwner, viewerIsStaff = false, groups = [], rooms = [], photos = [], friends = 0, friendList = [] }) => {
  const vis = visibility(user, viewerIsOwner, viewerIsStaff);
  const p = presence(user, vis);
  const base = {
    ok: true,
    user: {
      id: Number(user.id), name: String(user.username), figure: String(user.figure ?? ''), gender: String(user.gender ?? 'M'),
      motto: String(user.motto ?? ''), memberSince: Number(user.account_created) || 0,
      online: p === 'online', lastOnline: p === 'offline' ? Number(user.last_online) : null,
    },
    own: viewerIsOwner,
    private: on(user.hide_profile),
  };
  return {
    ...base,
    friends,
    friendList: [...friendList].map((f) => ({
      id: Number(f.id), name: String(f.username), figure: String(f.figure ?? ''), gender: String(f.gender ?? 'M'),
      online: on(f.on_now) || f.on_now === 1 || f.on_now === true,
    })),
    groups: [...groups].map((g) => ({
      id: Number(g.id), name: String(g.name ?? ''), role: Number(g.owner_id) === Number(user.id) ? 'Owner' : ROLE[Number(g.level)] ?? 'Member',
    })),
    rooms: [...rooms].map((r) => ({
      id: Number(r.id), name: String(r.name ?? ''), users: Number(r.player_count) || 0, max: Number(r.max_players) || 0,
      door: DOOR[r.door_mode] ?? 'open',
    })),
    photos: [...photos].map((ph) => ({ id: String(ph.id), published: Number(ph.published_at) || 0, thumb: photoThumb(ctx, ph.id) })),
    morePhotos: photos.length >= MAX_PHOTOS,
  };
};

export const routes = [
  {
    method: 'GET', path: '/rd/:id(\\d{1,10})', gate: 'players',
    handler: async (req, res, ctx) => {
      if (!lookupOk(ctx, res)) return;
      const id = intParam(ctx.params.id);
      const name = id ? await usernameById(ctx.db, id) : null;
      if (!name) return ctx.send.notFound(res, 'There is no one with that id.');
      return ctx.send.redirect(res, urls.profile(name));
    },
  },
  {
    method: 'GET', path: '/profile',
    handler: async (req, res, ctx) => ctx.send.redirect(res, urls.me()),
  },
  {
    method: 'GET', path: '/home/:name', gate: 'players',
    handler: async (req, res, ctx) => {
      if (!lookupOk(ctx, res)) return;
      if (!nameOk(ctx.params.name)) return noSuchUser(ctx, res, ctx.params.name.slice(0, MAX_NAME));
      return ctx.send.redirect(res, urls.profile(ctx.params.name));
    },
  },
  {
    method: 'GET', path: `${SITE_API}/profiles/:name`, gate: 'players',
    handler: async (req, res, ctx) => {
      if (!lookupOk(ctx, res)) return;
      const user = await loadUser(ctx, res);
      if (!user) return;
      const viewer = await viewerOf(ctx, user);
      if (visibility(user, viewer.owner, viewer.staff).hidden) return privateProfile(ctx, res);
      const [groups, rooms, photos, friends, friendList] = await Promise.all([
        memberGroups(ctx.db, user.id), ownedRooms(ctx.db, user.id), publishedPhotos(ctx.db, user.id), friendCount(ctx.db, user.id),
        shownFriends(ctx.db, user.id),
      ]);
      return ctx.send.json(res, 200, profileAnswer(ctx, {
        user, viewerIsOwner: viewer.owner, viewerIsStaff: viewer.staff, groups, rooms, photos, friends, friendList,
      }));
    },
  },
  {
    method: 'GET', path: `${SITE_API}/profiles/:name/photos/:photoId([0-9a-f-]{36})`, gate: 'photos',
    handler: async (req, res, ctx) => {
      if (!lookupOk(ctx, res)) return;
      const { photoId } = ctx.params;
      const user = await loadUser(ctx, res);
      if (!user) return;
      const viewer = await viewerOf(ctx, user);
      const photo = visibility(user, viewer.owner, viewer.staff).hidden ? null : await publishedPhoto(ctx.db, user.id, photoId);
      if (!photo) return ctx.send.notFound(res, 'This photo does not exist, or it is not public.');
      const room = Number(photo.room_id) > 0 ? await roomName(ctx.db, Number(photo.room_id)) : null;
      const hasImage = ctx.userContent?.exists(`${photoDir()}/${photo.id}.png`) ?? true;
      return ctx.send.json(res, 200, {
        ok: true,
        user: { id: Number(user.id), name: String(user.username) },
        photo: {
          id: String(photo.id), published: Number(photo.published_at) || 0,
          image: hasImage ? urls.photoImage(photo.id) : null,
        },
        room: room ? { id: Number(room.id), name: String(room.name ?? '') } : null,
      });
    },
  },
];
