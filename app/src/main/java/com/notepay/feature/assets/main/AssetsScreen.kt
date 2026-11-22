package com.notepay.feature.assets.main

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.designsystem.util.WalletUiHelper
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.core.navigation.shakeOnError
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.WalletPurpose
import com.notepay.feature.assets.export.ExportReportBottomSheet
import com.notepay.feature.assets.main.component.AssetsInkHeroCard
import com.notepay.feature.assets.main.component.WalletAssetCard
import com.notepay.feature.assets.main.sheet.TransferBottomSheet
import com.notepay.feature.assets.shared.CreateSharedWalletDialog
import com.notepay.feature.assets.shared.JoinSharedWalletDialog
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
    val reducedMotion = rememberNotePayReducedMotion()
    val contentGutter = LocalContentGutter.current
    val bottomClearance = if (LocalWindowLayout.current.usesNavigationRail) 24.dp else maxOf(
        120.dp,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

    var isArchivedExpanded by remember { mutableStateOf(false) }
    var isExportSheetVisible by remember { mutableStateOf(false) }
    var showCreateSharedDialog by remember { mutableStateOf(false) }
    var showJoinSharedDialog by remember { mutableStateOf(false) }

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

        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                }
            },
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
                            .padding(AppTheme.dimensions.spaceLarge),
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
                            .padding(AppTheme.dimensions.spaceLarge),
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
                                        canTransfer = state.wallets.size >= 2,
                                        hasForeignCurrency = state.hasForeignCurrency,
                                        ratesLastUpdated = state.ratesLastUpdated,
                                        onAddWallet = onAddWallet,
                                        onTransferClick = { viewModel.openTransferSheet() },
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    PurposeFilterChipRow(
                                        selectedPurpose = state.selectedPurpose,
                                        onPurposeSelected = viewModel::onPurposeFilterChanged,
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SharedWalletQuickActionsRow(
                                        onCreateSharedWallet = { showCreateSharedDialog = true },
                                        onJoinByInviteCode = { showJoinSharedDialog = true },
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

        if (showCreateSharedDialog) {
            CreateSharedWalletDialog(
                isCloudConfigured = state.isCloudConfigured,
                isSubmitting = state.isSharedActionSubmitting,
                onDismiss = { showCreateSharedDialog = false },
                onConfirm = { name, currency, budgetMinor ->
                    viewModel.createSharedWallet(name, currency, budgetMinor) { walletId ->
                        showCreateSharedDialog = false
                        onOpenSharedMembers(walletId)
                    }
                },
            )
        }

        if (showJoinSharedDialog) {
            JoinSharedWalletDialog(
                isCloudConfigured = state.isCloudConfigured,
                isSubmitting = state.isSharedActionSubmitting,
                onDismiss = { showJoinSharedDialog = false },
                onConfirm = { code, displayName ->
                    viewModel.joinSharedWallet(code, displayName) { walletId ->
                        showJoinSharedDialog = false
                        onOpenSharedMembers(walletId)
                    }
                },
            )
        }
    }
}

@Composable
private fun SharedWalletQuickActionsRow(
    onCreateSharedWallet: () -> Unit,
    onJoinByInviteCode: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SharedWalletActionPill(
            icon = Icons.Rounded.GroupAdd,
            label = stringResource(R.string.shared_wallet_create_action),
            onClick = onCreateSharedWallet,
            modifier = Modifier.weight(1f),
        )
        SharedWalletActionPill(
            icon = Icons.Rounded.Key,
            label = stringResource(R.string.shared_wallet_join_action),
            onClick = onJoinByInviteCode,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SharedWalletActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.corner16,
        color = AppTheme.colors.surface,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .iosCardShadow(shape = AppTheme.shapes.corner16, elevated = false),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppTheme.colors.textPrimary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = AppTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PurposeFilterChipRow(
    selectedPurpose: WalletPurpose?,
    onPurposeSelected: (WalletPurpose?) -> Unit,
) {
    val purposes = remember {
        listOf<WalletPurpose?>(
            null,
            WalletPurpose.PERSONAL,
            WalletPurpose.BUSINESS,
            WalletPurpose.SAVINGS_GOAL,
            WalletPurpose.TRAVEL,
            WalletPurpose.SHARED,
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        purposes.forEach { purpose ->
            val selected = selectedPurpose == purpose
            val label = if (purpose == null) {
                stringResource(R.string.wallet_purpose_all)
            } else {
                stringResource(WalletUiHelper.getPurposeTitleRes(purpose))
            }
            FilterChip(
                selected = selected,
                onClick = { onPurposeSelected(purpose) },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shape = AppTheme.shapes.capsule,
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = AppTheme.colors.hairline,
                    selectedBorderColor = AppTheme.colors.sand,
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = AppTheme.colors.surface,
                    labelColor = AppTheme.colors.textSecondary,
                    selectedContainerColor = AppTheme.colors.sand,
                    selectedLabelColor = AppTheme.colors.onSand,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.surface,
        shape = AppTheme.shapes.sheetTop,
        scrimColor = Color.Black.copy(alpha = 0.45f),
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
                    AnimatedVisibility(visible = hasError) {
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
