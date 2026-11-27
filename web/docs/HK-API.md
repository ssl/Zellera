# Housekeeping API

Housekeeping's pages (`/housekeeping/...`, the `hk/` files of a theme: [THEMES.md](THEMES.md)) read and write only
through the housekeeping API, `/api/hk/v1/...`. It is separate from the site API ([SITE-API.md](SITE-API.md)): its
own routes (the modules in `web/hk-api/`, listed in `web/hk-api/index.mjs`), its
own checks, its own database rights.

## Conventions and security

Answers, errors, sessions and the write rules (JSON body, Origin and fetch metadata, no token) are the site API's:
[SITE-API.md, Conventions](SITE-API.md#conventions) and [Security](SITE-API.md#security). On top of them, every call:

- needs a staff member: logged out → 401 `reason: login-required`; logged in without `moderation.tool` → the
  ordinary 404 (housekeeping does not show itself); a banned staff member → 403 `reason: banned`; a rank without
  the call's key → 403 `reason: permission` with `missing: [keys]`. Keys come from the staff member's rank
  (`rank_permissions`), checked on every request;
- is refused (403) when its `Sec-Fetch-Site` is `cross-site` or `same-site`, so a link on another site can never make
  a staff member's browser load a call that writes an audit row; the page addresses ask first instead
  (`hk/confirm`);
- counts against 600 GETs per 10 minutes and 60 POSTs an hour per staff member (per IP before the login is known),
  plus its own limit;
- answers with `x-robots-tag: noindex, nofollow` and `referrer-policy: no-referrer`;
- logs what it changes (and the views marked *audited*) in `staff_actions`.

## Calls

| Call | Key | Body / query | Answer |
| --- | --- | --- | --- |
| `GET /me` | `moderation.tool` | | `{ name, staff: { id, name, rank, rankName }, can: { <key>: bool }, avatars, extensions: [{ key, label, href }] }`: the game's name, who is using housekeeping, the keys they hold (for the navigation), whether to draw players with the avatar imager (`ZEL_WEB_AVATAR_IMAGES`; `false`: initials), and the housekeeping pages of the enabled extensions that this staff member may open |
| `GET /dashboard` | `moderation.tool` | | `{ stats: { online, newDay, newWeek, roomsWithUsers, openTickets?, pickedTickets?, activeBans? }, online: [{ id, name, lastLogin }] (30), log: [logRow] (10) \| null, version: { site, server } }`; `version.server` is the version the game server wrote at its last start; the ticket counts with `moderation.tickets`, bans and the log with `moderation.player.ban` |
| `GET /users` | `moderation.tool` | `?q=` (start of a name, or an id), `?page=` | `{ q, id, page, hasNext, rows: [user] }`, 50 a page; 60 searches a minute |
| `GET /users/<id>` | `moderation.tool` | `?commands=<ids>` | `{ user (+ motto, lastLogin, homeRoom), wallet: [{ key, label, amount }] (credits, points and premium, labelled with the content's names and left out while the content does not define them, then each seasonal currency while the game rules turn it on, key `currency_<type>`), standing \| null, ban \| null, account: { id, avatars: [user] } \| null, bans, friends, roomCount, rooms, badges: [{ code, name \| null, slot, image \| null }], badgesMore, commands: { sent, recent }, can: { ban, wallet, badges, rank }, levels: [{ id, name, text, keys: [{ key, comment }] }] }`; `levels` only with `can.rank` |
| `GET /users/<id>/sanctions` | `moderation.tool` | | `{ user, can: { ban }, bans: [ban + canLift], standing, sanctions: [{ id, level, kind, hours, reason, probationEndsAt, muted, muteUntil }], permissionChanges, log: [logRow] \| null }` |
| `GET /users/<id>/chatlog` | `moderation.player.chatlog`, audited | `?before=&bid=` | `{ user, cursor, rows: [{ id, at, room, to, message }], next }`, 100 a page |
| `GET /users/<id>/visits` | `moderation.player.chatlog` | `?before=&bid=` | `{ user, cursor, rows: [{ id, room, enteredAt, leftAt }], next }` |
| `GET /identity/<id>` | `moderation.player.ban`, audited | | `{ user, addresses: { registered, last, device }, shared: [{ kind, label, value, rows: [user], more }], bans: [ban + user, on] }`: the only call that shows IP addresses |
| `GET /actions/ban` | `moderation.player.ban` | `?user=<name or id>` | `{ target: { id, name, rank, rankName, online } \| null, bans: [active ban + canLift], lengths: [{ value, label }], reasonMax }`; 403 with the reason for yourself or a rank not below yours; 404 unknown user |
| `POST /actions/ban` | `moderation.player.ban` | `{ user, hours, reason }` | `{ ban: { id }, userId, message }`; 400 with `field` and `errors`; 403 (audited as refused); 409 when the rank changed; 30 an hour. The game disconnects them (web command `kickBanned`) |
| `GET /actions/unban` | `moderation.player.ban` | `?ban=` | `{ ban: { id, user, covers, reason, staff, bannedAt, expiresAt, permanent, active }, noteMax }`; 403 with the reason |
| `POST /actions/unban` | `moderation.player.ban` | `{ ban, note? }` | `{ userId, message }`; 403, 409 (already ended); 30 an hour |
| `GET /users/<id>/devices` | `moderation.tool` | | `{ user, exempt: { at, staff } \| null, can: { ban, exempt }, devices: [device + sharedId, sharedCombo, bans: [device ban ids]], more }`: the devices the player logged in from, last seen first (50); `sharedId` and `sharedCombo` count the other players with the same id and with the same gpu, scr and fnt (`null`: the piece is not usable), `bans` the active device bans that match |
| `GET /users/<id>/devices/shared` | `moderation.tool` | `?id=` (16 hex) or `?gpu=&scr=&fnt=` (8 each), `?page=` | `{ user, by: id \| combo, pieces: [{ name, value }], total, page, hasNext, rows: [user + firstSeen, lastSeen, logins, banned, exempt] }`: the other players with that id or combination, newest accounts first, 50 a page; 400 for a piece that is not usable; 404 when the player has no such device |
| `GET /devices/compare` | `moderation.tool` | `?a=&b=<name or id>`, `?da=&db=<reading>` | `{ a: user, b: user, devices: { a: [device], b: [device] } (20 each), pair: { a, b (readings), pieces: [{ name, a, b, state: equal \| different \| unusable }], equal, different, unusable, sameId, sameCombo } \| null }`: without `da` / `db` the two devices with the most equal pieces; `{ a: null, … }` without two users; 404 unknown user |
| `POST /users/<id>/device-exemption` | `moderation.player.ban` | `{ exempt: true \| false }` | `{ exempt, message }` or `{ unchanged: true }`; an exempt player is refused by no device ban (account, machine and IP bans still count); 403 for yourself or a rank not below yours; 30 an hour |
| `GET /device-bans` | `moderation.tool` | `?page=` | `{ page, hasNext, can: { ban }, patterns: [{ id, pieces: { id, gpu, … } (`null`: any value), reason, staff, createdAt, expiresAt \| null, active, matches \| null, canDelete }] }`, newest first, 50 a page; `matches`: the players an active pattern matches now |
| `GET /device-bans/new` | `moderation.player.ban` | `?user=&reading=` | `{ user, device, lengths: [{ value, label }], reasonMax, minWithoutId }`; 404 when the player has no device with that reading |
| `GET /device-bans/count` | `moderation.player.ban` | `?user=&reading=&pieces=id,gpu,…` | `{ pieces, matches, refusal \| null }`: the players the ticked pieces match now, and why this staff member may not save it (their own device, a rank not below theirs); 400 `field: pieces` below the minimum (the id, or at least three pieces; usable ones only) |
| `POST /device-bans` | `moderation.player.ban` | `{ user, reading, pieces, hours, reason, seen }` | `{ pattern: { id }, matches, message }`; `seen` is the `matches` the form showed: 400 without it, 409 with `matches` when it is another number by now; 400 `field` `pieces` / `hours` / `reason`; 403 (audited); 409 with `pattern: { id }` when an active ban holds the same pattern; 30 an hour. The game disconnects matching players (web command `kickBanned`) |
| `POST /device-bans/<id>/delete` | `moderation.player.ban` | `{}` | `{ message }` (the row is deleted; the audit log keeps the pattern); 403 for a pattern a higher rank made; 404; 409 |
| `POST /users/<id>/wallet` | `housekeeping.wallet` | `{ <key>, was_<key> }` for each wallet key | `{ commands: [{ id, kind, status, result, processedAt }] }` after up to ~2 s; `{ unchanged: true, message }`; 400, 503; 403 for an account not below yours, and for your own |
| `POST /users/<id>/badges/give`, `/badges/take` | `housekeeping.badges` | `{ badge }` | `{ commands }`; 400 / 409 `field: badge`; 403 for an account not below yours, and for your own |
| `POST /users/<id>/rank` | `housekeeping.ranks` | `{ level }` | `{ commands }` or `{ unchanged: true }`; 400 `field: level`; 403 for your own account |
| `GET /audit` | `moderation.player.ban` | `?tab=permissions&actor=&target=&action=&page=` | `{ tab, filter, warnings, page, hasNext, rows }`: `logRow`s, or with `tab=permissions` `{ id, at, actor, action, subject, old, new, via, target }`; 400 bad filter |
| `GET /rooms` | `moderation.room.info` | `?q=&page=` | without `q`: `{ q: '', busy, newest, users }`; with `q`: `{ q, page, hasNext, rows, users }`; a row is `{ id, name, ownerId, door, players, maxPlayers, categoryId, category, created }`; 60 searches a minute |
| `GET /rooms/<id>` | `moderation.room.info` | | `{ room, can: { logs, tickets, ask }, question: { max, seconds }, rights, bans, mutes, tickets \| null, visits, users, raw, note }`; lists are `{ rows, more }` (100 at most); `raw` is every column but the password |
| `GET /rooms/<id>/chatlog` | `moderation.room.chatlog`, audited | `?before=` | `{ room: { id, name, exists }, pageSize, before, nextBefore, lines, users }`, newest first, 150 a page |
| `POST /rooms/<id>/question` | `moderation.room.alert` | `{ question, seconds }` | `{ command: { id, status, result } }`; 400 `field` `question` / `seconds`; 503 read-only; audited (the game asks everyone in the room; one question at a time) |
| `GET /photos/<item id>` | `moderation.tickets` | | `{ item, uuid, image \| null, photo \| null, tickets, users, rooms }` |
| `GET /tickets` | `moderation.tickets` | `?state=open\|picked\|closed&page=` | `{ state, states: [{ key, label, count }], page, hasNext, tickets, users, rooms, note }`; a ticket is `{ id, state, type, topic, senderId, reportedId, roomId, moderatorId, created, closed, issue, score, photoItemId }` |
| `GET /help-requests` | `moderation.tickets` | `?state=open\|picked\|closed&page=` | `{ state, states: [{ key, label, count }], page, hasNext, requests, users }`, 50 a page, open and picked oldest first, closed newest first; a request is `{ id, state, topic: { key, label }, playerId, guestName, staffId, created, answered (the newest answer, 0 without one), closed, excerpt }` (questions from the site's help page, `ZEL_WEB_HELP_TICKETS`) |
| `GET /help-requests/<id>` | `moderation.tickets` | | `{ request (+ message, contact, ip, answers: [{ id, staffId, message, created }] oldest first), users, limits: { answer } }` |
| `POST /help-requests/<id>` | `moderation.tickets` | `{ action: pick \| release \| answer \| close \| reopen, answer?, close? }` | the request as it is now. `answer` (1 to 2000 characters) adds an answer to an open or picked request and leaves it picked by you, or closes it with `close: true`; the player reads every answer on the help page. 409 when the state does not allow the action (someone else changed it). Audit `hk.help.<action>` |
| `GET /tickets/<id>` | `moderation.tickets`, audited | | `{ ticket (+ sanctioned, appeal), chatBefore, can: { room, roomLogs, userLogs }, context, users, rooms, note }`; `context.kind`: `private`, `forum`, `photo`, `room`, `users`, `none` |
| `GET /chatlogs` | `moderation.room.chatlog` + `moderation.player.chatlog`, audited | `?user=&room=&q=&from=&to=&page=` | `{ search, limits, results \| null }`; a refused search is 400 / 404 `{ ok: false, error, errors, search, limits }`; 20 a minute |
| `GET /landing-view` | `housekeeping.landing_view` | | `{ slots, articles: [{ id, title, text, button_text, button_type, button_link, image, shown, imageShown }] }` |
| `GET /landing-view/new`, `GET /landing-view/<id>` | `housekeeping.landing_view` | | `{ article \| null, form: { actions, clientLinks, catalogPages, limits, imageHint } }` |
| `POST /landing-view`, `POST /landing-view/<id>` | `housekeeping.landing_view` | `{ title, text, image, button_type, button_text, button_link }` | 201 (new) or 200 `{ article }`; 400 with the reason; 60 an hour |
| `POST /landing-view/<id>/move` | `housekeeping.landing_view` | `{ direction: up \| down }` | the list, as `GET /landing-view` |
| `POST /landing-view/<id>/delete` | `housekeeping.landing_view` | `{}` | the list (the article is marked deleted) |
| `GET /news` | `housekeeping.news` | | `{ articles: [{ id, title, link_text, link_url, shown, published }] }`: the site's news in the order of the news page; `published` is the article's date, 0 while it was never shown |
| `GET /news/new`, `GET /news/<id>` | `housekeeping.news` | | `{ article \| null (a list row + body), limits: { title, bodyBytes, linkText, link } }` |
| `POST /news`, `POST /news/<id>` | `housekeeping.news` | `{ title, body, link_url, link_text, shown: true \| false }` | 201 (new, at the top of the list) or 200 `{ article }`; 400 with `field` and the reason; 60 an hour. `body` is plain text of at most `bodyBytes` bytes of UTF-8; `link_url` is empty or a path on this site |
| `POST /news/<id>/show` | `housekeeping.news` | `{ shown: true \| false }` | the list, as `GET /news`; an article shown for the first time gets its date |
| `POST /news/<id>/move` | `housekeeping.news` | `{ direction: up \| down }` | the list |
| `POST /news/<id>/delete` | `housekeeping.news` | `{}` | the list (the row is deleted) |
| `GET /youtube` | `housekeeping.youtube` | | `{ keySet, playlists: [code] }`: the playlists every TV furni offers, in order, and whether the config has `ZEL_YOUTUBE_API_KEY` (the key itself never reaches the web's answers) |
| `POST /youtube/add`, `/remove`, `/move` | `housekeeping.youtube` | `{ playlist }` (add takes an address with `list=` or a bare code), move also `{ direction: up \| down }` | 200 with the `GET` answer; 400 with the reason; 120 an hour. The game reads the playlists again at once (web command `reloadTvPlaylists`) |

`user` is `{ id, name, figure, gender, rank, rankName, online }`; `logRow` is `{ id, at, actor: { id, name }, action,
target: { type, id, label }, detail, success }` (IP addresses masked in `detail`).

`device` is `{ reading, pieces: [{ name, value, usable }], firstSeen, lastSeen, logins }`: the ten hashes the play
page measured at a login (`id`, `gpu`, `scr`, `cpu`, `tz`, `fnt`, `cnv`, `aud`, `gl`, `ua`) and `reading`, the same
ten as one string (`1-<id>-<gpu>-…`). No call gives what a piece was measured from: only the hashes are kept. A piece
of all zeros is one the browser does not give, one of all f's cannot be trusted; neither is `usable`, and it equals
nothing. A device ban is a pattern: a login whose device equals every piece the pattern names is refused; the game
reads the patterns and the exemptions at each login.

The mod tool's links stay plain redirects for staff: `GET /housekeeping/users/lookup` and
`GET /housekeeping/users/sanctions` take `?name=` or `?id=` and answer 302 to the user's page (no exact name:
`/housekeeping/users?q=<name>&missing=1`).
