'use strict';

HK.start((main) => HK.load(main, '/landing-view', (first) => {
  const { h, panel, table, pill, alertBox, urls } = HK;
  HK.setTitle('Landing view');
  const BUTTON = { catalog: 'Catalog page', room: 'Room', client: 'Client link', web: 'Page' };
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
    const button = (a) => (a.button_type === 'none' ? h('span', { class: 'muted' }, 'No button')
      : [h('b', null, a.button_text), ': ', BUTTON[a.button_type] ?? a.button_type, ' ', h('span', { class: 'mono' }, a.button_link)]);
    const shown = (a) => (!a.shown ? pill('Not shown') : a.imageShown || !a.image ? pill('Shown', 'on') : [pill('Shown', 'on'), ' ', h('span', { class: 'muted' }, 'without its image')]);
    const move = (a, direction, label, disabled) => h('button', {
      class: 'btn btn--small', type: 'button', disabled, 'aria-label': `Move ${a.title} ${direction}`,
      onclick: (e) => act(e.currentTarget, `/landing-view/${a.id}/move`, { direction }),
    }, label);
    const remove = (a) => h('button', {
      class: 'btn btn--small', type: 'button', 'aria-label': `Delete ${a.title}`,
      onclick: (e) => { if (window.confirm(`Delete “${a.title}”?`)) act(e.currentTarget, `/landing-view/${a.id}/delete`, {}); },
    }, 'Delete');
    HK.clear(box).append(panel({ id: 'articles', title: `Articles (${rows.length})`, body: [
      table({
        caption: 'Landing view articles', main: 1, head: ['#', 'Title', 'Button', 'Image', 'Shown', 'Order', ''],
        cls: ['num', 'nowrap', 'grow', 'mono-cell nowrap', 'nowrap', 'nowrap', 'nowrap'],
        emptyText: 'No articles: the landing view shows no promo blocks.',
        rows: rows.map((a, i) => [
          String(i + 1),
          h('a', { href: `${urls.landingView()}/${a.id}` }, a.title),
          button(a),
          a.image ? h('span', { title: a.image }, a.image.split('/').pop()) : h('span', { class: 'muted' }, 'None'),
          shown(a),
          h('span', { class: 'row row--tight' }, move(a, 'up', 'Up', i === 0), move(a, 'down', 'Down', i === rows.length - 1)),
          remove(a),
        ]),
      })] }));
  };

  main.append(
    HK.head({
      id: 'landingview-title', title: 'Landing view',
      lead: `The articles players see in the landing view, before they enter a room. The first ${first.slots} are shown, in this order: the 1st and 3rd on the left with their image, the 2nd and 4th on the right as text only. A player sees a change at their next login.`,
      actions: h('a', { class: 'btn btn--go', href: `${urls.landingView()}/new` }, 'Add an article'),
    }),
    problem, box);
  draw(first);
}));
