'use strict';

HK.start((main) => {
  const { h, panel, cols, pill, fmt, urls } = HK;
  const { userLink, banEnds } = HKUser;
  const NOT_HERE = 'Only account bans are made here. Alerts, kicks, mutes, trade locks and tickets are not here: they need the player’s game connection. Use the in-client mod tool for them.';
  const wanted = (new URLSearchParams(location.search).get('user') ?? '').trim();

  const bansTable = (bans) => HK.table({
    caption: 'Active bans', head: ['Ban', 'Covers', 'Reason', 'By', 'Since', 'Ends', ''], emptyText: 'No active bans.', main: 1,
    cls: ['nowrap', 'nowrap', '', 'nowrap', 'nowrap', 'nowrap', 'nowrap'],
    rows: bans.map((b) => [`#${b.id}`, b.covers, b.reason, userLink(b.staff), fmt.dateTime(b.bannedAt), banEnds(b), b.canLift ? h('a', { href: urls.unban(b.id) }, 'Unban') : '']),
  });
  const finder = (error) => [
    HK.head({ id: 'ban-title', crumbs: [['Users', urls.users()], ['Ban']], title: 'Ban a user' }),
    panel({ id: 'find', title: 'Who', body: [
      error ? h('p', { class: 'alert', role: 'alert' }, error) : '',
      HK.searchForm({ fields: [{ name: 'user', label: 'User (name or id)', value: wanted }], submit: 'Open' })] })];

  const draw = (r, refusal) => {
    const t = r.target;
    HK.setTitle(`Ban ${t.name}`);
    const form = refusal ? h('p', { class: 'alert', role: 'alert' }, refusal) : HK.jsonForm(h('form', { class: 'form-stack' },
      h('input', { type: 'hidden', name: 'user', value: String(t.id) }),
      HK.field({ id: 'ban-hours', name: 'hours', label: 'Length', type: 'select', value: '24', options: r.lengths.map((l) => [l.value, l.label]) }),
      HK.field({ id: 'ban-reason', name: 'reason', label: 'Reason', required: true, maxlength: r.reasonMax, autocomplete: 'off',
        hint: `1 to ${r.reasonMax} characters, kept in the ban list and the audit log. Letters from Western European languages only: no emoji or other scripts.` }),
      h('div', null, h('button', { class: 'btn btn--danger', type: 'submit', 'data-busy': 'Banning…' }, `Ban ${t.name}`))), '/actions/ban', {
      onDone: (d) => location.assign(`${urls.userSanctions(d.userId)}?done=ban`),
    });
    main.append(
      HK.head({
        id: 'ban-title', crumbs: [['Users', urls.users()], [t.name, urls.user(t.id)], ['Ban']], title: `Ban ${t.name}`,
        meta: [h('span', null, `User #${t.id}`), pill(t.rankName ?? String(t.rank)), t.online ? pill('Online now', 'on') : pill('Offline'), h('span', null, `${r.bans.length} active ${r.bans.length === 1 ? 'ban' : 'bans'}`)],
      }),
      cols('', panel({ id: 'ban', title: 'Account ban', tone: 'red', body: [
        form] }),
      panel({ id: 'active', title: `Active bans (${r.bans.length})`, more: ['All bans and sanctions', urls.userSanctions(t.id)], body: bansTable(r.bans) })),
      h('p', { class: 'foot-note' }, NOT_HERE));
  };

  if (!wanted) {
    HK.setTitle('Ban a user');
    HK.clear(main).append(...finder(), h('p', { class: 'foot-note' }, NOT_HERE));
    return;
  }
  return HK.api(`/actions/ban?user=${encodeURIComponent(wanted)}`).then((r) => {
    HK.clear(main);
    if (r.ok) return draw(r.data);
    if (r.status === 403 && r.data.target) return draw({ ...r.data, lengths: [], reasonMax: 0 }, r.data.error);
    if (r.status === 404) { HK.setTitle('Ban a user'); main.append(...finder(r.data.error), h('p', { class: 'foot-note' }, NOT_HERE)); return; }
    return HK.showError(main, r);
  }, () => HK.showError(main, { status: 0, data: {} }));
});
