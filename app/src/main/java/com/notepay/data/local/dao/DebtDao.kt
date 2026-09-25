package com.notepay.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.notepay.data.local.entity.DebtEntity
import com.notepay.data.local.entity.DebtPaymentEntity
import com.notepay.data.local.entity.DebtWithPayments
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {

    @Transaction
    @Query("SELECT * FROM debts ORDER BY is_settled ASC, created_at DESC")
    fun observeAllWithPayments(): Flow<List<DebtWithPayments>>

    @Transaction
    @Query("SELECT * FROM debts WHERE type = :type ORDER BY is_settled ASC, created_at DESC")
    fun observeByType(type: String): Flow<List<DebtWithPayments>>

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    fun observeWithPaymentsById(id: Long): Flow<DebtWithPayments?>

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getWithPaymentsById(id: Long): DebtWithPayments?

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: DebtPaymentEntity): Long

    @Delete
    suspend fun deletePayment(payment: DebtPaymentEntity)

    @Query("DELETE FROM debt_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("SELECT * FROM debt_payments WHERE debt_id = :debtId ORDER BY paid_at DESC")
    suspend fun getPaymentsByDebtId(debtId: Long): List<DebtPaymentEntity>

    @Transaction
    @Query("SELECT * FROM debts WHERE is_settled = 0 AND due_date IS NOT NULL AND due_date BETWEEN :startMillis AND :endMillis")
    suspend fun getDebtsDueInRange(startMillis: Long, endMillis: Long): List<DebtEntity>
}
