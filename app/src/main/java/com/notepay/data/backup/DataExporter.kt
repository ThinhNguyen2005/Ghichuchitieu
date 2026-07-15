package com.notepay.data.backup

import android.content.Context
import android.net.Uri
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.preferences.NotificationSettingsStore
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.domain.usecase.SuggestCategoryUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val billSplitDao: BillSplitDao,
    private val subscriptionDao: SubscriptionDao,
    private val categoryRepository: CategoryRepositoryImpl,
    private val notificationSettingsStore: NotificationSettingsStore,
    @ApplicationContext private val context: Context,
) {
    suspend fun exportToJson(): String {
        val wallets = walletDao.getAll()
        val transactions = transactionDao.getAll()
        val billSplits = billSplitDao.getAll()
        val subscriptions = subscriptionDao.getAll()
        val categories = categoryRepository.getCategories().filter { it.isCustom }

        val prefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        val themeColor = prefs.getString("theme_color", "green") ?: "green"
        val themeCustomColor = prefs.getString("theme_custom_color", "#1B7F4F") ?: "#1B7F4F"

        val habitsPrefs = context.getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE)
        val habits = mutableMapOf<String, Int>()
        for ((k, v) in habitsPrefs.all) {
            if (v is Int) habits[k] = v
        }

        val settings = try {
            notificationSettingsStore.settings.first()
        } catch (_: Exception) {
            com.notepay.data.preferences.NotificationSettings()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val backup = BackupPackage(
            exportedAt = dateFormat.format(Date()),
            data = BackupData(
                wallets = wallets,
                transactions = transactions,
                billSplits = billSplits,
                subscriptions = subscriptions,
                customCategories = categories.map {
                    CustomCategoryDto(it.id, it.displayName, it.colorArgb, it.iconId, it.isIncome)
                },
                preferences = BackupPreferences(
                    themeColor = themeColor,
                    themeCustomColor = themeCustomColor,
                    autoCaptureEnabled = settings.autoCaptureEnabled,
                    enabledPackages = settings.enabledPackages,
                    customBankApps = settings.customBankApps,
                    categoryHabits = habits,
                ),
            ),
        )
        return backupToJson(backup)
    }

    suspend fun readFromFile(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: throw Exception("Không thể đọc file")
    }

    private fun backupToJson(backup: BackupPackage): String {
        val root = JSONObject()
        root.put("version", backup.version)
        root.put("exportedAt", backup.exportedAt)

        val data = JSONObject()

        // Wallets
        val walletsArr = JSONArray()
        for (w in backup.data.wallets) {
            walletsArr.put(JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("initialBalanceCents", w.initialBalanceCents)
                put("iconKey", w.iconKey)
                put("colorKey", w.colorKey)
                put("isActive", w.isActive)
                put("budgetLimitCents", w.budgetLimitCents ?: JSONObject.NULL)
                put("linkedPackageName", w.linkedPackageName ?: JSONObject.NULL)
                put("bankBin", w.bankBin ?: JSONObject.NULL)
                put("accountNumber", w.accountNumber ?: JSONObject.NULL)
                put("accountName", w.accountName ?: JSONObject.NULL)
                put("createdAt", w.createdAt)
            })
        }
        data.put("wallets", walletsArr)

        // Transactions
        val txArr = JSONArray()
        for (t in backup.data.transactions) {
            txArr.put(JSONObject().apply {
                put("id", t.id)
                put("amountCents", t.amountCents)
                put("type", t.type)
                put("category", t.category)
                put("note", t.note)
                put("occurredAt", t.occurredAt)
                put("walletId", t.walletId)
                put("createdAt", t.createdAt)
                put("isAutoCapture", t.isAutoCapture)
                put("isInternalTransfer", t.isInternalTransfer)
            })
        }
        data.put("transactions", txArr)

        // Bill splits
        val bsArr = JSONArray()
        for (b in backup.data.billSplits) {
            bsArr.put(JSONObject().apply {
                put("id", b.id)
                put("transactionId", b.transactionId)
                put("debtorName", b.debtorName)
                put("amountCents", b.amountCents)
                put("isPaid", b.isPaid)
                put("memoCode", b.memoCode)
                put("paidAt", b.paidAt ?: JSONObject.NULL)
                put("createdAt", b.createdAt)
            })
        }
        data.put("billSplits", bsArr)

        // Subscriptions
        val subArr = JSONArray()
        for (s in backup.data.subscriptions) {
            subArr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("amountCents", s.amountCents)
                put("category", s.category)
                put("nextDueDate", s.nextDueDate)
                put("repeatMonths", s.repeatMonths)
                put("remindDaysBefore", s.remindDaysBefore)
                put("note", s.note)
                put("isActive", s.isActive)
                put("createdAt", s.createdAt)
            })
        }
        data.put("subscriptions", subArr)

        // Custom categories
        val catArr = JSONArray()
        for (c in backup.data.customCategories) {
            catArr.put(JSONObject().apply {
                put("id", c.id)
                put("displayName", c.displayName)
                put("colorArgb", c.colorArgb)
                put("iconId", c.iconId)
                put("isIncome", c.isIncome)
            })
        }
        data.put("customCategories", catArr)

        // Preferences
        val prefsObj = JSONObject()
        prefsObj.put("themeColor", backup.data.preferences.themeColor)
        prefsObj.put("themeCustomColor", backup.data.preferences.themeCustomColor)
        prefsObj.put("autoCaptureEnabled", backup.data.preferences.autoCaptureEnabled)
        prefsObj.put("enabledPackages", JSONArray(backup.data.preferences.enabledPackages.toList()))
        prefsObj.put("customBankApps", JSONArray(backup.data.preferences.customBankApps.toList()))
        val habitsObj = JSONObject()
        for ((k, v) in backup.data.preferences.categoryHabits) {
            habitsObj.put(k, v)
        }
        prefsObj.put("categoryHabits", habitsObj)
        data.put("preferences", prefsObj)

        root.put("data", data)
        return root.toString(2)
    }
}
