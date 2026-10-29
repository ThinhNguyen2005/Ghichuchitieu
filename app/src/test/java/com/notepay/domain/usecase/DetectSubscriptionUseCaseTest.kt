package com.notepay.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.time.Instant

class DetectSubscriptionUseCaseTest {

    private val zone = TimeZone.UTC

    private fun expense(
        instant: String,
        cents: Long,
        note: String,
        category: Category = Category.ENTERTAINMENT,
    ): Transaction = TestData.transaction(
        amount = Money(cents),
        category = category,
        note = note,
        occurredAt = Instant.parse(instant),
    )

    private fun detect(
        transactions: List<Transaction>,
        activeSubscriptionNames: List<String> = emptyList(),
    ) = DetectSubscriptionUseCase(
        transactions = transactions,
        activeSubscriptionNames = activeSubscriptionNames,
        displayNameOf = { it.displayName },
        zone = zone,
    )

    @Test
    fun `two expenses 30 days apart suggest a monthly subscription with a brand name`() {
        val transactions = listOf(
            expense("2026-01-10T03:00:00Z", 260_000L, note = "SPOTIFY PREMIUM"),
            expense("2026-02-09T03:00:00Z", 260_000L, note = "SPOTIFY PREMIUM"),
        )

        val candidates = detect(transactions)

        assertThat(candidates).hasSize(1)
        val candidate = candidates.single()
        assertThat(candidate.name).isEqualTo("Spotify")
        assertThat(candidate.amountInCents).isEqualTo(260_000L)
        assertThat(candidate.repeatMonths).isEqualTo(1)
        assertThat(candidate.nextDueEpochMs)
            .isEqualTo(Instant.parse("2026-03-09T03:00:00Z").toEpochMilliseconds())
    }

    @Test
    fun `cycles outside the monthly window are ignored`() {
        val transactions = listOf(
            expense("2026-01-10T03:00:00Z", 260_000L, note = "Gym"),
            expense("2026-01-30T03:00:00Z", 260_000L, note = "Gym"),
        )

        assertThat(detect(transactions)).isEmpty()
    }

    @Test
    fun `expenses in different price buckets are not grouped together`() {
        val transactions = listOf(
            expense("2026-01-10T03:00:00Z", 200_000L, note = "Cinema"),
            expense("2026-02-09T03:00:00Z", 600_000L, note = "Cinema"),
        )

        assertThat(detect(transactions)).isEmpty()
    }

    @Test
    fun `expense already covered by an active subscription is not suggested again`() {
        val transactions = listOf(
            expense("2026-01-10T03:00:00Z", 260_000L, note = "SPOTIFY PREMIUM"),
            expense("2026-02-09T03:00:00Z", 260_000L, note = "SPOTIFY PREMIUM"),
        )

        assertThat(detect(transactions, activeSubscriptionNames = listOf("Spotify Premium"))).isEmpty()
    }

    @Test
    fun `blank note falls back to the localized category name`() {
        val transactions = listOf(
            expense("2026-01-10T03:00:00Z", 260_000L, note = "", category = Category.FOOD),
            expense("2026-02-09T03:00:00Z", 260_000L, note = "", category = Category.FOOD),
        )

        assertThat(detect(transactions).single().name).isEqualTo(Category.FOOD.displayName)
    }
}
