package com.notepay.data.datastore

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
    fun `unsupported models automatically fallback to default model`() = runTest {
        listOf(
            "some-unknown-model" to AiSettingsDataStore.DEFAULT_MODEL,
            "gemini-3.1-flash-lite" to "gemini-3.1-flash-lite",
            "gemini-3-flash-preview" to "gemini-3-flash-preview",
            "gemini-3.8-flash" to "gemini-3.8-flash",
        ).forEach { (input, expected) ->
            dataStore.setCloudModelName(input)
            assertThat(dataStore.cloudModelName.first()).isEqualTo(expected)
        }
    }

    @Test
    fun `setGeminiApiKey trims and stores key or null if blank`() = runTest {
        listOf(
            "  AIzaSyD-12345  " to "AIzaSyD-12345",
            "   " to null,
            null to null,
        ).forEach { (input, expected) ->
            dataStore.setGeminiApiKey(input)
            assertThat(dataStore.geminiApiKey.first()).isEqualTo(expected)
        }
    }
}
