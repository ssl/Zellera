import { intParam, likePrefix, sql } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';
import { charCount, findUser } from './actions-lib.mjs';
import { ref, userIdParam } from './users-shared.mjs';
import { SECONDS_PER_HOUR } from '../lib/time.mjs';

export const owner = 'hk-actions';

export const grants = [
  {
    table: 'suspicions',
    select: ['id', 'player_id', 'address', 'device', 'kind', 'weight', 'detail', 'created_at'],
    delete: true,
  },
  { table: 'players', select: ['id', 'name', 'rank_id'] },
  { table: 'settings', select: ['name', 'value'] },
];

const PERM = 'moderation.player.ban';
const PER_PAGE = 100;
const MAX_PAGE = 1000;
const WINDOW = 'security.suspicion.window_hours';
const SCORE = 'security.shadow_mute.score';

const limits = async (ctx) => {
  const rows = await ctx.db.query(sql`SELECT name, value FROM settings WHERE name IN (${sql.in([WINDOW, SCORE])})`);
  const of = (name, dflt) => {
    const n = Number(rows.find((r) => r.name === name)?.value);
    return Number.isFinite(n) && n >= 0 ? n : dflt;
  };
  return { windowHours: of(WINDOW, 24), score: of(SCORE, 10) };
};

const list = async (req, res, ctx) => {
  const qp = ctx.query;
  const input = {
    player: String(qp.get('player') ?? '').trim(),
    kind: String(qp.get('kind') ?? '').trim(),
    address: String(qp.get('address') ?? '').trim(),
  };
  const errors = [];
  if (charCount(input.player) > 50) errors.push('That player is too long.');
  if (charCount(input.kind) > 40) errors.push('Kinds are at most 40 characters.');
  if (charCount(input.address) > 45) errors.push('That address is too long.');
  const pageNo = intParam(qp.get('page'), { max: MAX_PAGE }) ?? 1;

  const where = [];
  if (!errors.length && input.player) {
    const u = await findUser(ctx, input.player.replace(/^#/, ''));
    if (u) where.push(sql`s.player_id = ${Number(u.id)}`);
    else errors.push(`No user is called “${input.player}”.`);
  }
  if (!errors.length && input.kind) where.push(sql`s.kind LIKE ${likePrefix(input.kind)}`);
  if (!errors.length && input.address) where.push(sql`s.address = ${input.address}`);
  if (errors.length) return ctx.send.error(res, 400, 'Bad filter', errors.join(' '), { errors });
  const filter = where.length ? sql`WHERE ${sql.join(where, ' AND ')}` : sql``;

  let rows = await ctx.db.query(sql`SELECT s.id, s.player_id, p.name AS player_name, s.address, s.device, s.kind, s.weight, s.detail, s.created_at
    FROM suspicions s LEFT JOIN players p ON p.id = s.player_id ${filter}
    ORDER BY s.id DESC LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`);
  const hasNext = rows.length > PER_PAGE;
  rows = rows.slice(0, PER_PAGE);

  const { windowHours, score } = await limits(ctx);
  const ids = [...new Set(rows.map((r) => Number(r.player_id)).filter((id) => id > 0))];
  const sums = ids.length ? await ctx.db.query(sql`SELECT s.player_id, p.name AS player_name, SUM(s.weight) AS score
    FROM suspicions s LEFT JOIN players p ON p.id = s.player_id
    WHERE s.player_id IN (${sql.in(ids)}) AND s.created_at >= UNIX_TIMESTAMP() - ${Math.round(windowHours * SECONDS_PER_HOUR)}
    GROUP BY s.player_id, p.name`) : [];
  const scored = new Map([...sums].map((r) => [Number(r.player_id), r]));
  const names = new Map(rows.map((r) => [Number(r.player_id), r.player_name]));
  const summary = ids.map((id) => {
    const total = Number(scored.get(id)?.score) || 0;
    return { player: ref(id, scored.get(id)?.player_name ?? names.get(id)), score: total, shadowMuted: score > 0 && total >= score };
  }).sort((a, b) => b.score - a.score || b.player.id - a.player.id);

  return ctx.send.json(res, 200, {
    ok: true, filter: input, page: pageNo, hasNext, windowHours, shadowMuteScore: score, summary,
    rows: rows.map((r) => ({
      id: Number(r.id), at: Number(r.created_at), player: ref(r.player_id, r.player_name), kind: String(r.kind ?? ''),
      weight: Number(r.weight) || 0, address: String(r.address ?? ''), device: String(r.device ?? ''), detail: String(r.detail ?? ''),
    })),
  });
};

const clear = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, 'hk-action', 30, 3600)) return;
  const id = userIdParam(ctx.params.id);
  if (!id) return ctx.send.notFound(res, 'No user has that id.');
  const [u] = await ctx.db.query(sql`SELECT id, name FROM players WHERE id = ${id} LIMIT 1`);
  const target = { targetId: id, label: u ? String(u.name) : `User #${id}` };
  let result;
  try {
    result = await ctx.db.remove(sql`DELETE FROM suspicions WHERE player_id = ${id}`);
  } catch (e) {
    await audit(ctx, { action: 'hk.suspicions.clear', ...target, detail: 'failed', success: false });
    throw e;
  }
  const n = Number(result?.affectedRows) || 0;
  await audit(ctx, { action: 'hk.suspicions.clear', ...target, detail: `deleted ${n} suspicion rows` });
  return ctx.send.json(res, 200, { ok: true, deleted: n, message: `${n} suspicion ${n === 1 ? 'row' : 'rows'} of ${target.label} deleted.` });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/suspicions`, perm: PERM, handler: list },
  { method: 'POST', path: `${HK_API}/suspicions/:id(\\d{1,10})/clear`, perm: PERM, handler: clear },
];
