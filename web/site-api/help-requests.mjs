import { helpTicketsOf } from '../lib/site-access.mjs';
import { signedIn } from '../cms/lib/session.mjs';
import { sql, unixNow } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';
import { SECONDS_PER_DAY } from '../lib/time.mjs';

export const owner = 'help';
export const grants = [
  { table: 'help_requests', select: ['id', 'player_id', 'topic', 'message', 'created_at', 'state', 'closed_at'],
    insert: ['player_id', 'name', 'contact', 'topic', 'message', 'ip', 'created_at'] },
  { table: 'help_request_answers', select: ['id', 'request_id', 'message', 'created_at'] },
];

export const TOPICS = Object.freeze([
  { key: 'account', label: 'My account or logging in' },
  { key: 'bug', label: 'Something does not work' },
  { key: 'safety', label: 'Safety, or trouble with another player' },
  { key: 'idea', label: 'A question or an idea' },
  { key: 'other', label: 'Something else' },
]);
const MESSAGE_MIN = 10;
const MESSAGE_MAX = 2000;
const NAME_MAX = 50;
const CONTACT_MAX = 254;
const OPEN_PER_PLAYER = 3;
const PER_ADDRESS_DAY = 10;
const MINE_ROWS = 20;
const SEND_LIMIT = ['help-request', 3, 3600];
const MAIL = /^[^\s@]{1,64}@[^\s@]{1,190}\.[^\s@]{2,63}$/;
const STATE = { 1: 'open', 2: 'picked', 0: 'closed' };

const playerId = async (ctx) => Number((await signedIn(ctx))?.userId ?? 0);

const text = (v, max) => {
  if (typeof v !== 'string') return { value: '' };
  const value = v.replace(/\r\n?/g, '\n').trim();
  if (/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/.test(value)) return { value, bad: 'control' };
  return value.length > max ? { value, bad: 'long' } : { value };
};

const may = (mode, userId) => {
  if (mode === 'off') return { canSend: false, reason: 'off' };
  if (mode === 'account' && !userId) return { canSend: false, reason: 'login' };
  return { canSend: true, reason: null };
};

const mine = async (ctx, userId) => {
  if (!userId) return [];
  const rows = [...await ctx.db.query(sql`SELECT id, topic, message, created_at, state, closed_at
    FROM help_requests WHERE player_id = ${userId} ORDER BY id DESC LIMIT ${MINE_ROWS}`)];
  const answers = new Map(rows.map((r) => [Number(r.id), []]));
  if (rows.length) {
    for (const a of await ctx.db.query(sql`SELECT request_id, message, created_at FROM help_request_answers
      WHERE request_id IN (${sql.in([...answers.keys()])}) ORDER BY id ASC`)) {
      answers.get(Number(a.request_id))?.push({ message: String(a.message ?? ''), created: Number(a.created_at) || 0 });
    }
  }
  return rows.map((r) => ({
    id: Number(r.id), topic: String(r.topic), message: String(r.message ?? ''), created: Number(r.created_at) || 0,
    state: STATE[Number(r.state)] ?? 'open', closed: Number(r.closed_at) || 0, answers: answers.get(Number(r.id)),
  }));
};

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/help-requests`,
    handler: async (req, res, ctx) => {
      const [mode, userId] = await Promise.all([helpTicketsOf(ctx.access), playerId(ctx)]);
      return ctx.send.json(res, 200, {
        ok: true, mode, ...may(mode, userId), guest: !userId,
        topics: TOPICS, limits: { message: [MESSAGE_MIN, MESSAGE_MAX], name: NAME_MAX, contact: CONTACT_MAX, open: OPEN_PER_PLAYER },
        mine: await mine(ctx, userId),
      });
    },
  },
  {
    method: 'POST', path: `${SITE_API}/help-requests`, write: true, maxBody: 8192,
    handler: async (req, res, ctx) => {
      const [mode, userId] = await Promise.all([helpTicketsOf(ctx.access), playerId(ctx)]);
      const { canSend, reason } = may(mode, userId);
      if (!canSend) {
        return reason === 'login'
          ? ctx.send.error(res, 401, 'Log in first', 'Log in to send a question to staff.', { reason: 'login-required' })
          : ctx.send.error(res, 403, 'Not available', 'Questions to staff cannot be sent from the website.', { reason: 'off' });
      }
      const refuse = (field, message) => ctx.send.error(res, 400, 'Not sent', message, { field });
      const topic = TOPICS.find((t) => t.key === ctx.body.topic)?.key;
      if (!topic) return refuse('topic', 'Pick what your question is about.');
      const message = text(ctx.body.message, MESSAGE_MAX);
      if (message.bad === 'control') return refuse('message', 'The message has characters in it that cannot be sent.');
      if (message.bad === 'long') return refuse('message', `A message is at most ${MESSAGE_MAX} characters; this one has ${message.value.length}.`);
      if (message.value.length < MESSAGE_MIN) return refuse('message', `Tell staff a little more: at least ${MESSAGE_MIN} characters.`);
      let name = '';
      let contact = '';
      if (!userId) {
        const n = text(ctx.body.name, NAME_MAX);
        if (n.bad || n.value.includes('\n')) return refuse('name', `A name is one line of at most ${NAME_MAX} characters.`);
        const c = text(ctx.body.contact, CONTACT_MAX);
        if (c.bad || !MAIL.test(c.value)) return refuse('contact', 'Give an e-mail address, so staff can answer you.');
        [name, contact] = [n.value, c.value];
      }
      if (!ctx.rateLimit(res, ...SEND_LIMIT)) return;
      const now = unixNow();
      const [[open], [day]] = await Promise.all([
        userId ? ctx.db.query(sql`SELECT COUNT(*) AS n FROM help_requests WHERE player_id = ${userId} AND state <> 0`) : [{ n: 0 }],
        ctx.db.query(sql`SELECT COUNT(*) AS n FROM help_requests WHERE ip = ${ctx.ip} AND created_at > ${now - SECONDS_PER_DAY}`),
      ]);
      if (Number(open?.n) >= OPEN_PER_PLAYER) {
        return ctx.send.error(res, 429, 'Wait for an answer', `You have ${OPEN_PER_PLAYER} questions waiting for staff. Send a new one when one of them is answered.`, { reason: 'open' });
      }
      if (Number(day?.n) >= PER_ADDRESS_DAY) {
        return ctx.send.error(res, 429, 'Slow down', 'Too many questions came from your connection today. Try again tomorrow.', { reason: 'day' });
      }
      const r = await ctx.db.insert(sql`INSERT INTO help_requests (player_id, name, contact, topic, message, ip, created_at)
        VALUES (${userId || null}, ${name}, ${contact}, ${topic}, ${message.value}, ${ctx.ip.slice(0, 45)}, ${now})`);
      return ctx.send.json(res, 201, { ok: true, id: Number(r?.insertId ?? 0) });
    },
  },
];
