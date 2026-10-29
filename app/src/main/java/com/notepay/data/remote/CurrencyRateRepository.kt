package com.notepay.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class CurrencyRateSnapshot(
    val baseCode: String,
    val rates: Map<String, BigDecimal>,
    val lastUpdatedUtc: String,
    val nextUpdateEpochSeconds: Long,
)

@Singleton
class CurrencyRateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    suspend fun getVndRates(forceRefresh: Boolean = false): CurrencyRateSnapshot =
        withContext(Dispatchers.IO) {
            val cached = readCache()
            val nowSeconds = System.currentTimeMillis() / 1_000L
            if (!forceRefresh && cached != null && nowSeconds < cached.nextUpdateEpochSeconds) {
                return@withContext cached
            }

            try {
                val fresh = fetchRates()
                writeCache(fresh)
                fresh
            } catch (error: Exception) {
                cached ?: throw error
            }
        }

    private fun fetchRates(): CurrencyRateSnapshot {
        val connection = (URL("$BASE_URL/latest/VND").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NotePay-Android")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            if (status !in 200..299) {
                throw IllegalStateException("Exchange rate service returned HTTP $status")
            }

            val json = JSONObject(body)
            if (json.optString("result") != "success") {
                throw IllegalStateException("Exchange rate service returned an unsuccessful result")
            }

            val base = json.optString("base_code")
            require(base == BASE_CURRENCY) { "Unexpected base currency: $base" }

            val rawRates = json.getJSONObject("rates")
            val requiredCodes = listOf("USD", "EUR", "JPY", "KRW")
            val rates = requiredCodes.associateWith { code ->
                val value = rawRates.getDouble(code)
                require(value.isFinite() && value > 0.0) { "Invalid exchange rate for $code" }
                BigDecimal(rawRates.get(code).toString())
            }

            return CurrencyRateSnapshot(
                baseCode = base,
                rates = rates,
                lastUpdatedUtc = json.optString("time_last_update_utc", "Unknown"),
                nextUpdateEpochSeconds = json.optLong(
                    "time_next_update_unix",
                    System.currentTimeMillis() / 1_000L + DEFAULT_CACHE_SECONDS,
                ),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun readCache(): CurrencyRateSnapshot? = runCatching {
        val encodedRates = preferences.getString(KEY_RATES, null) ?: return null
        val json = JSONObject(encodedRates)
        val rates = buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val code = keys.next()
                put(code, BigDecimal(json.getString(code)))
            }
        }
        if (listOf("USD", "EUR", "JPY", "KRW").any { it !in rates }) return null

        CurrencyRateSnapshot(
            baseCode = BASE_CURRENCY,
            rates = rates,
            lastUpdatedUtc = preferences.getString(KEY_LAST_UPDATED, "Unknown") ?: "Unknown",
            nextUpdateEpochSeconds = preferences.getLong(KEY_NEXT_UPDATE, 0L),
        )
    }.getOrNull()

    private fun writeCache(snapshot: CurrencyRateSnapshot) {
        val json = JSONObject()
        snapshot.rates.forEach { (code, rate) -> json.put(code, rate.toPlainString()) }
        preferences.edit()
            .putString(KEY_RATES, json.toString())
            .putString(KEY_LAST_UPDATED, snapshot.lastUpdatedUtc)
            .putLong(KEY_NEXT_UPDATE, snapshot.nextUpdateEpochSeconds)
            .apply()
    }

    companion object {
        private const val BASE_URL = "https://open.er-api.com/v6"
        private const val BASE_CURRENCY = "VND"
        private const val PREFERENCES_NAME = "currency_rate_cache"
        private const val KEY_RATES = "rates"
        private const val KEY_LAST_UPDATED = "last_updated_utc"
        private const val KEY_NEXT_UPDATE = "next_update_epoch_seconds"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val DEFAULT_CACHE_SECONDS = 86_400L
    }
}
