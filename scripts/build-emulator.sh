#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
UPSTREAM="$ROOT/vendor/freej2me-web"
PINNED_COMMIT="c19416e75cbc15f9a27f7e967ee81cb108761e30"
IMAGE="j2me-cloud-player/freej2me-builder"

cd "$ROOT"

if [ ! -d "$UPSTREAM/.git" ]; then
  echo "Missing upstream checkout at $UPSTREAM." >&2
  exit 1
fi

ACTUAL_COMMIT="$(git -C "$UPSTREAM" rev-parse HEAD)"
if [ "$ACTUAL_COMMIT" != "$PINNED_COMMIT" ]; then
  echo "Upstream revision mismatch." >&2
  echo "Expected: $PINNED_COMMIT" >&2
  echo "Actual:   $ACTUAL_COMMIT" >&2
  exit 1
fi

python3 "$ROOT/scripts/apply-upstream-patches.py" "$UPSTREAM"
rm -rf "$UPSTREAM/build" "$UPSTREAM/web/freej2me-web.jar"

if command -v ant >/dev/null 2>&1; then
  (cd "$UPSTREAM" && ant)
elif command -v docker >/dev/null 2>&1; then
  docker build --build-arg UID=1000 -t "$IMAGE" "$UPSTREAM/builder_image"
  docker run --rm -w /app -v "$UPSTREAM:/app" "$IMAGE" ant
else
  echo "Install Ant and a JDK, or install Docker to use the upstream builder image." >&2
  exit 1
fi

test -s "$UPSTREAM/web/freej2me-web.jar"
if ! jar tf "$UPSTREAM/web/freej2me-web.jar" | grep -Fxq 'pl/zb3/freej2me/launcher/LauncherUtil.class'; then
  echo "LauncherUtil.class is missing from freej2me-web.jar." >&2
  exit 1
fi
bash "$ROOT/tools/run-gcf-adapter-tests.sh" "$UPSTREAM/web/freej2me-web.jar"

# Build a real MIDlet probe against the pinned upstream Java ME API classes.
PROBE_CLASSES="$(mktemp -d)"
trap 'rm -rf "$PROBE_CLASSES"' EXIT
javac -source 8 -target 8 -classpath "$UPSTREAM/web/freej2me-web.jar" \
  -d "$PROBE_CLASSES" \
  "$ROOT/tools/java-me-probe/GcfNetworkProbe.java" \
  "$ROOT/tools/java-me-probe/CoreCompatibilityProbe.java"

rm -rf "$ROOT/dist"
mkdir -p "$ROOT/dist"
cp -a "$ROOT/site/." "$ROOT/dist/"
mkdir -p "$ROOT/dist/emulator"
cp -a "$UPSTREAM/web/." "$ROOT/dist/emulator/"
cp "$UPSTREAM/LICENSE" "$ROOT/dist/emulator/UPSTREAM-LICENSE"
cp "$UPSTREAM/README.md" "$ROOT/dist/emulator/UPSTREAM-README.md"
jar cfm "$ROOT/dist/netprobe-midlet.jar" "$ROOT/tools/java-me-probe/MANIFEST.MF" \
  -C "$PROBE_CLASSES" .

cat > "$ROOT/dist/BUILD-INFO.txt" <<EOF
J2ME Cloud Player build
FreeJ2ME Web commit: $PINNED_COMMIT
The emulator is provided under its upstream license.
GCF HTTP(S), TCP client/server and UDP adapters are included; browser and real-game compatibility remain unverified.
EOF

echo "Built deployable site into $ROOT/dist"
echo "This build has not been browser-tested; verify HTTP Range support on the target host."
