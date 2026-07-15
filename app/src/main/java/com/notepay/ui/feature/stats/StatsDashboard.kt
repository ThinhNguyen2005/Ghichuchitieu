package com.notepay.ui.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

internal enum class StatsViewType { PHAN_BO, XU_HUONG }
enum class StatsMetric { CHI_TIEU, THU_NHAP }

@Composable
fun StatsDashboard(
    state: StatsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    supportingContent: @Composable (StatsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewType by rememberSaveable { mutableStateOf(StatsViewType.PHAN_BO) }
    var metric by rememberSaveable { mutableStateOf(StatsMetric.CHI_TIEU) }
    var showAmounts by rememberSaveable { mutableStateOf(true) }
    val total = if (metric == StatsMetric.CHI_TIEU) state.totalExpense else state.totalIncome
    val previousTotal = state.recentMonths.getOrNull(1)?.let {
        if (metric == StatsMetric.CHI_TIEU) it.expense else it.income
    } ?: Money.ZERO
    val difference = total.amountInCents - previousTotal.amountInCents
    val breakdown = if (metric == StatsMetric.CHI_TIEU) state.breakdown else state.incomeBreakdown

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            StatsHeader(
                viewType = viewType,
                showAmounts = showAmounts,
                onToggleVisibility = { showAmounts = !showAmounts },
                onViewTypeChanged = { viewType = it },
            )
        }
        item {
            OverviewCard(
                state = state,
                metric = metric,
                showAmounts = showAmounts,
                difference = difference,
                onMetricChanged = { metric = it },
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
            )
        }
        item {
            when (viewType) {
                StatsViewType.PHAN_BO -> AllocationChart(
                    breakdown = breakdown,
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = onCategorySelected,
                )

                StatsViewType.XU_HUONG -> TrendChart(
                    points = state.recentMonths,
                    metric = metric,
                    isSelectedMonthCurrent = state.isCurrentMonth,
                    onPointClick = onMonthSelected,
                )
            }
        }
        item { supportingContent(metric) }
    }
}

@Composable
private fun StatsHeader(
    viewType: StatsViewType,
    showAmounts: Boolean,
    onToggleVisibility: () -> Unit,
    onViewTypeChanged: (StatsViewType) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Tình hình thu chi",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            IconButton(onClick = onToggleVisibility, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (showAmounts) Icons.Rounded.RemoveRedEye else Icons.Rounded.VisibilityOff,
                    contentDescription = if (showAmounts) "Ẩn số tiền" else "Hiện số tiền",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        ViewModeToggle(viewType = viewType, onChanged = onViewTypeChanged)
    }
}

@Composable
private fun ViewModeToggle(viewType: StatsViewType, onChanged: (StatsViewType) -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(modifier = Modifier.padding(3.dp), verticalAlignment = Alignment.CenterVertically) {
            ViewModeItem(
                active = viewType == StatsViewType.PHAN_BO,
                icon = Icons.Rounded.PieChart,
                label = "Phân bổ",
                onClick = { onChanged(StatsViewType.PHAN_BO) },
            )
            ViewModeItem(
                active = viewType == StatsViewType.XU_HUONG,
                icon = Icons.Rounded.BarChart,
                label = "Xu hướng",
                onClick = { onChanged(StatsViewType.XU_HUONG) },
            )
        }
    }
}

@Composable
private fun ViewModeItem(
    active: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .animateContentSize(animationSpec = tween(220)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (active) 10.dp else 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp), tint = tint)
            AnimatedVisibility(active) {
                Row {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = tint,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewCard(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    difference: Long,
    onMetricChanged: (StatsMetric) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val previousExpense = state.recentMonths.getOrNull(1)?.expense ?: Money.ZERO
    val previousIncome = state.recentMonths.getOrNull(1)?.income ?: Money.ZERO

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tháng trước")
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isCurrentMonth) "Tháng này" else "Tháng %02d/%d".format(state.month, state.year),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onNextMonth, enabled = !state.isCurrentMonth, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Tháng sau")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = "Chi tiêu",
                    amount = state.totalExpense,
                    showAmounts = showAmounts,
                    delta = state.totalExpense.amountInCents - previousExpense.amountInCents,
                    positiveDeltaIsGood = false,
                    active = metric == StatsMetric.CHI_TIEU,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    tint = MaterialTheme.colorScheme.error,
                    onClick = { onMetricChanged(StatsMetric.CHI_TIEU) },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Thu nhập",
                    amount = state.totalIncome,
                    showAmounts = showAmounts,
                    delta = state.totalIncome.amountInCents - previousIncome.amountInCents,
                    positiveDeltaIsGood = true,
                    active = metric == StatsMetric.THU_NHAP,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    tint = Color(0xFF22A06B),
                    onClick = { onMetricChanged(StatsMetric.THU_NHAP) },
                    modifier = Modifier.weight(1f),
                )
            }

            val isDecrease = difference < 0
            val comparisonPrefix = if (difference == 0L) "Không đổi" else if (isDecrease) "Giảm" else "Tăng"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = .45f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    comparisonPrefix,
                    color = if (isDecrease) Color(0xFF22A06B) else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${MoneyFormatter.format(Money(kotlin.math.abs(difference)))} so với tháng trước",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Money,
    showAmounts: Boolean,
    delta: Long,
    positiveDeltaIsGood: Boolean,
    active: Boolean,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val isExpense = title.contains("Chi", ignoreCase = true)
    val amountText = if (showAmounts) MoneyFormatter.format(amount) else "••••••"
    val amountStyle = if (amountText.length > 13) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.titleMedium
    }
    val containerColor = if (active) {
        if (isExpense) {
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
        } else {
            Color(0xFF22A06B).copy(alpha = 0.15f)
        }
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }

    Column(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        Text(
            amountText,
            style = amountStyle,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            overflow = TextOverflow.Visible,
        )
        TrendBadge(
            delta = delta,
            showAmounts = showAmounts,
            positiveDeltaIsGood = positiveDeltaIsGood,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun TrendBadge(
    delta: Long,
    showAmounts: Boolean,
    positiveDeltaIsGood: Boolean,
    modifier: Modifier = Modifier,
) {
    val isIncrease = delta > 0L
    val isFlat = delta == 0L
    val good = if (isFlat) true else if (positiveDeltaIsGood) isIncrease else !isIncrease
    val color = when {
        isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
        good -> Color(0xFF22A06B)
        else -> MaterialTheme.colorScheme.error
    }
    val icon = if (isIncrease) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown
    val label = when {
        !showAmounts -> "Đã ẩn số tiền"
        isFlat -> "Ổn định so với tháng trước"
        isIncrease -> "Tăng ${MoneyFormatter.format(Money(kotlin.math.abs(delta)))}"
        else -> "Giảm ${MoneyFormatter.format(Money(kotlin.math.abs(delta)))}"
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Visible,
            )
        }
    }
}

@Composable
private fun AllocationChart(
    breakdown: List<CategoryBreakdownItem>,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    val progress by animateFloatAsState(if (breakdown.isEmpty()) 0f else 1f, tween(650), label = "donut")
    val chartTrack = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    val labelItems = breakdown.take(4)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
            .pointerInput(breakdown, selectedCategory) {
                detectTapGestures { tap ->
                    val item = findDonutItemAt(
                        tap = tap,
                        width = size.width.toFloat(),
                        height = size.height.toFloat(),
                        strokePx = 34.dp.toPx(),
                        breakdown = breakdown,
                    )
                    onCategorySelected(if (item?.category == selectedCategory) null else item?.category)
                }
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height) * .58f
            val center = Offset(size.width / 2f, size.height / 2f + 8.dp.toPx())
            val stroke = 34.dp.toPx()
            val radius = diameter / 2f
            val bounds = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(diameter, diameter)

            drawCircle(chartTrack, radius, center, style = Stroke(stroke))

            var startAngle = CHART_START_ANGLE
            breakdown.forEach { item ->
                val rawSweep = item.percentage * 360f
                val gap = if (breakdown.size > 1) 2.2f else 0f
                val sweep = max(0f, rawSweep - gap) * progress
                val isSelected = item.category == selectedCategory
                val muted = selectedCategory != null && !isSelected
                drawArc(
                    color = Color(item.category.colorArgb).copy(alpha = if (muted) 0.34f else 1f),
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = bounds,
                    size = arcSize,
                    style = Stroke(
                        width = if (isSelected) stroke + 5.dp.toPx() else stroke,
                        cap = StrokeCap.Butt,
                    ),
                )
                startAngle += rawSweep
            }

            labelItems.forEach { item ->
                val middle = middleAngleForItem(breakdown, item)
                val arcPoint = pointOnCircle(center, radius + stroke * .54f, middle)
                val tickPoint = pointOnCircle(center, radius + stroke * .54f + 18.dp.toPx(), middle)
                drawLine(
                    color = connectorColor,
                    start = arcPoint,
                    end = tickPoint,
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }

        labelItems.forEachIndexed { index, item ->
            val alignment = if (index == 0 || index == 3) Alignment.Start else Alignment.End
            val modifier = when (index) {
                0 -> Modifier.align(Alignment.TopStart).padding(top = 22.dp)
                1 -> Modifier.align(Alignment.BottomEnd).padding(bottom = 24.dp)
                2 -> Modifier.align(Alignment.TopEnd).padding(top = 22.dp)
                else -> Modifier.align(Alignment.BottomStart).padding(bottom = 24.dp)
            }
            CategoryChartLabel(
                item = item,
                selected = item.category == selectedCategory,
                modifier = modifier,
                alignment = alignment,
                onClick = {
                    onCategorySelected(if (item.category == selectedCategory) null else item.category)
                },
            )
        }

        if (breakdown.isEmpty()) {
            Text(
                "Chưa có dữ liệu",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryChartLabel(
    item: CategoryBreakdownItem,
    selected: Boolean,
    modifier: Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
    onClick: () -> Unit,
) {
    val color = Color(item.category.colorArgb)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        Text(
            "${(item.percentage * 100).toInt()}%",
            color = color,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(category = item.category, size = 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                item.category.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrendChart(
    points: List<MonthlyTrendPoint>,
    metric: StatsMetric,
    isSelectedMonthCurrent: Boolean,
    onPointClick: (MonthlyTrendPoint) -> Unit,
) {
    val values = points.map { if (metric == StatsMetric.CHI_TIEU) it.expense.amountInCents else it.income.amountInCents }
    val maxValue = max(1L, values.maxOrNull() ?: 0L)
    val maxMillions = max(0.5f, kotlin.math.ceil(maxValue / 500_000f) * .5f)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f)
    val activeBarColor = MaterialTheme.colorScheme.primary
    val inactiveBarColor = activeBarColor.copy(alpha = .23f)

    Column(modifier = Modifier.fillMaxWidth().height(290.dp)) {
        Text("(Triệu)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.width(34.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(maxMillions, maxMillions * .75f, maxMillions * .5f, maxMillions * .25f, 0f).forEach { value ->
                    Text(
                        "%.1f".format(value),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                points.forEachIndexed { index, point ->
                    val fraction by animateFloatAsState(
                        (values.getOrElse(index) { 0L }.toFloat() / maxValue).coerceIn(0f, 1f),
                        label = "bar$index",
                    )
                    val isCurrent = index == points.lastIndex
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onPointClick(point) },
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Canvas(Modifier.fillMaxSize()) {
                                repeat(4) { line ->
                                    val y = size.height * line / 4f
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(.72f)
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(if (isCurrent) activeBarColor else inactiveBarColor),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (isCurrent && isSelectedMonthCurrent) "Tháng này" else "T${point.month}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

private const val CHART_START_ANGLE = -90f

private fun findDonutItemAt(
    tap: Offset,
    width: Float,
    height: Float,
    strokePx: Float,
    breakdown: List<CategoryBreakdownItem>,
): CategoryBreakdownItem? {
    if (breakdown.isEmpty()) return null
    val diameter = min(width, height) * .58f
    val radius = diameter / 2f
    val center = Offset(width / 2f, height / 2f + 8f)
    val dx = tap.x - center.x
    val dy = tap.y - center.y
    val distance = sqrt(dx * dx + dy * dy)
    if (distance !in (radius - strokePx * .75f)..(radius + strokePx * .75f)) return null

    val angle = normalizeAngle((atan2(dy, dx) * 180f / PI.toFloat()))
    var start = normalizeAngle(CHART_START_ANGLE)
    breakdown.forEach { item ->
        val sweep = item.percentage * 360f
        if (angleInSweep(angle, start, sweep)) return item
        start = normalizeAngle(start + sweep)
    }
    return null
}

private fun middleAngleForItem(
    breakdown: List<CategoryBreakdownItem>,
    target: CategoryBreakdownItem,
): Float {
    var start = CHART_START_ANGLE
    for (item in breakdown) {
        val sweep = item.percentage * 360f
        if (item.category == target.category) return start + sweep / 2f
        start += sweep
    }
    return CHART_START_ANGLE
}

private fun pointOnCircle(center: Offset, radius: Float, angleDegrees: Float): Offset {
    val radians = angleDegrees * PI.toFloat() / 180f
    return Offset(
        x = center.x + cos(radians) * radius,
        y = center.y + sin(radians) * radius,
    )
}

private fun normalizeAngle(value: Float): Float {
    val result = value % 360f
    return if (result < 0f) result + 360f else result
}

private fun angleInSweep(angle: Float, start: Float, sweep: Float): Boolean {
    val end = normalizeAngle(start + sweep)
    return if (start <= end) {
        angle in start..end
    } else {
        angle >= start || angle <= end
    }
}
