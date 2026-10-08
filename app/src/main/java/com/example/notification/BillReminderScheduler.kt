package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.DateUtils
import com.example.data.PlannedExpenseEntity
import com.example.data.TransactionEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object BillReminderScheduler {

    const val CHANNEL_ID = "channel_bill_reminders"
    const val CHANNEL_NAME = "Bill & Expense Reminders"
    const val ACTION_BILL_REMINDER = "com.example.notification.ACTION_BILL_REMINDER"

    const val EXTRA_BILL_ID = "extra_bill_id"
    const val EXTRA_ORIGINAL_ID = "extra_original_id"
    const val EXTRA_IS_TX = "extra_is_tx"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_AMOUNT = "extra_amount"
    const val EXTRA_DUE_DATE = "extra_due_date"
    const val EXTRA_DAYS_BEFORE = "extra_days_before"
    const val EXTRA_IS_AUTOPAY = "extra_is_autopay"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and alerts for upcoming bills and scheduled expenses"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 200, 250)
                enableLights(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun sendTestNotification(context: Context, title: String = "Test Expense Reminder", amount: Double = 49.99) {
        val receiver = BillReminderReceiver()
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_BILL_REMINDER
            putExtra(EXTRA_BILL_ID, 99999)
            putExtra(EXTRA_ORIGINAL_ID, 99999)
            putExtra(EXTRA_IS_TX, true)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DUE_DATE, LocalDate.now().toString())
            putExtra(EXTRA_DAYS_BEFORE, 0)
            putExtra(EXTRA_IS_AUTOPAY, false)
        }
        receiver.onReceive(context, intent)
    }

    fun scheduleReminder(context: Context, bill: PlannedExpenseEntity) {
        val today = LocalDate.now()
        val dueDate = try {
            LocalDate.parse(bill.dueDate)
        } catch (_: Exception) {
            val day = (bill.dueDate.filter { it.isDigit() }.toIntOrNull() ?: 15).coerceIn(1, 28)
            val dateThisMonth = today.withDayOfMonth(minOf(day, today.lengthOfMonth()))
            if (!dateThisMonth.isBefore(today)) dateThisMonth else today.plusMonths(1).let { it.withDayOfMonth(minOf(day, it.lengthOfMonth())) }
        }
        scheduleGenericReminder(
            context = context,
            id = bill.id,
            title = bill.title,
            amount = bill.minAmount,
            dueDateStr = dueDate.toString(),
            daysBefore = bill.reminderDaysBefore,
            isAutoPay = bill.isAutoPay,
            isTx = false
        )
    }

    fun scheduleTransactionReminder(context: Context, tx: TransactionEntity) {
        if (tx.isIncome || tx.isCompleted) {
            cancelTransactionReminder(context, tx.id)
            return
        }
        val dueDateStr = DateUtils.millisToLocalDate(tx.dateMillis).toString()
        scheduleGenericReminder(
            context = context,
            id = tx.id,
            title = tx.title,
            amount = tx.amount,
            dueDateStr = dueDateStr,
            daysBefore = tx.reminderDaysBefore,
            isAutoPay = tx.isAutoPay,
            isTx = true
        )
    }

    private fun scheduleGenericReminder(
        context: Context,
        id: Int,
        title: String,
        amount: Double,
        dueDateStr: String,
        daysBefore: Int,
        isAutoPay: Boolean,
        isTx: Boolean
    ) {
        val requestCode = if (isTx) id + 100000 else id
        cancelReminderByRequestCode(context, requestCode)

        if (daysBefore < 0) {
            return
        }

        val dueDate = try {
            LocalDate.parse(dueDateStr)
        } catch (e: Exception) {
            Log.e("BillReminderScheduler", "Invalid date format for expense $title: $dueDateStr")
            return
        }

        val today = LocalDate.now()
        if (dueDate.isBefore(today)) {
            return
        }

        val reminderDate = dueDate.minusDays(daysBefore.toLong())
        if (reminderDate.isBefore(today)) {
            // Target reminder date already passed
            return
        }

        val now = LocalDateTime.now()
        val standardReminderTime = LocalTime.of(9, 0)
        var reminderDateTime = LocalDateTime.of(reminderDate, standardReminderTime)

        // If reminder is scheduled for today (e.g. day of due date) and 9:00 AM has already passed today:
        if (reminderDate == today && reminderDateTime.isBefore(now)) {
            // Bill is due today and uncompleted: schedule immediate reminder (in 5 seconds)
            reminderDateTime = now.plusSeconds(5)
        } else if (reminderDateTime.isBefore(now)) {
            return
        }

        val triggerMillis = reminderDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_BILL_REMINDER
            putExtra(EXTRA_BILL_ID, requestCode)
            putExtra(EXTRA_ORIGINAL_ID, id)
            putExtra(EXTRA_IS_TX, isTx)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_AMOUNT, amount)
            putExtra(EXTRA_DUE_DATE, dueDateStr)
            putExtra(EXTRA_DAYS_BEFORE, daysBefore)
            putExtra(EXTRA_IS_AUTOPAY, isAutoPay)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            createNotificationChannel(context)

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            var scheduledExact = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                    scheduledExact = true
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                scheduledExact = true
            }

            if (!scheduledExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            }
            Log.d("BillReminderScheduler", "Scheduled expense reminder for '$title' at $reminderDateTime (Exact: $scheduledExact, RequestCode: $requestCode)")
        } catch (e: SecurityException) {
            Log.e("BillReminderScheduler", "SecurityException scheduling exact alarm, using fallback setAndAllowWhileIdle()", e)
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } catch (e2: Exception) {
                Log.e("BillReminderScheduler", "Fallback alarm scheduling failed", e2)
            }
        } catch (e: Exception) {
            Log.e("BillReminderScheduler", "Failed to schedule alarm for expense $title", e)
        }
    }

    fun cancelReminder(context: Context, billId: Int) {
        cancelReminderByRequestCode(context, billId)
    }

    fun cancelTransactionReminder(context: Context, txId: Int) {
        cancelReminderByRequestCode(context, txId + 100000)
    }

    private fun cancelReminderByRequestCode(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BillReminderReceiver::class.java).apply {
            action = ACTION_BILL_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("BillReminderScheduler", "Cancelled reminder for request code $requestCode")
        }
        // Also cancel any posted notification from the system status bar
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(requestCode)
        } catch (e: Exception) {
            Log.e("BillReminderScheduler", "Error clearing notification tray for $requestCode", e)
        }
    }

    fun rescheduleAll(
        context: Context,
        bills: List<PlannedExpenseEntity>,
        transactions: List<TransactionEntity> = emptyList()
    ) {
        val today = LocalDate.now()
        // 1. Process planned bills: only schedule if NOT completed by matching expense transaction
        for (bill in bills) {
            if (bill.reminderDaysBefore >= 0) {
                val dueDate = try {
                    LocalDate.parse(bill.dueDate)
                } catch (_: Exception) {
                    val day = bill.dueDate.toIntOrNull()
                    if (day != null && day in 1..31) {
                        val targetDay = minOf(day, today.lengthOfMonth())
                        val dateThisMonth = today.withDayOfMonth(targetDay)
                        if (!dateThisMonth.isBefore(today)) dateThisMonth else today.plusMonths(1).let { it.withDayOfMonth(minOf(day, it.lengthOfMonth())) }
                    } else {
                        null
                    }
                }
                if (dueDate != null && !dueDate.isBefore(today)) {
                    val hasCompletedInTransactions = transactions.any { tx ->
                        !tx.isIncome && tx.isCompleted &&
                                (tx.title.equals(bill.title, ignoreCase = true) || tx.title.contains(bill.title, ignoreCase = true))
                    }
                    if (hasCompletedInTransactions) {
                        cancelReminder(context, bill.id)
                    } else {
                        scheduleGenericReminder(
                            context = context,
                            id = bill.id,
                            title = bill.title,
                            amount = bill.minAmount,
                            dueDateStr = dueDate.toString(),
                            daysBefore = bill.reminderDaysBefore,
                            isAutoPay = bill.isAutoPay,
                            isTx = false
                        )
                    }
                } else {
                    cancelReminder(context, bill.id)
                }
            } else {
                cancelReminder(context, bill.id)
            }
        }

        // 2. Process expense transactions: only schedule if active, uncompleted, and not in the past
        for (tx in transactions) {
            if (!tx.isIncome) {
                if (tx.isCompleted || tx.reminderDaysBefore < 0) {
                    cancelTransactionReminder(context, tx.id)
                } else {
                    val dueDate = DateUtils.millisToLocalDate(tx.dateMillis)
                    if (!dueDate.isBefore(today)) {
                        scheduleTransactionReminder(context, tx)
                    } else {
                        cancelTransactionReminder(context, tx.id)
                    }
                }
            }
        }

        // 3. Ensure periodic WorkManager backup is active
        try {
            NotificationReliabilityHelper.schedulePeriodicWork(context)
        } catch (e: Exception) {
            Log.e("BillReminderScheduler", "Could not enqueue periodic work backup", e)
        }
    }
}
