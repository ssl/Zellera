import { excerpt } from '../cms/lib/excerpt.mjs';
import { safeUrl } from '../cms/lib/html.mjs';
import { intParam, sql } from '../cms/lib/sql.mjs';
import { SITE_API } from './index.mjs';

export const owner = 'community';
export const grants = [
  { table: 'news_panels', select: ['id', 'title', 'body', 'link_text', 'link_url', 'enabled', 'sort_order', 'created_at'] },
];

export const PER_PAGE = 10;
const EXCERPT = 240;

export const linkHref = (url) => {
  const s = String(url ?? '').trim();
  if (!s) return null;
  const safe = safeUrl(s);
  return safe === '#' ? null : safe;
};

export const titleOf = (n) => String(n.title ?? '').trim() || `News ${n.id}`;

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/news`, gate: 'community',
    handler: async (req, res, ctx) => {
      const raw = ctx.query.get('page');
      const pageNo = raw == null || raw === '' ? 1 : intParam(raw, { max: 100000 });
      if (!pageNo) return ctx.send.notFound(res, 'There is no such news page.');
      const rows = await ctx.db.query(sql`
        SELECT id, title, LEFT(body, ${EXCERPT * 2}) AS body, created_at AS created
          FROM news_panels
         WHERE enabled = 1
         ORDER BY sort_order ASC, id DESC
         LIMIT ${PER_PAGE + 1} OFFSET ${(pageNo - 1) * PER_PAGE}`);
      const items = [...rows];
      if (pageNo > 1 && items.length === 0) return ctx.send.notFound(res, 'There is no such news page.');
      return ctx.send.json(res, 200, {
        ok: true,
        page: pageNo,
        hasNext: items.length > PER_PAGE,
        items: items.slice(0, PER_PAGE).map((n) => ({
          id: Number(n.id), title: titleOf(n), excerpt: excerpt(n.body, EXCERPT), created: Number(n.created) || 0,
        })),
      });
    },
  },
  {
    method: 'GET', path: `${SITE_API}/news/:id(\\d{1,10})`, gate: 'community',
    handler: async (req, res, ctx) => {
      const id = intParam(ctx.params.id);
      const [n] = id ? await ctx.db.query(sql`
        SELECT id, title, body, link_text, link_url, created_at AS created
          FROM news_panels
         WHERE id = ${id} AND enabled = 1
         LIMIT 1`) : [];
      if (!n) return ctx.send.notFound(res, 'There is no news article with this number.');
      const href = linkHref(n.link_url);
      return ctx.send.json(res, 200, {
        ok: true,
        article: {
          id: Number(n.id), title: titleOf(n), body: String(n.body ?? ''), created: Number(n.created) || 0,
          link: href ? { href, text: String(n.link_text ?? '').trim() || 'Read more' } : null,
        },
      });
    },
  },
];
