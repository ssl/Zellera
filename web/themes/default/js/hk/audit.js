'use strict';

HK.start((main) => {
  const { h, panel, fmt } = HK;
  const { userLink, targetCell, okPill } = HKUser;
  const q = new URLSearchParams(location.search);
  const tab = q.get('tab') === 'permissions' ? 'permissions' : 'log';
  const filter = { actor: q.get('actor') ?? '', target: q.get('target') ?? '', action: q.get('action') ?? '' };
  HK.setTitle('Audit log');

  const tabHref = (key) => {
    const p = new URLSearchParams();
    for (const [k, v] of Object.entries(filter)) if (v.trim()) p.set(k, v.trim());
    if (key !== 'log') p.set('tab', key);
    const s = p.toString();
    return `${location.pathname}${s ? `?${s}` : ''}`;
  };
  const form = h('form', { class: 'search-form', method: 'get', action: location.pathname, role: 'search' },
    tab === 'permissions' ? h('input', { type: 'hidden', name: 'tab', value: 'permissions' }) : '',
    [['actor', 'Staff (name or id)', ''], ['target', 'Target (user name or id)', ''], ['action', 'Action starts with', tab === 'log' ? 'hk.ban' : 'rank.change']]
      .map(([name, label, placeholder]) => h('div', { class: 'field' }, h('label', { for: `f-${name}` }, label),
        h('input', { id: `f-${name}`, name, type: 'search', value: filter[name], placeholder: placeholder || null, autocomplete: 'off', spellcheck: 'false' }))),
    h('button', { class: 'btn btn--go', type: 'submit' }, 'Filter'));
  const top = [HK.head({
    id: 'audit-title', title: 'Audit log',
    lead: tab === 'log'
      ? 'Every ban and unban made here, every chatlog search, every look at IPs, chatlogs or a private-message ticket, and the staff actions the game itself logs. Times are UTC.'
      : 'Ranks given to users and permission values changed, as the game logged them. Times are UTC.',
    tabs: HK.pageTabs([['log', 'Housekeeping log', tabHref('log')], ['permissions', 'Rank and permission changes', tabHref('permissions')]], tab, 'Logs'),
  }), form];

  const list = (r) => (tab === 'log'
    ? HK.table({
      caption: 'Housekeeping log', head: ['Time', 'Staff', 'Action', 'Target', 'Detail', 'Result'], emptyText: 'No rows match.', main: 2,
      cls: ['nowrap', 'nowrap', 'nowrap', 'nowrap', 'grow', 'nowrap'],
      rows: r.rows.map((a) => [fmt.dateTime(a.at), userLink(a.actor, 'system'), h('span', { class: 'mono' }, a.action), targetCell(a.target), a.detail, okPill(a.success)]),
    })
    : HK.table({
      caption: 'Rank and permission changes', head: ['Time', 'By', 'Change', 'Target', 'What', 'Old', 'New', 'Via'], emptyText: 'No rows match.',
      cls: ['nowrap', 'nowrap', 'nowrap', 'nowrap', 'grow', '', '', 'nowrap'],
      rows: r.rows.map((a) => [fmt.dateTime(a.at), userLink(a.actor, 'system'), h('span', { class: 'mono' }, a.action),
        a.target.type === 'rank' ? [h('span', { class: 'muted' }, 'rank'), ' ', a.target.label ? `${a.target.label} (${a.target.id})` : `#${a.target.id}`] : targetCell(a.target),
        h('span', { class: 'mono' }, a.subject), a.old, a.new, a.via]),
    }));

  const api = `/audit${HK.qs({ tab: tab === 'log' ? '' : tab, ...filter, page: q.get('page') })}`;
  return HK.api(api).then((r) => {
    HK.clear(main).append(...top);
    if (!r.ok) {
      if (r.status === 400) { main.append(h('p', { class: 'alert', role: 'alert' }, r.data.error)); return; }
      return HK.showError(main, r);
    }
    main.append(...r.data.warnings.map((w) => HK.notice(w, 'warn')),
      panel({ title: tab === 'log' ? 'Housekeeping log' : 'Rank and permission changes', body: [list(r.data), HK.pager({ page: r.data.page, hasNext: r.data.hasNext })] }));
  }, () => HK.showError(main, { status: 0, data: {} }));
});
