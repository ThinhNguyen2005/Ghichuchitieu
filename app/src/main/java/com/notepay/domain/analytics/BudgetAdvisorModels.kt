package com.notepay.domain.analytics

data class AdvisorCategorySummary(
    val name: String,
    val amountInCents: Long,
    val share: Double,
)

data class BudgetAdvisorInput(
    val prediction: SpendingPrediction,
    val budgetLimitInCents: Long?,
    val incomeThisMonthInCents: Long,
    val categories: List<AdvisorCategorySummary>,
)

enum class AdvisorProvider {
    GEMINI_NANO,
    LOCAL_LITERT_MODEL,
    STATISTICAL_FALLBACK,
}

enum class AdvisorAvailability {
    CHECKING,
    GEMINI_NANO,
    LOCAL_MODEL,
    STATISTICAL_ONLY,
}

data class BudgetAdvisorResult(
    val title: String,
    val content: String,
    val provider: AdvisorProvider,
    val providerMessage: String? = null,
)
