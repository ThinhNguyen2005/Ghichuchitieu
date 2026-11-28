package com.notepay.ai

import com.notepay.data.datastore.AiSettingsDataStore
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalFallbackNotice
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

fun interface NaturalAiJsonGenerator {
    suspend fun generateStructuredJson(
        modelName: String,
        apiKey: String,
        prompt: String,
        schema: JSONObject,
    ): String?
}

private class GeminiNaturalAiJsonGenerator(
    private val restClient: GeminiRestClient = GeminiRestClient(),
) : NaturalAiJsonGenerator {
    override suspend fun generateStructuredJson(
        modelName: String,
        apiKey: String,
        prompt: String,
        schema: JSONObject,
    ): String? {
        return restClient.generateContent(
            modelName = modelName,
            apiKey = apiKey,
            prompt = prompt,
            temperature = 0.1,
            maxOutputTokens = 512,
            responseMimeType = "application/json",
            responseSchema = schema,
            timeoutMillis = 8_000,
        ).takeIf { it.isNotBlank() }
    }
}

/**
 * Tier-2 structured-output Gemini fallback for complex Vietnamese natural language inputs.
 * Never invoked unless [NaturalParseResult.needsAiFallback] is true or explicitly requested by the user,
 * and always falls back safely to Tier-1 on-device results when offline/unconfigured/failing.
 */
@Singleton
class AiNaturalTransactionParser internal constructor(
    private val aiSettings: AiSettingsDataStore,
    private val ioDispatcher: CoroutineDispatcher,
    private val jsonGenerator: NaturalAiJsonGenerator,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    @Inject
    constructor(
        aiSettings: AiSettingsDataStore,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(
        aiSettings = aiSettings,
        ioDispatcher = ioDispatcher,
        jsonGenerator = GeminiNaturalAiJsonGenerator(),
        clock = Clock.System,
        timeZone = TimeZone.currentSystemDefault(),
    )

    internal var isOfflineFlavor: Boolean = false

    suspend fun parseOrFallback(
        rawInput: String,
        tier1Result: NaturalParseResult,
        forceAi: Boolean = false,
    ): NaturalParseResult = withContext(ioDispatcher) {
        if (rawInput.isBlank()) return@withContext tier1Result
        if (!forceAi && !tier1Result.needsAiFallback) return@withContext tier1Result
        if (isOfflineFlavor) return@withContext tier1Result

        val cloudEnabled = aiSettings.cloudAiEnabled.first()
        val apiKey = aiSettings.geminiApiKey.first()
        if (!cloudEnabled || apiKey.isNullOrBlank()) {
            return@withContext if (forceAi) {
                tier1Result.copy(fallbackNotice = NaturalFallbackNotice.AI_UNAVAILABLE)
            } else {
                tier1Result
            }
        }

        val today = clock.now().toLocalDateTime(timeZone).date
        val prompt = buildPrompt(rawInput, today)
        val configuredModel = aiSettings.cloudModelName.first()
        val modelsToTry = (listOf(configuredModel, AiSettingsDataStore.DEFAULT_MODEL) +
            AiSettingsDataStore.AVAILABLE_MODELS).distinct()

        var sawQuotaOrAuthError = false
        for (modelName in modelsToTry) {
            try {
                val rawJson = withTimeoutOrNull(AI_TIMEOUT_MILLIS.milliseconds) {
                    jsonGenerator.generateStructuredJson(
                        modelName = modelName,
                        apiKey = apiKey,
                        prompt = prompt,
                        schema = RESPONSE_SCHEMA,
                    )
                }.orEmpty().trim()

                if (rawJson.isNotEmpty()) {
                    val drafts = parseJsonDrafts(rawJson, fallbackDate = today)
                    if (drafts.isNotEmpty()) {
                        return@withContext NaturalParseResult(
                            drafts = drafts,
                            confidence = 0.90f,
                            needsAiFallback = false,
                            source = NaturalParseSource.AI_FALLBACK,
                            fallbackNotice = NaturalFallbackNotice.NONE,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                if (e is GeminiApiException && e.isQuotaOrAuthError()) {
                    sawQuotaOrAuthError = true
                    if (e.isInvalidApiKey()) break
                }
                // Continue to next candidate model, then safely fall back to Tier 1.
            }
        }

        tier1Result.copy(
            fallbackNotice = if (sawQuotaOrAuthError) {
                NaturalFallbackNotice.AI_QUOTA_OR_KEY_ERROR
            } else {
                NaturalFallbackNotice.AI_UNAVAILABLE
            },
        )
    }

    internal fun parseJsonDrafts(rawJson: String, fallbackDate: LocalDate): List<NaturalTransactionDraft> {
        val cleaned = rawJson
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val itemsArray: JSONArray = when {
            cleaned.startsWith("[") -> JSONArray(cleaned)
            else -> JSONObject(cleaned).optJSONArray("transactions") ?: return emptyList()
        }

        val drafts = ArrayList<NaturalTransactionDraft>(itemsArray.length())
        for (i in 0 until itemsArray.length()) {
            itemsArray.optJSONObject(i)
                ?.let { parseDraftObject(i, it, fallbackDate) }
                ?.let(drafts::add)
        }
        return drafts
    }

    private fun parseDraftObject(
        index: Int,
        obj: JSONObject,
        fallbackDate: LocalDate,
    ): NaturalTransactionDraft? {
        val amountVnd = obj.optLong("amountVnd", -1L)
        if (amountVnd <= 0L || amountVnd > Money.MAX_MAJOR_UNITS) return null

        val typeRaw = obj.optString("type", "EXPENSE").uppercase()
        val txType = if (typeRaw == "INCOME") TransactionType.INCOME else TransactionType.EXPENSE
        val isIncome = txType == TransactionType.INCOME

        val parsedCategory = Category.safeValueOf(obj.optString("categoryId"))
        val category = when {
            parsedCategory.isIncome == isIncome -> parsedCategory
            isIncome -> Category.DEFAULT_INCOME
            else -> Category.DEFAULT_EXPENSE
        }

        val note = obj.optString("note")
            .trim()
            .ifBlank { category.displayName }
            .take(Transaction.MAX_NOTE_LENGTH)

        val dateIso = obj.optString("dateIso").trim()
        val parsedDate = try {
            if (dateIso.isNotBlank()) LocalDate.parse(dateIso) else fallbackDate
        } catch (_: IllegalArgumentException) {
            fallbackDate
        }

        return NaturalTransactionDraft(
            id = "ai_${index}_${amountVnd}_${parsedDate}",
            amountVnd = amountVnd,
            type = txType,
            category = category,
            note = note,
            date = parsedDate,
            confidence = 0.90f,
        )
    }

    private fun buildPrompt(rawInput: String, today: LocalDate): String {
        val categoryIds = Category.getAll().joinToString(",") { "${it.id}(${if (it.isIncome) "INCOME" else "EXPENSE"})" }
        return """
            Tách câu tiếng Việt sau thành danh sách giao dịch tài chính.
            Ngày hôm nay: $today.
            Quy đổi mọi đơn vị tiếng lóng VND (k=1000, ngàn=1000, củ/chai/triệu=1000000, xị/lít=100000).
            categoryId hợp lệ: $categoryIds.
            Câu nhập: $rawInput
        """.trimIndent()
    }

    private companion object {
        const val AI_TIMEOUT_MILLIS = 8_000L

        val RESPONSE_SCHEMA: JSONObject = JSONObject().apply {
            put("type", "OBJECT")
            put("description", "Parsed Vietnamese natural language transactions")
            put(
                "properties",
                JSONObject().put(
                    "transactions",
                    JSONObject().apply {
                        put("type", "ARRAY")
                        put("description", "Extracted list of financial transactions")
                        put(
                            "items",
                            JSONObject().apply {
                                put("type", "OBJECT")
                                put("description", "Single extracted transaction")
                                put(
                                    "properties",
                                    JSONObject().apply {
                                        put(
                                            "amountVnd",
                                            JSONObject()
                                                .put("type", "INTEGER")
                                                .put("description", "Positive integer amount in VND"),
                                        )
                                        put(
                                            "type",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "Transaction type")
                                                .put("enum", JSONArray(listOf("EXPENSE", "INCOME"))),
                                        )
                                        put(
                                            "categoryId",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "Matching NotePay category id"),
                                        )
                                        put(
                                            "note",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "Short clean Vietnamese note"),
                                        )
                                        put(
                                            "dateIso",
                                            JSONObject()
                                                .put("type", "STRING")
                                                .put("description", "Date in YYYY-MM-DD format"),
                                        )
                                    },
                                )
                                put(
                                    "required",
                                    JSONArray(listOf("amountVnd", "type", "categoryId", "note", "dateIso")),
                                )
                            },
                        )
                    },
                ),
            )
            put("required", JSONArray(listOf("transactions")))
        }
    }
}
