package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import com.example.model.SavingsGoal
import java.util.Locale

/**
 * Savings Goal Component for the Transactions / Pay Cycles tab.
 * Lets users set target amounts, categories (e.g. "Vacation Fund"),
 * and visually tracks progress via a sleek animated progress bar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SavingsGoalComponent(
    savingsGoals: List<SavingsGoal>,
    transactions: List<TransactionEntity> = emptyList(),
    onAddGoal: (title: String, targetAmount: Double, category: String, currentAmount: Double, emoji: String, colorHex: Long, targetDate: String?, notes: String) -> Unit,
    onUpdateGoal: (SavingsGoal) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onContribute: (goalId: String, amount: Double, addAsTransaction: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false,
    isCompact: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(defaultExpanded) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    var contributingGoal by remember { mutableStateOf<SavingsGoal?>(null) }
    val haptic = LocalHapticFeedback.current

    // Summary calculation for collapsed preview
    val firstGoal = savingsGoals.firstOrNull()
    val firstGoalSaved = remember(firstGoal, transactions) {
        if (firstGoal == null) 0.0 else {
            val matching = transactions.filter { tx ->
                val catMatch = tx.category.trim().equals(firstGoal.category.trim(), ignoreCase = true) ||
                        (firstGoal.category.isNotBlank() && tx.category.contains(firstGoal.category, ignoreCase = true))
                val titleMatch = firstGoal.title.isNotBlank() && tx.title.contains(firstGoal.title, ignoreCase = true)
                (catMatch || titleMatch) && !tx.title.contains("Cancelled", ignoreCase = true)
            }
            firstGoal.currentAmount + matching.sumOf { tx -> if (tx.isIncome) tx.amount else tx.amount }
        }
    }
    val firstGoalPercent = if (firstGoal != null && firstGoal.targetAmount > 0) {
        ((firstGoalSaved / firstGoal.targetAmount) * 100).coerceAtLeast(0.0)
    } else 0.0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_goals_component_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row (Clickable to collapse/expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        isExpanded = !isExpanded
                    }
                    .testTag("savings_goals_header_toggle"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🎯", fontSize = 18.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Savings Goals",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (savingsGoals.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${savingsGoals.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }

                        if (!isExpanded && firstGoal != null) {
                            Text(
                                text = "${firstGoal.emoji} ${firstGoal.title}: ${String.format(Locale.US, "$%,.0f / $%,.0f (%.0f%%)", firstGoalSaved, firstGoal.targetAmount, firstGoalPercent)} • Tap to expand",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = if (isExpanded) "Set targets & track fund progress (Vacation, Emergency, etc.)" else "Tap to expand savings targets",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            showAddDialog = true
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("add_savings_goal_btn")
                    ) {
                        Icon(
                            Icons.Default.AddCircle,
                            contentDescription = "Add Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (savingsGoals.isEmpty()) {
                        // Empty State with Quick Preset Suggestions
                        EmptySavingsGoalsState(
                            onAddCustom = { showAddDialog = true },
                            onSelectPreset = { title, target, cat, emoji, color ->
                                onAddGoal(title, target, cat, 0.0, emoji, color, null, "")
                            }
                        )
                    } else {
                        // List of Savings Goals
                        savingsGoals.forEach { goal ->
                            SavingsGoalItemCard(
                                goal = goal,
                                transactions = transactions,
                                onEdit = { editingGoal = goal },
                                onDelete = { onDeleteGoal(goal.id) },
                                onContributeClick = { contributingGoal = goal },
                                isCompact = isCompact
                            )
                        }

                        // Bottom Add Goal button
                        OutlinedButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_add_another_goal"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Set Another Savings Goal", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Add Goal Dialog
    if (showAddDialog) {
        SavingsGoalFormDialog(
            goalToEdit = null,
            onDismiss = { showAddDialog = false },
            onSave = { title, target, category, current, emoji, color, targetDate, notes ->
                onAddGoal(title, target, category, current, emoji, color, targetDate, notes)
                showAddDialog = false
            }
        )
    }

    // Edit Goal Dialog
    editingGoal?.let { goal ->
        SavingsGoalFormDialog(
            goalToEdit = goal,
            onDismiss = { editingGoal = null },
            onSave = { title, target, category, current, emoji, color, targetDate, notes ->
                onUpdateGoal(
                    goal.copy(
                        title = title,
                        targetAmount = target,
                        category = category,
                        currentAmount = current,
                        emoji = emoji,
                        colorHex = color,
                        targetDate = targetDate,
                        notes = notes,
                        isCompleted = current >= target
                    )
                )
                editingGoal = null
            }
        )
    }

    // Contribute Funds Dialog
    contributingGoal?.let { goal ->
        ContributeGoalDialog(
            goal = goal,
            onDismiss = { contributingGoal = null },
            onConfirm = { amount, addAsTx ->
                onContribute(goal.id, amount, addAsTx)
                contributingGoal = null
            }
        )
    }
}

/**
 * Individual Savings Goal Item Card with visual progress bar and controls.
 */
@Composable
fun SavingsGoalItemCard(
    goal: SavingsGoal,
    transactions: List<TransactionEntity>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onContributeClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Calculate total saved from direct contributions PLUS transactions in that category
    val matchingTransactions = remember(transactions, goal.category, goal.title) {
        transactions.filter { tx ->
            val catMatch = tx.category.trim().equals(goal.category.trim(), ignoreCase = true) ||
                    (goal.category.isNotBlank() && tx.category.contains(goal.category, ignoreCase = true))
            val titleMatch = goal.title.isNotBlank() && tx.title.contains(goal.title, ignoreCase = true)
            (catMatch || titleMatch) && !tx.title.contains("Cancelled", ignoreCase = true)
        }
    }

    val transactionsSaved = remember(matchingTransactions) {
        // Any positive transaction recorded in this category contributes to the goal
        matchingTransactions.sumOf { tx ->
            if (tx.isIncome) tx.amount else tx.amount
        }
    }

    val totalSaved = goal.currentAmount + transactionsSaved
    val progressRatio = (totalSaved / goal.targetAmount.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressRatio,
        animationSpec = tween(durationMillis = 800),
        label = "goal_progress"
    )

    val percent = ((totalSaved / goal.targetAmount.coerceAtLeast(1.0)) * 100).coerceAtLeast(0.0)
    val remaining = (goal.targetAmount - totalSaved).coerceAtLeast(0.0)
    val isCompleted = totalSaved >= goal.targetAmount

    val goalColor = Color(goal.colorHex)
    val progressBrush = Brush.horizontalGradient(
        colors = listOf(
            goalColor.copy(alpha = 0.85f),
            if (isCompleted) Color(0xFFF59E0B) else goalColor
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("savings_goal_item_${goal.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) Color(0xFF10B981).copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Emoji, Title, Category Badge, Action Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(goalColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(goal.emoji, fontSize = 18.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = goal.title,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isCompleted) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "🎉 Reached!",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Category: ${goal.category}",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (!goal.targetDate.isNullOrBlank()) {
                                Text(
                                    text = "• ${goal.targetDate}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Edit / Delete buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Goal",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Goal",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            // Financial Progress Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Saved so far",
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f", totalSaved),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = String.format(Locale.US, "of $%,.2f", goal.targetAmount),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Percentage Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = goalColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f%%", percent),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = goalColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // VISUAL PROGRESS BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(progressBrush)
                )
            }

            // Bottom Info: Remaining amount & Quick Contribute
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCompleted) "🎯 Target completed! Great job!"
                    else String.format(Locale.US, "$%,.2f remaining", remaining),
                    fontSize = 11.sp,
                    fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Add Contribution Button
                FilledTonalButton(
                    onClick = onContributeClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("btn_contribute_${goal.id}")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contribute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // If matching transactions were detected, show subtle note
            if (transactionsSaved > 0.0) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = String.format(Locale.US, "Includes $%,.2f from %d transactions in '%s'", transactionsSaved, matchingTransactions.size, goal.category),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Empty state with one-click templates for common savings goals.
 */
@Composable
fun EmptySavingsGoalsState(
    onAddCustom: () -> Unit,
    onSelectPreset: (title: String, target: Double, category: String, emoji: String, color: Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🏖️ 🎯 🚗", fontSize = 28.sp)
            Text(
                text = "No Savings Goals Set Yet",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Set a target amount and category (e.g. 'Vacation Fund' or 'Emergency Fund') to track progress visually with every transaction or contribution.",
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Quick Starter Goals:",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Preset suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    Triple("Vacation Fund", 1500.0, "🏖️"),
                    Triple("Emergency Fund", 3000.0, "🛡️"),
                    Triple("Holiday Shopping", 800.0, "🎁"),
                    Triple("Car Maintenance", 1000.0, "🚗"),
                    Triple("Down Payment", 5000.0, "🏠")
                )

                presets.forEach { (name, target, emoji) ->
                    OutlinedButton(
                        onClick = { onSelectPreset(name, target, name, emoji, 0xFF10B981L) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("$emoji $name ($${target.toInt()})", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Button(
                onClick = onAddCustom,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_create_first_goal")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Create Custom Savings Goal", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Dialog for adding or editing a Savings Goal.
 */
@Composable
fun SavingsGoalFormDialog(
    goalToEdit: SavingsGoal?,
    onDismiss: () -> Unit,
    onSave: (title: String, targetAmount: Double, category: String, currentAmount: Double, emoji: String, colorHex: Long, targetDate: String?, notes: String) -> Unit
) {
    var title by remember { mutableStateOf(goalToEdit?.title ?: "") }
    var targetText by remember { mutableStateOf(goalToEdit?.targetAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var category by remember { mutableStateOf(goalToEdit?.category ?: "") }
    var currentText by remember { mutableStateOf(goalToEdit?.currentAmount?.let { String.format(Locale.US, "%.2f", it) } ?: "0.00") }
    var targetDate by remember { mutableStateOf(goalToEdit?.targetDate ?: "") }
    var notes by remember { mutableStateOf(goalToEdit?.notes ?: "") }
    var selectedEmoji by remember { mutableStateOf(goalToEdit?.emoji ?: "🎯") }
    var selectedColor by remember { mutableStateOf(goalToEdit?.colorHex ?: 0xFF10B981L) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val emojis = listOf("🎯", "🏖️", "✈️", "🚗", "🏠", "💻", "🛡️", "🎁", "💍", "🎓", "📱", "⛺")
    val colors = listOf(
        0xFF10B981L, // Emerald
        0xFF3B82F6L, // Blue
        0xFF8B5CF6L, // Purple
        0xFFEC4899L, // Pink
        0xFFF59E0BL, // Amber
        0xFF06B6D4L, // Cyan
        0xFFEF4444L  // Red
    )

    val commonCategories = listOf("Vacation Fund", "Emergency Fund", "Savings", "Travel", "Electronics", "Car", "Home", "Gifts")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (goalToEdit != null) "Edit Savings Goal" else "New Savings Goal",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Goal Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (category.isBlank() || commonCategories.contains(category)) {
                            category = it
                        }
                    },
                    label = { Text("Goal Name (e.g. Vacation Fund)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_goal_title")
                )

                // Target Amount
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Target Amount ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_goal_target")
                )

                // Category
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category to Track") },
                        placeholder = { Text("e.g. Vacation Fund or Savings") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_goal_category")
                    )

                    // Quick category chips
                    Text("Popular Categories:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        commonCategories.forEach { cat ->
                            SuggestionChip(
                                onClick = { category = cat },
                                label = { Text(cat, fontSize = 10.5.sp) }
                            )
                        }
                    }
                }

                // Initial / Starting Saved Amount
                OutlinedTextField(
                    value = currentText,
                    onValueChange = { currentText = it },
                    label = { Text("Starting / Already Saved ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Target Date (optional)
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target Date (Optional)") },
                    placeholder = { Text("e.g. Dec 2026 or Summer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Choose Icon:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        emojis.forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }

                // Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Accent Color:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        colors.forEach { colHex ->
                            val c = Color(colHex)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .clickable { selectedColor = colHex }
                                    .then(
                                        if (selectedColor == colHex)
                                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }

                // Error Message if any
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val targetAmt = targetText.toDoubleOrNull()
                    val currentAmt = currentText.toDoubleOrNull() ?: 0.0

                    if (title.isBlank()) {
                        errorMessage = "Please enter a goal name."
                        return@Button
                    }
                    if (targetAmt == null || targetAmt <= 0.0) {
                        errorMessage = "Please enter a valid target amount greater than $0."
                        return@Button
                    }

                    val cat = if (category.isBlank()) title.trim() else category.trim()
                    onSave(
                        title.trim(),
                        targetAmt,
                        cat,
                        currentAmt.coerceAtLeast(0.0),
                        selectedEmoji,
                        selectedColor,
                        targetDate.trim().takeIf { it.isNotBlank() },
                        notes.trim()
                    )
                },
                modifier = Modifier.testTag("btn_save_goal")
            ) {
                Text("Save Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for adding a quick contribution to a Savings Goal.
 */
@Composable
fun ContributeGoalDialog(
    goal: SavingsGoal,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, addAsTransaction: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var addAsTransaction by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val quickAmounts = listOf(25.0, 50.0, 100.0, 250.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(goal.emoji, fontSize = 22.sp)
                Text("Contribute to ${goal.title}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "Current saved: $%,.2f of $%,.2f target", goal.currentAmount, goal.targetAmount),
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick Amount Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        FilledTonalButton(
                            onClick = { amountText = String.format(Locale.US, "%.2f", amt) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+$${amt.toInt()}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Amount Text Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Contribution Amount ($)") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_contribution_amount")
                )

                // Checkbox: Record as transaction
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { addAsTransaction = !addAsTransaction },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = addAsTransaction,
                        onCheckedChange = { addAsTransaction = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Also log as transaction under '${goal.category}'",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0.0) {
                        errorMessage = "Please enter a valid amount greater than $0."
                        return@Button
                    }
                    onConfirm(amt, addAsTransaction)
                },
                modifier = Modifier.testTag("btn_confirm_contribution")
            ) {
                Text("Add Contribution")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
