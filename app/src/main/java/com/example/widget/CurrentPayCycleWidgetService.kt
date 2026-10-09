package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.BudgetDatabase
import com.example.data.DateUtils
import com.example.data.PlannedExpenseEntity
import com.example.data.TransactionEntity
import com.example.data.UpcomingExpenseCalculator
import com.example.data.UpcomingExpenseItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

class CurrentPayCycleWidgetService : RemoteViewsService() {
    companion object {
        fun buildExpenseItemRemoteViews(
            context: Context,
            expense: UpcomingExpenseItem,
            dateFmt: DateTimeFormatter
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_expense_item)

            views.setTextViewText(R.id.widget_item_title, expense.title)

            // AutoPay indicator
            views.setViewVisibility(
                R.id.widget_item_autopay,
                if (expense.isAutoPay) View.VISIBLE else View.GONE
            )

            // Account label
            val accText = if (expense.accountName.isNotBlank() &&
                !expense.accountName.equals("Primary Account", ignoreCase = true) &&
                !expense.accountName.equals("Primary", ignoreCase = true)
            ) {
                "• ${expense.accountName}"
            } else {
                ""
            }
            views.setTextViewText(R.id.widget_item_account, accText)
            views.setViewVisibility(
                R.id.widget_item_account,
                if (accText.isNotEmpty()) View.VISIBLE else View.GONE
            )

            // Due date countdown & styling
            val today = LocalDate.now()
            val formattedDueDate = expense.dueDate.format(dateFmt)
            val daysBetween = ChronoUnit.DAYS.between(today, expense.dueDate)

            val (dateLabel, dateTextColor) = when {
                daysBetween < 0 -> Pair("Overdue ($formattedDueDate)", Color.parseColor("#EF4444"))
                daysBetween == 0L -> Pair("Due Today", Color.parseColor("#EF4444"))
                daysBetween == 1L -> Pair("Due Tomorrow", Color.parseColor("#F59E0B"))
                daysBetween in 2..7 -> Pair("Due in $daysBetween days ($formattedDueDate)", Color.parseColor("#9CA3AF"))
                else -> Pair("Due $formattedDueDate", Color.parseColor("#9CA3AF"))
            }

            views.setTextViewText(R.id.widget_item_date, dateLabel)
            views.setTextColor(R.id.widget_item_date, dateTextColor)

            views.setTextViewText(
                R.id.widget_item_amount,
                String.format(Locale.US, "-$%,.2f", expense.amount)
            )

            // Fill-in intent to open app on item click
            val fillInIntent = Intent().apply {
                putExtra("extra_item_action", "open_app")
                putExtra("extra_tx_id", expense.transactionRef?.id ?: -1)
                putExtra("extra_bill_title", expense.title)
            }
            views.setOnClickFillInIntent(R.id.widget_expense_item_root, fillInIntent)

            // Fill-in intent to directly mark bill as paid from checkmark button without opening app!
            val checkFillInIntent = Intent().apply {
                putExtra("extra_item_action", "mark_paid")
                putExtra("extra_tx_id", expense.transactionRef?.id ?: -1)
                putExtra("extra_bill_title", expense.title)
                putExtra("extra_bill_amount", expense.amount)
                putExtra("extra_bill_due_date", expense.dueDate.toString())
                putExtra("extra_bill_account", expense.accountName)
                putExtra("extra_bill_category", expense.category)
                putExtra("extra_bill_is_autopay", expense.isAutoPay)
            }
            views.setOnClickFillInIntent(R.id.widget_item_check_btn, checkFillInIntent)

            return views
        }
    }

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ).let { id ->
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) id
            else intent.data?.pathSegments?.firstOrNull()?.toIntOrNull()
                ?: intent.data?.lastPathSegment?.toIntOrNull()
                ?: AppWidgetManager.INVALID_APPWIDGET_ID
        }
        return CurrentPayCycleRemoteViewsFactory(this.applicationContext, appWidgetId)
    }
}

class CurrentPayCycleRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items: List<UpcomingExpenseItem> = emptyList()
    private val dateFmt = DateTimeFormatter.ofPattern("MMM d", Locale.US)

    override fun onCreate() {
        onDataSetChanged()
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = BudgetDatabase.getDatabase(context)
                val dao = db.transactionDao()
                val allTransactions: List<TransactionEntity> = dao.getAllTransactions().first()
                val plannedExpenses: List<PlannedExpenseEntity> = dao.getAllPlannedExpenses().first()
                
                val rawGlobal = dao.getSetting("active_account_name")?.value
                val globalDefault = if (rawGlobal.isNullOrBlank() || rawGlobal.equals("Primary Account", ignoreCase = true) || rawGlobal.equals("Primary", ignoreCase = true)) "All Accounts" else rawGlobal
                val chosenAccount = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    PayCycleWidgetPrefs.getSelectedAccount(context, appWidgetId, globalDefault)
                } else {
                    globalDefault
                }

                val savedAnchor = dao.getSetting("pay_cycle_start_date")?.value
                val anchor = try {
                    if (savedAnchor != null) LocalDate.parse(savedAnchor) else LocalDate.of(2026, 7, 30)
                } catch (e: Exception) {
                    LocalDate.of(2026, 7, 30)
                }

                val today = LocalDate.now()
                val currentCycleStart = DateUtils.getPaydayOnOrBefore(today, anchor)
                val currentCycleEnd = currentCycleStart.plusDays(13)

                val currentExpenses = UpcomingExpenseCalculator.getUpcomingExpenses(
                    allTransactions = allTransactions,
                    plannedExpenses = plannedExpenses,
                    activeAccountName = chosenAccount,
                    today = today,
                    payPeriodStart = currentCycleStart,
                    payPeriodEnd = currentCycleEnd
                ).filter { item ->
                    // Rigorous check: Never show anything that has been marked paid or cleared
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
                         (!DateUtils.millisToLocalDate(existing.dateMillis).isBefore(currentCycleStart) && !DateUtils.millisToLocalDate(existing.dateMillis).isAfter(currentCycleEnd)))
                    }
                    !isPaidInDb
                }

                items = currentExpenses
            } catch (e: Exception) {
                e.printStackTrace()
                items = emptyList()
            }
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null
        return CurrentPayCycleWidgetService.buildExpenseItemRemoteViews(context, items[position], dateFmt)
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        if (position in items.indices) {
            return items[position].id.hashCode().toLong()
        }
        return position.toLong()
    }

    override fun hasStableIds(): Boolean = false
}
