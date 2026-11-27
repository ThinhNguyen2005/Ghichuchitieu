package com.notepay.domain.natural

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.repository.CategoryLearningEntry
import com.notepay.domain.repository.CategoryLearningMatch
import com.notepay.domain.repository.CategoryLearningSnapshot
import com.notepay.domain.repository.CategoryLearningStore
import com.notepay.domain.repository.CategoryLearningType
import com.notepay.domain.usecase.SuggestCategoryUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Instant

class VietnameseNaturalInputParserTest {

    private lateinit var learningStore: FakeCategoryLearningStore
    private lateinit var suggestCategoryUseCase: SuggestCategoryUseCase
    private lateinit var dateParser: VietnameseDateKeywordParser
    private lateinit var parser: VietnameseNaturalInputParser

    @Before
    fun setUp() {
        learningStore = FakeCategoryLearningStore()
        suggestCategoryUseCase = SuggestCategoryUseCase(learningStore)
        val fixedClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-10-20T05:00:00Z")
        }
        dateParser = VietnameseDateKeywordParser(fixedClock, TimeZone.of("Asia/Ho_Chi_Minh"))
        parser = VietnameseNaturalInputParser(suggestCategoryUseCase, dateParser)
    }

    @Test
    fun `parse instant single breakfast expense with zero AI fallback`() {
        val result = parser.parse("ăn sáng 35k")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.source).isEqualTo(NaturalParseSource.ON_DEVICE)
        assertThat(result.drafts).hasSize(1)
        val draft = result.drafts.single()
        assertThat(draft.amountVnd).isEqualTo(35_000L)
        assertThat(draft.category).isEqualTo(Category.FOOD)
        assertThat(draft.date).isEqualTo(LocalDate(2026, 10, 20))
    }

    @Test
    fun `parse recognizes bun bo and pho as FOOD`() {
        val bunBo = parser.parse("bún bò 50k").drafts.single()
        val pho = parser.parse("phở bò 55k").drafts.single()
        assertThat(bunBo.category).isEqualTo(Category.FOOD)
        assertThat(bunBo.amountVnd).isEqualTo(50_000L)
        assertThat(pho.category).isEqualTo(Category.FOOD)
    }

    @Test
    fun `parse recognizes grab be xanh sm as TRANSPORT`() {
        assertThat(parser.parse("grab 45k").drafts.single().category).isEqualTo(Category.TRANSPORT)
        assertThat(parser.parse("đặt be đi làm 35k").drafts.single().category).isEqualTo(Category.TRANSPORT)
        assertThat(parser.parse("xanh sm 60k").drafts.single().category).isEqualTo(Category.TRANSPORT)
    }

    @Test
    fun `parse recognizes Bach Hoa Xanh WinMart Coopmart Shopee as SHOPPING`() {
        assertThat(parser.parse("Bách Hoá Xanh 250k").drafts.single().category).isEqualTo(Category.SHOPPING)
        assertThat(parser.parse("WinMart 120k").drafts.single().category).isEqualTo(Category.SHOPPING)
        assertThat(parser.parse("Co.opmart 3 xị").drafts.single().category).isEqualTo(Category.SHOPPING)
        assertThat(parser.parse("Shopee nửa triệu").drafts.single().category).isEqualTo(Category.SHOPPING)
    }

    @Test
    fun `parse recognizes gui me bieu bo li xi expense as FAMILY`() {
        assertThat(parser.parse("gửi mẹ 2 củ").drafts.single().category).isEqualTo(Category.FAMILY)
        assertThat(parser.parse("biếu bố 500 ngàn").drafts.single().category).isEqualTo(Category.FAMILY)
        assertThat(parser.parse("lì xì 2 lít").drafts.single().category).isEqualTo(Category.FAMILY)
    }

    @Test
    fun `parse recognizes tien tro dien nuoc internet as HOME`() {
        assertThat(parser.parse("tiền trọ 3tr5").drafts.single().category).isEqualTo(Category.HOME)
        assertThat(parser.parse("điện nước 400k").drafts.single().category).isEqualTo(Category.HOME)
        assertThat(parser.parse("internet 220k").drafts.single().category).isEqualTo(Category.HOME)
    }

    @Test
    fun `parse splits compound sentence hom nay an trua 45k roi uong tra sua 55k into two drafts`() {
        val result = parser.parse("hôm nay ăn trưa 45k rồi uống trà sữa 55k")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.drafts).hasSize(2)

        assertThat(result.drafts[0].amountVnd).isEqualTo(45_000L)
        assertThat(result.drafts[0].category).isEqualTo(Category.FOOD)
        assertThat(result.drafts[0].note).contains("ăn trưa")
        assertThat(result.drafts[0].date).isEqualTo(LocalDate(2026, 10, 20))

        assertThat(result.drafts[1].amountVnd).isEqualTo(55_000L)
        assertThat(result.drafts[1].category).isEqualTo(Category.FOOD)
        assertThat(result.drafts[1].note).contains("trà sữa")
        assertThat(result.drafts[1].date).isEqualTo(LocalDate(2026, 10, 20))
    }

    @Test
    fun `parse handles do xang Wave 80 ngan cleanly`() {
        val result = parser.parse("đổ xăng Wave 80 ngàn")
        assertThat(result.needsAiFallback).isFalse()
        val draft = result.drafts.single()
        assertThat(draft.amountVnd).isEqualTo(80_000L)
        assertThat(draft.category).isEqualTo(Category.TRANSPORT)
        assertThat(draft.note).isEqualTo("đổ xăng Wave")
    }

    @Test
    fun `parse handles tra tien tro thang 8 ba trieu ruoi with month keyword and spoken amount`() {
        val result = parser.parse("trả tiền trọ tháng 8 ba triệu rưỡi")
        assertThat(result.needsAiFallback).isFalse()
        val draft = result.drafts.single()
        assertThat(draft.amountVnd).isEqualTo(3_500_000L)
        assertThat(draft.category).isEqualTo(Category.HOME)
        assertThat(draft.date).isEqualTo(LocalDate(2026, 8, 1))
        assertThat(draft.note).isEqualTo("trả tiền trọ")
    }

    @Test
    fun `parse keeps inline voi phrase when only one side has amount`() {
        val result = parser.parse("ăn bún bò với bạn 50k")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.drafts).hasSize(1)
        val draft = result.drafts.single()
        assertThat(draft.amountVnd).isEqualTo(50_000L)
        assertThat(draft.category).isEqualTo(Category.FOOD)
        assertThat(draft.note).contains("với bạn")
    }

    @Test
    fun `parse splits by comma xong sau do and inherits date`() {
        val result = parser.parse("hôm qua ăn phở 45k, mua shopee 2 lít xong gửi xe 10k")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.drafts).hasSize(3)
        assertThat(result.drafts.map { it.amountVnd }).containsExactly(45_000L, 200_000L, 10_000L).inOrder()
        assertThat(result.drafts.map { it.category }).containsExactly(
            Category.FOOD,
            Category.SHOPPING,
            Category.TRANSPORT,
        ).inOrder()
        assertThat(result.drafts.all { it.date == LocalDate(2026, 10, 19) }).isTrue()
    }

    @Test
    fun `parse does not split thousands comma inside 100,000d or decimal 1,5tr`() {
        val result = parser.parse("mua đồ 100,000đ, đóng mạng 1,5tr")
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts[0].amountVnd).isEqualTo(100_000L)
        assertThat(result.drafts[1].amountVnd).isEqualTo(1_500_000L)
    }

    @Test
    fun `parse handles unaccented mixed-case multi-transaction input`() {
        val result = parser.parse("HOM NAY AN BUN BO 50K ROI DI GRAB 35K")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts[0].category).isEqualTo(Category.FOOD)
        assertThat(result.drafts[0].amountVnd).isEqualTo(50_000L)
        assertThat(result.drafts[1].category).isEqualTo(Category.TRANSPORT)
        assertThat(result.drafts[1].amountVnd).isEqualTo(35_000L)
    }

    @Test
    fun `parse detects income cue for salary`() {
        val result = parser.parse("nhận lương tháng 8 15 củ")
        val draft = result.drafts.single()
        assertThat(draft.type).isEqualTo(TransactionType.INCOME)
        assertThat(draft.category).isEqualTo(Category.SALARY)
        assertThat(draft.amountVnd).isEqualTo(15_000_000L)
        assertThat(draft.date).isEqualTo(LocalDate(2026, 8, 1))
    }

    @Test
    fun `parse prioritizes CategoryLearningStoreImpl learned mapping over static VN rules`() {
        suggestCategoryUseCase.learn("bún bò", Category.FAMILY.id, isIncome = false)
        val result = parser.parse("bún bò 50k")
        assertThat(result.drafts.single().category).isEqualTo(Category.FAMILY)
    }

    @Test
    fun `parse handles adjacent amounts without connector in one clause`() {
        val result = parser.parse("ăn sáng 30k trà đá 10k")
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts.map { it.amountVnd }).containsExactly(30_000L, 10_000L).inOrder()
    }

    @Test
    fun `parse requests AI fallback for natural sentence missing numeric amount`() {
        val result = parser.parse("hôm nay đi siêu thị mua nhiều đồ quá")
        assertThat(result.drafts).isEmpty()
        assertThat(result.needsAiFallback).isTrue()
    }

    @Test
    fun `parse flags AI fallback when trailing roi clause has no amount`() {
        val result = parser.parse("ăn phở 50k rồi đi siêu thị mua đồ")
        assertThat(result.drafts).hasSize(1)
        assertThat(result.needsAiFallback).isTrue()
    }

    @Test
    fun `parse returns empty and false fallback for blank input or random garbage`() {
        val blank = parser.parse("   ")
        assertThat(blank.drafts).isEmpty()
        assertThat(blank.needsAiFallback).isFalse()

        val garbage = parser.parse("!!!###@@@ 123")
        assertThat(garbage.drafts).isEmpty()
        assertThat(garbage.needsAiFallback).isFalse()
    }

    @Test
    fun `parse ignores negative and zero amounts without triggering drafts`() {
        assertThat(parser.parse("ăn sáng -50k").drafts).isEmpty()
        assertThat(parser.parse("ăn sáng 0k").drafts).isEmpty()
    }

    @Test
    fun `parse preserves specific date override on second clause`() {
        val result = parser.parse("hôm qua ăn tối 120k rồi ngày 15/10 đóng tiền trọ 3 củ")
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts[0].date).isEqualTo(LocalDate(2026, 10, 19))
        assertThat(result.drafts[1].date).isEqualTo(LocalDate(2026, 10, 15))
        assertThat(result.drafts[1].category).isEqualTo(Category.HOME)
        assertThat(result.drafts[1].amountVnd).isEqualTo(3_000_000L)
    }

    @Test
    fun `parse recognizes 80k5 and 1tr5 slang within full sentences`() {
        val res1 = parser.parse("đi ăn sáng 80k5").drafts.single()
        val res2 = parser.parse("mua quà tặng gia đình 1tr5").drafts.single()
        assertThat(res1.amountVnd).isEqualTo(80_500L)
        assertThat(res2.amountVnd).isEqualTo(1_500_000L)
        assertThat(res2.category).isEqualTo(Category.FAMILY)
    }

    @Test
    fun `parse classifies everyday an com and cat toc without AI fallback`() {
        val result = parser.parse("ăn cơm 45k rồi cắt tóc 80k")
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts[0].category).isEqualTo(Category.FOOD)
        assertThat(result.drafts[0].amountVnd).isEqualTo(45_000L)
        assertThat(result.drafts[1].category).isEqualTo(Category.BEAUTY)
        assertThat(result.drafts[1].amountVnd).isEqualTo(80_000L)
    }

    @Test
    fun `parse handles multi-unit million and thousand amount as one draft`() {
        val result = parser.parse("mua tủ lạnh 2 triệu 500 nghìn")
        assertThat(result.drafts).hasSize(1)
        val draft = result.drafts.single()
        assertThat(draft.amountVnd).isEqualTo(2_500_000L)
        assertThat(draft.category).isEqualTo(Category.HOME)
    }

    @Test
    fun `parse sets needsAiFallback true when note falls back to OTHER`() {
        val result = parser.parse("abc xyz 50k")
        assertThat(result.drafts).hasSize(1)
        assertThat(result.drafts.single().category).isEqualTo(Category.OTHER)
        assertThat(result.needsAiFallback).isTrue()
    }

    private class FakeCategoryLearningStore : CategoryLearningStore {
        private val exactMatches = mutableMapOf<Pair<CategoryLearningType, String>, CategoryLearningMatch>()

        override fun read(
            type: CategoryLearningType,
            categoryIds: Set<String>,
            tokens: Set<String>,
        ): CategoryLearningSnapshot = CategoryLearningSnapshot()

        override fun findExact(type: CategoryLearningType, normalizedNote: String): CategoryLearningMatch? =
            exactMatches[type to normalizedNote]

        override fun learn(entry: CategoryLearningEntry) {
            val key = entry.type to entry.normalizedNote
            val prev = exactMatches[key]
            exactMatches[key] = CategoryLearningMatch(
                categoryId = entry.categoryId,
                timesSeen = (prev?.timesSeen ?: 0) + 1,
            )
        }
    }
}
