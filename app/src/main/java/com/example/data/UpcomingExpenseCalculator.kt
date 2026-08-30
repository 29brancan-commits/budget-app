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
        activeAccountName: String = "Primary Account",
        anchor: LocalDate = LocalDate.of(2026, 7, 30),
        today: LocalDate = LocalDate.now()
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
            payPeriodEnd = currentCycleEnd
        )

        val upcomingExpenses = getUpcomingExpenses(
            allTransactions = allTransactions,
            plannedExpenses = plannedExpenses,
            activeAccountName = activeAccountName,
            today = today,
            payPeriodStart = upcomingCycleStart,
            payPeriodEnd = upcomingCycleEnd
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
        activeAccountName: String = "Primary Account",
        today: LocalDate = LocalDate.now(),
        payPeriodStart: LocalDate? = null,
        payPeriodEnd: LocalDate? = null
    ): List<UpcomingExpenseItem> {
        fun isAccountMatching(acc: String): Boolean {
            if (activeAccountName == "All Accounts" || acc.isBlank()) return true
            if (acc.equals(activeAccountName, ignoreCase = true)) return true
            if (activeAccountName == "Primary Account" && (acc.contains("Primary", ignoreCase = true) || acc.isBlank())) return true
            return false
        }

        if (payPeriodStart != null && payPeriodEnd != null) {
            val items = mutableListOf<UpcomingExpenseItem>()
            val pStart = payPeriodStart
            val pEnd = payPeriodEnd

            // 1. Existing uncompleted expense transactions in DB falling within [pStart, pEnd]
            val uncompletedTx = allTransactions.filter { tx ->
                !tx.isIncome &&
                        !tx.isCompleted &&
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
                            accountName = tx.accountName.ifEmpty { "Primary Account" },
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
                        val alreadyInDb = allTransactions.any { existing ->
                            !existing.isIncome &&
                                    (existing.title.equals(tx.title, ignoreCase = true) ||
                                     existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                                     existing.title.equals("Cancelled " + tx.title.replace(" (Planned)", ""), ignoreCase = true) ||
                                     existing.title.contains("Cancelled " + tx.title, ignoreCase = true)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == projDate
                        }
                        if (!alreadyInItems && !alreadyInDb) {
                            items.add(
                                UpcomingExpenseItem(
                                    id = "rec_${tx.id}_${projDate}",
                                    title = tx.title,
                                    amount = tx.amount,
                                    dueDate = projDate,
                                    category = tx.category.ifEmpty { "Expense" },
                                    accountName = tx.accountName.ifEmpty { "Primary Account" },
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
                    bill.dueDate.toIntOrNull() ?: 15
                }

                monthsCovered.forEach { m ->
                    val dueDateInMonth = LocalDate.of(
                        m.year,
                        m.month,
                        minOf(origDay, m.lengthOfMonth())
                    )

                    if (!dueDateInMonth.isBefore(pStart) && !dueDateInMonth.isAfter(pEnd)) {
                        // Check if transaction exists for this bill in month m
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

                        if (!isCancelledForMonth) {
                            if (matchingTx == null) {
                                items.add(
                                    UpcomingExpenseItem(
                                        id = "plan_${bill.id}_${m}",
                                        title = bill.title,
                                        amount = bill.minAmount,
                                        dueDate = dueDateInMonth,
                                        category = "Planned Bill",
                                        accountName = bill.targetAccount.ifEmpty { "Primary Account" },
                                        isTransaction = false,
                                        isAutoPay = bill.isAutoPay,
                                        plannedExpenseRef = bill
                                    )
                                )
                            } else if (!matchingTx.isCompleted && !matchingTx.title.contains("Cancelled", ignoreCase = true) && matchingTx.amount > 0.0) {
                                // If matchingTx exists and is uncompleted, make sure it's in items
                                val alreadyInItems = items.any { it.transactionRef?.id == matchingTx.id || (it.title.equals(bill.title, ignoreCase = true) && it.dueDate == dueDateInMonth) }
                                if (!alreadyInItems) {
                                    items.add(
                                        UpcomingExpenseItem(
                                            id = "tx_${matchingTx.id}",
                                            title = matchingTx.title,
                                            amount = matchingTx.amount,
                                            dueDate = DateUtils.millisToLocalDate(matchingTx.dateMillis),
                                            category = matchingTx.category.ifEmpty { "Planned Bill" },
                                            accountName = matchingTx.accountName.ifEmpty { "Primary Account" },
                                            isTransaction = true,
                                            isAutoPay = matchingTx.isAutoPay,
                                            transactionRef = matchingTx,
                                            plannedExpenseRef = bill
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            return items.sortedBy { it.dueDate }
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
                    !tx.isCompleted &&
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
                    accountName = tx.accountName.ifEmpty { "Primary Account" },
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
                bill.dueDate.toIntOrNull() ?: 15
            }

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
                if (thisMonthTx.isCompleted) {
                    // Current month bill is paid! Show next month's due date
                    items.add(
                        UpcomingExpenseItem(
                            id = "plan_${bill.id}_next",
                            title = bill.title,
                            amount = bill.minAmount,
                            dueDate = nextMonthDueDate,
                            category = "Planned Bill",
                            accountName = bill.targetAccount.ifEmpty { "Primary Account" },
                            isTransaction = false,
                            isAutoPay = bill.isAutoPay,
                            plannedExpenseRef = bill
                        )
                    )
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
                if (!thisMonthCancelled) {
                    if (!thisMonthDueDate.isBefore(today)) {
                        // Due today or later this month
                        items.add(
                            UpcomingExpenseItem(
                                id = "plan_${bill.id}_curr",
                                title = bill.title,
                                amount = bill.minAmount,
                                dueDate = thisMonthDueDate,
                                category = "Planned Bill",
                                accountName = bill.targetAccount.ifEmpty { "Primary Account" },
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
                                    id = "plan_${bill.id}_overdue",
                                    title = bill.title,
                                    amount = bill.minAmount,
                                    dueDate = thisMonthDueDate,
                                    category = "Planned Bill",
                                    accountName = bill.targetAccount.ifEmpty { "Primary Account" },
                                    isTransaction = false,
                                    isAutoPay = bill.isAutoPay,
                                    plannedExpenseRef = bill
                                )
                            )
                        } else {
                            items.add(
                                UpcomingExpenseItem(
                                    id = "plan_${bill.id}_next",
                                    title = bill.title,
                                    amount = bill.minAmount,
                                    dueDate = nextMonthDueDate,
                                    category = "Planned Bill",
                                    accountName = bill.targetAccount.ifEmpty { "Primary Account" },
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

        return items.sortedBy { it.dueDate }
    }
}
