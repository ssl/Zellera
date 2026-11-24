'use strict';

HK.start((main) => {
  const { h, panel, pill, fmt } = HK;
  const { userLink } = HKUser;
  const page = new URLSearchParams(location.search).get('page');

  const draw = (r, message, render) => {
    HK.setTitle('Device bans');
    const remove = (b) => HK.jsonForm(h('form', null,
      h('button', { class: 'link small', type: 'submit', 'aria-label': `Delete device ban #${b.id}` }, 'Delete')),
    `/device-bans/${b.id}/delete`, { body: () => ({}), confirmText: `Delete device ban #${b.id}? Its devices can log in again.`, onDone: (d) => render(d.message) });
    HK.clear(main).append(
      HK.head({
        id: 'device-bans-title', title: 'Device bans',
        lead: 'A device ban is a pattern of pieces of a device: a login from a device with exactly these pieces is refused, whatever the account. Pieces the pattern leaves out match any value. Make one from a device on a user’s Devices page.',
      }),
      message ? HK.notice(message, 'ok') : '',
      panel({ id: 'patterns', title: 'Patterns', tone: 'red', body: [HK.table({
        caption: 'Device bans', head: ['Id', 'Pattern', 'Reason', 'By', 'Made', 'Until', 'State', 'Matches', ''],
        emptyText: 'No device bans.', cls: ['num', 'pattern-cell', 'excerpt grow', 'nowrap', 'nowrap', 'nowrap', 'nowrap', 'num', 'nowrap'],
        rows: r.patterns.map((b) => [String(b.id), HKDevice.pattern(b.pieces), b.reason || '-', userLink(b.staff, 'system'), fmt.dateTime(b.createdAt),
          b.expiresAt == null ? 'No end' : fmt.dateTime(b.expiresAt), b.active ? pill('Active', 'bad') : pill('Ended'),
          b.matches == null ? '-' : HKDevice.count(b.matches), b.canDelete ? remove(b) : '']),
      }), HK.pager({ page: r.page, hasNext: r.hasNext }),
      h('p', { class: 'foot-note' }, 'Matches: the users the pattern matches now. Besides these, the game refuses a login from the device of a banned account by itself; that ends when the account ban does. A user marked exempt on their Devices page is never refused for a device.')] }));
  };

  const render = (message) => HK.load(main, `/device-bans${HK.qs({ page })}`, (r) => draw(r, message, render));
  return render();
});
