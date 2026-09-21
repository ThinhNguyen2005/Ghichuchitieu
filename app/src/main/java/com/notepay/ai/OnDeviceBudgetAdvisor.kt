package com.notepay.ai

import android.content.Context
import android.util.Log
import com.notepay.R
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

/** Zero-config router: Gemini Nano -> imported LiteRT-LM model -> deterministic statistics. */
@Singleton
class OnDeviceBudgetAdvisor @Inject constructor(
    private val geminiNano: GeminiNanoBudgetAdvisor,
    private val liteRt: LiteRtBudgetAdvisor,
    @param:ApplicationContext private val context: Context,
) {
    suspend fun availability(): AdvisorAvailability = when {
        geminiNano.isGeminiNanoAvailable() -> AdvisorAvailability.GEMINI_NANO
        liteRt.isModelReady() -> AdvisorAvailability.LOCAL_MODEL
        else -> AdvisorAvailability.STATISTICAL_ONLY
    }

    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult {
        var geminiFallback: BudgetAdvisorResult? = null
        if (geminiNano.isGeminiNanoAvailable()) {
            val geminiResult = geminiNano.generate(input)
            if (geminiResult.provider == AdvisorProvider.GEMINI_NANO) {
                return geminiResult
            }
            geminiFallback = geminiResult
        }

        if (liteRt.isModelReady()) {
            try {
                return liteRt.generate(input)
            } catch (timeout: LiteRtInferenceTimeoutException) {
                if (com.notepay.BuildConfig.DEBUG) {
                    Log.e(TAG, "LiteRT-LM analysis timed out", timeout)
                }
                return (geminiFallback ?: geminiNano.generate(input)).copy(
                    providerMessage = context.getString(R.string.ai_litert_fallback, safeReason(timeout)),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (com.notepay.BuildConfig.DEBUG) Log.e(TAG, "LiteRT-LM analysis fell back: ${error.javaClass.simpleName}")
                return (geminiFallback ?: geminiNano.generate(input)).copy(
                    providerMessage = context.getString(R.string.ai_litert_fallback, safeReason(error)),
                )
            }
        }

        return (geminiFallback ?: geminiNano.generate(input)).copy(
            providerMessage = context.getString(R.string.ai_no_on_device_model),
        )
    }

    private fun safeReason(error: Throwable): String = when (error) {
        is OutOfMemoryError -> context.getString(R.string.ai_reason_insufficient_memory)
        is LiteRtInferenceTimeoutException -> context.getString(R.string.ai_reason_timeout)
        is IllegalArgumentException -> context.getString(R.string.ai_reason_incompatible_model)
        else -> context.getString(R.string.ai_reason_initialization_failed)
    }

    private companion object {
        const val TAG = "OnDeviceBudgetAdvisor"
    }
}
