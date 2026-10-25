# Support and troubleshooting

Use the repository's Issues tab for bugs and questions once it is hosted. Maintainer support is best effort; no response time or support for every TV model is guaranteed.

Include the TV model, Android/Fire OS version, app version, steps to reproduce, expected/actual behavior and whether the issue occurs after restarting the launcher. Attach only redacted logs or screenshots. Never share account data, Wi-Fi credentials, device serial numbers, upload URLs/tokens or full settings dumps.

## Common problems

- **Build cannot find Java/SDK:** set JAVA_HOME and ANDROID_HOME or ignored launcher/local.properties. Install platform 35 and build-tools 35.0.0 and accept SDK licenses.
- **No ADB target:** set TV_SERIAL to an authorized device from `adb devices`, or set TV_HOST. If offline/unauthorized, reconnect and approve the TV prompt. Do not guess a device.
- **Install signature mismatch:** use the original local signing key. Uninstalling removes saved preferences and wallpapers; do this only if you accept that loss.
- **Home still opens Amazon:** direct launch and accessibility redirect are separate. Opt into the helper explicitly. Fire OS protects stock Home on some versions; do not disable protected packages.
- **Settings or HDMI shortcut fails:** native settings components and `/HW4` input mapping are specific to the original TV. Use the input menu and inspect compatibility before changing mappings.
- **Upload link does not open:** leave the upload screen visible and use the same local network. Guest/client isolation, VPNs or expiry may prevent access. Reopen Upload wallpaper for a fresh URL. Do not expose it through router port forwarding.
- **Empty screenshot on HDMI/video:** hardware video/DRM may block capture. Inspect the focused activity rather than assuming playback failed.
- **Missing apps:** restore hidden apps from All Apps customization, check installation/enabled state, then restart the launcher to refresh discovery.

## Restore

Disable the helper with `python3 tools/home-helper.py disable`. For packages disabled using this checkout, `./tools/restore.sh` uses only the local ignored `disabled-by-us.txt` journal; it cannot restore a journal from another machine. Never copy another person's journal. `./tools/restore-home.sh` targets the original Amazon Home component and is device-specific. Reauthorize ADB before running these tools.

See SECURITY.md for sensitive vulnerability reports.
