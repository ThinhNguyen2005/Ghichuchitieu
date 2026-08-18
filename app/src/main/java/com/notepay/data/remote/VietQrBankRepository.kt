package com.notepay.data.remote

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.notepay.domain.model.VietQrBank
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Loads the VietQR bank snapshot bundled with the app.
 */
@Singleton
class VietQrBankRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var cached: List<VietQrBank>? = null

    /**
     * Returns the bundled bank list, using the in-memory cache if available.
     * Falls back to [FALLBACK] if the bundled snapshot cannot be read.
     */
    suspend fun getBanks(): List<VietQrBank> = withContext(Dispatchers.IO) {
        cached ?: loadAndCache()
    }

    private fun loadAndCache(): List<VietQrBank> {
        return try {
            val body = context.assets.open(ASSET_FILE).bufferedReader().use { it.readText() }
            val result = parseBundledBanks(body)
            cached = result
            result
        } catch (e: Exception) {
            FALLBACK
        }
    }

    companion object {
        private const val ASSET_FILE = "vietqr/banks.json"

        /** Comprehensive offline fallback bank list used when API is unreachable. */
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
            VietQrBank(34, "970403", "STB", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín", "https://cdn.vietqr.io/img/STB.png", true),
            VietQrBank(41, "970441", "VIB", "VIB", "Ngân hàng TMCP Quốc tế Việt Nam", "https://cdn.vietqr.io/img/VIB.png", true),
            VietQrBank(22, "970431", "MSB", "MSB", "Ngân hàng TMCP Hàng Hải Việt Nam", "https://cdn.vietqr.io/img/MSB.png", true),
            VietQrBank(26, "970448", "OCB", "OCB", "Ngân hàng TMCP Phương Đông", "https://cdn.vietqr.io/img/OCB.png", true),
            VietQrBank(19, "970449", "LPB", "LPBank", "Ngân hàng TMCP Lộc Phát Việt Nam", "https://cdn.vietqr.io/img/LPB.png", true),
            VietQrBank(33, "970424", "SHB", "ShinhanBank", "Ngân hàng TNHH MTV Shinhan Việt Nam", "https://cdn.vietqr.io/img/SHB.png", true),
            VietQrBank(50, "970454", "CAKE", "Cake", "Ngân hàng số Cake by VPBank", "https://cdn.vietqr.io/img/CAKE.png", true),
            VietQrBank(51, "970458", "TIMO", "Timo", "Ngân hàng số Timo", "https://cdn.vietqr.io/img/TIMO.png", true),
        )
    }
}

internal fun parseBundledBanks(body: String): List<VietQrBank> {
    val banks = JSONObject(body).getJSONArray("banks")
    return buildList {
        for (index in 0 until banks.length()) {
            val bank = banks.getJSONObject(index)
            val bin = bank.optString("bin").takeIf { it.isNotBlank() } ?: continue
            val logoAssetPath = bank.optString("logoAssetPath")
            add(
                VietQrBank(
                    id = bank.optInt("id"),
                    bin = bin,
                    code = bank.optString("code"),
                    shortName = bank.optString("shortName"),
                    name = bank.optString("name"),
                    logoUrl = logoAssetPath.takeIf { it.isNotBlank() }
                        ?.let { "file:///android_asset/$it" }
                        .orEmpty(),
                    transferSupported = bank.optBoolean("transferSupported"),
                ),
            )
        }
    }.filter { it.transferSupported }
}
