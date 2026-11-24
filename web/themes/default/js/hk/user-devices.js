'use strict';

HK.start((main) => {
  const id = HK.param(2);
  const { h, panel, pill, fmt, urls } = HK;
  const { userHead, when } = HKUser;
  const { COMBO, piecesGrid, count } = HKDevice;

  const value = (d, name) => d.pieces.find((p) => p.name === name).value;
  const sharedCell = (u, n, query) => (n == null ? h('span', { class: 'muted' }, 'Not usable')
    : n === 0 ? 'Nobody else' : h('a', { href: urls.userDevicesShared(u.id, query) }, count(n, 'other user', 'other users')));

  const devicePanel = (r, d, i) => panel({
    id: `device-${i + 1}`, title: `Device ${i + 1}`, tone: d.bans.length ? 'red' : '',
    body: [
      piecesGrid(d),
      HK.factsGrid([
        ['First seen', when(d.firstSeen)], ['Last seen', when(d.lastSeen)], ['Logins', fmt.number(d.logins)],
        ['Same id', sharedCell(r.user, d.sharedId, { id: value(d, 'id') })],
        [`Same ${COMBO}`, sharedCell(r.user, d.sharedCombo, { gpu: value(d, 'gpu'), scr: value(d, 'scr'), fnt: value(d, 'fnt') })],
      ]),
      h('p', { class: 'row small' },
        r.can.ban ? h('a', { class: 'btn btn--danger btn--small', href: urls.deviceBanNew({ user: r.user.id, reading: d.reading }) }, 'Ban this device…') : '',
        h('span', null, h('span', { class: 'muted' }, 'Device bans that match: '), d.bans.length ? h('a', { href: urls.deviceBans() }, d.bans.map((b) => `#${b}`).join(', ')) : 'None')),
    ],
  });

  const exemption = (r, render) => {
    const state = r.exempt
      ? HK.notice(`Exempt since ${fmt.dateTime(r.exempt.at)}${r.exempt.staff ? ` (${r.exempt.staff.name ?? `user #${r.exempt.staff.id}`})` : ''}: no device ban refuses this user.`, 'warn')
      : HK.notice('Not exempt: a device ban that matches one of their devices refuses their login.');
    const form = r.can.exempt ? HK.jsonForm(h('form', null,
      h('button', { class: 'btn', type: 'submit', 'data-busy': 'Saving…' }, r.exempt ? 'End the exemption' : 'Exempt from device bans')),
    `/users/${r.user.id}/device-exemption`, { body: () => ({ exempt: !r.exempt }), onDone: (d) => render(d.message) }) : '';
    return panel({ id: 'exemption', title: 'Exemption', body: [state, form,
      h('p', { class: 'foot-note' }, 'For a player who shares a device with a banned one (a family, a school). Account, machine and IP bans still count.')] });
  };

  const draw = (r, message, render) => {
    HK.setTitle(`${r.user.name} · Devices`);
    const compare = h('form', { class: 'search-form', method: 'get', action: urls.devicesCompare(), role: 'search' },
      h('input', { type: 'hidden', name: 'a', value: String(r.user.id) }),
      h('div', { class: 'field' }, h('label', { for: 'f-b' }, 'Compare with (name or id)'),
        h('input', { id: 'f-b', name: 'b', type: 'search', autocomplete: 'off', spellcheck: 'false', required: true })),
      h('button', { class: 'btn', type: 'submit' }, 'Compare'));
    HK.clear(main).append(
      userHead(r.user, 'devices', {
        meta: r.exempt ? pill('Exempt from device bans', 'warn') : '',
        lead: 'A device is what the play page measured at a login, kept as ten hashes; what they were measured from is not kept. A struck-through piece is not usable and never counts.',
      }),
      message ? HK.notice(message, 'ok') : '',
      ...(r.devices.length ? r.devices.map((d, i) => devicePanel(r, d, i)) : [HK.empty('No device recorded yet: this user has not logged in since devices are measured.')]),
      r.more ? h('p', { class: 'foot-note' }, `The ${r.devices.length} last seen are shown.`) : '',
      panel({ id: 'compare', title: 'Compare with another user', body: compare }),
      exemption(r, render));
  };

  const render = (message) => HK.load(main, `/users/${id}/devices`, (r) => draw(r, message, render));
  return render();
});
