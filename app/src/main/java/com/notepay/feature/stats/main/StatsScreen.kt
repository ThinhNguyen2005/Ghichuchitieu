package com.notepay.feature.stats.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.rememberMainTabBottomClearance
import com.notepay.domain.money.Money
import com.notepay.feature.assets.export.ExportReportBottomSheet
import com.notepay.feature.stats.main.component.AiInsightsCarousel
import com.notepay.feature.stats.main.component.BudgetProgressBar
import com.notepay.feature.stats.main.component.SpendingPredictionCard
import com.notepay.feature.stats.main.component.StatsDashboard
import com.notepay.feature.stats.main.component.StatsMetric
import com.notepay.feature.subscription.main.AddSubscriptionDialogState
import com.notepay.feature.subscription.main.sheet.AddSubscriptionBottomSheet
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") onAddTransaction: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onConfigureLocalModel: () -> Unit = {},
    onConfigureBudget: (Long?) -> Unit = {},
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addSubFormState by viewModel.addSubForm.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel, onFeedback) {
        viewModel.feedback.collect { feedback ->
            launch { onFeedback(feedback) }
        }
    }
    var showAmounts by rememberSaveable { mutableStateOf(true) }
    var isExportSheetVisible by rememberSaveable { mutableStateOf(false) }

    val contentState = when {
        state.isLoading -> StatsContentState.LOADING
        !state.hasAnyTransactions -> StatsContentState.EMPTY
        else -> StatsContentState.CONTENT
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        Column(modifier = Modifier.fillMaxSize()) {
            StatsEditorialHeader(
                primaryWalletName = state.selectedWallet?.name,
                showAmounts = showAmounts,
                onExportClick = { isExportSheetVisible = true },
                onToggleAmounts = { showAmounts = !showAmounts },
            )

            NotePayAnimatedContent(
                targetState = contentState,
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
                            onPreviousMonth = viewModel::previousPeriod,
                            onNextMonth = viewModel::nextPeriod,
                            onSelectRange = viewModel::selectRange,
                            onCategorySelected = viewModel::selectCategory,
                            onTransactionClick = onTransactionClick,
                            supportingContent = { metric ->
                                StatsSupportingContent(
                                    state = state,
                                    metric = metric,
                                    showAmounts = showAmounts,
                                    onConfigureBudget = { onConfigureBudget(state.selectedWallet?.id) },
                                    onAdviceFeedback = viewModel::sendAdviceFeedback,
                                    onAddSubscription = viewModel::showAddSubscription,
                                    onGenerateLocalAdvice = viewModel::generateLocalAdvice,
                                    onSelectLocalModel = onConfigureLocalModel,
                                )
                            },
                        )
                    }
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

    if (isExportSheetVisible) {
        ExportReportBottomSheet(
            onDismiss = { isExportSheetVisible = false },
            onFeedback = onFeedback,
        )
    }
}

@Composable
private fun StatsEditorialHeader(
    primaryWalletName: String?,
    showAmounts: Boolean,
    onExportClick: () -> Unit,
    onToggleAmounts: () -> Unit,
) {
    val pillContainerColor = AppTheme.colors.surface.copy(
        alpha = if (AppTheme.colors.isLight) 0.90f else 0.85f,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.stats_screen_title),
                style = AppTheme.typography.displaySerif,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (!primaryWalletName.isNullOrBlank()) {
                Text(
                    text = primaryWalletName,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Surface(
            onClick = onExportClick,
            shape = CircleShape,
            color = pillContainerColor,
            contentColor = AppTheme.colors.textPrimary,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(AppTheme.dimensions.minTouchTarget)
                .iosCardShadow(shape = CircleShape, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = stringResource(R.string.export_report_action_cd),
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            onClick = onToggleAmounts,
            shape = CircleShape,
            color = pillContainerColor,
            contentColor = AppTheme.colors.textPrimary,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(AppTheme.dimensions.minTouchTarget)
                .iosCardShadow(shape = CircleShape, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (showAmounts) {
                        Icons.Rounded.Visibility
                    } else {
                        Icons.Rounded.VisibilityOff
                    },
                    contentDescription = stringResource(
                        if (showAmounts) R.string.stats_cd_hide_amounts else R.string.stats_cd_show_amounts,
                    ),
                    tint = AppTheme.colors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
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
    val bottomClearance = rememberMainTabBottomClearance(LocalWindowLayout.current.usesNavigationRail)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = bottomClearance),
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
    onConfigureBudget: () -> Unit,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (DetectedSubscription) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
) {
    val isExpense = metric == StatsMetric.CHI_TIEU
    val hasInsights = state.spendingForecast != null ||
        state.aiAdvices.isNotEmpty() ||
        state.detectedSubscriptions.isNotEmpty()

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
            color = AppTheme.colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = AppTheme.colors.textSecondary,
        )
    }
}
