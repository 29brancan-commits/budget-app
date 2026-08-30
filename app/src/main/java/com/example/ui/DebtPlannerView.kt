package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DebtPlannerView(
    uiState: BudgetUiState,
    onAddDebt: (name: String, balance: Double, interestRate: Double, minimumPayment: Double, category: String, notes: String) -> Unit,
    onUpdateDebt: (DebtEntity) -> Unit,
    onDeleteDebt: (DebtEntity) -> Unit,
    isWideScreen: Boolean = false,
    onImportDebtsFromCsv: (suspend (String) -> Pair<Boolean, String>)? = null,
    onExportDebtsToCsv: (() -> String)? = null,
    onSyncGoogleDrive: (() -> Unit)? = null,
    onConnectGoogleDrive: (() -> Unit)? = null,
    isSyncing: Boolean = false,
    accountEmail: String? = null
) {
    var selectedStrategy by remember { mutableStateOf(PayoffStrategy.SNOWBALL) }
    var extraPaymentInput by remember { mutableStateOf("150") }
    var targetMonthsGoal by remember { mutableStateOf(24) } // Target payoff timeframe in months
    var isTargetTimelineMode by remember { mutableStateOf(false) }

    val debts = uiState.allDebts
    val isDark = uiState.isDarkMode
    val coroutineScope = rememberCoroutineScope()

    var showDebtCsvImportDialog by remember { mutableStateOf(false) }
    var showDebtCsvExportDialog by remember { mutableStateOf(false) }
    var debtCsvExportData by remember { mutableStateOf("") }
    var debtImportStatusMsg by remember { mutableStateOf("") }
    var isImportingDebtsProgress by remember { mutableStateOf(false) }

    // AI Debt Payoff Advisor States
    var aiSuggestionsResult by remember { mutableStateOf<String?>(null) }
    var isLoadingAiSuggestions by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    // Target timeline mode calculation
    val computedExtraForTarget = remember(debts, selectedStrategy, targetMonthsGoal) {
        if (debts.isNotEmpty()) {
            DebtPayoffCalculator.calculateRequiredExtraPayment(debts, selectedStrategy, targetMonthsGoal)
        } else 0.0
    }

    val extraMonthlyBudget = if (isTargetTimelineMode) {
        computedExtraForTarget
    } else {
        extraPaymentInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    val runAiAnalysis: (String?) -> Unit = { customPrompt ->
        if (debts.isEmpty()) {
            aiErrorMessage = "Please add at least one debt to generate AI payoff suggestions."
        } else {
            isLoadingAiSuggestions = true
            aiErrorMessage = null
            coroutineScope.launch {
                val result = com.example.data.GeminiDebtAdvisorService.getDebtPayoffSuggestions(
                    debts = debts,
                    selectedStrategy = selectedStrategy,
                    extraMonthlyBudget = extraMonthlyBudget,
                    targetMonthsGoal = targetMonthsGoal,
                    isTargetTimelineMode = isTargetTimelineMode,
                    customPrompt = customPrompt
                )
                isLoadingAiSuggestions = false
                result.onSuccess { text ->
                    aiSuggestionsResult = text
                    aiErrorMessage = null
                }.onFailure { err ->
                    aiErrorMessage = err.localizedMessage ?: "Failed to generate AI suggestions."
                }
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var debtToEdit by remember { mutableStateOf<DebtEntity?>(null) }
    var showTimelineDetails by remember { mutableStateOf(false) }

    // Strategy calculations
    val strategyMap = remember(debts, extraMonthlyBudget) {
        DebtPayoffCalculator.calculatePayoff(
            debts = debts,
            extraMonthlyBudget = extraMonthlyBudget
        )
    }

    val activeResult = strategyMap[selectedStrategy] ?: strategyMap[PayoffStrategy.SNOWBALL]!!
    val minimumResult = strategyMap[PayoffStrategy.MINIMUM_ONLY]!!

    val totalDebtBalance = remember(debts) { debts.sumOf { it.balance } }
    val totalMinimumPayments = remember(debts) { debts.sumOf { it.minimumPayment } }
    val averageApr = remember(debts) {
        if (debts.isNotEmpty()) debts.map { it.interestRate }.average() else 0.0
    }

    val monthYearFormatter = remember { DateTimeFormatter.ofPattern("MMM yyyy", Locale.US) }

    val plannerControls: @Composable () -> Unit = {
        // 1. Debt Planner Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("debt_summary_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF09090B) else Color(0xFFF1F5F9)
            ),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🎯", fontSize = 14.sp)
                            }
                        }
                        Column {
                            Text(
                                text = "Debt Payoff Planner",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Model Snowball vs Avalanche & optimize payoff timelines",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showAddDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_debt_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("Add Debt", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Summary Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF000000) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Debt", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "$%,.2f", totalDebtBalance),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF000000) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Min Payments", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "$%,.2f/mo", totalMinimumPayments),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isDark) Color(0xFF000000) else Color.White,
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Avg APR", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                String.format(Locale.US, "%.1f%%", averageApr),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }

        // 2. Google Sheets & CSV Import Card for Debts
        GoogleSheetsDebtSyncCard(
            accountEmail = accountEmail,
            isSyncing = isSyncing,
            onSyncClick = { onSyncGoogleDrive?.invoke() },
            onOpenImportDialog = {
                debtImportStatusMsg = ""
                showDebtCsvImportDialog = true
            },
            onOpenExportDialog = {
                debtCsvExportData = onExportDebtsToCsv?.invoke() ?: ""
                showDebtCsvExportDialog = true
            },
            onConnectDrive = { onConnectGoogleDrive?.invoke() },
            statusMessage = debtImportStatusMsg
        )

        // 3. AI Debt Payoff Advisor Card
        AiDebtAdvisorCard(
            debts = debts,
            selectedStrategy = selectedStrategy,
            extraMonthlyBudget = extraMonthlyBudget,
            targetMonthsGoal = targetMonthsGoal,
            isTargetTimelineMode = isTargetTimelineMode,
            aiResult = aiSuggestionsResult,
            isLoading = isLoadingAiSuggestions,
            errorMessage = aiErrorMessage,
            onGenerateSuggestions = { prompt -> runAiAnalysis(prompt) },
            onClearSuggestions = {
                aiSuggestionsResult = null
                aiErrorMessage = null
            },
            isDark = isDark
        )

        // 3. Planning Mode Switcher & Contribution/Target Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF18181B) else Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Planning Mode",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = !isTargetTimelineMode,
                            onClick = { isTargetTimelineMode = false },
                            label = { Text("💵 Monthly Budget", fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("mode_monthly_budget")
                        )
                        FilterChip(
                            selected = isTargetTimelineMode,
                            onClick = {
                                isTargetTimelineMode = true
                                targetMonthsGoal = activeResult.totalMonths.coerceAtLeast(6)
                            },
                            label = { Text("🎯 Target Timeline", fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("mode_target_timeline")
                        )
                    }
                }

                if (!isTargetTimelineMode) {
                    // Extra monthly payment input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = extraPaymentInput,
                            onValueChange = { extraPaymentInput = it },
                            label = { Text("Extra Monthly Payment ($)") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("extra_payment_input")
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Total Monthly Budget",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "$%,.2f/mo",
                                    totalMinimumPayments + extraMonthlyBudget
                                ),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    // Target timeline slider & goal box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Target Debt-Free Date",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val targetDate = java.time.LocalDate.now().plusMonths(targetMonthsGoal.toLong())
                                Text(
                                    text = "${targetDate.format(monthYearFormatter)} ($targetMonthsGoal months)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Required Extra Payment",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "+$%,.2f/mo",
                                        computedExtraForTarget
                                    ),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }

                        Slider(
                            value = targetMonthsGoal.toFloat(),
                            onValueChange = { targetMonthsGoal = it.toInt() },
                            valueRange = 6f..120f,
                            steps = 113,
                            modifier = Modifier.testTag("target_timeline_slider")
                        )

                        Text(
                            text = "Total Monthly Allocation: ${String.format(Locale.US, "$%,.2f", totalMinimumPayments + computedExtraForTarget)}/mo",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4. Payoff Strategy Selector Tabs
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Payoff Strategy",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 2.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    PayoffStrategy.SNOWBALL to "❄️ Snowball",
                    PayoffStrategy.AVALANCHE to "🌋 Avalanche",
                    PayoffStrategy.MINIMUM_ONLY to "⌛ Minimums Only"
                ).forEach { (strategy, label) ->
                    val isSelected = selectedStrategy == strategy
                    Card(
                        onClick = { selectedStrategy = strategy },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("strategy_tab_${strategy.name.lowercase()}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isDark) Color(0xFF18181B) else Color.White
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            val res = strategyMap[strategy]
                            if (res != null) {
                                Text(
                                    text = "${res.totalMonths} mos",
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Active Strategy Comparison & Metrics Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFEFF6FF)
            ),
            border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = when (selectedStrategy) {
                                PayoffStrategy.SNOWBALL -> "❄️ Debt Snowball (Lowest Balance First)"
                                PayoffStrategy.AVALANCHE -> "🌋 Debt Avalanche (Highest Interest First)"
                                PayoffStrategy.MINIMUM_ONLY -> "⌛ Minimum Payments Only"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }

                    if (selectedStrategy != PayoffStrategy.MINIMUM_ONLY) {
                        val interestSaved = minimumResult.totalInterestPaid - activeResult.totalInterestPaid
                        val monthsSaved = minimumResult.totalMonths - activeResult.totalMonths
                        if (interestSaved > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "Save $%,.0f & %d mos!", interestSaved, monthsSaved),
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Debt Free Date", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = activeResult.payoffDate.format(monthYearFormatter),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }

                    Column {
                        Text("Timeframe", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${activeResult.totalMonths} months",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Total Interest Paid", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format(Locale.US, "$%,.2f", activeResult.totalInterestPaid),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706)
                        )
                    }
                }

                // Show/Hide Timeline Details Button
                TextButton(
                    onClick = { showTimelineDetails = !showTimelineDetails },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (showTimelineDetails) "Hide Monthly Timeline Projection ▲" else "View Monthly Projection Schedule ▼",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                AnimatedVisibility(visible = showTimelineDetails) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Divider(color = Color(0xFF3B82F6).copy(alpha = 0.2f))
                        Text(
                            text = "Month-by-Month Projection",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(activeResult.monthlyProgress.take(36)) { step ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Month ${step.monthIndex} (${step.monthDate.format(monthYearFormatter)})",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "Remaining: $%,.0f", step.totalRemainingBalance),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (step.totalRemainingBalance <= 0) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = String.format(Locale.US, "Interest paid: $%,.0f", step.totalInterestPaidSoFar),
                                            fontSize = 9.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

    val trackedDebtsList: @Composable () -> Unit = {
        // 6. Tracked Debts List Section
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracked Debts (${debts.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Sorted by ${if (selectedStrategy == PayoffStrategy.AVALANCHE) "Highest APR" else "Lowest Balance"}",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (debts.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDark) Color(0xFF18181B) else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🏦", fontSize = 28.sp)
                        Text(
                            text = "No debts added yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap 'Add Debt' above or 'Import CSV' from Google Sheets to track your payoff timeline.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val sortedDebts = remember(debts, selectedStrategy) {
                    when (selectedStrategy) {
                        PayoffStrategy.AVALANCHE -> debts.sortedWith(
                            compareBy<DebtEntity> { it.balance <= 0.0 }
                                .thenByDescending { it.interestRate }
                                .thenBy { it.balance }
                        )
                        else -> debts.sortedWith(
                            compareBy<DebtEntity> { it.balance <= 0.0 }
                                .thenBy { it.balance }
                                .thenByDescending { it.interestRate }
                        )
                    }
                }

                sortedDebts.forEachIndexed { index, debt ->
                    val paybackResult = activeResult.individualPayoffs.firstOrNull { it.debtId == debt.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debt_item_${debt.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF18181B) else Color.White
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "${index + 1}",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${getCategoryEmoji(debt.category)} ${debt.name}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { debtToEdit = debt },
                                        modifier = Modifier.size(28.dp).testTag("edit_debt_${debt.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteDebt(debt) },
                                        modifier = Modifier.size(28.dp).testTag("delete_debt_${debt.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Balance", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.2f", debt.balance),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }

                                Column {
                                    Text("APR", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "%.2f%%", debt.interestRate),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }

                                Column {
                                    Text("Min Payment", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.2f/mo", debt.minimumPayment),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (paybackResult != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Payoff Date", fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = paybackResult.payoffDate.format(monthYearFormatter),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                }
                            }

                            if (debt.notes.isNotBlank()) {
                                Text(
                                    text = "Note: ${debt.notes}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isWideScreen) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("debt_planner_view"),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(0.48f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plannerControls()
                }
            }
            Box(modifier = Modifier.weight(0.52f)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    trackedDebtsList()
                }
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .testTag("debt_planner_view"),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            plannerControls()
            trackedDebtsList()
        }
    }

    // Add Debt Dialog
    if (showAddDialog) {
        DebtEditDialog(
            debt = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, balance, interest, minPay, cat, notes ->
                onAddDebt(name, balance, interest, minPay, cat, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Debt Dialog
    if (debtToEdit != null) {
        DebtEditDialog(
            debt = debtToEdit,
            onDismiss = { debtToEdit = null },
            onConfirm = { name, balance, interest, minPay, cat, notes ->
                val updated = debtToEdit!!.copy(
                    name = name,
                    balance = balance,
                    interestRate = interest,
                    minimumPayment = minPay,
                    category = cat,
                    notes = notes
                )
                onUpdateDebt(updated)
                debtToEdit = null
            }
        )
    }

    // Debt CSV Import Dialog
    if (showDebtCsvImportDialog) {
        DebtCsvImportDialog(
            onDismiss = { showDebtCsvImportDialog = false },
            onImport = { csvInput ->
                coroutineScope.launch {
                    isImportingDebtsProgress = true
                    val (success, msg) = onImportDebtsFromCsv?.invoke(csvInput) ?: Pair(false, "Import handler unavailable")
                    isImportingDebtsProgress = false
                    debtImportStatusMsg = msg
                    if (success) {
                        showDebtCsvImportDialog = false
                    }
                }
            },
            statusMsg = debtImportStatusMsg,
            isImporting = isImportingDebtsProgress
        )
    }

    // Debt CSV Export Dialog
    if (showDebtCsvExportDialog) {
        DebtCsvExportDialog(
            csvData = debtCsvExportData,
            onDismiss = { showDebtCsvExportDialog = false },
            onCopied = {
                showDebtCsvExportDialog = false
                debtImportStatusMsg = "CSV copied to clipboard!"
            }
        )
    }
}

private fun getCategoryEmoji(category: String): String {
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
fun GoogleSheetsDebtSyncCard(
    accountEmail: String?,
    isSyncing: Boolean,
    onSyncClick: () -> Unit,
    onOpenImportDialog: () -> Unit,
    onOpenExportDialog: () -> Unit,
    onConnectDrive: () -> Unit,
    statusMessage: String
) {
    var isExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("google_sheets_debt_sync_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0F766E).copy(alpha = 0.12f)
        ),
        border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
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
                        color = Color(0xFF0F766E),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("📊", fontSize = 14.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Google Drive Debt Payoff Sheet",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "Drive Sync",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp),
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = if (!accountEmail.isNullOrBlank()) "Account: $accountEmail" else "Creates 'Debt Payoff Sheet' in Google Drive to edit & sync balances/APR",
                            fontSize = 10.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color(0xFF0F766E),
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onSyncClick,
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .testTag("sync_debts_drive_button")
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isSyncing) "Syncing Sheet..." else "Create & Sync Debt Sheet",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }

                        Button(
                            onClick = onOpenImportDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D9488)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_debts_csv_button")
                        ) {
                            Text("Import CSV", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onOpenExportDialog,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_debts_csv_button")
                        ) {
                            Text("View/Copy CSV", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onConnectDrive,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Authorize Drive", fontWeight = FontWeight.Medium, fontSize = 9.5.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://sheets.new"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("New Sheet", fontWeight = FontWeight.Medium, fontSize = 9.5.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://drive.google.com/drive/my-drive"))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("My Drive", fontWeight = FontWeight.Medium, fontSize = 9.5.sp, maxLines = 1)
                        }
                    }

                    if (statusMessage.isNotEmpty()) {
                        Text(
                            text = statusMessage,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DebtCsvImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    statusMsg: String,
    isImporting: Boolean
) {
    var csvText by remember { mutableStateOf("") }
    val placeholderText = "Debt Name,Balance,Interest Rate (APR),Minimum Payment,Category,Notes\n\"Chase Freedom\",2450.00,19.99,65.00,\"Credit Card\",\"0% promo ends Dec\"\n\"Auto Loan\",14200.00,4.50,320.00,\"Auto Loan\",\"\""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Debts from Google Sheets / CSV", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste CSV data copied from Google Sheets or Excel. Header row is detected automatically:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = csvText,
                    onValueChange = { csvText = it },
                    placeholder = { Text(placeholderText, fontSize = 10.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("debt_csv_import_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    maxLines = 100
                )
                if (statusMsg.isNotEmpty()) {
                    Text(
                        text = statusMsg,
                        fontSize = 11.5.sp,
                        color = if (statusMsg.contains("Success", ignoreCase = true)) Color(0xFF047857) else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(csvText) },
                enabled = !isImporting && csvText.isNotBlank(),
                modifier = Modifier.testTag("confirm_import_debts_button")
            ) {
                Text(if (isImporting) "Importing..." else "Import Debts")
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
fun DebtCsvExportDialog(
    csvData: String,
    onDismiss: () -> Unit,
    onCopied: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exported Debts CSV", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Copy this CSV data to paste into Google Sheets or Excel:",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = csvData,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    maxLines = 100
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = {
                        try {
                            val file = java.io.File(context.filesDir, "debts_google_sheets_sync.csv")
                            file.writeText(csvData, Charsets.UTF_8)
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "com.example.fileprovider",
                                file
                            )
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "debts_google_sheets_sync.csv")
                                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share or Save CSV File"))
                        } catch (e: Exception) {
                            android.widget.Toast.makeText(context, "CSV saved to storage", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Share / Save", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("DebtsCSV", csvData)
                        clipboard.setPrimaryClip(clip)
                        onCopied()
                    }
                ) {
                    Text("Copy CSV", fontSize = 11.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun DebtEditDialog(
    debt: DebtEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: Double, interestRate: Double, minimumPayment: Double, category: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(debt?.name ?: "") }
    var balanceInput by remember { mutableStateOf(debt?.balance?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var interestInput by remember { mutableStateOf(debt?.interestRate?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var minPaymentInput by remember { mutableStateOf(debt?.minimumPayment?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var selectedCategory by remember { mutableStateOf(debt?.category ?: "Credit Card") }
    var notes by remember { mutableStateOf(debt?.notes ?: "") }

    var errorMessage by remember { mutableStateOf("") }

    val categories = listOf("Credit Card", "Auto Loan", "Student Loan", "Personal Loan", "Mortgage", "Medical", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "Add Debt" else "Edit Debt") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Debt Name") },
                    placeholder = { Text("e.g. Chase Freedom Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_name_field")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it },
                        label = { Text("Balance ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("debt_balance_field")
                    )

                    OutlinedTextField(
                        value = interestInput,
                        onValueChange = { interestInput = it },
                        label = { Text("APR (%)") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("debt_apr_field")
                    )
                }

                OutlinedTextField(
                    value = minPaymentInput,
                    onValueChange = { minPaymentInput = it },
                    label = { Text("Minimum Monthly Payment ($)") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("debt_min_payment_field")
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Category", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(4).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 10.5.sp) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val b = balanceInput.toDoubleOrNull()
                    val r = interestInput.toDoubleOrNull()
                    val m = minPaymentInput.toDoubleOrNull()

                    if (name.isBlank()) {
                        errorMessage = "Please enter a debt name."
                    } else if (b == null || b <= 0) {
                        errorMessage = "Please enter a valid balance."
                    } else if (r == null || r < 0) {
                        errorMessage = "Please enter a valid APR percentage."
                    } else if (m == null || m <= 0) {
                        errorMessage = "Please enter a valid minimum monthly payment."
                    } else {
                        onConfirm(name.trim(), b, r, m, selectedCategory, notes.trim())
                    }
                },
                modifier = Modifier.testTag("save_debt_button")
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
