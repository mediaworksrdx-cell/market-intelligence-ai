package com.example.marketintelligence.ui.scanner

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.ui.theme.*
import com.example.redxaiscanner.engine.MarketBias
import com.example.redxaiscanner.engine.TradeSetup
import com.example.redxaiscanner.engine.ExplanationComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScannerScreen(
    viewModel: AIScannerViewModel = hiltViewModel(),
    onViewChart: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Auto Study", "Manual Analysis")

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "FOR EDUCATIONAL PURPOSES ONLY",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.alpha(0.03f).rotate(-45f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                "INTELLIGENCE SCANNER",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Institutional-grade analysis models",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 8.sp
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.dp
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontSize = 9.5.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (selectedTab) {
                0 -> AutoScanTab(uiState, viewModel, onViewChart)
                1 -> ManualScanTab(uiState, viewModel, onViewChart)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AutoScanTab(uiState: AIScannerUiState, viewModel: AIScannerViewModel, onViewChart: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Study List Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Study List (${uiState.autoScanAssets.size}/10)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Button(
                            onClick = { viewModel.forceAutoScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp),
                            enabled = !uiState.isAutoScanning
                        ) {
                            if (uiState.isAutoScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(11.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 1.5.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(3.dp))
                                Text("Force Scan", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Text("Periodic updates every 2 hours", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 8.sp)
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (uiState.searchQuery.isEmpty()) {
                                            Text(
                                                "Add symbol...",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                fontSize = 10.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.addAutoScanAsset(uiState.searchQuery) },
                            modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                        ) {
                            Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        uiState.autoScanAssets.forEach { asset ->
                            InputChip(
                                selected = false,
                                onClick = { viewModel.removeAutoScanAsset(asset) },
                                label = { Text(asset, color = MaterialTheme.colorScheme.onSurface, fontSize = 8.5.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(24.dp)
                            )
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(2.dp))
            Text("Current Analysis Results", color = MaterialTheme.colorScheme.onBackground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        
        if (uiState.autoScanResults.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    Text("No active analysis. Next study in ~2h", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        } else {
            items(
                items = uiState.autoScanResults,
                key = { it.underlyingSignal.underlyingSignal.symbol + it.underlyingSignal.underlyingSignal.timeframe }
            ) { setup ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = {
                        if (it == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.removeAutoScanResult(setup)
                            true
                        } else {
                            false
                        }
                    }
                )
                
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.EndToStart -> AppRed
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(12.dp))
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                ) {
                    AdvancedStudyCard(setup, onViewChart)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualScanTab(
    uiState: AIScannerUiState, 
    viewModel: AIScannerViewModel,
    onViewChart: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Analysis Configuration", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    BasicTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            "Search symbol...",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontSize = 10.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Study Timeframe", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp)
                    val timeframes = listOf("15m", "1H", "4H", "1D")
                    Row(modifier = Modifier.padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        timeframes.forEach { tf ->
                            val isSelected = tf == uiState.selectedTimeframe
                            Surface(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).clickable { viewModel.onTimeframeSelected(tf) }.border(0.5.dp, if(isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                            ) {
                                Text(
                                    text = tf,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 5.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { viewModel.startManualScan() },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Trigger Study Scan", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        
        items(
            items = uiState.manualScanResults,
            key = { it.underlyingSignal.underlyingSignal.symbol + it.underlyingSignal.underlyingSignal.timeframe }
        ) { setup ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.EndToStart) {
                        viewModel.removeManualScanResult(setup)
                        true
                    } else {
                        false
                    }
                }
            )
            
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = false,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.EndToStart -> AppRed
                        else -> Color.Transparent
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            ) {
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
    val components = setup.underlyingSignal.underlyingSignal.explanation.components
    
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(symbol, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        if (setup.underlyingSignal.confidenceScore > 85) {
                            Surface(color = Color(0xFFFFD700).copy(0.2f), shape = RoundedCornerShape(4.dp)) {
                                Text("ALIGNED", color = Color(0xFFFFD700), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("Timeframe: ${setup.underlyingSignal.underlyingSignal.timeframe}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
                
                Surface(
                    color = biasColor.copy(0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        bias.name,
                        color = biasColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Institutional Footprint Sections
            components.forEach { component ->
                InstitutionalFootprintSection(component)
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("MTF CONFLUENCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tfs = listOf("15m", "1H", "4H", "1D")
                    tfs.forEach { tf ->
                        val tfBias = if (bias == MarketBias.BULLISH) AppGreen else AppRed
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.size(6.dp).background(tfBias, CircleShape))
                            Text(tf, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("CONFIDENCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    Text("${setup.underlyingSignal.confidenceScore}%", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LevelItem("ENTRY ZONE", setup.entryPrice.toPlainString(), MaterialTheme.colorScheme.onSurface)
                LevelItem("INVALIDATION", setup.stopLossPrice.toPlainString(), AppRed)
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                LevelItem("PROJECTION 1", setup.takeProfit1.toPlainString(), AppGreen)
                LevelItem("PROJECTION 2", setup.takeProfit2.toPlainString(), AppGreen)
            }
            
            Spacer(Modifier.height(12.dp))
            
            Button(
                onClick = { onViewChart(symbol) },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("View Detailed Chart", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InstitutionalFootprintSection(component: ExplanationComponent) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when(component.componentName) {
                    "Market Structure (SMC)" -> Icons.Default.AccountTree
                    "Fair Value Gap (FVG)" -> Icons.Default.ViewStream
                    else -> Icons.Default.Analytics
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(component.componentName.uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(2.dp))
        Text(component.reasoning, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
        
        if (component.details.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                component.details.forEach { (key, value) ->
                    Column {
                        Text(key.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun IndicatorTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun LevelItem(label: String, value: String, color: Color) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}
