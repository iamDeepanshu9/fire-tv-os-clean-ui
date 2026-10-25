# Source from device scripts. Require an explicit authorized target.
serial="${TV_SERIAL:-}"
if [ -z "$serial" ] && [ -n "${TV_HOST:-}" ]; then
  serial="${TV_HOST}:${TV_PORT:-5555}"
fi
if [ -z "$serial" ]; then
  echo 'Set TV_SERIAL or TV_HOST to your authorized ADB device first.' >&2
  exit 2
fi
case "$serial" in *[!A-Za-z0-9_.:\[\]-]*) echo 'Invalid TV_SERIAL/TV_HOST.' >&2; exit 2;; esac
