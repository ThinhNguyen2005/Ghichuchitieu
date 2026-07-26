package com.notepay.ui.feature.subscription

import com.notepay.ui.theme.AppTheme
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.LiquidButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.categoryIcon
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.FirefliesBackground
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionBottomSheet(
    state: AddSubscriptionDialogState,
    recentTransactions: List<Transaction> = emptyList(),
    onNameChanged: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onRepeatMonthsChanged: (Int) -> Unit,
    onRemindDaysChanged: (Int) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onNextDueDateChanged: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
    val localView = androidx.compose.ui.platform.LocalView.current
    val playHaptic = {
        try {
            localView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showRecentTxSheet by remember { mutableStateOf(false) }

    val tz = TimeZone.currentSystemDefault()
    val nextDueDate = remember(state.nextDueEpochMs) {
        Instant.fromEpochMilliseconds(state.nextDueEpochMs).toLocalDateTime(tz).date
    }
    val nextDueLabel = "${nextDueDate.day}/${nextDueDate.month.ordinal + 1}/${nextDueDate.year}"

    val selectedCategory = remember(state.category) {
        Category.getAll().firstOrNull { it.id == state.category } ?: Category.OTHER
    }
    val popularServicePresets = remember {
        listOf(
            "Netflix" to Category.ENTERTAINMENT,
            "Spotify" to Category.ENTERTAINMENT,
            "YouTube Premium" to Category.ENTERTAINMENT,
            "iCloud" to Category.INTERNET,
            "ChatGPT Plus" to Category.INTERNET,
            "Tiền điện" to Category.ELECTRICITY,
            "Tiền nước" to Category.WATER,
            "Tiền nhà" to Category.HOME,
        )
    }
    val yearlyCents = remember(state.amountInput, state.repeatMonths) {
        state.amountInput.filter(Char::isDigit).toLongOrNull()
            ?.takeIf { it > 0L && it <= Long.MAX_VALUE / 1_200L }
            ?.let { amountVnd -> amountVnd * 1_200L / state.repeatMonths.coerceAtLeast(1) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            FirefliesBackground(Modifier.fillMaxSize())
            // Column + weight(1f): thanh nút tự lấy chiều cao thật thay vì chừa
            // bottom = 86.dp cố định (nhỏ hơn chiều cao thật nên nội dung bị đè).
            // Bỏ padding ngang ở đây để LazyRow chip chạy hết bề ngang, không bị cắt chữ;
            // padding ngang chuyển xuống từng con.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                Text(
                    "Thêm nhắc nhở gia hạn",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.action_close))
                }
            }

            Text(
                text = "Dịch vụ phổ biến",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 4.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(popularServicePresets, key = { it.first }) { (name, category) ->
                    FilterChip(
                        selected = state.name == name,
                        onClick = {
                            playHaptic()
                            onNameChanged(name)
                            onCategoryChanged(category.id)
                            onRepeatMonthsChanged(1)
                        },
                        label = { Text(name) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    )
                }
            }

            // Gợi ý nhanh từ giao dịch gần đây dạng ngang (LazyRow)
            if (recentTransactions.isNotEmpty()) {
                Text(
                    text = "Gợi ý từ giao dịch gần đây",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(recentTransactions.take(5), key = { it.id }) { tx ->
                        val cat = tx.category
                        val noteText = tx.note.trim()
                        Card(
                            modifier = Modifier
                                .clickable {
                                    playHaptic()
                                    onNameChanged(tx.note.ifBlank { cat.displayName })
                                    onAmountChanged((tx.amount.amountInCents / 100).toString())
                                    onCategoryChanged(cat.id)
                                    onNextDueDateChanged(tx.occurredAt.toEpochMilliseconds())
                                },
                            shape = AppTheme.shapes.corner12,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CategoryAvatar(
                                    category = cat,
                                    size = 24.dp,
                                    iconSize = 12.dp
                                )
                                Column {
                                    Text(
                                        text = cat.displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 110.dp)
                                    )
                                    if (noteText.isNotBlank()) {
                                        Text(
                                            text = noteText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 110.dp)
                                        )
                                    }
                                    Text(
                                        text = MoneyFormatter.format(tx.amount),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChanged,
                label = { Text("Tên dịch vụ") },
                singleLine = true,
                shape = AppTheme.shapes.corner12,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            OutlinedTextField(
                value = state.amountInput,
                onValueChange = onAmountChanged,
                label = { Text("Số tiền (đ)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = currencyTransformation,
                suffix = { Text("đ") },
                shape = AppTheme.shapes.corner12,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            yearlyCents?.let { amount ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
                    ),
                ) {
                    Text(
                        text = "Dịch vụ này tốn khoảng ${MoneyFormatter.format(Money(amount))} mỗi năm",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Danh mục
            Text(
                "Danh mục",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(AppTheme.shapes.corner12)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .clickable { showCategorySheet = true }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(
                        category = selectedCategory,
                        size = 32.dp,
                        iconSize = 16.dp,
                    )
                    Spacer(Modifier.size(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedCategory.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
            }

            // Hợp nhất Ngày đến hạn & Chu kỳ gia hạn vào 1 Card "Lịch thanh toán" duy nhất
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape = AppTheme.shapes.corner16,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Lịch thanh toán & Gia hạn",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Ngày đến hạn tiếp theo (Click toàn bộ vùng để chọn ngày)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Ngày đến hạn tiếp theo",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = nextDueLabel,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                shape = AppTheme.shapes.corner12,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                trailingIcon = {
                                    Icon(Icons.Rounded.CalendarMonth, contentDescription = "Chọn ngày")
                                },
                            )
                        }
                    }

                    // 2. Chu kỳ lặp lại
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Chu kỳ lặp lại",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 4.dp),
                        ) {
                            items(Subscription.REPEAT_OPTIONS) { months ->
                                val label = when (months) {
                                    1 -> "Hàng tháng"
                                    3 -> "Hàng quý"
                                    6 -> "6 tháng"
                                    else -> "Hàng năm"
                                }
                                FilterChip(
                                    selected = state.repeatMonths == months,
                                    onClick = { onRepeatMonthsChanged(months) },
                                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                )
                            }
                        }
                    }
                }
            }

            Text(
                "Nhắc trước",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                items(Subscription.REMIND_OPTIONS) { days ->
                    FilterChip(
                        selected = state.remindDaysBefore == days,
                        onClick = { onRemindDaysChanged(days) },
                        label = {
                            Text(
                                pluralStringResource(
                                    R.plurals.subscription_remind_days,
                                    days,
                                    days,
                                )
                            )
                        },
                    )
                }
            }

            OutlinedTextField(
                value = state.note,
                onValueChange = onNoteChanged,
                label = { Text("Ghi chú (tùy chọn)") },
                singleLine = true,
                shape = AppTheme.shapes.corner12,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

                Spacer(Modifier.height(8.dp))
            }

            GradientBottomActionBar(
                modifier = Modifier.fillMaxWidth(),
            ) {
                LiquidButton(
                    onClick = onConfirm,
                    enabled = state.canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(
                        text = "Lưu nhắc nhở",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.nextDueEpochMs,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onNextDueDateChanged(it) }
                    showDatePicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            current = selectedCategory,
            onPicked = {
                onCategoryChanged(it.id)
                showCategorySheet = false
            },
            onDismiss = { showCategorySheet = false },
        )
    }

    if (showRecentTxSheet) {
        RecentTransactionsSheet(
            transactions = recentTransactions,
            onPicked = { tx ->
                // Prefill: tên = note, số tiền = amount / 100 (cents→VND)
                onNameChanged(tx.note.ifBlank { tx.category.displayName })
                onAmountChanged((tx.amount.amountInCents / 100).toString())
                onCategoryChanged(tx.category.id)
                showRecentTxSheet = false
            },
            onDismiss = { showRecentTxSheet = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    current: Category,
    onPicked: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Chọn danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Category.getAll().forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (category == current) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            else Color.Transparent
                        )
                        .clickable { onPicked(category) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryAvatar(
                        category = category,
                        size = 36.dp,
                        iconSize = 18.dp,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        category.displayName,
                        modifier = Modifier.weight(1f),
                        fontWeight = if (category == current) FontWeight.Bold else FontWeight.Normal,
                        color = if (category == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    if (category == current) {
                        Text("Đang chọn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * BottomSheet chọn 1 giao dịch gần đây để prefill nhắc nhở.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentTransactionsSheet(
    transactions: List<Transaction>,
    onPicked: (Transaction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    "Giao dịch gần đây",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Chọn 1 giao dịch để tự động điền tên, số tiền, danh mục.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
            ) {
            items(transactions, key = { it.id }) { tx ->
                val cat = tx.category
                val noteText = tx.note.trim()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onPicked(tx) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CategoryAvatar(
                            category = cat,
                            size = 40.dp,
                            iconSize = 20.dp,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                cat.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (noteText.isNotBlank()) {
                                Text(
                                    noteText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Text(
                            MoneyFormatter.format(tx.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
