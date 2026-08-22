# GOOLVPN Connect 0.8.0

Released source version: 0.8.0 (`versionCode=21`)

## Changes

- Added optional, curated per-app bypass presets for food and delivery, banks,
  Yandex services, Russian services and shopping. Presets use exact Android
  package names only; each selected app can be removed before reconnecting.
- Redesigned the preset UI: it is collapsed by default and leaves room for the
  installed-app list.
- Added device management: view registered installations, plan slots and
  revoke another installation after confirmation. The current device remains
  removable only through explicit deactivation.
- Added the separate `Ideas and improvements` item, which opens the public
  feedback page and sends only text voluntarily entered by the user. Support
  diagnostics remain a separate explicit action.
- Kept future domain/IP smart bypass out of this release. It requires its own
  small, versioned and tested rules before it can affect routing.

## Verification

- Signed arm64 APK SHA-256:
  `e420c2312f1e99b27c4a68ac65454f548ca69f7fb04832f8bb7aee08a3656d3e`
- Package: `site.goolv.connect`
- 35 unit tests passed.
- Physical Android 16 checks covered per-app preset changes and reconnect,
  device management, Wi-Fi, LTE, battery saver and the feedback flow.

## Source and license

This tag contains the complete corresponding source for the 0.8.0 APK,
including the GPLv3-or-later `LICENSE` and the included sing-box source tree.
GOOLVPN Connect is a modified derivative of sing-box for Android; upstream
notices are retained.
