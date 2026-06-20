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
                NotePayDatabase.MIGRATION_4_5
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
                NotePayDatabase.MIGRATION_4_5
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
}