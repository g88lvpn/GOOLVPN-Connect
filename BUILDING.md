# Local Android build

Updated: 2026-08-21. The current toolchain is AGP `9.0.1` with Gradle `9.1.0`.
Do not move the wrapper back to Gradle `9.3.1` without a compatible AGP update;
that combination caused a Kotlin DSL `NoSuchMethodError` in this workspace.

The Windows workspace contains non-ASCII characters. Android AIDL cannot read
its dependency files from that path, so `scripts/build.ps1` temporarily maps
the workspace to `V:` for the duration of each build.

Build the existing debug APK:

```powershell
.\scripts\build.ps1
```

Rebuild the pinned sing-box core and then create a clean APK:

```powershell
.\scripts\build.ps1 -RebuildLibbox -Clean
```

Debug APKs are written to:

```text
app/build/outputs/apk/other/debug/
```

The normal phone build is the `arm64-v8a` APK. The universal APK is useful
when the device architecture is unknown, but it is substantially larger.

## Signed release

GOOLVPN release builds use a permanent keystore outside the repository. Create
it once with:

```powershell
.\scripts\create_release_key.ps1
```

The script writes the keystore and credentials to
`%USERPROFILE%\GOOLVPN-signing`. Never commit either file. Back up that entire
directory in encrypted storage before distributing the first release.

Build the signed release APK:

```powershell
.\scripts\build.ps1 -Release
```

Release APKs are written to `app/build/outputs/apk/other/release/` and copied
to `dist/`. Back up the keystore and `signing.properties` together: losing the
key makes it impossible to publish updates over installed release builds.

## Public update

Build, verify, upload the arm64 APK and publish its manifest with:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\deploy_update.ps1 `
  -Version 0.7.2 -Publish
```

The script verifies the local manifest/hash, uploads the APK, checks the public
download and activates the new manifest. Publishing an APK/manifest does not
require restarting backend, bot, nginx or x-ui.

The permanent certificate fingerprint and first-release migration procedure
are recorded in `RELEASE_SIGNING.md`.
