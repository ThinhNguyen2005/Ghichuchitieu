package com.notepay.feature.assets.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme

@Composable
internal fun CreateSharedWalletDialog(
    isCloudConfigured: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, currencyCode: String, monthlyBudgetMinor: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_wallet_create_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isCloudConfigured) {
                    OfflineCloudBanner()
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(50) },
                    label = { Text(stringResource(R.string.shared_wallet_name_label)) },
                    singleLine = true,
                    enabled = isCloudConfigured && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { raw -> budgetInput = raw.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.shared_wallet_budget_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = isCloudConfigured && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val budgetMinor = budgetInput.toLongOrNull()?.takeIf { it > 0L }?.times(100L)
                    onConfirm(name.trim(), "VND", budgetMinor)
                },
                enabled = isCloudConfigured && !isSubmitting && name.trim().isNotEmpty(),
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.shared_wallet_create_submit), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
internal fun JoinSharedWalletDialog(
    isCloudConfigured: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (code: String, displayName: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_wallet_join_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isCloudConfigured) {
                    OfflineCloudBanner()
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.trim().uppercase().take(16) },
                    label = { Text(stringResource(R.string.shared_wallet_invite_code_label)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    singleLine = true,
                    enabled = isCloudConfigured && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(40) },
                    label = { Text(stringResource(R.string.shared_wallet_display_name_label)) },
                    singleLine = true,
                    enabled = isCloudConfigured && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(code.trim(), displayName.trim()) },
                enabled = isCloudConfigured && !isSubmitting && code.trim().length >= 6,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.shared_wallet_join_submit), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun OfflineCloudBanner() {
    Surface(
        shape = AppTheme.shapes.corner16,
        color = AppTheme.colors.surfaceSunken,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.shared_wallet_offline_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
            )
        }
    }
}
