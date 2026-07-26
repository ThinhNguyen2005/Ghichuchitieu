package com.notepay.ui.component

import com.notepay.ui.theme.AppTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
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
    selectedDate: LocalDate? = null,
    bottomContentPadding: Dp = 0.dp,
    onDayClick: (LocalDate) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val targetPage = (year - 2020) * 12 + (month - 1)
    val pagerState = rememberPagerState(initialPage = targetPage) { 2000 }

    // Đồng bộ trang từ ngoài truyền vào (ví dụ khi nhấn nút mũi tên chuyển tháng)
    LaunchedEffect(year, month) {
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Đồng bộ Year/Month ra ngoài khi người dùng vuốt chuyển trang ngang
    LaunchedEffect(pagerState.currentPage) {
        val page = pagerState.currentPage
        if (page != targetPage) {
            if (page > targetPage) {
                onNextMonth()
            } else {
                onPreviousMonth()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.stats_previous_month),
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "Tháng $month / $year",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.stats_next_month),
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            val daysOfWeek = listOf("Th 2", "Th 3", "Th 4", "Th 5", "Th 6", "Th 7", "CN")
            daysOfWeek.forEachIndexed { index, day ->
                val textColor = when (index) {
                    5 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    6 -> MaterialTheme.colorScheme.error
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

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pageYear = 2020 + page / 12
            val pageMonth = page % 12 + 1

            // Tạo danh sách ngày riêng biệt cho từng trang của lịch để hiệu ứng chuyển tiếp không bị giật
            val pageCalendarDays = remember(pageYear, pageMonth, transactions, highlightDates) {
                getCalendarDays(pageYear, pageMonth, transactions, highlightDates)
            }

            val rows = pageCalendarDays.chunked(7)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp + bottomContentPadding)
            ) {
                rows.forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        week.forEach { day ->
                            CalendarCell(
                                day = day,
                                selectedDate = selectedDate,
                                onClick = { if (day.isCurrentMonth) onDayClick(day.date) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarCell(
    day: CalendarDay,
    selectedDate: LocalDate?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val isToday = day.date == today
    val isSelected = selectedDate == day.date
    val hasData = !day.totalExpense.isZero() || !day.totalIncome.isZero()

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
        isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        !day.isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        else -> Color.Transparent
    }

    val dayNumberColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        day.date.dayOfWeek.ordinal + 1 == 6 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        day.date.dayOfWeek.ordinal + 1 == 7 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dayNumberModifier = Modifier
        .size(28.dp)
        .let { base ->
            when {
                isSelected -> base.background(MaterialTheme.colorScheme.primary, AppTheme.shapes.circle)
                isToday -> base.border(1.2.dp, MaterialTheme.colorScheme.primary, AppTheme.shapes.circle)
                else -> base
            }
        }

    Column(
        modifier = modifier
            .background(backgroundColor)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            .let { base ->
                if (day.isCurrentMonth) base.clickable(onClick = onClick) else base
            }
            .padding(4.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = dayNumberModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day.date.day.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = dayNumberColor,
                )
            }

            if (day.isCurrentMonth && day.hasUpcomingSubscription) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp, end = 2.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, AppTheme.shapes.circle)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        if (day.isCurrentMonth && (!day.totalIncome.isZero() || !day.totalExpense.isZero())) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                if (!day.totalIncome.isZero()) {
                    Text(
                        text = "+${formatCompactMoney(day.totalIncome)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                if (!day.totalExpense.isZero()) {
                    Text(
                        text = "-${formatCompactMoney(day.totalExpense)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun formatCompactMoney(money: Money): String {
    val dong = money.amountInCents / 100
    return when {
        dong < 1000L -> dong.toString()
        dong < 1000000L -> "${dong / 1000}k"
        else -> {
            val millions = dong / 1000000.0
            if (millions % 1.0 == 0.0) {
                "${millions.toInt()}Tr"
            } else {
                String.format(java.util.Locale.US, "%.1fTr", millions)
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
