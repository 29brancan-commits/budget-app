package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CashFlowForecastResult
import com.example.data.GeminiFinancialService
import com.example.data.PayPeriod
import com.example.data.SubscriptionAuditResult
import com.example.data.WhatIfSimulationResult
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

@Composable
fun AiFinancialHubView(
    uiState: BudgetUiState,
    onOpenQuickEntry: () -> Unit,
    onOpenReceiptScanner: () -> Unit,
    modifier: Modifier = Modifier,
    isWideScreen: Boolean = false
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "💬 AI Financial Chat",
        "🔮 Cash Flow & Safe-Spend",
        "🧪 Scenario Simulator",
        "🔍 Subscription Auditor"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "AI Financial Intelligence",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Predictive Insights • Scenario Modeling • Auditing",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Gemini 3.5 Flash",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onOpenQuickEntry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_hub_open_quick_entry"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("💬 Quick Entry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onOpenReceiptScanner,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_hub_open_receipt_scanner"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("🧾 Scan Receipt", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sub-tabs navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> FullDataChatBotView(uiState = uiState, isWideScreen = isWideScreen)
            1 -> CashFlowForecasterSection(uiState = uiState, isWideScreen = isWideScreen)
            2 -> WhatIfSimulatorSection(uiState = uiState, isWideScreen = isWideScreen)
            3 -> SubscriptionAuditorSection(uiState = uiState, isWideScreen = isWideScreen)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SECTION 1: Cash Flow & Safe-to-Spend Advisor
// ------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CashFlowForecasterSection(uiState: BudgetUiState, isWideScreen: Boolean = false) {
    val coroutineScope = rememberCoroutineScope()
    val currentPeriod = uiState.currentPayPeriod ?: uiState.payPeriods.firstOrNull()

    var forecastResult by remember { mutableStateOf<CashFlowForecastResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun runForecast() {
        if (currentPeriod == null) return
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = GeminiFinancialService.generateCashFlowForecast(
                payPeriod = currentPeriod,
                recentTransactions = uiState.allTransactions,
                futurePayPeriods = uiState.futurePayPeriods
            )
            isLoading = false
            res.onSuccess {
                forecastResult = it
            }.onFailure {
                errorMessage = it.message ?: "Failed to generate forecast."
            }
        }
    }

    LaunchedEffect(currentPeriod?.payday) {
        if (currentPeriod != null && forecastResult == null && !isLoading) {
            runForecast()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentPeriod == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "No active pay cycle found to forecast. Set an anchor payday in Settings.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        // Action header with Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Active Cycle: ${currentPeriod.startDate} – ${currentPeriod.endDate}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Net Buffer: $${String.format(Locale.US, "%,.2f", currentPeriod.remainingBalance)}",
                    fontSize = 12.sp,
                    color = if (currentPeriod.remainingBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = { runForecast() },
                enabled = !isLoading,
                modifier = Modifier.testTag("ai_refresh_forecast_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Forecast")
            }
        }

        if (isLoading) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Analyzing upcoming bills and daily burn rate with Gemini...", fontSize = 13.sp)
                }
            }
        }

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
            ) {
                Text(
                    text = errorMessage!!,
                    modifier = Modifier.padding(14.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp
                )
            }
        }

        forecastResult?.let { res ->
            val safeToSpendCards: @Composable () -> Unit = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("Safe to Spend / Day", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "$%,.2f", res.safeToSpendPerDay),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
                                Text("Weekly Safe Budget", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(Locale.US, "$%,.2f", res.safeToSpendPerWeek),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            val diagnosisCard: @Composable () -> Unit = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                            Text("Risk Diagnosis:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (res.riskLevel.uppercase()) {
                                    "LOW" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    "MODERATE" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                    else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                }
                            ) {
                                Text(
                                    text = "${res.riskLevel} RISK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (res.riskLevel.uppercase()) {
                                        "LOW" -> Color(0xFF059669)
                                        "MODERATE" -> Color(0xFFD97706)
                                        else -> Color(0xFFDC2626)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(res.summary, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Text("Strategic Action Items:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        res.recommendations.forEach { rec ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("•", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Text(rec, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        if (res.criticalDates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Text("High-impact dates to monitor:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                            }
                            res.criticalDates.forEach { cd ->
                                Text("⚠️ $cd", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            val horizonCard: @Composable () -> Unit = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "How to Spend Remaining Amounts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "3-Month Horizon",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (res.spendingAllocationTips.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                res.spendingAllocationTips.forEach { tip ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                        )
                                        Text(
                                            text = tip,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("3-Mo Income", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", res.threeMonthIncome),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("3-Mo Expenses", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", res.threeMonthExpenses),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("3-Mo Net", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", res.threeMonthNetSavings),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (res.threeMonthNetSavings >= 0) Color(0xFF10B981) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }

                        if (res.threeMonthOutlook.isNotBlank()) {
                            Text(
                                text = res.threeMonthOutlook,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (res.monthlyBreakdown.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Monthly Horizon Breakdown:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                res.monthlyBreakdown.forEach { mb ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mb.monthName,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Inc: $${String.format(Locale.US, "%,.0f", mb.projectedIncome)} | Exp: $${String.format(Locale.US, "%,.0f", mb.projectedExpenses)}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${if (mb.projectedNet >= 0) "+" else ""}$${String.format(Locale.US, "%,.0f", mb.projectedNet)}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (mb.projectedNet >= 0) Color(0xFF059669) else Color(0xFFDC2626)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isWideScreen) {
                // Galaxy Z Fold 8 Unfolded: Side-by-Side Dual Column View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        safeToSpendCards()
                        diagnosisCard()
                    }
                    Column(
                        modifier = Modifier.weight(1.05f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        horizonCard()
                    }
                }
            } else {
                safeToSpendCards()
                diagnosisCard()
                horizonCard()
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SECTION 2: AI What-If Scenario Simulator
// ------------------------------------------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhatIfSimulatorSection(uiState: BudgetUiState, isWideScreen: Boolean = false) {
    val coroutineScope = rememberCoroutineScope()
    var scenarioInput by remember { mutableStateOf("") }
    var simulationResult by remember { mutableStateOf<WhatIfSimulationResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val presetScenarios = listOf(
        "What if I cut dining out by $150/month?",
        "What if I receive a $600 tax refund next cycle?",
        "What if my car insurance increases by $35/month?",
        "What if I pay an extra $200 towards my highest-APR credit card?",
        "What if I cancel two unused subscriptions saving $30/month?"
    )

    fun runScenario(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = GeminiFinancialService.simulateWhatIfScenario(
                scenarioQuery = trimmed,
                payPeriods = uiState.payPeriods,
                debts = uiState.debts,
                transactions = uiState.allTransactions
            )
            isLoading = false
            res.onSuccess {
                simulationResult = it
            }.onFailure {
                errorMessage = it.message ?: "Simulation failed. Please try again."
            }
        }
    }

    val inputSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Model hypothetical financial decisions, pay adjustments, spending cuts, or unexpected bills to see their compounded impact on your cash flow and debt freedom.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = scenarioInput,
                onValueChange = { scenarioInput = it },
                placeholder = { Text("e.g., What if I save $100 on groceries each pay period?", fontSize = 13.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("what_if_input_field"),
                trailingIcon = {
                    if (scenarioInput.isNotBlank()) {
                        IconButton(
                            onClick = { runScenario(scenarioInput) },
                            enabled = !isLoading,
                            modifier = Modifier.testTag("what_if_submit_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Simulate",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp)
            )

            // Preset chips
            Text(
                text = "Quick Inspiration Scenarios:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetScenarios.forEach { sc ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clickable {
                            scenarioInput = sc
                            runScenario(sc)
                        }
                    ) {
                        Text(
                            text = sc,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    val resultSection: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Running financial simulation models...", fontSize = 12.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Text(errorMessage!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            simulationResult?.let { sim ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("what_if_result_card"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sim.scenario, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = sim.feasibilityAssessment,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        // Metrics row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Monthly", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", sim.monthlyImpact),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sim.monthlyImpact >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("3-Months", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", sim.threeMonthImpact),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sim.threeMonthImpact >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Annual Compounded", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", sim.annualSavings),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (sim.annualSavings >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        Text("Key Simulation Findings:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        sim.keyTakeaways.forEach { tk ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("💡", fontSize = 11.sp)
                                Text(tk, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(sim.strategicAdvice, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    inputSection()
                }
                Box(modifier = Modifier.weight(1.15f)) {
                    resultSection()
                }
            }
        } else {
            inputSection()
            resultSection()
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SECTION 3: Subscription & Price Creep Auditor
// ------------------------------------------------------------------------------------------------
@Composable
fun SubscriptionAuditorSection(uiState: BudgetUiState, isWideScreen: Boolean = false) {
    val coroutineScope = rememberCoroutineScope()
    var auditResult by remember { mutableStateOf<SubscriptionAuditResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun runAudit() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val res = GeminiFinancialService.auditSubscriptionsAndPriceCreep(uiState.allTransactions)
            isLoading = false
            res.onSuccess {
                auditResult = it
            }.onFailure {
                errorMessage = it.message ?: "Subscription audit failed."
            }
        }
    }

    LaunchedEffect(Unit) {
        if (auditResult == null && !isLoading && uiState.allTransactions.isNotEmpty()) {
            runAudit()
        }
    }

    val headerAndSummary: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Subscription & Recurring Auditor", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Identifies hidden bills, duplicates, & price creep", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                IconButton(
                    onClick = { runAudit() },
                    enabled = !isLoading,
                    modifier = Modifier.testTag("ai_refresh_audit_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Run Audit")
                }
            }

            if (isLoading) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Auditing recurring transactions and price changes...", fontSize = 12.sp)
                    }
                }
            }

            if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                ) {
                    Text(errorMessage!!, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }

            auditResult?.let { audit ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Recurring Drain", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                text = String.format(Locale.US, "$%,.2f/mo", audit.totalMonthlySubscriptionSpend),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Annual Total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
                            Text(
                                text = String.format(Locale.US, "$%,.2f/yr", audit.totalAnnualSubscriptionSpend),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📋 " + audit.overallInsights,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }

    val itemsList: @Composable () -> Unit = {
        auditResult?.let { audit ->
            if (audit.items.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "No recurring subscriptions detected yet. When you add bills or recurring expenses, Gemini will audit price changes over time.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Detected Subscriptions & Recurring Bills (${audit.items.size}):", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    audit.items.forEach { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            border = if (item.isCreeping) BorderStroke(1.dp, Color(0xFFF59E0B)) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(item.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        if (item.isCreeping) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(11.dp))
                                                    Text("PRICE CREEP", fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                                                }
                                            }
                                        }
                                    }

                                    Text(
                                        text = String.format(Locale.US, "$%,.2f", item.currentAmount),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Frequency: ${item.frequency} • $${String.format(Locale.US, "%,.0f", item.annualCost)}/yr", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (item.priceChangePct != null) {
                                        Text(
                                            text = "+${String.format(Locale.US, "%.1f", item.priceChangePct)}% increase",
                                            fontSize = 10.sp,
                                            color = Color(0xFFD97706),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text("💡 " + item.recommendation, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isWideScreen) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    headerAndSummary()
                }
                Box(modifier = Modifier.weight(1.25f)) {
                    itemsList()
                }
            }
        } else {
            headerAndSummary()
            itemsList()
        }
    }
}
