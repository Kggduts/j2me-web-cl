#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v node >/dev/null 2>&1 || {
  echo "Node.js is required for JavaScript syntax checks." >&2
  exit 1
}

test -s site/index.html
test -s site/styles.css
test -s site/netprobe/index.html
test -s site/netprobe/netprobe.js
test -s docs/network-compatibility.md
test -s docs/runtime-evaluation.md
test -s tools/java-probe/NetworkProbe.java
test -s tools/java-me-probe/GcfNetworkProbe.java
test -s tools/java-me-probe/GcfAdapterSmokeTest.java
test -s patches/src/javax/microedition/io/GcfNetwork.java
test -s patches/src/javax/microedition/io/Connector.java
test -s scripts/apply-upstream-patches.py
grep -Fq 'run.html?app=' scripts/apply-upstream-patches.py
grep -Fq '&mobile=1' scripts/apply-upstream-patches.py
grep -Fq 'Testing TCP ' tools/java-me-probe/GcfNetworkProbe.java
grep -Fq 'TCP ' tools/java-me-probe/GcfNetworkProbe.java
grep -Fq 'UDP ' tools/java-me-probe/GcfNetworkProbe.java
grep -Fq 'elapsed(started)' tools/java-me-probe/GcfNetworkProbe.java
grep -Fq 'pointer-based multi-touch keypad' scripts/apply-upstream-patches.py
grep -Fq 'data-key="Backspace"' scripts/apply-upstream-patches.py
grep -Fq 'pointercancel' scripts/apply-upstream-patches.py
grep -Fq 'OPEN_OK ' patches/src/javax/microedition/io/GcfNetwork.java
grep -Fq 'OPEN_FAIL ' patches/src/javax/microedition/io/GcfNetwork.java
grep -Fq '[J2ME-NET]' patches/src/javax/microedition/io/GcfNetwork.java
grep -Fq 'on-screen network diagnostics' scripts/apply-upstream-patches.py
grep -Fq 'j2me-netlog-copy' scripts/apply-upstream-patches.py
grep -Fq 'j2me-netlog-output' scripts/apply-upstream-patches.py

node --check site/netprobe/netprobe.js
bash -n scripts/build-emulator.sh
bash -n tools/run-java-probe-tests.sh
bash -n tools/run-gcf-adapter-tests.sh
python3 -c "compile(open('scripts/apply-upstream-patches.py', encoding='utf-8').read(), 'scripts/apply-upstream-patches.py', 'exec')"

if grep -R -nE '(tailscaleAuthKey|AuthKey[a-zA-Z0-9_-]{10,})' site; then
  echo "Possible Tailscale auth key found in public site files." >&2
  exit 1
fi

echo "Project sanity checks passed."
echo "Note: these checks do not build or execute the FreeJ2ME emulator."
