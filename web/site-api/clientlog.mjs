import { originOk, sameSiteFetch } from '../cms/lib/http.mjs';

export const owner = 'routing';

const FIELDS = Object.freeze([
  'crash_time', 'is_fatal', 'error_ctx', 'flash_version', 'avg_update', 'error_desc', 'error_cat', 'error_data',
  'debug', 'start_time', 'agent', 'system', 'in_room', 'last_room', 'click_target', 'click_time',
  'mouse_up_target', 'mouse_up_time', 'host', 'port', 'rece_msg_data', 'rece_msg_time', 'sent_msg_data',
  'sent_msg_time',
]);
const SECRET_KEY = /ticket|sso|token|password|auth/i;
const SECRET_VALUE = /zel\.[a-z0-9]{6,}|(?:ticket|sso|token|password)["']?\s*[:=]\s*["']?[^\s"',;&]+/gi;
const MAX_BODY = 16 * 1024;
const MAX_VALUE = 512;
const MAX_LINE = 4096;
const RATE = Object.freeze([10, 60]);

export const cleanReport = (body) => {
  const out = {};
  for (const key of FIELDS) {
    if (!Object.hasOwn(body, key) || SECRET_KEY.test(key)) continue;
    const v = body[key];
    if (v == null || typeof v === 'object') continue;
    out[key] = String(v).replace(SECRET_VALUE, '[removed]').slice(0, MAX_VALUE);
  }
  return out;
};

/** One line of printable ASCII, whatever the report holds: nothing in it can start a second log line or move the cursor. */
export const reportLine = (report) => JSON.stringify(report)
  .replace(/[^\x20-\x7e]/g, (c) => `\\u${c.charCodeAt(0).toString(16).padStart(4, '0')}`).slice(0, MAX_LINE);

export const routes = [
  {
    method: 'POST',
    path: '/api/log/crash',
    handler: async (req, res, ctx) => {
      if (!ctx.rateLimit(res, 'crashlog', ...RATE)) return;
      if (!originOk(req, ctx.SITE, ctx.PROD) || !sameSiteFetch(req)) {
        return ctx.send.json(res, 403, { ok: false, error: 'cross-site request refused' });
      }
      const body = await ctx.readJson(MAX_BODY);
      if (!body) return ctx.send.json(res, 400, { ok: false, error: 'JSON object only, at most 16 KB' });
      ctx.log(`client-error ${reportLine(cleanReport(body))}`);
      res.writeHead(204, { 'cache-control': 'no-store' });
      res.end();
    },
  },
];
