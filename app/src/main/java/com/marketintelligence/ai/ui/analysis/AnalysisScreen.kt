package com.marketintelligence.ai.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.marketintelligence.ai.domain.engine.MarketSessionInfo
import com.marketintelligence.ai.domain.engine.SessionState
import com.marketintelligence.ai.domain.model.StockData
import com.marketintelligence.ai.ui.composable.ChartIndicatorConfig
import com.marketintelligence.ai.ui.composable.ChartStyle
import com.marketintelligence.ai.ui.composable.InstitutionalChartEngine
import com.marketintelligence.ai.ui.market.MarketViewModel
import com.marketintelligence.ai.ui.theme.AppGreen
import com.marketintelligence.ai.ui.theme.AppRed

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel(),
    marketViewModel: MarketViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val marketUiState by marketViewModel.uiState.collectAsState()
    val activeChartEngine by viewModel.activeChartEngine.collectAsState()
    var isFullScreen by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val timeframes = listOf("1m", "5m", "15m", "1H", "4H", "1D", "1W")

    DisposableEffect(Unit) {
        marketViewModel.startListeningForLivePrices()
        marketViewModel.startPollingCryptoPrices()
        onDispose {}
    }

    val liveInstrumentData by remember(uiState.symbol) {
        derivedStateOf {
            marketViewModel.getSelectedInstrumentData(uiState.symbol)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CompositionLocalProvider(
                LocalLifecycleOwner provides lifecycleOwner,
                LocalSavedStateRegistryOwner provides savedStateRegistryOwner
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0E12))
                ) {
                    if (activeChartEngine != null && activeChartEngine?.engineName != "Proprietary Engine") {
                        activeChartEngine?.Render(
                            symbol = uiState.symbol,
                            timeframe = uiState.selectedTimeframe,
                            candles = uiState.candles
                        )
                    } else {
                        InstitutionalChartEngine(
                            symbol = uiState.symbol,
                            timeframe = uiState.selectedTimeframe,
                            candles = uiState.candles,
                            chartStyle = uiState.chartStyle,
                            indicatorConfig = uiState.indicatorConfig,
                            currentPrice = liveInstrumentData?.price ?: (if (uiState.currentPrice > 0.0) uiState.currentPrice else uiState.candles.lastOrNull()?.close)
                        )
                    }

                    IconButton(
                        onClick = { isFullScreen = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
            }
        }
    }

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
            sessionInfo = uiState.sessionInfo,
            currentPrice = uiState.currentPrice,
            priceChange = uiState.priceChange,
            priceChangePercent = uiState.priceChangePercent
        )

        var timeframeExpanded by remember { mutableStateOf(false) }
        var chartStyleExpanded by remember { mutableStateOf(false) }

        // 1. Controls Row (Timeframe & Chart Style Dropdowns)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timeframe Dropdown
            Box {
                Surface(
                    onClick = { timeframeExpanded = true },
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = uiState.selectedTimeframe,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select Timeframe",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = timeframeExpanded,
                    onDismissRequest = { timeframeExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    timeframes.forEach { tf ->
                        val isSelected = uiState.selectedTimeframe == tf
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = tf,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            onClick = {
                                viewModel.onTimeframeSelected(tf)
                                timeframeExpanded = false
                            }
                        )
                    }
                }
            }

            if (activeChartEngine?.engineName == "Proprietary Engine" || activeChartEngine == null) {
                // Chart Style Dropdown
                Box {
                    val currentLabel = when (uiState.chartStyle) {
                        ChartStyle.CANDLESTICK -> "Candles"
                        ChartStyle.HOLLOW_CANDLE -> "Hollow"
                        ChartStyle.LINE -> "Line"
                        ChartStyle.AREA -> "Area"
                        ChartStyle.HEIKIN_ASHI -> "Heikin-Ashi"
                    }
                    Surface(
                        onClick = { chartStyleExpanded = true },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentLabel,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Chart Style",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = chartStyleExpanded,
                        onDismissRequest = { chartStyleExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        ChartStyle.values().forEach { style ->
                            val isSelected = uiState.chartStyle == style
                            val label = when (style) {
                                ChartStyle.CANDLESTICK -> "Candles"
                                ChartStyle.HOLLOW_CANDLE -> "Hollow"
                                ChartStyle.LINE -> "Line"
                                ChartStyle.AREA -> "Area"
                                ChartStyle.HEIKIN_ASHI -> "Heikin-Ashi"
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace
                                    )
                                },
                                trailingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                onClick = {
                                    viewModel.onChartStyleSelected(style)
                                    chartStyleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Indicator Toggle Chips
        if (activeChartEngine?.engineName == "Proprietary Engine" || activeChartEngine == null) {
            IndicatorToggleBar(
                config = uiState.indicatorConfig,
                onToggleSMC = { viewModel.toggleSMC() },
                onToggleEMA = { viewModel.toggleEMA() },
                onToggleBB = { viewModel.toggleBollinger() },
                onToggleVWAP = { viewModel.toggleVWAP() },
                onToggleVOL = { viewModel.toggleVolume() },
                onToggleRSI = { viewModel.toggleRSI() },
                onToggleMACD = { viewModel.toggleMACD() }
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Institutional Chart Display Container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(470.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0E12)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else if (activeChartEngine != null && activeChartEngine?.engineName != "Proprietary Engine") {
                    activeChartEngine?.Render(
                        symbol = uiState.symbol,
                        timeframe = uiState.selectedTimeframe,
                        candles = uiState.candles
                    )
                } else {
                    InstitutionalChartEngine(
                        symbol = uiState.symbol,
                        timeframe = uiState.selectedTimeframe,
                        candles = uiState.candles,
                        chartStyle = uiState.chartStyle,
                        indicatorConfig = uiState.indicatorConfig,
                        currentPrice = liveInstrumentData?.price ?: (if (uiState.currentPrice > 0.0) uiState.currentPrice else uiState.candles.lastOrNull()?.close)
                    )
                }

                IconButton(
                    onClick = { isFullScreen = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                ) {
                    Icon(Icons.Filled.Fullscreen, contentDescription = "Full Screen", tint = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun IndicatorToggleBar(
    config: ChartIndicatorConfig,
    onToggleSMC: () -> Unit,
    onToggleEMA: () -> Unit,
    onToggleBB: () -> Unit,
    onToggleVWAP: () -> Unit,
    onToggleVOL: () -> Unit,
    onToggleRSI: () -> Unit,
    onToggleMACD: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IndicatorPill(label = "EMA 9/20/50/200", active = config.showEMA, activeColor = Color(0xFFFFD600), onClick = onToggleEMA)
        IndicatorPill(label = "Bollinger (20)", active = config.showBollinger, activeColor = Color(0xFF2979FF), onClick = onToggleBB)
        IndicatorPill(label = "VWAP", active = config.showVWAP, activeColor = Color(0xFF00E5FF), onClick = onToggleVWAP)
        IndicatorPill(label = "Volume", active = config.showVolume, activeColor = Color.White, onClick = onToggleVOL)
        IndicatorPill(label = "RSI (14)", active = config.showRSI, activeColor = Color(0xFFBA68C8), onClick = onToggleRSI)
        IndicatorPill(label = "MACD", active = config.showMACD, activeColor = Color(0xFF29B6F6), onClick = onToggleMACD)
    }
}

@Composable
fun IndicatorPill(label: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (active) activeColor.copy(alpha = 0.2f) else Color(0xFF14161D),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, if (active) activeColor else Color(0xFF263238)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (active) activeColor else Color.Gray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun InstitutionalHeader(
    symbol: String,
    liveData: StockData?,
    sessionInfo: MarketSessionInfo?,
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
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val sessionColor = if (sessionInfo?.isLiveTrading == true) AppGreen else Color(0xFFFFB74D)
                    Box(modifier = Modifier.size(5.dp).background(sessionColor, RoundedCornerShape(2.5.dp)))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = sessionInfo?.sessionLabel ?: "LIVE TRADING",
                        color = sessionColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
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
                Text(formattedPrice, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                Text("${if(isPositive) "+" else ""}$formattedChange (${if(isPositive) "+" else ""}${"%.2f".format(changePercent)}%)", color = if(isPositive) AppGreen else AppRed, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
