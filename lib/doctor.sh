# shellcheck shell=bash
# Checks this computer against what the chosen mode needs; changes nothing.

DOCTOR_FAILED=0
ok() { printf 'ok    %s\n' "$*"; }
warn() { printf 'warn  %s\n' "$*"; }
bad() { printf 'FAIL  %s\n' "$*"; DOCTOR_FAILED=1; }

doctor_tool() { # command, what to install
  if have "$1"; then ok "$1"; else bad "$1 is missing: $2"; fi
}

doctor_port() { # port, process that owns it, label
  if ! port_open "$1"; then ok "port $1 is free ($3)"
  elif [ -n "$2" ] && process_alive "$2"; then ok "port $1 is this game's $2"
  else bad "port $1 is taken by something else ($3): free it or set another in config/zellera.local.env"
  fi
}

doctor_memory() {
  local total="" pool want
  case $(uname -s) in
    Darwin) total=$(( $(sysctl -n hw.memsize 2>/dev/null || echo 0) / 1048576 )) ;;
    *) total=$(awk '/^MemTotal:/ { print int($2 / 1024) }' /proc/meminfo 2>/dev/null || true) ;;
  esac
  pool=$(size_mb "$BUFFER_POOL")
  want=$(( HEAP_MB + pool + 512 ))
  if [ -z "$total" ] || [ "$total" = 0 ]; then warn "could not read this computer's memory"
  elif [ "$total" -lt "$want" ]; then
    warn "memory: $total MB, but ZEL_SERVER_HEAP=$HEAP plus ZEL_DB_BUFFER_POOL=$BUFFER_POOL needs about $want MB (README.md, Server sizing)"
  else ok "memory: $total MB for a heap of $HEAP and a buffer pool of $BUFFER_POOL"
  fi
}

doctor() {
  local line mode_problem="" free
  DOCTOR_FAILED=0
  ok "user $OS_USER (not root), bash ${BASH_VERSION%%(*}, version $VERSION"
  mode_problem=$( (resolve_mode "${1:-}") 2>&1 >/dev/null) || true
  if [ -n "$mode_problem" ]; then
    bad "${mode_problem#zel: }"
    MODE=${ZEL_PROXY:-local}
  else
    resolve_mode "${1:-}"
    ok "network mode: $MODE$([ "$MODE" != tunnel ] || echo " ($TUNNEL_MODE)")"
  fi

  if ! have java; then bad "java is missing: install JDK 25"
  else
    line=$(java -version 2>&1 | head -1)
    if [[ $line =~ \"25[.\"] ]]; then ok "java 25"; else bad "java is not 25: $line"; fi
  fi
  if ! have node; then bad "node is missing: install Node.js 22 or newer"
  else
    line=$(node --version)
    if [[ $line =~ ^v(2[2-9]|[3-9][0-9])\. ]]; then ok "node $line"; else bad "node is not 22 or newer: $line"; fi
  fi
  doctor_tool npm "it comes with Node.js"
  if ! have mariadbd; then bad "mariadbd is missing: install MariaDB 11 or newer (the server, not only the client)"
  else
    line=$(mariadbd --version 2>/dev/null || true)
    if [[ $line =~ Ver\ ([0-9]+)\. ]] && [ "${BASH_REMATCH[1]}" -ge 11 ]; then ok "mariadbd ${BASH_REMATCH[1]}"
    else bad "MariaDB is older than 11: $line"; fi
  fi
  for line in mariadb-install-db mariadb mariadb-admin mariadb-dump; do doctor_tool "$line" "it comes with MariaDB"; done
  doctor_tool gzip "install gzip"
  case $MODE in local|domain|tunnel) doctor_tool caddy "install Caddy 2 (ZEL_PROXY=$MODE runs it)" ;; esac
  [ "$MODE" != tunnel ] || doctor_tool cloudflared "install cloudflared (ZEL_PROXY=tunnel runs it)"
  if have git; then ok git; else warn "git is missing: ./zel upgrade needs it"; fi

  doctor_port "$ZEL_DB_PORT" db ZEL_DB_PORT
  doctor_port "$ZEL_WS_PORT" server ZEL_WS_PORT
  doctor_port "$ZEL_WEB_PORT" web ZEL_WEB_PORT
  case $MODE in
    local) doctor_port "$ZEL_HTTPS_PORT" proxy ZEL_HTTPS_PORT ;;
    domain)
      doctor_port 443 proxy "the domain's HTTPS"
      doctor_port 80 proxy "the domain's HTTP, for the certificate"
      if [ "$(uname -s)" = Linux ] && [ "$(cat /proc/sys/net/ipv4/ip_unprivileged_port_start 2>/dev/null || echo 1024)" -gt 80 ]; then
        warn "an ordinary user may not open ports 80 and 443 here: sudo setcap cap_net_bind_service=+ep $(command -v caddy || echo caddy)"
      fi ;;
    tunnel)
      doctor_port "$TUNNEL_PORT" proxy ZEL_TUNNEL_PORT
      doctor_port "$TUNNEL_METRICS_PORT" tunnel "ZEL_TUNNEL_PORT + 1" ;;
  esac
  doctor_memory
  free=$(df -Pk "$ROOT" | awk 'NR == 2 { print int($4 / 1024) }')
  if [ "${free:-0}" -lt 2048 ]; then warn "disk: $free MB free in $ROOT"; else ok "disk: $free MB free"; fi
  if [ -d "$DATA" ] && [ -n "$(find "$DATA" -maxdepth 0 -perm -004 2>/dev/null)" ]; then
    warn ".data is readable by other users; the next start makes it private"
  fi
  if [ "$DOCTOR_FAILED" = 0 ]; then echo "doctor: ready"; else echo "doctor: fix the FAIL lines"; fi
  return "$DOCTOR_FAILED"
}

# Before a start: the same checks, shown only when one fails.
doctor_or_fail() {
  local report
  report=$(doctor "${1:-}") || { printf '%s\n' "$report" | grep -E '^(FAIL|warn)' >&2; fail "this computer is not ready (./zel doctor)"; }
}
