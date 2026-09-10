package com.notepay.domain.wallet

import com.notepay.domain.money.Money

/** Immutable Wallet Model */
data class WalletDomainModel(
    val id: Long,
    val name: String,
    val initialBalance: Money,
    val budgetLimit: Money,
    val linkedPackageName: String? = null,
    val isArchived: Boolean = false
)
