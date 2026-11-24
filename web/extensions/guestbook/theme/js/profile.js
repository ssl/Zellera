'use strict';

Site.widget('profile', ({ user, hidden }) => {
  if (hidden) return null;
  const { h, clear, panel, empty, notice, time, fmt, avatar, initials, profileLink, busy, alertBox, urls } = Site;
  const api = Site.extApi('guestbook');
  const base = `/entries/${encodeURIComponent(user.name)}`;
  const list = h('ol', { class: 'gb-list', 'aria-label': `Messages for ${user.name}` });
  const top = h('div', { class: 'gb-top' });
  const more = h('div', { class: 'gb-more' });
  const body = h('div', { class: 'gb' }, top, list, more);
  const box = panel({ id: 'guestbook', title: 'Guestbook', body });
  const title = box.querySelector('.panel__title');
  let total = 0;
  let oldest = 0;

  const setTotal = (n) => {
    total = Math.max(0, n);
    title.textContent = total ? `Guestbook (${fmt.number(total)})` : 'Guestbook';
  };

  const head = (p) => (Site.avatarsOn() ? h('span', { class: 'gb-head' }, avatar(p)) : h('span', { class: 'gb-head gb-head--plain' }, initials(p.name)));

  const item = (e) => {
    const li = h('li', { class: 'gb-entry', id: `gb-${e.id}` },
      head(e.author),
      h('div', { class: 'gb-entry__text' },
        h('p', { class: 'gb-bubble' }, e.message),
        h('p', { class: 'gb-meta' }, profileLink(e.author.name, h('b', null, e.author.name)), ' ', time(e.created, fmt.ago(e.created)),
          e.canDelete ? h('button', { class: 'link gb-delete', type: 'button', 'data-busy': 'Deleting…', onclick: (ev) => remove(e, li, ev.currentTarget) }, 'Delete') : '')));
    return li;
  };

  const showEmpty = () => {
    if (list.children.length) return;
    list.hidden = true;
    if (!body.querySelector('.gb-empty')) list.after(h('p', { class: 'empty gb-empty' }, Site.info?.viewer?.id === user.id ? 'No messages yet.' : `No messages yet. ${user.name}'s guestbook is waiting for its first one.`));
  };

  const remove = async (e, li, btn) => {
    if (!window.confirm('Delete this message for good?')) return;
    const restore = busy(btn);
    const r = await api.post(`${base}/${e.id}/delete`).catch(() => ({ ok: false, data: { error: Site.NETWORK_ERROR } }));
    if (!r.ok) { restore(); window.alert(r.data.error || 'Could not delete it. Try again.'); return; }
    li.remove();
    setTotal(total - 1);
    showEmpty();
  };

  const form = (r) => {
    const id = 'gb-message';
    const input = h('textarea', { id, name: 'message', rows: 2, maxlength: r.maxLength, required: true, 'aria-describedby': `${id}-count ${id}-error` });
    const count = h('span', { class: 'gb-count', id: `${id}-count`, 'aria-live': 'polite' }, `0 of ${r.maxLength}`);
    const send = h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Sending…' }, 'Sign the guestbook');
    const error = h('p', { class: 'field-error', id: `${id}-error` });
    const top = alertBox();
    const length = () => [...input.value].length;
    input.addEventListener('input', () => {
      count.textContent = `${length()} of ${r.maxLength}`;
      error.textContent = '';
      input.removeAttribute('aria-invalid');
    });
    const f = h('form', { class: 'gb-form', novalidate: true },
      top,
      h('label', { class: 'visually-hidden', for: id }, `A message for ${user.name}`),
      input,
      h('div', { class: 'gb-form__row' }, count, send),
      error);
    input.placeholder = `Say something nice to ${user.name}`;
    f.addEventListener('submit', async (ev) => {
      ev.preventDefault();
      if (send.disabled) return;
      top.textContent = '';
      if (!input.value.trim()) { error.textContent = 'Write a message first.'; input.setAttribute('aria-invalid', 'true'); input.focus(); return; }
      const restore = busy(send);
      let res;
      try { res = await api.post(base, { message: input.value }); } catch { res = { ok: false, data: { error: Site.NETWORK_ERROR } }; }
      restore();
      if (!res.ok) {
        const msg = res.data.error || 'Could not send it. Try again.';
        if (res.data.field === 'message') { error.textContent = msg; input.setAttribute('aria-invalid', 'true'); input.focus(); } else top.textContent = msg;
        return;
      }
      input.value = '';
      count.textContent = `0 of ${r.maxLength}`;
      body.querySelector('.gb-empty')?.remove();
      list.hidden = false;
      list.prepend(item(res.data.entry));
      setTotal(total + 1);
    });
    return f;
  };

  const writeArea = (r) => {
    if (r.canWrite) return form(r);
    if (r.reason === 'blocked') return notice(`${user.name} does not take messages from you.`);
    return h('p', { class: 'gb-login' }, h('a', { href: urls.home() }, 'Log in'), ` to write in ${user.name}'s guestbook.`);
  };

  const load = async (before = 0) => {
    let r;
    try { r = await api.get(`${base}${before ? `?before=${before}` : ''}`); } catch { r = { ok: false, data: { error: Site.NETWORK_ERROR } }; }
    if (!r.ok) {
      if (!before) { clear(body).append(empty(r.data.error || 'The guestbook could not be loaded.')); }
      return;
    }
    const d = r.data;
    if (!before) { clear(top).append(writeArea(d)); setTotal(d.total); }
    for (const e of d.entries) list.append(item(e));
    oldest = d.entries.length ? d.entries[d.entries.length - 1].id : oldest;
    clear(more);
    if (d.more) {
      const btn = h('button', { class: 'btn btn--small', type: 'button', 'data-busy': 'Loading…' }, 'Older messages');
      btn.addEventListener('click', async () => { const restore = busy(btn); await load(oldest); restore(); });
      more.append(btn);
    }
    list.hidden = list.children.length === 0;
    showEmpty();
  };
  load();
  return box;
});
