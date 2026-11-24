# Reaching the game

`ZEL_PROXY` in `config/zellera.local.env` says how players reach the game. Whatever the mode, the game has one
address, and `./zel start` prints it.

| `ZEL_PROXY` | Players reach it | Runs | Needs |
|---|---|---|---|
| `local` (default) | on this computer, `https://zellera.localhost:8444` | Caddy on loopback, with its own certificate authority | Caddy |
| `domain` | at `https://$ZEL_DOMAIN` | Caddy on ports 80 and 443, with a certificate it gets and renews itself | Caddy, a DNS record, ports 80 and 443 open |
| `tunnel` | through a Cloudflare Tunnel | Caddy on loopback and `cloudflared` | Caddy, `cloudflared` |
| `none` | through your own proxy | nothing in front | `ZEL_DOMAIN`, your proxy |

Restart after a change: `./zel restart`. `./zel doctor` checks what the chosen mode needs.

## local

Nothing to set. The browser warns about the certificate until this computer trusts the game's own authority once;
`./zel trust` prints the command. `./zel start --public` adds a quick tunnel for as long as the game runs: a later
`./zel start` keeps it, `./zel stop` ends it.

## domain

```
ZEL_PROXY=domain
ZEL_DOMAIN=game.example.org
ZEL_ACME_EMAIL=you@example.org     # optional
ZEL_BIND=203.0.113.7               # optional: one address instead of all
```

Point the domain's DNS record at this computer and open ports 80 and 443. On Linux an ordinary user may not open
ports below 1024; allow Caddy once: `sudo setcap cap_net_bind_service=+ep "$(command -v caddy)"`.

## tunnel

A quick tunnel needs no account and gets a new `trycloudflare.com` address on each start:

```
ZEL_PROXY=tunnel
```

A named tunnel keeps your own domain. Once, with the domain on your Cloudflare account:

```
cloudflared tunnel login
cloudflared tunnel create zellera
cloudflared tunnel route dns zellera game.example.org
```

```
ZEL_PROXY=tunnel
ZEL_TUNNEL_MODE=named
ZEL_TUNNEL=zellera
ZEL_DOMAIN=game.example.org
```

In Cloudflare, leave WebSockets on and turn Rocket Loader and Bot Fight Mode off: both break the game. A
`~/.cloudflared/config.yml` stops quick tunnels; move it away or use a named tunnel.

## none

```
ZEL_PROXY=none
ZEL_DOMAIN=game.example.org
```

Only the database, the server and the web start. Send the site to them yourself, over HTTPS:

| Path | Upstream |
|---|---|
| `/websocket` | `http://127.0.0.1:$ZEL_WS_PORT`, as a WebSocket upgrade, with a read timeout of an hour or more |
| everything else | `http://127.0.0.1:$ZEL_WEB_PORT` |

The proxy must pass the `Host` header on and put the player's address in `X-Forwarded-For`, replacing whatever the
request brought: the game takes the header's word only from the proxies in `ZEL_TRUSTED_PROXIES` (default
`127.0.0.1`), and a header that players can write themselves lets them pick any address. `ZEL_CLIENT_IP_HEADER`
names another header, `CF-Connecting-IP` for one. For a proxy on another computer or in a container, set `ZEL_BIND`
to the address the web and the server listen on, and keep both ports closed to everyone but the proxy.

Ready-made examples: [proxy-nginx.conf](proxy-nginx.conf) and [proxy-traefik.yml](proxy-traefik.yml).

## Your own additions to Caddy

`config/Caddyfile.local`, when it is there, is imported at the end of the Caddy configuration: other sites and
snippets go there. It is not in git, so an upgrade leaves it alone. The game's own sites are
`config/caddy/<mode>.caddy`.
