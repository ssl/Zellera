import { isIP } from 'node:net';
import mariadb from 'mariadb';
import { DB, DB_READONLY } from './config.mjs';
import { gameRules } from '../lib/game-rules.mjs';
import { currencyIds } from './display-names.mjs';
import { settingOn } from '../lib/production-check.mjs';
import { SECONDS_PER_DAY } from '../lib/time.mjs';
import { createWordFilter } from './word-filter.mjs';

let pool = null;
const getPool = () => {
  pool ??= mariadb.createPool({
    ...DB,
    connectionLimit: 4,
    acquireTimeout: 5000,
    connectTimeout: 5000,
    insertIdAsNumber: true,
    bigIntAsNumber: true,
    ...(DB_READONLY ? { initSql: ['SET SESSION TRANSACTION READ ONLY'] } : {}),
  });
  return pool;
};
export const closePool = async () => { await pool?.end(); pool = null; };
export const READ_ONLY = DB_READONLY;
export const query = (sql, params = []) => getPool().query(sql, params);

const SETTING_KEYS = ['site.registration.motto',
  'site.registration.home_room_id', 'site.registration.accounts_per_address', 'login.ticket.expires', 'game.name'];
const DEFAULTS = {
  'site.registration.motto': '',
  'site.registration.home_room_id': '0',
  'site.registration.accounts_per_address': '5',
  'login.ticket.expires': '1',
  'game.name': '',
};
let settingsCache = { at: 0, values: DEFAULTS };
export const settings = async () => {
  if (Date.now() - settingsCache.at < 60_000) return settingsCache.values;
  const rows = await query(
    `SELECT \`name\`, \`value\` FROM settings WHERE \`name\` IN (${SETTING_KEYS.map(() => '?').join(',')})`,
    SETTING_KEYS);
  const values = { ...DEFAULTS };
  for (const r of rows) values[r.name] = r.value;
  settingsCache = { at: Date.now(), values };
  return values;
};

const ACCOUNT_ROW = 'SELECT a.id, a.email AS mail, a.password_hash AS password FROM player_accounts a';
const NOT_REVOKED = 'NOT EXISTS (SELECT 1 FROM web_session_revocations r WHERE r.nonce = ?)';
const NOT_BANNED = 'NOT EXISTS (SELECT 1 FROM bans b WHERE b.player_id = p.id AND b.ends_at > UNIX_TIMESTAMP())';

export const sessionAccount = async (accountId, nonce) => (await query(
  `${ACCOUNT_ROW} WHERE a.id = ? AND ${NOT_REVOKED} LIMIT 1`, [accountId, nonce]))[0] ?? null;

export const accountAvatar = async (accountId, playerId) => (await query(
  `SELECT p.id, p.name AS username, p.figure AS look, p.gender, p.motto, p.account_id FROM players p
   WHERE p.id = ? AND p.account_id = ? AND ${NOT_BANNED} LIMIT 1`, [playerId, accountId]))[0] ?? null;

export const sessionBan = async (accountId, playerId, nonce) => (await query(`SELECT a.password_hash AS password, b.reason, b.ends_at AS expires_at, 1 AS own
  FROM players p JOIN player_accounts a ON a.id = p.account_id
  JOIN bans b ON b.player_id = p.id AND b.ends_at > UNIX_TIMESTAMP()
  WHERE p.id = ? AND a.id = ? AND ${NOT_REVOKED}
  ORDER BY b.ends_at DESC LIMIT 1`, [playerId, accountId, nonce]))[0] ?? null;

export const avatarsOf = async (accountId) => [...await query(`SELECT p.id, p.name, p.figure, p.gender, p.motto, p.created_at,
    COALESCE(p.last_online_at, 0) AS last_online,
    (SELECT b.reason FROM bans b WHERE b.player_id = p.id AND b.ends_at > UNIX_TIMESTAMP() ORDER BY b.ends_at DESC LIMIT 1) AS ban_reason,
    (SELECT MAX(b.ends_at) FROM bans b WHERE b.player_id = p.id AND b.ends_at > UNIX_TIMESTAMP()) AS ban_end
  FROM players p WHERE p.account_id = ? ORDER BY p.id LIMIT 100`, [accountId])];

export const avatarCount = async (accountId, run = query) => Number((await run(
  'SELECT COUNT(*) AS n FROM players WHERE account_id = ?', [accountId]))[0].n);

export const setPassword = (accountId, hash) => query(
  'UPDATE player_accounts SET password_hash = ? WHERE id = ? LIMIT 1', [hash, accountId]);

export const duplicateKey = (e) => (e?.errno === 1062 ? /for key '(?:[^']*\.)?([^'.]+)'/.exec(String(e.message))?.[1] ?? '' : null);

export const EMAIL_KEY = 'uq_player_accounts_3';

export const setEmail = (accountId, email) => query(
  'UPDATE player_accounts SET email = ?, email_verified = 0 WHERE id = ? LIMIT 1', [email, accountId]);

export const revokeSession = async (nonce, expiresAt) => {
  await query('INSERT IGNORE INTO web_session_revocations (nonce, expires_at) VALUES (?, ?)', [nonce, expiresAt]);
  await query('DELETE FROM web_session_revocations WHERE expires_at < UNIX_TIMESTAMP() LIMIT 1000');
};

export const accountByAvatarName = async (name) => (await query(
  `SELECT a.id, a.email AS mail, a.password_hash AS password, p.id AS avatarId
   FROM players p JOIN player_accounts a ON a.id = p.account_id WHERE p.name = ? LIMIT 1`, [name]))[0] ?? null;

export const accountByEmail = async (email) => (await query(
  `${ACCOUNT_ROW} WHERE a.email = ? LIMIT 1`, [email]))[0] ?? null;

export const firstAvatar = async (accountId) => Number((await query(
  `SELECT p.id FROM players p LEFT JOIN player_accounts a ON a.id = p.account_id AND a.player_id = p.id
   WHERE p.account_id = ? ORDER BY a.id IS NULL, p.id LIMIT 1`, [accountId]))[0]?.id ?? 0);

export const usernameTaken = async (username) =>
  (await query('SELECT 1 FROM players WHERE name = ? LIMIT 1', [username])).length > 0;

let nameFilter = null;
export const nameAllowed = async (name) => {
  nameFilter ??= createWordFilter(query);
  const { text, hidden } = await nameFilter.filter(name);
  return !hidden && text.toLowerCase() === String(name).toLowerCase();
};

export const emailTaken = async (email) =>
  (await query('SELECT 1 FROM player_accounts WHERE email = ? LIMIT 1', [email])).length > 0;

export const accountsForIp = async (ip, run = query) => Number((await (isIP(ip) === 6
  ? run("SELECT COUNT(*) AS n FROM player_accounts WHERE registered_ip LIKE '%:%' AND LEFT(INET6_ATON(registered_ip), 8) = LEFT(INET6_ATON(?), 8)", [ip])
  : run('SELECT COUNT(*) AS n FROM player_accounts WHERE registered_ip = ?', [ip])))[0].n);

const REGISTER_WAIT_MS = 5000;
const lockTimeout = () => Object.assign(new Error('register lock timeout'), { code: 'LOCK_TIMEOUT' });

let registerTail = Promise.resolve();
const registerTurn = async () => {
  const before = registerTail;
  let release;
  registerTail = new Promise((resolve) => { release = resolve; });
  let timer;
  const late = await Promise.race([
    before.then(() => false),
    new Promise((resolve) => { timer = setTimeout(() => resolve(true), REGISTER_WAIT_MS); }),
  ]);
  clearTimeout(timer);
  if (late) {
    before.then(release);
    throw lockTimeout();
  }
  return release;
};

export const withRegisterLock = async (fn) => {
  const release = await registerTurn();
  try {
    const conn = await getPool().getConnection();
    try {
      const [{ ok }] = await conn.query("SELECT GET_LOCK('zel_cms_register', 5) AS ok");
      if (Number(ok) !== 1) throw lockTimeout();
      try { return await fn(); } finally { await conn.query("SELECT RELEASE_LOCK('zel_cms_register')"); }
    } finally {
      conn.release();
    }
  } finally {
    release();
  }
};

const insertAvatar = async (conn, accountId, { username, look, gender, motto, credits, homeRoom }) => {
  const res = await conn.query(
    `INSERT INTO players (account_id, name, created_at, motto, figure, gender, rank_id, home_room_id)
     VALUES (?, ?, UNIX_TIMESTAMP(), ?, ?, ?, 0, ?)`,
    [accountId, username, motto, look, gender, Number(homeRoom) > 0 ? homeRoom : null]);
  const rules = gameRules();
  await conn.query('INSERT INTO player_balances (player_id, currency, amount) VALUES (?, -1, ?)', [res.insertId, credits]);
  const ids = currencyIds();
  if (ids.points !== null && rules['new_player.points'] > 0) {
    await conn.query('INSERT INTO player_balances (player_id, currency, amount) VALUES (?, ?, ?)', [res.insertId, ids.points, rules['new_player.points']]);
  }
  if (ids.premium !== null && rules['new_player.premium'] > 0) {
    await conn.query('INSERT INTO player_balances (player_id, currency, amount) VALUES (?, ?, ?)', [res.insertId, ids.premium, rules['new_player.premium']]);
  }
  if (rules['build_pass.membership_for_everyone']) {
    await conn.query(
      `INSERT INTO player_subscriptions (player_id, kind, started_at, duration_seconds, active)
       VALUES (?, 'build_pass', UNIX_TIMESTAMP(), ?, TRUE)`, [res.insertId, rules['build_pass.membership_days'] * SECONDS_PER_DAY]);
  }
  return res.insertId;
};

const inTransaction = async (fn) => {
  const conn = await getPool().getConnection();
  try {
    await conn.beginTransaction();
    const out = await fn(conn);
    await conn.commit();
    return out;
  } catch (e) {
    await conn.rollback();
    throw e;
  } finally {
    conn.release();
  }
};

export const createUser = ({ password, mail, ip, mayChangeName = false, ...avatar }) => inTransaction(async (conn) => {
  const acc = await conn.query(
    'INSERT INTO player_accounts (email, password_hash, registered_ip, last_ip) VALUES (?, ?, ?, ?)',
    [mail || null, password, ip, ip]);
  const userId = await insertAvatar(conn, acc.insertId, avatar);
  if (mayChangeName) await conn.query('INSERT INTO player_standing (player_id, may_change_name) VALUES (?, 1)', [userId]);
  await conn.query('UPDATE player_accounts SET player_id = ? WHERE id = ?', [userId, acc.insertId]);
  return { accountId: acc.insertId, userId };
});

export const createAvatar = (accountId, avatar) => inTransaction((conn) => insertAvatar(conn, accountId, avatar));

export const transaction = (fn) => inTransaction((conn) => fn((text, params = []) => conn.query(text, params)));

export const activeBan = async (userId, ip) => (await query(
  `SELECT b.reason, b.ends_at AS expires_at, b.player_id = p.id AS own
   FROM players p JOIN player_accounts a ON a.id = p.account_id JOIN bans b ON b.ends_at > UNIX_TIMESTAMP() AND (
     b.player_id = p.id
     OR (? <> '' AND b.barred_address = ?)
     OR (a.machine_id <> '' AND b.barred_machine = a.machine_id))
   WHERE p.id = ? ORDER BY own DESC, b.ends_at DESC LIMIT 1`, [ip, ip, userId]))[0] ?? null;

export const accountBan = async (accountId) => (await query(
  `SELECT b.reason, b.ends_at AS expires_at, 1 AS own
   FROM players p JOIN bans b ON b.player_id = p.id AND b.ends_at > UNIX_TIMESTAMP() AND b.avatar_only = 0
   WHERE p.account_id = ? ORDER BY b.ends_at DESC LIMIT 1`, [accountId]))[0] ?? null;

export const addressBan = async (ip) => (ip ? (await query(
  `SELECT reason, ends_at AS expires_at, 0 AS own FROM bans WHERE barred_address = ? AND ends_at > UNIX_TIMESTAMP()
   ORDER BY ends_at DESC LIMIT 1`, [ip]))[0] ?? null : null);

export const issueTicket = (userId, ticket, ip) => query(
  `UPDATE player_accounts a JOIN players p ON p.account_id = a.id
   SET a.player_id = p.id, a.login_ticket = ?, a.login_ticket_expires_at = UNIX_TIMESTAMP() + 60,
     a.login_ticket_device = NULL, a.last_ip = ?
   WHERE p.id = ?`, [ticket, ip, userId]);

const liveTicket = async () => {
  const honorExpiry = settingOn((await settings())['login.ticket.expires'], true);
  return `login_ticket = ?${honorExpiry ? ' AND (login_ticket_expires_at IS NULL OR login_ticket_expires_at >= UNIX_TIMESTAMP())' : ''}`;
};

export const ticketValid = async (ticket) => {
  const rows = await query(`SELECT player_id FROM player_accounts WHERE ${await liveTicket()} LIMIT 1`, [ticket]);
  return rows.length > 0;
};

export const bindTicketDevice = async (ticket, device) => query(
  `UPDATE player_accounts SET login_ticket_device = ? WHERE ${await liveTicket()} AND login_ticket_device IS NULL`,
  [device, ticket]);
