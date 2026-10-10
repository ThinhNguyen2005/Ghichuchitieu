package com.notepay.ui.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.ui.component.BalanceCard
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.SmartInsightsCard
import com.notepay.ui.component.SwipeableTransactionItem
import com.notepay.ui.feature.transaction.components.WalletPickerSheet
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.ImageStorageHelper
import com.notepay.ui.util.LocalAuthUser
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.localizedName
import com.notepay.ui.util.rememberUserGreeting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// ── Design tokens của màn Home (bội số của 4) ────────────────────────────────
private val ScreenHorizontalPadding = 16.dp
private val ItemSpacing = 12.dp          // khoảng cách giữa các item cùng nhóm
private val SectionExtraGap = 12.dp      // cộng thêm trước tiêu đề section → 24dp giữa 2 nhóm
private val ListBottomClearance = 128.dp // chừa chỗ cho thanh điều hướng / nút nổi (chỉnh tại 1 chỗ)
private val AvatarSize = 32.dp
private val InsightsDismissDelay = 250.milliseconds

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
    var showWalletPickerSheet by rememberSaveable { mutableStateOf(false) }
    var selectedWalletIdForBg by rememberSaveable { mutableStateOf<Long?>(null) }
    var isBudgetProjectionDismissed by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Resolve string resources trước khi dùng trong non-Composable scopes.
    val recentLabel = stringResource(R.string.home_recent_transactions)
    val seeAllLabel = stringResource(R.string.action_see_all)
    val emptyTx = stringResource(R.string.home_empty_transactions)
    val emptyWalletTitle = stringResource(R.string.home_empty_wallet_title)
    val emptyWalletDesc = stringResource(R.string.home_empty_wallet_desc)
    val createWalletLabel = stringResource(R.string.home_create_wallet)

    // Tra tên ví O(1) thay vì find{} cho từng dòng giao dịch.
    val walletNames = remember(state.wallets) { state.wallets.associate { it.id to it.name } }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val targetWalletId = selectedWalletIdForBg ?: state.activeWallet?.id
            targetWalletId?.let { walletId ->
                // 1. Copy ảnh từ URI tạm sang file nội bộ vĩnh viễn
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(
                    context = context,
                    sourceUri = uri,
                    walletId = walletId
                )

                // 2. Lưu đường dẫn file tĩnh này vào Database qua ViewModel
                if (savedPath != null) {
                    viewModel.setWalletBackground(walletId, savedPath)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            val greeting = rememberUserGreeting()
            val authUser = LocalAuthUser.current

            GradientTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (state.streakDays > 0) {
                            Surface(
                                shape = AppTheme.shapes.corner8,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ) {
                                Text(
                                    text = stringResource(R.string.home_streak_format, state.streakDays),
                                    // labelMedium (12sp) thay vì labelSmall (11sp) để đạt ngưỡng dễ đọc tối thiểu.
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (authUser != null) {
                        val initial = (authUser.displayName?.firstOrNull() ?: authUser.email?.firstOrNull())
                            ?.uppercaseChar()
                            ?.toString()
                        UserAvatar(
                            photoUrl = authUser.photoUrl,
                            initial = initial,
                            modifier = Modifier.padding(end = ScreenHorizontalPadding)
                        )
                    }
                }
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.wallets.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
            return@Scaffold
        }

        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = padding.calculateStartPadding(layoutDirection),
                    end = padding.calculateEndPadding(layoutDirection)
                ),
            state = listState,
            contentPadding = PaddingValues(
                start = ScreenHorizontalPadding,
                top = padding.calculateTopPadding() + 16.dp,
                end = ScreenHorizontalPadding,
                bottom = padding.calculateBottomPadding() + ListBottomClearance
            ),
            verticalArrangement = Arrangement.spacedBy(ItemSpacing),
        ) {
            item(key = "balance_card") {
                BalanceCard(
                    walletsSummary = state.walletsSummary,
                    activeWalletId = state.activeWallet?.id,
                    onWalletChanged = { walletId -> viewModel.selectWallet(walletId) },
                    onOpenWalletPicker = if (state.walletsSummary.size > 1) {
                        { showWalletPickerSheet = true }
                    } else null,
                    onChangeBackground = { walletId ->
                        selectedWalletIdForBg = walletId
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }

            val budgetProjection = state.budgetProjection
            val budgetLimit = state.activeWallet?.budgetLimit
            val budgetCardVisible = budgetProjection != null &&
                !isBudgetProjectionDismissed &&
                budgetLimit != null &&
                budgetLimit.amountInCents > 0L
            val budgetStatus = budgetProjection?.let(::budgetStatusOf)

            // Tối đa 1 thẻ ở đầu trang: cảnh báo ngân sách (Caution/Danger) > Smart Insights > ngân sách Safe
            when {
                budgetCardVisible && budgetStatus != BudgetStatus.Safe -> {
                    checkNotNull(budgetProjection)
                    checkNotNull(budgetLimit)
                    item(key = "budget_projection") {
                        BudgetProjectionCard(
                            projection = budgetProjection,
                            budgetLimit = budgetLimit,
                            onDismiss = { isBudgetProjectionDismissed = true }
                        )
                    }
                }
                !state.isSmartInsightsDismissed && (budgetStatus == null || budgetStatus == BudgetStatus.Safe) -> {
                    item(key = "smart_insights") {
                        SmartInsightsCard(
                            streakDays = state.streakDays,
                            isBudgetExceeded = state.isBudgetExceeded,
                            budgetSpentPercentage = state.budgetProjection?.spentPercentage ?: 0f,
                            onDismiss = {
                                coroutineScope.launch {
                                    delay(InsightsDismissDelay)
                                    viewModel.dismissSmartInsights()
                                }
                            },
                        )
                    }
                }
                budgetCardVisible && budgetStatus == BudgetStatus.Safe -> {
                    checkNotNull(budgetProjection)
                    checkNotNull(budgetLimit)
                    item(key = "budget_projection") {
                        BudgetProjectionCard(
                            projection = budgetProjection,
                            budgetLimit = budgetLimit,
                            onDismiss = { isBudgetProjectionDismissed = true }
                        )
                    }
                }
            }

            // Tiêu đề section cách các thẻ phía trên 24dp nhưng chỉ cách danh sách 12dp
            // → mắt nhóm "Giao dịch gần đây" thành một cụm (proximity).
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SectionExtraGap),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = recentLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    TextButton(onClick = onSeeAll) { Text(seeAllLabel) }
                }
            }

            if (state.recentTransactions.isEmpty()) {
                item(key = "recent_empty") { EmptyStateWithAction(title = emptyTx) }
            } else {
                items(
                    items = state.recentTransactions,
                    key = { it.id }
                ) { tx ->
                    SwipeableTransactionItem(
                        transaction = tx,
                        walletName = walletNames[tx.walletId].orEmpty(),
                        onClick = { onTransactionClick(tx.id) },
                        onEdit = { onEditTransaction(tx.id) },
                        onDelete = { pendingDeleteTransaction = tx },
                        hapticEnabled = hapticFeedbackEnabled,
                    )
                }
            }
        }
    }

    if (showWalletPickerSheet) {
        WalletPickerSheet(
            wallets = state.wallets,
            selectedWalletId = state.activeWallet?.id,
            onWalletSelected = { walletId ->
                viewModel.selectWallet(walletId)
                showWalletPickerSheet = false
            },
            onDismiss = { showWalletPickerSheet = false }
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

/** Avatar người dùng: ảnh nếu có, ngược lại chữ cái đầu, cuối cùng là icon người. */
@Composable
private fun UserAvatar(
    photoUrl: String?,
    initial: String?,
    modifier: Modifier = Modifier,
) {
    val avatarModifier = modifier
        .size(AvatarSize)
        .clip(CircleShape)

    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.auth_avatar_description),
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    } else {
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (initial != null) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spentPercentage = projection.spentPercentage
    val status = budgetStatusOf(projection)

    val overspentFormat = stringResource(R.string.budget_overspent)
    val willExceedFormat = stringResource(R.string.budget_will_exceed)
    val safeFormat = stringResource(R.string.budget_safe)

    val (rawAdvice, highlightAmount) = when {
        spentPercentage >= 1.0f -> {
            val overspent = projection.spentThisWallet.amountInCents - budgetLimit.amountInCents
            val amountStr = MoneyFormatter.format(Money(overspent))
            overspentFormat.format(amountStr) to amountStr
        }
        projection.isProjectedToExceed -> {
            val amountStr = MoneyFormatter.format(projection.safeDailyLimit)
            willExceedFormat.format(
                projection.exhaustionDateLabel,
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

                Text(
                    text = adviceMessage,
                    // bodyMedium (14sp) thay vì bodySmall (12sp): đây là thông điệp chính của thẻ.
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp)
                )

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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    NotePayTheme {
        Column(Modifier.padding(16.dp)) {
            val mockWallet = Wallet(id = 1L, name = "Tiền mặt", initialBalance = Money(1_500_000_00), iconKey = "cash", colorKey = "primary")
            BalanceCard(
                walletsSummary = listOf(
                    WalletSummary(
                        wallet = mockWallet,
                        balance = Money(1_500_000_00),
                        monthlyIncome = Money(5_000_000_00),
                        monthlyExpense = Money(3_500_000_00)
                    )
                ),
                activeWalletId = 1L,
                onWalletChanged = {}
            )
        }
    }
}