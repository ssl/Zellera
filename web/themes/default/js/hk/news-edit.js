'use strict';

HK.start((main) => {
  const id = HK.param(2);
  const isNew = id === 'new';
  return HK.load(main, isNew ? '/news/new' : `/news/${encodeURIComponent(id)}`, (r) => {
    const { h, panel, field, urls } = HK;
    const a = r.article ?? { title: '', body: '', link_text: '', link_url: '', shown: true };
    const { limits } = r;
    const title = isNew ? 'New article' : `Edit: ${a.title}`;
    HK.setTitle(title);
    const shown = h('input', { type: 'checkbox', name: 'shown', id: 'hn-shown', checked: a.shown });
    const size = h('p', { class: 'hint', 'aria-live': 'polite' });
    const bytesOf = (s) => new TextEncoder().encode(s.replace(/\r\n?/g, '\n').trim()).length;
    const count = (text) => {
      const n = bytesOf(text);
      size.textContent = `${n} of ${limits.bodyBytes} bytes${n > limits.bodyBytes ? ': too long' : ''}`;
    };
    const body = field({
      id: 'hn-body', name: 'body', label: 'Text', type: 'textarea', required: true, rows: 14, value: a.body,
      hint: 'Plain text; an empty line starts a new paragraph. Letters outside a to z count as two to four bytes.',
      oninput: (e) => count(e.currentTarget.value),
    });
    const form = HK.jsonForm(h('form', { class: 'hv-form form-stack' },
      field({ id: 'hn-title', name: 'title', label: 'Title', required: true, maxlength: limits.title, value: a.title, autocomplete: 'off' }),
      body, size,
      field({
        id: 'hn-link', name: 'link_url', label: 'Link (optional)', maxlength: limits.link, value: a.link_url, placeholder: '/help', autocomplete: 'off', spellcheck: 'false',
        hint: 'A page of this site: a path that starts with /.',
      }),
      field({ id: 'hn-link-text', name: 'link_text', label: 'Link text', maxlength: limits.linkText, value: a.link_text, placeholder: 'Read more', autocomplete: 'off' }),
      h('div', { class: 'field' }, h('label', { for: 'hn-shown' }, shown, ' Shown on the site'),
        h('p', { class: 'hint' }, a.published ? 'Hiding and showing it again keeps its date.' : 'It gets its date when it is shown for the first time.')),
      h('p', { class: 'row' }, h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Saving…' }, isNew ? 'Add the article' : 'Save'),
        h('a', { class: 'btn', href: urls.news() }, 'Back to the list'))), isNew ? '/news' : `/news/${encodeURIComponent(id)}`, {
      body: (f) => ({ title: f.elements.title.value, body: f.elements.body.value, link_url: f.elements.link_url.value, link_text: f.elements.link_text.value, shown: shown.checked }),
      onDone: () => location.assign(urls.news()),
    });
    count(a.body);
    main.append(HK.head({ id: 'article-title', crumbs: [['News', urls.news()], [isNew ? 'New article' : a.title]], title, lead: 'A title, a text and, if you like, one link to a page of this site.' }),
      panel({ title: 'Article', body: form }));
  });
});
