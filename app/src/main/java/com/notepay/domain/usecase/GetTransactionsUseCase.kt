package com.notepay.domain.usecase

import com.notepay.di.IoDispatcher
import com.notepay.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    operator fun invoke(): Flow<List<com.notepay.domain.model.Transaction>> =
        transactionRepo.observeAll().flowOn(dispatcher)
}
