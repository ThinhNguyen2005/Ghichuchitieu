package com.notepay.data.repository

import androidx.room.withTransaction
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.mapper.WalletMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.shared.SharedWalletPermissionDeniedException
import com.notepay.domain.shared.SharedWalletPermissionGuard
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val dao: WalletDao,
    private val transactionDao: TransactionDao,
    private val database: NotePayDatabase,
    private val mapper: WalletMapper,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) : WalletRepository {

    override fun observeAll(): Flow<List<Wallet>> =
        dao.observeAll().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeNonArchived(): Flow<List<Wallet>> =
        dao.observeNonArchived().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeActive(): Flow<Wallet?> =
        dao.observeActive().map { entity -> entity?.let(mapper::toDomain) }.flowOn(dispatcher)

    override suspend fun getById(id: Long): Wallet? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(wallet: Wallet): Long =
        dao.upsert(mapper.toEntity(wallet))

    override suspend fun delete(id: Long) {
        database.withTransaction {
            val wallet = dao.getById(id)?.let(mapper::toDomain)
            if (wallet != null && !SharedWalletPermissionGuard.canDeleteWallet(wallet)) {
                throw SharedWalletPermissionDeniedException("Only shared wallet OWNER can delete it")
            }
            require(!transactionDao.hasInternalTransfer(id)) {
                "Cannot delete a wallet containing internal transfer entries"
            }
            dao.delete(id)
        }
    }

    override suspend fun setArchived(id: Long, isArchived: Boolean) {
        database.withTransaction {
            if (isArchived) {
                val current = dao.getById(id)
                if (current?.isActive == true) {
                    dao.clearActive()
                    val fallback = dao.getAll().firstOrNull { it.id != id && !it.isArchived }
                    if (fallback != null) {
                        dao.setActive(fallback.id)
                    }
                }
            }
            dao.setArchived(id, isArchived)
        }
    }

    override suspend fun setActive(id: Long) = dao.setActiveExclusive(id)
}
