'use strict';

HK.start((main) => HK.load(main, `/help-requests${HK.query()}`, (r) => {
  const { h, panel, pill, table, pager, fmt, urls } = HK;
  const state = r.states.find((s) => s.key === r.state);
  HK.setTitle(`${state.label} help requests`);
  const who = (q) => (q.playerId > 0
    ? h('a', { href: urls.user(q.playerId) }, r.users[q.playerId]?.name ?? `#${q.playerId}`)
    : h('span', null, q.guestName || 'Visitor', ' ', pill('no account')));
  main.append(
    HK.head({
      id: 'help-title',
      title: 'Help requests',
      lead: 'Questions from the help page of the site. Reports from inside the game are under Tickets.',
      tabs: HK.pageTabs(r.states.map((s) => [s.key, s.label, urls.helpRequests(s.key === 'open' ? undefined : s.key), pill(fmt.number(s.count))]), r.state, 'Request state'),
    }),
    panel({ id: 'queue', title: `${state.label} requests, ${r.state === 'closed' ? 'newest' : 'oldest waiting'} first`, body: [
      table({
        caption: `${state.label} help requests`, head: ['#', 'Sent', 'From', 'About', 'Message', 'Staff'],
        emptyText: `No ${state.label.toLowerCase()} help requests.`, cls: ['nowrap', 'nowrap', 'nowrap', 'nowrap', 'excerpt grow', 'nowrap'],
        rows: r.requests.map((q) => [
          h('a', { href: urls.helpRequest(q.id) }, `#${q.id}`), fmt.dateTime(q.created), who(q), q.topic.label,
          h('a', { href: urls.helpRequest(q.id) }, q.excerpt),
          q.staffId > 0 ? (r.users[q.staffId]?.name ?? `#${q.staffId}`) : h('span', { class: 'muted' }, 'Nobody yet'),
        ]),
      }),
      pager({ page: r.page, hasNext: r.hasNext })] }));
}));
