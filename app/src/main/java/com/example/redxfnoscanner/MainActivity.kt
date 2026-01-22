package com.example.redxfnoscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.redxfnoscanner.domain.*
import com.example.redxfnoscanner.ui.MainViewModel
import com.example.redxfnoscanner.ui.theme.RedxFnoScannerTheme
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RedxFnoScannerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val marketRegime by viewModel.marketRegime.collectAsState()
                    val recommendedStrategy by viewModel.recommendedStrategy.collectAsState()
                    val tradeSignal by viewModel.tradeSignal.collectAsState()

                    // States for Practice Mode
                    val practicePosition by viewModel.practicePosition.collectAsState()
                    val payoffData by viewModel.payoffChartData.collectAsState()
                    val liveSpotPrice by viewModel.liveSpotPrice.collectAsState()
                    val livePnl by viewModel.livePnl.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.fetchFnoData("NIFTY") // Example symbol
                    }

                    MainScreen(
                        marketRegime,
                        recommendedStrategy,
                        tradeSignal,
                        practicePosition,
                        payoffData,
                        liveSpotPrice,
                        livePnl,
                        onStrategySelected = { strategy ->
                            viewModel.setupPracticeStrategy(strategy)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    marketRegime: MarketRegime?,
    recommendedStrategy: OptionStrategy?,
    tradeSignal: TradeSignal?,
    practicePosition: Position?,
    payoffData: List<PayoffPoint>,
    liveSpotPrice: Double?,
    livePnl: Double?,
    onStrategySelected: (OptionStrategy) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = when (marketRegime) {
                    MarketRegime.TRENDING -> "Market is Trending"
                    MarketRegime.RANGE_BOUND -> "Market is Range-Bound"
                    MarketRegime.VOLATILITY_DRIVEN -> "Market is Volatility-Driven"
                    null -> "Fetching market data..."
                },
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Recommended Strategy: ${recommendedStrategy?.name ?: "..."}",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            tradeSignal?.let {
                TradeSignalView(it)
                Divider(modifier = Modifier.padding(vertical = 16.dp))
            }
        }

        item {
            PracticeModeView(
                practicePosition,
                payoffData,
                liveSpotPrice,
                livePnl,
                onStrategySelected
            )
        }
    }
}

@Composable
fun TradeSignalView(tradeSignal: TradeSignal) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Trade Signal", style = MaterialTheme.typography.headlineSmall)
        Text(text = "Confidence: ${tradeSignal.confidenceScore}")
        // ... other trade signal details
    }
}

@Composable
fun PracticeModeView(
    position: Position?,
    payoffData: List<PayoffPoint>,
    liveSpotPrice: Double?,
    livePnl: Double?,
    onStrategySelected: (OptionStrategy) -> Unit
) {
    val df = DecimalFormat("#.##")
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Practice Mode", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onStrategySelected(OptionStrategy.BULL_CALL_SPREAD) }) {
            Text("Load Bull Call Spread")
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (position != null) {
            liveSpotPrice?.let {
                Text("Live Spot Price: ${df.format(it)}", style = MaterialTheme.typography.bodyLarge)
            }
            livePnl?.let {
                Text("Live P&L: ${df.format(it)}", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Payoff Chart Data", style = MaterialTheme.typography.titleMedium)
            payoffData.forEach { point ->
                Text("Price: ${df.format(point.price)}, P&L: ${df.format(point.profitLoss)}")
            }
        }
    }
}
