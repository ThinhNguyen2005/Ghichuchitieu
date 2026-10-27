package com.notepay.ui.feature.billsplit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.abs

/**
 * Thanh dưới cố định của phiếu chia hóa đơn: tiến độ + câu trạng thái tiền +
 * nút hành động theo bước (Tiếp tục / Xác nhận chia).
 *
 * Trước đây tiến độ nằm trong danh sách nên cuộn mất khi đang nhập ở dưới,
 * còn trạng thái tiền bị lặp ở thẻ tiến độ, thẻ tổng kết và lý do khoá nút.
 */
@Composable
internal fun BillSplitSummaryFooter(
    step: BillSplitStep,
    selectedTx: Transaction?,
    debtors: SnapshotStateList<DebtorEntry>,
    canSave: Boolean,
    onContinue: () -> Unit,
    onSaveSplit: (parentTransactionId: Long, entries: List<Pair<String, Long>>) -> Unit,
    onHapticTap: () -> Unit,
) {
    val parentCents = selectedTx?.amount?.amountInCents ?: 0L
    val totalCents = enteredTotalCents(debtors)
    val remainingCents = parentCents - totalCents
    val isOverLimit = totalCents > parentCents

    val progress = if (parentCents > 0L) {
        (totalCents.toFloat() / parentCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Một câu trạng thái tiền duy nhất cho cả màn, hiện ở thanh dưới cố định.
    val moneyStatus = when {
        isOverLimit -> stringResource(
            R.string.billsplit_status_over_format,
            MoneyFormatter.format(Money(abs(remainingCents))),
        )

        remainingCents == 0L -> stringResource(R.string.billsplit_status_exact)

        else -> stringResource(
            R.string.billsplit_status_remaining_format,
            MoneyFormatter.format(Money(remainingCents)),
        )
    }

    // accentColor chỉ dùng cho thanh tiến độ; chữ vẫn dùng onSurface để đủ tương phản ở mọi theme.
    val statusAccent = when {
        isOverLimit -> MaterialTheme.colorScheme.error
        remainingCents == 0L -> AppTheme.colors.success
        else -> MaterialTheme.colorScheme.primary
    }
    val statusTextColor = if (isOverLimit) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    GradientBottomActionBar(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (step == BillSplitStep.MANAGE_DEBTORS) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = statusAccent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    strokeCap = StrokeCap.Round,
                )

                Text(
                    text = moneyStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = statusTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (selectedTx == null) {
                Text(
                    text = stringResource(R.string.billsplit_select_transaction_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (step == BillSplitStep.SELECT_TRANSACTION) {
                SheetPrimaryButton(
                    label = stringResource(R.string.billsplit_continue),
                    enabled = selectedTx != null,
                    onClick = {
                        onHapticTap()
                        onContinue()
                    },
                )
            } else {
                SheetPrimaryButton(
                    label = stringResource(R.string.billsplit_save_invoice_split),
                    enabled = canSave,
                    onClick = {
                        selectedTx?.let { transaction ->
                            val entries = debtors.mapNotNull { entry ->
                                val cents = vndInputToCents(entry.amountInput)

                                if (cents != null) {
                                    entry.name.trim() to cents
                                } else {
                                    null
                                }
                            }

                            onHapticTap()

                            onSaveSplit(
                                transaction.id,
                                entries,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SheetPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
