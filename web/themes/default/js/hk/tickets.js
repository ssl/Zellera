'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, pill, pager, fmt, urls } = HK;
  const state = r.states.find((s) => s.key === r.state);
  HK.setTitle(`${state.label} tickets`);
  main.append(
    HK.head({
      id: 'tickets-title',
      title: 'Tickets',
      tabs: HK.pageTabs(r.states.map((s) => [s.key, s.label, urls.tickets(s.key === 'open' ? undefined : s.key), pill(fmt.number(s.count))]), r.state, 'Ticket state'),
    }),
    panel({ id: 'queue', title: `${state.label} tickets, newest first`, body: [
      HKRooms.ticketTable({ tickets: r.tickets, users: r.users, rooms: r.rooms, caption: `${state.label} tickets`, emptyText: `No ${state.label.toLowerCase()} tickets.` }),
      pager({ page: r.page, hasNext: r.hasNext })] }),
    h('p', { class: 'foot-note' }, r.note));
}));
