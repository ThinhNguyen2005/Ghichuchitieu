package com.notepay.di

import android.content.Context
import androidx.room.Room
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.SeedCallback
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        seedCallback: SeedCallback,
    ): NotePayDatabase = Room.databaseBuilder(
        context,
        NotePayDatabase::class.java,
        NotePayDatabase.DB_NAME,
    )
        .addCallback(seedCallback)
        .addMigrations(
            NotePayDatabase.MIGRATION_1_2,
            NotePayDatabase.MIGRATION_2_3,
            NotePayDatabase.MIGRATION_3_4,
            NotePayDatabase.MIGRATION_4_5,
        )
        .build()

    @Provides
    fun provideTransactionDao(db: NotePayDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideWalletDao(db: NotePayDatabase): WalletDao = db.walletDao()

    @Provides
    fun provideBillSplitDao(db: NotePayDatabase): BillSplitDao = db.billSplitDao()

    @Provides
    fun provideSubscriptionDao(db: NotePayDatabase): SubscriptionDao = db.subscriptionDao()
}
