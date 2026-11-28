package com.notepay.core.designsystem.feedback

import androidx.compose.material3.SnackbarDuration

enum class FeedbackType {
    Success,
    Error,
    Warning,
    Info,
    Pending,
    Offline,
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

internal fun FeedbackDuration.resolveSnackbarDuration(hasAction: Boolean): SnackbarDuration = when (this) {
    FeedbackDuration.Short -> if (hasAction) SnackbarDuration.Long else SnackbarDuration.Short
    FeedbackDuration.Long -> SnackbarDuration.Long
    FeedbackDuration.Indefinite -> SnackbarDuration.Indefinite
}
