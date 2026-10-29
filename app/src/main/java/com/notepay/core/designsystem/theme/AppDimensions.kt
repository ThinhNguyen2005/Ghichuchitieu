package com.notepay.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class AppDimensions(
    val spaceExtraSmall: Dp,
    val spaceSmall: Dp,
    val spaceMedium: Dp,
    val spaceLarge: Dp,
    val spaceExtraLarge: Dp,
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val paddingLarge: Dp
)

val DefaultAppDimensions = AppDimensions(
    spaceExtraSmall = 4.dp,
    spaceSmall = 8.dp,
    spaceMedium = 16.dp,
    spaceLarge = 24.dp,
    spaceExtraLarge = 32.dp,
    paddingSmall = 8.dp,
    paddingMedium = 16.dp,
    paddingLarge = 24.dp
)

val LocalAppDimensions = staticCompositionLocalOf { DefaultAppDimensions }
