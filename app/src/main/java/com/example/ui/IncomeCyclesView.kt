package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.model.IncomeItem
import java.time.LocalDate

@Composable
fun IncomeCyclesView(
    incomes: List<IncomeItem>,
    onAddIncome: (IncomeItem) -> Unit,
    onStatusChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFrequencyFilter by remember { mutableStateOf("All") }

    val totalIncome = remember(incomes) { incomes.sumOf { it.amount } }

    val filteredIncomes = remember(incomes, selectedFrequencyFilter) {
        if (selectedFrequencyFilter == "All") incomes
        else incomes.filter { it.frequency.equals(selectedFrequencyFilter, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero Income Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            border = BorderStroke(1.dp, Color(0xFFA5D6A7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TOTAL MONTHLY INFLOW",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "$${String.format("%,.2f", totalIncome)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "${incomes.size} regular income streams",
                        fontSize = 10.5.sp,
                        color = Color(0xFF388E3C)
                    )
                }

                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("add_income_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Income", modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Income", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Frequency Filter Chips (COMPACT!)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cycle:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            listOf("All", "Bi-Weekly", "Weekly", "Monthly").forEach { f ->
                val isSelected = selectedFrequencyFilter == f
                CompactFilterChip(
                    selected = isSelected,
                    onClick = { selectedFrequencyFilter = f },
                    label = f,
                    fontSize = 9.sp,
                    testTag = "income_filter_${f.lowercase()}"
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Incomes list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredIncomes, key = { it.id }) { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().testTag("income_item_${item.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.height(36.dp).width(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.sourceName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Account badge
                                CompactBadge(
                                    text = item.accountName,
                                    bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                    textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontSize = 8.sp
                                )
                                // Frequency badge
                                CompactBadge(
                                    text = item.frequency,
                                    bgColor = MaterialTheme.colorScheme.surfaceVariant,
                                    textColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.sp
                                )
                                Text("• ${item.payDate}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "+$${String.format("%,.2f", item.amount)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TransactionStatusChip(
                                status = item.status,
                                isIncome = true,
                                fontSize = 8.5.sp,
                                onStatusChange = { newStatus -> onStatusChange(item.id, newStatus) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddIncomeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = {
                onAddIncome(it)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddIncomeDialog(
    onDismiss: () -> Unit,
    onConfirm: (IncomeItem) -> Unit
) {
    var sourceName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("Bi-Weekly") }
    var accountName by remember { mutableStateOf("Checking Account") }
    var payDate by remember { mutableStateOf(LocalDate.now().plusDays(3).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Income Stream", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("Income Source (e.g. Salary, Freelance)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_income_source_input")
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($)", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_income_amount_input")
                )

                // Frequency chips (COMPACT!)
                Text("Payment Frequency", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Bi-Weekly", "Weekly", "Monthly").forEach { f ->
                        CompactFilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = f,
                            fontSize = 8.5.sp
                        )
                    }
                }

                OutlinedTextField(
                    value = accountName,
                    onValueChange = { accountName = it },
                    label = { Text("Deposit Account", fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (sourceName.isNotBlank() && amt > 0) {
                        onConfirm(
                            IncomeItem(
                                id = "inc_${System.currentTimeMillis()}",
                                sourceName = sourceName.trim(),
                                amount = amt,
                                frequency = frequency,
                                payDate = payDate,
                                accountName = accountName.trim(),
                                status = "Expected"
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_income_button")
            ) {
                Text("Save Income", fontSize = 12.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel", fontSize = 12.sp)
            }
        }
    )
}
