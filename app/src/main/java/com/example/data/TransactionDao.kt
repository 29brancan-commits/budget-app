package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateMillis ASC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("UPDATE transactions SET accountName = :newAccountName WHERE accountName = :oldAccountName")
    suspend fun updateAccountName(oldAccountName: String, newAccountName: String)

    @Query("DELETE FROM transactions WHERE accountName = :accountName")
    suspend fun deleteTransactionsByAccount(accountName: String)

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("SELECT * FROM income_sources ORDER BY id ASC")
    fun getAllIncomeSources(): Flow<List<IncomeSourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeSource(incomeSource: IncomeSourceEntity)

    @Delete
    suspend fun deleteIncomeSource(incomeSource: IncomeSourceEntity)

    @Query("DELETE FROM income_sources WHERE id = :id")
    suspend fun deleteIncomeSourceById(id: Int)

    @Query("SELECT * FROM planned_expenses ORDER BY dueDate ASC")
    fun getAllPlannedExpenses(): Flow<List<PlannedExpenseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlannedExpense(plannedExpense: PlannedExpenseEntity)

    @Update
    suspend fun updatePlannedExpense(plannedExpense: PlannedExpenseEntity)

    @Delete
    suspend fun deletePlannedExpense(plannedExpense: PlannedExpenseEntity)

    @Query("DELETE FROM planned_expenses WHERE id = :id")
    suspend fun deletePlannedExpenseById(id: Int)

    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Query("DELETE FROM income_sources")
    suspend fun clearIncomeSources()

    @Query("DELETE FROM planned_expenses")
    suspend fun clearPlannedExpenses()

    @Query("SELECT * FROM debts ORDER BY balance ASC")
    fun getAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteDebtById(id: Int)

    @Query("DELETE FROM debts")
    suspend fun clearDebts()
}
