import { fail, notFound, ok, validation } from '../lib/http.mjs';
import { groupIdOf, isUserIdFormat, userIdOf } from '../lib/ids.mjs';
import { texts as fileTexts } from '../lib/texts.mjs';
import { badgeDescription, badgeName } from '../../lib/badges.mjs';
import { createCatalogueCache, levelOf } from '../users/achievements.mjs';
import * as q from '../users/queries.mjs';
import * as shape from '../users/shapes.mjs';

export const grants = q.GRANTS;

const MAX_NAME = 50;
const MAX_BADGE_CODE = 32;

export const createUsersHandlers = ({ texts = fileTexts, catalogue = createCatalogueCache() } = {}) => {
  const userOfPath = async (ctx) => {
    const id = ctx.params.id;
    if (!isUserIdFormat(id)) fail(validation('id', 'user.invalid_id', id));
    const dbId = userIdOf(id);
    const row = dbId == null ? null : await q.userById(ctx.db, dbId);
    if (!row) fail(notFound());
    return row;
  };
  const visibleUserOfPath = async (ctx) => {
    const row = await userOfPath(ctx);
    if (shape.isHidden(row)) fail(notFound());
    return row;
  };

  const userObject = async (ctx, row) => {
    if (shape.isHidden(row)) return shape.hiddenUser(row);
    const [worn, all, stored, cat] = await Promise.all([q.wornBadges(ctx.db, row.id), q.allBadges(ctx.db, row.id), q.accountLevel(ctx.db, row.id), catalogue(ctx.db)]);
    return shape.user(row, worn, texts, cat.limitOf, stored?.points ?? shape.levelPoints(all.map((b) => b.badge_code)), stored?.level);
  };
  const friendList = async (ctx, row) => (await q.friends(ctx.db, row.id)).map(shape.friend);
  const groupList = async (ctx, row) => (await q.userGroups(ctx.db, row.id)).map(shape.userGroup);
  const roomList = async (ctx, row) => (await q.userRooms(ctx.db, row.id)).map((r) => shape.room(r, ctx.game, false));
  const badgeList = async (ctx, row) => {
    const [rows, cat] = await Promise.all([q.allBadges(ctx.db, row.id), catalogue(ctx.db)]);
    return shape.sortBadges(rows.map((b) => shape.badge(b.badge_code, texts, cat.limitOf)));
  };
  const photoList = (ctx, rows) => rows.map((p) => shape.photo(p, ctx.game));

  return {
    async getUserByName(ctx) {
      const name = ctx.query.get('name');
      if (name === null) return validation('name', 'user.invalid_name');
      if (name === '') return validation('name', 'user.invalid_name', '');
      if (name.length > MAX_NAME || !q.canBeUtf8mb3(name)) return notFound();
      const row = await q.userByName(ctx.db, name);
      return row ? ok(await userObject(ctx, row)) : notFound();
    },

    async getUser(ctx) {
      return ok(await userObject(ctx, await userOfPath(ctx)));
    },

    async getUserFriends(ctx) {
      return ok(await friendList(ctx, await visibleUserOfPath(ctx)));
    },

    async getUserGroups(ctx) {
      return ok(await groupList(ctx, await visibleUserOfPath(ctx)));
    },

    async getUserRooms(ctx) {
      return ok(await roomList(ctx, await visibleUserOfPath(ctx)));
    },

    async getUserBadges(ctx) {
      return ok(await badgeList(ctx, await visibleUserOfPath(ctx)));
    },

    async getUserProfile(ctx) {
      const row = await visibleUserOfPath(ctx);
      const [user, groups, badges, friends, rooms] = await Promise.all([
        userObject(ctx, row), groupList(ctx, row), badgeList(ctx, row), friendList(ctx, row), roomList(ctx, row),
      ]);
      return ok({ user, groups, badges, friends, rooms });
    },

    async listAchievements(ctx) {
      const cat = await catalogue(ctx.db);
      return ok(cat.list.map((e) => ({ achievement: e.achievement, levelRequirements: e.levels })));
    },

    async getUserAchievements(ctx) {
      const row = await userOfPath(ctx);
      const [progress, cat] = await Promise.all([q.userAchievements(ctx.db, row.id), catalogue(ctx.db)]);
      const out = [];
      for (const p of progress) {
        const entry = cat.byName.get(String(p.achievement_name).replace(/^ACH_/, ''));
        if (!entry) continue;
        const score = Number(p.progress) || 0;
        out.push({ achievement: entry.achievement, level: levelOf(entry.levels, score), score });
      }
      out.sort((a, b) => a.achievement.id - b.achievement.id);
      return ok(out);
    },

    async getBadgeOwners(ctx) {
      const code = ctx.params.badgeCode;
      const count = code.length <= MAX_BADGE_CODE && q.canBeUtf8mb3(code) ? await q.badgeOwnerCount(ctx.db, code) : 0;
      const cat = await catalogue(ctx.db);
      return ok({ ownerCount: count, name: badgeName(texts, code, code), description: badgeDescription(texts, code, cat.limitOf) });
    },

    async getGroup(ctx) {
      const id = groupIdOf(ctx.params.id);
      const row = id == null ? null : await q.groupById(ctx.db, id);
      return row ? ok(shape.group(row)) : notFound();
    },

    async listGroupMembers(ctx) {
      const id = groupIdOf(ctx.params.id);
      const row = id == null ? null : await q.groupById(ctx.db, id);
      if (!row) return notFound();
      return ok((await q.groupMembers(ctx.db, row.id)).map(shape.groupMember));
    },

    async getRoom(ctx) {
      const raw = ctx.params.roomId;
      if (!/^\d{1,10}$/.test(raw) || Number(raw) > 2147483647 || Number(raw) <= 0) return notFound();
      const row = await q.roomById(ctx.db, Number(raw));
      return row ? ok(shape.room(row, ctx.game, true)) : notFound();
    },

    async listUserPhotos(ctx) {
      const row = await visibleUserOfPath(ctx);
      return ok(photoList(ctx, await q.userPhotos(ctx.db, row.id)));
    },

    async listPublicPhotos(ctx) {
      return ok(photoList(ctx, await q.publicPhotos(ctx.db)));
    },
  };
};

export const handlers = createUsersHandlers();
