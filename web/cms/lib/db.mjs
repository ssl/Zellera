import { COMMAND_TYPES, PUBLIC_COMMAND_TYPES, insertCommand } from './commands.mjs';
import { Sql, sql } from './sql.mjs';

const READ = /^\s*(SELECT|WITH|SHOW|EXPLAIN|DESCRIBE)\b/i;
const WRITE = /^\s*(INSERT|UPDATE)\b/i;

export class DbRefused extends Error {}

const checked = (q) => {
  if (!(q instanceof Sql)) throw new DbRefused('ctx.db takes sql`…` only (import { sql } from "./sql.mjs")');
  for (const p of q.params) {
    const ok = p === null || ['string', 'number', 'boolean', 'bigint'].includes(typeof p) || p instanceof Date || Buffer.isBuffer(p);
    if (!ok) throw new DbRefused(`sql parameter of type ${typeof p} refused`);
    if (typeof p === 'number' && !Number.isFinite(p)) throw new DbRefused('sql parameter is not a finite number');
  }
  return q;
};

const positiveId = (v) => Number.isInteger(v) && v > 0 && v <= 2147483647;

const INSERT_INTO = /^\s*INSERT\s+INTO\s+`?([a-z_][a-z0-9_]*)`?\s*\(/i;
const DELETE_FROM = /^\s*DELETE\s+(?:[a-z_][a-z0-9_]*\s+)?FROM\s+`?([a-z_][a-z0-9_]*)`?\s/i;

export const makeDb = (deps, { canWrite = false, commands = null, inserts = [], deletes = [] } = {}) => ({
  query: async (q) => {
    checked(q);
    if (!READ.test(q.text)) throw new DbRefused('ctx.db.query is read-only: use ctx.db.write in a housekeeping POST route');
    return deps.query(q.text, q.params);
  },
  write: async (q) => {
    checked(q);
    if (!canWrite) throw new DbRefused('ctx.db.write: only housekeeping POST routes write');
    if (deps.readOnly) throw new DbRefused('ctx.db.write: this server runs read-only');
    if (!WRITE.test(q.text)) throw new DbRefused('ctx.db.write: INSERT or UPDATE only');
    return deps.query(q.text, q.params);
  },
  remove: async (q) => {
    checked(q);
    if (!canWrite) throw new DbRefused('ctx.db.remove: only housekeeping POST routes delete');
    const table = DELETE_FROM.exec(q.text)?.[1];
    if (!table || !deletes.includes(table)) throw new DbRefused(`ctx.db.remove: this module deletes no rows of ${table ?? 'that table'}`);
    if (deps.readOnly) throw new DbRefused('ctx.db.remove: this server runs read-only');
    return deps.query(q.text, q.params);
  },
  insert: async (q) => {
    checked(q);
    const table = INSERT_INTO.exec(q.text)?.[1];
    if (!table || !inserts.includes(table)) throw new DbRefused(`ctx.db.insert: this route hands in no rows to ${table ?? 'that table'}`);
    if (deps.readOnly) throw new DbRefused('ctx.db.insert: this server runs read-only');
    return deps.query(q.text, q.params);
  },
  command: async ({ userId, type, payload = {}, createdBy = null } = {}) => {
    if (commands !== 'staff' && commands !== 'self') {
      throw new DbRefused('ctx.db.command: only housekeeping POST routes and public POST routes marked write: true send commands');
    }
    const allowed = commands === 'staff' ? COMMAND_TYPES : PUBLIC_COMMAND_TYPES;
    if (!allowed.includes(type)) throw new DbRefused(`ctx.db.command: ${JSON.stringify(type)} is not a command this route may send`);
    if (!positiveId(userId)) throw new DbRefused('ctx.db.command: no user');
    if (commands === 'staff' ? !positiveId(createdBy) : createdBy !== null) {
      throw new DbRefused('ctx.db.command: created_by is the staff member on housekeeping routes and empty on the player\'s own');
    }
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) throw new DbRefused('ctx.db.command: the payload is an object');
    if (deps.readOnly) throw new DbRefused('ctx.db.command: this server runs read-only');
    return insertCommand(deps.query, { userId, type, payload, createdBy });
  },
  audit: async ({ actorId, targetType, targetId, targetLabel, action, detail, success }) => {
    const row = {
      actorId: Number(actorId) || 0,
      targetType: String(targetType ?? 'user').slice(0, 16),
      targetId: Number(targetId) || 0,
      targetLabel: String(targetLabel ?? '').slice(0, 128),
      action: String(action ?? '').slice(0, 64),
      detail: String(detail ?? '').slice(0, 500),
      success: success === false ? 0 : 1,
    };
    if (deps.readOnly) { deps.onAudit?.(row); return; }
    const q = sql`INSERT INTO staff_actions (acted_at, staff_id, action, subject_type, subject_id, subject_label, detail, success)
      VALUES (UNIX_TIMESTAMP(), ${row.actorId}, ${row.action}, ${row.targetType}, ${row.targetId}, ${row.targetLabel}, ${row.detail}, ${row.success})`;
    await deps.query(q.text, q.params);
  },
});
