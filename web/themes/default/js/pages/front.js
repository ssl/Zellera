'use strict';

Site.start(async (main, info) => {
  const { h, field, panel, jsonForm } = Site;
  if (info.choosingAvatar) { location.replace(Site.urls.avatars()); return; }
  Site.setTitle('Log in');
  const emailMode = info.accounts?.email ?? 'off';
  const many = Site.manyAvatars(info);
  const loginLabel = many ? 'E-mail address' : emailMode === 'off' ? 'Username' : 'Username or e-mail';
  const next = (data) => location.assign(data?.next || '/me');

  const login = jsonForm(h('form', { novalidate: true }, Site.alertBox(info.banNotice ?? ''),
    field({
      id: 'login-name', name: 'login', label: loginLabel, type: many ? 'email' : 'text', autocomplete: 'username', autocapitalize: 'none',
      spellcheck: 'false', required: true, maxlength: emailMode === 'off' ? 15 : 254,
    }),
    field({ id: 'login-password', name: 'password', label: 'Password', type: 'password', autocomplete: 'current-password', required: true, maxlength: 128 }),
    h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Logging in…' }, 'Log in')), '/login', {
    check: (f) => (!f.elements.login.value.trim() ? ['login', `Type your ${loginLabel.toLowerCase()}.`] : !f.elements.password.value ? ['password', 'Type your password.'] : null),
    onDone: next,
  });

  const emailField = emailMode === 'off' ? '' : field({
    id: 'reg-email', name: 'email', type: 'email', label: emailMode === 'required' ? 'E-mail address' : 'E-mail address (optional)',
    hint: `${many ? 'You log in with it. ' : 'You can log in with it too. '}${info.name} never sends you mail.`,
    autocomplete: 'email', autocapitalize: 'none', spellcheck: 'false', required: emailMode === 'required', maxlength: 254,
  });
  const registerForm = h('form', { novalidate: true },
    emailField,
    field({ id: 'reg-username', name: 'username', label: many ? 'Name your first avatar' : 'Pick a username', hint: 'Up to 15 letters, numbers and - = ? ! @ : . ,', autocomplete: 'username', autocapitalize: 'none', spellcheck: 'false', required: true, maxlength: 15 }),
    field({ id: 'reg-password', name: 'password', label: 'Password', type: 'password', hint: '8 characters or more.', autocomplete: 'new-password', required: true, maxlength: 128 }),
    field({ id: 'reg-password2', name: 'password2', label: 'Password again', type: 'password', autocomplete: 'new-password', required: true, maxlength: 128 }),
    info.register?.betaCode ? field({ id: 'reg-beta', name: 'beta', label: 'Beta code', hint: `${info.name} is in closed beta. Staff hand out the codes.`, autocomplete: 'off', spellcheck: 'false', required: true, maxlength: 128 }) : '',
    h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Making your account…' }, 'Make my account'));
  jsonForm(registerForm, '/register', {
    check: (f) => {
      if (emailMode === 'required' && !f.elements.email.value.trim()) return ['email', 'Type your e-mail address.'];
      if (!f.elements.username.value.trim()) return ['username', many ? 'Name your avatar first.' : 'Pick a username first.'];
      if (!f.elements.password.value) return ['password', 'Choose a password.'];
      if (f.elements.password.value !== f.elements.password2.value) return ['password2', 'The two passwords are not the same.'];
      return null;
    },
    onDone: next,
  });

  const loginPanel = panel({ id: 'login', title: 'Log in', body: [login,
    h('p', { class: 'door__switch' }, 'First time here? ', h('button', { class: 'link', type: 'button', dataset: { show: 'register' } }, 'Make an account'))] });
  const registerPanel = panel({ id: 'register', title: 'Make an account', body: [
    info.register?.open === false ? Site.notice('New accounts are closed during the beta.', 'warn') : '', registerForm,
    h('p', { class: 'door__switch' }, 'Have an account? ', h('button', { class: 'link', type: 'button', dataset: { show: 'login' } }, 'Log in'))] });
  registerPanel.hidden = true;
  const show = (id, focus) => {
    loginPanel.hidden = id !== 'login';
    registerPanel.hidden = id !== 'register';
    for (const a of document.querySelectorAll('.tabs__sub a')) {
      const on = new URL(a.href).hash === (id === 'register' ? '#register' : '');
      if (on) a.setAttribute('aria-current', 'page'); else a.removeAttribute('aria-current');
    }
    if (focus) {
      const open = id === 'login' ? loginPanel : registerPanel;
      open.querySelector('input')?.focus();
      open.scrollIntoView({ block: 'nearest' });
    }
  };
  for (const btn of [...loginPanel.querySelectorAll('[data-show]'), ...registerPanel.querySelectorAll('[data-show]')]) {
    btn.addEventListener('click', () => {
      show(btn.dataset.show, true);
      history.replaceState(null, '', btn.dataset.show === 'register' ? '#register' : location.pathname);
    });
  }
  const fromHash = () => show(location.hash === '#register' ? 'register' : 'login', location.hash === '#register');
  window.addEventListener('hashchange', fromHash);

  const line = (what, price) => h('div', null, h('dt', null, what), h('dd', null, price));
  const banner = h('div', { class: 'banner' },
    h('h1', { id: 'view-title' }, `A ${Site.place()} that costs nothing.`),
    h('p', { class: 'banner__line' }, `Everything here is free, ${Site.names().credits} too.`),
    h('div', { class: 'bill' }, h('div', { class: 'bill__sheet' }, h('h2', { class: 'bill__title', id: 'bill-title' }, `Your bill at ${info.name}`),
      h('dl', { 'aria-labelledby': 'bill-title' },
        line(`${Site.names().Credits}, ${Site.names().points} and ${Site.names().premium}`, 'Free'),
        line(Site.names().buildPass, 'Free, for everyone'),
        line('Every Club outfit', 'Free'),
        line('An email address', { off: 'Not needed', optional: 'Up to you', required: 'Only to log in' }[emailMode] ?? 'Not needed'),
        h('div', { class: 'bill__total' }, h('dt', null, 'To pay'), h('dd', null, h('span', { class: 'stamp' }, 'Nothing')))))));

  const firstVisit = panel({ id: 'first-visit', title: 'Your first visit', body: h('ol', { class: 'steps' },
    h('li', null, h('div', null, h('b', null, 'Make an account'), h('p', null, {
      off: 'A username and a password, nothing else.',
      optional: 'A username and a password; an e-mail address if you like.',
      required: many ? 'An e-mail address, a password and a name for your first avatar.' : 'An e-mail address, a username and a password.',
    }[emailMode] ?? 'A username and a password, nothing else.'))),
    h('li', null, h('div', null, h('b', null, `Enter the ${Site.place()}`), h('p', null, 'The big button at the top opens it in a new tab.'))),
    h('li', null, h('div', null, h('b', null, 'Build a room'), h('p', null, `Every ${Site.names().buildPass} furni is yours to place.`)))) });
  const rules = panel({ id: 'rules', title: 'House rules', body: [
    h('ul', { class: 'list rules' }, h('li', null, 'Be kind.'), h('li', null, 'Stay private.'), h('li', null, 'Never share your password.')),
    h('p', { class: 'small' }, h('a', { href: Site.urls.help('rules') }, 'All seven rules'), ' and ', h('a', { href: Site.urls.safety() }, 'staying safe'), '.')] });

  Site.clear(main).append(
    h('div', { class: 'cols door' }, h('div', { class: 'col' }, banner), h('div', { class: 'col' }, loginPanel, registerPanel, rules)),
    firstVisit);
  fromHash();
});
