import { intParam, likePrefix, sql } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { charCount, findUser } from './actions-lib.mjs';
import { logRow, ref } from './users-shared.mjs';

export const owner = 'hk-actions';

export const grants = [
  {
    table: 'staff_actions',
    select: ['id', 'acted_at', 'staff_id', 'action', 'subject_type', 'subject_id', 'subject_label', 'detail', 'success'],
  },
  {
    table: 'rank_changes',
    select: ['id', 'changed_at', 'actor_id', 'player_id', 'old_rank', 'new_rank', 'via'],
  },
  { table: 'players', select: ['id', 'name', 'rank_id'] },
  { table: 'ranks', select: ['id', 'name'] },
];

const PERM = 'moderation.player.ban';
const PER_PAGE = 100;
const MAX_PAGE = 1000;
const handler = async (req, res, ctx) => {
  const qp = ctx.query;
  const tab = qp.get('tab') === 'permissions' ? 'permissions' : 'log';
  const input = {
    actor: String(qp.get('actor') ?? '').trim(),
    target: String(qp.get('target') ?? '').trim(),
    action: String(qp.get('action') ?? '').trim(),
  };
  const warnings = [];
  const errors = [];
  if (charCount(input.actor) > 50) errors.push('That staff name is too long.');
  if (charCount(input.target) > 50) errors.push('That target is too long.');
  if (charCount(input.action) > 64) errors.push('Actions are at most 64 characters.');
  const pageNo = intParam(qp.get('page'), { max: MAX_PAGE }) ?? 1;

  const col = tab === 'log'
    ? { actor: sql`a.staff_id`, actorName: sql`s.name`, type: sql`a.subject_type`, id: sql`a.subject_id`, label: sql`a.subject_label` }
    : { actor: sql`a.actor_id`, actorName: sql`a.actor_name`, type: sql`a.target_type`, id: sql`a.target_id`, label: sql`a.target_label` };
  const where = [];
  if (!errors.length && input.actor) {
    const id = intParam(input.actor);
    if (id) where.push(sql`${col.actor} = ${id}`);
    else {
      const u = await findUser(ctx, input.actor);
      where.push(u ? sql`(${col.actor} = ${Number(u.id)} OR ${col.actorName} = ${input.actor})` : sql`${col.actorName} = ${input.actor}`);
    }
  }
  if (!errors.length && input.target) {
    const id = intParam(input.target.replace(/^#/, ''));
    if (id) where.push(sql`${col.id} = ${id}`);
    else {
      const u = await findUser(ctx, input.target);
      if (u) where.push(sql`${col.type} = 'user' AND ${col.id} = ${Number(u.id)}`);
      else if (tab === 'log') { where.push(sql`${col.label} = ${input.target}`); warnings.push(`No user is called “${input.target}” now; showing rows that name it.`); }
      else errors.push(`No user is called “${input.target}”.`);
    }
  }
  if (!errors.length && input.action) where.push(sql`a.action LIKE ${likePrefix(input.action)}`);
  const filter = where.length ? sql`WHERE ${sql.join(where, ' AND ')}` : sql``;
  const limit = sql`LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`;

  let rows = [];
  if (!errors.length) {
    rows = tab === 'log'
      ? await ctx.db.query(sql`SELECT a.id, a.acted_at, a.staff_id, s.name AS staff_name, a.action, a.subject_type, a.subject_id, a.subject_label,
            a.detail, a.success
          FROM staff_actions a LEFT JOIN players s ON s.id = a.staff_id ${filter} ORDER BY a.id DESC ${limit}`)
      : await ctx.db.query(sql`SELECT a.id, a.changed_at, a.actor_id, a.actor_name, a.action, a.target_type, a.target_id,
            a.subject, a.old_value, a.new_value, a.via, u.name AS target_name, r.name AS target_rank
          FROM (SELECT c.id, c.changed_at, c.actor_id, act.name AS actor_name, 'rank.change' AS action, 'user' AS target_type,
              c.player_id AS target_id, 'rank' AS subject, c.old_rank AS old_value, c.new_rank AS new_value, c.via
            FROM rank_changes c LEFT JOIN players act ON act.id = c.actor_id) a
          LEFT JOIN players u ON a.target_type = 'user' AND u.id = a.target_id
          LEFT JOIN ranks r ON a.target_type = 'rank' AND r.id = a.target_id
          ${filter} ORDER BY a.id DESC ${limit}`);
  }
  const hasNext = rows.length > PER_PAGE;
  rows = rows.slice(0, PER_PAGE);

  if (errors.length) return ctx.send.error(res, 400, 'Bad filter', errors.join(' '), { errors });
  const permRow = (a) => ({
    id: Number(a.id), at: Number(a.changed_at), actor: ref(a.actor_id, a.actor_name || (Number(a.actor_id) > 0 ? null : 'system')),
    action: String(a.action ?? ''), subject: String(a.subject ?? ''), old: String(a.old_value ?? ''), new: String(a.new_value ?? ''), via: String(a.via ?? ''),
    target: {
      type: String(a.target_type ?? ''), id: Number(a.target_id) || 0,
      label: a.target_type === 'rank' ? (a.target_rank != null ? String(a.target_rank) : null) : (a.target_name != null ? String(a.target_name) : null),
    },
  });
  return ctx.send.json(res, 200, {
    ok: true, tab, filter: input, warnings, page: pageNo, hasNext, rows: rows.map(tab === 'log' ? logRow : permRow),
  });
};

export const routes = [
  { method: 'GET', path: `${HK_API}/audit`, perm: PERM, handler },
];
