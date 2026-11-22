package com.notepay.ai

import com.notepay.data.datastore.AiSettingsDataStore
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.VietnameseNaturalInputParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates instant 0ms on-device natural language parsing and optional Gemini AI fallback.
 */
@Singleton
class HybridNaturalTransactionParser @Inject constructor(
    private val onDeviceParser: VietnameseNaturalInputParser,
    private val aiParser: AiNaturalTransactionParser,
    aiSettings: AiSettingsDataStore,
) {
    val isAiConfiguredFlow: Flow<Boolean> = combine(
        aiSettings.cloudAiEnabled,
        aiSettings.geminiApiKey,
    ) { enabled, apiKey ->
        !aiParser.isOfflineFlavor && enabled && !apiKey.isNullOrBlank()
    }

    fun parseOnDevice(input: String): NaturalParseResult = onDeviceParser.parse(input)

    suspend fun parseWithAi(
        input: String,
        tier1Result: NaturalParseResult,
        forceAi: Boolean = false,
    ): NaturalParseResult = aiParser.parseOrFallback(
        rawInput = input,
        tier1Result = tier1Result,
        forceAi = forceAi,
    )
}
