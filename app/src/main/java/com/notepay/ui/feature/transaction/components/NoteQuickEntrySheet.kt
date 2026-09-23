package com.notepay.ui.feature.transaction.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Transaction

/**
 * ModalBottomSheet nhập ghi chú nhanh khi nhấn phím [📝] trên bàn phím tự chế.
 *
 * Đây là nơi DUY NHẤT cho phép bàn phím hệ thống xuất hiện – trong BottomSheet
 * riêng – không ảnh hưởng layout màn hình chính.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteQuickEntrySheet(
    currentNote: String,
    onNoteChanged: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var localNote by remember { mutableStateOf(currentNote) }

    ModalBottomSheet(
        onDismissRequest = {
            onNoteChanged(localNote)
            onDismiss()
        },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.transaction_field_note),
                style = MaterialTheme.typography.titleMedium,
            )

            OutlinedTextField(
                value = localNote,
                onValueChange = { if (it.length <= Transaction.MAX_NOTE_LENGTH) localNote = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.transaction_field_note_placeholder)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    onNoteChanged(localNote)
                    onDismiss()
                }),
                trailingIcon = {
                    IconButton(onClick = {
                        keyboardController?.hide()
                        onNoteChanged(localNote)
                        onDismiss()
                    }) {
                        Icon(Icons.Rounded.Check, contentDescription = null)
                    }
                },
            )

            val remaining = Transaction.MAX_NOTE_LENGTH - localNote.length
            Text(
                text = "$remaining ký tự còn lại",
                style = MaterialTheme.typography.labelSmall,
                color = if (remaining < 20) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
