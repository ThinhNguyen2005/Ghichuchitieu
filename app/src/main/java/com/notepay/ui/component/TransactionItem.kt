package com.notepay.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItem(
    transaction: Transaction,
    walletName: String = "",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val amountColor = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val sign = if (isIncome) "+" else "−"

    val systemTz = TimeZone.currentSystemDefault()
    val localDateTime = transaction.occurredAt.toLocalDateTime(systemTz)
    val timeStr = String.format("%02d:%02d", localDateTime.hour, localDateTime.minute)

    val subtitleParts = listOfNotNull(
        timeStr,
        transaction.category.displayName,
        walletName.takeIf { it.isNotBlank() }
    )
    val subtitleText = subtitleParts.joinToString(" • ")

    val clickModifier = if (onClick != null || onLongClick != null) {
        Modifier.combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() }
        )
    } else {
        Modifier
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Avatar danh mục với badge ngân hàng nếu auto-capture
            Box {
                CategoryAvatar(
                    category = transaction.category,
                    size = 40.dp,
                    iconSize = 18.dp
                )
                if (transaction.isAutoCapture) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.AccountBalance,
                            contentDescription = "Tự động từ ngân hàng",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp),
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, end = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = transaction.note.ifBlank { transaction.category.displayName },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = "$sign${MoneyFormatter.format(transaction.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor,
                    )
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

