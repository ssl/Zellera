'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, cols, notice, fmt, urls } = HK;
  const { userLink, roomLink, when, ticketTable } = HKRooms;
  HK.setTitle(`Photo item ${r.item.id}`);
  const shot = r.photo;
  const photographer = shot ? r.users[shot.userId] : null;
  const unknown = () => h('span', { class: 'muted' }, 'unknown');
  main.append(
    HK.head({
      id: 'photo-title',
      crumbs: [['Tickets', urls.tickets()], [`Photo item #${r.item.id}`]],
      title: `Photo item #${r.item.id}`,
      meta: r.uuid ? h('span', { class: 'mono' }, r.uuid) : 'No photo id',
      actions: [
        shot && photographer && shot.published ? h('a', { class: 'btn', href: `/profile/${encodeURIComponent(photographer.name)}/photo/${encodeURIComponent(r.uuid)}` }, 'Public photo page') : '',
        r.item.roomId > 0 ? h('a', { class: 'btn', href: urls.room(r.item.roomId) }, 'Room') : ''],
    }),
    cols('side-left',
      panel({ title: 'The photo', body: r.image
        ? h('div', { class: 'photo-frame' }, h('img', { class: 'photo', src: r.image, alt: 'The reported photo', width: 320, height: 320, decoding: 'async' }))
        : notice(r.uuid ? 'The image file of this photo is missing on the server.' : 'This item holds no photo id, so there is no image to show.', 'warn') }),
      panel({ title: 'About it', body: [HK.factsGrid([
        ['Item owner', userLink(r.users, r.item.ownerId)],
        ['Hangs in', r.item.roomId > 0 ? roomLink(r.rooms, r.item.roomId) : h('span', { class: 'muted' }, 'an inventory (not placed)')],
        ['Photographer', shot ? userLink(r.users, shot.userId) : unknown()],
        ['Taken in', shot ? roomLink(r.rooms, shot.roomId) : unknown()],
        ['Taken', shot && shot.taken > 0 ? when(shot.taken) : unknown()],
        ['Published', shot ? (shot.published ? ['yes, ', when(shot.publishedAt)] : 'no') : unknown()],
        ['Copies bought', shot ? fmt.number(shot.purchases) : unknown()],
        ['Furni', [r.item.furni.name, ' ', h('span', { class: 'mono muted' }, r.item.furni.code)]],
      ]), h('p', { class: 'foot-note' }, 'Housekeeping only reads. To take the photo down, use the in-client mod tool (room tool, or pick the furni up as staff): the room is in the game server\'s memory.')] })),
    panel({ id: 'photo-tickets', title: `Reports of this photo (${fmt.number(r.tickets.length)})`, body: ticketTable({ tickets: r.tickets, users: r.users, rooms: r.rooms, caption: 'Reports of this photo', emptyText: 'No ticket names this photo item.' }) }));
}));
