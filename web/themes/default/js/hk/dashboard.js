'use strict';

HK.start((main, me) => HK.load(main, '/dashboard', (d) => {
  const { h, panel, cols, fmt, urls } = HK;
  const { userLink, logTable } = HKUser;
  HK.setTitle('Dashboard');
  const s = d.stats;
  const tallies = HK.figures([
    [s.online, 'Online now'], [s.newDay, 'New accounts, 24 hours'], [s.newWeek, 'New accounts, 7 days'],
    [s.roomsWithUsers, 'Rooms with users', me.can['moderation.room.info'] ? urls.rooms() : null],
    [s.openTickets, 'Open tickets', urls.tickets('open')], [s.pickedTickets, 'Picked tickets', urls.tickets('picked')],
    [s.activeBans, 'Active bans']], 'Tallies');
  const searchBox = (id, action, label, placeholder, submit) => h('form', { class: 'search-form', method: 'get', action, role: 'search' },
    h('div', { class: 'field' }, h('label', { for: id }, label), h('input', { id, name: 'q', type: 'search', placeholder, autocomplete: 'off', spellcheck: 'false' })),
    h('button', { class: 'btn btn--go', type: 'submit' }, submit));
  const finders = panel({ id: 'find', title: me.can['moderation.room.info'] ? 'Find a user or a room' : 'Find a user', body: [
    searchBox('hk-user-q', urls.users(), 'User name or id', 'Start of a name, or an id', 'Find user'),
    me.can['moderation.room.info'] ? searchBox('hk-room-q', urls.rooms(), 'Room id, name or owner', 'Id, name or owner', 'Find room') : ''] });
  const online = panel({ id: 'online', title: `Online now (${fmt.number(s.online)})`, body: HK.table({
    caption: 'Users online now', head: ['Name', 'Logged in'], emptyText: 'Nobody is online.', cls: ['nowrap', 'nowrap'],
    rows: d.online.map((u) => [userLink(u), fmt.dateTime(u.lastLogin)]),
  }) });
  const log = d.log ? panel({ id: 'log', title: 'Latest housekeeping actions', more: ['The whole audit log', urls.audit()], body: logTable(d.log) }) : '';
  main.append(
    HK.head({ id: 'dashboard-title', title: `Hello, ${me.staff.name}`, meta: 'Alerts, kicks, mutes, trade locks and ticket handling are in the in-client mod tool: they need the game connection.' }),
    tallies,
    cols('', [finders, log], [online]),
    h('p', { class: 'hint' }, d.version.server && d.version.server !== d.version.site
      ? `Version ${d.version.site} (site), ${d.version.server} (game server)`
      : `Version ${d.version.site}`));
}));
