package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith

class LiteRtBudgetAdvisorTest {

    @Test
    fun `backend attempts continue to CPU after a recoverable GPU failure`() = runTest {
        val attempted = mutableListOf<String>()

        val result = runBackendAttempts(
            labels = listOf("GPU", "CPU"),
            operation = { label ->
                attempted += label
                if (label == "GPU") error("GPU unavailable")
                "CPU result"
            },
            noBackend = { IllegalStateException("no backend") },
        )

        assertThat(result).isEqualTo("CPU result")
        assertThat(attempted).containsExactly("GPU", "CPU").inOrder()
    }

    @Test
    fun `backend attempts preserve caller cancellation`() = runTest {
        val thrown = assertFailsWith<CancellationException> {
            runBackendAttempts(
                labels = listOf("GPU", "CPU"),
                operation = { throw CancellationException("cancelled") },
                noBackend = { IllegalStateException("no backend") },
            )
        }

        assertThat(thrown.message).isEqualTo("cancelled")
    }
}
