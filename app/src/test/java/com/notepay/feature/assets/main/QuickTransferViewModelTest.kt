package com.notepay.feature.assets.main

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.TransferBetweenWalletsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickTransferViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val context: Context = mockk(relaxed = true) {
        every { getString(R.string.transfer_note_default) } returns "Chuyển tiền"
        every { getString(R.string.transfer_note_to, any(), any()) } returns "Chuyển đến ví"
        every { getString(R.string.transfer_note_from, any(), any()) } returns "Nhận từ ví"
        every { getString(R.string.feedback_transfer_success) } returns "Đã chuyển tiền giữa ví"
    }
    private val cashWallet = Wallet(
        id = 1L,
        name = "Tiền mặt",
        initialBalance = Money.fromMajorUnit(1_000_000L),
        iconKey = "cash",
        colorKey = "primary",
    )
    private val bankWallet = Wallet(
        id = 2L,
        name = "Ngân hàng",
        initialBalance = Money.fromMajorUnit(2_000_000L),
        iconKey = "bank",
        colorKey = "primary",
    )

    private fun createViewModel(
        transferUseCase: TransferBetweenWalletsUseCase = mockk(relaxed = true),
    ): QuickTransferViewModel {
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(listOf(cashWallet, bankWallet))
        }
        val txRepo = mockk<TransactionRepository> {
            every { observeWalletStats(any(), any()) } returns flowOf(emptyList())
        }
        val ratesRepo = mockk<CurrencyRateRepository> {
            coEvery { getVndRates() } returns CurrencyRateSnapshot("VND", emptyMap(), "", 0L)
        }
        return QuickTransferViewModel(
            context = context,
            walletRepository = walletRepo,
            transactionRepository = txRepo,
            transferBetweenWalletsUseCase = transferUseCase,
            currencyRateRepository = ratesRepo,
        )
    }

    @Test
    fun `open picks distinct source and target wallets and avoids same-wallet pair`() = runTest {
        val viewModel = createViewModel()
        viewModel.wallets.test {
            var loaded = awaitItem()
            while (loaded.isEmpty()) {
                loaded = awaitItem()
            }
            viewModel.open()
            val opened = viewModel.form.value
            assertThat(opened.isVisible).isTrue()
            assertThat(opened.fromWalletId).isEqualTo(1L)
            assertThat(opened.toWalletId).isEqualTo(2L)

            viewModel.onFromWalletChanged(2L)
            val swapped = viewModel.form.value
            assertThat(swapped.fromWalletId).isEqualTo(2L)
            assertThat(swapped.toWalletId).isEqualTo(1L)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeTransfer invokes use case and emits success feedback`() = runTest {
        val transferUseCase = mockk<TransferBetweenWalletsUseCase>()
        coEvery {
            transferUseCase.invoke(any(), any(), any(), any(), any(), any())
        } returns Result.success(Unit)
        val viewModel = createViewModel(transferUseCase)
        viewModel.wallets.test {
            var loaded = awaitItem()
            while (loaded.isEmpty()) {
                loaded = awaitItem()
            }
            viewModel.open()
            viewModel.onAmountChanged("50000")

            viewModel.feedback.test {
                viewModel.executeTransfer()
                runCurrent()
                val feedback = awaitItem()
                assertThat(feedback.type).isEqualTo(FeedbackType.Success)
                assertThat(viewModel.form.value.isVisible).isFalse()
            }
            coVerify(exactly = 1) {
                transferUseCase(
                    fromWalletId = 1L,
                    toWalletId = 2L,
                    amount = Money.fromMajorUnit(50_000L),
                    outgoingNote = any(),
                    incomingNote = any(),
                    occurredAt = any(),
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `executeTransfer failure keeps sheet open with Failed submitError`() = runTest {
        val transferUseCase = mockk<TransferBetweenWalletsUseCase>()
        coEvery {
            transferUseCase.invoke(any(), any(), any(), any(), any(), any())
        } returns Result.failure(IllegalStateException("db err"))
        val viewModel = createViewModel(transferUseCase)
        viewModel.wallets.test {
            var loaded = awaitItem()
            while (loaded.isEmpty()) {
                loaded = awaitItem()
            }
            viewModel.open()
            viewModel.onAmountChanged("25000")
            viewModel.executeTransfer()
            runCurrent()

            assertThat(viewModel.form.value.isVisible).isTrue()
            assertThat(viewModel.form.value.submitError).isEqualTo(TransferError.Failed)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
