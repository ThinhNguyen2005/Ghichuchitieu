# Repository Architecture Refactor Plan

The tasks are dependency ordered. Each checkbox represents an independently verifiable batch.

## Phase 1 — Shared rules and pure presentation formatting

- [x] **P0 Transaction validation source of truth**
  - Current: literal note limits in transaction UI, ingestion, and local notification parsing.
  - Destination: `Transaction.MAX_NOTE_LENGTH` plus a domain truncation helper where truncation is required.
  - Dependencies: add/edit ViewModels and screens, `TransactionAnalyzer`, `NotificationParser`.
  - Tests: domain boundary tests and local notification parser tests.
- [x] **P0 Vietnamese money words**
  - Current: private function in `AddTransactionScreen.kt`.
  - Destination: pure `ui/formatter/VietnameseMoneyWordsFormatter.kt`.
  - Dependencies: add transaction amount presentation.
  - Tests: required numeric boundaries from 0 through 1 billion.
- [x] **P1 Transaction date labels**
  - Current: private clock/date calculation in `AddTransactionScreen.kt`.
  - Destination: pure presentation formatter receiving `today`, target date, and localized labels.
  - Dependencies: add transaction screen/state mapping.
  - Tests: today, yesterday, earlier date, month boundary.
- [x] **P1 Amount input parsing scope**
  - Current: `feature/addtransaction/AmountParser.kt` with duplicated numeric bound.
  - Destination: `ui/feature/transaction/AmountParser.kt` using `Money.MAX_MAJOR_UNITS` and exact `Money.fromMajorUnit` conversion.
  - Dependencies: add/edit ViewModels and parser tests.
  - Tests: blank, zero, separators, max valid, overflow.

## Phase 2 — Stats domain/presentation separation

- [x] **P0 Extract pure analytics calculations**
  - Current: `ui/feature/stats/StatsInsightsEngine.kt` calculations coupled to resources and formatting.
  - Destination: `domain/analytics/StatsInsightsCalculator.kt` and `StatsSummaryCalculator.kt` returning structured forecast, trend, daily-budget, advice/anomaly, and summary results.
  - Dependencies: Stats ViewModel/UI models and existing analytics models.
  - Tests: zero days, month boundaries, absent budget, threshold edges, negative/empty inputs.
- [x] **P0 Add presentation mapper**
  - Current: domain values and resource text built in one engine.
  - Destination: `ui/feature/stats/StatsInsightsUiMapper.kt` mapping structured results to `StatsUiText` and formatted money.
  - Dependencies: strings and `MoneyFormatter` remain presentation-only.
  - Tests: mapping branches and resource arguments.
- [x] **P1 Remove obsolete mixed engine**
  - Update all callers, delete old implementation, and search for remaining analytics calculations under Compose files.

## Phase 3 — Transaction feature boundary

- [x] **P1 Repackage transaction feature**
  - Current: `ui/feature/addtransaction` contains add, edit, and shared elements.
  - Destination: `ui/feature/transaction/add`, `edit`, and `components`; transaction input/picker UI no longer lives in app-global `ui/component`.
  - Dependencies: navigation, Hilt ViewModels, previews, tests, imports.
  - Tests: add/edit ViewModel tests and navigation compile.
- [x] **P1 Normalize transaction transient effects**
  - Removed `savedSuccessfully`; Add/Edit navigation success is represented by the `UiFeedback.Success` effect path.
  - Tests: save success/failure emits one effect and leaves persistent state valid.
- [x] **P1 Split oversized transaction screens by semantic section**
  - `TransactionFormSections.kt` now owns the independently previewable amount, date, wallet, note, type, and suggestion sections; add/edit routes keep orchestration and navigation.

## Phase 4 — Shared picker/component policy

- [x] **P1 Extract genuinely shared category selection content**
  - Audited transaction `CategoryGridPicker` against subscription's single-select picker. Their creation/search versus single-select semantics are intentionally different, so no unsafe merge was made; app-wide `CategoryAvatar` remains shared.
- [x] **P1 Scope wallet and transaction input components**
  - Transaction input, wallet picker, and transaction category pickers now live under `ui/feature/transaction/components`; app-wide primitives remain in `ui/component`.
- [x] **P2 Remove duplicate wrappers and obsolete imports**
  - Removed stale addtransaction/component imports and verified moved components have transaction-wide responsibility or multiple consumers.

## Phase 5 — ViewModel state/effect consistency

- [x] **P0 Inventory all ViewModels and classify persistent state versus one-shot feedback/navigation.**
  - Persistent loading/content/dialog state remains in feature `UiState`; save/delete/backup/category feedback is emitted through `UiFeedback`; navigation remains screen-owned.
- [x] **P1 Standardize on `StateFlow<UiState>` plus `SharedFlow<UiFeedback>` for one-shot messages.**
  - Audited all ViewModels. `TransactionDetailUiState.error` is persistent load/content state, while Stats local-advisor state and subscription dialogs are persistent operation/UI state rather than one-shot effects.
- [x] **P1 Replace duplicated success/error flags and local snackbar triggers feature by feature.**
  - Removed transaction save flags, backup flags, list snackbar state, wallet/add/edit inline transient errors, and home/bill-split/subscription duplicate error fields; root feedback host handles snackbar actions.
- [x] **P1 Keep Android strings in presentation; introduce `UiText` only where deferred resolution is needed, not as a domain dependency.**
  - Android resource resolution remains at presentation boundaries. `SuggestCategoryUseCase` now returns structured reason data and uses a domain `CategoryLearningStore`; `CategorySuggestionUiMapper` resolves localized text in UI.
- [x] **Tests:** reducers/state transitions, single effect emission, cancellation and retry behavior.
  - Focused Play tests cover backup, list, add/edit transaction, wallet, category suggestion, and platform refactors; cancellation paths retain explicit `CancellationException` handling.

## Phase 6 — Formatter, mapper, and theme cleanup

- [x] **P1 Consolidate presentation money/date formatting without moving locale formatting into domain.**
  - Shared date/header and compact-money formatting now live under `ui/formatter`/`ui/util`; feature-private copies were removed. AI prompt formatting remains provider-specific.
- [x] **P1 Keep persistence mapping in `data/mapper`; remove presentation mapping from entities/repositories.**
  - Audited the repository boundary; Room/entity conversions remain in `data/mapper`, while Stats/category suggestion UI mapping stays in presentation.
- [x] **P2 Document Material schemes versus semantic `AppColors`/`AppTypography`; rename only ambiguous symbols with proven call-site benefit.**
  - Material color schemes provide dynamic/theme surfaces; `AppTheme` semantic colors/shapes/typography/dimensions provide app tokens. They are complementary and documented without a risky rename.
- [x] **P2 Remove dead helpers and duplicate constants after caller migration.**
  - Removed obsolete stats/money engines and `TransactionDetailViewModel.clearError`; validation now references domain note/amount policies.

## Phase 7 — Repository-wide final audit and documentation

- [x] Re-run CodeGraph and searches for private formatter/parser/picker/validation helpers, literal note limits, feedback mechanisms, repository/context/resource dependencies, and dead package references.
  - Final audit found no stale transaction package/obsolete engine/success-flag references and no Android/Compose/UI imports in domain; remaining Context/Color/Toast hits are classified presentation, platform, service, or runtime-palette responsibilities.
- [x] Run Play and Local unit tests, lint, and debug assemblies using the configured Android Studio JBR.
  - `testPlayDebugUnitTest`, `testLocalDebugUnitTest`, `lintPlayDebug`, `lintLocalDebug`, `assemblePlayDebug`, and `assembleLocalDebug` all passed.
- [x] Update `docs/ARCHITECTURE.md` with package structure, dependency direction, state/effect convention, component scope, formatter/mapper rules, repositories, and extension examples.
- [x] Record any device-only behavior that remains unverified.
  - Hardware Liquid Glass/GPU/NPU LiteRT-LM, QR scanning, and live service/backend behavior remain device/live acceptance work, not inferred from JVM/build results.
