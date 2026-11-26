package com.notepay.ai

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder

internal class GeminiApiException(
    val httpStatus: Int,
    val apiStatus: String?,
    message: String,
) : IOException(message) {

    fun isInvalidApiKey(): Boolean {
        return httpStatus == 401 ||
            httpStatus == 403 ||
            apiStatus == "UNAUTHENTICATED" ||
            message.orEmpty().contains("API_KEY_INVALID", ignoreCase = true) ||
            message.orEmpty().contains("API key not valid", ignoreCase = true)
    }
}

/**
 * Lightweight REST client for the Google Gemini `generateContent` endpoint.
 * Replaces `com.google.ai.client.generativeai:generativeai` to avoid Ktor 2.x/3.x ABI conflicts
 * with `supabase-kt`.
 */
internal open class GeminiRestClient {

    open fun generateContent(
        modelName: String,
        apiKey: String,
        prompt: String,
        temperature: Double,
        maxOutputTokens: Int,
        responseMimeType: String? = null,
        responseSchema: JSONObject? = null,
        timeoutMillis: Int = DEFAULT_TIMEOUT_MILLIS,
    ): String {
        val encodedModel = URLEncoder.encode(modelName, Charsets.UTF_8.name())
        val endpoint = "$BASE_URL/$encodedModel:generateContent"
        val connection = URI.create(endpoint).toURL().openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.connectTimeout = timeoutMillis
        connection.readTimeout = timeoutMillis
        connection.doOutput = true
        // Pass API key via header so it never leaks into URL logs or exception messages.
        connection.setRequestProperty("x-goog-api-key", apiKey)
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")

        val payload = buildRequestPayload(
            prompt = prompt,
            temperature = temperature,
            maxOutputTokens = maxOutputTokens,
            responseMimeType = responseMimeType,
            responseSchema = responseSchema,
        )

        try {
            connection.outputStream.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw parseError(status, body)
            }
            return extractTextFromResponse(body)
        } finally {
            connection.disconnect()
        }
    }

    internal companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_TIMEOUT_MILLIS = 15_000

        internal fun buildRequestPayload(
            prompt: String,
            temperature: Double,
            maxOutputTokens: Int,
            responseMimeType: String?,
            responseSchema: JSONObject?,
        ): String {
            val generationConfig = JSONObject().apply {
                put("temperature", temperature)
                put("maxOutputTokens", maxOutputTokens)
                if (!responseMimeType.isNullOrBlank()) {
                    put("responseMimeType", responseMimeType)
                }
                if (responseSchema != null) {
                    put("responseSchema", responseSchema)
                }
            }
            val userContent = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            }
            return JSONObject().apply {
                put("contents", JSONArray().put(userContent))
                put("generationConfig", generationConfig)
            }.toString()
        }

        internal fun extractTextFromResponse(body: String): String {
            if (body.isBlank()) return ""
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: return ""
            if (candidates.length() == 0) return ""
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: return ""
            val parts = content.optJSONArray("parts") ?: return ""

            val builder = StringBuilder()
            for (i in 0 until parts.length()) {
                val part = parts.optJSONObject(i)
                if (part != null && !part.optBoolean("thought", false)) {
                    val text = part.optString("text", "")
                    if (text.isNotEmpty()) {
                        builder.append(text)
                    }
                }
            }
            return builder.toString().trim()
        }

        internal fun parseError(httpStatus: Int, body: String): GeminiApiException {
            var apiStatus: String? = null
            var message = "Gemini API HTTP $httpStatus"
            if (body.isNotBlank()) {
                try {
                    val errorObj = JSONObject(body).optJSONObject("error")
                    if (errorObj != null) {
                        apiStatus = errorObj.optString("status").takeIf { it.isNotBlank() }
                        val parsedMsg = errorObj.optString("message").takeIf { it.isNotBlank() }
                        if (parsedMsg != null) {
                            message = parsedMsg
                        }
                    }
                } catch (_: Exception) {
                    // Preserve HTTP fallback message when error body is not valid JSON.
                }
            }
            return GeminiApiException(
                httpStatus = httpStatus,
                apiStatus = apiStatus,
                message = message,
            )
        }
    }
}
