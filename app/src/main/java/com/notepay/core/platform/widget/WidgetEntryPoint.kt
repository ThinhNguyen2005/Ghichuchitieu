package com.notepay.platform.widget

import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun transactionRepository(): TransactionRepository
    fun walletRepository(): WalletRepository
    fun budgetSettingsStore(): BudgetSettingsStore
    fun getMonthlySummaryUseCase(): GetMonthlySummaryUseCase
}
