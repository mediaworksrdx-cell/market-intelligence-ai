package com.example.marketintelligence.ui.fno.builder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.engine.StrategyAnalysisResult
import com.example.marketintelligence.domain.model.StrategyLeg
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@Composable
fun OpstraPositionsPanel(
    legs: List<StrategyLeg>,
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double,
    lotSize: Int,
    strikeStep: Double,
    onToggleLegEnabled: (String) -> Unit,
    onDuplicateLeg: (String) -> Unit,
    onRemoveLeg: (String) -> Unit,
    onToggleLegType: (String) -> Unit,
    onToggleLegInstrument: (String) -> Unit,
    onShiftStrike: (String, Int) -> Unit,
    onUpdateLegQty: (String, Int) -> Unit,
    onUpdateLegEntryPrice: (String, Double) -> Unit,
    onSelectAllLegs: (Boolean) -> Unit,
    onRequestReset: () -> Unit,
    onAddAtmLeg: () -> Unit,
    onOpenChainPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingPriceLegId by remember { mutableStateOf<String?>(null) }
    var priceInputText by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        // --- 1. STRATEGY ANALYTICAL METRICS CARD ---
        OpstraStrategyMetricsCard(
            analysisResult = analysisResult,
            spotPrice = spotPrice
        )

        Spacer(Modifier.height(14.dp))

        // --- 2. POSITIONS HEADER & BULK CONTROLS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "STRATEGY POSITIONS (${legs.size})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                if (legs.isNotEmpty()) {
                    val enabledCount = legs.count { it.isEnabled }
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            "$enabledCount Active",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Select All & Reset controls
            if (legs.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    val allSelected = legs.all { it.isEnabled }
                    TextButton(
                        onClick = { onSelectAllLegs(!allSelected) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            if (allSelected) "Deselect All" else "Select All",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onRequestReset() }
                            .border(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(11.dp))
                            Text("Reset", color = MaterialTheme.colorScheme.error, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // --- 3. MULTI-LEG POSITIONS LIST ---
        if (legs.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Layers, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("No positions in strategy", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text("Add an ATM leg, select a preset architecture, or pick strikes from option chain below.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                legs.forEach { leg ->
                    OpstraLegRow(
                        leg = leg,
                        spotPrice = spotPrice,
                        lotSize = lotSize,
                        strikeStep = strikeStep,
                        onToggleEnabled = { onToggleLegEnabled(leg.id) },
                        onToggleType = { onToggleLegType(leg.id) },
                        onToggleInstrument = { onToggleLegInstrument(leg.id) },
                        onShiftStrike = { d -> onShiftStrike(leg.id, d) },
                        onUpdateQty = { q -> onUpdateLegQty(leg.id, q) },
                        onOpenEditPrice = {
                            editingPriceLegId = leg.id
                            priceInputText = leg.entryPrice.toString()
                        },
                        onDuplicate = { onDuplicateLeg(leg.id) },
                        onRemove = { onRemoveLeg(leg.id) }
                    )
                }
            }
        }

        // --- 4. ADD POSITION BUTTONS ---
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onAddAtmLeg,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("ADD ATM LEG", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onOpenChainPicker,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.weight(1.3f).height(36.dp),
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.AutoGraph, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("OPTION CHAIN PICKER", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Direct Edit Price Dialog
    editingPriceLegId?.let { legId ->
        AlertDialog(
            onDismissRequest = { editingPriceLegId = null },
            title = { Text("Edit Entry Premium", fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = priceInputText,
                    onValueChange = { priceInputText = it },
                    label = { Text("Entry Price (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = priceInputText.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onUpdateLegEntryPrice(legId, parsed)
                        }
                        editingPriceLegId = null
                    }
                ) {
                    Text("Apply", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPriceLegId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun OpstraLegRow(
    leg: StrategyLeg,
    spotPrice: Double,
    lotSize: Int,
    strikeStep: Double,
    onToggleEnabled: () -> Unit,
    onToggleType: () -> Unit,
    onToggleInstrument: () -> Unit,
    onShiftStrike: (Int) -> Unit,
    onUpdateQty: (Int) -> Unit,
    onOpenEditPrice: () -> Unit,
    onDuplicate: () -> Unit,
    onRemove: () -> Unit
) {
    val isBuy = leg.type.equals("BUY", ignoreCase = true)
    val legColor = if (isBuy) OpstraGreen else OpstraRed
    val effLotSize = if (leg.lotSize > 0) leg.lotSize else if (lotSize > 0) lotSize else 65
    val currentLots = max(1, leg.qty / effLotSize)
    val isCall = leg.instrument.equals("CALL", ignoreCase = true)
    val isPut = leg.instrument.equals("PUT", ignoreCase = true)

    val halfStep = (strikeStep / 2.0).coerceAtLeast(0.5)
    val isAtm = spotPrice > 0.0 && abs(leg.strike - spotPrice) <= halfStep
    val computedMoneyness = when {
        leg.moneyness.isNotBlank() -> leg.moneyness
        leg.instrument == "FUT" -> "FUT"
        isAtm -> "ATM"
        isCall && spotPrice > 0.0 && leg.strike < spotPrice -> "ITM"
        isCall && spotPrice > 0.0 && leg.strike > spotPrice -> "OTM"
        isPut && spotPrice > 0.0 && leg.strike > spotPrice -> "ITM"
        isPut && spotPrice > 0.0 && leg.strike < spotPrice -> "OTM"
        else -> "ATM"
    }

    val alpha = if (leg.isEnabled) 1.0f else 0.4f

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (leg.isEnabled) 0.65f else 0.25f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                0.5.dp,
                if (leg.isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Row 1: Checkbox + BUY/SELL + Instrument + Moneyness + Strike Stepper + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Checkbox enable/disable
                    Checkbox(
                        checked = leg.isEnabled,
                        onCheckedChange = { onToggleEnabled() },
                        modifier = Modifier.size(20.dp),
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )

                    Spacer(Modifier.width(2.dp))

                    // BUY / SELL Chip (Clickable)
                    Surface(
                        color = legColor.copy(alpha = 0.18f * alpha),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onToggleType() }
                            .border(0.5.dp, legColor.copy(alpha = alpha), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            leg.type,
                            color = legColor.copy(alpha = alpha),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Instrument Chip (CALL / PUT / FUT)
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .clickable { onToggleInstrument() }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = alpha), RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            leg.instrument,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Moneyness
                    Surface(
                        color = OpstraCyan.copy(alpha = 0.12f * alpha),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier.border(0.5.dp, OpstraCyan.copy(alpha = 0.4f * alpha), RoundedCornerShape(3.dp))
                    ) {
                        Text(
                            computedMoneyness,
                            color = OpstraCyan.copy(alpha = alpha),
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                // Strike Stepper: [-] STRIKE [+]
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onShiftStrike(-1) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            "₹${leg.strike.toInt()}",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onShiftStrike(1) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Duplicate Button
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate Leg", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
                    }

                    // Remove Button
                    IconButton(onClick = onRemove, modifier = Modifier.size(22.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Leg", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Row 2: Lots Stepper + Entry Price (Editable) + LTP + Live P&L
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lots stepper: [-] X Lots [+] (QTY)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { if (currentLots > 1) onUpdateQty((currentLots - 1) * effLotSize) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("-", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    Text(
                        "${currentLots}x (${leg.qty})",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 3.dp)
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onUpdateQty((currentLots + 1) * effLotSize) }
                            .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(3.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("+", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Entry Price with Edit Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.clickable { onOpenEditPrice() }
                ) {
                    Text("Entry:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text(
                        "₹${String.format(Locale.US, "%.1f", leg.entryPrice)}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp))
                }

                // LTP
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("LTP:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                    Text(
                        "₹${String.format(Locale.US, "%.1f", leg.currentPrice)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Live P&L
                val pnlSign = if (leg.pnl >= 0.0) "+" else ""
                val pnlColor = if (leg.pnl >= 0.0) OpstraGreen else OpstraRed
                Text(
                    "$pnlSign₹${String.format(Locale.US, "%.1f", leg.pnl)} ($pnlSign${String.format(Locale.US, "%.1f", leg.pnlPercent)}%)",
                    color = pnlColor.copy(alpha = alpha),
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun OpstraStrategyMetricsCard(
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double
) {
    if (analysisResult == null) return

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Live Status Banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(OpstraGreen))
                    Text(
                        "STRATEGY METRICS & RISK PROFILE",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                // POP Badge
                Surface(
                    color = OpstraCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.border(0.5.dp, OpstraCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                ) {
                    Text(
                        "POP: ${analysisResult.probabilityOfProfit}%",
                        color = OpstraCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Row 1: Max Profit, Max Loss, Risk to Reward
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val maxProfitStr = if (analysisResult.isMaxProfitUnlimited) "UNLIMITED" else "₹${String.format(Locale.US, "%,d", analysisResult.maxProfit.toInt())}"
                val maxLossStr = if (analysisResult.isMaxLossUnlimited) "UNLIMITED" else "₹${String.format(Locale.US, "%,d", abs(analysisResult.maxLoss).toInt())}"

                MetricColumn("MAX PROFIT", maxProfitStr, OpstraGreen)
                MetricColumn("MAX LOSS", maxLossStr, OpstraRed)
                MetricColumn("RISK / REWARD", analysisResult.riskRewardRatio, OpstraAmber)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // Row 2: Net Credit/Debit, Estimated Margin, Capital Required
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val isCredit = analysisResult.netCreditDebit >= 0.0
                val netLabel = if (isCredit) "NET CREDIT" else "NET DEBIT"
                val netStr = "${if (isCredit) "+" else "-"}₹${String.format(Locale.US, "%,.1f", abs(analysisResult.netCreditDebit))}"
                val netColor = if (isCredit) OpstraGreen else MaterialTheme.colorScheme.onSurface

                MetricColumn(netLabel, netStr, netColor)
                MetricColumn("EST. MARGIN", "₹${String.format(Locale.US, "%,d", analysisResult.estimatedMargin.toLong())}", MaterialTheme.colorScheme.primary)
                MetricColumn("FUNDS REQUIRED", "₹${String.format(Locale.US, "%,d", analysisResult.capitalRequired.toLong())}", MaterialTheme.colorScheme.onSurface)
            }

            Spacer(Modifier.height(8.dp))

            // Greeks Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Δ: ${String.format(Locale.US, "%.2f", analysisResult.totalGreeks.delta)}", color = OpstraCyan, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Γ: ${String.format(Locale.US, "%.4f", analysisResult.totalGreeks.gamma)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("Θ: ₹${String.format(Locale.US, "%.1f", analysisResult.totalGreeks.theta)}/d", color = Color(0xFFFF80AB), fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("V: ₹${String.format(Locale.US, "%.1f", analysisResult.totalGreeks.vega)}/1%", color = OpstraGreen, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text("ρ: ₹${String.format(Locale.US, "%.1f", analysisResult.totalGreeks.rho)}", color = OpstraAmber, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
private fun MetricColumn(label: String, value: String, color: Color) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(value, color = color, fontSize = 11.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
    }
}
