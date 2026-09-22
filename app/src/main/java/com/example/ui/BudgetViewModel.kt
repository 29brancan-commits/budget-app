package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.sync.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppIcon
import com.example.CategoryTheme

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val database = BudgetDatabase.getDatabase(application)
    val repository = BudgetRepository(database.transactionDao())

    // UI state parameters
    private val _selectedDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedMonth = MutableStateFlow<YearMonth>(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    private val _anchorPayday = MutableStateFlow<LocalDate>(LocalDate.of(2026, 7, 30))
    val anchorPayday: StateFlow<LocalDate> = _anchorPayday.asStateFlow()

    private val _defaultPaycheckAmount = MutableStateFlow<Double>(0.0)
    val defaultPaycheckAmount: StateFlow<Double> = _defaultPaycheckAmount.asStateFlow()

    // New Account States
    private val _activeAccountName = MutableStateFlow<String>("All Accounts")
    val activeAccountName: StateFlow<String> = _activeAccountName.asStateFlow()

    private val _allAccounts = MutableStateFlow<List<String>>(emptyList())
    val allAccounts: StateFlow<List<String>> = _allAccounts.asStateFlow()

    private val _isDarkMode = MutableStateFlow<Boolean>(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _appTheme = MutableStateFlow<AppTheme>(AppTheme.EREADER)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _appIcon = MutableStateFlow<AppIcon>(AppIcon.getCurrentIcon(application))
    val appIcon: StateFlow<AppIcon> = _appIcon.asStateFlow()

    // Google Drive Sync Manager & States
    val driveSyncManager = com.example.sync.GoogleDriveSyncManager(application)

    private val _syncState = MutableStateFlow<com.example.sync.SyncState>(com.example.sync.SyncState.Idle)
    val syncState: StateFlow<com.example.sync.SyncState> = _syncState.asStateFlow()

    private val _driveAccountEmail = MutableStateFlow<String?>(driveSyncManager.getSignedInAccount()?.email ?: "86brancan@gmail.com")
    val driveAccountEmail: StateFlow<String?> = _driveAccountEmail.asStateFlow()

    private val _lastDriveSyncTime = MutableStateFlow<String?>(null)
    val lastDriveSyncTime: StateFlow<String?> = _lastDriveSyncTime.asStateFlow()

    private val _isDriveAutoSyncEnabled = MutableStateFlow<Boolean>(false)
    val isDriveAutoSyncEnabled: StateFlow<Boolean> = _isDriveAutoSyncEnabled.asStateFlow()

    // Undo Stack & Event
    private val _undoStack = MutableStateFlow<List<UndoActionItem>>(emptyList())
    val canUndo: StateFlow<Boolean> = _undoStack.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val lastUndoAction: StateFlow<UndoActionItem?> = _undoStack.map { it.lastOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _undoMessageEvent = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val undoMessageEvent: SharedFlow<String> = _undoMessageEvent.asSharedFlow()

    fun pushUndoAction(title: String, description: String, onUndo: suspend () -> Unit) {
        val item = UndoActionItem(
            title = title,
            description = description,
            onUndo = onUndo
        )
        val updated = (_undoStack.value + item).takeLast(30)
        _undoStack.value = updated
        _undoMessageEvent.tryEmit(description)
    }

    fun undo() {
        viewModelScope.launch {
            val currentStack = _undoStack.value
            if (currentStack.isNotEmpty()) {
                val lastAction = currentStack.last()
                _undoStack.value = currentStack.dropLast(1)
                try {
                    lastAction.onUndo()
                    _undoMessageEvent.emit("Undone: ${lastAction.title}")
                    autoSyncIfEnabled()
                    com.example.widget.CurrentPayCycleWidgetProvider.sendUpdateBroadcast(getApplication())
                } catch (e: Exception) {
                    Log.e("BudgetViewModel", "Error executing undo", e)
                }
            }
        }
    }

    private val settingsFlow = combine(
        _selectedDate,
        _anchorPayday,
        _defaultPaycheckAmount,
        _activeAccountName,
        _allAccounts
    ) { selectedDate, anchor, defaultAmount, activeAccount, allAccounts ->
        SettingsBundle(selectedDate, anchor, defaultAmount, activeAccount, allAccounts)
    }

    // Combined UI state
    val uiState: StateFlow<BudgetUiState> = combine(
        settingsFlow,
        repository.allTransactions,
        repository.allIncomeSources,
        repository.allPlannedExpenses,
        repository.allDebts,
        repository.allDebtPayments,
        _isDarkMode,
        _appTheme,
        _appIcon,
        _syncState,
        _driveAccountEmail,
        _lastDriveSyncTime,
        _isDriveAutoSyncEnabled,
        _undoStack
    ) { args: Array<Any?> ->
        val settings = args[0] as SettingsBundle
        @Suppress("UNCHECKED_CAST")
        val transactions = args[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val incomeSources = args[2] as List<IncomeSourceEntity>
        @Suppress("UNCHECKED_CAST")
        val plannedExpenses = args[3] as List<PlannedExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val debts = args[4] as List<DebtEntity>
        @Suppress("UNCHECKED_CAST")
        val debtPayments = args[5] as List<DebtPaymentEntity>
        val isDark = args[6] as Boolean
        val themeSelected = args[7] as AppTheme
        val iconSelected = args[8] as AppIcon
        val syncSt = args[9] as com.example.sync.SyncState
        val driveEmail = args[10] as String?
        val lastSync = args[11] as String?
        val autoSyncOn = args[12] as Boolean
        @Suppress("UNCHECKED_CAST")
        val undoStack = args[13] as List<UndoActionItem>
        val selectedDate = settings.selectedDate
        val anchor = settings.anchor
        val defaultAmount = settings.defaultAmount
        val activeAccount = settings.activeAccount
        val rawSettingsAccounts = settings.allAccounts

        val txAccountNames = transactions.map { it.accountName.trim() }
            .filter { it.isNotBlank() && !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) }

        val cleanRawAccounts = rawSettingsAccounts
            .filter { it.isNotBlank() && !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) }

        val baseAccounts = if (cleanRawAccounts.isEmpty()) {
            listOf("All Accounts")
        } else {
            if (!cleanRawAccounts.contains("All Accounts")) {
                listOf("All Accounts") + cleanRawAccounts
            } else {
                cleanRawAccounts
            }
        }

        val missingTxAccounts = txAccountNames.filter { !baseAccounts.contains(it) }
        val displayAccounts = (baseAccounts + missingTxAccounts).distinct()

        val referenceDate = selectedDate
        val currentPayday = DateUtils.getPaydayOnOrBefore(referenceDate, anchor)
        val currentPayPeriodStart = currentPayday
        val currentPayPeriodEnd = currentPayday.plusDays(13)
        val month = YearMonth.from(currentPayday)

        // Calculate dynamic paycheck amount based on specified income sources
        val computedPaycheckAmount = if (incomeSources.isNotEmpty()) {
            incomeSources.sumOf { source ->
                when (source.frequency.lowercase(java.util.Locale.US)) {
                    "weekly" -> source.amount * 2.0
                    "bi-weekly", "biweekly" -> source.amount
                    "monthly" -> (source.amount * 12.0) / 26.0
                    else -> source.amount
                }
            }
        } else {
            defaultAmount
        }

        // Dynamically project recurring transactions to every month after the initial input date (income = bi-weekly +14d, expense = monthly)
        val futureHorizonMonth = maxOf(month.plusMonths(6), YearMonth.now().plusMonths(6))
        val futureHorizonDate = futureHorizonMonth.atEndOfMonth()
        val horizonStartDate = minOf(anchor.minusMonths(3), month.minusMonths(3).atDay(1))

        val expandedTransactions = mutableListOf<TransactionEntity>()

        // 1. Process explicit database transactions and expand recurring items
        transactions.forEach { tx ->
            expandedTransactions.add(tx)

            if (tx.isRecurring) {
                val origDate = DateUtils.millisToLocalDate(tx.dateMillis)
                val catAndTitle = (tx.category + " " + tx.title).lowercase(java.util.Locale.US)
                val isWeekly = catAndTitle.contains("weekly") && !catAndTitle.contains("bi-weekly") && !catAndTitle.contains("biweekly")
                val isMonthly = !isWeekly && (!tx.isIncome || catAndTitle.contains("monthly")) && !catAndTitle.contains("semi-monthly") && !catAndTitle.contains("semimonthly")
                val isSemiMonthly = catAndTitle.contains("semi-monthly") || catAndTitle.contains("semimonthly")

                if (tx.isIncome) {
                    val projectedDates = mutableListOf<LocalDate>()
                    when {
                        isWeekly -> {
                            var d = origDate.plusDays(7)
                            while (!d.isAfter(futureHorizonDate)) {
                                projectedDates.add(d)
                                d = d.plusDays(7)
                            }
                        }
                        isSemiMonthly -> {
                            var m = YearMonth.from(origDate)
                            while (!m.isAfter(futureHorizonMonth)) {
                                val d1 = m.atDay(1)
                                val d15 = m.atDay(15)
                                if (d1.isAfter(origDate)) projectedDates.add(d1)
                                if (d15.isAfter(origDate)) projectedDates.add(d15)
                                m = m.plusMonths(1)
                            }
                        }
                        isMonthly -> {
                            var m = YearMonth.from(origDate).plusMonths(1)
                            while (!m.isAfter(futureHorizonMonth)) {
                                val day = minOf(origDate.dayOfMonth, m.lengthOfMonth())
                                projectedDates.add(m.atDay(day))
                                m = m.plusMonths(1)
                            }
                        }
                        else -> { // Bi-Weekly default (+14 days)
                            var d = origDate.plusDays(14)
                            while (!d.isAfter(futureHorizonDate)) {
                                projectedDates.add(d)
                                d = d.plusDays(14)
                            }
                        }
                    }

                    for (nextDate in projectedDates) {
                        val nextMillis = DateUtils.localDateToMillis(nextDate)
                        val alreadyExists = transactions.any { existing ->
                            (existing.title.equals(tx.title, ignoreCase = true) ||
                             existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                             existing.title.contains("Cancelled " + tx.title, ignoreCase = true) ||
                             (existing.id > 0 && existing.id == tx.id)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == nextDate
                        }
                        val alreadyInList = expandedTransactions.any { existing ->
                            existing.title.equals(tx.title, ignoreCase = true) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == nextDate
                        }
                        if (!alreadyExists && !alreadyInList) {
                            val synthId = -(Math.abs((tx.id.toString() + "_inc_" + nextDate.toString()).hashCode()) + 10000)
                            expandedTransactions.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = nextMillis,
                                    isCompleted = false
                                )
                            )
                        }
                    }
                } else {
                    // Recurring Expense: monthly projection
                    val origMonth = YearMonth.from(origDate)
                    var targetMonth = origMonth.plusMonths(1)
                    while (!targetMonth.isAfter(futureHorizonMonth)) {
                        val projectedDay = minOf(origDate.dayOfMonth, targetMonth.lengthOfMonth())
                        val projectedDate = LocalDate.of(targetMonth.year, targetMonth.month, projectedDay)
                        val projectedMillis = DateUtils.localDateToMillis(projectedDate)
                        val normTxTitle = DateUtils.normalizeDebtOrBillName(tx.title)
                        val alreadyExists = transactions.any { existing ->
                            (existing.title.equals(tx.title, ignoreCase = true) ||
                             DateUtils.normalizeDebtOrBillName(existing.title).equals(normTxTitle, ignoreCase = true) ||
                             existing.title.equals("Cancelled " + tx.title, ignoreCase = true) ||
                             existing.title.equals("Cancelled " + tx.title.replace(" (Planned)", ""), ignoreCase = true) ||
                             existing.title.contains("Cancelled " + tx.title, ignoreCase = true) ||
                             (existing.id > 0 && existing.id == tx.id)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == projectedDate
                        }
                        val alreadyInList = expandedTransactions.any { existing ->
                            (existing.title.equals(tx.title, ignoreCase = true) ||
                             DateUtils.normalizeDebtOrBillName(existing.title).equals(normTxTitle, ignoreCase = true)) &&
                                    DateUtils.millisToLocalDate(existing.dateMillis) == projectedDate
                        }
                        if (!alreadyExists && !alreadyInList) {
                            val synthId = -(Math.abs((tx.id.toString() + "_exp_" + projectedDate.toString()).hashCode()) + 10000)
                            expandedTransactions.add(
                                tx.copy(
                                    id = synthId,
                                    dateMillis = projectedMillis,
                                    isCompleted = false
                                )
                            )
                        }
                        targetMonth = targetMonth.plusMonths(1)
                    }
                }
            }
        }

        // 2. Project defined Recurring Income Sources (incomeSources) into transactions
        if (incomeSources.isNotEmpty()) {
            incomeSources.forEach { src ->
                val freq = src.frequency.lowercase(java.util.Locale.US).trim()
                val dates = mutableListOf<LocalDate>()
                when {
                    freq == "weekly" -> {
                        var d = anchor
                        while (d.isAfter(horizonStartDate)) d = d.minusDays(7)
                        while (!d.isAfter(futureHorizonDate)) {
                            if (!d.isBefore(horizonStartDate)) dates.add(d)
                            d = d.plusDays(7)
                        }
                    }
                    freq == "monthly" -> {
                        var m = YearMonth.from(horizonStartDate)
                        val endM = YearMonth.from(futureHorizonDate)
                        val day = minOf(anchor.dayOfMonth, 28)
                        while (!m.isAfter(endM)) {
                            dates.add(m.atDay(minOf(day, m.lengthOfMonth())))
                            m = m.plusMonths(1)
                        }
                    }
                    freq in listOf("semi-monthly", "semimonthly") -> {
                        var m = YearMonth.from(horizonStartDate)
                        val endM = YearMonth.from(futureHorizonDate)
                        while (!m.isAfter(endM)) {
                            dates.add(m.atDay(1))
                            dates.add(m.atDay(15))
                            m = m.plusMonths(1)
                        }
                    }
                    else -> { // bi-weekly (default)
                        var d = anchor
                        while (d.isAfter(horizonStartDate)) d = d.minusDays(14)
                        while (!d.isAfter(futureHorizonDate)) {
                            if (!d.isBefore(horizonStartDate)) dates.add(d)
                            d = d.plusDays(14)
                        }
                    }
                }

                for (d in dates) {
                    val alreadyInDb = transactions.any { existing ->
                        (existing.title.equals(src.name, ignoreCase = true) ||
                         existing.title.equals("Cancelled " + src.name, ignoreCase = true) ||
                         existing.title.contains("Cancelled " + src.name, ignoreCase = true) ||
                         (existing.isIncome && existing.category == "Paycheck" && (src.name.contains("Paycheck", ignoreCase = true) || src.name.contains("Salary", ignoreCase = true)))) &&
                                DateUtils.millisToLocalDate(existing.dateMillis) == d
                    }
                    val alreadyInList = expandedTransactions.any { existing ->
                        existing.title.equals(src.name, ignoreCase = true) &&
                                DateUtils.millisToLocalDate(existing.dateMillis) == d
                    }
                    if (!alreadyInDb && !alreadyInList) {
                        val synthId = -(Math.abs(("inc_src_" + src.id + "_" + src.name + "_" + d.toString()).hashCode()) + 20000)
                        expandedTransactions.add(
                            TransactionEntity(
                                id = synthId,
                                title = src.name,
                                amount = src.amount,
                                dateMillis = DateUtils.localDateToMillis(d),
                                category = "Paycheck",
                                isIncome = true,
                                isRecurring = true,
                                accountName = displayAccounts.firstOrNull { it != "All Accounts" } ?: "",
                                isCompleted = false,
                                isAutoPay = true
                            )
                        )
                    }
                }
            }
        } else {
            // 3. Fallback: Default recurring paycheck projected every 14 days on paydays
            val primaryAccountForProjection = displayAccounts.firstOrNull { it != "All Accounts" } ?: ""
            var cPayday = anchor
            while (cPayday.isAfter(horizonStartDate)) cPayday = cPayday.minusDays(14)
            while (!cPayday.isAfter(futureHorizonDate)) {
                if (!cPayday.isBefore(horizonStartDate)) {
                    val alreadyInDb = transactions.any { existing ->
                        (existing.isIncome && (existing.category == "Paycheck" || existing.title.contains("Paycheck", ignoreCase = true) || existing.title.contains("Salary", ignoreCase = true)) ||
                         existing.title.equals("Cancelled Paycheck", ignoreCase = true)) &&
                                DateUtils.millisToLocalDate(existing.dateMillis) == cPayday
                    }
                    val alreadyInList = expandedTransactions.any { existing ->
                        existing.isIncome && DateUtils.millisToLocalDate(existing.dateMillis) == cPayday
                    }
                    if (!alreadyInDb && !alreadyInList) {
                        val synthId = -(Math.abs(("def_paycheck_" + cPayday.toString()).hashCode()) + 30000)
                        expandedTransactions.add(
                            TransactionEntity(
                                id = synthId,
                                title = "Paycheck",
                                amount = defaultAmount,
                                dateMillis = DateUtils.localDateToMillis(cPayday),
                                category = "Paycheck",
                                isIncome = true,
                                isRecurring = true,
                                accountName = primaryAccountForProjection,
                                isCompleted = false,
                                isAutoPay = true
                            )
                        )
                    }
                }
                cPayday = cPayday.plusDays(14)
            }
        }

        val today = LocalDate.now()

        val currentPayWeekStart = if (referenceDate.isBefore(currentPayday.plusDays(7))) {
            currentPayday
        } else {
            currentPayday.plusDays(7)
        }
        val currentPayWeekEnd = currentPayWeekStart.plusDays(6)

        val payWeekStartMillis = DateUtils.localDateToMillis(currentPayWeekStart)
        val payWeekEndMillis = DateUtils.localDateToMillis(currentPayWeekEnd.plusDays(1))

        val payPeriodStartMillis = DateUtils.localDateToMillis(currentPayPeriodStart)
        val payPeriodEndMillis = DateUtils.localDateToMillis(currentPayPeriodEnd.plusDays(1))

        // Calculate paydays falling inside this calendar month
        val monthStartLocalDate = month.atDay(1)
        val monthEndLocalDate = month.atEndOfMonth()

        val paydaysInMonth = mutableListOf<LocalDate>()
        var d = DateUtils.getPaydayOnOrBefore(monthStartLocalDate, anchor)
        while (!d.isAfter(monthEndLocalDate)) {
            if (!d.isBefore(monthStartLocalDate)) {
                paydaysInMonth.add(d)
            }
            d = d.plusDays(14)
        }

        val primaryAccountName = displayAccounts.firstOrNull { it != "All Accounts" } ?: ""

        // Calculate summaries for ALL accounts so we can render them as cards
        val accountBalancesMap = displayAccounts.filter { it != "All Accounts" }.associateWith { acc ->
            val isPrimary = (primaryAccountName.isNotEmpty() && acc.equals(primaryAccountName, ignoreCase = true)) || (primaryAccountName.isEmpty() && (acc == "Primary Account" || acc.equals("Primary", ignoreCase = true)))
            val accTransactions = expandedTransactions.filter {
                it.accountName.equals(acc, ignoreCase = true) ||
                (isPrimary && (it.accountName.isBlank() || it.accountName.equals("Primary Account", ignoreCase = true) || it.accountName.equals("Primary", ignoreCase = true)))
            }
            
            // Current Pay Week paycheck (Primary Account gets default paycheck on payday in week 1)
            val isPaydayInWeek = !currentPayday.isBefore(currentPayWeekStart) && !currentPayday.isAfter(currentPayWeekEnd)
            val payWeekCustomPaycheck = if (isPrimary && isPaydayInWeek) {
                val pStart = DateUtils.localDateToMillis(currentPayday)
                val pEnd = DateUtils.localDateToMillis(currentPayday.plusDays(1))
                accTransactions.firstOrNull {
                    it.isIncome && it.category == "Paycheck" && it.dateMillis >= pStart && it.dateMillis < pEnd
                }
            } else null

            val payWeekPaycheck = if (isPrimary && isPaydayInWeek) {
                payWeekCustomPaycheck?.amount ?: computedPaycheckAmount
            } else 0.0

            val payWeekOtherIncome = accTransactions.filter {
                it.isIncome && it.dateMillis >= payWeekStartMillis && it.dateMillis < payWeekEndMillis && it.id != payWeekCustomPaycheck?.id
            }.sumOf { it.amount }

            val weekInc = payWeekPaycheck + payWeekOtherIncome
            val weekExp = accTransactions.filter {
                !it.isIncome && it.dateMillis >= payWeekStartMillis && it.dateMillis < payWeekEndMillis
            }.sumOf { it.amount }

            // Current Pay Period (2 weeks)
            val payPeriodPaychecks = if (isPrimary) {
                accTransactions.filter {
                    it.isIncome && it.category == "Paycheck" && it.dateMillis >= payPeriodStartMillis && it.dateMillis < payPeriodEndMillis
                }
            } else emptyList()

            val payPeriodPaycheck = if (isPrimary) {
                if (payPeriodPaychecks.isNotEmpty()) payPeriodPaychecks.sumOf { it.amount } else computedPaycheckAmount
            } else 0.0

            val usedPayPeriodPaycheckIds = payPeriodPaychecks.map { it.id }.toSet()

            val payPeriodOtherInc = accTransactions.filter {
                it.isIncome && it.dateMillis >= payPeriodStartMillis && it.dateMillis < payPeriodEndMillis && !usedPayPeriodPaycheckIds.contains(it.id)
            }.sumOf { it.amount }

            val periodInc = payPeriodPaycheck + payPeriodOtherInc
            val periodExp = accTransactions.filter {
                !it.isIncome && it.dateMillis >= payPeriodStartMillis && it.dateMillis < payPeriodEndMillis
            }.sumOf { it.amount }

            // Calculate carryover for current pay period from prior pay periods
            val earliestPriorTxMillis = accTransactions.filter { it.dateMillis < DateUtils.localDateToMillis(currentPayday) }.minOfOrNull { it.dateMillis }
            val calcStartPayday = if (earliestPriorTxMillis != null) {
                DateUtils.getPaydayOnOrBefore(DateUtils.millisToLocalDate(earliestPriorTxMillis), anchor)
            } else {
                currentPayday
            }
            val monthStartPayday = DateUtils.getPaydayOnOrBefore(month.atDay(1), anchor)
            val accStartPayday = if (monthStartPayday.isBefore(calcStartPayday)) monthStartPayday else calcStartPayday

            var accCarryover = 0.0
            var cPayday = accStartPayday
            while (cPayday.isBefore(currentPayday)) {
                val cPStartMillis = DateUtils.localDateToMillis(cPayday)
                val cPEndMillis = DateUtils.localDateToMillis(cPayday.plusDays(14))

                val cPaychecks = if (isPrimary) {
                    accTransactions.filter {
                        it.isIncome && it.category == "Paycheck" && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
                    }
                } else emptyList()

                val cPaycheck = if (isPrimary) {
                    if (cPaychecks.isNotEmpty()) cPaychecks.sumOf { it.amount } else computedPaycheckAmount
                } else 0.0

                val cUsedIds = cPaychecks.map { it.id }.toSet()

                val cOtherInc = accTransactions.filter {
                    it.isIncome && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis && !cUsedIds.contains(it.id)
                }.sumOf { it.amount }

                val cExp = accTransactions.filter {
                    !it.isIncome && it.dateMillis >= cPStartMillis && it.dateMillis < cPEndMillis
                }.sumOf { it.amount }

                accCarryover += (cPaycheck + cOtherInc - cExp)
                cPayday = cPayday.plusDays(14)
            }

            val currentPeriodRemaining = accCarryover + periodInc - periodExp

            // Monthly
            val usedMonthlyPaycheckIds = mutableSetOf<Int>()
            val paycheckIncome = if (isPrimary) {
                paydaysInMonth.sumOf { payday ->
                    val pS = DateUtils.localDateToMillis(payday)
                    val pE = DateUtils.localDateToMillis(payday.plusDays(1))
                    val customPaycheck = accTransactions.firstOrNull {
                        it.isIncome && it.category == "Paycheck" && it.dateMillis >= pS && it.dateMillis < pE
                    }
                    if (customPaycheck != null) {
                        usedMonthlyPaycheckIds.add(customPaycheck.id)
                    }
                    customPaycheck?.amount ?: computedPaycheckAmount
                }
            } else 0.0

            val monthOtherIncome = accTransactions.filter { tx ->
                val date = DateUtils.millisToLocalDate(tx.dateMillis)
                tx.isIncome && YearMonth.from(date) == month && !usedMonthlyPaycheckIds.contains(tx.id)
            }.sumOf { it.amount }

            val monthTotalInc = paycheckIncome + monthOtherIncome
            val monthTotalExp = accTransactions.filter { tx ->
                val date = DateUtils.millisToLocalDate(tx.dateMillis)
                !tx.isIncome && YearMonth.from(date) == month
            }.sumOf { it.amount }

            val periodCompletedExp = accTransactions.filter {
                !it.isIncome && it.isCompleted && it.dateMillis >= payPeriodStartMillis && it.dateMillis < payPeriodEndMillis
            }.sumOf { it.amount }

            val periodPendingExp = accTransactions.filter {
                !it.isIncome && !it.isCompleted && it.dateMillis >= payPeriodStartMillis && it.dateMillis < payPeriodEndMillis
            }.sumOf { it.amount }

            val currentClearedBalance = accCarryover + periodInc - periodCompletedExp

            AccountBalance(
                accountName = acc,
                totalIncome = periodInc,
                totalExpenses = periodExp,
                remainingBalance = currentPeriodRemaining,
                carryoverFromPrevious = accCarryover,
                payPeriodIncome = periodInc,
                payPeriodExpenses = periodExp,
                payPeriodRemaining = currentPeriodRemaining,
                monthlyTotalIncome = monthTotalInc,
                monthlyTotalExpenses = monthTotalExp,
                monthlyRemainingBalance = monthTotalInc - monthTotalExp,
                completedExpenses = periodCompletedExp,
                pendingExpenses = periodPendingExp,
                clearedBalance = currentClearedBalance
            )
        }

        val combinedBalance = AccountBalance(
            accountName = "All Accounts",
            totalIncome = accountBalancesMap.values.sumOf { it.totalIncome },
            totalExpenses = accountBalancesMap.values.sumOf { it.totalExpenses },
            remainingBalance = accountBalancesMap.values.sumOf { it.remainingBalance },
            carryoverFromPrevious = accountBalancesMap.values.sumOf { it.carryoverFromPrevious },
            payPeriodIncome = accountBalancesMap.values.sumOf { it.payPeriodIncome },
            payPeriodExpenses = accountBalancesMap.values.sumOf { it.payPeriodExpenses },
            payPeriodRemaining = accountBalancesMap.values.sumOf { it.payPeriodRemaining },
            monthlyTotalIncome = accountBalancesMap.values.sumOf { it.monthlyTotalIncome },
            monthlyTotalExpenses = accountBalancesMap.values.sumOf { it.monthlyTotalExpenses },
            monthlyRemainingBalance = accountBalancesMap.values.sumOf { it.monthlyRemainingBalance },
            completedExpenses = accountBalancesMap.values.sumOf { it.completedExpenses },
            pendingExpenses = accountBalancesMap.values.sumOf { it.pendingExpenses },
            clearedBalance = accountBalancesMap.values.sumOf { it.clearedBalance }
        )

        val accountBalances = mapOf("All Accounts" to combinedBalance) + accountBalancesMap

        // Filter transactions for the ACTIVE account
        val activeAccountTransactions = if (activeAccount == "All Accounts") {
            expandedTransactions
        } else {
            val isTargetPrimary = (primaryAccountName.isNotEmpty() && activeAccount.equals(primaryAccountName, ignoreCase = true)) || (primaryAccountName.isEmpty() && (activeAccount == "Primary Account" || activeAccount.equals("Primary", ignoreCase = true)))
            expandedTransactions.filter {
                it.accountName.equals(activeAccount, ignoreCase = true) ||
                (isTargetPrimary && (it.accountName.isBlank() || it.accountName.equals("Primary Account", ignoreCase = true) || it.accountName.equals("Primary", ignoreCase = true)))
            }
        }

        // Calc Pay Periods with carryover for the active account
        val earliestPriorTxMillis = activeAccountTransactions.filter { it.dateMillis < DateUtils.localDateToMillis(currentPayday) }.minOfOrNull { it.dateMillis }
        val calcStartPayday = if (earliestPriorTxMillis != null) {
            DateUtils.getPaydayOnOrBefore(DateUtils.millisToLocalDate(earliestPriorTxMillis), anchor)
        } else {
            currentPayday
        }
        val monthStartPayday = DateUtils.getPaydayOnOrBefore(month.atDay(1), anchor)
        val startPayday = if (monthStartPayday.isBefore(calcStartPayday)) monthStartPayday else calcStartPayday

        var pPayday = startPayday
        var runningCarryover = 0.0
        val allCalculatedPeriods = mutableListOf<PayPeriod>()

        // Project forward at least 3 full months beyond the selected month so financial intelligence has horizon data
        val targetHorizonMonth = maxOf(month.plusMonths(3), YearMonth.now().plusMonths(3))

        while (!pPayday.isAfter(targetHorizonMonth.atEndOfMonth())) {
            val pEnd = pPayday.plusDays(13)
            val pStartMillis = DateUtils.localDateToMillis(pPayday)
            val pEndMillis = DateUtils.localDateToMillis(pPayday.plusDays(14))

            val periodTransactions = activeAccountTransactions.filter {
                it.dateMillis >= pStartMillis && it.dateMillis < pEndMillis
            }

            val periodPaychecks = periodTransactions.filter {
                it.isIncome && it.category == "Paycheck"
            }

            val isPrimaryOrAll = activeAccount == "All Accounts" || (primaryAccountName.isNotEmpty() && activeAccount.equals(primaryAccountName, ignoreCase = true)) || activeAccount == "Primary Account" || activeAccount.equals("Primary", ignoreCase = true)

            val (paycheckVal, usedPaycheckIds) = if (periodPaychecks.isNotEmpty()) {
                periodPaychecks.sumOf { it.amount } to periodPaychecks.map { it.id }.toSet()
            } else if (isPrimaryOrAll) {
                computedPaycheckAmount to emptySet()
            } else {
                0.0 to emptySet()
            }

            val otherInc = periodTransactions.filter {
                it.isIncome && !usedPaycheckIds.contains(it.id)
            }.sumOf { it.amount }

            val expensesVal = periodTransactions.filter { !it.isIncome }.sumOf { it.amount }

            val carryover = runningCarryover
            val net = paycheckVal + otherInc - expensesVal
            val rem = carryover + net
            runningCarryover = rem

            allCalculatedPeriods.add(
                PayPeriod(
                    startDate = pPayday,
                    endDate = pEnd,
                    payday = pPayday,
                    transactions = periodTransactions,
                    paycheckAmount = paycheckVal,
                    otherIncome = otherInc,
                    totalExpenses = expensesVal,
                    carryoverFromPrevious = carryover
                )
            )

            pPayday = pPayday.plusDays(14)
        }

        val populatedPeriods = allCalculatedPeriods.filter { YearMonth.from(it.payday) == month }

        // Future pay periods spanning the next 3 months starting from the current pay cycle
        val futurePeriods = allCalculatedPeriods.filter { !it.endDate.isBefore(currentPayPeriodStart) }

        // Get the active account's summary details
        val activeSummary = accountBalances[activeAccount] ?: combinedBalance

        BudgetUiState(
            selectedMonth = month,
            anchorPayday = anchor,
            defaultPaycheckAmount = defaultAmount,
            payPeriods = populatedPeriods,
            futurePayPeriods = futurePeriods,
            allCalculatedPayPeriods = allCalculatedPeriods,
            monthlyTotalIncome = activeSummary.monthlyTotalIncome,
            monthlyTotalExpenses = activeSummary.monthlyTotalExpenses,
            monthlyRemainingBalance = activeSummary.monthlyRemainingBalance,
            incomeSources = incomeSources,
            plannedExpenses = plannedExpenses,
            allDebts = debts,
            allDebtPayments = debtPayments,
            activeAccountName = activeAccount,
            allAccounts = displayAccounts,
            accountBalances = accountBalances,
            allTransactions = expandedTransactions,
            isDarkMode = isDark,
            appTheme = themeSelected,
            appIcon = iconSelected,
            syncState = syncSt,
            driveAccountEmail = driveEmail,
            lastDriveSyncTime = lastSync,
            isDriveAutoSyncEnabled = autoSyncOn,
            currentPayWeekStart = currentPayWeekStart,
            currentPayWeekEnd = currentPayWeekEnd,
            currentPayPeriodStart = currentPayPeriodStart,
            currentPayPeriodEnd = currentPayPeriodEnd,
            canUndo = undoStack.isNotEmpty(),
            lastUndoDescription = undoStack.lastOrNull()?.description
        )
    }.flowOn(kotlinx.coroutines.Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetUiState(
            selectedMonth = YearMonth.now(),
            anchorPayday = LocalDate.of(2026, 7, 30),
            defaultPaycheckAmount = 2000.0,
            payPeriods = emptyList(),
            futurePayPeriods = emptyList(),
            allCalculatedPayPeriods = emptyList(),
            monthlyTotalIncome = 0.0,
            monthlyTotalExpenses = 0.0,
            monthlyRemainingBalance = 0.0,
            incomeSources = emptyList(),
            plannedExpenses = emptyList()
        )
    )

    private data class SettingsBundle(
        val selectedDate: LocalDate,
        val anchor: LocalDate,
        val defaultAmount: Double,
        val activeAccount: String,
        val allAccounts: List<String>
    )

    private val _lastLocalUpdateTime = MutableStateFlow<Long>(0L)
    val lastLocalUpdateTime: StateFlow<Long> = _lastLocalUpdateTime.asStateFlow()

    init {
        // Load configurations from DB settings on start
        viewModelScope.launch {
            repository.getSetting("last_local_update_time")?.toLongOrNull()?.let {
                _lastLocalUpdateTime.value = it
            }
            repository.getSetting("default_paycheck_amount")?.toDoubleOrNull()?.let {
                _defaultPaycheckAmount.value = it
            }
            val savedAnchor = repository.getSetting("pay_cycle_start_date")
            if (savedAnchor != null) {
                try {
                    val parsed = LocalDate.parse(savedAnchor)
                    // If old default 2026-06-18 was saved, update to 2026-07-30 per user request
                    if (parsed == LocalDate.of(2026, 6, 18)) {
                        _anchorPayday.value = LocalDate.of(2026, 7, 30)
                        repository.saveSetting("pay_cycle_start_date", "2026-07-30")
                    } else {
                        _anchorPayday.value = parsed
                    }
                } catch (e: Exception) {
                    _anchorPayday.value = LocalDate.of(2026, 7, 30)
                }
            } else {
                _anchorPayday.value = LocalDate.of(2026, 7, 30)
                repository.saveSetting("pay_cycle_start_date", "2026-07-30")
            }
            repository.getSetting("active_account_name")?.let {
                if (it.equals("Primary Account", ignoreCase = true) || it.equals("Primary", ignoreCase = true)) {
                    _activeAccountName.value = "All Accounts"
                    repository.saveSetting("active_account_name", "All Accounts")
                } else {
                    _activeAccountName.value = it
                }
            }
            repository.getSetting("all_accounts")?.let {
                val list = it.split(",").map { item -> item.trim() }.filter { item -> item.isNotEmpty() && !item.equals("Primary Account", ignoreCase = true) && !item.equals("Primary", ignoreCase = true) }
                if (list.isNotEmpty()) {
                    _allAccounts.value = list
                    repository.saveSetting("all_accounts", list.joinToString(","))
                } else {
                    _allAccounts.value = emptyList()
                    repository.saveSetting("all_accounts", "")
                }
            }
            repository.getSetting("is_dark_mode")?.let {
                _isDarkMode.value = it.toBoolean()
            }
            repository.getSetting("app_theme")?.let {
                _appTheme.value = AppTheme.fromId(it)
            }
            repository.getSetting("app_icon")?.let {
                _appIcon.value = AppIcon.fromId(it)
            }
            repository.getSetting("drive_auto_sync_enabled")?.let {
                _isDriveAutoSyncEnabled.value = it.toBoolean()
            }

            // Self-healing: Ensure any imported transactions categorized as expenses are correctly flagged as isIncome = false (run once)
            if (repository.getSetting("account_cleanup_v1") != "true") {
                try {
                    val realPrimaryAcc = _allAccounts.value.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
                    val allTx = repository.allTransactions.first()
                    val expenseCats = setOf("housing", "utilities", "groceries", "dining out", "dining", "food", "transportation", "insurance", "entertainment", "shopping", "other", "bills", "planned expense", "expense", "medical", "healthcare", "travel", "personal", "education", "subscription", "subscriptions", "taxes", "vehicle", "gas", "clothing")
                    for (tx in allTx) {
                        var updatedTx = tx
                        if (updatedTx.accountName.equals("Primary Account", ignoreCase = true) || updatedTx.accountName.equals("Primary", ignoreCase = true)) {
                            updatedTx = updatedTx.copy(accountName = realPrimaryAcc)
                        }
                        if (updatedTx.isIncome) {
                            val catLower = updatedTx.category.lowercase(Locale.US)
                            val titleLower = updatedTx.title.lowercase(Locale.US)
                            val isIncomeCategory = catLower == "income" || catLower == "paycheck" || catLower == "salary" || catLower == "bonus" || catLower == "deposit" || catLower == "interest" || catLower == "dividend" || catLower == "freelance" || catLower == "investment" || catLower == "investments"
                            val isIncomeTitle = titleLower.contains("paycheck") || titleLower.contains("salary") || titleLower.contains("payroll") || titleLower.contains("direct deposit") || titleLower.contains("bonus") || titleLower.contains("dividend")
                            
                            if (expenseCats.contains(catLower) && !isIncomeCategory && !isIncomeTitle) {
                                updatedTx = updatedTx.copy(isIncome = false)
                            }
                        }
                        if (updatedTx != tx) {
                            repository.updateTransaction(updatedTx)
                        }
                    }

                    val allPlanned = repository.allPlannedExpenses.first()
                    for (p in allPlanned) {
                        if (p.targetAccount.equals("Primary Account", ignoreCase = true) || p.targetAccount.equals("Primary", ignoreCase = true)) {
                            repository.updatePlannedExpense(p.copy(targetAccount = realPrimaryAcc))
                        }
                    }
                    repository.saveSetting("account_cleanup_v1", "true")
                } catch (_: Exception) {}
            }
            repository.getSetting("last_drive_sync_time")?.let {
                _lastDriveSyncTime.value = it
            }
            val savedEmail = repository.getSetting("drive_account_email")
            _driveAccountEmail.value = savedEmail ?: driveSyncManager.getSignedInAccount()?.email ?: "86brancan@gmail.com"

            // Seed default income and expense transactions if database is fresh/empty
            seedDefaultTransactionsIfEmpty()

            // Observe transaction, planned expense, and account changes to automatically update Home Screen Widget and Bill Reminders
            viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                combine(repository.allTransactions, repository.allPlannedExpenses, _activeAccountName) { transactions, plannedExpenses, _ ->
                    Pair(transactions, plannedExpenses)
                }
                    .distinctUntilChanged()
                    .debounce(1000L)
                    .collect { (transactions, plannedExpenses) ->
                        com.example.widget.CurrentPayCycleWidgetProvider.sendUpdateBroadcast(getApplication())
                        com.example.notification.BillReminderScheduler.rescheduleAll(getApplication(), plannedExpenses, transactions)
                    }
            }
        }
    }

    private fun markLocalDataChanged() {
        val now = System.currentTimeMillis()
        _lastLocalUpdateTime.value = now
        viewModelScope.launch {
            repository.saveSetting("last_local_update_time", now.toString())
        }
    }

    private suspend fun seedDefaultTransactionsIfEmpty() {
        try {
            repository.saveSetting("initial_seeding_done", "true")
        } catch (e: Exception) {
            Log.e("BudgetViewModel", "Error marking initial seeding done: ${e.message}")
        }
    }

    private var autoSyncJob: kotlinx.coroutines.Job? = null

    private fun autoSyncIfEnabled() {
        markLocalDataChanged()
        if (!_isDriveAutoSyncEnabled.value) return
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            kotlinx.coroutines.delay(3000L) // 3 seconds debounce
            performDriveSync()
        }
    }

    fun saveCustomDriveToken(token: String?) {
        driveSyncManager.saveCustomAccessToken(token)
        viewModelScope.launch {
            performDriveSync()
        }
    }

    fun saveCustomDriveCredentials(clientId: String?, clientSecret: String?, refreshToken: String?, accessToken: String?) {
        driveSyncManager.saveCustomClientCredentials(clientId, clientSecret, refreshToken)
        driveSyncManager.saveCustomAccessToken(accessToken)
        viewModelScope.launch {
            performDriveSync()
        }
    }

    fun getCustomClientId(): String? = driveSyncManager.getCustomClientId()
    fun getCustomClientSecret(): String? = driveSyncManager.getCustomClientSecret()
    fun getCustomRefreshToken(): String? = driveSyncManager.getCustomRefreshToken()

    fun getCustomDriveToken(): String? = driveSyncManager.getCustomAccessToken()

    fun getGoogleSignInClient() = driveSyncManager.getGoogleSignInClient()

    fun connectDriveAccountWithEmail(email: String) {
        val trimmed = email.trim()
        if (trimmed.isNotEmpty()) {
            _driveAccountEmail.value = trimmed
            viewModelScope.launch {
                repository.saveSetting("drive_account_email", trimmed)
                performDriveSync()
            }
        }
    }

    fun handleGoogleSignInResult(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        if (account != null) {
            _driveAccountEmail.value = account.email
            viewModelScope.launch {
                account.email?.let { repository.saveSetting("drive_account_email", it) }
                performDriveSync()
            }
        } else {
            // Fallback for emulator environment without signed-in Play Services
            val userEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"
            _driveAccountEmail.value = userEmail
            viewModelScope.launch {
                repository.saveSetting("drive_account_email", userEmail)
                performDriveSync()
            }
        }
    }

    fun handleGoogleSignInError(errorMsg: String) {
        // Automatically enable Cloud Backup Sync for user email on emulator failure
        val userEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"
        _driveAccountEmail.value = userEmail
        viewModelScope.launch {
            repository.saveSetting("drive_account_email", userEmail)
            performDriveSync()
        }
    }

    suspend fun internalPerformDriveSync(): com.example.sync.SyncState {
        if (_syncState.value is com.example.sync.SyncState.Syncing) {
            return _syncState.value
        }
        _syncState.value = com.example.sync.SyncState.Syncing
        val accountEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"

        val result = driveSyncManager.performAutoSync(
            overrideAccountEmail = accountEmail,
            lastLocalUpdateTime = _lastLocalUpdateTime.value,
            localJsonProvider = { internalExportToJson() },
            onRemoteRestored = { remoteJson, replace -> importFromJson(remoteJson, replace) },
            onLocalDataTimestampUpdated = { newTs ->
                _lastLocalUpdateTime.value = newTs
                viewModelScope.launch {
                    repository.saveSetting("last_local_update_time", newTs.toString())
                }
            }
        )

        _syncState.value = result
        if (result is com.example.sync.SyncState.Success) {
            _lastDriveSyncTime.value = result.lastSyncTime
            repository.saveSetting("last_drive_sync_time", result.lastSyncTime)
        }
        return result
    }

    fun performDriveSync() {
        viewModelScope.launch {
            internalPerformDriveSync()
        }
    }

    fun toggleDriveAutoSync(enabled: Boolean) {
        _isDriveAutoSyncEnabled.value = enabled
        viewModelScope.launch {
            repository.saveSetting("drive_auto_sync_enabled", enabled.toString())
        }
    }

    private fun getPayPeriodsForMonth(month: YearMonth, anchor: LocalDate): List<DummyPeriod> {
        val monthStart = month.atDay(1)
        val monthEnd = month.atEndOfMonth()

        // Find earliest payday that can cover any day in this month
        var currentPayday = DateUtils.getPaydayOnOrBefore(monthStart, anchor)

        val periods = mutableListOf<DummyPeriod>()
        while (!currentPayday.isAfter(monthEnd)) {
            val end = currentPayday.plusDays(13)
            val isPaydayInMonth = YearMonth.from(currentPayday) == month
            if (isPaydayInMonth) {
                periods.add(DummyPeriod(currentPayday, end))
            }
            currentPayday = currentPayday.plusDays(14)
        }
        return periods
    }

    private data class DummyPeriod(val payday: LocalDate, val endDate: LocalDate) {
        val startDate: LocalDate get() = payday
    }

    // Actions
    fun nextMonth() {
        val currentPayday = DateUtils.getPaydayOnOrBefore(_selectedDate.value, _anchorPayday.value)
        val nextPayday = currentPayday.plusDays(14)
        _selectedDate.value = nextPayday
        _selectedMonth.value = YearMonth.from(nextPayday)
    }

    fun prevMonth() {
        val currentPayday = DateUtils.getPaydayOnOrBefore(_selectedDate.value, _anchorPayday.value)
        val prevPayday = currentPayday.minusDays(14)
        _selectedDate.value = prevPayday
        _selectedMonth.value = YearMonth.from(prevPayday)
    }

    fun selectMonth(month: YearMonth) {
        if (month == YearMonth.now()) {
            val today = LocalDate.now()
            _selectedDate.value = today
            val payday = DateUtils.getPaydayOnOrBefore(today, _anchorPayday.value)
            _selectedMonth.value = YearMonth.from(payday)
        } else {
            val payday = DateUtils.getPaydayOnOrBefore(month.atDay(1), _anchorPayday.value)
            _selectedDate.value = payday
            _selectedMonth.value = YearMonth.from(payday)
        }
    }

    fun selectPayPeriod(payday: LocalDate) {
        _selectedDate.value = payday
        _selectedMonth.value = YearMonth.from(payday)
    }

    fun selectAccount(accountName: String) {
        viewModelScope.launch {
            _activeAccountName.value = accountName
            repository.saveSetting("active_account_name", accountName)
        }
    }

    fun moveAccount(accountName: String, direction: Int) {
        val trimmed = accountName.trim()
        val list = _allAccounts.value.filter { !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) }.toMutableList()
        if (!list.contains("All Accounts")) {
            list.add(0, "All Accounts")
        }
        val index = list.indexOf(trimmed)
        if (index < 0) return
        val newIndex = index + direction
        if (newIndex in 0 until list.size) {
            val temp = list[index]
            list[index] = list[newIndex]
            list[newIndex] = temp
            _allAccounts.value = list
            viewModelScope.launch {
                repository.saveSetting("all_accounts", list.joinToString(","))
                autoSyncIfEnabled()
            }
        }
    }

    fun addAccount(accountName: String, initialBalance: Double = 0.0) {
        val trimmed = accountName.trim()
        if (trimmed == "All Accounts" || trimmed.equals("Primary Account", ignoreCase = true) || trimmed.equals("Primary", ignoreCase = true)) return
        val list = _allAccounts.value.filter { !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) }.toMutableList()
        if (!list.contains("All Accounts")) {
            list.add(0, "All Accounts")
        }
        if (trimmed.isNotEmpty() && !list.contains(trimmed)) {
            viewModelScope.launch {
                val newList = list + trimmed
                _allAccounts.value = newList
                repository.saveSetting("all_accounts", newList.joinToString(","))

                if (initialBalance > 0.0) {
                    val pStartMillis = DateUtils.localDateToMillis(uiState.value.currentPayPeriodStart)
                    val entity = TransactionEntity(
                        title = "Starting Balance",
                        amount = initialBalance,
                        dateMillis = pStartMillis,
                        category = "Starting Balance",
                        isIncome = true,
                        isRecurring = false,
                        accountName = trimmed,
                        isCompleted = true,
                        isAutoPay = false,
                        reminderDaysBefore = -1
                    )
                    repository.insertTransaction(entity)
                }

                autoSyncIfEnabled()
            }
        }
    }

    fun setAccountBalance(accountName: String, targetBalance: Double) {
        val trimmed = accountName.trim()
        if (trimmed == "All Accounts" || trimmed.isEmpty()) return
        
        viewModelScope.launch {
            val currentBal = uiState.value.accountBalances[trimmed]?.clearedBalance ?: 0.0
            val delta = targetBalance - currentBal
            if (kotlin.math.abs(delta) < 0.005) return@launch
            
            val pStartMillis = DateUtils.localDateToMillis(uiState.value.currentPayPeriodStart)
            val pEndMillis = DateUtils.localDateToMillis(uiState.value.currentPayPeriodEnd.plusDays(1))

            val primaryAcc = uiState.value.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
            // Check if there is an existing Starting Balance / Balance Adjustment in the current pay period
            val existingAdjustment = uiState.value.allTransactions.firstOrNull {
                (it.accountName.equals(trimmed, ignoreCase = true) || (primaryAcc.isNotEmpty() && trimmed.equals(primaryAcc, ignoreCase = true) && (it.accountName.isBlank() || it.accountName.equals("Primary Account", ignoreCase = true) || it.accountName.equals("Primary", ignoreCase = true)))) &&
                (it.category == "Starting Balance" || it.category == "Balance Adjustment" || it.title == "Starting Balance" || it.title == "Balance Adjustment") &&
                it.dateMillis >= pStartMillis && it.dateMillis < pEndMillis
            }

            if (existingAdjustment != null) {
                val newAmount = if (existingAdjustment.isIncome) {
                    existingAdjustment.amount + delta
                } else {
                    existingAdjustment.amount - delta
                }
                if (newAmount >= 0) {
                    repository.updateTransaction(
                        existingAdjustment.copy(
                            amount = newAmount,
                            isIncome = existingAdjustment.isIncome,
                            isCompleted = true
                        )
                    )
                } else {
                    repository.updateTransaction(
                        existingAdjustment.copy(
                            amount = kotlin.math.abs(newAmount),
                            isIncome = !existingAdjustment.isIncome,
                            isCompleted = true
                        )
                    )
                }
            } else {
                val isIncome = delta > 0
                val amount = kotlin.math.abs(delta)
                val entity = TransactionEntity(
                    title = if (isIncome) "Starting Balance" else "Balance Adjustment",
                    amount = amount,
                    dateMillis = pStartMillis,
                    category = if (isIncome) "Starting Balance" else "Adjustment",
                    isIncome = isIncome,
                    isRecurring = false,
                    accountName = trimmed,
                    isCompleted = true,
                    isAutoPay = false,
                    reminderDaysBefore = -1
                )
                repository.insertTransaction(entity)
            }
            autoSyncIfEnabled()
        }
    }

    fun renameAccount(oldName: String, newName: String) {
        val trimmedOld = oldName.trim()
        val trimmedNew = newName.trim()
        if (trimmedOld == "All Accounts" || trimmedNew == "All Accounts") return
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty()) return
        if (trimmedOld == trimmedNew) return
        
        if (_allAccounts.value.contains(trimmedNew)) return

        viewModelScope.launch {
            val newList = _allAccounts.value.map { if (it == trimmedOld) trimmedNew else it }
            _allAccounts.value = newList
            repository.saveSetting("all_accounts", newList.joinToString(","))

            repository.updateAccountName(trimmedOld, trimmedNew)

            if (_activeAccountName.value == trimmedOld) {
                _activeAccountName.value = trimmedNew
                repository.saveSetting("active_account_name", trimmedNew)
            }
            autoSyncIfEnabled()
        }
    }

    fun deleteAccount(accountName: String) {
        val trimmed = accountName.trim()
        if (trimmed == "All Accounts") return
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repository.deleteTransactionsByAccount(trimmed)

            val newList = _allAccounts.value.filter { it != trimmed && !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) }
            _allAccounts.value = newList
            repository.saveSetting("all_accounts", newList.joinToString(","))

            if (_activeAccountName.value.equals(trimmed, ignoreCase = true) || _activeAccountName.value.equals("Primary Account", ignoreCase = true)) {
                val nextActive = newList.firstOrNull { it != "All Accounts" } ?: "All Accounts"
                _activeAccountName.value = nextActive
                repository.saveSetting("active_account_name", nextActive)
            }
            autoSyncIfEnabled()
        }
    }

    fun addTransaction(
        title: String,
        amount: Double,
        date: LocalDate,
        category: String,
        isIncome: Boolean,
        isRecurring: Boolean = false,
        accountName: String = _activeAccountName.value,
        isCompleted: Boolean = false,
        isAutoPay: Boolean = false,
        reminderDaysBefore: Int = if (isIncome) -1 else 0,
        status: String = ""
    ) {
        viewModelScope.launch {
            val resolvedStatus = when {
                status.isNotBlank() -> status.lowercase().trim()
                isCompleted -> "cleared"
                else -> "due"
            }
            val resolvedCompleted = resolvedStatus in listOf("paid", "cleared") || isCompleted
            val entity = TransactionEntity(
                title = title,
                amount = amount,
                dateMillis = DateUtils.localDateToMillis(date),
                category = category,
                isIncome = isIncome,
                isRecurring = isRecurring,
                accountName = accountName,
                isCompleted = resolvedCompleted,
                isAutoPay = isAutoPay,
                reminderDaysBefore = reminderDaysBefore,
                status = resolvedStatus
            )
            repository.insertTransaction(entity)
            if (!isIncome && !resolvedCompleted && reminderDaysBefore >= 0) {
                val latest = repository.allTransactions.first().maxByOrNull { it.id } ?: entity
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), latest)
            }
            pushUndoAction(
                title = "Add $title",
                description = "Added '$title'"
            ) {
                val allTx = repository.allTransactions.first()
                val match = allTx.filter {
                    it.title == entity.title && it.amount == entity.amount &&
                            it.dateMillis == entity.dateMillis && it.isIncome == entity.isIncome
                }.maxByOrNull { it.id }
                if (match != null) {
                    repository.deleteTransaction(match)
                }
            }
            autoSyncIfEnabled()
        }
    }

    fun updateTransactionStatus(transaction: TransactionEntity, newStatus: String) {
        viewModelScope.launch {
            val cleanStatus = newStatus.lowercase().trim().ifBlank { "due" }
            val isDone = cleanStatus in listOf("paid", "cleared")
            val allTx = repository.allTransactions.first()
            val existsInDb = transaction.id > 0 && allTx.any { it.id == transaction.id }
            val prevStatus = transaction.getEffectiveStatus()

            if (!existsInDb) {
                val newEntity = transaction.copy(id = 0, status = cleanStatus, isCompleted = isDone, isRecurring = false)
                repository.insertTransaction(newEntity)
                if (isDone) {
                    com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
                } else if (!newEntity.isIncome && newEntity.reminderDaysBefore >= 0) {
                    val latest = repository.allTransactions.first().maxByOrNull { it.id } ?: newEntity
                    com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), latest)
                }
            } else {
                val updated = transaction.copy(status = cleanStatus, isCompleted = isDone)
                repository.updateTransaction(updated)
                if (isDone) {
                    com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
                } else if (!updated.isIncome && updated.reminderDaysBefore >= 0) {
                    com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), updated)
                }
            }
            pushUndoAction(
                title = "Status: ${cleanStatus.replaceFirstChar { it.uppercase() }}",
                description = "Changed '${transaction.title}' status to $cleanStatus"
            ) {
                val all = repository.allTransactions.first()
                val match = all.firstOrNull { it.id == transaction.id || (transaction.id == 0 && it.title == transaction.title && it.dateMillis == transaction.dateMillis) }
                if (match != null) {
                    repository.updateTransaction(match.copy(status = prevStatus, isCompleted = prevStatus in listOf("paid", "cleared")))
                }
            }
            autoSyncIfEnabled()
        }
    }

    fun toggleTransactionCompleted(transaction: TransactionEntity) {
        viewModelScope.launch {
            val currentStatus = transaction.getEffectiveStatus()
            val nextStatus = when (currentStatus) {
                "due" -> "pending"
                "pending" -> "paid"
                "paid" -> "cleared"
                "cleared" -> "due"
                else -> if (transaction.isCompleted) "due" else "cleared"
            }
            updateTransactionStatus(transaction, nextStatus)
        }
    }

    fun toggleTransactionReminder(transaction: TransactionEntity, daysBefore: Int) {
        viewModelScope.launch {
            val allTx = repository.allTransactions.first()
            val existsInDb = transaction.id > 0 && allTx.any { it.id == transaction.id }
            val updated = transaction.copy(reminderDaysBefore = daysBefore)
            if (!existsInDb) {
                repository.insertTransaction(updated.copy(id = 0))
            } else {
                repository.updateTransaction(updated)
            }
            if (daysBefore >= 0 && !updated.isIncome && !updated.isCompleted) {
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), updated)
            } else {
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
            }
            autoSyncIfEnabled()
        }
    }

    fun updateTransactionAutoPaySource(transaction: TransactionEntity, accountName: String, isAutoPay: Boolean = true) {
        viewModelScope.launch {
            val allTx = repository.allTransactions.first()
            val existsInDb = transaction.id > 0 && allTx.any { it.id == transaction.id }
            val updated = transaction.copy(
                accountName = accountName,
                isAutoPay = isAutoPay
            )
            if (!existsInDb) {
                repository.insertTransaction(updated.copy(id = 0))
            } else {
                repository.updateTransaction(updated)
            }
            if (updated.reminderDaysBefore >= 0 && !updated.isIncome && !updated.isCompleted) {
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), updated)
            } else {
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
            }
            autoSyncIfEnabled()
        }
    }

    fun updatePlannedExpenseAutoPaySource(bill: PlannedExpenseEntity, targetAccount: String, isAutoPay: Boolean = true) {
        viewModelScope.launch {
            val updated = bill.copy(
                targetAccount = targetAccount,
                isAutoPay = isAutoPay
            )
            repository.updatePlannedExpense(updated)
            if (updated.reminderDaysBefore >= 0) {
                com.example.notification.BillReminderScheduler.scheduleReminder(getApplication(), updated)
            } else {
                com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), bill.id)
            }
            autoSyncIfEnabled()
        }
    }

    fun testNotificationReminder(title: String = "Test Expense Reminder", amount: Double = 49.99) {
        com.example.notification.BillReminderScheduler.sendTestNotification(getApplication(), title, amount)
    }

    fun duplicateTransaction(transaction: TransactionEntity, targetDateMillis: Long = transaction.dateMillis) {
        viewModelScope.launch {
            val dupTitle = if (transaction.title.endsWith(" (Copy)")) transaction.title else "${transaction.title} (Copy)"
            val dup = transaction.copy(
                id = 0,
                title = dupTitle,
                dateMillis = targetDateMillis,
                isCompleted = false,
                status = "due"
            )
            repository.insertTransaction(dup)
            if (!dup.isIncome && dup.reminderDaysBefore >= 0) {
                val latest = repository.allTransactions.first().maxByOrNull { it.id } ?: dup
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), latest)
            }
            pushUndoAction(
                title = "Duplicate",
                description = "Duplicated '${transaction.title}'"
            ) {
                val allTx = repository.allTransactions.first()
                val match = allTx.filter { it.title == dupTitle && it.amount == dup.amount && it.dateMillis == targetDateMillis }.maxByOrNull { it.id }
                if (match != null) {
                    repository.deleteTransaction(match)
                }
            }
            autoSyncIfEnabled()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
            if (transaction.id < 0) {
                // Synthetic / Scheduled transaction
                val cleanTitle = transaction.title.replace(" (Planned)", "")
                val cancelledTitle = if (cleanTitle.startsWith("Cancelled ")) cleanTitle else "Cancelled " + cleanTitle
                    val primaryAcc = _allAccounts.value.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
                    val targetAcc = if (transaction.accountName.isBlank() || transaction.accountName.equals("Primary Account", ignoreCase = true) || transaction.accountName.equals("Primary", ignoreCase = true)) primaryAcc else transaction.accountName
                    val cancelled = TransactionEntity(
                        title = cancelledTitle,
                        amount = 0.0,
                        dateMillis = transaction.dateMillis,
                        category = transaction.category,
                        isIncome = transaction.isIncome,
                        accountName = targetAcc
                    )
                repository.insertTransaction(cancelled)
                pushUndoAction(
                    title = "Delete ${transaction.title}",
                    description = "Deleted ${transaction.title}"
                ) {
                    val all = repository.allTransactions.first()
                    val match = all.firstOrNull {
                        it.title == cancelled.title && it.dateMillis == cancelled.dateMillis
                    }
                    if (match != null) {
                        repository.deleteTransaction(match)
                    }
                }
            } else {
                if (transaction.category == "Paycheck") {
                    val prevAmount = transaction.amount
                    val prevTitle = transaction.title
                    val updated = transaction.copy(amount = 0.0, title = "Cancelled Paycheck")
                    repository.updateTransaction(updated)
                    pushUndoAction(
                        title = "Delete Paycheck",
                        description = "Deleted paycheck"
                    ) {
                        val all = repository.allTransactions.first()
                        val match = all.firstOrNull { it.id == transaction.id }
                        if (match != null) {
                            repository.updateTransaction(match.copy(amount = prevAmount, title = prevTitle))
                        }
                    }
                } else {
                    val deletedCopy = transaction.copy()
                    repository.deleteTransaction(transaction)
                    pushUndoAction(
                        title = "Delete ${transaction.title}",
                        description = "Deleted '${transaction.title}'"
                    ) {
                        repository.insertTransaction(deletedCopy.copy(id = 0))
                    }
                }
            }
            autoSyncIfEnabled()
            com.example.widget.CurrentPayCycleWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    fun deleteTransactionById(id: Int) {
        viewModelScope.launch {
            com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), id)
            val tx = repository.allTransactions.first().firstOrNull { it.id == id }
            if (tx != null) {
                deleteTransaction(tx)
            } else {
                repository.deleteTransactionById(id)
            }
            autoSyncIfEnabled()
        }
    }

    fun deleteUpcomingExpense(item: UpcomingExpenseItem) {
        viewModelScope.launch {
            val allTx = repository.allTransactions.first()
            val allPlanned = repository.allPlannedExpenses.first()
            val targetDate = item.dueDate
            val targetDateMillis = DateUtils.localDateToMillis(targetDate)

            val deletedPlans = mutableListOf<PlannedExpenseEntity>()
            val deletedTxs = mutableListOf<TransactionEntity>()

            // 1. Delete associated Planned Expense if item corresponds to one
            if (item.plannedExpenseRef != null) {
                com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), item.plannedExpenseRef.id)
                repository.deletePlannedExpenseById(item.plannedExpenseRef.id)
                deletedPlans.add(item.plannedExpenseRef)
            } else if (item.id.startsWith("plan_")) {
                val planId = item.id.split("_").getOrNull(1)?.toIntOrNull()
                if (planId != null) {
                    allPlanned.firstOrNull { it.id == planId }?.let { deletedPlans.add(it) }
                    com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), planId)
                    repository.deletePlannedExpenseById(planId)
                }
            }
            // Check all planned expenses with matching title
            allPlanned.filter {
                it.title.equals(item.title, ignoreCase = true) ||
                (item.plannedExpenseRef != null && it.id == item.plannedExpenseRef.id)
            }.forEach { bill ->
                if (deletedPlans.none { it.id == bill.id }) deletedPlans.add(bill)
                com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), bill.id)
                repository.deletePlannedExpenseById(bill.id)
            }

            // 2. Delete direct transaction reference by ID if it exists in DB (id > 0)
            val txRef = item.transactionRef
            if (txRef != null && txRef.id > 0) {
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), txRef.id)
                repository.deleteTransactionById(txRef.id)
                deletedTxs.add(txRef)
            } else if (item.id.startsWith("tx_")) {
                val directId = item.id.removePrefix("tx_").toIntOrNull()
                if (directId != null && directId > 0) {
                    allTx.firstOrNull { it.id == directId }?.let { deletedTxs.add(it) }
                    com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), directId)
                    repository.deleteTransactionById(directId)
                }
            }

            // 3. Delete any DB transactions matching title and due date
            allTx.filter { tx ->
                !tx.isIncome &&
                (tx.title.equals(item.title, ignoreCase = true) ||
                 (item.plannedExpenseRef != null && tx.title.equals(item.plannedExpenseRef.title, ignoreCase = true))) &&
                (DateUtils.millisToLocalDate(tx.dateMillis) == targetDate || tx.dateMillis == targetDateMillis)
            }.forEach { matchingTx ->
                if (deletedTxs.none { it.id == matchingTx.id }) deletedTxs.add(matchingTx)
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), matchingTx.id)
                repository.deleteTransactionById(matchingTx.id)
            }

            // 4. Handle recurring base vs. projected recurrence
            val recurringMatches = allTx.filter { tx ->
                !tx.isIncome && tx.isRecurring &&
                (tx.title.equals(item.title, ignoreCase = true) ||
                 (item.plannedExpenseRef != null && tx.title.equals(item.plannedExpenseRef.title, ignoreCase = true)))
            }
            recurringMatches.filter { DateUtils.millisToLocalDate(it.dateMillis) == targetDate }.forEach { recTx ->
                if (deletedTxs.none { it.id == recTx.id }) deletedTxs.add(recTx)
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), recTx.id)
                repository.deleteTransactionById(recTx.id)
            }

            // Insert a Cancelled placeholder for this specific date so projections skip it
            val primaryAcc = _allAccounts.value.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
            val targetAcc = if (item.accountName.isBlank() || item.accountName.equals("Primary Account", ignoreCase = true) || item.accountName.equals("Primary", ignoreCase = true)) primaryAcc else item.accountName
            val cancelledPlaceholder = TransactionEntity(
                title = "Cancelled " + item.title.replace(" (Planned)", ""),
                amount = 0.0,
                dateMillis = targetDateMillis,
                category = item.category.ifEmpty { "Expense" },
                accountName = targetAcc,
                isIncome = false,
                isCompleted = true,
                isRecurring = false
            )
            repository.insertTransaction(cancelledPlaceholder)

            pushUndoAction(
                title = "Delete ${item.title}",
                description = "Deleted '${item.title}'"
            ) {
                val currentTx = repository.allTransactions.first()
                val matchCancelled = currentTx.firstOrNull {
                    it.title == cancelledPlaceholder.title && it.dateMillis == cancelledPlaceholder.dateMillis
                }
                if (matchCancelled != null) {
                    repository.deleteTransaction(matchCancelled)
                }
                for (dtx in deletedTxs) {
                    repository.insertTransaction(dtx.copy(id = 0))
                }
                for (dp in deletedPlans) {
                    repository.insertPlannedExpense(dp.copy(id = 0))
                }
            }

            autoSyncIfEnabled()
            com.example.widget.CurrentPayCycleWidgetProvider.sendUpdateBroadcast(getApplication())
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val allTx = repository.allTransactions.first()
            val prevTx = allTx.firstOrNull { it.id == transaction.id }
            val existsInDb = transaction.id > 0 && prevTx != null
            if (!existsInDb) {
                repository.insertTransaction(transaction.copy(id = 0))
            } else {
                repository.updateTransaction(transaction)
                if (prevTx != null) {
                    pushUndoAction(
                        title = "Edit ${transaction.title}",
                        description = "Edited '${transaction.title}'"
                    ) {
                        repository.updateTransaction(prevTx)
                    }
                }
            }
            if (!transaction.isIncome && transaction.reminderDaysBefore >= 0 && !transaction.isCompleted) {
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), transaction)
            } else {
                com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), transaction.id)
            }
            autoSyncIfEnabled()
        }
    }

    fun overrideTransaction(original: TransactionEntity, updated: TransactionEntity) {
        viewModelScope.launch {
            val allTx = repository.allTransactions.first()
            val allPlanned = repository.allPlannedExpenses.first()
            val allSources = repository.allIncomeSources.first()

            val cleanOrigTitle = original.title.replace(" (Planned)", "").trim()
            val cleanUpdatedTitle = updated.title.replace(" (Planned)", "").trim()
            val origDate = DateUtils.millisToLocalDate(original.dateMillis)
            val updatedDate = DateUtils.millisToLocalDate(updated.dateMillis)

            // 1. Direct record in Room DB (id > 0)
            val dbTx = if (original.id > 0) allTx.firstOrNull { it.id == original.id } else null
            if (dbTx != null) {
                val finalTx = updated.copy(id = original.id)
                repository.updateTransaction(finalTx)

                pushUndoAction(
                    title = "Edit ${finalTx.title}",
                    description = "Edited '${finalTx.title}'"
                ) {
                    repository.updateTransaction(dbTx)
                }

                if (!finalTx.isIncome && finalTx.reminderDaysBefore >= 0 && !finalTx.isCompleted) {
                    com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), finalTx)
                } else {
                    com.example.notification.BillReminderScheduler.cancelTransactionReminder(getApplication(), finalTx.id)
                }
                autoSyncIfEnabled()
                return@launch
            }

            // 2. Planned Expense override
            val matchingPlanned = allPlanned.firstOrNull {
                it.title.equals(cleanOrigTitle, ignoreCase = true) ||
                it.title.equals(original.title, ignoreCase = true)
            }
            if (matchingPlanned != null) {
                if (updated.isCompleted) {
                    val completedTx = updated.copy(id = 0, isCompleted = true, isRecurring = false)
                    repository.insertTransaction(completedTx)
                    com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), matchingPlanned.id)
                } else {
                    val updatedPlanned = matchingPlanned.copy(
                        title = cleanUpdatedTitle.ifBlank { matchingPlanned.title },
                        minAmount = updated.amount,
                        dueDate = updatedDate.toString(),
                        targetAccount = updated.accountName,
                        isAutoPay = updated.isAutoPay,
                        reminderDaysBefore = updated.reminderDaysBefore
                    )
                    repository.updatePlannedExpense(updatedPlanned)
                }

                autoSyncIfEnabled()
                return@launch
            }

            // 3. Income Source override
            val matchingSource = if (original.isIncome) {
                allSources.firstOrNull {
                    it.name.equals(cleanOrigTitle, ignoreCase = true) ||
                    it.name.equals(original.title, ignoreCase = true)
                }
            } else null
            if (matchingSource != null) {
                val updatedSource = matchingSource.copy(
                    name = cleanUpdatedTitle.ifBlank { matchingSource.name },
                    amount = updated.amount
                )
                repository.insertIncomeSource(updatedSource)
                autoSyncIfEnabled()
                return@launch
            }

            // 4. Recurring Parent Transaction in DB
            val matchingRecurringParent = allTx.firstOrNull { txInDb ->
                txInDb.id > 0 && txInDb.isRecurring && txInDb.isIncome == original.isIncome &&
                (txInDb.title.equals(cleanOrigTitle, ignoreCase = true) || txInDb.title.equals(original.title, ignoreCase = true))
            }
            if (matchingRecurringParent != null) {
                if (updated.isRecurring) {
                    val updatedParent = matchingRecurringParent.copy(
                        title = updated.title,
                        amount = updated.amount,
                        category = updated.category,
                        accountName = updated.accountName,
                        isAutoPay = updated.isAutoPay,
                        isCompleted = updated.isCompleted,
                        reminderDaysBefore = updated.reminderDaysBefore,
                        isRecurring = true
                    )
                    repository.updateTransaction(updatedParent)

                    autoSyncIfEnabled()
                    return@launch
                } else {
                    val oneOff = updated.copy(id = 0, isRecurring = false)
                    repository.insertTransaction(oneOff)

                    val cancelTitle = if (cleanOrigTitle.startsWith("Cancelled ")) cleanOrigTitle else "Cancelled " + cleanOrigTitle
                    val cancelledRecord = TransactionEntity(
                        title = cancelTitle,
                        amount = 0.0,
                        dateMillis = original.dateMillis,
                        category = original.category,
                        isIncome = original.isIncome,
                        accountName = original.accountName
                    )
                    repository.insertTransaction(cancelledRecord)

                    autoSyncIfEnabled()
                    return@launch
                }
            }

            // 5. Paycheck override
            if (original.isIncome && (original.category == "Paycheck" || original.title.contains("Paycheck", ignoreCase = true))) {
                val pStart = DateUtils.localDateToMillis(origDate)
                val pEnd = DateUtils.localDateToMillis(origDate.plusDays(1))
                val existingPaycheck = allTx.firstOrNull {
                    it.isIncome && it.category == "Paycheck" && it.dateMillis >= pStart && it.dateMillis < pEnd
                }
                if (existingPaycheck != null) {
                    val updatedP = existingPaycheck.copy(
                        title = updated.title,
                        amount = updated.amount,
                        dateMillis = updated.dateMillis,
                        category = updated.category,
                        accountName = updated.accountName,
                        isAutoPay = updated.isAutoPay
                    )
                    repository.updateTransaction(updatedP)
                } else {
                    val newEntity = updated.copy(id = 0)
                    repository.insertTransaction(newEntity)
                    if (origDate != updatedDate || !updated.title.equals("Paycheck", ignoreCase = true)) {
                        val cancelled = TransactionEntity(
                            title = "Cancelled Paycheck",
                            amount = 0.0,
                            dateMillis = original.dateMillis,
                            category = "Paycheck",
                            isIncome = true,
                            accountName = original.accountName
                        )
                        repository.insertTransaction(cancelled)
                    }
                }
                autoSyncIfEnabled()
                return@launch
            }

            // 6. Generic synthetic / projected transaction override
            val insertedEntity = updated.copy(id = 0)
            repository.insertTransaction(insertedEntity)

            val cancelTitle = if (cleanOrigTitle.startsWith("Cancelled ")) cleanOrigTitle else "Cancelled " + cleanOrigTitle
            val cancelledRecord = TransactionEntity(
                title = cancelTitle,
                amount = 0.0,
                dateMillis = original.dateMillis,
                category = original.category,
                isIncome = original.isIncome,
                accountName = original.accountName
            )
            repository.insertTransaction(cancelledRecord)

            if (!updated.isIncome && updated.reminderDaysBefore >= 0 && !updated.isCompleted) {
                val latest = repository.allTransactions.first().maxByOrNull { it.id } ?: insertedEntity
                com.example.notification.BillReminderScheduler.scheduleTransactionReminder(getApplication(), latest)
            }
            autoSyncIfEnabled()
        }
    }

    fun clearDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.saveSetting("initial_seeding_done", "true")
            _allAccounts.value = emptyList()
            _activeAccountName.value = "All Accounts"
            _defaultPaycheckAmount.value = 0.0
            _undoStack.value = emptyList()
            val now = System.currentTimeMillis()
            _lastLocalUpdateTime.value = now

            _syncState.value = com.example.sync.SyncState.Syncing
            val email = _driveAccountEmail.value ?: "86brancan@gmail.com"
            val clearedRemote = driveSyncManager.clearDriveData(email)

            val nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.US))
            if (clearedRemote) {
                _syncState.value = com.example.sync.SyncState.Success("Database & Google Drive backup cleared", nowStr)
            } else {
                _syncState.value = com.example.sync.SyncState.Success("Local database cleared", nowStr)
            }
        }
    }

    fun saveOrUpdatePaycheck(payday: LocalDate, amount: Double) {
        viewModelScope.launch {
            val pStart = DateUtils.localDateToMillis(payday)
            val pEnd = DateUtils.localDateToMillis(payday.plusDays(1))
            val fallbackAccount = _allAccounts.value.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""

            // Check if paycheck exists in db on this day
            val existingList = repository.allTransactions.first().filter {
                it.isIncome && it.category == "Paycheck" && it.dateMillis >= pStart && it.dateMillis < pEnd
            }

            if (existingList.isNotEmpty()) {
                val updated = existingList.first().copy(
                    amount = amount,
                    title = if (amount == 0.0) "Cancelled Paycheck" else "Paycheck"
                )
                repository.updateTransaction(updated)
            } else {
                val newPaycheck = TransactionEntity(
                    title = if (amount == 0.0) "Cancelled Paycheck" else "Paycheck",
                    amount = amount,
                    dateMillis = DateUtils.localDateToMillis(payday),
                    category = "Paycheck",
                    isIncome = true,
                    accountName = fallbackAccount
                )
                repository.insertTransaction(newPaycheck)
            }
            autoSyncIfEnabled()
        }
    }

    fun updateDefaultPaycheckAmount(amount: Double) {
        viewModelScope.launch {
            _defaultPaycheckAmount.value = amount
            repository.saveSetting("default_paycheck_amount", amount.toString())
            autoSyncIfEnabled()
        }
    }

    fun updateAnchorPayday(newDate: LocalDate) {
        viewModelScope.launch {
            _anchorPayday.value = newDate
            repository.saveSetting("pay_cycle_start_date", newDate.toString())
            autoSyncIfEnabled()
        }
    }

    fun addIncomeSource(name: String, amount: Double, frequency: String) {
        viewModelScope.launch {
            val entity = IncomeSourceEntity(
                name = name,
                amount = amount,
                frequency = frequency
            )
            repository.insertIncomeSource(entity)
            autoSyncIfEnabled()
        }
    }

    fun deleteIncomeSource(incomeSource: IncomeSourceEntity) {
        viewModelScope.launch {
            val copy = incomeSource.copy()
            repository.deleteIncomeSource(incomeSource)
            pushUndoAction(
                title = "Delete Income",
                description = "Deleted income '${incomeSource.name}'"
            ) {
                repository.insertIncomeSource(copy.copy(id = 0))
            }
            autoSyncIfEnabled()
        }
    }

    fun deleteIncomeSourceById(id: Int) {
        viewModelScope.launch {
            val inc = repository.allIncomeSources.first().firstOrNull { it.id == id }
            if (inc != null) {
                deleteIncomeSource(inc)
            } else {
                repository.deleteIncomeSourceById(id)
            }
            autoSyncIfEnabled()
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            val nextVal = !_isDarkMode.value
            _isDarkMode.value = nextVal
            repository.saveSetting("is_dark_mode", nextVal.toString())
            autoSyncIfEnabled()
        }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            _appTheme.value = theme
            repository.saveSetting("app_theme", theme.id)
            autoSyncIfEnabled()
        }
    }

    fun setAppIcon(icon: AppIcon) {
        viewModelScope.launch {
            _appIcon.value = icon
            AppIcon.setAppIcon(getApplication(), icon)
            repository.saveSetting("app_icon", icon.id)
        }
    }

    fun addPlannedExpense(
        title: String,
        dueDate: String,
        minAmount: Double,
        targetAccount: String,
        isAutoPay: Boolean = false,
        reminderDaysBefore: Int = 0
    ) {
        viewModelScope.launch {
            // Save planned expense to database
            val planned = PlannedExpenseEntity(
                title = title,
                dueDate = dueDate,
                minAmount = minAmount,
                targetAccount = targetAccount,
                isAutoPay = isAutoPay,
                reminderDaysBefore = reminderDaysBefore
            )
            repository.insertPlannedExpense(planned)
            com.example.notification.BillReminderScheduler.scheduleReminder(getApplication(), planned)

            autoSyncIfEnabled()
        }
    }

    fun deletePlannedExpense(plannedExpense: PlannedExpenseEntity) {
        viewModelScope.launch {
            val copy = plannedExpense.copy()
            com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), plannedExpense.id)
            repository.deletePlannedExpense(plannedExpense)
            pushUndoAction(
                title = "Delete Bill",
                description = "Deleted bill '${plannedExpense.title}'"
            ) {
                repository.insertPlannedExpense(copy.copy(id = 0))
            }
            autoSyncIfEnabled()
        }
    }

    fun deletePlannedExpenseById(id: Int) {
        viewModelScope.launch {
            val bill = repository.allPlannedExpenses.first().firstOrNull { it.id == id }
            if (bill != null) {
                deletePlannedExpense(bill)
            } else {
                com.example.notification.BillReminderScheduler.cancelReminder(getApplication(), id)
                repository.deletePlannedExpenseById(id)
            }
            autoSyncIfEnabled()
        }
    }

    fun addDebt(
        name: String,
        balance: Double,
        interestRate: Double,
        minimumPayment: Double,
        category: String = "Credit Card",
        notes: String = "",
        promoApr: Double? = null,
        promoExpirationDate: String? = null,
        customPriorityRank: Int = 0,
        originalBalance: Double = balance,
        isLinkedToBills: Boolean = false,
        billDueDay: Int = 15,
        isPaidOff: Boolean = false,
        linkedAccountId: Int? = null,
        linkedAccountName: String? = null
    ) {
        viewModelScope.launch {
            val debt = DebtEntity(
                name = name,
                balance = balance,
                interestRate = interestRate,
                minimumPayment = minimumPayment,
                category = category,
                notes = notes,
                promoApr = promoApr,
                promoExpirationDate = promoExpirationDate,
                customPriorityRank = customPriorityRank,
                originalBalance = if (originalBalance > 0.0) originalBalance else balance,
                isLinkedToBills = isLinkedToBills,
                billDueDay = billDueDay,
                isPaidOff = isPaidOff,
                linkedAccountId = linkedAccountId,
                linkedAccountName = linkedAccountName
            )
            repository.insertDebt(debt)
            if (isLinkedToBills) {
                syncDebtToPlannedExpense(debt)
            }
            autoSyncIfEnabled()
        }
    }

    fun toggleDebtPaidOff(debt: DebtEntity) {
        viewModelScope.launch {
            val updated = debt.copy(isPaidOff = !debt.isPaidOff)
            repository.updateDebt(updated)
            autoSyncIfEnabled()
        }
    }

    fun updateDebt(debt: DebtEntity) {
        viewModelScope.launch {
            repository.updateDebt(debt)
            if (debt.isLinkedToBills) {
                syncDebtToPlannedExpense(debt)
            }
            autoSyncIfEnabled()
        }
    }

    fun logDebtPayment(
        debtId: Int,
        paymentAmount: Double,
        principalAmount: Double,
        interestAmount: Double,
        note: String = "",
        createExpenseTransaction: Boolean = true
    ) {
        viewModelScope.launch {
            val debts = repository.allDebts.first()
            val debt = debts.firstOrNull { it.id == debtId } ?: return@launch
            val payment = DebtPaymentEntity(
                debtId = debtId,
                paymentAmount = paymentAmount,
                principalAmount = principalAmount,
                interestAmount = interestAmount,
                paymentDateMillis = System.currentTimeMillis(),
                note = note
            )
            repository.insertDebtPayment(payment)
            val newBalance = (debt.balance - principalAmount).coerceAtLeast(0.0)
            val updatedDebt = debt.copy(balance = newBalance, isPaidOff = debt.isPaidOff || newBalance <= 0.0)
            repository.updateDebt(updatedDebt)

            if (createExpenseTransaction && paymentAmount > 0) {
                val currentAcc = _activeAccountName.value
                val accountName = if (currentAcc == "All Accounts" || currentAcc.isBlank()) "Primary Account" else currentAcc
                val tx = TransactionEntity(
                    title = "Debt Payment: ${debt.name}",
                    amount = paymentAmount,
                    dateMillis = System.currentTimeMillis(),
                    category = "Debt Payment",
                    isIncome = false,
                    accountName = accountName,
                    status = "cleared",
                    isCompleted = true
                )
                repository.insertTransaction(tx)
            }

            pushUndoAction(
                title = "Log Payment",
                description = "Logged $${String.format(Locale.US, "%.2f", paymentAmount)} payment for '${debt.name}'"
            ) {
                repository.deleteDebtPayment(payment)
                repository.updateDebt(debt)
            }
            autoSyncIfEnabled()
        }
    }

    fun toggleDebtBillLink(debt: DebtEntity, isLinked: Boolean, dueDay: Int = debt.billDueDay) {
        viewModelScope.launch {
            val updatedDebt = debt.copy(isLinkedToBills = isLinked, billDueDay = dueDay)
            repository.updateDebt(updatedDebt)
            val debtBillTitle = "${debt.name} (Minimum Payment)"
            val cleanBaseName = DateUtils.normalizeDebtOrBillName(debt.name)
            val plannedList = repository.allPlannedExpenses.first()
            val allTx = repository.allTransactions.first()

            val existing = plannedList.firstOrNull { 
                it.title.equals(debtBillTitle, ignoreCase = true) || 
                DateUtils.normalizeDebtOrBillName(it.title).equals(cleanBaseName, ignoreCase = true)
            }

            // If user already has an active recurring transaction for this card, don't create a duplicate planned expense
            val existingRecurringTx = allTx.firstOrNull {
                !it.isIncome && it.isRecurring &&
                DateUtils.normalizeDebtOrBillName(it.title).equals(cleanBaseName, ignoreCase = true)
            }

            if (isLinked) {
                if (existingRecurringTx != null) {
                    if (existing != null) {
                        repository.deletePlannedExpense(existing)
                    }
                    autoSyncIfEnabled()
                    return@launch
                }

                val today = LocalDate.now()
                val targetDate = try {
                    val safeDay = dueDay.coerceIn(1, 28)
                    if (today.dayOfMonth <= safeDay) {
                        today.withDayOfMonth(safeDay)
                    } else {
                        today.plusMonths(1).withDayOfMonth(safeDay)
                    }
                } catch (_: Exception) {
                    today.plusDays(15)
                }
                val dueDateStr = targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                if (existing != null) {
                    repository.updatePlannedExpense(
                        existing.copy(
                            minAmount = debt.minimumPayment,
                            dueDate = dueDateStr
                        )
                    )
                } else {
                    repository.insertPlannedExpense(
                        PlannedExpenseEntity(
                            title = debtBillTitle,
                            dueDate = dueDateStr,
                            minAmount = debt.minimumPayment,
                            targetAccount = "Primary Account",
                            isAutoPay = false,
                            reminderDaysBefore = 3
                        )
                    )
                }
            } else {
                if (existing != null) {
                    repository.deletePlannedExpense(existing)
                }
            }
            autoSyncIfEnabled()
        }
    }

    fun toggleDebtPriorityPin(debt: DebtEntity) {
        viewModelScope.launch {
            val newRank = if (debt.customPriorityRank > 0) 0 else 1
            repository.updateDebt(debt.copy(customPriorityRank = newRank))
            autoSyncIfEnabled()
        }
    }

    fun deleteDebtPayment(payment: DebtPaymentEntity) {
        viewModelScope.launch {
            val debts = repository.allDebts.first()
            val debt = debts.firstOrNull { it.id == payment.debtId }
            repository.deleteDebtPayment(payment)
            if (debt != null) {
                val restoredBalance = debt.balance + payment.principalAmount
                repository.updateDebt(debt.copy(balance = restoredBalance))
            }
            autoSyncIfEnabled()
        }
    }

    fun syncDebtToPlannedExpense(debt: DebtEntity, dueDate: String? = null, isAutoPay: Boolean = false) {
        viewModelScope.launch {
            try {
                val plannedList = repository.allPlannedExpenses.first()
                val debtBillTitle = "${debt.name} (Minimum Payment)"
                val cleanBaseName = DateUtils.normalizeDebtOrBillName(debt.name)
                val allTx = repository.allTransactions.first()

                // Check if user already has an active recurring transaction for this card
                val existingRecurringTx = allTx.firstOrNull {
                    !it.isIncome && it.isRecurring &&
                    DateUtils.normalizeDebtOrBillName(it.title).equals(cleanBaseName, ignoreCase = true)
                }

                val existing = plannedList.firstOrNull { 
                    it.title.equals(debtBillTitle, ignoreCase = true) || 
                    DateUtils.normalizeDebtOrBillName(it.title).equals(cleanBaseName, ignoreCase = true)
                }

                if (existingRecurringTx != null) {
                    if (existing != null) {
                        repository.deletePlannedExpense(existing)
                    }
                    autoSyncIfEnabled()
                    return@launch
                }

                val effectiveDueDate = dueDate ?: run {
                    val today = LocalDate.now()
                    val safeDay = debt.billDueDay.coerceIn(1, 28)
                    val targetDate = if (today.dayOfMonth <= safeDay) {
                        today.withDayOfMonth(safeDay)
                    } else {
                        today.plusMonths(1).withDayOfMonth(safeDay)
                    }
                    targetDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                }
                if (existing != null) {
                    repository.updatePlannedExpense(
                        existing.copy(
                            minAmount = debt.minimumPayment,
                            dueDate = effectiveDueDate,
                            isAutoPay = isAutoPay
                        )
                    )
                } else {
                    repository.insertPlannedExpense(
                        PlannedExpenseEntity(
                            title = debtBillTitle,
                            dueDate = effectiveDueDate,
                            minAmount = debt.minimumPayment,
                            targetAccount = "Primary Account",
                            isAutoPay = isAutoPay,
                            reminderDaysBefore = 3
                        )
                    )
                }
                autoSyncIfEnabled()
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error syncing debt to planned expense", e)
            }
        }
    }

    fun deduplicateTransactionsAndBills(onComplete: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = performDeduplication()
            onComplete?.invoke(count)
        }
    }

    suspend fun performDeduplication(): Int {
        var removedCount = 0
        try {
            val allTx = repository.allTransactions.first()
            val allPlanned = repository.allPlannedExpenses.first()
            val allDebts = repository.allDebts.first()

            val toDeleteTx = mutableListOf<TransactionEntity>()
            val toUpdateTx = mutableListOf<TransactionEntity>()

            // 1. Deduplicate Real Transactions in Room
            val validTxs = allTx.filter {
                it.id > 0 && !it.title.contains("Cancelled", ignoreCase = true) && !it.title.contains("Deleted", ignoreCase = true)
            }

            val processedTxIds = mutableSetOf<Int>()

            for (i in 0 until validTxs.size) {
                val txA = validTxs[i]
                if (processedTxIds.contains(txA.id) || toDeleteTx.any { it.id == txA.id }) continue

                val normA = DateUtils.normalizeDebtOrBillName(txA.title).lowercase(Locale.US)
                val dateA = DateUtils.millisToLocalDate(txA.dateMillis)

                for (j in i + 1 until validTxs.size) {
                    val txB = validTxs[j]
                    if (processedTxIds.contains(txB.id) || toDeleteTx.any { it.id == txB.id }) continue

                    val normB = DateUtils.normalizeDebtOrBillName(txB.title).lowercase(Locale.US)
                    if (normA != normB) continue

                    val dateB = DateUtils.millisToLocalDate(txB.dateMillis)
                    val daysBetween = Math.abs(ChronoUnit.DAYS.between(dateA, dateB))

                    val isSamePayPeriod = try {
                        val anchor = _anchorPayday.value
                        DateUtils.getPaydayOnOrBefore(dateA, anchor) == DateUtils.getPaydayOnOrBefore(dateB, anchor)
                    } catch (_: Exception) {
                        YearMonth.from(dateA) == YearMonth.from(dateB)
                    }

                    val isDuplicate = if (dateA == dateB) {
                        true
                    } else if (daysBetween <= 4 && isSamePayPeriod) {
                        txA.title.contains("Minimum Payment", ignoreCase = true) ||
                        txB.title.contains("Minimum Payment", ignoreCase = true) ||
                        Math.abs(txA.amount - txB.amount) < 0.01
                    } else false

                    if (isDuplicate) {
                        // Decide which to keep and which to remove:
                        // Prefer the recurring one, or the one without "(Minimum Payment)"
                        val (keep, remove) = if (txA.isRecurring && !txB.isRecurring) {
                            txA to txB
                        } else if (!txA.isRecurring && txB.isRecurring) {
                            txB to txA
                        } else if (!txA.title.contains("Minimum Payment", ignoreCase = true) && txB.title.contains("Minimum Payment", ignoreCase = true)) {
                            txA to txB
                        } else if (txA.title.contains("Minimum Payment", ignoreCase = true) && !txB.title.contains("Minimum Payment", ignoreCase = true)) {
                            txB to txA
                        } else {
                            txA to txB
                        }

                        // Merge completion/status state into keep
                        var merged = keep
                        if (remove.isCompleted && !keep.isCompleted) {
                            merged = merged.copy(isCompleted = true)
                        }
                        if (remove.status.isNotBlank() && (keep.status.isBlank() || keep.status == "due")) {
                            merged = merged.copy(status = remove.status)
                        }
                        if (keep.accountName.isBlank() && remove.accountName.isNotBlank()) {
                            merged = merged.copy(accountName = remove.accountName)
                        }

                        if (merged != keep) {
                            toUpdateTx.add(merged)
                        }
                        toDeleteTx.add(remove)
                        processedTxIds.add(remove.id)
                    }
                }
            }

            // 2. Deduplicate Planned Expenses
            val toDeletePlanned = mutableListOf<PlannedExpenseEntity>()
            val processedPlannedIds = mutableSetOf<Int>()
            for (i in 0 until allPlanned.size) {
                val pA = allPlanned[i]
                if (processedPlannedIds.contains(pA.id)) continue
                val normA = DateUtils.normalizeDebtOrBillName(pA.title).lowercase(Locale.US)

                for (j in i + 1 until allPlanned.size) {
                    val pB = allPlanned[j]
                    if (processedPlannedIds.contains(pB.id)) continue
                    val normB = DateUtils.normalizeDebtOrBillName(pB.title).lowercase(Locale.US)

                    if (normA == normB) {
                        val remove = if (pA.title.contains("Minimum Payment", ignoreCase = true) && !pB.title.contains("Minimum Payment", ignoreCase = true)) {
                            pA
                        } else {
                            pB
                        }
                        toDeletePlanned.add(remove)
                        processedPlannedIds.add(remove.id)
                    }
                }
            }

            // Also remove planned expenses that redundantly shadow an active recurring transaction
            for (p in allPlanned) {
                if (toDeletePlanned.any { it.id == p.id }) continue
                val normP = DateUtils.normalizeDebtOrBillName(p.title).lowercase(Locale.US)
                val hasRecurringTx = validTxs.any {
                    it.isRecurring && !it.isIncome &&
                    toDeleteTx.none { del -> del.id == it.id } &&
                    DateUtils.normalizeDebtOrBillName(it.title).lowercase(Locale.US) == normP
                }
                if (hasRecurringTx && p.title.contains("Minimum Payment", ignoreCase = true)) {
                    toDeletePlanned.add(p)
                }
            }

            // 3. Deduplicate exact duplicate debts only (same name, balance, and APR)
            val toDeleteDebts = mutableListOf<DebtEntity>()
            val processedDebtIds = mutableSetOf<Int>()
            for (i in 0 until allDebts.size) {
                val dA = allDebts[i]
                if (processedDebtIds.contains(dA.id)) continue
                for (j in i + 1 until allDebts.size) {
                    val dB = allDebts[j]
                    if (processedDebtIds.contains(dB.id)) continue
                    if (dA.name.trim().equals(dB.name.trim(), ignoreCase = true) &&
                        Math.abs(dA.balance - dB.balance) < 0.01 &&
                        Math.abs(dA.interestRate - dB.interestRate) < 0.01) {
                        toDeleteDebts.add(dB)
                        processedDebtIds.add(dB.id)
                    }
                }
            }

            for (tx in toUpdateTx) {
                repository.updateTransaction(tx)
            }
            for (tx in toDeleteTx) {
                repository.deleteTransaction(tx)
                removedCount++
            }
            for (p in toDeletePlanned) {
                repository.deletePlannedExpense(p)
                removedCount++
            }
            for (d in toDeleteDebts) {
                repository.deleteDebt(d)
                removedCount++
            }

            if (removedCount > 0) {
                autoSyncIfEnabled()
                com.example.widget.CurrentPayCycleWidgetProvider.sendUpdateBroadcast(getApplication())
            }
        } catch (e: Exception) {
            Log.e("BudgetViewModel", "Error in performDeduplication", e)
        }
        return removedCount
    }

    fun deleteDebt(debt: DebtEntity) {
        viewModelScope.launch {
            val copy = debt.copy()
            repository.deleteDebt(debt)
            val debtBillTitle = "${debt.name} (Minimum Payment)"
            val plannedList = repository.allPlannedExpenses.first()
            val existing = plannedList.firstOrNull { 
                it.title.equals(debtBillTitle, ignoreCase = true) || it.title.equals(debt.name, ignoreCase = true)
            }
            if (existing != null) {
                repository.deletePlannedExpense(existing)
            }
            pushUndoAction(
                title = "Delete Debt",
                description = "Deleted debt '${debt.name}'"
            ) {
                repository.insertDebt(copy.copy(id = 0))
                if (copy.isLinkedToBills && existing != null) {
                    repository.insertPlannedExpense(existing.copy(id = 0))
                }
            }
            autoSyncIfEnabled()
        }
    }

    fun isCreditCardCategory(category: String): Boolean {
        val trimmed = category.trim()
        if (trimmed.equals("Credit Cards", ignoreCase = true) ||
            trimmed.equals("Credit Card", ignoreCase = true) ||
            trimmed.contains("credit card", ignoreCase = true) ||
            trimmed.contains("credit cards", ignoreCase = true)
        ) {
            return true
        }
        return try {
            val (parent, _) = CategoryTheme.parseCategory(trimmed)
            parent.equals("Credit Cards", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun isCreditCardExpense(title: String, category: String): Boolean {
        if (isCreditCardCategory(category)) return true
        val lowerTitle = title.lowercase(Locale.US)
        return lowerTitle.contains("credit card") || lowerTitle.contains("credit cards") ||
                lowerTitle.contains("chase") || lowerTitle.contains("amex") ||
                lowerTitle.contains("discover") || lowerTitle.contains("capital one") ||
                lowerTitle.contains("citi") || lowerTitle.contains("apple card") ||
                lowerTitle.contains("mastercard") || lowerTitle.contains("visa") ||
                lowerTitle.contains("card")
    }

    suspend fun syncExpenseToDebtIfCreditCard(title: String, amount: Double, category: String, isIncome: Boolean = false) {
        // Safe no-op: Tracked debts are managed explicitly by the user in Debt Planner.
        // Expense and bill transactions must never overwrite tracked debts or debt balances.
    }

    fun syncAllCreditCardExpensesToDebts() {
        // Safe no-op: Tracked debts are managed explicitly by the user in Debt Planner.
        // Expenses and bills must never overwrite or delete user debt entries.
    }

    fun exportDebtsToCsv(): String {
        val debts = uiState.value.allDebts
        val sb = StringBuilder()
        sb.append("Debt Name,Balance,Interest Rate (APR),Minimum Payment,Category,Notes,Promo APR,Promo Expiry,Priority Rank,Paid Off\n")
        for (debt in debts) {
            val safeName = "\"" + debt.name.replace("\"", "\"\"") + "\""
            val balStr = String.format(Locale.US, "%.2f", debt.balance)
            val rateStr = String.format(Locale.US, "%.2f", debt.interestRate)
            val minPayStr = String.format(Locale.US, "%.2f", debt.minimumPayment)
            val safeCat = "\"" + debt.category.replace("\"", "\"\"") + "\""
            val safeNotes = "\"" + debt.notes.replace("\"", "\"\"") + "\""
            val promoAprStr = debt.promoApr?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            val promoExpStr = debt.promoExpirationDate ?: ""
            val priorityStr = if (debt.customPriorityRank > 0) debt.customPriorityRank.toString() else ""
            val paidOffStr = if (debt.isPaidOff) "TRUE" else "FALSE"
            sb.append("$safeName,$balStr,$rateStr,$minPayStr,$safeCat,$safeNotes,$promoAprStr,$promoExpStr,$priorityStr,$paidOffStr\n")
        }
        return sb.toString()
    }

    suspend fun importDebtsFromCsv(csvText: String): Pair<Boolean, String> {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (csvText.isBlank()) return@withContext Pair(false, "CSV content is empty.")
                val rows = parseCsvContent(csvText)
                if (rows.isEmpty()) return@withContext Pair(false, "No rows found in CSV.")

                val existingDebts = repository.allDebts.first()
                val usedIds = mutableSetOf<Int>()

                val firstRowStr = rows[0].joinToString(" ").lowercase(Locale.US)
                val hasHeader = firstRowStr.contains("debt") ||
                        firstRowStr.contains("balance") ||
                        firstRowStr.contains("rate") ||
                        firstRowStr.contains("apr") ||
                        firstRowStr.contains("minimum") ||
                        firstRowStr.contains("payment") ||
                        firstRowStr.contains("creditor") ||
                        firstRowStr.contains("card") ||
                        firstRowStr.contains("name")

                val startIndex = if (hasHeader) 1 else 0

                var nameIdx = -1
                var balanceIdx = -1
                var rateIdx = -1
                var minPayIdx = -1
                var categoryIdx = -1
                var notesIdx = -1
                var promoAprIdx = -1
                var promoExpIdx = -1
                var priorityIdx = -1
                var paidOffIdx = -1

                if (hasHeader) {
                    val headers = rows[0].map { it.trim().lowercase(Locale.US) }
                    headers.forEachIndexed { idx, h ->
                        when {
                            h.contains("promo apr") || h.contains("promo rate") || h.contains("intro apr") || h.contains("intro rate") -> promoAprIdx = idx
                            h.contains("promo exp") || h.contains("promo end") || h.contains("expiry") || h.contains("expiration") -> promoExpIdx = idx
                            h.contains("priority") || h.contains("rank") || h.contains("pin") -> priorityIdx = idx
                            h.contains("paid") || h.contains("settled") || h.contains("closed") -> paidOffIdx = idx
                            h.contains("debt") || h.contains("creditor") || h.contains("lender") || h.contains("card") || h.contains("name") || h.contains("title") || h.contains("item") -> if (nameIdx == -1) nameIdx = idx
                            h.contains("balance") || h.contains("owed") || h.contains("principal") || h.contains("amount") || h.contains("total") -> if (balanceIdx == -1) balanceIdx = idx
                            h.contains("rate") || h.contains("apr") || h.contains("interest") || h.contains("%") -> if (rateIdx == -1) rateIdx = idx
                            h.contains("minimum") || h.contains("min pay") || h.contains("min payment") || h.contains("payment") || h.contains("mo pay") -> if (minPayIdx == -1) minPayIdx = idx
                            h.contains("category") || h.contains("cat") || h.contains("type") -> if (categoryIdx == -1) categoryIdx = idx
                            h.contains("notes") || h.contains("note") || h.contains("memo") || h.contains("description") || h.contains("desc") -> if (notesIdx == -1) notesIdx = idx
                        }
                    }
                }

                // Positional fallback
                if (nameIdx == -1) nameIdx = 0
                if (balanceIdx == -1) balanceIdx = if (rows[0].size > 1) 1 else -1
                if (rateIdx == -1) rateIdx = if (rows[0].size > 2) 2 else -1
                if (minPayIdx == -1) minPayIdx = if (rows[0].size > 3) 3 else -1
                if (categoryIdx == -1) categoryIdx = if (rows[0].size > 4) 4 else -1
                if (notesIdx == -1) notesIdx = if (rows[0].size > 5) 5 else -1
                if (promoAprIdx == -1 && rows[0].size > 6) promoAprIdx = 6
                if (promoExpIdx == -1 && rows[0].size > 7) promoExpIdx = 7
                if (priorityIdx == -1 && rows[0].size > 8) priorityIdx = 8
                if (paidOffIdx == -1 && rows[0].size > 9) paidOffIdx = 9

                var importedCount = 0

                for (i in startIndex until rows.size) {
                    val tokens = rows[i]
                    if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

                    val rawName = if (nameIdx >= 0 && nameIdx < tokens.size) tokens[nameIdx].trim() else ""
                    val cleanName = rawName.ifBlank {
                        tokens.firstOrNull { token ->
                            val trimmed = token.trim()
                            trimmed.isNotBlank() && parseAmountHelper(trimmed).first == null
                        }?.trim() ?: "Debt #${i + 1}"
                    }

                    val rawBal = if (balanceIdx >= 0 && balanceIdx < tokens.size) tokens[balanceIdx] else ""
                    val (parsedBal, _) = parseAmountHelper(rawBal)
                    val balanceVal = parsedBal ?: tokens.firstNotNullOfOrNull { token ->
                        parseAmountHelper(token).first
                    } ?: 0.0

                    if (balanceVal <= 0.0 && cleanName.startsWith("Debt #")) continue

                    val rawRate = if (rateIdx >= 0 && rateIdx < tokens.size) tokens[rateIdx] else ""
                    val (parsedRate, _) = parseAmountHelper(rawRate)
                    val rateVal = (parsedRate ?: 18.0).coerceIn(0.0, 100.0)

                    val rawMinPay = if (minPayIdx >= 0 && minPayIdx < tokens.size) tokens[minPayIdx] else ""
                    val (parsedMinPay, _) = parseAmountHelper(rawMinPay)
                    val minPayVal = parsedMinPay ?: (balanceVal * 0.025).coerceAtLeast(25.0)

                    val rawCat = if (categoryIdx >= 0 && categoryIdx < tokens.size) tokens[categoryIdx].trim() else ""
                    val categoryVal = if (rawCat.isNotBlank()) rawCat else "Credit Card"

                    val notesVal = if (notesIdx >= 0 && notesIdx < tokens.size) tokens[notesIdx].trim() else ""

                    val promoAprVal = if (promoAprIdx >= 0 && promoAprIdx < tokens.size) {
                        val (pApr, _) = parseAmountHelper(tokens[promoAprIdx])
                        pApr
                    } else null

                    val promoExpVal = if (promoExpIdx >= 0 && promoExpIdx < tokens.size) {
                        tokens[promoExpIdx].trim().ifBlank { null }
                    } else null

                    val priorityVal = if (priorityIdx >= 0 && priorityIdx < tokens.size) {
                        tokens[priorityIdx].trim().toIntOrNull() ?: 0
                    } else 0

                    val isPaidOffVal = if (paidOffIdx >= 0 && paidOffIdx < tokens.size) {
                        val v = tokens[paidOffIdx].trim().lowercase(Locale.US)
                        v == "true" || v == "1" || v == "yes" || v == "paid"
                    } else (balanceVal <= 0.0)

                    val existing = existingDebts.firstOrNull { debt ->
                        !usedIds.contains(debt.id) && debt.name.equals(cleanName, ignoreCase = true)
                    }

                    if (existing != null) {
                        usedIds.add(existing.id)
                        repository.updateDebt(
                            existing.copy(
                                balance = balanceVal,
                                interestRate = rateVal,
                                minimumPayment = minPayVal,
                                category = categoryVal,
                                notes = notesVal,
                                promoApr = promoAprVal ?: existing.promoApr,
                                promoExpirationDate = promoExpVal ?: existing.promoExpirationDate,
                                customPriorityRank = if (priorityVal > 0) priorityVal else existing.customPriorityRank,
                                isPaidOff = isPaidOffVal
                            )
                        )
                    } else {
                        repository.insertDebt(
                            DebtEntity(
                                name = cleanName,
                                balance = balanceVal,
                                interestRate = rateVal,
                                minimumPayment = minPayVal,
                                category = categoryVal,
                                notes = notesVal,
                                promoApr = promoAprVal,
                                promoExpirationDate = promoExpVal,
                                customPriorityRank = priorityVal,
                                isPaidOff = isPaidOffVal
                            )
                        )
                    }
                    importedCount++
                }

                if (importedCount > 0) {
                    autoSyncIfEnabled()
                    Pair(true, "Successfully imported/updated $importedCount debts from Google Sheets / CSV!")
                } else {
                    Pair(false, "No valid debt records could be imported from CSV.")
                }
            } catch (e: Exception) {
                Pair(false, "Error parsing debt CSV: ${e.message}")
            }
        }
    }

    suspend fun exportToJson(): String {
        if (_isDriveAutoSyncEnabled.value && _driveAccountEmail.value != null) {
            try {
                internalPerformDriveSync()
            } catch (_: Exception) {}
        }
        return internalExportToJson()
    }

    suspend fun internalExportToJson(): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = org.json.JSONObject()

                // 1. Transactions
                val txList = repository.allTransactions.first()
                val txArray = org.json.JSONArray()
                for (tx in txList) {
                    val obj = org.json.JSONObject()
                    obj.put("title", tx.title)
                    obj.put("amount", tx.amount)
                    obj.put("dateMillis", tx.dateMillis)
                    obj.put("category", tx.category)
                    obj.put("isIncome", tx.isIncome)
                    obj.put("isRecurring", tx.isRecurring)
                    obj.put("accountName", tx.accountName)
                    obj.put("isCompleted", tx.isCompleted)
                    obj.put("isAutoPay", tx.isAutoPay)
                    obj.put("status", tx.getEffectiveStatus())
                    txArray.put(obj)
                }
                json.put("transactions", txArray)

                // 2. Settings
                val settingsArray = org.json.JSONArray()
                val keys = listOf("default_paycheck_amount", "pay_cycle_start_date", "active_account_name", "all_accounts", "is_dark_mode")
                for (key in keys) {
                    val value = repository.getSetting(key)
                    if (value != null) {
                        val obj = org.json.JSONObject()
                        obj.put("key", key)
                        obj.put("value", value)
                        settingsArray.put(obj)
                    }
                }
                json.put("settings", settingsArray)

                // 3. Income Sources
                val incList = repository.allIncomeSources.first()
                val incArray = org.json.JSONArray()
                for (inc in incList) {
                    val obj = org.json.JSONObject()
                    obj.put("name", inc.name)
                    obj.put("amount", inc.amount)
                    obj.put("frequency", inc.frequency)
                    incArray.put(obj)
                }
                json.put("income_sources", incArray)

                // 4. Planned Expenses
                val expList = repository.allPlannedExpenses.first()
                val expArray = org.json.JSONArray()
                for (exp in expList) {
                    val obj = org.json.JSONObject()
                    obj.put("title", exp.title)
                    obj.put("dueDate", exp.dueDate)
                    obj.put("minAmount", exp.minAmount)
                    obj.put("targetAccount", exp.targetAccount)
                    obj.put("isAutoPay", exp.isAutoPay)
                    obj.put("reminderDaysBefore", exp.reminderDaysBefore)
                    expArray.put(obj)
                }
                json.put("planned_expenses", expArray)

                // 5. Debts
                val debtList = repository.allDebts.first()
                val debtArray = org.json.JSONArray()
                for (debt in debtList) {
                    val obj = org.json.JSONObject()
                    obj.put("name", debt.name)
                    obj.put("balance", debt.balance)
                    obj.put("interestRate", debt.interestRate)
                    obj.put("minimumPayment", debt.minimumPayment)
                    obj.put("category", debt.category)
                    obj.put("notes", debt.notes)
                    if (debt.promoApr != null) obj.put("promoApr", debt.promoApr)
                    if (debt.promoExpirationDate != null) obj.put("promoExpirationDate", debt.promoExpirationDate)
                    if (debt.customPriorityRank > 0) obj.put("customPriorityRank", debt.customPriorityRank)
                    debtArray.put(obj)
                }
                json.put("debts", debtArray)

                json.toString(4) // human-readable styled
            } catch (e: Exception) {
                "{ \"error\": \"${e.localizedMessage}\" }"
            }
        }
    }

    suspend fun importFromJson(jsonStr: String, isRemoteNewer: Boolean = false): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val json = org.json.JSONObject(jsonStr)

                val existingTxList = repository.allTransactions.first()
                val existingIncList = repository.allIncomeSources.first()
                val existingExpList = repository.allPlannedExpenses.first()
                val existingDebtList = repository.allDebts.first()

                // Insert/merge transactions
                if (json.has("transactions")) {
                    val array = json.getJSONArray("transactions")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = TransactionEntity(
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            dateMillis = obj.getLong("dateMillis"),
                            category = obj.optString("category", if (obj.getBoolean("isIncome")) "Income" else "Expense"),
                            isIncome = obj.getBoolean("isIncome"),
                            isRecurring = obj.optBoolean("isRecurring", false),
                            accountName = obj.optString("accountName", "").let { if (it.equals("Primary Account", ignoreCase = true) || it.equals("Primary", ignoreCase = true)) "" else it },
                            isCompleted = obj.optBoolean("isCompleted", false),
                            isAutoPay = obj.optBoolean("isAutoPay", false),
                            status = obj.optString("status", if (obj.optBoolean("isCompleted", false)) "cleared" else "due")
                        )
                        val exists = existingTxList.any {
                            it.title == entity.title &&
                            Math.abs(it.amount - entity.amount) < 0.001 &&
                            it.dateMillis == entity.dateMillis &&
                            it.accountName == entity.accountName
                        }
                        if (!exists) {
                            repository.insertTransaction(entity)
                        }
                    }
                }

                // Insert Settings
                var activeAccChanged: String? = null
                var allAccsChanged: String? = null
                var isDarkChanged: Boolean? = null
                var defaultPaycheckChanged: Double? = null
                var anchorPaydayChanged: String? = null

                if (json.has("settings")) {
                    val array = json.getJSONArray("settings")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val key = obj.getString("key")
                        val value = obj.getString("value")
                        if (key != "last_local_update_time") {
                            repository.saveSetting(key, value)
                        }

                        when (key) {
                            "active_account_name" -> activeAccChanged = value
                            "all_accounts" -> allAccsChanged = value
                            "is_dark_mode" -> isDarkChanged = value.toBoolean()
                            "default_paycheck_amount" -> defaultPaycheckChanged = value.toDoubleOrNull()
                            "pay_cycle_start_date" -> anchorPaydayChanged = value
                        }
                    }
                }

                // Insert/merge Income Sources
                if (json.has("income_sources")) {
                    val array = json.getJSONArray("income_sources")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = IncomeSourceEntity(
                            name = obj.getString("name"),
                            amount = obj.getDouble("amount"),
                            frequency = obj.getString("frequency")
                        )
                        val exists = existingIncList.any {
                            it.name == entity.name &&
                            Math.abs(it.amount - entity.amount) < 0.001 &&
                            it.frequency == entity.frequency
                        }
                        if (!exists) {
                            repository.insertIncomeSource(entity)
                        }
                    }
                }

                // Insert/merge Planned Expenses
                if (json.has("planned_expenses")) {
                    val array = json.getJSONArray("planned_expenses")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val entity = PlannedExpenseEntity(
                            title = obj.getString("title"),
                            dueDate = obj.getString("dueDate"),
                            minAmount = obj.getDouble("minAmount"),
                            targetAccount = obj.optString("targetAccount", "").let { if (it.equals("Primary Account", ignoreCase = true) || it.equals("Primary", ignoreCase = true)) "" else it },
                            isAutoPay = obj.optBoolean("isAutoPay", false),
                            reminderDaysBefore = obj.optInt("reminderDaysBefore", 1)
                        )
                        val exists = existingExpList.any {
                            it.title == entity.title &&
                            it.dueDate == entity.dueDate &&
                            it.targetAccount == entity.targetAccount
                        }
                        if (!exists) {
                            repository.insertPlannedExpense(entity)
                        }
                    }
                }

                // Insert/merge Debts
                if (json.has("debts")) {
                    val array = json.getJSONArray("debts")
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        val promoAprVal = if (obj.has("promoApr") && !obj.isNull("promoApr")) obj.getDouble("promoApr") else null
                        val promoExpVal = if (obj.has("promoExpirationDate") && !obj.isNull("promoExpirationDate")) obj.getString("promoExpirationDate") else null
                        val priorityVal = if (obj.has("customPriorityRank")) obj.optInt("customPriorityRank", 0) else 0

                        val entity = DebtEntity(
                            name = obj.getString("name"),
                            balance = obj.getDouble("balance"),
                            interestRate = obj.getDouble("interestRate"),
                            minimumPayment = obj.getDouble("minimumPayment"),
                            category = obj.optString("category", "Credit Card"),
                            notes = obj.optString("notes", ""),
                            promoApr = promoAprVal,
                            promoExpirationDate = promoExpVal,
                            customPriorityRank = priorityVal
                        )
                        val exists = existingDebtList.any {
                            it.name.equals(entity.name, ignoreCase = true)
                        }
                        if (!exists) {
                            repository.insertDebt(entity)
                        } else {
                            val current = existingDebtList.firstOrNull { it.name.equals(entity.name, ignoreCase = true) }
                            if (current != null && (Math.abs(current.balance - entity.balance) > 0.01 || Math.abs(current.interestRate - entity.interestRate) > 0.01 || Math.abs(current.minimumPayment - entity.minimumPayment) > 0.01 || current.promoApr != entity.promoApr || current.promoExpirationDate != entity.promoExpirationDate || current.customPriorityRank != entity.customPriorityRank)) {
                                repository.updateDebt(current.copy(balance = entity.balance, interestRate = entity.interestRate, minimumPayment = entity.minimumPayment, category = entity.category, notes = entity.notes, promoApr = entity.promoApr, promoExpirationDate = entity.promoExpirationDate, customPriorityRank = entity.customPriorityRank))
                            }
                        }
                    }
                }

                // Update state flows on Main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    activeAccChanged?.let { _activeAccountName.value = it }
                    allAccsChanged?.let {
                        val list = it.split(",").map { item -> item.trim() }.filter { item -> item.isNotEmpty() }
                        if (list.isNotEmpty()) _allAccounts.value = list
                    }
                    isDarkChanged?.let { _isDarkMode.value = it }
                    defaultPaycheckChanged?.let { _defaultPaycheckAmount.value = it }
                    anchorPaydayChanged?.let {
                        try { _anchorPayday.value = java.time.LocalDate.parse(it) } catch(e: Exception){}
                    }
                }

                true
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun exportToCsv(): String {
        if (_isDriveAutoSyncEnabled.value && _driveAccountEmail.value != null) {
            try {
                internalPerformDriveSync()
            } catch (_: Exception) {}
        }
        return internalExportToCsv()
    }

    suspend fun internalExportToCsv(): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sb = StringBuilder()
                sb.append("Date,Title,Amount,Category,Type,Account,Completed,Recurring,Auto Pay\n")

                val txList = repository.allTransactions.first()
                val plannedList = repository.allPlannedExpenses.first()

                val df = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                val exportedSignatures = mutableSetOf<String>()

                data class CsvRowData(
                    val dateStr: String,
                    val dateMillis: Long,
                    val title: String,
                    val amount: Double,
                    val category: String,
                    val type: String,
                    val account: String,
                    val isCompleted: Boolean,
                    val isRecurring: Boolean,
                    val isAutoPay: Boolean
                )

                val rows = mutableListOf<CsvRowData>()

                for (tx in txList) {
                    val dateStr = DateUtils.millisToLocalDate(tx.dateMillis).format(df)
                    val sig = "${tx.title.lowercase()}_${String.format(java.util.Locale.US, "%.2f", tx.amount)}_${dateStr}"
                    exportedSignatures.add(sig)

                    rows.add(
                        CsvRowData(
                            dateStr = dateStr,
                            dateMillis = tx.dateMillis,
                            title = tx.title,
                            amount = tx.amount,
                            category = tx.category,
                            type = if (tx.isIncome) "Income" else "Expense",
                            account = tx.accountName,
                            isCompleted = tx.isCompleted,
                            isRecurring = tx.isRecurring,
                            isAutoPay = tx.isAutoPay
                        )
                    )
                }

                for (planned in plannedList) {
                    val parsedDate = try { LocalDate.parse(planned.dueDate) } catch (e: Exception) { LocalDate.now() }
                    val dateStr = parsedDate.format(df)
                    val dateMillis = DateUtils.localDateToMillis(parsedDate)
                    val sig = "${planned.title.lowercase()}_${String.format(java.util.Locale.US, "%.2f", planned.minAmount)}_${dateStr}"
                    if (!exportedSignatures.contains(sig)) {
                        exportedSignatures.add(sig)
                        rows.add(
                            CsvRowData(
                                dateStr = dateStr,
                                dateMillis = dateMillis,
                                title = planned.title,
                                amount = planned.minAmount,
                                category = "Bills",
                                type = "Expense",
                                account = planned.targetAccount,
                                isCompleted = false,
                                isRecurring = false,
                                isAutoPay = planned.isAutoPay
                            )
                        )
                    }
                }

                val sortedRows = rows.sortedByDescending { it.dateMillis }

                for (row in sortedRows) {
                    val safeTitle = "\"" + row.title.replace("\"", "\"\"") + "\""
                    val amountStr = String.format(java.util.Locale.US, "%.2f", row.amount)
                    val safeCategory = "\"" + row.category.replace("\"", "\"\"") + "\""
                    val autoPayStr = if (row.isAutoPay) "Yes" else "No"
                    val safeAccount = "\"" + row.account.replace("\"", "\"\"") + "\""
                    val completedStr = if (row.isCompleted) "Yes" else "No"
                    val recurringStr = if (row.isRecurring) "Yes" else "No"

                    sb.append("${row.dateStr},$safeTitle,$amountStr,$safeCategory,${row.type},$safeAccount,$completedStr,$recurringStr,$autoPayStr\n")
                }

                sb.toString()
            } catch (e: Exception) {
                ""
            }
        }
    }

    private fun parseDateFlexible(dateStr: String): LocalDate {
        val clean = dateStr.trim().removeSurrounding("\"").removeSurrounding("'")
        if (clean.isBlank()) return LocalDate.now()

        try { return LocalDate.parse(clean) } catch (_: Exception) {}

        try {
            if (clean.contains("T") || clean.contains(" ")) {
                val part = clean.split("T", " ")[0]
                return LocalDate.parse(part)
            }
        } catch (_: Exception) {}

        val num = clean.toDoubleOrNull()
        if (num != null && num > 30000 && num < 70000) {
            try {
                val epochDay = (num.toLong() - 25569)
                return LocalDate.ofEpochDay(epochDay)
            } catch (_: Exception) {}
        }

        val patterns = listOf(
            "MM/dd/yyyy", "M/d/yyyy", "M/d/yy", "MM/dd/yy",
            "yyyy/MM/dd", "yyyy/M/d", "yyyy.MM.dd", "dd.MM.yyyy", "d.M.yyyy",
            "MM-dd-yyyy", "M-d-yyyy", "yyyy-M-d",
            "dd/MM/yyyy", "d/M/yyyy",
            "dd-MM-yyyy", "d-M-yyyy",
            "MMM d, yyyy", "MMM dd, yyyy", "MMMM d, yyyy", "MMMM dd, yyyy",
            "d MMM yyyy", "dd MMM yyyy", "d-MMM-yyyy", "dd-MMM-yyyy"
        )

        for (pattern in patterns) {
            try {
                val formatter = DateTimeFormatter.ofPattern(pattern, Locale.US)
                return LocalDate.parse(clean, formatter)
            } catch (_: Exception) {}
        }

        return LocalDate.now()
    }

    private fun parseCsvContent(csvText: String): List<List<String>> {
        if (csvText.isBlank()) return emptyList()

        val nonBlankLines = csvText.lines().filter { it.isNotBlank() }
        val sampleText = nonBlankLines.take(5).joinToString("\n")
        val tabCount = sampleText.count { it == '\t' }
        val semiCount = sampleText.count { it == ';' }
        val commaCount = sampleText.count { it == ',' }

        val delimiter = when {
            tabCount > 0 && tabCount >= commaCount && tabCount >= semiCount -> '\t'
            semiCount > 0 && semiCount >= commaCount && semiCount >= tabCount -> ';'
            else -> ','
        }

        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentCell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csvText.length) {
            val c = csvText[i]
            if (c == '"') {
                if (inQuotes && i + 1 < csvText.length && csvText[i + 1] == '"') {
                    currentCell.append('"')
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                currentRow.add(currentCell.toString().trim())
                currentCell.clear()
            } else if ((c == '\n' || c == '\r') && !inQuotes) {
                if (c == '\r' && i + 1 < csvText.length && csvText[i + 1] == '\n') {
                    i++
                }
                currentRow.add(currentCell.toString().trim())
                currentCell.clear()
                if (currentRow.any { it.isNotBlank() }) {
                    rows.add(ArrayList(currentRow))
                }
                currentRow.clear()
            } else {
                currentCell.append(c)
            }
            i++
        }
        if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentCell.toString().trim())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow)
            }
        }
        return rows
    }

    private fun parseAmountHelper(rawStr: String): Pair<Double?, Boolean> {
        if (rawStr.isBlank()) return Pair(null, false)
        val trimmed = rawStr.trim().removeSurrounding("\"").removeSurrounding("'").trim()
        if (trimmed.isBlank()) return Pair(null, false)

        // Reject strings that are dates or timestamps
        if (trimmed.contains("/") ||
            trimmed.matches(Regex(".*\\d{2,4}[-/. ]\\d{1,2}[-/. ]\\d{1,4}.*")) ||
            trimmed.contains("T00:00") || trimmed.contains(" 00:00")
        ) {
            return Pair(null, false)
        }

        var clean = trimmed

        val isNegative = clean.startsWith("-") ||
                (clean.startsWith("(") && clean.endsWith(")")) ||
                clean.endsWith("-") ||
                clean.contains("debit", ignoreCase = true) ||
                clean.contains("dr", ignoreCase = true) ||
                clean.contains("withdrawal", ignoreCase = true) ||
                clean.contains("outflow", ignoreCase = true)

        clean = clean.replace("$", "").replace("€", "").replace("£", "").replace("¥", "").replace("₹", "")
            .replace("USD", "", ignoreCase = true)
            .replace("EUR", "", ignoreCase = true)
            .replace("CAD", "", ignoreCase = true)
            .replace("DR", "", ignoreCase = true)
            .replace("CR", "", ignoreCase = true)
            .replace("DB", "", ignoreCase = true)
            .trim()

        clean = clean.replace(Regex("[^0-9.,-]"), "")
        if (clean.isBlank()) return Pair(null, false)

        if (clean.contains(",") && clean.contains(".")) {
            if (clean.lastIndexOf(",") > clean.lastIndexOf(".")) {
                clean = clean.replace(".", "").replace(",", ".")
            } else {
                clean = clean.replace(",", "")
            }
        } else if (clean.contains(",")) {
            val parts = clean.split(",")
            if (parts.size == 2 && parts[1].length == 2) {
                clean = clean.replace(",", ".")
            } else {
                clean = clean.replace(",", "")
            }
        }

        val valDouble = clean.toDoubleOrNull() ?: return Pair(null, false)
        val absVal = Math.abs(valDouble)
        return Pair(absVal, isNegative || valDouble < 0)
    }

    suspend fun importFromCsv(csvText: String): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (csvText.isBlank()) return@withContext false
                val rows = parseCsvContent(csvText)
                if (rows.isEmpty()) return@withContext false

                val existingTxList = repository.allTransactions.first()
                val existingPlannedList = repository.allPlannedExpenses.first()

                val usedExistingTxIds = mutableSetOf<Int>()
                val usedExistingPlannedIds = mutableSetOf<Int>()

                val firstRowStr = rows[0].joinToString(" ").lowercase(Locale.US)
                val firstRowHasHeaderNames = firstRowStr.contains("date") ||
                        firstRowStr.contains("title") ||
                        firstRowStr.contains("amount") ||
                        firstRowStr.contains("category") ||
                        firstRowStr.contains("description") ||
                        firstRowStr.contains("payee") ||
                        firstRowStr.contains("debit") ||
                        firstRowStr.contains("credit")

                val firstRowHasNumericData = rows[0].any { token ->
                    val (amt, _) = parseAmountHelper(token)
                    amt != null && amt > 0
                }

                val hasHeader = firstRowHasHeaderNames && !firstRowHasNumericData
                val startIndex = if (hasHeader) 1 else 0

                var dateIdx = -1
                var titleIdx = -1
                var amountIdx = -1
                var categoryIdx = -1
                var autoPayIdx = -1
                var typeIdx = -1
                var accountIdx = -1
                var completedIdx = -1
                var recurringIdx = -1
                var debitIdx = -1
                var creditIdx = -1

                if (hasHeader) {
                    val headers = rows[0].map { it.trim().lowercase(Locale.US) }
                    headers.forEachIndexed { idx, h ->
                        when {
                            h.contains("debit") || h.contains("spent") || h.contains("withdrawal") || h.contains("charge") || h.contains("outflow") || h == "dr" -> debitIdx = idx
                            h.contains("credit") || h.contains("deposit") || h.contains("received") || h.contains("inflow") || h == "cr" -> creditIdx = idx
                            h.contains("auto pay") || h.contains("autopay") || h.contains("auto") -> autoPayIdx = idx
                            h.contains("type") || h.contains("income/expense") || h.contains("kind") || h.contains("transaction type") -> typeIdx = idx
                            h.contains("date") || h.contains("time") || h.contains("when") || h.contains("day") || h.contains("posted") || h.contains("trans date") -> dateIdx = idx
                            h.contains("title") || h.contains("description") || h.contains("desc") || h.contains("name") || h.contains("item") || h.contains("payee") || h.contains("merchant") || h.contains("vendor") || h.contains("memo") || h.contains("details") || h.contains("narrative") || h.contains("particulars") -> if (titleIdx == -1) titleIdx = idx
                            h.contains("amount") || h.contains("amt") || h.contains("cost") || h.contains("price") || h.contains("val") || h.contains("total") || h.contains("sum") || h.contains("expense") || h.contains("net") -> if (amountIdx == -1) amountIdx = idx
                            h.contains("category") || h.contains("cat") || h.contains("tag") || h.contains("group") || h.contains("class") || h.contains("label") -> categoryIdx = idx
                            h.contains("account") || h.contains("ledger") || h.contains("bank") || h.contains("wallet") -> accountIdx = idx
                            h.contains("completed") || h.contains("status") || h.contains("cleared") || h.contains("paid") || h.contains("done") -> completedIdx = idx
                            h.contains("recurring") || h.contains("recur") || h.contains("repeat") -> recurringIdx = idx
                        }
                    }
                }

                var validRowsProcessed = 0
                var importedCount = 0
                val importedDates = mutableListOf<LocalDate>()
                val newAccounts = mutableSetOf<String>()

                for (i in startIndex until rows.size) {
                    val tokens = rows[i]
                    if (tokens.isEmpty() || tokens.all { it.isBlank() }) continue

                    // Accurate check for duplicate header rows
                    val isHeaderRow = tokens.any { token ->
                        val lower = token.trim().lowercase(Locale.US)
                        lower == "date" || lower == "transaction date" || lower == "amount" || lower == "description" || lower == "category" || lower == "debit" || lower == "credit"
                    } && tokens.none { token ->
                        val (amt, _) = parseAmountHelper(token)
                        amt != null && amt > 0
                    }

                    if (isHeaderRow) continue

                    // Resolve Amount
                    var parsedAmount: Double? = null
                    var isAmountNegative = false
                    var isDebitCreditAmount = false
                    var debitCreditIsIncome = false

                    if (debitIdx >= 0 || creditIdx >= 0) {
                        val debitStr = if (debitIdx >= 0 && debitIdx < tokens.size) tokens[debitIdx] else ""
                        val creditStr = if (creditIdx >= 0 && creditIdx < tokens.size) tokens[creditIdx] else ""

                        val (debitAmt, _) = parseAmountHelper(debitStr)
                        val (creditAmt, _) = parseAmountHelper(creditStr)

                        if (creditAmt != null && creditAmt > 0) {
                            parsedAmount = creditAmt
                            isDebitCreditAmount = true
                            debitCreditIsIncome = true
                        } else if (debitAmt != null && debitAmt > 0) {
                            parsedAmount = debitAmt
                            isDebitCreditAmount = true
                            debitCreditIsIncome = false
                        }
                    }

                    if (!isDebitCreditAmount) {
                        val rawAmount = if (amountIdx >= 0 && amountIdx < tokens.size && tokens[amountIdx].isNotBlank()) tokens[amountIdx]
                                        else tokens.firstNotNullOfOrNull { token ->
                                            val (amt, _) = parseAmountHelper(token)
                                            if (amt != null && amt > 0) token else null
                                        } ?: ""

                        val (amtVal, isNeg) = parseAmountHelper(rawAmount)
                        parsedAmount = amtVal
                        isAmountNegative = isNeg
                    }

                    if (parsedAmount == null || parsedAmount <= 0) continue

                    validRowsProcessed++

                    // Resolve Title
                    val title = when {
                        titleIdx >= 0 && titleIdx < tokens.size && tokens[titleIdx].isNotBlank() -> tokens[titleIdx].trim()
                        else -> {
                            tokens.firstOrNull { token ->
                                val trimmedToken = token.trim()
                                if (trimmedToken.isBlank()) return@firstOrNull false
                                val (amt, _) = parseAmountHelper(trimmedToken)
                                if (amt != null) return@firstOrNull false
                                if (dateIdx >= 0 && dateIdx < tokens.size && tokens[dateIdx].trim() == trimmedToken) return@firstOrNull false
                                true
                            }?.trim() ?: "Imported Item"
                        }
                    }

                    val typeStr = if (typeIdx >= 0 && typeIdx < tokens.size) tokens[typeIdx].trim() else ""
                    val rawCategory = if (categoryIdx >= 0 && categoryIdx < tokens.size) tokens[categoryIdx].trim() else ""

                    val isExplicitIncomeType = typeStr.equals("Income", ignoreCase = true) ||
                            typeStr.equals("Deposit", ignoreCase = true) ||
                            typeStr.equals("Credit", ignoreCase = true) ||
                            typeStr.equals("Earnings", ignoreCase = true) ||
                            typeStr.equals("Salary", ignoreCase = true) ||
                            typeStr.equals("Paycheck", ignoreCase = true) ||
                            typeStr.equals("Refund", ignoreCase = true)

                    val isExplicitExpenseType = typeStr.equals("Expense", ignoreCase = true) ||
                            typeStr.equals("Debit", ignoreCase = true) ||
                            typeStr.equals("Payment", ignoreCase = true) ||
                            typeStr.equals("Purchase", ignoreCase = true) ||
                            typeStr.equals("Withdrawal", ignoreCase = true) ||
                            typeStr.equals("Bill", ignoreCase = true) ||
                            typeStr.equals("Fee", ignoreCase = true) ||
                            typeStr.equals("Cost", ignoreCase = true) ||
                            typeStr.equals("Out", ignoreCase = true) ||
                            typeStr.equals("Charge", ignoreCase = true)

                    val lowerCat = rawCategory.lowercase(Locale.US)
                    val isIncomeCategory = lowerCat == "income" || lowerCat == "paycheck" || lowerCat == "salary" || lowerCat == "bonus" || lowerCat == "deposit" || lowerCat == "interest" || lowerCat == "dividend" || lowerCat == "freelance" || lowerCat == "investment" || lowerCat == "investments"
                    val isExpenseCategory = lowerCat in setOf("housing", "utilities", "groceries", "dining out", "dining", "food", "transportation", "insurance", "entertainment", "shopping", "other", "bills", "planned expense", "expense", "medical", "healthcare", "travel", "personal", "education", "subscription", "subscriptions", "taxes", "vehicle", "gas", "clothing")

                    val lowerTitle = title.lowercase(Locale.US)
                    val isIncomeTitle = lowerTitle.contains("paycheck") || lowerTitle.contains("salary") || lowerTitle.contains("payroll") || lowerTitle.contains("direct deposit") || lowerTitle.contains("bonus") || lowerTitle.contains("dividend")

                    val isIncome = when {
                        isDebitCreditAmount -> debitCreditIsIncome
                        isExplicitIncomeType -> true
                        isExplicitExpenseType -> false
                        isIncomeCategory -> true
                        isExpenseCategory -> false
                        isIncomeTitle -> true
                        isAmountNegative -> false
                        else -> false
                    }

                    val categoryVal = when {
                        rawCategory.isNotBlank() -> rawCategory
                        isIncome -> "Income"
                        else -> "Expense"
                    }

                    val autoPayVal = if (autoPayIdx >= 0 && autoPayIdx < tokens.size) tokens[autoPayIdx].trim() else ""
                    val isAutoPay = autoPayVal.equals("Yes", ignoreCase = true) || autoPayVal.equals("true", ignoreCase = true) || autoPayVal.equals("Auto", ignoreCase = true) || autoPayVal.contains("⚡")

                    val rawAccount = if (accountIdx >= 0 && accountIdx < tokens.size && tokens[accountIdx].isNotBlank()) tokens[accountIdx].trim() else ""
                    val accountName = if (rawAccount.equals("Primary Account", ignoreCase = true) || rawAccount.equals("Primary", ignoreCase = true)) {
                        _allAccounts.value.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
                    } else rawAccount

                    val completedStr = if (completedIdx >= 0 && completedIdx < tokens.size) tokens[completedIdx].trim() else ""
                    val isCompleted = completedStr.equals("Yes", ignoreCase = true) || completedStr.equals("true", ignoreCase = true) || completedStr.equals("Cleared", ignoreCase = true) || completedStr.equals("Paid", ignoreCase = true) || completedStr.equals("Done", ignoreCase = true)
                    val parsedStatus = when (completedStr.lowercase()) {
                        "due" -> "due"
                        "pending" -> "pending"
                        "paid" -> "paid"
                        "cleared" -> "cleared"
                        "yes", "true", "done" -> "cleared"
                        else -> if (isCompleted) "cleared" else "due"
                    }

                    val recurringStr = if (recurringIdx >= 0 && recurringIdx < tokens.size) tokens[recurringIdx].trim() else ""
                    val isRecurring = recurringStr.equals("Yes", ignoreCase = true) || recurringStr.equals("true", ignoreCase = true)

                    // Resolve Date
                    var parsedDate: LocalDate? = null
                    if (dateIdx >= 0 && dateIdx < tokens.size && tokens[dateIdx].isNotBlank()) {
                        val dStr = tokens[dateIdx].trim()
                        val d = parseDateFlexible(dStr)
                        if (dStr.isNotBlank() && (d != LocalDate.now() || dStr.contains("20") || dStr.contains("19") || dStr.contains("/"))) {
                            parsedDate = d
                        }
                    }
                    if (parsedDate == null) {
                        for (token in tokens) {
                            val trimmedToken = token.trim()
                            if (trimmedToken.isNotBlank() && (trimmedToken.contains("/") || trimmedToken.contains("-") || trimmedToken.contains("."))) {
                                val (amtCheck, _) = parseAmountHelper(trimmedToken)
                                if (amtCheck == null) {
                                    val d = parseDateFlexible(trimmedToken)
                                    if (d != LocalDate.now() || trimmedToken.contains("20") || trimmedToken.contains("19")) {
                                        parsedDate = d
                                        break
                                    }
                                }
                            }
                        }
                    }
                    val finalDate = parsedDate ?: LocalDate.now()
                    importedDates.add(finalDate)

                    if (accountName.isNotBlank() && accountName != "All Accounts") {
                        newAccounts.add(accountName)
                    }

                    val dateMillis = DateUtils.localDateToMillis(finalDate)

                    val entity = TransactionEntity(
                        title = title,
                        amount = parsedAmount,
                        dateMillis = dateMillis,
                        category = categoryVal,
                        isIncome = isIncome,
                        isRecurring = isRecurring,
                        accountName = accountName,
                        isCompleted = isCompleted,
                        isAutoPay = isAutoPay,
                        status = parsedStatus
                    )

                    val existing = existingTxList.firstOrNull { tx ->
                        !usedExistingTxIds.contains(tx.id) &&
                        tx.title.equals(entity.title, ignoreCase = true) &&
                        tx.dateMillis == entity.dateMillis &&
                        tx.accountName.equals(entity.accountName, ignoreCase = true)
                    } ?: existingTxList.firstOrNull { tx ->
                        !usedExistingTxIds.contains(tx.id) &&
                        tx.title.equals(entity.title, ignoreCase = true) &&
                        Math.abs(tx.amount - entity.amount) < 0.01 &&
                        tx.dateMillis == entity.dateMillis
                    }

                    if (existing != null) {
                        usedExistingTxIds.add(existing.id)
                        if (Math.abs(existing.amount - entity.amount) >= 0.01 ||
                            existing.isCompleted != entity.isCompleted ||
                            existing.isAutoPay != entity.isAutoPay ||
                            existing.isIncome != entity.isIncome ||
                            existing.isRecurring != entity.isRecurring ||
                            existing.category != entity.category) {
                            repository.updateTransaction(
                                existing.copy(
                                    amount = entity.amount,
                                    isCompleted = entity.isCompleted,
                                    isAutoPay = entity.isAutoPay,
                                    category = entity.category,
                                    isIncome = entity.isIncome,
                                    isRecurring = entity.isRecurring,
                                    accountName = entity.accountName
                                )
                            )
                            importedCount++
                        }
                    } else {
                        repository.insertTransaction(entity)
                        importedCount++
                    }

                    if (categoryVal.equals("Bills", ignoreCase = true) || categoryVal.equals("Planned Expense", ignoreCase = true)) {
                        val dueDateStr = finalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        val existingPlanned = existingPlannedList.firstOrNull { p ->
                            !usedExistingPlannedIds.contains(p.id) &&
                            p.title.equals(entity.title, ignoreCase = true) &&
                            p.dueDate == dueDateStr
                        }
                        if (existingPlanned != null) {
                            usedExistingPlannedIds.add(existingPlanned.id)
                            if (Math.abs(existingPlanned.minAmount - entity.amount) >= 0.01 || existingPlanned.isAutoPay != entity.isAutoPay) {
                                repository.updatePlannedExpense(
                                    existingPlanned.copy(
                                        minAmount = entity.amount,
                                        targetAccount = entity.accountName,
                                        isAutoPay = entity.isAutoPay
                                    )
                                )
                            }
                        } else {
                            repository.insertPlannedExpense(
                                PlannedExpenseEntity(
                                    title = entity.title,
                                    dueDate = dueDateStr,
                                    minAmount = entity.amount,
                                    targetAccount = entity.accountName,
                                    isAutoPay = entity.isAutoPay
                                )
                            )
                        }
                    }
                }

                val currentAccounts = _allAccounts.value.toMutableList()
                var updatedAccountList = false
                newAccounts.forEach { acc ->
                    if (!currentAccounts.contains(acc)) {
                        currentAccounts.add(acc)
                        updatedAccountList = true
                    }
                }
                if (updatedAccountList) {
                    _allAccounts.value = currentAccounts
                    try {
                        repository.saveSetting("all_accounts", currentAccounts.joinToString(","))
                    } catch (_: Exception) {}
                }

                if (importedDates.isNotEmpty()) {
                    val latestDate = importedDates.maxOrNull() ?: LocalDate.now()
                    _selectedMonth.value = YearMonth.from(latestDate)
                }

                if (importedCount > 0) {
                    markLocalDataChanged()
                }
                autoSyncIfEnabled()

                (validRowsProcessed > 0)
            } catch (e: Exception) {
                Log.e("BudgetViewModel", "Error importing CSV data", e)
                false
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        return parseCsvContent(line).firstOrNull() ?: emptyList()
    }

    fun performCsvDriveSync(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _syncState.value = com.example.sync.SyncState.Syncing
            val accountEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"
            val nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.US))

            try {
                internalPerformDriveSync()
            } catch (_: Exception) {}

            val (success, statusMsg) = driveSyncManager.syncCsvToDriveOrCloud(
                overrideAccountEmail = accountEmail,
                csvData = internalExportToCsv(),
                onRemoteCsvDownloaded = { remoteCsv ->
                    importFromCsv(remoteCsv)
                    internalExportToCsv()
                }
            )

            if (success) {
                autoSyncIfEnabled()
                _syncState.value = com.example.sync.SyncState.Success(statusMsg, nowStr)
                _lastDriveSyncTime.value = nowStr
                repository.saveSetting("last_drive_sync_time", nowStr)
            } else {
                _syncState.value = com.example.sync.SyncState.Error(statusMsg)
            }
            onComplete?.invoke(statusMsg)
        }
    }

    fun performDebtDriveSync(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _syncState.value = com.example.sync.SyncState.Syncing
            val accountEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"
            val nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.US))

            try {
                internalPerformDriveSync()
            } catch (_: Exception) {}

            val currentDebtsCsv = exportDebtsToCsv()
            val header = "Debt Name,Balance,Interest Rate (APR),Minimum Payment,Category,Notes\n"
            val payloadCsv = if (currentDebtsCsv.trim().lines().size > 1) currentDebtsCsv else header

            val (success, statusMsg) = driveSyncManager.syncDebtsToDriveOrCloud(
                overrideAccountEmail = accountEmail,
                debtCsvData = payloadCsv,
                onRemoteDebtCsvDownloaded = { remoteCsv ->
                    importDebtsFromCsv(remoteCsv)
                }
            )

            if (success) {
                autoSyncIfEnabled()
                _syncState.value = com.example.sync.SyncState.Success(statusMsg, nowStr)
                _lastDriveSyncTime.value = nowStr
                repository.saveSetting("last_drive_sync_time", nowStr)
            } else {
                _syncState.value = com.example.sync.SyncState.Error(statusMsg)
            }
            onComplete?.invoke(statusMsg)
        }
    }

    fun performExportToGoogleSheets(onComplete: ((String) -> Unit)? = null) {
        viewModelScope.launch {
            _syncState.value = com.example.sync.SyncState.Syncing
            val accountEmail = _driveAccountEmail.value ?: "86brancan@gmail.com"

            try {
                internalPerformDriveSync()
            } catch (_: Exception) {}

            val csvData = internalExportToCsv()
            val nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", java.util.Locale.US))

            val (success, statusMsg) = driveSyncManager.exportToGoogleDriveSheet(
                overrideAccountEmail = accountEmail,
                csvData = csvData
            )

            if (success) {
                _syncState.value = com.example.sync.SyncState.Success(statusMsg, nowStr)
                _lastDriveSyncTime.value = nowStr
                repository.saveSetting("last_drive_sync_time", nowStr)
            } else {
                _syncState.value = com.example.sync.SyncState.Error(statusMsg)
            }
            onComplete?.invoke(statusMsg)
        }
    }

    suspend fun exportWebCompanionToGoogleDrive(htmlContent: String): Pair<Boolean, String> {
        val email = _driveAccountEmail.value
        return driveSyncManager.uploadOrOverwriteWebCompanionHtml(email, htmlContent)
    }
}

data class BudgetUiState(
    val selectedMonth: YearMonth,
    val anchorPayday: LocalDate,
    val defaultPaycheckAmount: Double,
    val payPeriods: List<PayPeriod>,
    val futurePayPeriods: List<PayPeriod> = emptyList(),
    val allCalculatedPayPeriods: List<PayPeriod> = emptyList(),
    val monthlyTotalIncome: Double,
    val monthlyTotalExpenses: Double,
    val monthlyRemainingBalance: Double,
    val incomeSources: List<IncomeSourceEntity> = emptyList(),
    val plannedExpenses: List<PlannedExpenseEntity> = emptyList(),
    val allDebts: List<DebtEntity> = emptyList(),
    val allDebtPayments: List<DebtPaymentEntity> = emptyList(),
    val activeAccountName: String = "All Accounts",
    val allAccounts: List<String> = emptyList(),
    val accountBalances: Map<String, AccountBalance> = emptyMap(),
    val allTransactions: List<TransactionEntity> = emptyList(),
    val isDarkMode: Boolean = false,
    val appTheme: AppTheme = AppTheme.EREADER,
    val appIcon: AppIcon = AppIcon.DEFAULT,
    val syncState: com.example.sync.SyncState = com.example.sync.SyncState.Idle,
    val driveAccountEmail: String? = null,
    val lastDriveSyncTime: String? = null,
    val isDriveAutoSyncEnabled: Boolean = true,
    val currentPayWeekStart: LocalDate = LocalDate.now(),
    val currentPayWeekEnd: LocalDate = LocalDate.now(),
    val currentPayPeriodStart: LocalDate = LocalDate.now(),
    val currentPayPeriodEnd: LocalDate = LocalDate.now(),
    val canUndo: Boolean = false,
    val lastUndoDescription: String? = null
) {
    val debts: List<DebtEntity> get() = allDebts
    val currentPayday: LocalDate get() = currentPayPeriodStart
    val currentPayPeriod: PayPeriod?
        get() = payPeriods.firstOrNull { it.startDate == currentPayPeriodStart } ?: payPeriods.firstOrNull()
}

data class UndoActionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val onUndo: suspend () -> Unit
)

data class AccountBalance(
    val accountName: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val remainingBalance: Double,
    val carryoverFromPrevious: Double = 0.0,
    val payPeriodIncome: Double = totalIncome,
    val payPeriodExpenses: Double = totalExpenses,
    val payPeriodRemaining: Double = remainingBalance,
    val monthlyTotalIncome: Double = totalIncome,
    val monthlyTotalExpenses: Double = totalExpenses,
    val monthlyRemainingBalance: Double = remainingBalance,
    val completedExpenses: Double = 0.0,
    val pendingExpenses: Double = 0.0,
    val clearedBalance: Double = remainingBalance
)
