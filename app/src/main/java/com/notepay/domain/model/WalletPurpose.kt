package com.notepay.domain.model

enum class WalletPurpose {
    PERSONAL,
    BUSINESS,
    SAVINGS_GOAL,
    TRAVEL,
    SHARED;

    val supportsGoalTarget: Boolean
        get() = this == SAVINGS_GOAL || this == TRAVEL

    companion object {
        fun fromString(raw: String?): WalletPurpose =
            entries.firstOrNull { it.name.equals(raw?.trim(), ignoreCase = true) } ?: PERSONAL
    }
}
