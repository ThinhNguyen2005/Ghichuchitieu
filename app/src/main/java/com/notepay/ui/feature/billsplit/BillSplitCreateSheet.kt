package com.notepay.ui.feature.billsplit

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation

/**
 * Sheet tạo hóa đơn chia tiền — refactor từ AlertDialog lồng AlertDialog (P0-3).
 *
 * Ưu điểm so với phiên bản AlertDialog cũ:
 *  - 1 sheet duy nhất, không còn nested dialog khó chịu.
 *  - Khu vực chọn giao dịch dùng `LazyColumn` scroll độc lập, có thể chứa nhiều mục.
 *  - Khi đã chọn giao dịch, phần nhập người nợ chiếm phần còn lại của sheet
 *    (tự co giãn theo nội dung, có thể scroll mượt).
 *  - Bottom button "Lưu" sticky ở dưới cùng sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitCreateSheet(
    recentTransactions: List<Transaction>,
    onConfirm: (parentTransactionId: Long, entries: List<Pair<String, Long>>) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val expenses = remember(recentTransactions) {
        recentTransactions.filter { it.type == TransactionType.EXPENSE }
    }
    var selectedTx by remember { mutableStateOf<Transaction?>(null) }
    var showTxPicker by remember { mutableStateOf(false) }
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Chia hóa đơn chi tiêu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onDismiss) {
                    Text("Đóng")
                }
            }

            // Bước 1: chọn giao dịch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showTxPicker = !showTxPicker },
            ) {
                OutlinedTextField(
                    value = selectedTx?.let { "${it.note} (${MoneyFormatter.format(it.amount)})" }
                        ?: "Chọn giao dịch chi tiêu",
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Giao dịch chi tiêu") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    },
                )
            }

            // Lazy danh sách giao dịch — thay cho AlertDialog lồng
            if (showTxPicker) {
                if (expenses.isEmpty()) {
                    Text(
                        "Chưa có giao dịch chi tiêu nào gần đây.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp),
                        ) {
                            items(expenses.take(20), key = { it.id }) { tx ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTx = tx
                                            showTxPicker = false
                                            debtors.clear()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        tx.note.ifBlank { tx.category.displayName },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        MoneyFormatter.format(tx.amount),
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                )
                            }
                        }
                    }
                }
            }

            // Bước 2: nhập người nợ
            if (selectedTx != null) {
                HorizontalDivider()
                val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Người nợ (${debtors.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    AssistChip(
                        onClick = { debtors.add(DebtorEntry("", "")) },
                        label = { Text("Thêm") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        colors = AssistChipDefaults.assistChipColors(),
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(debtors.size) { index ->
                        DebtorRow(
                            index = index,
                            debtors = debtors,
                            currencyTransformation = currencyTransformation,
                        )
                    }
                }

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Tổng hóa đơn", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        MoneyFormatter.format(Money(parentCents)),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
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
                        color = if (isOverLimit) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
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

            // Sticky bottom button
            Button(
                onClick = {
                    val list = debtors.mapNotNull { entry ->
                        val cents = entry.amountInput.toLongOrNull()?.let { it * 100 } ?: 0L
                        if (entry.name.isNotBlank() && cents > 0L) entry.name to cents else null
                    }
                    selectedTx?.let { onConfirm(it.id, list) }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Lưu", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

data class DebtorEntry(val name: String, val amountInput: String)

@Composable
fun DebtorRow(
    index: Int,
    debtors: SnapshotStateList<DebtorEntry>,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    val entry = debtors[index]
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error,
                    )
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
                onValueChange = {
                    debtors[index] = entry.copy(amountInput = it.filter(Char::isDigit))
                },
                label = { Text("Số tiền (VND)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                singleLine = true,
            )
        }
    }
}
