package com.notepay.data.repository

import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.mapper.TransactionMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.WalletStats
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.CategoryRepository
import com.notepay.core.platform.widget.WidgetUpdateHelper
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val dao: TransactionDao,
    private val mapper: TransactionMapper,
    private val database: NotePayDatabase,
    private val categoryRepository: CategoryRepository,
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : TransactionRepository {

    override fun observeAll(): Flow<List<Transaction>> =
        combine(dao.observeAll(), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)

    override fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<Transaction>> =
        combine(dao.observeByRange(startMillis, endMillis), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)

    override fun observeAllCreatedDates(): Flow<List<kotlin.time.Instant>> =
        dao.observeAllOccurredDates().map { milliList ->
            milliList.map { kotlin.time.Instant.fromEpochMilliseconds(it) }
        }.flowOn(dispatcher)

    override fun observeWalletStats(monthStartMillis: Long, monthEndMillis: Long): Flow<List<WalletStats>> =
        dao.observeWalletStats(monthStartMillis, monthEndMillis)
            .map { rows ->
                rows.map { row ->
                    WalletStats(
                        walletId = row.walletId,
                        allTimeIncome = Money(row.allTimeIncomeCents),
                        allTimeExpense = Money(row.allTimeExpenseCents),
                        currentMonthIncome = Money(row.monthIncomeCents),
                        currentMonthExpense = Money(row.monthExpenseCents),
                        txCount = row.txCount,
                    )
                }
            }
            .flowOn(dispatcher)

    override fun observeRecentNonTransfers(sinceMillis: Long): Flow<List<Transaction>> =
        combine(dao.observeRecentNonTransfers(sinceMillis), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)

    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> {
        val (start, end) = monthRange(year, month)
        return combine(dao.observeByRange(start, end), categoryRepository.observeCategories()) { list, _ ->
            list.map(mapper::toDomain)
        }.flowOn(dispatcher)
    }

    override fun observeByWalletAndMonth(walletId: Long, year: Int, month: Int): Flow<List<Transaction>> {
        val (start, end) = monthRange(year, month)
        return combine(dao.observeByWalletAndRange(walletId, start, end), categoryRepository.observeCategories()) { list, _ ->
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

    override suspend fun upsert(transaction: Transaction): Long {
        val id = database.withTransaction {
            require(!transaction.isInternalTransfer && transaction.transferGroupId == null) {
                "Transfers can only be created as a pair"
            }
            if (transaction.id != 0L) {
                val existing = dao.getById(transaction.id)
                require(existing?.isInternalTransfer != true && existing?.transferGroupId == null) {
                    "Cannot edit an internal transfer"
                }
            }
            dao.upsert(mapper.toEntity(transaction))
        }
        notifyWidgetsSafely()
        return id
    }

    override suspend fun upsertTransferPair(outgoing: Transaction, incoming: Transaction) =
        withContext(dispatcher) {
            require(outgoing.id == 0L && incoming.id == 0L) { "Transfers must be new transactions" }
            requireValidTransferPair(outgoing, incoming)
            database.withTransaction {
                require(dao.getByTransferGroupId(requireNotNull(outgoing.transferGroupId)).isEmpty()) {
                    "Transfer group ID already exists"
                }
                dao.upsert(mapper.toEntity(outgoing))
                dao.upsert(mapper.toEntity(incoming))
            }
            notifyWidgetsSafely()
        }

    override suspend fun delete(id: Long) {
        database.withTransaction {
            val transaction = dao.getById(id) ?: return@withTransaction
            if (transaction.isInternalTransfer || transaction.transferGroupId != null) {
                val groupId = requireNotNull(transaction.transferGroupId?.takeIf { it.isNotBlank() }) {
                    "Legacy transfers cannot be deleted without a verified pair"
                }
                val pair = dao.getByTransferGroupId(groupId)
                require(pair.size == 2) { "Incomplete transfer pair" }
                val outgoing = pair.singleOrNull { it.type == TransactionType.EXPENSE.name }
                val incoming = pair.singleOrNull { it.type == TransactionType.INCOME.name }
                require(outgoing != null && incoming != null) { "Invalid transfer pair" }
                requireValidTransferPair(mapper.toDomain(outgoing), mapper.toDomain(incoming))
                dao.deleteTransferGroup(groupId)
            } else {
                dao.delete(id)
            }
        }
        notifyWidgetsSafely()
    }

    private fun requireValidTransferPair(outgoing: Transaction, incoming: Transaction) {
        require(outgoing.isInternalTransfer && incoming.isInternalTransfer) { "Transfer flag missing" }
        require(outgoing.walletId > 0L && incoming.walletId > 0L) { "Transfer wallet IDs must be positive" }
        require(outgoing.amount.amountInCents > 0L) { "Transfer amount must be positive" }
        require(outgoing.type == TransactionType.EXPENSE && incoming.type == TransactionType.INCOME) {
            "Transfer types must be expense and income"
        }
        require(outgoing.walletId != incoming.walletId) { "Transfer wallets must differ" }
        require(outgoing.amount == incoming.amount) { "Transfer amounts must match" }
        require(outgoing.transferGroupId?.isNotBlank() == true && outgoing.transferGroupId == incoming.transferGroupId) {
            "Transfer group ID must be shared"
        }
    }

    private fun notifyWidgetsSafely() {
        try {
            WidgetUpdateHelper.notifyWidgetsDataChanged(context)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Database commit succeeded. Widget refresh must not make callers retry the transfer.
        }
    }

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
            val first = LocalDateTime(firstDate.year, firstDate.month.number, firstDate.day, 0, 0)
                .toInstant(zone)
            val lastExclusive = LocalDateTime(nextMonth.year, nextMonth.month.number, nextMonth.day, 0, 0)
                .toInstant(zone)
            return first.toEpochMilliseconds() to (lastExclusive.toEpochMilliseconds() - 1)
        }
    }
}
