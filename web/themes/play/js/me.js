'use strict';

(() => {
  const API = '/api/site/v1';
  const $ = (id) => document.getElementById(id);
  const KEEP = 'zel.guest';

  const kept = () => {
    try { return JSON.parse(localStorage.getItem(KEEP) ?? 'null')?.password ?? ''; } catch { return ''; }
  };
  const forget = () => {
    try { localStorage.removeItem(KEEP); } catch { /* nothing kept */ }
  };

  const post = async (path, body) => {
    const res = await fetch(API + path, { method: 'POST', headers: { 'content-type': 'application/json' }, body: JSON.stringify(body) });
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  };

  const say = (id, text, good = false) => {
    const el = $(id);
    el.textContent = text;
    if (good) el.dataset.ok = ''; else delete el.dataset.ok;
  };

  const busy = (form, on) => { for (const b of form.querySelectorAll('button')) b.disabled = on; };

  // The password a guest account got when it was made: while this browser still has it, the page uses it as the
  // current password, so a guest only types the new one.
  let guestPassword = kept();
  if (!guestPassword) $('current-row').hidden = false;
  else $('switch-hint').textContent = 'Log in as someone else. This guest account has no password yet: set one first, or it is gone.';

  (async () => {
    try {
      const res = await fetch(`${API}/site`);
      const site = await res.json();
      if (!site.viewer) { location.replace('/'); return; }
      $('who').textContent = site.viewer.name;
    } catch {
      $('who').textContent = 'unknown';
    }
  })();

  $('password-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    const current = guestPassword || f.elements.current.value;
    if (!current) return say('password-msg', 'Type your current password.');
    if (!f.elements.password.value) return say('password-msg', 'Type a new password.');
    if (f.elements.password.value !== f.elements.password2.value) return say('password-msg', 'The two passwords are not the same.');
    busy(f, true);
    say('password-msg', '');
    try {
      const r = await post('/me/password', { current, password: f.elements.password.value, password2: f.elements.password2.value });
      if (!r.ok) return say('password-msg', r.data.error || 'Could not save the password. Try again.');
      forget();
      guestPassword = '';
      $('current-row').hidden = false;
      f.reset();
      say('password-msg', 'Password saved.', true);
    } catch {
      say('password-msg', 'The game is not reachable right now.');
    } finally {
      busy(f, false);
    }
  });

  $('switch-form').addEventListener('submit', async (e) => {
    e.preventDefault();
    const f = e.target;
    if (!f.elements.login.value.trim() || !f.elements.password.value) return say('switch-msg', 'Type the username and the password.');
    busy(f, true);
    say('switch-msg', '');
    try {
      const r = await post('/login', { login: f.elements.login.value.trim(), password: f.elements.password.value });
      if (!r.ok) return say('switch-msg', r.data.error || 'Could not log in. Try again.');
      forget();
      location.assign('/');
    } catch {
      say('switch-msg', 'The game is not reachable right now.');
    } finally {
      busy(f, false);
    }
  });
})();
