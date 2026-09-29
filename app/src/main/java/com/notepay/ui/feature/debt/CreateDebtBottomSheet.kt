package com.notepay.ui.feature.debt

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.debt.DebtType
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.formatter.PresentationDateFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateDebtBottomSheet(
    wallets: List<Wallet>,
    onDismiss: () -> Unit,
    onConfirm: (
        personName: String,
        phoneNumber: String?,
        type: DebtType,
        amount: Money,
        walletId: Long?,
        dueDate: Instant?,
        note: String,
        syncWithWallet: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(DebtType.LEND) }
    var personName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedWalletId by remember {
        mutableStateOf(wallets.firstOrNull { it.isActive }?.id ?: wallets.firstOrNull()?.id)
    }
    var syncWithWallet by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDueDateMillis by remember { mutableStateOf<Long?>(null) }

    val rawAmount = amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
    val isAmountValid = rawAmount > 0L
    val isNameValid = personName.isNotBlank()
    val canSave = isAmountValid && isNameValid

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.debt_create_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Segment: Cho vay vs Đi vay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val lendSelected = selectedType == DebtType.LEND
                FilterChip(
                    selected = lendSelected,
                    onClick = { selectedType = DebtType.LEND },
                    label = {
                        Text(
                            text = stringResource(R.string.debt_type_lend_label),
                            fontWeight = if (lendSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF34C759).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF248A3D),
                    )
                )

                val borrowSelected = selectedType == DebtType.BORROW
                FilterChip(
                    selected = borrowSelected,
                    onClick = { selectedType = DebtType.BORROW },
                    label = {
                        Text(
                            text = stringResource(R.string.debt_type_borrow_label),
                            fontWeight = if (borrowSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9500).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFC97600),
                    )
                )
            }

            // Tên người liên quan
            OutlinedTextField(
                value = personName,
                onValueChange = { personName = it },
                label = { Text(stringResource(R.string.debt_person_name_label)) },
                placeholder = { Text(stringResource(R.string.debt_person_name_hint)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Person, contentDescription = null)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
            )

            // Số điện thoại (tùy chọn)
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text(stringResource(R.string.debt_phone_label)) },
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Phone, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
            )

            // Số tiền
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    if (input.all { it.isDigit() } && input.length <= 12) {
                        amountText = input
                    }
                },
                label = { Text(stringResource(R.string.debt_amount_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = VietnamCurrencyVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
                trailingIcon = {
                    Text(
                        text = "₫",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
            )

            // Hạn trả nợ (Due date)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                shape = AppTheme.shapes.corner16,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.debt_due_date_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val dateText = selectedDueDateMillis?.let {
                            PresentationDateFormatter.formatDate(Instant.fromEpochMilliseconds(it))
                        } ?: stringResource(R.string.debt_due_date_hint)
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selectedDueDateMillis != null) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selectedDueDateMillis != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    if (selectedDueDateMillis != null) {
                        IconButton(onClick = { selectedDueDateMillis = null }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Ghi chú
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.debt_note_label)) },
                placeholder = { Text(stringResource(R.string.debt_note_hint)) },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
                shape = AppTheme.shapes.corner16,
            )

            // Chọn ví liên kết & Đồng bộ số dư
            if (wallets.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.debt_sync_wallet_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = stringResource(R.string.debt_sync_wallet_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = syncWithWallet,
                                onCheckedChange = { syncWithWallet = it }
                            )
                        }

                        if (syncWithWallet) {
                            Text(
                                text = stringResource(R.string.debt_wallet_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                wallets.forEach { wallet ->
                                    val isSelected = wallet.id == selectedWalletId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedWalletId = wallet.id },
                                        label = { Text(wallet.name) },
                                        leadingIcon = if (isSelected) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = AppTheme.shapes.corner16,
                ) {
                    Text(stringResource(R.string.action_cancel))
                }

                Button(
                    onClick = {
                        val finalDueDate = selectedDueDateMillis?.let {
                            Instant.fromEpochMilliseconds(it)
                        }
                        onConfirm(
                            personName,
                            phoneNumber,
                            selectedType,
                            Money(rawAmount * 100),
                            if (syncWithWallet) selectedWalletId else null,
                            finalDueDate,
                            note,
                            syncWithWallet,
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.weight(1.5f),
                    shape = AppTheme.shapes.corner16,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedType == DebtType.LEND) Color(0xFF34C759) else Color(0xFFFF9500)
                    )
                ) {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDueDateMillis ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDueDateMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.action_done), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
