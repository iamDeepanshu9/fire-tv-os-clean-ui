# Validation

Run from the repository root with JDK 17 or 21 and Android SDK 35 configured:

```sh
./tools/build-and-install.sh --build-only
(cd launcher && ./gradlew --console=plain lintDebug)
./tools/test-wallpaper-server.sh
python3 -m compileall -q tools
```

The wallpaper test uses a loopback socket and checks the upload page, wrong session path, cross-origin rejection, invalid-image callback, binary delivery, oversized upload rejection and closure. It does not replace Android image-decoder or device lifecycle tests.

Without TV_SERIAL and TV_HOST, device helpers must fail before contacting ADB. Fresh checkout tests must not rely on ignored backups, journals, SDKs or artwork. Configure a normal external Android SDK to verify portability. Keep generated output out of Git.

## Device checklist (only with explicit authorization)

- Install and direct-launch; navigate dock, top controls, game card and All Apps using only D-pad/OK/Back/Menu.
- Check final partial grid row, scrolling, reorder, hidden-app restore, empty grid and saved state after restart/update.
- Verify Home redirect opt-in/undo preserves other accessibility services; check the physical Home button after reboot.
- Exercise native settings and each required input, including actual picture/audio and return navigation.
- Wait for idle; confirm first key wakes without accidentally launching an app.
- Upload JPEG/PNG, verify orientation/crop, preview/apply/delete, default fallback and persistence. Test rejected/oversized uploads.
- Leave upload screen, background app and wait for expiry; confirm old URLs stop working. Check deletion does not recreate migrated images.
- Deny location and verify launcher still works. Confirm normal operation needs no cloud access.
- Close ADB after testing and confirm connection refusal.

Report which checks ran, exact commands and limitations. Never label the public asset refresh as hardware-verified based only on earlier private builds.

## Public preparation validation — 25 October 2026

- Debug APK build passed in the working checkout and in an isolated snapshot of the staged public files. The isolated build used an explicitly configured external SDK/JDK and existing dependency cache; no ignored project files were copied.
- Android lint completed with zero errors and 36 warnings: ApplySharedPref, DataExtractionRules, DiscouragedApi, DrawAllocation, NotifyDataSetChanged, RtlHardcoded, SetTextI18n and VectorRaster. These remain maintenance work; lint success does not mean warning-free code.
- All seven wallpaper-server checks passed. Python compilation and shell syntax checks passed.
- All seven device entry points rejected a missing target. Target validation accepted representative network/emulator/USB serials and rejected shell metacharacters and spaces.
- Staged paths excluded device records, private artwork, generated builds, local.properties and keys. A staged text scan found no obvious credential patterns or personal device addresses/paths; the author's name remains in the license attribution.
- No device was contacted or changed during preparation. The public artwork refresh has not been visually verified on the TV. A brand-new dependency download and Windows execution were not tested.
