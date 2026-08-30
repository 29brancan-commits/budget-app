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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BillReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BillReminderReceiver", "Received intent action: $action")

        if (Intent.ACTION_BOOT_COMPLETED == action || Intent.ACTION_MY_PACKAGE_REPLACED == action) {
            // Reschedule all reminders after device reboot or app update
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = BudgetDatabase.getDatabase(context)
                    val bills = db.transactionDao().getAllPlannedExpenses().first()
                    val txs = db.transactionDao().getAllTransactions().first()
                    BillReminderScheduler.rescheduleAll(context, bills, txs)
                } catch (e: Exception) {
                    Log.e("BillReminderReceiver", "Error rescheduling reminders on boot", e)
                }
            }
            return
        }

        if (BillReminderScheduler.ACTION_BILL_REMINDER == action) {
            val title = intent.getStringExtra(BillReminderScheduler.EXTRA_TITLE) ?: "Upcoming Bill"
            val minAmount = intent.getDoubleExtra(BillReminderScheduler.EXTRA_AMOUNT, 0.0)
            val dueDate = intent.getStringExtra(BillReminderScheduler.EXTRA_DUE_DATE) ?: ""
            val billId = intent.getIntExtra(BillReminderScheduler.EXTRA_BILL_ID, 0)
            val daysBefore = intent.getIntExtra(BillReminderScheduler.EXTRA_DAYS_BEFORE, 1)
            val isAutoPay = intent.getBooleanExtra(BillReminderScheduler.EXTRA_IS_AUTOPAY, false)

            postBillNotification(context, billId, title, minAmount, dueDate, daysBefore, isAutoPay)
        }
    }

    private fun postBillNotification(
        context: Context,
        billId: Int,
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

        val amountFormatted = String.format("$%.2f", amount)
        val timingText = when (daysBefore) {
            0 -> "Due TODAY ($dueDate)"
            1 -> "Due Tomorrow ($dueDate)"
            else -> "Due in $daysBefore days ($dueDate)"
        }

        val bodyText = if (isAutoPay) {
            "Amount: $amountFormatted ($timingText). Scheduled for Auto-Pay."
        } else {
            "Amount: $amountFormatted ($timingText). Tap to view or record payment."
        }

        try {
            val notification = NotificationCompat.Builder(context, BillReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("🔔 Bill Reminder: $title")
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(billId, notification)
        } catch (e: Exception) {
            Log.e("BillReminderReceiver", "Error posting system notification", e)
        }

        try {
            android.widget.Toast.makeText(
                context,
                "🔔 Expense Reminder: $title ($amountFormatted)",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Log.e("BillReminderReceiver", "Error showing notification toast", e)
        }
    }
}
