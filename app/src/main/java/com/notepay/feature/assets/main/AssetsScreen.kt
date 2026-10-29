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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyVerticalGrid(
                // Grid tự chia cột theo bề rộng khả dụng: 1 cột ở Compact, 2 cột khi đủ chỗ.
                columns = GridCells.Adaptive(minSize = AssetCardMinWidth),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp + contentGutter,
                    end = 16.dp + contentGutter,
                    top = 16.dp,
                    bottom = bottomClearance
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.assets_wallets_header),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(
                                R.string.assets_wallets_count_format,
                                state.wallets.size,
                                stringResource(R.string.wallets_count_label)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                .padding(20.dp)
        ) {
            // Dòng trên: Nhãn "Tổng tài sản ròng" + Badge xu hướng biến động
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.assets_total_balance),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.onInkMuted,
                    fontWeight = FontWeight.Medium,
                    // Nhường chỗ cho badge: không để nhãn đẩy badge tràn ra ngoài thẻ ở font scale 2.0.
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (changePercentage != 0f) {
                    val isPositive = changePercentage >= 0f
                    val trendColor = if (isPositive) AppTheme.colors.onInkIncome else AppTheme.colors.onInkExpense
                    Surface(
                        shape = AppTheme.shapes.capsule,
                        color = trendColor.copy(alpha = 0.18f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = trendColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val formattedPct = String.format(Locale.US, "%.1f", abs(changePercentage))
                            Text(
                                text = stringResource(
                                    if (isPositive) R.string.percent_change_positive else R.string.percent_change_negative,
                                    formattedPct
                                ),
                                style = MaterialTheme.typography.labelSmall,
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = null,
                                tint = AppTheme.colors.onInkMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.assets_balance_stable),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = AppTheme.colors.onInkMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Số tiền Hero: Inter + tnum để các chữ số không nhảy ngang khi số dư đổi
            Text(
                text = MoneyFormatter.format(totalNetWorth),
                style = AppTheme.typography.amountHero,
                color = AppTheme.colors.onInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Thanh phân bổ Segmented Bar
            if (allocationItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

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

                Spacer(modifier = Modifier.height(8.dp))

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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    topItems.forEach { item ->
                        val color = WalletUiHelper.getColor(item.colorKey)
                        val pct = (item.percentage * 100).roundToInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.onInkMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.percent_format, pct),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onInk
                            )
                        }
                    }

                    if (otherPct > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.onInkMuted)
                            )
                            Text(
                                text = stringResource(R.string.assets_allocation_other),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppTheme.colors.onInkMuted
                            )
                            Text(
                                text = stringResource(R.string.percent_format, otherPct),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppTheme.colors.onInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hai Action Buttons cân bằng phân cấp thị giác (Pill Shape, Touch Target 48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = AppTheme.shapes.capsule,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.assets_transfer_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Button(
                    onClick = onAddWallet,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                    shape = AppTheme.shapes.capsule,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.assets_add_wallet_btn),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
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
) {
    val wallet = item.wallet
    val iconVector = WalletUiHelper.getIcon(wallet.iconKey)
    val colorValue = WalletUiHelper.getColor(wallet.colorKey)

    val cardBorder = if (wallet.isActive) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    } else {
        BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner20)
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Hàng đầu: Icon ví | Cột thông tin ví & Badge | Cột số dư
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

                Spacer(modifier = Modifier.width(16.dp))

                // Cột thông tin ví: Tên ví trên dòng riêng, dòng phụ chứa Badge "Ví chính" và số giao dịch
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (wallet.isActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.wallet_badge_active),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.bullet_separator),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Text(
                            text = stringResource(
                                R.string.assets_transactions_count_format,
                                item.transactionCount,
                                stringResource(R.string.transactions_count_label)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Cột số dư
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = MoneyFormatter.format(item.balance),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Dòng thu chi tháng (chỉ hiển thị khi có phát sinh để loại bỏ clutter rườm rà)
            if (item.monthlyIncome.amountInCents > 0L || item.monthlyExpense.amountInCents > 0L) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = AppTheme.colors.success,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.amount_positive_format,
                                MoneyFormatter.format(item.monthlyIncome)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.success
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = AppTheme.colors.error,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.amount_negative_format,
                                MoneyFormatter.format(item.monthlyExpense)
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AppTheme.colors.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Row: Đặt ví chính (trái) | Chỉnh sửa ví (phải)
            // Hai hành động được tách biệt rõ ràng, mỗi bên có touch target ≥ 48dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Trái: Nút "Đặt làm ví chính" — chỉ hiện khi chưa là ví chính
                // Dùng icon Star (không phải Check) để khỏi nhầm với badge trạng thái
                if (!wallet.isActive) {
                    TextButton(
                        onClick = onSetDefault,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.set_as_active_wallet),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Ví đang là ví chính → không có action cần thực hiện,
                    // badge ở trên đã thông báo rõ trạng thái
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Phải: Nút Chỉnh sửa — TextButton với icon + nhãn, touch target đủ rộng
                // ChevronRight loại bỏ (card toàn thẻ đã clickable rồi, chevron gây nhầm lẫn)
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit_wallet_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.edit_wallet_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.assets_transfer_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // From Wallet selection
            Text(
                text = stringResource(R.string.assets_transfer_from_wallet),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            // LazyRow: cuộn ngang khi có nhiều ví, tránh tràn mép màn hình.
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // To Wallet selection
            Text(
                text = stringResource(R.string.assets_transfer_to_wallet),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            Text(
                text = stringResource(R.string.assets_transfer_amount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.transferAmountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(R.string.ui_0)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                visualTransformation = remember { VietnamCurrencyVisualTransformation() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Note input
            Text(
                text = stringResource(R.string.assets_transfer_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = state.transferNote,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(R.string.assets_transfer_note_hint)) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onConfirm,
                enabled = !state.isTransferSubmitting && state.transferAmountInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (state.isTransferSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = stringResource(R.string.assets_transfer_confirm_btn),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}
