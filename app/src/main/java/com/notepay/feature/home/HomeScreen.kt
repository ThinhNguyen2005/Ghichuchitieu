package com.notepay.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.WindowLayout
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.analytics.SmartTipItem
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.feature.auth.main.AuthViewModel
import com.notepay.feature.auth.main.component.sheet.AccountDetailsBottomSheet
import com.notepay.feature.home.component.HomeBackgroundLayer
import com.notepay.feature.home.component.HomeCustomizationSheet
import com.notepay.feature.home.component.HomeDaySection
import com.notepay.feature.home.component.HomeHeroBlock
import com.notepay.feature.home.component.ScreenHorizontalPadding
import com.notepay.feature.home.component.SmartInsightsCard
import com.notepay.feature.home.component.rememberHomeDayGroups
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import kotlinx.coroutines.launch

private val ListBottomClearance = 120.dp

private enum class HomeContentState { LOADING, EMPTY_WALLETS, CONTENT }

private object HomeListKeys {
    const val HERO_ASSETS = "home_item_hero_assets"
    const val INSIGHT = "home_item_insight"
    const val RECENT_HEADER = "home_item_recent_header"
    const val RECENT_EMPTY = "home_item_recent_empty"
}

private const val HomeDayKeyPrefix = "home_day_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeAll: () -> Unit,
    onAddWallet: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = onTransactionClick,
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    viewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var isBudgetProjectionDismissed by rememberSaveable(state.selectedWalletId) { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isWalletPickerOpen by remember { mutableStateOf(false) }
    var isCustomizationOpen by rememberSaveable { mutableStateOf(false) }
    var showAccountDetails by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reducedMotion = rememberNotePayReducedMotion()
    val listState = rememberLazyListState()
    val contentGutter = LocalContentGutter.current
    val listBottomClearance = maxOf(
        ListBottomClearance,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )
    val isTwoPane = LocalWindowLayout.current == WindowLayout.Expanded

    val recentLabel = stringResource(R.string.home_recent_transactions)
    val seeAllLabel = stringResource(R.string.action_see_all)
    val emptyTx = stringResource(R.string.home_empty_transactions)
    val emptyWalletTitle = stringResource(R.string.home_empty_wallet_title)
    val emptyWalletDesc = stringResource(R.string.home_empty_wallet_desc)
    val createWalletLabel = stringResource(R.string.home_create_wallet)
    val savedPhotoMessage = stringResource(R.string.home_bg_photo_saved)
    val errorPhotoMessage = stringResource(R.string.home_bg_photo_error)

    val walletNames = remember(state.wallets) { state.wallets.associate { it.id to it.name } }
    val walletsById = remember(state.wallets) { state.wallets.associateBy { it.id } }
    val effectiveUserId = state.currentUserId ?: authUiState.user?.uid
    val dayGroups = rememberHomeDayGroups(state.recentTransactions)

    val isAllWallets = state.selectedWalletId == null
    val selectedWallet = state.selectedWalletSummary
    val displayBalance = selectedWallet?.balance ?: state.totalBalance
    val displayIncome = selectedWallet?.monthlyIncome ?: state.totalMonthlyIncome
    val displayExpense = selectedWallet?.monthlyExpense ?: state.totalMonthlyExpense
    val balanceLabel = if (isAllWallets) {
        stringResource(R.string.home_total_balance_all)
    } else {
        stringResource(
            R.string.home_wallet_balance_format,
            selectedWallet?.wallet?.name.orEmpty(),
        )
    }
    val walletChipLabel = selectedWallet?.wallet?.name ?: stringResource(R.string.home_all_wallets)
    val contentState = when {
        state.isLoading -> HomeContentState.LOADING
        state.wallets.isEmpty() -> HomeContentState.EMPTY_WALLETS
        else -> HomeContentState.CONTENT
    }
    val openAccountSheet: (() -> Unit)? = if (authUiState.user != null) {
        { showAccountDetails = true }
    } else {
        null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HomeBackgroundLayer(
            preset = state.backgroundPreset,
            customImagePath = state.customBackgroundPath,
            blurEnabled = state.backgroundBlurEnabled,
            modifier = Modifier.matchParentSize(),
        )

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                }
            },
            label = "homeContentState",
            modifier = Modifier.fillMaxSize(),
        ) { targetState ->
            when (targetState) {
                HomeContentState.LOADING -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }

                HomeContentState.EMPTY_WALLETS -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(ScreenHorizontalPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateWithAction(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = emptyWalletTitle,
                            description = emptyWalletDesc,
                            actionLabel = createWalletLabel,
                            onClick = onAddWallet,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                HomeContentState.CONTENT -> {
                    if (isTwoPane) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = contentGutter),
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(0.45f)
                                    .fillMaxHeight()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 12.dp, bottom = listBottomClearance),
                            ) {
                                NotePayStaggeredEntrance(index = 0) {
                                    HomeHeroBlock(
                                        streakDays = state.streakDays,
                                        walletLabel = walletChipLabel,
                                        balanceLabel = balanceLabel,
                                        balance = displayBalance,
                                        income = displayIncome,
                                        expense = displayExpense,
                                        isBalanceHidden = state.isBalanceHidden,
                                        onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                                        onOpenWalletPicker = { isWalletPickerOpen = true },
                                        onOpenCustomization = { isCustomizationOpen = true },
                                        topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                        onAvatarClick = openAccountSheet,
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                NotePayStaggeredEntrance(index = 1) {
                                    HomeInsightCard(
                                        budgetProjection = state.budgetProjection,
                                        budgetLimit = selectedWallet?.wallet?.budgetLimit,
                                        walletName = selectedWallet?.wallet?.name.orEmpty(),
                                        activeTip = state.activeSmartTip,
                                        tipPosition = state.activeSmartTipIndex + 1,
                                        totalTips = state.smartTips.size,
                                        isAllWallets = isAllWallets,
                                        isBudgetCardDismissed = isBudgetProjectionDismissed,
                                        isInsightsDismissed = state.isSmartInsightsDismissed,
                                        reducedMotion = reducedMotion,
                                        onDismissBudget = { isBudgetProjectionDismissed = true },
                                        onNextTip = viewModel::nextSmartTip,
                                        onDismissInsights = viewModel::dismissSmartInsights,
                                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 6.dp),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .weight(0.55f)
                                    .fillMaxHeight()
                                    .statusBarsPadding()
                                    .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                            ) {
                                RecentTransactionsHeader(
                                    title = recentLabel,
                                    seeAllLabel = seeAllLabel,
                                    onSeeAll = onSeeAll,
                                    modifier = Modifier.padding(start = 4.dp),
                                )

                                if (dayGroups.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        EmptyStateWithAction(title = emptyTx)
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = listBottomClearance),
                                    ) {
                                        items(
                                            items = dayGroups,
                                            key = { HomeDayKeyPrefix + it.key },
                                        ) { group ->
                                            HomeDaySection(
                                                group = group,
                                                walletNames = walletNames,
                                                walletsById = walletsById,
                                                currentUserId = effectiveUserId,
                                                hapticEnabled = hapticFeedbackEnabled,
                                                isBalanceHidden = state.isBalanceHidden,
                                                onTransactionClick = onTransactionClick,
                                                onEditTransaction = onEditTransaction,
                                                onDeleteTransaction = { tx -> pendingDeleteTransaction = tx },
                                                modifier = Modifier
                                                    .then(if (reducedMotion) Modifier else Modifier.animateItem())
                                                    .padding(start = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = contentGutter,
                                end = contentGutter,
                                bottom = listBottomClearance,
                            ),
                        ) {
                            item(key = HomeListKeys.HERO_ASSETS) {
                                NotePayStaggeredEntrance(index = 0) {
                                    HomeHeroBlock(
                                        streakDays = state.streakDays,
                                        walletLabel = walletChipLabel,
                                        balanceLabel = balanceLabel,
                                        balance = displayBalance,
                                        income = displayIncome,
                                        expense = displayExpense,
                                        isBalanceHidden = state.isBalanceHidden,
                                        onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                                        onOpenWalletPicker = { isWalletPickerOpen = true },
                                        onOpenCustomization = { isCustomizationOpen = true },
                                        topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                                        modifier = Modifier.padding(top = 12.dp),
                                        onAvatarClick = openAccountSheet,
                                    )
                                }
                            }

                            item(key = HomeListKeys.INSIGHT) {
                                NotePayStaggeredEntrance(index = 1) {
                                    HomeInsightCard(
                                        budgetProjection = state.budgetProjection,
                                        budgetLimit = selectedWallet?.wallet?.budgetLimit,
                                        walletName = selectedWallet?.wallet?.name.orEmpty(),
                                        activeTip = state.activeSmartTip,
                                        tipPosition = state.activeSmartTipIndex + 1,
                                        totalTips = state.smartTips.size,
                                        isAllWallets = isAllWallets,
                                        isBudgetCardDismissed = isBudgetProjectionDismissed,
                                        isInsightsDismissed = state.isSmartInsightsDismissed,
                                        reducedMotion = reducedMotion,
                                        onDismissBudget = { isBudgetProjectionDismissed = true },
                                        onNextTip = viewModel::nextSmartTip,
                                        onDismissInsights = viewModel::dismissSmartInsights,
                                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 6.dp),
                                    )
                                }
                            }

                            item(key = HomeListKeys.RECENT_HEADER) {
                                RecentTransactionsHeader(
                                    title = recentLabel,
                                    seeAllLabel = seeAllLabel,
                                    onSeeAll = onSeeAll,
                                    modifier = Modifier.padding(start = ScreenHorizontalPadding, end = 4.dp, top = 12.dp),
                                )
                            }

                            if (dayGroups.isEmpty()) {
                                item(key = HomeListKeys.RECENT_EMPTY) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp, horizontal = ScreenHorizontalPadding),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        EmptyStateWithAction(title = emptyTx)
                                    }
                                }
                            } else {
                                items(
                                    items = dayGroups,
                                    key = { HomeDayKeyPrefix + it.key },
                                ) { group ->
                                    HomeDaySection(
                                        group = group,
                                        walletNames = walletNames,
                                        walletsById = walletsById,
                                        currentUserId = effectiveUserId,
                                        hapticEnabled = hapticFeedbackEnabled,
                                        isBalanceHidden = state.isBalanceHidden,
                                        onTransactionClick = onTransactionClick,
                                        onEditTransaction = onEditTransaction,
                                        onDeleteTransaction = { tx -> pendingDeleteTransaction = tx },
                                        modifier = Modifier
                                            .then(if (reducedMotion) Modifier else Modifier.animateItem())
                                            .padding(horizontal = ScreenHorizontalPadding),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isWalletPickerOpen) {
        WalletPickerSheet(
            wallets = state.wallets,
            selectedWalletId = state.selectedWalletId,
            onWalletSelected = viewModel::selectWallet,
            onAllWalletsSelected = { viewModel.selectWallet(null) },
            onDismiss = { isWalletPickerOpen = false },
        )
    }

    if (isCustomizationOpen) {
        HomeCustomizationSheet(
            activePreset = state.backgroundPreset,
            customBackgroundPath = state.customBackgroundPath,
            backgroundBlurEnabled = state.backgroundBlurEnabled,
            isSmartTipsDismissed = state.isSmartInsightsDismissed,
            onSelectPreset = viewModel::selectBackgroundPreset,
            onPickCustomPhoto = { uri ->
                viewModel.applyCustomBackground(uri) { success ->
                    coroutineScope.launch {
                        onFeedback(
                            UiFeedback(
                                message = if (success) savedPhotoMessage else errorPhotoMessage,
                                type = if (success) FeedbackType.Success else FeedbackType.Error,
                            )
                        )
                    }
                }
            },
            onClearCustomPhoto = viewModel::clearCustomBackground,
            onToggleBlur = viewModel::setBackgroundBlurEnabled,
            onToggleSmartTips = viewModel::setSmartInsightsEnabled,
            onDismiss = { isCustomizationOpen = false },
        )
    }

    val currentUser = authUiState.user
    if (showAccountDetails && currentUser != null) {
        val accountSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AccountDetailsBottomSheet(
            user = currentUser,
            sheetState = accountSheetState,
            onDismissRequest = { showAccountDetails = false },
            onSignOut = {
                authViewModel.signOut(context)
                showAccountDetails = false
            },
        )
    }

    pendingDeleteTransaction?.let { tx ->
        val itemName = tx.note.ifBlank { tx.category.localizedName() }
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_transaction_title),
            itemName = itemName,
            onConfirm = {
                viewModel.deleteTransaction(tx.id)
                pendingDeleteTransaction = null
            },
            onDismiss = {
                pendingDeleteTransaction = null
            },
        )
    }
}

@Composable
private fun RecentTransactionsHeader(
    title: String,
    seeAllLabel: String,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppTheme.typography.displaySerif,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = false)
                .semantics { heading() },
        )
        TextButton(
            onClick = onSeeAll,
            modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
        ) {
            Text(
                text = seeAllLabel,
                color = AppTheme.colors.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun HomeInsightCard(
    budgetProjection: BudgetProjection?,
    budgetLimit: Money?,
    walletName: String,
    activeTip: SmartTipItem?,
    tipPosition: Int,
    totalTips: Int,
    isAllWallets: Boolean,
    isBudgetCardDismissed: Boolean,
    isInsightsDismissed: Boolean,
    reducedMotion: Boolean,
    onDismissBudget: () -> Unit,
    onNextTip: () -> Unit,
    onDismissInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budgetStatus = budgetProjection?.let(::budgetStatusOf)
    val showBudgetProjection = !isAllWallets &&
        !isBudgetCardDismissed &&
        budgetProjection != null &&
        budgetLimit != null &&
        budgetLimit.amountInCents > 0L &&
        budgetStatus != BudgetStatus.Safe
    val showSmartInsights = !showBudgetProjection &&
        !isInsightsDismissed &&
        activeTip != null &&
        (budgetStatus == null || budgetStatus == BudgetStatus.Safe)

    val enterTransition = if (reducedMotion) {
        EnterTransition.None
    } else {
        fadeIn(animationSpec = tween(200)) + expandVertically(animationSpec = tween(220))
    }
    val exitTransition = if (reducedMotion) {
        ExitTransition.None
    } else {
        fadeOut(animationSpec = tween(150)) + shrinkVertically(animationSpec = tween(200))
    }

    AnimatedVisibility(
        visible = showBudgetProjection,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        val projection = budgetProjection ?: return@AnimatedVisibility
        val limit = budgetLimit ?: return@AnimatedVisibility
        BudgetProjectionCard(
            projection = projection,
            budgetLimit = limit,
            walletName = walletName,
            onDismiss = onDismissBudget,
            modifier = modifier,
        )
    }

    AnimatedVisibility(
        visible = showSmartInsights,
        enter = enterTransition,
        exit = exitTransition,
    ) {
        val tip = activeTip ?: return@AnimatedVisibility
        SmartInsightsCard(
            tip = tip,
            tipPosition = tipPosition,
            totalTips = totalTips,
            onNextTip = onNextTip,
            onDismiss = onDismissInsights,
            modifier = modifier,
        )
    }
}

private enum class BudgetStatus(val icon: ImageVector) {
    Safe(Icons.Rounded.CheckCircle),
    Caution(Icons.AutoMirrored.Rounded.TrendingUp),
    Danger(Icons.Rounded.Warning),
}

private val BudgetStatus.color: Color
    @Composable
    get() = when (this) {
        BudgetStatus.Safe -> AppTheme.colors.success
        BudgetStatus.Caution -> AppTheme.colors.warning
        BudgetStatus.Danger -> AppTheme.colors.error
    }

private fun budgetStatusOf(projection: BudgetProjection): BudgetStatus {
    val spentPercentage = projection.spentPercentage
    return when {
        spentPercentage >= 0.90f -> BudgetStatus.Danger
        spentPercentage >= 0.70f || projection.isProjectedToExceed -> BudgetStatus.Caution
        else -> BudgetStatus.Safe
    }
}

private fun highlightedText(text: String, vararg highlights: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (highlight in highlights) {
        if (highlight.isEmpty()) continue
        val startIndex = text.indexOf(highlight)
        if (startIndex >= 0) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = startIndex,
                end = startIndex + highlight.length,
            )
        }
    }
    return builder.toAnnotatedString()
}

@Composable
private fun BudgetProjectionCard(
    projection: BudgetProjection,
    budgetLimit: Money,
    walletName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spentPercentage = projection.spentPercentage
    val status = budgetStatusOf(projection)
    val cardShape = AppTheme.shapes.corner20

    val overspentFormat = stringResource(R.string.budget_overspent)
    val willExceedFormat = stringResource(R.string.budget_will_exceed)
    val safeFormat = stringResource(R.string.budget_safe)

    val spentCents = projection.spentThisWallet.amountInCents
    val limitCents = budgetLimit.amountInCents
    val isOverspent = spentCents > limitCents

    val (rawAdvice, highlightAmount) = when {
        isOverspent -> {
            val overspent = spentCents - limitCents
            val amountStr = MoneyFormatter.format(Money(overspent))
            overspentFormat.format(amountStr) to amountStr
        }
        projection.isProjectedToExceed -> {
            val amountStr = MoneyFormatter.format(projection.safeDailyLimit)
            willExceedFormat.format(
                projection.exhaustionDateLabel.orEmpty(),
                amountStr,
            ) to amountStr
        }
        else -> {
            val amountStr = MoneyFormatter.format(projection.projectedSpend)
            safeFormat.format(amountStr) to amountStr
        }
    }

    val adviceMessage = remember(rawAdvice, highlightAmount) {
        highlightedText(rawAdvice, highlightAmount)
    }
    val performHaptic = rememberNotePayHaptic()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = false),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
            contentColor = AppTheme.colors.textPrimary,
        ),
        border = BorderStroke(0.5.dp, status.color.copy(alpha = 0.28f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = null,
                    tint = status.color,
                    modifier = Modifier.size(24.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                ) {
                    if (walletName.isNotBlank()) {
                        Text(
                            text = walletName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = status.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = adviceMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary,
                    )
                }

                IconButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { spentPercentage.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 12.dp)
                    .height(8.dp)
                    .clip(CircleShape),
                color = status.color,
                trackColor = status.color.copy(alpha = 0.15f),
            )
        }
    }
}
