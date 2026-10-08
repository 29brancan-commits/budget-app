package com.example.notification

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.BudgetDatabase
import com.example.data.DateUtils
import kotlinx.coroutines.flow.first
import java.time.LocalDate

class BillReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "bill_reminder_periodic_worker"
        private const val TAG = "BillReminderWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Executing periodic background bill check via WorkManager")
        return try {
            val db = BudgetDatabase.getDatabase(applicationContext)
            val bills = db.transactionDao().getAllPlannedExpenses().first()
            val txs = db.transactionDao().getAllTransactions().first()

            // 1. Reschedule alarms so exact alarms remain fresh
            BillReminderScheduler.rescheduleAll(applicationContext, bills, txs)

            // 2. Check if any bill or expense is due today and uncompleted
            val today = LocalDate.now()

            // Check planned bills
            for (bill in bills) {
                if (bill.reminderDaysBefore >= 0) {
                    val dueDate = try { LocalDate.parse(bill.dueDate) } catch (_: Exception) { null }
                    if (dueDate != null && (dueDate == today || (dueDate.isAfter(today) && dueDate.minusDays(bill.reminderDaysBefore.toLong()) <= today))) {
                        val isPaid = txs.any { tx ->
                            !tx.isIncome && tx.isCompleted &&
                                    (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true))
                        }
                        if (!isPaid) {
                            Log.d(TAG, "WorkManager found uncompleted due bill: ${bill.title}")
                            // Post notification if within target window
                            BillReminderScheduler.scheduleReminder(applicationContext, bill)
                        }
                    }
                }
            }

            // Check transactions
            for (tx in txs) {
                if (!tx.isIncome && !tx.isCompleted && tx.reminderDaysBefore >= 0) {
                    val dueDate = DateUtils.millisToLocalDate(tx.dateMillis)
                    if (dueDate == today || (dueDate.isAfter(today) && dueDate.minusDays(tx.reminderDaysBefore.toLong()) <= today)) {
                        Log.d(TAG, "WorkManager found uncompleted due expense tx: ${tx.title}")
                        BillReminderScheduler.scheduleTransactionReminder(applicationContext, tx)
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in periodic BillReminderWorker", e)
            Result.retry()
        }
    }
}
