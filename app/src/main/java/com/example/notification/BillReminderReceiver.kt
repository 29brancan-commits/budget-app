package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.BudgetDatabase
import com.example.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_COMPLETE = "com.example.notification.ACTION_MARK_COMPLETE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BillReminderReceiver", "Received intent action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            // Reschedule all reminders after device reboot or app update
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = BudgetDatabase.getDatabase(context)
                    val bills = db.transactionDao().getAllPlannedExpenses().first()
                    val txs = db.transactionDao().getAllTransactions().first()
                    BillReminderScheduler.rescheduleAll(context, bills, txs)
                } catch (e: Exception) {
                    Log.e("BillReminderReceiver", "Error rescheduling reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (ACTION_MARK_COMPLETE == action) {
            val billId = intent.getIntExtra(BillReminderScheduler.EXTRA_BILL_ID, 0)
            val originalId = intent.getIntExtra(BillReminderScheduler.EXTRA_ORIGINAL_ID, 0)
            val isTx = intent.getBooleanExtra(BillReminderScheduler.EXTRA_IS_TX, false)
            val title = intent.getStringExtra(BillReminderScheduler.EXTRA_TITLE) ?: "Bill"

            // Dismiss notification immediately
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(billId)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = BudgetDatabase.getDatabase(context)
                    if (isTx) {
                        val allTx = db.transactionDao().getAllTransactions().first()
                        val tx = allTx.find { it.id == originalId }
                        if (tx != null) {
                            db.transactionDao().updateTransaction(tx.copy(isCompleted = true))
                        }
                    } else {
                        // For planned bill, insert completed transaction record
                        val bills = db.transactionDao().getAllPlannedExpenses().first()
                        val bill = bills.find { it.id == originalId }
                        if (bill != null) {
                            val newTx = TransactionEntity(
                                title = bill.title,
                                amount = bill.minAmount,
                                dateMillis = System.currentTimeMillis(),
                                category = "Planned Bill",
                                isIncome = false,
                                isCompleted = true,
                                accountName = bill.targetAccount,
                                isAutoPay = bill.isAutoPay
                            )
                            db.transactionDao().insertTransaction(newTx)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(
                            context,
                            "✓ '$title' marked as paid",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("BillReminderReceiver", "Error marking bill complete from notification action", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        if (BillReminderScheduler.ACTION_BILL_REMINDER == action) {
            val title = intent.getStringExtra(BillReminderScheduler.EXTRA_TITLE) ?: "Upcoming Bill"
            val minAmount = intent.getDoubleExtra(BillReminderScheduler.EXTRA_AMOUNT, 0.0)
            val dueDate = intent.getStringExtra(BillReminderScheduler.EXTRA_DUE_DATE) ?: ""
            val billId = intent.getIntExtra(BillReminderScheduler.EXTRA_BILL_ID, 0)
            val originalId = intent.getIntExtra(BillReminderScheduler.EXTRA_ORIGINAL_ID, 0)
            val isTx = intent.getBooleanExtra(BillReminderScheduler.EXTRA_IS_TX, false)
            val daysBefore = intent.getIntExtra(BillReminderScheduler.EXTRA_DAYS_BEFORE, 0)
            val isAutoPay = intent.getBooleanExtra(BillReminderScheduler.EXTRA_IS_AUTOPAY, false)

            // Verify in database that the expense or bill has NOT been marked complete
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = BudgetDatabase.getDatabase(context)
                    val allTx = db.transactionDao().getAllTransactions().first()

                    if (isTx) {
                        val tx = allTx.find { it.id == originalId }
                        if (tx != null && tx.isCompleted) {
                            Log.d("BillReminderReceiver", "Skipping reminder for '$title': already marked complete")
                            return@launch
                        }
                        if (tx == null && originalId != 99999) { // 99999 is test notification
                            Log.d("BillReminderReceiver", "Skipping reminder for '$title': transaction was removed")
                            return@launch
                        }
                    } else {
                        // Check planned bill
                        val bills = db.transactionDao().getAllPlannedExpenses().first()
                        val bill = bills.find { it.id == originalId }
                        if (bill == null && originalId != 99999) {
                            Log.d("BillReminderReceiver", "Skipping reminder for '$title': planned bill was removed")
                            return@launch
                        }
                        val hasCompleted = allTx.any { tx ->
                            !tx.isIncome && tx.isCompleted &&
                                    (tx.title.equals(title, ignoreCase = true) || tx.title.contains(title, ignoreCase = true))
                        }
                        if (hasCompleted) {
                            Log.d("BillReminderReceiver", "Skipping reminder for planned bill '$title': already marked complete")
                            return@launch
                        }
                    }

                    // Not marked complete: post notification
                    postBillNotification(context, billId, originalId, isTx, title, minAmount, dueDate, daysBefore, isAutoPay)
                } catch (e: Exception) {
                    Log.e("BillReminderReceiver", "Error checking bill completion state, posting notification", e)
                    postBillNotification(context, billId, originalId, isTx, title, minAmount, dueDate, daysBefore, isAutoPay)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun postBillNotification(
        context: Context,
        billId: Int,
        originalId: Int,
        isTx: Boolean,
        title: String,
        amount: Double,
        dueDate: String,
        daysBefore: Int,
        isAutoPay: Boolean
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        BillReminderScheduler.createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            billId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Mark complete directly from notification
        val markCompleteIntent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_MARK_COMPLETE
            putExtra(BillReminderScheduler.EXTRA_BILL_ID, billId)
            putExtra(BillReminderScheduler.EXTRA_ORIGINAL_ID, originalId)
            putExtra(BillReminderScheduler.EXTRA_IS_TX, isTx)
            putExtra(BillReminderScheduler.EXTRA_TITLE, title)
        }
        val markCompletePendingIntent = PendingIntent.getBroadcast(
            context,
            billId + 500000,
            markCompleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val amountFormatted = String.format("$%.2f", amount)
        val timingText = when (daysBefore) {
            0 -> "Due TODAY ($dueDate)"
            1 -> "Due Tomorrow ($dueDate)"
            else -> "Due in $daysBefore days ($dueDate)"
        }

        val titleText = if (daysBefore == 0) {
            "🔔 Bill Due Today: $title"
        } else {
            "🔔 Bill Reminder: $title"
        }

        val bodyText = if (isAutoPay) {
            "Amount: $amountFormatted ($timingText). Scheduled for Auto-Pay."
        } else {
            "Amount: $amountFormatted ($timingText). Tap to open or mark paid."
        }

        try {
            val notification = NotificationCompat.Builder(context, BillReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titleText)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(R.drawable.ic_launcher_foreground, "✓ Mark Paid", markCompletePendingIntent)
                .build()

            notificationManager.notify(billId, notification)
        } catch (e: Exception) {
            Log.e("BillReminderReceiver", "Error posting system notification", e)
        }

        try {
            android.widget.Toast.makeText(
                context,
                "$titleText: $amountFormatted",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("BillReminderReceiver", "Error showing notification toast", e)
        }
    }
}
