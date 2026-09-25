package com.notepay.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class LocaleHelperTest {

    @Test
    fun `normalizeLanguageCode maps Vietnamese tags to vi`() {
        val vietnameseTags = listOf("vi", "VI", "vi-VN", "vi_VN", "vi-Latn-VN", "vi-vn")
        for (tag in vietnameseTags) {
            assertEquals("vi", LocaleHelper.normalizeLanguageCode(tag))
        }
    }

    @Test
    fun `normalizeLanguageCode maps English tags to en`() {
        val englishTags = listOf("en", "EN", "en-US", "en_US", "en-GB", "en_GB", "en-CA")
        for (tag in englishTags) {
            assertEquals("en", LocaleHelper.normalizeLanguageCode(tag))
        }
    }

    @Test
    fun `normalizeLanguageCode maps empty or system or unsupported to system`() {
        val systemTags = listOf("system", "SYSTEM", "", "   ", null, "fr", "ja", "de-DE")
        for (tag in systemTags) {
            assertEquals("system", LocaleHelper.normalizeLanguageCode(tag))
        }
    }
}
