# Refactor Database Queries (Anti-Pattern Fix)

## Objective
Remove the anti-pattern of fetching entire tables into memory (e.g., `observeAll()`) and performing Kotlin-side aggregation/filtering (`sumOf`, `filter`). Replace these with optimized SQLite `@Query` functions in Room DAOs to improve performance, save memory, and prevent OOM errors.

## Phase 1: DAO Enhancements
Add specific query methods to DAOs to handle statistics and aggregations natively in SQLite.

- [ ] **`TransactionDao`**:
  - `observeSumByTypeAndWalletInRange(type, walletId, start, end)`
  - `observeSumByTypeInRange(type, start, end)`
  - `observeTransactionsInRange(start, end, walletId)`
- [ ] **`WalletDao` & `SubscriptionDao`**:
  - Evaluate if similar optimized queries are needed (e.g., sum of subscriptions due soon).

## Phase 2: Repository Updates
Expose the new DAO methods in the Repository interfaces and their implementations.

- [ ] **`TransactionRepository`**:
  - Add mapping for the new aggregation and filtering flows.

## Phase 3: ViewModel Refactoring (The Bottlenecks)
Refactor the most problematic ViewModels that currently use `observeAll()` for aggregation.

- [ ] **`HomeViewModel.kt`**:
  - Replace `transactionRepo.observeAll()` with direct queries for `monthlyIncome`, `monthlyExpense`, and `walletBalance`.
- [ ] **`StatsViewModel.kt`**:
  - Refactor `baseState` to rely on time-bounded SQL queries instead of loading all transactions and filtering by time.
- [ ] **`AssetsViewModel.kt`**:
  - Refactor total assets calculation to use SQL `SUM` instead of folding in Kotlin.
- [ ] **Background Workers (`WeeklyDigestWorker`, `DailyReminderWorker`)**:
  - Replace `transactionRepo.observeAll().firstOrNull()` with scoped `SELECT SUM` queries.

## Agent Assignments
- **@android-pro**: Enforce the new rule 12 against memory-heavy Kotlin aggregations. Validate Compose flows and ViewModel updates.
- **@database-design**: Ensure SQL queries are optimal (e.g. correct indexing on `type`, `wallet_id`, and `occurred_at`).

## Phase 4: Verification Checklist
- [ ] **Memory Profiling**: Verify RAM consumption drops significantly when navigating to Home and Stats tabs.
- [ ] **Compilation**: Run `./gradlew assembleDebug` successfully.
- [ ] **Correctness**: Validate that Home Wallet summaries and Stats chart totals match exactly as they did before the refactor.
