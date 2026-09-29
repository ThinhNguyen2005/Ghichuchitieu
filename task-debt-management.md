# Kế hoạch triển khai: Quản lý công nợ (Debt & Loan Management)

## 1. Mục tiêu
Xây dựng module Quản lý công nợ chuyên sâu, hiện đại và chuẩn công thái học cho ứng dụng NotePay:
1. **Quản lý 2 chiều linh hoạt:**
   - **Tôi cho vay (Người khác nợ tôi - LEND / Receivable):** Theo dõi số tiền cho bạn bè, người thân vay.
   - **Tôi đi vay (Tôi nợ người khác - BORROW / Payable):** Theo dõi các khoản vay mượn cá nhân cần trả.
2. **Theo dõi trả nợ từng phần (Partial Repayment):**
   - Không chỉ đóng/mở khoản nợ, người dùng có thể ghi nhận từng đợt trả nợ với số tiền, ngày trả, ghi chú, và ví nhận/trả.
   - Tự động tính toán số dư nợ còn lại và tiến độ % trả nợ.
3. **Đồng bộ số dư Ví thông minh:**
   - Tùy chọn (Switch bật/tắt): Tự động tạo giao dịch Thu/Chi tương ứng trong Ví tiền khi cho vay, đi vay, hoặc thanh toán nợ.
4. **Hạn trả nợ (Due Date) & Nhắc nợ thông minh:**
   - Đặt ngày hẹn trả nợ, tự động hiển thị huy hiệu (Còn hạn / Đến hạn hôm nay / Đã quá hạn).
   - Thông báo đẩy khi khoản nợ sắp đến hạn.
   - Nút "Nhắc nợ": Tích hợp tạo VietQR động + tin nhắn soạn sẵn để copy gửi Zalo/Messenger/SMS chỉ với 1 chạm.
5. **Trải nghiệm giao diện chuẩn Material 3 Expressive:**
   - Màn hình chính `DebtManagementScreen`: Bộ lọc tab (Tất cả / Cần thu / Cần trả), thanh tìm kiếm người nợ, thẻ thống kê tổng quan (Tổng cho vay, Tổng đi vay, Dư nợ ròng).
   - Thẻ tóm tắt Công nợ trên màn hình Tài sản (`AssetsScreen`) để theo dõi bức tranh tài chính toàn diện.
   - Màn hình chi tiết `DebtDetailScreen`: Lịch sử các lần thanh toán, nút trả nợ nhanh, nút chia sẻ VietQR.

---

## 2. Kiến trúc & Thiết kế kỹ thuật

### 2.1 Room Entities & Database Migration
- `DebtEntity`:
  - `id: Long = 0L` (Primary Key autoGenerate)
  - `personName: String`
  - `phoneNumber: String? = null`
  - `type: String` ("LEND" hoặc "BORROW")
  - `originalAmountCents: Long`
  - `walletId: Long? = null` (Ví liên kết nếu có đồng bộ)
  - `createdAt: Long` (Epoch millis)
  - `dueDate: Long? = null` (Epoch millis ngày hẹn trả)
  - `note: String = ""`
  - `isSettled: Boolean = false` (Đã trả hết chưa)
- `DebtPaymentEntity`:
  - `id: Long = 0L`
  - `debtId: Long` (Foreign Key -> DebtEntity, CASCADE)
  - `amountCents: Long`
  - `walletId: Long? = null`
  - `paidAt: Long`
  - `note: String = ""`
  - `transactionId: Long? = null` (Id giao dịch tạo ra nếu có đồng bộ ví)
- `AppDatabase`: Nâng cấp version database (version 5 -> version 6), viết Migration 5->6 an toàn dữ liệu.

### 2.2 Domain Layer
- Model: `Debt`, `DebtPayment`, `DebtType`, `DebtWithPayments`
- Repository: `DebtRepository`
- Use Cases:
  - `GetDebtsUseCase` (Lấy danh sách nợ kèm tổng đã trả và dư nợ còn lại)
  - `CreateDebtUseCase` (Tạo khoản nợ mới, tùy chọn trừ/cộng ví)
  - `RecordDebtPaymentUseCase` (Ghi nhận thanh toán từng phần, cập nhật trạng thái nếu trả đủ)
  - `DeleteDebtUseCase` (Xóa khoản nợ và các khoản thanh toán liên quan)

### 2.3 Data Layer
- `DebtDao`: Truy vấn danh sách nợ, chi tiết nợ kèm payments, upsert, delete.
- `DebtRepositoryImpl`: Triển khai các use case trên IO Dispatcher.

### 2.4 Worker & Notification Layer
- Tích hợp kiểm tra khoản nợ đến hạn vào `DailyReminderWorker` hoặc Worker chuyên biệt, gửi cảnh báo qua Notification Channel riêng.
- Sinh VietQR URL chuẩn Napas (dùng cấu hình ngân hàng hiện có của người dùng từ VietQR config).

### 2.5 UI Layer
- Màn hình `DebtManagementScreen`: Danh sách thẻ nợ phân loại rõ ràng (Màu xanh: Cần thu / Màu cam: Cần trả).
- Sheet `CreateDebtBottomSheet`: Nhập tên người, chọn loại (Cho vay / Vay), số tiền, ngày hẹn, ghi chú, toggle đồng bộ ví.
- Màn hình `DebtDetailScreen`: Chi tiết tiến trình trả nợ, nút Ghi nhận trả nợ, nút Nhắc nợ VietQR.
- Tích hợp thẻ tóm tắt vào `AssetsScreen` và nút mở tại `UtilitiesScreen`.

---

## 3. Kế hoạch kiểm thử & Đảm bảo chất lượng (@android-pro)
1. 100% Zero Hardcoded Strings (đầy đủ `values/strings.xml` và `values-en/strings.xml`).
2. Unit tests cho `DebtRepository`, `CreateDebtUseCase`, `RecordDebtPaymentUseCase` tính toán số dư nợ và logic đồng bộ ví.
3. Kiểm tra Room migration từ v5 sang v6 không làm mất dữ liệu người dùng.
4. Biên dịch debug APK và chạy toàn bộ unit tests dự án.
