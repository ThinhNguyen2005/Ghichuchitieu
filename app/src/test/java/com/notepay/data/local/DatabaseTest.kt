package com.notepay.data.local

import android.content.Context
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DatabaseTest {

    private lateinit var db: NotePayDatabase
    private lateinit var walletDao: WalletDao
    private lateinit var transactionDao: TransactionDao

    @Before
    fun createDb() {
        val context = RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, NotePayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        walletDao = db.walletDao()
        transactionDao = db.transactionDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndQueryWallets() = runTest {
        val w1 = WalletEntity(
            name = "Ví chính",
            initialBalanceCents = 0L,
            iconKey = "cash",
            colorKey = "primary",
            isActive = true,
            createdAt = 1000L
        )

        val id1 = walletDao.upsert(w1)
        val wallets = walletDao.observeAll().first()

        assertThat(wallets).hasSize(1)
        assertThat(wallets[0].id).isEqualTo(id1)
        assertThat(wallets[0].name).isEqualTo("Ví chính")
    }

    @Test
    fun insertMultipleInactiveWallets() = runTest {
        // Hãy test xem unique index wallets.is_active có gây lỗi khi chèn 2 ví cùng inactive (is_active = false) hay không.
        val w1 = WalletEntity(
            name = "Ví 1",
            initialBalanceCents = 0L,
            iconKey = "cash",
            colorKey = "primary",
            isActive = false,
            createdAt = 1000L
        )
        val w2 = WalletEntity(
            name = "Ví 2",
            initialBalanceCents = 0L,
            iconKey = "bank",
            colorKey = "secondary",
            isActive = false,
            createdAt = 2000L
        )

        walletDao.upsert(w1)
        // Nếu có unique index không có partial WHERE, câu lệnh tiếp theo sẽ quăng lỗi SQLiteConstraintException
        walletDao.upsert(w2)

        val wallets = walletDao.observeAll().first()
        assertThat(wallets).hasSize(2)
    }

    @Test
    fun walletExclusiveActiveSwitch() = runTest {
        val w1 = WalletEntity(id = 1L, name = "Ví 1", initialBalanceCents = 0L, iconKey = "cash", colorKey = "primary", isActive = true, createdAt = 1000L)
        val w2 = WalletEntity(id = 2L, name = "Ví 2", initialBalanceCents = 0L, iconKey = "bank", colorKey = "secondary", isActive = false, createdAt = 2000L)

        walletDao.upsert(w1)
        walletDao.upsert(w2)

        walletDao.setActiveExclusive(2L)

        val active = walletDao.observeActive().first()
        assertThat(active?.id).isEqualTo(2L)

        val wallets = walletDao.observeAll().first()
        assertThat(wallets.first { it.id == 1L }.isActive).isFalse()
        assertThat(wallets.first { it.id == 2L }.isActive).isTrue()
    }

    @Test
    fun insertAndObserveTransactions() = runTest {
        val w1 = WalletEntity(
            id = 1L,
            name = "Ví chính",
            initialBalanceCents = 0L,
            iconKey = "cash",
            colorKey = "primary",
            isActive = true,
            createdAt = 1000L
        )
        walletDao.upsert(w1)

        val t1 = TransactionEntity(
            id = 15L,
            amountCents = 5000000L,
            type = "EXPENSE",
            category = "OTHER",
            note = "Mua sach",
            occurredAt = 1000L,
            walletId = 1L,
            createdAt = 1000L,
            isAutoCapture = false
        )
        transactionDao.upsert(t1)

        val initialTxs = transactionDao.observeAll().first()
        assertThat(initialTxs).hasSize(1)
        assertThat(initialTxs[0].category).isEqualTo("OTHER")

        // Update the category to FOOD
        val updatedT1 = t1.copy(category = "FOOD")
        transactionDao.upsert(updatedT1)

        val updatedTxs = transactionDao.observeAll().first()
        assertThat(updatedTxs).hasSize(1)
        assertThat(updatedTxs[0].category).isEqualTo("FOOD")
    }
}
