'use strict';

HK.start((main) => HK.load(main, `/users/${HK.param(2)}/visits${HKUser.cursorQuery()}`, (r) => {
  const { panel, fmt } = HK;
  const { userHead, roomLink, cursorPager, duration } = HKUser;
  HK.setTitle(`${r.user.name} · Room visits`);
  main.append(userHead(r.user, 'visits'), panel({ id: 'visits', title: 'Room visits, newest first', body: [
    HK.table({
      caption: 'Room visits', head: ['Room', 'Entered', 'Left', 'Time in the room'], cls: ['', 'nowrap', 'nowrap', 'nowrap'], emptyText: r.cursor ? 'Nothing older.' : 'This user has not entered a room.',
      rows: r.rows.map((v) => [roomLink(v.room), fmt.dateTime(v.enteredAt), v.leftAt ? fmt.dateTime(v.leftAt) : '-', v.leftAt ? duration(v.leftAt - v.enteredAt) : '-']),
    }),
    cursorPager(r)] }));
}));
