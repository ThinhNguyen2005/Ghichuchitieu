package com.notepay.feature.assets.main

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.component.feedback.EmptyStateWithAction
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.feature.assets.main.component.AssetsInkHeroCard
import com.notepay.feature.assets.main.component.WalletAssetCard
import com.notepay.feature.assets.main.sheet.TransferBottomSheet
import com.notepay.feature.debt.management.component.DebtSummaryCard

/** Bề rộng tối thiểu của một thẻ ví trong grid; dưới ngưỡng này grid tự rơi về 1 cột. */
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
    // Tablet: lề bù để danh sách một cột không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    // Compact/Medium có thanh điều hướng nổi ở đáy nên phải chừa chỗ; chế độ rail thì không.
    val bottomClearance = if (LocalWindowLayout.current.usesNavigationRail) 24.dp else maxOf(
        120.dp,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    val contentState = when {
        state.isLoading -> AssetsContentState.LOADING
        state.hasError -> AssetsContentState.ERROR
        state.wallets.isEmpty() -> AssetsContentState.EMPTY
        else -> AssetsContentState.CONTENT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
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
                        // Grid tự chia cột theo bề rộng khả dụng: 1 cột ở Compact, 2 cột khi đủ chỗ.
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
                                // Item của grid không tự xếp chồng nhiều con như LazyColumn, nên inset
                                // phải nằm trong cùng một Column, nếu không thẻ Ink sẽ trôi lên dưới
                                // thanh trạng thái và bị đồng hồ hệ thống đè lên.
                                Column {
                                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                                    AssetsInkHeroCard(
                                        totalNetWorth = state.totalNetWorth,
                                        changePercentage = state.netWorthChangePercentage,
                                        allocationItems = state.allocationItems,
                                        canTransfer = state.wallets.size >= 2,
                                        onAddWallet = onAddWallet,
                                        onTransferClick = { viewModel.openTransferSheet() },
                                    )
                                }
                            }
                        }

                        // Thẻ tóm tắt Sổ nợ cá nhân (Personal Debts Summary)
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

                        // Header danh sách ví
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            NotePayStaggeredEntrance(index = 2) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = AppTheme.dimensions.spaceExtraSmall, vertical = AppTheme.dimensions.spaceExtraSmall),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = stringResource(R.string.assets_wallets_header),
                                        style = AppTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = AppTheme.colors.textPrimary,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.assets_wallets_count_format,
                                            state.wallets.size,
                                            stringResource(R.string.wallets_count_label),
                                        ),
                                        style = AppTheme.typography.bodySmall,
                                        color = AppTheme.colors.textSecondary,
                                    )
                                }
                            }
                        }

                        items(state.wallets, key = { it.wallet.id }) { item ->
                            WalletAssetCard(
                                item = item,
                                onClick = { onWalletClick(item.wallet.id) },
                                onEdit = { onEditWallet(item.wallet.id) },
                                onSetDefault = { viewModel.setActiveWallet(item.wallet.id) },
                                modifier = Modifier.animateItem(),
                            )
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
                onConfirm = { viewModel.executeTransfer() }
            )
        }
    }
}
