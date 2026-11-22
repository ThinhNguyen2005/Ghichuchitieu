package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.data.datastore.AiSettingsDataStore
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.natural.NaturalFallbackNotice
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AiNaturalTransactionParserTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var aiSettings: AiSettingsDataStore
    private lateinit var fixedClock: Clock
    private val fixedZone = TimeZone.of("Asia/Ho_Chi_Minh")

    @Before
    fun setUp() {
        aiSettings = mockk(relaxed = true) {
            every { cloudAiEnabled } returns flowOf(true)
            every { geminiApiKey } returns flowOf("AIzaSyTestKey123456")
            every { cloudModelName } returns flowOf(AiSettingsDataStore.DEFAULT_MODEL)
        }
        fixedClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-10-20T05:00:00Z")
        }
    }

    private fun sampleTier1(needsAiFallback: Boolean): NaturalParseResult = NaturalParseResult(
        drafts = listOf(
            NaturalTransactionDraft(
                id = "t1_0",
                amountVnd = 35_000L,
                type = TransactionType.EXPENSE,
                category = Category.FOOD,
                note = "ăn sáng",
                date = LocalDate(2026, 10, 20),
                confidence = 0.90f,
            ),
        ),
        confidence = 0.90f,
        needsAiFallback = needsAiFallback,
        source = NaturalParseSource.ON_DEVICE,
    )

    @Test
    fun `does not call AI when needsAiFallback is false and forceAi is false`() = runTest(mainDispatcherRule.testDispatcher) {
        var calls = 0
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ ->
                calls++
                "{}"
            },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = sampleTier1(needsAiFallback = false)
        val result = parser.parseOrFallback("ăn sáng 35k", tier1, forceAi = false)

        assertThat(calls).isEqualTo(0)
        assertThat(result).isEqualTo(tier1)
    }

    @Test
    fun `does not call AI when cloudAiEnabled is false or API key missing`() = runTest(mainDispatcherRule.testDispatcher) {
        every { aiSettings.cloudAiEnabled } returns flowOf(false)
        var calls = 0
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ ->
                calls++
                "{}"
            },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = sampleTier1(needsAiFallback = true)
        val result = parser.parseOrFallback("câu phức tạp", tier1, forceAi = true)

        assertThat(calls).isEqualTo(0)
        assertThat(result.drafts).isEqualTo(tier1.drafts)
        assertThat(result.fallbackNotice).isEqualTo(NaturalFallbackNotice.AI_UNAVAILABLE)
    }

    @Test
    fun `parses valid strict JSON response into AI_FALLBACK drafts`() = runTest(mainDispatcherRule.testDispatcher) {
        val jsonPayload = """
            {
              "transactions": [
                {
                  "amountVnd": 45000,
                  "type": "EXPENSE",
                  "categoryId": "FOOD",
                  "note": "Ăn trưa",
                  "dateIso": "2026-10-20"
                },
                {
                  "amountVnd": 55000,
                  "type": "EXPENSE",
                  "categoryId": "COFFEE",
                  "note": "Trà sữa",
                  "dateIso": "2026-10-20"
                }
              ]
            }
        """.trimIndent()
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ -> jsonPayload },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = NaturalParseResult(emptyList(), 0f, needsAiFallback = true)
        val result = parser.parseOrFallback("ăn trưa với uống trà sữa", tier1, forceAi = true)

        assertThat(result.source).isEqualTo(NaturalParseSource.AI_FALLBACK)
        assertThat(result.needsAiFallback).isFalse()
        assertThat(result.fallbackNotice).isEqualTo(NaturalFallbackNotice.NONE)
        assertThat(result.drafts).hasSize(2)
        assertThat(result.drafts[0].amountVnd).isEqualTo(45_000L)
        assertThat(result.drafts[0].category).isEqualTo(Category.FOOD)
        assertThat(result.drafts[1].amountVnd).isEqualTo(55_000L)
        assertThat(result.drafts[1].category).isEqualTo(Category.COFFEE)
    }

    @Test
    fun `falls back safely to Tier 1 on network error`() = runTest(mainDispatcherRule.testDispatcher) {
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ -> throw IOException("No network") },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = sampleTier1(needsAiFallback = true)
        val result = parser.parseOrFallback("ăn sáng 35k và...", tier1, forceAi = true)

        assertThat(result.source).isEqualTo(NaturalParseSource.ON_DEVICE)
        assertThat(result.drafts).isEqualTo(tier1.drafts)
        assertThat(result.fallbackNotice).isEqualTo(NaturalFallbackNotice.AI_UNAVAILABLE)
    }

    @Test
    fun `falls back safely to Tier 1 on malformed JSON`() = runTest(mainDispatcherRule.testDispatcher) {
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ -> "{not valid json" },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = sampleTier1(needsAiFallback = true)
        val result = parser.parseOrFallback("câu lỗi json", tier1, forceAi = true)

        assertThat(result.drafts).isEqualTo(tier1.drafts)
        assertThat(result.fallbackNotice).isEqualTo(NaturalFallbackNotice.AI_UNAVAILABLE)
    }

    @Test
    fun `falls back safely to Tier 1 on timeout`() = runTest(mainDispatcherRule.testDispatcher) {
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ ->
                delay(15_000L)
                "{}"
            },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        val tier1 = sampleTier1(needsAiFallback = true)
        val result = parser.parseOrFallback("timeout test", tier1, forceAi = true)

        assertThat(result.drafts).isEqualTo(tier1.drafts)
        assertThat(result.fallbackNotice).isEqualTo(NaturalFallbackNotice.AI_UNAVAILABLE)
    }

    @Test
    fun `rethrows CancellationException without swallowing`() = runTest(mainDispatcherRule.testDispatcher) {
        val parser = AiNaturalTransactionParser(
            aiSettings = aiSettings,
            ioDispatcher = mainDispatcherRule.testDispatcher,
            jsonGenerator = NaturalAiJsonGenerator { _, _, _, _ -> throw CancellationException("Cancelled") },
            clock = fixedClock,
            timeZone = fixedZone,
        )

        var thrown = false
        try {
            parser.parseOrFallback("cancel test", sampleTier1(needsAiFallback = true), forceAi = true)
        } catch (_: CancellationException) {
            thrown = true
        }
        assertThat(thrown).isTrue()
    }
}
