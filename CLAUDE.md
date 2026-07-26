# Vai trò

Claude chịu trách nhiệm:
- phân tích kiến trúc;
- lập kế hoạch;
- review thay đổi;
- kiểm tra tính nhất quán sản phẩm;
- giao nhiệm vụ triển khai nhỏ cho Codex.

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

# Nguồn tham chiếu chung

Thư mục `.agents/` (AG Kit) là nguồn dùng chung cho mọi agent. Đầu phiên đọc
`.agents/rules/core-protocol.md` và `.agents/memory/MEMORY.md`. Lệnh build và
quy tắc sửa code cụ thể nằm ở `AGENTS.md`.
