package com.notepay.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper

/**
 * Thẻ tổng số dư (Hero Balance Card) trên trang chủ.
 * Hỗ trợ:
 * - Ảnh nền tùy chỉnh cá nhân hóa có xử lý Multi-layer Scrim + Vignette bảo đảm tương phản WCAG AA.
 * - Text Shadow bảo đảm số dư luôn nổi bật trên bất kỳ chi tiết ảnh nền nào.
 * - Pill Selector cao cấp cho bộ chọn ví và Glass Circle cho nút đổi ảnh nền.
 * - Frosted Glass Capsule cho cụm Thu nhập & Chi tiêu.
 * - Hiệu ứng cuộn số (Rolling Number Ticker).
 */
@Composable
fun BalanceCard(
    modifier: Modifier = Modifier,
    wallet: Wallet?,
    balance: Money,
    income: Money,
    expense: Money,
    backgroundImageUri: String? = null,
    onClick: (() -> Unit)? = null,
    onEditWallet: ((Long) -> Unit)? = null,
    onChangeBackground: (() -> Unit)? = null,
) {
    if (wallet == null) return

    val hasCustomBg = !backgroundImageUri.isNullOrBlank()
    val isLightTheme = !isSystemInDarkTheme()
    val defaultCardBgColor = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer
    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

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
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = if (hasCustomBg) Color.Transparent else defaultCardBgColor
        ),
        border = if (!hasCustomBg) BorderStroke(1.dp, defaultBorderColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 6.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Lớp Ảnh nền + Multi-layer Scrim Gradient
            if (hasCustomBg) {
                val imageModel = remember(backgroundImageUri) {
                    if (backgroundImageUri?.startsWith("/") == true) {
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
                // Top Row: Wallet Name Pill Selector + Change Background & Wallet Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pill Badge cho bộ chọn ví
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
                                .clickable(enabled = onClick != null) { onClick?.invoke() }
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
                            if (onClick != null) {
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = stringResource(R.string.action_change_wallet),
                                    modifier = Modifier.size(20.dp),
                                    tint = primaryTextColor.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // Action Buttons bên phải: Đổi ảnh nền & Nút Chỉnh sửa ví (nếu có)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    modifier = Modifier
                                        .clickable { onChangeBackground() },
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

                        if (onEditWallet != null) {
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
                                    modifier = Modifier
                                        .clickable { onEditWallet(wallet.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Edit,
                                        contentDescription = stringResource(R.string.action_edit_wallet),
                                        modifier = Modifier.size(16.dp),
                                        tint = primaryTextColor.copy(alpha = 0.9f)
                                    )
                                }
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
                                    .background(Color(0xFF22C55E).copy(alpha = if (hasCustomBg) 0.25f else 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                    contentDescription = null,
                                    tint = if (hasCustomBg) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary,
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
                                    color = if (hasCustomBg) Color(0xFF4ADE80) else MaterialTheme.colorScheme.primary
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
                                    .background(Color(0xFFEF4444).copy(alpha = if (hasCustomBg) 0.25f else 0.15f)),
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

