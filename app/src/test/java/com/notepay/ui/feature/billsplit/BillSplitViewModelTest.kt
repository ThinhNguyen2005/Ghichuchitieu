package com.notepay.ui.feature.billsplit

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class BillSplitViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val wallet1 = Wallet(
        id = 1L,
        name = "Ví 1",
        initialBalance = Money.ZERO,
        iconKey = "wallet",
        colorKey = "primary",
        isActive = true,
        bankBin = "970423",
        accountNumber = "123456",
        accountName = "ACCOUNT 1"
    )

    private val wallet2 = Wallet(
        id = 2L,
        name = "Ví 2",
        initialBalance = Money.ZERO,
        iconKey = "wallet",
        colorKey = "secondary",
        isActive = false
    )

    private val parentTx = Transaction(
        id = 15L,
        amount = Money(90_000_00L),
        type = TransactionType.EXPENSE,
        category = Category.DEFAULT_EXPENSE,
        note = "Ăn tối",
        occurredAt = Clock.System.now(),
        walletId = 1L
    )

    private val unpaidSplit = BillSplit(
        id = 10L,
        transactionId = 15L,
        debtorName = "Ban A",
        amount = Money(20_000_00L),
        isPaid = false,
        memoCode = "NP15 BAN A",
        createdAt = Clock.System.now()
    )

    @Test
    fun `state combines data correctly`() = runTest {
        val viewModel = createViewModel(
            unpaidSplits = listOf(unpaidSplit),
            paidSplits = emptyList(),
            transactions = listOf(parentTx),
            wallets = listOf(wallet1, wallet2)
        )

        // Launch state flow collection to trigger stateIn active collection
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.unpaidSplits).hasSize(1)
        assertThat(state.unpaidSplits.first().split).isEqualTo(unpaidSplit)
        assertThat(state.unpaidSplits.first().parentTransaction).isEqualTo(parentTx)
        assertThat(state.unpaidSplits.first().wallet).isEqualTo(wallet1)
        assertThat(state.unpaidSplits.first().qrCodeString).isNotNull()
        assertThat(state.activeWallet).isEqualTo(wallet1)
        assertThat(state.wallets).containsExactly(wallet1, wallet2)

        collectJob.cancel()
    }

    @Test
    fun `createBillSplits generates correct splits`() = runTest {
        val billSplitRepository = FakeBillSplitRepository()
        val viewModel = createViewModel(
            billSplitRepository = billSplitRepository
        )

        viewModel.createBillSplits(
            transactionId = 15L,
            splits = listOf("Ban A" to 20_000_00L, "Ban B" to 30_000_00L)
        )
        testScheduler.advanceUntilIdle()

        val saved = billSplitRepository.savedSplits
        assertThat(saved).hasSize(2)
        assertThat(saved[0].debtorName).isEqualTo("Ban A")
        assertThat(saved[0].amount).isEqualTo(Money(20_000_00L))
        assertThat(saved[0].memoCode).isEqualTo("NP15 BAN A")
        
        assertThat(saved[1].debtorName).isEqualTo("Ban B")
        assertThat(saved[1].amount).isEqualTo(Money(30_000_00L))
        assertThat(saved[1].memoCode).isEqualTo("NP15 BAN B")
    }

    @Test
    fun `markAsPaidManually marks paid and records income`() = runTest {
        val billSplitRepository = FakeBillSplitRepository(listOf(unpaidSplit))
        val transactionRepository = FakeTransactionRepository(listOf(parentTx))
        val viewModel = createViewModel(
            billSplitRepository = billSplitRepository,
            transactionRepository = transactionRepository,
            wallets = listOf(wallet1)
        )

        viewModel.markAsPaidManually(10L)
        testScheduler.advanceUntilIdle()

        // Verify split is marked paid
        assertThat(billSplitRepository.markedPaidIds).containsExactly(10L)

        // Verify income transaction was added
        assertThat(transactionRepository.savedTransactions).hasSize(2) // parentTx + incomeTx
        val income = transactionRepository.savedTransactions.last()
        assertThat(income.type).isEqualTo(TransactionType.INCOME)
        assertThat(income.amount).isEqualTo(unpaidSplit.amount)
        assertThat(income.note).isEqualTo("Ban A trả tiền: Ăn tối")
        assertThat(income.walletId).isEqualTo(parentTx.walletId)
    }

    @Test
    fun `markDebtorAsPaid marks all specified splits as paid and records income for each`() = runTest {
        val split1 = unpaidSplit.copy(id = 10L, amount = Money(20_000_00L))
        val split2 = unpaidSplit.copy(id = 20L, amount = Money(30_000_00L))
        val billSplitRepository = FakeBillSplitRepository(listOf(split1, split2))
        val transactionRepository = FakeTransactionRepository(listOf(parentTx))
        val viewModel = createViewModel(
            billSplitRepository = billSplitRepository,
            transactionRepository = transactionRepository,
            wallets = listOf(wallet1)
        )

        viewModel.markDebtorAsPaid("Ban A", listOf(10L, 20L))
        testScheduler.advanceUntilIdle()

        // Verify both marked paid
        assertThat(billSplitRepository.markedPaidIds).containsExactly(10L, 20L)

        // Verify income transactions added
        val saved = transactionRepository.savedTransactions.filter { it.type == TransactionType.INCOME }
        assertThat(saved).hasSize(2)
        assertThat(saved[0].amount).isEqualTo(Money(20_000_00L))
        assertThat(saved[0].note).isEqualTo("Ban A trả tiền: Ăn tối")
        assertThat(saved[1].amount).isEqualTo(Money(30_000_00L))
        assertThat(saved[1].note).isEqualTo("Ban A trả tiền: Ăn tối")
    }

    private fun createViewModel(
        unpaidSplits: List<BillSplit> = emptyList(),
        paidSplits: List<BillSplit> = emptyList(),
        transactions: List<Transaction> = emptyList(),
        wallets: List<Wallet> = emptyList(),
        billSplitRepository: FakeBillSplitRepository = FakeBillSplitRepository(unpaidSplits + paidSplits),
        transactionRepository: FakeTransactionRepository = FakeTransactionRepository(transactions),
        walletRepository: FakeWalletRepository = FakeWalletRepository(wallets, wallets.find { it.isActive })
    ): BillSplitViewModel {
        val dispatcher = mainDispatcherRule.testDispatcher
        val addTransaction = AddTransactionUseCase(transactionRepository, walletRepository, dispatcher)
        return BillSplitViewModel(
            billSplitRepository = billSplitRepository,
            transactionRepository = transactionRepository,
            walletRepository = walletRepository,
            addTransaction = addTransaction
        )
    }
}

private class FakeBillSplitRepository(
    initialSplits: List<BillSplit> = emptyList()
) : BillSplitRepository {
    val savedSplits = initialSplits.toMutableList()
    val markedPaidIds = mutableListOf<Long>()

    override fun observeAll(): Flow<List<BillSplit>> = flowOf(savedSplits)
    override fun observeByTransaction(transactionId: Long): Flow<List<BillSplit>> = 
        flowOf(savedSplits.filter { it.transactionId == transactionId })
    override fun observeUnpaid(): Flow<List<BillSplit>> = flowOf(savedSplits.filter { !it.isPaid })
    override fun observePaid(): Flow<List<BillSplit>> = flowOf(savedSplits.filter { it.isPaid })
    override suspend fun getByMemoCode(memoCode: String): BillSplit? = 
        savedSplits.firstOrNull { it.memoCode == memoCode }
    override suspend fun getById(id: Long): BillSplit? = savedSplits.firstOrNull { it.id == id }
    override suspend fun upsert(billSplit: BillSplit): Long {
        savedSplits.add(billSplit)
        return billSplit.id
    }
    override suspend fun upsertAll(splits: List<BillSplit>) {
        savedSplits.addAll(splits)
    }
    override suspend fun markAsPaid(id: Long, paidAt: kotlinx.datetime.Instant) {
        markedPaidIds.add(id)
        val idx = savedSplits.indexOfFirst { it.id == id }
        if (idx != -1) {
            savedSplits[idx] = savedSplits[idx].copy(isPaid = true, paidAt = paidAt)
        }
    }
    override suspend fun delete(id: Long) {
        savedSplits.removeAll { it.id == id }
    }
}

private class FakeWalletRepository(
    initialWallets: List<Wallet>,
    private var activeWallet: Wallet?
) : WalletRepository {
    private val walletFlow = MutableStateFlow(initialWallets)
    override fun observeAll(): Flow<List<Wallet>> = walletFlow
    override fun observeActive(): Flow<Wallet?> = flowOf(activeWallet)
    override suspend fun getById(id: Long): Wallet? = walletFlow.value.find { it.id == id }
    override suspend fun upsert(wallet: Wallet): Long {
        val list = walletFlow.value.toMutableList()
        list.removeAll { it.id == wallet.id }
        list.add(wallet)
        walletFlow.value = list
        return wallet.id
    }
    override suspend fun delete(id: Long) = Unit
    override suspend fun setActive(id: Long) {
        activeWallet = walletFlow.value.find { it.id == id }
    }
}

private class FakeTransactionRepository(
    initialTransactions: List<Transaction> = emptyList()
) : TransactionRepository {
    val savedTransactions = initialTransactions.toMutableList()
    override fun observeAll(): Flow<List<Transaction>> = flowOf(savedTransactions)
    override fun observeByWallet(walletId: Long): Flow<List<Transaction>> = flowOf(savedTransactions.filter { it.walletId == walletId })
    override fun observeByMonth(year: Int, month: Int): Flow<List<Transaction>> = flowOf(savedTransactions)
    override suspend fun getById(id: Long): Transaction? = savedTransactions.find { it.id == id }
    override suspend fun upsert(transaction: Transaction): Long {
        savedTransactions.add(transaction)
        return transaction.id
    }
    override suspend fun delete(id: Long) = Unit
}
