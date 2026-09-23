package com.notepay.ai

import android.content.Context
import android.util.Log
import com.notepay.R
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 3-Tier Zero-config router:
 * Tier 1: Gemini Nano on-device (via Android AICore / mlkit-genai-prompt).
 * Tier 2: Cloud Gemini 2.0 Flash (optional Bring-Your-Own-Key via Google AI SDK).
 * Tier 3: Statistical deterministic analysis (100% offline, 0MB, instant math).
 */
@Singleton
class OnDeviceBudgetAdvisor @Inject constructor(
    private val geminiNano: GeminiNanoBudgetAdvisor,
    private val cloudAdvisor: CloudGeminiAdvisor,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun availability(): AdvisorAvailability = when {
        geminiNano.isGeminiNanoAvailable() -> AdvisorAvailability.GEMINI_NANO
        cloudAdvisor.isConfigured() -> AdvisorAvailability.CLOUD_GEMINI
        else -> AdvisorAvailability.STATISTICAL_ONLY
    }

    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult {
        var geminiFallback: BudgetAdvisorResult? = null

        // 1. Tier 1: Gemini Nano On-Device
        if (geminiNano.isGeminiNanoAvailable()) {
            val geminiResult = geminiNano.generate(input)
            if (geminiResult.provider == AdvisorProvider.GEMINI_NANO) {
                return geminiResult
            }
            geminiFallback = geminiResult
        }

        // 2. Tier 2: Cloud Gemini (if API Key configured)
        if (cloudAdvisor.isConfigured()) {
            try {
                return cloudAdvisor.generate(input)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (com.notepay.BuildConfig.DEBUG) {
                    Log.e(TAG, "Cloud Gemini analysis fell back: ${error.message}", error)
                }
                return (geminiFallback ?: geminiNano.generate(input)).copy(
                    providerMessage = context.getString(R.string.ai_cloud_fallback, error.message ?: context.getString(R.string.ai_cloud_network_error)),
                )
            }
        }

        // 3. Tier 3: Local deterministic statistical fallback
        return (geminiFallback ?: geminiNano.generate(input)).copy(
            providerMessage = context.getString(R.string.ai_statistical_provider_message),
        )
    }

    private companion object {
        const val TAG = "OnDeviceBudgetAdvisor"
    }
}
