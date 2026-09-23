# Notification tracking guide

## User setup

1. In NotePay, open **Settings → Read notifications**.
2. Turn on **Automatic transaction capture**. Android opens the notification-access settings.
3. Enable NotePay, return to the app, and ensure the access status is **Granted**.
4. Enable TPBank in the supported-app section and link the correct wallet to TPBank.
5. Keep TPBank notifications enabled. On MIUI/HyperOS and similar systems, set NotePay battery use to **Unrestricted** so the listener is not stopped.

Turning off automatic capture immediately prevents NotePay from processing later notifications. Revoking Android notification access prevents delivery to the listener altogether.

## Current support contract

TPBank is the only package permitted by `KnownBankApps.supportedPrimaryPackages`; it is the only bank with an automatic-recording contract today. Other bank and e-wallet names may appear for discovery or future configuration, but their notifications are blocked before parsing. Do not describe them as supported until real-device fixtures and tests are merged.

The listener reads notification title, text, big text, and text lines only after both Android access and the in-app switch are enabled. It selects the longest available text, parses it locally, links it to the matching wallet, then writes a transaction. It ignores NotePay's own notifications.

## Adding a bank safely

1. Obtain several sanitized notification samples with different transaction types and formats. Never commit account numbers, names, references, or real amounts.
2. Add a bank-specific parser or fixture in `domain/notification/NotificationParser.kt` and unit tests under `app/src/test/`.
3. Add the package alias to `KnownBankApps`, but do **not** add it to `supportedPrimaryPackages` yet.
4. Verify false positives, duplicate delivery, internal transfers, income, expense, and a notification with no usable amount.
5. Enable the package in `supportedPrimaryPackages` only after device testing and code review.

Run:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

## Debugging

Use the debug-only TPBank simulation in the settings screen, or inspect Logcat with `NotePayNotif`. Logs must never contain raw notification text or account data in a published build. Android's `NotificationListenerService` documentation explains the platform permission and lifecycle: <https://developer.android.com/reference/android/service/notification/NotificationListenerService>.
