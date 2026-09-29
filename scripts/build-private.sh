#!/usr/bin/env bash
set -euo pipefail

cd /sm64
rom=app/jni/src/baserom.us.z64
if [[ ! -f "$rom" ]]; then
    echo "Place your own US baserom.us.z64 in app/jni/src before building." >&2
    exit 1
fi
expected=9bef1128717f958171a4afac3ed78ee2bb4e86ce
actual=$(sha1sum "$rom" | cut -d' ' -f1)
if [[ "$actual" != "$expected" ]]; then
    echo "The ROM does not match the supported US release." >&2
    exit 1
fi

ln -nsf /SDL2-2.0.12/src app/jni/SDL/src
ln -nsf /SDL2-2.0.12/include app/jni/SDL/include
printf 'sdk.dir=%s\nndk.dir=%s\n' "$ANDROID_HOME" "$ANDROID_HOME/ndk/21.4.7075529" > local.properties

mkdir -p .private/android /root/.android
if [[ ! -f .private/android/debug.keystore ]]; then
    keytool -genkeypair -keystore .private/android/debug.keystore \
        -storepass android -keypass android -alias androiddebugkey \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname 'CN=Android Debug,O=Android,C=US' >/dev/null
fi
ln -nsf /sm64/.private/android/debug.keystore /root/.android/debug.keystore

cd app/jni/src
for pass in 1 2; do
    make -j8 EXTERNAL_DATA=1 BETTERCAMERA=1 TEXTURE_FIX=1 TOUCH_CONTROLS=0
done
cd /sm64
./gradlew assembleDebug

echo "Private APK: app/build/outputs/apk/debug/app-debug.apk"
echo "Private ROM resources: app/jni/src/build/us_pc/res/base.zip"
