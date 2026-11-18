package com.notepay.data.backup

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.google.common.truth.Truth.assertThat
import com.notepay.data.datastore.BudgetSettings
import com.notepay.data.datastore.BudgetSettingsStore
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.domain.model.Category
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupExportImportTest {

    private lateinit var context: Context
    private lateinit var database: NotePayDatabase
    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var exporter: DataExporter
    private lateinit var importer: DataImporter

    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, NotePayDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        categoryRepository = CategoryRepositoryImpl(context)
        val budgetSettingsStore = mockk<BudgetSettingsStore> {
            every { settings } returns flowOf(BudgetSettings())
        }

        exporter = DataExporter(
            transactionDao = database.transactionDao(),
            walletDao = database.walletDao(),
            billSplitDao = database.billSplitDao(),
            subscriptionDao = database.subscriptionDao(),
            categoryRepository = categoryRepository,
            budgetSettingsStore = budgetSettingsStore,
            context = context,
        )
        importer = DataImporter(
            database = database,
            transactionDao = database.transactionDao(),
            walletDao = database.walletDao(),
            billSplitDao = database.billSplitDao(),
            subscriptionDao = database.subscriptionDao(),
            categoryRepository = categoryRepository,
            budgetSettingsStore = budgetSettingsStore,
            context = context,
        )
    }

    @After
    fun tearDown() {
        runBlocking { categoryRepository.replaceCustomCategories(emptyList()) }
        database.close()
    }

    @Test
    fun `exportToJson and importFromJson round-trip preserves wallets transactions billSplits subscriptions and custom categories`() = runTest {
        val wallet = WalletEntity(
            id = 1L,
            name = "Ví chính",
            initialBalanceCents = 1_000_000_00L,
            iconKey = "cash",
            colorKey = "primary",
            isActive = true,
            budgetLimitCents = 5_000_000_00L,
            createdAt = 1_000L,
        )
        val tx = TransactionEntity(
            id = 10L,
            amountCents = 120_000_00L,
            type = "EXPENSE",
            category = "FOOD",
            note = "Lẩu cuối tuần",
            occurredAt = 2_000L,
            walletId = 1L,
            createdAt = 2_000L,
            isAutoCapture = true,
            isInternalTransfer = true,
            transferGroupId = "grp-backup-1",
        )
        val split = BillSplitEntity(
            id = 20L,
            transactionId = 10L,
            debtorName = "Minh",
            amountCents = 60_000_00L,
            isPaid = false,
            memoCode = "NP10 MINH",
            createdAt = 2_500L,
        )
        val sub = SubscriptionEntity(
            id = 30L,
            name = "Spotify",
            amountCents = 59_000_00L,
            category = "ENTERTAINMENT",
            nextDueDate = 9_000L,
            repeatMonths = 1,
            remindDaysBefore = 2,
            note = "Gói tháng",
            isActive = true,
            createdAt = 3_000L,
        )
        val customCat = Category(
            id = "custom_pet_1",
            displayName = "Thú cưng",
            colorArgb = 0xFFE57373L,
            isIncome = false,
            isCustom = true,
            iconId = "pets",
        )

        database.walletDao().upsert(wallet)
        database.transactionDao().upsert(tx)
        database.billSplitDao().upsert(split)
        database.subscriptionDao().upsert(sub)
        categoryRepository.addCustomCategory(customCat)

        val json = exporter.exportToJson()

        // Xóa sạch DB + custom categories trước khi restore.
        database.withTransaction {
            database.billSplitDao().deleteAll()
            database.transactionDao().deleteAll()
            database.subscriptionDao().deleteAll()
            database.walletDao().deleteAll()
        }
        categoryRepository.replaceCustomCategories(emptyList())

        importer.importFromJson(json)

        assertThat(database.walletDao().getAll()).containsExactly(wallet)
        assertThat(database.transactionDao().getAll()).containsExactly(tx)
        assertThat(database.billSplitDao().getAll()).containsExactly(split)
        assertThat(database.subscriptionDao().getAll()).containsExactly(sub)
        assertThat(categoryRepository.getCategories().filter { it.isCustom }).containsExactly(customCat)
    }

    @Test
    fun `importFromJson rejects incompatible version or invalid transaction amount without wiping existing data`() = runTest {
        val existingWallet = WalletEntity(
            id = 99L,
            name = "Ví an toàn",
            initialBalanceCents = 500_000_00L,
            iconKey = "wallet",
            colorKey = "primary",
            isActive = true,
            createdAt = 1_000L,
        )
        database.walletDao().upsert(existingWallet)

        val futureVersionJson = """{"version": 999, "data": {"wallets": [], "transactions": []}}"""
        val negativeAmountJson = """
            {
              "version": 1,
              "data": {
                "wallets": [{"id": 1, "name": "W", "initialBalanceCents": 0, "iconKey": "cash", "colorKey": "primary", "isActive": true, "createdAt": 1000}],
                "transactions": [{"id": 1, "amountCents": -50, "type": "EXPENSE", "category": "FOOD", "note": "", "occurredAt": 1000, "walletId": 1, "createdAt": 1000}]
              }
            }
        """.trimIndent()

        listOf(futureVersionJson, negativeAmountJson).forEach { invalidPayload ->
            assertFailsWith<IllegalArgumentException> {
                importer.importFromJson(invalidPayload)
            }
        }

        assertThat(database.walletDao().getAll()).containsExactly(existingWallet)
    }
}
