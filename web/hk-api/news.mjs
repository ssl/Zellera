import { DbRefused } from '../cms/lib/db.mjs';
import { safeUrl } from '../cms/lib/html.mjs';
import { intParam, sql, unixNow } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';

export const owner = 'hk-content';

const PERM = 'housekeeping.news';
const READ = ['id', 'title', 'body', 'link_text', 'link_url', 'enabled', 'sort_order', 'created_at'];
const WRITE = READ.filter((c) => c !== 'id');
export const grants = [
  { table: 'news_panels', select: READ, insert: WRITE, update: WRITE, delete: true },
];

const BASE = `${HK_API}/news`;
const EDIT_LIMIT = ['hk-news', 60, 3600];
// The text counts in bytes: with every other field at its longest, a request still fits the 16384 bytes a POST may have.
export const LIMITS = Object.freeze({ title: 150, bodyBytes: 6000, linkText: 80, link: 255 });
const SITE_PATH = /^\/[^\s"'<>\\]*$/;
const CONTROL = /[\u0000-\u0008\u000b-\u001f\u007f]/;

const lines = (s) => String(s ?? '').replace(/\r\n?/g, '\n').trim();
const refuse = (field, message) => ({ field, message });

const flagOf = (v) => (v === 'true' ? true : v === 'false' ? false : null);

export const articleRefusal = (a) => {
  const title = lines(a.title);
  const body = lines(a.body);
  const linkText = lines(a.link_text);
  const link = String(a.link_url ?? '').trim();
  if (!title) return refuse('title', 'An article needs a title.');
  if (title.includes('\n') || CONTROL.test(title)) return refuse('title', 'The title is one line.');
  if (title.length > LIMITS.title) return refuse('title', `The title is at most ${LIMITS.title} characters.`);
  if (!body) return refuse('body', 'An article needs a text.');
  if (CONTROL.test(body)) return refuse('body', 'The text is plain text: letters, digits, signs and line breaks.');
  const bytes = Buffer.byteLength(body, 'utf8');
  if (bytes > LIMITS.bodyBytes) return refuse('body', `The text is at most ${LIMITS.bodyBytes} bytes; this one has ${bytes}.`);
  if (link) {
    if (link.length > LIMITS.link || !SITE_PATH.test(link) || safeUrl(link) === '#') {
      return refuse('link_url', 'The link is a path on this site that starts with /, like /help (no spaces or quotes).');
    }
    if (linkText.includes('\n') || CONTROL.test(linkText) || linkText.length > LIMITS.linkText) {
      return refuse('link_text', `The link text is one line of at most ${LIMITS.linkText} characters.`);
    }
  }
  if (flagOf(a.shown) == null) return refuse('shown', 'Say whether the article is shown: true or false.');
  return null;
};

export const cleanArticle = (a) => {
  const link = String(a.link_url ?? '').trim();
  return { title: lines(a.title), body: lines(a.body), link_text: link ? lines(a.link_text) : '', link_url: link, shown: flagOf(a.shown) === true };
};

const FIELDS = ['title', 'body', 'link_text', 'link_url', 'shown'];
const formArticle = (ctx) => Object.fromEntries(FIELDS.map((k) => [k, String(ctx.form.get(k) ?? '')]));

const listArticles = (ctx) => ctx.db.query(sql`SELECT id, title, link_text, link_url, enabled, sort_order, created_at
  FROM news_panels ORDER BY sort_order ASC, id DESC`);

const loadArticle = async (ctx, id) => {
  if (!id) return null;
  const [a] = await ctx.db.query(sql`SELECT id, title, body, link_text, link_url, enabled, sort_order, created_at
    FROM news_panels WHERE id = ${id} LIMIT 1`);
  return a ?? null;
};

const rowJson = (a) => ({
  id: Number(a.id), title: String(a.title ?? ''), link_text: String(a.link_text ?? ''), link_url: String(a.link_url ?? ''),
  shown: Number(a.enabled) === 1, published: Number(a.created_at) || 0,
});
const articleJson = (a) => ({ ...rowJson(a), body: String(a.body ?? '') });

const listAnswer = async (ctx) => ({ ok: true, articles: (await listArticles(ctx)).map(rowJson) });

const listPage = async (req, res, ctx) => ctx.send.json(res, 200, await listAnswer(ctx));

const addPage = (req, res, ctx) => ctx.send.json(res, 200, { ok: true, article: null, limits: LIMITS });

const editPage = async (req, res, ctx) => {
  const a = await loadArticle(ctx, intParam(ctx.params.id));
  if (!a) return ctx.send.notFound(res, 'No article has that id.');
  return ctx.send.json(res, 200, { ok: true, article: articleJson(a), limits: LIMITS });
};

const writing = async (ctx, res, fn) => {
  try {
    await fn();
  } catch (e) {
    if (e instanceof DbRefused) {
      ctx.send.error(res, 503, 'Not saved', 'This server can not save changes right now.');
      return false;
    }
    throw e;
  }
  return true;
};

// An article that was never shown has no date; it gets one when it is shown for the first time.
const dateFor = (old, shown) => {
  const had = Number(old?.created_at) || 0;
  return had || !shown ? had : unixNow();
};

const save = async (req, res, ctx, id) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const old = id == null ? null : await loadArticle(ctx, intParam(id));
  if (id != null && !old) return ctx.send.notFound(res, 'No article has that id.');
  const sent = formArticle(ctx);
  const refusal = articleRefusal(sent);
  if (refusal) return ctx.send.error(res, 400, 'Not saved', refusal.message, { field: refusal.field });
  const a = cleanArticle(sent);
  const created = dateFor(old, a.shown);
  let newId = old ? Number(old.id) : 0;
  const ok = await writing(ctx, res, async () => {
    if (old) {
      await ctx.db.write(sql`UPDATE news_panels SET title = ${a.title}, body = ${a.body}, link_text = ${a.link_text},
        link_url = ${a.link_url}, enabled = ${a.shown ? 1 : 0}, created_at = ${created} WHERE id = ${newId}`);
    } else {
      const [m] = await ctx.db.query(sql`SELECT COALESCE(MIN(sort_order), 1) AS n FROM news_panels`);
      const r = await ctx.db.write(sql`INSERT INTO news_panels (title, body, link_text, link_url, enabled, sort_order, created_at)
        VALUES (${a.title}, ${a.body}, ${a.link_text}, ${a.link_url}, ${a.shown ? 1 : 0}, ${Number(m?.n ?? 1) - 1}, ${created})`);
      newId = Number(r?.insertId) || 0;
    }
  });
  if (!ok) return;
  await audit(ctx, {
    action: old ? 'hk.news.edit' : 'hk.news.add', targetType: 'news', targetId: newId, label: a.title,
    detail: `${a.shown ? 'shown' : 'hidden'} ${a.link_url}`.trim(),
  });
  return ctx.send.json(res, old ? 200 : 201, {
    ok: true, article: articleJson({ ...a, id: newId, enabled: a.shown ? 1 : 0, created_at: created }),
  });
};

const create = (req, res, ctx) => save(req, res, ctx, null);
const update = (req, res, ctx) => save(req, res, ctx, ctx.params.id);

const show = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const shown = flagOf(ctx.form.get('shown'));
  if (shown == null) return ctx.send.error(res, 400, 'Bad request', 'Say whether the article is shown: true or false.', { field: 'shown' });
  const a = await loadArticle(ctx, intParam(ctx.params.id));
  if (!a) return ctx.send.notFound(res, 'No article has that id.');
  if ((Number(a.enabled) === 1) !== shown) {
    const ok = await writing(ctx, res, () => ctx.db.write(sql`UPDATE news_panels SET enabled = ${shown ? 1 : 0},
      created_at = ${dateFor(a, shown)} WHERE id = ${Number(a.id)}`));
    if (!ok) return;
    await audit(ctx, { action: shown ? 'hk.news.show' : 'hk.news.hide', targetType: 'news', targetId: Number(a.id), label: String(a.title) });
  }
  return ctx.send.json(res, 200, await listAnswer(ctx));
};

const move = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const id = intParam(ctx.params.id);
  const direction = ctx.form.get('direction');
  if (direction !== 'up' && direction !== 'down') return ctx.send.error(res, 400, 'Bad request', 'Move it up or down.', { field: 'direction' });
  const rows = await listArticles(ctx);
  const i = rows.findIndex((r) => Number(r.id) === id);
  if (i < 0) return ctx.send.notFound(res, 'No article has that id.');
  const j = direction === 'up' ? i - 1 : i + 1;
  if (j >= 0 && j < rows.length) {
    [rows[i], rows[j]] = [rows[j], rows[i]];
    const ok = await writing(ctx, res, async () => {
      for (const [n, r] of rows.entries()) {
        if (Number(r.sort_order) !== n + 1) await ctx.db.write(sql`UPDATE news_panels SET sort_order = ${n + 1} WHERE id = ${Number(r.id)}`);
      }
    });
    if (!ok) return;
    await audit(ctx, { action: 'hk.news.move', targetType: 'news', targetId: id, label: String(rows[j].title), detail: `${direction}: now ${j + 1} of ${rows.length}` });
  }
  return ctx.send.json(res, 200, await listAnswer(ctx));
};

const remove = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const a = await loadArticle(ctx, intParam(ctx.params.id));
  if (!a) return ctx.send.notFound(res, 'No article has that id.');
  const ok = await writing(ctx, res, () => ctx.db.remove(sql`DELETE FROM news_panels WHERE id = ${Number(a.id)}`));
  if (!ok) return;
  await audit(ctx, { action: 'hk.news.delete', targetType: 'news', targetId: Number(a.id), label: String(a.title) });
  return ctx.send.json(res, 200, await listAnswer(ctx));
};

const ID = ':id(\\d{1,10})';
export const routes = [
  { method: 'GET', path: BASE, perm: PERM, handler: listPage },
  { method: 'GET', path: `${BASE}/new`, perm: PERM, handler: addPage },
  { method: 'GET', path: `${BASE}/${ID}`, perm: PERM, handler: editPage },
  { method: 'POST', path: BASE, perm: PERM, handler: create },
  { method: 'POST', path: `${BASE}/${ID}`, perm: PERM, handler: update },
  { method: 'POST', path: `${BASE}/${ID}/show`, perm: PERM, handler: show },
  { method: 'POST', path: `${BASE}/${ID}/move`, perm: PERM, handler: move },
  { method: 'POST', path: `${BASE}/${ID}/delete`, perm: PERM, handler: remove },
];
