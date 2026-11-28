package com.notepay.feature.assets.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.NotePayStatusBanner
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.NotePayAnimatedVisibility
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberMainTabBottomClearance
import com.notepay.core.navigation.shakeOnError
import com.notepay.domain.model.CurrencyConverter
import com.notepay.feature.assets.export.ExportReportBottomSheet
import com.notepay.feature.assets.main.component.AssetsInkHeroCard
import com.notepay.feature.assets.main.component.WalletAssetCard
import com.notepay.feature.assets.main.sheet.TransferBottomSheet
import com.notepay.feature.assets.shared.SharedWalletHubSheet
import com.notepay.feature.debt.management.component.DebtSummaryCard

private val AssetCardMinWidth = 320.dp

private enum class AssetsContentState {
    LOADING,
    ERROR,
    EMPTY,
    CONTENT,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onAddWallet: () -> Unit,
    onEditWallet: (Long) -> Unit,
    onWalletClick: (Long) -> Unit,
    onNavigateToDebtManagement: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    onOpenSharedMembers: (Long) -> Unit = {},
    viewModel: AssetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transferForm by viewModel.transferForm.collectAsStateWithLifecycle()
    val contentGutter = LocalContentGutter.current
    val bottomClearance = rememberMainTabBottomClearance(LocalWindowLayout.current.usesNavigationRail)

    var isArchivedExpanded by remember { mutableStateOf(false) }
    var isExportSheetVisible by remember { mutableStateOf(false) }
    var showSharedWalletHub by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    val contentState = when {
        state.isLoading -> AssetsContentState.LOADING
        state.hasError -> AssetsContentState.ERROR
        state.wallets.isEmpty() && state.archivedWallets.isEmpty() -> AssetsContentState.EMPTY
        else -> AssetsContentState.CONTENT
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppAtmosphericBackground(
            modifier = Modifier.matchParentSize(),
            forceSoftBlurOnPhoto = true,
        )

        NotePayAnimatedContent(
            targetState = contentState,
            label = "assets-content",
            modifier = Modifier.fillMaxSize(),
        ) { target ->
            when (target) {
                AssetsContentState.LOADING -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = AppTheme.colors.primary)
                    }
                }

                AssetsContentState.ERROR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(
                                start = AppTheme.dimensions.spaceLarge,
                                top = AppTheme.dimensions.spaceLarge,
                                end = AppTheme.dimensions.spaceLarge,
                                bottom = bottomClearance,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateWithAction(
                            icon = Icons.Outlined.WarningAmber,
                            iconTint = AppTheme.colors.error,
                            iconBackground = AppTheme.colors.error.copy(alpha = 0.12f),
                            title = stringResource(R.string.assets_load_failed),
                            actionLabel = stringResource(R.string.action_retry_assets),
                            onClick = viewModel::retryLoading,
                        )
                    }
                }

                AssetsContentState.EMPTY -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(
                                start = AppTheme.dimensions.spaceLarge,
                                top = AppTheme.dimensions.spaceLarge,
                                end = AppTheme.dimensions.spaceLarge,
                                bottom = bottomClearance,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyStateWithAction(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            title = stringResource(R.string.home_empty_wallet_title),
                            description = stringResource(R.string.home_empty_wallet_desc),
                            actionLabel = stringResource(R.string.home_create_wallet),
                            onClick = onAddWallet,
                        )
                    }
                }

                AssetsContentState.CONTENT -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = AssetCardMinWidth),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = AppTheme.dimensions.spaceLarge + contentGutter,
                            end = AppTheme.dimensions.spaceLarge + contentGutter,
                            top = AppTheme.dimensions.spaceSmall,
                            bottom = bottomClearance,
                        ),
                        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceLarge),
                        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceLarge),
                    ) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            NotePayStaggeredEntrance(index = 0) {
                                Column {
                                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(R.string.nav_assets),
                                            style = AppTheme.typography.displaySerif,
                                            color = AppTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .semantics { heading() }
                                                .padding(vertical = 4.dp),
                                        )
                                        Spacer(Modifier.width(AppTheme.dimensions.spaceSmall))
                                        Surface(
                                            onClick = { isExportSheetVisible = true },
                                            shape = AppTheme.shapes.capsule,
                                            color = AppTheme.colors.surface.copy(
                                                alpha = if (AppTheme.colors.isLight) 0.92f else 0.86f,
                                            ),
                                            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .iosCardShadow(shape = AppTheme.shapes.capsule, elevated = false),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.FileDownload,
                                                    contentDescription = null,
                                                    tint = AppTheme.colors.textPrimary,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                                Text(
                                                    text = stringResource(R.string.export_report_button),
                                                    style = AppTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = AppTheme.colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AssetsInkHeroCard(
                                        totalNetWorth = state.totalNetWorth,
                                        totalAssetsVnd = state.totalAssetsVnd,
                                        totalLiabilitiesVnd = state.totalLiabilitiesVnd,
                                        assetCount = state.assetCount,
                                        liabilityCount = state.liabilityCount,
                                        changePercentage = state.netWorthChangePercentage,
                                        allocationMode = state.allocationMode,
                                        onAllocationModeChanged = viewModel::onAllocationModeChanged,
                                        allocationItems = state.allocationItems,
                                        hasForeignCurrency = state.hasForeignCurrency,
                                        ratesLastUpdated = state.ratesLastUpdated,
                                    )
                                    if (state.pendingSyncCount > 0) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        NotePayStatusBanner(
                                            visible = true,
                                            message = if (state.isSyncingOutbox) {
                                                stringResource(R.string.assets_outbox_syncing_banner, state.pendingSyncCount)
                                            } else {
                                                stringResource(R.string.assets_outbox_pending_banner, state.pendingSyncCount)
                                            },
                                            type = FeedbackType.Pending,
                                            actionLabel = if (state.isSyncingOutbox) {
                                                null
                                            } else {
                                                stringResource(R.string.shared_wallet_sync_now)
                                            },
                                            onAction = viewModel::syncPendingOutbox,
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    AssetsQuickActionsRow(
                                        onAddWallet = onAddWallet,
                                        onOpenSharedWalletHub = { showSharedWalletHub = true },
                                    )
                                }
                            }
                        }

                        // Thẻ tóm tắt Sổ nợ cá nhân
                        state.debtSummary?.let { summary ->
                            val hasDebtSummary = summary.activeDebtsCount > 0 ||
                                summary.totalToCollect.amountInCents > 0L ||
                                summary.totalToPay.amountInCents > 0L
                            if (hasDebtSummary) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    NotePayStaggeredEntrance(index = 1) {
                                        DebtSummaryCard(
                                            summary = summary,
                                            isHero = false,
                                            onClick = onNavigateToDebtManagement,
                                        )
                                    }
                                }
                            }
                        }

                        // Nhóm 1: TÀI SẢN (ASSETS)
                        if (state.assetWallets.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                NotePayStaggeredEntrance(index = 2) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = AppTheme.dimensions.spaceExtraSmall,
                                                vertical = AppTheme.dimensions.spaceExtraSmall,
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.assets_section_assets_title,
                                                state.assetWallets.size,
                                            ),
                                            style = AppTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary,
                                            modifier = Modifier.semantics { heading() },
                                        )
                                        Text(
                                            text = MoneyFormatter.format(state.totalAssetsVnd),
                                            style = AppTheme.typography.amountRow,
                                            color = AppTheme.colors.success,
                                        )
                                    }
                                }
                            }

                            items(state.assetWallets, key = { "asset_${it.wallet.id}" }) { item ->
                                WalletAssetCard(
                                    item = item,
                                    onClick = { onWalletClick(item.wallet.id) },
                                    onEdit = { onEditWallet(item.wallet.id) },
                                    onSetDefault = { viewModel.setActiveWallet(item.wallet.id) },
                                    onAdjustBalance = { viewModel.openAdjustBalance(item) },
                                    onManageMembers = if (item.wallet.isShared) {
                                        { onOpenSharedMembers(item.wallet.id) }
                                    } else {
                                        null
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        // Nhóm 2: NỢ & THẺ TÍN DỤNG (LIABILITIES) - Ẩn nếu không có
                        if (state.liabilityWallets.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                NotePayStaggeredEntrance(index = 3) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = AppTheme.dimensions.spaceExtraSmall,
                                                vertical = AppTheme.dimensions.spaceExtraSmall,
                                            ),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.assets_section_liabilities_title,
                                                state.liabilityWallets.size,
                                            ),
                                            style = AppTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = AppTheme.colors.textPrimary,
                                            modifier = Modifier.semantics { heading() },
                                        )
                                        Text(
                                            text = MoneyFormatter.format(state.totalLiabilitiesVnd),
                                            style = AppTheme.typography.amountRow,
                                            color = AppTheme.colors.error,
                                        )
                                    }
                                }
                            }

                            items(state.liabilityWallets, key = { "liability_${it.wallet.id}" }) { item ->
                                WalletAssetCard(
                                    item = item,
                                    onClick = { onWalletClick(item.wallet.id) },
                                    onEdit = { onEditWallet(item.wallet.id) },
                                    onSetDefault = { viewModel.setActiveWallet(item.wallet.id) },
                                    onPayCard = { viewModel.openPayCreditCard(item.wallet.id) },
                                    modifier = Modifier.animateItem(),
                                )
                            }
                        }

                        // Nhóm 3: VÍ ĐÃ LƯU TRỮ (ARCHIVED) - Thu gọn / Mở rộng
                        if (state.archivedWallets.isNotEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                val expandStateLabel = stringResource(
                                    if (isArchivedExpanded) {
                                        R.string.stats_expand_state_open
                                    } else {
                                        R.string.stats_expand_state_closed
                                    },
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .clip(AppTheme.shapes.corner12)
                                        .clickable(role = Role.Button) {
                                            isArchivedExpanded = !isArchivedExpanded
                                        }
                                        .semantics(mergeDescendants = true) {
                                            heading()
                                            stateDescription = expandStateLabel
                                        }
                                        .padding(
                                            horizontal = AppTheme.dimensions.spaceExtraSmall,
                                            vertical = AppTheme.dimensions.spaceSmall,
                                        ),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.assets_section_archived_title,
                                            state.archivedWallets.size,
                                        ),
                                        style = AppTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppTheme.colors.textSecondary,
                                    )
                                    Icon(
                                        imageVector = if (isArchivedExpanded) {
                                            Icons.Rounded.ExpandLess
                                        } else {
                                            Icons.Rounded.ExpandMore
                                        },
                                        contentDescription = null,
                                        tint = AppTheme.colors.textSecondary,
                                    )
                                }
                            }

                            if (isArchivedExpanded) {
                                items(state.archivedWallets, key = { "archived_${it.wallet.id}" }) { item ->
                                    WalletAssetCard(
                                        item = item,
                                        onClick = { onWalletClick(item.wallet.id) },
                                        onEdit = { onEditWallet(item.wallet.id) },
                                        onSetDefault = { },
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (transferForm.isVisible) {
            TransferBottomSheet(
                form = transferForm,
                wallets = state.wallets,
                onDismiss = { viewModel.closeTransferSheet() },
                onFromWalletChange = { viewModel.onTransferFromWalletChanged(it) },
                onToWalletChange = { viewModel.onTransferToWalletChanged(it) },
                onAmountChange = { viewModel.onTransferAmountChanged(it) },
                onNoteChange = { viewModel.onTransferNoteChanged(it) },
                onConfirm = { viewModel.executeTransfer() },
            )
        }

        state.adjustBalanceWallet?.let { target ->
            AdjustBalanceBottomSheet(
                item = target,
                onDismiss = { viewModel.closeAdjustBalance() },
                onConfirm = { newBalanceCents ->
                    viewModel.submitAdjustBalance(target.wallet.id, newBalanceCents)
                },
            )
        }

        if (isExportSheetVisible) {
            ExportReportBottomSheet(
                onDismiss = { isExportSheetVisible = false },
                onFeedback = onFeedback,
            )
        }

        if (showSharedWalletHub) {
            SharedWalletHubSheet(
                isCloudConfigured = state.isCloudConfigured,
                isSubmitting = state.isSharedActionSubmitting,
                onDismiss = { showSharedWalletHub = false },
                onCreateWallet = { name, currency, budgetMinor ->
                    viewModel.createSharedWallet(name, currency, budgetMinor) { walletId ->
                        showSharedWalletHub = false
                        onOpenSharedMembers(walletId)
                    }
                },
                onJoinWallet = { code, displayName ->
                    viewModel.joinSharedWallet(code, displayName) { walletId ->
                        showSharedWalletHub = false
                        onOpenSharedMembers(walletId)
                    }
                },
            )
        }
    }
}

@Composable
private fun AssetsQuickActionsRow(
    onAddWallet: () -> Unit,
    onOpenSharedWalletHub: () -> Unit,
) {
    val stackVertically = LocalDensity.current.fontScale >= 1.45f
    val createTitle = stringResource(R.string.assets_quick_create_wallet_title)
    val createDesc = stringResource(R.string.assets_quick_create_wallet_desc)
    val sharedTitle = stringResource(R.string.assets_quick_shared_wallet_title)
    val sharedDesc = stringResource(R.string.assets_quick_shared_wallet_desc)

    if (stackVertically) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AssetsQuickActionCard(
                icon = Icons.Rounded.AddCard,
                title = createTitle,
                description = createDesc,
                containerColor = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
                iconContainerColor = AppTheme.colors.onInk.copy(alpha = 0.14f),
                iconTint = AppTheme.colors.onInk,
                onClick = onAddWallet,
                modifier = Modifier.fillMaxWidth(),
            )
            AssetsQuickActionCard(
                icon = Icons.Rounded.Groups,
                title = sharedTitle,
                description = sharedDesc,
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.textPrimary,
                iconContainerColor = AppTheme.colors.primary.copy(alpha = 0.12f),
                iconTint = AppTheme.colors.primary,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                onClick = onOpenSharedWalletHub,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AssetsQuickActionCard(
                icon = Icons.Rounded.AddCard,
                title = createTitle,
                description = createDesc,
                containerColor = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
                iconContainerColor = AppTheme.colors.onInk.copy(alpha = 0.14f),
                iconTint = AppTheme.colors.onInk,
                onClick = onAddWallet,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            AssetsQuickActionCard(
                icon = Icons.Rounded.Groups,
                title = sharedTitle,
                description = sharedDesc,
                containerColor = AppTheme.colors.surface,
                contentColor = AppTheme.colors.textPrimary,
                iconContainerColor = AppTheme.colors.primary.copy(alpha = 0.12f),
                iconTint = AppTheme.colors.primary,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                onClick = onOpenSharedWalletHub,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun AssetsQuickActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    iconContainerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    border: BorderStroke? = null,
) {
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.corner20,
        color = containerColor,
        contentColor = contentColor,
        border = border,
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppTheme.shapes.corner12)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = AppTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = AppTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.65f),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustBalanceBottomSheet(
    item: WalletAssetItem,
    onDismiss: () -> Unit,
    onConfirm: (newBalanceCents: Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
    val isVnd = item.wallet.currencyCode.equals("VND", ignoreCase = true)
    var input by remember {
        mutableStateOf(
            if (isVnd) (item.balance.amountInCents / 100L).toString()
            else (item.balance.amountInCents / 100.0).toString(),
        )
    }
    var hasError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    NotePayModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.wallet_adjust_balance_title),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(R.string.wallet_adjust_balance_desc),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
            Text(
                text = "${item.wallet.name} · ${stringResource(R.string.wallet_current_balance_badge, if (isVnd) MoneyFormatter.format(item.balance) else CurrencyConverter.formatForeignAmount(item.balance.amountInCents, item.wallet.currencyCode))}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
            )
            OutlinedTextField(
                value = input,
                onValueChange = { raw ->
                    val clean = if (isVnd) raw.filter(Char::isDigit) else raw.filter { it.isDigit() || it == '.' }
                    input = clean
                    if (hasError) hasError = false
                },
                label = { Text(stringResource(R.string.wallet_adjust_new_balance_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = if (isVnd) currencyTransformation else VisualTransformation.None,
                singleLine = true,
                isError = hasError,
                supportingText = {
                    NotePayAnimatedVisibility(visible = hasError) {
                        Text(text = stringResource(R.string.wallet_adjust_error_invalid))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnError(shakeTrigger),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val parsedCents = if (isVnd) {
                            input.toLongOrNull()?.times(100L)
                        } else {
                            input.toDoubleOrNull()?.times(100.0)?.toLong()
                        }
                        if (parsedCents == null) {
                            hasError = true
                            shakeTrigger++
                        } else {
                            onConfirm(parsedCents)
                        }
                    },
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.action_save_changes),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}
