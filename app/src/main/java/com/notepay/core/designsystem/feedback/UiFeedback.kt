package com.notepay.ui.feedback

enum class FeedbackType {
    Success,
    Error,
    Info,
}

enum class FeedbackDuration {
    Short,
    Long,
    Indefinite,
}

data class UiFeedback(
    val message: String,
    val actionLabel: String? = null,
    val type: FeedbackType = FeedbackType.Info,
    val duration: FeedbackDuration = FeedbackDuration.Short,
    val onAction: (() -> Unit)? = null,
)
