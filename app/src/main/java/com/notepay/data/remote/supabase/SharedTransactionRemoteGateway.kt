package com.notepay.data.remote.supabase

import com.notepay.data.remote.supabase.dto.RemoteSharedTransactionDto

interface SharedTransactionRemoteGateway {
    val isAvailable: Boolean

    suspend fun upsertTransaction(payload: RemoteSharedTransactionDto): RemoteSharedTransactionDto

    suspend fun fetchTransactions(walletRemoteId: String): List<RemoteSharedTransactionDto>
}
