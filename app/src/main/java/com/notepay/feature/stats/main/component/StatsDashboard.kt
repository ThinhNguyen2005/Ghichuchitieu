package com.notepay.feature.stats.main.component

import com.notepay.core.designsystem.theme.AppTheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NorthEast
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.MonthlyTrendPoint
import com.notepay.feature.stats.main.StatsUiState
import com.notepay.feature.stats.main.TrendWindow
import com.notepay.feature.stats.main.resolve
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.WindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.formatter.MoneyFormatter
import kotlin.math.abs

internal enum class StatsViewType { PHAN_BO, XU_HUONG }
enum class StatsMetric { CHI_TIEU, THU_NHAP }

@Composable
internal fun StatsDashboard(
    state: StatsUiState,
    showAmounts: Boolean,
    viewType: StatsViewType,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (StatsRange) -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTrendWindowSelected: (TrendWindow) -> Unit,
    onViewTypeChanged: (StatsViewType) -> Unit,
    supportingContent: @Composable (StatsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by rememberSaveable { mutableStateOf(StatsMetric.CHI_TIEU) }
    // Đổi tab Thu/Chi phải đóng danh mục đang mở ngay trong handler: danh mục của tab cũ
    // không tồn tại trong breakdown của tab mới. Để LaunchedEffect đuổi theo sẽ dư một frame
    // hiển thị sai và thêm một lần recompose.
    val handleMetricChanged: (StatsMetric) -> Unit = { newMetric ->
        if (newMetric != metric) {
            metric = newMetric
            if (state.selectedCategory != null) onCategorySelected(null)
        }
    }

    // Tablet: lề bù để nội dung không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    val windowLayout = LocalWindowLayout.current
    // Compact/Medium có thanh điều hướng nổi ở đáy nên phải chừa chỗ; chế độ rail thì không.
    val bottomClearance = if (windowLayout.usesNavigationRail) 24.dp else maxOf(
        120.dp,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

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
                        onMetricChanged = handleMetricChanged,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onSelectRange = onSelectRange,
                        onMonthSelected = onMonthSelected,
                        onCategorySelected = onCategorySelected,
                        onTrendWindowSelected = onTrendWindowSelected,
                        onViewTypeChanged = onViewTypeChanged,
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
                    onMetricChanged = handleMetricChanged,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectRange = onSelectRange,
                    onMonthSelected = onMonthSelected,
                    onCategorySelected = onCategorySelected,
                    onTrendWindowSelected = onTrendWindowSelected,
                    onViewTypeChanged = onViewTypeChanged,
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
    onSelectRange: (StatsRange) -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTrendWindowSelected: (TrendWindow) -> Unit,
    onViewTypeChanged: (StatsViewType) -> Unit,
) {
    val total = if (metric == StatsMetric.CHI_TIEU) state.totalExpense else state.totalIncome
    val previousTotal = if (metric == StatsMetric.CHI_TIEU) state.previousExpense else state.previousIncome
    val difference = if (previousTotal != null) total.amountInCents - previousTotal.amountInCents else null
    val breakdown = if (metric == StatsMetric.CHI_TIEU) state.breakdown else state.incomeBreakdown

    // Bộ chuyển Phân bổ / Xu hướng nằm ngay trên biểu đồ nó điều khiển, không chiếm chỗ
    // trên TopAppBar để tranh chỗ với tiêu đề màn hình.
    val chartHeader: @Composable () -> Unit = {
        if (state.currentPeriod?.range == StatsRange.MONTH) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                ViewModeToggle(viewType = viewType, onChanged = onViewTypeChanged)
            }
        }
    }

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
            chartHeader = chartHeader,
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
            chartHeader = chartHeader,
        ) {
            if (state.currentPeriod?.range == StatsRange.MONTH) {
                TrendChartContent(
                    points = state.recentMonths,
                    metric = metric,
                    isSelectedMonthCurrent = state.isCurrentMonth,
                    forecast = if (metric == StatsMetric.CHI_TIEU && state.spendingForecast?.prediction != null) state.spendingForecast.projectedSpend else null,
                    // Dự báo chỉ được cảnh báo khi có hạn mức tháng thật để so sánh.
                    budget = state.budgetLimit,
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

/** Bộ chọn đơn Material 3 dùng chung cho các bộ lọc trên màn Thống kê. */
@Composable
internal fun <T> StatsSegmentedButtonRow(
    options: List<Pair<T, String>>,
    selected: T?,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (value, label) ->
            val isSelected = value == selected
            SegmentedButton(
                selected = isSelected,
                onClick = { onSelected(value) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                // M3 mặc định tô segment đang chọn bằng secondaryContainer (#F4F1EC) trên
                // segment chưa chọn màu trắng: 1.13:1, dưới ngưỡng 3:1 của WCAG 1.4.11 nên
                // không nhận ra được đang chọn mục nào. Tô bằng ink để đạt 17.5:1 và nâng
                // vùng chạm lên 48dp (mặc định M3 là 40dp).
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    activeBorderColor = MaterialTheme.colorScheme.primary,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier.heightIn(min = 48.dp),
                icon = {},
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Bộ chọn một chế độ hiển thị bằng segmented buttons của Material 3. */
@Composable
private fun ViewModeToggle(
    viewType: StatsViewType,
    onChanged: (StatsViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        StatsViewType.PHAN_BO to stringResource(R.string.stats_view_allocation),
        StatsViewType.XU_HUONG to stringResource(R.string.stats_view_trend),
    )

    StatsSegmentedButtonRow(
        options = options,
        selected = viewType,
        onSelected = onChanged,
        modifier = modifier,
    )
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
    onSelectRange: (StatsRange) -> Unit,
    chartHeader: (@Composable () -> Unit)? = null,
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
                StatsRange.WEEK to stringResource(R.string.stats_range_week),
                StatsRange.MONTH to stringResource(R.string.stats_range_month),
                StatsRange.YEAR to stringResource(R.string.stats_range_year),
                StatsRange.ALL to stringResource(R.string.stats_range_all)
            )
            StatsSegmentedButtonRow(
                options = ranges,
                selected = state.currentPeriod?.range,
                onSelected = onSelectRange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
            )

            // Month navigation row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val hasArrows = state.currentPeriod?.range != StatsRange.ALL &&
                                state.currentPeriod?.range != StatsRange.CUSTOM
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
                        if (state.isCurrentMonth) stringResource(R.string.stats_current_month)
                        else state.dateRangeLabel.resolve(),
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
            ) {
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
    
                val absFormatted = MoneyFormatter.format(Money(abs(difference)))
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
                chartHeader?.invoke()
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
                role = Role.RadioButton,
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
