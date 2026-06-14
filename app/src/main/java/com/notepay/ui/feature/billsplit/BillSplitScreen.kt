package com.notepay.ui.feature.billsplit

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CallReceived
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.EmptyState
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation

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
                                onDelete = { viewModel.deleteBillSplit(item.split.id) }
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

    // Dialog Tạo Hóa Đơn Chia Tiền mới
    if (showCreateDialog) {
        val expenses = state.recentTransactions.filter { it.type == TransactionType.EXPENSE }
        var selectedTx by remember { mutableStateOf<Transaction?>(null) }
        var showTxDropdown by remember { mutableStateOf(false) }

        val debtors = remember { mutableStateListOf<DebtorEntry>() }

        // Prefill khi chọn giao dịch: 2 ô trống + gợi ý chia đều
        LaunchedEffect(selectedTx) {
            if (selectedTx != null && debtors.isEmpty()) {
                val equalCents = selectedTx!!.amount.amountInCents / 3
                val equalInput = (equalCents / 100).toString()
                debtors.add(DebtorEntry("", equalInput))
                debtors.add(DebtorEntry("", equalInput))
            }
        }

        val totalCents by remember {
            derivedStateOf {
                debtors.sumOf { (it.amountInput.toLongOrNull() ?: 0L) * 100 }
            }
        }

        val parentCents = selectedTx?.amount?.amountInCents ?: 0L
        val isOverLimit = totalCents > parentCents
        val hasName = debtors.any { it.name.isNotBlank() }
        val hasAmount = debtors.any { (it.amountInput.toLongOrNull() ?: 0L) > 0L }
        val canSave = selectedTx != null && hasName && hasAmount && !isOverLimit

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Chia hóa đơn chi tiêu") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chọn giao dịch
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTxDropdown = true }
                    ) {
                        OutlinedTextField(
                            value = selectedTx?.let { "${it.note} (${MoneyFormatter.format(it.amount)})" } ?: "Chọn giao dịch chi tiêu",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Giao dịch chi tiêu") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    if (showTxDropdown) {
                        AlertDialog(
                            onDismissRequest = { showTxDropdown = false },
                            title = { Text("Chọn giao dịch") },
                            text = {
                                if (expenses.isEmpty()) {
                                    Text("Chưa có giao dịch chi tiêu nào gần đây.")
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        expenses.take(8).forEach { tx ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedTx = tx
                                                        showTxDropdown = false
                                                        debtors.clear()
                                                    }
                                                    .padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(tx.note.ifBlank { tx.category.displayName }, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                                Text(MoneyFormatter.format(tx.amount), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTxDropdown = false }) { Text("Đóng") }
                            }
                        )
                    }

                    if (selectedTx != null) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }

                        Text(
                            text = "Người nợ (${debtors.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        debtors.forEachIndexed { index, _ ->
                            DebtorRow(
                                index = index,
                                debtors = debtors,
                                currencyTransformation = currencyTransformation,
                            )
                        }

                        OutlinedButton(
                            onClick = { debtors.add(DebtorEntry("", "")) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm người nợ")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Tổng hóa đơn", style = MaterialTheme.typography.bodyMedium)
                            Text(MoneyFormatter.format(Money(parentCents)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Tổng chia", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                MoneyFormatter.format(Money(totalCents)),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (isOverLimit) {
                            Text(
                                "Tổng số tiền chia vượt quá tổng hóa đơn gốc.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val list = debtors.mapNotNull { entry ->
                            val cents = entry.amountInput.toLongOrNull()?.let { it * 100 } ?: 0L
                            if (entry.name.isNotBlank() && cents > 0L) entry.name to cents else null
                        }
                        selectedTx?.let {
                            viewModel.createBillSplits(it.id, list)
                        }
                        showCreateDialog = false
                    },
                    enabled = canSave
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

private data class DebtorEntry(val name: String, val amountInput: String)

@Composable
private fun DebtorRow(
    index: Int,
    debtors: SnapshotStateList<DebtorEntry>,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    val entry = debtors[index]
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Người nợ #${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { debtors.removeAt(index) },
                    enabled = debtors.size > 1,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = entry.name,
                onValueChange = { debtors[index] = entry.copy(name = it) },
                label = { Text("Tên") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = entry.amountInput,
                onValueChange = { debtors[index] = entry.copy(amountInput = it.filter(Char::isDigit)) },
                label = { Text("Số tiền (VND)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                singleLine = true,
            )
        }
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
                IconButton(onClick = onDelete) {
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

