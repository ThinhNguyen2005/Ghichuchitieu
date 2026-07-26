package com.notepay.data.remote

import com.notepay.domain.model.VietQrBank
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Fetches and caches the official VietQR bank list from api.vietqr.io/v2/banks.
 * Results are cached in memory for the app session — the list rarely changes.
 */
@Singleton
class VietQrBankRepository @Inject constructor() {

    @Volatile
    private var cached: List<VietQrBank>? = null

    /**
     * Returns the bank list, using the in-memory cache if available.
     * Falls back to [FALLBACK] on network failure so the UI is never empty.
     */
    suspend fun getBanks(): List<VietQrBank> = withContext(Dispatchers.IO) {
        cached ?: fetchAndCache()
    }

    private fun fetchAndCache(): List<VietQrBank> {
        return try {
            val url = URL("https://api.vietqr.io/v2/banks")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/json")

            val body = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            val json = JSONObject(body)
            val arr = json.getJSONArray("data")
            val banks = mutableListOf<VietQrBank>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val bin = obj.optString("bin").takeIf { it.isNotBlank() } ?: continue
                val transfer = obj.optInt("transferSupported", 0) == 1
                banks += VietQrBank(
                    id = obj.optInt("id"),
                    bin = bin,
                    code = obj.optString("code"),
                    shortName = obj.optString("shortName"),
                    name = obj.optString("name"),
                    logoUrl = "https://cdn.vietqr.io/img/${obj.optString("code")}.png",
                    transferSupported = transfer,
                )
            }
            val result = banks.filter { it.transferSupported }
            cached = result
            result
        } catch (e: Exception) {
            FALLBACK
        }
    }

    companion object {
        /** Minimal fallback list used when the API is unreachable. */
        val FALLBACK: List<VietQrBank> = listOf(
            VietQrBank(39, "970423", "TPB", "TPBank", "Ngân hàng TMCP Tiên Phong", "https://cdn.vietqr.io/img/TPB.png", true),
            VietQrBank(43, "970436", "VCB", "Vietcombank", "Ngân hàng TMCP Ngoại Thương Việt Nam", "https://cdn.vietqr.io/img/VCB.png", true),
            VietQrBank(17, "970415", "ICB", "VietinBank", "Ngân hàng TMCP Công thương Việt Nam", "https://cdn.vietqr.io/img/ICB.png", true),
            VietQrBank(4, "970418", "BIDV", "BIDV", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam", "https://cdn.vietqr.io/img/BIDV.png", true),
            VietQrBank(42, "970405", "VBA", "Agribank", "Ngân hàng Nông nghiệp và Phát triển Nông thôn Việt Nam", "https://cdn.vietqr.io/img/VBA.png", true),
            VietQrBank(21, "970422", "MB", "MBBank", "Ngân hàng TMCP Quân đội", "https://cdn.vietqr.io/img/MB.png", true),
            VietQrBank(38, "970407", "TCB", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam", "https://cdn.vietqr.io/img/TCB.png", true),
            VietQrBank(2, "970416", "ACB", "ACB", "Ngân hàng TMCP Á Châu", "https://cdn.vietqr.io/img/ACB.png", true),
            VietQrBank(47, "970432", "VPB", "VPBank", "Ngân hàng TMCP Việt Nam Thịnh Vượng", "https://cdn.vietqr.io/img/VPB.png", true),
        )
    }
}
