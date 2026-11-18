# Java ME compatibility inventory

**Baseline:** FreeJ2ME Web commit `c19416e75cbc15f9a27f7e967ee81cb108761e30`, as pinned by `scripts/build-emulator.sh`.

This inventory is a source-level map, not a claim that every API is fully implemented or every game works. Presence of a Java class proves only that the class exists in the pinned source. Each area still needs behavioral tests in the browser runtime.

## Existing source coverage in the pinned upstream

| Area | Source presence | Current project evidence | Next validation |
|---|---|---|---|
| MIDlet lifecycle | `javax.microedition.midlet` | Emulator build succeeds; no lifecycle regression suite | start/pause/destroy, exception paths, restart |
| LCDUI | `javax.microedition.lcdui` | Existing emulator UI and keypad | Canvas repaint, Forms, commands, fonts, image clipping |
| Game API | `javax.microedition.lcdui.game` | Upstream classes present | Sprite transforms/collisions, LayerManager, GameCanvas flush |
| RMS | `javax.microedition.rms` | Upstream classes present; persistence not browser-verified | create/open/list/delete, record CRUD, enumeration, listeners, reload persistence |
| GCF HTTP/HTTPS | `javax.microedition.io` | Project adapter overlay + JVM smoke tests | browser CORS, redirects, binary streams, TLS, error mapping |
| GCF TCP | `SocketConnection`, `ServerSocketConnection` | Project adapter overlay + JVM smoke tests | CheerpJ browser connection, reconnect, half-close, timeout, concurrent sockets |
| GCF UDP | `DatagramConnection` | Project adapter overlay + JVM smoke tests | browser transport, datagram boundaries, packet loss and timeout |
| JSR-135 media | `javax.microedition.media` | Source classes present | audio formats, looping, volume, lifecycle, browser autoplay policy |
| JSR-184 M3G | `javax.microedition.m3g` | Source classes present | representative 3D MIDlets and rendering parity |
| JSR-75 file connection | `javax.microedition.io.file` | Source classes present | sandboxed virtual filesystem and persistence |
| JSR-82 Bluetooth | No verified browser transport | Not established | explicitly unsupported unless a safe browser-compatible implementation exists |
| JSR-120/205 messaging | No verified real SMS transport | Existing behavior must not be represented as carrier SMS | document unsupported/simulated behavior |
| Vendor APIs | Nokia and Siemens namespaces present | Source presence only | build per-vendor compatibility tests; verify device-specific semantics |
| Push registry | API class present | No verified browser push transport | registration lifecycle and browser constraints |

## Runtime test tiers

- **Tier 0 — source inventory:** class/package exists in the pinned source.
- **Tier 1 — build verified:** project compiles with the API.
- **Tier 2 — JVM verified:** deterministic unit/smoke tests pass outside the browser.
- **Tier 3 — browser verified:** tests pass in the deployed static player on supported browsers.
- **Tier 4 — game verified:** real MIDlets use the feature successfully, with a reproducible test record.

Do not promote an API to a higher tier based only on source presence or successful compilation.

## Compatibility test suite to add

1. **MIDlet lifecycle:** launch, pause/resume, destroy, restart, uncaught exception and cleanup.
2. **Graphics:** Canvas dimensions, clipping, transforms, alpha, image decode, sprite frame/transform/collision and GameCanvas flush.
3. **Input:** numeric keys, soft keys, arrows, fire, backspace, key repeat, pointer press/drag/release, multi-touch.
4. **RMS:** independent stores per MIDlet/vendor/suite, record CRUD, enumeration ordering, listeners, capacity errors, persistence after reload.
5. **Media:** tone sequence, short WAV/MIDI/AMR where supported, loop/stop/volume and cleanup.
6. **Files and descriptors:** JAR/JAD import, manifest parsing, attributes, suite identity, duplicate names and malformed archives.
7. **Networking:** HTTP/HTTPS/TCP/UDP tests kept separate; distinguish socket connection from application-protocol success.
8. **Vendor APIs:** representative Nokia and Siemens test MIDlets; every unsupported method must fail predictably rather than crash unpredictably.
9. **Storage and isolation:** two games with same RMS store name must not share data; remove/export/import behavior.
10. **Mobile browsers:** Android Chrome and Samsung Internet; touch input, screen resize, tab suspension, memory pressure and page reload.

## Release blockers

- No repeatable browser-based offline game test has yet been recorded in this inventory.
- RMS persistence across page reloads is not yet demonstrated.
- Browser media behavior is not yet demonstrated.
- Broad networking is not available from static hosting alone: browser policy and the selected runtime transport determine what can work. Any gateway must be optional and separated from the static player.
- Vendor extensions and device-specific behavior require per-title verification.

## Product rule

An API is not “supported” merely because its class compiles. Mark support only at the highest test tier actually passed, with browser and game evidence attached.
