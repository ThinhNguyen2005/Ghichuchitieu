package com.notepay.feature.billsplit.main.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.formatter.PresentationDateFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.model.Transaction
import com.notepay.feature.billsplit.main.sheet.BillSplitSectionTitle
import com.notepay.feature.billsplit.main.sheet.BillSplitStep
import com.notepay.feature.category.mapper.CategoryAvatar

internal fun LazyListScope.billSplitTransactionSelectorItems(
    step: BillSplitStep,
    expenses: List<Transaction>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTx: Transaction?,
    selectedTransactionId: Long,
    chooseTransactionTitle: String,
    onPickTransaction: (Transaction) -> Unit,
    onAddExpense: () -> Unit,
) {
    if (step != BillSplitStep.SELECT_TRANSACTION) return

    item {
        BillSplitSectionTitle(
            step = 1,
            title = chooseTransactionTitle,
            completed = selectedTx != null,
        )
    }

    if (expenses.isEmpty()) {
        item {
            NoExpenseWarningCard(onAddExpense = onAddExpense)
        }
        return
    }

    if (expenses.size > 4) {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.billsplit_search_tx_placeholder),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(48.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.content_description_clear_text),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = AppTheme.shapes.corner16,
            )
        }
    }

    val trimmed = searchQuery.trim()
    val filtered = if (trimmed.isBlank()) {
        expenses
    } else {
        expenses.filter { tx ->
            tx.note.contains(trimmed, ignoreCase = true) ||
                tx.category.displayName.contains(trimmed, ignoreCase = true)
        }
    }

    items(
        items = filtered.take(30),
        key = { transaction -> transaction.id },
    ) { transaction ->
        TransactionPickerRow(
            transaction = transaction,
            selected = transaction.id == selectedTransactionId,
            onClick = { onPickTransaction(transaction) },
        )
    }
}

@Composable
private fun NoExpenseWarningCard(onAddExpense: () -> Unit) {
    val shape = AppTheme.shapes.corner20
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = false),
        shape = shape,
        color = AppTheme.colors.surface,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = AppTheme.colors.warning,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.billsplit_no_expense_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                    )
                    Text(
                        text = stringResource(R.string.billsplit_no_expense_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textSecondary,
                    )
                }
            }

            Button(
                onClick = onAddExpense,
                shape = AppTheme.shapes.capsule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.ink,
                    contentColor = AppTheme.colors.onInk,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.billsplit_add_expense_shortcut),
                    fontWeight = FontWeight.Bold,
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
    val dateText = PresentationDateFormatter.formatDate(transaction.occurredAt)
    val subtitle = if (noteText.isNotBlank()) "$noteText · $dateText" else dateText
    val shape = AppTheme.shapes.corner16

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = shape, elevated = false),
        shape = shape,
        color = AppTheme.colors.surface,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.5.dp,
            color = if (selected) AppTheme.colors.sand else AppTheme.colors.hairline,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 42.dp,
                iconSize = 20.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.textPrimary,
            )

            if (selected) {
                Surface(
                    shape = CircleShape,
                    color = AppTheme.colors.sand,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.billsplit_selecting),
                            tint = AppTheme.colors.onSand,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
