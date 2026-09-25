package com.notepay.domain.usecase.debt

import com.notepay.domain.model.Money
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import javax.inject.Inject

data class DebtSummary(
    val totalToCollect: Money,     // Tổng số tiền người khác đang nợ mình (LEND)
    val totalToPay: Money,         // Tổng số tiền mình đang nợ người khác (BORROW)
    val netBalance: Money,         // Tài sản ròng từ công nợ (totalToCollect - totalToPay)
    val activeDebtsCount: Int,     // Số khoản nợ chưa tất toán
    val overdueCount: Int,         // Số khoản đã quá hạn
    val dueTodayCount: Int,        // Số khoản đến hạn hôm nay
)

class GetDebtSummaryUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    operator fun invoke(): Flow<DebtSummary> {
        return debtRepository.observeAll().map { debts ->
            calculateSummary(debts)
        }
    }

    fun calculateSummary(debts: List<DebtWithHistory>): DebtSummary {
        val now = Clock.System.now()
        var collectCents = 0L
        var payCents = 0L
        var activeCount = 0
        var overdueCount = 0
        var dueTodayCount = 0

        for (item in debts) {
            if (!item.isFullyPaid) {
                activeCount++
                val remaining = item.remainingAmount.amountInCents
                if (item.debt.type == DebtType.LEND) {
                    collectCents += remaining
                } else {
                    payCents += remaining
                }

                if (item.isOverdue(now)) {
                    overdueCount++
                } else if (item.isDueToday(now)) {
                    dueTodayCount++
                }
            }
        }

        val totalToCollect = Money(collectCents)
        val totalToPay = Money(payCents)
        val netBalance = Money(collectCents - payCents)

        return DebtSummary(
            totalToCollect = totalToCollect,
            totalToPay = totalToPay,
            netBalance = netBalance,
            activeDebtsCount = activeCount,
            overdueCount = overdueCount,
            dueTodayCount = dueTodayCount,
        )
    }
}
