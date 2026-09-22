package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TransactionEntity::class, SettingEntity::class, IncomeSourceEntity::class, PlannedExpenseEntity::class, DebtEntity::class, DebtPaymentEntity::class], version = 14, exportSchema = false)
abstract class BudgetDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: BudgetDatabase? = null

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN status TEXT NOT NULL DEFAULT 'due'")
                db.execSQL("UPDATE transactions SET status = 'cleared' WHERE isCompleted = 1")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN promoApr REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE debts ADD COLUMN promoExpirationDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE debts ADD COLUMN customPriorityRank INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN originalBalance REAL NOT NULL DEFAULT 0.0")
                db.execSQL("UPDATE debts SET originalBalance = balance WHERE originalBalance = 0.0")
                db.execSQL("ALTER TABLE debts ADD COLUMN isLinkedToBills INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debts ADD COLUMN billDueDay INTEGER NOT NULL DEFAULT 15")
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS debt_payments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        debtId INTEGER NOT NULL,
                        paymentAmount REAL NOT NULL,
                        principalAmount REAL NOT NULL,
                        interestAmount REAL NOT NULL,
                        paymentDateMillis INTEGER NOT NULL,
                        note TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_debtId ON debt_payments(debtId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_debt_payments_paymentDateMillis ON debt_payments(paymentDateMillis)")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE debts ADD COLUMN isPaidOff INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE debts ADD COLUMN linkedAccountId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE debts ADD COLUMN linkedAccountName TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): BudgetDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BudgetDatabase::class.java,
                    "budget_database"
                )
                .addMigrations(MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
