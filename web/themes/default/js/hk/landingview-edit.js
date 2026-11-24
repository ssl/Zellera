'use strict';

HK.start((main) => {
  const id = HK.param(2);
  const isNew = id === 'new';
  return HK.load(main, isNew ? '/landing-view/new' : `/landing-view/${encodeURIComponent(id)}`, (r) => {
    const { h, panel, field, urls } = HK;
    const a = r.article ?? { title: '', text: '', image: '', button_type: 'catalog', button_text: '', button_link: '' };
    const { limits } = r.form;
    const title = isNew ? 'New article' : `Edit: ${a.title}`;
    HK.setTitle(title);
    const form = HK.jsonForm(h('form', { class: 'hv-form form-split' }, h('div', { class: 'form-stack' },
      field({ id: 'hv-title', name: 'title', label: 'Title', required: true, maxlength: limits.title, value: a.title, autocomplete: 'off' }),
      field({ id: 'hv-text', name: 'text', label: 'Text', type: 'textarea', required: true, maxlength: limits.text, rows: 4, value: a.text }),
      field({
        id: 'hv-image', name: 'image', label: 'Image (optional)', maxlength: limits.image, value: a.image, placeholder: r.form.imageHint, autocomplete: 'off', spellcheck: 'false',
        hint: 'Shown when the article is 1st or 3rd (on the left); the 2nd and 4th are text only, as the client draws them. A path on this site that starts with /.',
      })),
    h('div', { class: 'form-stack' },
      h('fieldset', { class: 'field' }, h('legend', null, 'Button'),
        r.form.actions.map((x) => h('label', null, h('input', { type: 'radio', name: 'button_type', value: x.type, checked: x.type === (a.button_type || 'catalog') }), ' ', x.label))),
      field({ id: 'hv-button-text', name: 'button_text', label: 'Button text', maxlength: limits.button, value: a.button_text, autocomplete: 'off' }),
      field({ id: 'hv-link', name: 'button_link', label: 'Where it goes', maxlength: limits.link, value: a.button_link, list: 'hv-links', autocomplete: 'off', spellcheck: 'false' }),
      h('datalist', { id: 'hv-links' },
        r.form.clientLinks.map((l) => h('option', { value: l }, 'Client link')),
        r.form.catalogPages.map((p) => h('option', { value: p.code }, p.caption))),
      h('ul', { class: 'hint' }, r.form.actions.filter((x) => x.hint).map((x) => h('li', null, h('b', null, `${x.label}:`), ` ${x.hint}`)))),
      h('p', { class: 'row' }, h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Saving…' }, isNew ? 'Add the article' : 'Save'),
        h('a', { class: 'btn', href: urls.landingView() }, 'Back to the list'))), isNew ? '/landing-view' : `/landing-view/${encodeURIComponent(id)}`, {
      onDone: () => location.assign(urls.landingView()),
    });
    main.append(HK.head({ id: 'article-title', crumbs: [['Landing view', urls.landingView()], [isNew ? 'New article' : a.title]], title, lead: 'A title, a text, an image and one button, as the game draws them.' }),
      panel({ title: 'Article', body: form }));
  });
});
