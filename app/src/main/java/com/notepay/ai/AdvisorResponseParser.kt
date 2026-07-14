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

    private fun canonicalize(value: String): String = StringUtils
        .removeVietnameseAccents(value)
        .uppercase(Locale.ROOT)
}
