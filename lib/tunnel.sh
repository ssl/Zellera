# shellcheck shell=bash
# The Cloudflare Tunnel of ZEL_PROXY=tunnel: a quick one with a new address each start, or a named one.

tunnel_kind() { if [ "$TUNNEL_MODE" = named ]; then echo "named $ZEL_TUNNEL"; else echo quick; fi; }

# Sets CMD; cloudflared reaches Caddy on loopback and answers about itself on the port after it.
tunnel_command() {
  local credentials=$ZEL_SECRETS_DIR/cloudflared-${ZEL_TUNNEL:-}.json
  CMD=(cloudflared tunnel --no-autoupdate --metrics "127.0.0.1:$TUNNEL_METRICS_PORT")
  if [ "$TUNNEL_MODE" = named ]; then
    CMD+=(run --url "http://127.0.0.1:$TUNNEL_PORT")
    [ ! -f "$credentials" ] || CMD+=(--credentials-file "$credentials")
    CMD+=("$ZEL_TUNNEL")
  else
    CMD+=(--url "http://127.0.0.1:$TUNNEL_PORT")
  fi
}

# cloudflared tells the address of a quick tunnel on its metrics port; an older one only writes it in its log.
tunnel_quick_host() {
  local host
  host=$(http_get "$TUNNEL_METRICS_PORT" /quicktunnel | json_field hostname || true)
  [ -n "$host" ] || host=$({ grep -Eo 'https://[a-z0-9-]+\.trycloudflare\.com' "$LOGS/tunnel.log" 2>/dev/null || true; } \
    | tail -1 | sed 's#https://##')
  echo "$host"
}

tunnel_ready() {
  if [ "$TUNNEL_MODE" = quick ]; then [ -n "$(tunnel_quick_host)" ]; else http_get "$TUNNEL_METRICS_PORT" /ready >/dev/null; fi
}

# Opens the tunnel the mode asks for and, for a quick one, sets SITE_URL to its address. A tunnel of another kind,
# or one that a mode without a tunnel left behind, is stopped.
tunnel_up() {
  local host=""
  if ! process_wanted tunnel; then
    stop_or_fail tunnel
    rm -f "$RUN/tunnel.kind"
    return 0
  fi
  [ "$(cat "$RUN/tunnel.kind" 2>/dev/null || true)" = "$(tunnel_kind)" ] || stop_or_fail tunnel
  if ! process_alive tunnel; then
    step "opening the tunnel ($(tunnel_kind))"
    process_start tunnel
    tunnel_kind > "$RUN/tunnel.kind"
  fi
  [ "$TUNNEL_MODE" = quick ] || return 0
  process_wait tunnel 30
  host=$(tunnel_quick_host)
  SITE_URL=https://$host
}
