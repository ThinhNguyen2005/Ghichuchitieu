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
    val displayApps = listOf(
        KnownBankApp("com.tpb.mb.gprsandroid", "TPBank"),
        KnownBankApp("com.VCB", "Vietcombank"),
        KnownBankApp("com.technologies.tcb", "Techcombank"),
        KnownBankApp("com.mbmobile", "MB Bank"),
        KnownBankApp("com.vpbank.neo", "VPBank"),
        KnownBankApp("com.bidv.smartbanking", "BIDV"),
        KnownBankApp("com.vnpay.Agribank3g", "Agribank"),
        KnownBankApp("com.sacombank.mbanking", "Sacombank"),
        KnownBankApp("vn.com.acb.mbanking", "ACB"),
        KnownBankApp("com.mservice.momotransfer", "MoMo"),
    )

    val apps = displayApps

    val equivalentPackages = mapOf(
        "com.tpb.mb.gprsandroid" to listOf("com.tpb.mb.gprsandroid", "com.tpb.mb.android", "com.tpbank"),
        "com.VCB" to listOf("com.VCB", "com.vietcombank.digibank", "com.vietcombank.vietcombankdetail", "com.vietcombank.cardoproduct"),
        "com.technologies.tcb" to listOf("com.technologies.tcb", "com.technologiessoftech.tcb"),
        "com.mbmobile" to listOf("com.mbmobile"),
        "com.vpbank.neo" to listOf("com.vpbank.neo"),
        "com.bidv.smartbanking" to listOf("com.bidv.smartbanking", "com.vnpay.bidv"),
        "com.vnpay.Agribank3g" to listOf("com.vnpay.Agribank3g", "vn.com.agribank.emobilebanking", "com.vnpay.agribank"),
        "com.sacombank.mbanking" to listOf("com.sacombank.mbanking", "com.sacombank.ewallet", "com.sacombank.isacombank", "com.sacombank.mb"),
        "vn.com.acb.mbanking" to listOf("vn.com.acb.mbanking", "acb.app.acbone", "com.acb.dcb"),
        "com.mservice.momotransfer" to listOf("com.mservice.momotransfer", "com.mservice.momo"),
    )

    val packages = equivalentPackages.values.flatten().toSet()

    fun getPrimaryPackageName(packageName: String): String {
        return when (packageName) {
            in listOf("com.VCB", "com.vietcombank.digibank", "com.vietcombank.vietcombankdetail", "com.vietcombank.cardoproduct") -> "com.VCB"
            in listOf("com.tpb.mb.gprsandroid", "com.tpb.mb.android", "com.tpbank") -> "com.tpb.mb.gprsandroid"
            in listOf("com.technologies.tcb", "com.technologiessoftech.tcb") -> "com.technologies.tcb"
            in listOf("com.bidv.smartbanking", "com.vnpay.bidv") -> "com.bidv.smartbanking"
            in listOf("com.vnpay.Agribank3g", "vn.com.agribank.emobilebanking", "com.vnpay.agribank") -> "com.vnpay.Agribank3g"
            in listOf("com.sacombank.mbanking", "com.sacombank.ewallet", "com.sacombank.isacombank", "com.sacombank.mb") -> "com.sacombank.mbanking"
            in listOf("vn.com.acb.mbanking", "acb.app.acbone", "com.acb.dcb") -> "vn.com.acb.mbanking"
            in listOf("com.mservice.momotransfer", "com.mservice.momo") -> "com.mservice.momotransfer"
            else -> packageName
        }
    }
}

data class NotificationSettings(
    val autoCaptureEnabled: Boolean = true,
    val trackAllBanks: Boolean = true,
    val enabledPackages: Set<String> = KnownBankApps.packages,
    val excludedPackages: Set<String> = emptySet(),
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
            trackAllBanks = preferences[Keys.TRACK_ALL_BANKS] ?: true,
            enabledPackages = preferences[Keys.ENABLED_PACKAGES] ?: KnownBankApps.packages,
            excludedPackages = preferences[Keys.EXCLUDED_PACKAGES] ?: emptySet(),
        )
    }

    suspend fun setAutoCaptureEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.AUTO_CAPTURE_ENABLED] = enabled
        }
    }

    suspend fun setTrackAllBanks(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.TRACK_ALL_BANKS] = enabled
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

    suspend fun setExcludedPackages(packages: Set<String>) {
        dataStore.edit { preferences ->
            preferences[Keys.EXCLUDED_PACKAGES] = packages
        }
    }

    private object Keys {
        val AUTO_CAPTURE_ENABLED = booleanPreferencesKey("auto_save_notifications")
        val TRACK_ALL_BANKS = booleanPreferencesKey("pref_track_all_banks")
        val ENABLED_PACKAGES = stringSetPreferencesKey("enabled_notification_packages")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_notification_packages")
    }
}
