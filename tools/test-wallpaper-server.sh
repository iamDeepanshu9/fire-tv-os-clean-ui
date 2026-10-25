#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -n "${JAVA_HOME:-}" ]; then
  tv_java="$JAVA_HOME/bin/"
elif [ -d "$PWD/.toolchain/amazon-corretto-21.jdk/Contents/Home/bin" ]; then
  tv_java="$PWD/.toolchain/amazon-corretto-21.jdk/Contents/Home/bin/"
else
  tv_java=""
fi
test_classes=$(mktemp -d)
trap 'rm -rf "$test_classes"' EXIT
"${tv_java}javac" -d "$test_classes" launcher/app/src/main/java/dev/clean/tvhome/WallpaperServer.java tools/WallpaperServerTest.java
"${tv_java}java" -Dsun.net.http.allowRestrictedHeaders=true -cp "$test_classes" dev.clean.tvhome.WallpaperServerTest
