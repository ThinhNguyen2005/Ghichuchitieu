package com.notepay.ui.feature.billsplit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.Money
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyState
import com.notepay.ui.util.MoneyFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitScreen(
    onDebtorClick: (String) -> Unit,
    viewModel: BillSplitViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var qrConfigWalletId by remember { mutableStateOf<Long?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<BillSplitItemState?>(null) }

    val qrConfigWallet = qrConfigWalletId?.let { id ->
        state.wallets.find { it.id == id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý Chia tiền") },
                actions = {
                    IconButton(
                        onClick = {
                            qrConfigWalletId = state.activeWallet?.id
                        },
                        enabled = state.activeWallet != null,
                    ) {
                        Icon(Icons.Rounded.QrCode2, contentDescription = "Cấu hình VietQR")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Chia hóa đơn") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            androidx.compose.material3.TabRow(selectedTabIndex = selectedTab) {
                androidx.compose.material3.Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        val count = state.unpaidSplits.groupBy { it.split.debtorName }.size
                        if (count > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Chờ thanh toán")
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(MaterialTheme.colorScheme.error, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }
                        } else {
                            Text("Chờ thanh toán")
                        }
                    }
                )
                androidx.compose.material3.Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Đã thanh toán") }
                )
            }

            if (selectedTab == 0) {
                val unpaidGroups = remember(state.unpaidSplits) {
                    state.unpaidSplits.groupBy { it.split.debtorName }
                }

                if (unpaidGroups.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        EmptyState(message = "Không có khoản nợ nào chờ thanh toán.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(unpaidGroups.entries.toList(), key = { it.key }) { (debtorName, splits) ->
                            val totalAmount = splits.sumOf { it.split.amount.amountInCents }
                            val splitCount = splits.size
                            DebtorGroupRow(
                                debtorName = debtorName,
                                totalAmountCents = totalAmount,
                                splitCount = splitCount,
                                onClick = { onDebtorClick(debtorName) }
                            )
                        }
                    }
                }
            } else {
                if (state.paidSplits.isEmpty()) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        EmptyState(message = "Chưa có khoản nợ nào được trả.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.paidSplits, key = { it.split.id }) { item ->
                            BillSplitItemRow(
                                itemState = item,
                                onClick = {},
                                onDelete = { pendingDeleteBill = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // BottomSheet cấu hình VietQR
    qrConfigWallet?.let { wallet ->
        VietQrConfigSheet(
            wallet = wallet,
            onDismiss = { qrConfigWalletId = null },
            onSave = { bin, accNum, accName ->
                viewModel.updateWalletForQr(wallet.id, bin, accNum, accName)
                qrConfigWalletId = null
                Toast.makeText(context, "Đã lưu cấu hình VietQR cho ví ${wallet.name}", Toast.LENGTH_SHORT).show()
            },
        )
    }

    // Modal Bottom Sheet tạo hóa đơn chia tiền (P0-3: thay cho AlertDialog lồng)
    if (showCreateDialog) {
        BillSplitCreateSheet(
            recentTransactions = state.recentTransactions,
            onConfirm = { parentTxId, entries ->
                viewModel.createBillSplits(parentTxId, entries)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false },
        )
    }

    pendingDeleteBill?.let { item ->
        ConfirmDeleteDialog(
            title = "Xóa khoản nợ?",
            itemName = "${item.split.debtorName} • ${MoneyFormatter.format(item.split.amount)}",
            message = "Khoản nợ của ${item.split.debtorName} sẽ bị xóa vĩnh viễn và không thể khôi phục.",
            onConfirm = { viewModel.deleteBillSplit(item.split.id) },
            onDismiss = { pendingDeleteBill = null },
        )
    }
}

@Composable
private fun DebtorGroupRow(
    debtorName: String,
    totalAmountCents: Long,
    splitCount: Int,
    onClick: () -> Unit
) {
    val amountStr = MoneyFormatter.format(Money(totalAmountCents))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CallReceived,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = debtorName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tổng: $splitCount khoản nợ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = amountStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BillSplitItemRow(
    itemState: BillSplitItemState,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val split = itemState.split
    val parent = itemState.parentTransaction
    val amountStr = MoneyFormatter.format(split.amount)
    val note = parent?.note ?: "Hóa đơn chia tiền"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (split.isPaid) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (split.isPaid) Icons.Rounded.CheckCircle else Icons.Rounded.CallReceived,
                    contentDescription = null,
                    tint = if (split.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (split.isPaid) "${split.debtorName} đã trả" else "${split.debtorName} nợ",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Gốc: $note",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (split.isPaid) {
                    Text(
                        text = "Mã: ${split.memoCode} (Đã trả)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Nội dung QR: ${split.memoCode}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (split.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                // P1-5: padding(end = 4dp) tránh IconButton dính mép card,
                // tăng touch-target trong vùng an toàn, tránh chạm nhầm.
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.padding(end = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

