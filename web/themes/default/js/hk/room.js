'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, cols, table, pill, notice, fmt, field, urls } = HK;
  const { userLink, doorPill, when, ticketTable } = HKRooms;
  const room = r.room;
  const name = room.name || `Room ${room.id}`;
  HK.setTitle(`Room ${room.name || room.id}`);

  const count = (list) => (list.more ? `${list.rows.length}+` : String(list.rows.length));
  const more = (list, what) => (list.more ? h('p', { class: 'foot-note' }, `Showing the first ${list.rows.length} ${what}.`) : '');
  const until = (b) => (b.permanent ? pill('permanent', 'bad') : [when(b.ends), ' ', b.active ? pill('active', 'bad') : pill('over')]);

  const head = HK.head({
    id: 'room-title',
    crumbs: [['Rooms', urls.rooms()], [name]],
    title: name,
    meta: [h('span', null, `Room #${room.id}`), doorPill(room.door), room.isPublic ? pill('public room', 'on') : '', room.staffPick ? pill('staff pick', 'on') : '',
      room.promoted ? pill('promoted', 'on') : '', room.players > 0 ? pill(`${room.players} in the room`, 'on') : '',
      room.description ? h('span', { class: 'motto chat-line' }, room.description) : ''],
    actions: [
      room.door === 'invisible' ? '' : h('a', { class: 'btn', href: urls.room_public(room.id) }, 'Public room page'),
      room.ownerId > 0 ? h('a', { class: 'btn', href: urls.user(room.ownerId) }, 'Owner') : '',
      r.can.logs ? h('a', { class: 'btn', href: urls.roomChatlog(room.id) }, 'Room chatlog') : ''],
  });

  const info = panel({ id: 'room-facts', title: 'Room', body: [HK.factsGrid([
    ['Owner', userLink(r.users, room.ownerId)],
    ['Group', room.group ? h('a', { href: `/groups/${room.group.id}/id` }, room.group.name) : h('span', { class: 'muted' }, 'none')],
    ['Category', room.category ?? h('span', { class: 'muted' }, `#${room.categoryId}`)],
    ['Users (last saved)', `${fmt.number(room.players)} / ${fmt.number(room.maxPlayers)}`],
    ['Trading', room.trading],
    ['Who can mute', room.whoCanMute],
    ['Who can kick', room.whoCanKick],
    ['Who can ban', room.whoCanBan],
    ['Model', [h('span', { class: 'mono' }, room.model), room.customFloor ? ' (custom floor plan)' : '']],
    ['Tags', room.tags || h('span', { class: 'muted' }, 'none')],
    ['Created', when(room.created)],
    room.promotion ? ['Promotion', [room.promotion.title, ' ', h('span', { class: 'muted' }, `until ${fmt.dateTime(room.promotion.ends)}`)]] : null,
  ]), h('p', { class: 'foot-note' }, r.note)] });

  let question = '';
  if (r.can.ask) {
    const status = h('div');
    const form = HK.jsonForm(h('form', { id: 'room-question-form', class: 'form-stack' },
      field({ id: 'room-question-text', name: 'question', label: 'Question', required: true, maxlength: r.question.max, autocomplete: 'off', placeholder: 'Shall we play a game?' }),
      field({ id: 'room-question-seconds', name: 'seconds', label: 'Seconds', type: 'number', required: true, min: r.question.seconds.min, max: r.question.seconds.max, step: 1, inputmode: 'numeric', value: String(r.question.seconds.default) }),
      h('div', null, h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Asking…' }, 'Ask'))), `/rooms/${room.id}/question`, {
      onDone: (d) => {
        HK.clear(status).append(d.command.status === 'pending'
          ? notice('The game has not picked the question up yet; it does so within a second while it runs.')
          : notice(d.command.result ?? (d.command.status === 'done' ? 'Asked.' : 'Not asked.'), d.command.status === 'done' ? 'ok' : 'warn'));
      },
    });
    question = panel({ id: 'room-question', title: 'Ask the room a question', body: [status, form,
      h('p', { class: 'hint' }, 'Everyone in the room gets a like and a dislike button; the answers show on the avatars and the result stays up when the time is over. Only an open room with people in it can be asked.')] });
  }

  const endsTable = (list, title, emptyText) => [h('h3', { class: 'sub-title' }, `${title} (${count(list)})`),
    list.rows.length === 0 ? HK.empty(emptyText) : table({ caption: title, head: ['User', 'Ends'], cls: ['nowrap', ''], rows: list.rows.map((b) => [userLink(r.users, b.userId), until(b)]) }),
    more(list, title.toLowerCase())];
  const access = panel({ id: 'room-access', title: 'Rights, bans and mutes', body: [
    h('div', null, h('h3', { class: 'sub-title' }, `Users with rights (${count(r.rights)})`),
      r.rights.rows.length === 0 ? HK.empty('Nobody has rights here.') : h('ul', { class: 'names' }, r.rights.rows.map((id) => h('li', null, userLink(r.users, id)))),
      more(r.rights, 'users with rights')),
    h('div', null, endsTable(r.bans, 'Room bans', 'Nobody is banned from this room.')),
    h('div', null, endsTable(r.mutes, 'Room mutes', 'Nobody is muted in this room.'))] });
  const visitors = panel({ title: 'Last visitors', body: table({
    caption: 'Last visitors', stack: true, main: 0, head: ['User', 'Entered', 'Left'], emptyText: 'No visits logged.', cls: ['nowrap', 'nowrap', 'nowrap grow'],
    rows: r.visits.map((v) => [userLink(r.users, v.userId), when(v.entered), v.left > 0 ? when(v.left) : h('span', { class: 'muted' }, 'not logged')]),
  }) });

  const tickets = r.tickets ? panel({ id: 'room-tickets', title: `Tickets for this room (${count(r.tickets)})`, body: [
    ticketTable({ tickets: r.tickets.rows, users: r.users, caption: 'Tickets for this room', emptyText: 'No tickets mention this room.' }), more(r.tickets, 'tickets')] }) : '';
  const raw = h('details', { class: 'panel', id: 'room-raw' }, h('summary', null, `All room data (${r.raw.length} columns)`),
    h('div', { class: 'panel__body' }, h('p', { class: 'foot-note' }, 'Every column of the room as the database has it (the password is never shown).'),
      HK.factsGrid(r.raw.map(([c, v]) => [c, v === '' || v == null ? h('span', { class: 'muted' }, 'empty') : v]), { raw: true })));

  main.append(head, cols('', [info, visitors, raw], [question, access]), tickets);
}));
