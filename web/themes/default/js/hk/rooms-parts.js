'use strict';

(() => {
  const { h, pill, fmt, table, urls } = HK;

  const when = (unix) => (Number(unix) > 0
    ? h('time', { datetime: new Date(Number(unix) * 1000).toISOString(), title: fmt.ago(unix) }, fmt.dateTime(unix))
    : h('span', { class: 'muted' }, '-'));

  const userLink = (users, id, { none = '-' } = {}) => {
    const n = Number(id);
    if (!(n > 0)) return h('span', { class: 'muted' }, none);
    const u = users?.[n];
    if (!u) return h('span', { class: 'muted' }, `#${n} (deleted)`);
    return h('a', { href: urls.user(n) }, u.name);
  };

  const roomLink = (rooms, id, { none = '-' } = {}) => {
    const n = Number(id);
    if (!(n > 0)) return h('span', { class: 'muted' }, none);
    const r = rooms?.[n];
    if (!r) return h('span', { class: 'muted' }, `#${n} (deleted)`);
    return [h('a', { href: urls.room(n) }, r.name || `Room ${n}`), ' ', h('span', { class: 'muted' }, `#${n}`)];
  };

  const DOOR = { open: ['Open', 'on'], locked: ['Doorbell', 'warn'], password: ['Password', 'warn'], invisible: ['Invisible', 'bad'] };
  const doorPill = (state) => {
    const [label, kind] = DOOR[state] ?? [String(state || '?'), ''];
    return pill(label, kind);
  };

  const STATE_KIND = { open: 'warn', picked: 'on', closed: '', unknown: 'bad' };
  const statePill = (state) => pill(state.label, STATE_KIND[state.key] ?? '');
  const typeText = (type, { short = false } = {}) => (short
    ? h('span', { title: type.code || type.note }, type.words)
    : [type.words, type.code ? [' ', h('span', { class: 'mono muted' }, type.code)] : '', type.note ? [' ', h('span', { class: 'muted' }, `(${type.note})`)] : '']);
  const topicText = (topic, { short = false } = {}) => (short || !topic.note ? topic.label : [topic.label, ' ', h('span', { class: 'muted' }, `(${topic.note})`)]);
  const excerpt = (s, max = 90) => {
    const text = String(s ?? '').replace(/\s+/g, ' ').trim();
    return text.length > max ? `${text.slice(0, max - 1)}…` : text;
  };

  const ticketTable = ({ tickets, users, rooms = null, caption = 'Tickets', emptyText = 'No tickets.' }) => table({
    caption, emptyText,
    head: ['Ticket', 'State', 'Type', 'Topic', 'Reported by', 'Reported user', ...(rooms ? ['Room'] : []), 'Moderator', 'Sent', 'Message'],
    cls: ['nowrap', 'nowrap', 'nowrap', '', 'nowrap', 'nowrap', ...(rooms ? ['nowrap'] : []), 'nowrap', 'nowrap', 'excerpt grow'],
    rows: tickets.map((t) => [
      h('a', { href: urls.ticket(t.id) }, `#${t.id}`),
      statePill(t.state),
      typeText(t.type, { short: true }),
      topicText(t.topic, { short: true }),
      userLink(users, t.senderId, { none: 'System' }),
      userLink(users, t.reportedId),
      ...(rooms ? [roomLink(rooms, t.roomId)] : []),
      userLink(users, t.moderatorId, { none: 'Nobody yet' }),
      h('span', { title: fmt.dateTime(t.created) }, fmt.ago(t.created)),
      excerpt(t.issue),
    ]),
  });

  const chatTable = ({ lines, users, rooms = null, isPrivate = false, caption = 'Chat', emptyText = 'No chat in this range.' }) => table({
    caption, emptyText,
    head: ['Time (UTC)', 'From', ...(isPrivate ? ['To'] : []), ...(rooms ? ['Room'] : []), 'Message'],
    cls: ['nowrap', 'nowrap', ...(isPrivate ? ['nowrap'] : []), ...(rooms ? ['nowrap'] : []), 'grow'],
    rows: lines.map((r) => {
      if (r.marker) return [when(r.at), pill(r.marker, 'warn'), ...(isPrivate ? [''] : []), ...(rooms ? [''] : []), h('span', { class: 'muted' }, r.text)];
      const to = Number(r.targetId);
      return [
        when(r.at),
        userLink(users, r.senderId, { none: 'System' }),
        ...(isPrivate ? [userLink(users, to)] : []),
        ...(rooms ? [roomLink(rooms, r.roomId, { none: 'No room' })] : []),
        h('span', { class: 'chat-line' }, !isPrivate && to > 0 ? [pill('whisper'), ' ', h('span', { class: 'muted' }, 'to'), ' ', userLink(users, to), ': '] : '', r.message),
      ];
    }),
  });

  const apiPath = () => `${location.pathname.replace(/^\/housekeeping/, '')}${location.search}`;

  window.HKRooms = { when, userLink, roomLink, doorPill, statePill, typeText, topicText, ticketTable, chatTable, apiPath };
})();
