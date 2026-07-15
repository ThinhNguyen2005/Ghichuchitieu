package com.notepay.ui.feature.stats

import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Subscription
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.util.MoneyFormatter
import kotlin.math.max
import kotlin.time.Instant

/**
 * Pure-Kotlin engine cho Stats insights. Không có Android dependency.
 * Nhận data thô, trả về data class UI-ready.
 */
object StatsInsightsEngine {

    // ── Forecast ──────────────────────────────────────────────────────

    fun computeForecast(
        currentMonthExpense: Money,
        currentDay: Int,
        daysInMonth: Int,
        limit: Money?,
        previousMonthDailyAvg: Money? = null,
    ): BudgetForecast {
        // Weighted moving average: ngày gần nhất nặng hơn (linear weight)
        val weightSum = currentDay * (currentDay + 1L) / 2.0
        // avg = sum(day_i * spend_i) / weightSum
        // With uniform spend assumption: avg ≈ currentExpense / currentDay * (1 + currentDay) / (2 * currentDay)
        // Simplified: linear-weighted avg ≈ currentExpense * 2 / (currentDay + 1)
        val dailyAverageCents = if (currentDay > 0) {
            (currentMonthExpense.amountInCents.toDouble() * 2) / (currentDay + 1)
        } else 0.0
        val projectedSpendCents = dailyAverageCents * daysInMonth

        val dailyAvgStr = MoneyFormatter.format(Money(dailyAverageCents.toLong()))
        val projectedSpendStr = MoneyFormatter.format(Money(projectedSpendCents.toLong()))

        val baseMessage = "Với tốc độ hiện tại ($dailyAvgStr/ngày), cuối tháng dự kiến chi $projectedSpendStr"

        val isProjectedToExceed = limit != null && projectedSpendCents > limit.amountInCents
        val exceedMsg = if (isProjectedToExceed) {
            " ⚠️ Vượt hạn mức ${MoneyFormatter.format(limit)}!"
        } else ""

        // Trend so với tháng trước
        val trendPercent: Float? = if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0) {
            val prev = previousMonthDailyAvg.amountInCents.toDouble()
            ((dailyAverageCents - prev) / prev * 100).toFloat()
        } else null

        val trendMessage: String? = when {
            trendPercent == null -> null
            trendPercent > 5f -> "📈 Chi tiêu tăng %.0f%% so tháng trước".format(trendPercent)
            trendPercent < -5f -> "📉 Chi tiêu giảm %.0f%% so tháng trước".format(-trendPercent)
            else -> "➡️ Chi tiêu ổn định so tháng trước"
        }

        val prevAvgStr = if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0) {
            MoneyFormatter.format(previousMonthDailyAvg)
        } else null

        val fullMessage = buildString {
            append(baseMessage)
            append(exceedMsg)
            if (prevAvgStr != null) {
                append("\nTháng trước: $prevAvgStr/ngày")
            }
        }

        return BudgetForecast(
            dailyAverage = Money(dailyAverageCents.toLong()),
            projectedSpend = Money(projectedSpendCents.toLong()),
            forecastMessage = fullMessage,
            isProjectedToExceed = isProjectedToExceed,
            previousMonthDailyAverage = previousMonthDailyAvg,
            trendPercent = trendPercent,
            trendMessage = trendMessage,
        )
    }

    /**
     * Tính daily average của tháng trước từ danh sách transactions.
     */
    fun computePreviousMonthDailyAvg(
        allTransactions: List<Transaction>,
        previousMonthStartMillis: Long,
        previousMonthEndMillis: Long,
        previousDaysInMonth: Int,
    ): Money {
        val prevExpenses = allTransactions.filter {
            it.type == TransactionType.EXPENSE &&
                it.occurredAt.toEpochMilliseconds() in previousMonthStartMillis..previousMonthEndMillis
        }
        val totalPrevExpense = prevExpenses.fold(Money.ZERO) { acc, t -> acc + t.amount }
        return if (previousDaysInMonth > 0) {
            Money(totalPrevExpense.amountInCents / previousDaysInMonth)
        } else Money.ZERO
    }

    // ── Dynamic Daily Budget ──────────────────────────────────────────

    fun computeDynamicDailyBudget(
        limit: Money,
        spentExceptToday: Money,
        spentToday: Money,
        remainingDays: Int,
        daysInMonth: Int,
        currentDay: Int,
        currentMonthExpense: Money,
    ): DynamicDailyBudgetData {
        val remainingBudget = max(0L, limit.amountInCents - spentExceptToday.amountInCents)
        val dailyBudgetVal = if (remainingDays > 0) remainingBudget / remainingDays else 0L
        val remainingToday = max(0L, dailyBudgetVal - spentToday.amountInCents)

        val tomorrowBudget = if (remainingDays > 1) {
            val remainingForTomorrow = max(0L, limit.amountInCents - currentMonthExpense.amountInCents)
            remainingForTomorrow / (remainingDays - 1)
        } else {
            0L
        }

        val initialDailyBudget = limit.amountInCents / daysInMonth
        val spentTodayLong = spentToday.amountInCents

        // Early warning: chi nhanh đầu tháng
        val earlyWarning: String? = when {
            currentDay <= 3 && spentTodayLong > initialDailyBudget * 2L -> {
                "⚠️ Đã tiêu ${MoneyFormatter.format(spentToday)} trong 3 ngày đầu — gấp đôi hạn mức ngày. Hãy kiềm chế!"
            }
            currentDay <= 7 && spentTodayLong > initialDailyBudget * 15L / 10 -> {
                val ratio = spentTodayLong * 100 / initialDailyBudget
                "⚠️ Tuần đầu chi tiêu đạt $ratio%% hạn mức/ngày. Nên giảm xuống còn ${MoneyFormatter.format(Money(initialDailyBudget * 8 / 10))}/ngày."
            }
            else -> null
        }

        return DynamicDailyBudgetData(
            dailyBudget = Money(dailyBudgetVal),
            spentToday = spentToday,
            remainingToday = Money(remainingToday),
            tomorrowBudget = Money(tomorrowBudget),
            isExceeded = spentTodayLong > dailyBudgetVal,
            earlyWarning = earlyWarning,
        )
    }

    // ── AI Advices ────────────────────────────────────────────────────

    fun computeAdvices(
        breakdown: List<CategoryBreakdownItem>,
        expense: Money,
        income: Money,
        subscriptions: List<Subscription>,
        currentMonthExpense: Money,
        spentToday: Money,
        currentDay: Int,
        daysInMonth: Int,
        remainingDays: Int,
        limit: Money?,
        previousMonthDailyAvg: Money?,
        feedbacks: Map<String, Int>,
        nowInstant: Instant,
    ): List<AiAdviceItem> {
        val advices = mutableListOf<AiAdviceItem>()

        // Rule A: FOOD > 35% (giữ nguyên — hợp lý)
        val foodBreakdown = breakdown.find { it.category == Category.FOOD }
        if (foodBreakdown != null && foodBreakdown.percentage > 0.35f && (feedbacks["advice_food"] ?: 0) == 0) {
            val percentStr = "%.1f%%".format(foodBreakdown.percentage * 100)
            advices.add(
                AiAdviceItem(
                    id = "advice_food",
                    type = "warning",
                    title = "Cảnh báo ăn uống",
                    content = "Chi tiêu ăn uống chiếm $percentStr tổng chi tiêu. Hãy thử tự nấu ăn để tiết kiệm!",
                    categoryId = Category.FOOD.id,
                    feedback = feedbacks["advice_food"] ?: 0,
                )
            )
        }

        // Rule B: Phí sắp đến hạn & Số dư không đủ (giữ nguyên)
        val upcomingSubs = subscriptions.filter { sub ->
            sub.isActive && (sub.nextDueDate - nowInstant).inWholeDays in 0..3
        }
        if (upcomingSubs.isNotEmpty()) {
            val balanceVal = income.amountInCents - expense.amountInCents
            for (sub in upcomingSubs) {
                val feedbackKey = "advice_bill_balance_${sub.id}"
                if (balanceVal < sub.amount.amountInCents && (feedbacks[feedbackKey] ?: 0) == 0) {
                    advices.add(
                        AiAdviceItem(
                            id = feedbackKey,
                            type = "warning",
                            title = "Chuẩn bị tiền đóng phí",
                            content = "Hóa đơn '${sub.name}' (${MoneyFormatter.format(sub.amount)}) sẽ đến hạn sau vài ngày. Số dư ví hiện không đủ!",
                            feedback = feedbacks[feedbackKey] ?: 0,
                        )
                    )
                }
            }
        }

        // Rule C: Chi tiêu hôm nay spike > 2x daily avg
        if (remainingDays >= 3 && currentDay > 1 && (feedbacks["advice_spike"] ?: 0) == 0) {
            val dailyAvg = currentMonthExpense.amountInCents / currentDay
            if (dailyAvg > 0 && spentToday.amountInCents > dailyAvg * 2) {
                val spikeRatio = spentToday.amountInCents * 100 / dailyAvg
                advices.add(
                    AiAdviceItem(
                        id = "advice_spike",
                        type = "warning",
                        title = "Chi tiêu bất thường",
                        content = "Hôm nay chi ${MoneyFormatter.format(spentToday)} — gấp $spikeRatio%% trung bình ngày (${MoneyFormatter.format(Money(dailyAvg))}). Kiểm tra lại!",
                        feedback = feedbacks["advice_spike"] ?: 0,
                    )
                )
            }
        }

        // Rule D: Trend so tháng trước
        if (previousMonthDailyAvg != null && previousMonthDailyAvg.amountInCents > 0 && currentDay > 3
            && (feedbacks["advice_trend"] ?: 0) == 0 && limit != null && limit.amountInCents > 0
        ) {
            val currentDailyAvg = currentMonthExpense.amountInCents / currentDay
            val prevAvg = previousMonthDailyAvg.amountInCents
            val changePercent = ((currentDailyAvg - prevAvg).toFloat() / prevAvg * 100)

            if (changePercent > 20f) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_trend",
                        type = "warning",
                        title = "Chi tiêu tăng nhanh",
                        content = "Trung bình ngày tháng này cao hơn %.0f%% so tháng trước. Hãy kiểm soát trước khi quá muộn!".format(changePercent),
                        feedback = feedbacks["advice_trend"] ?: 0,
                    )
                )
            } else if (changePercent < -20f) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_trend",
                        type = "success",
                        title = "Tiết kiệm tốt",
                        content = "Chi tiêu trung bình ngày giảm %.0f%% so tháng trước. Hãy tiếp tục duy trì!".format(-changePercent),
                        feedback = feedbacks["advice_trend"] ?: 0,
                    )
                )
            }
        }

        // Rule E: Đang tiết kiệm — nếu daily avg < 80% hạn mức an toàn
        if (limit != null && limit.amountInCents > 0 && remainingDays >= 5 && currentDay > 1
            && (feedbacks["advice_saving"] ?: 0) == 0
        ) {
            val initialDailyBudget = limit.amountInCents / daysInMonth
            val currentDailyAverage = (currentMonthExpense.amountInCents - spentToday.amountInCents) / (currentDay - 1)
            if (currentDailyAverage in 1 until (initialDailyBudget * 8 / 10)) {
                advices.add(
                    AiAdviceItem(
                        id = "advice_saving",
                        type = "success",
                        title = "Đang tiết kiệm tốt",
                        content = "Chi tiêu trung bình chỉ ${currentDailyAverage * 100 / initialDailyBudget}% hạn mức/ngày. Tuyệt vời!",
                        feedback = feedbacks["advice_saving"] ?: 0,
                    )
                )
            }
        }

        return advices
    }
}
