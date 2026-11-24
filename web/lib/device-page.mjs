import { randomBytes } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export const DEVICE_SHAPE = /^1-[0-9a-f]{16}(-[0-9a-f]{8}){9}$/;
export const DEVICE_UNSTABLE = `1-${'f'.repeat(16)}${'-ffffffff'.repeat(9)}`;

const MEASURE = readFileSync(fileURLToPath(new URL('./device-measure.js', import.meta.url)), 'utf8');
if (/<\/script/i.test(MEASURE)) throw new Error('device-measure.js would close its own script tag');

export const renderDevicePage = (ticket) => {
  const nonce = randomBytes(16).toString('base64');
  const sso = JSON.stringify(String(ticket)).replace(/</g, '\\u003c');
  const html = `<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex">
<title>Loading</title>
<link rel="icon" href="data:,">
</head>
<body bgcolor="#000000" text="#ffffff">
<noscript>The game needs JavaScript.</noscript>
<form id="play" method="post" action="/play" hidden><input type="hidden" name="sso"><input type="hidden" name="device"></form>
<script nonce="${nonce}">
${MEASURE}
globalThis.history?.replaceState(null, '', '/play');
(async () => {
  const form = document.getElementById('play');
  let device;
  let limit;
  try {
    const late = new Promise((done) => { limit = setTimeout(done, 5000, ''); });
    device = await Promise.race([measureDevice().then((r) => r.device), late]);
  } catch {
    device = '';
  }
  clearTimeout(limit);
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
