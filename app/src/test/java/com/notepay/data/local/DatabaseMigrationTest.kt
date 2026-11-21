package com.notepay.data.local

import android.content.Context
import androidx.room.Room
import com.google.common.truth.Truth.assertThat
import com.notepay.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class DatabaseMigrationTest {

    @Test
    fun testMigration1To2() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbFile = context.getDatabasePath("test_migration.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // Bước 1: Tạo database v1 bằng SQLite thô với đầy đủ các bảng và index của v1
        val dbV1 = context.openOrCreateDatabase("test_migration.db", Context.MODE_PRIVATE, null)

        // Tạo bảng wallets v1
        dbV1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `initial_balance_cents` INTEGER NOT NULL, 
                `icon_key` TEXT NOT NULL, 
                `color_key` TEXT NOT NULL, 
                `is_active` INTEGER NOT NULL,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        dbV1.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")

        // Tạo bảng transactions v1
        dbV1.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `amount_cents` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `category` TEXT NOT NULL, 
                `note` TEXT NOT NULL, 
                `occurred_at` INTEGER NOT NULL, 
                `wallet_id` INTEGER NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        dbV1.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV1.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")

        // Tạo room_master_table và chèn identity_hash của v1
        dbV1.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV1.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '06e32b03c08499a5a0a9b01051501c3d')")

        // Chèn ví ở v1
        dbV1.execSQL(
            """
            INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, created_at)
            VALUES (42, 'Ví Tiết Kiệm', 100000, 'bank', 'secondary', 0, 1000)
            """.trimIndent()
        )
        // Gán version SQLite là 1 để Room nhận diện đúng và chạy Migration
        dbV1.execSQL("PRAGMA user_version = 1")
        dbV1.close()

        // Bước 2: Dùng Room mở database với version cao nhất và nạp các bước Migration đầy đủ
        // ĐÃ SỬA: Thêm NotePayDatabase.MIGRATION_4_5 để Room có thể biên dịch nâng cấp lên v5 thành công
        val dbV5 = Room.databaseBuilder(context, NotePayDatabase::class.java, "test_migration.db")
            .addMigrations(
                NotePayDatabase.MIGRATION_1_2,
                NotePayDatabase.MIGRATION_2_3,
                NotePayDatabase.MIGRATION_3_4,
                NotePayDatabase.MIGRATION_4_5,
                NotePayDatabase.MIGRATION_5_6,
                NotePayDatabase.MIGRATION_6_7,
                NotePayDatabase.MIGRATION_7_8,
            )
            .allowMainThreadQueries()
            .build()

        val wallets = dbV5.walletDao().observeAll().first()

        // Xác minh dữ liệu ví cũ vẫn nguyên vẹn
        assertThat(wallets).hasSize(1)
        val wallet = wallets[0]
        assertThat(wallet.id).isEqualTo(42L)
        assertThat(wallet.name).isEqualTo("Ví Tiết Kiệm")

        // Xác minh cột mới budgetLimitCents được chèn thành công với giá trị mặc định là NULL
        assertThat(wallet.budgetLimitCents).isNull()
        assertThat(wallet.linkedPackageName).isNull()

        dbV5.close()
        context.deleteDatabase("test_migration.db")
    }

    @Test
    fun testMigration2To3() = runTest {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val dbFile = context.getDatabasePath("test_migration_2_3.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        // Tạo database v2 bằng SQLite thô
        val dbV2 = context.openOrCreateDatabase("test_migration_2_3.db", Context.MODE_PRIVATE, null)

        // Tạo bảng wallets v2 (đã có budget_limit_cents)
        dbV2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `wallets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `name` TEXT NOT NULL, 
                `initial_balance_cents` INTEGER NOT NULL, 
                `icon_key` TEXT NOT NULL, 
                `color_key` TEXT NOT NULL, 
                `is_active` INTEGER NOT NULL,
                `budget_limit_cents` INTEGER,
                `created_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        dbV2.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")

        // Tạo bảng transactions v2
        dbV2.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `transactions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                `amount_cents` INTEGER NOT NULL, 
                `type` TEXT NOT NULL, 
                `category` TEXT NOT NULL, 
                `note` TEXT NOT NULL, 
                `occurred_at` INTEGER NOT NULL, 
                `wallet_id` INTEGER NOT NULL, 
                `created_at` INTEGER NOT NULL, 
                FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        dbV2.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV2.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")

        // Tạo room_master_table và chèn identity_hash của v2
        dbV2.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV2.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'db_v2_identity_hash')")

        // Chèn ví ở v2
        dbV2.execSQL(
            """
            INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, budget_limit_cents, created_at)
            VALUES (42, 'Ví Tiền Mặt', 150000, 'cash', 'primary', 1, 5000000, 1000)
            """.trimIndent()
        )
        // Chèn transaction ở v2
        dbV2.execSQL(
            """
            INSERT INTO transactions (id, amount_cents, type, category, note, occurred_at, wallet_id, created_at)
            VALUES (10, -30000, 'EXPENSE', 'FOOD', 'An trua', 2000, 42, 2000)
            """.trimIndent()
        )

        dbV2.execSQL("PRAGMA user_version = 2")
        dbV2.close()

        // Mở database bằng Room với đầy đủ các chuỗi Migration lên version 5 mới nhất
        // ĐÃ SỬA: Bổ sung NotePayDatabase.MIGRATION_4_5 để kết thúc chuỗi logic đối soát đĩa
        val dbV5 = Room.databaseBuilder(context, NotePayDatabase::class.java, "test_migration_2_3.db")
            .addMigrations(
                NotePayDatabase.MIGRATION_2_3,
                NotePayDatabase.MIGRATION_3_4,
                NotePayDatabase.MIGRATION_4_5,
                NotePayDatabase.MIGRATION_5_6,
                NotePayDatabase.MIGRATION_6_7,
                NotePayDatabase.MIGRATION_7_8,
            )
            .allowMainThreadQueries()
            .build()

        // Kiểm tra ví
        val wallets = dbV5.walletDao().observeAll().first()
        assertThat(wallets).hasSize(1)
        val wallet = wallets[0]
        assertThat(wallet.id).isEqualTo(42L)
        assertThat(wallet.name).isEqualTo("Ví Tiền Mặt")
        assertThat(wallet.linkedPackageName).isNull()
        assertThat(wallet.bankBin).isNull()
        assertThat(wallet.accountNumber).isNull()
        assertThat(wallet.accountName).isNull()

        // Thêm bản ghi vào bảng bill_splits mới để kiểm tra cấu trúc bảng hoạt động tốt
        val billSplit = com.notepay.data.local.entity.BillSplitEntity(
            id = 1L,
            transactionId = 10L,
            debtorName = "Nguyen Van A",
            amountCents = 10000,
            isPaid = false,
            memoCode = "NP10A",
            createdAt = System.currentTimeMillis()
        )
        dbV5.billSplitDao().upsert(billSplit)

        // Đọc lại để xác minh
        val splits = dbV5.billSplitDao().observeByTransaction(10L).first()
        assertThat(splits).hasSize(1)
        assertThat(splits[0].debtorName).isEqualTo("Nguyen Van A")
        assertThat(splits[0].amountCents).isEqualTo(10000L)
        assertThat(splits[0].memoCode).isEqualTo("NP10A")

        dbV5.close()
        context.deleteDatabase("test_migration_2_3.db")
    }

    @Test
    fun testMigration5To6() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbFile = context.getDatabasePath("test_migration_5_6.db")
        if (dbFile.exists()) {
            dbFile.delete()
        }

        val dbV5 = context.openOrCreateDatabase("test_migration_5_6.db", Context.MODE_PRIVATE, null)
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initial_balance_cents` INTEGER NOT NULL, `icon_key` TEXT NOT NULL, `color_key` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `budget_limit_cents` INTEGER, `linked_package_name` TEXT, `bank_bin` TEXT, `account_number` TEXT, `account_name` TEXT, `created_at` INTEGER NOT NULL)")
        dbV5.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_cents` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `wallet_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `is_auto_capture` INTEGER NOT NULL DEFAULT 0, `is_internal_transfer` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        dbV5.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV5.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT PRIMARY KEY NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `icon_name` TEXT NOT NULL, `color_hex` TEXT NOT NULL, `is_default` INTEGER NOT NULL DEFAULT 0, `created_at` INTEGER NOT NULL)")
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS `bill_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `debtor_name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `memo_code` TEXT NOT NULL, `paid_at` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        dbV5.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `category` TEXT NOT NULL, `next_due_date` INTEGER NOT NULL, `repeat_months` INTEGER NOT NULL, `remind_days_before` INTEGER NOT NULL, `note` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)")
        dbV5.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV5.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '8813cd4b386f74d312d5d6142521f842')")
        dbV5.execSQL("PRAGMA user_version = 5")
        dbV5.close()

        val dbV6 = Room.databaseBuilder(context, NotePayDatabase::class.java, "test_migration_5_6.db")
            .addMigrations(NotePayDatabase.MIGRATION_5_6, NotePayDatabase.MIGRATION_6_7, NotePayDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        val debtEntity = com.notepay.data.local.entity.DebtEntity(
            id = 1L,
            personName = "Tran Van Test",
            phoneNumber = "0987654321",
            type = "LEND",
            originalAmountCents = 50000000L,
            walletId = null,
            createdAt = 1000L,
            dueDate = 2000L,
            note = "Test note",
            isSettled = false,
        )
        dbV6.debtDao().insertDebt(debtEntity)

        val retrieved = dbV6.debtDao().getWithPaymentsById(1L)
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.debt?.personName).isEqualTo("Tran Van Test")
        assertThat(retrieved?.debt?.originalAmountCents).isEqualTo(50000000L)

        dbV6.close()
        context.deleteDatabase("test_migration_5_6.db")
    }

    @Test
    fun testMigration3To4() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_migration_3_4.db"
        context.deleteDatabase(dbName)

        val dbV3 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        dbV3.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initial_balance_cents` INTEGER NOT NULL, `icon_key` TEXT NOT NULL, `color_key` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `budget_limit_cents` INTEGER, `linked_package_name` TEXT, `bank_bin` TEXT, `account_number` TEXT, `account_name` TEXT, `created_at` INTEGER NOT NULL)")
        dbV3.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")
        dbV3.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_cents` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `wallet_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV3.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV3.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
        dbV3.execSQL("CREATE TABLE IF NOT EXISTS `bill_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `debtor_name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `memo_code` TEXT NOT NULL, `paid_at` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV3.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
        dbV3.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV3.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '83e5cb353d4334c0ecc9c0bf57e9b918')")
        dbV3.execSQL("INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, created_at) VALUES (1, 'Tiền mặt', 100000, 'cash', 'primary', 1, 1000)")
        dbV3.execSQL("INSERT INTO transactions (id, amount_cents, type, category, note, occurred_at, wallet_id, created_at) VALUES (5, 50000, 'EXPENSE', 'FOOD', 'Bún bò', 2000, 1, 2000)")
        dbV3.execSQL("PRAGMA user_version = 3")
        dbV3.close()

        val upgraded = Room.databaseBuilder(context, NotePayDatabase::class.java, dbName)
            .addMigrations(
                NotePayDatabase.MIGRATION_3_4,
                NotePayDatabase.MIGRATION_4_5,
                NotePayDatabase.MIGRATION_5_6,
                NotePayDatabase.MIGRATION_6_7,
                NotePayDatabase.MIGRATION_7_8,
            )
            .allowMainThreadQueries()
            .build()

        val tx = upgraded.transactionDao().getById(5L)
        assertThat(tx).isNotNull()
        assertThat(tx!!.note).isEqualTo("Bún bò")
        assertThat(tx.isAutoCapture).isFalse()

        upgraded.subscriptionDao().upsert(
            com.notepay.data.local.entity.SubscriptionEntity(
                id = 1L,
                name = "iCloud",
                amountCents = 19_000_00L,
                category = "UTILITIES",
                nextDueDate = 9_000L,
                repeatMonths = 1,
                remindDaysBefore = 3,
                createdAt = 3_000L,
            ),
        )
        val subs = upgraded.subscriptionDao().getAll()
        assertThat(subs).hasSize(1)
        assertThat(subs[0].name).isEqualTo("iCloud")

        upgraded.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration4To5() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_migration_4_5.db"
        context.deleteDatabase(dbName)

        val dbV4 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        dbV4.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initial_balance_cents` INTEGER NOT NULL, `icon_key` TEXT NOT NULL, `color_key` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `budget_limit_cents` INTEGER, `linked_package_name` TEXT, `bank_bin` TEXT, `account_number` TEXT, `account_name` TEXT, `created_at` INTEGER NOT NULL)")
        dbV4.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")
        dbV4.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_cents` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `wallet_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `is_auto_capture` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV4.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV4.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
        dbV4.execSQL("CREATE TABLE IF NOT EXISTS `bill_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `debtor_name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `memo_code` TEXT NOT NULL, `paid_at` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV4.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
        dbV4.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `category` TEXT NOT NULL, `next_due_date` INTEGER NOT NULL, `repeat_months` INTEGER NOT NULL, `remind_days_before` INTEGER NOT NULL, `note` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)")
        dbV4.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV4.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'd68177832bf4faeaa93c184dc432563d')")
        dbV4.execSQL("INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, created_at) VALUES (2, 'TPBank', 200000, 'bank', 'secondary', 1, 1000)")
        dbV4.execSQL("INSERT INTO transactions (id, amount_cents, type, category, note, occurred_at, wallet_id, created_at, is_auto_capture) VALUES (8, 75000, 'INCOME', 'SALARY', 'Hoàn tiền', 2500, 2, 2500, 1)")
        dbV4.execSQL("PRAGMA user_version = 4")
        dbV4.close()

        val upgraded = Room.databaseBuilder(context, NotePayDatabase::class.java, dbName)
            .addMigrations(
                NotePayDatabase.MIGRATION_4_5,
                NotePayDatabase.MIGRATION_5_6,
                NotePayDatabase.MIGRATION_6_7,
                NotePayDatabase.MIGRATION_7_8,
            )
            .allowMainThreadQueries()
            .build()

        val tx = upgraded.transactionDao().getById(8L)
        assertThat(tx).isNotNull()
        assertThat(tx!!.isAutoCapture).isTrue()
        assertThat(tx.isInternalTransfer).isFalse()

        upgraded.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration6To7() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_migration_6_7.db"
        context.deleteDatabase(dbName)

        val dbV6 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initial_balance_cents` INTEGER NOT NULL, `icon_key` TEXT NOT NULL, `color_key` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `budget_limit_cents` INTEGER, `linked_package_name` TEXT, `bank_bin` TEXT, `account_number` TEXT, `account_name` TEXT, `created_at` INTEGER NOT NULL)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_cents` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `wallet_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `is_auto_capture` INTEGER NOT NULL DEFAULT 0, `is_internal_transfer` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `bill_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `debtor_name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `memo_code` TEXT NOT NULL, `paid_at` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `category` TEXT NOT NULL, `next_due_date` INTEGER NOT NULL, `repeat_months` INTEGER NOT NULL, `remind_days_before` INTEGER NOT NULL, `note` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `person_name` TEXT NOT NULL, `phone_number` TEXT, `type` TEXT NOT NULL, `original_amount_cents` INTEGER NOT NULL, `wallet_id` INTEGER, `created_at` INTEGER NOT NULL, `due_date` INTEGER, `note` TEXT NOT NULL, `is_settled` INTEGER NOT NULL, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_person_name` ON `debts` (`person_name`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_wallet_id` ON `debts` (`wallet_id`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_is_settled` ON `debts` (`is_settled`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_due_date` ON `debts` (`due_date`)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS `debt_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debt_id` INTEGER NOT NULL, `amount_cents` INTEGER NOT NULL, `wallet_id` INTEGER, `paid_at` INTEGER NOT NULL, `note` TEXT NOT NULL, `transaction_id` INTEGER, FOREIGN KEY(`debt_id`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debt_id` ON `debt_payments` (`debt_id`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_transaction_id` ON `debt_payments` (`transaction_id`)")
        dbV6.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_paid_at` ON `debt_payments` (`paid_at`)")
        dbV6.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV6.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, '01c8e5ed30727e6f9fa898f58e87d55e')")
        dbV6.execSQL("INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, created_at) VALUES (3, 'Vietcombank', 500000, 'bank', 'primary', 1, 1000)")
        dbV6.execSQL("INSERT INTO transactions (id, amount_cents, type, category, note, occurred_at, wallet_id, created_at, is_auto_capture, is_internal_transfer) VALUES (12, 100000, 'EXPENSE', 'OTHER', 'Chuyển quỹ', 3000, 3, 3000, 0, 1)")
        dbV6.execSQL("PRAGMA user_version = 6")
        dbV6.close()

        val upgraded = Room.databaseBuilder(context, NotePayDatabase::class.java, dbName)
            .addMigrations(NotePayDatabase.MIGRATION_6_7, NotePayDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        val legacyTx = upgraded.transactionDao().getById(12L)
        assertThat(legacyTx).isNotNull()
        assertThat(legacyTx!!.isInternalTransfer).isTrue()
        assertThat(legacyTx.transferGroupId).isNull()

        upgraded.transactionDao().upsert(legacyTx.copy(transferGroupId = "grp-6-7"))
        val grouped = upgraded.transactionDao().getByTransferGroupId("grp-6-7")
        assertThat(grouped).hasSize(1)
        assertThat(grouped[0].id).isEqualTo(12L)

        upgraded.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun testMigration7To8() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dbName = "test_migration_7_8.db"
        context.deleteDatabase(dbName)

        val dbV7 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `wallets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `initial_balance_cents` INTEGER NOT NULL, `icon_key` TEXT NOT NULL, `color_key` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `budget_limit_cents` INTEGER, `linked_package_name` TEXT, `bank_bin` TEXT, `account_number` TEXT, `account_name` TEXT, `created_at` INTEGER NOT NULL)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_wallets_is_active` ON `wallets` (`is_active`)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `amount_cents` INTEGER NOT NULL, `type` TEXT NOT NULL, `category` TEXT NOT NULL, `note` TEXT NOT NULL, `occurred_at` INTEGER NOT NULL, `wallet_id` INTEGER NOT NULL, `created_at` INTEGER NOT NULL, `is_auto_capture` INTEGER NOT NULL DEFAULT 0, `is_internal_transfer` INTEGER NOT NULL DEFAULT 0, `transfer_group_id` TEXT, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_wallet_id` ON `transactions` (`wallet_id`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_occurred_at` ON `transactions` (`occurred_at`)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `bill_splits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transaction_id` INTEGER NOT NULL, `debtor_name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `is_paid` INTEGER NOT NULL, `memo_code` TEXT NOT NULL, `paid_at` INTEGER, `created_at` INTEGER NOT NULL, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `subscriptions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `amount_cents` INTEGER NOT NULL, `category` TEXT NOT NULL, `next_due_date` INTEGER NOT NULL, `repeat_months` INTEGER NOT NULL, `remind_days_before` INTEGER NOT NULL, `note` TEXT NOT NULL, `is_active` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `debts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `person_name` TEXT NOT NULL, `phone_number` TEXT, `type` TEXT NOT NULL, `original_amount_cents` INTEGER NOT NULL, `wallet_id` INTEGER, `created_at` INTEGER NOT NULL, `due_date` INTEGER, `note` TEXT NOT NULL, `is_settled` INTEGER NOT NULL, FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_person_name` ON `debts` (`person_name`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_wallet_id` ON `debts` (`wallet_id`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_is_settled` ON `debts` (`is_settled`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_due_date` ON `debts` (`due_date`)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS `debt_payments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `debt_id` INTEGER NOT NULL, `amount_cents` INTEGER NOT NULL, `wallet_id` INTEGER, `paid_at` INTEGER NOT NULL, `note` TEXT NOT NULL, `transaction_id` INTEGER, FOREIGN KEY(`debt_id`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debt_id` ON `debt_payments` (`debt_id`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_transaction_id` ON `debt_payments` (`transaction_id`)")
        dbV7.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_paid_at` ON `debt_payments` (`paid_at`)")
        dbV7.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)")
        dbV7.execSQL("INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, 'b86b4efb2bcf1b8df7cb35c91b5c3298')")
        dbV7.execSQL("INSERT INTO wallets (id, name, initial_balance_cents, icon_key, color_key, is_active, created_at) VALUES (7, 'Techcombank', 1200000, 'bank', 'primary', 1, 1000)")
        dbV7.execSQL("PRAGMA user_version = 7")
        dbV7.close()

        val upgraded = Room.databaseBuilder(context, NotePayDatabase::class.java, dbName)
            .addMigrations(NotePayDatabase.MIGRATION_7_8)
            .allowMainThreadQueries()
            .build()

        val migratedWallet = upgraded.walletDao().getById(7L)
        assertThat(migratedWallet).isNotNull()
        assertThat(migratedWallet!!.name).isEqualTo("Techcombank")
        assertThat(migratedWallet.walletType).isEqualTo("CASH")
        assertThat(migratedWallet.accountNature).isEqualTo("ASSET")
        assertThat(migratedWallet.currencyCode).isEqualTo("VND")
        assertThat(migratedWallet.creditLimitCents).isNull()
        assertThat(migratedWallet.isArchived).isFalse()

        // Thêm một tài khoản thẻ tín dụng nợ mới
        val creditCard = WalletEntity(
            id = 8L,
            name = "Visa Platinum",
            initialBalanceCents = 0L,
            iconKey = "card",
            colorKey = "coral",
            isActive = false,
            createdAt = 2000L,
            walletType = "CREDIT_CARD",
            accountNature = "LIABILITY",
            currencyCode = "VND",
            creditLimitCents = 50_000_000_00L,
            isArchived = false,
        )
        upgraded.walletDao().upsert(creditCard)

        // Lưu trữ ví số 7 và kiểm tra observeNonArchived
        upgraded.walletDao().setArchived(7L, true)
        val nonArchived = upgraded.walletDao().observeNonArchived().first()
        assertThat(nonArchived.map { it.id }).containsExactly(8L)

        upgraded.close()
        context.deleteDatabase(dbName)
    }
}
