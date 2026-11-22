package com.notepay.feature.home.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackgroundConfig
import com.notepay.core.designsystem.component.effect.AppPresetSwatch
import com.notepay.feature.home.HomeBackgroundPreset

/**
 * Full-canvas atmospheric background behind HomeScreen, delegating to [AppAtmosphericBackground]
 * so all screens share a single source of truth for preset stops and custom photo scrims.
 */
@Composable
internal fun HomeBackgroundLayer(
    preset: HomeBackgroundPreset,
    customImagePath: String?,
    blurEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    AppAtmosphericBackground(
        config = AppAtmosphericBackgroundConfig(
            presetId = preset.id,
            customImagePath = customImagePath,
            blurEnabled = blurEnabled,
        ),
        forceSoftBlurOnPhoto = false,
        modifier = modifier,
    )
}

/** Mini preview swatch rendering exact gradient stops of a [HomeBackgroundPreset]. */
@Composable
internal fun HomePresetSwatch(
    preset: HomeBackgroundPreset,
    modifier: Modifier = Modifier,
) {
    AppPresetSwatch(
        presetId = preset.id,
        modifier = modifier,
    )
}
