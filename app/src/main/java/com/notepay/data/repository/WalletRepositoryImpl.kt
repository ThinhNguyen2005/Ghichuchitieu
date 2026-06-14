package com.notepay.data.repository

import com.notepay.data.local.dao.WalletDao
import com.notepay.data.mapper.WalletMapper
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletRepositoryImpl @Inject constructor(
    private val dao: WalletDao,
    private val mapper: WalletMapper,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) : WalletRepository {

    override fun observeAll(): Flow<List<Wallet>> =
        dao.observeAll().map { list -> list.map(mapper::toDomain) }.flowOn(dispatcher)

    override fun observeActive(): Flow<Wallet?> =
        dao.observeActive().map { entity -> entity?.let(mapper::toDomain) }.flowOn(dispatcher)

    override suspend fun getById(id: Long): Wallet? =
        dao.getById(id)?.let(mapper::toDomain)

    override suspend fun upsert(wallet: Wallet): Long =
        dao.upsert(mapper.toEntity(wallet))

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun setActive(id: Long) = dao.setActiveExclusive(id)
}
