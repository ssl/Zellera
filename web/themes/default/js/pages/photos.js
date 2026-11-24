'use strict';

Site.start((main) => {
  const query = new URLSearchParams(location.search);
  const go = (page) => {
    const q = new URLSearchParams(location.search);
    if (page > 1) q.set('page', String(page)); else q.delete('page');
    location.assign(`${location.pathname}${q.size ? `?${q}` : ''}`);
  };
  const call = new URLSearchParams();
  for (const k of ['user', 'page']) if (query.get(k)) call.set(k, query.get(k));
  return Site.load(main, `/photos${call.size ? `?${call}` : ''}`, (r) => {
    const { h, panel, cols, empty, fmt, time, urls, pager } = Site;
    if (r.user && r.user.name !== query.get('user')) {
      query.set('user', r.user.name);
      history.replaceState(null, '', `${location.pathname}?${query}`);
    }
    const title = r.user ? `Photos by ${r.user.name}` : 'Photos';
    Site.setTitle(title);
    const count = `${fmt.number(r.total)} ${r.total === 1 ? 'photo' : 'photos'}`;
    const intro = panel({ id: 'camera', title: 'The camera', tone: 'gold', body: r.user?.private
      ? [h('p', null, 'This profile is private.'), h('p', null, h('a', { href: urls.photos() }, "Everyone's photos"))]
      : [h('p', null, r.user ? `${count} published from the game's camera.` : `${count} published from the game's camera, newest first.`),
        r.user ? h('ul', { class: 'list' }, Site.info.sees?.players === false ? '' : h('li', null, h('a', { href: urls.profile(r.user.name) }, `${r.user.name}'s profile`)), h('li', null, h('a', { href: urls.photos() }, "Everyone's photos"))) : '',
        h('p', { class: 'small muted' }, `Take a photo with the camera in the ${Site.place()} and publish it to show it here.`)] });
    const tiles = r.user?.private ? '' : panel({ id: 'photos', title: [r.user ? ['Photos by ', Site.profileLink(r.user.name)] : 'Photos'], level: 1, body: r.photos.length
      ? [h('ul', { class: 'photo-grid' }, r.photos.map((p) => h('li', { dataset: { photoId: p.id } },
        p.thumb ? h('a', { href: urls.photo(p.owner, p.id) }, h('img', { class: 'thumb', src: p.thumb, alt: `Photo by ${p.owner}`, width: 100, height: 100, loading: 'lazy', decoding: 'async' }))
          : h('span', { class: 'thumb', role: 'img', 'aria-label': `Photo by ${p.owner} (image not available)` }),
        r.user ? h('a', { href: urls.photo(p.owner, p.id) }, 'Photo') : Site.profileLink(p.owner),
        p.room ? h('span', null, 'In ', Site.roomLink(p.room)) : '',
        time(p.published)))),
        pager({ page: r.page, hasNext: r.hasNext, go })]
      : empty(r.user ? `${r.user.name} has not published a photo yet.` : `Nobody has published a photo yet. Take one with the camera in the ${Site.place()} and publish it.`) });
    main.append(cols('', tiles || intro, tiles ? intro : ''));
  });
});
