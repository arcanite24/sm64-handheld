# SM64 Handheld

A controller-first Android fork of [VDavid003's SM64 Android port](https://github.com/VDavid003/sm64-port-android-base). This repository tracks the Android wrapper; [sm64-handheld-engine](https://github.com/arcanite24/sm64-handheld-engine) is its engine submodule. [backlog.md](backlog.md) records the path to a casual, fully self-contained handheld experience.

## What this fork changes

- ARM64 build with the upstream DynOS and 60 FPS interpolation patches, widescreen, Better Camera, and texture fixes.
- Default right-stick camera and no on-screen touch controls; controller hot-unplug no longer closes a null haptics handle.
- Immersive gameplay and a 60 Hz display preference on supported handhelds.
- Correct, stable Android `files/user` path for saves and DynOS settings. When installed, the Render96 world and Mario model packs start enabled; their DynOS toggles can return to Classic.
- A landscape setup screen navigable with D-pad/A: import your own `base.zip` through Android's file picker, download the official Render96 archive with a pinned SHA-256 integrity check, choose Classic or Render96, then play.
- Tested local builds on an AYN Thor and Odin 2 Portal. An in-place Thor upgrade preserved its save file.

The current build is a **private development build**. It still needs a locally built `base.zip`. There is no public APK or bundled ROM, model, or texture assets.
The setup app targets Android 8.0+ (API 26); Thor and Odin 2 Portal run newer Android versions.

## Build with your own US ROM

Clone recursively, then put your own verified US `baserom.us.z64` in `app/jni/src/`. The build script checks SHA-1 `9bef1128717f958171a4afac3ed78ee2bb4e86ce` and rejects other versions. The ROM and generated files are ignored by Git.

```powershell
git clone --recursive --branch feat/handheld-foundation https://github.com/arcanite24/sm64-handheld.git
cd sm64-handheld
docker build -t sm64-handheld-builder .
docker run --rm --mount "type=bind,source=$((Get-Location).Path),target=/sm64" sm64-handheld-builder
```

The private outputs are `app/build/outputs/apk/debug/app-debug.apk` and `app/jni/src/build/us_pc/res/base.zip`. The APK is debug-signed; keep one signing key for future upgrades. Copy `base.zip` to a location the device's file picker can open, then choose it on the app's setup screen. The app validates the archive before replacing an existing copy.

For Render96, choose **Install Render96** on the setup screen. The app downloads the [official v3.25 DynOS archive](https://github.com/Render96/ModelPack/releases/tag/3.25), checks its SHA-256, and installs only the world and Mario packs. The model pack has no explicit redistribution license in its repository, so it is deliberately not bundled here.

## Current limits

The inherited in-game remapping menu is still awkward on Android. A ROM-only importer is not yet available, so producing `base.zip` still requires a private build. Controller mappings and long-session frame pacing also need hands-on validation. Follow [backlog.md](backlog.md) for those tasks. Do not publish a generated APK or `base.zip` from a ROM without distribution rights.
