package com.notepay.feature.assets.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.shakeOnError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateSharedWalletDialog(
    isCloudConfigured: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, currencyCode: String, monthlyBudgetMinor: Long?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.surface,
        shape = AppTheme.shapes.sheetTop,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.shared_wallet_create_title),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (!isCloudConfigured) {
                OfflineCloudBanner()
            }
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it.take(50)
                    if (nameError) nameError = false
                },
                label = { Text(stringResource(R.string.shared_wallet_name_label)) },
                singleLine = true,
                enabled = isCloudConfigured && !isSubmitting,
                isError = nameError,
                supportingText = {
                    AnimatedVisibility(visible = nameError) {
                        Text(text = stringResource(R.string.shared_wallet_error_name_required))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnError(shakeTrigger),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isEmpty()) {
                            nameError = true
                            shakeTrigger++
                        } else {
                            val budgetMinor = budgetInput.toLongOrNull()?.takeIf { it > 0L }?.times(100L)
                            onConfirm(trimmedName, "VND", budgetMinor)
                        }
                    },
                    enabled = isCloudConfigured && !isSubmitting,
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.ink,
                        contentColor = AppTheme.colors.onInk,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.shared_wallet_create_submit), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JoinSharedWalletDialog(
    isCloudConfigured: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (code: String, displayName: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var code by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppTheme.colors.surface,
        shape = AppTheme.shapes.sheetTop,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.shared_wallet_join_title),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            if (!isCloudConfigured) {
                OfflineCloudBanner()
            }
            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it.trim().uppercase().take(16)
                    if (codeError) codeError = false
                },
                label = { Text(stringResource(R.string.shared_wallet_invite_code_label)) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                singleLine = true,
                enabled = isCloudConfigured && !isSubmitting,
                isError = codeError,
                supportingText = {
                    AnimatedVisibility(visible = codeError) {
                        Text(text = stringResource(R.string.shared_wallet_error_code_invalid))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnError(shakeTrigger),
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it.take(40) },
                label = { Text(stringResource(R.string.shared_wallet_display_name_label)) },
                singleLine = true,
                enabled = isCloudConfigured && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        val trimmedCode = code.trim()
                        if (trimmedCode.length < 6) {
                            codeError = true
                            shakeTrigger++
                        } else {
                            onConfirm(trimmedCode, displayName.trim())
                        }
                    },
                    enabled = isCloudConfigured && !isSubmitting,
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.ink,
                        contentColor = AppTheme.colors.onInk,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.shared_wallet_join_submit), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
