package com.notepay.ui.component

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** A 48dp accessible Liquid Glass switch primitive. */
@Composable
fun LiquidToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    val activeBackdrop = backdrop ?: LocalNotePayBackdrop.current
    val isDarkTheme = isSystemInDarkTheme()
    val shape = RoundedCornerShape(percent = 50)
    val thumbOffset = animateDpAsState(
        targetValue = if (checked) 24.dp else 4.dp,
        label = "liquidToggleThumbOffset",
    )
    val trackSurface = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)
        checked -> MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme) 0.30f else 0.22f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.42f else 0.52f)
    }
    val thumbSurface = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
        alpha = if (enabled) 0.92f else 0.52f,
    )

    Box(
        modifier = modifier
            .widthIn(min = 52.dp)
            .heightIn(min = 48.dp)
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(shape)
                .drawBackdrop(
                    backdrop = activeBackdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        if (supportsLiquidLens()) lens(4.dp.toPx(), 12.dp.toPx())
                    },
                    onDrawSurface = { drawRect(trackSurface) },
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = thumbOffset.value)
                    .size(24.dp)
                    .clip(CircleShape)
                    .drawBackdrop(
                        backdrop = activeBackdrop,
                        shape = { CircleShape },
                        effects = {
                            blur(3.dp.toPx())
                            if (supportsLiquidLens()) lens(4.dp.toPx(), 10.dp.toPx())
                        },
                        onDrawSurface = { drawRect(thumbSurface) },
                    ),
            )
        }
    }
}
