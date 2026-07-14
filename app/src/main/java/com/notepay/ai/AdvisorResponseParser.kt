package com.notepay.ai

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
                labels.any { label -> candidate.trim().startsWith(label, ignoreCase = true) }
            } ?: return null
            return line.substringAfter(':', "").trim().takeIf { it.isNotBlank() }
        }

        val title = field("TIÊU ĐỀ", "TIEU DE") ?: return null
        val observation = field("NHẬN XÉT", "NHAN XET") ?: return null
        val action = field("HÀNH ĐỘNG", "HANH DONG") ?: return null
        return ParsedAdvisorResponse(title, observation, action)
    }
}
