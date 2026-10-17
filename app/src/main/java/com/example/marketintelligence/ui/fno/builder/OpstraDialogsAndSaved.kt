package com.example.marketintelligence.ui.fno.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.model.OptionStrategy
import java.util.Locale
import kotlin.math.abs

@Composable
fun OpstraResetConfirmDialog(
    isOpen: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Reset Strategy Positions?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "Are you sure you want to clear all positions? Any unsaved modifications to this strategy will be permanently lost.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Confirm Reset", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OpstraDeleteConfirmDialog(
    isOpen: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text("Delete Saved Strategy?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                "This saved strategy will be removed from your tracked portfolio.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OpstraRenameDialog(
    isOpen: Boolean,
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename Strategy", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = currentName,
                onValueChange = onNameChange,
                label = { Text("Strategy Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = currentName.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OpstraSavedStrategiesList(
    strategies: List<OptionStrategy>,
    editingStrategyId: String?,
    onEdit: (OptionStrategy) -> Unit,
    onRename: (String, String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDeleteRequest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (strategies.isEmpty()) return

    var expandedStrategyId by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "TRACKED PORTFOLIO STRATEGIES (${strategies.size})",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(8.dp))

        strategies.forEach { strategy ->
            val isEditing = editingStrategyId == strategy.id
            val isExpanded = expandedStrategyId == strategy.id

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isEditing) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { expandedStrategyId = if (isExpanded) null else strategy.id }
                    .border(
                        if (isEditing) 1.dp else 0.5.dp,
                        if (isEditing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strategy.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (isEditing) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            "ACTIVE IN WORKSPACE",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = strategy.legs.joinToString(" • "),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.5.sp,
                                maxLines = if (isExpanded) 10 else 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )

                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("POP: ${strategy.probability}%", color = OpstraCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                val maxPStr = if (strategy.isMaxProfitUnlimited || strategy.maxProfit > 1000000) "∞" else "₹${strategy.maxProfit.toInt()}"
                                val maxLStr = if (strategy.isMaxLossUnlimited || strategy.maxLoss < -1000000) "∞" else "₹${abs(strategy.maxLoss).toInt()}"
                                Text(
                                    "Max P/L: $maxPStr / $maxLStr",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.5.sp
                                )
                                if (strategy.rawLegs.isNotEmpty()) {
                                    val savedPnl = strategy.rawLegs.sumOf { it.pnl }
                                    val pnlColor = if (savedPnl >= 0.0) OpstraGreen else OpstraRed
                                    val sign = if (savedPnl >= 0.0) "+" else ""
                                    Text(
                                        "Live: $sign₹${String.format(Locale.US, "%.1f", savedPnl)}",
                                        color = pnlColor,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }

                        // Action buttons
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Expand/Collapse Chevron
                            Icon(
                                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Strategy Details",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            // Edit / Load into builder
                            IconButton(onClick = { onEdit(strategy) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Strategy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            // Rename
                            IconButton(onClick = { onRename(strategy.id, strategy.name) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "Rename Strategy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                            // Duplicate
                            IconButton(onClick = { onDuplicate(strategy.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Strategy", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(15.dp))
                            }
                            // Delete
                            IconButton(onClick = { onDeleteRequest(strategy.id) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Strategy", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // --- EXPANDED DETAILS PANEL ---
                    if (isExpanded) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                        Spacer(Modifier.height(8.dp))

                        // 1. Detailed Legs List
                        Text(
                            "STRATEGY LEGS ARCHITECTURE",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(Modifier.height(4.dp))

                        if (strategy.rawLegs.isNotEmpty()) {
                            strategy.rawLegs.forEach { leg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            color = if (leg.type.equals("BUY", ignoreCase = true)) OpstraGreen.copy(alpha = 0.15f) else OpstraRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                leg.type.uppercase(),
                                                color = if (leg.type.equals("BUY", ignoreCase = true)) OpstraGreen else OpstraRed,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                        Text(
                                            "${leg.strike.toInt()} ${leg.instrument}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (leg.moneyness.isNotBlank()) {
                                            Text(
                                                "[${leg.moneyness}]",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "${leg.qty} qty (${leg.lots} lot)",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 8.5.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            "@ ₹${String.format(Locale.US, "%.1f", leg.entryPrice)}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        } else {
                            strategy.legs.forEach { legStr ->
                                Text("• $legStr", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 2. Risk / Return Metrics Grid
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Max Profit / Max Loss
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("MAX PROFIT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    val maxPStr = if (strategy.isMaxProfitUnlimited || strategy.maxProfit > 1000000) "UNLIMITED" else "₹${String.format(Locale.US, "%,.1f", strategy.maxProfit)}"
                                    Text(maxPStr, color = OpstraGreen, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.height(2.dp))
                                    Text("MAX LOSS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    val maxLStr = if (strategy.isMaxLossUnlimited || strategy.maxLoss < -1000000) "UNLIMITED" else "₹${String.format(Locale.US, "%,.1f", abs(strategy.maxLoss))}"
                                    Text(maxLStr, color = OpstraRed, fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Margin / Capital
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("EST. MARGIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%,.0f", strategy.estimatedMargin)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.height(2.dp))
                                    Text("NET PREMIUM", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    val premStr = if (strategy.netCreditDebit >= 0) "Credit ₹${String.format(Locale.US, "%.1f", strategy.netCreditDebit)}" else "Debit ₹${String.format(Locale.US, "%.1f", abs(strategy.netCreditDebit))}"
                                    Text(premStr, color = if (strategy.netCreditDebit >= 0) OpstraGreen else OpstraAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }

                            // Breakevens
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1.2f).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Text("BREAKEVENS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    val beText = if (strategy.breakeven.isNotEmpty()) strategy.breakeven.joinToString(" • ") { "₹${it.toInt()}" } else "None"
                                    Text(beText, color = OpstraAmber, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(Modifier.height(2.dp))
                                    Text("EXPIRY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                    Text(strategy.expiry.ifBlank { "CURRENT" }, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 3. Load into Workspace Button
                        Button(
                            onClick = { onEdit(strategy) },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("LOAD STRATEGY INTO WORKSPACE & VIEW PAYOFF", fontWeight = FontWeight.Bold, fontSize = 9.5.sp)
                        }
                    }
                }
            }
        }
    }
}
