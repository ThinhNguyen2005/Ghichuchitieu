package com.notepay.core.platform.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Immutable
import com.notepay.domain.money.VietnameseAmountParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class VoiceInputError {
    PERMISSION_DENIED,
    UNAVAILABLE,
    NO_MATCH,
    NETWORK,
    AUDIO,
    UNKNOWN,
}

@Immutable
data class VoiceInputState(
    val isListening: Boolean = false,
    val rmsNormalized: Float = 0f,
    val error: VoiceInputError? = null,
)

/**
 * Pure helper for combining pre-existing manual input with live speech transcripts.
 */
object VoiceTranscriptMerger {

    const val DEFAULT_MAX_LENGTH = 500

    fun merge(
        baseText: String,
        spokenSegment: String,
        maxLength: Int = DEFAULT_MAX_LENGTH,
    ): String {
        val spoken = spokenSegment.trim()
        if (spoken.isEmpty()) return baseText.take(maxLength)
        val trimmedBase = baseText.trimEnd()
        val combined = if (trimmedBase.isEmpty()) spoken else "$trimmedBase $spoken"
        return combined.take(maxLength)
    }
}

/**
 * Normalizes common Vietnamese speech-to-text artifacts and selects the strongest financial
 * candidate from Android N-best recognition results.
 */
object VoiceTranscriptPostProcessor {

    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val MISHEARD_CA_REGEX = Regex("""(?<![.\d,])([1-9]\d{1,3})\s+ca\b""", RegexOption.IGNORE_CASE)
    private val TRAILING_DONG_AFTER_THOUSAND_REGEX = Regex(
        """\b(nghìn|ngàn|nghin|ngan)\s+(?:đồng|dong)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val MILLION_HUNDRED_SHORTHAND_REGEX = Regex(
        """\b(\d+)\s*(triệu|trieu)\s+([1-9]00)\b(?!\s*(?:nghìn|ngàn|nghin|ngan|k|triệu|trieu|tỷ|ty|ti))""",
        RegexOption.IGNORE_CASE,
    )

    fun normalizeSttArtifacts(raw: String): String {
        val collapsed = raw.trim().replace(WHITESPACE_REGEX, " ")
        if (collapsed.isEmpty()) return ""

        var normalized = collapsed.replace('₫', 'đ')
        normalized = MISHEARD_CA_REGEX.replace(normalized) { "${it.groupValues[1]}k" }
        normalized = TRAILING_DONG_AFTER_THOUSAND_REGEX.replace(normalized) { it.groupValues[1] }
        normalized = MILLION_HUNDRED_SHORTHAND_REGEX.replace(normalized) { match ->
            "${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[3]} nghìn"
        }
        return normalized.replace(WHITESPACE_REGEX, " ").trim()
    }

    fun selectBestCandidate(
        candidates: List<String>,
        confidences: FloatArray? = null,
    ): String? {
        if (candidates.isEmpty()) return null
        var bestText: String? = null
        var bestScore = Int.MIN_VALUE

        candidates.forEachIndexed { index, raw ->
            if (raw.isBlank()) return@forEachIndexed
            val normalized = normalizeSttArtifacts(raw)
            if (normalized.isEmpty()) return@forEachIndexed

            val rankScore = (candidates.size - index) * 2
            val amountScore = VietnameseAmountParser.parseAll(normalized).size * 12
            val confidenceBonus = if (confidences != null && index < confidences.size && confidences[index] >= 0.75f) {
                4
            } else {
                0
            }
            val diacriticBonus = if (normalized.any { it.code > ASCII_LIMIT && it.isLetter() }) 4 else 0
            val totalScore = rankScore + amountScore + confidenceBonus + diacriticBonus

            if (totalScore > bestScore) {
                bestScore = totalScore
                bestText = normalized
            }
        }
        return bestText
    }

    private const val ASCII_LIMIT = 127
}

/**
 * Main-thread wrapper around Android [SpeechRecognizer] providing partial transcript streaming
 * and [RecognizerIntent.ACTION_RECOGNIZE_SPEECH] intent construction for fallback launches.
 */
class AndroidVoiceInputController(
    context: Context,
    private val onPartialTranscript: (String) -> Unit,
    private val onFinalTranscript: (String) -> Unit,
) {
    private val appContext: Context = context.applicationContext
    private val _state = MutableStateFlow(VoiceInputState())
    val state = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.update { it.copy(isListening = true, error = null) }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) {
            if (!_state.value.isListening) return
            val normalized = ((rmsdB + RMS_FLOOR_DB) / RMS_SPAN_DB).coerceIn(0f, 1f)
            _state.update { it.copy(rmsNormalized = normalized) }
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.update { it.copy(rmsNormalized = 0f) }
        }

        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_CLIENT && !_state.value.isListening) {
                return
            }
            _state.update {
                VoiceInputState(
                    isListening = false,
                    rmsNormalized = 0f,
                    error = mapSpeechError(error),
                )
            }
        }

        override fun onResults(results: Bundle?) {
            _state.update { it.copy(isListening = false, rmsNormalized = 0f, error = null) }
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
            VoiceTranscriptPostProcessor.selectBestCandidate(matches, confidences)?.let(onFinalTranscript)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val topPartial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull { it.isNotBlank() }
                ?: return
            VoiceTranscriptPostProcessor.normalizeSttArtifacts(topPartial)
                .takeIf { it.isNotEmpty() }
                ?.let(onPartialTranscript)
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun isInlineAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun buildRecognizerIntent(languageTag: String, prompt: String): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, MAX_NBEST_RESULTS)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, COMPLETE_SILENCE_MS)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                POSSIBLY_COMPLETE_SILENCE_MS,
            )
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MINIMUM_SPEECH_MS)
            putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
        }
    }

    /**
     * Starts inline speech recognition. Returns false when inline recognition is unsupported
     * or fails to initialize so the caller can fall back to an activity launcher.
     */
    fun startListening(languageTag: String, prompt: String): Boolean {
        if (!isInlineAvailable()) return false
        val recognizer = try {
            ensureRecognizer()
        } catch (_: RuntimeException) {
            return false
        } ?: return false

        return try {
            if (_state.value.isListening) {
                _state.update { it.copy(isListening = false, rmsNormalized = 0f) }
                recognizer.cancel()
            }
            _state.update { VoiceInputState(isListening = true, rmsNormalized = 0f, error = null) }
            recognizer.startListening(buildRecognizerIntent(languageTag, prompt))
            true
        } catch (_: SecurityException) {
            _state.update {
                VoiceInputState(
                    isListening = false,
                    rmsNormalized = 0f,
                    error = VoiceInputError.PERMISSION_DENIED,
                )
            }
            true
        } catch (_: RuntimeException) {
            _state.update { VoiceInputState(isListening = false, rmsNormalized = 0f, error = null) }
            false
        }
    }

    fun stopListening() {
        _state.update { it.copy(isListening = false, rmsNormalized = 0f) }
        speechRecognizer?.stopListening()
    }

    fun cancel() {
        _state.update { it.copy(isListening = false, rmsNormalized = 0f) }
        speechRecognizer?.cancel()
    }

    fun destroy() {
        cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        speechRecognizer?.let { return it }
        if (!isInlineAvailable()) return null
        return SpeechRecognizer.createSpeechRecognizer(appContext).also { created ->
            created.setRecognitionListener(listener)
            speechRecognizer = created
        }
    }

    private fun mapSpeechError(errorCode: Int): VoiceInputError = when (errorCode) {
        SpeechRecognizer.ERROR_NO_MATCH,
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
        -> VoiceInputError.NO_MATCH

        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        -> VoiceInputError.NETWORK

        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputError.PERMISSION_DENIED
        SpeechRecognizer.ERROR_AUDIO -> VoiceInputError.AUDIO
        else -> VoiceInputError.UNKNOWN
    }

    private companion object {
        const val RMS_FLOOR_DB = 2f
        const val RMS_SPAN_DB = 12f
        const val MAX_NBEST_RESULTS = 5
        const val COMPLETE_SILENCE_MS = 2_000L
        const val POSSIBLY_COMPLETE_SILENCE_MS = 1_500L
        const val MINIMUM_SPEECH_MS = 3_000L
    }
}
