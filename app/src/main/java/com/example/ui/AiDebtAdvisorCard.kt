package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtEntity
import com.example.data.PayoffStrategy
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiDebtAdvisorCard(
    debts: List<DebtEntity>,
    selectedStrategy: PayoffStrategy,
    extraMonthlyBudget: Double,
    targetMonthsGoal: Int,
    isTargetTimelineMode: Boolean,
    aiResult: String?,
    isLoading: Boolean,
    errorMessage: String?,
    onGenerateSuggestions: (customPrompt: String?, windfall: Double, consolidationApr: Double?) -> Unit,
    onClearSuggestions: () -> Unit,
    isDark: Boolean = false
) {
    var customPromptText by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(true) }
    var showScenarioModeling by remember { mutableStateOf(false) }
    var windfallInput by remember { mutableStateOf("") }
    var consolidationAprInput by remember { mutableStateOf("") }

    // Balance Transfer Simulator State
    var showBalanceTransferSim by remember { mutableStateOf(false) }
    val defaultTransferBal = remember(debts) {
        debts.maxByOrNull { it.interestRate }?.balance?.let { String.format(java.util.Locale.US, "%.0f", it) } ?: "3500"
    }
    val defaultCurrentApr = remember(debts) {
        debts.maxByOrNull { it.interestRate }?.interestRate?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "22.9"
    }
    var transferBalanceInput by remember { mutableStateOf(defaultTransferBal) }
    var transferFeeInput by remember { mutableStateOf("3.0") }
    var promoAprInput by remember { mutableStateOf("0.0") }
    var promoDurationInput by remember { mutableStateOf("15") }
    var currentAprInput by remember { mutableStateOf(defaultCurrentApr) }
    var targetPaymentInput by remember { mutableStateOf("250") }
    var balanceTransferAiReport by remember { mutableStateOf<String?>(null) }
    var isTransferAiLoading by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val promptPresets = listOf(
        "⚡ Quick Win Strategy" to "How can I get my first debt payoff quick win as fast as possible?",
        "💰 Max Interest Savings" to "What is the exact mathematical strategy to save the maximum amount of interest on my debts?",
        "⏳ Promo APR Cliff" to "Which debts have promotional APRs expiring soon, and what monthly pace is required to clear them before standard rates hit?",
        "📌 Pinned Priorities" to "How do my custom pinned priority debts affect my overall payoff timeline and interest charges?",
        "🔄 Refinance & Consolidate" to "Which of my debts are candidates for balance transfers or consolidation loans?",
        "⏱️ Shave Off 6 Months" to "How much extra per month do I need to pay to cut 6 months off my overall payoff timeline?"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_debt_advisor_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF13111C) else Color(0xFFF3F0FF)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, Color(0xFF8B5CF6))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
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
                        color = Color(0xFF8B5CF6),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🤖", fontSize = 16.sp)
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Gemini AI Debt Advisor",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF8B5CF6).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "3.5 FLASH",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8B5CF6),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Smart suggestions & strategies to eliminate debt faster",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.testTag("toggle_ai_card_button")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle AI Advisor"
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Quick Action Presets
                    Text(
                        text = "Suggested Questions:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(promptPresets) { (label, prompt) ->
                            SuggestionChip(
                                onClick = {
                                    customPromptText = prompt
                                    keyboardController?.hide()
                                    val windfallVal = windfallInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                    val consolAprVal = consolidationAprInput.toDoubleOrNull()?.coerceIn(0.0, 100.0)
                                    onGenerateSuggestions(prompt, windfallVal, consolAprVal)
                                },
                                label = { Text(label, fontSize = 10.5.sp, fontWeight = FontWeight.Medium) },
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.5f)),
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isDark) Color(0xFF231E33) else Color.White
                                )
                            )
                        }
                    }

                    // Scenario Modeling Toggle (Windfall / Lump-Sum & Balance Transfer / Consolidation)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF1E1A2E) else Color.White,
                        border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showScenarioModeling = !showScenarioModeling },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🧪", fontSize = 13.sp)
                                    Text(
                                        text = "Simulate Lump-Sum Windfall & Balance Transfer",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                                Icon(
                                    imageVector = if (showScenarioModeling) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle Scenarios",
                                    tint = Color(0xFF8B5CF6),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            AnimatedVisibility(visible = showScenarioModeling) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // 1. Lump-Sum Windfall Modeling
                                    Text(
                                        text = "💵 Lump-Sum Windfall Modeling (Tax Refund, Bonus, Gift):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    // Quick windfall chips
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(
                                            "500" to "+$500 Bonus",
                                            "1200" to "+$1,200 Refund",
                                            "2500" to "+$2,500 Windfall",
                                            "5000" to "+$5,000"
                                        ).forEach { (amt, label) ->
                                            FilterChip(
                                                selected = windfallInput == amt,
                                                onClick = { windfallInput = if (windfallInput == amt) "" else amt },
                                                label = { Text(label, fontSize = 9.5.sp) }
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = windfallInput,
                                        onValueChange = { windfallInput = it },
                                        label = { Text("Custom Windfall Amount ($)", fontSize = 10.sp) },
                                        placeholder = { Text("e.g. 1500", fontSize = 10.sp) },
                                        prefix = { Text("$ ", fontSize = 10.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("ai_windfall_input")
                                    )

                                    val windfallAmountVal = windfallInput.toDoubleOrNull() ?: 0.0
                                    val targetWindfallDebt = debts.filter { it.customPriorityRank > 0 }.minByOrNull { it.customPriorityRank }
                                        ?: (if (selectedStrategy == PayoffStrategy.AVALANCHE) debts.maxByOrNull { it.interestRate } else debts.minByOrNull { it.balance })

                                    if (windfallAmountVal > 0.0 && targetWindfallDebt != null) {
                                        val newBal = (targetWindfallDebt.balance - windfallAmountVal).coerceAtLeast(0.0)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "⚡ Windfall Projection: Applying $${String.format(java.util.Locale.US, "%,.0f", windfallAmountVal)} to ${targetWindfallDebt.name}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857)
                                                )
                                                Text(
                                                    text = if (newBal <= 0) "🎉 Completely eliminates ${targetWindfallDebt.name} ($${String.format(java.util.Locale.US, "%,.2f", targetWindfallDebt.balance)}), instantly freeing up $${String.format(java.util.Locale.US, "%,.2f", targetWindfallDebt.minimumPayment)}/mo in cash flow!" else "Slashes balance from $${String.format(java.util.Locale.US, "%,.2f", targetWindfallDebt.balance)} down to $${String.format(java.util.Locale.US, "%,.2f", newBal)}, dramatically accelerating debt freedom.",
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                    // 2. Balance Transfer & Consolidation Simulator
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showBalanceTransferSim = !showBalanceTransferSim },
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("🔄", fontSize = 12.sp)
                                            Text(
                                                text = "0% Balance Transfer Simulator",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF8B5CF6)
                                            )
                                        }
                                        Text(
                                            text = if (showBalanceTransferSim) "Hide Simulator" else "Open Simulator",
                                            fontSize = 10.sp,
                                            color = Color(0xFF8B5CF6),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    AnimatedVisibility(visible = showBalanceTransferSim) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(
                                                text = "Test moving a high-interest card to a 0% Promo APR balance transfer card with fee breakdown:",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = transferBalanceInput,
                                                    onValueChange = { transferBalanceInput = it },
                                                    label = { Text("Transfer Balance ($)", fontSize = 10.sp) },
                                                    prefix = { Text("$ ", fontSize = 10.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedTextField(
                                                    value = transferFeeInput,
                                                    onValueChange = { transferFeeInput = it },
                                                    label = { Text("Fee (%)", fontSize = 10.sp) },
                                                    suffix = { Text("%", fontSize = 10.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(0.8f)
                                                )
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedTextField(
                                                    value = promoDurationInput,
                                                    onValueChange = { promoDurationInput = it },
                                                    label = { Text("Promo Months", fontSize = 10.sp) },
                                                    suffix = { Text("mo", fontSize = 10.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                OutlinedTextField(
                                                    value = currentAprInput,
                                                    onValueChange = { currentAprInput = it },
                                                    label = { Text("Current Card APR", fontSize = 10.sp) },
                                                    suffix = { Text("%", fontSize = 10.sp) },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }

                                            val pBal = transferBalanceInput.toDoubleOrNull() ?: 0.0
                                            val pFee = transferFeeInput.toDoubleOrNull() ?: 3.0
                                            val pDur = promoDurationInput.toIntOrNull() ?: 15
                                            val pApr = currentAprInput.toDoubleOrNull() ?: 22.0
                                            val upfrontFee = pBal * (pFee / 100.0)
                                            val newBalance = pBal + upfrontFee
                                            val estCurrentInterest = pBal * ((pApr / 100.0) / 12.0) * pDur
                                            val estNetSavings = estCurrentInterest - upfrontFee
                                            val requiredMonthly = if (pDur > 0) newBalance / pDur else 0.0

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isDark) Color(0xFF231E33) else Color(0xFFF3F0FF),
                                                border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.3f)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Transfer Fee:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("$$upfrontFee (${pFee}%)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Interest Avoided:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("~$${String.format(java.util.Locale.US, "%,.2f", estCurrentInterest)}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Estimated Net Savings:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = (if (estNetSavings > 0) "+$" else "-$") + String.format(java.util.Locale.US, "%,.2f", Math.abs(estNetSavings)),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (estNetSavings > 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                                        )
                                                    }
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                        Text("Pace to clear before promo ends:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        Text("$$${String.format(java.util.Locale.US, "%,.2f", requiredMonthly)}/mo", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                                    }
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        isTransferAiLoading = true
                                                        val res = com.example.data.GeminiDebtAdvisorService.simulateBalanceTransfer(
                                                            debts = debts,
                                                            transferBalance = pBal,
                                                            transferFeePercent = pFee,
                                                            promoApr = promoAprInput.toDoubleOrNull() ?: 0.0,
                                                            promoDurationMonths = pDur,
                                                            targetMonthlyPayment = targetPaymentInput.toDoubleOrNull() ?: requiredMonthly,
                                                            currentApr = pApr
                                                        )
                                                        balanceTransferAiReport = res.getOrNull() ?: "Simulation complete."
                                                        isTransferAiLoading = false
                                                    }
                                                },
                                                enabled = !isTransferAiLoading && pBal > 0,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isTransferAiLoading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Analyzing Transfer with Gemini...", fontSize = 10.5.sp)
                                                } else {
                                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Get Gemini Deep Transfer Analysis", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            if (!balanceTransferAiReport.isNullOrBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isDark) Color(0xFF1E1A2E) else Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Text("Gemini Transfer Verdict:", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                                                        Text(text = balanceTransferAiReport ?: "", fontSize = 10.sp, lineHeight = 14.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Custom Prompt Field + Generate Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customPromptText,
                            onValueChange = { customPromptText = it },
                            placeholder = { Text("Ask AI advisor a question about your debt...", fontSize = 11.5.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                val windfallVal = windfallInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val consolAprVal = consolidationAprInput.toDoubleOrNull()?.coerceIn(0.0, 100.0)
                                onGenerateSuggestions(customPromptText.ifBlank { null }, windfallVal, consolAprVal)
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ai_debt_custom_prompt_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                keyboardController?.hide()
                                val windfallVal = windfallInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                val consolAprVal = consolidationAprInput.toDoubleOrNull()?.coerceIn(0.0, 100.0)
                                onGenerateSuggestions(customPromptText.ifBlank { null }, windfallVal, consolAprVal)
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("generate_ai_suggestions_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ask AI", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Loading indicator text state
                    if (isLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E192E) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF8B5CF6),
                                    strokeWidth = 2.5.dp
                                )
                                Column {
                                    Text(
                                        text = "Gemini AI is analyzing your debt portfolio...",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Evaluating APR compounding, Snowball vs Avalanche math, and repayment milestones",
                                        fontSize = 9.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Error display
                    if (errorMessage != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AI Advisor Notice",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = errorMessage,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }

                    // AI Result Suggestions Output
                    if (!aiResult.isNullOrBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF1E1A2E) else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f))
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
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Psychology,
                                            contentDescription = null,
                                            tint = Color(0xFF8B5CF6),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "AI Payoff Recommendations",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Debt Suggestions", aiResult))
                                                Toast.makeText(context, "Suggestions copied to clipboard", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy suggestions",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        IconButton(
                                            onClick = onClearSuggestions,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Clear suggestions",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = aiResult,
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
