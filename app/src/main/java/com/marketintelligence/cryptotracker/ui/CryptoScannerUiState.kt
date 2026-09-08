package com.marketintelligence.cryptotracker.ui

import com.marketintelligence.cryptotracker.engine.*
import com.marketintelligence.cryptotracker.scanner.*
import com.marketintelligence.tradeengine.models.Candle

/**
 * Represents the UI state for the main Crypto Scanner Dashboard.
 */
data class CryptoScannerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val overview: MarketOverview? = null,
    val selectedTimeframe: String = "15m",
    val selectedFilter: SignalFilter = SignalFilter.ALL,
    val lastRefreshTime: Long = 0L
)

/**
 * Filter options for the scanner dashboard list.
 */
enum class SignalFilter { ALL, LONG_ONLY, SHORT_ONLY, A_PLUS_ONLY }

/**
 * Represents the UI state for the detailed view of a specific coin.
 */
data class CoinDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val symbol: String = "",
    val currentPrice: Double = 0.0,
    val candles: List<Candle> = emptyList(),
    val setup: CryptoSetup? = null,
    val smcAnalysis: CryptoSMCAnalysis? = null,
    val fvgs: List<EnrichedFVG> = emptyList(),
    val rsiAnalysis: CryptoRSIAnalysis? = null,
    val multiTfRSI: Map<String, Double> = emptyMap()
)
