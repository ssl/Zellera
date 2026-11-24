# Upgrading

```
./zel upgrade                # to the newest released version
./zel upgrade --to v1.2.0    # to that one
```

The folder must be a git checkout. The command:

1. checks that no tracked file is changed, that this computer is ready (`./zel doctor`) and that there is room for a
   backup;
2. fetches the tags and asks (`--yes` skips the question);
3. stops the game and dumps the database into `.data/backups/`;
4. checks out the version's tag;
5. takes the release's server jar when there is one and it matches the release's `.sha256`, else builds the server
   here (`ZEL_BUILD=source` always builds, `ZEL_BUILD=release` never does);
6. installs the web's packages, applies the database migrations and starts the game;
7. checks that the server that answers is the new version.

## When it fails

Nothing is restored by itself. The command prints the four commands that bring back what ran before, with the
backup it just made:

```
./zel stop
git checkout <where you were>
./zel restore .data/backups/<name>-<version>-<time>.sql.gz
./zel start
```

Going back to an older version is always this: a checkout of the older version plus the backup made before the
upgrade. A newer database does not run under an older version.

## What an upgrade leaves alone

These are not in git, so `git checkout` never touches them:

| Yours | What |
|---|---|
| `config/zellera.local.env` | your settings, over `config/zellera.env` |
| `config/game-rules.local.conf` | your game rules, over `config/game-rules.conf`: only the lines you change |
| `config/Caddyfile.local` | your own additions to Caddy ([PROXY.md](PROXY.md)) |
| `custom/` | your own themes and extensions |
| `client/`, `secrets/`, `.data/` | the client, the passwords and keys, the database, backups, logs and files |

A theme or extension of your own goes in `custom/` and is named by its full path:

```
ZEL_WEB_THEME=/home/you/zellera/custom/themes/mine
ZEL_WEB_EXTENSIONS=guestbook,/home/you/zellera/custom/extensions/mine
```

Change a file that is in git (a theme under `web/themes/`, `config/zellera.env`) and the upgrade stops at step 1;
copy it to its place above instead.

## Backups

`./zel backup` dumps the database while the game runs; the newest `ZEL_BACKUP_KEEP` (10) are kept, and `./zel import`
(once a game has content) and `./zel upgrade` make one first. A backup holds the database, not the files beside it: copy
`secrets/` and the folders `ZEL_DATA_DIR` and `ZEL_ASSET_DIR` name yourself. `./zel restore <file>` stops the server and
the web, backs up what is there, and loads the file.
