package com.notepay.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.entity.WalletEntity
import com.notepay.domain.model.Wallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Seed data khi DB tạo lần đầu:
 *  - 1 ví mặc định "Tiền mặt"
 *
 * Phase 1: chỉ ví. Không seed transaction mẫu để tránh data giả.
 */
@Singleton
class SeedCallback @Inject constructor(
    private val walletDaoProvider: Provider<WalletDao>,
) : RoomDatabase.Callback() {

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        CoroutineScope(Dispatchers.IO).launch {
            val wallet = Wallet.default()
            val entity = WalletEntity(
                id = 0L,
                name = wallet.name,
                initialBalanceCents = wallet.initialBalance.amountInCents,
                iconKey = wallet.iconKey,
                colorKey = wallet.colorKey,
                isActive = wallet.isActive,
                createdAt = System.currentTimeMillis(),
            )
            val id = walletDaoProvider.get().upsert(entity)
            walletDaoProvider.get().setActiveExclusive(id)
        }
    }
}
