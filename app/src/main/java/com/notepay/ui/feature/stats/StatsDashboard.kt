package com.notepay.ui.feature.stats

import com.notepay.ui.theme.AppTheme

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.ui.adaptive.LocalContentGutter
import com.notepay.ui.adaptive.LocalWindowLayout
import com.notepay.ui.adaptive.WindowLayout
import com.notepay.ui.adaptive.usesNavigationRail
import com.notepay.ui.util.MoneyFormatter

internal enum class StatsViewType { PHAN_BO, XU_HUONG }
enum class StatsMetric { CHI_TIEU, THU_NHAP }

@Composable
internal fun StatsDashboard(
    state: StatsUiState,
    showAmounts: Boolean,
    viewType: StatsViewType,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (com.notepay.domain.analytics.StatsRange) -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTrendWindowSelected: (TrendWindow) -> Unit,
    supportingContent: @Composable (StatsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by rememberSaveable { mutableStateOf(StatsMetric.CHI_TIEU) }
    // Tablet: lề bù để nội dung không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    val windowLayout = LocalWindowLayout.current
    // Compact/Medium có thanh điều hướng nổi ở đáy nên phải chừa chỗ; chế độ rail thì không.
    val bottomClearance = if (windowLayout.usesNavigationRail) 24.dp else 120.dp

    if (windowLayout == WindowLayout.Expanded) {
        // Expanded: biểu đồ + bộ lọc thời gian bên trái, phân tích chi tiết/insights bên phải.
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp + contentGutter, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(0.56f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "stats-chart-mode") {
                    StatsChartPane(
                        state = state,
                        metric = metric,
                        viewType = viewType,
                        showAmounts = showAmounts,
                        onMetricChanged = { metric = it },
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onSelectRange = onSelectRange,
                        onMonthSelected = onMonthSelected,
                        onCategorySelected = onCategorySelected,
                        onTrendWindowSelected = onTrendWindowSelected,
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(0.44f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(bottom = bottomClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "stats-supporting") { supportingContent(metric) }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + contentGutter,
                end = 16.dp + contentGutter,
                top = 16.dp,
                bottom = bottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "stats-chart-mode") {
                StatsChartPane(
                    state = state,
                    metric = metric,
                    viewType = viewType,
                    showAmounts = showAmounts,
                    onMetricChanged = { metric = it },
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectRange = onSelectRange,
                    onMonthSelected = onMonthSelected,
                    onCategorySelected = onCategorySelected,
                    onTrendWindowSelected = onTrendWindowSelected,
                )
            }
            item { supportingContent(metric) }
        }
    }
}

/** Biểu đồ tổng quan + bộ lọc thời gian/kỳ dùng chung cho bố cục 1 cột và 2 cột. */
@Composable
private fun StatsChartPane(
    state: StatsUiState,
    metric: StatsMetric,
    viewType: StatsViewType,
    showAmounts: Boolean,
    onMetricChanged: (StatsMetric) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (com.notepay.domain.analytics.StatsRange) -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTrendWindowSelected: (TrendWindow) -> Unit,
) {
    val total = if (metric == StatsMetric.CHI_TIEU) state.totalExpense else state.totalIncome
    val previousTotal = if (metric == StatsMetric.CHI_TIEU) state.previousExpense else state.previousIncome
    val difference = if (previousTotal != null) total.amountInCents - previousTotal.amountInCents else null
    val breakdown = if (metric == StatsMetric.CHI_TIEU) state.breakdown else state.incomeBreakdown

    when (viewType) {
        StatsViewType.PHAN_BO -> OverviewCard(
            state = state,
            metric = metric,
            showAmounts = showAmounts,
            difference = difference,
            onMetricChanged = onMetricChanged,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectRange = onSelectRange,
        ) {
            AllocationChartContent(
                breakdown = breakdown,
                total = total,
                showAmounts = showAmounts,
                selectedCategory = state.selectedCategory,
                onCategorySelected = onCategorySelected,
            )
        }

        StatsViewType.XU_HUONG -> OverviewCard(
            state = state,
            metric = metric,
            showAmounts = showAmounts,
            difference = difference,
            onMetricChanged = onMetricChanged,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectRange = onSelectRange,
        ) {
            if (state.currentPeriod?.range == com.notepay.domain.analytics.StatsRange.MONTH) {
                TrendChartContent(
                    points = state.recentMonths,
                    metric = metric,
                    isSelectedMonthCurrent = state.isCurrentMonth,
                    forecast = if (metric == StatsMetric.CHI_TIEU && state.spendingForecast?.prediction != null) state.spendingForecast.projectedSpend else null,
                    showAmounts = showAmounts,
                    trendWindow = state.trendWindow,
                    onTrendWindowSelected = onTrendWindowSelected,
                    onPointClick = onMonthSelected,
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.stats_trend_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun StatsHeader(
    viewType: StatsViewType,
    onViewTypeChanged: (StatsViewType) -> Unit,
) {
    ViewModeToggle(
        viewType = viewType,
        onChanged = onViewTypeChanged,
    )
}

/**
 * Bộ chuyển chế độ xem (Phân bổ / Xu hướng):
 * Tab chưa chọn chỉ hiển thị icon, tab đang chọn mở rộng thành [Icon] + [Text] trên nền token Sand.
 * Mỗi tab dùng [Role.Tab], trạng thái selected và vùng chạm tối thiểu 48dp.
 */
@Composable
private fun ViewModeToggle(
    viewType: StatsViewType,
    onChanged: (StatsViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceContainer
    val selectedColor = MaterialTheme.colorScheme.surface
    val selectedContentColor = MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner16,
        color = trackColor,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val isPhanBo = viewType == StatsViewType.PHAN_BO
            val allocationLabel = stringResource(R.string.stats_view_allocation)

            // 1. Tab Phân bổ
            Surface(
                modifier = Modifier
                    .clip(AppTheme.shapes.corner12)
                    .semantics { contentDescription = allocationLabel }
                    .selectable(
                        selected = isPhanBo,
                        role = Role.Tab,
                        onClick = { onChanged(StatsViewType.PHAN_BO) },
                    ),
                shape = AppTheme.shapes.corner12,
                color = if (isPhanBo) selectedColor else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = if (isPhanBo) 12.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PieChart,
                        contentDescription = null,
                        tint = if (isPhanBo) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    if (isPhanBo) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            allocationLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedContentColor,
                            maxLines = 1,
                        )
                    }
                }
            }

            // 2. Tab Xu hướng
            val isXuHuong = viewType == StatsViewType.XU_HUONG
            val trendLabel = stringResource(R.string.stats_view_trend)
            Surface(
                modifier = Modifier
                    .clip(AppTheme.shapes.corner12)
                    .semantics { contentDescription = trendLabel }
                    .selectable(
                        selected = isXuHuong,
                        role = Role.Tab,
                        onClick = { onChanged(StatsViewType.XU_HUONG) },
                    ),
                shape = AppTheme.shapes.corner12,
                color = if (isXuHuong) selectedColor else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = if (isXuHuong) 12.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.BarChart,
                        contentDescription = null,
                        tint = if (isXuHuong) selectedContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                    if (isXuHuong) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            trendLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = selectedContentColor,
                            maxLines = 1,
                        )
                    }
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
    difference: Long?,
    onMetricChanged: (StatsMetric) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (com.notepay.domain.analytics.StatsRange) -> Unit,
    chartContent: (@Composable () -> Unit)? = null,
) {
    val previousExpense = state.previousExpense
    val previousIncome = state.previousIncome

    val expenseDelta = if (previousExpense != null) state.totalExpense.amountInCents - previousExpense.amountInCents else null
    val incomeDelta = if (previousIncome != null) state.totalIncome.amountInCents - previousIncome.amountInCents else null

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val ranges = listOf(
                com.notepay.domain.analytics.StatsRange.WEEK to stringResource(R.string.stats_range_week),
                com.notepay.domain.analytics.StatsRange.MONTH to stringResource(R.string.stats_range_month),
                com.notepay.domain.analytics.StatsRange.YEAR to stringResource(R.string.stats_range_year),
                com.notepay.domain.analytics.StatsRange.ALL to stringResource(R.string.stats_range_all)
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
            ) {
                for (index in ranges.indices) {
                    val (range, label) = ranges[index]
                    SegmentedButton(
                        selected = state.currentPeriod?.range == range,
                        onClick = { onSelectRange(range) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                        icon = {},
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
            }

            // Month navigation row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val hasArrows = state.currentPeriod?.range != com.notepay.domain.analytics.StatsRange.ALL && 
                                state.currentPeriod?.range != com.notepay.domain.analytics.StatsRange.CUSTOM
                if (hasArrows) {
                    IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.stats_previous_month))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.isCurrentMonth) stringResource(R.string.stats_current_month) else state.dateRangeLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (hasArrows) {
                    IconButton(onClick = onNextMonth, enabled = !state.isLatestPeriod, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.stats_next_month))
                    }
                } else {
                    Spacer(Modifier.size(48.dp))
                }
            }

            // 2 Metric Cards: Chi tiêu | Thu nhập
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = stringResource(R.string.stats_expense),
                    amount = state.totalExpense,
                    showAmounts = showAmounts,
                    delta = expenseDelta,
                    positiveDeltaIsGood = false,
                    active = metric == StatsMetric.CHI_TIEU,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onMetricChanged(StatsMetric.CHI_TIEU) },
                    modifier = Modifier.weight(1f),
                )
                MetricCard(
                    title = stringResource(R.string.stats_income),
                    amount = state.totalIncome,
                    showAmounts = showAmounts,
                    delta = incomeDelta,
                    positiveDeltaIsGood = true,
                    active = metric == StatsMetric.THU_NHAP,
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { onMetricChanged(StatsMetric.THU_NHAP) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Comparison alert banner below tabs
            if (difference != null) {
                val isExpense = metric == StatsMetric.CHI_TIEU
                val isDecrease = difference < 0
                val isFlat = difference == 0L
                val isGood = if (isExpense) isDecrease else !isDecrease && !isFlat
    
                val bannerBg = when {
                    isFlat -> MaterialTheme.colorScheme.surfaceContainerHigh
                    isGood -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.errorContainer
                }
                val bannerContentColor = when {
                    isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
                    isGood -> AppTheme.colors.success
                    else -> MaterialTheme.colorScheme.error
                }
                val bannerIcon = when {
                    isFlat -> Icons.Rounded.Info
                    isGood -> Icons.Rounded.CheckCircle
                    else -> Icons.Rounded.Warning
                }
    
                val absFormatted = MoneyFormatter.format(Money(kotlin.math.abs(difference)))
                val bannerText = when {
                    !showAmounts -> stringResource(R.string.stats_amounts_hidden)
                    isFlat -> stringResource(R.string.stats_stable_previous)
                    isExpense -> if (isDecrease) {
                        stringResource(R.string.stats_banner_expense_decrease, absFormatted)
                    } else {
                        stringResource(R.string.stats_banner_expense_increase, absFormatted)
                    }
                    else -> if (!isDecrease) {
                        stringResource(R.string.stats_banner_income_increase, absFormatted)
                    } else {
                        stringResource(R.string.stats_banner_income_decrease, absFormatted)
                    }
                }
    
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner12)
                        .background(bannerBg)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        bannerIcon,
                        contentDescription = null,
                        tint = bannerContentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bannerText,
                        color = bannerContentColor,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            chartContent?.let { content ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .35f))
                content()
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    amount: Money,
    showAmounts: Boolean,
    delta: Long?,
    positiveDeltaIsGood: Boolean,
    active: Boolean,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = AppTheme.shapes.corner16
    val amountText = if (showAmounts) MoneyFormatter.format(amount) else "••••••"
    val amountStyle = if (amountText.length > 13) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.titleMedium
    }

    val containerColor = if (active) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

    val isIncrease = delta != null && delta > 0L
    val isFlat = delta == null || delta == 0L
    val isGood = if (isFlat) true else if (positiveDeltaIsGood) isIncrease else !isIncrease
    val statusColor = when {
        isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> AppTheme.colors.success
        else -> AppTheme.colors.error
    }
    val statusIcon = when {
        delta == null -> Icons.Rounded.Remove
        isFlat -> Icons.Rounded.Remove
        isIncrease -> Icons.Rounded.NorthEast
        else -> Icons.Rounded.SouthEast
    }
    val trendLabel = when {
        isFlat -> stringResource(R.string.stats_unchanged)
        isIncrease -> stringResource(R.string.stats_increase)
        else -> stringResource(R.string.stats_decrease)
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .selectable(
                selected = active,
                role = Role.Tab,
                onClick = onClick,
            ),
        shape = shape,
        color = containerColor,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                // Trend status icon ONLY in subtle circle badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = trendLabel,
                        tint = statusColor,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Text(
                amountText,
                style = amountStyle,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
