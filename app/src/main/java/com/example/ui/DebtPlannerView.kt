package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    accountEmail: String? = null,
    onLogPayment: ((debtId: Int, paymentAmount: Double, principalAmount: Double, interestAmount: Double, note: String, createExpense: Boolean) -> Unit)? = null,
    onDeletePayment: ((DebtPaymentEntity) -> Unit)? = null,
    onToggleBillLink: ((DebtEntity, Boolean, Int) -> Unit)? = null,
    onTogglePriorityPin: ((DebtEntity) -> Unit)? = null,
    onTogglePaidOff: ((DebtEntity) -> Unit)? = null,
    onAddFullDebt: ((DebtEntity) -> Unit)? = null
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

    // Payment logging & history states
    var debtForPaymentLog by remember { mutableStateOf<DebtEntity?>(null) }
    var prefilledPaymentAmount by remember { mutableStateOf<Double?>(null) }
    var expandedPaymentHistoryDebtId by remember { mutableStateOf<Int?>(null) }

    // Cycle Safe-to-Spend Buffer Calculation
    val cycleSafeToSpend = remember(uiState.currentPayPeriod, uiState.monthlyRemainingBalance) {
        maxOf(0.0, uiState.currentPayPeriod?.remainingBalance ?: uiState.monthlyRemainingBalance)
    }

    // AI Debt Payoff Advisor States
    var aiSuggestionsResult by remember { mutableStateOf<String?>(null) }
    var isLoadingAiSuggestions by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }

    val activeDebts = remember(debts) { debts.filter { !it.isPaidOff && it.balance > 0.0 } }

    // Target timeline mode calculation
    val computedExtraForTarget = remember(activeDebts, selectedStrategy, targetMonthsGoal) {
        if (activeDebts.isNotEmpty()) {
            DebtPayoffCalculator.calculateRequiredExtraPayment(activeDebts, selectedStrategy, targetMonthsGoal)
        } else 0.0
    }

    val extraMonthlyBudget = if (isTargetTimelineMode) {
        computedExtraForTarget
    } else {
        extraPaymentInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    val runAiAnalysis: (String?, Double, Double?) -> Unit = { customPrompt, windfall, consolidationApr ->
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
                    customPrompt = customPrompt,
                    windfallAmount = windfall,
                    consolidationApr = consolidationApr
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
    val strategyMap = remember(activeDebts, extraMonthlyBudget) {
        DebtPayoffCalculator.calculatePayoff(
            debts = activeDebts,
            extraMonthlyBudget = extraMonthlyBudget
        )
    }

    val activeResult = strategyMap[selectedStrategy] ?: strategyMap[PayoffStrategy.SNOWBALL]!!
    val minimumResult = strategyMap[PayoffStrategy.MINIMUM_ONLY]!!

    val surplusImpact = remember(activeDebts, selectedStrategy, extraMonthlyBudget, cycleSafeToSpend) {
        if (cycleSafeToSpend > 0.0 && activeDebts.isNotEmpty()) {
            DebtPayoffCalculator.calculateSurplusImpact(
                debts = activeDebts,
                strategy = selectedStrategy,
                currentExtraMonthlyBudget = extraMonthlyBudget,
                surplusAmount = cycleSafeToSpend
            )
        } else null
    }

    val totalDebtBalance = remember(activeDebts) { activeDebts.sumOf { it.balance } }
    val totalMinimumPayments = remember(activeDebts) { activeDebts.sumOf { it.minimumPayment } }
    val averageApr = remember(activeDebts) {
        if (activeDebts.isNotEmpty()) activeDebts.map { it.interestRate }.average() else 0.0
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
            onGenerateSuggestions = { prompt, windfall, apr ->
                runAiAnalysis(prompt, windfall, apr)
            },
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

        // 3b. Pay-Cycle "Safe-to-Spend" Surplus Accelerator Card
        val targetDebt = remember(debts, selectedStrategy, surplusImpact) {
            debts.firstOrNull { it.id == surplusImpact?.targetDebtId }
                ?: debts.firstOrNull { it.name == surplusImpact?.targetDebtName }
                ?: debts.filter { it.customPriorityRank > 0 }.minByOrNull { it.customPriorityRank }
                ?: (if (selectedStrategy == PayoffStrategy.AVALANCHE) debts.maxByOrNull { it.interestRate } else debts.minByOrNull { it.balance })
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cycle_surplus_acceleration_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF04241B) else Color(0xFFECFDF5)
            ),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🔗", fontSize = 13.sp)
                        Text(
                            text = "Accelerate with Pay Cycle Surplus",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF10B981).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "SAFE-TO-SPEND BUFFER",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Active Safe Surplus",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "$%,.2f", cycleSafeToSpend),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (cycleSafeToSpend > 0) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (surplusImpact != null && surplusImpact.surplusAmount > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Projected Interest Saved",
                                fontSize = 9.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = String.format(Locale.US, "+$%,.2f", surplusImpact.interestSaved),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }

                val surplusAmt = surplusImpact?.surplusAmount ?: 0.0
                val planItems = remember(debts, selectedStrategy, surplusAmt) {
                    DebtPayoffCalculator.calculateSurplusAllocation(
                        debts = debts,
                        strategy = selectedStrategy,
                        surplusAmount = surplusAmt
                    )
                }
                val primaryItem = planItems.firstOrNull { it.isPrimaryTarget }
                val rolloverItems = planItems.filter { !it.isPrimaryTarget && it.extraPayment > 0.0 }
                val minOnlyItems = planItems.filter { !it.isPrimaryTarget && it.extraPayment == 0.0 && it.debt.balance > 0.0 }

                if (primaryItem != null) {
                    val targetDebt = primaryItem.debt
                    val targetMinPay = primaryItem.minimumPayment
                    val targetExtraPay = primaryItem.extraPayment
                    val targetTotalPay = primaryItem.totalPayment
                    val isTargetPaidOff = primaryItem.isPaidOffThisCycle

                    val targetReasonBadge = when {
                        targetDebt.customPriorityRank > 0 -> "Pinned Priority #${targetDebt.customPriorityRank}"
                        targetDebt.promoApr != null && targetDebt.promoExpirationDate != null -> "0% Promo Expiring"
                        selectedStrategy == PayoffStrategy.AVALANCHE -> "Highest APR (${String.format(Locale.US, "%.1f%%", targetDebt.interestRate)}) • Avalanche"
                        selectedStrategy == PayoffStrategy.SNOWBALL -> "Lowest Balance ($${String.format(Locale.US, "%,.0f", targetDebt.balance)}) • Snowball"
                        else -> "Primary Payoff Target"
                    }

                    // Detailed Bill Payment Plan
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF0F172A) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(9.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("📋", fontSize = 11.sp)
                                    Text(
                                        text = "EXACT BILLS TO PAY THIS CYCLE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF047857),
                                        letterSpacing = 0.4.sp
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = targetReasonBadge,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // 1. Primary Target Debt Box (Never exceeds remaining balance)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("🎯", fontSize = 11.sp)
                                                Text(
                                                    text = targetDebt.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Text(
                                                text = "Balance: ${String.format(Locale.US, "$%,.2f", targetDebt.balance)} • ${String.format(Locale.US, "%.1f%% APR", targetDebt.interestRate)}${if (targetDebt.isLinkedToBills) " • Due day ${targetDebt.billDueDay}" else ""}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = String.format(Locale.US, "$%,.2f", targetTotalPay),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF059669)
                                            )
                                            Text(
                                                text = if (isTargetPaidOff) "🎉 PAYS OFF IN FULL" else if (targetExtraPay > 0) "min + surplus" else "minimum due",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isTargetPaidOff) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Divider(color = Color(0xFF10B981).copy(alpha = 0.2f), thickness = 0.5.dp)
                                    if (isTargetPaidOff) {
                                        Text(
                                            text = if (targetDebt.balance <= targetMinPay) {
                                                "• Balance is only ${String.format(Locale.US, "$%,.2f", targetDebt.balance)}. Required minimum payment of ${String.format(Locale.US, "$%,.2f", targetTotalPay)} eliminates this card completely!"
                                            } else {
                                                "• Balance is only ${String.format(Locale.US, "$%,.2f", targetDebt.balance)}. Paying ${String.format(Locale.US, "$%,.2f", targetTotalPay)} (${String.format(Locale.US, "$%,.2f", targetMinPay)} min + ${String.format(Locale.US, "$%,.2f", targetExtraPay)} surplus) clears this debt in full!"
                                            },
                                            fontSize = 9.sp,
                                            lineHeight = 12.5.sp,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        Text(
                                            text = "• Required Minimum: ${String.format(Locale.US, "$%,.2f", targetMinPay)}\n• Surplus Acceleration: +${String.format(Locale.US, "$%,.2f", targetExtraPay)}",
                                            fontSize = 9.sp,
                                            lineHeight = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // 2. Rollover Debts Box (When primary debt needed less than full surplus)
                            if (rolloverItems.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0EA5E9).copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color(0xFF0EA5E9).copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text("⚡", fontSize = 11.sp)
                                                Text(
                                                    text = "SURPLUS ROLLOVER CASCADE",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0284C7)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFF0EA5E9).copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = "+$${String.format(Locale.US, "%,.0f", rolloverItems.sumOf { it.extraPayment })} Rolled Over",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0284C7),
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Because ${targetDebt.name} only needed ${String.format(Locale.US, "$%,.2f", targetTotalPay)} to clear, your remaining surplus automatically rolls over to your next priority:",
                                            fontSize = 8.5.sp,
                                            lineHeight = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        rolloverItems.forEach { roll ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "• ${roll.debt.name} (Bal: ${String.format(Locale.US, "$%,.2f", roll.debt.balance)})",
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${String.format(Locale.US, "$%,.2f", roll.minimumPayment)} min + ${String.format(Locale.US, "$%,.2f", roll.extraPayment)} rollover surplus${if (roll.isPaidOffThisCycle) " (🎉 Pays off in full!)" else ""}",
                                                        fontSize = 8.5.sp,
                                                        color = Color(0xFF0284C7)
                                                    )
                                                }
                                                Text(
                                                    text = String.format(Locale.US, "$%,.2f", roll.totalPayment),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0284C7)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Other debts minimum obligations
                            if (minOnlyItems.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        text = "Other bills to pay minimum only to avoid late fees:",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    minOnlyItems.forEach { otherItem ->
                                        val other = otherItem.debt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 2.dp, vertical = 1.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "• ${other.name}${if (other.isLinkedToBills) " (Due day ${other.billDueDay})" else ""}",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = String.format(Locale.US, "$%,.2f min", otherItem.minimumPayment),
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            // 4. Total cycle cash outlay (exact allocated sum)
                            val totalOutlay = planItems.sumOf { it.totalPayment }
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Debt Payments This Cycle:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "$%,.2f", totalOutlay),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    if (surplusImpact != null && surplusImpact.surplusAmount > 0) {
                        val daysSaved = surplusImpact.daysSaved
                        val timelineAdvancementText = if (daysSaved > 0) "$daysSaved days" else "${surplusImpact.monthsSaved} months"
                        val summaryText = if (isTargetPaidOff && rolloverItems.isNotEmpty()) {
                            "Paying ${String.format(Locale.US, "$%,.2f", targetTotalPay)} eliminates ${targetDebt.name} in full! The remaining ${String.format(Locale.US, "$%,.2f", rolloverItems.sumOf { it.extraPayment })} surplus rolls over to your next debt, advancing your debt-free date by $timelineAdvancementText and saving ${String.format(Locale.US, "$%,.2f", surplusImpact.interestSaved)} in interest."
                        } else if (isTargetPaidOff) {
                            "Paying ${String.format(Locale.US, "$%,.2f", targetTotalPay)} eliminates ${targetDebt.name} in full, advancing your debt-free date by $timelineAdvancementText and saving ${String.format(Locale.US, "$%,.2f", surplusImpact.interestSaved)} in interest."
                        } else {
                            "Putting ${String.format(Locale.US, "$%,.0f", targetExtraPay)} toward ${targetDebt.name} moves your debt-free date forward by $timelineAdvancementText and saves ${String.format(Locale.US, "$%,.2f", surplusImpact.interestSaved)} in interest."
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isDark) Color(0xFF0F172A) else Color.White,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("⚡", fontSize = 13.sp)
                                Text(
                                    text = summaryText,
                                    fontSize = 10.sp,
                                    lineHeight = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (isTargetPaidOff) {
                                Button(
                                    onClick = {
                                        debtForPaymentLog = targetDebt
                                        prefilledPaymentAmount = targetTotalPay
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pay Off ${targetDebt.name} in Full (${String.format(Locale.US, "$%,.2f", targetTotalPay)})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            } else {
                                Button(
                                    onClick = {
                                        debtForPaymentLog = targetDebt
                                        prefilledPaymentAmount = targetExtraPay
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Pay ${String.format(Locale.US, "$%,.0f", targetExtraPay)} Extra to ${targetDebt.name}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            val firstRollover = rolloverItems.firstOrNull()
                            if (firstRollover != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            debtForPaymentLog = firstRollover.debt
                                            prefilledPaymentAmount = firstRollover.totalPayment
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Pay ${String.format(Locale.US, "$%,.0f", firstRollover.totalPayment)} to ${firstRollover.debt.name} (Rollover)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            extraPaymentInput = String.format(Locale.US, "%.0f", surplusImpact.surplusAmount)
                                            isTargetTimelineMode = false
                                        },
                                        modifier = Modifier.weight(0.8f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("⚡ Monthly Extra", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            debtForPaymentLog = targetDebt
                                            prefilledPaymentAmount = targetTotalPay
                                        },
                                        modifier = Modifier.weight(1.1f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Pay Full ${String.format(Locale.US, "$%,.0f", targetTotalPay)} (Min + Extra)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            extraPaymentInput = String.format(Locale.US, "%.0f", surplusImpact.surplusAmount)
                                            isTargetTimelineMode = false
                                        },
                                        modifier = Modifier.weight(0.9f),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("⚡ Set Monthly Extra", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Currently $0.00 safe surplus in this pay cycle. Pay minimums as scheduled above. Any surplus earned will accelerate ${targetDebt.name} first.",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    debtForPaymentLog = targetDebt
                                    prefilledPaymentAmount = targetMinPay
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Pay $${String.format(Locale.US, "%.0f", targetMinPay)} Min to ${targetDebt.name}", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Add at least one debt below to unlock one-tap pay cycle payoff acceleration and itemized bill instructions.",
                        fontSize = 9.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                    val pinned = debts.filter { it.customPriorityRank > 0 && !it.isPaidOff && it.balance > 0.0 }.sortedBy { it.customPriorityRank }
                    val unpinned = debts.filter { it.customPriorityRank <= 0 || it.isPaidOff || it.balance <= 0.0 }
                    val sortedUnpinned = when (selectedStrategy) {
                        PayoffStrategy.AVALANCHE -> unpinned.sortedWith(
                            compareBy<DebtEntity> { it.isPaidOff || it.balance <= 0.0 }
                                .thenByDescending { it.interestRate }
                                .thenBy { it.balance }
                        )
                        else -> unpinned.sortedWith(
                            compareBy<DebtEntity> { it.isPaidOff || it.balance <= 0.0 }
                                .thenBy { it.balance }
                                .thenByDescending { it.interestRate }
                        )
                    }
                    pinned + sortedUnpinned
                }

                sortedDebts.forEachIndexed { index, debt ->
                    val paybackResult = activeResult.individualPayoffs.firstOrNull { it.debtId == debt.id }
                    val originalBalance = if (debt.originalBalance > 0.0) debt.originalBalance else debt.balance
                    val amountPaid = maxOf(0.0, originalBalance - debt.balance)
                    val progressRatio = if (originalBalance > 0.0) (amountPaid / originalBalance).coerceIn(0.0, 1.0) else 0.0
                    val progressPct = (progressRatio * 100.0).toInt()
                    val debtPayments = remember(uiState.allDebtPayments, debt.id) {
                        uiState.allDebtPayments.filter { it.debtId == debt.id }
                    }
                    val linkedAccount = remember(uiState.allAccounts, debt.linkedAccountName, debt.linkedAccountId) {
                        debt.linkedAccountName?.takeIf { it.isNotBlank() }
                            ?: debt.linkedAccountId?.let { accId -> uiState.allAccounts.getOrNull(accId) }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("debt_item_${debt.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF18181B) else Color.White
                        ),
                        border = BorderStroke(
                            if (debt.customPriorityRank > 0) 1.5.dp else 1.dp,
                            if (debt.customPriorityRank > 0) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Header: Name, Priority Badge, Edit/Delete
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
                                    Surface(
                                        shape = CircleShape,
                                        color = if (debt.customPriorityRank > 0) Color(0xFF8B5CF6).copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = if (debt.customPriorityRank > 0) "📌" else "${index + 1}",
                                                fontSize = if (debt.customPriorityRank > 0) 11.sp else 10.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "${getCategoryEmoji(debt.category)} ${debt.name}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (linkedAccount != null) {
                                            Text(
                                                text = "🏦 $linkedAccount",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        if (debt.customPriorityRank > 0) {
                                            Text(
                                                text = "📌 Priority Target #${debt.customPriorityRank}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF7C3AED)
                                            )
                                        }
                                    }
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

                            // Progress Bar & Milestone Tracker
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f / $%,.0f paid", amountPaid, originalBalance),
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$progressPct%",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (progressPct >= 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { progressRatio.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (progressPct >= 100) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                // Visual Milestones Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    when {
                                        debt.balance <= 0.0 || progressPct >= 100 -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF10B981).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "🎉 PAID OFF / DEBT FREE",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        progressPct >= 75 -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "🏆 75% Mark Reached",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6D28D9),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        progressPct >= 50 -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF3B82F6).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "⭐ 50% Halfway Mark",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF1D4ED8),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        progressPct >= 25 -> {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    "🚀 25% Started Strong",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFB45309),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (debt.balance in 0.01..999.99 && originalBalance >= 1000.0) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEC4899).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                "🔥 Under $1,000 Milestone",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFBE185D),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Balance, APR, Min Payment, Payoff Date
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

                            // 0% Promo APR / Balance Transfer Badge
                            if (debt.promoApr != null) {
                                val promoAnalysis = remember(debt) { DebtPayoffCalculator.analyzePromoApr(debt) }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isDark) Color(0xFF2D2305) else Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "⏳ ${debt.promoApr}% Promo APR until ${debt.promoExpirationDate ?: "N/A"}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E)
                                            )
                                            if ((promoAnalysis?.monthsRemaining ?: 0) > 0) {
                                                Text(
                                                    text = "${promoAnalysis?.monthsRemaining} mos left",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
                                                )
                                            }
                                        }
                                        if (promoAnalysis != null && promoAnalysis.requiredMonthlyPayment > 0) {
                                            Text(
                                                text = "Pay $${String.format(Locale.US, "%,.2f", promoAnalysis.requiredMonthlyPayment)}/mo to clear before expiration and avoid retroactive ${debt.interestRate}% interest.",
                                                fontSize = 9.sp,
                                                color = if (isDark) Color(0xFFFCD34D) else Color(0xFF78350F)
                                            )
                                        }
                                    }
                                }
                            }

                            // Action Bar: Bill Link, Priority Pin, History & Log Payment
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Priority Pin Chip
                                    FilterChip(
                                        selected = debt.customPriorityRank > 0,
                                        onClick = { onTogglePriorityPin?.invoke(debt) },
                                        label = {
                                            Text(
                                                if (debt.customPriorityRank > 0) "📌 Pinned" else "📌 Pin",
                                                fontSize = 9.sp
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(24.dp)
                                    )

                                    // Scheduled Bill Link Chip
                                    FilterChip(
                                        selected = debt.isLinkedToBills,
                                        onClick = {
                                            onToggleBillLink?.invoke(debt, !debt.isLinkedToBills, debt.billDueDay)
                                        },
                                        label = {
                                            Text(
                                                if (debt.isLinkedToBills) "📅 Linked (${debt.billDueDay}th)" else "📅 Link Bill",
                                                fontSize = 9.sp
                                            )
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(24.dp)
                                    )

                                    // Paid Off Status Toggle Chip
                                    FilterChip(
                                        selected = debt.isPaidOff || debt.balance <= 0.0,
                                        onClick = { onTogglePaidOff?.invoke(debt) },
                                        label = {
                                            Text(
                                                if (debt.isPaidOff || debt.balance <= 0.0) "✅ Paid Off" else "⭕ Active",
                                                fontSize = 9.sp
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF10B981).copy(alpha = 0.2f),
                                            selectedLabelColor = Color(0xFF10B981)
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (debtPayments.isNotEmpty()) {
                                        TextButton(
                                            onClick = {
                                                expandedPaymentHistoryDebtId = if (expandedPaymentHistoryDebtId == debt.id) null else debt.id
                                            },
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text(
                                                text = "${debtPayments.size} logs ${if (expandedPaymentHistoryDebtId == debt.id) "▲" else "▼"}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            debtForPaymentLog = debt
                                            prefilledPaymentAmount = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.height(24.dp).testTag("log_payment_debt_${debt.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Log Payment", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Payment History dropdown
                            AnimatedVisibility(visible = expandedPaymentHistoryDebtId == debt.id) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .padding(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("Payment Log & Principal vs Interest Breakdown", fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                    debtPayments.forEach { pmt ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) Color(0xFF27272A) else Color.White, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = String.format(Locale.US, "$%,.2f paid", pmt.paymentAmount) +
                                                        if (pmt.note.isNotBlank()) " • ${pmt.note}" else "",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = String.format(Locale.US, "$%,.2f principal + $%,.2f interest", pmt.principalAmount, pmt.interestAmount) +
                                                        " • " + java.time.Instant.ofEpochMilli(pmt.paymentDateMillis)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .toLocalDate()
                                                            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)),
                                                    fontSize = 8.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { onDeletePayment?.invoke(pmt) },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(imageVector = Icons.Default.Close, contentDescription = "Delete payment", modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }

                            if (debt.notes.isNotBlank()) {
                                Text(
                                    text = "Note: ${debt.notes}",
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

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val useSplitColumns = isWideScreen && maxWidth >= 680.dp
        if (useSplitColumns) {
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
    }

    // Add Debt Dialog
    if (showAddDialog) {
        DebtEditDialog(
            debt = null,
            accounts = uiState.allAccounts,
            accountBalances = uiState.accountBalances,
            onDismiss = { showAddDialog = false },
            onConfirmDebt = { newDebt ->
                if (onAddFullDebt != null) {
                    onAddFullDebt(newDebt)
                } else {
                    onAddDebt(newDebt.name, newDebt.balance, newDebt.interestRate, newDebt.minimumPayment, newDebt.category, newDebt.notes)
                }
                showAddDialog = false
            }
        )
    }

    // Edit Debt Dialog
    if (debtToEdit != null) {
        DebtEditDialog(
            debt = debtToEdit,
            accounts = uiState.allAccounts,
            accountBalances = uiState.accountBalances,
            onDismiss = { debtToEdit = null },
            onConfirmDebt = { updated ->
                onUpdateDebt(updated)
                debtToEdit = null
            }
        )
    }

    // Log Debt Payment Dialog
    if (debtForPaymentLog != null) {
        LogDebtPaymentDialog(
            debt = debtForPaymentLog!!,
            prefilledAmount = prefilledPaymentAmount,
            onDismiss = {
                debtForPaymentLog = null
                prefilledPaymentAmount = null
            },
            onConfirm = { pAmt, princ, intAmt, note, createExp ->
                onLogPayment?.invoke(
                    debtForPaymentLog!!.id,
                    pAmt,
                    princ,
                    intAmt,
                    note,
                    createExp
                )
                debtForPaymentLog = null
                prefilledPaymentAmount = null
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
    accounts: List<String> = emptyList(),
    accountBalances: Map<String, AccountBalance> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirmDebt: (DebtEntity) -> Unit
) {
    var name by remember { mutableStateOf(debt?.name ?: "") }
    var balanceInput by remember { mutableStateOf(debt?.balance?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var originalBalanceInput by remember {
        mutableStateOf(debt?.let {
            val orig = if (it.originalBalance > 0.0) it.originalBalance else it.balance
            String.format(Locale.US, "%.2f", orig)
        } ?: "")
    }
    var interestInput by remember { mutableStateOf(debt?.interestRate?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var minPaymentInput by remember { mutableStateOf(debt?.minimumPayment?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var selectedCategory by remember { mutableStateOf(debt?.category ?: "Credit Card") }

    // 0% Promo APR & Balance Transfer Fields
    var hasPromoApr by remember { mutableStateOf(debt?.promoApr != null) }
    var promoAprInput by remember { mutableStateOf(debt?.promoApr?.let { String.format(Locale.US, "%.2f", it) } ?: "0.0") }
    var promoExpirationInput by remember { mutableStateOf(debt?.promoExpirationDate ?: "") }

    // Scheduled Bill Sync Fields
    var isLinkedToBills by remember { mutableStateOf(debt?.isLinkedToBills ?: false) }
    var billDueDayInput by remember { mutableStateOf((debt?.billDueDay ?: 15).toString()) }

    // Priority Pinning
    var isPriorityPinned by remember { mutableStateOf((debt?.customPriorityRank ?: 0) > 0) }

    // Link to Account
    var selectedAccountName by remember {
        mutableStateOf(
            debt?.linkedAccountName
                ?: debt?.linkedAccountId?.let { id -> accounts.getOrNull(id) }
        )
    }
    var accountDropdownExpanded by remember { mutableStateOf(false) }

    var notes by remember { mutableStateOf(debt?.notes ?: "") }
    var errorMessage by remember { mutableStateOf("") }

    val categories = listOf("Credit Card", "Auto Loan", "Student Loan", "Personal Loan", "Mortgage", "Medical", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "Add Debt" else "Edit Debt") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                        onValueChange = {
                            balanceInput = it
                            if (originalBalanceInput.isBlank()) {
                                originalBalanceInput = it
                            }
                        },
                        label = { Text("Current Balance ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("debt_balance_field")
                    )

                    OutlinedTextField(
                        value = originalBalanceInput,
                        onValueChange = { originalBalanceInput = it },
                        label = { Text("Starting Balance ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = interestInput,
                        onValueChange = { interestInput = it },
                        label = { Text("Regular APR (%)") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("debt_apr_field")
                    )

                    OutlinedTextField(
                        value = minPaymentInput,
                        onValueChange = { minPaymentInput = it },
                        label = { Text("Min Payment ($)") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("debt_min_payment_field")
                    )
                }

                // Category selection
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

                // 0% Promo APR & Balance Transfer Tracker
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("0% Promo APR / Balance Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Track promotional window to avoid retroactive interest", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = hasPromoApr,
                                onCheckedChange = { hasPromoApr = it }
                            )
                        }

                        if (hasPromoApr) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = promoAprInput,
                                    onValueChange = { promoAprInput = it },
                                    label = { Text("Promo APR (%)") },
                                    suffix = { Text("%") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = promoExpirationInput,
                                    onValueChange = { promoExpirationInput = it },
                                    label = { Text("Expiration Date") },
                                    placeholder = { Text("YYYY-MM-DD") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                        }
                    }
                }

                // Scheduled Bill Sync Toggle
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Link to Scheduled Bills", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("Auto-syncs minimum payment as upcoming bill in your pay cycle", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isLinkedToBills,
                                onCheckedChange = { isLinkedToBills = it }
                            )
                        }

                        if (isLinkedToBills) {
                            OutlinedTextField(
                                value = billDueDayInput,
                                onValueChange = { billDueDayInput = it },
                                label = { Text("Monthly Due Day (1-28)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Priority Pinning
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pin as #1 Priority Payoff Target", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Overrides strategy to tackle this debt first", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isPriorityPinned,
                        onCheckedChange = { isPriorityPinned = it }
                    )
                }

                // Link to Account Selector
                if (accounts.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Linked Account (Optional)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { accountDropdownExpanded = !accountDropdownExpanded }
                                .testTag("debt_account_selector"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = selectedAccountName ?: "None (Unlinked)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (selectedAccountName != null) {
                                        val bal = accountBalances[selectedAccountName]?.remainingBalance
                                        if (bal != null) {
                                            Text(
                                                text = "Balance: $${String.format(Locale.US, "%.2f", bal)}",
                                                fontSize = 9.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                Icon(
                                    imageVector = if (accountDropdownExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select Account",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = accountDropdownExpanded,
                            onDismissRequest = { accountDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None (Unlinked)", fontSize = 12.sp) },
                                onClick = {
                                    selectedAccountName = null
                                    accountDropdownExpanded = false
                                }
                            )
                            accounts.forEach { accName ->
                                val bal = accountBalances[accName]?.remainingBalance
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(accName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            if (bal != null) {
                                                Text(
                                                    "Balance: $${String.format(Locale.US, "%.2f", bal)}",
                                                    fontSize = 9.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedAccountName = accName
                                        accountDropdownExpanded = false
                                    }
                                )
                            }
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
                    val origB = originalBalanceInput.toDoubleOrNull() ?: b
                    val r = interestInput.toDoubleOrNull()
                    val m = minPaymentInput.toDoubleOrNull()
                    val pApr = if (hasPromoApr) promoAprInput.toDoubleOrNull() else null
                    val dueDay = billDueDayInput.toIntOrNull()?.coerceIn(1, 28) ?: 15

                    if (name.isBlank()) {
                        errorMessage = "Please enter a debt name."
                    } else if (b == null || b <= 0) {
                        errorMessage = "Please enter a valid balance."
                    } else if (r == null || r < 0) {
                        errorMessage = "Please enter a valid APR percentage."
                    } else if (m == null || m <= 0) {
                        errorMessage = "Please enter a valid minimum monthly payment."
                    } else {
                        val selAccIdx = selectedAccountName?.let { name -> accounts.indexOf(name).takeIf { it >= 0 } }
                        val resultDebt = debt?.copy(
                            name = name.trim(),
                            balance = b,
                            originalBalance = origB ?: b,
                            interestRate = r,
                            minimumPayment = m,
                            category = selectedCategory,
                            notes = notes.trim(),
                            promoApr = pApr,
                            promoExpirationDate = if (hasPromoApr && promoExpirationInput.isNotBlank()) promoExpirationInput.trim() else null,
                            customPriorityRank = if (isPriorityPinned) 1 else 0,
                            isLinkedToBills = isLinkedToBills,
                            billDueDay = dueDay,
                            linkedAccountId = selAccIdx,
                            linkedAccountName = selectedAccountName
                        ) ?: DebtEntity(
                            name = name.trim(),
                            balance = b,
                            originalBalance = origB ?: b,
                            interestRate = r,
                            minimumPayment = m,
                            category = selectedCategory,
                            notes = notes.trim(),
                            promoApr = pApr,
                            promoExpirationDate = if (hasPromoApr && promoExpirationInput.isNotBlank()) promoExpirationInput.trim() else null,
                            customPriorityRank = if (isPriorityPinned) 1 else 0,
                            isLinkedToBills = isLinkedToBills,
                            billDueDay = dueDay,
                            linkedAccountId = selAccIdx,
                            linkedAccountName = selectedAccountName
                        )
                        onConfirmDebt(resultDebt)
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

@Composable
fun LogDebtPaymentDialog(
    debt: DebtEntity,
    prefilledAmount: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (paymentAmount: Double, principalAmount: Double, interestAmount: Double, note: String, createExpenseTransaction: Boolean) -> Unit
) {
    val initialPayment = prefilledAmount ?: debt.minimumPayment
    var paymentInput by remember { mutableStateOf(String.format(Locale.US, "%.2f", initialPayment)) }

    // Calculate default interest and principal based on APR
    val estimatedMonthlyInterest = remember(debt) {
        (debt.balance * (debt.interestRate / 100.0) / 12.0).coerceAtLeast(0.0)
    }

    var isManualSplit by remember { mutableStateOf(false) }
    var principalInput by remember {
        val initialPrinc = (initialPayment - estimatedMonthlyInterest).coerceAtLeast(0.0).coerceAtMost(debt.balance)
        mutableStateOf(String.format(Locale.US, "%.2f", initialPrinc))
    }
    var interestInput by remember {
        val initialInt = (initialPayment - (initialPayment - estimatedMonthlyInterest).coerceAtLeast(0.0)).coerceAtLeast(0.0)
        mutableStateOf(String.format(Locale.US, "%.2f", minOf(estimatedMonthlyInterest, initialPayment)))
    }
    var note by remember { mutableStateOf("") }
    var createExpenseTransaction by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }

    // When paymentInput changes and not in manual split, auto-calculate
    LaunchedEffect(paymentInput, isManualSplit) {
        if (!isManualSplit) {
            val p = paymentInput.toDoubleOrNull() ?: 0.0
            val estInt = minOf(p, estimatedMonthlyInterest)
            val estPrinc = (p - estInt).coerceAtLeast(0.0).coerceAtMost(debt.balance)
            principalInput = String.format(Locale.US, "%.2f", estPrinc)
            interestInput = String.format(Locale.US, "%.2f", estInt)
        }
    }

    val currentPrinc = principalInput.toDoubleOrNull() ?: 0.0
    val projectedBalance = maxOf(0.0, debt.balance - currentPrinc)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("💳 Log Payment", fontWeight = FontWeight.Bold)
                Text(
                    text = "${debt.name} • Current Balance: ${String.format(Locale.US, "$%,.2f", debt.balance)}",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = paymentInput,
                    onValueChange = { paymentInput = it },
                    label = { Text("Total Payment Amount ($)") },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )

                // Quick buttons for Minimum and Full Payoff
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SuggestionChip(
                        onClick = {
                            paymentInput = String.format(Locale.US, "%.2f", debt.minimumPayment)
                        },
                        label = { Text("Min ($${String.format(Locale.US, "%.0f", debt.minimumPayment)})", fontSize = 10.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            paymentInput = String.format(Locale.US, "%.2f", debt.balance)
                        },
                        label = { Text("Payoff in Full", fontSize = 10.sp) }
                    )
                }

                // Principal vs Interest Breakdown Card
                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Principal vs. Interest Breakdown", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            TextButton(
                                onClick = { isManualSplit = !isManualSplit },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(if (isManualSplit) "Auto-Split" else "Edit Split", fontSize = 9.5.sp)
                            }
                        }

                        if (!isManualSplit) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Principal Paid:", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                Text(String.format(Locale.US, "$%,.2f", currentPrinc), fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Interest Portion:", fontSize = 10.sp, color = Color(0xFFEF4444))
                                Text(String.format(Locale.US, "$%,.2f", interestInput.toDoubleOrNull() ?: 0.0), fontSize = 10.sp, color = Color(0xFFEF4444))
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = principalInput,
                                    onValueChange = { principalInput = it },
                                    label = { Text("Principal ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = interestInput,
                                    onValueChange = { interestInput = it },
                                    label = { Text("Interest ($)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("New Balance Preview:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text(
                                String.format(Locale.US, "$%,.2f", projectedBalance),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (projectedBalance <= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Confirmation (Optional)") },
                    placeholder = { Text("e.g. Cycle surplus extra, Conf #1234") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = createExpenseTransaction,
                        onCheckedChange = { createExpenseTransaction = it },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Record as Expense in Budget Ledger",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val p = paymentInput.toDoubleOrNull()
                    val princ = principalInput.toDoubleOrNull() ?: 0.0
                    val intAmt = interestInput.toDoubleOrNull() ?: 0.0

                    if (p == null || p <= 0) {
                        errorMessage = "Please enter a valid payment amount."
                    } else {
                        onConfirm(p, princ, intAmt, note.trim(), createExpenseTransaction)
                    }
                },
                modifier = Modifier.testTag("confirm_log_payment_button")
            ) {
                Text("Log Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
