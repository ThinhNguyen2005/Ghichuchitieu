PHẦN 3: HƯỚNG DẪN TỐI ƯU CODE (THÊM, SỬA, XÓA)
1. TẠO MỚI: BillSplitUiEvent.kt

Màn hình chia tiền có rất nhiều tương tác (mở QR, mở đối soát, bấm back, chọn giao dịch). Đừng truyền 10 lambda rời rạc, hãy gom vào 1 Event duy nhất:

kotlin
// feature/billsplit/main/BillSplitUiEvent.kt
package com.notepay.feature.billsplit.main
sealed interface BillSplitUiEvent {
data object OnOpenCreateSheet : BillSplitUiEvent
data object OnDismissCreateSheet : BillSplitUiEvent
data object OnOpenQrConfig : BillSplitUiEvent
data object OnDismissQrConfig : BillSplitUiEvent
data object OnOpenReconciliation : BillSplitUiEvent
data object OnDismissReconciliation : BillSplitUiEvent
data object OnBackClicked : BillSplitUiEvent
data object OnConfirmDiscard : BillSplitUiEvent
data object OnDismissDiscardDialog : BillSplitUiEvent
data class OnSelectTransaction(val transactionId: Long) : BillSplitUiEvent
data class OnToggleDebtorPaid(val debtorId: Long) : BillSplitUiEvent
}
2. TỐI ƯU: BillSplitScreen.kt (Chuẩn hoá Stateless & Stateful)

Tách rõ ràng Route (nhận ViewModel) và Content (chỉ nhận State + Event) để Preview không bị crash:

kotlin
// feature/billsplit/main/BillSplitScreen.kt
package com.notepay.feature.billsplit.main
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
@Composable
fun BillSplitRoute(
viewModel: BillSplitViewModel,
onNavigateBack: () -> Unit,
onNavigateToDebtorDetail: (Long) -> Unit
) {
val uiState by viewModel.uiState.collectAsStateWithLifecycle()
BillSplitContent(
uiState = uiState,
onEvent = { event ->
when (event) {
is BillSplitUiEvent.OnBackClicked -> onNavigateBack()
else -> viewModel.onEvent(event)
}
},
onDebtorClick = onNavigateToDebtorDetail
)
}
@Composable
fun BillSplitContent(
uiState: BillSplitUiState,
onEvent: (BillSplitUiEvent) -> Unit,
onDebtorClick: (Long) -> Unit
) {
// 1. Giao diện chính với Scroller, Selector, List, Footer
// 2. Gọi các Sheet/Dialog dựa trên State:
if (uiState.isDiscardDialogOpen) {
BillSplitDiscardDialog(
onConfirm = { onEvent(BillSplitUiEvent.OnConfirmDiscard) },
onDismiss = { onEvent(BillSplitUiEvent.OnDismissDiscardDialog) }
)
}

    if (uiState.isQrConfigOpen) {
        VietQrConfigFullScreenDialog(
            onDismiss = { onEvent(BillSplitUiEvent.OnDismissQrConfig) }
        )
    }
}
3. XÓA BỎ VÀ LƯU Ý
   Cấm truyền ViewModel vào các Sheet: PaymentReconciliationSheet, VietQrConfigFullScreenDialog hay BillSplitCreateSheet chỉ nhận dữ liệu hiển thị và callback xác nhận (onDismiss, onConfirm). Tuyệt đối không truyền viewModel vào bên trong các dialog này.