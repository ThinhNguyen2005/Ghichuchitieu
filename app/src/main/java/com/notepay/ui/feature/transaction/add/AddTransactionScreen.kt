package com.notepay.ui.feature.transaction.add

import com.notepay.ui.theme.AppTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.*
import com.notepay.ui.feature.transaction.components.*
import com.notepay.ui.formatter.TransactionDateLabelFormatter
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    val showDatePicker    = remember { mutableStateOf(false) }
    val showWalletPicker  = remember { mutableStateOf(false) }
    val showAllCategories = remember { mutableStateOf(false) }
    val showNoteSheet     = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.onEvent(AddTransactionEvent.ImageSelected(it)) }
    }

    // ── Strings ───────────────────────────────────────────────────────────────
    val title              = stringResource(R.string.add_transaction_title)
    val backCd             = stringResource(R.string.action_back)
    val fieldWallet        = stringResource(R.string.transaction_field_wallet)
    val noWalletSelected   = stringResource(R.string.transaction_no_wallet_selected)
    val noOtherWalletToast = stringResource(R.string.transaction_no_other_wallet)
    val cancelLabel        = stringResource(R.string.action_cancel)
    val validationAmount   = stringResource(R.string.transaction_validation_amount)
    val validationWallet   = stringResource(R.string.transaction_validation_wallet)
    val validationAmountWallet = stringResource(R.string.transaction_validation_amount_wallet)
    val validationForm     = stringResource(R.string.transaction_validation_form)
    val doneLabel          = stringResource(R.string.action_done)
    val expenseLabel       = stringResource(R.string.transaction_type_expense)
    val incomeLabel        = stringResource(R.string.transaction_type_income)
    val todayPrefix        = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix    = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt     = stringResource(R.string.date_month_suffix)

    val snackbarHostState = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            if (feedback.type == com.notepay.ui.feedback.FeedbackType.Success) {
                onSaved()
            } else {
                snackbarHostState.showSnackbar(feedback.message)
            }
        }
    }

    val currentAmount = state.amount
    val amountMissing = currentAmount == null || currentAmount.amountInCents <= 0
    val validationMessage = when {
        amountMissing && state.walletId == null -> validationAmountWallet
        amountMissing -> validationAmount
        state.walletId == null -> validationWallet
        else -> validationForm
    }

    val systemTimeZone = TimeZone.currentSystemDefault()
    val dateLabel = remember(state.occurredAt, todayPrefix, yesterdayPrefix, monthSuffixFmt) {
        TransactionDateLabelFormatter.format(
            target = state.occurredAt.toLocalDateTime(systemTimeZone).date,
            today = kotlin.time.Clock.System.now().toLocalDateTime(systemTimeZone).date,
            todayPrefix = todayPrefix,
            yesterdayPrefix = yesterdayPrefix,
            monthSuffixFormat = monthSuffixFmt,
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            GradientTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backCd)
                    }
                },
                actions = {
                    // Nút quét ảnh giao dịch nằm trên TopBar
                    IconButton(
                        onClick = {
                            imagePicker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        enabled = !state.isImageScanning,
                    ) {
                        if (state.isImageScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PhotoLibrary,
                                contentDescription = stringResource(R.string.scan_image_button),
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // ── Scrollable content (không bao giờ bị keypad che vì weight(1f)) ─
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Loại giao dịch
                TransactionTypeSelector(
                    selectedType = state.type,
                    onTypeChanged = { viewModel.onEvent(AddTransactionEvent.TypeChanged(it)) },
                    expenseLabel = expenseLabel,
                    incomeLabel = incomeLabel,
                )

                // Thông báo từ OCR nếu có
                state.imageScanMessage?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Hiển thị số tiền (read-only, do custom keypad điều khiển)
                TransactionAmountDisplay(amountInput = state.displayExpression)

                // Gợi ý danh mục thời gian thực
                val suggested = state.suggestedCategory
                if (suggested != null) {
                    TransactionSuggestionChipRow(
                        suggestedCategory = suggested,
                        reason = state.suggestionReason,
                        isApplied = suggested == state.category,
                        onSelect = { viewModel.onEvent(AddTransactionEvent.CategoryChanged(it)) }
                    )
                }

                // Chọn nhanh danh mục
                CategoryQuickSelectionRow(
                    categories = state.availableCategories,
                    selectedCategory = state.category,
                    isIncome = state.type == TransactionType.INCOME,
                    onCategoryChanged = { viewModel.onEvent(AddTransactionEvent.CategoryChanged(it)) },
                    onSeeAllClick = { showAllCategories.value = true }
                )

                // Chọn ví
                val selectedWallet = state.availableWallets.firstOrNull { it.id == state.walletId }
                TransactionWalletField(
                    label = fieldWallet,
                    value = selectedWallet?.name ?: noWalletSelected,
                    onClick = {
                        if (state.availableWallets.size > 1) {
                            showWalletPicker.value = true
                        } else {
                            android.widget.Toast.makeText(
                                context, noOtherWalletToast, android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                // Ghi chú hiển thị tóm tắt (chỉ đọc) — nhập qua phím 📝 trên keypad
                if (state.note.isNotBlank()) {
                    NotePreviewChip(
                        note = state.note,
                        onClick = { showNoteSheet.value = true },
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── Bàn phím tự chế 4×5 cố định đáy — không che content phía trên ─
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                ) {
                    TransactionCustomKeypad(
                        dateLabel = dateLabel,
                        noteLabel = state.note.take(14).ifBlank { "" },
                        canSave = state.canSave,
                        isSaving = state.isSaving,
                        onKey = { key ->
                            when (key) {
                                CalcKey.Date -> showDatePicker.value = true
                                CalcKey.Note -> showNoteSheet.value = true
                                CalcKey.Save -> {
                                    if (state.canSave) {
                                        viewModel.onEvent(AddTransactionEvent.Save)
                                    } else {
                                        viewModel.onEvent(AddTransactionEvent.Save)
                                        screenScope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(validationMessage)
                                        }
                                    }
                                }
                                else -> viewModel.onEvent(AddTransactionEvent.CalcKeyPressed(key))
                            }
                        },
                        onBackspaceLong = { viewModel.onEvent(AddTransactionEvent.BackspaceLong) },
                    )
                }
            }
        }
    }

    // ── Dialogs & Sheets ──────────────────────────────────────────────────────

    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.occurredAt.toEpochMilliseconds()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        viewModel.onEvent(AddTransactionEvent.DateChanged(Instant.fromEpochMilliseconds(millis)))
                    }
                    showDatePicker.value = false
                }) { Text(doneLabel) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) { Text(cancelLabel) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showWalletPicker.value) {
        WalletPickerSheet(
            wallets = state.availableWallets,
            selectedWalletId = state.walletId,
            onWalletSelected = { id -> viewModel.onEvent(AddTransactionEvent.WalletChanged(id)) },
            onDismiss = { showWalletPicker.value = false },
        )
    }

    if (showAllCategories.value) {
        CategoryPickerSheet(
            categories = state.availableCategories,
            selectedCategory = state.category,
            isIncome = state.type == TransactionType.INCOME,
            onCategoryChanged = {
                viewModel.onEvent(AddTransactionEvent.CategoryChanged(it))
                showAllCategories.value = false
            },
            onDismiss = { showAllCategories.value = false },
            onCreateCategory = { name, color, iconId, isIncome ->
                viewModel.onEvent(AddTransactionEvent.CreateCategory(name, color, iconId, isIncome))
            },
        )
    }

    if (showNoteSheet.value) {
        NoteQuickEntrySheet(
            currentNote = state.note,
            onNoteChanged = { viewModel.onEvent(AddTransactionEvent.NoteChanged(it)) },
            onDismiss = { showNoteSheet.value = false },
        )
    }
}
