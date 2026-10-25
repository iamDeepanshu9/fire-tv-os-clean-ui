#!/bin/sh
set -eu
. "$(dirname "$0")/device.sh"
adb -s "$serial" shell pm enable --user 0 com.amazon.tv.launcher
adb -s "$serial" shell cmd package set-home-activity com.amazon.tv.launcher/.ui.HomeActivity_vNext
adb -s "$serial" shell am start -n com.amazon.tv.launcher/.ui.HomeActivity_vNext
