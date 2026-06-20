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

        // 4. Nhóm Ví điện tử & Trung gian thanh toán
        KnownBankApp("com.mservice.momotransfer", "MoMo"), // [cite: 19]
        KnownBankApp("vn.com.vng.zalopay", "ZaloPay"), // [cite: 20]
        KnownBankApp("com.beeasy.toppay", "ShopeePay"), // [cite: 7]
        KnownBankApp("vnpay.smartacccount", "VNPay"), // [cite: 49, 50]
        KnownBankApp("com.viettelpay.android", "Viettel Money") // [cite: 1.2.1]
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

data class NotificationSettings(
    val autoCaptureEnabled: Boolean = true,
    val enabledPackages: Set<String> = KnownBankApps.packages,
    val customBankApps: Set<String> = emptySet(),
    /** Hạn mức chi tiêu tháng (VND * 100). 0 = chưa cài, bỏ qua Budget Alert. */
    val monthlyBudgetCents: Long = 0L,
)

private val Context.notificationSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "notification_settings",
)

@Singleton
class NotificationSettingsStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.notificationSettingsDataStore

    val settings: Flow<NotificationSettings> = dataStore.data.map { preferences ->
        NotificationSettings(
            autoCaptureEnabled = preferences[Keys.AUTO_CAPTURE_ENABLED] ?: true,
            enabledPackages = preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.packages,
            customBankApps = preferences[Keys.CUSTOM_BANK_APPS] ?: emptySet(),
            monthlyBudgetCents = preferences[Keys.MONTHLY_BUDGET_CENTS] ?: 0L,
        )
    }

    suspend fun setAutoCaptureEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_CAPTURE_ENABLED] = enabled
        }
    }

    suspend fun setPackageEnabled(packageName: String, enabled: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.packages
            val packagesToModify = KnownBankApps.equivalentPackages[packageName] ?: listOf(packageName)
            preferences[Keys.ENABLED_PACKAGES] = if (enabled) {
                current + packagesToModify
            } else {
                current - packagesToModify
            }
        }
    }

    suspend fun addCustomBankApp(packageName: String, label: String) {
        dataStore.edit { preferences ->
            val currentCustom = preferences[Keys.CUSTOM_BANK_APPS] ?: emptySet()
            // Tránh trùng lặp package
            val filteredCustom = currentCustom.filterNot { it.startsWith("$packageName|") }.toSet()
            preferences[Keys.CUSTOM_BANK_APPS] = filteredCustom + "$packageName|$label"

            // Tự động enable package này
            val currentEnabled = preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.packages
            val packagesToModify = KnownBankApps.equivalentPackages[packageName] ?: listOf(packageName)
            preferences[Keys.ENABLED_PACKAGES] = currentEnabled + packagesToModify
        }
    }

    suspend fun removeCustomBankApp(packageName: String) {
        dataStore.edit { preferences ->
            val currentCustom = preferences[Keys.CUSTOM_BANK_APPS] ?: emptySet()
            val entryToRemove = currentCustom.find { it.startsWith("$packageName|") }
            if (entryToRemove != null) {
                preferences[Keys.CUSTOM_BANK_APPS] = currentCustom - entryToRemove
            }

            // Hủy enable package này
            val currentEnabled = preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.packages
            val packagesToModify = KnownBankApps.equivalentPackages[packageName] ?: listOf(packageName)
            preferences[Keys.ENABLED_PACKAGES] = currentEnabled - packagesToModify
        }
    }

    suspend fun setMonthlyBudget(amountCents: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.MONTHLY_BUDGET_CENTS] = amountCents
        }
    }

    private object Keys {
        val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_save_notifications")
        val ENABLED_PACKAGES = stringSetPreferencesKey("enabled_notification_packages")
        val CUSTOM_BANK_APPS = stringSetPreferencesKey("custom_bank_apps")
        val MONTHLY_BUDGET_CENTS = androidx.datastore.preferences.core.longPreferencesKey("monthly_budget_cents")
    }
}
