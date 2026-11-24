# Site API

The site's own pages read all their data and send all their changes through the site API, `/api/site/v1/...`.
The pages themselves are a theme of static files ([THEMES.md](THEMES.md)): any theme that speaks this API works.
Housekeeping has its own API ([HK-API.md](HK-API.md)); the game data API for other sites, `/api/public/...`, is a
third and unrelated one (its docs are on the site at `/api/public/api-docs/`): the site never calls it.

Code: the routes are page modules in `web/site-api/` listed in
`web/site-api/index.mjs`; login, register, logout, the game ticket and the password are in
`web/cms/routes.mjs`. The registry (`web/cms/registry.mjs`) applies the checks below to every route.

## Conventions

- Every answer is JSON with `cache-control: no-store`. Success: `{ "ok": true, ... }`. Failure: an HTTP status and
  `{ "ok": false, "error": "<a sentence to show>", "title"?: "<a short heading>", "field"?: "<form field>", "reason"?: "<code>" }`.
  `field` names the input the error belongs to.
- Times are Unix seconds (UTC). Players are `{ id, name, figure, gender }`; draw them with
  `/imaging/avatarimage?figure=…&gender=…` (`headonly=1` for a head) while `GET /site` says `avatars`, else (and
  when `figure` is empty) with initials.
- An unknown path under `/api/site/v1` is 404 `{ ok: false }`.
- 401 with `reason: "login-required"`: log in first (the call needs a session, or a closed area needs one, below).
- 429 with `retry-after`: too many requests.

## Security

**Sessions.** Logging in sets one cookie, `__Host-zel_session` (HTTPS) or `zel_session` (plain-HTTP dev):
`HttpOnly`, `Secure` on HTTPS, `SameSite=Lax`, `Path=/`, 7 days. It is HMAC-signed and tied to the stored password
hash; logout revokes it, a password change ends every other one, an active ban ends it.
No token is handed to the page: scripts never see the session, and nothing sits in `localStorage` or
`sessionStorage`.

**Writes (every POST).** A write is refused unless:

1. its body is `content-type: application/json` (415 otherwise). An HTML form on another site cannot send JSON, and
   a cross-site `fetch` with that type needs a CORS preflight, which this server never grants;
2. its `Origin` is this site (`ZEL_SITE_URL`, or one of `ZEL_EXTRA_ORIGINS`; in dev also the request's own host) and its
   `Sec-Fetch-Site`, when sent, is `same-origin` (403 otherwise);
3. the body is one JSON object of at most 16 KB, less where a call says so (400 otherwise).

`SameSite=Lax`, the Origin and fetch-metadata checks and the JSON rule are three independent layers, so there is no
separate CSRF token.

**Reads.** A GET whose `Sec-Fetch-Site` is `cross-site` or `same-site` is refused (403): only this site's pages (and
tools outside a browser, which send no fetch metadata) read the API. No CORS headers are sent, so another site's
script cannot read an answer either.

**Limits.** Per client (IPv4 address, IPv6 /64): 240 GETs and 30 POSTs a minute, plus each call's own limit (below).
Login: 10 per 10 minutes per client, a 2 s wait per attempt after 50 failed logins on one name. Registration: 5 an
hour per client and `site.registration.accounts_per_address`. At most 2 password hashes run at once.

**Bans.** An active ban of the account (or of its address or machine) refuses the login (403 with the ban's text)
and the game ticket, and ends the session: a page address opened with such a session redirects to `/`, whose
`GET /site` clears the cookie and answers the ban's text in `banNotice`.

**Closed areas.** The operator's switches in `config/zellera.env` (`ZEL_WEB_PUBLIC_COMMUNITY`, `_PHOTOS`, `_PROFILES`;
[README.md, Configuration](../../README.md#configuration)) hold for the API too: while the community area (news, staff), photos or players
(profiles, rooms, groups) are closed to visitors, their calls answer a logged-out visitor 401 `login-required`.
`ZEL_WEB_STAFF_PAGE=0` removes `GET /staff` (404) for everyone.

**Database.** The web has one database account. Its rights are what `web/grants.mjs` collects: the login and
registration columns plus the columns every module's `grants` names, so a module declares what it reads but shares
the account with the others. A site route's `ctx.db` only reads; a route marked `write: true` may add rows to the
tables its module's grants insert into (`POST /help-requests` writes `help_requests`), and the game itself changes
only through `web_commands` rows that the game server applies. Rows are deleted only by a housekeeping route, with
`ctx.db.remove`, from a table its module's grants mark `delete: true` (the device bans and their exemptions).

## Calls

### The site and your session

| Call | Body / query | Answer |
| --- | --- | --- |
| `GET /site` | | `{ name, names: { credits, points, premium, buildPass, … }, place: { word, plural }, avatars, register: { betaCode, open }, guests, accounts: { email: off \| optional \| required, avatarsPerAccount }, choosingAvatar, viewer: player \| null, sees: { photos, players, community, staff }, housekeeping, banNotice }`: the game's name, the currency and build pass names from the content (lower case, with capitalised and singular forms), what the site calls its world (the content pack's settings row `site.place.word`, lower case, else `game`, `games`; the theme capitalises it where a sentence starts), whether to draw players with the avatar imager (`ZEL_WEB_AVATAR_IMAGES`, and for a logged-out visitor only while `sees.players`: a figure is player data; `false`: initials, everywhere), whether registering needs a beta code (and is open at all), the login rules (`ZEL_WEB_EMAIL`, `ZEL_WEB_AVATARS_PER_ACCOUNT`), whether an account is logged in that has not picked an avatar yet (`choosingAvatar`: the theme sends it to `/avatars`), who is logged in (the avatar played as), which areas they may open (`staff`: the staff page exists and the community area is open to them), whether to show the way to housekeeping. A session cookie that no longer logs anyone in is cleared here (`set-cookie`); when a ban ended it (the cookie is genuine, not logged out, and its password still matches), `banNotice` is the ban's text for the login box, else `null` |
| `GET /front` | | `{ looks: [{ image }] }`: up to 6 featured looks, full figure pictures |
| `POST /login` | `{ login, password }` | `{ next }`, sets the session cookie. `login` is the avatar's name; while `ZEL_WEB_EMAIL` is on, also the account's e-mail address; with several avatars per account the e-mail address only, and `next` is `/avatars` (the session holds the account, no avatar yet) instead of `/me`. 400 / 401 with `field`, 403 banned (an address ban before an avatar is picked), 429 |
| `POST /register` | `{ username, password, password2, email?, beta? }` | 201 `{ next }`, sets the session cookie: a new account and its first avatar (`username`), played at once. `email` is asked while `ZEL_WEB_EMAIL` is `optional` or `required` (one account per address). A password has at least 8 characters. 400 / 403 with `field`; 409 with `field: username` for a taken name while no e-mail address was sent, else one 409 without a field for a taken name or address alike; 429 |
| `POST /guest` | `{}` | 201 `{ ok, username, password }`, sets a year-long session cookie: a new account with an avatar named `Guest` and a number, a random password (sent this once, so the page can keep it and set a new one with `/me/password`), and one free rename in the game. Only while `ZEL_WEB_GUEST_ACCOUNTS=1` (`GET /site` says so in `guests`); under the same address limits as `/register`. 403 guests off, closed beta or banned address, 429, 503 |
| `POST /logout` | `{}` | clears and revokes the session |
| `POST /me/ticket` | `{}` | `{ ticket }`: a one-time game ticket (60 s) for `/play`. The theme posts it as the form field `sso` (`POST /play`), so it never shows in an address; `/play?sso=<ticket>` works too. 401, 403 banned, 429 (6 a minute) |
| `POST /me/password` | `{ current, password, password2 }` | sets the account's new password; every other session of the account (every avatar) ends, this browser gets a new cookie. 400 / 401 with `field`, 429 |
| `GET /me/account` | | `{ rules: { email, avatars }, email \| null, avatars: { count, max } }`: the logged-in account's login facts for the settings page (`email` is `null` while `ZEL_WEB_EMAIL=off`). 401 |
| `POST /me/email` | `{ email, current }` | `{ email }`: the account's new login address, with the current password; empty removes it unless it is `required`. 400 / 401 / 409 (the address can not be used) with `field`; 404 while `ZEL_WEB_EMAIL=off`; 429 |
| `GET /me/privacy` | | `{ privacy: { profileVisible, onlineStatusVisible, friendCanFollow, friendRequestEnabled, roomInvitesEnabled } }`, stored in the game's `player_preferences`. 401 |
| `POST /me/privacy` | any of those keys as `true` / `false` | `{ state: saved \| pending, privacy }` once the game applied it (a `setPrivacy` command; 202 `pending` after ~2 s). 400 without a key, 422 refused, 503, 30 a minute. A hidden profile (`profileVisible: false`) exists only for its owner and staff: profile pages, photos and Find a player leave it out for everyone else |

### Several avatars per account

While `ZEL_WEB_AVATARS_PER_ACCOUNT` is above 1 (it needs `ZEL_WEB_EMAIL=required`), an account (e-mail address and
password) holds that many avatars. The session holds the account and the avatar played as; `/me/ticket` is for that
avatar. With one avatar per account these calls answer 404.

| Call | Body / query | Answer |
| --- | --- | --- |
| `GET /avatars` | | `{ max, canCreate, avatars: [{ id, name, figure, gender, motto, memberSince, lastOnline, current, banned, ban: { reason, until \| null } \| null }] }`, oldest first. 401 |
| `POST /avatars/select` | `{ id }` | `{ next }`, a new session cookie playing as that avatar (the old one is revoked). 404 not one of yours, 403 banned (its own ban, or an address or machine ban), 429 |
| `POST /avatars/check-name` | `{ name }` | `{ available, reason \| null }`: whether a new avatar could take this name |
| `POST /avatars` | `{ name }` | 201 `{ id, next }`: a new avatar with a new player's purse and look, played at once. 400 / 409 with `field: name`, 409 the account is full, 403 an address ban, 429 |

### My page

| Call | Body / query | Answer |
| --- | --- | --- |
| `GET /me` | | `{ user: { id, name, figure, gender, motto, online, lastOnline, memberSince }, purse: { credits, points, premium, seasonal: [{ type, name, amount }] }, memberships: { club: { until } \| null, buildPass: { until } \| null }, rooms: { total, rows: [{ id, name, users, max, door }] }, groups: { total, rows: [{ id, name, role }] }, news: [{ id, title, excerpt, created }], friends: { total, rows: [player + online] }, motto: { max } }`. Every balance is a number (0 without a row); `seasonal` lists the game rules' seasonal currency types while `seasonal_currency.enabled` is on, named by the client's catalog text. `memberships` holds the end of Club and Build Pass while they run. `rooms` are the player's own, invisible ones left out, the 5 busiest; `groups` the first 6 the player is in (`role` Owner, Admin or Member), the oldest membership first. 401 logged out |
| `GET /help-requests` | | `{ mode: account \| anyone \| off, canSend, reason: login \| off \| null, guest, topics: [{ key, label }], limits: { message: [min, max], name, contact, open }, mine: [{ id, topic, message, created, state: open \| picked \| closed, closed, answers: [{ message, created }] }] }`: whether this visitor may ask staff (`ZEL_WEB_HELP_TICKETS`) and, logged in, their last 20 questions with staff's answers, oldest first |
| `POST /help-requests` | `{ topic, message, name?, contact? }` (8 KB at most) | 201 `{ id }`. A visitor without an account (mode `anyone`) gives `contact`, an e-mail address, and may give `name`; a player gives neither. 400 with `field` (`topic`, `message` 10 to 2000 characters, `name`, `contact`); 401 `login-required` (mode `account`); 403 `off`; 429: 3 an hour per client, `reason: open` (3 questions of the player still waiting), `reason: day` (10 a day from one address) |
| `POST /me/motto` | `{ motto }` (2 KB at most) | `{ state: saved \| filtered \| pending, motto }` once the game applied it (202 `pending` after ~2 s). 400 `field: motto` with `reason` `length` / `line` / `symbols`; 422 `reason: game` (the game refused it); 503 `notSaved`. 10 a minute. The game's word filter, trim and `player.motto.max.characters` decide what is stored |
| `GET /players?name=` | | `{ q, rows: [player + motto, online], more }`: names that start with `name`, private profiles left out (staff see them), 20 at most. Logged in only, 30 a minute |

### Community

| Call | Body / query | Answer |
| --- | --- | --- |
| `GET /staff` | | `{ ranks: [{ level, name, members: [player + motto, online] }] }`: everyone from level 4 (Employee, the first staff level) up, highest rank first. Community area; 404 while the staff page is off |
| `GET /profiles/<name>` | | `{ user: { id, name, figure, gender, motto, memberSince, online, lastOnline \| null }, own, private, friends, friendList: [{ id, name, figure, gender, online }], groups: [{ id, name, role }], rooms: [{ id, name, users, max, door: open \| doorbell \| password }], photos: [{ id, published, thumb \| null }], morePhotos }`. `user.name` is the stored name (a different case still matches: the page puts the stored one in the address). A private profile answers 404 `reason: private` to everyone but its owner and staff (`private: true` tells them). Players area; 404; 120 look-ups a minute |
| `GET /profiles/<name>/photos/<uuid>` | | `{ user: { id, name }, photo: { id, published, image \| null }, room: { id, name } \| null }`. 404 for an unpublished photo, someone else's, or a hidden profile. Photos area; 120 a minute |
| `GET /photos` | `?user=<name>&page=<n>` | `{ user: { name, private } \| null, photos: [{ id, owner, published, room \| null, thumb \| null }], total, page, hasNext }`, 24 a page, newest first. A private profile gives `private: true` and no photos, except to its owner. 404 for an unknown user or a page that does not exist. Photos area; 120 a minute |
| `GET /groups/<id>` | | `{ group: { id, name, description, type, created, forum, owner \| null, room: { id, name, users, max } \| null }, memberCount, members: [player + role, since] }`, 100 members at most. Players area; 404; shares the 120-a-minute look-up limit |
| `GET /rooms/<id>` | | `{ room: { id, name, description, owner \| null, ownerLabel \| null, category \| null, users, max, door, group \| null, created, tags } }`. 404 for an invisible room; never the room's password. Players area; shares the look-up limit |

### Help, news and legal

| Call | Body / query | Answer |
| --- | --- | --- |
| `GET /news` | `?page=` (from 1) | `{ page, hasNext, items: [{ id, title, excerpt, created }] }`, newest first, 10 a page; a bad or empty later page is 404. Community area |
| `GET /news/<id>` | | `{ article: { id, title, body, created, link: { href, text } \| null } }`; `body` is plain text (a blank line between paragraphs); `link` only for a path on this site; disabled or unknown is 404. Community area |
| `GET /help` | | `{ title, lede, groups: [{ id, title, tone, topics: [{ slug, title, summary }] }], elsewhereHtml }` |
| `GET /help/<topic>` | | `{ topic: { slug, title, summary, bodyHtml }, related: [{ slug, title, summary }] }`; unknown is 404 with `groups` (every topic) |
| `GET /safety` | | `{ title, lede, sections: [{ id, title }], bodyHtml }`; the headings in `bodyHtml` carry the section ids |
| `GET /legal/terms`, `GET /legal/privacy` | | as `/safety`, plus `updated` |

`...Html` fields are this site's own texts (the game's name escaped), safe to insert as HTML; every other text in
an answer is data and is inserted as text.

## Not part of this API

Plain redirects stay on the server: `/rd/<id>` → the profile, `/home/<name>`, `/profile` → `/me`, `/groups/<id>` →
`/groups/<id>/id`, `/help/<anything>` → the matching topic, `/shop`, `/credits*`, `/identity/useOrCreateAvatar`,
`/account/disconnected`. The client's own calls are not the site's either: `POST /api/log/crash`
(client errors), `/gamedata/pages/*` (the client's help pages), `/play?sso=` (the game).
