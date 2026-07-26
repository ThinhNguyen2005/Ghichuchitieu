package com.notepay.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.notepay.data.local.NotePayDatabase
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.data.repository.CategoryRepositoryImpl
import com.notepay.domain.model.Category
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataImporter @Inject constructor(
    private val database: NotePayDatabase,
    private val transactionDao: TransactionDao,
    private val walletDao: WalletDao,
    private val billSplitDao: BillSplitDao,
    private val subscriptionDao: SubscriptionDao,
    private val categoryRepository: CategoryRepositoryImpl,
    private val budgetSettingsStore: BudgetSettingsStore,
    @ApplicationContext private val context: Context,
) {
    suspend fun readFromFile(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            var totalBytes = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                totalBytes += count
                require(totalBytes <= MAX_BACKUP_BYTES) { "File sao lưu vượt quá 20 MB." }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        } ?: throw Exception("Không thể đọc file")
    }

    /**
     * Import dữ liệu từ JSON. Các bảng Room được xóa và nhập trong cùng một transaction.
     * Phải chạy trên IO dispatcher.
     */
    suspend fun importFromJson(jsonString: String) {
        val root = JSONObject(jsonString)
        val version = root.optInt("version", 0)
        require(version <= BACKUP_VERSION) { "Phiên bản backup không tương thích: v$version" }

        val data = root.getJSONObject("data")
        validateBackupData(data)
        val customCategories = parseCustomCategories(data)

        database.withTransaction {
            billSplitDao.deleteAll()
            transactionDao.deleteAll()
            subscriptionDao.deleteAll()
            walletDao.deleteAll()

            val walletsArr = data.getJSONArray("wallets")
            for (i in 0 until walletsArr.length()) {
                val obj = walletsArr.getJSONObject(i)
                walletDao.upsert(
                    WalletEntity(
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
                    ),
                )
            }

            val transactionsArr = data.getJSONArray("transactions")
            for (i in 0 until transactionsArr.length()) {
                val obj = transactionsArr.getJSONObject(i)
                transactionDao.upsert(
                    TransactionEntity(
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
                    ),
                )
            }

            if (data.has("billSplits")) {
                val billSplitsArr = data.getJSONArray("billSplits")
                val billSplits = buildList {
                    for (i in 0 until billSplitsArr.length()) {
                        val obj = billSplitsArr.getJSONObject(i)
                        add(
                            BillSplitEntity(
                                id = obj.getLong("id"),
                                transactionId = obj.getLong("transactionId"),
                                debtorName = obj.getString("debtorName"),
                                amountCents = obj.getLong("amountCents"),
                                isPaid = obj.getBoolean("isPaid"),
                                memoCode = obj.getString("memoCode"),
                                paidAt = if (obj.isNull("paidAt")) null else obj.getLong("paidAt"),
                                createdAt = obj.getLong("createdAt"),
                            ),
                        )
                    }
                }
                billSplitDao.upsertAll(billSplits)
            }

            if (data.has("subscriptions")) {
                val subscriptionsArr = data.getJSONArray("subscriptions")
                for (i in 0 until subscriptionsArr.length()) {
                    val obj = subscriptionsArr.getJSONObject(i)
                    subscriptionDao.upsert(
                        SubscriptionEntity(
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
                        ),
                    )
                }
            }
        }

        categoryRepository.replaceCustomCategories(customCategories)

        if (data.has("preferences")) {
            importPreferences(data.getJSONObject("preferences"))
        }
    }

    private fun validateBackupData(data: JSONObject) {
        val wallets = data.getJSONArray("wallets")
        val transactions = data.getJSONArray("transactions")
        for (index in 0 until wallets.length()) {
            val wallet = wallets.getJSONObject(index)
            require(wallet.getLong("id") >= 0L) { "Ví trong bản sao lưu không hợp lệ." }
            require(wallet.getString("name").isNotBlank()) { "Tên ví không được để trống." }
            wallet.getLong("initialBalanceCents")
            wallet.getString("iconKey")
            wallet.getString("colorKey")
            wallet.getBoolean("isActive")
            wallet.getLong("createdAt")
        }
        for (index in 0 until transactions.length()) {
            val transaction = transactions.getJSONObject(index)
            require(transaction.getLong("id") >= 0L) { "Giao dịch trong bản sao lưu không hợp lệ." }
            require(transaction.getLong("amountCents") >= 0L) { "Số tiền giao dịch không hợp lệ." }
            require(transaction.getString("type").isNotBlank()) { "Loại giao dịch không hợp lệ." }
            require(transaction.getString("category").isNotBlank()) { "Danh mục giao dịch không hợp lệ." }
            transaction.getString("note")
            transaction.getLong("occurredAt")
            transaction.getLong("walletId")
            transaction.getLong("createdAt")
        }
        if (data.has("billSplits")) data.getJSONArray("billSplits")
        if (data.has("subscriptions")) data.getJSONArray("subscriptions")
        if (data.has("customCategories")) data.getJSONArray("customCategories")
        if (data.has("preferences")) data.getJSONObject("preferences")
    }

    private fun parseCustomCategories(data: JSONObject): List<Category> {
        if (!data.has("customCategories")) return emptyList()

        val categories = buildList {
            val categoryArray = data.getJSONArray("customCategories")
            for (index in 0 until categoryArray.length()) {
                val category = categoryArray.getJSONObject(index)
                add(
                    Category(
                        id = category.getString("id"),
                        displayName = category.getString("displayName"),
                        colorArgb = category.getLong("colorArgb"),
                        isIncome = category.getBoolean("isIncome"),
                        isCustom = true,
                        iconId = category.getString("iconId"),
                    ),
                )
            }
        }
        require(categories.all { it.id.isNotBlank() && it.displayName.isNotBlank() }) {
            "Danh mục tùy chỉnh trong bản sao lưu không hợp lệ."
        }
        require(categories.distinctBy { it.id }.size == categories.size) {
            "Danh mục tùy chỉnh trong bản sao lưu bị trùng mã."
        }
        return categories
    }

    private suspend fun importPreferences(prefsObj: JSONObject) {
        val settingsPrefs = context.getSharedPreferences("notepay_settings", Context.MODE_PRIVATE)
        settingsPrefs.edit()
            .putString("theme_color", prefsObj.optString("themeColor", "green"))
            .putString("theme_custom_color", prefsObj.optString("themeCustomColor", "#1B7F4F"))
            .commit()

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

    private companion object {
        const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
    }
}
