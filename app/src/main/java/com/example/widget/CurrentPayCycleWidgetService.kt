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
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
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
        // Init
    }

    override fun onDataSetChanged() {
        runBlocking {
            try {
                val db = BudgetDatabase.getDatabase(context)
                val dao = db.transactionDao()
                val allTransactions: List<TransactionEntity> = dao.getAllTransactions().first()
                val plannedExpenses: List<PlannedExpenseEntity> = dao.getAllPlannedExpenses().first()
                
                val globalDefault = dao.getSetting("active_account_name")?.value ?: "Primary Account"
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
                )

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

        val expense = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_expense_item)

        views.setTextViewText(R.id.widget_item_title, expense.title)

        // AutoPay indicator
        views.setViewVisibility(
            R.id.widget_item_autopay,
            if (expense.isAutoPay) View.VISIBLE else View.GONE
        )

        // Account label
        val accText = if (expense.accountName.isNotBlank() && expense.accountName != "Primary Account") {
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
        val fillInIntent = Intent()
        views.setOnClickFillInIntent(R.id.widget_expense_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
