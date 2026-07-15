package com.notepay.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.notepay.di.IoDispatcher
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/** Runs a user-supplied `.litertlm` model entirely inside the app process. */
@Singleton
class LiteRtBudgetAdvisor @Inject constructor(
    @ApplicationContext context: Context,
    private val modelManager: LocalAiModelManager,
    private val promptAdvisor: GeminiNanoBudgetAdvisor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val appContext = context.applicationContext

    fun isModelReady(): Boolean = modelManager.installedModelFile() != null

    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult = withContext(ioDispatcher) {
        val modelFile = modelManager.installedModelFile()
            ?: error("Chưa có mô hình AI cục bộ trên thiết bị.")

        try {
            withTimeout(INFERENCE_TIMEOUT_MILLIS) {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                var gpuFailure: Throwable? = null

                for (backend in BackendAttempt.DEFAULT_ORDER) {
                    try {
                        return@withTimeout generateWithBackend(
                            input = input,
                            modelPath = modelFile.absolutePath,
                            backend = backend.create(),
                            backendLabel = backend.label,
                        )
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (t: Throwable) {
                        if (backend.isCpu) throw t
                        gpuFailure = t
                    }
                }

                throw gpuFailure ?: error("Không thể khởi tạo mô hình AI cục bộ.")
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        }
    }

    private fun generateWithBackend(
        input: BudgetAdvisorInput,
        modelPath: String,
        backend: Backend,
        backendLabel: String,
    ): BudgetAdvisorResult {
        val config = EngineConfig(
            modelPath = modelPath,
            backend = backend,
            cacheDir = appContext.cacheDir.absolutePath,
        )
        Engine(config).use { engine ->
            engine.initialize()
            engine.createConversation().use { conversation ->
                val raw = conversation.sendMessage(promptAdvisor.buildPrompt(input))
                    .contents
                    .contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(separator = "") { it.text }
                val parsed = AdvisorResponseParser.parse(raw)
                    ?: error("Mô hình trả về nội dung chưa đúng định dạng an toàn.")
                val displayName = modelManager.state.value.displayName
                    ?: "mô hình LiteRT-LM"
                return BudgetAdvisorResult(
                    title = parsed.title,
                    content = "${parsed.observation} ${parsed.action}",
                    provider = AdvisorProvider.LOCAL_LITERT_MODEL,
                    providerMessage = "Phân tích bởi $displayName ngay trên thiết bị ($backendLabel)",
                )
            }
        }
    }

    private companion object {
        const val INFERENCE_TIMEOUT_MILLIS = 120_000L
    }

    private data class BackendAttempt(
        val label: String,
        val isCpu: Boolean = false,
        val create: () -> Backend,
    ) {
        companion object {
            val DEFAULT_ORDER = listOf(
                BackendAttempt(label = "GPU") { Backend.GPU() },
                BackendAttempt(label = "CPU", isCpu = true) { Backend.CPU() },
            )
        }
    }
}
