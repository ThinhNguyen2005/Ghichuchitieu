package com.notepay.feature.assets.main.sheet

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.data.remote.VietQrBankRepository
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletStats
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddWalletViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val walletRepository: WalletRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val vietQrBankRepository: VietQrBankRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `switching account nature updates wallet type and default icon`() = runTest {
        coEvery { walletRepository.observeAll() } returns flowOf(emptyList())
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()

        val viewModel = AddWalletViewModel(
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            savedStateHandle = SavedStateHandle(),
            vietQrBankRepository = vietQrBankRepository,
            context = context,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.accountNature).isEqualTo(AccountNature.ASSET)
        assertThat(viewModel.state.value.walletType).isEqualTo(WalletType.CASH)

        viewModel.onNatureChanged(AccountNature.LIABILITY)

        assertThat(viewModel.state.value.accountNature).isEqualTo(AccountNature.LIABILITY)
        assertThat(viewModel.state.value.walletType).isEqualTo(WalletType.CREDIT_CARD)
        assertThat(viewModel.state.value.iconKey).isEqualTo("card")
    }

    @Test
    fun `currency selection updates in create mode`() = runTest {
        coEvery { walletRepository.observeAll() } returns flowOf(emptyList())
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()

        val viewModel = AddWalletViewModel(
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            savedStateHandle = SavedStateHandle(),
            vietQrBankRepository = vietQrBankRepository,
            context = context,
        )
        advanceUntilIdle()

        viewModel.onCurrencyChanged("USD")
        assertThat(viewModel.state.value.currencyCode).isEqualTo("USD")
    }

    @Test
    fun `currency is locked when wallet has transactions`() = runTest {
        val existingWallet = Wallet(
            id = 42L,
            name = "Thẻ Techcombank",
            initialBalance = Money(1_000_000_00L),
            iconKey = "card",
            colorKey = "primary",
            type = WalletType.BANK,
            currencyCode = "VND",
        )
        val stats = WalletStats(
            walletId = 42L,
            currentMonthIncome = Money.ZERO,
            currentMonthExpense = Money.ZERO,
            allTimeIncome = Money.ZERO,
            allTimeExpense = Money.ZERO,
            txCount = 15,
        )

        coEvery { walletRepository.observeAll() } returns flowOf(listOf(existingWallet))
        coEvery { walletRepository.getById(42L) } returns existingWallet
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(listOf(stats))
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()

        val viewModel = AddWalletViewModel(
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            savedStateHandle = SavedStateHandle(mapOf("id" to 42L)),
            vietQrBankRepository = vietQrBankRepository,
            context = context,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isEditMode).isTrue()
        assertThat(state.transactionCount).isEqualTo(15)
        assertThat(state.isCurrencyLocked).isTrue()
        assertThat(state.canDeleteDirectly).isFalse()
        assertThat(state.canArchive).isTrue()

        viewModel.onCurrencyChanged("USD")
        assertThat(viewModel.state.value.currencyCode).isEqualTo("VND")
    }

    @Test
    fun `wallet with zero transactions allows direct delete`() = runTest {
        val emptyWallet = Wallet(
            id = 100L,
            name = "Ví mới tạo",
            initialBalance = Money.ZERO,
            iconKey = "cash",
            colorKey = "primary",
            type = WalletType.CASH,
            currencyCode = "VND",
        )
        val stats = WalletStats(
            walletId = 100L,
            currentMonthIncome = Money.ZERO,
            currentMonthExpense = Money.ZERO,
            allTimeIncome = Money.ZERO,
            allTimeExpense = Money.ZERO,
            txCount = 0,
        )

        coEvery { walletRepository.observeAll() } returns flowOf(listOf(emptyWallet))
        coEvery { walletRepository.getById(100L) } returns emptyWallet
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(listOf(stats))
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()

        val viewModel = AddWalletViewModel(
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            savedStateHandle = SavedStateHandle(mapOf("id" to 100L)),
            vietQrBankRepository = vietQrBankRepository,
            context = context,
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertThat(state.isEditMode).isTrue()
        assertThat(state.transactionCount).isEqualTo(0)
        assertThat(state.canDeleteDirectly).isTrue()
        assertThat(state.canArchive).isFalse()
    }

    @Test
    fun `saving a credit card stores credit limit and liability nature`() = runTest {
        coEvery { walletRepository.observeAll() } returns flowOf(emptyList())
        coEvery { vietQrBankRepository.getBanks() } returns emptyList()
        coEvery { walletRepository.upsert(any()) } returns 1L
        every { context.getString(R.string.wallet_created) } returns "Wallet created"

        val viewModel = AddWalletViewModel(
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            savedStateHandle = SavedStateHandle(),
            vietQrBankRepository = vietQrBankRepository,
            context = context,
        )
        advanceUntilIdle()

        viewModel.onNameChanged("Thẻ VPBank StepUp")
        viewModel.onNatureChanged(AccountNature.LIABILITY)
        viewModel.onWalletTypeChanged(WalletType.CREDIT_CARD)
        viewModel.onCreditLimitChanged("50000000") // 50,000,000 VND
        viewModel.save()
        advanceUntilIdle()

        coVerify {
            walletRepository.upsert(
                match { wallet ->
                    wallet.name == "Thẻ VPBank StepUp" &&
                        wallet.nature == AccountNature.LIABILITY &&
                        wallet.type == WalletType.CREDIT_CARD &&
                        wallet.creditLimit == Money(50_000_000_00L)
                }
            )
        }
    }
}
