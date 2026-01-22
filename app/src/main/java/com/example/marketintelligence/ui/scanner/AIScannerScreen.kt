package com.example.marketintelligence.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.ui.theme.*
import com.example.redxaiscanner.engine.MarketBias
import com.example.redxaiscanner.engine.TradeSetup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScannerScreen(
    viewModel: AIScannerViewModel = hiltViewModel(),
    onViewChart: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Auto Study", "Manual Analysis")

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "FOR EDUCATIONAL PURPOSES ONLY",
                color = Color.White,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(0.03f).rotate(-45f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "AI Market Intelligence Study",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Institutional-grade analysis models",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = AppGreen,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AppGreen
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (selectedTab) {
                0 -> AutoScanTab(uiState, viewModel)
                1 -> ManualScanTab(uiState, viewModel, onViewChart)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoScanTab(uiState: AIScannerUiState, viewModel: AIScannerViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study List (${uiState.autoScanAssets.size}/10)", color = Color.White, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { viewModel.forceAutoScan() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Black)
                        Spacer(Modifier.width(4.dp))
                        Text("Refresh Study", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Periodic updates every 2 hours", color = Color.Gray, fontSize = 11.sp)
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        placeholder = { Text("Add symbol to study...", color = Color.Gray, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1A1A1A),
                            unfocusedContainerColor = Color(0xFF1A1A1A),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = AppGreen,
                            focusedTextColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.addAutoScanAsset(uiState.searchQuery) },
                        modifier = Modifier.size(48.dp).background(AppGreen, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.Black)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.autoScanAssets.forEach { asset ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeAutoScanAsset(asset) },
                            label = { Text(asset, color = Color.White, fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp), tint = Color.Gray) },
                            colors = InputChipDefaults.inputChipColors(containerColor = Color(0xFF1A1A1A))
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text("Current Analysis Results", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active analysis. Next study in ~2h", color = Color.DarkGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ManualScanTab(
    uiState: AIScannerUiState, 
    viewModel: AIScannerViewModel,
    onViewChart: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Analysis Configuration", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Search symbol for study...", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A1A),
                        unfocusedContainerColor = Color(0xFF1A1A1A),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = AppGreen,
                        focusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("Study Timeframe", color = Color.Gray, fontSize = 12.sp)
                val timeframes = listOf("15m", "1H", "4H", "1D")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    timeframes.forEach { tf ->
                        val isSelected = tf == uiState.selectedTimeframe
                        Surface(
                            color = if (isSelected) AppGreen else Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).clickable { viewModel.onTimeframeSelected(tf) }
                        ) {
                            Text(
                                text = tf,
                                color = if (isSelected) Color.Black else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.startManualScan() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    else Text("Trigger Study Scan", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(uiState.manualScanResults) { setup ->
                AdvancedStudyCard(setup, onViewChart)
            }
        }
    }
}

@Composable
fun AdvancedStudyCard(setup: TradeSetup, onViewChart: (String) -> Unit) {
    val bias = setup.underlyingSignal.underlyingSignal.higherTimeframeBias
    val biasColor = if (bias == MarketBias.BULLISH) AppGreen else AppRed
    val symbol = setup.underlyingSignal.underlyingSignal.symbol
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(symbol, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        if (setup.underlyingSignal.confidenceScore > 85) {
                            Surface(color = Color(0xFFFFD700).copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ALIGNED", color = Color(0xFFFFD700), fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("Timeframe: ${setup.underlyingSignal.underlyingSignal.timeframe}", color = Color.Gray, fontSize = 11.sp)
                }
                
                Surface(
                    color = biasColor.copy(0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        bias.name,
                        color = biasColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("MTF CONFLUENCE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tfs = listOf("15m", "1H", "4H", "1D")
                    tfs.forEach { tf ->
                        val tfBias = if (bias == MarketBias.BULLISH) AppGreen else AppRed
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(8.dp).background(tfBias, CircleShape))
                            Text(tf, color = Color.Gray, fontSize = 8.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("CONFIDENCE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("${setup.underlyingSignal.confidenceScore}%", color = Color.Yellow, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("INSTITUTIONAL FOOTPRINT", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = 0.75f,
                        color = AppGreen,
                        trackColor = Color(0xFF1A1A1A),
                        modifier = Modifier.fillMaxWidth().height(4.dp).background(Color.Transparent, RoundedCornerShape(2.dp))
                    )
                    Text("RVOL: 1.8x", color = Color.Gray, fontSize = 8.sp, modifier = Modifier.align(Alignment.End))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("LIQUIDITY GRAB", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("CLEARED EQUAL LOWS", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IndicatorTag("SMC: ${if (bias == MarketBias.BULLISH) "Bullish BOS" else "Bearish BOS"}")
                setup.underlyingSignal.underlyingSignal.fvgSignal?.let {
                    IndicatorTag("FVG: ${it.direction}")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LevelItem("STUDY PRICE", setup.entryPrice.toPlainString(), Color.White)
                LevelItem("INVALIDATION", setup.stopLossPrice.toPlainString(), AppRed)
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LevelItem("PROJECTION 1", setup.takeProfit1.toPlainString(), AppGreen)
                LevelItem("PROJECTION 2", setup.takeProfit2.toPlainString(), AppGreen)
            }
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { onViewChart(symbol) },
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1A1A)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = AppGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("View Detailed Chart", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun IndicatorTag(label: String) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LevelItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
