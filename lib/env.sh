# shellcheck shell=bash
# Configuration, folders, the network mode and the lock; sourced by zel.

fail() { echo "zel: $*" >&2; exit 1; }
step() { echo "== $*"; }
note() { echo "   $*"; }
have() { command -v "$1" >/dev/null 2>&1; }
in_root() { case $1 in /*) printf '%s\n' "$1" ;; *) printf '%s\n' "$ROOT/$1" ;; esac; }
size_mb() { case $1 in *[gG]) echo $(( ${1%?} * 1024 )) ;; *) echo "${1%?}" ;; esac; }

load_env() {
  [ "$(id -u)" != 0 ] || fail "run as an ordinary user, not as root (the database and the game never need it)"
  # Debian keeps the MariaDB server binaries in /usr/sbin, which a user's PATH may lack.
  PATH=$PATH:/usr/sbin:/sbin
  export PATH
  OS_USER=$(id -un)
  [ -f "$ROOT/VERSION" ] || fail "no VERSION file in $ROOT"
  VERSION=$(cat "$ROOT/VERSION")
  VERSION=${VERSION//[[:space:]]/}

  set -a
  # shellcheck source=/dev/null
  . "$ROOT/config/zellera.env"
  # shellcheck source=/dev/null
  [ ! -f "$ROOT/config/zellera.local.env" ] || . "$ROOT/config/zellera.local.env"
  set +a
  export ZEL_ENV=${ZEL_ENV:-prod}
  export ZEL_ROOT=$ROOT

  DATA=$ROOT/.data
  RUN=$DATA/run
  LOGS=$DATA/logs
  BUILD_DIR=$DATA/build/server
  export ZEL_SECRETS_DIR; ZEL_SECRETS_DIR=$(in_root "${ZEL_SECRETS_DIR:-secrets}")
  export ZEL_CLIENT_DIR; ZEL_CLIENT_DIR=$(in_root "${ZEL_CLIENT_DIR:-client}")
  export ZEL_DATA_DIR; ZEL_DATA_DIR=$(in_root "${ZEL_DATA_DIR:-.data/run/data}")
  export ZEL_GAMEDATA_DIR; ZEL_GAMEDATA_DIR=$(in_root "${ZEL_GAMEDATA_DIR:-.data/gamedata}")
  export ZEL_ASSET_DIR; ZEL_ASSET_DIR=$(in_root "${ZEL_ASSET_DIR:-.data/assets}")
  export ZEL_GAME_RULES_FILE; ZEL_GAME_RULES_FILE=$(in_root "${ZEL_GAME_RULES_FILE:-config/game-rules.conf}")
  [ -z "${ZEL_KEY_FILE:-}" ] || { export ZEL_KEY_FILE; ZEL_KEY_FILE=$(in_root "$ZEL_KEY_FILE"); }
  export ZEL_DB_SOCKET=$DATA/mariadb.sock
  export ZEL_DB_HOST=127.0.0.1

  HEAP=${ZEL_SERVER_HEAP:-2g}
  BUFFER_POOL=${ZEL_DB_BUFFER_POOL:-128M}
  [[ $HEAP =~ ^[1-9][0-9]*[mMgG]$ ]] || fail "ZEL_SERVER_HEAP must be a size like 512m or 2g, not '$HEAP'"
  [[ $BUFFER_POOL =~ ^[1-9][0-9]*[mMgG]$ ]] || fail "ZEL_DB_BUFFER_POOL must be a size like 128M or 1G, not '$BUFFER_POOL'"
  HEAP_MB=$(size_mb "$HEAP")
  [ "$HEAP_MB" -ge 256 ] || fail "ZEL_SERVER_HEAP must be at least 256m, not '$HEAP'"
  # The heap starts at 512 MB, or at the whole heap when that is smaller.
  HEAP_START=$(( HEAP_MB < 512 ? HEAP_MB : 512 ))m

  LOG_KEEP=${ZEL_LOG_KEEP:-5}
  BACKUP_KEEP=${ZEL_BACKUP_KEEP:-10}
  [[ $LOG_KEEP =~ ^[1-9][0-9]*$ ]] || fail "ZEL_LOG_KEEP must be a number from 1, not '$LOG_KEEP'"
  [[ $BACKUP_KEEP =~ ^[1-9][0-9]*$ ]] || fail "ZEL_BACKUP_KEEP must be a number from 1, not '$BACKUP_KEEP'"
  TUNNEL_PORT=${ZEL_TUNNEL_PORT:-9445}
  export ZEL_TUNNEL_PORT=$TUNNEL_PORT
  TUNNEL_METRICS_PORT=$(( TUNNEL_PORT + 1 ))
  MODE=${ZEL_PROXY:-local}
  TUNNEL_MODE=${ZEL_TUNNEL_MODE:-quick}
  PUBLIC_MARK=""
  SITE_URL=""
  PROBE_HOST=127.0.0.1
}

# MODE is how players reach the game: local, domain, tunnel or none. --public, and the mark the last start with
# --public left, turn local into a quick tunnel, so a plain start keeps the tunnel that is open.
resolve_mode() { # 1 when --public was given, else empty
  local public=${1:-} domain=${ZEL_DOMAIN:-}
  MODE=${ZEL_PROXY:-local}
  TUNNEL_MODE=${ZEL_TUNNEL_MODE:-quick}
  PUBLIC_MARK=""
  case $MODE in
    local)
      if [ -n "$public" ] || [ -f "$RUN/public" ]; then MODE=tunnel TUNNEL_MODE=quick PUBLIC_MARK=1; fi ;;
    domain|tunnel|none) ;;
    *) fail "ZEL_PROXY must be local, domain, tunnel or none, not '$MODE'" ;;
  esac
  [ -z "$domain" ] || [[ $domain =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]] \
    || fail "ZEL_DOMAIN is a host name like game.example.org (no https:// and no path), not '$domain'"
  case $MODE in
    local)
      SITE_URL=https://$ZEL_HOST
      [ "$ZEL_HTTPS_PORT" = 443 ] || SITE_URL=$SITE_URL:$ZEL_HTTPS_PORT ;;
    domain|none)
      [ -n "$domain" ] || fail "ZEL_PROXY=$MODE needs ZEL_DOMAIN (config/zellera.local.env)"
      SITE_URL=https://$domain ;;
    tunnel)
      case $TUNNEL_MODE in
        quick) SITE_URL="" ;;
        named)
          [ -n "$domain" ] && [ -n "${ZEL_TUNNEL:-}" ] \
            || fail "ZEL_TUNNEL_MODE=named needs ZEL_TUNNEL and ZEL_DOMAIN (config/zellera.local.env)"
          SITE_URL=https://$domain ;;
        *) fail "ZEL_TUNNEL_MODE must be quick or named, not '$TUNNEL_MODE'" ;;
      esac ;;
  esac
  [ "$MODE" = local ] || [ "$ZEL_ENV" != dev ] \
    || fail "ZEL_ENV=dev keeps the dev logins on; a game on the internet runs in production mode"
  # Behind the game's own proxy the player's address always comes from loopback in X-Forwarded-For.
  if [ "$MODE" = none ]; then
    export ZEL_TRUSTED_PROXIES=${ZEL_TRUSTED_PROXIES:-127.0.0.1}
    export ZEL_CLIENT_IP_HEADER=${ZEL_CLIENT_IP_HEADER:-X-Forwarded-For}
    export ZEL_LISTEN_HOST=${ZEL_BIND:-127.0.0.1}
  else
    export ZEL_TRUSTED_PROXIES=127.0.0.1 ZEL_CLIENT_IP_HEADER=X-Forwarded-For ZEL_LISTEN_HOST=127.0.0.1
  fi
  # Where this computer reaches the server and the web: where they listen, or loopback when that is every address.
  case $ZEL_LISTEN_HOST in 0.0.0.0|::|'[::]') PROBE_HOST=127.0.0.1 ;; *) PROBE_HOST=$ZEL_LISTEN_HOST ;; esac
}

# The one address of the site, for the server and the web; unset while a quick tunnel has none yet.
export_site_url() {
  if [ -n "$SITE_URL" ]; then export ZEL_SITE_URL=$SITE_URL; else unset ZEL_SITE_URL; fi
}

prepare_data() {
  mkdir -p "$DATA"
  chmod 700 "$DATA"
  mkdir -p "$RUN/server" "$RUN/tool" "$LOGS"
  mkdir -p -m 700 "$ZEL_SECRETS_DIR"
}

# One zel command at a time changes the game. The lock is a folder, which mkdir makes or refuses in one step; a lock
# whose owner is gone is taken over.
lock_take() {
  local lock=$DATA/lock owner
  mkdir -p "$DATA"
  if ! mkdir "$lock" 2>/dev/null; then
    owner=$(cat "$lock/pid" 2>/dev/null || true)
    # Between another command's mkdir and its pid there is a moment without one.
    [ -n "$owner" ] || { sleep 1; owner=$(cat "$lock/pid" 2>/dev/null || true); }
    if [ "$owner" != $$ ]; then
      [ -z "$owner" ] || ! kill -0 "$owner" 2>/dev/null || fail "another zel command is at work (pid $owner); try again when it is done"
      rm -rf "$lock"
      mkdir "$lock" 2>/dev/null || fail "another zel command just started; try again when it is done"
    fi
  fi
  echo $$ > "$lock/pid"
  trap lock_release EXIT
  trap 'exit 130' INT TERM HUP
}
lock_release() { [ "$(cat "$DATA/lock/pid" 2>/dev/null || true)" != $$ ] || rm -rf "$DATA/lock"; }

# The body of http://<host>:<port><path> (host: 127.0.0.1 unless given); fails unless the answer is a 200.
http_get() { # port, path, host
  (
    exec 9<>"/dev/tcp/${3:-127.0.0.1}/$1" || exit 1
    printf 'GET %s HTTP/1.0\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n' "$2" >&9
    IFS= read -r -t 5 status <&9 || exit 1
    case $status in "HTTP/1."?" 200"*) ;; *) exit 1 ;; esac
    while IFS= read -r -t 5 line <&9; do [ -n "${line%$'\r'}" ] || break; done
    while IFS= read -r -t 5 line <&9 || [ -n "$line" ]; do printf '%s\n' "$line"; line=""; done
  ) 2>/dev/null
}
port_open() { (exec 9<>"/dev/tcp/127.0.0.1/$1") 2>/dev/null; }
json_field() { sed -n 's/.*"'"$1"'"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -1; }

# What every process gets: the configuration, never a database account or its password.
env_common() {
  local name
  ENVV=("PATH=$PATH" "HOME=$HOME")
  for name in LANG LC_ALL TZ TMPDIR JAVA_HOME USER LOGNAME XDG_RUNTIME_DIR; do
    [ -z "${!name:-}" ] || ENVV+=("$name=${!name}")
  done
  for name in $(compgen -e ZEL_ || true); do
    case $name in ZEL_DB_*_USER|ZEL_DB_*_PASS) continue ;; esac
    ENVV+=("$name=${!name}")
  done
}
