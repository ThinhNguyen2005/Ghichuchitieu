# Data Model: Liquid Glass UI v2

Tài liệu này xác nhận cấu trúc dữ liệu của dự án NotePay trong khuôn khổ nâng cấp giao diện v2.

---

## 1. Trạng thái cơ sở dữ liệu (Room Database State)

*   **Không thay đổi cấu trúc**: Căn cứ theo mục tiêu phi chức năng và giới hạn (Non-goals) của đặc tả, **không có bất kỳ sự thay đổi cấu trúc bảng, thêm cột hay dịch chuyển cơ sở dữ liệu (Database Migration)** nào trong đợt tinh chỉnh giao diện này.
*   **Danh sách thực thể Room được bảo toàn**:
    *   `WalletEntity` (Bảng: `wallets`)
    *   `TransactionEntity` (Bảng: `transactions`)
    *   `SubscriptionEntity` (Bảng: `subscriptions`)
    *   `BillSplitEntity` (Bảng: `bill_splits`)
*   **Kiểu dữ liệu tiền tệ**: Tiếp tục sử dụng định dạng số nguyên cents (`Long` - `amountCents` / `balanceCents`) để bảo đảm tính đúng đắn của logic tính toán số dư.

---

## 2. Lưu trữ ngoài Room (SharedPreferences)

*   **Danh mục (`Category`)**: Danh sách danh mục tùy chỉnh tiếp tục được lưu trữ qua khóa SharedPreferences `notepay_custom_categories` và nạp vào bộ nhớ động khi ứng dụng khởi chạy.

---

## 3. Quy tắc ràng buộc giao diện (UI State Contracts)

Mặc dù cấu trúc DB không thay đổi, các lớp biểu diễn trạng thái UI (UI State) sẽ được tối ưu hóa để hỗ trợ dựng hình mượt mà:
*   Tránh truyền tải trực tiếp các thực thể Room (`Entity`) lên tầng UI; toàn bộ dữ liệu phải được ánh xạ qua các lớp domain model thuần Kotlin (`Wallet`, `Transaction`, `Category`) trước khi đưa vào các lớp trạng thái UI (`UiState`) của các màn hình.
*   Các danh sách hiển thị trên giao diện Liquid Glass (như danh sách giao dịch, danh sách ví) bắt buộc phải sử dụng các kiểu dữ liệu bất biến (`List`) và khai báo thuộc tính ổn định (`@Stable` hoặc `@Immutable` nếu cần thiết) để giúp Jetpack Compose tối ưu hóa, tránh vẽ lại (recomposition) các hàng không thay đổi.
