package com.notepay.ui.feature.list

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Money
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val query: String = "",
    val selectedCategory: Category? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val pendingUndoTransaction: Transaction? = null,
    val isCalendarView: Boolean = false,
    val calendarYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val calendarMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month.ordinal + 1,
    /**
     * Giao dịch thô (chưa lọc theo query/category) nhóm theo ngày — dùng để hiển thị
     * dialog chi tiết khi user tap 1 ngày trên bảng lịch.
     */
    val transactionsByDate: Map<LocalDate, List<Transaction>> = emptyMap(),
    val walletsMap: Map<Long, String> = emptyMap(),
    val totalIncomeForSelectedMonth: Money = Money.ZERO,
    val totalExpenseForSelectedMonth: Money = Money.ZERO,
) {
    val isEmpty: Boolean get() = !isLoading && transactions.isEmpty()
}
