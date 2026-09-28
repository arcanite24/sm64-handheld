# SM64 Handheld backlog

**Goal:** make a casual Super Mario 64 playthrough feel at home on Android handhelds: launch easily, understand the controls, play smoothly, and resume safely. Target the AYN Thor first, then other Android controllers. This fork starts from `sm64ex_nightly`; the local Thor prototype's changes have not been imported yet.

## First: a dependable foundation

- [ ] **Reproduce the Thor build in this fork.** Bring over only the reviewed ARM64, DynOS, 60 FPS, widescreen, better-camera, texture-fix, and immersive-fullscreen changes. Pin the toolchain and build a signed, upgradable APK in CI. Pass: a clean checkout builds and upgrades without losing saves.
- [ ] **Make Render96 actually load.** DynOS reads settings from the app's external `files/user` directory, not its private `files` directory; fix the Android `sys_user_path()` lifetime bug and eliminate manual pack/config copying. Pass: a fresh install shows Render96 Mario and actors in gameplay, and a Classic toggle restores the originals.
- [ ] **Solve first-run ROM setup.** Determine a ROM-free distribution path, ideally an on-device importer that validates a user-supplied US ROM and prepares `base.zip`. Never commit or publish ROM-derived assets. Pass: a user can install the app and start playing without a PC or shell.
- [ ] **Decide asset licensing and delivery.** Verify permissions for Render96 models and texture packs; bundle what may be redistributed and offer a simple in-app download for anything that cannot. Pass: clean install needs no file-manager work and release artifacts include attribution.

## Next: make the handheld experience good

- [ ] **Replace the PC-style settings/remapping flow.** Build a controller-first Android UI with readable type, large targets, clear back/save actions, and a live button diagram. Pass: every action can be completed with the Thor's controls, without touch or a keyboard.
- [ ] **Make controls work out of the box.** Detect the active controller, ship Thor/Odin defaults, show matching button prompts, and support guided remapping, stick dead zones, trigger calibration, and a one-click reset. Hide the touch overlay when a controller is present. Pass: a new player can move, jump, control the camera, pause, and navigate menus immediately.
- [ ] **Choose a modern camera.** Evaluate Puppycam 2 against the current better-camera patch and use the best Android-compatible option. Default to right-stick look, sensible sensitivity, recenter, and no camera surprises in tight spaces. Pass: a full casual playthrough needs no camera configuration.
- [ ] **Create simple visual presets.** Offer Classic and Render96 presets, with an optional sharper-textures setting and a clear performance cost. Preserve a readable HUD and stable 16:9 framing. Pass: switching presets is reversible and survives relaunch.
- [ ] **Tune frame pacing for handhelds.** Keep game speed correct at 60 and 120 Hz, cap the default to a stable 60 FPS, and test battery/thermals on the Thor. Pass: no speed-up, stutter regression, or broken suspend/resume after a 30-minute session.
- [ ] **Make saving and resuming safe.** Preserve SM64's normal progress while recovering cleanly from app switching, screen sleep, and force close. Pass: repeated suspend/resume and app upgrades do not erase or corrupt progress.

## Then: release quality

- [ ] **Test the full game path.** Verify intro, file select, first star, Bowser, credits, controller hot-plug, audio, model switching, and save migration on the Thor; add a small second-device check.
- [ ] **Ship a friendly release.** Publish a stable APK, short install guide, clear ROM-ownership step, model/texture credits, known limitations, and screenshots. Keep the repository and release artifacts free of copyrighted ROM data.
