#!/usr/bin/env bash
# Makes the release bundle zellera-<version>.zip and its .sha256: what runs a game, with the server jar built, the
# web's packages installed and MANIFEST listing every file with its sha256. The same files give the same MANIFEST.
#
#   lib/bundle.sh [--jar <file>] [--out <folder>]
#
# The jar is server/build/libs/zellera-server-<version>-all.jar unless named; the zip goes to server/build/bundle.
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
fail() { echo "bundle: $*" >&2; exit 1; }
if command -v sha256sum >/dev/null 2>&1; then SHA256=(sha256sum); else SHA256=(shasum -a 256); fi

VERSION=$(tr -d '[:space:]' < "$ROOT/VERSION")
NAME=zellera-$VERSION
JAR=$ROOT/server/build/libs/zellera-server-$VERSION-all.jar
OUT=$ROOT/server/build/bundle
while [ $# -gt 0 ]; do
  case $1 in
    --jar) JAR=${2:?--jar needs a file}; shift ;;
    --out) OUT=${2:?--out needs a folder}; shift ;;
    *) fail "takes --jar <file> and --out <folder>" ;;
  esac
  shift
done
[ -f "$JAR" ] || fail "no $JAR: build it first (cd server && ./gradlew shadowJar)"
mkdir -p "$OUT"
OUT=$(cd "$OUT" && pwd)
export TZ=UTC LC_ALL=C
STAMP=$(git -C "$ROOT" log -1 --format=%cd --date=format-local:%Y%m%d%H%M.%S)

STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT
DIR=$STAGE/$NAME
mkdir "$DIR"

# The server's source, the build, CI and this script stay in the repository.
git -C "$ROOT" ls-files -s | while read -r mode _ _ path; do
  case $path in server/*|.*|lib/bundle.sh) continue ;; esac
  mkdir -p "$DIR/$(dirname "$path")"
  cp "$ROOT/$path" "$DIR/$path"
  if [ "$mode" = 100755 ]; then chmod 755 "$DIR/$path"; else chmod 644 "$DIR/$path"; fi
done
cp "$JAR" "$DIR/zellera-server-$VERSION-all.jar"
chmod 644 "$DIR/zellera-server-$VERSION-all.jar"
(cd "$DIR/web" && npm ci --omit=dev --ignore-scripts --no-audit --no-fund >/dev/null) || fail "npm ci in web/ failed"
find "$DIR/web/node_modules" -type f -exec chmod 644 {} +
mkdir -p "$DIR/custom/themes" "$DIR/custom/extensions" "$DIR/custom/game-extensions"
find "$DIR" -type d -exec chmod 755 {} +

(cd "$DIR" && find . -type f | sed 's#^\./##' | sort) > "$STAGE/files"
! grep -q '[[:space:]]' "$STAGE/files" || fail "a file name has a space: $(grep '[[:space:]]' "$STAGE/files" | head -1)"
(cd "$DIR" && xargs "${SHA256[@]}" < "$STAGE/files") > "$STAGE/MANIFEST"
mv "$STAGE/MANIFEST" "$DIR/MANIFEST"
chmod 644 "$DIR/MANIFEST"

find "$DIR" -exec touch -h -t "$STAMP" {} +
rm -f "$OUT/$NAME.zip" "$OUT/$NAME.zip.sha256"
(cd "$STAGE" && find "$NAME" | sort | zip -X -q -@ "$OUT/$NAME.zip")
(cd "$OUT" && "${SHA256[@]}" "$NAME.zip" > "$NAME.zip.sha256")
echo "$OUT/$NAME.zip"
