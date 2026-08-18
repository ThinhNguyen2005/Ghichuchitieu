# Feature Specification: Delight Motion & Smart Features

**Feature Directory**: `specs/003-delight-motion-and-smart-features`
**Created**: 2026-08-18
**Status**: Draft / Proposed
**Input**: Yêu cầu người dùng: Nâng cấp trải nghiệm cảm xúc và chuyển động (Delight UI/UX), cho phép đặt ảnh nền cá nhân cho Thẻ Số Dư có xử lý tương phản, làm lại hiệu ứng vuốt (Swipe-to-action) bám sát ngón tay mượt mà có đàn hồi, kiểm tra tương thích Android và công tắc bật/tắt Liquid Glass trong Cài đặt, Shared Element Transition giữa Danh sách và Chi tiết giao dịch, thông báo nhắc nhở Offline và chuỗi Streak ghi chép hàng ngày.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Cá Nhân Hóa Thẻ Số Dư với Ảnh Nền & Tương Phản Chuẩn (Priority: P1)
Người dùng muốn Thẻ Số Dư (Hero Balance Card) trên trang chủ mang dấu ấn cá nhân bằng cách chọn một bức ảnh kỷ niệm hoặc phong cảnh làm nền thẻ. Tuy nhiên, dù ảnh nền sáng, tối hay nhiều chi tiết, các thông tin số dư, tên ví, doanh thu, chi phí vẫn phải sắc nét, dễ đọc (đạt chuẩn WCAG AA).

* **Independent Test**: Chọn các loại ảnh nền khác nhau (rất sáng, rất tối, nhiều họa tiết) ➔ Quan sát lớp phủ bảo vệ tương phản (scrim gradient & blur nhẹ) đảm bảo tất cả chữ và số luôn đọc rõ ràng ở cả Light Theme và Dark Theme.
* **Acceptance Scenarios**:
  1. **Given** người dùng ở màn hình Trang chủ hoặc Chi tiết Ví, **When** họ bấm đổi ảnh nền thẻ, **Then** hệ thống mở Photo Picker chuẩn AndroidX để chọn ảnh an toàn.
  2. **Given** ảnh nền đã được áp dụng, **When** số dư thay đổi, **Then** số tiền hiển thị hiệu ứng cuộn số (Rolling Number) mượt mà trên nền thẻ.

---

### User Story 2 - Cử Chỉ Vuốt Thao Tác Mượt Mà Bám Ngón Tay (Priority: P1)
Khi người dùng vuốt một dòng giao dịch trong danh sách để Xóa hoặc Sửa, item phải di chuyển 1:1 theo ngón tay một cách tự nhiên. Khi kéo quá ngưỡng, có lực cản đàn hồi (rubber-band resistance) và phản hồi rung haptic tinh tế. Nếu buông tay hoặc kéo ngược lại, chuyển động ngắt tức thì không bị đơ giật.

* **Independent Test**: Dùng ngón tay vuốt chậm, vuốt nhanh, giữ ngón tay và kéo qua lại trên item giao dịch. Quan sát độ bám và chuyển động lò xo.
* **Acceptance Scenarios**:
  1. **Given** danh sách giao dịch, **When** người dùng vuốt sang trái, **Then** item trượt mượt mà lộ nút Xóa/Sửa, rung haptic nhẹ khi chạm ngưỡng kích hoạt.
  2. **Given** đang trong chuyển động trượt về, **When** người dùng chạm tay vào, **Then** chuyển động dừng lại ngay lập tức (interruptible) để nhận cử chỉ mới.

---

### User Story 3 - Kiểm Tra Tương Thích OS & Công Tắc Liquid Glass (Priority: P2)
Người dùng trên các thiết bị Android 12 trở lên có thể tận hưởng hiệu ứng kính mờ (Liquid Glass / RenderEffect / Blur) cao cấp và có thể chủ động bật/tắt trong Cài đặt. Trên các thiết bị Android 8-11 (API < 31), app tự động phát hiện và hiển thị switch ở trạng thái disabled kèm ghi chú thân thiện, đồng thời áp dụng giao diện phẳng mờ tối ưu hiệu năng 60fps.

* **Independent Test**: Kiểm tra màn hình Cài đặt trên cả thiết bị Android 12+ và Android 11 trở xuống.
* **Acceptance Scenarios**:
  1. **Given** máy chạy Android 12+, **When** mở Cài đặt Giao diện, **Then** switch "Hiệu ứng kính mờ (Liquid Glass)" bật/tắt bình thường.
  2. **Given** máy chạy Android 8-11, **When** mở Cài đặt, **Then** switch bị vô hiệu hóa (xám đi) và hiển thị dòng chữ *"Yêu cầu Android 12 trở lên"*, giao diện app tự động dùng theme phẳng chống giật lag.

---

### User Story 4 - Chuyển Cảnh Liền Mạch Shared Element Transition (Priority: P2)
Khi người dùng chạm vào một giao dịch bất kỳ trong Danh sách hoặc Trang chủ, toàn bộ thẻ giao dịch nở rộng liền mạch (expand) thành màn hình Chi tiết giao dịch (`TransactionDetailScreen`), icon danh mục trượt vào vị trí tiêu đề, loại bỏ hoàn toàn cảm giác chớp màn hình.

* **Independent Test**: Bấm vào item giao dịch và bấm nút Back ➔ Quan sát animation phóng to và thu nhỏ mượt mà.

---

### User Story 5 - Nhắc Nhở Thông Minh & Chuỗi Ngày Ghi Chép (Streak 🔥) (Priority: P3)
App chủ động nhắc nhở người dùng vào 20:30 tối mỗi ngày nếu hôm đó chưa có giao dịch nào được ghi chép, giúp duy trì thói quen quản lý tài chính. Trang chủ hiển thị huy hiệu ngọn lửa (Streak 🔥) đếm số ngày ghi chép liên tiếp để kích thích động lực.

* **Independent Test**: Ghi chép giao dịch trong 3 ngày liên tiếp ➔ Kiểm tra hiển thị chuỗi Streak 3 ngày. Kiểm tra thông báo xuất hiện đúng giờ mà không cần kết nối mạng.
