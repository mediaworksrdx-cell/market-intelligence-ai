package com.example.marketintelligence.data.engine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.marketintelligence.domain.engine.ChartEngine
import com.example.marketintelligence.ui.theme.AppGreen
import com.example.redxchartlibrary.charts.TradingChart
import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.state.ChartState
import com.example.redxchartlibrary.state.DrawingMode
import javax.inject.Inject

class ProprietaryChartEngine @Inject constructor() : ChartEngine {
    override val engineName: String = "Proprietary Engine"

    @Composable
    override fun Render(symbol: String) {
        var chartTimeframe by remember { mutableStateOf("1H") }
        val chartState = remember { ChartState() }
        val mockCandles = rememberMockCandles(chartTimeframe)

        Box(modifier = Modifier.fillMaxSize()) {
            TradingChart(
                modifier = Modifier.fillMaxSize(),
                candles = mockCandles,
                chartState = chartState,
                onSaveDrawing = {}
            )

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        val chartTfs = listOf("1m", "5m", "15m", "1H", "1D")
                        chartTfs.forEach { tf ->
                            val isSelected = tf == chartTimeframe
                            Text(
                                text = tf,
                                color = if (isSelected) AppGreen else Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { chartTimeframe = tf }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolButton(
                    icon = Icons.Filled.Timeline,
                    isSelected = chartState.drawingMode == DrawingMode.TRENDLINE,
                    onClick = { 
                        chartState.drawingMode = if (chartState.drawingMode == DrawingMode.TRENDLINE) DrawingMode.NONE else DrawingMode.TRENDLINE 
                    }
                )
                ToolButton(
                    icon = Icons.Filled.Create,
                    isSelected = chartState.drawingMode == DrawingMode.FIBONACCI,
                    onClick = { 
                        chartState.drawingMode = if (chartState.drawingMode == DrawingMode.FIBONACCI) DrawingMode.NONE else DrawingMode.FIBONACCI 
                    }
                )
            }
        }
    }

    @Composable
    private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
        Surface(
            color = if (isSelected) AppGreen else Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.size(36.dp).clickable { onClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    @Composable
    private fun rememberMockCandles(timeframe: String): List<Candle> {
        return remember(timeframe) {
            List(50) { i ->
                Candle(
                    timestamp = System.currentTimeMillis() - (50 - i) * 60000,
                    open = 22000f + (Math.random() * 100).toFloat(),
                    high = 22150f + (Math.random() * 50).toFloat(),
                    low = 21950f - (Math.random() * 50).toFloat(),
                    close = 22050f + (Math.random() * 100).toFloat(),
                    volume = 1000L + (Math.random() * 500).toLong()
                )
            }
        }
    }
}
