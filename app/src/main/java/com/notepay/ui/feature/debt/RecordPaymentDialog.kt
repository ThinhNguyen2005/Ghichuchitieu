package com.notepay.ui.feature.debt

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation

@Composable
fun RecordPaymentDialog(
    debtWithHistory: DebtWithHistory,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Money,
        walletId: Long?,
        note: String,
        syncWithWallet: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val remainingCents = debtWithHistory.remainingAmount.amountInCents
    val remainingMajor = remainingCents / 100

    var amountText by remember { mutableStateOf(remainingMajor.toString()) }
    var note by remember { mutableStateOf("") }
    var selectedWalletId by remember {
        mutableStateOf(wallets.firstOrNull { it.isActive }?.id ?: wallets.firstOrNull()?.id)
    }
    var syncWithWallet by remember { mutableStateOf(wallets.isNotEmpty()) }

    val rawAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val isAmountValid = rawAmount > 0L

    val isLend = debtWithHistory.debt.type == DebtType.LEND

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.debt_payment_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info còn lại
                Text(
                    text = stringResource(
                        R.string.debt_card_remaining,
                        MoneyFormatter.format(debtWithHistory.remainingAmount)
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // Quick chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { amountText = remainingMajor.toString() },
                        label = { Text("Tất cả (${MoneyFormatter.format(debtWithHistory.remainingAmount)})") }
                    )
                    if (remainingMajor >= 2) {
                        SuggestionChip(
                            onClick = { amountText = (remainingMajor / 2).toString() },
                            label = { Text("50%") }
                        )
                    }
                }

                // Input số tiền
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 12) {
                            amountText = input
                        }
                    },
                    label = { Text(stringResource(R.string.debt_amount_label)) },
                    placeholder = { Text(stringResource(R.string.debt_payment_amount_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = VietnamCurrencyVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                    trailingIcon = {
                        Text(
                            text = "₫",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                )

                // Ghi chú
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.debt_note_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                )

                // Chọn ví & đồng bộ
                if (wallets.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.debt_payment_sync_label),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = syncWithWallet,
                            onCheckedChange = { syncWithWallet = it }
                        )
                    }

                    if (syncWithWallet) {
                        Text(
                            text = stringResource(R.string.debt_payment_wallet_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            wallets.forEach { wallet ->
                                val isSelected = wallet.id == selectedWalletId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWalletId = wallet.id },
                                    label = { Text(wallet.name) },
                                    leadingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Rounded.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        Money(rawAmount * 100),
                        if (syncWithWallet) selectedWalletId else null,
                        note,
                        syncWithWallet,
                    )
                },
                enabled = isAmountValid,
                shape = AppTheme.shapes.corner16,
            ) {
                Text(stringResource(R.string.action_done), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = AppTheme.shapes.corner16,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = AppTheme.shapes.corner24,
        modifier = modifier
    )
}
