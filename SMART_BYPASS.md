# Smart service bypass

This document defines the release gate for the future smart bypass with a
curated default service list.
The initial reviewed catalog is delivered by the backend but remains disabled
until the Android user explicitly enables Smart mode or an individual group.
The app presents Smart mode as recommended, while retaining an explicit off
switch and per-group control.

## Scope

Smart bypass routes traffic to a reviewed service destination through the
`direct` outbound while the rest of the device traffic remains in GOOLVPN. It
is distinct from Android per-app VPN exclusions. The reviewed baseline is
enabled for a new profile, but every group remains visible and reversible.

Each catalog entry needs:

- a stable service name and user-visible group;
- exact domain suffixes, and IP prefixes only where a domain rule cannot work;
- source and verification date;
- an owner and expiry/review date;
- a Wi-Fi and mobile-network test result.

Never create a country-wide rule such as `*.ru`, use an application's package
name as a network rule, or claim that this bypasses provider white-list mode.

## Delivery design

1. Keep the catalog versioned in the backend app-profile builder.
2. Return a versioned curated baseline with the app profile. Enable only its
   reviewed groups for a new profile; never silently add direct traffic to an
   existing user's saved choice.
3. Let the Android client show every enabled group and a direct-traffic
   warning, disable an individual group, or switch back to "All through VPN".
4. Add sing-box route rules only for enabled, reviewed entries before the VPN
   selector final rule.
5. Use a test account and confirm both direct service access and unrelated VPN
   traffic on Wi-Fi and LTE before publishing.

## External reference assessed

`hydraponique/roscomvpn-routing` was reviewed on 2026-08-22. Its useful ideas
are an explicit rule order (`block`, then proxy exceptions, then direct rules),
separate domestic/remote DNS handling, and versioned route-data releases.

Its ready-made DEFAULT/WHITELIST profiles are not suitable for GOOLVPN:

- they route broad Russian/Belarusian domain and IP categories directly;
- they depend on third-party geo datasets and CDN URLs;
- their 3x-ui integration requires a custom panel fork.

GOOLVPN must not import those profiles or update x-ui for this feature. Any
future use of their data requires separate license/provenance review, an exact
version plus SHA-256 pinning, failure handling, and a small curated catalog.

The reusable pattern is the data workflow, not their broad lists: keep plain
reviewed source entries in small service groups, record source and review date,
compile or validate them deterministically, then route groups in a fixed order.
The reviewed reference's `whitelist` includes government and bank domains while
`category-ru` and `geoip:direct` intentionally cover a far broader set of
Russian/Belarusian destinations. GOOLVPN may use the former only as a research
lead for individual service verification; it must not use the latter as a
direct-routing category.

## Combined GOOLVPN pattern

The Hiddify Next routing model was compared on 2026-08-22. It keeps app bypass
separate from destination rules, gives each route an enabled state and explicit
order, and distinguishes direct and remote DNS. RoscomVPN contributes a
reviewable category-data pipeline.

GOOLVPN will combine these ideas as follows:

1. Android per-app presets remain an independent, local fallback.
2. The backend owns a small versioned catalog of named service groups.
3. A group contains only reviewed domain suffixes and exceptional CIDR entries,
   plus source, review date and DNS intent.
4. The app starts new profiles with the curated baseline, lets the user see its
   direct-traffic effect and remove a group or restore "All through VPN"; it
   does not offer raw rule editing or route-rule imports.
5. The generated sing-box profile preserves a fixed rule order: protection
   blocks, DNS handling, explicitly enabled direct groups, then the GOOLVPN
   selector as final.

This combines Hiddify's auditable rule semantics with RoscomVPN's data hygiene
without inheriting an unbounded rule editor, remote rule imports, broad RU/BY
direct categories, or external list auto-updates.

## Release gate

- No unreviewed domains or broad geographic rules.
- The default direct groups, their purpose and affected services are visible;
  a user can turn off a group or the whole feature.
- DNS, IPv4/IPv6, CDN and ECH behaviour are checked for every initial service.
- A failed rule is removed or disabled server-side; it is never silently
  expanded to a whole TLD.
- Initial catalog groups are Yandex, banking, marketplaces, government
  services, and VK/Mail.ru. They contain suffixes only; no CIDR is used.

## Product destination after 0.8.0

The intended user experience is one visible main-screen "Smart mode": reviewed
services that reject VPN use `direct`, while every other destination continues
through GOOLVPN. The aim is access to both compatible local services and
resources unavailable without VPN, without requiring the user to curate rules.

This is a staged destination, not a claim about the first catalog. It grows
only through small service groups with reproducible Wi-Fi/LTE tests and a clear
off switch. Country-wide RU/BY address space, `*.ru`, `category-ru` and broad
GeoIP rules remain out of scope. Provider white-list mode still needs a
separately reachable entry point or relay.
