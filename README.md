# NotePay

[![Android CI](https://github.com/ThinhNguyen2005/Ghichuchitieu/actions/workflows/android.yml/badge.svg)](https://github.com/ThinhNguyen2005/Ghichuchitieu/actions/workflows/android.yml)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-7F52FF.svg)](https://kotlinlang.org)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84.svg)](https://developer.android.com/about/versions/oreo)

NotePay is a local-first personal finance app for Android, written in Kotlin with Jetpack Compose. It records income and expenses, tracks wallets, debts and subscriptions, forecasts spending, splits bills with a VietQR-compatible payload, and can optionally import transactions from bank notifications or OCR on the device.

Everything the app knows about your money stays in a local Room database. Google Sign-In is optional and used only for identity: there is no sync server, no analytics upload, and no transaction data sent to any backend.

## Table of contents

- [Features](#features)
- [Privacy and security](#privacy-and-security)
- [Tech stack](#tech-stack)
- [Architecture](#architecture)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
- [Build variants](#build-variants)
- [Optional configuration](#optional-configuration)
- [Testing](#testing)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)
- [Trademarks and third-party assets](#trademarks-and-third-party-assets)

## Features

**Money management**

- Income and expense entries with categories, notes, dates and multi-wallet support.
- Category suggestions learned from your own history, entirely on the device.
- Statistics dashboard: summary, charts and written insights over custom or preset periods.
- Budget tracking and spending forecasts computed locally.

**Debts, bills and subscriptions**

- Two-way debt tracking (`LEND` / `BORROW`) with partial repayment history, reminders and optional wallet synchronisation. See [DEBT_MANAGEMENT.md](docs/DEBT_MANAGEMENT.md).
- Bill splitting with a generated EMV/VietQR payload, shareable as text or QR.
- Recurring subscription tracking.

**Capture and AI**

- Offline OCR from bank screenshots (bundled ML Kit Latin text recognition) to pre-fill amounts and memos.
- Optional automatic transaction capture from supported bank notifications (sideload `full` flavor only; TPBank is the currently supported parser). See [BANK_NOTIFICATION_GUIDE.md](docs/BANK_NOTIFICATION_GUIDE.md).
- On-device budget advice through Gemini Nano (ML Kit Prompt API). An optional cloud Gemini advisor exists and requires your own API key plus explicit opt-in in settings.

**Data ownership**

- JSON backup and restore, documented in [BACKUP_RESTORE.md](docs/BACKUP_RESTORE.md).
- Vietnamese and English UI, light and dark themes, and adaptive layouts for phones, tablets and foldables.

## Privacy and security

- Financial records live in the app's local Room database and DataStore preferences; nothing is uploaded by default.
- OCR, forecasting and the default AI advisor run on the device.
- Notification access is opt-in, and only packages you explicitly enable are parsed. Notification text is never logged.
- Cloud AI is disabled unless you enter an API key and enable it; when enabled, only the prompt needed for that request leaves the device.
- No transaction contents, notification samples, OCR images or tokens are written to logs, issues or pull requests.

## Tech stack

| Layer | Choice |
| --- | --- |
| Language | Kotlin 2.2, Java 17 toolchain |
| UI | Jetpack Compose, Material 3, Material 3 Adaptive (window size classes) |
| Architecture | MVVM: Compose screens, `StateFlow` view models, Kotlin-only domain layer |
| Persistence | Room (schema v6, exported to `app/schemas/`), DataStore Preferences |
| DI | Hilt (annotation processing via KSP) |
| Async | Coroutines and Flow, WorkManager for background work |
| AI / ML | ML Kit GenAI Prompt API (Gemini Nano), ML Kit text recognition, optional Gemini cloud client |
| Auth (optional) | Firebase Authentication with Credential Manager Google Sign-In |
| QR | ZXing core, local EMV payload generation |
| Build | AGP 9.3, Kotlin 2.2, version catalog in `gradle/libs.versions.toml` |
| SDK | `minSdk` 26, `compileSdk` 37 |

## Architecture

```text
Compose screens / ViewModels
        ↓
presentation state, events, effects, formatters, UI mappers
        ↓
domain models, policies, analytics, repository interfaces
        ↑
data/local/platform implementations and adapters
```

The `domain` package is pure Kotlin: no Android, Compose or resource imports. Room, DataStore, files and platform APIs stay in `data` and `platform`, and localized text plus visual mapping are resolved at the `ui` boundary. Design tokens for the "Sand & Ink" visual language live in `ui/theme`.

Full details, boundaries and source-of-truth policies: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Project structure

```text
com.notepay
├── domain/        models, analytics, use cases, repository contracts
├── data/          Room, DAOs, mappers, repositories, preferences, backup, remote
├── ai/            on-device and optional cloud advisors, OCR scanner
├── platform/      Android compatibility helpers
├── ui/            feature screens, components, theme, navigation, formatters
├── worker/        WorkManager workers
└── util/          shared helpers
```

Flavor-specific code lives in `app/src/full/` (notification listener, capture settings) and `app/src/play/` (watchdog-only build). Unit tests are under `app/src/test/`, flavor tests under `app/src/testFull/`, and instrumentation tests under `app/src/androidTest/`.

## Getting started

### Requirements

- Android Studio with an Android SDK matching `compileSdk` 37.
- JDK 17 (CI runs on JDK 21).
- A device or emulator running Android 8.0 (API 26) or newer.

### Clone and build

```powershell
git clone https://github.com/ThinhNguyen2005/Ghichuchitieu.git
cd Ghichuchitieu
.\gradlew.bat :app:assembleFullDebug
```

Install on a connected device with `.\gradlew.bat :app:installFullDebug`, or run the `app` configuration from Android Studio.

On low-memory machines add `--no-daemon` to keep a single JVM alive during the build.

## Build variants

Two flavors are published from the same source tree:

| Flavor | Application ID | Purpose |
| --- | --- | --- |
| `full` | `com.notepay.full` | Sideload / GitHub releases; includes bank notification capture |
| `play` | `com.notepay` | Google Play build; no notification listener |

| Build type | Suffix | Notes |
| --- | --- | --- |
| `debug` | `.debug` | No minification |
| `release` | — | R8 + resource shrinking, signed with `app/signing.properties` when present |

Release automation runs on `v*` tags (`.github/workflows/android-release.yml`), derives `versionCode`/`versionName` from the tag, builds both flavors, verifies signatures and publishes a GitHub release with `SHA256SUMS.txt`.

## Optional configuration

All values below are optional; the project builds and runs offline without them.

- **Version metadata** — `NOTEPAY_VERSION_NAME` and `NOTEPAY_VERSION_CODE` environment variables (default `1.0` / `1`).
- **Google Sign-In** — add `app/google-services.json` to enable Firebase; the `google-services` plugin is applied only when that file exists. Set the web client ID through `notepay.webClientId` in `gradle.properties` or the `NOTEPAY_WEB_CLIENT_ID` environment variable.
- **Release signing** — create `app/signing.properties` with `storeFile`, `storePassword`, `keyAlias` and `keyPassword`. Without it, release builds fall back to the debug key.
- **Cloud AI** — enter a Gemini API key inside the app and enable cloud advice; it is off by default and only used when both the key and the toggle are present.

Never commit `google-services.json`, `signing.properties`, keystores, `local.properties`, APKs or financial data.

## Testing

```powershell
.\gradlew.bat :app:testFullDebugUnitTest
.\gradlew.bat :app:testPlayDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unit tests run on the JVM with Robolectric, MockK, Turbine and Truth. Cover money arithmetic, date ranges, Room migrations, notification parsing and forecast edge cases when changing those areas. Device-only behaviour (hardware rendering, GPU/NPU inference, QR scanning, live notification service) still needs manual verification on a device.

## Documentation

| Document | Contents |
| --- | --- |
| [docs/SPEC.md](docs/SPEC.md) | Product specification and feature scope |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Layers, package boundaries, state and mapping rules |
| [DESIGN.md](DESIGN.md) | "Sand & Ink" design system (v4.0.0): colour, type, shape, components |
| [docs/DEBT_MANAGEMENT.md](docs/DEBT_MANAGEMENT.md) | Debt schemas, use cases and repayment flows |
| [docs/BANK_NOTIFICATION_GUIDE.md](docs/BANK_NOTIFICATION_GUIDE.md) | Notification capture flow and adding a new bank parser |
| [docs/BACKUP_RESTORE.md](docs/BACKUP_RESTORE.md) | Backup file format and restore behaviour |
| [docs/VIETQR_FULLSCREEN_FLOW.md](docs/VIETQR_FULLSCREEN_FLOW.md) | QR presentation and bill-split flow |
| [docs/TABLET_ADAPTIVE_PLAN.md](docs/TABLET_ADAPTIVE_PLAN.md) | Adaptive layout strategy for tablets and foldables |
| [docs/THIRD_PARTY_ASSETS.md](docs/THIRD_PARTY_ASSETS.md) | Bundled asset provenance and trademark notes |
| [AGENTS.md](AGENTS.md) | Contributor and agent guidelines: layout, style, commands |

## Contributing

Issues and pull requests are welcome.

1. Open an issue describing the bug or the proposed change before large work.
2. Follow [AGENTS.md](AGENTS.md) for module layout, style and commands. Read [DESIGN.md](DESIGN.md) before touching any UI, and use theme tokens instead of hard-coded colours or sizes.
3. Keep user-visible text in `app/src/main/res/values/strings.xml` (Vietnamese) and `values-en/strings.xml` (English).
4. Add tests for money math, parsing, migrations and state transitions; add sanitized fixtures for every new bank format.
5. Use Conventional Commit messages: `feat:`, `fix:`, `refactor:`, `test:`.
6. Describe user-visible changes, list the verification commands you ran, and attach screenshots for UI changes.

Do not include financial records, notification samples, OCR images, imported AI models, APKs, keystores or credentials in issues or pull requests.

## License

NotePay is released under the **GNU General Public License v3.0**. See [LICENSE](LICENSE) for the full text.

```
Copyright (C) 2026 ThinhNguyen2005

This program is free software: you can redistribute it and/or modify it under
the terms of the GNU General Public License as published by the Free Software
Foundation, version 3 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
PARTICULAR PURPOSE. See the GNU General Public License for more details.
```

Because the app is distributed under the GPL, any redistributed build — including modified flavors published on a store or as an APK — must ship the corresponding source code and preserve these terms. Third-party libraries remain under their own licenses; see their respective repository or POM metadata.

## Trademarks and third-party assets

NotePay is not affiliated with, endorsed by, or certified by NAPAS, VietQR, or any bank. The generated QR code is a payment request: always verify the recipient, amount and memo inside your banking app before confirming.

Bundled bank logos and VietQR metadata in `app/src/main/assets/vietqr/` are third-party trademarks with unclear redistribution rights. Do not assume that finding a logo online grants such rights; read [docs/THIRD_PARTY_ASSETS.md](docs/THIRD_PARTY_ASSETS.md) before reusing or redistributing them. Their licenses are separate from this project's GPL-3.0 license.
