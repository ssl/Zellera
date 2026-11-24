# shellcheck shell=bash
# The game's own MariaDB in .data/mariadb: its three accounts and their rights.

DB_APP_USER=zellera_app
DB_MIGRATE_USER=zellera_migrate
DB_WEB_USER=zellera_web

# This user owns the data folder and logs in over the socket without a password.
sql() { mariadb --no-defaults -S "$ZEL_DB_SOCKET" -u "$OS_USER" "$@"; }
db_ping() { mariadb-admin --no-defaults -S "$ZEL_DB_SOCKET" -u "$OS_USER" ping >/dev/null 2>&1; }
db_made() { [ -d "$DATA/mariadb/mysql" ]; }
sql_text() { local s=${1//\\/\\\\}; s=${s//\'/\'\'}; printf "'%s'" "$s"; }

# The passwords stay shell variables: a process gets its own through process_env, nothing gets them by export.
db_secrets() {
  local file=$ZEL_SECRETS_DIR/db.env k
  if [ ! -f "$file" ]; then
    (umask 077; for k in APP MIGRATE WEB; do
      echo "ZEL_DB_${k}_PASS=$(od -An -N16 -tx1 /dev/urandom | tr -d ' \n')"
    done > "$file")
  fi
  DB_APP_PASS=$(sed -n 's/^ZEL_DB_APP_PASS=//p' "$file")
  DB_MIGRATE_PASS=$(sed -n 's/^ZEL_DB_MIGRATE_PASS=//p' "$file")
  DB_WEB_PASS=$(sed -n 's/^ZEL_DB_WEB_PASS=//p' "$file")
  [ -n "$DB_APP_PASS" ] && [ -n "$DB_MIGRATE_PASS" ] && [ -n "$DB_WEB_PASS" ] \
    || fail "$file lacks a password (ZEL_DB_APP_PASS, ZEL_DB_MIGRATE_PASS, ZEL_DB_WEB_PASS)"
}

# The schema owner (migrations and imports), the server's data-only account and the web's account.
db_accounts() {
  local h k user pass
  echo "CREATE DATABASE IF NOT EXISTS \`$ZEL_DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
  for h in 127.0.0.1 localhost; do
    for k in MIGRATE APP WEB; do
      user=DB_${k}_USER pass=DB_${k}_PASS
      echo "CREATE USER IF NOT EXISTS '${!user}'@'$h' IDENTIFIED BY $(sql_text "${!pass}");"
      echo "ALTER USER '${!user}'@'$h' IDENTIFIED BY $(sql_text "${!pass}");"
    done
    echo "GRANT ALL PRIVILEGES ON \`$ZEL_DB_NAME\`.* TO '$DB_MIGRATE_USER'@'$h';"
    echo "GRANT SELECT, INSERT, UPDATE, DELETE, CREATE TEMPORARY TABLES, LOCK TABLES, EXECUTE, SHOW VIEW ON \`$ZEL_DB_NAME\`.* TO '$DB_APP_USER'@'$h';"
  done
  echo "FLUSH PRIVILEGES;"
}

# Table and column rights, after the migrations: every one the server and the web hold is revoked, then the web gets
# what its pages, API and extensions declare, so a right nothing declares any more is gone.
db_table_revokes() { # user
  sql -N -e "SELECT CONCAT('REVOKE ALL PRIVILEGES ON \`', Db, '\`.\`', Table_name, '\` FROM ''', User, '''@''', Host, ''';')
    FROM mysql.tables_priv WHERE Db = $(sql_text "$ZEL_DB_NAME") AND User = $(sql_text "$1")"
}
db_grants() {
  local grants
  grants=$(db_table_revokes "$DB_APP_USER" && db_table_revokes "$DB_WEB_USER" \
    && node "$ROOT/web/grants.mjs" --db "$ZEL_DB_NAME" --user "$DB_WEB_USER") \
    || fail "web/grants.mjs refused the web's rights (see above)"
  printf '%s\n' "$grants" | sql
}

db_up() {
  if ! process_alive db; then
    ! db_ping || fail "a database that this command did not start answers on $ZEL_DB_SOCKET; stop it first"
    if ! db_made; then
      step "making the database"
      mariadb-install-db --no-defaults "--datadir=$DATA/mariadb" --auth-root-authentication-method=socket \
        --skip-test-db >/dev/null || fail "mariadb-install-db failed"
    fi
    process_start db
  fi
  process_wait db 60
  db_accounts | sql
}
