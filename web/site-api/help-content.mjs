import { gameRules } from '../lib/game-rules.mjs';
import { html, urls } from '../cms/lib/html.mjs';

const PRODUCT = 'Zellera';
const a = (href, text) => html`<a href="${href}">${text}</a>`;
const cmd = (text) => html`<code class="mono">${text}</code>`;

export const TOPICS = Object.freeze([
  {
    slug: 'getting-started',
    title: 'Getting started',
    summary: 'Make an account, open the game and find your way around.',
    related: ['credits', 'rooms', 'rules'],
    body: (ctx, accounts) => html`
      <h2>Your account</h2>
      <p>Pick a name and a password on the ${a(urls.home(), 'front page')}. Names are ${gameRules()['player.name.min_length']} to ${gameRules()['player.name.max_length']} characters: letters,
        numbers and <span class="mono">${[...gameRules()['player.name.punctuation']].join(' ')}</span></p>
      <p>${{
        off: `${ctx.GAME_NAME} never asks for an email address.`,
        optional: 'An email address at sign-up is up to you.',
        required: 'You log in with your email address.',
      }[accounts.email]} The site never sends mail, so nobody can send you a reset link: keep your password somewhere
        safe.</p>
      <h2>Opening the game</h2>
      <p>After you log in you land on ${a(urls.me(), 'My page')}. Press <b>Open the game</b> and the game opens in a
        new tab.</p>
      <h2>What you start with</h2>
      <ul>
        <li>{names.Points} to spend in the Shop, where most offers cost 1 {names.point}.</li>
        <li>A {names.buildPass} membership for 10 years, so you can furnish rooms straight away
          (${a(urls.help('build-pass'), '{names.buildPass}')}).</li>
        <li>Every piece of clothing in the avatar editor, Club sets included.</li>
      </ul>
      <h2>Finding your way</h2>
      <ul>
        <li><b>Navigator:</b> find rooms, events and your own rooms, or create a new one.</li>
        <li><b>Shop and {names.buildPass}:</b> the two catalogs. See ${a(urls.help('credits'), '{names.credits} and currencies')}.</li>
        <li><b>Inventory:</b> your furni, pets, bots and badges.</li>
        <li><b>Earnings:</b> rewards wait here until you claim them, including a small gift every day.</li>
      </ul>`,
  },
  {
    slug: 'rules',
    title: 'The house rules',
    summary: 'The few rules that keep the game fun for everyone.',
    related: ['reporting', 'bans-and-sanctions', 'trading'],
    body: (ctx) => html`
      <p>These are the rules of ${ctx.GAME_NAME}. They are short:</p>
      <ul>
        <li><b>Be kind.</b> No bullying, threats, hate or harassment, and no sexual chat or content.</li>
        <li><b>Keep personal details private.</b> Don't share your real name, address, school, phone number,
          social media or photos of yourself, and don't ask other people for theirs.</li>
        <li><b>Don't scam.</b> No fake trades, no "trust trades", no fake giveaways, no lying about what something is worth.</li>
        <li><b>Your account is yours alone.</b> Never share your password and never use someone else's account.</li>
        <li><b>Don't pretend to be staff.</b></li>
        <li><b>Play fair.</b> No cheats, no scripts that play for you, and no using bugs on purpose. Found a bug?
          Tell a member of staff.</li>
        <li><b>Don't flood or spam,</b> and don't advertise other sites or games.</li>
      </ul>
      <p>Room owners can add their own rules for their rooms, as long as they fit these.</p>
      <h2>When rules are broken</h2>
      <p>${a(urls.help('reporting'), 'Report it')} and a moderator will look at it. Staff can warn, mute,
        trade lock or ban players who break the rules (${a(urls.help('bans-and-sanctions'), 'bans and sanctions')}).</p>`,
  },
  {
    slug: 'credits',
    title: '{names.Credits} and currencies',
    summary: '{names.Credits}, {names.points} and {names.premium} are free here. How you get them and what they are for.',
    related: ['club', 'build-pass', 'trading'],
    body: (ctx) => html`
      <p><b>Nothing on ${ctx.GAME_NAME} is sold for real money.</b> There is no shop where you pay: {names.credits}, {names.points} and
        {names.premium} can't be bought, only earned in the game. Anyone who offers to sell you currency or furni for
        money is trying to scam you.</p>
      <h2>The three currencies</h2>
      <ul>
        <li><b>{names.Points}</b> pay for almost everything in the Shop. Most offers cost 1 {names.point}.</li>
        <li><b>{names.Credits}</b> pay for Club, photos, {names.credit} furni and a few special offers, and they are what
          players use on the Marketplace.</li>
        <li><b>{names.Premium}</b> come from reward tracks and pay for a few special items.</li>
      </ul>
      <h2>How you get them</h2>
      <ul>
        <li>New accounts start with {names.points} to spend in the Shop.</li>
        <li>The <b>Earnings</b> window pays a small {names.point} gift every day, plus what you earn from achievements.</li>
        <li><b>Quests</b> and <b>daily tasks</b> pay {names.points}.</li>
        <li><b>Reward tracks</b> pay {names.points} and {names.premium} as you level up.</li>
        <li>Selling furni on the <b>Marketplace</b> pays {names.credits}, which arrive in your Earnings window.</li>
        <li><b>{names.Credit} furni</b> (coins, bags and bars) turn back into {names.credits} when you redeem them.</li>
        <li>Club members get <b>Club payday</b> once a month: a part of the {names.credits} they spent in the Shop
          comes back (${a(urls.help('club'), 'Club')}).</li>
      </ul>
      <h2>Free without currency</h2>
      <ul>
        <li>New accounts get {names.buildPass}, so most furni can be placed without spending anything
          (${a(urls.help('build-pass'), '{names.buildPass}')}).</li>
        <li>All clothes are free to wear, Club sets included.</li>
        <li>Creating a group costs nothing (you need Club).</li>
      </ul>`,
  },
  {
    slug: 'club',
    title: 'Club',
    summary: 'What Club gives you and how to join.',
    related: ['credits', 'groups', 'build-pass'],
    body: () => html`
      <p>Club is in the Shop, on its own page. A month (31 days) costs 1 {names.credit}.</p>
      <h2>What members get</h2>
      <ul>
        <li>Up to ${gameRules()['player.max_friends_club']} friends instead of ${gameRules()['player.max_friends']}.</li>
        <li>You can create groups (${a(urls.help('groups'), 'groups')}).</li>
        <li>Club payday: once a month you get back 10 % of the {names.credits} you spent in the Shop ({names.credit} furni don't
          count), plus a bonus that grows the longer you stay a member. It arrives in your Earnings window.</li>
      </ul>
      <p>Club clothes are free for everyone here, members or not.</p>`,
  },
  {
    slug: 'build-pass',
    title: '{names.buildPass}',
    summary: 'Place furni from the {names.buildPass} catalog without buying it.',
    related: ['rooms', 'credits', 'getting-started'],
    body: () => html`
      <p>Every new account gets a {names.buildPass} membership for 10 years. With it you place furni straight from the
        <b>{names.buildPass}</b> catalog into your rooms, without buying them.</p>
      <h2>The furni limit</h2>
      <p>You can have up to 1000 {names.buildPass} furni placed at once. Every {names.buildPass} month you buy in the Shop
        (<b>Memberships</b>, 1 {names.credit}) adds 250, and the <b>{names.buildPass} Upgrades</b> page adds 45, 250 or 600 more
        (1, 3 or 6 {names.credits}), as often as you like.</p>
      <h2>Good to know</h2>
      <ul>
        <li>{names.buildPass} furni belong to the {names.buildPass}, not to you: they never go into your inventory, so
          you can't trade or sell them.</li>
        <li>Picking them up sends them back to the catalog. ${cmd(':pickallbc')} picks up every {names.buildPass} furni in your room.</li>
        <li>Their info box says "{names.buildPass}" instead of an owner.</li>
      </ul>`,
  },
  {
    slug: 'trading',
    title: 'Trading',
    summary: 'How to trade safely, and what a trade lock is.',
    related: ['credits', 'rules', 'bans-and-sanctions'],
    body: () => html`
      <h2>Starting a trade</h2>
      <p>Click another player and choose <b>Trade</b>. The room owner decides whether trading is allowed in a
        room: not at all, only for the owner and people with rights, or for everyone.</p>
      <ul>
        <li>Put your items in, check what the other player offers, then both accept. Before the trade goes
          through you both confirm once more.</li>
        <li>{names.Credit} furni you trade turn into {names.credits} in the other player's purse.</li>
        <li>{names.buildPass} furni can't be traded.</li>
      </ul>
      <h2>Stay safe</h2>
      <ul>
        <li>Only trust what is in the trade window. Never hand something over first and "trust" the other player
          to pay later.</li>
        <li>If the other player puts nothing in, think twice.</li>
        <li>Every trade is logged, so staff can check what happened if you report a scam.</li>
      </ul>
      <h2>Trade locks</h2>
      <p>Moderators can lock a player's trading for a while, or for good, after scams or other trouble. While you
        are locked you can't trade. If you think a lock is a mistake, talk to a member of staff in the game.</p>
      <h2>Selling instead</h2>
      <p>The <b>Marketplace</b> in the Shop lets you sell furni for {names.credits} to anyone, without a trade window.</p>`,
  },
  {
    slug: 'reporting',
    title: 'Reporting a problem',
    summary: 'Report a player, a room or a photo, and block someone who bothers you.',
    related: ['rules', 'bans-and-sanctions', 'trading'],
    body: () => html`
      <h2>Reporting</h2>
      <ul>
        <li>Click the player and choose <b>Report</b>, or open the <b>Help</b> window in the game and choose what
          you want to report: a player, a room, or something else.</li>
        <li>Pick the topic that fits best and add a short note. The chat around it is sent along, so you don't
          need to copy it.</li>
        <li>Photos can be reported from the photo itself.</li>
      </ul>
      <p>A moderator picks up the report and decides what to do. Reports are for real problems: reporting someone
        on purpose for nothing is against the rules.</p>
      <h2>Blocking someone</h2>
      <ul>
        <li>Click the player and choose <b>Ignore</b>, or type ${cmd(':ignore name')}. ${cmd(':unignore name')} undoes it.</li>
        <li>Remove them from your friend list.</li>
        <li>In your own room you can kick, mute or ban people, and choose who may enter.</li>
      </ul>
      <h2>If someone is in danger</h2>
      <p>If someone may be in danger in real life, tell a parent or another adult you trust, or call your local
        emergency number. Staff in the game can't help with that.</p>`,
  },
  {
    slug: 'bans-and-sanctions',
    title: 'Bans and sanctions',
    summary: 'Warnings, mutes, trade locks and bans: what they mean and how long they last.',
    related: ['rules', 'reporting', 'trading'],
    body: () => html`
      <h2>What moderators can do</h2>
      <ul>
        <li><b>Warning:</b> a message telling you what went wrong.</li>
        <li><b>Mute:</b> you can't chat for a while.</li>
        <li><b>Trade lock:</b> you can't trade for a while (${a(urls.help('trading'), 'trading')}).</li>
        <li><b>Kick:</b> you are taken out of the room or the game.</li>
        <li><b>Ban:</b> you can't log in until it ends. A ban can also cover a connection or a device.</li>
      </ul>
      <h2>Each sanction is one step up</h2>
      <p>When a moderator sanctions you, it is one step higher than the last one: a warning, then a 1-hour mute,
        an 18-hour ban, a 7-day ban, a 30-day ban, and finally a permanent ban.</p>
      <p>After each step there is a probation time: 30 days, or 60 days after the longer bans. Stay out of trouble
        until it ends and the next sanction starts from the bottom again.</p>
      <h2>If you think it was a mistake</h2>
      <p>There is no appeal form. Talk to a member of staff in the game, or ask a friend to pass a message on if
        you can't log in.</p>`,
  },
  {
    slug: 'rooms',
    title: 'Rooms',
    summary: 'Create a room, set who may enter, and give friends rights.',
    related: ['build-pass', 'groups', 'camera'],
    body: () => html`
      <h2>Creating a room</h2>
      <p>Open the Navigator and press <b>Create room</b>. Pick a layout, a name and a category. You can change the
        floor plan later with the floor plan editor (${cmd(':floor')}). A room holds up to 4000 furni.</p>
      <h2>Room settings</h2>
      <ul>
        <li><b>Door:</b> open to everyone, doorbell, password, or invisible in the Navigator.</li>
        <li><b>Rights:</b> click a player and choose <b>Give rights</b> so they can move furni.</li>
        <li><b>Trading:</b> not allowed, owner and rights only, or everyone.</li>
        <li><b>Chat:</b> bubble style, scroll speed, flood protection and how far chat carries.</li>
        <li><b>Moderation:</b> who may mute, kick and ban, and the lists of banned and muted players.</li>
      </ul>
      <h2>Sharing a room</h2>
      <p>Each room has its own page on this site with its name, owner and description. The Navigator's
        <b>Link to this room</b> gives you its address. The room owner can take a thumbnail for the Navigator
        with the camera (${a(urls.help('camera'), 'camera')}).</p>`,
  },
  {
    slug: 'groups',
    title: 'Groups',
    summary: 'Start a group with its own badge, home room and forum.',
    related: ['club', 'rooms', 'credits'],
    body: () => html`
      <p>Groups are in the Shop, on the <b>Groups</b> page. Creating one costs nothing, but you need Club
        (${a(urls.help('club'), 'Club')}) and a room of your own to be its home room.</p>
      <h2>Setting it up</h2>
      <ul>
        <li>Give it a name and a description, design its badge and pick its colours.</li>
        <li>Choose who can join: anyone, only people a group admin accepts, or nobody new for now.</li>
        <li>Turn on the group forum in the group's settings if you want one, and choose who may read, post and
          moderate.</li>
      </ul>
      <p>Every group has its own page on this site with its badge, members and home room.</p>`,
  },
  {
    slug: 'pets',
    title: 'Pets',
    summary: 'Adopt a pet, look after it and teach it tricks.',
    related: ['rooms', 'bots', 'build-pass'],
    body: () => html`
      <p>Pets are on the <b>Pets</b> pages of the Shop. Food, toys, nests and breeding boxes are in the {names.buildPass}
        catalog.</p>
      <h2>Looking after a pet</h2>
      <ul>
        <li>Place it in a room from your inventory. Click it to see its level, energy and happiness.</li>
        <li>Hungry or tired pets don't listen well. Give them food, water and a nest to sleep in.</li>
        <li>Scratch a pet to make it happy. Everyone can scratch 3 pets a day.</li>
      </ul>
      <h2>Training</h2>
      <p>Say the pet's name and then a command, for example <span class="mono">Bobby sit</span>. The pet's info box
        lists the commands it knows. Each command it follows gives it experience, and new commands come with new
        levels.</p>
      <p>${cmd(':ejectpets')} sends other people's pets out of your room, and ${cmd(':mutepets')} hides pet chat.</p>`,
  },
  {
    slug: 'bots',
    title: 'Bots',
    summary: 'Place a bot, give it something to say and make it dance.',
    related: ['rooms', 'pets', 'groups'],
    body: () => html`
      <p>Bots are on the <b>Bots</b> page of the Shop. You can have 25 bots in your inventory and 10 in one room.</p>
      <h2>What a bot can do</h2>
      <p>Click your bot to see its menu:</p>
      <ul>
        <li><b>Setup speech:</b> what it says and how often.</li>
        <li><b>Copy my looks:</b> the bot dresses like you.</li>
        <li><b>Change name:</b> up to 15 characters.</li>
        <li><b>Dance</b>, and <b>Relax</b> or <b>Stand still</b>: walk around the room or stay put.</li>
        <li><b>Pick up:</b> back into your inventory.</li>
      </ul>
      <h2>Setting up speech</h2>
      <ul>
        <li>Write each thing the bot should say on its own line. All lines together can hold 120 characters.</li>
        <li><b>Automatic chat</b> on: the bot says a line by itself every few seconds. The chat delay is the number
          of seconds between lines, at least 5.</li>
        <li><b>Mix sentences</b> on: the bot picks lines in a random order instead of top to bottom.</li>
        <li>Bot speech goes through the same word filter as everyone's chat.</li>
      </ul>
      <p>${cmd(':mutebots')} hides bot chat for you.</p>`,
  },
  {
    slug: 'camera',
    title: 'Camera',
    summary: 'Take photos, keep them as wall photos and show them on your profile.',
    related: ['rooms', 'credits', 'getting-started'],
    body: () => html`
      <p>Everyone has the camera. Open it from the toolbar or type ${cmd(':camera')}.</p>
      <ul>
        <li>Frame the room, zoom in or out and try the filters.</li>
        <li><b>Buy</b> a photo for 2 {names.credits}: it goes into your inventory as a photo you can hang on a wall.</li>
        <li><b>Publish</b> it for 10 {names.points}: it appears on your profile page on this site.</li>
        <li>Room owners can also use the camera to take the room's thumbnail for the Navigator.</li>
      </ul>
      <p>Published photos can be seen by anyone, so don't publish anything you wouldn't want strangers to see.</p>`,
  },
  {
    slug: 'software',
    title: 'The software behind the game',
    summary: 'What runs the game, and what that means for you.',
    related: ['getting-started', 'credits', 'camera'],
    body: (ctx) => html`
      <p>${ctx.GAME_NAME} runs on <b>${PRODUCT}</b>, the game server: the program that is the game. It keeps the
        rooms, furni, wired, pets, bots, the catalog and every account, and it talks to the game in your browser.
        The website you are reading is a small part of the same project.</p>
      <h2>What that means for you</h2>
      <ul>
        <li>There is nothing to install: the game opens in a new tab from ${a(urls.me(), 'My page')}.</li>
        <li>Everything is free. {names.Credits}, {names.points} and {names.premium} are earned in the game and never sold for money
          (${a(urls.help('credits'), '{names.credits} and currencies')}).</li>
        <li>The game is run by its own staff, who set its rules, its name and what it offers
          (${a(urls.help('rules'), 'the house rules')}).</li>
      </ul>
      <h2>Found a bug?</h2>
      <p>Tell a member of staff in the game. Please don't use a bug on purpose: that breaks the
        ${a(urls.help('rules'), 'house rules')}.</p>`,
  },
]);

const BY_SLUG = new Map(TOPICS.map((t) => [t.slug, t]));
export const topicBySlug = (slug) => BY_SLUG.get(String(slug)) ?? null;
