package com.notepay.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.notepay.ui.navigation.utils.InteractiveHighlight
import com.notepay.ui.theme.AppTheme

/**
 * Glass surface for short, high-priority content. It samples the shared app backdrop by default.
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
    val activeBackdrop = backdrop ?: LocalNotePayBackdrop.current
    val isDarkTheme = isSystemInDarkTheme()
    val surfaceTint = if (tint.isSpecified) {
        tint
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.42f else 0.52f)
    }
    val scope = rememberCoroutineScope()
    val highlight = remember(scope) { InteractiveHighlight(animationScope = scope) }
    val interactiveModifier = if (onClick != null) {
        Modifier
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(highlight.modifier)
            .then(highlight.gestureModifier)
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .clip(shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .drawBackdrop(
                backdrop = activeBackdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(8.dp.toPx())
                    if (shape is CornerBasedShape) {
                        if (supportsLiquidLens()) lens(6.dp.toPx(), 16.dp.toPx())
                    }
                },
                highlight = { Highlight.Default.copy(alpha = highlight.pressProgress) },
                shadow = { Shadow(alpha = 0.12f) },
                innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.08f) },
                onDrawSurface = { drawRect(surfaceTint) },
            )
            .then(interactiveModifier),
        content = content,
    )
}
