# Plan thêm ghi chú giao dịch theo danh mục

## Luồng cần đạt

**Thêm chi tiêu / thu nhập → Chọn danh mục → Nhập chi tiết → Lưu.** Khi người dùng chạm một danh mục, nội dung đổi ngay trong cùng màn hình, cùng route và cùng bản nháp. Bước nhập chi tiết hiển thị danh mục vừa chọn, số tiền, ghi chú, ví và ngày.

- Giả định phạm vi là hành động **Thêm chi tiêu / thu nhập** và các điểm vào trực tiếp `Route.AddTransaction`. Nút **+** toàn app hiện mở menu nhiều tác vụ trên cả điện thoại và tablet. Luồng mới bắt đầu khi người dùng chọn tác vụ thêm giao dịch trong menu đó. Sửa giao dịch, auto-capture, nhập tự nhiên và chuyển ví tiếp tục dùng luồng riêng của chúng.
- Ba ảnh đính kèm gợi ý cách dùng biểu tượng kèm tên để nhận diện danh mục và giữ dấu hiệu nhận diện đó khi nhập số tiền. Áp dụng bố cục, màu và bàn phím của NotePay. Các nút chuyển tiền, lặp lại, tag và bộ chọn tiền tệ trong ảnh chỉ là nội dung của mẫu tham khảo.

## Điểm sửa đã xác minh

| Điểm sửa | Hành vi hiện tại |
| --- | --- |
| [NotePayNavHost.kt](../app/src/main/java/com/notepay/core/navigation/NotePayNavHost.kt), [CreateNewBottomSheet.kt](../app/src/main/java/com/notepay/core/designsystem/component/action/CreateNewBottomSheet.kt) | Menu **+** gọi `Route.AddTransaction` từ tác vụ thêm chi tiêu / thu nhập. Shortcut và callback điều hướng hiện có cũng dùng route này. |
| [TransactionNavigation.kt](../app/src/main/java/com/notepay/feature/transaction/TransactionNavigation.kt), [TransactionFormScreen.kt](../app/src/main/java/com/notepay/feature/transaction/form/TransactionFormScreen.kt) | Add và Edit cùng dùng form toàn màn hình. Form đưa số tiền lên trước, mở `CategoryPickerSheet` cho danh mục và `NoteQuickEntrySheet` cho ghi chú. |
| [CategoryGridPicker.kt](../app/src/main/java/com/notepay/feature/transaction/component/CategoryGridPicker.kt) | Lưới đã có tìm theo tên bản địa hóa, lọc nhóm, danh mục tùy chỉnh và key theo ID. Bố cục hiện chốt 4 cột, cao tối đa 360dp và tên 11sp một dòng. |
| [TransactionFormSections.kt](../app/src/main/java/com/notepay/feature/transaction/component/TransactionFormSections.kt), [TransactionCustomKeypad.kt](../app/src/main/java/com/notepay/feature/transaction/component/TransactionCustomKeypad.kt) | Có sẵn `TransactionNoteField`, `TransactionWalletField` và bàn phím tính toán với phím ngày, ghi chú, lưu. |
| [TransactionFormUiState.kt](../app/src/main/java/com/notepay/feature/transaction/form/TransactionFormUiState.kt), [TransactionFormUiEvent.kt](../app/src/main/java/com/notepay/feature/transaction/form/TransactionFormUiEvent.kt), [TransactionFormViewModel.kt](../app/src/main/java/com/notepay/feature/transaction/form/TransactionFormViewModel.kt) | ViewModel phân biệt chọn danh mục chủ động với mặc định/gợi ý. Cổng lưu hiện xác thực số tiền, ví và ghi chú. `SavedStateHandle` hiện đọc ID giao dịch ở Edit. |

## Triển khai theo thứ tự

### 1. Quản lý bước và bản nháp trong ViewModel

- Thêm trạng thái bước cho Add, khởi tạo ở **Chọn danh mục** với loại **Chi**. Edit vào form hiện có. Bước là trạng thái riêng với danh mục đã xác nhận vì người dùng có thể quay lại lưới trong khi bản nháp vẫn có danh mục.
- Ở Add, dùng sự kiện chọn danh mục hiện có để cập nhật danh mục và bước trong cùng lần cập nhật state. Kiểm tra danh mục khớp loại Thu/Chi và catalog hiện tại. Người dùng có thể chọn lại chính ô đang đánh dấu để tiếp tục. Danh mục mặc định, kể cả **Khác**, và gợi ý từ ghi chú chỉ tiến bước sau thao tác chọn của người dùng.
- **Đổi danh mục** và **Quay lại** từ chi tiết mở lại lưới, giữ số tiền hoặc biểu thức đang nhập, ghi chú, ngày, ví và dấu chọn hiện tại. Khi đổi Thu/Chi trong lưới, yêu cầu xác nhận danh mục thuộc loại mới và giữ các trường còn lại. Nếu catalog ngừng cung cấp danh mục đã xác nhận, trở về bước chọn và giữ bản nháp.
- Giữ tạo danh mục tùy chỉnh qua callback hiện có. Tạo thành công có thể xác nhận danh mục mới và chuyển bước nếu người dùng vẫn ở lần chọn và loại giao dịch tương ứng. Xử lý kết quả đến muộn sau khi đổi loại, chọn ô khác hoặc rời bước. Với lỗi hoặc trùng tên, ở lại lưới và dùng phản hồi lỗi hiện có. Phân biệt danh mục vừa tạo thành công với danh mục bị xóa trong lúc chờ catalog phát dữ liệu mới.
- Chặn sự kiện Save ở ViewModel khi Add chưa ở bước chi tiết hoặc chưa có danh mục hợp lệ đã xác nhận. Giữ thứ tự tính biểu thức, xác thực và lưu qua `AddTransactionUseCase`. `canSave` và trạng thái nút phải tính đến bước mới, nhưng vẫn cho phép đường xử lý biểu thức đang chờ tính và phản hồi validation hiện có. Giữ chặn lưu lặp và phân quyền ghi ví ở đường lưu thực tế.
- Cùng một ViewModel giữ bản nháp khi chuyển bước và xoay màn hình. Dùng cơ chế saveable của Compose cho trạng thái giao diện nhẹ như vị trí cuộn/tìm kiếm nếu cần giữ qua chuyển bước. Phạm vi của plan là bản nháp trong phiên Add, gồm tái tạo do đổi cấu hình. Khi tiến trình bị hệ thống hủy, phiên Add khởi tạo theo hành vi hiện tại. [Android Developers – Save UI state](https://developer.android.com/develop/ui/compose/state-saving)

### 2. Thay nội dung trong màn Add hiện tại

- Đặt nền `AppAtmosphericBackground(forceSoftBlurOnPhoto = true)`, header, snackbar và launcher OCR bên ngoài phần đổi bước của `TransactionFormScreen`. Dùng route hiện có và `Scaffold` gốc. Toàn bộ chuyển tiếp **danh mục → chi tiết** diễn ra bên trong phần nội dung. Sheet chọn ví và bộ chọn ngày vẫn là điều khiển phụ trên form chi tiết.
- **Chọn danh mục.** Đặt Thu/Chi ở đầu, dưới đó là lưới icon và tên. Tận dụng lưới hiện có và thêm cấu hình bố cục cho chỗ nhúng toàn màn hình. Dùng một vùng cuộn dọc hữu hạn cho lưới, để lưới tận dụng phần còn lại của màn hình. Điều chỉnh số cột theo chiều rộng và font scale, cho tên xuống dòng và giữ đủ tên trong semantics. Giữ tìm kiếm, lọc nhóm và tạo danh mục. Reset bộ lọc nhóm về **Tất cả** khi đổi loại để nhóm Chi khỏi che danh mục Thu. Tách trạng thái đang tải, danh mục trống và tìm kiếm không có kết quả.
- **Chi tiết.** Đặt avatar + tên danh mục và hành động **Đổi danh mục** trên hero v5 cùng số tiền. Tái sử dụng `TransactionNoteField` để hiển thị ghi chú trống hoặc có nội dung ngay trong form. Phím ghi chú của keypad chuyển focus vào trường này. Giữ chức năng tính toán, ngày và lưu của keypad, cùng chọn ví bằng `TransactionWalletField` và `WalletPickerSheet`. Nếu không có ví có quyền ghi theo chính sách hiện tại, hiển thị trạng thái rõ ràng tại trường ví và giữ bản nháp. Khi thiếu mạng, hiển thị kết quả theo chính sách lưu của loại ví hiện tại.
- Cho OCR xuất hiện ở bước chi tiết. Kết quả có thể điền số tiền và ghi chú theo quy tắc sẵn có. Danh mục người dùng đã xác nhận được giữ khi sửa ghi chú hoặc khi OCR trả kết quả. Giữ định dạng số tiền và các nhánh báo lỗi hiện có.
- Header back và system back dùng cùng quy tắc. Đóng IME hoặc điều khiển phụ đang mở trước, sau đó **Chi tiết → Chọn danh mục**, rồi **Chọn danh mục → màn gọi**. Thêm `BackHandler` cấp nội dung cho bước chi tiết, kiểm tra thứ tự ưu tiên với `PredictiveBackDestination` đang bọc route để gesture hoàn tất chỉ đổi bước một lần. Kiểm tra cả gesture hủy, nút Back 3 phím và thân màn có focus. [Android Developers – Activity Compose Back APIs](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary)
- Dùng màu, kiểu chữ và shape v5. Hero giữ gradient token sáng/tối và số dạng tabular. Màu danh mục đi cùng avatar, tên và dấu chọn. Dùng string resource VI/EN cho nhãn bước, hành động đổi danh mục và trạng thái mới. [DESIGN.md](../DESIGN.md)

## Hiệu ứng

- `AnimatedContent` nhận trạng thái bước, đọc đối số của content lambda để quyết định nội dung. Giữ vùng chứa ổn định và dùng fade kết hợp dịch ngắn theo chiều tới/lùi. Tái sử dụng `NotePayMotion.contentDurationMillis` (200 ms), `contentTranslation` (12dp) và easing hiện có. Bảo vệ khỏi hai lần chạm liên tiếp ở nội dung đang rời màn hình bằng kiểm tra step tại ViewModel.
- Khi giảm chuyển động, đổi nội dung ngay qua helper `rememberNotePayReducedMotion()`. Ô danh mục dùng dấu chọn và phản hồi nhấn có sẵn. Nếu giữ rung khi chọn, dồn về một lần qua `rememberNotePayHaptic()`. Rà lại hiệu ứng phóng của ô hiện tại khi dùng lưới trong Add. [NotePayMotion.kt](../app/src/main/java/com/notepay/core/navigation/NotePayMotion.kt), [Android Developers – Animation modifiers and composables](https://developer.android.com/develop/ui/compose/animation/composables-modifiers)

## Trợ năng

- Tiêu đề bước có `heading()`. Thu/Chi là nhóm chọn một, ô danh mục có trạng thái `selected`, vai trò và tên đầy đủ. Gộp icon trang trí và tên vào một node hành động. Nút chỉ có icon dùng mô tả bản địa hóa. Các ô, phím số và nút có vùng chạm ít nhất 48 × 48dp, kể cả cửa sổ hẹp. Dùng dấu chọn hoặc chữ đi cùng màu.
- Thứ tự đọc bước chi tiết đi từ tiêu đề và danh mục đến số tiền, ghi chú, ví, ngày và bàn phím/lưu. Trong lúc chuyển cảnh, bước đang rời không nhận thao tác hay nằm trong thứ tự đọc. Kiểm tra TalkBack báo tiêu đề hoặc một thông báo ngắn khi đổi bước, rồi vuốt được tới trường đầu tiên. Nếu tiêu đề chưa được thông báo, thử live region `Polite` hẹp cho thông báo bước. `FocusRequester` phục vụ focus nhập liệu của phím ghi chú. Mở form chi tiết để IME đóng sẵn cho bàn phím số nội bộ.
- Báo lỗi bằng nội dung nhìn thấy được và semantics `error` khi phù hợp. Giữ ô ghi chú và các trường quan trọng cuộn tới được khi IME mở, dùng inset trên viewport cuộn và kiểm tra cả điều hướng cử chỉ lẫn ba phím. Kiểm tra chữ VI/EN ở 1.3× và 2.0×, light/dark, ảnh nền tùy chỉnh, màn nhỏ và tablet. Chữ nội dung tối thiểu 12sp, tương phản tối thiểu 4.5:1. [Android Developers – Semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics), [Merging and clearing](https://developer.android.com/develop/ui/compose/accessibility/merging-clearing), [Programmatically request focus](https://developer.android.com/develop/ui/compose/touch-input/focus/request-focus), [Window insets](https://developer.android.com/develop/ui/compose/system/insets-ui)

## Nghiệm thu

| Kiểm tra | Kết quả mong đợi |
| --- | --- |
| Tạo Chi và Thu từ Add | Thấy lưới đầu tiên. Chạm một ô mở chi tiết trên cùng route và lưu đúng loại, danh mục, số tiền, note, ví, ngày. |
| Đổi danh mục hoặc loại sau khi nhập | Biểu thức đang nhập, ghi chú, ví và ngày còn nguyên. Chỉ thao tác chọn danh mục phù hợp mới trở lại chi tiết. Xoay thiết bị ở cả hai bước giữ đúng bản nháp. |
| Dữ liệu biên | Kiểm tra mặc định **Khác**, danh mục tùy chỉnh thành công/trùng/lỗi/kết quả đến muộn, tìm kiếm rỗng, 0/1/nhiều ví ghi được, gợi ý theo ghi chú và OCR đến muộn. |
| Lưu và hồi quy | Kiểm tra Save trực tiếp trước khi chọn, biểu thức chưa bấm bằng, chia 0, lỗi lưu và thử lại, bấm lưu nhanh hai lần, Edit thường và khóa số tiền ở auto-capture. |
| Thiết bị | Kiểm tra Back/gesture, TalkBack, điều hướng bằng bàn phím hoặc D-pad, IME, chạm nhanh hai ô, animator scale 0 và ảnh light/dark. Rà Hạng mục 1–6 và cập nhật mục Thêm / Sửa giao dịch khi đã xác nhận. |

- Bổ sung test chuyển bước và giữ bản nháp ở ViewModel. Cập nhật setup Add của test calculator để chọn danh mục trước khi test Save. Thêm một Compose test kiểm tra lưới → chi tiết → quay lại và semantics của ô. Xác minh thông báo TalkBack trên thiết bị. [TransactionFormViewModelCalculatorTest.kt](../app/src/test/java/com/notepay/feature/transaction/form/TransactionFormViewModelCalculatorTest.kt), [Android Developers – Test Compose accessibility](https://developer.android.com/develop/ui/compose/accessibility/testing)
- Chạy `./gradlew.bat detekt :app:lintFullDebug :app:testFullDebugUnitTest :app:assembleFullDebug`. Thêm `:app:lintPlayDebug :app:testPlayDebugUnitTest` khi `com.notepay.debug` có trong `app/google-services.json`. Chạy `./gradlew.bat :app:connectedFullDebugAndroidTest` khi có thiết bị hoặc emulator. [ui-ux-audit-guide.md](../ui-ux-audit-guide.md)

Plan đối chiếu checkout `5739aae`. Subagent dùng codebase-memory và đọc checkout để xác minh những file form đã đổi so với index. Hướng dẫn Compose được tra qua Context7 và liên kết tới Android Developers tại các mục tương ứng. Đường dẫn `.agents/skills/android-pro/SKILL.md` được nêu trong AGENTS.md hiện thiếu trong checkout.
