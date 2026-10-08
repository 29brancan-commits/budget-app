package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CategoryTheme
import com.example.model.ExpenseCategory
import com.example.model.ExpenseItem

@Composable
fun ExpensesListView(
    expenses: List<ExpenseItem>,
    onTogglePaid: (String) -> Unit,
    onAddExpense: (ExpenseItem) -> Unit,
    onStatusChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var statusFilterIndex by remember { mutableStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }

    val statusLabels = listOf("All", "Due", "Paid", "AutoPay")

    val filteredExpenses = remember(expenses, searchQuery, selectedCategoryFilter, statusFilterIndex) {
        expenses.filter { item ->
            val matchesQuery = searchQuery.isBlank() ||
                    item.name.contains(searchQuery, ignoreCase = true) ||
                    item.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryFilter == "All" ||
                    item.category.contains(selectedCategoryFilter, ignoreCase = true)

            val matchesStatus = when (statusFilterIndex) {
                1 -> item.status == "Due" || item.status == "Overdue"
                2 -> item.status == "Paid"
                3 -> item.isAutoPay
                else -> true
            }

            matchesQuery && matchesCategory && matchesStatus
        }
    }

    Box(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions or categories...", fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.height(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("expenses_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Status Filter Chips (COMPACT!)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                statusLabels.forEachIndexed { index, label ->
                    val selected = statusFilterIndex == index
                    CompactFilterChip(
                        selected = selected,
                        onClick = { statusFilterIndex = index },
                        label = label,
                        fontSize = 9.sp,
                        testTag = "status_filter_${label.lowercase()}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Chips (COMPACT!)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CompactFilterChip(
                        selected = selectedCategoryFilter == "All",
                        onClick = { selectedCategoryFilter = "All" },
                        label = "All Categories",
                        fontSize = 8.5.sp,
                        testTag = "cat_filter_all"
                    )
                }
                items(ExpenseCategory.values()) { cat ->
                    val selected = selectedCategoryFilter == cat.displayName
                    CompactFilterChip(
                        selected = selected,
                        onClick = { selectedCategoryFilter = if (selected) "All" else cat.displayName },
                        label = cat.displayName,
                        fontSize = 8.5.sp,
                        testTag = "cat_filter_${cat.name.lowercase()}"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Transactions list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filteredExpenses, key = { it.id }) { item ->
                    val isPaid = item.status == "Paid"
                    val catColor = CategoryTheme.getColor(item.category)

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isPaid) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth().testTag("transaction_row_${item.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onTogglePaid(item.id) },
                                modifier = Modifier.height(32.dp).width(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaid) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = if (isPaid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.height(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (item.isAutoPay) {
                                        CompactBadge(
                                            text = "AUTO-PAY",
                                            bgColor = Color(0xFFE0F2FE),
                                            textColor = Color(0xFF0369A1),
                                            borderColor = Color(0xFF7DD3FC),
                                            fontSize = 7.5.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CompactBadge(
                                        text = item.category.take(16),
                                        bgColor = catColor.copy(alpha = 0.12f),
                                        textColor = catColor,
                                        fontSize = 8.sp
                                    )
                                    Text(item.dueDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$${String.format("%,.2f", item.amount)}",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                TransactionStatusChip(
                                    status = item.status,
                                    fontSize = 8.5.sp,
                                    onStatusChange = { newStatus -> onStatusChange(item.id, newStatus) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB to add expense
        FloatingActionButton(
            onClick = { showAddDialog = true },
            shape = RoundedCornerShape(12.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 16.dp).testTag("fab_add_expense")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Expense")
        }
    }

    if (showAddDialog) {
        AddUpcomingSimpleDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddExpense(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddUpcomingSimpleDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExpenseItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Utilities & Bills") }
    var dueDate by remember { mutableStateOf(java.time.LocalDate.now().plusDays(7).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Scheduled Expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Expense Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_name")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_amount")
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_category")
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_expense_due_date")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && amt > 0) {
                        onConfirm(
                            ExpenseItem(
                                id = java.util.UUID.randomUUID().toString(),
                                name = name.trim(),
                                amount = amt,
                                category = category.trim(),
                                dueDate = dueDate.trim(),
                                status = "Due"
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_expense")
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

