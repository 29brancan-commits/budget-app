package com.example.ui.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PayPeriod
import java.util.Locale

/**
 * GlanceModeBar: Quick toggle between "Glance Mode" (all cards collapsed)
 * and "Deep Dive Mode" (all cards expanded).
 */
@Composable
fun GlanceModeBar(
    allExpanded: Boolean,
    onToggleAll: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (allExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier.size(20.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (allExpanded) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = if (allExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = if (allExpanded) "Deep Dive View" else "Glance View (Minimal)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (allExpanded) "• All details open" else "• Tap any card to expand",
                    fontSize = 9.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onToggleAll(!allExpanded)
                    }
                    .testTag("toggle_glance_mode_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                        contentDescription = if (allExpanded) "Collapse All" else "Expand All",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (allExpanded) "Collapse All" else "Expand All",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * BalanceWaterfallCard: Visual Equation breakdown reconciling Account Starting Balance,
 * Cycle Income, Scheduled Bills Due, and Other Expenses into Net Available.
 */
@Composable
fun BalanceWaterfallCard(
    period: PayPeriod,
    activeAccountName: String,
    totalBillsDue: Double,
    totalBillsPaid: Double,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    val startBalance = if (period.startingBalance != 0.0) period.startingBalance else period.carryoverFromPrevious
    val earnedIncome = period.totalIncome
    val totalAvailable = startBalance + earnedIncome
    val billsDue = totalBillsDue
    val otherExp = maxOf(0.0, period.totalExpenses - totalBillsDue - totalBillsPaid)
    val totalExp = period.totalExpenses
    val netRemaining = period.remainingBalance

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Header toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded }
                    .testTag("balance_waterfall_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Functions,
                        contentDescription = "Waterfall Equation",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "BALANCE RECONCILIATION EQUATION",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!isExpanded) {
                        Text(
                            text = String.format(Locale.US, "$%,.0f start + $%,.0f inc − $%,.0f exp = $%,.0f", startBalance, earnedIncome, totalExp, netRemaining),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Equation" else "Expand Equation",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Step 1: Starting Balance
                    WaterfallRow(
                        prefix = "1.",
                        label = if (activeAccountName != "All Accounts" && activeAccountName.isNotBlank()) "$activeAccountName Starting Balance" else "Account Starting Balance",
                        amount = startBalance,
                        isPositive = startBalance >= 0,
                        tag = "Not cycle income"
                    )

                    // Step 2: Cycle Income
                    WaterfallRow(
                        prefix = "+",
                        label = "Cycle Paycheck & Earned Income",
                        amount = earnedIncome,
                        isPositive = true,
                        tag = "Cycle Earned"
                    )

                    // Subtotal: Total Available
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "= Total Available to Spend in Account",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = String.format(Locale.US, "$%,.2f", totalAvailable),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Step 3: Scheduled Bills Due
                    WaterfallRow(
                        prefix = "−",
                        label = "Scheduled Recurring Bills Due",
                        amount = billsDue,
                        isPositive = false,
                        tag = "Fixed Bills"
                    )

                    // Step 4: Other Cycle Expenses
                    if (otherExp > 0.0) {
                        WaterfallRow(
                            prefix = "−",
                            label = "Other Cycle Allocations / One-Offs",
                            amount = otherExp,
                            isPositive = false,
                            tag = "Buffer / Extra"
                        )
                    }

                    // Final Net Equation Result
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (netRemaining >= 0) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, if (netRemaining >= 0) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (netRemaining >= 0) "✅ Net Remaining Cash After All Expenses" else "⚠️ Projected Shortfall After All Expenses",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (netRemaining >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                                )
                                Text(
                                    text = if (netRemaining >= 0) "All bills funded with remaining surplus" else "Account will need additional funding before end of cycle",
                                    fontSize = 8.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = String.format(Locale.US, "%s$%,.2f", if (netRemaining >= 0) "+" else "", netRemaining),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (netRemaining >= 0) Color(0xFF047857) else Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterfallRow(
    prefix: String,
    label: String,
    amount: Double,
    isPositive: Boolean,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = prefix,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                shape = RoundedCornerShape(3.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    text = tag,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }
        }

        Text(
            text = "${if (isPositive) "+" else "−"}${String.format(Locale.US, "$%,.2f", Math.abs(amount))}",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (isPositive) Color(0xFF047857) else Color(0xFFDC2626)
        )
    }
}
