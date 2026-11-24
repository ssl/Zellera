import { sql } from './sql.mjs';
import { currencyIds, displayNames } from '../display-names.mjs';

export const COMMAND_TYPES = Object.freeze(['setCurrency', 'addCurrency', 'giveBadge', 'takeBadge', 'setMotto', 'setRank', 'askRoomQuestion', 'setPrivacy', 'reloadTvPlaylists']);
export const PUBLIC_COMMAND_TYPES = Object.freeze(['setMotto', 'setPrivacy']);

const COMMAND_INSERT = Object.freeze(['player_id', 'kind', 'payload', 'created_by', 'created_at']);
export const COMMAND_SELECT = Object.freeze(['id', 'player_id', 'kind', 'status', 'result', 'processed_at']);
export const COMMAND_GRANT = Object.freeze({ table: 'web_commands', insert: [...COMMAND_INSERT], select: [...COMMAND_SELECT] });

const currencies = () => {
  const ids = currencyIds();
  const names = displayNames();
  return [
    { key: 'credits', type: ids.credits, label: names.Credits },
    ...(ids.points === null ? [] : [{ key: 'points', type: ids.points, label: names.Points }]),
    ...(ids.premium === null ? [] : [{ key: 'premium', type: ids.premium, label: names.Premium }]),
  ];
};

const SEASONAL_NAME = 'catalog.purchase.youractivitypoints.';

export const seasonalName = (texts, type) => String(texts?.get(`${SEASONAL_NAME}${type}`, '') ?? '')
  .replace(/%activitypoints%/g, '').replace(/[\s:]+$/, '').trim() || `Currency ${type}`;

export const seasonalTypes = (rules) => (rules['seasonal_currency.enabled']
  ? rules['seasonal_currency.types'].filter((t) => !currencies().some((c) => c.type === t)) : []);

export const walletCurrencies = (rules, texts) => [
  ...currencies(),
  ...seasonalTypes(rules).map((type) => ({ key: `currency_${type}`, type, label: seasonalName(texts, type) })),
];

const PAYLOAD_MAX = 2048;

export const insertCommand = async (query, { userId, type, payload, createdBy = null }) => {
  const json = JSON.stringify(payload ?? {});
  if (Buffer.byteLength(json) > PAYLOAD_MAX) throw new RangeError('web_commands: payload too long');
  const q = sql`INSERT INTO web_commands (player_id, kind, payload, created_by, created_at)
    VALUES (${userId}, ${type}, ${json}, ${createdBy}, UNIX_TIMESTAMP())`;
  const r = await query(q.text, q.params);
  const id = Number(r?.insertId);
  if (!(id > 0)) throw new Error('web_commands: no row id');
  return id;
};

const sleep = (ms) => new Promise((resolve) => { setTimeout(resolve, ms); });

export const waitForCommands = async (db, ids, { userId = null, timeoutMs = 2000, everyMs = 200 } = {}) => {
  const list = [...new Set([...ids].map(Number).filter((n) => Number.isInteger(n) && n > 0))].slice(0, 100);
  if (list.length === 0) return [];
  const read = () => db.query(sql`SELECT id, player_id, kind, status, result, processed_at FROM web_commands
    WHERE id IN (${sql.in(list)})${userId != null ? sql` AND player_id = ${Number(userId)}` : sql``} ORDER BY id`);
  const until = Date.now() + Math.max(0, timeoutMs);
  let rows = [...await read()];
  while (rows.some((r) => r.status === 'pending') && Date.now() < until) {
    await sleep(Math.min(everyMs, Math.max(0, until - Date.now())));
    rows = [...await read()];
  }
  return rows;
};
