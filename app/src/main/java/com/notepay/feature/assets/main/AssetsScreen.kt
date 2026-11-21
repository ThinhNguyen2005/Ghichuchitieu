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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.designsystem.util.WalletUiHelper
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.CurrencyConverter
import com.notepay.domain.model.WalletPurpose
import com.notepay.feature.assets.export.ExportReportBottomSheet
import com.notepay.feature.assets.main.component.AssetsInkHeroCard
import com.notepay.feature.assets.main.component.WalletAssetCard
import com.notepay.feature.assets.main.sheet.TransferBottomSheet
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
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
                            top = AppTheme.dimensions.spaceLarge,
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
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = AppTheme.colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(vertical = 4.dp),
                                        )
                                        OutlinedButton(
                                            onClick = { isExportSheetVisible = true },
                                            shape = AppTheme.shapes.capsule,
                                            modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.FileDownload,
                                                contentDescription = stringResource(R.string.export_report_action_cd),
                                                modifier = Modifier.size(18.dp),
                                            )
                                            Spacer(Modifier.size(6.dp))
                                            Text(
                                                text = stringResource(R.string.export_report_button),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
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
                                        )
                                        Text(
                                            text = MoneyFormatter.format(state.totalAssetsVnd),
                                            style = AppTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppTheme.colors.onInkIncome,
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
                                        )
                                        Text(
                                            text = MoneyFormatter.format(state.totalLiabilitiesVnd),
                                            style = AppTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppTheme.colors.onInkExpense,
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isArchivedExpanded = !isArchivedExpanded }
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
                                    IconButton(
                                        onClick = { isArchivedExpanded = !isArchivedExpanded },
                                        modifier = Modifier.size(36.dp),
                                    ) {
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
            AdjustBalanceDialog(
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
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.colors.sand,
                    selectedLabelColor = AppTheme.colors.onSand,
                ),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            )
        }
    }
}

@Composable
private fun AdjustBalanceDialog(
    item: WalletAssetItem,
    onDismiss: () -> Unit,
    onConfirm: (newBalanceCents: Long) -> Unit,
) {
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
    val isVnd = item.wallet.currencyCode.equals("VND", ignoreCase = true)
    var input by remember {
        mutableStateOf(
            if (isVnd) (item.balance.amountInCents / 100L).toString()
            else (item.balance.amountInCents / 100.0).toString(),
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.wallet_adjust_balance_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.wallet_adjust_balance_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${item.wallet.name} · ${stringResource(R.string.wallet_current_balance_badge, if (isVnd) MoneyFormatter.format(item.balance) else CurrencyConverter.formatForeignAmount(item.balance.amountInCents, item.wallet.currencyCode))}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        val clean = if (isVnd) raw.filter(Char::isDigit) else raw.filter { it.isDigit() || it == '.' }
                        input = clean
                    },
                    label = { Text(stringResource(R.string.wallet_adjust_new_balance_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = if (isVnd) currencyTransformation else VisualTransformation.None,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = if (isVnd) {
                        (input.toLongOrNull() ?: 0L) * 100L
                    } else {
                        ((input.toDoubleOrNull() ?: 0.0) * 100.0).toLong()
                    }
                    onConfirm(cents)
                },
            ) {
                Text(
                    text = stringResource(R.string.action_save_changes),
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
