package com.notepay.domain.notification

import com.notepay.domain.model.Money

sealed interface NotificationCase {
    data class Expense(
        val amount: Money,
        val note: String,
        val categoryName: String,
        val categoryEmoji: String
    ) : NotificationCase

    data class Income(
        val amount: Money,
        val note: String,
        val walletName: String
    ) : NotificationCase

    data class DebtSingle(
        val debtorName: String,
        val paidAmount: Money,
        val remainingDebt: Money,
        val memoCode: String
    ) : NotificationCase

    data class DebtBulk(
        val debtorName: String,
        val totalAmount: Money,
        val billCount: Int
    ) : NotificationCase

    data class BudgetAlert(
        val spentAmount: Money,
        val budgetAmount: Money,
        val percentUsed: Int
    ) : NotificationCase

    data class SubscriptionDetected(
        val name: String,
        val amount: Money,
        val lastOccurredDaysAgo: Int
    ) : NotificationCase

    object InternalTransfer : NotificationCase
}

object NotificationClassifier {
    fun getCategoryEmoji(categoryId: String): String = when (categoryId) {
        "FOOD" -> "🍔"
        "COFFEE" -> "☕"
        "ENTERTAINMENT" -> "🎬"
        "SPORTS" -> "⚽"
        "SHOPPING" -> "🛍️"
        "CLOTHES" -> "👕"
        "BEAUTY" -> "💅"
        "TRANSPORT" -> "🚌"
        "GAS" -> "⛽"
        "REPAIR" -> "🔧"
        "HOME" -> "🏠"
        "BILL" -> "💵"
        "ELECTRICITY" -> "💡"
        "WATER" -> "💧"
        "INTERNET" -> "🌐"
        "HEALTH" -> "🏥"
        "EDUCATION" -> "🎓"
        "FAMILY" -> "👪"
        "CHILDREN" -> "👶"
        "PETS" -> "🐶"
        "TRAVEL" -> "✈️"
        "GIFT" -> "🎁"
        "CHARITY" -> "❤️"
        "SALARY" -> "💵"
        "BONUS" -> "🎁"
        "INVESTMENT" -> "📈"
        "SAVINGS" -> "🐷"
        "DEBT_LOAN" -> "🏦"
        "INSURANCE" -> "🛡️"
        "TAX" -> "📄"
        else -> "🛍️"
    }
}
