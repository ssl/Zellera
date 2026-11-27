'use strict';

HK.start((main) => HK.load(main, `/users/${HK.param(2)}/sanctions`, (r) => {
  const { h, panel, pill, fmt, urls } = HK;
  const { userHead, userLink, banEnds, logTable } = HKUser;
  HK.setTitle(`${r.user.name} · Sanctions`);
  const now = Math.floor(Date.now() / 1000);
  const DONE = {
    ban: 'Banned.',
    unban: 'Unbanned. They can log in again.',
  };
  const done = DONE[new URLSearchParams(location.search).get('done')];
  const st = r.standing;
  const s = (k) => (st ? fmt.number(st[k]) : '-');
  main.append(
    userHead(r.user, 'sanctions', {
      actions: r.can.ban ? [h('a', { class: 'btn btn--danger', href: urls.ban(r.user.id) }, `Ban ${r.user.name}`)] : [],
      meta: [st?.tradeLockedUntil ? pill(`Trade locked until ${fmt.dateTime(st.tradeLockedUntil)}`, 'warn') : '', st?.mutedUntil ? pill('Muted', 'warn') : ''],
    }),
    done ? HK.notice(done, 'ok') : '',
    HK.figures([
      [s('reportsSent'), 'Calls for help sent'], [s('reportsAbusive'), 'Abusive calls for help'], [s('cautions'), 'Cautions'],
      [s('bansCounted'), 'Bans (counter)'], [s('tradeLocks'), 'Trade locks']], 'Counters'),
    panel({ id: 'bans', title: `Bans (${fmt.number(r.bans.length)})`, tone: 'red', body: HK.table({
      caption: 'Bans', head: ['Id', 'Covers', 'Reason', 'By', 'Start', 'Expires', 'State', ''], emptyText: 'Never banned.', main: 1,
      cls: ['num', 'nowrap', '', 'nowrap', 'nowrap', 'nowrap', 'nowrap', 'nowrap'],
      rows: r.bans.map((b) => [String(b.id), b.covers, b.reason || '-', userLink(b.staff, 'system'), fmt.dateTime(b.bannedAt), banEnds(b),
        b.active ? pill('Active', 'bad') : pill(b.lifted ? 'Lifted' : 'Ended'), b.canLift ? h('a', { href: urls.unban(b.id) }, 'Unban') : '']),
    }) }),
    panel({ title: 'Sanction ladder', body: [
      h('p', { class: 'foot-note' }, "Steps the in-client mod tool's Default sanction gave (ALERT, MUTE, BAN). The next one goes one step up while the probation runs. The caution, mute, ban and trade lock buttons act directly and are no step."),
      HK.table({
        caption: 'Sanctions', head: ['Id', 'Level', 'Type', 'Length', 'Reason', 'Probation until', 'Mute'], emptyText: 'No sanctions.', main: 2,
        cls: ['num', 'num', 'nowrap', 'nowrap', '', 'nowrap', 'nowrap'],
        rows: r.sanctions.map((x) => [String(x.id), String(x.level), x.kind ?? '-', x.hours != null ? `${fmt.number(x.hours)} h` : '-', x.reason || '-',
          x.probationEndsAt > 0 ? [fmt.dateTime(x.probationEndsAt), ' ', x.probationEndsAt > now ? pill('running', 'warn') : ''] : '-',
          x.muted ? (x.muteUntil > 0 ? `until ${fmt.dateTime(x.muteUntil)}` : 'yes') : '-']),
      })] }),
    panel({ title: 'Rank and permission changes', body: HK.table({
      caption: 'Rank and permission changes', head: ['When', 'By', 'Change', 'What', 'From', 'To', 'Via'], emptyText: 'No rank or permission changes.', main: 3,
      cls: ['nowrap', 'nowrap', 'nowrap', '', '', '', 'nowrap'],
      rows: r.permissionChanges.map((p) => [fmt.dateTime(p.at), userLink(p.actor, 'system'), h('span', { class: 'mono' }, p.action), p.subject || '-', p.old || '-', p.new || '-', p.via || '-']),
    }) }),
    r.log ? panel({ title: 'Housekeeping log about this user', body: logTable(r.log) }) : '',
    h('p', { class: 'foot-note' }, 'Cautions, mutes and trade locks are given in the in-client mod tool.'));
}));
