'use strict';

Site.start((main) => {
  const id = location.pathname.split('/')[2] ?? '';
  return Site.load(main, `/groups/${encodeURIComponent(id)}`, (r) => {
    const { h, panel, cols, avatar, empty, facts, fmt, urls, initials } = Site;
    const g = r.group;
    Site.setTitle(g.name);
    const about = panel({ id: 'group-facts', title: g.name, level: 1, body: [
      h('div', { class: 'group-mark' }, initials(g.name, true)),
      g.description ? h('p', { class: 'group-description' }, g.description) : '',
      facts([
        g.owner ? ['Owner', h('a', { href: urls.profile(g.owner.name) }, g.owner.name)] : null,
        ['Type', g.type],
        ['Members', fmt.number(r.memberCount)],
        g.created ? ['Created', fmt.date(g.created)] : null,
        ['Forum', g.forum ? `Yes, read it in the ${Site.place()}` : 'No'],
      ])] });
    const room = panel({ id: 'home-room', title: 'Home room', body: g.room
      ? [h('a', { href: urls.room(g.room.id) }, h('b', null, g.room.name)), h('p', { class: 'small muted' }, `${g.room.users} of ${g.room.max} visitors now`)]
      : empty('This group has no home room to show.') });
    const members = panel({ id: 'members', title: `Members (${fmt.number(r.memberCount)})`, body: r.members.length
      ? [h('ul', { class: 'list' }, r.members.map((m) => h('li', { class: 'person' }, avatar(m),
        h('div', { class: 'person__text' }, h('a', { href: urls.profile(m.name) }, h('b', null, m.name)),
          h('span', { class: 'list__sub' }, `${m.role}${m.since ? `, joined ${fmt.date(m.since)}` : ''}`))))),
        r.memberCount > r.members.length ? h('p', { class: 'muted small' }, `Showing the first ${fmt.number(r.members.length)} of ${fmt.number(r.memberCount)} members.`) : '']
      : empty('No members yet.') });
    main.append(cols('side-left', [about, room], members));
  });
});
