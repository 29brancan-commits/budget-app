package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryView(
    uiState: BudgetUiState,
    onAddPlannedBill: () -> Unit = {},
    onAddExpense: () -> Unit = {},
    onToggleCompleted: (TransactionEntity) -> Unit = {},
    onBack: (() -> Unit)? = null,
    isWideScreen: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedMonth by rememberSaveable { mutableStateOf(YearMonth.now()) }
    var activeAccountFilter by rememberSaveable { mutableStateOf("All Accounts") }
    var selectedViewTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Overview, 1 = Bills & Planned, 2 = All Expenses, 3 = Income
    var billStatusFilter by rememberSaveable { mutableStateOf("All") } // "All", "Due", "Paid", "AutoPay"
    var showMonthPickerModal by remember { mutableStateOf(false) }

    val report = remember(selectedMonth, uiState, activeAccountFilter) {
        MonthlySummaryCalculator.calculate(
            month = selectedMonth,
            uiState = uiState,
            activeAccountFilter = activeAccountFilter
        )
    }

    val currentRealMonth = remember { YearMonth.now() }
    val isCurrentMonth = selectedMonth == currentRealMonth

    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("monthly_summary_panel")
    ) {
        // Top Navigation Header for Monthly Summary
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            tonalElevation = 2.dp,
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(32.dp).testTag("monthly_summary_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(
                                    text = "Monthly Summary",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "AUTO-CALC",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Total planned expenses vs. monthly income",
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Month Switcher Controls
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { selectedMonth = selectedMonth.minusMonths(1) },
                            modifier = Modifier.size(32.dp).testTag("monthly_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrentMonth) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMonthPickerModal = true }
                                .testTag("monthly_picker_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (isCurrentMonth) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = selectedMonth.format(monthFormatter),
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrentMonth) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                    tint = if (isCurrentMonth) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                            modifier = Modifier.size(32.dp).testTag("monthly_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Month",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        if (!isCurrentMonth) {
                            IconButton(
                                onClick = { selectedMonth = currentRealMonth },
                                modifier = Modifier.size(32.dp).testTag("monthly_today_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Today,
                                    contentDescription = "Jump to Today",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Account Filter Strip if user has custom accounts
                val accounts = uiState.allAccounts
                if (accounts.size > 1) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filter Account:",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        accounts.forEach { acc ->
                            val isSelected = activeAccountFilter == acc
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { activeAccountFilter = acc }
                                    .testTag("monthly_filter_${acc.lowercase().replace(" ", "_")}")
                            ) {
                                Text(
                                    text = acc,
                                    fontSize = 9.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sub-tabs: Overview, Bills & Planned, All Expenses, Income Sources (Compact single line tabs)
        TabRow(
            selectedTabIndex = selectedViewTab,
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().testTag("monthly_subtabs")
        ) {
            Tab(
                selected = selectedViewTab == 0,
                onClick = { selectedViewTab = 0 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Overview", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("monthly_tab_overview")
            )
            Tab(
                selected = selectedViewTab == 1,
                onClick = { selectedViewTab = 1 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Bills", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                            Text("${report.bills.size}", fontSize = 8.5.sp)
                        }
                    }
                },
                modifier = Modifier.testTag("monthly_tab_bills")
            )
            Tab(
                selected = selectedViewTab == 2,
                onClick = { selectedViewTab = 2 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Expenses", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                },
                modifier = Modifier.testTag("monthly_tab_expenses")
            )
            Tab(
                selected = selectedViewTab == 3,
                onClick = { selectedViewTab = 3 },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text("Income", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Badge(containerColor = Color(0xFF10B981).copy(alpha = 0.2f)) {
                            Text("${report.incomeItems.size}", fontSize = 8.5.sp, color = Color(0xFF047857))
                        }
                    }
                },
                modifier = Modifier.testTag("monthly_tab_income")
            )
        }

        // Main Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedViewTab) {
                0 -> MonthlyOverviewContent(
                    report = report,
                    isCurrentMonth = isCurrentMonth,
                    isWideScreen = isWideScreen,
                    onNavigateToBills = { selectedViewTab = 1 },
                    onNavigateToIncome = { selectedViewTab = 3 },
                    onAddPlannedBill = onAddPlannedBill,
                    onAddExpense = onAddExpense
                )
                1 -> MonthlyBillsListContent(
                    report = report,
                    filter = billStatusFilter,
                    onFilterChange = { billStatusFilter = it },
                    dateFormatter = dateFormatter,
                    onToggleCompleted = onToggleCompleted,
                    onAddPlannedBill = onAddPlannedBill
                )
                2 -> MonthlyAllExpensesContent(
                    report = report,
                    dateFormatter = dateFormatter,
                    onToggleCompleted = onToggleCompleted,
                    onAddExpense = onAddExpense
                )
                3 -> MonthlyIncomeListContent(
                    report = report,
                    dateFormatter = dateFormatter
                )
            }
        }
    }

    // Modal dialog to jump to any month in 2026/2027
    if (showMonthPickerModal) {
        AlertDialog(
            onDismissRequest = { showMonthPickerModal = false },
            title = { Text("Select Month", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val availableMonths = remember {
                        (-6..12).map { offset -> YearMonth.now().plusMonths(offset.toLong()) }
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(availableMonths) { m ->
                            val isSelected = m == selectedMonth
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedMonth = m
                                        showMonthPickerModal = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = m.format(monthFormatter),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (m == currentRealMonth) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "CURRENT",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPickerModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun MonthlyOverviewContent(
    report: MonthlySummaryReport,
    isCurrentMonth: Boolean,
    isWideScreen: Boolean,
    onNavigateToBills: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onAddPlannedBill: () -> Unit,
    onAddExpense: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isWideScreen) {
            // Responsive 2-column layout on wide screens to eliminate wide whitespace margins
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthlyComparisonHeroCard(report = report, modifier = Modifier.weight(1.1f))
                    MonthlyCommitmentProgressCard(report = report, isCurrentMonth = isCurrentMonth, modifier = Modifier.weight(0.9f))
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthlyPaidVsRemainingCard(report = report, modifier = Modifier.weight(1f))
                    MonthlyCategoryDistributionCard(categories = report.categories, totalExpenses = report.totalPlannedExpenses, modifier = Modifier.weight(1f))
                }
            }
        } else {
            // Compact mobile layout with dense cards and reduced whitespace
            item {
                MonthlyComparisonHeroCard(report = report)
            }
            item {
                MonthlyCommitmentProgressCard(report = report, isCurrentMonth = isCurrentMonth)
            }
            item {
                MonthlyPaidVsRemainingCard(report = report)
            }
            item {
                MonthlyCategoryDistributionCard(categories = report.categories, totalExpenses = report.totalPlannedExpenses)
            }
        }

        // Quick Actions Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToBills,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("monthly_manage_bills_button")
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View All Bills", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAddPlannedBill,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(36.dp).testTag("monthly_add_bill_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Planned Bill", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
fun MonthlyPaidVsRemainingCard(
    report: MonthlySummaryReport,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_paid_vs_remaining_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Status & Fulfillment",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${report.bills.count { it.isPaid }} of ${report.bills.size} items paid",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dual segment progress bar
            val paidFraction = if (report.totalPlannedExpenses > 0.0) {
                (report.totalPaidSoFar / report.totalPlannedExpenses).toFloat().coerceIn(0f, 1f)
            } else 0f

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(paidFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF10B981))
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Paid: \$${String.format(Locale.US, "%,.2f", report.totalPaidSoFar)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Text(
                        text = "Due: \$${String.format(Locale.US, "%,.2f", report.totalRemainingDue)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyComparisonHeroCard(
    report: MonthlySummaryReport,
    modifier: Modifier = Modifier
) {
    val isSurplus = !report.isDeficit
    val surplusColor = if (isSurplus) Color(0xFF059669) else MaterialTheme.colorScheme.error
    val surplusBgColor = if (isSurplus) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)

    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_hero_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Net Difference Callout Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = surplusBgColor,
                border = BorderStroke(1.dp, surplusColor.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = if (isSurplus) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = surplusColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isSurplus) "PROJECTED NET SURPLUS" else "PROJECTED DEFICIT ALERT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = surplusColor,
                                letterSpacing = 0.3.sp
                            )
                        }
                        Text(
                            text = (if (report.netSurplus >= 0) "+\$" else "-\$") +
                                    String.format(Locale.US, "%,.2f", Math.abs(report.netSurplus)),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = surplusColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = surplusColor.copy(alpha = 0.18f)
                    ) {
                        Text(
                            text = if (isSurplus) "${String.format(Locale.US, "%.0f", 100f - report.commitmentPercentage)}% Free Buffer" else "Over Budget",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = surplusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Income vs Planned Expenses 2-Column Split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Income Column
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = CircleShape, color = Color(0xFF10B981).copy(alpha = 0.2f), modifier = Modifier.size(16.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color(0xFF047857))
                                }
                            }
                            Text("Total Income", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "\$${String.format(Locale.US, "%,.2f", report.totalIncome)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = "${report.incomeItems.size} deposit(s) projected",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Planned Expenses Column
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f), modifier = Modifier.size(16.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("Planned Bills", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "\$${String.format(Locale.US, "%,.2f", report.totalPlannedExpenses)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "${report.bills.size} bill(s) & expense(s)",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonthlyCommitmentProgressCard(
    report: MonthlySummaryReport,
    isCurrentMonth: Boolean,
    modifier: Modifier = Modifier
) {
    val ratio = report.commitmentPercentage / 100f
    val clampedRatio = ratio.coerceIn(0f, 1f)
    val barColor = when {
        ratio > 1.0f -> MaterialTheme.colorScheme.error
        ratio > 0.85f -> Color(0xFFF59E0B) // amber
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_commitment_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income Commitment Ratio",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${String.format(Locale.US, "%.1f", report.commitmentPercentage)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedRatio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (ratio <= 0.70f) "Healthy (~${String.format(Locale.US, "%.0f", report.commitmentPercentage)}% committed)"
                    else if (ratio <= 1.0f) "Moderate (~${String.format(Locale.US, "%.0f", report.commitmentPercentage)}% committed)"
                    else "Over-committed (Expenses > Income)",
                    fontSize = 10.5.sp,
                    color = barColor,
                    fontWeight = FontWeight.Medium
                )
                if (isCurrentMonth && report.daysLeftInMonth > 0 && report.remainingDailyBudget > 0) {
                    Text(
                        text = "\$${String.format(Locale.US, "%.2f", report.remainingDailyBudget)}/day buffer",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyCategoryDistributionCard(
    categories: List<MonthlyCategoryExpense>,
    totalExpenses: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("monthly_categories_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
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
                Text(
                    text = "Planned Expenses by Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${categories.size} categories",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (categories.isEmpty()) {
                Text(
                    text = "No planned expenses or bills found for this month.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                categories.forEach { cat ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${cat.category} (${cat.itemCount})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "\$${String.format(Locale.US, "%,.2f", cat.amount)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(${String.format(Locale.US, "%.0f", cat.percentageOfTotal)}%)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val barWidth = (cat.percentageOfTotal / 100f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(barWidth)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBillsListContent(
    report: MonthlySummaryReport,
    filter: String,
    onFilterChange: (String) -> Unit,
    dateFormatter: DateTimeFormatter,
    onToggleCompleted: (TransactionEntity) -> Unit,
    onAddPlannedBill: () -> Unit
) {
    val filteredBills = remember(report.bills, filter) {
        when (filter) {
            "Due" -> report.bills.filter { !it.isPaid }
            "Paid" -> report.bills.filter { it.isPaid }
            "AutoPay" -> report.bills.filter { it.isAutoPay }
            else -> report.bills
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp)) {
        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("All", "Due", "Paid", "AutoPay").forEach { f ->
                val isSelected = filter == f
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(f) },
                    modifier = Modifier.height(24.dp).testTag("filter_chip_${f.lowercase()}"),
                    label = {
                        val count = when (f) {
                            "Due" -> report.bills.count { !it.isPaid }
                            "Paid" -> report.bills.count { it.isPaid }
                            "AutoPay" -> report.bills.count { it.isAutoPay }
                            else -> report.bills.size
                        }
                        Text("$f ($count)", fontSize = 9.sp)
                    }
                )
            }
        }

        if (filteredBills.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No bills match this filter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(filteredBills, key = { it.id }) { bill ->
                    MonthlyBillCard(bill = bill, dateFormatter = dateFormatter, onToggleCompleted = onToggleCompleted)
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
fun MonthlyBillCard(
    bill: MonthlyBillItem,
    dateFormatter: DateTimeFormatter,
    onToggleCompleted: (TransactionEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("bill_item_${bill.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (bill.isPaid) MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp).copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        ),
        border = BorderStroke(
            1.dp,
            if (bill.isPaid) Color(0xFF10B981).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (bill.isPaid) Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (bill.isPaid) Icons.Default.Check else Icons.Default.Receipt,
                            contentDescription = null,
                            tint = if (bill.isPaid) Color(0xFF047857) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = bill.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (bill.isAutoPay) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "AutoPay",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Due ${bill.dueDate.format(dateFormatter)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("•", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = bill.category,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "\$${String.format(Locale.US, "%,.2f", bill.amount)}",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (bill.isPaid) Color(0xFF047857) else MaterialTheme.colorScheme.onSurface
                )

                if (bill.isPaid) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "PAID",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "UNPAID",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyAllExpensesContent(
    report: MonthlySummaryReport,
    dateFormatter: DateTimeFormatter,
    onToggleCompleted: (TransactionEntity) -> Unit,
    onAddExpense: () -> Unit
) {
    if (report.bills.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No recorded or planned expenses in this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Text(
                    text = "All Items (${report.bills.size}) • Total: \$${String.format(Locale.US, "%,.2f", report.totalPlannedExpenses)}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            items(report.bills, key = { it.id }) { item ->
                MonthlyBillCard(bill = item, dateFormatter = dateFormatter, onToggleCompleted = onToggleCompleted)
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MonthlyIncomeListContent(
    report: MonthlySummaryReport,
    dateFormatter: DateTimeFormatter
) {
    if (report.incomeItems.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No income records or paychecks projected for this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Text(
                    text = "Total Projected Income: \$${String.format(Locale.US, "%,.2f", report.totalIncome)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF047857),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            items(report.incomeItems, key = { it.id }) { inc ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("income_item_${inc.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF10B981).copy(alpha = 0.15f),
                                modifier = Modifier.size(30.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        text = inc.title,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (inc.isProjected) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "Scheduled",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = inc.date.format(dateFormatter) + " • " + inc.category,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "+\$${String.format(Locale.US, "%,.2f", inc.amount)}",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

/**
 * High-impact compact dashboard card to embed directly into the Home Screen (Page 0)
 */
@Composable
fun MonthlySummaryDashboardCard(
    uiState: BudgetUiState,
    onOpenFullMonthlySummary: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onToggleExpand: (() -> Unit)? = null
) {
    val currentMonth = remember { YearMonth.now() }
    val report = remember(currentMonth, uiState) {
        MonthlySummaryCalculator.calculate(
            month = currentMonth,
            uiState = uiState,
            activeAccountFilter = uiState.activeAccountName
        )
    }

    val isSurplus = !report.isDeficit
    val surplusColor = if (isSurplus) Color(0xFF059669) else MaterialTheme.colorScheme.error
    val ratio = (report.commitmentPercentage / 100f).coerceIn(0f, 1f)
    var localExpanded by rememberSaveable { mutableStateOf(false) }
    val isExpanded = expanded ?: if (onToggleExpand != null) uiState.monthlySummaryExpanded else localExpanded
    val haptic = LocalHapticFeedback.current
    val toggleAction = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onToggleExpand != null) onToggleExpand() else localExpanded = !localExpanded
    }
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "monthly_summary_arrow")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dashboard_monthly_summary_card"),
        shape = RoundedCornerShape(if (isExpanded) 14.dp else 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isExpanded) 3.dp else 1.dp)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isExpanded) 0.6f else 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (isExpanded) 10.dp else 8.dp, vertical = if (isExpanded) 10.dp else 2.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 6.dp else 1.5.dp)
        ) {
            // Header (Clickable to toggle expand / collapse)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { toggleAction() }
                    .testTag("monthly_summary_header_toggle")
                    .padding(vertical = if (isExpanded) 0.dp else 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(if (isExpanded) 26.dp else 18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Summarize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (isExpanded) 14.dp else 10.dp)
                            )
                        }
                    }
                    if (!isExpanded) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Text(
                                text = "Monthly Summary (${currentMonth.format(DateTimeFormatter.ofPattern("MMM", Locale.US))})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "• \$${String.format(Locale.US, "%,.0f", report.totalPlannedExpenses)} bills",
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Monthly Summary • ${currentMonth.format(DateTimeFormatter.ofPattern("MMM", Locale.US))}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Planned Bills vs. Monthly Income",
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = surplusColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = if (isSurplus) "+\$${String.format(Locale.US, "%,.0f", report.netSurplus)} Net"
                                   else "-\$${String.format(Locale.US, "%,.0f", Math.abs(report.netSurplus))} Deficit",
                            fontSize = if (isExpanded) 10.sp else 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            color = surplusColor,
                            modifier = Modifier.padding(horizontal = if (isExpanded) 6.dp else 4.dp, vertical = if (isExpanded) 2.5.dp else 0.5.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Monthly Summary" else "Expand Monthly Summary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(if (isExpanded) 18.dp else 15.dp)
                            .rotate(arrowRotation)
                    )
                }
            }

            // Collapsed Quick Pacing Progress Bar
            if (!isExpanded && report.totalIncome > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(ratio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(1.dp))
                            .background(if (report.isDeficit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    )
                }
            }

            // Body: Shown when expanded
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(modifier = Modifier.height(2.dp))

                    // Metric Summary Strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Income", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "\$${String.format(Locale.US, "%,.0f", report.totalIncome)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF047857)
                            )
                        }
                        Text("vs", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Column {
                            Text("Planned Bills", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "\$${String.format(Locale.US, "%,.0f", report.totalPlannedExpenses)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text("=", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Free Buffer", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "\$${String.format(Locale.US, "%,.0f", report.netSurplus.coerceAtLeast(0.0))}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = surplusColor
                            )
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (report.isDeficit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        )
                    }

                    // Action to view full panel
                    Button(
                        onClick = onOpenFullMonthlySummary,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth().height(34.dp).testTag("open_monthly_summary_button")
                    ) {
                        Text("View Full Monthly Summary", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(13.dp))
                    }
                }
            }
        }
    }
}
