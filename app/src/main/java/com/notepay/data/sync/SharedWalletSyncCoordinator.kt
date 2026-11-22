package com.notepay.data.sync

import android.content.Context
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.mapper.TransactionMapper
import com.notepay.data.remote.supabase.SupabaseClientProvider
import com.notepay.domain.model.SyncStatus
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.AuthRepository
import com.notepay.domain.shared.SharedWalletPermissionDeniedException
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.worker.SharedWalletSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.auth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinates permission checks, author metadata enrichment, outbox queueing,
 * and background WorkManager scheduling for transactions written into shared wallets.
 */
@Singleton
class SharedWalletSyncCoordinator @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val outboxProcessor: SyncOutboxProcessor,
    private val transactionDao: TransactionDao,
    private val transactionMapper: TransactionMapper,
    private val authRepository: AuthRepository,
    private val supabaseClientProvider: SupabaseClientProvider,
) {
    fun currentEffectiveUserId(): String? =
        supabaseClientProvider.client?.auth?.currentUserOrNull()?.id
            ?: authRepository.currentUser?.uid

    fun prepareTransactionForUpsert(
        wallet: Wallet,
        incoming: Transaction,
        existing: Transaction?,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Transaction {
        if (!wallet.isShared) return incoming

        val userId = currentEffectiveUserId()
        val allowed = if (incoming.id == 0L) {
            SharedWalletPermissionGuard.canAddTransaction(wallet)
        } else {
            SharedWalletPermissionGuard.canEditTransaction(
                wallet = wallet,
                transaction = existing ?: incoming,
                currentUserId = userId,
            )
        }
        if (!allowed) {
            throw SharedWalletPermissionDeniedException(
                "Permission denied: role=${wallet.sharedRole} cannot modify shared wallet ${wallet.id}",
            )
        }

        val displayName = incoming.createdByName
            ?: existing?.createdByName
            ?: authRepository.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: authRepository.currentUser?.email?.substringBefore('@')
        val operationId = existing?.operationId
            ?: incoming.operationId
            ?: UUID.randomUUID().toString()

        return incoming.copy(
            remoteId = existing?.remoteId ?: incoming.remoteId,
            operationId = operationId,
            syncStatus = SyncStatus.PENDING,
            createdByUserId = existing?.createdByUserId ?: incoming.createdByUserId ?: userId,
            createdByName = displayName,
            updatedAtEpochMs = maxOf(nowEpochMs, incoming.updatedAtEpochMs),
            deletedAtEpochMs = null,
        )
    }

    suspend fun onSharedTransactionSaved(wallet: Wallet, savedId: Long) {
        val remoteWalletId = wallet.remoteId?.takeIf { wallet.isShared && it.isNotBlank() } ?: return
        val entity = transactionDao.getById(savedId) ?: return
        outboxProcessor.enqueueTransactionUpsert(remoteWalletId, entity)
        SharedWalletSyncWorker.enqueueOneTime(context)
    }

    suspend fun deleteSharedTransaction(
        wallet: Wallet,
        txId: Long,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Boolean {
        if (!wallet.isShared) return false
        val entity = transactionDao.getById(txId) ?: return true
        val domainTx = transactionMapper.toDomain(entity)
        val userId = currentEffectiveUserId()
        if (!SharedWalletPermissionGuard.canDeleteTransaction(wallet, domainTx, userId)) {
            throw SharedWalletPermissionDeniedException(
                "Permission denied: role=${wallet.sharedRole} cannot delete transaction $txId",
            )
        }
        transactionDao.markSoftDeleted(
            id = txId,
            deletedAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
            syncStatus = SyncStatus.PENDING.name,
        )
        val updatedEntity = transactionDao.getById(txId)
        val remoteWalletId = wallet.remoteId?.takeIf { it.isNotBlank() }
        if (updatedEntity != null && remoteWalletId != null) {
            outboxProcessor.enqueueTransactionSoftDelete(remoteWalletId, updatedEntity, nowEpochMs)
            SharedWalletSyncWorker.enqueueOneTime(context)
        }
        return true
    }
}
