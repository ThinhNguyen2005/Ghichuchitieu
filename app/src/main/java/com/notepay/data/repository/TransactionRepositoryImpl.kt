package com.notepay.data.repository

import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.mapper.TransactionMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Transaction
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.CategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    private val categoryRepository: CategoryRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        combine(dao.observeAll(), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val (start, end) = monthRange(year, month)
        return combine(dao.observeByRange(start, end), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)
    }

    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> =
        combine(dao.observeByWallet(walletId), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)

    override fun observeById(id: Long): Flow<Transaction?> =
        dao.observeById(id).map { it?.let(mapper::toDomain) }.flowOn(dispatcher)

    override suspend fun getById(id: Long): Transaction? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(transaction: Transaction): Long =
        dao.upsert(mapper.toEntity(transaction))

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun findRecentSimilar(
        noteKeyword: String,
        fromMillis: Long,
        toMillis: Long
    ): List<Transaction> =
        dao.findRecentSimilar(noteKeyword, fromMillis, toMillis).map(mapper::toDomain)

    companion object {
        /**
         * Trả về (startMillis, endMillis) của tháng [month] (1..12) năm [year],
         * bao trùm cả tháng đó theo local timezone.
         */
        fun monthRange(year: Int, month: Int): Pair<Long, Long> {
            val zone = TimeZone.currentSystemDefault()
            val firstDate = LocalDate(year, month, 1)
            val nextMonth = if (month == 12) LocalDate(year + 1, 1, 1) else LocalDate(year, month + 1, 1)
            val first = LocalDateTime(firstDate.year, firstDate.monthNumber, firstDate.dayOfMonth, 0, 0)
                .toInstant(zone)
            val lastExclusive = LocalDateTime(nextMonth.year, nextMonth.monthNumber, nextMonth.dayOfMonth, 0, 0)
                .toInstant(zone)
            return first.toEpochMilliseconds() to (lastExclusive.toEpochMilliseconds() - 1)
        }
    }
}
