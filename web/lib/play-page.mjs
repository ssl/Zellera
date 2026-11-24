import { clientString } from './client-strings.mjs';

export const SCRIPT_SRC = "script-src 'self' 'unsafe-inline' 'unsafe-eval'";

const BRIDGE_PAGES = Object.freeze({ news: '/news', avatars: '/me' });

// The page came from a POST: without this a reload asks to send the form again; now a reload is GET /play.
export const PLAIN_RELOAD = "globalThis.history?.replaceState(null,'','/play');";
const NO_CONTEXT_MENU = "document.addEventListener('contextmenu',e=>{if(!e.target.closest('input,textarea'))e.preventDefault()});";
const NAME = /^[A-Za-z_$][\w$]*$/;
// The client reports through these; at debug level its notes stay out of the console's warnings and errors.
const LOG_HOOKS = ['logError', 'logLoginStep', 'logDisconnection', 'disconnect']
  .map((name) => `${name}(...a){console.debug('[client] ${name}',...a)}`).join(',');

/** The object the client calls to open a site page, under the names its profile gives; empty without them. */
export const bridgeScript = () => {
  const object = clientString('client.bridgeObject'), open = clientString('client.bridgeOpenPage');
  if (!NAME.test(object ?? '') || !NAME.test(open ?? '')) return '';
  return `globalThis.${object}={available:true,${open}(n){const to=${JSON.stringify(BRIDGE_PAGES)}[n];`
    + `if(typeof to==='string')window.open(to,'gameMain','noopener');return true},${LOG_HOOKS}};`;
};

export const renderPlayPage = (indexHtml, { base, session }) => {
  const inject = `<script>globalThis.__GAME_SESSION__=${JSON.stringify(session).replace(/</g, '\\u003c')};${PLAIN_RELOAD}${NO_CONTEXT_MENU}${bridgeScript()}</script>`;
  const out = indexHtml
    .replaceAll('href="./', `href="${base}`)
    .replaceAll('src="./', `src="${base}`)
    .replace('<script type="module"', `${inject}\n  <script type="module"`)
    .replace(`${SCRIPT_SRC};`, `${SCRIPT_SRC} blob:;`);
  if (out === indexHtml || !out.includes(inject)) throw new Error('index.html anchors not found');
  return out;
};
