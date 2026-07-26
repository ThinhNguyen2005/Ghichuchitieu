package com.notepay.ui.feature.stats

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.ForecastConfidence
import com.notepay.ai.LocalModelInstallStatus
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.component.LiquidGlassPanel
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.feature.subscription.AddSubscriptionBottomSheet
import com.notepay.ui.feature.subscription.AddSubscriptionDialogState
import com.notepay.ui.util.MoneyFormatter
import java.util.Locale
import kotlin.math.abs

private enum class StatsContentState {
    LOADING,
    EMPTY,
    CONTENT,
}

@OptIn(ExperimentalMaterial3Api::class)
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

    val contentState = when {
        state.isLoading -> StatsContentState.LOADING
        !state.hasAnyTransactions -> StatsContentState.EMPTY
        else -> StatsContentState.CONTENT
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
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
                            imageVector = if (showAmounts) Icons.Rounded.RemoveRedEye else Icons.Rounded.VisibilityOff,
                            contentDescription = stringResource(
                                if (showAmounts) R.string.stats_cd_hide_amounts else R.string.stats_cd_show_amounts,
                            ),
                        )
                    }
                },
            )
        },
    ) { padding ->
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                fadeIn(animationSpec = tween(180)) togetherWith
                        fadeOut(animationSpec = tween(140))
            },
            label = "stats-content",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding),
        ) { target ->
            when (target) {
                StatsContentState.LOADING -> StatsLoadingState()
                StatsContentState.EMPTY -> StatsEmptyState(onAddTransaction = onAddTransaction)
                StatsContentState.CONTENT -> {
                    StatsDashboard(
                        state = state,
                        showAmounts = showAmounts,
                        onPreviousMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onMonthSelected = { point ->
                            viewModel.selectMonth(point.year, point.month)
                        },
                        onCategorySelected = viewModel::selectCategory,
                        supportingContent = { metric ->
                            StatsSupportingContent(
                                state = state,
                                metric = metric,
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.semantics {
                contentDescription = "Đang tổng hợp dữ liệu thống kê"
            },
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }
            Text(
                text = "Đang tổng hợp số liệu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Phân tích giao dịch và xu hướng tài chính của bạn…",
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
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.size(88.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Chưa có dữ liệu thống kê",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Thêm giao dịch đầu tiên để xem phân bổ, xu hướng và các gợi ý tài chính.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onAddTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_transaction_title), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StatsSupportingContent(
    state: StatsUiState,
    metric: StatsMetric,
    onCategorySelected: (Category?) -> Unit,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
    onTransactionClick: (Long) -> Unit,
) {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val breakdown = if (isExpense) state.breakdown else state.incomeBreakdown
    val selectedTransactions = remember(
        state.transactions,
        state.selectedCategory,
        metric,
    ) {
        val expectedType = if (isExpense) {
            com.notepay.domain.model.TransactionType.EXPENSE
        } else {
            com.notepay.domain.model.TransactionType.INCOME
        }

        state.transactions.filter { transaction ->
            transaction.type == expectedType &&
                    transaction.category == state.selectedCategory
        }
    }

    // Avoid carrying an expense category selection into the income tab, and vice versa.
    LaunchedEffect(metric) {
        onCategorySelected(null)
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
            )
        }

        if (isExpense) {
            state.spendingForecast?.prediction?.let {
                SpendingPredictionCard(prediction = it)
            }

            val hasInsights = state.spendingForecast != null ||
                    state.dynamicDailyBudget != null ||
                    state.aiAdvices.isNotEmpty() ||
                    state.detectedSubscriptions.isNotEmpty()

            if (hasInsights) {
                AiInsightsCarousel(
                    state = state,
                    onAdviceFeedback = onAdviceFeedback,
                    onAddSubscription = onAddSubscription,
                    onGenerateLocalAdvice = onGenerateLocalAdvice,
                    onSelectLocalModel = onSelectLocalModel,
                )
            }
        }

        Text(
            text = stringResource(if (isExpense) R.string.stats_expense_category_detail else R.string.stats_income_category_detail),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )

        if (breakdown.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
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
        }

        breakdown.forEach { item ->
            val isSelected = item.category == state.selectedCategory

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryBreakdownRow(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        onCategorySelected(if (isSelected) null else item.category)
                    },
                )

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(animationSpec = tween(180)),
                ) {
                    Text(
                        text = stringResource(R.string.stats_transaction_history_format, item.category.displayName),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                    )

                    if (selectedTransactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        selectedTransactions.forEach { transaction ->
                            TransactionItem(
                                transaction = transaction,
                                onClick = { onTransactionClick(transaction.id) },
                            )
                        }
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
        verticalArrangement = Arrangement.spacedBy(3.dp),
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

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = buildString {
                    append(item.category.displayName)
                    append(", ")
                    append(MoneyFormatter.format(item.amount))
                    append(", ")
                    append(
                        String.format(
                            Locale.US,
                            "%.1f phần trăm",
                            item.percentage * 100f,
                        ),
                    )
                    if (isSelected) append(", đang mở")
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSystemInDarkTheme()) {
                Color.White
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        border = if (isSelected) BorderStroke(1.5.dp, Color(item.category.colorArgb)) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = MoneyFormatter.format(item.amount),
                        style = MaterialTheme.typography.titleMedium,
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
                            .height(6.dp),
                        color = Color(item.category.colorArgb),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )

                    Text(
                        text = String.format(
                            Locale.US,
                            "%.1f%%",
                            item.percentage * 100f,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End,
                    )
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
) {
    val safePercentage = if (percentage.isFinite()) {
        percentage.coerceAtLeast(0f)
    } else {
        0f
    }
    val visualProgress = safePercentage.coerceIn(0f, 1f)
    val remainingInCents = limit.amountInCents - spent.amountInCents

    val statusColor = when {
        safePercentage >= 1f -> MaterialTheme.colorScheme.error
        safePercentage >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        remainingInCents < 0L ->
            "Đã vượt ${MoneyFormatter.format(Money(abs(remainingInCents)))}"
        safePercentage >= 0.8f ->
            "Còn ${MoneyFormatter.format(Money(remainingInCents.coerceAtLeast(0L)))}"
        else ->
            "Đang trong hạn mức"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSystemInDarkTheme()) {
                Color.White
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp),
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
                    shape = RoundedCornerShape(10.dp),
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.0f%%",
                            safePercentage * 100f,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { visualProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = "Đã dùng ${(safePercentage * 100).toInt()} phần trăm hạn mức"
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
                    text = "Đã chi ${MoneyFormatter.format(spent)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Hạn mức ${MoneyFormatter.format(limit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SpendingPredictionCard(
    prediction: com.notepay.domain.analytics.SpendingPrediction,
) {
    val probability = prediction.overBudgetProbability
    val riskColor = when {
        probability == null -> MaterialTheme.colorScheme.secondary
        probability >= 0.70 -> MaterialTheme.colorScheme.error
        probability >= 0.35 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val confidenceLabel = when (prediction.confidence) {
        ForecastConfidence.LOW -> "Thấp"
        ForecastConfidence.MEDIUM -> "Trung bình"
        ForecastConfidence.HIGH -> "Cao"
    }

    // Pair tinted risk surfaces with matching on-* colors for readable text.
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSystemInDarkTheme()) {
                Color.White
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        text = "Dự báo cuối tháng",
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
                            text = "Xử lý trên thiết bị",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = riskContainerColor,
                ) {
                    Text(
                        text = "Tin cậy $confidenceLabel",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = riskOnContainerColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(
                    Money(prediction.predictedMonthTotalInCents),
                ),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "Khoảng dự kiến: ${
                    MoneyFormatter.format(Money(prediction.lowerBoundInCents))
                } – ${
                    MoneyFormatter.format(Money(prediction.upperBoundInCents))
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
            )

            Text(
                text = probability?.let {
                    "Khả năng vượt hạn mức ${(it * 100).toInt()}% · Dựa trên ${prediction.observedDays} ngày dữ liệu"
                } ?: "Đặt hạn mức để xem xác suất vượt chi · Dựa trên ${prediction.observedDays} ngày dữ liệu",
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
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
) {
    val localView = LocalView.current
    val itemCount = 1 +
            (if (state.dynamicDailyBudget != null) 1 else 0) +
            state.detectedSubscriptions.size +
            state.aiAdvices.size

    val playHaptic = {
        try {
            localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {
            // Haptic feedback is optional.
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeading(
            title = "Phân tích thông minh",
            description = "$itemCount gợi ý · Vuốt ngang để xem thêm",
        )

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cardWidth = (maxWidth - 24.dp).coerceIn(268.dp, 324.dp)

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

                state.dynamicDailyBudget?.let { budget ->
                    item(key = "daily-budget") {
                        DynamicDailyBudgetCard(
                            budget = budget,
                            modifier = Modifier.width(cardWidth),
                        )
                    }
                }

                items(
                    items = state.detectedSubscriptions,
                ) { subscription ->
                    SubscriptionProposalCard(
                        sub = subscription,
                        onAdd = {
                            playHaptic()
                            onAddSubscription(
                                subscription.name,
                                subscription.amount.amountInCents,
                                subscription.category.id,
                                subscription.possibleNextDueDate,
                            )
                        },
                        modifier = Modifier.width(cardWidth),
                    )
                }

                items(
                    items = state.aiAdvices,
                    key = { advice -> advice.id },
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
}

@Composable
private fun LocalAdvisorCard(
    advisor: LocalAdvisorUiState,
    onGenerate: () -> Unit,
    onSelectModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = advisor.result
    val idleDescription = when (advisor.availability) {
        AdvisorAvailability.GEMINI_NANO ->
            "Gemini Nano diễn giải số liệu tổng hợp ngay trên thiết bị."
        AdvisorAvailability.LOCAL_MODEL ->
            "Mô hình ${advisor.localModel.displayName ?: "AI cục bộ"} đã sẵn sàng; dữ liệu không rời khỏi máy."
        AdvisorAvailability.STATISTICAL_ONLY ->
            "Gemini Nano không khả dụng. Bạn có thể thêm Gemma 3 1B hoặc mô hình .litertlm phù hợp với máy."
        AdvisorAvailability.CHECKING ->
            "Đang kiểm tra khả năng phân tích AI cục bộ trên thiết bị."
    }

    InsightCard(
        modifier = modifier,
        accentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
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

        if (advisor.localModel.status == LocalModelInstallStatus.IMPORTING) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Đang cài mô hình AI trên thiết bị…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                val progress = advisor.localModel.progress
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        } else when (advisor.status) {
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
                            text = "Đang phân tích dữ liệu…",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidButton(
                        onClick = onGenerate,
                        enabled = advisor.availability != AdvisorAvailability.CHECKING,
                        modifier = Modifier.weight(1.3f),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = stringResource(R.string.stats_action_analyze),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (advisor.availability != AdvisorAvailability.GEMINI_NANO) {
                        LiquidButton(
                            onClick = onSelectModel,
                            modifier = Modifier.weight(1f),
                            tint = MaterialTheme.colorScheme.secondary,
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = stringResource(R.string.stats_action_ai_settings),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                advisor.localModel.message
                    ?.takeIf { advisor.localModel.status == LocalModelInstallStatus.ERROR }
                    ?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
            }

            LocalAdvisorStatus.READY -> {
                Text(
                    text = cleanAiMarkdown(result?.content.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (result?.provider) {
                                AdvisorProvider.GEMINI_NANO -> "Gemini Nano"
                                AdvisorProvider.LOCAL_LITERT_MODEL -> "AI cục bộ · LiteRT-LM"
                                AdvisorProvider.STATISTICAL_FALLBACK, null -> "Thống kê cục bộ"
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
                                Icons.Rounded.FolderOpen,
                                contentDescription = "Mở cài đặt AI cục bộ",
                                modifier = Modifier.size(18.dp),
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
                        Text("Làm mới")
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicDailyBudgetCard(
    budget: DynamicDailyBudgetData,
    modifier: Modifier = Modifier,
) {
    val rawProgress = if (budget.dailyBudget.amountInCents > 0L) {
        budget.spentToday.amountInCents.toFloat() /
                budget.dailyBudget.amountInCents.toFloat()
    } else {
        1f
    }
    val progress = rawProgress.coerceIn(0f, 1f)
    val accentColor = if (budget.isExceeded) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    InsightCard(
        modifier = modifier,
        accentColor = accentColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                Icons.Rounded.Wallet,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ngân sách hôm nay",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (budget.isExceeded) {
                        "Đã vượt hạn mức"
                    } else {
                        "Tự điều chỉnh theo ngân sách tháng"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = if (budget.isExceeded) {
                    "Đã chi ${MoneyFormatter.format(budget.spentToday)}"
                } else {
                    "Còn ${MoneyFormatter.format(budget.remainingToday)}"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
            )
            Text(
                text = "Hạn mức hôm nay ${MoneyFormatter.format(budget.dailyBudget)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape),
            color = accentColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
        )

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (budget.isExceeded) {
                    Icons.Rounded.Warning
                } else {
                    Icons.Rounded.CheckCircle
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = if (budget.isExceeded) {
                    "Hạn mức ngày mai sẽ điều chỉnh còn ${MoneyFormatter.format(budget.tomorrowBudget)}."
                } else {
                    "Bạn đang giữ nhịp chi tiêu phù hợp với ngân sách tháng."
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AiAdviceCard(
    advice: AiAdviceItem,
    onFeedback: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = when (advice.type) {
        "warning" -> MaterialTheme.colorScheme.error
        "success" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    InsightCard(
        modifier = modifier,
        accentColor = accentColor,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                imageVector = if (advice.type == "warning") {
                    Icons.Rounded.Warning
                } else {
                    Icons.Rounded.Info
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = advice.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = cleanAiMarkdown(advice.content),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (advice.feedback == 1 || advice.feedback == -1) {
                    "Cảm ơn phản hồi"
                } else {
                    "Gợi ý này hữu ích?"
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
                    contentDescription = "Hữu ích",
                    tint = if (advice.feedback == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(19.dp),
                )
            }
            IconButton(
                onClick = { onFeedback(-1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ThumbDown,
                    contentDescription = "Không hữu ích",
                    tint = if (advice.feedback == -1) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(19.dp),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionProposalCard(
    sub: DetectedSubscription,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InsightCard(
        modifier = modifier,
        accentColor = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Khoản chi định kỳ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Được phát hiện từ lịch sử giao dịch",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = sub.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${MoneyFormatter.format(sub.amount)} mỗi tháng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FilledTonalButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = "Thêm vào hóa đơn",
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    accentColor: Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.height(214.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (!isSystemInDarkTheme()) {
                Color.White
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

private fun cleanAiMarkdown(text: String?): String {
    if (text == null) return ""
    return text.replace("**", "").replace("* ", "• ").trim()
}
