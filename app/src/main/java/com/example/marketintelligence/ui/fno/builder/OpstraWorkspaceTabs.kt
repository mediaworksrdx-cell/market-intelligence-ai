package com.example.marketintelligence.ui.fno.builder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.engine.StrategyAnalysisResult
import com.example.marketintelligence.domain.model.StrategyLeg
import com.example.marketintelligence.ui.fno.OpstraWorkspaceTab
import java.util.Locale
import kotlin.math.abs

@Composable
fun OpstraWorkspaceTabs(
    selectedTab: OpstraWorkspaceTab,
    onTabSelected: (OpstraWorkspaceTab) -> Unit,
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double,
    legs: List<StrategyLeg>,
    rangePct: Double,
    onRangeChange: (Double) -> Unit,
    spotSimulationPct: Double = 0.0,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Tab Headers Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OpstraWorkspaceTab.values().forEach { tab ->
                val isSelected = tab == selectedTab
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Tab Content
        when (selectedTab) {
            OpstraWorkspaceTab.PAYOFF_CHART -> {
                OpstraPayoffChart(
                    analysisResult = analysisResult,
                    spotPrice = spotPrice,
                    legs = legs,
                    rangePct = rangePct,
                    onRangeChange = onRangeChange,
                    spotSimulationPct = spotSimulationPct
                )
            }
            OpstraWorkspaceTab.FUTURES_CHART -> {
                OpstraFuturesWorkspace(
                    analysisResult = analysisResult,
                    spotPrice = spotPrice
                )
            }
            OpstraWorkspaceTab.GREEKS -> {
                OpstraGreeksWorkspace(
                    analysisResult = analysisResult,
                    legs = legs
                )
            }
            OpstraWorkspaceTab.PNL_ANALYSIS -> {
                OpstraPnlAnalysisWorkspace(
                    analysisResult = analysisResult,
                    spotPrice = spotPrice
                )
            }
        }
    }
}

@Composable
fun OpstraFuturesWorkspace(
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double
) {
    val fut = analysisResult?.futuresAnalysis

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "UNDERLYING FUTURES ANALYTICS",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
            )
            Surface(
                color = (if (fut != null && fut.isAvailable) OpstraGreen else MaterialTheme.colorScheme.outline).copy(alpha = 0.15f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (fut != null && fut.isAvailable) "LIVE FUTURES" else "DATA UNAVAILABLE",
                    color = if (fut != null && fut.isAvailable) OpstraGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (fut == null || !fut.isAvailable) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text(
                        "Real-time futures contract data for the selected underlying is currently unavailable from the market feed.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        } else {
            // Metrics grid
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricMiniCard("FUTURES PRICE", "₹${String.format(Locale.US, "%,.2f", fut.futuresPrice)}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                MetricMiniCard("UNDERLYING SPOT", "₹${String.format(Locale.US, "%,.2f", fut.spotPrice)}", MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val basisColor = if (fut.basis >= 0.0) OpstraGreen else OpstraRed
                val sign = if (fut.basis >= 0.0) "+" else ""
                MetricMiniCard("BASIS SPREAD", "$sign₹${String.format(Locale.US, "%.2f", fut.basis)} ($sign${String.format(Locale.US, "%.2f", fut.basisPercent)}%)", basisColor, Modifier.weight(1f))
                MetricMiniCard("ANNUALIZED CARRY", "$sign${String.format(Locale.US, "%.2f", fut.annualizedCostOfCarry)}%", basisColor, Modifier.weight(1f))
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricMiniCard("THEORETICAL FAIR VALUE", "₹${String.format(Locale.US, "%,.2f", fut.fairValue)}", OpstraCyan, Modifier.weight(1f))
                val premiumDiscount = fut.futuresPrice - fut.fairValue
                val pdColor = if (premiumDiscount >= 0.0) OpstraGreen else OpstraRed
                val pdLabel = if (premiumDiscount >= 0.0) "PREMIUM TO FAIR" else "DISCOUNT TO FAIR"
                MetricMiniCard(pdLabel, "₹${String.format(Locale.US, "%.2f", abs(premiumDiscount))}", pdColor, Modifier.weight(1f))
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = OpstraCyan, modifier = Modifier.size(14.dp))
                Text(
                    "Futures basis reflects net cost of carry (risk-free interest rate less dividend yield) until upcoming expiry.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.5.sp
                )
            }
        }
    }
}

@Composable
fun OpstraGreeksWorkspace(
    analysisResult: StrategyAnalysisResult?,
    legs: List<StrategyLeg>
) {
    val totalGreeks = analysisResult?.totalGreeks
    val legGreeksMap = analysisResult?.legGreeks ?: emptyMap()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            "PORTFOLIO GREEKS OVERVIEW",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(10.dp))

        // Total Greeks summary cards
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GreeksCard("DELTA (Δ)", String.format(Locale.US, "%.2f", totalGreeks?.delta ?: 0.0), OpstraCyan, Modifier.weight(1f))
            GreeksCard("GAMMA (Γ)", String.format(Locale.US, "%.4f", totalGreeks?.gamma ?: 0.0), MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
            GreeksCard("THETA (Θ)", "₹${String.format(Locale.US, "%.1f", totalGreeks?.theta ?: 0.0)}/d", Color(0xFFFF80AB), Modifier.weight(1f))
            GreeksCard("VEGA (V)", "₹${String.format(Locale.US, "%.1f", totalGreeks?.vega ?: 0.0)}/1%", OpstraGreen, Modifier.weight(1f))
            GreeksCard("RHO (ρ)", "₹${String.format(Locale.US, "%.1f", totalGreeks?.rho ?: 0.0)}", OpstraAmber, Modifier.weight(1f))
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))

        Text(
            "LEG-BY-LEG GREEKS BREAKDOWN",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(8.dp))

        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Column(modifier = Modifier.widthIn(min = 450.dp)) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("INSTRUMENT / LEG", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(130.dp))
                    Text("DELTA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                    Text("GAMMA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(55.dp))
                    Text("THETA (₹/D)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                    Text("VEGA (₹/%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                    Text("RHO (₹/%)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                }

                Spacer(Modifier.height(4.dp))

                val activeLegs = legs.filter { it.isEnabled }
                if (activeLegs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No active legs to calculate Greeks", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                } else {
                    activeLegs.forEach { leg ->
                        val lg = legGreeksMap[leg.id]
                        val isBuy = leg.type.equals("BUY", ignoreCase = true)
                        val legColor = if (isBuy) OpstraGreen else OpstraRed

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${leg.type} ${leg.strike.toInt()} ${leg.instrument}",
                                color = legColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.width(130.dp)
                            )
                            Text(
                                String.format(Locale.US, "%.3f", lg?.delta ?: 0.0),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(55.dp)
                            )
                            Text(
                                String.format(Locale.US, "%.4f", lg?.gamma ?: 0.0),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(55.dp)
                            )
                            Text(
                                "₹${String.format(Locale.US, "%.1f", lg?.theta ?: 0.0)}",
                                color = if ((lg?.theta ?: 0.0) >= 0.0) OpstraGreen else OpstraRed,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(70.dp)
                            )
                            Text(
                                "₹${String.format(Locale.US, "%.1f", lg?.vega ?: 0.0)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(70.dp)
                            )
                            Text(
                                "₹${String.format(Locale.US, "%.1f", lg?.rho ?: 0.0)}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(70.dp)
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun OpstraPnlAnalysisWorkspace(
    analysisResult: StrategyAnalysisResult?,
    spotPrice: Double
) {
    val scenarios = analysisResult?.scenarioMatrix ?: emptyList()
    val volSens = analysisResult?.volatilitySensitivity ?: emptyList()
    val timeSens = analysisResult?.timeDecaySensitivity ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(
            "MULTI-SCENARIO P&L MATRIX",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp
        )
        Text(
            "Strategy theoretical P&L across underlying spot price movements",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.5.sp
        )

        Spacer(Modifier.height(10.dp))

        // Scenario Table
        val scrollState = rememberScrollState()
        Box(modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
            Column(modifier = Modifier.widthIn(min = 400.dp)) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SPOT MOVE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                    Text("TARGET SPOT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                    Text("T+0 P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Text("EXPIRY P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp))
                    Text("DELTA", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(60.dp))
                }

                Spacer(Modifier.height(4.dp))

                scenarios.forEach { row ->
                    val isZero = abs(row.spotChangePct) < 0.01
                    val rowBg = if (isZero) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
                    val t0Color = if (row.todayPnl >= 0.0) OpstraGreen else OpstraRed
                    val expColor = if (row.expiryPnl >= 0.0) OpstraGreen else OpstraRed
                    val sign = if (row.spotChangePct > 0) "+" else ""

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "$sign${row.spotChangePct.toInt()}%",
                            color = if (isZero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            fontSize = 9.sp,
                            fontWeight = if (isZero) FontWeight.Black else FontWeight.Bold,
                            modifier = Modifier.width(70.dp)
                        )
                        Text(
                            "₹${String.format(Locale.US, "%,.1f", row.simulatedSpot)}",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(90.dp)
                        )
                        Text(
                            "${if (row.todayPnl >= 0) "+" else ""}₹${String.format(Locale.US, "%,.1f", row.todayPnl)}",
                            color = t0Color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            "${if (row.expiryPnl >= 0) "+" else ""}₹${String.format(Locale.US, "%,.1f", row.expiryPnl)}",
                            color = expColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(80.dp)
                        )
                        Text(
                            String.format(Locale.US, "%.2f", row.delta),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(60.dp)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), thickness = 0.5.dp)
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
        Spacer(Modifier.height(12.dp))

        // Volatility & Decay Sensitivity Summary
        Text(
            "VOLATILITY & TIME DECAY SENSITIVITY",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Volatility shifts column
            Column(modifier = Modifier.weight(1f)) {
                Text("IMPLIED VOLATILITY IMPACT", color = OpstraCyan, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                volSens.forEach { vs ->
                    val color = if (vs.pnl >= 0.0) OpstraGreen else OpstraRed
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(vs.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp)
                        Text(
                            "${if (vs.pnl >= 0) "+" else ""}₹${String.format(Locale.US, "%,.1f", vs.pnl)}",
                            color = color,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Time decay column
            Column(modifier = Modifier.weight(1f)) {
                Text("TIME DECAY IMPACT", color = Color(0xFFFF80AB), fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                timeSens.forEach { ts ->
                    val color = if (ts.pnl >= 0.0) OpstraGreen else OpstraRed
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ts.label, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp)
                        Text(
                            "${if (ts.pnl >= 0) "+" else ""}₹${String.format(Locale.US, "%,.1f", ts.pnl)}",
                            color = color,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricMiniCard(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(
                value,
                color = valueColor,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun GreeksCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 6.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, color = color, fontSize = 9.5.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        }
    }
}
