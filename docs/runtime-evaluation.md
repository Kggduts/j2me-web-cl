# Runtime evaluation: CheerpJ and network access

## Findings from official documentation

The current CheerpJ documentation describes:

- HTTP and HTTPS through the browser networking stack, subject to same-origin and CORS rules.
- TCP and UDP through a Tailscale-based networking path, or a custom networking backend in enterprise deployments.
- Tailscale networking carried over WebSockets.
- Both `tailscaleLoginUrlCb` (interactive sign-in URL) and `tailscaleIpCb` (assigned client IP) as documented `cheerpjInit()` options. These are documented API options, not evidence that the pinned loader has been tested.
- A full OpenJDK class library for the supported Java versions.

Sources:

- https://cheerpj.com/docs/guides/Networking.html
- https://cheerpj.com/docs/reference/cheerpjInit
- https://cheerpj.com/docs/explanation/architecture
- https://cheerpj.com/docs/tutorials/serverclient
- https://cheerpj.com/compatibility/

## Pinned upstream runtime

The pinned FreeJ2ME Web revision uses:

`https://cjrtnc.leaningtech.com/20260317_2978/loader.js`

The current documentation describes CheerpJ 5.x capabilities, while the pinned URL is a dated CDN build identifier rather than a human-readable semantic version. I cannot verify from that URL alone that every currently documented option is available in this exact runtime build. The exact loader and browser behavior still require direct testing.

## Project implementation

The project now contains a build-time overlay for Java ME GCF:

- `Connector.open("http://...")` and `https://...` map to Java URL connection APIs.
- `Connector.open("socket://...")` maps to `java.net.Socket`.
- `Connector.open("serversocket://...")` maps to `java.net.ServerSocket`.
- `Connector.open("datagram://...")` maps to `java.net.DatagramSocket`.
- The emulator bootstrap adds `tailscaleLoginUrlCb` and a visible login link without a reusable client-side auth key.
- A JVM smoke test exercises HTTP GET/POST, TCP binary streams, UDP datagram DataInput/DataOutput behavior, server-socket accept, and close-unblocks behavior against controlled local endpoints.

This is source-level integration plus an automated JVM test path. It is not yet browser-verified. CheerpJ's official documentation describes the transport path and callbacks, but the pinned loader must still prove the same behavior.

## Remaining evaluation

1. Verify `tailscaleLoginUrlCb` and `tailscaleIpCb` with the pinned loader.
2. Run the GCF MIDlet in a supported desktop and mobile browser.
3. Verify HTTP(S) CORS, redirects, response status handling, and TLS behavior.
4. Verify TCP and UDP through a controlled Tailscale network.
5. Test multiple connections, reconnect, timeout, page refresh, and MIDlet shutdown.
6. Test real MIDlets/games using each claimed transport.
7. Review authentication, ephemeral device enrollment, network ACLs, relay costs, and abuse limits before exposing networking to arbitrary uploaded JARs.

## Provisional direction

Use the documented CheerpJ networking path rather than inventing a raw-socket bridge, subject to exact-loader compatibility, deployment model, terms, and repeatable tests.

Do not embed a long-lived Tailscale auth key in public client-side JavaScript. Authentication and device enrollment remain a security boundary.

## Decision status

**Implementation in source; browser verification pending.** No universal J2ME game compatibility claim is made.
