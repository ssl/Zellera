'use strict';

HK.start((main) => {
  const id = HK.param(2);
  const { h, panel, pill, fmt, urls } = HK;
  const { when, roomLink, banEnds, duration } = HKUser;

  const TYPE_LABEL = {
    setCurrency: 'Set balance', addCurrency: 'Add to balance', giveBadge: 'Give badge', takeBadge: 'Take badge', setMotto: 'Motto', setRank: 'Level', setPrivacy: 'Privacy',
  };
  const ONLINE_NOTE = 'The game applies it within a second, also while they are online; while it is off, when it starts.';
  const statePill = (s) => (s === 'done' ? pill('done', 'on') : s === 'failed' ? pill('failed', 'bad') : pill('waiting', 'warn'));
  const commandLine = (c) => `Command #${c.id} (${TYPE_LABEL[c.kind] ?? c.kind})`;
  const sentNotice = (c) => (c.status === 'done' ? HK.notice(`${commandLine(c)}: done. ${c.result ?? ''}`, 'ok')
    : c.status === 'failed' ? h('p', { class: 'alert', role: 'alert' }, `${commandLine(c)}: not done. ${c.result ?? ''}`)
      : HK.notice(`${commandLine(c)}: waiting for the game. It applies it within a second while it runs; if it is off, as soon as it starts. Reload this page to see it.`, 'warn'));

  const reloadWith = (data) => {
    if (data.unchanged) { render('', data.message); return; }
    const ids = (data.commands ?? []).map((c) => c.id).join(',');
    history.replaceState(null, '', `${location.pathname}${ids ? `?commands=${ids}` : ''}`);
    render(ids ? `?commands=${ids}` : '');
  };

  const otherAvatars = (list) => (list.length === 0 ? h('span', { class: 'muted' }, 'None')
    : h('span', null, list.map((a, i) => [i ? ', ' : '', h('a', { href: urls.user(a.id) }, a.name), a.online ? ' (online)' : ''])));

  const draw = (d, message) => {
    const u = d.user;
    HK.setTitle(u.name);
    const rankText = u.rankName ? `${u.rankName} (${u.rank})` : `Rank ${u.rank}`;
    const st = d.standing;
    const s = (k) => (st ? fmt.number(st[k]) : '-');
    const head = HKUser.userHead(u, 'detail', {
      actions: [h('a', { class: 'btn', href: urls.profile(u.name) }, 'Public profile'), d.can.ban ? h('a', { class: 'btn btn--danger', href: urls.ban(u.id) }, 'Ban') : ''],
      meta: [u.online ? pill('Online now', 'on') : pill('Offline'), d.ban ? pill('Banned', 'bad') : '',
        st?.safetyLocked ? pill('Safety locked', 'warn') : '', st?.mutedUntil ? pill('Muted', 'warn') : '', st?.tradeLockedUntil ? pill('Trade locked', 'warn') : '',
        u.motto ? h('span', { class: 'motto' }, u.motto) : ''],
    });

    const commands = d.commands.sent.length || d.commands.recent.length ? panel({ id: 'commands', title: 'Changes sent to the game', body: [
      d.commands.sent.map(sentNotice),
      HK.table({
        caption: 'Changes sent to the game', main: 1, head: ['Command', 'What', 'State', 'Result', 'Applied'], emptyText: 'None yet.', cls: ['nowrap', 'nowrap', 'nowrap', '', 'nowrap'],
        rows: d.commands.recent.map((c) => [`#${c.id}`, TYPE_LABEL[c.kind] ?? c.kind, statePill(c.status), c.result || '-', c.processedAt ? fmt.dateTime(c.processedAt) : '-']),
      })] }) : '';

    const tallies = HK.figures([
      ...d.wallet.map((c) => [c.amount, c.label]),
      [st ? st.achievementScore : '-', 'Achievement score'], [st ? duration(st.onlineSeconds) : '-', 'Time online'],
      [d.friends, 'Friends'], [d.roomCount, 'Rooms owned'],
    ], 'Wallet and stats');

    const account = panel({ title: 'Account', body: HK.factsGrid([
      ['Rank', rankText], ['Created', when(u.created)], ['Last login', when(u.lastLogin)],
      ['Last online', u.online ? 'Online now' : when(u.lastOnline)],
      ['Home room', u.homeRoom ? roomLink(u.homeRoom) : 'None'],
      ['Web account', d.account ? `#${d.account.id}, since ${fmt.date(d.account.created)}` : 'None (a game account)'],
      d.account ? ['Its other avatars', otherAvatars(d.account.avatars.filter((a) => a.id !== u.id))] : null]) });
    const moderation = panel({ title: 'Moderation', more: ['All bans and sanctions', urls.userSanctions(u.id)], body: HK.factsGrid([
      ['Calls for help sent', s('reportsSent')], ['Abusive calls for help', s('reportsAbusive')], ['Cautions', s('cautions')],
      ['Bans (counter)', s('bansCounted')], ['Bans (rows)', fmt.number(d.bans)], ['Trade locks', s('tradeLocks')],
      ['Safety locked', st ? (st.safetyLocked ? 'Yes' : 'No') : '-'],
      ['Muted until', st?.mutedUntil ? fmt.dateTime(st.mutedUntil) : 'Not muted'],
      ['Trade locked until', st?.tradeLockedUntil ? fmt.dateTime(st.tradeLockedUntil) : 'Not locked']]) });

    const wallet = d.can.wallet ? panel({ id: 'wallet-edit', title: 'Wallet', body: [
      HK.jsonForm(h('form', { class: 'form-grid' },
        d.wallet.map(({ key, label, amount }) => HK.field({ id: `wallet-${key}`, name: key, label, type: 'number', min: 0, max: 2147483647, step: 1, required: true, inputmode: 'numeric', value: String(amount) })),
        h('div', null, h('button', { class: 'btn', type: 'submit', 'data-busy': 'Saving…' }, 'Save'))), `/users/${u.id}/wallet`, {
        body: (f) => ({ ...Object.fromEntries(new FormData(f)), ...Object.fromEntries(d.wallet.map(({ key, amount }) => [`was_${key}`, String(amount)])) }),
        onDone: reloadWith,
      }),
      h('p', { class: 'hint' }, `The new balances; only the changed ones are sent. ${ONLINE_NOTE}`)] }) : '';

    const levelTable = () => HK.table({
      caption: 'What each level adds', className: 'levels', head: ['Level', 'What it is', 'Keys'], cls: ['', 'levels__text', ''],
      rowAttrs: (i) => (d.levels[i].id === u.rank ? { 'aria-current': 'true' } : null),
      rows: d.levels.map((l) => [
        [h('b', null, `${l.id} ${l.name}`), l.id === u.rank ? [' ', pill('now', 'on')] : ''],
        h('span', { class: 'small' }, l.text),
        l.keys.length === 0 ? h('span', { class: 'muted' }, 'nothing more')
          : h('details', null, h('summary', null, `${l.keys.length} ${l.keys.length === 1 ? 'key' : 'keys'}`),
            h('ul', { class: 'small' }, l.keys.map((k) => h('li', null, h('span', { class: 'mono' }, k.key), ' ', h('span', { class: 'muted' }, k.comment || '-'))))),
      ]),
    });
    const levels = d.can.rank ? panel({ id: 'rank-edit', title: 'Level', body: [
      HK.jsonForm(h('form', { class: 'inline-form' },
        HK.field({ id: 'rank-level', name: 'level', label: 'Level', type: 'select', value: String(u.rank), options: d.levels.map((l) => [String(l.id), `${l.id} ${l.name}`]) }),
        h('button', { class: 'btn', type: 'submit', 'data-busy': 'Saving…' }, 'Save')), `/users/${u.id}/rank`, {
        body: (f) => ({ level: f.elements.level.value, was_level: String(u.rank) }), onDone: reloadWith,
      }),
      h('p', { class: 'hint' }, `Each level has everything of the levels below it (the VIP badge is given at level 2 only, the AMB badge and the Ambassador effect belong to level 3 only). ${ONLINE_NOTE}`)] }) : '';
    const levelList = d.can.rank ? h('details', { class: 'panel', id: 'levels' }, h('summary', null, `What each level adds (${d.levels.length} levels)`), levelTable()) : '';

    const badgeCount = `${fmt.number(d.badges.length)}${d.badgesMore ? '+' : ''}`;
    const tile = (b) => h('li', null,
      h('span', { class: 'badge-grid__pic' }, h('img', { src: b.image, alt: '', width: 40, height: 40, loading: 'lazy', onerror: (e) => e.currentTarget.remove() })),
      h('span', { class: 'badge-grid__code' }, b.code.split('_').flatMap((part, i) => (i ? ['_', h('wbr'), part] : [part]))),
      h('span', { class: 'badge-grid__name', title: b.name ?? '' }, b.name ?? h('span', { class: 'muted' }, 'No name')),
      b.slot > 0 ? pill(`Worn, slot ${b.slot}`, 'on') : '',
      d.can.badges ? HK.jsonForm(h('form', null, h('button', { class: 'link small', type: 'submit', 'aria-label': `Take away ${b.code}` }, 'Take away')),
        `/users/${u.id}/badges/take`, { body: () => ({ badge: b.code }), onDone: reloadWith }) : '');
    const sorted = [...d.badges.filter((b) => b.slot > 0).sort((a, b) => a.slot - b.slot), ...d.badges.filter((b) => !(b.slot > 0))];
    const badges = panel({ id: 'badges', title: `Badges (${badgeCount})`, body: [
      d.can.badges ? [HK.jsonForm(h('form', { class: 'inline-form' },
        HK.field({ id: 'badge-give', name: 'badge', label: 'Give a badge', required: true, maxlength: 32, pattern: '[A-Za-z0-9_]{1,32}', autocomplete: 'off', spellcheck: 'false', placeholder: 'Badge code, like HC1' }),
        h('button', { class: 'btn', type: 'submit', 'data-busy': 'Giving…' }, 'Give')), `/users/${u.id}/badges/give`, { onDone: reloadWith }),
      h('p', { class: 'hint' }, `Any badge the game has: one its texts name (HC1), a level an achievement has (ACH_RoomEntry5, not past its last level), a rank's badge (AMB, ADM) or VIP. The game refuses a code it does not have. ${ONLINE_NOTE}`)] : '',
      d.badges.length ? h('ul', { class: 'badge-grid', 'aria-label': `Badges of ${u.name}` }, sorted.map(tile)) : HK.empty('No badges.'),
      d.badgesMore ? h('p', { class: 'foot-note' }, `The first ${fmt.number(d.badges.length)} are shown.`) : ''] });

    const rooms = panel({ title: `Rooms (${fmt.number(d.roomCount)})`, body: [HK.table({
      caption: 'Rooms owned', head: ['Room', 'Users now', 'Door'], emptyText: 'This user owns no rooms.', cls: ['', 'nowrap', 'nowrap'],
      rows: d.rooms.map((r) => [roomLink(r), `${fmt.number(r.users)} / ${fmt.number(r.maxUsers)}`, r.door]),
    }), d.roomCount > d.rooms.length ? h('p', { class: 'foot-note' }, `The first ${d.rooms.length} are shown.`) : ''] });

    HK.clear(main).append(head,
      d.ban ? h('p', { class: 'alert', role: 'alert' }, `Banned until ${banEnds(d.ban)} (${d.ban.covers}) by ${d.ban.staff?.name || 'system'}: ${d.ban.reason || 'no reason given'}.`) : '',
      message ? HK.notice(message) : '',
      commands,
      tallies,
      ...(wallet || levels ? [HK.cols('', [account, moderation, badges], [wallet, levels, rooms])] : [account, moderation, badges, rooms]),
      levelList,
      h('p', { class: 'foot-note' }, 'Alerts, kicks, mutes and trade locks are given in the in-client mod tool: they need the game connection.'));
  };

  const render = (query, message) => HK.load(main, `/users/${id}${query}`, (d) => draw(d, message));
  const sent = new URLSearchParams(location.search).get('commands');
  return render(sent && /^[\d,]{1,200}$/.test(sent) ? `?commands=${sent}` : '');
});
