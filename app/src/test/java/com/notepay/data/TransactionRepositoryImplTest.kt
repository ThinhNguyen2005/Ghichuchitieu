package com.notepay.data

import com.google.common.truth.Truth.assertThat
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.mapper.TransactionMapper
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.data.repository.TransactionRepositoryImpl
import com.notepay.domain.model.Category
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class TransactionRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private lateinit var categoryRepository: CategoryRepositoryImpl
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var fakeDao: FakeTransactionDao

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        
        // Reset SharedPreferences
        val prefs = context.getSharedPreferences("notepay_custom_categories", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        
        Category.registerCustomCategories(emptyList())

        categoryRepository = CategoryRepositoryImpl(context)
        fakeDao = FakeTransactionDao()
        transactionRepository = TransactionRepositoryImpl(
            dao = fakeDao,
            mapper = TransactionMapper(),
            categoryRepository = categoryRepository,
            dispatcher = mainDispatcherRule.testDispatcher
        )
    }

    @Test
    fun testCategoryRenameUpdatesTransactionFlow() = runTest {
        // 1. Add custom category
        val customCat = Category(
            id = "CUSTOM_1",
            displayName = "Di chuyển cũ",
            colorArgb = 0xFF64B5F6L,
            isIncome = false,
            isCustom = true
        )
        categoryRepository.addCustomCategory(customCat)

        // 2. Set transaction in fake DAO
        val tx = TransactionEntity(
            id = 10L,
            amountCents = 10000L,
            type = "EXPENSE",
            category = "CUSTOM_1",
            note = "Xe om",
            occurredAt = 1000L,
            walletId = 1L,
            createdAt = 1000L
        )
        fakeDao.emit(listOf(tx))

        // 3. Collect transaction flow
        val emittedList = mutableListOf<List<com.notepay.domain.model.Transaction>>()
        val collectJob = launch(mainDispatcherRule.testDispatcher) {
            transactionRepository.observeAll().collect {
                emittedList.add(it)
            }
        }

        // Verify initial state
        assertThat(emittedList).isNotEmpty()
        assertThat(emittedList.last()).hasSize(1)
        assertThat(emittedList.last()[0].category.displayName).isEqualTo("Di chuyển cũ")

        // 4. Edit custom category (rename to "Di chuyển mới")
        val updatedCat = customCat.copy(displayName = "Di chuyển mới")
        categoryRepository.addCustomCategory(updatedCat)

        // Verify the flow emitted a new list and it has the updated display name!
        assertThat(emittedList).hasSize(2)
        assertThat(emittedList.last()[0].category.displayName).isEqualTo("Di chuyển mới")

        collectJob.cancel()
    }

    private class FakeTransactionDao : TransactionDao {
        private val flow = MutableStateFlow<List<TransactionEntity>>(emptyList())

        fun emit(list: List<TransactionEntity>) {
            flow.value = list
        }

        override fun observeAll(): Flow<List<TransactionEntity>> = flow
        override fun observeByRange(startMillis: Long, endMillis: Long): Flow<List<TransactionEntity>> = flow
        override fun observeByWallet(walletId: Long): Flow<List<TransactionEntity>> = flow
        override suspend fun getById(id: Long): TransactionEntity? = flow.value.find { it.id == id }
        override suspend fun upsert(entity: TransactionEntity): Long = 0L
        override suspend fun delete(id: Long) = Unit
    }
}
