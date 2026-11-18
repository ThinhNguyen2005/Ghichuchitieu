package com.notepay.domain.analytics

import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Đầu vào thuần Kotlin cho [StatsSnapshotCalculator]. Toàn bộ mốc thời gian được truyền vào
 * thay vì đọc `Clock.System.now()` bên trong, giúp hàm tính toán tất định và unit test được.
 */
data class StatsSnapshotInput(
    val transactions: List<Transaction>,
    val wallets: List<Wallet>,
    val period: StatsPeriod,
    val selectedWalletId: Long?,
    val trendWindowMonths: Int,
    val today: LocalDate,
    val zone: TimeZone = TimeZone.currentSystemDefault(),
)

/** Tổng thu/chi của một tháng dương lịch, dùng cho biểu đồ xu hướng. */
data class MonthlyTrendTotals(
    val year: Int,
    val month: Int,
    val expenseInCents: Long,
    val incomeInCents: Long,
)

/**
 * Kết quả cắt dữ liệu Thống kê: chỉ số học thuần túy, không chứa chuỗi hiển thị hay kiểu UI.
 * ViewModel nhận khối này và chỉ làm nhiệm vụ ánh xạ sang trạng thái giao diện.
 */
data class StatsSnapshot(
    val filteredTransactions: List<Transaction>,
    /** null khi kỳ trước không đủ dữ liệu để đối chiếu (app chưa được dùng ở kỳ đó). */
    val previousExpenseInCents: Long?,
    val previousIncomeInCents: Long?,
    val currentMonthExpenseInCents: Long,
    val spentTodayInCents: Long,
    /** null khi ví được chọn không đặt hạn mức, hoặc tổng hạn mức các ví bằng 0. */
    val budgetLimitInCents: Long?,
    val budgetUsedRatio: Float,
    val dailyExpenses: List<DailyExpense>,
    val trendMonths: List<MonthlyTrendTotals>,
    val daysInMonth: Int,
    val currentDay: Int,
    val remainingDays: Int,
    val isViewingCurrentMonth: Boolean,
)

/**
 * tính toán số liệu gốc cho màn hình Thống kê.
 *
 * Trước đây toàn bộ vòng lặp lọc/tính tổng nằm trong lambda `combine()` của StatsViewModel;
 * chúng được chuyển xuống đây để ViewModel chỉ còn biến đổi trạng thái, và để logic tiền tệ
 * unit test được mà không cần Robolectric.
 */
object StatsSnapshotCalculator {

    fun snapshot(input: StatsSnapshotInput): StatsSnapshot {
        val transactions = input.transactions
        val period = input.period
        val today = input.today
        val zone = input.zone

        // 1. Giao dịch thuộc kỳ đang xem và thuộc ví được chọn (null = tất cả ví).
        val filteredTransactions = transactions.filter { tx ->
            period.contains(tx.occurredAt.toLocalDateTime(zone).date) && matchesWallet(tx, input.selectedWalletId)
        }

        // 2. So sánh cùng kỳ trước: chỉ có nghĩa khi người dùng đã dùng app trong kỳ đó.
        val firstTxDate = transactions.minByOrNull { it.occurredAt }
            ?.occurredAt?.toLocalDateTime(zone)?.date
        val prevRange = comparablePreviousRange(period, today)
        val hasComparable = hasComparableData(prevRange, firstTxDate)
        var previousExpenseInCents: Long? = null
        var previousIncomeInCents: Long? = null
        if (hasComparable && prevRange != null) {
            val previous = transactions.filter { tx ->
                tx.occurredAt.toLocalDateTime(zone).date in prevRange && matchesWallet(tx, input.selectedWalletId)
            }
            previousExpenseInCents = totalOf(previous, TransactionType.EXPENSE)
            previousIncomeInCents = totalOf(previous, TransactionType.INCOME)
        }

        // 3. Hạn mức: ví đang chọn dùng hạn mức của chính nó; chế độ "tất cả ví" cộng dồn hạn mức.
        val selectedWallet = input.wallets.find { it.id == input.selectedWalletId }
        val budgetLimitInCents = if (input.selectedWalletId != null) {
            selectedWallet?.budgetLimit?.amountInCents
        } else {
            input.wallets.mapNotNull { it.budgetLimit?.amountInCents }.sum().takeIf { it > 0L }
        }

        // 4. Chi tiêu trong tháng hiện tại (mốc "tháng này" luôn là tháng của hôm nay,
        // không phụ thuộc kỳ đang xem) và chi tiêu riêng trong hôm nay.
        val currentMonthRange = monthRangeMillis(today.year, today.month.number, zone)
        val currentMonthTransactions = transactions.filter { tx ->
            val millis = tx.occurredAt.toEpochMilliseconds()
            millis >= currentMonthRange.first && millis <= currentMonthRange.second
        }
        val currentMonthExpenseInCents = totalOf(
            currentMonthTransactions.filter { matchesWallet(it, input.selectedWalletId) },
            TransactionType.EXPENSE,
        )

        val daysInMonth = monthLength(today.year, today.month.number)
        val currentDay = today.day.coerceIn(1, daysInMonth)
        val spentTodayInCents = totalOf(
            currentMonthTransactions.filter {
                matchesWallet(it, input.selectedWalletId) &&
                    it.occurredAt.toEpochMilliseconds() in dayRangeMillis(today, zone)
            },
            TransactionType.EXPENSE,
        )

        val budgetUsedRatio = if (budgetLimitInCents != null && budgetLimitInCents > 0L) {
            currentMonthExpenseInCents.toFloat() / budgetLimitInCents
        } else {
            0f
        }

        val isViewingCurrentMonth = period.range == StatsRange.MONTH && period.contains(today)

        // 5. Chuỗi chi theo ngày cho SpendingForecastEngine (toàn bộ lịch sử, không cắt theo kỳ).
        val dailyExpenses = if (isViewingCurrentMonth) {
            transactions
                .filter { it.type == TransactionType.EXPENSE && matchesWallet(it, input.selectedWalletId) }
                .groupBy { it.occurredAt.toLocalDateTime(zone).date }
                .map { (date, rows) -> DailyExpense(date, rows.sumOf { it.amount.amountInCents }) }
        } else {
            emptyList()
        }

        // 6. Biểu đồ xu hướng: số tháng lùi từ mốc của kỳ đang xem, vẫn áp dụng bộ lọc ví.
        val anchor = period.start ?: today
        val trendMonths = ((input.trendWindowMonths - 1) downTo 0).map { offset ->
            val absoluteMonth = anchor.year * 12 + (anchor.month.number - 1) - offset
            val trendYear = absoluteMonth / 12
            val trendMonth = absoluteMonth % 12 + 1
            val monthRows = transactions.filter { tx ->
                val local = tx.occurredAt.toLocalDateTime(zone)
                matchesWallet(tx, input.selectedWalletId) &&
                    local.year == trendYear && local.month.number == trendMonth
            }
            MonthlyTrendTotals(
                year = trendYear,
                month = trendMonth,
                expenseInCents = totalOf(monthRows, TransactionType.EXPENSE),
                incomeInCents = totalOf(monthRows, TransactionType.INCOME),
            )
        }

        return StatsSnapshot(
            filteredTransactions = filteredTransactions,
            previousExpenseInCents = previousExpenseInCents,
            previousIncomeInCents = previousIncomeInCents,
            currentMonthExpenseInCents = currentMonthExpenseInCents,
            spentTodayInCents = spentTodayInCents,
            budgetLimitInCents = budgetLimitInCents,
            budgetUsedRatio = budgetUsedRatio,
            dailyExpenses = dailyExpenses,
            trendMonths = trendMonths,
            daysInMonth = daysInMonth,
            currentDay = currentDay,
            remainingDays = daysInMonth - currentDay + 1,
            isViewingCurrentMonth = isViewingCurrentMonth,
        )
    }

    /** Giới hạn ngày của tháng dương lịch [year]/[month] theo múi giờ thiết bị. */
    fun monthRangeMillis(year: Int, month: Int, zone: TimeZone): Pair<Long, Long> {
        val firstDate = LocalDate(year, month, 1)
        val nextMonth = firstDate.plus(1, DateTimeUnit.MONTH)
        val first = LocalDateTime(firstDate.year, firstDate.month.number, 1, 0, 0).toInstant(zone)
        val lastExclusive = LocalDateTime(nextMonth.year, nextMonth.month.number, 1, 0, 0).toInstant(zone)
        return first.toEpochMilliseconds() to (lastExclusive.toEpochMilliseconds() - 1)
    }

    /** Số ngày trong tháng dương lịch [year]/[month]. */
    fun monthLength(year: Int, month: Int): Int {
        val monthStart = LocalDate(year, month, 1)
        return (monthStart.plus(1, DateTimeUnit.MONTH).toEpochDays() - monthStart.toEpochDays()).toInt()
    }

    private fun dayRangeMillis(date: LocalDate, zone: TimeZone): LongRange {
        val dayStart = LocalDateTime(date.year, date.month.number, date.day, 0, 0).toInstant(zone)
        val dayEnd = LocalDateTime(date.year, date.month.number, date.day, 23, 59, 59, 999_000_000)
            .toInstant(zone)
        return dayStart.toEpochMilliseconds()..dayEnd.toEpochMilliseconds()
    }

    private fun matchesWallet(tx: Transaction, selectedWalletId: Long?): Boolean =
        selectedWalletId == null || tx.walletId == selectedWalletId

    private fun totalOf(transactions: List<Transaction>, type: TransactionType): Long =
        transactions.filter { it.type == type }.sumOf { it.amount.amountInCents }
}
