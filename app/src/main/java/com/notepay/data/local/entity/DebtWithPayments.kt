package com.notepay.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Quan hệ 1-N giữa khoản nợ và danh sách các đợt trả nợ từng phần
 */
data class DebtWithPayments(
    @Embedded val debt: DebtEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "debt_id"
    )
    val payments: List<DebtPaymentEntity> = emptyList(),
)
