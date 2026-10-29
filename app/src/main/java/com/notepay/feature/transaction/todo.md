PHẦN 1: BẮT BỆNH THƯ MỤC transaction HIỆN TẠI
"Bẫy trùng lặp code" giữa add và edit:
Màn hình Thêm giao dịch (add) và Sửa giao dịch (edit) có giao diện giống nhau tới 90% (đều có: ô nhập số tiền, chọn ngày, chọn danh mục, tài khoản nguồn/ví, ghi chú, đính kèm ảnh hoá đơn).
Nếu để 2 thư mục add/ và edit/ riêng biệt, bạn sẽ bị nhân đôi code (duplicate). Sau này muốn thêm một tính năng (ví dụ: gắn thẻ tag/định vị), bạn sẽ phải sửa code ở 2 nơi.
calculator bị tách thành thư mục ngang hàng:
Bàn phím số / máy tính nhập tiền (calculator) chỉ là một widget con hỗ trợ người dùng gõ số tiền cho Form giao dịch, không phải là một màn hình độc lập.
AmountParser và CategorySuggestionUiMapper trôi nổi:
Hai file này thực chất là tiện ích tính toán và mapper gợi ý danh mục thông minh, nên được đặt đúng vào tầng hỗ trợ của Form nhập liệu.

(Lưu ý thêm: Ở nhánh trên, thư mục assets.main đang bị hiển thị dạng phẳng assets.main. Khi chuyển lên feature/assets, bạn nên để feature/assets/main để đồng bộ hoàn toàn với feature/debt và feature/billsplit).
PHẦN 4: BÍ QUYẾT TỐI ƯU CODE: GOM ADD VÀ EDIT LÀM MỘT (TransactionForm)


Thay vì viết 2 màn hình riêng biệt, bạn dùng kỹ thuật Id-Driven Form:

Trong TransactionFormViewModel.kt, nhận transactionId từ SavedStateHandle:

kotlin
// feature/transaction/form/TransactionFormViewModel.kt
@HiltViewModel
class TransactionFormViewModel @Inject constructor(
savedStateHandle: SavedStateHandle,
private val transactionRepository: TransactionRepository
) : ViewModel() {
// Nếu mở từ nút "+", transactionId sẽ là null (Chế độ ADD)
// Nếu mở từ màn hình chi tiết, transactionId sẽ có giá trị (Chế độ EDIT)
private val transactionId: Long? = savedStateHandle["transactionId"]
val isEditMode: Boolean = transactionId != null
private val _uiState = MutableStateFlow(TransactionFormUiState(isEditMode = isEditMode))
val uiState = _uiState.asStateFlow()
init {
if (isEditMode) {
loadTransactionData(transactionId!!)
}
}
private fun loadTransactionData(id: Long) {
viewModelScope.launch {
// Lấy dữ liệu cũ từ Room DB và điền vào form để sửa
}
}
fun onSaveTransaction() {
if (isEditMode) {
// Gọi repository.updateTransaction(...)
} else {
// Gọi repository.insertTransaction(...)
}
}
}

Lợi ích: Tiết kiệm hơn 50% lượng code, không bao giờ bị lệch giao diện giữa lúc tạo mới và lúc chỉnh sửa!