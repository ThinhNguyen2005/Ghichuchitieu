package com.notepay.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "amount_cents") val amountCents: Long,
    @ColumnInfo(name = "category") val category: String,
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long, // epoch ms
    @ColumnInfo(name = "repeat_months") val repeatMonths: Int, // 1, 3, 6, 12
    @ColumnInfo(name = "remind_days_before") val remindDaysBefore: Int, // 1, 2, 3, 7
    @ColumnInfo(name = "note") val note: String = "",
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
