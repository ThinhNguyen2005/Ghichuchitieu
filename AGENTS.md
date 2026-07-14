# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android app (`app`) written in Kotlin with Jetpack Compose and Material 3. Production code lives in `app/src/main/java/com/notepay/`:

- `domain/`: models, repository contracts, analytics, and use cases.
- `data/`: Room database, DAOs, repository implementations, and local services.
- `ui/`: Compose screens, reusable components, navigation, theme, and UI utilities.
- `ai/`: local forecasting, parsing, and on-device assistant integrations.

Resources are in `app/src/main/res/`; Room schema exports are versioned in `app/schemas/`. Unit tests belong in `app/src/test/`; device tests belong in `app/src/androidTest/`. Keep feature specs and plans under `specs/`.

## Build, Test, and Development Commands

Run commands from the repository root on Windows:

- `./gradlew.bat :app:assembleDebug` — compile the debug APK.
- `./gradlew.bat :app:testDebugUnitTest` — run local JUnit tests.
- `./gradlew.bat :app:connectedDebugAndroidTest` — run instrumentation tests on an emulator/device.
- `./gradlew.bat :app:bundleRelease` — produce a release bundle; configure `app/signing.properties` locally when signing is needed.

Use `--no-daemon` when diagnosing clean CI-like builds. Do not commit APKs, keystores, `local.properties`, or generated `build/` output.

## Coding Style & Naming Conventions

Use Kotlin with four-space indentation and idiomatic, immutable-first code. Prefer `StateFlow` in ViewModels and stateless Compose components that receive state plus callbacks. Name screens `*Screen`, ViewModels `*ViewModel`, UI states `*UiState`, and use cases as verbs (for example, `SuggestCategoryUseCase`). Keep composables small; put reusable visuals in `ui/component/` and business rules outside the UI layer.

No formatter or linter is configured; match nearby code and let the Kotlin compiler enforce correctness. Add dependencies through `gradle/libs.versions.toml`, not inline versions.

## Testing Guidelines

Use JUnit for unit tests and AndroidX test tooling for instrumentation tests. Name tests `ThingTest` and methods for behavior, e.g. `suggest_returnsFood_forRestaurantNote`. Add focused tests for money calculations, date ranges, Room migrations, and parsing/forecast edge cases. Run the relevant test target before opening a PR.

## Commit & Pull Request Guidelines

Follow the existing Conventional Commit style: `feat:`, `fix:`, `refactor:`, or `test:` with a concise imperative summary. Keep commits scoped. PRs should explain user-visible changes, list verification commands, link related specs/issues, and include screenshots or recordings for Compose UI changes.

## Security & Local Data

Financial data is local-first. Never log transaction contents, secrets, OCR images, or notification text. Keep credentials only in ignored local configuration files and preserve Room schema exports when migrations change.
