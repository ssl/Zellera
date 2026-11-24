import { relative, sep } from 'node:path';
import { isFile, safeJoin, sendFile, sendText } from './files.mjs';
import { ipLimitKey, isLoopbackIp } from './limits.mjs';
import { DEFAULT_ACCESS } from './site-access.mjs';

export const MISS_LIMIT = Object.freeze([600, 60]);
const PREVIEW_DIR = 'servercamera/preview/';
const PREVIEW = /^servercamera\/preview\/[a-z0-9]{1,16}\/(\d{1,10})-\d{1,16}\.png$/;
const PHOTOS_DIR = 'servercamera/photos/';
const THUMBNAIL_DIR = 'navigator-thumbnail/';
const THUMBNAIL = /^navigator-thumbnail\/[a-z0-9]{1,16}\/\d{1,10}\.png$/;
const FULL_ROOM = /^fullroom-photo\/([a-z0-9]{1,16})\/(\d{1,10})\.png$/;

const userContentPath = (rel) => {
  const full = FULL_ROOM.exec(rel);
  return full ? `navigator-thumbnail/${full[1]}/${full[2]}.png` : rel;
};

export const createUserContent = ({ dir, currentUser, limiter, access = DEFAULT_ACCESS }) => async (req, res, rel, ip) => {
  const missing = () => {
    if (limiter && !isLoopbackIp(ip)) {
      const hit = limiter.hit(`usercontent:${ipLimitKey(ip)}`, ...MISS_LIMIT);
      if (!hit.ok) {
        return sendText(res, 429, 'too many requests, try again soon', 'text/plain; charset=utf-8',
          { 'retry-after': String(hit.retryAfter), 'cache-control': 'no-store' });
      }
    }
    // The game asks for every room's thumbnail and keeps its own picture when none loads; an empty answer says "none"
    // without a failed request for each room.
    if (THUMBNAIL.test(String(rel))) {
      res.writeHead(204, { 'cache-control': 'no-cache' });
      return res.end();
    }
    return sendText(res, 404, 'not found');
  };
  const login = () => Promise.resolve().then(() => currentUser(req)).catch(() => null);
  const file = safeJoin(dir, userContentPath(String(rel)));
  if (!file || !isFile(file)) return missing();
  const inside = relative(dir, file).split(sep).join('/');
  if (inside.toLowerCase().startsWith(PREVIEW_DIR)) {
    const owner = PREVIEW.exec(inside);
    const me = owner ? await login() : null;
    if (!me || Number(me.user?.id) !== Number(owner[1])) return missing();
    return sendFile(req, res, file, { 'cache-control': 'private, no-store', 'x-content-type-options': 'nosniff' });
  }
  const open = await access.on(inside.toLowerCase().startsWith(PHOTOS_DIR) ? 'photos' : 'players');
  if (!open && !(await login())) return missing();
  const scope = open ? 'public' : 'private';
  if (inside.toLowerCase().startsWith(THUMBNAIL_DIR)) {
    return sendFile(req, res, file, { 'cache-control': `${scope}, no-cache`, 'x-content-type-options': 'nosniff' }, { revalidate: true });
  }
  return sendFile(req, res, file, { 'cache-control': `${scope}, max-age=3600`, 'x-content-type-options': 'nosniff' });
};
