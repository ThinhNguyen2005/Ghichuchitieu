package com.notepay.domain.usecase.debt

import com.notepay.di.IoDispatcher
import com.notepay.domain.repository.DebtRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DeleteDebtUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend operator fun invoke(id: Long): Result<Unit> = try {
        withContext(ioDispatcher) {
            debtRepository.deleteDebt(id)
            Result.success(Unit)
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }
}
