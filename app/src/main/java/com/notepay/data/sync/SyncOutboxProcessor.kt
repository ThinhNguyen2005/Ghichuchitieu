package com.notepay.data.sync

import com.notepay.data.local.dao.SyncOutboxDao
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.entity.SyncOutboxEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.remote.supabase.SharedTransactionRemoteGateway
import com.notepay.data.remote.supabase.dto.OutboxTransactionPayload
import com.notepay.data.remote.supabase.dto.RemoteSharedTransactionDto
import com.notepay.domain.model.Category
import com.notepay.domain.model.SyncStatus
import com.notepay.domain.model.TransactionType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

data class SyncProcessResult(
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
)

enum class ConflictResolutionOutcome {
    INSERTED,
    UPDATED_FROM_REMOTE,
    KEPT_LOCAL_NEWER,
    SOFT_DELETED,
}

/**
 * Offline-first outbox engine with idempotent `operation_id` deduplication
 * and Last-Write-Wins (LWW) timestamp conflict resolution.
 */
@Singleton
class SyncOutboxProcessor internal constructor(
    private val outboxDao: SyncOutboxDao,
    private val transactionDao: TransactionDao,
    private val remoteGateway: SharedTransactionRemoteGateway,
    private val json: Json,
    private val nowEpochMs: () -> Long,
) {
    @Inject
    constructor(
        outboxDao: SyncOutboxDao,
        transactionDao: TransactionDao,
        remoteGateway: SharedTransactionRemoteGateway,
        json: Json,
    ) : this(
        outboxDao = outboxDao,
        transactionDao = transactionDao,
        remoteGateway = remoteGateway,
        json = json,
        nowEpochMs = { System.currentTimeMillis() },
    )

    suspend fun enqueueTransactionUpsert(
        walletRemoteId: String,
        transaction: TransactionEntity,
    ): String {
        val opId = transaction.operationId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val now = nowEpochMs()
        val effectiveUpdatedAt = transaction.updatedAtEpochMs.takeIf { it > 0L } ?: now
        val payload = OutboxTransactionPayload(
            operationId = opId,
            remoteId = transaction.remoteId,
            localTransactionId = transaction.id,
            walletRemoteId = walletRemoteId,
            type = transaction.type,
            amountMinor = transaction.amountCents,
            categoryId = transaction.category,
            note = transaction.note,
            occurredAtEpochMs = transaction.occurredAt,
            updatedAtEpochMs = effectiveUpdatedAt,
            deletedAtEpochMs = null,
            createdByName = transaction.createdByName.orEmpty(),
        )
        outboxDao.upsert(
            SyncOutboxEntity(
                operationId = opId,
                entityType = SyncOutboxEntity.ENTITY_TYPE_TRANSACTION,
                entityId = transaction.id.toString(),
                walletRemoteId = walletRemoteId,
                action = SyncOutboxEntity.ACTION_UPSERT,
                payloadJson = json.encodeToString(OutboxTransactionPayload.serializer(), payload),
                status = SyncOutboxEntity.STATUS_PENDING,
                retryCount = 0,
                lastError = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        return opId
    }

    suspend fun enqueueTransactionSoftDelete(
        walletRemoteId: String,
        transaction: TransactionEntity,
        deletedAtEpochMs: Long,
    ): String {
        val opId = transaction.operationId?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        val now = nowEpochMs()
        val effectiveUpdatedAt = maxOf(transaction.updatedAtEpochMs, deletedAtEpochMs)
        val payload = OutboxTransactionPayload(
            operationId = opId,
            remoteId = transaction.remoteId,
            localTransactionId = transaction.id,
            walletRemoteId = walletRemoteId,
            type = transaction.type,
            amountMinor = transaction.amountCents,
            categoryId = transaction.category,
            note = transaction.note,
            occurredAtEpochMs = transaction.occurredAt,
            updatedAtEpochMs = effectiveUpdatedAt,
            deletedAtEpochMs = deletedAtEpochMs,
            createdByName = transaction.createdByName.orEmpty(),
        )
        outboxDao.upsert(
            SyncOutboxEntity(
                operationId = opId,
                entityType = SyncOutboxEntity.ENTITY_TYPE_TRANSACTION,
                entityId = transaction.id.toString(),
                walletRemoteId = walletRemoteId,
                action = SyncOutboxEntity.ACTION_DELETE,
                payloadJson = json.encodeToString(OutboxTransactionPayload.serializer(), payload),
                status = SyncOutboxEntity.STATUS_PENDING,
                retryCount = 0,
                lastError = null,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
            ),
        )
        return opId
    }

    suspend fun processPending(maxRetries: Int = DEFAULT_MAX_RETRIES): SyncProcessResult {
        if (!remoteGateway.isAvailable) {
            val pending = outboxDao.getRetryable(maxRetries).size
            return SyncProcessResult(skippedCount = pending)
        }

        val items = outboxDao.getRetryable(maxRetries)
        var synced = 0
        var failed = 0
        var skipped = 0

        for (item in items) {
            if (item.entityType != SyncOutboxEntity.ENTITY_TYPE_TRANSACTION) {
                skipped++
                continue
            }
            val now = nowEpochMs()
            var decodedPayload: OutboxTransactionPayload? = null
            try {
                val payload = json.decodeFromString(OutboxTransactionPayload.serializer(), item.payloadJson)
                decodedPayload = payload
                val remoteDto = RemoteSharedTransactionDto(
                    operationId = payload.operationId,
                    walletId = payload.walletRemoteId,
                    createdByName = payload.createdByName,
                    type = payload.type,
                    amountMinor = payload.amountMinor,
                    categoryId = payload.categoryId,
                    note = payload.note,
                    occurredAt = Instant.fromEpochMilliseconds(payload.occurredAtEpochMs).toString(),
                    updatedAt = Instant.fromEpochMilliseconds(payload.updatedAtEpochMs).toString(),
                    deletedAt = payload.deletedAtEpochMs?.let { Instant.fromEpochMilliseconds(it).toString() },
                )
                val savedRemote = remoteGateway.upsertTransaction(remoteDto)
                transactionDao.markSyncSynced(
                    id = payload.localTransactionId,
                    operationId = payload.operationId,
                    expectedUpdatedAt = payload.updatedAtEpochMs,
                    expectedDeletedAt = payload.deletedAtEpochMs,
                    remoteId = savedRemote.id,
                    createdByUserId = savedRemote.createdBy,
                    createdByName = savedRemote.createdByName.ifBlank { null },
                    remoteUpdatedAt = parseEpochMsOrZero(savedRemote.updatedAt),
                )
                outboxDao.updateStatus(
                    operationId = item.operationId,
                    expectedUpdatedAt = item.updatedAtEpochMs,
                    expectedPayloadJson = item.payloadJson,
                    expectedStatus = item.status,
                    status = SyncOutboxEntity.STATUS_SYNCED,
                    retryCount = item.retryCount,
                    lastError = null,
                    updatedAt = now,
                )
                synced++
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                failed++
                val markedFailed = outboxDao.updateStatus(
                    operationId = item.operationId,
                    expectedUpdatedAt = item.updatedAtEpochMs,
                    expectedPayloadJson = item.payloadJson,
                    expectedStatus = item.status,
                    status = SyncOutboxEntity.STATUS_FAILED,
                    retryCount = item.retryCount + 1,
                    lastError = error.message?.take(MAX_ERROR_LENGTH) ?: error::class.simpleName,
                    updatedAt = now,
                )
                if (markedFailed > 0) {
                    decodedPayload?.let { payload ->
                        transactionDao.markSyncFailed(
                            payload.localTransactionId,
                            payload.operationId,
                            payload.updatedAtEpochMs,
                        )
                    }
                }
            }
        }

        if (synced > 0) {
            outboxDao.purgeSynced()
        }
        return SyncProcessResult(syncedCount = synced, failedCount = failed, skippedCount = skipped)
    }

    suspend fun applyRemoteTransaction(
        remote: RemoteSharedTransactionDto,
        localWalletId: Long,
    ): ConflictResolutionOutcome {
        val remoteUpdatedMs = parseEpochMsOrZero(remote.updatedAt)
        val remoteOccurredMs = parseEpochMsOrZero(remote.occurredAt).takeIf { it > 0L } ?: nowEpochMs()
        val remoteDeletedMs = remote.deletedAt?.let(::parseEpochMsOrZero)?.takeIf { it > 0L }

        val existing = remote.id?.let { transactionDao.getByRemoteId(it) }
            ?: transactionDao.getByOperationId(remote.operationId)

        if (existing != null && existing.updatedAtEpochMs > remoteUpdatedMs && remoteUpdatedMs > 0L) {
            return ConflictResolutionOutcome.KEPT_LOCAL_NEWER
        }

        val normalizedType = if (remote.type.equals(TransactionType.INCOME.name, ignoreCase = true)) {
            TransactionType.INCOME.name
        } else {
            TransactionType.EXPENSE.name
        }
        val normalizedCategory = Category.safeValueOf(remote.categoryId).id

        val merged = TransactionEntity(
            id = existing?.id ?: 0L,
            amountCents = remote.amountMinor.coerceAtLeast(1L),
            type = normalizedType,
            category = normalizedCategory,
            note = remote.note.take(MAX_NOTE_LENGTH),
            occurredAt = remoteOccurredMs,
            walletId = localWalletId,
            createdAt = existing?.createdAt ?: remoteOccurredMs,
            isAutoCapture = existing?.isAutoCapture ?: false,
            isInternalTransfer = false,
            transferGroupId = null,
            remoteId = remote.id ?: existing?.remoteId,
            operationId = remote.operationId,
            syncStatus = SyncStatus.SYNCED.name,
            createdByUserId = remote.createdBy ?: existing?.createdByUserId,
            createdByName = remote.createdByName.ifBlank { existing?.createdByName.orEmpty() }
                .ifBlank { null },
            updatedAtEpochMs = maxOf(remoteUpdatedMs, existing?.updatedAtEpochMs ?: 0L),
            deletedAtEpochMs = remoteDeletedMs,
        )
        transactionDao.upsert(merged)

        return when {
            remoteDeletedMs != null -> ConflictResolutionOutcome.SOFT_DELETED
            existing == null -> ConflictResolutionOutcome.INSERTED
            else -> ConflictResolutionOutcome.UPDATED_FROM_REMOTE
        }
    }

    private fun parseEpochMsOrZero(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(raw.trim()).toEpochMilliseconds() }.getOrDefault(0L)
    }

    companion object {
        const val DEFAULT_MAX_RETRIES = 5
        private const val MAX_ERROR_LENGTH = 240
        private const val MAX_NOTE_LENGTH = 200
    }
}
