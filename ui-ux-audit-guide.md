# Quy Chuẩn Kiểm Tra & Nâng Cấp UI/UX NotePay (`ui-ux-audit-guide.md`)

Tài liệu này là kịch bản kiểm tra chuẩn (**Audit Checklist & Actionable Prompt**) dành cho mã nguồn **Jetpack Compose + Material Design 3 (`androidx.compose.material3:1.4.0`)**, tuân thủ hệ thống thiết kế **NotePay v5.0.0 — Luminous iOS & Atmospheric Canvas (`DESIGN.md`)**, chuẩn **Trợ năng (WCAG AA / TalkBack)** và **Motion Design**.

> **Nguyên tắc thực thi:** Khi kiểm tra hoặc chuyển đổi bất kỳ màn hình nào sang v5.0, rà soát tuần tự cả **6 hạng mục** dưới đây (bao gồm bảng theo dõi toàn app ở Hạng mục 6). Không bỏ qua `Dialog` con, `BottomSheet` con, hoặc các trạng thái `Loading / Empty / Error / Offline`.

---

## Hạng mục 1: Kiểm tra Luồng tương tác & Nút chức năng trùng lặp (Flow & Action Redundancy)

### Checklist kiểm tra
- [ ] **Quét trùng lặp hành động trên cùng màn hình:** Liệt kê toàn bộ `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton`, `FloatingActionButton`, `DropdownMenuItem`, `ListItem`, thẻ `.clickable` để phát hiện các nút dẫn tới cùng một đích hoặc thực hiện cùng một hành động.
- [ ] **Đúng 1 Hành động Chính (Single Primary Action):**
  - Mỗi màn hình chỉ giữ tối đa **1 nút Primary nổi bật nhất** (`Button` màu `primary` hoặc `FloatingAddButton` ở 4 tab gốc).
  - Các hành động phụ hạ cấp xuống `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton` tròn phủ sương `48.dp` hoặc gom vào `ModalBottomSheet`.
  - Theo `DESIGN.md` v5: **không dùng thẻ nền đen (`ink`) cho Hero Card** — mỗi màn dùng tối đa **1 Luminous iOS Hero Card** (`HomeHeroCard*Top -> Bottom` + `iosCardShadow(..., elevated = true)`) làm tâm điểm dữ liệu.
- [ ] **Phát hiện nút "chết" hoặc dẫn vào ngõ cụt:** Không bật sẵn nút dẫn tới một form/sheet mà điều kiện tiên quyết chưa thỏa (ví dụ: nút chuyển tiền giữa các ví khi chỉ có 1 ví).
- [ ] **Độ dài luồng & Tham số thừa:** Kiểm tra xem thao tác phổ biến có bị kéo dài qua nhiều lớp không cần thiết hay không; loại bỏ tham số/trạng thái thừa.
- [ ] **Đầy đủ 4 trạng thái dữ liệu:** `Loading`, `Empty` (`EmptyStateWithAction`), `Error` (thông báo an toàn + nút Thử lại), và `Content`.

---

## Hạng mục 2: Chuẩn hóa Material 3 & Các Composable được Google khuyến nghị

### Checklist kiểm tra
- [ ] **Thay thế UI tự dựng thủ công bằng Component chính thức của `androidx.compose.material3`:**

| Mẫu tự dựng thường gặp (Cần thay) | Composable Material 3 khuyến nghị | Ghi chú áp dụng trong NotePay |
|---|---|---|
| `Row` + `Box` + `clickable` làm dòng danh sách / cài đặt | `ListItem` (`headlineContent`, `supportingContent`, `leadingContent`, `trailingContent`) | Với M3 `1.4.0`, dùng `headlineContent = { ... }`; có thể dùng `SettingListItem` có sẵn hoặc `ListItem` chuẩn. |
| `Row` + `Surface` + `selectable` làm bộ lọc / tab 2–4 lựa chọn | `SingleChoiceSegmentedButtonRow` + `SegmentedButton` | Áp dụng `heightIn(min = 48.dp)` và bảng màu `primary`/`onPrimary` để đạt tương phản WCAG $\ge 3:1$. |
| `Surface` + `Row` tự dựng làm chip chọn bộ lọc / ví | `FilterChip` / `AssistChip` | Đặt `heightIn(min = 48.dp)` hoặc `defaultMinSize(minHeight = 48.dp)`. |
| `Box` + `Text` tiêu đề màn hình tự căn lề / bóng đổ | `TopAppBar` / `GradientTopAppBar` | Dùng `Scaffold` gốc ở `NotePayNavHost`; các tab chính không lồng `Scaffold` con. |
| `Box` + `clickable` bọc `OutlinedTextField(enabled = false)` để chọn item | `ExposedDropdownMenuBox` hoặc `ListItem` / `Surface(onClick = ...)` | Tránh bọc `TextField` bị `enabled = false` làm TalkBack đọc nhầm là bị khóa. |
| Thanh thông báo / Toast tự dựng | `SnackbarHost` qua `UiFeedback` của `NotePayNavHost` | Giữ nhất quán khoảng chừa đáy tránh thanh điều hướng nổi. |

- [ ] **Quy tắc chuyển đổi `Dialog` sang `ModalBottomSheet`:**
  - **Bắt buộc chuyển sang `ModalBottomSheet`** (kết hợp `rememberModalBottomSheetState(skipPartiallyExpanded = true)` hoặc `BottomSheetGlass` của dự án) đối với mọi `Dialog` / `AlertDialog` chứa:
    - Form nhập liệu nhiều trường;
    - Danh sách lựa chọn dài (ví, danh mục, giao dịch gần đây, tiền tệ);
    - Nội dung chi tiết cần cuộn.
  - **Chỉ giữ lại `AlertDialog`** cho hộp thoại xác nhận ngắn mang tính cảnh báo nghiêm trọng hoặc hủy diệt (ví dụ: `ConfirmDeleteDialog` xóa giao dịch, xác nhận đăng xuất, xác nhận bỏ thay đổi chưa lưu).
  - Không dùng `Dialog(usePlatformDefaultWidth = false)` để giả dạng `BottomSheet` (hoặc đặt tên `*BottomSheet` nhưng bên trong lại là `Dialog` toàn màn hình).
- [ ] **Kiểm tra Design Token (`DESIGN.md` v5, `AppTheme`, `IosShadowModifier`, `AppAtmosphericBackground`):**
  - **Cấm hardcode mã màu Hex** (`Color(0xFF...)`) hoặc `Color.White` / `Color.Black` trực tiếp trong UI; luôn dùng `AppTheme.colors`, token đặt tên trong `Color.kt`, hoặc `MaterialTheme.colorScheme`.
  - **Cấm hardcode kích thước chữ `< 12.sp`** và góc bo ngoài thang `AppShapes` (`12 / 16 / 20 / 24 / 28 / capsule / circle`) hoặc khoảng cách ngoài lưới `4.dp`.
  - **Không dùng thẻ đen cho Hero Card:** Dùng Luminous iOS Hero Surface (`HomeHeroCardLightTop/Bottom/Rim` hoặc `HomeHeroCardDarkTop/Bottom/Rim`) + `Modifier.iosCardShadow(shape, elevated = true)` + 2 ô chỉ số lồng `corner16`.
  - **Nền & Bóng đổ v5:** Mọi màn chính/phụ đặt trên `AppAtmosphericBackground` (`forceSoftBlurOnPhoto = true` trên màn dữ liệu dày). Thẻ nội dung & pill nổi đặt `Modifier.iosCardShadow(shape, elevated = false)` **trước** `.clip`/`.background`/`Surface` + viền `0.5.dp hairline`.

---

## Hạng mục 3: Kiểm tra Trợ năng (Accessibility - a11y)

### Checklist kiểm tra
- [ ] **Vùng chạm tối thiểu `48.dp x 48.dp`:**
  - Mọi thành phần tương tác (`IconButton`, `FilterChip`, `SegmentedButton`, chip chọn ví, thẻ bấm mở rộng, hàng chọn trong sheet) phải có `Modifier.minimumInteractiveComponentSize()`, `Modifier.defaultMinSize(minHeight = 48.dp)`, hoặc `Modifier.heightIn(min = 48.dp)`.
  - Cảnh báo: `FilterChip` và `SegmentedButton` mặc định của M3 có chiều cao `32.dp` / `40.dp` — bắt buộc nâng tối thiểu `48.dp`.
- [ ] **`contentDescription` chuẩn xác:**
  - Mọi `Icon` và `Image` / `AsyncImage` đứng một mình hoặc mang ý nghĩa chức năng bắt buộc có `contentDescription = stringResource(...)`.
  - Mọi `Icon` chỉ mang tính trang trí (đi kèm nhãn `Text` mô tả cùng ý nghĩa ngay bên cạnh) phải truyền `contentDescription = null`.
  - Không chỉ dùng màu sắc để phân biệt Thu/Chi hay trạng thái an toàn/cảnh báo/vượt hạn mức: phải đi kèm **Icon + Chữ + Màu**.
- [ ] **Ngữ nghĩa TalkBack (`semantics`, `Role`, `stateDescription`):**
  - Gắn `Modifier.semantics(mergeDescendants = true) {}` cho các thẻ thông tin hoặc dòng danh sách gồm nhiều phần tử con (ví dụ: cụm `Icon` + `Text` số tiền, thẻ tóm tắt, cột biểu đồ) để TalkBack đọc liền mạch trong 1 lần chạm.
  - Gắn đúng `role` cho thành phần tương tác tùy chỉnh: `Role.Button` (mở rộng/thu gọn, hành động), `Role.RadioButton` (chọn 1 ví/1 mục), `Role.Switch` (bật/tắt), `Role.Tab` (chỉ khi đúng ngữ nghĩa tab).
  - Bổ sung `stateDescription` (ví dụ: *"Đang mở"* / *"Đang đóng"*) cho mọi card/accordion có trạng thái đóng/mở.
  - Bổ sung `onClickLabel` cho các thẻ tổng hợp có hành động khi chạm (ví dụ: *"Xem giao dịch của ví"*).
  - Biểu đồ `Canvas` tương tác phải có `semantics { contentDescription = ... }` hoặc cung cấp đầy đủ phần tử con truy cập được tương đương cho TalkBack.
- [ ] **Tương phản & Co giãn phông chữ 200% (`fontScale = 1.3f..2.0f`):**
  - Độ tương phản đạt $\ge 4.5:1$ cho chữ thường, $\ge 3:1$ cho chữ lớn và thành phần giao diện/trạng thái đang chọn ở cả **Light mode** và **Dark mode**.
  - Không đặt `.height(...)` cố định lên container chứa chữ; dùng `.heightIn(min = ...)` hoặc `.defaultMinSize(minHeight = ...)` kèm `maxLines` + `TextOverflow.Ellipsis` và `Modifier.weight(1f, fill = false)` khi xếp ngang.

---

## Hạng mục 4: Hiệu ứng Chuyển động khi Tải trang & Điều hướng (Entrance & Transition Motion)

### Checklist kiểm tra
- [ ] **Tôn trọng cài đặt giảm chuyển động hệ thống:** Mọi hiệu ứng chuyển động trang và chuyển trạng thái phải đọc `rememberNotePayReducedMotion()` (tắt hoặc chuyển sang `snap()` / `EnterTransition.None` khi người dùng bật Reduce Motion).
- [ ] **Chuyển trạng thái `Loading -> Empty / Error / Content` mượt mà:**
  - Tránh dùng `if (state.isLoading) { CircularProgressIndicator(); return@Box }` gây giật khung hình đột ngột.
  - Bọc khối trạng thái bằng `AnimatedContent` hoặc `Crossfade` sử dụng easing chuẩn Material 3:
    - `EmphasizedDecelerate`: `CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)` cho xuất hiện (`fadeIn` + `slideInVertically`).
    - `EmphasizedAccelerate`: `CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)` cho biến mất (`fadeOut`).
  - Đối với danh sách giao dịch / thẻ tài sản, ưu tiên Skeleton/Shimmer hoặc `AnimatedContent` chuyển êm sang nội dung thật.
- [ ] **Xuất hiện tuần tự (Staggered Entrance) & Biến đổi danh sách:**
  - Khối Hero, thẻ Insight và danh sách trên các tab chính nên có hiệu ứng vào trang nhẹ nhàng (`fadeIn` + `slideInVertically` có độ trễ ngắn theo nhóm logic, chỉ chạy 1 lần khi mở/chuyển trạng thái, không chạy lại khi cuộn).
  - Các item trong `LazyColumn` / `LazyVerticalGrid` phải gắn `Modifier.animateItem()` khi danh sách có thêm/xóa/sắp xếp lại.
- [ ] **Thay đổi kích thước động & Số liệu:**
  - Mọi thẻ có thể mở rộng/thu gọn (`SmartInsightsCard`, `LocalAdvisorCard`, `AiAdviceCard`, `WalletAssetCard`) phải gắn `Modifier.animateContentSize()`.
  - Số dư/tổng tiền trên Hero Card (`HeroBalanceCard`, `AssetsInkHeroCard`) dùng `AnimatedContent` khi giá trị thay đổi; các thẻ có thể ẩn/đóng bọc trong `AnimatedVisibility`.
  - Cân nhắc `SharedTransitionLayout` (đã cung cấp sẵn ở `NotePayNavHost`) khi liên kết phần tử từ danh sách sang màn chi tiết (nếu cùng đối tượng khái niệm).

### Mẫu tham khảo chuẩn (Motion Entrance & State Transition)
```kotlin
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

@Composable
fun StaggeredSection(
    visible: Boolean,
    index: Int,
    reducedMotion: Boolean,
    content: @Composable () -> Unit,
) {
    if (reducedMotion) {
        if (visible) content()
        return
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = 260,
                delayMillis = (index * 45).coerceAtMost(180),
                easing = EmphasizedDecelerate,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = 280,
                delayMillis = (index * 45).coerceAtMost(180),
                easing = EmphasizedDecelerate,
            ),
            initialOffsetY = { it / 10 },
        ),
        exit = fadeOut(animationSpec = tween(140)),
    ) {
        content()
    }
}
```

---

## Hạng mục 5: Phản hồi Vi mô & Báo lỗi (Micro-interactions & Error Feedback)

### Checklist kiểm tra
- [ ] **Xác thực biểu mẫu & Báo lỗi trực quan:**
  - Mọi `OutlinedTextField` có điều kiện hợp lệ phải truyền `isError` và `supportingText` bọc trong `AnimatedVisibility(visible = hasError)` để thông báo lỗi trượt ra mượt mà, không làm nhảy giật layout.
  - Không chặn thao tác im lặng: Nếu người dùng bấm lưu hoặc nhập giá trị sai (ví dụ `0`, âm, vượt số dư, thiếu tên), phải có thông báo rõ ràng ngay tại trường bị lỗi.
- [ ] **Hiệu ứng Rung lắc ngang (Horizontal Shake Animation) khi nhập sai / lỗi:**
  - Dùng `Animatable(0f)` + `keyframes` dịch chuyển theo trục X qua `Modifier.graphicsLayer { translationX = shakeOffset.value }` (chỉ tác động render layer, không gây re-layout).
  - Kích hoạt đồng thời cùng `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` (hoặc `Reject` trên Android 11+ nếu có kiểm tra cài đặt `hapticFeedbackEnabled`).
- [ ] **Phản hồi xúc giác (Haptic Feedback) nhất quán:**
  - Mọi thao tác chạm quan trọng (lưu, xóa, kéo vuốt qua ngưỡng, chọn chip quan trọng, báo lỗi) nên kiểm tra cờ cài đặt `hapticFeedbackEnabled` từ preferences trước khi phát rung, thay vì gọi trực tiếp `LocalView.performHapticFeedback` rải rác không kiểm soát.
- [ ] **Phản hồi nhấn nút (Press Micro-interaction):**
  - Nút tiêu chuẩn giữ ripple mặc định của Material 3; nút hành động nổi/chính có thể thêm scale bounce nhẹ (`0.96f` khi nhấn giữ qua `interactionSource.collectIsPressedAsState()` + `graphicsLayer`, tôn trọng `reducedMotion`).

### Mẫu tham khảo chuẩn (Horizontal Shake + Haptic + Animated Error Text)
```kotlin
@Composable
fun rememberShakeController(): Animatable<Float, AnimationVector1D> =
    remember { Animatable(0f) }

suspend fun Animatable<Float, AnimationVector1D>.triggerErrorShake(
    reducedMotion: Boolean,
) {
    if (reducedMotion) return
    snapTo(0f)
    animateTo(
        targetValue = 0f,
        animationSpec = keyframes {
            durationMillis = 360
            -12f at 60
            12f at 120
            -8f at 180
            8f at 240
            -4f at 300
            0f at 360
        },
    )
}

// Áp dụng trên OutlinedTextField:
// val haptic = LocalHapticFeedback.current
// LaunchedEffect(errorTriggerCount) {
//     if (errorTriggerCount > 0) {
//         if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
//         shakeOffset.triggerErrorShake(reducedMotion)
//     }
// }
// OutlinedTextField(
//     modifier = Modifier
//         .fillMaxWidth()
//         .graphicsLayer { translationX = shakeOffset.value },
//     isError = isError,
//     supportingText = {
//         AnimatedVisibility(visible = isError) {
//             Text(text = errorMessage)
//         }
//     },
//     ...
// )
```

---

## Hạng mục 6: Checklist Áp Dụng & Kiểm Tra Giao Diện Chuẩn iOS Toàn App (Full-App v5.0 Tracking)

### Cổng kiểm tra 8 điểm cho mỗi màn hình trước khi đánh dấu `[x]`
1. **Nền Atmospheric:** Bọc gốc màn hình bằng `AppAtmosphericBackground` (bật `forceSoftBlurOnPhoto = true` nếu có biểu đồ, danh sách dày hoặc bàn phím).
2. **Hero Card sáng chuẩn iOS (Không thẻ đen):** Mọi thẻ tổng quan dùng gradient `HomeHeroCard*Top -> Bottom` + viền `1.dp` `HomeHeroCard*Rim` + `iosCardShadow(corner28, elevated = true)` + ô chỉ số lồng `corner16` tự xếp dọc khi `fontScale >= 1.45f`.
3. **Thẻ nội dung & Control Pills nổi:** Gắn `Modifier.iosCardShadow(shape, elevated = false)` **trước** `.clip`/`Surface` và viền `0.5.dp` `hairline`. Nút header tròn/pill phủ sương `48.dp`.
4. **Vùng chạm `>= 48.dp`:** Toàn bộ nút, chip, hàng chọn, mũi tên điều hướng đạt `minHeight = 48.dp`.
5. **Chữ & Co giãn 200%:** Không có chữ `< 12.sp`, không gán `.height(...)` cố định cho container chứa text; tiêu đề ngắn dùng `displaySerif` + `heading()`, số liệu dùng Inter `tnum`.
6. **TalkBack & Trạng thái:** Đủ `contentDescription` / `mergeDescendants` / `Role` / `stateDescription` và xử lý đủ 4 trạng thái `Loading / Empty / Error / Content`.
7. **Dialog -> ModalBottomSheet:** Mọi form/picker/nội dung cuộn dùng `ModalBottomSheet` (`sheetTop`, scrim 45%, `shakeOnError`, `AnimatedVisibility`).
8. **Tương phản Light & Dark + Song ngữ:** Đạt `>= 4.5:1` cả 2 theme, không sót text hardcode ngoài `values/strings.xml` & `values-en/strings.xml`.

---

### Bảng tiến độ áp dụng toàn bộ 19 màn hình NotePay

#### Tier 1 — 4 Tab Gốc & Luồng Nhập Giao Dịch Chính (Đã áp dụng chuẩn v5.0)
- [x] **Trang chủ (`Home`)** — [HomeScreen.kt](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/feature/home/HomeScreen.kt), `HomeHeroSection`, `HeroBalanceCard`, `SmartInsightsCard`, `HomeTransactionFeed`, `HomeCustomizationSheet`
- [x] **Báo cáo (`Stats`)** — [StatsScreen.kt](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/feature/stats/main/StatsScreen.kt), `StatsDashboard`, `StatsCategoryBreakdownSection`, `LongTermTrendSection`, `StatsAdvisorSection`
- [x] **Tài sản (`Assets`)** — [AssetsScreen.kt](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/feature/assets/main/AssetsScreen.kt), `AssetsCards`, `DebtSummaryCard`
- [x] **Thêm / Sửa giao dịch (`AddTransaction` / `EditTransaction`)** — [TransactionFormScreen.kt](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/feature/transaction/form/TransactionFormScreen.kt), `CategoryGridPicker`, `TransactionInputComponents`, `TransactionFormSections`, `TransactionCustomKeypad` (luồng Add 2 bước; kiểm chứng code/unit test/Lint/Detekt trên `5739aae`, chưa chạy test thiết bị thật)
- [x] **Thêm / Tiện ích (`Utilities`)** — [UtilitiesScreen.kt](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/feature/utilities/UtilitiesScreen.kt)

#### Tier 2 — Luồng Nghiệp Vụ Tài Chính (Triển khai tiếp theo theo từng đợt)
- [ ] **Danh sách giao dịch (`TransactionList`)** — `feature/transaction/list/TransactionListScreen.kt`: thêm `AppAtmosphericBackground`, header frosted pills, `iosCardShadow` cho thanh tìm kiếm/filter chip/nhóm thẻ ngày (`>= 48.dp`, xóa `sp < 12`).
- [ ] **Chi tiết giao dịch (`TransactionDetail`)** — `feature/transaction/detail/TransactionDetailScreen.kt`: thay header phẳng bằng iOS frosted bar, đưa thẻ tóm tắt số tiền thành Luminous Card + `iosCardShadow`, kiểm tra nút Sửa/Xóa & sheet xác nhận.
- [ ] **Thêm / Sửa ví (`AddWallet` / `EditWallet`)** — `feature/assets/main/sheet/AddWalletScreen.kt`: thêm `AppAtmosphericBackground`, gom nhóm trường vào `Surface` `corner20` + `iosCardShadow`, kiểm tra `isError` + `shakeOnError`.
- [ ] **Thành viên ví chung (`SharedWalletMembers`)** — `feature/assets/shared/SharedWalletMembersScreen.kt`: thêm `AppAtmosphericBackground`, `iosCardShadow` cho thẻ thành viên/mã mời, đảm bảo toàn bộ action `>= 48.dp`.
- [ ] **Quản lý sổ nợ (`DebtManagement`)** — `feature/debt/management/DebtManagementScreen.kt` (*lưu ý:* `DebtSummaryCard` hero đã chuyển sang chuẩn Luminous v5 `[x]`): áp dụng `AppAtmosphericBackground`, header frosted pills, `iosCardShadow` cho thẻ khoản nợ và bộ lọc.
- [ ] **Chi tiết khoản nợ (`DebtDetail`)** — `feature/debt/detail/DebtDetailScreen.kt`: thay thẻ đen/phẳng bằng Luminous Hero Card + lịch sử trả nợ nhóm `corner20` có `iosCardShadow` & vạch chia thụt lề.
- [ ] **Chia hóa đơn (`BillSplit`)** — `feature/billsplit/main/BillSplitScreen.kt`: thêm `AppAtmosphericBackground`, `iosCardShadow` cho từng nhóm chia tiền và thanh tiến độ thanh toán.
- [ ] **Chi tiết người nợ (`DebtorDetail`)** — `feature/billsplit/main/detail/DebtorDetailScreen.kt`: nâng cấp thẻ tổng công nợ + thẻ VietQR (`TransferDetailsCopyCard`) theo v5 + `iosCardShadow`.
- [ ] **Gói đăng ký định kỳ (`Subscription`)** — `feature/subscription/main/SubscriptionScreen.kt` & `AddSubscriptionBottomSheet.kt`: nền `AppAtmosphericBackground`, `iosCardShadow` cho thẻ tóm tắt & item định kỳ, rà soát `>= 48.dp`.
- [ ] **Quản lý danh mục (`CategoryManagement`)** — `feature/category/CategoryManagementScreen.kt`: nền `AppAtmosphericBackground`, tách 2 nhóm Chi tiêu / Thu nhập dạng grouped `corner20` + `iosCardShadow` + sheet tạo/sửa danh mục.

#### Tier 3 — Cài Đặt Hệ Thống & Màn Hình Chào
- [ ] **Giao diện & Ngôn ngữ (`AppearanceLanguage`)** — `feature/settings/appearance/AppearanceLanguageScreen.kt`: thêm mục lối tắt mở tùy chỉnh nền Trang chủ hoặc đồng bộ thẻ grouped `corner20` + `iosCardShadow`, `displaySerif` header, `AppAtmosphericBackground`.
- [ ] **Cài đặt tiền tệ & Quy đổi (`CurrencySettings` / `CurrencyConverter`)** — `feature/settings/currency/CurrencySettingsScreen.kt` & `CurrencyConverterScreen.kt`: chuyển `ConverterInkResultCard` từ thẻ đen sang Luminous Hero Card, thêm `iosCardShadow` & `AppAtmosphericBackground`.
- [ ] **Cài đặt AI (`AiSettings`)** — `feature/settings/ai/AiSettingsScreen.kt`: đồng bộ `AiEngineHeaderCard` sang Luminous/Surface có `iosCardShadow`, `AppAtmosphericBackground`, kiểm tra `>= 48.dp` trên các hàng setting.
- [ ] **Cài đặt thông báo (`NotificationSettings`) & Sao lưu khôi phục (`BackupRestore`) & Giới thiệu (`About`) & Màn hình chào (`Onboarding`)** — `feature/settings/notification/NotificationSettingsScreen.kt`, `feature/backup/BackupRestoreScreen.kt`, `feature/settings/AboutScreen.kt`, `feature/onboarding/OnboardingScreen.kt`: áp dụng `AppAtmosphericBackground`, grouped cards `iosCardShadow`, `displaySerif` header và kiểm tra tương phản Light/Dark + `fontScale 2.0x`.

---

### Phản hồi thao tác và chuyển động

| Điểm kiểm tra | Hành vi trong mã |
| --- | --- |
| Điều hướng cấp app | `NotePayNavHost` dùng chuyển tiếp nhẹ và bỏ hiệu ứng khi bật giảm chuyển động. Menu **+** chờ sheet đóng rồi mới mở đích đã chọn. |
| Trang chủ, Báo cáo, Tài sản, Tiện ích và các màn nghiệp vụ có `onFeedback` | Kết quả thao tác qua host cấp app. Thông báo có hành động dùng hàng đợi Material để người dùng còn thời gian bấm **Xem** hoặc **Hoàn tác**. |
| Thêm và sửa giao dịch, thêm và sửa ví | Kết quả lưu hiện ở màn gọi sau khi màn đó tiếp tục hoạt động. **Xem** từ giao dịch dùng đúng ID đã lưu. Lỗi giữ người dùng ở form. |
| Chi tiết giao dịch ví chung | Trạng thái chờ đồng bộ và lỗi đồng bộ nằm cạnh giao dịch, gồm cả pane chi tiết trên màn rộng. **Thử đồng bộ lại** dùng cùng ID. |
| Nhập giọng nói/văn bản, chia hóa đơn, đăng nhập, sao lưu và giao diện | Phản hồi dùng cùng mẫu icon, chữ và hành động với host cấp app. Tác vụ đang mở sheet hiển thị lỗi trong sheet. |
| Tiến trình, nội dung trống và lỗi kiểm tra dữ liệu | Hiển thị tại nút, nội dung hoặc trường liên quan. Dùng thanh thông báo cho kết quả của thao tác người dùng vừa thực hiện. |

Nghiệm thu trên thiết bị gồm bấm nhanh hai lần, Back, bàn phím đang mở, **Hoàn tác** khi có thông báo kế tiếp, ví chung mất mạng và thử đồng bộ lại, TalkBack, cỡ chữ 200%, sáng/tối và animator scale 0.

## Actionable Prompt Mẫu (Dùng để gọi kiểm tra / áp dụng v5.0 cho bất kỳ màn hình nào)

```text
Hãy đóng vai Senior Mobile UI/UX Engineer (Material Design 3, Jetpack Compose, Accessibility, Motion Design).
Đọc file quy chuẩn `DESIGN.md` (v5.0.0 Luminous iOS & Atmospheric Canvas) và `ui-ux-audit-guide.md`, sau đó kiểm tra / áp dụng style v5 cho màn hình `[TÊN MÀN HÌNH / ROUTE]` cùng toàn bộ component con, Dialog và BottomSheet được gọi bên trong nó.

Yêu cầu:
1. Kiểm tra đủ 6 hạng mục trong `ui-ux-audit-guide.md` (bao gồm Cổng kiểm tra 8 điểm ở Hạng mục 6):
   - Bọc nền `AppAtmosphericBackground` (bật `forceSoftBlurOnPhoto = true` với màn dữ liệu dày).
   - Không dùng thẻ đen cho Hero Card: chuyển sang Luminous iOS Hero Card + `Modifier.iosCardShadow(shape, elevated = true)`.
   - Thẻ nội dung & pill nổi dùng `AppTheme.colors.surface` + `0.5.dp hairline` + `Modifier.iosCardShadow(shape, elevated = false)` đặt TRƯỚC `clip`/`Surface`.
   - Chuẩn Material 3 (`ModalBottomSheet`, không hardcode hex/sp `< 12.sp`, đủ Loading/Empty/Error/Content).
   - Trợ năng a11y (`>= 48dp`, `contentDescription`, `Role`, `stateDescription`, `heading()`, `semantics(mergeDescendants = true)`, Light/Dark `>= 4.5:1`, `fontScale 200%`).
   - Motion (`AnimatedContent`, `animateContentSize`, `animateItem`, `rememberNotePayReducedMotion()`) & Error Feedback (`shakeOnError` + `isError` + `AnimatedVisibility`).
2. Cập nhật dấu `[x]` tương ứng trong Bảng tiến độ Hạng mục 6 của `ui-ux-audit-guide.md` sau khi kiểm chứng build, test và ảnh/cây XML thực tế.
```
