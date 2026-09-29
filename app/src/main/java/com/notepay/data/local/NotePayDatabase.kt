package com.notepay.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.notepay.data.local.dao.TransactionDao
import com.notepay.data.local.dao.WalletDao
import com.notepay.data.local.dao.BillSplitDao
import com.notepay.data.local.dao.SubscriptionDao
import com.notepay.data.local.dao.DebtDao
import com.notepay.data.local.entity.TransactionEntity
import com.notepay.data.local.entity.WalletEntity
import com.notepay.data.local.entity.BillSplitEntity
import com.notepay.data.local.entity.SubscriptionEntity
import com.notepay.data.local.entity.DebtEntity
import com.notepay.data.local.entity.DebtPaymentEntity

@Database(
    entities = [
        TransactionEntity::class,
        WalletEntity::class,
        BillSplitEntity::class,
        SubscriptionEntity::class,
        DebtEntity::class,
        DebtPaymentEntity::class,
    ],
    version = 6, // Nâng cấp lên phiên bản 6 hỗ trợ quản lý công nợ chuyên biệt
    exportSchema = true,
)
abstract class NotePayDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun walletDao(): WalletDao
    abstract fun billSplitDao(): BillSplitDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun debtDao(): DebtDao

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

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `person_name` TEXT NOT NULL,
                        `phone_number` TEXT,
                        `type` TEXT NOT NULL,
                        `original_amount_cents` INTEGER NOT NULL,
                        `wallet_id` INTEGER,
                        `created_at` INTEGER NOT NULL,
                        `due_date` INTEGER,
                        `note` TEXT NOT NULL DEFAULT '',
                        `is_settled` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`wallet_id`) REFERENCES `wallets`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_person_name` ON `debts` (`person_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_wallet_id` ON `debts` (`wallet_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_is_settled` ON `debts` (`is_settled`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debts_due_date` ON `debts` (`due_date`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `debt_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `debt_id` INTEGER NOT NULL,
                        `amount_cents` INTEGER NOT NULL,
                        `wallet_id` INTEGER,
                        `paid_at` INTEGER NOT NULL,
                        `note` TEXT NOT NULL DEFAULT '',
                        `transaction_id` INTEGER,
                        FOREIGN KEY(`debt_id`) REFERENCES `debts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`transaction_id`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_debt_id` ON `debt_payments` (`debt_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_transaction_id` ON `debt_payments` (`transaction_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_debt_payments_paid_at` ON `debt_payments` (`paid_at`)")
            }
        }
    }
}