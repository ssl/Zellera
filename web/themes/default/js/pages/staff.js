'use strict';

Site.start((main) => Site.load(main, '/staff', (r) => {
  const { h, panel, avatar, pill, empty, urls } = Site;
  Site.setTitle('Staff');
  const intro = panel({ id: 'staff', title: `Who runs the ${Site.place()}`, level: 1, tone: 'gold', body: [
    h('p', null, `Staff never ask for your password and never sell anything. Stuck? Use Help in the ${Site.place()}, or read the help pages.`),
    h('ul', { class: 'list' }, h('li', null, h('a', { href: urls.ask() }, 'Ask staff a question')), h('li', null, h('a', { href: urls.help('reporting') }, 'Reporting a problem')),
      h('li', null, h('a', { href: urls.safety() }, 'Staying safe')))] });
  const ranks = r.ranks.length === 0 ? [panel({ title: 'Staff', body: empty('No staff to show yet.') })]
    : r.ranks.map((rank) => panel({ title: rank.name, body: h('ul', { class: 'list' }, rank.members.map((m) => h('li', { class: 'person' },
      avatar(m), h('div', { class: 'person__text' }, Site.profileLink(m.name, h('b', null, m.name)), m.motto ? h('p', { class: 'motto small' }, m.motto) : ''),
      m.online ? pill('Online', 'on') : ''))) }));
  main.append(Site.cols('side-left', intro, ranks));
}));
