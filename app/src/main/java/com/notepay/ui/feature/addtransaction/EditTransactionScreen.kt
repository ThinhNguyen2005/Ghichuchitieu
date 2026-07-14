package com.notepay.ui.feature.addtransaction

import com.notepay.ui.theme.AppTheme
import com.notepay.ui.component.LiquidButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.TransactionType
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    onSaved: suspend (UiFeedback) -> Unit,
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: EditTransactionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            if (feedback.type == FeedbackType.Success) {
                onSaved(feedback)
            } else {
                onFeedback(feedback)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.isAutoCapture) "Chi tiết giao dịch" else "Chỉnh sửa giao dịch")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
            )
        },
    ) { padding ->

        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Nhãn read-only nếu giao dịch do ngân hàng bắt
            if (state.isAutoCapture) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Column {
                                Text(
                                    "Giao dịch được bắt tự động",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                                Text(
                                    "Giao dịch từ ngân hàng không thể chỉnh sửa để đảm bảo tính toàn vẹn dữ liệu.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            // Số tiền
            item {
                val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = { viewModel.onAmountChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Số tiền") },
                    singleLine = true,
                    readOnly = state.isAutoCapture,
                    enabled = !state.isAutoCapture,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = currencyTransformation,
                    suffix = { Text("đ") },
                )
            }

            // Danh mục
            item {
                CategoryGridPicker(
                    categories = state.availableCategories,
                    selectedCategory = state.category,
                    isIncome = state.type == TransactionType.INCOME,
                    onCategoryChanged = { viewModel.onCategoryChanged(it) },
                    // Auto-capture: read-only, không cho thêm danh mục mới
                    onCreateCategory = if (state.isAutoCapture) null else { name, color, iconId, isIncome ->
                        viewModel.createCategory(name, color, iconId, isIncome)
                    },
                )
            }

            // Ghi chú
            item {
                val suggestedCategory = state.suggestedCategory
                Column(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !state.isAutoCapture && suggestedCategory != null && suggestedCategory != state.category,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically(),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { suggestedCategory?.let { viewModel.onCategoryChanged(it) } }
                                .background(
                                    color = androidx.compose.ui.graphics.Color(suggestedCategory?.colorArgb ?: 0L).copy(alpha = 0.12f),
                                    shape = AppTheme.shapes.corner12
                                )
                                .border(
                                    width = 1.dp,
                                    color = androidx.compose.ui.graphics.Color(suggestedCategory?.colorArgb ?: 0L).copy(alpha = 0.4f),
                                    shape = AppTheme.shapes.corner12
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "💡 Đề xuất danh mục:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            com.notepay.ui.component.CategoryAvatar(category = suggestedCategory!!, size = 20.dp)
                            Text(
                                text = suggestedCategory.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = androidx.compose.ui.graphics.Color(suggestedCategory.colorArgb)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = state.note,
                        onValueChange = { viewModel.onNoteChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ghi chú") },
                        minLines = 2,
                        readOnly = state.isAutoCapture,
                        enabled = !state.isAutoCapture,
                        supportingText = { if (!state.isAutoCapture) Text("${state.note.length}/200") },
                    )
                }
            }

            // P2-14: thay Text ngày bằng AssistChip có icon CalendarMonth,
            // nhấn vào sẽ mở DatePicker.
            item {
                var showDatePicker by remember { mutableStateOf(false) }
                AssistChip(
                    onClick = { showDatePicker = true },
                    enabled = !state.isAutoCapture,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    label = {
                        Text(
                            text = "Ngày: ${state.dateLabel}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                )

                if (showDatePicker) {
                    val initialMillis = state.date
                        ?.toEpochDays()?.toLong()?.times(86_400_000L) ?: 0L
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val picked = LocalDate.fromEpochDays((millis / 86_400_000L).toInt())
                                    viewModel.onDateChanged(picked)
                                }
                                showDatePicker = false
                            }) { Text("Xong") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
                        },
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }

            // Nút Lưu
            item {
                Spacer(Modifier.height(4.dp))
                LiquidButton(
                    onClick = { viewModel.save() },
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Đang lưu...")
                    } else {
                        Text("Lưu thay đổi")
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}
