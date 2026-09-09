package com.notepay.domain.util

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

/**
 * Tiện ích kiểm tra tương thích tính năng phần cứng & phiên bản Android.
 */
object OsCompatHelper {

    /**
     * Kiểm tra thiết bị có hỗ trợ RenderEffect / Shaders (Liquid Glass) hay không.
     * Navigation lens requires RuntimeShader (API 33). The UI also checks hardware acceleration.
     */
    fun supportsLiquidGlass(
        sdkInt: Int = Build.VERSION.SDK_INT,
        manufacturer: String = Build.MANUFACTURER,
        brand: String = Build.BRAND,
    ): Boolean {
        return liquidGlassCompatibility(
            sdkInt = sdkInt,
            release = Build.VERSION.RELEASE.orEmpty(),
            manufacturer = manufacturer,
            brand = brand,
            model = Build.MODEL.orEmpty(),
            isHardwareAccelerated = true,
        ).isSupported
    }

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

    /**
     * Kiểm tra hỗ trợ cử chỉ Predictive Back (Android 14+ / API 34+).
     */
    fun supportsPredictiveBack(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    }

    /**
     * Tên phiên bản Android thân thiện với người dùng (ví dụ: Android 14, Android 11).
     */
    fun getAndroidVersionName(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }
}
