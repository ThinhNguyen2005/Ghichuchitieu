package com.notepay.feature.stats.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.bar.GradientTopAppBar
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.analytics.StatsRange
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.component.AiInsightsCarousel
import com.notepay.feature.stats.main.component.BudgetProgressBar
import com.notepay.feature.stats.main.component.CategoryBreakdownAccordionCard
import com.notepay.feature.stats.main.component.SpendingPredictionCard
import com.notepay.feature.stats.main.component.StatsDashboard
import com.notepay.feature.stats.main.component.StatsMetric
import com.notepay.feature.stats.main.component.StatsViewType
import com.notepay.feature.subscription.main.AddSubscriptionDialogState
import com.notepay.feature.subscription.main.sheet.AddSubscriptionBottomSheet

/**
 * Chuẩn hóa số liệu thẳng hàng dọc (Tabular figures).
 */
internal val TextStyle.tabularNums: TextStyle
    get() = this.copy(fontFeatureSettings = "tnum")

/** Che số tiền theo nút eye trên TopAppBar; một nguồn che với TransactionItem. */
internal fun Money.formatOrMask(showAmounts: Boolean): String =
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
    @Suppress("UNUSED_PARAMETER") onAddTransaction: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onConfigureLocalModel: () -> Unit = {},
    onConfigureBudget: (Long?) -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addSubFormState by viewModel.addSubForm.collectAsStateWithLifecycle()
    var showAmounts by rememberSaveable { mutableStateOf(true) }
    var viewType by rememberSaveable { mutableStateOf(StatsViewType.XU_HUONG) }
    val reducedMotion = rememberNotePayReducedMotion()

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
            },
        )

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(140))
                }
            },
            label = "stats-content",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { target ->
            when (target) {
                StatsContentState.LOADING -> StatsLoadingState()
                StatsContentState.EMPTY -> StatsEmptyState()
                StatsContentState.CONTENT -> {
                    StatsDashboard(
                        state = state,
                        showAmounts = showAmounts,
                        viewType = viewType,
                        onViewTypeChanged = { viewType = it },
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
                                onConfigureBudget = { onConfigureBudget(state.selectedWallet?.id) },
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
private fun StatsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        EmptyStateWithAction(
            icon = Icons.Outlined.PieChart,
            iconTint = MaterialTheme.colorScheme.primary,
            iconBackground = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            title = stringResource(R.string.stats_empty_title),
            description = stringResource(R.string.stats_empty_description),
        )
    }
}

@Composable
private fun StatsSupportingContent(
    state: StatsUiState,
    metric: StatsMetric,
    showAmounts: Boolean,
    onCategorySelected: (Category?) -> Unit,
    onConfigureBudget: () -> Unit,
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
                    onConfigureBudget = onConfigureBudget,
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
internal fun SectionHeading(
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
