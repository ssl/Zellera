const ESC = { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' };
const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ESC[c]);
class Safe { constructor(s) { this.s = s; } toString() { return this.s; } }
const part = (v) => {
  if (v == null || v === false) return '';
  if (v instanceof Safe) return v.s;
  if (Array.isArray(v)) return v.map(part).join('');
  return esc(v);
};
const html = (strings, ...values) => {
  let out = strings[0];
  for (let i = 0; i < values.length; i++) out += part(values[i]) + strings[i + 1];
  return new Safe(out);
};
const text = (s) => new Safe(esc(s).replace(/`([^`]+)`/g, '<code>$1</code>'));

const STATUS_TEXT = {
  200: 'OK', 204: 'No Content', 301: 'Moved Permanently', 304: 'Not Modified', 400: 'Bad Request', 403: 'Forbidden',
  404: 'Not Found', 429: 'Too Many Requests', 500: 'Internal Server Error', 503: 'Service Unavailable',
};
const statusText = (code) => STATUS_TEXT[code] ?? '';

const slug = (s) => String(s).toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
const groupAnchor = (id) => `group-${slug(id)}`;
const methodPill = (m) => html`<span class="method method-${m.toLowerCase()}">${m}</span>`;
const statusClass = (code) => `status-${String(code)[0]}xx`;

const pathHtml = (path) => new Safe(path.split('/').map((seg, i) => {
  const s = /^\{[^}]+\}$/.test(seg) ? `<span class="path-param">${esc(seg)}</span>` : esc(seg);
  return i === 0 ? s : `/<wbr>${s}`;
}).join(''));
const shortPath = (path) => (path.startsWith('/api/public/') ? path.slice('/api/public'.length) : path);

const pretty = (value) => (typeof value === 'string' ? value : JSON.stringify(value, null, 2));
const codeBlock = (value, label) => html`<pre class="code" tabindex="0" aria-label="${label}"><code>${pretty(value)}</code></pre>`;

const schemaLink = (name) => html`<a href="#schema-${name}">${name}</a>`;

const typeLabel = (s) => {
  if (!s) return html`any`;
  if (s.$ref) return schemaLink(s.$ref);
  const alt = s.oneOf ?? s.anyOf;
  let label;
  if (alt) label = html`${alt.map((b, i) => html`${i ? ' or ' : ''}${typeLabel(b)}`)}`;
  else if (s.allOf) label = html`${s.allOf.map((b, i) => html`${i ? ' + ' : ''}${typeLabel(b)}`)}`;
  else if (s.type === 'array') label = html`array of ${s.items ? (s.items.$ref || s.items.oneOf || s.items.type !== 'object' ? typeLabel(s.items) : 'objects') : 'any'}`;
  else if (s.type === 'object' && s.additionalProperties && typeof s.additionalProperties === 'object') label = html`object (name → ${typeLabel(s.additionalProperties)})`;
  else label = html`${s.type ?? 'any'}${s.format ? ` (${s.format})` : ''}`;
  return s.nullable ? html`${label} or null` : label;
};

const fieldFacts = (s, required) => {
  const facts = [];
  if (required) facts.push(html`<span class="pill pill--bad">required</span>`);
  const t = s?.$ref ? null : s;
  if (t?.enum) facts.push(html`<span class="facts-line">One of ${t.enum.map((v, i) => html`${i ? ', ' : ''}<code>${String(v)}</code>`)}</span>`);
  if (t?.['x-format']) facts.push(html`<span class="facts-line">Layout <code>${t['x-format']}</code></span>`);
  if (t?.pattern) facts.push(html`<span class="facts-line">Pattern <code>${t.pattern}</code></span>`);
  if (t?.minimum !== undefined) facts.push(html`<span class="facts-line">At least <code>${t.minimum}</code></span>`);
  if (t?.default !== undefined) facts.push(html`<span class="facts-line">Default <code>${JSON.stringify(t.default)}</code></span>`);
  if (t?.minItems !== undefined) facts.push(html`<span class="facts-line">At least ${t.minItems} item${t.minItems === 1 ? '' : 's'}</span>`);
  if (t?.maxItems !== undefined) facts.push(html`<span class="facts-line">At most ${t.maxItems} items</span>`);
  if (t?.xml?.attribute) facts.push(html`<span class="facts-line">XML attribute</span>`);
  if (t?.example !== undefined && typeof t.example !== 'object') facts.push(html`<span class="facts-line">Example <code>${JSON.stringify(t.example)}</code></span>`);
  return facts;
};

const fieldRows = (schema, prefix = '', depth = 0) => {
  const rows = [];
  const props = schema.properties ?? {};
  const required = new Set(schema.required ?? []);
  for (const [name, s] of Object.entries(props)) {
    const full = `${prefix}${name}`;
    rows.push(html`<tr>
      <td data-label="Field"><div class="cell"><code class="field-name">${full}</code></div></td>
      <td data-label="Type"><div class="cell">${typeLabel(s)}</div></td>
      <td data-label="Description"><div class="cell">${s.description ? html`<span>${text(s.description)}</span>` : ''}${fieldFacts(s, required.has(name))}</div></td>
    </tr>`);
    if (depth < 3 && !s.$ref) {
      if (s.type === 'object' && s.properties) rows.push(...fieldRows(s, `${full}.`, depth + 1));
      else if (s.type === 'array' && s.items && !s.items.$ref && s.items.properties) rows.push(...fieldRows(s.items, `${full}[].`, depth + 1));
    }
  }
  if (schema.additionalProperties && typeof schema.additionalProperties === 'object') {
    rows.push(html`<tr>
      <td data-label="Field"><div class="cell"><code class="field-name">${prefix}&lt;name&gt;</code></div></td>
      <td data-label="Type"><div class="cell">${typeLabel(schema.additionalProperties)}</div></td>
      <td data-label="Description"><div class="cell">Any key.${fieldFacts(schema.additionalProperties, false)}</div></td>
    </tr>`);
  }
  return rows;
};

const fieldTable = (schema, caption) => html`<div class="table-wrap stacked-wrap">
  <table class="fields stacked">
    <caption class="visually-hidden">${caption}</caption>
    <thead><tr><th scope="col">Field</th><th scope="col">Type</th><th scope="col">Description</th></tr></thead>
    <tbody>${fieldRows(schema)}</tbody>
  </table>
</div>`;

const schemaBody = (s, caption) => {
  if (!s) return '';
  if (s.$ref) return html`<p class="schema-type">Schema: ${schemaLink(s.$ref)}</p>`;
  const out = [];
  const alt = s.oneOf ?? s.anyOf;
  if (alt) {
    out.push(html`<p class="schema-type">One of:</p><ul class="schema-parts">${alt.map((b) => html`<li>${b.$ref ? schemaLink(b.$ref) : html`${typeLabel(b)}${b.properties ? fieldTable(b, caption) : ''}`}</li>`)}</ul>`);
  } else if (s.allOf) {
    out.push(html`<p class="schema-type">All of:</p><ul class="schema-parts">${s.allOf.map((b) => html`<li>${b.$ref ? schemaLink(b.$ref) : html`${b.properties ? fieldTable(b, caption) : typeLabel(b)}`}</li>`)}</ul>`);
  } else if (s.type === 'object' && (s.properties || (s.additionalProperties && typeof s.additionalProperties === 'object'))) {
    out.push(fieldTable(s, caption));
  } else if (s.type === 'array' && s.items && !s.items.$ref && s.items.properties) {
    out.push(html`<p class="schema-type">Array of objects:</p>`, fieldTable(s.items, caption));
  } else {
    out.push(html`<p class="schema-type">Type: ${typeLabel(s)}</p>`);
    const facts = fieldFacts(s, false);
    if (facts.length) out.push(html`<p class="schema-facts">${facts}</p>`);
  }
  return out;
};

const AUTH_LABEL = {
  none: 'None',
  wiredRead: 'Read key (X-Wired-Read-Key)',
  wiredWrite: 'Write key (X-Wired-Write-Key)',
  wiredReadWrite: 'Read and write key (X-Wired-Read-Key and X-Wired-Write-Key)',
};
const AUTH_SHORT = { wiredRead: 'Read key', wiredWrite: 'Write key', wiredReadWrite: 'Read and write key' };

const rateText = (spec, cls) => {
  const r = spec.rateLimits[cls];
  if (!r || typeof r !== 'object') return cls;
  const parts = [`${r.perMinute} per minute`];
  if (r.per10s) parts.push(`${r.per10s} per 10 s`);
  return `${parts.join(', ')}, per ${r.scope === 'room' ? 'room' : 'client address'}`;
};

const paramTable = (e) => html`<div class="table-wrap stacked-wrap">
  <table class="params stacked">
    <caption class="visually-hidden">Parameters of ${e.method} ${e.path}</caption>
    <thead><tr><th scope="col">Name</th><th scope="col">In</th><th scope="col">Type</th><th scope="col">Description</th></tr></thead>
    <tbody>${e.params.map((p) => html`<tr>
      <td data-label="Name"><div class="cell"><code class="field-name">${p.name}</code>${p.required ? html` <span class="pill pill--bad">required</span>` : ''}</div></td>
      <td data-label="In"><div class="cell">${p.in}</div></td>
      <td data-label="Type"><div class="cell">${typeLabel(p.schema)}</div></td>
      <td data-label="Description"><div class="cell">${text(p.description ?? '')}${fieldFacts({ ...p.schema, example: undefined }, false)}${p.example !== undefined ? html`<span class="facts-line">Example <code>${typeof p.example === 'string' ? p.example : JSON.stringify(p.example)}</code></span>` : ''}${p.secret ? html`<span class="facts-line">A key: keep it secret. The tester never stores it.</span>` : ''}${p.route?.match ? html`<span class="facts-line">Other values are not routed (HTML 404).</span>` : ''}</div></td>
    </tr>`)}</tbody>
  </table>
</div>`;

const errorRows = (spec, codes) => html`<ul class="error-list">${codes.map((code) => {
  const err = spec.errors[code];
  return html`<li><a class="mono" href="#err-${code}">${code}</a>${err?.text ? html`: ${text(err.text)}` : ''}</li>`;
})}</ul>`;

const trivialError = (r, name, value) => r.errors?.includes(name) && value && typeof value === 'object'
  && Object.keys(value).length === 1 && value.error === name;

const responseBlock = (spec, e, status, r) => {
  const examples = Object.entries(r.examples ?? {}).filter(([name, value]) => !trivialError(r, name, value));
  return html`<div class="resp resp-${String(status)[0]}xx">
    <h5 class="resp-head"><span class="status ${statusClass(status)}">${status}</span> <span class="resp-text">${statusText(status)}</span></h5>
    <div class="resp-body">
    <p class="resp-desc"><span>${text(r.description ?? '')}</span>${r.contentType ? html` <code class="resp-type">${r.contentType}</code>` : html` <span class="resp-type">No body</span>`}</p>
    ${r.headers ? html`<dl class="resp-headers">${Object.entries(r.headers).map(([name, h]) => html`<div><dt><code>${name}</code></dt><dd>${text(h.description ?? '')}</dd></div>`)}</dl>` : ''}
    ${r.schema ? schemaBody(r.schema, `Fields of the ${status} answer of ${e.method} ${e.path}`) : ''}
    ${r.errors?.length ? html`<p class="resp-sub">Error codes${r.schema?.$ref === 'Error' || r.schema?.$ref === 'WiredError' ? html`, each sent as <code>{"error":"&lt;code&gt;"}</code>` : ''}</p>${errorRows(spec, r.errors)}` : ''}
    ${examples.map(([name, value]) => html`<details class="example">
      <summary>Example${examples.length > 1 || name !== 'default' ? html`: <code>${name}</code>` : ''}</summary>
      ${codeBlock(value, `Example ${name} of the ${status} answer`)}
    </details>`)}
    </div>
  </div>`;
};

const authCell = (spec, id) => {
  if (id === 'none') return AUTH_LABEL.none;
  const k = spec.auth[id];
  const names = k ? (k.names ?? [k.name]) : [];
  return html`<a href="#auth">${AUTH_SHORT[id] ?? id}</a>${names.map((n) => html` <code>${n}</code>`)}`;
};

const endpointCard = (spec, e) => {
  const statuses = Object.keys(e.responses);
  return html`<article class="panel op" id="op-${e.id}" aria-labelledby="op-${e.id}-title" data-op="${e.id}">
  <header class="panel__head op-head">
    <h3 class="op-title" id="op-${e.id}-title">${methodPill(e.method)} <code class="op-path">${pathHtml(e.path)}</code></h3>
    <p class="op-summary">${text(e.summary)}${e.inBaseSpec ? '' : html` <span class="pill pill--warn">Extra of this game</span>`}</p>
    <a class="btn btn--small try-link" href="#tester" data-try="${e.id}" aria-describedby="op-${e.id}-title">Try it</a>
  </header>
  <div class="panel__body op-body">
  ${e.description ? html`<p class="op-desc">${text(e.description)}</p>` : ''}
  <dl class="op-meta">
    <div><dt>Auth</dt><dd>${authCell(spec, e.auth)}</dd></div>
    <div><dt>Rate limit</dt><dd><a href="#rate-limits"><code>${e.rateLimit}</code></a> <span class="op-rate">${rateText(spec, e.rateLimit)}</span></dd></div>
    <div><dt>Id</dt><dd><code>${e.id}</code></dd></div>
  </dl>
  ${e.params?.length ? html`<h4>Parameters</h4>${paramTable(e)}` : ''}
  ${e.requestBody ? html`<h4>Request body</h4>
    <p class="op-bodytype"><span>${e.requestBody.required ? 'Required' : 'Optional'}</span> <code>${e.requestBody.contentType ?? 'application/json'}</code></p>
    ${schemaBody(e.requestBody.schema, `Fields of the request body of ${e.method} ${e.path}`)}
    ${Object.entries(e.requestBody.examples ?? {}).map(([name, value]) => html`<details class="example" open>
      <summary>Example${name !== 'default' ? html`: <code>${name}</code>` : ''}</summary>
      ${codeBlock(value, `Example request body ${name}`)}
    </details>`)}` : ''}
  <h4>Responses</h4>
  <div class="resps">${statuses.map((s) => responseBlock(spec, e, s, e.responses[s]))}</div>
  ${e.notes?.length ? html`<h4>Notes</h4><ul class="notes">${e.notes.map((n) => html`<li>${text(n)}</li>`)}</ul>` : ''}
  ${e.differsFromBaseSpec?.length ? html`<h4>Differences from the Web API 1.0.0 spec</h4><ul class="notes">${e.differsFromBaseSpec.map((n) => html`<li>${text(n)}</li>`)}</ul>` : ''}
  </div>
</article>`;
};

const LABELS = {
  ids: { user: 'User', room: 'Room', group: 'Group' },
  dates: { user: 'Users', roomAndMember: 'Rooms and group members', achievement: 'Achievements', wired: 'Wired Variables' },
  errors: { public: 'Public endpoints', wired: 'Wired Variables', routes: 'Unknown routes' },
  headers: { cors: 'CORS', etag: 'ETag and 304', cache: 'Caching', rateLimit: 'Rate limits' },
};
const CONVENTION_TITLES = { ids: 'Unique ids', targetIds: 'Target ids (Wired Variables)', dates: 'Dates', values: 'Wired values', errors: 'Errors', headers: 'Headers' };

const conventionBlock = (key, value) => {
  const title = CONVENTION_TITLES[key] ?? key;
  if (typeof value === 'string') return html`<section class="conv" aria-labelledby="conv-${key}"><h3 id="conv-${key}">${title}</h3><p>${text(value)}</p></section>`;
  const entries = Object.entries(value).filter(([k]) => k !== 'text');
  const label = (k) => LABELS[key]?.[k] ?? null;
  return html`<section class="conv" aria-labelledby="conv-${key}"><h3 id="conv-${key}">${title}</h3>
    ${value.text ? html`<p>${text(value.text)}</p>` : ''}
    <dl class="defs">${entries.map(([k, v]) => html`<div><dt>${label(k) ?? html`<code>${k}</code>`}</dt><dd>${text(v)}</dd></div>`)}</dl>
  </section>`;
};

const authSection = (spec) => {
  const a = spec.auth;
  const keyRow = (id) => {
    const k = a[id];
    const names = k.names ?? [k.name];
    return html`<div><dt>${AUTH_LABEL[id]}</dt><dd>${text(k.text)} ${names.map((n, i) => html`${i ? ' and ' : 'Header '}<code>${n}</code>`)}.</dd></div>`;
  };
  return html`<section class="panel" id="auth" aria-labelledby="auth-title">
    <div class="panel__head"><h2 class="panel__title" id="auth-title">Keys</h2></div>
    <div class="panel__body">
    <p>${text(a.none.text)} The default and extradata endpoints need nothing. The Wired Variables endpoints need the keys of the room's Web API box, sent as headers:</p>
    <dl class="defs">${['wiredRead', 'wiredWrite', 'wiredReadWrite'].map(keyRow)}</dl>
    <h3>How to get keys</h3>
    <p>${text(a.howToGetKeys)}</p>
    </div>
  </section>`;
};

const rateSection = (spec) => {
  const rows = Object.entries(spec.rateLimits).filter(([, r]) => r && typeof r === 'object');
  return html`<section class="panel" id="rate-limits" aria-labelledby="rate-title">
    <div class="panel__head"><h2 class="panel__title" id="rate-title">Rate limits</h2></div>
    <div class="panel__body">
    <div class="table-wrap stacked-wrap"><table class="stacked">
      <caption class="visually-hidden">Rate limit classes</caption>
      <thead><tr><th scope="col">Class</th><th scope="col">Counted per</th><th scope="col" class="num">Per minute</th><th scope="col" class="num">Per 10 s</th><th scope="col">What</th></tr></thead>
      <tbody>${rows.map(([cls, r]) => html`<tr>
        <td data-label="Class"><div class="cell"><code>${cls}</code>${r.ours ? html` <span class="pill">this game</span>` : ''}</div></td>
        <td data-label="Counted per"><div class="cell">${r.scope === 'room' ? 'room' : 'client address'}</div></td>
        <td data-label="Per minute" class="num"><div class="cell">${r.perMinute ?? '-'}</div></td>
        <td data-label="Per 10 s" class="num"><div class="cell">${r.per10s ?? '-'}</div></td>
        <td data-label="What"><div class="cell">${text(r.text ?? '')}</div></td>
      </tr>`)}</tbody>
    </table></div>
    ${spec.rateLimits.source ? html`<p class="muted">${text(spec.rateLimits.source)}</p>` : ''}
    <p class="muted">${text(spec.conventions.headers.rateLimit ?? '')}</p>
    </div>
  </section>`;
};

const errorsSection = (spec) => html`<section class="panel" id="errors" aria-labelledby="errors-title">
  <div class="panel__head"><h2 class="panel__title" id="errors-title">Error codes</h2></div>
  <div class="panel__body">
  <div class="table-wrap stacked-wrap"><table class="stacked">
    <caption class="visually-hidden">Every error code</caption>
    <thead><tr><th scope="col">Code</th><th scope="col" class="num">Status</th><th scope="col">Body</th><th scope="col">Meaning</th></tr></thead>
    <tbody>${Object.entries(spec.errors).map(([code, err]) => html`<tr id="err-${code}">
      <td data-label="Code"><div class="cell"><code>${code}</code>${err.ours ? html` <span class="pill">this game</span>` : ''}</div></td>
      <td data-label="Status" class="num"><div class="cell">${err.status}</div></td>
      <td data-label="Body"><div class="cell"><code class="nowrap-sm">${JSON.stringify(err.example)}</code></div></td>
      <td data-label="Meaning"><div class="cell">${text(err.text ?? '')}</div></td>
    </tr>`)}</tbody>
  </table></div>
  <h3>Paths without an endpoint</h3>
  <p>${text(spec.htmlNotFound.text)} Status ${spec.htmlNotFound.status}, <code>${spec.htmlNotFound.contentType}</code>.</p>
  </div>
</section>`;

const collectRefs = (value, into) => {
  if (Array.isArray(value)) { value.forEach((v) => collectRefs(v, into)); return into; }
  if (!value || typeof value !== 'object') return into;
  if (typeof value.$ref === 'string') into.add(value.$ref);
  for (const [k, v] of Object.entries(value)) if (k !== 'example' && k !== 'examples') collectRefs(v, into);
  return into;
};

const schemasSection = (spec) => {
  const usedBy = new Map();
  const partOf = new Map();
  for (const e of spec.endpoints) {
    for (const ref of collectRefs([e.requestBody?.schema, ...Object.values(e.responses).map((r) => r.schema)], new Set())) {
      (usedBy.get(ref) ?? usedBy.set(ref, []).get(ref)).push(e);
    }
  }
  for (const [name, s] of Object.entries(spec.schemas)) {
    for (const ref of collectRefs(s, new Set())) if (ref !== name) (partOf.get(ref) ?? partOf.set(ref, []).get(ref)).push(name);
  }
  return html`<section class="panel" id="schemas" aria-labelledby="schemas-title">
    <div class="panel__head"><h2 class="panel__title" id="schemas-title">Schemas</h2></div>
    <div class="panel__body">
    <p class="muted">Every shape the endpoints send or take. Field names, types and enums follow the Web API 1.0.0 spec.</p>
    <ul class="schema-index">${Object.keys(spec.schemas).map((name) => html`<li><a href="#schema-${name}">${name}</a></li>`)}</ul>
    ${Object.entries(spec.schemas).map(([name, s]) => html`<section class="schema" id="schema-${name}" aria-labelledby="schema-${name}-title">
      <h3 id="schema-${name}-title">${name}</h3>
      ${s.description ? html`<p>${text(s.description)}</p>` : ''}
      ${schemaBody(s, `Fields of ${name}`)}
      ${s.example !== undefined ? html`<details class="example"><summary>Example</summary>${codeBlock(s.example, `Example of ${name}`)}</details>` : ''}
      ${usedBy.has(name) ? html`<div class="used-by"><p class="used-by__head">Used by</p><ul class="used-by__list">${usedBy.get(name).map((e) => html`<li><a href="#op-${e.id}">${methodPill(e.method)} <span class="toc-path">${pathHtml(shortPath(e.path))}</span></a> <span class="used-by__what">${text(e.summary)}</span></li>`)}</ul></div>` : ''}
      ${partOf.has(name) ? html`<p class="part-of">Part of ${partOf.get(name).map((n, i) => html`${i ? ', ' : ''}${schemaLink(n)}`)}.</p>` : ''}
    </section>`)}
    </div>
  </section>`;
};

const endpointsByGroup = (spec) => spec.groups.map((g) => ({ group: g, endpoints: spec.endpoints.filter((e) => e.group === g.id) }));

const sideNav = (spec) => html`<nav class="docs-side" aria-label="Endpoints">
  <details class="docs-toc" open>
    <summary>Endpoints</summary>
    <div class="toc-filter field">
      <label for="toc-filter">Filter</label>
      <input id="toc-filter" type="search" autocomplete="off" spellcheck="false" placeholder="users, PATCH, variables…" disabled>
    </div>
    ${endpointsByGroup(spec).map(({ group, endpoints }) => html`<p class="toc-group"><a href="#${groupAnchor(group.id)}">${group.name}</a></p>
    <ul class="toc-ops">${endpoints.map((e) => html`<li data-filter="${`${e.method} ${e.path} ${e.summary} ${e.id}`.toLowerCase()}"><a href="#op-${e.id}">${methodPill(e.method)} <span class="toc-path">${pathHtml(shortPath(e.path))}</span></a></li>`)}</ul>`)}
    <p class="toc-group"><a href="#reference">Reference</a></p>
    <ul class="toc-ref">
      <li><a href="#auth">Keys</a></li>
      <li><a href="#conventions">Conventions</a></li>
      <li><a href="#rate-limits">Rate limits</a></li>
      <li><a href="#errors">Error codes</a></li>
      <li><a href="#schemas">Schemas</a></li>
    </ul>
  </details>
</nav>`;

export const renderPage = (spec) => {
  const game = spec.game;
  const base = `${game.site}${game.basePath}`;
  const count = spec.endpoints.length;
  const specCount = spec.endpoints.filter((e) => e.inBaseSpec).length;
  return html`<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${spec.info.title} · ${game.name}</title>
  <meta name="description" content="${spec.info.description}">
  <link rel="stylesheet" href="/theme/css/site.css">
  <link rel="stylesheet" href="${game.docsPath}docs.css">
  <script src="/theme/js/site.js" defer></script>
  <script type="module" src="${game.docsPath}docs.js"></script>
</head>
<body class="api-docs" data-tab="help" data-sub="api" data-frame="wide">
  <main id="main" class="content docs">
    <section class="panel panel--gold docs-intro" aria-labelledby="docs-title">
      <div class="panel__head"><h1 class="panel__title" id="docs-title">${spec.info.title} <span class="pill">${spec.info.version}</span> <span class="pill">OAS 3.0.0</span></h1></div>
      <div class="panel__body">
        <p class="docs-lede">${text(spec.info.description)}</p>
        <dl class="docs-facts">
          <div class="docs-facts__base"><dt>Base URL</dt><dd><code>${base}</code></dd></div>
          <div><dt>Endpoints</dt><dd>${count} (${specCount} in the Web API 1.0.0 spec)</dd></div>
          <div><dt>OpenAPI document</dt><dd><a href="${game.openapiPath}">${game.openapiPath}</a></dd></div>
          <div><dt>Data of this page</dt><dd><a href="${game.dataPath}">${game.dataPath}</a></dd></div>
        </dl>
        <p class="small muted">${text(spec.source.texts)}</p>
        <nav aria-label="On this page">
          <ul class="docs-jump">
            <li><a href="#endpoints">Endpoints</a></li>
            <li><a href="#tester">Try it</a></li>
            <li><a href="#auth">Keys</a></li>
            <li><a href="#errors">Errors</a></li>
            <li><a href="#schemas">Schemas</a></li>
            <li><a href="${game.openapiPath}">OpenAPI</a></li>
          </ul>
        </nav>
      </div>
    </section>

    <div class="docs-layout">
      ${sideNav(spec)}
      <div class="docs-main">
        <section id="tester" class="panel tester" aria-labelledby="tester-title">
          <div class="panel__head"><h2 class="panel__title" id="tester-title">Try it</h2></div>
          <div class="panel__body">
            <p>Send a real request to this game's API from your browser and see exactly what goes over the wire. Keys you type stay in this page: they are not stored, never put in a URL and never logged, and a copied curl command holds placeholders instead.</p>
            <div id="tester-app" data-data="${game.dataPath}">
              <p class="notice">The tester needs JavaScript. Every endpoint below can also be called with curl or any HTTP client.</p>
            </div>
          </div>
        </section>

        <div id="endpoints" class="docs-groups">
        ${endpointsByGroup(spec).map(({ group, endpoints }) => html`<section class="docs-group" id="${groupAnchor(group.id)}" aria-labelledby="${groupAnchor(group.id)}-title">
          <div class="group-head">
            <h2 id="${groupAnchor(group.id)}-title">${group.name}${group.extra ? html` <span class="pill pill--warn">Extra of this game</span>` : ''}</h2>
            <p>${text(group.text)}</p>
          </div>
          ${endpoints.map((e) => endpointCard(spec, e))}
        </section>`)}
        </div>

        <div id="reference" class="docs-reference">
          ${authSection(spec)}
          <section class="panel" id="conventions" aria-labelledby="conventions-title">
            <div class="panel__head"><h2 class="panel__title" id="conventions-title">Conventions</h2></div>
            <div class="panel__body">
            ${Object.entries(spec.conventions).map(([k, v]) => conventionBlock(k, v))}
            </div>
          </section>
          ${rateSection(spec)}
          ${errorsSection(spec)}
          ${schemasSection(spec)}
        </div>
      </div>
    </div>
  </main>
</body>
</html>
`.s;
};
