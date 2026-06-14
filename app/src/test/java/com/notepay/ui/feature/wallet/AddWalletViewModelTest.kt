package com.notepay.ui.feature.wallet

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Money
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.ui.feature.addtransaction.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWalletViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeWalletRepository = FakeWalletRepository()

    @Test
    fun `initial state has default values`() {
        val viewModel = AddWalletViewModel(fakeWalletRepository)
        val state = viewModel.state.value

        assertThat(state.name).isEmpty()
        assertThat(state.initialBalanceInput).isEmpty()
        assertThat(state.hasBudgetLimit).isFalse()
        assertThat(state.budgetLimitInput).isEmpty()
        assertThat(state.iconKey).isEqualTo("cash")
        assertThat(state.colorKey).isEqualTo("primary")
        assertThat(state.isSaving).isFalse()
        assertThat(state.error).isNull()
        assertThat(state.canSave).isFalse()
    }

    @Test
    fun `input changes update UI state`() {
        val viewModel = AddWalletViewModel(fakeWalletRepository)

        viewModel.onNameChanged("Ví chi tiêu")
        viewModel.onInitialBalanceChanged("500000")
        viewModel.onHasBudgetLimitChanged(true)
        viewModel.onBudgetLimitChanged("2000000")
        viewModel.onIconChanged("bank")
        viewModel.onColorChanged("secondary")

        val state = viewModel.state.value
        assertThat(state.name).isEqualTo("Ví chi tiêu")
        assertThat(state.initialBalanceInput).isEqualTo("500000")
        assertThat(state.hasBudgetLimit).isTrue()
        assertThat(state.budgetLimitInput).isEqualTo("2000000")
        assertThat(state.iconKey).isEqualTo("bank")
        assertThat(state.colorKey).isEqualTo("secondary")
        assertThat(state.canSave).isTrue()
    }

    @Test
    fun `save with budget limit inserts correct wallet`() = runTest {
        val viewModel = AddWalletViewModel(fakeWalletRepository)
        var successCalled = false

        viewModel.onNameChanged("Ví Tiết Kiệm")
        viewModel.onInitialBalanceChanged("1000")
        viewModel.onHasBudgetLimitChanged(true)
        viewModel.onBudgetLimitChanged("5000")
        viewModel.onIconChanged("card")
        viewModel.onColorChanged("tertiary")

        viewModel.save {
            successCalled = true
        }

        assertThat(successCalled).isTrue()
        assertThat(fakeWalletRepository.savedWallets).hasSize(1)
        
        val saved = fakeWalletRepository.savedWallets.first()
        assertThat(saved.name).isEqualTo("Ví Tiết Kiệm")
        assertThat(saved.initialBalance).isEqualTo(Money(1000L * 100))
        assertThat(saved.budgetLimit).isEqualTo(Money(5000L * 100))
        assertThat(saved.iconKey).isEqualTo("card")
        assertThat(saved.colorKey).isEqualTo("tertiary")
        assertThat(saved.isActive).isFalse()
    }

    @Test
    fun `save without budget limit inserts wallet with null budgetLimit`() = runTest {
        val viewModel = AddWalletViewModel(fakeWalletRepository)
        var successCalled = false

        viewModel.onNameChanged("Quỹ Đen")
        viewModel.onInitialBalanceChanged("2000")
        viewModel.onHasBudgetLimitChanged(false)
        viewModel.onBudgetLimitChanged("5000") // Should be ignored since hasBudgetLimit is false

        viewModel.save {
            successCalled = true
        }

        assertThat(successCalled).isTrue()
        assertThat(fakeWalletRepository.savedWallets).hasSize(1)
        
        val saved = fakeWalletRepository.savedWallets.first()
        assertThat(saved.name).isEqualTo("Quỹ Đen")
        assertThat(saved.initialBalance).isEqualTo(Money(2000L * 100))
        assertThat(saved.budgetLimit).isNull()
        assertThat(saved.isActive).isFalse()
    }

    @Test
    fun `save failure updates error state`() = runTest {
        val repositoryWithFailure = FakeWalletRepository(throwOnSave = true)
        val viewModel = AddWalletViewModel(repositoryWithFailure)
        var successCalled = false

        viewModel.onNameChanged("Ví lỗi")
        viewModel.save {
            successCalled = true
        }

        assertThat(successCalled).isFalse()
        val state = viewModel.state.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.error).isEqualTo("Database error")
    }

    private class FakeWalletRepository(
        private val throwOnSave: Boolean = false
    ) : WalletRepository {
        val savedWallets = mutableListOf<Wallet>()

        override fun observeAll(): Flow<List<Wallet>> = flowOf(savedWallets)
        override fun observeActive(): Flow<Wallet?> = flowOf(null)
        override suspend fun getById(id: Long): Wallet? = savedWallets.firstOrNull { it.id == id }
        
        override suspend fun upsert(wallet: Wallet): Long {
            if (throwOnSave) {
                throw IllegalStateException("Database error")
            }
            savedWallets.add(wallet)
            return wallet.id
        }

        override suspend fun delete(id: Long) = Unit
        override suspend fun setActive(id: Long) = Unit
    }
}
