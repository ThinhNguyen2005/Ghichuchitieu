package com.notepay.domain.usecase

import com.notepay.data.sync.SharedWalletSyncCoordinator
import com.notepay.di.IoDispatcher
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    private val walletRepo: WalletRepository?,
    private val sharedWalletCoordinator: SharedWalletSyncCoordinator?,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    internal constructor(
        transactionRepo: TransactionRepository,
        dispatcher: CoroutineDispatcher,
    ) : this(
        transactionRepo = transactionRepo,
        walletRepo = null,
        sharedWalletCoordinator = null,
        dispatcher = dispatcher,
    )

    suspend operator fun invoke(id: Long): Result<Unit> = try {
        Result.success(
            withContext(dispatcher) {
                val tx = if (walletRepo != null && sharedWalletCoordinator != null) {
                    transactionRepo.getById(id)
                } else {
                    null
                }
                val wallet = tx?.let { walletRepo?.getById(it.walletId) }
                val handledShared = if (wallet != null && wallet.isShared && sharedWalletCoordinator != null) {
                    sharedWalletCoordinator.deleteSharedTransaction(wallet, id)
                } else {
                    false
                }
                if (!handledShared) {
                    transactionRepo.delete(id)
                }
            },
        )
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
