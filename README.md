# Clean TV

A native Android TV launcher with D-pad navigation, a favourites dock, a customizable app grid, HDMI input access, and a local wallpaper gallery with Wi-Fi uploads.

Initially developed on a Xiaomi FX Pro 55 (L55MB-FPIN / AFTNE4E43F), Fire OS 8.1.8.5, Android 11. Other televisions are unverified. Android 11 (API 30) is the minimum; Fire OS settings links, the Home redirect, and the game shortcut are device-specific.

## Features

- Clock, configurable greeting, floating dock and idle wallpaper view.
- All Apps: reorder, hide/restore apps and choose 4–7 columns.
- Wallpaper gallery: preview, apply, delete and upload images from another device on the same network.
- Input picker through Android's TV Input Framework.
- Optional accessibility helper that redirects Amazon Home to this launcher.
- Explicitly targeted ADB tools for installation, backups and restoration.

The public build includes original geometric wallpaper and controller artwork. Private reference images, photos, device backups and screenshots are excluded.

## Build

Install JDK 17 or 21 and Android SDK platform 35, build-tools 35.0.0 and platform-tools. Set `JAVA_HOME` and `ANDROID_HOME` for your installation, or set `sdk.dir` in ignored `launcher/local.properties`. Accept Android SDK licenses through your SDK installation. Gradle 8.13 is pinned by the included wrapper; its first run downloads Gradle and dependencies.

```sh
./tools/build-and-install.sh --build-only
```

Output: `launcher/app/build/outputs/apk/debug/app-debug.apk`. Android debug signing is for development; retain your local debug key for updates and never publish it. A differently signed APK cannot update an existing installation without uninstalling it, which removes app data.

## Install on your own TV

Enable and authorize ADB on your TV, install `adb` on your computer, and choose the exact device listed by `adb devices`. For a network device, connect to its actual IP address first. Replace the example address below with your own.

```sh
adb connect 192.0.2.10:5555
export TV_SERIAL=192.0.2.10:5555
./tools/build-and-install.sh
adb -s "$TV_SERIAL" shell am start -n dev.clean.tvhome/.MainActivity
```

Alternatively set `TV_HOST` and optional `TV_PORT` (default 5555). The scripts refuse to select a TV without an explicit target. Installation does not change Home defaults or disable packages.

To opt into the Amazon Home accessibility redirect after installing:

```sh
python3 tools/home-helper.py enable
# Undo the redirect:
python3 tools/home-helper.py disable
```

The helper requires ADB authorization to change its setting. Other enabled accessibility services are preserved. Fire OS may restrict default Home replacement; do not disable its protected stock launcher. Read [setup and compatibility](docs/SETUP.md) before changing Home behavior or packages.

After testing, turn debugging off in the TV's Developer options, revoke debugging authorizations and verify closure with `python3 tools/adb-closed.py YOUR_TV_IP`. A timeout is inconclusive. Future ADB operations require authorization again.

## Use and customize

On Home, Up from the dock opens the top controls; Down reaches the game card or All Apps depending on focus. Back returns from All Apps. In All Apps, hold OK or press Menu to customize order, hidden apps and column count. The first navigation key after idle wakes the interface.

Settings → Wallpaper opens the gallery. Choose Upload wallpaper, then open its QR code/link on a device on the same network. Uploads are limited to 20 MiB, decoded with orientation handling, and reduced to at most 2560 pixels on the longest side. JPEG and PNG are recommended. Select a saved photo and choose Set wallpaper to apply it. The upload endpoint runs only while the upload screen is foreground, expires after ten minutes, and closes on exit.

Edit `launcher/app/src/main/res/values/config.xml` for the default greeting, dock packages, column count and idle timeout. Users' saved grid preferences override grid defaults. The game card currently targets the original TV's HDMI input ID suffix `/HW4`; adjust input selection in `MainActivity.java` for your hardware.

## Documentation

- [Setup, permissions and compatibility](docs/SETUP.md)
- [Architecture and maintenance](docs/ARCHITECTURE.md)
- [Validation and device test checklist](docs/TESTING.md)
- [Privacy and security model](docs/PRIVACY.md)
- [Artwork and dependency notices](docs/ASSETS.md)
- [Troubleshooting and support](SUPPORT.md)
- [Contributing](CONTRIBUTING.md) · [AI assistant guidance](AGENTS.md) · [Security reports](SECURITY.md)

Code and original generated geometric assets are MIT licensed; the Outfit font retains its SIL Open Font License. See [LICENSE](LICENSE), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and `licenses/`. Product names identify compatibility and do not imply affiliation.
