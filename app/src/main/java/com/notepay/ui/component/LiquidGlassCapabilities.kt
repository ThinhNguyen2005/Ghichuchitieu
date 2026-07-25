package com.notepay.ui.component

import android.os.Build
import java.util.Locale

/**
 * Lens uses RuntimeShader on Android 13+. Some MIUI RenderThread builds crash while
 * composing a lens over a moving/captured layer, so those devices use blur + tint.
 */
internal fun supportsLiquidLens(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

    val deviceMaker = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
    return listOf("xiaomi", "redmi", "poco").none(deviceMaker::contains)
}

internal fun requiresSafeLiquidButtonFallback(): Boolean {
    val deviceMaker = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase(Locale.ROOT)
    return listOf("xiaomi", "redmi", "poco").any(deviceMaker::contains)
}