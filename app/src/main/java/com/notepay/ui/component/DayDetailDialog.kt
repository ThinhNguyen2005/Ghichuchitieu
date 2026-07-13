package com.notepay.ui.component

import com.notepay.ui.theme.AppTheme

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.notepay.R
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Dialog chi tiết một ngày, dùng chung cho:
 *  - Trang Nhắc nhở (subscription đến hạn + giao dịch nếu có).
 *  - Trang Danh sách (chỉ giao dịch).
 *
 * Nếu danh sách rỗng sẽ hiện thông báo trống.
 */
@Composable
fun DayDetailDialog(
    date: LocalDate,
    transactions: List<Transaction> = emptyList(),
    subscriptions: List<Subscription> = emptyList(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(TimeZone.currentSystemDefault()).date }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            // P2-13: title có "Thứ X," phía trước + subtitle tương đối.
            val weekday = when (date.dayOfWeek.ordinal) {
                0 -> context.getString(R.string.day_monday)
                1 -> context.getString(R.string.day_tuesday)
                2 -> context.getString(R.string.day_wednesday)
                3 -> context.getString(R.string.day_thursday)
                4 -> context.getString(R.string.day_friday)
                5 -> context.getString(R.string.day_saturday)
                6 -> context.getString(R.string.day_sunday)
                else -> ""
            }
            val diffDays = date.toEpochDays().toLong() - today.toEpochDays().toLong()
            val relative = when {
                diffDays == 0L -> context.getString(R.string.date_today)
                diffDays > 0L -> context.getString(R.string.date_days_remaining_format, diffDays)
                else -> context.getString(R.string.date_days_ago_format, -diffDays)
            }
            Column {
                Text(
                    text = "$weekday, ${date.dayOfMonth}/${date.monthNumber}/${date.year}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = relative,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (subscriptions.isEmpty() && transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(stringResource(R.string.day_detail_empty_desc))
                    }
                }

                if (subscriptions.isNotEmpty()) {
                    Text(
                        stringResource(R.string.subscription_reminders_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    subscriptions.forEach { sub ->
                        val daysLeft = (sub.nextDueDate - now).inWholeDays
                        val isExpired = daysLeft < 0
                        val isUrgent = daysLeft in 0..3
                        val containerColor = when {
                            isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            isUrgent -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = containerColor),
                            shape = AppTheme.shapes.corner12,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(AppTheme.shapes.circle)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        sub.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        MoneyFormatter.format(sub.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isExpired) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                    )
                                    val dueLabel = when {
                                        isExpired -> context.getString(R.string.subscription_overdue_format, -daysLeft)
                                        daysLeft == 0L -> context.getString(R.string.subscription_due_today)
                                        daysLeft == 1L -> context.getString(R.string.subscription_due_one_day)
                                        else -> context.getString(R.string.subscription_due_days_format, daysLeft)
                                    }
                                    Text(
                                        dueLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExpired || daysLeft == 0L) Color(0xFFB71C1C) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                if (transactions.isNotEmpty()) {
                    if (subscriptions.isNotEmpty()) {
                        HorizontalDivider()
                    }
                    Text(
                        stringResource(R.string.day_detail_transactions_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            CategoryAvatar(
                                category = tx.category,
                                size = 32.dp,
                                iconSize = 16.dp,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    tx.note.ifBlank { tx.category.displayName },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    tx.category.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            val isIncome = tx.type == TransactionType.INCOME
                            val sign = if (isIncome) "+" else "−"
                            val color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            Text(
                                "$sign${MoneyFormatter.format(tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = color,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}
