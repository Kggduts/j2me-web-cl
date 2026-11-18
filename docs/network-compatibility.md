# Network compatibility contract

## Implementation approach

The project overlays the pinned FreeJ2ME Web source at build time instead of editing a moving upstream checkout. The overlay lives in `patches/src/javax/microedition/io/` and is applied only when the upstream HEAD matches the pinned commit.

The overlay replaces `Connector.open()` routing and adds GCF adapters backed by Java SE networking classes:

| GCF scheme | Adapter | Runtime transport | Current evidence |
| --- | --- | --- | --- |
| `http://` | `HttpConnection` | `HttpURLConnection` / browser Fetch in CheerpJ | Source added; JVM smoke test added; build/browser run pending |
| `https://` | `HttpsConnection` | `HttpsURLConnection` / browser Fetch in CheerpJ | Source added; browser TLS test pending |
| `socket://` | `SocketConnection` | `java.net.Socket` | Source added; JVM smoke test added; CheerpJ/Tailscale test pending |
| `serversocket://` | `ServerSocketConnection` | `java.net.ServerSocket` | Source added; JVM accept/stream smoke test added; CheerpJ/Tailscale test pending |
| `datagram://` | `DatagramConnection` | `java.net.DatagramSocket` | Source added; JVM smoke test added; CheerpJ/Tailscale test pending |
| `sms://` | Existing upstream placeholder | Simulated implementation | Not a real SMS transport |
| Other schemes | None | — | Unsupported |

This is an implementation in source, not a claim of verified compatibility. The project must not advertise universal J2ME game networking.

## Browser transport and authentication

CheerpJ documents HTTP/HTTPS networking through the browser's Fetch stack. Cross-origin destinations must permit the player origin through CORS; browser TLS and origin policy remain authoritative.

CheerpJ documents TCP and UDP through Tailscale. The upstream overlay adds an interactive login callback and a sign-in link to the emulator page. No reusable Tailscale auth key is embedded in the public client. Users must authenticate into a tailnet that can reach the destination service; private-network ACLs and exit-node routing remain the tailnet administrator's responsibility.

The pinned upstream loader has not yet been browser-tested with this callback. Treat the integration as experimental until a real browser session confirms the login flow and Java socket behavior.

## Adapter behavior and limitations

- HTTP(S) uses Java URL connection APIs. Request methods, request headers, response status, content metadata, and byte streams are mapped to the GCF interfaces.
- TLS validation and certificate policy are browser-managed in CheerpJ. `HttpsConnection.getSecurityInfo()` currently returns `null`; Java ME certificate metadata is not synthesized.
- TCP maps Java ME socket streams and the five standard socket options to `java.net.Socket`.
- UDP preserves packet boundaries and binary data using `DatagramPacket`. The adapter supports client-style destinations and local port binding with `datagram://:port`.
- The pinned upstream `InputConnection`/`OutputConnection` stream-opening methods and `DatagramConnection.send/receive` signatures do not declare checked exceptions. Adapter I/O failures at those boundaries are surfaced as `IllegalStateException`; verify game-specific error handling.
- Push registry, SMS, Bluetooth, multicast/broadcast, vendor APIs, and nonstandard URL schemes remain unsupported.
- Browser networking, Tailscale ACLs, remote server policy, and the runtime's exact loader version can affect behavior.

## Reproducible build and tests

`scripts/apply-upstream-patches.py` verifies the pinned upstream commit before copying the adapter overlay and adding the optional Tailscale login UI. `scripts/build-emulator.sh` then performs a clean Ant build and runs:

1. `tools/run-gcf-adapter-tests.sh` against the local HTTP/TCP/UDP echo server using the built FreeJ2ME JAR on a normal JVM.
2. Compilation of `GcfNetworkProbe.java`, a MIDlet that exercises the same GCF APIs inside the browser emulator.

The JVM smoke test verifies adapter mapping and byte preservation; it does not prove CheerpJ's browser networking path. A real browser run and real-game test are still release gates.

## Test matrix

| Area | Required checks |
| --- | --- |
| HTTP | GET, POST, headers, status codes, redirects, binary body, CORS denial, timeout |
| HTTPS | valid TLS, invalid certificate behavior, redirects, CORS, certificate metadata |
| TCP | client connect, server-socket accept, bidirectional binary stream, partial reads, ordering, timeout, remote close, reconnect, concurrent clients |
| UDP | datagram boundaries, binary payload, packet limits, timeout, local bind, unreachable destination |
| Lifecycle | explicit close, MIDlet exit, page refresh, network loss, server restart |
| Security | interactive sign-in, tailnet ACLs, no shared keys, least-privilege destinations |
| Load | small and large payloads, sustained connection, concurrent connections |

## Release gates

- **Source implemented:** adapter code and deterministic tests exist.
- **JVM-verified:** automated tests pass against a local controlled server.
- **Browser-verified:** tests pass in supported real browsers using the pinned CheerpJ loader.
- **Game-verified:** at least one real MIDlet for each claimed transport succeeds against a controlled endpoint.
- **Production-ready:** authentication, ACLs, abuse limits, observability, deployment, and lifecycle behavior are reviewed.

Until the applicable gate is met, label the feature experimental or unsupported.


## Runtime network diagnostics

The GCF adapter emits safe connection-open diagnostics to Java standard error,
which CheerpJ exposes in the browser developer console. Filter for
`[J2ME-NET]` while reproducing a connection attempt in the game.

Each record includes a timestamp, protocol, destination host and port,
connection mode, elapsed time, and (on failure) exception class/message.
URL paths, query strings, credentials, and packet payloads are deliberately
excluded. Records use `OPEN_OK` or `OPEN_FAIL`.

To investigate a game connection failure:

1. Open the player on the phone and launch the JAR.
2. Reproduce the failure by pressing Play / selecting a server.
3. Tap the **Network Log** button at the bottom-right of the emulator page.
4. Tap **Sao chép log** and paste the result into the support conversation.
5. If copying is unavailable, tap the text area, long-press, select all, and copy.

The same records remain available in the desktop browser developer console under
`[J2ME-NET]`. The in-page panel captures only those tagged records and keeps
the latest 400 lines in memory; it does not upload logs automatically.

A successful `OPEN_OK` means the transport connection was established; it does
not prove that the game server accepted the game protocol or login exchange.
