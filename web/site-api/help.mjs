import { accountRulesOf } from '../lib/site-access.mjs';
import { html, urls } from '../cms/lib/html.mjs';
import { SITE_API } from './index.mjs';
import { TOPICS, topicBySlug } from './help-content.mjs';
import { fillNames } from '../cms/display-names.mjs';

export const owner = 'help';
export const grants = [];

const DESK = [
  { id: 'start', title: 'Start here', slugs: ['getting-started', 'credits', 'club', 'build-pass', 'rooms'] },
  { id: 'game', title: 'In the game', slugs: ['groups', 'pets', 'bots', 'camera', 'software'], rest: true },
  { id: 'rules', title: 'Fair play', tone: 'gold', slugs: ['rules', 'trading', 'reporting', 'bans-and-sanctions'] },
];

const brief = (t) => ({ slug: t.slug, title: fillNames(t.title), summary: fillNames(t.summary) });

const deskGroups = () => {
  const placed = new Set(DESK.flatMap((g) => g.slugs));
  const rest = TOPICS.filter((t) => !placed.has(t.slug));
  return DESK.map((g) => ({
    id: g.id, title: g.title, tone: g.tone ?? '',
    topics: [...g.slugs.map(topicBySlug).filter(Boolean), ...(g.rest ? rest : [])].map(brief),
  }));
};

const elsewhere = (ctx) => html`<ul>
      <li>In the game, the <b>Help</b> window lets you report a player or a room to a moderator
        (<a href="${urls.help('reporting')}">how reporting works</a>).</li>
      <li><a href="${urls.ask()}">Ask staff</a>: a question about your account, something that does not work, or
        anything else.</li>
      <li><a href="${urls.safety()}">Staying safe</a>: your account, your personal details and the safety lock.</li>
      <li><a href="${urls.help('software')}">The software behind the game</a>: what ${ctx.GAME_NAME} runs on.</li>
      <li><a href="${urls.terms()}">Terms</a> and <a href="${urls.privacy()}">privacy</a>: the rules for using
        ${ctx.GAME_NAME} and what it stores about you.</li>
    </ul>`.value;

const noTopic = (ctx, res) => ctx.send.error(res, 404, 'There is no help topic by that name',
  'Maybe it moved. Pick one of the topics below.', { groups: deskGroups() });

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/help`,
    handler: (req, res, ctx) => ctx.send.json(res, 200, {
      ok: true,
      title: 'How can we help?',
      lede: `Short answers about how ${ctx.GAME_NAME} works. Everything here is free: ${ctx.NAMES.credits}, ${ctx.NAMES.points} and ${ctx.NAMES.premium} are never sold for money.`,
      groups: deskGroups(),
      elsewhereHtml: elsewhere(ctx),
    }),
  },
  {
    method: 'GET', path: `${SITE_API}/help/:topic([a-z0-9-]{1,40})`,
    handler: async (req, res, ctx) => {
      const t = topicBySlug(ctx.params.topic);
      if (!t) return noTopic(ctx, res);
      return ctx.send.json(res, 200, {
        ok: true,
        topic: { ...brief(t), bodyHtml: fillNames(t.body(ctx, await accountRulesOf(ctx.access)).value) },
        related: t.related.map(topicBySlug).filter(Boolean).map(brief),
      });
    },
  },
  {
    method: 'GET', path: '/help/*rest',
    handler: (req, res, ctx) => {
      const t = topicBySlug(String(ctx.params.rest).trim().toLowerCase());
      return t ? ctx.send.redirect(res, urls.help(t.slug), 301) : ctx.send.notFound(res);
    },
  },
];
