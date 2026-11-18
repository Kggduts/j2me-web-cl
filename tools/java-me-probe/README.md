# MIDlet GCF network probe

This MIDlet tests the actual `javax.microedition.io` API exposed by the
patched emulator. It reports each stage as it runs, then records the target
host, port, protocol, elapsed time and exception text for HTTP, TCP and UDP.
A timeout is reported separately for the protocol that did not answer.

## Test through your Tailscale network

Run the echo server on the VPS or another machine that is already in your
tailnet. Bind it only to that machine's Tailscale IPv4 address:

```sh
python3 tools/netprobe-server.py --host 100.x.y.z
```

On Windows, use `py -3` instead of `python3`. Replace `100.x.y.z` with
the server machine's actual Tailscale IPv4 address. Keep the server private:
the echo endpoints have no authentication and must not be exposed to the
public internet. Allow the three ports only from the intended tailnet device
using your Tailscale ACL/grants and host firewall.

Build output: `dist/netprobe-midlet.jar`, assembled by
`scripts/build-emulator.sh`. The deployed site exposes it at
`/netprobe-midlet.jar`; open the Network Test Lab page to download it.

The MIDlet now starts with the VPS Tailscale IPv4 address `100.81.186.89`.
It no longer depends on typing into a Java ME TextField: choose a digit or `.`
from the on-screen character picker and press **Insert**; use **Delete** to
remove the last character, or **Use VPS IP** to restore the preset. The default
HTTP/TCP/UDP ports are 8765/8766/8767. Run the MIDlet in the browser emulator
after signing in to Tailscale through CheerpJ. This tests the emulator's own GCF API path to
that tailnet peer. It does not by itself prove that a public game server is
reachable through an exit node.

The project overlays the pinned upstream `Connector` implementation with
HTTP(S), TCP socket and UDP datagram adapters. The build also runs
`GcfAdapterSmokeTest` on a normal JVM against the same controlled server.
That JVM smoke test checks adapter mapping, but does not verify CheerpJ's
browser networking path. A successful in-browser MIDlet test is the required
next signal before diagnosing a real game.

For TCP/UDP in CheerpJ, use the emulator's interactive Tailscale sign-in link
and a tailnet ACL that allows only the intended test destination. Never put a
reusable Tailscale auth key in the public site.


## Offline core compatibility probe

The same JAR also contains a second MIDlet, **Core Compatibility Probe**.
It exercises MIDlet start/pause/destroy callbacks, LCDUI Canvas drawing,
key and pointer input, and a basic RMS create/add/read/update/close/reopen/read
sequence. It is intentionally offline and does not require a Tailscale session
or a reachable game server.

A passing RMS result verifies only this sequence in the current browser runtime.
It does not yet establish storage durability across browser restarts, private
browsing, quota pressure, multiple game suites, or browser storage eviction.
The probe currently uses a dedicated fixed RMS store name; keep its test data
separate from real game saves.
