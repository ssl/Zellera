'use strict';

Site.start((main, info) => {
  const id = location.pathname.split('/')[2] ?? '';
  return Site.load(main, `/rooms/${encodeURIComponent(id)}`, (r) => {
    const { h, panel, cols, facts, fmt, pill, urls } = Site;
    const room = r.room;
    Site.setTitle(room.name);
    const about = panel({ id: 'room-facts', title: room.name, level: 1, body: [
      room.description ? h('p', null, room.description) : '',
      facts([
        room.owner ? ['Owner', h('a', { href: urls.profile(room.owner.name) }, room.owner.name)] : room.ownerLabel ? ['Owner', room.ownerLabel] : null,
        room.category ? ['Category', room.category] : null,
        ['Visitors now', `${room.users} of ${room.max}`],
        ['Door', room.door],
        room.group ? ['Group', h('a', { href: urls.group(room.group.id) }, room.group.name)] : null,
        room.created ? ['Created', fmt.date(room.created)] : null,
        room.tags.length ? ['Tags', h('span', { class: 'tags' }, room.tags.map((t) => pill(t)))] : null,
      ])] });
    const visit = panel({ id: 'visit', title: 'Visit this room', tone: 'gold', body: [
      h('p', null, `Enter the ${Site.place()}, then look the room up by its name in the navigator.`),
      h('p', { class: 'actions' }, info.viewer
        ? h('button', { class: 'btn', type: 'button', 'data-busy': 'Opening…', onclick: (e) => Site.openGame(e.currentTarget, document.getElementById('enter-alert')) }, `Enter the ${Site.place()}`)
        : h('a', { class: 'btn', href: urls.home() }, 'Log in first'))] });
    main.append(cols('', about, visit));
  });
});
