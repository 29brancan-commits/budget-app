package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class UpcomingFilter(val label: String) {
    CURRENT_CYCLE("This Pay Cycle"),
    NEXT_CYCLE("Next Cycle"),
    ALL("All"),
    DUE_SOON("🚨 Overdue / 48h"),
    AUTO_PAY("⚡ Auto-Pay")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingExpensesView(
    uiState: BudgetUiState,
    onToggleCompleted: (TransactionEntity) -> Unit,
    onOpenAddBill: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onDeleteExpense: (UpcomingExpenseItem) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDuplicateTransaction: ((TransactionEntity) -> Unit)? = null,
    onUpdateTransactionStatus: ((TransactionEntity, String) -> Unit)? = null,
    onToggleReminder: ((TransactionEntity, Int) -> Unit)? = null,
    onTestReminderNotification: (() -> Unit)? = null,
    isWideScreen: Boolean = false,
    initialFilter: UpcomingFilter? = null,
    onUndo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val dateFmt = remember { DateTimeFormatter.ofPattern("MMM d", Locale.US) }
    var selectedFilter by rememberSaveable { mutableStateOf(initialFilter ?: UpcomingFilter.CURRENT_CYCLE) }
    val upcomingSnackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(initialFilter) {
        if (initialFilter != null) {
            selectedFilter = initialFilter
        }
    }
    var itemToDelete by remember { mutableStateOf<UpcomingExpenseItem?>(null) }

    // Retrieve full cycle bundle
    val cycleBundle = remember(
        uiState.allTransactions,
        uiState.plannedExpenses,
        uiState.activeAccountName,
        uiState.anchorPayday,
        today
    ) {
        UpcomingExpenseCalculator.getExpensesForCurrentAndUpcomingCycles(
            allTransactions = uiState.allTransactions,
            plannedExpenses = uiState.plannedExpenses,
            activeAccountName = uiState.activeAccountName,
            anchor = uiState.anchorPayday,
            today = today
        )
    }

    // Retrieve all upcoming items
    val allUpcomingItems = remember(
        uiState.allTransactions,
        uiState.plannedExpenses,
        uiState.activeAccountName,
        uiState.currentPayPeriodStart,
        uiState.currentPayPeriodEnd,
        today,
        cycleBundle
    ) {
        val baseItems = UpcomingExpenseCalculator.getUpcomingExpenses(
            allTransactions = uiState.allTransactions,
            plannedExpenses = uiState.plannedExpenses,
            activeAccountName = uiState.activeAccountName,
            today = today,
            payPeriodStart = null,
            payPeriodEnd = null
        )
        (baseItems + cycleBundle.allExpenses)
            .distinctBy { item ->
                when {
                    item.plannedExpenseRef != null -> "plan_${item.plannedExpenseRef.id}_${item.dueDate}"
                    item.transactionRef != null && item.transactionRef.id > 0 -> "tx_${item.transactionRef.id}"
                    item.transactionRef != null -> "rec_${item.transactionRef.id}_${item.dueDate}"
                    else -> item.id
                }
            }
            .distinctBy { item ->
                if (item.plannedExpenseRef != null) {
                    "plan_title_${item.title.trim().lowercase()}_${item.dueDate}_${item.amount}_${item.accountName.trim().lowercase()}"
                } else if (item.transactionRef != null && item.transactionRef.id > 0) {
                    "tx_${item.transactionRef.id}"
                } else {
                    "${item.title.trim().lowercase()}_${item.dueDate}_${item.amount}_${item.accountName.trim().lowercase()}"
                }
            }
            .sortedBy { it.dueDate }
    }

    // Filter items based on user selection
    val filteredItems = remember(allUpcomingItems, selectedFilter, cycleBundle) {
        allUpcomingItems.filter { item ->
            when (selectedFilter) {
                UpcomingFilter.ALL -> true
                UpcomingFilter.CURRENT_CYCLE -> {
                    !item.dueDate.isBefore(cycleBundle.currentCycleStart) &&
                            !item.dueDate.isAfter(cycleBundle.currentCycleEnd)
                }
                UpcomingFilter.NEXT_CYCLE -> {
                    !item.dueDate.isBefore(cycleBundle.upcomingCycleStart) &&
                            !item.dueDate.isAfter(cycleBundle.upcomingCycleEnd)
                }
                UpcomingFilter.DUE_SOON -> {
                    val days = ChronoUnit.DAYS.between(today, item.dueDate)
                    days <= 2L // Overdue or due within next 48 hours
                }
                UpcomingFilter.AUTO_PAY -> item.isAutoPay
            }
        }
    }

    val overdueItems = remember(allUpcomingItems, today) {
        allUpcomingItems.filter { ChronoUnit.DAYS.between(today, it.dueDate) < 0L }
    }
    val dueWithin48hItems = remember(allUpcomingItems, today) {
        allUpcomingItems.filter { ChronoUnit.DAYS.between(today, it.dueDate) in 0L..2L }
    }
    val overdueOrDueSoonCount = overdueItems.size + dueWithin48hItems.size

    // Helper for Fold 8 adaptive filter labels
    fun getAdaptiveFilterLabel(filter: UpcomingFilter, isWide: Boolean): String = when (filter) {
        UpcomingFilter.CURRENT_CYCLE -> if (isWide) "This Pay Cycle" else "This Cycle"
        UpcomingFilter.NEXT_CYCLE -> "Next Cycle"
        UpcomingFilter.ALL -> "All"
        UpcomingFilter.DUE_SOON -> if (isWide) "🚨 Due in 48h / Overdue" else "🚨 Overdue/48h"
        UpcomingFilter.AUTO_PAY -> "⚡ Auto-Pay"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_expenses_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Undo Snackbar Host inside Upcoming Expenses View
        SnackbarHost(
            hostState = upcomingSnackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upcoming_expenses_snackbar_host")
        )

        // Top Header Banner with Cycle Summary (optimized for Fold 8 cover and inner screens)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideScreen) 16.dp else 12.dp, vertical = if (isWideScreen) 12.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cycleBills = remember(allUpcomingItems, cycleBundle) {
                    allUpcomingItems.filter {
                        !it.dueDate.isBefore(cycleBundle.currentCycleStart) && !it.dueDate.isAfter(cycleBundle.currentCycleEnd)
                    }
                }
                val cycleCommittedCash = remember(cycleBills) {
                    cycleBills.sumOf { it.amount }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 10.dp else 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isWideScreen) 40.dp else 34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Upcoming Bills",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(if (isWideScreen) 24.dp else 20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = if (isWideScreen) "Upcoming Bills & Expenses" else "Upcoming Bills",
                                fontSize = if (isWideScreen) 18.sp else 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isWideScreen) {
                                    "${cycleBundle.currentCycleStart.format(dateFmt)} – ${cycleBundle.currentCycleEnd.format(dateFmt)} • ${cycleBills.size} bills (${String.format(Locale.US, "$%,.2f", cycleCommittedCash)} remaining)"
                                } else {
                                    "${cycleBundle.currentCycleStart.format(dateFmt)} – ${cycleBundle.currentCycleEnd.format(dateFmt)} • ${cycleBills.size} bills ($${String.format(Locale.US, "%,.0f", cycleCommittedCash)})"
                                },
                                fontSize = if (isWideScreen) 12.sp else 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = onOpenAddBill,
                            contentPadding = PaddingValues(horizontal = if (isWideScreen) 12.dp else 8.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.testTag("btn_add_planned_bill")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(text = "Add Bill", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Sticky prominent subtotal summary bar for committed cash in current cycle
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isWideScreen) "Committed for Current Cycle:" else "Cycle Committed:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = String.format(Locale.US, "$%,.2f", cycleCommittedCash),
                            fontSize = if (isWideScreen) 14.sp else 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Upcoming Bill Due-Date Alerts & Overdue Badging
        if (overdueOrDueSoonCount > 0 && selectedFilter != UpcomingFilter.DUE_SOON) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Overdue Alert Card (Soft Crimson)
                if (overdueItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFilter = UpcomingFilter.DUE_SOON },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "⚠️ ${overdueItems.size} bill(s) are past-due overdue!",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF991B1B)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Total past-due: $%,.2f", overdueItems.sumOf { it.amount }),
                                        fontSize = 11.sp,
                                        color = Color(0xFFB91C1C)
                                    )
                                }
                            }
                            Text(
                                text = "View →",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }

                // Due within 48 Hours Alert Card (Warm Amber)
                if (dueWithin48hItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFilter = UpcomingFilter.DUE_SOON },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "⏳ ${dueWithin48hItems.size} bill(s) due within next 48 hours!",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Due soon: $%,.2f", dueWithin48hItems.sumOf { it.amount }),
                                        fontSize = 11.sp,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (onTestReminderNotification != null) {
                                    TextButton(
                                        onClick = onTestReminderNotification,
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "🔔 Test Alert",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFD97706)
                                        )
                                    }
                                }
                                Text(
                                    text = "View →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Filter Chips Carousel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UpcomingFilter.values().forEach { filter ->
                val isSelected = selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = filter },
                    label = {
                        Text(
                            text = getAdaptiveFilterLabel(filter, isWideScreen),
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // Section Title with Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${getAdaptiveFilterLabel(selectedFilter, isWideScreen)} (${filteredItems.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = if (isWideScreen) {
                    String.format(Locale.US, "Subtotal: $%,.2f", filteredItems.sumOf { it.amount })
                } else {
                    String.format(Locale.US, "Total: $%,.2f", filteredItems.sumOf { it.amount })
                },
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Items List or Empty State
        if (filteredItems.isEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "No Upcoming Expenses Found",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "You're all caught up! No unpaid bills scheduled for this criteria.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onOpenAddBill,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Planned Bill", fontSize = 13.sp)
                    }
                }
            }
        } else {
            // Group items by time period / urgency
            val groupedItems = remember(filteredItems, today) {
                val overdue = mutableListOf<UpcomingExpenseItem>()
                val dueToday = mutableListOf<UpcomingExpenseItem>()
                val thisWeek = mutableListOf<UpcomingExpenseItem>()
                val nextWeek = mutableListOf<UpcomingExpenseItem>()
                val later = mutableListOf<UpcomingExpenseItem>()

                filteredItems.forEach { item ->
                    val days = ChronoUnit.DAYS.between(today, item.dueDate)
                    when {
                        days < 0L -> overdue.add(item)
                        days == 0L -> dueToday.add(item)
                        days in 1L..6L -> thisWeek.add(item)
                        days in 7L..13L -> nextWeek.add(item)
                        else -> later.add(item)
                    }
                }

                listOf(
                    Triple(if (isWideScreen) "🚨 Overdue" else "Overdue", Color(0xFFDC2626), overdue),
                    Triple(if (isWideScreen) "⭐ Due Today" else "Due Today", Color(0xFFEA580C), dueToday),
                    Triple(if (isWideScreen) "📅 Later This Week" else "This Week", Color(0xFF2563EB), thisWeek),
                    Triple(if (isWideScreen) "📆 Next Week" else "Next Week", Color(0xFF7C3AED), nextWeek),
                    Triple(if (isWideScreen) "⏳ Later" else "Later", Color(0xFF4B5563), later)
                ).filter { it.third.isNotEmpty() }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                groupedItems.forEach { (groupTitle, groupColor, itemsInGroup) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = groupColor.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, groupColor.copy(alpha = 0.22f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = if (isWideScreen) 10.dp else 8.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(groupColor)
                                    )
                                    Text(
                                        text = groupTitle,
                                        fontSize = if (isWideScreen) 13.sp else 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = groupColor
                                    )
                                }
                                Text(
                                    text = "${itemsInGroup.size} ${if (itemsInGroup.size == 1) "bill" else "bills"} • ${String.format(Locale.US, "$%,.2f", itemsInGroup.sumOf { it.amount })}",
                                    fontSize = if (isWideScreen) 11.5.sp else 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isWideScreen && itemsInGroup.size > 1) {
                            // 2-column grid layout for Galaxy Z Fold 8 inner screen
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsInGroup.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { item ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                val itemTx = item.transactionRef ?: TransactionEntity(
                                                    id = 0,
                                                    title = item.title,
                                                    amount = item.amount,
                                                    dateMillis = DateUtils.localDateToMillis(item.dueDate),
                                                    category = item.category.ifEmpty { "Planned Bill" },
                                                    accountName = item.accountName,
                                                    isIncome = false,
                                                    isCompleted = false,
                                                    isAutoPay = item.isAutoPay
                                                )
                                                UpcomingExpenseItemCard(
                                                    item = item,
                                                    today = today,
                                                    dateFmt = dateFmt,
                                                    allAccounts = uiState.allAccounts,
                                                    isWideScreen = true,
                                                    onUpdateStatus = onUpdateTransactionStatus,
                                                    onToggleCompleted = { tx ->
                                                        val wasCompleted = tx.isCompleted
                                                        onToggleCompleted(tx)
                                                        if (!wasCompleted) {
                                                            coroutineScope.launch {
                                                                val result = upcomingSnackbarHostState.showSnackbar(
                                                                    message = "Marked \"${item.title}\" as paid",
                                                                    actionLabel = if (onUndo != null) "UNDO" else null,
                                                                    duration = SnackbarDuration.Short
                                                                )
                                                                if (result == SnackbarResult.ActionPerformed) {
                                                                    onUndo?.invoke()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    onDeleteClick = { itemToDelete = item },
                                                    onEditClick = { onEditTransaction(itemTx) },
                                                    onDuplicateClick = { onDuplicateTransaction?.invoke(itemTx) },
                                                    onToggleReminder = { days -> onToggleReminder?.invoke(itemTx, days) }
                                                )
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                itemsInGroup.forEach { item ->
                                    val itemTx = item.transactionRef ?: TransactionEntity(
                                        id = 0,
                                        title = item.title,
                                        amount = item.amount,
                                        dateMillis = DateUtils.localDateToMillis(item.dueDate),
                                        category = item.category.ifEmpty { "Planned Bill" },
                                        accountName = item.accountName,
                                        isIncome = false,
                                        isCompleted = false,
                                        isAutoPay = item.isAutoPay
                                    )
                                    UpcomingExpenseItemCard(
                                        item = item,
                                        today = today,
                                        dateFmt = dateFmt,
                                        allAccounts = uiState.allAccounts,
                                        isWideScreen = isWideScreen,
                                        onUpdateStatus = onUpdateTransactionStatus,
                                        onToggleCompleted = { tx ->
                                            val wasCompleted = tx.isCompleted
                                            onToggleCompleted(tx)
                                            if (!wasCompleted) {
                                                coroutineScope.launch {
                                                    val result = upcomingSnackbarHostState.showSnackbar(
                                                        message = "Marked \"${item.title}\" as paid",
                                                        actionLabel = if (onUndo != null) "UNDO" else null,
                                                        duration = SnackbarDuration.Short
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        onUndo?.invoke()
                                                    }
                                                }
                                            }
                                        },
                                        onDeleteClick = { itemToDelete = item },
                                        onEditClick = { onEditTransaction(itemTx) },
                                        onDuplicateClick = { onDuplicateTransaction?.invoke(itemTx) },
                                        onToggleReminder = { days -> onToggleReminder?.invoke(itemTx, days) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Delete Confirmation Dialog
    if (itemToDelete != null) {
        val target = itemToDelete!!
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Upcoming Expense",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Are you sure you want to remove \"${target.title}\"?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Amount: ${String.format(Locale.US, "$%,.2f", target.amount)} • Due: ${target.dueDate.format(dateFmt)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (target.isTransaction) {
                        Text(
                            text = "This will remove the transaction record.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "This will cancel this planned bill occurrence or template.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExpense(target)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_upcoming_expense")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UpcomingExpenseItemCard(
    item: UpcomingExpenseItem,
    today: LocalDate,
    dateFmt: DateTimeFormatter,
    allAccounts: List<String>,
    onToggleCompleted: (TransactionEntity) -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDuplicateClick: (() -> Unit)? = null,
    onToggleReminder: ((Int) -> Unit)? = null,
    onUpdateStatus: ((TransactionEntity, String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isWideScreen: Boolean = false
) {
    val formattedDueDate = item.dueDate.format(dateFmt)
    val daysBetween = ChronoUnit.DAYS.between(today, item.dueDate)
    val isDark = isSystemInDarkTheme()

    val targetTx = item.transactionRef ?: TransactionEntity(
        id = 0,
        title = item.title,
        amount = item.amount,
        dateMillis = DateUtils.localDateToMillis(item.dueDate),
        category = item.category.ifEmpty { "Planned Bill" },
        accountName = item.accountName,
        isIncome = false,
        isCompleted = false,
        isAutoPay = item.isAutoPay,
        status = "due"
    )
    val effectiveStatus = targetTx.getEffectiveStatus()
    val isSettled = targetTx.isPaidOrCleared

    val (dateLabel, dateBgColor, dateTextColor) = when {
        !isSettled && daysBetween < 0 -> Triple(
            if (isWideScreen) "⚠️ Overdue ($formattedDueDate)" else "⚠️ ${kotlin.math.abs(daysBetween)}d Overdue",
            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
        )
        !isSettled && daysBetween == 0L -> Triple(
            "⚡ Due Today",
            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFEE2E2),
            if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
        )
        !isSettled && daysBetween == 1L -> Triple(
            if (isWideScreen) "⏳ Due Tomorrow (24h)" else "⏳ Tomorrow",
            if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFDE68A) else Color(0xFFD97706)
        )
        !isSettled && daysBetween == 2L -> Triple(
            if (isWideScreen) "🕒 Due in 2d (48h)" else "🕒 In 2d",
            if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7),
            if (isDark) Color(0xFFFDE68A) else Color(0xFFB45309)
        )
        daysBetween in 3..5 -> Triple(
            "In ${daysBetween}d",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> Triple(
            if (isWideScreen) "Due $formattedDueDate" else formattedDueDate,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    val cardBorder = when {
        !isSettled && daysBetween < 0 -> BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.8f))
        !isSettled && daysBetween in 0L..2L -> BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.85f))
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    val cardBgColor = when {
        !isSettled && daysBetween < 0 -> if (isDark) Color(0xFF450A0A).copy(alpha = 0.35f) else Color(0xFFFEF2F2)
        !isSettled && daysBetween in 0L..2L -> if (isDark) Color(0xFF78350F).copy(alpha = 0.25f) else Color(0xFFFFFBEB)
        else -> MaterialTheme.colorScheme.surface
    }

    var isToggling by remember { mutableStateOf(false) }
    val checkScale by animateFloatAsState(
        targetValue = if (isToggling) 1.35f else 1.0f,
        animationSpec = tween(durationMillis = 180),
        label = "checkScale"
    )

    SwipeableTransactionRow(
        transaction = targetTx,
        onDelete = onDeleteClick,
        onEdit = onEditClick,
        onDuplicate = onDuplicateClick,
        onUpdateStatus = { newSt ->
            if (onUpdateStatus != null) {
                onUpdateStatus(targetTx, newSt)
            } else {
                onToggleCompleted(targetTx.copy(status = newSt, isCompleted = newSt in listOf("paid", "cleared")))
            }
        },
        onToggleCompleted = {
            onToggleCompleted(targetTx.copy(isCompleted = !targetTx.isCompleted))
        },
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("upcoming_item_${item.id}"),
            shape = RoundedCornerShape(12.dp),
            color = cardBgColor,
            border = cardBorder
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (isWideScreen) 12.dp else 8.dp, vertical = if (isWideScreen) 10.dp else 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left content: Date badge, title, account, auto-pay
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 10.dp else 8.dp)
                ) {
                    // Checkbox / Mark complete button with pulse animation
                    val coroutineScope = rememberCoroutineScope()
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isToggling = true
                                delay(140)
                                isToggling = false
                                val nextSt = if (isSettled) "due" else "cleared"
                                if (onUpdateStatus != null) {
                                    onUpdateStatus(targetTx, nextSt)
                                } else {
                                    onToggleCompleted(targetTx.copy(status = nextSt, isCompleted = nextSt in listOf("paid", "cleared")))
                                }
                            }
                        },
                        modifier = Modifier
                            .size(if (isWideScreen) 32.dp else 28.dp)
                            .scale(checkScale)
                            .testTag("mark_upcoming_paid_${item.id}")
                    ) {
                        Icon(
                            imageVector = if (isSettled) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = if (isSettled) "Settled ($effectiveStatus)" else "Mark Cleared",
                            tint = when (effectiveStatus) {
                                "cleared" -> Color(0xFF10B981)
                                "paid" -> Color(0xFF3B82F6)
                                "pending" -> Color(0xFFF59E0B)
                                else -> MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.size(if (isWideScreen) 24.dp else 21.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = item.title,
                                fontSize = if (isWideScreen) 14.sp else 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (item.isAutoPay) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFE0F2FE)
                                ) {
                                    Text(
                                        text = if (isWideScreen) "⚡ Auto-Pay" else "⚡ Auto",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }

                            // Status chip: Due, Pending, Paid, Cleared (clickable with dropdown)
                            TransactionStatusChip(
                                status = effectiveStatus,
                                isIncome = false,
                                fontSize = 9.sp,
                                onStatusChange = { newSt ->
                                    coroutineScope.launch {
                                        isToggling = true
                                        delay(120)
                                        isToggling = false
                                        if (onUpdateStatus != null) {
                                            onUpdateStatus(targetTx, newSt)
                                        } else {
                                            onToggleCompleted(targetTx.copy(status = newSt, isCompleted = newSt in listOf("paid", "cleared")))
                                        }
                                    }
                                },
                                testTag = "toggle_status_chip_${item.id}"
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = dateBgColor
                            ) {
                                Text(
                                    text = dateLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = dateTextColor,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }

                            if (item.accountName.isNotBlank()) {
                                Text(
                                    text = "• ${item.accountName}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if ((isWideScreen || item.accountName.isBlank()) && item.category.isNotBlank() && item.category != "Planned Bill") {
                                Text(
                                    text = "• ${item.category}",
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (targetTx.reminderDaysBefore >= 0) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(9.dp)
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = if (targetTx.reminderDaysBefore == 0) "Alert: Day of" else "Alert: ${targetTx.reminderDaysBefore}d before",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Right content: Amount & Action Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "-$%,.2f", item.amount),
                        fontSize = if (isWideScreen) 14.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )

                    // Optional Heads-Up Alert Reminder Button
                    if (onToggleReminder != null && !isSettled) {
                        val isReminderActive = targetTx.reminderDaysBefore >= 0
                        IconButton(
                            onClick = {
                                val nextRem = if (isReminderActive) -1 else 1
                                onToggleReminder(nextRem)
                            },
                            modifier = Modifier
                                .size(if (isWideScreen) 30.dp else 26.dp)
                                .testTag("reminder_upcoming_${item.id}")
                        ) {
                            Icon(
                                imageVector = if (isReminderActive) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = if (isReminderActive) "Reminder Active" else "Set Reminder",
                                tint = if (isReminderActive) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                            )
                        }
                    }

                    // Duplicate Button
                    if (onDuplicateClick != null) {
                        IconButton(
                            onClick = onDuplicateClick,
                            modifier = Modifier
                                .size(if (isWideScreen) 30.dp else 26.dp)
                                .testTag("duplicate_upcoming_${item.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Duplicate Expense",
                                tint = Color(0xFF8B5CF6).copy(alpha = 0.85f),
                                modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(if (isWideScreen) 30.dp else 26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Expense",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(if (isWideScreen) 30.dp else 26.dp)
                            .testTag("delete_upcoming_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Expense",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(if (isWideScreen) 16.dp else 14.dp)
                        )
                    }
                }
            }
        }
    }
}
