package com.notepay.feature.stats.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.WindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.rememberMainTabBottomClearance
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.CategoryBreakdownItem
import com.notepay.feature.stats.main.StatsUiState
import com.notepay.feature.stats.main.resolve
import com.notepay.feature.stats.main.tabularNums
import kotlin.math.abs

enum class StatsMetric { CHI_TIEU, THU_NHAP }

@Composable
internal fun StatsDashboard(
    state: StatsUiState,
    showAmounts: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (StatsRange) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTransactionClick: (Long) -> Unit,
    supportingContent: @Composable (StatsMetric) -> Unit,
    modifier: Modifier = Modifier,
) {
    var metric by rememberSaveable { mutableStateOf(StatsMetric.CHI_TIEU) }
    // Đổi tab Thu/Chi phải đóng danh mục đang mở ngay trong handler để tránh dư một frame sai.
    val handleMetricChanged: (StatsMetric) -> Unit = { newMetric ->
        if (newMetric != metric) {
            metric = newMetric
            if (state.selectedCategory != null) onCategorySelected(null)
        }
    }

    val isExpense = metric == StatsMetric.CHI_TIEU
    val breakdown = if (isExpense) state.breakdown else state.incomeBreakdown
    val activeSelectedCategory = remember(breakdown, state.selectedCategory) {
        state.selectedCategory?.takeIf { selected ->
            breakdown.any { it.category == selected }
        }
    }
    val selectedTransactions = remember(state.transactions, activeSelectedCategory, isExpense) {
        if (activeSelectedCategory == null) return@remember emptyList()
        val expectedType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
        state.transactions.filter { tx ->
            tx.type == expectedType && tx.category == activeSelectedCategory
        }
    }

    // Tablet: lề bù để nội dung không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    val windowLayout = LocalWindowLayout.current
    // Compact/Medium có thanh điều hướng nổi ở đáy nên phải chừa chỗ; chế độ rail thì không.
    val bottomClearance = rememberMainTabBottomClearance(windowLayout.usesNavigationRail)

    if (windowLayout == WindowLayout.Expanded) {
        // Expanded: bộ lọc + thẻ Ink + biểu đồ bên trái, ngân sách/insights bên phải.
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
                statsMainItems(
                    state = state,
                    metric = metric,
                    showAmounts = showAmounts,
                    breakdown = breakdown,
                    activeSelectedCategory = activeSelectedCategory,
                    selectedTransactions = selectedTransactions,
                    onMetricChanged = handleMetricChanged,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectRange = onSelectRange,
                    onCategorySelected = onCategorySelected,
                    onTransactionClick = onTransactionClick,
                )
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
                top = 12.dp,
                bottom = bottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            statsMainItems(
                state = state,
                metric = metric,
                showAmounts = showAmounts,
                breakdown = breakdown,
                activeSelectedCategory = activeSelectedCategory,
                selectedTransactions = selectedTransactions,
                onMetricChanged = handleMetricChanged,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onSelectRange = onSelectRange,
                onCategorySelected = onCategorySelected,
                onTransactionClick = onTransactionClick,
            )
            item(key = "stats-supporting") { supportingContent(metric) }
        }
    }
}

private fun LazyListScope.statsMainItems(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    breakdown: List<CategoryBreakdownItem>,
    activeSelectedCategory: Category?,
    selectedTransactions: List<Transaction>,
    onMetricChanged: (StatsMetric) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (StatsRange) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    item(key = "stats-period-controls") {
        StatsPeriodControlBar(
            state = state,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onSelectRange = onSelectRange,
        )
    }

    item(key = "stats-hero-ink") {
        StatsInkHeroCard(
            state = state,
            metric = metric,
            showAmounts = showAmounts,
            onMetricChanged = onMetricChanged,
        )
    }

    item(key = "stats-category-structure") {
        StatsCategoryStructureCard(
            metric = metric,
            breakdown = breakdown,
            showAmounts = showAmounts,
            activeSelectedCategory = activeSelectedCategory,
            selectedTransactions = selectedTransactions,
            onCategorySelected = onCategorySelected,
            onTransactionClick = onTransactionClick,
        )
    }

    item(key = "stats-time-bar") {
        StatsTimeBarCard(
            state = state,
            metric = metric,
            activeSelectedCategory = activeSelectedCategory,
            showAmounts = showAmounts,
        )
    }

    state.longTermTrend?.let { trend ->
        item(key = "stats-long-term-trend") {
            LongTermTrendSection(
                analysis = trend,
                showAmounts = showAmounts,
            )
        }
    }
}

/** Thanh chọn kỳ trung tính theo `filter-chip` v4 và bộ lùi/tiến kỳ gọn gàng. */
@Composable
private fun StatsPeriodControlBar(
    state: StatsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectRange: (StatsRange) -> Unit,
) {
    val ranges = listOf(
        StatsRange.WEEK to stringResource(R.string.stats_range_week),
        StatsRange.MONTH to stringResource(R.string.stats_range_month),
        StatsRange.YEAR to stringResource(R.string.stats_range_year),
        StatsRange.ALL to stringResource(R.string.stats_range_all),
    )
    val selectedRange = state.currentPeriod?.range
    val hasArrows = selectedRange != StatsRange.ALL && selectedRange != StatsRange.CUSTOM
    val periodLabel = if (state.isCurrentMonth) {
        stringResource(R.string.stats_current_month)
    } else {
        state.dateRangeLabel.resolve()
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (hasArrows) {
            StatsPeriodNavigatorPill(
                periodLabel = periodLabel,
                isLatestPeriod = state.isLatestPeriod,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onJumpToCurrent = { selectedRange?.let(onSelectRange) },
                fillLabel = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        val rangeBarBg = AppTheme.colors.surface.copy(
            alpha = if (AppTheme.colors.isLight) 0.88f else 0.82f,
        )
        Surface(
            shape = AppTheme.shapes.capsule,
            color = rangeBarBg,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape = AppTheme.shapes.capsule, elevated = false),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ranges.forEach { (range, label) ->
                    val isSelected = range == selectedRange
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(AppTheme.shapes.capsule)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onSelectRange(range) },
                            ),
                        shape = AppTheme.shapes.capsule,
                        color = if (isSelected) AppTheme.colors.sand else Color.Transparent,
                        border = if (isSelected) BorderStroke(0.5.dp, AppTheme.colors.hairline) else null,
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) {
                                    AppTheme.colors.onSand
                                } else {
                                    AppTheme.colors.textSecondary
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsPeriodNavigatorPill(
    periodLabel: String,
    isLatestPeriod: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onJumpToCurrent: () -> Unit,
    fillLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val jumpLabel = stringResource(R.string.stats_jump_to_current_period)
    val pillShape = AppTheme.shapes.capsule
    val containerColor = AppTheme.colors.surface.copy(
        alpha = if (AppTheme.colors.isLight) 0.92f else 0.86f,
    )

    Surface(
        shape = pillShape,
        color = containerColor,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = modifier.iosCardShadow(shape = pillShape, elevated = false),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.stats_previous_month),
                    tint = AppTheme.colors.textPrimary,
                )
            }
            val labelModifier = (if (fillLabel) Modifier.weight(1f) else Modifier.widthIn(max = 160.dp))
                .defaultMinSize(minHeight = 48.dp)
                .clip(pillShape)
                .clickable(
                    enabled = !isLatestPeriod,
                    role = Role.Button,
                    onClickLabel = jumpLabel,
                    onClick = onJumpToCurrent,
                )
                .padding(horizontal = 4.dp)
            Row(
                modifier = labelModifier,
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onNextMonth,
                enabled = !isLatestPeriod,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = stringResource(R.string.stats_next_month),
                    tint = if (isLatestPeriod) {
                        AppTheme.colors.textMuted.copy(alpha = 0.45f)
                    } else {
                        AppTheme.colors.textPrimary
                    },
                )
            }
        }
    }
}

/** Luminous iOS elevated hero summary card for Reports (replaces legacy dark ink hero card). */
@Composable
private fun StatsInkHeroCard(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    onMetricChanged: (StatsMetric) -> Unit,
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
    val isExpense = metric == StatsMetric.CHI_TIEU
    val total = if (isExpense) state.totalExpense else state.totalIncome
    val counterpartTotal = if (isExpense) state.totalIncome else state.totalExpense
    val previousTotal = if (isExpense) state.previousExpense else state.previousIncome
    val difference = if (previousTotal != null) total.amountInCents - previousTotal.amountInCents else null
    val cardShape = AppTheme.shapes.corner28
    val isLargeFont = LocalDensity.current.fontScale >= 1.45f

    val heroGradient = remember(isLight) {
        val top = if (isLight) HomeHeroCardLightTop else HomeHeroCardDarkTop
        val bottom = if (isLight) HomeHeroCardLightBottom else HomeHeroCardDarkBottom
        Brush.verticalGradient(listOf(top, bottom))
    }
    val rimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = true),
        shape = cardShape,
        color = Color.Transparent,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, rimColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroGradient)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroMetricPill(
                    label = stringResource(R.string.stats_expense),
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    selected = isExpense,
                    onClick = { onMetricChanged(StatsMetric.CHI_TIEU) },
                    modifier = Modifier.weight(1f),
                )
                HeroMetricPill(
                    label = stringResource(R.string.stats_income),
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    selected = !isExpense,
                    onClick = { onMetricChanged(StatsMetric.THU_NHAP) },
                    modifier = Modifier.weight(1f),
                )
            }

            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(
                        if (isExpense) R.string.stats_total_expense else R.string.stats_total_income,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                NotePayAnimatedContent(
                    targetState = total to showAmounts,
                    label = "StatsHeroAmount",
                ) { (targetAmount, visible) ->
                    Text(
                        text = MoneyFormatter.format(targetAmount, visible),
                        style = AppTheme.typography.amountHero,
                        color = colors.textPrimary,
                        maxLines = if (isLargeFont) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HeroCashflowRatioBar(
                incomeCents = state.totalIncome.amountInCents,
                expenseCents = state.totalExpense.amountInCents,
                showAmounts = showAmounts,
            )

            HorizontalDivider(thickness = 0.5.dp, color = colors.hairline)

            HeroCashflowSummaryRow(
                isExpense = isExpense,
                counterpartTotal = counterpartTotal,
                netCents = state.totalIncome.amountInCents - state.totalExpense.amountInCents,
                showAmounts = showAmounts,
                isLargeFont = isLargeFont,
            )

            if (difference != null && difference != 0L && showAmounts) {
                val isDecrease = difference < 0L
                val isGood = if (isExpense) isDecrease else !isDecrease
                val badgeColor = if (isGood) colors.success else colors.error
                val badgeIcon = if (isGood) Icons.Rounded.CheckCircle else Icons.Rounded.Warning
                val absFormatted = MoneyFormatter.format(Money(abs(difference)))
                val bannerText = when {
                    isExpense && isDecrease -> stringResource(R.string.stats_banner_expense_decrease, absFormatted)
                    isExpense -> stringResource(R.string.stats_banner_expense_increase, absFormatted)
                    !isDecrease -> stringResource(R.string.stats_banner_income_increase, absFormatted)
                    else -> stringResource(R.string.stats_banner_income_decrease, absFormatted)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner16)
                        .background(badgeColor.copy(alpha = if (isLight) 0.10f else 0.16f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroCashflowRatioBar(
    incomeCents: Long,
    expenseCents: Long,
    showAmounts: Boolean,
) {
    if (!showAmounts || (incomeCents <= 0L && expenseCents <= 0L)) return
    val totalFlow = (incomeCents + expenseCents).coerceAtLeast(1L)
    val incomeWeight = (incomeCents.toFloat() / totalFlow.toFloat()).coerceIn(0f, 1f)
    val expenseWeight = (1f - incomeWeight).coerceAtLeast(0f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(AppTheme.shapes.capsule)
            .background(AppTheme.colors.surfaceSunken),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (incomeWeight > 0f) {
            Box(
                modifier = Modifier
                    .weight(incomeWeight)
                    .fillMaxHeight()
                    .background(AppTheme.colors.success),
            )
        }
        if (expenseWeight > 0f) {
            Box(
                modifier = Modifier
                    .weight(expenseWeight)
                    .fillMaxHeight()
                    .background(AppTheme.colors.error),
            )
        }
    }
}

@Composable
private fun HeroCashflowSummaryRow(
    isExpense: Boolean,
    counterpartTotal: Money,
    netCents: Long,
    showAmounts: Boolean,
    isLargeFont: Boolean,
) {
    val isLight = AppTheme.colors.isLight
    val tileAlpha = if (isLight) 0.10f else 0.16f
    val counterpartColor = if (isExpense) AppTheme.colors.success else AppTheme.colors.error
    val counterpartIcon = if (isExpense) {
        Icons.AutoMirrored.Rounded.TrendingUp
    } else {
        Icons.AutoMirrored.Rounded.TrendingDown
    }
    val counterpartSign = if (!showAmounts) "" else if (isExpense) "+" else "−"
    val counterpartAmount = "$counterpartSign${MoneyFormatter.format(counterpartTotal, showAmounts)}"
    val counterpartLabel = stringResource(if (isExpense) R.string.stats_income else R.string.stats_expense)

    val netColor = if (netCents >= 0L) AppTheme.colors.success else AppTheme.colors.error
    val formattedNetAbs = MoneyFormatter.format(Money(abs(netCents)), showAmounts)
    val netValueText = when {
        !showAmounts -> formattedNetAbs
        netCents >= 0L -> stringResource(R.string.amount_positive_format, formattedNetAbs)
        else -> stringResource(R.string.amount_negative_format, formattedNetAbs)
    }
    val netLabel = stringResource(R.string.stats_net_balance_label)

    if (isLargeFont) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatsHeroInsetTile(
                label = counterpartLabel,
                amountText = counterpartAmount,
                accentColor = counterpartColor,
                containerAlpha = tileAlpha,
                icon = counterpartIcon,
                modifier = Modifier.fillMaxWidth(),
            )
            StatsHeroInsetTile(
                label = netLabel,
                amountText = netValueText,
                accentColor = netColor,
                containerAlpha = tileAlpha,
                icon = if (netCents >= 0L) {
                    Icons.AutoMirrored.Rounded.TrendingUp
                } else {
                    Icons.AutoMirrored.Rounded.TrendingDown
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsHeroInsetTile(
                label = counterpartLabel,
                amountText = counterpartAmount,
                accentColor = counterpartColor,
                containerAlpha = tileAlpha,
                icon = counterpartIcon,
                modifier = Modifier.weight(1f),
            )
            StatsHeroInsetTile(
                label = netLabel,
                amountText = netValueText,
                accentColor = netColor,
                containerAlpha = tileAlpha,
                icon = if (netCents >= 0L) {
                    Icons.AutoMirrored.Rounded.TrendingUp
                } else {
                    Icons.AutoMirrored.Rounded.TrendingDown
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatsHeroInsetTile(
    label: String,
    amountText: String,
    accentColor: Color,
    containerAlpha: Float,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner16,
        color = accentColor.copy(alpha = containerAlpha),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = amountText,
                style = MaterialTheme.typography.labelLarge.tabularNums,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(AppTheme.shapes.capsule)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = AppTheme.shapes.capsule,
        color = if (selected) AppTheme.colors.sand else AppTheme.colors.surfaceSunken,
        contentColor = if (selected) AppTheme.colors.onSand else AppTheme.colors.textSecondary,
        border = if (selected) BorderStroke(0.5.dp, AppTheme.colors.hairline) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Thẻ Cơ cấu danh mục: gộp Donut gọn + danh sách Accordion ngay bên dưới. */
@Composable
private fun StatsCategoryStructureCard(
    metric: StatsMetric,
    breakdown: List<CategoryBreakdownItem>,
    showAmounts: Boolean,
    activeSelectedCategory: Category?,
    selectedTransactions: List<Transaction>,
    onCategorySelected: (Category?) -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val sortedBreakdown = remember(breakdown) {
        breakdown.sortedByDescending { it.amount.amountInCents }
    }

    Card(
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = AppTheme.shapes.corner24, elevated = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(
                    if (isExpense) R.string.stats_tab_expense_structure else R.string.stats_tab_income_structure,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            if (sortedBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner16)
                        .background(AppTheme.colors.surfaceSunken)
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stats_no_transactions_month),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                AllocationChartContent(
                    breakdown = sortedBreakdown,
                    showAmounts = showAmounts,
                    selectedCategory = activeSelectedCategory,
                    onCategorySelected = onCategorySelected,
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sortedBreakdown.forEach { item ->
                        key(item.category.id) {
                            val isSelected = item.category == activeSelectedCategory
                            CategoryBreakdownAccordionCard(
                                item = item,
                                isSelected = isSelected,
                                showAmounts = showAmounts,
                                transactions = if (isSelected) selectedTransactions else emptyList(),
                                onToggle = {
                                    onCategorySelected(if (isSelected) null else item.category)
                                },
                                onTransactionClick = onTransactionClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Thẻ Diễn biến theo thời gian tách riêng dưới thẻ Cơ cấu danh mục. */
@Composable
private fun StatsTimeBarCard(
    state: StatsUiState,
    metric: StatsMetric,
    activeSelectedCategory: Category?,
    showAmounts: Boolean,
) {
    Card(
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = AppTheme.shapes.corner24, elevated = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            PeriodSpendingBarChart(
                bars = if (metric == StatsMetric.CHI_TIEU) {
                    state.periodExpenseBars
                } else {
                    state.periodIncomeBars
                },
                metric = metric,
                periodRange = state.currentPeriod?.range,
                selectedCategory = activeSelectedCategory,
                showAmounts = showAmounts,
            )
        }
    }
}
