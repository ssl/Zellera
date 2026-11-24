'use strict';

HK.start((main) => HK.load(main, `/help-requests/${HK.param(2)}`, function render(r) {
  const { h, panel, cols, facts, pill, field, jsonForm, fmt, urls, clear } = HK;
  const q = r.request;
  HK.setTitle(`Help request ${q.id}`);
  const name = (id) => r.users[id]?.name ?? `#${id}`;
  const redraw = (data) => { clear(main); render(data); };

  const act = (action, label, kind = '') => {
    const form = h('form', { class: 'inline' }, h('button', { class: `btn${kind ? ` btn--${kind}` : ''}`, type: 'submit', 'data-busy': '…' }, label));
    return jsonForm(form, `/help-requests/${q.id}`, { body: () => ({ action }), onDone: redraw });
  };
  const actions = q.state.key === 'open' ? [act('pick', 'Pick it'), act('close', 'Close without answer')]
    : q.state.key === 'picked' ? [act('release', 'Give it back'), act('close', 'Close without answer')]
      : [act('reopen', 'Open it again')];

  const answerForm = q.state.key === 'closed' ? '' : jsonForm(h('form', { id: 'answer-form', class: 'form-stack' },
    field({ id: 'answer', name: 'answer', label: q.playerId > 0 ? 'Answer (the player reads it on the help page)' : 'Answer (kept here; mail it to the visitor yourself)', type: 'textarea', maxlength: r.limits.answer, required: true, rows: 6 }),
    h('div', { class: 'actions row' },
      h('button', { class: 'btn btn--go', type: 'submit', name: 'close', value: '1', 'data-busy': 'Saving…' }, 'Answer and close'),
      h('button', { class: 'btn', type: 'submit', name: 'keep', value: '1', 'data-busy': 'Saving…' }, 'Answer'))),
  `/help-requests/${q.id}`, { body: (f, e) => ({ action: 'answer', answer: f.elements.answer.value, close: e?.submitter?.name !== 'keep' }), onDone: redraw });

  const statePill = pill(q.state.label, q.state.key === 'open' ? 'warn' : q.state.key === 'closed' ? '' : 'on');
  main.append(
    HK.head({
      id: 'help-title',
      crumbs: [['Help requests', urls.helpRequests(q.state.key === 'open' ? undefined : q.state.key)], [`#${q.id}`]],
      title: `Help request #${q.id}`,
      meta: [statePill, h('span', null, q.topic.label), h('span', null, `sent ${fmt.ago(q.created)}`)],
      actions,
    }),
    cols('',
      [
        panel({ id: 'help-message', title: q.topic.label, body: h('p', { class: 'message' }, q.message) }),
        q.answers.length ? panel({ id: 'help-answers', title: q.answers.length > 1 ? `Answers (${fmt.number(q.answers.length)})` : 'Answer',
          body: h('ol', { class: 'list' }, q.answers.map((a) => h('li', null, h('p', { class: 'chat-line' }, a.message),
            h('p', { class: 'foot-note' }, `By ${a.staffId > 0 ? name(a.staffId) : 'staff'}, ${fmt.dateTime(a.created)}`)))) }) : '',
        answerForm ? panel({ id: 'help-reply', title: 'Answer it', body: answerForm }) : '',
      ],
      [
        panel({ id: 'help-facts', title: 'Request', body: facts([
          ['From', q.playerId > 0 ? h('a', { href: urls.user(q.playerId) }, name(q.playerId)) : [q.guestName || 'A visitor', ' ', pill('no account')]],
          q.playerId > 0 ? null : ['E-mail', q.contact ? h('a', { href: `mailto:${q.contact}` }, q.contact) : '-'],
          ['Sent', fmt.dateTime(q.created)],
          ['Address', h('span', { class: 'mono' }, q.ip || '-')],
          ['Staff', q.staffId > 0 ? h('a', { href: urls.user(q.staffId) }, name(q.staffId)) : 'Nobody yet'],
          q.closed ? ['Closed', fmt.dateTime(q.closed)] : null,
        ]) }),
      ]));
}));
