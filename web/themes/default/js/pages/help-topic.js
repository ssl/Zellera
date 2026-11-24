'use strict';

Site.start(async (main) => {
  const { h, panel, cols, urls } = Site;
  const list = (topics) => h('ul', { class: 'list' }, topics.map((t) => h('li', null,
    h('a', { href: urls.help(t.slug) }, h('b', null, t.title)), h('span', { class: 'list__sub' }, t.summary))));
  const slug = location.pathname.split('/')[2];
  let r;
  try { r = await Site.api(`/help/${encodeURIComponent(slug)}`); } catch { r = { status: 0, ok: false, data: {} }; }
  Site.clear(main);
  if (!r.ok && r.status === 404 && r.data.groups) {
    Site.setTitle('Help topic not found');
    main.append(panel({ id: 'not-found', title: r.data.title, level: 1, tone: 'red', body: h('p', null, r.data.error) }),
      h('div', { class: 'cols cols--three' }, r.data.groups.map((g) => h('div', { class: 'col' }, panel({ title: g.title, tone: g.tone, body: list(g.topics) })))));
    return;
  }
  if (!r.ok) return Site.showError(main, r);
  const { topic, related } = r.data;
  Site.setTitle(topic.title);
  const body = h('div', { class: 'prose' });
  body.innerHTML = topic.bodyHtml;
  main.append(cols('',
    panel({ id: 'topic', title: topic.title, level: 1, tag: 'article', body: [h('nav', { class: 'crumbs', 'aria-label': 'Breadcrumb' }, h('a', { href: urls.help() }, 'Help'), ' › ', topic.title), h('p', { class: 'lede' }, topic.summary), body] }),
    panel({ id: 'related', title: 'Related help', body: [list(related), h('p', { class: 'small' }, h('a', { href: urls.help() }, 'All help topics'))] })));
});
