export const MAX_LENGTH = 200;
export const PAGE = 20;
const HK_PAGE = 50;
const MODERATE = 'moderation.tickets';
const WRITE_LIMITS = Object.freeze([['write', 3, 60], ['write-hour', 20, 3600]]);

const cleanMessage = (v) => String(v ?? '').replace(/[\p{Cc}\p{Zl}\p{Zp}]/gu, ' ').replace(/\s+/g, ' ').trim();
const length = (s) => [...s].length;

export default (ext) => {
  const { sql } = ext;
  const T = sql`ext_guestbook_entries`;

  const person = (p) => ({ id: p.id, name: p.name, figure: p.figure, gender: p.gender });

  const ownerFor = async (ctx, viewer) => {
    const owner = await ctx.players.byName(ctx.params.name);
    if (!owner || (owner.private && owner.id !== viewer.id && !viewer.staff)) {
      ctx.error(404, owner ? 'This guestbook is private.' : 'There is no such player.');
      return null;
    }
    return owner;
  };

  const writeRefusal = async (ctx, owner, viewer) => {
    if (!viewer.loggedIn) return 'login';
    if (await ctx.blocks.blocked(owner.id, viewer.id)) return 'blocked';
    return null;
  };

  const entry = (row, author, owner, viewer) => ({
    id: Number(row.id), author: person(author), message: String(row.message), created: Number(row.created_at),
    canDelete: viewer.loggedIn && (viewer.id === owner.id || viewer.id === author.id || viewer.can(MODERATE)),
  });

  ext.get('/entries/:name', { gate: 'players' }, async (ctx) => {
    if (!(await ctx.rateLimit('read', 120, 60))) return;
    const viewer = await ctx.viewer();
    const owner = await ownerFor(ctx, viewer);
    if (!owner) return;
    const before = /^\d{1,10}$/.test(ctx.query.get('before') ?? '') ? Number(ctx.query.get('before')) : 0;
    const rows = await ctx.db.query(sql`SELECT id, author_id, message, created_at FROM ${T}
      WHERE owner_id = ${owner.id} ${before ? sql`AND id < ${before}` : sql``} ORDER BY id DESC LIMIT ${PAGE + 1}`);
    const [{ n }] = await ctx.db.query(sql`SELECT COUNT(*) AS n FROM ${T} WHERE owner_id = ${owner.id}`);
    const authors = await ctx.players.byIds(rows.map((r) => r.author_id));
    const reason = await writeRefusal(ctx, owner, viewer);
    ctx.json(200, {
      owner: { id: owner.id, name: owner.name },
      total: Number(n),
      entries: rows.slice(0, PAGE).filter((r) => authors.has(Number(r.author_id)))
        .map((r) => entry(r, authors.get(Number(r.author_id)), owner, viewer)),
      more: rows.length > PAGE,
      canWrite: reason === null,
      reason,
      maxLength: MAX_LENGTH,
    });
  });

  ext.post('/entries/:name', { auth: 'user', maxBody: 2048 }, async (ctx) => {
    const viewer = await ctx.viewer();
    const message = cleanMessage(ctx.body.message);
    if (!message) return ctx.error(400, 'Write a message first.', { field: 'message' });
    if (length(message) > MAX_LENGTH) return ctx.error(400, `A message is at most ${MAX_LENGTH} characters.`, { field: 'message' });
    const owner = await ownerFor(ctx, viewer);
    if (!owner) return;
    if (await writeRefusal(ctx, owner, viewer) === 'blocked') {
      return ctx.error(403, `${owner.name} does not take messages from you.`, { reason: 'blocked' });
    }
    for (const [bucket, max, sec] of WRITE_LIMITS) if (!(await ctx.rateLimit(bucket, max, sec))) return;
    const filtered = await ctx.filter(message);
    const stored = cleanMessage(filtered.text);
    if (filtered.hidden || !stored) return ctx.error(400, 'This message cannot be posted.', { field: 'message', reason: 'filtered' });
    if (length(stored) > MAX_LENGTH) {
      return ctx.error(400, `With the word filter it is longer than ${MAX_LENGTH} characters.`, { field: 'message', reason: 'filtered' });
    }
    const r = await ctx.db.write(sql`INSERT INTO ${T} (owner_id, author_id, message, created_at)
      VALUES (${owner.id}, ${viewer.id}, ${stored}, UNIX_TIMESTAMP())`);
    const [row] = await ctx.db.query(sql`SELECT id, author_id, message, created_at FROM ${T} WHERE id = ${Number(r.insertId)}`);
    ctx.json(201, { entry: entry(row, viewer, owner, viewer) });
  });

  ext.post('/entries/:name/:id(\\d{1,10})/delete', { auth: 'user', maxBody: 256 }, async (ctx) => {
    if (!(await ctx.rateLimit('delete', 30, 60))) return;
    const viewer = await ctx.viewer();
    const owner = await ownerFor(ctx, viewer);
    if (!owner) return;
    const [row] = await ctx.db.query(sql`SELECT id, author_id FROM ${T} WHERE id = ${Number(ctx.params.id)} AND owner_id = ${owner.id}`);
    if (!row) return ctx.error(404, 'This message is gone.');
    if (viewer.id !== owner.id && viewer.id !== Number(row.author_id) && !viewer.can(MODERATE)) {
      return ctx.error(403, 'Only the profile\'s owner, the writer and staff delete a message.');
    }
    await ctx.db.write(sql`DELETE FROM ${T} WHERE id = ${Number(row.id)}`);
    ctx.json(200, { deleted: Number(row.id) });
  });

  ext.hk.get('/entries', { perm: MODERATE }, async (ctx) => {
    const page = /^\d{1,5}$/.test(ctx.query.get('page') ?? '') ? Math.max(1, Number(ctx.query.get('page'))) : 1;
    const rows = await ctx.db.query(sql`SELECT id, owner_id, author_id, message, created_at FROM ${T}
      ORDER BY id DESC LIMIT ${HK_PAGE + 1} OFFSET ${(page - 1) * HK_PAGE}`);
    const [{ n }] = await ctx.db.query(sql`SELECT COUNT(*) AS n FROM ${T}`);
    const people = await ctx.players.byIds(rows.flatMap((r) => [r.owner_id, r.author_id]));
    const who = (id) => (people.has(Number(id)) ? person(people.get(Number(id))) : { id: Number(id), name: null, figure: '', gender: 'M' });
    ctx.json(200, {
      page, total: Number(n), hasNext: rows.length > HK_PAGE,
      entries: rows.slice(0, HK_PAGE).map((r) => ({
        id: Number(r.id), owner: who(r.owner_id), author: who(r.author_id), message: String(r.message), created: Number(r.created_at),
      })),
    });
  });

  ext.hk.post('/entries/:id(\\d{1,10})/delete', { perm: MODERATE, maxBody: 256 }, async (ctx) => {
    const [row] = await ctx.db.query(sql`SELECT id, owner_id, author_id, message FROM ${T} WHERE id = ${Number(ctx.params.id)}`);
    if (!row) return ctx.error(404, 'This message is gone.');
    await ctx.db.write(sql`DELETE FROM ${T} WHERE id = ${Number(row.id)}`);
    const owner = await ctx.players.byId(row.owner_id);
    await ctx.audit({
      action: 'delete', targetType: 'user', targetId: Number(row.owner_id), label: owner?.name ?? '',
      detail: `entry ${row.id} by player ${row.author_id}: ${String(row.message).slice(0, 120)}`,
    });
    ctx.json(200, { deleted: Number(row.id) });
  });
};
