# GOOLVPN Connect release checklist

Use this checklist before publishing any APK. It is intentionally independent
from the build command and does not authorize publication by itself.

## GPL and source

- Publish the corresponding source for the exact APK revision in a public,
  stable location.
- Include the GPLv3-or-later license and retain upstream SFA/sing-box notices.
- Link the source, license and release notes from the download page and release
  announcement.
- Do not publish an APK until the source link resolves publicly.

## Artifact and updater

- Build the signed arm64 release with the permanent signing key.
- Check package name, v1/v2 signatures, certificate fingerprint, version code,
  SHA-256 and manifest URL.
- Upload using `scripts/deploy_update.ps1 -Version X.Y.Z -Publish`.
- Verify the public download URL, manifest fields and downloaded APK hash.

## Android smoke checks

- Activate using a fresh Telegram link and confirm fallback guidance.
- Verify the selected app-bypass preset, individual removal and reconnect.
- Open device management and revoke a separate test installation.
- Check feedback opens support without attaching diagnostics.
- Run Wi-Fi/LTE and battery-saver checks before changing any reconnect policy.
