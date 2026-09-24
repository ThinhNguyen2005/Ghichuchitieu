package com.notepay.ui.feature.transaction.components

import com.notepay.domain.model.Money
import androidx.compose.animation.core.Spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.feature.transaction.AmountParser
import com.notepay.ui.formatter.VietnameseMoneyWordsFormatter
import com.notepay.ui.theme.AppTheme

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

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(AppTheme.shapes.circle)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(4.dp),
    ) {
        val tabWidth = maxWidth / 2
        val indicatorOffset by animateDpAsState(
            targetValue = if (selectedIndex == 0) 0.dp else tabWidth,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            label = "IndicatorOffset",
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(AppTheme.shapes.circle)
                .background(MaterialTheme.colorScheme.primary),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "TextColor",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(AppTheme.shapes.circle)
                        .clickable(onClick = { onTypeChanged(option.first) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.second,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
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
    zeroWords: String,
    currencySuffix: String,
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
) {
    TransactionInputField(
        label = label,
        value = value,
        icon = Icons.Rounded.AccountBalanceWallet,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
fun TransactionNoteField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.length <= Transaction.MAX_NOTE_LENGTH) onValueChange(input)
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.EditNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
        maxLines = 3,
    )
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
                    androidx.compose.ui.res.stringResource(R.string.transaction_suggestion_applied_label)
                } else {
                    androidx.compose.ui.res.stringResource(R.string.transaction_suggestion_label)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(6.dp))
            CategoryAvatar(category = suggestedCategory, size = 18.dp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = suggestedCategory.displayName,
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
