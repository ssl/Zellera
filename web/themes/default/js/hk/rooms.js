'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, table, fmt, pager, urls } = HK;
  const { userLink, doorPill, when } = HKRooms;
  HK.setTitle(r.q ? `Rooms: ${r.q}` : 'Rooms');
  const rooms = (rows, caption, emptyText) => table({
    caption, emptyText,
    head: ['Room', 'Owner', 'Door', 'Users', 'Category', 'Created'],
    cls: ['', 'nowrap', 'nowrap', 'nowrap', '', 'nowrap'],
    rows: rows.map((x) => [
      [h('a', { href: urls.room(x.id) }, x.name || `Room ${x.id}`), ' ', h('span', { class: 'muted' }, `#${x.id}`)],
      userLink(r.users, x.ownerId),
      doorPill(x.door),
      `${fmt.number(x.players)} / ${fmt.number(x.maxPlayers)}`,
      x.category == null ? h('span', { class: 'muted' }, `#${x.categoryId}`) : x.category,
      when(x.created),
    ]),
  });
  const form = HK.searchForm({ fields: [{ name: 'q', label: 'Room id, room name or owner name', value: r.q, placeholder: 'e.g. 1024, Cafe or pixel' }] });
  main.append(HK.head({ id: 'rooms-title', crumbs: r.q ? [['Rooms', urls.rooms()], [`“${r.q}”`]] : [], title: 'Rooms' }), form);
  if (r.q) {
    main.append(panel({ id: 'results', title: `Results for “${r.q}”`, body: [
      rooms(r.rows, 'Matching rooms', 'No room matches. Try the room id, part of the name, or the start of the owner name.'),
      pager({ page: r.page, hasNext: r.hasNext })] }));
  } else {
    main.append(
      panel({ id: 'busy', title: 'Rooms with users now', body: rooms(r.busy, 'Rooms with users now', 'No room has users right now.') }),
      panel({ id: 'newest', title: 'Newest rooms', body: rooms(r.newest, 'Newest rooms', 'There are no rooms yet.') }));
  }
}));
