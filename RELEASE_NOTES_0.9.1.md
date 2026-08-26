# GOOLVPN Connect 0.9.1

## Smart mode

- Added the recommended Smart mode switch to the main screen.
- Made group controls in Settings collapsible; individual groups remain visible
  and reversible when the section is expanded.
- Added a Smart mode step to onboarding: reviewed local services use direct
  internet, while all other traffic continues through GOOLVPN.

## Verification

- Signed arm64 APK built with `versionCode 24`.
- Kotlin unit tests passed.
- The production catalog was manually checked with Ozon: with its group off,
  Firefox through VPN receives its VPN warning; after enabling the group and
  restarting VPN, Ozon opens through the direct route.
