# Repository Architecture Refactor Audit

## Scope and method

The audit covers Kotlin sources under `app/src/main`, `app/src/local`, and
their tests. CodeGraph call paths were used before textual searches. The
refactor preserves navigation, Room schema, flavors, supplied assets, and the
existing LiteRT-LM/Phase 1 work.

## Resolved findings

### Stats and analytics

Forecast, summary, chart, and insight calculations are now pure domain
calculators under `domain/analytics`. `StatsInsightsUiMapper` owns resources,
localized text, and presentation money formatting. The mixed
`StatsInsightsEngine` was removed and its tests were split by responsibility.

### Transaction boundary

The former `ui/feature/addtransaction` package is now split into
`transaction/add`, `transaction/edit`, and `transaction/components`. Add/edit
success no longer relies on a persistent `savedSuccessfully` flag. Transaction
form sections, input components, pickers, and wallet selection are scoped to
the transaction feature.

### State and effects

The ViewModel/screen audit established a project-wide convention:

- persistent loading/content/operation/dialog state stays in `StateFlow<UiState>`;
- one-shot save/delete/import/category feedback uses `SharedFlow<UiFeedback>`;
- navigation is handled by screens/navigation host;
- snackbar actions are dispatched through the root feedback host.

Backup, list, wallet, add/edit transaction, bill-split, home, and subscription
duplicate transient fields were removed or classified. `TransactionDetail`'s
error and Stats local-advisor status remain persistent state by design.

### Android/domain boundary

`SuggestCategoryUseCase` no longer imports `Context`, `SharedPreferences`, or
`R.string`; it consumes the domain `CategoryLearningStore` interface and
returns structured suggestion reasons. `CategoryLearningStoreImpl` is the
Android preferences adapter, while `CategorySuggestionUiMapper` resolves
localized text in presentation. `OsCompatHelper` was moved from `domain.util`
to `platform`. A repository-wide domain import audit found no Android or UI
dependency remaining in `domain`.

Android dependencies still present in ViewModels are presentation adapters or
required platform operations: backup needs `ContentResolver`/`Uri`, Home and
Stats coordinate model/reminder imports, and several feature ViewModels resolve
localized feedback. These are documented exceptions, not domain leaks; no
`NavController` is injected into a ViewModel.

### Validation and formatting

Note limits use `Transaction.MAX_NOTE_LENGTH`/`TransactionNotePolicy`, and
amount parsing uses `Money.MAX_MAJOR_UNITS`/`Money.fromMajorUnit`. Shared date,
date-header, compact-money, Vietnamese money-word, and transaction amount
formatting no longer has duplicate screen-private implementations. Persistence
mapping remains in `data/mapper`.

### Components and theme

The category picker audit kept transaction creation/search and subscription
single-select semantics separate. `CategoryAvatar` and common panels remain
app-wide; wallet/input/category transaction components remain feature-scoped.

Material color schemes and `AppTheme` semantic tokens have different jobs:
Material handles active/dynamic palettes, while `AppColors`, typography,
shapes, and dimensions express NotePay semantics. Runtime category/wallet
palettes, provider branding, contrast colors, and feature-specific status
illustrations are intentionally allowed to use explicit colors. Generic theme
surfaces should use the token layers.

Obsolete `StatsInsightsEngine`, `MoneyFormatterEngine`, stale package imports,
and the unused `TransactionDetailViewModel.clearError` helper were removed.

## Residual findings / acceptance boundaries

- Some ViewModels resolve presentation strings directly because `UiFeedback`
  currently carries resolved text. This remains a presentation-only dependency;
  a broad `UiText` migration would be a separate API change with no current
  behavior benefit.
- Device-only acceptance is still separate from JVM/build verification. No
  claim is made here about hardware Liquid Glass rendering, GPU/NPU LiteRT-LM
  runtime behavior, QR scanning, or live service/backend behavior without a
  corresponding device/live environment.
- Android lint may continue to report existing project/toolchain warnings; the
  final verification records exact task results rather than treating warnings
  as functional failures.

## Final verification

Focused Play/Local tests passed for amount parsing, Stats
calculators/mapping/chart, transaction add/edit, backup, list, wallet, category
learning, presentation formatters, `OsCompatHelper`, and notification note
truncation. The complete gate passed with Android Studio JBR 21:

- `:app:testPlayDebugUnitTest`
- `:app:testLocalDebugUnitTest`
- `:app:lintPlayDebug`
- `:app:lintLocalDebug`
- `:app:assemblePlayDebug`
- `:app:assembleLocalDebug`

`git diff --check` reports no whitespace errors. Gradle still prints ordinary
toolchain/cache warnings, but no task failed. No ADB/device or live-service
acceptance was claimed.
