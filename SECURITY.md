# Security reports

Do not post exploitable details, credentials, photos, upload tokens or private device dumps in a public issue. Once the GitHub repository supports private vulnerability reporting, use its Security → Report a vulnerability entry. Until then, open a minimal issue requesting a private reporting channel without sensitive details.

There is no published support lifetime or security response SLA. Report against the latest source and include a minimal reproduction with synthetic data.

Security-sensitive areas include the local wallpaper HTTP server, image decoding, accessibility helper and host-side ADB tools. See docs/PRIVACY.md and docs/TESTING.md. The wallpaper endpoint uses plaintext local HTTP: its random URL is a temporary capability and must be kept private. Use only a trusted local network.
