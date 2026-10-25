# Public artwork

The public repository contains original programmatically drawn geometric artwork, without using private reference photos:

- `launcher/app/src/main/res/drawable-nodpi/cyberpunk.png`: abstract gradient/orbit landscape. The historical filename remains for resource compatibility.
- `launcher/app/src/main/res/drawable-nodpi/gamepad.png`: unbranded geometric gamepad illustration.
- Existing XML icons/banner: native vector drawings.

The original geometric PNGs are covered by the project MIT license and can be reproduced from the repository root:

```sh
java -Djava.awt.headless=true tools/GenerateArtwork.java
```

Outfit font is bundled under the SIL Open Font License, retained in licenses/Outfit-OFL.txt. Private reference images, earlier wallpapers/controller artwork, and personal screenshots are not distributed. Users may upload their own photos locally; they are responsible for permission to share those photos separately.

App/service/product names and their branding identify installed apps or compatibility. This project is independent of those vendors; its license does not grant rights to their trademarks or third-party artwork.
