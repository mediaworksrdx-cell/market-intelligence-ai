package com.marketintelligence.ai.ui.fno

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.marketintelligence.ai.domain.model.*
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FnoScreen(
    viewModel: FnoViewModel = hiltViewModel(),
    onOpenBuildupScanner: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("MARKET", "ANALYTICS", "BUILDER")

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("F&O INTELLIGENCE HUB", color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(12.dp))

        // Search & Add Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                modifier = Modifier.weight(1f).height(52.dp),
                placeholder = { Text("Search F&O Asset...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) },
                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp)) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { viewModel.addAsset() },
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Asset Mini-Header
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(uiState.selectedAsset.uppercase(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "SPOT: ₹${"%.2f".format(uiState.spotPrice)}",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Surface(
                color = (if (uiState.summary.changePercent >= 0) AppGreen else AppRed).copy(0.1f),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.border(0.5.dp, if(uiState.summary.changePercent >= 0) AppGreen else AppRed, RoundedCornerShape(4.dp))
            ) {
                Text(
                    "${if (uiState.summary.changePercent >= 0) "+" else ""}${uiState.summary.changePercent}%",
                    color = if (uiState.summary.changePercent >= 0) AppGreen else AppRed,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 2.dp
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> SummaryTab(uiState)
                1 -> OptionsStudyTab(uiState, onOpenBuildupScanner)
                2 -> StrategyBuilderTab(uiState, viewModel)
            }
        }
    }
}

@Composable
fun SummaryTab(uiState: FnoUiState) {
    val scrollState = rememberScrollState()
    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("FUTURES TERM STRUCTURE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Spacer(Modifier.height(8.dp))
                for (fp in uiState.summary.contracts) {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fp.expiry, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("₹${"%.2f".format(fp.ltp)}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            "${if (fp.changePercent >= 0) "+" else ""}${fp.changePercent}%",
                            color = if (fp.changePercent >= 0) AppGreen else AppRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.summary.contracts.lastOrNull() != fp) Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("IMPLIED VOL (IV)", "${uiState.summary.iv}%", Modifier.weight(1f))
            SummaryStatCard("PUT-CALL RATIO", "${uiState.summary.pcr}", Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStatCard("MAX PAIN", "₹${uiState.summary.maxPain.toInt()}", Modifier.weight(1f))
            SummaryStatCard("LOT SIZE", "${uiState.summary.lotSize}", Modifier.weight(1f))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("OPEN INTEREST CONCENTRATION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("RESISTANCE (CALL OI)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("₹${uiState.summary.highestCallOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("SUPPORT (PUT OI)", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text("₹${uiState.summary.highestPutOIStrike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun OptionsStudyTab(uiState: FnoUiState, onOpenBuildupScanner: () -> Unit) {
    var viewMode by remember { mutableStateOf("OI") } // "OI", "GREEKS", "PAIN", "IV"

    Column(modifier = Modifier.fillMaxSize()) {
        // Updated Buildup Scanner Card - Large and Clickable
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Bigger Height
                .clickable { onOpenBuildupScanner() }
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("BUILDUP SCANNER", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Institutional Derivatives Positioning Analyzer", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = CircleShape) {
                    Icon(Icons.Default.Analytics, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp).size(24.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Toggle View
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ADVANCED CHAIN", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp)).padding(2.dp)) {
                listOf("OI", "GREEKS", "GEX", "PAIN", "IV").forEach { mode ->
                    val isSelected = viewMode == mode
                    Box(
                        modifier = Modifier
                            .background(if (isSelected) MaterialTheme.colorScheme.outline else Color.Transparent, RoundedCornerShape(3.dp))
                            .clickable { viewMode = mode }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Text(mode, color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (viewMode == "GEX") {
            GexAnalyticsView(
                profile = uiState.gexProfile,
                spotPrice = uiState.spotPrice
            )
        } else if (viewMode == "PAIN") {
            MaxPainAnalyticsView(
                history = uiState.maxPainHistory,
                currentMaxPain = uiState.summary.maxPain,
                spotPrice = uiState.spotPrice
            )
        } else if (viewMode == "IV") {
            IvAnalyticsView(
                history = uiState.ivHistory,
                currentIv = uiState.summary.iv
            )
        } else {
            // Existing Chain Logic
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewMode == "OI") {
                    Text("CALL OI", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Text("LTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("STRIKE", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("LTP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("PUT OI", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                } else {
                    Text("DELTA", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("THETA", color = Color.Magenta, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("STRIKE", color = MaterialTheme.colorScheme.onSurface, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("THETA", color = Color.Magenta, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text("DELTA", color = Color.Cyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 60.dp)) {
                uiState.optionChain?.let { chain ->
                    val maxOI = chain.strikes.maxOf { maxOf(it.callOI, it.putOI) }.coerceAtLeast(1.0)
                    items(chain.strikes) { strike ->
                        val isITMCall = strike.strike < uiState.spotPrice
                        val isITMPut = strike.strike > uiState.spotPrice
                        
                        Box(modifier = Modifier.fillMaxWidth().background(if(isITMCall || isITMPut) MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f) else Color.Transparent)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewMode == "OI") {
                                    Box(modifier = Modifier.weight(1f)) {
                                        Box(modifier = Modifier.fillMaxWidth((strike.callOI / maxOI).toFloat().coerceIn(0f, 1f)).height(14.dp).background(AppRed.copy(0.15f), RoundedCornerShape(2.dp)))
                                        Text("${(strike.callOI / 1000).toInt()}k", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp), fontFamily = FontFamily.Monospace)
                                    }
                                    Text("${strike.callLTP.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text("₹${strike.strike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text("${strike.putLTP.toInt()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, modifier = Modifier.weight(0.6f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                        Box(modifier = Modifier.fillMaxWidth((strike.putOI / maxOI).toFloat().coerceIn(0f, 1f)).height(14.dp).background(AppGreen.copy(0.15f), RoundedCornerShape(2.dp)))
                                        Text("${(strike.putOI / 1000).toInt()}k", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp), fontFamily = FontFamily.Monospace)
                                    }
                                } else {
                                    Text("%.2f".format(strike.callGreeks.delta), color = Color.Cyan, fontSize = 10.sp, modifier = Modifier.weight(1f), fontFamily = FontFamily.Monospace)
                                    Text("%.1f".format(strike.callGreeks.theta), color = Color.Magenta, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text("₹${strike.strike.toInt()}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text("%.1f".format(strike.putGreeks.theta), color = Color.Magenta, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontFamily = FontFamily.Monospace)
                                    Text("%.2f".format(strike.putGreeks.delta), color = Color.Cyan, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyBuilderTab(uiState: FnoUiState, viewModel: FnoViewModel) {
    val scrollState = rememberScrollState()
    var isCategoryDropdownExpanded by remember { mutableStateOf(false) }
    var isStrategyDropdownExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        Text("INSTITUTIONAL STRATEGY BUILDER", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        
        // 1. Category & Strategy Selection
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Category Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedCard(
                    onClick = { isCategoryDropdownExpanded = true },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedCategory.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = isCategoryDropdownExpanded,
                    onDismissRequest = { isCategoryDropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    StrategyCategory.values().forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.setStrategyCategory(category)
                                isCategoryDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Strategy Dropdown
            Box(modifier = Modifier.weight(1.5f)) {
                OutlinedCard(
                    onClick = { isStrategyDropdownExpanded = true },
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(uiState.selectedStrategy?.name ?: "SELECT ARCHITECTURE", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                DropdownMenu(
                    expanded = isStrategyDropdownExpanded,
                    onDismissRequest = { isStrategyDropdownExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    viewModel.strategyDefinitions[uiState.selectedCategory]?.forEach { strategyName ->
                        DropdownMenuItem(
                            text = { Text(strategyName, color = MaterialTheme.colorScheme.onSurface) },
                            onClick = {
                                viewModel.selectPredefinedStrategy(strategyName)
                                isStrategyDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // 2. Asset Display (from Main Header) & Leg Management
        if (uiState.activeLegs.isNotEmpty()) {
            Text("STRATEGY LEGS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            uiState.activeLegs.forEach { leg ->
                StrategyLegRow(leg, onUpdate = { viewModel.updateLeg(leg.id, it) }, onRemove = { viewModel.removeLeg(leg.id) })
                Spacer(Modifier.height(4.dp))
            }
            
            Button(
                onClick = { viewModel.addLeg("CALL", "BUY", uiState.spotPrice) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(32.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ADD CUSTOM LEG", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // 3. Analytics & Build Buttons
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.analyzeStrategy() },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("ANALYZE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.saveStrategy() },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text("BUILD & SAVE", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // 4. Payoff Metrics & Visualization
        if (uiState.selectedStrategy != null) {
            val strategy = uiState.selectedStrategy!!
            Spacer(Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(strategy.name.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StrategyMetric("MAX PROFIT", if(strategy.maxProfit > 1000000) "UNLIMITED" else "₹${strategy.maxProfit.toInt()}", AppGreen)
                        StrategyMetric("MAX LOSS", if(strategy.maxLoss < -1000000) "UNLIMITED" else "₹${strategy.maxLoss.toInt()}", AppRed)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StrategyMetric("BREAKEVEN", strategy.breakeven.joinToString(", ") { it.toInt().toString() }, MaterialTheme.colorScheme.onSurface)
                        StrategyMetric("WIN PROB", "${strategy.probability}%", Color.Cyan)
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        AdvancedPayoffChart(strategy.payoffPoints, uiState.spotPrice)
                    }
                }
            }
        }
        
        // 5. Tracked Strategies Section
        if (uiState.trackedStrategies.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("MY TRACKED STRATEGIES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            uiState.trackedStrategies.forEach { saved ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(saved.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Active until Expiry", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        }
                        Text("${saved.probability}% POP", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun StrategyLegRow(leg: StrategyLeg, onUpdate: (Double) -> Unit, onRemove: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            val ratioText = if (leg.qty > 50) "${leg.qty / 50}x " else ""
            Text(
                "$ratioText${leg.type} ${leg.instrument}", 
                color = if(leg.type == "BUY") AppGreen else AppRed, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(76.dp)
            )
            
            // Editable Strike & Expiry
            Text(
                "STRIKE:", 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontSize = 8.sp, 
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Text(
                "${leg.strike.toInt()} (${leg.expiry})",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = onRemove, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun StrategyMetric(label: String, value: String, color: Color) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun AdvancedPayoffChart(points: List<PayoffPoint>, spotPrice: Double) {
    if (points.isEmpty()) return
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        val minPrice = points.minOf { it.price }
        val maxPrice = points.maxOf { it.price }
        val priceRange = (maxPrice - minPrice).coerceAtLeast(1.0).toFloat()
        
        val minPnl = points.minOf { it.pnl }
        val maxPnl = points.maxOf { it.pnl }
        val pnlRange = (maxPnl - minPnl).coerceAtLeast(1.0).toFloat()
        
        // Zero Line
        val zeroY = height * (1 - (0 - minPnl) / pnlRange).toFloat()
        drawLine(
            color = outline,
            start = Offset(0f, zeroY),
            end = Offset(width, zeroY),
            strokeWidth = 1.dp.toPx()
        )
        
        // Spot Price Line
        val spotX = width * ((spotPrice - minPrice) / priceRange).toFloat()
        if (spotX in 0f..width) {
            drawLine(
                color = onSurface.copy(0.3f),
                start = Offset(spotX, 0f),
                end = Offset(spotX, height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        }

        // Main Payoff Path
        val mainPath = Path()
        points.forEachIndexed { index, point ->
            val x = width * ((point.price - minPrice) / priceRange).toFloat()
            val y = height * (1 - (point.pnl - minPnl) / pnlRange).toFloat()
            if (index == 0) mainPath.moveTo(x, y) else mainPath.lineTo(x, y)
        }
        
        drawPath(
            path = mainPath,
            color = onSurface,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Mark Breakeven points
        points.forEachIndexed { index, point ->
            if (index > 0) {
                val prev = points[index-1]
                if ((prev.pnl < 0 && point.pnl >= 0) || (prev.pnl >= 0 && point.pnl < 0)) {
                    val x = width * ((point.price - minPrice) / priceRange).toFloat()
                    drawCircle(onSurface, radius = 3.dp.toPx(), center = Offset(x, zeroY))
                }
            }
        }
    }
}

@Composable
fun MaxPainAnalyticsView(history: List<Double>, currentMaxPain: Double, spotPrice: Double) {
    val diff = spotPrice - currentMaxPain
    val diffPercent = if (currentMaxPain > 0) (diff / currentMaxPain) * 100 else 0.0
    val dataPoints = if (history.isNotEmpty()) history else listOf(currentMaxPain - 100, currentMaxPain - 50, currentMaxPain + 50, currentMaxPain)
    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("MAX PAIN PINNING & MIGRATION", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text("Theoretical writer optimal settlement strike vs spot trajectory", color = onSurfaceVariant, fontSize = 9.sp)

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MAX PAIN STRIKE", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${currentMaxPain.toInt()}", color = onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("SPOT GAP", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${if (diff >= 0) "+" else ""}${"%.1f".format(diff)} (${if (diffPercent >= 0) "+" else ""}${"%.2f".format(diffPercent)}%)",
                            color = if (diff >= 0) AppGreen else AppRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PINNING BIAS", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(if (kotlin.math.abs(diffPercent) < 0.8) "HIGH PIN" else "VOL EXPAND", color = Color.Cyan, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val minVal = (dataPoints + spotPrice).minOrNull() ?: 0.0
                        val maxVal = (dataPoints + spotPrice).maxOrNull() ?: 1.0
                        val range = (maxVal - minVal).coerceAtLeast(10.0).toFloat()

                        // Grid lines
                        for (step in 0..3) {
                            val y = h * (step / 3f)
                            drawLine(outlineColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                        }

                        // Spot price line
                        val spotY = h * (1f - ((spotPrice - minVal) / range).toFloat()).coerceIn(0f, h)
                        drawLine(
                            color = onSurface.copy(alpha = 0.4f),
                            start = Offset(0f, spotY),
                            end = Offset(w, spotY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                        )

                        // Max Pain Line
                        val path = Path()
                        val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else w
                        dataPoints.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val y = h * (1f - ((pt - minVal) / range).toFloat()).coerceIn(0f, h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(primaryColor, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path, color = primaryColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("--- Spot Price Baseline", color = onSurface.copy(alpha = 0.5f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("— Max Pain Migration", color = primaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun IvAnalyticsView(history: List<Double>, currentIv: Double) {
    val dataPoints = if (history.isNotEmpty()) history else listOf(currentIv - 1.2, currentIv - 0.5, currentIv + 0.8, currentIv)
    val maxIv = dataPoints.maxOrNull() ?: currentIv
    val minIv = dataPoints.minOrNull() ?: currentIv
    val avgIv = if (dataPoints.isNotEmpty()) dataPoints.average() else currentIv
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val ivColor = Color(0xFFBD7CFF)

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("IMPLIED VOLATILITY (IV) REGIME", color = ivColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                Text("Option pricing volatility skew across recent sessions", color = onSurfaceVariant, fontSize = 9.sp)

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("CURRENT IV", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(currentIv)}%", color = ivColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("10-DAY HIGH / LOW", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(maxIv)}% / ${"%.1f".format(minIv)}%", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("MEAN IV", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${"%.1f".format(avgIv)}%", color = Color.Cyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val minVal = (minIv * 0.9).coerceAtLeast(0.0)
                        val maxVal = maxIv * 1.1
                        val range = (maxVal - minVal).coerceAtLeast(1.0).toFloat()

                        // Grid lines
                        for (step in 0..3) {
                            val y = h * (step / 3f)
                            drawLine(outlineColor.copy(alpha = 0.3f), Offset(0f, y), Offset(w, y), 0.5.dp.toPx())
                        }

                        // Mean line
                        val meanY = h * (1f - ((avgIv - minVal) / range).toFloat()).coerceIn(0f, h)
                        drawLine(
                            color = Color.Cyan.copy(alpha = 0.4f),
                            start = Offset(0f, meanY),
                            end = Offset(w, meanY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )

                        // IV Path
                        val path = Path()
                        val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else w
                        dataPoints.forEachIndexed { index, pt ->
                            val x = index * stepX
                            val y = h * (1f - ((pt - minVal) / range).toFloat()).coerceIn(0f, h)
                            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            drawCircle(ivColor, radius = 3.dp.toPx(), center = Offset(x, y))
                        }
                        drawPath(path, color = ivColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("--- Mean IV Baseline", color = Color.Cyan.copy(alpha = 0.6f), fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("— Implied Volatility Curve", color = ivColor, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}

@Composable
fun GexAnalyticsView(profile: com.example.marketintelligence.domain.engine.GexProfile?, spotPrice: Double) {
    if (profile == null || profile.strikeGexList.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("Calculating Dealer Gamma Exposure (GEX)...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val isLongGamma = profile.totalNetGex >= 0

    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, outlineColor, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("DEALER GAMMA EXPOSURE (GEX)", color = primaryColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, letterSpacing = 0.5.sp)
                        Text("Market Maker hedging flow & structural boundaries", color = onSurfaceVariant, fontSize = 9.sp)
                    }
                    Surface(
                        color = (if (isLongGamma) AppGreen else AppRed).copy(0.12f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.border(0.5.dp, if (isLongGamma) AppGreen else AppRed, RoundedCornerShape(4.dp))
                    ) {
                        Text(
                            if (isLongGamma) "LONG GAMMA" else "SHORT GAMMA",
                            color = if (isLongGamma) AppGreen else AppRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TOTAL NET GEX", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("${if (profile.totalNetGex >= 0) "+" else ""}${"%.1f".format(profile.totalNetGex)} Cr", color = if (isLongGamma) AppGreen else AppRed, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("GAMMA FLIP", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${profile.gammaFlipStrike.toInt()}", color = Color.Cyan, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CALL / PUT WALL", color = onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${profile.callWallStrike.toInt()} / ₹${profile.putWallStrike.toInt()}", color = onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // GEX Strike distribution Canvas
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val strikes = profile.strikeGexList
                        val maxAbsGex = strikes.maxOfOrNull { maxOf(kotlin.math.abs(it.callGex), kotlin.math.abs(it.putGex)) }?.coerceAtLeast(1.0) ?: 1.0
                        val barHeight = (h / strikes.size).coerceAtMost(16.dp.toPx())
                        val centerX = w * 0.5f

                        // Center Zero Line
                        drawLine(
                            color = outlineColor.copy(alpha = 0.5f),
                            start = Offset(centerX, 0f),
                            end = Offset(centerX, h),
                            strokeWidth = 1.dp.toPx()
                        )

                        strikes.forEachIndexed { index, s ->
                            val y = index * (h / strikes.size) + 2f
                            
                            // Put GEX (Left, negative / red)
                            val putWidth = ((kotlin.math.abs(s.putGex) / maxAbsGex) * (w * 0.45f)).toFloat()
                            drawRect(
                                color = AppRed.copy(alpha = 0.8f),
                                topLeft = Offset(centerX - putWidth, y),
                                size = Size(putWidth, barHeight * 0.7f)
                            )

                            // Call GEX (Right, positive / green)
                            val callWidth = ((s.callGex / maxAbsGex) * (w * 0.45f)).toFloat()
                            drawRect(
                                color = AppGreen.copy(alpha = 0.8f),
                                topLeft = Offset(centerX, y),
                                size = Size(callWidth, barHeight * 0.7f)
                            )

                            // Highlight Spot Strike
                            if (kotlin.math.abs(s.strike - spotPrice) < 50.0) {
                                drawCircle(
                                    color = onSurface,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(centerX, y + barHeight * 0.35f)
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("← Put GEX (Short Vol Risk)", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("Spot ● Flip ₹${profile.gammaFlipStrike.toInt()}", color = onSurfaceVariant, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    Text("Call GEX (Pinning Magnet) →", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }
        Spacer(Modifier.height(60.dp))
    }
}
