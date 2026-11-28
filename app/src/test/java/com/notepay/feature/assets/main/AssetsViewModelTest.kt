package com.notepay.feature.assets.main

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.data.local.dao.SyncOutboxDao
import com.notepay.data.remote.CurrencyRateRepository
import com.notepay.data.remote.CurrencyRateSnapshot
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.SyncSummary
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.SharedWalletRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.TransferBetweenWalletsUseCase
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.domain.usecase.debt.GetDebtSummaryUseCase
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
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class AssetsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val walletRepository: WalletRepository = mockk(relaxed = true)
    private val transactionRepository: TransactionRepository = mockk(relaxed = true)
    private val getDebtSummaryUseCase: GetDebtSummaryUseCase = mockk(relaxed = true)
    private val transferBetweenWalletsUseCase: TransferBetweenWalletsUseCase = mockk(relaxed = true)
    private val currencyRateRepository: CurrencyRateRepository = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    @Test
    fun `net worth calculates total assets minus liabilities and debt summary`() = runTest {
        val cashWallet = Wallet(
            id = 1L,
            name = "Tiền mặt",
            initialBalance = Money(10_000_000_00L), // 10,000,000 VND
            iconKey = "cash",
            colorKey = "primary",
            nature = AccountNature.ASSET,
            type = WalletType.CASH,
            currencyCode = "VND",
        )
        val creditCard = Wallet(
            id = 2L,
            name = "Thẻ tín dụng",
            initialBalance = Money(-3_000_000_00L), // Dư nợ 3,000,000 VND
            iconKey = "card",
            colorKey = "primary",
            nature = AccountNature.LIABILITY,
            type = WalletType.CREDIT_CARD,
            currencyCode = "VND",
        )
        val debtSummary = DebtSummary(
            totalToCollect = Money.ZERO,
            totalToPay = Money(1_000_000_00L), // Nợ cá nhân 1,000,000 VND
            netBalance = Money(-1_000_000_00L),
            activeDebtsCount = 1,
            overdueCount = 0,
            dueTodayCount = 0,
        )

        coEvery { walletRepository.observeAll() } returns flowOf(listOf(cashWallet, creditCard))
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.observeRecentNonTransfers(any()) } returns flowOf(emptyList())
        coEvery { getDebtSummaryUseCase() } returns flowOf(debtSummary)
        coEvery { currencyRateRepository.getVndRates() } returns CurrencyRateSnapshot("VND", emptyMap(), "", 0L)

        val viewModel = AssetsViewModel(
            context = context,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            getDebtSummaryUseCase = getDebtSummaryUseCase,
            transferBetweenWalletsUseCase = transferBetweenWalletsUseCase,
            currencyRateRepository = currencyRateRepository,
        )

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            assertThat(current.totalAssetsVnd).isEqualTo(Money(10_000_000_00L))
            assertThat(current.totalLiabilitiesVnd).isEqualTo(Money(4_000_000_00L))
            assertThat(current.totalNetWorth).isEqualTo(Money(6_000_000_00L))
            assertThat(current.assetWallets).hasSize(1)
            assertThat(current.liabilityWallets).hasSize(1)
        }
    }

    @Test
    fun `multi-currency wallet balance is converted to VND in Net Worth`() = runTest {
        val usdWallet = Wallet(
            id = 10L,
            name = "Ví USD",
            initialBalance = Money(200_00L), // $200.00
            iconKey = "cash",
            colorKey = "primary",
            nature = AccountNature.ASSET,
            type = WalletType.CASH,
            currencyCode = "USD",
        )
        val rates = mapOf("USD" to BigDecimal("0.00004"))
        val snapshot = CurrencyRateSnapshot("VND", rates, "2026-11-19T00:00:00Z", 0L)

        coEvery { walletRepository.observeAll() } returns flowOf(listOf(usdWallet))
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.observeRecentNonTransfers(any()) } returns flowOf(emptyList())
        coEvery { getDebtSummaryUseCase() } returns flowOf(DebtSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0, 0, 0))
        coEvery { currencyRateRepository.getVndRates() } returns snapshot

        val viewModel = AssetsViewModel(
            context = context,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            getDebtSummaryUseCase = getDebtSummaryUseCase,
            transferBetweenWalletsUseCase = transferBetweenWalletsUseCase,
            currencyRateRepository = currencyRateRepository,
        )

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            assertThat(current.hasForeignCurrency).isTrue()
            assertThat(current.totalAssetsVnd).isEqualTo(Money(5_000_000_00L))
            assertThat(current.totalNetWorth).isEqualTo(Money(5_000_000_00L))
        }
    }

    @Test
    fun `submitAdjustBalance updates initialBalance without creating transaction`() = runTest {
        val investmentWallet = Wallet(
            id = 55L,
            name = "Quỹ DCBC",
            initialBalance = Money(10_000_000_00L),
            iconKey = "investment",
            colorKey = "primary",
            nature = AccountNature.ASSET,
            type = WalletType.FUND,
            currencyCode = "VND",
        )

        coEvery { walletRepository.getById(55L) } returns investmentWallet
        coEvery { walletRepository.observeAll() } returns flowOf(listOf(investmentWallet))
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.observeRecentNonTransfers(any()) } returns flowOf(emptyList())
        coEvery { getDebtSummaryUseCase() } returns flowOf(DebtSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0, 0, 0))
        coEvery { currencyRateRepository.getVndRates() } returns CurrencyRateSnapshot("VND", emptyMap(), "", 0L)
        coEvery { walletRepository.upsert(any()) } returns 55L
        every { context.getString(R.string.wallet_adjust_success) } returns "Balance adjusted"

        val viewModel = AssetsViewModel(
            context = context,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            getDebtSummaryUseCase = getDebtSummaryUseCase,
            transferBetweenWalletsUseCase = transferBetweenWalletsUseCase,
            currencyRateRepository = currencyRateRepository,
        )

        viewModel.submitAdjustBalance(walletId = 55L, newBalanceCents = 12_000_000_00L)
        advanceUntilIdle()

        coVerify {
            walletRepository.upsert(
                match { it.id == 55L && it.initialBalance == Money(12_000_000_00L) }
            )
        }
    }

    @Test
    fun `totalNetWorth and totalAssetsVnd exclude wallets with includeInTotalBalance false`() = runTest {
        val personalWallet = Wallet(
            id = 1L,
            name = "Ví cá nhân",
            initialBalance = Money.fromMajorUnit(10_000_000L),
            iconKey = "cash",
            colorKey = "primary",
            purpose = WalletPurpose.PERSONAL,
            includeInTotalBalance = true,
        )
        val businessWallet = Wallet(
            id = 2L,
            name = "Ví kinh doanh shop",
            initialBalance = Money.fromMajorUnit(50_000_000L),
            iconKey = "business",
            colorKey = "secondary",
            purpose = WalletPurpose.BUSINESS,
            includeInTotalBalance = false,
        )

        coEvery { walletRepository.observeAll() } returns flowOf(listOf(personalWallet, businessWallet))
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.observeRecentNonTransfers(any()) } returns flowOf(emptyList())
        coEvery { getDebtSummaryUseCase() } returns flowOf(DebtSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0, 0, 0))
        coEvery { currencyRateRepository.getVndRates() } returns CurrencyRateSnapshot("VND", emptyMap(), "", 0L)

        val viewModel = AssetsViewModel(
            context = context,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            getDebtSummaryUseCase = getDebtSummaryUseCase,
            transferBetweenWalletsUseCase = transferBetweenWalletsUseCase,
            currencyRateRepository = currencyRateRepository,
        )

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }

            // Business wallet (50M) has includeInTotalBalance = false -> personal total remains 10M
            assertThat(current.totalAssetsVnd).isEqualTo(Money.fromMajorUnit(10_000_000L))
            assertThat(current.totalNetWorth).isEqualTo(Money.fromMajorUnit(10_000_000L))
            assertThat(current.excludedWalletCount).isEqualTo(1)
            assertThat(current.assetWallets).hasSize(2)
            assertThat(current.wallets).hasSize(2)
        }
    }

    @Test
    fun `pendingSyncCount reflects outbox flow and syncPendingOutbox syncs shared wallets`() = runTest {
        val wallet = Wallet(
            id = 1L,
            name = "Ví chung",
            initialBalance = Money.fromMajorUnit(1_000_000L),
            iconKey = "cash",
            colorKey = "primary",
            isShared = true,
        )
        val sharedWalletRepository = mockk<SharedWalletRepository>(relaxed = true) {
            every { isCloudConfigured } returns true
            coEvery { syncAllSharedWallets() } returns Result.success(
                SyncSummary(pushedCount = 2, pulledWalletsCount = 1, pulledTransactionsCount = 1),
            )
        }
        val syncOutboxDao = mockk<SyncOutboxDao> {
            every { observePendingCount() } returns flowOf(3)
        }
        every { context.resources } returns mockk(relaxed = true) {
            every { getQuantityString(R.plurals.shared_wallet_sync_success, 1, 2, 1) } returns "Synced 2, pulled 1"
        }
        coEvery { walletRepository.observeAll() } returns flowOf(listOf(wallet))
        coEvery { transactionRepository.observeWalletStats(any(), any()) } returns flowOf(emptyList())
        coEvery { transactionRepository.observeRecentNonTransfers(any()) } returns flowOf(emptyList())
        coEvery { getDebtSummaryUseCase() } returns flowOf(DebtSummary(Money.ZERO, Money.ZERO, Money.ZERO, 0, 0, 0))
        coEvery { currencyRateRepository.getVndRates() } returns CurrencyRateSnapshot("VND", emptyMap(), "", 0L)

        val viewModel = AssetsViewModel(
            context = context,
            walletRepository = walletRepository,
            transactionRepository = transactionRepository,
            getDebtSummaryUseCase = getDebtSummaryUseCase,
            transferBetweenWalletsUseCase = transferBetweenWalletsUseCase,
            currencyRateRepository = currencyRateRepository,
            sharedWalletRepository = sharedWalletRepository,
            syncOutboxDao = syncOutboxDao,
        )

        viewModel.state.test {
            var current = awaitItem()
            while (current.isLoading) {
                current = awaitItem()
            }
            assertThat(current.pendingSyncCount).isEqualTo(3)
            cancelAndIgnoreRemainingEvents()
        }

        viewModel.feedback.test {
            viewModel.syncPendingOutbox()
            advanceUntilIdle()
            val feedback = awaitItem()
            assertThat(feedback.type).isEqualTo(FeedbackType.Success)
            assertThat(feedback.message).isEqualTo("Synced 2, pulled 1")
        }
        coVerify(exactly = 1) { sharedWalletRepository.syncAllSharedWallets() }
    }
}
