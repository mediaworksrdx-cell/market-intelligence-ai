package com.marketintelligence.cryptotracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.ui.composable.ChartIndicatorConfig
import com.marketintelligence.ai.ui.composable.InstitutionalChartEngine
import com.marketintelligence.cryptotracker.engine.FVGDirection
import com.marketintelligence.cryptotracker.engine.SetupDirection
import com.marketintelligence.cryptotracker.engine.StructureEventType
import com.marketintelligence.cryptotracker.engine.StructureState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinDetailScreen(
    viewModel: CryptoScannerViewModel = hiltViewModel()
) {
    val state by viewModel.detailState.collectAsState()

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BullishColor)
        }
        return
    }

    if (state.error != null) {
        Box(modifier = Modifier.fillMaxSize().background(BgDark), contentAlignment = Alignment.Center) {
            Text(state.error!!, color = BearishColor)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.symbol, color = TextPrimary, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("$%.4f", state.currentPrice),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                state.setup?.let {
                    Column(horizontalAlignment = Alignment.End) {
                        val dirColor = if (it.direction == SetupDirection.LONG) BullishColor else BearishColor
                        Badge(text = it.direction.name, color = dirColor, bgColor = dirColor.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Score: ${it.score.total}/100", color = GradeAPlus, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Interactive Candlestick Chart
            if (state.candles.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    InstitutionalChartEngine(
                        symbol = state.symbol,
                        timeframe = "15m",
                        candles = state.candles,
                        indicatorConfig = ChartIndicatorConfig(
                            showSMC = true,
                            showEMA = true,
                            showVWAP = true,
                            showVolume = true,
                            showRSI = false,
                            showMACD = false
                        )
                    )
                }
            }

            // Setup Section (Highlighted)
            state.setup?.let { setup ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GradeAPlus.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text("Trade Setup (${setup.grade.name.replace("_", "+")})", color = GradeAPlus, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Divider(modifier = Modifier.padding(vertical = 8.dp), color = TextSecondary.copy(alpha = 0.3f))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItem("Entry", String.format("%.4f", setup.entry))
                            DetailItem("Stop Loss", String.format("%.4f", setup.stopLoss), BearishColor)
                            DetailItem("R:R", "1:${String.format("%.1f", setup.riskRewardRatio)}")
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Take Profits", color = TextSecondary, fontSize = 12.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DetailItem("TP1", String.format("%.4f", setup.tp1), BullishColor)
                            DetailItem("TP2", String.format("%.4f", setup.tp2), BullishColor)
                            DetailItem("TP3", String.format("%.4f", setup.tp3), BullishColor)
                        }
                    }
                }
            }

            // Structure Section
            state.smcAnalysis?.let { smc ->
                SectionCard(title = "Market Structure") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailItem("Structure State", smc.structureState.name, if (smc.structureState == StructureState.BULLISH) BullishColor else BearishColor)
                        smc.premiumDiscountZone?.let { pd ->
                            DetailItem("Equilibrium", String.format("%.4f", pd.equilibrium), TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Recent Events", color = TextSecondary, fontSize = 12.sp)
                    val hasChoch = smc.structureEvents.any { it.type == StructureEventType.CHoCH }
                    val hasBos = smc.structureEvents.any { it.type == StructureEventType.BOS }
                    if (hasChoch) Text("• Change of Character (CHoCH) Detected", color = GradeAPlus, fontSize = 14.sp)
                    if (hasBos) Text("• Break of Structure (BOS) Confirmed", color = GradeA, fontSize = 14.sp)
                    if (!hasChoch && !hasBos) Text("Consolidating within range.", color = TextSecondary, fontSize = 14.sp)
                }
            }

            // Liquidity Section
            state.smcAnalysis?.let { smc ->
                SectionCard(title = "Liquidity & Sweeps") {
                    if (smc.sweeps.isEmpty()) {
                        Text("No recent liquidity sweeps detected.", color = TextSecondary)
                    } else {
                        smc.sweeps.take(3).forEach { sweep ->
                            Text("• Sweep at ${String.format("%.4f", sweep.sweepPrice)} (${sweep.level.side} pool)", color = GradeB)
                        }
                    }
                }
            }

            // FVG Section
            SectionCard(title = "Fair Value Gaps (FVGs)") {
                if (state.fvgs.isEmpty()) {
                    Text("No active FVGs found.", color = TextSecondary)
                } else {
                    state.fvgs.forEach { fvg ->
                        val color = if (fvg.direction == FVGDirection.BULLISH) BullishColor else BearishColor
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${fvg.direction} FVG (Score: ${fvg.qualityScore.total})", color = color)
                            Text("${String.format("%.4f", fvg.top)} - ${String.format("%.4f", fvg.bottom)}", color = TextPrimary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            // RSI Section
            SectionCard(title = "RSI Analysis") {
                state.rsiAnalysis?.let { rsi ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailItem("Current RSI", String.format("%.1f", rsi.currentRSI))
                        DetailItem("Momentum", rsi.momentumDirection.name, if (rsi.momentumDirection.name == "BULLISH") BullishColor else BearishColor)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text("Multi-Timeframe RSI", color = TextSecondary, fontSize = 12.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    state.multiTfRSI.forEach { (tf, value) ->
                        val color = when {
                            value > 70 -> BearishColor
                            value < 30 -> BullishColor
                            else -> TextSecondary
                        }
                        DetailItem(tf, String.format("%.1f", value), color)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun DetailItem(label: String, value: String, valueColor: Color = TextPrimary) {
    Column {
        Text(label, color = TextSecondary, fontSize = 12.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
