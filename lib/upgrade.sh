# shellcheck shell=bash
# Moves the checkout to a released version: backup, checkout, build, migrate, start. Never restores by itself.

UPGRADE_OK=""

git_root() { git -C "$ROOT" "$@"; }

upgrade_rollback_help() { # where the checkout was, backup file
  [ -z "$UPGRADE_OK" ] || return 0
  {
    echo
    echo "The upgrade did not finish. Nothing is restored by itself; to go back to what ran before:"
    echo "  ./zel stop"
    echo "  git checkout $1"
    echo "  ./zel restore $(printf %q "$2")"
    echo "  ./zel start"
  } >&2
}

upgrade() { # target tag or empty, 1 when --yes was given
  local target=${1:-} yes=${2:-} current=v$VERSION before answer free need public=""
  have git || fail "upgrade needs git"
  git_root rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "$ROOT is not a git checkout, so there is nothing to upgrade from"
  [ -z "$(git_root status --porcelain --untracked-files=no)" ] \
    || fail "tracked files are changed; commit them or put them back first (git status). Own settings belong in the *.local files"
  doctor_or_fail
  need=$(du -sk "$DATA/mariadb" 2>/dev/null | awk '{ print $1 }')
  free=$(df -Pk "$DATA" | awk 'NR == 2 { print $4 }')
  [ "${free:-0}" -gt "${need:-0}" ] || fail "not enough room for a backup: $(( free / 1024 )) MB free, the database is $(( need / 1024 )) MB"

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
  if [ -z "$yes" ]; then
    [ -t 0 ] || fail "run the upgrade on a terminal, or add --yes"
    printf 'Stop the game, back it up and upgrade? [y/N] '
    read -r answer
    case $answer in y|Y|yes) ;; *) fail "nothing changed" ;; esac
  fi

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
  [ "v$VERSION" = "$target" ] || fail "the checkout says version $VERSION, not $target"
  if [ "${ZEL_BUILD:-auto}" != source ] && [ ! -f "$(release_jar)" ]; then
    step "fetching the server $VERSION"
    release_fetch || note "no release jar; the server is built here"
  fi
  rm -f "$DATA/web-packages"
  start_game "$public"
  running=$(process_version server)
  [ "$running" = "$VERSION" ] || fail "the server that runs says version '${running:-?}', not $VERSION"
  UPGRADE_OK=1
  echo "upgraded to $target; the backup from before is $backup"
}
