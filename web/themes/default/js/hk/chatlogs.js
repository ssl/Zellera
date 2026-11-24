'use strict';

HK.start(async (main) => {
  const { h, panel, notice, alertBox, table, pill, pager, fmt, urls } = HK;
  HK.setTitle('Chatlogs');
  let r;
  try { r = await HK.api(HKRooms.apiPath()); } catch { r = { status: 0, ok: false, data: {} }; }
  if (!r.ok && !r.data.search) return HK.showError(main, r);
  const { search, limits, results } = r.data;
  const form = HK.searchForm({
    submit: 'Search chat',
    fields: [
      { name: 'user', label: 'User (name or id)', value: search.user },
      { name: 'room', label: 'Room id', value: search.room, inputmode: 'numeric', placeholder: '123' },
      { name: 'q', label: 'Text contains', value: search.q, placeholder: `min. ${limits.minText} characters` },
      { name: 'from', label: 'From (UTC)', value: search.from, type: 'date' },
      { name: 'to', label: 'To (UTC)', value: search.to, type: 'date' },
    ],
  });
  HK.clear(main).append(
    HK.head({ id: 'chat-title', title: 'Chatlogs', lead: `Room chat and whispers (private messages show only inside a ticket). Give a user (the messages they sent), a room, some text, or a mix. Dates are UTC days: the last ${limits.defaultDays} days unless you pick others, at most ${limits.maxDays} days at a time. Every search is written to the audit log with what you searched for.` }),
    form);
  if (!r.ok) { main.append(...(r.data.errors ?? [r.data.error]).map((e) => alertBox(e))); return; }
  if (!results) { main.append(notice('Fill in at least one of: user, room id or text.')); return; }

  const userCell = (id, name) => (id > 0 ? h('a', { href: urls.user(id) }, name ?? `#${id}`) : h('span', { class: 'muted' }, name ?? 'nobody'));
  const roomCell = (m) => {
    if (!(m.roomId > 0)) return h('span', { class: 'muted' }, 'no room');
    const label = m.room != null ? `${m.room} (#${m.roomId})` : `Room #${m.roomId} (deleted)`;
    return results.canRoom ? h('a', { href: urls.room(m.roomId) }, label) : label;
  };
  const u = results.user;
  const links = [
    u && h('a', { class: 'btn', href: urls.user(u.id) }, `User ${u.name}`),
    u && h('a', { class: 'btn', href: urls.userChatlog(u.id) }, `All chat by ${u.name}`),
    results.roomId && results.canRoom && h('a', { class: 'btn', href: urls.room(results.roomId) }, `Room #${results.roomId}`),
    results.roomId && h('a', { class: 'btn', href: urls.roomChatlog(results.roomId) }, `All chat in room #${results.roomId}`),
  ].filter(Boolean);
  const first = (results.page - 1) * results.perPage + 1;
  main.append(
    results.roomId && !results.roomExists ? notice(`Room #${results.roomId} does not exist (any more); its logged chat is still searched.`, 'warn') : '',
    panel({ id: 'res', title: 'Messages', body: [
      h('p', { class: 'foot-note' }, results.rows.length === 0 ? 'No messages match.'
        : `Messages ${first} to ${first + results.rows.length - 1}, newest first, ${results.from} to ${results.to} (UTC).`),
      links.length ? h('p', { class: 'row' }, links) : '',
      table({
        caption: 'Chat messages', head: ['Time', 'Room', 'From', 'To', 'Message'], cls: ['nowrap', 'nowrap', 'nowrap', 'nowrap', 'grow'],
        emptyText: 'No messages match. Try a longer range or fewer criteria.',
        rows: results.rows.map((m) => [
          fmt.dateTime(m.at), roomCell(m), userCell(m.senderId, m.from),
          m.targetId > 0 ? [pill('whisper', 'warn'), ' ', userCell(m.targetId, m.to)] : '',
          h('span', { class: 'chat-line' }, m.message),
        ]),
      }),
      pager({ page: results.page, hasNext: results.hasNext })] }));
});
