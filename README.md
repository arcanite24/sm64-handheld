# SM64 Handheld

A controller-first Android fork of [VDavid003's SM64 Android port](https://github.com/VDavid003/sm64-port-android-base). This repository tracks the Android wrapper; [sm64-handheld-engine](https://github.com/arcanite24/sm64-handheld-engine) is its engine submodule. [backlog.md](backlog.md) records the path to a casual, fully self-contained handheld experience.

## What this fork changes

- ARM64 build with the upstream DynOS and 60 FPS interpolation patches, widescreen, Better Camera, and texture fixes.
- Default right-stick camera and no on-screen touch controls; controller hot-unplug no longer closes a null haptics handle.
- Immersive gameplay, a 60 Hz display preference on supported handhelds, and an awake screen during play.
- Correct, stable Android `files/user` path for saves and DynOS settings. When installed, the Render96 world and Mario model packs start enabled; their DynOS toggles can return to Classic.
- Atomic Android save-file replacement, so an interrupted write leaves the previous save intact.
- A landscape setup screen navigable with D-pad/A: import your own `base.zip` through Android's file picker, download the official Render96 model and HD texture archives with pinned SHA-256 integrity checks, choose Classic or Render96 visuals, then play.
- A handheld controls screen shows the detected gamepad and a live button map, guides Jump/Attack/Crouch/Pause/Recenter remapping without duplicate gamepad bindings, adjusts camera look speed, vertical inversion, stick dead zone, and trigger press point, toggles Better Camera, and resets the handheld defaults. Settings take effect on the next game launch.
- Launched local builds on an AYN Thor and Odin 2 Portal. Thor controller events verified movement, camera, jump, attack, crouch, and pause; the Odin disconnected before final controller verification. An in-place Thor upgrade preserved its save file. On Thor, the HD installer placed 2,220 PNGs, and a game launch showed visibly sharper textures; the Classic/HD switch was exercised in both directions.

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

For sharper textures, choose **Download HD textures**. The app downloads the [official Render96 HD texture pack v1.3.26.7.7](https://github.com/pokeheadroom/RENDER96-HD-TEXTURE-PACK/releases/tag/1.3.26.7.7), verifies it, and installs its PNG textures into app storage. The download is 374 MiB and installation needs about 400 MiB more, temporarily requiring about 800 MiB free. HD textures may reduce frame rate on slower handhelds. The Classic/HD texture switch takes effect on the next game launch. The pack is downloaded from its authors rather than bundled; its [credits and GPL-3.0 license](https://github.com/pokeheadroom/RENDER96-HD-TEXTURE-PACK) remain with the source project, whose README also describes Nintendo-sourced textures.

CI compiles the Android setup UI without ROM data. It cannot produce a playable APK because the native engine build and `base.zip` require a user-owned ROM.

## Current limits

The launcher covers common controls, while the inherited advanced in-game menu is still awkward on Android. A ROM-only importer is not yet available, so producing `base.zip` still requires a private build. Physical control feel and long-session frame pacing also need hands-on validation. Follow [backlog.md](backlog.md) for those tasks. Do not publish a generated APK or `base.zip` from a ROM without distribution rights.
