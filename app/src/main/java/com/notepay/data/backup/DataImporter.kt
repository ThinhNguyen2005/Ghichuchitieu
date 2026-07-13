package com.notepay.data.backup

import android.content.Context
import android.net.Uri
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.preferences.NotificationSettingsStore
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.domain.model.Category
import com.notepay.domain.model.CategoryType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataImporter @Inject constructor(
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val billSplitDao: BillSplitDao,
    private val subscriptionDao: SubscriptionDao,
    private val categoryRepository: CategoryRepositoryImpl,
    private val notificationSettingsStore: NotificationSettingsStore,
    @ApplicationContext private val context: Context,
) {
    suspend fun readFromFile(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        } ?: throw Exception("Không thể đọc file")
    }

    /**
     * Import toàn bộ dữ liệu từ JSON. Clears dữ liệu cũ trước khi import.
     * Phải chạy trên IO dispatcher.
     */
    suspend fun importFromJson(jsonString: String) {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 0)
        require(version <= BACKUP_VERSION) { "Phiên bản backup không tương thích: v$version" }

        val data = root.getJSONObject("data")

        // 1. Clear dữ liệu cũ (thứ tự vì FK constraints)
        billSplitDao.deleteAll()
        transactionDao.deleteAll()
        subscriptionDao.deleteAll()
        walletDao.deleteAll()

        // 2. Import wallets trước (FK parent)
        val walletsArr = data.getJSONArray("wallets")
        for (i in 0 until walletsArr.length()) {
            val obj = walletsArr.getJSONObject(i)
            walletDao.upsert(WalletEntity(
                id = obj.getLong("id"),
                name = obj.getString("name"),
                initialBalanceCents = obj.getLong("initialBalanceCents"),
                iconKey = obj.getString("iconKey"),
                colorKey = obj.getString("colorKey"),
                isActive = obj.getBoolean("isActive"),
                budgetLimitCents = if (obj.isNull("budgetLimitCents")) null else obj.getLong("budgetLimitCents"),
                linkedPackageName = if (obj.isNull("linkedPackageName")) null else obj.getString("linkedPackageName"),
                bankBin = if (obj.isNull("bankBin")) null else obj.getString("bankBin"),
                accountNumber = if (obj.isNull("accountNumber")) null else obj.getString("accountNumber"),
                accountName = if (obj.isNull("accountName")) null else obj.getString("accountName"),
                createdAt = obj.getLong("createdAt"),
            ))
        }

        // 3. Import transactions (FK -> wallets)
        val txArr = data.getJSONArray("transactions")
        for (i in 0 until txArr.length()) {
            val obj = txArr.getJSONObject(i)
            transactionDao.upsert(TransactionEntity(
                id = obj.getLong("id"),
                amountCents = obj.getLong("amountCents"),
                type = obj.getString("type"),
                category = obj.getString("category"),
                note = obj.getString("note"),
                occurredAt = obj.getLong("occurredAt"),
                walletId = obj.getLong("walletId"),
                createdAt = obj.getLong("createdAt"),
                isAutoCapture = obj.optBoolean("isAutoCapture", false),
                isInternalTransfer = obj.optBoolean("isInternalTransfer", false),
            ))
        }

        // 4. Import bill splits (FK -> transactions)
        if (data.has("billSplits")) {
            val bsArr = data.getJSONArray("billSplits")
            val bsList = mutableListOf<BillSplitEntity>()
            for (i in 0 until bsArr.length()) {
                val obj = bsArr.getJSONObject(i)
                bsList.add(BillSplitEntity(
                    id = obj.getLong("id"),
                    transactionId = obj.getLong("transactionId"),
                    debtorName = obj.getString("debtorName"),
                    amountCents = obj.getLong("amountCents"),
                    isPaid = obj.getBoolean("isPaid"),
                    memoCode = obj.getString("memoCode"),
                    paidAt = if (obj.isNull("paidAt")) null else obj.getLong("paidAt"),
                    createdAt = obj.getLong("createdAt"),
                ))
            }
            billSplitDao.upsertAll(bsList)
        }

        // 5. Import subscriptions
        if (data.has("subscriptions")) {
            val subArr = data.getJSONArray("subscriptions")
            for (i in 0 until subArr.length()) {
                val obj = subArr.getJSONObject(i)
                subscriptionDao.upsert(SubscriptionEntity(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    amountCents = obj.getLong("amountCents"),
                    category = obj.getString("category"),
                    nextDueDate = obj.getLong("nextDueDate"),
                    repeatMonths = obj.getInt("repeatMonths"),
                    remindDaysBefore = obj.getInt("remindDaysBefore"),
                    note = obj.optString("note", ""),
                    isActive = obj.optBoolean("isActive", true),
                    createdAt = obj.getLong("createdAt"),
                ))
            }
        }

        // 6. Import custom categories
        if (data.has("customCategories")) {
            val catArr = data.getJSONArray("customCategories")
            for (i in 0 until catArr.length()) {
                val obj = catArr.getJSONObject(i)
                val category = Category(
                    id = obj.getString("id"),
                    displayName = obj.getString("displayName"),
                    colorArgb = obj.getLong("colorArgb"),
                    iconId = obj.getString("iconId"),
                    type = if (obj.getBoolean("isIncome")) CategoryType.INCOME else CategoryType.EXPENSE,
                    isCustom = true,
                )
                categoryRepository.addCustomCategory(category)
            }
        }

        // 7. Import preferences
        if (data.has("preferences")) {
            val prefsObj = data.getJSONObject("preferences")
            importPreferences(prefsObj)
        }
    }

    private suspend fun importPreferences(prefsObj: JSONObject) {
        // Theme
        val settingsPrefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        settingsPrefs.edit()
            .putString("theme_color", prefsObj.optString("themeColor", "green"))
            .putString("theme_custom_color", prefsObj.optString("themeCustomColor", "#1B7F4F"))
            .commit()

        // Notification settings via DataStore
        val autoCapture = prefsObj.optBoolean("autoCaptureEnabled", true)
        notificationSettingsStore.setAutoCaptureEnabled(autoCapture)

        if (prefsObj.has("enabledPackages")) {
            val arr = prefsObj.getJSONArray("enabledPackages")
            val packages = (0 until arr.length()).map { arr.getString(it) }.toSet()
            // Ghi trực tiếp vào DataStore
            val dataStore = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
            dataStore.edit().putStringSet("enabled_notification_packages", packages).commit()
        }

        if (prefsObj.has("customBankApps")) {
            val arr = prefsObj.getJSONArray("customBankApps")
            val custom = (0 until arr.length()).map { arr.getString(it) }.toSet()
            val dataStore = context.getSharedPreferences("notification_settings", Context.MODE_PRIVATE)
            dataStore.edit().putStringSet("custom_bank_apps", custom).commit()
        }

        // Category habits
        if (prefsObj.has("categoryHabits")) {
            val habitsObj = prefsObj.getJSONObject("categoryHabits")
            val habitsPrefs = context.getSharedPreferences("notepay_category_habits", Context.MODE_PRIVATE)
            val editor = habitsPrefs.edit()
            for (key in habitsObj.keys()) {
                editor.putInt(key, habitsObj.getInt(key))
            }
            editor.commit()
        }
    }
}
