'use strict';

Site.start((main) => Site.load(main, '/help-requests', function render(r) {
  const { h, panel, cols, field, jsonForm, notice, empty, time, fmt, urls } = Site;
  Site.setTitle('Ask staff');
  const inGame = `In the ${Site.place()}, the Help window reports a player or a room to a moderator straight away.`;

  const ask = () => {
    if (r.reason === 'off') {
      return panel({ id: 'ask', title: 'Ask staff', level: 1, body: [
        h('p', null, 'Questions to staff cannot be sent from the website.'), h('p', { class: 'muted' }, inGame),
        h('p', { class: 'actions' }, h('a', { class: 'btn', href: urls.help() }, 'Browse the help topics'))] });
    }
    if (r.reason === 'login') {
      return panel({ id: 'ask', title: 'Ask staff', level: 1, body: [
        h('p', null, 'Log in to send a question to staff. Their answer shows on this page.'),
        h('p', { class: 'actions' }, h('a', { class: 'btn btn--go', href: urls.home() }, 'Log in'), h('a', { class: 'btn', href: '/#register' }, 'Create an account'))] });
    }
    const [min, max] = r.limits.message;
    const sent = notice('Sent. Staff read every question; their answer shows here.', 'ok');
    sent.hidden = true;
    const topic = h('div', { class: 'field' }, h('label', { for: 'ask-topic' }, 'What is it about?'),
      h('select', { id: 'ask-topic', name: 'topic', required: true, 'aria-describedby': 'ask-topic-error' },
        h('option', { value: '' }, 'Pick one'), r.topics.map((t) => h('option', { value: t.key }, t.label))),
      h('p', { class: 'field-error', id: 'ask-topic-error' }));
    const form = jsonForm(h('form', { id: 'ask-form', novalidate: true },
      topic,
      field({ id: 'ask-message', name: 'message', label: 'Your question', type: 'textarea', hint: `${min} to ${fmt.number(max)} characters. Never write your password here.`, required: true, maxlength: max, rows: 7 }),
      r.guest ? [
        field({ id: 'ask-name', name: 'name', label: 'Your name in the game, if you have one', autocomplete: 'nickname', maxlength: r.limits.name }),
        field({ id: 'ask-contact', name: 'contact', label: 'Your e-mail address', type: 'email', hint: 'Staff answer you there. It is only for this question.', autocomplete: 'email', required: true, maxlength: r.limits.contact }),
      ] : '',
      h('button', { class: 'btn btn--go', type: 'submit', 'data-busy': 'Sending…' }, 'Send to staff')), '/help-requests', {
      check: (f) => (!f.elements.topic.value ? ['topic', 'Pick what your question is about.']
        : f.elements.message.value.trim().length < min ? ['message', `Tell staff a little more: at least ${min} characters.`] : null),
      onDone: async () => {
        if (!r.guest) {
          const again = await Site.api('/help-requests');
          if (again.ok) { Site.clear(main); render(again.data); main.querySelector('#ask-sent')?.removeAttribute('hidden'); return; }
        }
        form.reset();
        sent.hidden = false;
      },
    });
    sent.id = 'ask-sent';
    const waiting = r.mine.filter((m) => m.state !== 'closed').length;
    return panel({ id: 'ask', title: 'Ask staff', level: 1, body: [sent,
      h('p', null, `A question about your account, something that does not work, or anything else? Staff answer it ${r.guest ? 'by e-mail' : 'here'}.`),
      waiting >= r.limits.open
        ? notice(`You have ${waiting} questions waiting for staff. Send a new one when one of them is answered.`, 'warn')
        : form] });
  };

  const STATE = { open: ['Waiting', 'warn'], picked: ['Staff are on it', 'on'], closed: ['Closed', ''] };
  const topicLabel = (key) => r.topics.find((t) => t.key === key)?.label ?? key;
  const mine = r.guest ? null : panel({ id: 'mine', title: r.mine.length ? `Your questions (${fmt.number(r.mine.length)})` : 'Your questions', body: r.mine.length === 0
    ? empty('No questions yet. What you send shows here, with the answers.')
    : h('ul', { class: 'list asks' }, r.mine.map((m) => h('li', { class: 'ask' },
      h('div', { class: 'ask__head' }, h('b', null, topicLabel(m.topic)), m.answers.length ? Site.pill('Answered', 'on') : Site.pill(...STATE[m.state])),
      h('p', { class: 'list__sub' }, 'Sent ', time(m.created, fmt.dateTime(m.created))),
      h('p', { class: 'ask__text' }, m.message),
      m.answers.map((a) => h('div', { class: 'ask__answer' }, h('p', { class: 'ask__by' }, 'Staff answered ', time(a.created, fmt.dateTime(a.created))), h('p', { class: 'ask__text' }, a.message)))))) });

  const side = panel({ id: 'ask-else', title: 'Before you ask', body: h('ul', { class: 'list' },
    h('li', null, h('a', { href: urls.help() }, 'Help topics'), h('span', { class: 'list__sub' }, 'Short answers to the usual questions')),
    h('li', null, h('a', { href: urls.help('reporting') }, 'Reporting a player'), h('span', { class: 'list__sub' }, inGame)),
    h('li', null, h('a', { href: urls.safety() }, 'Staying safe'), h('span', { class: 'list__sub' }, 'Your account and your personal details'))) });
  main.append(cols('', [ask(), mine], [side]));
}));
