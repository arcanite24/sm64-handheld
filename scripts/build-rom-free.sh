#!/usr/bin/env bash
set -euo pipefail

cd /sm64
if [[ -e app/jni/src/baserom.us.z64 || -e app/jni/src/.assets-local.txt ]]; then
    echo "Use a clean checkout without extracted ROM assets for this build." >&2
    exit 1
fi

ln -nsf /SDL2-2.0.12/src app/jni/SDL/src
ln -nsf /SDL2-2.0.12/include app/jni/SDL/include
printf 'sdk.dir=%s\nndk.dir=%s\n' "$ANDROID_HOME" "$ANDROID_HOME/ndk/21.4.7075529" > local.properties

python3 scripts/prepare-rom-free-native.py
make -C app/jni/src -j8 NOEXTRACT=1 EXTERNAL_DATA=1 \
    build/us_pc/include/text_strings.h \
    build/us_pc/include/level_headers.h \
    build/us_pc/text/us/define_text.inc.c \
    build/us_pc/text/us/define_courses.inc.c
./gradlew assembleDebug

python3 - <<'PY'
from zipfile import ZipFile

with ZipFile("app/build/outputs/apk/debug/app-debug.apk") as apk:
    names = set(apk.namelist())
    assert "lib/arm64-v8a/libmain.so" in names
    assert not any(name.endswith((".z64", "base.zip")) for name in names)
PY

echo "ROM-free APK: app/build/outputs/apk/debug/app-debug.apk"
