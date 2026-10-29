package com.notepay.ui.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.SwipeableTransactionItem
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.localizedName
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

/** Một nhóm giao dịch theo ngày trên trang Home, kèm tiêu đề đã bản địa hoá. */
@Immutable
internal data class HomeDayGroup(
    val key: String,
    val header: String,
    val transactions: List<Transaction>,
)

/**
 * Gom giao dịch gần đây thành từng ngày như màn hình sổ tay: "HÔM NAY", "HÔM QUA" rồi
 * tới "23 THÁNG 10". Tiêu đề cần chuỗi bản địa hoá nên phép gom nằm ở tầng UI.
 */
@Composable
internal fun rememberHomeDayGroups(transactions: List<Transaction>): List<HomeDayGroup> {
    val zone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(zone).date
    val todayLabel = stringResource(R.string.home_day_today)
    val yesterdayLabel = stringResource(R.string.home_day_yesterday)
    val otherFormat = stringResource(R.string.home_day_other)

    return remember(transactions, today, todayLabel, yesterdayLabel, otherFormat) {
        transactions
            .groupBy { it.occurredAt.toLocalDateTime(zone).date }
            .map { (date, dayTransactions) ->
                val header = when (date.toEpochDays() - today.toEpochDays()) {
                    0L -> todayLabel
                    -1L -> yesterdayLabel
                    else -> {
                        // %2$s là tên tháng viết tắt (bản tiếng Anh), %3$d là số tháng (bản tiếng Việt).
                        // Dùng enum Month của kotlinx-datetime để không phụ thuộc java.time trên API cũ.
                        val monthName = date.month.name.take(3)
                        otherFormat.format(date.day, monthName, date.month.number)
                    }
                }
                HomeDayGroup(
                    key = date.toString(),
                    header = header,
                    transactions = dayTransactions,
                )
            }
    }
}

/**
 * Một ngày trong feed: nhãn ngày kiểu eyebrow nằm ngoài, còn giao dịch gói trong thẻ trắng
 * bo 20dp (DESIGN.md — day group card). Vuốt từng dòng vẫn lộ nút Sửa/Xóa.
 */
@Composable
internal fun HomeDaySection(
    group: HomeDayGroup,
    walletNames: Map<Long, String>,
    hapticEnabled: Boolean,
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = group.header,
            style = AppTheme.typography.eyebrow,
            color = AppTheme.colors.textMuted,
            modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = AppTheme.shapes.corner20,
            color = AppTheme.colors.surface,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.transactions.forEach { transaction ->
                    val walletName = walletNames[transaction.walletId].orEmpty()
                    SwipeableTransactionItem(
                        transaction = transaction,
                        walletName = walletName,
                        onClick = { onTransactionClick(transaction.id) },
                        onEdit = { onEditTransaction(transaction.id) },
                        onDelete = { onDeleteTransaction(transaction) },
                        hapticEnabled = hapticEnabled,
                        shape = RectangleShape,
                    ) {
                        HomeTransactionRow(
                            transaction = transaction,
                            walletName = walletName,
                            onClick = { onTransactionClick(transaction.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dòng giao dịch kiểu "sổ": tên giao dịch đậm, dòng phụ "Danh mục • Ví", số tiền canh phải.
 * Số tiền chi dùng màu chữ chính (ink) như bản thiết kế; số tiền thu dùng màu income.
 */
@Composable
private fun HomeTransactionRow(
    transaction: Transaction,
    walletName: String,
    onClick: () -> Unit,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "−"
    val amountColor = if (isIncome) AppTheme.colors.success else AppTheme.colors.textPrimary

    val title = transaction.note.trim().ifBlank { transaction.category.localizedName() }
    val subtitle = listOfNotNull(
        transaction.category.localizedName(),
        walletName.takeIf { it.isNotBlank() },
        if (transaction.isInternalTransfer) stringResource(R.string.transaction_internal_transfer) else null,
    ).joinToString(" • ")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.surface)
            .clickable(onClickLabel = title, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = "$sign${MoneyFormatter.format(transaction.amount)}",
            style = AppTheme.typography.amountRow,
            color = amountColor,
            maxLines = 1,
        )
    }
}
