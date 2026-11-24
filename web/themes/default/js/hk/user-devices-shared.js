'use strict';

HK.start((main) => HK.load(main, `/users/${HK.param(2)}/devices/shared${HK.query()}`, (r) => {
  const { h, panel, pill, fmt, urls } = HK;
  const { userHead, userLink } = HKUser;
  const what = r.by === 'id' ? 'id' : HKDevice.COMBO;
  HK.setTitle(`${r.user.name} · Same ${what}`);
  main.append(
    userHead(r.user, 'devices'),
    panel({ id: 'shared', title: `Other users with the same ${what} (${fmt.number(r.total)})`, body: [
      h('div', { class: 'row small' }, h('span', { class: 'muted' }, 'Matched on'), HKDevice.pattern(Object.fromEntries(r.pieces.map((p) => [p.name, p.value])))),
      HK.table({
        caption: `Other users with the same ${what}`, head: ['Name', 'Id', 'Rank', 'State', 'First seen on it', 'Last seen on it', 'Logins', ''],
        emptyText: 'Nobody else.', cls: ['nowrap', 'num', '', 'nowrap', 'nowrap', 'nowrap', 'num', 'nowrap'],
        rows: r.rows.map((o) => [userLink(o), String(o.id), o.rankName ? `${o.rankName} (${o.rank})` : `Rank ${o.rank}`,
          [o.banned ? pill('Banned', 'bad') : '', o.exempt ? pill('Exempt', 'warn') : '', o.online ? pill('Online now', 'on') : '', !o.banned && !o.exempt && !o.online ? '-' : ''],
          fmt.dateTime(o.firstSeen), fmt.dateTime(o.lastSeen), fmt.number(o.logins),
          h('a', { href: urls.devicesCompare({ a: r.user.id, b: o.id }) }, 'Compare')]),
      }),
      HK.pager({ page: r.page, hasNext: r.hasNext }),
      h('p', { class: 'foot-note' }, [h('a', { href: urls.userDevices(r.user.id) }, `All devices of ${r.user.name}`), '. Newest accounts first.'])] }));
}));
