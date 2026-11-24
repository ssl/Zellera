'use strict';

Site.start((main, info) => {
  const id = location.pathname.split('/')[2];
  return Site.load(main, `/news/${encodeURIComponent(id)}`, ({ article: n }) => {
    const { h, panel, cols, time, urls } = Site;
    Site.setTitle(n.title);
    const paragraphs = n.body.split(/\r?\n[ \t]*\r?\n/).map((p) => p.trim()).filter(Boolean)
      .map((p) => h('p', null, p.split(/\r\n|\r|\n/).flatMap((line, i) => (i ? [h('br'), line] : [line]))));
    const story = panel({ id: 'article', title: n.title, level: 1, tone: 'gold', tag: 'article', body: [
      n.created ? h('p', { class: 'when' }, time(n.created)) : '',
      h('div', { class: 'prose' }, paragraphs),
      h('p', { class: 'actions' }, n.link ? h('a', { class: 'btn btn--go', href: n.link.href }, n.link.text) : '', h('a', { class: 'btn', href: urls.news() }, 'All news'))] });
    main.append(cols('', story, panel({ id: 'more', title: 'More to read', body: h('ul', { class: 'list' },
      h('li', null, h('a', { href: urls.news() }, 'All news')), h('li', null, h('a', { href: urls.help() }, 'Help')), info.sees.staff ? h('li', null, h('a', { href: urls.staff() }, 'Staff')) : '') })));
  });
});
