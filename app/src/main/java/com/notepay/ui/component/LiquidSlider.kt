package com.notepay.ui.component

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

/** A 48dp accessible Liquid Glass slider primitive. */
@Composable
fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    contentDescription: String? = null,
    backdrop: Backdrop? = null,
) {
    require(valueRange.start <= valueRange.endInclusive) { "valueRange must not be descending." }
    val activeBackdrop = backdrop ?: LocalNotePayBackdrop.current
    val isDarkTheme = isSystemInDarkTheme()
    val rangeLength = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / rangeLength).coerceIn(0f, 1f)
    val trackShape = RoundedCornerShape(percent = 50)
    val inactiveSurface = if (enabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDarkTheme) 0.42f else 0.52f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDarkTheme) 0.16f else 0.10f)
    }
    val activeSurface = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 0.48f else 0.20f)
    val thumbSurface = MaterialTheme.colorScheme.surfaceContainerHighest.copy(
        alpha = if (enabled) 0.92f else 0.52f,
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange, steps)
                if (contentDescription != null) this.contentDescription = contentDescription
                if (!enabled) disabled()
                setProgress { targetValue ->
                    if (!enabled) {
                        false
                    } else {
                        onValueChange(targetValue.coerceIn(valueRange.start, valueRange.endInclusive))
                        true
                    }
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        fun updateFromPosition(positionX: Float) {
            val newFraction = (positionX / trackWidthPx).coerceIn(0f, 1f)
            onValueChange(valueRange.start + rangeLength * newFraction)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(trackShape)
                .drawBackdrop(
                    backdrop = activeBackdrop,
                    shape = { trackShape },
                    effects = {
                        vibrancy()
                        blur(4.dp.toPx())
                        if (supportsLiquidLens()) lens(4.dp.toPx(), 12.dp.toPx())
                    },
                    onDrawSurface = { drawRect(inactiveSurface) },
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(10.dp)
                    .clip(trackShape)
                    .drawBackdrop(
                        backdrop = activeBackdrop,
                        shape = { trackShape },
                        effects = { vibrancy() },
                        onDrawSurface = { drawRect(activeSurface) },
                    ),
            )
        }

        val thumbOffset = ((maxWidth - 24.dp) * fraction)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
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

        if (enabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(valueRange, trackWidthPx) {
                        detectTapGestures { offset ->
                            updateFromPosition(offset.x)
                            onValueChangeFinished?.invoke()
                        }
                    }
                    .pointerInput(valueRange, trackWidthPx) {
                        detectDragGestures(
                            onDragStart = { offset -> updateFromPosition(offset.x) },
                            onDragEnd = { onValueChangeFinished?.invoke() },
                            onDragCancel = { onValueChangeFinished?.invoke() },
                            onDrag = { change, _ ->
                                change.consume()
                                updateFromPosition(change.position.x)
                            },
                        )
                    },
            )
        }
    }
}
