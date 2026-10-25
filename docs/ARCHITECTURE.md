# Architecture and maintenance map

## App

`MainActivity.java` owns the home scene, focus/remote navigation, dock, app discovery, customizable grid, native settings links, game shortcut, clock and idle transitions. UI is built with Android views and Canvas. Heavy blur/reflection work is cached rather than recomputed on every focus change.

`InputActivity.java` hosts native TV inputs through TvView. Availability and IDs depend on the TV input provider.

`HomeRedirectService.java` listens for Amazon Home window events and launches Clean TV. It is independently opt-in and does not make the stock launcher disableable.

`WallpaperActivity.java` owns gallery, preview and upload-screen lifecycle. `WallpaperStore.java` stores normalized image files, active wallpaper and gallery metadata in private app storage. It imports an existing selected wallpaper once, retaining user data across the public source refresh; private bundled sample-photo migration has been removed.

`WallpaperServer.java` is a Java socket server with an unguessable session URL, request limits and origin checks. It binds to a selected local address; lifecycle/expiry are controlled by the wallpaper activity. Keep server code independent of Android where practical so the existing host test can exercise it.

## Data and assets

Grid ordering, hidden apps, selected columns and wallpaper metadata use local SharedPreferences. Images are private app files. No cloud account is required. Android application backup is disabled in the manifest. Update-compatible signing matters for preserving installed app data.

Public defaults live in res/values/config.xml. Original geometric PNGs are reproducible using tools/GenerateArtwork.java. Outfit font license lives in licenses/Outfit-OFL.txt. Third-party app branding drawn in code or loaded from installed packages remains associated with its respective owner.

## Host tools

Build runs through launcher/gradlew. Installation is separate from Home/accessibility or package changes. device.py and device.sh enforce explicit TV selection. Local journals/backups are ignored and are never initial repository state.

## Extension points

For another TV, inspect settings components and TvInputInfo mappings first. For navigation changes, inspect key routing and focus state in MainActivity. For wallpaper changes, preserve upload lifetime, size limits, image recycling, atomic writes, gallery deletion behavior and existing-photo migration. Update docs and validate the appropriate device behavior.
