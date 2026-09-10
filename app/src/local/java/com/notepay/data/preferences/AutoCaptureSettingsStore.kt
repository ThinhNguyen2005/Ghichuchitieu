package com.notepay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class KnownBankApp(
    val packageName: String,
    val label: String,
)

object KnownBankApps {
    const val VCB_PACKAGE = "com.VCB"
    const val VIETINBANK_PACKAGE = "com.vietinbank.ipay"
    const val MBBANK_PACKAGE = "com.mbmobile"
    const val TPBANK_PACKAGE = "com.tpb.mb.gprsandroid"

    // Danh sách hiển thị trực quan trên giao diện ứng dụng (Đã chuẩn hóa & Bổ sung)
    val displayApps = listOf(
        // 1. Nhóm Ngân hàng Thương mại Nhà nước (Big 4)
        KnownBankApp("com.VCB", "Vietcombank"), // [cite: 6]
        KnownBankApp("com.vietinbank.ipay", "VietinBank"), // [cite: 24]
        KnownBankApp("com.vnpay.bidv", "BIDV"), // [cite: 4]
        KnownBankApp("com.vnpay.Agribank3g", "Agribank"), // [cite: 3]

        // 2. Nhóm Ngân hàng Thương mại Cổ phần Tư nhân
        KnownBankApp("vn.com.techcombank.bb.app", "Techcombank"), // [cite: 13]
        KnownBankApp("com.mbmobile", "MB Bank"), // [cite: 14, 15]
        KnownBankApp("com.vnpay.vpbankonline", "VPBank"), // [cite: 5]
        KnownBankApp("com.tpb.mb.gprsandroid", "TPBank"), // [cite: 11, 12]
        KnownBankApp("mobile.acb.com.vn", "ACB"), // [cite: 8]
        KnownBankApp("src.com.sacombank", "Sacombank"), // [cite: 16, 17]
        KnownBankApp("com.vib.myvib2", "VIB"),
        KnownBankApp("com.msb.digibank.retail", "MSB"),
        KnownBankApp("vn.com.ocb.awe", "OCB"),
        KnownBankApp("vn.lienviet.app", "LPBank"),
        KnownBankApp("com.shinhan.global.vn.bank", "Shinhan Bank"),

        // 3. Nhóm Ngân hàng số (Digital Banks)
        KnownBankApp("com.ocb.liobank", "Liobank"),
        KnownBankApp("xyz.be.cake", "Cake by VPBank"),
        KnownBankApp("vn.banvietbank.mobilebanking", "Digimi"),
        KnownBankApp("io.lifestyle.plus", "Timo"),

    )

    val apps = displayApps

    // Ánh xạ các package tương đương (kế thừa lịch sử hoặc sai biệt do phỏng đoán cũ) về Package chuẩn
    val equivalentPackages = mapOf(
        "com.VCB" to listOf("com.VCB", "com.vietcombank.digibank", "com.vietcombank.vietcombankdetail", "com.vietcombank.cardoproduct", "com.vcbbobile"), // [cite: 6]
        "com.vietinbank.ipay" to listOf("com.vietinbank.ipay"), // [cite: 24]
        "com.vnpay.bidv" to listOf("com.vnpay.bidv", "com.bidv.smartbanking"), // [cite: 4]
        "com.vnpay.Agribank3g" to listOf("com.vnpay.Agribank3g", "vn.com.agribank.emobilebanking", "com.vnpay.agribank", "com.agribank.smartbanking"), // [cite: 3]
        "vn.com.techcombank.bb.app" to listOf("vn.com.techcombank.bb.app", "com.technologies.tcb", "com.technologiessoftech.tcb", "vn.com.techcombank.identity"), // [cite: 13]
        "com.mbmobile" to listOf("com.mbmobile"), // [cite: 14, 15]
        "com.vnpay.vpbankonline" to listOf("com.vnpay.vpbankonline", "com.vpbank.neo", "com.vnpay.vpbank"), // [cite: 5]
        "com.tpb.mb.gprsandroid" to listOf("com.tpb.mb.gprsandroid", "com.tpb.mb.android", "com.tpbank", "com.tpb.mbanking"), // [cite: 11, 12]
        "mobile.acb.com.vn" to listOf("mobile.acb.com.vn", "vn.com.acb.mbanking", "acb.app.acbone", "com.acb.dcb"), // [cite: 8]
        "src.com.sacombank" to listOf("src.com.sacombank", "com.sacombank.mbanking", "com.sacombank.ewallet", "com.sacombank.isacombank", "com.sacombank.mb"), // [cite: 16, 17, 18]
        "com.vib.myvib2" to listOf("com.vib.myvib2"),
        "com.msb.digibank.retail" to listOf("com.msb.digibank.retail"),
        "vn.com.ocb.awe" to listOf("vn.com.ocb.awe"),
        "vn.lienviet.app" to listOf("vn.lienviet.app"),
        "com.shinhan.global.vn.bank" to listOf("com.shinhan.global.vn.bank"),
        "com.ocb.liobank" to listOf("com.ocb.liobank"),
        "xyz.be.cake" to listOf("xyz.be.cake"),
        "vn.banvietbank.mobilebanking" to listOf("vn.banvietbank.mobilebanking"),
        "io.lifestyle.plus" to listOf("io.lifestyle.plus"),
        "com.mservice.momotransfer" to listOf("com.mservice.momotransfer", "com.mservice.momo", "com.mservice.momoandlending"), // [cite: 19]
        "vn.com.vng.zalopay" to listOf("vn.com.vng.zalopay"), // [cite: 20]
        "com.beeasy.toppay" to listOf("com.beeasy.toppay", "com.shopeepay.vn", "com.beeasy.airpay"), // [cite: 7, 2.1.4, 2.1.8]
        "vnpay.smartacccount" to listOf("vnpay.smartacccount"), // [cite: 49, 50]
        "com.viettelpay.android" to listOf("com.viettelpay.android", "com.viettel.viettelmoney", "com.bplus.vtpay") // [cite: 1.2.1, 22]
    )

    val packages = equivalentPackages.values.flatten().toSet()

    /**
     * Bank apps that are recognised by the local capture surface.
     *
     * This is intentionally based on the configured display list, not a claim that
     * every Vietnamese bank package is known. New/uncertain bank formats can still
     * be routed to pending confirmation without being auto-confirmed.
     */
    val recognizedBankPrimaryPackages: Set<String> = displayApps
        .map { it.packageName }
        .toSet()

    /** Bank packages whose transaction templates are currently verified for auto-confirm. */
    val verifiedPrimaryPackages: Set<String> = setOf(
        VCB_PACKAGE,
        VIETINBANK_PACKAGE,
        MBBANK_PACKAGE,
        TPBANK_PACKAGE,
    )

    private val walletPrimaryPackages = setOf(
        "com.mservice.momotransfer",
        "vn.com.vng.zalopay",
        "com.beeasy.toppay",
        "vnpay.smartacccount",
        "com.viettelpay.android",
    )

    private val excludedWalletPackages: Set<String> = walletPrimaryPackages
        .flatMap { equivalentPackages[it].orEmpty() }
        .plus("com.sacombank.ewallet")
        .toSet()

    val recognizedBankPackages: Set<String> = recognizedBankPrimaryPackages
        .flatMap { equivalentPackages[it].orEmpty() }
        .filterNot(excludedWalletPackages::contains)
        .toSet()

    val verifiedPackages: Set<String> = verifiedPrimaryPackages
        .flatMap { equivalentPackages[it].orEmpty() }
        .filterNot(excludedWalletPackages::contains)
        .toSet()

    /**
     * Compatibility aliases for older callers. "Supported" now means recognised
     * bank source (including pending-only banks), not verified auto-confirm source.
     */
    val supportedPrimaryPackages: Set<String> = recognizedBankPrimaryPackages
    val supportedPackages: Set<String> = recognizedBankPackages

    fun isRecognized(packageName: String): Boolean =
        packageName !in excludedWalletPackages &&
            getPrimaryPackageName(packageName) in recognizedBankPrimaryPackages

    fun isVerified(packageName: String): Boolean =
        packageName !in excludedWalletPackages &&
            getPrimaryPackageName(packageName) in verifiedPrimaryPackages

    fun isSupported(packageName: String): Boolean = isRecognized(packageName)

    fun normalizeSupportedPackages(packageNames: Iterable<String>): Set<String> =
        packageNames
            .filter(::isRecognized)
            .map(::getPrimaryPackageName)
            .filter(recognizedBankPrimaryPackages::contains)
            .flatMap { equivalentPackages[it].orEmpty() }
            .filterNot(excludedWalletPackages::contains)
            .toSet()

    // Chuyển đổi an toàn mọi định danh phụ/sai lệch về Package chính thức
    fun getPrimaryPackageName(packageName: String): String {
        return when (packageName) {
            in listOf("com.VCB", "com.vietcombank.digibank", "com.vietcombank.vietcombankdetail", "com.vietcombank.cardoproduct", "com.vcbbobile") -> "com.VCB" // [cite: 6]
            in listOf("com.tpb.mb.gprsandroid", "com.tpb.mb.android", "com.tpbank", "com.tpb.mbanking") -> "com.tpb.mb.gprsandroid" // [cite: 11, 12]
            in listOf("vn.com.techcombank.bb.app", "com.technologies.tcb", "com.technologiessoftech.tcb", "vn.com.techcombank.identity") -> "vn.com.techcombank.bb.app" // [cite: 13]
            in listOf("com.vnpay.bidv", "com.bidv.smartbanking") -> "com.vnpay.bidv" // [cite: 4]
            in listOf("com.vnpay.Agribank3g", "vn.com.agribank.emobilebanking", "com.vnpay.agribank", "com.agribank.smartbanking") -> "com.vnpay.Agribank3g" // [cite: 3]
            in listOf("src.com.sacombank", "com.sacombank.mbanking", "com.sacombank.ewallet", "com.sacombank.isacombank", "com.sacombank.mb") -> "src.com.sacombank" // [cite: 16, 17, 18]
            in listOf("mobile.acb.com.vn", "vn.com.acb.mbanking", "acb.app.acbone", "com.acb.dcb") -> "mobile.acb.com.vn" // [cite: 8]
            in listOf("com.vnpay.vpbankonline", "com.vpbank.neo", "com.vnpay.vpbank") -> "com.vnpay.vpbankonline" // [cite: 5]
            in listOf("com.mservice.momotransfer", "com.mservice.momo", "com.mservice.momoandlending") -> "com.mservice.momotransfer" // [cite: 19]
            in listOf("com.beeasy.toppay", "com.shopeepay.vn", "com.beeasy.airpay") -> "com.beeasy.toppay" // [cite: 7, 2.1.4, 2.1.8]
            in listOf("com.viettelpay.android", "com.viettel.viettelmoney", "com.bplus.vtpay") -> "com.viettelpay.android" // [cite: 1.2.1, 22]
            else -> packageName
        }
    }
}


data class AutoCaptureSettings(
    val autoCaptureEnabled: Boolean = false,
    val enabledPackages: Set<String> = KnownBankApps.supportedPackages,
)

private val Context.autoCaptureSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "auto_capture_settings",
)

@Singleton
class AutoCaptureSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.autoCaptureSettingsDataStore

    val settings: Flow<AutoCaptureSettings> = dataStore.data.map { preferences ->
        AutoCaptureSettings(
            autoCaptureEnabled = preferences[Keys.AUTO_CAPTURE_ENABLED] ?: false,
            enabledPackages = KnownBankApps.normalizeSupportedPackages(
                preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.supportedPackages,
            ),
        )
    }

    suspend fun setAutoCaptureEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_CAPTURE_ENABLED] = enabled
        }
    }

    suspend fun setPackageEnabled(packageName: String, enabled: Boolean) {
        dataStore.edit { preferences ->
            val current = KnownBankApps.normalizeSupportedPackages(
                preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.supportedPackages,
            )
            val primaryPackageName = KnownBankApps.getPrimaryPackageName(packageName)
            val packagesToModify = KnownBankApps.equivalentPackages[primaryPackageName]
                ?.filter(KnownBankApps::isRecognized)
                ?: emptyList()
            preferences[Keys.ENABLED_PACKAGES] = if (enabled && KnownBankApps.isSupported(packageName)) {
                current + packagesToModify
            } else {
                current - packagesToModify
            }
        }
    }

    private object Keys {
        val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_capture_enabled")
        val ENABLED_PACKAGES = stringSetPreferencesKey("enabled_auto_capture_packages")
    }
}

