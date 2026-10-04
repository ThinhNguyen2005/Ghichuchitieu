package com.notepay.ui.feature.billsplit

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter

private enum class ReconciliationMethod {
    CASH, TRANSFER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentReconciliationSheet(
    debtorName: String,
    totalDebt: Money,
    recentTransactions: List<Transaction>,
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (incomeTxId: Long?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var method by remember { mutableStateOf(ReconciliationMethod.CASH) }
    var selectedIncomeTxId by remember { mutableStateOf<Long?>(null) }

    // 1. Tối ưu: remember thuần túy thay vì bọc derivedStateOf thừa
    val incomeTransactions = remember(recentTransactions) {
        recentTransactions
            .filter { it.type == TransactionType.INCOME }
            .sortedByDescending { it.occurredAt }
    }

    // 2. Tối ưu tra cứu tên ví O(1)
    val walletMap = remember(wallets) {
        wallets.associate { it.id to it.name }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(animationSpec = tween(durationMillis = 250))
                .verticalScroll(rememberScrollState()) // Chống tràn màn hình nhỏ
                .padding(start = 20.dp, end = 20.dp, bottom = 36.dp, top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header vùng thông tin dư nợ
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.billsplit_collect_debt_format, debtorName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.billsplit_total_debt_format, MoneyFormatter.format(totalDebt)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.offset(x = 12.dp, y = (-8).dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                }
            }

            // Thẻ lựa chọn Phương thức thanh toán (Chuẩn Accessibility selectableGroup)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = AppTheme.shapes.corner20,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .padding(6.dp)
                        .selectableGroup(), // Gom nhóm Radio cho Accessibility
                ) {
                    PaymentMethodOption(
                        selected = method == ReconciliationMethod.CASH,
                        onClick = {
                            method = ReconciliationMethod.CASH
                            selectedIncomeTxId = null
                        },
                        icon = Icons.Rounded.Payments,
                        iconTint = MaterialTheme.colorScheme.primary,
                        iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        title = stringResource(R.string.billsplit_cash_payment),
                        desc = stringResource(R.string.billsplit_cash_payment_desc),
                    )

                    PaymentMethodOption(
                        selected = method == ReconciliationMethod.TRANSFER,
                        onClick = { method = ReconciliationMethod.TRANSFER },
                        icon = Icons.Rounded.AccountBalance,
                        iconTint = MaterialTheme.colorScheme.secondary,
                        iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                        title = stringResource(R.string.billsplit_receive_by_transfer),
                        desc = stringResource(R.string.billsplit_transfer_payment_desc),
                    )
                }
            }

            // Danh sách giao dịch Ngân hàng khớp đối soát
            if (method == ReconciliationMethod.TRANSFER) {
                Text(
                    text = stringResource(R.string.billsplit_choose_matching_transaction),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                )

                if (incomeTransactions.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner16,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(28.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.billsplit_no_recent_income),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(incomeTransactions, key = { it.id }) { tx ->
                            val isSelected = selectedIncomeTxId == tx.id
                            val walletName = walletMap[tx.walletId]
                                ?: stringResource(R.string.billsplit_other_wallet)

                            val cardBgColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                            val borderStroke = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }

                            // Dùng Card(onClick = ...) chuẩn Material 3
                            Card(
                                onClick = { selectedIncomeTxId = tx.id },
                                shape = AppTheme.shapes.corner14,
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                border = borderStroke,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    val noteText = tx.note.trim()
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(AppTheme.shapes.circle)
                                            .background(
                                                if (tx.isAutoCapture) MaterialTheme.colorScheme.tertiaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Icon(
                                            imageVector = if (tx.isAutoCapture) Icons.Rounded.Smartphone else Icons.Rounded.AccountBalance,
                                            contentDescription = null,
                                            tint = if (tx.isAutoCapture) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tx.category.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (noteText.isNotBlank()) {
                                            Text(
                                                text = noteText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            text = "$walletName • ${PresentationDateFormatter.formatDayMonthTime(tx.occurredAt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            text = "+${MoneyFormatter.format(tx.amount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = stringResource(R.string.billsplit_selected),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Nút bấm xác nhận
            val isEnabled = method == ReconciliationMethod.CASH || selectedIncomeTxId != null
            Button(
                onClick = { onConfirm(selectedIncomeTxId) },
                enabled = isEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = AppTheme.shapes.corner16,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(
                    text = if (method == ReconciliationMethod.CASH) {
                        stringResource(R.string.billsplit_record_cash_payment)
                    } else {
                        stringResource(R.string.billsplit_confirm_reconcile)
                    },
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Component lựa chọn phương thức thanh toán chuẩn Accessibility.
 */
@Composable
private fun PaymentMethodOption(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    desc: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner14)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else Color.Transparent,
            )
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = null, // Tránh double focus & double click cho TalkBack
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(AppTheme.shapes.circle)
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
