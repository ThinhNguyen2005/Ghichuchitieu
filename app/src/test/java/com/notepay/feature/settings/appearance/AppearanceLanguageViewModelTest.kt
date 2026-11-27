package com.notepay.feature.settings.appearance

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.data.datastore.AppSettingsDataStore
import com.notepay.feature.home.HomeBackgroundPreset
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceLanguageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `setThemeColor, selectBackgroundPreset and setSmartInsightsEnabled persist to DataStore`() = runTest {
        val themeModeFlow = MutableStateFlow("system")
        val themeColorFlow = MutableStateFlow("sand")
        val glassFlow = MutableStateFlow(false)
        val hapticFlow = MutableStateFlow(true)
        val langFlow = MutableStateFlow("system")
        val presetFlow = MutableStateFlow("warm_silk")
        val imagePathFlow = MutableStateFlow<String?>(null)
        val blurFlow = MutableStateFlow(false)
        val tipsDismissedFlow = MutableStateFlow(true)

        val dataStore = mockk<AppSettingsDataStore>(relaxed = true) {
            every { themeMode } returns themeModeFlow
            every { themeColor } returns themeColorFlow
            every { liquidGlassEnabled } returns glassFlow
            every { hapticFeedbackEnabled } returns hapticFlow
            every { appLanguage } returns langFlow
            every { homeBackgroundPreset } returns presetFlow
            every { homeBackgroundImagePath } returns imagePathFlow
            every { homeBackgroundBlurEnabled } returns blurFlow
            every { homeSmartTipsDismissed } returns tipsDismissedFlow

            coEvery { setThemeColor(any()) } coAnswers { themeColorFlow.value = firstArg() }
            coEvery { setHomeBackgroundPreset(any()) } coAnswers { presetFlow.value = firstArg() }
            coEvery { setHomeSmartTipsDismissed(any()) } coAnswers { tipsDismissedFlow.value = firstArg() }
        }
        val context = mockk<Context>(relaxed = true)
        val vm = AppearanceLanguageViewModel(dataStore, context)

        vm.uiState.test {
            awaitItem()
            vm.setThemeColor("emerald")
            assertThat(awaitItem().themeColor).isEqualTo("emerald")

            vm.selectBackgroundPreset(HomeBackgroundPreset.OCEAN_MIST)
            assertThat(awaitItem().backgroundPreset).isEqualTo(HomeBackgroundPreset.OCEAN_MIST)

            vm.setSmartInsightsEnabled(true)
            assertThat(awaitItem().isSmartTipsDismissed).isFalse()

            cancelAndIgnoreRemainingEvents()
        }

        coVerify(exactly = 1) { dataStore.setThemeColor("emerald") }
        coVerify(exactly = 1) { dataStore.setHomeBackgroundPreset("ocean_mist") }
        coVerify(exactly = 1) { dataStore.setHomeSmartTipsDismissed(false) }
    }
}
