package com.notepay.feature.transaction.component.sheet

import com.notepay.core.designsystem.theme.AppTheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Wallet
import com.notepay.core.designsystem.util.WalletUiHelper

/**
 * P1-4: Bottom sheet chọn ví — thay cho AssistChip xoay vòng nhiều lần.
 *
 * Ưu điểm:
 *  - Hiển thị đầy đủ danh sách ví, user thấy hết lựa chọn.
 *  - Check icon cho ví đang chọn.
 *  - Bấm trực tiếp vào từng dòng thay vì bấm chip nhiều lần.
 *  - Có thể dùng chung cho cả AddTransaction, EditTransaction và chỗ khác.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletPickerSheet(
    wallets: List<Wallet>,
    selectedWalletId: Long?,
    onWalletSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    onAllWalletsSelected: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp)
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.wallet_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }
            if (wallets.isEmpty()) {
                Text(
                    stringResource(R.string.wallet_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                ) {
                    // Trang Home cần quay về trạng thái "Tất cả ví" nên sheet hỗ trợ thêm dòng này.
                    if (onAllWalletsSelected != null) {
                        item(key = "wallet_picker_all") {
                            WalletPickerRow(
                                title = stringResource(R.string.home_all_wallets),
                                icon = Icons.Rounded.AccountBalanceWallet,
                                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                isSelected = selectedWalletId == null,
                                onClick = {
                                    onAllWalletsSelected()
                                    onDismiss()
                                },
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            )
                        }
                    }
                    items(wallets, key = { it.id }) { wallet ->
                        WalletPickerRow(
                            title = wallet.name,
                            subtitle = wallet.bankBin?.takeIf { it.isNotBlank() },
                            icon = WalletUiHelper.getIcon(wallet.iconKey),
                            iconTint = WalletUiHelper.getColor(wallet.colorKey),
                            isSelected = wallet.id == selectedWalletId,
                            onClick = {
                                onWalletSelected(wallet.id)
                                onDismiss()
                            },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletPickerRow(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = subtitle?.let { text ->
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
            )
        },
        trailingContent = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else null,
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected) selectedRowColor() else Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(AppTheme.shapes.corner12)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onClick,
            ),
    )
}

@Composable
private fun selectedRowColor(): Color =
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
