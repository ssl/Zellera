'use strict';

Site.start(async (main, info) => {
  const { h, panel, field, jsonForm, notice, urls, api, post } = Site;
  if (!info.viewer) { location.assign('/'); return; }
  Site.setTitle('Settings');

  const [privacyAnswer, accountAnswer] = await Promise.all([api('/me/privacy'), api('/me/account')]);
  if (!privacyAnswer.ok) { Site.showError(main, privacyAnswer); return; }
  const rules = accountAnswer.ok ? accountAnswer.data.rules : { email: 'off', avatars: 1 };

  const place = Site.place();
  const CHOICES = [
    ['profileVisible', 'Public profile', 'Other players can open your profile and find you in Find a player. Off: only you and staff see it.'],
    ['onlineStatusVisible', 'Show when I am online', `Friends and visitors of your profile see whether you are in the ${place}.`],
    ['friendRequestEnabled', 'Friend requests', 'Other players can ask to be your friend.'],
    ['friendCanFollow', 'Friends can follow me', 'Friends can jump to the room you are in from their friend list.'],
    ['roomInvitesEnabled', 'Room invites', `Friends can invite you to their room. The ${place}'s own settings change this one too.`],
  ];
  const state = { ...privacyAnswer.data.privacy };
  const STATE_TEXT = { saving: 'Saving…', saved: 'Saved', pending: `Sent; the ${place} applies it as soon as it runs` };

  const choice = ([key, title, text]) => {
    const id = `privacy-${key}`;
    const status = h('span', { class: 'choice__state', id: `${id}-state`, role: 'status', 'aria-live': 'polite' });
    const input = h('input', { type: 'checkbox', role: 'switch', id, name: key, checked: state[key], 'aria-describedby': `${id}-text ${id}-state` });
    input.addEventListener('change', async () => {
      const wanted = input.checked;
      input.disabled = true;
      status.dataset.kind = 'busy';
      status.textContent = STATE_TEXT.saving;
      let r;
      try { r = await post('/me/privacy', { [key]: wanted }); } catch { r = { ok: false, status: 0, data: { error: Site.NETWORK_ERROR } }; }
      input.disabled = false;
      if (r.status === 401) { location.assign('/'); return; }
      if (r.ok) {
        Object.assign(state, r.data.privacy);
        input.checked = Boolean(state[key]);
        status.dataset.kind = 'ok';
        status.textContent = STATE_TEXT[r.data.state] ?? STATE_TEXT.saved;
      } else {
        input.checked = !wanted;
        status.dataset.kind = 'error';
        status.textContent = r.data.error || 'Not saved. Try again.';
      }
    });
    return h('li', { class: 'choice' },
      h('div', { class: 'choice__text' }, h('div', { class: 'choice__head' }, h('label', { for: id }, title), status), h('p', { class: 'hint', id: `${id}-text` }, text)),
      h('span', { class: 'switch' }, input));
  };
  const privacy = panel({ id: 'privacy', title: 'Privacy', level: 1, body: [
    h('ul', { class: 'choices' }, CHOICES.map(choice)),
    h('p', { class: 'hint' }, `Each switch saves at once, also while you are in the ${place}.`)] });

  let email = '';
  if (rules.email !== 'off') {
    const current = accountAnswer.data.email;
    const shown = h('p', { class: 'email-now' }, current ? ['Now: ', h('b', null, current)] : 'No e-mail address yet.');
    const done = notice('E-mail address saved.', 'ok');
    done.hidden = true;
    const form = jsonForm(h('form', { class: 'email-form', novalidate: true },
      field({ id: 'email-new', name: 'email', type: 'email', label: 'New e-mail address', autocomplete: 'email', autocapitalize: 'none', spellcheck: 'false', required: rules.email === 'required', maxlength: 254, value: current ?? '' }),
      field({ id: 'email-current', name: 'current', label: 'Your password', type: 'password', autocomplete: 'current-password', required: true, maxlength: 128 }),
      h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Saving…' }, 'Save e-mail address')), '/me/email', {
      check: (f) => (rules.email === 'required' && !f.elements.email.value.trim() ? ['email', 'Type an e-mail address: you log in with it.']
        : !f.elements.current.value ? ['current', 'Type your password to confirm.'] : null),
      onDone: (data) => {
        Site.clear(shown).append(...(data.email ? ['Now: ', h('b', null, data.email)] : ['No e-mail address yet.']));
        form.elements.current.value = '';
        done.hidden = false;
      },
    });
    email = panel({ id: 'email', title: 'E-mail address', body: [shown, done, form,
      h('p', { class: 'hint' }, rules.avatars > 1 ? 'You log in with it. Nothing is ever sent to it.'
        : `You can log in with it instead of your name. ${rules.email === 'optional' ? 'Leave it empty to remove it. ' : ''}Nothing is ever sent to it.`)] });
  }

  const done = notice('Password changed. Every other browser is logged out.', 'ok');
  done.hidden = true;
  const form = jsonForm(h('form', { class: 'password-form', novalidate: true },
    field({ id: 'pw-current', name: 'current', label: 'Current password', type: 'password', autocomplete: 'current-password', required: true, maxlength: 128 }),
    field({ id: 'pw-new', name: 'password', label: 'New password', type: 'password', autocomplete: 'new-password', required: true, maxlength: 128 }),
    field({ id: 'pw-new2', name: 'password2', label: 'Repeat new password', type: 'password', autocomplete: 'new-password', required: true, maxlength: 128 }),
    h('p', { class: 'hint' }, `At least 8 characters. Changing it logs you out everywhere else${rules.avatars > 1 ? ', for every avatar' : ''}.`),
    h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Changing…' }, 'Change password')), '/me/password', {
    check: (f) => (f.elements.password.value !== f.elements.password2.value ? ['password2', 'The two passwords do not match.'] : null),
    onDone: () => { form.reset(); done.hidden = false; done.focus?.(); },
  });
  const password = panel({ id: 'password', title: 'Password', body: [done, form] });

  const avatars = rules.avatars > 1 && accountAnswer.ok ? panel({ id: 'avatars', title: 'Your avatars', body: [
    h('p', null, `Your account holds ${accountAnswer.data.avatars.count} of ${rules.avatars} avatars. They share this e-mail address and password.`),
    h('p', null, h('a', { class: 'btn', href: urls.avatars() }, 'Switch or make an avatar'))] }) : '';
  const account = panel({ id: 'account', title: rules.email === 'off' ? 'No email, no reset link' : 'No reset link', body: [
    h('p', null, rules.email === 'off'
      ? `${info.name} never asks for an email address, so nobody can send you a reset link. Keep your password somewhere safe.`
      : `${info.name} never sends mail, so there is no reset link. Keep your password somewhere safe.`),
    h('p', null, `Your look, motto, sounds and camera live in the ${place} itself.`),
    h('ul', { class: 'list' }, h('li', null, h('a', { href: urls.profile(info.viewer.name) }, 'Your profile')), h('li', null, h('a', { href: urls.safety() }, 'Staying safe')))] });

  Site.clear(main).append(Site.cols('', [privacy, password], [email, avatars, account]));
  if (location.hash) document.getElementById(location.hash.slice(1))?.scrollIntoView();
});
