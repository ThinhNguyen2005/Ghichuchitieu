package com.notepay.data.sync

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.entity.SyncOutboxEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.remote.supabase.SharedTransactionRemoteGateway
import com.notepay.data.remote.supabase.dto.RemoteSharedTransactionDto
import com.notepay.domain.model.SyncStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SyncOutboxProcessorTest {

    private lateinit var database: NotePayDatabase
    private var fixedTimeMs = 1_760_000_000_000L
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            NotePayDatabase::class.java,
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun insertSharedWallet(remoteId: String = "wallet-uuid-1"): Long =
        database.walletDao().upsert(
            WalletEntity(
                name = "Ví gia đình",
                initialBalanceCents = 0L,
                iconKey = "bank",
                colorKey = "primary",
                createdAt = 1_000L,
                purpose = "SHARED",
                remoteId = remoteId,
                isShared = true,
                sharedRole = "OWNER",
                syncStatus = SyncStatus.SYNCED.name,
            ),
        )

    private fun newProcessor(gateway: SharedTransactionRemoteGateway): SyncOutboxProcessor =
        SyncOutboxProcessor(
            outboxDao = database.syncOutboxDao(),
            transactionDao = database.transactionDao(),
            remoteGateway = gateway,
            json = json,
            nowEpochMs = { fixedTimeMs },
        )

    @Test
    fun `offline enqueue creates pending outbox item and skips sync while offline`() = runTest {
        val walletId = insertSharedWallet()
        val opId = UUID.randomUUID().toString()
        val txId = database.transactionDao().upsert(
            TransactionEntity(
                amountCents = 45_000_00L,
                type = "EXPENSE",
                category = "FOOD",
                note = "Bún bò sáng",
                occurredAt = fixedTimeMs,
                walletId = walletId,
                createdAt = fixedTimeMs,
                operationId = opId,
                syncStatus = SyncStatus.PENDING.name,
                createdByUserId = "uid-1",
                createdByName = "Thịnh",
                updatedAtEpochMs = fixedTimeMs,
            ),
        )
        val savedTx = database.transactionDao().getById(txId)!!
        val offlineGateway = FakeRemoteGateway(isAvailable = false)
        val processor = newProcessor(offlineGateway)

        val enqueuedOpId = processor.enqueueTransactionUpsert("wallet-uuid-1", savedTx)
        val outboxRow = database.syncOutboxDao().getByOperationId(enqueuedOpId)
        assertThat(outboxRow).isNotNull()
        assertThat(outboxRow!!.status).isEqualTo(SyncOutboxEntity.STATUS_PENDING)
        assertThat(database.syncOutboxDao().observePendingCount().first()).isEqualTo(1)

        val result = processor.processPending()
        assertThat(result.skippedCount).isEqualTo(1)
        assertThat(result.syncedCount).isEqualTo(0)
        assertThat(offlineGateway.upsertedPayloads).isEmpty()
    }

    @Test
    fun `online retry transitions failed item to synced and updates local tx remoteId`() = runTest {
        val walletId = insertSharedWallet()
        val opId = UUID.randomUUID().toString()
        val txId = database.transactionDao().upsert(
            TransactionEntity(
                amountCents = 120_000_00L,
                type = "EXPENSE",
                category = "SHOPPING",
                note = "Mua đồ siêu thị",
                occurredAt = fixedTimeMs,
                walletId = walletId,
                createdAt = fixedTimeMs,
                operationId = opId,
                syncStatus = SyncStatus.PENDING.name,
                createdByUserId = "uid-1",
                createdByName = "Thịnh",
                updatedAtEpochMs = fixedTimeMs,
            ),
        )
        val savedTx = database.transactionDao().getById(txId)!!
        val gateway = FakeRemoteGateway(isAvailable = true, failNextAttempts = 1)
        val processor = newProcessor(gateway)

        processor.enqueueTransactionUpsert("wallet-uuid-1", savedTx)

        // Attempt 1: transient network failure -> retry_count = 1, status = FAILED
        val firstPass = processor.processPending()
        assertThat(firstPass.failedCount).isEqualTo(1)
        assertThat(firstPass.syncedCount).isEqualTo(0)
        val failedRow = database.syncOutboxDao().getByOperationId(opId)!!
        assertThat(failedRow.status).isEqualTo(SyncOutboxEntity.STATUS_FAILED)
        assertThat(failedRow.retryCount).isEqualTo(1)
        assertThat(failedRow.lastError).contains("timeout")
        assertThat(database.transactionDao().getById(txId)?.syncStatus).isEqualTo(SyncStatus.FAILED.name)

        // Attempt 2: online recovery -> status = SYNCED (purged), local tx gets remoteId + SYNCED
        fixedTimeMs += 5_000L
        val secondPass = processor.processPending()
        assertThat(secondPass.syncedCount).isEqualTo(1)
        assertThat(secondPass.failedCount).isEqualTo(0)
        assertThat(database.syncOutboxDao().getByOperationId(opId)).isNull()

        val syncedLocal = database.transactionDao().getById(txId)!!
        assertThat(syncedLocal.remoteId).isEqualTo("remote-$opId")
        assertThat(syncedLocal.syncStatus).isEqualTo(SyncStatus.SYNCED.name)
    }

    @Test
    fun `idempotent dedup re-enqueue uses same operationId and prevents duplicate local transactions`() = runTest {
        val walletId = insertSharedWallet()
        val opId = UUID.randomUUID().toString()
        val txId = database.transactionDao().upsert(
            TransactionEntity(
                amountCents = 80_000_00L,
                type = "EXPENSE",
                category = "TRANSPORT",
                note = "Đổ xăng",
                occurredAt = fixedTimeMs,
                walletId = walletId,
                createdAt = fixedTimeMs,
                operationId = opId,
                syncStatus = SyncStatus.PENDING.name,
                updatedAtEpochMs = fixedTimeMs,
            ),
        )
        val savedTx = database.transactionDao().getById(txId)!!
        val gateway = FakeRemoteGateway(isAvailable = true)
        val processor = newProcessor(gateway)

        processor.enqueueTransactionUpsert("wallet-uuid-1", savedTx)
        processor.enqueueTransactionUpsert("wallet-uuid-1", savedTx)
        assertThat(database.syncOutboxDao().getAll()).hasSize(1)

        processor.processPending()
        assertThat(gateway.upsertedPayloads).hasSize(1)
        assertThat(gateway.upsertedPayloads.single().operationId).isEqualTo(opId)

        // Applying remote echo of same operationId updates in place without duplicating row.
        val echo = gateway.upsertedPayloads.single().copy(
            id = "remote-$opId",
            updatedAt = "2025-10-15T12:00:00Z",
        )
        val outcome = processor.applyRemoteTransaction(echo, walletId)
        assertThat(outcome).isEqualTo(ConflictResolutionOutcome.UPDATED_FROM_REMOTE)
        assertThat(database.transactionDao().observeByWallet(walletId).first()).hasSize(1)
    }

    @Test
    fun `stale push completion leaves newer local edit and outbox payload pending`() = runTest {
        val walletId = insertSharedWallet()
        val opId = UUID.randomUUID().toString()
        val txId = database.transactionDao().upsert(
            TransactionEntity(
                amountCents = 12_000L,
                type = "EXPENSE",
                category = "FOOD",
                note = "Old note",
                occurredAt = fixedTimeMs,
                walletId = walletId,
                createdAt = fixedTimeMs,
                operationId = opId,
                syncStatus = SyncStatus.PENDING.name,
                updatedAtEpochMs = fixedTimeMs,
            ),
        )
        val enteredGateway = CompletableDeferred<Unit>()
        val releaseGateway = CompletableDeferred<Unit>()
        val gateway = FakeRemoteGateway(isAvailable = true) {
            enteredGateway.complete(Unit)
            releaseGateway.await()
        }
        val processor = newProcessor(gateway)
        processor.enqueueTransactionUpsert("wallet-uuid-1", database.transactionDao().getById(txId)!!)

        val processing = async { processor.processPending() }
        enteredGateway.await()

        val newerVersion = database.transactionDao().getById(txId)!!.copy(
            amountCents = 34_000L,
            note = "New note",
            updatedAtEpochMs = fixedTimeMs + 1,
            syncStatus = SyncStatus.PENDING.name,
        )
        database.transactionDao().upsert(newerVersion)
        processor.enqueueTransactionUpsert("wallet-uuid-1", newerVersion)
        releaseGateway.complete(Unit)
        processing.await()

        val local = database.transactionDao().getById(txId)!!
        val queued = database.syncOutboxDao().getByOperationId(opId)!!
        assertThat(local.note).isEqualTo("New note")
        assertThat(local.syncStatus).isEqualTo(SyncStatus.PENDING.name)
        assertThat(queued.status).isEqualTo(SyncOutboxEntity.STATUS_PENDING)
        assertThat(queued.payloadJson).contains("New note")
    }

    @Test
    fun `last write wins keeps newer local edit and applies newer remote edit plus soft delete`() = runTest {
        val walletId = insertSharedWallet()
        val opId = UUID.randomUUID().toString()
        val localUpdatedMs = 1_760_000_010_000L // newer
        val olderRemoteIso = "2025-10-01T00:00:00Z" // ~1759276800000 < 1760000010000
        val newerRemoteIso = "2025-11-22T05:00:00Z" // > 1760000010000

        val txId = database.transactionDao().upsert(
            TransactionEntity(
                amountCents = 100_000_00L,
                type = "EXPENSE",
                category = "FOOD",
                note = "Bản sửa mới hơn ở máy",
                occurredAt = fixedTimeMs,
                walletId = walletId,
                createdAt = fixedTimeMs,
                remoteId = "remote-lww-1",
                operationId = opId,
                syncStatus = SyncStatus.PENDING.name,
                createdByName = "Thịnh",
                updatedAtEpochMs = localUpdatedMs,
            ),
        )

        val processor = newProcessor(FakeRemoteGateway(isAvailable = true))

        // 1. Stale remote payload -> keep local newer copy
        val staleRemote = RemoteSharedTransactionDto(
            id = "remote-lww-1",
            operationId = opId,
            walletId = "wallet-uuid-1",
            createdBy = "uid-2",
            createdByName = "An",
            type = "EXPENSE",
            amountMinor = 999_000_00L,
            categoryId = "FOOD",
            note = "Bản cũ từ cloud",
            occurredAt = olderRemoteIso,
            updatedAt = olderRemoteIso,
        )
        val kept = processor.applyRemoteTransaction(staleRemote, walletId)
        assertThat(kept).isEqualTo(ConflictResolutionOutcome.KEPT_LOCAL_NEWER)
        assertThat(database.transactionDao().getById(txId)!!.note).isEqualTo("Bản sửa mới hơn ở máy")

        // 2. Newer remote payload -> overwrite local
        val freshRemote = staleRemote.copy(
            note = "Cơm gia đình (đã sửa bởi An)",
            amountMinor = 150_000_00L,
            updatedAt = newerRemoteIso,
        )
        val updated = processor.applyRemoteTransaction(freshRemote, walletId)
        assertThat(updated).isEqualTo(ConflictResolutionOutcome.UPDATED_FROM_REMOTE)
        val localAfterUpdate = database.transactionDao().getById(txId)!!
        assertThat(localAfterUpdate.note).isEqualTo("Cơm gia đình (đã sửa bởi An)")
        assertThat(localAfterUpdate.amountCents).isEqualTo(150_000_00L)

        // 3. Remote soft delete -> marks deletedAtEpochMs and excludes from active observeByWallet
        val softDeletedRemote = freshRemote.copy(
            updatedAt = "2025-11-22T06:00:00Z",
            deletedAt = "2025-11-22T06:00:00Z",
        )
        val deletedOutcome = processor.applyRemoteTransaction(softDeletedRemote, walletId)
        assertThat(deletedOutcome).isEqualTo(ConflictResolutionOutcome.SOFT_DELETED)
        assertThat(database.transactionDao().observeByWallet(walletId).first()).isEmpty()
    }

    private class FakeRemoteGateway(
        override val isAvailable: Boolean,
        private var failNextAttempts: Int = 0,
        private val beforeUpsert: suspend () -> Unit = {},
    ) : SharedTransactionRemoteGateway {
        val upsertedPayloads = mutableListOf<RemoteSharedTransactionDto>()

        override suspend fun upsertTransaction(payload: RemoteSharedTransactionDto): RemoteSharedTransactionDto {
            beforeUpsert()
            if (failNextAttempts > 0) {
                failNextAttempts--
                throw IOException("Network timeout")
            }
            upsertedPayloads += payload
            return payload.copy(
                id = payload.id ?: "remote-${payload.operationId}",
                createdBy = "uid-1",
                updatedAt = "2025-10-15T12:00:00Z",
            )
        }

        override suspend fun fetchTransactions(walletRemoteId: String): List<RemoteSharedTransactionDto> = emptyList()
    }
}
