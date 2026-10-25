# Privacy and local networking

The launcher discovers installed launchable apps, reads local network state/SSID when available, and stores app-grid preferences and wallpaper photos locally. No analytics, cloud sign-in or intentional outbound network requests are implemented in the app. Dependencies and platform behavior should be reviewed when updating them.

The wallpaper upload feature opens a local HTTP server only from its upload screen. It uses a random port and unguessable per-session URL, closes on leaving that screen and expires after ten minutes. The link grants temporary upload access: do not publish it. HTTP is unencrypted, so a party able to observe local traffic may see photos or the URL. Use a trusted local network and no port forwarding.

Optional accessibility observes Amazon Home window events to redirect Home. It does not request screen contents or key-event filtering. Users can disable it independently.

Host-side ADB backups can contain account/device/network information even if the app itself does not upload it. .gitignore excludes backups, screenshots, inventories, journals, local paths and signing keys. Inspect every proposed attachment and staged file; ignore rules do not sanitize files you explicitly force-add.

Deleting a gallery item removes the app's saved file and resets to default if active. Uninstalling clears private app data. OS/filesystem recovery behavior is outside the app's deletion guarantee.
