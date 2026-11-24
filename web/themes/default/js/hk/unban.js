'use strict';

HK.start((main) => {
  const { h, panel, pill, fmt, urls } = HK;
  const { userLink, banEnds } = HKUser;
  const id = (new URLSearchParams(location.search).get('ban') ?? '').trim();

  const draw = (b, { refusal = null, noteMax = 200 } = {}) => {
    const name = b.user?.name ?? `user #${b.user?.id ?? 0}`;
    HK.setTitle(`Unban ${name}`);
    const form = b.active && !refusal ? HK.jsonForm(h('form', { class: 'form-stack' },
      h('input', { type: 'hidden', name: 'ban', value: String(b.id) }),
      HK.field({ id: 'unban-note', name: 'note', label: 'Note for the audit log (optional)', maxlength: noteMax, autocomplete: 'off' }),
      h('div', null, h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Lifting…' }, `Lift ban #${b.id}`))), '/actions/unban', {
      onDone: (d) => location.assign(d.userId ? `${urls.userSanctions(d.userId)}?done=unban` : urls.audit()),
    }) : '';
    HK.clear(main).append(HK.head({
      id: 'unban-title',
      crumbs: [['Users', urls.users()], ...(b.user?.id > 0 ? [[name, urls.user(b.user.id)], ['Sanctions', urls.userSanctions(b.user.id)]] : []), [`Ban #${b.id}`]],
      title: `Ban #${b.id} of ${name}`,
      meta: b.active ? pill('Active', 'bad') : pill('Ended'),
    }), panel({ id: 'unban', title: 'The ban', body: [
      HK.factsGrid([['User', userLink(b.user)], ['Covers', b.covers], ['Banned by', userLink(b.staff)],
        ['Since', fmt.dateTime(b.bannedAt)], ['Ends', banEnds(b)],
        ['Reason', b.reason || '-', 'wide']]),
      b.covers !== 'player' ? HK.notice('This ban also keeps out an address or a machine: lifting it lifts it for every account there.', 'warn') : '',
      refusal ? h('p', { class: 'alert', role: 'alert' }, refusal) : '',
      !b.active ? HK.notice('This ban has ended already. Nothing to lift.') : '',
      form] }));
  };

  return HK.api(`/actions/unban?ban=${encodeURIComponent(id)}`).then((r) => {
    if (r.ok) return draw(r.data.ban, { noteMax: r.data.noteMax });
    if (r.status === 403 && r.data.ban) return draw(r.data.ban, { refusal: r.data.error });
    return HK.showError(main, r);
  }, () => HK.showError(main, { status: 0, data: {} }));
});
