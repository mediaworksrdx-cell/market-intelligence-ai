package com.example.marketintelligence.ui.analysis

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import com.example.marketintelligence.ui.chart.*
import com.example.marketintelligence.ui.market.MarketViewModel
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.marketintelligence.ui.theme.AppRed

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel(),
    marketViewModel: MarketViewModel = hiltViewModel(),
    engineRouter: EngineRouter
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeChartEngine by engineRouter.activeChartEngine.collectAsState(initial = null)
    var isFullScreen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    val liveInstrumentData by remember(uiState.symbol) {
        derivedStateOf {
            marketViewModel.getSelectedInstrumentData(uiState.symbol)
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
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AdvancedCandleStickChart(
                    chartState = uiState.chartState,
                    modifier = Modifier.fillMaxSize(),
                    onAddDrawingPoint = { point -> viewModel.addDrawingPoint(point) }
                )

                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            InstitutionalHeader(
                symbol = uiState.symbol,
                liveData = liveInstrumentData,
            )

            // ── Chart Toolbar ──
            ChartToolbar(
                selectedTimeframe = uiState.selectedTimeframe,
                chartType = uiState.chartState.chartType,
                activeDrawingTool = uiState.chartState.activeDrawingTool,
                activeIndicatorCount = uiState.chartState.activeIndicators.count { it.enabled },
                activeIndicators = uiState.chartState.activeIndicators,
                onTimeframeSelected = { viewModel.onTimeframeSelected(it) },
                onChartTypeSelected = { viewModel.onChartTypeSelected(it) },
                onToggleIndicator = { viewModel.toggleIndicator(it) },
                onOpenIndicatorSettings = { viewModel.toggleIndicatorSheet() },
                onDrawingToolSelected = { viewModel.selectDrawingTool(it) },
                onClearDrawings = { viewModel.clearDrawings() },
                onToggleIndicators = { viewModel.toggleIndicatorSheet() },
                onToggleDrawingTools = { viewModel.toggleDrawingPalette() },
                onToggleFullScreen = { isFullScreen = true }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // ── Chart Card ──
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { isFullScreen = true })
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            onAddDrawingPoint = { point -> viewModel.addDrawingPoint(point) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                onToggleIndicator = { viewModel.toggleIndicator(it) },
                onUpdateIndicator = { viewModel.updateIndicator(it) },
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
                onToolSelected = { viewModel.selectDrawingTool(it) },
                onClearDrawings = { viewModel.clearDrawings() },
                onDismiss = { viewModel.toggleDrawingPalette() }
            )
        }
    }
}

@Composable
fun InstitutionalHeader(symbol: String, liveData: StockData?) {
    val price = liveData?.price ?: 0.0
    val change = liveData?.change ?: 0.0
    val changePercent = liveData?.changePercent ?: 0.0
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp)))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE DATA ACTIVE", color = MaterialTheme.colorScheme.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("%.2f".format(price), color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                Text("${if(isPositive) "+" else ""}${"%.2f".format(change)} (${if(isPositive) "+" else ""}${"%.2f".format(changePercent)}%)", color = if(isPositive) AppGreen else AppRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

