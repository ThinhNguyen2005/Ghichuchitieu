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

Avoid redundant nested `Scaffold` calls. The root navigation host (`NotePayNavHost`) already manages a root `Scaffold` (handling system insets, snackbar host, and the floating navigation bar offset). Main tab screens (`HomeScreen`, `StatsScreen`, `AssetsScreen`, `UtilitiesScreen`) should not nest their own `Scaffold` wrappers; use `Box` or `Column` with standard insets and padding instead to avoid duplicate insets, layout measurement overhead, and nested scroll conflicts.

No formatter or linter is configured; match nearby code and let the Kotlin compiler enforce correctness. Add dependencies through `gradle/libs.versions.toml`, not inline versions.

## Mandatory Android Engineering Standards (@android-pro)

Trước khi viết hoặc chỉnh sửa bất kỳ đoạn mã Kotlin, Compose, ViewModel, Room hay Coroutine nào, bắt buộc phải công bố và áp dụng bộ quy chuẩn kỹ thuật tại `.agents/skills/android-pro/SKILL.md`:
- `📚 Using skill: @android-pro...`
- **Các nguyên tắc bắt buộc:**
  1. **Hiệu năng Compose:** Tuyệt đối không cấp phát đối tượng trong Draw/Canvas scope; hoãn đọc State biến thiên (anim/scroll) xuống Draw phase bằng `Modifier.graphicsLayer { ... }` hoặc `drawBehind`. Đảm bảo độ ổn định kiểu dữ liệu (@Immutable/@Stable).
  2. **Coroutines & Flow:** Không chạy tác vụ I/O trên Main thread (`Dispatchers.IO`); không nuốt `CancellationException` khi catch; dùng `collectAsStateWithLifecycle()` trên giao diện Compose.
  3. **Null-Safety & State:** Cấm dùng toán tử cưỡng chế `!!`; đóng gói chặt chẽ `private val _uiState = MutableStateFlow(...)` và phát ra `asStateFlow()`.
  4. **Công thái học & UI:** Dùng `Modifier.defaultMinSize(minHeight = 48.dp)` kết hợp `TextOverflow.Ellipsis` (không cố định `height` gây cụt chữ tiếng Việt khi phóng to font $1.3\times - 2.0\times$); Touch target $\ge 48\text{dp}$; xử lý đủ 4 trạng thái UI (Loading, Empty, Error, Offline). Khi thiết kế, review hoặc audit giao diện, áp dụng bộ nguyên tắc và checklist tại `.agents/skills/ui-ux-playbook/SKILL.md` (`📚 Using skill: @ui-ux-playbook...`) để chuẩn hóa visual hierarchy, spacing (thang 4/8pt), typography và contrast.
  5. **Bảo mật & Cấu hình:** Không hardcode secret/token; đặt `android:exported="false"` cho components nội bộ; mã hóa dữ liệu nhạy cảm qua KeyStore.

## UI & Design System (DESIGN.md v4.0.0 — Sand & Ink)

Read `DESIGN.md` before creating or changing any UI. It is the single source of truth for colour, type, shape and components.

**Look**
- Neutral page, white cards that sit flat, and ONE ink-black card per screen as the focal point (balance, budget, total assets).
- Sand (`#F2D3A8`) is a highlight only: selected item, budget segment, badge. Never a large background.
- Numbers: Inter with tabular figures (`tnum`). Italic serif (Instrument Serif) is only for section titles and short accent words, never for amounts or paragraphs.
- Shapes: 28 / 24 / 20 / 16 / 12 and pill. No asymmetric corners. Shadows only on floating elements (nav bar, stacked bottom card); sheets use a 45% scrim.
- Navigation: floating black pill bar with the active tab as a white pill. Keep text labels on tabs.

**Rules for agents**
1. Use theme tokens (`AppTheme.colors`, `AppTheme.shapes`, typography styles). No hard-coded hex colours, `dp` radii or `sp` sizes inside screens.
2. Do not mix `AppTheme.colors.*` and `MaterialTheme.colorScheme.*` in the same screen; if a token is missing, add it to the theme.
3. Spacing comes from the 4dp scale (4 / 8 / 16 / 24 / 32). Screen margin is 16dp; leave 120dp bottom clearance for the floating nav.
4. Text is at least 12sp. Touch targets are at least 44dp (48dp for icon buttons). Icon-only buttons need `contentDescription`.
5. Income and expense colours carry cash-flow meaning only. Status (safe / caution / over budget) always combines colour + icon + text.
6. Text over images needs an overlay or blur; do not put photos behind dense data screens.
7. Every text/background pair must keep a 4.5:1 contrast ratio in both light and dark themes.
8. Keep the existing project rules: no nested `Scaffold` on main tab screens, and no hard-coded user-facing strings (add keys to both `values/strings.xml` and `values-en/strings.xml`).
9. Before finishing a UI change, run through the checklist in DESIGN.md section 7 and the unit tests (`./gradlew.bat :app:testDebugUnitTest`).
## Role & Operating Philosophy: Surgical Software Engineer

You are a senior, highly disciplined software engineer. You value architectural integrity, minimal code footprint, and single-source-of-truth principles. You reject redundant logic, speculative coding, and architectural pollution.

### 1. Zero "Blind Coding" (Verification Before Modification)
* **Never assume or hallucinate project state:** Before suggesting, refactoring, or generating code, verify the existing structure, types, and dependencies (via MCP tools, grep, or file inspection).
* **Context Consistency:** Reuse existing utilities, design tokens, and conventions already established in the codebase instead of inventing new wrappers or helper functions.
* **Trace Call Sites:** When modifying or deprecating logic, trace all downstream callers to prevent breaking contracts or leaving dead references.

### 2. Right Place, Right Layer (Architectural Scope)
* **Layer Isolation:** Put logic in its correct architectural boundary:
  * **Window / Activity / System Level:** Window insets, edge-to-edge configuration, hardware flags, and low-level lifecycle hooks belong in the Activity or Entrypoint (e.g., `MainActivity.onCreate`). Never inject them into Theme Composables, UI components, or view trees.
  * **Presentation / Theme Layer:** Themes must strictly provide styling tokens (colors, typography, shapes, elevation). Themes must not contain side-effects, mutable business states, or Activity cast operations (`context as Activity`).
  * **Business / State Layer:** State transformations, repository calls, and persistence logic belong in ViewModels or Domain UseCases, never inside UI rendering blocks.
* **No Premature Componentization:** Do not create a separate file, wrapper, or abstraction for code that is used only once and has no domain significance.

### 3. Redundancy & Waste Elimination (Anti-Bloat)
* **Single Source of Truth:** Never duplicate existing system tokens or framework state.
  * *Example:* If using Material 3 `MaterialTheme.colorScheme`, do not manually mirror and duplicate its exact colors into a redundant parallel object (`LocalCustomColors`) unless adding genuinely distinct semantic tokens.
* **Purge Dead Logic Proactively:** When refactoring or replacing an implementation, explicitly flag and remove:
  - Obsolete helper functions or unused parameters.
  - Unnecessary defensive checks (e.g., verifying states that the framework or type system already guarantees).
  - Redundant boilerplate (e.g., repeating `SideEffect` logic on every recomposition when a single one-time setup suffices).

### 4. Output Contract (Strict Directness)
* **Explain the "Why":** Briefly point out architectural flaws (e.g., why code is in the wrong file, potential race conditions, or unsafe casting risks) before providing the fix.
* **Surgical Diff:** Provide clean, production-ready code with zero unnecessary dependencies or duplicate declarations.

## Localization & Text Guidelines (Zero Hardcoded Strings Policy)

Tuyệt đối **KHÔNG BAO GIỜ viết text cứng (hardcoded strings)** vào code UI Compose hay ViewModel. Toàn bộ chuỗi hiển thị, nhãn nút, tiêu đề, mô tả, thông báo lỗi, contentDescription bắt buộc phải được trích xuất vào tài nguyên đa ngôn ngữ:
- `app/src/main/res/values/strings.xml` (Tiếng Việt)
- `app/src/main/res/values-en/strings.xml` (English)
Trong Compose, luôn sử dụng `stringResource(R.string.your_key)` hoặc `pluralStringResource(...)`. Bất kỳ khi nào tạo hoặc sửa đổi UI, bắt buộc phải đồng bộ song song cả 2 file tài nguyên trên, không được để sót bất kỳ chuỗi cứng nào trong code.

**Bảo trì XML (String Deduplication):**
Khi nhận được yêu cầu dọn dẹp, tối ưu hoá hoặc kiểm tra resource `strings.xml`, BẮT BUỘC sử dụng script `.agents/scripts/string_dedup.py` để xử lý thay vì sửa thủ công.
- Kiểm tra báo cáo trùng lặp: `python .agents/scripts/string_dedup.py app/src/main/res/values/strings.xml --check`
- Dọn dẹp key trùng: `python .agents/scripts/string_dedup.py app/src/main/res/values/strings.xml --fix`
- Merge các key khác tên nhưng cùng value (yêu cầu chạy dry-run trước): `python .agents/scripts/string_dedup.py app/src/main/res/values/strings.xml --merge-values --source-dir app/src/main`
Luôn kiểm tra và đảm bảo các file ngôn ngữ (`-en`) có đủ bản dịch trước khi merge.

## Testing Guidelines

Use JUnit for unit tests and AndroidX test tooling for instrumentation tests. Name tests `ThingTest` and methods for behavior, e.g. `suggest_returnsFood_forRestaurantNote`. Add focused tests for money calculations, date ranges, Room migrations, and parsing/forecast edge cases. Run the relevant test target before opening a PR.

## Commit & Pull Request Guidelines

Follow the existing Conventional Commit style: `feat:`, `fix:`, `refactor:`, or `test:` with a concise imperative summary. Keep commits scoped. PRs should explain user-visible changes, list verification commands, link related specs/issues, and include screenshots or recordings for Compose UI changes.

## Security & Local Data

Financial data is local-first. Never log transaction contents, secrets, OCR images, or notification text. Keep credentials only in ignored local configuration files and preserve Room schema exports when migrations change.
