# 📘 SPEC KỸ THUẬT — NotePay (Smart Budget Tracker)
> **App ghi chú chi tiêu Local-First** | Kotlin · Jetpack Compose · Material 3 · Room · Clean Architecture

---

## 0. Tổng quan dự án

| Mục | Giá trị |
|---|---|
| Tên app | **NotePay** |
| Package | `com.notepay` |
| Min SDK | **24** (Android 7.0) |
| Target SDK | **36** |
| Compile SDK | **36** (release, minorApiLevel 1) |
| Ngôn ngữ | Kotlin 100% |
| UI | Jetpack Compose + Material 3 (BOM mới nhất) |
| DB | Room 2.6+ (có migration path sang SQLCipher ở phase 4) |
| Architecture | Clean Architecture (data / domain / ui) + MVVM + UDF |
| DI | Hilt |
| Async | Coroutines + Flow |
| Test | JUnit4, MockK, Turbine, Truth, Compose UI Test, Room in-memory |
| Theme | Dynamic Color (Android 12+) + fallback palette |

### Nguyên tắc bất di bất dịch
1. **Local-first**: 0 byte dữ liệu rời khỏi máy. Không gọi network. Không analytics.
2. **Bảo mật từ pixel đầu tiên**: Từng quyết định kiến trúc phải xem xét ở góc privacy.
3. **Test-driven mindset**: Mỗi UseCase có test trước khi viết UI.
4. **Compose-first**: Không dùng View XML, không dùng Fragment (trừ khi buộc phải).
5. **Material 3 expressive**: Ưu tiên component mới nhất (SearchBar, BottomSheet, TimePicker, DatePicker).

---

## 1. Phân rã Module (Gradle)

```
app/
├── build.gradle.kts
├── src/
│   ├── main/java/com/notepay/
│   │   ├── NotePayApp.kt                    // @HiltAndroidApp
│   │   ├── MainActivity.kt                  // single-Activity
│   │   ├── data/                            // Data layer
│   │   │   ├── local/
│   │   │   │   ├── NotePayDatabase.kt
│   │   │   │   ├── dao/
│   │   │   │   │   ├── TransactionDao.kt
│   │   │   │   │   └── WalletDao.kt
│   │   │   │   ├── entity/
│   │   │   │   │   ├── TransactionEntity.kt
│   │   │   │   │   ├── WalletEntity.kt
│   │   │   │   │   └── CategoryEntity.kt
│   │   │   │   └── converter/
│   │   │   │       └── InstantConverter.kt
│   │   │   ├── repository/
│   │   │   │   ├── TransactionRepositoryImpl.kt
│   │   │   │   └── WalletRepositoryImpl.kt
│   │   │   └── mapper/
│   │   │       ├── TransactionMapper.kt
│   │   │       └── WalletMapper.kt
│   │   ├── domain/                          // Domain layer (pure Kotlin)
│   │   │   ├── model/
│   │   │   │   ├── Transaction.kt
│   │   │   │   ├── Wallet.kt
│   │   │   │   ├── Category.kt
│   │   │   │   └── Money.kt                // value class tránh lỗi số học
│   │   │   ├── repository/
│   │   │   │   ├── TransactionRepository.kt
│   │   │   │   └── WalletRepository.kt
│   │   │   └── usecase/
│   │   │       ├── AddTransactionUseCase.kt
│   │   │       ├── GetTransactionsUseCase.kt
│   │   │       ├── GetMonthlySummaryUseCase.kt
│   │   │       ├── DeleteTransactionUseCase.kt
│   │   │       └── ObserveWalletBalanceUseCase.kt
│   │   ├── ui/                              // UI layer
│   │   │   ├── theme/
│   │   │   │   ├── Color.kt
│   │   │   │   ├── Theme.kt
│   │   │   │   ├── Type.kt
│   │   │   │   └── DynamicColor.kt
│   │   │   ├── navigation/
│   │   │   │   ├── NotePayNavHost.kt
│   │   │   │   └── Route.kt
│   │   │   ├── component/                   // Reusable composables
│   │   │   │   ├── BalanceCard.kt
│   │   │   │   ├── TransactionItem.kt
│   │   │   │   ├── CategoryChip.kt
│   │   │   │   ├── EmptyState.kt
│   │   │   │   └── MoneyText.kt
│   │   │   ├── feature/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   └── HomeUiState.kt
│   │   │   │   ├── addtransaction/
│   │   │   │   │   ├── AddTransactionScreen.kt
│   │   │   │   │   ├── AddTransactionViewModel.kt
│   │   │   │   │   ├── AddTransactionUiState.kt
│   │   │   │   │   └── AddTransactionEvent.kt
│   │   │   │   ├── list/
│   │   │   │   │   ├── TransactionListScreen.kt
│   │   │   │   │   ├── TransactionListViewModel.kt
│   │   │   │   │   └── TransactionListUiState.kt
│   │   │   │   └── stats/
│   │   │   │       ├── StatsScreen.kt
│   │   │   │       ├── StatsViewModel.kt
│   │   │   │       └── StatsUiState.kt
│   │   │   └── util/
│   │   │       ├── MoneyFormatter.kt
│   │   │       └── DateFormatter.kt
│   │   └── di/
│   │       ├── DatabaseModule.kt
│   │       ├── RepositoryModule.kt
│   │       └── DispatcherModule.kt
│   ├── test/java/com/notepay/              // Unit test
│   │   ├── domain/
│   │   │   └── usecase/
│   │   │       ├── AddTransactionUseCaseTest.kt
│   │   │       └── GetMonthlySummaryUseCaseTest.kt
│   │   └── data/
│   │       └── repository/
│   │           └── TransactionRepositoryImplTest.kt
│   └── androidTest/java/com/notepay/       // UI/Integration test
│       ├── dao/
│       │   └── TransactionDaoTest.kt
│       └── feature/
│           ├── home/
│           │   └── HomeScreenTest.kt
│           └── addtransaction/
│               └── AddTransactionScreenTest.kt
```

---

## 2. Tech Stack — Phiên bản cụ thể

### 2.1. Version catalog (`gradle/libs.versions.toml`)

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
hilt = "2.53"
hiltNavigationCompose = "1.2.0"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigationCompose = "2.8.5"
room = "2.6.1"
coroutines = "1.9.0"
junit = "4.13.2"
junitExt = "1.2.1"
espresso = "3.6.1"
mockk = "1.13.13"
turbine = "1.2.0"
truth = "1.4.4"
coroutinesTest = "1.9.0"
robolectric = "4.14"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
truth = { group = "com.google.truth", name = "truth", version.ref = "truth" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

### 2.2. `app/build.gradle.kts` (Phase 1)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.notepay"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.notepay"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "/META-INF/LICENSE*",
            "/META-INF/NOTICE*",
        )
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
```

---

## 3. Domain Layer — Pure Kotlin (không phụ thuộc Android)

### 3.1. `Money.kt` — Value class chống lỗi số học

```kotlin
@JvmInline
value class Money(val amountInCents: Long) {
    operator fun plus(other: Money) = Money(amountInCents + other.amountInCents)
    operator fun minus(other: Money) = Money(amountInCents - other.amountInCents)
    operator fun compareTo(other: Money) = amountInCents.compareTo(other.amountInCents)
    fun isPositive() = amountInCents > 0
    fun isNegative() = amountInCents < 0
    fun abs() = Money(kotlin.math.abs(amountInCents))

    companion object {
        val ZERO = Money(0L)
        fun fromMajorUnit(major: Double): Money = Money((major * 100).toLong())
    }
}
```

**Lý do dùng cents (Long) thay vì Double**: Tránh floating point error khi cộng dồn hàng triệu giao dịch.

### 3.2. `Category.kt` — Enum category

```kotlin
enum class Category(
    val displayName: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector, // mapper layer chuyển
    val color: Long, // ARGB
) {
    FOOD("Ăn uống", 0xFFE57373),
    TRANSPORT("Di chuyển", 0xFF64B5F6),
    SHOPPING("Mua sắm", 0xFFFFB74D),
    BILL("Hóa đơn", 0xFF81C784),
    ENTERTAINMENT("Giải trí", 0xFFBA68C8),
    HEALTH("Sức khỏe", 0xFFF06292),
    EDUCATION("Học tập", 0xFF4DB6AC),
    SALARY("Lương", 0xFF66BB6A),
    GIFT("Quà/Cho", 0xFFFFD54F),
    OTHER("Khác", 0xFF90A4AE);
}
```

> **Lưu ý kiến trúc**: Domain layer KHÔNG được phụ thuộc `androidx.compose.ui.graphics`. Icon sẽ được map tại UI layer thông qua extension function. Trong file spec này tôi ghi tạm để bạn hình dung, code thực tế sẽ tách.

### 3.3. `Transaction.kt`

```kotlin
data class Transaction(
    val id: Long = 0L,
    val amount: Money,
    val type: TransactionType,
    val category: Category,
    val note: String,
    val occurredAt: kotlinx.datetime.Instant,
    val walletId: Long,
    val createdAt: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now(),
) {
    init {
        require(note.length <= 200) { "Note too long" }
        require(amount.amountInCents != 0L) { "Amount must not be zero" }
    }
}

enum class TransactionType { INCOME, EXPENSE }
```

> **Dùng `kotlinx-datetime`** thay vì `java.time` để đồng nhất multiplatform sau này và có `Instant` mạnh hơn.

### 3.4. `Wallet.kt`

```kotlin
data class Wallet(
    val id: Long = 0L,
    val name: String,
    val initialBalance: Money,
    val iconKey: String, // "cash", "bank", "momo", "card" — UI map sang icon
    val colorKey: String, // "primary", "secondary", "tertiary"
    val createdAt: kotlinx.datetime.Instant = kotlinx.datetime.Clock.System.now(),
) {
    init { require(name.isNotBlank()) { "Wallet name must not be blank" } }
}
```

### 3.5. Repository interfaces (Domain)

```kotlin
interface TransactionRepository {
    fun observeAll(): Flow<List<Transaction>>
    fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>>
    suspend fun getById(id: Long): Transaction?
    suspend fun upsert(transaction: Transaction): Long
    suspend fun delete(id: Long)
}

interface WalletRepository {
    fun observeAll(): Flow<List<Wallet>>
    fun observeActive(): Flow<Wallet?>
    suspend fun getById(id: Long): Wallet?
    suspend fun upsert(wallet: Wallet): Long
    suspend fun delete(id: Long)
    suspend fun setActive(id: Long)
}
```

### 3.6. UseCases (mỗi cái 1 file, có test riêng)

```kotlin
class AddTransactionUseCase(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(transaction: Transaction): Result<Long> = runCatching {
        withContext(dispatcher) {
            val wallet = walletRepo.getById(transaction.walletId)
                ?: error("Wallet ${transaction.walletId} not found")
            transactionRepo.upsert(transaction).also {
                // Phase 2+: cập nhật balance ví
            }
        }
    }
}

class GetMonthlySummaryUseCase(
    private val transactionRepo: TransactionRepository,
) {
    data class Summary(
        val totalIncome: Money,
        val totalExpense: Money,
        val balance: Money,
        val byCategory: Map<Category, Money>,
        val transactionCount: Int,
    )

    operator fun invoke(year: Int, month: Int): Flow<Summary> =
        transactionRepo.observeByMonth(year, month).map { txs ->
            val income = txs.filter { it.type == TransactionType.INCOME }
                .fold(Money.ZERO) { acc, t -> acc + t.amount }
            val expense = txs.filter { it.type == TransactionType.EXPENSE }
                .fold(Money.ZERO) { acc, t -> acc + t.amount }
            val byCategory = txs.filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { (_, list) -> list.fold(Money.ZERO) { acc, t -> acc + t.amount } }
            Summary(income, expense, income - expense, byCategory, txs.size)
        }
}
```

---

## 4. Data Layer

### 4.1. Entities & DAOs

**`TransactionEntity.kt`**

```kotlin
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = WalletEntity::class,
            parentColumns = ["id"],
            childColumns = ["wallet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("wallet_id"),
        Index("occurred_at"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "type") val type: String, // "INCOME" | "EXPENSE"
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long, // epoch millis
    @ColumnInfo(name = "wallet_id") val walletId: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

**`TransactionDao.kt`**

```kotlin
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurred_at DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE occurred_at BETWEEN :startMillis AND :endMillis
        ORDER BY occurred_at DESC
    """)
    fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Upsert
    suspend fun upsert(entity: TransactionEntity): Long

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: Long)
}
```

**`WalletEntity.kt`** — tương tự, có thêm cột `is_active` (chỉ 1 ví active nhờ UNIQUE index).

```kotlin
@Entity(
    tableName = "wallets",
    indices = [Index(value = ["is_active"], unique = true)],
)
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "initial_balance_cents") val initialBalanceCents: Long,
    @ColumnInfo(name = "icon_key") val iconKey: String,
    @ColumnInfo(name = "color_key") val colorKey: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
```

### 4.2. Database

```kotlin
@Database(
    entities = [TransactionEntity::class, WalletEntity::class],
    version = 1,
    exportSchema = true, // QUAN TRỌNG: bật để test migration
)
abstract class NotePayDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
}
```

`build.gradle.kts` phải có:
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

### 4.3. Repository Implementation

```kotlin
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        dao.observeAll().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val (start, end) = monthRange(year, month)
        return dao.observeByRange(start, end)
            .map { list -> list.map(mapper::toDomain) }
            .flowOn(dispatcher)
    }

    // ... các method khác tương tự
}
```

### 4.4. Seed data

Khi user mở app lần đầu, tự động:
1. Insert 1 ví mặc định ("Tiền mặt", icon `cash`, color `primary`, active = true)
2. Insert 12 Category nếu dùng bảng riêng (Phase 1 dùng enum nên không cần)
3. Có thể insert 1-2 transaction mẫu cho "Aha-moment" (tùy chọn, có flag trong Settings)

---

## 5. UI Layer — Material 3 + Compose

### 5.1. Theme

**`Color.kt`**

```kotlin
val LightColors = lightColorScheme(
    primary = Color(0xFF1B7F4F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB6F2CE),
    onPrimaryContainer = Color(0xFF002113),
    secondary = Color(0xFF4F6353),
    background = Color(0xFFFBFDF7),
    surface = Color(0xFFFBFDF7),
    surfaceVariant = Color(0xFFDEE5DC),
    error = Color(0xFFBA1A1A),
    // ... Material 3 token đầy đủ
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF9BD6B0),
    onPrimary = Color(0xFF003920),
    // ...
)
```

**`Theme.kt`**

```kotlin
@Composable
fun NotePayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NotePayTypography,
        shapes = NotePayShapes,
        content = content,
    )
}
```

### 5.2. Design tokens

| Token | Giá trị | Lý do |
|---|---|---|
| Corner radius (card) | 20.dp | Material 3 expressive — bo tròn mềm |
| Corner radius (button) | 12.dp | |
| Spacing unit | 4.dp grid | 4, 8, 12, 16, 24, 32 |
| Elevation card | 1.dp | Tone hơn shadow |
| Ripple | bounded | M3 default |
| Motion duration | 300ms enter, 200ms exit | |

**`Type.kt`** — dùng Material 3 type scale:
- `displayLarge` cho số dư lớn trên Home (36sp)
- `headlineMedium` cho tiêu đề section
- `bodyLarge` cho transaction amount
- `labelMedium` cho category chip
- `bodySmall` cho timestamp

### 5.3. Navigation

```kotlin
sealed interface Route {
    val path: String

    data object Home : Route { override val path = "home" }
    data object AddTransaction : Route { override val path = "add-transaction" }
    data object TransactionList : Route { override val path = "list" }
    data object Stats : Route { override val path = "stats" }
    data class EditTransaction(val id: Long) : Route { override val path = "edit/{id}" }
}
```

Single-Activity. `MainActivity` chứa `NotePayNavHost`. Bottom navigation có 3 tab: **Trang chủ · Danh sách · Thống kê**. Nút FAB ở Home → AddTransaction.

### 5.4. Feature: Home

**`HomeUiState.kt`**

```kotlin
data class HomeUiState(
    val activeWallet: Wallet? = null,
    val currentBalance: Money = Money.ZERO,
    val monthlyIncome: Money = Money.ZERO,
    val monthlyExpense: Money = Money.ZERO,
    val recentTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val monthLabel: String = "",
)
```

**`HomeViewModel.kt`**

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val walletRepo: WalletRepository,
    private val getMonthlySummary: GetMonthlySummaryUseCase,
    private val observeWalletBalance: ObserveWalletBalanceUseCase,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.from(clock.now()))
    val state: StateFlow<HomeUiState> = combine(
        walletRepo.observeActive(),
        _selectedMonth.flatMapLatest { ym -> getMonthlySummary(ym.year, ym.month) },
    ) { wallet, summary ->
        HomeUiState(
            activeWallet = wallet,
            currentBalance = wallet?.let { observeWalletBalance(it.id) }?.first() ?: Money.ZERO,
            monthlyIncome = summary.totalIncome,
            monthlyExpense = summary.totalExpense,
            recentTransactions = summary.transactions.take(5),
            isLoading = false,
            monthLabel = ym.toDisplayLabel(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun onMonthChange(ym: YearMonth) { _selectedMonth.value = ym }
}
```

> **Lưu ý**: `observeWalletBalance` phát `Flow<Money>` từ việc tính sum transactions. Trong Phase 1 có thể đơn giản: `wallet.initialBalance + sum(transactions.amount)`.

**`HomeScreen.kt`** — Compose UI:
- `Scaffold` với `TopAppBar` (title: "Trang chủ", action: chọn tháng)
- `BalanceCard` ở top (gradient brush, số dư lớn, sub: tháng này +/–)
- Row 2 card nhỏ: Thu nhập / Chi tiêu trong tháng
- `LazyColumn` 5 transaction gần nhất
- `ExtendedFloatingActionButton` "Thêm giao dịch" (icon `Icons.Rounded.Add`)
- Snackbar host (dùng cho error)

**Material 3 components ưu tiên dùng**:
- `Card` (filled, với `onClick` parameter M3)
- `FilterChip` cho category filter
- `ListItem` cho transaction
- `TopAppBar` với `TopAppBarDefaults.enterAlwaysScrollBehavior`
- `ModalBottomSheet` cho quick-add

### 5.5. Feature: AddTransaction

**`AddTransactionUiState.kt`**

```kotlin
data class AddTransactionUiState(
    val amountInput: String = "", // raw text để format realtime
    val amount: Money? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: Category = Category.FOOD,
    val note: String = "",
    val occurredAt: Instant = Clock.System.now(),
    val walletId: Long? = null,
    val isSaving: Boolean = false,
    val errors: Set<FieldError> = emptySet(),
    val savedSuccessfully: Boolean = false,
)

enum class FieldError { AMOUNT_EMPTY, AMOUNT_INVALID, NOTE_TOO_LONG }
```

**`AddTransactionViewModel.kt`** — UDF pattern:

```kotlin
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val addTransaction: AddTransactionUseCase,
    private val walletRepo: WalletRepository,
    private val clock: Clock = Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionUiState())
    val state: StateFlow<AddTransactionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            walletRepo.observeActive().first()?.let { wallet ->
                _state.update { it.copy(walletId = wallet.id) }
            }
        }
    }

    fun onEvent(event: AddTransactionEvent) { /* handle */ }
}

sealed interface AddTransactionEvent {
    data class AmountChanged(val text: String) : AddTransactionEvent
    data class TypeChanged(val type: TransactionType) : AddTransactionEvent
    data class CategoryChanged(val category: Category) : AddTransactionEvent
    data class NoteChanged(val note: String) : AddTransactionEvent
    data class DateChanged(val instant: Instant) : AddTransactionEvent
    data object Save : AddTransactionEvent
}
```

**`AddTransactionScreen.kt`** — UX flow:
1. **Segmented button** trên cùng: [Chi tiêu | Thu nhập]
2. **Big number input** — Tap vào hiện `NumberPadDialog` (custom keypad, locale VN, dấu phân cách `.`)
3. **Grid category** 3 cột — chip chọn nhanh (Material 3 `FilterChip` chứa icon + label)
4. **Note** — `OutlinedTextField` (single line, maxLength = 200, hiện counter)
5. **Date** — `DatePicker` M3
6. **Wallet** — chip hiện tên ví active, tap để đổi (nếu có > 1 ví — Phase 1 mặc định disabled vì chỉ 1 ví)
7. **Nút Lưu** — sticky bottom, enable khi amount hợp lệ, loading state khi saving

**Validation rules**:
- Amount: > 0, parse được từ text VN locale (vd: "1.000.000" → 1000000)
- Note: ≤ 200 ký tự
- Wallet: phải tồn tại

**NumberPad UX** (rất quan trọng cho Aha-moment):
- Long-press nút backspace = xóa hết
- Swipe down trên amount = đóng keypad
- Locale-aware: dùng `NumberFormat.getNumberInstance(Locale("vi", "VN"))`

### 5.6. Feature: TransactionList

- `Scaffold` với `SearchBar` M3 (top)
- Bộ lọc: tháng, category (chip row)
- `PullToRefreshBox` M3 (Phase 1.5)
- `LazyColumn` với `TransactionItem`
- Swipe to delete (`SwipeToDismissBox` M3) → Snackbar "Đã xóa" với Undo
- Empty state: illustration + text "Chưa có giao dịch nào"

**`TransactionItem.kt`**:
- Leading: icon category trong circle màu
- Title: note (fallback category name)
- Subtitle: tên ví · thời gian (relative: "5 phút trước")
- Trailing: amount với dấu `+`/`–` và màu income/expense
- `ListItem` M3 với `onClick` → EditTransaction

### 5.7. Feature: Stats (Phase 1 — biểu đồ cơ bản)

- Top: 3 KPI (Tổng thu, Tổng chi, Tiết kiệm được = income - expense)
- Middle: **Donut chart** theo category (dùng Compose Canvas — không cần thư viện)
- Bottom: list xếp hạng category (top 5)
- Thư viện: dùng **Vico Chart** (`com.patrykandpatrick.vico:compose-m3:2.0.0+`) — Compose-native, Material 3 theming, không cần View
- Hoặc tự vẽ Canvas (gọn, không thêm dep)

> **Quyết định Phase 1**: Dùng Canvas tự vẽ donut + bar chart. Phase 5 mới add Vico nếu cần chart phức tạp hơn.

### 5.8. Edge cases & a11y

- `Locale.getDefault()` để format tiền (`NumberFormat.getCurrencyInstance(Locale("vi","VN"))` cho VND)
- `contentDescription` cho mọi icon-button
- `semantics { contentDescription = ... }` cho chart
- TalkBack: đọc "Chi 50 nghìn cho Ăn uống, Ví tiền mặt, 5 phút trước"
- Dynamic font scale: test với font scale 1.3x
- Dark mode: tất cả screen test cả 2 mode
- Color blind: dùng cả icon + màu, không dựa mỗi màu

---

## 6. DI (Hilt)

### 6.1. `NotePayApp.kt`

```kotlin
@HiltAndroidApp
class NotePayApp : Application()
```

### 6.2. `DatabaseModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotePayDatabase =
        Room.databaseBuilder(context, NotePayDatabase::class.java, "notepay.db")
            .addCallback(SeedCallback())
            .build()

    @Provides fun provideTransactionDao(db: NotePayDatabase) = db.transactionDao()
    @Provides fun provideWalletDao(db: NotePayDatabase) = db.walletDao()
}
```

### 6.3. `RepositoryModule.kt`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindTransactionRepo(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds @Singleton
    abstract fun bindWalletRepo(impl: WalletRepositoryImpl): WalletRepository
}
```

### 6.4. `DispatcherModule.kt`

```kotlin
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher fun provideIo() = Dispatchers.IO
    @Provides fun provideDefault() = Dispatchers.Default
}
```

---

## 7. Testing Strategy

### 7.1. Quy tắc vàng

> **Mỗi UseCase phải có test trước khi viết UI dùng nó.** Mỗi ViewModel phải có test. DAO test bằng Room in-memory.

### 7.2. UseCase test template

```kotlin
class AddTransactionUseCaseTest {
    private val transactionRepo = mockk<TransactionRepository>(relaxed = true)
    private val walletRepo = mockk<WalletRepository>(relaxed = true)
    private val dispatcher = StandardTestDispatcher()
    private val useCase = AddTransactionUseCase(transactionRepo, walletRepo, dispatcher)

    @Test
    fun `invoke with valid transaction returns success`() = runTest(dispatcher) {
        val wallet = sampleWallet()
        coEvery { walletRepo.getById(wallet.id) } returns wallet
        coEvery { transactionRepo.upsert(any()) } returns 1L

        val result = useCase(sampleTransaction(walletId = wallet.id))

        assertThat(result.isSuccess).isTrue()
        coVerify(exactly = 1) { transactionRepo.upsert(any()) }
    }

    @Test
    fun `invoke with non-existent wallet returns failure`() = runTest(dispatcher) {
        coEvery { walletRepo.getById(any()) } returns null

        val result = useCase(sampleTransaction(walletId = 99L))

        assertThat(result.isFailure).isTrue()
        coVerify(exactly = 0) { transactionRepo.upsert(any()) }
    }

    @Test
    fun `invoke with zero amount throws at construction`() {
        assertFailsWith<IllegalArgumentException> {
            Transaction(
                amount = Money.ZERO,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                note = "x",
                occurredAt = Clock.System.now(),
                walletId = 1L,
            )
        }
    }
}
```

### 7.3. DAO test (instrumented)

```kotlin
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var db: NotePayDatabase
    private lateinit var dao: TransactionDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            NotePayDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.transactionDao()
    }

    @After
    fun closeDb() { db.close() }

    @Test
    fun upsertAndGetById() = runTest {
        val entity = sampleEntity()
        val id = dao.upsert(entity)
        val loaded = dao.getById(id)
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.amountCents).isEqualTo(entity.amountCents)
    }

    @Test
    fun observeByRange_filtersCorrectly() = runTest {
        val jan = sampleEntity(occurredAt = 1_700_000_000_000L)
        val feb = sampleEntity(occurredAt = 1_704_000_000_000L)
        dao.upsert(jan); dao.upsert(feb)

        dao.observeByRange(1_699_900_000_000L, 1_700_100_000_000L).test {
            val list = awaitItem()
            assertThat(list).hasSize(1)
            assertThat(list[0].id).isEqualTo(jan.id)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 7.4. ViewModel test

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val walletRepo = mockk<WalletRepository>()
    private val getMonthlySummary = mockk<GetMonthlySummaryUseCase>()
    private val observeBalance = mockk<ObserveWalletBalanceUseCase>()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `state emits loading then loaded`() = runTest(testDispatcher) {
        val wallet = sampleWallet()
        val summary = sampleSummary()
        every { walletRepo.observeActive() } returns flowOf(wallet)
        every { getMonthlySummary(any(), any()) } returns flowOf(summary)
        every { observeBalance(any()) } returns flowOf(Money(50_000_00))

        val vm = HomeViewModel(walletRepo, getMonthlySummary, observeBalance)
        vm.state.test {
            assertThat(awaitItem().isLoading).isTrue()
            val loaded = awaitItem()
            assertThat(loaded.isLoading).isFalse()
            assertThat(loaded.activeWallet).isEqualTo(wallet)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 7.5. UI test (Compose)

```kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AddTransactionScreenTest {
    @get:Rule val hiltRule = HiltAndroidRule(this)
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun saveButton_disabledWhenAmountEmpty() {
        composeRule.setContent {
            NotePayTheme { AddTransactionScreen(...) }
        }
        composeRule.onNodeWithText("Lưu").assertIsNotEnabled()
    }

    @Test
    fun saveButton_enabledAfterValidAmount() {
        // ...
    }
}
```

### 7.6. Coverage target

| Layer | Target |
|---|---|
| Domain (UseCase + model) | ≥ 90% |
| Data (DAO + Repository) | ≥ 80% |
| ViewModel | ≥ 70% |
| UI Compose | ≥ 50% (test smoke + key flow) |

---

## 8. Bảo mật & Privacy (ưu tiên #1)

### 8.1. Thiết kế bảo mật
| Rủi ro | Biện pháp |
|---|---|
| Data leak qua backup | `android:allowBackup="false"` + `dataExtractionRules` từ chối |
| Data leak qua root/adb | Phase 4 dùng SQLCipher + Android Keystore quản lý key |
| Snooping qua screenshot | `FLAG_SECURE` trên màn hình nhạy cảm (AddTransaction) |
| Logging lộ dữ liệu | Tự viết `Timber.DebugTree` strip data, release không log |
| Clipboard lộ số tiền | Khi copy, confirm dialog "Bạn có chắc copy số tiền?" |
| Permission creep | Không xin bất kỳ permission nào ở Phase 1. Permission ở Phase 2 (POST_NOTIFICATIONS) |

### 8.2. `AndroidManifest.xml` Phase 1

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:name=".NotePayApp"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.NotePay"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:usesCleartextTraffic="false">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.NotePay">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`res/xml/backup_rules.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <exclude domain="database" path="notepay.db" />
    <exclude domain="sharedpref" path="." />
</full-backup-content>
```

---

## 9. Roadmap các phase (để AI biết giới hạn Phase 1)

| Phase | Tính năng | Permissions mới | Thư viện thêm |
|---|---|---|---|
| **1 (hiện tại)** | CRUD giao dịch, 1 ví, thống kê cơ bản | Không | Room, Hilt, Coroutines, Compose, Material 3 |
| 2 | Notification parse tự động | POST_NOTIFICATIONS, BIND_NOTIFICATION_LISTENER | Regex engine, WorkManager (queue pending) |
| 3 | OCR hóa đơn | CAMERA | Google ML Kit Text Recognition (on-device) |
| 4 | Đa ví, Budget cap, SQLCipher | — | SQLCipher, Android Keystore |
| 5 | Xuất Excel/PDF | Storage (scoped) | Apache POI, iText hoặc OpenCSV + Print |

> **Spec này CHỈ dành cho Phase 1.** AI không tự ý thêm dependency Phase 2+.

---

## 10. Definition of Done — Phase 1

- [ ] App build được, install lên emulator API 24 và API 34 không crash
- [ ] Có thể thêm giao dịch mới từ FAB trên Home
- [ ] Giao dịch hiển thị trong Home + List sau khi save
- [ ] Swipe-to-delete hoạt động, có Undo
- [ ] Stats screen hiển thị donut chart + top categories cho tháng hiện tại
- [ ] Dark mode + Dynamic color hoạt động
- [ ] Locale VN format tiền (`1.000.000 ₫`)
- [ ] **Unit test pass ≥ 80% coverage domain layer**
- [ ] **Instrumented test pass cho DAO + key UI flow**
- [ ] Không có permission nào bị xin ngoài Phase 1
- [ ] `allowBackup="false"` đã bật
- [ ] Không có hardcoded secret, không có network code
- [ ] ProGuard/R8 rules đã viết cho Room + Hilt

---

## 11. Gợi ý prompt để gửi AI code

Khi bạn copy spec này cho AI, hãy mở đầu bằng:

```
Hãy đọc kỹ file SPEC.md. Tôi muốn bạn implement Phase 1 của app NotePay.

Yêu cầu:
1. Tuân thủ 100% kiến trúc Clean Architecture đã mô tả.
2. Viết test song song với code (mỗi UseCase có test).
3. KHÔNG thêm dependency ngoài Phase 1.
4. Mỗi file Kotlin phải có header comment ngắn giải thích mục đích.
5. Ưu tiên Material 3 component mới nhất.
6. Báo cáo sau mỗi phase nhỏ: file đã tạo, test pass/fail, coverage.
7. KHÔNG dùng println trong code production — dùng Timber hoặc Log nhưng wrap qua interface Logger.

Bắt đầu bằng: tạo version catalog (libs.versions.toml), app/build.gradle.kts,
sau đó Domain layer trước (vì không phụ thuộc Android), rồi test, rồi Data, rồi UI.
```

---

## 12. Anti-pattern cần tránh

| ❌ Đừng | ✅ Làm thế này |
|---|---|
| `Double` cho tiền | `Money` (Long cents) |
| `String` cho date | `kotlinx.datetime.Instant` |
| Truy vấn DB trên Main thread | `flowOn(IoDispatcher)` |
| ViewModel biết về Compose | ViewModel phát `StateFlow`, UI collect |
| Hardcode string | `R.string.*` |
| 1 file 1000 dòng | 1 class = 1 file, package theo feature |
| Singleton object global | Hilt `@Singleton` |
| `runBlocking` trong ViewModel | `viewModelScope.launch` |
| `mutableStateOf` ở ViewModel | `MutableStateFlow` |
| `when` không có else | Compiler sẽ warn — bổ sung `else -> error("...")` |
| `as? Type ?: defaultValue` khi sai type là lỗi logic | Để nó crash, fix bug |

---

**Tài liệu này dài nhưng cố tình.** Phase 1 của NotePay tuy đơn giản về tính năng nhưng là nền tảng cho 4 phase sau. Làm đúng từ đầu = 4 phase sau chỉ là "thêm tính năng", không phải "viết lại".
