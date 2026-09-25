package com.notepay.ui.feature.debt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.model.Money
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.ui.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
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
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val debtId: Long = savedStateHandle.get<Long>(Route.DebtDetail.ARG_ID)
        ?: savedStateHandle.get<String>(Route.DebtDetail.ARG_ID)?.toLongOrNull()
        ?: -1L

    private val _events = MutableSharedFlow<DebtDetailUiEvent>()
    val events = _events.asSharedFlow()

    private val _isDeleted = MutableStateFlow(false)
    private val _banks = MutableStateFlow(VietQrBankRepository.FALLBACK)

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

    val uiState: StateFlow<DebtDetailUiState> = combine(
        debtRepository.observeById(debtId),
        walletRepository.observeAll(),
        _banks,
        _isDeleted
    ) { debtWithHistory, wallets, banks, isDeleted ->
        DebtDetailUiState(
            debtWithHistory = debtWithHistory,
            wallets = wallets,
            banks = banks,
            isLoading = false,
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
            val payment = DebtPayment(
                id = 0L,
                debtId = debt.id,
                amount = amount,
                walletId = walletId,
                paidAt = Clock.System.now(),
                note = note.trim(),
            )
            val result = recordDebtPaymentUseCase(debt, payment, syncWithWallet)
            if (result.isSuccess) {
                _events.emit(DebtDetailUiEvent.PaymentRecorded)
            }
        }
    }

    fun toggleSettled() {
        val current = uiState.value.debtWithHistory ?: return
        viewModelScope.launch {
            debtRepository.markSettled(debtId, !current.debt.isSettled)
        }
    }

    fun deletePayment(paymentId: Long) {
        viewModelScope.launch {
            debtRepository.deletePayment(paymentId)
            _events.emit(DebtDetailUiEvent.PaymentDeleted)
        }
    }

    fun deleteDebt() {
        viewModelScope.launch {
            deleteDebtUseCase(debtId)
            _isDeleted.update { true }
            _events.emit(DebtDetailUiEvent.DebtDeleted)
        }
    }
}
