package com.notepay.ui.feature.onboarding

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Wallet
import com.notepay.feature.onboarding.OnboardingStep
import com.notepay.feature.onboarding.OnboardingUiState
import org.junit.Test

/**
 * Logic nghiệp vụ của onboarding: điều kiện Continue và cắt tên ví theo giới hạn của [Wallet].
 */
class OnboardingUiStateTest {

    @Test
    fun `continue is blocked only when the wallet name is blank`() {
        assertThat(OnboardingUiState(currentStep = OnboardingStep.CURRENCY).canContinue).isTrue()
        assertThat(OnboardingUiState(currentStep = OnboardingStep.REMINDER).canContinue).isTrue()

        listOf(
            "" to false,
            "   " to false,
            "Tiền mặt" to true,
        ).forEach { (name, expected) ->
            assertThat(OnboardingUiState(currentStep = OnboardingStep.WALLET, walletName = name).canContinue)
                .isEqualTo(expected)
        }
    }

    @Test
    fun `wallet name longer than max is rejected by the model`() {
        val tooLong = "a".repeat(Wallet.MAX_NAME_LENGTH + 1)
        val error = runCatching {
            Wallet.default().copy(name = tooLong)
        }.exceptionOrNull()

        assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
        val valid = Wallet.default().copy(name = tooLong.take(Wallet.MAX_NAME_LENGTH))
        assertThat(valid.name).hasLength(Wallet.MAX_NAME_LENGTH)
    }

    @Test
    fun `onboarding currencies cover all 8 supported codes`() {
        assertThat(OnboardingUiState.CURRENCIES)
            .containsExactly("VND", "USD", "EUR", "JPY", "GBP", "KRW", "SGD", "THB")
            .inOrder()
    }
}
