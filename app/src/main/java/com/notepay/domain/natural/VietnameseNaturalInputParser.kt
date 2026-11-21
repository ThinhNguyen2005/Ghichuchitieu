package com.notepay.domain.natural

import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.IndexedNormalizedText
import com.notepay.domain.money.ParsedAmountMatch
import com.notepay.domain.money.VietnameseAmountParser
import com.notepay.domain.usecase.SuggestCategoryUseCase
import kotlinx.datetime.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tier-1 on-device natural language parser (0ms, offline).
 * Splits multi-transaction Vietnamese sentences by connectors when clauses carry amounts,
 * inherits date context across clauses, and preserves CategoryLearningStoreImpl priority.
 */
@Singleton
class VietnameseNaturalInputParser @Inject constructor(
    private val suggestCategoryUseCase: SuggestCategoryUseCase,
    private val dateParser: VietnameseDateKeywordParser,
) {
    private data class SplitSegment(
        val text: String,
        val separatorBefore: String,
    )

    fun parse(rawInput: String): NaturalParseResult {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            return NaturalParseResult(drafts = emptyList(), confidence = 0f, needsAiFallback = false)
        }

        val sentenceDates = dateParser.parseAll(trimmed)
        var inheritedDate = sentenceDates.firstOrNull()?.date ?: dateParser.today()

        val mergedClauses = splitAndMergeClauses(trimmed)
        val expandedClauses = expandAdjacentMultiAmountClauses(mergedClauses)

        val drafts = ArrayList<NaturalTransactionDraft>()
        var unparsedMeaningfulClause = false
        var ambiguousMultiAmount = false

        expandedClauses.forEach { clause ->
            val clauseDates = dateParser.parseAll(clause)
            val resolvedDate = clauseDates.firstOrNull()?.date ?: inheritedDate
            if (clauseDates.isNotEmpty()) {
                inheritedDate = resolvedDate
            }

            val dateRanges = clauseDates.map { it.range }
            val amounts = VietnameseAmountParser.parseAll(clause, maskedRanges = dateRanges)

            if (amounts.isEmpty()) {
                if (hasMeaningfulLetters(clause) && dateRanges.isEmpty()) {
                    unparsedMeaningfulClause = true
                }
                return@forEach
            }

            if (amounts.size > 1) {
                ambiguousMultiAmount = true
            }

            amounts.forEachIndexed { idx, amountMatch ->
                val note = if (amounts.size == 1) {
                    cleanNoteText(clause, listOf(amountMatch.range) + dateRanges)
                } else {
                    cleanNoteText(clause, amounts.map { it.range } + dateRanges)
                }
                val draft = buildDraft(
                    index = drafts.size,
                    amountVnd = amountMatch.amountMajorVnd,
                    clauseText = clause,
                    cleanNote = note,
                    date = resolvedDate,
                    ambiguityPenalty = if (amounts.size > 1 && idx >= 0) 0.35f else 0f,
                )
                drafts.add(draft)
            }
        }

        val overallConfidence = if (drafts.isEmpty()) {
            0f
        } else {
            drafts.map { it.confidence }.average().toFloat()
        }

        val looksLikeNaturalSentence = looksLikeMultiWordSentence(trimmed)
        val needsAiFallback = when {
            drafts.isEmpty() -> looksLikeNaturalSentence && !containsNegativeAmountSyntax(trimmed)
            ambiguousMultiAmount -> true
            unparsedMeaningfulClause -> true
            else -> overallConfidence < AI_FALLBACK_CONFIDENCE_THRESHOLD
        }

        return NaturalParseResult(
            drafts = drafts,
            confidence = overallConfidence,
            needsAiFallback = needsAiFallback,
            source = NaturalParseSource.ON_DEVICE,
        )
    }

    private fun splitAndMergeClauses(input: String): List<String> {
        val rawSegments = splitByConnectors(input)
        if (rawSegments.size <= 1) return rawSegments.map { it.text }

        val result = ArrayList<SplitSegment>()
        var pendingWithoutAmount: SplitSegment? = null

        for (seg in rawSegments) {
            val dateRanges = dateParser.parseAll(seg.text).map { it.range }
            val hasAmount = VietnameseAmountParser.parseAll(seg.text, dateRanges).isNotEmpty()

            if (!hasAmount) {
                pendingWithoutAmount = if (pendingWithoutAmount == null) {
                    seg
                } else {
                    pendingWithoutAmount.copy(
                        text = "${pendingWithoutAmount.text}${seg.separatorBefore}${seg.text}",
                    )
                }
                continue
            }

            if (pendingWithoutAmount != null) {
                val combined = "${pendingWithoutAmount.text}${seg.separatorBefore}${seg.text}".trim()
                result.add(SplitSegment(combined, seg.separatorBefore))
                pendingWithoutAmount = null
            } else {
                result.add(seg)
            }
        }

        if (pendingWithoutAmount != null) {
            val sepNorm = IndexedNormalizedText.from(pendingWithoutAmount.separatorBefore).normalized.trim()
            val shouldAttachTailToPrevious = result.isNotEmpty() && sepNorm == "voi"
            if (shouldAttachTailToPrevious) {
                val last = result.removeAt(result.lastIndex)
                val merged = "${last.text}${pendingWithoutAmount.separatorBefore}${pendingWithoutAmount.text}".trim()
                result.add(last.copy(text = merged))
            } else {
                result.add(pendingWithoutAmount)
            }
        }

        return result.map { it.text.trim() }.filter { it.isNotEmpty() }
    }

    private fun expandAdjacentMultiAmountClauses(clauses: List<String>): List<String> {
        val expanded = ArrayList<String>(clauses.size)
        for (clause in clauses) {
            val dateRanges = dateParser.parseAll(clause).map { it.range }
            val amounts = VietnameseAmountParser.parseAll(clause, dateRanges)
            if (amounts.size <= 1) {
                expanded.add(clause)
                continue
            }

            // Try splitting right after each amount when descriptive words exist for the following amount.
            val splitParts = splitClauseAtAmountBoundaries(clause, amounts)
            if (splitParts.size == amounts.size) {
                expanded.addAll(splitParts)
            } else {
                expanded.add(clause)
            }
        }
        return expanded
    }

    private fun splitClauseAtAmountBoundaries(
        clause: String,
        amounts: List<ParsedAmountMatch>,
    ): List<String> {
        val parts = ArrayList<String>(amounts.size)
        var cursor = 0
        for (i in 0 until amounts.lastIndex) {
            val end = (amounts[i].range.last + 1).coerceAtMost(clause.length)
            val nextStart = amounts[i + 1].range.first
            val between = if (end <= nextStart) clause.substring(end, nextStart) else ""
            if (!hasMeaningfulLetters(between)) {
                return listOf(clause)
            }
            val slice = clause.substring(cursor, end).trim()
            if (slice.isEmpty()) return listOf(clause)
            parts.add(slice)
            cursor = end
        }
        val tail = clause.substring(cursor).trim()
        if (tail.isEmpty()) return listOf(clause)
        parts.add(tail)
        return parts
    }

    private fun splitByConnectors(input: String): List<SplitSegment> {
        val indexed = IndexedNormalizedText.from(input)
        val norm = indexed.normalized
        val delimiters = ArrayList<Pair<IntRange, String>>()

        CLAUSE_DELIMITER_REGEX.findAll(norm).forEach { match ->
            val origRange = indexed.toOriginalRange(match.range)
            if (!origRange.isEmpty()) {
                val rawSep = input.substring(origRange.first, origRange.last + 1)
                delimiters.add(origRange to rawSep)
            }
        }
        delimiters.sortBy { it.first.first }

        if (delimiters.isEmpty()) {
            return listOf(SplitSegment(input.trim(), ""))
        }

        val segments = ArrayList<SplitSegment>()
        var cursor = 0
        var prevSep = ""
        for ((range, rawSep) in delimiters) {
            if (range.first < cursor) continue
            val chunk = input.substring(cursor, range.first).trim()
            if (chunk.isNotEmpty()) {
                segments.add(SplitSegment(chunk, prevSep))
            }
            prevSep = rawSep
            cursor = (range.last + 1).coerceAtMost(input.length)
        }
        if (cursor < input.length) {
            val tail = input.substring(cursor).trim()
            if (tail.isNotEmpty()) {
                segments.add(SplitSegment(tail, prevSep))
            }
        }
        return segments
    }

    private fun buildDraft(
        index: Int,
        amountVnd: Long,
        clauseText: String,
        cleanNote: String,
        date: LocalDate,
        ambiguityPenalty: Float,
    ): NaturalTransactionDraft {
        val noteForSuggestion = cleanNote.ifBlank { clauseText }
        val isIncome = detectIsIncome(noteForSuggestion)
        val type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE
        val suggestion = suggestCategoryUseCase.suggestDetailed(noteForSuggestion, isIncome)
        val fallbackCategory = if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE
        val category = suggestion?.category ?: fallbackCategory

        val baseConfidence = when {
            cleanNote.isBlank() -> 0.50f
            suggestion != null -> suggestion.confidence.coerceIn(0.75f, 0.96f)
            else -> 0.72f
        }
        val confidence = (baseConfidence - ambiguityPenalty).coerceIn(0.10f, 0.99f)

        return NaturalTransactionDraft(
            id = "draft_${index}_${amountVnd}_${date}",
            amountVnd = amountVnd,
            type = type,
            category = category,
            note = cleanNote.ifBlank { category.displayName }.take(Transaction.MAX_NOTE_LENGTH),
            date = date,
            confidence = confidence,
        )
    }

    private fun detectIsIncome(text: String): Boolean {
        val norm = IndexedNormalizedText.from(text).normalized
        return INCOME_CUES.any { cue ->
            Regex("""\b${Regex.escape(cue)}\b""").containsMatchIn(norm)
        }
    }

    private fun cleanNoteText(clause: String, rangesToRemove: List<IntRange>): String {
        val chars = clause.toCharArray()
        for (range in rangesToRemove) {
            val start = range.first.coerceAtLeast(0)
            val end = range.last.coerceAtMost(chars.lastIndex)
            for (i in start..end) {
                chars[i] = ' '
            }
        }
        return String(chars)
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trim(',', ';', '-', ':', '.')
            .trim()
            .take(Transaction.MAX_NOTE_LENGTH)
    }

    private fun hasMeaningfulLetters(text: String): Boolean =
        IndexedNormalizedText.from(text).normalized.count { it.isLetter() } >= 2

    private fun looksLikeMultiWordSentence(text: String): Boolean {
        val norm = IndexedNormalizedText.from(text).normalized.trim()
        val words = norm.split(Regex("""\s+""")).filter { word -> word.any { it.isLetter() } }
        return words.size >= 2
    }

    private fun containsNegativeAmountSyntax(text: String): Boolean =
        Regex("""(?:^|[\s,;(])-\d+""").containsMatchIn(text)

    private companion object {
        const val AI_FALLBACK_CONFIDENCE_THRESHOLD = 0.55f
        val CLAUSE_DELIMITER_REGEX = Regex(
            """(?<!\d),(?!\d)|[;\n]+|\s+\b(?:sau\s+do|tiep\s+theo|roi|xong|va|voi)\b\s+""",
        )
        val INCOME_CUES = listOf(
            "nhan luong", "luong thang", "duoc thuong", "thuong thang", "nhan thuong",
            "duoc li xi", "nhan qua", "hoan tien", "khach tra", "ban hang thu",
            "thu nhap", "nhan tien", "co tuc", "lai tiet kiem",
        )
    }
}
