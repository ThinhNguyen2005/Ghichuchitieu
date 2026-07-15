package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import com.notepay.domain.analytics.ForecastConfidence
import com.notepay.domain.analytics.SpendingPrediction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OnDeviceBudgetAdvisorTest {
    private val geminiNano = mockk<GeminiNanoBudgetAdvisor>()
    private val liteRt = mockk<LiteRtBudgetAdvisor>()
    private val advisor = OnDeviceBudgetAdvisor(geminiNano, liteRt)

    @Test
    fun `availability prefers Gemini Nano over imported model`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns true

        assertThat(advisor.availability()).isEqualTo(AdvisorAvailability.GEMINI_NANO)
        coVerify(exactly = 0) { liteRt.isModelReady() }
    }

    @Test
    fun `generate uses imported local model when Gemini Nano is unavailable`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { liteRt.isModelReady() } returns true
        coEvery { liteRt.generate(input) } returns localResult

        assertThat(advisor.generate(input)).isEqualTo(localResult)
        coVerify(exactly = 0) { geminiNano.generate(any()) }
    }

    @Test
    fun `generate keeps statistical analysis when no AI model is available`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { liteRt.isModelReady() } returns false
        coEvery { geminiNano.generate(input) } returns statisticalResult

        val result = advisor.generate(input)

        assertThat(result.provider).isEqualTo(AdvisorProvider.STATISTICAL_FALLBACK)
        assertThat(result.providerMessage).contains("LiteRT-LM")
    }

    @Test
    fun `generate does not retry Gemini after it already returned fallback`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns true
        coEvery { geminiNano.generate(input) } returns statisticalResult
        coEvery { liteRt.isModelReady() } returns false

        assertThat(advisor.generate(input).provider)
            .isEqualTo(AdvisorProvider.STATISTICAL_FALLBACK)
        coVerify(exactly = 1) { geminiNano.generate(input) }
    }

    @Test
    fun `generate falls back safely when imported model cannot start`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { liteRt.isModelReady() } returns true
        coEvery { liteRt.generate(input) } throws OutOfMemoryError()
        coEvery { geminiNano.generate(input) } returns statisticalResult

        val result = advisor.generate(input)

        assertThat(result.provider).isEqualTo(AdvisorProvider.STATISTICAL_FALLBACK)
        assertThat(result.providerMessage).contains("th\u1ED1ng k\u00EA")
    }

    private companion object {
        val input = BudgetAdvisorInput(
            prediction = SpendingPrediction(
                spentSoFarInCents = 100_000_00,
                predictedMonthTotalInCents = 200_000_00,
                lowerBoundInCents = 150_000_00,
                upperBoundInCents = 250_000_00,
                dailyRunRateInCents = 10_000_00,
                overBudgetProbability = 0.4,
                trendVsPreviousMonth = 0.1,
                observedDays = 20,
                confidence = ForecastConfidence.MEDIUM,
            ),
            budgetLimitInCents = 220_000_00,
            incomeThisMonthInCents = 500_000_00,
            categories = emptyList(),
        )

        val localResult = BudgetAdvisorResult(
            title = "Local AI",
            content = "Local model result",
            provider = AdvisorProvider.LOCAL_LITERT_MODEL,
        )

        val statisticalResult = BudgetAdvisorResult(
            title = "Statistics",
            content = "Deterministic result",
            provider = AdvisorProvider.STATISTICAL_FALLBACK,
        )
    }
}
