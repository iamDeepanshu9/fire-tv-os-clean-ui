#!/bin/sh
set -eu
case "${1:-}" in ''|--build-only) ;; *) echo "Usage: $0 [--build-only]" >&2; exit 2;; esac
# Resolve scripts before changing directory.
if [ "${1:-}" != '--build-only' ]; then
  . "$(dirname "$0")/device.sh"
fi
cd "$(dirname "$0")/.."
# Optional local toolchain; a standard Android SDK/JDK also works.
if [ -z "${JAVA_HOME:-}" ] && [ -d "$PWD/.toolchain/amazon-corretto-21.jdk/Contents/Home" ]; then
  export JAVA_HOME="$PWD/.toolchain/amazon-corretto-21.jdk/Contents/Home"
fi
if [ -n "${JAVA_HOME:-}" ]; then export PATH="$JAVA_HOME/bin:$PATH"; fi
if [ -z "${ANDROID_HOME:-}" ] && [ -d "$PWD/.toolchain/sdk" ]; then
  export ANDROID_HOME="$PWD/.toolchain/sdk"
fi
if [ -d "$PWD/.toolchain/gradle-cache" ]; then export GRADLE_USER_HOME="$PWD/.toolchain/gradle-cache"; fi
(cd launcher && ./gradlew --console=plain assembleDebug)
if [ "${1:-}" != '--build-only' ]; then
  adb -s "$serial" push launcher/app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/clean-tv.apk
  adb -s "$serial" shell pm install -r /data/local/tmp/clean-tv.apk
fi
