# Network test server

`netprobe-server.py` starts three local test endpoints:

| Transport | Default endpoint | Behavior |
| --- | --- | --- |
| HTTP | `http://127.0.0.1:8765/health` | JSON health response |
| HTTP | `http://127.0.0.1:8765/status/418` | Configurable status-code response |
| HTTP | `http://127.0.0.1:8765/echo` | POST body echo, max 1 MiB |
| TCP | `127.0.0.1:8766` | Binary stream echo |
| UDP | `127.0.0.1:8767` | Binary datagram echo |

Run with Python 3:

```sh
python3 tools/netprobe-server.py
```

The default bind address is loopback. To test from a CheerpJ browser joined to the same Tailscale network, bind to the server's Tailscale IP:

```sh
python3 tools/netprobe-server.py --host 100.x.y.z
```

Replace the example with the machine's actual Tailscale IPv4 address. Do not use `0.0.0.0` on an internet-connected machine unless its firewall explicitly restricts access. The service has no authentication and must not be exposed to the public internet.

For a browser cross-origin test, pass exactly the origin serving the diagnostic page:

```sh
python3 tools/netprobe-server.py --host 100.x.y.z --cors-origin https://your-site.example
```

The HTTP endpoint handles preflight requests and allows only that exact origin. TCP and UDP endpoints do not implement authentication, encryption, or application-level framing; use them only inside a controlled test network.

The server logs client IP and request metadata, not payload contents. Limits are 1 MiB per HTTP POST and 16 MiB per TCP connection; TCP connections also have an idle timeout.
