# GOOLVPN Connect patch roadmap

Updated: 2026-08-22.

## Current baseline

- Public release: `0.8.0` (`versionCode=21`).
- Stable download: https://sub.goolv.site/app/download
- Package/signing identity is permanent and must not change.
- `0.7.2` was published manually on 2026-08-22.
- `0.8.0` was published on 2026-08-24 after the physical smoke checks. Its
  public source, GPL-3.0 license and tag `v0.8.0` are available at
  https://github.com/g88lvpn/GOOLVPN-Connect/tree/v0.8.0; the release notes
  shown by the updater link there. No service restart was required.

## 0.8.1 — reliable disconnect (in preparation)

1. The main GOOLVPN screen now sends the same direct stop broadcast as the
   foreground-notification action. It no longer depends on a possibly stale
   DashboardViewModel status on slower devices.
2. A tap immediately shows the existing "Disconnecting" state and blocks
   repeated taps. The UI returns to a retryable connected state after seven
   seconds if `Stopped` was not observed, with a clear retry message.
3. This patch is intentionally limited to the observed disconnect issue. No
   routing, profiles, backend API or release channel behavior changes.
4. `0.8.1` (`versionCode=22`) was built and published through the updater on
   2026-08-26. The public manifest, version, file size and SHA-256 were
   verified. No server restart was required; physical phone verification is
   still required. The owner publishes the matching source snapshot separately.

## 0.7.2 — reliability and support visibility (published)

Keep this patch narrow and compatible with the current backend.

1. Fresh Telegram activation smoke-test passed on a physical phone.
2. Diagnostics now report the configured strategy (`Авто`, `Быстро`,
   `Стабильно`) without guessing or exposing the active node, endpoint,
   UUID, credentials, device token or full config.
3. The result uses prominent colored `Интернет`, `Сервер` and `Доступ` cards,
   compact technical details and one primary send-to-support action.
4. The speed-notification switch already refreshes the active service without
   reconnecting VPN; vendor-specific visual verification remains an `0.8.0`
   evidence task.
5. `33` unit tests and the signed arm64 build passed. The release was published
   manually through the updater channel; no service restart was required.

## 0.8.0 — Android feature completion

1. Optional Android app-bypass presets are implemented locally: a reviewed
   exact-package catalog is split into food/delivery, banking, Yandex, Russian
   services and shopping. The collapsed-by-default presets block keeps the app
   list usable; its expanded groups show examples. A preset adds only installed
   apps; users can remove each one before reconnecting. Never infer it from
   `ru.*` alone and never force exclusions. Android 16 visual smoke-test,
   applying a group, individual removal and reconnect passed on 2026-08-23.
2. A future smart service bypass will start new profiles with a small,
   versioned and tested default domain/IP catalog routed directly. It stays
   separate from app presets; every group is visible and reversible, and an
   existing user's saved routing choice is never changed silently. It must be
   tested on Wi-Fi/LTE before any public release.
3. App device management is implemented locally: show registered installations,
   plan limit and confirmed revoke of another device. Current-device removal
   remains the explicit app deactivation flow. Backend limits: trial `1`,
   Standard `4`, Premium `6`, legacy Premium/Lifetime `7`. The
   physical-device list and separate-test-device revoke smoke-test passed on
   2026-08-23.
4. "Ideas and improvements" opens the separate public `/feedback` page, not
   Support or diagnostics. It submits only the voluntary idea text; the page is
   reachable publicly; a harmless Android end-to-end submission delivered its
   administrator notification on 2026-08-23.
5. Battery/reconnect evidence pass across Wi-Fi/LTE and battery saver passed on
   2026-08-23. Change recovery policy only for a reproduced failure.
6. Add controlled recovery for observed network transitions and repeated route
   failures. Keep auto switching hysteretic to avoid flapping.
7. GPL release readiness: `RELEASE_CHECKLIST.md` documents the exact source,
   license, updater and smoke-test release gates. Publishing the public source,
   notices and matching release notes remains a manual release prerequisite.

## After 0.8.x

- Freeze the Android feature surface and fix only observed reliability issues.
- Grow smart routing toward one visible "Smart mode" on the main screen: a
  reviewed local-service catalog goes direct while all other traffic stays in
  GOOLVPN. Expand it only from verified services and Wi-Fi/LTE evidence; do not
  replace it with broad Russian/Belarusian country, TLD or GeoIP rules.
- Start a separate iOS/App Store project. Do not assume Android per-app VPN can
  be reproduced on normal iOS; local-service bypass needs domain/IP rule sets.
- Google Play remains optional. Play Protect `uncommon app` is a distribution
  reputation warning and is not guaranteed to disappear after identity
  verification alone.

## 0.9.x — Smart mode stabilization before 1.0

- The app profile API now returns a versioned, small catalog for Yandex,
  banking, marketplaces, government services and VK/Mail.ru.
- Android validates catalog syntax, persists the user's enabled group IDs and
  injects only those suffixes as `direct` rules before the final GOOLVPN route.
  The all-through-VPN default and the existing per-app bypass remain intact.
- `0.9.0` was built for device testing. The production catalog deployment was
  verified manually with Ozon: it warns when the group is off and opens after
  the marketplace group is enabled and VPN is restarted.
- `0.9.1` (`versionCode 24`) adds the recommended main-screen switch, collapses
  per-group controls in settings, and adds Smart mode to onboarding. Its debug
  build and unit tests passed; it still needs a signed device candidate and
  Wi-Fi/LTE checks for every enabled group before release.

## Deferred infrastructure

- MTProto.
- Guaranteed operation during provider white-list mode; this requires a
  reachable entry point inside the allowed network and cannot be solved only
  by changing the Android UI or reordering current Aeza/QDE routes.
