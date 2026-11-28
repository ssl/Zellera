export const HK_API = '/api/hk/v1';

export const HK_MODULES = Object.freeze([
  ['hk-api/staff.mjs', 'hk-users'],
  ['hk-api/dashboard.mjs', 'hk-users'],
  ['hk-api/users.mjs', 'hk-users'],
  ['hk-api/rooms.mjs', 'hk-rooms'],
  ['hk-api/tickets.mjs', 'hk-rooms'],
  ['hk-api/help-requests.mjs', 'hk-help'],
  ['hk-api/chatlogs.mjs', 'hk-actions'],
  ['hk-api/actions.mjs', 'hk-actions'],
  ['hk-api/actions-user.mjs', 'hk-actions'],
  ['hk-api/audit.mjs', 'hk-actions'],
  ['hk-api/suspicions.mjs', 'hk-actions'],
  ['hk-api/devices.mjs', 'hk-devices'],
  ['hk-api/device-bans.mjs', 'hk-devices'],
  ['hk-api/landingview.mjs', 'hk-content'],
  ['hk-api/news.mjs', 'hk-content'],
  ['hk-api/youtube.mjs', 'hk-content'],
]);
