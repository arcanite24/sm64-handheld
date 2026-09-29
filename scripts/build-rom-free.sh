#!/usr/bin/env bash
set -euo pipefail

cd /sm64

variant=${1:-Debug}
case "$variant" in
    Debug|Release) ;;
    *) echo "Build variant must be Debug or Release." >&2; exit 1 ;;
esac
if [[ "$variant" == Release ]]; then
    for name in SM64_RELEASE_STORE_FILE SM64_RELEASE_STORE_PASSWORD SM64_RELEASE_KEY_ALIAS SM64_RELEASE_KEY_PASSWORD; do
        if [[ -z "${!name:-}" ]]; then
            echo "Missing release signing input: $name" >&2
            exit 1
        fi
    done
    if [[ ! -f "$SM64_RELEASE_STORE_FILE" ]]; then
        echo "Release keystore file is unavailable." >&2
        exit 1
    fi
fi
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
./gradlew "assemble$variant"

apk="app/build/outputs/apk/${variant,,}/app-${variant,,}.apk"
export SM64_APK_PATH="$apk"

python3 - <<'PY'
import os
from zipfile import ZipFile

with ZipFile(os.environ["SM64_APK_PATH"]) as apk:
    names = set(apk.namelist())
    assert "lib/arm64-v8a/libmain.so" in names
    assert not any(name.endswith((".z64", "base.zip")) for name in names)
PY

if [[ "$variant" == Release ]]; then
    "$ANDROID_HOME/build-tools/28.0.2/apksigner" verify --print-certs "$apk" |
        grep -Fq 'Signer #1 certificate SHA-256 digest: 442659335fcffec89b239cdb40456fac500202b3ace608ba7a26d5011c9d14fc'
fi
echo "ROM-free APK: $apk"
