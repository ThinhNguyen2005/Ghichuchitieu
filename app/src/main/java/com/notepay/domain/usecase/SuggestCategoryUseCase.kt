package com.notepay.domain.usecase

import com.notepay.domain.model.Category
import com.notepay.domain.repository.CategoryLearningEntry
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningType
import com.notepay.core.common.util.StringUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/** A category proposal together with structured evidence for presentation mapping. */
data class CategorySuggestion(
    val category: Category,
    val confidence: Float,
    val reason: CategorySuggestionReason,
)

sealed interface CategorySuggestionReason {
    data object History : CategorySuggestionReason
    data class Phrase(val phrase: String) : CategorySuggestionReason
    data object Learned : CategorySuggestionReason
}

/**
 * On-device category classifier.
 *
 * It deliberately returns no proposal when evidence is weak. A suggestion can come from a
 * repeated merchant/note, a precise merchant keyword, or a per-type Naive Bayes model learned
 * only from transactions the user has saved. No transaction text leaves the device.
 */
@Singleton
class SuggestCategoryUseCase @Inject constructor(
    private val learningStore: CategoryLearningStore,
) {
    private data class Rule(val category: Category, val phrases: Set<String>)

    private val expenseRules = listOf(
        Rule(
            Category.TRANSPORT,
            setOf(
                "grab", "gojek", "xanh sm", "taxi", "be", "be car", "be bike", "di be", "dat be",
                "xe buyt", "ve xe", "gui xe", "giu xe", "parking", "do xang", "rua xe", "cau duong",
                "di chuyen", "xe om", "xe khach", "ve tau", "tau hoa", "tau dien", "metro",
                "thue xe", "cuoc xe", "tien xe", "ve gui xe", "ve thang", "grab bike", "grab car",
            ),
        ),
        Rule(Category.GAS, setOf("xang", "xang xe", "tien xang", "do dau", "xang dau", "petrolimex", "pvoil", "shell", "caltex")),
        Rule(
            Category.FOOD,
            setOf(
                "quan an", "nha hang", "com tam", "com ga", "com suon", "com van phong", "com binh dan",
                "com rang", "com chien", "an com", "pho", "pho bo", "pho ga", "bun bo", "bun cha",
                "bun dau", "bun rieu", "bun thit nuong", "hu tieu", "banh mi", "mi quang", "mi tom",
                "mi cay", "banh canh", "banh cuon", "banh xeo", "banh bao", "banh ngot", "tra sua",
                "uong tra sua", "tra da", "tra dao", "tra chanh", "grabfood", "shopeefood", "baemin",
                "beamin", "an sang", "an trua", "an toi", "an vat", "an chieu", "an khuya",
                "buoi sang", "buoi trua", "buoi toi", "do an", "thuc an", "an uong", "lau", "nuong",
                "xoi", "chao", "nhau", "quan nhau", "bia", "nuoc ngot", "nuoc mia", "nuoc ep",
                "sinh to", "kem", "che", "pizza", "ga ran", "kfc", "lotteria", "jollibee",
                "hai san", "sushi", "buffet", "dimsum",
            ),
        ),
        Rule(
            Category.COFFEE,
            setOf(
                "ca phe", "cafe", "caphe", "cf", "bac xiu", "den da", "sua da", "latte",
                "americano", "espresso", "cappuccino", "matcha", "tra vai", "uong nuoc",
                "highlands", "highland", "starbucks", "phuc long", "the coffee house",
                "katinat", "phe la", "cong ca phe", "trung nguyen",
            ),
        ),
        Rule(
            Category.SHOPPING,
            setOf(
                "shopee", "lazada", "tiki", "mua sam", "mua hang", "mua do", "amazon",
                "bach hoa xanh", "bhx", "winmart", "vinmart", "co.opmart", "co opmart", "coopmart",
                "sieu thi", "di cho", "tap hoa", "tiem tap hoa", "cua hang tien loi", "circle k",
                "gs25", "ministop", "7 eleven", "tiktok shop", "dien may xanh", "aeon",
                "lotte mart", "emart", "big c", "mega market", "con cung", "do gia dung",
                "vat dung", "do dung", "phu kien", "order", "dat hang", "mua online",
                "phi ship", "tien ship", "giao hang",
            ),
        ),
        Rule(
            Category.FAMILY,
            setOf(
                "gui me", "gui ba", "gui bo", "gui nha", "bieu bo", "bieu me", "bieu ba",
                "bieu ong ba", "li xi", "cho me", "cho bo", "cho ba", "tien cho me", "tien cho bo",
                "mung cuoi", "dam cuoi", "dam gio", "mung tho", "ve que", "tham nha",
                "gia dinh", "cho tang",
            ),
        ),
        Rule(
            Category.CLOTHES,
            setOf(
                "quan ao", "thoi trang", "giay dep", "mua ao", "mua quan", "ao khoac", "ao thun",
                "so mi", "quan jean", "quan tay", "vay", "dam", "giay", "dep", "sandal",
                "mua giay", "mua dep", "tui xach", "balo", "uniqlo", "zara", "hm", "coolmate",
                "yody", "owen", "routine", "canifa",
            ),
        ),
        Rule(
            Category.BEAUTY,
            setOf(
                "my pham", "lam toc", "cat toc", "uon toc", "nhuom toc", "goi dau", "spa", "nail",
                "lam mong", "skincare", "cham soc da", "duong da", "trang diem", "lam dep",
                "son moi", "kem chong nang", "sua rua mat", "nuoc hoa", "triet long",
                "hasaki", "guardian", "watsons",
            ),
        ),
        Rule(Category.ELECTRICITY, setOf("tien dien", "hoa don dien", "dong tien dien", "so dien", "evn", "dien luc")),
        Rule(Category.WATER, setOf("tien nuoc", "hoa don nuoc", "dong tien nuoc", "cap nuoc", "nuoc sinh hoat", "binh nuoc")),
        Rule(
            Category.INTERNET,
            setOf(
                "cuoc mang", "tien mang", "dong mang", "tien wifi", "nap 4g", "goi 4g", "cuoc 4g",
                "data 4g", "cuoc dien thoai", "viettel", "vinaphone", "mobifone", "vnpt", "fpt telecom",
            ),
        ),
        Rule(
            Category.HOME,
            setOf(
                "tien nha", "tien thue", "thue nha", "chung cu", "phi chung cu", "noi that",
                "tien tro", "tien phong", "phong tro", "nha tro", "thue tro", "dien nuoc",
                "tien dien nuoc", "internet", "wifi", "cap quang", "tien gas", "doi gas", "binh gas",
                "ve sinh", "tien rac", "tu lanh", "may giat", "dieu hoa", "may lanh", "gia dung",
            ),
        ),
        Rule(
            Category.BILL,
            setOf("hoa don", "dong hoa don", "thanh toan hoa don", "phi dich vu", "phi quan ly", "tien cap", "truyen hinh", "rac"),
        ),
        Rule(
            Category.ENTERTAINMENT,
            setOf(
                "netflix", "spotify", "youtube premium", "icloud", "chatgpt", "cgv", "rap phim",
                "xem phim", "ve xem phim", "karaoke", "billiards", "bi a", "bowling", "concert",
                "steam", "game", "choi game", "nap game", "mua game", "giai tri",
            ),
        ),
        Rule(
            Category.TRAVEL,
            setOf(
                "may bay", "ve bay", "ve may bay", "khach san", "phong khach san", "homestay",
                "resort", "booking", "agoda", "traveloka", "du lich", "di choi", "phuot", "tour",
            ),
        ),
        Rule(
            Category.HEALTH,
            setOf(
                "benh vien", "phong kham", "nha khoa", "thuoc", "mua thuoc", "tiem thuoc",
                "nha thuoc", "long chau", "pharmacity", "an khang", "kham benh", "kham suc khoe",
                "bac si", "xet nghiem", "vitamin", "bao hiem y te", "nho rang", "lay cao rang",
            ),
        ),
        Rule(
            Category.EDUCATION,
            setOf(
                "hoc phi", "tien hoc", "hoc them", "khoa hoc", "sach", "mua sach", "nha sach",
                "fahasa", "sach vo", "van phong pham", "trung tam", "gia su", "hoc tieng anh",
                "ielts", "toeic",
            ),
        ),
        Rule(
            Category.PETS,
            setOf("thu cung", "cho meo", "thuc an cho", "thuc an meo", "hat cho meo", "pate", "cat cho", "tam cho", "thu y"),
        ),
        Rule(
            Category.SPORTS,
            setOf(
                "the thao", "gym", "tap gym", "phong gym", "yoga", "pilates", "cau long",
                "da bong", "san bong", "boi loi", "ve boi", "chay bo", "pickleball", "tennis",
                "the hinh", "thue san",
            ),
        ),
        Rule(
            Category.REPAIR,
            setOf(
                "sua chua", "sua xe", "thay nhot", "thay dau", "va xe", "bao duong",
                "sua dien thoai", "ep kinh", "thay pin", "sua may tinh", "sua laptop",
                "sua may lanh", "ve sinh may lanh", "sua ong nuoc", "thay bong den",
            ),
        ),
        Rule(
            Category.CHILDREN,
            setOf("con cai", "bim sua", "ta bim", "sua bot", "do choi tre em", "hoc phi con", "em be", "cho be", "sua cho be", "nha tre", "mau giao"),
        ),
        Rule(Category.INSURANCE, setOf("bao hiem", "dong bao hiem", "bao hiem xe", "bao hiem nhan tho", "bhyt", "bhxh")),
        Rule(Category.TAX, setOf("thue", "dong thue", "thue thu nhap", "thue tncn", "phi truoc ba", "phat nguoi", "dong phat", "phi cau duong")),
        Rule(Category.SAVINGS, setOf("tiet kiem", "gui tiet kiem", "quy du phong", "bo heo", "tich luy")),
        Rule(Category.DEBT_LOAN, setOf("tra no", "vay", "cho vay", "muon tien", "tra gop", "lai vay", "tra the tin dung")),
        Rule(Category.CHARITY, setOf("tu thien", "quyen gop", "ung ho", "cong duc", "phong sinh")),
    )

    private val incomeRules = listOf(
        Rule(
            Category.SALARY,
            setOf(
                "luong", "nhan luong", "tien luong", "luong thang", "luong ve",
                "tien cong", "nhan tien cong", "ung luong", "freelance", "salary", "payroll",
            ),
        ),
        Rule(Category.BONUS, setOf("thuong", "tien thuong", "thuong tet", "thuong doanh so", "bonus", "phu cap", "hoa hong", "kpi")),
        Rule(Category.GIFT, setOf("li xi", "duoc li xi", "qua tang", "cho tien", "duoc tang", "duoc cho", "nhan qua", "tien mung", "gift")),
        Rule(
            Category.INVESTMENT,
            setOf(
                "co tuc", "lai dau tu", "chung khoan", "ban co phieu", "lai tiet kiem", "tien lai",
                "ban hang", "doanh thu", "loi nhuan", "hoan tien", "cashback", "khach tra",
            ),
        ),
    )

    /** Nạp trước kho dữ liệu học danh mục vào RAM (nên gọi trên background dispatcher). */
    fun warmUp() {
        learningStore.warmUp()
    }

    /** Compatibility API for notification capture and existing callers. */
    fun suggest(note: String, isIncome: Boolean): Category =
        suggestDetailed(note, isIncome)?.category
            ?: if (isIncome) Category.DEFAULT_INCOME else Category.DEFAULT_EXPENSE

    fun suggestDetailed(note: String, isIncome: Boolean): CategorySuggestion? {
        val normalized = normalize(note)
        if (normalized.isBlank()) return null

        val type = learningType(isIncome)
        val available = Category.getAll().filter { it.isIncome == isIncome }
        if (available.isEmpty()) return null

        exactNoteSuggestion(normalized, type, available)?.let { return it }
        val learned = learnedSuggestion(normalized, type, available)
        if (learned != null && learned.confidence >= 0.78f) return learned
        val tokens = tokenize(normalized)
        displayNameSuggestion(normalized, tokens, available.filter { it.isCustom })?.let { return it }
        ruleSuggestion(normalized, available, isIncome)?.let { return it }
        displayNameSuggestion(normalized, tokens, available.filterNot { it.isCustom })?.let { return it }
        return learned
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
        learningStore.learn(
            CategoryLearningEntry(
                type = learningType(isIncome),
                categoryId = category.id,
                normalizedNote = normalized,
                tokens = tokens,
            ),
        )
    }

    private fun exactNoteSuggestion(
        normalized: String,
        type: CategoryLearningType,
        available: List<Category>,
    ): CategorySuggestion? {
        val match = learningStore.findExact(type, normalized) ?: return null
        val category = available.firstOrNull { it.id == match.categoryId } ?: return null
        val timesSeen = match.timesSeen
        if (timesSeen < 1) return null

        return CategorySuggestion(
            category = category,
            confidence = if (timesSeen >= 2) 0.98f else 0.93f,
            reason = CategorySuggestionReason.History,
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
            reason = CategorySuggestionReason.Phrase(matchedPhrase),
        )
    }

    private fun displayNameSuggestion(
        normalized: String,
        tokens: List<String>,
        candidates: List<Category>,
    ): CategorySuggestion? {
        if (candidates.isEmpty()) return null
        val best = candidates
            .asSequence()
            .filter { it.id != Category.OTHER.id && it.id != Category.INCOME_OTHER.id }
            .mapNotNull { category ->
                val variants = buildList {
                    val full = normalize(category.displayName)
                    if (full.length >= 3) add(full)
                    category.displayName
                        .split('/')
                        .map(::normalize)
                        .filter { it.length >= 3 }
                        .forEach(::add)
                }.distinct()
                val score = variants.maxOfOrNull { phraseScore(normalized, tokens, it) } ?: 0
                category.takeIf { score > 0 }?.let { it to score }
            }
            .maxByOrNull { it.second }
            ?: return null

        return CategorySuggestion(
            category = best.first,
            confidence = if (best.first.isCustom) 0.90f else 0.82f,
            reason = CategorySuggestionReason.Phrase(best.first.displayName),
        )
    }

    private fun learnedSuggestion(
        normalized: String,
        type: CategoryLearningType,
        available: List<Category>,
    ): CategorySuggestion? {
        val tokens = tokenize(normalized)
        val snapshot = learningStore.read(
            type = type,
            categoryIds = available.mapTo(linkedSetOf()) { it.id },
            tokens = tokens.toSet(),
        )
        val totalLearned = snapshot.totalSamples
        if (totalLearned < 3) return null

        val vocabularySize = snapshot.vocabularySize.coerceAtLeast(1)
        val seenTokenCount = tokens.count { token ->
            available.any { category -> snapshot.tokenSamples(category.id, token) > 0 }
        }
        if (seenTokenCount == 0) return null

        val logScores = available.associateWith { category ->
            val categoryCount = snapshot.categorySamples(category.id)
            var score = kotlin.math.ln((categoryCount + 1.0) / (totalLearned + available.size))
            val totalWords = snapshot.categoryWords(category.id)
            tokens.forEach { token ->
                val tokenCount = snapshot.tokenSamples(category.id, token)
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
        val samplesForWinner = snapshot.categorySamples(winner.key.id)

        // Avoid a confident-looking guess based only on category frequency.
        if (confidence < 0.62f || margin < 0.22 || samplesForWinner < 2) return null

        return CategorySuggestion(
            category = winner.key,
            confidence = confidence,
            reason = CategorySuggestionReason.Learned,
        )
    }

    private fun phraseScore(normalized: String, tokens: List<String>, phrase: String): Int {
        val normalizedPhrase = normalize(phrase)
        if (normalizedPhrase.all { it.isLetterOrDigit() }) {
            return if (normalizedPhrase in tokens) 1 else 0
        }
        val index = normalized.indexOf(normalizedPhrase)
        if (index < 0) return 0
        val beforeIsWord = index > 0 && normalized[index - 1].isLetterOrDigit()
        val end = index + normalizedPhrase.length
        val afterIsWord = end < normalized.length && normalized[end].isLetterOrDigit()
        return if (!beforeIsWord && !afterIsWord) normalizedPhrase.split(Regex("[^a-zA-Z0-9]+")).count { it.isNotEmpty() }.coerceAtLeast(1) else 0
    }

    private fun learningType(isIncome: Boolean) =
        if (isIncome) CategoryLearningType.INCOME else CategoryLearningType.EXPENSE

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
