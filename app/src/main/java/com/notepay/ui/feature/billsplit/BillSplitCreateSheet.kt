package com.notepay.ui.feature.billsplit

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlin.math.abs

private const val MAX_VND_INPUT_LENGTH = 15

data class DebtorEntry(
    val name: String,
    val amountInput: String,
)

private fun vndInputToCents(input: String): Long? {
    val vnd = input.toLongOrNull() ?: return null

    if (vnd <= 0L || vnd > Long.MAX_VALUE / 100L) {
        return null
    }

    return vnd * 100L
}

private fun enteredTotalCents(entries: List<DebtorEntry>): Long {
    var total = 0L

    entries.forEach { entry ->
        val cents = vndInputToCents(entry.amountInput) ?: return@forEach

        total = try {
            Math.addExact(total, cents)
        } catch (_: ArithmeticException) {
            return Long.MAX_VALUE
        }
    }

    return total
}

/**
 * Chia theo đơn vị VND nguyên vì field hiện tại không cho nhập phần lẻ.
 *
 * ponytail: nếu domain hỗ trợ phần nhỏ hơn 1 VND, nâng cấp field để nhập trực tiếp
 * amountInCents thay vì bỏ phần dư dưới 100 cents.
 */
private fun splitEvenlyInVnd(
    totalCents: Long,
    personCount: Int,
): List<String> {
    if (totalCents <= 0L || personCount <= 0) return emptyList()

    val totalVnd = totalCents / 100L
    val baseAmount = totalVnd / personCount
    val remainder = totalVnd % personCount

    return List(personCount) { index ->
        (baseAmount + if (index < remainder) 1L else 0L).toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillSplitCreateSheet(
    recentTransactions: List<Transaction>,
    allDebtorNames: List<String> = emptyList(),
    onConfirm: (
        parentTransactionId: Long,
        entries: List<Pair<String, Long>>,
    ) -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    ),
) {
    val expenses = remember(recentTransactions) {
        recentTransactions.filter { it.type == TransactionType.EXPENSE }
    }

    var selectedTx by remember { mutableStateOf<Transaction?>(null) }
    var showTxPicker by remember { mutableStateOf(true) }
    var newDebtorName by remember { mutableStateOf("") }

    val debtors = remember { mutableStateListOf<DebtorEntry>() }
    val currencyTransformation = remember {
        VietnamCurrencyVisualTransformation()
    }

    val focusManager = LocalFocusManager.current
    val localView = LocalView.current

    val parentCents = selectedTx?.amount?.amountInCents ?: 0L

    val totalCents by remember {
        derivedStateOf {
            enteredTotalCents(debtors)
        }
    }

    val remainingCents = parentCents - totalCents
    val isOverLimit = totalCents > parentCents

    val allAmountsValid by remember {
        derivedStateOf {
            debtors.isNotEmpty() &&
                    debtors.all { entry ->
                        entry.name.isNotBlank() &&
                                vndInputToCents(entry.amountInput) != null
                    }
        }
    }

    /*
     * SnapshotStateList không đổi reference khi thêm hoặc xóa phần tử,
     * vì vậy derivedStateOf phù hợp hơn remember(allDebtorNames, debtors).
     */
    val availableSuggestions by remember(allDebtorNames) {
        derivedStateOf {
            allDebtorNames.filter { suggestion ->
                debtors.none { debtor ->
                    debtor.name.equals(
                        suggestion,
                        ignoreCase = true,
                    )
                }
            }
        }
    }

    val canSave =
        selectedTx != null &&
                allAmountsValid &&
                !isOverLimit

    val disabledReason = when {
        selectedTx == null ->
            "Chọn một giao dịch cần chia."

        debtors.isEmpty() ->
            "Thêm ít nhất một người cùng chia."

        !allAmountsValid ->
            "Nhập số tiền hợp lệ cho từng người."

        isOverLimit ->
            "Tổng số tiền đang vượt hóa đơn gốc."

        else -> null
    }

    fun playSelectionHaptic() {
        localView.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun addDebtor(rawName: String) {
        val name = rawName.trim()

        if (
            name.isBlank() ||
            debtors.any { it.name.equals(name, ignoreCase = true) }
        ) {
            return
        }

        /*
         * Không tự điền toàn bộ tiền còn lại vì hành vi đó gây bất ngờ
         * khi người dùng thêm nhiều người liên tiếp.
         */
        debtors.add(
            DebtorEntry(
                name = name,
                amountInput = "",
            )
        )

        newDebtorName = ""
        playSelectionHaptic()
    }

    fun splitEvenly() {
        val amounts = splitEvenlyInVnd(
            totalCents = parentCents,
            personCount = debtors.size,
        )

        amounts.forEachIndexed { index, amount ->
            debtors[index] = debtors[index].copy(
                amountInput = amount,
            )
        }

        playSelectionHaptic()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .imePadding(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = 104.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Chia hóa đơn",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )

                            Text(
                                text = "Chọn giao dịch, thêm người và chia số tiền.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.heightIn(min = 48.dp),
                        ) {
                            Text("Đóng")
                        }
                    }
                }

                item {
                    BillSplitSectionTitle(
                        step = 1,
                        title = "Chọn giao dịch",
                        completed = selectedTx != null,
                    )
                }

                if (expenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner16,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme
                                    .errorContainer
                                    .copy(alpha = 0.3f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = "Chưa có giao dịch chi tiêu",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    Text(
                                        text = "Hãy tạo một giao dịch chi tiêu trước khi sử dụng tính năng chia hóa đơn.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    selectedTx?.let { transaction ->
                        item {
                            SelectedTransactionCard(
                                transaction = transaction,
                                expanded = showTxPicker,
                                onClick = {
                                    showTxPicker = !showTxPicker
                                },
                            )
                        }
                    }

                    if (selectedTx == null || showTxPicker) {
                        items(
                            items = expenses.take(20),
                            key = { transaction -> transaction.id },
                        ) { transaction ->
                            TransactionPickerRow(
                                transaction = transaction,
                                selected = transaction.id == selectedTx?.id,
                                onClick = {
                                    selectedTx = transaction
                                    showTxPicker = false
                                    debtors.clear()
                                    playSelectionHaptic()
                                },
                            )
                        }
                    }
                }

                if (selectedTx != null) {
                    item {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme
                                .outlineVariant
                                .copy(alpha = 0.6f),
                        )
                    }

                    item {
                        BillSplitSectionTitle(
                            step = 2,
                            title = "Thêm người cùng chia",
                            completed = debtors.isNotEmpty(),
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newDebtorName,
                            onValueChange = { newDebtorName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Tên người") },
                            placeholder = {
                                Text("Ví dụ: Minh, Lan…")
                            },
                            supportingText = {
                                Text("Nhấn Done để thêm nhanh.")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        addDebtor(newDebtorName)
                                        focusManager.clearFocus()
                                    },
                                    enabled = newDebtorName.isNotBlank(),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Thêm người",
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    addDebtor(newDebtorName)
                                    focusManager.clearFocus()
                                },
                            ),
                            singleLine = true,
                            shape = AppTheme.shapes.corner12,
                        )
                    }

                    if (availableSuggestions.isNotEmpty()) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = "Gợi ý gần đây",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(
                                        items = availableSuggestions.take(10),
                                        key = { it },
                                    ) { name ->
                                        AssistChip(
                                            onClick = { addDebtor(name) },
                                            label = {
                                                Text(
                                                    text = name,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Người cùng chia",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )

                                Text(
                                    text = if (debtors.isEmpty()) "Chưa thêm ai" else "${debtors.size} người đã thêm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            FilledTonalButton(
                                onClick = ::splitEvenly,
                                enabled = debtors.isNotEmpty(),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text("Chia đều")
                            }
                        }
                    }

                    if (debtors.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = AppTheme.shapes.corner12,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme
                                        .surfaceContainerLow,
                                ),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Chưa có người nào",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Medium,
                                        )

                                        Text(
                                            text = "Thêm tên ở phía trên để bắt đầu phân chia.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(
                            items = debtors,
                            key = { _, entry -> entry.name },
                        ) { index, entry ->
                            SelectedDebtorCard(
                                entry = entry,
                                onAmountChange = { input ->
                                    debtors[index] = entry.copy(
                                        amountInput = input,
                                    )
                                },
                                onRemove = {
                                    debtors.removeAt(index)
                                    playSelectionHaptic()
                                },
                                currencyTransformation = currencyTransformation,
                            )
                        }
                    }

                    if (debtors.isNotEmpty()) {
                        item {
                            BillSplitSummaryCard(
                                parentCents = parentCents,
                                totalCents = totalCents,
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                tonalElevation = 3.dp,
                shadowElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 12.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    disabledReason?.let { reason ->
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverLimit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    LiquidButton(
                        onClick = {
                            val entries = debtors.mapNotNull { entry ->
                                val cents = vndInputToCents(entry.amountInput)

                                if (
                                    entry.name.isNotBlank() &&
                                    cents != null
                                ) {
                                    entry.name.trim() to cents
                                } else {
                                    null
                                }
                            }

                            selectedTx?.let { transaction ->
                                localView.performHapticFeedback(
                                    HapticFeedbackConstants.KEYBOARD_TAP,
                                )

                                onConfirm(
                                    transaction.id,
                                    entries,
                                )
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        Text(
                            text = "Lưu chia hóa đơn",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillSplitSectionTitle(
    step: Int,
    title: String,
    completed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = AppTheme.shapes.circle,
            color = if (completed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (completed) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Đã hoàn thành",
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SelectedTransactionCard(
    transaction: Transaction,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .primaryContainer
                .copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 44.dp,
                iconSize = 22.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = if (expanded) {
                        "Chạm để đóng danh sách"
                    } else {
                        "Chạm để đổi giao dịch"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun TransactionPickerRow(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme
                    .primaryContainer
                    .copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 40.dp,
                iconSize = 20.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Đang chọn",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SelectedDebtorCard(
    entry: DebtorEntry,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    val focusManager = LocalFocusManager.current

    val amountIsInvalid =
        entry.amountInput.isNotBlank() &&
                vndInputToCents(entry.amountInput) == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = AppTheme.shapes.circle,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.name
                                .firstOrNull()
                                ?.uppercase()
                                ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = entry.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Xóa ${entry.name}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = entry.amountInput,
                onValueChange = { rawInput ->
                    onAmountChange(
                        rawInput
                            .filter(Char::isDigit)
                            .take(MAX_VND_INPUT_LENGTH),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Số tiền người này trả") },
                placeholder = { Text("0") },
                suffix = { Text("₫") },
                supportingText = if (amountIsInvalid) {
                    { Text("Số tiền không hợp lệ.") }
                } else {
                    null
                },
                isError = amountIsInvalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                ),
                visualTransformation = currencyTransformation,
                singleLine = true,
                shape = AppTheme.shapes.corner12,
            )
        }
    }
}

@Composable
private fun BillSplitSummaryCard(
    parentCents: Long,
    totalCents: Long,
) {
    val remainingCents = parentCents - totalCents
    val isOverLimit = totalCents > parentCents

    val progress = if (parentCents > 0L) {
        totalCents.toFloat()
            .div(parentCents.toFloat())
            .coerceIn(0f, 1f)
    } else {
        0f
    }

    val accentColor = if (isOverLimit) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = accentColor.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ReceiptLong,
                    contentDescription = null,
                    tint = accentColor,
                )

                Text(
                    text = "Tổng kết",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                strokeCap = StrokeCap.Round,
            )

            SummaryMoneyRow(
                label = "Hóa đơn gốc",
                amount = parentCents,
            )

            SummaryMoneyRow(
                label = "Đã phân chia",
                amount = totalCents,
                amountColor = accentColor,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SummaryMoneyRow(
                label = if (remainingCents < 0L) {
                    "Đang vượt"
                } else {
                    "Chưa phân chia"
                },
                amount = abs(remainingCents),
                amountColor = if (remainingCents < 0L) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                emphasized = true,
            )
        }
    }
}

@Composable
private fun SummaryMoneyRow(
    label: String,
    amount: Long,
    amountColor: Color = MaterialTheme.colorScheme.onSurface,
    emphasized: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (emphasized) {
                MaterialTheme.typography.titleSmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = if (emphasized) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            },
        )

        Text(
            text = MoneyFormatter.format(Money(amount)),
            style = if (emphasized) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyMedium
            },
            fontWeight = FontWeight.Bold,
            color = amountColor,
        )
    }
}
