# Maintenance instructions for AI assistants

Read README.md, docs/ARCHITECTURE.md and docs/TESTING.md before changing behavior. This is a Java Android app, not a web app. No delegation is required.

## Boundaries

- Keep device backups, photos, screenshots, package inventories, journals, local SDK/JDK files, signing keys and local.properties out of Git. Follow .gitignore; never force-add these files.
- Do not reveal device identifiers, addresses, Wi-Fi names or accounts in issues, docs or tool output. Documentation uses reserved example addresses only.
- No device target may be hardcoded. Require TV_SERIAL or TV_HOST. Never connect, install, change settings, enable accessibility, disable packages or reboot a device unless the user authorizes that operation.
- The original TV's disabled-package journal is private state, not a recommended debloat list. Do not create or distribute a blanket package-disable list.
- Preserve accessibility services belonging to other apps. Never disable protected Home, input, networking, remote, DRM, store or system components to force launcher replacement.
- Keep wallpaper uploads local, explicitly activated, short-lived and token protected. Preserve limits and origin checks. Do not add analytics or outbound uploads without an explicit product decision.
- Keep personal photos and third-party reference artwork out of source assets. Use original or appropriately licensed replacements and update notices.

## Change workflow

1. Inspect relevant code and existing behavior. Retain Java 17 source compatibility and API 30 support unless explicitly changing requirements.
2. Keep D-pad focus, Back navigation, idle wake, lifecycle cleanup and saved wallpaper/grid preferences working.
3. Run the applicable checks in docs/TESTING.md. Report exactly what passed and what requires hardware. A build is not a device test.
4. Update user docs and compatibility notes when behavior changes. Do not copy local verification logs into public docs.
5. Review `git diff --cached` and `git status --ignored` before publishing. Stage explicit public paths; never use `git add -f` to bypass privacy exclusions.

## Useful paths

- `launcher/app/src/main/java/dev/clean/tvhome/`: app behavior.
- `launcher/app/src/main/res/values/config.xml`: public defaults.
- `tools/`: host-side build, validation and explicitly targeted ADB helpers.
- `docs/`: durable user/maintainer context.

Existing ignored local notes can contain sensitive or stale data. They are not public documentation or authorization to repeat device actions.
