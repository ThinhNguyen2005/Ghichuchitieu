package com.notepay.feature.transaction.list

import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.feature.transaction.component.DateRangePreset
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class TransactionDayGroup(
    val date: LocalDate,
    val headerText: String,
    val totalIncome: Money,
    val totalExpense: Money,
    val transactions: List<Transaction>,
)

data class TransactionListUiState(
    val transactions: List<Transaction> = emptyList(),
    val dayGroups: List<TransactionDayGroup> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
    val pendingUndoTransaction: Transaction? = null,

    // Calendar View state
    val isCalendarView: Boolean = false,
    val calendarYear: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year,
    val calendarMonth: Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).month.ordinal + 1,
    val transactionsByDate: Map<LocalDate, List<Transaction>> = emptyMap(),

    // Wallet selection (null = All Accounts "Tất cả tài khoản")
    val selectedWalletId: Long? = null,
    val wallets: List<Wallet> = emptyList(),
    val walletsMap: Map<Long, String> = emptyMap(),

    // Date range filter
    val selectedDateRangePreset: DateRangePreset = DateRangePreset.ALL_TIME,
    val customStartDate: LocalDate? = null,
    val customEndDate: LocalDate? = null,

    // Filter sheet state
    val selectedTransactionType: TransactionType? = null, // null = All
    val isTransferOnly: Boolean = false,
    val selectedCategoryIds: Set<String> = emptySet(),

    // Summary statistics for current filter
    val totalIncome: Money = Money.ZERO,
    val totalExpense: Money = Money.ZERO,
    val netBalance: Money = Money.ZERO,
    val transactionCount: Int = 0,

    // Modal Sheet states
    val isDateRangeSheetOpen: Boolean = false,
    val isFilterSheetOpen: Boolean = false,
    val isWalletPickerOpen: Boolean = false,
) {
    val isEmpty: Boolean get() = !isLoading && transactions.isEmpty()

    /**
     * Số mục lọc phụ đang bật, dùng cho badge trên thanh hành động. Mỗi mục đã chọn tính là
     * một đơn vị để người dùng biết bộ lọc đang thu hẹp danh sách ở bao nhiêu chỗ.
     *
     * Bộ lọc ví cũng được đếm: nếu bỏ sót nó, người dùng lọc còn một ví mà danh sách rỗng sẽ
     * không thấy dấu hiệu nào trên badge để biết vì sao.
     */
    val activeFilterCount: Int
        get() = (if (selectedTransactionType != null) 1 else 0) +
                (if (isTransferOnly) 1 else 0) +
                (if (selectedWalletId != null) 1 else 0) +
                selectedCategoryIds.size
}
