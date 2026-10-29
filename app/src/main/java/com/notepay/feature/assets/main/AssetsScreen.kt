package com.notepay.feature.assets.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.feature.assets.main.component.WalletAppIcon
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.feature.debt.management.component.DebtSummaryCard
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.designsystem.util.WalletUiHelper
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** Bề rộng tối thiểu của một thẻ ví trong grid; dưới ngưỡng này grid tự rơi về 1 cột. */
private val AssetCardMinWidth = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onAddWallet: () -> Unit,
    onEditWallet: (Long) -> Unit,
    onWalletClick: (Long) -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    onNavigateToDebtManagement: () -> Unit = {},
    viewModel: AssetsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val transferSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Tablet: lề bù để danh sách một cột không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    // Compact/Medium có thanh điều hướng nổi ở đáy nên phải chừa chỗ; chế độ rail thì không.
    val bottomClearance = if (LocalWindowLayout.current.usesNavigationRail) 24.dp else 120.dp

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AppTheme.colors.primary)
            }
        } else {
            LazyVerticalGrid(
                // Grid tự chia cột theo bề rộng khả dụng: 1 cột ở Compact, 2 cột khi đủ chỗ.
                columns = GridCells.Adaptive(minSize = AssetCardMinWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = AppTheme.dimensions.spaceLarge + contentGutter,
                    end = AppTheme.dimensions.spaceLarge + contentGutter,
                    top = AppTheme.dimensions.spaceLarge,
                    bottom = bottomClearance
                ),
                verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceLarge),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceLarge)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    // Item của grid không tự xếp chồng nhiều con như LazyColumn, nên inset
                    // phải nằm trong cùng một Column, nếu không thẻ Ink sẽ trôi lên dưới
                    // thanh trạng thái và bị đồng hồ hệ thống đè lên.
                    Column {
                        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                        AssetsInkHeroCard(
                            totalNetWorth = state.totalNetWorth,
                            changePercentage = state.netWorthChangePercentage,
                            allocationItems = state.allocationItems,
                            onAddWallet = onAddWallet,
                            onTransferClick = { viewModel.openTransferSheet() }
                        )
                    }
                }

                // Thẻ tóm tắt Sổ nợ cá nhân (Personal Debts Summary)
                val summary = state.debtSummary
                if (summary != null && (summary.activeDebtsCount > 0 || summary.totalToCollect.amountInCents > 0L || summary.totalToPay.amountInCents > 0L)) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        DebtSummaryCard(
                            summary = summary,
                            onClick = onNavigateToDebtManagement,
                        )
                    }
                }

                // Header danh sách ví
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = AppTheme.dimensions.spaceExtraSmall, vertical = AppTheme.dimensions.spaceExtraSmall),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.assets_wallets_header),
                            style = AppTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AppTheme.colors.textPrimary
                        )
                        Text(
                            text = stringResource(
                                R.string.assets_wallets_count_format,
                                state.wallets.size,
                                stringResource(R.string.wallets_count_label)
                            ),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary
                        )
                    }
                }

                items(state.wallets, key = { it.wallet.id }) { item ->
                    WalletAssetCard(
                        item = item,
                        onClick = { onWalletClick(item.wallet.id) },
                        onEdit = { onEditWallet(item.wallet.id) },
                        onSetDefault = { viewModel.setActiveWallet(item.wallet.id) }
                    )
                }
            }
        }

        if (state.isTransferSheetVisible) {
            TransferBottomSheet(
                state = state,
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

@Composable
private fun AssetsInkHeroCard(
    totalNetWorth: Money,
    changePercentage: Float,
    allocationItems: List<WalletAllocationItem>,
    onAddWallet: () -> Unit,
    onTransferClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.ink,
        ),
        border = if (!AppTheme.colors.isLight) {
            BorderStroke(1.dp, AppTheme.colors.inkBorder)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.spaceExtraLarge)
        ) {
            // Dòng trên: Nhãn "Tổng tài sản ròng" + Badge xu hướng biến động
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.assets_net_worth_title),
                    style = AppTheme.typography.labelMedium,
                    color = AppTheme.colors.onInkMuted,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))

                if (changePercentage != 0f) {
                    val isPositive = changePercentage >= 0f
                    val trendColor = if (isPositive) AppTheme.colors.onInkIncome else AppTheme.colors.onInkExpense
                    Surface(
                        shape = AppTheme.shapes.capsule,
                        color = trendColor.copy(alpha = 0.18f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            val formattedPct = String.format(Locale.US, "%.1f", abs(changePercentage))
                            Text(
                                text = stringResource(
                                    if (isPositive) R.string.percent_change_positive else R.string.percent_change_negative,
                                    formattedPct
                                ),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = trendColor
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = AppTheme.shapes.capsule,
                        color = AppTheme.colors.onInk.copy(alpha = 0.10f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = null,
                                tint = AppTheme.colors.onInkMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(R.string.badge_stable),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.onInkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

            // Số tiền Hero: Inter + tnum để các chữ số không nhảy ngang khi số dư đổi
            Text(
                text = MoneyFormatter.format(totalNetWorth),
                style = AppTheme.typography.amountHero,
                color = AppTheme.colors.onInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // UX: Xử lý cảnh báo số âm dưới số tiền hero
            if (totalNetWorth.amountInCents < 0L) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = AppTheme.colors.onInkExpense,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.assets_negative_balance_hint),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.onInkExpense
                    )
                }
            }

            // Thanh phân bổ Segmented Bar (chỉ hiển thị khi tổng tài sản > 0)
            if (totalNetWorth.amountInCents > 0L && allocationItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(AppTheme.shapes.capsule),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    allocationItems.forEach { item ->
                        val weight = item.percentage.coerceAtLeast(0.01f)
                        val color = WalletUiHelper.getColor(item.colorKey)
                        Box(
                            modifier = Modifier
                                .weight(weight)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

                // Dòng Legend: tối đa 3 ví lớn nhất + gom "Khác" nếu > 3 ví
                val (topItems, otherPct) = remember(allocationItems) {
                    if (allocationItems.size <= 3) {
                        allocationItems to 0
                    } else {
                        val top = allocationItems.take(3)
                        val other = (allocationItems.drop(3).sumOf { it.percentage.toDouble() } * 100).roundToInt()
                        top to other
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topItems.forEach { item ->
                        val color = WalletUiHelper.getColor(item.colorKey)
                        val pct = (item.percentage * 100).roundToInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceExtraSmall),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Text(
                                text = item.name,
                                style = AppTheme.typography.labelMedium,
                                color = AppTheme.colors.onInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.percent_format, pct),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onInk
                            )
                        }
                    }

                    if (otherPct > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceExtraSmall)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.onInkMuted)
                            )
                            Text(
                                text = stringResource(R.string.assets_allocation_other),
                                style = AppTheme.typography.labelMedium,
                                color = AppTheme.colors.onInkMuted
                            )
                            Text(
                                text = stringResource(R.string.percent_format, otherPct),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

            // Hai Action Buttons cân bằng phân cấp thị giác (Pill Shape, Touch Target >= 48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceMedium)
            ) {
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                    shape = AppTheme.shapes.capsule,
                    contentPadding = PaddingValues(horizontal = AppTheme.dimensions.spaceMedium, vertical = AppTheme.dimensions.spaceMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.sand,
                        contentColor = AppTheme.colors.onSand
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.action_transfer),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onAddWallet,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                    shape = AppTheme.shapes.capsule,
                    contentPadding = PaddingValues(horizontal = AppTheme.dimensions.spaceMedium, vertical = AppTheme.dimensions.spaceMedium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.inkRaised,
                        contentColor = AppTheme.colors.onInk
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = stringResource(R.string.action_add_wallet),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletAssetCard(
    item: WalletAssetItem,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wallet = item.wallet
    val colorValue = WalletUiHelper.getColor(wallet.colorKey)

    val cardBorder = if (wallet.isActive) {
        BorderStroke(1.dp, AppTheme.colors.primary.copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, AppTheme.colors.separator)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner20)
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTheme.dimensions.spaceLarge)
        ) {
            // Hàng đầu: Icon ví | Cột tên & Badge | Nút Sửa ví (IconButton minTouchTarget 48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorValue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    WalletAppIcon(
                        wallet = wallet,
                        modifier = Modifier,
                        iconSize = 24.dp,
                        tint = colorValue
                    )
                }

                Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceMedium))

                // Cột thông tin ví
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = wallet.name,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (wallet.isActive) {
                            Surface(
                                shape = AppTheme.shapes.corner12,
                                color = AppTheme.colors.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = AppTheme.dimensions.spaceSmall, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(AppTheme.colors.primary)
                                    )
                                    Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                                    Text(
                                        text = stringResource(R.string.wallet_badge_active),
                                        style = AppTheme.typography.labelMedium,
                                        color = AppTheme.colors.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                            Text(
                                text = stringResource(R.string.bullet_separator),
                                style = AppTheme.typography.bodySmall,
                                color = AppTheme.colors.textSecondary
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                        }

                        Text(
                            text = stringResource(R.string.wallet_tx_count, item.transactionCount),
                            style = AppTheme.typography.bodySmall,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                // Chuyển nút Sửa ví lên góc trên bên phải để tiết kiệm không gian dưới cùng
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.action_edit_wallet),
                        tint = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))

            // Dòng số dư
            Text(
                text = MoneyFormatter.format(item.balance),
                style = AppTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.align(Alignment.End)
            )

            // Dải thu chi tháng: Lọc triệt để số 0
            val hasIncome = item.monthlyIncome.amountInCents > 0L
            val hasExpense = item.monthlyExpense.amountInCents > 0L

            if (hasIncome || hasExpense) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.corner12)
                        .background(AppTheme.colors.surfaceSunken)
                        .padding(horizontal = AppTheme.dimensions.spaceMedium, vertical = AppTheme.dimensions.spaceSmall),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasIncome) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = AppTheme.colors.success,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(
                                    R.string.amount_positive_format,
                                    MoneyFormatter.format(item.monthlyIncome)
                                ),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.success
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (hasExpense) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceExtraSmall))
                            Text(
                                text = stringResource(
                                    R.string.amount_negative_format,
                                    MoneyFormatter.format(item.monthlyExpense)
                                ),
                                style = AppTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = AppTheme.colors.error
                            )
                        }
                    }
                }
            }

            // Nút "Đặt làm ví chính" — chỉ xuất hiện khi chưa phải là ví chính
            if (!wallet.isActive) {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = PaddingValues(horizontal = AppTheme.dimensions.spaceMedium, vertical = AppTheme.dimensions.spaceSmall),
                        modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AppTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.width(AppTheme.dimensions.spaceSmall))
                        Text(
                            text = stringResource(R.string.set_as_active_wallet),
                            style = AppTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.primary
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferBottomSheet(
    state: AssetsUiState,
    onDismiss: () -> Unit,
    onFromWalletChange: (Long) -> Unit,
    onToWalletChange: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    _root_ide_package_.com.notepay.core.designsystem.component.effect.BottomSheetGlass(
        visible = state.isTransferSheetVisible,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTheme.dimensions.spaceExtraLarge, vertical = AppTheme.dimensions.spaceSmall)
                .padding(bottom = AppTheme.dimensions.spaceMassive)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.assets_transfer_dialog_title),
                    style = AppTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = AppTheme.colors.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // From Wallet selection
            Text(
                text = stringResource(R.string.assets_transfer_from_wallet),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(items = state.wallets, key = { it.wallet.id }) { item ->
                    val isSelected = item.wallet.id == state.transferFromWalletId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFromWalletChange(item.wallet.id) },
                        label = {
                            Text(
                                text = item.wallet.name,
                                style = AppTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = AppTheme.shapes.capsule,
                        modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.ink,
                            selectedLabelColor = AppTheme.colors.onInk,
                            containerColor = AppTheme.colors.surfaceSunken,
                            labelColor = AppTheme.colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = AppTheme.colors.separator,
                            selectedBorderColor = AppTheme.colors.ink,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // To Wallet selection
            Text(
                text = stringResource(R.string.assets_transfer_to_wallet),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(items = state.wallets, key = { it.wallet.id }) { item ->
                    val isSelected = item.wallet.id == state.transferToWalletId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToWalletChange(item.wallet.id) },
                        label = {
                            Text(
                                text = item.wallet.name,
                                style = AppTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        shape = AppTheme.shapes.capsule,
                        modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppTheme.colors.ink,
                            selectedLabelColor = AppTheme.colors.onInk,
                            containerColor = AppTheme.colors.surfaceSunken,
                            labelColor = AppTheme.colors.textSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = AppTheme.colors.separator,
                            selectedBorderColor = AppTheme.colors.ink,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // Amount input
            Text(
                text = stringResource(R.string.assets_transfer_amount),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            OutlinedTextField(
                value = state.transferAmountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
                placeholder = { Text(stringResource(R.string.ui_0), color = AppTheme.colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                visualTransformation = remember { VietnamCurrencyVisualTransformation() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.ink,
                    unfocusedBorderColor = AppTheme.colors.border,
                    cursorColor = AppTheme.colors.ink
                )
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // Note input
            Text(
                text = stringResource(R.string.assets_transfer_note),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            OutlinedTextField(
                value = state.transferNote,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
                placeholder = { Text(stringResource(R.string.assets_transfer_note_hint), color = AppTheme.colors.textMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.ink,
                    unfocusedBorderColor = AppTheme.colors.border,
                    cursorColor = AppTheme.colors.ink
                )
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

            Button(
                onClick = onConfirm,
                enabled = !state.isTransferSubmitting && state.transferAmountInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
                shape = AppTheme.shapes.capsule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.ink,
                    contentColor = AppTheme.colors.onInk,
                    disabledContainerColor = AppTheme.colors.surfaceSunken,
                    disabledContentColor = AppTheme.colors.textMuted
                )
            ) {
                if (state.isTransferSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppTheme.colors.onInk
                    )
                } else {
                    Text(
                        text = stringResource(R.string.assets_transfer_confirm_btn),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
