'use strict';

(() => {
  const API = '/api/site/v1';
  const NETWORK_ERROR = 'Could not reach the game. Check your connection and try again.';

  const call = (base) => async (path, { method = 'GET', body } = {}) => {
    const init = { method, credentials: 'same-origin', headers: { accept: 'application/json' } };
    if (body !== undefined) {
      init.headers['content-type'] = 'application/json';
      init.body = JSON.stringify(body);
    }
    const res = await fetch(`${base}${path}`, init);
    let data = {};
    try { data = await res.json(); } catch { }
    return { status: res.status, ok: res.ok && data.ok !== false, data };
  };
  const api = call(API);
  const post = (path, body = {}) => api(path, { method: 'POST', body });
  const extApi = (name) => {
    const ext = call(`/api/ext/${encodeURIComponent(name)}`);
    return { get: (path) => ext(path), post: (path, body = {}) => ext(path, { method: 'POST', body }) };
  };

  const widgets = new Map();
  const widget = (slot, render) => { widgets.set(slot, [...(widgets.get(slot) ?? []), render]); };
  const slot = (name, data) => (widgets.get(name) ?? []).map((render) => {
    try { return render(data); } catch (e) { console.error(e); return null; }
  }).filter((el) => el instanceof Node);

  const h = (tag, attrs, ...children) => {
    const el = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs ?? {})) {
      if (v == null || v === false) continue;
      if (k === 'class') el.className = v;
      else if (k === 'dataset') Object.assign(el.dataset, v);
      else if (k.startsWith('on') && typeof v === 'function') el.addEventListener(k.slice(2), v);
      else if (k === 'hidden' || k === 'disabled' || k === 'required' || k === 'checked' || k === 'selected') el[k] = Boolean(v);
      else if (k === 'value') el.value = v;
      else el.setAttribute(k, v === true ? '' : String(v));
    }
    append(el, children);
    return el;
  };
  const append = (el, children) => {
    for (const c of children.flat(Infinity)) {
      if (c == null || c === false || c === '') continue;
      el.append(c instanceof Node ? c : document.createTextNode(String(c)));
    }
    return el;
  };
  const clear = (el) => { while (el.firstChild) el.firstChild.remove(); return el; };

  const pad = (n) => String(n).padStart(2, '0');
  const fmt = {
    date: (unix) => {
      if (!(Number(unix) > 0)) return '-';
      const d = new Date(Number(unix) * 1000);
      return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}`;
    },
    dateTime: (unix) => {
      if (!(Number(unix) > 0)) return '-';
      const d = new Date(Number(unix) * 1000);
      return `${fmt.date(unix)} ${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())} UTC`;
    },
    ago: (unix) => {
      const t = Number(unix);
      if (!(t > 0)) return 'never';
      const s = Math.max(0, Math.floor(Date.now() / 1000) - t);
      for (const [size, name] of [[31536000, 'year'], [2592000, 'month'], [86400, 'day'], [3600, 'hour'], [60, 'minute']]) {
        if (s >= size) { const n = Math.floor(s / size); return `${n} ${name}${n === 1 ? '' : 's'} ago`; }
      }
      return 'just now';
    },
    number: (n) => Number(n ?? 0).toLocaleString('en-US'),
  };
  const time = (unix, text = fmt.date(unix)) => (Number(unix) > 0 ? h('time', { datetime: new Date(Number(unix) * 1000).toISOString() }, text) : '');

  const enc = encodeURIComponent;
  const urls = {
    home: () => '/', me: () => '/me', avatars: () => '/avatars', search: (name) => (name ? `/community/players?name=${enc(name)}` : '/community/players'), settings: () => '/settings',
    profile: (name) => `/profile/${enc(name)}`, photo: (name, id) => `/profile/${enc(name)}/photo/${enc(id)}`,
    photos: () => '/photos', room: (id) => `/room/${Number(id)}`, group: (id) => `/groups/${Number(id)}/id`,
    news: (id) => (id == null ? '/news' : `/news/${Number(id)}`), help: (topic) => (topic ? `/help/${enc(topic)}` : '/help'), ask: () => '/help/ask',
    staff: () => '/community/staff', safety: () => '/safety', terms: () => '/legal/terms', privacy: () => '/legal/privacy',
    apiDocs: () => '/api/public/api-docs/', hk: () => '/housekeeping',
  };
  const FIGURE = /^[a-z0-9.-]{1,256}$/;
  const avatarUrl = ({ figure, gender, head = false, size = 'm', action, gesture }) => {
    const q = new URLSearchParams({ figure, gender: String(gender ?? '').toUpperCase() === 'F' ? 'F' : 'M', size });
    if (head) q.set('headonly', '1');
    if (action) q.set('action', action);
    if (gesture) q.set('gesture', gesture);
    return `/imaging/avatarimage?${q}`;
  };
  const initials = (name, big = false) => {
    const letters = [...String(name ?? '')].filter((c) => /[\p{L}\p{N}]/u.test(c));
    const tone = letters.reduce((n, c) => n + c.toLowerCase().codePointAt(0), 0) % 6;
    return h('span', { class: `ini${big ? ` ini--${big === true ? 'l' : big}` : ''}`, dataset: { tone: String(tone) }, 'aria-hidden': 'true' },
      (letters.length ? letters.slice(0, 2).join('') : '?').toUpperCase());
  };
  const FULL = { m: [64, 110], l: [128, 220] };
  const avatar = (p, { full = false, size = 'm', action, gesture } = {}) => {
    if (info?.avatars === false || typeof p?.figure !== 'string' || !FIGURE.test(p.figure)) {
      return initials(p?.name, full ? (size === 'l' ? 'xl' : true) : false);
    }
    const [width, height] = full ? FULL[size] ?? FULL.m : [54, 62];
    return h('img', {
      class: `avatar avatar--${full ? `full avatar--${size}` : 'head'}`,
      src: avatarUrl({ figure: p.figure, gender: p.gender, head: !full, size: full ? size : 'm', action, gesture }),
      width, height, alt: '', loading: 'lazy', decoding: 'async',
    });
  };
  const avatarsOn = () => info?.avatars !== false;
  const profileLink = (name, content = name) => (info?.sees?.players === false ? h('span', null, content) : h('a', { href: urls.profile(name) }, content));
  const roomLink = (room, content = room.name) => (info?.sees?.players === false ? h('span', null, content) : h('a', { href: urls.room(room.id) }, content));

  const place = ({ capital = false, plural = false } = {}) => {
    const p = info?.place ?? {};
    const w = String((plural ? p.plural : p.word) || (plural ? 'games' : 'game'));
    return capital ? w.charAt(0).toUpperCase() + w.slice(1) : w;
  };

  const panel = ({ title, body, tone = '', id, more, tag = 'section', level = 2 }) => {
    const headingId = id ? `${id}-title` : undefined;
    return h(tag, { class: `panel${tone ? ` panel--${tone}` : ''}`, id, 'aria-labelledby': headingId },
      title ? h('div', { class: 'panel__head' }, h(`h${level}`, { class: 'panel__title', id: headingId }, title),
        more ? h('a', { class: 'panel__more', href: more[1] }, more[0]) : '') : '',
      h('div', { class: 'panel__body' }, body));
  };
  const cols = (kind, ...columns) => h('div', { class: `cols${kind ? ` cols--${kind}` : ''}` }, columns.map((c) => h('div', { class: 'col' }, c)));
  const pill = (text, kind = '') => h('span', { class: `pill${kind ? ` pill--${kind}` : ''}` }, text);
  const empty = (text) => h('p', { class: 'empty' }, text);
  const notice = (text, kind = '') => h('p', { class: `notice${kind ? ` notice--${kind}` : ''}`, role: 'status' }, text);
  const alertBox = (text = '') => h('p', { class: 'alert', role: 'alert', 'aria-live': 'assertive' }, text);
  const facts = (rows) => h('dl', { class: 'facts' }, rows.filter(Boolean).map(([k, v]) => [h('dt', null, k), h('dd', null, v)]));
  const table = ({ caption, head, rows, emptyText = 'Nothing to show.' }) => (rows.length === 0 ? empty(emptyText)
    : h('div', { class: 'table-wrap', tabindex: 0, 'aria-label': caption },
      h('table', { class: head.length > 4 ? 'table--wide' : null }, caption ? h('caption', { class: 'visually-hidden' }, caption) : '',
        h('thead', null, h('tr', null, head.map((c) => h('th', { scope: 'col' }, c)))),
        h('tbody', null, rows.map((r) => h('tr', null, r.map((c) => h('td', null, c))))))));
  const pager = ({ page, hasNext, go }) => {
    if (page <= 1 && !hasNext) return '';
    return h('nav', { class: 'pager', 'aria-label': 'Pages' },
      page > 1 ? h('button', { class: 'btn btn--small', type: 'button', onclick: () => go(page - 1) }, 'Previous') : h('span'),
      h('span', null, `Page ${page}`),
      hasNext ? h('button', { class: 'btn btn--small', type: 'button', onclick: () => go(page + 1) }, 'Next') : h('span'));
  };

  const busy = (btn) => {
    const label = btn.textContent;
    btn.disabled = true;
    btn.setAttribute('aria-busy', 'true');
    if (btn.dataset.busy) btn.textContent = btn.dataset.busy;
    return () => { btn.disabled = false; btn.removeAttribute('aria-busy'); btn.textContent = label; };
  };

  const field = ({ id, name, label, type = 'text', hint, value, ...attrs }) => h('div', { class: 'field' },
    h('label', { for: id }, label),
    type === 'textarea'
      ? h('textarea', { id, name, 'aria-describedby': `${id}-error${hint ? ` ${id}-hint` : ''}`, ...attrs }, value ?? '')
      : h('input', { id, name, type, value, 'aria-describedby': `${id}-error${hint ? ` ${id}-hint` : ''}`, ...attrs }),
    hint ? h('p', { class: 'hint', id: `${id}-hint` }, hint) : '',
    h('p', { class: 'field-error', id: `${id}-error` }));

  const jsonForm = (form, path, { onDone, body = (f) => Object.fromEntries(new FormData(f)), check } = {}) => {
    const top = form.querySelector('.alert') ?? form.insertBefore(alertBox(), form.firstChild);
    const fieldError = (name, message) => {
      const input = form.elements[name];
      if (!input || !input.id) return false;
      input.setAttribute('aria-invalid', message ? 'true' : 'false');
      const out = document.getElementById(`${input.id}-error`);
      if (out) out.textContent = message || '';
      return true;
    };
    form.addEventListener('input', (e) => { if (e.target.getAttribute('aria-invalid') === 'true') fieldError(e.target.name, ''); });
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn = form.querySelector('button[type="submit"]');
      if (btn?.disabled) return;
      top.textContent = '';
      for (const input of form.querySelectorAll('input, textarea, select')) if (input.name) fieldError(input.name, '');
      const local = check?.(form);
      if (local) {
        if (!fieldError(local[0], local[1])) top.textContent = local[1];
        form.elements[local[0]]?.focus();
        return;
      }
      const restore = btn ? busy(btn) : () => {};
      try {
        const r = await post(path, body(form));
        if (r.ok) { restore(); onDone?.(r.data, r); return; }
        const msg = r.data.error || 'Something went wrong. Try again.';
        if (r.data.field && fieldError(r.data.field, msg)) form.elements[r.data.field].focus();
        else top.textContent = msg;
      } catch {
        top.textContent = NETWORK_ERROR;
      }
      restore();
    });
    return form;
  };

  const postTicket = (win, ticket) => {
    const doc = win.document;
    const form = doc.createElement('form');
    form.method = 'post';
    form.action = new URL('/play', location.href).href;
    const input = doc.createElement('input');
    input.type = 'hidden';
    input.name = 'sso';
    input.value = ticket;
    form.append(input);
    doc.body.append(form);
    form.submit();
  };

  const openGame = async (btn, alertEl) => {
    if (btn.disabled) return;
    const win = window.open('about:blank', '_blank');
    const restore = busy(btn);
    if (alertEl) alertEl.textContent = '';
    try {
      const r = await post('/me/ticket');
      if (r.status === 401) { win?.close(); location.assign('/'); return; }
      if (!r.ok || !r.data.ticket) throw new Error(r.data.error || 'Could not open the game. Try again.');
      if (!win) postTicket(window, r.data.ticket);
      else if (!win.closed) { postTicket(win, r.data.ticket); win.opener = null; }
    } catch (err) {
      win?.close();
      if (alertEl) alertEl.textContent = err instanceof TypeError ? NETWORK_ERROR : err.message;
    } finally {
      restore();
    }
  };
  const logout = async () => {
    try { await post('/logout'); } finally { location.assign('/'); }
  };

  const community = (info) => {
    const sees = info.sees ?? {};
    const subs = [sees.community ? ['news', 'News', urls.news()] : null, sees.photos ? ['photos', 'Photos', urls.photos()] : null,
      info.viewer ? ['search', 'Find a player', urls.search()] : null, sees.staff ? ['staff', 'Staff', urls.staff()] : null].filter(Boolean);
    return subs.length ? { key: 'community', label: 'Community', href: subs[0][2], subs } : null;
  };

  const manyAvatars = (info) => Number(info.accounts?.avatarsPerAccount ?? 1) > 1;
  const homeSubs = (info) => {
    if (info.viewer) {
      return [['me', 'My page', urls.me()], ['profile', 'My profile', urls.profile(info.viewer.name)],
        manyAvatars(info) ? ['avatars', 'Your avatars', urls.avatars()] : null, ['settings', 'Settings', urls.settings()]];
    }
    if (info.choosingAvatar) return [['avatars', 'Your avatars', urls.avatars()]];
    return [['front', 'Log in', urls.home()], ['register', 'Create an account', '/#register']];
  };

  const TABS = (info) => [
    { key: 'home', label: 'Home', href: info.viewer ? urls.me() : info.choosingAvatar ? urls.avatars() : urls.home(), subs: homeSubs(info) },
    community(info),
    {
      key: 'help', label: 'Help', href: urls.help(),
      subs: [['help', 'Help', urls.help()], ['ask', 'Ask staff', urls.ask()], ['safety', 'Safety', urls.safety()], ['terms', 'Terms', urls.terms()], ['privacy', 'Privacy', urls.privacy()], ['api', 'Web API', urls.apiDocs()]],
    },
    info.housekeeping ? { key: 'hk', label: 'Housekeeping', href: urls.hk(), subs: [] } : null,
  ].filter(Boolean);

  const current = (on) => (on ? 'page' : null);

  const header = (info) => {
    const logOut = h('button', { class: 'link', type: 'button', id: 'log-out', onclick: logout }, 'Log out');
    const who = info.viewer
      ? h('p', { class: 'header__who' }, 'Checked in as ', h('a', { href: urls.profile(info.viewer.name) }, info.viewer.name), h('br'),
        manyAvatars(info) ? [h('a', { href: urls.avatars() }, 'Switch avatar'), ' or '] : '', logOut)
      : info.choosingAvatar
        ? h('p', { class: 'header__who' }, 'Logged in. Pick an avatar', h('br'), logOut)
        : h('p', { class: 'header__who' }, 'Been here before?', h('br'), h('a', { href: urls.home() }, 'Log in'));
    const enterAlert = alertBox();
    enterAlert.id = 'enter-alert';
    enterAlert.classList.add('header__alert');
    const enter = info.choosingAvatar ? h('a', { class: 'enter', href: urls.avatars() }, 'Pick an avatar') : info.viewer
      ? h('button', { class: 'enter', type: 'button', id: 'enter-game', 'data-busy': 'Opening…', onclick: (e) => openGame(e.currentTarget, enterAlert) }, `Enter the ${place()}`)
      : h('a', { class: 'enter', href: '/#register' }, 'Join for free');
    return h('header', { class: 'header' },
      h('a', { class: 'logo', href: info.viewer ? urls.me() : info.choosingAvatar ? urls.avatars() : urls.home() }, info.name),
      h('div', { class: 'header__side' }, who, enter), enterAlert);
  };

  const tabs = (info, tab, sub) => {
    const all = TABS(info);
    const open = all.find((t) => t.key === tab) ?? all[0];
    return h('nav', { class: 'tabs', 'aria-label': 'Site' },
      h('ul', { class: 'tabs__main' }, all.map((t) => h('li', null, h('a', { href: t.href, 'aria-current': current(t === open) }, t.label)))),
      h('ul', { class: 'tabs__sub' }, open.subs.filter(Boolean).map(([key, label, href]) => h('li', null, h('a', { href, 'aria-current': current(key === sub) }, label)))));
  };

  const footer = (info) => h('footer', { class: 'footer' },
    h('p', null, `${info.name} gives ${names().credits}, ${names().points} and ${names().premium} away. None of it is ever sold for money.`),
    h('ul', null, [info.sees?.community ? ['News', urls.news()] : null, ['Help', urls.help()], ['Safety', urls.safety()],
      info.sees?.staff ? ['Staff', urls.staff()] : null, ['Terms', urls.terms()], ['Privacy', urls.privacy()], ['Web API', urls.apiDocs()],
      ['Source', 'https://github.com/ssl/zellera']]
      .filter(Boolean).map(([label, href]) => h('li', null, h('a', { href }, label)))));

  let info = null;
  const names = () => ({ credits: 'credits', Credits: 'Credits', points: 'points', Points: 'Points', premium: 'premium', Premium: 'Premium', buildPass: 'build_pass', ...info?.names });
  const setTitle = (title) => { document.title = title ? `${title} · ${info?.name ?? ''}` : (info?.name ?? ''); };

  const showError = (main, r, { loginRedirect = true } = {}) => {
    if (r.status === 401 && loginRedirect) { location.assign('/'); return; }
    clear(main);
    setTitle(r.data?.title || 'Something went wrong');
    main.append(panel({ id: 'error', title: r.data?.title || 'Something went wrong', level: 1, tone: 'red', body: [
      r.status ? h('p', { class: 'error-page__code' }, String(r.status)) : '',
      h('p', null, r.data?.error || NETWORK_ERROR),
      h('p', { class: 'actions' }, h('a', { class: 'btn', href: info?.viewer ? urls.me() : urls.home() }, 'Go to the front page'))] }));
  };

  const start = async (render, { tabOf } = {}) => {
    const body = document.body;
    let r;
    try { r = await api('/site'); } catch { r = { status: 0, ok: false, data: {} }; }
    info = r.ok ? r.data : { name: '', place: { word: 'game', plural: 'games' }, avatars: true, viewer: null, sees: {}, housekeeping: false };
    const kept = body.querySelector(':scope > main');
    const main = kept ?? h('main', { class: 'content', id: 'main' }, h('p', { class: 'loading' }, 'Loading…'));
    const at = { tab: body.dataset.tab, sub: body.dataset.sub, ...tabOf?.(info) };
    const nav = tabs(info, at.tab, at.sub);
    clear(body).append(
      h('a', { class: 'skip', href: '#main' }, 'Skip to content'),
      h('div', { class: `frame${body.dataset.frame === 'wide' ? ' frame--wide' : ''}` }, header(info), nav, main, footer(info)));
    for (const row of nav.children) {
      const open = [...row.children].map((li) => li.firstChild).find((a) => a?.getAttribute('aria-current') === 'page');
      if (open && row.scrollWidth > row.clientWidth) row.scrollLeft = open.offsetLeft - row.offsetLeft - (row.clientWidth - open.offsetWidth) / 2;
    }
    if (kept) {
      if (location.hash) document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView();
      return render(main, info);
    }
    setTitle(body.dataset.title ?? '');
    if (!r.ok) return showError(main, r, { loginRedirect: false });
    try {
      await render(main, info);
    } catch (e) {
      console.error(e);
      showError(main, { status: 0, data: {} });
    }
  };

  const load = async (main, path, render) => {
    let r;
    try { r = await api(path); } catch { r = { status: 0, ok: false, data: {} }; }
    if (!r.ok) return showError(main, r);
    clear(main);
    return render(r.data);
  };

  window.Site = {
    API, api, post, names, manyAvatars, extApi, widget, slot, h, append, clear, fmt, time, urls, avatarUrl, avatar, avatarsOn, profileLink, roomLink, initials, place, panel, cols, pill, empty, notice,
    alertBox, facts, table, pager, busy, field, jsonForm, openGame, logout, start, load, showError, setTitle, NETWORK_ERROR,
    get info() { return info; },
  };
})();
