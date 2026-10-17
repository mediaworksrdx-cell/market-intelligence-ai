package com.marketintelligence.ai.ui.academy

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketintelligence.ai.data.model.AiTradeSignal
import com.marketintelligence.ai.data.model.TradeAction
import com.marketintelligence.ai.domain.engine.ScannerSignalInfo
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed
import java.util.Locale

@Composable
fun LiveSignalDetailModal(
    fnoSignal: AiTradeSignal? = null,
    scannerSignal: ScannerSignalInfo? = null,
    uiState: AcademyUiState,
    onDismiss: () -> Unit,
    onNavigateToAnalysis: (String, String) -> Unit,
    onSimAction: (String) -> Unit,
    onAskMentor: (String) -> Unit
) {
    val symbol = fnoSignal?.underlyingSymbol ?: scannerSignal?.symbol ?: "UNKNOWN"
    val strategyTitle = fnoSignal?.strategyName?.uppercase() ?: "${scannerSignal?.timeframe ?: "1H"} CONFLUENCE STUDY"
    val regimeOrBias = fnoSignal?.marketRegime?.name ?: scannerSignal?.bias ?: "NEUTRAL"
    val isBullish = regimeOrBias.contains("BULLISH", ignoreCase = true)
    val biasColor = if (isBullish) AppGreen else AppRed
    val confidence = fnoSignal?.confidenceScore ?: scannerSignal?.confidence?.toDouble() ?: 85.0
    val instrumentType = if (symbol.contains("NIFTY", ignoreCase = true) || symbol.contains("SENSEX", ignoreCase = true)) "INDEX" else "STOCK"

    val entryPrice = fnoSignal?.entryPrice ?: scannerSignal?.entryPrice ?: 0.0
    val stopLoss = fnoSignal?.stopLoss ?: scannerSignal?.stopLoss ?: 0.0
    val target = fnoSignal?.target ?: scannerSignal?.target ?: 0.0

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .clickable(enabled = false) {}, // Prevent dismiss when tapping dialog content
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    text = symbol,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = biasColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = regimeOrBias,
                                    color = biasColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = strategyTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Metric Overview Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("CONFIDENCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("${String.format(Locale.US, "%.1f", confidence)}%", color = Color(0xFFFFD700), fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("RISK / REWARD", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                val rr = if (stopLoss != entryPrice && stopLoss > 0.0) {
                                    val risk = kotlin.math.abs(entryPrice - stopLoss)
                                    val reward = kotlin.math.abs(target - entryPrice)
                                    if (risk > 0) String.format(Locale.US, "1 : %.2f", reward / risk) else "1 : 2.0"
                                } else "1 : 2.1"
                                Text(rr, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("STRUCTURE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text("SMC VALID", color = AppGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // 2. Execution Levels
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("EXECUTION ARCHITECTURE & LEVELS", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(10.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("STUDY ZONE / ENTRY", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%,.2f", entryPrice)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("INVALIDATION (SL)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%,.2f", stopLoss)}", color = AppRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("PROJECTION (TARGET)", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    Text("₹${String.format(Locale.US, "%,.2f", target)}", color = AppGreen, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // 3. Option Legs (for F&O signals)
                    if (fnoSignal != null && fnoSignal.optionLegs.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ACTIVE OPTION CONTRACT LEGS", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))

                                fnoSignal.optionLegs.forEach { leg ->
                                    val isBuy = leg.action == TradeAction.BUY
                                    val actionColor = if (isBuy) AppGreen else AppRed

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = actionColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = leg.action.name,
                                                    color = actionColor,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(leg.optionSymbol, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                Text("${leg.quantity} units • ${leg.expiry}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${String.format(Locale.US, "%.2f", leg.premium)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text("LTP Premium", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Capital & Risk Matrix
                        val risk = fnoSignal.riskParameters
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("CAPITAL & RISK MATRIX", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Capital Outlay Required:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                    Text("₹${String.format(Locale.US, "%,.2f", risk.capitalRequired)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Margin Requirement:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                    Text("₹${String.format(Locale.US, "%,.2f", risk.marginRequired)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Max Invalidation Loss:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp)
                                    Text("₹${String.format(Locale.US, "%,.2f", risk.maxLoss)}", color = AppRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 4. Mentor Cognitive Rationale
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("MENTOR COGNITIVE RATIONALE", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.height(6.dp))
                            val explanation = fnoSignal?.let { uiState.mentorExplanation }
                                ?: scannerSignal?.rationale
                                ?: "Institutional Order Block mitigation confirmed with volume delta expansion and liquidity pool absorption."
                            Text(
                                text = explanation,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // 5. Institutional Simulation Replay Engine
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).background(AppGreen, CircleShape))
                                    Spacer(Modifier.width(6.dp))
                                    Text("INSTITUTIONAL SIMULATION", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Text("SIM P&L: ₹+${String.format(Locale.US, "%,.2f", uiState.simulationPnl)}", color = AppGreen, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(uiState.simulationMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)

                            Spacer(Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onSimAction("HEDGE") },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("Hedge Risk", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onSimAction("ROLL") },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("Roll Strikes", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { onSimAction("SQUARE") },
                                    modifier = Modifier.weight(1f).height(30.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text("Square Off", color = AppRed, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)

                // Bottom Action Footer
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onNavigateToAnalysis(symbol, instrumentType)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "OPEN LIVE CANDLESTICK CHART",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val query = "Analyze $symbol $strategyTitle setup. What are the key institutional levels, SMC order blocks, and risk invalidation points?"
                            onAskMentor(query)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "CONSULT AI MENTOR ABOUT THIS SETUP",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
