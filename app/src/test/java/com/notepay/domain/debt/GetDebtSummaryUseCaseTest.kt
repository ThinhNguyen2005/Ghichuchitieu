package com.notepay.domain.debt

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.repository.DebtRepository
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalCoroutinesApi::class)
class GetDebtSummaryUseCaseTest {

    private val debtRepo = mockk<DebtRepository>()
    private val useCase = GetDebtSummaryUseCase(debtRepo)

    @Test
    fun `calculateSummary computes totals, net balance and active count correctly`() {
        val now = Clock.System.now()

        // Debt 1: Lend 1,000,000, paid 200,000 -> remaining 800,000
        val debt1 = Debt(
            id = 1L,
            personName = "Person A",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(1_000_000_00L),
            walletId = null,
            createdAt = now - 5.days,
            dueDate = now + 5.days,
            note = "",
            isSettled = false,
        )
        val history1 = DebtWithHistory(
            debt = debt1,
            payments = listOf(
                DebtPayment(1L, 1L, Money(200_000_00L), null, now - 2.days, "")
            )
        )

        // Debt 2: Borrow 500,000, paid 100,000 -> remaining 400,000 (Overdue: due 2 days ago)
        val debt2 = Debt(
            id = 2L,
            personName = "Person B",
            phoneNumber = null,
            type = DebtType.BORROW,
            originalAmount = Money(500_000_00L),
            walletId = null,
            createdAt = now - 10.days,
            dueDate = now - 2.days,
            note = "",
            isSettled = false,
        )
        val history2 = DebtWithHistory(
            debt = debt2,
            payments = listOf(
                DebtPayment(2L, 2L, Money(100_000_00L), null, now - 5.days, "")
            )
        )

        // Debt 3: Lend 300,000, fully paid -> settled (should be ignored in active counts and totals)
        val debt3 = Debt(
            id = 3L,
            personName = "Person C",
            phoneNumber = null,
            type = DebtType.LEND,
            originalAmount = Money(300_000_00L),
            walletId = null,
            createdAt = now - 7.days,
            dueDate = now - 1.days,
            note = "",
            isSettled = true,
        )
        val history3 = DebtWithHistory(
            debt = debt3,
            payments = listOf(
                DebtPayment(3L, 3L, Money(300_000_00L), null, now - 1.days, "")
            )
        )

        val summary = useCase.calculateSummary(listOf(history1, history2, history3))

        assertThat(summary.totalToCollect).isEqualTo(Money(800_000_00L))
        assertThat(summary.totalToPay).isEqualTo(Money(400_000_00L))
        assertThat(summary.netBalance).isEqualTo(Money(400_000_00L))
        assertThat(summary.activeDebtsCount).isEqualTo(2)
        assertThat(summary.overdueCount).isEqualTo(1)
        assertThat(summary.dueTodayCount).isEqualTo(0)
    }

    @Test
    fun `calculateSummary returns zero for empty debt list`() {
        val summary = useCase.calculateSummary(emptyList())

        assertThat(summary.totalToCollect).isEqualTo(Money.ZERO)
        assertThat(summary.totalToPay).isEqualTo(Money.ZERO)
        assertThat(summary.netBalance).isEqualTo(Money.ZERO)
        assertThat(summary.activeDebtsCount).isEqualTo(0)
        assertThat(summary.overdueCount).isEqualTo(0)
        assertThat(summary.dueTodayCount).isEqualTo(0)
    }
}
