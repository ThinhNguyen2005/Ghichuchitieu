# Vai trò

Claude chịu trách nhiệm:
- phân tích kiến trúc;
- lập kế hoạch;
- review thay đổi;
- kiểm tra tính nhất quán sản phẩm;
- giao nhiệm vụ triển khai nhỏ cho Codex.

Gọi Codex là "bạn Codex".

# Kiến trúc

- Kotlin
- Jetpack Compose
- Material 3
- domain không phụ thuộc Android
- data triển khai repository
- UI không chứa business logic
- ViewModel điều phối state, không parse dữ liệu

# Nguyên tắc

- Không thay đổi kiến trúc khi chưa có kế hoạch.
- Không tạo abstraction nếu chỉ có một trường hợp sử dụng.
- Không sửa ngoài phạm vi task.
- Mỗi đợt refactor phải giữ nguyên hành vi hoặc ghi rõ hành vi thay đổi.

# Hai biến thể sản phẩm

Repo dùng hai product flavor trên cùng nhánh, chiều `distribution`:

- `play`: có Internet, dùng ảnh VietQR qua mạng; không có mã đọc thông báo.
- `local`: offline, có đọc thông báo; `applicationIdSuffix = ".local"`.

Mã đọc thông báo và resource riêng nằm trong `app/src/local/`; test riêng
nằm trong `app/src/testLocal/`. Play dùng `app/src/play/`.
Giữ UI/nghiệp vụ dùng chung trong `app/src/main/`.
Chỉ thanh điều hướng được dùng Liquid Glass khi tương thích và người dùng bật.
AI API cho Play chờ quyết định nhà cung cấp/cấu hình; chưa triển khai.

# Lệnh

```
./gradlew.bat :app:assemblePlayDebug
./gradlew.bat :app:testPlayDebugUnitTest
./gradlew.bat :app:assembleLocalDebug
./gradlew.bat :app:testLocalDebugUnitTest
```

Play không chạy test đọc thông báo; thay đổi code chung phải kiểm tra cả hai bản.

# Kiểm chứng

- Tự chạy lại build/test. Không kết luận dựa trên báo cáo của agent khác.
- Đọc file kết quả test **mới nhất theo thời gian**; lấy file đầu tiên tìm được
  dễ ra báo cáo cũ và kết luận sai.
- Muốn chứng minh bản `play` thật sự không có quyền đọc thông báo thì xem
  manifest đã merge, không xem file nguồn.

# Chuỗi và ngôn ngữ

- `values/` là tiếng Việt, `values-en/` là tiếng Anh. Số key hai file phải bằng nhau.
- Chuỗi có đếm số phải dùng `<plurals>`: tiếng Việt chỉ cần `other`, tiếng Anh
  cần cả `one` và `other`.
- Dấu nháy đơn trong chuỗi phải escape thành `\'`, nếu không `mergeResources` lỗi.
- Trước khi thêm key mới, kiểm tra key đã tồn tại chưa — phần lớn chuỗi hardcode
  trong repo này đã có key sẵn nhưng chưa được nối vào.

Bốn nhóm chuỗi tiếng Việt **không** được đưa vào `strings.xml`:

- Từ khóa dò OCR trong `LocalTransactionImageScanner` (`amountMarkers`,
  `balanceMarkers`, …) — cố tình có cả biến thể không dấu; localize là làm hỏng
  tính năng quét ảnh sao kê.
- Tên danh mục và ví mặc định (`Category.kt`, `Wallet.kt`) — seed data ghi vào
  database, đổi là bài toán migration.
- Prompt gửi cho model AI (`ai/`, `LocalAiModelManager`).
- Tên ngân hàng trong `VietQrBankRepository` — danh từ riêng.

# Tiền

`Money` lưu theo đơn vị nhỏ (xu) và dùng arithmetic exact, **ném
`ArithmeticException` khi tràn** thay vì để `Long` wrap âm thầm. Số tiền thật
không bao giờ tới gần giới hạn `Long`, nên tràn nghĩa là dữ liệu hỏng. Đừng đổi
policy này thành trả về giá trị bão hòa.

# Skill

Skill lấy từ bộ Anthropic đã cài toàn cục, Claude tự nhận theo mô tả — gợi ý sử dụng skill phù hợp để tối ưu hơn
thư mục nào trong repo. Việc UI/thiết kế dùng `frontend-design`.

# Ghi chú

`AGENTS.md` là file dành cho bạn Codex: lệnh build và quy tắc sửa code khi giao việc.
