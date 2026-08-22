# GOOLVPN 0.3.0 rollout

Archived rollout document. The activation/update architecture remains useful,
but current public release and deployment instructions are in `README.md`,
`BUILDING.md`, `RELEASE_SIGNING.md` and `PATCH_ROADMAP.md`.

`0.3.0` adds the simplified home screen, dedicated GOOLVPN settings and
one-tap sign-in from the Telegram bot. It is signed with the permanent
GOOLVPN release key and can update `0.2.3` without clearing application data.

## Artifacts

```text
dist/GOOLVPN-Connect-0.3.0-arm64-v8a.apk
dist/app_release-0.3.0-test.json
dist/release-notes-0.3.0.txt
```

Expected arm64 SHA-256:

```text
c6ed97fa07ec998e080b2dacd534f8db940f2b5b558c988aaf1b09a3c03a09d4
```

## Server rollout order

1. Upload `nodegate-site/app-activate.html` to
   `/var/www/nodegate/app-activate.html`. Nginx does not need a restart.
2. Upload the arm64 APK to
   `/var/www/nodegate/downloads/GOOLVPN-Connect-0.3.0-arm64-v8a.apk`.
3. Verify the remote APK hash before publishing the manifest.
4. Upload `app_release-0.3.0-test.json` to a staging path, then copy it to
   `/opt/vpn_bot/app_release.json` only when the APK and activation page are
   available.
5. Upload `bot.py` and `kb.py`, add the new environment values, then restart
   only `vpn-bot`.

Required `.env` additions:

```dotenv
TELEGRAM_CHANNEL_URL=https://t.me/your_channel
APP_ACTIVATION_LANDING_URL=https://goolv.site/app-activate.html
```

## Phone checks

1. On `0.2.3`, note the current activation, connection mode and selected apps.
2. Open `GOOLVPN` in the bot and test the application sign-in button.
3. Publish the `0.3.0` manifest and install the offered update.
4. Confirm version `0.3.0`, activation, VPN connection, connection mode and
   selected apps are preserved.
5. Open Settings and test app routing, notifications, refresh, account,
   support and update screens.
