package com.notepay.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.notepay.ui.theme.AppTheme
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.notepay.ui.navigation.utils.InteractiveHighlight
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

/**
 * Liquid Glass button that samples the shared NotePay backdrop.
 * Incorporates authentic Kyant0 AndroidLiquidGlass specular highlights, translucent surface fills,
 * and high-depth lens refraction.
 */
@Composable
fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
    surfaceColor: Color = Color.Unspecified,
    backdrop: Backdrop? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val activeBackdrop = backdrop ?: LocalNotePayBackdrop.current
    val isDarkTheme = isSystemInDarkTheme()
    val defaultSurface = if (isDarkTheme) {
        Color(0xFF2B2C30).copy(alpha = 0.65f)
    } else {
        Color(0xFFE2E2E8).copy(alpha = 0.72f)
    }
    val disabledSurface = MaterialTheme.colorScheme.onSurface.copy(
        alpha = if (isDarkTheme) 0.16f else 0.10f,
    )
    val glassRimColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.70f)
    }

    val shape = AppTheme.shapes.capsule
    val useSafeFallback = requiresSafeLiquidButtonFallback()
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) {
        InteractiveHighlight(animationScope = animationScope)
    }

    val safeSurface = when {
        surfaceColor.isSpecified -> surfaceColor
        enabled && tint.isSpecified -> tint.copy(alpha = if (isDarkTheme) 0.50f else 0.38f)
        enabled -> defaultSurface
        else -> disabledSurface
    }

    val fallbackVisual = Modifier
        .graphicsLayer {
            if (enabled) {
                val width = size.width.coerceAtLeast(1f)
                val height = size.height.coerceAtLeast(1f)
                val progress = interactiveHighlight.pressProgress
                val scale = lerp(1f, 1f + 4.dp.toPx() / height, progress)
                val maxOffset = size.minDimension.coerceAtLeast(1f)
                val maxDimension = size.maxDimension.coerceAtLeast(1f)
                val offset = interactiveHighlight.offset
                translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                val maxDragScale = 4.dp.toPx() / height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / maxDimension) *
                    (width / height).fastCoerceAtMost(1f)
                scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / maxDimension) *
                    (height / width).fastCoerceAtMost(1f)
            }
        }
        .clip(shape)
        .background(safeSurface)

    val glassVisual = Modifier.drawBackdrop(
        backdrop = activeBackdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(10.dp.toPx())
            if (supportsLiquidLens()) lens(8.dp.toPx(), 20.dp.toPx())
        },
        layerBlock = if (enabled) {
            {
                val width = size.width
                val height = size.height
                val progress = interactiveHighlight.pressProgress
                val scale = lerp(1f, 1f + 4.dp.toPx() / height, progress)
                val maxOffset = size.minDimension
                val offset = interactiveHighlight.offset
                translationX = maxOffset * tanh(0.05f * offset.x / maxOffset)
                translationY = maxOffset * tanh(0.05f * offset.y / maxOffset)
                val maxDragScale = 4.dp.toPx() / height
                val offsetAngle = atan2(offset.y, offset.x)
                scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                    (width / height).fastCoerceAtMost(1f)
                scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                    (height / width).fastCoerceAtMost(1f)
            }
        } else {
            null
        },
        onDrawSurface = {
            drawRect(if (enabled) defaultSurface else disabledSurface)
            if (enabled && tint.isSpecified) {
                drawRect(tint.copy(alpha = 0.32f))
            }
            if (enabled && surfaceColor.isSpecified) {
                drawRect(surfaceColor)
            }
        },
    )

    Row(
        modifier = modifier
            .then(if (useSafeFallback) fallbackVisual else glassVisual)
            .border(1.2.dp, glassRimColor, shape)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .then(
                if (enabled) {
                    Modifier.then(interactiveHighlight.modifier).then(interactiveHighlight.gestureModifier)
                } else {
                    Modifier
                },
            )
            .height(52.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}