# Themes

The web's pages are a theme: a folder of static HTML, CSS, JavaScript and images. The server never writes data into
a page; each page loads its data in the browser from the site API ([SITE-API.md](SITE-API.md)) or the housekeeping API
([HK-API.md](HK-API.md)). A new look is a new theme folder; the APIs stay the same.

## Choosing a theme

`ZEL_WEB_THEME` (in `config/zellera.local.env`) names a folder under `custom/themes/` or `web/themes/`, or an
absolute path; unset means `default`. Files and route entries your theme lacks come from `default`, so a theme may
replace one stylesheet or every page. Restart the web after changing the setting; theme files are read on every
request.

Keep your own theme in `custom/themes/<name>/` (next to `zel`): that folder is not in git, so an upgrade never
touches it, and a theme there wins over one of the same name in `web/themes/`.

## Pages: one layout and a route table

Every page is the same empty frame with its own title, stylesheets, scripts and tab. A theme has one layout,
`page.html`, and a route table, `theme.json`; the server fills each entry's static fields into the layout:

```json
{
  "layout": "page.html",
  "needs": [],
  "areas": {
    "site": { "styles": ["css/site.css"], "scripts": ["js/site.js"] },
    "hk": { "styles": ["css/site.css", "css/hk.css"], "scripts": ["js/hk.js"] }
  },
  "pages": {
    "me": { "title": "My page", "scripts": ["js/pages/me.js"], "tab": "home", "sub": "me" },
    "profile": { "title": "Profile", "styles": ["css/community.css"], "scripts": ["js/pages/profile.js"], "tab": "community", "sub": "profile", "slots": ["profile"] },
    "hk/users": { "title": "Users", "scripts": ["js/hk/user-pages.js", "js/hk/users.js"], "tab": "users" }
  }
}
```

- A page's name is the one in the table below (`hk/...` for housekeeping). Its stylesheets and scripts are its area's
  (`site`, or `hk` for a name starting with `hk/`) followed by its own; paths are theme paths (`/theme/<path>`).
- The layout holds four fields: `{{title}}` (the entry's title), `{{styles}}` (a `<link>` per stylesheet),
  `{{scripts}}` (a deferred `<script>` per script) and `{{body}}` (the `<body>` attributes `data-tab`, `data-sub` and
  `data-title`, which the frame script reads). Nothing else is ever written into a page: no data, no name of the game.
- `slots` lists the places a page offers the web extensions ([EXTENSIONS.md](EXTENSIONS.md)): while an extension with a
  part for that slot is on, its stylesheets and scripts follow the page's own.
- `needs` lists the web extensions the theme cannot do without; the web says so at start when one is not on.
- Your theme's `theme.json` goes over the default's: its `layout`, each area and each page entry replace the
  default's as a whole; what it leaves out comes from `default`. A theme without a `theme.json` uses the default's.
- `"inherit": false` stops the fallback for pages: a site page the theme does not have itself (its own `<page>.html` or
  route entry) sends the visitor to `/` instead of showing the default's, and so does an address that is no page
  (there is no `404` page then unless the theme has one). Housekeeping pages (`hk/...`) still come from the
  default. Files such as styles, scripts and images still fall back to the default's.
- A theme may still ship its own `<page>.html` (e.g. `profile.html`) for one page: that file wins over the layout and
  is sent as it is (extension parts included only if the file names them itself).
- At start the web checks that every page address has a page and that every file the route table names exists, and
  stops with the list when not.

## What the server serves

| Address | File |
| --- | --- |
| `/theme/<path>` | `<theme>/<path>`: CSS, JS, images, fonts (never `.html`, never dot files) |
| a page address (below) | its page (the layout and its route entry, or the theme's own `<page>.html`), with the site's headers: `content-security-policy` (`default-src 'self'`, no inline script or style, no `data:` images), `x-frame-options: DENY`, `cache-control: no-store` |
| `/theme/ext/<name>/<path>` | a web extension's theme part: the theme's own `ext/<name>/<path>` first, else the extension's `theme/<path>` |
| an address that is no page | the page `404`, status 404 (also for a non-staff visitor at a housekeeping address) |

The page addresses and their page names are fixed (`web/cms/shells.mjs`); web extensions add theirs under
`/ext/<name>` and `/housekeeping/ext/<name>`. Before sending a page the server only redirects:

- `/` → 302 `/me` when logged in; the pages marked *user* → 302 `/` when logged out;
- every page but `/` → 302 `/` when the session cookie was ended by a ban (the front page shows `banNotice` from `GET /site`);
- the pages of a closed area (the `ZEL_WEB_PUBLIC_*` switches) → 302 `/`
  for a logged-out visitor; the staff page while `ZEL_WEB_STAFF_PAGE=0` → the page `404`, status 404;
- a housekeeping page answers 401 (logged out), 403 (banned, or the rank lacks the page's key) or 200 with the same
  page, so the page can show the API's reason; a page that writes an audit row, opened from a link on another site,
  gets the page `hk/confirm` instead (a page that asks first and links to itself).

| Page address | Page | |
| --- | --- | --- |
| `/` | `index` | log in, register (`#register`) |
| `/me` | `me` | user |
| `/community/players?name=` | `search` | user |
| `/settings` | `settings` | user |
| `/avatars` | `avatars` | logged in to an account (several avatars per account; the page sends others away) |
| `/profile/<name>` | `profile` | players |
| `/profile/<name>/photo/<id>` | `photo` | photos |
| `/photos` | `photos` | photos |
| `/groups/<id>/id` | `group` | players |
| `/room/<id>` | `room` | players |
| `/community/staff` | `staff` | community, staff page |
| `/news`, `/news/<id>` | `news`, `article` | community |
| `/help`, `/help/<topic>` | `help`, `help-topic` | |
| `/help/ask` | `ask` | (says itself when it needs a login or is off) |
| `/safety`, `/legal/terms`, `/legal/privacy` | `safety`, `legal` | |
| `/housekeeping/...` | `hk/...` | staff, see `shells.mjs` for each page's key |

Other addresses stay server redirects (`/rd/<id>`, `/home/<name>`, `/profile`, `/groups/<id>`, `/shop`, `/credits`, the
mod tool's `/housekeeping/users/lookup?name=`), and `/play` is the game itself: it takes the ticket as the form field
`sso` of a POST (what the default theme sends, so no ticket shows in an address) or as `?sso=`. Before the game it shows
a short black page of the web's own, not the theme's, that measures the device and posts the ticket on; a theme has
nothing to do for it.

A page may also be a file of its own: `<page>.html` in the theme folder is sent as it is, without the layout. The `play`
theme is one such page, `index.html`, with `"inherit": false`: a flashing logo on black while it makes a guest account
for a visitor without a session (`POST /guest`) and opens the game. Its `me.html` is the account page: a password
(`POST /me/password`) and switching to another account (`POST /login`). A theme with its own `index.html` decides itself
what a logged-in visitor sees at `/`; the default theme's front page sends them to `/me`.

## Writing a theme

- A page reads what it shows from the API with `fetch` (`credentials: 'same-origin'`), and writes with a JSON
  `POST` from the page itself. There is no token to fetch or send: the session is an HttpOnly cookie the script never
  sees ([SITE-API.md, Security](SITE-API.md#security)).
- Build the page with DOM calls (`textContent`, `createElement`), never by putting API text into `innerHTML`:
  names, mottos, chat lines and news are written by players and staff. The one exception is a field the API names
  `...Html` (the game's own help and legal texts), which is written by this site and safe to insert.
- Every page starts with `GET /api/site/v1/site` (housekeeping: `GET /api/hk/v1/me`) for the game's name, who is
  looking and what they may open (`sees`), and draws its own header and navigation from it: no menu entry or link
  for an area `sees` closes.
- The site's own texts name the game's world with `place` from `GET /site` ("Enter the game", `place.plural` for
  more than one), never a fixed word: the content pack sets it (settings row `site.place.word`, else *game*).
- Players are drawn with the avatar imager only while `avatars` (`GET /site`, housekeeping's `GET /me`) is true;
  false means initials (or another picture that is no figure) on every page.
- The Web API docs (`/api/public/api-docs/`) are rendered by the server and load `css/site.css`
  and `js/site.js` from the theme; `Site.start` keeps the page's own `<main>` and draws the frame around it, with
  `<body data-tab="help" data-sub="api" data-frame="wide">`.
- Link to the page addresses above; the API's answers carry ids and names, not page links.

## The default theme

`web/themes/default/`: the layout (`page.html`) and route table (`theme.json`), stylesheets (`css/site.css`, plus
`css/hk.css` for housekeeping), a shared script for the site (`js/site.js`) and for housekeeping (`js/hk.js`), and a
small script per page (`js/pages/*.js`, `js/hk/*.js`). The layout is a centred fixed-width page with the game's name
and an *Enter the game* button in the header, tabs joined to a band of sub-tabs, and two or three columns of rounded
panels; below 880 px the columns stack. `Site.avatar` draws a player's figure, or initials while `avatars` is off.
Colours are tokens at the top of `site.css` and meet WCAG AA.

To start a theme, copy `default` to `themes/<name>`, change what you like, delete what you did not change (it comes
from `default`; in `theme.json` keep only the entries you changed), and set `ZEL_WEB_THEME=<name>`. Server features a
theme cannot bring (routes, tables) are web extensions: [EXTENSIONS.md](EXTENSIONS.md).
