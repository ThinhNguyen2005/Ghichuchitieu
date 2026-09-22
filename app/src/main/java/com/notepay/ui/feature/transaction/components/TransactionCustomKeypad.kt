package com.notepay.ui.feature.transaction.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.ui.theme.AppTheme

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
    onKey: (CalcKey) -> Unit,
    onBackspaceLong: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSaveKey      = key is CalcKey.Save
    val isOperatorKey  = key is CalcKey.Operator || key is CalcKey.Equals
    val isActionKey    = key is CalcKey.Date || key is CalcKey.Note
    val isBackspaceKey = key is CalcKey.Backspace

    val containerColor = when {
        isSaveKey      -> if (canSave) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.surfaceVariant
        isOperatorKey  -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        isActionKey    -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else           -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val contentColor = when {
        isSaveKey      -> if (canSave) MaterialTheme.colorScheme.onPrimary
                          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isOperatorKey  -> MaterialTheme.colorScheme.secondary
        isActionKey    -> MaterialTheme.colorScheme.tertiary
        isBackspaceKey -> MaterialTheme.colorScheme.error
        else           -> MaterialTheme.colorScheme.onSurface
    }

    val shape = AppTheme.shapes.corner12

    Box(
        modifier = modifier
            .aspectRatio(1.1f)
            .clip(shape)
            .background(containerColor)
            .border(
                width = if (isSaveKey && canSave) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                shape = shape,
            )
            .combinedClickable(
                onClick = { onKey(key) },
                onLongClick = { if (key is CalcKey.Backspace) onBackspaceLong() },
            ),
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
                    text = ".000",
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
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            CalcKey.Backspace -> {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = stringResource(R.string.content_description_backspace),
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
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
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
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
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.transaction_save_button),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contentColor,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
