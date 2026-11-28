package com.notepay.feature.subscription.main.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.button.LiquidButton
import com.notepay.core.designsystem.component.effect.BottomSheetGlass
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.NotePayAnimatedVisibility
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.shakeOnError
import com.notepay.domain.model.Category
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.subscription.main.AddSubscriptionDialogState
import kotlin.time.Instant
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
    val performHaptic = rememberNotePayHaptic()

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var attemptedSubmit by remember { mutableStateOf(false) }
    var nameTouched by remember { mutableStateOf(false) }
    var amountTouched by remember { mutableStateOf(false) }
    var nameErrorTrigger by remember { mutableIntStateOf(0) }
    var amountErrorTrigger by remember { mutableIntStateOf(0) }

    val isNameError = (attemptedSubmit || nameTouched) && state.name.isBlank()
    val isAmountInvalidWhileTyping = state.amountInput.isNotEmpty() && !state.isValidAmount
    val isAmountError = ((attemptedSubmit || amountTouched) && !state.isValidAmount) || isAmountInvalidWhileTyping

    LaunchedEffect(isAmountInvalidWhileTyping) {
        if (isAmountInvalidWhileTyping) {
            amountErrorTrigger++
        }
    }

    val tz = TimeZone.currentSystemDefault()
    val nextDueDate = remember(state.nextDueEpochMs) {
        Instant.fromEpochMilliseconds(state.nextDueEpochMs).toLocalDateTime(tz).date
    }
    val nextDueLabel = stringResource(
        R.string.subscription_date_format,
        nextDueDate.day,
        nextDueDate.month.ordinal + 1,
        nextDueDate.year,
    )

    val selectedCategory = remember(state.category) {
        Category.getAll().firstOrNull { it.id == state.category } ?: Category.OTHER
    }
    val electricityPreset = stringResource(R.string.subscription_service_electricity)
    val waterPreset = stringResource(R.string.subscription_service_water)
    val rentPreset = stringResource(R.string.subscription_service_rent)
    val popularServicePresets = remember(electricityPreset, waterPreset, rentPreset) {
        listOf(
            "Netflix" to Category.ENTERTAINMENT,
            "Spotify" to Category.ENTERTAINMENT,
            "YouTube Premium" to Category.ENTERTAINMENT,
            "iCloud" to Category.INTERNET,
            "ChatGPT Plus" to Category.INTERNET,
            electricityPreset to Category.ELECTRICITY,
            waterPreset to Category.WATER,
            rentPreset to Category.HOME,
        )
    }
    val yearlyCents = remember(state.amountInput, state.repeatMonths) {
        state.amountInput.filter(Char::isDigit).toLongOrNull()
            ?.takeIf { it > 0L && it <= Long.MAX_VALUE / 1_200L }
            ?.let { amountVnd -> amountVnd * 1_200L / state.repeatMonths.coerceAtLeast(1) }
    }

    BottomSheetGlass(
        visible = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.subscription_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    text = stringResource(R.string.subscription_popular_services),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(popularServicePresets, key = { it.first }) { (name, category) ->
                        FilterChip(
                            selected = state.name == name,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                nameTouched = true
                                onNameChanged(name)
                                onCategoryChanged(category.id)
                                onRepeatMonthsChanged(1)
                            },
                            label = { Text(name) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                if (recentTransactions.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.subscription_recent_suggestions),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 4.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(recentTransactions.take(5), key = { it.id }) { tx ->
                            val cat = tx.category
                            val noteText = tx.note.trim()
                            val categoryName = cat.localizedName()
                            Card(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable(role = Role.Button) {
                                        performHaptic(HapticFeedbackType.TextHandleMove)
                                        nameTouched = true
                                        amountTouched = true
                                        onNameChanged(tx.note.ifBlank { categoryName })
                                        onAmountChanged((tx.amount.amountInCents / 100).toString())
                                        onCategoryChanged(cat.id)
                                        onNextDueDateChanged(tx.occurredAt.toEpochMilliseconds())
                                    },
                                shape = AppTheme.shapes.corner12,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CategoryAvatar(
                                        category = cat,
                                        size = 24.dp,
                                        iconSize = 12.dp,
                                    )
                                    Column {
                                        Text(
                                            text = categoryName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 110.dp),
                                        )
                                        if (noteText.isNotBlank()) {
                                            Text(
                                                text = noteText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.widthIn(max = 110.dp),
                                            )
                                        }
                                        Text(
                                            text = MoneyFormatter.format(tx.amount),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = state.name,
                    onValueChange = {
                        nameTouched = true
                        onNameChanged(it)
                    },
                    label = { Text(stringResource(R.string.subscription_name_label)) },
                    singleLine = true,
                    isError = isNameError,
                    supportingText = {
                        NotePayAnimatedVisibility(visible = isNameError) {
                            Text(
                                text = stringResource(R.string.subscription_error_empty_name),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    shape = AppTheme.shapes.corner12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shakeOnError(nameErrorTrigger),
                )

                OutlinedTextField(
                    value = state.amountInput,
                    onValueChange = {
                        amountTouched = true
                        onAmountChanged(it)
                    },
                    label = { Text(stringResource(R.string.subscription_amount_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    visualTransformation = currencyTransformation,
                    suffix = { Text(stringResource(R.string.currency_vnd_symbol)) },
                    isError = isAmountError,
                    supportingText = {
                        NotePayAnimatedVisibility(visible = isAmountError) {
                            Text(
                                text = stringResource(R.string.feedback_transfer_invalid_amount),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    },
                    shape = AppTheme.shapes.corner12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shakeOnError(amountErrorTrigger),
                )

                yearlyCents?.let { amount ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner16,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f),
                        ),
                    ) {
                        Text(
                            text = stringResource(
                                R.string.subscription_yearly_cost_format,
                                MoneyFormatter.format(Money(amount)),
                            ),
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.subscription_category_label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    onClick = { showCategorySheet = true },
                    shape = AppTheme.shapes.corner12,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CategoryAvatar(
                            category = selectedCategory,
                            size = 32.dp,
                            iconSize = 16.dp,
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = selectedCategory.localizedName(),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.subscription_schedule_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.subscription_next_due_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                onClick = { showDatePicker = true },
                                shape = AppTheme.shapes.corner12,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = nextDueLabel,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = stringResource(R.string.subscription_choose_date),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.subscription_repeat_cycle),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(end = 4.dp),
                            ) {
                                items(Subscription.REPEAT_OPTIONS) { months ->
                                    val label = when (months) {
                                        1 -> stringResource(R.string.subscription_repeat_monthly)
                                        3 -> stringResource(R.string.subscription_repeat_quarterly)
                                        6 -> stringResource(R.string.subscription_repeat_six_months)
                                        else -> stringResource(R.string.subscription_repeat_yearly)
                                    }
                                    FilterChip(
                                        selected = state.repeatMonths == months,
                                        onClick = {
                                            performHaptic(HapticFeedbackType.TextHandleMove)
                                            onRepeatMonthsChanged(months)
                                        },
                                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.subscription_remind_before),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Subscription.REMIND_OPTIONS) { days ->
                        FilterChip(
                            selected = state.remindDaysBefore == days,
                            onClick = {
                                performHaptic(HapticFeedbackType.TextHandleMove)
                                onRemindDaysChanged(days)
                            },
                            label = {
                                Text(
                                    pluralStringResource(
                                        R.plurals.subscription_remind_days,
                                        days,
                                        days,
                                    ),
                                )
                            },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = state.note,
                    onValueChange = onNoteChanged,
                    label = { Text(stringResource(R.string.subscription_note_optional)) },
                    singleLine = true,
                    shape = AppTheme.shapes.corner12,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(4.dp))
            }

            LiquidButton(
                onClick = {
                    if (state.canSave) {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onConfirm()
                    } else {
                        attemptedSubmit = true
                        if (state.name.isBlank()) nameErrorTrigger++
                        if (!state.isValidAmount) amountErrorTrigger++
                    }
                },
                surfaceColor = if (state.canSave) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (state.canSave) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.subscription_save_reminder),
                    fontWeight = FontWeight.Bold,
                )
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
                }) { Text(stringResource(R.string.subscription_choose_date)) }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    current: Category,
    onPicked: (Category) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    NotePayModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.subscription_choose_category),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(Category.getAll(), key = { it.id }) { category ->
                    val isSelected = category == current
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(AppTheme.shapes.corner12)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onPicked(category) },
                            ),
                        colors = ListItemDefaults.colors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            },
                        ),
                        leadingContent = {
                            CategoryAvatar(
                                category = category,
                                size = 36.dp,
                                iconSize = 18.dp,
                            )
                        },
                        headlineContent = {
                            Text(
                                text = category.localizedName(),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        trailingContent = if (isSelected) {
                            {
                                Text(
                                    text = stringResource(R.string.subscription_selected),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else null,
                    )
                }
            }
        }
    }
}
