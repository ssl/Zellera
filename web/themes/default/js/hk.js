'use strict';

(() => {
  const API = '/api/hk/v1';
  const NETWORK_ERROR = 'Could not reach the server. Check your connection and try again.';

  const api = async (path, { method = 'GET', body } = {}) => {
    const init = { method, credentials: 'same-origin', headers: { accept: 'application/json' } };
    if (body !== undefined) {
      init.headers['content-type'] = 'application/json';
      init.body = JSON.stringify(body);
    }
    const res = await fetch(`${API}${path}`, init);
    let data = {};
    try { data = await res.json(); } catch { }
    return { status: res.status, ok: res.ok && data.ok !== false, data };
  };
  const post = (path, body = {}) => api(path, { method: 'POST', body });

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

  const FIGURE = /^[a-z0-9.-]{1,256}$/;
  const avatar = (p, { full = false } = {}) => {
    if (me?.avatars === false || typeof p?.figure !== 'string' || !FIGURE.test(p.figure)) {
      const letters = [...String(p?.name ?? '')].filter((c) => /[\p{L}\p{N}]/u.test(c));
      const tone = letters.reduce((n, c) => n + c.toLowerCase().codePointAt(0), 0) % 6;
      return h('span', { class: `ini${full ? ' ini--l' : ''}`, dataset: { tone: String(tone) }, 'aria-hidden': 'true' }, (letters.slice(0, 2).join('') || '?').toUpperCase());
    }
    const q = new URLSearchParams({ figure: p.figure, gender: String(p.gender ?? '').toUpperCase() === 'F' ? 'F' : 'M', size: 'm' });
    if (!full) q.set('headonly', '1');
    return h('img', { class: `avatar avatar--${full ? 'full' : 'head'}`, src: `/imaging/avatarimage?${q}`, width: full ? 64 : 54, height: full ? 110 : 62, alt: '', loading: 'lazy' });
  };

  const panel = ({ title, body, tone = '', id, more, level = 2 }) => {
    const headingId = id ? `${id}-title` : undefined;
    return h('section', { class: `panel${tone ? ` panel--${tone}` : ''}`, id, 'aria-labelledby': headingId },
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
  const WIDE_FROM = 44;
  const textOf = (v) => [v].flat(Infinity).map((x) => (x instanceof Node ? x.textContent : x == null || x === false ? '' : String(x))).join('');
  const factsGrid = (rows, { raw = false } = {}) => h('dl', { class: `facts-grid${raw ? ' facts-grid--raw' : ''}` },
    rows.filter(Boolean).map(([k, v, span]) => h('div', { class: span === 'wide' || textOf(v).length > WIDE_FROM ? 'facts-grid__wide' : null },
      h('dt', null, k), h('dd', null, v))));
  const figures = (items, label) => h('dl', { class: 'figures', 'aria-label': label },
    items.filter((x) => x && x[0] != null).map(([value, text, href]) => h('div', null,
      h('dt', null, href ? h('a', { href }, text) : text), h('dd', null, typeof value === 'number' ? fmt.number(value) : value))));
  const table = ({ caption, head, rows, emptyText = 'Nothing to show.', cls = [], rowAttrs, className, stack = head.length > 3, main }) => {
    if (rows.length === 0) return empty(emptyText);
    const lead = main ?? Math.max(0, cls.findIndex((c) => /(^| )grow( |$)/.test(c ?? '')));
    const cell = (c, i) => h('td', { class: [cls[i], stack && i === lead ? 'cell--main' : ''].filter(Boolean).join(' ') || null,
      'data-label': stack && i !== lead ? textOf(head[i]) || null : null }, c);
    return h('div', { class: 'table-wrap', tabindex: 0, 'aria-label': caption },
      h('table', { class: [className, stack ? 'table--stack' : ''].filter(Boolean).join(' ') || null }, caption ? h('caption', { class: 'visually-hidden' }, caption) : '',
        h('thead', null, h('tr', null, head.map((c, i) => h('th', { scope: 'col', class: cls[i] || null }, c)))),
        h('tbody', null, rows.map((r, j) => h('tr', rowAttrs?.(j) ?? null, r.map(cell))))));
  };
  const showOpen = (row, open) => {
    if (open && row.scrollWidth > row.clientWidth) row.scrollLeft = open.offsetLeft - row.offsetLeft - (row.clientWidth - open.offsetWidth) / 2;
  };
  const pageTabs = (items, active, label) => {
    const row = h('nav', { class: 'hk-tabs', 'aria-label': label },
      items.map(([key, text, href, extra]) => h('a', { href, 'aria-current': key === active ? 'page' : null }, text, extra ?? '')));
    setTimeout(() => showOpen(row, [...row.children].find((a) => a.getAttribute('aria-current') === 'page')), 0);
    return row;
  };
  const head = ({ crumbs = [], title, id = 'page-title', picture, meta = [], actions = [], lead, tabs: pageTabsNode }) => {
    const metaItems = [meta].flat(Infinity).filter((x) => x != null && x !== false && x !== '');
    const actionItems = [actions].flat(Infinity).filter(Boolean);
    return h('header', { class: 'hk-head' },
      crumbs.length ? h('p', { class: 'hk-head__crumbs' }, crumbs.map(([text, href], i) => [i ? ' › ' : '', href ? h('a', { href }, text) : text])) : '',
      h('div', { class: 'hk-head__main' }, picture ?? '',
        h('div', { class: 'hk-head__text' }, h('h1', { id }, title), metaItems.length ? h('p', { class: 'hk-head__meta' }, metaItems) : '')),
      actionItems.length ? h('div', { class: 'hk-head__actions' }, actionItems) : '',
      lead ? h('p', { class: 'hk-head__lead' }, lead) : '',
      pageTabsNode ?? '');
  };
  const pager = ({ page, hasNext, param = 'page' }) => {
    if (page <= 1 && !hasNext) return '';
    const link = (p) => {
      const q = new URLSearchParams(location.search);
      if (p <= 1) q.delete(param); else q.set(param, String(p));
      const s = q.toString();
      return `${location.pathname}${s ? `?${s}` : ''}`;
    };
    return h('nav', { class: 'pager', 'aria-label': 'Pages' },
      page > 1 ? h('a', { class: 'btn btn--small', href: link(page - 1), rel: 'prev' }, 'Previous') : h('span'),
      h('span', null, `Page ${page}`),
      hasNext ? h('a', { class: 'btn btn--small', href: link(page + 1), rel: 'next' }, 'Next') : h('span'));
  };
  const busy = (btn) => {
    const label = btn.textContent;
    btn.disabled = true;
    btn.setAttribute('aria-busy', 'true');
    if (btn.dataset.busy) btn.textContent = btn.dataset.busy;
    return () => { btn.disabled = false; btn.removeAttribute('aria-busy'); btn.textContent = label; };
  };
  const field = ({ id, name, label, type = 'text', hint, value, options, ...attrs }) => h('div', { class: 'field' },
    h('label', { for: id }, label),
    type === 'textarea' ? h('textarea', { id, name, 'aria-describedby': `${id}-error`, ...attrs }, value ?? '')
      : type === 'select' ? h('select', { id, name, 'aria-describedby': `${id}-error`, ...attrs },
        (options ?? []).map(([v, text]) => h('option', { value: v, selected: String(v) === String(value ?? '') }, text)))
        : h('input', { id, name, type, value, 'aria-describedby': `${id}-error`, ...attrs }),
    hint ? h('p', { class: 'hint' }, hint) : '',
    h('p', { class: 'field-error', id: `${id}-error` }));
  const searchForm = ({ fields, submit = 'Search' }) => h('form', { class: 'search-form', method: 'get', action: location.pathname, role: 'search' },
    fields.map((f) => h('div', { class: 'field' }, h('label', { for: `f-${f.name}` }, f.label),
      h('input', { id: `f-${f.name}`, name: f.name, type: f.type ?? 'search', value: f.value ?? '', placeholder: f.placeholder, inputmode: f.inputmode, autocomplete: 'off', spellcheck: 'false' }))),
    h('button', { class: 'btn btn--go', type: 'submit' }, submit));

  const jsonForm = (form, path, { onDone, body = (f) => Object.fromEntries(new FormData(f)), confirmText } = {}) => {
    const top = form.querySelector('.alert') ?? form.insertBefore(alertBox(), form.firstChild);
    const fieldError = (name, message) => {
      const input = form.elements[name];
      if (!input || !input.id) return false;
      input.setAttribute('aria-invalid', message ? 'true' : 'false');
      const out = document.getElementById(`${input.id}-error`);
      if (out) out.textContent = message || '';
      return true;
    };
    form.addEventListener('submit', async (e) => {
      e.preventDefault();
      const btn = e.submitter ?? form.querySelector('button[type="submit"]');
      if ([...form.querySelectorAll('button[type="submit"]')].some((b) => b.disabled)) return;
      if (confirmText && !window.confirm(confirmText)) return;
      top.textContent = '';
      for (const input of form.querySelectorAll('input, textarea, select')) if (input.name) fieldError(input.name, '');
      const restore = btn ? busy(btn) : () => {};
      try {
        const r = await post(typeof path === 'function' ? path(form) : path, body(form, e));
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

  const enc = encodeURIComponent;
  const qs = (params) => {
    const q = new URLSearchParams();
    for (const [k, v] of Object.entries(params ?? {})) if (v != null && v !== '') q.set(k, String(v));
    const s = q.toString();
    return s ? `?${s}` : '';
  };
  const urls = {
    hk: () => '/housekeeping', users: (q) => `/housekeeping/users${qs({ q })}`, user: (id) => `/housekeeping/users/${Number(id)}`,
    userSanctions: (id) => `/housekeeping/users/${Number(id)}/sanctions`, userChatlog: (id, p) => `/housekeeping/users/${Number(id)}/chatlog${qs(p)}`,
    userVisits: (id, p) => `/housekeeping/users/${Number(id)}/visits${qs(p)}`, identity: (id) => `/housekeeping/identity/${Number(id)}`,
    rooms: (q) => `/housekeeping/rooms${qs({ q })}`, room: (id) => `/housekeeping/rooms/${Number(id)}`,
    roomChatlog: (id, p) => `/housekeeping/rooms/${Number(id)}/chatlog${qs(p)}`, photo: (id) => `/housekeeping/photos/${Number(id)}`,
    tickets: (state) => `/housekeeping/tickets${qs({ state })}`, ticket: (id) => `/housekeeping/tickets/${Number(id)}`,
    helpRequests: (state) => `/housekeeping/help-requests${qs({ state })}`, helpRequest: (id) => `/housekeeping/help-requests/${Number(id)}`,
    chatlogs: (p) => `/housekeeping/chatlogs${qs(p)}`, ban: (userId) => `/housekeeping/actions/ban${qs({ user: userId })}`,
    unban: (banId) => `/housekeeping/actions/unban${qs({ ban: banId })}`, audit: (p) => `/housekeeping/audit${qs(p)}`,
    landingView: () => '/housekeeping/landing-view', news: () => '/housekeeping/news', youtube: () => '/housekeeping/youtube',
    userDevices: (id) => `/housekeeping/users/${Number(id)}/devices`, userDevicesShared: (id, p) => `/housekeeping/users/${Number(id)}/devices/shared${qs(p)}`,
    devicesCompare: (p) => `/housekeeping/devices/compare${qs(p)}`, deviceBans: () => '/housekeeping/device-bans',
    deviceBanNew: (p) => `/housekeeping/device-bans/new${qs(p)}`,
    profile: (name) => `/profile/${enc(name)}`, room_public: (id) => `/room/${Number(id)}`,
  };

  const NAV = [
    ['dashboard', 'Dashboard', urls.hk(), ['moderation.tool']],
    ['users', 'Users', urls.users(), ['moderation.tool']],
    ['rooms', 'Rooms', urls.rooms(), ['moderation.room.info']],
    ['tickets', 'Tickets', urls.tickets(), ['moderation.tickets']],
    ['help', 'Help requests', urls.helpRequests(), ['moderation.tickets']],
    ['chatlogs', 'Chatlogs', urls.chatlogs(), ['moderation.room.chatlog', 'moderation.player.chatlog']],
    ['devicebans', 'Device bans', urls.deviceBans(), ['moderation.tool']],
    ['audit', 'Audit log', urls.audit(), ['moderation.player.ban']],
    ['landingview', 'Landing view', urls.landingView(), ['housekeeping.landing_view']],
    ['news', 'News', urls.news(), ['housekeeping.news']],
    ['youtube', 'YouTube', urls.youtube(), ['housekeeping.youtube']],
  ];

  let me = null;
  const setTitle = (title) => { document.title = `${title ? `${title} · ` : ''}Housekeeping · ${me?.name ?? ''}`; };

  const header = () => h('header', { class: 'header header--hk' },
    h('a', { class: 'logo', href: urls.hk() }, me?.name ?? '', h('span', { class: 'logo__sub' }, 'Housekeeping')),
    h('div', { class: 'header__side' },
      h('p', { class: 'header__who' }, me ? [h('b', null, me.staff.name), ' ', pill(me.staff.rankName), h('br')] : '', h('a', { href: '/me' }, 'Back to the site'))));

  const tabs = (active) => h('nav', { class: 'tabs', 'aria-label': 'Housekeeping' },
    h('ul', { class: 'tabs__main' }, [...NAV.filter(([, , , perms]) => me && perms.every((p) => me.can[p])),
      ...(me?.extensions ?? []).map((e) => [e.key, e.label, e.href])]
      .map(([key, label, href]) => h('li', null, h('a', { href, 'aria-current': key === active ? 'page' : null }, label)))),
    h('div', { class: 'tabs__sub' }, h('span', null, 'All times are UTC.')));

  const showError = (main, r) => {
    clear(main);
    const title = r.data?.title || (r.status === 401 ? 'Log in first' : 'Something went wrong');
    setTitle(title);
    main.append(h('div', { class: 'error-page' },
      h('p', { class: 'error-page__code' }, String(r.status || '')),
      h('h1', { id: 'error-title' }, title),
      h('p', null, r.data?.error || NETWORK_ERROR),
      h('p', { class: 'actions' }, r.status === 401 ? h('a', { class: 'btn btn--go', href: '/' }, 'Go to log in') : h('a', { class: 'btn', href: urls.hk() }, 'Back to the dashboard'))));
  };

  const start = async (render) => {
    const body = document.body;
    let r;
    try { r = await api('/me'); } catch { r = { status: 0, ok: false, data: {} }; }
    me = r.ok ? r.data : null;
    const main = h('main', { class: 'content', id: 'main' }, h('p', { class: 'loading' }, 'Loading…'));
    const nav = tabs(body.dataset.tab);
    clear(body).append(h('a', { class: 'skip', href: '#main' }, 'Skip to content'), h('div', { class: 'frame frame--wide' }, header(), nav, main));
    showOpen(nav.firstChild, [...nav.firstChild.children].map((li) => li.firstChild).find((a) => a.getAttribute('aria-current') === 'page'));
    setTitle(body.dataset.title ?? '');
    if (!r.ok) return showError(main, r);
    try {
      await render(main, me);
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

  const query = () => location.search;
  const param = (n) => location.pathname.split('/').filter(Boolean)[n] ?? '';

  window.HK = {
    API, api, post, h, clear, fmt, avatar, panel, cols, pill, empty, notice, alertBox, facts, factsGrid, figures, table, pageTabs, head, pager, busy, field,
    searchForm, jsonForm, urls, start, load, showError, setTitle, query, param, qs, NETWORK_ERROR,
    get me() { return me; },
  };
})();
