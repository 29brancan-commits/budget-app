package com.example

import android.Manifest
import android.content.Intent
import android.widget.Toast
import android.content.Context
import android.app.NotificationManager
import com.example.sync.*
import com.example.notification.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.automirrored.filled.Undo
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.ui.MonthlySummaryView
import com.example.ui.MonthlySummaryDashboardCard
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.BudgetUiState
import com.example.ui.AiNaturalLanguageQuickEntryDialog
import com.example.ui.AiReceiptScannerDialog
import com.example.ui.AiFinancialHubView
import com.example.ui.BudgetViewModel
import com.example.ui.TransactionStatusChip
import com.example.ui.TxStatus
import com.example.ui.LogDebtPaymentDialog
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppIcon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.example.ui.SwipeableTransactionRow
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun DatePickerTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    fun openDatePicker() {
        val parsedDate = try {
            LocalDate.parse(value.trim())
        } catch (e: Exception) {
            LocalDate.now()
        }

        val datePickerDialog = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                onValueChange(selected.toString())
            },
            parsedDate.year,
            parsedDate.monthValue - 1,
            parsedDate.dayOfMonth
        )
        datePickerDialog.show()
    }

    val displayValue = remember(value) {
        try {
            val parsed = LocalDate.parse(value.trim())
            parsed.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        } catch (e: Exception) {
            if (value.isNotEmpty()) value else ""
        }
    }

    Column(modifier = modifier) {
        if (label != null) {
            CompositionLocalProvider(
                LocalContentColor provides (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
            ) {
                ProvideTextStyle(
                    androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
                ) {
                    label()
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clickable { openDatePicker() }
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
            shape = RoundedCornerShape(6.dp),
            color = if (isDark) Color(0xFF09090B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (displayValue.isNotEmpty()) displayValue else "YYYY-MM-DD",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (displayValue.isNotEmpty()) {
                        if (isDark) Color.White else Color(0xFF0F172A)
                    } else {
                        if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.size(16.dp),
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("DebtFlow", "Intercepted uncaught exception in ${thread.name}: ${throwable.message}", throwable)
                try {
                    getSharedPreferences("app_crash_logs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("last_crash", "${throwable.javaClass.simpleName}: ${throwable.message}")
                        .apply()
                } catch (_: Exception) {}
                defaultHandler?.uncaughtException(thread, throwable)
            }
        } catch (_: Exception) {}

        try {
            BillReminderScheduler.createNotificationChannel(applicationContext)
            NotificationReliabilityHelper.startKeepAliveServiceIfEnabled(applicationContext)
            NotificationReliabilityHelper.schedulePeriodicWork(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error initializing notification reliability subsystem", e)
        }

        setContent {
            val viewModel: BudgetViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            MyApplicationTheme(
                darkTheme = uiState.isDarkMode,
                appTheme = uiState.appTheme
            ) {
                BudgetAppMain(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

// Global Category Emojis & Colors
object CategoryTheme {
    val expenseCategories = listOf(
        CategoryItem("Core Bills", "📋", Color(0xFF3B82F6)),
        CategoryItem("Credit Cards", "💳", Color(0xFF8B5CF6))
    )

    fun formatCategory(parentGroup: String, subCategory: String = ""): String {
        return if (parentGroup.contains("Credit", ignoreCase = true) || parentGroup.contains("Card", ignoreCase = true)) {
            "Credit Cards"
        } else {
            "Core Bills"
        }
    }

    fun parseCategory(categoryStr: String): Pair<String, String> {
        val cat = categoryStr.trim()
        val lower = cat.lowercase(Locale.US)
        val normalized = when {
            lower.contains("credit card") || lower.contains("card") || lower.contains("chase") ||
                    lower.contains("amex") || lower.contains("discover") || lower.contains("capital one") ||
                    lower.contains("citi") || lower.contains("apple card") -> "Credit Cards"
            else -> "Core Bills"
        }
        return Pair(normalized, normalized)
    }

    fun getEmoji(categoryName: String): String {
        if (categoryName == "Paycheck") return "💰"
        if (categoryName == "Income") return "💵"
        val (parent, _) = parseCategory(categoryName)
        return if (parent.equals("Credit Cards", ignoreCase = true)) "💳" else "📋"
    }

    fun getColor(categoryName: String): Color {
        if (categoryName == "Paycheck") return Color(0xFF10B981)
        if (categoryName == "Income") return Color(0xFF34D399)
        val (parent, _) = parseCategory(categoryName)
        return if (parent.equals("Credit Cards", ignoreCase = true)) Color(0xFF8B5CF6) else Color(0xFF3B82F6)
    }
}

data class CategoryItem(val name: String, val emoji: String, val color: Color)

@Composable
fun ExpenseCategorySubCategoryPicker(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val (parsedCategory, _) = remember(selectedCategory) {
        CategoryTheme.parseCategory(selectedCategory)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Row 1: Header + Active Category display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXPENSE CATEGORY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val currentEmoji = CategoryTheme.getEmoji(parsedCategory)
            val currentColor = CategoryTheme.getColor(parsedCategory)
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = currentColor.copy(alpha = if (isDark) 0.25f else 0.12f),
                border = BorderStroke(1.dp, currentColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "$currentEmoji $parsedCategory",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = currentColor,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Row 2: Only two categories: "Core Bills" and "Credit Cards"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = listOf(
                Triple("Core Bills", "📋", Color(0xFF3B82F6)),
                Triple("Credit Cards", "💳", Color(0xFF8B5CF6))
            )
            categories.forEach { (name, emoji, color) ->
                val isSelected = parsedCategory.equals(name, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        if (isDark) color.copy(alpha = 0.35f) else color.copy(alpha = 0.18f)
                    } else (if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else (if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onCategorySelected(name)
                        }
                        .testTag("expense_cat_${name.lowercase().replace(" ", "_")}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(emoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = name,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) (if (isDark) Color.White else color) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetAppMain(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val localContext = androidx.compose.ui.platform.LocalContext.current

    val consentLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.handleAuthIntentApproved()
        }
    }

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                viewModel.handleGoogleSignInResult(account)
            } catch (_: Exception) {
                val lastAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(localContext)
                if (lastAccount != null) {
                    viewModel.handleGoogleSignInResult(lastAccount)
                } else {
                    viewModel.handleAuthIntentApproved()
                }
            }
        } else {
            val lastAccount = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(localContext)
            if (lastAccount != null) {
                viewModel.handleGoogleSignInResult(lastAccount)
            } else {
                viewModel.connectDriveAccountWithEmail("86brancan@gmail.com")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pendingAuthIntentFlow.collect { intent ->
            try {
                consentLauncher.launch(intent)
            } catch (_: Exception) {}
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Cycles, 1 = Income, 2 = Expenses, 3 = Upcoming, 4 = Debt, 5 = Calendar, 6 = AI Hub, 7 = Settings, 8 = Monthly Summary

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlannedBillsDialog by remember { mutableStateOf(false) }
    var showAiQuickEntryDialog by remember { mutableStateOf(false) }
    var showAiReceiptScannerDialog by remember { mutableStateOf(false) }
    var showGlobalTransferModal by remember { mutableStateOf(false) }
    var showGlobalReconcileModal by remember { mutableStateOf(false) }
    var preselectedDateForAdd by remember { mutableStateOf<LocalDate?>(null) }
    var showPaycheckEditDialog by remember { mutableStateOf<LocalDate?>(null) }
    var editingPaycheckAmount by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deleteConfirmationTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToConfirmComplete by remember { mutableStateOf<TransactionEntity?>(null) }
    var notificationBillIdToCancel by remember { mutableIntStateOf(0) }

    val handleRequestToggleCompleted: (TransactionEntity) -> Unit = { tx ->
        if (!tx.isCompleted) {
            transactionToConfirmComplete = tx
        } else {
            viewModel.toggleTransactionCompleted(tx)
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        val intent = activity?.intent
        val confirmTxId = intent?.getIntExtra("confirm_complete_tx_id", -1) ?: -1
        if (confirmTxId != -1) {
            val billNotifId = intent?.getIntExtra("confirm_complete_bill_id", 0) ?: 0
            notificationBillIdToCancel = billNotifId
            intent?.removeExtra("confirm_complete_tx_id")
            val isTx = intent?.getBooleanExtra("confirm_complete_is_tx", true) ?: true
            val title = intent?.getStringExtra("confirm_complete_title") ?: ""
            val amount = intent?.getDoubleExtra("confirm_complete_amount", 0.0) ?: 0.0
            val dueDateStr = intent?.getStringExtra("confirm_complete_due_date") ?: ""
            if (isTx) {
                val match = uiState.allTransactions.firstOrNull { it.id == confirmTxId }
                if (match != null) {
                    transactionToConfirmComplete = match
                } else if (title.isNotEmpty()) {
                    transactionToConfirmComplete = TransactionEntity(
                        id = confirmTxId,
                        title = title,
                        amount = amount,
                        dateMillis = System.currentTimeMillis(),
                        category = "Expense",
                        isIncome = false,
                        isCompleted = false
                    )
                }
            } else {
                val planned = uiState.plannedExpenses.firstOrNull { it.id == confirmTxId }
                if (planned != null) {
                    val dateParsed = try { LocalDate.parse(planned.dueDate) } catch (e: Exception) { LocalDate.now() }
                    transactionToConfirmComplete = TransactionEntity(
                        id = 0,
                        title = planned.title,
                        amount = planned.minAmount,
                        dateMillis = DateUtils.localDateToMillis(dateParsed),
                        category = "Planned Bill",
                        accountName = planned.targetAccount,
                        isIncome = false,
                        isCompleted = false,
                        isAutoPay = planned.isAutoPay
                    )
                } else if (title.isNotEmpty()) {
                    transactionToConfirmComplete = TransactionEntity(
                        id = 0,
                        title = title,
                        amount = amount,
                        dateMillis = System.currentTimeMillis(),
                        category = "Planned Bill",
                        isIncome = false,
                        isCompleted = false
                    )
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.undoMessageEvent.collectLatest { message ->
            if (!message.startsWith("Undone:")) {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "UNDO",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undo()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Formatters
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val configuration = LocalConfiguration.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Unfolded Foldable & Multi-Window split-screen handling:
        // On foldables (e.g. Galaxy Z Fold, Pixel Fold) in split-screen or multi-window mode,
        // the app may only occupy half the screen (< 800dp width) even when the device is unfolded
        // or rotated to landscape. We adapt strictly according to the actual container width (maxWidth)
        // rather than the physical device screen width or device orientation.
        // Navigation rail is only shown when width is at least 768dp (Expanded/Large screen).
        // Dual-pane side-by-side dashboard is only activated when width is at least 840dp.
        val showNavRail = maxWidth >= 768.dp
        val isWideScreen = maxWidth >= 600.dp
        val isDualPaneDashboard = maxWidth >= 840.dp
        val isCompactHeight = maxHeight < 480.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.testTag("undo_snackbar_host")
                )
            },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 4.dp)
                ) {
                    if (isWideScreen) {
                        // Spacious single-line header for Fold 8 Inner Screen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 2.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Month / Pay Cycle Controls
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.prevMonth() },
                                    modifier = Modifier.testTag("prev_month_button")
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Pay Cycle")
                                }
                                Button(
                                    onClick = { viewModel.selectMonth(YearMonth.now()) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("today_button")
                                ) {
                                    Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { viewModel.nextMonth() },
                                    modifier = Modifier.testTag("next_month_button")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Next Pay Cycle")
                                }
                            }

                            // Center: Date Title
                            val payCycleTitleFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
                            val payCycleTitleYearFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
                            val pStart = uiState.currentPayPeriodStart
                            val pEnd = uiState.currentPayPeriodEnd
                            val payCycleText = if (pStart.year == pEnd.year) {
                                "${pStart.format(payCycleTitleFormatter)} – ${pEnd.format(payCycleTitleYearFormatter)}"
                            } else {
                                "${pStart.format(payCycleTitleYearFormatter)} – ${pEnd.format(payCycleTitleYearFormatter)}"
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = payCycleText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Pay Cycle • ${uiState.selectedMonth.format(monthTitleFormatter)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // Right: Utility Actions for Galaxy Z Fold 8 Unfolded
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (uiState.canUndo) {
                                    FilledTonalButton(
                                        onClick = { viewModel.undo() },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        ),
                                        modifier = Modifier.testTag("undo_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Undo", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                FilledTonalButton(
                                    onClick = { showGlobalTransferModal = true },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("topbar_transfer_button")
                                ) {
                                    Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Transfer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        preselectedDateForAdd = null
                                        showAddDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.testTag("topbar_add_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                IconButton(
                                    onClick = { viewModel.toggleDarkMode() },
                                    modifier = Modifier.testTag("topbar_theme_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                        contentDescription = "Toggle Theme",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        // Galaxy Z Fold 8 Cover Screen (Narrow portrait, ~380-420dp width)
                        // Tier 1: Dedicated Cycle Date Navigator
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewModel.prevMonth() },
                                modifier = Modifier.size(36.dp).testTag("prev_month_button")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Previous Pay Cycle", modifier = Modifier.size(18.dp))
                            }

                            val payCycleTitleFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
                            val payCycleTitleYearFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
                            val pStart = uiState.currentPayPeriodStart
                            val pEnd = uiState.currentPayPeriodEnd
                            val payCycleText = if (pStart.year == pEnd.year) {
                                "${pStart.format(payCycleTitleFormatter)} – ${pEnd.format(payCycleTitleFormatter)}"
                            } else {
                                "${pStart.format(payCycleTitleYearFormatter)} – ${pEnd.format(payCycleTitleYearFormatter)}"
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = payCycleText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Cycle • ${uiState.selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.US))}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }

                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier.size(36.dp).testTag("next_month_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Next Pay Cycle", modifier = Modifier.size(18.dp))
                            }
                        }

                        // Tier 2: Compact utility strip with Today, Undo, and quick top actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedTab == 8) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clickable { selectedTab = if (selectedTab == 8) 0 else 8 }
                                        .testTag("topbar_summary_chip")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Summarize,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = if (selectedTab == 8) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Summary",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedTab == 8) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier
                                        .clickable { viewModel.selectMonth(YearMonth.now()) }
                                        .testTag("today_button")
                                ) {
                                    Text(
                                        text = "Today",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                if (uiState.canUndo) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clickable { viewModel.undo() }
                                            .testTag("undo_button")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                                contentDescription = "Undo",
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "Undo",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { selectedTab = 4 },
                                    modifier = Modifier.size(30.dp).testTag("topbar_debt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = "Debt Planner",
                                        modifier = Modifier.size(17.dp),
                                        tint = if (selectedTab == 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { selectedTab = 6 },
                                    modifier = Modifier.size(30.dp).testTag("topbar_ai_hub_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Hub",
                                        modifier = Modifier.size(17.dp),
                                        tint = if (selectedTab == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { selectedTab = 7 },
                                    modifier = Modifier.size(30.dp).testTag("topbar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        modifier = Modifier.size(17.dp),
                                        tint = if (selectedTab == 7) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!showNavRail) {
                    FoldableBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            },
            floatingActionButtonPosition = FabPosition.End,
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(
                        visible = uiState.canUndo,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        SmallFloatingActionButton(
                            onClick = { viewModel.undo() },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.testTag("floating_undo_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (selectedTab != 4 && selectedTab != 6 && selectedTab != 7) {
                        SmallFloatingActionButton(
                            onClick = {
                                preselectedDateForAdd = null
                                showAddDialog = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_transaction_fab")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Transaction",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (showNavRail) {
                    FoldableNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        isCompactHeight = isCompactHeight
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (selectedTab) {
                    0 -> {
                        // Pay Cycles Dashboard with Adaptive Side-by-Side Dual-Pane for Galaxy Z Fold / Wide Screens
                        val dashboardSectionOrder by viewModel.dashboardSectionOrder.collectAsState()
                        var isDashboardReorderActive by rememberSaveable { mutableStateOf(false) }
                        val accountsOverviewExpanded by viewModel.accountsOverviewExpanded.collectAsState()
                        val monthlySummaryExpanded by viewModel.monthlySummaryExpanded.collectAsState()
                        val anyExpanded = accountsOverviewExpanded || monthlySummaryExpanded
                        val haptic = LocalHapticFeedback.current

                        if (isDualPaneDashboard) {
                            // Galaxy Z Fold 8 Unfolded: True Ergonomic Dual-Pane Split Layout
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left Pane: Financial Command Center & Runways (~46% width)
                                val leftScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .verticalScroll(leftScrollState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Overview Control Bar: Active Account Quick Stat + Expand/Collapse Master Toggle
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val activeAcc = uiState.activeAccountName
                                        val activeBal = if (activeAcc == "All Accounts" || activeAcc.isBlank()) {
                                            uiState.accountBalances.values.sumOf { it.clearedBalance }.takeIf { it != 0.0 }
                                                ?: (uiState.currentPayPeriod?.remainingBalance ?: uiState.monthlyRemainingBalance)
                                        } else {
                                            uiState.accountBalances[activeAcc]?.clearedBalance ?: 0.0
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = if (activeAcc == "All Accounts" || activeAcc.isBlank()) "🏛️ All Accounts" else "💳 $activeAcc",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "$%,.2f", activeBal),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (activeBal >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    val target = !anyExpanded
                                                    viewModel.setAccountsOverviewExpanded(target)
                                                    viewModel.setMonthlySummaryExpanded(target)
                                                }
                                                .testTag("dashboard_master_expand_collapse_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (anyExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                                    contentDescription = if (anyExpanded) "Collapse All" else "Expand All",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (anyExpanded) "Collapse All" else "Expand All",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isDashboardReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                            border = BorderStroke(1.dp, if (isDashboardReorderActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    isDashboardReorderActive = !isDashboardReorderActive
                                                }
                                                .testTag("dashboard_dual_reorder_toggle_button")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isDashboardReorderActive) Icons.Default.Lock else Icons.Default.SwapVert,
                                                    contentDescription = if (isDashboardReorderActive) "Lock Sections" else "Reorder Sections",
                                                    tint = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = if (isDashboardReorderActive) "Lock Order" else "Reorder",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    // Display sections in left pane according to dashboardSectionOrder
                                    val dualPaneLeftSections = dashboardSectionOrder.filter { it != "cycles" }
                                    for ((idx, sectionKey) in dualPaneLeftSections.withIndex()) {
                                        val title = when (sectionKey) {
                                            "accounts" -> "Accounts Overview"
                                            "monthly_summary" -> "Monthly Summary (Planned vs. Income)"
                                            else -> "Section"
                                        }
                                        MovableSectionWrapper(
                                            sectionKey = sectionKey,
                                            sectionTitle = title,
                                            index = idx,
                                            totalSections = dualPaneLeftSections.size,
                                            isReordering = isDashboardReorderActive,
                                            onToggleReorder = { isDashboardReorderActive = !isDashboardReorderActive },
                                            onMoveUp = {
                                                if (idx > 0) {
                                                    val fullList = dashboardSectionOrder.toMutableList()
                                                    val itemA = dualPaneLeftSections[idx]
                                                    val itemB = dualPaneLeftSections[idx - 1]
                                                    val posA = fullList.indexOf(itemA)
                                                    val posB = fullList.indexOf(itemB)
                                                    if (posA >= 0 && posB >= 0) {
                                                        fullList[posA] = itemB
                                                        fullList[posB] = itemA
                                                        viewModel.setDashboardSectionOrder(fullList)
                                                    }
                                                }
                                            },
                                            onMoveDown = {
                                                if (idx < dualPaneLeftSections.size - 1) {
                                                    val fullList = dashboardSectionOrder.toMutableList()
                                                    val itemA = dualPaneLeftSections[idx]
                                                    val itemB = dualPaneLeftSections[idx + 1]
                                                    val posA = fullList.indexOf(itemA)
                                                    val posB = fullList.indexOf(itemB)
                                                    if (posA >= 0 && posB >= 0) {
                                                        fullList[posA] = itemB
                                                        fullList[posB] = itemA
                                                        viewModel.setDashboardSectionOrder(fullList)
                                                    }
                                                }
                                            }
                                        ) {
                                            when (sectionKey) {
                                                "accounts" -> {
                                                    AccountSwitcherCarousel(
                                                        uiState = uiState,
                                                        onSelectAccount = { viewModel.selectAccount(it) },
                                                        onAddAccount = { viewModel.addAccount(it) },
                                                        onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                                        onDeleteAccount = { viewModel.deleteAccount(it) },
                                                        onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                                        onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                                        onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                                        onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                                        onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                                        isWideScreen = true,
                                                        expanded = accountsOverviewExpanded,
                                                        onToggleExpand = { viewModel.setAccountsOverviewExpanded(!accountsOverviewExpanded) }
                                                    )
                                                }
                                                "monthly_summary" -> {
                                                    MonthlySummaryDashboardCard(
                                                        uiState = uiState,
                                                        onOpenFullMonthlySummary = { selectedTab = 8 },
                                                        expanded = monthlySummaryExpanded,
                                                        onToggleExpand = { viewModel.setMonthlySummaryExpanded(!monthlySummaryExpanded) }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                                }

                                // Center Fold Crease Buffer Divider
                                VerticalDivider(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )

                                // Right Pane: Pay Cycles & Ledger (~54% width)
                                val rightScrollState = rememberScrollState()
                                Column(
                                    modifier = Modifier
                                        .weight(1.15f)
                                        .fillMaxHeight()
                                        .verticalScroll(rightScrollState),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PayCyclesListView(
                                        uiState = uiState,
                                        onAddTransactionClick = { date ->
                                            preselectedDateForAdd = date
                                            showAddDialog = true
                                        },
                                        onCreateExpenseForRemaining = { amount, date ->
                                            viewModel.addTransaction(
                                                title = "Remaining Cycle Expense",
                                                amount = amount,
                                                date = date,
                                                category = "Other",
                                                isIncome = false,
                                                isRecurring = false,
                                                accountName = if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName,
                                                isCompleted = false,
                                                isAutoPay = false
                                            )
                                        },
                                        onEditPaycheckClick = { date, currentAmount ->
                                            editingPaycheckAmount = String.format(Locale.US, "%.2f", currentAmount)
                                            showPaycheckEditDialog = date
                                        },
                                        onDeleteTransaction = { tx -> deleteConfirmationTx = tx },
                                        onEditTransaction = { tx -> editingTransaction = tx },
                                        onDuplicateTransaction = { tx -> viewModel.duplicateTransaction(tx) },
                                        onToggleCompleted = handleRequestToggleCompleted,
                                        onUpdateTransactionStatus = { tx, st -> viewModel.updateTransactionStatus(tx, st) },
                                        onToggleReminder = { tx, days -> viewModel.toggleTransactionReminder(tx, days) },
                                        onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                        onNavigateToUpcoming = { selectedTab = 3 },
                                        onDeduplicate = {
                                            viewModel.deduplicateTransactionsAndBills { count ->
                                                val msg = if (count > 0) "Deduplicated $count item(s)!" else "No duplicates found"
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        onRestoreFinancialData = { viewModel.restoreFinancialDataManually() },
                                        onManualSyncDrive = { viewModel.performDriveSync() },
                                        isWideScreen = true
                                    )
                                    Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                                }
                            }
                        } else {
                            // Phone / Cover Screen: Single Column Scrollable with Reorderable Sections
                            val pageScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 1800.dp)
                                    .align(Alignment.TopCenter)
                                    .verticalScroll(pageScrollState)
                            ) {
                            // Overview Control Bar: Active Account Quick Stat + Expand/Collapse Master Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val activeAcc = uiState.activeAccountName
                                val activeBal = if (activeAcc == "All Accounts" || activeAcc.isBlank()) {
                                    uiState.accountBalances.values.sumOf { it.clearedBalance }.takeIf { it != 0.0 }
                                        ?: (uiState.currentPayPeriod?.remainingBalance ?: uiState.monthlyRemainingBalance)
                                } else {
                                    uiState.accountBalances[activeAcc]?.clearedBalance ?: 0.0
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = if (activeAcc == "All Accounts" || activeAcc.isBlank()) "🏛️ All Accounts" else "💳 $activeAcc",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", activeBal),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (activeBal >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val target = !anyExpanded
                                            viewModel.setAccountsOverviewExpanded(target)
                                            viewModel.setMonthlySummaryExpanded(target)
                                        }
                                        .testTag("dashboard_master_expand_collapse_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (anyExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                            contentDescription = if (anyExpanded) "Collapse All" else "Expand All",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (anyExpanded) "Collapse All" else "Expand All",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isDashboardReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                    border = BorderStroke(1.dp, if (isDashboardReorderActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            isDashboardReorderActive = !isDashboardReorderActive
                                        }
                                        .testTag("dashboard_reorder_toggle_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDashboardReorderActive) Icons.Default.Lock else Icons.Default.SwapVert,
                                            contentDescription = if (isDashboardReorderActive) "Lock Sections" else "Reorder Sections",
                                            tint = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isDashboardReorderActive) "Lock Order" else "Reorder",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (isDashboardReorderActive) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "Sections Unlocked • Reorder then Lock",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    viewModel.setDashboardSectionOrder(dashboardSectionOrder)
                                                    isDashboardReorderActive = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                modifier = Modifier.height(28.dp).testTag("lock_layout_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Lock Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = {
                                                    viewModel.resetDashboardSectionOrder()
                                                    isDashboardReorderActive = false
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("reset_section_order_button")
                                            ) {
                                                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            for ((index, sectionKey) in dashboardSectionOrder.withIndex()) {
                                val title = when (sectionKey) {
                                    "accounts" -> "Accounts Overview"
                                    "monthly_summary" -> "Monthly Summary (Planned vs. Income)"
                                    "cycles" -> if (isWideScreen) "Pay Cycles & Transactions (Side-by-Side)" else "Pay Cycles & Transactions"
                                    else -> "Section"
                                }

                                MovableSectionWrapper(
                                    sectionKey = sectionKey,
                                    sectionTitle = title,
                                    index = index,
                                    totalSections = dashboardSectionOrder.size,
                                    isReordering = isDashboardReorderActive,
                                    onToggleReorder = { isDashboardReorderActive = !isDashboardReorderActive },
                                    onMoveUp = {
                                        if (index > 0) {
                                            val list = dashboardSectionOrder.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index - 1]
                                            list[index - 1] = temp
                                            viewModel.setDashboardSectionOrder(list)
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < dashboardSectionOrder.size - 1) {
                                            val list = dashboardSectionOrder.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index + 1]
                                            list[index + 1] = temp
                                            viewModel.setDashboardSectionOrder(list)
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp)
                                ) {
                                    when (sectionKey) {
                                        "accounts" -> {
                                            AccountSwitcherCarousel(
                                                uiState = uiState,
                                                onSelectAccount = { viewModel.selectAccount(it) },
                                                onAddAccount = { viewModel.addAccount(it) },
                                                onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                                onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                                onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                                onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                                onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                                onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                                isWideScreen = isWideScreen,
                                                expanded = accountsOverviewExpanded,
                                                onToggleExpand = { viewModel.setAccountsOverviewExpanded(!accountsOverviewExpanded) }
                                            )
                                        }
                                        "monthly_summary" -> {
                                            MonthlySummaryDashboardCard(
                                                uiState = uiState,
                                                onOpenFullMonthlySummary = { selectedTab = 8 },
                                                expanded = monthlySummaryExpanded,
                                                onToggleExpand = { viewModel.setMonthlySummaryExpanded(!monthlySummaryExpanded) }
                                            )
                                        }
                                        "cycles" -> {
                                            PayCyclesListView(
                                                uiState = uiState,
                                                onAddTransactionClick = { date ->
                                                    preselectedDateForAdd = date
                                                    showAddDialog = true
                                                },
                                                onCreateExpenseForRemaining = { amount, date ->
                                                    viewModel.addTransaction(
                                                        title = "Remaining Cycle Expense",
                                                        amount = amount,
                                                        date = date,
                                                        category = "Other",
                                                        isIncome = false,
                                                        isRecurring = false,
                                                        accountName = if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName,
                                                        isCompleted = false,
                                                        isAutoPay = false
                                                    )
                                                },
                                                onEditPaycheckClick = { date, currentAmount ->
                                                    editingPaycheckAmount = String.format(Locale.US, "%.2f", currentAmount)
                                                    showPaycheckEditDialog = date
                                                },
                                                onDeleteTransaction = { tx -> deleteConfirmationTx = tx },
                                                onEditTransaction = { tx -> editingTransaction = tx },
                                                onDuplicateTransaction = { tx -> viewModel.duplicateTransaction(tx) },
                                                onToggleCompleted = handleRequestToggleCompleted,
                                                onUpdateTransactionStatus = { tx, st -> viewModel.updateTransactionStatus(tx, st) },
                                                onToggleReminder = { tx, days -> viewModel.toggleTransactionReminder(tx, days) },
                                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                                onNavigateToUpcoming = { selectedTab = 3 },
                                                onDeduplicate = {
                                                    viewModel.deduplicateTransactionsAndBills { count ->
                                                        val msg = if (count > 0) "Deduplicated $count item(s)!" else "No duplicates found"
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onRestoreFinancialData = { viewModel.restoreFinancialDataManually() },
                                                onManualSyncDrive = { viewModel.performDriveSync() },
                                                isWideScreen = isWideScreen
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    }
                    1 -> {
                        // Income Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .verticalScroll(pageScrollState)
                        ) {
                            AccountSwitcherCarousel(
                                uiState = uiState,
                                onSelectAccount = { viewModel.selectAccount(it) },
                                onAddAccount = { viewModel.addAccount(it) },
                                onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EnterIncomeView(
                                uiState = uiState,
                                onAddIncome = { title, amount, dateDeposited, category, isRecurring, accountName, isAutoPay, isCompleted ->
                                    viewModel.addTransaction(
                                        title = title,
                                        amount = amount,
                                        date = dateDeposited,
                                        category = category,
                                        isIncome = true,
                                        isRecurring = isRecurring,
                                        accountName = accountName,
                                        isCompleted = isCompleted,
                                        isAutoPay = isAutoPay
                                    )
                                },
                                onDeleteIncome = { tx -> deleteConfirmationTx = tx },
                                onEditIncome = { tx -> editingTransaction = tx },
                                onToggleIncome = handleRequestToggleCompleted,
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                onAddIncomeSource = { name, amount, freq -> viewModel.addIncomeSource(name, amount, freq) },
                                onDeleteIncomeSource = { src -> viewModel.deleteIncomeSource(src) },
                                onUpdateDefaultPaycheck = { amt -> viewModel.updateDefaultPaycheckAmount(amt) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    2 -> {
                        // Expenses Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .verticalScroll(pageScrollState)
                        ) {
                            AccountSwitcherCarousel(
                                uiState = uiState,
                                onSelectAccount = { viewModel.selectAccount(it) },
                                onAddAccount = { viewModel.addAccount(it) },
                                onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EnterExpensesView(
                                uiState = uiState,
                                onAddExpense = { title, amount, dueDate, category, isRecurring, accountName, isAutoPay, isCompleted, status ->
                                    viewModel.addTransaction(
                                        title = title,
                                        amount = amount,
                                        date = dueDate,
                                        category = category,
                                        isIncome = false,
                                        isRecurring = isRecurring,
                                        accountName = accountName,
                                        isCompleted = isCompleted,
                                        isAutoPay = isAutoPay,
                                        status = status
                                    )
                                },
                                onDeleteExpense = { tx -> deleteConfirmationTx = tx },
                                onEditExpense = { tx -> editingTransaction = tx },
                                onDuplicateExpense = { tx -> viewModel.duplicateTransaction(tx) },
                                onToggleExpense = handleRequestToggleCompleted,
                                onUpdateExpenseStatus = { tx, st -> viewModel.updateTransactionStatus(tx, st) },
                                onToggleReminder = { tx, days -> viewModel.toggleTransactionReminder(tx, days) },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    3 -> {
                        // Upcoming Bills & Expenses Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = if (isWideScreen) 14.dp else 8.dp, vertical = 4.dp)
                                .verticalScroll(pageScrollState)
                        ) {
                            AccountSwitcherCarousel(
                                uiState = uiState,
                                onSelectAccount = { viewModel.selectAccount(it) },
                                onAddAccount = { viewModel.addAccount(it) },
                                onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            com.example.ui.UpcomingExpensesView(
                                uiState = uiState,
                                onToggleCompleted = handleRequestToggleCompleted,
                                onUpdateTransactionStatus = { tx, st -> viewModel.updateTransactionStatus(tx, st) },
                                onOpenAddBill = { showPlannedBillsDialog = true },
                                onOpenAddExpense = {
                                    preselectedDateForAdd = null
                                    showAddDialog = true
                                },
                                onDeleteExpense = { item -> viewModel.deleteUpcomingExpense(item) },
                                onEditTransaction = { tx -> editingTransaction = tx },
                                onDuplicateTransaction = { tx -> viewModel.duplicateTransaction(tx) },
                                onToggleReminder = { tx, days -> viewModel.toggleTransactionReminder(tx, days) },
                                onTestReminderNotification = { viewModel.testNotificationReminder() },
                                onUndo = { viewModel.undo() },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    4 -> {
                        // Debt Planner Page
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            com.example.ui.DebtPlannerView(
                                uiState = uiState,
                                onAddDebt = { name, bal, rate, minPay, cat, notes, dueDay ->
                                    viewModel.addDebt(name, bal, rate, minPay, cat, notes, billDueDay = dueDay)
                                },
                                onDeleteDebt = { debt ->
                                    viewModel.deleteDebt(debt)
                                },
                                onUpdateDebt = { debt ->
                                    viewModel.updateDebt(debt)
                                },
                                isWideScreen = isWideScreen,
                                onImportDebtsFromCsv = { csvText ->
                                    val (debtOk, debtMsg) = viewModel.importDebtsFromAnyFormat(csvText)
                                    if (debtOk) Pair(true, debtMsg)
                                    else viewModel.importFinancialContentSmartDetailed(csvText, "Debt Import", isDebtHint = true)
                                },
                                onExportDebtsToCsv = { viewModel.exportDebtsToCsv() },
                                onSyncGoogleDrive = { viewModel.performDebtDriveSync(null) },
                                onConnectGoogleDrive = {
                                    try {
                                        val pendingIntent = viewModel.getPendingAuthIntent()
                                        if (pendingIntent != null) {
                                            googleSignInLauncher.launch(pendingIntent)
                                        } else {
                                            val client = viewModel.getGoogleSignInClient()
                                            googleSignInLauncher.launch(client.signInIntent)
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            googleSignInLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                isSyncing = uiState.syncState is com.example.sync.SyncState.Syncing,
                                accountEmail = uiState.driveAccountEmail,
                                onListDriveFiles = { viewModel.listDriveFiles() },
                                onImportSpecificDriveFile = { file -> viewModel.importSpecificDriveFile(file, isDebtSection = true) },
                                onImportGoogleSheetByUrlOrId = { urlOrId -> viewModel.importGoogleSheetByUrlOrId(urlOrId, isDebtHint = true) },
                                onImportAnyDataDetailed = { data -> viewModel.importFinancialContentSmartDetailed(data, "Debt Import", isDebtHint = true) },
                                onSaveCustomToken = { token -> viewModel.saveCustomDriveToken(token) },
                                onLogPayment = { dId, pAmt, princ, intAmt, note, createExp ->
                                    viewModel.logDebtPayment(dId, pAmt, princ, intAmt, note, createExp)
                                },
                                onDeletePayment = { payment ->
                                    viewModel.deleteDebtPayment(payment)
                                },
                                onToggleBillLink = { debt, isLinked, dueDay ->
                                    viewModel.toggleDebtBillLink(debt, isLinked, dueDay)
                                },
                                onTogglePriorityPin = { debt ->
                                    viewModel.toggleDebtPriorityPin(debt)
                                },
                                onTogglePaidOff = { debt ->
                                    viewModel.toggleDebtPaidOff(debt)
                                },
                                onAddFullDebt = { debt ->
                                    viewModel.addDebt(
                                        name = debt.name,
                                        balance = debt.balance,
                                        interestRate = debt.interestRate,
                                        minimumPayment = debt.minimumPayment,
                                        category = debt.category,
                                        notes = debt.notes,
                                        promoApr = debt.promoApr,
                                        promoExpirationDate = debt.promoExpirationDate,
                                        customPriorityRank = debt.customPriorityRank,
                                        originalBalance = debt.originalBalance,
                                        isLinkedToBills = debt.isLinkedToBills,
                                        billDueDay = debt.billDueDay,
                                        isPaidOff = debt.isPaidOff,
                                        linkedAccountId = debt.linkedAccountId,
                                        linkedAccountName = debt.linkedAccountName
                                    )
                                }
                            )
                        }
                    }
                    5 -> {
                        // Calendar Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = if (isWideScreen) 14.dp else 8.dp, vertical = 4.dp)
                                .verticalScroll(pageScrollState)
                        ) {
                            AccountSwitcherCarousel(
                                uiState = uiState,
                                onSelectAccount = { viewModel.selectAccount(it) },
                                onAddAccount = { viewModel.addAccount(it) },
                                onRenameAccount = { old, new -> viewModel.renameAccount(old, new) },
                                onDeleteAccount = { viewModel.deleteAccount(it) },
                                onMoveAccount = { acc, dir -> viewModel.moveAccount(acc, dir) },
                                onSetAccountBalance = { acc, bal -> viewModel.setAccountBalance(acc, bal) },
                                onAddAccountWithBalance = { name, bal -> viewModel.addAccount(name, bal) },
                                onTransferFunds = { from, to, amt, note -> viewModel.transferFunds(from, to, amt, note) },
                                onReconcileBalance = { acc, bal -> viewModel.reconcileAccountBalance(acc, bal) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            CalendarGridView(
                                uiState = uiState,
                                onAddTransactionClick = { date ->
                                    preselectedDateForAdd = date
                                    showAddDialog = true
                                },
                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                onEditTransaction = { tx -> editingTransaction = tx },
                                onDuplicateTransaction = { tx -> viewModel.duplicateTransaction(tx) },
                                onToggleCompleted = handleRequestToggleCompleted,
                                onUpdateTransactionStatus = { tx, st -> viewModel.updateTransactionStatus(tx, st) },
                                onToggleReminder = { tx, days -> viewModel.toggleTransactionReminder(tx, days) },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    6 -> {
                        // AI Insights Hub Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = if (isWideScreen) 14.dp else 8.dp, vertical = 4.dp)
                                .verticalScroll(pageScrollState)
                        ) {
                            AiFinancialHubView(
                                uiState = uiState,
                                onOpenQuickEntry = { showAiQuickEntryDialog = true },
                                onOpenReceiptScanner = { showAiReceiptScannerDialog = true },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    7 -> {
                        // Settings Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .padding(horizontal = if (isWideScreen) 14.dp else 8.dp, vertical = 4.dp)
                                .verticalScroll(pageScrollState)
                        ) {
                            SettingsView(
                                uiState = uiState,
                                onUpdateDefaultPaycheck = { viewModel.updateDefaultPaycheckAmount(it) },
                                onUpdateAnchorDay = { viewModel.updateAnchorPayday(it) },
                                onAddIncomeSource = { name, amount, freq -> viewModel.addIncomeSource(name, amount, freq) },
                                onDeleteIncomeSource = { viewModel.deleteIncomeSource(it) },
                                onToggleDarkMode = { viewModel.toggleDarkMode() },
                                onSelectTheme = { viewModel.setAppTheme(it) },
                                onSelectAppIcon = { viewModel.setAppIcon(it) },
                                onExportBackup = viewModel::exportToJson,
                                onImportBackup = viewModel::importFromJson,
                                onConnectGoogleDrive = {
                                    try {
                                        val pendingIntent = viewModel.getPendingAuthIntent()
                                        if (pendingIntent != null) {
                                            googleSignInLauncher.launch(pendingIntent)
                                        } else {
                                            val client = viewModel.getGoogleSignInClient()
                                            googleSignInLauncher.launch(client.signInIntent)
                                        }
                                    } catch (e: Exception) {
                                        try {
                                            googleSignInLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
                                        } catch (_: Exception) {}
                                    }
                                },
                                onSaveCustomDriveToken = { viewModel.saveCustomDriveToken(it) },
                                onSaveCustomDriveCredentials = { clientId, clientSecret, refreshToken, accessToken ->
                                    viewModel.saveCustomDriveCredentials(clientId, clientSecret, refreshToken, accessToken)
                                },
                                onGetCustomClientId = { viewModel.getCustomClientId() },
                                onGetCustomClientSecret = { viewModel.getCustomClientSecret() },
                                onGetCustomRefreshToken = { viewModel.getCustomRefreshToken() },
                                onGetCustomDriveToken = { viewModel.getCustomDriveToken() },
                                onManualSyncDrive = { viewModel.performDriveSync() },
                                onToggleDriveAutoSync = { viewModel.toggleDriveAutoSync(it) },
                                onConnectCustomSyncEmail = { viewModel.connectDriveAccountWithEmail(it) },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                onListDriveFiles = { viewModel.listDriveFiles() },
                                onImportSpecificDriveFile = { viewModel.importSpecificDriveFile(it) },
                                onImportGoogleSheetByUrlOrId = { viewModel.importGoogleSheetByUrlOrId(it) },
                                onExportToGoogleSheets = { onComplete -> viewModel.performExportToGoogleSheets(onComplete) },
                                onExportCsv = viewModel::exportToCsv,
                                onImportCsv = viewModel::importFromCsv,
                                onImportAnyDataDetailed = { viewModel.importAnyDataDetailed(it) },
                                onExportWebCompanionToGoogleDrive = { htmlContent -> viewModel.exportWebCompanionToGoogleDrive(htmlContent) },
                                onRestoreFinancialData = { viewModel.restoreFinancialDataManually() },
                                onClearDatabase = { viewModel.clearDatabase() },
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(if (isCompactHeight) 16.dp else if (isWideScreen) 24.dp else 56.dp))
                        }
                    }
                    8 -> {
                        // Monthly Summary Full Page
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                        ) {
                            MonthlySummaryView(
                                uiState = uiState,
                                onAddPlannedBill = { showPlannedBillsDialog = true },
                                onAddExpense = {
                                    preselectedDateForAdd = null
                                    showAddDialog = true
                                },
                                onToggleCompleted = handleRequestToggleCompleted,
                                onBack = { selectedTab = 0 },
                                isWideScreen = isWideScreen
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Transaction
    if (showAddDialog) {
        AddTransactionDialog(
            preselectedDate = preselectedDateForAdd ?: uiState.selectedMonth.atDay(1),
            selectedMonth = uiState.selectedMonth,
            allAccounts = uiState.allAccounts,
            initialAccountName = uiState.activeAccountName,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, amount, date, category, isIncome, isRecurring, accountName, isAutoPay, reminderDaysBefore, isCompleted, status ->
                viewModel.addTransaction(
                    title = title,
                    amount = amount,
                    date = date,
                    category = category,
                    isIncome = isIncome,
                    isRecurring = isRecurring,
                    accountName = accountName,
                    isCompleted = isCompleted,
                    isAutoPay = isAutoPay,
                    reminderDaysBefore = reminderDaysBefore,
                    status = status
                )
                showAddDialog = false
            }
        )
    }

    // Dialog: Edit Transaction
    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            allAccounts = uiState.allAccounts,
            onDismiss = { editingTransaction = null },
            onSave = { updated: TransactionEntity, applyToBaseline: Boolean ->
                viewModel.overrideTransaction(original = tx, updated = updated, applyToBaseline = applyToBaseline)
                editingTransaction = null
            },
            onDelete = { deleted: TransactionEntity ->
                deleteConfirmationTx = deleted
                editingTransaction = null
            }
        )
    }

    // Dialog: Confirm Delete (This Cycle Only vs Recurring Baseline)
    deleteConfirmationTx?.let { tx ->
        val isRecurringOrSynth = tx.isRecurring || tx.id < 0 || tx.title.contains("(Planned)")
        AlertDialog(
            onDismissRequest = { deleteConfirmationTx = null },
            title = {
                Text(
                    text = if (isRecurringOrSynth) "Delete Recurring Item" else "Delete Transaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "'${tx.title.replace(" (Planned)", "")}' (${String.format(Locale.US, "$%,.2f", tx.amount)})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    if (isRecurringOrSynth) {
                        Text(
                            text = "This item is part of a recurring baseline across cycles. How would you like to remove it?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("• This Cycle Only: Keeps other cycles independent.", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                                Text("• Delete Baseline: Completely removes it from all cycles.", fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        Text(
                            text = "Are you sure you want to delete this transaction from this pay cycle?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (isRecurringOrSynth) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.deleteTransaction(tx, deleteBaseline = false)
                                deleteConfirmationTx = null
                            },
                            modifier = Modifier.testTag("delete_cycle_only_button")
                        ) {
                            Text("This Cycle Only", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = {
                                viewModel.deleteTransaction(tx, deleteBaseline = true)
                                deleteConfirmationTx = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("delete_baseline_button")
                        ) {
                            Text("Delete Baseline", fontSize = 12.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            viewModel.deleteTransaction(tx, deleteBaseline = false)
                            deleteConfirmationTx = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Delete")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmationTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Edit paycheck on specific date
    showPaycheckEditDialog?.let { date ->
        AlertDialog(
            onDismissRequest = { showPaycheckEditDialog = null },
            title = { Text("Edit Paycheck Amount") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize the paycheck received on ${date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US))}.",
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = editingPaycheckAmount,
                        onValueChange = { editingPaycheckAmount = it },
                        label = { Text("Paycheck Amount") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("paycheck_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingPaycheckAmount.toDoubleOrNull()?.let { amt ->
                            viewModel.saveOrUpdatePaycheck(date, amt)
                        }
                        showPaycheckEditDialog = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaycheckEditDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPlannedBillsDialog) {
        PlannedBillsDialog(
            uiState = uiState,
            onAddPlannedExpense = { title, dueDate, minAmount, targetAccount, isAutoPay, reminderDaysBefore ->
                viewModel.addPlannedExpense(title, dueDate, minAmount, targetAccount, isAutoPay, reminderDaysBefore)
            },
            onDeletePlannedExpense = { bill ->
                viewModel.deletePlannedExpense(bill)
            },
            onUpdatePlannedExpenseAutoPaySource = { bill, targetAccount, isAutoPay ->
                viewModel.updatePlannedExpenseAutoPaySource(bill, targetAccount, isAutoPay)
            },
            onDismiss = { showPlannedBillsDialog = false }
        )
    }

    if (showAiQuickEntryDialog) {
        AiNaturalLanguageQuickEntryDialog(
            availableAccounts = uiState.allAccounts,
            activeAccount = uiState.activeAccountName,
            onDismiss = { showAiQuickEntryDialog = false },
            onConfirmTransaction = { title, amount, date, category, isIncome, isRecurring, accountName, isAutoPay, reminderDaysBefore ->
                viewModel.addTransaction(title, amount, date, category, isIncome, isRecurring, accountName, false, isAutoPay, reminderDaysBefore)
                showAiQuickEntryDialog = false
            }
        )
    }

    if (showAiReceiptScannerDialog) {
        AiReceiptScannerDialog(
            availableAccounts = uiState.allAccounts,
            activeAccount = uiState.activeAccountName,
            onDismiss = { showAiReceiptScannerDialog = false },
            onConfirmExpense = { title, amount, date, category, isIncome, isRecurring, accountName, isAutoPay, reminderDaysBefore ->
                viewModel.addTransaction(title, amount, date, category, isIncome, isRecurring, accountName, false, isAutoPay, reminderDaysBefore)
                showAiReceiptScannerDialog = false
            }
        )
    }

    if (showGlobalTransferModal) {
        TransferFundsDialog(
            uiState = uiState,
            onDismiss = { showGlobalTransferModal = false },
            onConfirmTransfer = { from, to, amount, note ->
                viewModel.transferFunds(from, to, amount, note)
            }
        )
    }

    if (showGlobalReconcileModal) {
        ReconcileBalanceDialog(
            uiState = uiState,
            onDismiss = { showGlobalReconcileModal = false },
            onConfirmReconcile = { account, actualBal ->
                viewModel.reconcileAccountBalance(account, actualBal)
            }
        )
    }

    // Dialog: Confirmation before marking complete
    transactionToConfirmComplete?.let { tx ->
        MarkCompleteConfirmationDialog(
            transaction = tx,
            onConfirm = {
                viewModel.updateTransactionStatus(tx, if (tx.isIncome) "cleared" else "paid")
                if (notificationBillIdToCancel != 0) {
                    val notifManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    notifManager?.cancel(notificationBillIdToCancel)
                    notificationBillIdToCancel = 0
                }
                transactionToConfirmComplete = null
            },
            onDismiss = {
                transactionToConfirmComplete = null
            }
        )
    }
}
}

@Composable
fun MarkCompleteConfirmationDialog(
    transaction: TransactionEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val amountFormatted = String.format(Locale.US, "$%,.2f", transaction.amount)
    val dueDate = remember(transaction.dateMillis) {
        DateUtils.millisToLocalDate(transaction.dateMillis)
    }
    val dateFormatted = remember(dueDate) {
        dueDate.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("mark_complete_confirmation_dialog"),
        icon = {
            Surface(
                shape = CircleShape,
                color = Color(0xFF10B981).copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        },
        title = {
            Text(
                text = if (transaction.isIncome) "Mark as Received?" else "Mark as Paid?",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (transaction.isIncome) {
                        "Are you sure you want to mark this income as received / complete?"
                    } else {
                        "Are you sure you want to mark this expense as complete?"
                    },
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = amountFormatted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF10B981)
                            )
                            Text(
                                text = if (transaction.isIncome) "Received: $dateFormatted" else "Due: $dateFormatted",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (transaction.accountName.isNotEmpty() && transaction.accountName != "All Accounts") {
                            Text(
                                text = "Account: ${transaction.accountName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!transaction.isIncome) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        )
                        Text(
                            text = "Any scheduled reminder alerts for this bill will be cancelled.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("confirm_mark_complete_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (transaction.isIncome) "Mark Received" else "Mark Complete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("cancel_mark_complete_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExcelDropdownCell(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "excel_dropdown"
) {
    var expanded by remember { mutableStateOf(false) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clickable { expanded = true }
                .testTag("${testTagPrefix}_trigger"),
            shape = RoundedCornerShape(6.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(1.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 12.sp,
                            fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier.testTag("${testTagPrefix}_option_$option")
                )
            }
        }
    }
}

@Composable
fun EnterIncomeView(
    uiState: BudgetUiState,
    onAddIncome: (title: String, amount: Double, dateDeposited: LocalDate, category: String, isRecurring: Boolean, accountName: String, isAutoPay: Boolean, isCompleted: Boolean) -> Unit,
    onDeleteIncome: (TransactionEntity) -> Unit,
    onEditIncome: (TransactionEntity) -> Unit = {},
    onToggleIncome: ((TransactionEntity) -> Unit)? = null,
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    onAddIncomeSource: ((String, Double, String) -> Unit)? = null,
    onDeleteIncomeSource: ((IncomeSourceEntity) -> Unit)? = null,
    onUpdateDefaultPaycheck: ((Double) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val isDark = uiState.isDarkMode
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by remember { mutableStateOf("Paycheck") }
    var selectedAccount by remember { mutableStateOf(if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurFrequency by remember { mutableStateOf("Bi-Weekly") }
    var isAutoPay by remember { mutableStateOf(false) }
    var isIncomeCompleted by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isQuickInsertExpanded by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showEditDefaultPaycheckDialog by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSyncingSheets by remember { mutableStateOf(false) }

    var filterScope by remember { mutableIntStateOf(0) } // 0 = All Rows, 1 = Full Month, 2 = Pay Cycle, 3 = Pay Week
    var sortIncomeAscending by remember { mutableStateOf(false) } // false = Newest First, true = Oldest First
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Pending, 2 = Cleared

    LaunchedEffect(uiState.selectedMonth) {
        val today = LocalDate.now()
        if (YearMonth.from(today) == uiState.selectedMonth) {
            dateInput = today.toString()
        } else {
            dateInput = uiState.selectedMonth.atDay(1).toString()
        }
    }

    LaunchedEffect(uiState.activeAccountName) {
        selectedAccount = if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val dateRangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val weekRangeText = remember(uiState.currentPayWeekStart, uiState.currentPayWeekEnd) {
        "${uiState.currentPayWeekStart.format(dateRangeFormatter)} – ${uiState.currentPayWeekEnd.format(dateRangeFormatter)}"
    }
    val cycleRangeText = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd) {
        "${uiState.currentPayPeriodStart.format(dateRangeFormatter)} – ${uiState.currentPayPeriodEnd.format(dateRangeFormatter)}"
    }

    val incomeCategories = listOf("Paycheck", "Income", "Freelance", "Bonus", "Investment", "Other")

    // Filter income transactions
    val incomeList = remember(uiState.allTransactions, uiState.activeAccountName, sortIncomeAscending) {
        val primaryAcc = uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
        val dbIncome = uiState.allTransactions.filter { tx ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    tx.accountName.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (primaryAcc.isNotEmpty() && uiState.activeAccountName.equals(primaryAcc, ignoreCase = true) && (tx.accountName.isBlank() || tx.accountName.equals("Primary Account", ignoreCase = true) || tx.accountName.equals("Primary", ignoreCase = true)))
            val catLower = tx.category.lowercase(java.util.Locale.US)
            val isIncomeCat = catLower == "paycheck" || catLower == "income" || catLower == "salary" || catLower == "bonus" || catLower == "deposit" || catLower == "freelance" || catLower == "investment" || catLower == "investments"
            matchesAccount && (tx.isIncome || isIncomeCat) && tx.amount > 0.0 && !tx.title.contains("Cancelled") && !tx.title.contains("Deleted")
        }

        if (sortIncomeAscending) {
            dbIncome.sortedWith(compareBy({ it.isCompleted }, { it.dateMillis }))
        } else {
            dbIncome.sortedWith(compareBy({ it.isCompleted }, { -it.dateMillis }))
        }
    }

    val displayedIncomeList = remember(
        incomeList, filterScope, statusFilter,
        uiState.currentPayWeekStart, uiState.currentPayWeekEnd,
        uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd,
        uiState.selectedMonth
    ) {
        val currentCycleMonths = setOf(
            YearMonth.from(uiState.currentPayPeriodStart),
            YearMonth.from(uiState.currentPayPeriodEnd),
            uiState.selectedMonth
        )
        val scopeFiltered = when (filterScope) {
            1 -> incomeList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                YearMonth.from(d) == uiState.selectedMonth
            }
            2 -> incomeList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                !d.isBefore(uiState.currentPayPeriodStart) && !d.isAfter(uiState.currentPayPeriodEnd)
            }
            3 -> incomeList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                !d.isBefore(uiState.currentPayWeekStart) && !d.isAfter(uiState.currentPayWeekEnd)
            }
            else -> incomeList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                YearMonth.from(d) in currentCycleMonths
            } // 0 = All Rows (scoped to months for the current cycle)
        }

        scopeFiltered.filter { tx ->
            when (statusFilter) {
                1 -> tx.getEffectiveStatus() == "due"
                2 -> tx.getEffectiveStatus() == "pending"
                3 -> tx.getEffectiveStatus() == "paid"
                4 -> tx.getEffectiveStatus() == "cleared"
                else -> true
            }
        }
    }

    val totalIncomeAmount = displayedIncomeList.sumOf { it.amount }

    val incomeFormContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Scope Filter Toggle & Date Sort Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val options = listOf("All Rows", "Full Month", "Pay Cycle", "Pay Week")
                    options.forEachIndexed { idx, label ->
                        val selected = filterScope == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0xFF107C41) else Color.Transparent)
                                .clickable { filterScope = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Date Sort Control Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sortIncomeAscending = !sortIncomeAscending }
                        .testTag("sort_income_by_date_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (sortIncomeAscending) "📅 Date ⬆" else "📅 Date ⬇",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }

            TransactionSearchAndFilterBar(
                statusFilter = statusFilter,
                onStatusFilterChange = { statusFilter = it },
                customLabels = listOf("All Statuses", "⏳ Due", "🕒 Pending", "✓ Received", "🛡️ Cleared")
            )

            // Excel Sheet Formula Bar / Status Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF107C41),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📊", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Income Spreadsheet Ledger",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "fx = SUM(B2:B${displayedIncomeList.size + 1})",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "+$%,.2f", totalIncomeAmount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${displayedIncomeList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Excel Simple Entry Row (Quick Row Insert)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF86EFAC))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isQuickInsertExpanded = !isQuickInsertExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isQuickInsertExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isQuickInsertExpanded) "Collapse" else "Expand",
                                tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "➕ Quick Insert Row",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            var freqDropdownExpanded by remember { mutableStateOf(false) }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isRecurring = !isRecurring }
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_income_recurring_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Recurring Baseline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF166534))
                            }

                            if (isRecurring) {
                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isDark) Color(0xFF047857) else Color(0xFFDCFCE7),
                                        border = BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFF86EFAC)),
                                        modifier = Modifier.clickable { freqDropdownExpanded = true }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(recurFrequency, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, color = if (isDark) Color.White else Color(0xFF14532D))
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(12.dp), tint = if (isDark) Color.White else Color(0xFF14532D))
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = freqDropdownExpanded,
                                        onDismissRequest = { freqDropdownExpanded = false }
                                    ) {
                                        listOf("Bi-Weekly", "Weekly", "Monthly", "Semi-Monthly").forEach { freq ->
                                            DropdownMenuItem(
                                                text = { Text(freq, fontSize = 12.sp, fontWeight = if (recurFrequency == freq) FontWeight.Bold else FontWeight.Normal) },
                                                onClick = {
                                                    recurFrequency = freq
                                                    freqDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isAutoPay = !isAutoPay }
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isAutoPay,
                                    onCheckedChange = { isAutoPay = it },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_income_autopay_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("⚡ Auto-Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isIncomeCompleted = !isIncomeCompleted }
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isIncomeCompleted,
                                    onCheckedChange = { isIncomeCompleted = it },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_income_completed_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = if (isIncomeCompleted) "✓ Received" else "⏳ Pending",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isIncomeCompleted) Color(0xFF10B981) else (if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309))
                                )
                            }
                        }
                    }

                    if (isQuickInsertExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DatePickerTextField(
                                value = dateInput,
                                onValueChange = { dateInput = it; errorMessage = "" },
                                label = { Text("Date", fontSize = 10.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                testTag = "enter_income_date_input",
                                modifier = Modifier.weight(1.3f)
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; errorMessage = "" },
                                label = { Text("Income Description *", fontSize = 10.sp) },
                                placeholder = { Text("e.g. Paycheck, Bonus", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .testTag("enter_income_title_input")
                            )

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it; errorMessage = "" },
                                label = { Text("Amount ($) *", fontSize = 10.sp) },
                                prefix = { Text("$", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("enter_income_amount_input")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            ExcelDropdownCell(
                                label = "DEPOSIT ACCOUNT",
                                value = selectedAccount,
                                options = uiState.allAccounts.filter { it != "All Accounts" },
                                onSelect = { selectedAccount = it },
                                modifier = Modifier.weight(1.2f),
                                testTagPrefix = "enter_income_account"
                            )

                            Button(
                                onClick = {
                                    val t = title.trim()
                                    val amt = amount.toDoubleOrNull()
                                    val parsedDate = try {
                                        LocalDate.parse(dateInput.trim())
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (t.isEmpty()) {
                                        errorMessage = "Please enter an income description."
                                        statusMessage = ""
                                    } else if (amt == null || amt <= 0.0) {
                                        errorMessage = "Please enter a valid amount."
                                        statusMessage = ""
                                    } else if (parsedDate == null) {
                                        errorMessage = "Please enter a valid date (YYYY-MM-DD)."
                                        statusMessage = ""
                                    } else {
                                        val freqSuffix = when (recurFrequency) {
                                            "Weekly" -> " (Weekly)"
                                            "Monthly" -> " (Monthly)"
                                            "Semi-Monthly" -> " (Semi-Monthly)"
                                            else -> ""
                                        }
                                        val finalCategory = if (isRecurring && freqSuffix.isNotEmpty() && !selectedCategory.contains("(")) "$selectedCategory$freqSuffix" else selectedCategory
                                        onAddIncome(t, amt, parsedDate, finalCategory, isRecurring, selectedAccount, isAutoPay, isIncomeCompleted)
                                        filterScope = 0
                                        statusMessage = "✅ Added +${String.format(Locale.US, "%.2f", amt)} row to spreadsheet!"
                                        errorMessage = ""
                                        title = ""
                                        amount = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF107C41)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(42.dp)
                                    .testTag("save_income_page_button")
                            ) {
                                Text("+ Add Row", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                        if (statusMessage.isNotEmpty()) {
                            Text(statusMessage, color = Color(0xFF15803D), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Recurring Paychecks & Income Streams summary banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF132F23) else Color(0xFFECFDF5)),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFFA7F3D0))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("💵", fontSize = 14.sp)
                            Text(
                                text = "Recurring Income Streams",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                            )
                        }

                        if (onAddIncomeSource != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDark) Color(0xFF047857) else Color(0xFF10B981),
                                modifier = Modifier.clickable { showAddSourceDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text("+ Add Recurring Stream", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // Display primary paycheck or income sources
                    if (uiState.incomeSources.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Primary Paycheck (Every 14 Days)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Next payday: ${uiState.currentPayday}",
                                    fontSize = 9.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", uiState.defaultPaycheckAmount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                )
                                if (onUpdateDefaultPaycheck != null) {
                                    IconButton(
                                        onClick = { showEditDefaultPaycheckDialog = true },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Paycheck", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        uiState.incomeSources.forEach { src ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = src.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = src.frequency,
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "+$%,.2f", src.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                    )
                                    if (onDeleteIncomeSource != null) {
                                        IconButton(
                                            onClick = { onDeleteIncomeSource(src) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddSourceDialog && onAddIncomeSource != null) {
                var newName by remember { mutableStateOf("") }
                var newAmount by remember { mutableStateOf("") }
                var newFreq by remember { mutableStateOf("Bi-Weekly") }
                AlertDialog(
                    onDismissRequest = { showAddSourceDialog = false },
                    title = { Text("Add Recurring Income Stream", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = newName,
                                onValueChange = { newName = it },
                                label = { Text("Income Name / Employer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newAmount,
                                onValueChange = { newAmount = it },
                                label = { Text("Amount ($)") },
                                prefix = { Text("$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Column {
                                Text("Frequency:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Bi-Weekly", "Weekly", "Monthly").forEach { f ->
                                        FilterChip(
                                            selected = newFreq == f,
                                            onClick = { newFreq = f },
                                            label = { Text(f, fontSize = 9.5.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = newAmount.toDoubleOrNull()
                                if (newName.isNotBlank() && amt != null && amt > 0.0) {
                                    onAddIncomeSource(newName.trim(), amt, newFreq)
                                    showAddSourceDialog = false
                                }
                            }
                        ) {
                            Text("Add Stream")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddSourceDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showEditDefaultPaycheckDialog && onUpdateDefaultPaycheck != null) {
                var editAmount by remember { mutableStateOf(String.format(Locale.US, "%.2f", uiState.defaultPaycheckAmount)) }
                AlertDialog(
                    onDismissRequest = { showEditDefaultPaycheckDialog = false },
                    title = { Text("Edit Default Paycheck", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                    text = {
                        OutlinedTextField(
                            value = editAmount,
                            onValueChange = { editAmount = it },
                            label = { Text("Default Paycheck Amount ($)") },
                            prefix = { Text("$") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = editAmount.toDoubleOrNull()
                                if (amt != null && amt >= 0.0) {
                                    onUpdateDefaultPaycheck(amt)
                                    showEditDefaultPaycheckDialog = false
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEditDefaultPaycheckDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    val incomeListContent: @Composable () -> Unit = {
        // Income Spreadsheet Cards View
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (displayedIncomeList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No income rows found for current filter selection.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                displayedIncomeList.forEach { tx ->
                    val txDate = DateUtils.millisToLocalDate(tx.dateMillis)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditIncome(tx) }
                            .testTag("income_card_${tx.id}"),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        ),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Top Row: Title + Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = String.format(Locale.US, "+$%,.2f", tx.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFF4ADE80) else Color(0xFF10B981)
                                )
                            }

                            // Bottom Details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    // Account Badge
                                    val accStyle = getAccountColorStyle(tx.accountName, isDark)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accStyle.containerColor,
                                        border = BorderStroke(0.8.dp, accStyle.borderColor)
                                    ) {
                                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.5.dp)
                                                    .background(accStyle.borderColor)
                                            )
                                            Text(
                                                text = "💳 " + tx.accountName,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accStyle.contentColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    // Date Chip
                                    Surface(
                                        shape = RoundedCornerShape(3.5.dp),
                                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.6.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            text = txDate.format(formatter),
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        )
                                    }

                                    if (tx.isRecurring) {
                                        Surface(
                                            shape = RoundedCornerShape(3.5.dp),
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = "🔄 Bi-Weekly",
                                                fontSize = 7.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }
                                    if (tx.isAutoPay) {
                                        EditableAutoPayTag(
                                            accountName = tx.accountName,
                                            isAutoPay = tx.isAutoPay,
                                            allAccounts = uiState.allAccounts,
                                            onUpdateAutoPaySource = { newAccount, enabled ->
                                                onEditIncome(tx.copy(accountName = newAccount, isAutoPay = enabled))
                                            }
                                        )
                                    }

                                    TransactionStatusChip(
                                        status = tx.getEffectiveStatus(),
                                        isIncome = true,
                                        fontSize = 7.sp,
                                        onStatusChange = { newSt ->
                                            onEditIncome(tx.copy(status = newSt, isCompleted = newSt in listOf("paid", "cleared")))
                                        },
                                        testTag = "income_status_chip_${tx.id}"
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (onToggleIncome != null) {
                                        IconButton(
                                            onClick = { onToggleIncome(tx) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("toggle_income_${tx.id}")
                                        ) {
                                            Icon(
                                                imageVector = if (tx.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                                contentDescription = if (tx.isCompleted) "Mark Pending" else "Mark Received",
                                                tint = if (tx.isCompleted) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onEditIncome(tx) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("edit_page_income_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit row",
                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteIncome(tx) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_page_income_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete row",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Income Total Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF059669) else Color(0xFF86EFAC))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL INCOME (SUM):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFF86EFAC) else Color(0xFF14532D)
                    )
                    Text(
                        text = String.format(Locale.US, "+$%,.2f", totalIncomeAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSplitColumns = isWideScreen && maxWidth >= 540.dp
        if (useSplitColumns) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(0.45f)) {
                incomeFormContent()
            }
            Box(modifier = Modifier.weight(0.55f)) {
                incomeListContent()
            }
        }
    } else {
        var incomeSectionOrder by rememberSaveable { mutableStateOf(listOf("income_form", "income_list")) }
        var isIncomeReorderActive by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isIncomeReorderActive) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isIncomeReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (isIncomeReorderActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isIncomeReorderActive) "Income Sections Unlocked • Move then Lock" else "Custom Income Layout • Locked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isIncomeReorderActive) {
                                Button(
                                    onClick = { isIncomeReorderActive = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { isIncomeReorderActive = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(
                                onClick = {
                                    incomeSectionOrder = listOf("income_form", "income_list")
                                    isIncomeReorderActive = false
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            incomeSectionOrder.forEachIndexed { index, sectionKey ->
                val sectionTitle = if (sectionKey == "income_form") "Income Entry & Formula Ledger" else "Income Spreadsheet Transactions"
                MovableSectionWrapper(
                    sectionKey = sectionKey,
                    sectionTitle = sectionTitle,
                    index = index,
                    totalSections = incomeSectionOrder.size,
                    isReordering = isIncomeReorderActive,
                    onToggleReorder = { isIncomeReorderActive = !isIncomeReorderActive },
                    onMoveUp = {
                        if (index > 0) {
                            val list = incomeSectionOrder.toMutableList()
                            val temp = list[index]
                            list[index] = list[index - 1]
                            list[index - 1] = temp
                            incomeSectionOrder = list
                        }
                    },
                    onMoveDown = {
                        if (index < incomeSectionOrder.size - 1) {
                            val list = incomeSectionOrder.toMutableList()
                            val temp = list[index]
                            list[index] = list[index + 1]
                            list[index + 1] = temp
                            incomeSectionOrder = list
                        }
                    },
                    isSubSection = true
                ) {
                    if (sectionKey == "income_form") {
                        incomeFormContent()
                    } else {
                        incomeListContent()
                    }
                }
            }
        }
    }
}
}

@Composable
fun EnterExpensesView(
    uiState: BudgetUiState,
    onAddExpense: (title: String, amount: Double, dueDate: LocalDate, category: String, isRecurring: Boolean, accountName: String, isAutoPay: Boolean, isCompleted: Boolean, status: String) -> Unit,
    onDeleteExpense: (TransactionEntity) -> Unit,
    onEditExpense: (TransactionEntity) -> Unit = {},
    onDuplicateExpense: ((TransactionEntity) -> Unit)? = null,
    onToggleExpense: ((TransactionEntity) -> Unit)? = null,
    onUpdateExpenseStatus: ((TransactionEntity, String) -> Unit)? = null,
    onToggleReminder: ((TransactionEntity, Int) -> Unit)? = null,
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val isDark = uiState.isDarkMode
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by remember { mutableStateOf("Core Bills") }
    var selectedAccount by remember { mutableStateOf(if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName) }
    var isRecurring by remember { mutableStateOf(false) }
    var isAutoPay by remember { mutableStateOf(false) }
    var expenseStatus by remember { mutableStateOf("due") }
    val isExpenseCompleted = expenseStatus in listOf("paid", "cleared")
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isQuickInsertExpanded by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSyncingSheets by remember { mutableStateOf(false) }

    var filterScope by remember { mutableIntStateOf(2) } // 0 = All Rows, 1 = Full Month, 2 = Pay Cycle, 3 = Pay Week
    var sortExpensesAscending by remember { mutableStateOf(false) } // false = Newest First, true = Oldest First
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Pending, 2 = Cleared
    var categoryFilter by remember { mutableStateOf<String?>(null) } // null = All, "Credit Cards", "Core Bills"

    LaunchedEffect(uiState.selectedMonth) {
        val today = LocalDate.now()
        if (YearMonth.from(today) == uiState.selectedMonth) {
            dueDateInput = today.toString()
        } else {
            dueDateInput = uiState.selectedMonth.atDay(1).toString()
        }
    }

    LaunchedEffect(uiState.activeAccountName) {
        selectedAccount = if (uiState.activeAccountName == "All Accounts") (uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else uiState.activeAccountName
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val dateRangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val weekRangeText = remember(uiState.currentPayWeekStart, uiState.currentPayWeekEnd) {
        "${uiState.currentPayWeekStart.format(dateRangeFormatter)} – ${uiState.currentPayWeekEnd.format(dateRangeFormatter)}"
    }
    val cycleRangeText = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd) {
        "${uiState.currentPayPeriodStart.format(dateRangeFormatter)} – ${uiState.currentPayPeriodEnd.format(dateRangeFormatter)}"
    }

    val expenseCategories = CategoryTheme.expenseCategories.map { it.name }

    // Filter expenses (including planned bills for active month)
    val expensesList = remember(uiState.allTransactions, uiState.plannedExpenses, uiState.selectedMonth, uiState.activeAccountName, sortExpensesAscending) {
        val primaryAcc = uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
        val dbExpenses = uiState.allTransactions.filter { tx ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    tx.accountName.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (primaryAcc.isNotEmpty() && uiState.activeAccountName.equals(primaryAcc, ignoreCase = true) && (tx.accountName.isBlank() || tx.accountName.equals("Primary Account", ignoreCase = true) || tx.accountName.equals("Primary", ignoreCase = true)))
            val catLower = tx.category.lowercase(java.util.Locale.US)
            val isIncomeCat = catLower == "paycheck" || catLower == "income" || catLower == "salary" || catLower == "bonus" || catLower == "deposit" || catLower == "freelance" || catLower == "investment" || catLower == "investments"
            matchesAccount && !tx.isIncome && !isIncomeCat && tx.amount > 0.0 && !tx.title.contains("Cancelled") && !tx.title.contains("Deleted")
        }

        val month = uiState.selectedMonth
        val syntheticPlanned = uiState.plannedExpenses.mapIndexedNotNull { idx, planned ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    planned.targetAccount.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (primaryAcc.isNotEmpty() && uiState.activeAccountName.equals(primaryAcc, ignoreCase = true) && (planned.targetAccount.isBlank() || planned.targetAccount.equals("Primary Account", ignoreCase = true) || planned.targetAccount.equals("Primary", ignoreCase = true)))
            if (!matchesAccount) return@mapIndexedNotNull null

            val dueDate = try {
                val orig = LocalDate.parse(planned.dueDate)
                val day = orig.dayOfMonth.coerceIn(1, month.lengthOfMonth())
                LocalDate.of(month.year, month.month, day)
            } catch (e: Exception) {
                val day = (planned.dueDate.filter { it.isDigit() }.toIntOrNull() ?: 15).coerceIn(1, month.lengthOfMonth())
                LocalDate.of(month.year, month.month, day)
            }
            val dStart = DateUtils.localDateToMillis(dueDate)
            val dEnd = DateUtils.localDateToMillis(dueDate.plusDays(1))
            val existsInDb = uiState.allTransactions.any {
                (it.title.equals(planned.title, ignoreCase = true) || it.title.contains(planned.title, ignoreCase = true)) && it.dateMillis >= dStart && it.dateMillis < dEnd
            }
            if (!existsInDb) {
                val synthId = -kotlin.math.abs((planned.id.toLong() * 10000L + dueDate.dayOfYear.toLong()).toInt()) - 5000
                val targetAcc = if (planned.targetAccount.isBlank() || planned.targetAccount.equals("Primary Account", ignoreCase = true) || planned.targetAccount.equals("Primary", ignoreCase = true)) primaryAcc else planned.targetAccount
                TransactionEntity(
                    id = synthId,
                    title = planned.title + " (Planned)",
                    amount = planned.minAmount,
                    dateMillis = DateUtils.localDateToMillis(dueDate),
                    category = "Utilities",
                    isIncome = false,
                    isRecurring = true,
                    accountName = targetAcc
                )
            } else null
        }

        val combined = dbExpenses + syntheticPlanned
        if (sortExpensesAscending) {
            combined.sortedWith(compareBy({ it.isCompleted }, { it.dateMillis }))
        } else {
            combined.sortedWith(compareBy({ it.isCompleted }, { -it.dateMillis }))
        }
    }

    val displayedExpensesList = remember(
        expensesList, filterScope, statusFilter, categoryFilter,
        uiState.currentPayWeekStart, uiState.currentPayWeekEnd,
        uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd,
        uiState.selectedMonth
    ) {
        val currentCycleMonths = setOf(
            YearMonth.from(uiState.currentPayPeriodStart),
            YearMonth.from(uiState.currentPayPeriodEnd),
            uiState.selectedMonth
        )
        val scopeFiltered = when (filterScope) {
            1 -> expensesList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                YearMonth.from(d) == uiState.selectedMonth
            }
            2 -> expensesList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                !d.isBefore(uiState.currentPayPeriodStart) && !d.isAfter(uiState.currentPayPeriodEnd)
            }
            3 -> expensesList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                !d.isBefore(uiState.currentPayWeekStart) && !d.isAfter(uiState.currentPayWeekEnd)
            }
            else -> expensesList.filter { tx ->
                val d = DateUtils.millisToLocalDate(tx.dateMillis)
                YearMonth.from(d) in currentCycleMonths
            } // 0 = All Rows (scoped to months for the current cycle)
        }

        val statusFiltered = scopeFiltered.filter { tx ->
            when (statusFilter) {
                1 -> tx.getEffectiveStatus() == "due"
                2 -> tx.getEffectiveStatus() == "pending"
                3 -> tx.getEffectiveStatus() == "paid"
                4 -> tx.getEffectiveStatus() == "cleared"
                else -> true
            }
        }

        if (categoryFilter == null) {
            statusFiltered
        } else {
            statusFiltered.filter { tx ->
                val (parent, _) = CategoryTheme.parseCategory(tx.category)
                parent.equals(categoryFilter, ignoreCase = true)
            }
        }
    }

    val totalExpensesAmount = displayedExpensesList.sumOf { it.amount }

    val expenseFormContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Scope Filter Toggle & Date Sort Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val options = listOf("All Rows", "Full Month", "Pay Cycle", "Pay Week")
                    options.forEachIndexed { idx, label ->
                        val selected = filterScope == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0xFF1E293B) else Color.Transparent)
                                .clickable { filterScope = idx }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else (if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155)),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Date Sort Control Button
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { sortExpensesAscending = !sortExpensesAscending }
                        .testTag("sort_expenses_by_date_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (sortExpensesAscending) "📅 Date ⬆" else "📅 Date ⬇",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }

            // Unified Filter Row: All Statuses, Pending, Cleared, Credit Cards, and Core Bills on the same line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusLabels = listOf("All Statuses", "⏳ Due", "🕒 Pending", "💳 Paid", "🛡️ Cleared")
                statusLabels.forEachIndexed { index, label ->
                    val selected = statusFilter == index
                    FilterChip(
                        selected = selected,
                        onClick = { statusFilter = index },
                        label = {
                            Text(
                                text = label,
                                fontSize = 9.5.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.height(24.dp).testTag("filter_chip_$index")
                    )
                }

                FilterChip(
                    selected = categoryFilter == "Credit Cards",
                    onClick = {
                        categoryFilter = if (categoryFilter == "Credit Cards") null else "Credit Cards"
                    },
                    label = {
                        Text(
                            text = "💳 Credit Cards",
                            fontSize = 9.5.sp,
                            fontWeight = if (categoryFilter == "Credit Cards") FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(24.dp).testTag("filter_chip_credit_cards")
                )

                FilterChip(
                    selected = categoryFilter == "Core Bills",
                    onClick = {
                        categoryFilter = if (categoryFilter == "Core Bills") null else "Core Bills"
                    },
                    label = {
                        Text(
                            text = "📋 Core Bills",
                            fontSize = 9.5.sp,
                            fontWeight = if (categoryFilter == "Core Bills") FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    modifier = Modifier.height(24.dp).testTag("filter_chip_core_bills")
                )
            }

            // Excel Sheet Formula Bar / Status Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E293B),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📉", fontSize = 18.sp)
                        Column {
                            Text(
                                text = "Expenses & Bills Ledger",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "fx = SUM(D2:D${displayedExpensesList.size + 1})",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "-$%,.2f", totalExpensesAmount),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFCA5A5),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "(${displayedExpensesList.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Excel Quick Entry Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF451A1A) else Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFECACA))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isQuickInsertExpanded = !isQuickInsertExpanded }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isQuickInsertExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isQuickInsertExpanded) "Collapse" else "Expand",
                                tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "➕ Quick Insert Expense Row",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isRecurring = !isRecurring }
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_expense_recurring_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Recurring Baseline", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { isAutoPay = !isAutoPay }
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isAutoPay,
                                    onCheckedChange = { isAutoPay = it },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_expense_autopay_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("⚡ Auto-Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            ) {
                                Checkbox(
                                    checked = isExpenseCompleted,
                                    onCheckedChange = { checked ->
                                        expenseStatus = if (checked) "paid" else "due"
                                    },
                                    modifier = Modifier
                                        .size(16.dp)
                                        .testTag("enter_expense_completed_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                TransactionStatusChip(
                                    status = expenseStatus,
                                    isIncome = false,
                                    fontSize = 7.sp,
                                    onStatusChange = { expenseStatus = it },
                                    testTag = "enter_expense_status_chip"
                                )
                            }
                            if (isAutoPay) {
                                EditableAutoPayTag(
                                    accountName = selectedAccount,
                                    isAutoPay = isAutoPay,
                                    allAccounts = uiState.allAccounts,
                                    onUpdateAutoPaySource = { newAccount, enabled ->
                                        selectedAccount = newAccount
                                        isAutoPay = enabled
                                    }
                                )
                            }
                        }
                    }

                    if (isQuickInsertExpanded) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DatePickerTextField(
                                value = dueDateInput,
                                onValueChange = { dueDateInput = it; errorMessage = "" },
                                label = { Text("Due Date", fontSize = 10.sp) },
                                placeholder = { Text("YYYY-MM-DD", fontSize = 10.sp) },
                                testTag = "enter_expense_duedate_input",
                                modifier = Modifier.weight(1.3f)
                            )

                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it; errorMessage = "" },
                                label = { Text("Expense Title *", fontSize = 10.sp) },
                                placeholder = { Text("e.g. Electric Bill, Rent", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .testTag("enter_expense_title_input")
                            )

                            OutlinedTextField(
                                value = amount,
                                onValueChange = { amount = it; errorMessage = "" },
                                label = { Text("Amount ($) *", fontSize = 10.sp) },
                                prefix = { Text("$", fontSize = 11.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.1f)
                                    .testTag("enter_expense_amount_input")
                            )
                        }

                        // Quick Date Suggestions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val today = LocalDate.now()
                            val quickDates = listOf(
                                "Today" to today,
                                "+7 Days" to today.plusDays(7),
                                "1st" to uiState.selectedMonth.atDay(1),
                                "15th" to uiState.selectedMonth.atDay(minOf(15, uiState.selectedMonth.lengthOfMonth()))
                            )
                            quickDates.forEach { (lbl, qDate) ->
                                SuggestionChip(
                                    onClick = { dueDateInput = qDate.toString() },
                                    label = { Text(lbl, fontSize = 8.5.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }

                        // Subcategory Picker
                        ExpenseCategorySubCategoryPicker(
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            ExcelDropdownCell(
                                label = "PAYMENT ACCOUNT",
                                value = selectedAccount,
                                options = uiState.allAccounts.filter { it != "All Accounts" },
                                onSelect = { selectedAccount = it },
                                modifier = Modifier.weight(1.2f),
                                testTagPrefix = "enter_expense_account"
                            )

                            Button(
                                onClick = {
                                    val t = title.trim()
                                    val amt = amount.toDoubleOrNull()
                                    val parsedDate = try {
                                        LocalDate.parse(dueDateInput.trim())
                                    } catch (e: Exception) {
                                        null
                                    }

                                    if (t.isEmpty()) {
                                        errorMessage = "Please enter an expense title."
                                        statusMessage = ""
                                    } else if (amt == null || amt <= 0.0) {
                                        errorMessage = "Please enter a valid amount."
                                        statusMessage = ""
                                    } else if (parsedDate == null) {
                                        errorMessage = "Please enter a valid due date (YYYY-MM-DD)."
                                        statusMessage = ""
                                    } else {
                                        onAddExpense(t, amt, parsedDate, selectedCategory, isRecurring, selectedAccount, isAutoPay, isExpenseCompleted, expenseStatus)
                                        filterScope = 0
                                        statusMessage = "✅ Added '$t' (${String.format(Locale.US, "%.2f", amt)}) to expense spreadsheet!"
                                        errorMessage = ""
                                        title = ""
                                        amount = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .weight(1.0f)
                                    .height(42.dp)
                                    .testTag("save_expense_page_button")
                            ) {
                                Text("+ Add Row", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        if (errorMessage.isNotEmpty()) {
                            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                        }
                        if (statusMessage.isNotEmpty()) {
                            Text(statusMessage, color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val expenseListContent: @Composable () -> Unit = {
        // Expense Spreadsheet Cards View
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (displayedExpensesList.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color.White),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No expense rows found for current filter selection.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            } else {
                val today = LocalDate.now()
                displayedExpensesList.forEach { tx ->
                    val txDate = DateUtils.millisToLocalDate(tx.dateMillis)
                    val daysDiff = ChronoUnit.DAYS.between(today, txDate)
                    val isSettled = tx.isPaidOrCleared
                    val cardBorder = when {
                        !isSettled && daysDiff < 0 -> BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                        !isSettled && daysDiff in 0L..2L -> BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
                        else -> BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                    }
                    val cardBg = when {
                        !isSettled && daysDiff < 0 -> if (isDark) Color(0xFF450A0A).copy(alpha = 0.35f) else Color(0xFFFEF2F2)
                        !isSettled && daysDiff in 0L..2L -> if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFFBEB)
                        else -> if (isDark) Color(0xFF1E293B) else Color.White
                    }

                    SwipeableTransactionRow(
                        transaction = tx,
                        onDelete = { onDeleteExpense(tx) },
                        onEdit = { onEditExpense(tx) },
                        onDuplicate = { onDuplicateExpense?.invoke(tx) },
                        onUpdateStatus = { newSt ->
                            if (onUpdateExpenseStatus != null) {
                                onUpdateExpenseStatus(tx, newSt)
                            } else {
                                onToggleExpense?.invoke(tx)
                            }
                        },
                        onToggleCompleted = { onToggleExpense?.invoke(tx) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditExpense(tx) }
                                .testTag("expense_card_${tx.id}"),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = cardBg
                            ),
                            border = cardBorder,
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            // Top Row: Title + Amount
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tx.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = String.format(Locale.US, "-$%,.2f", tx.amount),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFEF4444)
                                )
                            }

                            // Bottom Details Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .horizontalScroll(rememberScrollState())
                                ) {
                                    // Category & Subcategory Badge
                                    val (catParent, catSub) = CategoryTheme.parseCategory(tx.category)
                                    val catColor = CategoryTheme.getColor(tx.category)
                                    val catEmoji = CategoryTheme.getEmoji(tx.category)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = catColor.copy(alpha = if (isDark) 0.22f else 0.12f),
                                        border = BorderStroke(0.8.dp, catColor.copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = "$catEmoji $catParent",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = catColor,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    // Account Badge
                                    val accStyle = getAccountColorStyle(tx.accountName, isDark)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accStyle.containerColor,
                                        border = BorderStroke(0.8.dp, accStyle.borderColor)
                                    ) {
                                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(2.5.dp)
                                                    .background(accStyle.borderColor)
                                            )
                                            Text(
                                                text = "💳 " + tx.accountName,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accStyle.contentColor,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    // Date Chip
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                        border = BorderStroke(0.8.dp, if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1))
                                    ) {
                                        Text(
                                            text = txDate.format(formatter),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }

                                    if (tx.isRecurring) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                        ) {
                                            Text(
                                                text = "🔄 Monthly",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    if (tx.isAutoPay) {
                                        EditableAutoPayTag(
                                            accountName = tx.accountName,
                                            isAutoPay = tx.isAutoPay,
                                            allAccounts = uiState.allAccounts,
                                            onUpdateAutoPaySource = { newAccount, enabled ->
                                                onEditExpense(tx.copy(accountName = newAccount, isAutoPay = enabled))
                                            }
                                        )
                                    }
                                    if (tx.reminderDaysBefore >= 0 && !tx.isCompleted) {
                                        val reminderText = when (tx.reminderDaysBefore) {
                                            0 -> "🔔 Day of"
                                            1 -> "🔔 1d before"
                                            3 -> "🔔 3d before"
                                            7 -> "🔔 1w before"
                                            else -> "🔔 Active"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = reminderText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    TransactionStatusChip(
                                        status = tx.getEffectiveStatus(),
                                        isIncome = false,
                                        fontSize = 7.sp,
                                        onStatusChange = { newSt ->
                                            if (onUpdateExpenseStatus != null) {
                                                onUpdateExpenseStatus(tx, newSt)
                                            } else {
                                                onEditExpense(tx.copy(status = newSt, isCompleted = newSt in listOf("paid", "cleared")))
                                            }
                                        },
                                        testTag = "expense_status_chip_${tx.id}"
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (onToggleExpense != null || onUpdateExpenseStatus != null) {
                                        val curStatus = tx.getEffectiveStatus()
                                        val isDone = curStatus in listOf("paid", "cleared")
                                        IconButton(
                                            onClick = {
                                                if (onUpdateExpenseStatus != null) {
                                                    onUpdateExpenseStatus(tx, TxStatus.nextStatus(curStatus))
                                                } else {
                                                    onToggleExpense?.invoke(tx)
                                                }
                                            },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("toggle_page_expense_${tx.id}")
                                        ) {
                                            val circleBg = when (curStatus) {
                                                "cleared" -> Color(0xFF10B981)
                                                "paid" -> Color(0xFF3B82F6)
                                                "pending" -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            }
                                            val circleBorder = when (curStatus) {
                                                "cleared" -> Color(0xFF10B981)
                                                "paid" -> Color(0xFF3B82F6)
                                                "pending" -> Color(0xFFF59E0B)
                                                else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .border(
                                                        width = 2.dp,
                                                        color = circleBorder,
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = circleBg,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isDone) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = if (curStatus == "cleared") "Cleared" else "Paid",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (onToggleReminder != null && !tx.isIncome) {
                                        var showReminderMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showReminderMenu = true },
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("reminder_page_expense_${tx.id}")
                                            ) {
                                                Icon(
                                                    imageVector = if (tx.reminderDaysBefore >= 0) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                                                    contentDescription = "Set Due Reminder",
                                                    tint = if (tx.reminderDaysBefore >= 0) Color(0xFF10B981) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showReminderMenu,
                                                onDismissRequest = { showReminderMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("No reminder") },
                                                    onClick = {
                                                        onToggleReminder(tx, -1)
                                                        showReminderMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("On due date (Day of)") },
                                                    onClick = {
                                                        onToggleReminder(tx, 0)
                                                        showReminderMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("1 day before") },
                                                    onClick = {
                                                        onToggleReminder(tx, 1)
                                                        showReminderMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("3 days before") },
                                                    onClick = {
                                                        onToggleReminder(tx, 3)
                                                        showReminderMenu = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("1 week before") },
                                                    onClick = {
                                                        onToggleReminder(tx, 7)
                                                        showReminderMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (onDuplicateExpense != null) {
                                        IconButton(
                                            onClick = { onDuplicateExpense(tx) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("duplicate_page_expense_${tx.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Duplicate expense",
                                                tint = if (isDark) Color(0xFFA78BFA) else Color(0xFF7C3AED),
                                                modifier = Modifier.size(17.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onEditExpense(tx) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("edit_page_expense_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit row",
                                            tint = if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onDeleteExpense(tx) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_page_expense_${tx.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete row",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // Expense Total Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF451A1A) else Color(0xFFFEE2E2)
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF991B1B) else Color(0xFFFCA5A5))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TOTAL EXPENSES (SUM):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D)
                    )
                    Text(
                        text = String.format(Locale.US, "-$%,.2f", totalExpensesAmount),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSplitColumns = isWideScreen && maxWidth >= 540.dp
        if (useSplitColumns) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(0.45f)) {
                expenseFormContent()
            }
            Box(modifier = Modifier.weight(0.55f)) {
                expenseListContent()
            }
        }
    } else {
        var expenseSectionOrder by rememberSaveable { mutableStateOf(listOf("expense_form", "expense_list")) }
        var isExpenseReorderActive by rememberSaveable { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isExpenseReorderActive) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isExpenseReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                imageVector = if (isExpenseReorderActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isExpenseReorderActive) "Expense Sections Unlocked • Move then Lock" else "Custom Expense Layout • Locked",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isExpenseReorderActive) {
                                Button(
                                    onClick = { isExpenseReorderActive = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                FilledTonalButton(
                                    onClick = { isExpenseReorderActive = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            TextButton(
                                onClick = {
                                    expenseSectionOrder = listOf("expense_form", "expense_list")
                                    isExpenseReorderActive = false
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                            ) {
                                Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            expenseSectionOrder.forEachIndexed { index, sectionKey ->
                val sectionTitle = if (sectionKey == "expense_form") "Expense Entry & Formula Ledger" else "Expense Spreadsheet Transactions"
                MovableSectionWrapper(
                    sectionKey = sectionKey,
                    sectionTitle = sectionTitle,
                    index = index,
                    totalSections = expenseSectionOrder.size,
                    isReordering = isExpenseReorderActive,
                    onToggleReorder = { isExpenseReorderActive = !isExpenseReorderActive },
                    onMoveUp = {
                        if (index > 0) {
                            val list = expenseSectionOrder.toMutableList()
                            val temp = list[index]
                            list[index] = list[index - 1]
                            list[index - 1] = temp
                            expenseSectionOrder = list
                        }
                    },
                    onMoveDown = {
                        if (index < expenseSectionOrder.size - 1) {
                            val list = expenseSectionOrder.toMutableList()
                            val temp = list[index]
                            list[index] = list[index + 1]
                            list[index + 1] = temp
                            expenseSectionOrder = list
                        }
                    },
                    isSubSection = true
                ) {
                    if (sectionKey == "expense_form") {
                        expenseFormContent()
                    } else {
                        expenseListContent()
                    }
                }
            }
        }
    }
}
}

@Composable
fun MovableSectionWrapper(
    sectionKey: String,
    sectionTitle: String,
    index: Int,
    totalSections: Int,
    isReordering: Boolean,
    onToggleReorder: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
    isSubSection: Boolean = false,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    if (!isReordering) {
        // When not reordering, render content cleanly without any banner or header taking up space
        Box(modifier = modifier.fillMaxWidth()) {
            content()
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = if (isSubSection) 1.dp else 2.dp)
                .animateContentSize(),
            shape = RoundedCornerShape(if (isSubSection) 10.dp else 12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSubSection) 0.08f else 0.12f),
            border = BorderStroke(
                width = 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Drag handle & Section header bar with Move controls
                Surface(
                    shape = RoundedCornerShape(if (isSubSection) 10.dp else 12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleReorder()
                        }
                        .testTag("section_header_$sectionKey")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = if (isSubSection) 8.dp else 10.dp, vertical = if (isSubSection) 3.dp else 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = "Unlocked",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isSubSection) 18.dp else 20.dp)
                            )
                            Text(
                                text = sectionTitle,
                                fontSize = if (isSubSection) 11.sp else 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        // Move Up / Move Down buttons & Lock control
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (index > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable(enabled = index > 0) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onMoveUp()
                                    }
                                    .testTag("move_up_$sectionKey")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = CircleShape,
                                color = if (index < totalSections - 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .size(26.dp)
                                    .clickable(enabled = index < totalSections - 1) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onMoveDown()
                                    }
                                    .testTag("move_down_$sectionKey")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = if (index < totalSections - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Prominent Lock Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onToggleReorder()
                                    }
                                    .testTag("lock_section_$sectionKey")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Lock",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Lock",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Section body
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isWideScreen: Boolean = false
) {
    data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val items = listOf(
        TabItem("Overview", Icons.Default.Home),
        TabItem("Income", Icons.Default.TrendingUp),
        TabItem("Expenses", Icons.Default.ShoppingCart),
        TabItem("Upcoming", Icons.Default.Schedule),
        TabItem("Debt", Icons.Default.CreditCard),
        TabItem("Calendar", Icons.Default.DateRange),
        TabItem("AI Hub", Icons.Default.AutoAwesome),
        TabItem("Settings", Icons.Default.Settings)
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fitAllTabs = isWideScreen && maxWidth >= 720.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (fitAllTabs) Modifier else Modifier.horizontalScroll(rememberScrollState()))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        modifier = Modifier
                            .then(if (fitAllTabs) Modifier.weight(1f) else Modifier)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onTabSelected(index) }
                            .testTag("nav_tab_$index")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoldableNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompactHeight: Boolean = false
) {
    data class NavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tabIndex: Int)
    val items = listOf(
        NavItem("Overview", Icons.Default.Home, 0),
        NavItem("Income", Icons.Default.TrendingUp, 1),
        NavItem("Expenses", Icons.Default.ShoppingCart, 2),
        NavItem("Upcoming", Icons.Default.Schedule, 3),
        NavItem("Summary", Icons.Default.Summarize, 8),
        NavItem("Debt", Icons.Default.CreditCard, 4),
        NavItem("Calendar", Icons.Default.DateRange, 5),
        NavItem("AI Hub", Icons.Default.AutoAwesome, 6),
        NavItem("Settings", Icons.Default.Settings, 7)
    )
    val haptic = LocalHapticFeedback.current

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = if (isCompactHeight) 4.dp else 16.dp, bottom = if (isCompactHeight) 2.dp else 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.size(if (isCompactHeight) 30.dp else 40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "PayCycle Fold",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(if (isCompactHeight) 16.dp else 22.dp)
                        )
                    }
                }
                if (!isCompactHeight) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PayCycle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "FOLD 8",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(if (isCompactHeight) 1.dp else 4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.tabIndex
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(item.tabIndex)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(if (isCompactHeight) 18.dp else 22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = if (isCompactHeight) 9.5.sp else 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_rail_${item.tabIndex}")
                )
            }
        }
    }
}

@Composable
fun FoldableBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    data class NavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val tabIndex: Int)
    val items = listOf(
        NavItem("Cycles", Icons.Default.Refresh, 0),
        NavItem("Income", Icons.Default.Add, 1),
        NavItem("Expenses", Icons.Default.ShoppingCart, 2),
        NavItem("Upcoming", Icons.Default.Schedule, 3),
        NavItem("Summary", Icons.Default.Summarize, 8),
        NavItem("Calendar", Icons.Default.DateRange, 5)
    )

    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 8.dp,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedTab == item.tabIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_bottom_${item.tabIndex}")
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTabSelected(item.tabIndex)
                        }
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun SafeToSpendHeroCard(
    uiState: BudgetUiState,
    modifier: Modifier = Modifier,
    isWideScreen: Boolean = false,
    isCompactHeight: Boolean = false,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null,
    onOpenTransfer: (() -> Unit)? = null
) {
    val today = LocalDate.now()
    val period = uiState.currentPayPeriod
    val df = remember { java.time.format.DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    var isRecapDismissed by rememberSaveable { mutableStateOf(false) }
    var isDeficitBannerDismissed by rememberSaveable { mutableStateOf(false) }
    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val haptic = LocalHapticFeedback.current
    val toggleAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onToggleExpand != null) onToggleExpand() else internalExpanded = !internalExpanded
    }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "safe_arrow_rot")

    val safeToSpend = period?.remainingBalance ?: uiState.monthlyRemainingBalance
    val totalIncome = period?.totalIncome ?: uiState.monthlyTotalIncome
    val totalExpenses = period?.totalExpenses ?: uiState.monthlyTotalExpenses
    val totalAvailable = period?.totalAvailable ?: (totalIncome)
    val spentPct = if (totalAvailable > 0) ((totalExpenses / totalAvailable).coerceIn(0.0, 1.0)).toFloat() else 0f

    val nextPayday = period?.endDate?.plusDays(1) ?: today.plusDays(14)
    val daysUntilPayday = java.time.temporal.ChronoUnit.DAYS.between(today, nextPayday).coerceAtLeast(0)
    val dailyAllowance = if (daysUntilPayday > 0 && safeToSpend > 0) safeToSpend / daysUntilPayday else 0.0

    // Feature 1: Safe-to-Spend Runway & Cashflow Deficit Warning Calculations
    val pStart = period?.startDate ?: today
    val pEnd = period?.endDate ?: today.plusDays(13)
    val upcomingBillsInCycle = remember(uiState.plannedExpenses, uiState.allTransactions, uiState.allDebts, pStart, pEnd, uiState.activeAccountName) {
        UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = uiState.allTransactions,
            plannedExpenses = uiState.plannedExpenses,
            activeAccountName = uiState.activeAccountName,
            today = today,
            payPeriodStart = pStart,
            payPeriodEnd = pEnd,
            allDebts = uiState.allDebts
        ).filter { item ->
            val tx = item.transactionRef
            !(tx?.isPaidOrCleared == true || tx?.isCompleted == true)
        }
    }
    val upcomingBillsTotal = upcomingBillsInCycle.sumOf { it.amount }
    val isDeficit = safeToSpend < 0.0
    val isLowRunway = !isDeficit && (safeToSpend < upcomingBillsTotal * 0.3 || (dailyAllowance < 15.0 && daysUntilPayday > 2))

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Cashflow Deficit & Low Runway Warning Banner
        AnimatedVisibility(
            visible = (isDeficit || isLowRunway) && !isDeficitBannerDismissed,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            val isDark = isSystemInDarkTheme()
            val bannerBg = if (isDeficit) {
                if (isDark) Color(0xFF450A0A) else Color(0xFFFEF2F2)
            } else {
                if (isDark) Color(0xFF78350F).copy(alpha = 0.6f) else Color(0xFFFFFBEB)
            }
            val bannerBorder = if (isDeficit) Color(0xFFEF4444) else Color(0xFFF59E0B)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = bannerBg,
                border = BorderStroke(1.2.dp, bannerBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cashflow_deficit_warning_banner")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isDeficit) "🚨" else "⚠️", fontSize = 16.sp)
                            Text(
                                text = if (isDeficit) "CASHFLOW DEFICIT WARNING" else "LOW RUNWAY WARNING",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDeficit) Color(0xFFDC2626) else Color(0xFFD97706),
                                letterSpacing = 0.5.sp
                            )
                        }
                        IconButton(
                            onClick = { isDeficitBannerDismissed = true },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss warning",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isDeficit) {
                            "Upcoming bills ($${String.format(Locale.US, "%,.2f", upcomingBillsTotal)}) exceed projected cashflow by $${String.format(Locale.US, "%,.2f", kotlin.math.abs(safeToSpend))} before next payday on ${nextPayday.format(df)}."
                        } else {
                            "Tight buffer: Only $${String.format(Locale.US, "%,.2f", safeToSpend)} remaining with $${String.format(Locale.US, "%,.2f", upcomingBillsTotal)} in bills due across the next ${daysUntilPayday} days."
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = (if (isDeficit) Color(0xFFDC2626) else Color(0xFFD97706)).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (isDeficit) "Safe Daily Spend: $0.00/day (Freeze discretionary)" else "Suggested Cap: $${String.format(Locale.US, "%,.2f", dailyAllowance)}/day",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDeficit) Color(0xFFDC2626) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (onOpenTransfer != null) {
                            FilledTonalButton(
                                onClick = onOpenTransfer,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Transfer Funds", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        // Cycle Recap Banner (Dismissible, only shown when expanded)
        AnimatedVisibility(
            visible = isExpanded && !isRecapDismissed,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cycle_recap_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✨", fontSize = 16.sp)
                        Column {
                            Text(
                                text = if (period != null) {
                                    if (isWideScreen) "Cycle Recap: ${period.startDate.format(df)} – ${period.endDate.format(df)}"
                                    else "Cycle: ${period.startDate.format(df)} – ${period.endDate.format(df)}"
                                } else "Cycle Summary",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (isWideScreen) {
                                    "You have ${String.format(Locale.US, "$%,.2f", safeToSpend)} safe to spend with ${daysUntilPayday}d until payday."
                                } else {
                                    "${String.format(Locale.US, "$%,.0f", safeToSpend)} safe to spend • ${daysUntilPayday}d to payday"
                                },
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }
                    IconButton(
                        onClick = { isRecapDismissed = true },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("dismiss_cycle_recap_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss recap",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Main Safe-To-Spend Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isExpanded) 3.dp else 1.5.dp)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isExpanded) 0.6f else 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("safe_to_spend_hero_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideScreen) 14.dp else 10.dp, vertical = if (isCompactHeight) 4.dp else if (isWideScreen) 10.dp else 7.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Top header: Status Badge + Cycle Range (Clickable to toggle expand / collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { toggleAction() }
                        .testTag("safe_to_spend_header_toggle"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (safeToSpend >= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(7.dp)
                        ) {}
                        Text(
                            text = "SAFE-TO-SPEND",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = if (period != null) "${period.startDate.format(df)} – ${period.endDate.format(df)}" else "Current Month",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!isExpanded) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", safeToSpend),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (safeToSpend >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                if (safeToSpend > 0 && daysUntilPayday > 0) {
                                    Text(
                                        text = String.format(Locale.US, "$%,.2f/day (%dd)", dailyAllowance, daysUntilPayday),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    safeToSpend > totalAvailable * 0.3 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    safeToSpend >= 0 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = when {
                                        safeToSpend > totalAvailable * 0.3 -> if (isWideScreen) "💚 On Track" else "💚 Good"
                                        safeToSpend >= 0 -> if (isWideScreen) "⚠️ Watchful" else "⚠️ Caution"
                                        else -> if (isWideScreen) "🚨 Attention" else "🚨 Over"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        safeToSpend > totalAvailable * 0.3 -> Color(0xFF059669)
                                        safeToSpend >= 0 -> Color(0xFFD97706)
                                        else -> MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse Safe-To-Spend" else "Expand Safe-To-Spend",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }

                // Collapsed Mini Progress Bar
                if (!isExpanded && totalAvailable > 0) {
                    LinearProgressIndicator(
                        progress = { spentPct },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = when {
                            spentPct > 0.9f -> MaterialTheme.colorScheme.error
                            spentPct > 0.7f -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Expanded Card Content
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))

                        // Main Amount Display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", safeToSpend),
                                    fontSize = if (isWideScreen) 28.sp else 23.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (safeToSpend >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = if (safeToSpend >= 0) {
                                        if (isWideScreen) "Free cash remaining for this pay cycle" else "Free cash this cycle"
                                    } else {
                                        "Over budget by ${String.format(Locale.US, "$%,.2f", -safeToSpend)}"
                                    },
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when {
                                    safeToSpend > totalAvailable * 0.3 -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    safeToSpend >= 0 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            ) {
                                Text(
                                    text = when {
                                        safeToSpend > totalAvailable * 0.3 -> if (isWideScreen) "💚 On Track" else "💚 Good"
                                        safeToSpend >= 0 -> if (isWideScreen) "⚠️ Watchful" else "⚠️ Caution"
                                        else -> if (isWideScreen) "🚨 Attention" else "🚨 Over"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        safeToSpend > totalAvailable * 0.3 -> Color(0xFF059669)
                                        safeToSpend >= 0 -> Color(0xFFD97706)
                                        else -> MaterialTheme.colorScheme.onErrorContainer
                                    },
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            LinearProgressIndicator(
                                progress = { spentPct },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    spentPct > 0.9f -> MaterialTheme.colorScheme.error
                                    spentPct > 0.7f -> Color(0xFFF59E0B)
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${(spentPct * 100).toInt()}% committed",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${((1f - spentPct).coerceAtLeast(0f) * 100).toInt()}% available",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 3 Quick Metrics Footer (Income/Available, Bills/Expenses, Days to Payday)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isWideScreen) "AVAILABLE CASH" else "AVAILABLE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isWideScreen) String.format(Locale.US, "$%,.2f", totalAvailable) else String.format(Locale.US, "$%,.0f", totalAvailable),
                                    fontSize = if (isWideScreen) 14.sp else 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isWideScreen) "PLANNED BILLS" else "BILLS DUE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isWideScreen) String.format(Locale.US, "$%,.2f", totalExpenses) else String.format(Locale.US, "$%,.0f", totalExpenses),
                                    fontSize = if (isWideScreen) 14.sp else 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isWideScreen) "NEXT PAYDAY" else "PAYDAY",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (daysUntilPayday == 0L) (if (isWideScreen) "Today! 🎉" else "Today!") else (if (isWideScreen) "in ${daysUntilPayday}d" else "${daysUntilPayday}d"),
                                    fontSize = if (isWideScreen) 14.sp else 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // 3-Month Horizon Financial Intelligence Snapshot
                        if (uiState.futurePayPeriods.isNotEmpty()) {
                            val horizonPeriods = uiState.futurePayPeriods.take(7)
                            val threeMoIncome = horizonPeriods.sumOf { it.totalIncomeWithCarryover - it.carryoverFromPrevious }
                            val threeMoExpenses = horizonPeriods.sumOf { it.totalExpenses }
                            val threeMoNet = threeMoIncome - threeMoExpenses

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = "3-Mo Financial Outlook",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Text(
                                    text = "${if (threeMoNet >= 0) "Surplus: +" else "Deficit: "}${String.format(Locale.US, "$%,.0f", threeMoNet)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (threeMoNet >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
                                )
                            }

                            // Spend Remaining Guidance Summary
                            val guidance = if (safeToSpend > 0) {
                                if (threeMoNet >= 0) {
                                    "Safe to allocate: 40% safety buffer ($${String.format(Locale.US, "%.0f", safeToSpend * 0.4)}), 30% savings/goals ($${String.format(Locale.US, "%.0f", safeToSpend * 0.3)}), 30% guilt-free ($${String.format(Locale.US, "%.0f", safeToSpend * 0.3)})."
                                } else {
                                    "Caution: Upcoming 3-mo deficit ($${String.format(Locale.US, "%,.0f", Math.abs(threeMoNet))}). Prioritize reserving remaining buffer to protect future cycles."
                                }
                            } else {
                                "Discretionary funds exhausted for this cycle. Freeze optional spending until next payday."
                            }

                            Text(
                                text = guidance,
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getDebtCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.US)) {
        "credit card" -> "💳"
        "auto loan", "car loan" -> "🚗"
        "student loan" -> "🎓"
        "personal loan" -> "💰"
        "mortgage" -> "🏠"
        "medical" -> "🩺"
        else -> "🏦"
    }
}

@Composable
fun CurrentFocusDebtCard(
    uiState: BudgetUiState,
    onNavigateToDebtPlanner: () -> Unit,
    onLogPayment: (debtId: Int, paymentAmount: Double, principalAmount: Double, interestAmount: Double, note: String, createExpense: Boolean) -> Unit,
    onTogglePaidOff: (DebtEntity) -> Unit,
    modifier: Modifier = Modifier,
    isWideScreen: Boolean = false,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    val debts = uiState.allDebts
    if (debts.isEmpty()) return

    val activeDebts = remember(debts) {
        debts.filter { !it.isPaidOff && it.balance > 0.0 }
    }

    var showPaymentDialog by remember { mutableStateOf(false) }
    var internalExpanded by rememberSaveable { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded
    val haptic = LocalHapticFeedback.current
    val toggleAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onToggleExpand != null) onToggleExpand() else internalExpanded = !internalExpanded
    }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "debt_arrow_rot")

    // If all debts are paid off, show an inspiring celebration banner
    if (activeDebts.isEmpty()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF10B981).copy(alpha = 0.12f)
            ),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
            modifier = modifier
                .fillMaxWidth()
                .clickable { onNavigateToDebtPlanner() }
                .testTag("debt_free_celebration_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🎉", fontSize = 22.sp)
                    Column {
                        Text(
                            text = "100% Debt-Free!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "All ${debts.size} debts are completely paid off. Amazing work!",
                            fontSize = 10.5.sp,
                            color = Color(0xFF065F46)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Debt Planner",
                    tint = Color(0xFF047857),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        return
    }

    // Determine the focus debt:
    // 1. Pinned debts first (lowest customPriorityRank > 0)
    // 2. Snowball order: lowest current balance first, then highest APR
    val focusDebt = remember(activeDebts) {
        val pinned = activeDebts.filter { it.customPriorityRank > 0 }.minByOrNull { it.customPriorityRank }
        pinned ?: activeDebts.minWithOrNull(
            compareBy<DebtEntity> { it.balance }.thenByDescending { it.interestRate }
        )
    } ?: return

    val isPinned = focusDebt.customPriorityRank > 0
    val originalBal = if (focusDebt.originalBalance > 0.0) focusDebt.originalBalance else focusDebt.balance
    val progress = if (originalBal > 0.0) {
        ((originalBal - focusDebt.balance) / originalBal).coerceIn(0.0, 1.0).toFloat()
    } else 0f
    val progressPct = (progress * 100).toInt()

    if (showPaymentDialog) {
        LogDebtPaymentDialog(
            debt = focusDebt,
            prefilledAmount = focusDebt.minimumPayment,
            onDismiss = { showPaymentDialog = false },
            onConfirm = { paymentAmount, principalAmount, interestAmount, note, createExpense ->
                onLogPayment(focusDebt.id, paymentAmount, principalAmount, interestAmount, note, createExpense)
                showPaymentDialog = false
            }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isExpanded) 3.dp else 1.5.dp)
        ),
        border = BorderStroke(
            1.5.dp,
            if (isPinned) Color(0xFF8B5CF6).copy(alpha = if (isExpanded) 0.8f else 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = if (isExpanded) 0.6f else 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("current_focus_debt_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isWideScreen) 14.dp else 12.dp, vertical = if (isExpanded) 10.dp else 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header Row: Target Badge + Total Active Count + Link to Planner / Quick Info (Clickable to toggle expand / collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { toggleAction() }
                    .testTag("focus_debt_header_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPinned) Color(0xFF8B5CF6).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isPinned) "📌 PINNED FOCUS" else "🎯 TARGET #1 (SNOWBALL)",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                color = if (isPinned) Color(0xFF7C3AED) else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (!isExpanded) {
                        Text(
                            text = "${getDebtCategoryEmoji(focusDebt.category)} ${focusDebt.name}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "1 of ${activeDebts.size} active",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isExpanded) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f", focusDebt.balance),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (progressPct > 0) {
                            Text(
                                text = "$progressPct%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }

                        // Quick 1-tap Pay action button in collapsed header
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showPaymentDialog = true
                                }
                                .testTag("focus_debt_quick_pay_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = "Quick Pay",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "Pay",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onNavigateToDebtPlanner() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .testTag("focus_debt_open_planner")
                        ) {
                            Text(
                                text = "Planner",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Debt Planner",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Target #1" else "Expand Target #1",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            // Collapsed Progress Bar
            if (!isExpanded && progressPct > 0) {
                LinearProgressIndicator(
                    progress = { (progressPct / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF10B981),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // Middle: Name, Category, Balance & Terms
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = getDebtCategoryEmoji(focusDebt.category),
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = focusDebt.name,
                                    fontSize = if (isWideScreen) 16.sp else 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.US, "%.1f", focusDebt.interestRate)}% APR",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = "•",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Text(
                                    text = "Min: $${String.format(Locale.US, "%.0f", focusDebt.minimumPayment)}/mo",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (!focusDebt.linkedAccountName.isNullOrBlank()) {
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                    Text(
                                        text = "🏦 ${focusDebt.linkedAccountName}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "$%,.2f", focusDebt.balance),
                                fontSize = if (isWideScreen) 20.sp else 17.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = if (focusDebt.totalPaidSoFar > 0.0) {
                                    "Paid: $${String.format(Locale.US, "%,.0f", focusDebt.totalPaidSoFar)}"
                                } else {
                                    "Remaining"
                                },
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Progress Bar
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (isPinned) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$progressPct% Paid Off",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (progressPct > 0) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📅 Due: ${focusDebt.formatNextDueDate()}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    // Action Row: Log Payment Button + Mark Paid Off Button + Payoff Hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💡", fontSize = 12.sp)
                            Text(
                                text = "Focus extra cash here for fastest payoff.",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onTogglePaidOff(focusDebt) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("focus_debt_mark_paid_button"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Mark Paid",
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Paid Off", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = { showPaymentDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("focus_debt_log_payment_button"),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payment,
                                    contentDescription = "Log Payment",
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Log Payment", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionSearchAndFilterBar(
    statusFilter: Int, // 0 = All, 1 = Pending / Uncleared, 2 = Cleared / Paid
    onStatusFilterChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    customLabels: List<String>? = null
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val filters = customLabels ?: listOf("All Statuses", "⏳ Due", "🕒 Pending", "💳 Paid", "🛡️ Cleared")
        filters.forEachIndexed { index, label ->
            val selected = statusFilter == index
            FilterChip(
                selected = selected,
                onClick = { onStatusFilterChange(index) },
                label = { Text(label, fontSize = 9.5.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                modifier = Modifier.height(24.dp).testTag("filter_chip_$index")
            )
        }
    }
}

@Composable
fun PayCyclesListView(
    uiState: BudgetUiState,
    onAddTransactionClick: (LocalDate) -> Unit,
    onCreateExpenseForRemaining: ((Double, LocalDate) -> Unit)? = null,
    onEditPaycheckClick: (LocalDate, Double) -> Unit = { _, _ -> },
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDuplicateTransaction: ((TransactionEntity) -> Unit)? = null,
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onUpdateTransactionStatus: ((TransactionEntity, String) -> Unit)? = null,
    onToggleReminder: ((TransactionEntity, Int) -> Unit)? = null,
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    onNavigateToUpcoming: (() -> Unit)? = null,
    onDeduplicate: (() -> Unit)? = null,
    onRestoreFinancialData: (() -> Unit)? = null,
    onManualSyncDrive: (() -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    var isSyncingSheets by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Pending, 2 = Cleared
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 0.dp, end = 0.dp, top = 2.dp, bottom = 24.dp)
            .testTag("pay_cycles_list"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (uiState.payPeriods.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("📅", fontSize = 48.sp)
                                        Text(
                                            "No pay periods found",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Adjust your anchor date in settings or sync with Google Sheets.",
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Pay Periods for " + uiState.selectedMonth.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.US) + " " + uiState.selectedMonth.year,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Ledger / Account:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = uiState.activeAccountName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (onDeduplicate != null) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                                modifier = Modifier
                                                    .clickable { onDeduplicate() }
                                                    .testTag("cycles_deduplicate_all_button")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AutoAwesome,
                                                        contentDescription = "Deduplicate",
                                                        tint = Color(0xFFD97706),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "Deduplicate",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFD97706)
                                                    )
                                                }
                                            }
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = if (isWideScreen) "✨ Side-by-Side (${uiState.payPeriods.size} Cycles)" else "${uiState.payPeriods.size} Cycles",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }

                                TransactionSearchAndFilterBar(
                                    statusFilter = statusFilter,
                                    onStatusFilterChange = { statusFilter = it }
                                )

                                if (uiState.allTransactions.isEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .testTag("empty_data_recovery_card"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                                Text("No Transactions Yet", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                            }
                                            Text(
                                                "Start tracking your budget by adding your income and expenses, or restore your budget from 'My Drive/ PayCycle Budget/'.",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (onRestoreFinancialData != null) {
                                                    OutlinedButton(
                                                        onClick = { onRestoreFinancialData() },
                                                        modifier = Modifier.weight(1f).testTag("btn_restore_erased_data")
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Restore Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                                if (onManualSyncDrive != null) {
                                                    Button(
                                                        onClick = { onManualSyncDrive() },
                                                        modifier = Modifier.weight(1f).testTag("btn_sync_erased_data"),
                                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                    ) {
                                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val canUseTwoColumns = isWideScreen && maxWidth >= 640.dp && uiState.payPeriods.size > 1
                                    val colSpacing = 8.dp
                                    if (canUseTwoColumns) {
                                        val today = LocalDate.now()
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (pair in uiState.payPeriods.chunked(2)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(colSpacing)
                                                ) {
                                                    Box(modifier = Modifier.weight(1f)) {
                                                        val isCurrent0 = pair[0].startDate == uiState.currentPayPeriodStart ||
                                                            (!today.isBefore(pair[0].startDate) && !today.isAfter(pair[0].endDate))
                                                        PayCycleCard(
                                                            period = pair[0],
                                                            isCurrentCycle = isCurrent0,
                                                            statusFilter = statusFilter,
                                                            activeAccountName = uiState.activeAccountName,
                                                            onAddTransactionClick = onAddTransactionClick,
                                                            onCreateExpenseForRemaining = onCreateExpenseForRemaining,
                                                            onEditPaycheckClick = onEditPaycheckClick,
                                                            onDeleteTransaction = onDeleteTransaction,
                                                            onEditTransaction = onEditTransaction,
                                                            onDuplicateTransaction = onDuplicateTransaction,
                                                            onToggleCompleted = onToggleCompleted,
                                                            onUpdateTransactionStatus = onUpdateTransactionStatus,
                                                            onToggleReminder = onToggleReminder,
                                                            onNavigateToUpcoming = onNavigateToUpcoming,
                                                            onDeduplicate = onDeduplicate,
                                                            isWideScreen = isWideScreen
                                                        )
                                                    }
                                                    if (pair.size > 1) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            val isCurrent1 = pair[1].startDate == uiState.currentPayPeriodStart ||
                                                                (!today.isBefore(pair[1].startDate) && !today.isAfter(pair[1].endDate))
                                                            PayCycleCard(
                                                                period = pair[1],
                                                                isCurrentCycle = isCurrent1,
                                                                statusFilter = statusFilter,
                                                                activeAccountName = uiState.activeAccountName,
                                                                onAddTransactionClick = onAddTransactionClick,
                                                                onCreateExpenseForRemaining = onCreateExpenseForRemaining,
                                                                onEditPaycheckClick = onEditPaycheckClick,
                                                                onDeleteTransaction = onDeleteTransaction,
                                                                onEditTransaction = onEditTransaction,
                                                                onDuplicateTransaction = onDuplicateTransaction,
                                                                onToggleCompleted = onToggleCompleted,
                                                                onUpdateTransactionStatus = onUpdateTransactionStatus,
                                                                onToggleReminder = onToggleReminder,
                                                                onNavigateToUpcoming = onNavigateToUpcoming,
                                                                onDeduplicate = onDeduplicate,
                                                                isWideScreen = isWideScreen
                                                            )
                                                        }
                                                    } else {
                                                        Spacer(modifier = Modifier.weight(1f))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        val today = LocalDate.now()
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            for (period in uiState.payPeriods) {
                                                val isCurrent = period.startDate == uiState.currentPayPeriodStart ||
                                                    (!today.isBefore(period.startDate) && !today.isAfter(period.endDate))
                                                PayCycleCard(
                                                    period = period,
                                                    isCurrentCycle = isCurrent,
                                                    statusFilter = statusFilter,
                                                    activeAccountName = uiState.activeAccountName,
                                                    onAddTransactionClick = onAddTransactionClick,
                                                    onCreateExpenseForRemaining = onCreateExpenseForRemaining,
                                                    onEditPaycheckClick = onEditPaycheckClick,
                                                    onDeleteTransaction = onDeleteTransaction,
                                                    onEditTransaction = onEditTransaction,
                                                    onDuplicateTransaction = onDuplicateTransaction,
                                                    onToggleCompleted = onToggleCompleted,
                                                    onUpdateTransactionStatus = onUpdateTransactionStatus,
                                                    onToggleReminder = onToggleReminder,
                                                    onNavigateToUpcoming = onNavigateToUpcoming,
                                                    onDeduplicate = onDeduplicate,
                                                    isWideScreen = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayCycleCard(
    period: PayPeriod,
    isCurrentCycle: Boolean = false,
    statusFilter: Int = 0, // 0 = All, 1 = Pending, 2 = Cleared
    activeAccountName: String = "All Accounts",
    onAddTransactionClick: (LocalDate) -> Unit,
    onCreateExpenseForRemaining: ((Double, LocalDate) -> Unit)? = null,
    onEditPaycheckClick: (LocalDate, Double) -> Unit = { _, _ -> },
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDuplicateTransaction: ((TransactionEntity) -> Unit)? = null,
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onUpdateTransactionStatus: ((TransactionEntity, String) -> Unit)? = null,
    onToggleReminder: ((TransactionEntity, Int) -> Unit)? = null,
    onNavigateToUpcoming: (() -> Unit)? = null,
    onDeduplicate: (() -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val isNextCycle = remember(period.startDate, isCurrentCycle, today) {
        !isCurrentCycle && (period.startDate.isAfter(today) || period.startDate.isEqual(today))
    }
    var expanded by remember(period.payday, isCurrentCycle) {
        mutableStateOf(isCurrentCycle)
    }
    var selectedAccountFilterInCycle by remember(period.payday) { mutableStateOf<String?>(null) }

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f

    val df = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val dfFull = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }

    val pct = period.spentPercentage

    val unpaidBillsInCycle = remember(period.transactions) {
        period.transactions.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment && !it.isCompleted && it.status !in listOf("paid", "cleared") && it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted") }
    }
    val paidBillsInCycle = remember(period.transactions) {
        period.transactions.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment && (it.isCompleted || it.status in listOf("paid", "cleared")) && it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted") }
    }
    val totalBillsDue = remember(unpaidBillsInCycle) { unpaidBillsInCycle.sumOf { it.amount } }
    val totalBillsPaid = remember(paidBillsInCycle) { paidBillsInCycle.sumOf { it.amount } }

    val accountsInThisCycle = remember(period.transactions, period.paycheckAmount, period.otherIncome) {
        val validTx = period.transactions.filter {
            it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted")
        }
        val defaultAcc = if (activeAccountName != "All Accounts" && !activeAccountName.equals("Primary Account", ignoreCase = true) && !activeAccountName.equals("Primary", ignoreCase = true)) activeAccountName else (period.transactions.map { it.accountName }.firstOrNull { it.isNotBlank() && !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) } ?: "")
        val accountsMap = linkedMapOf<String, MutableList<TransactionEntity>>()
        for (tx in validTx) {
            val acc = if (tx.accountName.isBlank() || tx.accountName.equals("Primary Account", ignoreCase = true) || tx.accountName.equals("Primary", ignoreCase = true)) defaultAcc else tx.accountName
            if (acc.isNotBlank()) {
                accountsMap.getOrPut(acc) { mutableListOf() }.add(tx)
            }
        }

        accountsMap.map { (acc, txs) ->
            val inc = txs.filter { it.isIncome && !it.isStartingBalanceOrAdjustment }.sumOf { it.amount }
            val exp = txs.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment }.sumOf { it.amount }
            val compExp = txs.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment && it.isCompleted }.sumOf { it.amount }
            val pendExp = txs.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment && !it.isCompleted }.sumOf { it.amount }
            AccountInCycleSummary(
                accountName = acc,
                totalIncome = inc,
                totalExpenses = exp,
                completedExpenses = compExp,
                pendingExpenses = pendExp,
                net = inc - exp,
                transactionCount = txs.size
            )
        }.sortedByDescending { it.totalExpenses + it.totalIncome }
    }

    // Solid opaque background colors (0% transparency) - lighter when behind dark background, darker when behind light background
    val cardBgColor by animateColorAsState(
        targetValue = when {
            expanded && isNextCycle -> if (isDark) Color(0xFF233044) else Color(0xFFE2E8F0)
            expanded && isCurrentCycle -> if (isDark) Color(0xFF133633) else Color(0xFFD1FAE5)
            expanded -> if (isDark) Color(0xFF1A2230) else Color(0xFFF1F5F9)
            else -> MaterialTheme.colorScheme.surface
        },
        label = "cardBgColor"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            expanded && isNextCycle -> if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7)
            expanded && isCurrentCycle -> MaterialTheme.colorScheme.primary
            expanded -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        label = "cardBorderColor"
    )

    val cardBorderWidth = if (expanded) 1.5.dp else 1.dp
    val cardElevation = if (expanded) 6.dp else 1.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("pay_period_card_${period.payday}"),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(cardBorderWidth, cardBorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = when {
                                    isCurrentCycle -> MaterialTheme.colorScheme.primary
                                    isNextCycle -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Text(
                                text = "${period.startDate.format(df)} – ${period.endDate.format(df)}",
                                fontSize = if (isWideScreen) 15.sp else 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (isCurrentCycle) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isWideScreen) "Current Cycle" else "Current",
                                    fontSize = if (isWideScreen) 9.sp else 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (isNextCycle) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = if (isWideScreen) "Next Cycle" else "Next",
                                    fontSize = if (isWideScreen) 9.sp else 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        if (unpaidBillsInCycle.isNotEmpty() || paidBillsInCycle.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .testTag("cycle_bills_due_chip_${period.payday}")
                                    .clickable { onNavigateToUpcoming?.invoke() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(9.5.dp)
                                    )
                                    val otherExpenseAmount = maxOf(0.0, period.totalExpenses - totalBillsDue - totalBillsPaid)
                                    val chipLabel = when {
                                        paidBillsInCycle.isNotEmpty() && unpaidBillsInCycle.isNotEmpty() ->
                                            "${unpaidBillsInCycle.size} due (${String.format(Locale.US, "$%,.0f", totalBillsDue)}) • ${paidBillsInCycle.size} paid (${String.format(Locale.US, "$%,.0f", totalBillsPaid)})${if (otherExpenseAmount > 0.5) " + extra ($" + String.format(Locale.US, "%,.0f", otherExpenseAmount) + ")" else ""} • Total $${String.format(Locale.US, "%,.0f", period.totalExpenses)} →"
                                        unpaidBillsInCycle.isNotEmpty() ->
                                            if (otherExpenseAmount > 0.5) {
                                                "${unpaidBillsInCycle.size} bills ($${String.format(Locale.US, "%,.0f", totalBillsDue)}) + other ($${String.format(Locale.US, "%,.0f", otherExpenseAmount)}) • Total $${String.format(Locale.US, "%,.0f", period.totalExpenses)} →"
                                            } else {
                                                if (isWideScreen) "${unpaidBillsInCycle.size} bills due (${String.format(Locale.US, "$%,.0f", totalBillsDue)}) →" else "${unpaidBillsInCycle.size} bills (${String.format(Locale.US, "$%,.0f", totalBillsDue)}) →"
                                            }
                                        paidBillsInCycle.isNotEmpty() ->
                                            if (otherExpenseAmount > 0.5) {
                                                "All ${paidBillsInCycle.size} paid ($${String.format(Locale.US, "%,.0f", totalBillsPaid)}) + extra ($${String.format(Locale.US, "%,.0f", otherExpenseAmount)}) →"
                                            } else {
                                                "All ${paidBillsInCycle.size} bills paid (${String.format(Locale.US, "$%,.0f", totalBillsPaid)}) →"
                                            }
                                        else -> "No bills"
                                    }
                                    Text(
                                        text = chipLabel,
                                        fontSize = if (isWideScreen) 8.5.sp else 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                    if (expanded) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Payday: ${period.payday.format(dfFull)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (activeAccountName == "All Accounts") MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = if (activeAccountName == "All Accounts") "All Accounts" else activeAccountName,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeAccountName == "All Accounts") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Health Progress & Financial Summary Container Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            val isFunded = period.remainingBalance >= 0
                            val accountHeaderLabel = if (activeAccountName != "All Accounts" && activeAccountName.isNotBlank()) "$activeAccountName Remaining:" else "Cycle Remaining:"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = accountHeaderLabel,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = if (isFunded) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (isFunded) "Surplus" else "Deficit",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFunded) Color(0xFF059669) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = String.format(Locale.US, "%s$%,.2f", if (period.remainingBalance >= 0) "+" else "", period.remainingBalance),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isFunded) Color(0xFF059669) else Color(0xFFDC2626),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Progress Bar: Percentage of account funds spent
                            LinearProgressIndicator(
                                progress = { pct.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    pct > 0.95f -> Color(0xFFDC2626)
                                    pct > 0.8f -> Color(0xFFD97706)
                                    else -> Color(0xFF059669)
                                },
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Cycle Breakdown Metrics Row
                            val startFunds = period.startingBalance + period.carryoverFromPrevious
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "EXPENSES",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", period.totalExpenses),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (paidBillsInCycle.isNotEmpty() && unpaidBillsInCycle.isNotEmpty()) {
                                            Text(
                                                text = String.format(Locale.US, "$%,.0f due • $%,.0f paid", totalBillsDue, totalBillsPaid),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFDC2626).copy(alpha = 0.85f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        } else if (unpaidBillsInCycle.isNotEmpty()) {
                                            val otherExp = maxOf(0.0, period.totalExpenses - totalBillsDue - totalBillsPaid)
                                            Text(
                                                text = if (otherExp > 0.5) "${unpaidBillsInCycle.size} bills ($${String.format(Locale.US, "%,.0f", totalBillsDue)}) + other ($${String.format(Locale.US, "%,.0f", otherExp)})" else "${unpaidBillsInCycle.size} bills due",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFFDC2626).copy(alpha = 0.85f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        } else if (paidBillsInCycle.isNotEmpty()) {
                                            Text(
                                                text = "All paid",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF059669),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                val currentAccSummary = accountsInThisCycle.firstOrNull { it.accountName.equals(activeAccountName, ignoreCase = true) }
                                val accountBalanceVal = if (activeAccountName != "All Accounts" && activeAccountName.isNotBlank() && currentAccSummary != null) {
                                    startFunds + currentAccSummary.net
                                } else {
                                    period.totalAvailable
                                }
                                val accountBoxLabel = if (activeAccountName != "All Accounts" && activeAccountName.isNotBlank()) "BAL IN " + activeAccountName.uppercase(Locale.US) else "IN ACCOUNT"
                                val accFundsSubtitle = when {
                                    startFunds > 0 && period.totalIncome > 0 ->
                                        String.format(Locale.US, "$%,.0f start + $%,.0f inc", startFunds, period.totalIncome)
                                    startFunds > 0 ->
                                        String.format(Locale.US, "$%,.0f start balance", startFunds)
                                    period.totalIncome > 0 ->
                                        String.format(Locale.US, "$%,.0f cycle income", period.totalIncome)
                                    else -> "Available in account"
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = accountBoxLabel,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", accountBalanceVal),
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = accFundsSubtitle,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF059669).copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isFunded) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (isFunded) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 3.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "REMAINING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFunded) Color(0xFF059669) else Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%s$%,.2f", if (period.remainingBalance >= 0) "+" else "", period.remainingBalance),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isFunded) Color(0xFF059669) else Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = if (isFunded) "After all bills" else "Shortfall",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isFunded) Color(0xFF059669).copy(alpha = 0.85f) else Color(0xFFDC2626).copy(alpha = 0.85f),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val textParts = mutableListOf<String>()
                                    if (startFunds != 0.0) {
                                        textParts.add("Starting balance: ${if (startFunds > 0) "+" else ""}${String.format(Locale.US, "$%,.2f", startFunds)}")
                                    }
                                    if (period.totalIncome > 0.0) {
                                        textParts.add("Cycle income: +${String.format(Locale.US, "$%,.2f", period.totalIncome)}")
                                    }
                                    Text(
                                        text = if (textParts.isNotEmpty()) textParts.joinToString(" • ") else "Available in account: ${String.format(Locale.US, "$%,.2f", period.totalAvailable)}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Total in Account: ${String.format(Locale.US, "$%,.2f", period.totalAvailable)}",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (period.totalAvailable >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val cycleNetText = String.format(Locale.US, "%s$%,.2f", if (period.cycleNet >= 0) "+" else "", period.cycleNet)
                                    val note = if (period.remainingBalance >= 0 && period.cycleNet < 0) " (Covered by starting balance)" else ""
                                    Text(
                                        text = "Income vs bills net: $cycleNetText$note",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "Remaining balance: ${String.format(Locale.US, "$%,.2f", period.remainingBalance)}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (period.remainingBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }

                    if (accountsInThisCycle.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "TARGET ACCOUNTS IN THIS CYCLE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                        letterSpacing = 0.5.sp
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                    ) {
                                        Text(
                                            text = "${accountsInThisCycle.size} ${if (accountsInThisCycle.size == 1) "account" else "accounts"}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        )
                                    }
                                }
                                if (selectedAccountFilterInCycle != null) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .clickable { selectedAccountFilterInCycle = null }
                                            .testTag("clear_cycle_account_filter")
                                    ) {
                                        Text(
                                            text = "Show All Accounts ✕",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(accountsInThisCycle, key = { it.accountName }) { accSummary ->
                                    val isFiltered = selectedAccountFilterInCycle == accSummary.accountName
                                    val accStyle = getAccountColorStyle(accSummary.accountName, isDark)

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFiltered) accStyle.containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        border = BorderStroke(
                                            width = if (isFiltered) 1.5.dp else 1.dp,
                                            color = if (isFiltered) accStyle.borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                selectedAccountFilterInCycle = if (isFiltered) null else accSummary.accountName
                                            }
                                            .testTag("cycle_target_account_${accSummary.accountName}")
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .widthIn(min = 160.dp, max = 240.dp)
                                                .padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Left: Indicator dot + Account Name & count badge + Paid/Left summary
                                            Row(
                                                modifier = Modifier.weight(1f, fill = false),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(7.dp)
                                                        .background(accStyle.borderColor, CircleShape)
                                                )
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = accSummary.accountName,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isFiltered) accStyle.contentColor else MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = accStyle.borderColor.copy(alpha = 0.2f)
                                                        ) {
                                                            Text(
                                                                text = "${accSummary.transactionCount}",
                                                                fontSize = 8.5.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = accStyle.borderColor,
                                                                modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp)
                                                            )
                                                        }
                                                    }

                                                    // Compact horizontal status line
                                                    if (accSummary.completedExpenses > 0.0 || accSummary.pendingExpenses > 0.0) {
                                                        Row(
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            if (accSummary.completedExpenses > 0.0) {
                                                                Text(
                                                                    text = String.format(Locale.US, "✓$%,.0f", accSummary.completedExpenses),
                                                                    fontSize = 8.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = Color(0xFF047857)
                                                                )
                                                            }
                                                            if (accSummary.completedExpenses > 0.0 && accSummary.pendingExpenses > 0.0) {
                                                                Text(
                                                                    text = "•",
                                                                    fontSize = 8.sp,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                            }
                                                            if (accSummary.pendingExpenses > 0.0) {
                                                                Text(
                                                                    text = String.format(Locale.US, "⏳$%,.0f", accSummary.pendingExpenses),
                                                                    fontSize = 8.5.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = Color(0xFFD97706)
                                                                )
                                                            }
                                                        }
                                                    } else if (accSummary.totalIncome > 0.0 && accSummary.totalExpenses == 0.0) {
                                                        Text(
                                                            text = "Income source",
                                                            fontSize = 8.5.sp,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            // Right: Financial amounts (expenses / income)
                                            Column(
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                if (accSummary.totalExpenses > 0.0) {
                                                    Text(
                                                        text = String.format(Locale.US, "-$%,.2f", accSummary.totalExpenses),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFDC2626),
                                                        maxLines = 1
                                                    )
                                                }
                                                if (accSummary.totalIncome > 0.0) {
                                                    Text(
                                                        text = String.format(Locale.US, "+$%,.2f", accSummary.totalIncome),
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669),
                                                        maxLines = 1
                                                    )
                                                }
                                                if (accSummary.totalExpenses == 0.0 && accSummary.totalIncome == 0.0) {
                                                    Text(
                                                        text = "$0.00",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Transactions list
                        val cycleTransactions = period.transactions.filter { tx ->
                            val isValid = tx.amount > 0.0 && !tx.title.contains("Cancelled") && !tx.title.contains("Deleted")
                            if (!isValid) return@filter false

                            val matchesStatus = when (statusFilter) {
                                1 -> tx.getEffectiveStatus() == "due"
                                2 -> tx.getEffectiveStatus() == "pending"
                                3 -> tx.getEffectiveStatus() == "paid"
                                4 -> tx.getEffectiveStatus() == "cleared"
                                else -> true
                            }
                            if (!matchesStatus) return@filter false

                            val defaultAcc = if (activeAccountName != "All Accounts" && !activeAccountName.equals("Primary Account", ignoreCase = true) && !activeAccountName.equals("Primary", ignoreCase = true)) activeAccountName else (period.transactions.map { it.accountName }.firstOrNull { it.isNotBlank() && !it.equals("Primary Account", ignoreCase = true) && !it.equals("Primary", ignoreCase = true) } ?: "")
                            val txAcc = if (tx.accountName.isBlank() || tx.accountName.equals("Primary Account", ignoreCase = true) || tx.accountName.equals("Primary", ignoreCase = true)) defaultAcc else tx.accountName
                            if (selectedAccountFilterInCycle != null && !txAcc.equals(selectedAccountFilterInCycle, ignoreCase = true)) {
                                return@filter false
                            }

                            true
                        }.sortedWith(compareBy({ it.isCompleted }, { it.dateMillis }))

                        val hasDuplicates = remember(cycleTransactions) {
                            val names = cycleTransactions.map { DateUtils.normalizeDebtOrBillName(it.title).lowercase(Locale.US) }
                            names.size != names.distinct().size
                        }

                        val cycleExpenses = remember(cycleTransactions) { cycleTransactions.filter { !it.isIncome && !it.isStartingBalanceOrAdjustment } }
                        val cycleIncome = remember(cycleTransactions) { cycleTransactions.filter { it.isIncome && !it.isStartingBalanceOrAdjustment } }
                        val cycleStartingBalances = remember(cycleTransactions) { cycleTransactions.filter { it.isStartingBalanceOrAdjustment } }
                        val cycleTotalExpenses = remember(cycleExpenses) { cycleExpenses.sumOf { it.amount } }
                        val cycleTotalIncome = remember(cycleIncome) { cycleIncome.sumOf { it.amount } }
                        val cycleTotalStartingBalance = remember(cycleStartingBalances) { cycleStartingBalances.sumOf { if (it.isIncome) it.amount else -it.amount } }
                        val cycleDueExpenses = remember(cycleExpenses) { cycleExpenses.filter { !it.isCompleted && it.getEffectiveStatus() !in listOf("paid", "cleared") }.sumOf { it.amount } }
                        val cyclePaidExpenses = remember(cycleExpenses) { cycleExpenses.filter { it.isCompleted || it.getEffectiveStatus() in listOf("paid", "cleared") }.sumOf { it.amount } }

                        var cycleItemTypeTab by remember { mutableIntStateOf(0) } // 0 = All, 1 = Expenses, 2 = Income, 3 = Starting Balance

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "CYCLE SCHEDULE & TRANSACTIONS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    letterSpacing = 0.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "${cycleTransactions.size} items",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasDuplicates && onDeduplicate != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f)),
                                        modifier = Modifier
                                            .clickable { onDeduplicate() }
                                            .testTag("cycle_item_deduplicate_btn")
                                            .padding(end = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "Deduplicate",
                                                tint = Color(0xFFD97706),
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Deduplicate",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD97706)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Scheduled items subtotal summary banner: Reconciles items directly with Independent Cycle Net!
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Cycle Expenses:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "-$%,.2f", cycleTotalExpenses),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626)
                                    )
                                    if (cyclePaidExpenses > 0.0) {
                                        Text(
                                            text = String.format(Locale.US, "(⌛$%,.0f • ✓$%,.0f)", cycleDueExpenses, cyclePaidExpenses),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFDC2626).copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Cycle Income:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "+$%,.2f", cycleTotalIncome),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
                                    )
                                }
                            }
                        }

                        // Type filter chips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = cycleItemTypeTab == 0,
                                onClick = { cycleItemTypeTab = 0 },
                                label = { Text("All (${cycleTransactions.size})", fontSize = 9.sp, fontWeight = if (cycleItemTypeTab == 0) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.height(24.dp).testTag("cycle_filter_all")
                            )
                            FilterChip(
                                selected = cycleItemTypeTab == 1,
                                onClick = { cycleItemTypeTab = 1 },
                                label = {
                                    Text(
                                        text = "Expenses: -$${String.format(Locale.US, "%,.0f", cycleTotalExpenses)} (${cycleExpenses.size})",
                                        fontSize = 9.sp,
                                        fontWeight = if (cycleItemTypeTab == 1) FontWeight.Bold else FontWeight.Normal,
                                        color = if (cycleItemTypeTab == 1) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.height(24.dp).testTag("cycle_filter_expenses")
                            )
                            FilterChip(
                                selected = cycleItemTypeTab == 2,
                                onClick = { cycleItemTypeTab = 2 },
                                label = {
                                    Text(
                                        text = "Income: +$${String.format(Locale.US, "%,.0f", cycleTotalIncome)} (${cycleIncome.size})",
                                        fontSize = 9.sp,
                                        fontWeight = if (cycleItemTypeTab == 2) FontWeight.Bold else FontWeight.Normal,
                                        color = if (cycleItemTypeTab == 2) Color(0xFF059669) else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                modifier = Modifier.height(24.dp).testTag("cycle_filter_income")
                            )
                            if (cycleStartingBalances.isNotEmpty()) {
                                FilterChip(
                                    selected = cycleItemTypeTab == 3,
                                    onClick = { cycleItemTypeTab = 3 },
                                    label = {
                                        Text(
                                            text = "Starting Bal: $${String.format(Locale.US, "%,.0f", cycleTotalStartingBalance)} (${cycleStartingBalances.size})",
                                            fontSize = 9.sp,
                                            fontWeight = if (cycleItemTypeTab == 3) FontWeight.Bold else FontWeight.Normal,
                                            color = if (cycleItemTypeTab == 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    modifier = Modifier.height(24.dp).testTag("cycle_filter_starting_bal")
                                )
                            }
                        }

                        if (cycleTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⚡ No bills or expenses scheduled in this cycle.\nTap below to add an item.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            val groupsToDisplay = when (cycleItemTypeTab) {
                                1 -> listOf("Expenses" to cycleExpenses)
                                2 -> listOf("Income" to cycleIncome)
                                3 -> listOf("Starting Balance" to cycleStartingBalances)
                                else -> listOf("Expenses" to cycleExpenses, "Income" to cycleIncome, "Starting Balance" to cycleStartingBalances).filter { it.second.isNotEmpty() }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                for ((groupName, groupTxs) in groupsToDisplay) {
                                    if (groupsToDisplay.size > 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = when (groupName) {
                                                    "Expenses" -> "EXPENSES & BILLS (${groupTxs.size})"
                                                    "Income" -> "EARNED INCOME (${groupTxs.size})"
                                                    else -> "ACCOUNT STARTING BALANCE (${groupTxs.size})"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (groupName) {
                                                    "Expenses" -> Color(0xFFDC2626)
                                                    "Income" -> Color(0xFF059669)
                                                    else -> MaterialTheme.colorScheme.primary
                                                },
                                                letterSpacing = 0.5.sp
                                            )
                                            Text(
                                                text = when (groupName) {
                                                    "Expenses" -> String.format(Locale.US, "-$%,.2f", groupTxs.sumOf { it.amount })
                                                    "Income" -> String.format(Locale.US, "+$%,.2f", groupTxs.sumOf { it.amount })
                                                    else -> String.format(Locale.US, "%s$%,.2f", if (groupTxs.sumOf { if (it.isIncome) it.amount else -it.amount } >= 0) "+" else "", groupTxs.sumOf { if (it.isIncome) it.amount else -it.amount })
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = when (groupName) {
                                                    "Expenses" -> Color(0xFFDC2626)
                                                    "Income" -> Color(0xFF059669)
                                                    else -> MaterialTheme.colorScheme.primary
                                                }
                                            )
                                        }
                                    }

                                    for (tx in groupTxs) {
                                        val txDate = DateUtils.millisToLocalDate(tx.dateMillis)
                                        val daysDiff = ChronoUnit.DAYS.between(today, txDate)
                                        val isSettled = tx.isPaidOrCleared
                                        val itemBorder = when {
                                            !isSettled && !tx.isIncome && daysDiff < 0 -> BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                                            !isSettled && !tx.isIncome && daysDiff in 0L..2L -> BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
                                            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                        val itemBg = when {
                                            !isSettled && !tx.isIncome && daysDiff < 0 -> if (isDark) Color(0xFF450A0A).copy(alpha = 0.35f) else Color(0xFFFEF2F2)
                                            !isSettled && !tx.isIncome && daysDiff in 0L..2L -> if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFFBEB)
                                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                        }

                                        SwipeableTransactionRow(
                                            transaction = tx,
                                            onDelete = { onDeleteTransaction(tx) },
                                            onEdit = { onEditTransaction(tx) },
                                            onDuplicate = { onDuplicateTransaction?.invoke(tx) },
                                            onUpdateStatus = { newSt ->
                                                onUpdateTransactionStatus?.invoke(tx, newSt) ?: onToggleCompleted(tx)
                                            },
                                            onToggleCompleted = { onToggleCompleted(tx) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                color = itemBg,
                                                border = itemBorder
                                            ) {
                                                TransactionItemRow(
                                                    transaction = tx,
                                                    onDelete = { onDeleteTransaction(tx) },
                                                    onEdit = { onEditTransaction(tx) },
                                                    onDuplicate = { onDuplicateTransaction?.invoke(tx) },
                                                    onToggleCompleted = { onToggleCompleted(tx) },
                                                    onUpdateStatus = { newSt ->
                                                        onUpdateTransactionStatus?.invoke(tx, newSt) ?: onToggleCompleted(tx)
                                                    },
                                                    onToggleReminder = { days ->
                                                        onToggleReminder?.invoke(tx, days)
                                                    },
                                                    modifier = Modifier.padding(vertical = 0.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Actions footer inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (period.remainingBalance > 0 && onCreateExpenseForRemaining != null) {
                                val context = LocalContext.current
                                val remAmount = period.remainingBalance
                                val today = LocalDate.now()
                                val targetDate = if (!today.isBefore(period.startDate) && !today.isAfter(period.endDate)) today else period.startDate

                                OutlinedButton(
                                    onClick = {
                                        onCreateExpenseForRemaining.invoke(remAmount, targetDate)
                                        android.widget.Toast.makeText(
                                            context,
                                            String.format(Locale.US, "Created $%,.2f expense for remaining balance", remAmount),
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    modifier = Modifier.testTag("create_remaining_expense_${period.payday}"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 3.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "Expense Remaining ($%,.2f)", remAmount),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(
                                onClick = { onAddTransactionClick(period.startDate) },
                                modifier = Modifier.testTag("add_item_cycle_${period.payday}"),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Bill / Income", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onToggleCompleted: (() -> Unit)? = null,
    onUpdateStatus: ((String) -> Unit)? = null,
    onToggleReminder: ((Int) -> Unit)? = null,
    showDateChip: Boolean = true,
    modifier: Modifier = Modifier
) {
    val df = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val date = remember(transaction.dateMillis) { DateUtils.millisToLocalDate(transaction.dateMillis) }
    val today = remember { LocalDate.now() }
    val isUnpaidExpense = !transaction.isIncome && !transaction.isPaidOrCleared
    val daysBetween = remember(today, date) { ChronoUnit.DAYS.between(today, date) }
    val dark = isSystemInDarkTheme()

    val transitionState = remember {
        MutableTransitionState(false).apply {
            targetState = true
        }
    }

    AnimatedVisibility(
        visibleState = transitionState,
        enter = slideInHorizontally(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) { fullWidth -> fullWidth } + fadeIn(animationSpec = tween(300))
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clickable(enabled = onEdit != null) { onEdit?.invoke() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .testTag("transaction_row_${transaction.id}"),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Main Top Line: [Checkbox] [Icon] [Title Text] ... [Amount]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onToggleCompleted != null || onUpdateStatus != null) {
                    val status = transaction.getEffectiveStatus()
                    val isDone = status in listOf("paid", "cleared")
                    IconButton(
                        onClick = {
                            if (onUpdateStatus != null) {
                                onUpdateStatus(TxStatus.nextStatus(status))
                            } else {
                                onToggleCompleted?.invoke()
                            }
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("toggle_completed_tx_${transaction.id}")
                    ) {
                        val checkBg = when (status) {
                            "cleared" -> Color(0xFF10B981)
                            "paid" -> Color(0xFF3B82F6)
                            "pending" -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                            else -> Color.Transparent
                        }
                        val checkBorder = when (status) {
                            "cleared" -> Color(0xFF10B981)
                            "paid" -> Color(0xFF3B82F6)
                            "pending" -> Color(0xFFF59E0B)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = checkBorder,
                                    shape = CircleShape
                                )
                                .background(
                                    color = checkBg,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = if (status == "cleared") "Cleared" else "Paid",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (transaction.isIncome) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(if (transaction.isIncome) "💰" else CategoryTheme.getEmoji(transaction.category), fontSize = 12.sp)
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = transaction.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (transaction.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (transaction.isCompleted && !transaction.isIncome) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (transaction.isRecurring) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = if (transaction.isIncome) "Recurring bi-weekly" else "Recurring monthly",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                val isStartingBal = transaction.isStartingBalanceOrAdjustment
                val amountColor = when {
                    isStartingBal -> MaterialTheme.colorScheme.primary
                    transaction.isIncome -> Color(0xFF10B981)
                    transaction.isCompleted -> Color(0xFF047857)
                    else -> Color(0xFFDC2626)
                }
                Text(
                    text = (if (transaction.isIncome) "+" else "-") + String.format(Locale.US, "$%,.2f", transaction.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Bottom Line: Badges/Chips on the left, Edit/Delete Action Icons on the right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (onToggleCompleted != null) 30.dp else 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlowRow(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (transaction.isStartingBalanceOrAdjustment) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Account Starting Balance (Not cycle income)",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (transaction.title.contains("Remaining Cycle Expense", ignoreCase = true)) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f),
                            border = BorderStroke(0.5.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "Cycle Allocation",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7C3AED),
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (showDateChip) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = date.format(df),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Upcoming Due-Date Alerts & Overdue Badging:
                    // Highlighting transactions Due within next 48 hours in warm amber, and past-due unpaid bills in soft crimson
                    if (isUnpaidExpense) {
                        if (daysBetween < 0) {
                            // Overdue badge (Soft Crimson)
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (dark) Color(0xFF450A0A) else Color(0xFFFEE2E2),
                                border = BorderStroke(0.6.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = if (daysBetween == -1L) "⚠️ 1d Overdue" else "⚠️ ${kotlin.math.abs(daysBetween)}d Overdue",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (daysBetween == 0L) {
                            // Due Today badge (Warm Orange)
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (dark) Color(0xFF7C2D12).copy(alpha = 0.5f) else Color(0xFFFFEDD5),
                                border = BorderStroke(0.6.dp, Color(0xFFEA580C).copy(alpha = 0.8f))
                            ) {
                                Text(
                                    text = "⚡ Due Today",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dark) Color(0xFFFDBA74) else Color(0xFFC2410C),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (daysBetween in 1L..2L) {
                            // Due within 48 hours badge (Warm Amber)
                            Surface(
                                shape = RoundedCornerShape(3.dp),
                                color = if (dark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
                                border = BorderStroke(0.6.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
                            ) {
                                Text(
                                    text = if (daysBetween == 1L) "⏳ Due Tomorrow" else "🕒 Due in 2d (48h)",
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (dark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    TransactionStatusChip(
                        status = transaction.getEffectiveStatus(),
                        isIncome = transaction.isIncome,
                        fontSize = 7.sp,
                        onStatusChange = { newStatus ->
                            if (onUpdateStatus != null) {
                                onUpdateStatus(newStatus)
                            } else {
                                onToggleCompleted?.invoke()
                            }
                        },
                        testTag = "tx_status_chip_${transaction.id}"
                    )
                    if (transaction.isAutoPay) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "⚡ Auto Pay",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (isUnpaidExpense && transaction.reminderDaysBefore >= 0) {
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (dark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7),
                            border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🔔 Alert Active",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (dark) Color(0xFFFDE68A) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    val accountTag = if (transaction.accountName.isBlank() || transaction.accountName.equals("Primary Account", ignoreCase = true) || transaction.accountName.equals("Primary", ignoreCase = true)) "" else transaction.accountName
                    if (accountTag.isNotBlank()) {
                        val accountStyle = getAccountColorStyle(accountTag, isSystemInDarkTheme())
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = accountStyle.containerColor,
                            border = BorderStroke(0.8.dp, accountStyle.borderColor)
                        ) {
                            Text(
                                text = accountTag,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accountStyle.contentColor,
                                modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!transaction.isIncome && transaction.category.isNotBlank()) {
                        val (catParent, _) = CategoryTheme.parseCategory(transaction.category)
                        val catEmoji = CategoryTheme.getEmoji(transaction.category)
                        val catColor = CategoryTheme.getColor(transaction.category)
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = catColor.copy(alpha = if (isSystemInDarkTheme()) 0.22f else 0.12f),
                            border = BorderStroke(0.8.dp, catColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "$catEmoji $catParent",
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = catColor,
                                modifier = Modifier.padding(horizontal = 3.5.dp, vertical = 0.5.dp),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onToggleReminder != null && isUnpaidExpense) {
                        val isReminderActive = transaction.reminderDaysBefore >= 0
                        IconButton(
                            onClick = {
                                val nextRem = if (isReminderActive) -1 else 1
                                onToggleReminder(nextRem)
                            },
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("reminder_tx_${transaction.id}")
                        ) {
                            Icon(
                                if (isReminderActive) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = if (isReminderActive) "Reminder Active" else "Set Reminder",
                                tint = if (isReminderActive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (onDuplicate != null) {
                        IconButton(
                            onClick = onDuplicate,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("duplicate_tx_${transaction.id}")
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Duplicate",
                                tint = Color(0xFF8B5CF6).copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (onEdit != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("edit_tx_${transaction.id}")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("delete_tx_${transaction.id}")
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatCalendarAmount(amount: Double): String {
    return when {
        amount >= 1_000_000 -> String.format(Locale.US, "$%.1fM", amount / 1_000_000.0)
        amount >= 1_000 -> {
            val kVal = amount / 1_000.0
            if (amount % 1_000 == 0.0) String.format(Locale.US, "$%.0fk", kVal)
            else String.format(Locale.US, "$%.1fk", kVal)
        }
        amount % 1.0 == 0.0 -> String.format(Locale.US, "$%.0f", amount)
        else -> String.format(Locale.US, "$%.0f", amount)
    }
}

@Composable
fun CalendarGridView(
    uiState: BudgetUiState,
    onAddTransactionClick: (LocalDate) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit = {},
    onDuplicateTransaction: ((TransactionEntity) -> Unit)? = null,
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onUpdateTransactionStatus: ((TransactionEntity, String) -> Unit)? = null,
    onToggleReminder: ((TransactionEntity, Int) -> Unit)? = null,
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var displayMonth by rememberSaveable { mutableStateOf(uiState.selectedMonth) }
    LaunchedEffect(uiState.selectedMonth) {
        displayMonth = uiState.selectedMonth
    }

    val anchor = uiState.anchorPayday
    val today = LocalDate.now()
    var selectedDay by rememberSaveable { mutableStateOf(today) }

    // Filter modes: 0 = All, 1 = Unpaid Bills, 2 = Paid Bills, 3 = Paydays
    var selectedFilterIndex by rememberSaveable { mutableIntStateOf(0) }

    // Core Calendar Logic for displayMonth
    val monthStart = displayMonth.atDay(1)
    val monthEnd = displayMonth.atEndOfMonth()
    val firstDayOffset = (monthStart.dayOfWeek.value - 1) % 7 // Monday = 0

    val calendarDays = remember(displayMonth) {
        val days = mutableListOf<LocalDate>()
        // Back padding from previous month
        for (i in firstDayOffset downTo 1) {
            days.add(monthStart.minusDays(i.toLong()))
        }
        // Month days
        for (i in 1..displayMonth.lengthOfMonth()) {
            days.add(monthStart.plusDays((i - 1).toLong()))
        }
        // Front padding for next month
        while (days.size % 7 != 0) {
            days.add(monthEnd.plusDays((days.size - firstDayOffset - displayMonth.lengthOfMonth() + 1).toLong()))
        }
        days
    }

    // Calculate full monthly summary report using MonthlySummaryCalculator
    val monthReport = remember(displayMonth, uiState.allTransactions, uiState.plannedExpenses, uiState.allDebts, uiState.activeAccountName, uiState.defaultPaycheckAmount) {
        MonthlySummaryCalculator.calculate(
            month = displayMonth,
            uiState = uiState,
            activeAccountFilter = uiState.activeAccountName
        )
    }

    // Bills mapped by due date across the month
    val billsByDateMap = remember(monthReport.bills) {
        monthReport.bills.groupBy { it.dueDate }
    }

    // Incomes mapped by date across the month
    val incomesByDateMap = remember(monthReport.incomeItems) {
        monthReport.incomeItems.groupBy { it.date }
    }

    // Non-bill transactions mapped by date
    val extraTxsByDateMap = remember(calendarDays, uiState.allTransactions, uiState.activeAccountName, monthReport.bills) {
        val map = mutableMapOf<LocalDate, MutableList<TransactionEntity>>()
        val rawActiveTxList = if (uiState.activeAccountName == "All Accounts") {
            uiState.allTransactions
        } else {
            uiState.allTransactions.filter { it.accountName == uiState.activeAccountName }
        }
        val activeTxList = rawActiveTxList.filter {
            it.amount > 0.0 &&
            !it.isStartingBalanceOrAdjustment &&
            !it.title.contains("Cancelled", ignoreCase = true) &&
            !it.title.contains("Deleted", ignoreCase = true)
        }
        activeTxList.forEach { tx ->
            val date = DateUtils.millisToLocalDate(tx.dateMillis)
            val isAlreadyInBills = monthReport.bills.any { it.transactionRef?.id == tx.id }
            if (!isAlreadyInBills) {
                map.getOrPut(date) { mutableListOf() }.add(tx)
            }
        }
        map
    }

    // Selected day items
    val dayBills = remember(selectedDay, billsByDateMap) {
        billsByDateMap[selectedDay] ?: emptyList()
    }
    val dayIncomes = remember(selectedDay, incomesByDateMap) {
        incomesByDateMap[selectedDay] ?: emptyList()
    }
    val dayExtraTxs = remember(selectedDay, extraTxsByDateMap) {
        extraTxsByDateMap[selectedDay] ?: emptyList()
    }

    // Filter counts
    val totalUnpaidBillsCount = remember(monthReport.bills) {
        monthReport.bills.count { !it.isPaid }
    }
    val totalPaidBillsCount = remember(monthReport.bills) {
        monthReport.bills.count { it.isPaid }
    }
    val totalPaydaysCount = remember(calendarDays, anchor, displayMonth) {
        calendarDays.filter { it.month == displayMonth.month && it.year == displayMonth.year }
            .count { DateUtils.getPaydayOnOrBefore(it, anchor) == it }
    }

    val handleToggleBillPaid: (MonthlyBillItem) -> Unit = { bill ->
        val existingTx = bill.transactionRef
        if (existingTx != null) {
            val nextDone = !bill.isPaid
            val nextStatus = if (nextDone) "paid" else "due"
            onUpdateTransactionStatus?.invoke(existingTx, nextStatus) ?: onToggleCompleted(existingTx.copy(isCompleted = nextDone, status = nextStatus))
        } else {
            val targetAccount = if (bill.targetAccount.isNotBlank() && bill.targetAccount != "All Accounts") {
                bill.targetAccount
            } else if (uiState.activeAccountName != "All Accounts") {
                uiState.activeAccountName
            } else {
                "Primary Account"
            }
            val newTx = TransactionEntity(
                id = 0,
                title = bill.title,
                amount = bill.amount,
                dateMillis = DateUtils.localDateToMillis(bill.dueDate),
                category = bill.category.ifEmpty { "Planned Bill" },
                accountName = targetAccount,
                isIncome = false,
                isCompleted = true,
                isAutoPay = bill.isAutoPay,
                status = "paid"
            )
            onUpdateTransactionStatus?.invoke(newTx, "paid") ?: onToggleCompleted(newTx)
        }
    }

    val topBannerContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Month Navigation Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayMonth = displayMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            displayMonth = YearMonth.now()
                            selectedDay = today
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (displayMonth == YearMonth.now()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Current",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${monthReport.bills.size} scheduled bills • ${totalUnpaidBillsCount} due",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (displayMonth != YearMonth.now() || selectedDay != today) {
                            FilledTonalButton(
                                onClick = {
                                    displayMonth = YearMonth.now()
                                    selectedDay = today
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { displayMonth = displayMonth.plusMonths(1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                }
            }

            // Monthly Summary & Legend Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("INCOME", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                Text(
                                    text = String.format(Locale.US, "$%,.0f", monthReport.totalIncome),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF059669)
                                )
                            }
                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Column {
                                Text("BILLS & EXP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                Text(
                                    text = String.format(Locale.US, "$%,.0f", monthReport.totalPlannedExpenses),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626)
                                )
                            }
                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Column {
                                Text("REMAINING DUE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                Text(
                                    text = String.format(Locale.US, "$%,.0f", monthReport.totalRemainingDue),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
                                )
                            }
                            Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Column {
                                val net = monthReport.netSurplus
                                Text("NET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (net >= 0) Color(0xFF059669) else Color(0xFFDC2626))
                                Text(
                                    text = (if (net >= 0) "+" else "") + String.format(Locale.US, "$%,.0f", net),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (net >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                )
                            }
                        }

                        // Progress indicator for bills paid
                        val paidPercent = if (monthReport.totalPlannedExpenses > 0) {
                            ((monthReport.totalPaidSoFar / monthReport.totalPlannedExpenses) * 100).toInt().coerceIn(0, 100)
                        } else 100
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$paidPercent% Paid",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (paidPercent == 100) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LinearProgressIndicator(
                                progress = { paidPercent / 100f },
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF10B981),
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981).copy(alpha = 0.15f)) {
                            Text("💰 Payday", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981).copy(alpha = 0.15f)) {
                            Text("✓ Paid Bill", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEF4444).copy(alpha = 0.15f)) {
                            Text("⚠️ Due / Overdue", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF8B5CF6).copy(alpha = 0.15f)) {
                            Text("⚡ AutoPay", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }

            // Quick Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilterIndex == 0,
                    onClick = { selectedFilterIndex = 0 },
                    label = { Text("All Days", fontSize = 8.5.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(22.dp)
                )
                FilterChip(
                    selected = selectedFilterIndex == 1,
                    onClick = { selectedFilterIndex = 1 },
                    label = { Text("Unpaid Bills ($totalUnpaidBillsCount)", fontSize = 8.5.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(6.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFFDC2626)
                    ),
                    modifier = Modifier.height(22.dp)
                )
                FilterChip(
                    selected = selectedFilterIndex == 2,
                    onClick = { selectedFilterIndex = 2 },
                    label = { Text("Paid Bills ($totalPaidBillsCount)", fontSize = 8.5.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(6.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                        selectedLabelColor = Color(0xFF047857)
                    ),
                    modifier = Modifier.height(22.dp)
                )
                FilterChip(
                    selected = selectedFilterIndex == 3,
                    onClick = { selectedFilterIndex = 3 },
                    label = { Text("Paydays ($totalPaydaysCount)", fontSize = 8.5.sp, fontWeight = FontWeight.Medium) },
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.height(22.dp)
                )
            }
        }
    }

    val calendarGridContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Calendar Grid Header (Weeks: Mon - Sun)
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Days Grid
            val rows = calendarDays.size / 7
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (c in 0 until 7) {
                            val date = calendarDays[r * 7 + c]
                            val isCurrentMonth = date.month == displayMonth.month && date.year == displayMonth.year

                            // Corresponding Pay Period tint
                            val payPeriodIndex = uiState.payPeriods.indexOfFirst {
                                !it.startDate.isAfter(date) && !it.endDate.isBefore(date)
                            }

                            val isDark = uiState.isDarkMode
                            val cellBg = when {
                                date == selectedDay -> MaterialTheme.colorScheme.primaryContainer
                                !isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                payPeriodIndex == 0 -> if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF)
                                payPeriodIndex == 1 -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5)
                                payPeriodIndex == 2 -> if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFFFBEB)
                                payPeriodIndex == 3 -> if (isDark) Color(0xFF581C87).copy(alpha = 0.5f) else Color(0xFFFDF4FF)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            }

                            val isToday = date == today
                            val isPayday = DateUtils.getPaydayOnOrBefore(date, anchor) == date

                            // Aggregate bills & transactions on this cell date
                            val cellBills = billsByDateMap[date] ?: emptyList()
                            val cellIncomes = incomesByDateMap[date] ?: emptyList()
                            val cellExtraTxs = extraTxsByDateMap[date] ?: emptyList()

                            val unpaidBillsAmount = cellBills.filter { !it.isPaid }.sumOf { it.amount } + cellExtraTxs.filter { !it.isIncome && !it.isPaidOrCleared }.sumOf { it.amount }
                            val paidBillsAmount = cellBills.filter { it.isPaid }.sumOf { it.amount } + cellExtraTxs.filter { !it.isIncome && it.isPaidOrCleared }.sumOf { it.amount }
                            val totalBillsCount = cellBills.size + cellExtraTxs.count { !it.isIncome }
                            val totalIncomeAmount = cellIncomes.sumOf { it.amount } + cellExtraTxs.filter { it.isIncome }.sumOf { it.amount }

                            // Filter dimming
                            val matchesFilter = when (selectedFilterIndex) {
                                1 -> unpaidBillsAmount > 0
                                2 -> paidBillsAmount > 0
                                3 -> isPayday || totalIncomeAmount > 0
                                else -> true
                            }
                            val cellAlpha = if (matchesFilter || !isCurrentMonth) 1.0f else 0.4f

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cellBg)
                                    .border(
                                        border = if (date == selectedDay) {
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                        } else if (isToday) {
                                            BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
                                        } else {
                                            BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedDay = date }
                                    .testTag("calendar_day_$date"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 3.dp, horizontal = 2.dp)
                                ) {
                                    // Top Row: Day number + Payday icon
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                date == selectedDay -> MaterialTheme.colorScheme.onPrimaryContainer
                                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = cellAlpha)
                                            }
                                        )

                                        if (isPayday) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("💰", fontSize = 9.sp)
                                        }
                                    }

                                    // Center Badges: Income / Bills
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        if (totalIncomeAmount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (date == selectedDay)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                else
                                                    Color(0xFF10B981).copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "+${formatCalendarAmount(totalIncomeAmount)}",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (date == selectedDay) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF047857),
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        }

                                        if (unpaidBillsAmount > 0) {
                                            val isOverdue = date.isBefore(today)
                                            val isDueToday = date == today
                                            val badgeBg = when {
                                                date == selectedDay -> MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                                isOverdue -> Color(0xFFEF4444).copy(alpha = 0.25f)
                                                isDueToday -> Color(0xFFF59E0B).copy(alpha = 0.25f)
                                                else -> Color(0xFFEF4444).copy(alpha = 0.18f)
                                            }
                                            val badgeText = when {
                                                isOverdue -> "⚠️-${formatCalendarAmount(unpaidBillsAmount)}"
                                                isDueToday -> "⚡-${formatCalendarAmount(unpaidBillsAmount)}"
                                                else -> "-${formatCalendarAmount(unpaidBillsAmount)}"
                                            }
                                            val badgeColor = when {
                                                date == selectedDay -> MaterialTheme.colorScheme.onErrorContainer
                                                isOverdue -> Color(0xFFB91C1C)
                                                isDueToday -> Color(0xFFB45309)
                                                else -> Color(0xFFB91C1C)
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = badgeBg
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        } else if (paidBillsAmount > 0) {
                                            // All bills on this day are settled/paid!
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "✓-${formatCalendarAmount(paidBillsAmount)}",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Bottom Indicator Dots for scheduled bills
                                    if (totalBillsCount > 0) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val displayBills = cellBills.take(3)
                                            displayBills.forEach { b ->
                                                val dotColor = when {
                                                    b.isPaid -> Color(0xFF10B981)
                                                    b.isAutoPay -> Color(0xFF8B5CF6)
                                                    date.isBefore(today) -> Color(0xFFEF4444)
                                                    else -> Color(0xFFF59E0B)
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                )
                                            }
                                            if (totalBillsCount > 3) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(3.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val dailyDetailCardContent: @Composable () -> Unit = {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isWideScreen) 0.dp else 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Date & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (selectedDay == today) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        "Today",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        // Day Summary Badges
                        val dayIncomeTotal = dayIncomes.sumOf { it.amount } + dayExtraTxs.filter { it.isIncome }.sumOf { it.amount }
                        val dayBillsTotal = dayBills.sumOf { it.amount } + dayExtraTxs.filter { !it.isIncome }.sumOf { it.amount }
                        val dayUnpaidTotal = dayBills.filter { !it.isPaid }.sumOf { it.amount } + dayExtraTxs.filter { !it.isIncome && !it.isPaidOrCleared }.sumOf { it.amount }

                        if (dayIncomeTotal > 0 || dayBillsTotal > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (dayIncomeTotal > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "+$%,.2f In", dayIncomeTotal),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (dayBillsTotal > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "-$%,.2f Bills", dayBillsTotal),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                if (dayUnpaidTotal > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f Due", dayUnpaidTotal),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { onAddTransactionClick(selectedDay) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_transaction_for_day_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                val hasAnyItems = dayBills.isNotEmpty() || dayIncomes.isNotEmpty() || dayExtraTxs.isNotEmpty()

                if (!hasAnyItems) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("🛡️", fontSize = 28.sp)
                            Text(
                                "Safe! No bills or transactions scheduled today.",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val isDark = isSystemInDarkTheme()

                        // 1. Scheduled & Planned Bills on this date
                        if (dayBills.isNotEmpty()) {
                            Text(
                                text = "BILLS & EXPENSES (${dayBills.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 0.8.sp
                            )

                            dayBills.forEach { bill ->
                                val isOverdue = !bill.isPaid && bill.dueDate.isBefore(today)
                                val isDueToday = !bill.isPaid && bill.dueDate == today
                                val billBg = when {
                                    bill.isPaid -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    isOverdue -> if (isDark) Color(0xFF450A0A).copy(alpha = 0.35f) else Color(0xFFFEF2F2)
                                    isDueToday -> if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFFBEB)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                }
                                val billBorder = when {
                                    bill.isPaid -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    isOverdue -> BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                                    isDueToday -> BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
                                    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                }

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bill_calendar_item_${bill.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = billBg,
                                    border = billBorder
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Left: Checkmark button + Title & Details
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            IconButton(
                                                onClick = { handleToggleBillPaid(bill) },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                    contentDescription = if (bill.isPaid) "Mark as Due" else "Mark as Paid",
                                                    tint = if (bill.isPaid) Color(0xFF10B981) else (if (isOverdue) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant),
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Column {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = bill.title,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                                        textDecoration = if (bill.isPaid) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                    if (bill.isAutoPay) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                "⚡ AutoPay",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF7C3AED),
                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    val statusLabel = when {
                                                        bill.isPaid -> "PAID ✓"
                                                        isOverdue -> "⚠️ OVERDUE"
                                                        isDueToday -> "⚡ DUE TODAY"
                                                        else -> "DUE ${bill.dueDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}"
                                                    }
                                                    val statusColor = when {
                                                        bill.isPaid -> Color(0xFF047857)
                                                        isOverdue -> Color(0xFFDC2626)
                                                        isDueToday -> Color(0xFFD97706)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                    Text(
                                                        text = statusLabel,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )

                                                    if (bill.targetAccount.isNotBlank() && !bill.targetAccount.equals("Primary Account", ignoreCase = true) && !bill.targetAccount.equals("All Accounts", ignoreCase = true)) {
                                                        Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                                        Text(
                                                            text = bill.targetAccount,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Right: Amount + Quick Action
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format(Locale.US, "-$%,.2f", bill.amount),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (bill.isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else Color(0xFFDC2626),
                                                textDecoration = if (bill.isPaid) TextDecoration.LineThrough else TextDecoration.None
                                            )
                                            Text(
                                                text = bill.source,
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Incomes on this date
                        if (dayIncomes.isNotEmpty()) {
                            Text(
                                text = "INCOMES & PAYDAYS (${dayIncomes.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            dayIncomes.forEach { income ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("💰", fontSize = 18.sp)
                                            Column {
                                                Text(
                                                    text = income.title,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (income.isProjected) "Projected Payday" else "Income Received",
                                                    fontSize = 10.sp,
                                                    color = Color(0xFF047857),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        Text(
                                            text = String.format(Locale.US, "+$%,.2f", income.amount),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Other recorded transactions on this date
                        if (dayExtraTxs.isNotEmpty()) {
                            Text(
                                text = "OTHER TRANSACTIONS (${dayExtraTxs.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )

                            for (tx in dayExtraTxs) {
                                SwipeableTransactionRow(
                                    transaction = tx,
                                    onDelete = { onDeleteTransaction(tx) },
                                    onEdit = { onEditTransaction(tx) },
                                    onDuplicate = { onDuplicateTransaction?.invoke(tx) },
                                    onUpdateStatus = { newSt ->
                                        onUpdateTransactionStatus?.invoke(tx, newSt) ?: onToggleCompleted(tx)
                                    },
                                    onToggleCompleted = { onToggleCompleted(tx) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        TransactionItemRow(
                                            transaction = tx,
                                            onDelete = { onDeleteTransaction(tx) },
                                            onEdit = { onEditTransaction(tx) },
                                            onDuplicate = { onDuplicateTransaction?.invoke(tx) },
                                            onToggleCompleted = { onToggleCompleted(tx) },
                                            onUpdateStatus = { newSt ->
                                                onUpdateTransactionStatus?.invoke(tx, newSt) ?: onToggleCompleted(tx)
                                            },
                                            onToggleReminder = { days ->
                                                onToggleReminder?.invoke(tx, days)
                                            },
                                            showDateChip = false,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useSideBySide = isWideScreen && maxWidth >= 680.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            topBannerContent()

            if (useSideBySide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        calendarGridContent()
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        dailyDetailCardContent()
                    }
                }
            } else {
                calendarGridContent()
                dailyDetailCardContent()
            }
        }
    }
}

@Composable
fun SettingsView(
    uiState: BudgetUiState,
    onUpdateDefaultPaycheck: (Double) -> Unit,
    onUpdateAnchorDay: (LocalDate) -> Unit,
    onAddIncomeSource: (String, Double, String) -> Unit,
    onDeleteIncomeSource: (IncomeSourceEntity) -> Unit,
    onToggleDarkMode: () -> Unit,
    onSelectTheme: (AppTheme) -> Unit = {},
    onSelectAppIcon: (AppIcon) -> Unit = {},
    onExportBackup: suspend () -> String,
    onImportBackup: suspend (String) -> Boolean,
    onConnectGoogleDrive: () -> Unit,
    onSaveCustomDriveToken: (String) -> Unit = {},
    onSaveCustomDriveCredentials: (String?, String?, String?, String?) -> Unit = { _, _, _, _ -> },
    onGetCustomClientId: () -> String? = { null },
    onGetCustomClientSecret: () -> String? = { null },
    onGetCustomRefreshToken: () -> String? = { null },
    onGetCustomDriveToken: () -> String? = { null },
    onManualSyncDrive: () -> Unit,
    onToggleDriveAutoSync: (Boolean) -> Unit,
    onConnectCustomSyncEmail: (String) -> Unit = {},
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    onListDriveFiles: suspend () -> Pair<List<com.example.sync.DriveFileInfo>, String?> = { Pair(emptyList(), null) },
    onImportSpecificDriveFile: suspend (com.example.sync.DriveFileInfo) -> Pair<Boolean, String> = { Pair(false, "") },
    onImportGoogleSheetByUrlOrId: suspend (String) -> Pair<Boolean, String> = { Pair(false, "") },
    onExportToGoogleSheets: (((String) -> Unit) -> Unit)? = null,
    onExportCsv: suspend () -> String = { "" },
    onImportCsv: suspend (String) -> Boolean = { false },
    onImportAnyDataDetailed: suspend (String) -> Pair<Boolean, String> = { Pair(false, "") },
    onExportWebCompanionToGoogleDrive: (suspend (String) -> Pair<Boolean, String>)? = null,
    onRestoreFinancialData: () -> Unit = {},
    onClearDatabase: () -> Unit = {},
    isWideScreen: Boolean = false
) {
    var showAuthorizeDriveDialog by remember { mutableStateOf(false) }
    var showClearDataConfirmDialog by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingEmailInput by remember { mutableStateOf(uiState.driveAccountEmail ?: "") }
    var driveTokenInput by remember { mutableStateOf(onGetCustomDriveToken() ?: "") }
    var clientIdInput by remember { mutableStateOf(onGetCustomClientId() ?: "") }
    var clientSecretInput by remember { mutableStateOf(onGetCustomClientSecret() ?: "") }
    var refreshTokenInput by remember { mutableStateOf(onGetCustomRefreshToken() ?: "") }
    var loginEmailInput by remember { mutableStateOf(uiState.driveAccountEmail ?: "86brancan@gmail.com") }
    var showAdvancedOAuth by remember { mutableStateOf(false) }
    var themeCardExpanded by remember { mutableStateOf(false) }
    var payScheduleExpanded by remember { mutableStateOf(false) }
    var remindersExpanded by remember { mutableStateOf(false) }
    var webCompanionExpanded by remember { mutableStateOf(false) }
    var showAddIncomeSourceDialog by remember { mutableStateOf(false) }
    var showEditAnchorDialog by remember { mutableStateOf(false) }
    var showEditDefaultPaycheckDialog by remember { mutableStateOf(false) }
    var driveSyncExpanded by remember { mutableStateOf(false) }
    var sheetsSyncExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var dangerZoneExpanded by remember { mutableStateOf(false) }
    val localContext = LocalContext.current
    var showManufacturerGuideDialog by remember { mutableStateOf(false) }
    var keepAliveServiceEnabled by remember {
        mutableStateOf(com.example.notification.NotificationReliabilityHelper.isKeepAliveServiceEnabled(localContext))
    }
    var isBatteryIgnored by remember {
        mutableStateOf(com.example.notification.NotificationReliabilityHelper.isBatteryOptimizationIgnored(localContext))
    }
    var canScheduleExact by remember {
        mutableStateOf(com.example.notification.NotificationReliabilityHelper.canScheduleExactAlarms(localContext))
    }
    var hasNotifPermission by remember {
        mutableStateOf(com.example.notification.NotificationReliabilityHelper.isNotificationPermissionGranted(localContext))
    }

    val refreshDiagnosticStates: () -> Unit = {
        keepAliveServiceEnabled = com.example.notification.NotificationReliabilityHelper.isKeepAliveServiceEnabled(localContext)
        isBatteryIgnored = com.example.notification.NotificationReliabilityHelper.isBatteryOptimizationIgnored(localContext)
        canScheduleExact = com.example.notification.NotificationReliabilityHelper.canScheduleExactAlarms(localContext)
        hasNotifPermission = com.example.notification.NotificationReliabilityHelper.isNotificationPermissionGranted(localContext)
    }

    val settingsCoroutineScope = rememberCoroutineScope()
    var filePickerMode by remember { mutableStateOf("backup") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var importErrorMsg by remember { mutableStateOf("") }
    var isImportingProgress by remember { mutableStateOf(false) }

    var showCsvImportDialog by remember { mutableStateOf(false) }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var csvExportData by remember { mutableStateOf("") }
    var csvImportInput by remember { mutableStateOf("") }
    var csvStatusMsg by remember { mutableStateOf("") }
    var isSyncingCsv by remember { mutableStateOf(false) }
    var isExportingToSheets by remember { mutableStateOf(false) }

    var showSyncImportPickerDialog by remember { mutableStateOf(false) }
    var driveFileList by remember { mutableStateOf<List<com.example.sync.DriveFileInfo>>(emptyList()) }
    var driveFileSearchQuery by remember { mutableStateOf("") }
    var isDriveFilesLoading by remember { mutableStateOf(false) }
    var driveFilesErrorMsg by remember { mutableStateOf<String?>(null) }
    var importingDriveFileId by remember { mutableStateOf<String?>(null) }
    var sheetUrlOrIdInput by remember { mutableStateOf("") }
    var isImportingSheetUrl by remember { mutableStateOf(false) }
    var showTokenBoxInPicker by remember { mutableStateOf(false) }
    var customTokenInputInPicker by remember { mutableStateOf(onGetCustomDriveToken() ?: "") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        uri?.let {
            settingsCoroutineScope.launch {
                try {
                    val (content, pickedFileName) = com.example.sync.SpreadsheetFileParser.extractContentFromUri(localContext, uri)

                    if (!content.isNullOrBlank()) {
                        csvImportInput = content
                        importInputText = content
                        val (ok, msg) = onImportAnyDataDetailed(content)
                        if (ok) {
                            csvStatusMsg = msg
                            Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                            showCsvImportDialog = false
                            showImportDialog = false
                            showSyncImportPickerDialog = false
                        } else {
                            var fallbackOk = onImportCsv(content)
                            if (!fallbackOk) {
                                fallbackOk = onImportBackup(content)
                            }
                            if (fallbackOk) {
                                csvStatusMsg = "✅ File '$pickedFileName' imported and synced with local budget!"
                                Toast.makeText(localContext, csvStatusMsg, Toast.LENGTH_SHORT).show()
                                showCsvImportDialog = false
                                showImportDialog = false
                                showSyncImportPickerDialog = false
                            } else {
                                csvStatusMsg = msg
                                importErrorMsg = msg
                                Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                if (filePickerMode == "csv") showCsvImportDialog = true else showImportDialog = true
                            }
                        }
                    } else {
                        // If file is a Google Sheet or virtual document, try fetching via Google Drive
                        val (sheetOk, sheetMsg) = onImportGoogleSheetByUrlOrId(pickedFileName)
                        if (sheetOk) {
                            csvStatusMsg = sheetMsg
                            Toast.makeText(localContext, sheetMsg, Toast.LENGTH_LONG).show()
                            showCsvImportDialog = false
                            showImportDialog = false
                            showSyncImportPickerDialog = false
                        } else {
                            Toast.makeText(localContext, "Selected '$pickedFileName'. You can also tap 'Import' directly in the Google Drive Files list.", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(localContext, "Error opening file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotifPermission = isGranted
        com.example.notification.BillReminderScheduler.sendTestNotification(
            localContext,
            title = "Electric Bill Reminder",
            amount = 84.50
        )
    }

    if (showManufacturerGuideDialog) {
        val guideData = remember { com.example.notification.NotificationReliabilityHelper.getManufacturerGuide() }
        AlertDialog(
            onDismissRequest = { showManufacturerGuideDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(guideData.first, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Many phone manufacturers enforce aggressive power-management rules that kill background processes or delay alarms. Follow these steps to ensure 100% on-time reminders:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    guideData.second.forEach { step ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = step,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showManufacturerGuideDialog = false }) {
                    Text("Understood")
                }
            }
        )
    }

    if (showClearDataConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Clear Entire Database?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently clear all transactions, income sources, planned bills, and account settings? This action cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearDatabase()
                        showClearDataConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_database_button")
                ) {
                    Text("Confirm Clear", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAuthorizeDriveDialog) {
        AlertDialog(
            onDismissRequest = { showAuthorizeDriveDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Google Drive & Cloud Sync", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Google Drive & Google Sheets permanent sync is active for your account:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Google Account: ${uiState.driveAccountEmail ?: "86brancan@gmail.com"}",
                                    color = Color(0xFF047857),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Permanent OAuth Scopes: Google Drive & Google Sheets enabled.",
                                color = Color(0xFF065F46),
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showAuthorizeDriveDialog = false
                                onConnectGoogleDrive()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("sign_in_google_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("1-Tap Google Sign-In Authorization", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "Linked Google Email for Cloud Sync:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = loginEmailInput,
                        onValueChange = { loginEmailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("86brancan@gmail.com") },
                        modifier = Modifier.fillMaxWidth().testTag("email_login_dialog_input"),
                        singleLine = true
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    TextButton(
                        onClick = { showAdvancedOAuth = !showAdvancedOAuth },
                        modifier = Modifier.fillMaxWidth().testTag("toggle_advanced_oauth_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (showAdvancedOAuth) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (showAdvancedOAuth) "Hide Advanced Settings" else "Show Optional Token / Custom Credentials",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (showAdvancedOAuth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = driveTokenInput,
                                onValueChange = { driveTokenInput = it },
                                label = { Text("Custom Access Token (Optional)") },
                                placeholder = { Text("Auto-managed via Google Sign-In") },
                                modifier = Modifier.fillMaxWidth().testTag("dialog_oauth_token_input"),
                                maxLines = 2,
                                singleLine = false,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                            )

                            OutlinedTextField(
                                value = clientIdInput,
                                onValueChange = { clientIdInput = it },
                                label = { Text("Client ID (Optional)") },
                                placeholder = { Text("Auto-configured") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = clientSecretInput,
                                onValueChange = { clientSecretInput = it },
                                label = { Text("Client Secret (Optional)") },
                                placeholder = { Text("Auto-configured") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = refreshTokenInput,
                                onValueChange = { refreshTokenInput = it },
                                label = { Text("Refresh Token (Optional)") },
                                placeholder = { Text("Auto-managed") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                singleLine = false
                            )
                        }
                    }

                    val isConfigured = !onGetCustomClientId().isNullOrBlank() || !onGetCustomDriveToken().isNullOrBlank() || uiState.driveAccountEmail != null
                    if (isConfigured) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "✓ Active Account: ${uiState.driveAccountEmail ?: "Connected"}",
                                color = Color(0xFF047857),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = loginEmailInput.trim()
                        if (email.isNotEmpty()) {
                            onConnectCustomSyncEmail(email)
                        }
                        val token = driveTokenInput.trim()
                        val cId = clientIdInput.trim()
                        val cSecret = clientSecretInput.trim()
                        val rToken = refreshTokenInput.trim()
                        onSaveCustomDriveCredentials(
                            cId.ifBlank { null },
                            cSecret.ifBlank { null },
                            rToken.ifBlank { null },
                            token.ifBlank { null }
                        )
                        showAuthorizeDriveDialog = false
                        onManualSyncDrive()
                        onSyncCsvDrive?.invoke {}
                    },
                    modifier = Modifier.testTag("save_drive_credentials_button")
                ) {
                    Text("Save & Sync")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hasExisting = !onGetCustomClientId().isNullOrBlank() || !onGetCustomDriveToken().isNullOrBlank() || uiState.driveAccountEmail != null
                    if (hasExisting) {
                        TextButton(
                            onClick = {
                                onSaveCustomDriveCredentials(null, null, null, null)
                                onConnectCustomSyncEmail("")
                                driveTokenInput = ""
                                clientIdInput = ""
                                clientSecretInput = ""
                                refreshTokenInput = ""
                                loginEmailInput = ""
                                showAuthorizeDriveDialog = false
                            }
                        ) {
                            Text("Clear Credentials", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { showAuthorizeDriveDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    val cardPaySchedule: @Composable () -> Unit = {
        // --- Card: Pay Schedule & Recurring Income ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { payScheduleExpanded = !payScheduleExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("💵", fontSize = 18.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Pay Schedule & Recurring Income",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Next Payday: ${uiState.currentPayday}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { payScheduleExpanded = !payScheduleExpanded }) {
                        Icon(
                            imageVector = if (payScheduleExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (payScheduleExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                if (payScheduleExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Anchor Payday row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Anchor Payday Date",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Reference date for 14-day pay cycle: ${uiState.anchorPayday}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { showEditAnchorDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Change Date", fontSize = 12.sp)
                        }
                    }

                    // Default Paycheck row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Default Paycheck Amount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Base bi-weekly deposit for cycles",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "$%,.2f", uiState.defaultPaycheckAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF10B981)
                            )
                            IconButton(
                                onClick = { showEditDefaultPaycheckDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit default paycheck", modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Recurring Income Streams section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Recurring Income Streams",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (uiState.incomeSources.isEmpty())
                                    "Using primary paycheck only. Add side gigs, salary, or secondary jobs."
                                else
                                    "${uiState.incomeSources.size} recurring stream(s) configured",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showAddIncomeSourceDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ Add Stream", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (uiState.incomeSources.isNotEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            uiState.incomeSources.forEach { source ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = source.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = source.frequency,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "+$%,.2f", source.amount),
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF10B981)
                                            )
                                            IconButton(
                                                onClick = { onDeleteIncomeSource(source) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete income source",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditAnchorDialog) {
        var anchorInput by remember { mutableStateOf(uiState.anchorPayday.toString()) }
        var anchorError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEditAnchorDialog = false },
            title = { Text("Change Anchor Payday", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Set the reference date from which all 14-day bi-weekly pay cycles and pay weeks are calculated.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    DatePickerTextField(
                        value = anchorInput,
                        onValueChange = { anchorInput = it; anchorError = "" },
                        label = { Text("Anchor Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (anchorError.isNotEmpty()) {
                        Text(anchorError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = try { LocalDate.parse(anchorInput.trim()) } catch (e: Exception) { null }
                        if (parsed != null) {
                            onUpdateAnchorDay(parsed)
                            showEditAnchorDialog = false
                        } else {
                            anchorError = "Please enter a valid date (YYYY-MM-DD)"
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAnchorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showEditDefaultPaycheckDialog) {
        var defaultPaycheckInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", uiState.defaultPaycheckAmount)) }
        var defaultPaycheckError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showEditDefaultPaycheckDialog = false },
            title = { Text("Edit Default Paycheck", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter your standard bi-weekly paycheck deposit amount.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = defaultPaycheckInput,
                        onValueChange = { defaultPaycheckInput = it; defaultPaycheckError = "" },
                        label = { Text("Amount ($)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (defaultPaycheckError.isNotEmpty()) {
                        Text(defaultPaycheckError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = defaultPaycheckInput.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onUpdateDefaultPaycheck(parsed)
                            showEditDefaultPaycheckDialog = false
                        } else {
                            defaultPaycheckError = "Please enter a valid amount"
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDefaultPaycheckDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddIncomeSourceDialog) {
        var sourceName by remember { mutableStateOf("") }
        var sourceAmount by remember { mutableStateOf("") }
        var sourceFreq by remember { mutableStateOf("Bi-Weekly") }
        var sourceError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddIncomeSourceDialog = false },
            title = { Text("Add Recurring Income Stream", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = { sourceName = it; sourceError = "" },
                        label = { Text("Stream Name (e.g. Salary, Side Gig)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceAmount,
                        onValueChange = { sourceAmount = it; sourceError = "" },
                        label = { Text("Amount ($)") },
                        prefix = { Text("$") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Column {
                        Text("Frequency:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Bi-Weekly", "Weekly", "Monthly").forEach { f ->
                                FilterChip(
                                    selected = sourceFreq == f,
                                    onClick = { sourceFreq = f },
                                    label = { Text(f, fontSize = 9.5.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                    if (sourceError.isNotEmpty()) {
                        Text(sourceError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = sourceAmount.toDoubleOrNull()
                        if (sourceName.isBlank()) {
                            sourceError = "Please enter a stream name"
                        } else if (parsed == null || parsed <= 0.0) {
                            sourceError = "Please enter a valid amount"
                        } else {
                            onAddIncomeSource(sourceName.trim(), parsed, sourceFreq)
                            showAddIncomeSourceDialog = false
                        }
                    }
                ) {
                    Text("Add Stream")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIncomeSourceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val cardRemindersAndThemes: @Composable () -> Unit = {
        // --- Card: Expense Notification Reminders & Background Reliability ---
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { remindersExpanded = !remindersExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notifications",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Bill & Expense Reminders",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automated on-time alerts with persistent background reliability",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { remindersExpanded = !remindersExpanded }) {
                            Icon(
                                imageVector = if (remindersExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (remindersExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    if (remindersExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        val scheduledCount = remember(uiState.allTransactions, uiState.plannedExpenses) {
                            val txReminders = uiState.allTransactions.count { !it.isIncome && !it.isCompleted && it.reminderDaysBefore >= 0 }
                            val billReminders = uiState.plannedExpenses.count { it.reminderDaysBefore >= 0 }
                            txReminders + billReminders
                        }

                    // Active alerts and instant test row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Active Scheduled Reminders",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "$scheduledCount Active Alerts",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        com.example.notification.BillReminderScheduler.rescheduleAll(
                                            localContext,
                                            uiState.plannedExpenses,
                                            uiState.allTransactions
                                        )
                                        refreshDiagnosticStates()
                                        Toast.makeText(
                                            localContext,
                                            "Rescheduled $scheduledCount alerts & refreshed background work",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reschedule", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                            ContextCompat.checkSelfPermission(localContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            com.example.notification.BillReminderScheduler.sendTestNotification(
                                                localContext,
                                                title = "Electric Bill Reminder",
                                                amount = 84.50
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("send_test_notification_button")
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Test Alert", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // --- Card: Keep App Active (Foreground Service) ---
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (keepAliveServiceEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (keepAliveServiceEnabled) Color(0xFF10B981).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            tint = if (keepAliveServiceEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(6.dp).size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Keep App Running in Background",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Runs a persistent foreground service so Android and OEM task killers never stop reminders when swiped away or phone is locked.",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = keepAliveServiceEnabled,
                                    onCheckedChange = { enabled ->
                                        com.example.notification.NotificationReliabilityHelper.setKeepAliveServiceEnabled(localContext, enabled)
                                        keepAliveServiceEnabled = enabled
                                        refreshDiagnosticStates()
                                        Toast.makeText(
                                            localContext,
                                            if (enabled) "✓ Persistent background monitor active" else "Background monitor stopped",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }

                            if (keepAliveServiceEnabled) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("●", color = Color(0xFF10B981), fontSize = 12.sp)
                                        Text(
                                            text = "Active in notification tray • RAM protected from low-memory kill",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "💡 Tip: Enable this if your phone silences or skips bill reminders after closing the app.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // --- Section: System Diagnostics Checklist ---
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "System Reliability Diagnostics",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            IconButton(
                                onClick = { refreshDiagnosticStates() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh status", modifier = Modifier.size(16.dp))
                            }
                        }

                        // 1. Battery Optimization Diagnostic Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isBatteryIgnored) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (isBatteryIgnored) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Battery Optimization", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (isBatteryIgnored) "Unrestricted (Allowed to wake device)" else "Optimized (Android may delay alarms)",
                                            fontSize = 10.sp,
                                            color = if (isBatteryIgnored) Color(0xFF047857) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        com.example.notification.NotificationReliabilityHelper.openBatteryOptimizationSettings(localContext)
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(if (isBatteryIgnored) "Adjust" else "Exempt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 2. Exact Alarm Diagnostic Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (canScheduleExact) Icons.Default.CheckCircle else Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (canScheduleExact) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Exact Alarms (Alarms & Reminders)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (canScheduleExact) "Granted (Exact minute delivery)" else "Restricted (Alarms will be batched)",
                                            fontSize = 10.sp,
                                            color = if (canScheduleExact) Color(0xFF047857) else Color(0xFFB45309)
                                        )
                                    }
                                }

                                if (!canScheduleExact) {
                                    TextButton(
                                        onClick = {
                                            com.example.notification.NotificationReliabilityHelper.openExactAlarmSettings(localContext)
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Allow", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 3. Notification Permission Row
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (hasNotifPermission) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (hasNotifPermission) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text("Notification Permission", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            text = if (hasNotifPermission) "Granted (Status bar popups allowed)" else "Blocked (Notifications cannot display)",
                                            fontSize = 10.sp,
                                            color = if (hasNotifPermission) Color(0xFF047857) else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }

                                if (!hasNotifPermission) {
                                    Button(
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            } else {
                                                com.example.notification.NotificationReliabilityHelper.openAppNotificationSettings(localContext)
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // 4. Device Manufacturer Guide Button
                        OutlinedButton(
                            onClick = { showManufacturerGuideDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Device Manufacturer Guide (${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

            // --- Card 3: Visual Theme & Appearance ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { themeCardExpanded = !themeCardExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Themes",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Visual Theme & Appearance",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (uiState.isDarkMode) "Dark Mode active • Custom color palette" else "Light Mode active • Custom color palette",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { themeCardExpanded = !themeCardExpanded }) {
                            Icon(
                                imageVector = if (themeCardExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (themeCardExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    if (themeCardExpanded) {
                        // Dark Mode Toggle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dark Mode Theme",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Switch between light and dark background modes for comfortable viewing.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Switch(
                            checked = uiState.isDarkMode,
                            onCheckedChange = { onToggleDarkMode() },
                            modifier = Modifier.testTag("theme_dark_mode_toggle"),
                            thumbContent = {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = if (uiState.isDarkMode) "Dark mode active" else "Light mode active",
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Color Theme Palettes Selector
                    var themesExpanded by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = { themesExpanded = !themesExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_theme_palette_header"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Color Theme Palette",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        ) {
                                            Text(
                                                text = uiState.appTheme.displayName,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (themesExpanded) "Tap to collapse theme choices" else "Tap to view and change themes",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = if (themesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (themesExpanded) "Collapse themes" else "Expand themes",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (themesExpanded) {
                            // Dedicated Color Theory Spectrum Feature Banner
                            val isColorTheorySelected = uiState.appTheme == AppTheme.COLOR_THEORY
                            Surface(
                                onClick = { onSelectTheme(AppTheme.COLOR_THEORY) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_theme_color_theory"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isColorTheorySelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(2.dp, if (isColorTheorySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "🎨", fontSize = 20.sp)
                                            Column {
                                                Text(
                                                    text = "Color Theory Triadic Spectrum",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "60-30-10 Visual Balance • Triadic Harmony",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (isColorTheorySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                        ) {
                                            Text(
                                                text = if (isColorTheorySelected) "ACTIVE" else "SELECT",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isColorTheorySelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    // Triadic Swatch Preview Row
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF2DD4BF),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Teal Growth",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F3832),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF818CF8),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Indigo Trust",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E1B4B),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFFBBF24),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Amber Gold",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF451A03),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Dedicated 8-Bit Retro Mode Button
                            val is8BitSelected = uiState.appTheme == AppTheme.RETRO_8BIT
                            Surface(
                                onClick = { onSelectTheme(AppTheme.RETRO_8BIT) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_theme_8bit_retro"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (is8BitSelected) Color(0xFF003815) else Color(0xFF1E261E),
                                border = BorderStroke(2.dp, if (is8BitSelected) Color(0xFF00FF66) else Color(0xFF00AA44))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "🕹️",
                                            fontSize = 24.sp
                                        )
                                        Column {
                                            Text(
                                                text = "8-BIT RETRO ARCADE",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color(0xFF00FF66)
                                            )
                                            Text(
                                                text = "Transform entire app into 90s pixel graphics & neon arcade green",
                                                fontSize = 10.sp,
                                                color = Color(0xFF88FFB4)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onSelectTheme(AppTheme.RETRO_8BIT) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (is8BitSelected) Color(0xFFFF0055) else Color(0xFF00AA44),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (is8BitSelected) "ACTIVE 🎮" else "ENABLE 8-BIT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Dedicated E-Reader Sepia Mode Button
                            val isEReaderSelected = uiState.appTheme == AppTheme.EREADER
                            Surface(
                                onClick = { onSelectTheme(AppTheme.EREADER) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_theme_ereader"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (isEReaderSelected) Color(0xFF382A22) else Color(0xFFEFE6D5),
                                border = BorderStroke(2.dp, if (isEReaderSelected) Color(0xFFE8D3BC) else Color(0xFF5C4033))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "📖",
                                            fontSize = 24.sp
                                        )
                                        Column {
                                            Text(
                                                text = "E-READER SEPIA PAPER",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = if (isEReaderSelected) Color(0xFFF5EDDE) else Color(0xFF2A1C15)
                                            )
                                            Text(
                                                text = "Warm paper parchment & high contrast sepia ink for comfortable reading",
                                                fontSize = 10.sp,
                                                color = if (isEReaderSelected) Color(0xFFD4C3AD) else Color(0xFF5C4033)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { onSelectTheme(AppTheme.EREADER) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isEReaderSelected) Color(0xFFE8D3BC) else Color(0xFF5C4033),
                                            contentColor = if (isEReaderSelected) Color(0xFF2A1C15) else Color.White
                                        ),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (isEReaderSelected) "ACTIVE 📖" else "SELECT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            val themes = AppTheme.values()
                            val rows = themes.toList().chunked(2)
                            for (rowThemes in rows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (theme in rowThemes) {
                                        val isSelected = uiState.appTheme == theme
                                        Surface(
                                            onClick = { onSelectTheme(theme) },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("theme_option_${theme.id}"),
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(if (uiState.isDarkMode) theme.primaryColorDark else theme.primaryColorLight)
                                                        )
                                                        Box(
                                                            modifier = Modifier
                                                                .size(16.dp)
                                                                .clip(CircleShape)
                                                                .background(if (uiState.isDarkMode) theme.containerColorDark else theme.containerColorLight)
                                                        )
                                                    }
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = "Selected",
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = theme.displayName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = theme.description,
                                                    fontSize = 10.sp,
                                                    lineHeight = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (rowThemes.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // --- App Launcher Icon Selector ---
                    var appIconsExpanded by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            onClick = { appIconsExpanded = !appIconsExpanded },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_app_icon_header"),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Mini Active Icon Badge
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(uiState.appIcon.primaryColor, uiState.appIcon.secondaryColor)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = uiState.appIcon.previewForegroundRes),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }

                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "App Launcher Icon",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                            ) {
                                                Text(
                                                    text = uiState.appIcon.title,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (appIconsExpanded) "Tap to collapse icon choices" else "Tap to change launcher icon on home screen",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (appIconsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (appIconsExpanded) "Collapse icons" else "Expand icons",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (appIconsExpanded) {
                            Text(
                                text = "Select an icon for your home screen & app drawer. Changes only the app launcher icon on your home screen without altering your app color theme.",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            val iconOptions = AppIcon.values()
                            val iconOptionRows = iconOptions.toList().chunked(2)
                            for (rowIcons in iconOptionRows) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (iconOption in rowIcons) {
                                        val isSelected = uiState.appIcon == iconOption
                                        Surface(
                                            onClick = {
                                                onSelectAppIcon(iconOption)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("app_icon_option_${iconOption.id}"),
                                            shape = RoundedCornerShape(16.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                // Authentic App Icon Preview in squircle
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(
                                                            Brush.linearGradient(
                                                                listOf(iconOption.primaryColor, iconOption.secondaryColor)
                                                            )
                                                        )
                                                        .border(
                                                            1.5.dp,
                                                            if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                                                            RoundedCornerShape(14.dp)
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        painter = painterResource(id = iconOption.previewForegroundRes),
                                                        contentDescription = iconOption.title,
                                                        tint = Color.Unspecified,
                                                        modifier = Modifier.size(42.dp)
                                                    )
                                                }

                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = iconOption.title,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                    Text(
                                                        text = iconOption.subtitle,
                                                        fontSize = 10.sp,
                                                        lineHeight = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Surface(
                                                    shape = RoundedCornerShape(20.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        if (isSelected) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                        }
                                                        Text(
                                                            text = if (isSelected) "ACTIVE" else "SET ICON",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (rowIcons.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

            // --- Google Drive Cloud Sync Card ---
            if (showPairingDialog) {
                AlertDialog(
                    onDismissRequest = { showPairingDialog = false },
                    title = { Text("Pair Devices for Cloud Sync", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Enter the email address or Sync ID associated with your account. Use the EXACT same email/Sync ID on all your devices to automatically sync all your transactions and budget settings across devices.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = pairingEmailInput,
                                onValueChange = { pairingEmailInput = it },
                                label = { Text("Email or Sync ID") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("pairing_email_input")
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (pairingEmailInput.isNotBlank()) {
                                    onConnectCustomSyncEmail(pairingEmailInput.trim())
                                    showPairingDialog = false
                                }
                            },
                            modifier = Modifier.testTag("confirm_pair_device_button")
                        ) {
                            Text("Save & Pair")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showPairingDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

    val cardDriveSync: @Composable () -> Unit = {
        val coroutineScope = rememberCoroutineScope()
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { driveSyncExpanded = !driveSyncExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Cloud Sync Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text("Google Drive Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(
                                    text = if (uiState.driveAccountEmail != null) "Account: ${uiState.driveAccountEmail}" else "Encrypted cross-device cloud sync",
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (uiState.driveAccountEmail != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF22C55E).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Text("Connected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                    }
                                }
                            }
                            IconButton(onClick = { driveSyncExpanded = !driveSyncExpanded }) {
                                Icon(
                                    imageVector = if (driveSyncExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (driveSyncExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }

                    if (driveSyncExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "Sync your budget database seamlessly across all your devices via encrypted cloud sync.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    if (uiState.driveAccountEmail == null) {
                        Button(
                            onClick = {
                                showAuthorizeDriveDialog = true
                                onConnectGoogleDrive()
                            },
                            modifier = Modifier.fillMaxWidth().testTag("connect_google_drive_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Connect Cloud Sync", fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { showPairingDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("pair_device_sync_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text("Pair Second Device (Enter Email / ID)", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Account: ${uiState.driveAccountEmail ?: "Primary Google Account"}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (uiState.lastDriveSyncTime != null) {
                                        Text(
                                            text = "Last synced: ${uiState.lastDriveSyncTime}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Auto-sync background changes", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Switch(
                                    checked = uiState.isDriveAutoSyncEnabled,
                                    onCheckedChange = { onToggleDriveAutoSync(it) }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onManualSyncDrive() },
                                    modifier = Modifier.weight(1f).testTag("manual_drive_sync_button")
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Sync Now", fontSize = 12.sp)
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showAuthorizeDriveDialog = true
                                        onConnectGoogleDrive()
                                    },
                                    modifier = Modifier.weight(1f).testTag("reconnect_google_drive_button")
                                ) {
                                    Text("Authorize Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { onRestoreFinancialData() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                modifier = Modifier.fillMaxWidth().testTag("restore_financial_data_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Restore From Cloud Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

            // --- Google Sheets & Google Drive CSV Sync Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { sheetsSyncExpanded = !sheetsSyncExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "Google Sheets CSV Icon",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(22.dp)
                            )
                            Column {
                                Text("Google Sheets & Drive Export", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Location: My Drive/ PayCycle Budget/", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text("Live Drive Sync", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                }
                            }
                            IconButton(onClick = { sheetsSyncExpanded = !sheetsSyncExpanded }) {
                                Icon(
                                    imageVector = if (sheetsSyncExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (sheetsSyncExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }

                    if (sheetsSyncExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "Export and import directly to & from 'My Drive/ PayCycle Budget/' without saving any files locally on your device. Live sync keeps your Google Sheet updated.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                    Button(
                        onClick = {
                            isExportingToSheets = true
                            onExportToGoogleSheets?.invoke { msg ->
                                csvStatusMsg = msg
                                isExportingToSheets = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth().testTag("export_to_google_sheets_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                if (isExportingToSheets) "Exporting to My Drive/ PayCycle Budget/..." else "Export to My Drive/ PayCycle Budget/",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            showSyncImportPickerDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().testTag("sync_google_sheets_csv_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                "Sync / Pick & Import (Google Drive & Device)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    val context = LocalContext.current
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAuthorizeDriveDialog = true
                                onConnectGoogleDrive()
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("csv_authorize_drive_button")
                        ) {
                            Text("Authorize Drive", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://sheets.new"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("open_sheets_new_button")
                        ) {
                            Text("New Sheet", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val folderId = com.example.sync.GoogleDriveSyncManager(context).getCachedFolderId()
                                    val uriStr = if (!folderId.isNullOrBlank()) {
                                        "https://drive.google.com/drive/folders/$folderId"
                                    } else {
                                        "https://drive.google.com/drive/my-drive"
                                    }
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriStr))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("open_drive_button")
                        ) {
                            Text("PayCycle Folder", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                settingsCoroutineScope.launch {
                                    csvExportData = onExportCsv()
                                    showCsvExportDialog = true
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("export_csv_button")
                        ) {
                            Text("View / Copy CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                csvImportInput = ""
                                csvStatusMsg = ""
                                showCsvImportDialog = true
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("import_csv_button")
                        ) {
                            Text("Import CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    if (csvStatusMsg.isNotEmpty()) {
                        Text(csvStatusMsg, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF047857))
                    }

                    // CSV Export Dialog
                    if (showCsvExportDialog) {
                        val context = LocalContext.current
                        AlertDialog(
                            onDismissRequest = { showCsvExportDialog = false },
                            title = { Text("Google Sheets CSV Data", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Copy this CSV payload to open in Google Sheets or Excel:", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = csvExportData,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(160.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        maxLines = 100
                                    )
                                }
                            },
                            confirmButton = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(android.content.Intent.EXTRA_TEXT, csvExportData)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "expenses_google_sheets_sync.csv")
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share CSV Data"))
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "Could not share CSV", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Share CSV")
                                    }
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("ExpensesCSV", csvExportData)
                                            clipboard.setPrimaryClip(clip)
                                            showCsvExportDialog = false
                                            csvStatusMsg = "CSV copied to clipboard!"
                                        }
                                    ) {
                                        Text("Copy CSV")
                                    }
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCsvExportDialog = false }) {
                                    Text("Close")
                                }
                            }
                        )
                    }

                    // CSV Import Dialog
                    if (showCsvImportDialog) {
                        val clipboardManager = localContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        AlertDialog(
                            onDismissRequest = { showCsvImportDialog = false },
                            title = { Text("Import Google Sheets CSV", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                filePickerMode = "csv"
                                                try {
                                                    filePickerLauncher.launch(arrayOf(
                                                        "application/vnd.google-apps.spreadsheet",
                                                        "text/csv",
                                                        "text/comma-separated-values",
                                                        "text/tab-separated-values",
                                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                        "application/vnd.ms-excel",
                                                        "application/json",
                                                        "*/*"
                                                    ))
                                                } catch (e: Exception) {
                                                    csvStatusMsg = "File picker unavailable on this system. Please paste data below."
                                                }
                                            },
                                            modifier = Modifier.weight(1f).testTag("btn_select_csv_file_dialog")
                                        ) {
                                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("File", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                showCsvImportDialog = false
                                                showSyncImportPickerDialog = true
                                            },
                                            modifier = Modifier.weight(1.2f).testTag("btn_drive_pick_in_csv_dialog")
                                        ) {
                                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF10B981))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Google Drive", fontSize = 10.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val clipData = clipboardManager?.primaryClip
                                                    if (clipData != null && clipData.itemCount > 0) {
                                                        val text = clipData.getItemAt(0)?.text?.toString() ?: ""
                                                        if (text.isNotBlank()) {
                                                            csvImportInput = text
                                                            csvStatusMsg = "Pasted from clipboard!"
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            },
                                            modifier = Modifier.weight(1f).testTag("btn_paste_csv_dialog")
                                        ) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Paste", fontSize = 10.sp)
                                        }
                                    }

                                    Text("Or paste CSV text copied from sheets/file:", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = csvImportInput,
                                        onValueChange = { csvImportInput = it },
                                        placeholder = { Text("Date,Title,Amount,Category,Type,Account,Completed\n2026-07-29,\"Grocery Store\",45.50,\"Groceries\",\"Expense\",\"Checking\",\"Yes\"") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(140.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontSize = 11.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        maxLines = 100
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (csvImportInput.isBlank()) {
                                            csvStatusMsg = "Please paste CSV lines or select a file."
                                            return@Button
                                        }
                                        settingsCoroutineScope.launch {
                                            try {
                                                val (ok, msg) = onImportAnyDataDetailed(csvImportInput)
                                                if (ok) {
                                                    csvStatusMsg = msg
                                                    showCsvImportDialog = false
                                                    Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show()
                                                } else {
                                                    var fallbackOk = onImportCsv(csvImportInput)
                                                    if (!fallbackOk) {
                                                        fallbackOk = onImportBackup(csvImportInput)
                                                    }
                                                    if (fallbackOk) {
                                                        csvStatusMsg = "✅ Data imported & synced with local budget!"
                                                        showCsvImportDialog = false
                                                        Toast.makeText(localContext, "✅ Successfully imported data into budget!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        csvStatusMsg = msg
                                                        Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                csvStatusMsg = "Error: ${e.localizedMessage}"
                                            }
                                        }
                                    }
                                ) {
                                    Text("Import & Sync")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showCsvImportDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    // Sync & Import File Picker Dialog
                    if (showSyncImportPickerDialog) {
                        val clipboardManager = localContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                        LaunchedEffect(showSyncImportPickerDialog, uiState.driveAccountEmail, uiState.driveAuthVersion) {
                            isDriveFilesLoading = true
                            driveFilesErrorMsg = null
                            val (files, err) = onListDriveFiles()
                            driveFileList = files
                            driveFilesErrorMsg = err
                            isDriveFilesLoading = false
                        }

                        AlertDialog(
                            onDismissRequest = {
                                if (importingDriveFileId == null && !isSyncingCsv) {
                                    showSyncImportPickerDialog = false
                                }
                            },
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color(0xFF10B981))
                                    Column {
                                        Text("Sync & Import Files", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                        Text("Select any file to import into your budget", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 500.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 1. Direct Google Sheet Link or ID card (Primary 1-Tap Option)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                    Text("📊 Permanent Google Sheet Link (Never Expires)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        try {
                                                            val clipData = clipboardManager?.primaryClip
                                                            if (clipData != null && clipData.itemCount > 0) {
                                                                val text = clipData.getItemAt(0)?.text?.toString() ?: ""
                                                                if (text.isNotBlank()) {
                                                                    sheetUrlOrIdInput = text.trim()
                                                                }
                                                            }
                                                        } catch (_: Exception) {}
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Paste Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Text(
                                                "Set sharing on your Google Sheet to 'Anyone with link can view'. This link syncs permanently without expiring or needing OAuth tokens.",
                                                fontSize = 10.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedTextField(
                                                    value = sheetUrlOrIdInput,
                                                    onValueChange = { sheetUrlOrIdInput = it },
                                                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...", fontSize = 10.sp) },
                                                    modifier = Modifier.weight(1f).testTag("input_sheet_url_picker"),
                                                    singleLine = true,
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                                )
                                                Button(
                                                    onClick = {
                                                        val input = sheetUrlOrIdInput.trim()
                                                        if (input.isNotEmpty()) {
                                                            isImportingSheetUrl = true
                                                            settingsCoroutineScope.launch {
                                                                try {
                                                                    val (ok, msg) = onImportGoogleSheetByUrlOrId(input)
                                                                    csvStatusMsg = msg
                                                                    Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                                                    if (ok) {
                                                                        showSyncImportPickerDialog = false
                                                                    }
                                                                } catch (e: Exception) {
                                                                    csvStatusMsg = "Import failed: ${e.localizedMessage}"
                                                                    Toast.makeText(localContext, csvStatusMsg, Toast.LENGTH_LONG).show()
                                                                } finally {
                                                                    isImportingSheetUrl = false
                                                                }
                                                            }
                                                        }
                                                    },
                                                    enabled = sheetUrlOrIdInput.isNotBlank() && !isImportingSheetUrl,
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(48.dp).testTag("btn_import_sheet_picker")
                                                ) {
                                                    if (isImportingSheetUrl) {
                                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                                    } else {
                                                        Text("Import Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // 2. Google Drive files section
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("☁️ Google Drive & Cloud Files", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    val emailDisplay = uiState.driveAccountEmail ?: "86brancan@gmail.com"
                                                    Text(emailDisplay, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    OutlinedButton(
                                                        onClick = { showTokenBoxInPicker = !showTokenBoxInPicker },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Token", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    OutlinedButton(
                                                        onClick = onConnectGoogleDrive,
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp)
                                                    ) {
                                                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                                        Spacer(modifier = Modifier.width(3.dp))
                                                        Text("Sign In", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            settingsCoroutineScope.launch {
                                                                isDriveFilesLoading = true
                                                                driveFilesErrorMsg = null
                                                                val (files, err) = onListDriveFiles()
                                                                driveFileList = files
                                                                driveFilesErrorMsg = err
                                                                isDriveFilesLoading = false
                                                            }
                                                        },
                                                        modifier = Modifier.size(28.dp).testTag("refresh_drive_files_button")
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Drive Files", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }

                                            if (showTokenBoxInPicker) {
                                                Card(
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text("🔑 Enter Drive Token or Auth Code:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            OutlinedTextField(
                                                                value = customTokenInputInPicker,
                                                                onValueChange = { customTokenInputInPicker = it },
                                                                placeholder = { Text("Paste ya29... token or 4/... code", fontSize = 10.sp) },
                                                                singleLine = true,
                                                                modifier = Modifier.weight(1f),
                                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                                            )
                                                            Button(
                                                                onClick = {
                                                                    val input = customTokenInputInPicker.trim()
                                                                    if (input.isNotBlank()) {
                                                                        onSaveCustomDriveToken(input)
                                                                        showTokenBoxInPicker = false
                                                                        settingsCoroutineScope.launch {
                                                                            isDriveFilesLoading = true
                                                                            driveFilesErrorMsg = null
                                                                            val (files, err) = onListDriveFiles()
                                                                            driveFileList = files
                                                                            driveFilesErrorMsg = err
                                                                            isDriveFilesLoading = false
                                                                        }
                                                                    }
                                                                },
                                                                enabled = customTokenInputInPicker.isNotBlank(),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(38.dp)
                                                            ) {
                                                                Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            if (!driveFilesErrorMsg.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                                        Text(driveFilesErrorMsg ?: "", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                                        Button(
                                                            onClick = onConnectGoogleDrive,
                                                            shape = RoundedCornerShape(6.dp),
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text("Authorize Drive & Sheets", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }

                                            if (isDriveFilesLoading) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text("Loading files from Google Drive...", fontSize = 11.sp)
                                                }
                                            } else if (driveFileList.isEmpty()) {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text("No files found yet in Drive / Cloud Sync.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text("Save files in 'PayCycle Budget' folder, import a sheet above, or link a permanent Google Sheet.", fontSize = 10.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        modifier = Modifier.padding(top = 4.dp)
                                                    ) {
                                                        Button(
                                                            onClick = onConnectGoogleDrive,
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Sign In with Google", fontSize = 11.sp)
                                                        }

                                                        OutlinedButton(
                                                            onClick = {
                                                                settingsCoroutineScope.launch {
                                                                    isDriveFilesLoading = true
                                                                    driveFilesErrorMsg = null
                                                                    val (files, err) = onListDriveFiles()
                                                                    driveFileList = files
                                                                    driveFilesErrorMsg = err
                                                                    isDriveFilesLoading = false
                                                                }
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                        ) {
                                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("Refresh", fontSize = 11.sp)
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text("Tap 'Import' on any spreadsheet or file to load into your budget (${driveFileList.size} files found):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                OutlinedTextField(
                                                    value = driveFileSearchQuery,
                                                    onValueChange = { driveFileSearchQuery = it },
                                                    placeholder = { Text("Search ${driveFileList.size} Drive files...", fontSize = 11.sp) },
                                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                                    trailingIcon = {
                                                        if (driveFileSearchQuery.isNotEmpty()) {
                                                            IconButton(onClick = { driveFileSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                                            }
                                                        }
                                                    },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    textStyle = LocalTextStyle.current.copy(fontSize = 11.sp)
                                                )
                                                val filteredDriveFiles = remember(driveFileList, driveFileSearchQuery) {
                                                    if (driveFileSearchQuery.isBlank()) driveFileList
                                                    else driveFileList.filter { it.name.contains(driveFileSearchQuery.trim(), ignoreCase = true) }
                                                }
                                                if (filteredDriveFiles.isEmpty()) {
                                                    Text("No files matching \"$driveFileSearchQuery\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                                                }
                                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    filteredDriveFiles.forEach { file ->
                                                        val isCurrentImporting = importingDriveFileId == file.id
                                                        Surface(
                                                            shape = RoundedCornerShape(8.dp),
                                                            color = MaterialTheme.colorScheme.surface,
                                                            tonalElevation = 2.dp,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable(enabled = importingDriveFileId == null && !isSyncingCsv) {
                                                                    importingDriveFileId = file.id
                                                                    settingsCoroutineScope.launch {
                                                                        try {
                                                                            val (ok, msg) = onImportSpecificDriveFile(file)
                                                                            csvStatusMsg = msg
                                                                            Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                                                            if (ok) {
                                                                                showSyncImportPickerDialog = false
                                                                            }
                                                                        } catch (e: Exception) {
                                                                            csvStatusMsg = "Import failed: ${e.localizedMessage}"
                                                                            Toast.makeText(localContext, csvStatusMsg, Toast.LENGTH_LONG).show()
                                                                        } finally {
                                                                            importingDriveFileId = null
                                                                        }
                                                                    }
                                                                }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                val iconColor = when {
                                                                    file.isGoogleSheet -> Color(0xFF0F9D58)
                                                                    file.isCsv -> Color(0xFF1E88E5)
                                                                    file.isJson -> Color(0xFFF59E0B)
                                                                    else -> Color(0xFF6B7280)
                                                                }
                                                                Icon(
                                                                    when {
                                                                        file.isGoogleSheet -> Icons.Default.TableChart
                                                                        file.isJson -> Icons.Default.Code
                                                                        else -> Icons.Default.Description
                                                                    },
                                                                    contentDescription = null,
                                                                    tint = iconColor,
                                                                    modifier = Modifier.size(22.dp)
                                                                )
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                        Text(
                                                                            if (file.location == "PayCycle Budget") "📁 PayCycle Folder" else "☁️ My Drive",
                                                                            fontSize = 9.sp,
                                                                            color = if (file.location == "PayCycle Budget") Color(0xFF059669) else Color(0xFF4B5563),
                                                                            fontWeight = FontWeight.Medium
                                                                        )
                                                                        if (file.sizeString.isNotBlank()) {
                                                                            Text("• ${file.sizeString}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                        }
                                                                    }
                                                                    if (file.formattedDate.isNotBlank()) {
                                                                        Text(file.formattedDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                    }
                                                                }

                                                                Button(
                                                                    onClick = {
                                                                        importingDriveFileId = file.id
                                                                        settingsCoroutineScope.launch {
                                                                            try {
                                                                                val (ok, msg) = onImportSpecificDriveFile(file)
                                                                                csvStatusMsg = msg
                                                                                Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                                                                if (ok) {
                                                                                    showSyncImportPickerDialog = false
                                                                                }
                                                                            } catch (e: Exception) {
                                                                                csvStatusMsg = "Import error: ${e.localizedMessage}"
                                                                                Toast.makeText(localContext, csvStatusMsg, Toast.LENGTH_LONG).show()
                                                                            } finally {
                                                                                importingDriveFileId = null
                                                                            }
                                                                        }
                                                                    },
                                                                    enabled = importingDriveFileId == null && !isSyncingCsv,
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                                    modifier = Modifier.testTag("btn_import_drive_file_${file.id}")
                                                                ) {
                                                                    if (isCurrentImporting) {
                                                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                                                    } else {
                                                                        Text("Import", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Sync all folder files button
                                            OutlinedButton(
                                                onClick = {
                                                    isSyncingCsv = true
                                                    settingsCoroutineScope.launch {
                                                        try {
                                                            csvExportData = onExportCsv()
                                                            onSyncCsvDrive?.invoke { msg ->
                                                                csvStatusMsg = msg
                                                                isSyncingCsv = false
                                                                Toast.makeText(localContext, msg, Toast.LENGTH_LONG).show()
                                                                if (msg.contains("Successfully", ignoreCase = true)) {
                                                                    showSyncImportPickerDialog = false
                                                                }
                                                            }
                                                        } catch (e: Exception) {
                                                            isSyncingCsv = false
                                                            csvStatusMsg = "Sync error: ${e.localizedMessage}"
                                                            Toast.makeText(localContext, csvStatusMsg, Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                },
                                                enabled = !isSyncingCsv && importingDriveFileId == null,
                                                modifier = Modifier.fillMaxWidth().testTag("sync_all_drive_files_in_picker_button")
                                            ) {
                                                if (isSyncingCsv) {
                                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Syncing all PayCycle files...", fontSize = 11.sp)
                                                } else {
                                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Sync All PayCycle Budget Files", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }

                                    // 3. Pick from Device Storage section
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("📁 Pick from Device / File Storage", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text("Select any CSV, Spreadsheet, JSON, or Excel file:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        filePickerMode = "csv"
                                                        try {
                                                            filePickerLauncher.launch(arrayOf(
                                                                "application/vnd.google-apps.spreadsheet",
                                                                "text/csv",
                                                                "text/comma-separated-values",
                                                                "text/tab-separated-values",
                                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                                "application/vnd.ms-excel",
                                                                "application/json",
                                                                "*/*"
                                                            ))
                                                        } catch (e: Exception) {
                                                            Toast.makeText(localContext, "File picker unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                                    modifier = Modifier.weight(1f).testTag("dialog_pick_device_file_button")
                                                ) {
                                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Pick File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        showSyncImportPickerDialog = false
                                                        showCsvImportDialog = true
                                                    },
                                                    modifier = Modifier.weight(1f).testTag("dialog_paste_text_button")
                                                ) {
                                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(15.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Paste Data", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {},
                            dismissButton = {
                                TextButton(
                                    onClick = { showSyncImportPickerDialog = false },
                                    enabled = importingDriveFileId == null && !isSyncingCsv
                                ) {
                                    Text("Close")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    val cardWebCompanion: @Composable () -> Unit = {
        // --- Card: PC & Desktop Web App Companion ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            val context = LocalContext.current
            var toastMsg by remember { mutableStateOf("") }
            val coroutineScope = rememberCoroutineScope()

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { webCompanionExpanded = !webCompanionExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("💻", fontSize = 18.sp, modifier = Modifier.padding(8.dp))
                        }
                        Column {
                            Text("PC & Desktop Web Companion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Browser access & real-time sync channel", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    IconButton(onClick = { webCompanionExpanded = !webCompanionExpanded }) {
                        Icon(
                            imageVector = if (webCompanionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (webCompanionExpanded) "Collapse" else "Expand"
                        )
                    }
                }

                if (webCompanionExpanded) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Manage your PayCycle budget, transactions, and debt snowball payoff from any PC or laptop web browser with real-time cloud sync.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Cloud Sync Channel ID:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val rawEmail = uiState.driveAccountEmail ?: "86brancan@gmail.com"
                            val sanitizedKey = rawEmail.lowercase().replace(Regex("[@.#$\\[\\]]"), "_").replace(Regex("[^a-z0-9_]"), "")
                            Text(
                                text = "paycycle_sync_$sanitizedKey",
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val currentJson = onExportBackup()
                                        val assetManager = context.assets
                                        val inputStream = assetManager.open("web_companion.html")
                                        var htmlContent = inputStream.bufferedReader().use { it.readText() }
                                        
                                        // Pre-embed user's current live budget data directly into the exported HTML
                                        if (currentJson.isNotBlank()) {
                                            val cleanJson = currentJson.trim()
                                            val replacementBlock = "window.EMBEDDED_DATA = $cleanJson;\ntry { localStorage.setItem('paycycle_budget_db', JSON.stringify(window.EMBEDDED_DATA)); } catch(e) {}"
                                            htmlContent = htmlContent.replace("window.EMBEDDED_DATA = null;", replacementBlock)
                                        }
                                        
                                        // Overwrite previous "PayCycle Budget - Web Companion App" on Google Drive if connected
                                        if (onExportWebCompanionToGoogleDrive != null) {
                                            val (driveSuccess, driveStatus) = onExportWebCompanionToGoogleDrive(htmlContent)
                                            if (driveSuccess) {
                                                toastMsg = driveStatus
                                            }
                                        }

                                        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                            type = "text/html"
                                            putExtra(android.content.Intent.EXTRA_TEXT, htmlContent)
                                            putExtra(android.content.Intent.EXTRA_TITLE, "PayCycle Budget - Web Companion App")
                                            putExtra(android.content.Intent.EXTRA_SUBJECT, "PayCycle Budget - Web Companion App")
                                        }
                                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share 'PayCycle Budget - Web Companion App'"))
                                    } catch (e: Exception) {
                                        toastMsg = "Could not export web app: ${e.localizedMessage}"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f).testTag("btn_share_pc_web_app")
                        ) {
                            Text("Export / Share Web App", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                val rawEmail = uiState.driveAccountEmail ?: "86brancan@gmail.com"
                                val sanitizedKey = rawEmail.lowercase().replace(Regex("[@.#$\\[\\]]"), "_").replace(Regex("[^a-z0-9_]"), "")
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("PayCycleSyncKey", "paycycle_sync_$sanitizedKey")
                                clipboard.setPrimaryClip(clip)
                                toastMsg = "Sync Channel copied to clipboard!"
                            },
                            modifier = Modifier.weight(1f).testTag("btn_copy_sync_code")
                        ) {
                            Text("Copy Sync Code", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                        }
                    }

                    if (toastMsg.isNotEmpty()) {
                        Text(
                            text = toastMsg,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    val cardBackup: @Composable () -> Unit = {
        // --- Card 5: Data Portability & Backup ---
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                var exportedJson by remember { mutableStateOf("") }
                var showExportDialog by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupExpanded = !backupExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Data Backup Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Column {
                                Text("Data Portability & Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Export, import & restore device storage", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { backupExpanded = !backupExpanded }) {
                            Icon(
                                imageVector = if (backupExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (backupExpanded) "Collapse" else "Expand"
                            )
                        }
                    }

                    if (backupExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Text(
                            text = "Export your complete financial records or restore them anytime.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        exportedJson = onExportBackup()
                                        showExportDialog = true
                                    }
                                },
                                modifier = Modifier.weight(1f).testTag("export_backup_button")
                            ) {
                                Text("Export Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    importInputText = ""
                                    importErrorMsg = ""
                                    showImportDialog = true
                                },
                                modifier = Modifier.weight(1f).testTag("import_backup_button")
                            ) {
                                Text("Restore Backup", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerMode = "backup"
                                try {
                                    filePickerLauncher.launch(arrayOf(
                                        "application/json",
                                        "text/csv",
                                        "text/comma-separated-values",
                                        "application/vnd.google-apps.spreadsheet",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "*/*"
                                    ))
                                } catch (e: Exception) {
                                    Toast.makeText(localContext, "File picker unavailable. Tap 'Restore Backup' and paste your data directly.", Toast.LENGTH_LONG).show()
                                    showImportDialog = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("select_backup_file_button")
                        ) {
                            Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Select Backup File from Storage (.json / .csv)", fontSize = 12.sp)
                        }

                        // Export Dialog
                        if (showExportDialog) {
                            val context = LocalContext.current
                            AlertDialog(
                                onDismissRequest = { showExportDialog = false },
                                title = { Text("Backup String Ready", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Copy the backup payload below or save it in a safe location:", fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = exportedJson,
                                            onValueChange = {},
                                            readOnly = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            ),
                                            maxLines = 100
                                        )
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("BudgetBackup", exportedJson)
                                            clipboard.setPrimaryClip(clip)
                                            showExportDialog = false
                                        }
                                    ) {
                                        Text("Copy to Clipboard")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showExportDialog = false }) {
                                        Text("Close")
                                    }
                                }
                            )
                        }

                        // Import Dialog
                        if (showImportDialog) {
                            val clipboardManager = localContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            AlertDialog(
                                onDismissRequest = { if (!isImportingProgress) showImportDialog = false },
                                title = { Text("Restore From Backup", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    filePickerMode = "backup"
                                                    try {
                                                        filePickerLauncher.launch(arrayOf(
                                                            "application/json",
                                                            "text/csv",
                                                            "text/comma-separated-values",
                                                            "application/vnd.google-apps.spreadsheet",
                                                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                            "*/*"
                                                        ))
                                                    } catch (e: Exception) {
                                                        importErrorMsg = "System file picker is unavailable. Please paste your data directly below."
                                                    }
                                                },
                                                modifier = Modifier.weight(1f).testTag("btn_select_backup_file_dialog")
                                            ) {
                                                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Select File", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    try {
                                                        val clipData = clipboardManager?.primaryClip
                                                        if (clipData != null && clipData.itemCount > 0) {
                                                            val text = clipData.getItemAt(0)?.text?.toString() ?: ""
                                                            if (text.isNotBlank()) {
                                                                importInputText = text
                                                                importErrorMsg = ""
                                                            }
                                                        }
                                                    } catch (_: Exception) {}
                                                },
                                                modifier = Modifier.weight(1f).testTag("btn_paste_backup_dialog")
                                            ) {
                                                Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Paste Clipboard", fontSize = 11.sp)
                                            }
                                        }

                                        Text("Or paste your backup JSON/CSV text below:", fontSize = 12.sp)
                                        OutlinedTextField(
                                            value = importInputText,
                                            onValueChange = {
                                                importInputText = it
                                                importErrorMsg = ""
                                            },
                                            placeholder = { Text("{ \"transactions\": [ ... ] }") },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(140.dp),
                                            textStyle = androidx.compose.ui.text.TextStyle(
                                                fontSize = 11.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            ),
                                            maxLines = 100
                                        )
                                        if (importErrorMsg.isNotEmpty()) {
                                            Text(
                                                text = importErrorMsg,
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (importInputText.isBlank()) {
                                                importErrorMsg = "❌ Please paste backup data or select a file."
                                                return@Button
                                            }
                                            isImportingProgress = true
                                            coroutineScope.launch {
                                                try {
                                                    val (ok, msg) = onImportAnyDataDetailed(importInputText)
                                                    isImportingProgress = false
                                                    if (ok) {
                                                        Toast.makeText(localContext, msg, Toast.LENGTH_SHORT).show()
                                                        showImportDialog = false
                                                    } else {
                                                        var success = onImportBackup(importInputText)
                                                        if (!success) {
                                                            success = onImportCsv(importInputText)
                                                        }
                                                        if (success) {
                                                            Toast.makeText(localContext, "✅ Data restored successfully!", Toast.LENGTH_SHORT).show()
                                                            showImportDialog = false
                                                        } else {
                                                            importErrorMsg = "❌ Failed to restore data: $msg"
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    isImportingProgress = false
                                                    importErrorMsg = "❌ Error: ${e.localizedMessage}"
                                                }
                                            }
                                        },
                                        enabled = !isImportingProgress
                                    ) {
                                        if (isImportingProgress) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Restore Data")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showImportDialog = false },
                                        enabled = !isImportingProgress
                                    ) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

    val cardDangerZone: @Composable () -> Unit = {
        // --- Card 5: Danger Zone / Clear Database ---
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dangerZoneExpanded = !dangerZoneExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(8.dp).size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    "Reset & Danger Zone",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text("Clear all stored local database entries", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { dangerZoneExpanded = !dangerZoneExpanded }) {
                            Icon(
                                imageVector = if (dangerZoneExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (dangerZoneExpanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    if (dangerZoneExpanded) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f))

                        Text(
                            "Clear all stored local database entries including transactions, accounts, income sources, and scheduled bills. Make sure to export a backup beforehand if needed.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedButton(
                            onClick = { showClearDataConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("clear_database_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Clear Database", fontWeight = FontWeight.Bold)
                        }
                    }
                }
        }
    }

    val anySettingsExpanded = payScheduleExpanded || remindersExpanded || themeCardExpanded || webCompanionExpanded || driveSyncExpanded || sheetsSyncExpanded || backupExpanded || dangerZoneExpanded
    val setAllSettingsExpanded: (Boolean) -> Unit = { target ->
        payScheduleExpanded = target
        remindersExpanded = target
        themeCardExpanded = target
        webCompanionExpanded = target
        driveSyncExpanded = target
        sheetsSyncExpanded = target
        backupExpanded = target
        dangerZoneExpanded = target
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Master Collapse All / Expand All header bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings & Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { setAllSettingsExpanded(!anySettingsExpanded) }
                    .testTag("settings_master_expand_collapse_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (anySettingsExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (anySettingsExpanded) "Collapse All" else "Expand All",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (anySettingsExpanded) "Collapse All" else "Expand All",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val useTwoColumns = isWideScreen && maxWidth >= 680.dp
            if (useTwoColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        cardPaySchedule()
                        cardRemindersAndThemes()
                        cardWebCompanion()
                        cardDangerZone()
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        cardDriveSync()
                        cardBackup()
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    cardPaySchedule()
                    cardRemindersAndThemes()
                    cardWebCompanion()
                    cardDriveSync()
                    cardBackup()
                    cardDangerZone()
                }
            }
        }
    }
}

@Composable
fun StatusSelectorSegmentedRow(
    status: String,
    onStatusSelected: (String) -> Unit,
    isIncome: Boolean = false,
    prefixTag: String = "dialog_status",
    modifier: Modifier = Modifier
) {
    val dark = isSystemInDarkTheme()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val options = listOf(
            Triple("due", "Due", "⏳"),
            Triple("pending", "Pending", "🕒"),
            Triple("paid", if (isIncome) "Received" else "Paid", "💳"),
            Triple("cleared", "Cleared", "🛡️")
        )
        options.forEach { (key, label, icon) ->
            val selected = status.lowercase().trim() == key
            val selectedBg = when (key) {
                "cleared" -> if (dark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFD1FAE5)
                "paid" -> if (dark) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFDBEAFE)
                "pending" -> if (dark) Color(0xFF78350F).copy(alpha = 0.6f) else Color(0xFFFEF3C7)
                else -> if (dark) Color(0xFF334155).copy(alpha = 0.8f) else Color(0xFFE2E8F0)
            }
            val selectedBorder = when (key) {
                "cleared" -> Color(0xFF10B981)
                "paid" -> Color(0xFF3B82F6)
                "pending" -> Color(0xFFF59E0B)
                else -> Color(0xFF94A3B8)
            }
            val selectedTextColor = when (key) {
                "cleared" -> if (dark) Color(0xFF6EE7B7) else Color(0xFF047857)
                "paid" -> if (dark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                "pending" -> if (dark) Color(0xFFFCD34D) else Color(0xFFB45309)
                else -> if (dark) Color(0xFFCBD5E1) else Color(0xFF334155)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (selected) Modifier
                            .background(selectedBg)
                            .border(1.dp, selectedBorder, RoundedCornerShape(6.dp))
                        else Modifier.background(Color.Transparent)
                    )
                    .clickable { onStatusSelected(key) }
                    .padding(vertical = 6.dp)
                    .testTag("${prefixTag}_$key"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(icon, fontSize = 11.sp)
                    Text(
                        text = label,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (selected) selectedTextColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddTransactionDialog(
    preselectedDate: LocalDate,
    selectedMonth: YearMonth,
    allAccounts: List<String>,
    initialAccountName: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, amount: Double, date: LocalDate, category: String, isIncome: Boolean, isRecurring: Boolean, accountName: String, isAutoPay: Boolean, reminderDaysBefore: Int, isCompleted: Boolean, status: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(preselectedDate.toString()) }
    var selectedCategory by remember { mutableStateOf("Core Bills") }
    var isIncome by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var recurFrequency by remember { mutableStateOf("Bi-Weekly") }
    var isAutoPay by remember { mutableStateOf(false) }
    var txStatus by remember { mutableStateOf("due") }
    val isCompleted = txStatus in listOf("paid", "cleared")
    var selectedAccount by remember { mutableStateOf(if (initialAccountName == "All Accounts" || initialAccountName.isBlank() || initialAccountName.equals("Primary Account", ignoreCase = true) || initialAccountName.equals("Primary", ignoreCase = true)) (allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else initialAccountName) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableIntStateOf(0) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }
    var isAdvancedExpanded by rememberSaveable { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIncome) "Add Income Source" else "Schedule Bill / Expense") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Segmented Toggle for Income/Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isIncome) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isIncome = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Expense", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIncome) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isIncome = true; selectedCategory = "Income" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Income", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Status Segmented Selector: due, pending, paid, cleared
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusSelectorSegmentedRow(
                        status = txStatus,
                        onStatusSelected = { txStatus = it },
                        isIncome = isIncome,
                        prefixTag = "add_dialog_status"
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

                // Category Picker
                if (isIncome) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val catList = listOf("Paycheck", "Side Gig", "Bonus", "Refund", "Income")
                            val icons = listOf("💼", "🚀", "🎁", "🔄", "💵")
                            catList.forEachIndexed { idx, cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${icons[idx]} $cat", fontSize = 9.5.sp) },
                                    modifier = Modifier.height(24.dp).testTag("tx_category_chip_${cat.lowercase()}")
                                )
                            }
                        }
                    }
                } else {
                    ExpenseCategorySubCategoryPicker(
                        selectedCategory = if (selectedCategory in listOf("Paycheck", "Income", "Side Gig", "Bonus", "Refund")) "Core Bills" else selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DatePickerTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    testTag = "tx_date_input",
                    modifier = Modifier.fillMaxWidth()
                )

                // More Details & Rules Collapsible Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAdvancedExpanded = !isAdvancedExpanded }
                        .testTag("tx_advanced_toggle")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (isAdvancedExpanded) "Hide Options (Account, Reminders, Rules)" else "More Options (Account, Reminders, Rules)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(visible = isAdvancedExpanded) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Target Account selector dropdown
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("Target Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                    .clickable { accountDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("dialog_account_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedAccount, fontSize = 14.sp)
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Dropdown icon"
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            ) {
                                allAccounts.filter { it != "All Accounts" }.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc, fontSize = 14.sp) },
                                        onClick = {
                                            selectedAccount = acc
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        if (!isIncome) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("🔔 Notification Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable { reminderDropdownExpanded = true }
                                        .padding(horizontal = 12.dp, vertical = 12.dp)
                                        .testTag("tx_reminder_dropdown_trigger")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val label = when (reminderDaysBefore) {
                                            0 -> "On Due Date (Day of at 9:00 AM)"
                                            1 -> "1 Day Before Due Date"
                                            3 -> "3 Days Before Due Date"
                                            7 -> "1 Week Before Due Date"
                                            else -> "No Notification Reminder"
                                        }
                                        Text(label, fontSize = 14.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown icon")
                                    }
                                }
                                DropdownMenu(
                                    expanded = reminderDropdownExpanded,
                                    onDismissRequest = { reminderDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    listOf(
                                        0 to "On Due Date (Day of at 9:00 AM)",
                                        1 to "1 Day Before Due Date",
                                        3 to "3 Days Before Due Date",
                                        7 to "1 Week Before Due Date",
                                        -1 to "No Reminder"
                                    ).forEach { (value, label) ->
                                        DropdownMenuItem(
                                            text = { Text(label, fontSize = 14.sp) },
                                            onClick = {
                                                reminderDaysBefore = value
                                                reminderDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Recurring Checkbox Toggle Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isRecurring = !isRecurring }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isRecurring,
                                    onCheckedChange = { isRecurring = it },
                                    modifier = Modifier.testTag("tx_recurring_checkbox")
                                )
                            }
                            Column {
                                Text(
                                    text = if (isRecurring) "Set as recurring baseline (Reoccurs every cycle)" else "Single transaction (This cycle only)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isRecurring) "Baseline repeats across cycles. Each cycle's items remain independent and can be individually edited." else "Applies only to this specific cycle without affecting other cycles.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isRecurring && isIncome) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "Recurrence Frequency:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("Bi-Weekly", "Weekly", "Monthly", "Semi-Monthly").forEach { f ->
                                        FilterChip(
                                            selected = recurFrequency == f,
                                            onClick = { recurFrequency = f },
                                            label = { Text(f, fontSize = 9.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Auto Pay Checkbox Toggle Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isAutoPay = !isAutoPay }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isAutoPay,
                                    onCheckedChange = { isAutoPay = it },
                                    modifier = Modifier.testTag("tx_autopay_checkbox")
                                )
                            }
                            Text(
                                text = "⚡ Auto Pay enabled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isAutoPay) {
                            EditableAutoPayTag(
                                accountName = selectedAccount,
                                isAutoPay = isAutoPay,
                                allAccounts = allAccounts,
                                onUpdateAutoPaySource = { newAccount, enabled ->
                                    selectedAccount = newAccount
                                    isAutoPay = enabled
                                }
                            )
                        }
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val dateParsed = try {
                        LocalDate.parse(dateInput)
                    } catch (e: Exception) {
                        null
                    }

                    if (title.isBlank()) {
                        errorMsg = "Please enter a title."
                    } else if (amt == null || amt <= 0.0) {
                        errorMsg = "Please enter a valid amount."
                    } else if (dateParsed == null) {
                        errorMsg = "Please enter a valid date (YYYY-MM-DD)."
                    } else {
                        val finalCategory = if (isIncome) {
                            if (isRecurring && recurFrequency != "Bi-Weekly") "Income ($recurFrequency)" else "Income"
                        } else {
                            selectedCategory
                        }
                        onConfirm(title, amt, dateParsed, finalCategory, isIncome, isRecurring, selectedAccount, isAutoPay, if (isIncome) -1 else reminderDaysBefore, isCompleted, txStatus)
                    }
                },
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditTransactionDialog(
    transaction: TransactionEntity,
    allAccounts: List<String>,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity, Boolean) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(if (transaction.amount > 0) String.format(Locale.US, "%.2f", transaction.amount) else "") }
    val initialDate = remember(transaction.dateMillis) { DateUtils.millisToLocalDate(transaction.dateMillis) }
    var dateInput by remember { mutableStateOf(initialDate.toString()) }
    var applyToBaseline by remember { mutableStateOf(false) }
    var selectedCategory by remember {
        mutableStateOf(
            if (transaction.isIncome) {
                transaction.category.ifEmpty { "Income" }
            } else {
                CategoryTheme.parseCategory(transaction.category).first
            }
        )
    }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    var isRecurring by remember { mutableStateOf(transaction.isRecurring) }
    var recurFrequency by remember {
        mutableStateOf(
            when {
                transaction.category.contains("Weekly", ignoreCase = true) && !transaction.category.contains("Bi-Weekly", ignoreCase = true) -> "Weekly"
                transaction.category.contains("Monthly", ignoreCase = true) && !transaction.category.contains("Semi-Monthly", ignoreCase = true) -> "Monthly"
                transaction.category.contains("Semi-Monthly", ignoreCase = true) -> "Semi-Monthly"
                else -> "Bi-Weekly"
            }
        )
    }
    var isAutoPay by remember { mutableStateOf(transaction.isAutoPay) }
    var txStatus by remember { mutableStateOf(transaction.getEffectiveStatus()) }
    val isCompleted = txStatus in listOf("paid", "cleared")
    var selectedAccount by remember { mutableStateOf(if (transaction.accountName.isEmpty() || transaction.accountName == "All Accounts" || transaction.accountName.equals("Primary Account", ignoreCase = true) || transaction.accountName.equals("Primary", ignoreCase = true)) (allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "") else transaction.accountName) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableIntStateOf(transaction.reminderDaysBefore) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (isIncome) "Edit Income" else "Edit Expense")
                IconButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier.testTag("delete_transaction_icon")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Transaction",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Segmented Toggle for Income/Expense
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isIncome) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isIncome = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Expense", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isIncome) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isIncome = true; selectedCategory = "Income" }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Income", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // Status Segmented Selector: due, pending, paid, cleared
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusSelectorSegmentedRow(
                        status = txStatus,
                        onStatusSelected = { txStatus = it },
                        isIncome = isIncome,
                        prefixTag = "edit_dialog_status"
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_title_input")
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_tx_amount_input")
                )

                // Category Picker
                if (isIncome) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedCategory,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val catList = listOf("Paycheck", "Side Gig", "Bonus", "Refund", "Income")
                            val icons = listOf("💼", "🚀", "🎁", "🔄", "💵")
                            catList.forEachIndexed { idx, cat ->
                                val isSelected = selectedCategory == cat
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCategory = cat },
                                    label = { Text("${icons[idx]} $cat", fontSize = 9.5.sp) },
                                    modifier = Modifier.height(24.dp).testTag("edit_tx_category_chip_${cat.lowercase()}")
                                )
                            }
                        }
                    }
                } else {
                    ExpenseCategorySubCategoryPicker(
                        selectedCategory = if (selectedCategory in listOf("Paycheck", "Income", "Side Gig", "Bonus", "Refund")) "Core Bills" else selectedCategory,
                        onCategorySelected = { selectedCategory = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                DatePickerTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    testTag = "edit_tx_date_input",
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Account selector dropdown
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Target Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            .clickable { accountDropdownExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                            .testTag("edit_dialog_account_dropdown_trigger")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedAccount, fontSize = 14.sp)
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown icon"
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = accountDropdownExpanded,
                        onDismissRequest = { accountDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        allAccounts.filter { it != "All Accounts" }.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc, fontSize = 14.sp) },
                                onClick = {
                                    selectedAccount = acc
                                    accountDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                if (!isIncome) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("🔔 Notification Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { reminderDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                                .testTag("edit_tx_reminder_dropdown_trigger")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val label = when (reminderDaysBefore) {
                                    0 -> "On Due Date (Day of at 9:00 AM)"
                                    1 -> "1 Day Before Due Date"
                                    3 -> "3 Days Before Due Date"
                                    7 -> "1 Week Before Due Date"
                                    else -> "No Notification Reminder"
                                }
                                Text(label, fontSize = 14.sp)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown icon")
                            }
                        }
                        DropdownMenu(
                            expanded = reminderDropdownExpanded,
                            onDismissRequest = { reminderDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.7f)
                        ) {
                            listOf(
                                0 to "On Due Date (Day of at 9:00 AM)",
                                1 to "1 Day Before Due Date",
                                3 to "3 Days Before Due Date",
                                7 to "1 Week Before Due Date",
                                -1 to "No Reminder"
                            ).forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, fontSize = 14.sp) },
                                    onClick = {
                                        reminderDaysBefore = value
                                        reminderDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Recurring Checkbox Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isRecurring = !isRecurring }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it },
                            modifier = Modifier.testTag("edit_tx_recurring_checkbox")
                        )
                    }
                    Text(
                        text = if (isIncome) "Recur automatically" else "Recur monthly on this day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isRecurring && isIncome) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Recurrence Frequency:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("Bi-Weekly", "Weekly", "Monthly", "Semi-Monthly").forEach { f ->
                                FilterChip(
                                    selected = recurFrequency == f,
                                    onClick = { recurFrequency = f },
                                    label = { Text(f, fontSize = 9.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                    }
                }

                // Auto Pay Checkbox Toggle Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isAutoPay = !isAutoPay }
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Checkbox(
                            checked = isAutoPay,
                            onCheckedChange = { isAutoPay = it },
                            modifier = Modifier.testTag("edit_tx_autopay_checkbox")
                        )
                    }
                    Text(
                        text = "⚡ Auto Pay enabled",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (isAutoPay) {
                    EditableAutoPayTag(
                        accountName = selectedAccount,
                        isAutoPay = isAutoPay,
                        allAccounts = allAccounts,
                        onUpdateAutoPaySource = { newAccount, enabled ->
                            selectedAccount = newAccount
                            isAutoPay = enabled
                        }
                    )
                }

                // Recurring Baseline Independence Scope Selector
                if (transaction.isRecurring || transaction.id < 0 || transaction.title.contains("(Planned)") || isRecurring) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().testTag("edit_tx_scope_selector")
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Apply Changes To",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip(
                                    selected = !applyToBaseline,
                                    onClick = { applyToBaseline = false },
                                    label = { Text("This Cycle Only", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f).height(24.dp).testTag("edit_scope_cycle_only")
                                )
                                FilterChip(
                                    selected = applyToBaseline,
                                    onClick = { applyToBaseline = true },
                                    label = { Text("Update Baseline", fontSize = 9.5.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f).height(24.dp).testTag("edit_scope_baseline")
                                )
                            }
                            Text(
                                text = if (!applyToBaseline) "⚡ This cycle's income/expenses remain independent from other cycles." else "🔄 Updates the recurring baseline so future cycles will use these values.",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull()
                    val dateParsed = try {
                        LocalDate.parse(dateInput)
                    } catch (e: Exception) {
                        null
                    }

                    if (title.isBlank()) {
                        errorMsg = "Please enter a title."
                    } else if (amt == null || amt <= 0.0) {
                        errorMsg = "Please enter a valid amount."
                    } else if (dateParsed == null) {
                        errorMsg = "Please enter a valid date (YYYY-MM-DD)."
                    } else {
                        val finalCat = if (isIncome) {
                            if (isRecurring && recurFrequency != "Bi-Weekly") "Income ($recurFrequency)" else "Income"
                        } else {
                            selectedCategory
                        }
                        val updatedTx = transaction.copy(
                            title = title,
                            amount = amt,
                            dateMillis = DateUtils.localDateToMillis(dateParsed),
                            category = finalCat,
                            isIncome = isIncome,
                            isRecurring = isRecurring,
                            isAutoPay = isAutoPay,
                            accountName = selectedAccount,
                            reminderDaysBefore = if (isIncome) -1 else reminderDaysBefore,
                            isCompleted = isCompleted,
                            status = txStatus
                        )
                        onSave(updatedTx, applyToBaseline)
                    }
                },
                modifier = Modifier.testTag("save_edited_transaction_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun UpcomingExpensesWidget(
    uiState: BudgetUiState,
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onOpenAddBill: () -> Unit = {},
    onDeleteExpense: (UpcomingExpenseItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<UpcomingExpenseItem?>(null) }
    val today = remember { LocalDate.now() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }

    val upcomingItems = remember(
        uiState.allTransactions,
        uiState.plannedExpenses,
        uiState.allDebts,
        uiState.activeAccountName,
        uiState.currentPayPeriodStart,
        uiState.currentPayPeriodEnd
    ) {
        com.example.data.UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = uiState.allTransactions,
            plannedExpenses = uiState.plannedExpenses,
            activeAccountName = uiState.activeAccountName,
            today = today,
            payPeriodStart = uiState.currentPayPeriodStart,
            payPeriodEnd = uiState.currentPayPeriodEnd,
            allDebts = uiState.allDebts
        )
    }

    val totalUpcomingAmount = upcomingItems.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 2.dp)
            .testTag("upcoming_expenses_widget"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Widget Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Upcoming Expenses",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Upcoming Expenses",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (upcomingItems.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${upcomingItems.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "Total due: $%,.2f", totalUpcomingAmount),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onOpenAddBill,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Bill",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                if (upcomingItems.isEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                        Text(
                            text = "No pending upcoming expenses! You're all caught up.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        upcomingItems.take(10).forEach { item ->
                            val formattedDueDate = item.dueDate.format(dateFmt)
                            val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, item.dueDate)
                            val (dateLabel, dateBgColor, dateTextColor) = when {
                                daysBetween < 0 -> Triple("Overdue ($formattedDueDate)", Color(0xFFFEE2E2), Color(0xFFDC2626))
                                daysBetween == 0L -> Triple("Due Today", Color(0xFFFEE2E2), Color(0xFFDC2626))
                                daysBetween == 1L -> Triple("Due Tomorrow", Color(0xFFFEF3C7), Color(0xFFD97706))
                                daysBetween in 2..5 -> Triple("In $daysBetween days", Color(0xFFFEF3C7), Color(0xFFB45309))
                                else -> Triple("Due $formattedDueDate", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = dateBgColor
                                    ) {
                                        Text(
                                            text = dateLabel,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = dateTextColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (item.isAutoPay) {
                                                EditableAutoPayTag(
                                                    accountName = item.accountName,
                                                    isAutoPay = item.isAutoPay,
                                                    allAccounts = uiState.allAccounts,
                                                    onUpdateAutoPaySource = { newAccount, enabled ->
                                                        // Update transaction if item comes from transaction
                                                    }
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Due $formattedDueDate • ${item.category} • ${item.accountName}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "-$%,.2f", item.amount),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )

                                    val targetTx = item.transactionRef ?: com.example.data.TransactionEntity(
                                        id = 0,
                                        title = item.title,
                                        amount = item.amount,
                                        dateMillis = com.example.data.DateUtils.localDateToMillis(item.dueDate),
                                        category = item.category.ifEmpty { "Planned Bill" },
                                        accountName = item.accountName.ifEmpty { uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: "" },
                                        isIncome = false,
                                        isCompleted = false,
                                        isAutoPay = item.isAutoPay
                                    )
                                    val isItemCompleted = targetTx.isCompleted

                                    IconButton(
                                        onClick = { onToggleCompleted(targetTx) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("mark_upcoming_completed_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = if (isItemCompleted) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                            contentDescription = if (isItemCompleted) "Marked Paid" else "Mark Paid",
                                            tint = if (isItemCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { itemToDelete = item },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .testTag("delete_upcoming_expense_${item.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Expense",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (upcomingItems.size > 5) {
                            Text(
                                text = "+ ${upcomingItems.size - 5} more upcoming expenses",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .clickable { onOpenAddBill() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = {
                Text(
                    text = "Delete Upcoming Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.title}' (${formatCurrency(target.amount)}) scheduled for ${target.dueDate}?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteExpense(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_delete_upcoming_expense_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_upcoming_expense_btn")
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountSwitcherCarousel(
    uiState: BudgetUiState,
    onSelectAccount: (String) -> Unit,
    onAddAccount: (String) -> Unit,
    onRenameAccount: (String, String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onMoveAccount: ((String, Int) -> Unit)? = null,
    onSetAccountBalance: ((String, Double) -> Unit)? = null,
    onAddAccountWithBalance: ((String, Double) -> Unit)? = null,
    onTransferFunds: ((String, String, Double, String) -> Unit)? = null,
    onReconcileBalance: ((String, Double) -> Unit)? = null,
    isWideScreen: Boolean = false,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountInitialBalance by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // Dialog state for Transfers and Reconciliation
    var showTransferModal by remember { mutableStateOf(false) }
    var showReconcileModal by remember { mutableStateOf(false) }

    // Dialog state for Reordering
    var showReorderAccountsModal by remember { mutableStateOf(false) }

    // Dialog state for Editing / Deleting
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf("") }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountBalanceStr by remember { mutableStateOf("") }
    var editErrorMsg by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var internalExpanded by rememberSaveable { mutableStateOf(true) }
    val carouselExpanded = expanded ?: internalExpanded
    var isReorderMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val toggleAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onToggleExpand != null) onToggleExpand() else internalExpanded = !internalExpanded
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 1.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { toggleAction() }
                    .padding(vertical = 3.dp, horizontal = 2.dp)
            ) {
                Icon(
                    imageVector = if (carouselExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (carouselExpanded) "Collapse accounts" else "Expand accounts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isWideScreen) "ACCOUNTS & LEDGERS" else "ACCOUNTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }

            // Horizontally scrollable actions toolbar so it NEVER squeezes the title into a vertical line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                // Transfer Funds button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showTransferModal = true
                        }
                        .testTag("transfer_accounts_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Transfer Funds",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Transfer",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Reconcile Balance button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.75f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReconcileModal = true
                        }
                        .testTag("reconcile_accounts_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Reconcile Balance",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Reconcile",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Manage / Edit Accounts button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReorderAccountsModal = true
                        }
                        .testTag("manage_accounts_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Manage Accounts",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Manage",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (uiState.allAccounts.size > 1) {
                    if (isReorderMode) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                isReorderMode = false
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("toggle_reorder_accounts_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Accounts",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Lock",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showReorderAccountsModal = true
                                }
                                .testTag("toggle_reorder_accounts_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = "Reorder Accounts",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "Reorder",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { showAddAccountDialog = true }
                        .testTag("add_account_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Add",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (carouselExpanded) {
            if (isWideScreen) {
                // Adaptive Compact Multi-Column Grid for Galaxy Z Fold / Landscape
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val colCount = when {
                        maxWidth >= 1050.dp -> 5
                        maxWidth >= 800.dp -> 4
                        maxWidth >= 520.dp -> 3
                        maxWidth >= 320.dp -> 2
                        else -> 2
                    }
                    val chunkedAccounts = remember(uiState.allAccounts, colCount) { uiState.allAccounts.chunked(colCount) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunkedAccounts.forEach { rowAccounts ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowAccounts.forEach { acc ->
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        AccountCardItem(
                                            acc = acc,
                                            uiState = uiState,
                                            isReorderMode = isReorderMode,
                                            isWideScreen = true,
                                            haptic = haptic,
                                            onSelectAccount = onSelectAccount,
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                if (acc != "All Accounts") {
                                                    accountToEdit = acc
                                                    editAccountName = acc
                                                    val currBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                                                    editAccountBalanceStr = String.format(Locale.US, "%.2f", currBal)
                                                    editErrorMsg = ""
                                                    showEditAccountDialog = true
                                                } else {
                                                    showReorderAccountsModal = true
                                                }
                                            },
                                            onEditAccount = {
                                                accountToEdit = acc
                                                editAccountName = acc
                                                val currBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                                                editAccountBalanceStr = String.format(Locale.US, "%.2f", currBal)
                                                editErrorMsg = ""
                                                showEditAccountDialog = true
                                            },
                                            onMoveLeft = { onMoveAccount?.invoke(acc, -1) },
                                            onMoveRight = { onMoveAccount?.invoke(acc, 1) },
                                            modifier = Modifier.fillMaxHeight()
                                        )
                                    }
                                }
                                val missing = colCount - rowAccounts.size
                                repeat(missing) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.allAccounts, key = { it }) { acc ->
                        AccountCardItem(
                            acc = acc,
                            uiState = uiState,
                            isReorderMode = isReorderMode,
                            isWideScreen = false,
                            haptic = haptic,
                            onSelectAccount = onSelectAccount,
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (acc != "All Accounts") {
                                    accountToEdit = acc
                                    editAccountName = acc
                                    val currBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                                    editAccountBalanceStr = String.format(Locale.US, "%.2f", currBal)
                                    editErrorMsg = ""
                                    showEditAccountDialog = true
                                } else {
                                    showReorderAccountsModal = true
                                }
                            },
                            onEditAccount = {
                                accountToEdit = acc
                                editAccountName = acc
                                val currBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                                editAccountBalanceStr = String.format(Locale.US, "%.2f", currBal)
                                editErrorMsg = ""
                                showEditAccountDialog = true
                            },
                            onMoveLeft = { onMoveAccount?.invoke(acc, -1) },
                            onMoveRight = { onMoveAccount?.invoke(acc, 1) }
                        )
                    }
                }
            }
        }
    }

    // Modal Dialog: Manage & Reorder Accounts
    if (showReorderAccountsModal) {
        AlertDialog(
            onDismissRequest = { showReorderAccountsModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Manage Accounts & Ledgers", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Edit names and balances, switch active ledgers, or reorder the display sequence of your accounts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    uiState.allAccounts.forEachIndexed { index, acc ->
                        val accStyle = getAccountColorStyle(acc, isSystemInDarkTheme())
                        val isSelected = acc == uiState.activeAccountName
                        val accBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            onSelectAccount(acc)
                                        }
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accStyle.borderColor,
                                        modifier = Modifier.size(10.dp)
                                    ) {}
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = acc,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Active",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = String.format(Locale.US, "$%,.2f", accBal),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (accBal >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                            )
                                            Text(
                                                text = "• Position ${index + 1} of ${uiState.allAccounts.size}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    // Edit / Rename / Balance Button
                                    if (acc != "All Accounts") {
                                        FilledTonalIconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                accountToEdit = acc
                                                editAccountName = acc
                                                val currBal = uiState.accountBalances[acc]?.clearedBalance ?: 0.0
                                                editAccountBalanceStr = String.format(Locale.US, "%.2f", currBal)
                                                editErrorMsg = ""
                                                showEditAccountDialog = true
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Account $acc",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Move Up / Left
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onMoveAccount?.invoke(acc, -1)
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move earlier",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Move Down / Right
                                    FilledTonalIconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onMoveAccount?.invoke(acc, 1)
                                        },
                                        enabled = index < uiState.allAccounts.size - 1,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move later",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            newAccountName = ""
                            newAccountInitialBalance = ""
                            errorMsg = ""
                            showAddAccountDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Create Another Account / Ledger")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showReorderAccountsModal = false
                        isReorderMode = false
                    }
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Done")
                }
            }
        )
    }

    if (showAddAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddAccountDialog = false
                newAccountName = ""
                newAccountInitialBalance = ""
                errorMsg = ""
            },
            title = { Text("Create New Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Associate incomes or expenses with this specific ledger to manage multiple pockets.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        label = { Text("Account Name") },
                        placeholder = { Text("e.g. Savings, Vault, Groceries") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_account_name_input")
                    )
                    OutlinedTextField(
                        value = newAccountInitialBalance,
                        onValueChange = { newAccountInitialBalance = it },
                        label = { Text("Starting Balance / Amount ($) [Optional]") },
                        placeholder = { Text("0.00") },
                        leadingIcon = {
                            Text(
                                "$",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_account_balance_input")
                    )
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newAccountName.trim()
                        if (name.isEmpty()) {
                            errorMsg = "Account name cannot be empty."
                        } else if (uiState.allAccounts.contains(name)) {
                            errorMsg = "An account with that name already exists."
                        } else {
                            val initBal = newAccountInitialBalance.toDoubleOrNull() ?: 0.0
                            if (onAddAccountWithBalance != null) {
                                onAddAccountWithBalance(name, initBal)
                            } else {
                                onAddAccount(name)
                                if (initBal > 0) {
                                    onSetAccountBalance?.invoke(name, initBal)
                                }
                            }
                            onSelectAccount(name) // auto-select the new account
                            showAddAccountDialog = false
                            newAccountName = ""
                            newAccountInitialBalance = ""
                            errorMsg = ""
                        }
                    },
                    modifier = Modifier.testTag("add_account_confirm_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddAccountDialog = false
                    newAccountName = ""
                    newAccountInitialBalance = ""
                    errorMsg = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit and Delete Options Dialog
    if (showEditAccountDialog) {
        AlertDialog(
            onDismissRequest = {
                showEditAccountDialog = false
                accountToEdit = ""
                editAccountName = ""
                editAccountBalanceStr = ""
                editErrorMsg = ""
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Edit Account: $accountToEdit", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Update the ledger name and set its current balance amount.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = editAccountName,
                        onValueChange = { editAccountName = it },
                        label = { Text("Account Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_account_name_input")
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    // Editable Amount / Balance
                    Text(
                        text = "Account Balance / Amount:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = editAccountBalanceStr,
                        onValueChange = { editAccountBalanceStr = it },
                        label = { Text("Current Balance / Amount ($)") },
                        leadingIcon = {
                            Text(
                                "$",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_account_balance_input")
                    )

                    Text(
                        text = "💡 Updating this amount sets this account's starting balance so your PayCycle funds align with your real bank balance.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    if (editErrorMsg.isNotEmpty()) {
                        Text(editErrorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    // Set as active account quick action
                    if (accountToEdit != uiState.activeAccountName) {
                        OutlinedButton(
                            onClick = {
                                onSelectAccount(accountToEdit)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Switch Active View to '$accountToEdit'")
                        }
                    }

                    if (onMoveAccount != null && uiState.allAccounts.size > 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        val accIdx = uiState.allAccounts.indexOf(accountToEdit)
                        Text(
                            text = "Reorder Account Position",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onMoveAccount(accountToEdit, -1)
                                },
                                enabled = accIdx > 0,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Move Left", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = {
                                    onMoveAccount(accountToEdit, 1)
                                },
                                enabled = accIdx < uiState.allAccounts.size - 1,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Move Right", fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    if (uiState.allAccounts.size > 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Danger Zone",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Permanently remove this account and all its transactions.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .testTag("delete_account_trigger_$accountToEdit")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = editAccountName.trim()
                        if (name.isEmpty()) {
                            editErrorMsg = "Account name cannot be empty."
                        } else if (name != accountToEdit && uiState.allAccounts.contains(name)) {
                            editErrorMsg = "An account with that name already exists."
                        } else {
                            if (name != accountToEdit) {
                                onRenameAccount(accountToEdit, name)
                            }
                            val targetBal = editAccountBalanceStr.toDoubleOrNull()
                            if (targetBal != null) {
                                onSetAccountBalance?.invoke(name, targetBal)
                            }
                            showEditAccountDialog = false
                            accountToEdit = ""
                            editAccountName = ""
                            editAccountBalanceStr = ""
                            editErrorMsg = ""
                        }
                    },
                    modifier = Modifier.testTag("edit_account_save_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditAccountDialog = false
                    accountToEdit = ""
                    editAccountName = ""
                    editAccountBalanceStr = ""
                    editErrorMsg = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Account?") },
            text = {
                Text("Are you sure you want to delete '$accountToEdit'? All transactions belonging to this account will be permanently deleted. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteAccount(accountToEdit)
                        showDeleteConfirmDialog = false
                        showEditAccountDialog = false
                        accountToEdit = ""
                        editAccountName = ""
                        editErrorMsg = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showTransferModal) {
        TransferFundsDialog(
            uiState = uiState,
            onDismiss = { showTransferModal = false },
            onConfirmTransfer = { from, to, amount, note ->
                onTransferFunds?.invoke(from, to, amount, note)
            }
        )
    }

    if (showReconcileModal) {
        ReconcileBalanceDialog(
            uiState = uiState,
            onDismiss = { showReconcileModal = false },
            onConfirmReconcile = { acc, actualBal ->
                onReconcileBalance?.invoke(acc, actualBal)
            }
        )
    }
}

@Composable
fun TransferFundsDialog(
    uiState: BudgetUiState,
    onDismiss: () -> Unit,
    onConfirmTransfer: (from: String, to: String, amount: Double, note: String) -> Unit
) {
    val nonAllAccounts = remember(uiState.allAccounts) {
        uiState.allAccounts.filter { it != "All Accounts" }
    }
    var fromAccount by remember {
        mutableStateOf(
            if (uiState.activeAccountName != "All Accounts" && nonAllAccounts.contains(uiState.activeAccountName)) {
                uiState.activeAccountName
            } else {
                nonAllAccounts.firstOrNull() ?: "Primary Account"
            }
        )
    }
    var toAccount by remember {
        mutableStateOf(
            nonAllAccounts.firstOrNull { it != fromAccount } ?: nonAllAccounts.lastOrNull() ?: ""
        )
    }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val fromBal = uiState.accountBalances[fromAccount]?.clearedBalance ?: 0.0
    val toBal = uiState.accountBalances[toAccount]?.clearedBalance ?: 0.0
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val newFromBal = fromBal - parsedAmount
    val newToBal = toBal + parsedAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Transfer Funds", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // From Account Selector
                Column {
                    Text("From Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        nonAllAccounts.forEach { acc ->
                            val isSelected = acc == fromAccount
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    fromAccount = acc
                                    if (toAccount == acc) {
                                        toAccount = nonAllAccounts.firstOrNull { it != acc } ?: ""
                                    }
                                },
                                label = {
                                    Text(
                                        "$acc (${String.format(Locale.US, "$%,.0f", uiState.accountBalances[acc]?.clearedBalance ?: 0.0)})",
                                        fontSize = 9.5.sp
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                // To Account Selector
                Column {
                    Text("To Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        nonAllAccounts.filter { it != fromAccount }.forEach { acc ->
                            val isSelected = acc == toAccount
                            FilterChip(
                                selected = isSelected,
                                onClick = { toAccount = acc },
                                label = {
                                    Text(
                                        "$acc (${String.format(Locale.US, "$%,.0f", uiState.accountBalances[acc]?.clearedBalance ?: 0.0)})",
                                        fontSize = 9.5.sp
                                    )
                                },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() || c == '.' }
                        errorMessage = ""
                    },
                    label = { Text("Transfer Amount ($)") },
                    prefix = { Text("$", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Amount Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(25.0, 50.0, 100.0, 250.0, 500.0).forEach { quickVal ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { amountText = String.format(Locale.US, "%.0f", quickVal) }
                        ) {
                            Text(
                                text = "+$${quickVal.toInt()}",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (fromBal > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.clickable { amountText = String.format(Locale.US, "%.2f", fromBal) }
                        ) {
                            Text(
                                text = "Max ($${String.format(Locale.US, "%,.2f", fromBal)})",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Real-time Balance Preview
                if (parsedAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("BALANCE IMPACT PREVIEW", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(fromAccount, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${String.format(Locale.US, "$%,.2f", fromBal)} → ${String.format(Locale.US, "$%,.2f", newFromBal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (newFromBal >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(toAccount, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "${String.format(Locale.US, "$%,.2f", toBal)} → ${String.format(Locale.US, "$%,.2f", newToBal)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                }

                // Optional Memo
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Memo (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (parsedAmount <= 0.0) {
                        errorMessage = "Please enter a valid transfer amount greater than $0"
                        return@Button
                    }
                    if (fromAccount == toAccount) {
                        errorMessage = "Source and destination accounts must be different"
                        return@Button
                    }
                    onConfirmTransfer(fromAccount, toAccount, parsedAmount, noteText.trim())
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_transfer_funds_button")
            ) {
                Text("Confirm Transfer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReconcileBalanceDialog(
    uiState: BudgetUiState,
    onDismiss: () -> Unit,
    onConfirmReconcile: (account: String, actualBalance: Double) -> Unit
) {
    val nonAllAccounts = remember(uiState.allAccounts) {
        uiState.allAccounts.filter { it != "All Accounts" }
    }
    var selectedAccount by remember {
        mutableStateOf(
            if (uiState.activeAccountName != "All Accounts" && nonAllAccounts.contains(uiState.activeAccountName)) {
                uiState.activeAccountName
            } else {
                nonAllAccounts.firstOrNull() ?: "Primary Account"
            }
        )
    }
    val currentAppBal = uiState.accountBalances[selectedAccount]?.clearedBalance ?: 0.0
    var actualBankBalanceText by remember(selectedAccount) {
        mutableStateOf(String.format(Locale.US, "%.2f", currentAppBal))
    }
    var errorMessage by remember { mutableStateOf("") }

    val parsedActualBal = actualBankBalanceText.toDoubleOrNull() ?: currentAppBal
    val delta = parsedActualBal - currentAppBal

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                Text("Bank Balance Reconciliation", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Compare your app balance against your bank's online statement. If there's a difference, the app will log an adjustment to match to the penny.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Account Selection
                Column {
                    Text("Select Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        nonAllAccounts.forEach { acc ->
                            FilterChip(
                                selected = acc == selectedAccount,
                                onClick = { selectedAccount = acc },
                                label = { Text(acc, fontSize = 9.5.sp) },
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }

                // Current App Balance Display
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("CURRENT APP CLEARED BALANCE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", currentAppBal),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (currentAppBal >= 0) Color(0xFF059669) else MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Bank Online Balance Input
                OutlinedTextField(
                    value = actualBankBalanceText,
                    onValueChange = {
                        actualBankBalanceText = it.filter { c -> c.isDigit() || c == '.' || c == '-' }
                        errorMessage = ""
                    },
                    label = { Text("Actual Online Bank Balance ($)") },
                    prefix = { Text("$", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Discrepancy Live Feedback
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        kotlin.math.abs(delta) < 0.005 -> Color(0xFF10B981).copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (kotlin.math.abs(delta) < 0.005) Color(0xFF10B981).copy(alpha = 0.4f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(if (kotlin.math.abs(delta) < 0.005) "✅" else "⚖️", fontSize = 16.sp)
                        Column {
                            Text(
                                text = when {
                                    kotlin.math.abs(delta) < 0.005 -> "Balances match perfectly! No adjustment needed."
                                    delta > 0 -> "Bank is higher by +$${String.format(Locale.US, "%,.2f", delta)}"
                                    else -> "Bank is lower by -$${String.format(Locale.US, "%,.2f", kotlin.math.abs(delta))}"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    kotlin.math.abs(delta) < 0.005 -> Color(0xFF047857)
                                    delta > 0 -> Color(0xFF059669)
                                    else -> Color(0xFFDC2626)
                                }
                            )
                            if (kotlin.math.abs(delta) >= 0.005) {
                                Text(
                                    text = "Reconciling will record a $${String.format(Locale.US, "%,.2f", kotlin.math.abs(delta))} balance adjustment to match.",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val actualBal = actualBankBalanceText.toDoubleOrNull()
                    if (actualBal == null) {
                        errorMessage = "Please enter a valid bank balance"
                        return@Button
                    }
                    onConfirmReconcile(selectedAccount, actualBal)
                    onDismiss()
                },
                modifier = Modifier.testTag("confirm_reconcile_balance_button")
            ) {
                Text("Reconcile Balance", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCardItem(
    acc: String,
    uiState: BudgetUiState,
    isReorderMode: Boolean,
    isWideScreen: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    onSelectAccount: (String) -> Unit,
    onLongClick: () -> Unit,
    onEditAccount: () -> Unit,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSelected = acc == uiState.activeAccountName
    val balance = uiState.accountBalances[acc]
    val clearedBalance = balance?.clearedBalance ?: ((balance?.payPeriodIncome ?: 0.0) + (balance?.carryoverFromPrevious ?: 0.0) - (balance?.completedExpenses ?: 0.0))
    val completedExpenses = balance?.completedExpenses ?: 0.0
    val totalIncome = balance?.payPeriodIncome ?: balance?.totalIncome ?: 0.0
    val isOverBudget = clearedBalance < 0.0

    val borderStroke = if (isReorderMode) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
    } else if (isOverBudget) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
    } else if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    val cardModifier = if (isWideScreen) {
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .combinedClickable(
                onClick = { onSelectAccount(acc) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("account_card_$acc")
    } else {
        modifier
            .width(if (isReorderMode) 120.dp else 110.dp)
            .combinedClickable(
                onClick = { onSelectAccount(acc) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .testTag("account_card_$acc")
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverBudget) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f)
            } else if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = borderStroke
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            val accStyle = getAccountColorStyle(acc, isSystemInDarkTheme())
            // Top accent color bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isWideScreen) 2.dp else 4.dp)
                    .background(accStyle.borderColor)
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = if (isWideScreen) 6.dp else 7.dp, vertical = if (isWideScreen) 4.dp else 5.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 1.dp else 1.5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = acc,
                        fontSize = if (isWideScreen) 11.sp else 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isOverBudget) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isOverBudget) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning: Over Budget",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(if (isWideScreen) 11.dp else 12.dp)
                                    .testTag("account_warning_icon_$acc")
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isWideScreen) 11.dp else 12.dp)
                            )
                        }

                        if (acc != "All Accounts") {
                            Box(
                                modifier = Modifier
                                    .size(if (isWideScreen) 18.dp else 22.dp)
                                    .clip(CircleShape)
                                    .clickable { onEditAccount() }
                                    .testTag("edit_account_icon_$acc"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Account $acc",
                                    tint = if (isOverBudget) {
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                    } else if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                                    },
                                    modifier = Modifier.size(if (isWideScreen) 10.dp else 12.dp)
                                )
                            }
                        }
                    }
                }

                val inCycleTxCount = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd, uiState.allTransactions, acc) {
                    val pStart = DateUtils.localDateToMillis(uiState.currentPayPeriodStart)
                    val pEnd = DateUtils.localDateToMillis(uiState.currentPayPeriodEnd.plusDays(1))
                    if (acc == "All Accounts") {
                        uiState.allTransactions.count { it.dateMillis >= pStart && it.dateMillis < pEnd && it.amount > 0.0 }
                    } else {
                        val primaryAcc = uiState.allAccounts.firstOrNull { it != "All Accounts" && !it.equals("Primary Account", ignoreCase = true) } ?: ""
                        uiState.allTransactions.count {
                            it.dateMillis >= pStart && it.dateMillis < pEnd && it.amount > 0.0 &&
                            (it.accountName.equals(acc, ignoreCase = true) || (primaryAcc.isNotEmpty() && acc.equals(primaryAcc, ignoreCase = true) && (it.accountName.isBlank() || it.accountName.equals("Primary Account", ignoreCase = true) || it.accountName.equals("Primary", ignoreCase = true))))
                        }
                    }
                }

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f", clearedBalance),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            color = if (isOverBudget) {
                                MaterialTheme.colorScheme.error
                            } else if (clearedBalance >= 0) {
                                Color(0xFF10B981)
                            } else {
                                Color(0xFFEF4444)
                            }
                        )

                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (inCycleTxCount > 0) accStyle.borderColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = if (inCycleTxCount > 0) "$inCycleTxCount in cycle" else "0",
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (inCycleTxCount > 0) accStyle.borderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp)
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "+$%,.0f • -$%,.0f", totalIncome, completedExpenses),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isOverBudget) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        }
                    )
                } else {
                    Text(
                        text = String.format(Locale.US, "$%,.2f", clearedBalance),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isOverBudget) {
                            MaterialTheme.colorScheme.error
                        } else if (clearedBalance >= 0) {
                            Color(0xFF10B981)
                        } else {
                            Color(0xFFEF4444)
                        }
                    )

                    Text(
                        text = String.format(Locale.US, "+$%,.0f • -$%,.0f", totalIncome, completedExpenses),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverBudget) {
                            MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = if (inCycleTxCount > 0) accStyle.borderColor.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Text(
                            text = if (inCycleTxCount > 0) "$inCycleTxCount in cycle" else "0 items",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (inCycleTxCount > 0) accStyle.borderColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                        )
                    }
                }

                // Quick Reorder Controls (Move Left / Move Right)
                if (isReorderMode && uiState.allAccounts.size > 1) {
                    val accIndex = uiState.allAccounts.indexOf(acc)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (accIndex > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(enabled = accIndex > 0) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMoveLeft()
                                }
                                .testTag("move_left_account_$acc")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "Move Left",
                                    tint = if (accIndex > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        Text(
                            text = "#${accIndex + 1}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Surface(
                            shape = CircleShape,
                            color = if (accIndex < uiState.allAccounts.size - 1) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(enabled = accIndex < uiState.allAccounts.size - 1) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMoveRight()
                                }
                                .testTag("move_right_account_$acc")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Move Right",
                                    tint = if (accIndex < uiState.allAccounts.size - 1) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlannedBillsDialog(
    uiState: BudgetUiState,
    onAddPlannedExpense: (title: String, dueDate: String, minAmount: Double, targetAccount: String, isAutoPay: Boolean, reminderDaysBefore: Int) -> Unit,
    onDeletePlannedExpense: (PlannedExpenseEntity) -> Unit,
    onUpdatePlannedExpenseAutoPaySource: ((PlannedExpenseEntity, String, Boolean) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    var title by remember { mutableStateOf("") }
    var minAmount by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var targetAccount by remember { mutableStateOf(uiState.activeAccountName) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var isAutoPay by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableStateOf(0) } // Default on due date (day of)
    var reminderDropdownExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    val reminderOptions = listOf(
        0 to "On Due Date (Day of at 9 AM)",
        1 to "1 Day Before Due Date",
        3 to "3 Days Before Due Date",
        7 to "1 Week Before Due Date",
        -1 to "No Reminder"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Planned Bills & Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Manage recurring bills and set automated due date reminders. Creating a bill automatically appends it directly to your transaction ledger forecast.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Form: Create a new Planned Bill Card container
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Add New Scheduled Bill",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Bill Title (e.g. Electric Bill, Rent)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("planned_expense_title_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = minAmount,
                                onValueChange = { minAmount = it },
                                label = { Text("Minimum Amount") },
                                prefix = { Text("$") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("planned_expense_min_amount_input")
                            )

                            DatePickerTextField(
                                value = dueDate,
                                onValueChange = { dueDate = it },
                                label = { Text("Due Date (YYYY-MM-DD)") },
                                testTag = "planned_expense_due_date_input",
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        // Target Account dropdown selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = targetAccount,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Deduct From Account") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { accountDropdownExpanded = !accountDropdownExpanded },
                                        modifier = Modifier.testTag("planned_expense_account_dropdown_trigger")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Show accounts"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { accountDropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                uiState.allAccounts.forEach { acc ->
                                    DropdownMenuItem(
                                        text = { Text(acc) },
                                        onClick = {
                                            targetAccount = acc
                                            accountDropdownExpanded = false
                                        },
                                        modifier = Modifier.testTag("planned_expense_account_option_$acc")
                                    )
                                }
                            }
                        }

                        // Reminder Dropdown Option
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = reminderOptions.find { it.first == reminderDaysBefore }?.second ?: "1 Day Before Due Date",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("🔔 Bill Due Reminder") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = if (reminderDaysBefore >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { reminderDropdownExpanded = !reminderDropdownExpanded },
                                        modifier = Modifier.testTag("planned_expense_reminder_dropdown_trigger")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Show reminder options"
                                        )
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { reminderDropdownExpanded = true }
                            )

                            DropdownMenu(
                                expanded = reminderDropdownExpanded,
                                onDismissRequest = { reminderDropdownExpanded = false }
                            ) {
                                reminderOptions.forEach { (days, label) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Notifications,
                                                    contentDescription = null,
                                                    tint = if (days >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Text(label)
                                            }
                                        },
                                        onClick = {
                                            reminderDaysBefore = days
                                            reminderDropdownExpanded = false
                                            if (days >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        },
                                        modifier = Modifier.testTag("planned_expense_reminder_option_$days")
                                    )
                                }
                            }
                        }

                        // Auto Pay Checkbox Toggle Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { isAutoPay = !isAutoPay }
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = isAutoPay,
                                    onCheckedChange = { isAutoPay = it },
                                    modifier = Modifier.testTag("planned_expense_autopay_checkbox")
                                )
                            }
                            Text(
                                text = "⚡ Auto Pay enabled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (isAutoPay) {
                            EditableAutoPayTag(
                                accountName = targetAccount,
                                isAutoPay = isAutoPay,
                                allAccounts = uiState.allAccounts,
                                onUpdateAutoPaySource = { newAccount, enabled ->
                                    targetAccount = newAccount
                                    isAutoPay = enabled
                                }
                            )
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission && reminderDaysBefore >= 0) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Tap to grant notification permission for bill reminders.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val t = title.trim()
                                val amtVal = minAmount.toDoubleOrNull()
                                val parsedDDateStr = dueDate.trim()
                                
                                val dateValid = try {
                                    LocalDate.parse(parsedDDateStr)
                                    true
                                } catch (e: Exception) {
                                    false
                                }

                                if (t.isEmpty()) {
                                    errorMsg = "Please enter a descriptive bill title."
                                } else if (amtVal == null || amtVal <= 0.0) {
                                    errorMsg = "Please enter a valid minimum amount."
                                } else if (!dateValid) {
                                    errorMsg = "Please format due date correctly (YYYY-MM-DD)."
                                } else {
                                    if (reminderDaysBefore >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onAddPlannedExpense(t, parsedDDateStr, amtVal, targetAccount, isAutoPay, reminderDaysBefore)
                                    // Reset fields after successful draft
                                    title = ""
                                    minAmount = ""
                                    dueDate = LocalDate.now().toString()
                                    isAutoPay = false
                                    reminderDaysBefore = 0
                                    errorMsg = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("planned_expense_add_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create & Auto-Post Bill")
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Subtitle of Existing Items
                Text(
                    text = "Scheduled Bills Calendar Catalog (${uiState.plannedExpenses.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (uiState.plannedExpenses.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "No scheduled bills yet.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (bill in uiState.plannedExpenses) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bill.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ) {
                                                Text(
                                                    text = bill.targetAccount,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(
                                                text = "Due: ${bill.dueDate}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (bill.isAutoPay) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                ) {
                                                    Text(
                                                        text = "⚡ AutoPay",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = formatCurrency(bill.minAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        IconButton(
                                            onClick = { onDeletePlannedExpense(bill) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete planned expense",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("planned_expenses_close_button")
            ) {
                Text("Close")
            }
        }
    )
}

data class AccountColorStyle(
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color
)

@Composable
fun getAccountColorStyle(accountName: String, isDark: Boolean): AccountColorStyle {
    val norm = accountName.trim().lowercase()
    return when {
        norm.contains("savings") || norm.contains("reserve") -> {
            if (isDark) {
                AccountColorStyle(
                    containerColor = Color(0xFF064E3B).copy(alpha = 0.35f),
                    borderColor = Color(0xFF10B981),
                    contentColor = Color(0xFF6EE7B7)
                )
            } else {
                AccountColorStyle(
                    containerColor = Color(0xFFD1FAE5),
                    borderColor = Color(0xFF059669),
                    contentColor = Color(0xFF065F46)
                )
            }
        }
        norm.contains("credit") || norm.contains("card") || norm.contains("visa") || norm.contains("mastercard") -> {
            if (isDark) {
                AccountColorStyle(
                    containerColor = Color(0xFF4C1D95).copy(alpha = 0.35f),
                    borderColor = Color(0xFF8B5CF6),
                    contentColor = Color(0xFFC4B5FD)
                )
            } else {
                AccountColorStyle(
                    containerColor = Color(0xFFEDE9FE),
                    borderColor = Color(0xFF7C3AED),
                    contentColor = Color(0xFF5B21B6)
                )
            }
        }
        norm.contains("emergency") || norm.contains("rainy") -> {
            if (isDark) {
                AccountColorStyle(
                    containerColor = Color(0xFF78350F).copy(alpha = 0.35f),
                    borderColor = Color(0xFFF59E0B),
                    contentColor = Color(0xFFFCD34D)
                )
            } else {
                AccountColorStyle(
                    containerColor = Color(0xFFFEF3C7),
                    borderColor = Color(0xFFD97706),
                    contentColor = Color(0xFF92400E)
                )
            }
        }
        norm.contains("investment") || norm.contains("crypto") || norm.contains("stock") -> {
            if (isDark) {
                AccountColorStyle(
                    containerColor = Color(0xFF1E3A8A).copy(alpha = 0.35f),
                    borderColor = Color(0xFF3B82F6),
                    contentColor = Color(0xFF93C5FD)
                )
            } else {
                AccountColorStyle(
                    containerColor = Color(0xFFDBEAFE),
                    borderColor = Color(0xFF2563EB),
                    contentColor = Color(0xFF1E40AF)
                )
            }
        }
        else -> {
            if (isDark) {
                AccountColorStyle(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                    borderColor = Color(0xFF38BDF8),
                    contentColor = Color(0xFFBAE6FD)
                )
            } else {
                AccountColorStyle(
                    containerColor = Color(0xFFE0F2FE),
                    borderColor = Color(0xFF0284C7),
                    contentColor = Color(0xFF0369A1)
                )
            }
        }
    }
}

data class AccountInCycleSummary(
    val accountName: String,
    val totalIncome: Double,
    val totalExpenses: Double,
    val completedExpenses: Double,
    val pendingExpenses: Double,
    val net: Double,
    val transactionCount: Int
)

@Composable
fun EditableAutoPayTag(
    accountName: String,
    isAutoPay: Boolean,
    allAccounts: List<String>,
    onUpdateAutoPaySource: (String, Boolean) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isAutoPay) Color(0xFF0284C7).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            0.8.dp,
            if (isAutoPay) Color(0xFF38BDF8) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .clickable { expanded = true }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        ) {
            Text(
                text = "⚡ AutoPay",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isAutoPay) Color(0xFF38BDF8) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "▾",
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("⚡ Keep AutoPay Enabled") },
                onClick = {
                    onUpdateAutoPaySource(accountName, true)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("❌ Disable AutoPay") },
                onClick = {
                    onUpdateAutoPaySource(accountName, false)
                    expanded = false
                }
            )
            if (allAccounts.size > 1) {
                HorizontalDivider()
                allAccounts.filter { it != accountName }.forEach { acc ->
                    DropdownMenuItem(
                        text = { Text("Switch to ") },
                        onClick = {
                            onUpdateAutoPaySource(acc, true)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

fun formatCurrency(amount: Double): String {
    return String.format(Locale.US, "$%,.2f", amount)
}
