package com.notepay.core.designsystem.component.effect

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderDarkBottom
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderDarkMid
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderDarkTop
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderLightBottom
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderLightMid
import com.notepay.core.designsystem.theme.HomePresetAuroraLavenderLightTop
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasDarkBottom
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasDarkMid
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasDarkTop
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasLightBottom
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasLightMid
import com.notepay.core.designsystem.theme.HomePresetMinimalCanvasLightTop
import com.notepay.core.designsystem.theme.HomePresetOceanMistDarkBottom
import com.notepay.core.designsystem.theme.HomePresetOceanMistDarkMid
import com.notepay.core.designsystem.theme.HomePresetOceanMistDarkTop
import com.notepay.core.designsystem.theme.HomePresetOceanMistLightBottom
import com.notepay.core.designsystem.theme.HomePresetOceanMistLightMid
import com.notepay.core.designsystem.theme.HomePresetOceanMistLightTop
import com.notepay.core.designsystem.theme.HomePresetSageMorningDarkBottom
import com.notepay.core.designsystem.theme.HomePresetSageMorningDarkMid
import com.notepay.core.designsystem.theme.HomePresetSageMorningDarkTop
import com.notepay.core.designsystem.theme.HomePresetSageMorningLightBottom
import com.notepay.core.designsystem.theme.HomePresetSageMorningLightMid
import com.notepay.core.designsystem.theme.HomePresetSageMorningLightTop
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomDarkBottom
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomDarkMid
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomDarkTop
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomLightBottom
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomLightMid
import com.notepay.core.designsystem.theme.HomePresetSunsetBloomLightTop
import com.notepay.core.designsystem.theme.HomePresetWarmSilkDarkBottom
import com.notepay.core.designsystem.theme.HomePresetWarmSilkDarkMid
import com.notepay.core.designsystem.theme.HomePresetWarmSilkDarkTop
import com.notepay.core.designsystem.theme.HomePresetWarmSilkLightBottom
import com.notepay.core.designsystem.theme.HomePresetWarmSilkLightMid
import com.notepay.core.designsystem.theme.HomePresetWarmSilkLightTop
import java.io.File

@Immutable
data class AppAtmosphericBackgroundConfig(
    val presetId: String = "warm_silk",
    val customImagePath: String? = null,
    val blurEnabled: Boolean = false,
)

val LocalAppAtmosphericBackground = staticCompositionLocalOf {
    AppAtmosphericBackgroundConfig()
}

/**
 * Shared full-canvas atmospheric background used across Home, Stats, Assets, TransactionForm,
 * Utilities, and secondary screens. Respects the user's selected preset or custom gallery photo
 * with an adaptive Light/Dark readability scrim.
 */
@Composable
fun AppAtmosphericBackground(
    modifier: Modifier = Modifier,
    config: AppAtmosphericBackgroundConfig = LocalAppAtmosphericBackground.current,
    forceSoftBlurOnPhoto: Boolean = false,
) {
    val colors = AppTheme.colors
    val isLight = colors.isLight
    val customFile = remember(config.customImagePath) {
        config.customImagePath?.let(::File)?.takeIf { it.exists() && it.canRead() }
    }
    val useCustomImage = config.presetId == "custom" && customFile != null
    val effectiveBlur = config.blurEnabled || forceSoftBlurOnPhoto

    if (useCustomImage) {
        val scrimBrush = remember(isLight, effectiveBlur, colors.background) {
            if (isLight) {
                Brush.verticalGradient(
                    0f to colors.background.copy(alpha = if (effectiveBlur) 0.72f else 0.58f),
                    0.38f to colors.background.copy(alpha = if (effectiveBlur) 0.52f else 0.28f),
                    1f to colors.background.copy(alpha = if (effectiveBlur) 0.68f else 0.62f),
                )
            } else {
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = if (effectiveBlur) 0.66f else 0.52f),
                    0.38f to Color.Black.copy(alpha = if (effectiveBlur) 0.52f else 0.34f),
                    1f to Color.Black.copy(alpha = if (effectiveBlur) 0.72f else 0.68f),
                )
            }
        }
        val imageModifier = if (effectiveBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Modifier.fillMaxSize().blur(14.dp)
        } else {
            Modifier.fillMaxSize()
        }

        Box(modifier = modifier.background(colors.background)) {
            AsyncImage(
                model = customFile,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = imageModifier,
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimBrush),
            )
        }
    } else {
        val effectivePresetId = if (config.presetId == "custom") "warm_silk" else config.presetId
        val (washBrush, sheenBrush, glowBrush) = remember(effectivePresetId, isLight) {
            buildPresetBrushes(effectivePresetId, isLight)
        }
        Box(
            modifier = modifier
                .background(washBrush)
                .background(sheenBrush)
                .background(glowBrush),
        )
    }
}

/** Mini preview swatch rendering exact gradient stops of a preset ID. */
@Composable
fun AppPresetSwatch(
    presetId: String,
    modifier: Modifier = Modifier,
) {
    val isLight = AppTheme.colors.isLight
    val (washBrush, sheenBrush, _) = remember(presetId, isLight) {
        buildPresetBrushes(presetId, isLight)
    }
    Box(
        modifier = modifier
            .background(washBrush)
            .background(sheenBrush),
    )
}

private fun buildPresetBrushes(
    presetId: String,
    isLight: Boolean,
): Triple<Brush, Brush, Brush> {
    val (top, mid, bottom) = presetStopsById(presetId, isLight)
    val wash = Brush.verticalGradient(
        0f to top,
        0.42f to mid,
        1f to bottom,
    )
    val sheen = Brush.linearGradient(
        colors = listOf(
            mid.copy(alpha = if (isLight) 0.35f else 0.28f),
            Color.Transparent,
            top.copy(alpha = if (isLight) 0.25f else 0.16f),
        ),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
    val glow = Brush.radialGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isLight) 0.36f else 0.05f),
            Color.Transparent,
        ),
    )
    return Triple(wash, sheen, glow)
}

private fun presetStopsById(
    presetId: String,
    isLight: Boolean,
): Triple<Color, Color, Color> = when (presetId) {
    "ocean_mist" -> if (isLight) {
        Triple(HomePresetOceanMistLightTop, HomePresetOceanMistLightMid, HomePresetOceanMistLightBottom)
    } else {
        Triple(HomePresetOceanMistDarkTop, HomePresetOceanMistDarkMid, HomePresetOceanMistDarkBottom)
    }

    "sage_morning" -> if (isLight) {
        Triple(HomePresetSageMorningLightTop, HomePresetSageMorningLightMid, HomePresetSageMorningLightBottom)
    } else {
        Triple(HomePresetSageMorningDarkTop, HomePresetSageMorningDarkMid, HomePresetSageMorningDarkBottom)
    }

    "sunset_bloom" -> if (isLight) {
        Triple(HomePresetSunsetBloomLightTop, HomePresetSunsetBloomLightMid, HomePresetSunsetBloomLightBottom)
    } else {
        Triple(HomePresetSunsetBloomDarkTop, HomePresetSunsetBloomDarkMid, HomePresetSunsetBloomDarkBottom)
    }

    "aurora_lavender" -> if (isLight) {
        Triple(HomePresetAuroraLavenderLightTop, HomePresetAuroraLavenderLightMid, HomePresetAuroraLavenderLightBottom)
    } else {
        Triple(HomePresetAuroraLavenderDarkTop, HomePresetAuroraLavenderDarkMid, HomePresetAuroraLavenderDarkBottom)
    }

    "minimal_canvas" -> if (isLight) {
        Triple(HomePresetMinimalCanvasLightTop, HomePresetMinimalCanvasLightMid, HomePresetMinimalCanvasLightBottom)
    } else {
        Triple(HomePresetMinimalCanvasDarkTop, HomePresetMinimalCanvasDarkMid, HomePresetMinimalCanvasDarkBottom)
    }

    else -> if (isLight) {
        Triple(HomePresetWarmSilkLightTop, HomePresetWarmSilkLightMid, HomePresetWarmSilkLightBottom)
    } else {
        Triple(HomePresetWarmSilkDarkTop, HomePresetWarmSilkDarkMid, HomePresetWarmSilkDarkBottom)
    }
}
