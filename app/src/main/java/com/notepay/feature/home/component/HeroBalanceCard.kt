package com.notepay.feature.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.designsystem.theme.isAppDarkTheme
import com.notepay.core.designsystem.formatter.MoneyFormatter

/**
 * Thẻ số dư Hero — thẻ ink nguyên khối của v4 (DESIGN.md §6, `ink-balance-card`).
 *
 * Nền luôn lấy từ token `ink` của hệ theme, không dùng ảnh nền. Ở dark mode thẻ dùng `ink`
 * `#262626` kèm viền 1px `inkBorder` vì ink `#0B0B0B` trên nền `#141414` chỉ đạt 1.07:1.
 */
@Composable
internal fun HeroBalanceCard(
    label: String,
    balance: Money,
    income: Money,
    expense: Money,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner28,
        color = AppTheme.colors.ink,
        contentColor = AppTheme.colors.onInk,
        border = if (isAppDarkTheme()) BorderStroke(1.dp, AppTheme.colors.inkBorder) else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppTheme.colors.onInkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedContent(
                targetState = balance,
                label = "BalanceAnimation",
            ) { targetBalance ->
                Text(
                    text = MoneyFormatter.format(targetBalance),
                    style = AppTheme.typography.amountHero,
                    color = AppTheme.colors.onInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                CashFlowAmount(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    contentDescription = stringResource(R.string.home_income_month),
                    amount = income,
                    color = AppTheme.colors.onInkIncome,
                )
                CashFlowAmount(
                    icon = Icons.AutoMirrored.Rounded.TrendingDown,
                    contentDescription = stringResource(R.string.home_expense_month),
                    amount = expense,
                    color = AppTheme.colors.onInkExpense,
                )
            }
        }
    }
}

/** Một chiều dòng tiền trong thẻ ink: biểu tượng + số tiền, cả hai cùng màu `*-on-ink`. */
@Composable
private fun CashFlowAmount(
    icon: ImageVector,
    contentDescription: String,
    amount: Money,
    color: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = MoneyFormatter.format(amount),
            style = MaterialTheme.typography.labelLarge,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
