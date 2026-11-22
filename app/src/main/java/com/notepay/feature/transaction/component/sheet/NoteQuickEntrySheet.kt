package com.notepay.feature.transaction.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.formatter.TransactionDateLabelFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.feature.transaction.form.NaturalEntryUiState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * ModalBottomSheet nhập ghi chú hoặc tách nhanh đa giao dịch từ câu tiếng Việt tự nhiên.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteQuickEntrySheet(
    currentNote: String,
    onNoteChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    enableNaturalParsing: Boolean = false,
    naturalEntry: NaturalEntryUiState = NaturalEntryUiState(),
    availableCategories: List<Category> = Category.getAll(),
    onNaturalInputChanged: (String) -> Unit = {},
    onAnalyzeWithAi: () -> Unit = {},
    onUpdateDraft: (NaturalTransactionDraft) -> Unit = {},
    onRemoveDraft: (String) -> Unit = {},
    onSaveAllDrafts: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var localNote by remember(currentNote) { mutableStateOf(currentNote) }

    ModalBottomSheet(
        onDismissRequest = {
            if (!enableNaturalParsing) {
                onNoteChanged(localNote)
            } else {
                onNoteChanged(naturalEntry.rawInput.take(Transaction.MAX_NOTE_LENGTH))
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = AppTheme.colors.surface,
        shape = AppTheme.shapes.sheetTop,
        scrimColor = Color.Black.copy(alpha = 0.45f),
    ) {
        if (!enableNaturalParsing) {
            SimpleNoteEntryContent(
                localNote = localNote,
                onLocalNoteChange = { if (it.length <= Transaction.MAX_NOTE_LENGTH) localNote = it },
                focusRequester = focusRequester,
                onConfirm = {
                    keyboardController?.hide()
                    onNoteChanged(localNote)
                    onDismiss()
                },
            )
        } else {
            NaturalQuickEntryContent(
                naturalEntry = naturalEntry,
                availableCategories = availableCategories,
                focusRequester = focusRequester,
                onNaturalInputChanged = onNaturalInputChanged,
                onAnalyzeWithAi = {
                    keyboardController?.hide()
                    onAnalyzeWithAi()
                },
                onUpdateDraft = onUpdateDraft,
                onRemoveDraft = onRemoveDraft,
                onSaveAllDrafts = {
                    keyboardController?.hide()
                    onSaveAllDrafts()
                },
                onApplyNoteOnly = {
                    keyboardController?.hide()
                    onNoteChanged(naturalEntry.rawInput.trim().take(Transaction.MAX_NOTE_LENGTH))
                    onDismiss()
                },
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun SimpleNoteEntryContent(
    localNote: String,
    onLocalNoteChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.transaction_field_note),
            style = MaterialTheme.typography.titleMedium,
            color = AppTheme.colors.textPrimary,
        )

        OutlinedTextField(
            value = localNote,
            onValueChange = onLocalNoteChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text(stringResource(R.string.transaction_field_note_placeholder)) },
            shape = AppTheme.shapes.input,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() }),
            trailingIcon = {
                IconButton(onClick = onConfirm) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.action_done),
                    )
                }
            },
        )

        val remaining = Transaction.MAX_NOTE_LENGTH - localNote.length
        Text(
            text = pluralStringResource(R.plurals.characters_remaining, remaining, remaining),
            style = MaterialTheme.typography.labelSmall,
            color = if (remaining < 20) AppTheme.colors.error else AppTheme.colors.textSecondary,
        )
    }
}

@Composable
private fun NaturalQuickEntryContent(
    naturalEntry: NaturalEntryUiState,
    availableCategories: List<Category>,
    focusRequester: FocusRequester,
    onNaturalInputChanged: (String) -> Unit,
    onAnalyzeWithAi: () -> Unit,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onRemoveDraft: (String) -> Unit,
    onSaveAllDrafts: () -> Unit,
    onApplyNoteOnly: () -> Unit,
) {
    var editingDraftId by remember { mutableStateOf<String?>(null) }
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    val keyboardController = LocalSoftwareKeyboardController.current
    val todayPrefix = stringResource(R.string.date_today_prefix)
    val yesterdayPrefix = stringResource(R.string.date_yesterday_prefix)
    val monthSuffixFmt = stringResource(R.string.date_month_suffix)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.natural_entry_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        OutlinedTextField(
            value = naturalEntry.rawInput,
            onValueChange = onNaturalInputChanged,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = {
                Text(
                    text = stringResource(R.string.natural_entry_placeholder),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            shape = AppTheme.shapes.input,
            minLines = 2,
            maxLines = 3,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                if (naturalEntry.drafts.isEmpty()) onApplyNoteOnly()
            }),
        )

        if (naturalEntry.drafts.isNotEmpty()) {
            PreviewHeaderRow(
                draftCount = naturalEntry.drafts.size,
                source = naturalEntry.source,
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = naturalEntry.drafts, key = { it.id }) { draft ->
                    val dateText = remember(draft.date, todayPrefix, yesterdayPrefix, monthSuffixFmt) {
                        TransactionDateLabelFormatter.format(
                            target = draft.date,
                            today = today,
                            todayPrefix = todayPrefix,
                            yesterdayPrefix = yesterdayPrefix,
                            monthSuffixFormat = monthSuffixFmt,
                        )
                    }
                    NaturalDraftPreviewCard(
                        draft = draft,
                        dateLabel = dateText,
                        isEditing = editingDraftId == draft.id,
                        availableCategories = availableCategories,
                        onToggleEdit = {
                            editingDraftId = if (editingDraftId == draft.id) null else draft.id
                        },
                        onUpdateDraft = onUpdateDraft,
                        onRemoveDraft = {
                            if (editingDraftId == draft.id) editingDraftId = null
                            onRemoveDraft(draft.id)
                        },
                    )
                }
            }
        }

        if (naturalEntry.showAiFallbackNotice) {
            Text(
                text = stringResource(R.string.natural_entry_ai_fallback_notice),
                style = MaterialTheme.typography.bodySmall,
                color = AppTheme.colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (naturalEntry.isAiAvailable && naturalEntry.rawInput.isNotBlank()) {
            DeepAiAnalyzeButton(
                isLoading = naturalEntry.isAiLoading,
                highlighted = naturalEntry.needsAiFallback,
                onClick = onAnalyzeWithAi,
            )
        }

        if (naturalEntry.drafts.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onApplyNoteOnly,
                    enabled = !naturalEntry.isSavingBatch,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(
                        text = stringResource(R.string.natural_entry_apply_note),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Button(
                    onClick = onSaveAllDrafts,
                    enabled = !naturalEntry.isSavingBatch && !naturalEntry.isAiLoading,
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.ink,
                        contentColor = AppTheme.colors.onInk,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    if (naturalEntry.isSavingBatch) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AppTheme.colors.onInk,
                        )
                    } else {
                        Text(
                            text = pluralStringResource(R.plurals.natural_entry_save_all, naturalEntry.drafts.size, naturalEntry.drafts.size),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        } else {
            OutlinedButton(
                onClick = onApplyNoteOnly,
                shape = AppTheme.shapes.capsule,
                border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_done),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PreviewHeaderRow(
    draftCount: Int,
    source: NaturalParseSource,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.natural_entry_preview_header, draftCount),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppTheme.colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.size(8.dp))
        val isAi = source == NaturalParseSource.AI_FALLBACK
        Surface(
            color = if (isAi) AppTheme.colors.sand else AppTheme.colors.surfaceSunken,
            contentColor = if (isAi) AppTheme.colors.onSand else AppTheme.colors.textSecondary,
            shape = AppTheme.shapes.capsule,
        ) {
            Text(
                text = stringResource(
                    if (isAi) R.string.natural_entry_source_ai else R.string.natural_entry_source_device,
                ),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun DeepAiAnalyzeButton(
    isLoading: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (highlighted) AppTheme.colors.sand else AppTheme.colors.surfaceRaised
    val contentColor = if (highlighted) AppTheme.colors.onSand else AppTheme.colors.textPrimary
    val borderColor = if (highlighted) AppTheme.colors.sand else AppTheme.colors.borderStrong

    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        shape = AppTheme.shapes.capsule,
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = contentColor,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.natural_entry_ai_analyzing),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.natural_entry_ai_button),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun NaturalDraftPreviewCard(
    draft: NaturalTransactionDraft,
    dateLabel: String,
    isEditing: Boolean,
    availableCategories: List<Category>,
    onToggleEdit: () -> Unit,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onRemoveDraft: () -> Unit,
) {
    Surface(
        color = AppTheme.colors.surfaceRaised,
        shape = AppTheme.shapes.corner20,
        border = BorderStroke(1.dp, AppTheme.colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = MoneyFormatter.format(Money.fromMajorUnit(draft.amountVnd)),
                        style = AppTheme.typography.amountRow,
                        color = if (draft.type == TransactionType.INCOME) {
                            AppTheme.colors.success
                        } else {
                            AppTheme.colors.error
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = draft.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppTheme.colors.textPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            color = AppTheme.colors.surfaceSunken,
                            shape = AppTheme.shapes.capsule,
                        ) {
                            Text(
                                text = draft.category.localizedName(),
                                style = MaterialTheme.typography.labelMedium,
                                color = AppTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = AppTheme.colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                IconButton(
                    onClick = onToggleEdit,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.natural_entry_edit_cd),
                        tint = AppTheme.colors.textSecondary,
                    )
                }

                IconButton(
                    onClick = onRemoveDraft,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.natural_entry_delete_cd),
                        tint = AppTheme.colors.textSecondary,
                    )
                }
            }

            if (isEditing) {
                DraftInlineEditor(
                    draft = draft,
                    availableCategories = availableCategories,
                    onUpdateDraft = onUpdateDraft,
                    onDoneEdit = onToggleEdit,
                )
            }
        }
    }
}

@Composable
private fun DraftInlineEditor(
    draft: NaturalTransactionDraft,
    availableCategories: List<Category>,
    onUpdateDraft: (NaturalTransactionDraft) -> Unit,
    onDoneEdit: () -> Unit,
) {
    var amountText by remember(draft.id) { mutableStateOf(draft.amountVnd.toString()) }
    var noteText by remember(draft.id) { mutableStateOf(draft.note) }
    val matchingCategories = remember(availableCategories, draft.type) {
        val isIncome = draft.type == TransactionType.INCOME
        availableCategories.filter { it.isIncome == isIncome }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                val digits = raw.filter { it.isDigit() }.take(12)
                amountText = digits
                val parsed = digits.toLongOrNull()
                if (parsed != null && parsed in 1..Money.MAX_MAJOR_UNITS) {
                    onUpdateDraft(draft.copy(amountVnd = parsed))
                }
            },
            label = { Text(stringResource(R.string.natural_entry_amount_label)) },
            shape = AppTheme.shapes.input,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = noteText,
            onValueChange = { raw ->
                val capped = raw.take(Transaction.MAX_NOTE_LENGTH)
                noteText = capped
                if (capped.isNotBlank()) {
                    onUpdateDraft(draft.copy(note = capped))
                }
            },
            label = { Text(stringResource(R.string.natural_entry_note_label)) },
            shape = AppTheme.shapes.input,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            matchingCategories.forEach { cat ->
                val selected = cat.id == draft.category.id
                FilterChip(
                    selected = selected,
                    onClick = { onUpdateDraft(draft.copy(category = cat)) },
                    label = {
                        Text(
                            text = cat.localizedName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    shape = AppTheme.shapes.capsule,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AppTheme.colors.sand,
                        selectedLabelColor = AppTheme.colors.onSand,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = onDoneEdit,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.natural_entry_done_edit))
            }
        }
    }
}
