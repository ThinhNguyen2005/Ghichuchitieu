package com.notepay.domain.repository

import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import kotlinx.coroutines.flow.Flow

interface DebtRepository {
    fun observeAll(): Flow<List<DebtWithHistory>>
    fun observeByType(type: DebtType): Flow<List<DebtWithHistory>>
    fun observeById(id: Long): Flow<DebtWithHistory?>
    suspend fun getById(id: Long): DebtWithHistory?
    suspend fun upsertDebt(debt: Debt): Long
    suspend fun deleteDebt(id: Long)
    suspend fun recordPayment(payment: DebtPayment): Long
    suspend fun deletePayment(id: Long)
    suspend fun markSettled(debtId: Long, isSettled: Boolean)
    suspend fun getDebtsDueInRange(startMillis: Long, endMillis: Long): List<Debt>
}
