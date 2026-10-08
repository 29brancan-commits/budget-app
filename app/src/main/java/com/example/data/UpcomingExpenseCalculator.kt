package com.example.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class UpcomingExpenseItem(
    val id: String,
    val title: String,
    val amount: Double,
    val dueDate: LocalDate,
    val category: String,
    val accountName: String,
    val isTransaction: Boolean,
    val isAutoPay: Boolean = false,
    val transactionRef: TransactionEntity? = null,
    val plannedExpenseRef: PlannedExpenseEntity? = null
)

data class CycleExpensesBundle(
    val currentCycleStart: LocalDate,
    val currentCycleEnd: LocalDate,
    val currentCycleExpenses: List<UpcomingExpenseItem>,
    val upcomingCycleStart: LocalDate,
    val upcomingCycleEnd: LocalDate,
    val upcomingCycleExpenses: List<UpcomingExpenseItem>
) {
    val totalCurrentDue: Double get() = currentCycleExpenses.sumOf { it.amount }
    val totalUpcomingDue: Double get() = upcomingCycleExpenses.sumOf { it.amount }
    val totalCombinedDue: Double get() = totalCurrentDue + totalUpcomingDue
    val allExpenses: List<UpcomingExpenseItem> get() = (currentCycleExpenses + upcomingCycleExpenses).distinctBy { it.id }.sortedBy { it.dueDate }
    val isEmpty: Boolean get() = currentCycleExpenses.isEmpty() && upcomingCycleExpenses.isEmpty()
}

object UpcomingExpenseCalculator {

    fun getExpensesForCurrentAndUpcomingCycles(
        allTransactions: List<TransactionEntity>,
        plannedExpenses: List<PlannedExpenseEntity>,
        activeAccountName: String = "All Accounts",
        anchor: LocalDate = LocalDate.of(2026, 7, 30),
        today: LocalDate = LocalDate.now(),
        allDebts: List<DebtEntity> = emptyList()
    ): CycleExpensesBundle {
        val currentCycleStart = DateUtils.getPaydayOnOrBefore(today, anchor)
        val currentCycleEnd = currentCycleStart.plusDays(13)
        val upcomingCycleStart = currentCycleStart.plusDays(14)
        val upcomingCycleEnd = upcomingCycleStart.plusDays(13)

        val currentExpenses = getUpcomingExpenses(
            allTransactions = allTransactions,
            plannedExpenses = plannedExpenses,
            activeAccountName = activeAccountName,
            today = today,
            payPeriodStart = currentCycleStart,
            payPeriodEnd = currentCycleEnd,
            allDebts = allDebts
        )

        val upcomingExpenses = getUpcomingExpenses(
            allTransactions = allTransactions,
            plannedExpenses = plannedExpenses,
            activeAccountName = activeAccountName,
            today = today,
            payPeriodStart = upcomingCycleStart,
            payPeriodEnd = upcomingCycleEnd,
            allDebts = allDebts
        )

        return CycleExpensesBundle(
            currentCycleStart = currentCycleStart,
            currentCycleEnd = currentCycleEnd,
            currentCycleExpenses = currentExpenses,
            upcomingCycleStart = upcomingCycleStart,
            upcomingCycleEnd = upcomingCycleEnd,
            upcomingCycleExpenses = upcomingExpenses
        )
    }

    fun getUpcomingExpenses(
        allTransactions: List<TransactionEntity>,
        plannedExpenses: List<PlannedExpenseEntity>,
        activeAccountName: String = "All Accounts",
        today: LocalDate = LocalDate.now(),
        payPeriodStart: LocalDate? = null,
        payPeriodEnd: LocalDate? = null,
        allDebts: List<DebtEntity> = emptyList()
    ): List<UpcomingExpenseItem> {
        fun isAccountMatching(acc: String): Boolean {
            if (activeAccountName == "All Accounts" || acc.isBlank()) return true
            if (acc.equals(activeAccountName, ignoreCase = true)) return true
            val isTargetPrimary = activeAccountName.equals("Primary Account", ignoreCase = true) || activeAccountName.equals("Primary", ignoreCase = true)
            val isAccPrimary = acc.equals("Primary Account", ignoreCase = true) || acc.equals("Primary", ignoreCase = true)
            if (isTargetPrimary && isAccPrimary) return true
            return false
        }

        fun isTxPaidOrCleared(tx: TransactionEntity): Boolean {
            if (tx.isCompleted || tx.isPaidOrCleared || tx.isPaid || tx.isCleared) return true
            val st = tx.status.lowercase().trim()
            if (st in listOf("paid", "cleared", "completed", "settled", "done")) return true
            val eff = tx.getEffectiveStatus()
            return eff in listOf("paid", "cleared")
        }

        if (payPeriodStart != null && payPeriodEnd != null) {
            val items = mutableListOf<UpcomingExpenseItem>()
            val pStart = payPeriodStart
            val pEnd = payPeriodEnd

            // 1. Existing uncompleted expense transactions in DB falling within [pStart, pEnd]
            val uncompletedTx = allTransactions.filter { tx ->
                !tx.isIncome &&
                        !isTxPaidOrCleared(tx) &&
                        !tx.isStartingBalanceOrAdjustment &&
                        tx.amount > 0.0 &&
                        !tx.title.contains("Cancelled", ignoreCase = true) &&
                        !tx.title.contains("Deleted", ignoreCase = true) &&
                        isAccountMatching(tx.accountName)
            }

            uncompletedTx.forEach { tx ->
                val date = DateUtils.millisToLocalDate(tx.dateMillis)
                if (!date.isBefore(pStart) && !date.isAfter(pEnd)) {
                    items.add(
                        UpcomingExpenseItem(
                            id = "tx_${tx.id}",
                            title = tx.title,
                            amount = tx.amount,
                            dueDate = date,
                            category = tx.category.ifEmpty { "Expense" },
                            accountName = tx.accountName,
                            isTransaction = true,
                            isAutoPay = tx.isAutoPay,
                            transactionRef = tx
                        )
                    )
                }
            }

            // 2. Recurring expense transactions projected into [pStart, pEnd]
            val monthsCovered = mutableSetOf<YearMonth>()
            var mCursor = YearMonth.from(pStart)
            val endMonth = YearMonth.from(pEnd)
            while (!mCursor.isAfter(endMonth)) {
                monthsCovered.add(mCursor)
                mCursor = mCursor.plusMonths(1)
            }

            allTransactions.filter { tx -> !tx.isIncome && tx.isRecurring && isAccountMatching(tx.accountName) }.forEach { tx ->
                val origDate = DateUtils.millisToLocalDate(tx.dateMillis)
                monthsCovered.forEach { m ->
                    val projDay = minOf(origDate.dayOfMonth, m.lengthOfMonth())
                    val projDate = LocalDate.of(m.year, m.month, projDay)
                    if (!projDate.isBefore(pStart) && !projDate.isAfter(pEnd)) {
                        val alreadyInItems = items.any { it.title.equals(tx.title, ignoreCase = true) && it.dueDate == projDate }

                        val isPaidOrCompletedInCycle = allTransactions.any { existing ->
                            !existing.isIncome &&
                                    !existing.isStartingBalanceOrAdjustment &&
                                    isTxPaidOrCleared(existing) &&
                                    (existing.title.equals(tx.title, ignoreCase = true) ||
                                     existing.title.equals(tx.title.replace(" (Planned)", ""), ignoreCase = true) ||
                                     existing.title.contains(tx.title, ignoreCase = true) ||
                                     tx.title.contains(existing.title, ignoreCase = true) ||
                                     (existing.id > 0 && existing.id == tx.id)) &&
                                    (DateUtils.millisToLocalDate(existing.dateMillis) == projDate ||
                                     (!DateUtils.millisToLocalDate(existing.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(existing.dateMillis).isAfter(pEnd)) ||
                                     YearMonth.from(DateUtils.millisToLocalDate(existing.dateMillis)) == m ||
                                     Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(existing.dateMillis), projDate)) <= 10)
                        }

                        val isCancelled = allTransactions.any { existing ->
                            !existing.isIncome &&
                                    (existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                                     existing.title.contains("Cancelled " + tx.title, ignoreCase = true)) &&
                                    (DateUtils.millisToLocalDate(existing.dateMillis) == projDate ||
                                     YearMonth.from(DateUtils.millisToLocalDate(existing.dateMillis)) == m)
                        }

                        val alreadyInDb = allTransactions.any { existing ->
                            !existing.isIncome &&
                                    (existing.title.equals(tx.title, ignoreCase = true) || (existing.id > 0 && existing.id == tx.id)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == projDate
                        }

                        if (!alreadyInItems && !alreadyInDb && !isPaidOrCompletedInCycle && !isCancelled) {
                            items.add(
                                UpcomingExpenseItem(
                                    id = "rec_${tx.id}_${projDate}",
                                    title = tx.title,
                                    amount = tx.amount,
                                    dueDate = projDate,
                                    category = tx.category.ifEmpty { "Expense" },
                                    accountName = tx.accountName,
                                    isTransaction = false,
                                    isAutoPay = tx.isAutoPay,
                                    transactionRef = tx
                                )
                            )
                        }
                    }
                }
            }

            // 3. Planned Bills falling into [pStart, pEnd]
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

                    if (!dueDateInMonth.isBefore(pStart) && !dueDateInMonth.isAfter(pEnd)) {
                        val hasPaidOrCompletedTx = allTransactions.any { tx ->
                            !tx.isIncome &&
                                    !tx.isStartingBalanceOrAdjustment &&
                                    isTxPaidOrCleared(tx) &&
                                    (tx.title.equals(bill.title, ignoreCase = true) ||
                                     tx.title.contains(bill.title, ignoreCase = true) ||
                                     bill.title.contains(tx.title, ignoreCase = true)) &&
                                    (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                     (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)) ||
                                     Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(tx.dateMillis), dueDateInMonth)) <= 15)
                        }

                        val isCancelledForMonth = allTransactions.any { tx ->
                            !tx.isIncome &&
                                    (tx.title.equals("Cancelled " + bill.title, ignoreCase = true) ||
                                     tx.title.contains("Cancelled " + bill.title, ignoreCase = true)) &&
                                    (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                     (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)))
                        }

                        if (!hasPaidOrCompletedTx && !isCancelledForMonth) {
                            val matchingUncompletedTx = allTransactions.firstOrNull { tx ->
                                !tx.isIncome &&
                                        !tx.isStartingBalanceOrAdjustment &&
                                        !isTxPaidOrCleared(tx) &&
                                        !tx.title.contains("Cancelled", ignoreCase = true) &&
                                        tx.amount > 0.0 &&
                                        (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true) || bill.title.contains(tx.title, ignoreCase = true)) &&
                                        (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                         (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)))
                            }

                            val alreadyInItems = items.any {
                                it.transactionRef?.id == matchingUncompletedTx?.id ||
                                (it.title.equals(bill.title, ignoreCase = true) && (it.dueDate == dueDateInMonth || Math.abs(ChronoUnit.DAYS.between(it.dueDate, dueDateInMonth)) <= 7))
                            }

                            if (!alreadyInItems) {
                                if (matchingUncompletedTx != null) {
                                    items.add(
                                        UpcomingExpenseItem(
                                            id = "tx_${matchingUncompletedTx.id}",
                                            title = matchingUncompletedTx.title,
                                            amount = matchingUncompletedTx.amount,
                                            dueDate = DateUtils.millisToLocalDate(matchingUncompletedTx.dateMillis),
                                            category = matchingUncompletedTx.category.ifEmpty { "Planned Bill" },
                                            accountName = matchingUncompletedTx.accountName,
                                            isTransaction = true,
                                            isAutoPay = matchingUncompletedTx.isAutoPay,
                                            transactionRef = matchingUncompletedTx,
                                            plannedExpenseRef = bill
                                        )
                                    )
                                } else {
                                    items.add(
                                        UpcomingExpenseItem(
                                            id = "plan_${bill.id}_${m}",
                                            title = bill.title,
                                            amount = bill.minAmount,
                                            dueDate = dueDateInMonth,
                                            category = "Planned Bill",
                                            accountName = bill.targetAccount,
                                            isTransaction = false,
                                            isAutoPay = bill.isAutoPay,
                                            plannedExpenseRef = bill
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Active Debt Minimum Payments falling into [pStart, pEnd]
            allDebts.filter { !it.isPaidOff && it.balance > 0.0 && it.minimumPayment > 0.0 && isAccountMatching(it.linkedAccountName ?: "") }.forEach { debt ->
                val dueDay = debt.billDueDay.coerceIn(1, 28)
                val debtTitle = "${debt.name} (Minimum Payment)"
                val cleanDebtName = DateUtils.normalizeDebtOrBillName(debt.name)

                monthsCovered.forEach { m ->
                    val dueDateInMonth = LocalDate.of(
                        m.year,
                        m.month,
                        minOf(dueDay, m.lengthOfMonth())
                    )

                    if (!dueDateInMonth.isBefore(pStart) && !dueDateInMonth.isAfter(pEnd)) {
                        val alreadyInPlanned = plannedExpenses.any { bill ->
                            bill.title.equals(debtTitle, ignoreCase = true) ||
                            DateUtils.normalizeDebtOrBillName(bill.title).equals(cleanDebtName, ignoreCase = true)
                        }

                        val hasPaidOrCompletedTx = allTransactions.any { tx ->
                            !tx.isIncome &&
                                    !tx.isStartingBalanceOrAdjustment &&
                                    isTxPaidOrCleared(tx) &&
                                    (tx.title.equals(debt.name, ignoreCase = true) ||
                                     tx.title.contains(debt.name, ignoreCase = true) ||
                                     DateUtils.normalizeDebtOrBillName(tx.title).equals(cleanDebtName, ignoreCase = true)) &&
                                    (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                     (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)) ||
                                     Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(tx.dateMillis), dueDateInMonth)) <= 15)
                        }

                        val isCancelledForMonth = allTransactions.any { tx ->
                            !tx.isIncome &&
                                    (tx.title.equals("Cancelled " + debt.name, ignoreCase = true) ||
                                     tx.title.contains("Cancelled " + debt.name, ignoreCase = true)) &&
                                    (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                     (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)))
                        }

                        if (!alreadyInPlanned && !hasPaidOrCompletedTx && !isCancelledForMonth) {
                            val matchingUncompletedTx = allTransactions.firstOrNull { tx ->
                                !tx.isIncome &&
                                        !tx.isStartingBalanceOrAdjustment &&
                                        !isTxPaidOrCleared(tx) &&
                                        !tx.title.contains("Cancelled", ignoreCase = true) &&
                                        tx.amount > 0.0 &&
                                        (tx.title.equals(debt.name, ignoreCase = true) || tx.title.contains(debt.name, ignoreCase = true) || DateUtils.normalizeDebtOrBillName(tx.title).equals(cleanDebtName, ignoreCase = true)) &&
                                        (YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == m ||
                                         (!DateUtils.millisToLocalDate(tx.dateMillis).isBefore(pStart) && !DateUtils.millisToLocalDate(tx.dateMillis).isAfter(pEnd)))
                            }

                            val alreadyInItems = items.any {
                                it.transactionRef?.id == matchingUncompletedTx?.id ||
                                (it.title.contains(debt.name, ignoreCase = true) && (it.dueDate == dueDateInMonth || Math.abs(ChronoUnit.DAYS.between(it.dueDate, dueDateInMonth)) <= 7))
                            }

                            if (!alreadyInItems) {
                                if (matchingUncompletedTx != null) {
                                    items.add(
                                        UpcomingExpenseItem(
                                            id = "tx_${matchingUncompletedTx.id}",
                                            title = matchingUncompletedTx.title,
                                            amount = matchingUncompletedTx.amount,
                                            dueDate = DateUtils.millisToLocalDate(matchingUncompletedTx.dateMillis),
                                            category = "Debt Payment",
                                            accountName = matchingUncompletedTx.accountName,
                                            isTransaction = true,
                                            isAutoPay = matchingUncompletedTx.isAutoPay,
                                            transactionRef = matchingUncompletedTx
                                        )
                                    )
                                } else {
                                    items.add(
                                        UpcomingExpenseItem(
                                            id = "debt_min_${debt.id}_${m}",
                                            title = debtTitle,
                                            amount = debt.minimumPayment,
                                            dueDate = dueDateInMonth,
                                            category = "Debt Payment",
                                            accountName = debt.linkedAccountName ?: "Primary Account",
                                            isTransaction = false,
                                            isAutoPay = false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return items.filter { item ->
                val tx = item.transactionRef
                if (tx != null) !isTxPaidOrCleared(tx) else true
            }.distinctBy { it.id }.sortedBy { it.dueDate }
        }

        val currentMonth = YearMonth.from(today)
        val nextMonth = currentMonth.plusMonths(1)
        val items = mutableListOf<UpcomingExpenseItem>()

        // Expand recurring transactions if not already expanded (income = bi-weekly +14 days, expense = monthly)
        val expandedTransactions = mutableListOf<TransactionEntity>()
        allTransactions.forEach { tx ->
            expandedTransactions.add(tx)
            if (tx.isRecurring) {
                val origDate = DateUtils.millisToLocalDate(tx.dateMillis)
                if (tx.isIncome) {
                    var nextDate = origDate.plusDays(14)
                    val limitDate = nextMonth.atEndOfMonth()
                    while (!nextDate.isAfter(limitDate)) {
                        val nextMillis = DateUtils.localDateToMillis(nextDate)
                        val alreadyExists = allTransactions.any { existing ->
                            existing.title.equals(tx.title, ignoreCase = true) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == nextDate
                        }
                        if (!alreadyExists && !nextDate.isBefore(today)) {
                            expandedTransactions.add(
                                tx.copy(
                                    id = (tx.id * 10000 + nextDate.dayOfYear),
                                    dateMillis = nextMillis,
                                    isCompleted = false
                                )
                            )
                        }
                        nextDate = nextDate.plusDays(14)
                    }
                } else {
                    val origMonth = YearMonth.from(origDate)
                    var m = origMonth.plusMonths(1)
                    val maxM = nextMonth.plusMonths(1)
                    while (!m.isAfter(maxM)) {
                        val projectedDay = minOf(origDate.dayOfMonth, m.lengthOfMonth())
                        val projectedDate = LocalDate.of(m.year, m.month, projectedDay)
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
                            expandedTransactions.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = projectedMillis,
                                    isCompleted = false
                                )
                            )
                        }
                        m = m.plusMonths(1)
                    }
                }
            }
        }

        // 1. Add uncompleted expense transactions from DB
        val uncompletedTx = expandedTransactions.filter { tx ->
            !tx.isIncome &&
                    !isTxPaidOrCleared(tx) &&
                    tx.amount > 0.0 &&
                    !tx.title.contains("Cancelled", ignoreCase = true) &&
                    !tx.title.contains("Deleted", ignoreCase = true) &&
                    isAccountMatching(tx.accountName)
        }

        uncompletedTx.forEach { tx ->
            val date = DateUtils.millisToLocalDate(tx.dateMillis)
            items.add(
                UpcomingExpenseItem(
                    id = "tx_${tx.id}",
                    title = tx.title,
                    amount = tx.amount,
                    dueDate = date,
                    category = tx.category.ifEmpty { "Expense" },
                    accountName = tx.accountName,
                    isTransaction = true,
                    isAutoPay = tx.isAutoPay,
                    transactionRef = tx
                )
            )
        }

        // 2. Process Planned Bills
        plannedExpenses.filter { isAccountMatching(it.targetAccount) }.forEach { bill ->
            val origDay = try {
                LocalDate.parse(bill.dueDate).dayOfMonth
            } catch (e: Exception) {
                bill.dueDate.filter { it.isDigit() }.toIntOrNull() ?: 15
            }.coerceIn(1, 28)

            val thisMonthDueDate = LocalDate.of(
                currentMonth.year,
                currentMonth.month,
                minOf(origDay, currentMonth.lengthOfMonth())
            )

            val nextMonthDueDate = LocalDate.of(
                nextMonth.year,
                nextMonth.month,
                minOf(origDay, nextMonth.lengthOfMonth())
            )

            // Check if there's any transaction matching this bill in current month
            val thisMonthTx = expandedTransactions.firstOrNull { tx ->
                !tx.isIncome &&
                        (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true)) &&
                        YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == currentMonth
            }

            if (thisMonthTx != null) {
                if (isTxPaidOrCleared(thisMonthTx)) {
                    // Current month bill is paid/cleared! It should not be carried over.
                } else {
                    // Transaction exists and is uncompleted - already handled in uncompletedTx above!
                }
            } else {
                val thisMonthCancelled = expandedTransactions.any { tx ->
                    !tx.isIncome &&
                            (tx.title.equals("Cancelled " + bill.title, ignoreCase = true) ||
                             tx.title.contains("Cancelled " + bill.title, ignoreCase = true)) &&
                            YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == currentMonth
                }
                val hasCompletedInMonth = expandedTransactions.any { tx ->
                    !tx.isIncome && isTxPaidOrCleared(tx) &&
                            (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true)) &&
                            DateUtils.millisToLocalDate(tx.dateMillis).isAfter(thisMonthDueDate.minusDays(20))
                }
                if (!thisMonthCancelled && !hasCompletedInMonth) {
                    if (!thisMonthDueDate.isBefore(today)) {
                        // Due today or later this month
                        items.add(
                            UpcomingExpenseItem(
                                id = "plan_${bill.id}_${currentMonth}",
                                title = bill.title,
                                amount = bill.minAmount,
                                dueDate = thisMonthDueDate,
                                category = "Planned Bill",
                                accountName = bill.targetAccount,
                                isTransaction = false,
                                isAutoPay = bill.isAutoPay,
                                plannedExpenseRef = bill
                            )
                        )
                    } else {
                        val daysPassed = ChronoUnit.DAYS.between(thisMonthDueDate, today)
                        if (daysPassed <= 14) {
                            items.add(
                                UpcomingExpenseItem(
                                    id = "plan_${bill.id}_${currentMonth}",
                                    title = bill.title,
                                    amount = bill.minAmount,
                                    dueDate = thisMonthDueDate,
                                    category = "Planned Bill",
                                    accountName = bill.targetAccount,
                                    isTransaction = false,
                                    isAutoPay = bill.isAutoPay,
                                    plannedExpenseRef = bill
                                )
                            )
                        }
                    }
                }
            }
        }

        return items.filter { item ->
            val tx = item.transactionRef
            if (tx != null) !isTxPaidOrCleared(tx) else true
        }.distinctBy { it.id }.sortedBy { it.dueDate }
    }
}
