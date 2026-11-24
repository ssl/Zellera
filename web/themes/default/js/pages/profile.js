'use strict';

const asked = decodeURIComponent(location.pathname.split('/')[2] ?? '');
const isOwn = (info) => Boolean(info.viewer) && info.viewer.name.toLowerCase() === asked.trim().toLowerCase();

Site.start((main) => Site.load(main, `/profiles/${encodeURIComponent(asked)}`, (r) => {
  const { h, panel, cols, avatar, pill, empty, fmt, time, urls } = Site;
  const u = r.user;
  if (u.name !== asked) history.replaceState(null, '', `${urls.profile(u.name)}${location.hash}`);
  Site.setTitle(u.name);

  const stat = (label, value) => h('div', { class: 'me-stat' }, h('dt', null, label), h('dd', null, value));
  const card = h('section', { class: `me-card${Site.avatarsOn() ? '' : ' me-card--plain'}`, id: 'about', 'aria-labelledby': 'profile-title' },
    h('div', { class: 'me-card__scene' },
      h('div', { class: 'stage stage--l me-card__stage' }, avatar(u, { full: true, size: 'l' })),
      h('div', { class: 'me-card__text' },
        h('div', { class: 'profile-name' }, h('h1', { id: 'profile-title' }, u.name), u.online ? pill('Online now', 'on') : ''),
        u.motto ? h('p', { class: 'bubble' }, u.motto) : h('p', { class: 'bubble bubble--quiet' }, 'No motto yet.'))),
    h('dl', { class: 'me-stats', 'aria-label': `About ${u.name}` },
      stat('Member since', time(u.memberSince) || '-'),
      !u.online && u.lastOnline ? stat('Last seen', time(u.lastOnline, fmt.ago(u.lastOnline))) : null,
      stat('Friends', fmt.number(r.friends))));

  const friends = panel({ id: 'friends', title: r.friends ? `Friends (${fmt.number(r.friends)})` : 'Friends', body: [
    r.friendList.length === 0 ? empty(r.friends ? 'Friends are not shown.' : 'No friends yet.')
      : h('ul', { class: 'people' }, r.friendList.map((f) => h('li', null, h('a', { href: urls.profile(f.name) }, avatar(f),
        h('span', null, f.name), f.online ? pill('Online', 'on') : '')))),
    r.friendList.length > 0 && r.friends > r.friendList.length
      ? h('p', { class: 'small muted' }, `${fmt.number(r.friends - r.friendList.length)} more.`) : ''] });
  const groups = panel({ id: 'groups', title: 'Groups', body: r.groups.length
    ? h('ul', { class: 'list' }, r.groups.map((g) => h('li', { class: 'person' }, Site.initials(g.name),
      h('div', { class: 'person__text' }, h('a', { href: urls.group(g.id) }, h('b', null, g.name)), h('span', { class: 'list__sub' }, g.role)))))
    : empty('Not in any group.') });
  const DOOR = { doorbell: 'Doorbell', password: 'Password' };
  const rooms = panel({ id: 'rooms', title: r.rooms.length ? `Rooms (${fmt.number(r.rooms.length)})` : 'Rooms', body: r.rooms.length
    ? h('ul', { class: 'list' }, r.rooms.map((room) => h('li', { class: 'room-row' },
      h('a', { href: urls.room(room.id) }, h('b', null, room.name)),
      h('span', { class: 'room-row__count' }, room.users > 0 ? `${fmt.number(room.users)} of ${fmt.number(room.max)} inside` : 'Empty'),
      DOOR[room.door] ? pill(DOOR[room.door]) : '')))
    : empty('No rooms yet.') });
  const photos = panel({ id: 'photos', title: 'Photos', body: r.photos.length
    ? [h('ul', { class: 'photo-grid' }, r.photos.map((p) => h('li', null,
      h('a', { href: urls.photo(u.name, p.id), tabindex: '-1', 'aria-hidden': 'true' },
        p.thumb ? h('img', { class: 'thumb', src: p.thumb, alt: '', width: 100, height: 100, loading: 'lazy', decoding: 'async' }) : h('span', { class: 'thumb' })),
      h('a', { href: urls.photo(u.name, p.id) }, 'Photo'), time(p.published)))),
      r.morePhotos ? h('p', null, h('a', { href: `${urls.photos()}?user=${encodeURIComponent(u.name)}` }, `All photos by ${u.name}`)) : '']
    : empty('No photos published yet.') });

  main.append(
    r.private ? h('p', { class: 'notice notice--warn', role: 'status', id: 'private' },
      r.own ? 'Your profile is private: only you and staff can see it. ' : 'This profile is private: you see it because you are staff.',
      r.own ? h('a', { href: `${urls.settings()}#privacy` }, 'Change it in Settings') : '') : '',
    h('div', { class: 'me-top' }, card, photos),
    cols('three', rooms, friends, groups),
    ...Site.slot('profile', { user: u, own: r.own, hidden: false }));
  if (location.hash) document.getElementById(location.hash.slice(1))?.scrollIntoView();
}), { tabOf: (info) => (isOwn(info) ? { tab: 'home', sub: 'profile' } : null) });
