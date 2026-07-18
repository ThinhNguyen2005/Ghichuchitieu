package com.notepay.ai

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.google.ai.edge.litertlm.SamplerConfig
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
                        if (com.notepay.BuildConfig.DEBUG) Log.e(TAG, "LiteRT-LM ${backend.label} failed: ${t.javaClass.simpleName}")
                        throw t
                    }
                }

                error("No LiteRT-LM backend is configured.")
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
            engine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = 20,
                        topP = 0.9,
                        temperature = 0.2,
                        seed = 2026,
                    ),
                ),
            ).use { conversation ->
                val raw = conversation.sendMessage(promptAdvisor.buildLiteRtPrompt(input))
                    .contents
                    .contents
                    .filterIsInstance<Content.Text>()
                    .joinToString(separator = "") { it.text }
                val parsed = AdvisorResponseParser.parseLenient(raw)
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
        const val TAG = "LiteRtBudgetAdvisor"
        const val INFERENCE_TIMEOUT_MILLIS = 120_000L
    }

    private data class BackendAttempt(
        val label: String,
        val create: () -> Backend,
    ) {
        companion object {
            // This Qwen3 LiteRT package is reliable on CPU/XNNPACK. A GPU attempt can
            // block a request, then fail and fall back to CPU on many Android devices.
            val DEFAULT_ORDER = listOf(BackendAttempt(label = "CPU") { Backend.CPU() })
        }
    }
}
