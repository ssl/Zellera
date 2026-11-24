import { accountRulesOf } from '../lib/site-access.mjs';
import { html, urls } from '../cms/lib/html.mjs';
import { SITE_API } from './index.mjs';
import { doc } from './safety.mjs';

export const owner = 'help';
export const grants = [];

const UPDATED = '26 October 2026';

const terms = (ctx) => [
  ['what', `What ${ctx.GAME_NAME} is`, html`
  <p>${ctx.GAME_NAME} is a free online game run by its own staff.</p>`],

  ['free', 'Free, with no money involved', html`
  <ul>
    <li>Playing is free. Nothing in the game is sold for real money.</li>
    <li>${ctx.NAMES.Credits}, ${ctx.NAMES.points}, ${ctx.NAMES.premium} and furni have no value in money and can't be exchanged for money.</li>
    <li>Selling or buying accounts, currency or furni for money is not allowed.</li>
  </ul>`],

  ['account', 'Your account', html`
  <ul>
    <li>One account belongs to one person. Keep your password to yourself: you are responsible for what is done
      with your account.</li>
    <li>The number of accounts one internet connection can make may be limited.</li>
    <li>Pick a name that follows the rules below.</li>
  </ul>`],

  ['rules', 'The rules', html`
  <p>Everyone follows <a href="${urls.help('rules')}">the house rules</a>. Staff can warn, mute, trade lock,
    kick or ban players who break it, and remove rooms, photos, group badges or texts that break it
    (<a href="${urls.help('bans-and-sanctions')}">bans and sanctions</a>).</p>`],

  ['what-you-make', 'What you make', html`
  <p>Rooms, photos you publish, groups and what you say in public can be seen by other players. Only share what
    you are allowed to share and what is OK for everyone to see.</p>`],

  ['no-guarantees', 'No guarantees', html`
  <ul>
    <li>${ctx.GAME_NAME} is offered as it is. It can have bugs, be offline, or change at any time.</li>
    <li>If a bug hands out currency or furni, staff may take it back.</li>
  </ul>`],

  ['changes', 'Changes and questions', html`
  <p>When these terms change, this page changes. Questions go to a member of staff in the game. What we store
    about you is on the <a href="${urls.privacy()}">privacy page</a>.</p>`],
];

const privacy = (ctx, accounts) => [
  ['account', 'Your account', html`
  <ul>
    <li>Your username and your password. The password is stored only as a scrypt hash, so nobody can read it
      back, staff included.</li>
    <li>${accounts.email === 'off' ? 'No email address, real name or birthday: we never ask for them.'
      : `No real name or birthday: we never ask for them. The email address you give at sign-up${accounts.email === 'optional'
        ? ' (if you give one)' : ''} is kept with your account to log in with; the site never sends mail.`}
      A question to staff from the help page without an account keeps the email address you give with that
      question, so staff can answer it.</li>
    <li>What you do in the game: your look, motto, friends, rooms, furni, badges, currencies and settings.</li>
  </ul>`],

  ['connection', 'Connection details', html`
  <ul>
    <li>Your IP address when you sign up, and again each time you open the game (your account keeps only the
      latest one).</li>
    <li>A device id: the game client makes a random id, keeps it in your browser and sends it when you log in.</li>
    <li>Moderators use these to stop banned players from coming back and to limit accounts per connection.</li>
    <li>The web server's access log: IP address, the address asked for, the time and the browser. Older entries
      are overwritten as the log grows. Cookies and login tickets are left out.</li>
  </ul>`],

  ['logs', 'Logs', html`
  <ul>
    <li>Chat: what you say in rooms, your whispers and your private messages to friends.</li>
    <li>Which rooms you enter and leave, and when.</li>
    <li>Questions you send to staff from the help page, their answers, and the IP address each question came
      from.</li>
    <li>Trades, Shop purchases, changes to your ${ctx.NAMES.credits}, ${ctx.NAMES.points} and ${ctx.NAMES.premium}, and name changes.</li>
    <li>Error reports: when the game crashes, the client sends the error, its version and its start-up
      settings. Your login ticket is removed before anything is kept.</li>
  </ul>
  <p>Logs are kept as long as the game runs; nothing deletes them automatically.</p>`],

  ['who-sees', 'Who can see what', html`
  <ul>
    <li><b>Everyone:</b> your public profile (name, look, motto, groups, rooms and the photos you
      publish) and what you say in rooms.</li>
    <li><b>Staff:</b> moderators can read chat logs, room visits and trades when they handle reports, and
      staff with ban rights can see IP addresses and device ids. The staff website records each time someone
      opens IP addresses or chat logs there.</li>
    <li>Nobody else: nothing is sold or passed on.</li>
  </ul>`],

  ['cookies', 'Cookies and browser storage', html`
  <ul>
    <li>One cookie keeps you logged in on this site for up to 7 days. Scripts can't read it, and logging out
      removes it.</li>
    <li>The game client keeps a few things in your browser's storage: its device id, its login preferences
      and its graphics setting.</li>
  </ul>`],

  ['not-done', 'What we don\'t do', html`
  <p>No ads, no analytics, no tracking scripts and no outside services on these pages.</p>`],

  ['questions', 'Questions or removal', html`
  <p>To ask what is stored about you, or to have your account removed, talk to a member of staff in the game.
    The rules for using the game are in the <a href="${urls.terms()}">terms</a>.</p>`],
];

const legal = (title, lede, parts) => async (req, res, ctx) => ctx.send.json(res, 200, {
  ok: true, title, lede: lede(ctx), updated: UPDATED, ...doc(parts(ctx, await accountRulesOf(ctx.access))),
});

export const routes = [
  {
    method: 'GET', path: `${SITE_API}/legal/terms`,
    handler: legal('Terms of use', (ctx) => `The rules for using ${ctx.GAME_NAME}. They are short, so please read them.`, terms),
  },
  {
    method: 'GET', path: `${SITE_API}/legal/privacy`,
    handler: legal('Privacy', (ctx) => `What ${ctx.GAME_NAME} stores about you, who can see it, and what it doesn't do.`, privacy),
  },
];
