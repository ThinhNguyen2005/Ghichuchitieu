package com.notepay.core.designsystem.component.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomeShadowDark
import com.notepay.core.designsystem.theme.HomeShadowLight

/**
 * Dual-layer soft drop shadow inspired by iOS surface elevation.
 * Must be applied before `clip`, `background`, or `Surface` so the shadow is not clipped.
 */
@Composable
fun Modifier.iosCardShadow(
    shape: Shape,
    elevated: Boolean = false,
): Modifier {
    val isLight = AppTheme.colors.isLight
    val baseColor = if (isLight) HomeShadowLight else HomeShadowDark
    val ambientAlpha = when {
        elevated && isLight -> 0.10f
        elevated -> 0.42f
        isLight -> 0.07f
        else -> 0.34f
    }
    val contactAlpha = when {
        elevated && isLight -> 0.05f
        elevated -> 0.24f
        isLight -> 0.04f
        else -> 0.20f
    }
    val ambientRadius = if (elevated) 24.dp else 16.dp
    val ambientOffsetY = if (elevated) 12.dp else 6.dp
    val contactRadius = if (elevated) 6.dp else 3.dp
    val contactOffsetY = if (elevated) 2.dp else 1.dp

    return this
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = ambientRadius,
                spread = 0.dp,
                color = baseColor,
                offset = DpOffset(x = 0.dp, y = ambientOffsetY),
                alpha = ambientAlpha,
            ),
        )
        .dropShadow(
            shape = shape,
            shadow = Shadow(
                radius = contactRadius,
                spread = 0.dp,
                color = baseColor,
                offset = DpOffset(x = 0.dp, y = contactOffsetY),
                alpha = contactAlpha,
            ),
        )
}
