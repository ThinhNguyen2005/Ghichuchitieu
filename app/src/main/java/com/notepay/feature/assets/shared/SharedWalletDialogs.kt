package com.notepay.feature.assets.shared

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.NotePayStatusBanner
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.core.navigation.NotePayAnimatedVisibility
import com.notepay.core.navigation.shakeOnError

internal enum class SharedWalletHubTab {
    CREATE,
    JOIN,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SharedWalletHubSheet(
    isCloudConfigured: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onCreateWallet: (name: String, currencyCode: String, monthlyBudgetMinor: Long?) -> Unit,
    onJoinWallet: (code: String, displayName: String) -> Unit,
    initialTab: SharedWalletHubTab = SharedWalletHubTab.CREATE,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    NotePayModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                text = stringResource(R.string.shared_wallet_hub_title),
                style = AppTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            SharedWalletHubTabBar(
                selectedTab = selectedTab,
                enabled = !isSubmitting,
                onTabSelected = { selectedTab = it },
            )

            NotePayStatusBanner(
                visible = !isCloudConfigured,
                message = stringResource(R.string.shared_wallet_offline_hint),
                type = FeedbackType.Offline,
            )

            NotePayAnimatedContent(
                targetState = selectedTab,
                label = "shared-wallet-hub-tab",
            ) { tab ->
                when (tab) {
                    SharedWalletHubTab.CREATE -> SharedWalletCreateForm(
                        enabled = isCloudConfigured && !isSubmitting,
                        isSubmitting = isSubmitting,
                        onDismiss = onDismiss,
                        onConfirm = onCreateWallet,
                    )

                    SharedWalletHubTab.JOIN -> SharedWalletJoinForm(
                        enabled = isCloudConfigured && !isSubmitting,
                        isSubmitting = isSubmitting,
                        onDismiss = onDismiss,
                        onConfirm = onJoinWallet,
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedWalletHubTabBar(
    selectedTab: SharedWalletHubTab,
    enabled: Boolean,
    onTabSelected: (SharedWalletHubTab) -> Unit,
) {
    Surface(
        shape = AppTheme.shapes.capsule,
        color = AppTheme.colors.surfaceSunken,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SharedWalletHubTabButton(
                label = stringResource(R.string.shared_wallet_hub_tab_create),
                selected = selectedTab == SharedWalletHubTab.CREATE,
                enabled = enabled,
                onClick = { onTabSelected(SharedWalletHubTab.CREATE) },
                modifier = Modifier.weight(1f),
            )
            SharedWalletHubTabButton(
                label = stringResource(R.string.shared_wallet_hub_tab_join),
                selected = selectedTab == SharedWalletHubTab.JOIN,
                enabled = enabled,
                onClick = { onTabSelected(SharedWalletHubTab.JOIN) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SharedWalletHubTabButton(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = AppTheme.shapes.capsule,
        color = if (selected) AppTheme.colors.surface else AppTheme.colors.surfaceSunken,
        border = if (selected) BorderStroke(0.5.dp, AppTheme.colors.hairline) else null,
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { this.selected = selected },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = AppTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SharedWalletCreateForm(
    enabled: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, currencyCode: String, monthlyBudgetMinor: Long?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var budgetInput by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it.take(50)
                if (nameError) nameError = false
            },
            label = { Text(stringResource(R.string.shared_wallet_name_label)) },
            singleLine = true,
            enabled = enabled,
            isError = nameError,
            supportingText = {
                NotePayAnimatedVisibility(visible = nameError) {
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
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        SharedWalletFormActionRow(
            submitLabel = stringResource(R.string.shared_wallet_create_submit),
            enabled = enabled,
            isSubmitting = isSubmitting,
            onDismiss = onDismiss,
            onSubmit = {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    nameError = true
                    shakeTrigger++
                } else {
                    val budgetMinor = budgetInput.toLongOrNull()?.takeIf { it > 0L }?.times(100L)
                    onConfirm(trimmedName, "VND", budgetMinor)
                }
            },
        )
    }
}

@Composable
private fun SharedWalletJoinForm(
    enabled: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (code: String, displayName: String) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf(false) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it.trim().uppercase().take(16)
                if (codeError) codeError = false
            },
            label = { Text(stringResource(R.string.shared_wallet_invite_code_label)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            singleLine = true,
            enabled = enabled,
            isError = codeError,
            supportingText = {
                NotePayAnimatedVisibility(visible = codeError) {
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
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        SharedWalletFormActionRow(
            submitLabel = stringResource(R.string.shared_wallet_join_submit),
            enabled = enabled,
            isSubmitting = isSubmitting,
            onDismiss = onDismiss,
            onSubmit = {
                val trimmedCode = code.trim()
                if (trimmedCode.length < 6) {
                    codeError = true
                    shakeTrigger++
                } else {
                    onConfirm(trimmedCode, displayName.trim())
                }
            },
        )
    }
}

@Composable
private fun SharedWalletFormActionRow(
    submitLabel: String,
    enabled: Boolean,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
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
            onClick = onSubmit,
            enabled = enabled,
            shape = AppTheme.shapes.capsule,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
            ),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = AppTheme.colors.onInk,
                )
            } else {
                Text(
                    text = submitLabel,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
