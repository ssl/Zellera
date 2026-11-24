import mariadb from 'mariadb';
import { config } from './config.mjs';

let pool = null;
const getPool = () => {
  pool ??= mariadb.createPool({
    ...config.db,
    connectionLimit: 6,
    acquireTimeout: 5000,
    connectTimeout: 5000,
    insertIdAsNumber: true,
    bigIntAsNumber: true,
    dateStrings: false,
  });
  return pool;
};

const READ = /^\s*(SELECT|WITH)\b/i;

class DbRefused extends Error {}

export const query = async (sql, params = []) => {
  if (!READ.test(sql)) throw new DbRefused('web/api reads only (SELECT / WITH)');
  if (!Array.isArray(params)) throw new DbRefused('params must be an array');
  return getPool().query(sql, params);
};

export const one = async (sql, params = []) => (await query(sql, params))[0] ?? null;

const DOWN = new Set(['ECONNREFUSED', 'ENOENT', 'ETIMEDOUT', 'ER_GET_CONNECTION_TIMEOUT', 'ER_CONNECTION_TIMEOUT',
  'ER_SOCKET_UNEXPECTED_CLOSE', 'ER_CLOSING_POOL', 'PROTOCOL_CONNECTION_LOST', 'ER_ACCESS_DENIED_ERROR',
  'ER_TABLEACCESS_DENIED_ERROR', 'ER_COLUMNACCESS_DENIED_ERROR', 'ER_NO_SUCH_TABLE']);
export const isDbDown = (e) => !!e && (DOWN.has(e.code) || e.fatal === true);

export const database = { query, one };

