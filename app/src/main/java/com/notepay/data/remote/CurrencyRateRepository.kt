package com.notepay.data.remote

import android.content.Context
import androidx.core.content.edit
import com.notepay.domain.model.CurrencyConverter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.math.BigDecimal
import java.math.MathContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class CurrencyRateSnapshot(
    val baseCode: String,
    val rates: Map<String, BigDecimal>,
    val lastUpdatedUtc: String,
    val nextUpdateEpochSeconds: Long,
    val isOfflineFallback: Boolean = false,
    val sourceName: String = CurrencyRateRepository.SOURCE_ER_API,
)

@Singleton
class CurrencyRateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val preferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    suspend fun getVndRates(forceRefresh: Boolean = false): CurrencyRateSnapshot =
        withContext(Dispatchers.IO) {
            val cached = readCachedSnapshot()
            val nowSeconds = System.currentTimeMillis() / 1_000L
            if (!forceRefresh && cached != null && nowSeconds < cached.nextUpdateEpochSeconds) {
                return@withContext cached
            }

            try {
                val fresh = fetchWithFallback()
                writeCache(fresh)
                fresh
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                cached ?: throw error
            }
        }

    suspend fun getVndRatesOrFallback(forceRefresh: Boolean = false): CurrencyRateSnapshot =
        try {
            getVndRates(forceRefresh)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            withContext(Dispatchers.IO) { readCachedSnapshot() } ?: offlineFallbackSnapshot()
        }

    fun readCachedSnapshot(): CurrencyRateSnapshot? = runCatching {
        if (preferences.getInt(KEY_CACHE_VERSION, 0) != CACHE_VERSION) return null
        val encodedRates = preferences.getString(KEY_RATES, null) ?: return null
        val json = JSONObject(encodedRates)
        val rates = buildMap {
            val keys = json.keys()
            while (keys.hasNext()) {
                val code = keys.next().uppercase(Locale.ROOT)
                val value = runCatching { BigDecimal(json.getString(code)) }.getOrNull()
                if (value != null && value > BigDecimal.ZERO) {
                    put(code, value)
                }
            }
            put(BASE_CURRENCY, BigDecimal.ONE)
        }
        if (REQUIRED_CODES.any { it !in rates }) return null

        CurrencyRateSnapshot(
            baseCode = BASE_CURRENCY,
            rates = rates,
            lastUpdatedUtc = preferences.getString(KEY_LAST_UPDATED, "") ?: "",
            nextUpdateEpochSeconds = preferences.getLong(KEY_NEXT_UPDATE, 0L),
            isOfflineFallback = false,
            sourceName = preferences.getString(KEY_SOURCE_NAME, SOURCE_ER_API) ?: SOURCE_ER_API,
        )
    }.getOrNull()

    fun offlineFallbackSnapshot(): CurrencyRateSnapshot {
        val rates = CurrencyConverter.supportedCurrencies.associate { info ->
            val rate = if (info.code.equals(BASE_CURRENCY, ignoreCase = true)) {
                BigDecimal.ONE
            } else {
                BigDecimal.ONE.divide(BigDecimal.valueOf(info.approximateRateToVnd), MathContext.DECIMAL128)
            }
            info.code.uppercase(Locale.ROOT) to rate
        }
        return CurrencyRateSnapshot(
            baseCode = BASE_CURRENCY,
            rates = rates,
            lastUpdatedUtc = "",
            nextUpdateEpochSeconds = 0L,
            isOfflineFallback = true,
            sourceName = SOURCE_OFFLINE,
        )
    }

    private fun fetchWithFallback(): CurrencyRateSnapshot = try {
        fetchFromErApiUsd()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (primaryError: Exception) {
        try {
            fetchFromFawazApi()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (backupError: Exception) {
            primaryError.addSuppressed(backupError)
            throw primaryError
        }
    }

    private fun fetchFromErApiUsd(): CurrencyRateSnapshot {
        val json = httpGetJson(ER_API_USD_URL)
        if (json.optString("result") != "success") {
            throw IllegalStateException("Exchange rate service returned an unsuccessful result")
        }
        val base = json.optString("base_code")
        require(base == "USD") { "Unexpected base currency: $base" }

        val usdRates = parsePositiveRates(json.getJSONObject("rates"))
        val vndPerUsd = usdRates[BASE_CURRENCY]
            ?.takeIf { it > BigDecimal.ZERO }
            ?: throw IllegalStateException("Missing VND rate in USD snapshot")

        val vndBasedRates = buildMap {
            usdRates.forEach { (code, ratePerUsd) ->
                put(code, ratePerUsd.divide(vndPerUsd, MathContext.DECIMAL128))
            }
            put(BASE_CURRENCY, BigDecimal.ONE)
            put("USD", BigDecimal.ONE.divide(vndPerUsd, MathContext.DECIMAL128))
        }
        require(REQUIRED_CODES.all { it in vndBasedRates }) {
            "Missing required exchange rates in ExchangeRate-API response"
        }

        val nowSeconds = System.currentTimeMillis() / 1_000L
        return CurrencyRateSnapshot(
            baseCode = BASE_CURRENCY,
            rates = vndBasedRates,
            lastUpdatedUtc = json.optString("time_last_update_utc", ""),
            nextUpdateEpochSeconds = json.optLong("time_next_update_unix", nowSeconds + DEFAULT_CACHE_SECONDS),
            isOfflineFallback = false,
            sourceName = SOURCE_ER_API,
        )
    }

    private fun fetchFromFawazApi(): CurrencyRateSnapshot {
        val json = httpGetJson(FAWAZ_VND_URL)
        val rawVnd = json.getJSONObject("vnd")
        val vndBasedRates = buildMap {
            putAll(parsePositiveRates(rawVnd))
            put(BASE_CURRENCY, BigDecimal.ONE)
        }
        require(REQUIRED_CODES.all { it in vndBasedRates }) {
            "Missing required exchange rates in Currency-API response"
        }

        val nowSeconds = System.currentTimeMillis() / 1_000L
        return CurrencyRateSnapshot(
            baseCode = BASE_CURRENCY,
            rates = vndBasedRates,
            lastUpdatedUtc = json.optString("date", ""),
            nextUpdateEpochSeconds = nowSeconds + DEFAULT_CACHE_SECONDS,
            isOfflineFallback = false,
            sourceName = SOURCE_FAWAZ_API,
        )
    }

    private fun parsePositiveRates(rawRates: JSONObject): Map<String, BigDecimal> = buildMap {
        val keys = rawRates.keys()
        while (keys.hasNext()) {
            val rawKey = keys.next()
            val codeUpper = rawKey.uppercase(Locale.ROOT)
            runCatching {
                val decimal = BigDecimal(rawRates.get(rawKey).toString())
                if (decimal > BigDecimal.ZERO) {
                    put(codeUpper, decimal)
                }
            }
        }
    }

    private fun httpGetJson(endpoint: String): JSONObject {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "NotePay-Android")
        }

        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw IllegalStateException("Exchange rate service returned HTTP $status")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun writeCache(snapshot: CurrencyRateSnapshot) {
        val json = JSONObject()
        snapshot.rates.forEach { (code, rate) -> json.put(code, rate.toPlainString()) }
        preferences.edit {
            putInt(KEY_CACHE_VERSION, CACHE_VERSION)
            putString(KEY_RATES, json.toString())
            putString(KEY_LAST_UPDATED, snapshot.lastUpdatedUtc)
            putLong(KEY_NEXT_UPDATE, snapshot.nextUpdateEpochSeconds)
            putString(KEY_SOURCE_NAME, snapshot.sourceName)
        }
    }

    companion object {
        const val SOURCE_ER_API = "ExchangeRate-API"
        const val SOURCE_FAWAZ_API = "Currency-API"
        const val SOURCE_OFFLINE = "Offline"

        private const val ER_API_USD_URL = "https://open.er-api.com/v6/latest/USD"
        private const val FAWAZ_VND_URL =
            "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/vnd.json"
        private const val BASE_CURRENCY = "VND"
        private const val PREFERENCES_NAME = "currency_rate_cache"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val KEY_RATES = "rates"
        private const val KEY_LAST_UPDATED = "last_updated_utc"
        private const val KEY_NEXT_UPDATE = "next_update_epoch_seconds"
        private const val KEY_SOURCE_NAME = "source_name"
        private const val CACHE_VERSION = 2
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 10_000
        private const val DEFAULT_CACHE_SECONDS = 86_400L

        private val REQUIRED_CODES = listOf("USD", "EUR", "JPY", "GBP", "KRW", "SGD", "THB")
    }
}
