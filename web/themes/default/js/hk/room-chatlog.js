'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, fmt, urls } = HK;
  HK.setTitle(`Chatlog ${r.room.name}`);
  const nav = (r.before || r.nextBefore) ? h('nav', { class: 'pager', 'aria-label': 'Chat pages' },
    r.before ? h('a', { class: 'btn btn--small', href: urls.roomChatlog(r.room.id) }, 'Newest') : h('span'),
    h('span', null, r.before ? `Before ${fmt.dateTime(r.before)}` : 'Newest lines'),
    r.nextBefore ? h('a', { class: 'btn btn--small', href: urls.roomChatlog(r.room.id, { before: r.nextBefore }), rel: 'next' }, 'Older') : h('span')) : '';
  main.append(
    HK.head({
      id: 'chat-title',
      crumbs: [['Rooms', urls.rooms()], r.room.exists ? [r.room.name, urls.room(r.room.id)] : [`Room #${r.room.id}`], ['Chatlog']],
      title: `Chatlog: ${r.room.name}`,
      meta: h('span', null, `Room #${r.room.id}`),
      lead: `Room chat and whispers, newest first, ${r.pageSize} lines a page. Times are UTC. Opening this page is written to the audit log.`,
    }),
    panel({ title: r.before ? `Lines before ${fmt.dateTime(r.before)}` : 'Newest lines', body: [
      HKRooms.chatTable({ lines: r.lines, users: r.users, caption: `Chat in ${r.room.name}`, emptyText: r.before ? 'No older chat.' : 'No chat logged in this room.' }), nav] }));
}));
