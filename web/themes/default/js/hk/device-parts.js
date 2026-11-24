'use strict';

(() => {
  const { h } = HK;
  const COMBO = 'gpu + scr + fnt';
  const why = (value) => (/^0+$/.test(value) ? 'not given by the browser' : 'not trusted');
  const piece = (p) => (p.usable ? h('span', { class: 'mono' }, p.value)
    : [h('span', { class: 'mono piece--unusable' }, p.value), ' ', h('span', { class: 'muted' }, `(${why(p.value)})`)]);
  const piecesGrid = (device) => HK.factsGrid(device.pieces.map((p) => [p.name, piece(p)]), { raw: true });
  const pattern = (pieces) => h('ul', { class: 'pattern', 'aria-label': 'Pattern' },
    Object.entries(pieces).filter(([, value]) => value != null).map(([name, value]) => h('li', null, `${name}=${value}`)));
  const count = (n, one = 'user', many = 'users') => `${HK.fmt.number(n)} ${n === 1 ? one : many}`;

  window.HKDevice = { COMBO, piece, piecesGrid, pattern, count };
})();
