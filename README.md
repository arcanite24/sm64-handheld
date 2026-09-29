# SM64 Handheld

A controller-first Android fork of [VDavid003's SM64 Android port](https://github.com/VDavid003/sm64-port-android-base). This repository tracks the Android wrapper; [sm64-handheld-engine](https://github.com/arcanite24/sm64-handheld-engine) is its engine submodule. [backlog.md](backlog.md) records the path to a casual, fully self-contained handheld experience.

For the private Android build, see the short [handheld install guide](INSTALL.md).

## What this fork changes

- ARM64 build with the upstream DynOS and 60 FPS interpolation patches, widescreen, Better Camera, and texture fixes.
- Default right-stick camera and no on-screen touch controls; controller hot-unplug no longer closes a null haptics handle.
- Immersive gameplay, a 60 Hz display preference on supported handhelds, and an awake screen during play.
- Correct, stable Android `files/user` path for saves and DynOS settings. When installed, the Render96 world and Mario model packs start enabled; their DynOS toggles can return to Classic.
- Atomic Android save-file replacement, so an interrupted write leaves the previous save intact. Setup also recovers interrupted ROM, archive, and controller-settings replacements on the next launch. Its Export/Import progress buttons move a validated save across reinstallations.
- A landscape setup screen navigable with D-pad/A: choose your verified US ROM and prepare the ARM64 `base.zip` privately on-device, download the official Render96 model and HD texture archives with pinned SHA-256 integrity checks, choose Classic or Render96 visuals, then play.
- In-app [credits and source links](app/src/main/assets/credits.txt) for the Android port, engine, SDL2, and optional model and texture packs.
- A handheld controls screen shows the detected gamepad and a live button map, guides Jump/Attack/Crouch/Pause/Recenter remapping without duplicate gamepad bindings, adjusts camera look speed, vertical inversion, stick dead zone, and trigger press point, toggles Better Camera, and resets the handheld defaults. Settings take effect on the next game launch.
- Launched local builds on an AYN Thor and Odin 2 Portal. Thor controller events verified movement, camera, jump, attack, crouch, and pause; the Odin disconnected before final controller verification. An in-place Thor upgrade preserved its save file. On Thor, the HD installer placed 2,220 PNGs, and a game launch showed visibly sharper textures; the Classic/HD switch was exercised in both directions.

The current build is a **private development build**. Its setup screen prepares game files from a user-owned US ROM; a PC-built `base.zip` is no longer needed for first-run setup. The APK itself now builds from a clean checkout without a ROM. There is no public APK or bundled ROM, model, or texture assets.
The app supports Android 8.0+ (minimum API 26). The v0.6 APK targets API 35 and builds ROM-free with its ARM64 library. Its launcher, controls, and progress picker have been checked on an Android 15 emulator; gameplay with this build still needs a physical-device check.

## Build the ROM-free APK

Clone recursively and build in the pinned Docker toolchain. The build uses checked-in metadata to generate native resource names; it does not need a ROM or extracted assets.

```powershell
git clone --recursive --branch feat/handheld-foundation https://github.com/arcanite24/sm64-handheld.git
cd sm64-handheld
docker build -t sm64-handheld-builder .
docker run --rm --mount "type=bind,source=$((Get-Location).Path),target=/sm64" sm64-handheld-builder bash scripts/build-rom-free.sh
```

The output is `app/build/outputs/apk/debug/app-debug.apk`. It is debug-signed; the build keeps its signing key in ignored `.private/android/debug.keystore` so later local builds can upgrade it. Preserve that key if you move the checkout. Copy your own original US ROM to a location the device's file picker can open, then choose it on the app's setup screen. The app checks SHA-1 `9bef1128717f958171a4afac3ed78ee2bb4e86ce` and reconstructs the graphics and four ARM64 sound files into a private `base.zip` on the device. Mario animations and demo inputs load from that ROM at runtime.

For Render96, choose **Install Render96** on the setup screen. The app downloads the [official v3.25 DynOS archive](https://github.com/Render96/ModelPack/releases/tag/3.25), checks its SHA-256, and installs only the world and Mario packs. The model pack has no explicit redistribution license in its repository, so it is deliberately not bundled here.

For sharper textures, choose **Download HD textures**. The app downloads the [official Render96 HD texture pack v1.3.26.7.7](https://github.com/pokeheadroom/RENDER96-HD-TEXTURE-PACK/releases/tag/1.3.26.7.7), verifies it, and installs its PNG textures into app storage. The download is 374 MiB and installation needs about 400 MiB more, temporarily requiring about 800 MiB free. HD textures may reduce frame rate on slower handhelds. The Classic/HD texture switch takes effect on the next game launch. The pack is downloaded from its authors rather than bundled; its [credits and GPL-3.0 license](https://github.com/pokeheadroom/RENDER96-HD-TEXTURE-PACK) remain with the source project, whose README also describes Nintendo-sourced textures.

The APK bundles neither optional pack. The [Render96 ModelPack repository](https://github.com/Render96/ModelPack) does not state a redistribution license, and the HD pack's own README describes Nintendo-derived material; neither repository's availability is clearance to bundle those assets. The [Android base](https://github.com/VDavid003/sm64-port-android-base) and [sm64ex engine](https://github.com/sm64pc/sm64ex) also do not publish a project-wide license in their repositories. Keep APK distribution private pending a rights review. The app includes its bundled Apache Commons Compress 1.19 notice and license under **Open-source licenses**, with source credits for SDL2 and XZ for Java.

CI builds the full ROM-free Android APK, runs ROM-free asset-decoder checks, and verifies a separately signed release build against its pinned signing certificate. The release key is stored outside the repository; CI does not publish the APK. Existing debug installs use a different certificate: use **Export progress** and keep the backup outside app storage before uninstalling. On an Android 15 emulator, a different test key restored a byte-identical save after reinstalling; a separate reinstall left an old app-storage directory with the wrong owner until app storage was cleared, so confirm the backup before clearing storage if import reports a permission error. The actual ARM64 release and Thor/Odin migration still need testing. Thor's v0.4 debug upgrade preserved its save, and its file picker imported a verified US ROM and built the archive on-device. All 1,897 paths matched the host reference; 224 PNGs differed only in compression and decoded to identical image data. Gameplay with that archive still needs a sustained Thor test before the APK can be published.

## Current limits

The launcher covers common controls, while the inherited advanced in-game menu is still awkward on Android. Thor has launched and played using the on-device-generated archive, but a full playthrough is still pending. Native resource licensing and debug-to-release save migration on physical hardware need final checks. The API 35 ARM64 APK needs a gameplay check on Thor/Odin. The user still reports severe choppiness despite earlier 60 FPS presentation samples; motion and input feel need live reproduction. Follow [backlog.md](backlog.md) for those tasks. Do not publish a generated `base.zip` from a ROM without distribution rights.
