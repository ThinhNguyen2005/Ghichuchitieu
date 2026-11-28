package com.notepay.feature.transaction.component.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.model.Transaction

/**
 * ModalBottomSheet nhập ghi chú ngắn cho một giao dịch đơn lẻ trong TransactionForm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteQuickEntrySheet(
    currentNote: String,
    onNoteChanged: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var localNote by remember(currentNote) { mutableStateOf(currentNote) }

    NotePayModalBottomSheet(
        onDismissRequest = {
            onNoteChanged(localNote)
            onDismiss()
        },
        sheetState = sheetState,
        modifier = modifier,
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
                onValueChange = { if (it.length <= Transaction.MAX_NOTE_LENGTH) localNote = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.transaction_field_note_placeholder)) },
                shape = AppTheme.shapes.input,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onNoteChanged(localNote)
                        onDismiss()
                    },
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onNoteChanged(localNote)
                            onDismiss()
                        },
                    ) {
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
