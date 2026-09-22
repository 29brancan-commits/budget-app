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
    val reminderDaysBefore: Int = -1, // -1 = off, 0 = on due date, 1 = 1 day before, 3 = 3 days before, 7 = 1 week before
    val status: String = "due" // Options: "due", "pending", "paid", "cleared"
) {
    fun getEffectiveStatus(): String {
        val s = status.lowercase().trim()
        if (s in listOf("due", "pending", "paid", "cleared")) {
            return s
        }
        return if (isCompleted) "cleared" else "due"
    }

    val isPaidOrCleared: Boolean
        get() = getEffectiveStatus() in listOf("paid", "cleared")

    val isDue: Boolean
        get() = getEffectiveStatus() == "due"

    val isPending: Boolean
        get() = getEffectiveStatus() == "pending"

    val isPaid: Boolean
        get() = getEffectiveStatus() == "paid"

    val isCleared: Boolean
        get() = getEffectiveStatus() == "cleared"
}

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
    val notes: String = "",
    val promoApr: Double? = null,
    val promoExpirationDate: String? = null, // "YYYY-MM-DD"
    val customPriorityRank: Int = 0, // 0 = automatic (Snowball/Avalanche), 1, 2, 3... = pinned priority
    val originalBalance: Double = balance,
    val isLinkedToBills: Boolean = false,
    val billDueDay: Int = 15,
    val isPaidOff: Boolean = false,
    val linkedAccountId: Int? = null,
    val linkedAccountName: String? = null
) {
    val totalPaidSoFar: Double
        get() = (originalBalance - balance).coerceAtLeast(0.0)

    val progressPercentage: Int
        get() = if (originalBalance > 0.0) {
            ((totalPaidSoFar / originalBalance) * 100.0).toInt().coerceIn(0, 100)
        } else if (balance <= 0.0) 100 else 0
}

@Entity(
    tableName = "debt_payments",
    indices = [
        Index(value = ["debtId"]),
        Index(value = ["paymentDateMillis"])
    ]
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val debtId: Int,
    val paymentAmount: Double,
    val principalAmount: Double,
    val interestAmount: Double,
    val paymentDateMillis: Long,
    val note: String = ""
)


