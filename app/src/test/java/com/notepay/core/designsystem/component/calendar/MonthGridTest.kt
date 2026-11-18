package com.notepay.core.designsystem.component.calendar

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.TestData
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import org.junit.Test

class MonthGridTest {

    @Test
    fun `october 2026 starts on thursday and fits in five weeks`() {
        val grid = buildMonthGrid(2026, 10, emptyMap())

        assertThat(grid.weeks).hasSize(5)
        assertThat(grid.weeks.first().map { it.date.day })
            .containsExactly(28, 29, 30, 1, 2, 3, 4)
            .inOrder()
        assertThat(grid.weeks.first().take(3).all { !it.inCurrentMonth }).isTrue()
        assertThat(grid.weeks.first()[3].date).isEqualTo(LocalDate(2026, 10, 1))
        assertThat(grid.weeks.last().last().date).isEqualTo(LocalDate(2026, 11, 1))
    }

    @Test
    fun `a month starting on monday still fits in five weeks`() {
        // 1/2/2027 là Thứ 2 nên lưới không có ô đệm đầu: 28 ngày nằm gọn trong 35 ô.
        val grid = buildMonthGrid(2027, 2, emptyMap())

        assertThat(grid.weeks).hasSize(5)
        assertThat(grid.weeks.first().first().date).isEqualTo(LocalDate(2027, 2, 1))
        assertThat(grid.weeks.first().all { it.inCurrentMonth }).isTrue()
    }

    @Test
    fun `a month that needs six rows produces 42 cells`() {
        val grid = buildMonthGrid(2027, 5, emptyMap())

        assertThat(grid.weeks).hasSize(6)
        assertThat(grid.weeks.sumOf { it.size }).isEqualTo(42)
        assertThat(grid.weeks.all { it.size == 7 }).isTrue()
    }

    @Test
    fun `daily totals and dots only apply to days inside the month`() {
        val days = mapOf(
            LocalDate(2026, 9, 30) to listOf(
                TestData.transaction(amount = Money(900_000_00), type = TransactionType.EXPENSE)
            ),
            LocalDate(2026, 10, 26) to listOf(
                TestData.transaction(amount = Money(300_000_00), type = TransactionType.EXPENSE)
            ),
        )

        val grid = buildMonthGrid(2026, 10, days)
        val previousMonthCell = grid.weeks.flatten().first { it.date == LocalDate(2026, 9, 30) }

        assertThat(previousMonthCell.date).isEqualTo(LocalDate(2026, 9, 30))
        assertThat(previousMonthCell.expense.amountInCents).isEqualTo(0L)
        assertThat(previousMonthCell.dotLevel).isEqualTo(0)
        assertThat(grid.totalExpense.amountInCents).isEqualTo(300_000_00L)
        assertThat(grid.activeDayCount).isEqualTo(1)
    }

    @Test
    fun `dot level scales with the busiest day of the month`() {
        val days = mapOf(
            LocalDate(2026, 10, 5) to listOf(TestData.transaction(amount = Money(100_000_00))),
            LocalDate(2026, 10, 6) to listOf(TestData.transaction(amount = Money(200_000_00))),
            LocalDate(2026, 10, 7) to listOf(TestData.transaction(amount = Money(300_000_00))),
        )

        val byDate = buildMonthGrid(2026, 10, days).weeks.flatten().associateBy { it.date }

        assertThat(byDate.getValue(LocalDate(2026, 10, 5)).dotLevel).isEqualTo(1)
        assertThat(byDate.getValue(LocalDate(2026, 10, 6)).dotLevel).isEqualTo(2)
        assertThat(byDate.getValue(LocalDate(2026, 10, 7)).dotLevel).isEqualTo(3)
        assertThat(byDate.getValue(LocalDate(2026, 10, 8)).dotLevel).isEqualTo(0)
    }

    @Test
    fun `a day with income only still shows a dot`() {
        val days = mapOf(
            LocalDate(2026, 10, 27) to listOf(
                TestData.transaction(amount = Money(500_000_00), type = TransactionType.INCOME)
            ),
        )

        val grid = buildMonthGrid(2026, 10, days)
        val cell = grid.weeks.flatten().first { it.date == LocalDate(2026, 10, 27) }

        assertThat(cell.income.amountInCents).isEqualTo(500_000_00L)
        assertThat(cell.expense.amountInCents).isEqualTo(0L)
        assertThat(cell.dotLevel).isAtLeast(1)
        assertThat(grid.totalIncome.amountInCents).isEqualTo(500_000_00L)
        assertThat(grid.totalExpense.amountInCents).isEqualTo(0L)
    }

    @Test
    fun `leap february 2028 has 29 days`() {
        val currentMonthDays = buildMonthGrid(2028, 2, emptyMap())
            .weeks
            .flatten()
            .filter { it.inCurrentMonth }

        assertThat(currentMonthDays).hasSize(29)
        assertThat(currentMonthDays.last().date).isEqualTo(LocalDate(2028, 2, 29))
    }
}
