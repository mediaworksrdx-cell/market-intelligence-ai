package com.example.marketintelligence.ui.analysis

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.marketintelligence.domain.engine.ChartEngine
import com.example.marketintelligence.ui.theme.*

@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val activeChartEngine by viewModel.activeChartEngine.collectAsState(initial = null)
    var isFullScreen by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    // Full Screen Chart Dialog
    if (isFullScreen) {
        Dialog(
            onDismissRequest = { isFullScreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                activeChartEngine?.Render(symbol = uiState.symbol)
                
                IconButton(
                    onClick = { isFullScreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
    ) {
        StudyHeaderSection(uiState.symbol, uiState.selectedTimeframe) {
            viewModel.onTimeframeSelected(it)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(450.dp)
                .padding(horizontal = 16.dp)
                .background(Color(0xFF0A0A0A), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { isFullScreen = true }
                    )
                }
        ) {
            activeChartEngine?.Render(symbol = uiState.symbol) ?: CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AppGreen
            )
            
            Row(
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartOverlayButton(icon = Icons.Filled.Layers, label = "SMC ENGINE")
                ChartOverlayButton(icon = Icons.Filled.Info, label = "AI THEORY")
            }
            
            Text(
                "Double tap for Full Screen",
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TechnicalIndicatorsSection(uiState.technicalIndicators, uiState.selectedTimeframe)

        Spacer(modifier = Modifier.height(24.dp))

        uiState.aiAnalysis?.let { analysis ->
            AiDeepDiveSection(analysis)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ChartOverlayButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Surface(
        color = Color.Black.copy(alpha = 0.7f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = AppGreen, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun StudyHeaderSection(symbol: String, selectedTimeframe: String, onTimeframeSelected: (String) -> Unit) {
    val timeframes = listOf("5m", "15m", "30m", "1H", "4H", "1D", "1W", "1M")
    
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = symbol,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "SELECT STUDY CONTEXT",
            color = Color.Gray,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(timeframes) { tf ->
                val isSelected = tf == selectedTimeframe
                Surface(
                    color = if (isSelected) AppGreen else Color(0xFF1A1A1A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onTimeframeSelected(tf) }
                ) {
                    Text(
                        text = tf,
                        color = if (isSelected) Color.Black else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TechnicalIndicatorsSection(indicators: TechnicalIndicators, timeframe: String) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("TECHNICAL ANALYSIS STUDY ($timeframe)", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IndicatorCard("RSI (14)", indicators.rsi.toString(), if (indicators.rsi > 70) AppRed else if (indicators.rsi < 30) AppGreen else Color.White, Modifier.weight(1f))
            IndicatorCard("EMA (20)", indicators.ema20.toString(), Color.Cyan, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IndicatorCard("EMA (50)", indicators.ema50.toString(), Color.Yellow, Modifier.weight(1f))
            IndicatorCard("EMA (200)", indicators.ema200.toString(), Color.Magenta, Modifier.weight(1f))
        }
    }
}

@Composable
fun IndicatorCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, color = Color.Gray, fontSize = 11.sp)
            Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AiDeepDiveSection(analysis: com.example.marketintelligence.domain.model.AIAnalysisResult) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text("AI THEORY ANALYSIS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121212)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (analysis.signal.name == "BULLISH") AppGreen.copy(0.2f) else AppRed.copy(0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            analysis.signal.name,
                            color = if (analysis.signal.name == "BULLISH") AppGreen else AppRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Study Confidence: ${analysis.confidence}%", color = Color.Yellow, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text("THEORETICAL RATIONALE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(analysis.rationale, color = Color.White, fontSize = 14.sp, lineHeight = 20.sp)
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = Color(0xFF1A1A1A))
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TradeSetupItem("STUDY PRICE", analysis.entryZone ?: "-", Color.White)
                    TradeSetupItem("INVALIDATION", analysis.stopLoss?.toString() ?: "-", AppRed)
                    TradeSetupItem("PROJECTION", analysis.target?.firstOrNull() ?: "-", AppGreen)
                }
            }
        }
    }
}

@Composable
fun TradeSetupItem(label: String, value: String, valueColor: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}
