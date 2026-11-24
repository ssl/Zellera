'use strict';

(() => {
  const API = '/api/site/v1';
  const logo = document.getElementById('logo');
  const problem = document.getElementById('problem');

  const call = async (path, method = 'GET') => {
    const init = method === 'GET' ? {} : { method, headers: { 'content-type': 'application/json' }, body: '{}' };
    const res = await fetch(API + path, init);
    const data = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, data };
  };

  // The logo keeps flashing while the game opens; after a problem it stops and a click on it tries again.
  const fail = (text, again = true) => {
    problem.textContent = text;
    problem.hidden = false;
    logo.dataset.still = '';
    logo.dataset.again = again ? '1' : '';
  };

  // A guest's random password stays in this browser until the player sets one of their own on the account page.
  const remember = (password) => {
    try { if (password) localStorage.setItem('zel.guest', JSON.stringify({ password })); } catch { /* the browser keeps nothing */ }
  };

  const play = (ticket) => {
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/play';
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'sso';
    input.value = ticket;
    form.append(input);
    document.body.append(form);
    form.submit();
  };

  const start = async () => {
    problem.hidden = true;
    delete logo.dataset.still;
    try {
      const site = await call('/site');
      if (!site.ok) return fail('The game is not reachable right now.');
      if (!site.data.viewer) {
        if (!site.data.guests) return fail('Guests can not join here right now.', false);
        const made = await call('/guest', 'POST');
        if (!made.ok) return fail(made.data.error || 'Could not open the game. Try again.');
        remember(made.data.password);
      }
      const ticket = await call('/me/ticket', 'POST');
      if (!ticket.ok || !ticket.data.ticket) return fail(ticket.data.error || 'Could not open the game. Try again.');
      play(ticket.data.ticket);
    } catch {
      fail('The game is not reachable right now.');
    }
  };

  logo.addEventListener('click', () => { if (logo.dataset.again) start(); });
  start();
})();
