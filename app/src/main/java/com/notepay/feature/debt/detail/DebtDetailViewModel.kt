package com.notepay.feature.debt.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.money.Money
import com.notepay.domain.model.Debt
import com.notepay.domain.model.DebtPayment
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.feature.debt.debtPaymentTransactionNote
import com.notepay.core.navigation.Route
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
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
    data object DebtDeleted : DebtDetailUiEvent
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

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    private val _isDeleted = MutableStateFlow(false)
    private val _banks = MutableStateFlow(VietQrBankRepository.FALLBACK)
    private val _isSubmitting = MutableStateFlow(false)

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
            } catch (e: CancellationException) {
                throw e
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
        combine(_isDeleted, _isSwitching, _isSubmitting, ::Triple),
    ) { debtWithHistory, wallets, banks, (isDeleted, isSwitching, isSubmitting) ->
        DebtDetailUiState(
            debtWithHistory = if (isSwitching) null else debtWithHistory,
            wallets = wallets,
            banks = banks,
            isLoading = isSwitching,
            isDeleted = isDeleted,
            isSubmitting = isSubmitting,
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
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val trimmedNote = note.trim()
                val payment = DebtPayment(
                    id = 0L,
                    debtId = debt.id,
                    amount = amount,
                    walletId = walletId,
                    paidAt = Clock.System.now(),
                    note = trimmedNote,
                )
                val transactionNote = if (syncWithWallet) {
                    context.debtPaymentTransactionNote(debt.type, debt.personName, trimmedNote)
                } else ""

                val result = recordDebtPaymentUseCase(debt, payment, syncWithWallet, transactionNote)
                _isSubmitting.value = false
                result.onSuccess {
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_payment_success),
                            type = FeedbackType.Success,
                        ),
                    )
                }.onFailure {
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_payment_failed),
                            actionLabel = context.getString(R.string.action_retry),
                            type = FeedbackType.Error,
                            duration = FeedbackDuration.Long,
                            onAction = {
                                recordPayment(
                                    debt = debt,
                                    amount = amount,
                                    walletId = walletId,
                                    note = note,
                                    syncWithWallet = syncWithWallet,
                                )
                            },
                        ),
                    )
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun toggleSettled() {
        val current = uiState.value.debtWithHistory ?: return
        val id = selectedDebtId.value ?: return
        if (!_isSubmitting.compareAndSet(false, true)) return
        val previousSettled = current.debt.isSettled
        val newSettled = !previousSettled
        viewModelScope.launch {
            try {
                debtRepository.markSettled(id, newSettled)
                _isSubmitting.value = false
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(
                            if (newSettled) {
                                R.string.debt_marked_settled_success
                            } else {
                                R.string.debt_marked_unsettled_success
                            },
                        ),
                        actionLabel = context.getString(R.string.feedback_undo),
                        type = FeedbackType.Success,
                        duration = FeedbackDuration.Long,
                        onAction = { restoreSettled(id, previousSettled) },
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _isSubmitting.value = false
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.debt_mark_settled_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { toggleSettled() },
                    ),
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun restoreSettled(debtId: Long, previousSettled: Boolean) {
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                debtRepository.markSettled(debtId, previousSettled)
                _isSubmitting.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _isSubmitting.value = false
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.debt_mark_settled_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { restoreSettled(debtId, previousSettled) },
                    ),
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deletePayment(paymentId: Long) {
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                debtRepository.deletePayment(paymentId)
                _isSubmitting.value = false
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.debt_payment_delete_success),
                        type = FeedbackType.Success,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _isSubmitting.value = false
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(R.string.debt_payment_delete_failed),
                        actionLabel = context.getString(R.string.action_retry),
                        type = FeedbackType.Error,
                        duration = FeedbackDuration.Long,
                        onAction = { deletePayment(paymentId) },
                    ),
                )
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteDebt() {
        val id = selectedDebtId.value ?: return
        if (_isDeleted.value) return
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val result = deleteDebtUseCase(id)
                result.onSuccess {
                    _isDeleted.update { true }
                    _isSubmitting.value = false
                    _events.emit(DebtDetailUiEvent.DebtDeleted)
                }.onFailure {
                    _isSubmitting.value = false
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_delete_failed),
                            actionLabel = context.getString(R.string.action_retry),
                            type = FeedbackType.Error,
                            duration = FeedbackDuration.Long,
                            onAction = { deleteDebt() },
                        ),
                    )
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
