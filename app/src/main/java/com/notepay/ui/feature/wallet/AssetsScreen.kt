package com.notepay.ui.feature.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
import com.notepay.ui.component.WalletAppIcon
import com.notepay.domain.money.Money
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.feature.debt.DebtSummaryCard
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 120.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                    AssetsHeader(
                        totalNetWorth = state.totalNetWorth,
                        changePercentage = state.netWorthChangePercentage,
                        onAddWallet = onAddWallet,
                        onTransferClick = { viewModel.openTransferSheet() }
                    )
                }

                // Biểu đồ phân bổ tài sản theo ví (Donut Chart chuẩn UX trang Thống kê)
                item {
                    WalletAllocationChartCard(
                        allocationItems = state.allocationItems,
                        totalNetWorth = state.totalNetWorth,
                    )
                }

                // Thẻ tóm tắt Sổ nợ cá nhân (Personal Debts Summary)
                val summary = state.debtSummary
                if (summary != null && (summary.activeDebtsCount > 0 || summary.totalToCollect.amountInCents > 0L || summary.totalToPay.amountInCents > 0L)) {
                    item {
                        DebtSummaryCard(
                            summary = summary,
                            onClick = onNavigateToDebtManagement,
                        )
                    }
                }

                // Header danh sách ví
                item {
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
private fun AssetsHeader(
    totalNetWorth: Money,
    changePercentage: Float,
    onAddWallet: () -> Unit,
    onTransferClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.assets_total_balance),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    if (changePercentage != 0f) {
                        val isPositive = changePercentage >= 0f
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPositive) Color(0xFF34C759).copy(alpha = 0.15f) else Color(0xFFFF3B30).copy(alpha = 0.15f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPositive) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = if (isPositive) Color(0xFF34C759) else Color(0xFFFF3B30),
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
                                    color = if (isPositive) Color(0xFF34C759) else Color(0xFFFF3B30)
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.assets_balance_stable),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = MoneyFormatter.format(totalNetWorth),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Hai Action Buttons cân bằng phân cấp thị giác (Pill Shape, Touch Target 46dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = onTransferClick,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.CompareArrows,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.assets_transfer_btn),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }

                    FilledTonalButton(
                        onClick = onAddWallet,
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.assets_add_wallet_btn),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletAllocationChartCard(
    allocationItems: List<WalletAllocationItem>,
    totalNetWorth: Money,
) {
    var selectedItem by remember(allocationItems) {
        mutableStateOf<WalletAllocationItem?>(null)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.assets_allocation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedItem != null) {
                    TextButton(
                        onClick = { selectedItem = null },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 32.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.wallet_all),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (allocationItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.assets_allocation_no_balance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart bên trái (Box cân bằng)
                    Box(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(allocationItems) {
                                    detectTapGestures { offset ->
                                        val tapped = findWalletAllocationItem(
                                            tap = offset,
                                            width = size.width.toFloat(),
                                            height = size.height.toFloat(),
                                            strokePx = 34.dp.toPx(),
                                            items = allocationItems
                                        )
                                        selectedItem = if (tapped?.walletId == selectedItem?.walletId) null else tapped
                                    }
                                }
                        ) {
                            val diameter = min(size.width, size.height) * 0.76f
                            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                            var startAngle = 270f

                            allocationItems.forEach { item ->
                                val sweep = item.percentage * 360f
                                val isSelected = item.walletId == selectedItem?.walletId
                                val color = WalletUiHelper.getColor(item.colorKey)

                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    topLeft = topLeft,
                                    size = Size(diameter, diameter),
                                    style = Stroke(
                                        width = if (isSelected) 38.dp.toPx() else 30.dp.toPx(),
                                        cap = StrokeCap.Butt
                                    )
                                )
                                startAngle += sweep
                            }
                        }

                        WalletAllocationCenter(
                            item = selectedItem,
                            totalNetWorth = totalNetWorth
                        )
                    }

                    // Danh sách Legend ví bên phải
                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                    ) {
                        allocationItems.forEach { item ->
                            val isSelected = item.walletId == selectedItem?.walletId
                            val color = WalletUiHelper.getColor(item.colorKey)
                            val pct = (item.percentage * 100).roundToInt()

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(AppTheme.shapes.corner12)
                                    .clickable {
                                        selectedItem = if (isSelected) null else item
                                    },
                                shape = AppTheme.shapes.corner12,
                                color = color.copy(alpha = if (isSelected) 0.20f else 0.08f),
                                border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.6f)) else null
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.percent_format, pct),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletAllocationCenter(
    item: WalletAllocationItem?,
    totalNetWorth: Money,
) {
    val tint = item?.let { WalletUiHelper.getColor(it.colorKey) } ?: MaterialTheme.colorScheme.primary
    val bg = if (item != null) {
        tint.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        shape = CircleShape,
        color = bg
    ) {
        Column(
            modifier = Modifier
                .size(102.dp)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item?.name ?: stringResource(R.string.assets_total_balance),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (item != null) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = MoneyFormatter.format(item?.balance ?: totalNetWorth),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (item != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = tint.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = stringResource(R.string.percent_format, (item.percentage * 100).roundToInt()),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tint,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun findWalletAllocationItem(
    tap: Offset,
    width: Float,
    height: Float,
    strokePx: Float,
    items: List<WalletAllocationItem>
): WalletAllocationItem? {
    if (items.isEmpty()) return null
    val diameter = min(width, height) * 0.76f
    val radius = diameter / 2f
    val center = Offset(width / 2f, height / 2f)
    val dx = tap.x - center.x
    val dy = tap.y - center.y
    val dist = sqrt(dx * dx + dy * dy)
    if (dist !in (radius - strokePx * 0.75f)..(radius + strokePx * 0.75f)) return null
    val angle = ((atan2(dy, dx) * 180f / PI.toFloat()) + 360f) % 360f
    var start = 270f
    items.forEach { item ->
        val sweep = item.percentage * 360f
        val end = (start + sweep) % 360f
        if (if (start <= end) angle in start..end else angle >= start || angle <= end) return item
        start = end
    }
    return null
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        .size(46.dp)
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

                Spacer(modifier = Modifier.width(14.dp))

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
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                        softWrap = false
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.bullet_separator),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = stringResource(
                                R.string.assets_transactions_count_format,
                                item.transactionCount,
                                stringResource(R.string.transactions_count_label)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
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
                            color = Color(0xFF34C759)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
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
                            color = Color(0xFFFF3B30)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Row: Đặt ví chính (trái) | Chỉnh sửa ví (phải)
            // Hai hành động được tách biệt rõ ràng, mỗi bên có touch target ≥ 44dp
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Star,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(5.dp))
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
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit_wallet_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
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
    com.notepay.ui.component.BottomSheetGlass(
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
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.wallets.forEach { item ->
                    val isSelected = item.wallet.id == state.transferFromWalletId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onFromWalletChange(item.wallet.id) },
                        label = { Text(item.wallet.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // To Wallet selection
            Text(
                text = stringResource(R.string.assets_transfer_to_wallet),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.wallets.forEach { item ->
                    val isSelected = item.wallet.id == state.transferToWalletId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToWalletChange(item.wallet.id) },
                        label = { Text(item.wallet.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount input
            Text(
                text = stringResource(R.string.assets_transfer_amount),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = state.transferAmountInput,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text(stringResource(R.string.ui_0)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Note input
            Text(
                text = stringResource(R.string.assets_transfer_note),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
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
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
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
