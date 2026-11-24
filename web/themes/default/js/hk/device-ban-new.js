'use strict';

HK.start((main) => {
  const { h, panel, urls } = HK;
  const q = new URLSearchParams(location.search);
  const source = { user: q.get('user') ?? '', reading: q.get('reading') ?? '' };

  return HK.load(main, `/device-bans/new${HK.qs(source)}`, (r) => {
    const u = r.user;
    HK.setTitle(`Ban a device of ${u.name}`);
    const usable = r.device.pieces.filter((p) => p.usable).map((p) => p.name);
    const first = usable.includes('id') ? ['id'] : ['gpu', 'scr', 'fnt'].filter((name) => usable.includes(name));
    const ticks = r.device.pieces.map((p) => h('input', { type: 'checkbox', name: `piece-${p.name}`, value: p.name, disabled: !p.usable, checked: first.includes(p.name) }));
    const ticked = () => ticks.filter((t) => t.checked && !t.disabled).map((t) => t.value);
    const status = h('p', { class: 'notice', role: 'status', 'aria-live': 'polite' }, '');
    const save = h('button', { class: 'btn btn--danger', type: 'submit', 'data-busy': 'Saving…', disabled: true }, 'Save the device ban');
    let seen = null;
    let asked = 0;
    const show = (text, n = null) => { status.textContent = text; seen = n; save.disabled = n == null; };
    const recount = async () => {
      const mine = ++asked;
      const pieces = ticked().join(',');
      show('Counting…');
      let c;
      try { c = await HK.api(`/device-bans/count${HK.qs({ ...source, pieces })}`); } catch { c = { ok: false, data: { error: HK.NETWORK_ERROR } }; }
      if (mine !== asked) return;
      if (!c.ok) return show(c.data.error || 'Could not count the matches.');
      const text = `This pattern matches ${HKDevice.count(c.data.matches)} now (${u.name} included).`;
      return c.data.refusal ? show(`${text} ${c.data.refusal}`) : show(text, c.data.matches);
    };
    for (const t of ticks) t.addEventListener('change', recount);

    const failed = HK.alertBox();
    const form = h('form', { class: 'form-split' },
      failed,
      h('div', { class: 'form-stack' }, h('fieldset', { class: 'piece-ticks' }, h('legend', null, 'Pieces that count'),
        r.device.pieces.map((p, i) => h('label', null, ticks[i], h('b', null, p.name), ' ', HKDevice.piece(p)))),
      h('p', { class: 'hint' }, `The id alone is enough; without it tick at least ${r.minWithoutId}. A piece left out matches any value, so fewer pieces match more devices.`),
      status),
      h('div', { class: 'form-stack' }, HK.field({ id: 'device-ban-hours', name: 'hours', label: 'Until', type: 'select', value: 'permanent', options: r.lengths.map((l) => [l.value, l.label]) }),
      HK.field({ id: 'device-ban-reason', name: 'reason', label: 'Reason', required: true, maxlength: r.reasonMax, autocomplete: 'off',
        hint: `1 to ${r.reasonMax} characters, kept with the device ban and in the audit log.` }),
      h('div', null, save)));
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      if (save.disabled || seen == null) return;
      failed.textContent = '';
      const restore = HK.busy(save);
      const body = { ...source, pieces: ticked().join(','), hours: form.elements.hours.value, reason: form.elements.reason.value, seen };
      let sent;
      try { sent = await HK.post('/device-bans', body); } catch { sent = { ok: false, data: { error: HK.NETWORK_ERROR } }; }
      if (sent.ok) { location.assign(urls.deviceBans()); return; }
      restore();
      failed.textContent = sent.data.error || 'Something went wrong. Try again.';
      await recount();
    });

    main.append(
      HK.head({
        id: 'device-ban-title', title: `Ban a device of ${u.name}`,
        crumbs: [['Users', urls.users()], [u.name, urls.user(u.id)], ['Devices', urls.userDevices(u.id)], ['Ban this device']],
        lead: 'Every login from a device with the ticked pieces is refused, whatever the account, until the device ban ends or is deleted.',
      }),
      panel({ id: 'device-ban', title: 'Device ban', tone: 'red', body: form }));
    return recount();
  });
});
