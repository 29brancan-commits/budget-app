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
    val spentPercentage: Int,
    val startingBalance: Double = 0.0
) {
    val cycleNet: Double get() = totalIncome - totalExpenses
    val isOverspentInCycle: Boolean get() = totalExpenses > totalIncome
}

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
                                    (DateUtils.millisToLocalDate(existing.dateMillis) == projectedDate ||
                                     (existing.isPaidOrCleared && Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(existing.dateMillis), projectedDate)) <= 10))
                        }
                        if (!alreadyExists) {
                            val synthId = -(Math.abs((tx.id.toString() + "_exp_" + projectedDate.toString()).hashCode()) + 10000)
                            list.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = projectedMillis,
                                    isCompleted = false,
                                    status = "due"
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
                it.isIncome && !it.isStartingBalanceOrAdjustment && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis && !cUsedIds.contains(it.id)
            }.sumOf { it.amount }

            val cStartingBal = accountTransactions.filter {
                it.isStartingBalanceOrAdjustment && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
            }.sumOf { if (it.isIncome) it.amount else -it.amount }

            val cExp = accountTransactions.filter {
                !it.isIncome && !it.isStartingBalanceOrAdjustment && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
            }.sumOf { it.amount }

            accCarryover += (cPaycheck + cOtherInc + cStartingBal - cExp)
            cPayday = cPayday.plusDays(14)
        }

        // Transactions in current pay period
        val periodTransactions = accountTransactions.filter {
            it.dateMillis >= cycleStartMillis && it.dateMillis < cycleEndMillis
        }

        val periodPaychecks = if (isPrimary) {
            periodTransactions.filter { it.isIncome && !it.isStartingBalanceOrAdjustment && it.category == "Paycheck" }
        } else emptyList()

        val paycheckAmount = if (isPrimary) {
            if (periodPaychecks.isNotEmpty()) periodPaychecks.sumOf { it.amount } else defaultPaycheck
        } else 0.0

        val usedPaycheckIds = periodPaychecks.map { it.id }.toSet()
        val otherIncome = periodTransactions.filter {
            it.isIncome && !it.isStartingBalanceOrAdjustment && !usedPaycheckIds.contains(it.id)
        }.sumOf { it.amount }

        val periodStartingBalance = periodTransactions.filter {
            it.isStartingBalanceOrAdjustment
        }.sumOf { if (it.isIncome) it.amount else -it.amount }

        val totalIncome = paycheckAmount + otherIncome
        val totalAvailable = accCarryover + periodStartingBalance + totalIncome

        // Also factor in planned bills falling into this cycle if not already tracked as transaction
        val validExpenses = periodTransactions.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment && !it.title.contains("Cancelled", ignoreCase = true) }
        val periodExpenses = validExpenses.sumOf { it.amount }

        val completedExpenses = validExpenses.filter { it.isCompleted || it.isPaidOrCleared }.sumOf { it.amount }
        val pendingTxExpenses = validExpenses.filter { !it.isCompleted && !it.isPaidOrCleared }.sumOf { it.amount }

        fun isAccountMatching(acc: String): Boolean {
            if (isAllAccounts || acc.isBlank()) return true
            if (acc.equals(activeAccount, ignoreCase = true)) return true
            if (isPrimary && (acc.equals("Primary Account", ignoreCase = true) || acc.equals("Primary", ignoreCase = true))) return true
            return false
        }

        val monthsCovered = mutableSetOf<YearMonth>()
        var mCursor = YearMonth.from(cycleStart)
        val endMonth = YearMonth.from(cycleEnd)
        while (!mCursor.isAfter(endMonth)) {
            monthsCovered.add(mCursor)
            mCursor = mCursor.plusMonths(1)
        }

        var plannedBillsDueAmount = 0.0
        plannedExpenses.filter { isAccountMatching(it.targetAccount) }.forEach { bill ->
            val origDay = try {
                LocalDate.parse(bill.dueDate).dayOfMonth
            } catch (e: Exception) {
                bill.dueDate.filter { it.isDigit() }.toIntOrNull() ?: 15
            }.coerceIn(1, 28)

            monthsCovered.forEach { m ->
                val dueDateInMonth = LocalDate.of(
                    m.year,
                    m.month,
                    minOf(origDay, m.lengthOfMonth())
                )

                if (!dueDateInMonth.isBefore(cycleStart) && !dueDateInMonth.isAfter(cycleEnd)) {
                    val hasPaidOrCompletedTx = allTransactions.any { tx ->
                        !tx.isIncome &&
                                !tx.isStartingBalanceOrAdjustment &&
                                tx.isPaidOrCleared &&
                                (tx.title.equals(bill.title, ignoreCase = true) ||
                                 tx.title.contains(bill.title, ignoreCase = true) ||
                                 bill.title.contains(tx.title, ignoreCase = true)) &&
                                (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                 (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(cycleStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(cycleEnd)) ||
                                 Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(tx.dateMillis), dueDateInMonth)) <= 15)
                    }

                    val matchingTx = allTransactions.firstOrNull { tx ->
                        !tx.isIncome &&
                                (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true)) &&
                                YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m
                    }

                    val isCancelledForMonth = allTransactions.any { tx ->
                        !tx.isIncome &&
                                (tx.title.equals("Cancelled " + bill.title, ignoreCase = true) ||
                                 tx.title.contains("Cancelled " + bill.title, ignoreCase = true)) &&
                                YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m
                    }

                    if (!isCancelledForMonth && !hasPaidOrCompletedTx && matchingTx == null) {
                        plannedBillsDueAmount += bill.minAmount
                    }
                }
            }
        }

        val totalExpenses = periodExpenses + plannedBillsDueAmount
        val totalPending = pendingTxExpenses + plannedBillsDueAmount

        val remainingBalance = totalAvailable - totalExpenses
        val clearedBalance = totalAvailable - completedExpenses

        val daysRemaining = if (today.isBefore(cycleStart)) {
            14L
        } else if (today.isAfter(cycleEnd)) {
            0L
        } else {
            maxOf(0L, ChronoUnit.DAYS.between(today, cycleEnd) + 1)
        }

        val spentPercentage = if (totalAvailable > 0) {
            ((totalExpenses / totalAvailable) * 100).toInt().coerceIn(0, 100)
        } else {
            if (totalExpenses > 0) 100 else 0
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
            totalExpenses = totalExpenses,
            completedExpenses = completedExpenses,
            pendingExpenses = totalPending,
            remainingBalance = remainingBalance,
            clearedBalance = clearedBalance,
            daysRemaining = daysRemaining,
            spentPercentage = spentPercentage,
            startingBalance = periodStartingBalance
        )
    }
}
