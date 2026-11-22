package com.notepay.data.sync

import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.remote.supabase.SupabaseClientProvider
import com.notepay.data.remote.supabase.dto.RemoteSharedTransactionDto
import com.notepay.di.IoDispatcher
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Supabase Realtime subscriptions filtered by each joined `shared_wallet_id`.
 * Incoming insert/update/delete events flow directly into Room via [SyncOutboxProcessor],
 * triggering reactive Compose updates automatically.
 */
@Singleton
class SharedWalletRealtimeManager @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val outboxProcessor: SyncOutboxProcessor,
    private val walletDao: WalletDao,
    private val transactionDao: TransactionDao,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val mutex = Mutex()
    private val channels = mutableMapOf<String, RealtimeChannel>()
    private val collectorJobs = mutableMapOf<String, Job>()

    suspend fun startForJoinedWallets() {
        val client = clientProvider.client ?: return
        val sharedWallets = walletDao.getSharedWallets()
        val desiredIds = sharedWallets.mapNotNull { w ->
            w.remoteId?.takeIf { it.isNotBlank() }?.let { it to w.id }
        }.toMap()

        mutex.withLock {
            val toRemove = channels.keys.filter { it !in desiredIds.keys }
            for (remoteId in toRemove) {
                unsubscribeLocked(remoteId)
            }
            for ((remoteId, localWalletId) in desiredIds) {
                if (remoteId !in channels) {
                    subscribeLocked(client, remoteId, localWalletId)
                }
            }
        }
    }

    suspend fun stopAll() {
        mutex.withLock {
            val keys = channels.keys.toList()
            for (remoteId in keys) {
                unsubscribeLocked(remoteId)
            }
        }
    }

    private suspend fun subscribeLocked(
        client: io.github.jan.supabase.SupabaseClient,
        walletRemoteId: String,
        localWalletId: Long,
    ) {
        val channelName = "shared-wallet-tx-$walletRemoteId"
        val channel = client.realtime.channel(channelName)
        val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "shared_transactions"
            filter("wallet_id", FilterOperator.EQ, walletRemoteId)
        }

        channels[walletRemoteId] = channel
        collectorJobs[walletRemoteId] = scope.launch {
            changeFlow
                .catch { error ->
                    if (error is CancellationException) throw error
                }
                .collect { action ->
                    handlePostgresAction(action, localWalletId)
                }
        }

        try {
            channel.subscribe(blockUntilSubscribed = false)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            // Best-effort subscription; WorkManager periodic sync handles catch-up when offline.
        }
    }

    private suspend fun handlePostgresAction(action: PostgresAction, localWalletId: Long) {
        when (action) {
            is PostgresAction.Insert -> {
                val dto = runCatching {
                    json.decodeFromJsonElement(RemoteSharedTransactionDto.serializer(), action.record)
                }.getOrNull() ?: return
                outboxProcessor.applyRemoteTransaction(dto, localWalletId)
            }
            is PostgresAction.Update -> {
                val dto = runCatching {
                    json.decodeFromJsonElement(RemoteSharedTransactionDto.serializer(), action.record)
                }.getOrNull() ?: return
                outboxProcessor.applyRemoteTransaction(dto, localWalletId)
            }
            is PostgresAction.Delete -> {
                val deletedRemoteId = action.oldRecord["id"]?.jsonPrimitive?.content ?: return
                val existing = transactionDao.getByRemoteId(deletedRemoteId) ?: return
                transactionDao.markSoftDeleted(
                    id = existing.id,
                    deletedAtEpochMs = System.currentTimeMillis(),
                    updatedAtEpochMs = System.currentTimeMillis(),
                    syncStatus = "SYNCED",
                )
            }
            else -> Unit
        }
    }

    private suspend fun unsubscribeLocked(walletRemoteId: String) {
        collectorJobs.remove(walletRemoteId)?.cancel()
        val channel = channels.remove(walletRemoteId) ?: return
        val client = clientProvider.client ?: return
        runCatching { client.realtime.removeChannel(channel) }
    }
}
