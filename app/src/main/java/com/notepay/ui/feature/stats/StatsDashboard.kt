package com.notepay.ui.feature.stats

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.SouthEast
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter

internal enum class StatsViewType { PHAN_BO, XU_HUONG }
enum class StatsMetric { CHI_TIEU, THU_NHAP }

// Signature MoMo Pink colors
private val MoMoPink = Color(0xFFE91E63)
private val MoMoPinkLightBorder = Color(0xFFF48FB1)

@Composable
fun StatsDashboard(
    state: StatsUiState,
    showAmounts: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onMonthSelected: (MonthlyTrendPoint) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    supportingContent: @Composable (StatsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewType by rememberSaveable { mutableStateOf(StatsViewType.XU_HUONG) }
    var metric by rememberSaveable { mutableStateOf(StatsMetric.CHI_TIEU) }
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
                onViewTypeChanged = { viewType = it },
            )
        }
        item(key = "stats-chart-mode") {
            when (viewType) {
                StatsViewType.PHAN_BO -> OverviewCard(
                    state = state,
                    metric = metric,
                    showAmounts = showAmounts,
                    difference = difference,
                    onMetricChanged = { metric = it },
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
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
                    onMetricChanged = { metric = it },
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                ) {
                    TrendChartContent(
                        points = state.recentMonths,
                        metric = metric,
                        isSelectedMonthCurrent = state.isCurrentMonth,
                        forecast = if (metric == StatsMetric.CHI_TIEU && state.spendingForecast?.prediction != null) state.spendingForecast.projectedSpend else null,
                        showAmounts = showAmounts,
                        onPointClick = onMonthSelected,
                    )
                }
            }
        }
        item { supportingContent(metric) }
    }
}

@Composable
private fun StatsHeader(
    viewType: StatsViewType,
    onViewTypeChanged: (StatsViewType) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        ViewModeToggle(
            viewType = viewType,
            onChanged = onViewTypeChanged,
        )
    }
}

/**
 * MoMo-style View Mode Filter Toggle:
 * Unselected option displays ONLY the icon (compact).
 * Selected option expands to show [Icon] + [Text] in signature MoMo pink accent color on soft pink tint background.
 */
@Composable
private fun ViewModeToggle(
    viewType: StatsViewType,
    onChanged: (StatsViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val momoPinkBg = if (isDark) Color(0xFF3D1426) else Color(0xFFFFF0F5)
    val cardBg = if (isDark) MaterialTheme.colorScheme.surfaceContainerHigh else Color.White

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val isPhanBo = viewType == StatsViewType.PHAN_BO

            // 1. Tab Phân bổ
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onChanged(StatsViewType.PHAN_BO) },
                shape = RoundedCornerShape(10.dp),
                color = if (isPhanBo) momoPinkBg else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isPhanBo) 10.dp else 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.PieChart,
                        contentDescription = stringResource(R.string.stats_view_allocation),
                        tint = if (isPhanBo) MoMoPink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    if (isPhanBo) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.stats_view_allocation),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MoMoPink,
                            maxLines = 1,
                        )
                    }
                }
            }

            // 2. Tab Xu hướng
            val isXuHuong = viewType == StatsViewType.XU_HUONG
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onChanged(StatsViewType.XU_HUONG) },
                shape = RoundedCornerShape(10.dp),
                color = if (isXuHuong) momoPinkBg else Color.Transparent,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = if (isXuHuong) 10.dp else 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.BarChart,
                        contentDescription = stringResource(R.string.stats_view_trend),
                        tint = if (isXuHuong) MoMoPink else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                    if (isXuHuong) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.stats_view_trend),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MoMoPink,
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
    difference: Long,
    onMetricChanged: (StatsMetric) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    chartContent: (@Composable () -> Unit)? = null,
) {
    val previousExpense = state.recentMonths.getOrNull(1)?.expense ?: Money.ZERO
    val previousIncome = state.recentMonths.getOrNull(1)?.income ?: Money.ZERO

    val expenseDelta = state.totalExpense.amountInCents - previousExpense.amountInCents
    val incomeDelta = state.totalIncome.amountInCents - previousIncome.amountInCents

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Month navigation row
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.stats_previous_month))
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.isCurrentMonth) stringResource(R.string.stats_current_month) else stringResource(R.string.stats_month_year_format, state.month, state.year),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(onClick = onNextMonth, enabled = !state.isCurrentMonth, modifier = Modifier.size(44.dp)) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = stringResource(R.string.stats_next_month))
                }
            }

            // 2 Metric Cards: Chi tiêu | Thu nhập
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard(
                    title = stringResource(R.string.stats_expense),
                    amount = state.totalExpense,
                    showAmounts = showAmounts,
                    delta = expenseDelta,
                    positiveDeltaIsGood = false,
                    active = metric == StatsMetric.CHI_TIEU,
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    tint = MoMoPink,
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
                    tint = MoMoPink,
                    onClick = { onMetricChanged(StatsMetric.THU_NHAP) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Comparison alert banner below tabs
            val isExpense = metric == StatsMetric.CHI_TIEU
            val isDecrease = difference < 0
            val isFlat = difference == 0L
            val isGood = if (isExpense) isDecrease else !isDecrease && !isFlat

            val bannerBg = when {
                isFlat -> MaterialTheme.colorScheme.surfaceContainerHigh
                isGood -> if (isSystemInDarkTheme()) Color(0xFF1B382B) else Color(0xFFE8F5E9)
                else -> if (isSystemInDarkTheme()) Color(0xFF3E1B1B) else Color(0xFFFFEBEE)
            }
            val bannerContentColor = when {
                isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
                isGood -> Color(0xFF2E7D32)
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(bannerBg)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
    delta: Long,
    positiveDeltaIsGood: Boolean,
    active: Boolean,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val amountText = if (showAmounts) MoneyFormatter.format(amount) else "••••••"
    val amountStyle = if (amountText.length > 13) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.titleMedium
    }

    val isDark = isSystemInDarkTheme()
    val containerColor = if (active) {
        if (isDark) Color(0xFF2A1B24) else Color.White
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceContainerLow else Color(0xFFFAFAFA)
    }

    val isIncrease = delta > 0L
    val isFlat = delta == 0L
    val isGood = if (isFlat) true else if (positiveDeltaIsGood) isIncrease else !isIncrease
    val statusColor = when {
        isFlat -> MaterialTheme.colorScheme.onSurfaceVariant
        isGood -> Color(0xFF2E7D32)
        else -> Color(0xFFE65100)
    }
    val statusIcon = when {
        isFlat -> Icons.Rounded.Remove
        isIncrease -> Icons.Rounded.NorthEast
        else -> Icons.Rounded.SouthEast
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = containerColor,
        border = BorderStroke(
            width = if (active) 1.5.dp else 1.dp,
            color = if (active) MoMoPinkLightBorder else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
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
                        contentDescription = null,
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
