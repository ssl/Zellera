'use strict';

HK.start((main) => {
  const { h, panel, pill, fmt } = HK;
  const { userLink } = HKUser;
  const q = new URLSearchParams(location.search);
  const filter = { player: q.get('player') ?? '', kind: q.get('kind') ?? '', address: q.get('address') ?? '' };
  const KINDS = {
    'net.unknown_id': 'Sent packet ids the shuffled session does not know (a protocol bot)',
    'login.ticket_address': 'Used a login ticket from another address than the one that made it',
    'login.release': 'Logged in with another client release than this hotel serves',
    'room.raid': 'Was a suspect in a room raid',
    'net.flood': 'Sent more packets than the limit allows',
    'site.pow': 'Sent a missing or wrong proof-of-work',
    'device.headless': 'The play page ran in an automated or headless browser',
    'client.integrity': 'Gave a wrong or no answer when asked to prove it runs the client this hotel serves',
  };
  HK.setTitle('Suspicious activity');

  const form = h('form', { class: 'search-form', method: 'get', action: location.pathname, role: 'search' },
    [['player', 'Player (name or id)', ''], ['kind', 'Kind starts with', 'net.'], ['address', 'Address', '']]
      .map(([name, label, placeholder]) => h('div', { class: 'field' }, h('label', { for: `f-${name}` }, label),
        h('input', { id: `f-${name}`, name, type: 'search', value: filter[name], placeholder: placeholder || null, autocomplete: 'off', spellcheck: 'false' }))),
    h('button', { class: 'btn btn--go', type: 'submit' }, 'Filter'));
  const top = () => [HK.head({
    id: 'suspicions-title', title: 'Suspicious activity',
    lead: 'What the game and the site noticed about players: each row has a weight. An online player whose weights add up to the score within the window is shadow-muted by the game. Times are UTC.',
  }), form];

  const clearButton = (s, render) => HK.jsonForm(h('form', null,
    h('button', { class: 'link small', type: 'submit', 'aria-label': `Clear the suspicions of ${s.player.name || `#${s.player.id}`}` }, 'Clear')),
  `/suspicions/${s.player.id}/clear`, {
    body: () => ({}), confirmText: `Delete every suspicion row of ${s.player.name || `#${s.player.id}`}?`, onDone: (d) => render(d.message),
  });

  const draw = (r, message, render) => {
    HK.clear(main).append(...top(), message ? HK.notice(message, 'ok') : '',
      panel({ id: 'scores', title: 'Scores', body: [HK.table({
        caption: `Score over the last ${r.windowHours} hours`, head: ['Player', 'Score', 'State', ''], emptyText: 'No players on this page.',
        cls: ['grow', 'num', 'nowrap', 'nowrap'],
        rows: r.summary.map((s) => [userLink(s.player), String(s.score), s.shadowMuted ? pill('shadow-muted', 'bad') : '', clearButton(s, render)]),
      }), h('p', { class: 'foot-note' }, r.shadowMuteScore > 0
        ? `Shadow-muted from a score of ${r.shadowMuteScore}. The scores are for the players on this page.`
        : 'Shadow muting is off (the score setting is 0).')] }),
      panel({ id: 'rows', title: 'Rows', body: [HK.table({
        caption: 'Suspicions', head: ['Time', 'Player', 'Kind', 'Weight', 'Address', 'Device', 'Detail'], emptyText: 'No rows match.',
        cls: ['nowrap', 'nowrap', 'nowrap', 'num', 'nowrap', 'mono', 'grow'],
        rows: r.rows.map((s) => [fmt.dateTime(s.at), userLink(s.player), h('span', { class: 'mono', title: KINDS[s.kind] ?? null }, s.kind),
          String(s.weight), s.address || '-', s.device || '-', s.detail || '-']),
      }), HK.pager({ page: r.page, hasNext: r.hasNext })] }),
      panel({ id: 'kinds', title: 'Kinds', body: HK.facts(Object.entries(KINDS).map(([k, text]) => [h('span', { class: 'mono' }, k), text])) }));
  };

  const api = `/suspicions${HK.qs({ ...filter, page: q.get('page') })}`;
  const render = (message) => HK.api(api).then((r) => {
    if (!r.ok) {
      HK.clear(main).append(...top());
      if (r.status === 400) { main.append(h('p', { class: 'alert', role: 'alert' }, r.data.error)); return; }
      return HK.showError(main, r);
    }
    draw(r.data, message, render);
  }, () => HK.showError(main, { status: 0, data: {} }));
  return render();
});
