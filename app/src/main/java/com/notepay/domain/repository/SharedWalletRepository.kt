package com.notepay.domain.repository

import com.notepay.domain.model.SharedWalletInvite
import com.notepay.domain.model.SharedWalletMember
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.SyncSummary
import com.notepay.domain.model.Wallet

/**
 * Contract for shared wallets management, offline-first outbox synchronization,
 * and Supabase Realtime subscription.
 */
interface SharedWalletRepository {
    val isCloudConfigured: Boolean

    fun currentUserId(): String?

    suspend fun createSharedWallet(
        name: String,
        currencyCode: String = "VND",
        monthlyBudgetMinor: Long? = null,
    ): Result<Wallet>

    suspend fun joinWalletByCode(
        inviteCode: String,
        displayName: String = "",
    ): Result<Wallet>

    suspend fun getMembers(walletRemoteId: String): Result<List<SharedWalletMember>>

    suspend fun updateMember(
        walletRemoteId: String,
        userId: String,
        role: SharedWalletRole,
        spendingLimitMinor: Long?,
    ): Result<Unit>

    suspend fun removeMember(walletRemoteId: String, userId: String): Result<Unit>

    suspend fun createInvite(
        walletRemoteId: String,
        role: SharedWalletRole,
        expiresInHours: Int,
        maxUses: Int,
    ): Result<SharedWalletInvite>

    suspend fun listInvites(walletRemoteId: String): Result<List<SharedWalletInvite>>

    suspend fun revokeInvite(code: String): Result<Unit>

    suspend fun syncAllSharedWallets(): Result<SyncSummary>

    suspend fun startRealtimeSync()

    suspend fun stopRealtimeSync()
}
