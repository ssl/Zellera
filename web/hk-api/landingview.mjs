import { DbRefused } from '../cms/lib/db.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { HK_API } from './index.mjs';
import { ACTIONS, ARTICLE_SLOTS, CLIENT_LINKS, LIMITS, articleRefusal, cleanArticle, refreshLandingView, shownAt } from '../lib/landing-view.mjs';
import { audit } from './_guard.mjs';

export const owner = 'hk-content';

const PERM = 'housekeeping.landing_view';
const COLS = ['id', 'title', 'body', 'button_text', 'button_kind', 'button_link', 'image', 'sort_order', 'deleted'];
export const grants = [
  { table: 'news_articles', select: COLS, insert: COLS.filter((c) => c !== 'id' && c !== 'deleted'), update: COLS.filter((c) => c !== 'id') },
  { table: 'catalogue_pages', select: ['id', 'code', 'caption', 'layout', 'visible', 'enabled', 'catalogue'] },
  { table: 'rooms', select: ['id', 'name'] },
];

const BASE = `${HK_API}/landing-view`;
const EDIT_LIMIT = ['hk-landingview', 60, 3600];
const FIELDS = ['title', 'text', 'button_text', 'button_type', 'button_link', 'image'];
const IMAGE_HINT = '/path/to/picture.png';

const listArticles = (ctx) => ctx.db.query(sql`SELECT id, title, body AS text, button_text, button_kind AS button_type, button_link, image, sort_order
  FROM news_articles WHERE deleted = 0 ORDER BY sort_order, id`);

const loadArticle = async (ctx, id) => {
  if (!id) return null;
  const [a] = await ctx.db.query(sql`SELECT id, title, body AS text, button_text, button_kind AS button_type, button_link, image, sort_order
    FROM news_articles WHERE id = ${id} AND deleted = 0 LIMIT 1`);
  return a ?? null;
};

const catalogPages = (ctx) => ctx.db.query(sql`SELECT code AS caption_save, caption FROM catalogue_pages
  WHERE catalogue = 'normal' AND visible = TRUE AND enabled = TRUE AND layout <> 'folder' AND code <> '' ORDER BY code`);

const existsRefusal = async (ctx, a) => {
  if (a.button_type === 'catalog') {
    const [p] = await ctx.db.query(sql`SELECT id FROM catalogue_pages WHERE code = ${a.button_link}
      AND catalogue = 'normal' AND visible = TRUE AND enabled = TRUE AND layout <> 'folder' LIMIT 1`);
    if (!p) return `The catalog has no open page called ${a.button_link}.`;
  }
  if (a.button_type === 'room') {
    const [r] = await ctx.db.query(sql`SELECT id FROM rooms WHERE id = ${Number(a.button_link)} LIMIT 1`);
    if (!r) return `There is no room ${a.button_link}.`;
  }
  return null;
};

const articleJson = (a) => ({
  id: Number(a.id), title: String(a.title ?? ''), text: String(a.text ?? ''), button_text: String(a.button_text ?? ''),
  button_type: String(a.button_type ?? 'none'), button_link: String(a.button_link ?? ''), image: String(a.image ?? ''),
});

const listAnswer = async (ctx) => {
  const rows = await listArticles(ctx);
  return {
    ok: true,
    slots: ARTICLE_SLOTS.length,
    articles: rows.map((a, i) => ({ ...articleJson(a), shown: shownAt(i).shown, imageShown: shownAt(i).shown && shownAt(i).image })),
  };
};

const listPage = async (req, res, ctx) => ctx.send.json(res, 200, await listAnswer(ctx));

const formAnswer = async (ctx, article) => ({
  ok: true,
  article,
  form: {
    actions: ACTIONS.map((x) => ({ type: x.type, label: x.label, hint: x.hint })),
    clientLinks: [...CLIENT_LINKS],
    catalogPages: (await catalogPages(ctx)).map((p) => ({ code: String(p.caption_save), caption: String(p.caption ?? '') })),
    limits: LIMITS,
    imageHint: IMAGE_HINT,
  },
});

const addPage = async (req, res, ctx) => ctx.send.json(res, 200, await formAnswer(ctx, null));

const editPage = async (req, res, ctx) => {
  const a = await loadArticle(ctx, intParam(ctx.params.id));
  if (!a) return ctx.send.notFound(res, 'No article has that id.');
  return ctx.send.json(res, 200, await formAnswer(ctx, articleJson(a)));
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
  await refreshLandingView();
  return true;
};

const formArticle = (ctx) => Object.fromEntries(FIELDS.map((k) => [k, String(ctx.form.get(k) ?? '')]));

const save = async (req, res, ctx, id) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const old = id == null ? null : await loadArticle(ctx, intParam(id));
  if (id != null && !old) return ctx.send.notFound(res, 'No article has that id.');
  const sent = formArticle(ctx);
  const refusal = articleRefusal(sent) ?? await existsRefusal(ctx, cleanArticle(sent));
  if (refusal) return ctx.send.error(res, 400, 'Not saved', refusal);
  const a = cleanArticle(sent);
  let newId = old ? Number(old.id) : 0;
  const ok = await writing(ctx, res, async () => {
    if (old) {
      await ctx.db.write(sql`UPDATE news_articles SET title = ${a.title}, body = ${a.text}, button_text = ${a.button_text},
        button_kind = ${a.button_type}, button_link = ${a.button_link}, image = ${a.image} WHERE id = ${newId} AND deleted = 0`);
    } else {
      const [m] = await ctx.db.query(sql`SELECT COALESCE(MAX(sort_order), 0) AS n FROM news_articles WHERE deleted = 0`);
      const r = await ctx.db.write(sql`INSERT INTO news_articles (title, body, button_text, button_kind, button_link, image, sort_order)
        VALUES (${a.title}, ${a.text}, ${a.button_text}, ${a.button_type}, ${a.button_link}, ${a.image}, ${Number(m?.n ?? 0) + 1})`);
      newId = Number(r?.insertId) || 0;
    }
  });
  if (!ok) return;
  await audit(ctx, { action: old ? 'hk.landingview.edit' : 'hk.landingview.add', targetType: 'article', targetId: newId, label: a.title, detail: `${a.button_type} ${a.button_link}`.trim() });
  return ctx.send.json(res, old ? 200 : 201, { ok: true, article: articleJson({ ...a, id: newId }) });
};

const create = (req, res, ctx) => save(req, res, ctx, null);
const update = (req, res, ctx) => save(req, res, ctx, ctx.params.id);

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
        if (Number(r.sort_order) !== n + 1) await ctx.db.write(sql`UPDATE news_articles SET sort_order = ${n + 1} WHERE id = ${Number(r.id)}`);
      }
    });
    if (!ok) return;
    await audit(ctx, { action: 'hk.landingview.move', targetType: 'article', targetId: id, label: String(rows[j].title), detail: `${direction}: now ${j + 1} of ${rows.length}` });
  }
  return ctx.send.json(res, 200, await listAnswer(ctx));
};

const remove = async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const a = await loadArticle(ctx, intParam(ctx.params.id));
  if (!a) return ctx.send.notFound(res, 'No article has that id.');
  const ok = await writing(ctx, res, () => ctx.db.write(sql`UPDATE news_articles SET deleted = 1 WHERE id = ${Number(a.id)}`));
  if (!ok) return;
  await audit(ctx, { action: 'hk.landingview.delete', targetType: 'article', targetId: Number(a.id), label: String(a.title) });
  return ctx.send.json(res, 200, await listAnswer(ctx));
};

const ID = ':id(\\d{1,10})';
export const routes = [
  { method: 'GET', path: BASE, perm: PERM, handler: listPage },
  { method: 'GET', path: `${BASE}/new`, perm: PERM, handler: addPage },
  { method: 'GET', path: `${BASE}/${ID}`, perm: PERM, handler: editPage },
  { method: 'POST', path: BASE, perm: PERM, handler: create },
  { method: 'POST', path: `${BASE}/${ID}`, perm: PERM, handler: update },
  { method: 'POST', path: `${BASE}/${ID}/move`, perm: PERM, handler: move },
  { method: 'POST', path: `${BASE}/${ID}/delete`, perm: PERM, handler: remove },
];
