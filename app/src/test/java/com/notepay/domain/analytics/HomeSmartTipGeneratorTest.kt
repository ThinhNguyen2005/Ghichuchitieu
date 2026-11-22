package com.notepay.domain.analytics

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import org.junit.Test
import kotlin.time.Instant

class HomeSmartTipGeneratorTest {

    private val sampleInstant = Instant.fromEpochMilliseconds(1_763_800_000_000L)

    private fun tx(
        id: Long,
        amountMajor: Long,
        type: TransactionType = TransactionType.EXPENSE,
        category: Category = Category.FOOD,
        isInternalTransfer: Boolean = false,
    ): Transaction = Transaction(
        id = id,
        amount = Money.fromMajorUnit(amountMajor),
        type = type,
        category = category,
        note = "Sample $id",
        occurredAt = sampleInstant,
        walletId = 1L,
        isInternalTransfer = isInternalTransfer,
    )

    @Test
    fun `generatePool ignores internal transfers when computing spending and savings tips`() {
        val transactions = listOf(
            tx(id = 1L, amountMajor = 90_000_000L, type = TransactionType.EXPENSE, category = Category.OTHER, isInternalTransfer = true),
            tx(id = 2L, amountMajor = 90_000_000L, type = TransactionType.INCOME, category = Category.INCOME_OTHER, isInternalTransfer = true),
            tx(id = 3L, amountMajor = 10_000_000L, type = TransactionType.INCOME, category = Category.SALARY),
            tx(id = 4L, amountMajor = 2_000_000L, type = TransactionType.EXPENSE, category = Category.FOOD),
        )
        val input = SmartTipInput(
            transactions = transactions,
            wallets = listOf(
                SmartTipWalletSnapshot("Tiền mặt", Money.fromMajorUnit(5_000_000L), Money.fromMajorUnit(4_000_000L)),
            ),
            streakDays = 1,
            dueRemindersCount = 0,
            epochDay = 20_414L,
        )

        val pool = HomeSmartTipGenerator.generatePool(input)
        val kinds = pool.map { it.kind }

        assertThat(kinds).contains(SmartTipKind.SAVINGS_RATE_HIGH)
        assertThat(kinds).doesNotContain(SmartTipKind.SPENDING_EXCEEDS_INCOME)

        val topCategoryTip = pool.first { it.kind == SmartTipKind.TOP_SPENDING_CATEGORY }
        assertThat(topCategoryTip.category).isEqualTo(Category.FOOD)
        assertThat(topCategoryTip.primaryMoney).isEqualTo(Money.fromMajorUnit(2_000_000L))
    }

    @Test
    fun `generatePool triggers contextual rules when thresholds are met`() {
        val transactions = listOf(
            tx(id = 1L, amountMajor = 1_000_000L, type = TransactionType.INCOME, category = Category.SALARY),
            tx(id = 2L, amountMajor = 1_200_000L, type = TransactionType.EXPENSE, category = Category.SHOPPING),
            tx(id = 3L, amountMajor = 50_000L, type = TransactionType.EXPENSE, category = Category.COFFEE),
            tx(id = 4L, amountMajor = 40_000L, type = TransactionType.EXPENSE, category = Category.COFFEE),
            tx(id = 5L, amountMajor = 30_000L, type = TransactionType.EXPENSE, category = Category.FOOD),
            tx(id = 6L, amountMajor = 20_000L, type = TransactionType.EXPENSE, category = Category.FOOD),
        )
        val wallets = listOf(
            SmartTipWalletSnapshot("Ví chính", Money.fromMajorUnit(9_000_000L), budgetLimit = null),
            SmartTipWalletSnapshot("Ví phụ", Money.fromMajorUnit(1_000_000L), budgetLimit = null),
        )
        val input = SmartTipInput(
            transactions = transactions,
            wallets = wallets,
            streakDays = 5,
            dueRemindersCount = 2,
            epochDay = 20_414L,
        )

        val pool = HomeSmartTipGenerator.generatePool(input)
        val kinds = pool.map { it.kind }

        assertThat(kinds).containsAtLeast(
            SmartTipKind.TOP_SPENDING_CATEGORY,
            SmartTipKind.SPENDING_EXCEEDS_INCOME,
            SmartTipKind.UPCOMING_SUBSCRIPTIONS,
            SmartTipKind.SMALL_EXPENSES_ADD_UP,
            SmartTipKind.SET_WALLET_BUDGET,
            SmartTipKind.STREAK_MOMENTUM,
            SmartTipKind.CONCENTRATED_WALLET,
        )
        assertThat(pool.size).isAtLeast(15)
    }

    @Test
    fun `selectTip rotates daily even with empty transactions`() {
        val dayOnePool = HomeSmartTipGenerator.generatePool(
            SmartTipInput(
                transactions = emptyList(),
                wallets = emptyList(),
                streakDays = 0,
                dueRemindersCount = 0,
                epochDay = 20_414L,
            )
        )
        val dayTwoPool = HomeSmartTipGenerator.generatePool(
            SmartTipInput(
                transactions = emptyList(),
                wallets = emptyList(),
                streakDays = 0,
                dueRemindersCount = 0,
                epochDay = 20_415L,
            )
        )

        val (tipDayOne, _) = HomeSmartTipGenerator.selectTip(dayOnePool, epochDay = 20_414L, rotationOffset = 0)
        val (tipDayTwo, _) = HomeSmartTipGenerator.selectTip(dayTwoPool, epochDay = 20_415L, rotationOffset = 0)

        assertThat(dayOnePool.size).isEqualTo(8)
        assertThat(dayOnePool.map { it.domain }.toSet().size).isAtLeast(4)
        assertThat(tipDayTwo.kind).isNotEqualTo(tipDayOne.kind)
    }

    @Test
    fun `selectTip manual rotation visits every item and wraps negative or large offsets`() {
        val pool = HomeSmartTipGenerator.generatePool(
            SmartTipInput(
                transactions = emptyList(),
                wallets = emptyList(),
                streakDays = 4,
                dueRemindersCount = 1,
                epochDay = 20_414L,
            )
        )

        val visitedIndices = (0 until pool.size).map { offset ->
            HomeSmartTipGenerator.selectTip(pool, epochDay = 20_414L, rotationOffset = offset).second
        }
        assertThat(visitedIndices.toSet()).hasSize(pool.size)

        val (_, negativeIndex) = HomeSmartTipGenerator.selectTip(pool, epochDay = 20_414L, rotationOffset = -3)
        val (_, largeIndex) = HomeSmartTipGenerator.selectTip(pool, epochDay = 20_414L, rotationOffset = 10_000)
        assertThat(negativeIndex).isIn(0 until pool.size)
        assertThat(largeIndex).isIn(0 until pool.size)
    }
}
