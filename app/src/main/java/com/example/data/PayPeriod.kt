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
    val carryoverFromPrevious: Double = 0.0,
    val startingBalance: Double = 0.0,
    val nextCycleDeficitToSave: Double = 0.0,
    val savedFromPreviousCycle: Double = 0.0
) {
    val totalIncome: Double get() = paycheckAmount + otherIncome
    val totalAvailable: Double get() = carryoverFromPrevious + startingBalance + totalIncome
    val totalIncomeWithCarryover: Double get() = totalAvailable
    val remainingBalance: Double get() = totalAvailable - totalExpenses
    val cycleNet: Double get() = totalIncome - totalExpenses
    val isOverspentInCycle: Boolean get() = totalExpenses > totalIncome
    val isOverspentInAccount: Boolean get() = totalExpenses > totalAvailable
    val hasNextCycleDeficitReserve: Boolean get() = nextCycleDeficitToSave > 0.0
    val hasSavedFromPreviousCycle: Boolean get() = savedFromPreviousCycle > 0.0
    val spentPercentage: Float 
        get() = if (totalAvailable > 0) (totalExpenses / totalAvailable).toFloat() else if (totalIncome > 0) (totalExpenses / totalIncome).toFloat() else 0f
}
