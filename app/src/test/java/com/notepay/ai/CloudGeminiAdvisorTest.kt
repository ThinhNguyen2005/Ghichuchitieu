package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import com.notepay.data.datastore.AiSettingsDataStore
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val restClient = mockk<GeminiRestClient>()
    private val testDispatcher = StandardTestDispatcher()

    private fun createAdvisor(isOffline: Boolean = false) = CloudGeminiAdvisor(
        context = context,
        aiSettings = aiSettings,
        geminiNanoAdvisor = nanoAdvisor,
        ioDispatcher = testDispatcher,
        restClient = restClient,
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

    @Test
    fun `testConnection falls back to working model and persists it when configured model returns 404`() =
        runTest(testDispatcher) {
            every { aiSettings.cloudModelName } returns flowOf("gemini-3.8-flash")
            coJustRun { aiSettings.setCloudModelName(any()) }
            every {
                restClient.generateContent(
                    modelName = "gemini-3.8-flash",
                    apiKey = "AIzaSyFakeKey123",
                    prompt = any(),
                    temperature = any(),
                    maxOutputTokens = any(),
                    responseMimeType = any(),
                    responseSchema = any(),
                    timeoutMillis = any(),
                )
            } throws GeminiApiException(404, "NOT_FOUND", "models/gemini-3.8-flash is not found")
            every {
                restClient.generateContent(
                    modelName = "gemini-2.5-flash",
                    apiKey = "AIzaSyFakeKey123",
                    prompt = any(),
                    temperature = any(),
                    maxOutputTokens = any(),
                    responseMimeType = any(),
                    responseSchema = any(),
                    timeoutMillis = any(),
                )
            } returns "Sẵn sàng."

            val advisor = createAdvisor()
            val result = advisor.testConnection("  AIzaSyFakeKey123  ")

            assertThat(result.isSuccess).isTrue()
            assertThat(result.getOrNull()).isEqualTo("Sẵn sàng.")
            coVerify(exactly = 1) { aiSettings.setCloudModelName("gemini-2.5-flash") }
        }

    @Test
    fun `testConnection stops immediately on invalid API key without trying fallback models`() =
        runTest(testDispatcher) {
            every { aiSettings.cloudModelName } returns flowOf("gemini-3.8-flash")
            every {
                restClient.generateContent(
                    modelName = any(),
                    apiKey = any(),
                    prompt = any(),
                    temperature = any(),
                    maxOutputTokens = any(),
                    responseMimeType = any(),
                    responseSchema = any(),
                    timeoutMillis = any(),
                )
            } throws GeminiApiException(400, "INVALID_ARGUMENT", "API key not valid. Please pass a valid API key.")

            val advisor = createAdvisor()
            val result = advisor.testConnection("bad-key")

            assertThat(result.isFailure).isTrue()
            assertThat(result.exceptionOrNull()?.message).contains("API Key không hợp lệ")
            verify(exactly = 1) {
                restClient.generateContent(
                    modelName = any(),
                    apiKey = any(),
                    prompt = any(),
                    temperature = any(),
                    maxOutputTokens = any(),
                    responseMimeType = any(),
                    responseSchema = any(),
                    timeoutMillis = any(),
                )
            }
        }

    @Test
    fun `extractTextFromResponse skips thought parts and joins visible text`() {
        val body = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {"text": "Internal reasoning...", "thought": true},
                      {"text": "Sẵn "},
                      {"text": "sàng."}
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        assertThat(GeminiRestClient.extractTextFromResponse(body)).isEqualTo("Sẵn sàng.")
    }

    @Test
    fun `parseError extracts Google API status and message and handles non-JSON body`() {
        val jsonError = GeminiRestClient.parseError(
            403,
            """{"error":{"code":403,"message":"API_KEY_INVALID","status":"PERMISSION_DENIED"}}""",
        )
        assertThat(jsonError.httpStatus).isEqualTo(403)
        assertThat(jsonError.apiStatus).isEqualTo("PERMISSION_DENIED")
        assertThat(jsonError.message).isEqualTo("API_KEY_INVALID")
        assertThat(jsonError.isInvalidApiKey()).isTrue()

        val plainError = GeminiRestClient.parseError(502, "<html>Bad Gateway</html>")
        assertThat(plainError.httpStatus).isEqualTo(502)
        assertThat(plainError.apiStatus).isNull()
        assertThat(plainError.message).isEqualTo("Gemini API HTTP 502")
    }
}
