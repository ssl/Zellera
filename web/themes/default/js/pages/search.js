'use strict';

Site.start(async (main) => {
  const { h, panel, avatar, pill, empty, urls } = Site;
  const q = new URLSearchParams(location.search).get('name')?.trim() ?? '';
  Site.setTitle(q ? `Find a player: ${q}` : 'Find a player');
  const form = h('form', { class: 'inline-form inline-form--short', method: 'get', action: urls.search(), role: 'search' },
    h('label', { class: 'visually-hidden', for: 'search-name' }, 'Player name'),
    h('input', { id: 'search-name', name: 'name', type: 'search', value: q, maxlength: 50, autocomplete: 'off', autocapitalize: 'none', required: true }),
    h('button', { class: 'btn btn--go', type: 'submit' }, 'Search'));
  const ask = panel({ id: 'find', title: 'Find a player', level: q ? 2 : 1, body: [
    h('p', { class: 'small' }, 'Type the start of a name. Private profiles stay hidden.'), form] });
  if (!q) { Site.clear(main).append(ask); return; }
  return Site.load(main, `/players?name=${encodeURIComponent(q)}`, (r) => {
    const results = r.rows.length === 0
      ? panel({ id: 'results', title: 'No one found', level: 1, body: empty(`No player's name starts with “${r.q}”. Check the spelling, or type fewer letters.`) })
      : panel({ id: 'results', title: `Names that start with “${r.q}”`, level: 1, body: [
        h('ul', { class: 'list' }, r.rows.map((p) => h('li', { class: 'person' }, avatar(p),
          h('div', { class: 'person__text' }, h('a', { href: urls.profile(p.name) }, h('b', null, p.name)), p.motto ? h('p', { class: 'motto small' }, p.motto) : ''),
          p.online ? pill('Online', 'on') : ''))),
        r.more ? h('p', { class: 'muted small' }, `Only the first ${r.rows.length} are shown. Type more letters to narrow it down.`) : ''] });
    main.append(ask, results);
  });
});
