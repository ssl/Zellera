# shellcheck shell=bash
# The five processes of a game: how each is started, stopped, found and asked whether it is ready.

PROCESSES="db server web proxy tunnel"

process_wanted() { # name
  case $1 in
    proxy) [ "$MODE" != none ] ;;
    tunnel) [ "$MODE" = tunnel ] ;;
    *) return 0 ;;
  esac
}

# Sets CMD and CMD_DIR.
process_command() { # name
  CMD_DIR=$ROOT
  case $1 in
    db)
      CMD=(mariadbd --no-defaults "--datadir=$DATA/mariadb" "--port=$ZEL_DB_PORT" --bind-address=127.0.0.1
        "--socket=$ZEL_DB_SOCKET" --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci
        --max-allowed-packet=256M "--innodb-buffer-pool-size=$BUFFER_POOL") ;;
    server)
      CMD_DIR=$RUN/server
      CMD=(java "-Xms$HEAP_START" "-Xmx$HEAP" -jar "$RUN/server/zellera-server.jar") ;;
    web) CMD=(node "$ROOT/web/server.mjs") ;;
    proxy) CMD=(caddy run --adapter caddyfile --config "$DATA/caddy/Caddyfile") ;;
    tunnel) tunnel_command ;;
  esac
}

# The process's own secrets on top of the common environment; sets ENVV.
process_env() { # name
  env_common
  case $1 in
    server) ENVV+=("ZEL_DB_APP_USER=$DB_APP_USER" "ZEL_DB_APP_PASS=$DB_APP_PASS") ;;
    web) ENVV+=("ZEL_DB_WEB_USER=$DB_WEB_USER" "ZEL_DB_WEB_PASS=$DB_WEB_PASS") ;;
  esac
}

process_port() { # name
  case $1 in
    db) echo "$ZEL_DB_PORT" ;;
    server) echo "$ZEL_WS_PORT" ;;
    web) echo "$ZEL_WEB_PORT" ;;
    proxy) case $MODE in local) echo "$ZEL_HTTPS_PORT" ;; domain) echo 443 ;; tunnel) echo "$TUNNEL_PORT" ;; esac ;;
    tunnel) echo "$TUNNEL_METRICS_PORT" ;;
  esac
}

# Part of the command line that only this game's process has, so a pid file left from an earlier run never names
# someone else's process.
process_marker() { # name
  case $1 in
    db) echo "$DATA/mariadb" ;;
    server) echo "$RUN/server/zellera-server.jar" ;;
    web) echo "$ROOT/web/server.mjs" ;;
    proxy) echo "$DATA/caddy/Caddyfile" ;;
    tunnel) echo "http://127.0.0.1:$TUNNEL_PORT" ;;
  esac
}

process_pid() { # name; empty when it does not run
  local pid command
  if service_manages "$1"; then
    pid=$(systemctl --user show -p MainPID --value "zel-$1.service" 2>/dev/null || true)
    [ -z "$pid" ] || [ "$pid" = 0 ] || echo "$pid"
    return 0
  fi
  [ -f "$RUN/$1.pid" ] || return 0
  pid=$(cat "$RUN/$1.pid")
  kill -0 "$pid" 2>/dev/null || return 0
  if have ps; then
    command=$(ps -p "$pid" -o command= 2>/dev/null) || return 0
    [[ $command == *"$(process_marker "$1")"* ]] || return 0
  fi
  echo "$pid"
}
process_alive() { [ -n "$(process_pid "$1")" ]; }

# The log of the last run moves to .1 (and on, LOG_KEEP of them); the new one opens with a line that says when.
log_open() { # name
  local file=$LOGS/$1.log i=$LOG_KEEP
  mkdir -p "$LOGS"
  if [ -s "$file" ]; then
    rm -f "$file.$i"
    while [ "$i" -gt 1 ]; do
      [ ! -f "$file.$(( i - 1 ))" ] || mv "$file.$(( i - 1 ))" "$file.$i"
      i=$(( i - 1 ))
    done
    mv "$file" "$file.1"
  fi
  printf '==== %s, %s, version %s ====\n' "$1" "$(date '+%Y-%m-%d %H:%M:%S %z')" "$VERSION" >> "$file"
}

process_start() { # name
  local name=$1
  ! service_manages "$name" || service_env_file "$name"
  if process_alive "$name"; then note "$name already runs (pid $(process_pid "$name"))"; return 0; fi
  log_open "$name"
  if service_manages "$name"; then
    systemctl --user start "zel-$name.service" || fail "zel-$name.service did not start (.data/logs/$name.log)"
  else
    process_command "$name"
    process_env "$name"
    (cd "$CMD_DIR" && exec_env nohup "${CMD[@]}" >> "$LOGS/$name.log" 2>&1 < /dev/null) &
    echo $! > "$RUN/$name.pid"
  fi
  note "$name started (pid $(process_pid "$name"))"
}

# Fails when it still runs after the wait: the server saves every loaded room first, so nothing is ever killed.
process_stop() { # name
  local name=$1 pid halves=60
  pid=$(process_pid "$name")
  if [ -n "$pid" ]; then
    case $name in server) halves=360 ;; db) halves=240 ;; esac
    if service_manages "$name"; then
      systemctl --user stop "zel-$name.service" || return 1
    else
      kill "$pid" 2>/dev/null || true
      while [ "$halves" -gt 0 ] && kill -0 "$pid" 2>/dev/null; do sleep 0.5; halves=$(( halves - 1 )); done
      ! kill -0 "$pid" 2>/dev/null || return 1
    fi
    note "$name stopped"
  elif service_manages "$name"; then
    systemctl --user stop "zel-$name.service" 2>/dev/null || true
  fi
  rm -f "$RUN/$name.pid"
}
stop_or_fail() { # name
  process_stop "$1" || fail "$1 (pid $(process_pid "$1")) is still at work; it is never killed, so try again when it is done"
}

process_healthy() { # name
  case $1 in
    db) db_ping ;;
    server) http_get "$ZEL_WS_PORT" /health "$PROBE_HOST" >/dev/null ;;
    web) http_get "$ZEL_WEB_PORT" /health "$PROBE_HOST" >/dev/null ;;
    proxy) port_open "$(process_port proxy)" ;;
    tunnel) tunnel_ready ;;
  esac
}

process_wait() { # name, seconds
  local name=$1 halves=$(( $2 * 2 ))
  while [ "$halves" -gt 0 ]; do
    ! process_healthy "$name" || return 0
    process_alive "$name" || { tail -20 "$LOGS/$name.log" >&2; fail "$name stopped (.data/logs/$name.log)"; }
    sleep 0.5
    halves=$(( halves - 1 ))
  done
  fail "$name is not ready after $2 s (.data/logs/$name.log)"
}

process_version() { # name; of the running process where it says so itself
  case $1 in
    db) sql -N -e "SELECT VERSION()" 2>/dev/null || true ;;
    server) http_get "$ZEL_WS_PORT" /health "$PROBE_HOST" | json_field version ;;
    web) http_get "$ZEL_WEB_PORT" /health "$PROBE_HOST" | json_field version ;;
    proxy) caddy version 2>/dev/null | awk '{ print $1; exit }' ;;
    tunnel) cloudflared --version 2>/dev/null | awk '{ print $3; exit }' ;;
  esac
}

# Fails when a process the mode wants is down or not healthy.
status_table() {
  local name pid state health version all=0
  printf '%-8s %-8s %-8s %-6s %-7s %s\n' NAME STATE PID PORT HEALTH VERSION
  for name in $PROCESSES; do
    pid=$(process_pid "$name")
    if ! process_wanted "$name" && [ -z "$pid" ]; then
      printf '%-8s %-8s\n' "$name" "not used"
      continue
    fi
    if [ -n "$pid" ]; then
      state=running version=$(process_version "$name" || true)
      if process_healthy "$name"; then health=ok; else health=failing all=1; fi
    else
      state=stopped health=- version="" all=1
    fi
    printf '%-8s %-8s %-8s %-6s %-7s %s\n' "$name" "$state" "${pid:--}" "$(process_port "$name")" "$health" "${version:--}"
  done
  return $all
}
