'use strict';

HK.start((main) => {
  const q = new URLSearchParams(location.search);
  const asked = (q.get('q') ?? '').trim();
  const api = `/users${HK.qs({ q: asked, page: q.get('page') })}`;
  return HK.load(main, api, (r) => {
    const { panel, pill, fmt } = HK;
    HK.setTitle('Users');
    const form = HK.searchForm({ fields: [{ name: 'q', label: 'Name or id', value: r.q, placeholder: 'Start of a name, or an id' }], submit: 'Find user' });
    const missing = q.get('missing') === '1'
      ? HK.notice(asked ? `No user is called exactly “${asked}”. Users whose name starts with it:` : 'No user name was given.', 'warn') : '';
    main.append(
      HK.head({ id: 'users-title', crumbs: r.q ? [['Users', HK.urls.users()], [`“${r.q}”`]] : [], title: 'Users' }),
      form,
      missing,
      panel({
        id: 'search', title: r.q ? `Names starting with “${r.q}”${r.id != null ? ` or id ${r.id}` : ''}` : 'Newest accounts',
        body: [HK.table({
          caption: 'Users', head: ['Name', 'Id', 'Rank', 'Online', 'Last online', 'Created'], emptyText: r.q ? 'No user matches.' : 'No users yet.',
          cls: ['nowrap', 'num', '', 'nowrap', 'nowrap', 'nowrap'],
          rows: r.rows.map((u) => [HKUser.userLink(u), String(u.id), u.rankName ? `${u.rankName} (${u.rank})` : `Rank ${u.rank}`,
            u.online ? pill('Online now', 'on') : pill('Offline'), fmt.dateTime(u.lastOnline), fmt.dateTime(u.created)]),
        }), HK.pager({ page: r.page, hasNext: r.hasNext })],
      }));
  });
});
