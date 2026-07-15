package com.notepay.ui.feature.stats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.max
import kotlin.math.min

internal enum class StatsViewType { PHAN_BO, XU_HUONG }
enum class StatsMetric { CHI_TIEU, THU_NHAP }

/**
 * Premium, compact entry point for statistics. State is local UI state; month data itself remains
 * in [StatsViewModel] and is therefore retained across recomposition and configuration changes.
 */
@Composable
fun StatsDashboard(
    state: StatsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (com.notepay.domain.model.Category?) -> Unit,
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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
            Text("Tình hình thu chi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
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
        modifier = Modifier,
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
private fun ViewModeItem(active: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
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
                    Text(label, style = MaterialTheme.typography.labelLarge, color = tint, fontWeight = FontWeight.Bold)
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
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
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
                    title = "Chi tiêu", amount = state.totalExpense, showAmounts = showAmounts,
                    active = metric == StatsMetric.CHI_TIEU, icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    tint = MaterialTheme.colorScheme.error, onClick = { onMetricChanged(StatsMetric.CHI_TIEU) }, modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = "Thu nhập", amount = state.totalIncome, showAmounts = showAmounts,
                    active = metric == StatsMetric.THU_NHAP, icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    tint = Color(0xFF22A06B), onClick = { onMetricChanged(StatsMetric.THU_NHAP) }, modifier = Modifier.weight(1f),
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
                Icon(Icons.Rounded.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(comparisonPrefix, color = if (isDecrease) Color(0xFF22A06B) else MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(4.dp))
                Text("${MoneyFormatter.format(Money(kotlin.math.abs(difference)))} so với cùng kỳ tháng trước", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), maxLines = 1)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Money,
    showAmounts: Boolean,
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val isExpense = title.contains("Chi tiêu", ignoreCase = true)
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
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(5.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(5.dp))
        Text(if (showAmounts) MoneyFormatter.format(amount) else "••••••", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun AllocationChart(
    breakdown: List<CategoryBreakdownItem>,
    selectedCategory: com.notepay.domain.model.Category?,
    onCategorySelected: (com.notepay.domain.model.Category?) -> Unit,
) {
    val progress by animateFloatAsState(if (breakdown.isEmpty()) 0f else 1f, tween(650), label = "donut")
    val primary = breakdown.firstOrNull()
    val secondary = breakdown.getOrNull(1)
    val chartTrack = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = min(size.width, size.height) * .54f
            val center = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
            val stroke = 34.dp.toPx()
            drawCircle(chartTrack, diameter / 2f, center, style = Stroke(stroke))
            var startAngle = -105f
            breakdown.forEach { item ->
                val sweep = item.percentage * 360f * progress
                drawArc(Color(item.category.colorArgb), startAngle, sweep, false, Offset(center.x - diameter / 2f, center.y - diameter / 2f), Size(diameter, diameter), style = Stroke(stroke, cap = StrokeCap.Round))
                startAngle += item.percentage * 360f
            }
            primary?.let {
                drawLine(connectorColor, Offset(center.x - diameter / 2f, center.y - diameter / 3f), Offset(18.dp.toPx(), 54.dp.toPx()), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
            }
            secondary?.let {
                drawLine(connectorColor, Offset(center.x + diameter / 2f, center.y + diameter / 3f), Offset(size.width - 18.dp.toPx(), size.height - 42.dp.toPx()), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
            }
        }
        primary?.let { item -> CategoryChartLabel(item, Modifier.align(Alignment.TopStart).padding(top = 32.dp), onClick = { onCategorySelected(item.category) }) }
        secondary?.let { item -> CategoryChartLabel(item, Modifier.align(Alignment.BottomEnd).padding(bottom = 20.dp), alignment = Alignment.End, onClick = { onCategorySelected(item.category) }) }
        if (breakdown.isEmpty()) Text("Chưa có dữ liệu chi tiêu", modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CategoryChartLabel(item: CategoryBreakdownItem, modifier: Modifier, alignment: Alignment.Horizontal = Alignment.Start, onClick: () -> Unit) {
    Column(modifier = modifier.clickable(onClick = onClick), horizontalAlignment = alignment) {
        Text("${(item.percentage * 100).toInt()}%", color = Color(item.category.colorArgb), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            CategoryAvatar(category = item.category, size = 16.dp)
            Spacer(Modifier.width(4.dp))
            Text(item.category.displayName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Column(modifier = Modifier.width(34.dp).fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                listOf(maxMillions, maxMillions * .75f, maxMillions * .5f, maxMillions * .25f, 0f).forEach { value ->
                    Text("%.1f".format(value), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(modifier = Modifier.weight(1f).fillMaxSize(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                points.forEachIndexed { index, point ->
                    val fraction by animateFloatAsState((values.getOrElse(index) { 0L }.toFloat() / maxValue).coerceIn(0f, 1f), label = "bar$index")
                    val isCurrent = index == points.lastIndex
                    Column(modifier = Modifier.weight(1f).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { onPointClick(point) }, contentAlignment = Alignment.BottomCenter) {
                            Canvas(Modifier.fillMaxSize()) {
                                repeat(4) { line ->
                                    val y = size.height * line / 4f
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth(.72f).fillMaxSize(fraction).clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)).background(if (isCurrent) activeBarColor else inactiveBarColor))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(if (isCurrent && isSelectedMonthCurrent) "Tháng này" else "T${point.month}", style = MaterialTheme.typography.labelMedium, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
