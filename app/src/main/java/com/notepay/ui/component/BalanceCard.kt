package com.notepay.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.ui.feature.home.WalletSummary
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.isAppDarkTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

/**
 * Thẻ số dư đa ví (Hero Balance Carousel) hỗ trợ trượt ngang HorizontalPager.
 * - Đồng bộ hai chiều với activeWalletId qua settledPage.
 * - Animated Dots Indicator dạng capsule.
 * - Lớp phủ kính đa tầng Multi-layer Scrim + Vignette.
 * - Loại bỏ hoàn toàn nút chỉnh sửa (đã có bên màn Tài sản) và dialog cũ.
 */
@Composable
fun BalanceCard(
    walletsSummary: List<WalletSummary>,
    activeWalletId: Long?,
    modifier: Modifier = Modifier,
    onWalletChanged: (Long) -> Unit,
    onOpenWalletPicker: (() -> Unit)? = null,
    onChangeBackground: ((Long) -> Unit)? = null,
) {
    if (walletsSummary.isEmpty()) return

    val currentWallets by rememberUpdatedState(walletsSummary)
    val currentActiveId by rememberUpdatedState(activeWalletId)

    val initialIndex = remember(walletsSummary) {
        val found = walletsSummary.indexOfFirst { it.wallet.id == activeWalletId }
        if (found >= 0) found else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { walletsSummary.size }
    )

    // Chiều ngược: state bên ngoài đổi -> pager cuộn theo
    LaunchedEffect(activeWalletId) {
        val target = currentWallets.indexOfFirst { it.wallet.id == activeWalletId }
        if (target >= 0 && target != pagerState.settledPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    // Chiều xuôi: người dùng vuốt xong -> báo ra ngoài
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val id = currentWallets.getOrNull(page)?.wallet?.id
                if (id != null && id != currentActiveId) {
                    onWalletChanged(id)
                }
            }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> walletsSummary.getOrNull(page)?.wallet?.id ?: page.toLong() },
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) { page ->
            val summary = walletsSummary[page]
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val cardAlpha = lerp(0.88f, 1.0f, 1f - pageOffset.coerceIn(0f, 1f))

            WalletCardItem(
                summary = summary,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = cardAlpha
                    }
                    .semantics {
                        stateDescription = "Ví ${page + 1} trên ${walletsSummary.size}: ${summary.wallet.name}"
                    },
                showPickerAffordance = walletsSummary.size > 1,
                onOpenWalletPicker = onOpenWalletPicker,
                onChangeBackground = onChangeBackground?.let { callback ->
                    { callback(summary.wallet.id) }
                }
            )
        }

        // Animated Dots Indicator (chỉ hiện khi có từ 2 ví trở lên)
        if (walletsSummary.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                repeat(walletsSummary.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 6.dp,
                        label = "dotWidth"
                    )
                    val dotColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                    }

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletCardItem(
    summary: WalletSummary,
    modifier: Modifier = Modifier,
    showPickerAffordance: Boolean = false,
    onOpenWalletPicker: (() -> Unit)? = null,
    onChangeBackground: (() -> Unit)? = null,
) {
    val wallet = summary.wallet
    val balance = summary.balance
    val income = summary.monthlyIncome
    val expense = summary.monthlyExpense
    val backgroundImageUri = summary.backgroundUri

    val hasCustomBg = !backgroundImageUri.isNullOrBlank()
    val isLightTheme = !isAppDarkTheme()
    val defaultCardBgColor = if (isLightTheme) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainer
    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val primaryColor = MaterialTheme.colorScheme.primary
    val cardShape = if (hasCustomBg) AppTheme.shapes.corner24 else AppTheme.shapes.card

    val primaryTextColor = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (hasCustomBg) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val textShadow = if (hasCustomBg) {
        Shadow(
            color = Color.Black.copy(alpha = 0.90f),
            offset = Offset(0f, 2.5f),
            blurRadius = 8f
        )
    } else null

    val labelShadow = if (hasCustomBg) {
        Shadow(
            color = Color.Black.copy(alpha = 0.80f),
            offset = Offset(0f, 1.5f),
            blurRadius = 4f
        )
    } else null

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (hasCustomBg) Color.Transparent else defaultCardBgColor
        ),
        border = if (!hasCustomBg) BorderStroke(1.dp, defaultBorderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!hasCustomBg) {
                        Modifier.drawBehind {
                            drawRect(
                                color = primaryColor,
                                topLeft = Offset.Zero,
                                size = Size(3.5f * density, size.height)
                            )
                        }
                    } else Modifier
                )
        ) {
            // 1. Lớp Ảnh nền + Multi-layer Scrim Gradient
            if (hasCustomBg) {
                val imageModel = remember(backgroundImageUri) {
                    if (backgroundImageUri.startsWith("/")) {
                        java.io.File(backgroundImageUri)
                    } else {
                        backgroundImageUri
                    }
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                // Lớp 1: Scrim gradient dọc nhiều tầng bảo vệ độ tương phản mọi vị trí
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.55f),
                                    0.28f to Color.Black.copy(alpha = 0.38f),
                                    0.55f to Color.Black.copy(alpha = 0.62f),
                                    0.82f to Color.Black.copy(alpha = 0.82f),
                                    1.0f to Color.Black.copy(alpha = 0.92f)
                                )
                            )
                        )
                )

                // Lớp 2: Radial Vignette mờ nhẹ 4 góc tạo chiều sâu điện ảnh
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.35f)
                                )
                            )
                        )
                )
            }

            // 2. Nội dung Thẻ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Row: Wallet Name Pill Selector + Change Background
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill Badge cho bộ chọn ví (hỗ trợ mở nhanh sheet nếu có nhiều ví)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (hasCustomBg) Color.Black.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = BorderStroke(
                            1.dp,
                            if (hasCustomBg) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.clip(RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .then(
                                    if (onOpenWalletPicker != null) {
                                        Modifier.clickable { onOpenWalletPicker() }
                                    } else Modifier
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = WalletUiHelper.getIcon(wallet.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = primaryTextColor
                            )
                            Text(
                                text = wallet.name,
                                style = MaterialTheme.typography.titleSmall.copy(shadow = labelShadow),
                                fontWeight = FontWeight.Bold,
                                color = primaryTextColor
                            )
                            if (showPickerAffordance && onOpenWalletPicker != null) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = stringResource(R.string.action_change_wallet),
                                    modifier = Modifier.size(20.dp),
                                    tint = primaryTextColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Action Button bên phải: Chỉ còn nút đổi ảnh nền (đã bỏ hoàn toàn nút chỉnh sửa)
                    if (onChangeBackground != null) {
                        Surface(
                            shape = CircleShape,
                            color = if (hasCustomBg) Color.Black.copy(alpha = 0.42f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                1.dp,
                                if (hasCustomBg) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(
                                modifier = Modifier.clickable { onChangeBackground() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.action_change_card_background),
                                    modifier = Modifier.size(18.dp),
                                    tint = primaryTextColor.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Middle: Available Balance với Rolling Number Ticker & Shadow
                Text(
                    text = stringResource(R.string.balance_available),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.5.sp,
                        shadow = labelShadow
                    ),
                    color = secondaryTextColor,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                RollingNumberTicker(
                    text = MoneyFormatter.format(balance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp,
                        shadow = textShadow
                    ),
                    color = primaryTextColor
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Frosted Glass Capsule cho Cụm Thu nhập & Chi tiêu
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (hasCustomBg) Color.Black.copy(alpha = 0.48f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (hasCustomBg) Color.White.copy(alpha = 0.16f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Cột Thu nhập
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(AppTheme.colors.success.copy(alpha = if (hasCustomBg) 0.25f else 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = if (hasCustomBg) Color(0xFF4ADE80) else AppTheme.colors.success,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.stats_income),
                                    style = MaterialTheme.typography.labelSmall.copy(shadow = labelShadow),
                                    color = secondaryTextColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                RollingNumberTicker(
                                    text = MoneyFormatter.format(income),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = labelShadow
                                    ),
                                    color = if (hasCustomBg) Color(0xFF4ADE80) else AppTheme.colors.success
                                )
                            }
                        }

                        // Vạch phân cách kính dọc
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(1.dp)
                                .background(
                                    if (hasCustomBg) Color.White.copy(alpha = 0.18f)
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )
                        )

                        // Cột Chi tiêu
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = if (hasCustomBg) 0.25f else 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                    contentDescription = null,
                                    tint = if (hasCustomBg) Color(0xFFF87171) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.stats_expense),
                                    style = MaterialTheme.typography.labelSmall.copy(shadow = labelShadow),
                                    color = secondaryTextColor,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                RollingNumberTicker(
                                    text = MoneyFormatter.format(expense),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        shadow = labelShadow
                                    ),
                                    color = if (hasCustomBg) Color(0xFFF87171) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // 4. Budget Limit Progress (nếu có hạn mức)
                val budgetLimit = wallet.budgetLimit
                if (budgetLimit != null && budgetLimit.amountInCents > 0L) {
                    Spacer(modifier = Modifier.height(14.dp))

                    val spentAmount = expense.amountInCents
                    val limitAmount = budgetLimit.amountInCents
                    val progress = (spentAmount.toFloat() / limitAmount.toFloat()).coerceIn(0f, 1f)
                    val isExceeded = spentAmount > limitAmount
                    val progressColor = when {
                        isExceeded -> if (hasCustomBg) Color(0xFFF87171) else MaterialTheme.colorScheme.error
                        progress > 0.8f -> if (hasCustomBg) Color(0xFFFB923C) else Color(0xFFFF9800)
                        else -> if (hasCustomBg) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                if (isExceeded) R.string.budget_spending_exceeded else R.string.budget_monthly_limit,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(shadow = labelShadow),
                            color = if (isExceeded) progressColor else secondaryTextColor,
                            fontWeight = if (isExceeded) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            text = stringResource(
                                R.string.budget_progress_format,
                                (progress * 100).toInt(),
                                MoneyFormatter.format(expense),
                                MoneyFormatter.format(budgetLimit),
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(shadow = labelShadow),
                            color = secondaryTextColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(AppTheme.shapes.circle),
                        color = progressColor,
                        trackColor = if (hasCustomBg) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
