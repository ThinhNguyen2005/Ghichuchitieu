package com.notepay.ui.component

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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter

@Composable
fun BalanceCard(
    walletName: String,
    balance: Money,
    onClick: (() -> Unit)? = null,
    budgetLimit: Money? = null,
    monthlyExpense: Money = Money.ZERO,
    modifier: Modifier = Modifier,
) {
    val isBudgetExceeded = budgetLimit != null && monthlyExpense.amountInCents > budgetLimit.amountInCents
    val progress = if (budgetLimit != null && budgetLimit.amountInCents > 0L) {
        (monthlyExpense.amountInCents.toFloat() / budgetLimit.amountInCents).coerceIn(0f, 1f)
    } else {
        0f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = walletName,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowDropDown,
                        contentDescription = "Đổi ví",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = MoneyFormatter.format(balance),
                style = MaterialTheme.typography.displayMedium,
            )

            if (budgetLimit != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ngân sách tháng",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${MoneyFormatter.format(monthlyExpense)} / ${MoneyFormatter.format(budgetLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = if (isBudgetExceeded) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                if (isBudgetExceeded) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Chi tiêu vượt quá hạn mức ví!",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
