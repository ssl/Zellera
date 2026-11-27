# shellcheck shell=bash
# The server jar of this version, the web's packages, and the server's one-shot commands.

jar_name() { echo "zellera-server-$VERSION-all.jar"; }
release_jar() { echo "$DATA/build/release/$(jar_name)"; }

# The checkout is exactly the tagged release, so a released jar is the same code.
tree_is_release() {
  have git || return 1
  [ "$(git -C "$ROOT" describe --exact-match --tags HEAD 2>/dev/null || true)" = "v$VERSION" ] || return 1
  [ -z "$(git -C "$ROOT" status --porcelain --untracked-files=no 2>/dev/null)" ]
}

release_url() { echo "${ZEL_RELEASE_URL:-https://github.com/ssl/zellera/releases/download}"; }

# Downloads a release file; it is kept only when it matches the release's .sha256 next to it.
release_download() { # url, file
  mkdir -p "$(dirname "$2")"
  node --input-type=module -e '
    import { createHash } from "node:crypto";
    import { writeFileSync } from "node:fs";
    const [url, out] = process.argv.slice(1);
    const get = async (u) => {
      const r = await fetch(u, { redirect: "follow" });
      if (!r.ok) throw new Error(`${u}: HTTP ${r.status}`);
      return Buffer.from(await r.arrayBuffer());
    };
    try {
      const wanted = (await get(`${url}.sha256`)).toString("utf8").trim().split(/\s+/)[0].toLowerCase();
      const body = await get(url);
      const got = createHash("sha256").update(body).digest("hex");
      if (got !== wanted) throw new Error(`${url}: sha256 is ${got}, the release says ${wanted}`);
      writeFileSync(out, body);
    } catch (e) { console.error(e.message); process.exit(1); }
  ' "$1" "$2"
}

release_fetch() { release_download "$(release_url)/v$VERSION/$(jar_name)" "$(release_jar)"; }

# A release bundle has no server source: its jar and the web's packages come with it, and MANIFEST lists its files.
is_bundle() { [ -f "$ROOT/MANIFEST" ]; }
bundle_jar() { echo "$ROOT/$(jar_name)"; }

build_from_source() {
  local built
  built=$BUILD_DIR/libs/$(jar_name)
  [[ $(java -version 2>&1 | head -1) =~ \"25[.\"] ]] \
    || fail "java is not JDK 25 (the server builds with 25): $(java -version 2>&1 | head -1)"
  if [ -f "$built" ]; then step "building the server"; else step "building the server (first time, a few minutes)"; fi
  log_open build
  (cd "$ROOT/server" && ZEL_BUILD_DIR="$BUILD_DIR" ./gradlew -q shadowJar >> "$LOGS/build.log" 2>&1) \
    || fail "the server build failed (.data/logs/build.log)"
  [ -f "$built" ] || fail "the build made no $(jar_name): VERSION and server/build.gradle.kts disagree"
  JAR=$built
}

# Sets JAR. A bundle takes its own jar. In a checkout, ZEL_BUILD: source builds here, release takes the release's jar,
# auto takes the release's jar when it is there and the checkout is that release, else builds.
build_server() {
  local mode=${ZEL_BUILD:-auto} release
  release=$(release_jar)
  if is_bundle; then
    case $mode in
      auto|release) ;;
      source) fail "this folder is a release bundle and holds no server source; to build the server, clone the repository (README.md, Build from source)" ;;
      *) fail "ZEL_BUILD must be auto, source or release, not '$mode'" ;;
    esac
    [ -f "$(bundle_jar)" ] || fail "the bundle's $(jar_name) is missing; unzip the bundle again"
    JAR=$(bundle_jar)
    return 0
  fi
  case $mode in
    source) build_from_source ;;
    release)
      if [ ! -f "$release" ]; then
        step "fetching the server $VERSION"
        release_fetch || fail "no release jar for v$VERSION (ZEL_BUILD=release); ZEL_BUILD=source builds it here"
      fi
      JAR=$release ;;
    auto) if [ -f "$release" ] && tree_is_release; then JAR=$release; else build_from_source; fi ;;
    *) fail "ZEL_BUILD must be auto, source or release, not '$mode'" ;;
  esac
}

# A running server keeps the jar it started with.
install_jar() {
  if process_alive server; then
    note "the server runs and keeps its jar (./zel restart starts the new build)"
  else
    cp -p "$JAR" "$RUN/server/zellera-server.jar"
  fi
}

# Installed again when web/package-lock.json changes.
web_packages() {
  local sum
  if is_bundle; then
    [ -d "$ROOT/web/node_modules" ] || fail "the bundle's web/node_modules is missing; unzip the bundle again"
    return 0
  fi
  sum=$(cksum < "$ROOT/web/package-lock.json")
  if [ ! -d "$ROOT/web/node_modules" ] || [ "$(cat "$DATA/web-packages" 2>/dev/null || true)" != "$sum" ]; then
    step "installing the web's packages"
    (cd "$ROOT/web" && npm ci --omit=dev --ignore-scripts --no-audit --no-fund >/dev/null) || fail "npm ci in web/ failed"
    echo "$sum" > "$DATA/web-packages"
  fi
}

# A server command that ends by itself. account: owner (the schema owner) or none.
jar_tool() { # account, arguments...
  local account=$1
  shift
  env_common
  [ "$account" != owner ] || ENVV+=("ZEL_DB_MIGRATE_USER=$DB_MIGRATE_USER" "ZEL_DB_MIGRATE_PASS=$DB_MIGRATE_PASS")
  (cd "$RUN/tool" && exec_env java -jar "$RUN/server/zellera-server.jar" "$@")
}

# The server checks the configuration first and ends with 3 when a value is not usable.
migrate() {
  local code=0
  log_open migrate
  jar_tool owner schema apply >> "$LOGS/migrate.log" 2>&1 || code=$?
  case $code in
    0) ;;
    3) cat "$LOGS/migrate.log" >&2; fail "fix config/zellera.env (or config/zellera.local.env) and start again" ;;
    *) tail -20 "$LOGS/migrate.log" >&2; fail "the migrations failed (.data/logs/migrate.log)" ;;
  esac
  db_grants
}

# What the server says about its content: 0 ready, 3 nothing imported, 4 imported but files are missing, 5 imported
# from a pack format this version does not read.
CONTENT_CODE=0
CONTENT_LINE=""
content_status() {
  CONTENT_CODE=0
  CONTENT_LINE=$(jar_tool owner content status 2>&1 | tail -1) || CONTENT_CODE=$?
}
content_required() {
  content_status
  case $CONTENT_CODE in
    0) ;;
    3) fail "${CONTENT_LINE:-nothing is imported yet}: ./zel import <pack.zip>" ;;
    4) fail "${CONTENT_LINE:-files of the imported pack are missing}: ./zel import <pack.zip> again" ;;
    5) fail "${CONTENT_LINE:-the imported pack has a format this version does not read}: ./zel import a pack made for this version" ;;
    *) fail "the server could not say whether a pack is imported (code $CONTENT_CODE): $CONTENT_LINE" ;;
  esac
}
