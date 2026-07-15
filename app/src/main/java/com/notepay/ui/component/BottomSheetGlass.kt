package com.notepay.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun BoxScope.GlassDropBox(
    backdrop: Backdrop,
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animProgress = remember { Animatable(0f) }
    val isLightTheme = !isSystemInDarkTheme()
    val containerColor = if (isLightTheme) Color(0xFFFAFAFA).copy(alpha = 0.4f) else Color(0xFF121212).copy(alpha = 0.4f)
    
    LaunchedEffect(visible) {
        if (visible) {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f, // Spring bouncy jiggle
                    stiffness = 350f
                )
            )
        } else {
            animProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(180)
            )
        }
    }

    if (animProgress.value > 0f) {
        // Scrim background detector for dismissal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                )
        )

        val progress = animProgress.value
        val scale = lerp(0.3f, 1f, progress)
        val alpha = lerp(0f, 1f, progress)
        val translationY = lerp(120f, 0f, progress) // Pop up from center of FAB
        val shape = RoundedCornerShape(32.dp) // Keep shape constant to allow GPU path caching (prevents black corner artifacts)

        Column(
            modifier = modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 100.dp) // Float perfectly above the navigation bar
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                    this.translationY = translationY
                }
                .clip(shape) // Clip container color and children to shape
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Intercept clicks inside the box
                )
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape }, // Sync backdrop blur mask to the exact same shape
                    effects = {
                        vibrancy()
                        blur(12f.dp.toPx()) // Premium glass blur matching bottom bar aesthetic
                        lens(8f.dp.toPx(), 8f.dp.toPx()) // Symmetric lens refraction to make corner colors completely even
                    },
                    onDrawSurface = { drawRect(containerColor) }
                )
                .fillMaxWidth(),
            content = content
        )
    }
}