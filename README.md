# J2ME Cloud Player

A browser-based J2ME (MIDP/CLDC) player built around the FreeJ2ME Web emulator.

## Project status

Early development. The project contains a source overlay for Java ME Generic Connection Framework (GCF) HTTP(S), TCP client/server, and UDP adapters. The pinned upstream build and automated JVM smoke tests pass in GitHub Actions. Browser runtime and real-game compatibility have not yet been confirmed, so browser transports remain experimental.

## Goals

- Run MIDP games in a modern browser without a native Android/desktop installation.
- Preserve the upstream emulator core and pin the exact source revision.
- Implement Java ME GCF networking over CheerpJ's documented Java networking paths.
- Validate HTTP, HTTPS, TCP, and UDP independently before calling them compatible.
- Keep game files local to browser storage unless the user explicitly exports them.

## Network design

- HTTP/HTTPS adapters use Java URL connection APIs and CheerpJ's browser Fetch path. CORS and browser TLS/origin policies apply.
- TCP client/server and UDP adapters use Java `Socket`, `ServerSocket`, and `DatagramSocket`. In CheerpJ, generalized networking requires Tailscale (or a supported custom backend).
- The emulator page has an interactive Tailscale login callback; no reusable auth key is shipped in public assets.
- Users need a tailnet route and ACL that allow the destination game server. A public player does not automatically gain access to arbitrary private networks.
- Server sockets, push registry, real SMS, Bluetooth, multicast/broadcast, vendor APIs, and universal game compatibility are not claimed.

See [Network compatibility](docs/network-compatibility.md) and [Security model](docs/security-model.md).

## Current implementation status

- Project landing page and browser HTTP(S) diagnostic: `site/`.
- Mobile player keypad: numeric keys, soft keys, directional pad, OK, and a dedicated Backspace/Delete button; touch input uses pointer events for multi-touch support.
- Local controlled HTTP/TCP/UDP echo server: `tools/netprobe-server.py`.
- Java SE baseline smoke test: `tools/java-probe/`.
- Java ME GCF adapter overlay: `patches/src/javax/microedition/io/`.
- Upstream patcher: `scripts/apply-upstream-patches.py` (refuses an unexpected upstream revision).
- GCF adapter JVM smoke test: `tools/run-gcf-adapter-tests.sh`.
- GCF diagnostic MIDlet: `tools/java-me-probe/`; packaged as `dist/netprobe-midlet.jar`.
- Static checks and reproducible emulator build: `.github/workflows/`.
- Reproducible emulator build and JVM adapter tests: verified in GitHub Actions (latest successful run: [Emulator build #83](https://github.com/Kggduts/j2me-cloud-player/actions/runs/44144865200)).
- Browser networking, Tailscale login, and real-game compatibility: pending verification.

The browser Fetch diagnostic is not a GCF or TCP/UDP test. The GCF JVM test is not a browser runtime test.

## Development

Run static checks:

```sh
bash scripts/check-project.sh
```

Run the Java SE baseline test:

```sh
bash tools/run-java-probe-tests.sh
```

The complete emulator build expects the pinned upstream checkout at `vendor/freej2me-web`, Python 3, a JDK 8-compatible toolchain, and Ant. It prefers native Ant; Docker is a fallback if Ant is unavailable. It applies the adapter overlay and automatically runs GCF adapter smoke tests:

```sh
bash scripts/build-emulator.sh
```

The build applies the GCF overlay, compiles FreeJ2ME, runs the GCF adapter smoke tests, packages the diagnostic MIDlet, and creates a deployable site under `dist/`. This pipeline has passed on GitHub Actions. The resulting site has not yet been browser-tested; the deployment host must support HTTP Range requests.

## Upstream and licensing

The emulator baseline is [zb3/freej2me-web](https://github.com/zb3/freej2me-web), pinned in the build script. Preserve its license and notices. The overlay is intended to be distributed under GPL-3.0-or-later with the upstream project; verify all notices before publishing binaries.

## Project links

- [Network compatibility design](docs/network-compatibility.md)
- [Runtime evaluation](docs/runtime-evaluation.md)
- [Security model](docs/security-model.md)
- [Java ME GCF probe](tools/java-me-probe/README.md)
- [Java SE transport probe](tools/java-probe/README.md)
- [Browser HTTP(S) diagnostic](site/netprobe/index.html)
