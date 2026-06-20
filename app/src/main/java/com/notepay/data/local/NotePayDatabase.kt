package com.notepay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity

@Database(
    entities = [TransactionEntity::class, WalletEntity::class, BillSplitEntity::class, SubscriptionEntity::class],
    version = 5, // Nâng cấp lên phiên bản 5 quản lý dòng tiền nội bộ
    exportSchema = true,
)
abstract class NotePayDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun billSplitDao(): BillSplitDao
    abstract fun subscriptionDao(): SubscriptionDao

    companion object {
        const val DB_NAME = "notepay.db"

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wallets ADD COLUMN budget_limit_cents INTEGER")
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE wallets ADD COLUMN linked_package_name TEXT")
                db.execSQL("ALTER TABLE wallets ADD COLUMN bank_bin TEXT")
                db.execSQL("ALTER TABLE wallets ADD COLUMN account_number TEXT")
                db.execSQL("ALTER TABLE wallets ADD COLUMN account_name TEXT")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `bill_splits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transaction_id` INTEGER NOT NULL,
                        `debtor_name` TEXT NOT NULL,
                        `amount_cents` INTEGER NOT NULL,
                        `is_paid` INTEGER NOT NULL,
                        `memo_code` TEXT NOT NULL,
                        `paid_at` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bill_splits_transaction_id` ON `bill_splits` (`transaction_id`)")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Thêm cột is_auto_capture vào bảng transactions
                db.execSQL("ALTER TABLE transactions ADD COLUMN is_auto_capture INTEGER NOT NULL DEFAULT 0")

                // Tạo bảng subscriptions mới
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `subscriptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `amount_cents` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `next_due_date` INTEGER NOT NULL,
                        `repeat_months` INTEGER NOT NULL,
                        `remind_days_before` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Thêm cột is_internal_transfer phục vụ cơ chế lọc chặn trùng lặp dòng tiền nội bộ (Case 7)
                db.execSQL("ALTER TABLE transactions ADD COLUMN is_internal_transfer INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}