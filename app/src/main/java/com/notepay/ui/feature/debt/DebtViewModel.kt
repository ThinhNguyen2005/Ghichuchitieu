package com.notepay.ui.feature.debt

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.debt.CreateDebtUseCase
import com.notepay.domain.usecase.debt.DeleteDebtUseCase
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import com.notepay.domain.usecase.debt.GetDebtsUseCase
import com.notepay.domain.usecase.debt.RecordDebtPaymentUseCase
import com.notepay.domain.repository.DebtRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    data class ShowToast(val messageResId: Int) : DebtUiEvent
    data object DebtCreated : DebtUiEvent
    data object PaymentRecorded : DebtUiEvent
    data object DebtDeleted : DebtUiEvent
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

    private val _events = MutableSharedFlow<DebtUiEvent>()
    val events = _events.asSharedFlow()

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
        walletRepository.observeAll()
    ) { (tab, query, onlyUnsettled), allDebts, summary, wallets ->
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
        viewModelScope.launch {
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
            val actionNote = if (syncWithWallet) {
                if (type == DebtType.LEND) {
                    if (trimmedNote.isNotBlank()) {
                        context.getString(R.string.debt_tx_note_lend_with_note, trimmedName, trimmedNote)
                    } else {
                        context.getString(R.string.debt_tx_note_lend, trimmedName)
                    }
                } else {
                    if (trimmedNote.isNotBlank()) {
                        context.getString(R.string.debt_tx_note_borrow_with_note, trimmedName, trimmedNote)
                    } else {
                        context.getString(R.string.debt_tx_note_borrow, trimmedName)
                    }
                }
            } else null

            val result = createDebtUseCase(debt, syncWithWallet, actionNote)
            if (result.isSuccess) {
                _events.emit(DebtUiEvent.DebtCreated)
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
                _events.emit(DebtUiEvent.PaymentRecorded)
            }
        }
    }

    fun toggleSettled(debtId: Long, currentSettled: Boolean) {
        viewModelScope.launch {
            debtRepository.markSettled(debtId, !currentSettled)
        }
    }

    fun deleteDebt(debtId: Long) {
        viewModelScope.launch {
            deleteDebtUseCase(debtId)
            _events.emit(DebtUiEvent.DebtDeleted)
        }
    }
}
