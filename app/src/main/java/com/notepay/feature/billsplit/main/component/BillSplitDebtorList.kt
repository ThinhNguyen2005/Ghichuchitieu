package com.notepay.ui.feature.billsplit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import com.notepay.ui.util.localizedName

private const val MAX_VND_INPUT_LENGTH = 15

/**
 * Bước 2 của phiếu chia hóa đơn: thẻ giao dịch đã chọn (bấm để quay lại bước 1),
 * form thêm người nợ, gợi ý tên gần đây, header danh sách + nút Chia đều,
 * và các thẻ người nợ với ô số tiền.
 *
 * Cùng kiểu extension trên `LazyListScope` (không phải @Composable) với
 * [billSplitTransactionSelectorItems] để bảo toàn tuyệt đối hành vi cuộn/key của
 * LazyColumn trong sheet. Chuỗi tiêu đề bước được hoist thành tham số vì body
 * hàm này không ở Composable scope.
 */
internal fun LazyListScope.billSplitDebtorListItems(
    step: BillSplitStep,
    selectedTx: Transaction?,
    debtors: SnapshotStateList<DebtorEntry>,
    allDebtorNames: List<String>,
    addParticipantTitle: String,
    newDebtorName: String,
    onNewDebtorNameChange: (String) -> Unit,
    onAddDebtor: (String) -> Unit,
    onSplitEvenly: () -> Unit,
    onAmountChange: (index: Int, input: String) -> Unit,
    onRemoveDebtor: (index: Int) -> Unit,
    onBackToStep1: () -> Unit,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    if (step != BillSplitStep.MANAGE_DEBTORS) return

    /*
     * Tính trực tiếp thay vì derivedStateOf: body LazyListScope extension không
     * phải Composable scope nên không gọi được remember. Danh sách gợi ý ngắn
     * (tên người nợ từng dùng) nên chi phí lọc mỗi lần compose là không đáng kể.
     */
    val availableSuggestions = allDebtorNames.filter { suggestion ->
        debtors.none { debtor ->
            debtor.name.equals(
                suggestion,
                ignoreCase = true,
            )
        }
    }

    selectedTx?.let { transaction ->
        item {
            // Bấm vào thẻ để quay lại bước 1 đổi giao dịch.
            SelectedTransactionCard(
                transaction = transaction,
                onClick = onBackToStep1,
            )
        }
    }

    item {
        BillSplitSectionTitle(
            step = 2,
            title = addParticipantTitle,
            completed = debtors.isNotEmpty(),
        )
    }

    item {
        OutlinedTextField(
            value = newDebtorName,
            onValueChange = { onNewDebtorNameChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.billsplit_person_name)) },
            placeholder = {
                Text(stringResource(R.string.billsplit_person_name_placeholder))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Group,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                IconButton(
                    // Không clearFocus: giữ bàn phím để thêm người tiếp theo.
                    onClick = { onAddDebtor(newDebtorName) },
                    enabled = newDebtorName.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = stringResource(R.string.billsplit_add_person),
                    )
                }
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAddDebtor(newDebtorName) },
            ),
            singleLine = true,
            shape = AppTheme.shapes.corner12,
        )
    }

    if (availableSuggestions.isNotEmpty()) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.billsplit_recent_suggestions),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = availableSuggestions.take(10),
                        key = { it },
                    ) { name ->
                        AssistChip(
                            onClick = { onAddDebtor(name) },
                            label = {
                                Text(
                                    text = name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    item {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.billsplit_participants),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )

                Text(
                    text = if (debtors.isEmpty()) {
                        stringResource(R.string.billsplit_no_debtor_yet)
                    } else {
                        pluralStringResource(
                            R.plurals.billsplit_debtor_added_count,
                            debtors.size,
                            debtors.size,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FilledTonalButton(
                onClick = onSplitEvenly,
                enabled = debtors.isNotEmpty(),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.billsplit_split_evenly))
            }
        }
    }

    itemsIndexed(
        items = debtors,
        key = { _, entry -> entry.name },
    ) { index, entry ->
        SelectedDebtorCard(
            entry = entry,
            onAmountChange = { input ->
                onAmountChange(index, input)
            },
            onRemove = { onRemoveDebtor(index) },
            currencyTransformation = currencyTransformation,
        )
    }
}

@Composable
private fun SelectedTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .primaryContainer
                .copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 44.dp,
                iconSize = 22.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Trước đây là một dòng chữ nhỏ trong thẻ nên rất dễ bỏ sót.
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.billsplit_change_transaction_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedDebtorCard(
    entry: DebtorEntry,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    val focusManager = LocalFocusManager.current

    val amountIsInvalid =
        entry.amountInput.isNotBlank() &&
                vndInputToCents(entry.amountInput) == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = AppTheme.shapes.circle,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.name
                                .firstOrNull()
                                ?.uppercase()
                                ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = entry.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.billsplit_delete_person_format, entry.name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = entry.amountInput,
                onValueChange = { rawInput ->
                    onAmountChange(
                        rawInput
                            .filter(Char::isDigit)
                            .take(MAX_VND_INPUT_LENGTH),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.billsplit_person_amount)) },
                placeholder = { Text("0") },
                suffix = { Text("₫") },
                supportingText = if (amountIsInvalid) {
                    { Text(stringResource(R.string.billsplit_amount_invalid)) }
                } else {
                    null
                },
                isError = amountIsInvalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                ),
                visualTransformation = currencyTransformation,
                singleLine = true,
                shape = AppTheme.shapes.corner12,
            )
        }
    }
}
