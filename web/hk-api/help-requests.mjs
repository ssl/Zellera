import { excerpt } from '../cms/lib/excerpt.mjs';
import { intParam, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { TOPICS } from '../site-api/help-requests.mjs';
import { audit } from './_guard.mjs';
import { USER_GRANT, loadUsers, peopleOf } from './rooms-shared.mjs';

export const owner = 'hk-help';

const PERM = 'moderation.tickets';
const PER_PAGE = 50;
const ANSWER_MAX = 2000;

export const grants = [
  USER_GRANT,
  {
    table: 'help_requests',
    select: ['id', 'player_id', 'name', 'contact', 'topic', 'message', 'ip', 'created_at', 'state', 'staff_id', 'closed_at'],
    update: ['state', 'staff_id', 'closed_at'],
  },
  {
    table: 'help_request_answers',
    select: ['id', 'request_id', 'staff_id', 'message', 'created_at'],
    insert: ['request_id', 'staff_id', 'message', 'created_at'],
  },
];

const STATES = Object.freeze([
  { key: 'open', value: 1, label: 'Open' },
  { key: 'picked', value: 2, label: 'Picked' },
  { key: 'closed', value: 0, label: 'Closed' },
]);
const stateOf = (v) => STATES.find((s) => s.value === Number(v)) ?? STATES[0];
const topicOf = (key) => ({ key: String(key), label: TOPICS.find((t) => t.key === key)?.label ?? String(key) });
const EXCERPT = 140;

const rowJson = (r, answers = null) => ({
  id: Number(r.id), state: { key: stateOf(r.state).key, label: stateOf(r.state).label }, topic: topicOf(r.topic),
  playerId: Number(r.player_id ?? 0), guestName: String(r.name ?? ''), staffId: Number(r.staff_id ?? 0),
  created: Number(r.created_at) || 0, answered: Number(r.answered_at) || 0, closed: Number(r.closed_at) || 0,
  ...(answers
    ? {
      message: String(r.message ?? ''), contact: String(r.contact ?? ''), ip: String(r.ip ?? ''),
      answers: answers.map((a) => ({ id: Number(a.id), staffId: Number(a.staff_id ?? 0), message: String(a.message ?? ''), created: Number(a.created_at) || 0 })),
    }
    : { excerpt: excerpt(r.message, EXCERPT, { atWord: false }) }),
});

const COLS = sql`id, player_id, name, contact, topic, message, ip, created_at, state, staff_id, closed_at,
  COALESCE((SELECT MAX(a.created_at) FROM help_request_answers a WHERE a.request_id = help_requests.id), 0) AS answered_at`;

const list = async (req, res, ctx) => {
  const state = STATES.find((s) => s.key === ctx.query.get('state')) ?? STATES[0];
  const pageNo = intParam(ctx.query.get('page'), { max: 10000 }) ?? 1;
  const counts = new Map((await ctx.db.query(sql`SELECT state, COUNT(id) AS n FROM help_requests GROUP BY state`))
    .map((r) => [Number(r.state), Number(r.n)]));
  const rows = [...await ctx.db.query(sql`SELECT ${COLS} FROM help_requests WHERE state = ${state.value}
    ORDER BY id ${state.value === 0 ? sql`DESC` : sql`ASC`} LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`)];
  const shown = rows.slice(0, PER_PAGE);
  const users = await loadUsers(ctx, shown.flatMap((r) => [r.player_id, r.staff_id]));
  return ctx.send.json(res, 200, {
    ok: true, state: state.key,
    states: STATES.map((s) => ({ key: s.key, label: s.label, count: counts.get(s.value) ?? 0 })),
    page: pageNo, hasNext: rows.length > PER_PAGE,
    requests: shown.map((r) => rowJson(r)), users: peopleOf(users),
  });
};

const one = async (ctx, id) => {
  const [r] = id ? await ctx.db.query(sql`SELECT ${COLS} FROM help_requests WHERE id = ${id} LIMIT 1`) : [];
  return r ?? null;
};

const answersOf = async (ctx, id) => [...await ctx.db.query(sql`SELECT id, staff_id, message, created_at FROM help_request_answers
  WHERE request_id = ${id} ORDER BY id ASC`)];

const full = async (ctx, res, r) => {
  const answers = await answersOf(ctx, Number(r.id));
  const users = await loadUsers(ctx, [r.player_id, r.staff_id, ...answers.map((a) => a.staff_id)]);
  return ctx.send.json(res, 200, { ok: true, request: rowJson(r, answers), users: peopleOf(users), limits: { answer: ANSWER_MAX } });
};

const detail = async (req, res, ctx) => {
  const r = await one(ctx, intParam(ctx.params.id));
  if (!r) return ctx.send.notFound(res, 'There is no help request with this id.');
  return full(ctx, res, r);
};

const change = async (req, res, ctx) => {
  const id = intParam(ctx.params.id);
  const r = await one(ctx, id);
  if (!r) return ctx.send.notFound(res, 'There is no help request with this id.');
  const action = String(ctx.body.action ?? '');
  const now = unixNow();
  const me = ctx.staff.id;
  const state = Number(r.state);
  let q;
  let answer = '';
  let detailText = '';
  if (action === 'pick' && state === 1) q = sql`UPDATE help_requests SET state = 2, staff_id = ${me} WHERE id = ${id} AND state = 1`;
  else if (action === 'release' && state === 2) q = sql`UPDATE help_requests SET state = 1 WHERE id = ${id} AND state = 2`;
  else if (action === 'close' && state !== 0) q = sql`UPDATE help_requests SET state = 0, staff_id = ${me}, closed_at = ${now} WHERE id = ${id} AND state <> 0`;
  else if (action === 'reopen' && state === 0) q = sql`UPDATE help_requests SET state = 1, closed_at = NULL WHERE id = ${id} AND state = 0`;
  else if (action === 'answer' && state !== 0) {
    answer = typeof ctx.body.answer === 'string' ? ctx.body.answer.replace(/\r\n?/g, '\n').trim() : '';
    if (!answer) return ctx.send.error(res, 400, 'Not saved', 'Write an answer first.', { field: 'answer' });
    if (answer.length > ANSWER_MAX) return ctx.send.error(res, 400, 'Not saved', `An answer is at most ${ANSWER_MAX} characters.`, { field: 'answer' });
    const close = ctx.body.close === true;
    q = close
      ? sql`UPDATE help_requests SET state = 0, staff_id = ${me}, closed_at = ${now} WHERE id = ${id} AND state <> 0`
      : sql`UPDATE help_requests SET state = 2, staff_id = ${me} WHERE id = ${id} AND state <> 0`;
    detailText = `${answer.length} characters${close ? ', closed' : ''}`;
  } else {
    return ctx.send.error(res, 409, 'Not changed', `This request is ${stateOf(state).label.toLowerCase()}, so it cannot be changed that way. Reload the page.`, { reason: 'state' });
  }
  const done = await ctx.db.write(q);
  if (!Number(done?.affectedRows)) {
    return ctx.send.error(res, 409, 'Not changed', 'Someone changed this request just now. Reload the page.', { reason: 'state' });
  }
  if (answer) {
    await ctx.db.write(sql`INSERT INTO help_request_answers (request_id, staff_id, message, created_at) VALUES (${id}, ${me}, ${answer}, ${now})`);
  }
  await audit(ctx, { action: `hk.help.${action}`, targetType: 'help', targetId: id, label: `#${id}`, detail: detailText });
  return full(ctx, res, await one(ctx, id));
};

const ID = ':id(\\d{1,10})';
export const routes = [
  { method: 'GET', path: `${HK_API}/help-requests`, perm: PERM, handler: list },
  { method: 'GET', path: `${HK_API}/help-requests/${ID}`, perm: PERM, handler: detail },
  { method: 'POST', path: `${HK_API}/help-requests/${ID}`, perm: PERM, handler: change },
];
