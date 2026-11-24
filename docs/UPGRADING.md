# Upgrading

```
./zel upgrade                       # to the newest released version
./zel upgrade --to v1.2.0           # to that one
./zel upgrade zellera-1.2.0.zip     # to the bundle you downloaded
```

The same command works in a release bundle and in a git checkout; how it gets the new version differs.

## A release bundle

`MANIFEST` in the bundle lists every file the bundle owns with its sha256. The command:

1. checks that this computer is ready (`./zel doctor`) and that there is room for a backup;
2. downloads `zellera-<version>.zip` from the releases and checks it against its `.sha256`, or takes the zip you name
   (for a machine without internet, or a copy you were given), and checks its files against its `MANIFEST`;
3. lists what changes: new files, replaced files and removed files;
4. stops when a file it would replace or remove was changed here, and names the files: put your changes in `custom/`
   or a `*.local` file, or add `--force`, which replaces them and keeps your version in the backup. A changed file
   the new version leaves as it was stays as you changed it;
5. asks (`--yes` skips the question), stops the game and dumps the database into `.data/backups/`;
6. copies the bundle's files as they are now into `.data/backups/bundle-<version>-<time>/`;
7. replaces the files the old `MANIFEST` owned, adds the new ones and removes the ones the new version no longer has;
8. applies the database migrations, starts the game and checks that the server that answers is the new version;
9. prints the way back.

## A git checkout

The command:

1. checks that no tracked file is changed, that this computer is ready and that there is room for a backup;
2. fetches the tags and asks (`--yes` skips the question);
3. stops the game and dumps the database into `.data/backups/`;
4. checks out the version's tag;
5. takes the release's server jar when there is one and it matches the release's `.sha256`, else builds the server
   here (`ZEL_BUILD=source` always builds, `ZEL_BUILD=release` never does);
6. installs the web's packages, applies the database migrations and starts the game;
7. checks that the server that answers is the new version.

## When it fails

Nothing is restored by itself. The command prints the commands that bring back what ran before, with the backups it
just made. In a bundle:

```
./zel stop
xargs rm -f < .data/backups/bundle-<version>-<time>/added
cp -Rp .data/backups/bundle-<version>-<time>/files/. .
./zel restore .data/backups/<name>-<version>-<time>.sql.gz
./zel start
```

In a git checkout:

```
./zel stop
git checkout <where you were>
./zel restore .data/backups/<name>-<version>-<time>.sql.gz
./zel start
```

Going back to an older version is always this: the older version's files plus the database backup made before the
upgrade. A newer database does not run under an older version.

## What an upgrade leaves alone

These are not the bundle's files and not in git, so an upgrade never touches them:

| Yours | What |
|---|---|
| `config/zellera.local.env` | your settings, over `config/zellera.env` |
| `config/game-rules.local.conf` | your game rules, over `config/game-rules.conf`: only the lines you change |
| `config/Caddyfile.local` | your own additions to Caddy ([PROXY.md](PROXY.md)) |
| `custom/` | your own themes, web extensions and game extensions |
| `client/`, `secrets/`, `.data/` | the client, the passwords and keys, the database, backups, logs and files |

A theme or extension of your own goes in `custom/` and is named by its folder:

```
ZEL_WEB_THEME=mine
ZEL_WEB_EXTENSIONS=guestbook,mine
```

Change one of the bundle's files (a theme under `web/themes/`, `config/zellera.env`) and a bundle upgrade stops at
step 4; a git checkout stops at step 1. Copy it to its place above instead.

## Backups

`./zel backup` dumps the database while the game runs; the newest `ZEL_BACKUP_KEEP` (10) are kept, and `./zel import`
(once a game has content) and `./zel upgrade` make one first. A backup holds the database, not the files beside it: copy
`secrets/` and the folders `ZEL_DATA_DIR` and `ZEL_ASSET_DIR` name yourself. `./zel restore <file>` stops the server and
the web, backs up what is there, and loads the file. The copies of a bundle's files that an upgrade makes stay in
`.data/backups/` until you remove them.
