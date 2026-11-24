# shellcheck shell=bash
# systemd user units (Linux): one per process under zel.target, so the game comes back after a crash or a reboot.

UNIT_DIR=${XDG_CONFIG_HOME:-$HOME/.config}/systemd/user

# The units of this folder are installed; another folder's units are not this game's.
service_installed() {
  [ -f "$UNIT_DIR/zel.target" ] && grep -qF "# root: $ROOT" "$UNIT_DIR/zel.target" 2>/dev/null
}

unit_text() { local s=${1//\\/\\\\}; s=${s//\"/\\\"}; s=${s//%/%%}; s=${s//\$/\$\$}; printf '"%s"' "$s"; }

# The process's environment as a file only this user reads; systemd hands it to the unit.
service_env_file() { # name
  local file=$DATA/service/$1.env entry value
  mkdir -p -m 700 "$DATA/service"
  process_env "$1"
  (umask 077; for entry in "${ENVV[@]}"; do
    value=${entry#*=}; value=${value//\\/\\\\}; value=${value//\"/\\\"}
    printf '%s="%s"\n' "${entry%%=*}" "$value"
  done > "$file")
}

service_unit() { # name, units it starts after
  local name=$1 after=$2 arg exec stop=90 start=120
  process_command "$name"
  exec=$(unit_text "$(command -v "${CMD[0]}")")
  for arg in "${CMD[@]:1}"; do exec="$exec $(unit_text "$arg")"; done
  case $name in server) stop=200 start=300 ;; db) stop=150 ;; esac
  cat <<UNIT
[Unit]
Description=Zellera $name
PartOf=zel.target
${after:+After=$after}
${after:+Requires=${after%% *}}

[Service]
WorkingDirectory=$CMD_DIR
EnvironmentFile=$DATA/service/$name.env
ExecStart=$exec
ExecStartPost=$(unit_text "$ROOT/zel") _wait $name
StandardOutput=append:$LOGS/$name.log
StandardError=inherit
Restart=on-failure
RestartSec=5
SuccessExitStatus=143
TimeoutStartSec=$start
TimeoutStopSec=$stop

[Install]
WantedBy=zel.target
UNIT
}

# Writes the units the mode needs and removes the others; fails when nothing changed.
service_write() {
  local name after wants="" changed=1 file
  mkdir -p "$UNIT_DIR"
  for name in $PROCESSES; do
    file=$UNIT_DIR/zel-$name.service
    if ! process_wanted "$name"; then
      [ ! -f "$file" ] || { systemctl --user disable --now "zel-$name.service" >/dev/null 2>&1 || true; rm -f "$file"; changed=0; }
      continue
    fi
    case $name in
      db) after="" ;;
      server) after="zel-db.service" ;;
      web) after="zel-db.service zel-server.service" ;;
      proxy) after="zel-web.service" ;;
      tunnel) after="zel-proxy.service" ;;
    esac
    service_env_file "$name"
    service_unit "$name" "$after" > "$file.new"
    if cmp -s "$file.new" "$file"; then rm -f "$file.new"; else mv "$file.new" "$file"; changed=0; fi
    wants="$wants zel-$name.service"
  done
  file=$UNIT_DIR/zel.target
  printf '# root: %s\n[Unit]\nDescription=Zellera\nWants=%s\n\n[Install]\nWantedBy=default.target\n' "$ROOT" "${wants# }" > "$file.new"
  if cmp -s "$file.new" "$file"; then rm -f "$file.new"; else mv "$file.new" "$file"; changed=0; fi
  return $changed
}

# Before a start: the units follow the configuration.
service_sync() {
  service_installed || return 0
  ! service_write || systemctl --user daemon-reload
}

service_install() {
  local name
  [ "$(uname -s)" = Linux ] && have systemctl || fail "install-service is for Linux with systemd"
  systemctl --user show-environment >/dev/null 2>&1 || fail "no systemd user session (log in as $OS_USER directly, not through su)"
  [ "$MODE" != tunnel ] || [ "$TUNNEL_MODE" = named ] \
    || fail "a quick tunnel has a new address each start, which a service cannot follow: use a named tunnel or another ZEL_PROXY"
  [ ! -f "$UNIT_DIR/zel.target" ] || service_installed \
    || fail "$UNIT_DIR/zel.target belongs to another Zellera folder; remove that one first (./zel uninstall-service there)"
  for name in $PROCESSES; do
    ! process_alive "$name" || service_installed || fail "the game runs: ./zel stop first"
  done
  [ -f "$RUN/server/zellera-server.jar" ] || fail "start the game once first (./zel start, then ./zel stop), so everything it needs is there"
  db_secrets
  service_write || true
  systemctl --user daemon-reload
  for name in $PROCESSES; do
    ! process_wanted "$name" || systemctl --user enable "zel-$name.service" >/dev/null
  done
  systemctl --user enable zel.target >/dev/null
  echo "installed: $UNIT_DIR/zel.target and zel-*.service"
  echo "  ./zel start, stop, restart and status now work through systemd; a process that fails is started again."
  echo "  To have the game start at boot, without anyone logged in:  sudo loginctl enable-linger $OS_USER"
}

service_uninstall() {
  local name
  service_installed || fail "no units of this folder are installed"
  for name in tunnel proxy web server db; do
    systemctl --user disable --now "zel-$name.service" >/dev/null 2>&1 || true
    rm -f "$UNIT_DIR/zel-$name.service"
  done
  systemctl --user disable zel.target >/dev/null 2>&1 || true
  rm -f "$UNIT_DIR/zel.target"
  rm -rf "$DATA/service"
  systemctl --user daemon-reload
  echo "removed the units; the game is stopped"
}
