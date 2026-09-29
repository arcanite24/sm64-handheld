# Install on an Android handheld

This is a private test build; no public APK is available yet. Build the ROM-free APK using the [README](README.md#build-the-rom-free-apk), or use an APK supplied by the project owner. The APK contains no game ROM or generated game archive.

1. If this app is already installed, tap **Export progress** and save the backup outside the app's storage before updating.
2. Install the APK on the handheld. An update over the existing debug build requires the same signing key; the local build script creates and reuses the ignored `.private/android/debug.keystore`.
3. Put your own original **US Super Mario 64 ROM** somewhere the Android file picker can reach. In the app, tap **Choose US ROM & prepare game**. The app verifies the 8 MB ROM and prepares its game files on the handheld.
4. Open **Controls & camera** to see the detected gamepad and button map. The default right stick looks around, L1 recenters, and L2 crouches. Tap **Play Super Mario 64**.
5. Optional: tap **Install Render96** for models or **Download HD textures** for sharper textures. Each download is verified before installation. You can return to Classic models or textures in the launcher; changes take effect on the next game launch. HD textures need about 800 MiB free during installation and may affect performance.

If an APK with a different signing certificate is installed, Android requires uninstalling it before installing this one. Export progress first, keep the backup outside app storage, then reinstall, prepare the game from your ROM again, and tap **Import progress**. If an old app-storage directory prevents import after reinstall, confirm the outside backup exists before clearing this app's storage and importing again.

The current build still needs a Thor gameplay and smoothness check, an Odin controller check, and a full playthrough. Do not redistribute your ROM, the generated `base.zip`, or downloaded model and texture packs.
