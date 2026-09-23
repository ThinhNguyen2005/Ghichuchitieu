package com.notepay.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.ui.component.BottomSheetGlass
import com.notepay.ui.component.CreateNewActionCard

@Composable
fun QuickAddSheet(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    onAddExpense: () -> Unit,
    onAddBillSplit: () -> Unit,
    onAddSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quickAddTitle = stringResource(R.string.quick_add_title)
    val quickAddExpenseTitle = stringResource(R.string.quick_add_expense_title)
    val quickAddExpenseDesc = stringResource(R.string.quick_add_expense_desc)
    val quickAddBillSplitTitle = stringResource(R.string.quick_add_bill_split_title)
    val quickAddBillSplitDesc = stringResource(R.string.quick_add_bill_split_desc)
    val quickAddSubscriptionTitle = stringResource(R.string.quick_add_subscription_title)
    val quickAddSubscriptionDesc = stringResource(R.string.quick_add_subscription_desc)

    BottomSheetGlass(
        visible = visible,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Text(
            text = quickAddTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
        )
        CreateNewActionCard(
            visible = true,
            index = 0,
            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
            title = quickAddExpenseTitle,
            description = quickAddExpenseDesc,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = onAddExpense,
        )
        CreateNewActionCard(
            visible = true,
            index = 1,
            icon = Icons.AutoMirrored.Outlined.CallSplit,
            title = quickAddBillSplitTitle,
            description = quickAddBillSplitDesc,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = onAddBillSplit,
        )
        CreateNewActionCard(
            visible = true,
            index = 2,
            icon = Icons.Outlined.Notifications,
            title = quickAddSubscriptionTitle,
            description = quickAddSubscriptionDesc,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = onAddSubscription,
        )
    }
}
