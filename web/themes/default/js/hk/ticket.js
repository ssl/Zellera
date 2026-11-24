'use strict';

HK.start((main) => HK.load(main, HKRooms.apiPath(), (r) => {
  const { h, panel, cols, facts, pill, notice, empty, table, fmt, urls } = HK;
  const { userLink, roomLink, when, statePill, typeText, topicText, chatTable } = HKRooms;
  const t = r.ticket;
  const c = r.context;
  HK.setTitle(`Ticket ${t.id}`);

  const links = [
    t.roomId > 0 && r.can.roomLogs ? h('a', { class: 'btn', href: urls.roomChatlog(t.roomId, { before: r.chatBefore }) }, 'Room chatlog at that time') : '',
    t.reportedId > 0 && r.can.userLogs ? h('a', { class: 'btn', href: urls.userChatlog(t.reportedId, { before: r.chatBefore }) }, "Reported user's chatlog") : '',
  ].filter(Boolean);

  const about = () => {
    if (c.kind === 'none') return empty(c.empty);
    if (c.kind === 'photo') return h('p', null, h('a', { class: 'btn btn--go', href: urls.photo(c.photoItemId) }, `Open the photo (item #${c.photoItemId})`));
    if (c.kind === 'forum') {
      const th = c.thread;
      return [
        facts([
          ['Thread', [th.subject, ' ', pill(th.state), th.locked ? [' ', pill('locked')] : '']],
          ['Group', th.group != null ? h('a', { href: `/groups/${th.groupId}/id` }, th.group) : h('span', { class: 'muted' }, `#${th.groupId} (gone)`)],
          ['Started by', userLink(r.users, th.authorId)],
          ['Started', when(th.created)],
          ['Messages', fmt.number(th.posts)],
        ]),
        table({
          caption: 'Forum messages', head: ['Time (UTC)', 'Author', 'State', 'Message'], emptyText: 'The thread has no messages.', cls: ['nowrap', 'nowrap', 'nowrap', 'grow'],
          rows: c.comments.map((m) => [when(m.at), userLink(r.users, m.authorId), pill(m.state), h('span', { class: 'chat-line' }, m.reported ? [pill('reported', 'bad'), ' '] : '', m.message)]),
        }),
        h('p', { class: 'foot-note' }, c.note),
      ];
    }
    return [
      c.warn ? notice(c.warn, 'warn') : '',
      h('p', { class: 'foot-note' }, c.note),
      chatTable({ lines: c.lines, users: r.users, rooms: c.showRooms ? r.rooms : null, isPrivate: Boolean(c.private), caption: c.title, emptyText: c.empty }),
    ];
  };

  main.append(
    HK.head({
      id: 'ticket-title',
      crumbs: [['Tickets', urls.tickets(t.state.key === 'open' || t.state.key === 'unknown' ? undefined : t.state.key)], [`#${t.id}`]],
      title: `Ticket #${t.id}`,
      meta: [statePill(t.state), h('span', null, t.type.words), h('span', null, `sent ${fmt.ago(t.created)}`)],
      actions: links,
    }),
    cols(c.kind === 'photo' || c.kind === 'none' ? 'halves' : 'side-left',
      [
        panel({ id: 'ticket-message', title: 'Message', body: t.issue.trim() ? h('p', { class: 'message' }, t.issue) : empty('No message.') }),
        panel({ id: 'ticket-facts', title: 'Ticket', body: facts([
          ['Type', typeText(t.type)],
          ['Topic', topicText(t.topic)],
          ['Reported by', userLink(r.users, t.senderId, { none: 'System' })],
          ['Reported user', userLink(r.users, t.reportedId)],
          ['Room', roomLink(r.rooms, t.roomId, { none: 'No room' })],
          ['Moderator', userLink(r.users, t.moderatorId, { none: 'Nobody yet' })],
          ['Sent', when(t.created)],
          ['Closed', t.closed > 0 ? when(t.closed) : h('span', { class: 'muted' }, 'not yet')],
          ['Priority', fmt.number(t.score)],
          ['Sanctioned', t.sanctioned],
          ['Appeal', t.appeal ? [t.appeal.label, t.appeal.filed > 0 ? `, filed ${fmt.dateTime(t.appeal.filed)}` : ''] : 'none'],
        ]) }),
      ],
      panel({ id: 'ticket-context', title: c.title, body: about() })),
    h('p', { class: 'foot-note' }, r.note));
}));
