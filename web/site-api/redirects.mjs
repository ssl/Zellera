import { urls } from '../cms/lib/html.mjs';

export const owner = 'routing';

const to = (location) => (req, res, ctx) => ctx.send.redirect(res, location);

export const routes = [
  { method: 'GET', path: '/shop', handler: to(urls.help('credits')) },
  { method: 'GET', path: '/shop/*rest', handler: to(urls.help('credits')) },
  { method: 'GET', path: '/credits/club', handler: to(urls.help('club')) },
  { method: 'GET', path: '/credits', handler: to(urls.help('credits')) },
  { method: 'GET', path: '/credits/*rest', handler: to(urls.help('credits')) },
  { method: 'GET', path: '/identity/useOrCreateAvatar', handler: to(urls.safety()) },
  { method: 'GET', path: '/account/disconnected', handler: to(urls.me()) },
];
