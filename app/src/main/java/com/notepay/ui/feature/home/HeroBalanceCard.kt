package com.notepay.ui.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.MoneyFormatter
import java.io.File

/**
 * Thẻ số dư Hero + Capsule dòng tiền tháng này (Cashflow Summary).
 *
 * Đây là phiên bản iOS-style thay thế `component/BalanceCard` cũ: cùng một layout cho cả hai
 * trạng thái, chỉ lớp nền (ảnh + scrim) và bảng màu nội dung thay đổi theo việc ví có ảnh nền
 * tuỳ chỉnh hay không.
 */
@Composable
internal fun HeroBalanceCard(
    label: String,
    modifier: Modifier = Modifier,
    balance: Money,
    income: Money,
    expense: Money,
    backgroundUri: String? = null,
    isSpecificWallet: Boolean = false,
    onChangeBackground: (() -> Unit)? = null,
) {
    val hasCustomBg = !backgroundUri.isNullOrBlank()

    val contentColor = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface
    val labelColor = if (hasCustomBg) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant
    val labelShadow = if (hasCustomBg) Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 1.5f), 4f) else null
    val balanceShadow = if (hasCustomBg) Shadow(Color.Black.copy(alpha = 0.9f), Offset(0f, 2f), 8f) else null
    val capsuleColor = if (hasCustomBg) Color.Black.copy(alpha = 0.42f) else AppTheme.colors.surfaceSunken
    val capsuleBorder = if (hasCustomBg) {
        BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
    } else {
        BorderStroke(1.dp, AppTheme.colors.border.copy(alpha = 0.5f))
    }
    val dividerColor = if (hasCustomBg) Color.White.copy(alpha = 0.3f) else AppTheme.colors.border
    val incomeTextColor = if (hasCustomBg) Color.White else AppTheme.colors.success
    val expenseTextColor = if (hasCustomBg) Color.White else AppTheme.colors.error

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        // Không có ảnh nền: dùng nền `surface` + shadow nhẹ như spec balance-hero-card.
        colors = CardDefaults.cardColors(
            containerColor = if (hasCustomBg) Color.Transparent else AppTheme.colors.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (hasCustomBg) 4.dp else 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (hasCustomBg) {
                val imageModel = remember(backgroundUri) {
                    if (backgroundUri.startsWith("/")) File(backgroundUri) else backgroundUri
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // Multi-layer Scrim Gradient bảo vệ độ tương phản WCAG AA
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.55f),
                                    0.30f to Color.Black.copy(alpha = 0.40f),
                                    0.70f to Color.Black.copy(alpha = 0.70f),
                                    1.0f to Color.Black.copy(alpha = 0.90f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(shadow = labelShadow),
                        color = labelColor,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (isSpecificWallet && onChangeBackground != null) {
                        // Vùng chạm 48dp: icon 20dp nằm giữa, đạt chuẩn Material touch target.
                        IconButton(
                            onClick = onChangeBackground,
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.action_change_card_background),
                                tint = contentColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedContent(
                    targetState = balance,
                    label = "BalanceAnimation",
                ) { targetBalance ->
                    Text(
                        text = MoneyFormatter.format(targetBalance),
                        style = MaterialTheme.typography.headlineLarge.copy(shadow = balanceShadow),
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Capsule dòng tiền (Thu / Chi tháng này)
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = capsuleColor,
                    border = capsuleBorder,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Thu nhập
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                contentDescription = stringResource(R.string.home_income_month),
                                tint = AppTheme.colors.success,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "+${MoneyFormatter.format(income)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = incomeTextColor,
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(12.dp)
                                .background(dividerColor),
                        )

                        // Chi tiêu
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                                contentDescription = stringResource(R.string.home_expense_month),
                                tint = AppTheme.colors.error,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = "-${MoneyFormatter.format(expense)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = expenseTextColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HeroBalanceCardPreview() {
    NotePayTheme {
        HeroBalanceCard(
            label = "Tổng số dư tất cả ví",
            balance = Money(15_000_000_00),
            income = Money(5_000_000_00),
            expense = Money(3_500_000_00),
            modifier = Modifier.padding(16.dp),
        )
    }
}
