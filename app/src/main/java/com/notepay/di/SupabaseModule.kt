package com.notepay.di

import com.notepay.data.remote.supabase.SharedTransactionRemoteGateway
import com.notepay.data.remote.supabase.SupabaseSharedTransactionGateway
import com.notepay.data.repository.SharedWalletRepositoryImpl
import com.notepay.domain.repository.SharedWalletRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SupabaseModule {

    @Binds
    @Singleton
    abstract fun bindSharedTransactionRemoteGateway(
        impl: SupabaseSharedTransactionGateway,
    ): SharedTransactionRemoteGateway

    @Binds
    @Singleton
    abstract fun bindSharedWalletRepository(
        impl: SharedWalletRepositoryImpl,
    ): SharedWalletRepository

    companion object {
        @Provides
        @Singleton
        fun provideSupabaseJson(): Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }
    }
}
