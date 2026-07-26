package com.notepay.domain.wallet

import com.notepay.domain.money.Money

/** Internal Cashflow Summary (distinguishing internal transfers) */
data class WalletCashflowSummary(
    val totalExternalIncome: Money,
    val totalExternalExpense: Money,
    val totalInternalTransfers: Money,
    val netCashflow: Money
)
