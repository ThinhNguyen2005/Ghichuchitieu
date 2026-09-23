package com.notepay.ai

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.notepay.R
import com.notepay.data.preferences.AiSettingsDataStore
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

data class SmartReceiptExtractionResult(
    val amountInCents: Long? = null,
    val merchant: String? = null,
    val note: String? = null,
)

@Singleton
class CloudGeminiAdvisor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiSettings: AiSettingsDataStore,
    private val geminiNanoAdvisor: GeminiNanoBudgetAdvisor,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    companion object {
        private const val TIMEOUT_MILLIS = 15_000L
    }

    internal var isOfflineFlavor: Boolean = (com.notepay.BuildConfig.FLAVOR == "local")

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
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = cleanKey,
                    generationConfig = generationConfig {
                        temperature = 0.2f
                        maxOutputTokens = 50
                    },
                )
                val text = withTimeout(TIMEOUT_MILLIS) {
                    val response = model.generateContent("Chào bạn, hãy trả lời đúng 2 từ: Sẵn sàng.")
                    response.text.orEmpty().trim()
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
                return@withContext withTimeout(TIMEOUT_MILLIS) {
                    val model = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.2f
                            maxOutputTokens = 220
                        },
                    )
                    val prompt = geminiNanoAdvisor.buildPrompt(input)
                    val response = model.generateContent(prompt)
                    val rawText = response.text.orEmpty().trim()

                    val parsed = AdvisorResponseParser.parse(rawText)
                        ?: AdvisorResponseParser.parseLenient(
                            raw = rawText,
                            fallbackTitle = context.getString(R.string.stats_advisor_title),
                            fallbackAction = context.getString(R.string.ai_fallback_more_data_action),
                        )

                    if (parsed != null) {
                        if (modelName != modelsToTry.first()) {
                            aiSettings.setCloudModelName(modelName)
                        }
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
            }
        }
        throw Exception(sanitizeAiErrorMessage(lastException), lastException)
    }

    suspend fun extractReceiptInfo(ocrText: String): SmartReceiptExtractionResult? = withContext(ioDispatcher) {
        if (!isConfigured()) return@withContext null
        val apiKey = aiSettings.geminiApiKey.first() ?: return@withContext null
        val modelsToTry = getModelsToTry()

        for (modelName in modelsToTry) {
            try {
                val result = withTimeout(TIMEOUT_MILLIS) {
                    val model = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.1f
                            maxOutputTokens = 200
                            responseMimeType = "application/json"
                        },
                    )
                    val prompt = """
                        Bạn là hệ thống trích xuất biên lai / hóa đơn chuyển khoản ngân hàng Việt Nam.
                        Từ văn bản OCR dưới đây, hãy trích xuất:
                        - "amount": Số tiền giao dịch (dạng số nguyên VND, không chứa dấu chấm phẩy, ví dụ 50000). Không lấy số dư hay số tài khoản.
                        - "merchant": Tên người nhận, tên đơn vị hoặc tên cửa hàng thụ hưởng (chữ hoa/thường chuẩn).
                        - "note": Nội dung chuyển khoản hoặc tên hàng hóa dịch vụ.

                        VĂN BẢN OCR:
                        $ocrText
                    """.trimIndent()

                    val response = model.generateContent(prompt)
                    val raw = response.text.orEmpty().trim()
                    if (raw.isBlank()) return@withTimeout null

                    val json = JSONObject(raw)
                    val amountVal = json.optLong("amount", -1L)
                    val merchantVal = json.optString("merchant").takeIf { it.isNotBlank() && it != "null" }
                    val noteVal = json.optString("note").takeIf { it.isNotBlank() && it != "null" }

                    val amountCents = if (amountVal > 0) amountVal * 100L else null
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
                    if (modelName != modelsToTry.first()) {
                        aiSettings.setCloudModelName(modelName)
                    }
                    return@withContext result
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Continue to next candidate model
            }
        }
        null
    }
}
