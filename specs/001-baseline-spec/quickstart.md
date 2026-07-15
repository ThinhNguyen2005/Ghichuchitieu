# Quickstart Validation Guide: Baseline Specification

Tài liệu này hướng dẫn cách kiểm thử và xác thực hoạt động của dự án NotePay Baseline.

---

## 1. Prerequisites (Yêu cầu chuẩn bị)

* **JDK**: Java Development Kit 17 hoặc mới hơn.
* **Android SDK**: Bản cài đặt Android SDK tương thích với SDK Android 34+.
* **Gradle**: Sử dụng Gradle wrapper tích hợp sẵn trong dự án (`./gradlew`).
* **Thiết bị kiểm thử**: Điện thoại Android chạy hệ điều hành hỗ trợ AICore (Android 14+ khuyến nghị cho Gemini Nano) hoặc thiết bị giả lập (Emulator).

---

## 2. Setup & Compile (Thiết lập & Biên dịch)

Để chuẩn bị môi trường và biên dịch dự án chạy thử, sử dụng lệnh:

```bash
# Cấp quyền thực thi cho gradle wrapper (chỉ chạy trên Linux/macOS)
chmod +x gradlew

# Biên dịch dự án và kiểm tra cú pháp lỗi
./gradlew compileDebugSources
```

---

## 3. Automated Tests (Kiểm thử tự động)

Hệ thống cung cấp hai cấp độ kiểm thử để bảo vệ dự án khỏi lỗi hồi quy (regressions) tuân thủ theo nguyên lý Hiến pháp.

### A. Chạy Unit Tests (Kiểm thử logic nghiệp vụ & Room DB)
```bash
./gradlew testDebugUnitTest
```
* **Kỳ vọng**: 100% các ca kiểm thử liên quan đến Repository, UseCases, ViewModels và Room DAOs phải vượt qua thành công.

### B. Chạy Instrumentation Tests (Kiểm thử giao diện Liquid Glass Compose)
```bash
./gradlew connectedAndroidTest
```
* **Kỳ vọng**: Chạy thành công các ca kiểm thử giao diện Compose, đảm bảo các thành phần Liquid Glass (như blur, hiệu ứng kính, transition) được dựng đúng và không gây lỗi crash.

---

## 4. Manual Verification (Xác thực thủ công)

### A. Kiểm tra ghi nhận giao dịch & Số dư ví
1. Khởi động ứng dụng, truy cập màn hình thêm giao dịch.
2. Thêm một khoản chi tiêu 50.000 VND danh mục "Ăn uống" từ ví "Tiền mặt".
3. Xác nhận số dư ví "Tiền mặt" bị trừ đúng 50.000 VND và giao dịch hiển thị ngay lập tức trên màn hình chính với giao diện kính đục Liquid Glass.

### B. Kiểm tra đọc thông báo ngân hàng
1. Đảm bảo ứng dụng đã được cấp quyền "Notification Access" (Lắng nghe thông báo) trong cài đặt Android.
2. Gửi một thông báo giả lập (ví dụ sử dụng adb shell) chứa từ khóa số dư ngân hàng:
   ```bash
   adb shell cmd notification post -S bigText -t "Vietcombank" "NotePay" "GD: +500,000 VND vao tai khoan 123456. So du cuoi: 10,500,000 VND."
   ```
3. Mở NotePay, kiểm tra xem hệ thống có tự động nhận diện và đề xuất tạo một giao dịch thu nhập (income) trị giá 500.000 VND hay không.

### C. Kiểm tra Gợi ý AI (Gemini Nano)
1. Truy cập tab "AI Insights" trên ứng dụng.
2. Nhấn nút "Yêu cầu gợi ý chi tiêu tuần này".
3. Xác nhận giao diện hiển thị hiệu ứng mờ kính Liquid Glass trong lúc AI phân tích, và kết quả phân tích tài chính xuất hiện sau tối đa 3 giây (SC-004).
