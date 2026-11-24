'use strict';

(() => {
  const { h, fmt, pill, urls } = HK;
  const can = (key) => Boolean(HK.me?.can?.[key]);

  const userLink = (u, none = '-') => (u && u.id > 0 ? h('a', { href: urls.user(u.id) }, u.name || `#${u.id}`) : (u?.name ? h('span', { class: 'muted' }, u.name) : none));
  const roomLink = (r) => {
    if (!r || !(r.id > 0)) return '-';
    const label = r.name ? `${r.name} (#${r.id})` : `#${r.id}`;
    return can('moderation.room.info') ? h('a', { href: urls.room(r.id) }, label) : label;
  };
  const targetCell = (t) => {
    const n = t?.id ?? 0;
    const text = t?.label || (n > 0 ? `#${n}` : '-');
    if (n > 0) {
      if (t.type === 'user') return h('a', { href: urls.user(n) }, text);
      if (t.type === 'room') return roomLink({ id: n, name: t.label });
      if (t.type === 'ticket' && can('moderation.tickets')) return h('a', { href: urls.ticket(n) }, `Ticket #${n}`);
      if (t.type === 'photo' && can('moderation.tickets')) return h('a', { href: urls.photo(n) }, `Photo #${n}`);
    }
    return [h('span', { class: 'muted' }, t?.type ?? ''), ' ', text];
  };
  const when = (unix) => (unix > 0 ? [h('span', { class: 'nowrap' }, fmt.dateTime(unix)), ' ', h('span', { class: 'muted ago' }, `(${fmt.ago(unix)})`)] : '-');
  const banEnds = (b) => (b.permanent ? 'Permanent' : fmt.dateTime(b.expiresAt));
  const duration = (seconds) => {
    const s = Math.max(0, Number(seconds) || 0);
    if (s < 60) return `${s} s`;
    const hours = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    return hours > 0 ? `${fmt.number(hours)} h ${m} min` : `${m} min`;
  };
  const okPill = (ok) => (ok ? pill('ok', 'on') : pill('failed', 'bad'));

  const logTable = (rows, { emptyText = 'Nothing logged yet.', caption = 'Housekeeping log' } = {}) => HK.table({
    caption, emptyText, main: 2,
    head: ['When', 'Staff', 'Action', 'Target', 'Detail', 'Result'],
    cls: ['nowrap', 'nowrap', 'nowrap', 'nowrap', 'grow', 'nowrap'],
    rows: rows.map((r) => [fmt.dateTime(r.at), userLink(r.actor, 'system'), h('span', { class: 'mono' }, r.action), targetCell(r.target), r.detail || '-', okPill(r.success)]),
  });

  const TABS = [
    ['detail', 'Overview', (id) => urls.user(id), 'moderation.tool'],
    ['sanctions', 'Sanctions', (id) => urls.userSanctions(id), 'moderation.tool'],
    ['chatlog', 'Chatlog', (id) => urls.userChatlog(id), 'moderation.player.chatlog'],
    ['visits', 'Room visits', (id) => urls.userVisits(id), 'moderation.player.chatlog'],
    ['devices', 'Devices', (id) => urls.userDevices(id), 'moderation.tool'],
    ['identity', 'Identity', (id) => urls.identity(id), 'moderation.player.ban'],
  ];
  const userTabs = (id, active) => HK.pageTabs(TABS.filter(([, , , key]) => can(key)).map(([key, label, href]) => [key, label, href(id)]), active, 'User pages');

  const userHead = (u, active, { actions = [], meta = [], lead } = {}) => {
    const tab = TABS.find(([key]) => key === active);
    return HK.head({
      id: 'user-title',
      crumbs: [['Users', urls.users()], ...(active === 'detail' ? [[u.name]] : [[u.name, urls.user(u.id)], [tab?.[1] ?? '']])],
      picture: HK.avatar(u),
      title: u.name,
      meta: [h('span', null, `User #${u.id}`), u.rankName ? pill(u.rankName) : '', meta],
      actions,
      lead,
      tabs: userTabs(u.id, active),
    });
  };

  const cursorPager = ({ cursor, next }) => {
    if (!cursor && !next) return '';
    return h('nav', { class: 'pager', 'aria-label': 'Pages' },
      cursor ? h('a', { class: 'btn btn--small', href: location.pathname }, 'Newest') : h('span'),
      h('span', null, cursor ? `From ${fmt.dateTime(cursor.before)} back` : 'Newest first'),
      next ? h('a', { class: 'btn btn--small', href: `${location.pathname}?${new URLSearchParams({ before: String(next.before), bid: String(next.bid) })}`, rel: 'next' }, 'Older') : h('span'));
  };
  const cursorQuery = () => {
    const q = new URLSearchParams(location.search);
    const out = new URLSearchParams();
    for (const k of ['before', 'bid']) if (/^\d{1,10}$/.test(q.get(k) ?? '')) out.set(k, q.get(k));
    const s = out.toString();
    return s ? `?${s}` : '';
  };

  window.HKUser = { can, userLink, roomLink, targetCell, when, banEnds, duration, okPill, logTable, userHead, cursorPager, cursorQuery };
})();
