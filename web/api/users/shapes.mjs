import { colonTime, userTime } from '../lib/dates.mjs';
import { groupUid, roomUid, userUid } from '../lib/ids.mjs';
import { badgeDescription, badgeName } from '../../lib/badges.mjs';

const on = (v) => String(v ?? '0') === '1';

export const LEVEL_STARTS = [0, 30, 35, 45, 60, 75, 95, 115, 140, 165, 190, 215, 240, 270, 300, 340, 390, 450, 520, 600, 700, 800, 900, 1100, 1300, 1600, 2000, 2500, 3200, 4000];

export const levelPoints = (codes) => {
  const best = new Map();
  for (const code of codes) {
    const m = /^ACH_(.*?)(\d+)$/.exec(code ?? '');
    if (!m || m[2].length > 4) continue;
    best.set(m[1], Math.max(best.get(m[1]) ?? 0, Number(m[2])));
  }
  let sum = 0;
  for (const level of best.values()) sum += level;
  return sum;
};

export const profileLevel = (points, reached = 1) => {
  const p = Math.max(0, Number(points) || 0);
  const t = LEVEL_STARTS;
  let level = 1;
  while (level < t.length && p >= t[level]) level++;
  if (reached > level) return { level: reached, percent: 0 };
  if (level >= t.length) return { level, percent: 0 };
  return { level, percent: Math.floor((100 * (p - t[level - 1])) / (t[level] - t[level - 1])) };
};

const selectedBadge = (row, texts, limitOf) => ({
  badgeIndex: Number(row.slot_id),
  code: row.badge_code,
  name: badgeName(texts, row.badge_code),
  description: badgeDescription(texts, row.badge_code, limitOf),
});

export const badge = (code, texts, limitOf) => ({
  code,
  name: badgeName(texts, code),
  description: badgeDescription(texts, code, limitOf),
});

export const sortBadges = (list) => list.sort((a, b) => (a.name < b.name ? -1 : a.name > b.name ? 1 : a.code < b.code ? -1 : a.code > b.code ? 1 : 0));

export const user = (row, worn, texts, limitOf, points, reached = 1) => {
  const { level, percent } = profileLevel(points, reached);
  const hideOnline = on(row.hide_online);
  return {
    uniqueId: userUid(row.id),
    name: row.username,
    figureString: row.look ?? '',
    motto: row.motto ?? '',
    online: on(row.online) && !hideOnline,
    lastAccessTime: hideOnline ? null : userTime(row.last_online),
    memberSince: userTime(row.account_created),
    profileVisible: true,
    currentLevel: level,
    currentLevelCompletePercent: percent,
    totalExperience: Math.max(0, Number(points) || 0),
    starGemCount: Number(row.respects_received) || 0,
    selectedBadges: worn.map((b) => selectedBadge(b, texts, limitOf)),
  };
};

export const hiddenUser = (row) => ({
  uniqueId: userUid(row.id),
  name: row.username,
  figureString: row.look ?? '',
  motto: row.motto ?? '',
  memberSince: null,
  profileVisible: false,
  selectedBadges: [],
});

export const isHidden = (row) => on(row.hide_profile);

export const friend = (row) => ({
  name: row.username,
  motto: row.motto ?? '',
  online: on(row.online) && !on(row.hide_online),
  uniqueId: userUid(row.id),
  figureString: row.look ?? '',
});

const GROUP_TYPES = { 0: 'NORMAL', 1: 'EXCLUSIVE', 2: 'CLOSED' };
const colour = (v) => String(v ?? '').replace(/^#/, '').toLowerCase();

export const group = (row) => ({
  id: groupUid(row.id),
  name: row.name,
  description: row.description ?? '',
  type: GROUP_TYPES[Number(row.join_mode)] ?? 'NORMAL',
  roomId: Number(row.room_id) > 0 ? roomUid(row.room_id) : null,
  badgeCode: row.badge_code ?? '',
  primaryColour: colour(row.primary_colour),
  secondaryColour: colour(row.secondary_colour),
});

export const userGroup = (row) => ({ ...group(row), online: false, isAdmin: Number(row.level) <= 1 });

export const groupMember = (row) => ({
  gender: String(row.gender ?? 'M').toUpperCase() === 'F' ? 'f' : 'm',
  motto: row.motto ?? '',
  figure: row.look ?? '',
  memberSince: colonTime(row.account_created),
  uniqueId: userUid(row.id),
  name: row.username,
  online: on(row.online) && !on(row.hide_online),
  isAdmin: Number(row.level) <= 1,
});

const DOOR = { open: 'open', locked: 'closed', password: 'password', invisible: 'invisible' };

export const categoryKey = (caption) => {
  if (caption == null || caption === '') return 'navigator.flatcategory.global.null';
  const m = /^\$\{(.+)\}$/.exec(String(caption).trim());
  return m ? m[1] : String(caption);
};

export const roomTags = (tags) => String(tags ?? '').split(';').filter((t) => t !== '');

export const room = (row, game, full) => {
  const isPublic = on(row.is_public);
  const o = {
    id: Number(row.id),
    name: row.name,
    description: row.description ?? '',
    creationTime: colonTime(row.created_at),
  };
  if (Number(row.group_id) > 0) o.groupId = groupUid(row.group_id);
  o.tags = roomTags(row.tags);
  o.maximumVisitors = Number(row.max_players);
  o.showOwnerName = !isPublic;
  o.ownerName = row.owner_name;
  o.ownerUniqueId = userUid(row.owner_id);
  o.categories = isPublic ? [] : [categoryKey(row.caption)];
  o.thumbnailUrl = `${game.site}/usercontent/navigator-thumbnail/${game.serverId}/${row.id}.png`;
  o.imageUrl = `${game.site}/usercontent/fullroom-photo/${game.serverId}/${row.id}.png`;
  o.rating = Number(row.score);
  if (full) {
    o.publicRoom = isPublic;
    o.doorMode = DOOR[row.door_mode] ?? 'open';
  }
  o.uniqueId = roomUid(row.id);
  return o;
};

export const photo = (row, game) => {
  const url = `${game.site}/usercontent/servercamera/photos/${game.serverId}/${row.id}.png`;
  return {
    previewUrl: url,
    tags: [],
    creator_uniqueId: userUid(row.user_id),
    type: 'PHOTO',
    url,
    version: 1,
    time: Number(row.created_at),
    creator_name: row.username,
    creator_id: Number(row.user_id),
    room_id: Number(row.room_id),
    id: row.id,
    likes: [],
  };
};
