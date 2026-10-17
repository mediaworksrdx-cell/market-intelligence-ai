package com.example.marketintelligence.ui.academy

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.model.TrainingModule
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcademyScreen(
    viewModel: AcademyViewModel = hiltViewModel(),
    onNavigateToAnalysis: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Library", "Ask Mentor", "Live Training")

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("INTELLIGENCE MENTOR", color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                Text("Institutional Learning & Simulation", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
            }
            
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
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                when(selectedTab) {
                    0 -> CurriculumTab(uiState.modules) { viewModel.selectModule(it) }
                    1 -> AskMentorTab(uiState, viewModel)
                    2 -> LiveTrainingTab(
                        uiState = uiState,
                        viewModel = viewModel,
                        onNavigateToAnalysis = onNavigateToAnalysis,
                        onAskMentor = { query ->
                            viewModel.sendMessage(query)
                            selectedTab = 1
                        }
                    )
                }
            }
        }

        // Module Detail Overlay
        AnimatedVisibility(
            visible = uiState.selectedModule != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            uiState.selectedModule?.let { module ->
                ModuleDetailView(
                    module = module,
                    uiState = uiState,
                    onDismiss = { viewModel.closeModule() },
                    onInitialize = { viewModel.initializeModule(module) },
                    onRetryInitialize = { viewModel.retryInitializeModule(module) },
                    onSimAction = { viewModel.executeSimAction(it) },
                    onNiftySectorFilter = { viewModel.setNiftySectorFilter(it) },
                    onNiftyConstituentToggle = { viewModel.toggleNiftyConstituentBias(it) },
                    onNiftyDrillSubmit = { viewModel.submitNiftyDrillAnswer(it) },
                    onToggleEmotionShield = { viewModel.toggleEmotionShield() },
                    onNeutralizeBias = { viewModel.neutralizeBias(it) },
                    onActivateCooldown = { viewModel.activateRevengeCooldown() },
                    onBiasDrillSubmit = { viewModel.submitBiasDrillAnswer(it) },
                    onUpdateRisk = { cap, risk, ent, sl -> viewModel.updateRiskParameters(cap, risk, ent, sl) },
                    onSimulateRiskLoss = { viewModel.simulateDrawdownLoss() },
                    onResetRiskHalt = { viewModel.resetDrawdownCircuitBreaker() },
                    onAskMentor = { query ->
                        viewModel.sendMessage(query)
                        selectedTab = 1
                        viewModel.closeModule()
                    }
                )
            }
        }

        // Signal Detail Overlay
        AnimatedVisibility(
            visible = uiState.isSignalDetailOpen && (uiState.selectedSignalAnalysis != null || uiState.selectedScannerSignal != null),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            LiveSignalDetailModal(
                fnoSignal = uiState.selectedSignalAnalysis,
                scannerSignal = uiState.selectedScannerSignal,
                uiState = uiState,
                onDismiss = { viewModel.closeSignalAnalysis() },
                onNavigateToAnalysis = onNavigateToAnalysis,
                onSimAction = { viewModel.executeSimAction(it) },
                onAskMentor = { query ->
                    viewModel.sendMessage(query)
                    selectedTab = 1
                    viewModel.closeSignalAnalysis()
                }
            )
        }
    }
}

@Composable
fun CurriculumTab(modules: List<TrainingModule>, onModuleClick: (TrainingModule) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            Text("UNIFIED KNOWLEDGE FRAMEWORK", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(Modifier.height(4.dp))
        }
        items(modules) { module ->
            TrainingModuleCard(module, onClick = { onModuleClick(module) })
        }
        
        item {
            Spacer(Modifier.height(16.dp))
            Text("INSTITUTIONAL PERFORMANCE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCard("Theory Mastery", "84%", AppGreen, Modifier.weight(1f))
                StatCard("Sim Accuracy", "72%", Color.Cyan, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun AskMentorTab(uiState: AcademyUiState, viewModel: AcademyViewModel) {
    Column(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
        // Aarka AI Engine Status Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(AppGreen, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "AARKA AI 7B NEURAL ENGINE",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    "DYNAMIC COGNITIVE MENTOR",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.chatHistory) { chat ->
                ChatBubble(chat)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = uiState.currentInput,
                onValueChange = { viewModel.onInputChanged(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (uiState.currentInput.isEmpty()) {
                            Text(
                                "Ask Aarka AI (e.g. NIFTY 50 structure, company results, Greeks)...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Spacer(Modifier.width(6.dp))
            IconButton(
                onClick = { viewModel.sendMessage() },
                enabled = !uiState.isLoading,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
            ) {
                if (uiState.isLoading) CircularProgressIndicator(Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                else {
                    @Suppress("DEPRECATION")
                    Icon(Icons.Default.Send, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
fun LiveTrainingTab(
    uiState: AcademyUiState,
    viewModel: AcademyViewModel,
    onNavigateToAnalysis: (String, String) -> Unit,
    onAskMentor: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(AppGreen, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "LIVE SIGNAL ANALYSIS",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (uiState.simulationMode) "SIMULATION ACTIVE" else "SIMULATION",
                    color = if (uiState.simulationMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Switch(
                    checked = uiState.simulationMode,
                    onCheckedChange = { viewModel.toggleSimulation() },
                    modifier = Modifier.scale(0.65f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(0.3f)
                    )
                )
            }
        }

        // Simulation banner if active
        if (uiState.simulationMode) {
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("SIMULATION DRILL ACTIVE", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                        Text(uiState.simulationMessage, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, maxLines = 1)
                    }
                    Text(
                        "P&L: ₹+${String.format(Locale.US, "%,.0f", uiState.simulationPnl)}",
                        color = AppGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // 1. Primary Featured F&O Strategy Signal Card
        val fnoSignal = uiState.lastFnoSignal
        if (fnoSignal != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.openSignalAnalysis(fnoSignal) }
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Clickable hint banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FEATURED F&O STRATEGY", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("TAP TO OPEN FULL ANALYSIS", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(2.dp))
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(0.15f),
                                shape = RoundedCornerShape(4.dp),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Text(
                                    fnoSignal.underlyingSymbol,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                fnoSignal.strategyName.uppercase(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Surface(
                            color = AppGreen.copy(0.15f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${String.format(Locale.US, "%.1f", fnoSignal.confidenceScore)}% ACCURACY",
                                color = AppGreen,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("MENTOR COGNITIVE RATIONALE", color = MaterialTheme.colorScheme.primary, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        uiState.mentorExplanation ?: "Analyzing institutional footprints...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 15.sp,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("STUDY ZONE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("₹${String.format(Locale.US, "%,.2f", fnoSignal.entryPrice)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("INVALIDATION", color = AppRed, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("₹${String.format(Locale.US, "%,.2f", fnoSignal.stopLoss)}", color = AppRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("PROJECTION", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            Text("₹${String.format(Locale.US, "%,.2f", fnoSignal.target)}", color = AppGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.openSignalAnalysis(fnoSignal) },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Visibility, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("OPEN ANALYSIS", color = MaterialTheme.colorScheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                onNavigateToAnalysis(fnoSignal.underlyingSymbol, "INDEX")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("VIEW CHART", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 2. Confluence Scanner Studies List
        Text(
            "ACTIVE CONFLUENCE SCANNER STUDIES (${uiState.activeScannerSignals.size})",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp
        )
        Text(
            "Select any setup below to open full institutional analysis & chart",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontSize = 8.sp
        )

        Spacer(Modifier.height(8.dp))

        if (uiState.activeScannerSignals.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Scanning institutional order blocks...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.activeScannerSignals.forEach { scannerSignal ->
                    val isBull = scannerSignal.bias.contains("BULL", ignoreCase = true)
                    val sBiasColor = if (isBull) AppGreen else AppRed

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openScannerSignalAnalysis(scannerSignal) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            scannerSignal.symbol,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Text(scannerSignal.timeframe, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = sBiasColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            scannerSignal.bias,
                                            color = sBiasColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(Modifier.height(6.dp))
                            Text(
                                scannerSignal.rationale,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )

                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), thickness = 0.5.dp)
                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Zone: ₹${String.format(Locale.US, "%,.1f", scannerSignal.entryPrice)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text("SL: ₹${String.format(Locale.US, "%,.1f", scannerSignal.stopLoss)}", color = AppRed, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text("TP: ₹${String.format(Locale.US, "%,.1f", scannerSignal.target)}", color = AppGreen, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                }

                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.clickable { viewModel.openScannerSignalAnalysis(scannerSignal) }
                                ) {
                                    Text(
                                        "OPEN ANALYSIS →",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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

@Composable
fun ChatBubble(chat: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (chat.isMentor) Alignment.Start else Alignment.End
    ) {
        if (chat.isMentor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "AARKA AI 7B",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Surface(
            color = if (chat.isMentor) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(
                topStart = if (chat.isMentor) 2.dp else 10.dp,
                topEnd = 10.dp,
                bottomStart = 10.dp,
                bottomEnd = if (chat.isMentor) 10.dp else 2.dp
            ),
            modifier = Modifier.border(
                0.5.dp,
                if (chat.isMentor) MaterialTheme.colorScheme.outline.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(
                    topStart = if (chat.isMentor) 2.dp else 10.dp,
                    topEnd = 10.dp,
                    bottomStart = 10.dp,
                    bottomEnd = if (chat.isMentor) 10.dp else 2.dp
                )
            )
        ) {
            Text(
                text = chat.message,
                color = if (chat.isMentor) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(12.dp),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun TrainingModuleCard(module: TrainingModule, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).background(if(module.locked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (module.locked) Icons.Default.Lock else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (module.locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(module.title.uppercase(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text("${module.duration} • ${module.topics.joinToString(", ")}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
            if (!module.locked) {
                Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
        }
    }
}
