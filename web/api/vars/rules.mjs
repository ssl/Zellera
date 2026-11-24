import { isoMs } from '../lib/dates.mjs';
import { userUid } from '../lib/ids.mjs';

export const E = {
  notFound: 'not-found',
  requestFailed: 'request-failed-non-200',
  apiDisabled: 'wired.variables.api_disabled',
  invalidTarget: 'wired.variables.invalid_target',
  invalidValue: 'wired.variables.invalid_value',
  notAllowed: 'wired.variables.operation_not_allowed',
  notFoundVar: 'wired.variables.not_found',
  entityNotFound: 'wired.variables.entity_not_found',
  bulkEmpty: 'wired.variables.bulk_delete_empty',
  bulkInvalid: 'wired.variables.bulk_delete_invalid_variable',
  batchEmpty: 'wired.variables.batch_empty',
  batchLimit: 'wired.variables.batch_limit_exceeded',
  tooMany: 'wired.variables.too_many_requests',
};

const KINDS = { user: ['users', 'pets', 'bots'], furni: ['furni', 'furni-bc', 'wall-items', 'wall-items-bc'] };
const TARGET_KEY = { users: 'user', pets: 'pet', bots: 'bot', furni: 'furni', 'furni-bc': 'furni_bc', 'wall-items': 'wall_item', 'wall-items-bc': 'wall_item_bc' };
export const SCOPE_LIST = { user: 'users', furni: 'furni', global: 'global' };

const NAME_RE = /^[A-Za-z0-9_]{1,40}$/;
export const isName = (s) => typeof s === 'string' && NAME_RE.test(s);

const BATCH_LIMIT = 50;
const PAGE_SIZE = 50;
const MAX_PAGE_SIZE = 100;
const BULK_DELETE_MAX = 1000;

const INT_RE = /^\d{1,10}$/;
const INT_MAX = 2147483647;
const BC_MAX = 0xFFFF;

export const parseRoomId = (raw) => (typeof raw === 'string' && INT_RE.test(raw) && Number(raw) > 0 && Number(raw) <= INT_MAX ? Number(raw) : null);

export const scopeOf = (raw) => String(raw).toLowerCase();

const isKindOf = (scope, kind) => KINDS[scope]?.includes(kind) ?? false;

const entityIdOf = (kind, raw) => {
  if (typeof raw !== 'string' || !INT_RE.test(raw)) return null;
  const n = Number(raw);
  return n <= (kind.endsWith('-bc') ? BC_MAX : INT_MAX) ? n : null;
};

const fail = (status, code) => ({ fail: { status, code } });
const isObject = (o) => o !== null && typeof o === 'object' && !Array.isArray(o);

export const checkTarget = ({ scope, kind, nameRaw, rawId }) => {
  if (!isKindOf(scope, kind)) return fail(400, E.invalidTarget);
  if (nameRaw !== undefined && !isName(nameRaw)) return fail(400, E.invalidTarget);
  if (rawId === undefined) return { scope, kind };
  const id = entityIdOf(kind, rawId);
  return id === null ? fail(404, E.notFound) : { scope, kind, id };
};

const LONG_MIN = -(2n ** 63n);
const LONG_MAX = 2n ** 63n - 1n;
export const parseValue = (v) => {
  if (v === undefined || v === null) return fail(400, E.invalidValue);
  let n;
  if (typeof v === 'bigint') n = v;
  else if (typeof v === 'number') {
    if (!Number.isInteger(v)) return fail(400, E.requestFailed);
    n = BigInt(v);
  } else if (typeof v === 'string' && /^[+-]?\d{1,20}$/.test(v)) n = BigInt(v);
  else return fail(400, E.requestFailed);
  if (n < LONG_MIN || n > LONG_MAX) return fail(400, E.requestFailed);
  return { value: n.toString() };
};
const unreadable = (v) => v !== undefined && v !== null && parseValue(v).fail?.code === E.requestFailed;

export const parseValueBody = (body) => parseValue(isObject(body) ? body.value : undefined);

export const parseListQuery = (q) => {
  const has = (k) => q.has(k);
  const get = (k) => q.get(k);
  const orderBy = has('order_by') ? get('order_by') : 'creation_time';
  const orderDir = has('order_dir') ? get('order_dir') : 'asc';
  if (!['value', 'creation_time', 'update_time'].includes(orderBy) || !['asc', 'desc'].includes(orderDir)) return fail(400, E.invalidTarget);
  let page = 1;
  let size = PAGE_SIZE;
  if (has('page')) {
    const p = get('page');
    if (!INT_RE.test(p) || Number(p) < 1 || Number(p) > INT_MAX) return fail(400, E.invalidTarget);
    page = Number(p);
  }
  if (has('size')) {
    const s = get('size');
    if (!INT_RE.test(s) || Number(s) < 1) return fail(400, E.invalidTarget);
    size = Math.min(Number(s), MAX_PAGE_SIZE);
  }
  return { orderBy, orderDir, page, size };
};

export const parseBulkDelete = (body) => {
  const list = isObject(body) ? body.variables : undefined;
  if (!Array.isArray(list) || list.length === 0) return fail(400, E.bulkEmpty);
  if (!list.every(isName)) return fail(400, E.bulkInvalid);
  const names = [...new Set(list)];
  if (names.length > BULK_DELETE_MAX) return fail(400, E.bulkInvalid);
  return { names };
};

export const parseVariablesPatch = (body) => {
  const vars = isObject(body) ? body.variables : undefined;
  if (vars !== undefined && vars !== null && !isObject(vars)) return fail(400, E.requestFailed);
  if (isObject(vars) && Object.values(vars).some(unreadable)) return fail(400, E.requestFailed);
  if (!isObject(vars) || Object.keys(vars).length === 0) return fail(400, E.invalidTarget);
  const entries = [];
  for (const [name, raw] of Object.entries(vars)) {
    if (!isName(name)) return fail(400, E.invalidTarget);
    if (raw === null) { entries.push([name, null]); continue; }
    const v = parseValue(raw);
    if (v.fail) return v;
    entries.push([name, v.value]);
  }
  return { entries };
};

const METHODS = ['GET', 'PUT', 'PATCH', 'DELETE'];
export const parseBatch = (body, scope) => {
  const list = isObject(body) ? body.requests : undefined;
  if (list !== undefined && list !== null && !Array.isArray(list)) return fail(400, E.requestFailed);
  if (Array.isArray(list) && list.some((op) => !isObject(op) || (op.body != null && !isObject(op.body)) || (isObject(op.body) && unreadable(op.body.value)))) {
    return fail(400, E.requestFailed);
  }
  if (!Array.isArray(list) || list.length === 0) return fail(400, E.batchEmpty);
  if (list.length > BATCH_LIMIT) return fail(400, E.batchLimit);
  const ops = list.map((op, i) => {
    const opId = typeof op.op_id === 'string' ? op.op_id : null;
    if (!METHODS.includes(op.method)) return { i, opId, ...fail(400, E.invalidTarget) };
    const method = op.method;
    const parts = typeof op.path === 'string' ? op.path.replace(/^\//, '').split('/') : [];
    if (parts.length !== 2) return { i, opId, method, ...fail(400, E.invalidTarget) };
    const t = checkTarget({ scope, kind: parts[0], rawId: parts[1] });
    if (t.fail) return { i, opId, method, fail: t.fail };
    const out = { i, opId, method, kind: t.kind, id: t.id };
    if (method === 'PUT' || method === 'PATCH') {
      const v = parseValueBody(op.body);
      if (v.fail) return { i, opId, method, fail: v.fail.code === E.invalidValue && (op.body?.value ?? null) === null ? { status: 400, code: E.invalidTarget } : v.fail };
      out.value = v.value;
    }
    return out;
  });
  return { ops };
};

export const batchNeed = (ops) => {
  const reads = ops.some((o) => o.method === 'GET');
  const writes = ops.some((o) => o.method && o.method !== 'GET');
  return reads && writes ? 'both' : writes ? 'write' : 'read';
};

export const impossibleName = (name) => name.length > 50 || /[\u{10000}-\u{10FFFF}]/u.test(name);

export const wiredVariable = (e) => ({
  value: String(e?.value ?? 0),
  creation_time: isoMs(e?.createdAt),
  update_time: isoMs(e?.updatedAt),
});

export const variableMap = (vars) => Object.fromEntries(Object.entries(isObject(vars) ? vars : {}).map(([n, e]) => [n, wiredVariable(e)]));

const userName = (t, userNames) => (typeof t?.name === 'string' ? t.name : (userNames.get(Number(t?.id)) ?? ''));

const targetObject = (kind, t, userNames = new Map()) => {
  const id = Number(t?.id);
  if (kind === 'users') return { id, name: userName(t, userNames), unique_id: id > 0 ? userUid(id) : null };
  if (kind === 'pets' || kind === 'bots') return typeof t?.name === 'string' ? { id, name: t.name } : { id };
  return { id };
};

const listTarget = (kind, t, userNames = new Map()) => {
  const id = Number(t?.id);
  if (kind === 'users') return { unique_id: id > 0 ? userUid(id) : null, name: userName(t, userNames), id };
  if (kind === 'pets' || kind === 'bots') return typeof t?.name === 'string' ? { name: t.name, id } : { id };
  return { id };
};

export const profileOf = (kind, data, userNames) => ({
  [TARGET_KEY[kind]]: targetObject(kind, data?.target, userNames),
  variables: variableMap(data?.variables),
});

export const holderItem = (kind, h, userNames) => (kind === 'bots'
  ? { [TARGET_KEY[kind]]: listTarget(kind, h, userNames), variable: wiredVariable(h?.entry) }
  : { variable: wiredVariable(h?.entry), [TARGET_KEY[kind]]: listTarget(kind, h, userNames) });

export const roomVariables = (data) => Object.fromEntries(['users', 'furni', 'global']
  .map((k) => [k, Array.isArray(data?.[k]) ? data[k].filter((n) => typeof n === 'string') : []]));

export const batchResult = (opId, status, { entry, error } = {}) => ({
  status,
  body: status === 200 ? wiredVariable(entry) : null,
  error: status === 200 || status === 204 ? null : { code: error, message: error },
  op_id: opId,
});

export const countTtlMs = (count) => (count < 1_000 ? 20_000 : count < 10_000 ? 60_000 : count < 100_000 ? 300_000 : 600_000);

export const createCountCache = ({ now = () => Date.now(), maxEntries = 20_000 } = {}) => {
  const map = new Map();
  return {
    get(key) {
      const hit = map.get(key);
      if (!hit) return null;
      if (hit.until <= now()) { map.delete(key); return null; }
      return hit.count;
    },
    set(key, count) {
      const t = now();
      if (map.size >= maxEntries) {
        for (const [k, v] of map) if (v.until <= t) map.delete(k);
        while (map.size >= maxEntries) map.delete(map.keys().next().value);
      }
      map.set(key, { count, until: t + countTtlMs(count) });
    },
    size: () => map.size,
  };
};
