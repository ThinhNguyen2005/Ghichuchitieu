package com.notepay.data.repository

import com.notepay.data.local.dao.DebtDao
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.mapper.DebtMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.repository.DebtRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DebtRepositoryImpl @Inject constructor(
    private val dao: DebtDao,
    private val transactionDao: TransactionDao,
    private val mapper: DebtMapper,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DebtRepository {

    override fun observeAll(): Flow<List<DebtWithHistory>> =
        dao.observeAllWithPayments()
            .map { list -> list.map(mapper::toDomainWithHistory) }
            .flowOn(ioDispatcher)

    override fun observeByType(type: DebtType): Flow<List<DebtWithHistory>> =
        dao.observeByType(type.name)
            .map { list -> list.map(mapper::toDomainWithHistory) }
            .flowOn(ioDispatcher)

    override fun observeById(id: Long): Flow<DebtWithHistory?> =
        dao.observeWithPaymentsById(id)
            .map { it?.let(mapper::toDomainWithHistory) }
            .flowOn(ioDispatcher)

    override suspend fun getById(id: Long): DebtWithHistory? = withContext(ioDispatcher) {
        dao.getWithPaymentsById(id)?.let(mapper::toDomainWithHistory)
    }

    override suspend fun upsertDebt(debt: Debt): Long = withContext(ioDispatcher) {
        val entity = mapper.toEntityDebt(debt)
        if (entity.id == 0L) {
            dao.insertDebt(entity)
        } else {
            dao.updateDebt(entity)
            entity.id
        }
    }

    override suspend fun deleteDebt(id: Long) = withContext(ioDispatcher) {
        // Dọn dẹp toàn bộ các giao dịch ví liên kết với các khoản trả nợ của nợ này
        val payments = dao.getPaymentsByDebtId(id)
        payments.forEach { payment ->
            payment.transactionId?.let { txId ->
                transactionDao.delete(txId)
            }
        }
        dao.deleteDebtById(id)
    }

    override suspend fun recordPayment(payment: DebtPayment): Long = withContext(ioDispatcher) {
        val entity = mapper.toEntityPayment(payment)
        val paymentId = dao.insertPayment(entity)

        // Sau khi thêm khoản trả nợ, tự động kiểm tra xem tổng trả đã >= số tiền ban đầu chưa
        val debtWithPayments = dao.getWithPaymentsById(payment.debtId)
        if (debtWithPayments != null) {
            val totalPaidCents = debtWithPayments.payments.sumOf { it.amountCents }
            val originalCents = debtWithPayments.debt.originalAmountCents
            if (totalPaidCents >= originalCents && !debtWithPayments.debt.isSettled) {
                dao.updateDebt(debtWithPayments.debt.copy(isSettled = true))
            }
        }
        paymentId
    }

    override suspend fun deletePayment(id: Long) = withContext(ioDispatcher) {
        val payment = dao.getPaymentById(id) ?: return@withContext
        // 1. Nếu có giao dịch ví tương ứng, xóa giao dịch ví để hoàn tiền chính xác
        payment.transactionId?.let { txId ->
            transactionDao.delete(txId)
        }
        // 2. Xóa bản ghi thanh toán
        dao.deletePaymentById(id)

        // 3. Tự động kiểm tra lại tổng tiền đã trả: nếu bị hụt dưới nợ gốc, bỏ cờ isSettled
        val debtWithPayments = dao.getWithPaymentsById(payment.debtId)
        if (debtWithPayments != null) {
            val totalPaidCents = debtWithPayments.payments.sumOf { it.amountCents }
            val originalCents = debtWithPayments.debt.originalAmountCents
            if (totalPaidCents < originalCents && debtWithPayments.debt.isSettled) {
                dao.updateDebt(debtWithPayments.debt.copy(isSettled = false))
            }
        }
    }

    override suspend fun markSettled(debtId: Long, isSettled: Boolean) = withContext(ioDispatcher) {
        val debt = dao.getById(debtId) ?: return@withContext
        dao.updateDebt(debt.copy(isSettled = isSettled))
    }

    override suspend fun getDebtsDueInRange(startMillis: Long, endMillis: Long): List<Debt> =
        withContext(ioDispatcher) {
            dao.getDebtsDueInRange(startMillis, endMillis).map(mapper::toDomainDebt)
        }
}
