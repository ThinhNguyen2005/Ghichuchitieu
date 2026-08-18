# Tasks: Delight Motion & Smart Features

**Status Legend**:
- `[ ]` Not Started
- `[/]` In Progress
- `[x]` Completed

---

## Phase 1: Core Motion & Balance Card Customization (P1)
- [x] **Task 1.1**: Tạo `RollingNumberTicker.kt` (hiệu ứng cuộn số khi giá trị tiền tệ thay đổi).
- [x] **Task 1.2**: Nâng cấp `BalanceCard.kt` hỗ trợ background ảnh người dùng tùy chọn kèm lớp phủ tương phản (Vignette Scrim Layer) đảm bảo WCAG AA.
- [x] **Task 1.3**: Thêm PhotoPicker vào màn hình Thẻ Số Dư / Quản lý Ví để chọn và lưu URI ảnh nền.
- [x] **Task 1.4**: Viết Unit Test cho logic hiển thị và cuộn số.

---

## Phase 2: Gesture-driven Swipe to Action (P1)
- [x] **Task 2.1**: Tạo component `SwipeableTransactionItem.kt` sử dụng `Animatable` + pointer drag bám 1:1 theo ngón tay, có độ đàn hồi lò xo (rubber-band) và rung haptic.
- [x] **Task 2.2**: Tích hợp `SwipeableTransactionItem` vào `TransactionListScreen.kt` và phần danh sách gần đây trong `HomeScreen.kt`.
- [x] **Task 2.3**: Kiểm tra tương tác ngắt quãng (interruptibility) và độ mượt 60fps trên thiết bị.

---

## Phase 3: Android Version Compatibility & Liquid Glass Toggle (P2)
- [x] **Task 3.1**: Tạo `OsCompatHelper.kt` để kiểm tra phiên bản Android (hỗ trợ `RenderEffect` trên API 31+).
- [x] **Task 3.2**: Thêm thiết lập bật/tắt `Liquid Glass` trong `AppSettingsScreen.kt` (vô hiệu hóa và hiển thị chú thích khi chạy Android < 12).
- [x] **Task 3.3**: Cấu hình fallback giao diện phẳng mờ khi Liquid Glass bị tắt hoặc thiết bị không hỗ trợ.

---

## Phase 4: Shared Element Transition Navigation (P2)
- [x] **Task 4.1**: Cấu hình `SharedTransitionLayout` bao bọc `NavHost` trong `NotePayNavHost.kt`.
- [x] **Task 4.2**: Gắn các animation chuyển tiếp slide và fade mượt mà giữa các màn hình và danh sách.
- [x] **Task 4.3**: Kiểm thử chuyển cảnh và thao tác vuốt back cử chỉ (Predictive Back Gesture).

---

## Phase 5: Smart Reminders & Streak Habit System (P3)
- [x] **Task 5.1**: Tạo `StreakTrackerHelper.kt` tính toán chuỗi ngày ghi chép liên tiếp và viết unit test.
- [x] **Task 5.2**: Hiển thị huy hiệu Streak 🔥 trên Header của `HomeScreen.kt`.
- [x] **Task 5.3**: Tạo `DailyReminderWorker.kt` và `ReminderScheduler.kt` với `WorkManager` nhắc nhở lúc 20:30 tối.
- [x] **Task 5.4**: Tạo `SmartInsightsCard.kt` hiển thị phân tích thông minh trên Dashboard.

---

## Phase 6: Verification & Final Polish
- [x] **Task 6.1**: Chạy toàn bộ Unit Tests (`:app:testPlayDebugUnitTest`, `:app:testFullDebugUnitTest`, `:app:testLocalDebugUnitTest`).
- [x] **Task 6.2**: Biên dịch kiểm thử mã nguồn và xác minh không có lỗi crash.
