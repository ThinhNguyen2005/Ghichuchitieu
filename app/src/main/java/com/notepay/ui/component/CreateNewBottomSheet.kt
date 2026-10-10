package com.notepay.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.ui.theme.AppTheme
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNewBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToBillSplit: () -> Unit,
    onNavigateToSubscription: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        delay(50) // Small delay to let the bottom sheet settle slightly before starting staggering
        isVisible = true
    }

    BottomSheetGlass(
        visible = isVisible,
        onDismissRequest = {
            isVisible = false
            onDismissRequest()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_add_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CreateNewActionCard(
                    visible = isVisible,
                    index = 0,
                    icon = Icons.Rounded.Receipt,
                    title = stringResource(R.string.quick_add_expense_title),
                    description = stringResource(R.string.quick_add_expense_desc),
                    accentColor = AppTheme.colors.error,
                    onClick = {
                        isVisible = false
                        onDismissRequest()
                        onNavigateToAddTransaction()
                    },
                )

                CreateNewActionCard(
                    visible = isVisible,
                    index = 1,
                    icon = Icons.AutoMirrored.Rounded.CallSplit,
                    title = stringResource(R.string.quick_add_bill_split_title),
                    description = stringResource(R.string.quick_add_bill_split_desc),
                    accentColor = AppTheme.colors.warning,
                    onClick = {
                        isVisible = false
                        onDismissRequest()
                        onNavigateToBillSplit()
                    },
                )

                CreateNewActionCard(
                    visible = isVisible,
                    index = 2,
                    icon = Icons.Rounded.Autorenew,
                    title = stringResource(R.string.quick_add_subscription_title),
                    description = stringResource(R.string.quick_add_subscription_desc),
                    accentColor = AppTheme.colors.secondary,
                    onClick = {
                        isVisible = false
                        onDismissRequest()
                        onNavigateToSubscription()
                    },
                )
            }
        }
    }
}
