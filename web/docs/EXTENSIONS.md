# Web extensions

A theme ([THEMES.md](THEMES.md)) is static files only. A feature that needs the server (its own routes, its own
tables) is a web extension: a folder the web loads at start, with a small fixed interface to the rest of the web.
The bundled one, `guestbook`, is on by default and is the example to copy.

## Turning them on

An extension is code you run: it loads into the web process and can do whatever that process can (read its
secrets, reach its database account, answer any request). Install one only from someone you trust as much as the
game's own code. The table check and the rules below keep an honest extension inside its own corner; they are a
convenience, not a boundary against a hostile one.

`ZEL_WEB_EXTENSIONS` in `config/zellera.env` (or `zellera.local.env`): a comma list of folder names under
`custom/extensions/` or `web/extensions/`, or absolute folders of your own (the folder's last part is the extension's
name). Unset means `guestbook`; empty means none. Keep your own in `custom/extensions/<name>/` (next to `zel`):
that folder is not in git, so an upgrade never touches it, and an extension there wins over one of the same name in
`web/extensions/`. Only this list decides what is loaded: the settings row can turn a loaded extension off, never
load another one. `./zel start` checks the list, writes it into the settings row `site.extensions` and
grants the web's database account its rights on the extensions' tables; the web loads them at start. Restart after a
change. A broken extension (bad manifest, failing or changed migration) stops the web with its reason. An extension
whose migrations can't reach the database is left out and logged; restart the web once the database answers.

## A folder

```
web/extensions/<name>/
  extension.json          the manifest
  server.mjs              the server module (optional)
  migrations/V1__x.sql    its SQL migrations (optional)
  theme/                  its theme parts: css/, js/ (optional), served at /theme/ext/<name>/...
```

`<name>` is 2 to 32 characters `a-z 0-9`, no `_` or `-`, so `ext_<name>_` always names one extension's tables.

### extension.json

```json
{
  "name": "guestbook",
  "version": "1.0.0",
  "description": "Short messages players leave on each other's profile.",
  "server": "server.mjs",
  "needs": {
    "tables": ["entries"],
    "lookups": ["players", "blocks", "filter"],
    "permissions": ["moderation.tickets"]
  },
  "theme": {
    "slots": { "profile": { "styles": ["css/guestbook.css"], "scripts": ["js/profile.js"] } },
    "pages": {
      "hk/ext/guestbook": { "path": "/housekeeping/ext/guestbook", "perm": "moderation.tickets", "title": "Guestbook",
        "nav": "Guestbook", "scripts": ["js/hk.js"] }
    }
  }
}
```

- `zel` (optional): the versions the extension is made for, as `">=1.0.0"` or `">=1.0.0 <2.0.0"`. The web refuses to
  start with an extension that needs another version than the one running.
- `needs.tables`: the extension's own tables, named `ext_<name>_<table>` in the database (`ext_guestbook_entries`).
  Its SQL may name these and nothing else.
- `needs.lookups`: the read-only game lookups it gets (below): `players`, `blocks`, `filter`.
- `needs.permissions`: the housekeeping keys it may ask about (`viewer.can(key)`), from the keys housekeeping knows
  (`web/hk-api/_guard.mjs`).
- `theme.slots`: stylesheets and scripts added to every theme page whose route entry lists that slot (the default
  theme's profile page has the slot `profile`).
- `theme.pages`: pages of its own, in the theme's route table format plus `path`: a site page `ext/<name>[/...]` at
  `/ext/<name>[/...]` (optional `gate`, `auth: "user"`), or a staff page `hk/ext/<name>[/...]` at
  `/housekeeping/ext/<name>[/...]` with the key it needs (`perm`). `nav` puts a staff page in housekeeping's tabs for
  those who hold the key. Their files are the extension's (`theme/`), the frame the theme's (`site.js` / `hk.js`).

### server.mjs

The default export gets a registrar and adds routes; it runs once at start.

```js
export default (ext) => {
  const { sql } = ext;
  ext.get('/entries/:name', { gate: 'players' }, async (ctx) => { ... ctx.json(200, { entries }); });
  ext.post('/entries/:name', { auth: 'user', maxBody: 2048 }, async (ctx) => { ... });
  ext.hk.get('/entries', { perm: 'moderation.tickets' }, async (ctx) => { ... });
  ext.hk.post('/entries/:id(\\d{1,10})/delete', { perm: 'moderation.tickets' }, async (ctx) => { ... });
};
```

A path is the part after the extension's prefix, with the site's route syntax (`:param`, `:param(regex)`):
`ext.get` / `ext.post` serve `/api/ext/<name><path>`, `ext.hk.get` / `ext.hk.post` serve
`/api/hk/v1/ext/<name><path>`. No other address can be registered. Options: `gate` (`players`, `photos`,
`community`: closed to logged-out visitors while the area is), `auth: 'user'` (401 when logged out), `maxBody` (a
POST's JSON body, at most 16384 bytes), and for staff routes `perm` (required).

The handler gets `ctx`:

| | |
| --- | --- |
| `ctx.params`, `ctx.query`, `ctx.body` | the path's parameters, the query (`URLSearchParams`), a POST's JSON object |
| `await ctx.viewer()` | `{ loggedIn, id, name, figure, gender, staff, can(key) }`; `staff`: housekeeping access, which also shows private profiles; `can` answers only for `needs.permissions` |
| `ctx.staff` | on staff routes: `{ id, name, rankName, can(key) }` |
| `await ctx.site()` | `{ place: { word, plural }, avatars, sees: { photos, players, community, staff } }`: the site's switches for this visitor |
| `ctx.db.query(sql\`…\`)` | a read (`SELECT`) of its own tables |
| `ctx.db.write(sql\`…\`)` | `INSERT`, `UPDATE` or `DELETE` of its own tables, in POST routes only |
| `ctx.players` | `byId(id)`, `byName(name)`, `byIds(ids)` → `{ id, name, figure, gender, private }` (`needs.lookups: players`) |
| `ctx.blocks` | `blocked(by, whom)`: whether `by` blocked `whom` in the game (`needs.lookups: blocks`) |
| `await ctx.filter(text)` | `{ text, hidden }`: the game's word filter on what the viewer wrote, as room chat filters a line (below) (`needs.lookups: filter`) |
| `await ctx.rateLimit(bucket, max, seconds)` | `false` after it answered 429; counted per player, or per address when logged out |
| `ctx.json(status, value)`, `ctx.error(status, message, extra)` | the answer, in the site API's shape (`{ ok, ... }`, `{ ok: false, error, field?, reason? }`) |
| `ctx.audit({ action, targetType, targetId, label, detail })` | staff routes: a row in housekeeping's audit log, action `ext.<name>.<action>` |
| `ctx.log(message)` | a line in the web's log |

### The word filter

`ctx.filter(text)` applies the game's word filter as room chat does: `word_filters` rows matched anywhere, case
ignored, longest first, replaced by their `replacement`; look-alike characters count as
letters while `word_filter.normalises_text` is on, and nothing is filtered while `word_filter.is_on` is off. `hidden` is true
when the text holds a word without a `replacement`: the game drops such a chat line, so don't store or show it. A viewer whose
rank holds `chat.unfiltered` gets the text back unchanged. The words are re-read at most every 2 seconds. The site
can't apply a word's `mute_seconds`. Store the filtered text and check lengths after filtering (a replacement can be
longer than the word).

### Migrations

`migrations/V<number>__<words>.sql`, applied in order at start and recorded in the table `ext_migrations`
(extension, version, file name, checksum). Never edit one that is applied: a changed or deleted applied migration stops
the web with that reason; add a new one. A migration creates, changes or fills the extension's own tables
(`CREATE TABLE`, `CREATE INDEX`, `ALTER TABLE`, `DROP TABLE`, `INSERT`, `UPDATE`, `DELETE`), one statement per `;` at a
line's end. No foreign keys to the game's tables: keep player ids as plain columns and look the players up.

## Security

- **Routes.** Only under `/api/ext/<name>/` and `/api/hk/v1/ext/<name>/`; the loader refuses anything else. Every
  call goes through the same checks as the site API ([SITE-API.md, Security](SITE-API.md#security)): the HttpOnly
  session cookie, writes only as a JSON body from a page of this site (Origin and `Sec-Fetch-Site`), reads refused
  from other sites, the per-client limits of 240 GETs and 30 POSTs a minute before the extension's own. Staff routes
  add housekeeping's checks ([HK-API.md](HK-API.md)): a staff member, not banned, holding the route's key.
- **An extension that is off** answers nothing: its routes are 404, its pages the theme's 404 page, its slot parts are
  not on any page.
- **Tables.** Every statement an extension sends through `ctx.db` is checked to name only its own tables (no comma
  joins, one statement, no table in parentheses; comments are taken out before it runs), and the database enforces
  the same: the web's account has rights on `ext_migrations` and on each
  declared `ext_<name>_<table>` (`web/grants.mjs`, run by `zel`), nothing more there. The game's
  tables are reached only through the lookups, which read a few columns (id, name, figure, gender, the profile's
  privacy, blocks, the keys of a rank, the filtered words and the filter's two settings).
- **Text.** What players write is data: a widget sets it with `textContent` (the theme's `h()`), never `innerHTML`.

## Theme side

A widget script registers itself for a slot; the page asks the slot for its widgets when it draws:

```js
Site.widget('profile', ({ user, own, hidden }) => element || null);   // the extension's script
Site.slot('profile', { user, own, hidden });                          // the page: an array of elements
```

`Site.extApi('<name>')` is the extension's API client (`get(path)`, `post(path, body)`), the same as `Site.api` under
`/api/ext/<name>`. A theme can restyle a widget by shipping `ext/<name>/<file>` itself; it wins over the extension's
copy. A theme that cannot do without an extension lists it in `theme.json` (`"needs": ["guestbook"]`); the web says so
at start when it is not on.

## The guestbook

Players write messages of at most 200 characters on a profile; the profile shows the newest 20 (older ones on
request) with the writer's head (initials while `ZEL_WEB_AVATAR_IMAGES=0`), name and time. A message goes through
the game's word filter and is stored filtered; one with a hiding word, or one that the filter leaves empty or makes
longer than 200 characters, is refused (400, `reason: "filtered"`). The owner, the writer and
staff with `moderation.tickets` delete a message; a player the owner blocked in the game cannot write there. The
messages follow the profile: visitors see them only while `ZEL_WEB_PUBLIC_PROFILES=1`, a private profile shows them to
its owner and to staff. 3 messages a minute and 20 an hour per player. Housekeeping, Guestbook lists the newest messages of
every profile with a delete button (audited as `ext.guestbook.delete`).
