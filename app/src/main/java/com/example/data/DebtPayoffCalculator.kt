package com.example.data

import java.time.LocalDate

enum class PayoffStrategy {
    SNOWBALL,
    AVALANCHE,
    MINIMUM_ONLY
}

data class DebtPayoffDetail(
    val debtId: Int,
    val debtName: String,
    val initialBalance: Double,
    val interestRate: Double,
    val minimumPayment: Double,
    val payoffMonth: Int,
    val totalInterestPaid: Double,
    val payoffDate: LocalDate,
    val newMonthlyPayment: Double = 0.0
)

data class MonthlyDebtProgress(
    val monthIndex: Int,
    val monthDate: LocalDate,
    val totalRemainingBalance: Double,
    val totalInterestPaidSoFar: Double,
    val minPaymentThisMonth: Double = 0.0,
    val newPaymentThisMonth: Double = 0.0,
    val balancesByDebtId: Map<Int, Double> = emptyMap()
)

data class StrategyPayoffResult(
    val strategy: PayoffStrategy,
    val strategyName: String,
    val totalMonths: Int,
    val totalInterestPaid: Double,
    val totalAmountPaid: Double,
    val payoffDate: LocalDate,
    val individualPayoffs: List<DebtPayoffDetail>,
    val monthlyProgress: List<MonthlyDebtProgress>,
    val interestSavedVsMinimum: Double = 0.0,
    val monthsSavedVsMinimum: Int = 0
)

object DebtPayoffCalculator {

    fun calculatePayoff(
        debts: List<DebtEntity>,
        extraMonthlyBudget: Double = 0.0,
        startDate: LocalDate = LocalDate.now(),
        maxMonths: Int = 360
    ): Map<PayoffStrategy, StrategyPayoffResult> {
        if (debts.isEmpty()) {
            val emptyResult = StrategyPayoffResult(
                strategy = PayoffStrategy.SNOWBALL,
                strategyName = "Snowball",
                totalMonths = 0,
                totalInterestPaid = 0.0,
                totalAmountPaid = 0.0,
                payoffDate = startDate,
                individualPayoffs = emptyList(),
                monthlyProgress = emptyList()
            )
            return mapOf(
                PayoffStrategy.SNOWBALL to emptyResult.copy(strategy = PayoffStrategy.SNOWBALL, strategyName = "Snowball"),
                PayoffStrategy.AVALANCHE to emptyResult.copy(strategy = PayoffStrategy.AVALANCHE, strategyName = "Avalanche"),
                PayoffStrategy.MINIMUM_ONLY to emptyResult.copy(strategy = PayoffStrategy.MINIMUM_ONLY, strategyName = "Minimum Only")
            )
        }

        val minimumResult = simulate(debts, PayoffStrategy.MINIMUM_ONLY, 0.0, startDate, maxMonths)
        val snowballRaw = simulate(debts, PayoffStrategy.SNOWBALL, extraMonthlyBudget, startDate, maxMonths)
        val avalancheRaw = simulate(debts, PayoffStrategy.AVALANCHE, extraMonthlyBudget, startDate, maxMonths)

        val snowballResult = snowballRaw.copy(
            interestSavedVsMinimum = (minimumResult.totalInterestPaid - snowballRaw.totalInterestPaid).coerceAtLeast(0.0),
            monthsSavedVsMinimum = (minimumResult.totalMonths - snowballRaw.totalMonths).coerceAtLeast(0)
        )

        val avalancheResult = avalancheRaw.copy(
            interestSavedVsMinimum = (minimumResult.totalInterestPaid - avalancheRaw.totalInterestPaid).coerceAtLeast(0.0),
            monthsSavedVsMinimum = (minimumResult.totalMonths - avalancheRaw.totalMonths).coerceAtLeast(0)
        )

        return mapOf(
            PayoffStrategy.SNOWBALL to snowballResult,
            PayoffStrategy.AVALANCHE to avalancheResult,
            PayoffStrategy.MINIMUM_ONLY to minimumResult
        )
    }

    fun calculateRequiredExtraPayment(
        debts: List<DebtEntity>,
        strategy: PayoffStrategy,
        targetMonths: Int,
        startDate: LocalDate = LocalDate.now()
    ): Double {
        if (debts.isEmpty() || targetMonths <= 0) return 0.0

        val minOnlyResult = simulate(debts, PayoffStrategy.MINIMUM_ONLY, 0.0, startDate, 360)
        if (minOnlyResult.totalMonths <= targetMonths) return 0.0

        var low = 0.0
        var high = debts.sumOf { it.balance }
        var best = high

        for (i in 0..25) {
            val mid = (low + high) / 2.0
            val res = simulate(debts, strategy, mid, startDate, 360)
            if (res.totalMonths <= targetMonths) {
                best = mid
                high = mid
            } else {
                low = mid
            }
        }
        return best
    }

    private class SimulationDebt(
        val id: Int,
        val name: String,
        var currentBalance: Double,
        val interestRate: Double,
        val minimumPayment: Double
    ) {
        val initialBalance: Double = currentBalance
        var totalInterestPaid: Double = 0.0
        var payoffMonth: Int = 0
        var isPaidOff: Boolean = false
    }

    private fun simulate(
        debts: List<DebtEntity>,
        strategy: PayoffStrategy,
        extraMonthlyBudget: Double,
        startDate: LocalDate,
        maxMonths: Int
    ): StrategyPayoffResult {
        val simDebts = debts.map {
            SimulationDebt(
                id = it.id,
                name = it.name,
                currentBalance = it.balance,
                interestRate = it.interestRate,
                minimumPayment = it.minimumPayment
            )
        }.toMutableList()

        var monthIndex = 0
        var totalInterestPaidAll = 0.0
        val monthlyProgressList = mutableListOf<MonthlyDebtProgress>()
        val month1Payments = mutableMapOf<Int, Double>()

        while (simDebts.any { !it.isPaidOff } && monthIndex < maxMonths) {
            monthIndex++
            val currentDate = startDate.plusMonths(monthIndex.toLong())

            // 1. Accrue monthly interest on remaining balances
            for (debt in simDebts) {
                if (!debt.isPaidOff && debt.currentBalance > 0) {
                    val monthlyInterestRate = (debt.interestRate / 100.0) / 12.0
                    val accruedInterest = debt.currentBalance * monthlyInterestRate
                    debt.currentBalance += accruedInterest
                    debt.totalInterestPaid += accruedInterest
                    totalInterestPaidAll += accruedInterest
                }
            }

            // 2. Pay minimum payments
            var availableExtra = if (strategy == PayoffStrategy.MINIMUM_ONLY) 0.0 else extraMonthlyBudget
            var minPaidThisMonth = 0.0
            var totalPaidThisMonth = 0.0

            for (debt in simDebts) {
                if (!debt.isPaidOff) {
                    val minPay = minOf(debt.currentBalance, debt.minimumPayment)
                    debt.currentBalance -= minPay
                    minPaidThisMonth += minPay
                    totalPaidThisMonth += minPay

                    if (monthIndex == 1) {
                        month1Payments[debt.id] = (month1Payments[debt.id] ?: 0.0) + minPay
                    }

                    if (debt.currentBalance <= 0.001) {
                        debt.currentBalance = 0.0
                        debt.isPaidOff = true
                        debt.payoffMonth = monthIndex
                    }
                } else if (strategy != PayoffStrategy.MINIMUM_ONLY) {
                    // Freed up minimum payment rolls over into extra budget
                    availableExtra += debt.minimumPayment
                }
            }

            // 3. Apply extra budget to target debt based on strategy
            if (availableExtra > 0 && simDebts.any { !it.isPaidOff }) {
                val targetDebt = when (strategy) {
                    PayoffStrategy.SNOWBALL -> {
                        simDebts.filter { !it.isPaidOff }.minByOrNull { it.currentBalance }
                    }
                    PayoffStrategy.AVALANCHE -> {
                        simDebts.filter { !it.isPaidOff }.minWithOrNull(
                            compareByDescending<SimulationDebt> { it.interestRate }
                                .thenBy { it.currentBalance }
                        )
                    }
                    PayoffStrategy.MINIMUM_ONLY -> null
                }

                if (targetDebt != null) {
                    val extraPayment = minOf(targetDebt.currentBalance, availableExtra)
                    targetDebt.currentBalance -= extraPayment
                    totalPaidThisMonth += extraPayment

                    if (monthIndex == 1) {
                        month1Payments[targetDebt.id] = (month1Payments[targetDebt.id] ?: 0.0) + extraPayment
                    }

                    if (targetDebt.currentBalance <= 0.001) {
                        targetDebt.currentBalance = 0.0
                        targetDebt.isPaidOff = true
                        targetDebt.payoffMonth = monthIndex
                    }
                }
            }

            val totalRemBal = simDebts.sumOf { it.currentBalance }
            val balancesMap = simDebts.associate { it.id to it.currentBalance }

            monthlyProgressList.add(
                MonthlyDebtProgress(
                    monthIndex = monthIndex,
                    monthDate = currentDate,
                    totalRemainingBalance = totalRemBal,
                    totalInterestPaidSoFar = totalInterestPaidAll,
                    minPaymentThisMonth = minPaidThisMonth,
                    newPaymentThisMonth = totalPaidThisMonth,
                    balancesByDebtId = balancesMap
                )
            )
        }

        val totalInitialBalance = debts.sumOf { it.balance }
        val totalAmountPaidAll = totalInitialBalance + totalInterestPaidAll
        val finalPayoffDate = startDate.plusMonths(monthIndex.toLong())

        val individualPayoffs = simDebts.map { debt ->
            val pMonth = if (debt.payoffMonth > 0) debt.payoffMonth else monthIndex
            DebtPayoffDetail(
                debtId = debt.id,
                debtName = debt.name,
                initialBalance = debt.initialBalance,
                interestRate = debt.interestRate,
                minimumPayment = debt.minimumPayment,
                payoffMonth = pMonth,
                totalInterestPaid = debt.totalInterestPaid,
                payoffDate = startDate.plusMonths(pMonth.toLong()),
                newMonthlyPayment = month1Payments[debt.id] ?: debt.minimumPayment
            )
        }

        val nameStr = when (strategy) {
            PayoffStrategy.SNOWBALL -> "Snowball (Smallest Balance First)"
            PayoffStrategy.AVALANCHE -> "Avalanche (Highest Interest First)"
            PayoffStrategy.MINIMUM_ONLY -> "Minimum Payments Only"
        }

        return StrategyPayoffResult(
            strategy = strategy,
            strategyName = nameStr,
            totalMonths = monthIndex,
            totalInterestPaid = totalInterestPaidAll,
            totalAmountPaid = totalAmountPaidAll,
            payoffDate = finalPayoffDate,
            individualPayoffs = individualPayoffs,
            monthlyProgress = monthlyProgressList
        )
    }
}
