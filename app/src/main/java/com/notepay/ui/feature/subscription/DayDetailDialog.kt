package com.notepay.ui.feature.subscription

import androidx.compose.runtime.Composable
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.DayDetailDialog
import kotlinx.datetime.LocalDate

// Re-export DayDetailDialog sang package subscription để giữ API tương thích
// với SubscriptionScreen. Nội dung đã được chuyển sang ui/component.
@Composable
fun DayDetailDialog(
    date: LocalDate,
    transactions: List<Transaction> = emptyList(),
    subscriptions: List<Subscription> = emptyList(),
    onDismiss: () -> Unit,
) {
    DayDetailDialog(
        date = date,
        transactions = transactions,
        subscriptions = subscriptions,
        onDismiss = onDismiss,
    )
}
