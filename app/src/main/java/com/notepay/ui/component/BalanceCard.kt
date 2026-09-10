package com.notepay.ui.component

import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.compose.foundation.Image
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Thẻ tổng số dư (Hero Balance Card) trên trang chủ.
 * Hỗ trợ:
 * - Ảnh nền tùy chỉnh cá nhân hóa có xử lý Vignette Scrim bảo đảm tương phản WCAG AA.
 * - Hiệu ứng cuộn số (Rolling Number Ticker).
 */
@Composable
fun BalanceCard(
    wallet: Wallet?,
    balance: Money,
    income: Money,
    expense: Money,
    backgroundImageUri: String? = null,
    onClick: (() -> Unit)? = null,
    onEditWallet: ((Long) -> Unit)? = null,
    onChangeBackground: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (wallet == null) return

    val bgBitmap = rememberLocalImageBitmap(backgroundImageUri)
    val hasCustomBg = bgBitmap != null

    val isLightTheme = !isSystemInDarkTheme()
    val defaultCardBgColor = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer
    val defaultBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    val primaryTextColor = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface
    val secondaryTextColor = if (hasCustomBg) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
    val dividerColor = if (hasCustomBg) Color.White.copy(alpha = 0.25f) else defaultBorderColor

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = if (hasCustomBg) Color.Transparent else defaultCardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 4.dp else 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 1. Background Image & Scrim Overlay (nếu có ảnh nền)
            if (bgBitmap != null) {
                Image(
                    bitmap = bgBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                // Lớp phủ Scrim Gradient đa tầng bảo vệ tương phản text (WCAG AA)
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.72f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // 2. Nội dung Thẻ
            Column(modifier = Modifier.padding(24.dp)) {
                // Top Row: Wallet Name & Dropdown Arrow + Wallet Icon / Add Background Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
                    ) {
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                        if (onClick != null) {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = stringResource(R.string.action_change_wallet),
                                modifier = Modifier.size(24.dp),
                                tint = primaryTextColor
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (onChangeBackground != null) {
                            IconButton(
                                onClick = onChangeBackground,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AddPhotoAlternate,
                                    contentDescription = stringResource(R.string.action_change_card_background),
                                    modifier = Modifier.size(20.dp),
                                    tint = primaryTextColor.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Icon(
                            imageVector = WalletUiHelper.getIcon(wallet.iconKey),
                            contentDescription = null,
                            modifier = Modifier
                                .size(28.dp)
                                .alpha(0.9f),
                            tint = primaryTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Middle: Available Balance với Rolling Number Ticker
                Text(
                    text = stringResource(R.string.balance_available),
                    style = MaterialTheme.typography.labelMedium,
                    color = secondaryTextColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                RollingNumberTicker(
                    text = MoneyFormatter.format(balance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp
                    ),
                    color = primaryTextColor
                )

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = dividerColor)
                Spacer(modifier = Modifier.height(16.dp))

                // Cashflow Row (Income & Expense)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income Column
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = null,
                                tint = if (hasCustomBg) Color(0xFF81C784) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.stats_income),
                                style = MaterialTheme.typography.labelMedium,
                                color = secondaryTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RollingNumberTicker(
                            text = MoneyFormatter.format(income),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasCustomBg) Color(0xFF81C784) else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(dividerColor)
                    )

                    // Expense Column
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = null,
                                tint = if (hasCustomBg) Color(0xFFE57373) else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(R.string.stats_expense),
                                style = MaterialTheme.typography.labelMedium,
                                color = secondaryTextColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        RollingNumberTicker(
                            text = MoneyFormatter.format(expense),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (hasCustomBg) Color(0xFFE57373) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Budget Limit Progress (nếu có)
                val budgetLimit = wallet.budgetLimit
                if (budgetLimit != null && budgetLimit.amountInCents > 0L) {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = dividerColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    val spentAmount = expense.amountInCents
                    val limitAmount = budgetLimit.amountInCents
                    val progress = (spentAmount.toFloat() / limitAmount.toFloat()).coerceIn(0f, 1f)
                    val isExceeded = spentAmount > limitAmount
                    val progressColor = when {
                        isExceeded -> if (hasCustomBg) Color(0xFFE57373) else MaterialTheme.colorScheme.error
                        progress > 0.8f -> if (hasCustomBg) Color(0xFFFFB74D) else Color(0xFFFF9800)
                        else -> if (hasCustomBg) Color(0xFF81C784) else MaterialTheme.colorScheme.primary
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
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isExceeded) progressColor else secondaryTextColor,
                            fontWeight = if (isExceeded) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = stringResource(
                                R.string.budget_progress_format,
                                (progress * 100).toInt(),
                                MoneyFormatter.format(expense),
                                MoneyFormatter.format(budgetLimit),
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor,
                            fontWeight = FontWeight.Medium
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
                        trackColor = if (hasCustomBg) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round
                    )
                }

                // Nút chỉnh sửa ví (nếu có callback)
                if (onEditWallet != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditWallet(wallet.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.content_description_edit_wallet),
                            tint = secondaryTextColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.home_edit_wallet),
                            style = MaterialTheme.typography.labelSmall,
                            color = secondaryTextColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tải ảnh bất đồng bộ từ Content Uri / File Uri một cách an toàn.
 */
@Composable
private fun rememberLocalImageBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    if (uriString.isNullOrBlank()) return null

    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = withContext(Dispatchers.IO) {
            try {
                val uri = uriString.toUri()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    return bitmapState.value
}
