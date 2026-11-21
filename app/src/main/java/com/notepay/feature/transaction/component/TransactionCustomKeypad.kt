package com.notepay.feature.transaction.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme

// ─── Key model ────────────────────────────────────────────────────────────────

sealed interface CalcKey {
    data class Digit(val value: Int) : CalcKey
    data class Operator(val symbol: Char) : CalcKey
    data object Equals     : CalcKey
    data object ThreeZeros : CalcKey  // .000 shortcut
    data object Backspace  : CalcKey
    data object Date       : CalcKey
    data object Note       : CalcKey
    data object Save       : CalcKey
}

// ─── Layout: 4 rows × 5 cols ──────────────────────────────────────────────────
//
//  [ 1 ][ 2 ][ 3 ][ + ][ Date  ]
//  [ 4 ][ 5 ][ 6 ][ - ][ Note  ]
//  [ 7 ][ 8 ][ 9 ][ × ][ ⌫     ]
//  [.000][ 0 ][ = ][ ÷ ][ SAVE  ]
//
private val KEYPAD_ROWS_FINAL: List<List<CalcKey>> = listOf(
    listOf(CalcKey.Digit(1), CalcKey.Digit(2), CalcKey.Digit(3), CalcKey.Operator('+'), CalcKey.Date),
    listOf(CalcKey.Digit(4), CalcKey.Digit(5), CalcKey.Digit(6), CalcKey.Operator('-'), CalcKey.Note),
    listOf(CalcKey.Digit(7), CalcKey.Digit(8), CalcKey.Digit(9), CalcKey.Operator('*'), CalcKey.Backspace),
    listOf(CalcKey.ThreeZeros, CalcKey.Digit(0), CalcKey.Equals, CalcKey.Operator('/'), CalcKey.Save),
)

// ─── Keypad composable ────────────────────────────────────────────────────────

@Composable
fun TransactionCustomKeypad(
    dateLabel: String,
    noteLabel: String,
    canSave: Boolean,
    isSaving: Boolean,
    onKey: (CalcKey) -> Unit,
    onBackspaceLong: () -> Unit,
    modifier: Modifier = Modifier,
    activeOperator: Char? = null,
    isAmountLocked: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        KEYPAD_ROWS_FINAL.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { key ->
                    KeyCell(
                        key = key,
                        dateLabel = dateLabel,
                        noteLabel = noteLabel,
                        canSave = canSave,
                        isSaving = isSaving,
                        activeOperator = activeOperator,
                        isAmountLocked = isAmountLocked,
                        onKey = onKey,
                        onBackspaceLong = onBackspaceLong,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// ─── Single key cell ─────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KeyCell(
    key: CalcKey,
    dateLabel: String,
    noteLabel: String,
    canSave: Boolean,
    isSaving: Boolean,
    activeOperator: Char?,
    isAmountLocked: Boolean,
    onKey: (CalcKey) -> Unit,
    onBackspaceLong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSaveKey      = key is CalcKey.Save
    val isOperatorKey  = key is CalcKey.Operator || key is CalcKey.Equals
    val isActionKey    = key is CalcKey.Date || key is CalcKey.Note
    val isBackspaceKey = key is CalcKey.Backspace
    val isAmountKey    = key is CalcKey.Digit || isOperatorKey || key is CalcKey.ThreeZeros || isBackspaceKey
    val isEnabled      = !(isSaveKey && isSaving) && !(isAmountLocked && isAmountKey)
    val isActiveOperator = key is CalcKey.Operator && key.symbol == activeOperator

    val containerColor = when {
        isSaveKey        -> if (canSave) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
        isActiveOperator -> MaterialTheme.colorScheme.primaryContainer
        isOperatorKey    -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        isActionKey      -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else             -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val baseContentColor = when {
        isSaveKey        -> if (canSave) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isActiveOperator -> MaterialTheme.colorScheme.onPrimaryContainer
        isOperatorKey    -> MaterialTheme.colorScheme.secondary
        isActionKey      -> MaterialTheme.colorScheme.tertiary
        isBackspaceKey   -> MaterialTheme.colorScheme.error
        else             -> MaterialTheme.colorScheme.onSurface
    }
    val contentColor = if (!isEnabled && !isSaveKey) baseContentColor.copy(alpha = 0.38f) else baseContentColor

    val clearAllLabel = stringResource(R.string.cd_calc_clear_all)
    val keyContentDescription = when (key) {
        is CalcKey.Digit -> key.value.toString()
        CalcKey.ThreeZeros -> stringResource(R.string.cd_calc_three_zeros)
        is CalcKey.Operator -> when (key.symbol) {
            '+' -> stringResource(R.string.cd_calc_add)
            '-' -> stringResource(R.string.cd_calc_subtract)
            '*' -> stringResource(R.string.cd_calc_multiply)
            '/' -> stringResource(R.string.cd_calc_divide)
            else -> key.symbol.toString()
        }
        CalcKey.Equals -> stringResource(R.string.cd_calc_equals)
        CalcKey.Backspace -> stringResource(R.string.content_description_backspace)
        CalcKey.Date -> stringResource(R.string.cd_calc_date_format, dateLabel)
        CalcKey.Note -> if (noteLabel.isBlank()) {
            stringResource(R.string.cd_calc_note_empty)
        } else {
            stringResource(R.string.cd_calc_note_format, noteLabel)
        }
        CalcKey.Save -> stringResource(R.string.action_save)
    }

    val shape = AppTheme.shapes.corner12

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .clip(shape)
            .background(containerColor)
            .border(
                width = when {
                    isActiveOperator -> 1.5.dp
                    isSaveKey && canSave -> 0.dp
                    else -> 1.dp
                },
                color = if (isActiveOperator) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                },
                shape = shape,
            )
            .semantics {
                role = Role.Button
                contentDescription = keyContentDescription
            }
            .combinedClickable(
                enabled = isEnabled,
                onClick = { onKey(key) },
                onLongClickLabel = if (isBackspaceKey) clearAllLabel else null,
                onLongClick = { if (isBackspaceKey) onBackspaceLong() },
            )
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (key) {
            is CalcKey.Digit -> {
                Text(
                    text = key.value.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }

            CalcKey.ThreeZeros -> {
                Text(
                    text = "000",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }

            is CalcKey.Operator -> {
                val symbol = when (key.symbol) {
                    '+' -> "+"
                    '-' -> "−"
                    '*' -> "×"
                    '/' -> "÷"
                    else -> key.symbol.toString()
                }
                Text(
                    text = symbol,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                )
            }

            CalcKey.Equals -> {
                Text(
                    text = "=",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else contentColor,
                )
            }

            CalcKey.Backspace -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp),
                )
            }

            CalcKey.Date -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = dateLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            CalcKey.Note -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = noteLabel.ifBlank { stringResource(R.string.transaction_field_note) },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            CalcKey.Save -> {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = contentColor,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.action_save),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
