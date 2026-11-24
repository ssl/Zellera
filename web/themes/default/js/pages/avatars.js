'use strict';

Site.start(async (main, info) => {
  const { h, panel, field, jsonForm, notice, fmt, pill, avatar, post, urls } = Site;
  if (!info.viewer && !info.choosingAvatar) { location.assign('/'); return; }
  if (!Site.manyAvatars(info)) { location.assign(urls.settings()); return; }
  Site.setTitle('Your avatars');

  return Site.load(main, '/avatars', (d) => {
    const place = Site.place();
    const alert = Site.alertBox();

    const play = async (btn, a) => {
      alert.textContent = '';
      const restore = Site.busy(btn);
      let r;
      try { r = await post('/avatars/select', { id: a.id }); } catch { r = { ok: false, data: { error: Site.NETWORK_ERROR } }; }
      if (r.status === 401) { location.assign('/'); return; }
      if (r.ok) { location.assign(r.data.next || urls.me()); return; }
      restore();
      alert.textContent = r.data.error || 'That did not work. Try again.';
    };

    const banLine = (a) => (a.ban.until ? `Banned until ${fmt.dateTime(a.ban.until)}` : 'Banned permanently')
      + (a.ban.reason ? `. Reason: ${a.ban.reason}` : '.');
    const card = (a) => h('li', { class: `avatar-card${a.current ? ' avatar-card--current' : ''}${a.banned ? ' avatar-card--banned' : ''}` },
      h('div', { class: 'stage avatar-card__stage' }, avatar(a, { full: true })),
      h('div', { class: 'avatar-card__text' },
        h('h2', { class: 'avatar-card__name' }, a.name, a.current ? [' ', pill('Playing now', 'on')] : ''),
        a.motto ? h('p', { class: 'avatar-card__motto' }, a.motto) : '',
        h('p', { class: 'hint' }, a.lastOnline ? `Last in the ${place} ${fmt.ago(a.lastOnline)}` : `Not in the ${place} yet`,
          h('br'), `Since ${fmt.date(a.memberSince)}`),
        a.banned ? h('p', { class: 'avatar-card__ban' }, banLine(a)) : ''),
      a.banned ? h('button', { class: 'btn', type: 'button', disabled: true }, 'Banned')
        : a.current ? h('a', { class: 'btn', href: urls.me() }, 'Go to my page')
          : h('button', { class: 'btn btn--go', type: 'button', 'data-busy': 'Switching…', onclick: (e) => play(e.currentTarget, a) }, `Play as ${a.name}`));

    const count = `${d.avatars.length} of ${d.max}`;
    const list = panel({ id: 'avatars', title: `Your avatars (${count})`, level: 1, body: [
      alert,
      d.avatars.length
        ? h('ul', { class: 'avatar-cards' }, d.avatars.map(card))
        : Site.empty('This account has no avatar yet. Make your first one.'),
      h('p', { class: 'hint' }, `All avatars share your e-mail address and password. Each has its own friends, rooms, purse and badges; Enter the ${place} plays as the one you picked.`)] });

    let made = '';
    if (d.canCreate) {
      const form = jsonForm(h('form', { novalidate: true },
        field({ id: 'avatar-name', name: 'name', label: 'Name', hint: 'Up to 15 letters, numbers and - = ? ! @ : . , (you cannot change it later)', autocomplete: 'off', autocapitalize: 'none', spellcheck: 'false', required: true, maxlength: 15 }),
        h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Making it…' }, 'Make this avatar')), '/avatars', {
        check: (f) => (!f.elements.name.value.trim() ? ['name', 'Name your avatar first.'] : null),
        onDone: (data) => location.assign(data.next || urls.me()),
      });
      const input = form.elements.name;
      const out = form.querySelector('#avatar-name-error');
      let timer = 0;
      input.addEventListener('input', () => {
        clearTimeout(timer);
        const name = input.value.trim();
        if (!name) return;
        timer = setTimeout(async () => {
          let r;
          try { r = await post('/avatars/check-name', { name }); } catch { return; }
          if (!r.ok || input.value.trim() !== name) return;
          input.setAttribute('aria-invalid', r.data.available ? 'false' : 'true');
          out.textContent = r.data.available ? '' : r.data.reason;
        }, 400);
      });
      made = panel({ id: 'new-avatar', title: 'Make a new avatar', body: [form,
        h('p', { class: 'hint' }, d.max - d.avatars.length === 1 ? 'One more avatar fits on this account.' : `${d.max - d.avatars.length} more avatars fit on this account.`)] });
    } else {
      made = panel({ id: 'new-avatar', title: 'Make a new avatar', body: notice(`This account holds the most avatars it can: ${d.max}.`, 'warn') });
    }

    main.append(Site.cols('', [list], [made]));
  });
});
