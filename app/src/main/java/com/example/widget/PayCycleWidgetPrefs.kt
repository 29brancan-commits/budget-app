package com.example.widget

import android.content.Context
import android.content.SharedPreferences
import com.example.data.BudgetDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

object PayCycleWidgetPrefs {

    private const val PREFS_NAME = "com.example.widget.PayCycleWidgetPrefs"
    private const val KEY_PREFIX_ACCOUNT = "selected_account_widget_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getSelectedAccount(context: Context, appWidgetId: Int, defaultAccount: String = "Primary Account"): String {
        val saved = getPrefs(context).getString(KEY_PREFIX_ACCOUNT + appWidgetId, null)
        return saved ?: defaultAccount
    }

    fun setSelectedAccount(context: Context, appWidgetId: Int, account: String) {
        getPrefs(context).edit().putString(KEY_PREFIX_ACCOUNT + appWidgetId, account).apply()
    }

    fun clearWidgetAccount(context: Context, appWidgetId: Int) {
        getPrefs(context).edit().remove(KEY_PREFIX_ACCOUNT + appWidgetId).apply()
    }

    suspend fun getAvailableAccounts(context: Context): List<String> = withContext(Dispatchers.IO) {
        try {
            val db = BudgetDatabase.getDatabase(context)
            val dao = db.transactionDao()

            val rawSettingsAccounts = dao.getSetting("all_accounts")?.value
            val baseList = if (rawSettingsAccounts.isNullOrBlank()) {
                listOf("All Accounts", "Primary Account")
            } else {
                val parsed = rawSettingsAccounts.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                if (!parsed.contains("All Accounts")) {
                    listOf("All Accounts") + parsed
                } else {
                    parsed
                }
            }

            val allTransactions = dao.getAllTransactions().first()
            val txAccounts = allTransactions.map { it.accountName.trim() }.filter { it.isNotBlank() }

            val planned = dao.getAllPlannedExpenses().first()
            val plannedAccounts = planned.map { it.targetAccount.trim() }.filter { it.isNotBlank() }

            val allAccounts = (baseList + txAccounts + plannedAccounts).distinct()
            if (allAccounts.isEmpty()) {
                listOf("All Accounts", "Primary Account")
            } else {
                allAccounts
            }
        } catch (e: Exception) {
            listOf("All Accounts", "Primary Account")
        }
    }

    suspend fun cycleAccount(context: Context, appWidgetId: Int, forward: Boolean): String = withContext(Dispatchers.IO) {
        val accounts = getAvailableAccounts(context)
        if (accounts.isEmpty()) return@withContext "Primary Account"

        val currentAccount = getSelectedAccount(context, appWidgetId, accounts.firstOrNull { it == "Primary Account" } ?: accounts.first())
        val currentIndex = accounts.indexOfFirst { it.equals(currentAccount, ignoreCase = true) }.let { if (it == -1) 0 else it }

        val newIndex = if (forward) {
            (currentIndex + 1) % accounts.size
        } else {
            if (currentIndex - 1 < 0) accounts.size - 1 else currentIndex - 1
        }

        val newAccount = accounts[newIndex]
        setSelectedAccount(context, appWidgetId, newAccount)
        newAccount
    }
}
