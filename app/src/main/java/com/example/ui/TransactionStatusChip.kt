package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class TxStatus(val key: String, val label: String, val icon: String) {
    DUE("due", "Due", "⏳"),
    PENDING("pending", "Pending", "🕒"),
    PAID("paid", "Paid", "💳"),
    CLEARED("cleared", "Cleared", "🛡️");

    companion object {
        fun from(raw: String?, isCompleted: Boolean = false): TxStatus {
            val clean = raw?.lowercase()?.trim() ?: ""
            return when (clean) {
                "due" -> DUE
                "pending" -> PENDING
                "paid" -> PAID
                "cleared" -> CLEARED
                else -> if (isCompleted) CLEARED else DUE
            }
        }

        fun nextStatus(current: String): String = when (current.lowercase().trim()) {
            "due" -> "pending"
            "pending" -> "paid"
            "paid" -> "cleared"
            "cleared" -> "due"
            else -> "due"
        }
    }
}

@Composable
fun TransactionStatusChip(
    status: String,
    isIncome: Boolean = false,
    fontSize: TextUnit = 7.sp,
    onStatusChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val dark = isSystemInDarkTheme()
    val currentTxStatus = TxStatus.from(status)
    var menuExpanded by remember { mutableStateOf(false) }

    val displayText = when (currentTxStatus) {
        TxStatus.DUE -> "⏳ Due"
        TxStatus.PENDING -> "🕒 Pending"
        TxStatus.PAID -> if (isIncome) "✓ Received" else "✓ Paid"
        TxStatus.CLEARED -> "🛡️ Cleared"
    }

    val containerColor = when (currentTxStatus) {
        TxStatus.DUE -> if (dark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.8f)
        TxStatus.PENDING -> if (dark) Color(0xFF78350F).copy(alpha = 0.35f) else Color(0xFFFEF3C7)
        TxStatus.PAID -> if (dark) Color(0xFF1E3A8A).copy(alpha = 0.35f) else Color(0xFFDBEAFE)
        TxStatus.CLEARED -> if (dark) Color(0xFF064E3B).copy(alpha = 0.35f) else Color(0xFFD1FAE5)
    }

    val borderColor = when (currentTxStatus) {
        TxStatus.DUE -> if (dark) Color(0xFF64748B) else Color(0xFF94A3B8)
        TxStatus.PENDING -> if (dark) Color(0xFFF59E0B).copy(alpha = 0.6f) else Color(0xFFF59E0B)
        TxStatus.PAID -> if (dark) Color(0xFF3B82F6).copy(alpha = 0.6f) else Color(0xFF3B82F6)
        TxStatus.CLEARED -> if (dark) Color(0xFF10B981).copy(alpha = 0.6f) else Color(0xFF10B981)
    }

    val contentColor = when (currentTxStatus) {
        TxStatus.DUE -> if (dark) Color(0xFFCBD5E1) else Color(0xFF334155)
        TxStatus.PENDING -> if (dark) Color(0xFFFCD34D) else Color(0xFFB45309)
        TxStatus.PAID -> if (dark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
        TxStatus.CLEARED -> if (dark) Color(0xFF6EE7B7) else Color(0xFF047857)
    }

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(3.dp),
            color = containerColor,
            border = BorderStroke(0.6.dp, borderColor),
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .clickable(enabled = onStatusChange != null) {
                    menuExpanded = true
                }
                .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
        ) {
            Text(
                text = displayText,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                modifier = Modifier.padding(horizontal = 3.dp, vertical = 0.5.dp),
                maxLines = 1,
                softWrap = false
            )
        }

        if (onStatusChange != null) {
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                TxStatus.values().forEach { opt ->
                    val isSelected = opt == currentTxStatus
                    val optLabel = when (opt) {
                        TxStatus.DUE -> "⏳ Due"
                        TxStatus.PENDING -> "🕒 Pending"
                        TxStatus.PAID -> if (isIncome) "✓ Received" else "💳 Paid"
                        TxStatus.CLEARED -> "🛡️ Cleared"
                    }
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = optLabel,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            menuExpanded = false
                            onStatusChange(opt.key)
                        },
                        modifier = Modifier.testTag("status_option_${opt.key}")
                    )
                }
            }
        }
    }
}
