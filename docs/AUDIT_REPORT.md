# BÁO CÁO KIỂM TOÁN TĨNH TOÀN DIỆN MÃ NGUỒN VÀ KIẾN TRÚC GIAO DIỆN NOTEPAY
**Comprehensive Static Audit & Architectural Assessment Report**

- **Dự án**: NotePay Android Application (`app`)
- **Phiên bản thiết kế mục tiêu**: Luminous iOS & Atmospheric Canvas v5.0.0 (`DESIGN.md`)
- **Nền tảng công nghệ**: Kotlin 2.1.0, Jetpack Compose (BOM 2026.09.00), Material 3, Clean Architecture Local-First, Room DB 2.6.1, Hilt DI.
- **Ngày lập báo cáo**: 22/11/2026
- **Trạng thái thực thi**: Read-Only Audit (Bảo toàn tuyệt đối 100% mã nguồn và mã kiểm thử, không có bất kỳ commit/file code nào bị sửa đổi).
- **Branch**: `test-bug/v1` (HEAD: `2f9116e`)

---

## 1. TỔNG QUAN ĐIỀU HÀNH (EXECUTIVE SUMMARY)

### 1.1 Mục tiêu và Phạm vi Rà soát
Đợt kiểm toán tĩnh toàn diện được thực hiện nhằm rà soát, đánh giá chuyên sâu toàn bộ mã nguồn của ứng dụng NotePay đối chiếu với các tiêu chuẩn công nghiệp:
- **Chuẩn mã nguồn Kotlin & Android Compose**: Nguyên tắc kiến trúc Google Now in Android, Jetpack Compose Performance Guidelines, Quản lý tài nguyên Coroutine/Flow, Null-safety và Clean Architecture UDF (Unidirectional Data Flow).
- **Hệ thống Thiết kế Luminous iOS & Atmospheric Canvas v5.0.0**: 3 trụ cột thiết kế (Thẻ Elevated Hero hai lớp thay thế thẻ đen `ink`, Đổ bóng mềm 2 lớp `iosCardShadow`, Nền Atmospheric toàn màn hình kèm Scrim bảo vệ tương phản); Thang chữ Inter (`tnum`) và Fraunces (`displaySerif`); Thang bo góc chuẩn 4dp; Padding đáy tối thiểu 120dp chống che thanh điều hướng nổi.
- **Quy chuẩn Trải nghiệm UI/UX & Khả năng tiếp cận (Accessibility)**: Cổng kiểm tra 8 điểm theo `ui-ux-audit-guide.md`; Khả năng co giãn chữ an toàn (Font scaling từ 1.3x đến 2.0x); Vùng chạm tối thiểu $\ge 48\text{dp}$; Nhãn ngữ nghĩa TalkBack (`contentDescription`, `semantics`); 4 trạng thái dữ liệu (Loading, Empty, Error, Content).

**Quy mô mã nguồn được rà soát**:
- **Tổng số tệp Kotlin (.kt)**: 419 tệp (339 tệp mã nguồn production, 80 tệp kiểm thử unit/instrumentation).
- **Dòng mã nguồn (LOC)**: 74,979 dòng (66,592 source lines of code, 48,859 logical lines).
- **Phân hệ tính năng (`com.notepay.feature.*`)**: 13 tính năng (`home`, `stats`, `assets`, `transaction`, `utilities`, `debt`, `subscription`, `billsplit`, `category`, `auth`, `backup`, `onboarding`, `settings`) với 131 tệp.
- **Phân hệ cốt lõi (`com.notepay.core.*`)**: 4 packages nền tảng (`common`, `designsystem`, `navigation`, `platform`) với 58 tệp.
- **Tầng dữ liệu & nghiệp vụ (`domain`, `data`, `ai`, `worker`, `di`)**: 135 tệp.
- **Product Flavors (`full`, `play`)**: 15 tệp.

### 1.2 Bảng Phân Bố Phát Hiện Theo Mức Độ Nghiêm Trọng

| Danh mục Kiểm toán | P0 (Critical) | P1 (High) | P2 (Medium) | P3 (Low) | Tổng cộng |
|---|:---:|:---:|:---:|:---:|:---:|
| 1. Lỗi phân tích tĩnh (Static Tools & Lint) | 0 | 2 | 19 | 374 | **395** |
| 2. Hiệu năng Compose & Recomposition | 0 | 11 | 2 | 0 | **13** |
| 3. Quản lý Tài nguyên, Coroutine & Flow | 4 | 3 | 0 | 0 | **7** |
| 4. Kiến trúc Clean Architecture & State | 0 | 1 | 3 | 0 | **4** |
| 5. Tràn viền & Co giãn chữ (Font Scaling) | 0 | 4 | 2 | 0 | **6** |
| 6. Màu sắc, Dark Mode & Design System v5 | 0 | 0 | 5 | 0 | **5** |
| 7. Khả năng tiếp cận (Accessibility / a11y) | 0 | 2 | 3 | 0 | **5** |
| 8. Xử lý Trạng thái UI (Loading/Empty/Error) | 0 | 1 | 2 | 0 | **3** |
| 9. Chuẩn hóa Material 3 & Navigation | 0 | 0 | 3 | 0 | **3** |
| **TỔNG CỘNG CÁC VẤN ĐỀ ĐƯỢC ĐỊNH DANH** | **4** | **24** | **39** | **374** | **441** |

*Định nghĩa mức độ ưu tiên (Severity Levels)*:
- **P0 (Critical)**: Lỗi trực tiếp phá vỡ cơ chế hủy coroutine (`CancellationException`) hoặc gây sập ứng dụng (Runtime Crash), dẫn tới rò rỉ tác vụ nền, không thể dừng tiến trình không đồng bộ khi hủy màn hình hoặc phát sinh lỗi ảo gây hoang mang cho người dùng.
- **P1 (High)**: Lỗi gây tụt khung hình (Jank/Drop frames < 60fps) do cấp phát bộ nhớ liên tục trong Canvas draw loop; recomposition lan tỏa do thiếu tính ổn định (`@Immutable`); cắt mất nội dung chữ khi người dùng phóng to phông chữ hệ thống ($\ge 1.3\text{x}$); TalkBack bị câm trên nút chức năng (`contentDescription = null`); nháy màn hình trống (Flash of Empty State); rủi ro format chuỗi đa ngôn ngữ do lệch số lượng tham số format giữa các locale (`StringFormatCount` tại `home_day_other`).
- **P2 (Medium)**: Vi phạm chuẩn Compose API guidelines (`ModifierParameter`), dùng API đo kích thước màn hình cũ (`Configuration.screenWidthDp`), hardcode mã màu Hex phá vỡ Dark Mode, Dialog dạng cũ chưa chuyển thành `ModalBottomSheet`, artifact IDE sót trong code, thành phần Tier 2 chưa chuyển đổi sang Design System v5, khuyết thiếu tham số ngày tháng trong bản dịch tiếng Việt (`billsplit_paid_note_format`).
- **P3 (Low)**: Tài nguyên XML không sử dụng (`UnusedResources`), chưa chuyển đổi `<plurals>`, khuyến nghị sử dụng hàm mở rộng KTX (`UseKtx`), cập nhật thư viện Gradle.

### 1.3 Đánh Giá Hiện Trạng Chung
1. **Điểm mạnh cốt lõi**:
   - Ứng dụng tuân thủ nghiêm ngặt mô hình ngoại tuyến trước (Local-First): 100% nghiệp vụ tài chính được lưu trữ bền vững tại Room SQLite Database với migration script đầy đủ (`MIGRATION_11_12`).
   - Cấu trúc thư mục được module hóa theo tính năng (`feature/`) và phân hệ dùng chung (`core/designsystem`, `core/navigation`), loại bỏ hoàn toàn thư mục `ui/` monolithic cũ.
   - Đã bước đầu áp dụng thành công Design System v5.0.0 (Luminous iOS & Atmospheric Canvas) trên Tier 1 (Home, Stats, Assets, TransactionForm, Utilities).
2. **Các rủi ro kỹ thuật chính**:
   - **Bất đồng bộ format chuỗi đa ngôn ngữ**: Bản dịch tiếng Việt và tiếng Anh có độ lệch cấu trúc tham số format ở khóa tài nguyên `home_day_other` (2 vs 3 tham số, tiềm ẩn lỗi nếu call site bị thay đổi) và khuyết thiếu ngày tháng thanh toán ở `billsplit_paid_note_format`.
   - **Nuốt CancellationException**: Các ViewModel (`BillSplitViewModel`, `SubscriptionViewModel`, `DebtDetailViewModel`) và tầng Data (`DataExporter`) bắt ngoại lệ dạng `catch (e: Exception)` mà không re-throw `CancellationException`, phá vỡ cơ chế hủy của Coroutines khi người dùng rời màn hình hoặc hủy tác vụ xuất dữ liệu.
   - **Hiệu năng Render**: Các hiệu ứng động nền (`FirefliesBackground`) và biểu đồ tròn (`StatsCharts`) đang cấp phát đối tượng mới (`Brush`, `Stroke`, `Size`, `Offset`) trực tiếp trong Draw/Canvas scope ở tần số 60-120Hz.
   - **Độ ổn định Recomposition**: Nhiều UI State model chứa kiểu danh sách (`List<T>`, `Set<T>`, `Map<K, V>`) nhưng chưa được đánh dấu `@Immutable`, khiến Compose compiler coi là Unstable và buộc phải recompose toàn bộ cây giao diện con mỗi khi trạng thái cha thay đổi.
   - **Khả năng tiếp cận & Phóng to chữ**: Nhiều container và nút bấm đang bị khóa cứng chiều cao cố định (`height(48.dp)`, `height(68.dp)`), gây tràn viền và mất nét chữ khi hệ thống kích hoạt cỡ chữ lớn ($\ge 1.3\text{x}$).

---

## 2. BÁO CÁO CÔNG CỤ PHÂN TÍCH TĨNH (STATIC ANALYSIS TOOLS)

### 2.1 Kết Quả Phân Tích Detekt
Lệnh thực thi:
```powershell
.\gradlew.bat detekt --no-daemon
```
- **Kết quả**: `BUILD SUCCESSFUL` (0 issues).
- **Phân tích kỹ thuật**:
  - Detekt quét qua toàn bộ 419 tệp Kotlin, kiểm tra 111 packages, 750 classes, 1,926 functions, 5,498 properties với tổng độ phức tạp Cyclomatic là 6,693 và Cognitive là 7,622.
  - Dự án không phát sinh lỗi Detekt do tệp cấu hình `config/detekt/detekt.yml` đã bật cờ `ignoreAnnotated: ['Composable', 'Preview']` cho hàng loạt quy tắc cấu trúc: `LongMethod`, `LargeClass`, `TooManyFunctions`, `LongParameterList`, `CyclomaticComplexMethod`, `FunctionNaming`.
  - Đồng thời, một số quy tắc chất lượng code khắt khe đang ở trạng thái tắt (`active: false`): `MagicNumber`, `WildcardImport`, `MaxLineLength`, `TooGenericExceptionCaught`, `SwallowedException`.
  - *Kết luận*: Kết quả "0 lỗi" của Detekt chỉ phản ánh sự nới lỏng của bộ quy tắc cấu hình đối với mã nguồn Jetpack Compose. Các vấn đề chuyên sâu về rò rỉ bộ nhớ, nuốt exception, và hiệu năng recomposition đòi hỏi phải được kiểm toán thủ công và phân tích thông qua Android Lint.

### 2.2 Kết Quả Phân Tích Android Lint
Lệnh thực thi:
```powershell
.\gradlew.bat :app:lintFullDebug --no-daemon
```
- **Kết quả tổng thể**: `0 errors, 395 warnings, 2 hints`.
- **Bảng tổng hợp chi tiết theo mã định danh Issue Lint**:

| Mã Issue Lint | Mức độ | Số lượng | Vấn đề kỹ thuật | Rủi ro thực tế |
|---|:---:|:---:|---|---|
| `StringFormatCount` | Warning | 4 | Bất đồng bộ số lượng tham số format giữa `strings.xml` và `values-en/strings.xml` | **High / Medium**: Rủi ro lệch cấu trúc tham số format giữa các ngôn ngữ (`home_day_other` - P1); khuyết ngày trả nợ trong tiếng Việt (`billsplit_paid_note_format` - P2) |
| `ModifierParameter` | Warning | 12 | Tham số `modifier` không phải là tham số tùy chọn đầu tiên trong hàm Composable | **Medium**: Vi phạm Jetpack Compose API Guidelines, gây khó khăn khi gọi từ bên ngoài |
| `ConfigurationScreenWidthHeight` | Warning | 3 | Dùng `Configuration.screenWidthDp` thay vì `containerSize` | **Medium**: Không tính insets thanh điều hướng, kích thước bị làm tròn gây sai lệch bố cục |
| `AutoboxingStateCreation` | Hint | 2 | Dùng `mutableStateOf` với kiểu nguyên thủy (`Long`, `Color`) | **Medium**: Gây boxing bộ nhớ liên tục trong state mutation |
| `UnusedResources` | Warning | 273 | Chuỗi, màu sắc mẫu, drawable không còn được tham chiếu | **Low**: Tăng kích thước APK không cần thiết, rác mã nguồn |
| `PluralsCandidate` | Warning | 52 | Ghép chuỗi số lượng bằng `%d` thay vì dùng `<plurals>` | **Low**: Sai lệch ngữ pháp số ít/số nhiều ở tiếng Anh |
| `GradleDependency` | Warning | 19 | Thư viện trong `libs.versions.toml` có bản cập nhật mới trên Google Maven | **Low**: Bỏ lỡ các bản vá lỗi và tối ưu hiệu năng |
| `NewerVersionAvailable` | Warning | 10 | Dependency trên Maven Central có phiên bản mới hơn | **Low**: Nợ kỹ thuật quản lý phiên bản |
| `UseKtx` | Warning | 7 | Dùng method Java/Android cũ thay vì extension Kotlin KTX | **Low**: Code dài dòng, thiếu an toàn kiểu |
| `UnusedAttribute` | Warning | 6 | Thuộc tính XML Widget chỉ hỗ trợ API 31+ trong khi `minSdkVersion = 26` | **Low**: Không ảnh hưởng crash, thuộc tính bị bỏ qua ở API cũ |
| `RelativeOverlap` | Warning | 2 | Khả năng đè chữ khi phóng to chữ trong layout widget XML | **Medium**: Mất chữ ở widget màn hình chính khi fontScale tăng |
| `IconXmlAndPng` | Warning | 2 | Icon xuất hiện đồng thời dưới dạng XML Vector và Bitmap WebP | **Low**: Trùng lặp tài nguyên |
| `TypographyDashes` | Warning | 2 | Dùng dấu gạch nối thường `-` thay vì en dash `–` | **Low**: Lỗi chuẩn typographic |
| `AndroidGradlePluginVersion` | Warning | 1 | AGP 9.3.3 đã có bản cập nhật mới hơn | **Low**: Nợ kỹ thuật build tool |
| `HardcodedText` | Warning | 1 | Hardcode chuỗi `"0 ₫"` trong layout XML widget | **Low**: Không bản địa hóa đơn vị tiền tệ |
| `IconLocation` | Warning | 1 | File bitmap `logo_vietqr.png` nằm trong thư mục vô định mật độ (`res/drawable/`) | **Low**: Ảnh hưởng chất lượng scale bitmap |

---

## 3. CHI TIẾT CÁC PHÁT HIỆN THEO 9 DANH MỤC TRỌNG YẾU

---

### DANH MỤC 1: HIỆU NĂNG JETPACK COMPOSE & RECOMPOSITION

#### [PERF-01] Cấp phát đối tượng liên tục trong Canvas Draw Loop gây áp lực Garbage Collection
- **Vị trí**:
  1. `app/src/main/java/com/notepay/core/designsystem/component/effect/FirefliesBackground.kt:30-44`
  2. `app/src/main/java/com/notepay/feature/stats/main/component/StatsCharts.kt:188, 205, 208, 214, 222, 229, 230`
  3. `app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt:327`
- **Mô tả vấn đề**:
  - Tại `FirefliesBackground.kt`, bên trong hàm `Canvas(modifier) { repeat(12) { index -> ... } }`, các đối tượng `Brush.radialGradient`, `listOf(...)`, và `Offset(x, y)` được khởi tạo mới 12 lần trên mỗi frame vẽ. Vì đây là animation vô hạn chạy liên tục trong 9,000ms (`infiniteRepeatable`), trên màn hình 120Hz, thành phần này tạo ra hơn 1,440 đối tượng `Brush` và `List` mỗi giây.
  - Tại `StatsCharts.kt`, bên trong vòng lặp `animatedSlices.forEach { ... }` của `Canvas`, code liên tục tạo mới các đối tượng `Triple(slice, drawStart, drawSweep)`, `Stroke(width = slice.strokeDp.dp.toPx())`, `Size(diameter, diameter)` và chuyển đổi `Color(...)`.
  - Tại `CategoryGridPicker.kt:327`, hàm `Modifier.drawBehind` khởi tạo `Stroke(width = 2.dp.toPx())` trên mỗi lượt vẽ của từng item danh mục được chọn.
- **Phân tích nguyên nhân gốc rễ**:
  Giai đoạn Draw (Draw Phase) của Jetpack Compose được triệu gọi trên từng khung hình hiển thị (tần số 60Hz - 120Hz). Việc cấp phát các đối tượng đồ họa phức tạp (`Brush`, `Stroke`, `List`, `Size`) trong phạm vi `DrawScope` khiến bộ nhớ Heap bị phân mảnh nhanh chóng, kích hoạt Garbage Collector (GC) liên tục, gây ra hiện tượng giật khung hình (frame drops / jank).
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Chuyển việc khởi tạo `Brush` và `Stroke` ra ngoài Draw phase bằng cách dùng `remember`, hoặc sử dụng `CacheDrawScope` thông qua `Modifier.drawWithCache`:
  ```kotlin
  // Đề xuất sửa cho CategoryGridPicker.kt:
  val selectionStroke = remember(density) { Stroke(width = 2.dp.toPx()) }
  Modifier.drawBehind {
      if (isSelected) {
          drawCircle(
              color = accent,
              radius = size.minDimension / 2f + 2.5.dp.toPx(),
              style = selectionStroke, // Tái sử dụng đối tượng Stroke duy nhất
          )
      }
  }

  // Đề xuất sửa cho StatsCharts.kt:
  // Khởi tạo trước các đối tượng Stroke cố định qua remember bên ngoài Canvas
  val ringStroke = remember(RingStrokeWidth, density) {
      Stroke(width = RingStrokeWidth.toPx(), cap = StrokeCap.Round)
  }
  ```

---

#### [PERF-02] Trì hoãn đọc State biến thiên nhanh sang Draw Phase
- **Vị trí**:
  1. `app/src/main/java/com/notepay/core/designsystem/component/effect/FirefliesBackground.kt:24-30`
  2. `app/src/main/java/com/notepay/feature/stats/main/component/StatsCharts.kt:170-179`
- **Mô tả vấn đề**:
  State biến thiên liên tục (giá trị tiến trình animation `progress` từ `infiniteTransition.animateFloat`) đang được đọc trực tiếp tại Composition Scope trước khi chuyển vào Canvas, khiến toàn bộ Composable phải recompose lại trên mỗi frame thay vì chỉ thực hiện vẽ lại (draw pass).
- **Phân tích nguyên nhân gốc rễ**:
  Quy trình render của Jetpack Compose gồm 3 bước: Composition -> Layout -> Draw. Nếu một `State` được đọc ở Composition phase, bất kỳ sự thay đổi giá trị nào cũng buộc Compose phải chạy lại hàm Composable (recomposition). Khi đọc State trong lambda của `graphicsLayer { ... }` hoặc `drawWithCache { ... }`, Compose bỏ qua hoàn toàn Composition và Layout phase, chỉ kích hoạt Draw phase, giúp tiết kiệm tối đa CPU.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Đưa việc đọc state animation vào bên trong `graphicsLayer` hoặc lambda của `drawWithContent`:
  ```kotlin
  // Thay vì đọc progress ở scope ngoài, truyền trực tiếp State<Float> hoặc đọc trong draw scope
  Modifier.graphicsLayer {
      // Đọc progress.value tại đây để trì hoãn xuống draw phase
  }
  ```

---

#### [PERF-03] Mô hình UI State thiếu chú thích `@Immutable` / `@Stable` gây Recomposition không cần thiết
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/billsplit/main/BillSplitUiState.kt:8-22` (`BillSplitUiState`, `BillSplitItemState`)
  2. `app/src/main/java/com/notepay/feature/debt/management/DebtUiState.kt:8-16` (`DebtUiState`)
  3. `app/src/main/java/com/notepay/feature/assets/main/sheet/AddWalletUiState.kt:13-42` (`AddWalletUiState`)
  4. `app/src/main/java/com/notepay/feature/category/CategoryManagementViewModel.kt:15-18` (`CategoryManagementUiState`)
  5. `app/src/main/java/com/notepay/feature/subscription/main/SubscriptionViewModel.kt:28-46` (`SubscriptionUiState`, `AddSubscriptionDialogState`)
  6. `app/src/main/java/com/notepay/feature/transaction/detail/TransactionDetailUiState.kt:5-11` (`TransactionDetailUiState`)
  7. `app/src/main/java/com/notepay/feature/transaction/list/TransactionListUiState.kt:14-40` (`TransactionListUiState`, `TransactionDayGroup`)
  8. `app/src/main/java/com/notepay/feature/settings/ai/AiSettingsViewModel.kt:26-32` (`AiSettingsUiState`)
- **Mô tả vấn đề**:
  Toàn bộ các data class UI State nêu trên chứa các thuộc tính danh sách như `List<T>`, `Set<T>`, hoặc `Map<K, V>` nhưng hoàn toàn không được gắn annotation `@Immutable` hoặc `@Stable`.
- **Phân tích nguyên nhân gốc rễ**:
  Trình biên dịch Compose Compiler không thể suy luận các giao diện chuẩn của Kotlin (`List`, `Set`, `Map`) là bất biến (vì về mặt kỹ thuật, đối tượng truyền vào có thể là một `MutableList` ngầm). Do đó, Compose compiler đánh dấu các lớp UI State này là **Unstable**. Hậu quả là mọi hàm Composable nhận các State này làm tham số sẽ mất khả năng **Skipping** (bỏ qua recomposition khi dữ liệu không đổi), dẫn tới hiện tượng recomposition dây chuyền toàn bộ màn hình mỗi khi có bất kỳ state cục bộ nào cập nhật.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Gắn nhãn `@Immutable` lên định nghĩa của toàn bộ các lớp UI State và các đối tượng dữ liệu con:
  ```kotlin
  // Đề xuất sửa cho BillSplitUiState.kt:
  import androidx.compose.runtime.Immutable

  @Immutable
  data class BillSplitUiState(
      val unpaidSplits: List<BillSplitItemState> = emptyList(),
      val paidSplits: List<BillSplitItemState> = emptyList(),
      val recentTransactions: List<Transaction> = emptyList(),
      val wallets: List<Wallet> = emptyList(),
      val activeWallet: Wallet? = null,
      val banks: List<VietQrBank> = emptyList(),
      val isLoading: Boolean = true,
  )

  @Immutable
  data class BillSplitItemState(
      val split: BillSplit,
      val parentTransaction: Transaction?,
      val wallet: Wallet? = null,
  )
  ```

---

#### [PERF-04] Cấp phát đối tượng gây Autoboxing State (`AutoboxingStateCreation`)
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/billsplit/main/sheet/BillSplitCreateSheet.kt:169`
  2. `app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt:446`
- **Mô tả vấn đề**:
  - Tại `BillSplitCreateSheet.kt:169`: `var selectedTransactionId by rememberSaveable { mutableStateOf(NoTransactionSelected) }` (trong đó `NoTransactionSelected` là kiểu nguyên thủy `Long = -1L`).
  - Tại `CategoryGridPicker.kt:446`: `var selectedColor by remember { mutableStateOf(colors.first()) }` (lưu giá trị `Long` mã màu).
- **Phân tích nguyên nhân gốc rễ**:
  Hàm `mutableStateOf(value)` trả về `SnapshotMutableStateImpl<T>`, buộc trình biên dịch phải đóng gói (box) giá trị kiểu `Long` thành đối tượng `java.lang.Long` trên Heap mỗi khi giá trị thay đổi.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Sử dụng các hàm chuyên biệt dành riêng cho kiểu nguyên thủy do Jetpack Compose cung cấp:
  ```kotlin
  // Đề xuất sửa:
  var selectedTransactionId by rememberSaveable { mutableLongStateOf(NoTransactionSelected) }
  var selectedColor by remember { mutableLongStateOf(colors.first()) }
  ```

---

### DANH MỤC 2: QUẢN LÝ TÀI NGUYÊN, COROUTINES & VÒNG ĐỜI (RESOURCE MANAGEMENT)

#### [RES-01] Thu thập Flow không nhận biết vòng đời ứng dụng (`collectAsState` thay vì `collectAsStateWithLifecycle`)
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/backup/BackupRestoreScreen.kt:72`
  2. `app/src/main/java/com/notepay/feature/billsplit/BillSplitNavigation.kt:36`
  3. `app/src/main/java/com/notepay/feature/subscription/SubscriptionNavigation.kt:34`
- **Mô tả vấn đề**:
  - Tại `BackupRestoreScreen.kt:72`, `viewModel.state` được thu thập bằng `collectAsState()` trong khi ngay dòng 73 kế tiếp, `authViewModel.uiState` lại dùng chuẩn `collectAsStateWithLifecycle()`.
  - Tại `BillSplitNavigation.kt:36` và `SubscriptionNavigation.kt:34`, `showCreateFlow` được thu thập từ `savedStateHandle` bằng `collectAsState()`.
- **Phân tích nguyên nhân gốc rễ**:
  Hàm `collectAsState()` thu thập Flow theo vòng đời của Composition, hoàn toàn không nhận biết trạng thái của Android Lifecycle (`STARTED`, `RESUMED`, `STOPPED`). Khi người dùng đưa ứng dụng xuống nền (Home screen) hoặc mở màn hình khác đè lên, Flow collection vẫn tiếp tục hoạt động ngầm, tiêu tốn CPU, pin và giữ các luồng phát dữ liệu từ Room Database tiếp tục chạy lãng phí.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Thay thế toàn bộ bằng `collectAsStateWithLifecycle()`:
  ```kotlin
  // Đề xuất sửa:
  val state by viewModel.state.collectAsStateWithLifecycle()
  val showCreate by showCreateFlow.collectAsStateWithLifecycle()
  ```

---

#### [RES-02] Bắt ngoại lệ chung làm nuốt mất `CancellationException` phá vỡ cơ chế hủy Coroutine
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/billsplit/main/BillSplitViewModel.kt:117, 141, 162, 195`
  2. `app/src/main/java/com/notepay/feature/subscription/main/SubscriptionViewModel.kt:151`
  3. `app/src/main/java/com/notepay/feature/debt/detail/DebtDetailViewModel.kt:90`
  4. `app/src/main/java/com/notepay/data/backup/DataExporter.kt:53-57`
  *(Ghi chú làm rõ về `AuthViewModel.kt`: Trước đó có nghi vấn tại dòng 136, tuy nhiên đối chiếu mã nguồn thực tế xác nhận tại dòng 122-123, hàm `signIn()` đã xử lý chuẩn `catch (e: CancellationException) { throw e }` trước khi tới khối generic catch ở dòng 136, do đó không vi phạm)*.
- **Mô tả vấn đề**:
  - Tại `BillSplitViewModel.kt`, các hàm `createBillSplits()`, `markAsPaid()`, `deleteBillSplit()`, và `saveVietQrConfig()` sử dụng khối `try { ... } catch (e: Exception) { ... }` và phát tín hiệu `FeedbackType.Error` lên UI khi gặp ngoại lệ. Khi coroutine bị hủy, `CancellationException` bị bắt lại như một lỗi thực tế và hiển thị thông báo lỗi sai lệch cho người dùng.
  - Tại `SubscriptionViewModel.kt:151`, khối `catch (e: Exception)` trong `saveSubscription()` âm thầm nuốt sạch ngoại lệ mà không re-throw `CancellationException`.
  - Tại `DebtDetailViewModel.kt:90`, khối `init { viewModelScope.launch { ... catch (_: Exception) } }` nuốt ngoại lệ mạng mà không re-throw `CancellationException`.
  - Tại `DataExporter.kt:53-57`, hàm `suspend fun exportToJson()` thực hiện thu thập cài đặt ngân sách:
    ```kotlin
    val settings = try {
        budgetSettingsStore.settings.first()
    } catch (_: Exception) {
        com.notepay.data.datastore.BudgetSettings()
    }
    ```
    Khi coroutine scope chứa tác vụ xuất dữ liệu bị hủy (ví dụ: người dùng hủy tiến trình hoặc rời màn hình), `budgetSettingsStore.settings.first()` ném `CancellationException`. Khối `catch (_: Exception)` nuốt mất ngoại lệ này và âm thầm tiếp tục tiến trình xuất tệp sao lưu với cấu hình mặc định thay vì dừng coroutine ngay lập tức theo nguyên tắc cooperative cancellation.
- **Phân tích nguyên nhân gốc rễ**:
  Cơ chế Cooperative Cancellation của Kotlin Coroutines hoạt động dựa trên việc ném ra `CancellationException`. Nếu lập trình viên sử dụng `catch (e: Exception)` hoặc `catch (t: Throwable)` mà không kiểm tra hoặc re-throw `CancellationException`, Coroutine Job sẽ bị coi là đã hoàn thành với lỗi thay vì bị hủy bình thường, dẫn tới việc giữ lại các tài nguyên chưa được dọn dẹp và phát sinh các hành vi lỗi logic không mong muốn.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Tuân thủ nguyên tắc "Never swallow CancellationException" đã quy định trong `AGENTS.md`:
  ```kotlin
  // Đề xuất chuẩn hóa cho ViewModel:
  try {
      billSplitRepository.delete(splitId)
      _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_deleted), type = FeedbackType.Success))
  } catch (e: CancellationException) {
      throw e // Bắt buộc re-throw để coroutine hierarchy xử lý hủy
  } catch (e: Exception) {
      _feedback.emit(UiFeedback(str(R.string.feedback_bill_split_delete_failed), type = FeedbackType.Error))
  }

  // Đề xuất chuẩn hóa cho DataExporter.kt:53-57:
  val settings = try {
      budgetSettingsStore.settings.first()
  } catch (e: CancellationException) {
      throw e // Cho phép hủy tác vụ xuất tệp
  } catch (_: Exception) {
      com.notepay.data.datastore.BudgetSettings()
  }
  ```

---

### DANH MỤC 3: KIẾN TRÚC CLEAN ARCHITECTURE & STATE

#### [ARCH-01] Rò rỉ khởi tạo ViewModel xuống sâu trong Composable con (`hiltViewModel()` lồng nhau)
- **Vị trí**: `app/src/main/java/com/notepay/feature/transaction/list/TransactionListScreen.kt:545`
- **Mô tả vấn đề**:
  Bên trong hàm hiển thị chi tiết phụ `DetailPane` của màn hình danh sách giao dịch, code gọi trực tiếp:
  ```kotlin
  val detailViewModel: TransactionDetailViewModel = hiltViewModel()
  LaunchedEffect(transactionId) { detailViewModel.select(transactionId) }
  ```
- **Phân tích nguyên nhân gốc rễ**:
  Vi phạm nguyên tắc Clean Architecture và Stateful Route / Stateless Screen. Việc gọi `hiltViewModel()` ở tầng sâu của một Composable con khiến thành phần này bị gắn chặt với vòng đời của Hilt và ViewModelStore của màn hình cha, làm Composable mất khả năng kiểm thử giao diện độc lập (UI testing), không thể hiển thị trong `@Preview`, và phá vỡ luồng dữ liệu đơn hướng (UDF).
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Nâng (hoist) trạng thái lên cấp Route gốc (`TransactionListRoute`), khởi tạo `TransactionDetailViewModel` ở Route và chỉ truyền `detailState` cùng các callbacks (`onEdit`, `onCreateBillSplit`) xuống `TransactionListScreen`.

---

#### [ARCH-02] Dấu vết tàn dư của IDE sót trong mã nguồn (`_root_ide_package_`)
- **Vị trí**: `app/src/main/java/com/notepay/feature/assets/main/sheet/AddWalletScreen.kt:155-156`
- **Mô tả vấn đề**:
  Mã nguồn chứa tiền tố import giả lập của IDE:
  ```kotlin
  _root_ide_package_.com.notepay.core.designsystem.component.bar.GradientBottomActionBar {
      _root_ide_package_.com.notepay.core.designsystem.component.button.LiquidButton(...)
  }
  ```
- **Phân tích nguyên nhân gốc rễ**:
  Đây là kết quả của tính năng tự động sửa code hoặc refactor chưa hoàn tất từ Android Studio / IntelliJ IDEA, phản ánh sự thiếu hụt quy trình kiểm tra mã nguồn tự động đối với các tiền tố package bất thường trước khi commit.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Thêm import trực tiếp vào đầu tệp và loại bỏ hoàn toàn tiền tố `_root_ide_package_`:
  ```kotlin
  import com.notepay.core.designsystem.component.bar.GradientBottomActionBar
  import com.notepay.core.designsystem.component.button.LiquidButton
  ```

---

#### [ARCH-03] Màn hình đơn khối (Screen Monolith) thiếu tách biệt Component
- **Vị trí**: `app/src/main/java/com/notepay/feature/assets/main/sheet/AddWalletScreen.kt` (1,229 dòng code trong 1 tệp duy nhất).
- **Mô tả vấn đề**:
  Toàn bộ giao diện tạo ví, danh sách icon, bộ chọn màu sắc, danh sách ngân hàng liên kết, các nút xóa/lưu trữ/phục hồi đều nằm dồn vào một tệp duy nhất với hơn 1,200 dòng code.
- **Phân tích nguyên nhân gốc rễ**:
  Thiếu sự phân rã thành các composable thành phần tái sử dụng, làm tăng độ phức tạp nhận thức (cognitive complexity) và gây khó khăn lớn cho việc bảo trì, tối ưu recomposition.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Tách các section thành các component riêng biệt: `WalletBasicInfoSection.kt`, `WalletColorPickerSection.kt`, `WalletBankLinkingSection.kt`, `WalletActionButtonsSection.kt`.

---

### DANH MỤC 4: TRÀN VIỀN & HỖ TRỢ CO GIÃN CHỮ (UI CLIPPING & FONT SCALING)

#### [CLIP-01] Khóa cứng chiều cao (Fixed Height) trên các thành phần chứa chữ
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/billsplit/main/sheet/VietQrConfigFullScreenDialog.kt:226`
     ```kotlin
     Card(modifier = Modifier.fillMaxWidth().height(68.dp).clickable(onClick = onClick))
     ```
  2. `app/src/main/java/com/notepay/feature/transaction/component/TransactionFormSections.kt:72`
     ```kotlin
     BoxWithConstraints(modifier = modifier.fillMaxWidth().height(48.dp)...)
     ```
  3. `app/src/main/java/com/notepay/feature/assets/main/sheet/AddWalletScreen.kt:454, 475, 495`
     ```kotlin
     OutlinedButton(modifier = Modifier.fillMaxWidth().height(48.dp))
     ```
  4. `app/src/main/java/com/notepay/feature/assets/main/sheet/AddWalletScreen.kt:162`
     ```kotlin
     LiquidButton(modifier = Modifier.fillMaxWidth().height(52.dp))
     ```
- **Mô tả vấn đề**:
  Các container và nút bấm chứa chuỗi văn bản bản địa hóa tiếng Việt đang bị giới hạn cứng bằng `Modifier.height(...)`.
- **Phân tích nguyên nhân gốc rễ**:
  Chuỗi văn bản tiếng Việt có độ dài trung bình lớn hơn tiếng Anh từ 20% đến 30%. Khi người dùng kích hoạt cài đặt trợ năng phóng to phông chữ (Font Scale từ 1.3x đến 2.0x), chiều cao của dòng chữ (`lineHeight`) tăng vượt quá chiều cao cố định của container. Do bị giới hạn bởi `height(48.dp)` hoặc `height(68.dp)`, các dòng chữ bên dưới (`bank.name`, nhãn nút "Lưu trữ ví", "Xóa ví") sẽ bị cắt cụt (clipping) nửa chữ hoặc tràn mất hoàn toàn khỏi màn hình.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Tuân thủ quy tắc số 4 trong `AGENTS.md` và §4 của `DESIGN.md`: Không dùng `Modifier.height(...)` cho các phần tử chứa text, chuyển sang dùng `Modifier.defaultMinSize(minHeight = ...)` hoặc `Modifier.heightIn(min = ...)` kết hợp với `TextOverflow.Ellipsis`:
  ```kotlin
  // Đề xuất sửa:
  Card(
      modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 68.dp)
          .clickable(onClick = onClick)
  )
  OutlinedButton(
      modifier = Modifier
          .fillMaxWidth()
          .defaultMinSize(minHeight = 48.dp)
  )
  ```

---

#### [CLIP-02] Thiếu cơ chế tự động xếp dọc (Vertical Stacking) khi Font Scale $\ge 1.45\text{x}$
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/billsplit/main/sheet/VietQrConfigFullScreenDialog.kt:231` (Row chứa logo ngân hàng và thông tin)
  2. `app/src/main/java/com/notepay/feature/debt/management/component/DebtSummaryCard.kt`
- **Mô tả vấn đề**:
  Các ô chỉ số tài chính nằm ngang trong `Row` không kiểm tra ngưỡng `LocalDensity.current.fontScale`. Khi phông chữ đạt mức 1.5x - 2.0x, các cột số tiền và nhãn chữ đè lấn lên nhau hoặc đẩy thành phần bên phải tràn khỏi màn hình.
- **Phân tích nguyên nhân gốc rễ**:
  `DESIGN.md` §4 quy định: Khi `fontScale >= 1.45f`, các khối chỉ số đôi (như Thu nhập / Chi tiêu, Cho vay / Nợ) bắt buộc phải tự động chuyển bố cục từ xếp ngang (`Row`) sang xếp dọc (`Column`).
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Áp dụng kiểm tra fontScale theo spec:
  ```kotlin
  val fontScale = LocalDensity.current.fontScale
  if (fontScale >= 1.45f) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { ... }
  } else {
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { ... }
  }
  ```

---

### DANH MỤC 5: MÀU SẮC, CHẾ ĐỘ TỐI (DARK MODE) & DESIGN SYSTEM V5.0.0

#### [COLOR-01] Sử dụng mã màu Hex cố định (Hardcoded Hex Colors) phá vỡ tính năng giao diện động
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/settings/ai/AiSettingsScreen.kt:208, 221, 248, 269, 362, 368, 393, 413, 636, 638, 649, 655` (các mã màu Hex thô: `Color(0xFF007AFF)`, `Color(0xFFFF9500)`, `Color(0xFF5856D6)`, `Color(0xFF34C759)`, `Color(0xFF2E7D32)`).
  2. `app/src/main/java/com/notepay/feature/settings/appearance/AppearanceLanguageScreen.kt:259, 285` (`Color(0xFF007AFF)`, `Color(0xFFFF9500)`).
  3. `app/src/main/java/com/notepay/feature/settings/notification/NotificationSettingsScreen.kt:122, 138, 154, 171` (`Color(0xFFFF9500)`, `Color(0xFF007AFF)`, `Color(0xFF34C759)`, `Color(0xFF5856D6)`).
  4. `app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt:78-83` (hardcoded palette màu sắc).
- **Mô tả vấn đề**:
  Nhiều màn hình cài đặt và thành phần chọn danh mục đang dùng trực tiếp hàm khởi tạo `Color(0x...)` với các mã màu chuẩn của iOS cũ hoặc bảng màu cứng.
- **Phân tích nguyên nhân gốc rễ**:
  Vi phạm nghiêm trọng Quy tắc cứng số 1 trong `AGENTS.md` ("No hard-coded hex in composables"). Việc hardcode mã màu hex làm mất khả năng tự động thích ứng với Chế độ tối (Dark Mode), không được hưởng lợi từ cơ chế bảo vệ tương phản của `AppAtmosphericBackground`, và gây sai lệch độ tương phản WCAG khi nền chuyển đổi.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Thay thế toàn bộ bằng các token màu ngữ nghĩa từ `AppTheme.colors` hoặc `MaterialTheme.colorScheme`:
  ```kotlin
  // Đề xuất sửa:
  // Thay Color(0xFF007AFF) bằng AppTheme.colors.accent hoặc MaterialTheme.colorScheme.primary
  // Thay Color(0xFF34C759) bằng AppTheme.colors.income
  // Thay Color(0xFFFF9500) bằng AppTheme.colors.warning
  ```

---

#### [COLOR-02] Thành phần Tier 2 chưa chuyển đổi sang Design System v5.0.0 (Thiếu đổ bóng hai lớp `iosCardShadow`)
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/debt/management/component/DebtItemCard.kt`
  2. `app/src/main/java/com/notepay/feature/billsplit/main/component/BillSplitDebtorList.kt`
- **Mô tả vấn đề**:
  Các thành phần thẻ thuộc phân hệ Tier 2 này hiện tại hoàn toàn chưa được nâng cấp lên chuẩn thiết kế v5.0.0 (Luminous iOS & Atmospheric Canvas). Cụ thể, `DebtItemCard.kt` và `BillSplitDebtorList.kt` vẫn đang dùng giao diện phẳng v4 Sand & Ink (`elevation = 0.dp`, không có đổ bóng), hoàn toàn thiếu vắng `Modifier.iosCardShadow(shape, elevated = false)` và các token thẻ Luminous iOS.
- **Phân tích nguyên nhân gốc rễ**:
  Dự án mới hoàn thành chuyển đổi Design System v5.0.0 trên các màn hình Tier 1 (Home, Stats, Assets, TransactionForm, Utilities). Các thành phần con ở Tier 2 chưa được tích hợp hệ thống đổ bóng hai lớp (Ambient shadow 24dp + Contact shadow 6dp) và viền hairline 0.5dp (`hairline`). Khi tiến hành chuyển đổi sang v5, cần đặc biệt lưu ý thứ tự Modifier trong Compose: `Modifier.iosCardShadow` bắt buộc phải đặt trước các modifier `.clip()`, `.background()`, hoặc `Surface()`. Nếu đặt sau `.clip()`, toàn bộ phần bóng lan tỏa bên ngoài sẽ bị cắt cụt.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Thực hiện nâng cấp các thành phần này lên chuẩn v5 theo `DESIGN.md` §3:
  ```kotlin
  // Đề xuất chuẩn hóa khi di chuyển lên v5:
  Card(
      modifier = modifier
          .fillMaxWidth()
          .iosCardShadow(shape = AppTheme.shapes.corner24, elevated = false) // Đặt TRƯỚC clip/background
          .clip(AppTheme.shapes.corner24)
          .clickable(onClick = onClick),
      shape = AppTheme.shapes.corner24,
      colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
      border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
  )
  ```

---

### DANH MỤC 6: KHẢ NĂNG TIẾP CẬN (ACCESSIBILITY / TALKBACK)

#### [A11Y-01] Thiếu mô tả trợ năng (`contentDescription = null`) trên nút tương tác
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/settings/ai/AiSettingsScreen.kt:556`
     ```kotlin
     IconButton(onClick = onToggleVisibility) {
         Icon(
             imageVector = if (isApiKeyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
             contentDescription = null, // Vi phạm a11y
         )
     }
     ```
  2. `app/src/main/java/com/notepay/feature/debt/management/sheet/CreateDebtBottomSheet.kt:260`
     ```kotlin
     IconButton(onClick = { selectedDueDateMillis = null }) {
         Icon(
             imageVector = Icons.Rounded.Clear,
             contentDescription = null, // Vi phạm a11y
         )
     }
     ```
- **Mô tả vấn đề**:
  Các nút bấm biểu tượng thực hiện hành động (Bật/tắt ẩn hiện API Key, Xóa ngày hạn nợ) đang được gán `contentDescription = null`.
- **Phân tích nguyên nhân gốc rễ**:
  Khi người dùng khiếm thị sử dụng trình đọc màn hình Google TalkBack, các phần tử `IconButton` có `contentDescription = null` sẽ bị TalkBack đọc thành "Nút không có nhãn" (Unlabelled button). Người dùng không thể biết nút này có công dụng gì để kích hoạt.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Gán mô tả chuỗi đa ngôn ngữ rõ ràng cho mọi nút hành động:
  ```kotlin
  // Đề xuất sửa cho AiSettingsScreen.kt:
  contentDescription = stringResource(
      if (isApiKeyVisible) R.string.cd_hide_api_key else R.string.cd_show_api_key
  )

  // Đề xuất sửa cho CreateDebtBottomSheet.kt:
  contentDescription = stringResource(R.string.cd_clear_due_date)
  ```

---

#### [A11Y-02] Kích thước vùng chạm không đạt chuẩn tối thiểu $\ge 48\text{dp}$
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt:321` (kích thước ô chọn danh mục `Modifier.size(52.dp)` nhưng padding trong chỉ khoảng 32dp).
  2. Các `FilterChip` mặc định trong các màn hình lọc (`TransactionFilterSheets.kt`).
- **Mô tả vấn đề**:
  Một số thành phần chip lọc và nút bấm tùy biến có chiều cao vật lý chỉ đạt 32dp đến 40dp mà không có vùng đệm tương tác mở rộng.
- **Phân tích nguyên nhân gốc rễ**:
  Tiêu chuẩn WCAG 2.1 và Material 3 Accessibility Guidelines yêu cầu mọi thành phần có thể tương tác (Interactive Target) phải có kích thước tối thiểu là $48\text{dp} \times 48\text{dp}$ để người dùng gặp khó khăn về vận động có thể thao tác chính xác, không bấm trượt.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Bổ sung `Modifier.minimumInteractiveComponentSize()` hoặc thiết lập `defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)`.

---

### DANH MỤC 7: XỬ LÝ TRẠNG THÁI GIAO DIỆN (STATE HANDLING & EDGE CASES)

#### [STATE-01] Thiếu trạng thái Loading gây hiện tượng nháy màn hình trống (Flash of Empty State)
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/category/CategoryManagementScreen.kt:179-180`
  2. `app/src/main/java/com/notepay/feature/category/CategoryManagementViewModel.kt:15-18`
- **Mô tả vấn đề**:
  Lớp `CategoryManagementUiState` chỉ định nghĩa:
  ```kotlin
  data class CategoryManagementUiState(
      val expenseCategories: List<Category> = emptyList(),
      val incomeCategories: List<Category> = emptyList(),
  )
  ```
  Không hề có thuộc tính `isLoading: Boolean`.
  Khi mở màn hình `CategoryManagementScreen`, trong thời gian vài trăm mili-giây đầu tiên khi Room DB đang tải dữ liệu không đồng bộ, `customList.isEmpty()` trả về `true`. Màn hình lập tức dựng thẻ thông báo "Chưa có danh mục tùy biến nào", sau đó khi dữ liệu tải xong thì danh mục thật mới hiện ra, gây hiện tượng nháy giao diện (flicker) rất khó chịu.
- **Phân tích nguyên nhân gốc rễ**:
  Thiếu mô hình hóa đầy đủ 4 trạng thái dữ liệu (Loading, Empty, Error, Content). Khởi tạo danh sách bằng `emptyList()` mà không có cờ phân biệt giữa "Đang tải dữ liệu" và "Dữ liệu thực tế rỗng".
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Bổ sung cờ `isLoading: Boolean = true` vào `CategoryManagementUiState` và hiển thị Skeleton / Shimmer hoặc CircularProgressIndicator trong khi `isLoading == true`:
  ```kotlin
  // Đề xuất sửa:
  @Immutable
  data class CategoryManagementUiState(
      val expenseCategories: List<Category> = emptyList(),
      val incomeCategories: List<Category> = emptyList(),
      val isLoading: Boolean = true,
  )
  ```

---

### DANH MỤC 8: CHUẨN HÓA MATERIAL 3 & NAVIGATION

#### [M3-01] Vi phạm quy chuẩn tham số `modifier` của Jetpack Compose (`ModifierParameter`)
- **Vị trí**: Phát hiện tại 12 composable công khai bởi Android Lint:
  1. `app/src/main/java/com/notepay/feature/settings/appearance/AppearanceLanguageScreen.kt:93`
  2. `app/src/main/java/com/notepay/feature/assets/main/component/AssetsCards.kt:684`
  3. `app/src/main/java/com/notepay/feature/auth/AuthWelcomeScreen.kt:65`
  4. `app/src/main/java/com/notepay/feature/category/CategoryManagementScreen.kt:82`
  5. `app/src/main/java/com/notepay/feature/settings/currency/CurrencyConverterScreen.kt:112`
  6. `app/src/main/java/com/notepay/feature/settings/currency/CurrencySettingsScreen.kt:73`
  7. `app/src/main/java/com/notepay/feature/billsplit/main/detail/DebtorDetailScreen.kt:499`
  8. `app/src/main/java/com/notepay/core/navigation/MainTabPager.kt:30`
  9. `app/src/main/java/com/notepay/feature/settings/notification/NotificationSettingsScreen.kt:61`
  10. `app/src/main/java/com/notepay/feature/onboarding/components/OnboardingComponents.kt:90`
  11. `app/src/main/java/com/notepay/feature/stats/main/component/StatsCharts.kt:267`
  12. `app/src/main/java/com/notepay/feature/transaction/component/TransactionInputComponents.kt:481`
- **Mô tả vấn đề**:
  Trong các Composable trên, tham số `modifier: Modifier = Modifier` được đặt sau các tham số có giá trị mặc định khác (ví dụ tại `MainTabPager.kt:28-30`, `userScrollEnabled: Boolean = true` và `contentStartInset: Dp = 0.dp` đứng trước `modifier`).
- **Phân tích nguyên nhân gốc rễ**:
  Quy chuẩn thiết kế API của Google Jetpack Compose quy định: Tham số `modifier` phải là tham số tùy chọn (optional parameter) đầu tiên trong danh sách tham số để duy trì tính nhất quán cho các lập trình viên khi gọi hàm bằng vị trí hoặc named argument.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Chuyển `modifier: Modifier = Modifier` lên vị trí tham số tùy chọn đầu tiên ngay sau các tham số bắt buộc.

---

#### [M3-02] Dùng API đo kích thước cửa sổ cũ (`ConfigurationScreenWidthHeight`)
- **Vị trí**:
  1. `app/src/main/java/com/notepay/feature/stats/main/component/StatsAdvisorSection.kt:249-250`
  2. `app/src/main/java/com/notepay/core/designsystem/adaptive/WindowLayout.kt:69`
- **Mô tả vấn đề**:
  Code đang gọi `LocalConfiguration.current.screenWidthDp` để tính toán bề rộng của thẻ và khoảng lùi Navigation Rail.
- **Phân tích nguyên nhân gốc rễ**:
  `Configuration.screenWidthDp` là API cũ của Android, giá trị trả về bị làm tròn số nguyên và không phản ánh chính xác kích thước cửa sổ khi ứng dụng chạy ở chế độ chia đôi màn hình (Multi-window), màn hình gập (Foldables) hoặc khi có insets phức tạp.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Sử dụng `LocalWindowInfo.current.containerSize` (hoặc WindowSizeClass chính thức):
  ```kotlin
  // Đề xuất sửa:
  val windowInfo = LocalWindowInfo.current
  val density = LocalDensity.current
  val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
  ```

---

#### [M3-03] Chuyển đổi hộp thoại dạng nổi (Dialog) sang `ModalBottomSheet`
- **Vị trí**: `app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt:449`
- **Mô tả vấn đề**:
  Thành phần tạo danh mục mới đang sử dụng `AlertDialog` truyền thống chứa form nhập liệu phức tạp (gồm TextField, bảng màu sắc và danh sách icon cuộn).
- **Phân tích nguyên nhân gốc rễ**:
  Quy định tại `ui-ux-audit-guide.md` Hạng mục 2: Bắt buộc chuyển đổi các hộp thoại chứa form nhập liệu hoặc danh sách chọn dài sang `ModalBottomSheet` (với bo góc `sheetTop`, lớp phủ scrim 45%). `AlertDialog` chỉ được giữ lại cho các thông báo xác nhận hủy diệt ngắn (như xác nhận Xóa).
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  Thay thế `AlertDialog` bằng `ModalBottomSheet` có thanh kéo chuẩn M3.

---

### DANH MỤC 9: BẢN ĐỊA HÓA & CHUỖI ĐA NGÔN NGỮ (LOCALIZATION & STRINGS)

#### [STR-01] Bất đồng bộ tham số Format chuỗi đa ngôn ngữ (`StringFormatCount`) — [MỨC ĐỘ P1 & P2]
- **Vị trí**:
  1. `app/src/main/res/values/strings.xml:57` vs `app/src/main/res/values-en/strings.xml:58` (`home_day_other`) — **Mức độ P1 (High)**
  2. `app/src/main/res/values-en/strings.xml:701` vs `app/src/main/res/values/strings.xml:1267` (`billsplit_paid_note_format`) — **Mức độ P2 (Medium)**
- **Mô tả vấn đề & Phân tích chi tiết**:
  - **Khóa `home_day_other` (P1 - Rủi ro format chuỗi đa ngôn ngữ)**:
    - Điểm gọi hàm thực tế tại `HomeTransactionFeed.kt:74-76`:
      ```kotlin
      val monthName = date.month.name.take(3)
      otherFormat.format(date.day, monthName, date.month.number)
      ```
      Hàm `format` truyền chính xác **3 tham số**:
      - Tham số 1: `date.day` (kiểu `Int`)
      - Tham số 2: `monthName` (kiểu `String`, ví dụ `"OCT"`, `"NOV"`)
      - Tham số 3: `date.month.number` (kiểu `Int`, ví dụ `10`, `11`)
    - Trong `values/strings.xml:57` (tiếng Việt): `<string name="home_day_other">%1$d THÁNG %3$d</string>` (dùng tham số 1 và 3, kiểu số nguyên).
    - Trong `values-en/strings.xml:58` (tiếng Anh): `<string name="home_day_other">%2$s %1$d</string>` (dùng tham số 2 kiểu chuỗi và tham số 1 kiểu số nguyên).
    - *Hiện trạng thực tế*: Ứng dụng hiện tại **KHÔNG BỊ CRASH** ở cả 2 ngôn ngữ vì call site luôn cung cấp đủ 3 tham số hợp lệ về kiểu. Android Lint cảnh báo `StringFormatCount` do số lượng và vị trí chỉ số format specifier không đồng nhất giữa các locale (tiếng Việt dùng specifier 1 và 3; tiếng Anh dùng specifier 1 và 2).
    - *Cảnh báo rủi ro (Caveat)*: Tuyệt đối **KHÔNG ĐƯỢC** tùy tiện đổi chuỗi tiếng Việt thành `<string name="home_day_other">%1$d tháng %2$d</string>` nếu không sửa call site. Vì tham số thứ 2 truyền vào là `monthName` (`String`), specifier `%2$d` sẽ lập tức ném ra ngoại lệ `java.util.IllegalFormatConversionException: d != java.lang.String` và gây sập ứng dụng (Crash) ngay trên màn hình Home!
  - **Khóa `billsplit_paid_note_format` (P2 - Thiếu thông tin bản địa hóa)**:
    - Điểm gọi hàm thực tế tại `BillSplitScreen.kt:696-700`:
      ```kotlin
      text = stringResource(
          R.string.billsplit_paid_note_format,
          note,
          PresentationDateFormatter.formatDayMonth(split.paidAt),
      )
      ```
      Call site truyền đủ 2 tham số: `note` (String) và `paidAt` (String).
    - Tiếng Anh: `<string name="billsplit_paid_note_format">%1$s · %2$s</string>` (sử dụng cả 2 tham số).
    - Tiếng Việt: `<string name="billsplit_paid_note_format">Đã trả nợ cho %1$s</string>` (chỉ sử dụng tham số 1).
    - *Hiện trạng thực tế*: Trong Java và Android `String.format`, việc truyền thừa tham số sẽ được JVM tự động bỏ qua mà **HOÀN TOÀN KHÔNG BỊ CRASH**. Tuy nhiên, bản dịch tiếng Việt đang bị rơi mất thông tin ngày trả nợ (`paidAt`), gây thiệt thòi trải nghiệm cho người dùng tiếng Việt.
- **Hướng xử lý & Code mẫu đề xuất khắc phục**:
  - Đối với `billsplit_paid_note_format` (P2): Bổ sung tham số thứ 2 vào bản tiếng Việt:
    ```xml
    <!-- values/strings.xml -->
    <string name="billsplit_paid_note_format">Đã trả nợ cho %1$s · %2$s</string>
    ```
  - Đối với `home_day_other` (P1): Chọn một trong hai phương án đồng bộ an toàn:
    - **Phương án A (Giữ nguyên Call Site 3 tham số, triệt tiêu cảnh báo Lint)**:
      Giữ nguyên chuỗi định dạng an toàn cho cả hai locale:
      ```xml
      <!-- values/strings.xml -->
      <string name="home_day_other">%1$d THÁNG %3$d</string>
      <!-- values-en/strings.xml -->
      <string name="home_day_other">%2$s %1$d</string>
      ```
      (Có thể bổ sung `tools:ignore="StringFormatCount"` để làm rõ chủ đích thiết kế đa ngôn ngữ).
    - **Phương án B (Đồng bộ đồng thời Call Site và XML về 2 tham số)**:
      Sửa call site tại `HomeTransactionFeed.kt:75`:
      ```kotlin
      otherFormat.format(date.day, date.month.number) // Truyền 2 tham số Int
      ```
      Đồng thời cập nhật tài nguyên chuỗi XML:
      ```xml
      <!-- values/strings.xml -->
      <string name="home_day_other">%1$d THÁNG %2$d</string>
      <!-- values-en/strings.xml -->
      <string name="home_day_other">%2$d/%1$d</string>
      ```

---

#### [STR-02] Chưa sử dụng tài nguyên số nhiều `<plurals>` (`PluralsCandidate`)
- **Vị trí**: 52 vị trí được cảnh báo bởi Android Lint (ví dụ: `%1$d nguồn`, `%1$d choices`, `%1$d ngày`, `%1$d transactions`, `%1$d giao dịch`...).
- **Mô tả vấn đề**: Dùng ghép chuỗi `%d` với danh từ số ít khiến ngôn ngữ tiếng Anh không thể phân biệt giữa `1 transaction` và `2 transactions`.
- **Hướng xử lý**: Di chuyển các chuỗi số lượng sang khai báo `<plurals>` trong `strings.xml`.

---

#### [STR-03] Tài nguyên chuỗi và màu sắc không sử dụng (`UnusedResources`)
- **Vị trí**: 273 cảnh báo từ Android Lint, tập trung vào các màu sắc mẫu (`purple_*`, `teal_*`), các chuỗi string thử nghiệm cũ chưa dọn dẹp.
- **Hướng xử lý**: Chạy script dọn dẹp `python .agents/scripts/string_dedup.py` và loại bỏ các tài nguyên mồ côi.

---

## 4. MA TRẬN TUÂN THỦ HIỆN TRẠNG (13 FEATURES & 4 CORE MODULES)

Bảng đánh giá mức độ tuân thủ kỹ thuật và thiết kế v5.0.0 trên toàn bộ 17 phân hệ của ứng dụng:

| Phân hệ / Feature | Trạng thái Tier | Hiệu năng Compose | Coroutines & Lifecycle | Clean Architecture | Font Scale Resiliency | Dark Mode & Theme v5 | Trợ năng (a11y) | Đánh giá chung |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|---|
| **`feature/home`** | Tier 1 | Tốt (Đã tối ưu) | Tốt | Tốt | Tốt (Hỗ trợ 2 dòng amountHero) | Chuẩn v5 (Elevated Card, Không thẻ đen) | Tốt | **Đạt chuẩn (Sẵn sàng sản xuất)** |
| **`feature/stats`** | Tier 1 | **Vi phạm [PERF-01]** (Allocations trong Charts) | Tốt | Tốt | Cần bổ sung [CLIP-02] | Chuẩn v5 | Khá | **Cần tối ưu Draw allocations** |
| **`feature/assets`** | Tier 1 / 2 | Khá | Tốt | **Vi phạm [ARCH-02, ARCH-03]** (Monolith, IDE artifact) | **Vi phạm [CLIP-01]** (Fixed height nút bấm) | Khá | Khá | **Cần tái cấu trúc AddWalletScreen** |
| **`feature/transaction`** | Tier 1 / 2 | **Vi phạm [PERF-01, PERF-03]** | Tốt | **Vi phạm [ARCH-01]** (hiltViewModel con) | **Vi phạm [CLIP-01]** (Fixed height segment) | Vi phạm [COLOR-01] (Hex colors) | Khá | **Cần tách ViewModel & gỡ fixed height** |
| **`feature/utilities`** | Tier 1 | Tốt | Tốt | Tốt | Tốt | Chuẩn v5 | Tốt | **Đạt chuẩn** |
| **`feature/billsplit`** | Tier 2 | **Vi phạm [PERF-03, PERF-04]** | **Vi phạm [RES-01, RES-02]** (Nuốt exception, collect cũ) | Khá | **Vi phạm [CLIP-01]** (Height 68dp) | Cần migrate v5 ([COLOR-02]) | Khá | **Cần sửa P0 Exception, Flow & Format chuỗi [STR-01]** |
| **`feature/debt`** | Tier 2 | **Vi phạm [PERF-03]** (Thiếu @Immutable) | **Vi phạm [RES-02]** (Nuốt exception) | Khá | Khá | **Vi phạm [COLOR-02]** (Chưa migrate v5, thiếu iosCardShadow) | **Vi phạm [A11Y-01]** (contentDesc null) | **Cần sửa Nuốt Exception, a11y & migrate v5** |
| **`feature/subscription`** | Tier 2 | **Vi phạm [PERF-03]** | **Vi phạm [RES-01, RES-02]** | Khá | Khá | Khá | Khá | **Cần sửa Lifecycle Flow & Exception** |
| **`feature/category`** | Tier 2 | Khá | Tốt | Khá | Khá | Khá | Khá | **Vi phạm [STATE-01] (Flash of Empty)** |
| **`feature/auth`** | Tier 2 | Tốt | Tốt (Đã re-throw CancellationException dòng 122) | Tốt | Tốt | Khá | Tốt | **Đạt chuẩn (Không nuốt CancellationException)** |
| **`feature/backup`** | Tier 3 | Tốt | **Vi phạm [RES-01]** (collectAsState); tầng data vi phạm [RES-02] | Tốt | Tốt | Khá | Tốt | **Cần đổi collect Lifecycle & sửa DataExporter** |
| **`feature/onboarding`** | Tier 3 | Tốt | Tốt | Tốt | Khá | Khá | Khá | **Khá tốt** |
| **`feature/settings`** | Tier 3 | **Vi phạm [PERF-03]** | Tốt | Tốt | Khá | **Vi phạm [COLOR-01]** (Nhiều hex colors) | **Vi phạm [A11Y-01]** (API key toggle) | **Cần dọn mã màu Hex & nhãn a11y** |
| **`core/designsystem`** | Core | **Vi phạm [PERF-01]** (Fireflies) | Tốt | Tốt | Nền tảng tốt | Đầy đủ token v5 | Nền tảng tốt | **Cần tối ưu Fireflies Canvas loop** |
| **`core/navigation`** | Core | Tốt | Tốt | Tốt (Single Scaffold) | Tốt (Padding 120dp đáy) | Tốt | Tốt | **Đạt chuẩn kiến trúc điều hướng** |
| **`core/platform`** | Core | Tốt | Tốt | Tốt | Cần lưu ý widget layout | Tốt | Tốt | **Đạt chuẩn** |
| **`core/common`** | Core | Tốt | Tốt | Tốt | Tốt | Tốt | Tốt | **Đạt chuẩn** |

---

## 5. KẾ HOẠCH HÀNH ĐỘNG KHẮC PHỤC (REMEDIATION ROADMAP)

Để đưa toàn bộ ứng dụng NotePay đạt chuẩn chất lượng sản xuất và hoàn thiện 100% mục tiêu của Design System v5.0.0, lộ trình khắc phục được chia thành 4 giai đoạn ưu tiên:

```
[Giai đoạn P0: Khẩn cấp] -> [Giai đoạn P1: Chất lượng cao] -> [Giai đoạn P2: Chuẩn hóa] -> [Giai đoạn P3: Tinh chỉnh]
   (Crash & Coroutines)        (Hiệu năng & Trải nghiệm UI)        (M3 & Clean Architecture)      (Dọn dẹp mã & KTX)
```

### 5.1 Ưu tiên 0 (P0 - Critical): Khắc phục Lỗi Phá vỡ Hủy Coroutine Nền (4 lỗi)
- **Hành động 1.1**: Chuẩn hóa toàn bộ các khối `try-catch` trong `BillSplitViewModel.kt` (dòng 117, 141, 162, 195), `SubscriptionViewModel.kt` (dòng 151), `DebtDetailViewModel.kt` (dòng 90), và `DataExporter.kt` (dòng 53-57). Bổ sung `catch (e: CancellationException) { throw e }` để khôi phục cơ chế cooperative cancellation chuẩn của Kotlin Coroutines, loại bỏ triệt để nguy cơ rò rỉ tác vụ ngầm và lỗi logic khi hủy tác vụ hoặc hủy màn hình. *(Ghi nhận: `AuthViewModel.kt` đã có re-throw chuẩn ở dòng 122 nên không cần can thiệp)*.

### 5.2 Ưu tiên 1 (P1 - High): Tối ưu Hiệu năng Render, Trải nghiệm Trực quan & Rủi ro Chuỗi (24 lỗi)
- **Hành động 2.1**: Tối ưu giai đoạn vẽ Canvas của `FirefliesBackground.kt` và `StatsCharts.kt`. Sử dụng `remember` và `Modifier.drawWithCache` để loại bỏ 100% việc cấp phát `Brush`, `Stroke`, `Size` trong Draw scope ([PERF-01]).
- **Hành động 2.2**: Trì hoãn việc đọc State biến thiên liên tục sang Draw phase với `Modifier.graphicsLayer { }` tại `FirefliesBackground.kt` và `StatsCharts.kt` ([PERF-02]).
- **Hành động 2.3**: Gắn annotation `@Immutable` lên tất cả các lớp UI State model đang chứa danh sách: `BillSplitUiState`, `DebtUiState`, `AddWalletUiState`, `CategoryManagementUiState`, `SubscriptionUiState`, `TransactionDetailUiState`, `TransactionListUiState`, `AiSettingsUiState` ([PERF-03]).
- **Hành động 2.4**: Xóa bỏ thuộc tính `height(...)` cố định tại `VietQrConfigFullScreenDialog.kt:226`, `TransactionFormSections.kt:72`, và `AddWalletScreen.kt:454, 475, 495, 162`. Thay thế bằng `defaultMinSize(minHeight = ...)` để đảm bảo giao diện co giãn hoàn hảo ở Font Scale 1.3x - 2.0x ([CLIP-01]).
- **Hành động 2.5**: Bổ sung cơ chế tự động xếp dọc (Vertical Stacking) cho các ô chỉ số tài chính đôi khi `fontScale >= 1.45f` tại `VietQrConfigFullScreenDialog.kt:231` và `DebtSummaryCard.kt` ([CLIP-02]).
- **Hành động 2.6**: Bổ sung `contentDescription` cụ thể cho các nút IconButton tại `AiSettingsScreen.kt:556` và `CreateDebtBottomSheet.kt:260` ([A11Y-01]).
- **Hành động 2.7**: Bổ sung cờ `isLoading` vào `CategoryManagementUiState` để loại bỏ hiện tượng nháy màn hình trống khi khởi động ([STATE-01]).
- **Hành động 2.8**: Nâng trạng thái khởi tạo `TransactionDetailViewModel` khỏi Composable con `DetailPane` lên Route gốc `TransactionListRoute` ([ARCH-01]).
- **Hành động 2.9**: Thay thế `collectAsState()` bằng `collectAsStateWithLifecycle()` tại `BackupRestoreScreen.kt:72`, `BillSplitNavigation.kt:36`, và `SubscriptionNavigation.kt:34` ([RES-01]).
- **Hành động 2.10**: Đồng bộ hóa cấu trúc tham số `home_day_other` giữa Call Site (`HomeTransactionFeed.kt:75`) và tài nguyên XML (`values/strings.xml` và `values-en/strings.xml`) tránh cảnh báo `StringFormatCount` và triệt tiêu nguy cơ ngoại lệ `IllegalFormatConversionException` ([STR-01]).

### 5.3 Ưu tiên 2 (P2 - Medium): Chuẩn hóa Kiến trúc, Hệ Thống Thiết Kế & Bản Địa Hóa (39 lỗi)
- **Hành động 3.1**: Loại bỏ toàn bộ mã màu Hex hardcoded trong `AiSettingsScreen`, `AppearanceLanguageScreen`, `NotificationSettingsScreen`, và `CategoryGridPicker`, quy về sử dụng token `AppTheme.colors` ([COLOR-01]).
- **Hành động 3.2**: Nâng cấp các thành phần Tier 2 (`DebtItemCard.kt`, `BillSplitDebtorList.kt`) lên chuẩn Design System v5.0.0, bổ sung `Modifier.iosCardShadow(shape, elevated = false)` đặt trước `.clip()`/`Surface` ([COLOR-02]).
- **Hành động 3.3**: Sắp xếp lại vị trí tham số `modifier: Modifier = Modifier` tại 12 hàm Composable vi phạm chuẩn `ModifierParameter` ([M3-01]).
- **Hành động 3.4**: Thay thế API đo kích thước màn hình cũ `Configuration.screenWidthDp` bằng `LocalWindowInfo.current.containerSize` tại `StatsAdvisorSection.kt` và `WindowLayout.kt` ([M3-02]).
- **Hành động 3.5**: Chuyển đổi hộp thoại tạo danh mục trong `CategoryGridPicker.kt` từ `AlertDialog` sang `ModalBottomSheet` ([M3-03]).
- **Hành động 3.6**: Dọn dẹp artifact `_root_ide_package_` và phân rã tệp monolith `AddWalletScreen.kt` ([ARCH-02, ARCH-03]).
- **Hành động 3.7**: Bổ sung tham số ngày thanh toán cho `billsplit_paid_note_format` trong `values/strings.xml` thành `Đã trả nợ cho %1$s · %2$s` để đồng bộ đủ thông tin với bản tiếng Anh ([STR-01]).
- **Hành động 3.8**: Thay thế `mutableStateOf` bằng `mutableLongStateOf` tại `BillSplitCreateSheet.kt:169` và `CategoryGridPicker.kt:446` ([PERF-04]).
- **Hành động 3.9**: Mở rộng vùng tương tác chạm $\ge 48\text{dp}$ với `Modifier.minimumInteractiveComponentSize()` tại `CategoryGridPicker.kt:321` và các FilterChip ([A11Y-02]).

### 5.4 Ưu tiên 3 (P3 - Low): Dọn dẹp Tài nguyên và Nợ Kỹ thuật
- **Hành động 4.1**: Xóa bỏ 273 tài nguyên không sử dụng (`UnusedResources`) qua script dọn dẹp chuỗi tự động.
- **Hành động 4.2**: Chuyển đổi 52 vị trí ghép chuỗi số lượng sang thẻ `<plurals>` chuẩn.
- **Hành động 4.3**: Thay thế các lệnh gọi SharedPreferences và Uri.parse cũ sang Kotlin KTX (`preferences.edit { }`, `toUri()`).
- **Hành động 4.4**: Lập kế hoạch nâng cấp các thư viện trong `gradle/libs.versions.toml` lên phiên bản ổn định mới nhất.

---

## 6. PHƯƠNG PHÁP XÁC MINH & ĐỐI CHỨNG (VERIFICATION PROTOCOL)

Báo cáo kiểm toán tĩnh này có thể được đối chứng và tái kiểm tra độc lập thông qua các bước kỹ thuật sau:

### 6.1 Lệnh Thực Thi Kiểm Tra Tĩnh
Thực hiện các lệnh kiểm tra tĩnh chính thức của dự án trên môi trường Windows PowerShell:
```powershell
# 1. Chạy phân tích cú pháp Detekt
.\gradlew.bat detekt --no-daemon

# 2. Chạy Android Lint trên variant fullDebug
.\gradlew.bat :app:lintFullDebug --no-daemon

# 3. Chạy toàn bộ Unit Tests để xác nhận logic nghiệp vụ không bị ảnh hưởng
.\gradlew.bat :app:testFullDebugUnitTest --no-daemon
```

### 6.2 Báo Cáo Đối Chứng Từ Android Lint
Kết quả kiểm toán có thể được đối chiếu trực tiếp qua các báo cáo sinh ra tại thư mục build:
- Báo cáo định dạng văn bản: `app\build\reports\lint-results-fullDebug.txt`
- Báo cáo định dạng XML: `app\build\reports\lint-results-fullDebug.xml`
- Báo cáo định dạng trực quan HTML: `app\build\reports\lint-results-fullDebug.html`

### 6.3 Xác Nhận Tính Toàn Vẹn Mã Nguồn (Integrity Verification)
Để đảm bảo phiên làm việc tuân thủ tuyệt đối nguyên tắc không chỉnh sửa bất kỳ tệp code nào trong repository:
```powershell
git status
```
*Kết quả kỳ vọng*: Chỉ duy nhất tệp báo cáo `docs/AUDIT_REPORT.md` xuất hiện dưới dạng tệp mới chưa theo dõi (`untracked file`). Toàn bộ cây mã nguồn (`app/src/main/`, `app/src/test/`, `app/build.gradle.kts`, v.v.) hoàn toàn sạch sẽ, không có bất kỳ thay đổi nào (`working tree clean`).

---
*Báo cáo được lập bởi: Teamwork Static Audit Worker*  
*Đối chiếu kỹ thuật: Google Android Architecture Guidelines & NotePay DESIGN System v5.0.0*
