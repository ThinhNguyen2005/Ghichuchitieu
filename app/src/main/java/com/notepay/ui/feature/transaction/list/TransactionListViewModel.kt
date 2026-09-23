package com.notepay.ui.feature.transaction.list

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.DeleteTransactionUseCase
import com.notepay.domain.usecase.GetTransactionsUseCase
import com.notepay.ui.feedback.FeedbackDuration
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.formatter.TransactionDateHeaderFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

@HiltViewModel
class TransactionListViewModel internal constructor(
    getTransactions: GetTransactionsUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val walletRepository: WalletRepository,
    private val ioDispatcher: CoroutineDispatcher,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val getString: (Int) -> String,
) : ViewModel() {

    @Inject
    constructor(
        getTransactions: GetTransactionsUseCase,
        deleteTransaction: DeleteTransactionUseCase,
        addTransaction: AddTransactionUseCase,
        walletRepository: WalletRepository,
        savedStateHandle: SavedStateHandle,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        getTransactions,
        deleteTransaction,
        addTransaction,
        walletRepository,
        ioDispatcher,
        savedStateHandle,
        context::getString,
    )

    private val initialWalletId: Long? =
        savedStateHandle.get<String>("walletId")?.toLongOrNull()
            ?: savedStateHandle.get<Long>("walletId")

    private val filters = MutableStateFlow(
        TransactionListFilters(selectedWalletId = initialWalletId)
    )
    private val actionState = MutableStateFlow(TransactionListActionState())
    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    val state = combine(
        getTransactions(),
        filters,
        actionState,
        walletRepository.observeAll()
    ) { transactions, currentFilters, action, wallets ->
        val walletsMap = wallets.associate { it.id to it.name }
        val filtered = filterTransactions(transactions, currentFilters)

        // Calculate summary
        var incomeCents = 0L
        var expenseCents = 0L
        for (tx in filtered) {
            when (tx.type) {
                TransactionType.INCOME -> incomeCents += tx.amount.amountInCents
                TransactionType.EXPENSE -> expenseCents += tx.amount.amountInCents
            }
        }

        // Group by date
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val dayGroups = groupTransactionsByDay(filtered, tz, today, getString)
        val transactionsByDate = filtered.groupBy { tx ->
            tx.occurredAt.toLocalDateTime(tz).date
        }

        TransactionListUiState(
            transactions = filtered,
            dayGroups = dayGroups,
            query = currentFilters.query,
            isLoading = false,
            pendingUndoTransaction = action.pendingUndoTransaction,
            isCalendarView = currentFilters.isCalendarView,
            calendarYear = currentFilters.calendarYear,
            calendarMonth = currentFilters.calendarMonth,
            transactionsByDate = transactionsByDate,
            selectedWalletId = currentFilters.selectedWalletId,
            wallets = wallets,
            walletsMap = walletsMap,
            selectedDateRangePreset = currentFilters.dateRangePreset,
            customStartDate = currentFilters.customStartDate,
            customEndDate = currentFilters.customEndDate,
            selectedTransactionType = currentFilters.selectedType,
            isTransferOnly = currentFilters.isTransferOnly,
            selectedCategoryIds = currentFilters.selectedCategoryIds,
            totalIncome = Money(incomeCents),
            totalExpense = Money(expenseCents),
            netBalance = Money(incomeCents - expenseCents),
            transactionCount = filtered.size,
            isDateRangeSheetOpen = currentFilters.isDateRangeSheetOpen,
            isFilterSheetOpen = currentFilters.isFilterSheetOpen,
            isWalletPickerOpen = currentFilters.isWalletPickerOpen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionListUiState(),
    )

    fun toggleViewMode() {
        filters.update { it.copy(isCalendarView = !it.isCalendarView) }
    }

    fun setCalendarView(isCalendar: Boolean) {
        filters.update { it.copy(isCalendarView = isCalendar) }
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

    fun onQueryChanged(query: String) {
        filters.update { it.copy(query = query) }
    }

    fun onCategorySelected(category: Category?) {
        filters.update {
            it.copy(
                selectedCategoryIds = if (category != null) setOf(category.id) else emptySet()
            )
        }
    }

    fun selectWallet(walletId: Long?) {
        filters.update { it.copy(selectedWalletId = walletId, isWalletPickerOpen = false) }
    }

    fun setDateRangePreset(preset: DateRangePreset, customStart: LocalDate? = null, customEnd: LocalDate? = null) {
        filters.update {
            it.copy(
                dateRangePreset = preset,
                customStartDate = customStart,
                customEndDate = customEnd,
                isDateRangeSheetOpen = false
            )
        }
    }

    fun applyFilters(
        type: TransactionType?,
        isTransferOnly: Boolean,
        categoryIds: Set<String>
    ) {
        filters.update {
            it.copy(
                selectedType = type,
                isTransferOnly = isTransferOnly,
                selectedCategoryIds = categoryIds,
                isFilterSheetOpen = false
            )
        }
    }

    fun resetFilters() {
        filters.update {
            it.copy(
                selectedType = null,
                isTransferOnly = false,
                selectedCategoryIds = emptySet()
            )
        }
    }

    fun openDateRangeSheet(isOpen: Boolean) {
        filters.update { it.copy(isDateRangeSheetOpen = isOpen) }
    }

    fun openFilterSheet(isOpen: Boolean) {
        filters.update { it.copy(isFilterSheetOpen = isOpen) }
    }

    fun openWalletPicker(isOpen: Boolean) {
        filters.update { it.copy(isWalletPickerOpen = isOpen) }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch(ioDispatcher) {
            val result = deleteTransaction(transaction.id)
            actionState.update {
                if (result.isSuccess) {
                    it.copy(pendingUndoTransaction = transaction)
                } else {
                    val message = getString(R.string.feedback_transaction_delete_failed)
                    _feedback.tryEmit(UiFeedback(message, type = FeedbackType.Error))
                    it
                }
            }
            if (result.isSuccess) {
                _feedback.emit(
                    UiFeedback(
                        message = getString(R.string.transaction_deleted),
                        actionLabel = getString(R.string.feedback_undo),
                        type = FeedbackType.Success,
                        duration = FeedbackDuration.Long,
                        onAction = { undoDelete(transaction) }
                    )
                )
            }
        }
    }

    fun undoDelete() {
        val transaction = actionState.value.pendingUndoTransaction ?: return
        undoDelete(transaction)
    }

    private fun undoDelete(transaction: Transaction) {
        viewModelScope.launch(ioDispatcher) {
            val result = addTransaction(transaction.copy(id = 0L))
            actionState.update {
                if (result.isSuccess) {
                    _feedback.tryEmit(UiFeedback(getString(R.string.feedback_transaction_restored), type = FeedbackType.Success))
                    it.copy(pendingUndoTransaction = null)
                } else {
                    val message = getString(R.string.feedback_transaction_restore_failed)
                    _feedback.tryEmit(UiFeedback(message, type = FeedbackType.Error))
                    it
                }
            }
        }
    }
}

private data class TransactionListFilters(
    val query: String = "",
    val isCalendarView: Boolean = false,
    val calendarYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val calendarMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month.ordinal + 1,
    val selectedWalletId: Long? = null,
    val dateRangePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,
    val selectedType: TransactionType? = null,
    val isTransferOnly: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),
    val isDateRangeSheetOpen: Boolean = false,
    val isFilterSheetOpen: Boolean = false,
    val isWalletPickerOpen: Boolean = false,
)

private data class TransactionListActionState(
    val pendingUndoTransaction: Transaction? = null,
)

private fun filterTransactions(
    transactions: List<Transaction>,
    filters: TransactionListFilters,
): List<Transaction> {
    val tz = TimeZone.currentSystemDefault()
    val now = Clock.System.now().toLocalDateTime(tz)
    val today = now.date
    val normalizedQuery = filters.query.trim().lowercase()

    return transactions.filter { tx ->
        // 1. Wallet isolation filter
        if (filters.selectedWalletId != null && tx.walletId != filters.selectedWalletId) {
            return@filter false
        }

        // 2. Query filter
        if (normalizedQuery.isNotBlank()) {
            val matches = tx.note.lowercase().contains(normalizedQuery) ||
                tx.category.displayName.lowercase().contains(normalizedQuery) ||
                (tx.amount.amountInCents / 100).toString().contains(normalizedQuery)
            if (!matches) return@filter false
        }

        // 3. Category filter
        if (filters.selectedCategoryIds.isNotEmpty()) {
            if (tx.category.id !in filters.selectedCategoryIds) return@filter false
        }

        // 4. Transaction type & transfer filter
        if (filters.isTransferOnly) {
            if (!tx.isInternalTransfer) return@filter false
        } else if (filters.selectedType != null) {
            if (tx.type != filters.selectedType || tx.isInternalTransfer) return@filter false
        }

        // 5. Date range filter
        val txDate = tx.occurredAt.toLocalDateTime(tz).date
        when (filters.dateRangePreset) {
            DateRangePreset.ALL_TIME -> true
            DateRangePreset.THIS_MONTH -> {
                txDate.year == today.year && txDate.monthNumber == today.monthNumber
            }
            DateRangePreset.LAST_MONTH -> {
                val lastMonth = if (today.monthNumber == 1) 12 else today.monthNumber - 1
                val lastMonthYear = if (today.monthNumber == 1) today.year - 1 else today.year
                txDate.year == lastMonthYear && txDate.monthNumber == lastMonth
            }
            DateRangePreset.LAST_30_DAYS -> {
                (today.toEpochDays() - txDate.toEpochDays()) in 0..30
            }
            DateRangePreset.LAST_3_MONTHS -> {
                (today.toEpochDays() - txDate.toEpochDays()) in 0..90
            }
            DateRangePreset.LAST_6_MONTHS -> {
                (today.toEpochDays() - txDate.toEpochDays()) in 0..180
            }
            DateRangePreset.THIS_YEAR -> {
                txDate.year == today.year
            }
            DateRangePreset.LAST_YEAR -> {
                txDate.year == today.year - 1
            }
            DateRangePreset.CUSTOM -> {
                val start = filters.customStartDate
                val end = filters.customEndDate
                if (start != null && end != null) {
                    txDate in start..end
                } else {
                    true
                }
            }
        }
    }
}

private fun groupTransactionsByDay(
    transactions: List<Transaction>,
    tz: TimeZone,
    today: LocalDate,
    getString: (Int) -> String,
): List<TransactionDayGroup> {
    val grouped = transactions
        .sortedByDescending { it.occurredAt }
        .groupBy { it.occurredAt.toLocalDateTime(tz).date }

    return grouped.map { (date, txs) ->
        var incomeCents = 0L
        var expenseCents = 0L
        for (tx in txs) {
            when (tx.type) {
                TransactionType.INCOME -> incomeCents += tx.amount.amountInCents
                TransactionType.EXPENSE -> expenseCents += tx.amount.amountInCents
            }
        }

        val dayOfWeekStr = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "THỨ HAI"
            DayOfWeek.TUESDAY -> "THỨ BA"
            DayOfWeek.WEDNESDAY -> "THỨ TƯ"
            DayOfWeek.THURSDAY -> "THỨ NĂM"
            DayOfWeek.FRIDAY -> "THỨ SÁU"
            DayOfWeek.SATURDAY -> "THỨ BẢY"
            DayOfWeek.SUNDAY -> "CHỦ NHẬT"
            else -> ""
        }

        val headerText = when (today.toEpochDays() - date.toEpochDays()) {
            0L -> "HÔM NAY · thg ${date.monthNumber} ${date.day}"
            1L -> "HÔM QUA · thg ${date.monthNumber} ${date.day}"
            else -> "$dayOfWeekStr · thg ${date.monthNumber} ${date.day}"
        }

        TransactionDayGroup(
            date = date,
            headerText = headerText,
            totalIncome = Money(incomeCents),
            totalExpense = Money(expenseCents),
            transactions = txs
        )
    }
}
