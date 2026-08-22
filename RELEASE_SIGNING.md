# GOOLVPN Android release signing

## Permanent identity

- First permanent release: `0.2.0` (`versionCode=7`).
- Package: `site.goolv.connect`.
- Alias: `goolvpn-release`.
- Certificate SHA-256:
  `01:F9:F1:C6:FD:93:B1:EC:6C:65:3C:25:4B:7C:05:55:76:06:38:F2:04:1A:5A:88:AB:EE:4E:66:54:46:8A:59`.
- Certificate validity: 2026-08-05 through 2126-07-12.

The private files live outside the repository:

```text
C:\Users\erikf\GOOLVPN-signing\goolvpn-release.jks
C:\Users\erikf\GOOLVPN-signing\signing.properties
```

Back up both files together in encrypted storage. Never commit, upload, send,
or regenerate them. Losing either the keystore or its credentials makes it
impossible to update installed release builds under the same package name.

## Build

```powershell
.\scripts\build.ps1 -Release
```

The build reads credentials only from the external signing directory. Debug
builds continue to use the Android debug key and must never be distributed as
an update after the `0.2.0` rollout.

## First migration from debug

The permanent key differs from the old debug key. For the first `0.2.0`
installation only:

1. Stop VPN and remove the old debug application.
2. Install the signed `0.2.0` APK.
3. Sign in again with a one-time GOOLVPN code.
4. Verify connection modes, per-app routing, and update settings.

Every later release signed by this same key can be installed over `0.2.0`
without deleting application data.

## Current public release

```text
GOOLVPN-Connect-0.7.1-arm64-v8a.apk
versionCode: 19
size: 28738096
SHA-256: e7bdb91cb86cc8f2114e4d4f151a8609e7886bcb3540d60ba83e3c3221589223
```

The controlled `0.2.2` to `0.2.3` migration passed on 2026-08-06 and remains
historical evidence that updater state survives. `0.7.1` is now the public
baseline. The next release must use at least `versionCode=20`; never reuse a
published version code, even if the APK file itself changes.

Rebuilding can change the APK file hash. Always publish the hash calculated
from the exact uploaded file. Prefer `scripts/deploy_update.ps1`; keep
`publish_android_release.py` only as the server-side fallback.
