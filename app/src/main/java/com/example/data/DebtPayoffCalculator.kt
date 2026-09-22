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

data class SurplusImpactResult(
    val surplusAmount: Double,
    val daysSaved: Int,
    val monthsSaved: Int,
    val interestSaved: Double,
    val baselinePayoffDate: LocalDate,
    val acceleratedPayoffDate: LocalDate,
    val targetDebtName: String?,
    val targetDebtId: Int? = null
)

data class DebtPaymentPlanItem(
    val debt: DebtEntity,
    val minimumPayment: Double,
    val extraPayment: Double,
    val totalPayment: Double,
    val isPaidOffThisCycle: Boolean,
    val remainingBalance: Double,
    val isPrimaryTarget: Boolean
)

data class PromoPayoffAnalysis(
    val debtId: Int,
    val debtName: String,
    val monthsRemaining: Long,
    val isExpiringSoon: Boolean,
    val isExpired: Boolean,
    val requiredMonthlyPayment: Double,
    val minimumPayment: Double,
    val monthlyPaymentDifference: Double,
    val standardApr: Double,
    val promoApr: Double,
    val expirationDateStr: String
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

    fun calculateSurplusImpact(
        debts: List<DebtEntity>,
        strategy: PayoffStrategy,
        currentExtraMonthlyBudget: Double,
        surplusAmount: Double,
        startDate: LocalDate = LocalDate.now()
    ): SurplusImpactResult {
        if (debts.isEmpty() || surplusAmount <= 0.0) {
            return SurplusImpactResult(
                surplusAmount = surplusAmount,
                daysSaved = 0,
                monthsSaved = 0,
                interestSaved = 0.0,
                baselinePayoffDate = startDate,
                acceleratedPayoffDate = startDate,
                targetDebtName = null
            )
        }

        // Baseline calculation with current extra monthly budget
        val baseline = simulate(debts, strategy, currentExtraMonthlyBudget, startDate, 360)

        // Identify target debt according to pinned priority / strategy
        val unpaid = debts.filter { it.balance > 0.0 }
        val pinned = unpaid.filter { it.customPriorityRank > 0 }.minByOrNull { it.customPriorityRank }
        val target = pinned ?: when (strategy) {
            PayoffStrategy.SNOWBALL -> unpaid.minByOrNull { it.balance }
            PayoffStrategy.AVALANCHE -> unpaid.maxByOrNull { it.interestRate }
            PayoffStrategy.MINIMUM_ONLY -> unpaid.minByOrNull { it.balance }
        }

        if (target == null) {
            return SurplusImpactResult(
                surplusAmount = surplusAmount,
                daysSaved = 0,
                monthsSaved = 0,
                interestSaved = 0.0,
                baselinePayoffDate = baseline.payoffDate,
                acceleratedPayoffDate = baseline.payoffDate,
                targetDebtName = null
            )
        }

        // Sort unpaid debts in priority order (pinned first, then by strategy)
        val sortedDebts = unpaid.sortedWith { a, b ->
            val rankA = if (a.customPriorityRank > 0) a.customPriorityRank else Int.MAX_VALUE
            val rankB = if (b.customPriorityRank > 0) b.customPriorityRank else Int.MAX_VALUE
            if (rankA != rankB) {
                rankA.compareTo(rankB)
            } else {
                when (strategy) {
                    PayoffStrategy.SNOWBALL -> a.balance.compareTo(b.balance)
                    PayoffStrategy.AVALANCHE -> b.interestRate.compareTo(a.interestRate)
                    PayoffStrategy.MINIMUM_ONLY -> a.balance.compareTo(b.balance)
                }
            }
        }

        // Model immediate application of surplus cascading across debts so surplus is not lost
        var remSurplus = surplusAmount
        val balMap = debts.associate { it.id to it.balance }.toMutableMap()
        for (d in sortedDebts) {
            if (remSurplus <= 0.0) break
            val currentBal = balMap[d.id] ?: d.balance
            val payment = minOf(currentBal, remSurplus)
            balMap[d.id] = (currentBal - payment).coerceAtLeast(0.0)
            remSurplus -= payment
        }

        val acceleratedDebts = debts.map { d ->
            d.copy(balance = balMap[d.id] ?: d.balance)
        }

        val accelerated = simulate(acceleratedDebts, strategy, currentExtraMonthlyBudget, startDate, 360)

        val monthsSaved = (baseline.totalMonths - accelerated.totalMonths).coerceAtLeast(0)
        val totalMonthlyPay = debts.sumOf { it.minimumPayment } + currentExtraMonthlyBudget
        val daysSaved = if (monthsSaved > 0) {
            monthsSaved * 30
        } else {
            if (totalMonthlyPay > 0) {
                ((surplusAmount / totalMonthlyPay) * 30.0).toInt().coerceIn(1, 29)
            } else 0
        }

        val interestSaved = (baseline.totalInterestPaid - accelerated.totalInterestPaid).coerceAtLeast(0.0)

        return SurplusImpactResult(
            surplusAmount = surplusAmount,
            daysSaved = maxOf(daysSaved, if (monthsSaved > 0) monthsSaved * 30 else 0),
            monthsSaved = monthsSaved,
            interestSaved = interestSaved,
            baselinePayoffDate = baseline.payoffDate,
            acceleratedPayoffDate = accelerated.payoffDate,
            targetDebtName = target.name,
            targetDebtId = target.id
        )
    }

    fun calculateSurplusAllocation(
        debts: List<DebtEntity>,
        strategy: PayoffStrategy,
        surplusAmount: Double
    ): List<DebtPaymentPlanItem> {
        val unpaid = debts.filter { it.balance > 0.0 }
        if (unpaid.isEmpty()) return emptyList()

        val sortedDebts = unpaid.sortedWith { a, b ->
            val rankA = if (a.customPriorityRank > 0) a.customPriorityRank else Int.MAX_VALUE
            val rankB = if (b.customPriorityRank > 0) b.customPriorityRank else Int.MAX_VALUE
            if (rankA != rankB) {
                rankA.compareTo(rankB)
            } else {
                when (strategy) {
                    PayoffStrategy.SNOWBALL -> a.balance.compareTo(b.balance)
                    PayoffStrategy.AVALANCHE -> b.interestRate.compareTo(a.interestRate)
                    PayoffStrategy.MINIMUM_ONLY -> a.balance.compareTo(b.balance)
                }
            }
        }

        var remainingSurplus = surplusAmount.coerceAtLeast(0.0)
        val result = mutableListOf<DebtPaymentPlanItem>()

        for ((index, debt) in sortedDebts.withIndex()) {
            val minPay = minOf(debt.balance, debt.minimumPayment)
            // Available room to pay off after minimum payment
            val roomForExtra = (debt.balance - minPay).coerceAtLeast(0.0)
            val extraPay = minOf(remainingSurplus, roomForExtra)
            remainingSurplus -= extraPay

            val totalPay = minPay + extraPay
            val remainingBal = (debt.balance - totalPay).coerceAtLeast(0.0)
            val isPaidOff = remainingBal <= 0.001

            result.add(
                DebtPaymentPlanItem(
                    debt = debt,
                    minimumPayment = minPay,
                    extraPayment = extraPay,
                    totalPayment = totalPay,
                    isPaidOffThisCycle = isPaidOff,
                    remainingBalance = remainingBal,
                    isPrimaryTarget = (index == 0)
                )
            )
        }

        return result
    }

    fun analyzePromoApr(
        debt: DebtEntity,
        today: LocalDate = LocalDate.now()
    ): PromoPayoffAnalysis? {
        val expStr = debt.promoExpirationDate?.trim() ?: return null
        if (debt.promoApr == null || debt.balance <= 0) return null

        val expDate = try {
            LocalDate.parse(expStr)
        } catch (_: Exception) {
            return null
        }

        val monthsRemaining = java.time.temporal.ChronoUnit.MONTHS.between(today, expDate)
        val isExpired = today.isAfter(expDate)
        val isExpiringSoon = !isExpired && monthsRemaining in 0..18

        val requiredPayment = if (monthsRemaining > 0) {
            debt.balance / monthsRemaining
        } else {
            debt.balance
        }

        return PromoPayoffAnalysis(
            debtId = debt.id,
            debtName = debt.name,
            monthsRemaining = monthsRemaining,
            isExpiringSoon = isExpiringSoon,
            isExpired = isExpired,
            requiredMonthlyPayment = requiredPayment,
            minimumPayment = debt.minimumPayment,
            monthlyPaymentDifference = (requiredPayment - debt.minimumPayment).coerceAtLeast(0.0),
            standardApr = debt.interestRate,
            promoApr = debt.promoApr,
            expirationDateStr = expStr
        )
    }

    private class SimulationDebt(
        val id: Int,
        val name: String,
        var currentBalance: Double,
        val standardInterestRate: Double,
        val promoApr: Double?,
        val promoExpirationDate: LocalDate?,
        val customPriorityRank: Int,
        val minimumPayment: Double
    ) {
        val initialBalance: Double = currentBalance
        var totalInterestPaid: Double = 0.0
        var payoffMonth: Int = 0
        var isPaidOff: Boolean = false

        fun getEffectiveRate(currentDate: LocalDate): Double {
            if (promoApr != null && promoExpirationDate != null) {
                if (!currentDate.isAfter(promoExpirationDate)) {
                    return promoApr
                }
            } else if (promoApr != null && promoExpirationDate == null) {
                return promoApr
            }
            return standardInterestRate
        }
    }

    private fun simulate(
        debts: List<DebtEntity>,
        strategy: PayoffStrategy,
        extraMonthlyBudget: Double,
        startDate: LocalDate,
        maxMonths: Int
    ): StrategyPayoffResult {
        val simDebts = debts.map {
            val promoExpDate = it.promoExpirationDate?.let { expStr ->
                try { LocalDate.parse(expStr.trim()) } catch (_: Exception) { null }
            }
            SimulationDebt(
                id = it.id,
                name = it.name,
                currentBalance = it.balance,
                standardInterestRate = it.interestRate,
                promoApr = it.promoApr,
                promoExpirationDate = promoExpDate,
                customPriorityRank = it.customPriorityRank,
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

            // 1. Accrue monthly interest on remaining balances using effective APR (checking promo APR & expiration)
            for (debt in simDebts) {
                if (!debt.isPaidOff && debt.currentBalance > 0) {
                    val effectiveApr = debt.getEffectiveRate(currentDate)
                    val monthlyInterestRate = (effectiveApr / 100.0) / 12.0
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

            // 3. Apply extra budget to target debt based on priority pinning & strategy
            if (availableExtra > 0 && simDebts.any { !it.isPaidOff }) {
                val unpaidDebts = simDebts.filter { !it.isPaidOff }
                // Check if any debt has a custom pinned priority (customPriorityRank > 0)
                val pinnedDebt = unpaidDebts.filter { it.customPriorityRank > 0 }
                    .minByOrNull { it.customPriorityRank }

                val targetDebt = if (pinnedDebt != null) {
                    pinnedDebt
                } else {
                    when (strategy) {
                        PayoffStrategy.SNOWBALL -> {
                            unpaidDebts.minByOrNull { it.currentBalance }
                        }
                        PayoffStrategy.AVALANCHE -> {
                            unpaidDebts.minWithOrNull(
                                compareByDescending<SimulationDebt> { it.getEffectiveRate(currentDate) }
                                    .thenBy { it.currentBalance }
                            )
                        }
                        PayoffStrategy.MINIMUM_ONLY -> null
                    }
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
                interestRate = debt.standardInterestRate,
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
