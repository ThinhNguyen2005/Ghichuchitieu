# Implementation Plan: Delight Motion & Smart Features

**Branch**: `feature/delight-motion-smart-features` | **Spec**: [spec.md](file:///d:/APP/Ghichuchitieu/specs/003-delight-motion-and-smart-features/spec.md)

---

## 1. Mục Tiêu Kỹ Thuật (Technical Goals)

1. **Hiệu năng 60fps & Phản hồi tức thì**:
   - Sử dụng `graphicsLayer` cho các transform biến đổi thị giác.
   - Thao tác kéo vuốt cử chỉ sử dụng `pointerInput` với `Animatable` và `VelocityTracker` thay vì các modifier cứng nhắc.
2. **Xử lý hình ảnh & Tương phản (Contrast & Accessibility)**:
   - Dùng `AsyncImage` (Coil) hoặc Painter nạp ảnh local an toàn.
   - Áp dụng lớp phủ đa tầng: `Box` chứa hình ảnh nền ➔ `Box` phủ lớp Gradient mờ chuyển sắc (Dark/Light Scrim) + viền bóng mờ nhẹ ➔ Lớp hiển thị dữ liệu văn bản với kiểu chữ có Shadow vi tế, đảm bảo luôn đạt độ tương phản chuẩn WCAG AA.
3. **Quản lý tương thích Android Version**:
   - Helper `OsCompatHelper.supportsLiquidGlass()` kiểm tra `Build.VERSION.SDK_INT >= 31`.
   - DataStore lưu preference `KEY_LIQUID_GLASS_ENABLED`.
4. **Shared Transitions**:
   - Sử dụng `SharedTransitionLayout` / `SharedTransitionScope` và `AnimatedContentScope` của Navigation Compose 2.9.8+.
5. **WorkManager Periodic Daily Notification**:
   - `DailyReminderWorker` được đăng ký qua `WorkManager` (PeriodicWorkRequestBuilder 24 giờ).
   - Kiểm tra trong `TransactionDao` xem ngày hiện tại đã có giao dịch chưa trước khi bắn Notification nhắc nhở.

---

## 2. Danh Mục File Chạm & Thêm Mới

```text
app/src/main/java/com/notepay/
├── data/
│   ├── datastore/
│   │   └── AppSettingsDataStore.kt       # Lưu setting bật tắt Liquid Glass, giờ nhắc nhở, ảnh nền ví
├── domain/
│   └── util/
│       ├── OsCompatHelper.kt             # Kiểm tra SDK_INT & tính năng phần cứng
│       └── StreakTrackerHelper.kt        # Tính toán chuỗi ngày ghi chép liên tiếp
├── ui/
│   ├── component/
│   │   ├── BalanceCard.kt                # Thẻ số dư hỗ trợ ảnh nền + Scrim + Rolling Number
│   │   ├── SwipeableTransactionItem.kt   # Item giao dịch vuốt mượt bám ngón tay
│   │   ├── RollingNumberTicker.kt        # Composable cuộn số tiền mượt mà
│   │   └── SmartInsightsCard.kt          # Thẻ thông tin tài chính thông minh
│   ├── feature/
│   │   ├── home/
│   │   │   └── HomeScreen.kt             # Hiển thị Streak 🔥 và SmartInsightsCard
│   │   └── settings/
│   │       └── AppSettingsScreen.kt      # Toggle Liquid Glass tương thích OS
│   └── navigation/
│       └── NotePayNavHost.kt             # SharedTransitionLayout tích hợp
└── worker/
    ├── DailyReminderWorker.kt            # Worker thông báo nhắc nhở 20:30 tối
    └── ReminderScheduler.kt              # Tiện ích kích hoạt / hủy WorkManager
```

---

## 3. Chiến Lược Kiểm Thử (Testing Strategy)

1. **Unit Test Logic**:
   - `StreakTrackerHelperTest`: Kiểm tra các trường hợp ngày liên tiếp, ngày bị ngắt quãng, ngày có nhiều giao dịch, danh sách rỗng.
   - `RollingNumberTickerTest` / `OsCompatHelperTest`: Kiểm tra logic điều kiện và fallback.
2. **Compose UI & Interaction Test**:
   - Test vuốt item giao dịch và trigger callback Xóa / Sửa.
   - Test hiển thị thẻ số dư khi có ảnh nền và khi không có ảnh nền.
