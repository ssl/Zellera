# shellcheck shell=bash
# Dumps of the game's database in .data/backups, and loading one back.

# Sets BACKUP_FILE. The dump is one consistent snapshot, so the game may run meanwhile.
backup_make() {
  local dir=$DATA/backups started="" old
  have mariadb-dump || fail "mariadb-dump is missing: install the MariaDB client tools"
  db_made || fail "there is no database yet, so nothing to back up"
  if ! process_alive db; then db_up; started=1; fi
  [ -n "$(sql -N -e "SHOW DATABASES LIKE $(sql_text "$ZEL_DB_NAME")")" ] || fail "there is no database $ZEL_DB_NAME to back up"
  mkdir -p -m 700 "$dir"
  BACKUP_FILE=$dir/$ZEL_DB_NAME-$VERSION-$(date +%Y%m%d-%H%M%S).sql.gz
  (umask 077; set -o pipefail
    mariadb-dump --no-defaults -S "$ZEL_DB_SOCKET" -u "$OS_USER" --single-transaction --routines --triggers \
      "$ZEL_DB_NAME" | gzip > "$BACKUP_FILE.part") || { rm -f "$BACKUP_FILE.part"; fail "the backup failed"; }
  mv "$BACKUP_FILE.part" "$BACKUP_FILE"
  # Only the backups made here are counted; a file with another name stays.
  # shellcheck disable=SC2012
  ls -t "$dir/$ZEL_DB_NAME"-[0-9]*-????????-??????.sql.gz 2>/dev/null | tail -n "+$(( BACKUP_KEEP + 1 ))" \
    | while IFS= read -r old; do rm -f "$old"; done
  [ -z "$started" ] || stop_or_fail db
  note "backup: $BACKUP_FILE"
}

# Replaces the database with a backup. The server and the web stop first, and what is there now is backed up.
backup_restore() { # file, 1 when --yes was given
  local file=$1 word=""
  [ -f "$file" ] || fail "no such backup: $file"
  gzip -t "$file" 2>/dev/null || fail "$file is not a gzip file"
  if [ -z "${2:-}" ]; then
    [ -t 0 ] || fail "restore replaces the whole database: run it on a terminal, or add --yes"
    printf 'This replaces the database %s with %s.\nType restore to go on: ' "$ZEL_DB_NAME" "$file"
    read -r word
    [ "$word" = restore ] || fail "nothing changed"
  fi
  stop_or_fail web
  stop_or_fail server
  db_secrets
  db_up
  if [ -n "$(sql -N -e "SHOW TABLES IN \`$ZEL_DB_NAME\`" 2>/dev/null)" ]; then
    step "backing up what is there now"
    backup_make
  fi
  step "restoring $file"
  sql -e "DROP DATABASE IF EXISTS \`$ZEL_DB_NAME\`"
  db_accounts | sql
  gunzip -c "$file" | sql "$ZEL_DB_NAME" || fail "the restore failed; the backup made just before it is $BACKUP_FILE"
  note "restored; ./zel start brings the game back (and applies the migrations this version has)"
}
