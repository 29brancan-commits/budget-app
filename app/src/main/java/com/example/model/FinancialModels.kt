package com.example.model

import androidx.compose.ui.graphics.Color
import java.time.LocalDate

enum class DebtCategory(val displayName: String, val colorHex: Long) {
    CREDIT_CARD("Credit Card", 0xFFE53935),
    STUDENT_LOAN("Student Loan", 0xFF1E88E5),
    AUTO_LOAN("Auto Loan", 0xFFFB8C00),
    PERSONAL_LOAN("Personal Loan", 0xFF8E24AA),
    MORTGAGE("Mortgage", 0xFF43A047),
    MEDICAL("Medical", 0xFF00ACC1),
    OTHER("Other", 0xFF757575)
}

enum class ExpenseCategory(val displayName: String, val iconName: String, val colorHex: Long) {
    HOUSING("Housing & Rent", "Home", 0xFF3F51B5),
    UTILITIES("Utilities & Bills", "Bolt", 0xFF009688),
    FOOD("Food & Groceries", "Restaurant", 0xFFFF9800),
    TRANSPORT("Transportation", "DirectionsCar", 0xFF2196F3),
    INSURANCE("Insurance", "Shield", 0xFF9C27B0),
    HEALTH("Health & Fitness", "FitnessCenter", 0xFFE91E63),
    SUBSCRIPTIONS("Subscriptions", "Subscriptions", 0xFF673AB7),
    DEBT_PAYMENT("Debt Payment", "CreditCard", 0xFFF44336),
    ENTERTAINMENT("Entertainment", "Movie", 0xFF00BCD4),
    OTHER("Miscellaneous", "MoreHoriz", 0xFF607D8B)
}

enum class PayoffStrategy(val title: String, val subtitle: String) {
    SNOWBALL("Snowball", "Lowest balance first for fast psychological wins"),
    AVALANCHE("Avalanche", "Highest APR first to mathematically minimize interest"),
    CUSTOM("Custom Priority", "Your own custom priority order")
}

data class DebtItem(
    val id: String,
    val name: String,
    val balance: Double,
    val originalBalance: Double,
    val apr: Double,
    val minPayment: Double,
    val extraPayment: Double = 0.0,
    val dueDate: String,
    val category: DebtCategory = DebtCategory.CREDIT_CARD,
    val customPriorityRank: Int = 0,
    val isPaidOff: Boolean = false,
    val isLinkedToBill: Boolean = false,
    val notes: String = ""
) {
    val progress: Float
        get() = if (originalBalance > 0) {
            ((originalBalance - balance) / originalBalance).coerceIn(0.0, 1.0).toFloat()
        } else 0f
}

data class ExpenseItem(
    val id: String,
    val name: String,
    val amount: Double,
    val category: String,
    val subcategory: String = "",
    val dueDate: String,
    val status: String = "Due", // "Due", "Paid", "Pending", "Overdue"
    val isAutoPay: Boolean = false,
    val isRecurring: Boolean = true,
    val frequency: String = "Monthly" // "Monthly", "Weekly", "Bi-Weekly"
)

data class IncomeItem(
    val id: String,
    val sourceName: String,
    val amount: Double,
    val frequency: String = "Bi-Weekly",
    val payDate: String,
    val accountName: String = "Main Checking",
    val status: String = "Expected" // "Received", "Expected"
)

object CategoryTheme {
    fun parseCategory(categoryString: String): Pair<String, String> {
        val parts = categoryString.split(":")
        return if (parts.size >= 2) {
            Pair(parts[0].trim(), parts[1].trim())
        } else {
            Pair(categoryString.trim(), "")
        }
    }

    fun getColor(categoryString: String): Color {
        val (parent, _) = parseCategory(categoryString)
        val match = ExpenseCategory.values().find { it.displayName.startsWith(parent, ignoreCase = true) || parent.startsWith(it.name, ignoreCase = true) }
        return if (match != null) Color(match.colorHex) else Color(0xFF5C6BC0)
    }
}
