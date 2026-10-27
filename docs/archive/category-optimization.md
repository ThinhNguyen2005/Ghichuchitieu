# Plan - Category UI Harmonization & Suggestion Logic Optimization

This file defines the tasks, structure, and verification plan for refining the Category experience in the Ghichuchitieu application.

## Overview
- **Objective**: Harmonize Category selection UI between Add and Edit transaction screens, optimize SuggestCategoryUseCase prediction logic, fix custom category creation limitations (including Purple Ban compliance), and verify correctness with unit tests.
- **Project Type**: MOBILE (Android, Jetpack Compose)
- **Success Criteria**:
  - Consistent category selection UX on Add & Edit screens (quick-select row + bottom sheet).
  - Clear classification (tabs for Chi tiêu / Thu nhập) in the selection grid.
  - Predictable category suggestions (fallback to rules on zero vocab overlap).
  - No purple/violet choices in custom category creation; grid scrollable with all icons.
  - 100% pass on all unit tests.

---

## Proposed File Structure
- `app/src/main/java/com/notepay/ui/feature/addtransaction/CategoryPickerSheet.kt` [NEW]
- `app/src/main/java/com/notepay/ui/feature/addtransaction/CategoryGridPicker.kt` [MODIFY]
- `app/src/main/java/com/notepay/ui/component/TransactionInputComponents.kt` [MODIFY]
- `app/src/main/java/com/notepay/ui/feature/addtransaction/AddTransactionScreen.kt` [MODIFY]
- `app/src/main/java/com/notepay/ui/feature/addtransaction/EditTransactionScreen.kt` [MODIFY]
- `app/src/main/java/com/notepay/domain/usecase/SuggestCategoryUseCase.kt` [MODIFY]

---

## Task Breakdown

### Phase 1: Core Suggestion Logic
1. **Task 1.1**: Enhance `SuggestCategoryUseCase.kt`
   - **Agent**: `backend-specialist`
   - **Skills**: `clean-code`
   - **Input**: `SuggestCategoryUseCase.kt`
   - **Output**: Optimized prediction logic with vocabulary check fallback, stop words filtering, and richer static rules.
   - **Verify**: Run `.\gradlew.bat testDebugUnitTest` to ensure existing tests pass.

### Phase 2: Category Components & UI
2. **Task 2.1**: Update `TransactionInputComponents.kt` (`CategoryQuickSelectionRow`)
   - **Agent**: `mobile-developer`
   - **Skills**: `mobile-design`
   - **Input**: `TransactionInputComponents.kt`
   - **Output**: Updated quick selection row to show current selected category and custom categories of matching type first.
   
3. **Task 2.2**: Update `CategoryGridPicker.kt` and `AddCategoryDialog`
   - **Agent**: `mobile-developer`
   - **Skills**: `mobile-design`
   - **Input**: `CategoryGridPicker.kt`
   - **Output**: Tabbed Category picker (Expense vs Income), scrollable icon grid in custom creator dialog, updated colors list without purple/violet (Purple Ban).

4. **Task 2.3**: Create `CategoryPickerSheet.kt`
   - **Agent**: `mobile-developer`
   - **Skills**: `mobile-design`
   - **Input**: None (New File)
   - **Output**: A bottom sheet wrapper enclosing `CategoryGridPicker`.

### Phase 3: Screen Harmonization
5. **Task 3.1**: Refactor `AddTransactionScreen.kt` and `EditTransactionScreen.kt`
   - **Agent**: `mobile-developer`
   - **Skills**: `mobile-design`
   - **Input**: `AddTransactionScreen.kt`, `EditTransactionScreen.kt`
   - **Output**: Unified category pickers using `CategoryQuickSelectionRow` + `CategoryPickerSheet`.

---

## Phase X: Verification
- Run SuggestCategoryUseCase unit tests.
- Compile check: `.\gradlew.bat compileDebugSources`
- Manual check: Socratic gate and Purple Ban compliance.
