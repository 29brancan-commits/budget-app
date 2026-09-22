package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Swipeable transaction row wrapper:
 * - Swipe Right: Quickly cycle status (Due -> Pending -> Paid -> Cleared)
 * - Swipe Left: Reveal quick action buttons (Edit, Duplicate, Delete)
 */
@Composable
fun SwipeableTransactionRow(
    transaction: TransactionEntity,
    onDelete: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDuplicate: (() -> Unit)? = null,
    onUpdateStatus: ((String) -> Unit)? = null,
    onToggleCompleted: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val dark = isSystemInDarkTheme()

    val currentStatus = transaction.getEffectiveStatus()
    val nextStatus = TxStatus.nextStatus(currentStatus)
    val nextStatusObj = TxStatus.from(nextStatus)

    val offsetX = remember { Animatable(0f) }

    val thresholdRightPx = with(density) { 70.dp.toPx() }
    val maxRightPx = with(density) { 150.dp.toPx() }
    val revealedLeftWidthDp = if (onDuplicate != null) 165.dp else 115.dp
    val revealedLeftWidthPx = with(density) { revealedLeftWidthDp.toPx() }
    val maxLeftPx = with(density) { (revealedLeftWidthDp + 30.dp).toPx() }
    val thresholdLeftSnapPx = with(density) { 55.dp.toPx() }

    // Visual indicators for next status cycle (Swipe Right)
    val (statusBgColor, statusContentColor, statusPrompt) = when (nextStatus) {
        "pending" -> Triple(
            if (dark) Color(0xFF78350F) else Color(0xFFFEF3C7),
            if (dark) Color(0xFFFDE68A) else Color(0xFFB45309),
            "Mark Pending 🕒"
        )
        "paid" -> Triple(
            if (dark) Color(0xFF1E3A8A) else Color(0xFFDBEAFE),
            if (dark) Color(0xFFBFDBFE) else Color(0xFF1D4ED8),
            "Mark Paid 💳"
        )
        "cleared" -> Triple(
            if (dark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
            if (dark) Color(0xFFA7F3D0) else Color(0xFF047857),
            "Mark Cleared 🛡️"
        )
        else -> Triple(
            if (dark) Color(0xFF334155) else Color(0xFFE2E8F0),
            if (dark) Color(0xFFE2E8F0) else Color(0xFF475569),
            "Reset to Due ⏳"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .testTag("swipeable_tx_${transaction.id}")
    ) {
        // BACKGROUND TRAY
        // Left Side: Status Progression (Swipe Right)
        if (offsetX.value > 0f) {
            val progress = (offsetX.value / thresholdRightPx).coerceIn(0f, 1.5f)
            val isThresholdPassed = offsetX.value >= thresholdRightPx

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(statusBgColor)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.offset {
                        IntOffset(((offsetX.value * 0.15f) - 10f).roundToInt(), 0)
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size((26 * progress.coerceAtLeast(0.8f)).dp)
                            .clip(CircleShape)
                            .background(if (isThresholdPassed) statusContentColor else statusContentColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = nextStatusObj.icon,
                            fontSize = 13.sp
                        )
                    }

                    Column {
                        Text(
                            text = if (isThresholdPassed) "Release to update" else "Swipe right to cycle",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusContentColor.copy(alpha = 0.85f)
                        )
                        Text(
                            text = statusPrompt,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = statusContentColor
                        )
                    }
                }
            }
        }

        // Right Side: Quick Action Buttons (Swipe Left)
        if (offsetX.value < 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (dark) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .padding(end = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onEdit != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF3B82F6),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                    onEdit()
                                }
                                .testTag("swipe_action_edit_${transaction.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Transaction",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    if (onDuplicate != null) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF8B5CF6),
                            modifier = Modifier
                                .size(36.dp)
                                .clickable {
                                    coroutineScope.launch {
                                        offsetX.animateTo(0f)
                                    }
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onDuplicate()
                                }
                                .testTag("swipe_action_duplicate_${transaction.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Duplicate Transaction",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                coroutineScope.launch {
                                    offsetX.animateTo(0f)
                                }
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDelete()
                            }
                            .testTag("swipe_action_delete_${transaction.id}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Transaction",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // FOREGROUND CONTENT (The Transaction Item Row)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(transaction.id, currentStatus, transaction.isCompleted) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            coroutineScope.launch {
                                if (offsetX.value >= thresholdRightPx) {
                                    // Trigger status cycle
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (onUpdateStatus != null) {
                                        onUpdateStatus(nextStatus)
                                    } else {
                                        onToggleCompleted?.invoke()
                                    }
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                } else if (offsetX.value <= -thresholdLeftSnapPx) {
                                    // Snap open the action tray
                                    offsetX.animateTo(
                                        -revealedLeftWidthPx,
                                        spring(
                                            dampingRatio = Spring.DampingRatioLowBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                } else {
                                    // Snap back to closed
                                    offsetX.animateTo(
                                        0f,
                                        spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMediumLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetX.animateTo(
                                    0f,
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                )
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val target = (offsetX.value + dragAmount).coerceIn(-maxLeftPx, maxRightPx)
                                offsetX.snapTo(target)
                            }
                        }
                    )
                }
                .clickable(enabled = offsetX.value != 0f) {
                    // Tap on open card snaps it closed
                    coroutineScope.launch {
                        offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    }
                }
        ) {
            content()
        }
    }
}
