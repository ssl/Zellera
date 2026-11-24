'use strict';

HK.start((main) => HK.load(main, '/news', (first) => {
  const { h, panel, table, pill, alertBox, fmt, urls } = HK;
  HK.setTitle('News');
  const problem = alertBox();
  const box = h('div');

  const act = async (btn, path, body) => {
    if (btn.disabled) return;
    const restore = HK.busy(btn);
    problem.textContent = '';
    try {
      const r = await HK.post(path, body);
      if (r.ok) { draw(r.data); return; }
      problem.textContent = r.data.error || 'Something went wrong. Try again.';
    } catch {
      problem.textContent = HK.NETWORK_ERROR;
    }
    restore();
  };

  const draw = (r) => {
    const rows = r.articles;
    const move = (a, direction, label, disabled) => h('button', {
      class: 'btn btn--small', type: 'button', disabled, 'aria-label': `Move ${a.title} ${direction}`,
      onclick: (e) => act(e.currentTarget, `/news/${a.id}/move`, { direction }),
    }, label);
    const toggle = (a) => h('button', {
      class: 'btn btn--small', type: 'button', 'aria-label': `${a.shown ? 'Hide' : 'Show'} ${a.title}`,
      onclick: (e) => act(e.currentTarget, `/news/${a.id}/show`, { shown: !a.shown }),
    }, a.shown ? 'Hide' : 'Show');
    const remove = (a) => h('button', {
      class: 'btn btn--small', type: 'button', 'aria-label': `Delete ${a.title}`,
      onclick: (e) => { if (window.confirm(`Delete “${a.title}”? It can not be brought back.`)) act(e.currentTarget, `/news/${a.id}/delete`, {}); },
    }, 'Delete');
    HK.clear(box).append(panel({ id: 'articles', title: `Articles (${rows.length})`, body: [
      table({
        caption: 'News articles', main: 1, head: ['#', 'Title', 'Link', 'Date', 'Shown', 'Order', ''],
        cls: ['num', 'grow', 'mono-cell nowrap', 'nowrap', 'nowrap', 'nowrap', 'nowrap'],
        emptyText: 'No articles: the news page of the site is empty.',
        rows: rows.map((a, i) => [
          String(i + 1),
          h('a', { href: `${urls.news()}/${a.id}` }, a.title),
          a.link_url ? h('span', { title: a.link_text || null }, a.link_url) : h('span', { class: 'muted' }, 'None'),
          a.published ? fmt.date(a.published) : h('span', { class: 'muted' }, 'Not shown yet'),
          a.shown ? pill('Shown', 'on') : pill('Hidden'),
          h('span', { class: 'row row--tight' }, move(a, 'up', 'Up', i === 0), move(a, 'down', 'Down', i === rows.length - 1)),
          h('span', { class: 'row row--tight' }, toggle(a), remove(a)),
        ]),
      })] }));
  };

  main.append(
    HK.head({
      id: 'news-title', title: 'News',
      lead: 'The articles of the site\'s news page, in this order. A hidden article is not on the site; it gets its date when it is shown for the first time.',
      actions: h('a', { class: 'btn btn--go', href: `${urls.news()}/new` }, 'Write an article'),
    }),
    problem, box);
  draw(first);
}));
