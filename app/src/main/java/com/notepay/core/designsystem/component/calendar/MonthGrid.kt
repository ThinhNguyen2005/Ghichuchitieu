package com.notepay.core.designsystem.component.calendar

import androidx.compose.runtime.Immutable
import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import kotlinx.datetime.LocalDate
import kotlin.math.ceil

/**
 * Một ô của lưới lịch tháng gọn.
 *
 * [dotLevel] nằm trong khoảng 0..3 và mã hoá cường độ chi tiêu của ngày so với ngày đắt nhất
 * trong tháng — UI vẽ nó thành một thanh mảnh thay cho số tiền, vốn không đọc được ở kích
 * thước ô 44dp.
 */
@Immutable
data class MonthDayCell(
    val date: LocalDate,
    val inCurrentMonth: Boolean,
    val expense: Money,
    val income: Money,
    val dotLevel: Int,
)

/**
 * Lưới lịch một tháng đã gộp sẵn dữ liệu, để Composable chỉ còn việc vẽ.
 *
 * @param weeks luôn chia hết cho 7; tuần đầu có thể chứa ngày của tháng trước.
 */
@Immutable
data class MonthGrid(
    val year: Int,
    val month: Int,
    val weeks: List<List<MonthDayCell>>,
    val totalExpense: Money,
    val totalIncome: Money,
    val activeDayCount: Int,
)

/** Số mức cường độ tối đa cho một ngày. */
private const val MAX_DOT_LEVEL = 3

/**
 * Dựng lưới lịch tháng từ dữ liệu đã lọc của màn Giao dịch.
 *
 * Khác với [MonthlyCalendarView] (lọc lại toàn bộ danh sách cho từng ô), hàm này đi một lượt
 * qua [transactionsByDate] nên chi phí tỉ lệ với số ngày có giao dịch, không phải số ô × số
 * giao dịch. Kết quả là hàm thuần: không Compose, không đọc giờ hệ thống, nên test được bằng
 * JUnit thuần.
 */
fun buildMonthGrid(
    year: Int,
    month: Int,
    transactionsByDate: Map<LocalDate, List<Transaction>>,
): MonthGrid {
    val monthStart = LocalDate(year, month, 1)
    val nextMonthYear = if (month == 12) year + 1 else year
    val nextMonthNumber = month % 12 + 1
    val nextMonthStart = LocalDate(nextMonthYear, nextMonthNumber, 1)
    val daysInMonth = (nextMonthStart.toEpochDays() - monthStart.toEpochDays()).toInt()

    val prevMonthYear = if (month == 1) year - 1 else year
    val prevMonthNumber = if (month == 1) 12 else month - 1
    val prevMonthStart = LocalDate(prevMonthYear, prevMonthNumber, 1)
    val daysInPrevMonth = (monthStart.toEpochDays() - prevMonthStart.toEpochDays()).toInt()

    // Tuần bắt đầu Thứ 2, khớp nhãn "Th 2 ... CN" của lịch.
    val leadingCount = monthStart.dayOfWeek.ordinal
    val totalCells = if (leadingCount + daysInMonth <= 35) 35 else 42

    var totalExpense = 0L
    var totalIncome = 0L
    var activeDayCount = 0
    var maxDailyMagnitude = 0L
    val cells = ArrayList<MonthDayCell>(totalCells)

    for (index in 0 until totalCells) {
        val inCurrentMonth = index >= leadingCount && index < leadingCount + daysInMonth
        val date = when {
            index < leadingCount -> LocalDate(
                prevMonthYear,
                prevMonthNumber,
                daysInPrevMonth - leadingCount + 1 + index,
            )
            inCurrentMonth -> LocalDate(year, month, index - leadingCount + 1)
            else -> LocalDate(nextMonthStart.year, nextMonthNumber, index - leadingCount - daysInMonth + 1)
        }

        var expense = 0L
        var income = 0L
        if (inCurrentMonth) {
            transactionsByDate[date]?.forEach { transaction ->
                if (transaction.type == TransactionType.EXPENSE) {
                    expense += transaction.amount.amountInCents
                } else {
                    income += transaction.amount.amountInCents
                }
            }
            totalExpense += expense
            totalIncome += income
            val magnitude = maxOf(expense, income)
            if (magnitude > 0L) activeDayCount++
            if (magnitude > maxDailyMagnitude) maxDailyMagnitude = magnitude
        }

        cells += MonthDayCell(
            date = date,
            inCurrentMonth = inCurrentMonth,
            expense = Money(expense),
            income = Money(income),
            dotLevel = 0,
        )
    }

    val scored = cells.map { cell ->
        val magnitude = maxOf(cell.expense.amountInCents, cell.income.amountInCents)
        val level = if (magnitude <= 0L || maxDailyMagnitude <= 0L) {
            0
        } else {
            ceil(MAX_DOT_LEVEL.toDouble() * magnitude / maxDailyMagnitude)
                .toInt()
                .coerceIn(1, MAX_DOT_LEVEL)
        }
        if (level == cell.dotLevel) cell else cell.copy(dotLevel = level)
    }

    return MonthGrid(
        year = year,
        month = month,
        weeks = scored.chunked(7),
        totalExpense = Money(totalExpense),
        totalIncome = Money(totalIncome),
        activeDayCount = activeDayCount,
    )
}
