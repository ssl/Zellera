# shellcheck shell=bash
# Caddy in front of the web and the server, for the modes local, domain and tunnel.

CADDY_CHANGED=""

# Writes .data/caddy/Caddyfile from config/caddy/<mode>.caddy. A line that names an empty variable is left out, so an
# optional setting needs no second template; config/Caddyfile.local, when there, is imported at the end.
caddy_render() {
  local out=$DATA/caddy/Caddyfile line rest name skip placeholder='\{\$([A-Z0-9_]+)\}'
  mkdir -p "$DATA/caddy"
  {
    while IFS= read -r line || [ -n "$line" ]; do
      skip="" rest=$line
      while [[ $rest =~ $placeholder ]]; do
        name=${BASH_REMATCH[1]}
        [ -n "${!name:-}" ] || skip=1
        rest=${rest#*"${BASH_REMATCH[0]}"}
      done
      [ -n "$skip" ] || printf '%s\n' "$line"
    done < "$ROOT/config/caddy/$MODE.caddy"
    [ ! -f "$ROOT/config/Caddyfile.local" ] || printf 'import "%s"\n' "$ROOT/config/Caddyfile.local"
  } > "$out.new"
  env_common
  env -i "${ENVV[@]}" caddy adapt --adapter caddyfile --config "$out.new" >/dev/null 2> "$out.error" \
    || { cat "$out.error" >&2; fail "Caddy refuses the configuration (config/caddy/$MODE.caddy, config/Caddyfile.local)"; }
  rm -f "$out.error"
  CADDY_CHANGED=""
  cmp -s "$out.new" "$out" || CADDY_CHANGED=1
  mv "$out.new" "$out"
}

proxy_up() {
  if ! process_wanted proxy; then
    stop_or_fail proxy
    return 0
  fi
  caddy_render
  [ -z "$CADDY_CHANGED" ] || stop_or_fail proxy
  process_start proxy
}

trust_help() {
  local ca=$DATA/caddy/pki/authorities/local/root.crt
  [ -f "$ca" ] || fail "no certificate authority yet at $ca: start the game once with ZEL_PROXY=local"
  echo "The local address is signed by Caddy's own certificate authority. Make this computer trust it once:"
  echo "  $ca"
  case $(uname -s) in
    Darwin)
      echo "  macOS:  security add-trusted-cert -r trustRoot -k ~/Library/Keychains/login.keychain-db $(printf %q "$ca")"
      echo "          (asks for your login password; Chrome and Safari then accept it, Firefox asks once in its own dialog)" ;;
    *)
      echo "  Debian: sudo cp $(printf %q "$ca") /usr/local/share/ca-certificates/zellera-local.crt && sudo update-ca-certificates"
      echo "  Chrome: certutil -d sql:\$HOME/.pki/nssdb -A -t C,, -n zellera-local -i $(printf %q "$ca")   (package libnss3-tools)" ;;
  esac
  echo "  Or open the address and accept the warning. Only ZEL_PROXY=local needs this."
}
