package com.notepay.ai

import android.content.Context
import com.notepay.R
import com.notepay.data.datastore.AiSettingsDataStore
import com.notepay.di.IoDispatcher
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

data class SmartReceiptExtractionResult(
    val amountInCents: Long? = null,
    val merchant: String? = null,
    val note: String? = null,
)

@Singleton
class CloudGeminiAdvisor internal constructor(
    private val context: Context,
    private val aiSettings: AiSettingsDataStore,
    private val geminiNanoAdvisor: GeminiNanoBudgetAdvisor,
    private val ioDispatcher: CoroutineDispatcher,
    private val restClient: GeminiRestClient,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        aiSettings: AiSettingsDataStore,
        geminiNanoAdvisor: GeminiNanoBudgetAdvisor,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        context = context,
        aiSettings = aiSettings,
        geminiNanoAdvisor = geminiNanoAdvisor,
        ioDispatcher = ioDispatcher,
        restClient = GeminiRestClient(),
    )

    companion object {
        private const val TIMEOUT_MILLIS = 15_000L
        private const val TEST_MAX_OUTPUT_TOKENS = 256
        private const val ADVISOR_MAX_OUTPUT_TOKENS = 512
        private const val RECEIPT_MAX_OUTPUT_TOKENS = 300
    }

    internal var isOfflineFlavor: Boolean = false

    suspend fun isConfigured(): Boolean = withContext(ioDispatcher) {
        if (isOfflineFlavor) return@withContext false
        val key = aiSettings.geminiApiKey.first()
        val enabled = aiSettings.cloudAiEnabled.first()
        enabled && !key.isNullOrBlank()
    }

    private suspend fun getModelsToTry(): List<String> {
        val configured = aiSettings.cloudModelName.first()
        return (listOf(configured, AiSettingsDataStore.DEFAULT_MODEL) + AiSettingsDataStore.AVAILABLE_MODELS).distinct()
    }

    private fun sanitizeAiErrorMessage(e: Exception?): String {
        if (e is GeminiApiException) {
            if (e.isInvalidApiKey()) {
                return "Google Gemini API Key không hợp lệ. Vui lòng kiểm tra lại mã API key."
            }
            if (e.httpStatus == 404 || e.apiStatus == "NOT_FOUND") {
                return "Mô hình AI cũ không còn khả dụng trên Google Cloud. Hệ thống đã tự động chuyển sang ${AiSettingsDataStore.DEFAULT_MODEL}. Vui lòng thử lại."
            }
        }
        val raw = e?.message.orEmpty()
        return when {
            raw.contains("GrpcError") || raw.contains("404") || raw.contains("no longer available") ->
                "Mô hình AI cũ không còn khả dụng trên Google Cloud. Hệ thống đã tự động chuyển sang ${AiSettingsDataStore.DEFAULT_MODEL}. Vui lòng thử lại."
            raw.contains("API_KEY_INVALID") || raw.contains("API key not valid") ->
                "Google Gemini API Key không hợp lệ. Vui lòng kiểm tra lại mã API key."
            raw.isNotBlank() -> raw
            else -> context.getString(R.string.ai_cloud_empty_response)
        }
    }

    suspend fun testConnection(apiKey: String): Result<String> = withContext(ioDispatcher) {
        if (isOfflineFlavor) {
            return@withContext Result.failure(IllegalStateException(context.getString(R.string.settings_ai_local_offline_title)))
        }
        val cleanKey = apiKey.trim()
        val modelsToTry = getModelsToTry()
        var lastException: Exception? = null

        for (modelName in modelsToTry) {
            try {
                val text = withTimeout(TIMEOUT_MILLIS.milliseconds) {
                    restClient.generateContent(
                        modelName = modelName,
                        apiKey = cleanKey,
                        prompt = "Chào bạn, hãy trả lời đúng 2 từ: Sẵn sàng.",
                        temperature = 0.2,
                        maxOutputTokens = TEST_MAX_OUTPUT_TOKENS,
                        timeoutMillis = TIMEOUT_MILLIS.toInt(),
                    )
                }
                if (text.isNotBlank()) {
                    if (modelName != modelsToTry.first()) {
                        aiSettings.setCloudModelName(modelName)
                    }
                    return@withContext Result.success(text)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                lastException = e
                if (e is GeminiApiException && e.isInvalidApiKey()) {
                    break
                }
            }
        }

        val friendlyMessage = sanitizeAiErrorMessage(lastException)
        Result.failure(Exception(friendlyMessage, lastException))
    }

    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult = withContext(ioDispatcher) {
        if (isOfflineFlavor) {
            return@withContext geminiNanoAdvisor.generate(input)
        }
        val apiKey = aiSettings.geminiApiKey.first()
        if (apiKey.isNullOrBlank()) {
            error(context.getString(R.string.ai_cloud_api_key_missing))
        }
        val modelsToTry = getModelsToTry()
        var lastException: Exception? = null

        for (modelName in modelsToTry) {
            try {
                return@withContext withTimeout(TIMEOUT_MILLIS.milliseconds) {
                    val prompt = geminiNanoAdvisor.buildPrompt(input)
                    val rawText = restClient.generateContent(
                        modelName = modelName,
                        apiKey = apiKey,
                        prompt = prompt,
                        temperature = 0.2,
                        maxOutputTokens = ADVISOR_MAX_OUTPUT_TOKENS,
                        timeoutMillis = TIMEOUT_MILLIS.toInt(),
                    )

                    val parsed = AdvisorResponseParser.parse(rawText)
                        ?: AdvisorResponseParser.parseLenient(
                            raw = rawText,
                            fallbackTitle = context.getString(R.string.stats_advisor_title),
                            fallbackAction = context.getString(R.string.ai_fallback_more_data_action),
                        )

                    if (parsed != null) {
                        BudgetAdvisorResult(
                            title = parsed.title,
                            content = "${parsed.observation} ${parsed.action}",
                            provider = AdvisorProvider.CLOUD_GEMINI,
                            providerMessage = context.getString(R.string.ai_cloud_provider_message, modelName),
                        )
                    } else {
                        error(context.getString(R.string.ai_cloud_invalid_response))
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                lastException = e
                if (e is GeminiApiException && e.isInvalidApiKey()) {
                    break
                }
            }
        }
        throw IllegalStateException(sanitizeAiErrorMessage(lastException), lastException)
    }

    suspend fun extractReceiptInfo(ocrText: String): SmartReceiptExtractionResult? = withContext(ioDispatcher) {
        val apiKey = aiSettings.geminiApiKey.first()
        if (isOfflineFlavor || apiKey.isNullOrBlank() || !aiSettings.cloudAiEnabled.first()) {
            return@withContext null
        }
        val modelsToTry = getModelsToTry()

        for (modelName in modelsToTry) {
            try {
                val result = withTimeout(TIMEOUT_MILLIS.milliseconds) {
                    val prompt = """
                        Bạn là hệ thống trích xuất biên lai / hóa đơn chuyển khoản ngân hàng Việt Nam.
                        Từ văn bản OCR dưới đây, hãy trích xuất:
                        - "amount": Số tiền giao dịch (dạng số nguyên VND, không chứa dấu chấm phẩy, ví dụ 50000). Không lấy số dư hay số tài khoản.
                        - "merchant": Tên người nhận, tên đơn vị hoặc tên cửa hàng thụ hưởng (chữ hoa/thường chuẩn).
                        - "note": Nội dung chuyển khoản hoặc tên hàng hóa dịch vụ.

                        VĂN BẢN OCR:
                        $ocrText
                    """.trimIndent()

                    val raw = restClient.generateContent(
                        modelName = modelName,
                        apiKey = apiKey,
                        prompt = prompt,
                        temperature = 0.1,
                        maxOutputTokens = RECEIPT_MAX_OUTPUT_TOKENS,
                        responseMimeType = "application/json",
                        timeoutMillis = TIMEOUT_MILLIS.toInt(),
                    )
                    if (raw.isBlank()) return@withTimeout null

                    val json = JSONObject(raw)
                    val amountVal = json.optLong("amount", -1L)
                    val merchantVal = json.optString("merchant").takeIf { it.isNotBlank() && it != "null" }
                    val noteVal = json.optString("note").takeIf { it.isNotBlank() && it != "null" }

                    val amountCents = if (amountVal in 1..9_999_999_999L) amountVal * 100L else null
                    if (amountCents == null && merchantVal == null && noteVal == null) {
                        null
                    } else {
                        SmartReceiptExtractionResult(
                            amountInCents = amountCents,
                            merchant = merchantVal,
                            note = noteVal,
                        )
                    }
                }
                if (result != null) {
                    return@withContext result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (e is GeminiApiException && e.isInvalidApiKey()) {
                    return@withContext null
                }
                // Continue to next candidate model
            }
        }
        null
    }
}
