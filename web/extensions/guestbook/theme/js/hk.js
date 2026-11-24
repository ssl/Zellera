'use strict';

HK.start((main) => HK.load(main, `/ext/guestbook/entries${HK.query()}`, (r) => {
  const { h, head, table, pager, fmt, urls, empty, busy } = HK;
  HK.setTitle('Guestbook');
  const who = (p) => (p.name ? h('a', { href: urls.profile(p.name) }, p.name) : `#${p.id}`);
  const remove = async (e, row, btn) => {
    if (!window.confirm(`Delete this message on ${e.owner.name ?? `#${e.owner.id}`}'s profile?`)) return;
    const restore = busy(btn);
    const res = await HK.post(`/ext/guestbook/entries/${e.id}/delete`).catch(() => ({ ok: false, data: { error: HK.NETWORK_ERROR } }));
    if (!res.ok) { restore(); window.alert(res.data.error || 'Could not delete it.'); return; }
    row.remove();
  };
  main.append(
    head({ title: 'Guestbook', meta: r.total ? `${fmt.number(r.total)} messages on profiles` : null,
      lead: 'Messages players leave on each other\'s profile. Deleting one is logged in the audit log.' }),
    r.entries.length === 0 ? empty(r.page > 1 ? 'No messages on this page.' : 'No one has written in a guestbook yet.') : '');
  if (r.entries.length) {
    const t = table({
      caption: 'Guestbook messages', head: ['When', 'Profile', 'Writer', 'Message', ''], cls: ['nowrap', 'nowrap', 'nowrap', 'excerpt', 'nowrap'],
      rows: r.entries.map((e) => [fmt.dateTime(e.created), who(e.owner), who(e.author), e.message,
        h('button', { class: 'btn btn--small', type: 'button', 'data-busy': 'Deleting…', 'data-entry': String(e.id) }, 'Delete')]),
    });
    t.addEventListener('click', (ev) => {
      const btn = ev.target.closest('button[data-entry]');
      if (!btn) return;
      const e = r.entries.find((x) => String(x.id) === btn.dataset.entry);
      remove(e, btn.closest('tr'), btn);
    });
    main.append(t);
  }
  main.append(pager({ page: r.page, hasNext: r.hasNext }));
}));
