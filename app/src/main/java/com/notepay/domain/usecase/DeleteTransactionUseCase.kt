package com.notepay.domain.usecase

import com.notepay.di.IoDispatcher
import com.notepay.domain.repository.TransactionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteTransactionUseCase @Inject constructor(
    private val transactionRepo: TransactionRepository,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = runCatching {
        withContext(dispatcher) { transactionRepo.delete(id) }
    }
}
