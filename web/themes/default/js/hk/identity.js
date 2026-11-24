'use strict';

HK.start((main) => HK.load(main, `/identity/${HK.param(2)}`, (r) => {
  const { h, panel, fmt, pill } = HK;
  const { userHead, userLink, banEnds } = HKUser;
  HK.setTitle(`${r.user.name} · Identity`);
  const mono = (v) => (v ? h('span', { class: 'mono' }, v) : 'Not recorded');
  const rank = (u) => (u.rankName ? `${u.rankName} (${u.rank})` : `Rank ${u.rank}`);
  main.append(
    userHead(r.user, 'identity'),
    panel({ id: 'addresses', title: 'Addresses', body: [
      HK.factsGrid([['IP at registration', mono(r.addresses.registered)], ['Last IP', mono(r.addresses.last)], ['Device', mono(r.addresses.device), 'wide']]),
      h('p', { class: 'foot-note' }, 'The device is the one of the last game login: its id and nine pieces. The Devices tab compares the pieces. Viewing this page is logged.')] }),
    ...r.shared.map((v) => panel({ title: [v.label, ': ', h('span', { class: 'mono' }, v.value)], body: [
      h('p', { class: 'foot-note' }, v.kind === 'ip' ? 'Other accounts that registered or last logged in from this IP.' : 'Other accounts that logged in to the game with this device id.'),
      HK.table({
        caption: `Other accounts, ${v.label}`, head: ['Name', 'Id', 'Rank', 'Online', 'Last online'], emptyText: 'No other account.', cls: ['nowrap', 'num', '', 'nowrap', 'nowrap'],
        rows: v.rows.map((o) => [userLink(o), String(o.id), rank(o), o.online ? pill('Online now', 'on') : pill('Offline'), fmt.dateTime(o.lastOnline)]),
      }),
      v.more ? h('p', { class: 'foot-note' }, `More than ${v.rows.length}; the ${v.rows.length} last online are shown.`) : ''] })),
    panel({ id: 'ip-bans', title: 'Bans that keep out these addresses or this machine', body: HK.table({
      caption: 'Bans on an address or machine', main: 1, head: ['Id', 'Banned account', 'Covers', 'On', 'Reason', 'By', 'Start', 'Expires', 'State'],
      cls: ['num', 'nowrap', 'nowrap', 'nowrap', '', 'nowrap', 'nowrap', 'nowrap', 'nowrap'],
      emptyText: 'No ban keeps out one of these.',
      rows: r.bans.map((b) => [String(b.id), userLink(b.user), b.covers, h('span', { class: 'mono' }, b.on), b.reason || '-', userLink(b.staff, 'system'),
        fmt.dateTime(b.bannedAt), banEnds(b), b.active ? pill('Active', 'bad') : pill(b.lifted ? 'Lifted' : 'Ended')]),
    }) }),
    h('p', { class: 'foot-note' }, 'A ban is on one account; it can name an address or a machine it keeps out as well. Ban on the user page bans the account only.'));
}));
