# NotePay — Trạng thái dự án (Phase 1)

> **Phase 1 hoàn tất xuất sắc.** Domain layer + test xong. Data layer + DI xong. Toàn bộ UI (Home, AddTransaction, TransactionList, Stats) hoạt động mượt mà. Đã hoàn thành bộ test suite toàn diện bao gồm cả Unit Test, Database Test và Security Test.

## ✅ Đã hoàn thành

### Build & Config
- `gradle/libs.versions.toml` — AGP 9.1.1, Kotlin 2.2.10, Compose BOM 2025.12, Hilt 2.57, Room 2.8.4
- `app/build.gradle.kts` — KSP, Hilt, Room schema export, testOptions
- `proguard-rules.pro` — Hilt + Room rules
- `app/src/main/AndroidManifest.xml` — `allowBackup=false`, 0 permission, edge-to-edge ready
- `res/xml/backup_rules.xml` + `data_extraction_rules.xml` — exclude DB
- `res/values/themes.xml` + `res/values-night/themes.xml` — Theme.NotePay

### Domain layer (`com.notepay.domain`)
- `model/Money.kt` — value class cents/Long, có `+`, `-`, `unaryMinus`, `compareTo`, `abs`, `fromMajorUnit`
- `model/Category.kt` — enum 10 category
- `model/TransactionType.kt`
- `model/Transaction.kt` — init validation: amount ≠ 0, note ≤ 200, walletId > 0
- `model/Wallet.kt` — init validation + `default()` factory
- `repository/TransactionRepository.kt`, `WalletRepository.kt` — interface thuần Kotlin
- `usecase/AddTransactionUseCase.kt` — Result<Long>
- `usecase/GetTransactionsUseCase.kt`
- `usecase/GetMonthlySummaryUseCase.kt` — Summary data class với byCategory
- `usecase/DeleteTransactionUseCase.kt`
- `usecase/ObserveWalletBalanceUseCase.kt` — combine wallets + transactions

### Data layer (`com.notepay.data`)
- `local/entity/TransactionEntity.kt` — FK → wallets, index wallet_id + occurred_at
- `local/entity/WalletEntity.kt` — Sửa từ index UNIQUE sang Index thông thường trên `is_active` để giải quyết lỗi ghi đè dữ liệu và crash constraint khi chuyển đổi active.
- `local/dao/TransactionDao.kt` — observeAll, observeByRange, observeByWallet, getById, upsert, delete
- `local/dao/WalletDao.kt` — observeAll, observeActive, setActiveExclusive (Transaction)
- `local/NotePayDatabase.kt` — version 1, exportSchema=true
- `local/SeedCallback.kt` — tạo ví "Tiền mặt" lần đầu
- `mapper/TransactionMapper.kt`, `WalletMapper.kt` — toDomain/toEntity
- `repository/TransactionRepositoryImpl.kt` — flowOn(IoDispatcher), monthRange helper
- `repository/WalletRepositoryImpl.kt` — flowOn(IoDispatcher)

### DI (`com.notepay.di`)
- `Qualifiers.kt` — `@IoDispatcher`, `@DefaultDispatcher`
- `DispatcherModule.kt`
- `DatabaseModule.kt` — Room build với SeedCallback
- `RepositoryModule.kt` — @Binds

### App entry
- `NotePayApp.kt` — `@HiltAndroidApp`
- `MainActivity.kt` — single-Activity, `@AndroidEntryPoint`, edge-to-edge

### UI layer (`com.notepay.ui`)
- `theme/Color.kt` — Light + Dark M3 palette (full token)
- `theme/Type.kt` — Material 3 type scale
- `theme/Theme.kt` — Dynamic Color (Android 12+) + fallback
- `util/MoneyFormatter.kt` — locale VN
- `navigation/Route.kt` — sealed
- `navigation/NotePayNavHost.kt` — bottom bar 3 tab
- `component/BalanceCard.kt`, `KpiRow.kt`, `EmptyState.kt`, `TransactionItem.kt`
- `feature/home/HomeUiState.kt`, `HomeViewModel.kt`, `HomeScreen.kt` — hoạt động
- `feature/addtransaction/AddTransactionScreen.kt` — Hoàn thành (SegmentedButton, nhập tiền realtime, Grid category 3 cột, note counter, DatePicker, lưu giao dịch)
- `feature/list/TransactionListScreen.kt` — Hoàn thành (LazyColumn, Search theo nội dung/danh mục, lọc theo Category, Swipe to Delete, Snackbar Undo)
- `feature/stats/StatsScreen.kt` — Hoàn thành (Canvas Donut Chart, KPI Card, Breakdown Progress Bar, Next/Prev Month)

### Tests đã viết (100% PASS)
- `domain/model/MoneyTest.kt` — Sửa assert logic đúng 5_000_000L.
- `domain/model/TransactionTest.kt` — 5 test (init validation)
- `domain/usecase/AddTransactionUseCaseTest.kt` — 4 test (MockK + runTest)
- `domain/usecase/GetMonthlySummaryUseCaseTest.kt` — 3 test (Turbine)
- `domain/usecase/DeleteTransactionUseCaseTest.kt` — 2 test
- `domain/TestData.kt` + `domain/model/TestTransactionFactory.kt` — fixture
- `ui/feature/addtransaction/AmountParserTest.kt` — Unit test cho parser tiền tệ độc lập.
- `ui/feature/addtransaction/AddTransactionViewModelTest.kt` — Test UDF validation, save state.
- `ui/feature/list/TransactionListViewModelTest.kt` — Test tìm kiếm, lọc danh mục, xoá, và undo.
- `ui/feature/stats/StatsViewModelTest.kt` — Test breakdown percentage, sort giảm dần, chuyển tháng.
- `data/local/DatabaseTest.kt` — Test in-memory Room DB (WalletDao + TransactionDao) chạy trên Robolectric.
- `data/local/SecuritySmokeTest.kt` — Chạy Robolectric để test an toàn bảo mật manifest (chặn internet, allowBackup = false).
- **Tổng: ~45 tests, PASS 100%**

## 🚀 Chạy ứng dụng hoặc test

```bash
cd d:\Ghichuchitieu
# Chạy toàn bộ test suite
gradlew.bat :app:testDebugUnitTest
# Build APK Debug
gradlew.bat :app:assembleDebug
```
