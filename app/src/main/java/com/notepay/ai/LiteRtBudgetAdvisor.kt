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
import com.notepay.R
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.BudgetAdvisorInput
import com.notepay.domain.analytics.BudgetAdvisorResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val appContext = context.applicationContext

    fun isModelReady(): Boolean = modelManager.installedModelFile() != null

    suspend fun generate(input: BudgetAdvisorInput): BudgetAdvisorResult = withContext(ioDispatcher) {
        val modelFile = modelManager.installedModelFile()
            ?: error(appContext.getString(R.string.ai_no_model_installed))

        try {
            withTimeout(INFERENCE_TIMEOUT_MILLIS) {
                Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
                val attempts = BackendAttempt.DEFAULT_ORDER
                return@withTimeout runBackendAttempts(
                    labels = attempts.map { it.label },
                    operation = { backendLabel ->
                        val backend = attempts.first { it.label == backendLabel }
                        try {
                            generateWithBackend(
                                input = input,
                                modelPath = modelFile.absolutePath,
                                backend = backend.create(),
                                backendLabel = backend.label,
                            )
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Throwable) {
                            if (com.notepay.BuildConfig.DEBUG) {
                                Log.e(
                                    TAG,
                                    "LiteRT-LM ${backend.label} failed: ${error.javaClass.simpleName}",
                                )
                            }
                            throw error
                        }
                    },
                    noBackend = {
                        IllegalStateException(appContext.getString(R.string.ai_backend_unavailable))
                    },
                )
            }
        } catch (timeout: TimeoutCancellationException) {
            currentCoroutineContext().ensureActive()
            throw LiteRtInferenceTimeoutException(timeout)
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
                val parsed = AdvisorResponseParser.parseLenient(
                    raw = raw,
                    fallbackTitle = appContext.getString(R.string.ai_advisor_fallback_title),
                    fallbackAction = appContext.getString(R.string.ai_advisor_fallback_action),
                )
                    ?: error(appContext.getString(R.string.ai_invalid_response))
                val displayName = modelManager.state.value.displayName
                    ?: appContext.getString(R.string.ai_litert_default_model_name)
                return BudgetAdvisorResult(
                    title = parsed.title,
                    content = "${parsed.observation} ${parsed.action}",
                    provider = AdvisorProvider.LOCAL_LITERT_MODEL,
                    providerMessage = appContext.getString(
                        R.string.ai_litert_provider_message,
                        displayName,
                        backendLabel,
                    ),
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
            // Tận dụng GPU Adreno (OpenCL) trước, nếu thiết bị không hỗ trợ thì fallback mượt về CPU (XNNPACK).
            val DEFAULT_ORDER = listOf(
                BackendAttempt(label = "GPU") { Backend.GPU() },
                BackendAttempt(label = "CPU") { Backend.CPU() },
            )
        }
    }
}

internal class LiteRtInferenceTimeoutException(
    cause: TimeoutCancellationException? = null,
) : RuntimeException(cause)

internal suspend fun <T> runBackendAttempts(
    labels: List<String>,
    operation: suspend (String) -> T,
    noBackend: () -> Throwable,
): T {
    var lastError: Throwable? = null
    for (label in labels) {
        try {
            return operation(label)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            lastError = error
        }
    }
    throw lastError ?: noBackend()
}
