# Research: Baseline Specification

Tài liệu này tổng hợp kết quả nghiên cứu và các quyết định kỹ thuật cho 3 vấn đề cốt lõi của NotePay Baseline.

---

## 1. Single Base Currency (Đơn vị tiền tệ cơ sở duy nhất)

* **Quyết định**: Ứng dụng chỉ sử dụng một đơn vị tiền tệ cơ sở duy nhất (Ví dụ: VND hoặc USD) cho toàn bộ hệ thống.
* **Lý do chọn**:
  * Giảm thiểu sự phức tạp trong cơ sở dữ liệu và các phép tính toán ngân sách/phân tích chi tiêu.
  * Không phụ thuộc vào mạng internet để tải tỷ giá hối đoái liên tục (phù hợp với thiết kế offline-first).
* **Các phương án thay thế khác**:
  * *Multi-currency wallets*: Cho phép mỗi ví dùng một đơn vị tiền tệ và quy đổi động. Bị loại bỏ vì làm tăng độ phức tạp của cơ sở dữ liệu và yêu cầu cơ chế đồng bộ tỷ giá ngoại tuyến (phải có file tỷ giá lưu trữ sẵn hoặc nhập thủ công), vi phạm nguyên lý YAGNI (lười biếng/tối giản) của Ponytail Mode.

---

## 2. On-Device AI: Google AI Edge SDK (Gemini Nano)

* **Quyết định**: Sử dụng **Google AI Edge SDK** (AICore) để giao tiếp và chạy mô hình **Gemini Nano** cục bộ trên thiết bị của người dùng.
* **Lý do chọn**:
  * Hoàn toàn ngoại tuyến và bảo mật: Dữ liệu giao dịch nhạy cảm của người dùng không bao giờ rời khỏi thiết bị.
  * Khả năng hiểu ngôn ngữ tự nhiên tốt để đưa ra các gợi ý tài chính thông minh dựa trên lịch sử giao dịch.
* **Các phương án thay thế khác**:
  * *Rule-based heuristic engine*: Sử dụng bộ quy tắc cứng để đưa ra gợi ý (ví dụ: "Chi tiêu ăn uống > 30% -> cảnh báo"). Nhẹ hơn nhưng thiếu sự linh hoạt và tính tự nhiên của AI. Được giữ lại làm phương án dự phòng (fallback) nếu thiết bị không hỗ trợ AICore/Gemini Nano.
  * *Local ONNX / TensorFlow Lite*: Tải mô hình nhỏ tự huấn luyện. Bị loại bỏ vì kích thước ứng dụng tăng lớn và độ chính xác của mô hình nhỏ tự huấn luyện không bằng Gemini Nano được tích hợp sẵn trong hệ điều hành Android của các dòng máy hiện đại.

---

## 3. Bank Notification Access (Notification Listener Service)

* **Quyết định**: Triển khai một dịch vụ kế thừa từ `NotificationListenerService` của Android để lắng nghe các thông báo biến động số dư.
* **Lý do chọn**:
  * Đọc thông báo trực tiếp từ các ứng dụng ngân hàng và ứng dụng tin nhắn (SMS, Messenger) khi có biến động số dư.
  * Tuân thủ các chính sách bảo mật mới của Google Play Store (dễ được duyệt hơn so với xin quyền đọc toàn bộ SMS).
* **Các phương án thay thế khác**:
  * *SMS Read Permission*: Đọc trực tiếp hộp thư SMS. Phương án này đáng tin cậy cho các tin nhắn ngân hàng truyền thống, nhưng khó được Google duyệt phát hành ứng dụng và không bắt được thông báo đẩy (push notifications) từ các app ngân hàng số hiện đại.
