package com.notepay.platform.notification

import android.content.Context
import com.notepay.data.preferences.AppSettingsDataStore
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.domain.TestData
import com.notepay.domain.model.Money
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.GetMonthlySummaryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class BudgetAlertNotifierTest {

    private val appSettingsDataStore = mockk<AppSettingsDataStore>(relaxed = true)
    private val budgetSettingsStore = mockk<BudgetSettingsStore>(relaxed = true)
    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val getMonthlySummaryUseCase = mockk<GetMonthlySummaryUseCase>()
    private val context = mockk<Context>(relaxed = true)

    private val notifier = BudgetAlertNotifier(
        appSettingsDataStore = appSettingsDataStore,
        budgetSettingsStore = budgetSettingsStore,
        walletRepository = walletRepository,
        getMonthlySummaryUseCase = getMonthlySummaryUseCase,
        context = context,
    )

    @Before
    fun setUp() {
        mockkObject(NotificationHelper)
        every { NotificationHelper.sendBudgetAlert(any(), any(), any(), any(), any()) } returns Unit
        coEvery { budgetSettingsStore.settings } returns flowOf(com.notepay.data.preferences.BudgetSettings())
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `checkAndNotify when disabled does not query budget or notify`() = runTest {
        coEvery { appSettingsDataStore.budgetAlertsEnabled } returns flowOf(false)

        notifier.checkAndNotify(1L)

        coVerify(exactly = 0) { walletRepository.getById(any()) }
        coVerify(exactly = 0) { NotificationHelper.sendBudgetAlert(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `checkAndNotify when spending reaches 80 percent sends warning notification and records 80`() = runTest {
        coEvery { appSettingsDataStore.budgetAlertsEnabled } returns flowOf(true)
        coEvery { appSettingsDataStore.lastNotifiedBudgetMonth } returns flowOf("2026-09")
        coEvery { appSettingsDataStore.lastNotifiedBudgetThreshold } returns flowOf(0)

        val wallet = TestData.wallet(id = 1L, name = "Chính").copy(
            budgetLimit = Money(10_000_000L),
        )
        coEvery { walletRepository.getById(1L) } returns wallet

        val summary = GetMonthlySummaryUseCase.Summary(
            year = 2026,
            month = 9,
            totalIncome = Money.ZERO,
            totalExpense = Money(8_500_000L), // 85%
            balance = Money(-8_500_000L),
            byCategory = emptyMap(),
            byIncomeCategory = emptyMap(),
            transactions = emptyList(),
        )
        every { getMonthlySummaryUseCase.invoke(any(), any(), 1L) } returns flowOf(summary)

        notifier.checkAndNotify(1L)

        coVerify(exactly = 1) {
            NotificationHelper.sendBudgetAlert(
                context = any(),
                isOverspent = false,
                spentFormatted = any(),
                limitFormatted = any(),
                percentage = 85,
            )
        }
        coVerify(exactly = 1) {
            appSettingsDataStore.recordBudgetAlertNotification(any(), 80)
        }
    }

    @Test
    fun `checkAndNotify when spending reaches 100 percent sends exceeded alert and records 100`() = runTest {
        coEvery { appSettingsDataStore.budgetAlertsEnabled } returns flowOf(true)
        coEvery { appSettingsDataStore.lastNotifiedBudgetMonth } returns flowOf("2026-09")
        coEvery { appSettingsDataStore.lastNotifiedBudgetThreshold } returns flowOf(80)

        val wallet = TestData.wallet(id = 1L, name = "Chính").copy(
            budgetLimit = Money(10_000_000L),
        )
        coEvery { walletRepository.getById(1L) } returns wallet

        val summary = GetMonthlySummaryUseCase.Summary(
            year = 2026,
            month = 9,
            totalIncome = Money.ZERO,
            totalExpense = Money(10_500_000L), // 105%
            balance = Money(-10_500_000L),
            byCategory = emptyMap(),
            byIncomeCategory = emptyMap(),
            transactions = emptyList(),
        )
        every { getMonthlySummaryUseCase.invoke(any(), any(), 1L) } returns flowOf(summary)

        notifier.checkAndNotify(1L)

        coVerify(exactly = 1) {
            NotificationHelper.sendBudgetAlert(
                context = any(),
                isOverspent = true,
                spentFormatted = any(),
                limitFormatted = any(),
                percentage = 105,
            )
        }
        coVerify(exactly = 1) {
            appSettingsDataStore.recordBudgetAlertNotification(any(), 100)
        }
    }

    @Test
    fun `checkAndNotify when 80 percent already notified does not duplicate alert`() = runTest {
        coEvery { appSettingsDataStore.budgetAlertsEnabled } returns flowOf(true)
        coEvery { appSettingsDataStore.lastNotifiedBudgetMonth } returns flowOf("2026-09")
        coEvery { appSettingsDataStore.lastNotifiedBudgetThreshold } returns flowOf(80)

        val wallet = TestData.wallet(id = 1L, name = "Chính").copy(
            budgetLimit = Money(10_000_000L),
        )
        coEvery { walletRepository.getById(1L) } returns wallet

        val summary = GetMonthlySummaryUseCase.Summary(
            year = 2026,
            month = 9,
            totalIncome = Money.ZERO,
            totalExpense = Money(8_800_000L), // 88%
            balance = Money(-8_800_000L),
            byCategory = emptyMap(),
            byIncomeCategory = emptyMap(),
            transactions = emptyList(),
        )
        every { getMonthlySummaryUseCase.invoke(any(), any(), 1L) } returns flowOf(summary)

        notifier.checkAndNotify(1L)

        coVerify(exactly = 0) {
            NotificationHelper.sendBudgetAlert(any(), any(), any(), any(), any())
        }
    }
}
