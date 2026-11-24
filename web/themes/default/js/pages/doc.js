'use strict';

window.siteDoc = (main, r) => {
  const { h, panel, cols } = Site;
  Site.setTitle(r.title);
  const body = h('div', { class: 'prose' });
  body.innerHTML = r.bodyHtml;
  main.append(cols('',
    panel({ id: 'doc', title: r.title, level: 1, tag: 'article', body: [h('p', { class: 'lede' }, r.lede), body,
      r.updated ? h('p', { class: 'when' }, `Last changed ${r.updated}.`) : ''] }),
    panel({ id: 'contents', title: 'On this page', body: h('nav', { 'aria-label': 'On this page' },
      h('ol', { class: 'list' }, r.sections.map((s) => h('li', null, h('a', { href: `#${s.id}` }, s.title))))) })));
  if (location.hash) document.getElementById(location.hash.slice(1))?.scrollIntoView();
};
