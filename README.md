# NotePay

NotePay is a local-first Android expense manager written in Kotlin and Jetpack Compose. It records income and expenses, forecasts spending, supports bill splitting with a VietQR-compatible payment payload, and can optionally parse selected bank notifications on-device.

## Privacy model

Financial data is stored in the app's local Room database. OCR, spending forecasts, and the optional LiteRT-LM advisor run on the device. Notification access is opt-in and limited to the packages explicitly enabled in the app. NotePay does not upload notification text, transaction data, screenshots, or imported AI models.

## Build

Requirements: Android Studio, JDK 17, and an Android SDK with a device or emulator.

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Install the resulting debug build from Android Studio or with `:app:installDebug` while a device is connected.

## Notification tracking

Open **Settings → Read notifications**, turn on **Automatic transaction capture**, then grant Android's notification-listener permission. Keep the banking app's notifications enabled and disable battery restrictions for NotePay if your device aggressively stops background services.

Only TPBank is currently enabled for automatic recording. Other apps shown in the picker are discovery candidates, not supported parsers. See [BANK_NOTIFICATION_GUIDE.md](doc/BANK_NOTIFICATION_GUIDE.md) for the exact flow, verification, and instructions for contributors.

## QR and bill split

The QR payload is generated locally in `ui/util/VietQrGenerator.kt`; it contains the receiving bank BIN, account number, amount, transfer memo, and CRC. Treat it as a payment instruction: verify recipient, amount, and memo in the banking app before confirming. NotePay is not affiliated with NAPAS, VietQR, or any bank.

## Contributing

Read [AGENTS.md](AGENTS.md) for module layout, style, and test commands. Do not commit financial records, notification samples, OCR images, local models, APKs, keystores, or `local.properties`. Add sanitized parser fixtures and tests for every new bank format.

## Before publishing

This repository currently has no root software license; choose one deliberately before accepting contributions. Also review [THIRD_PARTY_ASSETS.md](THIRD_PARTY_ASSETS.md) before publishing: bundled third-party logos need documented permission or replacement.
