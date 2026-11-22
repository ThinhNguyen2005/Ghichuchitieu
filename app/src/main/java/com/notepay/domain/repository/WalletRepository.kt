package com.notepay.domain.repository

import com.notepay.domain.model.Wallet
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    fun observeAll(): Flow<List<Wallet>>
    fun observeNonArchived(): Flow<List<Wallet>>
    fun observeActive(): Flow<Wallet?>
    suspend fun getById(id: Long): Wallet?
    suspend fun upsert(wallet: Wallet): Long
    suspend fun delete(id: Long)
    suspend fun setArchived(id: Long, isArchived: Boolean)
    suspend fun setActive(id: Long)
}
