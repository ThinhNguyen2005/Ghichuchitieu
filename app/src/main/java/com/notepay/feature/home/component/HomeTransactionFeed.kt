package com.notepay.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.transaction.component.SwipeableTransactionItem
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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
 * Day-grouped transaction card with iOS soft drop shadow, inset dividers, and category avatars.
 */
@Composable
internal fun HomeDaySection(
    group: HomeDayGroup,
    walletNames: Map<Long, String>,
    hapticEnabled: Boolean,
    isBalanceHidden: Boolean,
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier,
    walletsById: Map<Long, Wallet> = emptyMap(),
    currentUserId: String? = null,
) {
    val cardShape = AppTheme.shapes.corner20

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = group.header,
            style = AppTheme.typography.eyebrow,
            color = AppTheme.colors.textSecondary,
            modifier = Modifier
                .padding(start = 4.dp, top = 20.dp, bottom = 8.dp)
                .semantics { heading() },
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape = cardShape, elevated = false),
            shape = cardShape,
            color = AppTheme.colors.surface,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.transactions.forEachIndexed { index, transaction ->
                    val walletName = walletNames[transaction.walletId].orEmpty()
                    val wallet = walletsById[transaction.walletId]
                    val canEdit = !transaction.isInternalTransfer &&
                        (wallet == null || SharedWalletPermissionGuard.canEditTransaction(wallet, transaction, currentUserId))
                    val canDelete = !(transaction.isInternalTransfer && transaction.transferGroupId == null) &&
                        (wallet == null || SharedWalletPermissionGuard.canDeleteTransaction(wallet, transaction, currentUserId))
                    SwipeableTransactionItem(
                        transaction = transaction,
                        walletName = walletName,
                        onClick = { onTransactionClick(transaction.id) },
                        onEdit = if (canEdit) ({ onEditTransaction(transaction.id) }) else null,
                        onDelete = if (canDelete) ({ onDeleteTransaction(transaction) }) else null,
                        hapticEnabled = hapticEnabled,
                        shape = RectangleShape,
                    ) {
                        HomeTransactionRow(
                            transaction = transaction,
                            walletName = walletName,
                            isBalanceHidden = isBalanceHidden,
                            onClick = { onTransactionClick(transaction.id) },
                        )
                    }
                    if (index < group.transactions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 68.dp, end = 16.dp),
                            thickness = 0.5.dp,
                            color = AppTheme.colors.hairline,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTransactionRow(
    transaction: Transaction,
    walletName: String,
    isBalanceHidden: Boolean,
    onClick: () -> Unit,
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val sign = if (isIncome) "+" else "−"
    val amountColor = if (isIncome) AppTheme.colors.success else AppTheme.colors.textPrimary

    val categoryName = transaction.category.localizedName()
    val trimmedNote = transaction.note.trim()
    val title = trimmedNote.ifBlank { categoryName }
    val authorText = transaction.createdByName?.takeIf { it.isNotBlank() }?.let {
        stringResource(R.string.shared_tx_recorded_by, it)
    }
    val subtitle = listOfNotNull(
        categoryName.takeIf { trimmedNote.isNotBlank() },
        walletName.takeIf { it.isNotBlank() },
        authorText,
        if (transaction.isInternalTransfer) stringResource(R.string.transaction_internal_transfer) else null,
    ).joinToString(" • ")

    val displayAmount = if (isBalanceHidden) {
        MoneyFormatter.MASK
    } else {
        "$sign${MoneyFormatter.format(transaction.amount)}"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .background(AppTheme.colors.surface)
            .clickable(onClickLabel = title, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryAvatar(
            category = transaction.category,
            size = 40.dp,
            iconSize = 18.dp,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Text(
            text = displayAmount,
            style = AppTheme.typography.amountRow,
            color = amountColor,
            maxLines = 1,
        )
    }
}
