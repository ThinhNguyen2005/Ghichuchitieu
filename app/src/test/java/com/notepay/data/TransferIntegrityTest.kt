package com.notepay.data

import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.mapper.TransactionMapper
import com.notepay.data.mapper.WalletMapper
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.data.repository.TransactionRepositoryImpl
import com.notepay.data.repository.WalletRepositoryImpl
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TransferIntegrityTest {
    private lateinit var database: NotePayDatabase
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var wallets: WalletRepositoryImpl

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, NotePayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactions = TransactionRepositoryImpl(
            dao = database.transactionDao(),
            mapper = TransactionMapper(),
            database = database,
            categoryRepository = CategoryRepositoryImpl(context),
            context = context,
            dispatcher = Dispatchers.IO,
        )
        wallets = WalletRepositoryImpl(
            dao = database.walletDao(),
            transactionDao = database.transactionDao(),
            database = database,
            mapper = WalletMapper(),
            dispatcher = Dispatchers.IO,
        )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun seedWallets() {
        for (id in 1L..2L) {
            database.walletDao().upsert(WalletEntity(
                id = id,
                name = "Wallet $id",
                initialBalanceCents = 0,
                iconKey = "cash",
                colorKey = "primary",
                createdAt = 1_000,
            ))
        }
    }

    private fun transferPair(groupId: String = "transfer-1"): Pair<Transaction, Transaction> {
        val outgoing = Transaction(
            amount = Money(12_345),
            type = TransactionType.EXPENSE,
            category = Category.OTHER,
            note = "Send",
            occurredAt = Instant.fromEpochMilliseconds(1_000),
            walletId = 1,
            isInternalTransfer = true,
            transferGroupId = groupId,
        )
        return outgoing to outgoing.copy(
            type = TransactionType.INCOME,
            note = "Receive",
            walletId = 2,
        )
    }

    @Test
    fun deletingOneTransferLegRemovesBothAndProtectsWalletUntilDeletion() = runTest {
        seedWallets()
        val (outgoing, incoming) = transferPair()
        transactions.upsertTransferPair(outgoing, incoming)

        assertThat(database.transactionDao().getByTransferGroupId("transfer-1")).hasSize(2)
        assertFailsWith<IllegalArgumentException> { wallets.delete(1) }

        val id = database.transactionDao().getByTransferGroupId("transfer-1").first().id
        database.transactionDao().delete(id)
        assertThat(database.transactionDao().getByTransferGroupId("transfer-1")).hasSize(2)
        assertFailsWith<IllegalArgumentException> {
            transactions.upsert(outgoing.copy(id = id, note = "Edited"))
        }
        transactions.delete(id)

        assertThat(database.transactionDao().getByTransferGroupId("transfer-1")).isEmpty()
        wallets.delete(1)
        assertThat(database.walletDao().getById(1)).isNull()
    }

    @Test
    fun failedSecondLegInsertionRollsBackTheFirstLeg() = runTest {
        seedWallets()
        val (outgoing, incoming) = transferPair()

        assertFailsWith<Exception> {
            transactions.upsertTransferPair(outgoing, incoming.copy(walletId = 999))
        }
        assertThat(database.transactionDao().getAll()).isEmpty()
    }

    @Test
    fun repositoryRejectsInvalidTransferAmount() = runTest {
        seedWallets()
        val (outgoing, incoming) = transferPair()

        assertFailsWith<IllegalArgumentException> {
            transactions.upsertTransferPair(
                outgoing.copy(amount = Money.ZERO),
                incoming.copy(amount = Money.ZERO),
            )
        }
        assertThat(database.transactionDao().getAll()).isEmpty()
    }

    @Test
    fun unlinkedLegacyTransferCannotBeDeletedOrOrphanedByWalletDeletion() = runTest {
        seedWallets()
        val id = database.transactionDao().upsert(TransactionEntity(
            amountCents = 12_345,
            type = "EXPENSE",
            category = "OTHER",
            note = "Old transfer",
            occurredAt = 1_000,
            walletId = 1,
            createdAt = 1_000,
            isInternalTransfer = true,
        ))

        assertFailsWith<IllegalArgumentException> { transactions.delete(id) }
        assertFailsWith<IllegalArgumentException> { wallets.delete(1) }
        assertThat(database.transactionDao().getById(id)).isNotNull()
    }
}
