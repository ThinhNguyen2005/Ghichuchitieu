package com.notepay.feature.onboarding

/**
 * Toàn bộ action từ OnboardingScreen → OnboardingViewModel.
 */
sealed interface OnboardingAction {
    data class SelectCurrency(val code: String) : OnboardingAction
    data class SelectLanguage(val code: String) : OnboardingAction
    data class UpdateWalletName(val name: String) : OnboardingAction
    data class SelectWalletIcon(val iconKey: String) : OnboardingAction
    data class ToggleReminder(val enabled: Boolean) : OnboardingAction
    data class UpdateReminderTime(val hour: Int, val minute: Int) : OnboardingAction
    data object ReportNotificationDenied : OnboardingAction
    data class NavigateToStep(val step: OnboardingStep) : OnboardingAction
    data object Complete : OnboardingAction
    data object Skip : OnboardingAction
}
