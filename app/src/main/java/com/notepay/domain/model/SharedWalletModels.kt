package com.notepay.domain.model

import androidx.compose.runtime.Immutable

/**
 * Access roles inside a collaborative shared wallet.
 * Enforced by both Supabase PostgreSQL RLS policies and client permission guards.
 */
enum class SharedWalletRole {
    OWNER,
    EDITOR,
    VIEWER;

    companion object {
        fun fromString(raw: String?): SharedWalletRole? =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) }
    }
}

/**
 * Synchronization state between local Room database and Supabase cloud.
 */
enum class SyncStatus {
    LOCAL_ONLY,
    PENDING,
    SYNCED,
    FAILED;

    companion object {
        fun fromString(raw: String?): SyncStatus =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: LOCAL_ONLY
    }
}

@Immutable
data class SharedWalletMember(
    val walletRemoteId: String,
    val userId: String,
    val displayName: String,
    val role: SharedWalletRole,
    val spendingLimitMinor: Long? = null,
    val joinedAtEpochMs: Long = 0L,
)

@Immutable
data class SharedWalletInvite(
    val code: String,
    val walletRemoteId: String,
    val role: SharedWalletRole,
    val expiresAtEpochMs: Long,
    val maxUses: Int,
    val usedCount: Int,
    val revokedAtEpochMs: Long? = null,
)

@Immutable
data class SyncSummary(
    val pushedCount: Int = 0,
    val failedPushCount: Int = 0,
    val pulledTransactionsCount: Int = 0,
    val pulledWalletsCount: Int = 0,
)
