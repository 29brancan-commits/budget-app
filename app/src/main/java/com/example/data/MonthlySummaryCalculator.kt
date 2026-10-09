package com.example.data

import com.example.ui.BudgetUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

data class MonthlyBillItem(
    val id: String,
    val title: String,
    val amount: Double,
    val dueDate: LocalDate,
    val category: String,
    val targetAccount: String,
    val isAutoPay: Boolean,
    val isPaid: Boolean,
    val status: String, // "paid", "due", "pending", "scheduled"
    val source: String, // "Planned Bill", "Recurring Expense", "Debt Minimum", "Transaction"
    val transactionRef: TransactionEntity? = null,
    val plannedExpenseRef: PlannedExpenseEntity? = null,
    val debtRef: DebtEntity? = null
)

data class MonthlyIncomeItem(
    val id: String,
    val title: String,
    val amount: Double,
    val date: LocalDate,
    val category: String,
    val accountName: String,
    val isProjected: Boolean,
    val transactionRef: TransactionEntity? = null
)

data class MonthlyCategoryExpense(
    val category: String,
    val amount: Double,
    val percentageOfTotal: Float,
    val itemCount: Int
)

data class MonthlySummaryReport(
    val month: YearMonth,
    val totalIncome: Double,
    val totalPlannedExpenses: Double,
    val netSurplus: Double,
    val commitmentPercentage: Float,
    val totalPaidSoFar: Double,
    val totalRemainingDue: Double,
    val bills: List<MonthlyBillItem>,
    val incomeItems: List<MonthlyIncomeItem>,
    val categories: List<MonthlyCategoryExpense>,
    val daysLeftInMonth: Int,
    val remainingDailyBudget: Double,
    val isDeficit: Boolean = netSurplus < 0.0
)

object MonthlySummaryCalculator {

    fun calculate(
        month: YearMonth,
        uiState: BudgetUiState,
        activeAccountFilter: String = "All Accounts"
    ): MonthlySummaryReport {
        val today = LocalDate.now()
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()
        val daysInMonth = month.lengthOfMonth()

        val daysLeft = if (month == YearMonth.from(today)) {
            (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
        } else if (month.isBefore(YearMonth.from(today))) {
            0
        } else {
            daysInMonth
        }

        fun isAccountMatching(acc: String): Boolean {
            if (activeAccountFilter == "All Accounts" || acc.isBlank()) return true
            return acc.equals(activeAccountFilter, ignoreCase = true)
        }

        // =========================================================================
        // 1. CALCULATE MONTHLY INCOME
        // =========================================================================
        val incomeList = mutableListOf<MonthlyIncomeItem>()
        val usedPaycheckDates = mutableSetOf<LocalDate>()

        // 1a. Explicit Income Transactions in this month
        val dbIncomeTxs = uiState.allTransactions.filter { tx ->
            tx.isIncome &&
            !tx.title.contains("Cancelled", ignoreCase = true) &&
            isAccountMatching(tx.accountName)
        }

        dbIncomeTxs.forEach { tx ->
            val date = DateUtils.millisToLocalDate(tx.dateMillis)
            if (YearMonth.from(date) == month) {
                val isPaycheck = tx.category.equals("Paycheck", ignoreCase = true) ||
                        tx.title.contains("Paycheck", ignoreCase = true) ||
                        tx.title.contains("Salary", ignoreCase = true)
                if (isPaycheck) {
                    usedPaycheckDates.add(date)
                }

                incomeList.add(
                    MonthlyIncomeItem(
                        id = "tx_inc_${tx.id}",
                        title = tx.title,
                        amount = tx.amount,
                        date = date,
                        category = tx.category.ifEmpty { "Income" },
                        accountName = tx.accountName,
                        isProjected = false,
                        transactionRef = tx
                    )
                )
            }
        }

        // 1b. Scheduled Paydays / Income Sources falling into this month
        // Previous months past the current cycle's month without cycles should not factor in projected income or expenses
        val earliestCycleMonth = uiState.allCalculatedPayPeriods.map { YearMonth.from(it.payday) }.minOrNull() ?: YearMonth.now()
        val isPastMonthWithoutCycles = month.isBefore(YearMonth.now()) && month.isBefore(earliestCycleMonth)

        val anchor = uiState.anchorPayday
        val primaryAccountName = uiState.allAccounts.firstOrNull { it != "All Accounts" } ?: "Primary Account"

        if (!isPastMonthWithoutCycles) {
            if (uiState.incomeSources.isNotEmpty()) {
                uiState.incomeSources.forEach { src ->
                when (src.frequency.lowercase(java.util.Locale.US)) {
                    "weekly" -> {
                        // Project weekly income for every 7 days in month
                        var d = monthStart
                        while (!d.isAfter(monthEnd)) {
                            if (d.dayOfWeek == anchor.dayOfWeek) {
                                val alreadyRecorded = incomeList.any {
                                    it.title.equals(src.name, ignoreCase = true) && it.date == d
                                }
                                if (!alreadyRecorded) {
                                    incomeList.add(
                                        MonthlyIncomeItem(
                                            id = "src_wk_${src.id}_${d}",
                                            title = src.name,
                                            amount = src.amount,
                                            date = d,
                                            category = "Paycheck",
                                            accountName = primaryAccountName,
                                            isProjected = true
                                        )
                                    )
                                }
                            }
                            d = d.plusDays(1)
                        }
                    }
                    "monthly" -> {
                        val d = month.atDay(minOf(1, month.lengthOfMonth()))
                        val alreadyRecorded = incomeList.any {
                            it.title.equals(src.name, ignoreCase = true) && YearMonth.from(it.date) == month
                        }
                        if (!alreadyRecorded) {
                            incomeList.add(
                                MonthlyIncomeItem(
                                    id = "src_m_${src.id}_${month}",
                                    title = src.name,
                                    amount = src.amount,
                                    date = d,
                                    category = "Paycheck",
                                    accountName = primaryAccountName,
                                    isProjected = true
                                )
                            )
                        }
                    }
                    else -> { // bi-weekly
                        var d = DateUtils.getPaydayOnOrBefore(monthStart, anchor)
                        while (!d.isAfter(monthEnd)) {
                            if (!d.isBefore(monthStart)) {
                                val alreadyRecorded = incomeList.any {
                                    (it.title.equals(src.name, ignoreCase = true) || it.title.contains("Paycheck", ignoreCase = true)) &&
                                    it.date == d
                                }
                                if (!alreadyRecorded) {
                                    incomeList.add(
                                        MonthlyIncomeItem(
                                            id = "src_bw_${src.id}_${d}",
                                            title = src.name,
                                            amount = src.amount,
                                            date = d,
                                            category = "Paycheck",
                                            accountName = primaryAccountName,
                                            isProjected = true
                                        )
                                    )
                                }
                            }
                            d = d.plusDays(14)
                        }
                    }
                }
            }
        } else {
            // Default Bi-Weekly Paycheck projection for paydays falling in this month
            var d = DateUtils.getPaydayOnOrBefore(monthStart, anchor)
            while (!d.isAfter(monthEnd)) {
                if (!d.isBefore(monthStart) && !usedPaycheckDates.contains(d)) {
                    val alreadyRecorded = incomeList.any {
                        it.title.contains("Paycheck", ignoreCase = true) && it.date == d
                    }
                    if (!alreadyRecorded) {
                        incomeList.add(
                            MonthlyIncomeItem(
                                id = "def_pay_${d}",
                                title = "Paycheck",
                                amount = uiState.defaultPaycheckAmount,
                                date = d,
                                category = "Paycheck",
                                accountName = primaryAccountName,
                                isProjected = true
                            )
                        )
                    }
                }
                d = d.plusDays(14)
            }
        }
        }

        val totalIncome = incomeList.sumOf { it.amount }

        // =========================================================================
        // 2. CALCULATE MONTHLY PLANNED EXPENSES & BILLS
        // =========================================================================
        val billList = mutableListOf<MonthlyBillItem>()
        val accountedTransactionIds = mutableSetOf<Int>()

        if (!isPastMonthWithoutCycles) {
            // 2a. Planned Expenses / Bills configured in database
            uiState.plannedExpenses.filter { isAccountMatching(it.targetAccount) }.forEach { bill ->
            val dueDay = try {
                LocalDate.parse(bill.dueDate).dayOfMonth
            } catch (e: Exception) {
                bill.dueDate.filter { it.isDigit() }.toIntOrNull() ?: 15
            }.coerceIn(1, 28)
            val dueDateInMonth = LocalDate.of(month.year, month.month, minOf(dueDay, daysInMonth))

            // Check if matching transaction exists for this bill in this month
            val matchingTx = uiState.allTransactions.firstOrNull { tx ->
                !tx.isIncome &&
                (tx.title.equals(bill.title, ignoreCase = true) ||
                 tx.title.contains(bill.title, ignoreCase = true) ||
                 bill.title.contains(tx.title, ignoreCase = true)) &&
                YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == month &&
                !tx.title.contains("Cancelled", ignoreCase = true) &&
                !tx.title.contains("Deleted", ignoreCase = true)
            }

            val isCancelled = uiState.allTransactions.any { tx ->
                !tx.isIncome &&
                (tx.title.equals("Cancelled " + bill.title, ignoreCase = true) ||
                 tx.title.contains("Cancelled " + bill.title, ignoreCase = true)) &&
                YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == month
            }

            if (!isCancelled) {
                val isPaid = matchingTx?.isPaidOrCleared ?: false
                val effectiveAmount = matchingTx?.amount ?: bill.minAmount
                val effectiveStatus = if (matchingTx != null) {
                    matchingTx.getEffectiveStatus()
                } else if (dueDateInMonth.isBefore(today)) {
                    "due"
                } else {
                    "scheduled"
                }

                if (matchingTx != null) {
                    accountedTransactionIds.add(matchingTx.id)
                }

                billList.add(
                    MonthlyBillItem(
                        id = "plan_${bill.id}_${month}",
                        title = bill.title,
                        amount = effectiveAmount,
                        dueDate = matchingTx?.let { DateUtils.millisToLocalDate(it.dateMillis) } ?: dueDateInMonth,
                        category = "Bills & Utilities",
                        targetAccount = bill.targetAccount,
                        isAutoPay = bill.isAutoPay,
                        isPaid = isPaid,
                        status = effectiveStatus,
                        source = "Planned Bill",
                        transactionRef = matchingTx,
                        plannedExpenseRef = bill
                    )
                )
            }
        }

        // 2b. Debts linked to bills / minimum payments (e.g. Credit Card & Loan minimum payments)
        uiState.allDebts.filter { !it.isPaidOff && it.balance > 0.0 && (it.isLinkedToBills || it.minimumPayment > 0.0) }.forEach { debt ->
            val alreadyCovered = billList.any {
                it.title.equals(debt.name, ignoreCase = true) ||
                it.title.contains(debt.name, ignoreCase = true)
            }
            if (!alreadyCovered && isAccountMatching(debt.linkedAccountName ?: "")) {
                val dueDay = debt.billDueDay.coerceIn(1, 28)
                val dueDateInMonth = LocalDate.of(month.year, month.month, minOf(dueDay, daysInMonth))

                val matchingTx = uiState.allTransactions.firstOrNull { tx ->
                    !tx.isIncome &&
                    (tx.title.equals(debt.name, ignoreCase = true) || tx.title.contains(debt.name, ignoreCase = true)) &&
                    YearMonth.from(DateUtils.millisToLocalDate(tx.dateMillis)) == month
                }

                if (matchingTx != null) {
                    accountedTransactionIds.add(matchingTx.id)
                }

                val isPaid = matchingTx?.isPaidOrCleared ?: false
                val effectiveStatus = matchingTx?.getEffectiveStatus() ?: if (dueDateInMonth.isBefore(today)) "due" else "scheduled"

                billList.add(
                    MonthlyBillItem(
                        id = "debt_${debt.id}_${month}",
                        title = "${debt.name} (Min Pay)",
                        amount = matchingTx?.amount ?: debt.minimumPayment,
                        dueDate = dueDateInMonth,
                        category = "Debt Payment",
                        targetAccount = debt.linkedAccountName ?: primaryAccountName,
                        isAutoPay = false,
                        isPaid = isPaid,
                        status = effectiveStatus,
                        source = "Debt Minimum",
                        transactionRef = matchingTx,
                        debtRef = debt
                    )
                )
            }
        }
        }

        // 2c. All Expense Transactions in DB for this month (one-time or recurring items)
        val monthExpenses = uiState.allTransactions.filter { tx ->
            !tx.isIncome &&
            tx.amount > 0.0 &&
            !tx.title.contains("Cancelled", ignoreCase = true) &&
            !tx.title.contains("Deleted", ignoreCase = true) &&
            isAccountMatching(tx.accountName)
        }

        monthExpenses.forEach { tx ->
            val date = DateUtils.millisToLocalDate(tx.dateMillis)
            if (YearMonth.from(date) == month && !accountedTransactionIds.contains(tx.id)) {
                // Ensure this transaction wasn't already bundled with a planned bill
                val alreadyInBills = billList.any { it.transactionRef?.id == tx.id }
                if (!alreadyInBills) {
                    billList.add(
                        MonthlyBillItem(
                            id = "tx_${tx.id}",
                            title = tx.title,
                            amount = tx.amount,
                            dueDate = date,
                            category = tx.category.ifEmpty { "General Expense" },
                            targetAccount = tx.accountName,
                            isAutoPay = tx.isAutoPay,
                            isPaid = tx.isPaidOrCleared,
                            status = tx.getEffectiveStatus(),
                            source = if (tx.isRecurring) "Recurring Expense" else "Transaction",
                            transactionRef = tx
                        )
                    )
                }
            }
        }

        // Sort bills chronologically by due date
        val sortedBills = billList.sortedBy { it.dueDate }

        val totalPlannedExpenses = sortedBills.sumOf { it.amount }
        val totalPaidSoFar = sortedBills.filter { it.isPaid }.sumOf { it.amount }
        val totalRemainingDue = sortedBills.filter { !it.isPaid }.sumOf { it.amount }

        val netSurplus = totalIncome - totalPlannedExpenses
        val commitmentPercentage = if (totalIncome > 0.0) {
            ((totalPlannedExpenses / totalIncome) * 100.0).toFloat().coerceAtLeast(0f)
        } else {
            if (totalPlannedExpenses > 0.0) 100f else 0f
        }

        val remainingDailyBudget = if (daysLeft > 0 && netSurplus > 0.0) {
            netSurplus / daysLeft
        } else {
            0.0
        }

        // 3. Category Breakdown
        val categoryGroups = sortedBills.groupBy { it.category }
        val categories = categoryGroups.map { (cat, items) ->
            val catSum = items.sumOf { it.amount }
            val pct = if (totalPlannedExpenses > 0.0) {
                ((catSum / totalPlannedExpenses) * 100.0).toFloat()
            } else 0f
            MonthlyCategoryExpense(
                category = cat,
                amount = catSum,
                percentageOfTotal = pct,
                itemCount = items.size
            )
        }.sortedByDescending { it.amount }

        return MonthlySummaryReport(
            month = month,
            totalIncome = totalIncome,
            totalPlannedExpenses = totalPlannedExpenses,
            netSurplus = netSurplus,
            commitmentPercentage = commitmentPercentage,
            totalPaidSoFar = totalPaidSoFar,
            totalRemainingDue = totalRemainingDue,
            bills = sortedBills,
            incomeItems = incomeList.sortedBy { it.date },
            categories = categories,
            daysLeftInMonth = daysLeft,
            remainingDailyBudget = remainingDailyBudget
        )
    }
}
