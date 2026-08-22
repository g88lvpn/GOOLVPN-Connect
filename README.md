# GOOLVPN Connect for Android

GOOLVPN Connect is the Android client for GOOLVPN. It is based on sing-box for
Android (SFA) and keeps the proven Android VPN/TUN layer while providing a
simplified GOOLVPN interface, account activation, managed profiles, automatic
route selection, per-app VPN, diagnostics and signed self-hosted updates.

Source release: `0.8.0` (`versionCode=21`).

- Package: `site.goolv.connect`
- Android: minSdk 23, targetSdk 35
- Stable download: https://sub.goolv.site/app/download
- Release notes: `RELEASE_NOTES_0.8.0.md`
- Build instructions: `BUILDING.md`
- Release signing: `RELEASE_SIGNING.md`
- Next patches: `PATCH_ROADMAP.md`

This repository contains a modified GPL build. Public APK releases must be
accompanied by the corresponding source and license information.

## Build from source

Use Windows PowerShell with the Android SDK, JDK and Go toolchain available.
The project build script safely maps a temporary ASCII-only drive path because
Android AIDL cannot reliably build from a Windows path containing non-ASCII
characters.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build.ps1 -Release -Test
```

The release key is deliberately not included. Follow `RELEASE_SIGNING.md` to
create or provide your own signing setup; never commit a keystore or
`signing.properties`.

## License

```
Copyright (C) 2022 by nekohasekai <contact-sagernet@sekai.icu>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.

In addition, no derivative work may use the upstream name or imply association
with this application without prior consent.
```

GOOLVPN Connect does not use the upstream SFA name as its product name and does
not imply official affiliation with the upstream project.
