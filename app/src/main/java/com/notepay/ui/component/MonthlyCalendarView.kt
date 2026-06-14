package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val totalExpense: Money,
    val totalIncome: Money,
    val hasUpcomingSubscription: Boolean = false,
)

/**
 * Calendar tháng dùng chung cho cả trang Danh sách (xem giao dịch) và trang Nhắc nhở (xem subscription đến hạn).
 *
 * @param highlightDates tập ngày nên đánh dấu (vd: subscription đến hạn) — hiển thị chấm đỏ nhạt.
 * @param onDayClick callback khi user tap vào 1 ngày trong tháng hiện tại.
 */
@Composable
fun MonthlyCalendarView(
    year: Int,
    month: Int,
    transactions: List<Transaction>,
    modifier: Modifier = Modifier,
    highlightDates: Set<LocalDate> = emptySet(),
    onDayClick: (LocalDate) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val calendarDays = remember(year, month, transactions, highlightDates) {
        getCalendarDays(year, month, transactions, highlightDates)
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Tháng trước")
            }
            Text(
                text = "Tháng $month / $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Tháng sau")
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            val daysOfWeek = listOf("Th 2", "Th 3", "Th 4", "Th 5", "Th 6", "Th 7", "CN")
            daysOfWeek.forEachIndexed { index, day ->
                val textColor = when (index) {
                    5 -> Color(0xFF40C4FF)
                    6 -> Color(0xFFFF5722)
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    )
                }
            }
        }

        val rows = calendarDays.chunked(7)
        Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 16.dp)) {
            rows.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    week.forEach { day ->
                        CalendarCell(
                            day = day,
                            onClick = { if (day.isCurrentMonth) onDayClick(day.date) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    day: CalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val isToday = day.date == today
    val hasData = !day.totalExpense.isZero() || !day.totalIncome.isZero()

    val backgroundColor = when {
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        !day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val contentAlpha = if (day.isCurrentMonth) 1f else 0.38f
    val dayNumberColor = when {
        isToday -> MaterialTheme.colorScheme.primary
        day.date.dayOfWeek.ordinal + 1 == 6 -> Color(0xFF40C4FF).copy(alpha = contentAlpha)
        day.date.dayOfWeek.ordinal + 1 == 7 -> Color(0xFFFF5722).copy(alpha = contentAlpha)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    }

    // P0-2: Box ngoài dùng clickable với ripple mặc định (bỏ indication = null)
    // để user thấy rõ "bấm được". Bỏ Box rỗng chồng lên nhau gây lỗi 4 góc.
    Box(
        modifier = modifier
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            .let { base ->
                if (day.isCurrentMonth) base.clickable(onClick = onClick) else base
            }
            .padding(4.dp),
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = dayNumberColor,
            modifier = Modifier.align(Alignment.TopStart),
        )

        if (day.isCurrentMonth) {
            if (day.hasUpcomingSubscription) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .background(Color(0xFFFFCDD2), CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFB71C1C),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // P0-2: chấm dot màu primary dưới số ngày khi có dữ liệu
            // (giao dịch hoặc subscription highlight) — cho người dùng biết ô có nội dung.
            if (hasData) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 4.dp, bottom = 4.dp)
                        .size(5.dp)
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            shape = CircleShape,
                        ),
                )
            }

            if (!day.totalExpense.isZero() || !day.totalIncome.isZero()) {
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    if (!day.totalExpense.isZero()) {
                        Text(
                            text = "-${MoneyFormatter.formatCompact(day.totalExpense)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE57373),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                    if (!day.totalIncome.isZero()) {
                        Text(
                            text = "+${MoneyFormatter.formatCompact(day.totalIncome)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF81C784),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

private fun getCalendarDays(
    year: Int,
    month: Int,
    transactions: List<Transaction>,
    highlightDates: Set<LocalDate>,
): List<CalendarDay> {
    val firstDayOfMonth = LocalDate(year, month, 1)
    val dayOfWeek = firstDayOfMonth.dayOfWeek.ordinal + 1

    val daysInMonth = getDaysInMonth(year, month)
    val list = ArrayList<CalendarDay>()

    val prevMonthYear = if (month == 1) year - 1 else year
    val prevMonth = if (month == 1) 12 else month - 1
    val daysInPrevMonth = getDaysInMonth(prevMonthYear, prevMonth)
    val prevDaysCount = dayOfWeek - 1

    for (i in prevDaysCount - 1 downTo 0) {
        val d = daysInPrevMonth - i
        val date = LocalDate(prevMonthYear, prevMonth, d)
        list.add(createCalendarDay(date, false, transactions, highlightDates))
    }

    for (d in 1..daysInMonth) {
        val date = LocalDate(year, month, d)
        list.add(createCalendarDay(date, true, transactions, highlightDates))
    }

    val nextMonthYear = if (month == 12) year + 1 else year
    val nextMonth = if (month == 12) 1 else month + 1
    val totalCells = if (list.size <= 35) 35 else 42
    val nextDaysCount = totalCells - list.size

    for (d in 1..nextDaysCount) {
        val date = LocalDate(nextMonthYear, nextMonth, d)
        list.add(createCalendarDay(date, false, transactions, highlightDates))
    }

    return list
}

private fun getDaysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) 29 else 28
        else -> 30
    }
}

private fun createCalendarDay(
    date: LocalDate,
    isCurrentMonth: Boolean,
    transactions: List<Transaction>,
    highlightDates: Set<LocalDate>,
): CalendarDay {
    val systemTz = TimeZone.currentSystemDefault()
    val dayTransactions = transactions.filter {
        val localDate = it.occurredAt.toLocalDateTime(systemTz).date
        localDate == date
    }

    var expense = 0L
    var income = 0L
    for (t in dayTransactions) {
        if (t.type == TransactionType.EXPENSE) {
            expense += t.amount.amountInCents
        } else {
            income += t.amount.amountInCents
        }
    }

    return CalendarDay(
        date = date,
        isCurrentMonth = isCurrentMonth,
        totalExpense = Money(expense),
        totalIncome = Money(income),
        hasUpcomingSubscription = highlightDates.contains(date),
    )
}
