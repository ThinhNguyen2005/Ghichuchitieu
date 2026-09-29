# NotePay architecture

This document describes the architecture currently implemented in the Android
module. It is intentionally pragmatic: boundaries make responsibilities and
tests clear, but the project does not add a use case or wrapper when a direct
repository call is already the right abstraction.

## Dependency direction

```text
Compose screens / ViewModels
        ↓
presentation state, events, effects, formatters, UI mappers
        ↓
domain models, policies, analytics, and repository interfaces
        ↑
data/local/platform implementations and adapters
```

The `domain` package is Kotlin-only. It does not import Android, Compose,
resources, or presentation types. Android-specific persistence and compatibility
code is kept in `data` or `platform`, while localized text and visual mapping
are resolved in `ui`.

## Package structure

```text
com.notepay
├── domain
│   ├── model/          invariants and entities (`Money`, `Transaction`, `Category`)
│   ├── analytics/      pure forecast, summary, chart, and insight calculations
│   ├── repository/     repository and persistence-boundary interfaces
│   ├── usecase/        reusable business operations
│   ├── ingestion/      notification/message analysis
│   ├── money/          currency parsing and Vietnamese money-word logic
│   └── util/           Kotlin-only domain helpers
├── data
│   ├── local/          Room database, DAOs, and entities
│   ├── mapper/         entity ↔ domain conversions
│   ├── repository/     repository implementations
│   ├── preferences/    DataStore/SharedPreferences adapters
│   ├── backup/         JSON/file backup adapters
│   └── remote/         Android-backed remote/bundled data adapters
├── platform/           Android compatibility checks (`OsCompatHelper`)
└── ui
    ├── feature/<name>/ screen, ViewModel, UiState, and feature-local UI
    ├── feature/transaction/{add,edit,components}/ transaction boundary
    ├── feedback/       `UiFeedback` and feedback types
    ├── formatter/      pure presentation date/text formatters
    ├── util/           shared presentation formatting/helpers
    ├── component/      app-wide UI components
    ├── theme/          Material schemes and NotePay semantic tokens
    └── navigation/     route definitions and screen-owned navigation
```

The `local` source set contains offline notification capture and its service
integration. The `play` source set contains Play Services-backed behavior.
Both source sets use the same domain contracts where possible.

## Feature and component boundaries

Transaction creation and editing share a feature boundary, but their route
orchestration remains separate:

- `transaction/add` owns creation state and actions.
- `transaction/edit` owns editing state and actions.
- `transaction/components` owns transaction-wide input, wallet, category, and
  form sections. `TransactionFormSections` keeps large screens readable and
  provides independently testable/previewable sections.
- `ui/component` is reserved for components used across unrelated features,
  such as `CategoryAvatar`, transaction list items, and common panels.

The transaction category grid and subscription category picker are deliberately
not merged: the former supports transaction filtering/creation behavior and
the latter is a single-select subscription flow. Similar names do not imply
shared semantics.

- `ui/feature/debt` owns debt and loan management (`DebtManagementScreen`, `DebtDetailScreen`,
  `DebtRemindBottomSheet`, `CreateDebtBottomSheet`, `RecordPaymentDialog`, `DebtSummaryCard`).
  It manages two-way debt tracking (`LEND` / `BORROW`), partial repayment history, dynamic
  VietQR generation with pre-filled SMS/share reminder messages, and optional wallet transaction synchronization.
  See [DEBT_MANAGEMENT.md](DEBT_MANAGEMENT.md) for data schemas and use cases.

## State, events, and effects

ViewModels expose persistent screen data as `StateFlow<FeatureUiState>` and
accept user intent through explicit methods/events. One-shot user feedback is
emitted through `SharedFlow<UiFeedback>` and collected by the screen/root
feedback host. Navigation is supplied by the screen or navigation host; no
ViewModel owns a `NavController`.

Persistent operation state is kept in `UiState`, including loading/content
errors, dialog visibility, undo data, and local-advisor availability/results.
For example, `TransactionDetailUiState.error` describes the current load
state, not a snackbar event. Save/delete/import feedback is not duplicated in a
success boolean or a manually reset nullable message.

## Domain, presentation, and mapping rules

- Domain calculations return structured values. `StatsInsightsCalculator`,
  `StatsSummaryCalculator`, and `StatsChartCalculator` contain no resource or
  Compose dependency.
- `StatsInsightsUiMapper` converts structured analytics results to localized
  `StatsUiText` at the presentation boundary.
- `CategorySuggestionUseCase` returns a structured
  `CategorySuggestionReason`; `CategorySuggestionUiMapper` resolves its
  localized label. The use case depends on the domain `CategoryLearningStore`
  interface, implemented by `CategoryLearningStoreImpl` in `data`.
- Presentation formatting lives in `ui/formatter` or `ui/util`. Shared date
  labels use `PresentationDateFormatter` and
  `TransactionDateHeaderFormatter`; money display uses `MoneyFormatter`.
  Provider-specific AI prompt formatting is intentionally local to the AI
  adapter.
- Persistence entity/domain conversions remain in `data/mapper`.

## Source-of-truth policies

Business limits and normalization rules live with the domain model/policy:

- `Transaction.MAX_NOTE_LENGTH` and `TransactionNotePolicy` define note bounds
  and truncation.
- `Money.MAX_MAJOR_UNITS` is the amount bound.
- `ui/feature/transaction/AmountParser` delegates amount conversion to
  `Money.fromMajorUnit` and uses the domain maximum rather than a second
  literal.

UI may provide immediate input feedback, but it must use these same policies.

## Theme and design tokens

`Theme.kt` supplies the active Material `ColorScheme`, including dynamic and
named theme palettes. `AppColors`, `AppTypography`, `AppShapes`, and
`AppDimensions` are NotePay semantic tokens exposed through `AppTheme`; they
are complementary to Material tokens rather than duplicate replacements.

Hard-coded colors found outside the theme are intentional when they represent
runtime/category/wallet palettes, contrast calculation, provider branding, or
feature-specific status illustrations. Generic surfaces and state colors
should use `MaterialTheme.colorScheme` or `AppTheme.colors` when the existing
visual contract permits it.

## Adding a feature

1. Put durable business data and invariants in `domain/model` or a pure domain
   policy; add a repository interface under `domain/repository` only when the
   boundary is needed.
2. Put Room, preferences, files, and Android APIs in `data`/`platform`, then
   bind implementations from the DI modules.
3. Add `ui/feature/<feature>/FeatureUiState`, explicit intent methods/events,
   and a `SharedFlow<UiFeedback>` only for one-shot feedback.
4. Keep localized strings, date/number formatting, icons, and Compose models in
   presentation. Add a pure formatter or UI mapper when the logic is shared or
   has meaningful edge cases.
5. Add focused tests for pure calculations, policies, mappers, and state/effect
   transitions. Verify the flavor-qualified unit tests, lint, and debug build.

## Verification boundaries

Repository unit/build verification covers the Play and Local source sets. ADB
device behavior is a separate acceptance boundary: hardware rendering,
Liquid Glass compatibility, GPU/NPU LiteRT-LM execution, QR scanning, and live
service/backend behavior require an appropriate device or live environment and
are not inferred from a successful JVM test or APK assembly.
