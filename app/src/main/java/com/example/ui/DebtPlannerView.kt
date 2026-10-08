package com.example.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DebtEntity
import com.example.data.DebtPaymentEntity
import com.example.model.DebtCategory
import com.example.model.DebtItem
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun LogDebtPaymentDialog(
    debt: DebtEntity,
    prefilledAmount: Double = debt.minimumPayment,
    onDismiss: () -> Unit,
    onConfirm: (paymentAmount: Double, principalAmount: Double, interestAmount: Double, note: String, createExpense: Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf(if (prefilledAmount > 0) String.format(Locale.US, "%.2f", prefilledAmount) else "") }
    var principalText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var createExpense by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Payment: ${debt.name}") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Current balance: $${String.format(Locale.US, "%,.2f", debt.balance)} (APR: ${debt.interestRate}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (principalText.isBlank()) principalText = it
                    },
                    label = { Text("Payment Amount ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                )
                OutlinedTextField(
                    value = principalText,
                    onValueChange = { principalText = it },
                    label = { Text("Principal Portion ($) - optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("principal_amount_input")
                )
                OutlinedTextField(
                    value = interestText,
                    onValueChange = { interestText = it },
                    label = { Text("Interest Portion ($) - optional") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("interest_amount_input")
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Confirmation #") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("payment_note_input")
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = createExpense,
                        onCheckedChange = { createExpense = it }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add to cycle transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val totalAmt = amountText.toDoubleOrNull() ?: 0.0
                    if (totalAmt > 0) {
                        val princ = principalText.toDoubleOrNull() ?: totalAmt
                        val interest = interestText.toDoubleOrNull() ?: 0.0
                        onConfirm(totalAmt, princ, interest, note.trim(), createExpense)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_payment")
            ) {
                Text("Record Payment")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class DebtSortOption(val displayName: String, val shortLabel: String) {
    SNOWBALL("Debt Snowball (Lowest Balance)", "Snowball"),
    AVALANCHE("Debt Avalanche (Highest APR)", "Avalanche"),
    BALANCE_DESC("Highest Balance First", "High Balance"),
    NAME_ASC("Account Name (A to Z)", "Name (A-Z)"),
    NAME_DESC("Account Name (Z to A)", "Name (Z-A)"),
    APR_ASC("Lowest APR First", "Low APR"),
    MIN_PAYMENT_DESC("Highest Min Payment", "High Min Pay"),
    MIN_PAYMENT_ASC("Lowest Min Payment", "Low Min Pay"),
    CUSTOM_PRIORITY("Custom Priority / Pinned", "Custom Priority")
}

@Composable
fun DebtPlannerView(
    uiState: BudgetUiState,
    onAddDebt: (name: String, balance: Double, interestRate: Double, minimumPayment: Double, category: String, notes: String, billDueDay: Int) -> Unit = { _, _, _, _, _, _, _ -> },
    onDeleteDebt: (DebtEntity) -> Unit,
    onUpdateDebt: (DebtEntity) -> Unit = {},
    isWideScreen: Boolean = false,
    onImportDebtsFromCsv: suspend (String) -> Pair<Boolean, String> = { Pair(true, "") },
    onExportDebtsToCsv: () -> String = { "" },
    onSyncGoogleDrive: () -> Unit = {},
    onConnectGoogleDrive: () -> Unit = {},
    isSyncing: Boolean = false,
    accountEmail: String? = null,
    onListDriveFiles: suspend () -> Pair<List<com.example.sync.DriveFileInfo>, String?> = { Pair(emptyList(), null) },
    onImportSpecificDriveFile: suspend (com.example.sync.DriveFileInfo) -> Pair<Boolean, String> = { Pair(false, "") },
    onImportGoogleSheetByUrlOrId: suspend (String) -> Pair<Boolean, String> = { Pair(false, "") },
    onImportAnyDataDetailed: suspend (String) -> Pair<Boolean, String> = { Pair(false, "") },
    onSaveCustomToken: (String) -> Unit = {},
    onLogPayment: (debtId: Int, paymentAmount: Double, principalAmount: Double, interestAmount: Double, note: String, createExpense: Boolean) -> Unit = { _, _, _, _, _, _ -> },
    onDeletePayment: (DebtPaymentEntity) -> Unit = {},
    onToggleBillLink: (debt: DebtEntity, isLinked: Boolean, dueDay: Int) -> Unit = { _, _, _ -> },
    onTogglePriorityPin: (DebtEntity) -> Unit = {},
    onTogglePaidOff: (DebtEntity) -> Unit = {},
    onAddFullDebt: (DebtEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val debts = uiState.allDebts
    var showAddDialog by remember { mutableStateOf(false) }
    var editingDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var loggingPaymentDebt by remember { mutableStateOf<DebtEntity?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedSortOption by remember { mutableStateOf(DebtSortOption.SNOWBALL) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showPaidOff by remember { mutableStateOf(true) }
    var showImportDebtsDialog by remember { mutableStateOf(false) }
    var debtsImportInput by remember { mutableStateOf("") }
    var debtsImportError by remember { mutableStateOf("") }
    var isImportingDebts by remember { mutableStateOf(false) }

    var driveFileList by remember { mutableStateOf<List<com.example.sync.DriveFileInfo>>(emptyList()) }
    var driveFileSearchQuery by remember { mutableStateOf("") }
    var isDriveFilesLoading by remember { mutableStateOf(false) }
    var driveFilesErrorMsg by remember { mutableStateOf<String?>(null) }
    var importingDriveFileId by remember { mutableStateOf<String?>(null) }
    var sheetUrlOrIdInput by remember { mutableStateOf("") }
    var isImportingSheetUrl by remember { mutableStateOf(false) }
    var showTokenBox by remember { mutableStateOf(false) }
    var customTokenInput by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val debtSpreadsheetMimeTypes = remember {
        arrayOf(
            "application/vnd.google-apps.spreadsheet",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-excel",
            "text/csv",
            "text/comma-separated-values",
            "text/tab-separated-values",
            "text/plain",
            "application/json",
            "*/*"
        )
    }

    LaunchedEffect(showImportDebtsDialog, accountEmail, uiState.driveAuthVersion) {
        if (showImportDebtsDialog) {
            isDriveFilesLoading = true
            driveFilesErrorMsg = null
            try {
                val (files, err) = onListDriveFiles()
                driveFileList = files
                driveFilesErrorMsg = err
            } catch (e: Exception) {
                driveFilesErrorMsg = e.localizedMessage
            } finally {
                isDriveFilesLoading = false
            }
        }
    }

    val debtFilePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                try {
                    isImportingDebts = true
                    debtsImportError = ""

                    val (content, fileName) = com.example.sync.SpreadsheetFileParser.extractContentFromUri(context, uri)

                    if (!content.isNullOrBlank()) {
                        debtsImportInput = content
                        val (ok, msg) = onImportDebtsFromCsv(content)
                        isImportingDebts = false
                        if (ok) {
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            showImportDebtsDialog = false
                        } else {
                            val (ok2, msg2) = onImportAnyDataDetailed(content)
                            if (ok2) {
                                android.widget.Toast.makeText(context, msg2, android.widget.Toast.LENGTH_LONG).show()
                                showImportDebtsDialog = false
                            } else {
                                debtsImportError = msg
                            }
                        }
                    } else {
                        // Attempt fallback to Google Sheet importer if document ID or file name is available
                        val (sheetOk, sheetMsg) = onImportGoogleSheetByUrlOrId(fileName)
                        isImportingDebts = false
                        if (sheetOk) {
                            android.widget.Toast.makeText(context, sheetMsg, android.widget.Toast.LENGTH_LONG).show()
                            showImportDebtsDialog = false
                        } else {
                            debtsImportError = sheetMsg
                        }
                    }
                } catch (e: Exception) {
                    isImportingDebts = false
                    debtsImportError = "Error reading file: ${e.localizedMessage}"
                }
            }
        }
    }

    val categories = remember(debts) {
        listOf("All") + debts.map { it.category }.distinct()
    }

    val filteredDebts = remember(debts, selectedCategoryFilter, showPaidOff, selectedSortOption) {
        debts.filter { debt ->
            (showPaidOff || debt.balance > 0.0) &&
                    (selectedCategoryFilter == "All" || debt.category.equals(selectedCategoryFilter, ignoreCase = true))
        }.sortedWith { d1, d2 ->
            // First condition: move zero balance / paid off debts to bottom
            val zero1 = if (d1.balance <= 0.0 || d1.isPaidOff) 1 else 0
            val zero2 = if (d2.balance <= 0.0 || d2.isPaidOff) 1 else 0
            if (zero1 != zero2) return@sortedWith zero1.compareTo(zero2)

            when (selectedSortOption) {
                DebtSortOption.SNOWBALL -> {
                    val p1 = if (d1.customPriorityRank > 0) d1.customPriorityRank else Int.MAX_VALUE
                    val p2 = if (d2.customPriorityRank > 0) d2.customPriorityRank else Int.MAX_VALUE
                    if (p1 != p2) return@sortedWith p1.compareTo(p2)
                    val bal = d1.balance.compareTo(d2.balance)
                    if (bal != 0) bal
                    else d2.interestRate.compareTo(d1.interestRate)
                }
                DebtSortOption.AVALANCHE -> {
                    val p1 = if (d1.customPriorityRank > 0) d1.customPriorityRank else Int.MAX_VALUE
                    val p2 = if (d2.customPriorityRank > 0) d2.customPriorityRank else Int.MAX_VALUE
                    if (p1 != p2) return@sortedWith p1.compareTo(p2)
                    val apr = d2.interestRate.compareTo(d1.interestRate)
                    if (apr != 0) apr
                    else d1.balance.compareTo(d2.balance)
                }
                DebtSortOption.BALANCE_DESC -> {
                    val bal = d2.balance.compareTo(d1.balance)
                    if (bal != 0) bal
                    else d2.interestRate.compareTo(d1.interestRate)
                }
                DebtSortOption.NAME_ASC -> {
                    d1.name.lowercase(Locale.US).compareTo(d2.name.lowercase(Locale.US))
                }
                DebtSortOption.NAME_DESC -> {
                    d2.name.lowercase(Locale.US).compareTo(d1.name.lowercase(Locale.US))
                }
                DebtSortOption.APR_ASC -> {
                    val apr = d1.interestRate.compareTo(d2.interestRate)
                    if (apr != 0) apr
                    else d1.balance.compareTo(d2.balance)
                }
                DebtSortOption.MIN_PAYMENT_DESC -> {
                    val mp = d2.minimumPayment.compareTo(d1.minimumPayment)
                    if (mp != 0) mp
                    else d2.balance.compareTo(d1.balance)
                }
                DebtSortOption.MIN_PAYMENT_ASC -> {
                    val mp = d1.minimumPayment.compareTo(d2.minimumPayment)
                    if (mp != 0) mp
                    else d1.balance.compareTo(d2.balance)
                }
                DebtSortOption.CUSTOM_PRIORITY -> {
                    val p1 = if (d1.customPriorityRank > 0) d1.customPriorityRank else Int.MAX_VALUE
                    val p2 = if (d2.customPriorityRank > 0) d2.customPriorityRank else Int.MAX_VALUE
                    if (p1 != p2) p1.compareTo(p2)
                    else d1.balance.compareTo(d2.balance)
                }
            }
        }
    }

    val totalActiveBalance = remember(debts) {
        debts.filter { !it.isPaidOff }.sumOf { it.balance }
    }
    val totalMinMonthly = remember(debts) {
        debts.filter { !it.isPaidOff }.sumOf { it.minimumPayment }
    }

    val activeDebts = remember(filteredDebts) {
        filteredDebts.filter { it.balance > 0.0 && !it.isPaidOff }
    }
    val zeroBalanceDebts = remember(filteredDebts) {
        filteredDebts.filter { it.balance <= 0.0 || it.isPaidOff }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))

            // Debt Overview Summary Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("debt_overview_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Active Debt",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format(Locale.US, "%,.2f", totalActiveBalance)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    debtsImportInput = ""
                                    debtsImportError = ""
                                    showImportDebtsDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_open_import_debts")
                            ) {
                                Icon(Icons.Default.FileOpen, contentDescription = "Import Debts", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("btn_add_debt")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Debt", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Debt", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Monthly Minimums: $${String.format(Locale.US, "%,.2f", totalMinMonthly)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${debts.count { !it.isPaidOff }} active accounts",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            // Filters, Sorting and Toggles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(categories) { cat ->
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text(cat, fontSize = 11.sp) },
                                modifier = Modifier.height(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Box {
                        AssistChip(
                            onClick = { showSortMenu = true },
                            label = { Text("Sort: ${selectedSortOption.shortLabel}", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Debts",
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("btn_sort_debts")
                        )

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DebtSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontSize = 12.sp,
                                            fontWeight = if (option == selectedSortOption) FontWeight.Bold else FontWeight.Normal,
                                            color = if (option == selectedSortOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    leadingIcon = {
                                        if (option == selectedSortOption) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        selectedSortOption = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    TextButton(
                        onClick = { showPaidOff = !showPaidOff },
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Text(
                            text = if (showPaidOff) "Hide Zero/Paid" else "Show Zero/Paid",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (filteredDebts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (debts.isEmpty()) "No debts added yet! Click 'Add Debt' above." else "No debts in this filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            // Active positive balance debts on top
            items(activeDebts, key = { it.id }) { debt ->
                DebtItemCard(
                    debt = debt,
                    onLogPayment = { loggingPaymentDebt = debt },
                    onEdit = { editingDebt = debt },
                    onDelete = { onDeleteDebt(debt) },
                    onTogglePin = { onTogglePriorityPin(debt) },
                    onTogglePaidOff = { onTogglePaidOff(debt) },
                    onToggleBillLink = { isLinked, dueDay -> onToggleBillLink(debt, isLinked, dueDay) }
                )
            }

            // Zero balance debts moved to the bottom of the section
            if (zeroBalanceDebts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Zero Balance Debts (${zeroBalanceDebts.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Paid Off • Bottom",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                items(zeroBalanceDebts, key = { it.id }) { debt ->
                    DebtItemCard(
                        debt = debt,
                        onLogPayment = { loggingPaymentDebt = debt },
                        onEdit = { editingDebt = debt },
                        onDelete = { onDeleteDebt(debt) },
                        onTogglePin = { onTogglePriorityPin(debt) },
                        onTogglePaidOff = { onTogglePaidOff(debt) },
                        onToggleBillLink = { isLinked, dueDay -> onToggleBillLink(debt, isLinked, dueDay) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showAddDialog) {
        AddOrEditDebtDialog(
            debt = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, bal, rate, minPay, cat, notes, dueDay ->
                onAddDebt(name, bal, rate, minPay, cat, notes, dueDay)
                showAddDialog = false
            }
        )
    }

    editingDebt?.let { debt ->
        AddOrEditDebtDialog(
            debt = debt,
            onDismiss = { editingDebt = null },
            onConfirm = { name, bal, rate, minPay, cat, notes, dueDay ->
                onUpdateDebt(
                    debt.copy(
                        name = name,
                        balance = bal,
                        interestRate = rate,
                        minimumPayment = minPay,
                        category = cat,
                        notes = notes,
                        billDueDay = dueDay
                    )
                )
                editingDebt = null
            }
        )
    }

    loggingPaymentDebt?.let { debt ->
        LogDebtPaymentDialog(
            debt = debt,
            prefilledAmount = debt.minimumPayment,
            onDismiss = { loggingPaymentDebt = null },
            onConfirm = { paymentAmount, principalAmount, interestAmount, note, createExpense ->
                onLogPayment(debt.id, paymentAmount, principalAmount, interestAmount, note, createExpense)
                loggingPaymentDebt = null
            }
        )
    }

    if (showImportDebtsDialog) {
        AlertDialog(
            onDismissRequest = { if (!isImportingDebts && importingDriveFileId == null && !isImportingSheetUrl) showImportDebtsDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync & Import Debts", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(
                        onClick = { showImportDebtsDialog = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }
            },
            text = {
                val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val triggerConnectAndReload: () -> Unit = {
                        isDriveFilesLoading = true
                        driveFilesErrorMsg = null
                        onConnectGoogleDrive()
                    }

                    // 1. Direct Google Sheet Link / ID (Primary 1-Tap Option)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.TableChart, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Text("📊 Permanent Google Sheet Link (Never Expires)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                TextButton(
                                    onClick = {
                                        try {
                                            val clipData = clipboardManager?.primaryClip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val text = clipData.getItemAt(0)?.text?.toString() ?: ""
                                                if (text.isNotBlank()) {
                                                    sheetUrlOrIdInput = text.trim()
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Paste Link", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text("Set sharing on your Google Sheet to 'Anyone with link can view'. This link syncs permanently without expiring or needing Playground OAuth.", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = sheetUrlOrIdInput,
                                    onValueChange = { sheetUrlOrIdInput = it },
                                    placeholder = { Text("https://docs.google.com/spreadsheets/d/...", fontSize = 10.sp) },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("debt_sheet_url_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                Button(
                                    onClick = {
                                        val input = sheetUrlOrIdInput.trim()
                                        if (input.isBlank()) {
                                            debtsImportError = "Please paste a Google Sheet link or ID"
                                            return@Button
                                        }
                                        isImportingSheetUrl = true
                                        coroutineScope.launch {
                                            try {
                                                val (ok, msg) = onImportGoogleSheetByUrlOrId(input)
                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                if (ok) {
                                                    showImportDebtsDialog = false
                                                    sheetUrlOrIdInput = ""
                                                } else {
                                                    debtsImportError = msg
                                                }
                                            } catch (e: Exception) {
                                                debtsImportError = "Import failed: ${e.localizedMessage}"
                                                android.widget.Toast.makeText(context, debtsImportError, android.widget.Toast.LENGTH_LONG).show()
                                            } finally {
                                                isImportingSheetUrl = false
                                            }
                                        }
                                    },
                                    enabled = sheetUrlOrIdInput.isNotBlank() && !isImportingSheetUrl,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(48.dp).testTag("btn_import_debt_sheet")
                                ) {
                                    if (isImportingSheetUrl) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Import Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // 2. Google Drive & Cloud Files
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("☁️ Google Drive & Cloud Files", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    val emailDisplay = accountEmail ?: "86brancan@gmail.com"
                                    Text(emailDisplay, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(
                                        onClick = { showTokenBox = !showTokenBox },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Token", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = triggerConnectAndReload,
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Sign In", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isDriveFilesLoading = true
                                                driveFilesErrorMsg = null
                                                try {
                                                    val (files, err) = onListDriveFiles()
                                                    driveFileList = files
                                                    driveFilesErrorMsg = err
                                                } catch (e: Exception) {
                                                    driveFilesErrorMsg = e.localizedMessage
                                                } finally {
                                                    isDriveFilesLoading = false
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(28.dp).testTag("refresh_debt_drive_files_button")
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Drive Files", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            if (showTokenBox || (!driveFilesErrorMsg.isNullOrBlank() && driveFileList.isEmpty())) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "🔑 OAuth Access Token or Auth Code:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = customTokenInput,
                                                onValueChange = { customTokenInput = it },
                                                placeholder = { Text("Paste ya29... token or 4/... code", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.weight(1f),
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                            )
                                            Button(
                                                onClick = {
                                                    val input = customTokenInput.trim()
                                                    if (input.isNotBlank()) {
                                                        onSaveCustomToken(input)
                                                        coroutineScope.launch {
                                                            isDriveFilesLoading = true
                                                            driveFilesErrorMsg = null
                                                            try {
                                                                val (files, err) = onListDriveFiles()
                                                                driveFileList = files
                                                                driveFilesErrorMsg = err
                                                            } catch (e: Exception) {
                                                                driveFilesErrorMsg = e.localizedMessage
                                                            } finally {
                                                                isDriveFilesLoading = false
                                                            }
                                                        }
                                                    }
                                                },
                                                enabled = customTokenInput.isNotBlank(),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(38.dp)
                                            ) {
                                                Text("Connect", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            if (!driveFilesErrorMsg.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Text(driveFilesErrorMsg ?: "", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                                        Button(
                                            onClick = triggerConnectAndReload,
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("Authorize Drive & Sheets", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            if (isDriveFilesLoading) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Loading files from Google Drive...", fontSize = 11.sp)
                                }
                            } else if (driveFileList.isEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("No files found yet in Drive / Cloud Sync.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Save files in 'PayCycle Budget' folder, import a sheet above, or tap 'Sign In with Google' below.", fontSize = 10.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Button(
                                            onClick = triggerConnectAndReload,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Sign In with Google", fontSize = 11.sp)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isDriveFilesLoading = true
                                                    driveFilesErrorMsg = null
                                                    try {
                                                        val (files, err) = onListDriveFiles()
                                                        driveFileList = files
                                                        driveFilesErrorMsg = err
                                                    } catch (e: Exception) {
                                                        driveFilesErrorMsg = e.localizedMessage
                                                    } finally {
                                                        isDriveFilesLoading = false
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Refresh", fontSize = 11.sp)
                                        }
                                    }
                                }
                            } else {
                                Text("Tap 'Import' on any debt payoff file or spreadsheet to load (${driveFileList.size} files found):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = driveFileSearchQuery,
                                    onValueChange = { driveFileSearchQuery = it },
                                    placeholder = { Text("Search ${driveFileList.size} Drive files...", fontSize = 11.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                    trailingIcon = {
                                        if (driveFileSearchQuery.isNotEmpty()) {
                                            IconButton(onClick = { driveFileSearchQuery = "" }, modifier = Modifier.size(20.dp)) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                                )
                                val filteredDriveFiles = remember(driveFileList, driveFileSearchQuery) {
                                    if (driveFileSearchQuery.isBlank()) driveFileList
                                    else driveFileList.filter { it.name.contains(driveFileSearchQuery.trim(), ignoreCase = true) }
                                }
                                if (filteredDriveFiles.isEmpty()) {
                                    Text("No files matching \"$driveFileSearchQuery\"", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    filteredDriveFiles.forEach { file ->
                                        val isCurrentImporting = importingDriveFileId == file.id
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 2.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = importingDriveFileId == null && !isImportingDebts) {
                                                    importingDriveFileId = file.id
                                                    coroutineScope.launch {
                                                        try {
                                                            val (ok, msg) = onImportSpecificDriveFile(file)
                                                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                            if (ok) {
                                                                showImportDebtsDialog = false
                                                            } else {
                                                                debtsImportError = msg
                                                            }
                                                        } catch (e: Exception) {
                                                            debtsImportError = "Import failed: ${e.localizedMessage}"
                                                            android.widget.Toast.makeText(context, debtsImportError, android.widget.Toast.LENGTH_LONG).show()
                                                        } finally {
                                                            importingDriveFileId = null
                                                        }
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val iconColor = when {
                                                    file.isGoogleSheet -> Color(0xFF0F9D58)
                                                    file.isCsv -> Color(0xFF1E88E5)
                                                    file.isJson -> Color(0xFFF59E0B)
                                                    else -> Color(0xFF6B7280)
                                                }
                                                Icon(
                                                    when {
                                                        file.isGoogleSheet -> Icons.Default.TableChart
                                                        file.isJson -> Icons.Default.Code
                                                        else -> Icons.Default.Description
                                                    },
                                                    contentDescription = null,
                                                    tint = iconColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(file.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(
                                                            if (file.location == "PayCycle Budget") "📁 PayCycle Folder" else "☁️ My Drive",
                                                            fontSize = 9.sp,
                                                            color = if (file.location == "PayCycle Budget") Color(0xFF059669) else Color(0xFF4B5563),
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        if (file.sizeString.isNotBlank()) {
                                                            Text("• ${file.sizeString}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                        }
                                                    }
                                                    if (file.formattedDate.isNotBlank()) {
                                                        Text(file.formattedDate, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        importingDriveFileId = file.id
                                                        coroutineScope.launch {
                                                            try {
                                                                val (ok, msg) = onImportSpecificDriveFile(file)
                                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                                if (ok) {
                                                                    showImportDebtsDialog = false
                                                                } else {
                                                                    debtsImportError = msg
                                                                }
                                                            } catch (e: Exception) {
                                                                debtsImportError = "Import error: ${e.localizedMessage}"
                                                                android.widget.Toast.makeText(context, debtsImportError, android.widget.Toast.LENGTH_LONG).show()
                                                            } finally {
                                                                importingDriveFileId = null
                                                            }
                                                        }
                                                    },
                                                    enabled = importingDriveFileId == null && !isImportingDebts,
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    if (isCurrentImporting) {
                                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                                    } else {
                                                        Text("Import", fontSize = 10.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Pick from Device or Paste CSV
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("📁 Pick File from Device or Paste Data", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            debtFilePickerLauncher.launch(debtSpreadsheetMimeTypes)
                                        } catch (e: Exception) {
                                            debtsImportError = "File picker error: ${e.localizedMessage}"
                                        }
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_select_debt_file_dialog")
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pick from Device", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val clipData = clipboardManager?.primaryClip
                                            if (clipData != null && clipData.itemCount > 0) {
                                                val text = clipData.getItemAt(0)?.text?.toString() ?: ""
                                                if (text.isNotBlank()) {
                                                    debtsImportInput = text
                                                    debtsImportError = ""
                                                }
                                            }
                                        } catch (_: Exception) {}
                                    },
                                    modifier = Modifier.weight(1f).testTag("btn_paste_debts_dialog")
                                ) {
                                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Paste Clipboard", fontSize = 11.sp)
                                }
                            }

                            Text("Or paste debt CSV rows below (e.g. Debt Name,Balance,APR,MinPay):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = debtsImportInput,
                                onValueChange = {
                                    debtsImportInput = it
                                    debtsImportError = ""
                                },
                                placeholder = { Text("Debt Name,Balance,APR,Minimum Payment", fontSize = 10.sp) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                maxLines = 100
                            )

                            if (debtsImportInput.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            isImportingDebts = true
                                            coroutineScope.launch {
                                                try {
                                                    val (ok, msg) = onImportDebtsFromCsv(debtsImportInput)
                                                    isImportingDebts = false
                                                    if (ok) {
                                                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                                                        showImportDebtsDialog = false
                                                    } else {
                                                        debtsImportError = msg
                                                    }
                                                } catch (e: Exception) {
                                                    isImportingDebts = false
                                                    debtsImportError = "Error: ${e.localizedMessage}"
                                                }
                                            }
                                        },
                                        enabled = !isImportingDebts
                                    ) {
                                        if (isImportingDebts) {
                                            CircularProgressIndicator(modifier = Modifier.size(14.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Importing...", fontSize = 11.sp)
                                        } else {
                                            Text("Import & Merge", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (debtsImportError.isNotEmpty()) {
                        Text(
                            text = debtsImportError,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { showImportDebtsDialog = false },
                    enabled = !isImportingDebts && importingDriveFileId == null && !isImportingSheetUrl
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun DebtItemCard(
    debt: DebtEntity,
    onLogPayment: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
    onTogglePaidOff: () -> Unit,
    onToggleBillLink: ((Boolean, Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isZeroBalance = debt.balance <= 0.0 || debt.isPaidOff

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isZeroBalance) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (debt.customPriorityRank > 0 && !isZeroBalance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("debt_card_${debt.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = debt.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = debt.category,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (isZeroBalance) {
                        Surface(
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Zero Balance",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (debt.customPriorityRank > 0) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned Focus Debt",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = "$${String.format(Locale.US, "%,.2f", debt.balance)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isZeroBalance) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            val progress = (debt.progressPercentage / 100f).coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isZeroBalance) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "APR: ${debt.interestRate}%  •  Min: $${String.format(Locale.US, "%,.0f", debt.minimumPayment)}/mo  •  Due: ${debt.dueDayOrdinal}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isZeroBalance) "100% paid off" else "${debt.progressPercentage}% paid",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isZeroBalance) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Due Date Indicator & Bill Link Status
            val today = remember { LocalDate.now() }
            val nextDueDate = remember(debt.billDueDay) { debt.getNextDueDate(today) }
            val daysUntilDue = remember(nextDueDate, today) { ChronoUnit.DAYS.between(today, nextDueDate) }
            val isDueSoon = daysUntilDue in 0..3 && !isZeroBalance

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        isZeroBalance -> Color(0xFF10B981).copy(alpha = 0.12f)
                        isDueSoon -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                        daysUntilDue <= 7 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isZeroBalance -> Color(0xFF10B981).copy(alpha = 0.35f)
                            isDueSoon -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Due Date",
                            tint = when {
                                isZeroBalance -> Color(0xFF10B981)
                                isDueSoon -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isZeroBalance) "Due ${debt.dueDayOrdinal} • Paid Off ($0.00)" else "Due: ${debt.formatNextDueDate(today)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                isZeroBalance -> Color(0xFF10B981)
                                isDueSoon -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }

                if (debt.isLinkedToBills) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "Bills Linked",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "Bills Linked",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (debt.customPriorityRank > 0) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Priority",
                            tint = if (debt.customPriorityRank > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onTogglePaidOff,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (debt.isPaidOff) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = "Toggle Paid",
                            tint = if (debt.isPaidOff) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!debt.isPaidOff) {
                    Button(
                        onClick = onLogPayment,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp).testTag("btn_log_payment_${debt.id}")
                    ) {
                        Text("Log Payment", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AddOrEditDebtDialog(
    debt: DebtEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, balance: Double, interestRate: Double, minimumPayment: Double, category: String, notes: String, billDueDay: Int) -> Unit
) {
    var name by remember { mutableStateOf(debt?.name ?: "") }
    var balanceText by remember { mutableStateOf(if (debt != null) String.format(Locale.US, "%.2f", debt.balance) else "") }
    var rateText by remember { mutableStateOf(if (debt != null) debt.interestRate.toString() else "") }
    var minPayText by remember { mutableStateOf(if (debt != null) String.format(Locale.US, "%.2f", debt.minimumPayment) else "") }
    var dueDayText by remember { mutableStateOf(debt?.billDueDay?.toString() ?: "15") }
    var category by remember { mutableStateOf(debt?.category ?: "Credit Card") }
    var notes by remember { mutableStateOf(debt?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (debt == null) "Add Debt Account" else "Edit Debt Account") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_name")
                )
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("Current Balance ($)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_balance")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = rateText,
                        onValueChange = { rateText = it },
                        label = { Text("APR (%)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_debt_rate")
                    )
                    OutlinedTextField(
                        value = minPayText,
                        onValueChange = { minPayText = it },
                        label = { Text("Min Pay ($)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("input_debt_min_pay")
                    )
                }

                // Due Day of Month Section
                val parsedDueDay = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 15
                val previewDate = remember(parsedDueDay) {
                    val ref = LocalDate.now()
                    val targetDay = parsedDueDay.coerceAtMost(ref.lengthOfMonth())
                    val d = ref.withDayOfMonth(targetDay)
                    if (!ref.isAfter(d)) d else {
                        val next = ref.plusMonths(1)
                        next.withDayOfMonth(parsedDueDay.coerceAtMost(next.lengthOfMonth()))
                    }
                }
                val previewFormatted = remember(previewDate) {
                    previewDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }.take(2)
                            dueDayText = digits
                        },
                        label = { Text("Payment Due Day (1 - 31)") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Due Date", modifier = Modifier.size(18.dp))
                        },
                        supportingText = {
                            Text("Next due: $previewFormatted (Day ${parsedDueDay})", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_debt_due_day")
                    )

                    // Quick day presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(1, 5, 10, 15, 20, 25, 28).forEach { day ->
                            FilterChip(
                                selected = parsedDueDay == day,
                                onClick = { dueDayText = day.toString() },
                                label = { Text("${day}th", fontSize = 10.5.sp) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (Credit Card, Loan, etc.)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_category")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_debt_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bal = balanceText.toDoubleOrNull() ?: 0.0
                    val rate = rateText.toDoubleOrNull() ?: 0.0
                    val min = minPayText.toDoubleOrNull() ?: 0.0
                    val dueDay = dueDayText.toIntOrNull()?.coerceIn(1, 31) ?: 15
                    if (name.isNotBlank() && bal >= 0) {
                        onConfirm(name.trim(), bal, rate, min, category.trim(), notes.trim(), dueDay)
                    }
                },
                modifier = Modifier.testTag("btn_save_debt")
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

// Backward-compatible overload for DebtItem
@Composable
fun DebtPlannerView(
    debts: List<DebtItem>,
    onAddDebt: (DebtItem) -> Unit,
    onDeleteDebt: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Debt: $${String.format(Locale.US, "%,.2f", debts.sumOf { it.balance })}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Button(onClick = { showAddDialog = true }) {
                Text("Add")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val sortedDebts = remember(debts) {
            debts.sortedWith(
                compareBy<DebtItem> { if (it.balance <= 0.0) 1 else 0 }.thenBy { it.balance }
            )
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(sortedDebts, key = { it.id }) { debt ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(debt.name, fontWeight = FontWeight.Bold)
                            Text("APR: ${debt.apr}% • Min: $${debt.minPayment}")
                            if (debt.dueDate.isNotBlank()) {
                                Text("📅 Due: ${debt.dueDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$${String.format(Locale.US, "%,.2f", debt.balance)}", fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDeleteDebt(debt.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}
