package com.notepay.feature.stats.main

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.bar.GradientTopAppBar
import com.notepay.core.designsystem.component.button.LiquidButton
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.ForecastConfidence
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.StatsAdviceType
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.stats.main.component.StatsDashboard
import com.notepay.feature.stats.main.component.StatsHeader
import com.notepay.feature.stats.main.component.StatsMetric
import com.notepay.feature.stats.main.component.StatsViewType
import com.notepay.feature.subscription.main.AddSubscriptionDialogState
import com.notepay.feature.subscription.main.sheet.AddSubscriptionBottomSheet
import com.notepay.feature.transaction.component.TransactionItem
import java.util.Locale
import kotlin.math.abs

private const val INITIAL_VISIBLE_TRANSACTIONS = 5
private const val PAGE_VISIBLE_TRANSACTIONS = 10

/**
 * Chuẩn hóa số liệu thẳng hàng dọc (Tabular figures).
 */
private val TextStyle.tabularNums: TextStyle
    get() = this.copy(fontFeatureSettings = "tnum")

/** Che số tiền theo nút eye trên TopAppBar; một nguồn che với TransactionItem. */
private fun Money.formatOrMask(showAmounts: Boolean): String =
    MoneyFormatter.format(this, showAmounts)

@Composable
internal fun StatsUiText.resolve(): String = when (this) {
    is StatsUiText.Plain -> value
    is StatsUiText.Resource -> stringResource(resId, *args.toTypedArray())
    is StatsUiText.Composite -> buildString {
        // append trực tiếp từng phần tử, không tạo danh sách trung gian: resolve() chạy trong
        // body Composable nên mỗi lần recompose đều đi qua đây.
        parts.forEach { append(it.resolve()) }
    }
}

private enum class StatsContentState {
    LOADING,
    EMPTY,
    CONTENT,
}

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onConfigureLocalModel: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addSubFormState by viewModel.addSubForm.collectAsStateWithLifecycle()
    var showAmounts by rememberSaveable { mutableStateOf(true) }
    var viewType by rememberSaveable { mutableStateOf(StatsViewType.XU_HUONG) }

    val contentState = when {
        state.isLoading -> StatsContentState.LOADING
        !state.hasAnyTransactions -> StatsContentState.EMPTY
        else -> StatsContentState.CONTENT
    }

    LaunchedEffect(state.currentPeriod?.range) {
        if (state.currentPeriod?.range != StatsRange.MONTH) {
            viewType = StatsViewType.PHAN_BO
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        GradientTopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.stats_screen_title),
                    fontWeight = FontWeight.ExtraBold,
                )
            },
            actions = {
                IconButton(onClick = { showAmounts = !showAmounts }) {
                    Icon(
                        imageVector = if (showAmounts) {
                            Icons.Rounded.Visibility
                        } else {
                            Icons.Rounded.VisibilityOff
                        },
                        contentDescription = stringResource(
                            if (showAmounts) R.string.stats_cd_hide_amounts else R.string.stats_cd_show_amounts
                        ),
                    )
                }
                if (state.currentPeriod?.range == StatsRange.MONTH) {
                    StatsHeader(
                        viewType = viewType,
                        onViewTypeChanged = { viewType = it },
                    )
                }
            },
        )

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(140))
            },
            label = "stats-content",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { target ->
            when (target) {
                StatsContentState.LOADING -> StatsLoadingState()
                StatsContentState.EMPTY -> StatsEmptyState(onAddTransaction = onAddTransaction)
                StatsContentState.CONTENT -> {
                    StatsDashboard(
                        state = state,
                        showAmounts = showAmounts,
                        viewType = viewType,
                        onPreviousMonth = viewModel::previousPeriod,
                        onNextMonth = viewModel::nextPeriod,
                        onSelectRange = viewModel::selectRange,
                        onMonthSelected = { point ->
                            viewModel.selectMonth(point.year, point.month)
                        },
                        onCategorySelected = viewModel::selectCategory,
                        onTrendWindowSelected = viewModel::selectTrendWindow,
                        supportingContent = { metric ->
                            StatsSupportingContent(
                                state = state,
                                metric = metric,
                                showAmounts = showAmounts,
                                onCategorySelected = viewModel::selectCategory,
                                onAdviceFeedback = viewModel::sendAdviceFeedback,
                                onAddSubscription = viewModel::showAddSubscription,
                                onGenerateLocalAdvice = viewModel::generateLocalAdvice,
                                onSelectLocalModel = onConfigureLocalModel,
                                onTransactionClick = onTransactionClick,
                            )
                        },
                    )
                }
            }
        }
    }

    if (addSubFormState.isVisible) {
        AddSubscriptionBottomSheet(
            state = AddSubscriptionDialogState(
                name = addSubFormState.name,
                amountInput = addSubFormState.amountInput,
                repeatMonths = addSubFormState.repeatMonths,
                remindDaysBefore = addSubFormState.remindDaysBefore,
                note = addSubFormState.note,
                category = addSubFormState.category,
                nextDueEpochMs = addSubFormState.nextDueEpochMs,
            ),
            onNameChanged = viewModel::updateSubFormName,
            onAmountChanged = viewModel::updateSubFormAmount,
            onRepeatMonthsChanged = viewModel::updateSubFormRepeatMonths,
            onRemindDaysChanged = viewModel::updateSubFormRemindDays,
            onNoteChanged = viewModel::updateSubFormNote,
            onCategoryChanged = viewModel::updateSubFormCategory,
            onNextDueDateChanged = viewModel::updateSubFormNextDueDate,
            onConfirm = viewModel::saveSubscription,
            onDismiss = viewModel::dismissSubForm,
        )
    }
}

@Composable
private fun StatsLoadingState() {
    val loadingContentDescription = stringResource(R.string.stats_loading_cd)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.semantics {
                contentDescription = loadingContentDescription
            },
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }
            Text(
                text = stringResource(R.string.stats_loading_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.stats_loading_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatsEmptyState(
    onAddTransaction: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 360.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = AppTheme.shapes.corner24,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.stats_empty_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.stats_empty_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAddTransaction,
                modifier = Modifier
                    .widthIn(min = 200.dp, max = 280.dp)
                    .heightIn(min = 48.dp),
                shape = AppTheme.shapes.corner16,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.add_transaction_title),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StatsSupportingContent(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    onCategorySelected: (Category?) -> Unit,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (DetectedSubscription) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val breakdown = if (isExpense) state.breakdown else state.incomeBreakdown

    // Kiểm tra xem selectedCategory có thực sự thuộc tab hiện tại hay không
    val activeSelectedCategory = remember(breakdown, state.selectedCategory) {
        state.selectedCategory?.takeIf { selected ->
            breakdown.any { it.category == selected }
        }
    }

    val selectedTransactions = remember(
        state.transactions,
        activeSelectedCategory,
        isExpense,
    ) {
        if (activeSelectedCategory == null) return@remember emptyList()
        val expectedType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
        state.transactions.filter { tx ->
            tx.type == expectedType && tx.category == activeSelectedCategory
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (isExpense && state.budgetLimit != null) {
            BudgetProgressBar(
                spent = state.budgetSpent,
                limit = state.budgetLimit,
                percentage = state.budgetPercentage,
                showAmounts = showAmounts,
                dailyBudget = state.dynamicDailyBudget,
            )
        }

        if (isExpense) {
            state.spendingForecast?.prediction?.let {
                SpendingPredictionCard(
                    prediction = it,
                    showAmounts = showAmounts,
                )
            }

            val hasInsights = state.spendingForecast != null ||
                    state.aiAdvices.isNotEmpty() ||
                    state.detectedSubscriptions.isNotEmpty()

            if (hasInsights) {
                AiInsightsCarousel(
                    state = state,
                    showAmounts = showAmounts,
                    onAdviceFeedback = onAdviceFeedback,
                    onAddSubscription = onAddSubscription,
                    onGenerateLocalAdvice = onGenerateLocalAdvice,
                    onSelectLocalModel = onSelectLocalModel,
                )
            }
        }

        Text(
            text = stringResource(
                if (isExpense) {
                    R.string.stats_expense_category_detail
                } else {
                    R.string.stats_income_category_detail
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        if (breakdown.isEmpty()) {
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                breakdown.forEach { item ->
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

@Composable
private fun SectionHeading(
    title: String,
    description: String,
) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Gop CategoryRow va danh sach Transaction con vao chung 1 Unified Accordion Card,
 * giup phan cap thi giac ro rang (Gestalt Common Region) va gioi han so luong render.
 */
@Composable
private fun CategoryBreakdownAccordionCard(
    item: CategoryBreakdownItem,
    isSelected: Boolean,
    showAmounts: Boolean,
    transactions: List<Transaction>,
    onToggle: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryColor = Color(item.category.colorArgb)
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "category-card-bg",
    )

    val categoryName = item.category.localizedName()
    val amountLabel = item.amount.formatOrMask(showAmounts)
    val percentageLabel = stringResource(
        R.string.stats_category_percentage_cd,
        String.format(Locale.US, "%.1f", item.percentage * 100f),
    )
    // Mô tả cho screen reader lấy nguyên văn từ resource (dấu phân cách nằm trong chuỗi để
    // bản địa hoá đúng), không nối chuỗi thủ công bằng ", ".
    val rowDescription = if (isSelected) {
        stringResource(
            R.string.stats_category_row_open_cd,
            categoryName,
            amountLabel,
            percentageLabel,
            stringResource(R.string.stats_category_selected_cd),
        )
    } else {
        stringResource(R.string.stats_category_row_cd, categoryName, amountLabel, percentageLabel)
    }

    var visibleCount by remember(item.category.id, isSelected) {
        mutableIntStateOf(INITIAL_VISIBLE_TRANSACTIONS)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                categoryColor
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            },
        ),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header của Accordion (chính là Row danh mục)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(16.dp)
                    .semantics {
                        contentDescription = rowDescription
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryAvatar(
                    category = item.category,
                    size = 40.dp,
                    iconSize = 18.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.amount.formatOrMask(showAmounts),
                            style = MaterialTheme.typography.titleMedium.tabularNums,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { item.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = categoryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )

                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f%%",
                                item.percentage * 100f,
                            ),
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 48.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Nội dung mở rộng bên trong cùng Card
            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Text(
                        text = stringResource(
                            R.string.stats_transaction_history_format,
                            categoryName,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    if (transactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        val displayedTransactions = remember(transactions, visibleCount) {
                            transactions.take(visibleCount)
                        }
                        displayedTransactions.forEach { transaction ->
                            key(transaction.id) {
                                TransactionItem(
                                    transaction = transaction,
                                    // Hàng con cũng phải obey nút ẩn số tiền, nếu không mở
                                    // accordion ra là số liệu rò rỉ lại toàn bộ.
                                    showAmounts = showAmounts,
                                    onClick = { onTransactionClick(transaction.id) },
                                )
                            }
                        }

                        if (transactions.size > visibleCount) {
                            val remaining = transactions.size - visibleCount
                            TextButton(
                                onClick = { visibleCount += PAGE_VISIBLE_TRANSACTIONS },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.stats_show_more_transactions, remaining),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(
    spent: Money,
    limit: Money,
    percentage: Float,
    showAmounts: Boolean,
    dailyBudget: DynamicDailyBudgetData?,
) {
    val safePercentage = if (percentage.isFinite()) percentage.coerceAtLeast(0f) else 0f
    val visualProgress = safePercentage.coerceIn(0f, 1f)
    val remainingInCents = limit.amountInCents - spent.amountInCents

    val statusColor = when {
        safePercentage >= 1f -> MaterialTheme.colorScheme.error
        safePercentage >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        remainingInCents < 0L ->
            stringResource(
                R.string.stats_budget_exceeded_format,
                Money(abs(remainingInCents)).formatOrMask(showAmounts),
            )
        safePercentage >= 0.8f ->
            stringResource(
                R.string.stats_budget_remaining_format,
                Money(remainingInCents.coerceAtLeast(0L)).formatOrMask(showAmounts),
            )
        else -> stringResource(R.string.stats_budget_in_limit)
    }

    val budgetUsedContentDescription = stringResource(
        R.string.stats_budget_used_cd,
        (safePercentage * 100).toInt(),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (safePercentage >= 0.8f) {
                                Icons.Rounded.Warning
                            } else {
                                Icons.Rounded.Wallet
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wallet_field_budget),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f%%", safePercentage * 100f),
                        style = MaterialTheme.typography.labelLarge.tabularNums,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { visualProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = budgetUsedContentDescription
                    },
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.stats_spent_format,
                        spent.formatOrMask(showAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.stats_limit_format,
                        limit.formatOrMask(showAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Hạn mức ngày gộp thành 1 chip footer gọn thay vì thẻ progress riêng trên Carousel,
            // tránh trùng lặp thị giác với thanh ngân sách tháng ở trên.
            if (dailyBudget != null) {
                val dailyAccent = if (dailyBudget.isExceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = dailyAccent.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = dailyAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.stats_daily_limit_format,
                                dailyBudget.dailyBudget.formatOrMask(showAmounts),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (dailyBudget.isExceeded) {
                                stringResource(
                                    R.string.stats_daily_spent_format,
                                    dailyBudget.spentToday.formatOrMask(showAmounts),
                                )
                            } else {
                                stringResource(
                                    R.string.stats_daily_remaining_format,
                                    dailyBudget.remainingToday.formatOrMask(showAmounts),
                                )
                            },
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            fontWeight = FontWeight.Bold,
                            color = dailyAccent,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingPredictionCard(
    prediction: SpendingPrediction,
    showAmounts: Boolean,
) {
    val probability = prediction.overBudgetProbability

    val confidenceLabel = when (prediction.confidence) {
        ForecastConfidence.LOW -> stringResource(R.string.stats_confidence_low)
        ForecastConfidence.MEDIUM -> stringResource(R.string.stats_confidence_medium)
        ForecastConfidence.HIGH -> stringResource(R.string.stats_confidence_high)
    }

    val riskContainerColor = when {
        probability == null -> MaterialTheme.colorScheme.secondaryContainer
        probability >= 0.70 -> MaterialTheme.colorScheme.errorContainer
        probability >= 0.35 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val riskOnContainerColor = when {
        probability == null -> MaterialTheme.colorScheme.onSecondaryContainer
        probability >= 0.70 -> MaterialTheme.colorScheme.onErrorContainer
        probability >= 0.35 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = riskContainerColor,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            tint = riskOnContainerColor,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stats_forecast_end_month),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.stats_processed_on_device),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = riskContainerColor,
                ) {
                    Text(
                        text = stringResource(R.string.stats_confidence_format, confidenceLabel),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = riskOnContainerColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }

            Text(
                text = Money(prediction.predictedMonthTotalInCents).formatOrMask(showAmounts),
                style = MaterialTheme.typography.headlineSmall.tabularNums,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = stringResource(
                    R.string.stats_expected_range_format,
                    Money(prediction.lowerBoundInCents).formatOrMask(showAmounts),
                    Money(prediction.upperBoundInCents).formatOrMask(showAmounts),
                ),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            )

            Text(
                text = probability?.let {
                    stringResource(
                        R.string.stats_probability_format,
                        (it * 100).toInt(),
                        prediction.observedDays,
                    )
                } ?: stringResource(
                    R.string.stats_probability_no_limit_format,
                    prediction.observedDays,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AiInsightsCarousel(
    state: StatsUiState,
    showAmounts: Boolean,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (DetectedSubscription) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
) {
    val localView = LocalView.current
    val configuration = LocalConfiguration.current
    // Tinh cardWidth truc tiep tu screenWidthDp, tranh dung BoxWithConstraints gay Subcomposition
    val cardWidth = remember(configuration.screenWidthDp) {
        (configuration.screenWidthDp.dp - 48.dp).coerceIn(272.dp, 328.dp)
    }

    // Chi dem LocalAdvisor la 1 insight khi da READY (co loi khuyen thuc su)
    val itemCount = (if (state.localAdvisor.status == LocalAdvisorStatus.READY) 1 else 0) +
            (if (state.dynamicDailyBudget != null) 1 else 0) +
            state.detectedSubscriptions.size +
            state.aiAdvices.size

    val playHaptic = {
        try {
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeading(
            title = stringResource(R.string.stats_section_advice),
            description = stringResource(R.string.stats_advice_count_format, itemCount.coerceAtLeast(1)),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "local-advisor") {
                LocalAdvisorCard(
                    advisor = state.localAdvisor,
                    onGenerate = onGenerateLocalAdvice,
                    onSelectModel = onSelectLocalModel,
                    modifier = Modifier.width(cardWidth),
                )
            }

            items(
                items = state.detectedSubscriptions,
                key = { sub -> "sub-${sub.name}-${sub.amount.amountInCents}" },
            ) { subscription ->
                SubscriptionProposalCard(
                    sub = subscription,
                    showAmounts = showAmounts,
                    onAdd = {
                        playHaptic()
                        onAddSubscription(subscription)
                    },
                    modifier = Modifier.width(cardWidth),
                )
            }

            items(
                items = state.aiAdvices,
                key = { advice -> "advice-${advice.id}" },
            ) { advice ->
                AiAdviceCard(
                    advice = advice,
                    onFeedback = { score ->
                        playHaptic()
                        onAdviceFeedback(advice.id, score)
                    },
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@Composable
private fun LocalAdvisorCard(
    advisor: LocalAdvisorUiState,
    onGenerate: () -> Unit,
    onSelectModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = advisor.result
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val idleDescription = when (advisor.availability) {
        AdvisorAvailability.GEMINI_NANO ->
            stringResource(R.string.stats_advisor_gemini_description)
        AdvisorAvailability.CLOUD_GEMINI ->
            stringResource(R.string.stats_advisor_cloud_ready_format, "Gemini 2.0 Flash")
        AdvisorAvailability.STATISTICAL_ONLY ->
            stringResource(R.string.stats_advisor_statistical_description)
        AdvisorAvailability.CHECKING ->
            stringResource(R.string.stats_advisor_checking_description)
    }

    InsightCard(
        modifier = modifier.clickable(
            enabled = advisor.status == LocalAdvisorStatus.READY,
            onClick = { isExpanded = !isExpanded },
        ),
    ) {
        // 1. Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result?.title ?: stringResource(R.string.stats_advisor_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.stats_advisor_privacy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 2. Body + 3. Footer (đẩy sát đáy bằng Spacer(weight(1f)))
        when (advisor.status) {
            LocalAdvisorStatus.RUNNING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text = stringResource(R.string.stats_analyzing),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            LocalAdvisorStatus.NOT_REQUESTED -> {
                Text(
                    text = idleDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                // Chi 1 Primary CTA duy nhat (LiquidButton), nut Settings la Secondary IconButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidButton(
                        onClick = onGenerate,
                        enabled = advisor.availability != AdvisorAvailability.CHECKING,
                        modifier = Modifier.weight(1f),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.stats_action_analyze),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (advisor.availability != AdvisorAvailability.GEMINI_NANO) {
                        FilledTonalIconButton(
                            onClick = onSelectModel,
                            modifier = Modifier.size(48.dp),
                            shape = AppTheme.shapes.corner12,
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.stats_action_ai_settings),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            LocalAdvisorStatus.READY -> {
                Text(
                    text = cleanAiMarkdown(result?.content.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = !isExpanded),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (result?.provider) {
                                AdvisorProvider.GEMINI_NANO -> stringResource(R.string.stats_provider_gemini)
                                AdvisorProvider.CLOUD_GEMINI -> stringResource(R.string.stats_provider_cloud)
                                AdvisorProvider.STATISTICAL_FALLBACK, null -> stringResource(R.string.stats_provider_statistics)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        result?.providerMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (result?.provider != AdvisorProvider.GEMINI_NANO) {
                        IconButton(onClick = onSelectModel) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.stats_cd_open_ai_settings),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    TextButton(
                        onClick = onGenerate,
                        modifier = Modifier.heightIn(min = 40.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.stats_refresh))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAdviceCard(
    advice: AiAdviceItem,
    onFeedback: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(advice.id) { mutableStateOf(false) }
    // type là enum domain nên when tất định; nhánh "info" trước đây là code chết vì
    // StatsAdviceType chỉ có WARNING/SUCCESS.
    val accentColor = when (advice.type) {
        StatsAdviceType.WARNING -> MaterialTheme.colorScheme.error
        StatsAdviceType.SUCCESS -> MaterialTheme.colorScheme.primary
    }

    InsightCard(
        modifier = modifier.clickable { isExpanded = !isExpanded },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (advice.type == StatsAdviceType.WARNING) {
                    Icons.Rounded.Warning
                } else {
                    Icons.Rounded.Info
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = advice.title.resolve(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = cleanAiMarkdown(advice.content.resolve()),
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = !isExpanded),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (advice.feedback == 1 || advice.feedback == -1) {
                    stringResource(R.string.stats_feedback_thanks)
                } else {
                    stringResource(R.string.stats_feedback_question)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = { onFeedback(1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ThumbUp,
                    contentDescription = stringResource(R.string.stats_feedback_helpful),
                    tint = if (advice.feedback == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = { onFeedback(-1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ThumbDown,
                    contentDescription = stringResource(R.string.stats_feedback_not_helpful),
                    tint = if (advice.feedback == -1) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionProposalCard(
    sub: DetectedSubscription,
    showAmounts: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InsightCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_subscription_expense),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.stats_subscription_detected_history),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = sub.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.stats_subscription_monthly_format,
                    sub.amount.formatOrMask(showAmounts),
                ),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FilledTonalButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = AppTheme.shapes.corner12,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.stats_add_to_subscriptions),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.heightIn(min = 220.dp),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private val AI_MARKDOWN_BOLD_REGEX = Regex("\\*\\*")
private val AI_MARKDOWN_BULLET_REGEX = Regex("(?m)^\\s*\\*\\s+")

private fun cleanAiMarkdown(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return text
        .replace(AI_MARKDOWN_BOLD_REGEX, "")
        .replace(AI_MARKDOWN_BULLET_REGEX, "• ")
        .trim()
}