# Hướng dẫn mở rộng và cấu hình Nhận diện biến động số dư qua Thông báo

Tài liệu này hướng dẫn chi tiết về cấu trúc hệ thống tự động nhận diện giao dịch (Local Parse) từ thông báo điện thoại của NotePay, cách hệ thống gạch nợ hoạt động, và cách mở rộng cấu hình cho nhiều ngân hàng khác trong tương lai.

---

## 1. Kiến trúc Hệ thống Nhận diện Thông báo

Hệ thống hoạt động 100% offline (local parse) trên thiết bị người dùng để bảo vệ quyền riêng tư tuyệt đối, gồm 2 thành phần chính:

```
[Thông báo hệ thống (SBN)] 
         │
         ▼
[NotePayNotificationListenerService] ──► (Trích xuất text, title, textLines)
         │
         ▼
[NotificationParser] ──────────────────► (Dùng Regex trích xuất Số tiền, Loại GD, Ghi chú)
         │
         ├─► [Nếu GD THU NHẬP] ────────► [Đối soát gạch nợ Chia tiền]
         │                                       ├─► Khớp đơn lẻ: memoCode (VD: "NP12 DUC")
         │                                       └─► Khớp gộp: "NP <TÊN_NGƯỜI_NỢ>" (VD: "NP DUC")
         └─► [Giao dịch thường] ───────► [Thêm giao dịch tự động vào Ví hiện tại]
```

### Các lớp chính:
1. **[NotePayNotificationListenerService.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/service/NotePayNotificationListenerService.kt):** Kế thừa `NotificationListenerService` của Android, lắng nghe thông báo từ các ứng dụng ngân hàng và Momo. Nó gộp các trường văn bản (`android.text`, `android.bigText`, `android.textLines` cho các thông báo nhiều dòng) để gửi sang bộ parser.
2. **[NotificationParser.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/domain/notification/NotificationParser.kt):** Sử dụng các biểu thức chính quy (Regex) để bóc tách thông tin giao dịch (số tiền, loại thu/chi, nội dung chuyển khoản).

---

## 2. Cách mở rộng nhận diện cho ngân hàng mới

Hiện tại, `NotificationParser` hỗ trợ:
- **Ngân hàng chung (Regex chung):** Nhận diện các từ khóa `GD`, `Giao dịch`, `PS` kèm dấu `+` hoặc `-` và số tiền (ví dụ: `PS: -30.000VND`, `GD +150,000 VND`).
- **Ví Momo:** Nhận diện tin nhắn thanh toán/chuyển tiền/nhận tiền từ ứng dụng Momo.

Để thêm một ngân hàng có định dạng tin nhắn đặc biệt hoặc tối ưu hóa độ chính xác, bạn chỉ cần sửa đổi tệp [NotificationParser.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/domain/notification/NotificationParser.kt) theo các bước sau:

### Bước 1: Khai báo Regex định dạng tin nhắn của ngân hàng đó
Ví dụ, tin nhắn biến động của Vietcombank thường có dạng:
`"SD TK 0123... -100,000 VND luc 14-06-2026. Ref: NP4 DUC..."`

Ta thêm Regex:
```kotlin
private val VCB_TRANSACTION_REGEX = Regex(
    """TK\s+\d+\s*([+-])\s*([0-9.,\s]+)\s*(?:VND|đ)""",
    RegexOption.IGNORE_CASE
)
```

### Bước 2: Tích hợp vào hàm `parse` chính
Trong hàm `parse(title: String?, body: String?)`:
```kotlin
// Thêm kiểm tra Vietcombank
val vcbMatch = VCB_TRANSACTION_REGEX.find(normalizedBody)
if (vcbMatch != null) {
    val sign = vcbMatch.groupValues[1]
    val amountStr = vcbMatch.groupValues[2]
    val amount = parseAmount(amountStr) ?: return null
    val type = if (sign == "+") TransactionType.INCOME else TransactionType.EXPENSE
    val note = extractNote(normalizedBody) ?: title ?: "Giao dịch Vietcombank"
    return ParsedNotification(amount, type, note)
}
```

---

## 3. Cơ chế tự động đối soát gạch nợ Chia tiền

Khi hệ thống nhận dạng được một thông báo **Nhận tiền (INCOME)**, hệ thống sẽ tự động chuyển sang luồng đối soát chia tiền:

### 3.1. Đối soát đơn lẻ (Khớp mã giao dịch gốc):
- Mã đối soát đơn lẻ có dạng: `NP{transactionId} {TÊN_NGƯỜI_NỢ}` (ví dụ: `NP12 DUC`).
- Trực tiếp đối khớp mã này trong nội dung thông báo (`textToParse`). Nếu khớp, hệ thống đánh dấu khoản chia tiền đó là đã thanh toán, đồng thời tạo giao dịch thu nhập tương ứng để cộng lại số dư cho ví.

### 3.2. Đối soát gộp (Thanh toán gộp nhiều khoản nợ):
- Mã đối soát gộp có dạng: `NP {TÊN_NGƯỜI_NỢ}` (ví dụ: `NP DUC` hoặc `NP DUC ANH`).
- Khi phát hiện thông báo nhận tiền chứa mã gộp này, hệ thống sẽ thực hiện truy vấn tất cả các khoản nợ chưa thanh toán (`unpaidSplits`) của người có tên trùng khớp (không phân biệt chữ hoa/thường và đã loại bỏ dấu tiếng Việt).
- **Hành động:** Gạch nợ toàn bộ các khoản nợ của người đó cùng một lúc và tạo các giao dịch thu nhập bù lại số dư tương ứng.

---

## 4. Cách kiểm thử và mô phỏng thông báo (Debug)

Để kiểm thử tính năng này trên Emulator hoặc thiết bị thật mà không cần chuyển khoản thật:
1. Trên màn hình Trang chủ (`HomeScreen`), bấm nút **"Gửi test"** (nút này chỉ xuất hiện khi ứng dụng đã được cấp quyền đọc thông báo và đang chạy chế độ tự động).
2. Hàm `simulateTpBankNotification` trong [HomeScreen.kt](file:///d:/Ghichuchitieu/app/src/main/java/com/notepay/ui/feature/home/HomeScreen.kt) sẽ đẩy một thông báo giả lập của TPBank với nội dung:
   ```
   (TPBank): 14/06/26;06:25
   TK: xxxx5539020
   PS:-30.000VND
   ND: NAP TIEN VI MOMO...
   ```
3. Bạn có thể sửa nội dung ND này thành mã đối soát của bạn (ví dụ: `NP DUC`) để test tính năng gạch nợ tự động chạy ngầm.
