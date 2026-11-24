'use strict';

const helpTopics = (topics) => Site.h('ul', { class: 'list' }, topics.map((t) => Site.h('li', null,
  Site.h('a', { href: Site.urls.help(t.slug) }, Site.h('b', null, t.title)), Site.h('span', { class: 'list__sub' }, t.summary))));

Site.start((main) => Site.load(main, '/help', (r) => {
  const { h, panel } = Site;
  Site.setTitle('Help');
  const intro = panel({ id: 'help', title: r.title, level: 1, tone: 'gold', body: h('p', { class: 'lede' }, r.lede) });
  const groups = r.groups.map((g) => panel({ id: `topics-${g.id}`, title: g.title, body: helpTopics(g.topics) }));
  const elsewhere = h('div', { class: 'prose prose--cols' });
  elsewhere.innerHTML = r.elsewhereHtml;
  main.append(intro, h('div', { class: 'cols cols--three' }, groups.map((g) => h('div', { class: 'col' }, g))),
    panel({ id: 'elsewhere', title: 'Still stuck?', body: elsewhere }));
}));
