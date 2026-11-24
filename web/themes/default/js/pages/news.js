'use strict';

Site.start((main, info) => {
  const { h, panel, cols, time, empty, urls } = Site;
  const show = (pageNo) => Site.load(main, `/news${pageNo > 1 ? `?page=${pageNo}` : ''}`, (r) => {
    Site.setTitle(r.page > 1 ? `News, page ${r.page}` : 'News');
    const [first, ...rest] = r.items;
    const go = (n) => {
      history.pushState(null, '', n > 1 ? `/news?page=${n}` : '/news');
      show(n);
    };
    const lead = first
      ? panel({ id: `news-${first.id}`, title: first.title, level: 1, tone: 'gold', body: [
        first.created ? h('p', { class: 'when' }, time(first.created)) : '',
        first.excerpt ? h('p', null, first.excerpt) : '',
        h('p', { class: 'actions' }, h('a', { class: 'btn', href: urls.news(first.id) }, 'Read the whole story', h('span', { class: 'visually-hidden' }, `: ${first.title}`)))] })
      : panel({ id: 'news', title: 'News', level: 1, tone: 'gold', body: empty('No news yet. Staff post updates and events here.') });
    const earlier = rest.length ? panel({ id: 'earlier', title: 'Earlier news', body: h('ul', { class: 'list' }, rest.map((n) => h('li', null,
      h('a', { href: urls.news(n.id) }, h('b', null, n.title)),
      n.created ? h('span', { class: 'list__sub' }, time(n.created)) : '',
      n.excerpt ? h('p', { class: 'small' }, n.excerpt) : ''))) }) : '';
    const side = panel({ id: 'about-news', title: `News from ${info.name}`, body: [
      h('p', null, 'Updates, events and competitions from the staff.'),
      h('ul', { class: 'list' }, info.sees.staff ? h('li', null, h('a', { href: urls.staff() }, `Who runs the ${Site.place()}`)) : '', h('li', null, h('a', { href: urls.help() }, 'Help')))] });
    main.append(cols('', [lead, earlier, Site.pager({ page: r.page, hasNext: r.hasNext, go })], side));
  });
  window.addEventListener('popstate', () => show(Number(new URLSearchParams(location.search).get('page')) || 1));
  return show(Number(new URLSearchParams(location.search).get('page')) || 1);
});
