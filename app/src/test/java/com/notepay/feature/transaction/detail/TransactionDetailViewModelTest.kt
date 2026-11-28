package com.notepay.feature.transaction.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.navigation.Route
import com.notepay.data.sync.SharedWalletSyncCoordinator
import com.notepay.domain.TestData
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.SyncStatus
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `retrySync emits Offline or Error with Retry on failure and Success on recovery`() = runTest {
        val wallet = TestData.wallet(id = 2L, name = "Shared").copy(
            isShared = true,
            sharedRole = SharedWalletRole.OWNER,
        )
        val failedTx = TestData.transaction(id = 7L, walletId = 2L).copy(syncStatus = SyncStatus.FAILED)
        val txRepo = mockk<TransactionRepository> {
            every { observeById(7L) } returns flowOf(failedTx)
        }
        val walletRepo = mockk<WalletRepository> {
            coEvery { getById(2L) } returns wallet
        }
        val coordinator = mockk<SharedWalletSyncCoordinator>()
        coEvery { coordinator.retryTransactionSync(wallet, 7L) } throws IllegalStateException("offline timeout") andThen true

        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.feedback_transaction_sync_retried_success) } returns "Sync OK"
            every { getString(R.string.feedback_transaction_sync_retry_failed) } returns "Sync failed"
            every { getString(R.string.action_retry_transaction_sync) } returns "Retry sync"
        }

        val vm = TransactionDetailViewModel(
            transactionRepository = txRepo,
            walletRepository = walletRepo,
            savedStateHandle = SavedStateHandle(mapOf(Route.TransactionDetail.ARG_ID to 7L)),
            context = context,
            sharedWalletSyncCoordinator = coordinator,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )

        vm.feedback.test {
            vm.retrySync()
            val offline = awaitItem()
            assertThat(offline.type).isEqualTo(FeedbackType.Offline)
            assertThat(offline.actionLabel).isEqualTo("Retry sync")

            offline.onAction?.invoke()
            val ok = awaitItem()
            assertThat(ok.type).isEqualTo(FeedbackType.Success)
            assertThat(ok.message).isEqualTo("Sync OK")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
