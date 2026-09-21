package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class LocalAiModelManagerTest {

    @Test
    fun `model import accepts only LiteRT-LM packages`() {
        assertThat(isSupportedLiteRtLmModelName("gemma.litertlm")).isTrue()
        assertThat(isSupportedLiteRtLmModelName("gemma.LITERTLM")).isTrue()
        assertThat(isSupportedLiteRtLmModelName("gemma.bin")).isFalse()
        assertThat(isSupportedLiteRtLmModelName("gemma.tflite")).isFalse()
    }

    @Test
    fun `model validation propagates probe inference failure`() = runTest {
        val error = IllegalStateException("probe inference failed")

        val thrown = assertFailsWith<IllegalStateException> {
            runModelValidationProbe { throw error }
        }

        assertThat(thrown).isSameInstanceAs(error)
    }
}
