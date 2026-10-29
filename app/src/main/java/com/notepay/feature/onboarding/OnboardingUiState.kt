package com.notepay.feature.onboarding

import androidx.annotation.StringRes
import com.notepay.R
import com.notepay.domain.model.Wallet

enum class OnboardingStep(val stepIndex: Int) {
    CURRENCY(0),
    WALLET(1),
    REMINDER(2);

    val nextStep: OnboardingStep?
        get() = when (this) {
            CURRENCY -> WALLET
            WALLET -> REMINDER
            REMINDER -> null
        }

    val previousStep: OnboardingStep?
        get() = when (this) {
            CURRENCY -> null
            WALLET -> CURRENCY
            REMINDER -> WALLET
        }
}

data class ChoiceOption(
    val key: String,
    @get:StringRes val labelRes: Int
)

data class OnboardingUiState(
    val currentStep: OnboardingStep = OnboardingStep.CURRENCY,
    val currencyCode: String = "VND",
    val languageCode: String = "system",
    val walletName: String = "",
    val walletIconKey: String = Wallet.ICON_CASH,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 30,
    val notificationDenied: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: Boolean = false,
    val isCompleted: Boolean = false
) {
    val canContinue: Boolean
        get() = when (currentStep) {
            OnboardingStep.CURRENCY -> true
            OnboardingStep.WALLET -> walletName.isNotBlank()
            OnboardingStep.REMINDER -> true
        }

    companion object {
        /** Mã tiền tệ hiển thị nguyên văn — không cần dịch. */
        val CURRENCIES = listOf("VND", "USD", "EUR", "JPY")

        val LANGUAGES = listOf(
            ChoiceOption("vi", R.string.appearance_lang_vi),
            ChoiceOption("en", R.string.appearance_lang_en),
            ChoiceOption("system", R.string.appearance_lang_system)
        )

        val WALLET_ICONS = listOf(
            ChoiceOption(Wallet.ICON_CASH, R.string.icon_label_cash),
            ChoiceOption(Wallet.ICON_BANK, R.string.transfer_bank),
            ChoiceOption(Wallet.ICON_MOMO, R.string.icon_label_ewallet),
            ChoiceOption(Wallet.ICON_CARD, R.string.icon_label_credit_card)
        )

        /** Giờ nhắc gợi ý; người dùng chọn nhanh thay vì mở dial. */
        val REMINDER_PRESETS = listOf(19 to 0, 20 to 0, 20 to 30, 21 to 0, 22 to 0)
    }
}
