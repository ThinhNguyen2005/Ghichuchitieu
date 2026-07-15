# Research: Liquid Glass UI v2

Tài liệu này tổng hợp các nghiên cứu công nghệ và quyết định thiết kế giao diện v2 cho dự án NotePay.

---

## Các Quyết Định Kỹ Thuật (Design Decisions)

### 1. Hệ thống lưới khoảng cách thống nhất (Spacing Grid)
*   **Quyết định**: Áp dụng lưới khoảng cách cơ sở 8dp làm quy chuẩn thống nhất cho tất cả các giá trị padding, margin, và spacer trong ứng dụng.
*   **Lý do chọn**: 
    *   Tạo nhịp điệu thị giác nhất quán trên tất cả các màn hình.
    *   Đơn giản hóa việc tính toán khoảng cách và căn chỉnh Responsive trên nhiều độ phân giải thiết bị Android khác nhau.
*   **Phương án thay thế đã cân nhắc**: Lưới 10dp hoặc lưới tự do (ad-hoc). Bị loại bỏ vì không tuân theo các quy chuẩn thiết kế phổ biến của Android/Material 3 và gây khó khăn khi tái sử dụng các components.

### 2. Đồng bộ kiểu chữ (Typography Alignment)
*   **Quyết định**: Sử dụng hệ thống font chữ mặc định của hệ thống Android (đã được tinh chỉnh trong Theme của Material 3) kết hợp với các tỷ lệ Type Scale chuẩn (Display, Headline, Title, Body, Label) thay vì nhúng font bên ngoài.
*   **Lý do chọn**:
    *   Giữ kích thước tệp APK ở mức tối giản (không tải thêm tệp font chữ TTF/OTF).
    *   Đảm bảo khả năng tương thích cao với cơ chế hiển thị chữ của hệ điều hành, tự động hỗ trợ Dynamic Type (phóng to chữ của hệ thống để hỗ trợ người khiếm thị).
*   **Phương án thay thế đã cân nhắc**: Nhúng font Google Fonts (Outfit hoặc Inter). Bị loại bỏ vì làm tăng kích thước nhị phân của ứng dụng một cách không cần thiết, vi phạm nguyên lý Ponytail.

### 3. Tối ưu hiệu ứng chuyển động và phản hồi xúc giác (Motion & Haptics)
*   **Quyết định**: Sử dụng các API chuyển động có sẵn của Jetpack Compose (`animateFloatAsState`, `tween` kết hợp `FastOutSlowInEasing`) cho các tương tác micro-animations (ví dụ: thu nhỏ nhẹ nút ấn xuống tỉ lệ `0.97f` khi người dùng chạm vào). Đồng thời kích hoạt phản hồi xúc giác nhẹ (`LocalHapticFeedback.current.performHapticFeedback`) cho các thao tác chạm bàn phím số và chuyển tab.
*   **Lý do chọn**:
    *   Đạt tần số quét 60fps mượt mà nhờ chạy trực tiếp trên luồng dựng hình tối ưu của Compose.
    *   Tăng cường cảm giác chân thực của mặt kính tương tác mà không gánh thêm chi phí hiệu năng.
*   **Phương án thay thế đã cân nhắc**: Sử dụng thư viện ngoài Lottie hoặc các thư viện chuyển động phức tạp. Bị loại bỏ do tăng độ phức tạp và kích thước tệp APK.

### 4. Giải pháp tương thích hiệu ứng kính (Fallback Blur) trên thiết bị yếu
*   **Quyết định**: Thiết lập cơ chế kiểm tra phiên bản Android SDK. Trên các thiết bị chạy Android API < 31 (hoặc các thiết bị cấu hình thấp được đánh dấu), hiệu ứng `RenderEffect.createBlurEffect` động sẽ tự động chuyển đổi sang lớp phủ màu bán trong suốt (semi-transparent solid background) kết hợp viền stroke mỏng.
*   **Lý do chọn**:
    *   Đảm bảo ứng dụng luôn chạy mượt mà ở mức 60fps trên mọi thiết bị mà không gây giật lag hoặc quá nhiệt.
    *   Duy trì tính thẩm mỹ Liquid Glass ở mức tương đối tốt trên các phiên bản Android cũ.
*   **Phương án thay thế đã cân nhắc**: Luôn kích hoạt blur động cho tất cả các thiết bị. Bị loại bỏ vì gây sụt giảm khung hình nghiêm trọng trên các máy cấu hình thấp.
