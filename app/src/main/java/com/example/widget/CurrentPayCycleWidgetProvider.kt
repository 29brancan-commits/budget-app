package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.example.MainActivity
import com.example.R
import com.example.data.BudgetDatabase
import com.example.data.DateUtils
import com.example.data.PayCycleCalculator
import com.example.data.PlannedExpenseEntity
import com.example.data.TransactionEntity
import com.example.data.UpcomingExpenseCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class CurrentPayCycleWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_CYCLE_WIDGET = "com.example.widget.ACTION_REFRESH_CYCLE_WIDGET"
        const val ACTION_PREV_ACCOUNT = "com.example.widget.ACTION_PREV_ACCOUNT"
        const val ACTION_NEXT_ACCOUNT = "com.example.widget.ACTION_NEXT_ACCOUNT"
        const val ACTION_WIDGET_ITEM_CLICK = "com.example.widget.ACTION_WIDGET_ITEM_CLICK"

        fun sendUpdateBroadcast(context: Context) {
            val intent = Intent(context, CurrentPayCycleWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_CYCLE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            PayCycleWidgetPrefs.clearWidgetAccount(context, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        when (intent.action) {
            ACTION_PREV_ACCOUNT -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    CoroutineScope(Dispatchers.IO).launch {
                        PayCycleWidgetPrefs.cycleAccount(context, appWidgetId, forward = false)
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
            ACTION_NEXT_ACCOUNT -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    CoroutineScope(Dispatchers.IO).launch {
                        PayCycleWidgetPrefs.cycleAccount(context, appWidgetId, forward = true)
                        updateAppWidget(context, appWidgetManager, appWidgetId)
                    }
                }
            }
            ACTION_REFRESH_CYCLE_WIDGET, AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val componentName = ComponentName(context, CurrentPayCycleWidgetProvider::class.java)
                val allIds = appWidgetManager.getAppWidgetIds(componentName).toMutableSet()
                val extraId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (extraId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    allIds.add(extraId)
                }
                val extraIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                if (extraIds != null) {
                    allIds.addAll(extraIds.toList())
                }
                for (appWidgetId in allIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
            }
            ACTION_WIDGET_ITEM_CLICK -> {
                val itemAction = intent.getStringExtra("extra_item_action") ?: "open_app"
                if (itemAction == "mark_paid") {
                    val txId = intent.getIntExtra("extra_tx_id", -1)
                    val title = intent.getStringExtra("extra_bill_title") ?: "Bill"
                    val amount = intent.getDoubleExtra("extra_bill_amount", 0.0)
                    val dueDateStr = intent.getStringExtra("extra_bill_due_date") ?: ""
                    val account = intent.getStringExtra("extra_bill_account") ?: "Primary Account"
                    val category = intent.getStringExtra("extra_bill_category") ?: "Planned Bill"
                    val isAutoPay = intent.getBooleanExtra("extra_bill_is_autopay", false)

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = BudgetDatabase.getDatabase(context)
                            val dao = db.transactionDao()
                            val allTxs = dao.getAllTransactions().first()
                            val existing = if (txId > 0) allTxs.firstOrNull { it.id == txId } else null

                            if (existing != null) {
                                dao.updateTransaction(existing.copy(status = "paid", isCompleted = true))
                            } else {
                                val dueDateMillis = try {
                                    DateUtils.localDateToMillis(LocalDate.parse(dueDateStr))
                                } catch (e: Exception) {
                                    System.currentTimeMillis()
                                }
                                val newTx = TransactionEntity(
                                    id = 0,
                                    title = title,
                                    amount = amount,
                                    dateMillis = dueDateMillis,
                                    category = category,
                                    accountName = account,
                                    isIncome = false,
                                    isCompleted = true,
                                    isAutoPay = isAutoPay,
                                    status = "paid"
                                )
                                dao.insertTransaction(newTx)
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "✓ '$title' marked as paid", Toast.LENGTH_SHORT).show()
                            }
                            sendUpdateBroadcast(context)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } else {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtras(intent)
                    }
                    context.startActivity(launchIntent)
                }
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo == null) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BudgetDatabase.getDatabase(context)
                val dao = db.transactionDao()

                val allTransactions: List<TransactionEntity> = dao.getAllTransactions().first()
                val plannedExpenses: List<PlannedExpenseEntity> = dao.getAllPlannedExpenses().first()
                val rawGlobal = dao.getSetting("active_account_name")?.value
                val globalDefaultAccount = if (rawGlobal.isNullOrBlank() || rawGlobal.equals("Primary Account", ignoreCase = true) || rawGlobal.equals("Primary", ignoreCase = true)) "All Accounts" else rawGlobal

                // Get widget-specific chosen account
                val chosenAccount = PayCycleWidgetPrefs.getSelectedAccount(context, appWidgetId, globalDefaultAccount)

                val savedAnchor = dao.getSetting("pay_cycle_start_date")?.value
                val anchor = try {
                    if (savedAnchor != null) LocalDate.parse(savedAnchor) else LocalDate.of(2026, 7, 30)
                } catch (e: Exception) {
                    LocalDate.of(2026, 7, 30)
                }

                val savedPaycheck = dao.getSetting("default_paycheck_amount")?.value
                val defaultPaycheck = savedPaycheck?.toDoubleOrNull() ?: 2500.0
                val today = LocalDate.now()

                val summary = PayCycleCalculator.calculateCurrentPayCycle(
                    allTransactions = allTransactions,
                    plannedExpenses = plannedExpenses,
                    activeAccount = chosenAccount,
                    anchor = anchor,
                    defaultPaycheck = defaultPaycheck,
                    today = today
                )

                // Calculate number of expenses due in this cycle
                val currentExpenses = UpcomingExpenseCalculator.getUpcomingExpenses(
                    allTransactions = allTransactions,
                    plannedExpenses = plannedExpenses,
                    activeAccountName = chosenAccount,
                    today = today,
                    payPeriodStart = summary.startDate,
                    payPeriodEnd = summary.endDate
                ).filter { item ->
                    val tx = item.transactionRef
                    if (tx != null && (tx.isCompleted || tx.isPaidOrCleared || tx.isPaid || tx.isCleared || tx.status.lowercase().trim() in listOf("paid", "cleared", "completed", "settled", "done"))) {
                        return@filter false
                    }
                    val isPaidInDb = allTransactions.any { existing ->
                        !existing.isIncome &&
                        !existing.isStartingBalanceOrAdjustment &&
                        (existing.isCompleted || existing.isPaidOrCleared || existing.isPaid || existing.isCleared || existing.status.lowercase().trim() in listOf("paid", "cleared", "completed", "settled", "done")) &&
                        (existing.title.equals(item.title, ignoreCase = true) ||
                         existing.title.equals(item.title.replace(" (Planned)", ""), ignoreCase = true) ||
                         existing.title.contains(item.title, ignoreCase = true) ||
                         item.title.contains(existing.title, ignoreCase = true)) &&
                        (DateUtils.millisToLocalDate(existing.dateMillis) == item.dueDate ||
                         Math.abs(ChronoUnit.DAYS.between(DateUtils.millisToLocalDate(existing.dateMillis), item.dueDate)) <= 15 ||
                         (!DateUtils.millisToLocalDate(existing.dateMillis).isBefore(summary.startDate) && !DateUtils.millisToLocalDate(existing.dateMillis).isAfter(summary.endDate)))
                    }
                    !isPaidInDb
                }

                val views = RemoteViews(context.packageName, R.layout.widget_current_pay_cycle)

                // Set pending intent to launch MainActivity on tapping the widget background/root
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingLaunch = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 1,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_pay_cycle_root, pendingLaunch)

                // Set pending intent for Refresh Button
                val refreshIntent = Intent(context, CurrentPayCycleWidgetProvider::class.java).apply {
                    action = ACTION_REFRESH_CYCLE_WIDGET
                }
                val pendingRefresh = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 2,
                    refreshIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_refresh_cycle, pendingRefresh)

                // Set pending intent for Previous Account Button (◀)
                val prevAccIntent = Intent(context, CurrentPayCycleWidgetProvider::class.java).apply {
                    action = ACTION_PREV_ACCOUNT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingPrevAcc = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 3,
                    prevAccIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_prev_account, pendingPrevAcc)

                // Set pending intent for Next Account Button (▶)
                val nextAccIntent = Intent(context, CurrentPayCycleWidgetProvider::class.java).apply {
                    action = ACTION_NEXT_ACCOUNT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val pendingNextAcc = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 4,
                    nextAccIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_next_account, pendingNextAcc)

                // Set pending intent for Account Badge / Picker Activity
                val pickAccIntent = Intent(context, WidgetAccountSelectionActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingPickAcc = PendingIntent.getActivity(
                    context,
                    appWidgetId * 10 + 5,
                    pickAccIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_account_badge_container, pendingPickAcc)

                val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)
                val rangeStr = "${summary.startDate.format(dateFmt)} – ${summary.endDate.format(dateFmt)}"
                views.setTextViewText(R.id.widget_pay_cycle_range, rangeStr)

                val daysBadge = "${summary.daysRemaining}d left"
                views.setTextViewText(R.id.widget_days_left_badge, daysBadge)

                views.setTextViewText(R.id.widget_remaining_label, "SELECTED ACCOUNT")
                views.setTextViewText(R.id.widget_remaining_balance, summary.accountName)
                views.setTextColor(R.id.widget_remaining_balance, Color.parseColor("#38BDF8")) // Sky Blue accent

                val spentPercentStr = "${summary.spentPercentage}% spent"
                views.setTextViewText(R.id.widget_spent_percent_text, spentPercentStr)
                views.setProgressBar(R.id.widget_budget_progress, 100, summary.spentPercentage, false)

                val clearedFormatted = String.format(Locale.US, "Cleared: $%,.2f", summary.clearedBalance)
                views.setTextViewText(R.id.widget_cleared_subtext, clearedFormatted)

                views.setTextViewText(R.id.widget_account_badge, summary.accountName)

                val countText = "${currentExpenses.size} due"
                views.setTextViewText(R.id.widget_expenses_section_count, countText)

                // Handle collections based on Android platform version
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val collectionBuilder = RemoteViews.RemoteCollectionItems.Builder()
                    currentExpenses.forEachIndexed { _, item ->
                        collectionBuilder.addItem(
                            item.id.hashCode().toLong(),
                            CurrentPayCycleWidgetService.buildExpenseItemRemoteViews(context, item, dateFmt)
                        )
                    }
                    views.setRemoteAdapter(R.id.widget_pay_cycle_expense_list, collectionBuilder.build())
                } else {
                    val serviceIntent = Intent(context, CurrentPayCycleWidgetService::class.java).apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        data = Uri.parse("widget://pay_cycle_widget_expenses/$appWidgetId/${System.currentTimeMillis()}")
                    }
                    views.setRemoteAdapter(R.id.widget_pay_cycle_expense_list, serviceIntent)
                }

                views.setEmptyView(R.id.widget_pay_cycle_expense_list, R.id.widget_pay_cycle_empty_expenses)
                views.setViewVisibility(
                    R.id.widget_pay_cycle_empty_expenses,
                    if (currentExpenses.isEmpty()) View.VISIBLE else View.GONE
                )
                views.setViewVisibility(
                    R.id.widget_pay_cycle_expense_list,
                    if (currentExpenses.isEmpty()) View.GONE else View.VISIBLE
                )

                // PendingIntent template for items in the expenses ListView
                val itemClickIntent = Intent(context, CurrentPayCycleWidgetProvider::class.java).apply {
                    action = ACTION_WIDGET_ITEM_CLICK
                }
                val pendingItemClick = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + 6,
                    itemClickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
                views.setPendingIntentTemplate(R.id.widget_pay_cycle_expense_list, pendingItemClick)

                // Update AppWidget synchronously and atomically
                appWidgetManager.updateAppWidget(appWidgetId, views)

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    try {
                        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_pay_cycle_expense_list)
                    } catch (e: Throwable) {
                        Log.w("PayCycleWidget", "Error notifying collection change: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("PayCycleWidget", "Error updating widget: ${e.message}", e)
            }
        }
    }
}
