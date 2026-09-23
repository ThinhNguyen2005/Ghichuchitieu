package com.notepay.ai

import android.content.Context
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import com.notepay.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoBudgetAdvisor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val model by lazy { Generation.getClient() }

    /** Safe preflight for zero-config UI. Unsupported devices simply use statistical analysis. */
    suspend fun isGeminiNanoAvailable(): Boolean = try {
        model.checkStatus() == FeatureStatus.AVAILABLE
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        false
    }

    /**
     * Chỉ gọi từ thao tác chủ động trên UI vì Prompt API không cho inference ở background.
     * Nếu máy không hỗ trợ, dữ liệu vẫn được phân tích bằng mô hình thống kê local.
     */
    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult {
        return try {
            when (model.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return fallback(
                    input,
                    context.getString(R.string.ai_gemini_unavailable),
                )
                FeatureStatus.DOWNLOADABLE -> model.download().collect { status ->
                    if (status is DownloadStatus.DownloadFailed) throw status.e
                }
                FeatureStatus.DOWNLOADING -> return fallback(
                    input,
                    context.getString(R.string.ai_gemini_downloading),
                )
                FeatureStatus.AVAILABLE -> Unit
                else -> return fallback(
                    input,
                    context.getString(R.string.ai_gemini_unknown_status),
                )
            }

            val request = generateContentRequest(TextPart(buildPrompt(input))) {
                temperature = 0.2f
                topK = 12
                seed = 2026
                candidateCount = 1
                maxOutputTokens = 180
            }
            val raw = model.generateContent(request).candidates.firstOrNull()?.text.orEmpty()
            val parsed = AdvisorResponseParser.parse(raw)
                ?: return fallback(input, context.getString(R.string.ai_gemini_invalid_response))
            BudgetAdvisorResult(
                title = parsed.title,
                content = "${parsed.observation} ${parsed.action}",
                provider = AdvisorProvider.GEMINI_NANO,
                providerMessage = context.getString(R.string.ai_gemini_provider_message),
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            fallback(input, context.getString(R.string.ai_gemini_start_failed))
        }
    }

    internal fun buildPrompt(input: BudgetAdvisorInput): String {
        val p = input.prediction
        val categories = input.categories.take(5).joinToString(separator = "\n") { category ->
            "- ${category.name}: ${formatVnd(category.amountInCents)} (${(category.share * 100).toInt()}%)"
        }.ifBlank { "- Chưa đủ dữ liệu danh mục" }
        val probability = p.overBudgetProbability?.let { "${(it * 100).toInt()}%" } ?: "không có định mức"
        val trend = p.trendVsPreviousMonth?.let { "${if (it >= 0) "+" else ""}${(it * 100).toInt()}%" } ?: "chưa đủ dữ liệu"

        return """
            Bạn là trợ lý quản lý chi tiêu cá nhân, trả lời bằng tiếng Việt tự nhiên, bình tĩnh và cụ thể.
            Tất cả số liệu đã được Kotlin tính sẵn. Không tự tính lại, không thay đổi và không phát minh số mới.
            Không đưa lời khuyên đầu tư, vay nợ hoặc khẳng định chắc chắn về tương lai.

            DỮ LIỆU TỔNG HỢP (không có giao dịch thô hay thông tin định danh):
            - Đã chi tháng này: ${formatVnd(p.spentSoFarInCents)}
            - Dự báo cuối tháng: ${formatVnd(p.predictedMonthTotalInCents)}
            - Khoảng dự báo 80%: ${formatVnd(p.lowerBoundInCents)} đến ${formatVnd(p.upperBoundInCents)}
            - Định mức: ${input.budgetLimitInCents?.let(::formatVnd) ?: "chưa đặt"}
            - Khả năng vượt định mức: $probability
            - Nhịp chi gần đây: ${formatVnd(p.dailyRunRateInCents)}/ngày
            - So với tháng trước: $trend
            - Mức tin cậy dữ liệu: ${p.confidence}
            - Thu nhập tháng này: ${formatVnd(input.incomeThisMonthInCents)}
            Danh mục chi:
            $categories

            Chọn một điểm đáng chú ý nhất, giải thích nguyên nhân có thể có bằng ngôn ngữ xác suất,
            rồi đề xuất đúng một hành động nhỏ có thể làm trong 7 ngày. Tối đa 90 từ.
            Trả đúng ba dòng, không Markdown:
            TIÊU ĐỀ: ...
            NHẬN XÉT: ...
            HÀNH ĐỘNG: ...
        """.trimIndent()
    }

    private fun fallback(input: BudgetAdvisorInput, reason: String): BudgetAdvisorResult {
        val p = input.prediction
        val probability = p.overBudgetProbability
        val topCategory = input.categories.maxByOrNull { it.amountInCents }
        val (title, riskText) = when {
            probability == null -> context.getString(R.string.ai_fallback_no_budget_title) to
                context.getString(R.string.ai_fallback_no_budget_content, formatVnd(p.predictedMonthTotalInCents))
            probability >= 0.70 -> context.getString(R.string.ai_fallback_high_risk_title) to
                context.getString(R.string.ai_fallback_high_risk_content, (probability * 100).toInt(), formatVnd(p.upperBoundInCents))
            probability >= 0.35 -> context.getString(R.string.ai_fallback_watch_title) to
                context.getString(R.string.ai_fallback_watch_content, (probability * 100).toInt(), formatVnd(p.predictedMonthTotalInCents))
            else -> context.getString(R.string.ai_fallback_controlled_title) to
                context.getString(R.string.ai_fallback_controlled_content, (probability * 100).toInt())
        }
        val action = topCategory?.let {
            context.getString(
                R.string.ai_fallback_category_action,
                it.name.lowercase(Locale.getDefault()),
                (it.share * 100).toInt(),
            )
        } ?: context.getString(R.string.ai_fallback_more_data_action)
        return BudgetAdvisorResult(
            title = title,
            content = "$riskText $action",
            provider = AdvisorProvider.STATISTICAL_FALLBACK,
            providerMessage = reason,
        )
    }

    private fun formatVnd(cents: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return "${formatter.format(cents / 100)} ₫"
    }
}
