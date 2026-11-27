package com.notepay.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ThemeColorPaletteTest {

    @Test
    fun `resolveAppColors produces distinct palettes across all theme ids in light and dark modes`() {
        val ids = listOf("sand", "emerald", "ocean", "lavender", "ios")

        val lightPalettes = ids.map { id ->
            resolveAppColors(
                themeColor = id,
                darkTheme = false,
                materialColorScheme = getLightColorScheme(id),
            )
        }
        assertThat(lightPalettes.map { it.ink }.distinct()).hasSize(ids.size)
        assertThat(lightPalettes.map { it.sand }.distinct()).hasSize(ids.size)
        assertThat(lightPalettes.map { it.background }.distinct()).hasSize(ids.size)

        val darkPalettes = ids.map { id ->
            resolveAppColors(
                themeColor = id,
                darkTheme = true,
                materialColorScheme = getDarkColorScheme(id),
            )
        }
        assertThat(darkPalettes.map { it.ink }.distinct()).hasSize(ids.size)
        assertThat(darkPalettes.map { it.sand }.distinct()).hasSize(ids.size)
        assertThat(darkPalettes.map { it.background }.distinct()).hasSize(ids.size)
    }

    @Test
    fun `resolveAppColors maps dynamic scheme tokens directly`() {
        val customScheme = SandLightColorScheme.copy(
            primary = Color(0xFF882244),
            primaryContainer = Color(0xFFFFD8E4),
            background = Color(0xFFFFF8F9),
        )
        val resolved = resolveAppColors(
            themeColor = "dynamic",
            darkTheme = false,
            materialColorScheme = customScheme,
        )
        assertThat(resolved.ink).isEqualTo(Color(0xFF882244))
        assertThat(resolved.sand).isEqualTo(Color(0xFFFFD8E4))
        assertThat(resolved.background).isEqualTo(Color(0xFFFFF8F9))
    }
}
