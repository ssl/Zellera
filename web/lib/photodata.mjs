import { userUid } from '../api/lib/ids.mjs';

export { PHOTO_ID } from './photo-files.mjs';

const uid = (id) => {
  try {
    return userUid(id);
  } catch {
    return '';
  }
};

export const photoData = (id, row, site, serverId) => {
  const url = `${site}/usercontent/servercamera/photos/${serverId}/${id}.png`;
  if (!row) return { id, url };
  return {
    room_id: Number(row.room_id),
    creator_id: Number(row.user_id),
    creator_name: row.username,
    time: Number(row.created_at),
    version: 1,
    url,
    type: 'PHOTO',
    creator_uniqueId: uid(row.user_id),
    tags: [],
    previewUrl: url,
    id,
  };
};

export const photoRows = async (query, ids) => {
  const rows = new Map();
  if (ids.length === 0) return rows;
  try {
    const found = await query(`SELECT p.id, p.player_id AS user_id, p.room_id, p.created_at_ms AS created_at, u.name AS username
      FROM camera_photos p JOIN players u ON u.id = p.player_id WHERE p.id IN (?)`, [ids]);
    for (const r of found) rows.set(r.id, r);
  } catch (e) {
    console.error(`web: the photos' rows were not read: ${e?.message ?? e}`);
  }
  return rows;
};
