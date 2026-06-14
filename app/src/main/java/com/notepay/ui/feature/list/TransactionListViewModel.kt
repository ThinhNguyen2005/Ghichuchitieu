package com.notepay.ui.feature.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    getTransactions: GetTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val filters = MutableStateFlow(TransactionListFilters())
    private val actionState = MutableStateFlow(TransactionListActionState())

    val state = combine(getTransactions(), filters, actionState) { transactions, filters, action ->
        TransactionListUiState(
            transactions = filterTransactions(transactions, filters),
            query = filters.query,
            selectedCategory = filters.category,
            isLoading = false,
            errorMessage = action.errorMessage,
            pendingUndoTransaction = action.pendingUndoTransaction,
            isCalendarView = filters.isCalendarView,
            calendarYear = filters.calendarYear,
            calendarMonth = filters.calendarMonth,
            transactionsByDate = transactions.groupBy { tx ->
                tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListUiState(),
    )

    fun onQueryChanged(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun onCategorySelected(category: Category?) {
        filters.update { it.copy(category = category) }
    }

    fun toggleViewMode() {
        filters.update { it.copy(isCalendarView = !it.isCalendarView) }
    }

    fun onPreviousMonth() {
        filters.update {
            val newMonth = if (it.calendarMonth == 1) 12 else it.calendarMonth - 1
            val newYear = if (it.calendarMonth == 1) it.calendarYear - 1 else it.calendarYear
            it.copy(calendarMonth = newMonth, calendarYear = newYear)
        }
    }

    fun onNextMonth() {
        filters.update {
            val newMonth = if (it.calendarMonth == 12) 1 else it.calendarMonth + 1
            val newYear = if (it.calendarMonth == 12) it.calendarYear + 1 else it.calendarYear
            it.copy(calendarMonth = newMonth, calendarYear = newYear)
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch(ioDispatcher) {
            val result = deleteTransaction(transaction.id)
            actionState.update {
                if (result.isSuccess) {
                    it.copy(pendingUndoTransaction = transaction, errorMessage = null)
                } else {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Không thể xóa giao dịch")
                }
            }
        }
    }

    fun undoDelete() {
        val transaction = actionState.value.pendingUndoTransaction ?: return
        viewModelScope.launch(ioDispatcher) {
            val result = addTransaction(transaction.copy(id = 0L))
            actionState.update {
                if (result.isSuccess) {
                    it.copy(pendingUndoTransaction = null, errorMessage = null)
                } else {
                    it.copy(errorMessage = result.exceptionOrNull()?.message ?: "Không thể khôi phục giao dịch")
                }
            }
        }
    }

    fun clearUndo() {
        actionState.update { it.copy(pendingUndoTransaction = null) }
    }

    fun clearError() {
        actionState.update { it.copy(errorMessage = null) }
    }
}

private data class TransactionListFilters(
    val query: String = "",
    val category: Category? = null,
    val isCalendarView: Boolean = false,
    val calendarYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val calendarMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).monthNumber,
)

private data class TransactionListActionState(
    val errorMessage: String? = null,
    val pendingUndoTransaction: Transaction? = null,
)

private fun filterTransactions(
    transactions: List<Transaction>,
    filters: TransactionListFilters,
): List<Transaction> {
    val normalizedQuery = filters.query.trim().lowercase()
    return transactions.filter { transaction ->
        val matchesQuery = normalizedQuery.isBlank() ||
            transaction.note.lowercase().contains(normalizedQuery) ||
            transaction.category.displayName.lowercase().contains(normalizedQuery)
        val matchesCategory = filters.category == null || transaction.category == filters.category
        matchesQuery && matchesCategory
    }
}
