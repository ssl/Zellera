import { fmtNumber } from '../cms/lib/html.mjs';
import { COMMAND_GRANT, walletCurrencies } from '../cms/lib/commands.mjs';
import { DbRefused } from '../cms/lib/db.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { gameRules } from '../lib/game-rules.mjs';
import { audit } from './_guard.mjs';

export const owner = 'hk-actions';

export const grants = [
  { table: 'players', select: ['id', 'name', 'rank_id'] },
  { table: 'ranks', select: ['id', 'name'] },
  { table: 'permissions', select: ['code', 'description'] },
  { table: 'rank_permissions', select: ['rank_id', 'permission_code'] },
  { table: 'player_badges', select: ['player_id', 'badge_code', 'slot'] },
  COMMAND_GRANT,
];

export const CURRENCY_PERM = 'housekeeping.wallet';
export const BADGE_PERM = 'housekeeping.badges';
const RANK_PERM = 'housekeeping.ranks';
const SUPER_USER = 9;
const BALANCE_MAX = 2147483647;
export const BADGE_CODE = /^[A-Za-z0-9_]{1,32}$/;
const EDIT_LIMIT = ['hk-edit', 60, 3600];

const loadTarget = async (ctx, id) => {
  if (!id) return null;
  const [u] = await ctx.db.query(sql`SELECT u.id, u.name AS username, u.rank_id AS \`rank\`, r.name AS rank_name
    FROM players u LEFT JOIN ranks r ON r.id = u.rank_id WHERE u.id = ${id} LIMIT 1`);
  return u ?? null;
};

export const editRefusal = (staff, target) => {
  if (Number(target.id) === staff.id) return 'This is your own account: another staff member changes its wallet and badges.';
  if (Number(target.rank) >= staff.rank) {
    return `${target.username} has the rank ${target.rank_name ?? target.rank}, which is not below yours (${staff.rankName}).`;
  }
  return null;
};

export const balance = (value) => {
  const s = String(value ?? '').trim();
  if (!/^\d{1,10}$/.test(s)) return null;
  const n = Number(s);
  return n <= BALANCE_MAX ? n : null;
};

export const giveRefusal = (code) => (BADGE_CODE.test(code) ? null : 'A badge code is 1 to 32 letters, digits or _ (like ADM or HC1).');

const ownedBadge = async (ctx, userId, code) => {
  const [b] = await ctx.db.query(sql`SELECT badge_code, slot AS slot_id FROM player_badges WHERE player_id = ${userId} AND badge_code = ${code} LIMIT 1`);
  return b ?? null;
};

export const commandOf = (r) => ({
  id: Number(r.id), kind: String(r.kind), status: String(r.status), result: r.result != null ? String(r.result) : null,
  processedAt: Number(r.processed_at) > 0 && r.status !== 'pending' ? Number(r.processed_at) : 0,
});

const send = async (ctx, res, target, sends) => {
  const ids = [];
  for (const s of sends) {
    let id;
    try {
      id = await ctx.command(s.type, s.payload, { userId: Number(target.id) });
    } catch (e) {
      await audit(ctx, { action: s.action, targetId: Number(target.id), label: String(target.username), detail: `not sent: ${s.detail}`, success: false });
      if (e instanceof DbRefused) {
        return ctx.send.error(res, 503, 'Not sent', `${ids.length ? 'Only the first changes were sent: ' : ''}this server can not send changes to the game right now.`);
      }
      throw e;
    }
    ids.push(id);
    await audit(ctx, { action: s.action, targetId: Number(target.id), label: String(target.username), detail: `${s.detail}, command #${id}` });
  }
  const rows = await ctx.waitForCommands(ids, { userId: Number(target.id) });
  const byId = new Map([...(rows ?? [])].map((r) => [Number(r.id), r]));
  return ctx.send.json(res, 200, {
    ok: true,
    commands: ids.map((id) => commandOf(byId.get(id) ?? { id, kind: sends[ids.indexOf(id)].type, status: 'pending' })),
  });
};

const start = async (ctx, res, action) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return null;
  const target = await loadTarget(ctx, intParam(ctx.params.id));
  if (!target || Number(target.id) <= 0) {
    ctx.send.notFound(res, 'No user has that id.');
    return null;
  }
  const refusal = editRefusal(ctx.staff, target);
  if (refusal) {
    await audit(ctx, { action, targetId: Number(target.id), label: String(target.username), detail: `refused: ${refusal}`, success: false });
    ctx.send.error(res, 403, 'Not for your rank', refusal);
    return null;
  }
  return target;
};

const walletPost = async (req, res, ctx) => {
  const target = await start(ctx, res, 'hk.set.wallet');
  if (!target) return;
  const errors = [];
  const sends = [];
  for (const c of walletCurrencies(gameRules(), ctx.texts)) {
    const value = balance(ctx.form.get(c.key));
    const was = balance(ctx.form.get(`was_${c.key}`));
    if (value == null) errors.push(`${c.label}: a whole number from 0 to ${fmtNumber(BALANCE_MAX)}.`);
    else if (value !== was) {
      sends.push({
        type: 'setCurrency', payload: { currency: c.type, amount: value }, action: `hk.set.${c.key}`,
        detail: `${c.key} to ${value}${was == null ? '' : ` (the page showed ${was})`}`,
      });
    }
  }
  if (errors.length) return ctx.send.error(res, 400, 'Not sent', errors.join(' '));
  if (sends.length === 0) return ctx.send.json(res, 200, { ok: true, unchanged: true, commands: [], message: 'Nothing was sent: the form had the balances it showed.' });
  return send(ctx, res, target, sends);
};

const giveBadgePost = async (req, res, ctx) => {
  const target = await start(ctx, res, 'hk.give.badge');
  if (!target) return;
  const code = String(ctx.form.get('badge') ?? '').trim();
  const refusal = giveRefusal(code);
  if (refusal) return ctx.send.error(res, 400, 'Not given', refusal, { field: 'badge' });
  const had = await ownedBadge(ctx, Number(target.id), code);
  if (had) return ctx.send.error(res, 409, 'Not given', `${target.username} has the badge ${had.badge_code} already.`, { field: 'badge' });
  return send(ctx, res, target, [{ type: 'giveBadge', payload: { badge: code }, action: 'hk.give.badge', detail: `badge ${code}` }]);
};

const takeBadgePost = async (req, res, ctx) => {
  const target = await start(ctx, res, 'hk.take.badge');
  if (!target) return;
  const code = String(ctx.form.get('badge') ?? '').trim();
  if (!BADGE_CODE.test(code)) return ctx.send.error(res, 400, 'Not taken', 'A badge code is 1 to 32 letters, digits or _.');
  const had = await ownedBadge(ctx, Number(target.id), code);
  if (!had) return ctx.send.error(res, 409, 'Not taken', `${target.username} does not have the badge ${code}.`);
  const worn = Number(had.slot_id) > 0 ? ` (worn in slot ${Number(had.slot_id)})` : '';
  return send(ctx, res, target, [{ type: 'takeBadge', payload: { badge: String(had.badge_code) }, action: 'hk.take.badge', detail: `badge ${had.badge_code}${worn}` }]);
};

export const rankRefusal = (staff, target) => {
  if (staff.rank < SUPER_USER || !staff.can(RANK_PERM)) return 'Only a super user (level 9) changes levels.';
  if (Number(target.id) === staff.id) return 'Not your own account: another super user changes it.';
  return null;
};

const rankPost = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const target = await loadTarget(ctx, intParam(ctx.params.id));
  if (!target || Number(target.id) <= 0) return ctx.send.notFound(res, 'No user has that id.');
  const refusal = rankRefusal(ctx.staff, target);
  if (refusal) {
    await audit(ctx, { action: 'hk.set.rank', targetId: Number(target.id), label: String(target.username), detail: `refused: ${refusal}`, success: false });
    return ctx.send.error(res, 403, 'Not for your rank', refusal);
  }
  const levels = (await ctx.db.query(sql`SELECT id FROM ranks ORDER BY id`)).map((r) => Number(r.id));
  const level = Number(String(ctx.form.get('level') ?? '').trim());
  if (!/^\d{1,2}$/.test(String(ctx.form.get('level') ?? '').trim()) || !levels.includes(level)) {
    return ctx.send.error(res, 400, 'Not sent', `Pick a level from ${levels[0]} to ${levels.at(-1)}.`, { field: 'level' });
  }
  const before = Number(target.rank);
  if (before === level) {
    return ctx.send.json(res, 200, { ok: true, unchanged: true, commands: [], message: 'Nothing was sent: that was already the level.' });
  }
  const detail = `level ${before} -> ${level}`;
  return send(ctx, res, target, [{ type: 'setRank', payload: { level }, action: 'hk.set.rank', detail }]);
};

export const routes = [
  { method: 'POST', path: `${HK_API}/users/:id(\\d{1,10})/wallet`, perm: CURRENCY_PERM, handler: walletPost },
  { method: 'POST', path: `${HK_API}/users/:id(\\d{1,10})/badges/give`, perm: BADGE_PERM, handler: giveBadgePost },
  { method: 'POST', path: `${HK_API}/users/:id(\\d{1,10})/badges/take`, perm: BADGE_PERM, handler: takeBadgePost },
  { method: 'POST', path: `${HK_API}/users/:id(\\d{1,10})/rank`, perm: RANK_PERM, handler: rankPost },
];

const LEVEL_TEXT = Object.freeze({
  0: 'A member: everything a player has.',
  1: 'A member without video ads. We never play ads, so the same as a member.',
  2: 'A VIP (celebrity, special guest; not the club): enters full rooms and gets the VIP badge at this level (it stays theirs after). In the client they can type :furni, which opens the list of every furni in the room.',
  3: 'An Ambassador (helps other players): in public rooms kick, alert, mute (2 minutes to 72 hours) and unmute from the avatar menu; the ambassador chat bubble; the AMB badge and the Ambassador effect at this level only (taken away when they leave it). The levels above keep the powers.',
  4: 'An employee: staff commands in the client, every chat bubble, the ADM badge.',
  5: 'A moderator: rights in every room (the client treats 5+ as room controller), the mod tool and its commands.',
  6: 'Player support: what a moderator has.',
  7: 'Community: staff picks and the staff-only room categories.',
  8: 'An administrator: the game\'s settings, catalogue and reload commands, housekeeping wallet and badges.',
  9: 'A super user: everything, also the levels here and the shutdown.',
});

export const rankLevels = async (ctx) => {
  const [ranks, defs] = await Promise.all([
    ctx.db.query(sql`SELECT id, name FROM ranks ORDER BY id`),
    ctx.db.query(sql`SELECT p.code, p.description, rp.rank_id FROM permissions p
      LEFT JOIN rank_permissions rp ON rp.permission_code = p.code ORDER BY p.code`),
  ]);
  const given = new Map();
  for (const d of defs) {
    if (!given.has(d.code)) given.set(d.code, { code: d.code, description: d.description, on: new Set() });
    if (d.rank_id != null) given.get(d.code).on.add(Number(d.rank_id));
  }
  const keys = [...given.values()];
  const on = (d, id) => d.on.has(id);
  return ranks.map((r, i) => {
    const id = Number(r.id);
    const prev = i > 0 ? Number(ranks[i - 1].id) : null;
    return {
      id, name: String(r.name), text: LEVEL_TEXT[id] ?? '',
      keys: keys.filter((d) => on(d, id) && (prev == null || !on(d, prev)))
        .map((d) => ({ key: String(d.code), comment: String(d.description ?? '') })),
    };
  });
};
