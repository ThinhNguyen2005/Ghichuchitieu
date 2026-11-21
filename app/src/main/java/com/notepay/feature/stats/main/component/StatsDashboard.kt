package com.notepay.feature.stats.main.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
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
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
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
    onOpenWalletPicker: () -> Unit,
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
    val bottomClearance = if (windowLayout.usesNavigationRail) 24.dp else maxOf(
        120.dp,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

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
                    onOpenWalletPicker = onOpenWalletPicker,
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
                onOpenWalletPicker = onOpenWalletPicker,
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
    onOpenWalletPicker: () -> Unit,
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
            onOpenWalletPicker = onOpenWalletPicker,
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
}

/** Thanh chọn ví + chọn kỳ trung tính theo `filter-chip` v4 và bộ lùi/tiến kỳ gọn gàng. */
@Composable
private fun StatsPeriodControlBar(
    state: StatsUiState,
    onOpenWalletPicker: () -> Unit,
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.wallets.isNotEmpty()) {
            val walletLabel = state.selectedWallet?.name ?: stringResource(R.string.home_all_wallets)
            val selectWalletLabel = stringResource(R.string.home_select_wallet)
            AssistChip(
                onClick = onOpenWalletPicker,
                label = {
                    Text(
                        text = walletLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                shape = AppTheme.shapes.corner12,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = AppTheme.colors.surface.copy(alpha = 0.72f),
                    labelColor = AppTheme.colors.textPrimary,
                    leadingIconContentColor = AppTheme.colors.textSecondary,
                    trailingIconContentColor = AppTheme.colors.textSecondary,
                ),
                border = BorderStroke(1.dp, AppTheme.colors.hairline),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) {
                        onClick(label = selectWalletLabel, action = null)
                    },
            )
        }
        Surface(
            shape = AppTheme.shapes.capsule,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
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
                        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                        border = if (isSelected) BorderStroke(1.dp, AppTheme.colors.border) else null,
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
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        if (hasArrows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronLeft,
                        contentDescription = stringResource(R.string.stats_previous_month),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (state.isCurrentMonth) {
                            stringResource(R.string.stats_current_month)
                        } else {
                            state.dateRangeLabel.resolve()
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onNextMonth,
                    enabled = !state.isLatestPeriod,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.stats_next_month),
                        tint = if (state.isLatestPeriod) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

/** Thẻ Ink tâm điểm duy nhất của màn Báo cáo theo `DESIGN.md` §6 (`Sand & Ink`). */
@Composable
private fun StatsInkHeroCard(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    onMetricChanged: (StatsMetric) -> Unit,
) {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val total = if (isExpense) state.totalExpense else state.totalIncome
    val counterpartTotal = if (isExpense) state.totalIncome else state.totalExpense
    val previousTotal = if (isExpense) state.previousExpense else state.previousIncome
    val difference = if (previousTotal != null) total.amountInCents - previousTotal.amountInCents else null
    val reducedMotion = rememberNotePayReducedMotion()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        color = AppTheme.colors.ink,
        contentColor = AppTheme.colors.onInk,
        border = if (!AppTheme.colors.isLight) BorderStroke(1.dp, AppTheme.colors.inkBorder) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Chuyển đổi Chi tiêu / Thu nhập: duy nhất mục đang chọn mang màu Sand.
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(
                        if (isExpense) R.string.stats_total_expense else R.string.stats_total_income,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.onInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                AnimatedContent(
                    targetState = total to showAmounts,
                    transitionSpec = {
                        if (reducedMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                        }
                    },
                    label = "StatsHeroAmount",
                ) { (targetAmount, visible) ->
                    Text(
                        text = MoneyFormatter.format(targetAmount, visible),
                        style = AppTheme.typography.amountHero,
                        color = AppTheme.colors.onInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            val counterpartColor = if (isExpense) AppTheme.colors.onInkIncome else AppTheme.colors.onInkExpense
            val counterpartIcon = if (isExpense) {
                Icons.AutoMirrored.Rounded.TrendingUp
            } else {
                Icons.AutoMirrored.Rounded.TrendingDown
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {},
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = counterpartIcon,
                    contentDescription = null,
                    tint = counterpartColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = stringResource(
                        if (isExpense) R.string.stats_income else R.string.stats_expense,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.onInkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = MoneyFormatter.format(counterpartTotal, showAmounts),
                    style = MaterialTheme.typography.labelLarge.tabularNums,
                    fontWeight = FontWeight.SemiBold,
                    color = counterpartColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (difference != null && difference != 0L && showAmounts) {
                val isDecrease = difference < 0L
                val isGood = if (isExpense) isDecrease else !isDecrease
                val badgeColor = if (isGood) AppTheme.colors.onInkIncome else AppTheme.colors.onInkExpense
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
                        .clip(AppTheme.shapes.corner12)
                        .background(badgeColor.copy(alpha = 0.16f))
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
        color = if (selected) AppTheme.colors.sand else AppTheme.colors.inkRaised,
        contentColor = if (selected) AppTheme.colors.onSand else AppTheme.colors.onInkMuted,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
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
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (sortedBreakdown.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner16)
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stats_no_transactions_month),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
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
