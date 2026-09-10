package com.notepay.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.notepay.ui.theme.AppTheme

/**
 * Solid surface for short, high-priority content.
 */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.corner24,
    tint: Color = Color.Unspecified,
    border: BorderStroke? = null,
    onClick: (() -> Unit)? = null,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceTint = if (tint.isSpecified) {
        tint
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.42f else 0.52f)
    }
    val interactiveModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .background(surfaceTint.compositeOver(MaterialTheme.colorScheme.surface))
            .then(interactiveModifier),
        content = content,
    )
}
