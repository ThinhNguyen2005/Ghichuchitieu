package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.notepay.ui.theme.AppTheme

/** Solid action button; keeps existing call sites and touch geometry. */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    contentColor: Color = Color.Unspecified,
    backdrop: Backdrop? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val surface = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        surfaceColor.isSpecified -> surfaceColor
        tint.isSpecified -> tint.copy(alpha = 1f)
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }

    val resolvedContentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        contentColor.isSpecified -> contentColor
        else -> contentColorFor(surface)
    }

    CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
        Row(
            modifier = modifier
                .clip(AppTheme.shapes.capsule)
                .background(surface)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .height(52.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
