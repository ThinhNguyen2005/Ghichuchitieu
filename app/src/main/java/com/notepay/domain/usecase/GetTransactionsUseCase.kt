package com.notepay.domain.usecase

import com.notepay.di.IoDispatcher
import com.notepay.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(walletId: Long? = null): Flow<List<com.notepay.domain.model.Transaction>> =
        (if (walletId == null) {
            transactionRepo.observeAll()
        } else {
            transactionRepo.observeByWallet(walletId)
        }).flowOn(dispatcher)
}
