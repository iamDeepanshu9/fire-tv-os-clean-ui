# Contributing

Start with README.md and docs/ARCHITECTURE.md. Keep changes focused and explain the user-visible problem, resulting behavior, validation and hardware tested.

Use JDK 17 or 21, Android SDK 35 and the pinned Gradle wrapper. Run the relevant commands in docs/TESTING.md. UI changes need D-pad and lifecycle checks on a TV or suitable emulator; document any untested hardware behavior.

Keep Java 17/API 30 compatibility. Avoid blocking I/O or repeated bitmap work during navigation. Preserve saved preferences and gallery data across updates. Add meaningful tests for server/security behavior; avoid tests that merely repeat implementation details.

Do not submit private logs, photos, network details, keys, APK backups or copied proprietary artwork. Keep dependency and asset notices accurate. Contributions are provided under this project's MIT license, with separately licensed third-party materials identified in notices.
