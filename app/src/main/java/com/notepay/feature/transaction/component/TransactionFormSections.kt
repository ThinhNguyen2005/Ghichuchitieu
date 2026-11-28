package com.notepay.feature.transaction.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.transaction.form.calculator.AmountParser
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeChanged: (TransactionType) -> Unit,
    expenseLabel: String,
    incomeLabel: String,
    modifier: Modifier = Modifier,
) {
    val options = listOf(TransactionType.EXPENSE to expenseLabel, TransactionType.INCOME to incomeLabel)
    val selectedIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1
    val primaryColor = AppTheme.colors.primary
    val indicatorFractionState = animateFloatAsState(
        targetValue = if (selectedIndex == 0) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "IndicatorOffset",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(AppTheme.shapes.circle)
            .background(AppTheme.colors.surfaceSunken)
            .border(0.5.dp, AppTheme.colors.hairline, AppTheme.shapes.circle)
            .padding(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
                .drawBehind {
                    val slotWidth = size.width / 2f
                    val corner = size.height / 2f
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(slotWidth * indicatorFractionState.value, 0f),
                        size = Size(slotWidth, size.height),
                        cornerRadius = CornerRadius(corner, corner),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else AppTheme.colors.textSecondary,
                    label = "TextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(AppTheme.shapes.circle)
                        .selectable(
                            selected = isSelected,
                            role = Role.Tab,
                            onClick = { onTypeChanged(option.first) },
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.second,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionAmountSection(
    amountInput: String,
    onAmountChanged: (String) -> Unit,
    focusRequester: FocusRequester,
    onRequestFocus: () -> Unit,
    @Suppress("UnusedParameter") zeroWords: String,
    @Suppress("UnusedParameter") currencySuffix: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = amountInput,
            onValueChange = { newValue ->
                if (newValue.all(Char::isDigit) && newValue.length <= AmountParser.MAX_DIGITS) {
                    onAmountChanged(newValue)
                }
            },
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRequestFocus),
        ) {
            TransactionAmountDisplay(amountInput = amountInput)
        }
    }
}

@Composable
fun TransactionDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TransactionInputField(
        label = label,
        value = value,
        icon = Icons.Rounded.CalendarMonth,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TransactionWalletField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    isError: Boolean = false,
) {
    TransactionInputField(
        label = label,
        value = value,
        icon = Icons.Rounded.AccountBalanceWallet,
        onClick = onClick,
        modifier = modifier,
        supportingText = supportingText,
        isError = isError,
    )
}

@Composable
fun TransactionNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    @Suppress("UnusedParameter") label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
) {
    val focusManager = LocalFocusManager.current
    val isLight = AppTheme.colors.isLight
    val shape = AppTheme.shapes.corner20

    Column(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = shape,
            color = AppTheme.colors.surface.copy(alpha = if (isLight) 0.94f else 0.88f),
            border = BorderStroke(
                width = if (isError) 1.dp else 0.5.dp,
                color = if (isError) AppTheme.colors.error else AppTheme.colors.hairline,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .iosCardShadow(shape = shape, elevated = false)
                .semantics {
                    if (isError && !errorText.isNullOrBlank()) {
                        error(errorText)
                    }
                },
        ) {
            TextField(
                value = value,
                onValueChange = { input ->
                    if (input.length <= Transaction.MAX_NOTE_LENGTH) onValueChange(input)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = AppTheme.colors.textMuted,
                    )
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = AppTheme.colors.textPrimary,
                ),
                minLines = 2,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = AppTheme.colors.primary,
                ),
            )
        }
        if (isError && !errorText.isNullOrBlank()) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

@Composable
fun TransactionSuggestionChipRow(
    suggestedCategory: Category,
    reason: String?,
    isApplied: Boolean,
    onSelect: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelect(suggestedCategory) }
            .padding(vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (isApplied) {
                    stringResource(R.string.transaction_suggestion_applied_label)
                } else {
                    stringResource(R.string.transaction_suggestion_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            CategoryAvatar(category = suggestedCategory, size = 18.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suggestedCategory.localizedName(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(suggestedCategory.colorArgb),
            )
        }
        reason?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}
