# Walkthrough - Category Rename Propagation & WorkManager Hilt Setup

Tài liệu này ghi lại các cải tiến kỹ thuật đã thực hiện để khắc phục lỗi đồng bộ danh mục và cấu hình chạy ngầm WorkManager.

---

## 🛠️ Các nội dung đã thực hiện

### 1. Đồng bộ hóa Thay đổi Danh mục thời gian thực (Category Rename Propagation)
* **Vấn đề**:
  - Giao dịch (`TransactionEntity`) trong Room database lưu danh mục dưới dạng chuỗi ID (ví dụ: `"CUSTOM_1234"`).
  - Khi người dùng chỉnh sửa danh mục tùy biến (thay đổi tên hiển thị hoặc màu sắc), thông tin mới được lưu vào `SharedPreferences` và được nạp vào bộ đệm in-memory `Category.customCategories`.
  - Tuy nhiên, Room DAO truy vấn danh sách giao dịch (`observeAll`, `observeByMonth`, `observeByWallet`) không biết về sự thay đổi này vì bảng `transactions` trong SQLite không có bất kỳ thay đổi nào.
  - Kết quả là các màn hình như Home, Danh sách giao dịch, Thống kê vẫn hiển thị tên danh mục cũ cho đến khi có giao dịch mới được thêm/sửa/xóa hoặc khởi động lại app.
* **Giải pháp**:
  - Inject `CategoryRepository` vào [TransactionRepositoryImpl.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/data/repository/TransactionRepositoryImpl.kt).
  - Sử dụng toán tử `combine` của Kotlin Coroutines Flow để kết hợp luồng dữ liệu truy vấn từ `dao` với luồng danh mục `categoryRepository.observeCategories()`.
  - Mỗi khi danh mục được cập nhật hoặc chỉnh sửa, luồng kết hợp sẽ tự động kích hoạt ánh xạ lại (re-map) danh sách giao dịch thông qua `mapper.toDomain(...)` và phát lại (emit) giá trị mới nhất tới UI.
  - Giờ đây, mọi thay đổi về tên danh mục hoặc màu sắc được cập nhật tức thì trên tất cả các transaction card/item ngoài màn hình.

### 2. Sửa lỗi WorkManager không thể khởi tạo `SubscriptionReminderWorker`
* **Vấn đề**:
  - Lỗi `java.lang.NoSuchMethodException: com.notepay.worker.SubscriptionReminderWorker.<init> [class android.content.Context, class androidx.work.WorkerParameters]` xảy ra vì WorkManager cố gắng khởi tạo worker bằng `WorkerFactory` mặc định của hệ thống.
  - Factory mặc định yêu cầu constructor chỉ có đúng hai tham số là `(Context, WorkerParameters)`. Vì class sử dụng Dependency Injection để truyền `SubscriptionRepository` vào nên WorkManager bị lỗi.
* **Giải pháp**:
  - **Sửa khai báo Worker**: Sử dụng các annotation `@HiltWorker` và `@AssistedInject` cùng với `@Assisted` cho `Context` và `WorkerParameters`.
  - **Tắt bộ khởi tạo tự động**: Cấu hình thẻ `<provider>` của `androidx.startup.InitializationProvider` trong [AndroidManifest.xml](file:///d:/Ghichuchitieu/app/src/main/AndroidManifest.xml) để loại bỏ `androidx.work.WorkManagerInitializer` thông qua `tools:node="remove"`.
  - **Khởi tạo WorkManager bằng Hilt**: Cho [NotePayApp.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/NotePayApp.kt) triển khai `Configuration.Provider`, `@Inject lateinit var workerFactory: HiltWorkerFactory` và override lại cấu hình `workManagerConfiguration` trỏ về `workerFactory` của Hilt.

---

## 🧪 Kết quả kiểm thử (Unit Tests)

### 1. Viết mới bộ Unit Test
* Đã viết mới lớp [TransactionRepositoryImplTest.kt](file:///d:/Ghichuchitieu/app/src/test/java/com/notepay/data/TransactionRepositoryImplTest.kt) sử dụng `FakeTransactionDao` và `MainDispatcherRule` để kiểm tra luồng phát dữ liệu:
  - Giả lập việc thêm một danh mục tùy biến `"CUSTOM_1"` với tên `"Di chuyển cũ"`.
  - Giả lập việc chèn một giao dịch nằm trong danh mục `"CUSTOM_1"`.
  - Đăng ký lắng nghe luồng `observeAll()`.
  - Thực hiện chỉnh sửa đổi tên danh mục thành `"Di chuyển mới"`.
  - Kiểm thử xác nhận luồng tự động phát lại dữ liệu giao dịch mới với tên danh mục đã được đổi thành công mà không cần chỉnh sửa database.

### 2. Chạy toàn bộ Test Suite
* Thực hiện chạy bộ test tự động của toàn bộ ứng dụng qua Gradle:
  `.\gradlew.bat test`
* **Kết quả**: **BUILD SUCCESSFUL** với toàn bộ **129 tests passed 100%** thành công.
