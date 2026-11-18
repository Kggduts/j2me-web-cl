# NetProbe

NetProbe contains the diagnostic plan and runnable probes for Java ME
networking compatibility.

## Executable probes

- `tools/java-me-probe/GcfNetworkProbe.java`: MIDlet exercising the actual
  `javax.microedition.io` GCF API in the emulator. Packaged as
  `dist/netprobe-midlet.jar`.
- `tools/java-me-probe/GcfAdapterSmokeTest.java`: runs the project's GCF
  adapters on a normal JVM against the local controlled server, including a
  `serversocket://` accept-and-echo test.
- `tools/java-probe/NetworkProbe.java`: Java SE baseline using ordinary
  `java.net`, not GCF.
- `site/netprobe/`: browser Fetch diagnostic; it does not test raw sockets.

The GCF overlay is in `patches/src/javax/microedition/io/` and is applied to
the pinned FreeJ2ME Web checkout by `scripts/apply-upstream-patches.py`.

## Test principles

- Run the same probe JAR, endpoint, browser, and device across runtime variants.
- Record browser/runtime version, emulator revision, origin, network type, and timestamp with each run.
- Keep expected results separate from observed results.
- Never mark a transport as supported based only on compilation or a successful JavaScript fetch.
- Separate JVM adapter test results from browser and real-game results.

## Planned cases

### HTTP
- GET and POST
- Request and response headers
- Status codes including 2xx, 3xx, 4xx, and 5xx
- Redirect behavior
- Text and binary payloads
- Same-origin success and cross-origin CORS denial
- Connect/read timeout and connection close

### HTTPS
- Valid certificate
- Invalid/expired certificate behavior
- Redirects and CORS
- Binary request/response
- Java ME `SecurityInfo` behavior (currently not implemented)

### TCP
- Client connect and report connection failure
- Server-socket bind and accept
- Bidirectional stream echo
- Partial reads and writes
- Binary payload integrity
- Long-lived connection
- Timeout and remote close
- Disconnect/reconnect
- Multiple simultaneous connections
- Socket option mapping

### UDP
- Datagram send/receive
- Datagram boundary preservation
- `DataInput` / `DataOutput` payload handling
- Packet-size limits
- Timeout and unreachable destination
- Local port binding and explicit destination

### Lifecycle and robustness
- Explicit close
- MIDlet termination while I/O is pending
- Browser refresh/navigation
- Network loss and recovery
- Server restart
- Repeated open/close cycles

## Payload sizes

At minimum: 1 byte, 64 bytes, 1 KiB, 8 KiB, and a larger configurable payload. Record exact bytes sent and received; compare hashes for larger binary payloads.

## Result format

Each case should record:

- Case ID
- Pass/fail/blocked
- Runtime and emulator revision
- Browser and OS/device
- Endpoint and origin
- Start/end time
- Expected behavior
- Observed behavior
- Error/exception
- Payload length and integrity result
- Notes and reproducible steps

## Release gate

A passing JVM test is only JVM evidence. Browser-verified status requires running
the MIDlet against a controlled endpoint in a supported browser with the pinned
CheerpJ loader. Game-verified status requires a real MIDlet using the claimed
transport. Do not infer universal compatibility from the diagnostic probe.
