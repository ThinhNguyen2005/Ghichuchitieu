package com.notepay.core.designsystem.component.effect

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.emptyBackdrop
import com.notepay.core.designsystem.theme.AppTheme

/**
 * Shared DESIGN.md v5.0.0 ModalBottomSheet wrapper used across NotePay so every sheet shares
 * `sheetTop` (28.dp), `colors.surface`, `0.45f` scrim, and an isolated backdrop window.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotePayModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    sheetGesturesEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isolatedBackdrop = remember { emptyBackdrop() }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        sheetGesturesEnabled = sheetGesturesEnabled,
        shape = AppTheme.shapes.sheetTop,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = AppTheme.colors.hairline)
        },
    ) {
        CompositionLocalProvider(LocalNotePayBackdrop provides isolatedBackdrop) {
            content()
        }
    }
}
