package com.notepay.domain.util

import android.os.Build

/**
 * Tiện ích kiểm tra tương thích tính năng phần cứng & phiên bản Android.
 */
object OsCompatHelper {

    /**
     * Kiểm tra thiết bị có hỗ trợ RenderEffect / Shaders (Liquid Glass) hay không.
     * RenderEffect yêu cầu Android 12 (API level 31) trở lên.
     */
    fun supportsLiquidGlass(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
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
