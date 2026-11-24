import { intParam, sql } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';
import { PHOTO_ID, photoDir } from '../lib/photo-files.mjs';

export const owner = 'photos';

export const grants = [
  { table: 'camera_photos', select: ['id', 'player_id', 'room_id', 'published', 'published_at'] },
  { table: 'players', select: ['id', 'name'] },
  { table: 'player_preferences', select: ['player_id', 'hide_profile'] },
  { table: 'rooms', select: ['id', 'name', 'door_mode'] },
];

export const PER_PAGE = 24;
export const MAX_PAGE = 1000;
const MAX_NAME = 50;
const LOOKUP = ['pub-photos', 120, 60];

const nameOk = (name) => name.length > 0 && name.length <= MAX_NAME && !/[\u{10000}-\u{10FFFF}]/u.test(name);

const visible = (viewerId, userId) => sql`p.published = 1
  AND (COALESCE(s.hide_profile, FALSE) = FALSE OR u.id = ${viewerId})${userId == null ? sql`` : sql` AND p.player_id = ${userId}`}`;

const countPhotos = async (db, { viewerId = 0, userId = null } = {}) => {
  const [r] = await db.query(sql`SELECT COUNT(*) AS n FROM camera_photos p
    JOIN players u ON u.id = p.player_id
    LEFT JOIN player_preferences s ON s.player_id = u.id
    WHERE ${visible(viewerId, userId)}`);
  return Number(r?.n ?? 0);
};

const listPhotos = (db, { viewerId = 0, userId = null, limit = PER_PAGE, offset = 0 } = {}) => db.query(sql`SELECT p.id, p.published_at, u.name AS username, r.id AS room_id, r.name AS room_name
  FROM camera_photos p
  JOIN players u ON u.id = p.player_id
  LEFT JOIN player_preferences s ON s.player_id = u.id
  LEFT JOIN rooms r ON r.id = p.room_id AND r.door_mode <> 'invisible'
  WHERE ${visible(viewerId, userId)}
  ORDER BY p.published_at DESC, p.id ASC
  LIMIT ${limit} OFFSET ${offset}`);

const userByName = async (db, name) => {
  const [u] = await db.query(sql`SELECT u.id, u.name AS username, COALESCE(s.hide_profile, FALSE) AS hide_profile
    FROM players u LEFT JOIN player_preferences s ON s.player_id = u.id
    WHERE u.name = ${name} LIMIT 1`);
  return u ?? null;
};

export const photoThumb = (ctx, id) => {
  if (!PHOTO_ID.test(String(id))) return null;
  if (ctx.userContent?.exists(`${photoDir()}/${id}_small.png`)) return `/usercontent/${photoDir()}/${id}_small.png`;
  return ctx.userContent?.exists(`${photoDir()}/${id}.png`) ? `/usercontent/${photoDir()}/${id}.png` : null;
};

export const photoRow = (ctx, p) => ({
  id: String(p.id),
  owner: String(p.username),
  published: Number(p.published_at) || 0,
  room: p.room_id != null ? { id: Number(p.room_id), name: String(p.room_name ?? '') } : null,
  thumb: photoThumb(ctx, p.id),
});

const noSuchPage = (ctx, res) => ctx.send.notFound(res, 'There is no such page of photos.');

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/photos`, gate: 'photos',
    handler: async (req, res, ctx) => {
      if (!ctx.rateLimit(res, ...LOOKUP)) return;
      const rawPage = ctx.query.get('page');
      const pageNo = rawPage == null ? 1 : intParam(rawPage, { max: MAX_PAGE });
      if (pageNo == null) return noSuchPage(ctx, res);
      const viewerId = Number((await ctx.session())?.userId ?? 0) || 0;

      let user = null;
      const name = String(ctx.query.get('user') ?? '');
      if (name !== '') {
        user = nameOk(name) ? await userByName(ctx.db, name) : null;
        if (!user) return ctx.send.notFound(res, `There is no one called ${name.slice(0, MAX_NAME)} in ${ctx.GAME_NAME}.`);
        if (String(user.hide_profile) === '1' && Number(user.id) !== viewerId) {
          return ctx.send.json(res, 200, {
            ok: true, user: { name: String(user.username), private: true }, photos: [], total: 0, page: pageNo, hasNext: false,
          });
        }
      }

      const userId = user ? Number(user.id) : null;
      const total = await countPhotos(ctx.db, { viewerId, userId });
      const photos = (pageNo - 1) * PER_PAGE < total
        ? await listPhotos(ctx.db, { viewerId, userId, limit: PER_PAGE, offset: (pageNo - 1) * PER_PAGE })
        : [];
      if (pageNo > 1 && !photos.length) return noSuchPage(ctx, res);
      return ctx.send.json(res, 200, {
        ok: true,
        user: user ? { name: String(user.username), private: false } : null,
        photos: [...photos].map((p) => photoRow(ctx, p)),
        total,
        page: pageNo,
        hasNext: pageNo * PER_PAGE < total,
      });
    },
  },
];
