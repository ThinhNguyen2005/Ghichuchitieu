package com.notepay.core.designsystem.component.action

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.CompareArrows
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.theme.AppTheme
import kotlinx.coroutines.launch

internal enum class QuickAddAction {
    Transaction, Transfer, ScanReceipt, NaturalEntry, Debt, BillSplit, Subscription,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateNewBottomSheet(
    onDismissRequest: () -> Unit,
    onAction: (QuickAddAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }

    fun choose(action: QuickAddAction) {
        if (closing) return
        closing = true
        scope.launch {
            var actionConfirmed = false
            try {
                sheetState.hide()
                if (!sheetState.isVisible) {
                    actionConfirmed = true
                    onAction(action)
                }
            } finally {
                if (!actionConfirmed) closing = false
            }
        }
    }

    NotePayModalBottomSheet(
        onDismissRequest = { if (!closing) onDismissRequest() },
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = !closing,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_add_title),
                style = AppTheme.typography.titleLarge,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 4.dp)
                    .semantics { heading() },
            )

            CreateNewActionCard(
                visible = true,
                index = 0,
                icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                title = stringResource(R.string.quick_add_expense_title),
                description = stringResource(R.string.quick_add_expense_desc),
                accentColor = AppTheme.colors.error,
                enabled = !closing,
                onClick = { choose(QuickAddAction.Transaction) },
            )

            CreateNewActionCard(
                visible = true,
                index = 1,
                icon = Icons.AutoMirrored.Rounded.CompareArrows,
                title = stringResource(R.string.quick_add_transfer_title),
                description = stringResource(R.string.quick_add_transfer_desc),
                accentColor = AppTheme.colors.primary,
                enabled = !closing,
                onClick = { choose(QuickAddAction.Transfer) },
            )

            CreateNewActionCard(
                visible = true,
                index = 2,
                icon = Icons.Rounded.PhotoLibrary,
                title = stringResource(R.string.quick_add_scan_receipt_title),
                description = stringResource(R.string.quick_add_scan_receipt_desc),
                accentColor = AppTheme.colors.primary,
                enabled = !closing,
                onClick = { choose(QuickAddAction.ScanReceipt) },
            )

            CreateNewActionCard(
                visible = true,
                index = 3,
                icon = Icons.Rounded.Mic,
                title = stringResource(R.string.quick_add_text_entry_title),
                description = stringResource(R.string.quick_add_text_entry_desc),
                accentColor = MaterialTheme.colorScheme.primary,
                enabled = !closing,
                onClick = { choose(QuickAddAction.NaturalEntry) },
            )

            CreateNewActionCard(
                visible = true,
                index = 4,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = stringResource(R.string.quick_add_debt_title),
                description = stringResource(R.string.quick_add_debt_desc),
                accentColor = AppTheme.colors.warning,
                enabled = !closing,
                onClick = { choose(QuickAddAction.Debt) },
            )

            CreateNewActionCard(
                visible = true,
                index = 5,
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                title = stringResource(R.string.bill_split_title),
                description = stringResource(R.string.quick_add_bill_split_desc),
                accentColor = MaterialTheme.colorScheme.tertiary,
                enabled = !closing,
                onClick = { choose(QuickAddAction.BillSplit) },
            )

            CreateNewActionCard(
                visible = true,
                index = 6,
                icon = Icons.Rounded.Autorenew,
                title = stringResource(R.string.quick_add_subscription_title),
                description = stringResource(R.string.quick_add_subscription_desc),
                accentColor = AppTheme.colors.success,
                enabled = !closing,
                onClick = { choose(QuickAddAction.Subscription) },
            )
        }
    }
}
