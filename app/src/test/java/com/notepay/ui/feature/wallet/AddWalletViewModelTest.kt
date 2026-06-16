package com.notepay.ui.feature.wallet

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.launch
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.feedback.FeedbackType
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWalletViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(UnconfinedTestDispatcher())

    private val fakeWalletRepository = FakeWalletRepository()

    @Test
    fun `initial state has default values`() {
        val viewModel = AddWalletViewModel(fakeWalletRepository, SavedStateHandle())
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
        val viewModel = AddWalletViewModel(fakeWalletRepository, SavedStateHandle())

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
        val viewModel = AddWalletViewModel(fakeWalletRepository, SavedStateHandle())
        val feedbacks = mutableListOf<UiFeedback>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks.add(it) }
        }

        viewModel.onNameChanged("Ví Tiết Kiệm")
        viewModel.onInitialBalanceChanged("1000")
        viewModel.onHasBudgetLimitChanged(true)
        viewModel.onBudgetLimitChanged("5000")
        viewModel.onIconChanged("card")
        viewModel.onColorChanged("tertiary")

        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertThat(feedbacks.any { it.type == FeedbackType.Success }).isTrue()
        assertThat(fakeWalletRepository.savedWallets).hasSize(1)
        
        val saved = fakeWalletRepository.savedWallets.first()
        assertThat(saved.name).isEqualTo("Ví Tiết Kiệm")
        assertThat(saved.initialBalance).isEqualTo(Money(1000L * 100))
        assertThat(saved.budgetLimit).isEqualTo(Money(5000L * 100))
        assertThat(saved.iconKey).isEqualTo("card")
        assertThat(saved.colorKey).isEqualTo("tertiary")
        assertThat(saved.isActive).isFalse()

        collectJob.cancel()
    }

    @Test
    fun `save without budget limit inserts wallet with null budgetLimit`() = runTest {
        val viewModel = AddWalletViewModel(fakeWalletRepository, SavedStateHandle())
        val feedbacks = mutableListOf<UiFeedback>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks.add(it) }
        }

        viewModel.onNameChanged("Quỹ Đen")
        viewModel.onInitialBalanceChanged("2000")
        viewModel.onHasBudgetLimitChanged(false)
        viewModel.onBudgetLimitChanged("5000") // Should be ignored since hasBudgetLimit is false

        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertThat(feedbacks.any { it.type == FeedbackType.Success }).isTrue()
        assertThat(fakeWalletRepository.savedWallets).hasSize(1)
        
        val saved = fakeWalletRepository.savedWallets.first()
        assertThat(saved.name).isEqualTo("Quỹ Đen")
        assertThat(saved.initialBalance).isEqualTo(Money(2000L * 100))
        assertThat(saved.budgetLimit).isNull()
        assertThat(saved.isActive).isFalse()

        collectJob.cancel()
    }

    @Test
    fun `save failure updates error state`() = runTest {
        val repositoryWithFailure = FakeWalletRepository(throwOnSave = true)
        val viewModel = AddWalletViewModel(repositoryWithFailure, SavedStateHandle())
        val feedbacks = mutableListOf<UiFeedback>()
        val collectJob = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.feedback.collect { feedbacks.add(it) }
        }

        viewModel.onNameChanged("Ví lỗi")
        viewModel.save()
        testScheduler.advanceUntilIdle()

        assertThat(feedbacks.any { it.type == FeedbackType.Error }).isTrue()
        assertThat(feedbacks.first { it.type == FeedbackType.Error }.message).isEqualTo("Không thể tạo ví")
        val state = viewModel.state.value
        assertThat(state.isSaving).isFalse()
        assertThat(state.error).isEqualTo("Không thể tạo ví")

        collectJob.cancel()
    }

    @Test
    fun `initial state in edit mode loads existing wallet data`() = runTest {
        val wallet = Wallet(
            id = 42L,
            name = "Ví Cũ",
            initialBalance = Money(1500_00),
            iconKey = "bank",
            colorKey = "secondary",
            budgetLimit = Money(5000_00)
        )
        fakeWalletRepository.savedWallets.add(wallet)

        val savedStateHandle = SavedStateHandle(mapOf("id" to 42L))
        val viewModel = AddWalletViewModel(fakeWalletRepository, savedStateHandle)
        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.name).isEqualTo("Ví Cũ")
        assertThat(state.initialBalanceInput).isEqualTo("1500")
        assertThat(state.hasBudgetLimit).isTrue()
        assertThat(state.budgetLimitInput).isEqualTo("5000")
        assertThat(state.iconKey).isEqualTo("bank")
        assertThat(state.colorKey).isEqualTo("secondary")
        assertThat(state.isEditMode).isTrue()
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
