package com.notepay.ui.feature.debt

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Schedule
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.money.Money
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.MoneyFormatter
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Thẻ nợ đơn lẻ v4 Sand & Ink:
 * - Nền surface phẳng bo 24dp không đổ bóng.
 * - Avatar chữ cái đầu nền sand, chữ onSand.
 * - Số tiền dùng màu thu/chi ([AppTheme.colors.success] / [AppTheme.colors.error]), font Inter tnum.
 * - Thanh tiến độ 6dp bo capsule.
 * - Huy hiệu trạng thái kết hợp đủ icon + chữ + màu.
 */
@Composable
fun DebtItemCard(
    item: DebtWithHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    val debt = item.debt
    val isLend = debt.type == DebtType.LEND
    val typeColor = if (isLend) AppTheme.colors.success else AppTheme.colors.error
    val now = Clock.System.now()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top row: Avatar + Name/Phone + StatusBadge (icon + chữ + màu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar chữ cái đầu với nền sand
                val initial = debt.personName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppTheme.shapes.capsule)
                        .background(AppTheme.colors.sand),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.onSand
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = debt.personName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = if (isLend) Icons.AutoMirrored.Rounded.CallMade else Icons.AutoMirrored.Rounded.CallReceived,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    if (!debt.phoneNumber.isNullOrBlank()) {
                        Text(
                            text = debt.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (debt.note.isNotBlank()) {
                        Text(
                            text = debt.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Status Badge: Luôn có icon + chữ + màu
                when {
                    item.isFullyPaid -> {
                        StatusBadge(
                            icon = Icons.Rounded.CheckCircle,
                            text = stringResource(R.string.debt_status_settled),
                            containerColor = AppTheme.colors.success.copy(alpha = 0.15f),
                            contentColor = AppTheme.colors.success
                        )
                    }
                    item.isOverdue(now) -> {
                        StatusBadge(
                            icon = Icons.Rounded.Warning,
                            text = stringResource(R.string.debt_status_overdue),
                            containerColor = AppTheme.colors.error.copy(alpha = 0.15f),
                            contentColor = AppTheme.colors.error
                        )
                    }
                    item.isDueToday(now) -> {
                        StatusBadge(
                            icon = Icons.Rounded.Schedule,
                            text = stringResource(R.string.debt_status_due_today),
                            containerColor = AppTheme.colors.warning.copy(alpha = 0.15f),
                            contentColor = AppTheme.colors.warning
                        )
                    }
                    debt.dueDate != null -> {
                        StatusBadge(
                            icon = Icons.Rounded.CalendarToday,
                            text = stringResource(
                                R.string.debt_status_due_date,
                                PresentationDateFormatter.formatDate(debt.dueDate)
                            ),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Middle: Thanh tiến độ 6dp bo capsule
            LinearProgressIndicator(
                progress = { item.progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(AppTheme.shapes.capsule),
                color = typeColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            // Bottom numbers: Remaining / Paid / Total với font Inter tnum
            // Cụm "Đã trả" và "Gốc" được xếp dọc ở mép phải để không bị tràn màn hình khi fontScale 1.3x - 2.0x
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = stringResource(
                            R.string.debt_card_remaining,
                            MoneyFormatter.format(item.remainingAmount)
                        ),
                        style = AppTheme.typography.amountRow,
                        color = if (item.isFullyPaid) MaterialTheme.colorScheme.onSurfaceVariant else typeColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (item.totalPaid.amountInCents > 0L) {
                        Text(
                            text = stringResource(
                                R.string.debt_card_paid,
                                MoneyFormatter.format(item.totalPaid)
                            ),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.debt_card_total,
                            MoneyFormatter.format(debt.originalAmount)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    icon: ImageVector,
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(AppTheme.shapes.corner12)
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ==== PREVIEWS ====

private val sampleDebtItem = DebtWithHistory(
    debt = Debt(
        id = 1L,
        personName = "Nguyễn Văn A",
        phoneNumber = "0901234567",
        type = DebtType.LEND,
        originalAmount = Money(5_000_000_00),
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        dueDate = Instant.parse("2026-10-30T00:00:00Z"),
        note = "Vay tiền mua laptop",
    ),
    payments = listOf(
        DebtPayment(
            id = 101L,
            debtId = 1L,
            amount = Money(2_000_000_00),
            paidAt = Instant.parse("2026-10-15T00:00:00Z"),
        )
    )
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun DebtItemCardPreviewLight() {
    NotePayTheme(darkTheme = false) {
        DebtItemCard(
            item = sampleDebtItem,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DebtItemCardPreviewDark() {
    NotePayTheme(darkTheme = true) {
        DebtItemCard(
            item = sampleDebtItem,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "FontScale 1.3", fontScale = 1.3f, showBackground = true)
@Composable
private fun DebtItemCardPreviewFont13() {
    NotePayTheme(darkTheme = false) {
        DebtItemCard(
            item = sampleDebtItem,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(name = "FontScale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
private fun DebtItemCardPreviewFont20() {
    NotePayTheme(darkTheme = false) {
        DebtItemCard(
            item = sampleDebtItem,
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
