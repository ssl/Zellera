export const SITE_API = '/api/site/v1';

export const SITE_MODULES = Object.freeze([
  ['site-api/site.mjs', 'me'],
  ['site-api/me.mjs', 'me'],
  ['site-api/privacy.mjs', 'me'],
  ['site-api/staff.mjs', 'community'],
  ['site-api/profile.mjs', 'profile'],
  ['site-api/photos.mjs', 'photos'],
  ['site-api/groups.mjs', 'community'],
  ['site-api/rooms.mjs', 'community'],
  ['site-api/news.mjs', 'community'],
  ['site-api/help.mjs', 'help'],
  ['site-api/help-requests.mjs', 'help'],
  ['site-api/safety.mjs', 'help'],
  ['site-api/legal.mjs', 'help'],
  ['site-api/static.mjs', 'help'],
  ['site-api/redirects.mjs', 'routing'],
  ['site-api/clientlog.mjs', 'routing'],
]);
