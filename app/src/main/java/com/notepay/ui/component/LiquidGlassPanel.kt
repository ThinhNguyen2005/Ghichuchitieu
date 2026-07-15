package com.notepay.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
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
 * Bề mặt Liquid Glass dùng cho các khối ưu tiên cao như số dư, bộ lọc và CTA.
 * Không dùng cho từng phần tử trong danh sách dài để giữ cuộn mượt trên máy tầm trung.
 */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = AppTheme.shapes.corner24,
    tint: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
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
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(12.dp.toPx())
                    lens(8.dp.toPx(), 18.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = highlight.pressProgress) },
                shadow = { Shadow(alpha = 0.16f) },
                innerShadow = { InnerShadow(radius = 6.dp, alpha = 0.10f) },
                onDrawSurface = {
                    drawRect(tint)
                },
            )
            .then(interactiveModifier),
        content = content,
    )
}
