# Sổ Nợ (Quản Lý Vay & Cho Vay - Debt & Loan Management)

Tài liệu đặc tả kỹ thuật và kiến trúc cho phân hệ **Sổ Nợ (Debt & Loan Management)** trong NotePay.

---

## 1. Tổng quan tính năng

Phân hệ **Sổ Nợ** giúp người dùng theo dõi toàn diện các khoản tiền cho vay và đi vay cá nhân một cách riêng tư (Local-First):
- **Hai chiều công nợ**:
  - `LEND` (Cho vay - Cần thu): Người dùng cho bạn bè/người quen vay tiền.
  - `BORROW` (Đi vay - Cần trả): Người dùng vay tiền từ người khác.
- **Trả nợ linh hoạt từng phần (Partial Repayments)**: Hỗ trợ ghi nhận nhiều lần trả nợ với số tiền và ghi chú riêng biệt.
- **Tự động tất toán**: Hệ thống tự động tính lũy kế các đợt trả nợ, tính số tiền còn lại (`remainingAmount`) và tự động chuyển trạng thái `isSettled = true` khi đã trả hết.
- **Đồng bộ ví tùy chọn (`syncWithWallet`)**:
  - Khi cho vay (`LEND`): Tự động sinh giao dịch `EXPENSE` trừ tiền trong ví.
  - Khi thu hồi nợ (`LEND` payment): Tự động sinh giao dịch `INCOME` cộng tiền vào ví.
  - Khi đi vay (`BORROW`): Tự động sinh giao dịch `INCOME` cộng tiền vào ví.
  - Khi trả nợ (`BORROW` payment): Tự động sinh giao dịch `EXPENSE` trừ tiền khỏi ví.
- **Nhắc nợ thông minh (Smart VietQR & Reminder)**:
  - Sinh mã VietQR động trực tiếp (chuẩn EMVCo payload và ảnh QR ngân hàng) từ ví tài khoản của người dùng để gửi người vay quét chuyển khoản ngay.
  - Cung cấp các mẫu tin nhắn nhắc nợ lịch sự, tự động điền tên, số tiền, hạn trả; tích hợp phím tắt Gọi điện (`ACTION_DIAL`), SMS (`ACTION_SENDTO`) và Chia sẻ (`ACTION_SEND` tới Zalo/Messenger).
- **Tích hợp giao diện**:
  - Thẻ tóm tắt tổng quan (`DebtSummaryCard`) đặt nổi bật trong tab **Tài sản** (`AssetsScreen`).
  - Lối vào trung tâm tại màn hình **Tiện ích** (`UtilitiesScreen`).
  - Màn hình danh sách công nợ (`DebtManagementScreen`) với bộ lọc thông minh (Tất cả, Cần thu, Cần trả, Ẩn/Hiện nợ đã tất toán) và thanh tìm kiếm tức thì.
  - Màn hình chi tiết (`DebtDetailScreen`) với thanh tiến độ hoàn nợ, lịch sử trả từng phần, các hành động thao tác nhanh.

---

## 2. Mô hình dữ liệu & Cơ sở dữ liệu (Room v6)

### 2.1 Bảng SQLite `debts`
```sql
CREATE TABLE IF NOT EXISTS `debts` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `person_name` TEXT NOT NULL,
    `phone_number` TEXT,
    `type` TEXT NOT NULL,                  -- 'LEND' hoặc 'BORROW'
    `original_amount_cents` INTEGER NOT NULL,
    `wallet_id` INTEGER,                   -- Ví liên kết (nếu có)
    `created_at` INTEGER NOT NULL,
    `due_date` INTEGER,                    -- Hạn trả nợ (timestamp millis)
    `note` TEXT NOT NULL,
    `is_settled` INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS `index_debts_wallet_id` ON `debts` (`wallet_id`);
CREATE INDEX IF NOT EXISTS `index_debts_due_date` ON `debts` (`due_date`);
CREATE INDEX IF NOT EXISTS `index_debts_is_settled` ON `debts` (`is_settled`);
```

### 2.2 Bảng SQLite `debt_payments`
```sql
CREATE TABLE IF NOT EXISTS `debt_payments` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `debt_id` INTEGER NOT NULL,
    `amount_cents` INTEGER NOT NULL,
    `paid_at` INTEGER NOT NULL,
    `note` TEXT NOT NULL,
    `wallet_id` INTEGER,                   -- Ví nhận/trừ tiền trong lần trả nợ
    FOREIGN KEY(`debt_id`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS `index_debt_payments_debt_id` ON `debt_payments` (`debt_id`);
CREATE INDEX IF NOT EXISTS `index_debt_payments_wallet_id` ON `debt_payments` (`wallet_id`);
```

### 2.3 Migration `MIGRATION_5_6`
Nâng cấp cơ sở dữ liệu `NotePayDatabase` từ version 5 lên version 6, đăng ký thực thi trong `DatabaseModule.kt` và bảo đảm tính tương thích ngược toàn diện cho dữ liệu giao dịch và ví hiện tại.

---

## 3. Tầng Nghiệp vụ (Domain Layer)

### 3.1 Mô hình Domain
- [`DebtType`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/model/debt/Debt.kt): Enum `LEND` (Cho vay), `BORROW` (Đi vay).
- [`Debt`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/model/debt/Debt.kt): Chứa thông tin gốc của khoản nợ.
- [`DebtPayment`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/model/debt/Debt.kt): Bản ghi lần thanh toán từng phần.
- [`DebtWithHistory`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/model/debt/Debt.kt): Tổng hợp `Debt` và danh sách `List<DebtPayment>`, cung cấp các thuộc tính tính toán:
  - `totalPaid`: Tổng số tiền đã thanh toán.
  - `remainingAmount`: Số tiền còn nợ (`originalAmount - totalPaid`).
  - `isFullyPaid`: Cờ xác định đã trả đủ (`remainingAmount.amountInCents <= 0`).
  - `progressRatio`: Tỷ lệ hoàn thành từ `0.0f` đến `1.0f`.
  - `isOverdue()`: Kiểm tra đã quá hạn trả chưa.
  - `isDueToday()`: Kiểm tra có đến hạn trong ngày hôm nay không.

### 3.2 Use Cases
| Use Case | Mục đích |
|---|---|
| [`CreateDebtUseCase`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/usecase/debt/CreateDebtUseCase.kt) | Tạo khoản nợ mới; đồng thời tự động tạo giao dịch trong ví nếu người dùng chọn đồng bộ ví |
| [`RecordDebtPaymentUseCase`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/usecase/debt/RecordDebtPaymentUseCase.kt) | Thêm đợt trả nợ từng phần hoặc toàn bộ; cập nhật trạng thái `isSettled` và sinh giao dịch ví đối ứng |
| [`GetDebtsUseCase`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/usecase/debt/GetDebtsUseCase.kt) | Truy vấn luồng (`Flow`) danh sách nợ kèm hỗ trợ lọc theo loại nợ, tìm kiếm từ khóa, ẩn/hiện nợ đã thanh toán |
| [`GetDebtSummaryUseCase`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/usecase/debt/GetDebtSummaryUseCase.kt) | Tính toán số dư ròng nợ (`netBalance`), tổng cần thu (`totalToCollect`), tổng cần trả (`totalToPay`) |
| [`DeleteDebtUseCase`](file:///d:/APP/Ghichuchitieu/app/src/main/java/com/notepay/domain/usecase/debt/DeleteDebtUseCase.kt) | Xóa một khoản nợ và toàn bộ lịch sử thanh toán tương ứng |

---

## 4. Tầng Giao diện (Presentation Layer)

### 4.1 Danh sách file giao diện chính
```text
ui/feature/debt/
├── DebtUiState.kt               // Trạng thái màn hình danh sách & filter
├── DebtViewModel.kt             // ViewModel quản lý danh sách, filter, tổng hợp
├── DebtDetailUiState.kt         // Trạng thái màn hình chi tiết
├── DebtDetailViewModel.kt       // ViewModel xử lý trả nợ, xóa, nhắc nợ
├── DebtManagementScreen.kt      // Màn hình chính Sổ Nợ (Tabs, Search, List)
├── DebtDetailScreen.kt          // Màn hình chi tiết nợ, tiến độ, lịch sử trả
├── DebtRemindBottomSheet.kt     // BottomSheet nhắc nợ (VietQR + Mẫu tin nhắn)
├── CreateDebtBottomSheet.kt     // BottomSheet tạo khoản nợ mới
├── RecordPaymentDialog.kt       // Hộp thoại ghi nhận trả nợ từng phần
├── DebtItemCard.kt              // Thẻ hiển thị từng khoản nợ trong danh sách
├── DebtSummaryCard.kt           // Thẻ tóm tắt gắn trong màn hình Tài sản
└── DebtNavigation.kt            // Đăng ký Route & Điều hướng Jetpack Compose
```

### 4.2 Thiết kế UI/UX & Công thái học
- **Zero Hardcoded Strings**: 100% chuỗi ký tự hiển thị, nhãn nút, tiêu đề, tin nhắn mẫu và `contentDescription` được trích xuất vào `res/values/strings.xml` và `res/values-en/strings.xml`.
- **Material 3 Expressive**:
  - Sử dụng `CardDefaults.elevatedCardColors`, `PrimaryTabRow`, `FilterChip`.
  - Phối màu trực quan: Màu cam/đỏ cho `BORROW` (Cần trả / Quá hạn), màu xanh lá cho `LEND` (Cần thu), màu xanh dương/trung tính cho Đã tất toán.
  - Touch target đạt chuẩn $\ge 48\text{dp}$.
  - Hỗ trợ Dynamic Color thích ứng với Material You trên Android 12+.

---

## 5. Quy trình xác thực & Kiểm thử

1. **Unit Tests**:
   - `DatabaseMigrationTest`: Kiểm thử migration v1 -> v6 và v5 -> v6, kiểm tra tính toàn vẹn của bảng `debts`, `debt_payments` và khóa ngoại `wallets`.
   - `CreateDebtUseCaseTest`: Kiểm thử logic khởi tạo khoản nợ và đồng bộ giao dịch.
   - `RecordDebtPaymentUseCaseTest`: Kiểm thử tự động tất toán (`isSettled`) và tính số tiền còn lại.
   - `GetDebtSummaryUseCaseTest`: Kiểm thử tổng hợp số dư ròng nợ.
   - `AssetsViewModelTest`: Kiểm thử tích hợp `GetDebtSummaryUseCase` trên màn hình Tài sản.
2. **Lệnh chạy kiểm thử**:
   ```powershell
   cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%&& gradlew.bat :app:testLocalDebugUnitTest"
   ```
   **Kết quả**: 313/313 tests Passed (100% GREEN).
3. **Build APK**:
   ```powershell
   cmd /c "set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr&& set PATH=C:\Program Files\Android\Android Studio\jbr\bin;%PATH%&& gradlew.bat :app:assembleLocalDebug"
   ```
   **Kết quả**: BUILD SUCCESSFUL.
