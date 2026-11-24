'use strict';

Site.start((main) => {
  const [, , name = '', , id = ''] = location.pathname.split('/');
  const asked = decodeURIComponent(name);
  return Site.load(main, `/profiles/${encodeURIComponent(asked)}/photos/${encodeURIComponent(id)}`, (r) => {
    const { h, panel, cols, facts, fmt, urls, notice } = Site;
    if (r.user.name !== asked) history.replaceState(null, '', urls.photo(r.user.name, r.photo.id));
    Site.setTitle(`Photo by ${r.user.name}`);
    const picture = panel({ id: 'photo', title: ['Photo by ', Site.profileLink(r.user.name)], level: 1, body: r.photo.image
      ? h('img', { class: 'photo-full', src: r.photo.image, alt: `Photo by ${r.user.name}`, width: 320, height: 320, decoding: 'async' })
      : notice('The image of this photo is not available.', 'warn') });
    const about = panel({ id: 'photo-facts', title: 'About this photo', body: [
      facts([
        ['Published', fmt.date(r.photo.published)],
        r.room ? ['Taken in', Site.roomLink(r.room)] : null,
      ]),
      h('p', { class: 'actions' }, Site.info.sees?.players === false
        ? h('a', { class: 'btn', href: urls.photos() }, 'All photos')
        : h('a', { class: 'btn', href: urls.profile(r.user.name) }, `Back to ${r.user.name}'s profile`))] });
    main.append(cols('halves', picture, about));
  });
});
