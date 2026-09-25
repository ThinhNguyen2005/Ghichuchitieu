package com.notepay.domain.usecase.debt

import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.repository.DebtRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetDebtsUseCase @Inject constructor(
    private val debtRepository: DebtRepository,
) {
    operator fun invoke(
        type: DebtType? = null,
        query: String = "",
        onlyUnsettled: Boolean = false,
    ): Flow<List<DebtWithHistory>> {
        val flow = if (type != null) {
            debtRepository.observeByType(type)
        } else {
            debtRepository.observeAll()
        }

        return flow.map { list ->
            list.filter { item ->
                val matchesType = type == null || item.debt.type == type
                val matchesSettled = !onlyUnsettled || !item.isFullyPaid
                val matchesQuery = query.isBlank() ||
                    item.debt.personName.contains(query, ignoreCase = true) ||
                    item.debt.note.contains(query, ignoreCase = true) ||
                    (item.debt.phoneNumber?.contains(query) == true)

                matchesType && matchesSettled && matchesQuery
            }
        }
    }
}
