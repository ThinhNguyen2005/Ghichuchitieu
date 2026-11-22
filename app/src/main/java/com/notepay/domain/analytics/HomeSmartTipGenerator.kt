package com.notepay.domain.analytics

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlin.math.roundToInt

enum class SmartTipDomain {
    SPENDING,
    SAVINGS,
    BUDGET,
    HABIT,
    SUBSCRIPTION,
    WALLET,
}

enum class SmartTipKind {
    // Contextual data-driven tips
    TOP_SPENDING_CATEGORY,
    SAVINGS_RATE_HIGH,
    SPENDING_EXCEEDS_INCOME,
    UPCOMING_SUBSCRIPTIONS,
    SMALL_EXPENSES_ADD_UP,
    SET_WALLET_BUDGET,
    STREAK_MOMENTUM,
    CONCENTRATED_WALLET,

    // Evergreen financial coach tips
    EVERGREEN_RULE_50_30_20,
    EVERGREEN_EMERGENCY_FUND,
    EVERGREEN_WAIT_24H,
    EVERGREEN_PAY_YOURSELF_FIRST,
    EVERGREEN_AUDIT_SUBSCRIPTIONS,
    EVERGREEN_WEEKLY_REVIEW,
    EVERGREEN_SEPARATE_WALLETS,
    EVERGREEN_LOG_IMMEDIATELY,
}

@Immutable
data class SmartTipWalletSnapshot(
    val name: String,
    val balance: Money,
    val budgetLimit: Money?,
)

@Immutable
data class SmartTipInput(
    val transactions: List<Transaction>,
    val wallets: List<SmartTipWalletSnapshot>,
    val streakDays: Int,
    val dueRemindersCount: Int,
    val epochDay: Long,
)

@Immutable
data class SmartTipItem(
    val kind: SmartTipKind,
    val domain: SmartTipDomain,
    val category: Category? = null,
    val primaryInt: Int = 0,
    val secondaryText: String = "",
    val primaryMoney: Money = Money.ZERO,
)

/**
 * Builds a diverse, data-aware pool of smart financial tips and selects the active tip
 * deterministically per day + user rotation offset.
 */
object HomeSmartTipGenerator {

    private const val TOP_CATEGORY_SHARE_THRESHOLD = 0.25f
    private const val HIGH_SAVINGS_RATE_THRESHOLD = 0.25
    private const val SMALL_EXPENSE_THRESHOLD_CENTS = 100_000_00L
    private const val SMALL_EXPENSE_MIN_COUNT = 4
    private const val STREAK_MIN_DAYS = 3
    private const val CONCENTRATED_WALLET_SHARE_THRESHOLD = 0.80

    private val evergreenTemplates: List<SmartTipItem> = listOf(
        SmartTipItem(SmartTipKind.EVERGREEN_RULE_50_30_20, SmartTipDomain.BUDGET),
        SmartTipItem(SmartTipKind.EVERGREEN_EMERGENCY_FUND, SmartTipDomain.SAVINGS),
        SmartTipItem(SmartTipKind.EVERGREEN_WAIT_24H, SmartTipDomain.SPENDING),
        SmartTipItem(SmartTipKind.EVERGREEN_PAY_YOURSELF_FIRST, SmartTipDomain.SAVINGS),
        SmartTipItem(SmartTipKind.EVERGREEN_AUDIT_SUBSCRIPTIONS, SmartTipDomain.SUBSCRIPTION),
        SmartTipItem(SmartTipKind.EVERGREEN_WEEKLY_REVIEW, SmartTipDomain.HABIT),
        SmartTipItem(SmartTipKind.EVERGREEN_SEPARATE_WALLETS, SmartTipDomain.WALLET),
        SmartTipItem(SmartTipKind.EVERGREEN_LOG_IMMEDIATELY, SmartTipDomain.HABIT),
    )

    fun generatePool(input: SmartTipInput): List<SmartTipItem> {
        val contextual = buildContextualTips(input)
        val dayShift = Math.floorMod(input.epochDay.toInt(), evergreenTemplates.size)
        val rotatedEvergreens = List(evergreenTemplates.size) { index ->
            evergreenTemplates[(index + dayShift) % evergreenTemplates.size]
        }

        val combined = interleaveTips(contextual, rotatedEvergreens)
        return declumpDomains(combined)
    }

    fun selectTip(
        pool: List<SmartTipItem>,
        epochDay: Long,
        rotationOffset: Int,
    ): Pair<SmartTipItem, Int> {
        if (pool.isEmpty()) {
            return evergreenTemplates.first() to 0
        }
        val dayIndex = Math.floorMod(epochDay.toInt(), pool.size)
        val selectedIndex = Math.floorMod(dayIndex + rotationOffset, pool.size)
        return pool[selectedIndex] to selectedIndex
    }

    private fun buildContextualTips(input: SmartTipInput): List<SmartTipItem> {
        val tips = mutableListOf<SmartTipItem>()
        val nonTransfer = input.transactions.filterNot { it.isInternalTransfer }
        val expenses = nonTransfer.filter { it.type == TransactionType.EXPENSE }
        val incomes = nonTransfer.filter { it.type == TransactionType.INCOME }

        val totalExpenseCents = expenses.sumOf { it.amount.amountInCents }
        val totalIncomeCents = incomes.sumOf { it.amount.amountInCents }

        addTopCategoryTip(expenses, totalExpenseCents, tips)
        addCashflowRatioTip(totalIncomeCents, totalExpenseCents, tips)

        if (input.dueRemindersCount > 0) {
            tips.add(
                SmartTipItem(
                    kind = SmartTipKind.UPCOMING_SUBSCRIPTIONS,
                    domain = SmartTipDomain.SUBSCRIPTION,
                    primaryInt = input.dueRemindersCount,
                )
            )
        }

        val smallExpenses = expenses.filter { it.amount.amountInCents <= SMALL_EXPENSE_THRESHOLD_CENTS }
        if (smallExpenses.size >= SMALL_EXPENSE_MIN_COUNT) {
            tips.add(
                SmartTipItem(
                    kind = SmartTipKind.SMALL_EXPENSES_ADD_UP,
                    domain = SmartTipDomain.SPENDING,
                    primaryInt = smallExpenses.size,
                    primaryMoney = Money(smallExpenses.sumOf { it.amount.amountInCents }),
                )
            )
        }

        val hasAnyBudget = input.wallets.any { (it.budgetLimit?.amountInCents ?: 0L) > 0L }
        if (input.wallets.isNotEmpty() && !hasAnyBudget) {
            tips.add(
                SmartTipItem(
                    kind = SmartTipKind.SET_WALLET_BUDGET,
                    domain = SmartTipDomain.BUDGET,
                    secondaryText = input.wallets.first().name,
                )
            )
        }

        if (input.streakDays >= STREAK_MIN_DAYS) {
            tips.add(
                SmartTipItem(
                    kind = SmartTipKind.STREAK_MOMENTUM,
                    domain = SmartTipDomain.HABIT,
                    primaryInt = input.streakDays,
                )
            )
        }

        addConcentratedWalletTip(input.wallets, tips)
        return tips
    }

    private fun addTopCategoryTip(
        expenses: List<Transaction>,
        totalExpenseCents: Long,
        output: MutableList<SmartTipItem>,
    ) {
        if (totalExpenseCents <= 0L) return
        val topGroup = expenses
            .groupBy { it.category.id }
            .values
            .maxByOrNull { group -> group.sumOf { it.amount.amountInCents } }
            ?: return
        val topCategory = topGroup.first().category
        val topCents = topGroup.sumOf { it.amount.amountInCents }
        val share = topCents.toFloat() / totalExpenseCents.toFloat()
        if (share >= TOP_CATEGORY_SHARE_THRESHOLD) {
            output.add(
                SmartTipItem(
                    kind = SmartTipKind.TOP_SPENDING_CATEGORY,
                    domain = SmartTipDomain.SPENDING,
                    category = topCategory,
                    primaryInt = (share * 100f).roundToInt(),
                    primaryMoney = Money(topCents),
                )
            )
        }
    }

    private fun addCashflowRatioTip(
        totalIncomeCents: Long,
        totalExpenseCents: Long,
        output: MutableList<SmartTipItem>,
    ) {
        if (totalIncomeCents <= 0L) return
        if (totalExpenseCents > totalIncomeCents) {
            output.add(
                SmartTipItem(
                    kind = SmartTipKind.SPENDING_EXCEEDS_INCOME,
                    domain = SmartTipDomain.BUDGET,
                    primaryMoney = Money(totalExpenseCents - totalIncomeCents),
                )
            )
            return
        }
        val savingsCents = totalIncomeCents - totalExpenseCents
        val savingsRate = savingsCents.toDouble() / totalIncomeCents.toDouble()
        if (savingsRate >= HIGH_SAVINGS_RATE_THRESHOLD) {
            output.add(
                SmartTipItem(
                    kind = SmartTipKind.SAVINGS_RATE_HIGH,
                    domain = SmartTipDomain.SAVINGS,
                    primaryInt = (savingsRate * 100.0).roundToInt(),
                    primaryMoney = Money(savingsCents),
                )
            )
        }
    }

    private fun addConcentratedWalletTip(
        wallets: List<SmartTipWalletSnapshot>,
        output: MutableList<SmartTipItem>,
    ) {
        val positiveWallets = wallets.filter { it.balance.amountInCents > 0L }
        if (positiveWallets.size < 2) return
        val totalPositiveCents = positiveWallets.sumOf { it.balance.amountInCents }
        if (totalPositiveCents <= 0L) return
        val largest = positiveWallets.maxByOrNull { it.balance.amountInCents } ?: return
        val share = largest.balance.amountInCents.toDouble() / totalPositiveCents.toDouble()
        if (share >= CONCENTRATED_WALLET_SHARE_THRESHOLD) {
            output.add(
                SmartTipItem(
                    kind = SmartTipKind.CONCENTRATED_WALLET,
                    domain = SmartTipDomain.WALLET,
                    primaryInt = (share * 100.0).roundToInt(),
                    secondaryText = largest.name,
                )
            )
        }
    }

    private fun interleaveTips(
        contextual: List<SmartTipItem>,
        evergreen: List<SmartTipItem>,
    ): List<SmartTipItem> {
        if (contextual.isEmpty()) return evergreen
        val result = ArrayList<SmartTipItem>(contextual.size + evergreen.size)
        val maxLen = maxOf(contextual.size, evergreen.size)
        for (i in 0 until maxLen) {
            if (i < contextual.size) result.add(contextual[i])
            if (i < evergreen.size) result.add(evergreen[i])
        }
        return result
    }

    private fun declumpDomains(items: List<SmartTipItem>): List<SmartTipItem> {
        if (items.size <= 2) return items
        val remaining = items.toMutableList()
        val ordered = ArrayList<SmartTipItem>(items.size)
        while (remaining.isNotEmpty()) {
            val previousDomain = ordered.lastOrNull()?.domain
            val pickIndex = remaining.indexOfFirst { it.domain != previousDomain }
                .let { idx -> if (idx >= 0) idx else 0 }
            ordered.add(remaining.removeAt(pickIndex))
        }
        return ordered
    }
}
