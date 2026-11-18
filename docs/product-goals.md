# J2ME Web Loader — Product Goals

## Product objective

Build a browser-based Java ME game loader that can be hosted as a static website. The product should run the broadest practical range of Java ME games, including offline and online titles, without requiring a dedicated application install.

The NRO/Teamobi title is one compatibility test case only. It must not dictate the architecture or priorities of the whole product.

## Core principles

- Static-first: the emulator UI and runtime assets are served as static files. A backend is not required for basic offline game execution.
- Broad compatibility: prioritize standard Java ME APIs and document unsupported extensions instead of claiming universal compatibility.
- Per-game isolation: game files, settings, RMS data, and display/input configuration should be managed per title.
- User-owned files: users can import JAR/JAD files and manage their local library.
- Persistent data: preserve game data across reloads where browser storage permits.
- Network transparency: distinguish browser HTTP(S), TCP, UDP, DNS, TLS, and any tunnel/gateway-dependent capabilities. Never report transport creation as proof that a game protocol succeeded.
- Test-driven compatibility: maintain reproducible test games and a compatibility matrix.

## Functional scope

### Runtime and Java ME APIs
- MIDP 1.0 and MIDP 2.0 compatibility where supported by the upstream runtime.
- CLDC 1.0/1.1 behavior where supported by the upstream runtime.
- Canvas, GameCanvas, sprites, tiled layers, fonts, images, and common UI APIs.
- Key events, pointer/touch events, vibration and sound behavior where browser capabilities permit.
- JAR/JAD metadata parsing and per-game launch configuration.
- RMS persistence and lifecycle behavior.

### Web loader
- Import and manage multiple JAR/JAD files.
- Launch, stop, restart, and remove games.
- Virtual phone screen with configurable aspect ratio and scaling.
- On-screen keypad, soft keys, touch controls, and keyboard input.
- Per-game settings and saved data.
- Clear diagnostics for unsupported APIs and runtime errors.

### Networking
- Support the standard networking APIs to the extent the browser runtime can provide them.
- HTTP/HTTPS must be tested separately from raw TCP and UDP.
- TCP/UDP may require a supported browser tunnel or separately operated network component; a static host alone cannot act as a general-purpose raw-socket proxy.
- Tailscale is an optional transport experiment, not a mandatory dependency for offline games or the overall product.
- Keep game-specific server troubleshooting separate from core runtime compatibility work.

## Static hosting target

The deployed website should be distributable as static assets and work on static hosting providers. Any optional network gateway, if required for a particular class of online games, must be clearly separated from the static frontend and must not be a prerequisite for offline execution.

## Compatibility policy

Do not promise that every J2ME game will run. Games may depend on:
- manufacturer-specific APIs or native extensions;
- unsupported MIDP/CLDC behavior;
- proprietary media, DRM, or device services;
- server-specific protocols, ports, DNS behavior, or TLS versions;
- timing, threading, or device-specific behavior.

Record each tested title, device profile, runtime result, API gaps, and network result in a compatibility matrix.

## Priority order

1. Runtime stability and repeatable launch.
2. Standard Java ME API coverage and compatibility tests.
3. Input, display, audio, lifecycle, and RMS correctness.
4. JAR/JAD library management and per-game profiles.
5. Static deployment, storage reliability, and diagnostics.
6. Broad network support and optional gateway integrations.
7. Individual game-specific fixes only when they improve general compatibility or are clearly isolated.

## Acceptance criteria

- A clean static deployment can load the launcher and run an imported offline test game without a project-specific VPS.
- Game data survives page reloads when persistent browser storage is available.
- Multiple games can be imported and launched independently.
- Input, display scaling, and lifecycle behavior are covered by repeatable tests.
- Network diagnostics clearly distinguish connection creation, successful transport, and application-protocol success.
- Known incompatibilities are documented; no universal compatibility claim is made without evidence.
