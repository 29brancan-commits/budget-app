package com.example.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class CurrentPayCycleSummary(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val payday: LocalDate,
    val accountName: String,
    val paycheckAmount: Double,
    val otherIncome: Double,
    val totalIncome: Double,
    val carryover: Double,
    val totalAvailable: Double,
    val totalExpenses: Double,
    val completedExpenses: Double,
    val pendingExpenses: Double,
    val remainingBalance: Double,
    val clearedBalance: Double,
    val daysRemaining: Long,
    val spentPercentage: Int
)

object PayCycleCalculator {

    fun calculateCurrentPayCycle(
        allTransactions: List<TransactionEntity>,
        plannedExpenses: List<PlannedExpenseEntity> = emptyList(),
        activeAccount: String = "Primary Account",
        anchor: LocalDate = LocalDate.of(2026, 7, 30),
        defaultPaycheck: Double = 2500.0,
        today: LocalDate = LocalDate.now()
    ): CurrentPayCycleSummary {
        val currentPayday = DateUtils.getPaydayOnOrBefore(today, anchor)
        val cycleStart = currentPayday
        val cycleEnd = currentPayday.plusDays(13)

        val cycleStartMillis = DateUtils.localDateToMillis(cycleStart)
        val cycleEndMillis = DateUtils.localDateToMillis(cycleEnd.plusDays(1))

        // Expand recurring transactions up to end of cycle month
        val futureHorizonMonth = YearMonth.from(cycleEnd.plusMonths(1))
        val expandedTransactions = allTransactions.flatMap { tx ->
            if (tx.isRecurring) {
                val origDate = DateUtils.millisToLocalDate(tx.dateMillis)
                val list = mutableListOf(tx)
                if (tx.isIncome) {
                    var nextDate = origDate.plusDays(14)
                    val limitDate = futureHorizonMonth.atEndOfMonth()
                    while (!nextDate.isAfter(limitDate)) {
                        val nextMillis = DateUtils.localDateToMillis(nextDate)
                        val alreadyExists = allTransactions.any { existing ->
                            (existing.title.equals(tx.title, ignoreCase = true) ||
                                    existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                                    existing.title.contains("Cancelled " + tx.title, ignoreCase = true) ||
                                    (existing.id > 0 && existing.id == tx.id)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == nextDate
                        }
                        if (!alreadyExists) {
                            val synthId = -(Math.abs((tx.id.toString() + "_inc_" + nextDate.toString()).hashCode()) + 10000)
                            list.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = nextMillis,
                                    isCompleted = false
                                )
                            )
                        }
                        nextDate = nextDate.plusDays(14)
                    }
                    list
                } else {
                    val origMonth = YearMonth.from(origDate)
                    var targetMonth = origMonth.plusMonths(1)
                    while (!targetMonth.isAfter(futureHorizonMonth)) {
                        val projectedDay = minOf(origDate.dayOfMonth, targetMonth.lengthOfMonth())
                        val projectedDate = LocalDate.of(targetMonth.year, targetMonth.month, projectedDay)
                        val projectedMillis = DateUtils.localDateToMillis(projectedDate)
                        val alreadyExists = allTransactions.any { existing ->
                            (existing.title.equals(tx.title, ignoreCase = true) ||
                                    existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                                    existing.title.equals("Cancelled " + tx.title.replace(" (Planned)", ""), ignoreCase = true) ||
                                    existing.title.contains("Cancelled " + tx.title, ignoreCase = true) ||
                                    (existing.id > 0 && existing.id == tx.id)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == projectedDate
                        }
                        if (!alreadyExists) {
                            val synthId = -(Math.abs((tx.id.toString() + "_exp_" + projectedDate.toString()).hashCode()) + 10000)
                            list.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = projectedMillis,
                                    isCompleted = false
                                )
                            )
                        }
                        targetMonth = targetMonth.plusMonths(1)
                    }
                    list
                }
            } else {
                listOf(tx)
            }
        }

        val isAllAccounts = activeAccount == "All Accounts"
        val isPrimary = activeAccount == "Primary Account" || activeAccount.equals("Primary", ignoreCase = true) || isAllAccounts

        val accountTransactions = if (isAllAccounts) {
            expandedTransactions
        } else {
            expandedTransactions.filter {
                it.accountName.equals(activeAccount, ignoreCase = true) ||
                        (activeAccount == "Primary Account" && (it.accountName.isBlank() || it.accountName.equals("Primary", ignoreCase = true)))
            }
        }

        // Calculate carryover for current pay period from prior pay periods
        val earliestPriorTxMillis = accountTransactions.filter { it.dateMillis < cycleStartMillis }.minOfOrNull { it.dateMillis }
        val accStartPayday = if (earliestPriorTxMillis != null) {
            DateUtils.getPaydayOnOrBefore(DateUtils.millisToLocalDate(earliestPriorTxMillis), anchor)
        } else {
            currentPayday
        }

        var accCarryover = 0.0
        var cPayday = accStartPayday
        while (cPayday.isBefore(currentPayday)) {
            val cPStartMillis = DateUtils.localDateToMillis(cPayday)
            val cPEndMillis = DateUtils.localDateToMillis(cPayday.plusDays(14))

            val cPaychecks = if (isPrimary) {
                accountTransactions.filter {
                    it.isIncome && it.category == "Paycheck" && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
                }
            } else emptyList()

            val cPaycheck = if (isPrimary) {
                if (cPaychecks.isNotEmpty()) cPaychecks.sumOf { it.amount } else defaultPaycheck
            } else 0.0

            val cUsedIds = cPaychecks.map { it.id }.toSet()
            val cOtherInc = accountTransactions.filter {
                it.isIncome && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis && !cUsedIds.contains(it.id)
            }.sumOf { it.amount }

            val cExp = accountTransactions.filter {
                !it.isIncome && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
            }.sumOf { it.amount }

            accCarryover += (cPaycheck + cOtherInc - cExp)
            cPayday = cPayday.plusDays(14)
        }

        // Transactions in current pay period
        val periodTransactions = accountTransactions.filter {
            it.dateMillis >= cycleStartMillis && it.dateMillis < cycleEndMillis
        }

        val periodPaychecks = if (isPrimary) {
            periodTransactions.filter { it.isIncome && it.category == "Paycheck" }
        } else emptyList()

        val paycheckAmount = if (isPrimary) {
            if (periodPaychecks.isNotEmpty()) periodPaychecks.sumOf { it.amount } else defaultPaycheck
        } else 0.0

        val usedPaycheckIds = periodPaychecks.map { it.id }.toSet()
        val otherIncome = periodTransactions.filter {
            it.isIncome && !usedPaycheckIds.contains(it.id)
        }.sumOf { it.amount }

        val totalIncome = paycheckAmount + otherIncome
        val totalAvailable = accCarryover + totalIncome

        // Also factor in planned bills falling into this cycle if not already tracked as transaction
        val validExpenses = periodTransactions.filter { !it.isIncome && !it.title.contains("Cancelled", ignoreCase = true) }
        val periodExpenses = validExpenses.sumOf { it.amount }

        val completedExpenses = validExpenses.filter { it.isCompleted }.sumOf { it.amount }
        val pendingExpenses = validExpenses.filter { !it.isCompleted }.sumOf { it.amount }

        val remainingBalance = totalAvailable - periodExpenses
        val clearedBalance = totalAvailable - completedExpenses

        val daysRemaining = if (today.isBefore(cycleStart)) {
            14L
        } else if (today.isAfter(cycleEnd)) {
            0L
        } else {
            maxOf(0L, ChronoUnit.DAYS.between(today, cycleEnd) + 1)
        }

        val spentPercentage = if (totalAvailable > 0) {
            ((periodExpenses / totalAvailable) * 100).toInt().coerceIn(0, 100)
        } else {
            if (periodExpenses > 0) 100 else 0
        }

        return CurrentPayCycleSummary(
            startDate = cycleStart,
            endDate = cycleEnd,
            payday = currentPayday,
            accountName = activeAccount,
            paycheckAmount = paycheckAmount,
            otherIncome = otherIncome,
            totalIncome = totalIncome,
            carryover = accCarryover,
            totalAvailable = totalAvailable,
            totalExpenses = periodExpenses,
            completedExpenses = completedExpenses,
            pendingExpenses = pendingExpenses,
            remainingBalance = remainingBalance,
            clearedBalance = clearedBalance,
            daysRemaining = daysRemaining,
            spentPercentage = spentPercentage
        )
    }
}
