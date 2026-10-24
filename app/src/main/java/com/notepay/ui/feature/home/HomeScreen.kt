package com.notepay.ui.feature.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
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
import com.notepay.ui.component.SmartInsightsCard
import com.notepay.ui.component.SwipeableTransactionItem
import com.notepay.ui.feature.transaction.components.WalletPickerSheet
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.ImageStorageHelper
import com.notepay.ui.util.LocalAuthUser
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import com.notepay.ui.util.localizedName
import com.notepay.ui.util.rememberUserGreeting
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// ── Design tokens của màn Home (bội số của 4) ────────────────────────────────
private val ScreenHorizontalPadding = 16.dp
private val ItemSpacing = 12.dp          // khoảng cách giữa các item cùng nhóm
private val ListBottomClearance = 112.dp // chừa chỗ cho thanh điều hướng nổi
private val AvatarSize = 36.dp
private val InsightsDismissDelay = 250.milliseconds

private object HomeListKeys {
    const val HERO_ASSETS = "home_item_hero_assets"
    const val SURFACE_HEADER = "home_item_surface_header"
    const val BUDGET_PROJECTION = "home_item_budget_projection"
    const val SMART_INSIGHTS = "home_item_smart_insights"
    const val RECENT_EMPTY = "home_item_recent_empty"
    const val BOTTOM_FILLER = "home_item_bottom_filler"
}

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
    // Tự động reset trạng thái ẩn khi người dùng đổi sang ví khác
    var isBudgetProjectionDismissed by rememberSaveable(state.selectedWalletId) { mutableStateOf(false) }
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

    val isAllWallets = state.selectedWalletId == null
    val displayBalance = state.selectedWalletSummary?.balance ?: state.totalBalance
    val displayIncome = state.selectedWalletSummary?.monthlyIncome ?: state.totalMonthlyIncome
    val displayExpense = state.selectedWalletSummary?.monthlyExpense ?: state.totalMonthlyExpense

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val targetWalletId = selectedWalletIdForBg ?: state.activeWallet?.id
            targetWalletId?.let { walletId ->
                val savedPath = ImageStorageHelper.saveImageToInternalStorage(
                    context = context,
                    sourceUri = uri,
                    walletId = walletId
                )
                if (savedPath != null) {
                    viewModel.setWalletBackground(walletId, savedPath)
                }
            }
        }
    }

    // 1. KỸ THUẬT ĐẢO NỀN: Nền gốc là Surface (Tầng dưới) -> Đáy không bao giờ bị hở Sunken
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.surface)
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

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = ListBottomClearance),
        ) {
            // ==================== TẦNG TRÊN: HERO ASSETS ====================
            item(key = HomeListKeys.HERO_ASSETS) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.surfaceSunken)
                        .statusBarsPadding()
                        .padding(top = 12.dp)
                ) {
                    // Top Bar: Lời chào + Streak + Avatar
                    HomeGreetingBar(
                        greeting = rememberUserGreeting(),
                        streakDays = state.streakDays,
                        authUser = LocalAuthUser.current,
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Hero Balance & Cashflow Summary (Đồng bộ tức thì 0ms qua selectedWalletSummary)
                    HeroBalanceWithCashflow(
                        label = if (isAllWallets) {
                            stringResource(R.string.home_total_balance_all)
                        } else {
                            stringResource(R.string.home_wallet_balance_format, state.selectedWalletSummary?.wallet?.name.orEmpty())
                        },
                        balance = displayBalance,
                        income = displayIncome,
                        expense = displayExpense,
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Carousel Thẻ Ví Nhỏ Gọn (Thẻ "Tất cả ví" + từng ví + nút Thêm)
                    WalletsCompactCarousel(
                        wallets = state.walletsSummary,
                        selectedWalletId = state.selectedWalletId,
                        totalBalanceAll = state.totalBalance,
                        onSelectWallet = { walletId -> viewModel.selectWallet(walletId) },
                        onAddWallet = onAddWallet
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 2. Mép bo cong 24dp chuyển tiếp Sunken -> Surface nằm ngay cuối khối Hero
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = AppTheme.colors.surface,
                                shape = AppTheme.shapes.sheetTop // Bo đúng 2 góc trên 24dp, 2 góc dưới 0dp!
                            )
                            .padding(start = ScreenHorizontalPadding, end = ScreenHorizontalPadding, top = 20.dp, bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recentLabel,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
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
                }
            }

            // ==================== TẦNG DƯỚI: NỀN SURFACE TỰ NHIÊN ====================
            // 3. Thẻ dự báo ngân sách: Chỉ hiện khi chọn một ví cụ thể có đặt hạn mức
            val budgetProjection = state.budgetProjection
            val budgetLimit = state.selectedWalletSummary?.wallet?.budgetLimit
            val budgetCardVisible = !isAllWallets &&
                budgetProjection != null &&
                !isBudgetProjectionDismissed &&
                budgetLimit != null &&
                budgetLimit.amountInCents > 0L
            val budgetStatus = budgetProjection?.let(::budgetStatusOf)

            if (budgetCardVisible && budgetStatus != BudgetStatus.Safe) {
                item(key = HomeListKeys.BUDGET_PROJECTION) {
                    BudgetProjectionCard(
                        projection = budgetProjection!!,
                        budgetLimit = budgetLimit!!,
                        walletName = state.selectedWalletSummary?.wallet?.name.orEmpty(),
                        onDismiss = { isBudgetProjectionDismissed = true },
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 6.dp)
                    )
                }
            } else if (!state.isSmartInsightsDismissed && (budgetStatus == null || budgetStatus == BudgetStatus.Safe)) {
                item(key = HomeListKeys.SMART_INSIGHTS) {
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
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 6.dp)
                    )
                }
            }

            // 4. Danh sách giao dịch gần đây - Nền tự nhiên là Surface
            if (state.recentTransactions.isEmpty()) {
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
                        modifier = Modifier.padding(horizontal = ScreenHorizontalPadding, vertical = 4.dp)
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
                viewAllWallets = false
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

/** Thanh chào mừng người dùng phía trên cùng */
@Composable
private fun HomeGreetingBar(
    greeting: String,
    streakDays: Int,
    authUser: com.notepay.domain.model.AuthUser?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (streakDays > 0) {
                Surface(
                    shape = AppTheme.shapes.corner8,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.home_streak_format, streakDays),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (authUser != null) {
            val initial = (authUser.displayName?.firstOrNull() ?: authUser.email?.firstOrNull())
                ?.uppercaseChar()
                ?.toString()
            UserAvatar(
                photoUrl = authUser.photoUrl,
                initial = initial,
            )
        }
    }
}

/** Cụm số dư Hero + Capsule dòng tiền tháng này (Cashflow Summary) */
@Composable
private fun HeroBalanceWithCashflow(
    label: String,
    balance: Money,
    income: Money,
    expense: Money,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = balance,
            label = "BalanceAnimation"
        ) { targetBalance ->
            Text(
                text = MoneyFormatter.format(targetBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Capsule dòng tiền (Thu / Chi tháng này)
        Surface(
            shape = AppTheme.shapes.corner12,
            color = AppTheme.colors.surface,
            border = BorderStroke(1.dp, AppTheme.colors.border.copy(alpha = 0.5f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Thu nhập
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = stringResource(R.string.home_income_month),
                        tint = AppTheme.colors.success,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "+${MoneyFormatter.format(income)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.success
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(12.dp)
                        .background(AppTheme.colors.border)
                )

                // Chi tiêu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = stringResource(R.string.home_expense_month),
                        tint = AppTheme.colors.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "-${MoneyFormatter.format(expense)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppTheme.colors.error
                    )
                }
            }
        }
    }
}

/** Carousel thẻ ví nhỏ gọn (Compact Wallet Cards) */
@Composable
private fun WalletsCompactCarousel(
    wallets: List<WalletSummary>,
    selectedWalletId: Long?,
    totalBalanceAll: Money,
    onSelectWallet: (Long?) -> Unit,
    onAddWallet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thẻ đầu tiên: Tất cả ví
        item(key = "wallet_all") {
            val isSelected = selectedWalletId == null
            CompactWalletCard(
                name = stringResource(R.string.home_all_wallets),
                balance = totalBalanceAll,
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppTheme.colors.primary,
                isSelected = isSelected,
                onClick = { onSelectWallet(null) }
            )
        }

        // Từng thẻ ví riêng biệt
        items(
            items = wallets,
            key = { "wallet_${it.wallet.id}" }
        ) { summary ->
            val isSelected = summary.wallet.id == selectedWalletId
            val icon = WalletUiHelper.getIcon(summary.wallet.iconKey)
            val color = WalletUiHelper.getColor(summary.wallet.colorKey)

            CompactWalletCard(
                name = summary.wallet.name,
                balance = summary.balance,
                icon = icon,
                iconTint = color,
                isSelected = isSelected,
                onClick = { onSelectWallet(summary.wallet.id) }
            )
        }

        // Thẻ nút Thêm ví (+)
        item(key = "wallet_add_button") {
            Surface(
                modifier = Modifier
                    .size(width = 88.dp, height = 92.dp)
                    .clip(AppTheme.shapes.corner16)
                    .clickable(onClick = onAddWallet),
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, AppTheme.colors.border.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppTheme.colors.primaryContainer.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.home_add_wallet),
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.home_add_wallet),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Từng thẻ ví nhỏ gọn 156dp x 92dp */
@Composable
private fun CompactWalletCard(
    name: String,
    balance: Money,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val containerColor = if (isSelected) {
        AppTheme.colors.surfaceRaised
    } else {
        AppTheme.colors.surface
    }

    Surface(
        modifier = modifier
            .size(width = 156.dp, height = 92.dp)
            .clip(AppTheme.shapes.corner16)
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner16,
        color = containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(iconTint.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = MoneyFormatter.format(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
    walletName: String,
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