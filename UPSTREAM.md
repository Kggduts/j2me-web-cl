# Upstream source

This project uses [zb3/freej2me-web](https://github.com/zb3/freej2me-web) as its emulator baseline.

- Repository: https://github.com/zb3/freej2me-web
- Branch at evaluation time: `main`
- Pinned commit: `c19416e75cbc15f9a27f7e967ee81cb108761e30`
- Commit page: https://github.com/zb3/freej2me-web/commit/c19416e75cbc15f9a27f7e967ee81cb108761e30

The upstream repository is checked out at this exact commit during the emulator build. The build uses its documented Docker/Ant toolchain. The resulting web runtime is packaged as a CI artifact; upstream source is not copied into this repository.

The upstream project is GPL-3.0-or-later and includes a separate ObjectWeb ASM license notice. Preserve upstream license and attribution files when redistributing the built runtime. Review the exact upstream license files and dependency notices before public distribution.

To update the pinned revision, review the upstream diff, re-run the build and compatibility checks, then change the commit in this file and in the build workflow together.
