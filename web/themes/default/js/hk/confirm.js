'use strict';

HK.start((main) => {
  const { h, panel } = HK;
  HK.setTitle('Open this page?');
  HK.clear(main).append(h('div', { class: 'confirm' }, panel({ title: 'Open this page?', tone: 'red', body: [
    h('p', null, 'You came here from another site. This page shows private data, and opening it writes a row with your name to the audit log.'),
    h('p', { class: 'row' }, h('a', { class: 'btn btn--go', href: `${location.pathname}${location.search}` }, 'Open it'), h('a', { class: 'btn', href: HK.urls.hk() }, 'Back to the dashboard'))] })));
});
