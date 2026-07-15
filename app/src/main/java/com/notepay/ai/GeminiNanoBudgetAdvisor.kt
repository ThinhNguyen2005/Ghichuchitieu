package com.notepay.ai

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CancellationException
import java.text.NumberFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiNanoBudgetAdvisor @Inject constructor() {
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
                    "Thiết bị chưa hỗ trợ Gemini Nano qua Android AICore.",
                )
                FeatureStatus.DOWNLOADABLE -> model.download().collect { status ->
                    if (status is DownloadStatus.DownloadFailed) throw status.e
                }
                FeatureStatus.DOWNLOADING -> return fallback(
                    input,
                    "Gemini Nano đang tải về máy; tạm dùng phân tích thống kê trên thiết bị.",
                )
                FeatureStatus.AVAILABLE -> Unit
                else -> return fallback(
                    input,
                    "Không xác định được trạng thái Gemini Nano; đang dùng phân tích thống kê trên thiết bị.",
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
                ?: return fallback(input, "Gemini Nano trả về nội dung không đúng định dạng an toàn.")
            BudgetAdvisorResult(
                title = parsed.title,
                content = "${parsed.observation} ${parsed.action}",
                provider = AdvisorProvider.GEMINI_NANO,
                providerMessage = "Phân tích bởi Gemini Nano ngay trên thiết bị",
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            fallback(input, "Không thể khởi chạy Gemini Nano; đang dùng phân tích thống kê trên máy.")
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

    /** Compact prompt for LiteRT-LM models with a short on-device context window. */
    internal fun buildLiteRtPrompt(input: BudgetAdvisorInput): String {
        val prediction = input.prediction
        val categories = input.categories
            .sortedByDescending { it.amountInCents }
            .take(3)
            .joinToString("; ") { category ->
                "${category.name} ${formatVnd(category.amountInCents)} (${(category.share * 100).toInt()}%)"
            }
            .ifBlank { "chưa đủ dữ liệu danh mục" }
        val budget = input.budgetLimitInCents?.let(::formatVnd) ?: "chưa đặt"

        return """
            Bạn là trợ lý chi tiêu. Trả lời tiếng Việt ngắn, tối đa 70 từ.
            Số liệu đã được tính sẵn, không tự tạo số mới.
            Đã chi: ${formatVnd(prediction.spentSoFarInCents)}. Dự báo: ${formatVnd(prediction.predictedMonthTotalInCents)}.
            Hạn mức: $budget. Nguy cơ vượt: ${prediction.overBudgetProbability?.let { "${(it * 100).toInt()}%" } ?: "chưa xác định"}.
            Danh mục lớn: $categories.
            Nêu một nhận xét và một hành động nhỏ trong 7 ngày tới.
        """.trimIndent()
    }

    private fun fallback(input: BudgetAdvisorInput, reason: String): BudgetAdvisorResult {
        val p = input.prediction
        val probability = p.overBudgetProbability
        val topCategory = input.categories.maxByOrNull { it.amountInCents }
        val (title, riskText) = when {
            probability == null -> "Cần đặt định mức" to
                "Dự báo cuối tháng là ${formatVnd(p.predictedMonthTotalInCents)}, nhưng chưa thể đo rủi ro vượt mức vì bạn chưa đặt định mức."
            probability >= 0.70 -> "Nguy cơ vượt định mức cao" to
                "Mô hình ước tính ${(probability * 100).toInt()}% khả năng vượt định mức; khoảng trên có thể đạt ${formatVnd(p.upperBoundInCents)}."
            probability >= 0.35 -> "Chi tiêu cần theo dõi" to
                "Khả năng vượt định mức hiện khoảng ${(probability * 100).toInt()}%, với dự báo ${formatVnd(p.predictedMonthTotalInCents)}."
            else -> "Chi tiêu đang trong tầm kiểm soát" to
                "Khả năng vượt định mức hiện khoảng ${(probability * 100).toInt()}%, nhưng dự báo vẫn có thể thay đổi theo các ngày tới."
        }
        val action = topCategory?.let {
            "Trong 7 ngày tới, hãy theo dõi riêng ${it.name.lowercase(Locale.getDefault())}, hiện chiếm ${(it.share * 100).toInt()}% tổng chi."
        } ?: "Hãy ghi nhận thêm giao dịch trong 7 ngày tới để tăng độ tin cậy của dự báo."
        return BudgetAdvisorResult(
            title = title,
            content = "$riskText $action",
            provider = AdvisorProvider.STATISTICAL_FALLBACK,
            providerMessage = reason,
        )
    }

    private fun formatVnd(cents: Long): String {
        val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        return "${formatter.format(cents / 100)} ₫"
    }
}
