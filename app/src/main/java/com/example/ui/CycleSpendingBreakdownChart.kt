package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Data model for spending breakdown category
 */
data class CategorySpendingItem(
    val categoryName: String,
    val emoji: String,
    val color: Color,
    val totalAmount: Double,
    val percentage: Float,
    val count: Int
)

/**
 * Rich interactive category spending breakdown visualization (Donut Chart & Breakdown bars)
 * Designed for the Transactions/Pay Cycle view.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleSpendingBreakdownChart(
    transactions: List<TransactionEntity>,
    cycleLabel: String = "Current Pay Cycle",
    defaultExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Only analyze expense transactions with positive amounts
    val expenses = remember(transactions) {
        transactions.filter {
            !it.isIncome && !it.isStartingBalanceOrAdjustment && it.amount > 0.0 &&
                    !it.title.contains("Cancelled", ignoreCase = true) &&
                    !it.title.contains("Deleted", ignoreCase = true)
        }
    }

    if (expenses.isEmpty()) {
        return
    }

    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }
    if (totalExpenses <= 0.0) return

    val categoryPalette = listOf(
        Color(0xFF3B82F6), // Blue
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899), // Pink
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFF06B6D4), // Cyan
        Color(0xFFEF4444), // Red
        Color(0xFF6366F1), // Indigo
        Color(0xFF14B8A6), // Teal
        Color(0xFF84CC16)  // Lime
    )

    val spendingItems = remember(expenses, totalExpenses) {
        val grouped = expenses.groupBy { tx ->
            val cat = tx.category.trim()
            if (cat.isBlank()) "General" else cat
        }

        grouped.entries.mapIndexed { idx, (catName, txList) ->
            val catTotal = txList.sumOf { it.amount }
            val pct = (catTotal / totalExpenses).toFloat()
            val color = categoryPalette[idx % categoryPalette.size]
            val emoji = when {
                catName.contains("Credit", ignoreCase = true) || catName.contains("Card", ignoreCase = true) -> "💳"
                catName.contains("Rent", ignoreCase = true) || catName.contains("Mortgage", ignoreCase = true) || catName.contains("Home", ignoreCase = true) -> "🏠"
                catName.contains("Grocer", ignoreCase = true) || catName.contains("Food", ignoreCase = true) || catName.contains("Dining", ignoreCase = true) -> "🛒"
                catName.contains("Util", ignoreCase = true) || catName.contains("Electric", ignoreCase = true) || catName.contains("Water", ignoreCase = true) -> "⚡"
                catName.contains("Car", ignoreCase = true) || catName.contains("Auto", ignoreCase = true) || catName.contains("Gas", ignoreCase = true) -> "🚗"
                catName.contains("Health", ignoreCase = true) || catName.contains("Med", ignoreCase = true) -> "💊"
                catName.contains("Sub", ignoreCase = true) || catName.contains("Stream", ignoreCase = true) -> "📺"
                catName.contains("Loan", ignoreCase = true) || catName.contains("Debt", ignoreCase = true) -> "🏦"
                else -> "📋"
            }
            CategorySpendingItem(
                categoryName = catName,
                emoji = emoji,
                color = color,
                totalAmount = catTotal,
                percentage = pct,
                count = txList.size
            )
        }.sortedByDescending { it.totalAmount }
    }

    var selectedCategoryIndex by remember { mutableStateOf<Int?>(null) }
    var isExpanded by remember { mutableStateOf(defaultExpanded) }

    // Animation progress for chart rings
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(spendingItems) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("cycle_spending_chart_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = "Spending Breakdown",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "SPENDING BREAKDOWN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "$cycleLabel • ${spendingItems.size} Categories",
                            fontSize = 9.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFDC2626).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFFDC2626).copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "$%,.2f", totalExpenses),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Toggle Breakdown",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Donut Chart Canvas with Center Summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        val activeItem = selectedCategoryIndex?.let { spendingItems.getOrNull(it) }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(136.dp)
                                .testTag("donut_chart_canvas_box")
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .size(130.dp)
                                    .pointerInput(spendingItems) {
                                        detectTapGestures { offset ->
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            val dx = offset.x - center.x
                                            val dy = offset.y - center.y
                                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                            if (angle < 0) angle += 360f

                                            // Determine which slice was clicked
                                            var accumulated = -90f
                                            if (accumulated < 0) accumulated += 360f

                                            var currentStart = 0f
                                            // Map angle relative to top (-90 degrees)
                                            val angleFromTop = (angle + 90f) % 360f

                                            for ((index, item) in spendingItems.withIndex()) {
                                                val sweep = item.percentage * 360f
                                                if (angleFromTop >= currentStart && angleFromTop < currentStart + sweep) {
                                                    selectedCategoryIndex = if (selectedCategoryIndex == index) null else index
                                                    break
                                                }
                                                currentStart += sweep
                                            }
                                        }
                                    }
                            ) {
                                val strokeWidth = 24.dp.toPx()
                                val selectedStrokeWidth = 30.dp.toPx()
                                val diameter = size.minDimension - selectedStrokeWidth
                                val arcSize = Size(diameter, diameter)
                                val topLeft = Offset(
                                    (size.width - diameter) / 2f,
                                    (size.height - diameter) / 2f
                                )

                                var startAngle = -90f
                                spendingItems.forEachIndexed { index, item ->
                                    val isSelected = selectedCategoryIndex == index
                                    val sweepAngle = item.percentage * 360f * animProgress.value

                                    drawArc(
                                        color = if (selectedCategoryIndex == null || isSelected) item.color else item.color.copy(alpha = 0.35f),
                                        startAngle = startAngle,
                                        sweepAngle = maxOf(sweepAngle - 2f, 0.5f), // subtle slice gap
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = arcSize,
                                        style = Stroke(
                                            width = if (isSelected) selectedStrokeWidth else strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    )
                                    startAngle += sweepAngle
                                }
                            }

                            // Center Label
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(8.dp)
                            ) {
                                if (activeItem != null) {
                                    Text(
                                        text = activeItem.emoji,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.0f%%", activeItem.percentage * 100),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeItem.color
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", activeItem.totalAmount),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    Text(
                                        text = "Total Spent",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "$%,.0f", totalExpenses),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${expenses.size} items",
                                        fontSize = 8.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        // Top 3 Highlights beside Donut
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Top Categories",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            spendingItems.take(4).forEachIndexed { idx, item ->
                                val isSelected = selectedCategoryIndex == idx
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) item.color.copy(alpha = 0.15f) else Color.Transparent,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, item.color) else null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedCategoryIndex = if (selectedCategoryIndex == idx) null else idx
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(item.color)
                                            )
                                            Text(
                                                text = "${item.emoji} ${item.categoryName}",
                                                fontSize = 9.5.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = String.format(Locale.US, "$%,.0f", item.totalAmount),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Horizontal breakdown bars
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "CATEGORY PROPORTIONS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            letterSpacing = 0.5.sp
                        )

                        // Multi-segment stacked bar
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                spendingItems.forEachIndexed { idx, item ->
                                    val isSelected = selectedCategoryIndex == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(item.percentage.coerceAtLeast(0.01f))
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                if (selectedCategoryIndex == null || isSelected) item.color
                                                else item.color.copy(alpha = 0.35f)
                                            )
                                            .clickable {
                                                selectedCategoryIndex = if (selectedCategoryIndex == idx) null else idx
                                            }
                                    )
                                }
                            }
                        }

                        // Category filter chips / Legend
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            spendingItems.forEachIndexed { idx, item ->
                                val isSelected = selectedCategoryIndex == idx
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) item.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        if (isSelected) 1.dp else 0.5.dp,
                                        if (isSelected) item.color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .clickable {
                                            selectedCategoryIndex = if (selectedCategoryIndex == idx) null else idx
                                        }
                                        .testTag("spending_cat_chip_${idx}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(item.color)
                                        )
                                        Text(
                                            text = "${item.emoji} ${item.categoryName}",
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.0f%%", item.percentage * 100),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = item.color
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
