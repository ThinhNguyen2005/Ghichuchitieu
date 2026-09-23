package com.notepay.platform

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OsCompatHelperTest {
    @Test
    fun `compatibility explains an unsupported Android version`() {
        val result = OsCompatHelper.liquidGlassCompatibility(
            sdkInt = 32,
            release = "12L",
            manufacturer = "Google",
            brand = "google",
            model = "Pixel 3a",
            isHardwareAccelerated = true,
        )

        assertFalse(result.isSupported)
        assertEquals(LiquidGlassBlockReason.ANDROID_VERSION, result.blockReason)
        assertEquals("Android 12L (API 32) • Google Pixel 3a", result.deviceDescription)
    }

    @Test
    fun `Android 16 Xiaomi meets navigation glass requirements`() {
        val result = OsCompatHelper.liquidGlassCompatibility(
            sdkInt = 36,
            release = "16",
            manufacturer = "Xiaomi",
            brand = "Redmi",
            model = "2312DRA50G",
            isHardwareAccelerated = true,
        )

        assertTrue(result.isSupported)
        assertEquals(null, result.blockReason)
        assertEquals("Android 16 (API 36) • Xiaomi 2312DRA50G", result.deviceDescription)
    }

    @Test
    fun `compatibility explains disabled hardware acceleration`() {
        val result = OsCompatHelper.liquidGlassCompatibility(
            sdkInt = 36,
            release = "16",
            manufacturer = "Samsung",
            brand = "samsung",
            model = "SM-S928B",
            isHardwareAccelerated = false,
        )

        assertFalse(result.isSupported)
        assertEquals(LiquidGlassBlockReason.HARDWARE_ACCELERATION, result.blockReason)
    }

    @Test
    fun `navigation lens is disabled below Android 13`() {
        for (sdk in 26..32) {
            assertFalse(OsCompatHelper.supportsLiquidGlass(sdk, "Google", "google"))
        }
    }

    @Test
    fun `navigation lens is available from Android 13`() {
        assertTrue(OsCompatHelper.supportsLiquidGlass(33, "Google", "google"))
        assertTrue(OsCompatHelper.supportsLiquidGlass(36, "Samsung", "samsung"))
    }

    @Test
    fun `manufacturer and brand do not block navigation glass`() {
        assertTrue(OsCompatHelper.supportsLiquidGlass(35, "XIAOMI", "android"))
        assertTrue(OsCompatHelper.supportsLiquidGlass(35, "android", "Redmi"))
        assertTrue(OsCompatHelper.supportsLiquidGlass(35, "android", "POCO"))
    }
}
