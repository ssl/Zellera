'use strict';

Site.start((main) => Site.load(main, '/me', (me) => {
  const { h, panel, avatar, fmt, time, pill, empty, urls, place } = Site;
  Site.setTitle(me.user.name);

  const SAVED = {
    saved: 'Motto saved.',
    filtered: 'Motto saved. The word filter changed part of it.',
    pending: 'Motto saved. It shows in a moment.',
  };
  let motto = me.user.motto;
  const said = h('p', { class: 'bubble me-bubble', id: 'motto-said' });
  const showMotto = () => {
    said.textContent = motto || 'No motto yet.';
    said.classList.toggle('bubble--quiet', !motto);
  };
  showMotto();
  const note = h('p', { class: 'hint', id: 'motto-note', role: 'status' });
  const input = h('input', {
    id: 'motto-input', name: 'motto', type: 'text', value: motto, maxlength: Math.max(0, me.motto.max),
    placeholder: 'Write a motto', autocomplete: 'off', 'aria-describedby': 'motto-note',
  });
  const save = h('button', { class: 'btn', type: 'submit', 'data-busy': 'Saving…' }, 'Save');
  const cancel = h('button', { class: 'btn', type: 'button' }, 'Cancel');
  const form = h('form', { class: 'motto-form', id: 'motto', hidden: true },
    h('label', { class: 'visually-hidden', for: 'motto-input' }, 'Your motto'),
    h('div', { class: 'inline-form' }, input, save, cancel));
  const change = h('button', { class: 'link me-change', type: 'button', id: 'motto-change', 'aria-controls': 'motto', 'aria-expanded': 'false' }, 'Change motto');
  const editing = (on) => {
    form.hidden = !on;
    said.hidden = on;
    change.hidden = on;
    change.setAttribute('aria-expanded', String(on));
    if (on) {
      input.value = motto;
      note.className = 'hint';
      note.textContent = `Up to ${me.motto.max} characters.`;
      input.focus();
    }
  };
  change.addEventListener('click', () => editing(true));
  cancel.addEventListener('click', () => { editing(false); note.textContent = ''; change.focus(); });
  form.addEventListener('keydown', (e) => { if (e.key === 'Escape') cancel.click(); });
  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (save.disabled) return;
    const restore = Site.busy(save);
    input.removeAttribute('aria-invalid');
    try {
      const r = await Site.post('/me/motto', { motto: input.value });
      restore();
      if (r.ok) {
        motto = r.data.motto;
        showMotto();
        editing(false);
        note.className = 'hint notice--ok';
        note.textContent = SAVED[r.data.state] ?? SAVED.saved;
        change.focus();
        return;
      }
      if (r.status === 401) { location.assign('/'); return; }
      if (r.data.reason !== 'notSaved' && r.data.reason !== 'game') input.setAttribute('aria-invalid', 'true');
      note.className = 'field-error';
      note.textContent = r.data.error || 'Your motto was not saved. Try again.';
    } catch {
      restore();
      note.className = 'field-error';
      note.textContent = Site.NETWORK_ERROR;
    }
  });

  const lastIn = me.user.online ? `In the ${place()} right now.`
    : me.user.lastOnline > 0 ? [`Last in the ${place()} `, time(me.user.lastOnline, fmt.ago(me.user.lastOnline)), '.'] : 'Your first visit is waiting.';
  const now = Date.now() / 1000;
  const days = (m) => Math.max(1, Math.ceil((m.until - now) / 86400));
  const stat = (label, value, id) => h('div', { class: 'me-stat', id }, h('dt', null, label), h('dd', null, value));
  const member = (label, m, id) => stat(label, m
    ? h('time', { datetime: new Date(m.until * 1000).toISOString(), title: `Until ${fmt.date(m.until)}` }, `${fmt.number(days(m))} ${days(m) === 1 ? 'day' : 'days'}`)
    : h('span', { class: 'me-stat__none' }, 'Not a member'), id);
  const strip = h('dl', { class: 'me-stats', 'aria-label': 'Your purse and memberships' },
    stat(Site.names().Credits, fmt.number(me.purse.credits), 'wallet-credits'),
    stat(Site.names().Points, fmt.number(me.purse.points), 'wallet-points'),
    stat(Site.names().Premium, fmt.number(me.purse.premium), 'wallet-premium'),
    (me.purse.seasonal ?? []).map((c) => stat(c.name, fmt.number(c.amount))),
    member('Club', me.memberships?.club, 'member-club'),
    member(Site.names().buildPass, me.memberships?.buildPass, 'member-build-pass'));
  const plain = !Site.avatarsOn();
  const you = h('section', { class: `me-card${plain ? ' me-card--plain' : ''}`, id: 'you', 'aria-labelledby': 'me-name' },
    h('div', { class: 'me-card__scene' },
      h('div', { class: 'stage stage--l me-card__stage' }, avatar(me.user, { full: true, size: 'l', action: 'wav', gesture: 'sml' })),
      h('div', { class: 'me-card__text' },
        h('h1', { id: 'me-name' }, me.user.name),
        h('p', { class: 'me-status' }, lastIn, me.user.memberSince ? [' Here since ', time(me.user.memberSince), '.'] : ''),
        said, form, h('div', { class: 'me-motto-foot' }, change, note))),
    strip);

  const news = panel({ id: 'news', title: `${place({ capital: true })} news`, tone: 'gold', more: me.news.length ? ['All news', urls.news()] : null, body: me.news.length === 0
    ? empty('No news yet. Staff post updates and events here.')
    : h('ul', { class: 'list me-news' }, me.news.map((n, i) => h('li', null,
      h('a', { href: urls.news(n.id) }, i === 0 ? h('b', null, n.title) : n.title),
      n.created ? h('span', { class: 'list__sub' }, time(n.created)) : '',
      i === 0 && n.excerpt ? h('p', { class: 'small me-news__lede' }, n.excerpt) : ''))) });

  const DOOR = { doorbell: 'Doorbell', password: 'Password' };
  const count = (title, n) => (n > 0 ? `${title} (${fmt.number(n)})` : title);
  const rooms = panel({ id: 'rooms', title: count('Your rooms', me.rooms.total), more: me.rooms.total > me.rooms.rows.length ? ['All rooms', `${urls.profile(me.user.name)}#rooms`] : null,
    body: me.rooms.total === 0 ? empty(`No rooms yet. Make your first one in the navigator in the ${place()}.`)
      : h('ul', { class: 'list' }, me.rooms.rows.map((r) => h('li', { class: 'room-row' },
        h('a', { href: urls.room(r.id) }, h('b', null, r.name)),
        h('span', { class: 'room-row__count' }, r.users > 0 ? `${fmt.number(r.users)} inside` : 'Empty'),
        DOOR[r.door] ? pill(DOOR[r.door]) : ''))) });

  const friends = panel({ id: 'friends', title: count('Friends', me.friends.total), body: [
    me.friends.total === 0
      ? [empty(`No friends yet. Add people as friends in the ${place()}.`), h('p', { class: 'small' }, h('a', { href: urls.search() }, 'Find a player'))]
      : h('ul', { class: 'people' }, me.friends.rows.map((f) => h('li', null, h('a', { href: urls.profile(f.name) }, avatar(f),
        h('span', null, f.name), f.online ? pill('Online', 'on') : '')))),
    me.friends.total > me.friends.rows.length ? h('p', { class: 'small muted' }, `${fmt.number(me.friends.total - me.friends.rows.length)} more in your friend list in the ${place()}.`) : ''] });

  const groups = panel({ id: 'groups', title: count('Your groups', me.groups.total), more: me.groups.total > me.groups.rows.length ? ['All groups', `${urls.profile(me.user.name)}#groups`] : null,
    body: me.groups.total === 0 ? empty(`Not in a group yet. Join one, or start your own, in the ${place()}.`)
      : h('ul', { class: 'list' }, me.groups.rows.map((g) => h('li', { class: 'person' }, Site.initials(g.name),
        h('div', { class: 'person__text' }, h('a', { href: urls.group(g.id) }, h('b', null, g.name)), h('span', { class: 'list__sub' }, g.role))))) });

  main.append(h('div', { class: 'me-top' }, you, news), Site.cols('three', rooms, friends, groups));
}));
