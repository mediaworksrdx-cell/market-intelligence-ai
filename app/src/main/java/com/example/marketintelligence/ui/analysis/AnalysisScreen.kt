package com.example.marketintelligence.ui.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.engine.EngineRouter
import com.example.marketintelligence.domain.model.StockData
import com.example.marketintelligence.domain.chart.DrawingToolType
import com.example.marketintelligence.ui.chart.*
import com.example.marketintelligence.ui.market.MarketViewModel
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel(),
    marketViewModel: MarketViewModel = hiltViewModel(),
    engineRouter: EngineRouter? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val marketUiState by marketViewModel.uiState.collectAsState()
    var isFullScreen by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        marketViewModel.startListeningForLivePrices()
        marketViewModel.startPollingCryptoPrices()
        onDispose {
            marketViewModel.stopListeningForLivePrices()
            marketViewModel.stopPollingCryptoPrices()
        }
    }

    val liveInstrumentData by remember(uiState.symbol) {
        derivedStateOf {
            marketViewModel.getSelectedInstrumentData(uiState.symbol)
        }
    }

    val currentLivePrice by remember(uiState.symbol, uiState.currentPrice, liveInstrumentData) {
        derivedStateOf {
            when {
                uiState.currentPrice > 0.0 -> uiState.currentPrice
                liveInstrumentData != null && liveInstrumentData!!.price > 0.0 -> liveInstrumentData!!.price
                else -> uiState.candles.lastOrNull()?.close ?: 0.0
            }
        }
    }

    // Fullscreen Dialog
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Fullscreen Toolbar Row with Close Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ChartToolbar(
                                selectedTimeframe = uiState.selectedTimeframe,
                                chartType = uiState.chartState.chartType,
                                activeDrawingTool = uiState.chartState.activeDrawingTool,
                                activeIndicatorCount = uiState.chartState.activeIndicators.count { it.enabled },
                                activeIndicators = uiState.chartState.activeIndicators,
                                cursorMode = uiState.chartState.cursorMode,
                                showVolume = uiState.chartState.showVolume,
                                showVolumeProfile = uiState.chartState.showVolumeProfile,
                                showFnoOverlay = uiState.chartState.showFnoOverlay,
                                showSmcOverlay = uiState.chartState.showSmcOverlay,
                                onTimeframeSelected = { viewModel.onTimeframeSelected(it) },
                                onChartTypeSelected = { viewModel.onChartTypeSelected(it) },
                                onCursorModeChanged = { viewModel.setCursorMode(it) },
                                onToggleIndicator = { viewModel.toggleIndicator(it) },
                                onOpenIndicatorSettings = { viewModel.toggleIndicatorSheet() },
                                onOpenIndicatorSettingsFor = { viewModel.openIndicatorSettings(it) },
                                onDrawingToolSelected = { viewModel.selectDrawingTool(it) },
                                onClearDrawings = { viewModel.clearDrawings() },
                                onToggleIndicators = { viewModel.toggleIndicatorSheet() },
                                onToggleDrawingTools = { viewModel.toggleDrawingPalette() },
                                onToggleVolume = { viewModel.toggleVolume() },
                                onToggleVolumeProfile = { viewModel.toggleVolumeProfile() },
                                onToggleFnoOverlay = { viewModel.toggleFnoOverlay() },
                                onToggleSmcOverlay = { viewModel.toggleSmcOverlay() },
                                onUndo = { viewModel.undoDrawing() },
                                onRedo = { viewModel.redoDrawing() },
                                onToggleFullScreen = { isFullScreen = false }
                            )
                        }
                        IconButton(
                            onClick = { isFullScreen = false },
                            modifier = Modifier.size(34.dp).padding(end = 2.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF344054))
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        AdvancedCandleStickChart(
                            chartState = uiState.chartState,
                            modifier = Modifier.fillMaxSize(),
                            timeframe = uiState.selectedTimeframe,
                            currentPrice = currentLivePrice,
                            onAddDrawingPoint = { point -> viewModel.addDrawingPoint(point) },
                            onSelectDrawing = { viewModel.selectDrawing(it) },
                            onDeleteDrawing = { viewModel.deleteDrawing(it) },
                            onContinueDrawing = { viewModel.continueDrawing(it) },
                            onMoveDrawingPoint = { id, idx, pt -> viewModel.moveDrawingPoint(id, idx, pt) },
                            onUpdateDrawingPoints = { id, pts -> viewModel.updateDrawingPoints(id, pts) },
                            onUpdateDrawingText = { id, text -> viewModel.updateDrawingText(id, text) },
                            onDrawingDragStart = { viewModel.recordDrawingStateSnapshot() },
                            onCancelDrawing = { viewModel.selectDrawingTool(DrawingToolType.NONE) },
                            onOpenIndicatorSettingsFor = { viewModel.openIndicatorSettings(it) },
                            onToggleIndicator = { viewModel.toggleIndicator(it) },
                            onRemoveIndicator = { viewModel.removeIndicator(it) },
                            onRemoveIndicatorConfig = { viewModel.removeIndicatorConfig(it) }
                        )
                    }
                }

                // Indicator Settings Sheet in Fullscreen
                AnimatedVisibility(
                    visible = uiState.showIndicatorSheet,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    IndicatorSettingsSheet(
                        activeIndicators = uiState.chartState.activeIndicators,
                        targetIndicatorType = uiState.targetIndicatorType,
                        onToggleIndicator = { viewModel.toggleIndicator(it) },
                        onUpdateIndicator = { viewModel.updateIndicator(it) },
                        onRemoveIndicator = { viewModel.removeIndicator(it) },
                        onAddIndicatorInstance = { viewModel.addIndicatorInstance(it) },
                        onRemoveIndicatorConfig = { viewModel.removeIndicatorConfig(it) },
                        onDismiss = { viewModel.toggleIndicatorSheet() }
                    )
                }

                // Drawing Tool Palette in Fullscreen
                AnimatedVisibility(
                    visible = uiState.showDrawingPalette,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it }),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    DrawingToolPalette(
                        activeDrawingTool = uiState.chartState.activeDrawingTool,
                        onToolSelected = {
                            viewModel.selectDrawingTool(it)
                            viewModel.toggleDrawingPalette()
                        },
                        onClearDrawings = { viewModel.clearDrawings() },
                        onDismiss = { viewModel.toggleDrawingPalette() }
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(horizontal = 4.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                InstitutionalHeader(
                    symbol = uiState.symbol,
                    liveData = liveInstrumentData,
                    currentPrice = uiState.currentPrice,
                    priceChange = uiState.priceChange,
                    priceChangePercent = uiState.priceChangePercent
                )
            }

            // ── Chart Toolbar ──
            ChartToolbar(
                selectedTimeframe = uiState.selectedTimeframe,
                chartType = uiState.chartState.chartType,
                activeDrawingTool = uiState.chartState.activeDrawingTool,
                activeIndicatorCount = uiState.chartState.activeIndicators.count { it.enabled },
                activeIndicators = uiState.chartState.activeIndicators,
                cursorMode = uiState.chartState.cursorMode,
                showVolume = uiState.chartState.showVolume,
                showVolumeProfile = uiState.chartState.showVolumeProfile,
                showFnoOverlay = uiState.chartState.showFnoOverlay,
                showSmcOverlay = uiState.chartState.showSmcOverlay,
                onTimeframeSelected = { viewModel.onTimeframeSelected(it) },
                onChartTypeSelected = { viewModel.onChartTypeSelected(it) },
                onCursorModeChanged = { viewModel.setCursorMode(it) },
                onToggleIndicator = { viewModel.toggleIndicator(it) },
                onOpenIndicatorSettings = { viewModel.toggleIndicatorSheet() },
                onOpenIndicatorSettingsFor = { viewModel.openIndicatorSettings(it) },
                onDrawingToolSelected = { viewModel.selectDrawingTool(it) },
                onClearDrawings = { viewModel.clearDrawings() },
                onToggleIndicators = { viewModel.toggleIndicatorSheet() },
                onToggleDrawingTools = { viewModel.toggleDrawingPalette() },
                onToggleVolume = { viewModel.toggleVolume() },
                onToggleVolumeProfile = { viewModel.toggleVolumeProfile() },
                onToggleFnoOverlay = { viewModel.toggleFnoOverlay() },
                onToggleSmcOverlay = { viewModel.toggleSmcOverlay() },
                onUndo = { viewModel.undoDrawing() },
                onRedo = { viewModel.redoDrawing() },
                onToggleFullScreen = { isFullScreen = true }
            )

            Spacer(modifier = Modifier.height(2.dp))

            // ── Chart Card (Fills Remaining Screen Space - No Swiping Down Required) ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 2.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFEAECF0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else {
                        AdvancedCandleStickChart(
                            chartState = uiState.chartState,
                            modifier = Modifier.fillMaxSize(),
                            timeframe = uiState.selectedTimeframe,
                            currentPrice = currentLivePrice,
                            onAddDrawingPoint = { point -> viewModel.addDrawingPoint(point) },
                            onSelectDrawing = { viewModel.selectDrawing(it) },
                            onDeleteDrawing = { viewModel.deleteDrawing(it) },
                            onContinueDrawing = { viewModel.continueDrawing(it) },
                            onMoveDrawingPoint = { id, idx, pt -> viewModel.moveDrawingPoint(id, idx, pt) },
                            onUpdateDrawingPoints = { id, pts -> viewModel.updateDrawingPoints(id, pts) },
                            onUpdateDrawingText = { id, text -> viewModel.updateDrawingText(id, text) },
                            onDrawingDragStart = { viewModel.recordDrawingStateSnapshot() },
                            onCancelDrawing = { viewModel.selectDrawingTool(DrawingToolType.NONE) },
                            onOpenIndicatorSettingsFor = { viewModel.openIndicatorSettings(it) },
                            onToggleIndicator = { viewModel.toggleIndicator(it) },
                            onRemoveIndicator = { viewModel.removeIndicator(it) },
                            onRemoveIndicatorConfig = { viewModel.removeIndicatorConfig(it) }
                        )
                    }
                }
            }
        }

        // ── Indicator Settings Sheet (slides up from bottom) ──
        AnimatedVisibility(
            visible = uiState.showIndicatorSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            IndicatorSettingsSheet(
                activeIndicators = uiState.chartState.activeIndicators,
                targetIndicatorType = uiState.targetIndicatorType,
                onToggleIndicator = { viewModel.toggleIndicator(it) },
                onUpdateIndicator = { viewModel.updateIndicator(it) },
                onRemoveIndicator = { viewModel.removeIndicator(it) },
                onAddIndicatorInstance = { viewModel.addIndicatorInstance(it) },
                onRemoveIndicatorConfig = { viewModel.removeIndicatorConfig(it) },
                onDismiss = { viewModel.toggleIndicatorSheet() }
            )
        }

        // ── Drawing Tool Palette (slides up from bottom) ──
        AnimatedVisibility(
            visible = uiState.showDrawingPalette,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DrawingToolPalette(
                activeDrawingTool = uiState.chartState.activeDrawingTool,
                onToolSelected = {
                    viewModel.selectDrawingTool(it)
                    viewModel.toggleDrawingPalette()
                },
                onClearDrawings = { viewModel.clearDrawings() },
                onDismiss = { viewModel.toggleDrawingPalette() }
            )
        }
    }
}

@Composable
fun InstitutionalHeader(
    symbol: String,
    liveData: StockData?,
    currentPrice: Double = 0.0,
    priceChange: Double = 0.0,
    priceChangePercent: Double = 0.0
) {
    val price = when {
        currentPrice > 0.0 -> currentPrice
        liveData != null && liveData.price > 0.0 -> liveData.price
        else -> 0.0
    }
    val change = when {
        currentPrice > 0.0 -> priceChange
        liveData != null && liveData.price > 0.0 -> liveData.change
        else -> 0.0
    }
    val changePercent = when {
        currentPrice > 0.0 -> priceChangePercent
        liveData != null && liveData.price > 0.0 -> liveData.changePercent
        else -> 0.0
    }
    val isPositive = change >= 0

    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = symbol.uppercase(),
                    color = Color(0xFF131722),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(Color(0xFF089981), RoundedCornerShape(2.5.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text("LIVE DATA ACTIVE", color = Color(0xFF089981), fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val formattedPrice = when {
                    price <= 0.0 -> "0.00"
                    price < 0.001 -> String.format(java.util.Locale.US, "%.7f", price)
                    price < 1.0 -> String.format(java.util.Locale.US, "%.4f", price)
                    else -> String.format(java.util.Locale.US, "%,.2f", price)
                }
                val formattedChange = when {
                    kotlin.math.abs(change) < 0.001 -> String.format(java.util.Locale.US, "%.7f", change)
                    else -> String.format(java.util.Locale.US, "%.2f", change)
                }
                Text(
                    text = formattedPrice,
                    color = Color(0xFF131722),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "${if (isPositive) "+" else ""}$formattedChange (${if (isPositive) "+" else ""}${"%.2f".format(changePercent)}%)",
                    color = if (isPositive) Color(0xFF089981) else Color(0xFFF23645),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

