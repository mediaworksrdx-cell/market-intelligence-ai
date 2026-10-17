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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marketintelligence.ai.domain.model.TrainingModule
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed
import java.util.Locale

@Composable
fun ModuleDetailView(
    module: TrainingModule,
    uiState: AcademyUiState,
    onDismiss: () -> Unit,
    onInitialize: () -> Unit,
    onRetryInitialize: () -> Unit = onInitialize,
    onSimAction: (String) -> Unit = {},
    onNiftySectorFilter: (String) -> Unit = {},
    onNiftyConstituentToggle: (String) -> Unit = {},
    onNiftyDrillSubmit: (Int) -> Unit = {},
    onToggleEmotionShield: () -> Unit = {},
    onNeutralizeBias: (String) -> Unit = {},
    onActivateCooldown: () -> Unit = {},
    onBiasDrillSubmit: (Int) -> Unit = {},
    onUpdateRisk: (Double?, Double?, Double?, Double?) -> Unit = { _, _, _, _ -> },
    onSimulateRiskLoss: () -> Unit = {},
    onResetRiskHalt: () -> Unit = {},
    onAskMentor: (String) -> Unit = {}
) {
    val isEngineReady = uiState.engineInitStatus == EngineInitStatus.READY && uiState.initializedModuleId == module.id
    val isEngineInitializing = uiState.engineInitStatus == EngineInitStatus.INITIALIZING && uiState.selectedModule?.id == module.id
    val isEngineFailed = uiState.engineInitStatus == EngineInitStatus.FAILED && uiState.selectedModule?.id == module.id

    var detailTab by remember(module.id, isEngineReady) {
        mutableIntStateOf(if (isEngineReady) 1 else 0)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.88f))
            .padding(horizontal = 10.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.96f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when {
                                        module.title.contains("Simulation", ignoreCase = true) -> Icons.Default.Tune
                                        module.title.contains("Nifty", ignoreCase = true) -> Icons.Default.TrendingUp
                                        module.title.contains("Bias", ignoreCase = true) -> Icons.Default.Lightbulb
                                        module.title.contains("Risk", ignoreCase = true) -> Icons.Default.Security
                                        else -> Icons.Default.School
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                module.title.uppercase(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = when {
                                        isEngineReady -> AppGreen.copy(alpha = 0.15f)
                                        isEngineInitializing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        isEngineFailed -> AppRed.copy(alpha = 0.15f)
                                        else -> Color(0xFFFFB300).copy(alpha = 0.15f)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        when {
                                            isEngineReady -> "ONLINE & READY"
                                            isEngineInitializing -> "INITIALIZING..."
                                            isEngineFailed -> "INIT FAILED"
                                            else -> "STANDBY"
                                        },
                                        color = when {
                                            isEngineReady -> AppGreen
                                            isEngineInitializing -> MaterialTheme.colorScheme.primary
                                            isEngineFailed -> AppRed
                                            else -> Color(0xFFFFB300)
                                        },
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    module.duration,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Sub Tabs (Curriculum vs Interactive Engine)
                TabRow(
                    selectedTabIndex = detailTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline) }
                ) {
                    Tab(
                        selected = detailTab == 0,
                        onClick = { detailTab = 0 },
                        text = {
                            Text("CURRICULUM & THEORY", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    )
                    Tab(
                        selected = detailTab == 1,
                        onClick = {
                            detailTab = 1
                            if (!isEngineReady && !isEngineInitializing && !isEngineFailed) {
                                onInitialize()
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isEngineReady) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(AppGreen, CircleShape)
                                    )
                                    Spacer(Modifier.width(5.dp))
                                } else if (isEngineInitializing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(8.dp),
                                        strokeWidth = 1.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(5.dp))
                                }
                                Text("INTERACTIVE ENGINE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Tab Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    if (detailTab == 0) {
                        ModuleCurriculumTab(
                            module = module,
                            uiState = uiState,
                            onInitialize = {
                                onInitialize()
                                detailTab = 1
                            },
                            onRetryInitialize = {
                                onRetryInitialize()
                                detailTab = 1
                            },
                            onOpenEngine = { detailTab = 1 },
                            onAskMentor = onAskMentor
                        )
                    } else {
                        // Check if this module has been initialized and is ready
                        if (!isEngineReady) {
                            EngineStandbyAndInitView(
                                module = module,
                                uiState = uiState,
                                onInitialize = onInitialize,
                                onRetryInitialize = onRetryInitialize
                            )
                        } else {
                            // Dedicated Engine Interactive Content
                            when (module.toEngineType()) {
                                AcademyEngineType.INSTITUTIONAL_SIMULATION -> {
                                    SimulationEngineInteractiveContent(
                                        uiState = uiState,
                                        onSimAction = onSimAction,
                                        onAskMentor = onAskMentor
                                    )
                                }
                                AcademyEngineType.NIFTY_50_DYNAMICS -> {
                                    NiftyDynamicsInteractiveContent(
                                        uiState = uiState,
                                        onSectorFilter = onNiftySectorFilter,
                                        onConstituentToggle = onNiftyConstituentToggle,
                                        onDrillSubmit = onNiftyDrillSubmit,
                                        onAskMentor = onAskMentor
                                    )
                                }
                                AcademyEngineType.BEHAVIORAL_BIAS -> {
                                    BehavioralBiasInteractiveContent(
                                        uiState = uiState,
                                        onToggleShield = onToggleEmotionShield,
                                        onNeutralize = onNeutralizeBias,
                                        onActivateCooldown = onActivateCooldown,
                                        onDrillSubmit = onBiasDrillSubmit,
                                        onAskMentor = onAskMentor
                                    )
                                }
                                AcademyEngineType.RISK_DISCIPLINE -> {
                                    RiskDisciplineInteractiveContent(
                                        uiState = uiState,
                                        onUpdateRisk = onUpdateRisk,
                                        onSimulateLoss = onSimulateRiskLoss,
                                        onResetHalt = onResetRiskHalt,
                                        onAskMentor = onAskMentor
                                    )
                                }
                                else -> {
                                    GeneralModuleInteractiveContent(module = module, onAskMentor = onAskMentor)
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
fun EngineStandbyAndInitView(
    module: TrainingModule,
    uiState: AcademyUiState,
    onInitialize: () -> Unit,
    onRetryInitialize: () -> Unit
) {
    val isInitializing = uiState.engineInitStatus == EngineInitStatus.INITIALIZING
    val isFailed = uiState.engineInitStatus == EngineInitStatus.FAILED
    val errorMsg = uiState.engineInitError

    val engineType = module.toEngineType()
    val engineTitle = when (engineType) {
        AcademyEngineType.INSTITUTIONAL_SIMULATION -> "INSTITUTIONAL SIMULATION ENGINE v2.4"
        AcademyEngineType.NIFTY_50_DYNAMICS -> "NIFTY 50 DYNAMICS ENGINE"
        AcademyEngineType.BEHAVIORAL_BIAS -> "BEHAVIORAL BIAS DETECTION ENGINE"
        AcademyEngineType.RISK_DISCIPLINE -> "RISK DISCIPLINE ENGINE"
        else -> module.title.uppercase()
    }

    val subsystems = when (engineType) {
        AcademyEngineType.INSTITUTIONAL_SIMULATION -> listOf(
            "Historical Tick Replay & Order Flow Feeds",
            "Delta-Neutral Gamma Hedging Calculator",
            "Option Surface Greeks & Volatility Drift",
            "Simulated Institutional P&L Book"
        )
        AcademyEngineType.NIFTY_50_DYNAMICS -> listOf(
            "Index Heavyweight Constituent Weight Matrix",
            "Sectoral Beta & Relative Strength Radar",
            "Smart Money (SMC) Liquidity Pools & FVGs",
            "FII / DII Derivative Participant Flow"
        )
        AcademyEngineType.BEHAVIORAL_BIAS -> listOf(
            "Trader Discipline Index Scoring Model",
            "Real-time Emotion Shield & Cortisol Monitor",
            "Cognitive Biases (FOMO, Loss Aversion) Auditor",
            "Mechanical 20-Min Cooling Circuit Breaker"
        )
        AcademyEngineType.RISK_DISCIPLINE -> listOf(
            "Mathematical 1% Capital Risk Allocation Matrix",
            "Dynamic Lot Sizing & Invalidation Distance Calculator",
            "1:2 and 1:3 Multi-Target Projection Model",
            "Hard Drawdown Circuit Breaker (Halt at 3.0%)"
        )
        else -> listOf(
            "Institutional Knowledge Base Runtime",
            "Neural Mentor Telemetry",
            "Interactive Sandbox Environment"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(Modifier.height(4.dp))

        // Center Engine Icon
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            modifier = Modifier.size(60.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = when (engineType) {
                        AcademyEngineType.INSTITUTIONAL_SIMULATION -> Icons.Default.Tune
                        AcademyEngineType.NIFTY_50_DYNAMICS -> Icons.Default.TrendingUp
                        AcademyEngineType.BEHAVIORAL_BIAS -> Icons.Default.Lightbulb
                        AcademyEngineType.RISK_DISCIPLINE -> Icons.Default.Security
                        else -> Icons.Default.School
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // Engine Title & Subtitle
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = engineTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Dedicated Interactive Neural Module • ${module.duration}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isFailed -> AppRed.copy(alpha = 0.1f)
                    isInitializing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                }
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                1.dp,
                when {
                    isFailed -> AppRed
                    isInitializing -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                }
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ENGINE INITIALIZATION STATUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = when {
                            isFailed -> AppRed.copy(alpha = 0.2f)
                            isInitializing -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else -> Color(0xFFFFB300).copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = when {
                                isFailed -> "FAILED"
                                isInitializing -> "INITIALIZING (${(uiState.engineInitProgress * 100).toInt()}%)"
                                else -> "STANDBY (OFFLINE)"
                            },
                            color = when {
                                isFailed -> AppRed
                                isInitializing -> MaterialTheme.colorScheme.primary
                                else -> Color(0xFFFFB300)
                            },
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (isInitializing) {
                    LinearProgressIndicator(
                        progress = { uiState.engineInitProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.engineInitStepMessage.ifBlank { "Initializing engine telemetry..." },
                        fontSize = 10.5.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                } else if (isFailed) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = AppRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = errorMsg ?: "Engine initialization failed due to connection error.",
                            fontSize = 10.5.sp,
                            color = AppRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "Engine telemetry is in standby. Click initialize below to mount real-time telemetry and open the interactive sandbox.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Subsystems Checklist Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ENGINE SUBSYSTEMS TO MOUNT",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                subsystems.forEach { sys ->
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isInitializing) Icons.Default.Refresh else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isInitializing) MaterialTheme.colorScheme.primary else AppGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = sys,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Main Action Button
        if (isFailed) {
            Button(
                onClick = onRetryInitialize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppRed)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("RETRY INITIALIZATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        } else if (isInitializing) {
            Button(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("INITIALIZING ENGINE...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Button(
                onClick = onInitialize,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("INITIALIZE ENGINE", color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
fun ModuleCurriculumTab(
    module: TrainingModule,
    uiState: AcademyUiState,
    onInitialize: () -> Unit,
    onRetryInitialize: () -> Unit,
    onOpenEngine: () -> Unit,
    onAskMentor: (String) -> Unit
) {
    val isInitializing = uiState.engineInitStatus == EngineInitStatus.INITIALIZING && uiState.selectedModule?.id == module.id
    val isFailed = uiState.engineInitStatus == EngineInitStatus.FAILED && uiState.selectedModule?.id == module.id
    val isReady = uiState.engineInitStatus == EngineInitStatus.READY && uiState.initializedModuleId == module.id

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Overview Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("EXECUTIVE LEARNING OBJECTIVE", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                val description = when {
                    module.title.contains("Simulation", ignoreCase = true) ->
                        "Master order book flow, gamma scalping, delta neutrality, and execution discipline through real-time market replay scenarios."
                    module.title.contains("Nifty", ignoreCase = true) ->
                        "Deconstruct the index engine: constituent heavyweight weightings, sectoral beta rotation, and Smart Money Concepts (SMC) liquidity sweeps."
                    module.title.contains("Bias", ignoreCase = true) ->
                        "Identify and eradicate retail psychological errors: loss aversion, breakout FOMO, revenge trading, and confirmation bias."
                    module.title.contains("Risk", ignoreCase = true) ->
                        "Enforce strict capital preservation: mathematical 1% risk rules, dynamic position sizing, and mandatory daily drawdown circuit breakers."
                    else ->
                        "Institutional market framework covering advanced positioning, derivative hedging, and risk mitigation protocols."
                }
                Text(description, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.5.sp, lineHeight = 16.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Curriculum Topics Breakdown
        Text("CORE CURRICULUM MODULES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
        Spacer(Modifier.height(6.dp))

        module.topics.forEachIndexed { index, topic ->
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("${index + 1}", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(topic, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        val subtext = when (index) {
                            0 -> "Theoretical foundation & institutional execution rules"
                            1 -> "Mathematical edge, Greeks exposure & risk management"
                            2 -> "Live market mechanics & order flow footprint"
                            else -> "Scenario stress testing & post-trade audit"
                        }
                        Text(subtext, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp)
                    }
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppGreen, modifier = Modifier.size(14.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action Buttons Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onAskMentor("Explain ${module.title} and institutional execution rules.") },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text("ASK MENTOR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            if (isFailed) {
                Button(
                    onClick = onRetryInitialize,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RETRY INITIALIZATION", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 10.sp)
                }
            } else if (isInitializing) {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("INITIALIZING...", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
            } else if (isReady) {
                Button(
                    onClick = onOpenEngine,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("OPEN ENGINE", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp)
                }
            } else {
                Button(
                    onClick = onInitialize,
                    modifier = Modifier.weight(1.3f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("INITIALIZE MODULE", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 10.5.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. INSTITUTIONAL SIMULATION ENGINE INTERACTIVE VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SimulationEngineInteractiveContent(
    uiState: AcademyUiState,
    onSimAction: (String) -> Unit,
    onAskMentor: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Engine Status Banner
        EngineOnlineBanner("INSTITUTIONAL SIMULATION ENGINE", "Real-Time Historical Tick Replay • Delta Neutrality Active")

        // Scenario Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("LIVE SIMULATION SCENARIO", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    Surface(color = AppGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text("STEP ${uiState.simulationStep} / 4", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(uiState.simScenario, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Spot: ${String.format(Locale.US, "%,.2f", uiState.simSpot)} • VIX: 13.15 • PCR: 0.94 • Regime: PINNING MAGNET",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Active Position & Live P&L
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("ACTIVE SIMULATED POSITION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("Bull Call Spread (+23800 CE / -23950 CE)", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("REAL-TIME P&L", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "${if (uiState.simulationPnl >= 0) "+₹" else "-₹"}${String.format(Locale.US, "%,d", kotlin.math.abs(uiState.simulationPnl.toInt()))}",
                            color = if (uiState.simulationPnl >= 0) AppGreen else AppRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline)
                Spacer(Modifier.height(8.dp))

                // Greeks Meter
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    GreekChip("DELTA", String.format(Locale.US, "%+.2f", uiState.simGreeks.delta), MaterialTheme.colorScheme.primary)
                    GreekChip("GAMMA", String.format(Locale.US, "%+.3f", uiState.simGreeks.gamma), Color.Cyan)
                    GreekChip("THETA", "${uiState.simGreeks.theta.toInt()}/h", AppRed)
                    GreekChip("VEGA", "+₹${uiState.simGreeks.vega.toInt()}", AppGreen)
                }
            }
        }

        // Feedback / Message
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(uiState.simulationMessage, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, lineHeight = 14.sp)
            }
        }

        // Simulation Action Controls
        Text("SIMULATION DECISION CONSOLE", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onSimAction("HEDGE") },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("DEPLOY HEDGE", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSimAction("ROLL") },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("ROLL STRIKE (+50)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onSimAction("NEXT") },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("ADVANCE TICK", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onSimAction("SQUARE") },
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AppGreen)
            ) {
                Text("SQUARE OFF", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Action History Log
        if (uiState.simHistory.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("DECISION AUDIT TRAIL", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    uiState.simHistory.takeLast(3).forEach { log ->
                        Text("• $log", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Performance Score
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("DISCIPLINE SCORE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("96 / 100", fontSize = 13.sp, color = AppGreen, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("VALUE AT RISK", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text("1.1% of Capital", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Column {
                    Text("RESET DRILL", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Text(
                        "RESTART",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onSimAction("RESET") }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. NIFTY 50 DYNAMICS INTERACTIVE VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun NiftyDynamicsInteractiveContent(
    uiState: AcademyUiState,
    onSectorFilter: (String) -> Unit,
    onConstituentToggle: (String) -> Unit,
    onDrillSubmit: (Int) -> Unit,
    onAskMentor: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Engine Status Banner
        EngineOnlineBanner("NIFTY 50 DYNAMICS ENGINE", "Index Weight Matrix • Sectoral Beta • SMC Liquidity Pools")

        // Market Architecture Metrics
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("NIFTY 50 INDEX ARCHITECTURE", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("SPOT", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(String.format(Locale.US, "%,.2f", uiState.niftySpot), fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("BREADTH", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text(uiState.niftyBreadth, fontSize = 13.sp, color = AppGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                    Column {
                        Text("FII NET FUTURES", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        Text("${String.format(Locale.US, "%.1f", uiState.niftyFiiLongPct)}% LONG", fontSize = 13.sp, color = Color.Cyan, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Live Simulated Index Delta Meter
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CONSTITUENT WEIGHT DELTA SIMULATOR", fontSize = 8.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text("Toggle heavyweight bias below to forecast move", fontSize = 8.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    color = if (uiState.niftyProjectedMove >= 0) AppGreen.copy(0.15f) else AppRed.copy(0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "DELTA: ${if (uiState.niftyProjectedMove >= 0) "+" else ""}${String.format(Locale.US, "%.1f", uiState.niftyProjectedMove)} pts",
                        color = if (uiState.niftyProjectedMove >= 0) AppGreen else AppRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Sector Filter Chips
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("ALL", "BANKING (36%)", "IT (14%)", "OIL & GAS (10%)").forEach { filterTag ->
                val isSelected = uiState.niftySectorFilter.equals(filterTag, ignoreCase = true) ||
                        (filterTag.startsWith("BANK") && uiState.niftySectorFilter.contains("BANK", ignoreCase = true)) ||
                        (filterTag.startsWith("IT") && uiState.niftySectorFilter.contains("IT", ignoreCase = true)) ||
                        (filterTag.startsWith("OIL") && uiState.niftySectorFilter.contains("OIL", ignoreCase = true))

                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onSectorFilter(filterTag) }
                ) {
                    Text(
                        text = filterTag,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Heavyweight Correlation Tracker
        Text("TOP CONSTITUENTS WEIGHTAGE MATRIX (TAP TO TOGGLE BIAS)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val filtered = uiState.niftyConstituents.filter {
                when {
                    uiState.niftySectorFilter.contains("BANK", ignoreCase = true) -> it.sector == "BANKING"
                    uiState.niftySectorFilter.contains("IT", ignoreCase = true) -> it.sector == "IT"
                    uiState.niftySectorFilter.contains("OIL", ignoreCase = true) -> it.sector == "OIL & GAS"
                    else -> true
                }
            }

            filtered.forEach { item ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onConstituentToggle(item.name) }
                        .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.name, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("${item.sector} • ${item.weight}% WT", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = (if (item.isSimulatedBullish) AppGreen else AppRed).copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = if (item.isSimulatedBullish) "▲ BULLISH" else "▼ BEARISH",
                                    color = if (item.isSimulatedBullish) AppGreen else AppRed,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // SMC Liquidity Map
        Text("SMART MONEY LIQUIDITY POOLS (SMC)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SmcLevelRow("Premium Supply Zone", "> 23,880", "Institutional Call Writing Wall", AppRed)
                SmcLevelRow("Fair Value Gap (FVG)", "23,760 - 23,785", "Defended by Domestic SIP Flows", Color.Cyan)
                SmcLevelRow("Discount Demand Pool", "< 23,720", "Buy-Side Liquidity & Put Writing Floor", AppGreen)
            }
        }

        // Interactive Drill
        Text("INTERACTIVE INDEX DYNAMICS DRILL", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Scenario: HDFC Bank (+1.8%) and ICICI Bank (+1.4%) surge on credit growth data, while Infosys (-0.9%) drags. How should you position NIFTY 50?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                val options = listOf(
                    "Buy Far OTM Puts due to tech earnings weakness",
                    "Deploy ATM Bull Call Spread (Banking 36% weight dominates IT 14%)",
                    "Short ATM Straddle without directional delta hedge"
                )

                options.forEachIndexed { idx, opt ->
                    val isSelected = uiState.niftyDrillAnswer == idx
                    val isCorrect = idx == 1
                    Surface(
                        onClick = { onDrillSubmit(idx) },
                        color = when {
                            isSelected && isCorrect -> AppGreen.copy(alpha = 0.2f)
                            isSelected && !isCorrect -> AppRed.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .border(
                                0.5.dp,
                                when {
                                    isSelected && isCorrect -> AppGreen
                                    isSelected && !isCorrect -> AppRed
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                opt,
                                fontSize = 9.5.sp,
                                color = if (isSelected && isCorrect) AppGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (uiState.niftyDrillAnswer != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (uiState.niftyDrillAnswer == 1)
                            "✓ Correct! Private banking weightage (36%) completely eclipses IT (14%), creating reliable upward index momentum."
                        else
                            "✗ Incorrect: Remember that BankNIFTY constituents account for over 36% of NIFTY weight versus IT at ~14%.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.niftyDrillAnswer == 1) AppGreen else AppRed
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. BEHAVIORAL BIAS DETECTION INTERACTIVE VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BehavioralBiasInteractiveContent(
    uiState: AcademyUiState,
    onToggleShield: () -> Unit,
    onNeutralize: (String) -> Unit,
    onActivateCooldown: () -> Unit,
    onDrillSubmit: (Int) -> Unit,
    onAskMentor: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Engine Status Banner
        EngineOnlineBanner("BEHAVIORAL BIAS DETECTION ENGINE", "Trader Discipline Index • Emotion Shield • Cognitive Auditor")

        // Discipline Score Card
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("TRADER DISCIPLINE INDEX", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(2.dp))
                    Text("${uiState.biasDisciplineScore} / 100", color = AppGreen, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    Text("Grade A • Institutional Compliance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = (if (uiState.biasEmotionShield) AppGreen else AppRed).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { onToggleShield() }
                    ) {
                        Text(
                            text = if (uiState.biasEmotionShield) "EMOTION SHIELD: ON" else "EMOTION SHIELD: OFF",
                            color = if (uiState.biasEmotionShield) AppGreen else AppRed,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (uiState.biasCooldownSeconds > 0) "Cooldown Active (${uiState.biasCooldownSeconds / 60}m)" else "0 Revenge Trades",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.5.sp
                    )
                }
            }
        }

        // 4 Biases Breakdown with Interactive Neutralize Actions
        Text("CRITICAL COGNITIVE BIASES (TAP TO NEUTRALIZE)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

        InteractiveBiasCard(
            biasKey = "LOSS_AVERSION",
            title = "1. Loss Aversion (Holding Losers)",
            flaw = "Holding losing positions 3.4x longer than winning trades hoping for breakeven.",
            remedy = "Institutional Protocol: Mandatory GTT Stop-Loss placed at entry.",
            actionLabel = "ATTACH MANDATORY GTT",
            isNeutralized = uiState.neutralizedBiases.contains("LOSS_AVERSION"),
            onAction = { onNeutralize("LOSS_AVERSION") }
        )

        InteractiveBiasCard(
            biasKey = "FOMO_CHASING",
            title = "2. FOMO Breakout Chasing",
            flaw = "Buying at high of day after 3 consecutive large green candles.",
            remedy = "Institutional Protocol: Limit orders placed inside Fair Value Gap retracements only.",
            actionLabel = "ENFORCE FVG LIMIT RULE",
            isNeutralized = uiState.neutralizedBiases.contains("FOMO_CHASING"),
            onAction = { onNeutralize("FOMO_CHASING") }
        )

        InteractiveBiasCard(
            biasKey = "REVENGE_TRADING",
            title = "3. Revenge Trading",
            flaw = "Increasing lot size after a stop-out to quickly recoup losses.",
            remedy = "Institutional Protocol: Mandatory 20-minute terminal cooling period.",
            actionLabel = "ACTIVATE 20-MIN COOLDOWN",
            isNeutralized = uiState.neutralizedBiases.contains("REVENGE_TRADING") || uiState.biasCooldownSeconds > 0,
            onAction = {
                onNeutralize("REVENGE_TRADING")
                onActivateCooldown()
            }
        )

        InteractiveBiasCard(
            biasKey = "CONFIRMATION_BIAS",
            title = "4. Confirmation Bias",
            flaw = "Selectively seeking bullish news while ignoring bearish options open interest build-up.",
            remedy = "Institutional Protocol: Pre-trade 2-sided Devil's Advocate checklist mandatory.",
            actionLabel = "RUN 2-SIDED AUDIT",
            isNeutralized = uiState.neutralizedBiases.contains("CONFIRMATION_BIAS"),
            onAction = { onNeutralize("CONFIRMATION_BIAS") }
        )

        // Interactive Drill
        Text("COGNITIVE BIAS SELF-AUDIT DRILL", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "Scenario: You took a -₹4,500 stop loss on NIFTY calls at 10:15 AM. 5 minutes later, market jumps 25 points. What is your institutional action?",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                val options = listOf(
                    "Re-enter with 2x lot size to quickly recoup the lost ₹4,500",
                    "Enforce 20-min cooldown, review journal, and only trade if a new valid 1% setup forms",
                    "Average down by selling naked put options without structure"
                )

                options.forEachIndexed { idx, opt ->
                    val isSelected = uiState.biasDrillAnswer == idx
                    val isCorrect = idx == 1
                    Surface(
                        onClick = { onDrillSubmit(idx) },
                        color = when {
                            isSelected && isCorrect -> AppGreen.copy(alpha = 0.2f)
                            isSelected && !isCorrect -> AppRed.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .border(
                                0.5.dp,
                                when {
                                    isSelected && isCorrect -> AppGreen
                                    isSelected && !isCorrect -> AppRed
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                opt,
                                fontSize = 9.5.sp,
                                color = if (isSelected && isCorrect) AppGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (uiState.biasDrillAnswer != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (uiState.biasDrillAnswer == 1)
                            "✓ Correct! Professional traders eliminate emotional cortisol by enforcing mechanical cooldowns."
                        else
                            "✗ Flaw detected: Revenge trading is the #1 destroyer of retail trading capital.",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.biasDrillAnswer == 1) AppGreen else AppRed
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. RISK DISCIPLINE ENGINE INTERACTIVE VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun RiskDisciplineInteractiveContent(
    uiState: AcademyUiState,
    onUpdateRisk: (Double?, Double?, Double?, Double?) -> Unit,
    onSimulateLoss: () -> Unit,
    onResetHalt: () -> Unit,
    onAskMentor: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val capital = uiState.riskCapital
    val riskPct = uiState.riskPercent
    val entry = uiState.riskEntry
    val sl = uiState.riskStopLoss

    val maxRiskAmount = capital * (riskPct / 100.0)
    val riskPerShare = (entry - sl).coerceAtLeast(1.0)
    val allowedShares = (maxRiskAmount / riskPerShare).toInt()
    val niftyLots = (allowedShares / 25).coerceAtLeast(1)
    val target1to2 = entry + (riskPerShare * 2.0)
    val target1to3 = entry + (riskPerShare * 3.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Engine Status Banner
        EngineOnlineBanner("RISK DISCIPLINE ENGINE", "1% Capital Mandate • Dynamic Sizing • Hard Circuit Breaker")

        // Calculator Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("DYNAMIC POSITION SIZING CALCULATOR", color = MaterialTheme.colorScheme.primary, fontSize = 8.5.sp, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(4.dp))
                Text("Mathematical capital allocation based on institutional 1% risk rule.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
            }
        }

        // Live Calculated Outputs Card
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("MAX RISK AMOUNT", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format(Locale.US, "%,d", maxRiskAmount.toInt())}", color = AppRed, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ALLOWED LOTS (NIFTY 25)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("$niftyLots Lots (${niftyLots * 25} qty)", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("1:2 MIN TARGET", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        Text("₹${String.format(Locale.US, "%,d", target1to2.toInt())}", color = AppGreen, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Quick Adjustment Sliders / Chips
        Text("INTERACTIVE CAPITAL & RISK ALLOCATION", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(200000.0 to "₹2L", 500000.0 to "₹5L", 1000000.0 to "₹10L", 2500000.0 to "₹25L").forEach { (capVal, label) ->
                val isSel = (capital == capVal)
                Surface(
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).clickable { onUpdateRisk(capVal, null, null, null) }
                ) {
                    Text(
                        label,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0.5 to "0.5% Risk", 1.0 to "1.0% Risk", 1.5 to "1.5% Risk", 2.0 to "2.0% Risk").forEach { (rVal, label) ->
                val isSel = (riskPct == rVal)
                Surface(
                    color = if (isSel) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f).clickable { onUpdateRisk(null, rVal, null, null) }
                ) {
                    Text(
                        label,
                        color = if (isSel) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcInputCard("ENTRY PRICE", "₹${entry.toInt()}", Modifier.weight(1f))
            CalcInputCard("STOP LOSS PRICE", "₹${sl.toInt()} (-₹${riskPerShare.toInt()})", Modifier.weight(1f))
        }

        // Daily Drawdown Circuit Breaker & Simulator
        Text("DRAWDOWN CIRCUIT BREAKER & MANDATES", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (uiState.riskCircuitBreakerHalted) AppRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(8.dp),
            border = if (uiState.riskCircuitBreakerHalted) BorderStroke(1.dp, AppRed) else null
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Daily Max Drawdown Limit (3.0%)", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text("₹${(capital * 0.03).toInt()} (HARD HALT)", fontSize = 9.sp, color = AppRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Current Session Drawdown", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "${String.format(Locale.US, "%.1f", uiState.riskCurrentDrawdown)}% / 3.0%",
                        fontSize = 9.sp,
                        color = if (uiState.riskCircuitBreakerHalted) AppRed else AppGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Circuit Breaker Status", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (uiState.riskCircuitBreakerHalted) "CIRCUIT BREAKER TRIGGERED (HALT)" else "ACTIVE & ARMED",
                        fontSize = 9.sp,
                        color = if (uiState.riskCircuitBreakerHalted) AppRed else AppGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onSimulateLoss,
                        enabled = !uiState.riskCircuitBreakerHalted,
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppRed)
                    ) {
                        Text("SIMULATE LOSS (-1.1%)", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onResetHalt,
                        modifier = Modifier.weight(1f).height(34.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("RESET CIRCUIT BREAKER", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED HELPER COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun EngineOnlineBanner(engineName: String, subtitle: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, AppGreen.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).background(AppGreen, CircleShape))
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(engineName, color = MaterialTheme.colorScheme.onSurface, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
            }
            Surface(color = AppGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text("ONLINE", color = AppGreen, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
fun InteractiveBiasCard(
    biasKey: String,
    title: String,
    flaw: String,
    remedy: String,
    actionLabel: String,
    isNeutralized: Boolean,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isNeutralized) AppGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().border(
            0.5.dp,
            if (isNeutralized) AppGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
            RoundedCornerShape(8.dp)
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = if (isNeutralized) AppGreen else MaterialTheme.colorScheme.onSurface)
                Surface(
                    color = (if (isNeutralized) AppGreen else AppRed).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        if (isNeutralized) "✓ NEUTRALIZED" else "ACTIVE RISK",
                        color = if (isNeutralized) AppGreen else AppRed,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(flaw, fontSize = 9.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(remedy, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onAction,
                enabled = !isNeutralized,
                modifier = Modifier.fillMaxWidth().height(30.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = AppGreen.copy(alpha = 0.2f),
                    disabledContentColor = AppGreen
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    if (isNeutralized) "✓ BIAS AUDIT PASSED" else actionLabel,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SmcLevelRow(label: String, price: String, context: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
            Text(context, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(price, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun CalcInputCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier.border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 7.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
fun GreekChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 7.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 10.5.sp, color = color, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun GeneralModuleInteractiveContent(module: TrainingModule, onAskMentor: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(module.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Interactive environment initialized. Topics: ${module.topics.joinToString(", ")}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onAskMentor("Explain the practical institutional execution of ${module.title}") },
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("CONSULT AI MENTOR", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}
