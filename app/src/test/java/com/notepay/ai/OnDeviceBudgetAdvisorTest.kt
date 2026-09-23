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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "vi")
class OnDeviceBudgetAdvisorTest {
    private val geminiNano = mockk<GeminiNanoBudgetAdvisor>()
    private val cloudAdvisor = mockk<CloudGeminiAdvisor>()
    private val advisor = OnDeviceBudgetAdvisor(
        geminiNano = geminiNano,
        cloudAdvisor = cloudAdvisor,
        context = RuntimeEnvironment.getApplication(),
    )

    @Test
    fun `availability prefers Gemini Nano over Cloud AI`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns true

        assertThat(advisor.availability()).isEqualTo(AdvisorAvailability.GEMINI_NANO)
        coVerify(exactly = 0) { cloudAdvisor.isConfigured() }
    }

    @Test
    fun `availability uses Cloud AI when Gemini Nano is unavailable and Cloud is configured`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns true

        assertThat(advisor.availability()).isEqualTo(AdvisorAvailability.CLOUD_GEMINI)
    }

    @Test
    fun `availability falls back to STATISTICAL_ONLY when neither is available`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns false

        assertThat(advisor.availability()).isEqualTo(AdvisorAvailability.STATISTICAL_ONLY)
    }

    @Test
    fun `generate uses Cloud Gemini when Gemini Nano is unavailable`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns true
        coEvery { cloudAdvisor.generate(input) } returns cloudResult

        assertThat(advisor.generate(input)).isEqualTo(cloudResult)
        coVerify(exactly = 0) { geminiNano.generate(any()) }
    }

    @Test
    fun `generate keeps statistical analysis when no AI model or key is available`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns false
        coEvery { geminiNano.generate(input) } returns statisticalResult

        val result = advisor.generate(input)

        assertThat(result.provider).isEqualTo(AdvisorProvider.STATISTICAL_FALLBACK)
    }

    @Test
    fun `generate falls back safely when Cloud AI network fails`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns true
        coEvery { cloudAdvisor.generate(input) } throws java.io.IOException("Network unavailable")
        coEvery { geminiNano.generate(input) } returns statisticalResult

        val result = advisor.generate(input)

        assertThat(result.provider).isEqualTo(AdvisorProvider.STATISTICAL_FALLBACK)
        assertThat(result.providerMessage).contains("Network unavailable")
    }

    @Test
    fun `generate preserves genuine caller cancellation`() = runTest {
        coEvery { geminiNano.isGeminiNanoAvailable() } returns false
        coEvery { cloudAdvisor.isConfigured() } returns true
        coEvery { cloudAdvisor.generate(input) } throws CancellationException("cancelled")

        val thrown = kotlin.test.assertFailsWith<CancellationException> {
            advisor.generate(input)
        }

        assertThat(thrown.message).isEqualTo("cancelled")
        coVerify(exactly = 0) { geminiNano.generate(input) }
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

        val cloudResult = BudgetAdvisorResult(
            title = "Cloud AI",
            content = "Cloud model result",
            provider = AdvisorProvider.CLOUD_GEMINI,
        )

        val statisticalResult = BudgetAdvisorResult(
            title = "Statistics",
            content = "Deterministic result",
            provider = AdvisorProvider.STATISTICAL_FALLBACK,
        )
    }
}
