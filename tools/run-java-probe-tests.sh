#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
command -v javac >/dev/null 2>&1 || { echo "JDK (javac) is required." >&2; exit 1; }
TMP="$(mktemp -d)"
SERVER_PID=""
cleanup() {
  if [ -n "$SERVER_PID" ]; then kill "$SERVER_PID" 2>/dev/null || true; wait "$SERVER_PID" 2>/dev/null || true; fi
  rm -rf "$TMP"
}
trap cleanup EXIT
mkdir -p "$TMP/classes"
python3 tools/netprobe-server.py --host 127.0.0.1 --http-port 18765 --tcp-port 18766 --udp-port 18767 >"$TMP/server.log" 2>&1 &
SERVER_PID=$!
python3 - <<'PY'
import time
import urllib.request
for _ in range(50):
    try:
        with urllib.request.urlopen("http://127.0.0.1:18765/health", timeout=0.2) as r:
            if r.status == 200: break
    except Exception:
        time.sleep(0.1)
else:
    raise SystemExit("Probe server did not become ready")
PY
javac -source 8 -target 8 -d "$TMP/classes" tools/java-probe/NetworkProbe.java
java -cp "$TMP/classes" NetworkProbe 127.0.0.1 18765 18766 18767
