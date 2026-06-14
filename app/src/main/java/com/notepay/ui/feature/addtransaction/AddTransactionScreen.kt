package com.notepay.ui.feature.addtransaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            viewModel.onEvent(AddTransactionEvent.Reset)
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thêm giao dịch") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { TransactionTypeSelector(state.type, viewModel::onEvent) }
            item { AmountInput(state, viewModel::onEvent) }
            item { CategoryGrid(state, viewModel::onEvent) }
            item { NoteInput(state, viewModel::onEvent) }
            item { DateAndWalletRow(state, viewModel::onEvent) }
            item {
                Button(
                    onClick = { viewModel.onEvent(AddTransactionEvent.Save) },
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Lưu giao dịch")
                    }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onEvent: (AddTransactionEvent) -> Unit,
) {
    val options = listOf(TransactionType.EXPENSE to "Chi tiêu", TransactionType.INCOME to "Thu nhập")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedType == option.first,
                onClick = { onEvent(AddTransactionEvent.TypeChanged(option.first)) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(option.second)
            }
        }
    }
}

@Composable
private fun AmountInput(state: AddTransactionUiState, onEvent: (AddTransactionEvent) -> Unit) {
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
    OutlinedTextField(
        value = state.amountInput,
        onValueChange = { onEvent(AddTransactionEvent.AmountChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Số tiền") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = currencyTransformation,
        isError = FieldError.AMOUNT_EMPTY in state.errors || FieldError.AMOUNT_INVALID in state.errors,
        supportingText = {
            if (FieldError.AMOUNT_EMPTY in state.errors || FieldError.AMOUNT_INVALID in state.errors) {
                Text("Nhập số tiền lớn hơn 0")
            }
        },
    )
}

@Composable
private fun CategoryGrid(
    state: AddTransactionUiState,
    onEvent: (AddTransactionEvent) -> Unit,
) {
    CategoryGridPicker(
        categories = state.availableCategories,
        selectedCategory = state.category,
        isIncome = state.type == TransactionType.INCOME,
        onCategoryChanged = { onEvent(AddTransactionEvent.CategoryChanged(it)) },
        onCreateCategory = { name, color, isIncome ->
            onEvent(AddTransactionEvent.CreateCategory(name, color, isIncome))
        },
    )
}

@Composable
private fun NoteInput(state: AddTransactionUiState, onEvent: (AddTransactionEvent) -> Unit) {
    OutlinedTextField(
        value = state.note,
        onValueChange = { onEvent(AddTransactionEvent.NoteChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Ghi chú") },
        minLines = 2,
        isError = FieldError.NOTE_TOO_LONG in state.errors,
        supportingText = { Text("${state.note.length}/200") },
    )
}

@Composable
private fun DateAndWalletRow(state: AddTransactionUiState, onEvent: (AddTransactionEvent) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        InfoCard(
            title = "Ngày",
            value = state.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString(),
            icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
            modifier = Modifier.weight(1f),
        )
        WalletCard(
            wallets = state.availableWallets,
            selectedWalletId = state.walletId,
            hasError = FieldError.WALLET_MISSING in state.errors,
            onEvent = onEvent,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.height(88.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            icon()
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletCard(
    wallets: List<Wallet>,
    selectedWalletId: Long?,
    hasError: Boolean,
    onEvent: (AddTransactionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedWallet = wallets.firstOrNull { it.id == selectedWalletId }
    var showPicker by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .height(88.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            // P1-4: cho phép bấm vào cả card để mở picker (khi có nhiều ví).
            .then(
                if (wallets.size > 1) {
                    Modifier.clickable(onClick = { showPicker = true })
                } else {
                    Modifier
                },
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null)
                Text(
                    "Ví",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // P1-4: dùng Row + chevron để rõ "bấm được". Khi 1 ví thì vẫn hiển thị tên,
            // không bấm được; khi nhiều ví thì click cả card mở WalletPickerSheet.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = selectedWallet?.name ?: "Chưa có ví",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
                if (wallets.size > 1) {
                    Icon(
                        Icons.Rounded.ArrowDropDown,
                        contentDescription = "Đổi ví",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (hasError) {
                Text(
                    "Cần chọn ví",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    if (showPicker) {
        WalletPickerSheet(
            wallets = wallets,
            selectedWalletId = selectedWalletId,
            onWalletSelected = { id -> onEvent(AddTransactionEvent.WalletChanged(id)) },
            onDismiss = { showPicker = false },
        )
    }
}
