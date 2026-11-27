import { DbRefused } from '../cms/lib/db.mjs';
import { COMMAND_GRANT } from '../cms/lib/commands.mjs';
import { sql } from '../cms/lib/sql.mjs';
import { YOUTUBE_KEY_SET } from '../cms/config.mjs';
import { HK_API } from './index.mjs';
import { audit } from './_guard.mjs';

export const owner = 'hk-content';

const PERM = 'housekeeping.youtube';
export const grants = [
  { table: 'youtube_playlists', select: ['position', 'playlist_code'], insert: ['position', 'playlist_code'], delete: true },
  COMMAND_GRANT,
];

const BASE = `${HK_API}/youtube`;
const EDIT_LIMIT = ['hk-youtube', 120, 3600];
const CODE = /^[A-Za-z0-9_-]{2,64}$/;

/** A bare playlist code, or the list= parameter of a YouTube address. */
export const playlistCode = (input) => {
  const text = String(input ?? '').trim();
  if (CODE.test(text)) return text;
  let url;
  try { url = new URL(text); } catch { return null; }
  if (!/(^|\.)youtube\.com$|(^|\.)youtu\.be$/.test(url.hostname)) return null;
  const list = url.searchParams.get('list') ?? '';
  return CODE.test(list) ? list : null;
};

const playlists = async (ctx) => (await ctx.db.query(sql`SELECT playlist_code FROM youtube_playlists ORDER BY position`))
  .map((r) => String(r.playlist_code));

const overview = async (ctx) => ({ ok: true, keySet: YOUTUBE_KEY_SET, playlists: await playlists(ctx) });

const listPage = async (req, res, ctx) => ctx.send.json(res, 200, await overview(ctx));

const rewrite = (ctx, codes) => ctx.db.transaction(async (tx) => {
  await tx.remove(sql`DELETE FROM youtube_playlists WHERE position >= 0`);
  for (const [i, code] of codes.entries()) {
    await tx.write(sql`INSERT INTO youtube_playlists (position, playlist_code) VALUES (${i}, ${code})`);
  }
});

const reload = async (ctx) => {
  try {
    await ctx.command('reloadTvPlaylists', {}, { userId: ctx.staff.id });
  } catch (e) {
    ctx.log(`reloadTvPlaylists not queued: ${e?.message ?? e}`);
  }
};

const change = (how) => async (req, res, ctx) => {
  if (!ctx.rateLimit(res, ...EDIT_LIMIT)) return;
  const codes = await playlists(ctx);
  const next = how(ctx, codes);
  if (typeof next === 'string') return ctx.send.error(res, 400, 'Not saved', next, { field: 'playlist' });
  try {
    await rewrite(ctx, next.codes);
  } catch (e) {
    if (e instanceof DbRefused) return ctx.send.error(res, 503, 'Not saved', 'This server can not save changes right now.');
    throw e;
  }
  await audit(ctx, { action: `hk.youtube.${next.action}`, targetType: 'playlist', targetId: 0, label: next.code, detail: '' });
  await reload(ctx);
  return ctx.send.json(res, 200, await overview(ctx));
};

const add = change((ctx, codes) => {
  const code = playlistCode(ctx.form.get('playlist'));
  if (!code) return 'Give a YouTube playlist address with list= in it, or the playlist code itself.';
  if (codes.includes(code)) return 'That playlist is already in the list.';
  return { action: 'add', code, codes: [...codes, code] };
});

const remove = change((ctx, codes) => {
  const code = String(ctx.form.get('playlist') ?? '');
  if (!codes.includes(code)) return 'That playlist is not in the list.';
  return { action: 'remove', code, codes: codes.filter((c) => c !== code) };
});

const move = change((ctx, codes) => {
  const code = String(ctx.form.get('playlist') ?? '');
  const direction = ctx.form.get('direction');
  const i = codes.indexOf(code);
  if (i < 0) return 'That playlist is not in the list.';
  if (direction !== 'up' && direction !== 'down') return 'Move it up or down.';
  const j = direction === 'up' ? i - 1 : i + 1;
  const next = [...codes];
  if (j >= 0 && j < next.length) [next[i], next[j]] = [next[j], next[i]];
  return { action: 'move', code, codes: next };
});

export const routes = [
  { method: 'GET', path: BASE, perm: PERM, handler: listPage },
  { method: 'POST', path: `${BASE}/add`, perm: PERM, handler: add },
  { method: 'POST', path: `${BASE}/remove`, perm: PERM, handler: remove },
  { method: 'POST', path: `${BASE}/move`, perm: PERM, handler: move },
];
