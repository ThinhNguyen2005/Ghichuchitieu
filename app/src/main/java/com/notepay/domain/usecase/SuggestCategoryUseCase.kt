package com.notepay.domain.usecase

import android.content.Context
import com.notepay.domain.model.Category
import com.notepay.util.StringUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/** A category proposal together with a user-facing explanation of its evidence. */
data class CategorySuggestion(
    val category: Category,
    val confidence: Float,
    val reason: String,
)

/**
 * On-device category classifier.
 *
 * It deliberately returns no proposal when evidence is weak. A suggestion can come from a
 * repeated merchant/note, a precise merchant keyword, or a per-type Naive Bayes model learned
 * only from transactions the user has saved. No transaction text leaves the device.
 */
@Singleton
class SuggestCategoryUseCase @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE)

    private data class Rule(val category: Category, val phrases: Set<String>)

    private val expenseRules = listOf(
        Rule(Category.TRANSPORT, setOf("grab", "gojek", "xanh sm", "taxi", "be car", "xe buyt", "ve xe", "gui xe", "parking")),
        Rule(Category.GAS, setOf("xang", "petrolimex", "pvoil", "shell", "caltex")),
        Rule(Category.FOOD, setOf("quan an", "nha hang", "com tam", "com ga", "pho", "bun bo", "banh mi", "tra sua", "grabfood", "shopeefood", "beamin")),
        Rule(Category.COFFEE, setOf("ca phe", "cafe", "highlands", "starbucks", "phuc long", "the coffee house")),
        Rule(Category.SHOPPING, setOf("shopee", "lazada", "tiki", "mua sam", "amazon")),
        Rule(Category.CLOTHES, setOf("quan ao", "thoi trang", "giay dep", "uniqlo", "zara", "hm")),
        Rule(Category.BEAUTY, setOf("my pham", "lam toc", "spa", "nail", "skincare")),
        Rule(Category.ELECTRICITY, setOf("tien dien", "evn", "dien luc")),
        Rule(Category.WATER, setOf("tien nuoc", "cap nuoc")),
        Rule(Category.INTERNET, setOf("wifi", "internet", "cuoc mang", "viettel", "vinaphone", "mobifone")),
        Rule(Category.HOME, setOf("tien nha", "tien thue", "chung cu", "noi that")),
        Rule(Category.BILL, setOf("hoa don", "phi dich vu", "phi quan ly", "rac")),
        Rule(Category.ENTERTAINMENT, setOf("netflix", "spotify", "cgv", "rap phim", "karaoke", "steam", "game")),
        Rule(Category.TRAVEL, setOf("may bay", "ve bay", "khach san", "booking", "agoda", "du lich")),
        Rule(Category.HEALTH, setOf("benh vien", "phong kham", "nha khoa", "thuoc", "xet nghiem", "bao hiem y te")),
        Rule(Category.EDUCATION, setOf("hoc phi", "khoa hoc", "sach", "trung tam", "gia su")),
        Rule(Category.PETS, setOf("thu cung", "cho meo", "thuc an cho", "thuc an meo", "thu y")),
        Rule(Category.CHILDREN, setOf("con cai", "bim sua", "do choi tre em", "hoc phi con")),
        Rule(Category.INSURANCE, setOf("bao hiem", "bao hiem xe", "bao hiem nhan tho")),
        Rule(Category.TAX, setOf("thue", "phi truoc ba", "phat nguoi", "phi cau duong")),
        Rule(Category.SAVINGS, setOf("tiet kiem", "gui tiet kiem", "quy du phong")),
        Rule(Category.DEBT_LOAN, setOf("tra no", "vay", "tra gop", "lai vay")),
        Rule(Category.CHARITY, setOf("tu thien", "quyen gop", "ung ho")),
    )

    private val incomeRules = listOf(
        Rule(Category.SALARY, setOf("tien luong", "luong thang", "salary", "payroll")),
        Rule(Category.BONUS, setOf("thuong", "bonus", "phu cap", "hoa hong")),
        Rule(Category.GIFT, setOf("li xi", "qua tang", "cho tien", "gift")),
        Rule(Category.INVESTMENT, setOf("co tuc", "lai dau tu", "chung khoan", "ban co phieu", "lai tiet kiem")),
    )

    /** Compatibility API for notification capture and existing callers. */
    fun suggest(note: String, isIncome: Boolean): Category =
        suggestDetailed(note, isIncome)?.category
            ?: if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE

    fun suggestDetailed(note: String, isIncome: Boolean): CategorySuggestion? {
        val normalized = normalize(note)
        if (normalized.isBlank()) return null

        val typeKey = typeKey(isIncome)
        val available = Category.getAll().filter { it.isIncome == isIncome }
        if (available.isEmpty()) return null

        exactNoteSuggestion(normalized, typeKey, available)?.let { return it }
        ruleSuggestion(normalized, available, isIncome)?.let { return it }
        return learnedSuggestion(normalized, typeKey, available)
    }

    /**
     * Persists feedback from a saved transaction. Data is partitioned by income/expense so an
     * expense vocabulary can never bias income proposals (the old implementation mixed them).
     */
    fun learn(
        note: String,
        categoryId: String,
        isIncome: Boolean = Category.safeValueOf(categoryId).isIncome,
    ) {
        val normalized = normalize(note)
        val tokens = tokenize(normalized)
        if (normalized.isBlank() || tokens.isEmpty()) return

        val category = Category.getAll().firstOrNull {
            it.id == categoryId && it.isIncome == isIncome
        } ?: return
        val typeKey = typeKey(isIncome)
        val editor = prefs.edit()

        val totalKey = "v2_total_$typeKey"
        editor.putInt(totalKey, prefs.getInt(totalKey, 0) + 1)

        val categoryCountKey = "v2_category_${typeKey}_${category.id}"
        editor.putInt(categoryCountKey, prefs.getInt(categoryCountKey, 0) + 1)

        val totalWordsKey = "v2_words_${typeKey}_${category.id}"
        editor.putInt(totalWordsKey, prefs.getInt(totalWordsKey, 0) + tokens.size)

        val vocabularyKey = "v2_vocabulary_$typeKey"
        val vocabulary = prefs.getStringSet(vocabularyKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        tokens.forEach { token ->
            val key = "v2_token_${typeKey}_${token}_${category.id}"
            editor.putInt(key, prefs.getInt(key, 0) + 1)
            vocabulary.add(token)
        }
        editor.putStringSet(vocabularyKey, vocabulary)

        val exactKey = exactKey(typeKey, normalized)
        editor.putString(exactKey, category.id)
        editor.putInt("${exactKey}_count", prefs.getInt("${exactKey}_count", 0) + 1)
        editor.apply()
    }

    private fun exactNoteSuggestion(
        normalized: String,
        typeKey: String,
        available: List<Category>,
    ): CategorySuggestion? {
        val key = exactKey(typeKey, normalized)
        val categoryId = prefs.getString(key, null) ?: return null
        val timesSeen = prefs.getInt("${key}_count", 0)
        val category = available.firstOrNull { it.id == categoryId } ?: return null
        if (timesSeen < 1) return null

        return CategorySuggestion(
            category = category,
            confidence = if (timesSeen >= 2) 0.98f else 0.93f,
            reason = "Dựa trên ghi chú tương tự bạn đã lưu trước đây",
        )
    }

    private fun ruleSuggestion(
        normalized: String,
        available: List<Category>,
        isIncome: Boolean,
    ): CategorySuggestion? {
        val tokens = tokenize(normalized)
        val rules = if (isIncome) incomeRules else expenseRules
        val match = rules
            .filter { rule -> rule.category in available }
            .mapNotNull { rule ->
                val score = rule.phrases.maxOfOrNull { phrase -> phraseScore(normalized, tokens, phrase) } ?: 0
                rule.takeIf { score > 0 }?.let { it to score }
            }
            .maxByOrNull { it.second }
            ?: return null

        val matchedPhrase = match.first.phrases.maxByOrNull { phrase -> phraseScore(normalized, tokens, phrase) }.orEmpty()
        return CategorySuggestion(
            category = match.first.category,
            confidence = (0.80f + match.second.coerceAtMost(4) * 0.04f).coerceAtMost(0.96f),
            reason = "Nhận diện cụm từ “$matchedPhrase” trong ghi chú",
        )
    }

    private fun learnedSuggestion(
        normalized: String,
        typeKey: String,
        available: List<Category>,
    ): CategorySuggestion? {
        val totalLearned = prefs.getInt("v2_total_$typeKey", 0)
        if (totalLearned < 3) return null

        val tokens = tokenize(normalized)
        val vocabularySize = prefs.getStringSet("v2_vocabulary_$typeKey", emptySet())?.size?.coerceAtLeast(1) ?: 1
        val seenTokenCount = tokens.count { token ->
            available.any { category -> prefs.getInt("v2_token_${typeKey}_${token}_${category.id}", 0) > 0 }
        }
        if (seenTokenCount == 0) return null

        val logScores = available.associateWith { category ->
            val categoryCount = prefs.getInt("v2_category_${typeKey}_${category.id}", 0)
            var score = kotlin.math.ln((categoryCount + 1.0) / (totalLearned + available.size))
            val totalWords = prefs.getInt("v2_words_${typeKey}_${category.id}", 0)
            tokens.forEach { token ->
                val tokenCount = prefs.getInt("v2_token_${typeKey}_${token}_${category.id}", 0)
                score += kotlin.math.ln((tokenCount + 1.0) / (totalWords + vocabularySize))
            }
            score
        }
        val ranked = logScores.entries.sortedByDescending { it.value }
        val winner = ranked.firstOrNull() ?: return null
        val runnerUp = ranked.getOrNull(1)
        val maximum = winner.value
        val normalizer = ranked.sumOf { exp(it.value - maximum) }
        val confidence = (exp(winner.value - maximum) / normalizer).toFloat()
        val margin = winner.value - (runnerUp?.value ?: Double.NEGATIVE_INFINITY)
        val samplesForWinner = prefs.getInt("v2_category_${typeKey}_${winner.key.id}", 0)

        // Avoid a confident-looking guess based only on category frequency.
        if (confidence < 0.62f || margin < 0.22 || samplesForWinner < 2) return null

        return CategorySuggestion(
            category = winner.key,
            confidence = confidence,
            reason = "Dựa trên thói quen phân loại của bạn với các từ tương tự",
        )
    }

    private fun phraseScore(normalized: String, tokens: List<String>, phrase: String): Int {
        val normalizedPhrase = normalize(phrase)
        if (' ' !in normalizedPhrase) return if (normalizedPhrase in tokens) 1 else 0
        val index = normalized.indexOf(normalizedPhrase)
        if (index < 0) return 0
        val beforeIsWord = index > 0 && normalized[index - 1].isLetterOrDigit()
        val end = index + normalizedPhrase.length
        val afterIsWord = end < normalized.length && normalized[end].isLetterOrDigit()
        return if (!beforeIsWord && !afterIsWord) normalizedPhrase.split(' ').size else 0
    }

    private fun typeKey(isIncome: Boolean) = if (isIncome) "income" else "expense"

    private fun exactKey(typeKey: String, normalized: String) =
        "v2_exact_${typeKey}_${sha256(normalized.replace(Regex("\\d+"), "#"))}"

    private fun sha256(value: String): String = MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun normalize(text: String): String = StringUtils
        .removeVietnameseAccents(text)
        .lowercase(Locale.ROOT)
        .trim()
        .replace(Regex("\\s+"), " ")

    private fun tokenize(text: String): List<String> = text
        .split(Regex("[^a-zA-Z0-9]+"))
        .map(String::trim)
        .filter { it.length >= 2 }
        .distinct()
}
