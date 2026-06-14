package com.notepay.di

import com.notepay.data.repository.TransactionRepositoryImpl
import com.notepay.data.repository.WalletRepositoryImpl
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.data.repository.BillSplitRepositoryImpl
import com.notepay.data.repository.SubscriptionRepositoryImpl
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindWalletRepository(impl: WalletRepositoryImpl): WalletRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBillSplitRepository(impl: BillSplitRepositoryImpl): BillSplitRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository
}
