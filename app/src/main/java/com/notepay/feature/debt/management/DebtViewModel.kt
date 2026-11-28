package com.notepay.feature.debt.management

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackDuration
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.domain.money.Money
import com.notepay.domain.model.Debt
import com.notepay.domain.model.DebtPayment
import com.notepay.domain.model.DebtType
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.CreateDebtUseCase
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import com.notepay.domain.usecase.debt.GetDebtsUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.domain.repository.DebtRepository
import com.notepay.feature.debt.debtCreationTransactionNote
import com.notepay.feature.debt.debtPaymentTransactionNote
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import javax.inject.Inject

sealed interface DebtUiEvent {
    data class DebtCreated(val debtId: Long) : DebtUiEvent
}

@HiltViewModel
class DebtViewModel @Inject constructor(
    private val getDebtsUseCase: GetDebtsUseCase,
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase,
    private val createDebtUseCase: CreateDebtUseCase,
    private val recordDebtPaymentUseCase: RecordDebtPaymentUseCase,
    private val deleteDebtUseCase: DeleteDebtUseCase,
    private val debtRepository: DebtRepository,
    private val walletRepository: WalletRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val _selectedTab = MutableStateFlow<DebtType?>(null)
    private val _onlyUnsettled = MutableStateFlow(true)
    private val _searchQuery = MutableStateFlow("")
    private val _isSubmitting = MutableStateFlow(false)

    private val _events = MutableSharedFlow<DebtUiEvent>()
    val events = _events.asSharedFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    private val _createSheetFeedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val createSheetFeedback = _createSheetFeedback.asSharedFlow()

    private val debtsFlow = combine(
        _selectedTab,
        _searchQuery,
        _onlyUnsettled
    ) { tab, query, onlyUnsettled ->
        Triple(tab, query, onlyUnsettled)
    }

    val uiState: StateFlow<DebtUiState> = combine(
        debtsFlow,
        debtRepository.observeAll(),
        getDebtSummaryUseCase(),
        walletRepository.observeAll(),
        _isSubmitting,
    ) { (tab, query, onlyUnsettled), allDebts, summary, wallets, isSubmitting ->
        val filtered = allDebts.filter { item ->
            val matchesTab = tab == null || item.debt.type == tab
            val matchesSettled = !onlyUnsettled || !item.isFullyPaid
            val matchesQuery = query.isBlank() ||
                item.debt.personName.contains(query, ignoreCase = true) ||
                item.debt.note.contains(query, ignoreCase = true) ||
                (item.debt.phoneNumber?.contains(query) == true)

            matchesTab && matchesSettled && matchesQuery
        }

        DebtUiState(
            debts = filtered,
            selectedTab = tab,
            onlyUnsettled = onlyUnsettled,
            searchQuery = query,
            summary = summary,
            wallets = wallets,
            isLoading = false,
            isSubmitting = isSubmitting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DebtUiState(isLoading = true)
    )

    fun setTab(type: DebtType?) {
        _selectedTab.value = type
    }

    fun setOnlyUnsettled(enabled: Boolean) {
        _onlyUnsettled.value = enabled
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun createDebt(
        personName: String,
        phoneNumber: String?,
        type: DebtType,
        amount: Money,
        walletId: Long?,
        dueDate: Instant?,
        note: String,
        syncWithWallet: Boolean,
    ) {
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val trimmedName = personName.trim()
                val trimmedNote = note.trim()
                val debt = Debt(
                    id = 0L,
                    personName = trimmedName,
                    phoneNumber = phoneNumber?.takeIf { it.isNotBlank() }?.trim(),
                    type = type,
                    originalAmount = amount,
                    walletId = walletId,
                    createdAt = Clock.System.now(),
                    dueDate = dueDate,
                    note = trimmedNote,
                    isSettled = false,
                )
                val transactionNote = if (syncWithWallet) {
                    context.debtCreationTransactionNote(type, trimmedName, trimmedNote)
                } else ""

                val result = createDebtUseCase(debt, syncWithWallet, transactionNote)
                _isSubmitting.value = false
                result.onSuccess { debtId ->
                    _events.emit(DebtUiEvent.DebtCreated(debtId))
                }.onFailure {
                    _createSheetFeedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_create_failed),
                            actionLabel = context.getString(R.string.action_retry),
                            type = FeedbackType.Error,
                            duration = FeedbackDuration.Long,
                            onAction = {
                                createDebt(
                                    personName = personName,
                                    phoneNumber = phoneNumber,
                                    type = type,
                                    amount = amount,
                                    walletId = walletId,
                                    dueDate = dueDate,
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

    fun toggleSettled(debtId: Long, currentSettled: Boolean) {
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            val newSettled = !currentSettled
            try {
                debtRepository.markSettled(debtId, newSettled)
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
                        onAction = { restoreSettled(debtId, currentSettled) },
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
                        onAction = { toggleSettled(debtId, currentSettled) },
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

    fun deleteDebt(debtId: Long) {
        if (!_isSubmitting.compareAndSet(false, true)) return
        viewModelScope.launch {
            try {
                val result = deleteDebtUseCase(debtId)
                _isSubmitting.value = false
                result.onSuccess {
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_delete_success),
                            type = FeedbackType.Success,
                        ),
                    )
                }.onFailure {
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(R.string.debt_delete_failed),
                            actionLabel = context.getString(R.string.action_retry),
                            type = FeedbackType.Error,
                            duration = FeedbackDuration.Long,
                            onAction = { deleteDebt(debtId) },
                        ),
                    )
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
