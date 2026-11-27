import { sql, unixNow } from '../cms/lib/sql.mjs';

export const PIECES = Object.freeze(['id', 'gpu', 'scr', 'cpu', 'tz', 'fnt', 'cnv', 'aud', 'gl', 'ua']);
export const COMBO = Object.freeze(['gpu', 'scr', 'fnt']);
export const MIN_WITHOUT_ID = 3;

const READING = /^1-[0-9a-f]{16}(?:-[0-9a-f]{8}){9}$/;
const D_COLUMNS = PIECES.map((p) => `d.${p}`);
const B_COLUMNS = PIECES.map((p) => `b.p_${p}`);
const d = (name) => sql.ident(`d.${name}`, D_COLUMNS);
const b = (name) => sql.ident(`b.p_${name}`, B_COLUMNS);

export const usable = (piece) => /[^0]/.test(String(piece ?? '')) && /[^f]/.test(String(piece ?? ''));

export const parseReading = (text) => {
  const s = String(text ?? '');
  if (!READING.test(s)) return null;
  const values = s.split('-').slice(1);
  return Object.fromEntries(PIECES.map((name, i) => [name, values[i]]));
};
export const readingOf = (row) => `1-${PIECES.map((name) => String(row[name])).join('-')}`;

export const deviceOf = (row) => ({
  reading: readingOf(row),
  pieces: PIECES.map((name) => ({ name, value: String(row[name]), usable: usable(row[name]) })),
  firstSeen: Number(row.first_seen_at) || 0, lastSeen: Number(row.last_seen_at) || 0, logins: Number(row.logins) || 0,
});

export const piecesEqual = (values, names) => sql.join(names.map((name) => sql`${d(name)} = ${String(values[name])}`), ' AND ');
export const anyOrEqual = (names) => sql.join(names.map((name) => sql`(${b(name)} IS NULL OR ${d(name)} = ${b(name)})`), ' AND ');
export const patternEqual = (names) => sql.join(names.map((name) => sql`${d(name)} = ${b(name)}`), ' AND ');
export const patternHas = (names) => sql.join(names.map((name) => sql`${b(name)} IS NOT NULL`), ' AND ');

export const pieceNames = (value) => {
  const asked = String(value ?? '').split(',').map((s) => s.trim()).filter(Boolean);
  if (asked.some((name) => !PIECES.includes(name))) return null;
  return PIECES.filter((name) => asked.includes(name));
};

export const patternRefusal = (device, names) => {
  if (!names || names.length === 0) return 'Tick at least one piece.';
  const bad = names.filter((name) => !usable(device[name]));
  if (bad.length) return `Not usable on this device: ${bad.join(', ')}. A pattern holds usable pieces only.`;
  if (!names.includes('id') && names.length < MIN_WITHOUT_ID) {
    return `Without the id a pattern needs at least ${MIN_WITHOUT_ID} pieces: fewer would match far too many devices.`;
  }
  return null;
};

export const patternText = (pattern) => PIECES.filter((name) => pattern[name] != null).map((name) => `${name}=${pattern[name]}`).join(', ');

export const loadUser = async (ctx, id) => {
  if (id == null) return null;
  const [u] = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.figure, u.gender, u.rank_id, r.name AS rank_name
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id WHERE u.id = ${id} LIMIT 1`);
  return u ?? null;
};
export const userNotFound = (ctx, res) => ctx.send.notFound(res, 'There is no user with this id.');

const DAY_SECONDS = 86400;
const banRules = async (ctx) => {
  const rows = await ctx.db.query(sql`SELECT name, value FROM settings
    WHERE name IN (${sql.in(['login.device.ban.days', 'login.device.ban.others'])})`);
  const setting = (name, fallback) => {
    const v = Number([...rows].find((r) => r.name === name)?.value);
    return Number.isInteger(v) ? v : fallback;
  };
  return { window: setting('login.device.ban.days', 30) * DAY_SECONDS, others: setting('login.device.ban.others', 0) };
};

const idsOf = (devs) => [...new Set(devs.map((r) => String(r.id)).filter(usable))];
const combosOf = (devs) => [...new Map(devs.filter((r) => COMBO.every((name) => usable(r[name])))
  .map((r) => [COMBO.map((name) => String(r[name])).join('-'), r])).values()];
const devicesOf = (ctx, id, since) => ctx.db.query(sql`SELECT d.id, d.gpu, d.scr, d.fnt FROM player_devices d
  WHERE d.player_id = ${id} AND d.last_seen_at >= ${since}`);

export const deviceSharerTopRank = async (ctx, playerId, now = unixNow()) => {
  const id = Number(playerId);
  const { window, others } = await banRules(ctx);
  const devs = await devicesOf(ctx, id, now - window);
  const ids = idsOf(devs);
  const tops = [];
  if (ids.length) {
    tops.push(...await ctx.db.query(sql`SELECT COALESCE(MAX(p.rank_id), 0) AS top
      FROM player_devices o JOIN players p ON p.id = o.player_id
      WHERE o.player_id <> ${id} AND o.id IN (${sql.in(ids)})
        AND NOT EXISTS (SELECT 1 FROM device_ban_exemptions e WHERE e.player_id = o.player_id)`));
  }
  for (const c of combosOf(devs)) {
    const [gpu, scr, fnt] = COMBO.map((name) => String(c[name]));
    tops.push(...await ctx.db.query(sql`SELECT COALESCE(MAX(p.rank_id), 0) AS top
      FROM player_devices o JOIN players p ON p.id = o.player_id
      WHERE o.gpu = ${gpu} AND o.scr = ${scr} AND o.fnt = ${fnt} AND o.player_id <> ${id}
        AND NOT EXISTS (SELECT 1 FROM device_ban_exemptions e WHERE e.player_id = o.player_id)
        AND (SELECT COUNT(DISTINCT q.player_id) FROM player_devices q
          WHERE q.gpu = ${gpu} AND q.scr = ${scr} AND q.fnt = ${fnt} AND q.player_id <> o.player_id AND q.player_id <> ${id}
            AND NOT EXISTS (SELECT 1 FROM bans z WHERE z.player_id = q.player_id AND z.ends_at > ${now})) <= ${others}`));
  }
  return Math.max(0, ...tops.map((r) => Number(r?.top) || 0));
};

export const loadDevice = async (ctx, userId, reading) => {
  const values = parseReading(reading);
  if (!values) return null;
  const [row] = await ctx.db.query(sql`SELECT d.id, d.gpu, d.scr, d.cpu, d.tz, d.fnt, d.cnv, d.aud, d.gl, d.ua, d.first_seen_at, d.last_seen_at, d.logins
    FROM player_devices d WHERE d.player_id = ${userId} AND ${piecesEqual(values, PIECES)} LIMIT 1`);
  return row ?? null;
};
