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
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewBottomSheet(
    onDismissRequest: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToNaturalTextEntry: () -> Unit,
    onNavigateToAddDebt: () -> Unit,
    onNavigateToBillSplit: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = AppTheme.shapes.sheetTop,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            )
        },
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
                onClick = {
                    onDismissRequest()
                    onNavigateToAddTransaction()
                },
            )

            CreateNewActionCard(
                visible = true,
                index = 1,
                icon = Icons.Rounded.Mic,
                title = stringResource(R.string.quick_add_text_entry_title),
                description = stringResource(R.string.quick_add_text_entry_desc),
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = {
                    onDismissRequest()
                    onNavigateToNaturalTextEntry()
                },
            )

            CreateNewActionCard(
                visible = true,
                index = 2,
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                title = stringResource(R.string.quick_add_debt_title),
                description = stringResource(R.string.quick_add_debt_desc),
                accentColor = AppTheme.colors.warning,
                onClick = {
                    onDismissRequest()
                    onNavigateToAddDebt()
                },
            )

            CreateNewActionCard(
                visible = true,
                index = 3,
                icon = Icons.AutoMirrored.Rounded.CallSplit,
                title = stringResource(R.string.bill_split_title),
                description = stringResource(R.string.quick_add_bill_split_desc),
                accentColor = MaterialTheme.colorScheme.tertiary,
                onClick = {
                    onDismissRequest()
                    onNavigateToBillSplit()
                },
            )

            CreateNewActionCard(
                visible = true,
                index = 4,
                icon = Icons.Rounded.Autorenew,
                title = stringResource(R.string.quick_add_subscription_title),
                description = stringResource(R.string.quick_add_subscription_desc),
                accentColor = AppTheme.colors.success,
                onClick = {
                    onDismissRequest()
                    onNavigateToSubscription()
                },
            )
        }
    }
}
