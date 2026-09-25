# Kế hoạch triển khai: Đa dạng hóa thông báo & Nhắc nhở thông minh

## 1. Mục tiêu
Nâng cấp toàn diện hệ thống thông báo của NotePay từ thông báo tĩnh đơn điệu thành hệ thống thông báo tài chính thông minh, đa dạng, hữu ích và mang tính cá nhân hóa cao:
1. **Cảnh báo ngân sách tức thời (Instant Budget Alerts):** Tự động phát hiện khi chi tiêu chạm ngưỡng 80% và 100% hạn mức (ví hoặc tháng) ngay sau khi lưu giao dịch, có cơ chế debounce tránh spam trong cùng 1 tháng.
2. **Báo cáo tổng kết tuần (Weekly Financial Digest):** Tự động gửi lúc 20:00 tối Chủ Nhật, tổng kết chi tiêu 7 ngày qua, so sánh % tăng/giảm với tuần trước đó, danh mục chi tiêu cao nhất, chạm vào mở thẳng tab Thống kê.
3. **Nhắc nhở Streak thông minh với nội dung xoay vòng đa dạng (Smart Rotating Daily & Streak Reminders):** Nhắc nhở buổi tối kèm thông điệp xoay vòng (hài hước, kỷ luật tài chính, chúc mừng mốc Streak 3, 7, 14, 30 ngày).
4. **Cài đặt Thông báo trong AppSettings (`AppSettingsScreen`):** Giao diện bật/tắt riêng cho từng loại thông báo và chọn giờ nhắc nhở.

## 2. Các bước triển khai chi tiết

### Bước 1: Mở rộng DataStore lưu cấu hình thông báo (`AppSettingsDataStore.kt`)
- `budgetAlertsEnabled`: boolean (default = true)
- `weeklyDigestEnabled`: boolean (default = true)
- `streakReminderEnabled`: boolean (default = true)
- Lưu trữ mốc cảnh báo đã phát trong tháng: `lastNotifiedBudgetMonth`, `lastNotifiedThreshold` (80 hoặc 100) để chống spam.

### Bước 2: Tạo Notification Channel & Helper quản lý thông báo (`NotificationHelper.kt`)
- Khởi tạo 3 kênh thông báo riêng biệt:
  - `channel_budget_alerts`: Cảnh báo ngân sách (Ưu tiên cao, rung/chuông)
  - `channel_weekly_digest`: Tổng kết tài chính tuần (Ưu tiên mặc định)
  - `channel_daily_reminders`: Nhắc nhở ghi chép & Streak (Ưu tiên mặc định)
- Đóng gói hàm gửi thông báo chuẩn `PendingIntent` điều hướng chính xác (`Route.AddTransaction`, `Route.Stats`, `Route.Home`).

### Bước 3: Triển khai Cảnh báo ngân sách tức thì (`BudgetAlertNotifier.kt`)
- Được kích hoạt sau khi giao dịch mới được lưu (`TransactionRepositoryImpl.upsert`).
- Đọc tổng chi tiêu tháng và hạn mức hiệu lực.
- Nếu tỷ lệ >= 80% (và chưa báo mốc 80% tháng này) -> Bắn thông báo cảnh báo vàng.
- Nếu tỷ lệ >= 100% (và chưa báo mốc 100% tháng này) -> Bắn thông báo cảnh báo đỏ nguy cấp.

### Bước 4: Triển khai Worker Tổng kết tuần (`WeeklyDigestWorker.kt` & `ReminderScheduler.kt`)
- Lên lịch chạy lúc 20:00 tối Chủ Nhật.
- Tính toán:
  - Chi tiêu tuần này (7 ngày qua).
  - Chi tiêu tuần trước (7 ngày trước đó).
  - Tỷ lệ thay đổi % và danh mục chi lớn nhất.
- Bắn thông báo tổng kết tuần.

### Bước 5: Nâng cấp Lời nhắc Streak thông minh (`DailyReminderWorker.kt`)
- Xây dựng kho thông điệp phong phú theo danh sách template tài nguyên chuỗi.
- Tự động chọn nội dung phù hợp dựa trên số ngày Streak và ngày trong tuần.

### Bước 6: Thêm mục "Thông báo & Lời nhắc" vào UI (`AppSettingsScreen.kt` & `HomeViewModel.kt`)
- Thiết kế thẻ Material 3 cao cấp với các Switch bật/tắt trực quan.
- Hộp thoại / Time Picker chọn giờ nhắc nhở hàng ngày.

### Bước 7: Trích xuất 100% chuỗi ra `strings.xml` & `values-en/strings.xml`
- Tuyệt đối tuân thủ Zero Hardcoded Strings policy.

### Bước 8: Viết Unit Test & Kiểm thử
- Unit test cho logic cảnh báo ngân sách.
- Unit test cho tính toán digest tuần.
- Biên dịch APK và chạy bộ test.
