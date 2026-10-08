package com.example

import com.example.data.PlannedExpenseEntity
import com.example.data.TransactionEntity
import com.example.data.UpcomingExpenseCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpcomingExpenseCalculatorTest {

    private val cycleStart = LocalDate.of(2026, 8, 1)
    private val cycleEnd = LocalDate.of(2026, 8, 14)
    private val today = LocalDate.of(2026, 8, 2)

    @Test
    fun `expense marked paid is not returned in upcoming expenses`() {
        val unpaidTx = TransactionEntity(
            id = 1,
            title = "Water Bill",
            amount = 45.0,
            dateMillis = com.example.data.DateUtils.localDateToMillis(LocalDate.of(2026, 8, 5)),
            category = "Utilities",
            isIncome = false,
            isCompleted = false,
            status = "due"
        )
        val paidTx = TransactionEntity(
            id = 2,
            title = "Electric Bill",
            amount = 120.0,
            dateMillis = com.example.data.DateUtils.localDateToMillis(LocalDate.of(2026, 8, 6)),
            category = "Utilities",
            isIncome = false,
            isCompleted = true,
            status = "paid"
        )

        val expenses = UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = listOf(unpaidTx, paidTx),
            plannedExpenses = emptyList(),
            activeAccountName = "All Accounts",
            today = today,
            payPeriodStart = cycleStart,
            payPeriodEnd = cycleEnd
        )

        assertEquals(1, expenses.size)
        assertEquals("Water Bill", expenses[0].title)
        assertFalse(expenses.any { it.title == "Electric Bill" })
    }

    @Test
    fun `planned bill marked paid via transaction is not returned in upcoming expenses`() {
        val plannedBill = PlannedExpenseEntity(
            id = 10,
            title = "Internet Bill",
            minAmount = 80.0,
            dueDate = "8",
            targetAccount = "Primary Account"
        )
        val paidPaymentTx = TransactionEntity(
            id = 3,
            title = "Internet Bill",
            amount = 80.0,
            dateMillis = com.example.data.DateUtils.localDateToMillis(LocalDate.of(2026, 8, 7)),
            category = "Planned Bill",
            isIncome = false,
            isCompleted = true,
            status = "paid"
        )

        val expenses = UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = listOf(paidPaymentTx),
            plannedExpenses = listOf(plannedBill),
            activeAccountName = "All Accounts",
            today = today,
            payPeriodStart = cycleStart,
            payPeriodEnd = cycleEnd
        )

        assertTrue("Expected no expenses due since planned bill was paid", expenses.isEmpty())
    }

    @Test
    fun `recurring expense paid in cycle is not returned in upcoming expenses`() {
        val recurringTx = TransactionEntity(
            id = 20,
            title = "Streaming Service",
            amount = 15.0,
            dateMillis = com.example.data.DateUtils.localDateToMillis(LocalDate.of(2026, 7, 10)),
            category = "Entertainment",
            isIncome = false,
            isRecurring = true,
            isCompleted = true,
            status = "paid"
        )
        val currentPaymentTx = TransactionEntity(
            id = 21,
            title = "Streaming Service",
            amount = 15.0,
            dateMillis = com.example.data.DateUtils.localDateToMillis(LocalDate.of(2026, 8, 10)),
            category = "Entertainment",
            isIncome = false,
            isCompleted = true,
            status = "paid"
        )

        val expenses = UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = listOf(recurringTx, currentPaymentTx),
            plannedExpenses = emptyList(),
            activeAccountName = "All Accounts",
            today = today,
            payPeriodStart = cycleStart,
            payPeriodEnd = cycleEnd
        )

        assertTrue("Expected no recurring expenses due since it was paid", expenses.isEmpty())
    }
}
