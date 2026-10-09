package com.example.data

import kotlinx.coroutines.flow.Flow

class BudgetRepository(private val transactionDao: TransactionDao) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        transactionDao.deleteTransactionById(id)
    }

    suspend fun updateAccountName(oldAccountName: String, newAccountName: String) {
        transactionDao.updateAccountName(oldAccountName, newAccountName)
    }

    suspend fun deleteTransactionsByAccount(accountName: String) {
        transactionDao.deleteTransactionsByAccount(accountName)
    }

    suspend fun getSetting(key: String): String? {
        return transactionDao.getSetting(key)?.value
    }

    suspend fun saveSetting(key: String, value: String) {
        transactionDao.insertSetting(SettingEntity(key, value))
    }

    val allIncomeSources: Flow<List<IncomeSourceEntity>> = transactionDao.getAllIncomeSources()

    suspend fun insertIncomeSource(incomeSource: IncomeSourceEntity) {
        transactionDao.insertIncomeSource(incomeSource)
    }

    suspend fun updateIncomeSource(incomeSource: IncomeSourceEntity) {
        transactionDao.updateIncomeSource(incomeSource)
    }

    suspend fun deleteIncomeSource(incomeSource: IncomeSourceEntity) {
        transactionDao.deleteIncomeSource(incomeSource)
    }

    suspend fun deleteIncomeSourceById(id: Int) {
        transactionDao.deleteIncomeSourceById(id)
    }

    val allPlannedExpenses: Flow<List<PlannedExpenseEntity>> = transactionDao.getAllPlannedExpenses()

    suspend fun insertPlannedExpense(plannedExpense: PlannedExpenseEntity) {
        transactionDao.insertPlannedExpense(plannedExpense)
    }

    suspend fun updatePlannedExpense(plannedExpense: PlannedExpenseEntity) {
        transactionDao.updatePlannedExpense(plannedExpense)
    }

    suspend fun deletePlannedExpense(plannedExpense: PlannedExpenseEntity) {
        transactionDao.deletePlannedExpense(plannedExpense)
    }

    suspend fun deletePlannedExpenseById(id: Int) {
        transactionDao.deletePlannedExpenseById(id)
    }

    val allDebts: Flow<List<DebtEntity>> = transactionDao.getAllDebts()

    suspend fun insertDebt(debt: DebtEntity) {
        transactionDao.insertDebt(debt)
    }

    suspend fun updateDebt(debt: DebtEntity) {
        transactionDao.updateDebt(debt)
    }

    suspend fun deleteDebt(debt: DebtEntity) {
        transactionDao.deleteDebt(debt)
    }

    suspend fun deleteDebtById(id: Int) {
        transactionDao.deleteDebtById(id)
    }

    val allDebtPayments: Flow<List<DebtPaymentEntity>> = transactionDao.getAllDebtPayments()

    fun getPaymentsForDebt(debtId: Int): Flow<List<DebtPaymentEntity>> {
        return transactionDao.getPaymentsForDebt(debtId)
    }

    suspend fun insertDebtPayment(payment: DebtPaymentEntity) {
        transactionDao.insertDebtPayment(payment)
    }

    suspend fun deleteDebtPayment(payment: DebtPaymentEntity) {
        transactionDao.deleteDebtPayment(payment)
    }

    suspend fun clearAllData() {
        transactionDao.clearTransactions()
        transactionDao.clearSettings()
        transactionDao.clearIncomeSources()
        transactionDao.clearPlannedExpenses()
        transactionDao.clearDebts()
        transactionDao.clearDebtPayments()
    }
}
