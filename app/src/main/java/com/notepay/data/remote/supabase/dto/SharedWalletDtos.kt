package com.notepay.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateSharedWalletInsertDto(
    @SerialName("name") val name: String,
    @SerialName("currency") val currency: String,
    @SerialName("purpose") val purpose: String = "SHARED",
    @SerialName("monthly_budget_minor") val monthlyBudgetMinor: Long? = null,
)

@Serializable
data class RemoteSharedWalletDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("purpose") val purpose: String = "SHARED",
    @SerialName("monthly_budget_minor") val monthlyBudgetMinor: Long? = null,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class RemoteSharedWalletMemberDto(
    @SerialName("wallet_id") val walletId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String = "",
    @SerialName("role") val role: String,
    @SerialName("spending_limit_minor") val spendingLimitMinor: Long? = null,
    @SerialName("joined_at") val joinedAt: String = "",
)

@Serializable
data class UpdateSharedWalletMemberDto(
    @SerialName("role") val role: String,
    @SerialName("spending_limit_minor") val spendingLimitMinor: Long? = null,
)

@Serializable
data class CreateSharedWalletInviteDto(
    @SerialName("code") val code: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("role") val role: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("max_uses") val maxUses: Int,
)

@Serializable
data class RemoteSharedWalletInviteDto(
    @SerialName("code") val code: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("role") val role: String,
    @SerialName("created_by") val createdBy: String = "",
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("max_uses") val maxUses: Int = 1,
    @SerialName("used_count") val usedCount: Int = 0,
    @SerialName("revoked_at") val revokedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class UpsertSharedTransactionDto(
    @SerialName("operation_id") val operationId: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("created_by_name") val createdByName: String = "",
    @SerialName("type") val type: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("category_id") val categoryId: String,
    @SerialName("note") val note: String = "",
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class RemoteSharedTransactionDto(
    @SerialName("id") val id: String? = null,
    @SerialName("operation_id") val operationId: String,
    @SerialName("wallet_id") val walletId: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_by_name") val createdByName: String = "",
    @SerialName("type") val type: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("category_id") val categoryId: String,
    @SerialName("note") val note: String = "",
    @SerialName("occurred_at") val occurredAt: String,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null,
)

@Serializable
data class JoinSharedWalletRpcResult(
    @SerialName("wallet_id") val walletId: String,
    @SerialName("wallet_name") val walletName: String,
    @SerialName("currency") val currency: String = "VND",
    @SerialName("monthly_budget_minor") val monthlyBudgetMinor: Long? = null,
    @SerialName("role") val role: String,
)

@Serializable
data class OutboxTransactionPayload(
    val operationId: String,
    val remoteId: String? = null,
    val localTransactionId: Long,
    val walletRemoteId: String,
    val type: String,
    val amountMinor: Long,
    val categoryId: String,
    val note: String,
    val occurredAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val deletedAtEpochMs: Long? = null,
    val createdByName: String = "",
)
