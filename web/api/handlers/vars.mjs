import { BridgeDown, callBridge } from '../lib/bridge.mjs';
import { config } from '../lib/config.mjs';
import { error, fail, HttpError, noContent, notFound, ok } from '../lib/http.mjs';
import { userIdOf } from '../lib/ids.mjs';
import { checkWiredKeys } from '../lib/keys.mjs';
import { wiredRules } from '../lib/limiter.mjs';
import { spec } from '../lib/spec.mjs';
import {
  batchNeed, batchResult, checkTarget, createCountCache, E, holderItem, impossibleName, isName, parseBatch,
  parseBulkDelete, parseListQuery, parseValueBody, parseVariablesPatch, profileOf, parseRoomId, roomVariables,
  SCOPE_LIST, scopeOf, variableMap, wiredVariable,
} from '../vars/rules.mjs';

export const grants = [
  { table: 'rooms', select: ['id'] },
  { table: 'players', select: ['id', 'name'] },
];

const CODE_RE = /^[a-z0-9_.-]{1,80}$/i;

export const enforcedRules = (cls) => {
  const e = spec.rateLimits[cls]?.enforced;
  if (!e) return wiredRules(cls);
  const rules = [{ windowMs: 60_000, max: e.perMinute }];
  if (e.per10s) rules.push({ windowMs: 10_000, max: e.per10s });
  return rules;
};
const tooMany = (retryAfter) => error(429, E.tooMany, { 'Retry-After': String(retryAfter) });
export const badJson = () => ({ status: 400, type: 'text/html; charset=utf-8', body: Buffer.from('Bad JSON', 'utf8'), headers: {} });

const userNames = async (db, kind, targets) => {
  if (kind !== 'users') return new Map();
  const ids = [...new Set(targets.filter((t) => typeof t?.name !== 'string').map((t) => Number(t?.id)))]
    .filter((id) => Number.isInteger(id) && id > 0);
  if (ids.length === 0) return new Map();
  const rows = await db.query(`SELECT id, name AS username FROM players WHERE id IN (${ids.map(() => '?').join(', ')})`, ids);
  return new Map(rows.map((r) => [Number(r.id), String(r.username)]));
};

const PATCH_CODES = { [E.notFoundVar]: [400, E.invalidTarget] };

export const createVarsHandlers = ({ bridge = callBridge, now = () => Date.now(), enabled = config.wiredApiEnabled } = {}) => {
  const counts = createCountCache({ now });

  const ask = async (op, roomId, boxes, args = {}, codes = {}) => {
    const r = await bridge(op, { roomId, boxes, ...args });
    if (r.ok) return r.data;
    if (!(r.status >= 400 && r.status <= 599) || !CODE_RE.test(r.error)) throw new BridgeDown(`bridge ${op}: bad refusal`);
    const [status, code] = codes[r.error] ?? [r.status, r.error];
    return fail(error(status, code));
  };

  const wired = ({ need, cls, check, run }) => async (ctx) => {
    let body;
    try { body = await ctx.body(); } catch (e) {
      if (e instanceof HttpError && e.reply.status === 400) return badJson();
      throw e;
    }
    const roomId = parseRoomId(ctx.params.roomId);
    if (roomId === null || !(await ctx.db.one('SELECT id FROM rooms WHERE id = ? LIMIT 1', [roomId]))) return notFound();
    if (!enabled) return error(403, E.apiDisabled);
    const input = check ? await check(ctx, body) : {};
    if (input.fail) return error(input.fail.status, input.fail.code);
    const batch = typeof need === 'function';
    const kinds = batch ? need(input) : need;
    const keys = await checkWiredKeys(ctx.db, roomId, kinds, ctx.req.headers, { batch });
    if (!keys.ok) return keys.reply;
    const take = (c, cost = 1) => ctx.limiter.take(`room:${roomId}:${c}`, enforcedRules(c), cost);
    const r = take(cls);
    if (!r.ok) return tooMany(r.retryAfter);
    return run({ ctx, roomId, input, take, boxes: keys.boxes, call: (op, args, codes) => ask(op, roomId, keys.boxes, args, codes) });
  };

  const target = (ctx, { withName = true, withId = true, scope = scopeOf(ctx.params.scope) } = {}) => checkTarget({
    scope,
    kind: ctx.params.targetKind,
    nameRaw: withName ? ctx.params.variableName : undefined,
    rawId: withId ? ctx.params.entityId : undefined,
  });
  const withValue = (t, body) => {
    if (t.fail) return t;
    const v = parseValueBody(body);
    return v.fail ? v : { ...t, value: v.value };
  };
  const withPatch = (t, body) => {
    if (t.fail) return t;
    const p = parseVariablesPatch(body);
    return p.fail ? p : { ...t, entries: p.entries };
  };
  const missingName = (name) => (isName(name) ? null : error(404, E.notFoundVar));
  const spendWrites = (take, n) => {
    const r = take('wired.batchWrites', n);
    return r.ok ? null : tooMany(r.retryAfter);
  };

  const entry = (op, need, cls, check) => wired({
    need, cls, check,
    run: async ({ ctx, input: t, call }) => {
      const args = { scope: t.scope, name: ctx.params.variableName, kind: t.kind, id: t.id };
      if (t.value !== undefined) args.value = t.value;
      const data = await call(op, args);
      return op === 'deleteEntry' ? noContent() : ok(wiredVariable(data));
    },
  });

  const getProfile = (scope) => async ({ ctx, input: t, call }) => {
    const data = await call('getProfile', { scope, kind: t.kind, id: t.id });
    return ok(profileOf(t.kind, data, await userNames(ctx.db, t.kind, [data?.target])));
  };
  const patchProfile = (scope) => async ({ ctx, input: t, call, take }) => {
    const limited = spendWrites(take, t.entries.length);
    if (limited) return limited;
    const data = await call('patchProfile', { scope, kind: t.kind, id: t.id, variables: Object.fromEntries(t.entries) }, PATCH_CODES);
    return ok(profileOf(t.kind, data, await userNames(ctx.db, t.kind, [data?.target])));
  };

  return {
    listVariables: wired({
      need: 'read', cls: 'wired.read',
      run: async ({ call }) => ok(roomVariables(await call('listVariables'))),
    }),

    getVariable: entry('getEntry', 'read', 'wired.read', (ctx) => target(ctx)),
    putVariable: entry('putEntry', 'write', 'wired.write', (ctx, body) => withValue(target(ctx), body)),
    patchVariable: entry('patchEntry', 'write', 'wired.write', (ctx, body) => withValue(target(ctx), body)),
    deleteVariable: entry('deleteEntry', 'write', 'wired.write', (ctx) => target(ctx)),

    listVariableHolders: wired({
      need: 'read', cls: 'wired.list',
      check: (ctx) => {
        const t = target(ctx, { withId: false });
        if (t.fail) return t;
        const q = parseListQuery(ctx.query);
        return q.fail ? q : { ...t, ...q };
      },
      run: async ({ ctx, input: t, call }) => {
        const data = await call('listHolders', {
          scope: t.scope, name: ctx.params.variableName, kind: t.kind, orderBy: t.orderBy, orderDir: t.orderDir, page: t.page, size: t.size,
        });
        const holders = Array.isArray(data?.holders) ? data.holders : [];
        const names = await userNames(ctx.db, t.kind, holders);
        return ok({ items: holders.map((h) => holderItem(t.kind, h, names)), page: t.page, size: t.size });
      },
    }),

    countVariableHolders: wired({
      need: 'read', cls: 'wired.list',
      check: (ctx) => target(ctx, { withId: false }),
      run: async ({ ctx, roomId, input: t, boxes, call }) => {
        const key = `${roomId}\n${t.scope}\n${ctx.params.variableName}\n${t.kind}\n${boxes.read}`;
        let count = counts.get(key);
        if (count === null) {
          const data = await call('countHolders', { scope: t.scope, name: ctx.params.variableName, kind: t.kind });
          count = Number(data?.count);
          if (!Number.isSafeInteger(count) || count < 0) throw new BridgeDown('bridge countHolders: bad count');
          counts.set(key, count);
        }
        return ok({ count });
      },
    }),

    bulkDeleteVariables: wired({
      need: 'write', cls: 'wired.bulkDelete',
      check: (ctx, body) => parseBulkDelete(body),
      run: async ({ input, call }) => {
        await call('bulkDelete', { names: input.names });
        return noContent();
      },
    }),

    batchVariable: wired({
      need: (input) => batchNeed(input.ops), cls: 'wired.batch',
      check: (ctx, body) => {
        const scope = scopeOf(ctx.params.scope);
        const b = parseBatch(body, scope);
        return b.fail ? b : { scope, ops: b.ops };
      },
      run: async ({ ctx, input: { scope, ops }, call, take }) => {
        const name = ctx.params.variableName;
        const missing = missingName(name);
        if (missing) return missing;
        const results = new Array(ops.length);
        const send = [];
        for (const op of ops) {
          if (op.fail) { results[op.i] = batchResult(op.opId, op.fail.status, { error: op.fail.code }); continue; }
          if (op.method !== 'GET') {
            const r = take('wired.batchWrites');
            if (!r.ok) { results[op.i] = batchResult(op.opId, 429, { error: E.tooMany }); continue; }
          }
          send.push(op);
        }
        if (send.length === 0) {
          if (!roomVariables(await call('listVariables'))[SCOPE_LIST[scope]].includes(name)) return error(404, E.notFoundVar);
          return ok({ results });
        }
        const data = await call('batch', {
          scope, name,
          ops: send.map(({ i, method, kind, id, value }) => (value === undefined ? { i, method, kind, id } : { i, method, kind, id, value })),
        });
        const byIndex = new Map((Array.isArray(data?.results) ? data.results : []).map((r) => [r?.i, r]));
        for (const op of send) {
          const r = byIndex.get(op.i);
          const fine = r && (r.status === 200 || r.status === 204 || (r.status >= 400 && r.status <= 599 && CODE_RE.test(r.error)));
          if (!fine) throw new BridgeDown('bridge batch: bad result');
          results[op.i] = batchResult(op.opId, r.status, r);
        }
        return ok({ results });
      },
    }),

    getGlobalVariable: wired({
      need: 'read', cls: 'wired.read',
      run: async ({ ctx, call }) => missingName(ctx.params.variableName)
        ?? ok(wiredVariable(await call('getGlobal', { name: ctx.params.variableName }))),
    }),
    patchGlobalVariable: wired({
      need: 'write', cls: 'wired.write',
      check: (ctx, body) => parseValueBody(body),
      run: async ({ ctx, input, call }) => missingName(ctx.params.variableName)
        ?? ok(wiredVariable(await call('patchGlobal', { name: ctx.params.variableName, value: input.value }))),
    }),

    getUserVariablesByLookup: wired({
      need: 'read', cls: 'wired.profileRead',
      check: (ctx) => {
        const uniqueId = ctx.query.get('unique_id') ?? '';
        const name = ctx.query.get('name') ?? '';
        return uniqueId || name ? { uniqueId, name } : { fail: { status: 400, code: E.invalidTarget } };
      },
      run: async ({ ctx, input, call }) => {
        let id = null;
        const names = new Map();
        if (input.uniqueId) {
          id = userIdOf(input.uniqueId);
        } else if (!impossibleName(input.name)) {
          const row = await ctx.db.one('SELECT id, name AS username FROM players WHERE name = ? LIMIT 1', [input.name]);
          if (row) { id = Number(row.id); names.set(id, String(row.username)); }
        }
        if (id === null) return error(404, E.entityNotFound);
        const data = await call('getProfile', { scope: 'user', kind: 'users', id });
        const more = await userNames(ctx.db, 'users', names.has(id) ? [] : [data?.target]);
        return ok(profileOf('users', data, new Map([...names, ...more])));
      },
    }),

    getUserVariables: wired({
      need: 'read', cls: 'wired.profileRead',
      check: (ctx) => target(ctx, { withName: false, scope: 'user' }),
      run: getProfile('user'),
    }),
    patchUserVariables: wired({
      need: 'write', cls: 'wired.batch',
      check: (ctx, body) => withPatch(target(ctx, { withName: false, scope: 'user' }), body),
      run: patchProfile('user'),
    }),
    deleteUserVariables: wired({
      need: 'write', cls: 'wired.batch',
      check: (ctx) => target(ctx, { withName: false, scope: 'user' }),
      run: async ({ input: t, call }) => {
        await call('deleteProfile', { kind: t.kind, id: t.id });
        return noContent();
      },
    }),

    getFurniVariables: wired({
      need: 'read', cls: 'wired.profileRead',
      check: (ctx) => target(ctx, { withName: false, scope: 'furni' }),
      run: getProfile('furni'),
    }),
    patchFurniVariables: wired({
      need: 'write', cls: 'wired.batch',
      check: (ctx, body) => withPatch(target(ctx, { withName: false, scope: 'furni' }), body),
      run: patchProfile('furni'),
    }),

    getGlobalVariables: wired({
      need: 'read', cls: 'wired.profileRead',
      run: async ({ call }) => ok({ variables: variableMap((await call('getGlobalProfile'))?.variables) }),
    }),
    patchGlobalVariables: wired({
      need: 'write', cls: 'wired.batch',
      check: (ctx, body) => parseVariablesPatch(body),
      run: async ({ input, call, take }) => {
        if (input.entries.some(([, v]) => v === null)) return error(403, E.notAllowed);
        const limited = spendWrites(take, input.entries.length);
        if (limited) return limited;
        const data = await call('patchGlobalProfile', { variables: Object.fromEntries(input.entries) }, PATCH_CODES);
        return ok({ variables: variableMap(data?.variables) });
      },
    }),
  };
};

export const handlers = createVarsHandlers();
