'use strict';

HK.start((main) => {
  const { h, panel, pill, fmt, urls } = HK;
  const { userLink } = HKUser;
  const q = new URLSearchParams(location.search);
  const [a, b] = [(q.get('a') ?? '').trim(), (q.get('b') ?? '').trim()];
  const STATE = { equal: ['Same', 'on'], different: ['Different', 'bad'], unusable: ['Not usable', ''] };

  const finder = (error) => [
    HK.head({ id: 'compare-title', crumbs: [['Users', urls.users()], ['Compare devices']], title: 'Compare devices' }),
    panel({ id: 'find', title: 'Who', body: [
      error ? h('p', { class: 'alert', role: 'alert' }, error) : '',
      HK.searchForm({ fields: [{ name: 'a', label: 'User (name or id)', value: a }, { name: 'b', label: 'Other user (name or id)', value: b }], submit: 'Compare' })] })];

  const picker = (r, side) => {
    const list = r.devices[side];
    const other = side === 'a' ? 'b' : 'a';
    const href = (d) => urls.devicesCompare({ a: r.a.id, b: r.b.id, [`d${side}`]: d.reading, [`d${other}`]: r.pair?.[other] });
    return panel({ title: `Devices of ${r[side].name} (${list.length})`, body: list.length === 0 ? HK.empty('No device recorded.') : HK.table({
      caption: `Devices of ${r[side].name}`, head: ['Id', 'Last seen', 'Logins', ''], cls: ['nowrap', 'nowrap', 'num', 'nowrap'],
      rows: list.map((d) => [HKDevice.piece(d.pieces[0]), fmt.dateTime(d.lastSeen), fmt.number(d.logins),
        r.pair?.[side] === d.reading ? pill('compared', 'on') : h('a', { href: href(d) }, 'Compare this one')]),
    }) });
  };

  const draw = (r) => {
    HK.setTitle(`${r.a.name} and ${r.b.name} · Devices`);
    const p = r.pair;
    const verdict = !p ? '' : p.sameId ? HK.notice('The same id: the same browser on the same computer.', 'warn')
      : p.sameCombo ? HK.notice(`A different id, the same ${HKDevice.COMBO}: likely the same computer.`, 'warn')
        : HK.notice(`Neither the id nor ${HKDevice.COMBO} is the same.`);
    main.append(
      HK.head({
        id: 'compare-title', crumbs: [['Users', urls.users()], ['Compare devices']], title: `${r.a.name} and ${r.b.name}`,
        meta: p ? [pill(`${p.equal} same`, 'on'), pill(`${p.different} different`, 'bad'), pill(`${p.unusable} not usable`)] : [],
      }),
      panel({ id: 'compare', title: 'Piece by piece', body: !p ? HK.empty('One of them has no device recorded, so there is nothing to compare.') : [
        verdict,
        h('div', { class: 'table-wrap', tabindex: 0, 'aria-label': 'The two devices piece by piece' },
          h('table', { class: 'compare' }, h('caption', { class: 'visually-hidden' }, 'The two devices piece by piece'),
            h('thead', null, h('tr', null, ['Piece', userLink(r.a), userLink(r.b), 'Result'].map((c) => h('th', { scope: 'col', class: 'nowrap' }, c)))),
            h('tbody', null, p.pieces.map((x) => h('tr', null,
              h('td', { class: 'nowrap' }, x.name),
              h('td', { class: `nowrap mono piece--${x.state}` }, x.a), h('td', { class: `nowrap mono piece--${x.state}` }, x.b),
              h('td', { class: 'nowrap' }, pill(...STATE[x.state]))))))),
        h('p', { class: 'foot-note' }, 'Of all their devices, the two with the most pieces the same are shown first. Pick another below.')] }),
      HK.cols('halves', picker(r, 'a'), picker(r, 'b')));
  };

  if (!a || !b) {
    HK.setTitle('Compare devices');
    HK.clear(main).append(...finder());
    return;
  }
  return HK.api(`/devices/compare${HK.qs({ a, b, da: q.get('da'), db: q.get('db') })}`).then((r) => {
    HK.clear(main);
    if (r.ok) return draw(r.data);
    if (r.status === 404) { HK.setTitle('Compare devices'); main.append(...finder(r.data.error)); return; }
    return HK.showError(main, r);
  }, () => HK.showError(main, { status: 0, data: {} }));
});
