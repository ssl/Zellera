# shellcheck shell=bash
# Moves the game to a released version: backup, the new files (a git checkout, or a bundle's files), migrate, start.
# Never restores by itself.

UPGRADE_OK=""

git_root() { git -C "$ROOT" "$@"; }

upgrade_way_back() { # where the checkout was, or the bundle's file backup; database backup or empty
  echo "  ./zel stop"
  if is_bundle; then
    echo "  xargs rm -f < $(printf %q "$1/added")"
    echo "  cp -Rp $(printf %q "$1/files")/. ."
  else
    echo "  git checkout $1"
  fi
  [ -z "$2" ] || echo "  ./zel restore $(printf %q "$2")"
  echo "  ./zel start"
}

upgrade_rollback_help() { # as upgrade_way_back
  [ -z "$UPGRADE_OK" ] || return 0
  {
    echo
    echo "The upgrade did not finish. Nothing is restored by itself; to go back to what ran before:"
    upgrade_way_back "$@"
  } >&2
}

# Before anything stops: this computer is ready and has room for a backup.
upgrade_ready() {
  local free need
  doctor_or_fail
  need=$(du -sk "$DATA/mariadb" 2>/dev/null | awk '{ print $1 }')
  free=$(df -Pk "$DATA" | awk 'NR == 2 { print $4 }')
  [ "${free:-0}" -gt "${need:-0}" ] || fail "not enough room for a backup: $(( free / 1024 )) MB free, the database is $(( need / 1024 )) MB"
}

upgrade_ask() {
  local answer
  [ -t 0 ] || fail "run the upgrade on a terminal, or add --yes"
  printf 'Stop the game, back it up and upgrade? [y/N] '
  read -r answer
  case $answer in y|Y|yes) ;; *) fail "nothing changed" ;; esac
}

upgrade() { # target tag or empty, 1 when --yes was given
  local target=${1:-} yes=${2:-} current=v$VERSION before public=""
  have git || fail "upgrade needs git"
  git_root rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "$ROOT is not a git checkout, so there is nothing to upgrade from"
  [ -z "$(git_root status --porcelain --untracked-files=no)" ] \
    || fail "tracked files are changed; commit them or put them back first (git status). Own settings belong in the *.local files"
  upgrade_ready

  step "looking for versions"
  git_root fetch --tags --quiet || fail "git fetch failed"
  [ -n "$target" ] || target=$(git_root tag -l 'v[0-9]*' --sort=-v:refname | head -1)
  [ -n "$target" ] || fail "no released version (a v* tag) is known"
  git_root rev-parse -q --verify "refs/tags/$target" >/dev/null || fail "no such version: $target"
  [ "$target" != "$current" ] || { echo "already at $current"; UPGRADE_OK=1; return 0; }
  if git_root rev-parse -q --verify "refs/tags/$current" >/dev/null; then
    git_root merge-base --is-ancestor "$current" "$target" \
      || fail "$target is not newer than $current; going back is a restore of a backup, not an upgrade (docs/UPGRADING.md)"
  fi
  echo "upgrade $current -> $target"
  [ -n "$yes" ] || upgrade_ask

  before=$(git_root symbolic-ref -q --short HEAD || git_root rev-parse HEAD)
  [ ! -f "$RUN/public" ] || public=1
  db_secrets
  stop_game
  step "backing up"
  backup_make
  stop_or_fail db
  step "checking out $target"
  git_root checkout --quiet --detach "$target" || fail "git checkout $target failed; nothing else changed, ./zel start brings the game back"
  # The rest runs as the new version's own code.
  exec "$ROOT/zel" _upgrade-finish "$before" "$BACKUP_FILE" "$target" "$public"
}

upgrade_finish() { # where the checkout was, backup file, target tag, 1 when the game was public
  local before=$1 backup=$2 target=$3 public=${4:-} running
  # shellcheck disable=SC2064
  trap "upgrade_rollback_help $(printf %q "$before") $(printf %q "$backup"); lock_release" EXIT
  [ "v$VERSION" = "$target" ] || fail "the folder says version $VERSION, not $target"
  if ! is_bundle && [ "${ZEL_BUILD:-auto}" != source ] && [ ! -f "$(release_jar)" ]; then
    step "fetching the server $VERSION"
    release_fetch || note "no release jar; the server is built here"
  fi
  rm -f "$DATA/web-packages"
  start_game "$public"
  running=$(process_version server)
  [ "$running" = "$VERSION" ] || fail "the server that runs says version '${running:-?}', not $VERSION"
  UPGRADE_OK=1
  if is_bundle; then
    echo "upgraded to $target; to go back to what ran before:"
    upgrade_way_back "$before" "$backup"
  else
    echo "upgraded to $target; the backup from before is $backup"
  fi
}

# A bundle's MANIFEST: "<sha256>  <path>" for every file the bundle owns.
manifest_paths() { sed 's/^[0-9a-f]\{64\}  //' "$@"; }

# The MANIFEST lines of the paths on stdin, relative to the current folder.
file_sums() {
  node -e '
    const { createHash } = require("node:crypto");
    const fs = require("node:fs");
    for (const p of fs.readFileSync(0, "utf8").split("\n").filter(Boolean))
      process.stdout.write(`${createHash("sha256").update(fs.readFileSync(p)).digest("hex")}  ${p}\n`);
  '
}

version_newer() { # a, b: whether x.y.z a is newer than b
  local a b i
  IFS=. read -ra a <<< "$1"
  IFS=. read -ra b <<< "$2"
  for i in 0 1 2; do
    [ "${a[i]}" = "${b[i]}" ] || { [ "${a[i]}" -gt "${b[i]}" ]; return; }
  done
  return 1
}

release_newest() {
  node -e '
    fetch(process.argv[1], { redirect: "follow" }).then((r) => {
      const tag = /\/tag\/(v[0-9]+\.[0-9]+\.[0-9]+)$/.exec(r.url);
      if (!tag) process.exit(1);
      console.log(tag[1]);
    }, () => process.exit(1));
  ' "$(dirname "$(release_url)")/latest"
}

# What a new bundle does to each file, one "<action> <path>" line: add, update or remove, with a ! when the file here
# is neither the old bundle's nor the new one's, and kept for a file changed here that the new bundle leaves as it was.
upgrade_plan() { # old MANIFEST, new MANIFEST, sums of those files as they are here
  awk '
    FILENAME == ARGV[1] { old[$2] = $1; next }
    FILENAME == ARGV[2] { new[$2] = $1; order[++n] = $2; next }
    { here[$2] = $1 }
    END {
      for (i = 1; i <= n; i++) {
        p = order[i]; h = (p in here) ? here[p] : ""
        if (h == new[p]) continue
        if (!(p in old)) print (h == "" ? "add" : "add!"), p
        else if (h == "" || h == old[p]) print "update", p
        else if (new[p] == old[p]) print "kept", p
        else print "update!", p
      }
      for (p in old) if (!(p in new) && (p in here)) print (here[p] == old[p] ? "remove" : "remove!"), p
    }' "$1" "$2" "$3" | sort -k2
}

upgrade_bundle() { # target tag or empty, bundle zip or empty, 1 when --yes was given, 1 when --force was given
  local target=$1 zip=$2 yes=$3 force=$4 work=$DATA/upgrade new version backup files public="" action path
  have unzip || fail "upgrade needs unzip"
  upgrade_ready
  rm -rf "$work"
  mkdir -p "$work"
  # shellcheck disable=SC2064
  trap "rm -rf $(printf %q "$work"); lock_release" EXIT
  if [ -z "$zip" ]; then
    step "looking for versions"
    [ -n "$target" ] || target=$(release_newest) \
      || fail "could not learn the newest version from $(release_url); name one with --to, or give a bundle zip"
    [ "$target" != "v$VERSION" ] || { echo "already at v$VERSION"; UPGRADE_OK=1; return 0; }
    zip=$work/zellera-${target#v}.zip
    step "fetching zellera-${target#v}.zip"
    release_download "$(release_url)/$target/zellera-${target#v}.zip" "$zip" \
      || fail "no bundle for $target at $(release_url); one you have: ./zel upgrade <zellera-x.y.z.zip>"
  fi
  [ -f "$zip" ] || fail "no such bundle: $zip"
  unzip -q "$zip" -d "$work/new" || fail "$zip could not be unpacked"
  version=$(cat "$work"/new/zellera-*/VERSION 2>/dev/null | tr -d '[:space:]')
  new=$work/new/zellera-$version
  [[ $version =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] && [ -f "$new/MANIFEST" ] \
    || fail "$zip is not a Zellera bundle (it has no zellera-<version>/ with VERSION and MANIFEST)"
  [ -z "$target" ] || [ "$target" = "v$version" ] || fail "$zip holds version $version, not $target"
  [ "$version" != "$VERSION" ] || { echo "already at v$VERSION"; UPGRADE_OK=1; return 0; }
  version_newer "$version" "$VERSION" \
    || fail "$version is not newer than $VERSION; going back is a restore of a backup, not an upgrade (docs/UPGRADING.md)"
  (cd "$new" && manifest_paths MANIFEST | file_sums) 2>/dev/null | cmp -s - "$new/MANIFEST" \
    || fail "the files in $zip do not match its MANIFEST"

  (cd "$ROOT" && manifest_paths MANIFEST "$new/MANIFEST" | sort -u \
    | while IFS= read -r path; do [ ! -f "$path" ] || echo "$path"; done | file_sums) > "$work/here"
  upgrade_plan "$ROOT/MANIFEST" "$new/MANIFEST" "$work/here" > "$work/plan"

  echo "upgrade v$VERSION -> v$version:"
  while read -r action path; do
    case $action in
      add) note "new      $path" ;;
      add!) note "new      $path (a file here is in its place)" ;;
      update) note "replaced $path" ;;
      update!) note "replaced $path (changed here)" ;;
      remove) note "removed  $path" ;;
      remove!) note "removed  $path (changed here)" ;;
      kept) note "kept     $path (changed here, the same in v$version)" ;;
    esac
  done < "$work/plan"
  if grep -q '^[a-z]*! ' "$work/plan" && [ -z "$force" ]; then
    {
      echo "These files differ here from the bundle's, and the upgrade would replace or remove them:"
      sed -n 's/^[a-z]*! /  /p' "$work/plan"
      echo "Put your changes in custom/ or a *.local file, or add --force: the backup keeps your version."
    } >&2
    fail "nothing changed"
  fi
  [ -n "$yes" ] || upgrade_ask

  [ ! -f "$RUN/public" ] || public=1
  db_secrets
  stop_game
  step "backing up"
  BACKUP_FILE=""
  if db_made; then backup_make; else note "no database yet, so none to back up"; fi
  stop_or_fail db
  backup=$DATA/backups/bundle-$VERSION-$(date +%Y%m%d-%H%M%S)
  files=$backup/files
  mkdir -p -m 700 "$files"
  (cd "$ROOT" && { echo MANIFEST; manifest_paths MANIFEST; sed -n 's/^add! //p' "$work/plan"; } \
    | while IFS= read -r path; do [ ! -f "$path" ] || echo "$path"; done | tar -cf - -T - | tar -xpf - -C "$files") \
    || fail "the backup of the bundle's files failed; nothing changed, ./zel start brings the game back"
  sed -n 's/^add!* //p' "$work/plan" > "$backup/added"
  note "files: $backup"

  # shellcheck disable=SC2064
  trap "rm -rf $(printf %q "$work"); upgrade_rollback_help $(printf %q "$backup") $(printf %q "$BACKUP_FILE"); lock_release" EXIT
  step "putting v$version in place"
  while read -r action path; do
    case $action in
      add*|update*)
        mkdir -p "$ROOT/$(dirname "$path")"
        # A new file, not a rewrite, so the zel that runs now keeps reading its own.
        cp -p "$new/$path" "$ROOT/$path.upgrade" && mv -f "$ROOT/$path.upgrade" "$ROOT/$path" || fail "could not write $path" ;;
      remove*) rm -f "$ROOT/$path"; (cd "$ROOT" && rmdir -p "$(dirname "$path")" 2>/dev/null) || true ;;
    esac
  done < "$work/plan"
  cp -p "$new/MANIFEST" "$ROOT/MANIFEST.upgrade" && mv -f "$ROOT/MANIFEST.upgrade" "$ROOT/MANIFEST" || fail "could not write MANIFEST"
  rm -rf "$work"
  exec "$ROOT/zel" _upgrade-finish "$backup" "$BACKUP_FILE" "v$version" "$public"
}
