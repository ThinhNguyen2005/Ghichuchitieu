# Feature Specification: Baseline Specification

**Feature Branch**: `001-baseline-spec`

**Created**: 2026-07-13

**Status**: Approved

**Input**: User description: "Feature: Baseline Specification — NotePay"

## Clarifications

### Session 2026-07-13

- Q: Multi-currency wallets or single base currency? → A: Single base currency.
- Q: Local LLM (Gemini Nano) or rule-based offline heuristic engine? → A: Rule-based offline heuristic engine (StatsInsightsEngine).
- Q: Android Notification Listener Service or SMS read permission? → A: Android Notification Listener Service.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Expense and Income Tracking (Priority: P1)

A user records their daily expenses and incomes to keep track of their cash flow.

**Why this priority**: Core functionality of NotePay; the application has no value without recording transactions.

**Independent Test**: Can be verified by adding a transaction (income/expense) on the transaction screen and confirming that the wallet balance updates and the transaction appears in the list.

**Acceptance Scenarios**:

1. **Given** the user is on the Add Transaction screen, **When** they enter an expense of $10 for "Food" from the "Cash" wallet, **Then** the transaction is recorded, the "Cash" wallet balance decreases by $10, and the transaction is displayed in the list.
2. **Given** the user is on the Add Transaction screen, **When** they enter an income of $1000 for "Salary" to the "Bank" wallet, **Then** the transaction is recorded, the "Bank" wallet balance increases by $1000, and the transaction is displayed in the list.

---

### User Story 2 - Budget and Multi-Wallet Management (Priority: P1)

A user manages multiple wallets (e.g., Cash, Bank) and sets monthly budget limits for different categories to prevent overspending.

**Why this priority**: Key to personal financial planning; supports multi-source incomes and spending limits.

**Independent Test**: Verify budget progress indicator updates immediately when a transaction is added under a budgeted category.

**Acceptance Scenarios**:

1. **Given** multiple wallets exist, **When** the user transfers $100 from "Bank" to "Cash", **Then** the "Bank" wallet balance decreases by $100, the "Cash" wallet balance increases by $100, and a transfer record is created.
2. **Given** a budget of $200 is set for "Food", **When** the user records an expense of $50 under "Food", **Then** the system shows the remaining budget as $150 (75% remaining).

---

### User Story 3 - Local AI Insights and Analytics (Priority: P2)

A user reviews category-wise spending analytics and gets intelligent financial recommendations processed entirely offline.

**Why this priority**: Differentiator feature providing intelligent financial planning while maintaining absolute privacy.

**Independent Test**: Verify spending charts load and AI insights generate locally without any network requests.

**Acceptance Scenarios**:

1. **Given** transaction history exists, **When** the user views the Analytics tab, **Then** a category breakdown pie chart is generated showing percentage distributions of expenses.
2. **Given** active transactions, **When** the user requests financial insights, **Then** the system analyzes spending patterns and displays local text suggestions on how to optimize budgets.

---

### Edge Cases

* **Low Device Storage**: What happens when local DB storage limits are reached? The system must warn the user and safely restrict database operations without crashing.
* **Notification Parsing on System Restart**: What happens when the device restarts? The background notification listener service must restart automatically to capture incoming bank alerts.

## Requirements *(mandatory)*

### Functional Requirements

* **FR-001**: System MUST operate fully offline without requiring an active internet connection or external servers.
* **FR-002**: System MUST persist all wallets, transactions, and bill splits locally in a secure database (Room).
* **FR-003**: System MUST support multiple wallets with independent balances.
* **FR-004**: System MUST allow setting wallet-specific budget limits and track remaining limits.
* **FR-005**: System MUST provide visual analytics (charts and stats) for income/expense distribution.
* **FR-006**: System MUST support a single base currency for all wallets and transactions, avoiding decimal rounding errors by storing money as integer cents (Long).
* **FR-007**: System MUST generate financial insights locally using an offline rule-based heuristic engine (StatsInsightsEngine).
* **FR-008**: System MUST read incoming bank notifications using the Android Notification Listener Service.
* **FR-009**: System MUST parse bank notifications locally to automatically identify and draft transactions.
* **FR-010**: System MUST automatically detect recurring subscriptions based on historical transaction frequency.
* **FR-011**: System MUST support splitting bills with automatic debtor status tracking (BillSplit).

### Key Entities

* **Wallet**: Represents a financial account (e.g. Cash, Bank). Attributes: ID, name, initial balance cents, icon, color, active state, linked package name, bank BIN, account number, account name, budget limit cents.
* **Transaction**: Represents an income, expense, or transfer. Attributes: ID, amount cents, type (Income/Expense/Transfer), category ID, wallet ID, timestamp, note, auto-capture flag, internal transfer flag.
* **Category**: Represents transaction groups (e.g. Food, Salary). Custom categories are persisted via SharedPreferences and registered dynamically.
* **Subscription**: Represents a recurring payment alert. Attributes: ID, name, amount cents, category, next due date, repeat months, remind days before, note, active state.
* **BillSplit**: Represents a shared bill. Attributes: ID, transaction ID, debtor name, amount cents, paid state, memo code, paid timestamp.

## Success Criteria *(mandatory)*

### Measurable Outcomes

* **SC-001**: 100% of user data is stored locally on the device (privacy-first, offline-first).
* **SC-002**: User can create a wallet and add a transaction in under 15 seconds.
* **SC-003**: Analytics charts render within 200ms of navigation.
* **SC-004**: Local financial insights are generated within 500ms of user request.

## Assumptions

* SQLite/Room is used for local data persistence.
* Local AI/heuristic analysis respects absolute privacy.
* Standard Android device features (like Notification Listener Service) are used for notification parsing.
