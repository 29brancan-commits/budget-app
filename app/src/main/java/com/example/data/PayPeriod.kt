package com.example.data

import java.time.LocalDate

data class PayPeriod(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val payday: LocalDate,
    val transactions: List<TransactionEntity>,
    val paycheckAmount: Double, // User customized or default
    val otherIncome: Double,
    val totalExpenses: Double,
    val carryoverFromPrevious: Double = 0.0
) {
    val totalIncome: Double get() = paycheckAmount + otherIncome
    val totalAvailable: Double get() = carryoverFromPrevious + totalIncome
    val totalIncomeWithCarryover: Double get() = totalAvailable
    val remainingBalance: Double get() = totalAvailable - totalExpenses
    val spentPercentage: Float 
        get() = if (totalAvailable > 0) (totalExpenses / totalAvailable).toFloat() else 0f
}
