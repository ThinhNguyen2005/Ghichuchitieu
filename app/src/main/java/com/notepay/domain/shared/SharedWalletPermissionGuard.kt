package com.notepay.domain.shared

import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet

class SharedWalletPermissionDeniedException(
    message: String = "Permission denied for shared wallet operation",
) : SecurityException(message)

/**
 * Pure domain permission guard mirroring Supabase PostgreSQL RLS rules for UI & UseCases.
 * Personal wallets (`!wallet.isShared`) permit all local-first operations unconditionally.
 */
object SharedWalletPermissionGuard {

    fun canAddTransaction(wallet: Wallet): Boolean {
        if (!wallet.isShared) return true
        return wallet.sharedRole == SharedWalletRole.OWNER ||
            wallet.sharedRole == SharedWalletRole.EDITOR
    }

    fun canEditTransaction(
        wallet: Wallet,
        transaction: Transaction,
        currentUserId: String?,
    ): Boolean {
        if (!wallet.isShared) return true
        return when (wallet.sharedRole) {
            SharedWalletRole.OWNER -> true
            SharedWalletRole.EDITOR -> {
                val authorId = transaction.createdByUserId
                // Allow editing rows created by this user locally before cloud auth UID existed.
                currentUserId != null && (authorId == null || authorId == currentUserId)
            }
            SharedWalletRole.VIEWER, null -> false
        }
    }

    fun canDeleteTransaction(
        wallet: Wallet,
        transaction: Transaction,
        currentUserId: String?,
    ): Boolean = canEditTransaction(wallet, transaction, currentUserId)

    fun canManageMembers(wallet: Wallet): Boolean {
        if (!wallet.isShared) return false
        return wallet.sharedRole == SharedWalletRole.OWNER
    }

    fun canCreateInvite(wallet: Wallet): Boolean = canManageMembers(wallet)

    fun canDeleteWallet(wallet: Wallet): Boolean {
        if (!wallet.isShared) return true
        return wallet.sharedRole == SharedWalletRole.OWNER
    }
}
