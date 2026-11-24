export class Sql {
  constructor(text, params) {
    this.text = text;
    this.params = params;
    Object.freeze(this);
  }
}

export const sql = (strings, ...values) => {
  if (!Array.isArray(strings) || !Array.isArray(strings.raw) || !Object.isFrozen(strings)) {
    throw new TypeError('sql must be used as a template tag: sql`SELECT … ${value}`');
  }
  let text = strings[0];
  const params = [];
  values.forEach((v, i) => {
    if (v instanceof Sql) { text += v.text; params.push(...v.params); } else { text += '?'; params.push(v); }
    text += strings[i + 1];
  });
  return new Sql(text, params);
};

sql.in = (list) => {
  const arr = Array.from(list ?? []);
  if (arr.length < 1 || arr.length > 1000) throw new RangeError(`sql.in: 1..1000 values, got ${arr.length}`);
  return new Sql(arr.map(() => '?').join(', '), arr);
};
sql.ident = (name, allowed) => {
  if (!Array.isArray(allowed) || !allowed.includes(name) || !/^[a-z_][a-z0-9_]*(\.[a-z_][a-z0-9_]*)?$/i.test(name)) {
    throw new RangeError(`sql.ident: ${JSON.stringify(name)} is not in the allow-list`);
  }
  return new Sql(name.split('.').map((p) => `\`${p}\``).join('.'), []);
};
sql.desc = (descending) => new Sql(descending ? 'DESC' : 'ASC', []);
sql.join = (parts, sep = ', ') => {
  if (!/^[\s,]*(AND|OR)?[\s,]*$/i.test(sep)) throw new RangeError('sql.join: separator must be , AND or OR');
  const list = Array.from(parts ?? []);
  for (const p of list) if (!(p instanceof Sql)) throw new TypeError('sql.join: parts must be sql`` values');
  return new Sql(list.map((p) => p.text).join(sep), list.flatMap((p) => p.params));
};

const likeEscape = (s) => String(s ?? '').replace(/[\\%_]/g, (c) => `\\${c}`);
export const likePrefix = (s) => `${likeEscape(s)}%`;
export const likeContains = (s) => `%${likeEscape(s)}%`;

export const intParam = (v, { min = 1, max = 2147483647 } = {}) => {
  const s = String(v ?? '').trim();
  if (!/^\d{1,10}$/.test(s)) return null;
  const n = Number(s);
  return n >= min && n <= max ? n : null;
};

export { unixNow } from '../../lib/time.mjs';

export const FORBIDDEN_COLUMNS = {
  player_accounts: ['password_hash', 'login_ticket', 'login_ticket_expires_at', 'login_ticket_device', 'email', 'email_verified'],
  rooms: ['door_secret'],
};
export const STAFF_ONLY_COLUMNS = {
  bans: ['barred_address', 'barred_machine'],
  help_requests: ['ip', 'contact'],
};
export const PUBLIC_INSERTS = Object.freeze({
  help_requests: Object.freeze(['player_id', 'name', 'contact', 'topic', 'message', 'ip', 'created_at']),
});
export const STAFF_ONLY_TABLES = [
  'bans', 'sanctions', 'sanction_steps', 'room_chat_messages', 'direct_messages', 'room_visits', 'support_tickets',
  'report_topics', 'report_categories', 'issue_categories', 'issue_presets', 'moderator_presets',
  'staff_actions', 'rank_changes', 'command_runs', 'name_changes', 'player_accounts', 'trades', 'trade_items',
  'permissions', 'rank_permissions', 'player_devices', 'device_bans', 'device_ban_exemptions',
];
