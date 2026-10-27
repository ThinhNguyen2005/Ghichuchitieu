package com.notepay.ui.feature.billsplit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.localizedName

/**
 * Bước 1 của phiếu chia hóa đơn: tiêu đề bước, cảnh báo khi chưa có giao dịch
 * chi tiêu nào, và danh sách dọc các giao dịch để chọn.
 *
 * Là extension trên `LazyListScope` (không phải @Composable) để giữ nguyên vị trí
 * từng thẻ trong LazyColumn của sheet thay vì dồn hết vào một `item {}` — hành vi
 * cuộn, key, tái tạo composition được bảo toàn tuyệt đối (pure code motion).
 * Chuỗi tiêu đề được hoist thành tham số vì body hàm này không ở Composable scope.
 */
internal fun LazyListScope.billSplitTransactionSelectorItems(
    step: BillSplitStep,
    expenses: List<Transaction>,
    selectedTx: Transaction?,
    selectedTransactionId: Long,
    chooseTransactionTitle: String,
    onPickTransaction: (Transaction) -> Unit,
) {
    if (step != BillSplitStep.SELECT_TRANSACTION) return

    item {
        BillSplitSectionTitle(
            step = 1,
            title = chooseTransactionTitle,
            completed = selectedTx != null,
        )
    }

    if (expenses.isEmpty()) {
        item {
            NoExpenseWarningCard()
        }
    } else {
        items(
            items = expenses.take(20),
            key = { transaction -> transaction.id },
        ) { transaction ->
            TransactionPickerRow(
                transaction = transaction,
                selected = transaction.id == selectedTransactionId,
                onClick = { onPickTransaction(transaction) },
            )
        }
    }
}

@Composable
private fun NoExpenseWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .errorContainer
                .copy(alpha = 0.3f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.billsplit_no_expense_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = stringResource(R.string.billsplit_no_expense_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TransactionPickerRow(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme
                    .primaryContainer
                    .copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 40.dp,
                iconSize = 20.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.billsplit_selecting),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
