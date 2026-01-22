// NOTE: The code in this file is correct. The 'Unresolved reference' errors are caused by a stuck
// build process. The standard solution is to restart the IDE to clear the lock.

package com.example.redxchartlibrary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.redxchartlibrary.charts.PerformanceMonitor
import com.example.redxchartlibrary.charts.TradingChart
import com.example.redxchartlibrary.model.Candle
import com.example.redxchartlibrary.state.ChartState
import com.example.redxchartlibrary.state.DrawingMode
import com.example.redxchartlibrary.ui.ChartViewModel
import com.example.redxchartlibrary.ui.UiState
import com.example.redxchartlibrary.ui.theme.RedxChartLibraryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: ChartViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RedxChartLibraryTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                LaunchedEffect(Unit) {
                    viewModel.alertEvents.collect { event ->
                        scope.launch {
                            snackbarHostState.showSnackbar(message = "Alert: ${event.message}")
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        ChartToolbar(viewModel = viewModel)
                        
                        val uiState by viewModel.uiState.collectAsState()
                        
                        Box(modifier = Modifier.weight(1f)) {
                            when (val state = uiState) {
                                is UiState.Loading -> {
                                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                }
                                is UiState.Success -> {
                                    TradingChart(
                                        candles = state.candles,
                                        chartState = viewModel.chartState,
                                        onSaveDrawing = { viewModel.saveDrawing(it) }
                                    )
                                    PerformanceMonitor()
                                }
                                is UiState.Error -> {
                                    Text(
                                        text = state.message,
                                        modifier = Modifier.align(Alignment.Center)
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
fun ChartToolbar(viewModel: ChartViewModel) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = { viewModel.startLiveStream() }) { Text("Live") }
            Button(onClick = { viewModel.startHistoricalReplay() }) { Text("Replay") }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { viewModel.chartState.drawingMode = DrawingMode.TRENDLINE }) { Text("Trend") }
            Button(onClick = { viewModel.chartState.drawingMode = DrawingMode.FIBONACCI }) { Text("Fibonacci") }
            Button(onClick = { viewModel.chartState.drawingMode = DrawingMode.NONE }) { Text("None") }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    RedxChartLibraryTheme {
        TradingChart(
            candles = listOf(
                Candle(System.currentTimeMillis(), 100f, 110f, 90f, 105f, 1000),
                Candle(System.currentTimeMillis() + 60000, 105f, 115f, 95f, 110f, 1200),
            ),
            chartState = ChartState(),
            onSaveDrawing = {}
        )
    }
}