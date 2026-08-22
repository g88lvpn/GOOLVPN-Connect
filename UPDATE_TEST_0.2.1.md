# GOOLVPN 0.2.1 updater test

Archived updater incident document. Superseded by the public `0.7.1` baseline;
retain for regression history only and do not publish these old manifests.

`0.2.1` is intentionally identical to `0.2.0` except for its version. The test
must prove that activation, connection mode, excluded apps, and notification
preferences survive an update signed by the permanent release key.

## Files

```text
dist/GOOLVPN-Connect-0.2.1-arm64-v8a.apk
dist/app_release-0.2.1-test.json
```

Expected APK SHA-256:

```text
87ca0da56671d48832172e7846b896c87ec1176b89f2843e09287490dac443f6
```

## Controlled publication

1. Upload the exact arm64 APK to the nginx directory served as
   `https://goolv.site/downloads/GOOLVPN-Connect-0.2.1-arm64-v8a.apk`.
2. Confirm that the public URL returns HTTP 200 and the same SHA-256.
3. Upload `app_release-0.2.1-test.json` to Aeza as
   `/opt/vpn_bot/app_release.json`.
4. Do not restart `nodegate-sub`: the manifest is read for every request.
5. Confirm `https://sub.goolv.site/app/releases/latest` returns version code 8,
   version name `0.2.1`, and the expected SHA-256.

## Phone test

Before updating, note the activation state, selected connection mode, and the
per-app list. Install the update through the GOOLVPN prompt, then verify:

- Android updates the existing app instead of asking to replace it;
- the app reports `0.2.1`;
- activation is still present;
- connection mode and per-app selection are unchanged;
- VPN connects and traffic passes;
- the cached update and update-ready notification disappear.

If the update fails, keep `0.2.0` installed and remove or disable the server
manifest before investigating. Never regenerate the release key.
