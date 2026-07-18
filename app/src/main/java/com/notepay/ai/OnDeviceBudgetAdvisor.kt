package com.notepay.ai

import android.util.Log
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import javax.inject.Inject
import javax.inject.Singleton

/** Zero-config router: Gemini Nano -> imported LiteRT-LM model -> deterministic statistics. */
@Singleton
class OnDeviceBudgetAdvisor @Inject constructor(
    private val geminiNano: GeminiNanoBudgetAdvisor,
    private val liteRt: LiteRtBudgetAdvisor,
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
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (com.notepay.BuildConfig.DEBUG) Log.e(TAG, "LiteRT-LM analysis fell back: ${error.javaClass.simpleName}")
                return (geminiFallback ?: geminiNano.generate(input)).copy(
                    providerMessage = "Mô hình AI cục bộ chưa chạy được (${safeReason(error)}); " +
                        "đang dùng phân tích thống kê trên máy.",
                )
            }
        }

        return (geminiFallback ?: geminiNano.generate(input)).copy(
            providerMessage = "Thiết bị không có Gemini Nano và chưa cài mô hình LiteRT-LM; " +
                "đang dùng phân tích thống kê trên máy.",
        )
    }

    private fun safeReason(error: Throwable): String = when (error) {
        is OutOfMemoryError -> "không đủ bộ nhớ"
        is TimeoutCancellationException -> "mất quá lâu để phản hồi"
        is IllegalArgumentException -> "mô hình không tương thích"
        else -> "không thể khởi tạo"
    }

    private companion object {
        const val TAG = "OnDeviceBudgetAdvisor"
    }
}
