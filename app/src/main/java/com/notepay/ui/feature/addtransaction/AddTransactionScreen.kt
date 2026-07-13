@file:Suppress("DEPRECATION")
package com.notepay.ui.feature.addtransaction

import com.notepay.ui.theme.AppTheme
import com.notepay.ui.component.LiquidButton

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.*
import kotlinx.datetime.Instant
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

    val showDatePicker = remember { mutableStateOf(false) }
    val showWalletPicker = remember { mutableStateOf(false) }
    val showAllCategories = remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current

    // Resolve strings trước khi dùng (đặc biệt cho các callback ngoài Composable scope).
    val title = stringResource(R.string.add_transaction_title)
    val backCd = stringResource(R.string.action_back)
    val fieldDate = stringResource(R.string.transaction_field_date)
    val fieldWallet = stringResource(R.string.transaction_field_wallet)
    val fieldNote = stringResource(R.string.transaction_field_note)
    val notePlaceholder = stringResource(R.string.transaction_field_note_placeholder)
    val noWalletSelected = stringResource(R.string.transaction_no_wallet_selected)
    val noOtherWalletToast = stringResource(R.string.transaction_no_other_wallet)
    val saveLabel = stringResource(R.string.transaction_save_button)
    val cancelLabel = stringResource(R.string.action_cancel)
    val doneLabel = stringResource(R.string.action_done)
    val expenseLabel = stringResource(R.string.transaction_type_expense)
    val incomeLabel = stringResource(R.string.transaction_type_income)
    val todayPrefix = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt = stringResource(R.string.date_month_suffix)
    val zeroWords = stringResource(R.string.number_words_zero)
    val currencySuffix = stringResource(R.string.number_words_currency_suffix)

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) {
            viewModel.onEvent(AddTransactionEvent.Reset)
            onSaved()
        }
    }

    // Tự động yêu cầu focus vào ô số tiền để kích hoạt mở bàn phím mặc định của hệ thống
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = backCd)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Phần trên: Form có thể cuộn
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TransactionTypeSelector(
                    selectedType = state.type,
                    onEvent = viewModel::onEvent,
                    expenseLabel = expenseLabel,
                    incomeLabel = incomeLabel,
                )

                // Nhập số tiền: Thiết kế nổi bật to ở giữa, có BasicTextField ẩn và text đọc chữ mờ bên dưới
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    BasicTextField(
                        value = state.amountInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } && newValue.length <= 15) {
                                viewModel.onEvent(AddTransactionEvent.AmountChanged(newValue))
                            }
                        },
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                    ) {
                        TransactionAmountDisplay(
                            amountInput = state.amountInput
                        )

                        // Đọc số tiền bằng chữ mờ bên dưới
                        val amountLong = remember(state.amountInput) { state.amountInput.toLongOrNull() ?: 0L }
                        val amountInWords = remember(amountLong, currencySuffix, zeroWords) {
                            if (amountLong > 0L) convertNumberToVietnameseWords(amountLong, zeroWords) + " " + currencySuffix else ""
                        }
                        if (amountInWords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = amountInWords,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                // Gợi ý danh mục thời gian thực (nếu có)
                val suggested = state.suggestedCategory
                if (suggested != null && suggested != state.category) {
                    SuggestionChipRow(
                        suggestedCategory = suggested,
                        onSelect = { viewModel.onEvent(AddTransactionEvent.CategoryChanged(it)) }
                    )
                }

                // Dải chọn nhanh danh mục
                CategoryQuickSelectionRow(
                    categories = state.availableCategories,
                    selectedCategory = state.category,
                    isIncome = state.type == TransactionType.INCOME,
                    onCategoryChanged = { viewModel.onEvent(AddTransactionEvent.CategoryChanged(it)) },
                    onSeeAllClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showAllCategories.value = true
                    }
                )

                // Ô chọn ngày giờ
                val dateLabel = remember(state.occurredAt, todayPrefix, yesterdayPrefix, monthSuffixFmt) {
                    formatDateLabel(state.occurredAt, todayPrefix, yesterdayPrefix, monthSuffixFmt)
                }
                TransactionInputField(
                    label = fieldDate,
                    value = dateLabel,
                    icon = Icons.Rounded.CalendarMonth,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        showDatePicker.value = true
                    }
                )

                // Ô chọn ví - Click vào hiện thông báo nhẹ nếu chỉ có 1 ví
                val selectedWallet = state.availableWallets.firstOrNull { it.id == state.walletId }
                val walletName = selectedWallet?.name ?: noWalletSelected
                TransactionInputField(
                    label = fieldWallet,
                    value = walletName,
                    icon = Icons.Rounded.AccountBalanceWallet,
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        if (state.availableWallets.size > 1) {
                            showWalletPicker.value = true
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                noOtherWalletToast,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )

                // Ghi chú trực tiếp (Inline TextField)
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { if (it.length <= 200) viewModel.onEvent(AddTransactionEvent.NoteChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(fieldNote) },
                    placeholder = { Text(notePlaceholder) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = AppTheme.shapes.corner16,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Phần dưới: Nút lưu giao dịch (Ẩn bàn phím tự chế theo yêu cầu người dùng)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Chú thích: Đã triển khai bàn phím tự chế (NumericKeypad) nhưng hiện tại tạm dừng sử dụng để dùng bàn phím hệ thống.

                // Nút Lưu giao dịch nổi bật động theo canSave
                LiquidButton(
                    onClick = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.onEvent(AddTransactionEvent.Save)
                    },
                    enabled = state.canSave,
                    tint = Color(0xFF1B7F4F),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = if (state.canSave) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            Text(saveLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Các BottomSheet và Dialog picker
    if (showDatePicker.value) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.occurredAt.toEpochMilliseconds())
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
        ModalBottomSheet(
            onDismissRequest = { showAllCategories.value = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                CategoryGridPicker(
                    categories = state.availableCategories,
                    selectedCategory = state.category,
                    isIncome = state.type == TransactionType.INCOME,
                    onCategoryChanged = {
                        viewModel.onEvent(AddTransactionEvent.CategoryChanged(it))
                        showAllCategories.value = false
                    },
                    onCreateCategory = { name, color, isIncome ->
                        viewModel.onEvent(AddTransactionEvent.CreateCategory(name, color, isIncome))
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeSelector(
    selectedType: TransactionType,
    onEvent: (AddTransactionEvent) -> Unit,
    expenseLabel: String,
    incomeLabel: String,
) {
    val options = listOf(TransactionType.EXPENSE to expenseLabel, TransactionType.INCOME to incomeLabel)
    val selectedIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppTheme.shapes.circle)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp)
    ) {
        val width = maxWidth
        val tabWidth = width / 2

        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 0.dp else tabWidth,
            animationSpec = androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            ),
            label = "IndicatorOffset"
        )

        // Khối màu nền trượt di chuyển mượt mà
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppTheme.shapes.circle)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "TextColor"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AppTheme.shapes.circle)
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) {
                            onEvent(AddTransactionEvent.TypeChanged(option.first))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.second,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SuggestionChipRow(
    suggestedCategory: Category,
    onSelect: (Category) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(suggestedCategory) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        val suggestionLabel = stringResource(R.string.transaction_suggestion_label)
        Text(
            text = suggestionLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        CategoryAvatar(category = suggestedCategory, size = 18.dp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = suggestedCategory.displayName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(suggestedCategory.colorArgb)
        )
    }
}

private fun formatDateLabel(
    instant: Instant,
    todayPrefix: String,
    yesterdayPrefix: String,
    monthSuffixFmt: String,
): String {
    val systemTz = TimeZone.currentSystemDefault()
    val now = kotlin.time.Clock.System.now().toLocalDateTime(systemTz).date
    val target = instant.toLocalDateTime(systemTz).date
    val isToday = now == target
    val isYesterday = now.toEpochDays() - target.toEpochDays() == 1L

    val prefix = when {
        isToday -> todayPrefix
        isYesterday -> yesterdayPrefix
        else -> ""
    }

    return "${prefix}${target.day} ${monthSuffixFmt.format(target.month.ordinal + 1)}"
}

private fun convertNumberToVietnameseWords(number: Long, zeroWord: String): String {
    if (number == 0L) return zeroWord
    
    val units = listOf("", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")
    
    fun readThreeDigits(n: Int, showZeroHundred: Boolean): String {
        val hundred = n / 100
        val ten = (n % 100) / 10
        val unit = n % 10
        
        if (hundred == 0 && ten == 0 && unit == 0) return ""
        
        val sb = StringBuilder()
        if (hundred > 0 || showZeroHundred) {
            sb.append(units[hundred]).append(" trăm ")
        }
        
        if (ten > 0) {
            if (ten == 1) {
                sb.append("mười ")
            } else {
                sb.append(units[ten]).append(" mươi ")
            }
            if (unit > 0) {
                when {
                    unit == 1 && ten > 1 -> sb.append("mốt ")
                    unit == 5 -> sb.append("lăm ")
                    unit == 4 && ten > 1 -> sb.append("tư ")
                    else -> sb.append(units[unit])
                }
            }
        } else {
            if ((hundred > 0 || showZeroHundred) && unit > 0) {
                sb.append("lẻ ").append(units[unit])
            } else if (unit > 0) {
                sb.append(units[unit])
            }
        }
        return sb.toString().trim()
    }
    
    var temp = number
    val billions = (temp / 1_000_000_000).toInt()
    temp %= 1_000_000_000
    val millions = (temp / 1_000_000).toInt()
    temp %= 1_000_000
    val thousands = (temp / 1_000).toInt()
    val remaining = (temp % 1_000).toInt()
    
    val result = StringBuilder()
    
    if (billions > 0) {
        result.append(readThreeDigits(billions, false)).append(" tỷ ")
    }
    if (millions > 0) {
        result.append(readThreeDigits(millions, billions > 0)).append(" triệu ")
    }
    if (thousands > 0) {
        result.append(readThreeDigits(thousands, billions > 0 || millions > 0)).append(" nghìn ")
    }
    if (remaining > 0) {
        result.append(readThreeDigits(remaining, billions > 0 || millions > 0 || thousands > 0))
    }
    
    val words = result.toString().trim().replace("\\s+".toRegex(), " ")
    if (words.isEmpty()) return ""
    return words.substring(0, 1).uppercase() + words.substring(1)
}
