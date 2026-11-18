# Quy Chuẩn Kiểm Tra & Nâng Cấp UI/UX NotePay (`ui-ux-audit-guide.md`)

Tài liệu này là kịch bản kiểm tra chuẩn (**Audit Checklist & Actionable Prompt**) dành cho mã nguồn **Jetpack Compose + Material Design 3 (`androidx.compose.material3:1.4.0`)**, tuân thủ hệ thống token **NotePay v4 — Sand & Ink (`DESIGN.md`)**, chuẩn **Trợ năng (WCAG AA / TalkBack)** và **Motion Design** của Google.

> **Nguyên tắc thực thi:** Khi kiểm tra bất kỳ màn hình nào, rà soát tuần tự cả **5 hạng mục** dưới đây. Không bỏ qua `Dialog` con, `BottomSheet` con, hoặc các trạng thái `Loading / Empty / Error / Offline` được gọi bên trong màn hình đó.

---

## Hạng mục 1: Kiểm tra Luồng tương tác & Nút chức năng trùng lặp (Flow & Action Redundancy)

### Checklist kiểm tra
- [ ] **Quét trùng lặp hành động trên cùng màn hình:** Liệt kê toàn bộ `Button`, `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton`, `FloatingActionButton`, `DropdownMenuItem`, `ListItem`, thẻ `.clickable` để phát hiện các nút dẫn tới cùng một đích hoặc thực hiện cùng một hành động (ví dụ: có cả nút CTA trong Empty State lẫn `FloatingAddButton` `+` ở thanh điều hướng nổi cùng mở màn hình Thêm giao dịch).
- [ ] **Đúng 1 Hành động Chính (Single Primary Action):**
  - Mỗi màn hình chỉ giữ tối đa **1 nút Primary nổi bật nhất** (`Button` màu `ink` / `primary` hoặc `FloatingAddButton` ở 4 tab gốc).
  - Các hành động phụ phải hạ cấp xuống `FilledTonalButton`, `OutlinedButton`, `TextButton`, `IconButton` hoặc gom vào `DropdownMenu` / `ModalBottomSheet`.
  - Theo `DESIGN.md` v4: mỗi màn hình chỉ có **1 thẻ Ink đen (`AppTheme.colors.ink`)** làm tâm điểm dữ liệu; không đặt 2 thẻ Ink cạnh nhau làm phân tán sự chú ý.
- [ ] **Phát hiện nút "chết" hoặc dẫn vào ngõ cụt:** Không bật sẵn nút dẫn tới một form/sheet mà điều kiện tiên quyết chưa thỏa (ví dụ: nút "Chuyển tiền giữa các ví" vẫn bật khi người dùng mới có 0 hoặc 1 ví, bấm vào mới thấy thông báo lỗi). Hãy disable kèm gợi ý rõ ràng, ẩn khi không khả dụng, hoặc đổi thành CTA dẫn đúng bước tạo tiền đề.
- [ ] **Độ dài luồng & Tham số thừa:** Kiểm tra xem thao tác phổ biến có bị kéo dài qua nhiều lớp không cần thiết hay không; loại bỏ các callback/tham số điều hướng thừa không được gọi.
- [ ] **Đầy đủ 4 trạng thái dữ liệu:** Kiểm tra đủ cả `Loading`, `Empty` (dùng component chuẩn `EmptyStateWithAction`), `Error` (có thông báo nguyên nhân an toàn + nút Thử lại), và `Content`.

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
- [ ] **Kiểm tra Design Token (`DESIGN.md` v4 & `AppTheme`):**
  - **Cấm hardcode mã màu Hex** (`Color(0xFF...)`) hoặc `Color.White` / `Color.Black` trực tiếp trong UI; luôn dùng `MaterialTheme.colorScheme` hoặc `AppTheme.colors`.
  - **Cấm hardcode kích thước chữ** (`.sp` tùy tiện) và góc bo ngoài thang `AppShapes` (`12 / 16 / 20 / 24 / 28 / capsule`) hoặc khoảng cách ngoài lưới `4.dp` (`4 / 8 / 16 / 24 / 32`).
  - **Thẻ phẳng theo v4:** Thẻ nội dung dùng `defaultElevation = 0.dp` (phân tầng bằng màu nền và viền `1.dp`, không đổ bóng cho thẻ tĩnh).

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

## Actionable Prompt Mẫu (Dùng để gọi kiểm tra bất kỳ màn hình nào)

```text
Hãy đóng vai Senior Mobile UI/UX Engineer (Material Design 3, Jetpack Compose, Accessibility, Motion Design).
Đọc file quy chuẩn `ui-ux-audit-guide.md` và `DESIGN.md` của dự án, sau đó kiểm tra toàn diện màn hình `[TÊN MÀN HÌNH / ROUTE]` cùng toàn bộ component con, Dialog và BottomSheet được gọi bên trong nó.

Yêu cầu:
1. Kiểm tra đủ 5 hạng mục trong `ui-ux-audit-guide.md`:
   - Luồng tương tác & Nút trùng chức năng (1 Primary Action, trạng thái Loading/Empty/Error/Content).
   - Chuẩn hóa Material 3 (`ListItem`, `SegmentedButton`, `FilterChip`, `ModalBottomSheet` thay `Dialog` nhập liệu/danh sách, không hardcode hex/sp).
   - Trợ năng a11y (vùng chạm >= 48dp, `contentDescription`, `Role`, `stateDescription`, `semantics(mergeDescendants = true)`, tương phản Light/Dark, fontScale 200%).
   - Motion Design (Entrance/Transition mượt, `AnimatedContent`, `animateContentSize`, `animateItem`, tôn trọng `rememberNotePayReducedMotion()`).
   - Micro-interactions & Error Feedback (Horizontal Shake bằng `Animatable` + `keyframes`, `LocalHapticFeedback`, `isError` + `AnimatedVisibility` cho `supportingText`).
2. Trích dẫn chính xác file nguồn, tên hàm composable và số dòng cho từng vấn đề phát hiện.
3. Đề xuất thứ tự ưu tiên nâng cấp (P0 -> P3) trước khi chỉnh sửa code.
```
