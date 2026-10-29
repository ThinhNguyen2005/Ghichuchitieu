PHẦN 2: HƯỚNG DẪN TỐI ƯU, TÁI CẤU TRÚC VÀ VIẾT LẠI CODE

Sau khi gom file vào đúng thư mục, bạn cần tối ưu lại code bên trong theo 4 tiêu chuẩn vàng sau:

1. THÊM MỚI: DebtUiEvent.kt (Xóa bỏ việc truyền 10 hàm callback lẻ tẻ)

Hiện tại có thể bạn đang truyền từng callback: onAddClick, onItemClick, onRemindClick, onPayClick... Hãy gom lại thành 1 Sealed Interface duy nhất:

kotlin
// Tạo file: feature/debt/management/DebtUiEvent.kt
package com.notepay.feature.debt.management
sealed interface DebtUiEvent {
data class OnDebtClick(val debtId: Long) : DebtUiEvent
data object OnOpenCreateSheet : DebtUiEvent
data object OnDismissCreateSheet : DebtUiEvent
data class OnOpenRecordPayment(val debtId: Long) : DebtUiEvent
data class OnRemindDebt(val debtId: Long) : DebtUiEvent
data class OnDeleteDebt(val debtId: Long) : DebtUiEvent
}
2. TÁI CẤU TRÚC: DebtUiState.kt (Đóng gói toàn bộ trạng thái vào 1 State duy nhất)

Đừng để isSheetOpen, selectedDebtId trôi nổi trong Screen hay ViewModel. Gom hết vào UiState:

kotlin
// feature/debt/management/DebtUiState.kt
package com.notepay.feature.debt.management
import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Debt
@Immutable
data class DebtUiState(
val isLoading: Boolean = false,
val totalLendAmount: Long = 0L,        // Tổng cho vay
val totalBorrowAmount: Long = 0L,      // Tổng đi vay
val debtList: List<Debt> = emptyList(),
val isCreateSheetOpen: Boolean = false,
val selectedDebtForPayment: Debt? = null,
val errorMessage: String? = null
)
3. VIẾT LẠI: Tách DebtManagementScreen.kt thành 2 phần (Stateful & Stateless)

Đây là kỹ thuật quan trọng nhất trong Jetpack Compose để Preview hoạt động mượt và UI không bị dính chặt vào ViewModel:

kotlin
// feature/debt/management/DebtManagementScreen.kt
package com.notepay.feature.debt.management
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
// 1. STATEFUL: Nhiệm vụ duy nhất là kết nối với ViewModel và Navigation
@Composable
fun DebtManagementRoute(
viewModel: DebtViewModel,
onNavigateToDetail: (Long) -> Unit
) {
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
DebtManagementContent(
uiState = uiState,
onEvent = { event ->
when (event) {
is DebtUiEvent.OnDebtClick -> onNavigateToDetail(event.debtId)
else -> viewModel.onEvent(event)
}
}
)
}
// 2. STATELESS: Chỉ nhận State và gửi Event (KHÔNG NHẬN VIEWMODEL)
// Composable này có thể Preview và Test cực kỳ dễ dàng!
@Composable
fun DebtManagementContent(
uiState: DebtUiState,
onEvent: (DebtUiEvent) -> Unit
) {
// Render Scaffold / Column / DebtSummaryCard / DebtItemCard ở đây...

    // Hiển thị Sheet/Dialog dựa trên State:
    if (uiState.isCreateSheetOpen) {
        CreateDebtBottomSheet(
            onDismiss = { onEvent(DebtUiEvent.OnDismissCreateSheet) },
            onConfirm = { /* ... */ }
        )
    }
}
4. XÓA BỎ: Những thói quen xấu cần loại bỏ ngay
   ❌ Xóa bỏ việc truyền ViewModel vào BottomSheet / Dialog:
   CreateDebtBottomSheet(viewModel = viewModel)
   →
   → CẤM!
   Thay vào đó: Chỉ truyền dữ liệu và callback onConfirm = { amount, note -> ... }.
   ❌ Xóa bỏ các class trùng lặp: Kiểm tra xem DebtItemCard và DebtSummaryCard có đang tự tạo style màu hay bo góc riêng không. Hãy dùng chung token trong core.designsystem.theme.AppTheme.