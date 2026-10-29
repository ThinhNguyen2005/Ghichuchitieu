package com.notepay.feature.debt.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.money.Money
import com.notepay.domain.model.Debt
import com.notepay.domain.model.DebtPayment
import com.notepay.domain.model.DebtType
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.core.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import javax.inject.Inject

sealed interface DebtDetailUiEvent {
    data class ShowToast(val messageResId: Int) : DebtDetailUiEvent
    data object PaymentRecorded : DebtDetailUiEvent
    data object DebtDeleted : DebtDetailUiEvent
    data object PaymentDeleted : DebtDetailUiEvent
}

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val walletRepository: WalletRepository,
    private val vietQrBankRepository: VietQrBankRepository,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * Route độc lập lấy id từ nav args; ở chế độ hai pane thì [select] được gọi khi người
     * dùng chọn một khoản nợ trong danh sách.
     */
    private val initialDebtId: Long? = savedStateHandle.get<Long>(Route.DebtDetail.ARG_ID)
        ?: savedStateHandle.get<String>(Route.DebtDetail.ARG_ID)?.toLongOrNull()

    private val selectedDebtId = MutableStateFlow(initialDebtId)

    private val _events = MutableSharedFlow<DebtDetailUiEvent>()
    val events = _events.asSharedFlow()

    private val _isDeleted = MutableStateFlow(false)
    private val _banks = MutableStateFlow(VietQrBankRepository.FALLBACK)

    /**
     * `true` trong khoảng thời gian giữa lúc đổi lựa chọn và lúc Room phát bản ghi mới.
     * Không có cờ này thì `combine` giữ dữ liệu của khoản nợ trước đó vài frame, đủ để
     * người dùng thấy thoáng tên/số tiền của người khác.
     */
    private val _isSwitching = MutableStateFlow(false)

    /**
     * Đổi khoản nợ đang hiển thị. `null` làm luồng repo không phát gì nên `uiState` giữ
     * nguyên trạng thái loading thay vì nháy "không tìm thấy" một frame.
     */
    fun select(debtId: Long?) {
        _isDeleted.value = false
        if (debtId != selectedDebtId.value) _isSwitching.value = true
        selectedDebtId.value = debtId
    }

    init {
        viewModelScope.launch {
            try {
                val banks = vietQrBankRepository.getBanks()
                _banks.value = banks
            } catch (_: Exception) {
                // Ignore network error; fallback to cached/fallback banks
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DebtDetailUiState> = combine(
        selectedDebtId.flatMapLatest { id ->
            if (id == null) {
                emptyFlow()
            } else {
                debtRepository.observeById(id).onEach { _isSwitching.value = false }
            }
        },
        walletRepository.observeAll(),
        _banks,
        _isDeleted,
        _isSwitching,
    ) { debtWithHistory, wallets, banks, isDeleted, isSwitching ->
        DebtDetailUiState(
            debtWithHistory = if (isSwitching) null else debtWithHistory,
            wallets = wallets,
            banks = banks,
            isLoading = isSwitching,
            isDeleted = isDeleted,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtDetailUiState(isLoading = true)
    )

    fun recordPayment(
        debt: Debt,
        amount: Money,
        walletId: Long?,
        note: String,
        syncWithWallet: Boolean,
    ) {
        viewModelScope.launch {
            val trimmedNote = note.trim()
            val payment = DebtPayment(
                id = 0L,
                debtId = debt.id,
                amount = amount,
                walletId = walletId,
                paidAt = Clock.System.now(),
                note = trimmedNote,
            )
            val actionNote = if (syncWithWallet) {
                val isLend = debt.type == DebtType.LEND
                if (isLend) {
                    if (trimmedNote.isNotBlank()) {
                        context.getString(R.string.debt_tx_note_repay_lend_with_note, debt.personName, trimmedNote)
                    } else {
                        context.getString(R.string.debt_tx_note_repay_lend, debt.personName)
                    }
                } else {
                    if (trimmedNote.isNotBlank()) {
                        context.getString(R.string.debt_tx_note_repay_borrow_with_note, debt.personName, trimmedNote)
                    } else {
                        context.getString(R.string.debt_tx_note_repay_borrow, debt.personName)
                    }
                }
            } else null

            val result = recordDebtPaymentUseCase(debt, payment, syncWithWallet, actionNote)
            if (result.isSuccess) {
                _events.emit(DebtDetailUiEvent.PaymentRecorded)
            }
        }
    }

    fun toggleSettled() {
        val current = uiState.value.debtWithHistory ?: return
        val id = selectedDebtId.value ?: return
        viewModelScope.launch {
            debtRepository.markSettled(id, !current.debt.isSettled)
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            debtRepository.deletePayment(paymentId)
            _events.emit(DebtDetailUiEvent.PaymentDeleted)
        }
    }

    fun deleteDebt() {
        val id = selectedDebtId.value ?: return
        viewModelScope.launch {
            deleteDebtUseCase(id)
            _isDeleted.update { true }
            _events.emit(DebtDetailUiEvent.DebtDeleted)
        }
    }
}
