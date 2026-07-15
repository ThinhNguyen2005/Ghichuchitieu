package com.notepay.ai

import com.notepay.util.StringUtils
import java.util.Locale

data class ParsedAdvisorResponse(
    val title: String,
    val observation: String,
    val action: String,
)

object AdvisorResponseParser {
    fun parse(raw: String): ParsedAdvisorResponse? {
        val normalized = raw
            .replace("```json", "")
            .replace("```", "")
            .trim()
        fun field(vararg labels: String): String? {
            val line = normalized.lineSequence().firstOrNull { candidate ->
                val normalizedCandidate = canonicalize(candidate.trim())
                labels.any { label -> normalizedCandidate.startsWith(canonicalize(label)) }
            } ?: return null
            return line.substringAfter(':', "").trim().takeIf { it.isNotBlank() }
        }

        val title = field("TIÊU ĐỀ") ?: return null
        val observation = field("NHẬN XÉT") ?: return null
        val action = field("HÀNH ĐỘNG") ?: return null
        return ParsedAdvisorResponse(title, observation, action)
    }

    /**
     * A local model can give useful text without reproducing every requested label.
     * Do not discard a completed inference solely because its presentation differs.
     */
    fun parseLenient(raw: String): ParsedAdvisorResponse? {
        parse(raw)?.let { return it }
        val content = raw
            .replace(Regex("(?s)<think>.*?</think>"), "")
            .replace("```", "")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: return null
        return ParsedAdvisorResponse(
            title = "Gợi ý chi tiêu",
            observation = content,
            action = "Bạn có thể đối chiếu gợi ý này với các giao dịch gần đây trước khi điều chỉnh chi tiêu.",
        )
    }

    private fun canonicalize(value: String): String = StringUtils
        .removeVietnameseAccents(value)
        .replace(Regex("[^A-Za-z0-9:]"), "")
        .uppercase(Locale.ROOT)
}
