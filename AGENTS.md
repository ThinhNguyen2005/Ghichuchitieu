# Repository Guidelines

NotePay: single-module Android app (`app`), Kotlin + Jetpack Compose + Material 3, local-first.

## Working Principle: Lazy Senior Dev

Lazy means efficient, not careless. The best code is the code never written.

First trace the task and every caller/test it touches using CodeGraph (`codegraph_explore` / `codegraph_impact` when `.codegraph/` exists; otherwise targeted search/read), and verify that every file, type, token and dependency exists before editing; never assume. Then stop at the first rung that holds:

1. Does this need to exist at all? (YAGNI)
2. Is it already in this codebase? Reuse the helper, theme token, component or pattern.
3. Does the Kotlin/Android stdlib do it?
4. Does a Compose / Material 3 component do it natively?
5. Does a dependency already in `gradle/libs.versions.toml` do it?
6. Can it be one line?
7. Only then: write the minimum code that works.

Rules:
- Deletion over addition, boring over clever, fewest files possible. No abstraction, wrapper or new file for code used once with no domain meaning.
- Bug fix = root cause. Trace every caller of the function you touch and fix the shared function once, not each call site.
- When replacing code, remove what it orphans: unused helpers and parameters, dead references, redundant defensive checks, duplicated tokens.
- Mark a deliberate shortcut with a known ceiling using a `shortcut:` comment naming the ceiling and the upgrade path.
- The shortest diff wins only once the problem is understood. The smallest change in the wrong place is a second bug.
- When the task is a migration or refactor (e.g. moving a screen to DESIGN v4), the task defines the scope, not the ladder. Do all of it, but still reuse existing tokens and components.

Never lazy about: understanding the problem, input validation at trust boundaries, error handling that prevents data loss, security, accessibility, localization, and anything explicitly requested. The existing `domain/` / `data/` / `ui/` layering is requested architecture, not bloat.

## Project Structure

Production code is in `app/src/main/java/com/notepay/`:
- `domain/`: models, repository contracts, analytics, use cases.
- `data/`: Room database, DAOs, repository implementations, local services.
- `ui/`: Compose screens, `component/` (reusable visuals), navigation, theme, UI utilities.
- `ai/`: local forecasting, parsing, on-device assistant integrations.

Resources: `app/src/main/res/`. Room schema exports: `app/schemas/` (keep them updated when migrations change). Tests: `app/src/test/` (unit), `app/src/androidTest/` (device). Specs and design notes: `docs/`; `docs/archive/` is historical only.

## Commands (Windows, repo root)

- `./gradlew.bat detekt :app:lintFullDebug :app:lintPlayDebug` static analysis (Detekt + Android Lint)
- `./gradlew.bat :app:assembleFullDebug :app:assemblePlayDebug` compile debug APKs (`full` & `play`)
- `./gradlew.bat :app:testFullDebugUnitTest :app:testPlayDebugUnitTest` flavor unit tests
- `./gradlew.bat :app:connectedDebugAndroidTest` instrumentation tests
- `./gradlew.bat :app:assembleFullRelease :app:assemblePlayRelease :app:bundlePlayRelease` signed release APKs & Play bundle (needs local `app/signing.properties`)

Add `--no-daemon --max-workers=1 "-Dkotlin.compiler.execution.strategy=in-process" "-Dorg.gradle.jvmargs=-Xmx1536m -XX:MaxMetaspaceSize=512m -XX:+UseG1GC -Dfile.encoding=UTF-8"` on Windows to avoid `KotlinCompileDaemon` locking `classes.jar`. Never commit APKs, keystores, `local.properties`, or `build/` output.
- **Real-device testing:** `fullDebug` installs as `com.notepay.full.debug` (test APK `com.notepay.full.debug.test`). Keep screen awake during ADB/instrumentation runs (`svc power stayon usb`), save `screencap` files outside the repo, and restore `font_scale`, `animator_duration_scale`, night mode, and `stayon false` afterward. On Xiaomi/HyperOS devices, `createComposeRule()` requires *"Display pop-up windows while running in the background"* (`MIUIOP(10021)`) enabled for `com.notepay.full.debug`; never bypass OS permissions via `appops`.

## Architecture: Right Layer

- Window insets, edge-to-edge and lifecycle hooks live in `MainActivity` / the entrypoint, never in theme or components.
- Theme provides tokens only (colors, type, shapes, elevation): no side effects, no state, no `context as Activity`.
- State transformation, repository calls and persistence live in ViewModels / use cases, never in composables.
- `NotePayNavHost` owns the root `Scaffold` (insets, snackbar host, floating-nav offset). Main tab screens (`HomeScreen`, `StatsScreen`, `AssetsScreen`, `UtilitiesScreen`) must not nest a `Scaffold`; use `Box`/`Column` with insets and padding.
- Do not modify shared navigation UI (floating bottom bar or navigation rail) unless navigation work is explicitly requested. Screen-local filters and segmented controls are not shared navigation UI.
- Single source of truth: do not mirror `MaterialTheme.colorScheme` values into `AppTheme.colors`, and do not add aliases of existing tokens (e.g. a second pill next to `capsule`).

## Kotlin & Compose Standards

Before writing Kotlin, Compose, ViewModel, Room or Coroutine code, read `.agents/skills/android-pro/SKILL.md` once per session. Non-negotiables:
- **Compose perf:** no allocation in Draw/Canvas scope; defer fast-changing state reads (anim/scroll) to the draw phase with `Modifier.graphicsLayer { }` or `drawBehind`; keep types `@Immutable`/`@Stable`.
- **Coroutines/Flow:** I/O on `Dispatchers.IO`; never swallow `CancellationException`; use `collectAsStateWithLifecycle()` in Compose.
- **State & null-safety:** no `!!`; `private val _uiState = MutableStateFlow(...)` exposed as `asStateFlow()`; ViewModels prefer `StateFlow`; composables are stateless (state + callbacks).
- **Naming:** `*Screen`, `*ViewModel`, `*UiState`, use cases as verbs (`SuggestCategoryUseCase`). Four-space indent, immutable-first. No formatter/linter: match nearby code.
- **Dependencies:** only via `gradle/libs.versions.toml`, never inline versions.
- **Compose Lint & Detekt:** resolve strings in Compose callbacks via `LocalResources.current.getString(...)` (avoid `LocalContext.current.getString(...)`, which fails `LocalContextGetResourceValueCall`); construct `ContentTransform(..., sizeTransform = SizeTransform(...))` in helpers outside `AnimatedContentTransitionScope`; keep functions, `NavGraphBuilder` extensions, and `@Test` methods under Detekt's 120-line `LongMethod` limit (target `<= 80` lines for Compose UI tests).

## UI & Design System

Read `DESIGN.md` (v5.0.0 Luminous iOS & Atmospheric Canvas) before creating or changing any UI; it is the spec. Read `Color.kt`, `AppColors.kt`, `AppShapes.kt`, `IosShadowModifier.kt`, and `AppAtmosphericBackground.kt` for exact token and component names before using or adding any. Migrate one screen or flow at a time, tracking status in the full-app checklist inside `ui-ux-audit-guide.md` (Hạng mục 6).

Look (the easy-to-break parts of the v5 spec):
- Full-canvas `AppAtmosphericBackground` (with `forceSoftBlurOnPhoto = true` on data-heavy screens) and NO black (`ink`) hero cards: hero cards use Luminous iOS Elevated gradient (`HomeHeroCardLightTop/Bottom/Rim` or `HomeHeroCardDarkTop/Bottom/Rim`) + `Modifier.iosCardShadow(shape, elevated = true)`.
- Content cards, grouped cards, and floating header/filter pills use `AppTheme.colors.surface` + `0.5.dp` `hairline` border + `Modifier.iosCardShadow(shape, elevated = false)` placed BEFORE `clip`/`background`/`Surface`.
- `ink` (`primary`) is reserved for Primary CTA buttons and the floating bottom navigation pill. `sand` (`#F2D3A8`) is a highlight only (selected pills, active tabs, goal progress), never a large background.
- Numbers use Inter with tabular figures (`tnum`). Fraunces (`displaySerif`) only for screen/section titles and short accents, never amounts or paragraphs.
- Corner radii 28/24/20/16/12 and capsule/circle, no asymmetric corners. Sheets use `NotePayModalBottomSheet` (or `BottomSheetGlass` for 20dp-padded sheets; `sheetTop`, `surface`, 45% scrim, isolated `emptyBackdrop()`), never raw `ModalBottomSheet`.
- **Shared motion & predictive back:** Reuse `NotePayMotion.kt` helpers (`notePayForwardEnterTransition`/`ExitTransition`, `NotePayAnimatedContent`, `NotePayAnimatedVisibility`, `Modifier.notePayPressScale`, `Modifier.notePayStaggeredEntrance`, `rememberNotePayReducedMotion()`) instead of duplicating transition specs per screen. Use `Modifier.notePayStaggeredEntrance` (`graphicsLayer`) inside sheets and lists so frame-0 height does not jump. Preserve predictive-back contracts (`PredictiveBackCorners.kt`, `NotePayNavHost` `popEnterTransition`/`popExitTransition`, `PredictiveBackDestination`, `BackHandler`).
- **Unified status feedback:** Emit status feedback via `UiFeedback` (`FeedbackSnackbarHost` / `LatestFeedbackDispatcher`) and `NotePayStatusBanner`; never call `Toast.makeText`. Host validation errors inside the open sheet window and dismiss (`hide()` -> `!isVisible`) before forwarding `Success` to the root host; align confirmation dialog copy when Undo is available.

Hard rules:
1. Colors, shapes, shadows and type come from `AppTheme.colors`, `AppTheme.shapes`, `IosShadowModifier`, `Color.kt` tokens, and `AppTheme.typography`. No hard-coded hex in composables.
2. Spacing on the 4dp scale (4/8/12/16/24/32), screen margin 16dp, 120dp bottom clearance for the floating nav.
3. Text at least 12sp, touch targets at least 48dp, icon-only buttons have `contentDescription`.
4. Layouts survive Vietnamese text at font scale 1.3x to 2.0x: `Modifier.defaultMinSize(minHeight = 48.dp)` + `TextOverflow.Ellipsis`, never a fixed `height` on text containers. Inset metric tiles stack vertically at `fontScale >= 1.45f`.
5. Handle Loading, Empty, Error and Offline states.
6. Income/expense colors mean cash flow only. Status (safe/caution/over) = color + icon + text.
7. Contrast at least 4.5:1 in light and dark. Text over custom background photos requires the `AppAtmosphericBackground` scrim.
8. Before finishing a UI change: run the checklists in `ui-ux-audit-guide.md` (Hạng mục 1–6), check light/dark and font scale 1.3x/2.0x, then run `./gradlew.bat detekt :app:lintFullDebug :app:testFullDebugUnitTest :app:assembleFullDebug` (plus `:app:lintPlayDebug :app:testPlayDebugUnitTest` when `com.notepay.debug` is configured in `app/google-services.json`).

## Localization: Zero Hardcoded Strings

Never hardcode user-facing text (labels, titles, errors, `contentDescription`) in Compose or ViewModels. Use `stringResource(R.string.key)` / `pluralStringResource(...)` and update both files in the same change:
- `app/src/main/res/values/strings.xml` (Vietnamese)
- `app/src/main/res/values-en/strings.xml` (English)

For `strings.xml` cleanup, use the script instead of editing by hand; ensure `-en` has every translation before merging:
- Check: `python .agents/scripts/string_dedup.py app/src/main/res/values/strings.xml --check`
- Fix duplicate keys: `... --fix`
- Merge same-value keys (dry-run first): `... --merge-values --source-dir app/src/main`
- Verify 100% key parity between `values/strings.xml` and `values-en/strings.xml` before finishing any UI/feedback change.

## Testing

JUnit for unit tests, AndroidX for instrumentation. Name tests `ThingTest` with behavior methods, e.g. `suggest_returnsFood_forRestaurantNote`. Non-trivial logic leaves at least one small test that fails if the logic breaks. Money calculations, date ranges, Room migrations, and parsing/forecast edge cases need a test per edge case, not just one. Trivial one-liners need none. Run the relevant target before opening a PR.

## Security & Local Data

Financial data is local-first. Never log transaction contents, secrets, OCR images or notification text. No hardcoded secrets or tokens (credentials live in ignored local files). Internal components use `android:exported="false"`. Sensitive data is encrypted via KeyStore.

## Commits & Pull Requests

Conventional Commits (`feat:`, `fix:`, `refactor:`, `test:`), concise imperative, scoped. PRs: user-visible changes, verification commands run, linked specs/issues, screenshots or recordings for UI changes.

## Reply Format

Before a fix, state in one or two lines why the old code was wrong or misplaced (wrong layer, race, unsafe cast). Give the smallest correct diff. Add a closing note only when something was actually skipped, left unverified, or risky; otherwise say nothing extra.
