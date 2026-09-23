package com.notepay.data.preferences

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AiSettingsDataStoreTest {
    private lateinit var dataStore: AiSettingsDataStore

    @Before
    fun setup() {
        val context = RuntimeEnvironment.getApplication()
        dataStore = AiSettingsDataStore(context)
    }

    @Test
    fun `default values are correct`() = runTest {
        assertThat(dataStore.geminiApiKey.first()).isNull()
        assertThat(dataStore.cloudAiEnabled.first()).isTrue()
        assertThat(dataStore.cloudModelName.first()).isEqualTo(AiSettingsDataStore.DEFAULT_MODEL)
        assertThat(dataStore.smartReceiptAiEnabled.first()).isTrue()
    }

    @Test
    fun `unsupported models automatically fallback to default model`() = runTest {
        dataStore.setCloudModelName("some-unknown-model")
        assertThat(dataStore.cloudModelName.first()).isEqualTo(AiSettingsDataStore.DEFAULT_MODEL)

        dataStore.setCloudModelName("gemini-3.1-flash-lite")
        assertThat(dataStore.cloudModelName.first()).isEqualTo("gemini-3.1-flash-lite")

        dataStore.setCloudModelName("gemini-3-flash-preview")
        assertThat(dataStore.cloudModelName.first()).isEqualTo("gemini-3-flash-preview")

        dataStore.setCloudModelName("gemini-3.8-flash")
        assertThat(dataStore.cloudModelName.first()).isEqualTo("gemini-3.8-flash")
    }

    @Test
    fun `setGeminiApiKey trims and stores key or null if blank`() = runTest {
        dataStore.setGeminiApiKey("  AIzaSyD-12345  ")
        assertThat(dataStore.geminiApiKey.first()).isEqualTo("AIzaSyD-12345")

        dataStore.setGeminiApiKey("   ")
        assertThat(dataStore.geminiApiKey.first()).isNull()

        dataStore.setGeminiApiKey(null)
        assertThat(dataStore.geminiApiKey.first()).isNull()
    }

    @Test
    fun `setCloudAiEnabled updates flag`() = runTest {
        dataStore.setCloudAiEnabled(false)
        assertThat(dataStore.cloudAiEnabled.first()).isFalse()

        dataStore.setCloudAiEnabled(true)
        assertThat(dataStore.cloudAiEnabled.first()).isTrue()
    }
}
