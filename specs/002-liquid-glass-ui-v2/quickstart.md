# Quickstart Guide: Liquid Glass UI v2

Tài liệu này hướng dẫn cách xây dựng và kiểm thử xác minh các cải tiến giao diện v2 trên NotePay.

---

## 1. Yêu cầu môi trường (Prerequisites)

*   **Android Studio**: Phiên bản mới nhất (Koala hoặc Jellyfish).
*   **JDK**: Phiên bản 17.
*   **Thiết bị chạy thử**: Thiết bị thật hoặc máy ảo Android Emulator chạy Android API 26+ (Khuyến nghị API 31+ để kiểm thử hiệu ứng Blur thực tế tốt nhất).

---

## 2. Lệnh biên dịch và kiểm tra (Build & Verification Commands)

Chạy các lệnh sau tại thư mục gốc của dự án bằng PowerShell hoặc Terminal để kiểm tra tính toàn vẹn:

### Kiểm tra biên dịch Kotlin
```bash
./gradlew compileDebugKotlin
```

### Chạy các bài kiểm thử đơn vị (Unit Tests) để phòng tránh hồi quy
```bash
./gradlew testDebugUnitTest
```

### Đóng gói ứng dụng để cài đặt thử nghiệm
```bash
./gradlew assembleDebug
```

---

## 3. Các kịch bản kiểm thử nghiệm thu giao diện (UI Verification Scenarios)

### Kịch bản 1: Xác thực lưới khoảng cách (8dp Grid) & Phân cấp thông tin
1. **Thao tác**: Mở ứng dụng, quan sát màn hình Home và màn hình Lịch sử giao dịch.
2. **Tiêu chí đạt**:
    *   Khoảng cách lề trái/phải của các thẻ ví, tiêu đề mục, và hàng giao dịch luôn thẳng hàng tuyệt đối (16dp padding lề).
    *   Kích thước các chữ hiển thị số tiền có độ dày và cỡ chữ vượt trội rõ ràng so với chữ mô tả giao dịch.
    *   Không xuất hiện tình trạng các nút bấm hoặc ô nhập liệu bị lệch trục hay chồng lấn khoảng cách.

### Kịch bản 2: Kiểm tra hiệu ứng kính (Liquid Glass) & Khả năng đọc chữ (Contrast)
1. **Thao tác**: Truy cập màn hình Thống kê (StatsScreen), bật chế độ nền tối (Dark Mode) nếu có, và cuộn qua các thẻ AI Insights.
2. **Tiêu chí đạt**:
    *   Tất cả các thẻ nền kính đều tạo ra bóng mờ mịn (backdrop blur) che nhẹ các thành phần phía sau.
    *   Viền của các thẻ kính có độ dày stroke mỏng (1dp) bán trong suốt màu sáng/tối để tăng chiều sâu.
    *   Chữ viết trên nền kính có độ tương phản cao, dễ đọc trong cả điều kiện ánh sáng mạnh hoặc yếu (đạt độ tương phản tối thiểu 4.5:1).

### Kịch bản 3: Kiểm tra chuyển động mượt mà (60fps Transitions) & Phản hồi xúc giác
1. **Thao tác**: Nhấn chuyển nhanh giữa các Tab điều hướng chính (Home, Stats, Subscriptions) 5-10 lần liên tục. Nhấn thử các nút số trên NumericKeypad tự chế (nếu hiển thị).
2. **Tiêu chí đạt**:
    *   Giao diện chuyển trang mượt mà nhờ hiệu ứng trượt/mờ nhẹ, không xảy ra hiện tượng giật màn hình hoặc trễ thao tác.
    *   Mỗi lượt chuyển tab thành công hoặc nhấn phím số đều kích hoạt một phản hồi rung xúc giác nhẹ (Haptic Feedback) vừa phải.

### Kịch bản 4: Cơ chế Fallback trên máy yếu (API < 31)
1. **Thao tác**: Chạy ứng dụng trên một thiết bị ảo chạy hệ điều hành Android API 29 hoặc API 30.
2. **Tiêu chí đạt**:
    *   Ứng dụng hoạt động ổn định, không bị crash.
    *   Các thẻ kính tự động chuyển sang màu nền bán trong suốt đơn sắc (semi-transparent solid background) thay cho blur động để giữ vững hiệu năng chuyển động 60fps.
    *   Không xảy ra hiện tượng tụt khung hình (drop frames) khi thực hiện cuộn hoặc mở dialog.
