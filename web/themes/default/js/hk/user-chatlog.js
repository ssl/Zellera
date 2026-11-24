'use strict';

HK.start((main) => HK.load(main, `/users/${HK.param(2)}/chatlog${HKUser.cursorQuery()}`, (r) => {
  const { h, panel, fmt } = HK;
  const { userHead, userLink, roomLink, cursorPager } = HKUser;
  HK.setTitle(`${r.user.name} · Chatlog`);
  const head = userHead(r.user, 'chatlog');
  head.append(h('p', { class: 'hk-head__lead' }, 'Room chat and whispers, newest first. Private messages show only inside an IM ticket. Viewing this page is logged.'));
  main.append(head, panel({ id: 'chat', title: 'Room chat and whispers', body: [
    HK.table({
      caption: 'Chatlog', head: ['When', 'Room', 'To', 'Message'], cls: ['nowrap', 'nowrap', 'nowrap', 'grow'], emptyText: r.cursor ? 'Nothing older.' : 'This user has not said anything in a room.',
      rows: r.rows.map((c) => [fmt.dateTime(c.at), roomLink(c.room), c.to ? ['whisper to ', userLink(c.to)] : 'room', h('span', { class: 'chat-line' }, c.message)]),
    }),
    cursorPager(r)] }));
}));
