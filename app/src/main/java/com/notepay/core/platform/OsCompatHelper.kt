package com.notepay.platform

import android.os.Build
import java.util.Locale

enum class LiquidGlassBlockReason {
    ANDROID_VERSION,
    HARDWARE_ACCELERATION,
}

data class LiquidGlassCompatibility(
    val isSupported: Boolean,
    val deviceDescription: String,
    val blockReason: LiquidGlassBlockReason? = null,
)

/** Android platform compatibility checks kept outside the pure domain layer. */
object OsCompatHelper {
    fun supportsLiquidGlass(
        sdkInt: Int = Build.VERSION.SDK_INT,
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
    ): Boolean = liquidGlassCompatibility(
        sdkInt = sdkInt,
        release = Build.VERSION.RELEASE.orEmpty(),
        manufacturer = manufacturer,
        brand = brand,
        model = Build.MODEL.orEmpty(),
        isHardwareAccelerated = true,
    ).isSupported

    fun liquidGlassCompatibility(
        sdkInt: Int = Build.VERSION.SDK_INT,
        release: String = Build.VERSION.RELEASE.orEmpty(),
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
        model: String = Build.MODEL.orEmpty(),
        isHardwareAccelerated: Boolean,
    ): LiquidGlassCompatibility {
        val maker = manufacturer.trim().ifBlank { brand.trim() }.ifBlank { "Unknown" }
        val device = listOf(maker, model.trim())
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }
            .joinToString(" ")
        val description = "Android $release (API $sdkInt) • $device"
        val blockReason = when {
            sdkInt < Build.VERSION_CODES.TIRAMISU -> LiquidGlassBlockReason.ANDROID_VERSION
            !isHardwareAccelerated -> LiquidGlassBlockReason.HARDWARE_ACCELERATION
            else -> null
        }
        return LiquidGlassCompatibility(
            isSupported = blockReason == null,
            deviceDescription = description,
            blockReason = blockReason,
        )
    }

    fun supportsPredictiveBack(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    fun getAndroidVersionName(): String =
        "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
}
