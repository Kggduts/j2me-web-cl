# Initial runtime source audit

**Audit baseline:** pinned FreeJ2ME Web revision `c19416e75cbc15f9a27f7e967ee81cb108761e30`.

This is a static source review. Findings below describe code visible in the pinned upstream source; they are not browser-runtime test results.

## Confirmed RMS implementation details

File: `src/javax/microedition/rms/RecordStore.java`

- RMS files are written under `Mobile.getPlatform().dataPath + "/rms"`.
- Record stores are serialized to ordinary files using `FileOutputStream`.
- The vendor/suite overload `openRecordStore(name, vendorName, suiteName)` currently constructs a store using only `recordStoreName`; the vendor and suite parameters are not used to namespace storage.
- The `authmode/writable` overload ignores those access arguments.
- `setMode` is empty.
- `closeRecordStore` is empty.
- `getSize()` and `getSizeAvailable()` return fixed constants rather than measuring browser quota or actual stored bytes.
- The source does not show a browser Storage API bridge in this class. Persistence across a browser reload therefore depends on how CheerpJ maps the runtime filesystem; this must be tested in the deployed browser.

### RMS priority

Treat RMS as a high-priority compatibility area because many games depend on it for saves, account/session state, settings, and progression.

Before changing implementation, add regression tests for:
1. Store identity isolation across vendor/suite combinations.
2. Concurrent open handles and stale-write behavior.
3. Add/get/set/delete and record enumeration.
4. Record listener ordering.
5. Close/reopen behavior.
6. Persistence after reload and browser storage quota/error handling.
7. Removal/export/import of a game's saved data.

Do not change the on-disk format without a migration plan for existing saves.

## Network adapter observations

File: `patches/src/javax/microedition/io/GcfNetwork.java`

- `Connector.open()` logs `OPEN_OK` immediately after constructing the adapter. For HTTP this does not mean a request has been sent or a response received.
- TCP `OPEN_OK SOCKET` is emitted after the Java socket connect call returns, so it confirms transport establishment but not the game protocol.
- Stream-opening methods wrap checked I/O failures in `IllegalStateException`, which needs compatibility tests against MIDlets expecting Java ME-specific exception behavior.
- The current socket adapter uses a 30-second connect timeout when timeout support is enabled.
- UDP uses Java datagram sockets. Actual browser/Tailscale behavior remains a separate verification gate.

## Project-level conclusions

1. The pinned upstream already contains a broad set of Java ME API classes, including LCDUI, Game API, RMS, media, M3G, file connection, and Nokia/Siemens namespaces. Class presence alone does not establish API correctness.
2. RMS isolation and browser persistence deserve attention before adding more game-specific network work.
3. Maintain a distinction between source inventory, build success, JVM tests, browser tests, and real-game tests.
4. The NRO connection issue remains a single compatibility case and should not block offline runtime improvements.
