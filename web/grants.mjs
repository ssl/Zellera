#!/usr/bin/env node
import { grants as keyGrants } from './api/lib/keys.mjs';
import { MODULES } from './api/index.mjs';
import { collectGrants, loadModules } from './cms/registry.mjs';
import { EXTENSIONS_SETTING, MIGRATIONS_TABLE, parseList, readManifest } from './cms/extensions.mjs';

const args = process.argv.slice(2);
const opt = (name) => {
  const out = [];
  for (let i = 0; i < args.length; i++) if (args[i] === `--${name}` && args[i + 1]) out.push(args[++i]);
  return out;
};
const db = opt('db')[0] ?? process.env.ZEL_DB_NAME ?? 'zellera';
const user = opt('user')[0] ?? process.env.ZEL_DB_WEB_USER ?? 'zellera_web';
const hosts = opt('host').length ? opt('host') : ['127.0.0.1', 'localhost'];
for (const v of [db, user, ...hosts]) {
  if (!/^[A-Za-z0-9_.-]{1,64}$/.test(v)) { console.error(`grants.mjs: refusing ${JSON.stringify(v)}`); process.exit(2); }
}

const LOGIN = [
  { table: 'players', select: ['id', 'account_id', 'name', 'figure', 'gender', 'motto', 'created_at', 'last_online_at'], insert: ['account_id', 'name', 'created_at', 'motto', 'figure', 'gender', 'rank_id', 'home_room_id'] },
  { table: 'player_accounts', select: ['id', 'player_id', 'email', 'password_hash', 'login_ticket', 'login_ticket_expires_at', 'login_ticket_device', 'registered_ip', 'machine_id'], insert: ['email', 'password_hash', 'registered_ip', 'last_ip'], update: ['player_id', 'email', 'email_verified', 'password_hash', 'login_ticket', 'login_ticket_expires_at', 'login_ticket_device', 'login_ticket_ip', 'last_ip'] },
  { table: 'bans', select: ['player_id', 'ends_at', 'reason', 'barred_address', 'barred_machine'] },
  { table: 'settings', select: '*' },
  { table: 'client_strings', select: '*' },
  { table: 'currencies', select: ['id', 'name', 'role'] },
  { table: 'display_names', select: ['code', 'name'] },
  { table: 'player_subscriptions', insert: ['player_id', 'kind', 'started_at', 'duration_seconds', 'active'] },
  { table: 'player_balances', insert: ['player_id', 'currency', 'amount'] },
  { table: 'player_standing', insert: ['player_id', 'may_change_name'] },
  { table: 'suspicions', insert: ['player_id', 'address', 'device', 'kind', 'weight', 'detail', 'created_at'] },
];
const SESSION_REVOCATIONS = 'web_session_revocations';

const apiGrants = () => {
  const merged = new Map();
  for (const g of [...keyGrants, ...Object.values(MODULES).flatMap((m) => m.grants ?? [])]) {
    if (!/^[a-z0-9_]{1,64}$/.test(g.table)) throw new Error(`bad table ${g.table}`);
    const set = merged.get(g.table) ?? new Set();
    for (const c of g.select ?? []) {
      if (!/^[a-z0-9_]{1,64}$/.test(c)) throw new Error(`bad column ${g.table}.${c}`);
      set.add(c);
    }
    merged.set(g.table, set);
  }
  return [...merged].sort(([a], [b]) => a.localeCompare(b)).map(([table, cols]) => ({ table, select: [...cols] }));
};

let extensionTables;
try {
  extensionTables = parseList(process.env[EXTENSIONS_SETTING.env] ?? EXTENSIONS_SETTING.list).flatMap(({ dir }) => readManifest(dir).tables);
} catch (e) {
  console.error(`${EXTENSIONS_SETTING.env}: ${e.message}`);
  process.exit(1);
}

const { loaded, missing } = await loadModules();
const columnGrants = [...LOGIN, ...collectGrants(loaded), ...apiGrants()];
const cols = (list) => list.map((c) => `\`${c}\``).join(', ');
const lines = missing.length ? [`-- page modules not present yet: ${missing.join(', ')}`] : [];
for (const host of hosts) {
  const to = `'${user}'@'${host}'`;
  const on = (table) => `\`${db}\`.\`${table}\``;
  for (const g of columnGrants) {
    const select = g.select ?? [];
    if (select === '*') lines.push(`GRANT SELECT ON ${on(g.table)} TO ${to};`);
    else if (select.length) lines.push(`GRANT SELECT (${cols(select)}) ON ${on(g.table)} TO ${to};`);
    if (g.insert?.length) lines.push(`GRANT INSERT (${cols(g.insert)}) ON ${on(g.table)} TO ${to};`);
    if (g.update?.length) lines.push(`GRANT UPDATE (${cols(g.update)}) ON ${on(g.table)} TO ${to};`);
    if (g.delete === true) lines.push(`GRANT DELETE ON ${on(g.table)} TO ${to};`);
  }
  lines.push(`GRANT SELECT, INSERT, DELETE ON ${on(SESSION_REVOCATIONS)} TO ${to};`);
  if (extensionTables.length) lines.push(`GRANT CREATE, SELECT, INSERT ON ${on(MIGRATIONS_TABLE)} TO ${to};`);
  for (const t of extensionTables) lines.push(`GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON ${on(t)} TO ${to};`);
}
lines.push('FLUSH PRIVILEGES;');
process.stdout.write(`${lines.join('\n')}\n`);
