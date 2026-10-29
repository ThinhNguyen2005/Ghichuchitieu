package com.notepay.feature.home

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
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.WindowLayout
import com.notepay.ui.component.dialog.ConfirmDeleteDialog
import com.notepay.ui.component.feedback.EmptyStateWithAction
import com.notepay.feature.home.component.SmartInsightsCard
import com.notepay.feature.home.component.HomeDaySection
import com.notepay.feature.home.component.HomeHeroBlock
import com.notepay.feature.home.component.ScreenHorizontalPadding
import com.notepay.feature.home.component.rememberHomeDayGroups
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.util.localizedName
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// ── Design tokens của màn Home (bội số của 4) ────────────────────────────────
private val ListBottomClearance = 112.dp // chừa chỗ cho thanh điều hướng nổi
private val InsightsDismissDelay = 250.milliseconds

private object HomeListKeys {
    const val HERO_ASSETS = "home_item_hero_assets"
    const val INSIGHT = "home_item_insight"
    const val RECENT_HEADER = "home_item_recent_header"
    const val RECENT_EMPTY = "home_item_recent_empty"
}

/** Tiền tố key cho từng nhóm ngày trong LazyColumn (key phải duy nhất toàn danh sách). */
private const val HomeDayKeyPrefix = "home_day_"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSeeAll: () -> Unit,
    onAddWallet: () -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = onTransactionClick,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    // Tự động reset trạng thái ẩn khi người dùng đổi sang ví khác
    var isBudgetProjectionDismissed by rememberSaveable(state.selectedWalletId) { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isWalletPickerOpen by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    // Tablet: lề bù để nội dung một cột không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    // Expanded: chuyển sang 2 cột — hero bên trái, danh sách giao dịch bên phải.
    val isTwoPane = LocalWindowLayout.current == WindowLayout.Expanded

    // Resolve string resources trước khi dùng trong non-Composable scopes.
    val recentLabel = stringResource(R.string.home_recent_transactions)
    val seeAllLabel = stringResource(R.string.action_see_all)
    val emptyTx = stringResource(R.string.home_empty_transactions)
    val emptyWalletTitle = stringResource(R.string.home_empty_wallet_title)
    val emptyWalletDesc = stringResource(R.string.home_empty_wallet_desc)
    val createWalletLabel = stringResource(R.string.home_create_wallet)

    // Tra tên ví O(1) thay vì find{} cho từng dòng giao dịch.
    val walletNames = remember(state.wallets) { state.wallets.associate { it.id to it.name } }
    // Gom giao dịch gần đây theo ngày để hiển thị thành từng thẻ nhóm như sổ tay.
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

    // Toàn trang dùng nền `background` của app; thẻ trắng (nhóm giao dịch) nổi lên trên.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
            return@Box
        }

        if (state.wallets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(ScreenHorizontalPadding),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateWithAction(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    title = emptyWalletTitle,
                    description = emptyWalletDesc,
                    actionLabel = createWalletLabel,
                    onClick = onAddWallet,
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@Box
        }

        // ==================== EXPANDED: BỐ CỤC 2 CỘT ====================
        if (isTwoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = contentGutter)
            ) {
                // Cột trái: nền trang trí, lời chào, bộ chọn ví, số dư và thẻ gợi ý.
                Column(
                    modifier = Modifier
                        .weight(0.45f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 12.dp, bottom = 24.dp)
                ) {
                    HomeHeroBlock(
                        streakDays = state.streakDays,
                        walletLabel = walletChipLabel,
                        balanceLabel = balanceLabel,
                        balance = displayBalance,
                        income = displayIncome,
                        expense = displayExpense,
                        onOpenWalletPicker = { isWalletPickerOpen = true },
                        topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HomeInsightCard(
                        streakDays = state.streakDays,
                        budgetProjection = state.budgetProjection,
                        budgetLimit = selectedWallet?.wallet?.budgetLimit,
                        walletName = selectedWallet?.wallet?.name.orEmpty(),
                        budgetSpentPercentage = state.budgetProjection?.spentPercentage ?: 0f,
                        isAllWallets = isAllWallets,
                        isBudgetCardDismissed = isBudgetProjectionDismissed,
                        isInsightsDismissed = state.isSmartInsightsDismissed,
                        isBudgetExceeded = state.isBudgetExceeded,
                        onDismissBudget = { isBudgetProjectionDismissed = true },
                        onDismissInsights = {
                            coroutineScope.launch {
                                delay(InsightsDismissDelay)
                                viewModel.dismissSmartInsights()
                            }
                        },
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp)
                    )
                }

                // Cột phải: feed giao dịch gần đây gom theo ngày — thẻ trắng trên nền xám.
                Column(
                    modifier = Modifier
                        .weight(0.55f)
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 24.dp)
                ) {
                    RecentTransactionsHeader(
                        title = recentLabel,
                        seeAllLabel = seeAllLabel,
                        onSeeAll = onSeeAll,
                        modifier = Modifier.padding(start = 4.dp)
                    )

                    if (dayGroups.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateWithAction(title = emptyTx)
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = dayGroups,
                                key = { HomeDayKeyPrefix + it.key }
                            ) { group ->
                                HomeDaySection(
                                    group = group,
                                    walletNames = walletNames,
                                    hapticEnabled = hapticFeedbackEnabled,
                                    onTransactionClick = onTransactionClick,
                                    onEditTransaction = onEditTransaction,
                                    onDeleteTransaction = { tx -> pendingDeleteTransaction = tx },
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            return@Box
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentGutter,
                end = contentGutter,
                bottom = ListBottomClearance,
            ),
        ) {
            // ==================== KHỐI HERO + NỀN TRANG TRÍ ====================
            item(key = HomeListKeys.HERO_ASSETS) {
                HomeHeroBlock(
                    streakDays = state.streakDays,
                    walletLabel = walletChipLabel,
                    balanceLabel = balanceLabel,
                    balance = displayBalance,
                    income = displayIncome,
                    expense = displayExpense,
                    onOpenWalletPicker = { isWalletPickerOpen = true },
                    topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            // 3. Thẻ dự báo ngân sách hoặc gợi ý thông minh (dùng chung với bố cục 2 cột).
            item(key = HomeListKeys.INSIGHT) {
                HomeInsightCard(
                    streakDays = state.streakDays,
                    budgetProjection = state.budgetProjection,
                    budgetLimit = selectedWallet?.wallet?.budgetLimit,
                    walletName = selectedWallet?.wallet?.name.orEmpty(),
                    budgetSpentPercentage = state.budgetProjection?.spentPercentage ?: 0f,
                    isAllWallets = isAllWallets,
                    isBudgetCardDismissed = isBudgetProjectionDismissed,
                    isInsightsDismissed = state.isSmartInsightsDismissed,
                    isBudgetExceeded = state.isBudgetExceeded,
                    onDismissBudget = { isBudgetProjectionDismissed = true },
                    onDismissInsights = {
                        coroutineScope.launch {
                            delay(InsightsDismissDelay)
                            viewModel.dismissSmartInsights()
                        }
                    },
                    modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 8.dp)
                )
            }

            // 4. Feed giao dịch gần đây gom theo ngày — thẻ trắng trên nền xám.
            item(key = HomeListKeys.RECENT_HEADER) {
                RecentTransactionsHeader(
                    title = recentLabel,
                    seeAllLabel = seeAllLabel,
                    onSeeAll = onSeeAll,
                    modifier = Modifier.padding(start = ScreenHorizontalPadding, end = 4.dp, top = 8.dp)
                )
            }

            if (dayGroups.isEmpty()) {
                item(key = HomeListKeys.RECENT_EMPTY) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = ScreenHorizontalPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateWithAction(title = emptyTx)
                    }
                }
            } else {
                items(
                    items = dayGroups,
                    key = { HomeDayKeyPrefix + it.key }
                ) { group ->
                    HomeDaySection(
                        group = group,
                        walletNames = walletNames,
                        hapticEnabled = hapticFeedbackEnabled,
                        onTransactionClick = onTransactionClick,
                        onEditTransaction = onEditTransaction,
                        onDeleteTransaction = { tx -> pendingDeleteTransaction = tx },
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
                    )
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
            onDismiss = { isWalletPickerOpen = false }
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
            }
        )
    }
}

/** Tiêu đề mục feed giao dịch: tên mục kiểu serif + lối tắt "Xem tất cả". */
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTheme.typography.displaySerif,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        TextButton(onClick = onSeeAll) {
            Text(
                text = seeAllLabel,
                color = AppTheme.colors.primary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Thẻ gợi ý dưới khối hero: dự báo ngân sách khi ví có hạn mức cần cảnh báo, ngược lại là
 * thẻ gợi ý thông minh. Tách riêng để điều kiện hiển thị chỉ tồn tại ở một nơi duy nhất.
 */
@Composable
private fun HomeInsightCard(
    streakDays: Int,
    budgetProjection: BudgetProjection?,
    budgetLimit: Money?,
    walletName: String,
    budgetSpentPercentage: Float,
    isAllWallets: Boolean,
    isBudgetCardDismissed: Boolean,
    isInsightsDismissed: Boolean,
    isBudgetExceeded: Boolean,
    onDismissBudget: () -> Unit,
    onDismissInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val budgetStatus = budgetProjection?.let(::budgetStatusOf)

    if (!isAllWallets &&
        !isBudgetCardDismissed &&
        budgetProjection != null &&
        budgetLimit != null &&
        budgetLimit.amountInCents > 0L &&
        budgetStatus != BudgetStatus.Safe
    ) {
        BudgetProjectionCard(
            projection = budgetProjection,
            budgetLimit = budgetLimit,
            walletName = walletName,
            onDismiss = onDismissBudget,
            modifier = modifier
        )
    } else if (!isInsightsDismissed && (budgetStatus == null || budgetStatus == BudgetStatus.Safe)) {
        SmartInsightsCard(
            streakDays = streakDays,
            isBudgetExceeded = isBudgetExceeded,
            budgetSpentPercentage = budgetSpentPercentage,
            onDismiss = onDismissInsights,
            modifier = modifier
        )
    }
}

/**
 * Trạng thái ngân sách. Mỗi trạng thái có icon riêng để không chỉ dựa vào màu sắc.
 * Màu sắc được ánh xạ động qua [AppTheme.colors] (hỗ trợ cả Light và Dark mode).
 */
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

/** Xác định trạng thái ngân sách dựa trên tỷ lệ chi tiêu và dự báo vượt hạn mức. */
private fun budgetStatusOf(projection: BudgetProjection): BudgetStatus {
    val spentPercentage = projection.spentPercentage
    return when {
        spentPercentage >= 0.90f -> BudgetStatus.Danger
        spentPercentage >= 0.70f || projection.isProjectedToExceed -> BudgetStatus.Caution
        else -> BudgetStatus.Safe
    }
}

/** Tạo chuỗi AnnotatedString với các đoạn văn bản trong [highlights] được in đậm. */
private fun highlightedText(text: String, vararg highlights: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (highlight in highlights) {
        if (highlight.isEmpty()) continue
        val startIndex = text.indexOf(highlight)
        if (startIndex >= 0) {
            builder.addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = startIndex,
                end = startIndex + highlight.length
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
    modifier: Modifier = Modifier
) {
    val spentPercentage = projection.spentPercentage
    val status = budgetStatusOf(projection)

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
                amountStr
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = status.color.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, status.color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // end = 4dp vì nút đóng đã có vùng chạm 48dp bên trong.
                .padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = status.icon,
                    contentDescription = null,
                    tint = status.color,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    if (walletName.isNotBlank()) {
                        Text(
                            text = walletName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = status.color,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = adviceMessage,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                // IconButton đảm bảo vùng chạm tối thiểu 48dp (trước đây icon 18dp bấm rất khó).
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Thanh tiến độ: nhìn là hiểu đã dùng bao nhiêu ngân sách, không cần đọc chữ.
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