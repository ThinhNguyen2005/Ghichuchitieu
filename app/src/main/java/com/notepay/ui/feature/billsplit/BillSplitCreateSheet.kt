package com.notepay.ui.feature.billsplit

import com.notepay.ui.theme.AppTheme
import com.notepay.ui.component.LiquidButton

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import com.notepay.ui.component.categoryIcon
import com.notepay.ui.component.CategoryAvatar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitCreateSheet(
    recentTransactions: List<Transaction>,
    allDebtorNames: List<String> = emptyList(),
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
    var newDebtorName by remember { mutableStateOf("") }

    val totalCents by remember {
        derivedStateOf {
            debtors.sumOf { (it.amountInput.toLongOrNull() ?: 0L) * 100 }
        }
    }
    val parentCents = selectedTx?.amount?.amountInCents ?: 0L
    val remainingCents = parentCents - totalCents
    val isOverLimit = totalCents > parentCents

    val availableSuggestions = remember(allDebtorNames, debtors) {
        allDebtorNames.filter { name ->
            debtors.none { it.name.equals(name, ignoreCase = true) }
        }
    }

    val canSave = selectedTx != null && debtors.isNotEmpty() && 
            debtors.all { it.name.isNotBlank() && (it.amountInput.toLongOrNull() ?: 0L) > 0L } && 
            !isOverLimit

    fun addDebtor(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && debtors.none { it.name.equals(trimmed, ignoreCase = true) }) {
            // Suggest remaining split amount as placeholder helper
            val suggestAmount = if (remainingCents > 0) (remainingCents / 100).toString() else ""
            debtors.add(DebtorEntry(trimmed, suggestAmount))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
            if (selectedTx == null) {
                Text(
                    text = "Chọn giao dịch chi tiêu gần đây để chia:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            } else {
                val tx = selectedTx!!
                val cat = tx.category
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTxPicker = !showTxPicker },
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CategoryAvatar(
                            category = cat,
                            size = 40.dp,
                            iconSize = 20.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tx.note.ifBlank { cat.displayName },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = MoneyFormatter.format(tx.amount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Rounded.ArrowDropDown,
                                contentDescription = "Thay đổi giao dịch",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Danh sách giao dịch để chọn
            if (expenses.isEmpty()) {
                // Khi không có giao dịch nào, hiển thị hướng dẫn tạo giao dịch thủ công
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    ),
                    shape = AppTheme.shapes.corner16
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = "Chưa có giao dịch chi tiêu",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Để chia tiền, trước hết bạn cần tạo ít nhất một giao dịch chi tiêu (Expense) thủ công bằng nút (+) ở màn hình chính.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Khi có giao dịch, hiển thị danh sách chọn giao dịch hoặc chi tiết giao dịch đã chọn
                if (selectedTx == null || showTxPicker) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        shape = AppTheme.shapes.corner16
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                        ) {
                            items(expenses.take(20), key = { it.id }) { tx ->
                                val cat = tx.category
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedTx = tx
                                            showTxPicker = false
                                            debtors.clear()
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    CategoryAvatar(
                                        category = cat,
                                        size = 36.dp,
                                        iconSize = 18.dp,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            tx.note.ifBlank { cat.displayName },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            cat.displayName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        MoneyFormatter.format(tx.amount),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                )
                            }
                        }
                    }
                }
            }


            // Bước 2: Thêm người nợ và phân chia tiền
            if (selectedTx != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Input thêm người nợ mới
                OutlinedTextField(
                    value = newDebtorName,
                    onValueChange = { newDebtorName = it },
                    label = { Text("Tên người nợ") },
                    placeholder = { Text("Nhập tên để thêm vào danh sách...") },
                    singleLine = true,
                    shape = AppTheme.shapes.corner12,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (newDebtorName.isNotBlank()) {
                                    addDebtor(newDebtorName)
                                    newDebtorName = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Thêm người nợ",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips gợi ý người nợ gần đây
                if (availableSuggestions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Người nợ gần đây:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(availableSuggestions) { name ->
                                AssistChip(
                                    onClick = { addDebtor(name) },
                                    label = { Text(name) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                        labelColor = MaterialTheme.colorScheme.primary
                                    ),
                                    border = null,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Người nợ đã chọn (${debtors.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }

                // List các debtor đã chọn
                if (debtors.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = AppTheme.shapes.corner12
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Chưa chọn người nợ nào. Vui lòng thêm ở trên!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(debtors.size) { index ->
                            val entry = debtors[index]
                            SelectedDebtorCard(
                                entry = entry,
                                onAmountChange = { input ->
                                    debtors[index] = entry.copy(amountInput = input)
                                },
                                onRemove = {
                                    debtors.removeAt(index)
                                },
                                currencyTransformation = currencyTransformation
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Summary info
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Tổng hóa đơn gốc", style = MaterialTheme.typography.bodyMedium)
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
                        Text("Đã phân chia", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            MoneyFormatter.format(Money(totalCents)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isOverLimit) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Còn lại", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            MoneyFormatter.format(Money(remainingCents)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                remainingCents < 0 -> MaterialTheme.colorScheme.error
                                remainingCents == 0L -> Color(0xFF4CAF50)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                if (isOverLimit) {
                    Text(
                        "Tổng số tiền chia vượt quá tổng hóa đơn gốc.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Save Button
            LiquidButton(
                onClick = {
                    val list = debtors.mapNotNull { entry ->
                        val cents = entry.amountInput.toLongOrNull()?.let { it * 100 } ?: 0L
                        if (entry.name.isNotBlank() && cents > 0L) entry.name to cents else null
                    }
                    selectedTx?.let { onConfirm(it.id, list) }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Lưu cấu hình chia hóa đơn", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}


data class DebtorEntry(val name: String, val amountInput: String)

@Composable
fun SelectedDebtorCard(
    entry: DebtorEntry,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(AppTheme.shapes.circle)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = entry.name.firstOrNull()?.uppercase() ?: "?",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            OutlinedTextField(
                value = entry.amountInput,
                onValueChange = { onAmountChange(it.filter(Char::isDigit)) },
                label = { Text("Số tiền chia (VND)") },
                placeholder = { Text("Nhập số tiền...") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                singleLine = true,
                shape = AppTheme.shapes.corner12
            )
        }
    }
}
