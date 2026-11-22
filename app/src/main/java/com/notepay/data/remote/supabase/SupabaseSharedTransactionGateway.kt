package com.notepay.data.remote.supabase

import com.notepay.data.remote.supabase.dto.RemoteSharedTransactionDto
import com.notepay.data.remote.supabase.dto.UpsertSharedTransactionDto
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseSharedTransactionGateway @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
) : SharedTransactionRemoteGateway {

    override val isAvailable: Boolean
        get() = clientProvider.client != null

    override suspend fun upsertTransaction(payload: RemoteSharedTransactionDto): RemoteSharedTransactionDto {
        val client = requireNotNull(clientProvider.client) {
            "Supabase is not configured (offline-only mode)"
        }
        val requestBody = UpsertSharedTransactionDto(
            operationId = payload.operationId,
            walletId = payload.walletId,
            createdByName = payload.createdByName,
            type = payload.type,
            amountMinor = payload.amountMinor,
            categoryId = payload.categoryId,
            note = payload.note,
            occurredAt = payload.occurredAt,
            updatedAt = payload.updatedAt,
            deletedAt = payload.deletedAt,
        )
        return client.postgrest["shared_transactions"]
            .upsert(requestBody) {
                onConflict = "operation_id"
                select()
            }
            .decodeSingle<RemoteSharedTransactionDto>()
    }

    override suspend fun fetchTransactions(walletRemoteId: String): List<RemoteSharedTransactionDto> {
        val client = requireNotNull(clientProvider.client) {
            "Supabase is not configured (offline-only mode)"
        }
        return client.postgrest["shared_transactions"]
            .select {
                filter {
                    eq("wallet_id", walletRemoteId)
                }
            }
            .decodeList<RemoteSharedTransactionDto>()
    }
}
