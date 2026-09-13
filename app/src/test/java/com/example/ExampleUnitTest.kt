package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testIncomeScalingToBiWeeklyCycle() {
    // 14-day pay cycle calculations
    val weeklyAmount = 500.0
    val biWeeklyAmount = 1500.0
    val monthlyAmount = 3000.0

    // Weekly contributes x2
    val weeklyScaled = weeklyAmount * 2.0
    assertEquals(1000.0, weeklyScaled, 0.001)

    // Bi-Weekly contributes x1
    val biWeeklyScaled = biWeeklyAmount * 1.0
    assertEquals(1500.0, biWeeklyScaled, 0.001)

    // Monthly contributes (amount * 12) / 26
    val monthlyScaled = (monthlyAmount * 12.0) / 26.0
    assertEquals(1384.615, monthlyScaled, 0.01)

    // Total combined sum
    val totalScaled = weeklyScaled + biWeeklyScaled + monthlyScaled
    assertEquals(3884.615, totalScaled, 0.01)
  }

  @Test
  fun testRecurringTransactionProjection() {
    // Current viewed month is July 2026
    val selectedMonth = java.time.YearMonth.of(2026, 7)

    // Original transaction starts in June 2026
    val origDate = java.time.LocalDate.of(2026, 6, 15)
    val origMonth = java.time.YearMonth.from(origDate)

    // Check if viewed month is after original month
    assertTrue(selectedMonth > origMonth)

    // Calculate projected date for July 2026
    val projectedDay = minOf(origDate.dayOfMonth, selectedMonth.lengthOfMonth())
    val projectedDate = java.time.LocalDate.of(selectedMonth.year, selectedMonth.month, projectedDay)

    assertEquals(15, projectedDay)
    assertEquals(java.time.LocalDate.of(2026, 7, 15), projectedDate)
  }

  @Test
  fun testDarkModeToggleLogic() {
    var isDarkMode = false
    fun toggle() {
        isDarkMode = !isDarkMode
    }

    assertFalse(isDarkMode)
    toggle()
    assertTrue(isDarkMode)
    toggle()
    assertFalse(isDarkMode)
  }

  @Test
  fun testAccountRenameAndFilterLogic() {
    val initialAccounts = listOf("Primary Account", "Savings Account")
    
    // Rename "Savings Account" to "Emergency Fund"
    val oldName = "Savings Account"
    val newName = "Emergency Fund"
    val renamedAccounts = initialAccounts.map { if (it == oldName) newName else it }
    assertEquals(listOf("Primary Account", "Emergency Fund"), renamedAccounts)
    
    // Delete "Emergency Fund"
    val deletedName = "Emergency Fund"
    val filteredAccounts = renamedAccounts.filter { it != deletedName }
    assertEquals(listOf("Primary Account"), filteredAccounts)
  }

  @Test
  fun testPlannedBillCreationAndAutoExpensing() {
    // Test creation of standard planned bill entity
    val billTitle = "Water Utility"
    val dueDateStr = "2026-06-25"
    val minAmt = 45.50
    val targetAcc = "Primary Account"

    val plannedBill = com.example.data.PlannedExpenseEntity(
        id = 1,
        title = billTitle,
        dueDate = dueDateStr,
        minAmount = minAmt,
        targetAccount = targetAcc
    )

    assertEquals("Water Utility", plannedBill.title)
    assertEquals("2026-06-25", plannedBill.dueDate)
    assertEquals(45.50, plannedBill.minAmount, 0.0)
    assertEquals("Primary Account", plannedBill.targetAccount)

    // Simulate auto-posting of expense transaction matching planned bill
    val parsedDate = java.time.LocalDate.parse(plannedBill.dueDate)
    val parsedMillis = parsedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    val simulatedTransaction = com.example.data.TransactionEntity(
        id = 10,
        title = plannedBill.title,
        amount = plannedBill.minAmount,
        dateMillis = parsedMillis,
        category = "Bills",
        isIncome = false,
        accountName = plannedBill.targetAccount
    )

    assertFalse(simulatedTransaction.isIncome)
    assertEquals("Water Utility", simulatedTransaction.title)
    assertEquals(45.50, simulatedTransaction.amount, 0.0)
    assertEquals("Bills", simulatedTransaction.category)
    assertEquals("Primary Account", simulatedTransaction.accountName)
  }

  @Test
  fun testThresholdWarningSystem() {
    // Case 1: Expenses exceed income -> should trigger over budget
    val overBudgetBalance = com.example.ui.AccountBalance(
        accountName = "Primary Account",
        totalIncome = 1500.00,
        totalExpenses = 1800.00,
        remainingBalance = -300.00
    )
    val isOverBudgetTrue = overBudgetBalance.totalExpenses > overBudgetBalance.totalIncome
    assertTrue(isOverBudgetTrue)
    assertTrue(overBudgetBalance.remainingBalance < 0.0)

    // Case 2: Expenses do not exceed income -> normal
    val healthyBalance = com.example.ui.AccountBalance(
        accountName = "Savings Account",
        totalIncome = 500.00,
        totalExpenses = 200.00,
        remainingBalance = 300.00
    )
    val isOverBudgetFalse = healthyBalance.totalExpenses > healthyBalance.totalIncome
    assertFalse(isOverBudgetFalse)
    assertFalse(healthyBalance.remainingBalance < 0.0)
  }

  @Test
  fun testRecurringExpenseProjectsEveryMonthAfterInitialDate() {
    // Initial expense entered on August 31, 2026
    val initialDate = java.time.LocalDate.of(2026, 8, 31)
    val origMonth = java.time.YearMonth.from(initialDate)
    
    // Project across 6 future months
    val projectedDates = mutableListOf<java.time.LocalDate>()
    var targetMonth = origMonth.plusMonths(1)
    val endMonth = origMonth.plusMonths(6)
    
    while (!targetMonth.isAfter(endMonth)) {
        val projDay = minOf(initialDate.dayOfMonth, targetMonth.lengthOfMonth())
        projectedDates.add(java.time.LocalDate.of(targetMonth.year, targetMonth.month, projDay))
        targetMonth = targetMonth.plusMonths(1)
    }
    
    assertEquals(6, projectedDates.size)
    // September (30 days) -> Sep 30
    assertEquals(java.time.LocalDate.of(2026, 9, 30), projectedDates[0])
    // October (31 days) -> Oct 31
    assertEquals(java.time.LocalDate.of(2026, 10, 31), projectedDates[1])
    // November (30 days) -> Nov 30
    assertEquals(java.time.LocalDate.of(2026, 11, 30), projectedDates[2])
    // December (31 days) -> Dec 31
    assertEquals(java.time.LocalDate.of(2026, 12, 31), projectedDates[3])
    // January (31 days) -> Jan 31
    assertEquals(java.time.LocalDate.of(2027, 1, 31), projectedDates[4])
    // February (28 days in 2027) -> Feb 28
    assertEquals(java.time.LocalDate.of(2027, 2, 28), projectedDates[5])
  }

  @Test
  fun testCurrentAndUpcomingCycleExpensesWidgetLogic() {
    val anchor = java.time.LocalDate.of(2026, 7, 30)
    val today = java.time.LocalDate.of(2026, 8, 27)

    // Current cycle: Aug 27 to Sep 9
    // Upcoming cycle: Sep 10 to Sep 23
    val currentCycleStart = com.example.data.DateUtils.getPaydayOnOrBefore(today, anchor)
    val currentCycleEnd = currentCycleStart.plusDays(13)
    val upcomingCycleStart = currentCycleStart.plusDays(14)
    val upcomingCycleEnd = upcomingCycleStart.plusDays(13)

    assertEquals(java.time.LocalDate.of(2026, 8, 27), currentCycleStart)
    assertEquals(java.time.LocalDate.of(2026, 9, 9), currentCycleEnd)
    assertEquals(java.time.LocalDate.of(2026, 9, 10), upcomingCycleStart)
    assertEquals(java.time.LocalDate.of(2026, 9, 23), upcomingCycleEnd)

    val currentCycleTx = com.example.data.TransactionEntity(
        id = 1,
        title = "Electric Bill",
        amount = 120.00,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 30)),
        category = "Utilities",
        isIncome = false,
        isCompleted = false
    )

    val upcomingCycleTx = com.example.data.TransactionEntity(
        id = 2,
        title = "Car Insurance",
        amount = 150.00,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 9, 15)),
        category = "Insurance",
        isIncome = false,
        isCompleted = false
    )

    val bundle = com.example.data.UpcomingExpenseCalculator.getExpensesForCurrentAndUpcomingCycles(
        allTransactions = listOf(currentCycleTx, upcomingCycleTx),
        plannedExpenses = emptyList(),
        activeAccountName = "Primary Account",
        anchor = anchor,
        today = today
    )

    assertEquals(1, bundle.currentCycleExpenses.size)
    assertEquals("Electric Bill", bundle.currentCycleExpenses[0].title)
    assertEquals(120.00, bundle.totalCurrentDue, 0.001)

    assertEquals(1, bundle.upcomingCycleExpenses.size)
    assertEquals("Car Insurance", bundle.upcomingCycleExpenses[0].title)
    assertEquals(150.00, bundle.totalUpcomingDue, 0.001)

    assertEquals(270.00, bundle.totalCombinedDue, 0.001)
    assertEquals(2, bundle.allExpenses.size)
  }

  @Test
  fun testUpcomingExpenseDeletionReferences() {
    val tx = com.example.data.TransactionEntity(
        id = 42,
        title = "Internet Bill",
        amount = 75.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 30)),
        category = "Utilities",
        isIncome = false,
        isCompleted = false
    )

    val item = com.example.data.UpcomingExpenseItem(
        id = "tx_42",
        title = tx.title,
        amount = tx.amount,
        dueDate = java.time.LocalDate.of(2026, 8, 30),
        category = tx.category,
        accountName = "Primary Account",
        isTransaction = true,
        transactionRef = tx
    )

    assertEquals("tx_42", item.id)
    assertEquals(42, item.transactionRef?.id)
    assertEquals(75.0, item.amount, 0.001)
  }

  @Test
  fun testCancelledExpenseSuppressionInUpcomingCalculator() {
    val date = java.time.LocalDate.of(2026, 8, 28)
    val dateMillis = com.example.data.DateUtils.localDateToMillis(date)
    val cancelledTx = com.example.data.TransactionEntity(
        id = 101,
        title = "Cancelled house",
        amount = 0.0,
        dateMillis = dateMillis,
        category = "Expense",
        isIncome = false,
        isCompleted = true
    )
    val plannedBill = com.example.data.PlannedExpenseEntity(
        id = 5,
        title = "house",
        dueDate = "28",
        minAmount = 2228.0,
        targetAccount = "USAA"
    )

    val items = com.example.data.UpcomingExpenseCalculator.getUpcomingExpenses(
        allTransactions = listOf(cancelledTx),
        plannedExpenses = listOf(plannedBill),
        activeAccountName = "All Accounts",
        today = java.time.LocalDate.of(2026, 8, 29),
        payPeriodStart = java.time.LocalDate.of(2026, 8, 27),
        payPeriodEnd = java.time.LocalDate.of(2026, 9, 9)
    )

    // The cancelled house expense should NOT appear in upcoming items for that period
    val houseItems = items.filter { it.title.equals("house", ignoreCase = true) }
    assertEquals(0, houseItems.size)
  }

  @Test
  fun testPayCycleCalculatorForCurrentPayCycle() {
    val anchor = java.time.LocalDate.of(2026, 7, 30)
    val today = java.time.LocalDate.of(2026, 8, 29)
    val defaultPaycheck = 2500.0

    val tx1 = com.example.data.TransactionEntity(
        id = 1,
        title = "Paycheck",
        amount = 2600.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 27)),
        category = "Paycheck",
        isIncome = true,
        isCompleted = true,
        accountName = "Primary Account"
    )

    val tx2 = com.example.data.TransactionEntity(
        id = 2,
        title = "Electric Bill",
        amount = 150.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 30)),
        category = "Utilities",
        isIncome = false,
        isCompleted = true,
        accountName = "Primary Account"
    )

    val tx3 = com.example.data.TransactionEntity(
        id = 3,
        title = "Groceries",
        amount = 200.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 9, 2)),
        category = "Groceries",
        isIncome = false,
        isCompleted = false,
        accountName = "Primary Account"
    )

    val summary = com.example.data.PayCycleCalculator.calculateCurrentPayCycle(
        allTransactions = listOf(tx1, tx2, tx3),
        plannedExpenses = emptyList(),
        activeAccount = "Primary Account",
        anchor = anchor,
        defaultPaycheck = defaultPaycheck,
        today = today
    )

    assertEquals(java.time.LocalDate.of(2026, 8, 27), summary.startDate)
    assertEquals(java.time.LocalDate.of(2026, 9, 9), summary.endDate)
    assertEquals(2600.0, summary.totalIncome, 0.001)
    assertEquals(350.0, summary.totalExpenses, 0.001)
    assertEquals(150.0, summary.completedExpenses, 0.001)
    assertEquals(200.0, summary.pendingExpenses, 0.001)
    assertEquals(2250.0, summary.remainingBalance, 0.001)
    assertEquals(2450.0, summary.clearedBalance, 0.001)
    assertEquals(12L, summary.daysRemaining) // Aug 29 to Sep 9 inclusive is 12 days
    assertEquals(13, summary.spentPercentage) // 350 / 2600 = 13.46%
  }

  @Test
  fun testPayCycleCalculatorForDifferentAccounts() {
    val anchor = java.time.LocalDate.of(2026, 7, 30)
    val today = java.time.LocalDate.of(2026, 8, 29)
    val defaultPaycheck = 2500.0

    val txPrimary = com.example.data.TransactionEntity(
        id = 1,
        title = "Paycheck",
        amount = 3000.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 27)),
        category = "Paycheck",
        isIncome = true,
        isCompleted = true,
        accountName = "Primary Account"
    )

    val txSavings = com.example.data.TransactionEntity(
        id = 2,
        title = "Deposit",
        amount = 500.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 28)),
        category = "Income",
        isIncome = true,
        isCompleted = true,
        accountName = "Savings"
    )

    val txSavingsExp = com.example.data.TransactionEntity(
        id = 3,
        title = "Transfer Out",
        amount = 100.0,
        dateMillis = com.example.data.DateUtils.localDateToMillis(java.time.LocalDate.of(2026, 8, 30)),
        category = "Transfer",
        isIncome = false,
        isCompleted = true,
        accountName = "Savings"
    )

    // Calculate for Savings account
    val savingsSummary = com.example.data.PayCycleCalculator.calculateCurrentPayCycle(
        allTransactions = listOf(txPrimary, txSavings, txSavingsExp),
        plannedExpenses = emptyList(),
        activeAccount = "Savings",
        anchor = anchor,
        defaultPaycheck = defaultPaycheck,
        today = today
    )

    assertEquals("Savings", savingsSummary.accountName)
    assertEquals(500.0, savingsSummary.totalIncome, 0.001)
    assertEquals(100.0, savingsSummary.totalExpenses, 0.001)
    assertEquals(400.0, savingsSummary.remainingBalance, 0.001)

    // Calculate for All Accounts
    val allSummary = com.example.data.PayCycleCalculator.calculateCurrentPayCycle(
        allTransactions = listOf(txPrimary, txSavings, txSavingsExp),
        plannedExpenses = emptyList(),
        activeAccount = "All Accounts",
        anchor = anchor,
        defaultPaycheck = defaultPaycheck,
        today = today
    )

    assertEquals("All Accounts", allSummary.accountName)
    assertEquals(3500.0, allSummary.totalIncome, 0.001)
    assertEquals(100.0, allSummary.totalExpenses, 0.001)
    assertEquals(3400.0, allSummary.remainingBalance, 0.001)
  }

  @Test
  fun testUndoActionItemExecution() = kotlinx.coroutines.runBlocking {
    var counter = 10
    val undoStack = mutableListOf<com.example.ui.UndoActionItem>()

    // Push action that incremented counter
    counter += 5
    undoStack.add(
        com.example.ui.UndoActionItem(
            title = "Increment",
            description = "Incremented counter by 5",
            onUndo = { counter -= 5 }
        )
    )

    assertEquals(15, counter)
    assertTrue(undoStack.isNotEmpty())

    // Perform undo
    val lastAction = undoStack.removeAt(undoStack.lastIndex)
    lastAction.onUndo()

    assertEquals(10, counter)
    assertTrue(undoStack.isEmpty())
  }

  @Test
  fun testPlannedExpenseDefaultsToDayOfDueDate() {
    val planned = com.example.data.PlannedExpenseEntity(
        title = "Electric Bill",
        dueDate = "2026-09-15",
        minAmount = 120.0
    )
    // 0 corresponds to Day of Due Date (at 9:00 AM)
    assertEquals(0, planned.reminderDaysBefore)
  }

  @Test
  fun testExpenseReminderExclusionWhenCompleted() {
    val bill = com.example.data.PlannedExpenseEntity(
        id = 1,
        title = "Internet Bill",
        dueDate = "2026-09-10",
        minAmount = 75.0,
        reminderDaysBefore = 0
    )

    val transactionsUncompleted = listOf(
        com.example.data.TransactionEntity(
            id = 10,
            title = "Internet Bill",
            amount = 75.0,
            dateMillis = System.currentTimeMillis(),
            category = "Utilities",
            isIncome = false,
            isCompleted = false,
            reminderDaysBefore = 0
        )
    )

    // Check completed status
    val isCompletedUnpaid = transactionsUncompleted.any { tx ->
        !tx.isIncome && tx.isCompleted &&
                (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true))
    }
    assertFalse("Bill should not be completed when unpaid", isCompletedUnpaid)

    val transactionsCompleted = listOf(
        com.example.data.TransactionEntity(
            id = 11,
            title = "Internet Bill",
            amount = 75.0,
            dateMillis = System.currentTimeMillis(),
            category = "Utilities",
            isIncome = false,
            isCompleted = true,
            reminderDaysBefore = 0
        )
    )

    val isCompletedPaid = transactionsCompleted.any { tx ->
        !tx.isIncome && tx.isCompleted &&
                (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true))
    }
    assertTrue("Bill should be recognized as completed when paid transaction exists", isCompletedPaid)
  }

  @Test
  fun testConfirmationBeforeMarkingCompleteLogic() {
    val uncompletedTx = com.example.data.TransactionEntity(
        id = 42,
        title = "Water Bill",
        amount = 55.0,
        dateMillis = System.currentTimeMillis(),
        category = "Utilities",
        isIncome = false,
        isCompleted = false
    )

    var pendingConfirmationTx: com.example.data.TransactionEntity? = null
    var actualCompletedStatus = uncompletedTx.isCompleted

    // Handler intercepting completion requests
    val handleRequestToggleCompleted: (com.example.data.TransactionEntity) -> Unit = { tx ->
      if (!tx.isCompleted) {
        pendingConfirmationTx = tx
      } else {
        actualCompletedStatus = !tx.isCompleted
      }
    }

    // User taps complete button
    handleRequestToggleCompleted(uncompletedTx)

    // Verify confirmation is requested before changing status
    assertNotNull("Confirmation dialog must be triggered for uncompleted expense", pendingConfirmationTx)
    assertEquals(42, pendingConfirmationTx?.id)
    assertFalse("Transaction must remain uncompleted until user confirms", actualCompletedStatus)

    // User dismisses/cancels confirmation dialog
    pendingConfirmationTx = null
    assertFalse("Status should remain uncompleted after dismissing dialog", actualCompletedStatus)

    // User taps complete again and confirms
    handleRequestToggleCompleted(uncompletedTx)
    assertNotNull(pendingConfirmationTx)

    // User confirms in dialog
    actualCompletedStatus = true
    pendingConfirmationTx = null
    assertTrue("Transaction is marked complete only upon confirmation", actualCompletedStatus)
  }
}
