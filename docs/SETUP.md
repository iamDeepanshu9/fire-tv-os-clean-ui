# Setup and compatibility

## Known baseline

Development baseline: Xiaomi FX Pro 55 L55MB-FPIN (AFTNE4E43F), Fire OS 8.1.8.5 / Android 11 API 30. Compilation targets API 35 with minimum API 30. Other hardware/firmware is unverified. Historical local device tests used private artwork; the public artwork refresh needs a fresh on-device visual check.

Native settings links use Amazon components. InputActivity uses Android TvView and the TV Input Framework. The game shortcut searches for a top-level HDMI input ending in `/HW4`, which mapped to HDMI 3 on the development TV. Do not assume this mapping on other TVs. Home replacement may be restricted even when an ADB command reports success.

## Permissions

- INTERNET: on-demand local wallpaper HTTP listener.
- ACCESS_NETWORK_STATE / ACCESS_WIFI_STATE: local network state/address and Wi-Fi display.
- Fine/coarse location: Android Wi-Fi SSID access. The launcher does not need location to place the clock or launch apps; Wi-Fi text may be limited when permission is denied.
- Optional accessibility service: Amazon Home window event redirect. It requests no screen content or key filtering. Enabling it is a separate opt-in operation.
- Package queries: discover launcher apps and native TV settings/input providers.

## Device utilities

All mutating ADB helpers require an explicitly selected authorized target. `tools/tv.py` can execute arbitrary shell commands; use it deliberately. `tools/backup.py` saves sensitive device settings under ignored backups/. `tools/disable.py PACKAGE` is an expert tool for individually inspected packages, not a supported universal debloat preset. Its blocklist is incomplete and is not proof a package is safe to disable. It journals verified changes locally; restore uses that journal only.

Before any package changes, inspect dependencies and backup the target. Verify remote, Wi-Fi, Bluetooth, HDMI, audio, DRM, Home and settings afterward. A successful command alone does not establish device health. Avoid factory/service/production menus.

After device work, turn ADB debugging off and revoke authorizations on the TV. Confirm closure from the host; a refusal confirms closure, while timeout/no-route errors do not.
