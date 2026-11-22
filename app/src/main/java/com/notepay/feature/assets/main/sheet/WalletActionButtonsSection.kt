package com.notepay.feature.assets.main.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.theme.AppTheme

@Composable
internal fun WalletActionButtonsSection(
    canDeleteDirectly: Boolean,
    canArchive: Boolean,
    isArchived: Boolean,
    onDeleteClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onUnarchiveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (canDeleteDirectly) {
            OutlinedButton(
                onClick = onDeleteClick,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                shape = AppTheme.shapes.corner12,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.wallet_action_delete),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else if (canArchive) {
            if (!isArchived) {
                OutlinedButton(
                    onClick = onArchiveClick,
                    shape = AppTheme.shapes.corner12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.wallet_action_archive),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onUnarchiveClick,
                    shape = AppTheme.shapes.corner12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Unarchive,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.wallet_action_unarchive),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WalletActionDialogs(
    walletName: String,
    transactionCount: Int,
    showDeleteConfirm: Boolean,
    showArchiveConfirm: Boolean,
    showUnarchiveConfirm: Boolean,
    onDismissDelete: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissArchive: () -> Unit,
    onConfirmArchive: () -> Unit,
    onDismissUnarchive: () -> Unit,
    onConfirmUnarchive: () -> Unit,
) {
    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = stringResource(R.string.wallet_delete_confirm_title),
            itemName = walletName,
            message = stringResource(R.string.wallet_delete_confirm_message, walletName),
            confirmLabel = stringResource(R.string.wallet_action_delete),
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = onDismissArchive,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Archive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.wallet_archive_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = pluralStringResource(
                        R.plurals.wallet_archive_confirm_message,
                        transactionCount,
                        walletName,
                        transactionCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmArchive) {
                    Text(
                        text = stringResource(R.string.wallet_action_archive),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissArchive) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showUnarchiveConfirm) {
        AlertDialog(
            onDismissRequest = onDismissUnarchive,
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Unarchive,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.wallet_action_unarchive),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.wallet_unarchive_confirm_message, walletName),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(onClick = onConfirmUnarchive) {
                    Text(
                        text = stringResource(R.string.wallet_action_unarchive),
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissUnarchive) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
