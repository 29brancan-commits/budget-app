package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["dateMillis"]),
        Index(value = ["category"]),
        Index(value = ["accountName"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dateMillis: Long,
    val category: String,
    val isIncome: Boolean,
    val isRecurring: Boolean = false,
    val accountName: String = "Primary Account",
    val isCompleted: Boolean = false,
    val isAutoPay: Boolean = false,
    val reminderDaysBefore: Int = -1 // -1 = off, 0 = on due date, 1 = 1 day before, 3 = 3 days before, 7 = 1 week before
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "income_sources")
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val frequency: String // "Weekly", "Monthly"
)

@Entity(
    tableName = "planned_expenses",
    indices = [
        Index(value = ["dueDate"])
    ]
)
data class PlannedExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val dueDate: String, // format YYYY-MM-DD
    val minAmount: Double,
    val targetAccount: String = "Primary Account",
    val isAutoPay: Boolean = false,
    val reminderDaysBefore: Int = 0 // -1 = off, 0 = on due date (day of), 1 = 1 day before, 3 = 3 days before, 7 = 1 week before
)

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val interestRate: Double,
    val minimumPayment: Double,
    val category: String = "Credit Card",
    val notes: String = ""
)


