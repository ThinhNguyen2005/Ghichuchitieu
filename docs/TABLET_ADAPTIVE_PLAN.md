# Kế hoạch Adaptive Layout cho Tablet / Foldable

Tài liệu này ghi lại audit, quyết định kiến trúc và lộ trình P0–P5 để NotePay chạy
đúng trên tablet và thiết bị gập, trong khi giao diện điện thoại không đổi.

Trạng thái: **P0–P4 đã xong** · P5 còn lại (BillSplit list-detail tạm hoãn).

---

## 1. Quyết định kiến trúc (đã chốt)

| # | Vấn đề | Quyết định | Lý do |
|---|---|---|---|
| 1 | Navigation shell | **Option A — custom adaptive glass shell**: `<600dp` giữ `NotePayBottomBar`; `>=600dp` dùng `NotePayNavigationRail` là **pill dọc nổi, căn giữa chiều dọc, cao vừa đủ 4 tab (260dp)** | Giữ hiệu ứng Liquid Glass đặc trưng. Pill ngắn thay vì rail full-height để `Backdrop` không phải blur/lens toàn bộ chiều cao màn hình tablet (chi phí GPU tỉ lệ với diện tích bề mặt kính) |
| 2 | Pager gesture trên tablet | **Tắt `userScrollEnabled` khi `>=600dp`** | Tránh tranh chấp cử chỉ với `SwipeableTransactionItem` và `WalletsCompactCarousel`; chuyển tab bằng cách chạm rail |
| 3 | List-detail | **Tự dựng `Row` hai pane + `rememberSaveable`**, không dùng `ListDetailPaneScaffold`. Pilot Transactions, sau đó Debt; BillSplit hoãn | `ListDetailPaneScaffold` cần một `NavBackStackEntry` riêng cho mỗi pane, trong khi `transaction-detail/{id}` và `debt-detail/{id}` đã là route độc lập phục vụ deep-link/Home. Tự dựng `Row` cho phép giữ nguyên route cũ, không nhân bản UI, không đụng back stack |
| 4 | Manifest | **Khai báo tường minh `android:resizeableActivity="true"`** | Tránh phụ thuộc giá trị mặc định của từng OEM/targetSdk; là điều kiện để multi-window hoạt động |
| 5 | Tài liệu hoá | Chính file này | Theo dõi tiến độ P0–P5 |

---

## 2. Hiện trạng sau P0–P2

### 2.1 Lớp window (`ui/adaptive/WindowLayout.kt`)

```text
MainActivity.setContent
└── ProvideWindowLayout            ← đọc currentWindowAdaptiveInfo() DUY NHẤT 1 lần
    ├── LocalWindowLayout : WindowLayout (Compact | Medium | Expanded)
    ├── LocalContentGutter : Dp     (lề bù để nội dung ≤ 840dp)
    └── NotePayTheme → NotePayNavHost
```

- `WindowLayout` lấy từ `WindowSizeClass` (`WIDTH_DP_MEDIUM_LOWER_BOUND` = 600,
  `WIDTH_DP_EXPANDED_LOWER_BOUND` = 840). Các bậc Large/ExtraLarge gộp vào `Expanded`.
- Provider **không** đặt trong `NotePayTheme`: theme chỉ được cung cấp token hiển thị,
  còn thông tin cửa sổ là việc của entrypoint (AGENTS.md — layer isolation).
- Mặc định của `staticCompositionLocalOf` là `Compact`, nên `@Preview` và
  Robolectric test render y như điện thoại mà không cần dựng activity.
- `LocalContentGutter` = `max(0, (availableWidth − 840) / 2)`, trong đó
  `availableWidth = screenWidthDp − 96dp` khi có rail (phải trừ dải rail, nếu không
  nội dung bị đẩy lệch phải thay vì căn giữa phần còn lại). Bằng `0dp` ở Compact
  → padding trên điện thoại không đổi một pixel nào.

### 2.2 Giới hạn bề rộng nội dung (P1)

`widthIn(max = 840.dp)` được thể hiện bằng **lề bù hai bên** (`LocalContentGutter`)
thay vì bọc `widthIn` + căn giữa, vì:

- `LazyColumn` đã có `contentPadding` sẵn → cộng thẳng vào đó, không phải lồng thêm
  `Box` căn giữa (không đổi cấu trúc 4 file, không re-indent 100+ dòng).
- Nền full-bleed (hero band `surfaceSunken` của Home, `GradientTopAppBar` của Stats)
  giữ nguyên chất liệu, không xuất hiện "đường nối" giữa vùng nội dung và gutter.
- Kết quả hình học tương đương `widthIn`: bề rộng nội dung = `min(windowWidth, 840dp)`.

Đã áp dụng cho 4 tab: `HomeScreen`, `StatsDashboard`, `AssetsScreen`, `UtilitiesScreen`.

### 2.3 Navigation rail (P2)

`ui/navigation/NotePayNavigationRail.kt`:

- Pill 72dp × 260dp (4 × 56dp + 3 × 4dp + 2 × 12dp), `RoundedCornerShape(width/2)`,
  neo `Alignment.CenterStart`.
- Nền kính dùng đúng bộ effect của bottom bar (`vibrancy` + `blur(10dp)` + `lens(16dp)`),
  có nhánh fallback `surfaceContainer` khi thiết bị không hỗ trợ Liquid Glass.
- Capsule chọn tab trượt dọc bằng `animateDpAsState` + `graphicsLayer{ translationY }`
  → state chỉ đọc ở draw phase, không recompose mỗi frame.
- Item cao 56dp (đúng spec `NavigationRailItem`), icon 24dp, label 10sp, `Role.Tab`,
  `semantics { selected }`, haptic giống hệt bottom bar.
- FAB "Thêm nhanh" tách khỏi rail, neo `Alignment.BottomEnd` (rail không còn chỗ cho FAB).
- `MainTabPager` nhận `userScrollEnabled` và `contentStartInset = 96dp` để nội dung
  không nằm dưới pill.

File đã sửa ở P2: `NotePayNavHost.kt`, `MainTabPager.kt`, `NotePayNavigationRail.kt` (mới).
`NotePayBottomBar` giữ nguyên, `onTabSelected` nay dùng chung `navigateToRootTab`
(xoá 10 dòng lambda trùng lặp).

### 2.4 Dependencies

```toml
material3Adaptive = "1.3.0"   # androidx.compose.material3.adaptive:adaptive
window            = "1.5.1"   # androidx.window:window-core (hằng số breakpoint)
```

`adaptive-layout` (cho `ListDetailPaneScaffold`) đã khai báo sẵn nhưng **P4 không dùng**: bố cục
hai pane được dựng bằng `Row` + `weight`, vì mỗi pane sẽ cần một pane-scoped navigator riêng
và như vậy phải nhân bản route chi tiết đang phục vụ deep-link.
`adaptive-navigation` **chưa** thêm: phiên bản 2.10 của Navigation-Compose chưa cần
artifact này, và nó kéo theo phụ thuộc Navigation3.

> Có thể cân nhắc bỏ `adaptive-layout` ở P5 nếu không màn nào dùng tới.

> Lần build đầu tiên sau khi thêm dependency phải **bỏ `--offline`** để Gradle tải AAR
> về cache; các lần sau dùng `--offline` như bình thường.

### 2.5 Bố cục nội dung theo bề rộng (P3)

Ba tab chính đổi bố cục khi đủ rộng; Compact/Medium **không đổi gì** ngoài lề bù P1.

| Màn hình | Cơ chế | Ngưỡng chuyển |
|---|---|---|
| Home | `Row` hai pane: trái `weight(0.45f)` + `verticalScroll`, phải `weight(0.55f)` là `Surface(corner24)` chứa `LazyColumn` giao dịch | `WindowLayout.Expanded` |
| Stats | Hai `LazyColumn` song song trong `Row`: chart + bộ lọc (0.56f) / `supportingContent` (0.44f) | `WindowLayout.Expanded` |
| Assets | `LazyVerticalGrid(GridCells.Adaptive(320dp))`: header + donut + sổ nợ + tiêu đề ví chiếm `maxLineSpan`, mỗi `WalletAssetCard` một span | tự động khi đủ 2 × 320dp |

- Ở chế độ 2 cột, `HomeScreen` đổi nền gốc sang `surfaceSunken` để panel `surface` bo góc
  bên phải nổi lên đúng nghĩa; nền Compact vẫn là `surface` như cũ.
- `HomeHeroBlock` và `HomeInsightCard` được tách riêng để bố cục 1 cột và 2 cột dùng chung
  một điều kiện hiển thị duy nhất (không nhân bản logic ngân sách).
- `StatsDashboard` tách `StatsChartPane`; `StatsScreen` bỏ `Scaffold` lồng — nay chỉ còn
  `Column` + `GradientTopAppBar` (top bar tự lo inset status bar qua `windowInsets`).
- Bottom clearance giờ phụ thuộc chế độ điều hướng: `120dp` khi có bottom bar nổi,
  `24dp` khi có rail (rail không chiếm đáy màn hình). Đồng thời sửa luôn việc Stats
  trước đây không chừa chỗ cho thanh điều hướng nổi.
- Không đặt `height` cố định cho `WalletAssetCard`: grid lấy chiều cao hàng lớn nhất,
  thẻ ngắn hơn để trống bên dưới thay vì cắt chữ ở font scale 2.0×.
- `AssetsScreen` dùng `GridCells.Adaptive` thay vì `Fixed(2)` để bề rộng 600–660dp
  không bị ép thành hai cột chật; dưới ngưỡng đó grid tự rơi về 1 cột.

### 2.6 List-detail hai pane (P4)

Mẫu chung (Transactions và Debt dùng đúng cùng một khuôn):

| Thành phần | Transactions | Debt |
|---|---|---|
| Pane trái | `TransactionListPane` (tìm kiếm, lịch, bộ lọc, danh sách) | `DebtListPane` (tìm kiếm, tab, chip lọc, danh sách) |
| Pane phải | `TransactionDetailPane` → `TransactionDetailContent` | `DebtDetailPane` → `DebtDetailScreen(paneMode = true)` |
| Chọn dòng | `selectedTransactionId` (Long?) | `selectedDebtId` (Long?) |
| Tỉ lệ | 0.45 / 0.55 | 0.45 / 0.55 |

- Route chi tiết **giữ nguyên** (`transaction-detail/{id}`, `debt-detail/{id}`): ở
  Compact/Medium và khi nhảy từ Home vẫn `navController.navigate` như cũ. Ở Expanded
  danh sách mới đổi `onClick` sang "chọn tại chỗ", không đụng back stack.
- Lựa chọn dùng `rememberSaveable` → sống sót khi activity recreate vì resize cửa sổ
  (manifest chỉ khai báo `locale|layoutDirection`).
- `resolvedXxxId` kiểm tra lại id với danh sách đang hiển thị: bản ghi vừa bị xóa hoặc
  rơi ra khỏi bộ lọc sẽ tự trả pane phải về `DetailPaneEmptyState`.
- `BackHandler(enabled = isTwoPane && id != null)` đóng pane chi tiết trước khi thoát màn.
- Pane phải dùng `Surface(corner24, surfaceSunken)`; nội dung bên trong vẫn là card
  `surface` nên đọc đúng tầng.
- Dòng đang chọn được đánh dấu bằng `isSelected` (viền 2dp `primary`) — thêm vào
  `SwipeableTransactionItem` (đi xuyên xuống `TransactionItem`) và `DebtItemCard`.
- **ViewModel dùng lại, không nhân bản**: hai ViewModel chi tiết đọc id từ nav args;
  ở pane thì không có nav arg nên được bổ sung `select(id)`
  (`TransactionDetailViewModel`: huỷ job cũ rồi observe lại;
  `DebtDetailViewModel`: `MutableStateFlow` + `flatMapLatest`, `null` → `emptyFlow()`
  để không nháy trạng thái "không tìm thấy").
- Xóa ở pane phải **không** gọi `popBackStack()`: sự kiện `DebtDeleted` gọi `onBack`,
  mà pane truyền `onBack = { selectedDebtId = null }`; Transactions xóa qua swipe nên
  `onConfirm` chỉ cần kiểm tra id vừa xóa có trùng lựa chọn hay không.
- `GradientTopAppBar` thêm tham số `topBarColor` (mặc định `colorScheme.background`)
  để dải gradient trùng màu container thật khi thanh nằm trên `surfaceSunken`.
- Bỏ `Scaffold` lồng ở cả hai màn: top bar tự lo inset status bar, `Box` ngoài tô nền,
  FAB của Debt chuyển vào trong pane danh sách để không đè lên pane chi tiết.

**BillSplit hoãn có chủ đích**: danh sách của nó là hub tạo nhóm chia tiền
(`onDebtorClick(String)` — khóa theo **tên người**, không phải id) và "chi tiết" là bản
tổng hợp theo tên; muốn list-detail sạch cần đổi khoá dữ liệu trước, nên để P5.

---

## 3. Bản đồ màn hình

```text
NotePayNavHost (Scaffold gốc, insets, snackbar)
├── welcome
├── home  →  MainTabPager (HorizontalPager 4 trang)
│           0 Home · 1 Stats · 2 Assets · 3 Utilities        ← tab gốc
├── transactionGraph   list → detail/{id} → edit/{id}         ← pilot P4
├── debtGraph          management → detail/{id} · debtor/{name}
├── billSplitGraph     bill-split → debtor/{name}
├── settings: Ai · Appearance · Currency · Notification · Category · Backup · About
└── subscription
```

| Màn hình | Bố cục hiện tại | Compact | Medium (600–839) | Expanded (≥840) | Phase |
|---|---|---|---|---|---|
| Home | 1 `LazyColumn`: hero → carousel ví → giao dịch | giữ nguyên | lề bù, ≤840dp ✅ | 2 cột: trái hero + ví + ngân sách, phải giao dịch gần đây ✅ | P3 ✅ |
| Stats | `LazyColumn` + slot `supportingContent` | giữ nguyên | lề bù ✅ | **panel phụ bên phải**: biểu đồ trái / insight phải ✅ | P3 ✅ |
| Assets | grid thẻ ví + khối tổng quan | giữ nguyên (grid = 1 cột) | lề bù ✅ | `LazyVerticalGrid` adaptive, header/donut full span ✅ | P3 ✅ |
| Utilities | `LazyColumn` card nhóm cài đặt | giữ nguyên | lề bù ✅ | giữ 1 cột (≥840đã đủ đọc) | — |
| Transactions | list ↔ detail (route riêng + pane) | push/pop như cũ | lề bù ✅ | **2 pane** 0.45/0.55, chọn tại chỗ, `BackHandler` ✅ | P4 ✅ |
| Debt | list ↔ detail (route riêng + pane) | push/pop như cũ | lề bù ✅ | **2 pane** 0.45/0.55, FAB trong pane trái ✅ | P4 ✅ |
| BillSplit | hub tạo nhóm → "chi tiết" theo tên người | push/pop | giữ nguyên | giữ nguyên (khoá dữ liệu theo tên) | P5 |
| Form (add/edit transaction, wallet) | `ModalBottomSheet` / full-screen | giữ nguyên | `SideSheet`-style panel | panel cạnh nội dung | P5 |
| Settings (7 màn) | `Column`/`LazyColumn` | giữ nguyên | `widthIn(max = 720dp)` căn giữa | như Medium | P5 |
| Welcome / onboarding | wizard + footer | giữ nguyên | giữ nguyên | 2 pane: minh hoạ trái, bước phải | P5 |

---

## 4. Lộ trình

### P0 — Nền tảng window ✅
- [x] `material3-adaptive` + `window-core` vào `libs.versions.toml` và `build.gradle.kts`.
- [x] `ProvideWindowLayout` + `LocalWindowLayout` + `LocalContentGutter`.
- [x] `MainActivity` bọc provider.
- [x] `resizeableActivity="true"` tường minh.
- [x] `currentWindowAdaptiveInfo()` thay cho `LocalConfiguration` thủ công.
- Kiểm chứng: `:app:compileFullDebugKotlin` + `:app:testFullDebugUnitTest` xanh.

### P1 — Giới hạn bề rộng ✅
- [x] `LocalContentGutter` áp cho Home / Stats / Assets / Utilities.
- [x] Không đổi giao diện Compact (gutter = 0dp).
- Còn lại (đẩy sang P3): khung 2 cột cho Expanded — cần tách `LazyColumn` thành
  `Row` hai pane nên phải làm cùng lúc với việc tách item, không thể "chuẩn bị" nửa vời.

### P2 — Adaptive navigation shell ✅
- [x] `NotePayNavigationRail` (glass pill dọc, capsule trượt, haptic, a11y).
- [x] `NotePayNavHost` chọn bar/rail theo `LocalWindowLayout`.
- [x] `MainTabPager`: `userScrollEnabled = false` khi có rail, `contentStartInset = 96dp`.
- [x] FAB dời về góc dưới-phải ở chế độ rail.
- [x] Snackbar hạ bottom padding 104dp → 16dp khi có rail.

### P3 — Bố cục 2 cột cho nội dung ✅
- [x] Home 2 cột ở Expanded (hero + ví + ngân sách | giao dịch gần đây).
- [x] Stats: `supportingContent` ra panel phải thật sự (không còn render dưới chart).
- [x] Assets: `LazyVerticalGrid(GridCells.Adaptive(320dp))` cho danh sách thẻ ví.
- [x] Bỏ `Scaffold` lồng trong `StatsScreen` (AGENTS.md cấm).
- [x] Pill rail về đúng 4dp grid: 260dp (trước là 258dp).
- [x] Sửa công thức gutter: trừ `navigationRailInset` để nội dung căn giữa phần còn lại.
- Kiểm chứng: `:app:compileFullDebugKotlin` + `:app:testFullDebugUnitTest` xanh.
- Chưa kiểm được bằng mắt trên thiết bị thật (xem §6).

### P4 — List-detail hai pane ✅
- [x] Transactions: `Row` 0.45/0.55 ở Expanded; Compact/Medium giữ push/pop.
- [x] `selectedTransactionId` / `selectedDebtId` bằng `rememberSaveable`.
- [x] Highlight dòng đang chọn (`isSelected` ở `SwipeableTransactionItem` và `DebtItemCard`).
- [x] Pane phải: `DetailPaneEmptyState` khi chưa chọn / bản ghi không còn tồn tại.
- [x] `BackHandler` đóng pane trước khi thoát màn.
- [x] Xóa trong pane chỉ bỏ lựa chọn — không `popBackStack()`.
- [x] Bỏ `Scaffold` lồng ở `DebtManagementScreen`; `TransactionListScreen` vốn đã không có.
- [x] Giữ nguyên route độc lập `transaction-detail/{id}` và `debt-detail/{id}` cho deep-link/Home.
- [x] `select(id)` cho hai ViewModel chi tiết — dùng lại UI/VM, không nhân bản.
- [x] BillSplit hoãn sang P5 (lý do ở §2.6).
- Kiểm chứng: `:app:compileFullDebugKotlin` + `:app:testFullDebugUnitTest` xanh.
- Chưa kiểm được bằng mắt trên thiết bị thật (xem §6).

### P5 — Form & settings
- `AdaptiveSettingsContainer` (một wrapper `widthIn(720dp)` dùng cho 7 màn cài đặt)
  thay vì sửa từng file.
- Form: `SideSheet` ở `>=600dp`.
- Welcome/onboarding: 2 pane ở `>=840dp`.
- Kiểm thử font scale 1.3×–2.0× trên toàn bộ màn hình đã đổi.

---

## 5. Rủi ro & điểm cần kiểm

- **Font scale 1.3×–2.0×**: mọi item trong grid/rail phải dùng `minHeight` +
  `TextOverflow.Ellipsis`, tuyệt đối không `height` cố định cho vùng có chữ.
- **Độ dài dòng**: pane rộng hơn ~720dp cần `widthIn` cho khối text, nếu không
  readability giảm trên tablet 12.9".
- **Foldable**: `windowPosture` (đã có trong `WindowAdaptiveInfo`) cần cho case
  tabletop — tới P5 mới dùng, hiện `ProvideWindowLayout` chưa khai thác.
- **State khi resize**: activity recreate → mọi lựa chọn pane phải `rememberSaveable`.
- **`Scaffold` lồng nhau**: đã dọn xong ở P3 (`StatsScreen`) và P4
  (`DebtManagementScreen`). `TransactionListScreen` vốn chỉ có `Box` + `Column`.
  Các màn còn `Scaffold` (`TransactionDetailScreen`, `BillSplitScreen`, welcome, settings)
  không đồng thời render hai pane nên giữ nguyên.
- **Chi phí GPU của kính**: pill 260dp cao đã giảm mạnh diện tích blur so với rail
  full-height, nhưng vẫn cần đo lại trên tablet thật (profile GPU rendering) trước P4.
- **Haptic/scale feedback** của rail hiện chỉ đổi màu + capsule; nếu muốn "nảy" như
  bottom bar có thể tái dùng `DampedDragAnimation` (kéo dọc để đổi tab) ở P4.

---

## 6. Cách kiểm chứng

```powershell
# Build + unit test (sau lần đầu tiên có thể thêm --offline)
./gradlew.bat :app:compileFullDebugKotlin
./gradlew.bat :app:testFullDebugUnitTest

# Kiểm tra bằng mắt (resizable emulator, Android 15+)
# 1. Điện thoại dọc 411dp  → bottom bar, không gutter, pager vuốt được, tab 3 màn hình y như cũ
# 2. Tablet dọc 800dp      → rail, gutter = 0, không vuốt ngang, Assets vẫn 1 cột
# 3. Tablet ngang 1280dp   → rail, gutter = 172dp, nội dung ≤ 840dp, Home/Stats 2 cột
#                            (172 = (1280 − 96 rail − 840) / 2)
# 4. Kéo resize cửa sổ     → tab đang chọn và scroll position được giữ, grid tự đổi số cột
# 5. Font scale 2.0×       → nhãn rail và thẻ ví không bị cắt chữ
# 6. Tablet ngang 1280dp   → Giao dịch/Khoản nợ: chạm một dòng thì pane phải hiện chi tiết,
#                            dòng đó có viền primary; bấm Back lần 1 chỉ đóng pane;
#                            xóa bản ghi đang mở thì pane về trạng thái chờ, KHÔNG văng khỏi màn
# 7. Co cửa sổ về 800dp     → lựa chọn pane bị bỏ, danh sách chiếm full width, chạm lại thì
#                            điều hướng sang route chi tiết như điện thoại
```

Nếu gặp lỗi `PersistentMap` / `Unresolved reference` hàng loạt với class của chính
project sau khi đổi cấu hình build, đó là Kotlin incremental cache hỏng — xoá
`app/build/kotlin` rồi build lại (đã xảy ra một lần trong đợt này).
