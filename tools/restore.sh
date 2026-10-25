#!/bin/sh
# Re-enable only packages recorded by this project. Existing disabled apps are untouched.
set -eu
. "$(dirname "$0")/device.sh"
cd "$(dirname "$0")/.."
if [ -f backups/home-helper-before.json ]; then
  python3 tools/home-helper.py disable
fi
if [ ! -f disabled-by-us.txt ]; then
  echo 'No local disable journal found; no packages to restore.'
  exit 0
fi
while IFS= read -r package; do
  case "$package" in ''|'#'*) continue;; esac
  adb -s "$serial" shell pm enable --user 0 "$package"
done < disabled-by-us.txt
adb -s "$serial" shell pm list packages -d
