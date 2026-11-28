import { crossSite, readForm } from '../cms/lib/http.mjs';
import { DEVICE_SHAPE, renderDevicePage } from './device-page.mjs';
import { sendText as send } from './files.mjs';
import { ipLimitKey } from './limits.mjs';

export const PLAY_FORM_MAX = 1024;
export const PLAY_LIMIT = Object.freeze([60, 60]);
export const FRAME_HEADERS = Object.freeze({
  'content-security-policy': "frame-ancestors 'self'", 'x-frame-options': 'SAMEORIGIN', 'x-content-type-options': 'nosniff',
});
const ENV_FLAGS = Object.freeze({ w: 'webdriver', h: 'headless', g: 'software-gl', s: 'zero-size', l: 'no-languages', p: 'no-plugins' });
export const envFlags = (value) => (/^[a-z]{1,12}$/.test(value ?? '') ? Object.keys(ENV_FLAGS).filter((flag) => value.includes(flag)) : []);
export const headlessNote = (flags) => (flags.length ? {
  weight: flags.includes('w') || flags.includes('h') ? 8 : 1, detail: flags.map((flag) => ENV_FLAGS[flag]).join(' '),
} : null);
const PRIVATE = { 'cache-control': 'no-store', 'referrer-policy': 'no-referrer', ...FRAME_HEADERS };

export const createPlayRoute = ({ cms, playPage, limiter = null, clientIp = () => '' }) => async (req, res, url) => {
  const hit = limiter?.hit(`play:${ipLimitKey(clientIp(req))}`, ...PLAY_LIMIT);
  if (hit && !hit.ok) {
    return send(res, 429, 'Too many requests. Try again soon.', 'text/plain; charset=utf-8', { 'retry-after': String(hit.retryAfter), ...PRIVATE });
  }
  const form = req.method === 'POST' ? await readForm(req, PLAY_FORM_MAX) : null;
  const asked = crossSite(req) ? null : (req.method === 'POST' ? form?.get('sso') : url.searchParams.get('sso'));
  const ticket = (asked ?? (req.method === 'GET' ? await cms.freshPlayTicket?.(req) : null)) ?? '';
  if (!ticket || !(await cms.checkPlayTicket(ticket))) return send(res, 303, '', 'text/plain', { location: '/me', ...PRIVATE });
  const device = form?.get('device') ?? '';
  if (!DEVICE_SHAPE.test(device)) {
    const page = renderDevicePage(ticket, await cms.playWork?.());
    return send(res, 200, page.html, 'text/html; charset=utf-8', page.headers);
  }
  if (cms.checkPlayProof && !(await cms.checkPlayProof(req, ticket, form?.get('pow') ?? ''))) {
    return send(res, 303, '', 'text/plain', { location: '/me', ...PRIVATE });
  }
  const note = headlessNote(envFlags(form?.get('env')));
  if (note) cms.noteHeadless?.(req, ticket, note);
  await cms.bindPlayDevice(ticket, device);
  return send(res, 200, await playPage(ticket), 'text/html; charset=utf-8', PRIVATE);
};
