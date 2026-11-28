package com.notepay.feature.home.component

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeHeroCardDarkBottom
import com.notepay.core.designsystem.theme.HomeHeroCardDarkRim
import com.notepay.core.designsystem.theme.HomeHeroCardDarkTop
import com.notepay.core.designsystem.theme.HomeHeroCardLightBottom
import com.notepay.core.designsystem.theme.HomeHeroCardLightRim
import com.notepay.core.designsystem.theme.HomeHeroCardLightTop
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.domain.money.Money

/**
 * Luminous iOS-elevated balance card for HomeScreen with dual-layer soft drop shadow,
 * privacy masking control, and inset monthly cashflow metric tiles.
 */
@Composable
internal fun HeroBalanceCard(
    label: String,
    balance: Money,
    income: Money,
    expense: Money,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
    val performHaptic = rememberNotePayHaptic()
    val isLargeFont = LocalDensity.current.fontScale >= 1.45f
    val cardShape = AppTheme.shapes.corner28

    val cardGradient = remember(isLight) {
        val top = if (isLight) HomeHeroCardLightTop else HomeHeroCardDarkTop
        val bottom = if (isLight) HomeHeroCardLightBottom else HomeHeroCardDarkBottom
        Brush.verticalGradient(listOf(top, bottom))
    }
    val rimColor = if (isLight) HomeHeroCardLightRim else HomeHeroCardDarkRim
    val incomeAmount = if (isBalanceHidden) MoneyFormatter.MASK else "+${MoneyFormatter.format(income)}"
    val expenseAmount = if (isBalanceHidden) MoneyFormatter.MASK else "−${MoneyFormatter.format(expense)}"
    val visibilityCd = stringResource(
        if (isBalanceHidden) R.string.home_show_balance else R.string.home_hide_balance
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = true),
        shape = cardShape,
        color = Color.Transparent,
        contentColor = colors.textPrimary,
        border = BorderStroke(1.dp, rimColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(start = 20.dp, top = 14.dp, end = 12.dp, bottom = 20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onToggleBalanceHidden()
                    },
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                ) {
                    Icon(
                        imageVector = if (isBalanceHidden) {
                            Icons.Rounded.VisibilityOff
                        } else {
                            Icons.Rounded.Visibility
                        },
                        contentDescription = visibilityCd,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            NotePayAnimatedContent(
                targetState = if (isBalanceHidden) null else balance,
                label = "BalanceAnimation",
                modifier = Modifier.padding(end = 8.dp),
            ) { visibleBalance ->
                Text(
                    text = if (visibleBalance == null) {
                        MoneyFormatter.MASK
                    } else {
                        MoneyFormatter.format(visibleBalance)
                    },
                    style = AppTheme.typography.amountHero,
                    color = colors.textPrimary,
                    maxLines = if (isLargeFont) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(
                modifier = Modifier.padding(end = 8.dp),
                thickness = 0.5.dp,
                color = colors.hairline,
            )

            Spacer(modifier = Modifier.height(14.dp))

            val incomeLabel = stringResource(R.string.home_income_month)
            val expenseLabel = stringResource(R.string.home_expense_month)
            val incomeTintAlpha = if (isLight) 0.10f else 0.16f
            val expenseTintAlpha = if (isLight) 0.09f else 0.15f

            if (isLargeFont) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CashflowMetricTile(
                        label = incomeLabel,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = colors.success,
                        containerAlpha = incomeTintAlpha,
                        amountText = incomeAmount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CashflowMetricTile(
                        label = expenseLabel,
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        accentColor = colors.error,
                        containerAlpha = expenseTintAlpha,
                        amountText = expenseAmount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CashflowMetricTile(
                        label = incomeLabel,
                        icon = Icons.AutoMirrored.Rounded.TrendingUp,
                        accentColor = colors.success,
                        containerAlpha = incomeTintAlpha,
                        amountText = incomeAmount,
                        modifier = Modifier.weight(1f),
                    )
                    CashflowMetricTile(
                        label = expenseLabel,
                        icon = Icons.AutoMirrored.Rounded.TrendingDown,
                        accentColor = colors.error,
                        containerAlpha = expenseTintAlpha,
                        amountText = expenseAmount,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CashflowMetricTile(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    containerAlpha: Float,
    amountText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner16,
        color = accentColor.copy(alpha = containerAlpha),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(AppTheme.shapes.circle)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = amountText,
                style = AppTheme.typography.amountRow,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
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
            isBalanceHidden = false,
            onToggleBalanceHidden = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
