import { createHash, timingSafeEqual } from 'node:crypto';
import { error } from './http.mjs';

export const KEY_RE = /^[A-Za-z0-9_-]{43}$/;

export const hashKey = (key) => (typeof key === 'string' && KEY_RE.test(key) ? createHash('sha256').update(key, 'ascii').digest() : null);

export const grants = [
  { table: 'wired_api_keys', select: ['box_id', 'kind', 'room_id', 'key_hash'] },
  { table: 'furni', select: ['id', 'room_id'] },
  { table: 'build_pass_furni', select: ['furni_id', 'client_id'] },
];

export const KEYS_SQL = 'SELECT COALESCE(b.client_id, k.box_id) AS box_id, k.kind, k.key_hash FROM wired_api_keys k '
  + 'INNER JOIN furni i ON i.id = k.box_id AND i.room_id = k.room_id '
  + 'LEFT JOIN build_pass_furni b ON b.furni_id = k.box_id WHERE k.room_id = ?';

const HEADER = { read: 'x-wired-read-key', write: 'x-wired-write-key' };

export const checkWiredKeys = async (db, roomId, need, headers, { batch = false } = {}) => {
  const rows = await db.query(KEYS_SQL, [roomId]);
  const kinds = need === 'both' ? ['read', 'write'] : [need];
  const boxes = {};
  for (const kind of kinds) {
    const raw = headers[HEADER[kind]];
    const sent = Array.isArray(raw) ? raw[0] : raw;
    const ofKind = rows.filter((r) => r.kind === kind);
    if (ofKind.length === 0 || (!sent && batch)) return { ok: false, reply: error(403, 'wired.variables.key_missing') };
    const valid = hashKey(sent);
    const hash = valid ?? Buffer.alloc(32);
    let match = null;
    for (const r of ofKind) {
      const stored = Buffer.from(r.key_hash);
      if (stored.length === 32 && timingSafeEqual(hash, stored) && valid) match = r.box_id;
    }
    if (match === null) return { ok: false, reply: error(403, 'wired.variables.key_invalid') };
    boxes[kind] = match;
  }
  return { ok: true, boxes };
};
