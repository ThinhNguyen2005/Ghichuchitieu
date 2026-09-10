package com.notepay.ui.feature.billsplit

import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.VietQrBank
import com.notepay.domain.model.Wallet

data class BillSplitUiState(
    val unpaidSplits: List<BillSplitItemState> = emptyList(),
    val paidSplits: List<BillSplitItemState> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val wallets: List<Wallet> = emptyList(),
    val activeWallet: Wallet? = null,
    val banks: List<VietQrBank> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class BillSplitItemState(
    val split: BillSplit,
    val parentTransaction: Transaction?,
    val wallet: Wallet? = null,
)
