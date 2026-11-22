package com.notepay.di

import android.content.Context
import androidx.room.Room
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.SeedCallback
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.dao.SyncOutboxDao
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
            NotePayDatabase.MIGRATION_5_6,
            NotePayDatabase.MIGRATION_6_7,
            NotePayDatabase.MIGRATION_7_8,
            NotePayDatabase.MIGRATION_8_9,
            NotePayDatabase.MIGRATION_9_10,
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

    @Provides
    fun provideDebtDao(db: NotePayDatabase): com.notepay.data.local.dao.DebtDao = db.debtDao()

    @Provides
    fun provideSyncOutboxDao(db: NotePayDatabase): SyncOutboxDao = db.syncOutboxDao()
}
