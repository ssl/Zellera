
import { checkShape } from './shape.mjs';

const STATUS_TEXT = {
  200: 'OK', 201: 'Created', 204: 'No Content', 301: 'Moved Permanently', 302: 'Found', 304: 'Not Modified',
  400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden', 404: 'Not Found', 405: 'Method Not Allowed',
  413: 'Payload Too Large', 415: 'Unsupported Media Type', 429: 'Too Many Requests', 500: 'Internal Server Error',
  502: 'Bad Gateway', 503: 'Service Unavailable', 504: 'Gateway Timeout',
};
const TIMEOUT_MS = 20000;
const KEY_PLACEHOLDER = { 'x-wired-read-key': '<read key>', 'x-wired-write-key': '<write key>' };
const CSS_MASK = typeof CSS !== 'undefined' && CSS.supports('-webkit-text-security', 'disc');

const el = (tag, attrs = {}, ...children) => {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (v == null || v === false) continue;
    if (k === 'class') node.className = v;
    else if (k === 'text') node.textContent = v;
    else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2), v);
    else node.setAttribute(k, v === true ? '' : String(v));
  }
  for (const c of children.flat(Infinity)) if (c != null && c !== false) node.append(c instanceof Node ? c : String(c));
  return node;
};
let uid = 0;
const nextId = (p) => `${p}-${++uid}`;
const methodPill = (m) => el('span', { class: `method method-${m.toLowerCase()}`, text: m });
const shortPath = (p) => (p.startsWith('/api/public/') ? p.slice('/api/public'.length) : p);
const pathNodes = (p) => p.split('/').flatMap((seg, i) => (i === 0 ? [seg] : ['/', el('wbr'), seg]));

const JSON_TOKEN = /("(?:\\.|[^"\\])*")(\s*:)?|\b(true|false|null)\b|(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g;
const highlightJson = (src) => {
  const frag = document.createDocumentFragment();
  let at = 0;
  for (const m of src.matchAll(JSON_TOKEN)) {
    if (m.index > at) frag.append(src.slice(at, m.index));
    if (m[1]) {
      frag.append(el('span', { class: m[2] ? 'j-key' : 'j-str', text: m[1] }));
      if (m[2]) frag.append(m[2]);
    } else frag.append(el('span', { class: m[3] ? 'j-lit' : 'j-num', text: m[0] }));
    at = m.index + m[0].length;
  }
  if (at < src.length) frag.append(src.slice(at));
  return frag;
};
const codePre = (label, content) => el('pre', { class: 'code', tabindex: '0', 'aria-label': label }, el('code', {}, content));

const shellQuote = (s) => `'${String(s).replace(/'/g, `'\\''`)}'`;
const mask = (v) => (v.length <= 4 ? '*'.repeat(v.length) : v.slice(0, 4) + '*'.repeat(v.length - 4));

const setupSideList = () => {
  const toc = document.querySelector('.docs-toc');
  if (!toc) return;
  const narrow = window.matchMedia('(max-width: 63.99rem)');
  if (narrow.matches) toc.open = false;
  narrow.addEventListener('change', (e) => { toc.open = !e.matches; });
  toc.addEventListener('click', (e) => {
    if (narrow.matches && e.target.closest('a')) toc.open = false;
  });
  const filter = document.getElementById('toc-filter');
  if (!filter) return;
  filter.disabled = false;
  const empty = el('p', { class: 'toc-empty', hidden: true, text: 'No endpoint matches.' });
  filter.closest('.toc-filter').after(empty);
  filter.addEventListener('input', () => {
    const words = filter.value.toLowerCase().split(/\s+/).filter(Boolean);
    let shown = 0;
    for (const list of toc.querySelectorAll('.toc-ops')) {
      let any = false;
      for (const li of list.children) {
        const hit = words.every((w) => li.dataset.filter.includes(w));
        li.hidden = !hit;
        any ||= hit;
        if (hit) shown++;
      }
      list.hidden = !any;
      list.previousElementSibling.hidden = !any;
    }
    empty.hidden = shown > 0;
  });
};

const harvest = { user: new Map(), group: new Map(), room: new Map() };
const USER_ID = /^hh[a-z0-9]{2,8}-[0-9a-f]{32}$/;
const GROUP_ID = /^g-hh[a-z0-9]{2,8}-[0-9a-f]{32}$/;
const collectIds = (value) => {
  const walk = (v) => {
    if (Array.isArray(v)) { v.forEach(walk); return; }
    if (!v || typeof v !== 'object') return;
    for (const [k, x] of Object.entries(v)) {
      if (typeof x === 'string' && USER_ID.test(x) && !harvest.user.has(x)) harvest.user.set(x, k === 'uniqueId' && typeof v.name === 'string' ? v.name : '');
      if (typeof x === 'string' && GROUP_ID.test(x) && !harvest.group.has(x)) harvest.group.set(x, k === 'id' && typeof v.name === 'string' ? v.name : '');
      if (x && typeof x === 'object') walk(x);
    }
    if (Number.isInteger(v.id) && typeof v.uniqueId === 'string' && v.uniqueId.startsWith('r-') && !harvest.room.has(String(v.id))) {
      harvest.room.set(String(v.id), typeof v.name === 'string' ? v.name : '');
    }
  };
  walk(value);
};
const idKindOf = (endpoint, param) => {
  if (param.in !== 'path') return null;
  if (param.name === 'roomId') return 'room';
  if (param.name !== 'id') return null;
  return /\/groups\/\{id\}/.test(endpoint.path) ? 'group' : 'user';
};

const startTester = async (app) => {
  let spec;
  try {
    const r = await fetch(app.dataset.data || '/api/public/api-docs/api.json', { cache: 'no-cache', credentials: 'omit' });
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    spec = await r.json();
  } catch (e) {
    app.replaceChildren(el('p', { class: 'alert', text: `The tester could not load the API data (${e.message}).` }));
    return null;
  }
  const endpoints = spec.endpoints;
  const byId = new Map(endpoints.map((e) => [e.id, e]));
  const typed = new Map();
  const keys = new Map();
  let current = null;
  let confirmed = false;
  let inflight = null;

  const select = el('select', { id: 't-op' },
    spec.groups.map((g) => el('optgroup', { label: g.extra ? `${g.name} (extra of this game)` : g.name },
      endpoints.filter((e) => e.group === g.id).map((e) => el('option', { value: e.id, text: `${e.method} ${shortPath(e.path)}: ${e.summary}` })))));
  const chosen = el('p', { class: 't-chosen' });
  const fieldsBox = el('div', { class: 't-groups' });
  const sendBtn = el('button', { class: 'btn btn--go', type: 'button', text: 'Send request' });
  const curlBtn = el('button', { class: 'btn', type: 'button', text: 'Copy as curl' });
  const resetBtn = el('button', { class: 'link', type: 'button', text: 'Reset to example' });
  const confirmText = el('p', { class: 'notice notice--warn' });
  const confirmYes = el('button', { class: 'btn btn--danger', type: 'button', text: 'Yes, send it' });
  const confirmNo = el('button', { class: 'btn', type: 'button', text: 'Cancel' });
  const confirmBox = el('div', { class: 't-confirm', hidden: true, role: 'group', 'aria-label': 'Confirm' }, confirmText, el('div', { class: 't-actions' }, confirmYes, confirmNo));
  const status = el('p', { class: 't-status', role: 'status', 'aria-live': 'polite' });
  const reqPane = el('section', { class: 't-pane', 'aria-labelledby': 't-req-title' });
  const resPane = el('section', { class: 't-pane', 'aria-labelledby': 't-res-title' });
  const out = el('div', { class: 't-out', hidden: true }, reqPane, resPane);
  const curlPre = codePre('curl command', '');
  const curlBox = el('details', { class: 't-curl', open: true }, el('summary', { text: 'The request as curl (keys as placeholders)' }), curlPre);
  const datalists = Object.fromEntries(Object.keys(harvest).map((k) => [k, el('datalist', { id: `t-ids-${k}` })]));

  const form = el('div', { class: 't-form', role: 'group', 'aria-label': 'Request' },
    el('div', { class: 't-pick field' }, el('label', { for: 't-op', text: 'Endpoint' }), select),
    chosen, fieldsBox,
    el('div', { class: 't-actions' }, sendBtn, curlBtn, resetBtn),
    confirmBox, status, Object.values(datalists));
  const wire = el('div', { class: 't-wire' }, curlBox, out);
  app.replaceChildren(form, wire);

  const fieldOf = new Map();
  let bodyField = null;

  const paramValue = (p) => {
    const key = `${p.in}:${p.name}`;
    if (p.secret) return keys.get(p.name.toLowerCase()) ?? '';
    if (typed.has(key)) return typed.get(key);
    const v = current.tester?.params?.[p.name];
    return v === undefined || v === null ? '' : String(v);
  };

  const buildField = (p) => {
    const id = nextId('t-f');
    const hintId = `${id}-hint`;
    const errId = `${id}-err`;
    const label = el('label', { for: id }, el('code', { text: p.name }), el('span', { class: 't-in', text: p.in }),
      p.required ? el('span', { class: 'pill pill--bad', text: 'required' }) : null);
    const described = `${hintId} ${errId}`;
    const kind = idKindOf(current, p);
    let input;
    const enumValues = p.schema?.enum;
    if (enumValues && !p.secret) {
      input = el('select', { id, 'aria-describedby': described },
        p.required && p.in === 'path' ? null : el('option', { value: '', text: '(not sent)' }),
        enumValues.map((v) => el('option', { value: String(v), text: String(v) })));
    } else {
      input = el('input', {
        id, type: p.secret && !CSS_MASK ? 'password' : 'text', class: p.secret && CSS_MASK ? 'key-masked' : null,
        autocomplete: 'off', autocapitalize: 'off', spellcheck: 'false',
        inputmode: p.schema?.type === 'integer' ? 'numeric' : null, 'aria-describedby': described,
        list: kind ? `t-ids-${kind}` : null, 'data-1p-ignore': p.secret ? 'true' : null, 'data-lpignore': p.secret ? 'true' : null,
        'data-bwignore': p.secret ? 'true' : null, 'data-form-type': p.secret ? 'other' : null,
        placeholder: p.secret ? 'paste the key from the Web API box' : (p.in === 'path' ? '' : '(not sent when empty)'),
      });
    }
    input.value = paramValue(p);
    if (enumValues && input.value === '' && p.required && p.in === 'path') input.value = String(enumValues[0]);
    input.addEventListener('input', () => {
      if (p.secret) keys.set(p.name.toLowerCase(), input.value);
      else typed.set(`${p.in}:${p.name}`, input.value);
      input.removeAttribute('aria-invalid');
      error.textContent = '';
      confirmed = false;
      hideConfirm();
      updateCurl();
    });
    input.addEventListener('change', () => input.dispatchEvent(new Event('input')));
    const error = el('p', { class: 't-error', id: errId });
    let control = input;
    if (p.secret) {
      const toggle = el('button', { class: 'btn', type: 'button', 'aria-pressed': 'false', 'aria-controls': id, text: 'Show' });
      toggle.addEventListener('click', () => {
        const show = toggle.getAttribute('aria-pressed') !== 'true';
        if (CSS_MASK) input.classList.toggle('key-masked', !show);
        else input.type = show ? 'text' : 'password';
        toggle.setAttribute('aria-pressed', String(show));
        toggle.textContent = show ? 'Hide' : 'Show';
      });
      control = el('div', { class: 't-key' }, input, toggle);
    }
    const tip = kind === 'user' ? ' Ids from earlier answers are suggested here: look a user up by name first.'
      : kind ? ' Ids from earlier answers are suggested here.' : '';
    const hint = el('p', { class: 'hint', id: hintId, text: `${p.description ?? ''}${tip}` });
    fieldOf.set(`${p.in}:${p.name}`, { param: p, input, error });
    return el('div', { class: p.secret || p.in === 'header' ? 'field field-wide' : 'field' }, label, control, hint, error);
  };

  const renderFields = () => {
    fieldOf.clear();
    bodyField = null;
    const groups = [['path', 'Path'], ['query', 'Query'], ['header', 'Headers']];
    const sets = [];
    for (const [where, title] of groups) {
      const params = (current.params ?? []).filter((p) => p.in === where);
      if (!params.length) continue;
      sets.push(el('fieldset', {}, el('legend', { text: title }), el('div', { class: 't-fields' }, params.map(buildField))));
    }
    if (current.params?.some((p) => p.secret)) {
      const forget = el('button', { class: 'btn btn--small', type: 'button', text: 'Forget keys' });
      forget.addEventListener('click', () => {
        keys.clear();
        for (const { param, input } of fieldOf.values()) if (param.secret) input.value = '';
        updateCurl();
        status.textContent = 'Keys cleared from this page.';
      });
      sets.push(el('p', { class: 't-hint-row hint' }, 'Keys stay in this page only while it is open.', forget, el('a', { href: '#auth', text: 'How to get keys' })));
    }
    if (current.requestBody) {
      const id = nextId('t-body');
      const textarea = el('textarea', { id, spellcheck: 'false', autocapitalize: 'off', rows: '10', 'aria-describedby': `${id}-hint ${id}-err` });
      const saved = typed.get(`body:${current.id}`);
      textarea.value = saved ?? (current.tester?.body == null ? '' : JSON.stringify(current.tester.body, null, 2));
      const error = el('p', { class: 't-error', id: `${id}-err` });
      textarea.addEventListener('input', () => {
        typed.set(`body:${current.id}`, textarea.value);
        error.textContent = '';
        confirmed = false;
        hideConfirm();
        updateCurl();
      });
      bodyField = { textarea, error };
      sets.push(el('div', { class: 'field' },
        el('label', { for: id }, 'Body ', el('span', { class: 't-in', text: current.requestBody.contentType ?? 'application/json' }), current.requestBody.required ? el('span', { class: 'pill pill--bad', text: 'required' }) : null),
        textarea,
        el('p', { class: 'hint', id: `${id}-hint`, text: 'JSON, sent with Content-Type: application/json. Text that is not JSON is sent as typed (the API then answers 400).' }),
        error));
    }
    if (!sets.length) sets.push(el('p', { class: 'muted', text: 'This endpoint takes no parameters.' }));
    fieldsBox.replaceChildren(...sets);
  };

  const choose = (id, { focus = false } = {}) => {
    const e = byId.get(id) ?? endpoints[0];
    current = e;
    select.value = e.id;
    confirmed = false;
    hideConfirm();
    chosen.replaceChildren(methodPill(e.method), el('code', {}, pathNodes(e.path)), el('span', { text: e.summary }),
      el('a', { href: `#op-${e.id}`, text: 'Read the docs' }));
    renderFields();
    for (const [kind, list] of Object.entries(datalists)) list.replaceChildren(...[...harvest[kind]].map(([v, label]) => el('option', { value: v, label: label || null })));
    updateCurl();
    if (focus) {
      const first = fieldsBox.querySelector('input, select, textarea');
      (first ?? select).focus({ preventScroll: true });
    }
  };

  const buildRequest = ({ forCurl = false } = {}) => {
    const problems = [];
    const values = {};
    for (const [key, f] of fieldOf) values[key] = f.input.value;
    const path = current.path.replace(/\{([^}]+)\}/g, (_, name) => {
      const v = values[`path:${name}`] ?? '';
      if (v === '') problems.push(`path:${name}`);
      return encodeURIComponent(v);
    });
    const query = new URLSearchParams();
    const headers = [];
    for (const p of current.params ?? []) {
      const v = values[`${p.in}:${p.name}`] ?? '';
      if (p.in === 'query' && v !== '') query.append(p.name, v);
      if (p.in === 'header' && v !== '') headers.push([p.name, forCurl && KEY_PLACEHOLDER[p.name.toLowerCase()] ? KEY_PLACEHOLDER[p.name.toLowerCase()] : v, !!p.secret]);
      if (p.in === 'header' && v === '' && p.secret && forCurl) headers.push([p.name, KEY_PLACEHOLDER[p.name.toLowerCase()] ?? '<key>', true]);
    }
    if (!forCurl && headers.some(([name]) => name.toLowerCase() === 'if-none-match')) headers.push(['Cache-Control', 'max-age=0', false]);
    let body = null;
    let bodyNote = '';
    if (current.requestBody && bodyField) {
      body = bodyField.textarea.value;
      if (body.trim() === '') body = null;
      else {
        try { body = JSON.stringify(JSON.parse(body)); } catch { bodyNote = 'not valid JSON: sent as typed'; }
      }
      if (body !== null) headers.push(['Content-Type', current.requestBody.contentType ?? 'application/json', false]);
    }
    const qs = query.toString();
    const url = `${location.origin}${path}${qs ? `?${qs}` : ''}`;
    return { method: current.method, url, path: `${path}${qs ? `?${qs}` : ''}`, headers, body, bodyNote, problems };
  };

  const curlOf = (req) => {
    const parts = ['curl -i'];
    if (req.method !== 'GET') parts.push(`-X ${req.method}`);
    parts.push(shellQuote(req.url));
    for (const [name, value] of req.headers) parts.push(`-H ${shellQuote(`${name}: ${value}`)}`);
    if (req.body !== null) parts.push(`--data-raw ${shellQuote(req.body)}`);
    return parts.join(' \\\n  ');
  };
  const updateCurl = () => {
    if (!current) return;
    curlPre.firstChild.textContent = curlOf(buildRequest({ forCurl: true }));
  };

  const needsConfirm = (e) => /\/bulk-delete$|\/batch$/.test(e.path) && e.group === 'Wired Variables';
  function hideConfirm() { confirmBox.hidden = true; }
  const askConfirm = (req) => {
    const room = fieldOf.get('path:roomId')?.input.value ?? '?';
    let n = 0;
    try { n = JSON.parse(req.body ?? '{}').requests?.length ?? 0; } catch { }
    confirmText.textContent = /bulk-delete$/.test(current.path)
      ? `This deletes the listed variables from every user and furni in room ${room}, for good. Send it?`
      : `This runs ${n || 'the'} operation${n === 1 ? '' : 's'} in room ${room}, writes and deletes included. Send it?`;
    confirmBox.hidden = false;
    confirmYes.focus();
  };
  confirmYes.addEventListener('click', () => { confirmed = true; hideConfirm(); send(); });
  confirmNo.addEventListener('click', () => { hideConfirm(); status.textContent = 'Not sent.'; sendBtn.focus(); });

  const showRequest = (req) => {
    const lines = [`${req.method} ${req.url}`];
    for (const [name, value, secret] of req.headers) lines.push(`${name}: ${secret ? mask(value) : value}`);
    const text = req.body !== null ? `${lines.join('\n')}\n\n${req.body}` : lines.join('\n');
    reqPane.replaceChildren(
      el('h3', { id: 't-req-title', text: 'Request' }),
      el('p', { class: 't-line' }, methodPill(req.method), el('code', { text: req.path })),
      codePre('Request', text),
      el('p', { class: 'hint', text: `The headers this page sets${req.headers.some((h) => h[2]) ? ' (keys masked)' : ''}; the browser adds its own, such as User-Agent and Accept.${req.headers.some(([n]) => n === 'Cache-Control') ? ' Cache-Control: max-age=0 keeps the browser from sending no-cache, which would make the server ignore If-None-Match.' : ''}${req.bodyNote ? ` Body ${req.bodyNote}.` : ''}` }));
  };

  const documented = (statusCode) => current.responses?.[String(statusCode)] ?? null;

  const shapeNote = (res, bodyText, json) => {
    const doc = documented(res.status);
    const type = (res.headers.get('content-type') ?? '').split(';')[0].trim();
    if (res.status === 404 && type === 'text/html') {
      return { ok: false, text: 'HTML 404: nothing answers this path and method here (an unrouted value, or the endpoint\'s handler is not installed yet).' };
    }
    if (!doc) return { ok: false, text: `Status ${res.status} is not documented for this endpoint.` };
    if (!doc.contentType) return bodyText === '' ? { ok: true, text: `No body, as documented for ${res.status}.` } : { ok: false, text: `A body came back, but ${res.status} is documented without one.` };
    const docType = doc.contentType.split(';')[0].trim();
    if (type !== docType) return { ok: false, text: `Content type ${type || '(none)'} differs from the documented ${docType}.` };
    if (!doc.schema || json === undefined) return { ok: true, text: `Documented ${res.status} answer (${docType}).` };
    const problems = checkShape(json, doc.schema, spec.schemas, { strictKeys: true });
    if (!problems.length) return { ok: true, text: `The body has the documented shape of the ${res.status} answer.` };
    return { ok: false, text: `The body differs from the documented ${res.status} answer:`, list: problems };
  };

  const showResponse = (req, res, bodyText, ms) => {
    const type = res.headers.get('content-type') ?? '';
    let json;
    let bodyNode;
    if (bodyText === '') bodyNode = el('p', { class: 'muted', text: '(empty body)' });
    else if (/json/i.test(type)) {
      try {
        json = JSON.parse(bodyText);
        bodyNode = codePre('Response body', highlightJson(JSON.stringify(json, null, 2)));
        collectIds(json);
      } catch { bodyNode = codePre('Response body', bodyText); }
    } else bodyNode = codePre('Response body', bodyText);
    const note = shapeNote(res, bodyText, json);
    const headerText = [...res.headers].map(([k, v]) => `${k}: ${v}`).join('\n');
    const etag = res.headers.get('etag');
    const inm = fieldOf.get('header:If-None-Match');
    const extra = [];
    if (etag && inm && res.status === 200) {
      const again = el('button', { class: 'btn', type: 'button', text: 'Send again with this ETag' });
      again.addEventListener('click', () => {
        inm.input.value = etag;
        inm.input.dispatchEvent(new Event('input'));
        send();
      });
      extra.push(el('p', { class: 't-hint-row hint' }, 'A GET with this ETag in If-None-Match answers 304 while nothing changed.', again));
    }
    const kb = new TextEncoder().encode(bodyText).length;
    resPane.replaceChildren(
      el('h3', { id: 't-res-title', text: 'Response' }),
      el('p', { class: 't-line' },
        el('span', { class: `status status-${String(res.status)[0]}xx`, text: String(res.status) }),
        el('strong', { text: STATUS_TEXT[res.status] ?? '' }),
        el('span', { class: 'muted', text: `${ms} ms` }),
        el('span', { class: 'muted', text: `${kb} byte${kb === 1 ? '' : 's'}` })),
      el('div', { class: `t-check notice ${note.ok ? 'notice--ok' : 'notice--warn'}` }, note.text, note.list ? el('ul', {}, note.list.map((p) => el('li', { text: p }))) : null),
      el('h4', { class: 't-sub', text: 'Body' }),
      bodyNode,
      el('h4', { class: 't-sub', text: 'Headers' }),
      codePre('Response headers', headerText || '(none)'),
      ...extra);
    return note;
  };

  async function send() {
    const req = buildRequest();
    let firstBad = null;
    for (const key of req.problems) {
      const f = fieldOf.get(key);
      if (!f) continue;
      f.input.setAttribute('aria-invalid', 'true');
      f.error.textContent = `Fill in ${f.param.name}: it is part of the path.`;
      firstBad ??= f.input;
    }
    if (firstBad) { status.textContent = 'Fill in the path parameters first.'; firstBad.focus(); return; }
    if (needsConfirm(current) && !confirmed) { askConfirm(req); return; }
    confirmed = false;
    inflight?.abort();
    const ctrl = new AbortController();
    inflight = ctrl;
    const timer = setTimeout(() => ctrl.abort(), TIMEOUT_MS);
    const headers = {};
    for (const [name, value] of req.headers) headers[name] = value;
    sendBtn.setAttribute('aria-busy', 'true');
    sendBtn.textContent = 'Sending…';
    status.textContent = `Sending ${req.method} ${req.path}`;
    showRequest(req);
    out.hidden = false;
    const started = performance.now();
    try {
      const res = await fetch(req.url, {
        method: req.method, headers, body: req.body ?? undefined, cache: 'no-store', credentials: 'omit', redirect: 'manual', signal: ctrl.signal,
      });
      const ms = Math.round(performance.now() - started);
      if (res.type === 'opaqueredirect') {
        resPane.replaceChildren(el('h3', { id: 't-res-title', text: 'Response' }),
          el('p', { class: 'notice notice--warn', text: 'A redirect: the browser does not let the page see where it points. Try the curl command.' }));
        status.textContent = `Redirect after ${ms} ms.`;
        return;
      }
      const text = await res.text();
      const note = showResponse(req, res, text, ms);
      status.textContent = `${res.status} ${STATUS_TEXT[res.status] ?? ''} in ${ms} ms. ${note.ok ? 'Shape as documented.' : 'See the note under Response.'}`;
    } catch (e) {
      const aborted = e.name === 'AbortError';
      resPane.replaceChildren(el('h3', { id: 't-res-title', text: 'Response' }),
        el('p', { class: 'alert', text: aborted ? `No answer within ${TIMEOUT_MS / 1000} s (or a newer request replaced it).` : `No answer: ${e.message}` }));
      status.textContent = aborted ? 'The request was stopped.' : 'The request failed.';
    } finally {
      clearTimeout(timer);
      if (inflight === ctrl) inflight = null;
      sendBtn.removeAttribute('aria-busy');
      sendBtn.textContent = 'Send request';
      updateCurl();
    }
  }

  sendBtn.addEventListener('click', () => send());
  form.addEventListener('keydown', (e) => {
    if (e.key !== 'Enter' || e.isComposing || !(e.target instanceof HTMLInputElement)) return;
    e.preventDefault();
    send();
  });
  select.addEventListener('change', () => {
    choose(select.value);
    history.replaceState(null, '', `#try-${current.id}`);
    out.hidden = true;
    status.textContent = '';
  });
  resetBtn.addEventListener('click', () => {
    for (const p of current.params ?? []) typed.delete(`${p.in}:${p.name}`);
    typed.delete(`body:${current.id}`);
    choose(current.id);
    status.textContent = 'Back to the example values (keys kept).';
  });
  curlBtn.addEventListener('click', async () => {
    const text = curlOf(buildRequest({ forCurl: true }));
    try {
      await navigator.clipboard.writeText(text);
      status.textContent = 'Copied the curl command (keys as placeholders).';
    } catch {
      const range = document.createRange();
      range.selectNodeContents(curlPre);
      const sel = window.getSelection();
      sel.removeAllRanges();
      sel.addRange(range);
      status.textContent = 'Could not copy here: the curl command is selected, copy it with Ctrl+C or Cmd+C.';
    }
  });

  choose(location.hash.startsWith('#try-') && byId.has(location.hash.slice(5)) ? location.hash.slice(5) : endpoints[0].id);
  return { choose, byId };
};

const start = async () => {
  window.Site?.start(() => {});
  setupSideList();
  const app = document.getElementById('tester-app');
  if (!app) return;
  const tester = await startTester(app);
  if (!tester) return;
  const section = document.getElementById('tester');
  const openTester = (id) => {
    tester.choose(id, { focus: true });
    history.replaceState(null, '', `#try-${id}`);
    section.scrollIntoView({ block: 'start' });
  };
  document.addEventListener('click', (e) => {
    const link = e.target.closest('a[data-try]');
    if (!link || !tester.byId.has(link.dataset.try)) return;
    e.preventDefault();
    openTester(link.dataset.try);
  });
  if (location.hash.startsWith('#try-')) section.scrollIntoView({ block: 'start' });
  else if (location.hash.length > 1) document.getElementById(decodeURIComponent(location.hash.slice(1)))?.scrollIntoView({ block: 'start' });
  window.addEventListener('hashchange', () => {
    const id = location.hash.startsWith('#try-') ? location.hash.slice(5) : '';
    if (tester.byId.has(id)) openTester(id);
  });
};

start();
