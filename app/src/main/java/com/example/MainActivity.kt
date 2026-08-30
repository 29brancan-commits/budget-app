package com.example

import android.Manifest
import android.widget.Toast
import com.example.sync.*
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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.AppTheme
import com.example.ui.theme.AppIcon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
        setContent {
            val viewModel: BudgetViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            MyApplicationTheme(
                darkTheme = uiState.isDarkMode,
                appTheme = uiState.appTheme
            ) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_activity_scaffold")
                ) { innerPadding ->
                    BudgetAppMain(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

// Global Category Emojis & Colors
object CategoryTheme {
    val expenseCategories = listOf(
        CategoryItem("Housing", "ðŸ ", Color(0xFFF87171)),
        CategoryItem("Utilities", "âš¡", Color(0xFFFBBF24)),
        CategoryItem("Groceries", "ðŸ›’", Color(0xFF34D399)),
        CategoryItem("Dining Out", "ðŸ”", Color(0xFF2DD4BF)),
        CategoryItem("Transportation", "ðŸš—", Color(0xFF60A5FA)),
        CategoryItem("Insurance", "ðŸ›¡ï¸", Color(0xFF818CF8)),
        CategoryItem("Entertainment", "ðŸŽ¬", Color(0xFFC084FC)),
        CategoryItem("Shopping", "ðŸ›ï¸", Color(0xFFFB923C)),
        CategoryItem("Other", "ðŸ“¦", Color(0xFF94A3B8))
    )

    fun getEmoji(categoryName: String): String {
        if (categoryName == "Paycheck") return "ðŸ’°"
        if (categoryName == "Income") return "ðŸ’µ"
        return expenseCategories.firstOrNull { it.name == categoryName }?.emoji ?: "ðŸ’¸"
    }

    fun getColor(categoryName: String): Color {
        if (categoryName == "Paycheck") return Color(0xFF10B981)
        if (categoryName == "Income") return Color(0xFF34D399)
        return expenseCategories.firstOrNull { it.name == categoryName }?.color ?: Color(0xFF64748B)
    }
}

data class CategoryItem(val name: String, val emoji: String, val color: Color)

@Composable
fun BudgetAppMain(
    modifier: Modifier = Modifier,
    viewModel: BudgetViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    val googleSignInLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
            viewModel.handleGoogleSignInResult(account)
        } catch (e: Exception) {
            viewModel.handleGoogleSignInError(e.localizedMessage ?: "Sign in cancelled or not available in emulator")
        }
    }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Cycles, 1 = Income, 2 = Expenses, 3 = Debt, 4 = Calendar, 5 = AI Hub, 6 = Settings
    var showAddDialog by remember { mutableStateOf(false) }
    var showPlannedBillsDialog by remember { mutableStateOf(false) }
    var showAiQuickEntryDialog by remember { mutableStateOf(false) }
    var showAiReceiptScannerDialog by remember { mutableStateOf(false) }
    var preselectedDateForAdd by remember { mutableStateOf<LocalDate?>(null) }
    var showPaycheckEditDialog by remember { mutableStateOf<LocalDate?>(null) }
    var editingPaycheckAmount by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }

    // Formatters
    val monthTitleFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp || isLandscape

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 4.dp)
                ) {
                    // Selected pay cycle / month navigation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.prevMonth() },
                            modifier = Modifier.testTag("prev_month_button")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Previous Pay Cycle")
                        }

                        val payCycleTitleFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
                        val payCycleTitleYearFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
                        val pStart = uiState.currentPayPeriodStart
                        val pEnd = uiState.currentPayPeriodEnd
                        val payCycleText = if (pStart.year == pEnd.year) {
                            "${pStart.format(payCycleTitleFormatter)} â€“ ${pEnd.format(payCycleTitleYearFormatter)}"
                        } else {
                            "${pStart.format(payCycleTitleYearFormatter)} â€“ ${pEnd.format(payCycleTitleYearFormatter)}"
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = payCycleText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Pay Cycle â€¢ ${uiState.selectedMonth.format(monthTitleFormatter)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { viewModel.selectMonth(YearMonth.now()) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                modifier = Modifier.testTag("today_button")
                            ) {
                                Text("Today", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.nextMonth() },
                                modifier = Modifier.testTag("next_month_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Next Pay Cycle")
                            }
                        }
                    }
                }
            },
            bottomBar = {
                if (!isWideScreen) {
                    FoldableBottomNavBar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab != 3 && selectedTab != 5 && selectedTab != 6) {
                    FloatingActionButton(
                        onClick = {
                            preselectedDateForAdd = null
                            showAddDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_transaction_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                if (isWideScreen) {
                    FoldableNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .animateContentSize(
                            animationSpec = spring(
                                stiffness = Spring.StiffnessLow,
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                ) {
                    when (selectedTab) {
                    0 -> {
                        // Pay Cycles Dashboard with Adaptive Side-by-Side Dual-Pane for Galaxy Z Fold / Wide Screens
                        val pageScrollState = rememberScrollState()
                        var dashboardSectionOrder by rememberSaveable {
                            mutableStateOf(listOf("accounts", "upcoming", "cycles"))
                        }
                        var isDashboardReorderActive by rememberSaveable { mutableStateOf(false) }
                        val haptic = LocalHapticFeedback.current

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1800.dp)
                                .align(Alignment.TopCenter)
                                .verticalScroll(pageScrollState)
                        ) {
                            if (isDashboardReorderActive || dashboardSectionOrder != listOf("accounts", "upcoming", "cycles")) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDashboardReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isDashboardReorderActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                                contentDescription = null,
                                                tint = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = if (isDashboardReorderActive) "Sections Unlocked â€¢ Reorder then Lock" else "Custom Section Order â€¢ Locked",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDashboardReorderActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (isDashboardReorderActive) {
                                                Button(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isDashboardReorderActive = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Lock,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Lock Layout", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                FilledTonalButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        isDashboardReorderActive = true
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            TextButton(
                                                onClick = {
                                                    dashboardSectionOrder = listOf("accounts", "upcoming", "cycles")
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

                            dashboardSectionOrder.forEachIndexed { index, sectionKey ->
                                val title = when (sectionKey) {
                                    "accounts" -> "Accounts Overview"
                                    "upcoming" -> "Upcoming Bills & Expenses"
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
                                            dashboardSectionOrder = list
                                        }
                                    },
                                    onMoveDown = {
                                        if (index < dashboardSectionOrder.size - 1) {
                                            val list = dashboardSectionOrder.toMutableList()
                                            val temp = list[index]
                                            list[index] = list[index + 1]
                                            list[index + 1] = temp
                                            dashboardSectionOrder = list
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
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
                                                isWideScreen = isWideScreen
                                            )
                                        }
                                        "upcoming" -> {
                                            UpcomingExpensesWidget(
                                                uiState = uiState,
                                                onToggleCompleted = { tx -> viewModel.toggleTransactionCompleted(tx) },
                                                onOpenAddBill = { showPlannedBillsDialog = true },
                                                onDeleteExpense = { item -> viewModel.deleteUpcomingExpense(item) }
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
                                                        accountName = if (uiState.activeAccountName == "All Accounts") "Primary Account" else uiState.activeAccountName,
                                                        isCompleted = false,
                                                        isAutoPay = false
                                                    )
                                                },
                                                onEditPaycheckClick = { date, currentAmount ->
                                                    editingPaycheckAmount = String.format(Locale.US, "%.2f", currentAmount)
                                                    showPaycheckEditDialog = date
                                                },
                                                onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                                                onEditTransaction = { tx -> editingTransaction = tx },
                                                onToggleCompleted = { tx -> viewModel.toggleTransactionCompleted(tx) },
                                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                                isWideScreen = isWideScreen
                                            )
                                        }
                                    }
                                }
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
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EnterIncomeView(
                                uiState = uiState,
                                onAddIncome = { title, amount, dateDeposited, category, isRecurring, accountName, isAutoPay ->
                                    viewModel.addTransaction(title, amount, dateDeposited, category, true, isRecurring, accountName, isAutoPay)
                                },
                                onDeleteIncome = { tx -> viewModel.deleteTransaction(tx) },
                                onEditIncome = { tx -> editingTransaction = tx },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                isWideScreen = isWideScreen
                            )
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
                                isWideScreen = isWideScreen
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            EnterExpensesView(
                                uiState = uiState,
                                onAddExpense = { title, amount, dueDate, category, isRecurring, accountName, isAutoPay ->
                                    viewModel.addTransaction(title, amount, dueDate, category, false, isRecurring, accountName, isAutoPay)
                                },
                                onDeleteExpense = { tx -> viewModel.deleteTransaction(tx) },
                                onEditExpense = { tx -> editingTransaction = tx },
                                onToggleExpense = { tx -> viewModel.toggleTransactionCompleted(tx) },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                isWideScreen = isWideScreen
                            )
                        }
                    }
                    3 -> {
                        // Debt Planner Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .verticalScroll(pageScrollState)
                        ) {
                            com.example.ui.DebtPlannerView(
                                uiState = uiState,
                                onAddDebt = { name, bal, rate, minPay, cat, notes ->
                                    viewModel.addDebt(name, bal, rate, minPay, cat, notes)
                                },
                                onDeleteDebt = { debt ->
                                    viewModel.deleteDebt(debt)
                                },
                                onUpdateDebt = { debt ->
                                    viewModel.updateDebt(debt)
                                },
                                isWideScreen = isWideScreen,
                                onImportDebtsFromCsv = { csvText -> viewModel.importDebtsFromCsv(csvText) },
                                onExportDebtsToCsv = { viewModel.exportDebtsToCsv() },
                                onSyncGoogleDrive = { viewModel.performDebtDriveSync(null) },
                                onConnectGoogleDrive = { viewModel.performDebtDriveSync(null) },
                                isSyncing = uiState.syncState is com.example.sync.SyncState.Syncing,
                                accountEmail = uiState.driveAccountEmail
                            )
                        }
                    }
                    4 -> {
                        // Calendar Page
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
                                onToggleCompleted = { tx -> viewModel.toggleTransactionCompleted(tx) },
                                onSyncCsvDrive = { onComplete -> viewModel.performCsvDriveSync(onComplete) },
                                isWideScreen = isWideScreen
                            )
                        }
                    }
                    5 -> {
                        // AI Insights Hub Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
                                .verticalScroll(pageScrollState)
                        ) {
                            AiFinancialHubView(
                                uiState = uiState,
                                onOpenQuickEntry = { showAiQuickEntryDialog = true },
                                onOpenReceiptScanner = { showAiReceiptScannerDialog = true }
                            )
                        }
                    }
                    6 -> {
                        // Settings Page
                        val pageScrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 1400.dp)
                                .align(Alignment.TopCenter)
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
                                    googleSignInLauncher.launch(viewModel.getGoogleSignInClient().signInIntent)
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
                                onExportToGoogleSheets = { onComplete -> viewModel.performExportToGoogleSheets(onComplete) },
                                onExportCsv = viewModel::exportToCsv,
                                onImportCsv = viewModel::importFromCsv,
                                onExportWebCompanionToGoogleDrive = { htmlContent -> viewModel.exportWebCompanionToGoogleDrive(htmlContent) },
                                onClearDatabase = { viewModel.clearDatabase() },
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
            onConfirm = { title, amount, date, category, isIncome, isRecurring, accountName, isAutoPay, reminderDaysBefore ->
                viewModel.addTransaction(title, amount, date, category, isIncome, isRecurring, accountName, false, isAutoPay, reminderDaysBefore)
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
            onSave = { updated: TransactionEntity ->
                if (tx.id < 0) {
                    viewModel.addTransaction(
                        title = updated.title,
                        amount = updated.amount,
                        date = DateUtils.millisToLocalDate(updated.dateMillis),
                        category = updated.category,
                        isIncome = updated.isIncome,
                        isRecurring = updated.isRecurring,
                        accountName = updated.accountName,
                        isCompleted = updated.isCompleted,
                        isAutoPay = updated.isAutoPay,
                        reminderDaysBefore = updated.reminderDaysBefore
                    )
                } else {
                    viewModel.updateTransaction(updated)
                }
                editingTransaction = null
            },
            onDelete = { deleted: TransactionEntity ->
                if (deleted.id > 0) {
                    viewModel.deleteTransaction(deleted)
                }
                editingTransaction = null
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
}
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
    onAddIncome: (title: String, amount: Double, dateDeposited: LocalDate, category: String, isRecurring: Boolean, accountName: String, isAutoPay: Boolean) -> Unit,
    onDeleteIncome: (TransactionEntity) -> Unit,
    onEditIncome: (TransactionEntity) -> Unit = {},
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val isDark = uiState.isDarkMode
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by remember { mutableStateOf("Paycheck") }
    var selectedAccount by remember { mutableStateOf(if (uiState.activeAccountName == "All Accounts") "Primary Account" else uiState.activeAccountName) }
    var isRecurring by remember { mutableStateOf(false) }
    var isAutoPay by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isQuickInsertExpanded by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSyncingSheets by remember { mutableStateOf(false) }

    var filterScope by remember { mutableIntStateOf(2) } // 0 = All Rows, 1 = Full Month, 2 = Pay Cycle, 3 = Pay Week
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
        selectedAccount = if (uiState.activeAccountName == "All Accounts") "Primary Account" else uiState.activeAccountName
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
    val dateRangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val weekRangeText = remember(uiState.currentPayWeekStart, uiState.currentPayWeekEnd) {
        "${uiState.currentPayWeekStart.format(dateRangeFormatter)} â€“ ${uiState.currentPayWeekEnd.format(dateRangeFormatter)}"
    }
    val cycleRangeText = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd) {
        "${uiState.currentPayPeriodStart.format(dateRangeFormatter)} â€“ ${uiState.currentPayPeriodEnd.format(dateRangeFormatter)}"
    }

    val incomeCategories = listOf("Paycheck", "Income", "Freelance", "Bonus", "Investment", "Other")

    // Filter income transactions
    val incomeList = remember(uiState.allTransactions, uiState.activeAccountName, sortIncomeAscending) {
        val dbIncome = uiState.allTransactions.filter { tx ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    tx.accountName.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (uiState.activeAccountName == "Primary Account" && (tx.accountName.isBlank() || tx.accountName.equals("Primary", ignoreCase = true)))
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
            else -> incomeList // 0 = All Rows
        }

        scopeFiltered.filter { tx ->
            when (statusFilter) {
                1 -> !tx.isCompleted
                2 -> tx.isCompleted
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
                            text = if (sortIncomeAscending) "ðŸ“… Date â¬†" else "ðŸ“… Date â¬‡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
            }

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
                        Text("ðŸ“Š", fontSize = 18.sp)
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
                                text = "âž• Quick Insert Row",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)
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
                                        .testTag("enter_income_recurring_checkbox")
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("Bi-Weekly", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF166534))
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
                                Text("âš¡ Auto-Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
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
                                        onAddIncome(t, amt, parsedDate, selectedCategory, isRecurring, selectedAccount, isAutoPay)
                                        filterScope = 0
                                        statusMessage = "âœ… Added +${String.format(Locale.US, "%.2f", amt)} row to spreadsheet!"
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
                                        border = BorderStroke(1.dp, accStyle.borderColor)
                                    ) {
                                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.5.dp)
                                                    .background(accStyle.borderColor)
                                            )
                                            Text(
                                                text = "ðŸ’³ " + tx.accountName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accStyle.contentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Date
                                    Text(
                                        text = txDate.format(formatter),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )

                                    if (tx.isRecurring) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                                        ) {
                                            Text(
                                                text = "ðŸ”„ Bi-Weekly",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
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

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isIncomeReorderActive || incomeSectionOrder != listOf("income_form", "income_list")) {
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
                                text = if (isIncomeReorderActive) "Income Sections Unlocked â€¢ Move then Lock" else "Custom Income Layout â€¢ Locked",
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

@Composable
fun EnterExpensesView(
    uiState: BudgetUiState,
    onAddExpense: (title: String, amount: Double, dueDate: LocalDate, category: String, isRecurring: Boolean, accountName: String, isAutoPay: Boolean) -> Unit,
    onDeleteExpense: (TransactionEntity) -> Unit,
    onEditExpense: (TransactionEntity) -> Unit = {},
    onToggleExpense: ((TransactionEntity) -> Unit)? = null,
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    val isDark = uiState.isDarkMode
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dueDateInput by remember { mutableStateOf(LocalDate.now().toString()) }
    var selectedCategory by remember { mutableStateOf("Utilities") }
    var selectedAccount by remember { mutableStateOf(if (uiState.activeAccountName == "All Accounts") "Primary Account" else uiState.activeAccountName) }
    var isRecurring by remember { mutableStateOf(false) }
    var isAutoPay by remember { mutableStateOf(false) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var isQuickInsertExpanded by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isSyncingSheets by remember { mutableStateOf(false) }

    var filterScope by remember { mutableIntStateOf(2) } // 0 = All Rows, 1 = Full Month, 2 = Pay Cycle, 3 = Pay Week
    var sortExpensesAscending by remember { mutableStateOf(false) } // false = Newest First, true = Oldest First
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Pending, 2 = Cleared

    LaunchedEffect(uiState.selectedMonth) {
        val today = LocalDate.now()
        if (YearMonth.from(today) == uiState.selectedMonth) {
            dueDateInput = today.toString()
        } else {
            dueDateInput = uiState.selectedMonth.atDay(1).toString()
        }
    }

    LaunchedEffect(uiState.activeAccountName) {
        selectedAccount = if (uiState.activeAccountName == "All Accounts") "Primary Account" else uiState.activeAccountName
    }

    val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US) }
    val dateRangeFormatter = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val weekRangeText = remember(uiState.currentPayWeekStart, uiState.currentPayWeekEnd) {
        "${uiState.currentPayWeekStart.format(dateRangeFormatter)} â€“ ${uiState.currentPayWeekEnd.format(dateRangeFormatter)}"
    }
    val cycleRangeText = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd) {
        "${uiState.currentPayPeriodStart.format(dateRangeFormatter)} â€“ ${uiState.currentPayPeriodEnd.format(dateRangeFormatter)}"
    }

    val expenseCategories = CategoryTheme.expenseCategories.map { it.name }

    // Filter expenses (including planned bills for active month)
    val expensesList = remember(uiState.allTransactions, uiState.plannedExpenses, uiState.selectedMonth, uiState.activeAccountName, sortExpensesAscending) {
        val dbExpenses = uiState.allTransactions.filter { tx ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    tx.accountName.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (uiState.activeAccountName == "Primary Account" && (tx.accountName.isBlank() || tx.accountName.equals("Primary", ignoreCase = true)))
            val catLower = tx.category.lowercase(java.util.Locale.US)
            val isIncomeCat = catLower == "paycheck" || catLower == "income" || catLower == "salary" || catLower == "bonus" || catLower == "deposit" || catLower == "freelance" || catLower == "investment" || catLower == "investments"
            matchesAccount && !tx.isIncome && !isIncomeCat && tx.amount > 0.0 && !tx.title.contains("Cancelled") && !tx.title.contains("Deleted")
        }

        val month = uiState.selectedMonth
        val syntheticPlanned = uiState.plannedExpenses.mapIndexedNotNull { idx, planned ->
            val matchesAccount = uiState.activeAccountName == "All Accounts" ||
                    planned.targetAccount.equals(uiState.activeAccountName, ignoreCase = true) ||
                    (uiState.activeAccountName == "Primary Account" && (planned.targetAccount.isBlank() || planned.targetAccount.equals("Primary", ignoreCase = true)))
            if (!matchesAccount) return@mapIndexedNotNull null

            val dueDate = try {
                val orig = LocalDate.parse(planned.dueDate)
                val day = minOf(orig.dayOfMonth, month.lengthOfMonth())
                LocalDate.of(month.year, month.month, day)
            } catch (e: Exception) {
                month.atDay(15)
            }
            val dStart = DateUtils.localDateToMillis(dueDate)
            val dEnd = DateUtils.localDateToMillis(dueDate.plusDays(1))
            val existsInDb = uiState.allTransactions.any {
                (it.title.equals(planned.title, ignoreCase = true) || it.title.contains(planned.title, ignoreCase = true)) && it.dateMillis >= dStart && it.dateMillis < dEnd
            }
            if (!existsInDb) {
                val synthId = -kotlin.math.abs((planned.id.toLong() * 10000L + dueDate.dayOfYear.toLong()).toInt()) - 5000
                TransactionEntity(
                    id = synthId,
                    title = planned.title + " (Planned)",
                    amount = planned.minAmount,
                    dateMillis = DateUtils.localDateToMillis(dueDate),
                    category = "Utilities",
                    isIncome = false,
                    isRecurring = true,
                    accountName = if (planned.targetAccount.isBlank()) "Primary Account" else planned.targetAccount
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
        expensesList, filterScope, statusFilter,
        uiState.currentPayWeekStart, uiState.currentPayWeekEnd,
        uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd,
        uiState.selectedMonth
    ) {
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
            else -> expensesList // 0 = All Rows
        }

        scopeFiltered.filter { tx ->
            when (statusFilter) {
                1 -> !tx.isCompleted
                2 -> tx.isCompleted
                else -> true
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
                            text = if (sortExpensesAscending) "ðŸ“… Date â¬†" else "ðŸ“… Date â¬‡",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
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
                        Text("ðŸ“‰", fontSize = 18.sp)
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
                                text = "âž• Quick Insert Expense Row",
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
                                Text("Recur Monthly", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFFCA5A5) else Color(0xFF7F1D1D))
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
                                Text("âš¡ Auto-Pay", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE))
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
                                    label = { Text(lbl, fontSize = 9.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

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
                                        onAddExpense(t, amt, parsedDate, selectedCategory, isRecurring, selectedAccount, isAutoPay)
                                        filterScope = 0
                                        statusMessage = "âœ… Added '$t' (${String.format(Locale.US, "%.2f", amt)}) to expense spreadsheet!"
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
                displayedExpensesList.forEach { tx ->
                    val txDate = DateUtils.millisToLocalDate(tx.dateMillis)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditExpense(tx) }
                            .testTag("expense_card_${tx.id}"),
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
                                    // Account Badge
                                    val accStyle = getAccountColorStyle(tx.accountName, isDark)
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = accStyle.containerColor,
                                        border = BorderStroke(1.dp, accStyle.borderColor)
                                    ) {
                                        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(3.5.dp)
                                                    .background(accStyle.borderColor)
                                            )
                                            Text(
                                                text = "ðŸ’³ " + tx.accountName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accStyle.contentColor,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Date
                                    Text(
                                        text = txDate.format(formatter),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                                    )

                                    if (tx.isRecurring) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE)
                                        ) {
                                            Text(
                                                text = "ðŸ”„ Monthly",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
                                            0 -> "ðŸ”” Day of"
                                            1 -> "ðŸ”” 1d before"
                                            3 -> "ðŸ”” 3d before"
                                            7 -> "ðŸ”” 1w before"
                                            else -> "ðŸ”” Active"
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = reminderText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF34D399) else Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    if (tx.isCompleted) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "âœ“ Paid",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = "â³ Unpaid",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (onToggleExpense != null) {
                                        IconButton(
                                            onClick = { onToggleExpense(tx) },
                                            modifier = Modifier
                                                .size(28.dp)
                                                .testTag("toggle_page_expense_${tx.id}")
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .border(
                                                        width = 2.dp,
                                                        color = if (tx.isCompleted) Color(0xFF10B981) else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                                        shape = CircleShape
                                                    )
                                                    .background(
                                                        color = if (tx.isCompleted) Color(0xFF10B981) else Color.Transparent,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (tx.isCompleted) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Paid / Completed",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
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

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isExpenseReorderActive || expenseSectionOrder != listOf("expense_form", "expense_list")) {
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
                                text = if (isExpenseReorderActive) "Expense Sections Unlocked â€¢ Move then Lock" else "Custom Expense Layout â€¢ Locked",
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = if (isSubSection) 2.dp else 4.dp)
            .animateContentSize(),
        shape = RoundedCornerShape(if (isSubSection) 12.dp else 16.dp),
        color = if (isReordering) 
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isSubSection) 0.08f else 0.12f) 
        else 
            Color.Transparent,
        border = BorderStroke(
            width = if (isReordering) 1.5.dp else 0.dp,
            color = if (isReordering) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) 
                   else Color.Transparent
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Drag handle & Section header bar with Move controls
            Surface(
                shape = RoundedCornerShape(if (isSubSection) 10.dp else 12.dp),
                color = if (isReordering) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                       else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (isSubSection) 0.25f else 0.35f),
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
                        .padding(horizontal = if (isSubSection) 8.dp else 12.dp, vertical = if (isSubSection) 4.dp else 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Drag Handle / Lock Status icon
                        Icon(
                            imageVector = if (isReordering) Icons.Default.LockOpen else Icons.Default.DragHandle,
                            contentDescription = if (isReordering) "Unlocked" else "Reorder section",
                            tint = if (isReordering) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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

                    // Move Up / Move Down buttons & Reorder / Lock controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isReordering) {
                            // Move Up button
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

                            // Move Down button
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
                        } else {
                            // Unlocked/Reorder affordance pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                modifier = Modifier.clickable { 
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleReorder() 
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Reorder",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
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

@Composable
fun TabSelector(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    isWideScreen: Boolean = false
) {
    data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val items = listOf(
        TabItem("Pay Cycles", Icons.Default.Refresh),
        TabItem("Income", Icons.Default.Add),
        TabItem("Expenses", Icons.Default.ShoppingCart),
        TabItem("Debt Planner", Icons.Default.Star),
        TabItem("Calendar", Icons.Default.DateRange),
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isWideScreen) Modifier else Modifier.horizontalScroll(rememberScrollState()))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .then(if (isWideScreen) Modifier.weight(1f) else Modifier)
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

@Composable
fun FoldableNavigationRail(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    data class NavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val items = listOf(
        NavItem("Cycles", Icons.Default.Refresh),
        NavItem("Income", Icons.Default.Add),
        NavItem("Expenses", Icons.Default.ShoppingCart),
        NavItem("Debt", Icons.Default.Star),
        NavItem("Calendar", Icons.Default.DateRange),
        NavItem("AI Hub", Icons.Default.AutoAwesome),
        NavItem("Settings", Icons.Default.Settings)
    )
    val haptic = LocalHapticFeedback.current

    NavigationRail(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "PayCycle Fold",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
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
        },
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = selectedTab == index
                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTabSelected(index)
                    },
                    icon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("nav_rail_$index")
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
    data class NavItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
    val items = listOf(
        NavItem("Cycles", Icons.Default.Refresh),
        NavItem("Income", Icons.Default.Add),
        NavItem("Expenses", Icons.Default.ShoppingCart),
        NavItem("Debt", Icons.Default.Star),
        NavItem("Calendar", Icons.Default.DateRange),
        NavItem("AI Hub", Icons.Default.AutoAwesome),
        NavItem("Settings", Icons.Default.Settings)
    )

    NavigationBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 0.dp),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        tonalElevation = 8.dp
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = selectedTab == index
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_bottom_$index")
            )
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
        val filters = customLabels ?: listOf("All Statuses", "â³ Pending", "âœ… Cleared")
        filters.forEachIndexed { index, label ->
            val selected = statusFilter == index
            FilterChip(
                selected = selected,
                onClick = { onStatusFilterChange(index) },
                label = { Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                modifier = Modifier.testTag("filter_chip_$index")
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
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    var isSyncingSheets by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableIntStateOf(0) } // 0 = All, 1 = Pending, 2 = Cleared
    var isCycleHealthExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var subSectionOrder by rememberSaveable { mutableStateOf(listOf("health", "cycles_list")) }
    var isSubSectionReorderActive by rememberSaveable { mutableStateOf(false) }

    val currentPeriod = remember(uiState.payPeriods, uiState.currentPayPeriodStart) {
        uiState.payPeriods.find { it.startDate == uiState.currentPayPeriodStart }
            ?: uiState.payPeriods.firstOrNull()
    }

    val activeAccSummary = uiState.accountBalances[uiState.activeAccountName]

    val currentCycleStats = remember(currentPeriod, activeAccSummary, uiState.activeAccountName) {
        if (currentPeriod == null && activeAccSummary == null) null
        else {
            val validTxs = currentPeriod?.transactions?.filter {
                it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted")
            } ?: emptyList()
            val incomeTxsSum = validTxs.filter { it.isIncome }.sumOf { it.amount }

            val startingCarryover = activeAccSummary?.carryoverFromPrevious ?: currentPeriod?.carryoverFromPrevious ?: 0.0
            val paycheck = if (activeAccSummary != null) activeAccSummary.payPeriodIncome else maxOf(currentPeriod?.totalIncome ?: 0.0, incomeTxsSum, currentPeriod?.paycheckAmount ?: 0.0)
            val clearedExpenses = activeAccSummary?.completedExpenses ?: validTxs.filter { it.isCompleted && !it.isIncome }.sumOf { it.amount }
            val pendingExpenses = activeAccSummary?.pendingExpenses ?: validTxs.filter { !it.isCompleted && !it.isIncome }.sumOf { it.amount }
            val totalExpenses = clearedExpenses + pendingExpenses
            val totalAvailable = startingCarryover + paycheck
            val safeToSpend = totalAvailable - totalExpenses
            val clearedBalance = startingCarryover + paycheck - clearedExpenses
            val progress = if (totalAvailable > 0) (totalExpenses / totalAvailable).coerceIn(0.0, 1.0).toFloat() else if (totalExpenses > 0) 1.0f else 0f

            object {
                val accountName = uiState.activeAccountName
                val startingCarryover = startingCarryover
                val paycheckAmount = paycheck
                val totalAvailable = totalAvailable
                val cleared = clearedExpenses
                val pending = pendingExpenses
                val total = totalExpenses
                val remaining = safeToSpend
                val clearedBalance = clearedBalance
                val progressRatio = progress
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp)
            .testTag("pay_cycles_list"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (isSubSectionReorderActive || subSectionOrder != listOf("health", "cycles_list")) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSubSectionReorderActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector = if (isSubSectionReorderActive) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isSubSectionReorderActive) "Sub-sections Unlocked â€¢ Move then Lock" else "Custom Sub-sections â€¢ Locked",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isSubSectionReorderActive) {
                            Button(
                                onClick = { isSubSectionReorderActive = false },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Lock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = { isSubSectionReorderActive = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text("Unlock", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        TextButton(
                            onClick = {
                                subSectionOrder = listOf("health", "cycles_list")
                                isSubSectionReorderActive = false
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Reset", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        subSectionOrder.forEachIndexed { index, subKey ->
            when (subKey) {
                "health" -> {
                    if (currentCycleStats != null) {
                        val isDark = uiState.isDarkMode
                        val isOverBudget = currentCycleStats.remaining < 0
                        val isNearLimit = !isOverBudget && currentCycleStats.progressRatio > 0.8f

                        val badgeColor = when {
                            isOverBudget -> Color(0xFFEF4444)
                            isNearLimit -> Color(0xFFF59E0B)
                            else -> Color(0xFF10B981)
                        }
                        val badgeText = when {
                            isOverBudget -> "ðŸ”´ Over Budget"
                            isNearLimit -> "ðŸŸ¡ Budget Nearing Limit"
                            else -> "ðŸŸ¢ Healthy Budget"
                        }

                        MovableSectionWrapper(
                            sectionKey = "cycle_health",
                            sectionTitle = "Current Cycle Health & Safe-to-Spend",
                            index = index,
                            totalSections = subSectionOrder.size,
                            isReordering = isSubSectionReorderActive,
                            onToggleReorder = { isSubSectionReorderActive = !isSubSectionReorderActive },
                            onMoveUp = {
                                if (index > 0) {
                                    val mutable = subSectionOrder.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index - 1]
                                    mutable[index - 1] = temp
                                    subSectionOrder = mutable
                                }
                            },
                            onMoveDown = {
                                if (index < subSectionOrder.size - 1) {
                                    val mutable = subSectionOrder.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index + 1]
                                    mutable[index + 1] = temp
                                    subSectionOrder = mutable
                                }
                            },
                            isSubSection = true
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("budget_health_card"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(if (isCycleHealthExpanded) 12.dp else 0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isCycleHealthExpanded = !isCycleHealthExpanded },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Current Cycle Health",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                            ) {
                                                Text(
                                                    text = uiState.activeAccountName,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (!isCycleHealthExpanded) {
                                                Text(
                                                    text = String.format(Locale.US, "$%.2f safe", currentCycleStats.remaining),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(20.dp),
                                                color = badgeColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = badgeText,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { isCycleHealthExpanded = !isCycleHealthExpanded },
                                                modifier = Modifier.size(24.dp).testTag("toggle_cycle_health_button")
                                            ) {
                                                Icon(
                                                    imageVector = if (isCycleHealthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                    contentDescription = if (isCycleHealthExpanded) "Collapse" else "Expand",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (isCycleHealthExpanded) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Safe-to-Spend Balance",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "$%.2f", currentCycleStats.remaining),
                                                    fontSize = 24.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (isOverBudget) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (currentCycleStats.startingCarryover != 0.0) {
                                                    Text(
                                                        text = String.format(Locale.US, "Starting: $%.2f â€¢ Total Avail: $%.2f", currentCycleStats.startingCarryover, currentCycleStats.totalAvailable),
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Commitment Ratio",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "%.0f%% spent/committed", currentCycleStats.progressRatio * 100),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor
                                                )
                                            }
                                        }

                                        LinearProgressIndicator(
                                            progress = { currentCycleStats.progressRatio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = badgeColor,
                                            trackColor = if (uiState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            if (currentCycleStats.startingCarryover != 0.0) {
                                                Surface(
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = if (uiState.isDarkMode) Color(0xFF0F172A) else Color.White,
                                                    border = BorderStroke(1.dp, if (uiState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(8.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Text("Starting", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text(
                                                            String.format(Locale.US, "$%.2f", currentCycleStats.startingCarryover),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (uiState.isDarkMode) Color(0xFF0F172A) else Color.White,
                                                border = BorderStroke(1.dp, if (uiState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("Total Income", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        String.format(Locale.US, "$%.2f", currentCycleStats.paycheckAmount),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (uiState.isDarkMode) Color(0xFF0F172A) else Color.White,
                                                border = BorderStroke(1.dp, if (uiState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("Cleared Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        String.format(Locale.US, "$%.2f", currentCycleStats.cleared),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }

                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (uiState.isDarkMode) Color(0xFF0F172A) else Color.White,
                                                border = BorderStroke(1.dp, if (uiState.isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0))
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(8.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Text("Pending Bills", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(
                                                        String.format(Locale.US, "$%.2f", currentCycleStats.pending),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFF59E0B)
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
                "cycles_list" -> {
                    MovableSectionWrapper(
                        sectionKey = "cycles_timeline_list",
                        sectionTitle = "Pay Cycles List",
                        index = index,
                        totalSections = subSectionOrder.size,
                        isReordering = isSubSectionReorderActive,
                        onToggleReorder = { isSubSectionReorderActive = !isSubSectionReorderActive },
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = subSectionOrder.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index - 1]
                                mutable[index - 1] = temp
                                subSectionOrder = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < subSectionOrder.size - 1) {
                                val mutable = subSectionOrder.toMutableList()
                                val temp = mutable[index]
                                mutable[index] = mutable[index + 1]
                                mutable[index + 1] = temp
                                subSectionOrder = mutable
                            }
                        },
                        isSubSection = true
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        Text("ðŸ“…", fontSize = 48.sp)
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
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                    ) {
                                        Text(
                                            text = if (isWideScreen) "âœ¨ Side-by-Side (${uiState.payPeriods.size} Cycles)" else "${uiState.payPeriods.size} Cycles",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                                    val canUseTwoColumns = (isWideScreen || maxWidth >= 540.dp) && uiState.payPeriods.size > 1
                                    val colSpacing = 16.dp
                                    if (canUseTwoColumns) {
                                        val today = LocalDate.now()
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                                            onToggleCompleted = onToggleCompleted,
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
                                                                onToggleCompleted = onToggleCompleted,
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
                                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                                    onToggleCompleted = onToggleCompleted,
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
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    isWideScreen: Boolean = false
) {
    val today = remember { LocalDate.now() }
    val isNextCycle = remember(period.startDate, isCurrentCycle, today) {
        !isCurrentCycle && (period.startDate.isAfter(today) || period.startDate.isEqual(today))
    }
    var expanded by remember(period.payday, isCurrentCycle, isWideScreen) {
        mutableStateOf(isCurrentCycle || isWideScreen || isNextCycle)
    }
    var selectedAccountFilterInCycle by remember(period.payday) { mutableStateOf<String?>(null) }

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.background.luminance() < 0.5f

    val df = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val dfFull = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }

    val pct = period.spentPercentage

    val accountsInThisCycle = remember(period.transactions, period.paycheckAmount, period.otherIncome) {
        val validTx = period.transactions.filter {
            it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted")
        }
        val accountsMap = linkedMapOf<String, MutableList<TransactionEntity>>()
        for (tx in validTx) {
            val acc = if (tx.accountName.isBlank() || tx.accountName.equals("Primary", ignoreCase = true)) "Primary Account" else tx.accountName
            accountsMap.getOrPut(acc) { mutableListOf() }.add(tx)
        }

        accountsMap.map { (acc, txs) ->
            val inc = txs.filter { it.isIncome }.sumOf { it.amount }
            val exp = txs.filter { !it.isIncome }.sumOf { it.amount }
            val compExp = txs.filter { !it.isIncome && it.isCompleted }.sumOf { it.amount }
            val pendExp = txs.filter { !it.isIncome && !it.isCompleted }.sumOf { it.amount }
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                text = "${period.startDate.format(df)} â€“ ${period.endDate.format(df)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (isCurrentCycle) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Current Cycle",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        } else if (isNextCycle) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                Text(
                                    text = "Next Cycle",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
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
                                shape = RoundedCornerShape(6.dp),
                                color = if (activeAccountName == "All Accounts") MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                            ) {
                                Text(
                                    text = if (activeAccountName == "All Accounts") "All Accounts" else activeAccountName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeAccountName == "All Accounts") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
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
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
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
                                Text(
                                    text = "Carried Over from Previous Cycle:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Text(
                                    text = String.format(Locale.US, "%s$%,.2f", if (period.carryoverFromPrevious > 0) "+" else "", period.carryoverFromPrevious),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (period.carryoverFromPrevious >= 0) Color(0xFF059669) else Color(0xFFDC2626),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { pct.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = when {
                                    pct > 0.9f -> Color(0xFFDC2626)
                                    pct > 0.7f -> Color(0xFFD97706)
                                    else -> Color(0xFF059669)
                                },
                                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )

                            // Cycle Breakdown Metrics Row
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
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
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
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", period.totalExpenses),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.2f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "INCOME",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.US, "+$%,.2f", period.totalIncome),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF059669),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (period.remainingBalance >= 0) Color(0xFF10B981).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, if (period.remainingBalance >= 0) Color(0xFF10B981).copy(alpha = 0.3f) else Color(0xFFEF4444).copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "REMAINING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (period.remainingBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = String.format(Locale.US, "$%,.2f", period.remainingBalance),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (period.remainingBalance >= 0) Color(0xFF059669) else Color(0xFFDC2626),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (accountsInThisCycle.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
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
                                            text = "Show All Accounts âœ•",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(accountsInThisCycle, key = { it.accountName }) { accSummary ->
                                    val isFiltered = selectedAccountFilterInCycle == accSummary.accountName
                                    val accStyle = getAccountColorStyle(accSummary.accountName, isDark)

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isFiltered) accStyle.containerColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        border = BorderStroke(
                                            width = if (isFiltered) 2.dp else 1.dp,
                                            color = if (isFiltered) accStyle.borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                                        ),
                                        modifier = Modifier
                                            .clickable {
                                                selectedAccountFilterInCycle = if (isFiltered) null else accSummary.accountName
                                            }
                                            .testTag("cycle_target_account_${accSummary.accountName}")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .width(135.dp)
                                                .padding(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = accSummary.accountName,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isFiltered) accStyle.contentColor else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = accStyle.borderColor.copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "${accSummary.transactionCount}",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = accStyle.borderColor,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }

                                            if (accSummary.totalExpenses > 0.0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Expenses:",
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = String.format(Locale.US, "-$%,.2f", accSummary.totalExpenses),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFDC2626)
                                                    )
                                                }
                                            }

                                            if (accSummary.totalIncome > 0.0) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "Income:",
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = String.format(Locale.US, "+$%,.2f", accSummary.totalIncome),
                                                        fontSize = 10.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF059669)
                                                    )
                                                }
                                            }

                                            // Cleared vs Pending
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (accSummary.completedExpenses > 0.0) {
                                                    Text(
                                                        text = String.format(Locale.US, "âœ“ $%,.0f paid", accSummary.completedExpenses),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF047857)
                                                    )
                                                }
                                                if (accSummary.pendingExpenses > 0.0) {
                                                    Text(
                                                        text = String.format(Locale.US, "â³ $%,.0f left", accSummary.pendingExpenses),
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFFD97706)
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
                                1 -> !tx.isCompleted
                                2 -> tx.isCompleted
                                else -> true
                            }
                            if (!matchesStatus) return@filter false

                            val txAcc = if (tx.accountName.isBlank() || tx.accountName.equals("Primary", ignoreCase = true)) "Primary Account" else tx.accountName
                            if (selectedAccountFilterInCycle != null && !txAcc.equals(selectedAccountFilterInCycle, ignoreCase = true)) {
                                return@filter false
                            }

                            true
                        }.sortedWith(compareBy({ it.isCompleted }, { it.dateMillis }))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SCHEDULED ITEMS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                letterSpacing = 0.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${cycleTransactions.size} items",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        if (cycleTransactions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "âš¡ No bills or expenses scheduled in this cycle.\nTap below to add an item.",
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (tx in cycleTransactions) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    ) {
                                        TransactionItemRow(
                                            transaction = tx,
                                            onDelete = { onDeleteTransaction(tx) },
                                            onEdit = { onEditTransaction(tx) },
                                            onToggleCompleted = { onToggleCompleted(tx) },
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Actions footer inside card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp)
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
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
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
    onToggleCompleted: (() -> Unit)? = null,
    showDateChip: Boolean = true,
    modifier: Modifier = Modifier
) {
    val df = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    val date = remember(transaction.dateMillis) { DateUtils.millisToLocalDate(transaction.dateMillis) }

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
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .testTag("transaction_row_${transaction.id}"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Main Top Line: [Checkbox] [Icon] [Title Text] ... [Amount]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (onToggleCompleted != null) {
                    IconButton(
                        onClick = onToggleCompleted,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("toggle_completed_tx_${transaction.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .border(
                                    width = 1.5.dp,
                                    color = if (transaction.isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    shape = CircleShape
                                )
                                .background(
                                    color = if (transaction.isCompleted) Color(0xFF10B981) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (transaction.isCompleted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Completed / Paid",
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
                        Text(if (transaction.isIncome) "ðŸ’°" else "ðŸ’¸", fontSize = 12.sp)
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

                Text(
                    text = (if (transaction.isIncome) "+" else "-") + String.format(Locale.US, "$%,.2f", transaction.amount),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (transaction.isIncome) Color(0xFF10B981) else if (transaction.isCompleted) Color(0xFF047857) else Color(0xFFDC2626),
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
                    if (showDateChip) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = date.format(df),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (transaction.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "âœ“ Cleared",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    if (transaction.isAutoPay) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                        ) {
                            Text(
                                text = "âš¡ Auto Pay",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    val accountTag = if (transaction.accountName.isBlank() || transaction.accountName.equals("Primary", ignoreCase = true)) "Primary Account" else transaction.accountName
                    val accountStyle = getAccountColorStyle(accountTag, isSystemInDarkTheme())
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accountStyle.containerColor,
                        border = BorderStroke(1.dp, accountStyle.borderColor)
                    ) {
                        Text(
                            text = accountTag,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accountStyle.contentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onSyncCsvDrive: (((String) -> Unit) -> Unit)? = null,
    isWideScreen: Boolean = false
) {
    var isSyncingSheets by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val yearMonth = uiState.selectedMonth
    val anchor = uiState.anchorPayday

    // Core Calendar Logic
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()
    val firstDayOffset = (monthStart.dayOfWeek.value - 1) % 7 // Monday = 0

    val calendarDays = remember(yearMonth) {
        val days = mutableListOf<LocalDate>()
        // Back padding from previous month
        for (i in firstDayOffset downTo 1) {
            days.add(monthStart.minusDays(i.toLong()))
        }
        // Month days
        for (i in 1..yearMonth.lengthOfMonth()) {
            days.add(monthStart.plusDays((i - 1).toLong()))
        }
        // Front padding for next month
        while (days.size % 7 != 0) {
            days.add(monthEnd.plusDays((days.size - firstDayOffset - yearMonth.lengthOfMonth() + 1).toLong()))
        }
        days
    }

    var selectedDay by remember { mutableStateOf(LocalDate.now()) }

    // Map transactions by date across the visible calendar
    val transactionsByDateMap = remember(calendarDays, uiState.allTransactions, uiState.payPeriods, uiState.activeAccountName, anchor) {
        val map = mutableMapOf<LocalDate, MutableList<TransactionEntity>>()

        val rawActiveTxList = if (uiState.activeAccountName == "All Accounts") {
            uiState.allTransactions
        } else {
            uiState.allTransactions.filter { it.accountName == uiState.activeAccountName }
        }

        val activeTxList = rawActiveTxList.filter { it.amount > 0.0 && !it.title.contains("Cancelled") && !it.title.contains("Deleted") }

        activeTxList.forEach { tx ->
            val date = DateUtils.millisToLocalDate(tx.dateMillis)
            map.getOrPut(date) { mutableListOf() }.add(tx)
        }
        map
    }

    // Filtered transaction list for the selected day
    val dayTransactions = remember(selectedDay, transactionsByDateMap) {
        (transactionsByDateMap[selectedDay] ?: emptyList())
            .distinctBy { it.id }
            .sortedWith(compareBy({ it.isCompleted }, { it.dateMillis }))
    }

    // Calculate overall month summary for visible current-month days
    val monthIncomesSum = remember(calendarDays, transactionsByDateMap, yearMonth) {
        calendarDays.filter { it.month == yearMonth.month && it.year == yearMonth.year }
            .flatMap { transactionsByDateMap[it] ?: emptyList() }
            .distinctBy { it.id }
            .filter { it.isIncome }
            .sumOf { it.amount }
    }

    val monthExpensesSum = remember(calendarDays, transactionsByDateMap, yearMonth) {
        calendarDays.filter { it.month == yearMonth.month && it.year == yearMonth.year }
            .flatMap { transactionsByDateMap[it] ?: emptyList() }
            .distinctBy { it.id }
            .filter { !it.isIncome }
            .sumOf { it.amount }
    }

    val topBannerContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

            // Monthly Summary & Legend Banner
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                text = String.format(Locale.US, "$%,.0f", monthIncomesSum),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF059669)
                            )
                        }
                        Text("â€¢", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Column {
                            Text("EXPENSES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                            Text(
                                text = String.format(Locale.US, "$%,.0f", monthExpensesSum),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                        Text("â€¢", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        Column {
                            val net = monthIncomesSum - monthExpensesSum
                            Text("NET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (net >= 0) Color(0xFF059669) else Color(0xFFDC2626))
                            Text(
                                text = (if (net >= 0) "+" else "") + String.format(Locale.US, "$%,.0f", net),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (net >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                            Text("+$ Inc", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFEF4444).copy(alpha = 0.2f)) {
                            Text("-$ Exp", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }

    val calendarGridContent: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Calendar Grid Header (Weeks)
            Row(modifier = Modifier.fillMaxWidth()) {
                val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
                weekdays.forEach { day ->
                    Text(
                        text = day,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
                            val isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year

                            // Find corresponding Pay Period to tint background
                            val payPeriodIndex = uiState.payPeriods.indexOfFirst {
                                !it.startDate.isAfter(date) && !it.endDate.isBefore(date)
                            }

                            val isDark = uiState.isDarkMode
                            val cellBg = when {
                                date == selectedDay -> MaterialTheme.colorScheme.primaryContainer
                                !isCurrentMonth -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                payPeriodIndex == 0 -> if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color(0xFFEFF6FF)
                                payPeriodIndex == 1 -> if (isDark) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFECFDF5)
                                payPeriodIndex == 2 -> if (isDark) Color(0xFF78350F).copy(alpha = 0.6f) else Color(0xFFFFFBEB)
                                payPeriodIndex == 3 -> if (isDark) Color(0xFF581C87).copy(alpha = 0.6f) else Color(0xFFFDF4FF)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            }

                            val isToday = date == LocalDate.now()
                            val isPayday = DateUtils.getPaydayOnOrBefore(date, anchor) == date

                            val dateTxs = transactionsByDateMap[date] ?: emptyList()
                            val dateIncome = dateTxs.filter { it.isIncome }.sumOf { it.amount }
                            val dateExpense = dateTxs.filter { !it.isIncome }.sumOf { it.amount }

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
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrentMonth) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                date == selectedDay -> MaterialTheme.colorScheme.onPrimaryContainer
                                                !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                                else -> MaterialTheme.colorScheme.onSurface
                                            }
                                        )

                                        if (isPayday) {
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("ðŸ’°", fontSize = 9.sp)
                                        }
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        if (dateIncome > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (date == selectedDay)
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                                else
                                                    Color(0xFF10B981).copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "+${formatCalendarAmount(dateIncome)}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (date == selectedDay) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF047857),
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (dateExpense > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = if (date == selectedDay)
                                                    MaterialTheme.colorScheme.error.copy(alpha = 0.25f)
                                                else
                                                    Color(0xFFEF4444).copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "-${formatCalendarAmount(dateExpense)}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (date == selectedDay) MaterialTheme.colorScheme.onErrorContainer else Color(0xFFB91C1C),
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
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

    val dailyDetailCardContent: @Composable () -> Unit = {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isWideScreen) 0.dp else 80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header with Date, Financial Pill Summary & Add Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedDay.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.US)),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val dayIncomes = dayTransactions.filter { it.isIncome }.sumOf { it.amount }
                        val dayExpenses = dayTransactions.filter { !it.isIncome }.sumOf { it.amount }
                        if (dayIncomes > 0 || dayExpenses > 0) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                if (dayIncomes > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "+$%,.2f In", dayIncomes),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (dayExpenses > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = String.format(Locale.US, "-$%,.2f Out", dayExpenses),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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

                if (dayTransactions.isEmpty()) {
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
                            Text("ðŸ›¡ï¸", fontSize = 28.sp)
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
                        for (tx in dayTransactions) {
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
                                    onToggleCompleted = { onToggleCompleted(tx) },
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        topBannerContent()

        if (isWideScreen) {
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
    onExportToGoogleSheets: (((String) -> Unit) -> Unit)? = null,
    onExportCsv: suspend () -> String = { "" },
    onImportCsv: suspend (String) -> Boolean = { false },
    onExportWebCompanionToGoogleDrive: (suspend (String) -> Pair<Boolean, String>)? = null,
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
    var driveSyncExpanded by remember { mutableStateOf(false) }
    var sheetsSyncExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }
    var dangerZoneExpanded by remember { mutableStateOf(false) }
    val localContext = LocalContext.current
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        com.example.notification.BillReminderScheduler.sendTestNotification(
            localContext,
            title = "Electric Bill Reminder",
            amount = 84.50
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
                        text = "Simply log in with your Google Email below. No Client ID or Client Secret is required!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = loginEmailInput,
                        onValueChange = { loginEmailInput = it },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("86brancan@gmail.com") },
                        modifier = Modifier.fillMaxWidth().testTag("email_login_dialog_input"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val email = loginEmailInput.trim()
                                if (email.isNotEmpty()) {
                                    onConnectCustomSyncEmail(email)
                                    showAuthorizeDriveDialog = false
                                    onManualSyncDrive()
                                    onSyncCsvDrive?.invoke {}
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("save_email_login_button")
                        ) {
                            Text("Log In & Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                showAuthorizeDriveDialog = false
                                onConnectGoogleDrive()
                            },
                            modifier = Modifier.weight(1f).testTag("sign_in_google_button")
                        ) {
                            Text("Sign In with Google", fontSize = 11.sp)
                        }
                    }

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
                                text = if (showAdvancedOAuth) "Hide Advanced Credentials (Client ID / Secret)" else "Show Advanced Credentials (Client ID / Secret)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (showAdvancedOAuth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = clientIdInput,
                                onValueChange = { clientIdInput = it },
                                label = { Text("Client ID (Optional)") },
                                placeholder = { Text("123456789-xyz.apps.googleusercontent.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = clientSecretInput,
                                onValueChange = { clientSecretInput = it },
                                label = { Text("Client Secret (Optional)") },
                                placeholder = { Text("GOCSPX-...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = refreshTokenInput,
                                onValueChange = { refreshTokenInput = it },
                                label = { Text("Refresh Token or Auth Code (Optional)") },
                                placeholder = { Text("1//04... or 4/0A...") },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                singleLine = false
                            )

                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse("https://developers.google.com/oauthplayground/#step1&scopes=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fdrive.file%20https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fspreadsheets")
                                        )
                                        localContext.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Get OAuth Token/Code (OAuth Playground)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedTextField(
                                value = driveTokenInput,
                                onValueChange = { driveTokenInput = it },
                                label = { Text("Direct OAuth Access Token (Optional)") },
                                placeholder = { Text("ya29.a0A...") },
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
                                text = "âœ“ Active Account: ${uiState.driveAccountEmail ?: "Connected"}",
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

    val cardRemindersAndThemes: @Composable () -> Unit = {
        // --- Card: Expense Notification Reminders & Themes ---
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
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
                                    text = "Automated alerts before expenses & bills are due",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    val scheduledCount = remember(uiState.allTransactions, uiState.plannedExpenses) {
                        val txReminders = uiState.allTransactions.count { !it.isIncome && !it.isCompleted && it.reminderDaysBefore >= 0 }
                        val billReminders = uiState.plannedExpenses.count { it.reminderDaysBefore >= 0 }
                        txReminders + billReminders
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Scheduled Reminders Active",
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
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("send_test_notification_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Test Alert Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                            Text(text = "ðŸŽ¨", fontSize = 20.sp)
                                            Column {
                                                Text(
                                                    text = "Color Theory Triadic Spectrum",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "60-30-10 Visual Balance â€¢ Triadic Harmony",
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
                                            text = "ðŸ•¹ï¸",
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
                                            text = if (is8BitSelected) "ACTIVE ðŸŽ®" else "ENABLE 8-BIT",
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
                                            text = "ðŸ“–",
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
                                            text = if (isEReaderSelected) "ACTIVE ðŸ“–" else "SELECT",
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
                                text = "Select an icon style below. Your device launcher and home screen will update to display the chosen icon style.",
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
                                                Toast.makeText(
                                                    localContext,
                                                    "App icon set to ${iconOption.title}. Home screen will update shortly!",
                                                    Toast.LENGTH_SHORT
                                                ).show()
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
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Cloud Sync Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Google Drive Cloud Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

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
                    }

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
                        }
                    }
                }
            }

            // --- Google Sheets & Google Drive CSV Sync Card ---
            var showCsvExportDialog by remember { mutableStateOf(false) }
            var showCsvImportDialog by remember { mutableStateOf(false) }
            var csvExportData by remember { mutableStateOf("") }
            var csvImportInput by remember { mutableStateOf("") }
            var csvStatusMsg by remember { mutableStateOf("") }
            var isSyncingCsv by remember { mutableStateOf(false) }
            var isExportingToSheets by remember { mutableStateOf(false) }
            val csvCoroutineScope = rememberCoroutineScope()

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
                        modifier = Modifier.fillMaxWidth(),
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
                                Text("Export: 'Expenses & Budget Export Sheet' | Import: 'Expenses & Budget Import Sheet'", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

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
                    }

                    Text(
                        text = "Export income and expenses directly into a Google Sheet saved on your Google Drive. You can also sync remote changes back into the app on demand.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            isExportingToSheets = true
                            onExportToGoogleSheets?.invoke { msg ->
                                csvStatusMsg = msg
                                isExportingToSheets = false
                                if (!msg.startsWith("Successfully") && (msg.contains("failed") || msg.contains("authorize") || msg.contains("OAuth") || msg.contains("not connected") || msg.contains("Could not upload") || msg.contains("permissions"))) {
                                    showAuthorizeDriveDialog = true
                                }
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
                                if (isExportingToSheets) "Exporting to 'Expenses & Budget Export Sheet'..." else "Export to 'Expenses & Budget Export Sheet'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isSyncingCsv = true
                            csvCoroutineScope.launch {
                                csvExportData = onExportCsv()
                            }
                            onSyncCsvDrive?.invoke { msg ->
                                csvStatusMsg = msg
                                isSyncingCsv = false
                                if (!msg.startsWith("Synced") && !msg.startsWith("Created") && (msg.contains("failed") || msg.contains("authorize") || msg.contains("OAuth") || msg.contains("not connected"))) {
                                    showAuthorizeDriveDialog = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth().testTag("sync_google_sheets_csv_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(
                                if (isSyncingCsv) "Syncing with Google Sheets..." else "Sync / Import from 'Expenses & Budget Import Sheet'",
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
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://drive.google.com/drive/my-drive"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f).testTag("open_drive_button")
                        ) {
                            Text("My Drive", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                csvCoroutineScope.launch {
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
                                                val file = java.io.File(context.filesDir, "expenses_google_sheets_sync.csv")
                                                file.writeText(csvExportData, Charsets.UTF_8)
                                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                                    context,
                                                    "com.example.fileprovider",
                                                    file
                                                )
                                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "text/csv"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    putExtra(android.content.Intent.EXTRA_SUBJECT, "expenses_google_sheets_sync.csv")
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share or Save CSV File"))
                                            } catch (e: Exception) {
                                                android.widget.Toast.makeText(context, "CSV saved to app storage", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Text("Share / Save File")
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
                        AlertDialog(
                            onDismissRequest = { showCsvImportDialog = false },
                            title = { Text("Import Google Sheets CSV", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Paste CSV content copied from Google Sheets or CSV file:", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = csvImportInput,
                                        onValueChange = { csvImportInput = it },
                                        placeholder = { Text("Date,Title,Amount,Category,Type,Account,Completed\n2026-07-29,\"Grocery Store\",45.50,\"Groceries\",\"Expense\",\"Primary Account\",\"Yes\"") },
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
                                        csvCoroutineScope.launch {
                                            val ok = onImportCsv(csvImportInput)
                                            showCsvImportDialog = false
                                            if (ok) {
                                                csvStatusMsg = "CSV imported & synced with local database!"
                                            } else {
                                                csvStatusMsg = "Failed to parse CSV lines. Check format."
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("ðŸ’»", fontSize = 20.sp)
                    Text("PC & Desktop Web Companion", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

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
                                    
                                    // Save file as "PayCycle Budget - Web Companion App.html" for direct overwrites and clean sharing
                                    val file = java.io.File(context.filesDir, "PayCycle Budget - Web Companion App.html")
                                    file.writeText(htmlContent, Charsets.UTF_8)
                                    try {
                                        val legacyFile = java.io.File(context.filesDir, "PayCycle_Web_Companion.html")
                                        legacyFile.writeText(htmlContent, Charsets.UTF_8)
                                    } catch (_: Exception) {}

                                    // Overwrite previous "PayCycle Budget - Web Companion App" on Google Drive if connected
                                    if (onExportWebCompanionToGoogleDrive != null) {
                                        val (driveSuccess, driveStatus) = onExportWebCompanionToGoogleDrive(htmlContent)
                                        if (driveSuccess) {
                                            toastMsg = driveStatus
                                        }
                                    }

                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        context,
                                        "com.example.fileprovider",
                                        file
                                    )
                                    val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/html"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        putExtra(android.content.Intent.EXTRA_TITLE, "PayCycle Budget - Web Companion App")
                                        putExtra(android.content.Intent.EXTRA_SUBJECT, "PayCycle Budget - Web Companion App")
                                        putExtra(android.content.Intent.EXTRA_TEXT, "Open this PayCycle Web Companion file on your PC or desktop browser to manage your budget and debt snowball.")
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(shareIntent, "Share or Save 'PayCycle Budget - Web Companion App'"))
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

    val cardBackup: @Composable () -> Unit = {
        // --- Card 5: Data Portability & Backup ---
        Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                var exportedJson by remember { mutableStateOf("") }
                var showExportDialog by remember { mutableStateOf(false) }
                var showImportDialog by remember { mutableStateOf(false) }
                var importInputText by remember { mutableStateOf("") }
                var importErrorMsg by remember { mutableStateOf("") }
                var isImportingProgress by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Data Backup Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Data Portability & Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

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
                        AlertDialog(
                            onDismissRequest = { if (!isImportingProgress) showImportDialog = false },
                            title = { Text("Restore From Backup", fontWeight = FontWeight.Bold) },
                            text = {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Paste your exported JSON backup data below:", fontSize = 12.sp)
                                    OutlinedTextField(
                                        value = importInputText,
                                        onValueChange = {
                                            importInputText = it
                                            importErrorMsg = ""
                                        },
                                        placeholder = { Text("{ ... }") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp),
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
                                            importErrorMsg = "âŒ Please paste a valid backup string first."
                                            return@Button
                                        }
                                        isImportingProgress = true
                                        coroutineScope.launch {
                                            val success = onImportBackup(importInputText)
                                            isImportingProgress = false
                                            if (success) {
                                                showImportDialog = false
                                            } else {
                                                importErrorMsg = "âŒ Failed to restore backup. Please ensure the formatting is correct."
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Reset & Danger Zone",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                cardRemindersAndThemes()
                cardWebCompanion()
                cardDriveSync()
                cardBackup()
                cardDangerZone()
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
    onConfirm: (title: String, amount: Double, date: LocalDate, category: String, isIncome: Boolean, isRecurring: Boolean, accountName: String, isAutoPay: Boolean, reminderDaysBefore: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf(preselectedDate.toString()) }
    var selectedCategory by remember { mutableStateOf("Groceries") }
    var isIncome by remember { mutableStateOf(false) }
    var isRecurring by remember { mutableStateOf(false) }
    var isAutoPay by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf(if (initialAccountName == "All Accounts") (allAccounts.firstOrNull { it != "All Accounts" } ?: "Primary Account") else initialAccountName) }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var reminderDaysBefore by remember { mutableIntStateOf(1) }
    var reminderDropdownExpanded by remember { mutableStateOf(false) }

    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isIncome) "Add Income Source" else "Schedule Bill / Expense") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
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

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_title_input")
                )

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

                DatePickerTextField(
                    value = dateInput,
                    onValueChange = { dateInput = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    testTag = "tx_date_input",
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
                        Text("ðŸ”” Notification Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    0 -> "On Due Date (9:00 AM)"
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
                                1 to "1 Day Before Due Date",
                                0 to "On Due Date (9:00 AM)",
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
                    Text(
                        text = if (isIncome) "Recur bi-weekly (every 14 days)" else "Recur monthly on this day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                        text = "âš¡ Auto Pay enabled",
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
                        onConfirm(title, amt, dateParsed, if (isIncome) "Income" else "Expense", isIncome, isRecurring, selectedAccount, isAutoPay, if (isIncome) -1 else reminderDaysBefore)
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
    onSave: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    var title by remember { mutableStateOf(transaction.title) }
    var amount by remember { mutableStateOf(if (transaction.amount > 0) String.format(Locale.US, "%.2f", transaction.amount) else "") }
    val initialDate = remember(transaction.dateMillis) { DateUtils.millisToLocalDate(transaction.dateMillis) }
    var dateInput by remember { mutableStateOf(initialDate.toString()) }
    var selectedCategory by remember { mutableStateOf(transaction.category.ifEmpty { "Groceries" }) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }
    var isRecurring by remember { mutableStateOf(transaction.isRecurring) }
    var isAutoPay by remember { mutableStateOf(transaction.isAutoPay) }
    var selectedAccount by remember { mutableStateOf(if (transaction.accountName.isEmpty() || transaction.accountName == "All Accounts") (allAccounts.firstOrNull { it != "All Accounts" } ?: "Primary Account") else transaction.accountName) }
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
                modifier = Modifier.fillMaxWidth(),
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
                        Text("ðŸ”” Notification Reminder", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    0 -> "On Due Date (9:00 AM)"
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
                                1 to "1 Day Before Due Date",
                                0 to "On Due Date (9:00 AM)",
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
                        text = if (isIncome) "Recur bi-weekly (every 14 days)" else "Recur monthly on this day",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                        text = "âš¡ Auto Pay enabled",
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
                        val updatedTx = transaction.copy(
                            title = title,
                            amount = amt,
                            dateMillis = DateUtils.localDateToMillis(dateParsed),
                            category = if (isIncome) "Income" else "Expense",
                            isIncome = isIncome,
                            isRecurring = isRecurring,
                            isAutoPay = isAutoPay,
                            accountName = selectedAccount,
                            reminderDaysBefore = if (isIncome) -1 else reminderDaysBefore
                        )
                        onSave(updatedTx)
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
            payPeriodEnd = uiState.currentPayPeriodEnd
        )
    }

    val totalUpcomingAmount = upcomingItems.sumOf { it.amount }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("upcoming_expenses_widget"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                                            text = "Due $formattedDueDate â€¢ ${item.category} â€¢ ${item.accountName}",
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
                                        accountName = item.accountName.ifEmpty { "Primary Account" },
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
    isWideScreen: Boolean = false
) {
    var showAddAccountDialog by remember { mutableStateOf(false) }
    var newAccountName by remember { mutableStateOf("") }
    var newAccountInitialBalance by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    // Dialog state for Reordering
    var showReorderAccountsModal by remember { mutableStateOf(false) }

    // Dialog state for Editing / Deleting
    var showEditAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf("") }
    var editAccountName by remember { mutableStateOf("") }
    var editAccountBalanceStr by remember { mutableStateOf("") }
    var editErrorMsg by remember { mutableStateOf("") }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var carouselExpanded by remember { mutableStateOf(true) }
    var isReorderMode by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
        val payCycleTag = remember(uiState.currentPayPeriodStart, uiState.currentPayPeriodEnd) {
            "Pay Cycle: ${uiState.currentPayPeriodStart.format(dateFmt)} â€“ ${uiState.currentPayPeriodEnd.format(dateFmt)}"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { carouselExpanded = !carouselExpanded }
            ) {
                Icon(
                    imageVector = if (carouselExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (carouselExpanded) "Collapse accounts" else "Expand accounts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isWideScreen) "ACCOUNTS & LEDGERS (GRID)" else "ACCOUNTS & LEDGERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    letterSpacing = 0.5.sp
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = payCycleTag,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Manage / Edit Accounts button
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
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
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
                            color = MaterialTheme.colorScheme.onSecondaryContainer
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
                                fontWeight = FontWeight.Bold
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
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "+ Add",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { showAddAccountDialog = true }
                        .testTag("add_account_button")
                )
            }
        }

        if (carouselExpanded) {
            if (isWideScreen) {
                // Adaptive Compact Multi-Column Grid for Galaxy Z Fold / Landscape
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val colCount = if (maxWidth >= 1050.dp) 6 else if (maxWidth >= 750.dp) 5 else if (maxWidth >= 480.dp) 4 else 3
                    val chunkedAccounts = remember(uiState.allAccounts, colCount) { uiState.allAccounts.chunked(colCount) }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        chunkedAccounts.forEach { rowAccounts ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                rowAccounts.forEach { acc ->
                                    Box(modifier = Modifier.weight(1f)) {
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
                                            onMoveRight = { onMoveAccount?.invoke(acc, 1) }
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                                text = "â€¢ Position ${index + 1} of ${uiState.allAccounts.size}",
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
                        text = "ðŸ’¡ Updating this amount sets this account's starting balance so your PayCycle funds align with your real bank balance.",
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
            .width(if (isReorderMode) 120.dp else 108.dp)
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
        Column(modifier = Modifier.fillMaxWidth()) {
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
                    .padding(horizontal = if (isWideScreen) 5.dp else 6.dp, vertical = if (isWideScreen) 2.5.dp else 4.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 1.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = acc,
                        fontSize = if (isWideScreen) 10.5.sp else 11.sp,
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
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        if (isOverBudget) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning: Over Budget",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(if (isWideScreen) 10.dp else 12.dp)
                                    .testTag("account_warning_icon_$acc")
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isWideScreen) 10.dp else 12.dp)
                            )
                        }

                        if (acc != "All Accounts") {
                            IconButton(
                                onClick = { onEditAccount() },
                                modifier = Modifier
                                    .size(if (isWideScreen) 20.dp else 22.dp)
                                    .testTag("edit_account_icon_$acc")
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
                                    modifier = Modifier.size(if (isWideScreen) 11.dp else 12.dp)
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
                        uiState.allTransactions.count {
                            it.dateMillis >= pStart && it.dateMillis < pEnd && it.amount > 0.0 &&
                            (it.accountName.equals(acc, ignoreCase = true) || (acc == "Primary Account" && (it.accountName.isBlank() || it.accountName.equals("Primary", ignoreCase = true))))
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
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
                        text = String.format(Locale.US, "+$%,.0f â€¢ -$%,.0f", totalIncome, completedExpenses),
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
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
                        text = String.format(Locale.US, "+$%,.0f â€¢ -$%,.0f paid", totalIncome, completedExpenses),
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
                            text = if (inCycleTxCount > 0) "$inCycleTxCount in cycle" else "0 in cycle",
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
    var reminderDaysBefore by remember { mutableStateOf(1) } // Default 1 day before
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
                                label = { Text("ðŸ”” Bill Due Reminder") },
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
                                text = "âš¡ Auto Pay enabled",
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
                                    reminderDaysBefore = 1
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
                                                color = MaterialThexœÄY[oÛ6~÷¯àŒ°Oð=v°³nX°v+ê¶{,™NˆH¤FQIÝ4ÿ`Ï{°‡ý‘ý˜ý’êbË²›®»ð!‘y9ä9ç;Äð¸ÏÅÜ»&18›Çb‰=òŠ™l ÖÖšM—¨uI}ß Ñ,–ü5^µÑ½Õ²ã¶´ª]ã çèÙ‚,,.sÕÙ‹°Ý9Šj"O ú
KBôß&’-J94Àbeq&1…-Ý‡IÂ¤µw;Î^—6ÔÞïÕ¨ö–|”ÇéF5	«³æ¿þ2Œ4“•jKà~N?)…÷ºF~¥_	½ºV§s×?“û‹ã©|A—”$ºÌ>/”]µ®¹ Ÿ”ö|6;è–I½¤£¯ÀzÔ¾ú«´V>û°™‡ÍzÃï].Ç™O¯X ]ü!â}6Ç_®ÞÊfB`vErÚ›_ Bð]sÕš¬AKÔ·¾Ìâ–\XZ±„y«ÔM”Íp”RÓ-žÎXŠ<Ò6àƒÝòÁT7žðÕf,%gzêàÌò©w§¾‡o›øD’×>fŒ,œ!aItÓFz²¨r5H¶5Ðs%ºaAÉA?€V®È{âÉDŠFdØd‰c_©Lôn<my‚†’r¦MJ…©ˆIe|DÌ‘4±õz¬!øq¿Vq½Ã=HÞŸ}*ßüøŒúÑê‘ÝÞížÍ¯‚u€Þ—T©1*»ÚZ¢fnl,‘F¢]ýUéG’H¾ÅW­f«¬¢žÏ#òá2Ù°¹­Ž*ËJÜyÓR‹J³¬&_íÆC£±À#ÏÇQ„fž§<w’*ÎåÊÏòå[È¼<'LÏQò¯³¾äbQ7VL@³ÁF»ÑøÁâAÈ#|é“Æ2fèŠÈÝýqÚó3È9šKINÑÈÆâæ™œû³öùîÁ3±¨í)s’t‚VÛðùï˜ŠI†î®	+ˆU­72ö£V3Â·pˆ¨ÙFŸ?—Ç‰ˆ¸©£ï^”£®1é¹«TV#ørÛV0•üou?ºnw<tf®ZØ¯1wÁhY‡Û"ÔëšÓI¯fQé.±Y5vœ3ól×U”ð‡ˆ‘¯Â¿ÝsgÎH“×îh:OuyíŽGîp¼—×ÆîW	.ž *+‘äa±¨¸¥®p$š“uÿ;ü†VÏžŽN ¿‰9²Ü±®J¬¡9rí'„Ÿc;S×ÑäõÌÌ[—×‘Ùï™§€äâJ%îÕ¾þ­žJg“Á¨ëž JîhêtM]ñº–=>%”\ÇXgš¼ÚÓ³³®¶ÙLûÃn×9”(»…ìEÝU«½™X…’WE’{7O³ž3˜Mf'€ÙÀœôõ=Öt`=­Ç²Mg¦í±ú£ñÀÑ6©ž3ìÎÜc`–pÿÐèO+r©±62&¦íNt¥eÎœñÓÆ²®Û×FF·?Öz­úTj0žÎzºÈx¨¾¶\0kåùdª<³¹ºTÝ!Öƒ’Kì_0«A›Çp)f”hw…ª°¨Ÿýª0[?Y±¯À,Âž*7Xêèçè‚Éª;“ù£ú‘U½Õõ1!RËòúe}{Jû±ïgR„3¾¤‘ü>]õ"æì]Ï÷™óXx@¹•ßÈò›˜²ÕwŒÂY×0¡Ê#X½› Ë\±\ÂÅ÷qrô¹º¿,[Kxnƒn“u;ï5¼ÁŒ·ß`ò^ê%ÖoF;P-›wÙNmª¾m=zm¶L¨˜ÉH…ß”ì®§Šñ^¬â|™ÛØwK¸üWDE•aë†§*J	 Žµ–žüb’W
f§\þ%%q­²wÅ+Û©^AÊ¤º~ØS±ðÜ«,<ë¹÷"ùP¤<öZÛÖaÿö°]ý`¶¿¦~à3ï š-x¸àwìañö‰HÎ?·´.Ì½!¿Å·&uóÂªÄ¢N#Å­/$	jvŸÕâl~"$Ì±ƒ¦¬NÝÔ+*ò…j~e4­tÆÅJY'±àêºm™ÍI CŸñ¿~G ëÄÅävóµxNÈé™VÖWI½@½ªÜóÇµç±é-…PÐÚ=P‘Ö’ú2	†T¢o¶j”èÁXrá`ïF¡"k%kûU’·’jæwTqÉÑ3 _­•¼íÓÎf^–CdÞö*)oÕeþ]âµeþM©r¨Ò#&ÒŒ+ÍÉÚyú”qž•ˆÓ>#]ÚzÉ!²ãÝ¼ƒšÏ¾í@&Ñì ”Žª°ÿ  ÿÿ p¡zß