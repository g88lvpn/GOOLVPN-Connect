# GOOLVPN 0.2.2 updater test

Archived updater recovery document. Superseded by the public `0.7.1` baseline;
retain for regression history only and do not publish these old manifests.

`0.2.2` fixes the failed `0.2.1` updater test. The production flavor now
contains `REQUEST_INSTALL_PACKAGES`, opens the Android package installer from
the foreground activity, and does not stop the VPN before the user confirms
installation.

It also reloads selected applications by package name and mirrors the GOOLVPN
selection in a separate preferences file. Existing Room settings remain the
primary migration source on the first `0.2.2` launch.

## Files

```text
dist/GOOLVPN-Connect-0.2.2-arm64-v8a.apk
dist/app_release-0.2.2-test.json
```

Expected APK SHA-256:

```text
e67799bab9cf16b7414229346c04c3d3b5b4ebbe1cb4c5b654e7eefabc878320
```

## Controlled publication

1. Keep the failed `0.2.1` manifest disabled.
2. Upload the arm64 APK as
   `/var/www/nodegate/downloads/GOOLVPN-Connect-0.2.2-arm64-v8a.apk`.
3. Upload `app_release-0.2.2-test.json` as
   `/opt/vpn_bot/update-staging/app_release-0.2.2-test.json`.
4. Verify the public APK URL and SHA-256 before activating the manifest.
5. Copy the staged manifest to `/opt/vpn_bot/app_release.json`.

No service restart is needed because the backend reads the manifest for every
request.

## Phone test

1. Keep `0.2.0` installed and do not clear its application data.
2. Note activation and connection mode. Do not change the currently disabled
   app switches before the first `0.2.2` launch; this lets the migration prove
   whether the stored package list is still present.
3. Accept the update prompt. Android must open its own installation screen.
4. If Android asks once for permission to install from GOOLVPN Connect, allow
   it and repeat the update action if the installer does not resume itself.
5. Confirm that the application reports `0.2.2`, remains activated, connects,
   and shows the saved per-app selection.
6. Toggle one application, fully close the app, reopen it, and confirm that the
   switch remains selected.

If any step fails, disable the server manifest again. Do not uninstall the app,
clear its data, or regenerate the release key.
