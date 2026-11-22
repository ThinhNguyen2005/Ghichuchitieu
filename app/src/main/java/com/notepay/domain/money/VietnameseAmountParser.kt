package com.notepay.domain.money

import com.notepay.core.common.util.StringUtils
import java.util.Locale

data class ParsedAmountMatch(
    val amountMajorVnd: Long,
    val range: IntRange,
    val rawText: String,
)

internal class IndexedNormalizedText(
    val normalized: String,
    private val originalStarts: IntArray,
    private val originalEndsExclusive: IntArray,
) {
    fun toOriginalRange(normRange: IntRange): IntRange {
        if (normalized.isEmpty() || normRange.first > normRange.last) return IntRange.EMPTY
        val safeStart = normRange.first.coerceIn(0, originalStarts.lastIndex)
        val safeEnd = normRange.last.coerceIn(safeStart, originalEndsExclusive.lastIndex)
        return originalStarts[safeStart] until originalEndsExclusive[safeEnd]
    }

    companion object {
        fun from(source: String, maskedRanges: List<IntRange> = emptyList()): IndexedNormalizedText {
            if (source.isEmpty()) {
                return IndexedNormalizedText("", IntArray(0), IntArray(0))
            }
            val maskedChars = BooleanArray(source.length)
            for (range in maskedRanges) {
                val start = range.first.coerceAtLeast(0)
                val end = range.last.coerceAtMost(source.lastIndex)
                for (i in start..end) {
                    maskedChars[i] = true
                }
            }

            val normBuilder = StringBuilder(source.length)
            val starts = ArrayList<Int>(source.length)
            val ends = ArrayList<Int>(source.length)

            for (i in source.indices) {
                if (maskedChars[i]) {
                    normBuilder.append(' ')
                    starts.add(i)
                    ends.add(i + 1)
                    continue
                }
                val stripped = StringUtils.removeVietnameseAccents(source[i].toString())
                if (stripped.isEmpty()) {
                    // Combining diacritical mark attaches to the preceding base character's span.
                    if (ends.isNotEmpty()) {
                        ends[ends.lastIndex] = i + 1
                    }
                } else {
                    val lower = stripped.lowercase(Locale.ROOT)
                    for (ch in lower) {
                        normBuilder.append(ch)
                        starts.add(i)
                        ends.add(i + 1)
                    }
                }
            }

            return IndexedNormalizedText(
                normalized = normBuilder.toString(),
                originalStarts = starts.toIntArray(),
                originalEndsExclusive = ends.toIntArray(),
            )
        }
    }
}

/**
 * On-device 0ms parser for spoken and slang Vietnamese monetary expressions.
 * Handles accented and unaccented inputs while mapping matches back to exact source spans.
 */
object VietnameseAmountParser {

    private data class CandidateMatch(
        val amountMajorVnd: Long,
        val normRange: IntRange,
    )

    private val COMPOUND_MILLION_REGEX = Regex("""\b(\d+)\s*(trieu|tr|cu|chai)(\d{1,2})\b""")
    private val COMPOUND_THOUSAND_REGEX = Regex("""\b(\d+)\s*(ngan|nghin|k)(\d{1,2})\b""")
    private val HALF_EXPRESSION_REGEX = Regex("""\bnua\s+(ty|ti|trieu|tr|cu|chai|xi|lit|tram\s*(?:ngan|nghin|k))\b""")
    private val NUMERIC_UNIT_REGEX = Regex(
        """(?<![.\d,])(\d+(?:[.,]\d+)*)\s*(ty|ti|trieu|tr|cu|chai|xi|lit|ngan|nghin|k|vnd|dong|d)\b(?:\s+(ruoi))?\b""",
    )
    private val SPOKEN_WORDS_REGEX = run {
        val word = "(?:khong|mot|hai|ba|bon|tu|nam|lam|sau|bay|tam|chin|muoi|tram|linh|le|ruoi)"
        val unit = "(?:ty|ti|trieu|tr|cu|chai|xi|lit|ngan|nghin|k|vnd|dong)"
        val clause = "(?:$word\\s+){1,7}$unit"
        val tailDigits = "(?:ruoi|mot|hai|ba|bon|tu|nam|lam|sau|bay|tam|chin)"
        Regex("""\b$clause(?:\s+$clause)*(?:\s+$tailDigits)*\b""")
    }
    private val GROUPED_PLAIN_NUMBER_REGEX = Regex("""(?<![.\d,])\d{1,3}(?:[.,]\d{3})+(?![.\d,])""")
    private val PLAIN_INTEGER_REGEX = Regex("""(?<![.\d,])\d{4,10}(?![.\d,])""")

    private val UNIT_DIGITS = mapOf(
        "khong" to 0L,
        "mot" to 1L,
        "hai" to 2L,
        "ba" to 3L,
        "bon" to 4L,
        "tu" to 4L,
        "nam" to 5L,
        "lam" to 5L,
        "sau" to 6L,
        "bay" to 7L,
        "tam" to 8L,
        "chin" to 9L,
    )

    private val MONEY_UNIT_MULTIPLIERS = mapOf(
        "ty" to 1_000_000_000L,
        "ti" to 1_000_000_000L,
        "trieu" to 1_000_000L,
        "tr" to 1_000_000L,
        "cu" to 1_000_000L,
        "chai" to 1_000_000L,
        "xi" to 100_000L,
        "lit" to 100_000L,
        "ngan" to 1_000L,
        "nghin" to 1_000L,
        "k" to 1_000L,
        "vnd" to 1L,
        "dong" to 1L,
        "d" to 1L,
    )

    fun parseSingle(text: String): Long? = parseAll(text).singleOrNull()?.amountMajorVnd

    fun parseAll(text: String, maskedRanges: List<IntRange> = emptyList()): List<ParsedAmountMatch> {
        if (text.isBlank()) return emptyList()
        val indexed = IndexedNormalizedText.from(text, maskedRanges)
        val norm = indexed.normalized
        val candidates = ArrayList<CandidateMatch>()

        collectCompoundMatches(norm, candidates)
        collectHalfMatches(norm, candidates)
        collectNumericUnitMatches(norm, candidates)
        collectSpokenWordMatches(norm, candidates)
        collectPlainNumberMatches(norm, candidates)

        val selected = selectNonOverlapping(norm, candidates)
        return selected.map { candidate ->
            val origRange = indexed.toOriginalRange(candidate.normRange)
            val safeRaw = if (!origRange.isEmpty()) text.substring(origRange.first, origRange.last + 1) else ""
            ParsedAmountMatch(
                amountMajorVnd = candidate.amountMajorVnd,
                range = origRange,
                rawText = safeRaw,
            )
        }
    }

    private fun collectCompoundMatches(norm: String, out: MutableList<CandidateMatch>) {
        COMPOUND_MILLION_REGEX.findAll(norm).forEach { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return@forEach
            val fracDigits = match.groupValues[3]
            val fracVal = fracDigits.toLongOrNull() ?: return@forEach
            val fracScale = if (fracDigits.length == 1) 100_000L else 10_000L
            val total = safeAdd(safeMul(whole, 1_000_000L), safeMul(fracVal, fracScale))
            if (isValidMajorAmount(total)) out.add(CandidateMatch(total, match.range))
        }
        COMPOUND_THOUSAND_REGEX.findAll(norm).forEach { match ->
            val whole = match.groupValues[1].toLongOrNull() ?: return@forEach
            val fracDigits = match.groupValues[3]
            val fracVal = fracDigits.toLongOrNull() ?: return@forEach
            val fracScale = if (fracDigits.length == 1) 100L else 10L
            val total = safeAdd(safeMul(whole, 1_000L), safeMul(fracVal, fracScale))
            if (isValidMajorAmount(total)) out.add(CandidateMatch(total, match.range))
        }
    }

    private fun collectHalfMatches(norm: String, out: MutableList<CandidateMatch>) {
        HALF_EXPRESSION_REGEX.findAll(norm).forEach { match ->
            val unitToken = match.groupValues[1].replace(Regex("""\s+"""), " ")
            val amount = when {
                unitToken in setOf("ty", "ti") -> 500_000_000L
                unitToken in setOf("trieu", "tr", "cu", "chai") -> 500_000L
                unitToken in setOf("xi", "lit") -> 50_000L
                unitToken.startsWith("tram ") -> 50_000L
                else -> 0L
            }
            if (isValidMajorAmount(amount)) out.add(CandidateMatch(amount, match.range))
        }
    }

    private fun collectNumericUnitMatches(norm: String, out: MutableList<CandidateMatch>) {
        NUMERIC_UNIT_REGEX.findAll(norm).forEach { match ->
            val numberStr = match.groupValues[1]
            val unit = match.groupValues[2]
            val hasRuoi = match.groupValues[3].isNotEmpty()
            val multiplier = MONEY_UNIT_MULTIPLIERS[unit] ?: return@forEach
            val baseAmount = computeNumericAmount(numberStr, multiplier) ?: return@forEach
            val total = if (hasRuoi && '.' !in numberStr && ',' !in numberStr) {
                safeAdd(baseAmount, multiplier / 2L)
            } else {
                baseAmount
            }
            if (isValidMajorAmount(total)) out.add(CandidateMatch(total, match.range))
        }
    }

    private fun computeNumericAmount(numberStr: String, multiplier: Long): Long? {
        if (multiplier == 1L) {
            return numberStr.replace(".", "").replace(",", "").toLongOrNull()
        }
        val groupedMatch = Regex("""^\d{1,3}(?:[.,]\d{3})+$""").matches(numberStr)
        if (groupedMatch) {
            val cleaned = numberStr.replace(".", "").replace(",", "").toLongOrNull() ?: return null
            return safeMul(cleaned, multiplier)
        }
        val normalizedDecimal = numberStr.replace(',', '.')
        if (normalizedDecimal.count { it == '.' } > 1) {
            val cleaned = normalizedDecimal.replace(".", "").toLongOrNull() ?: return null
            return safeMul(cleaned, multiplier)
        }
        val decimalValue = normalizedDecimal.toDoubleOrNull() ?: return null
        val product = decimalValue * multiplier.toDouble()
        if (!product.isFinite() || product <= 0.0 || product > Money.MAX_MAJOR_UNITS.toDouble()) return null
        return kotlin.math.round(product).toLong()
    }

    private fun collectSpokenWordMatches(norm: String, out: MutableList<CandidateMatch>) {
        SPOKEN_WORDS_REGEX.findAll(norm).forEach { match ->
            val parsed = parseSpokenPhrase(match.value, UNIT_DIGITS, MONEY_UNIT_MULTIPLIERS)
            if (parsed != null && isValidMajorAmount(parsed)) {
                out.add(CandidateMatch(parsed, match.range))
            }
        }
    }

    private fun collectPlainNumberMatches(norm: String, out: MutableList<CandidateMatch>) {
        GROUPED_PLAIN_NUMBER_REGEX.findAll(norm).forEach { match ->
            val cleaned = match.value.replace(".", "").replace(",", "").toLongOrNull() ?: return@forEach
            if (isValidMajorAmount(cleaned)) out.add(CandidateMatch(cleaned, match.range))
        }
        PLAIN_INTEGER_REGEX.findAll(norm).forEach { match ->
            val num = match.value.toLongOrNull() ?: return@forEach
            val before = norm.substring(0, match.range.first).trimEnd()
            val isYearAfterNam = num in 1900L..2100L && (before == "nam" || before.endsWith(" nam"))
            if (!isYearAfterNam && isValidMajorAmount(num)) {
                out.add(CandidateMatch(num, match.range))
            }
        }
    }

    private fun selectNonOverlapping(norm: String, candidates: List<CandidateMatch>): List<CandidateMatch> {
        val valid = candidates
            .filter { candidate -> !hasLeadingMinusSign(norm, candidate.normRange.first) }
            .sortedWith(
                compareByDescending<CandidateMatch> { it.normRange.last - it.normRange.first }
                    .thenBy { it.normRange.first },
            )
        val chosen = ArrayList<CandidateMatch>()
        for (candidate in valid) {
            val overlaps = chosen.any { existing ->
                candidate.normRange.first <= existing.normRange.last &&
                    existing.normRange.first <= candidate.normRange.last
            }
            if (!overlaps) {
                chosen.add(candidate)
            }
        }
        return chosen.sortedBy { it.normRange.first }
    }

    private fun hasLeadingMinusSign(norm: String, startIdx: Int): Boolean {
        var i = startIdx - 1
        while (i >= 0 && norm[i].isWhitespace()) i--
        if (i < 0 || norm[i] != '-') return false
        return i == 0 || norm[i - 1].isWhitespace() || norm[i - 1] in charArrayOf(',', ';', ':', '(')
    }
}

private fun isValidMajorAmount(amount: Long): Boolean =
    amount > 0L && amount <= Money.MAX_MAJOR_UNITS

private fun safeAdd(a: Long, b: Long): Long = try {
    Math.addExact(a, b)
} catch (_: ArithmeticException) {
    Long.MAX_VALUE
}

private fun safeMul(a: Long, b: Long): Long = try {
    Math.multiplyExact(a, b)
} catch (_: ArithmeticException) {
    Long.MAX_VALUE
}

private fun parseSpokenPhrase(
    phrase: String,
    unitDigits: Map<String, Long>,
    unitMultipliers: Map<String, Long>,
): Long? {
    val tokens = phrase.trim().split(Regex("""\s+""")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return null

    var totalVnd = 0L
    var lastMultiplier = 0L
    var currentBlock = ArrayList<String>()

    for (token in tokens) {
        val unitMultiplier = unitMultipliers[token]
        when {
            unitMultiplier != null -> {
                val blockVal = parseUnderThousandWords(currentBlock, unitDigits) ?: return null
                totalVnd = safeAdd(totalVnd, safeMul(blockVal, unitMultiplier))
                lastMultiplier = unitMultiplier
                currentBlock = ArrayList()
            }
            token == "ruoi" && currentBlock.isEmpty() && lastMultiplier >= 1_000L -> {
                totalVnd = safeAdd(totalVnd, lastMultiplier / 2L)
                lastMultiplier = 0L
            }
            else -> currentBlock.add(token)
        }
    }

    if (currentBlock.isNotEmpty()) {
        // Handle trailing single digit after a major unit, e.g. "hai trieu mot" -> 2_100_000L.
        val trailingDigit = if (currentBlock.size == 1) unitDigits[currentBlock[0]] else null
        if (trailingDigit != null && trailingDigit in 1..9 && lastMultiplier >= 1_000L) {
            totalVnd = safeAdd(totalVnd, safeMul(trailingDigit, lastMultiplier / 10L))
        } else {
            return null
        }
    }
    return totalVnd.takeIf { it > 0L }
}

private fun parseUnderThousandWords(tokens: List<String>, unitDigits: Map<String, Long>): Long? {
    if (tokens.isEmpty()) return null
    var result = 0L
    var i = 0

    val tramIdx = tokens.indexOf("tram")
    if (tramIdx >= 0) {
        if (tramIdx != 1) return null
        val hundreds = unitDigits[tokens[0]] ?: return null
        result += hundreds * 100L
        i = tramIdx + 1
        if (i < tokens.size && tokens[i] == "ruoi") {
            result += 50L
            return if (i == tokens.lastIndex) result else null
        }
    }

    if (i >= tokens.size) return result

    if (tokens[i] in setOf("linh", "le")) {
        i++
        if (i != tokens.lastIndex) return null
        val digit = unitDigits[tokens[i]] ?: return null
        return result + digit
    }

    val muoiIdx = tokens.indexOfFrom("muoi", i)
    if (muoiIdx >= 0) {
        val tens = when (muoiIdx - i) {
            0 -> 1L
            1 -> unitDigits[tokens[i]] ?: return null
            else -> return null
        }
        result += tens * 10L
        val remIdx = muoiIdx + 1
        if (remIdx == tokens.lastIndex) {
            val unit = if (tokens[remIdx] == "ruoi") 5L else unitDigits[tokens[remIdx]] ?: return null
            return result + unit
        }
        return if (remIdx == tokens.size) result else null
    }

    if (i == tokens.lastIndex) {
        val digit = unitDigits[tokens[i]] ?: return null
        result += if (result > 0L && result % 100L == 0L) digit * 10L else digit
        return result
    }
    return null
}

private fun List<String>.indexOfFrom(target: String, startIndex: Int): Int {
    for (idx in startIndex until size) {
        if (this[idx] == target) return idx
    }
    return -1
}
