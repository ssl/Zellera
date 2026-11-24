'use strict';

HK.start((main) => HK.load(main, '/youtube', (first) => {
  const { h, panel, table, pill, alertBox } = HK;
  HK.setTitle('YouTube');
  const problem = alertBox();
  const box = h('div');

  const act = async (btn, path, body) => {
    if (btn.disabled) return;
    const restore = HK.busy(btn);
    problem.textContent = '';
    try {
      const r = await HK.post(path, body);
      if (r.ok) { draw(r.data); return; }
      problem.textContent = r.data.error || 'Something went wrong. Try again.';
    } catch {
      problem.textContent = HK.NETWORK_ERROR;
    }
    restore();
  };

  const keyNote = (set) => h('p', null, set ? pill('Key set', 'on') : pill('No key'), ' ',
    h('span', { class: 'muted' }, set
      ? 'The YouTube key comes from the config (ZEL_YOUTUBE_API_KEY).'
      : 'Set ZEL_YOUTUBE_API_KEY in the config and restart; until then the TVs stay empty.'));

  const listPanel = (codes) => {
    const input = h('input', { type: 'text', autocomplete: 'off', spellcheck: 'false', placeholder: 'https://www.youtube.com/playlist?list=…', 'aria-label': 'Playlist to add' });
    const btn = (label, path, body, disabled) => h('button', {
      class: 'btn btn--small', type: 'button', disabled, onclick: (e) => act(e.currentTarget, `/youtube/${path}`, body),
    }, label);
    return panel({ title: 'Playlists', body: [
      table({
        caption: 'Playlists of the TVs', main: 1, head: ['#', 'Playlist', 'Order', ''],
        cls: ['num', 'grow mono-cell', 'nowrap', 'nowrap'],
        emptyText: 'No playlists: the TVs show nothing.',
        rows: codes.map((code, i) => [
          String(i + 1),
          h('a', { href: `https://www.youtube.com/playlist?list=${encodeURIComponent(code)}`, target: '_blank', rel: 'noopener noreferrer' }, code),
          h('span', { class: 'row row--tight' },
            btn('Up', 'move', { playlist: code, direction: 'up' }, i === 0),
            btn('Down', 'move', { playlist: code, direction: 'down' }, i === codes.length - 1)),
          btn('Remove', 'remove', { playlist: code }, false),
        ]),
      }),
      h('p', { class: 'row' }, input,
        h('button', { class: 'btn btn--go', type: 'button', onclick: (e) => act(e.currentTarget, '/youtube/add', { playlist: input.value }) }, 'Add')),
    ] });
  };

  const draw = (r) => {
    HK.clear(box).append(keyNote(r.keySet), listPanel(r.playlists));
  };

  main.append(
    HK.head({
      id: 'youtube-title', title: 'YouTube',
      lead: 'The playlists every TV furni offers, in this order. Paste a YouTube address with list= in it or the playlist code. Changes reach the game within a few seconds.',
    }),
    problem, box);
  draw(first);
}));
