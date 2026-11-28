import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { gameName } from '../cms/game-name.mjs';

export const DEVICE_SHAPE = /^1-[0-9a-f]{16}(-[0-9a-f]{8}){9}$/;
export const DEVICE_UNSTABLE = `1-${'f'.repeat(16)}${'-ffffffff'.repeat(9)}`;

const MEASURE = readFileSync(fileURLToPath(new URL('./device-measure.js', import.meta.url)), 'utf8');
if (/<\/script/i.test(MEASURE)) throw new Error('device-measure.js would close its own script tag');
const WORK = readFileSync(fileURLToPath(new URL('./work.js', import.meta.url)), 'utf8');
if (/<\/script/i.test(WORK)) throw new Error('work.js would close its own script tag');

const escaped = (text) => String(text).replace(/[&<>"]/g, (c) => `&#${c.charCodeAt(0)};`);

export const renderDevicePage = (ticket, work = null, title = gameName()) => {
  const nonce = randomBytes(16).toString('base64');
  const sso = JSON.stringify(String(ticket)).replace(/</g, '\\u003c');
  const task = JSON.stringify(work ? { challenge: String(work.challenge), bits: Number(work.bits) } : null).replace(/</g, '\\u003c');
  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<meta name="generator" content="Zellera">
<title>${escaped(title)}</title>
<link rel="icon" href="data:,">
</head>
<body bgcolor="#000000" text="#ffffff">
<noscript>The game needs JavaScript.</noscript>
<form id="play" method="post" action="/play" hidden><input type="hidden" name="sso"><input type="hidden" name="device"><input type="hidden" name="pow"><input type="hidden" name="env"></form>
<script nonce="${nonce}">
${MEASURE}
${WORK}
const zelleraEnv = () => [
  ['w', () => navigator.webdriver === true],
  ['h', () => navigator.userAgent.includes('HeadlessChrome')],
  ['g', () => {
    const gl = document.createElement('canvas').getContext('webgl');
    const info = gl.getExtension('WEBGL_debug_renderer_info');
    return /SwiftShader|llvmpipe|Software/.test(gl.getParameter(info ? info.UNMASKED_RENDERER_WEBGL : gl.RENDERER));
  }],
  ['s', () => window.outerWidth === 0 || window.outerHeight === 0],
  ['l', () => navigator.languages.length === 0],
  ['p', () => navigator.userAgent.includes('Chrome/') && !/Mobile|Android/.test(navigator.userAgent) && navigator.plugins.length === 0],
].filter(([, seen]) => { try { return seen() === true; } catch { return false; } }).map(([flag]) => flag).join('');
globalThis.history?.replaceState(null, '', '/play');
(async () => {
  const form = document.getElementById('play');
  let device;
  let limit;
  try {
    const late = new Promise((done) => { limit = setTimeout(done, 5000, ''); });
    device = await Promise.race([zelleraSeal().then((r) => r.device), late]);
  } catch {
    device = '';
  }
  clearTimeout(limit);
  const task = ${task};
  if (task) form.elements.pow.value = task.challenge + ':' + await zelleraWork(task.challenge, task.bits);
  form.elements.env.value = zelleraEnv();
  form.elements.sso.value = ${sso};
  form.elements.device.value = ${DEVICE_SHAPE}.test(device) ? device : '${DEVICE_UNSTABLE}';
  form.submit();
})();
</script>
</body>
</html>
`;
  return {
    html,
    headers: {
      'cache-control': 'no-store', 'referrer-policy': 'no-referrer', 'x-content-type-options': 'nosniff', 'x-frame-options': 'SAMEORIGIN',
      'content-security-policy': `default-src 'none'; script-src 'nonce-${nonce}'; img-src data:; form-action 'self'; base-uri 'none'; frame-ancestors 'self'`,
    },
  };
};
