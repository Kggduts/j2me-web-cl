# Security model

## Untrusted MIDlets

Uploaded JAR files must be treated as untrusted input. Browser sandboxing reduces access to the user's local system, but it does not make a MIDlet harmless: a game may still consume CPU, memory, storage, network bandwidth, or abuse any network permissions exposed by the runtime.

The product must not claim that every JAR is safe.

## Network access

- HTTP(S) requests follow browser origin, CORS, TLS, cookie, and mixed-content policies.
- TCP/UDP in CheerpJ require an authenticated Tailscale network (or a separately configured supported backend).
- The emulator overlay uses the interactive `tailscaleLoginUrlCb` flow. Never embed a reusable auth key, API token, or shared secret in public HTML, JavaScript, source maps, or a public repository.
- A logged-in user may be able to reach services permitted by their tailnet ACLs. Do not place the emulator in a privileged tailnet without restrictive ACLs.
- Prefer per-user identity, ephemeral devices, short device expiry, least-privilege ACLs, and straightforward revocation.
- Do not grant all uploaded games access to a privileged private network by default.
- Define connection limits, bandwidth limits, destination policy, logging policy, and abuse response before production deployment.

## Data handling

- Game files should remain in the user's browser storage unless the user explicitly exports or uploads them.
- Do not upload JARs to a server by default.
- If server-side storage is introduced, document retention, deletion, access control, and malware-handling behavior.
- Never log game contents, credentials, auth keys, or full sensitive URLs.

## Runtime lifecycle

- Apply resource limits where the runtime allows them.
- Provide a way to stop a running MIDlet and close its connections.
- Handle page refresh, tab closure, and runtime failure as connection teardown events.
- Verify that background MIDlet threads and open sockets are closed when a game exits.
- Do not claim CPU/memory isolation between games unless the implementation actually provides it.

## Release checklist

- [ ] No secrets in public assets or repository history.
- [ ] Interactive network authentication works in the pinned loader.
- [ ] Tailnet ACLs restrict users and games to intended destinations.
- [ ] Device expiry and revocation are documented.
- [ ] Connection and bandwidth limits are defined.
- [ ] MIDlet stop/close behavior is tested.
- [ ] Browser security and privacy behavior is documented.
- [ ] JAR handling and retention policy is documented.
