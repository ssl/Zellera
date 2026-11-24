import { accountRulesOf } from '../lib/site-access.mjs';
import { html, urls } from '../cms/lib/html.mjs';
import { SITE_API } from './index.mjs';

export const owner = 'help';
export const grants = [];

const sections = (ctx, accounts) => [
  ['account', 'Your account', html`
      <ul>
        <li>Only log in on ${ctx.GAME_NAME}'s own site, at <span class="mono">${ctx.SITE}</span>. A page that looks
          like it but has another address is a fake.</li>
        <li>Never give your password to anyone. Staff will never ask for it.</li>
        <li>Use a password you don't use anywhere else. ${accounts.email === 'off' ? 'There is no email on your account'
          : 'The site never sends mail'}, so a lost password can't be reset by email.</li>
        <li>Log out on computers other people use.</li>
      </ul>`],
  ['private-life', 'Your private life', html`
      <ul>
        <li>You never really know who you are talking to. Keep your real name, address, school, phone number,
          social media and photos of yourself to yourself.</li>
        <li>Don't meet up with people you only know from the game. If someone asks, say no and tell an adult
          you trust.</li>
        <li>Think before you share a photo: once it is out, you can't take it back.</li>
      </ul>`],
  ['scams', 'Scams', html`
      <ul>
        <li>Nothing on ${ctx.GAME_NAME} is sold for money. Anyone who offers ${ctx.NAMES.credits}, furni or Club for money,
          or for your password, is scamming you.</li>
        <li>Sites that promise free ${ctx.NAMES.credits} or ask you to log in "to claim a prize" are after your password.</li>
        <li>Never download files people send you a link to.</li>
        <li>When you trade, only trust what is in the trade window (<a href="${urls.help('trading')}">trading</a>).</li>
      </ul>`],
  ['something-wrong', 'If something is wrong', html`
      <p>Report a player or a room from the <b>Help</b> window in the game, and ignore anyone who bothers you
        (<a href="${urls.help('reporting')}">reporting and blocking</a>). If someone may be in danger in real life,
        tell a parent or another adult you trust, or call your local emergency number.</p>`],
  ['safety-lock', 'The safety lock', html`
      <p>Staff can put a safety lock on an account that may be in the wrong hands, for example when someone else
        may know its password. While it is locked, the game shows a notice and some things are switched off:
        buying and selling, buying and publishing photos, and taking rights away in rooms. Everything you own
        stays where it is.</p>
      <p>There is no form to lift the lock yourself. Talk to a member of staff in the game: they check that the
        account is really yours and remove the lock.</p>`],
];

export const doc = (parts) => ({
  sections: parts.map(([id, title]) => ({ id, title: String(title) })),
  bodyHtml: html`${parts.map(([id, title, content]) => html`
      <h2 id="${id}">${title}</h2>${content}`)}`.value,
});

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/safety`,
    handler: async (req, res, ctx) => ctx.send.json(res, 200, {
      ok: true,
      title: 'Staying safe',
      lede: 'How to keep your account safe, keep your private life private, and what the safety lock is.',
      ...doc(sections(ctx, await accountRulesOf(ctx.access))),
    }),
  },
];
