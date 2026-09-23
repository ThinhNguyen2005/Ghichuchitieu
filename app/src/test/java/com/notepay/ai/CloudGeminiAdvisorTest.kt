package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import com.notepay.data.preferences.AiSettingsDataStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "vi")
class CloudGeminiAdvisorTest {

    private val context = RuntimeEnvironment.getApplication()
    private val aiSettings = mockk<AiSettingsDataStore>()
    private val nanoAdvisor = mockk<GeminiNanoBudgetAdvisor>()
    private val testDispatcher = StandardTestDispatcher()

    private fun createAdvisor(isOffline: Boolean = false) = CloudGeminiAdvisor(
        context = context,
        aiSettings = aiSettings,
        geminiNanoAdvisor = nanoAdvisor,
        ioDispatcher = testDispatcher,
    ).apply {
        isOfflineFlavor = isOffline
    }

    @Test
    fun `isConfigured returns false when apiKey is null or blank`() = runTest(testDispatcher) {
        every { aiSettings.geminiApiKey } returns flowOf(null)
        every { aiSettings.cloudAiEnabled } returns flowOf(true)

        val advisor = createAdvisor()
        assertThat(advisor.isConfigured()).isFalse()

        every { aiSettings.geminiApiKey } returns flowOf("   ")
        assertThat(advisor.isConfigured()).isFalse()
    }

    @Test
    fun `isConfigured returns false when cloudAiEnabled is false`() = runTest(testDispatcher) {
        every { aiSettings.geminiApiKey } returns flowOf("AIzaSyFakeKey123")
        every { aiSettings.cloudAiEnabled } returns flowOf(false)

        val advisor = createAdvisor()
        assertThat(advisor.isConfigured()).isFalse()
    }

    @Test
    fun `isConfigured returns true when apiKey is present and cloudAiEnabled is true`() = runTest(testDispatcher) {
        every { aiSettings.geminiApiKey } returns flowOf("AIzaSyFakeKey123")
        every { aiSettings.cloudAiEnabled } returns flowOf(true)

        val advisor = createAdvisor()
        assertThat(advisor.isConfigured()).isTrue()
    }

    @Test
    fun `extractReceiptInfo returns null when not configured`() = runTest(testDispatcher) {
        every { aiSettings.geminiApiKey } returns flowOf(null)
        every { aiSettings.cloudAiEnabled } returns flowOf(false)

        val advisor = createAdvisor()
        val result = advisor.extractReceiptInfo("VND 50,000 chuyen tien Circle K")
        assertThat(result).isNull()
    }

    @Test
    fun `isConfigured returns false when isOfflineFlavor is true regardless of apiKey`() = runTest(testDispatcher) {
        every { aiSettings.geminiApiKey } returns flowOf("AIzaSyFakeKey123")
        every { aiSettings.cloudAiEnabled } returns flowOf(true)

        val advisor = createAdvisor(isOffline = true)
        assertThat(advisor.isConfigured()).isFalse()
    }

    @Test
    fun `testConnection returns failure when isOfflineFlavor is true`() = runTest(testDispatcher) {
        val advisor = createAdvisor(isOffline = true)
        val result = advisor.testConnection("AIzaSyFakeKey123")
        assertThat(result.isFailure).isTrue()
    }
}
