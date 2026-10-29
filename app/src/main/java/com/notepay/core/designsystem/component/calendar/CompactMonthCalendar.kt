package com.notepay.core.designsystem.component.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Độ đậm của nền ô khi ngày đang được chọn. */
private const val SelectedCellWash = 0.07f

/** Độ đậm của thanh mật độ trên ngày thường — đủ thấy trên nền giấy, không thành một khối mực. */
private const val ActivityBarAlpha = 0.45f

/**
 * Bề rộng thanh mật độ theo mức cường độ 1..3.
 *
 * Trước đây là cụm 1-3 chấm 3dp, nhưng 3 chấm cạnh nhau bị đọc thành dấu "...", tức là
 * "còn nữa" thay vì "ngày này chi nhiều". Một thanh dài dần đọc đúng nghĩa hơn và vẫn chỉ
 * tốn 3dp chiều cao.
 */
private val ActivityBarWidths = listOf(6.dp, 11.dp, 16.dp)

/**
 * Lịch tháng gọn dùng cho màn Giao dịch.
 *
 * Mỗi ô cao tối thiểu 44dp và chỉ chứa số ngày + một thanh mật độ, nhờ đó cả tháng 5-6 tuần
 * tốn khoảng 300dp thay vì chiếm trọn màn hình như bảng lịch cũ. Chi tiết từng ngày nằm ngay
 * bên dưới trong danh sách giao dịch — chạm một ngày là cuộn tới nhóm ngày đó, không mở dialog.
 *
 * @param onGoToToday chỉ dùng khi đang xem tháng khác tháng hiện tại.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactMonthCalendar(
    grid: MonthGrid,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val isViewingOtherMonth = grid.year != today.year || grid.month != today.month.ordinal + 1

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.stats_previous_month),
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.calendar_month_year_format, grid.month, grid.year),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.stats_next_month),
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            val weekdayLabels = listOf(
                stringResource(R.string.calendar_weekday_monday),
                stringResource(R.string.calendar_weekday_tuesday),
                stringResource(R.string.calendar_weekday_wednesday),
                stringResource(R.string.calendar_weekday_thursday),
                stringResource(R.string.calendar_weekday_friday),
                stringResource(R.string.calendar_weekday_saturday),
                stringResource(R.string.calendar_weekday_sunday),
            )
            weekdayLabels.forEach { label ->
                Box(
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = AppTheme.colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }

        grid.weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { cell ->
                    CalendarDayCell(
                        cell = cell,
                        isToday = cell.date == today,
                        isSelected = cell.date == selectedDate,
                        onClick = { onDayClick(cell.date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonthTotalsText(
                month = grid.month,
                expense = MoneyFormatter.formatCompactVietnamese(grid.totalExpense),
                income = MoneyFormatter.formatCompactVietnamese(grid.totalIncome),
                modifier = Modifier.weight(1f),
            )
            if (isViewingOtherMonth) {
                Surface(
                    onClick = onGoToToday,
                    shape = AppTheme.shapes.corner12,
                    color = AppTheme.colors.sand,
                    contentColor = AppTheme.colors.onSand,
                ) {
                    Box(
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.calendar_today_action),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthTotalsText(
    month: Int,
    expense: String,
    income: String,
    modifier: Modifier = Modifier,
) {
    val template = stringResource(R.string.calendar_month_totals_format)
    val expenseColor = AppTheme.colors.error
    val incomeColor = AppTheme.colors.success
    val text = remember(template, month, expense, income, expenseColor, incomeColor) {
        buildMonthTotals(template, month, expense, income, expenseColor, incomeColor)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = AppTheme.colors.textSecondary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * Ghép dòng tổng tháng từ mẫu chuỗi đã dịch, tô riêng màu cho tiền chi và tiền thu.
 *
 * Cắt mẫu theo các chỗ trống %1$d / %2$s / %3$s thay vì tự nối chữ "Chi"/"Thu", nhờ đó
 * thứ tự từ ngữ vẫn do bản dịch quyết định chứ không bị khoá vào tiếng Việt.
 */
private fun buildMonthTotals(
    template: String,
    month: Int,
    expense: String,
    income: String,
    expenseColor: Color,
    incomeColor: Color,
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in SLOT_PATTERN.findAll(template)) {
        append(template.substring(cursor, match.range.first))
        when (match.groupValues[1]) {
            "1" -> append(month.toString())
            "2" -> withStyle(SpanStyle(color = expenseColor)) { append(expense) }
            else -> withStyle(SpanStyle(color = incomeColor)) { append(income) }
        }
        cursor = match.range.last + 1
    }
    append(template.substring(cursor))
}

/** Chỗ trống định dạng của [R.string.calendar_month_totals_format]. */
private val SLOT_PATTERN = Regex("""%([123])\$[ds]""")

@Composable
private fun CalendarDayCell(
    cell: MonthDayCell,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = AppTheme.colors.ink
    val dayOfMonth = cell.date.day
    val accessibilityLabel = stringResource(
        if (cell.dotLevel > 0) R.string.calendar_day_activity_cd else R.string.calendar_day_cd,
        dayOfMonth,
    )
    val dayNumberColor = when {
        isToday -> AppTheme.colors.onInk
        !cell.inCurrentMonth -> AppTheme.colors.textMuted
        isSelected -> ink
        else -> AppTheme.colors.textPrimary
    }

    Column(
        modifier = modifier
            // Không đặt height cố định: ở font scale 2.0 số ngày cao gần gấp đôi mà ô vẫn phải
            // giãn ra để không cắt chân chữ.
            .defaultMinSize(minHeight = 44.dp)
            .then(
                if (isSelected) {
                    Modifier.background(ink.copy(alpha = SelectedCellWash), AppTheme.shapes.corner12)
                } else {
                    Modifier
                }
            )
            // selectable thay vì clickable: nó công bố trạng thái "đã chọn" cho TalkBack
            // bằng đúng thuộc tính semantics mà nền tảng đọc, không chỉ đổi màu nền.
            .then(
                if (cell.inCurrentMonth) {
                    Modifier.selectable(
                        selected = isSelected,
                        role = Role.Button,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityLabel
            }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = 26.dp)
                .defaultMinSize(minHeight = 26.dp)
                .then(
                    if (isToday) Modifier.background(ink, AppTheme.shapes.circle) else Modifier
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                color = dayNumberColor,
                maxLines = 1,
            )
        }

        if (cell.dotLevel > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .width(ActivityBarWidths[cell.dotLevel - 1])
                    .height(3.dp)
                    .background(
                        ink.copy(alpha = if (isToday) 1f else ActivityBarAlpha),
                        AppTheme.shapes.capsule,
                    )
            )
        }
    }
}
