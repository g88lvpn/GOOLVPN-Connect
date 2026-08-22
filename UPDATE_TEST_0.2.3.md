# GOOLVPN 0.2.3 updater test

Archived successful updater test. Superseded by the public `0.7.1` baseline;
retain as evidence that activation and per-app state survive signed updates.

`0.2.3` is functionally identical to `0.2.2`. It exists only to verify the
fixed updater after `0.2.2` has been installed manually over `0.2.0`.

## Preconditions

- The phone must report GOOLVPN Connect `0.2.2`.
- `0.2.2` must have been installed over `0.2.0` without uninstalling or
  clearing application data.
- The failed `0.2.2` server manifest must be disabled before publishing this
  test.

## Files

```text
dist/GOOLVPN-Connect-0.2.3-arm64-v8a.apk
dist/app_release-0.2.3-test.json
```

Expected APK SHA-256:

```text
c7d2dc6e1476f778cbafbb35c125afc36178816f7dada7abd1d0c5752fa66fa4
```

Upload and verify the APK before copying the staged manifest to
`/opt/vpn_bot/app_release.json`. No backend restart is required.

The update action must open the Android package installer. The VPN should stay
connected until Android replaces the application. After confirmation, verify
version `0.2.3`, activation, connection mode, per-app selection, and VPN
connectivity.

## Result

Passed on 2026-08-06:

- `0.2.2` updated to `0.2.3` through the in-app updater;
- Android applied the new version successfully;
- VPN connectivity remained functional;
- newly selected per-app switches survived a full application close and
  restart.

The selection that was already missing in `0.2.0` could not be recovered and
must be selected once again. New selections are persisted correctly.
