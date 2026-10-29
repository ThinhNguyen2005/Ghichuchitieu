package com.notepay.ui.feature.onboarding

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Wallet
import com.notepay.feature.onboarding.OnboardingStep
import com.notepay.feature.onboarding.OnboardingUiState
import org.junit.Test

/**
 * Logic thuần của onboarding: thứ tự bước, điều kiện Continue, cắt tên ví theo giới hạn của
 * [Wallet]. Không cần Android — chạy bằng JUnit thường.
 */
class OnboardingUiStateTest {

    @Test
    fun `steps advance and go back in order`() {
        assertThat(OnboardingStep.CURRENCY.nextStep).isEqualTo(OnboardingStep.WALLET)
        assertThat(OnboardingStep.WALLET.nextStep).isEqualTo(OnboardingStep.REMINDER)
        assertThat(OnboardingStep.REMINDER.nextStep).isNull()

        assertThat(OnboardingStep.CURRENCY.previousStep).isNull()
        assertThat(OnboardingStep.REMINDER.previousStep).isEqualTo(OnboardingStep.WALLET)
    }

    @Test
    fun `step indexes match pager pages`() {
        assertThat(OnboardingStep.entries.map { it.stepIndex }).containsExactly(0, 1, 2).inOrder()
    }

    @Test
    fun `continue is blocked only when the wallet name is blank`() {
        assertThat(OnboardingUiState(currentStep = OnboardingStep.CURRENCY).canContinue).isTrue()
        assertThat(OnboardingUiState(currentStep = OnboardingStep.REMINDER).canContinue).isTrue()

        assertThat(
            OnboardingUiState(currentStep = OnboardingStep.WALLET, walletName = "").canContinue
        ).isFalse()
        assertThat(
            OnboardingUiState(currentStep = OnboardingStep.WALLET, walletName = "   ").canContinue
        ).isFalse()
        assertThat(
            OnboardingUiState(currentStep = OnboardingStep.WALLET, walletName = "Tiền mặt").canContinue
        ).isTrue()
    }

    /**
     * `Wallet.init` ném nếu tên dài quá [Wallet.MAX_NAME_LENGTH]; ViewModel phải cắt trước khi
     * lưu. Test này khoá lại giả định đó để đổi MAX_NAME_LENGTH không âm thầm làm hỏng input.
     */
    @Test
    fun `wallet name longer than max is rejected by the model`() {
        val tooLong = "a".repeat(Wallet.MAX_NAME_LENGTH + 1)
        val error = runCatching {
            Wallet.default().copy(name = tooLong)
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        // Cắt đúng giới hạn thì hợp lệ.
        Wallet.default().copy(name = tooLong.take(Wallet.MAX_NAME_LENGTH))
    }

    @Test
    fun `preset reminder times are inside a single day`() {
        assertThat(OnboardingUiState.REMINDER_PRESETS).isNotEmpty()
        OnboardingUiState.REMINDER_PRESETS.forEach { (hour, minute) ->
            assertThat(hour).isIn(0..23)
            assertThat(minute).isIn(0..59)
        }
    }
}
