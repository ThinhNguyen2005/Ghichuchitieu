# Sao lưu và khôi phục dữ liệu

NotePay xuất bản sao lưu dạng JSON có phiên bản (`version`). Người dùng chọn nơi lưu và chọn file để nhập thông qua Android Storage Access Framework (SAF); ứng dụng chỉ nhận file JSON và giới hạn kích thước đọc là 20 MB.

## Dữ liệu được sao lưu

- Ví, giao dịch, chia tiền và đăng ký định kỳ.
- Danh mục tùy chỉnh.
- Màu giao diện, màu tùy chỉnh và thói quen danh mục.

Các thiết lập khác không có trong file sao lưu sẽ không bị thay đổi khi khôi phục.

## Quy trình khôi phục

1. Đọc file từ URI SAF và kiểm tra giới hạn kích thước.
2. Kiểm tra phiên bản và các trường bắt buộc trước khi ghi dữ liệu.
3. Trong một `RoomDatabase.withTransaction`, xóa theo thứ tự khóa ngoại rồi nhập ví, giao dịch, chia tiền và đăng ký. Nếu bất kỳ thao tác Room nào thất bại, Room rollback toàn bộ phần dữ liệu này.
4. Thay thế toàn bộ danh mục tùy chỉnh bằng danh sách trong file, không gộp với danh mục cũ.
5. Áp dụng các thiết lập có trong file sao lưu.

Vì danh mục tùy chỉnh và thiết lập được lưu trong SharedPreferences, chúng không thuộc transaction SQLite/Room. Dữ liệu Room vẫn được kiểm tra trước và khôi phục nguyên tố; danh mục được ghi một lần bằng `commit()` trước khi cập nhật bộ nhớ đệm.

## Lưu ý WAL

Không sao chép trực tiếp file SQLite khi ứng dụng đang dùng WAL: dữ liệu có thể còn ở các file `-wal` và `-shm`. Định dạng JSON của NotePay tránh phụ thuộc vào việc checkpoint file SQLite và phù hợp để chia sẻ qua SAF.
